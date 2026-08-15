package org.simplemodeling.textus.cbdsupport

import org.goldenport.cncf.context.ExecutionContext
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewProductionExecutionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The production Review execution factory" should {
    "freeze server-owned production capsules" which {
    "freeze and execute each supported server-owned profile through the registered provider" in {
      Given("one digest-bound target and all four supported profile names")
      given ExecutionContext = ExecutionContext.test()
      val profiles = Vector("development", "ci", "release", "server")

      When("CBD creates and runs a server-owned execution capsule for each profile")
      val executions = profiles.map { profile =>
        val execution = CarReviewProductionExecution.create(_request(profile)).fold(_fail, identity)
        execution -> execution.execute(Set("reviewer"), _completed_at).fold(_fail, identity)
      }

      Then("each response has one frozen provider/rule attribution and unknown-only static evidence")
      executions.foreach { case (execution, response) =>
        execution.plan.reuseKey.definitionId shouldBe CarReviewReuseKey.DEFINITION_ID
        execution.plan.reuseInput.policyBindings.map(_.scope).toSet shouldBe Set("profile", "gate", "reconciliation", "suppression")
        execution.plan.reuseInput.providerSelections.map(_.availabilityPolicyDigest.value).head should fullyMatch regex "sha256:[0-9a-f]{64}"
        response.report.profile shouldBe execution.plan.request.profile
        response.report.execution.startedAt shouldBe execution.plan.request.startedAt
        response.report.execution.completedAt shouldBe _completed_at
        response.report.execution.providers.map(_.provider.id.value) shouldBe Vector("cbd-initial-static-quality")
        response.report.execution.providers.map(_.ruleSet.id.value) shouldBe Vector("cbd-initial-static-quality.car-review")
        response.report.observations.forall(value => value.`type`.value == "unknown") shouldBe true
        response.report.observations.exists(value => value.`type`.value == "assurance" || value.`type`.value == "finding") shouldBe false
        response.report.evidence shouldBe Vector.empty
        val delivery = CarReviewDeliveryProjection.project(response.report)
        delivery.qualityCoverage.size shouldBe CarReviewCapabilityCatalog.definitions.size
      }
    }

    "retain reuse identity across report IDs and distinguish the selected production profile" in {
      Given("the same request and two different server-owned report identifiers")
      given ExecutionContext = ExecutionContext.test()
      val request = _request("development")

      When("CBD creates capsules for the same and a different selected profile")
      val first = CarReviewProductionExecution.create(request).fold(_fail, identity)
      val second = CarReviewProductionExecution.create(request).fold(_fail, identity)
      val changed = CarReviewProductionExecution.create(_request("ci")).fold(_fail, identity)

      Then("the report identifier is excluded while profile semantics remain part of the reuse identity")
      first.plan.reuseKey shouldBe second.plan.reuseKey
      first.plan.reuseKey should not be changed.plan.reuseKey
      first.execute(Set("reviewer"), _completed_at).fold(_fail, identity).report.reportId should not be second.execute(Set("reviewer"), _completed_at).fold(_fail, identity).report.reportId
    }

    "exclude only server-generated Review identity from the reusable production selection" in {
      Given("two otherwise identical production starts with distinct server-generated Review IDs")
      given ExecutionContext = ExecutionContext.test()
      val firstrequest = _request("development").copy(reviewId = ReviewId("review-production-reuse-a"))
      val secondrequest = _request("development").copy(reviewId = ReviewId("review-production-reuse-b"))

      When("CBD creates both server-owned execution capsules")
      val first = CarReviewProductionExecution.create(firstrequest).fold(_fail, identity)
      val second = CarReviewProductionExecution.create(secondrequest).fold(_fail, identity)
      val firstresponse = first.execute(Set("reviewer"), _completed_at).fold(_fail, identity)
      val secondresponse = second.execute(Set("reviewer"), _completed_at).fold(_fail, identity)

      Then("the executable requests retain their identities while the plan reuse identity remains the same")
      first.plan.request.reviewId shouldBe firstrequest.reviewId
      second.plan.request.reviewId shouldBe secondrequest.reviewId
      first.plan.reuseInput.providerSelections.head.availabilityPolicyDigest shouldBe second.plan.reuseInput.providerSelections.head.availabilityPolicyDigest
      first.plan.reuseKey shouldBe second.plan.reuseKey
      firstresponse.report.reviewId shouldBe firstrequest.reviewId
      secondresponse.report.reviewId shouldBe secondrequest.reviewId
    }

    }
    "reject invalid production boundaries" which {
    "reject unsupported profiles, malformed identities, and malformed start instants before producing a capsule" in {
      Given("requests that fail the server-owned profile, identity, or strict start-time boundary")
      given ExecutionContext = ExecutionContext.test()

      When("CBD creates invalid production execution capsules")
      val unsupported = CarReviewProductionExecution.create(_request("caller-controlled"))
      val malformedidentity = CarReviewProductionExecution.create(_request("development").copy(reviewId = ReviewId("raw request=credential")))
      val oversizedidentity = CarReviewProductionExecution.create(_request("development").copy(reviewId = ReviewId("a" * 181)))
      val malformed = CarReviewProductionExecution.create(_request("development").copy(startedAt = ReviewInstant("not-an-instant")))

      Then("all failures are stable and no invalid request produces a capsule")
      unsupported.fold(error => error.toString should include ("review-profile-unsupported"), _ => fail("Unexpected production execution."))
      malformedidentity.fold(error => error.toString should include ("review-id-invalid"), _ => fail("Unexpected production execution."))
      oversizedidentity.fold(error => error.toString should include ("review-id-invalid"), _ => fail("Unexpected production execution."))
      malformed.fold(error => error.toString should include ("review-started-at-invalid"), _ => fail("Unexpected production execution."))
    }

    "reject malformed and pre-start completion times before provider execution" in {
      Given("one valid server-owned production execution with a frozen start time")
      given ExecutionContext = ExecutionContext.test()
      val execution = CarReviewProductionExecution.create(_request("development")).fold(_fail, identity)

      When("CBD receives malformed or pre-start server completion times")
      val malformed = execution.execute(Set("reviewer"), ReviewInstant("not-an-instant"))
      val beforestart = execution.execute(Set("reviewer"), ReviewInstant("2026-08-14T23:59:59Z"))

      Then("both fail through stable completion-time validation without a canonical response")
      malformed.fold(error => error.toString should include ("review-completed-at-invalid"), _ => fail("Unexpected canonical response."))
      beforestart.fold(error => error.toString should include ("review-completed-before-start"), _ => fail("Unexpected canonical response."))
    }
    }
  }

  private def _request(profile: String): ReviewStartRequest =
    ReviewStartRequest(
      ReviewId(s"review-production-$profile"),
      ReviewTarget(
        ReviewTargetKind("car"),
        Some("org.simplemodeling"),
        "textus-cbd-support",
        Some(ReviewVersion("0.1.0-SNAPSHOT")),
        ReviewDigest("sha256:" + ("a" * 64))
      ),
      ReviewProfile(profile),
      ReviewInstant("2026-08-15T00:00:00Z")
    )

  private val _completed_at = ReviewInstant("2026-08-15T00:01:00Z")

  private def _fail(error: Any): Nothing = fail(error.toString)
}
