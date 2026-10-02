package org.simplemodeling.textus.cbdsupport.runtime

/**
 * Point-in-time recorded-work eligibility vocabulary, without execution authority.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  2, 2026
 */
private[runtime] enum InternalModelContinuationStage(val token: String) {
  case ModelReady extends InternalModelContinuationStage("model-ready")
  case CandidateReady extends InternalModelContinuationStage("candidate-ready")
  case ReviewReady extends InternalModelContinuationStage("review-ready")
  case HumanDecisionRecorded extends InternalModelContinuationStage("human-decision-recorded")
}

private[runtime] enum InternalModelContinuationAction(val token: String) {
  case InspectProjections extends InternalModelContinuationAction("inspect-projections")
  case ReviewCandidate extends InternalModelContinuationAction("review-candidate")
  case RequestHumanDecision extends InternalModelContinuationAction("request-human-decision")
  case HandoffApprovedCandidate extends InternalModelContinuationAction("handoff-approved-candidate")
}

private[runtime] enum InternalModelContinuationCondition(val token: String, val blocker: Boolean) {
  case ModelAdmitted extends InternalModelContinuationCondition("model-admitted", false)
  case SourceVersionsKnown extends InternalModelContinuationCondition("source-versions-known", false)
  case DecisionsComplete extends InternalModelContinuationCondition("decisions-complete", false)
  case MappingsComplete extends InternalModelContinuationCondition("mappings-complete", false)
  case ReviewAdmitted extends InternalModelContinuationCondition("review-admitted", false)
  case HumanApproved extends InternalModelContinuationCondition("human-approved", false)
  case NoApprovalBlockingIssues extends InternalModelContinuationCondition("no-approval-blocking-issues", false)
  case ApprovalBlockingIssues extends InternalModelContinuationCondition("approval-blocking-issues", true)
  case HumanUnresolvedItems extends InternalModelContinuationCondition("human-unresolved-items", true)
}

private[runtime] enum InternalModelContinuationEligibility {
  case Eligible, Incomplete, Inconsistent
}

private[runtime] enum InternalModelContinuationProblemKind {
  case MissingPrerequisite, IdentityMismatch, RevisionMismatch, ScopeMismatch, SelectionMismatch
  case UnsupportedStage, UnsupportedAction, StageActionMismatch, UnsupportedCondition, ConditionOwnerMismatch
  case DecisionMismatch, MappingMismatch, SemanticAdmissionFailed, BlockingIssue, HumanDecisionNotApproved, HumanUnresolvedItems
}

private[runtime] enum InternalModelContinuationConditionList {
  case Preconditions, InvalidationChecks, AcceptanceCriteria, Blockers
}

private[runtime] final case class InternalModelContinuationConditionAttribution(
  origin: InternalModelContinuationConditionList,
  condition: InternalModelResumeCondition
)

private[runtime] final case class InternalModelContinuationProblem(
  kind: InternalModelContinuationProblemKind,
  dimension: String,
  artifactreference: Option[InternalModelArtifactReference],
  recordreference: Option[InternalModelRecordReference],
  diagnostic: String,
  attribution: Option[InternalModelContinuationConditionAttribution]
)

private[runtime] final case class InternalModelContinuationDecisionRequirement(
  artifactreference: InternalModelArtifactReference,
  recordreference: InternalModelRecordReference,
  topicidentity: String,
  choiceidentity: String,
  affectedtargets: Vector[InternalModelSemanticTarget]
)

private[runtime] final case class InternalModelContinuationMappingRequirement(
  targetid: String,
  mappingid: String,
  semantictarget: InternalModelSemanticTarget
)

/** None is unsupplied; Some(empty) is the independently supplied empty requirement set. */
private[runtime] final case class InternalModelContinuationRequest(
  packagereference: InternalModelPackageReference,
  carrierrevision: Long,
  realizationreference: InternalModelRecordReference,
  scope: InternalModelSemanticScope,
  candidateartifact: Option[InternalModelArtifactReference],
  semanticdiffartifact: Option[InternalModelArtifactReference],
  reviewartifact: Option[InternalModelArtifactReference],
  approvalartifact: Option[InternalModelArtifactReference],
  executionbasis: Option[InternalModelCandidateReviewExecutionBasis],
  humandecision: Option[InternalModelCandidateHumanApprovalInput],
  requireddecisions: Option[Vector[InternalModelContinuationDecisionRequirement]],
  requiredmappings: Option[Vector[InternalModelContinuationMappingRequirement]]
)

private[runtime] final case class InternalModelContinuationDecisionEvidence(
  artifactreference: InternalModelArtifactReference,
  admission: InternalModelDecisionAdmission
)

private[runtime] final case class InternalModelContinuationOpenIssueEvidence(
  artifactreference: InternalModelArtifactReference,
  admission: InternalModelOpenIssueAdmission
)

/** Retains complete successful semantic values and every missing/contradictory dimension. */
private[runtime] final case class InternalModelContinuationReport(
  eligibility: InternalModelContinuationEligibility,
  action: Option[InternalModelContinuationAction],
  state: InternalModelRehydratedState,
  candidate: Option[InternalModelCandidateCmlAdmission],
  semanticdiff: Option[InternalModelSemanticDiffAdmission],
  review: Option[InternalModelCandidateReviewBindingAdmission],
  approval: Option[InternalModelCandidateHumanApprovalAdmission],
  decisions: Vector[InternalModelContinuationDecisionEvidence],
  openissues: Vector[InternalModelContinuationOpenIssueEvidence],
  problems: Vector[InternalModelContinuationProblem]
)
