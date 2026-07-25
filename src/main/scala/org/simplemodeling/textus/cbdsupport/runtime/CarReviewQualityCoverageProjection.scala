package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 24, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
enum CarReviewQualityCoverageState(val value: String) {
  case Observed extends CarReviewQualityCoverageState("observed")
  case Unknown extends CarReviewQualityCoverageState("unknown")
}

/** A visible capability-level result; this projection never edits the Report. */
final case class CarReviewQualityCoverageItem(
  rule: CarReviewQualityRule,
  state: CarReviewQualityCoverageState,
  observationIds: Vector[ReviewObservationId],
  evidenceIds: Vector[ReviewEvidenceId],
  limitation: Option[ReviewLimitation]
)

/**
 * Total quality coverage view. Every catalog capability appears exactly once:
 * canonical Evidence/Observations when a provider supplied them, or a rule-bound
 * explicit Unknown when no check/provider did.
 */
object CarReviewQualityCoverageProjection {
  def project(report: CarReviewReport): Vector[CarReviewQualityCoverageItem] =
    CarReviewQualityRuleMatrix.rules.filter(rule => _base_rule(rule)).map { rule =>
      val observations = report.observations.filter(_.mappings.qualityCapabilities.contains(rule.capabilityId)).sortBy(_.id.value)
      val evidenceids = observations.flatMap(_.evidenceIds).distinct.sortBy(_.value)
      if observations.nonEmpty then
        CarReviewQualityCoverageItem(rule, CarReviewQualityCoverageState.Observed, observations.map(_.id), evidenceids, None)
      else
        CarReviewQualityCoverageItem(
          rule,
          CarReviewQualityCoverageState.Unknown,
          Vector.empty,
          Vector.empty,
          Some(ReviewLimitation(
            rule.missingEvidenceLimitationCode,
            ReviewLimitationScope("capability"),
            Some(rule.capabilityId.value),
            s"No admitted provider Evidence is available for ${rule.checkId.value}.",
            retryable = true
          ))
        )
    }.sortBy(_.rule.capabilityId.value)

  private def _base_rule(rule: CarReviewQualityRule): Boolean =
    rule.checkId.value == s"cbd.car-review.${rule.capabilityId.value}"
}
