package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Supplies a CBD-owned response template after the client has identified one Review and Target. */
trait CarReviewCanonicalTemplateProvider {
  def template(reviewId: ReviewId, target: ReviewTarget): Consequence[CarReviewReport]
}

/**
 * Public application boundary for local/CI provider documents. Its payload has
 * no report template, workspace path, process command, or source reference;
 * CBD chooses canonical report policy through its template provider.
 */
final case class SuppliedProviderBundleSet(
  bundles: Vector[SuppliedProviderBundleSubmission]
)

final class CarReviewProviderDocumentSubmissionApplication(
  templateProvider: CarReviewCanonicalTemplateProvider,
  pairedBundleApplication: CarReviewPairedBundleReviewApplication = new CarReviewPairedBundleReviewApplication()
) {
  def submit(
    submission: SuppliedProviderBundleSet,
    actorroles: Set[String]
  ): Consequence[CarReviewCanonicalResponse] =
    for {
      _ <- CarReviewAuthorization.authorize("review.submit-bundle", actorroles)
      binding <- _binding(submission)
      template <- templateProvider.template(binding._1, binding._2)
      response <- pairedBundleApplication.submit(SuppliedReviewBundleSet(template, submission.bundles), actorroles)
    } yield response

  private def _binding(submission: SuppliedProviderBundleSet): Consequence[(ReviewId, ReviewTarget)] =
    submission.bundles.headOption match {
      case Some(first) if submission.bundles.forall(value => value.reviewId == first.reviewId && value.target == first.target) =>
        Consequence.success(first.reviewId -> first.target)
      case Some(_) =>
        Consequence.operationInvalid("supplied provider documents must identify one Review and Target")
      case None =>
        Consequence.operationInvalid("submitted provider document set cannot be empty")
    }
}
