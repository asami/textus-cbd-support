package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 28, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Immutable retained open-issue account; admission remains a separate read-only operation. */
private[runtime] enum InternalModelOpenIssueState(val wireValue: String) {
  case Open extends InternalModelOpenIssueState("open")
}

private[runtime] enum InternalModelOpenIssueEvidenceKind(val wireValue: String) {
  case RealizationSource extends InternalModelOpenIssueEvidenceKind("realization-source")
  case ExternalHuman extends InternalModelOpenIssueEvidenceKind("external-human")
  case ProviderProposal extends InternalModelOpenIssueEvidenceKind("provider-proposal")
  case ExternalOther extends InternalModelOpenIssueEvidenceKind("external-other")
}

private[runtime] final case class InternalModelOpenIssueBasis(
  realizationArtifactReference: InternalModelArtifactReference,
  realizationReference: InternalModelRecordReference
)

private[runtime] final case class InternalModelOpenIssueBlocking(
  semanticApproval: Boolean,
  cmlProjection: Boolean,
  application: Boolean,
  validation: Boolean
)

private[runtime] final case class InternalModelOpenIssueEvidence(
  evidenceIdentity: String,
  kind: InternalModelOpenIssueEvidenceKind,
  source: InternalModelSemanticSource,
  sourceReferenceId: Option[String],
  conditionIds: Vector[String],
  conditions: Vector[String],
  limitations: Vector[String]
)

private[runtime] final case class InternalModelOpenIssueOption(
  optionIdentity: String,
  description: String,
  evidenceIds: Vector[String],
  assumptions: Vector[String],
  conditions: Vector[String],
  limitations: Vector[String]
)

private[runtime] final case class InternalModelOpenIssueRecord(
  issueReference: InternalModelRecordReference,
  state: InternalModelOpenIssueState,
  question: String,
  decisionRole: String,
  ownerIdentity: Option[String],
  impact: String,
  affectedTargets: Vector[InternalModelSemanticTarget],
  consideredEvidence: Vector[InternalModelOpenIssueEvidence],
  options: Vector[InternalModelOpenIssueOption],
  assumptions: Vector[String],
  conditions: Vector[String],
  limitations: Vector[String],
  realizationConditionIds: Vector[String],
  blocking: InternalModelOpenIssueBlocking
)

private[runtime] final case class InternalModelOpenIssueLedger(
  profile: String,
  schemaVersion: String,
  ledgerReference: InternalModelRecordReference,
  scope: InternalModelSemanticScope,
  basis: InternalModelOpenIssueBasis,
  issues: Vector[InternalModelOpenIssueRecord]
)

private[runtime] final case class InternalModelOpenIssueAdmission(
  ledger: InternalModelOpenIssueLedger,
  realization: InternalModelSemanticRealization
)
