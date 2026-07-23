package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 23, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewExecutionPlanSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review execution plan" should {
    "freeze one complete reusable identity before provider work" in {
      Given("one admitted Review request and a complete conclusion-affecting reuse input")
      val request = _request

      When("the execution plan freezes the provider-work inputs")
      val plan = CarReviewExecutionPlan.create(request, _input)

      Then("the plan retains the request, input, and their canonical reuse key")
      plan.map(_.reuseKey) shouldBe CarReviewReuseKey.calculate(_input)
      plan.map(_.request) shouldBe Right(request)
      plan.map(_.reuseInput) shouldBe Right(_input)
    }

    "refuse a target or profile that differs from the admitted Review request" in {
      Given("reuse inputs whose target or profile differs from the admitted request")
      val changedtarget = _input.copy(target = _input.target.copy(digest = _digest('b')))
      val changedprofile = _input.copy(profile = ReviewProfile("release"))

      When("each inconsistent input is frozen into an execution plan")
      val targetresult = CarReviewExecutionPlan.create(_request, changedtarget)
      val profileresult = CarReviewExecutionPlan.create(_request, changedprofile)

      Then("the plan rejects each mismatch with its specific contract code")
      targetresult.left.toOption.map(_.code) shouldBe Some("review-plan-target-mismatch")
      profileresult.left.toOption.map(_.code) shouldBe Some("review-plan-profile-mismatch")
    }

    "refuse a selected provider rule that is absent from the frozen rule set" in {
      Given("one selected provider whose rule is absent from the frozen rule set")
      val unboundprovider = _input.copy(
        providerSelections = Vector(_provider.copy(ruleSet = ReviewRuleIdentity(ReviewRuleId("rule-unbound"), ReviewVersion("1.0"))))
      )

      When("the inconsistent provider selection is frozen")
      val result = CarReviewExecutionPlan.create(_request, unboundprovider)

      Then("the plan rejects the unbound provider rule")
      result.left.toOption.map(_.code) shouldBe Some("unbound-reuse-provider-rule")
    }
  }

  private val _target = ReviewTarget(
    ReviewTargetKind("car"),
    Some("org.textus"),
    "textus-cbd-support",
    Some(ReviewVersion("0.1.0-SNAPSHOT")),
    _digest('a')
  )
  private val _provider = CarReviewReuseProviderSelection(
    ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
    ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")),
    _digest('b')
  )
  private val _input = CarReviewReuseKeyInput(
    CarReviewReuseKey.DEFINITION_ID,
    ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
    _target,
    ReviewProfile("development"),
    None,
    Vector(_provider.ruleSet),
    Vector(_provider),
    Vector(CarReviewReuseEvidenceSnapshot(
      "runtime",
      "runtime-snapshot-main",
      ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")),
      _digest('c')
    )),
    Vector(
      CarReviewReusePolicyBinding("profile", "profile-development", ReviewVersion("1.0"), _digest('1')),
      CarReviewReusePolicyBinding("gate", "gate-default", ReviewVersion("1.0"), _digest('2')),
      CarReviewReusePolicyBinding("reconciliation", "reconciliation-default", ReviewVersion("1.0"), _digest('3')),
      CarReviewReusePolicyBinding("suppression", "suppression-default", ReviewVersion("1.0"), _digest('4'))
    )
  )
  private val _request = ReviewStartRequest(
    ReviewId("review-1"),
    _target,
    ReviewProfile("development"),
    ReviewInstant("2026-07-23T00:00:00Z")
  )

  private def _digest(character: Char): ReviewDigest =
    ReviewDigest("sha256:" + character.toString * 64)
}
