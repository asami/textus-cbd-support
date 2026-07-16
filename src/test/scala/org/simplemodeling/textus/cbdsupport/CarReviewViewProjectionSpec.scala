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
final class CarReviewViewProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review cross-view projection" should {
    "reuse canonical Evidence and Observation identities across CNCF, implementation, and quality views" in {
      Given("one canonical report with mappings, provider attribution, and implementation locations")
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)

      When("CBD projects the three read-only views")
      val views = CarReviewViewProjection.project(report)

      Then("each view retains canonical IDs and links back to its provider and source location")
      views.cncf.map(_.key) should contain allOf ("cncf.component-model", "cncf.observability")
      views.implementation.map(_.key) should contain("project.yaml")
      views.quality.map(_.key) should contain("quality.documentation.rationale")
      val implementation = views.implementation.find(_.key == "project.yaml").getOrElse(fail("implementation projection missing"))
      implementation.observationIds.map(_.value) should contain("report-finding-missing-rationale")
      implementation.evidenceIds.map(_.value) should contain("report-evidence-project-yaml")
      implementation.providerLinks.map(_.provider.id.value) should contain("cozy")
      implementation.locations.flatMap(_.path) should contain("project.yaml")
      report.observations.map(_.id).toSet should contain allElementsOf views.cncf.flatMap(_.observationIds).toSet
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
