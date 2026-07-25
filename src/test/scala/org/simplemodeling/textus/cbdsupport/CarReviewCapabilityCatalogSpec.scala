package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 25, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewCapabilityCatalogSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review capability catalog" should {
    "reuse canonical Evidence and Observation mappings without executing another provider" in {
      Given("one canonical Report")
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)

      When("CBD projects reusable capabilities")
      val definitions = CarReviewCapabilityCatalog.definitions
      val capabilities = CarReviewCapabilityCatalog.project(report)

      Then("the complete taxonomy has stable unique identities and mapped views retain canonical IDs")
      definitions should have size 157
      definitions.map(_.id).distinct should have size 157
      capabilities.map(_.capability.id.value) should contain allOf (
        "quality.domain.identity-consistency",
        "quality.documentation.rationale",
        "quality.observability.runtime-evidence"
      )
      val documentation = capabilities.find(_.capability.id.value == "quality.documentation.rationale").getOrElse(fail("documentation capability missing"))
      documentation.observationIds.map(_.value) should contain("report-finding-missing-rationale")
      documentation.evidenceIds.map(_.value) should contain("report-evidence-project-yaml")
      val evaluability = CarReviewCapabilityCatalog.definition(ReviewCapabilityId("quality.evaluability.corpus-first-experiment")).getOrElse(fail("evaluability capability missing"))
      evaluability.views should contain("evaluability")
      evaluability.assessmentFocus should include("corpus")
      evaluability.runtimeEvidenceRequired shouldBe true
      CarReviewCapabilityCatalog.definition(ReviewCapabilityId("quality.observability.runtime-evidence")).exists(_.runtimeEvidenceRequired) shouldBe true
      CarReviewCapabilityCatalog.capabilityIdsForView("security").map(_.value) should contain allElementsOf Set(
        "quality.security.authentication",
        "quality.security.authorization",
        "quality.security.auditability",
        "quality.security.domain.bounded-text-datatype",
        "quality.security.domain.constrained-numeric-datatype",
        "quality.security.infrastructure.immutable",
        "quality.security.cyber-resilience",
        "quality.security.moving-target-defense",
        "quality.security.supply-chain.artifact-provenance",
        "quality.privacy.data-minimization",
        "quality.safety.blast-radius-containment",
        "quality.ai-trustworthiness.secure-resilient"
      )
      CarReviewCapabilityCatalog.capabilityIdsForView("observability").map(_.value) should contain allElementsOf Set(
        "quality.evaluability.corpus-first-experiment",
        "quality.observability.runtime-evidence",
        "quality.observability.structured-logging",
        "quality.observability.distributed-tracing",
        "quality.observability.metrics-visualization",
        "quality.observability.state-visibility",
        "quality.operability.tracing",
        "quality.operability.metrics",
        "quality.security.auditability"
      )
      Vector("performance", "security", "availability", "reliability", "scalability", "resilience", "operability", "observability", "deployability", "configurability", "maintainability", "extensibility", "reusability", "testability", "readability", "consistency", "portability", "interoperability", "evolvability", "cost-efficiency", "ai-readiness", "functional-suitability", "accessibility", "supply-chain", "privacy", "safety", "data-quality", "ai-trustworthiness", "compatibility", "business-continuity", "internationalization", "supportability", "compliance", "sustainability").foreach { view =>
        withClue(s"quality taxonomy view '$view': ") {
          CarReviewCapabilityCatalog.capabilityIdsForView(view) should not be empty
        }
      }
      val restidempotency = CarReviewCapabilityCatalog.definition(ReviewCapabilityId("quality.reliability.rest-request-idempotency")).getOrElse(fail("REST idempotency capability missing"))
      restidempotency.views should contain allElementsOf Vector("reliability", "resilience", "interoperability", "security", "observability")
      restidempotency.representativeEvidenceKinds should contain allElementsOf Vector("api-contract", "idempotency-policy", "concurrency-test", "runtime-observation")
      restidempotency.runtimeEvidenceRequired shouldBe true
      val webformidempotency = CarReviewCapabilityCatalog.definition(ReviewCapabilityId("quality.reliability.web-form-submission-idempotency")).getOrElse(fail("Web Form idempotency capability missing"))
      webformidempotency.views should contain allElementsOf Vector("reliability", "resilience", "ux", "web", "security", "observability")
      webformidempotency.representativeEvidenceKinds should contain allElementsOf Vector("web-form-contract", "idempotency-policy", "interaction-test", "concurrency-test", "runtime-observation")
      webformidempotency.runtimeEvidenceRequired shouldBe true
      CarReviewCapabilityCatalog.capabilityIdsForView("ux").map(_.value) should contain("quality.reliability.web-form-submission-idempotency")
      CarReviewCapabilityCatalog.capabilityIdsForView("reliability").map(_.value) should contain allOf (
        "quality.reliability.rest-request-idempotency",
        "quality.reliability.web-form-submission-idempotency"
      )
      CarReviewCapabilityCatalog.capabilityIdsForView("ux").map(_.value) should contain allElementsOf Set(
        "quality.ux.web",
        "quality.ux.cli",
        "quality.ux.skill-assisted",
        "quality.ux.cross-surface-consistency"
      )
      CarReviewCapabilityCatalog.viewNames shouldBe CarReviewCapabilityCatalog.viewNames.sorted
      CarReviewCapabilityCatalog.viewNames should contain allOf (
        "accessibility", "ai-trustworthiness", "business-continuity", "compliance",
        "data-quality", "functional-suitability", "internationalization", "privacy",
        "safety", "supply-chain", "supportability", "sustainability"
      )
      CarReviewCapabilityCatalog.capabilityIdsForView("ai-operability").map(_.value) should contain allOf (
        "quality.ai.operability.mcp",
        "quality.ai.operability.skill"
      )
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
