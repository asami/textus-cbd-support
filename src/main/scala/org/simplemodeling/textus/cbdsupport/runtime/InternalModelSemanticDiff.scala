package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 * @version Oct.  1, 2026
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

/** Closed persisted content; the captured package inventory remains external. */
private[runtime] final case class InternalModelSemanticDiff(
  candidateArtifactReference: InternalModelArtifactReference,
  candidateReference: InternalModelRecordReference,
  candidateModelIdentity: String,
  profile: String,
  schemaVersion: String,
  scope: InternalModelSemanticScope,
  semanticDiffReference: InternalModelRecordReference,
  targets: Vector[InternalModelSemanticDiffTarget]
)

/** Captured candidate/diff context admitted without reopening mutable paths. */
private[runtime] final case class InternalModelSemanticDiffAdmission(
  diff: InternalModelSemanticDiff,
  diffArtifactReference: InternalModelArtifactReference,
  diffPackageRelativePath: String,
  candidateAdmission: InternalModelCandidateCmlAdmission
)
