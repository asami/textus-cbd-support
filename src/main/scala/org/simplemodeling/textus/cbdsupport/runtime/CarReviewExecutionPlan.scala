package org.simplemodeling.textus.cbdsupport.runtime

import org.simplemodeling.model.datatype.EntityId

/*
 * @since   Jul. 23, 2026
 * @version Oct. 16, 2026
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
final case class CarReviewExecutionPlanReference(diagnosisId: EntityId, revision: Long)

final case class CarReviewExecutionPlan(
  request: ReviewStartRequest,
  reference: CarReviewExecutionPlanReference,
  reuseInput: CarReviewReuseKeyInput
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
    val reference: CarReviewExecutionPlanReference
  ) extends CarReviewDiagnosisAdmission {
    private[cbdsupport] def isOwnerFor(
      diagnosisid: String,
      plan: CarReviewExecutionPlan
    ): Boolean =
      diagnosisId == diagnosisid &&
        reviewId == plan.request.reviewId &&
        reference == plan.reference
  }

  object Owner {
    private[cbdsupport] def issue(
      diagnosisid: String,
      reviewid: ReviewId,
      reference: CarReviewExecutionPlanReference
    ): Owner =
      new Owner(diagnosisid, reviewid, reference)
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
    reference: CarReviewExecutionPlanReference,
    reuseInput: CarReviewReuseKeyInput
  ): Either[CarReviewExecutionPlanFailure, CarReviewExecutionPlan] =
    for {
      _ <- Either.cond(
        reference != null && reference.diagnosisId != null && reference.revision > 0,
        (), CarReviewExecutionPlanFailure("review-plan-reference-invalid", "A declared native plan identity and positive revision are required.")
      )
      _ <- CarReviewReuseKey.validate(reuseInput).left.map(error => CarReviewExecutionPlanFailure(error.code, error.message))
      _ <- Either.cond(
        request.target.kind == reuseInput.targetKind && request.target.organization == reuseInput.organization &&
          request.target.name == reuseInput.name && request.target.version == reuseInput.version,
        (), CarReviewExecutionPlanFailure("review-plan-target-mismatch", "Selected target semantics must match the admitted Review request.")
      )
      _ <- Either.cond(
        request.profile == reuseInput.profile,
        (), CarReviewExecutionPlanFailure("review-plan-profile-mismatch", "Selected profile must match the admitted Review request.")
      )
    } yield CarReviewExecutionPlan(request, reference, reuseInput)
}
