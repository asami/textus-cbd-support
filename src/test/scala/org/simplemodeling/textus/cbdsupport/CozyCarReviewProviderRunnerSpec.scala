package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.goldenport.cncf.context.{ExecutionContext, ScopeContext, ScopeKind}
import org.goldenport.cncf.processexecution.{ProcessArtifactKind, ProcessExecutionAdmission, ProcessExecutionCapture, ProcessExecutionDriver, ProcessExecutionGrant, ProcessExecutionLimits, ProcessExecutionPolicy, ProcessExecutionResult, ProcessExecutionStream, ProcessExecutionTermination, ProcessProgramDefinition}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 *  version Jul. 18, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CozyCarReviewProviderRunnerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD Cozy CAR Review provider runner" should {
    "invoke only the registered Cozy capability for its admitted CAR target" in {
      Given("one registered Cozy descriptor, admitted target, and deterministic process result")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val registry = new CarReviewProviderRegistry()
      val runner = _runner(_target, _provider, _result(_bundle))
      registry.register(_descriptor, runner).isRight shouldBe true

      When("CBD selects the descriptor-bound runner through the provider protocol")
      val outcome = coordinator.execute(_request(), registry)

      Then("the exact provider request reaches Cozy once and CBD admits its bundle")
      outcome should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(AdmittedProviderBundle(ReviewProviderIdentity(ReviewProviderId("cozy"), _), _, _, _, _, _, _), false) =>
      }
    }

    "refuse a non-admitted target before invoking Cozy" in {
      Given("one Cozy runner bound to a different admitted CAR target")
      val runner = _runner(_target.copy(name = "other-car"), _provider, _result(_bundle))

      When("CBD tries to invoke it for the original target")
      val result = runner.execute(_request())

      Then("the capability is never invoked and target authority remains explicit")
      result shouldBe ProviderBundleRunnerResult.Failed(
        "provider-target-not-admitted",
        "Configured Cozy target does not match the admitted Review target.",
        0L
      )
    }

    "refuse a configured Cozy capability whose version differs from the registered provider" in {
      Given("one target-bound capability for a different Cozy implementation version")
      val runner = _runner(
        _target,
        ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.15")),
        _result(_bundle)
      )

      When("the registered provider identity requests execution")
      val result = runner.execute(_request())

      Then("CBD refuses the mismatch before process execution")
      result shouldBe ProviderBundleRunnerResult.Failed(
        "provider-identity-mismatch",
        "Configured Cozy capability does not match the registered provider identity.",
        0L
      )
    }

    "map every neutral process terminal failure to the stable provider vocabulary" in {
      Given("the complete set of non-success Process Execution terminal outcomes")
      val cases = Vector(
        ProcessExecutionTermination.Exited(7) -> ("provider-command-failed", "Cozy provider process returned a non-zero status."),
        ProcessExecutionTermination.TimedOut -> ("provider-timeout", "Cozy provider process timed out."),
        ProcessExecutionTermination.Cancelled -> ("provider-cancelled", "Cozy provider process was cancelled."),
        ProcessExecutionTermination.OutputLimitExceeded(ProcessExecutionStream.Stdout) ->
          ("provider-response-byte-limit", "Cozy provider response exceeded the admitted output limit."),
        ProcessExecutionTermination.ArtifactLimitExceeded ->
          ("provider-response-byte-limit", "Cozy provider artifacts exceeded the admitted output limit."),
        ProcessExecutionTermination.LaunchFailed ->
          ("provider-transport-failed", "Cozy provider process could not be launched.")
      )

      When("CBD receives each terminal result through the deterministic Process Execution driver")
      val results = cases.map { case (termination, (code, message)) =>
        _runner(_target, _provider, _result("", termination)).execute(_request()) ->
          ProviderBundleRunnerResult.Failed(code, message, 0L)
      }

      Then("each neutral terminal state has one explicit CBD provider interpretation")
      results.foreach { case (actual, expected) => actual shouldBe expected }
    }

    "resolve the registered process capability only from the invocation scope" in {
      Given("a scope with a runtime-owned Cozy admission and driver")
      val result = _runner_from_scope(_target, _provider, _result(_bundle))

      Then("CBD creates an adapter without executable or host-process configuration")
      result.toOption shouldBe defined
    }
  }

  private val _descriptor = _load("car-review-provider-descriptor-v1.json")
  private val _providerrequest = _load("car-review-provider-request-v1.json")
  private val _bundle = _load("car-review-evidence-bundle-v1.json")

  private val _target = ReviewTarget(
    ReviewTargetKind("project"),
    Some("org.textus"),
    "textus-user-account",
    Some(ReviewVersion("0.2.0-SNAPSHOT")),
    ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
  )

  private val _provider = ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))

  private def _request(): ProviderBundleExecutionRequest =
    ProviderBundleExecutionRequest(
      ReviewId("review-example-001"),
      _target,
      _provider,
      ProviderBundleAvailability.Enabled,
      _descriptor,
      _providerrequest,
      startedAtMillis = 0L
    )

  private def _load(name: String): String = Files.readString(Path.of("docs", "spec", "examples", name))

  private def _runner(
    target: ReviewTarget,
    provider: ReviewProviderIdentity,
    result: ProcessExecutionResult
  ): CozyCarReviewProviderRunner = {
    val limits = ProcessExecutionLimits(
      Some(1000L), Some(120000L), Some(1000L), Some(4096L), Some(1024L),
      Some(1024L), Some(1L), Some(1024L), Some(1L), Some(1024L), Some(4096L)
    )
    val definition = _value(ProcessProgramDefinition.fromRuntimeC(
      CozyCarReviewProviderProcess.capability,
      "cozy-car-review",
      "runtime-owned-cozy",
      Vector.empty,
      org.goldenport.cncf.processexecution.ProcessArgumentPolicy(Vector.empty),
      limits,
      Set.empty
    ))
    val policy = _value(ProcessExecutionPolicy.createC(Vector(definition)))
    val admission = _value(ProcessExecutionAdmission.createC(
      policy,
      Vector(ProcessExecutionGrant(CozyCarReviewProviderProcess.capability))
    ))
    val driver = new org.goldenport.cncf.processexecution.ProcessExecutionTestProfile(
      Map(CozyCarReviewProviderProcess.capability -> result)
    ).driver
    new CozyCarReviewProviderRunner(target, provider, admission, driver)
  }

  private def _runner_from_scope(
    target: ReviewTarget,
    provider: ReviewProviderIdentity,
    result: ProcessExecutionResult
  ) = {
    val limits = ProcessExecutionLimits(
      Some(1000L), Some(120000L), Some(1000L), Some(4096L), Some(1024L),
      Some(1024L), Some(1L), Some(1024L), Some(1L), Some(1024L), Some(4096L)
    )
    val definition = _value(ProcessProgramDefinition.fromRuntimeC(
      CozyCarReviewProviderProcess.capability,
      "cozy-car-review",
      "runtime-owned-cozy",
      Vector.empty,
      org.goldenport.cncf.processexecution.ProcessArgumentPolicy(Vector.empty),
      limits,
      Set.empty
    ))
    val admission = _value(ProcessExecutionAdmission.createC(
      _value(ProcessExecutionPolicy.createC(Vector(definition))),
      Vector(ProcessExecutionGrant(CozyCarReviewProviderProcess.capability))
    ))
    val driver = new org.goldenport.cncf.processexecution.ProcessExecutionTestProfile(
      Map(CozyCarReviewProviderProcess.capability -> result)
    ).driver
    val scope = ScopeContext(
      ScopeKind.Action,
      "car-review",
      None,
      ExecutionContext.create().observability,
      processExecutionDriverOption = Some(driver),
      processExecutionAdmissionOption = Some(admission)
    )
    CozyCarReviewProviderRunner.fromScopeC(target, provider, scope)
  }

  private def _result(
    output: String,
    termination: ProcessExecutionTermination = ProcessExecutionTermination.Exited(0)
  ): ProcessExecutionResult =
    ProcessExecutionResult(
      termination,
      ProcessExecutionCapture(output.getBytes(StandardCharsets.UTF_8).toVector, output.getBytes(StandardCharsets.UTF_8).length.toLong, truncated = false),
      ProcessExecutionCapture(Vector.empty, 0L, truncated = false),
      Vector.empty,
      1000L,
      "cozy-car-review"
    )

  private def _value[A](consequence: org.goldenport.Consequence[A]): A = consequence match {
    case org.goldenport.Consequence.Success(value) => value
    case org.goldenport.Consequence.Failure(conclusion) => fail(conclusion.display)
  }
}
