package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Creates the one initial CBD-owned development profile template per submission. */
final class CarReviewDevelopmentTemplateProvider(
  now: ReviewInstant,
  nextReportId: () => ReviewReportId
) extends CarReviewCanonicalTemplateProvider {
  def template(reviewId: ReviewId, target: ReviewTarget): Consequence[CarReviewReport] =
    Consequence.success(CarReviewReport(
      ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      ReviewDocumentType(CarReviewVocabulary.DOCUMENT_TYPE),
      nextReportId(),
      ReviewDigest("sha256:" + ("0" * 64)),
      reviewId,
      now,
      target,
      ReviewProfile("development"),
      ReviewExecution(now, now, Vector.empty),
      Vector.empty,
      Vector.empty,
      Vector(ReviewAssessment(
        ReviewCapabilityId("quality.domain.identity-consistency"),
        ReviewApplicability("unknown"),
        ReviewMaturity("unassessed"),
        None,
        ReviewConfidence("low"),
        Vector.empty,
        Vector.empty,
        Vector.empty,
        Vector.empty,
        Vector.empty
      )),
      Vector.empty,
      None,
      ReviewGate(
        "cbd-review.development",
        ReviewVersion("1.0.0"),
        ReviewGateResult("unknown"),
        Vector("CBD has not yet reconciled provider observations."),
        Vector.empty
      )
    ))
}
