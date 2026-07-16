package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewQualityCapabilitySummary(
  capabilityId: ReviewCapabilityId,
  applicability: ReviewApplicability,
  maturity: ReviewMaturity,
  coverage: Option[ReviewCoverage],
  confidence: ReviewConfidence,
  providerIds: Vector[ReviewProviderId],
  evidenceIds: Vector[ReviewEvidenceId],
  observationIds: Vector[ReviewObservationId],
  strengths: Vector[String],
  gaps: Vector[String],
  unknownObservationIds: Vector[ReviewObservationId]
)

/** Explicit per-capability quality accounting. There is intentionally no aggregate score. */
final case class CarReviewQualitySummary(
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  capabilities: Vector[CarReviewQualityCapabilitySummary]
)

object CarReviewQualitySummary {
  def project(report: CarReviewReport): CarReviewQualitySummary = {
    val unknowns = report.observations.filter(_.`type`.value == "unknown").map(_.id).toSet
    val capabilities = report.assessments.sortBy(_.capabilityId.value).map { assessment =>
      CarReviewQualityCapabilitySummary(
        assessment.capabilityId,
        assessment.applicability,
        assessment.maturity,
        assessment.coverage,
        assessment.confidence,
        assessment.providerIds.distinct.sortBy(_.value),
        assessment.evidenceIds.distinct.sortBy(_.value),
        assessment.observationIds.distinct.sortBy(_.value),
        assessment.strengths.distinct.sorted,
        assessment.gaps.distinct.sorted,
        assessment.observationIds.filter(unknowns.contains).distinct.sortBy(_.value)
      )
    }
    CarReviewQualitySummary(report.reportId, report.reportDigest, capabilities)
  }
}
