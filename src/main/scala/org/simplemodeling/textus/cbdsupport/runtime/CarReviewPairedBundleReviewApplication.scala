package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * CBD application boundary for a local or CI client that supplies multiple
 * already-produced provider bundles. It admits documents only; neither a
 * workspace path nor a provider command is accepted at this boundary.
 */
final case class SuppliedReviewBundleSet(
  template: CarReviewReport,
  bundles: Vector[SuppliedProviderBundleSubmission]
)

final class CarReviewPairedBundleReviewApplication(
  suppliedBundleApplication: CarReviewSuppliedBundleApplication = new CarReviewSuppliedBundleApplication(),
  canonicalResponseApplication: CarReviewCanonicalResponseApplication = new CarReviewCanonicalResponseApplication()
) {
  def submit(
    submission: SuppliedReviewBundleSet,
    actorroles: Set[String]
  ): Consequence[CarReviewCanonicalResponse] =
    for {
      _ <- CarReviewAuthorization.authorize("review.submit-bundle", actorroles)
      _ <- _validate_submission(submission)
      admitted <- _admit(submission, actorroles)
      response <- canonicalResponseApplication.build(submission.template, admitted, actorroles)
    } yield response

  private def _validate_submission(submission: SuppliedReviewBundleSet): Consequence[Unit] =
    if submission.bundles.isEmpty then
      Consequence.operationInvalid("canonical review response requires at least one supplied provider bundle")
    else if submission.bundles.forall(value => value.reviewId == submission.template.reviewId && value.target == submission.template.target) then
      Consequence.unit
    else
      Consequence.operationInvalid("supplied provider bundle review or target differs from the canonical report template")

  private def _admit(
    submission: SuppliedReviewBundleSet,
    actorroles: Set[String]
  ): Consequence[Vector[AdmittedProviderBundleInput]] =
    submission.bundles.foldLeft(Consequence.success(Vector.empty[AdmittedProviderBundleInput])) { (z, value) =>
      for {
        xs <- z
        outcome <- suppliedBundleApplication.submit(value, actorroles)
        admitted <- _admitted(outcome, value.bundle)
      } yield xs :+ admitted
    }

  private def _admitted(
    outcome: ProviderBundleAdmissionOutcome,
    bundle: String
  ): Consequence[AdmittedProviderBundleInput] =
    outcome match {
      case ProviderBundleAdmissionOutcome.Admitted(value) => Consequence.success(AdmittedProviderBundleInput(value, bundle))
      case ProviderBundleAdmissionOutcome.Refused(value) =>
        Consequence.operationInvalid(s"provider bundle was not admitted: ${value.limitation.code}")
    }
}
