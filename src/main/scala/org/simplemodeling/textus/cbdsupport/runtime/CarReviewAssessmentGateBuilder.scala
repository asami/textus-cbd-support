package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ReviewAssessmentGateInput(
  capabilityId: ReviewCapabilityId,
  observations: Vector[ReviewObservation],
  evidence: Vector[ReviewEvidence],
  policyId: String,
  policyVersion: ReviewVersion
)

final case class ReviewAssessmentGateResult(
  assessment: ReviewAssessment,
  gate: ReviewGate
)

/** Builds one CBD-owned deterministic assessment and gate after reconciliation. */
object CarReviewAssessmentGateBuilder {
  def build(input: ReviewAssessmentGateInput): ReviewAssessmentGateResult = {
    val observations = input.observations.sortBy(_.id.value)
    val subjects = observations.map(_.subject).distinct.sortBy(value => (value.kind, value.id))
    val unknownsubjects = observations.filter(_.`type`.value == "unknown").map(_.subject).toSet
    val assessedsubjects = subjects.filterNot(unknownsubjects.contains)
    val applicable = subjects.size
    val coverage = Option.when(applicable > 0)(ReviewCoverage(
      applicable,
      assessedsubjects.size,
      unknownsubjects.size,
      assessedsubjects.size * 10000 / applicable
    ))
    val findingids = observations.filter(_.`type`.value == "finding").map(_.id)
    val assuranceids = observations.filter(_.`type`.value == "assurance").map(_.id)
    val unknownids = observations.filter(_.`type`.value == "unknown").map(_.id)
    val maturity =
      if applicable == 0 || unknownids.nonEmpty then "unassessed"
      else if findingids.nonEmpty && assuranceids.nonEmpty then "partial"
      else if findingids.nonEmpty then "missing"
      else "established"
    val confidence =
      if unknownids.nonEmpty then "low"
      else if observations.forall(_.confidence.value == "high") then "high"
      else "medium"
    val providers = observations.map(_.provider.provider.id).distinct.sortBy(_.value)
    val evidenceids = observations.flatMap(_.evidenceIds).distinct.sortBy(_.value)
    val strengths = if assuranceids.nonEmpty then Vector("Admitted provider Assurance is evidence-backed.") else Vector.empty
    val gaps = (if findingids.nonEmpty then Vector("CBD gate has active Finding evidence.") else Vector.empty) ++
      (if unknownids.nonEmpty then Vector("Unknown observations prevent complete assessment.") else Vector.empty)
    val assessment = ReviewAssessment(
      input.capabilityId,
      ReviewApplicability(if applicable == 0 then "unknown" else "applicable"),
      ReviewMaturity(maturity),
      coverage,
      ReviewConfidence(confidence),
      providers,
      observations.map(_.id),
      evidenceids,
      strengths,
      gaps
    )
    val gate = if findingids.nonEmpty then ReviewGate(
      input.policyId,
      input.policyVersion,
      ReviewGateResult("fail"),
      Vector("Active Finding observations block this profile gate."),
      findingids
    ) else if unknownids.nonEmpty then ReviewGate(
      input.policyId,
      input.policyVersion,
      ReviewGateResult("unknown"),
      Vector("Unknown observations prevent a passing gate result."),
      Vector.empty
    ) else ReviewGate(
      input.policyId,
      input.policyVersion,
      ReviewGateResult("pass"),
      Vector("No active Finding or Unknown observations remain."),
      Vector.empty
    )
    ReviewAssessmentGateResult(assessment, gate)
  }
}
