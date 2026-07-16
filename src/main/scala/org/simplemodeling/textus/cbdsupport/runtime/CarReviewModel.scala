package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.JsonObject

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ReviewId(value: String)
final case class ReviewReportId(value: String)
final case class ReviewEvidenceId(value: String)
final case class ReviewObservationId(value: String)
final case class ReviewCapabilityId(value: String)
final case class ReviewProviderId(value: String)
final case class ReviewRuleId(value: String)
final case class ReviewDigest(value: String)
final case class ReviewVersion(value: String)
final case class ReviewProfile(value: String)
final case class ReviewInstant(value: String)
final case class ReviewSchemaVersion(value: String)
final case class ReviewDocumentType(value: String)
final case class ReviewTargetKind(value: String)
final case class ReviewProviderState(value: String)
final case class ReviewLimitationScope(value: String)
final case class ReviewObservationType(value: String)
final case class ReviewSeverity(value: String)
final case class ReviewConfidence(value: String)
final case class ReviewDispositionState(value: String)
final case class ReviewApplicability(value: String)
final case class ReviewMaturity(value: String)
final case class ReviewGateResult(value: String)

final case class ReviewTarget(
  kind: ReviewTargetKind,
  organization: Option[String],
  name: String,
  version: Option[ReviewVersion],
  digest: ReviewDigest
)

final case class ReviewProviderIdentity(
  id: ReviewProviderId,
  version: ReviewVersion
)

final case class ReviewRuleIdentity(
  id: ReviewRuleId,
  version: ReviewVersion
)

final case class ReviewLimitation(
  code: String,
  scope: ReviewLimitationScope,
  subjectId: Option[String],
  message: String,
  retryable: Boolean
)

final case class ReviewProviderExecution(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  state: ReviewProviderState,
  bundleDigest: Option[ReviewDigest],
  startedAt: Option[ReviewInstant],
  completedAt: Option[ReviewInstant],
  limitations: Vector[ReviewLimitation]
)

final case class ReviewSubject(
  kind: String,
  id: String
)

final case class ReviewLocation(
  uri: Option[String],
  path: Option[String],
  line: Option[Int],
  column: Option[Int],
  endLine: Option[Int],
  endColumn: Option[Int]
)

final case class ReviewEvidence(
  id: ReviewEvidenceId,
  kind: String,
  subject: ReviewSubject,
  providerId: ReviewProviderId,
  bundleDigest: ReviewDigest,
  providerEvidenceId: String,
  location: Option[ReviewLocation],
  digest: Option[ReviewDigest],
  facts: JsonObject
)

final case class ReviewProviderAttribution(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  bundleDigest: ReviewDigest
)

final case class ReviewDisposition(
  state: ReviewDispositionState,
  reason: Option[String],
  author: Option[String],
  expiresAt: Option[ReviewInstant]
)

final case class ReviewMappings(
  cncfFeatures: Vector[String],
  implementationSubjects: Vector[String],
  qualityCapabilities: Vector[ReviewCapabilityId]
)

final case class ReviewObservation(
  id: ReviewObservationId,
  `type`: ReviewObservationType,
  rule: ReviewRuleIdentity,
  subject: ReviewSubject,
  message: String,
  rationale: String,
  severity: Option[ReviewSeverity],
  confidence: ReviewConfidence,
  evidenceIds: Vector[ReviewEvidenceId],
  locations: Vector[ReviewLocation],
  provider: ReviewProviderAttribution,
  disposition: ReviewDisposition,
  mappings: ReviewMappings
)

final case class ReviewCoverage(
  applicableSubjects: Int,
  assessedSubjects: Int,
  unknownSubjects: Int,
  basisPoints: Int
)

final case class ReviewAssessment(
  capabilityId: ReviewCapabilityId,
  applicability: ReviewApplicability,
  maturity: ReviewMaturity,
  coverage: Option[ReviewCoverage],
  confidence: ReviewConfidence,
  providerIds: Vector[ReviewProviderId],
  observationIds: Vector[ReviewObservationId],
  evidenceIds: Vector[ReviewEvidenceId],
  strengths: Vector[String],
  gaps: Vector[String]
)

final case class ReviewBaseline(
  reportId: ReviewReportId,
  digest: ReviewDigest,
  addedObservationIds: Vector[ReviewObservationId],
  removedObservationIds: Vector[ReviewObservationId],
  unchangedObservationIds: Vector[ReviewObservationId]
)

final case class ReviewGate(
  policyId: String,
  policyVersion: ReviewVersion,
  result: ReviewGateResult,
  reasons: Vector[String],
  blockingObservationIds: Vector[ReviewObservationId]
)

final case class ReviewExecution(
  startedAt: ReviewInstant,
  completedAt: ReviewInstant,
  providers: Vector[ReviewProviderExecution]
)

final case class CarReviewReport(
  schemaVersion: ReviewSchemaVersion,
  documentType: ReviewDocumentType,
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  reviewId: ReviewId,
  createdAt: ReviewInstant,
  target: ReviewTarget,
  profile: ReviewProfile,
  execution: ReviewExecution,
  evidence: Vector[ReviewEvidence],
  observations: Vector[ReviewObservation],
  assessments: Vector[ReviewAssessment],
  limitations: Vector[ReviewLimitation],
  baseline: Option[ReviewBaseline],
  gate: ReviewGate
)

final case class CarReviewCodecFailure(
  code: String,
  path: String,
  message: String
)

object CarReviewVocabulary {
  val SCHEMA_VERSION = "textus.cbd.review-report.v1"
  val DOCUMENT_TYPE = "review-report"

  val TARGET_KINDS: Set[String] = Set("project", "car")
  val OBSERVATION_TYPES: Set[String] = Set("finding", "assurance", "unknown")
  val SEVERITIES: Set[String] = Set("info", "low", "medium", "high", "critical")
  val CONFIDENCES: Set[String] = Set("low", "medium", "high")
  val DISPOSITIONS: Set[String] = Set("active", "accepted", "suppressed", "deferred")
  val APPLICABILITIES: Set[String] = Set("applicable", "not-applicable", "unknown")
  val MATURITIES: Set[String] = Set(
    "unassessed", "missing", "ad-hoc", "partial", "established", "verified", "operational"
  )
  val GATE_RESULTS: Set[String] = Set("pass", "fail", "unknown")
  val PROVIDER_STATES: Set[String] = Set(
    "queued", "running", "completed", "failed", "disabled", "incompatible", "unavailable", "cancelled"
  )
  val LIMITATION_SCOPES: Set[String] = Set(
    "run", "provider", "report", "evidence", "observation", "assessment", "gate"
  )
}
