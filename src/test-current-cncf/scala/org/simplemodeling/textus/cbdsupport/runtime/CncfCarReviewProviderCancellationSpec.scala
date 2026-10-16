package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct.  6, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CncfCarReviewProviderCancellationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import CarReviewJobExecutionFixtures.*

  "Native CAR Review provider cancellation" should {
    "return structured Unit through the actual ProviderCall" which {
      "records success and invokes one cancellation without execution (CANC1)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("an actual native-backed ActionCall, UOW interpreter and deterministic cancellable delegate")
          val wrapper = new CncfCarReviewProviderRunner(fixture.core, fixture.runner)

          When("the structured cancellation API executes its Unit ProviderCall")
          val result = fixture.native.managed(wrapper.cancelC(fixture.request))

          Then("Unit succeeds once and actual ProviderCall and step observations retain only safe identities")
          result shouldBe Consequence.success(())
          fixture.runner.cancellations shouldBe 1
          fixture.runner.executions shouldBe 0
          val trace = fixture.trace()
          trace should include("provider:cbd-car-review-provider.cancel")
          trace should include("cancel-car-review-provider")
          trace should include("outcome=success")
          trace should include("provider_id=cozy")
          _private_payloads_absent(trace, fixture)
        }
      }

      "bounds NonFatal failure inside the actual cancellation step and engine (CANC2)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("an actual native context and delegate cancellation throwing a private NonFatal marker")
          fixture.runner.cancellation = () => throw new IllegalStateException("PRIVATE-CANCELLATION-MARKER")
          val wrapper = new CncfCarReviewProviderRunner(fixture.core, fixture.runner)

          When("the actual CancellationCall interprets that delegate once")
          val result = fixture.native.managed(wrapper.cancelC(fixture.request))

          Then("the returned and observed failures are bounded and contain no private payload or exception text")
          failure(result) should include("provider-cancellation-failed")
          failure(result) should not include "PRIVATE-CANCELLATION-MARKER"
          fixture.runner.cancellations shouldBe 1
          fixture.runner.executions shouldBe 0
          val trace = fixture.trace()
          trace should include("provider:cbd-car-review-provider.cancel")
          trace should include("cancel-car-review-provider")
          trace should include("outcome=failure")
          trace should include("provider-cancellation-failed")
          trace should not include "PRIVATE-CANCELLATION-MARKER"
          _private_payloads_absent(trace, fixture)
        }
      }
    }

    "select the original registered cancellation boundary" which {
      "uses the exact typed descriptor and registered delegate through ProviderCall (CANC3)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("one real registered descriptor and a native-backed selected ActionCall")

          When("the application cancels the exact registered provider")
          val result = fixture.native.managed(fixture.application.cancelRegistered(fixture.core, fixture.request))

          Then("one actual Unit ProviderCall succeeds with no runner fallback")
          result shouldBe Consequence.success(())
          fixture.runner.cancellations shouldBe 1
          fixture.runner.executions shouldBe 0
          fixture.trace() should include("cancel-car-review-provider")
          _private_payloads_absent(fixture.trace(), fixture)
        }
      }

      "refuses missing registration before any cancellation call (CANC3)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("a native-backed core and an empty actual registry")
          val application = new CarReviewProviderExecutionApplication(new CarReviewProviderRegistry, new CarReviewProviderExecutionCoordinator)

          When("the application selects the unregistered provider")
          val result = fixture.native.managed(application.cancelRegistered(fixture.core, fixture.request))

          Then("the original provider-not-registered refusal invokes no delegate or ProviderCall")
          failure(result) should include("provider-not-registered")
          fixture.runner.cancellations shouldBe 0
          fixture.trace() should not include "cancel-car-review-provider"
        }
      }

      "refuses a complete typed descriptor mismatch before invocation (CANC3)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("one exact registered provider and a shape-valid changed typed descriptor")
          val document = io.circe.parser.parse(fixture.descriptor).toOption.get
          val changed = document.hcursor.downField("ruleSet").withFocus(_.mapObject(_.add("version", io.circe.Json.fromString("different")))).top.get.noSpaces
          val request = fixture.request.copy(descriptor = changed)

          When("the application checks the whole registered descriptor")
          val result = fixture.native.managed(fixture.application.cancelRegistered(fixture.core, request))

          Then("provider-registration-mismatch refuses before any cancellation delegate or call")
          failure(result) should include("provider-registration-mismatch")
          fixture.runner.cancellations shouldBe 0
          fixture.trace() should not include "cancel-car-review-provider"
        }
      }

      Vector("core", "request", "provider", "descriptor").foreach { absent =>
        s"refuses a null $absent before registration invocation (CANC3)" in {
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given(s"an actual native fixture with an absent $absent cancellation field")
            val core: ActionCall.Core = if absent == "core" then null else fixture.core
            val request = absent match {
              case "request" => null
              case "provider" => fixture.request.copy(provider = null)
              case "descriptor" => fixture.request.copy(descriptor = null)
              case _ => fixture.request
            }

            When("the selected application validates cancellation input")
            val result = fixture.native.managed(fixture.application.cancelRegistered(core, request))

            Then("provider-cancellation-request-invalid returns before a delegate or ProviderCall")
            failure(result) should include("provider-cancellation-request-invalid")
            fixture.runner.cancellations shouldBe 0
            fixture.runner.executions shouldBe 0
            fixture.trace() should not include "cancel-car-review-provider"
          }
        }
      }
    }

    "retain the original Unit SPI compatibility" which {
      Vector(false, true).foreach { throwing =>
        s"discards the actual result deliberately while delegate failure=$throwing remains observed (CANC4)" in {
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given(s"a CarReviewProviderRunner-typed native wrapper with cancellation failure=$throwing")
            if throwing then fixture.runner.cancellation = () => throw new IllegalStateException("PRIVATE-UNIT-SPI-MARKER")
            val wrapper: CarReviewProviderRunner = new CncfCarReviewProviderRunner(fixture.core, fixture.runner)

            When("the compatible cancel Unit method forwards once to structured cancellation")
            val result: Unit = CarReviewJobExecutionFixtures.value(fixture.native.managed(Consequence.success(wrapper.cancel(fixture.request))))

            Then("Unit remains available with one actual call and no exception or fallback")
            result shouldBe ()
            fixture.runner.cancellations shouldBe 1
            fixture.runner.executions shouldBe 0
            fixture.trace() should include("cancel-car-review-provider")
            fixture.trace() should include(if throwing then "outcome=failure" else "outcome=success")
            if throwing then fixture.trace() should include("provider-cancellation-failed")
            fixture.trace() should not include "PRIVATE-UNIT-SPI-MARKER"
            _private_payloads_absent(fixture.trace(), fixture)
          }
        }
      }
    }
  }

  private def _private_payloads_absent(trace: String, fixture: Fixture): Unit = {
    trace should not include fixture.descriptor
    trace should not include fixture.request.providerRequest
    trace should not include load("car-review-evidence-bundle-v1.json")
    trace should not include "ProviderBundleRunnerResult.Failed"
  }
}
