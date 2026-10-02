package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 *  version Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** A closed recorded human decision; P10-34 owns any later applicability state. */
private[runtime] enum InternalModelCandidateHumanApprovalDecision(val token: String) {
  case Approved extends InternalModelCandidateHumanApprovalDecision("approved")
  case Rejected extends InternalModelCandidateHumanApprovalDecision("rejected")
  case ChangesRequested extends InternalModelCandidateHumanApprovalDecision("changes-requested")
}

private[runtime] object InternalModelCandidateHumanApprovalDecision {
  def fromToken(token: String): Option[InternalModelCandidateHumanApprovalDecision] = values.find(_.token == token)
}

private[runtime] final case class InternalModelCandidateHumanApprovalBasis(
  candidateArtifactReference: InternalModelArtifactReference,
  candidateReference: InternalModelRecordReference,
  candidateModelIdentity: String,
  reviewArtifactReference: InternalModelArtifactReference,
  reviewReference: InternalModelRecordReference,
  subject: InternalModelReviewSubject,
  scope: InternalModelSemanticScope,
  semanticDiffArtifactReference: InternalModelArtifactReference,
  semanticDiffReference: InternalModelRecordReference
)

private[runtime] final case class InternalModelCandidateHumanApprovalInput(
  actor: InternalModelDecisionActor,
  approvalReference: InternalModelRecordReference,
  basis: InternalModelCandidateHumanApprovalBasis,
  decision: InternalModelCandidateHumanApprovalDecision,
  provenance: InternalModelSemanticSource,
  rationale: String,
  unresolvedItems: Vector[String]
)

private[runtime] final case class InternalModelCandidateHumanApproval(
  approval: InternalModelCandidateHumanApprovalInput,
  profile: String,
  schemaVersion: String
)

private[runtime] final case class InternalModelVerifiedCandidateHumanApprovalPackage(
  reviewPackage: InternalModelVerifiedCandidateReviewBindingPackage,
  approvalArtifact: InternalModelVerifiedProjection
)

private[runtime] final case class InternalModelCandidateHumanApprovalAdmission(
  record: InternalModelCandidateHumanApproval,
  approvalArtifactReference: InternalModelArtifactReference,
  approvalArtifactPackageRelativePath: String,
  reviewAdmission: InternalModelCandidateReviewBindingAdmission
)
