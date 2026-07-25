package org.simplemodeling.textus.cbdsupport.runtime

/**
 * The executable, provider-neutral rule matrix for every supported CAR Review
 * quality capability.  A row is a policy declaration, not an assertion that a
 * provider has run the check: missing required evidence remains an explicit
 * Unknown and can never become an Assurance.
 */
final case class CarReviewQualityCheckId(value: String)

enum CarReviewQualityCheckAuthority(val value: String) {
  case Deterministic extends CarReviewQualityCheckAuthority("deterministic")
  case Runtime extends CarReviewQualityCheckAuthority("runtime")
  /** AI content analysis is reportable but requires deterministic or human corroboration. */
  case Advisory extends CarReviewQualityCheckAuthority("advisory")
}

final case class CarReviewQualityRule(
  checkId: CarReviewQualityCheckId,
  capabilityId: ReviewCapabilityId,
  applicability: ReviewApplicability,
  requiredEvidenceKinds: Vector[String],
  authority: CarReviewQualityCheckAuthority,
  missingEvidenceObservationType: ReviewObservationType,
  missingEvidenceLimitationCode: String,
  evidenceBackedMaturity: ReviewMaturity
)

object CarReviewQualityRuleMatrix {
  val ApplicabilityWhenRequested = ReviewApplicability("applicable")
  val MissingEvidenceOutcome = ReviewObservationType("unknown")

  /** One base row for every catalog capability plus explicit AI content rows. */
  lazy val rules: Vector[CarReviewQualityRule] =
    (CarReviewCapabilityCatalog.definitions.map(_rule) ++ _ai_content_rules).sortBy(_.checkId.value)

  def rule(capabilityId: ReviewCapabilityId): Option[CarReviewQualityRule] =
    rules.find(rule => rule.capabilityId == capabilityId && rule.checkId.value == s"cbd.car-review.${capabilityId.value}")

  def rulesFor(capabilityId: ReviewCapabilityId): Vector[CarReviewQualityRule] =
    rules.filter(_.capabilityId == capabilityId)

  private def _rule(capability: CarReviewCapabilityDefinition): CarReviewQualityRule = {
    val authority =
      if capability.runtimeEvidenceRequired then CarReviewQualityCheckAuthority.Runtime
      else CarReviewQualityCheckAuthority.Deterministic
    CarReviewQualityRule(
      CarReviewQualityCheckId(s"cbd.car-review.${capability.id.value}"),
      capability.id,
      ApplicabilityWhenRequested,
      capability.representativeEvidenceKinds.distinct.sorted,
      authority,
      MissingEvidenceOutcome,
      s"cbd.car-review.${capability.id.value}.evidence-unavailable",
      if authority == CarReviewQualityCheckAuthority.Runtime then ReviewMaturity("operational") else ReviewMaturity("partial")
    )
  }

  private val _ai_content_rules = Vector(
    _ai_content_rule("mcp", Vector("mcp-description", "operation-contract", "ai-content-review")),
    _ai_content_rule("skill", Vector("skill-manifest", "skill-validation", "ai-content-review"))
  )

  private def _ai_content_rule(surface: String, evidencekinds: Vector[String]): CarReviewQualityRule = {
    val capabilityid = ReviewCapabilityId(s"quality.ai.operability.$surface")
    val checkid = CarReviewQualityCheckId(s"cbd.car-review.${capabilityid.value}.content")
    CarReviewQualityRule(
      checkid,
      capabilityid,
      ApplicabilityWhenRequested,
      evidencekinds.distinct.sorted,
      CarReviewQualityCheckAuthority.Advisory,
      MissingEvidenceOutcome,
      s"${checkid.value}.evidence-unavailable",
      ReviewMaturity("unassessed")
    )
  }
}
