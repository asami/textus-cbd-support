package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] final case class InternalModelCandidateCmlContent(
  contentReference: InternalModelRecordReference,
  byteLength: Long,
  rawBytesBase64: String
)

private[runtime] final case class InternalModelCandidateCmlMapping(
  canonicalAssertionIds: Vector[String],
  cmlAnchor: String,
  cmlSemanticIdentity: String,
  conditionIds: Vector[String],
  enrichmentAssertionIds: Vector[String],
  mappingId: String,
  semanticIdentity: String,
  semanticIdentityKind: String
)

private[runtime] final case class InternalModelCandidateCmlEffect(
  assessment: String,
  detail: String,
  effectId: String,
  kind: String,
  mappingIds: Vector[String],
  sourceReferenceId: String
)

private[runtime] final case class InternalModelCandidateCmlTarget(
  baselineArtifactReference: InternalModelArtifactReference,
  effects: Vector[InternalModelCandidateCmlEffect],
  mappings: Vector[InternalModelCandidateCmlMapping],
  patchIdentity: String,
  projectRelativePath: String,
  proposedContent: InternalModelCandidateCmlContent,
  source: InternalModelSemanticSource,
  targetId: String
)

private[runtime] final case class InternalModelCandidateCmlProjection(
  candidateReference: InternalModelRecordReference,
  candidateModelIdentity: String,
  continuityArtifactReference: InternalModelArtifactReference,
  profile: String,
  realizationArtifactReference: InternalModelArtifactReference,
  schemaVersion: String,
  scope: InternalModelSemanticScope,
  targets: Vector[InternalModelCandidateCmlTarget]
)

private[runtime] final case class InternalModelCandidateCmlBaseline(
  reference: InternalModelArtifactReference,
  projectRelativePath: String,
  source: InternalModelSemanticSource,
  rawBytes: Vector[Byte]
)

private[runtime] final case class InternalModelCandidateCmlTargetBytes(
  targetId: String,
  baseline: InternalModelCandidateCmlBaseline,
  proposedRawBytes: Vector[Byte]
)

private[runtime] final case class InternalModelCandidateCmlAdmission(
  projection: InternalModelCandidateCmlProjection,
  continuity: InternalModelProjectionContinuity,
  packageContext: InternalModelVerifiedPackageContext,
  candidateArtifactReference: InternalModelArtifactReference,
  candidatePackageRelativePath: String,
  targetBytes: Vector[InternalModelCandidateCmlTargetBytes]
)
