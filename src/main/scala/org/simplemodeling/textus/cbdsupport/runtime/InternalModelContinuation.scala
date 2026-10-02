package org.simplemodeling.textus.cbdsupport.runtime

/**
 * Serialized cursor fields retain their schema names and producer-declared references.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
private[runtime] final case class InternalModelResumeCondition(
  code: String,
  detail: String,
  artifactId: Option[String]
)

private[runtime] final case class InternalModelResumeCursor(
  schemaVersion: String,
  profile: String,
  packageId: InternalModelPackageId,
  packageRevision: Long,
  selectedArtifacts: Vector[InternalModelArtifactReference],
  currentStage: String,
  lastCompletedAction: Option[String],
  nextPermittedAction: Option[String],
  blockers: Vector[InternalModelResumeCondition],
  preconditions: Vector[InternalModelResumeCondition],
  invalidationChecks: Vector[InternalModelResumeCondition],
  acceptanceCriteria: Vector[InternalModelResumeCondition]
)

/** Captured inventory evidence; non-core bytes remain raw and semantically unadmitted. */
private[runtime] final case class InternalModelVerifiedContinuationArtifact(
  context: InternalModelVerifiedArtifactContext,
  bytes: Option[Vector[Byte]]
)

private[runtime] final case class InternalModelVerifiedContinuationPackage(
  packageContext: InternalModelVerifiedPackageContext,
  continuityPackage: InternalModelVerifiedProjectionContinuityPackage,
  artifacts: Vector[InternalModelVerifiedContinuationArtifact]
)

/** Existing continuity semantics plus their exact captured carrier, with no action permission. */
private[runtime] final case class InternalModelRehydratedState(
  packageContext: InternalModelVerifiedPackageContext,
  cursor: InternalModelResumeCursor,
  continuity: InternalModelProjectionContinuity,
  artifacts: Vector[InternalModelVerifiedContinuationArtifact]
)
