package org.simplemodeling.textus.cbdsupport.runtime

import scala.util.control.NonFatal
import org.goldenport.Consequence
import org.simplemodeling.textus.cbdsupport.entity.ReviewDiagnosis

/*
 * @since   Oct.  6, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewServerProviderCall(
  selection: CarReviewReuseProviderSelection,
  request: ProviderBundleExecutionRequest
)

/** Explicit server selections and an Entity-issued owner, never a transport key. */
final case class CarReviewJobExecutionWork(
  plan: CarReviewExecutionPlan,
  owner: CarReviewDiagnosisAdmission.Owner,
  template: CarReviewReport,
  providers: Vector[CarReviewServerProviderCall]
) {
  def validate: Consequence[Unit] =
    try {
      if plan == null || owner == null || template == null || providers == null ||
          plan.request == null || plan.request.reviewId == null || plan.request.startedAt == null then
        Consequence.operationInvalid("review-job-work-incomplete")
      else CarReviewExecutionPlan.create(plan.request, plan.reference, plan.reuseInput) match {
        case Left(error) => Consequence.operationInvalid(error.code)
        case Right(_) => _validate_bindings
      }
    } catch {
      case NonFatal(_) => Consequence.operationInvalid("review-job-work-incomplete")
    }

  private def _validate_bindings: Consequence[Unit] = {
    val declared = plan.reuseInput.providerSelections
    if plan.reference.diagnosisId.collection != ReviewDiagnosis.collectionId ||
        owner.reference != plan.reference || owner.diagnosisId != plan.reference.diagnosisId.print ||
        !owner.isOwnerFor(plan.reference.diagnosisId.print, plan) then
      Consequence.operationInvalid("review-job-owner-mismatch")
    else if providers.isEmpty || providers.size > 8 ||
        providers.exists(call => call == null || call.selection == null || call.request == null) then
      Consequence.operationInvalid("review-job-provider-selection-invalid")
    else if providers.map(_.selection.provider.id).distinct.size != providers.size ||
        providers.map(_.selection).toSet != declared.toSet then
      Consequence.operationInvalid("review-job-provider-selection-mismatch")
    else if template.reviewId != plan.request.reviewId || template.profile != plan.request.profile ||
        !_same_target(template.target, plan.request.target) then
      Consequence.operationInvalid("review-job-template-binding-mismatch")
    else if template.schemaVersion.value != CarReviewVocabulary.SCHEMA_VERSION ||
        template.documentType.value != "review-report" || template.baseline.nonEmpty ||
        plan.reuseInput.baselineReference.nonEmpty || template.assessments.size != 1 then
      Consequence.operationInvalid("review-job-canonical-template-unsupported")
    else if !plan.reuseInput.policyBindings.exists(binding => binding.scope == "gate" &&
        binding.policyId == template.gate.policyId && binding.policyVersion == template.gate.policyVersion) then
      Consequence.operationInvalid("review-job-gate-policy-mismatch")
    else providers.foldLeft(Consequence.unit) { (result, call) =>
      result.flatMap { _ =>
        val request = call.request
        if request.reviewId != plan.request.reviewId || !_same_target(request.target, plan.request.target) ||
            request.provider != call.selection.provider || !plan.reuseInput.ruleSets.contains(call.selection.ruleSet) then
          Consequence.operationInvalid("review-job-provider-binding-mismatch")
        else CarReviewProviderBundleAdmission.describeDescriptor(request.descriptor) match {
          case Right(descriptor) if descriptor.provider == call.selection.provider && descriptor.ruleSet == call.selection.ruleSet =>
            Consequence.unit
          case _ => Consequence.operationInvalid("review-job-provider-descriptor-mismatch")
        }
      }
    }
  }

  private def _same_target(left: ReviewTarget, right: ReviewTarget): Boolean =
    left != null && right != null && left.kind == right.kind && left.organization == right.organization &&
      left.name == right.name && left.version == right.version
}
