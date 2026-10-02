package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Independently caller-admitted rule execution basis. */
private[runtime] final case class InternalModelCandidateReviewRule(
  ruleId: String,
  ruleVersion: String
)

/** Independently caller-admitted provider execution basis. */
private[runtime] final case class InternalModelCandidateReviewProvider(
  providerId: String,
  providerVersion: String
)

/** The separate rule/provider basis that a caller admits for review validation. */
private[runtime] final case class InternalModelCandidateReviewExecutionBasis(
  rules: Vector[InternalModelCandidateReviewRule],
  providers: Vector[InternalModelCandidateReviewProvider]
)

/** One complete candidate target's attributable review evidence. */
private[runtime] final case class InternalModelCandidateReviewTarget(
  evidenceArtifactIds: Vector[String],
  reviewSnapshot: CandidateDesignReviewSnapshot,
  targetId: String
)

/** Complete immutable semantic basis, distinct from its current carrier. */
private[runtime] final case class InternalModelReviewSubject(
  subjectId: InternalModelRecordId,
  subjectRevision: InternalModelRecordRevision,
  packageReference: InternalModelPackageReference,
  scope: InternalModelSemanticScope,
  artifacts: Vector[InternalModelArtifactReference]
)

/** Closed, portable candidate-review evidence; review state grants no approval. */
private[runtime] final case class InternalModelCandidateReviewBinding(
  candidateArtifactReference: InternalModelArtifactReference,
  candidateReference: InternalModelRecordReference,
  candidateModelIdentity: String,
  continuityArtifactReference: InternalModelArtifactReference,
  evidenceArtifacts: Vector[InternalModelArtifactReference],
  profile: String,
  providers: Vector[InternalModelCandidateReviewProvider],
  realizationArtifactReference: InternalModelArtifactReference,
  realizationReference: InternalModelRecordReference,
  reviewReference: InternalModelRecordReference,
  rules: Vector[InternalModelCandidateReviewRule],
  schemaVersion: String,
  scope: InternalModelSemanticScope,
  semanticDiffArtifactReference: InternalModelArtifactReference,
  semanticDiffReference: InternalModelRecordReference,
  subject: InternalModelReviewSubject,
  targets: Vector[InternalModelCandidateReviewTarget]
)

/** Captured review artifact and its same-capture semantic-diff carrier context. */
private[runtime] final case class InternalModelVerifiedCandidateReviewBindingPackage(
  carrierPackageContext: InternalModelVerifiedPackageContext,
  semanticDiffPackage: InternalModelVerifiedSemanticDiffPackage,
  reviewArtifact: InternalModelVerifiedProjection
)

/** Successful review admission retains its exact selection and current capture. */
private[runtime] final case class InternalModelCandidateReviewBindingAdmission(
  binding: InternalModelCandidateReviewBinding,
  reviewArtifactReference: InternalModelArtifactReference,
  reviewArtifactPackageRelativePath: String,
  carrierPackageContext: InternalModelVerifiedPackageContext,
  semanticDiffAdmission: InternalModelSemanticDiffAdmission
)
