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
final class CarReviewAssessmentGateBuilderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review assessment and gate builder" should {
    "build deterministic coverage and an unknown gate without converting uncertainty to assurance" in {
      Given("one evidence-backed assurance and one Unknown for distinct subjects")
      val input = _input(Vector(_observation("assurance", "assured", Vector(_evidence.id)), _observation("unknown", "unknown", Vector.empty)))

      When("CBD derives the assessment and profile gate after reconciliation")
      val result = CarReviewAssessmentGateBuilder.build(input)

      Then("coverage, provider attribution, maturity, and gate uncertainty remain explicit")
      result.assessment.coverage shouldBe Some(ReviewCoverage(2, 1, 1, 5000))
      result.assessment.maturity shouldBe ReviewMaturity("unassessed")
      result.assessment.providerIds shouldBe Vector(ReviewProviderId("cozy"))
      result.gate.result shouldBe ReviewGateResult("unknown")
      result.gate.blockingObservationIds shouldBe Vector.empty
    }

    "fail only through canonical Findings and retain their exact blocking identities" in {
      Given("one canonical Finding and one Assurance")
      val input = _input(Vector(_observation("finding", "failure", Vector(_evidence.id)), _observation("assurance", "assured", Vector(_evidence.id))))

      When("CBD applies the selected profile policy")
      val result = CarReviewAssessmentGateBuilder.build(input)

      Then("the gate fails with the Finding ID rather than a provider-level verdict")
      result.assessment.maturity shouldBe ReviewMaturity("partial")
      result.gate.result shouldBe ReviewGateResult("fail")
      result.gate.blockingObservationIds shouldBe Vector(ReviewObservationId("cozy:failure"))
    }

    "exclude advisory AI candidate Findings from deterministic gate decisions" in {
      Given("one deterministic Assurance and one AI advisory candidate Finding")
      val ai = _observation("finding", "candidate", Vector(_evidence.id)).copy(
        id = ReviewObservationId("textus-ai:candidate"),
        rule = ReviewRuleIdentity(ReviewRuleId("ai.advisory.documentation.clarity"), ReviewVersion("1.0.0"))
      )
      val input = _input(Vector(_observation("assurance", "assured", Vector(_evidence.id)), ai))

      When("CBD derives the deterministic profile gate")
      val result = CarReviewAssessmentGateBuilder.build(input)

      Then("the candidate remains reportable but cannot fail or pass the deterministic gate")
      result.gate.result shouldBe ReviewGateResult("pass")
      result.gate.blockingObservationIds shouldBe empty
      result.assessment.gaps should contain("AI candidate observations require deterministic or human corroboration.")
    }
  }

  private val _provider = ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))
  private val _rule = ReviewRuleIdentity(ReviewRuleId("cozy.car-review"), ReviewVersion("1.0.0"))
  private val _digest = ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
  private val _evidence = ReviewEvidence(ReviewEvidenceId("cozy:evidence"), "cml-model", ReviewSubject("component", "account"), _provider.id, _digest, "evidence", None, None, JsonObject.empty)

  private def _input(observations: Vector[ReviewObservation]): ReviewAssessmentGateInput =
    ReviewAssessmentGateInput(ReviewCapabilityId("quality.domain.identity-consistency"), observations, Vector(_evidence), "cbd.default", ReviewVersion("1.0.0"))

  private def _observation(observationtype: String, id: String, evidenceids: Vector[ReviewEvidenceId]): ReviewObservation =
    ReviewObservation(ReviewObservationId(s"cozy:$id"), ReviewObservationType(observationtype), _rule, ReviewSubject("component", id), id, id, if observationtype == "finding" then Some(ReviewSeverity("high")) else None, ReviewConfidence("high"), evidenceids, Vector.empty, ReviewProviderAttribution(_provider, _rule, _digest), ReviewDisposition(ReviewDispositionState("active"), None, None, None), ReviewMappings(Vector.empty, Vector.empty, Vector.empty))
}
