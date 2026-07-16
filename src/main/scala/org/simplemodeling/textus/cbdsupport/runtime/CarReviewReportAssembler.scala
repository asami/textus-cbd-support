package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
object CarReviewReportAssembler {
  def assemble(
    template: CarReviewReport,
    reconciled: CarReviewReconciliationResult,
    assessed: ReviewAssessmentGateResult
  ): Either[CarReviewCodecFailure, CarReviewReport] =
    CarReviewReportCodec.withCalculatedDigest(template.copy(
      evidence = reconciled.evidence,
      observations = reconciled.observations,
      assessments = Vector(assessed.assessment),
      limitations = (template.limitations ++ reconciled.limitations).distinct.sortBy(value => (value.code, value.subjectId.getOrElse(""))),
      gate = assessed.gate
    ))
}
