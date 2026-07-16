package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewRuntimeEvidencePolicySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review runtime evidence policy" should {
    "reject Operational maturity derived from static evidence alone" in {
      Given("one canonical static-analysis Report")
      val report = _report
      val operational = report.copy(assessments = report.assessments.map(_.copy(maturity = ReviewMaturity("operational"))))

      When("a caller attempts to calculate its canonical digest")
      val result = CarReviewReportCodec.withCalculatedDigest(operational)

      Then("CBD refuses the unsupported Operational claim before it can be encoded")
      result.left.map(_.code) shouldBe Left("runtime-evidence-required")
    }

    "admit Operational maturity only with bounded attributable mapped runtime evidence" in {
      Given("one assessment augmented with a provider-bound runtime observation")
      val report = _report
      val assessment = report.assessments.head
      val sourceEvidence = report.evidence.head
      val sourceObservation = report.observations.find(_.`type`.value == "assurance").getOrElse(fail("assurance missing"))
      val evidence = sourceEvidence.copy(
        id = ReviewEvidenceId("report-evidence-runtime"),
        kind = CarReviewRuntimeEvidencePolicy.RuntimeObservationKind,
        providerEvidenceId = "runtime-observation"
      )
      val observation = sourceObservation.copy(
        id = ReviewObservationId("report-assurance-runtime"),
        subject = ReviewSubject("component", "textus-user-account-runtime"),
        evidenceIds = Vector(evidence.id),
        mappings = ReviewMappings(Vector.empty, Vector.empty, Vector(assessment.capabilityId))
      )
      val operationalAssessment = assessment.copy(
        maturity = ReviewMaturity("operational"),
        observationIds = (assessment.observationIds :+ observation.id).distinct.sortBy(_.value),
        evidenceIds = (assessment.evidenceIds :+ evidence.id).distinct.sortBy(_.value)
      )
      val operational = report.copy(
        evidence = report.evidence :+ evidence,
        observations = report.observations :+ observation,
        assessments = Vector(operationalAssessment)
      )

      When("CBD validates and calculates the canonical Report")
      val admitted = CarReviewReportCodec.withCalculatedDigest(operational).fold(error => fail(error.message), identity)

      Then("the Operational claim retains the accepted runtime Evidence and Observation identities")
      admitted.assessments.head.maturity shouldBe ReviewMaturity("operational")
      CarReviewRuntimeEvidencePolicy.supportsOperational(admitted.assessments.head, admitted.evidence, admitted.observations) shouldBe true
      CarReviewReportCodec.encode(admitted).isRight shouldBe true
    }
  }

  private def _report: CarReviewReport =
    CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)
}
