package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewCapabilityDefinition(
  id: ReviewCapabilityId,
  views: Vector[String],
  runtimeEvidenceRequired: Boolean
)

final case class CarReviewCapabilityProjection(
  capability: CarReviewCapabilityDefinition,
  evidenceIds: Vector[ReviewEvidenceId],
  observationIds: Vector[ReviewObservationId]
)

/** CBD-owned reusable capability definitions; projection only consumes canonical Report references. */
object CarReviewCapabilityCatalog {
  private val _definitions = Vector(
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.security.boundary"), Vector("security"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.domain.identity-consistency"), Vector("domain", "implementation"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.documentation.rationale"), Vector("documentation", "implementation"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.ai-readiness"), Vector("ai-readiness"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.resilience"), Vector("resilience"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.testability"), Vector("testability"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.observability.runtime-evidence"), Vector("observability", "runtime"), runtimeEvidenceRequired = true)
  )

  val definitions: Vector[CarReviewCapabilityDefinition] = _definitions.sortBy(_.id.value)

  def definition(id: ReviewCapabilityId): Option[CarReviewCapabilityDefinition] =
    definitions.find(_.id == id)

  def project(report: CarReviewReport): Vector[CarReviewCapabilityProjection] =
    definitions.flatMap { definition =>
      val assessment = report.assessments.find(_.capabilityId == definition.id)
      val observations = report.observations.filter(_.mappings.qualityCapabilities.contains(definition.id))
      val evidenceids = (assessment.toVector.flatMap(_.evidenceIds) ++ observations.flatMap(_.evidenceIds)).distinct.sortBy(_.value)
      val observationids = (assessment.toVector.flatMap(_.observationIds) ++ observations.map(_.id)).distinct.sortBy(_.value)
      if (evidenceids.nonEmpty || observationids.nonEmpty) Some(CarReviewCapabilityProjection(definition, evidenceids, observationids)) else None
    }
}
