package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
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

private[runtime] final case class InternalModelCandidateHumanApprovalPackageBasis(
  packageDigest: String,
  packageId: String,
  projectId: String,
  projectNamespace: String,
  revision: Long,
  schemaVersion: String
)

private[runtime] final case class InternalModelCandidateHumanApprovalBasis(
  candidateArtifact: InternalModelCandidateReviewArtifact,
  candidateIdentity: String,
  candidateModelIdentity: String,
  candidateRevision: Int,
  reviewArtifact: InternalModelCandidateReviewArtifact,
  reviewIdentity: String,
  reviewRevision: Int,
  reviewedPackage: InternalModelCandidateHumanApprovalPackageBasis,
  scope: InternalModelSemanticScope,
  semanticDiffArtifact: InternalModelCandidateReviewArtifact,
  semanticDiffIdentity: String,
  semanticDiffRevision: Int
)

private[runtime] final case class InternalModelCandidateHumanApprovalInput(
  actor: InternalModelDecisionActor,
  approvalIdentity: String,
  approvalRevision: Int,
  basis: InternalModelCandidateHumanApprovalBasis,
  decision: InternalModelCandidateHumanApprovalDecision,
  provenance: InternalModelSemanticSource,
  rationale: String,
  unresolvedItems: Vector[String]
)

private[runtime] final case class InternalModelCandidateHumanApproval(
  approval: InternalModelCandidateHumanApprovalInput,
  profile: String,
  schemaVersion: String,
  canonicalBytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedCandidateHumanApprovalPackage(
  reviewPackage: InternalModelVerifiedCandidateReviewBindingPackage,
  approvalArtifact: InternalModelVerifiedProjection
)

private[runtime] final case class InternalModelCandidateHumanApprovalAdmission(
  record: InternalModelCandidateHumanApproval,
  approvalArtifactId: String,
  approvalArtifactSha256: String,
  approvalArtifactPackageRelativePath: String,
  reviewAdmission: InternalModelCandidateReviewBindingAdmission
)
