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
final class CarReviewProviderRegistrySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review Provider registry" should {
    "register a strict descriptor and discover it only for all requested capabilities" in {
      Given("one registered Cozy provider with its canonical descriptor")
      val registry = new CarReviewProviderRegistry()
      val runner = new NoopRunner()
      val registration = registry.register(_descriptor, runner, _deterministic_policy)

      When("CBD discovers providers for the advertised capability")
      val discovery = registry.discover(CarReviewProviderDiscoveryRequest(Vector(ReviewCapabilityId("cozy.car-analysis")), 1))

      Then("the exact descriptor and its local runner are selected without fallback")
      registration.toOption.map(_.provider) shouldBe Some(ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14")))
      discovery.toOption.map(_.map(_.provider.id.value)) shouldBe Some(Vector("cozy"))
      registry.runnerFor(ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))) shouldBe Some(runner)
    }

    "reject a second descriptor that changes an already registered provider identity" in {
      Given("a registered provider and another descriptor for the same implementation identity")
      val registry = new CarReviewProviderRegistry()
      registry.register(_descriptor, new NoopRunner(), _deterministic_policy).isRight shouldBe true
      val conflicting = _descriptor.replace("\"version\": \"1.0.0\"", "\"version\": \"1.0.1\"")

      When("the conflicting descriptor attempts registration")
      val result = registry.register(conflicting, new NoopRunner(), _deterministic_policy)

      Then("CBD preserves the original registration rather than choosing a descriptor")
      result.left.toOption.map(_.code) shouldBe Some("provider-registration-conflict")
    }

    "reject a runner substitution for an otherwise identical provider descriptor" in {
      Given("a provider identity registered with one local runner")
      val registry = new CarReviewProviderRegistry()
      val runner = new NoopRunner()
      registry.register(_descriptor, runner, _deterministic_policy).isRight shouldBe true

      When("another runner registers the same descriptor")
      val result = registry.register(_descriptor, new NoopRunner(), _deterministic_policy)

      Then("CBD preserves the implementation bound to the immutable registration")
      result.left.toOption.map(_.code) shouldBe Some("provider-runner-conflict")
      registry.runnerFor(ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))) shouldBe Some(runner)
    }

    "reject unbounded provider discovery requests before inspecting registrations" in {
      Given("one provider registry")
      val registry = new CarReviewProviderRegistry()

      When("a caller requests zero provider capacity")
      val result = registry.discover(CarReviewProviderDiscoveryRequest(Vector(ReviewCapabilityId("cozy.car-analysis")), 0))

      Then("the request is refused without a permissive default")
      result.left.toOption.map(_.code) shouldBe Some("invalid-provider-discovery-bound")
    }

    "refuse a descriptor that advertises an unknown canonical Observation kind" in {
      Given("a provider descriptor containing a non-v1 Observation kind")
      val registry = new CarReviewProviderRegistry()
      val invalid = _descriptor.replace("\"unknown\"", "\"unsupported-observation\"")

      When("the descriptor reaches strict provider registration")
      val result = registry.register(invalid, new NoopRunner(), _deterministic_policy)

      Then("CBD refuses the descriptor before it can become discoverable")
      result.left.toOption.map(_.code) shouldBe Some("descriptor-observation-kinds-invalid")
    }

    "refuse a descriptor whose declared limitation is malformed" in {
      Given("a provider descriptor with an unsupported limitation scope")
      val registry = new CarReviewProviderRegistry()
      val invalid = _descriptor.replace("\"scope\": \"capability\"", "\"scope\": \"unsupported\"")

      When("the descriptor reaches strict provider registration")
      val result = registry.register(invalid, new NoopRunner(), _deterministic_policy)

      Then("the descriptor is rejected before provider discovery")
      result.left.toOption.map(_.code) shouldBe Some("bundle-limitation-scope-invalid")
    }

    "retain an exact immutable policy and refuse invalid or conflicting policy registration" in {
      Given("one registered provider, its exact runner, and explicit quality policies")
      val registry = new CarReviewProviderRegistry()
      val runner = new NoopRunner()
      registry.register(_descriptor, runner, _deterministic_policy).isRight shouldBe true

      When("the same registration is repeated, then supplied with a different or invalid cost policy")
      val repeated = registry.register(_descriptor, runner, _deterministic_policy)
      val conflict = registry.register(_descriptor, runner, _runtime_policy)
      val invalid = registry.register(_descriptor, runner, CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Deterministic, declaredCostUnits = 2L, maximumCostUnits = 1L))

      Then("only exact idempotence succeeds and the registered policy cannot be replaced")
      repeated.isRight shouldBe true
      conflict.left.toOption.map(_.code) shouldBe Some("provider-policy-conflict")
      invalid.left.toOption.map(_.code) shouldBe Some("provider-cost-limit-exceeded")
      registry.registrationFor(ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))).map(_.qualityPolicy) shouldBe Some(_deterministic_policy)
    }
  }

  private val _descriptor = Files.readString(Path.of("docs", "spec", "examples", "car-review-provider-descriptor-v1.json"))
  private val _deterministic_policy = CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Deterministic, declaredCostUnits = 0L, maximumCostUnits = 0L)
  private val _runtime_policy = CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Runtime, declaredCostUnits = 0L, maximumCostUnits = 0L)

  private final class NoopRunner extends CarReviewProviderRunner {
    def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
      ProviderBundleRunnerResult.Failed("not-executed", request.provider.id.value, request.startedAtMillis)

    def cancel(request: ProviderBundleExecutionRequest): Unit = {
      val _ = request
    }
  }
}
