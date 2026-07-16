package org.simplemodeling.textus.cbdsupport.runtime

import java.time.Instant

import org.goldenport.cncf.job.JobStatus

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
object CarReviewRunLifecycle {
  def admitted(
    reviewid: ReviewId,
    target: ReviewTarget,
    profile: ReviewProfile,
    startedat: ReviewInstant,
    providers: Vector[ReviewProviderExecution] = Vector.empty,
    limitations: Vector[ReviewLimitation] = Vector.empty
  ): Either[CarReviewCodecFailure, CarReviewRun] = {
    val run = CarReviewRun(
      ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      ReviewDocumentType(CarReviewRunVocabulary.DOCUMENT_TYPE),
      reviewid,
      target,
      profile,
      ReviewRunState("admitted"),
      providers,
      limitations,
      startedat,
      startedat,
      None,
      None,
      None,
      None
    )
    CarReviewRunCodec.validate(run).map(_ => run)
  }

  def requestCancellation(
    run: CarReviewRun,
    updatedat: ReviewInstant
  ): Either[ReviewRunLifecycleFailure, CarReviewRun] =
    for {
      _ <- _fresh_update(run, updatedat)
      _ <- _state_allowed(run, Set("queued", "running"), "cancellation-not-allowed")
      changed = run.copy(state = ReviewRunState("cancelling"), updatedAt = updatedat)
      _ <- _valid(changed)
    } yield changed

  def projectJob(
    run: CarReviewRun,
    update: ReviewRunJobUpdate
  ): Either[ReviewRunLifecycleFailure, CarReviewRun] =
    for {
      _ <- _fresh_update(run, update.updatedAt)
      _ <- _update_shape(update, run.state)
      changed <- if (CarReviewRunVocabulary.TERMINAL_STATES.contains(run.state.value))
        _terminal_replay(run, update)
      else
        _project_job(run, update)
      _ <- _valid(changed)
    } yield changed

  private def _terminal_replay(
    run: CarReviewRun,
    update: ReviewRunJobUpdate
  ): Either[ReviewRunLifecycleFailure, CarReviewRun] = {
    val unchangedpayload = update.providers.isEmpty && update.limitations.isEmpty
    val matching = run.state.value match {
      case "cancelled" =>
        update.status == JobStatus.Cancelled && update.completion.isEmpty && update.failureCode.isEmpty
      case "completed" =>
        val samecompletion = (run.reportId, run.reportDigest, update.completion) match {
          case (Some(reportid), Some(reportdigest), Some(completion)) =>
            completion == ReviewRunCompletion(reportid, reportdigest)
          case _ => false
        }
        update.status == JobStatus.Succeeded && update.failureCode.isEmpty && samecompletion
      case "failed" if run.failureCode.contains(ReviewFailureCode("review-report-missing")) =>
        update.status == JobStatus.Succeeded && update.completion.isEmpty && update.failureCode.isEmpty
      case "failed" =>
        update.status == JobStatus.Failed && update.completion.isEmpty &&
          update.failureCode.forall(run.failureCode.contains)
      case _ => false
    }
    Either.cond(
      unchangedpayload && matching,
      run,
      _failure("terminal-run", run.state, "A terminal Review Run is immutable; only identical Job readback is admitted.")
    )
  }

  private def _project_job(
    run: CarReviewRun,
    update: ReviewRunJobUpdate
  ): Either[ReviewRunLifecycleFailure, CarReviewRun] = {
    val providers = update.providers.getOrElse(run.providers)
    val limitations = _merge_limitations(run.limitations, update.limitations)
    val changed = update.status match {
      case JobStatus.Submitted =>
        _transition(run, ReviewRunState("queued"), update.updatedAt).map(
          _.copy(providers = providers, limitations = limitations)
        )
      case JobStatus.Running =>
        val state = if (run.state.value == "cancelling") run.state else ReviewRunState("running")
        _transition(run, state, update.updatedAt).map(_.copy(providers = providers, limitations = limitations))
      case JobStatus.Suspended =>
        val state = if (run.state.value == "cancelling") run.state else ReviewRunState("running")
        val suspended = _merge_limitations(limitations, Vector(_suspended_limitation(run.reviewId)))
        _transition(run, state, update.updatedAt).map(_.copy(providers = providers, limitations = suspended))
      case JobStatus.Cancelled =>
        _terminal(run, ReviewRunState("cancelled"), update.updatedAt).map(
          _.copy(providers = providers, limitations = limitations)
        )
      case JobStatus.Succeeded =>
        update.completion match {
          case Some(completion) =>
            _terminal(run, ReviewRunState("completed"), update.updatedAt).map(_.copy(
              providers = providers,
              limitations = limitations,
              reportId = Some(completion.reportId),
              reportDigest = Some(completion.reportDigest)
            ))
          case None =>
            val missing = _merge_limitations(limitations, Vector(_missing_report_limitation(run.reviewId)))
            _terminal(run, ReviewRunState("failed"), update.updatedAt).map(_.copy(
              providers = providers,
              limitations = missing,
              failureCode = Some(ReviewFailureCode("review-report-missing"))
            ))
        }
      case JobStatus.Failed =>
        val failurecode = update.failureCode.getOrElse(ReviewFailureCode("cncf-job-failed"))
        val failed = if (update.limitations.nonEmpty) limitations
          else _merge_limitations(limitations, Vector(_job_failure_limitation(run.reviewId)))
        _terminal(run, ReviewRunState("failed"), update.updatedAt).map(_.copy(
          providers = providers,
          limitations = failed,
          failureCode = Some(failurecode)
        ))
    }
    changed
  }

  private def _transition(
    run: CarReviewRun,
    target: ReviewRunState,
    updatedat: ReviewInstant
  ): Either[ReviewRunLifecycleFailure, CarReviewRun] = {
    val allowed = run.state.value match {
      case "admitted" => Set("queued")
      case "queued" => Set("queued", "running", "cancelled", "failed")
      case "running" => Set("running", "cancelled", "completed", "failed")
      case "cancelling" => Set("cancelling", "cancelled", "failed")
      case state if CarReviewRunVocabulary.TERMINAL_STATES.contains(state) => Set(state)
      case _ => Set.empty[String]
    }
    if (!allowed.contains(target.value))
      Left(_failure("invalid-transition", run.state, s"Review Run cannot transition to ${target.value}."))
    else if (CarReviewRunVocabulary.TERMINAL_STATES.contains(run.state.value))
      Right(run)
    else
      Right(run.copy(state = target, updatedAt = updatedat))
  }

  private def _terminal(
    run: CarReviewRun,
    target: ReviewRunState,
    updatedat: ReviewInstant
  ): Either[ReviewRunLifecycleFailure, CarReviewRun] =
    _transition(run, target, updatedat).flatMap { changed =>
      if (CarReviewRunVocabulary.TERMINAL_STATES.contains(run.state.value))
        Right(run)
      else
        Right(changed.copy(completedAt = Some(updatedat)))
    }

  private def _fresh_update(
    run: CarReviewRun,
    updatedat: ReviewInstant
  ): Either[ReviewRunLifecycleFailure, Unit] =
    (_instant(run.updatedAt), _instant(updatedat)) match {
      case (Some(previous), Some(next)) if !next.isBefore(previous) => Right(())
      case (Some(_), Some(_)) => Left(_failure("stale-update", run.state, "Review Run update time regressed."))
      case _ => Left(_failure("invalid-instant", run.state, "Review Run update time is invalid."))
    }

  private def _update_shape(
    update: ReviewRunJobUpdate,
    state: ReviewRunState
  ): Either[ReviewRunLifecycleFailure, Unit] =
    if (update.status != JobStatus.Succeeded && update.completion.nonEmpty)
      Left(_failure("unexpected-completion", state, "Only a succeeded CNCF Job may supply report completion."))
    else if (update.status != JobStatus.Failed && update.failureCode.nonEmpty)
      Left(_failure("unexpected-failure", state, "Only a failed CNCF Job may supply a failure code."))
    else
      Right(())

  private def _state_allowed(
    run: CarReviewRun,
    allowed: Set[String],
    code: String
  ): Either[ReviewRunLifecycleFailure, Unit] =
    Either.cond(
      allowed.contains(run.state.value),
      (),
      _failure(code, run.state, "Review Run state does not admit this operation.")
    )

  private def _valid(run: CarReviewRun): Either[ReviewRunLifecycleFailure, Unit] =
    CarReviewRunCodec.validate(run).left.map(error =>
      _failure(error.code, run.state, s"${error.path}: ${error.message}")
    )

  private def _merge_limitations(
    current: Vector[ReviewLimitation],
    additions: Vector[ReviewLimitation]
  ): Vector[ReviewLimitation] =
    (current ++ additions).distinctBy(limitation => (
      limitation.code,
      limitation.scope,
      limitation.subjectId,
      limitation.message
    ))

  private def _suspended_limitation(reviewid: ReviewId): ReviewLimitation =
    ReviewLimitation(
      "cncf-job-suspended",
      ReviewLimitationScope("run"),
      Some(reviewid.value),
      "The CNCF Job is suspended; Review progress is paused without a terminal conclusion.",
      retryable = true
    )

  private def _missing_report_limitation(reviewid: ReviewId): ReviewLimitation =
    ReviewLimitation(
      "review-report-missing",
      ReviewLimitationScope("run"),
      Some(reviewid.value),
      "The CNCF Job succeeded without a canonical Review Report identity.",
      retryable = false
    )

  private def _job_failure_limitation(reviewid: ReviewId): ReviewLimitation =
    ReviewLimitation(
      "cncf-job-failed",
      ReviewLimitationScope("run"),
      Some(reviewid.value),
      "The CNCF Job failed before producing a canonical Review Report.",
      retryable = false
    )

  private def _instant(value: ReviewInstant): Option[Instant] =
    try Some(Instant.parse(value.value))
    catch {
      case _: RuntimeException => None
    }

  private def _failure(
    code: String,
    state: ReviewRunState,
    message: String
  ): ReviewRunLifecycleFailure =
    ReviewRunLifecycleFailure(code, state, message)
}
