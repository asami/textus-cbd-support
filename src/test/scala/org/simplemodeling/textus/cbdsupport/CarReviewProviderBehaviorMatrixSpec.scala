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
/**
 * P5-24's cross-boundary provider behavior matrix. Detailed boundary specs
 * retain their own contracts; this matrix keeps the required outcomes visible
 * as one executable phase-level statement.
 */
final class CarReviewProviderBehaviorMatrixSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "P5-24 CAR Review provider behavior matrix" should {
    "admit compatible evidence while preserving its provider identity and explicit limitation" in {
      Given("one compatible Cozy descriptor, provider request, and evidence bundle")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))

      When("CBD executes and admits the provider bundle")
      val outcome = coordinator.execute(_request(), runner)

      Then("provider identity and the provider-owned runtime limitation remain attributable")
      outcome match {
        case ProviderBundleExecutionOutcome.Admitted(value, false) =>
          value.provider shouldBe _provider
          value.limitations shouldBe Vector(
            ReviewLimitation(
              "runtime-evidence-not-supported",
              ReviewLimitationScope("capability"),
              Some("cozy.car-analysis"),
              "Operational maturity cannot be assessed from Cozy static evidence.",
              false
            )
          )
        case _ => fail("Compatible provider evidence was not admitted.")
      }
      runner.executions shouldBe 1
    }

    "refuse incompatible target-digest evidence without changing the provider identity" in {
      Given("one otherwise valid bundle whose target digest belongs to another CAR")
      val mismatched = _replace(_bundle, _targetdigest, _otherdigest)
      val context = ProviderBundleAdmissionContext(
        ReviewId("review-example-001"),
        _target,
        ProviderBundleAvailability.Enabled,
        _descriptor,
        _providerrequest,
        mismatched
      )

      When("CBD applies the provider admission contract")
      val outcome = CarReviewProviderBundleAdmission.admit(context)

      Then("the result is incompatible rather than reattributing or repairing the evidence")
      outcome should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(Some(`_provider`), ReviewProviderState("incompatible"), ReviewLimitation("bundle-target-mismatch", _, _, _, _), false)) =>
      }
    }

    "cancel, time out, and de-duplicate provider work without a fallback" in {
      Given("independent cancelled, overdue, and repeated compatible provider executions")
      val cancelledrunner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val timeoutrunner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 120001L))
      val duplicaterunner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val coordinator = new CarReviewProviderExecutionCoordinator()

      When("CBD receives cancellation, timeout, and a repeat of the same request digest")
      val cancelled = coordinator.execute(_request(cancellationrequested = true), cancelledrunner)
      val timedout = coordinator.execute(_request(reviewid = "review-example-002"), timeoutrunner)
      val first = coordinator.execute(_request(), duplicaterunner)
      val duplicate = coordinator.execute(_request(), duplicaterunner)

      Then("each terminal state is attributable and only the first duplicate request invokes the provider")
      cancelled should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(Some(`_provider`), ReviewProviderState("cancelled"), ReviewLimitation("provider-cancelled", _, _, _, _), false)) =>
      }
      timedout should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(Some(`_provider`), ReviewProviderState("failed"), ReviewLimitation("provider-timeout", _, _, _, _), true)) =>
      }
      first should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(_, false) =>
      }
      duplicate should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(_, true) =>
      }
      cancelledrunner.executions shouldBe 0
      cancelledrunner.cancellations shouldBe 1
      timeoutrunner.executions shouldBe 1
      timeoutrunner.cancellations shouldBe 1
      duplicaterunner.executions shouldBe 1
    }
  }

  private val _descriptor = _load("car-review-provider-descriptor-v1.json")
  private val _providerrequest = _load("car-review-provider-request-v1.json")
  private val _bundle = _load("car-review-evidence-bundle-v1.json")
  private val _targetdigest = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  private val _otherdigest = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
  private val _provider = ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))
  private val _target = ReviewTarget(
    ReviewTargetKind("project"),
    Some("org.textus"),
    "textus-user-account",
    Some(ReviewVersion("0.2.0-SNAPSHOT")),
    ReviewDigest(_targetdigest)
  )

  private def _request(
    reviewid: String = "review-example-001",
    cancellationrequested: Boolean = false
  ): ProviderBundleExecutionRequest =
    ProviderBundleExecutionRequest(
      ReviewId(reviewid),
      _target,
      _provider,
      ProviderBundleAvailability.Enabled,
      _descriptor,
      _providerrequest.replace("review-example-001", reviewid),
      startedAtMillis = 0L,
      cancellationRequested = cancellationrequested
    )

  private def _load(name: String): String = Files.readString(Path.of("docs", "spec", "examples", name))
  private def _replace(value: String, from: String, to: String): String = value.replace(from, to)

  private final class RecordingRunner(result: ProviderBundleRunnerResult) extends CarReviewProviderRunner {
    private var _executions = 0
    private var _cancellations = 0

    def executions: Int = _executions
    def cancellations: Int = _cancellations

    def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult = {
      val _ = request
      _executions += 1
      result
    }

    def cancel(request: ProviderBundleExecutionRequest): Unit = {
      val _ = request
      _cancellations += 1
    }
  }
}
