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
final class CarReviewQualitySummarySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review quality summary" should {
    "retain deterministic per-capability accounting without an unexplained aggregate score" in {
      Given("one canonical Report with assessment coverage and an Unknown")
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)

      When("CBD projects the quality summary")
      val summary = CarReviewQualitySummary.project(report)

      Then("every capability retains its individual maturity, coverage, confidence, attribution, and Unknown accounting")
      summary.reportDigest shouldBe report.reportDigest
      summary.capabilities.map(_.capabilityId.value) shouldBe report.assessments.map(_.capabilityId.value).sorted
      val identitycapability = summary.capabilities.find(_.capabilityId.value == "quality.domain.identity-consistency").getOrElse(fail("identity summary missing"))
      identitycapability.coverage shouldBe report.assessments.find(_.capabilityId == identitycapability.capabilityId).flatMap(_.coverage)
      identitycapability.providerIds should not be empty
      identitycapability.unknownObservationIds.map(_.value) should contain("report-unknown-runtime")
      summary.productElementNames.toSet should not contain "score"
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
