package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 23, 2026
 *  version Jul. 23, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * The common, read-only document model for Review delivery surfaces.
 *
 * It projects one already-admitted canonical Report. It never invokes a
 * provider, reads repository history, changes a Report, or creates a Finding,
 * Assurance, Unknown, capability assessment, or gate decision.
 */
final case class CarReviewDeliveryDocument(
  dashboard: CarReviewDeliveryDashboard,
  observations: Vector[CarReviewDeliveryObservation],
  capabilities: Vector[CarReviewDeliveryCapability],
  qualityCoverage: Vector[CarReviewDeliveryQualityCoverage],
  limitations: Vector[CarReviewDeliveryLimitation]
) {
  def diagnoseObservation(id: ReviewObservationId): Option[CarReviewItemDiagnosis] =
    observations.find(_.id == id).map { observation =>
      _with_identity(_observation_diagnosis(observation))
    }

  def diagnoseCapability(id: ReviewCapabilityId): Option[CarReviewItemDiagnosis] =
    capabilities.find(_.id == id).map { capability =>
      _with_identity(_capability_diagnosis(capability))
    }

  private def _with_identity(value: CarReviewItemDiagnosis): CarReviewItemDiagnosis = {
    val subjectids = value.observationIds.map(_.value).toSet ++ value.capabilityIds.map(_.value)
    val limitations = this.limitations.filter { limitation =>
      limitation.subjectId.forall(subjectids.contains) || limitation.scope.value == "report"
    }
    value.copy(
      reportId = dashboard.reportId,
      reportDigest = dashboard.reportDigest,
      limitations = limitations
    )
  }

  private def _observation_diagnosis(value: CarReviewDeliveryObservation): CarReviewItemDiagnosis =
    CarReviewItemDiagnosis(
      "observation",
      value.id.value,
      dashboard.reportId,
      dashboard.reportDigest,
      Some(value.rule),
      Vector(value.id),
      value.evidenceIds,
      value.capabilityIds,
      Vector(value.provider.provider.id),
      value.locations,
      Some(value.disposition),
      Vector.empty
    )

  private def _capability_diagnosis(value: CarReviewDeliveryCapability): CarReviewItemDiagnosis =
    CarReviewItemDiagnosis(
      "capability",
      value.id.value,
      dashboard.reportId,
      dashboard.reportDigest,
      None,
      value.observationIds,
      value.evidenceIds,
      Vector(value.id),
      value.providerIds,
      observations.filter(observation => value.observationIds.contains(observation.id)).flatMap(_.locations).distinct.sorted,
      None,
      Vector.empty
    )
}

final case class CarReviewDeliveryDashboard(
  reviewId: ReviewId,
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  target: CarReviewDeliveryTarget,
  profile: ReviewProfile,
  gate: CarReviewDeliveryGate,
  findingCount: Int,
  assuranceCount: Int,
  unknownCount: Int,
  qualityObservedCount: Int,
  qualityUnknownCount: Int,
  baseline: Option[CarReviewDeliveryBaseline]
)

final case class CarReviewDeliveryTarget(
  kind: ReviewTargetKind,
  organization: Option[String],
  name: String,
  version: Option[ReviewVersion],
  digest: ReviewDigest
)

final case class CarReviewDeliveryGate(
  policyId: String,
  policyVersion: ReviewVersion,
  result: ReviewGateResult,
  reasons: Vector[String],
  blockingObservationIds: Vector[ReviewObservationId]
)

final case class CarReviewDeliveryBaseline(
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  addedObservationIds: Vector[ReviewObservationId],
  removedObservationIds: Vector[ReviewObservationId],
  unchangedObservationIds: Vector[ReviewObservationId]
)

final case class CarReviewDeliveryObservation(
  id: ReviewObservationId,
  `type`: ReviewObservationType,
  rule: ReviewRuleIdentity,
  subject: CarReviewDeliverySubject,
  message: String,
  severity: Option[ReviewSeverity],
  confidence: ReviewConfidence,
  evidenceIds: Vector[ReviewEvidenceId],
  capabilityIds: Vector[ReviewCapabilityId],
  provider: ReviewProviderAttribution,
  locations: Vector[String],
  disposition: CarReviewDeliveryDisposition
)

final case class CarReviewDeliverySubject(
  kind: String,
  id: String
)

final case class CarReviewDeliveryDisposition(
  state: ReviewDispositionState,
  reason: Option[String],
  author: Option[String],
  expiresAt: Option[ReviewInstant]
)

final case class CarReviewDeliveryCapability(
  id: ReviewCapabilityId,
  applicability: ReviewApplicability,
  maturity: ReviewMaturity,
  coverage: Option[ReviewCoverage],
  confidence: ReviewConfidence,
  providerIds: Vector[ReviewProviderId],
  evidenceIds: Vector[ReviewEvidenceId],
  observationIds: Vector[ReviewObservationId],
  strengths: Vector[String],
  gaps: Vector[String]
)

final case class CarReviewDeliveryLimitation(
  code: String,
  scope: ReviewLimitationScope,
  subjectId: Option[String],
  message: String,
  retryable: Boolean
)

/** Delivery-safe total quality coverage derived from one canonical Report. */
final case class CarReviewDeliveryQualityCoverage(
  checkId: CarReviewQualityCheckId,
  capabilityId: ReviewCapabilityId,
  state: CarReviewQualityCoverageState,
  observationIds: Vector[ReviewObservationId],
  evidenceIds: Vector[ReviewEvidenceId],
  limitation: Option[CarReviewDeliveryLimitation]
)

final case class CarReviewItemDiagnosis(
  kind: String,
  itemId: String,
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  rule: Option[ReviewRuleIdentity],
  observationIds: Vector[ReviewObservationId],
  evidenceIds: Vector[ReviewEvidenceId],
  capabilityIds: Vector[ReviewCapabilityId],
  providerIds: Vector[ReviewProviderId],
  locations: Vector[String],
  disposition: Option[CarReviewDeliveryDisposition],
  limitations: Vector[CarReviewDeliveryLimitation]
)

object CarReviewDeliveryProjection {
  private val _absolute_path_pattern = "(?<![A-Za-z0-9])/(?:[^\\s,;:]+(?:/[^\\s,;:]+)*)".r

  def project(report: CarReviewReport): CarReviewDeliveryDocument = {
    val limitations = report.limitations.map(_limitation).sortBy(value => (value.scope.value, value.code, value.subjectId.getOrElse(""), value.message))
    val observations = report.observations.sortBy(_.id.value).map(_observation)
    val capabilities = report.assessments.sortBy(_.capabilityId.value).map(_capability)
    val qualitycoverage = CarReviewQualityCoverageProjection.project(report).map(_quality_coverage)
    val dashboard = CarReviewDeliveryDashboard(
      report.reviewId,
      report.reportId,
      report.reportDigest,
      _target(report.target),
      report.profile,
      _gate(report.gate),
      observations.count(_.`type`.value == "finding"),
      observations.count(_.`type`.value == "assurance"),
      observations.count(_.`type`.value == "unknown"),
      qualitycoverage.count(_.state == CarReviewQualityCoverageState.Observed),
      qualitycoverage.count(_.state == CarReviewQualityCoverageState.Unknown),
      report.baseline.map(_baseline)
    )
    CarReviewDeliveryDocument(dashboard, observations, capabilities, qualitycoverage, limitations)
  }

  def diagnoseObservation(report: CarReviewReport, id: ReviewObservationId): Option[CarReviewItemDiagnosis] =
    project(report).diagnoseObservation(id)

  def diagnoseCapability(report: CarReviewReport, id: ReviewCapabilityId): Option[CarReviewItemDiagnosis] =
    project(report).diagnoseCapability(id)

  private def _baseline(value: ReviewBaseline): CarReviewDeliveryBaseline =
    CarReviewDeliveryBaseline(
      value.reportId,
      value.digest,
      value.addedObservationIds.distinct.sortBy(_.value),
      value.removedObservationIds.distinct.sortBy(_.value),
      value.unchangedObservationIds.distinct.sortBy(_.value)
    )

  private def _target(value: ReviewTarget): CarReviewDeliveryTarget =
    CarReviewDeliveryTarget(
      value.kind,
      value.organization.map(_safe_text),
      _safe_text(value.name),
      value.version,
      value.digest
    )

  private def _observation(value: ReviewObservation): CarReviewDeliveryObservation =
    CarReviewDeliveryObservation(
      value.id,
      value.`type`,
      value.rule,
      _subject(value.subject),
      _safe_text(value.message),
      value.severity,
      value.confidence,
      value.evidenceIds.distinct.sortBy(_.value),
      value.mappings.qualityCapabilities.distinct.sortBy(_.value),
      value.provider,
      value.locations.flatMap(CarReviewMcpReadApplication.renderLocation).distinct.sorted,
      _disposition(value.disposition)
    )

  private def _capability(value: ReviewAssessment): CarReviewDeliveryCapability =
    CarReviewDeliveryCapability(
      value.capabilityId,
      value.applicability,
      value.maturity,
      value.coverage,
      value.confidence,
      value.providerIds.distinct.sortBy(_.value),
      value.evidenceIds.distinct.sortBy(_.value),
      value.observationIds.distinct.sortBy(_.value),
      value.strengths.distinct.sorted.map(_safe_text),
      value.gaps.distinct.sorted.map(_safe_text)
    )

  private def _limitation(value: ReviewLimitation): CarReviewDeliveryLimitation =
    CarReviewDeliveryLimitation(
      value.code,
      value.scope,
      value.subjectId.map(_safe_text),
      _safe_text(value.message),
      value.retryable
    )

  private def _quality_coverage(value: CarReviewQualityCoverageItem): CarReviewDeliveryQualityCoverage =
    CarReviewDeliveryQualityCoverage(
      value.rule.checkId,
      value.rule.capabilityId,
      value.state,
      value.observationIds.distinct.sortBy(_.value),
      value.evidenceIds.distinct.sortBy(_.value),
      value.limitation.map(_limitation)
    )

  private def _gate(value: ReviewGate): CarReviewDeliveryGate =
    CarReviewDeliveryGate(
      value.policyId,
      value.policyVersion,
      value.result,
      value.reasons.distinct.sorted.map(_safe_text),
      value.blockingObservationIds.distinct.sortBy(_.value)
    )

  private def _subject(value: ReviewSubject): CarReviewDeliverySubject =
    CarReviewDeliverySubject(_safe_text(value.kind), _safe_text(value.id))

  private def _disposition(value: ReviewDisposition): CarReviewDeliveryDisposition =
    CarReviewDeliveryDisposition(
      value.state,
      value.reason.map(_safe_text),
      value.author.map(_safe_text),
      value.expiresAt
    )

  private def _safe_text(value: String): String =
    _absolute_path_pattern.replaceAllIn(InformationSourceDiagnosticPolicy.sanitize(value), "[redacted-path]")
}
