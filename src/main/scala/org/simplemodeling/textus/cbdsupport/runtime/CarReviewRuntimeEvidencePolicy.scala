package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits Operational maturity only when bounded, attributable runtime Evidence backs its mapped observation. */
object CarReviewRuntimeEvidencePolicy {
  val RuntimeObservationKind = "runtime-observation"

  def supportsOperational(
    assessment: ReviewAssessment,
    evidence: Vector[ReviewEvidence],
    observations: Vector[ReviewObservation]
  ): Boolean = {
    val runtimeEvidence = evidence.filter { item =>
      assessment.evidenceIds.contains(item.id) &&
        item.kind == RuntimeObservationKind &&
        assessment.providerIds.contains(item.providerId)
    }
    runtimeEvidence.exists { item =>
      observations.exists { observation =>
        assessment.observationIds.contains(observation.id) &&
          observation.evidenceIds.contains(item.id) &&
          observation.provider.provider.id == item.providerId &&
          observation.mappings.qualityCapabilities.contains(assessment.capabilityId)
      }
    }
  }
}
