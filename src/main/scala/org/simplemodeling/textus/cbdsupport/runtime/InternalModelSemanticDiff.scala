package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** One explicit Phase 9 entry associated with a candidate mapping. */
private[runtime] final case class InternalModelSemanticDiffMappedEntry(
  entry: CandidateDesignSemanticDiffEntry,
  mappingId: String,
  semanticIdentityKind: String
)

/** One candidate target's exact patch trace and Phase 9 semantic-diff entries. */
private[runtime] final case class InternalModelSemanticDiffTarget(
  entries: Vector[InternalModelSemanticDiffMappedEntry],
  patchTrace: CandidateDesignProposedCmlPatchTrace,
  targetId: String
)

/** Closed persisted content; package inventory/hash context remains external. */
private[runtime] final case class InternalModelSemanticDiff(
  candidateArtifactId: String,
  candidateArtifactSha256: String,
  candidateIdentity: String,
  candidateModelIdentity: String,
  candidateRevision: Int,
  profile: String,
  schemaVersion: String,
  scope: InternalModelSemanticScope,
  semanticDiffIdentity: String,
  semanticDiffRevision: Int,
  targets: Vector[InternalModelSemanticDiffTarget],
  canonicalBytes: Vector[Byte]
)

/** Captured candidate/diff context admitted without reopening mutable paths. */
private[runtime] final case class InternalModelSemanticDiffAdmission(
  diff: InternalModelSemanticDiff,
  diffArtifactId: String,
  diffArtifactSha256: String,
  diffPackageRelativePath: String,
  candidateAdmission: InternalModelCandidateCmlAdmission
)
