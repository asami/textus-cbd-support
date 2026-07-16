package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewReportAssemblerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review report assembler" should {
    "replace the template evidence, observations, assessment, gate, and digest with CBD-owned reconciled content" in {
      Given("the canonical report template and one reconciled CBD result")
      val template = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
      val reconciled = CarReviewReconciliationResult(template.evidence, template.observations, Vector.empty, Vector.empty)
      val assessed = ReviewAssessmentGateResult(template.assessments.head, template.gate)

      When("CBD assembles the immutable canonical report")
      val report = CarReviewReportAssembler.assemble(template, reconciled, assessed).fold(_fail, identity)

      Then("only reconciled records remain and the deterministic digest is self-verifying")
      report.evidence shouldBe reconciled.evidence
      report.observations shouldBe reconciled.observations
      report.assessments shouldBe Vector(assessed.assessment)
      report.gate shouldBe assessed.gate
      CarReviewReportCodec.encode(report).isRight shouldBe true
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
