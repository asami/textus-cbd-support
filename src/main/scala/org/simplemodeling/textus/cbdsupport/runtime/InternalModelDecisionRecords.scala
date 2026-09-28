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

private[runtime] final case class InternalModelDecisionEvidence(
  evidenceIdentity: String,
  kind: String,
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
  status: String
)

private[runtime] final case class InternalModelDecisionRecord(
  decisionIdentity: String,
  topicIdentity: String,
  state: String,
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
