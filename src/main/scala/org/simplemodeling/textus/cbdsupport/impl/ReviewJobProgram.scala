package org.simplemodeling.textus.cbdsupport.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, ActionCallEntityStorePart}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.EntityQuery
import org.goldenport.cncf.job.JobStatus
import org.goldenport.cncf.unitofwork.ExecUowM
import org.simplemodeling.model.directive.Condition
import org.simplemodeling.textus.cbdsupport.entity.{ReviewJobBinding, ReviewReportSnapshot}
import org.simplemodeling.textus.cbdsupport.entity.create.{ReviewJobBinding as BindingCreate}
import org.simplemodeling.textus.cbdsupport.entity.query.{ReviewJobBinding as BindingQuery, ReviewReportSnapshot as ReportQuery}
import org.simplemodeling.textus.cbdsupport.runtime.*
import org.simplemodeling.textus.cbdsupport.value.{ReviewId as EntityReviewId, ReviewJobId as EntityReviewJobId,
  ReviewSubmissionDocument as EntityDocument}

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Durable application wiring; native Entity ownership and Job staging own admission. */
private[cbdsupport] final class ReviewJobProgram(
  val core: ActionCall.Core,
  factory: ComponentFactory
) extends ActionCall.Core.Holder with ActionCallEntityStorePart {
  private given org.goldenport.cncf.context.ExecutionContext = core.executionContext
  private val _gateway = new CncfCarReviewJobGateway(core.component.get.jobEngine)

  def start(selected: CarReviewServerExecution): ExecUowM[ReviewRunAdmission] =
    for {
      admission <- factory._admit_and_start_review_execution(core, selected.plan) { owner =>
        for {
          jobid <- exec_from(_gateway.stageExecution(core,
            CarReviewJobExecutionWork(selected.plan, owner, selected.template, selected.providers),
            selected.application, factory, core.component.get.actionEngine))
          _ <- bind(selected.plan, jobid)
        } yield ()
      }
      result <- admission match {
        case owner: CarReviewDiagnosisAdmission.Owner => _binding(owner.reviewId).flatMap(binding => exec_from(_admission(binding)))
        case joined: CarReviewDiagnosisAdmission.Joined => get(joined.reviewId)
        case reused: CarReviewDiagnosisAdmission.Reused => get(reused.reviewId)
      }
    } yield result

  def bind(plan: CarReviewExecutionPlan, jobid: ReviewJobId): ExecUowM[Unit] =
    for {
      run <- exec_from(_result(CarReviewRunLifecycle.admitted(plan.request.reviewId, plan.request.target,
        plan.request.profile, plan.request.startedAt).left.map(_.code)))
      queued <- exec_from(_result(CarReviewRunLifecycle.projectJob(run,
        ReviewRunJobUpdate(JobStatus.Submitted, plan.request.startedAt)).left.map(_.code)))
      document <- exec_from(_result(CarReviewRunCodec.encode(queued).left.map(_.code)))
      candidate <- exec_from(BindingCreate.Builder()
        .withReview_id(EntityReviewId(plan.request.reviewId.value))
        .withJob_id(EntityReviewJobId(jobid.value))
        .withDiagnosis_id(plan.reference.diagnosisId)
        .withPlan_revision(plan.reference.revision)
        .withRun_document(EntityDocument(org.simplemodeling.model.value.ContentBody(document)))
        .buildC())
      _ <- entity_create_internal(candidate)
    } yield ()

  def get(reviewid: ReviewId): ExecUowM[ReviewRunAdmission] =
    for {
      _ <- exec_from(CarReviewAuthorization.authorize("review.read-run", CarReviewAuthorization.roles(core.executionContext)))
      binding <- _binding(reviewid)
      admitted <- exec_from(_admission(binding))
      update <- exec_from(_gateway.read(admitted.binding.jobId).flatMap(_.map(Consequence.success)
        .getOrElse(Consequence.operationNotFound("review-job-not-found"))))
      completed <- if (update.status == JobStatus.Succeeded && update.completion.isEmpty)
        _completion(binding).map(value => update.copy(completion = Some(value)))
      else exec_from(Consequence.success(update))
      projected <- exec_from(_result(CarReviewRunLifecycle.projectJob(admitted.run, completed).left.map(_.code)))
    } yield admitted.copy(run = projected)

  def cancel(reviewid: ReviewId): ExecUowM[ReviewRunAdmission] =
    for {
      _ <- exec_from(CarReviewAuthorization.authorize("review.cancel", CarReviewAuthorization.roles(core.executionContext)))
      current <- get(reviewid)
      _ <- exec_from(_gateway.cancel(current.binding.jobId))
      result <- get(reviewid)
    } yield result

  def authorizeReport(reviewid: ReviewId, jobid: ReviewJobId): ExecUowM[Unit] =
    for {
      binding <- _binding(reviewid)
      _ <- exec_from(if (binding.job_id.value == jobid.value) Consequence.unit
        else Consequence.operationInvalid("review-report-job-binding-invalid"))
      _ <- exec_from(_gateway.read(jobid).flatMap {
        case Some(update) if update.status == JobStatus.Succeeded => Consequence.unit
        case _ => Consequence.operationInvalid("review-report-job-not-completed")
      })
    } yield ()

  private def _binding(reviewid: ReviewId): ExecUowM[ReviewJobBinding] =
    entity_search_internal[ReviewJobBinding](EntityQuery(ReviewJobBinding.collectionId,
      Query(Query.Plan(BindingQuery.create().withReview_id(Condition.is(EntityReviewId(reviewid.value))), limit = Some(2)))))
      .flatMap(results => exec_from(results.data match {
        case Vector(binding) => Consequence.success(binding)
        case Vector() => Consequence.operationNotFound("review-job-binding-not-found")
        case _ => Consequence.operationInvalid("review-job-binding-not-unique")
      }))

  private def _admission(binding: ReviewJobBinding): Consequence[ReviewRunAdmission] =
    _result(CarReviewRunCodec.decode(binding.run_document.value.value).left.map(_.code)).flatMap { run =>
      if (run.reviewId.value != binding.review_id.value)
        Consequence.operationInvalid("review-job-binding-attribution-invalid")
      else Consequence.success(ReviewRunAdmission(run, ReviewRunJobBinding(run.reviewId, ReviewJobId(binding.job_id.value))))
    }

  private def _completion(binding: ReviewJobBinding): ExecUowM[ReviewRunCompletion] =
    entity_search_internal[ReviewReportSnapshot](EntityQuery(ReviewReportSnapshot.collectionId,
      Query(Query.Plan(ReportQuery.create().withReview_id(Condition.is(binding.review_id)), limit = Some(2)))))
      .flatMap(results => exec_from(results.data match {
        case Vector(snapshot) if snapshot.diagnosis_id == binding.diagnosis_id &&
            snapshot.job_id.exists(_.value == binding.job_id.value) =>
          Consequence.success(ReviewRunCompletion(ReviewReportId(snapshot.report_id.value), ReviewDigest(snapshot.report_digest.value)))
        case _ => Consequence.operationInvalid("review-job-completion-binding-invalid")
      }))

  private def _result[A](result: Either[String, A]): Consequence[A] =
    result.fold(Consequence.operationInvalid(_), Consequence.success)
}
