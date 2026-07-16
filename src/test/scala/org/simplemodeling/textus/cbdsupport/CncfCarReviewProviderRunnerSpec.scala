package org.simplemodeling.textus.cbdsupport

import cats.~>
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
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CncfCarReviewProviderRunnerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CNCF CAR Review Provider runner" should {
    "execute a registered runner through ProviderCall without exposing provider payloads to CallTree" in {
      Given("one runner, provider-bound ActionCall, and a bundle text that must stay private")
      val context = _context
      val delegate = new RecordingRunner(ProviderBundleRunnerResult.Completed("bundle-secret-content", 10L))
      val runner = new CncfCarReviewProviderRunner(_actionCore(context), delegate)

      When("CBD executes the provider through the CNCF boundary")
      val result = runner.execute(_request)

      Then("the result is preserved and only safe identity metadata enters CallTree")
      result shouldBe ProviderBundleRunnerResult.Completed("bundle-secret-content", 10L)
      delegate.executions shouldBe 1
      val calltree = context.observability.callTreeContext.build().getOrElse(fail("calltree missing"))
      val text = calltree.toRecord.print
      text should include("provider_id=cozy")
      text should include("provider_version=0.1.14")
      text should include("target_digest=sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
      text should not include "bundle-secret-content"
    }

    "deliver cancellation through ProviderCall without exposing descriptor or request text" in {
      Given("one provider-bound ActionCall and a cancellable local runner")
      val context = _context
      val delegate = new RecordingRunner(ProviderBundleRunnerResult.Completed("bundle-secret-content", 10L))
      val runner = new CncfCarReviewProviderRunner(_actionCore(context), delegate)

      When("CBD sends provider cancellation through the CNCF boundary")
      runner.cancel(_request)

      Then("cancellation reaches the local runner and CallTree retains no provider payload")
      delegate.cancellations shouldBe 1
      val calltree = context.observability.callTreeContext.build().getOrElse(fail("calltree missing"))
      val text = calltree.toRecord.print
      text should include("cancel-car-review-provider")
      text should not include "descriptor-secret-content"
      text should not include "request-secret-content"
    }
  }

  private def _context: ExecutionContext = {
    val base = ExecutionContext.withFrameworkCallTreeEnabled(ExecutionContext.create(), enabled = true)
    lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = RuntimeContext.core("cncf-car-review-provider-runner-spec", None, base.cncfCore.observability),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
      },
      commitAction = _ => (),
      abortAction = _ => (),
      disposeAction = _ => (),
      token = "cncf-car-review-provider-runner-spec"
    )
    context
  }

  private def _actionCore(context: ExecutionContext): ActionCall.Core =
    ActionCall.Core(TestAction(Request.ofOperation("cncf-car-review-provider-runner")), context, None, None)

  private val _request = ProviderBundleExecutionRequest(
    ReviewId("review-example-001"),
    ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")),
    ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14")),
    ProviderBundleAvailability.Enabled,
    "descriptor-secret-content",
    "request-secret-content",
    startedAtMillis = 0L
  )

  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall = {
      val _ = core
      throw new UnsupportedOperationException("Test fixture exposes ActionCall.Core only.")
    }
  }

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
