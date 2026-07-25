package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

final class CarReviewQualityRuleMatrixSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review quality rule matrix" should {
    "publish one stable, explicit Unknown-safe rule for every supported quality capability" in {
      Given("the CBD quality capability catalog and its derived rule matrix")
      val capabilities = CarReviewCapabilityCatalog.definitions
      val rules = CarReviewQualityRuleMatrix.rules

      When("every declared capability and its rule semantics are inspected")
      capabilities.foreach { capability =>
        CarReviewQualityRuleMatrix.rule(capability.id).map(_.capabilityId) shouldBe Some(capability.id)
      }

      Then("every capability has one stable rule with explicit evidence, Unknown, and maturity semantics")
      rules.map(_.checkId).distinct should have size rules.size
      rules.map(_.checkId.value) shouldBe rules.map(_.checkId.value).sorted
      rules.foreach { rule =>
        val basecheckid = s"cbd.car-review.${rule.capabilityId.value}"
        if rule.authority == CarReviewQualityCheckAuthority.Advisory then
          rule.checkId.value shouldBe s"$basecheckid.content"
        else
          rule.checkId.value shouldBe basecheckid
        rule.applicability shouldBe ReviewApplicability("applicable")
        rule.requiredEvidenceKinds should not be empty
        rule.requiredEvidenceKinds shouldBe rule.requiredEvidenceKinds.distinct.sorted
        rule.missingEvidenceObservationType shouldBe ReviewObservationType("unknown")
        rule.missingEvidenceLimitationCode shouldBe s"${rule.checkId.value}.evidence-unavailable"
        rule.authority match {
          case CarReviewQualityCheckAuthority.Runtime => rule.evidenceBackedMaturity shouldBe ReviewMaturity("operational")
          case CarReviewQualityCheckAuthority.Deterministic => rule.evidenceBackedMaturity shouldBe ReviewMaturity("partial")
          case CarReviewQualityCheckAuthority.Advisory => rule.evidenceBackedMaturity shouldBe ReviewMaturity("unassessed")
        }
      }
    }

    "make MCP and Skill AI-operability checks independently addressable" in {
      Given("the AI operability capabilities in the quality catalog")

      When("CBD resolves their primary and advisory rules")
      val mcprules = CarReviewQualityRuleMatrix.rulesFor(ReviewCapabilityId("quality.ai.operability.mcp"))
      val skillrules = CarReviewQualityRuleMatrix.rulesFor(ReviewCapabilityId("quality.ai.operability.skill"))

      Then("MCP and Skill support retain independent deterministic and advisory identities")
      CarReviewQualityRuleMatrix.rule(ReviewCapabilityId("quality.ai.operability.mcp")).map(_.checkId.value) shouldBe
        Some("cbd.car-review.quality.ai.operability.mcp")
      CarReviewQualityRuleMatrix.rule(ReviewCapabilityId("quality.ai.operability.skill")).map(_.checkId.value) shouldBe
        Some("cbd.car-review.quality.ai.operability.skill")
      mcprules.map(_.checkId.value) shouldBe
        Vector("cbd.car-review.quality.ai.operability.mcp", "cbd.car-review.quality.ai.operability.mcp.content")
      skillrules.map(_.checkId.value) shouldBe
        Vector("cbd.car-review.quality.ai.operability.skill", "cbd.car-review.quality.ai.operability.skill.content")
      mcprules.last.authority shouldBe
        CarReviewQualityCheckAuthority.Advisory
    }
  }
}
