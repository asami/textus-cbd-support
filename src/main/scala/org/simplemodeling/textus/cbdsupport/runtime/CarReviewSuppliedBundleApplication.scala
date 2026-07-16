package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * CBD-owned admission boundary for evidence that a local/CI client already
 * produced. The submission intentionally has no path, command, or workspace
 * reference: server-side code can admit bounded provider documents, but cannot
 * acquire arbitrary client source files through this surface.
 */
final case class SuppliedProviderBundleSubmission(
  reviewId: ReviewId,
  target: ReviewTarget,
  availability: ProviderBundleAvailability,
  descriptor: String,
  providerRequest: String,
  bundle: String
)

final class CarReviewSuppliedBundleApplication {
  import CarReviewSuppliedBundleApplication.MAX_DOCUMENT_BYTES

  def submit(
    submission: SuppliedProviderBundleSubmission,
    actorroles: Set[String]
  ): Consequence[ProviderBundleAdmissionOutcome] =
    for {
      _ <- CarReviewAuthorization.authorize("review.submit-bundle", actorroles)
      _ <- _bounded(submission)
    } yield CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(
      submission.reviewId,
      submission.target,
      submission.availability,
      submission.descriptor,
      submission.providerRequest,
      submission.bundle
    ))

  private def _bounded(submission: SuppliedProviderBundleSubmission): Consequence[Unit] = {
    val sizes = Vector(submission.descriptor, submission.providerRequest, submission.bundle)
      .map(_.getBytes(StandardCharsets.UTF_8).length.toLong)
    if (sizes.forall(_ <= MAX_DOCUMENT_BYTES)) Consequence.unit
    else Consequence.operationInvalid(s"review provider document exceeds $MAX_DOCUMENT_BYTES bytes")
  }
}

object CarReviewSuppliedBundleApplication {
  val MAX_DOCUMENT_BYTES = 16L * 1024L * 1024L
}
