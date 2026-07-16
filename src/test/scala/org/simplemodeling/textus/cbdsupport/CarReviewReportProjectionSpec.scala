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
final class CarReviewReportProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CBD Review report projections" should {
    "render deterministic text, canonical JSON, safe HTML, and explicitly lossy SARIF from one Report" in {
      Given("one canonical Report containing location-bearing and location-free Findings")
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)

      When("CBD projects the Report twice")
      val first = CarReviewReportProjection.render(report).fold(_fail, identity)
      val second = CarReviewReportProjection.render(report).fold(_fail, identity)

      Then("all complete projections agree on Review/gate identity and SARIF declares its loss")
      first shouldBe second
      first.text should include(s"Review: ${report.reviewId.value}")
      first.text should include(s"Gate: ${report.gate.result.value}")
      first.canonicalJson should include(report.reportDigest.value)
      first.html should include("canonical-review-text")
      first.sarif should include("location-bearing-findings-only")
      first.sarif should include("report-finding-missing-rationale")
      first.sarif should not include "report-unknown-runtime"

      And("a canonical message that contains markup is escaped in the HTML projection")
      val changed = report.copy(observations = report.observations.updated(0, report.observations.head.copy(message = "<unsafe>")))
      val escaped = CarReviewReportProjection.render(CarReviewReportCodec.withCalculatedDigest(changed).fold(_fail, identity)).fold(_fail, identity)
      escaped.html should include("&lt;unsafe&gt;")
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
