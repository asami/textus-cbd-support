package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import cats.~>
import io.circe.Json
import io.circe.parser.parse
import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.context.{ExecutionContext, RuntimeContext}
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.protocol.Request
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Oct.  6, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewCanonicalProviderInputSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD canonical provider input" should {
    "hand registered provider input to the canonical report through ProviderCall" in {
      Given("a real registered descriptor, controlled runner, framework CallTree, and report template without a baseline")
      val context = _context
      val core = _action_core(context)
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val application = _application(runner)
      val template = CarReviewReportCodec.decode(_report).fold(error => fail(error.toString), identity).copy(baseline = None)
      val request = _execution_request

      When("the registered canonical API supplies its actual admitted input to canonical response construction")
      val outcome = application.executeForCanonicalReport(core, request)
      val input = _canonical_input(outcome, fromcache = false)
      val response = new CarReviewCanonicalResponseApplication().build(template, Vector(input), Set("reviewer")).fold(error => fail(error.toString), identity)

      Then("one provider execution supplies bounded attributable evidence and coherent report, gate, and attestation bindings")
      runner.executions shouldBe 1
      input.bundle shouldBe _bundle
      input.admitted.provider shouldBe request.provider
      input.admitted.evidenceIds.nonEmpty shouldBe true
      response.report.reviewId shouldBe request.reviewId
      response.report.target shouldBe request.target
      response.report.evidence.map(_.providerEvidenceId).sorted shouldBe input.admitted.evidenceIds.sorted
      response.report.evidence.map(_.id.value) shouldBe response.report.evidence.map(_.id.value).sorted
      response.report.evidence.map(_.providerId).distinct shouldBe Vector(request.provider.id)
      response.report.evidence.map(_.bundleDigest).distinct shouldBe Vector(input.admitted.bundleDigest)
      response.report.execution.providers.map(_.provider) shouldBe Vector(request.provider)
      response.report.execution.providers.map(_.bundleDigest) shouldBe Vector(Some(input.admitted.bundleDigest))
      response.report.gate shouldBe response.gate
      response.gate.result.value shouldBe "unknown"
      response.attestation.reviewId shouldBe response.report.reviewId
      response.attestation.reportId shouldBe response.report.reportId
      response.attestation.reportDigest shouldBe response.report.reportDigest
      response.attestation.targetDigest shouldBe response.report.target.digest
      response.attestation.profile shouldBe response.report.profile
      response.attestation.gate shouldBe response.gate
      response.attestation.providers.map(_.provider) shouldBe Vector(request.provider)
      CarReviewAttestationCodec.encode(response.attestation).isRight shouldBe true
      val calltree = context.observability.callTreeContext.build().getOrElse(fail("CallTree missing")).toRecord.print
      calltree should include("execute-car-review-provider")
      calltree should include("provider_id=cozy")
      calltree should not include _descriptor
      calltree should not include _provider_request
      calltree should not include _bundle
      calltree should not include _report
      calltree should not include response.report.toString
    }

    "return canonical cached input after legacy metadata execution" in {
      Given("a registered request already completed through the legacy metadata API")
      val core = _action_core(_context)
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val application = _application(runner)
      val request = _execution_request
      val admitted = application.execute(core, request) match {
        case ProviderBundleExecutionOutcome.Admitted(value, false) => value
        case other => fail(s"Expected fresh legacy admission, received $other")
      }

      When("the canonical API requests the same Review Run and provider")
      val outcome = application.executeForCanonicalReport(core, request)

      Then("the exact admitted input is cached and the legacy constructor and pattern remain compatible")
      outcome shouldBe CanonicalProviderBundleExecutionOutcome.Admitted(AdmittedProviderBundleInput(admitted, _bundle), fromCache = true)
      ProviderBundleExecutionOutcome.Admitted(value = admitted, fromCache = false) should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(_, false) =>
      }
      runner.executions shouldBe 1
    }

    "return legacy cached metadata after canonical input execution" in {
      Given("a registered request already completed through the canonical API")
      val core = _action_core(_context)
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val application = _application(runner)
      val request = _execution_request
      val input = _canonical_input(application.executeForCanonicalReport(core, request), fromcache = false)

      When("the legacy metadata API requests the same Review Run and provider")
      val outcome = application.execute(core, request)

      Then("the existing Admitted metadata shape and values are preserved without another execution")
      outcome shouldBe ProviderBundleExecutionOutcome.Admitted(input.admitted, fromCache = true)
      outcome should matchPattern { case ProviderBundleExecutionOutcome.Admitted(_, true) => }
      runner.executions shouldBe 1
    }

    "refuse a foreign outer Review envelope before provider execution" in {
      Given("a valid request and bundle already admitted into the explicit Review and provider cache")
      val core = _action_core(_context)
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val application = _application(runner)
      val request = _execution_request
      val primed = application.executeForCanonicalReport(core, request)
      val foreign = request.copy(reviewId = ReviewId("review-foreign"))

      When("the outer Review ID changes while its provider request document stays unchanged")
      val outcome = application.executeForCanonicalReport(core, foreign)

      Then("the incompatible review-id-mismatch refusal returns no input and does not execute a provider")
      primed should matchPattern { case CanonicalProviderBundleExecutionOutcome.Admitted(_, false) => }
      _expect_refusal(outcome, request.provider, "review-id-mismatch", "incompatible", runfailure = false)
      runner.executions shouldBe 1
    }

    "refuse a foreign outer target envelope before provider execution" in {
      Given("a valid request and bundle already admitted for one target")
      val core = _action_core(_context)
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val application = _application(runner)
      val request = _execution_request
      val primed = application.executeForCanonicalReport(core, request)
      val foreign = request.copy(target = request.target.copy(name = "foreign-target"))

      When("the outer target changes while the provider request document stays unchanged")
      val outcome = application.executeForCanonicalReport(core, foreign)

      Then("the existing request-target-mismatch attribution supplies no input and no rerun")
      primed should matchPattern { case CanonicalProviderBundleExecutionOutcome.Admitted(_, false) => }
      _expect_refusal(outcome, request.provider, "request-target-mismatch", "incompatible", runfailure = false)
      runner.executions shouldBe 1
    }

    "preserve registry selection refusals without a provider fallback" in {
      Given("an unregistered provider and a separate registered provider with a mismatched descriptor rule-set version")
      val core = _action_core(_context)
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val application = _application(runner)
      val request = _execution_request
      val unregistered = request.copy(provider = ReviewProviderIdentity(ReviewProviderId("unregistered"), ReviewVersion("1.0.0")))
      val descriptor = parse(_descriptor).fold(error => fail(error.toString), identity)
      val changed = descriptor.hcursor.downField("ruleSet").withFocus(_.mapObject(_.add("version", Json.fromString("1.0.1")))).top.getOrElse(fail("Descriptor missing"))
      val mismatched = request.copy(descriptor = changed.noSpaces)

      When("the canonical API resolves both explicit provider selections")
      val absent = application.executeForCanonicalReport(core, unregistered)
      val mismatch = application.executeForCanonicalReport(core, mismatched)

      Then("both original registry reasons remain attributable refusals with zero executions and no input")
      _expect_refusal(absent, unregistered.provider, "provider-not-registered", "unavailable", runfailure = false)
      _expect_refusal(mismatch, request.provider, "provider-registration-mismatch", "incompatible", runfailure = false)
      runner.executions shouldBe 0
    }

    "keep malformed, failed, timed out, and cancelled providers attributable" in {
      val cases = Vector(
        FailureCase("malformed bundle", ProviderBundleRunnerResult.Completed("{", 1000L), false, "bundle-json-invalid", "incompatible", false, 1, 0),
        FailureCase("runner failure", ProviderBundleRunnerResult.Failed("controlled-provider-failed", "Private failure detail", 1000L), false, "controlled-provider-failed", "failed", true, 1, 0),
        FailureCase("timeout", ProviderBundleRunnerResult.Completed(_bundle, 120001L), false, "provider-timeout", "failed", true, 1, 1),
        FailureCase("pre-cancellation", ProviderBundleRunnerResult.Completed(_bundle, 1000L), true, "provider-cancelled", "cancelled", false, 0, 1)
      )
      cases.foreach { scenario =>
        Given(s"a separate controlled ${scenario.name} runner and registered framework request")
        val core = _action_core(_context)
        val runner = new RecordingRunner(scenario.result)
        val application = _application(runner)
        val request = _execution_request.copy(cancellationRequested = scenario.cancelled)

        When(s"the canonical API interprets ${scenario.name} through the existing registry and ProviderCall path")
        val outcome = application.executeForCanonicalReport(core, request)

        Then("the original reason, state, and run-failure attribution return no canonical input with bounded execution and cancellation counts")
        _expect_refusal(outcome, request.provider, scenario.code, scenario.state, scenario.runfailure)
        runner.executions shouldBe scenario.executions
        runner.cancellations shouldBe scenario.cancellations
      }
    }

    "refuse a changed request inside an immutable same-Review provider run" in {
      Given("a successful provider bundle cached under explicit Review ID and provider identity")
      val core = _action_core(_context)
      val runner = new RecordingRunner(ProviderBundleRunnerResult.Completed(_bundle, 1000L))
      val application = _application(runner)
      val request = _execution_request
      val primed = application.executeForCanonicalReport(core, request)
      val document = parse(request.providerRequest).fold(error => fail(error.toString), identity)
      val changed = document.hcursor.downField("limits").withFocus(_.mapObject(_.add("timeoutMillis", Json.fromLong(120001L)))).top.getOrElse(fail("Request missing"))
      val immutable = request.copy(providerRequest = changed.noSpaces)

      When("the same Review and provider supply a shape-valid changed timeout limit")
      val outcome = application.executeForCanonicalReport(core, immutable)

      Then("existing bundle admission refuses the changed request binding without another execution or an admitted body")
      primed should matchPattern { case CanonicalProviderBundleExecutionOutcome.Admitted(_, false) => }
      _expect_refusal(outcome, request.provider, "request-digest-mismatch", "incompatible", runfailure = false)
      runner.executions shouldBe 1
    }
  }

  private val _descriptor = _load("car-review-provider-descriptor-v1.json")
  private val _provider_request = _load("car-review-provider-request-v1.json")
  private val _bundle = _load("car-review-evidence-bundle-v1.json")
  private val _report = _load("car-review-report-v1.json")

  private def _execution_request: ProviderBundleExecutionRequest = ProviderBundleExecutionRequest(
    ReviewId("review-example-001"),
    ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")),
    ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14")),
    ProviderBundleAvailability.Enabled,
    _descriptor,
    _provider_request,
    startedAtMillis = 0L
  )

  private def _application(runner: RecordingRunner): CarReviewProviderExecutionApplication = {
    val registry = new CarReviewProviderRegistry()
    registry.register(_descriptor, runner).fold(error => fail(error.toString), identity)
    new CarReviewProviderExecutionApplication(registry, new CarReviewProviderExecutionCoordinator())
  }

  private def _canonical_input(outcome: CanonicalProviderBundleExecutionOutcome, fromcache: Boolean): AdmittedProviderBundleInput =
    outcome match {
      case CanonicalProviderBundleExecutionOutcome.Admitted(input, cached) =>
        cached shouldBe fromcache
        input
      case CanonicalProviderBundleExecutionOutcome.Refused(value) => fail(value.limitation.code)
    }

  private def _expect_refusal(
    outcome: CanonicalProviderBundleExecutionOutcome,
    provider: ReviewProviderIdentity,
    code: String,
    state: String,
    runfailure: Boolean
  ): Unit = outcome match {
    case CanonicalProviderBundleExecutionOutcome.Refused(value) =>
      value.provider shouldBe Some(provider)
      value.limitation.code shouldBe code
      value.limitation.scope shouldBe ReviewLimitationScope("provider")
      value.limitation.subjectId shouldBe Some(provider.id.value)
      value.state shouldBe ReviewProviderState(state)
      value.runFailure shouldBe runfailure
    case CanonicalProviderBundleExecutionOutcome.Admitted(_, _) => fail("Refusal must not return admitted canonical input")
  }

  private def _load(name: String): String = Files.readString(Path.of("docs", "spec", "examples", name))

  private def _context: ExecutionContext = {
    val base = ExecutionContext.withFrameworkCallTreeEnabled(ExecutionContext.create(), enabled = true)
    lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = RuntimeContext.core("canonical-provider-input-spec", None, base.observability),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
      },
      commitAction = _ => (),
      abortAction = _ => (),
      disposeAction = _ => (),
      token = "canonical-provider-input-spec"
    )
    context
  }

  private def _action_core(context: ExecutionContext): ActionCall.Core =
    ActionCall.Core(TestAction(Request.ofOperation("canonical-provider-input")), context, None, None)

  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall = {
      val _ = core
      throw new UnsupportedOperationException("Test fixture exposes ActionCall.Core only.")
    }
  }

  private final case class FailureCase(
    name: String,
    result: ProviderBundleRunnerResult,
    cancelled: Boolean,
    code: String,
    state: String,
    runfailure: Boolean,
    executions: Int,
    cancellations: Int
  )

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
