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
final class CarReviewCapabilityCatalogSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review capability catalog" should {
    "reuse canonical Evidence and Observation mappings without executing another provider" in {
      Given("one canonical Report")
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)

      When("CBD projects reusable capabilities")
      val capabilities = CarReviewCapabilityCatalog.project(report)

      Then("domain, documentation, and runtime capability views retain existing canonical IDs")
      capabilities.map(_.capability.id.value) should contain allOf (
        "quality.domain.identity-consistency",
        "quality.documentation.rationale",
        "quality.observability.runtime-evidence"
      )
      val documentation = capabilities.find(_.capability.id.value == "quality.documentation.rationale").getOrElse(fail("documentation capability missing"))
      documentation.observationIds.map(_.value) should contain("report-finding-missing-rationale")
      documentation.evidenceIds.map(_.value) should contain("report-evidence-project-yaml")
      CarReviewCapabilityCatalog.definition(ReviewCapabilityId("quality.observability.runtime-evidence")).exists(_.runtimeEvidenceRequired) shouldBe true
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
