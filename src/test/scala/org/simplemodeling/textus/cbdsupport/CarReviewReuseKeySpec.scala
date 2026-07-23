package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewReuseKeySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review diagnosis reuse key" should {
    "retain one digest when canonical selections arrive in a different order" in {
      Given("one complete diagnosis input with rules, providers, evidence snapshots, and policies")
      val input = _input
      val reordered = input.copy(
        ruleSets = input.ruleSets.reverse,
        providerSelections = input.providerSelections.reverse,
        evidenceSnapshots = input.evidenceSnapshots.reverse,
        policyBindings = input.policyBindings.reverse
      )

      When("both semantically identical requests calculate their reuse keys")
      val first = _key(input)
      val second = _key(reordered)

      Then("array arrival order and omitted volatile Run metadata cannot split reusable work")
      first shouldBe second
      first.definitionId shouldBe CarReviewReuseKey.DEFINITION_ID
    }

    "invalidate reuse for every target, profile, rule, provider, runtime evidence, baseline, and policy input" in {
      Given("one canonical diagnosis reuse input")
      val input = _input
      val expected = _key(input)
      val changed = Vector(
        input.copy(target = input.target.copy(digest = _digest('b'))),
        input.copy(profile = ReviewProfile("release")),
        input.copy(ruleSets = Vector(ReviewRuleIdentity(ReviewRuleId("rule-security"), ReviewVersion("2.0")))),
        input.copy(providerSelections = Vector(_provider.copy(provider = ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("2.0"))))),
        input.copy(evidenceSnapshots = Vector(_runtime.copy(digest = _digest('c')))),
        input.copy(baselineDigest = Some(_digest('d'))),
        input.copy(policyBindings = _policies.updated(1, _policies(1).copy(policyDigest = _digest('e'))))
      )

      When("each conclusion-affecting input changes independently")
      val keys = changed.map(_key)

      Then("no changed input reuses the prior diagnosis key")
      keys.foreach(_ should not equal expected)
    }

    "refuse ambiguous provider selections and incomplete policy scope" in {
      Given("a request with either duplicate provider identity or one missing policy scope")
      val duplicate = _input.copy(providerSelections = Vector(_provider, _provider.copy(ruleSet = ReviewRuleIdentity(ReviewRuleId("rule-domain"), ReviewVersion("1.0")))))
      val incomplete = _input.copy(policyBindings = _policies.filterNot(_.scope == "suppression"))

      When("the calculator validates the canonical input")
      val duplicatefailure = CarReviewReuseKey.calculate(duplicate)
      val incompletefailure = CarReviewReuseKey.calculate(incomplete)

      Then("it rejects an incomplete identity instead of guessing a reusable conclusion")
      duplicatefailure.left.toOption.map(_.code) shouldBe Some("duplicate-reuse-provider")
      incompletefailure.left.toOption.map(_.code) shouldBe Some("invalid-reuse-policy-scopes")
    }

    "reject an incompatible Review schema while preserving distinct colon-qualified Evidence identities" in {
      Given("a request with an unsupported document schema and two distinct Evidence snapshot identities")
      val unsupported = _input.copy(reviewSchemaVersion = ReviewSchemaVersion("textus.cbd.review-report.v2"))
      val separated = _input.copy(evidenceSnapshots = Vector(
        _runtime.copy(evidenceClass = "runtime:source", snapshotId = "main"),
        _runtime.copy(evidenceClass = "runtime", snapshotId = "source:main")
      ))

      When("the calculator validates both input identities")
      val unsupportedfailure = CarReviewReuseKey.calculate(unsupported)
      val separatedkey = CarReviewReuseKey.calculate(separated)

      Then("it refuses an unrenderable Review schema without conflating structured Evidence identities")
      unsupportedfailure.left.toOption.map(_.code) shouldBe Some("incompatible-reuse-schema")
      separatedkey.isRight shouldBe true
    }
  }

  private val _provider = CarReviewReuseProviderSelection(
    ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
    ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")),
    _digest('f')
  )
  private val _runtime = CarReviewReuseEvidenceSnapshot(
    "runtime",
    "runtime-snapshot-main",
    ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")),
    _digest('a')
  )
  private val _policies = Vector(
    CarReviewReusePolicyBinding("profile", "profile-development", ReviewVersion("1.0"), _digest('1')),
    CarReviewReusePolicyBinding("gate", "gate-default", ReviewVersion("1.0"), _digest('2')),
    CarReviewReusePolicyBinding("reconciliation", "reconciliation-default", ReviewVersion("1.0"), _digest('3')),
    CarReviewReusePolicyBinding("suppression", "suppression-default", ReviewVersion("1.0"), _digest('4'))
  )

  private def _input: CarReviewReuseKeyInput =
    CarReviewReuseKeyInput(
      CarReviewReuseKey.DEFINITION_ID,
      ReviewSchemaVersion("textus.cbd.review-report.v1"),
      ReviewTarget(ReviewTargetKind("car"), Some("org.textus"), "textus-cbd-support", Some(ReviewVersion("0.1.0-SNAPSHOT")), _digest('0')),
      ReviewProfile("development"),
      Some(_digest('9')),
      Vector(ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0"))),
      Vector(_provider),
      Vector(_runtime),
      _policies
    )

  private def _key(input: CarReviewReuseKeyInput): CarReviewReuseKey =
    CarReviewReuseKey.calculate(input).fold(error => fail(s"${error.code}: ${error.message}"), identity)

  private def _digest(character: Char): ReviewDigest =
    ReviewDigest("sha256:" + character.toString * 64)
}
