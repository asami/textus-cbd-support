package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.job.JobStatus

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ReviewRunState(value: String)
final case class ReviewFailureCode(value: String)
final case class ReviewJobId(value: String)

final case class CarReviewRun(
  schemaVersion: ReviewSchemaVersion,
  documentType: ReviewDocumentType,
  reviewId: ReviewId,
  target: ReviewTarget,
  profile: ReviewProfile,
  state: ReviewRunState,
  providers: Vector[ReviewProviderExecution],
  limitations: Vector[ReviewLimitation],
  startedAt: ReviewInstant,
  updatedAt: ReviewInstant,
  completedAt: Option[ReviewInstant],
  reportId: Option[ReviewReportId],
  reportDigest: Option[ReviewDigest],
  failureCode: Option[ReviewFailureCode]
)

final case class ReviewRunCompletion(
  reportId: ReviewReportId,
  reportDigest: ReviewDigest
)

final case class ReviewRunJobUpdate(
  status: JobStatus,
  updatedAt: ReviewInstant,
  completion: Option[ReviewRunCompletion] = None,
  failureCode: Option[ReviewFailureCode] = None,
  providers: Option[Vector[ReviewProviderExecution]] = None,
  limitations: Vector[ReviewLimitation] = Vector.empty
)

final case class ReviewRunJobBinding(
  reviewId: ReviewId,
  jobId: ReviewJobId
)

final case class ReviewRunLifecycleFailure(
  code: String,
  state: ReviewRunState,
  message: String
)

object CarReviewRunVocabulary {
  val DOCUMENT_TYPE = "review-run"
  val STATES: Set[String] = Set(
    "admitted", "queued", "running", "cancelling", "cancelled", "completed", "failed"
  )
  val TERMINAL_STATES: Set[String] = Set("cancelled", "completed", "failed")
}
