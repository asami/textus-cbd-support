package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.model.datatype.EntityId

/*
 * @since   Jul. 23, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewExecutionPlanSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The declared CAR Review execution plan" should {
    "retain the supplied native identity and refuse target, profile or reference inconsistencies" in {
      Given("an explicit native reference and complete target and profile selection")
      val reference = _reference
      val request = _request
      val later = request.copy(reviewId = ReviewId("review-later"), startedAt = ReviewInstant("2026-10-06T01:00:00Z"))
      val changedtarget = _input.copy(name = "different-car")
      val changedprofile = _input.copy(profile = ReviewProfile("release"))

      When("production creation receives valid, inconsistent and missing or invalid references")
      val plan = CarReviewExecutionPlan.create(request, reference, _input)
      val laterplan = CarReviewExecutionPlan.create(later, reference, _input)
      val targetresult = CarReviewExecutionPlan.create(request, reference, changedtarget)
      val profileresult = CarReviewExecutionPlan.create(request, reference, changedprofile)
      val invalid = Vector(null, reference.copy(revision = 0L), reference.copy(revision = -1L), reference.copy(diagnosisId = null))
        .map(CarReviewExecutionPlan.create(request, _, _input))
      val payloadonly = CarReviewExecutionPlan.create(request.copy(target = request.target.copy(digest = ReviewDigest("transport-payload"))), reference, _input)

      Then("the exact reference survives and Review ID, times or payload values issue no management identity")
      plan.map(_.reference) shouldBe Right(reference)
      plan.map(_.request) shouldBe Right(request)
      plan.map(_.reuseInput) shouldBe Right(_input)
      laterplan.map(_.reference) shouldBe Right(reference)
      payloadonly.map(_.reference) shouldBe Right(reference)
      targetresult.left.toOption.map(_.code) shouldBe Some("review-plan-target-mismatch")
      profileresult.left.toOption.map(_.code) shouldBe Some("review-plan-profile-mismatch")
      invalid.foreach(_.left.toOption.map(_.code) shouldBe Some("review-plan-reference-invalid"))
    }

    "refuse a selected provider rule absent from the declared rule selection" in {
      Given("a provider whose selected rule is absent from the declared rules")
      val unbound = _input.copy(providerSelections = Vector(_provider.copy(ruleSet = ReviewRuleIdentity(ReviewRuleId("rule-unbound"), ReviewVersion("1.0")))))

      When("production creation admits that selection")
      val result = CarReviewExecutionPlan.create(_request, _reference, unbound)

      Then("the plan preserves the attributed rule-membership refusal")
      result.left.toOption.map(_.code) shouldBe Some("unbound-reuse-provider-rule")
    }
  }

  private def _reference: CarReviewExecutionPlanReference =
    CarReviewExecutionPlanReference(EntityId.parse("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planFixture")
      .toOption.getOrElse(fail("Invalid declared fixture identity")), 7L)

  private val _target = ReviewTarget(ReviewTargetKind("car"), Some("org.textus"), "textus-cbd-support", Some(ReviewVersion("0.1.0-SNAPSHOT")), ReviewDigest("sha256:" + "a" * 64))
  private val _provider = CarReviewReuseProviderSelection(ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
    ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")), CarReviewDeclaredReference("availability:cozy", 1L))
  private def _input: CarReviewReuseKeyInput =
    CarReviewReuseKeyInput(CarReviewReuseKey.DEFINITION_ID, ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      CarReviewDeclaredReference("target:main", 1L), _target.kind, _target.organization, _target.name, _target.version,
      ReviewProfile("development"), None, Vector(_provider.ruleSet), Vector(_provider), Vector.empty,
      Vector("profile", "gate", "reconciliation", "suppression").map(scope => CarReviewReusePolicyBinding(scope, scope + "-default", ReviewVersion("1.0"), 1L)))
  private def _request: ReviewStartRequest =
    ReviewStartRequest(ReviewId("review-1"), _target, ReviewProfile("development"), ReviewInstant("2026-07-23T00:00:00Z"))
}
