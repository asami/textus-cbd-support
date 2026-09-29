package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Exact raw artifact identity retained by a candidate-review record. */
private[runtime] final case class InternalModelCandidateReviewArtifact(
  artifactId: String,
  sha256: String
)

/** Independently caller-admitted rule execution basis. */
private[runtime] final case class InternalModelCandidateReviewRule(
  ruleId: String,
  ruleVersion: String,
  sha256: String
)

/** Independently caller-admitted provider execution basis. */
private[runtime] final case class InternalModelCandidateReviewProvider(
  providerId: String,
  providerVersion: String,
  sha256: String
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

/**
 * Closed, portable candidate-review evidence.  It deliberately stores review
 * evidence rather than an approval, and its reviewed package bytes remain
 * distinct from the current carrier package context.
 */
private[runtime] final case class InternalModelCandidateReviewBinding(
  candidateArtifact: InternalModelCandidateReviewArtifact,
  candidateIdentity: String,
  candidateModelIdentity: String,
  candidateRevision: Int,
  continuityArtifact: InternalModelCandidateReviewArtifact,
  evidenceArtifacts: Vector[InternalModelCandidateReviewArtifact],
  profile: String,
  providers: Vector[InternalModelCandidateReviewProvider],
  realizationArtifact: InternalModelCandidateReviewArtifact,
  realizationIdentity: String,
  reviewIdentity: String,
  reviewRevision: Int,
  reviewedPackageManifest: InternalModelCandidateCmlContent,
  rules: Vector[InternalModelCandidateReviewRule],
  schemaVersion: String,
  scope: InternalModelSemanticScope,
  semanticDiffArtifact: InternalModelCandidateReviewArtifact,
  semanticDiffIdentity: String,
  semanticDiffRevision: Int,
  targets: Vector[InternalModelCandidateReviewTarget],
  canonicalBytes: Vector[Byte]
)

/** Captured review artifact and its same-capture semantic-diff carrier context. */
private[runtime] final case class InternalModelVerifiedCandidateReviewBindingPackage(
  carrierPackageContext: InternalModelVerifiedPackageContext,
  semanticDiffPackage: InternalModelVerifiedSemanticDiffPackage,
  reviewArtifact: InternalModelVerifiedProjection
)

/** Successful review admission retains both historical and current package contexts. */
private[runtime] final case class InternalModelCandidateReviewBindingAdmission(
  binding: InternalModelCandidateReviewBinding,
  reviewArtifactId: String,
  reviewArtifactSha256: String,
  reviewArtifactPackageRelativePath: String,
  reviewedPackageContext: InternalModelVerifiedPackageContext,
  carrierPackageContext: InternalModelVerifiedPackageContext,
  semanticDiffAdmission: InternalModelSemanticDiffAdmission
)
