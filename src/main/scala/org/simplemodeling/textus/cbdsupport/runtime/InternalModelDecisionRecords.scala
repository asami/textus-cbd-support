package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
/** Immutable retained decision account; admission remains a separate read-only operation. */
private[runtime] final case class InternalModelDecisionActor(
  kind: String,
  identity: String,
  role: String
)

private[runtime] final case class InternalModelDecisionChoice(
  choiceIdentity: String,
  description: String
)

private[runtime] enum InternalModelDecisionState(val token: String) {
  case Accepted extends InternalModelDecisionState("accepted")
  case Superseded extends InternalModelDecisionState("superseded")
}

private[runtime] object InternalModelDecisionState {
  def fromToken(token: String): Option[InternalModelDecisionState] = values.find(_.token == token)
}

private[runtime] enum InternalModelDecisionEvidenceKind(val token: String) {
  case RealizationSource extends InternalModelDecisionEvidenceKind("realization-source")
  case ExternalHuman extends InternalModelDecisionEvidenceKind("external-human")
  case ProviderProposal extends InternalModelDecisionEvidenceKind("provider-proposal")
  case ExternalOther extends InternalModelDecisionEvidenceKind("external-other")
}

private[runtime] object InternalModelDecisionEvidenceKind {
  def fromToken(token: String): Option[InternalModelDecisionEvidenceKind] = values.find(_.token == token)
}

private[runtime] enum InternalModelDecisionBasisStatus(val token: String) {
  case Current extends InternalModelDecisionBasisStatus("current")
  case HistoricalUnverified extends InternalModelDecisionBasisStatus("historical-unverified")
}

private[runtime] object InternalModelDecisionBasisStatus {
  def fromToken(token: String): Option[InternalModelDecisionBasisStatus] = values.find(_.token == token)
}

private[runtime] final case class InternalModelDecisionEvidence(
  evidenceIdentity: String,
  kind: InternalModelDecisionEvidenceKind,
  source: InternalModelSemanticSource,
  sourceReferenceId: Option[String],
  conditionIds: Vector[String],
  conditions: Vector[String],
  limitations: Vector[String]
)

private[runtime] final case class InternalModelDecisionAlternative(
  alternativeIdentity: String,
  description: String,
  rejectionRationale: String
)

private[runtime] final case class InternalModelDecisionBasis(
  realizationArtifactId: String,
  realizationIdentity: String,
  sha256: String,
  scope: InternalModelSemanticScope,
  status: InternalModelDecisionBasisStatus
)

private[runtime] final case class InternalModelDecisionRecord(
  decisionIdentity: String,
  topicIdentity: String,
  state: InternalModelDecisionState,
  actor: InternalModelDecisionActor,
  provenance: InternalModelSemanticSource,
  selectedChoice: InternalModelDecisionChoice,
  rationale: String,
  affectedTargets: Vector[InternalModelSemanticTarget],
  consideredEvidence: Vector[InternalModelDecisionEvidence],
  assumptions: Vector[String],
  conditions: Vector[String],
  limitations: Vector[String],
  realizationConditionIds: Vector[String],
  rejectedAlternatives: Vector[InternalModelDecisionAlternative],
  basis: InternalModelDecisionBasis,
  supersedes: Option[String]
)

private[runtime] final case class InternalModelDecisionLedger(
  profile: String,
  schemaVersion: String,
  ledgerIdentity: String,
  scope: InternalModelSemanticScope,
  records: Vector[InternalModelDecisionRecord],
  canonicalBytes: Vector[Byte]
)

private[runtime] final case class InternalModelDecisionAdmission(
  ledger: InternalModelDecisionLedger,
  realization: InternalModelSemanticRealization
)
