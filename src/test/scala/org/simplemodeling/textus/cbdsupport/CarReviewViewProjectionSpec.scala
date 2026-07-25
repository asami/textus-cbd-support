package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 24, 2026
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

    "project Security and Observability capabilities without deriving conclusions in the renderer" in {
      Given("canonical security observations mapped to bounded text and auditability capabilities")
      val original = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
      val source = original.observations.head
      val boundedtext = source.copy(
        id = ReviewObservationId("report-assurance-bounded-text-datatype"),
        mappings = ReviewMappings(Vector.empty, Vector.empty, Vector(ReviewCapabilityId("quality.security.domain.bounded-text-datatype")))
      )
      val auditability = source.copy(
        id = ReviewObservationId("report-assurance-security-auditability"),
        mappings = ReviewMappings(Vector.empty, Vector.empty, Vector(ReviewCapabilityId("quality.security.auditability")))
      )
      val report = original.copy(observations = original.observations ++ Vector(boundedtext, auditability))

      When("CBD projects the named views from canonical quality mappings")
      val views = CarReviewViewProjection.project(report)

      Then("Security, Observability, and quality retain the same canonical identities")
      val security = views.namedView("security").flatMap(_.items.find(_.key == "quality.security.domain.bounded-text-datatype")).getOrElse(fail("Security projection missing"))
      security.observationIds should contain(boundedtext.id)
      security.evidenceIds should contain allElementsOf boundedtext.evidenceIds
      views.namedView("observability").flatMap(_.items.find(_.key == "quality.security.auditability")).map(_.observationIds) should contain(Vector(auditability.id))
      views.quality.find(_.key == "quality.security.domain.bounded-text-datatype").map(_.observationIds) should contain(security.observationIds)
    }

    "project UX capabilities without deriving conclusions in the renderer" in {
      Given("one canonical UX observation mapped to the Web user-experience capability")
      val original = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
      val source = original.observations.head
      val uxobservation = source.copy(
        id = ReviewObservationId("report-assurance-web-ux"),
        mappings = ReviewMappings(
          Vector.empty,
          Vector.empty,
          Vector(ReviewCapabilityId("quality.ux.web"))
        )
      )
      val report = original.copy(observations = original.observations :+ uxobservation)

      When("CBD projects the UX view from canonical quality mappings")
      val views = CarReviewViewProjection.project(report)

      Then("the UX and quality views share the same canonical observation and evidence identities")
      val ux = views.namedView("ux").flatMap(_.items.find(_.key == "quality.ux.web")).getOrElse(fail("UX projection missing"))
      ux.observationIds should contain(uxobservation.id)
      ux.evidenceIds should contain allElementsOf uxobservation.evidenceIds
      views.quality.find(_.key == "quality.ux.web").map(_.observationIds) should contain(ux.observationIds)
      views.namedViews.map(_.name) shouldBe CarReviewCapabilityCatalog.viewNames
      views.namedView("sustainability").map(_.items) should contain(Vector.empty)
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
