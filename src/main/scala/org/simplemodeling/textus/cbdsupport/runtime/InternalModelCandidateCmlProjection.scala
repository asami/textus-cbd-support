package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] final case class InternalModelCandidateCmlContent(
  byteLength: Long,
  rawBytesBase64: String,
  sha256: String
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
  baselineArtifactId: String,
  effects: Vector[InternalModelCandidateCmlEffect],
  mappings: Vector[InternalModelCandidateCmlMapping],
  patchIdentity: String,
  projectRelativePath: String,
  proposedContent: InternalModelCandidateCmlContent,
  source: InternalModelSemanticSource,
  targetId: String
)

private[runtime] final case class InternalModelCandidateCmlProjection(
  candidateIdentity: String,
  candidateModelIdentity: String,
  candidateRevision: Int,
  continuityArtifactId: String,
  profile: String,
  realizationArtifactId: String,
  schemaVersion: String,
  scope: InternalModelSemanticScope,
  targets: Vector[InternalModelCandidateCmlTarget],
  canonicalBytes: Vector[Byte]
)

private[runtime] final case class InternalModelCandidateCmlBaseline(
  artifactId: String,
  projectRelativePath: String,
  source: InternalModelSemanticSource,
  rawBytes: Vector[Byte],
  snapshotSha256: String
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
  candidateArtifactId: String,
  candidateArtifactSha256: String,
  candidatePackageRelativePath: String,
  targetBytes: Vector[InternalModelCandidateCmlTargetBytes]
)
