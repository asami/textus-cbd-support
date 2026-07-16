package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewCapabilityDefinition(
  id: ReviewCapabilityId,
  views: Vector[String],
  assessmentFocus: String,
  representativeEvidenceKinds: Vector[String],
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
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.security.boundary"), Vector("security"), "Declared boundary and admission controls protect review targets and exposed operations.", Vector("security-policy", "configuration"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.domain.identity-consistency"), Vector("domain", "implementation"), "Domain identifiers and relations remain consistent across model, implementation, and CAR boundaries.", Vector("cml-model", "abi"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.documentation.rationale"), Vector("documentation", "implementation"), "Public documentation explains the implemented component and operation rationale.", Vector("documentation", "cml-model"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.ai-readiness"), Vector("ai-readiness"), "Structured, bounded evidence is suitable for explicit opt-in AI review without provider fallback.", Vector("provider-descriptor", "evidence-bundle"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.resilience"), Vector("resilience"), "Failure, cancellation, timeout, and limitation behavior is explicit and attributable.", Vector("test-result", "execution-fact"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.testability"), Vector("testability"), "Executable tests cover the admitted component behavior and declared contracts.", Vector("test-result", "build"), runtimeEvidenceRequired = false),
    CarReviewCapabilityDefinition(ReviewCapabilityId("quality.observability.runtime-evidence"), Vector("observability", "runtime"), "Accepted runtime observations establish operational behavior beyond static analysis.", Vector("runtime-observation", "execution-fact"), runtimeEvidenceRequired = true)
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
