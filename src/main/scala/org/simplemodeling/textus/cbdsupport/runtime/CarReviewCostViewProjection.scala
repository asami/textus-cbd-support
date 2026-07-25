package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json

/*
 * @since   Jul. 24, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewCostViewReduction(value: BigDecimal, unit: String)

/**
 * Read-only Cost View entry. Expected and measured reductions are deliberately
 * independent so renderers cannot accidentally present an estimate as savings.
 */
final case class CarReviewCostViewItem(
  scenarioId: String,
  kind: CarReviewCostScenarioKind,
  currentArchitecture: String,
  costDriver: String,
  optimization: String,
  expectedReduction: Option[CarReviewCostViewReduction],
  measuredReduction: Option[CarReviewCostViewReduction],
  comparisonPeriod: Option[String],
  normalizedUnit: Option[String],
  qualityConstraints: Vector[String],
  operationalTradeoffs: Vector[String],
  confidence: ReviewConfidence,
  observationIds: Vector[ReviewObservationId],
  evidenceIds: Vector[ReviewEvidenceId]
) {
  def measurementIsComparable: Boolean =
    measuredReduction.nonEmpty && comparisonPeriod.exists(_.nonEmpty) && normalizedUnit.exists(_.nonEmpty)
}

/** Projects only canonical Evidence/Observations; it never calculates a cost. */
object CarReviewCostViewProjection {
  private val _cost_capabilities = Set(
    ReviewCapabilityId("quality.cost-efficiency.infrastructure"),
    ReviewCapabilityId("quality.cost-efficiency.operations"),
    ReviewCapabilityId("quality.cost-efficiency.development"),
    ReviewCapabilityId("quality.performance.resource-efficiency"),
    ReviewCapabilityId("quality.sustainability.work-avoidance")
  )

  def project(report: CarReviewReport): Vector[CarReviewCostViewItem] = {
    val observations = report.observations.filter(_.mappings.qualityCapabilities.exists(_cost_capabilities.contains))
    val byevidence = observations.flatMap(observation => observation.evidenceIds.map(_ -> observation)).groupMap(_._1)(_._2)
    report.evidence.flatMap { evidence =>
      byevidence.get(evidence.id).flatMap { linked =>
        _item(evidence, linked.sortBy(_.id.value))
      }
    }.sortBy(value => (value.kind.value, value.scenarioId))
  }

  private def _item(evidence: ReviewEvidence, observations: Vector[ReviewObservation]): Option[CarReviewCostViewItem] =
    evidence.facts("costOptimization").flatMap(_.asObject).flatMap { facts =>
      for {
        id <- _string(facts, "id")
        kind <- _kind(_string(facts, "kind"))
        current <- _string(facts, "currentArchitecture")
        driver <- _string(facts, "costDriver")
        optimization <- _string(facts, "optimization")
        constraints <- _strings(facts, "qualityConstraints")
        tradeoffs <- _strings(facts, "operationalTradeoffs")
        confidence <- _string(facts, "confidence").filter(CarReviewVocabulary.CONFIDENCES.contains)
      } yield CarReviewCostViewItem(
        id,
        kind,
        current,
        driver,
        optimization,
        _reduction(facts("expectedReduction")),
        _reduction(facts("measuredReduction")),
        _string(facts, "comparisonPeriod"),
        _string(facts, "normalizedUnit"),
        constraints,
        tradeoffs,
        ReviewConfidence(confidence),
        observations.map(_.id).distinct.sortBy(_.value),
        Vector(evidence.id)
      )
    }

  private def _kind(value: Option[String]): Option[CarReviewCostScenarioKind] = value.flatMap {
    case "static-web-app" => Some(CarReviewCostScenarioKind.StaticWebApp)
    case "gemma-mcp" => Some(CarReviewCostScenarioKind.GemmaMcp)
    case _ => None
  }

  private def _reduction(value: Option[Json]): Option[CarReviewCostViewReduction] =
    value.flatMap(_.asObject).flatMap { fields =>
      for {
        amount <- fields("value").flatMap(_.asNumber).flatMap(_.toBigDecimal).filter(_ >= 0)
        unit <- _string(fields, "unit")
      } yield CarReviewCostViewReduction(amount, unit)
    }

  private def _string(fields: io.circe.JsonObject, name: String): Option[String] =
    fields(name).flatMap(_.asString).map(_.trim).filter(_.nonEmpty)

  private def _strings(fields: io.circe.JsonObject, name: String): Option[Vector[String]] =
    fields(name).flatMap(_.asArray).map(_.toVector.flatMap(_.asString).map(_.trim).filter(_.nonEmpty)).filter(_.nonEmpty)
}
