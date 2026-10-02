package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 *  version Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Point-in-time applicability states derived from an immutable human decision. */
private[runtime] enum InternalModelCandidateApprovalLifecycleState(val token: String) {
  case Approved extends InternalModelCandidateApprovalLifecycleState("approved")
  case Rejected extends InternalModelCandidateApprovalLifecycleState("rejected")
  case ChangesRequested extends InternalModelCandidateApprovalLifecycleState("changes-requested")
  case Superseded extends InternalModelCandidateApprovalLifecycleState("superseded")
  case Invalidated extends InternalModelCandidateApprovalLifecycleState("invalidated")
}

/** One portable, explicit replacement link; its bytes exclude local selection metadata. */
private[runtime] final case class InternalModelCandidateApprovalSupersession(
  predecessorApproval: InternalModelArtifactReference,
  successorApproval: InternalModelArtifactReference,
  profile: String,
  schemaVersion: String
)

/** A link and the separately admitted successor record that it names. */
private[runtime] final case class InternalModelCandidateApprovalSupersessionInput(
  record: InternalModelCandidateApprovalSupersession,
  successorApproval: InternalModelCandidateHumanApprovalAdmission
)

/** The closed non-applicability vocabulary; concrete differences remain lossless below. */
private[runtime] enum InternalModelCandidateApprovalInvalidationKind {
  case CandidateBasisChanged
  case ReviewBasisChanged
  case SemanticDiffBasisChanged
  case ReviewSubjectChanged
  case ScopeChanged
  case RulesChanged
  case ProvidersChanged
  case SourceChanged
  case SourceIncomplete
  case SourceUnavailable
  case SourceUnauthorized
  case SourceMalformed
  case SourceAmbiguousOrConflicting
  case MissingBaseline
}

/** One deterministic reason with its optional affected source artifact and exact dimensions. */
private[runtime] final case class InternalModelCandidateApprovalInvalidation(
  kind: InternalModelCandidateApprovalInvalidationKind,
  artifactReference: Option[InternalModelArtifactReference],
  changedDimensionNames: Vector[String],
  missingDimensionNames: Vector[String]
)

/** Immutable applicability result retaining the original decision and every supplied evidence link. */
private[runtime] final case class InternalModelCandidateApprovalLifecycleReport(
  state: InternalModelCandidateApprovalLifecycleState,
  approval: InternalModelCandidateHumanApprovalAdmission,
  invalidations: Vector[InternalModelCandidateApprovalInvalidation],
  supersession: Option[InternalModelCandidateApprovalSupersessionInput]
)
