package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 23, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewExecutionPlanFailure(code: String, message: String)

/** Non-success terminal outcomes retained as history, never as Report reuse. */
final case class CarReviewDiagnosisTerminalState private (value: String)

object CarReviewDiagnosisTerminalState {
  val Failed = CarReviewDiagnosisTerminalState("failed")
  val Cancelled = CarReviewDiagnosisTerminalState("cancelled")
  val Expired = CarReviewDiagnosisTerminalState("expired")
  val Incompatible = CarReviewDiagnosisTerminalState("incompatible")

  def parse(value: String): Either[CarReviewExecutionPlanFailure, CarReviewDiagnosisTerminalState] =
    Vector(Failed, Cancelled, Expired, Incompatible).find(_.value == value.trim.toLowerCase(java.util.Locale.ROOT))
      .toRight(CarReviewExecutionPlanFailure("review-terminal-state-invalid", "Only failed, cancelled, expired, or incompatible are non-success terminal diagnosis states."))
}

/**
 * Server-owned admission input for a Review execution.
 *
 * The plan freezes every conclusion-affecting selection before provider work
 * starts.  It is deliberately separate from the P5 transport request, which
 * carries only a target and profile and therefore cannot safely claim a
 * reusable diagnosis.
 */
final case class CarReviewExecutionPlan(
  request: ReviewStartRequest,
  reuseInput: CarReviewReuseKeyInput,
  reuseKey: CarReviewReuseKey
)

sealed trait CarReviewDiagnosisAdmission {
  def diagnosisId: String
}

object CarReviewDiagnosisAdmission {
  /**
   * Internal owner lease.  Only the claim path can issue it, and completion or
   * terminal retention must present the exact lease rather than reconstructing
   * ownership from a transport plan.
   */
  final class Owner private[cbdsupport] (
    val diagnosisId: String,
    val reviewId: ReviewId,
    private val _reuse_key_digest: ReviewDigest
  ) extends CarReviewDiagnosisAdmission {
    private[cbdsupport] def isOwnerFor(
      diagnosisid: String,
      plan: CarReviewExecutionPlan
    ): Boolean =
      diagnosisId == diagnosisid &&
        reviewId == plan.request.reviewId &&
        _reuse_key_digest == plan.reuseKey.digest
  }

  object Owner {
    private[cbdsupport] def issue(
      diagnosisid: String,
      reviewid: ReviewId,
      reusekeydigest: ReviewDigest
    ): Owner =
      new Owner(diagnosisid, reviewid, reusekeydigest)
  }

  final case class Joined(
    diagnosisId: String,
    reviewId: ReviewId
  ) extends CarReviewDiagnosisAdmission

  final case class Reused(
    diagnosisId: String,
    reviewId: ReviewId,
    reportId: ReviewReportId,
    reportDigest: ReviewDigest
  ) extends CarReviewDiagnosisAdmission
}

object CarReviewExecutionPlan {
  def create(
    request: ReviewStartRequest,
    reuseinput: CarReviewReuseKeyInput
  ): Either[CarReviewExecutionPlanFailure, CarReviewExecutionPlan] =
    for {
      _ <- Either.cond(
        request.target == reuseinput.target,
        (),
        CarReviewExecutionPlanFailure(
          "review-plan-target-mismatch",
          "The execution plan target must exactly match the admitted Review request."
        )
      )
      _ <- Either.cond(
        request.profile == reuseinput.profile,
        (),
        CarReviewExecutionPlanFailure(
          "review-plan-profile-mismatch",
          "The execution plan profile must exactly match the admitted Review request."
        )
      )
      reusekey <- CarReviewReuseKey.calculate(reuseinput).left.map(error =>
        CarReviewExecutionPlanFailure(error.code, error.message)
      )
    } yield CarReviewExecutionPlan(request, reuseinput, reusekey)
}
