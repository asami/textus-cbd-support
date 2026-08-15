package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewProviderExecutionCoordinatorSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review Provider execution coordination" should {
    "admit one bounded provider result and never re-run its admitted request digest" in {
      Given("one enabled Cozy provider returning the canonical evidence bundle")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val runner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val request = _request()

      When("CBD executes the same provider request twice")
      val first = coordinator.execute(request, runner)
      val second = coordinator.execute(request, runner)

      Then("the first result is admitted and the second returns the admitted bundle without provider work")
      first should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(AdmittedProviderBundle(ReviewProviderIdentity(ReviewProviderId("cozy"), _), _, _, _, _, _, _), false, _) =>
      }
      second should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(_, true, bundle) if bundle == _bundle =>
      }
      runner.executions shouldBe 1
      runner.cancellations shouldBe 0
    }

    "never reuse an enabled admitted bundle for disabled availability or a different full descriptor document" in {
      Given("one admitted enabled request, a disabled repeat, and a descriptor change outside the projected descriptor")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val runner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val request = _request()
      val changeddescriptor = _descriptor.replace("runtime-evidence-not-supported", "runtime-evidence-alternate")

      When("CBD repeats the request with disabled availability and then with the changed valid descriptor document")
      val first = coordinator.execute(request, runner)
      val disabled = coordinator.execute(request.copy(availability = ProviderBundleAvailability.Disabled), runner)
      val changed = coordinator.execute(request.copy(descriptor = changeddescriptor), runner)

      Then("disabled execution is attributable refusal and the changed descriptor cannot return the cached bundle")
      first should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(_, false, bundle) if bundle == _bundle =>
      }
      disabled should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("disabled"), ReviewLimitation("provider-disabled", _, _, _, _), false)) =>
      }
      changed should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(_, false, bundle) if bundle == _bundle =>
      }
      runner.executions shouldBe 2
      runner.cancellations shouldBe 0
    }

    "cancel a provider exceeding its admitted timeout and preserve a failed provider outcome" in {
      Given("a provider result which arrives one millisecond after the request timeout")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val runner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 120001L))

      When("CBD checks the request timeout before admitting the returned bundle")
      val outcome = coordinator.execute(_request(), runner)

      Then("the provider is cancelled and its timeout remains an attributable run failure")
      outcome should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(Some(ReviewProviderIdentity(ReviewProviderId("cozy"), _)), ReviewProviderState("failed"), ReviewLimitation("provider-timeout", _, _, _, _), true)) =>
      }
      runner.executions shouldBe 1
      runner.cancellations shouldBe 1
    }

    "honor cancellation and provider failure without silently trying another provider" in {
      Given("one cancelled invocation and one provider-owned failed invocation")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val cancelledrunner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val failedrunner = new FakeRunner(ProviderBundleRunnerResult.Failed("provider-transport-failed", "connection refused", 1000L))

      When("CBD processes both terminal provider outcomes")
      val cancelled = coordinator.execute(_request(cancellationrequested = true), cancelledrunner)
      val failed = coordinator.execute(_request(reviewid = "review-example-002"), failedrunner)

      Then("cancellation stops the runner while provider failure retains its exact attributable code")
      cancelled should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("cancelled"), ReviewLimitation("provider-cancelled", _, _, _, _), false)) =>
      }
      failed should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("failed"), ReviewLimitation("provider-transport-failed", _, _, _, _), true)) =>
      }
      cancelledrunner.executions shouldBe 0
      cancelledrunner.cancellations shouldBe 1
      failedrunner.executions shouldBe 1
      failedrunner.cancellations shouldBe 0
    }

    "refuse a provider completion timestamp that predates its admitted start" in {
      Given("a provider result carrying a clock value before the CBD execution start")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val runner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, -1L))

      When("CBD checks the deterministic execution interval")
      val outcome = coordinator.execute(_request(), runner)

      Then("the impossible time is retained as a provider-attributed failed result")
      outcome should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("failed"), ReviewLimitation("provider-time-invalid", _, _, _, _), true)) =>
      }
      runner.executions shouldBe 1
      runner.cancellations shouldBe 0
    }

    "execute only the runner bound to the exact registered provider descriptor" in {
      Given("a registered Cozy provider and its canonical request")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val registry = new CarReviewProviderRegistry()
      val runner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      registry.register(_descriptor, runner, _deterministic_policy).isRight shouldBe true

      When("CBD selects the registered provider through the coordinator")
      val result = coordinator.execute(_request(), registry)

      Then("only the registered runner executes and its admitted evidence remains attributable")
      result should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(AdmittedProviderBundle(ReviewProviderIdentity(ReviewProviderId("cozy"), _), _, _, _, _, _, _), false, _) =>
      }
      runner.executions shouldBe 1
    }

    "refuse unregistered or descriptor-mismatched runners before provider execution" in {
      Given("an empty registry and a conflicting descriptor for the same provider identity")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val registry = new CarReviewProviderRegistry()
      val runner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val conflicting = _descriptor.replace("\"version\": \"1.0.0\"", "\"version\": \"1.0.1\"")

      When("the unregistered and mismatched requests reach selection")
      val unavailable = coordinator.execute(_request(), registry)
      registry.register(_descriptor, runner, _deterministic_policy).isRight shouldBe true
      val mismatched = coordinator.execute(_request(descriptor = conflicting), registry)

      Then("neither path invokes an arbitrary runner")
      unavailable should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("unavailable"), ReviewLimitation("provider-not-registered", _, _, _, _), false)) =>
      }
      mismatched should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("incompatible"), ReviewLimitation("provider-registration-mismatch", _, _, _, _), false)) =>
      }
      runner.executions shouldBe 0
    }

    "refuse a static Assurance through a registered Runtime policy while admitting it through a deterministic policy" in {
      Given("the same static provider bundle registered once with Runtime authority and once with deterministic authority")
      val runtimecoordinator = new CarReviewProviderExecutionCoordinator()
      val deterministiccoordinator = new CarReviewProviderExecutionCoordinator()
      val runtimeregistry = new CarReviewProviderRegistry()
      val deterministicregistry = new CarReviewProviderRegistry()
      val runtimerunner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val deterministicrunner = new FakeRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      runtimeregistry.register(_descriptor, runtimerunner, _runtime_policy).isRight shouldBe true
      deterministicregistry.register(_descriptor, deterministicrunner, _deterministic_policy).isRight shouldBe true

      When("both coordinator registry routes execute the static Assurance bundle")
      val refused = runtimecoordinator.execute(_request(), runtimeregistry)
      val admitted = deterministiccoordinator.execute(_request(), deterministicregistry)

      Then("Runtime authority retains the missing runtime Evidence refusal while deterministic authority admits the canonical bundle")
      refused should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("incompatible"), ReviewLimitation("runtime-assurance-evidence-required", _, _, _, _), false)) =>
      }
      admitted shouldBe a[ProviderBundleExecutionOutcome.Admitted]
      runtimerunner.executions shouldBe 1
      deterministicrunner.executions shouldBe 1
    }
  }

  private val _descriptor = _load("car-review-provider-descriptor-v1.json")
  private val _providerrequest = _load("car-review-provider-request-v1.json")
  private val _bundle = _load("car-review-evidence-bundle-v1.json")
  private val _deterministic_policy = CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Deterministic, declaredCostUnits = 0L, maximumCostUnits = 0L)
  private val _runtime_policy = CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Runtime, declaredCostUnits = 0L, maximumCostUnits = 0L)

  private def _request(
    reviewid: String = "review-example-001",
    cancellationrequested: Boolean = false,
    descriptor: String = _descriptor
  ): ProviderBundleExecutionRequest =
    ProviderBundleExecutionRequest(
      ReviewId(reviewid),
      ReviewTarget(
        ReviewTargetKind("project"),
        Some("org.textus"),
        "textus-user-account",
        Some(ReviewVersion("0.2.0-SNAPSHOT")),
        ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
      ),
      ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14")),
      ProviderBundleAvailability.Enabled,
      descriptor,
      _providerrequest.replace("review-example-001", reviewid),
      startedAtMillis = 0L,
      cancellationRequested = cancellationrequested
    )

  private def _load(name: String): String = Files.readString(Path.of("docs", "spec", "examples", name))

  private final class FakeRunner(result: ProviderBundleRunnerResult) extends CarReviewProviderRunner {
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
