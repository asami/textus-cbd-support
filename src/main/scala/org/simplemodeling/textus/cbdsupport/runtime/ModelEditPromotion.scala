package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import org.goldenport.Consequence

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Connection to the accepted Phase10 owner; it creates no decision or authority. */
private[runtime] object ModelEditPromotion {
  /** Inputs belong to the independently executing review, actual human and source owner. */
  final case class Inputs(executionBasis: InternalModelCandidateReviewExecutionBasis,
    humanInput: InternalModelCandidateHumanApprovalInput, approvalArtifact: InternalModelArtifactReference,
    authority: InternalModelCmlMutationAuthority,
    liveSources: Map[InternalModelArtifactReference, InternalModelPackageFreshnessInput])

  def admitAndApply(projectRoot: Path, prepared: ModelEditConfirmation.Prepared, inputs: Inputs,
    applicationReference: InternalModelRecordReference): Consequence[InternalModelCmlChangeApplication.ApplicationReport] =
    InternalModelCandidateHumanApprovalValidator.validate(projectRoot, inputs.approvalArtifact,
      prepared.reviewArtifact, inputs.executionBasis, inputs.humanInput).flatMap { approval =>
      InternalModelPackageValidator.verifiedSourceSnapshots(projectRoot).flatMap { snapshots =>
        val review = approval.reviewAdmission
        val candidate = review.semanticDiffAdmission.candidateAdmission
        val required = candidate.projection.targets.flatMap(target => target.mappings.map(mapping =>
          InternalModelContinuationMappingRequirement(target.targetId, mapping.mappingId,
            InternalModelSemanticTarget(mapping.semanticIdentityKind, mapping.semanticIdentity))))
        val continuation = InternalModelContinuationRequest(review.carrierPackageContext.reference,
          review.carrierPackageContext.revision, review.binding.realizationReference, review.binding.scope,
          Some(review.binding.candidateArtifactReference), Some(review.binding.semanticDiffArtifactReference),
          Some(prepared.reviewArtifact), Some(inputs.approvalArtifact), Some(inputs.executionBasis),
          Some(inputs.humanInput), Some(Vector.empty), Some(required))
        applyApproved(projectRoot, prepared, InternalModelCmlChangeRequest(continuation, approval,
          snapshots, review, inputs.liveSources, None, Some(inputs.authority)), applicationReference)
      }
    }

  def applyApproved(projectRoot: Path, prepared: ModelEditConfirmation.Prepared,
    request: InternalModelCmlChangeRequest, applicationReference: InternalModelRecordReference): Consequence[InternalModelCmlChangeApplication.ApplicationReport] = {
    val review = request.currentreview
    val human = request.originalapproval.record.approval
    val exact = review.binding.subject == prepared.subject && review.reviewArtifactReference == prepared.reviewArtifact &&
      review.binding.reviewReference == prepared.reviewReference && review.binding.candidateReference == prepared.session.candidate.reference &&
      review.semanticDiffAdmission.diff == prepared.diff.diff && human.basis.subject == prepared.subject &&
      human.basis.reviewArtifactReference == prepared.reviewArtifact && human.basis.reviewReference == prepared.reviewReference
    if (!exact) Consequence.operationInvalid("Model Edit promotion requires independently admitted review and actual-human input for the exact confirmed subject")
    else InternalModelCmlChangeApplication.applyApproved(projectRoot, request, applicationReference)
  }

  /** The caller retains the actual application while executing required validation and source-owner refresh. */
  def validateApplied(projectRoot: Path, application: InternalModelCmlChangeApplication.ApplicationReport,
    postValidationReference: InternalModelRecordReference,
    commands: Vector[InternalModelCmlChangePostValidation.CommandObservation],
    owner: Option[InternalModelCmlChangePostValidation.OwnerEvidence]): Consequence[InternalModelCmlChangePostValidation.Report] =
    InternalModelCmlChangePostValidation.validate(projectRoot,
      InternalModelCmlChangePostValidation.Request(application, postValidationReference, commands, owner))
}
