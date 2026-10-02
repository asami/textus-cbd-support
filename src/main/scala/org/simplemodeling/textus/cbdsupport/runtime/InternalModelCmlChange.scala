package org.simplemodeling.textus.cbdsupport.runtime

/**
 * Explicit caller-owned CML change evidence; no serialization or permission token.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] enum InternalModelCmlChangeEligibility {
  case EligibleForSkillApplication, ReReviewRequired, Incomplete, Inconsistent, Blocked
}

private[runtime] final case class InternalModelCmlMutationTarget(
  targetid: String,
  projectrelativepath: String,
  sourceauthority: String,
  sourceidentity: String,
  nextsourcerevision: String
)

private[runtime] final case class InternalModelCmlMutationAuthority(
  requestreference: InternalModelRecordReference,
  projectroot: String,
  packagereference: InternalModelPackageReference,
  scope: InternalModelSemanticScope,
  provenance: InternalModelSemanticSource,
  targets: Vector[InternalModelCmlMutationTarget]
)

private[runtime] final case class InternalModelCmlChangeRequest(
  currentrequest: InternalModelContinuationRequest,
  originalapproval: InternalModelCandidateHumanApprovalAdmission,
  originalsnapshots: Vector[InternalModelVerifiedSourceSnapshot],
  currentreview: InternalModelCandidateReviewBindingAdmission,
  livesources: Map[InternalModelArtifactReference, InternalModelPackageFreshnessInput],
  supersession: Option[InternalModelCandidateApprovalSupersessionInput],
  mutationauthority: Option[InternalModelCmlMutationAuthority]
)

private[runtime] enum InternalModelCmlChangeProblemKind {
  case MissingAuthority, MissingExecutionBasis, MissingApprovedHandoff
  case OriginalDecisionBlocked, SupersededApproval
  case CurrentReviewMismatch, SemanticBasisChanged, UnknownSourceReference, AuthorityRootMismatch
  case AuthorityPackageMismatch, AuthorityScopeMismatch, AuthorityTargetMismatch
  case AuthorityOwnerMismatch, MissingNextSourceRevision, NextSourceRevisionMismatch
}

private[runtime] final case class InternalModelCmlChangeProblem(
  kind: InternalModelCmlChangeProblemKind,
  eligibility: InternalModelCmlChangeEligibility,
  dimension: String,
  artifactreference: Option[InternalModelArtifactReference],
  targetid: Option[String],
  diagnostic: String
)

/** Complete ordinary target payload and explicit version supplied by the source owner. */
private[runtime] final case class InternalModelCmlApplicationTarget(
  target: InternalModelCandidateCmlTarget,
  payload: InternalModelCandidateCmlTargetBytes,
  nextsourcerevision: String
)

/** Point-in-time application input: Slice B must reevaluate immediately before use. */
private[runtime] final case class InternalModelCmlApplicationPlan(
  authority: InternalModelCmlMutationAuthority,
  packagereference: InternalModelPackageReference,
  scope: InternalModelSemanticScope,
  subject: InternalModelReviewSubject,
  candidateartifactreference: InternalModelArtifactReference,
  candidatereference: InternalModelRecordReference,
  candidatemodelidentity: String,
  realizationartifactreference: InternalModelArtifactReference,
  realizationreference: InternalModelRecordReference,
  continuityartifactreference: InternalModelArtifactReference,
  continuityreference: InternalModelRecordReference,
  semanticdiffartifactreference: InternalModelArtifactReference,
  semanticdiffreference: InternalModelRecordReference,
  reviewartifactreference: InternalModelArtifactReference,
  reviewreference: InternalModelRecordReference,
  approvalartifactreference: InternalModelArtifactReference,
  approvalreference: InternalModelRecordReference,
  actualapproval: InternalModelCandidateHumanApprovalAdmission,
  targets: Vector[InternalModelCmlApplicationTarget]
)

/** Every original source and every owner report survives deterministic precedence. */
private[runtime] final case class InternalModelCmlChangeReport(
  eligibility: InternalModelCmlChangeEligibility,
  originalapproval: InternalModelCandidateHumanApprovalAdmission,
  currentreview: InternalModelCandidateReviewBindingAdmission,
  supersession: Option[InternalModelCandidateApprovalSupersessionInput],
  continuation: InternalModelContinuationReport,
  lifecycle: Option[InternalModelCandidateApprovalLifecycleReport],
  livesources: Vector[InternalModelPackageFreshnessEntry],
  problems: Vector[InternalModelCmlChangeProblem],
  plan: Option[InternalModelCmlApplicationPlan]
)
