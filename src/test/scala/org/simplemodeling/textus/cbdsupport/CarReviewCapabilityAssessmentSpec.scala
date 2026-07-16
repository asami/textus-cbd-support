package org.simplemodeling.textus.cbdsupport

import io.circe.JsonObject
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewCapabilityAssessmentSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review named capability assessments" should {
    "specify and assess every required quality view from mapped attributable evidence" in {
      Given("the complete CBD quality capability catalog")
      val expected = Set(
        "quality.security.boundary",
        "quality.domain.identity-consistency",
        "quality.documentation.rationale",
        "quality.ai-readiness",
        "quality.resilience",
        "quality.testability",
        "quality.observability.runtime-evidence"
      )

      When("one representative assurance is mapped to each named capability")
      val results = CarReviewCapabilityCatalog.definitions.map { definition =>
        definition.assessmentFocus should not be empty
        definition.representativeEvidenceKinds should not be empty
        val evidence = _evidence(definition)
        val result = CarReviewCapabilityAssessment.build(
          definition.id,
          Vector(_observation(definition, evidence)),
          Vector(evidence),
          "cbd.default",
          ReviewVersion("1.0.0")
        )
        definition.id.value -> result
      }

      Then("each view has an explicit, attributable established assessment")
      results.map(_._1).toSet shouldBe expected
      results.foreach { case (id, result) =>
        val assessment = result.fold(error => fail(error), identity).assessment
        assessment.capabilityId.value shouldBe id
        assessment.maturity shouldBe ReviewMaturity("established")
        assessment.coverage shouldBe Some(ReviewCoverage(1, 1, 0, 10000))
        assessment.providerIds shouldBe Vector(ReviewProviderId("cozy"))
      }
    }

    "refuse an assessment for an unspecified capability" in {
      CarReviewCapabilityAssessment.build(ReviewCapabilityId("quality.unspecified"), Vector.empty, Vector.empty, "cbd.default", ReviewVersion("1.0.0")) shouldBe Left("Unknown Review capability 'quality.unspecified'.")
    }
  }

  private val _provider = ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))
  private val _rule = ReviewRuleIdentity(ReviewRuleId("cozy.car-review"), ReviewVersion("1.0.0"))
  private val _digest = ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")

  private def _evidence(definition: CarReviewCapabilityDefinition): ReviewEvidence =
    ReviewEvidence(ReviewEvidenceId(s"cozy:${definition.id.value}"), definition.representativeEvidenceKinds.head, ReviewSubject("component", definition.id.value), _provider.id, _digest, definition.id.value, None, None, JsonObject.empty)

  private def _observation(definition: CarReviewCapabilityDefinition, evidence: ReviewEvidence): ReviewObservation =
    ReviewObservation(ReviewObservationId(s"cozy:${definition.id.value}"), ReviewObservationType("assurance"), _rule, evidence.subject, definition.assessmentFocus, definition.assessmentFocus, None, ReviewConfidence("high"), Vector(evidence.id), Vector.empty, ReviewProviderAttribution(_provider, _rule, _digest), ReviewDisposition(ReviewDispositionState("active"), None, None, None), ReviewMappings(Vector.empty, Vector.empty, Vector(definition.id)))
}
