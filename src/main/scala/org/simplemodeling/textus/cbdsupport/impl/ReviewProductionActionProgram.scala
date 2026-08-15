package org.simplemodeling.textus.cbdsupport.impl

import java.time.Instant

import cats.syntax.all.*
import org.goldenport.{Conclusion, Consequence}
import org.goldenport.cncf.action.{ActionCall, ActionCallEntityStorePart}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.job.JobStatus
import org.goldenport.cncf.unitofwork.ExecUowM
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * Production action boundary for server-owned CAR Review Jobs.  The gateway
 * owns durable Job discovery while ReviewDiagnosisAdmissionProgram owns the
 * Entity aggregate and its terminal settlement.  A handled submit failure is
 * recoverable through exact Job discovery, and a claim-to-submit race is
 * retryable as pending; an abrupt process death after the Entity claim and
 * before submit or reconciliation is not crash-recoverable without persisted
 * submission intent, jobId, or an outbox.
 */
private[cbdsupport] final class ReviewProductionActionProgram(
  val core: ActionCall.Core,
  private val _injected_gateway: Option[CarReviewProductionJobPort] = None
) extends ActionCall.Core.Holder with ActionCallEntityStorePart {
  given ExecutionContext = core.executionContext

  private val _diagnosis = new ReviewDiagnosisAdmissionProgram(core)

  def start(request: ReviewStartRequest): ExecUowM[ReviewRunAdmission] =
    for {
      _ <- exec_from(CarReviewAuthorization.authorize("review.start", CarReviewAuthorization.roles(core.executionContext)))
      execution <- exec_from(CarReviewProductionExecution.create(request))
      gateway <- exec_from(_gateway)
      admission <- _diagnosis.admit(execution.plan)
      result <- admission match {
        case owner: CarReviewDiagnosisAdmission.Owner =>
          _submit_owner(owner, execution, gateway)
        case joined: CarReviewDiagnosisAdmission.Joined =>
          _join_or_retry(joined, execution, gateway)
        case reused: CarReviewDiagnosisAdmission.Reused =>
          _reuse_completed(reused, execution, gateway)
      }
    } yield result

  def get(reviewId: ReviewId): ExecUowM[ReviewRunAdmission] =
    for {
      gateway <- exec_from(_gateway)
      discovered <- exec_from(_required(
        gateway.findByReviewId(reviewId),
        s"review job: ${reviewId.value}"
      ))
      _ <- _settle(discovered.terminalLease)
      admission <- exec_from(_admission(discovered))
    } yield admission

  def cancel(
    reviewId: ReviewId,
    requestedAt: ReviewInstant
  ): ExecUowM[ReviewRunAdmission] =
    for {
      _ <- exec_from(CarReviewAuthorization.authorize("review.cancel", CarReviewAuthorization.roles(core.executionContext)))
      gateway <- exec_from(_gateway)
      discovered <- exec_from(_required(
        gateway.findByReviewId(reviewId),
        s"review job: ${reviewId.value}"
      ))
      _ <- exec_from(gateway.cancel(discovered.jobId))
      refreshed <- exec_from(_required(
        gateway.findByReviewId(reviewId),
        s"review job: ${reviewId.value}"
      ))
      _ <- exec_from(_same_job(discovered, refreshed))
      result <- refreshed.terminalLease match {
        case Some(lease) =>
          _settle(Some(lease)).flatMap(_ => exec_from(_admission(refreshed)))
        case None =>
          exec_from(_cancelling(refreshed, requestedAt))
      }
    } yield result

  private def _submit_owner(
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[ReviewRunAdmission] =
    for {
      binding <- exec_from(_binding(owner, execution))
      admission <- _submit_or_recover(owner, execution, binding, gateway)
    } yield admission

  private def _submit_or_recover(
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution,
    binding: CarReviewProductionJobBinding,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[ReviewRunAdmission] =
    gateway.submit(binding, execution) match {
      case Consequence.Success(jobid) =>
        exec_from(_queued(execution.plan.request, jobid))
      case Consequence.Failure(primary) =>
        _recover_submit_failure(primary, owner, execution, gateway)
    }

  private def _recover_submit_failure(
    primary: Conclusion,
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[ReviewRunAdmission] =
    gateway.findByReviewReuse(
      owner.reviewId,
      execution.plan.reuseKey.definitionId,
      execution.plan.reuseKey.digest
    ) match {
      case Consequence.Success(Some(discovered)) =>
        _validate_discovered(owner.diagnosisId, owner.reviewId, execution.plan, discovered) match {
          case Consequence.Success(_) => _settle_discovered_without_retry(discovered)
          case Consequence.Failure(_) => exec_from(Consequence.Failure(primary))
        }
      case Consequence.Success(None) =>
        _record_submit_failure(primary, owner, execution)
      case Consequence.Failure(_) =>
        exec_from(Consequence.Failure(primary))
    }

  private def _record_submit_failure(
    primary: Conclusion,
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution
  ): ExecUowM[ReviewRunAdmission] =
    _submit_failure_run(execution.plan) match {
      case Consequence.Success((rundocument, completedat)) =>
        _return_primary_after_cleanup(
          primary,
          _diagnosis.recordTerminal(
            owner,
            execution.plan,
            CarReviewDiagnosisTerminalState.Failed,
            rundocument,
            completedat
          )
        )
      case Consequence.Failure(_) =>
        exec_from(Consequence.Failure(primary))
    }

  private def _join_or_retry(
    joined: CarReviewDiagnosisAdmission.Joined,
    execution: CarReviewProductionExecution,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[ReviewRunAdmission] =
    _lookup_joined(joined, execution, gateway).flatMap { discovered =>
      discovered.terminalLease match {
        case None => exec_from(_admission(discovered))
        case Some(lease: CarReviewProductionTerminalLease.Completed) =>
          _diagnosis.completeFromJob(lease).flatMap(_ => exec_from(_admission(discovered)))
        case Some(lease: CarReviewProductionTerminalLease.Failed) =>
          _diagnosis.recordTerminalFromJob(lease).flatMap(_ => _readmit_once(execution, gateway))
        case Some(lease: CarReviewProductionTerminalLease.Cancelled) =>
          _diagnosis.recordTerminalFromJob(lease).flatMap(_ => _readmit_once(execution, gateway))
      }
    }

  private def _readmit_once(
    execution: CarReviewProductionExecution,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[ReviewRunAdmission] =
    _diagnosis.admit(execution.plan).flatMap {
      case owner: CarReviewDiagnosisAdmission.Owner =>
        _submit_owner(owner, execution, gateway)
      case joined: CarReviewDiagnosisAdmission.Joined =>
        _joined_successor(joined, execution, gateway)
      case reused: CarReviewDiagnosisAdmission.Reused =>
        _reuse_completed(reused, execution, gateway)
    }

  private def _joined_successor(
    joined: CarReviewDiagnosisAdmission.Joined,
    execution: CarReviewProductionExecution,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[ReviewRunAdmission] =
    _lookup_joined(joined, execution, gateway).flatMap(_settle_discovered_without_retry)

  private def _reuse_completed(
    reused: CarReviewDiagnosisAdmission.Reused,
    execution: CarReviewProductionExecution,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[ReviewRunAdmission] =
    for {
      discovered <- exec_from(_required(
        gateway.findByReviewReuse(
          reused.reviewId,
          execution.plan.reuseKey.definitionId,
          execution.plan.reuseKey.digest
        ),
        "review-job-reused-missing"
      ))
      lease <- exec_from(_reused_completed_lease(reused, execution.plan, discovered))
      _ <- _diagnosis.completeFromJob(lease)
      admission <- exec_from(_admission(discovered))
    } yield admission

  private def _settle(
    lease: Option[CarReviewProductionTerminalLease]
  ): ExecUowM[Unit] =
    lease match {
      case None => exec_from(Consequence.unit)
      case Some(value: CarReviewProductionTerminalLease.Completed) =>
        _diagnosis.completeFromJob(value).map(_ => ())
      case Some(value: CarReviewProductionTerminalLease.Failed) =>
        _diagnosis.recordTerminalFromJob(value)
      case Some(value: CarReviewProductionTerminalLease.Cancelled) =>
        _diagnosis.recordTerminalFromJob(value)
    }

  private def _gateway: Consequence[CarReviewProductionJobPort] =
    _injected_gateway.map(Consequence.success).getOrElse(
      core.component
        .map(component => Consequence.success(new CncfCarReviewJobGateway(component.jobEngine)))
        .getOrElse(Consequence.operationInvalid("CNCF component context is required for Review Job execution."))
    )

  private def _binding(
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution
  ): Consequence[CarReviewProductionJobBinding] =
    CarReviewProductionJobBinding.from(owner.diagnosisId, execution).fold(
      Consequence.operationInvalid,
      Consequence.success
    )

  private def _required(
    result: Consequence[Option[CarReviewDiscoveredProductionJob]],
    missing: String
  ): Consequence[CarReviewDiscoveredProductionJob] =
    result.flatMap(_.fold[Consequence[CarReviewDiscoveredProductionJob]](
      Consequence.operationNotFound(missing)
    )(Consequence.success))

  private def _lookup_joined(
    joined: CarReviewDiagnosisAdmission.Joined,
    execution: CarReviewProductionExecution,
    gateway: CarReviewProductionJobPort
  ): ExecUowM[CarReviewDiscoveredProductionJob] =
    gateway.findByReviewReuse(
      joined.reviewId,
      execution.plan.reuseKey.definitionId,
      execution.plan.reuseKey.digest
    ) match {
      case Consequence.Success(Some(discovered)) =>
        exec_from(_validate_discovered(joined.diagnosisId, joined.reviewId, execution.plan, discovered))
          .map(_ => discovered)
      case Consequence.Success(None) =>
        exec_from(Consequence.serviceUnavailable("review-job-submission-pending"))
      case Consequence.Failure(conclusion) =>
        exec_from(Consequence.Failure[CarReviewDiscoveredProductionJob](conclusion))
    }

  private def _queued(
    request: ReviewStartRequest,
    jobid: ReviewJobId
  ): Consequence[ReviewRunAdmission] =
    for {
      admitted <- _admitted_lifecycle(CarReviewRunLifecycle.admitted(
        request.reviewId,
        request.target,
        request.profile,
        request.startedAt
      ))
      queued <- _run_lifecycle(CarReviewRunLifecycle.projectJob(
        admitted,
        ReviewRunJobUpdate(JobStatus.Submitted, request.startedAt)
      ))
    } yield ReviewRunAdmission(queued, ReviewRunJobBinding(queued.reviewId, jobid))

  private def _validate_discovered(
    diagnosisid: String,
    reviewid: ReviewId,
    plan: CarReviewExecutionPlan,
    discovered: CarReviewDiscoveredProductionJob
  ): Consequence[Unit] =
    if discovered.binding.diagnosisId == diagnosisid &&
        discovered.binding.reviewId == reviewid &&
        discovered.binding.reuseKeyDefinition == plan.reuseKey.definitionId &&
        discovered.binding.reuseKeyDigest == plan.reuseKey.digest &&
        discovered.binding.target == plan.request.target &&
        discovered.binding.profile == plan.request.profile then
      Consequence.unit
    else
      Consequence.operationInvalid("review-job-joined-binding-mismatch")

  private def _reused_completed_lease(
    reused: CarReviewDiagnosisAdmission.Reused,
    plan: CarReviewExecutionPlan,
    discovered: CarReviewDiscoveredProductionJob
  ): Consequence[CarReviewProductionTerminalLease.Completed] =
    discovered.terminalLease match {
      case Some(lease: CarReviewProductionTerminalLease.Completed)
          if _validate_discovered(reused.diagnosisId, reused.reviewId, plan, discovered).isSuccess &&
            lease.jobId == discovered.jobId && lease.binding == discovered.binding &&
            lease.run == discovered.run &&
            discovered.run.state == ReviewRunState("completed") &&
            discovered.run.reportId.contains(reused.reportId) &&
            discovered.run.reportDigest.contains(reused.reportDigest) &&
            lease.response.report.reportId == reused.reportId &&
            lease.response.report.reportDigest == reused.reportDigest =>
        Consequence.success(lease)
      case _ =>
        Consequence.operationInvalid("review-job-reused-completion-mismatch")
    }

  /** Applies a discovered exact Job without starting another successor. */
  private def _settle_discovered_without_retry(
    discovered: CarReviewDiscoveredProductionJob
  ): ExecUowM[ReviewRunAdmission] =
    discovered.terminalLease match {
      case None => exec_from(_admission(discovered))
      case Some(lease: CarReviewProductionTerminalLease.Completed) =>
        _diagnosis.completeFromJob(lease).flatMap(_ => exec_from(_admission(discovered)))
      case Some(lease: CarReviewProductionTerminalLease.Failed) =>
        _diagnosis.recordTerminalFromJob(lease).flatMap(_ => exec_from(_job_terminal_failure(discovered)))
      case Some(lease: CarReviewProductionTerminalLease.Cancelled) =>
        _diagnosis.recordTerminalFromJob(lease).flatMap(_ => exec_from(_job_terminal_failure(discovered)))
    }

  private def _job_terminal_failure(
    discovered: CarReviewDiscoveredProductionJob
  ): Consequence[ReviewRunAdmission] =
    discovered.run.state.value match {
      case "failed" =>
        Consequence.operationInvalid(
          discovered.run.failureCode.map(_.value).getOrElse("cncf-job-failed")
        )
      case "cancelled" =>
        Consequence.operationInvalid("review-job-cancelled")
      case _ =>
        Consequence.operationInvalid("review-job-terminal-state-invalid")
    }

  private def _submit_failure_run(
    plan: CarReviewExecutionPlan
  ): Consequence[(String, ReviewInstant)] =
    _admitted_lifecycle(CarReviewRunLifecycle.admitted(
      plan.request.reviewId,
      plan.request.target,
      plan.request.profile,
      plan.request.startedAt
    )).flatMap { admitted =>
      _run_lifecycle(CarReviewRunLifecycle.projectJob(
        admitted,
        ReviewRunJobUpdate(JobStatus.Submitted, plan.request.startedAt)
      )).flatMap { submitted =>
        val completedat = _submit_failure_completed_at(plan.request.startedAt)
        _run_lifecycle(CarReviewRunLifecycle.projectJob(
          submitted,
          ReviewRunJobUpdate(
            JobStatus.Failed,
            completedat,
            failureCode = Some(ReviewFailureCode("review-job-submit-failed"))
          )
        )).flatMap { failed =>
          CarReviewRunCodec.encode(failed).fold(
            error => Consequence.operationInvalid(error.code),
            document => Consequence.success(document -> completedat)
          )
        }
      }
    }

  private def _submit_failure_completed_at(startedat: ReviewInstant): ReviewInstant = {
    val now = core.executionContext.clock.instant()
    val start = scala.util.Try(Instant.parse(startedat.value)).getOrElse(now)
    ReviewInstant((if (now.isBefore(start)) start else now).toString)
  }

  /**
   * Cleanup is intentionally attempted after a reliable missing reconciliation.
   * Its consequence must never replace the original submit conclusion.
   */
  private def _return_primary_after_cleanup(
    primary: Conclusion,
    cleanup: ExecUowM[Unit]
  ): ExecUowM[ReviewRunAdmission] = {
    // Entity-operation failures live in the outer Free foldMap layer; capture
    // both layers through this action's current runtime/UOW before returning
    // the submit failure to the enclosing dynamic action program.
    val cleanupresult: Consequence[Unit] =
      cleanup.value
        .foldMap(core.executionContext.unitOfWorkInterpreter)
        .flatMap(identity)
    cleanupresult match {
      case Consequence.Success(_) =>
        exec_from(Consequence.Failure[ReviewRunAdmission](primary))
      case Consequence.Failure(cleanupfailure) =>
        // Cleanup is preceding bounded diagnostic context; submit remains
        // the final/effective protocol-authoritative conclusion.
        exec_from(Consequence.Failure[ReviewRunAdmission](cleanupfailure ++ primary))
    }
  }

  private def _admission(
    discovered: CarReviewDiscoveredProductionJob
  ): Consequence[ReviewRunAdmission] =
    if discovered.run.reviewId == discovered.binding.reviewId then
      Consequence.success(ReviewRunAdmission(
        discovered.run,
        ReviewRunJobBinding(discovered.run.reviewId, discovered.jobId)
      ))
    else
      Consequence.operationInvalid("review-job-discovered-run-mismatch")

  private def _same_job(
    original: CarReviewDiscoveredProductionJob,
    refreshed: CarReviewDiscoveredProductionJob
  ): Consequence[Unit] =
    if original.jobId == refreshed.jobId && original.binding == refreshed.binding then
      Consequence.unit
    else
      Consequence.operationInvalid("review-job-cancel-rediscovery-mismatch")

  private def _cancelling(
    discovered: CarReviewDiscoveredProductionJob,
    requestedat: ReviewInstant
  ): Consequence[ReviewRunAdmission] =
    _run_lifecycle(CarReviewRunLifecycle.requestCancellation(discovered.run, requestedat)).flatMap { run =>
      if run.reviewId == discovered.run.reviewId then
        Consequence.success(ReviewRunAdmission(run, ReviewRunJobBinding(run.reviewId, discovered.jobId)))
      else
        Consequence.operationInvalid("review-job-cancellation-run-mismatch")
    }

  private def _admitted_lifecycle(
    result: Either[CarReviewCodecFailure, CarReviewRun]
  ): Consequence[CarReviewRun] =
    result.fold(error => Consequence.operationInvalid(error.code), Consequence.success)

  private def _run_lifecycle(
    result: Either[ReviewRunLifecycleFailure, CarReviewRun]
  ): Consequence[CarReviewRun] =
    result.fold(error => Consequence.operationInvalid(error.code), Consequence.success)
}
