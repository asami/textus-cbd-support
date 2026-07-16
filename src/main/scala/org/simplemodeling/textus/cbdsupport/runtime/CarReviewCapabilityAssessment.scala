package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Builds a named capability assessment only from observations mapped to that capability. */
object CarReviewCapabilityAssessment {
  def build(
    capabilityId: ReviewCapabilityId,
    observations: Vector[ReviewObservation],
    evidence: Vector[ReviewEvidence],
    policyId: String,
    policyVersion: ReviewVersion
  ): Either[String, ReviewAssessmentGateResult] =
    CarReviewCapabilityCatalog.definition(capabilityId).toRight(s"Unknown Review capability '${capabilityId.value}'.").map { _ =>
      val mapped = observations.filter(_.mappings.qualityCapabilities.contains(capabilityId))
      CarReviewAssessmentGateBuilder.build(ReviewAssessmentGateInput(capabilityId, mapped, evidence, policyId, policyVersion))
    }
}
