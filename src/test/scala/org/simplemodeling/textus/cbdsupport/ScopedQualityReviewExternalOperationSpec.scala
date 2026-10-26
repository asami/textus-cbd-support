package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}
import cats.~>
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.bootstrap.BootstrapConfig
import org.goldenport.cncf.cli.CncfRuntime
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, RuntimeContext}
import org.goldenport.cncf.http.UrlConnectionHttpDriver
import org.goldenport.cncf.spi.SpiResolver
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.goldenport.record.io.{RecordDecoder, RecordEncoder}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.simplemodeling.textus.cbdsupport.{value => cbdvalue}

/*
 * @since   Oct. 18, 2026
 * @version Oct. 26, 2026
 * @author  ASAMI, Tomoharu
 */
final class ScopedQualityReviewExternalOperationSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import ScopedQualityReviewSpecSupport.*
  import ScopedQualityReviewExternalSupport.*

  "External scoped review Operations (E1-E5)" should {
    "prepare an explicit bounded definition without calling an installed AI runner" in {
      Given("an ordinary scoped request and a runner that would fail if called")
      val input = request()
      val runner = trapRunner()
      val fixture = runtime(Some(runner))
      When("the generated preparation Operation executes through its ActionCall and provider UoW")
      val definition = prepare(input, fixture)
      Then("the typed definition retains the request, common instructions and exact identity schema without AI")
      definition.request shouldBe input
      definition.executionIntent shouldBe "orchestrated"
      definition.profile shouldBe "scoped-quality"
      definition.prompt should include(input.reviewId)
      definition.responseSchema should include("reviewId")
      runner.requests shouldBe Vector.empty
      fixture.trace should include("prepare-scoped-quality")
      fixture.component.isMcpReady("CbdReviewAdmin", "prepareScopedQuality") shouldBe false
      fixture.component.isMcpReady("CbdReviewAdmin", "acceptScopedQuality") shouldBe false
    }

    "accept clean and blocking responses with actual external attribution and no AI" in {
      Given("a prepared request, observed external executor and either clean or blocking structured findings")
      val input = request()
      val runner = trapRunner()
      val fixture = runtime(Some(runner))
      val definition = prepare(input, fixture)
      val blocker = cbdvalue.ScopedQualityReviewFinding("scope.issue", "integration", "blocker",
        Some(input.paths.head), None, "The supplied integration contract is inconsistent.")
      When("the ordinary acceptance Operation evaluates both response variants")
      val findings = Vector(Vector.empty, Vector(blocker))
      val results = findings.map(items => accept(definition, typedResponse(input).copy(findings = items), fixture))
      Then("coverage completes independently of blocking findings and retains observed execution without inventing a provider")
      results.map(_.status) shouldBe Vector("completed", "completed")
      results.map(_.findings) shouldBe findings
      results.foreach(_.execution shouldBe Some(execution()))
      results.foreach(_.model shouldBe None)
      results.foreach(_.providerId shouldBe "cbd-scoped-quality")
      runner.requests shouldBe Vector.empty
      fixture.trace should include("accept-scoped-quality")
    }

    "bind every request, revision and target before allowing a completed review" in {
      Given("a prepared definition and independently changed review or subject references")
      val input = request()
      val fixture = runtime(None)
      val definition = prepare(input, fixture)
      val response = typedResponse(input)
      val wrong = Vector(response.copy(reviewId = "other-review"), response.copy(revision = input.revision + 1),
        response.copy(subjectId = "other-subject"), response.copy(subjectRevision = "other-revision"))
      When("the normal acceptance Operation receives each mismatched response")
      val results = wrong.map(reply => accept(definition, reply, fixture))
      Then("each result fails selected semantic coverage instead of granting acceptance")
      results.foreach(_.status shouldBe "failed")
      results.foreach(_.checks.foreach(_.status shouldBe "failed"))
    }

    "preserve incomplete, failed and unattributed execution as explicit coverage gaps" in {
      Given("a definition with exact selected paths and four semantic checks")
      val input = request()
      val fixture = runtime(None)
      val definition = prepare(input, fixture)
      val response = typedResponse(input)
      val first = response.checks.head
      val replies = Vector(response.copy(checks = response.checks.tail),
        response.copy(checks = response.checks.updated(0, first.copy(paths = Vector.empty))),
        response.copy(checks = response.checks.updated(0, first.copy(status = "failed", limitations = Vector("executor.failed")))),
        response.copy(checks = response.checks.updated(0, first.copy(paths = Vector("outside.scala")))))
      When("the acceptance Operation receives incomplete or failed responses and an otherwise clean unattributed response")
      val results = replies.map(reply => accept(definition, reply, fixture))
      val unattributed = accept(definition, response, fixture, execution().copy(executorId = None))
      Then("none is completed and missing actual executor attribution remains unsupported")
      results.map(_.status) shouldBe Vector("failed", "unavailable", "failed", "failed")
      unattributed.status shouldBe "unavailable"
      unattributed.checks.foreach(_.limitations should contain("review.provenance.unavailable"))
    }

    "retain deterministic blockers beside a clean semantic response" in {
      Given("selected CML with a missing operation declaration and a clean external semantic response")
      val input = request().copy(checks = Vector("cml-contract", "integration"), modelSubjects = Vector("cml:review-operation"),
        materials = request().materials :+ metadataMaterial(metadata(operationType = "")))
      val fixture = runtime(None)
      When("preparation and acceptance use the same deterministic evaluator")
      val definition = prepare(input, fixture)
      val result = accept(definition, typedResponse(input), fixture)
      Then("external findings cannot erase the deterministic finding")
      definition.prompt should include("cml.declaration.incomplete")
      result.status shouldBe "completed"
      result.findings.map(_.identity) should contain("cml.declaration.incomplete")
      result.checks.map(_.checkId) shouldBe input.checks
    }

    "preserve selected check order for every nonempty semantic selection and response permutation" in {
      Given("arbitrary nonempty semantic selections and reversed response order")
      val available = request().checks
      forAll(Gen.someOf(available).suchThat(_.nonEmpty)) { selected =>
        val input = request().copy(checks = selected.toVector)
        val fixture = runtime(None)
        val definition = prepare(input, fixture)
        val response = typedResponse(input)
        When("the ordinary acceptance Operation receives complete coverage in another order")
        val result = accept(definition, response.copy(checks = response.checks.reverse), fixture)
        Then("the result covers precisely the selection and follows its declared order")
        result.status shouldBe "completed"
        result.checks.map(_.checkId) shouldBe input.checks
      }
    }

    "reject omitted execution intent and mismatched standalone route without invoking AI" in {
      Given("a request with no execution selector and an orchestrated definition submitted as standalone")
      val input = request()
      val runner = trapRunner()
      val fixture = runtime(Some(runner))
      val definition = prepare(input, fixture)
      When("preparation receives the missing selector and acceptance receives the wrong route")
      val missing = intercept[IllegalStateException] {
        invoke("prepareScopedQuality", Record.dataAuto("request" -> input.toRecord()), fixture)
      }
      val mismatch = intercept[IllegalStateException] {
        accept(definition, typedResponse(input), fixture, execution().copy(route = "standalone", selection = "standalone"))
      }
      Then("both ordinary Operations reject the request without a hidden standalone invocation")
      missing.getMessage should not be empty
      mismatch.getMessage should not be empty
      runner.requests shouldBe Vector.empty
    }
  }
}

private[cbdsupport] object ScopedQualityReviewExternalSupport {
  import ScopedQualityReviewSpecSupport.*

  def trapRunner(): RecordingAiRunner = new RecordingAiRunner(Consequence.operationInvalid("CBD must not execute external review"))

  def execution(): cbdvalue.ScopedQualityReviewExecution =
    cbdvalue.ScopedQualityReviewExecution("external", "independent", Some("external-spec-executor"), None, None)

  def typedResponse(input: cbdvalue.ScopedQualityReviewRequest): cbdvalue.ScopedQualityReviewResponse =
    valueOf(cbdvalue.ScopedQualityReviewResponse.createC(response(input).record))

  def prepare(input: cbdvalue.ScopedQualityReviewRequest, fixture: RuntimeFixture): cbdvalue.ScopedQualityReviewDefinition =
    valueOf(cbdvalue.ScopedQualityReviewDefinition.createC(invoke("prepareScopedQuality",
      cbdvalue.ScopedQualityReviewPreparation(input, "orchestrated").toRecord(), fixture)))

  def accept(definition: cbdvalue.ScopedQualityReviewDefinition, response: cbdvalue.ScopedQualityReviewResponse,
    fixture: RuntimeFixture, observedExecution: cbdvalue.ScopedQualityReviewExecution = execution()): cbdvalue.ScopedQualityReviewResult =
    valueOf(cbdvalue.ScopedQualityReviewResult.createC(invoke("acceptScopedQuality",
      cbdvalue.ScopedQualityReviewAcceptance(definition, response, observedExecution).toRecord(), fixture)))

  def invoke(operation: String, record: Record, fixture: RuntimeFixture): Record = {
    val envelope = Request.of(component = "CbdSupport", service = "CbdReviewAdmin", operation = operation,
      properties = record.fields.map(field => Property(field.key, field.value.single, None)).toList)
    val output = operation match {
      case "prepareScopedQuality" =>
        CbdSupportComponent.CbdReviewAdminService.PrepareScopedQualityOperation.createOperationRequest(envelope).flatMap { action =>
          action.createCall(ActionCall.Core(action, fixture.context, Some(fixture.component), None)).execute()
        }
      case "acceptScopedQuality" =>
        CbdSupportComponent.CbdReviewAdminService.AcceptScopedQualityOperation.createOperationRequest(envelope).flatMap { action =>
          action.createCall(ActionCall.Core(action, fixture.context, Some(fixture.component), None)).execute()
        }
      case _ => Consequence.operationInvalid("Unknown external review Operation")
    }
    valueOf(output) match {
      case OperationResponse.RecordResponse(result) => result
      case _ => throw new IllegalStateException("Expected a normal Operation record response")
    }
  }
}

/** Test-host bridge for a real external worker, not an emulated AI response. */
object ScopedQualityReviewExternalExecutionMain {
  import ScopedQualityReviewSpecSupport.*
  import ScopedQualityReviewExternalSupport.*

  def main(args: Array[String]): Unit = {
    require(args.length == 3, "operation input-json output-json")
    val runner = trapRunner()
    val fixture = runtime(Some(runner))
    val input = valueOf(new RecordDecoder().json(Files.readString(Path.of(args(1)))))
    val output = invoke(args(0), input, fixture)
    require(runner.requests.isEmpty, "External Operation called CBD AI")
    Files.writeString(Path.of(args(2)), RecordEncoder.json(output))
    println(s"[scoped-review-external] operation=${args(0)} ai_calls=${runner.requests.size} output=${args(2)}")
  }
}

/** Explicit test-host connection to an actual optional Textus AI runtime. */
object ScopedQualityReviewStandaloneExecutionMain {
  import ScopedQualityReviewSpecSupport.*

  def main(args: Array[String]): Unit = {
    require(args.length == 5, "input-json output-json endpoint model trace-json")
    val input = valueOf(new RecordDecoder().json(Files.readString(Path.of(args(0)))))
    val configuration = ResolvedConfiguration(Configuration(Map(
      "textus.ai.profile" -> ConfigurationValue.StringValue("gemma"),
      "textus.ai.gemma.endpoint" -> ConfigurationValue.StringValue(args(2)),
      "textus.ai.gemma.model" -> ConfigurationValue.StringValue(args(3)),
      "textus.ai.gemma.timeout-seconds" -> ConfigurationValue.StringValue("180"),
      "textus.ai.execution-classes.advanced-thinking.model" -> ConfigurationValue.StringValue(args(3)),
      "textus.ai.execution-classes.advanced-thinking.max-output-tokens" -> ConfigurationValue.StringValue("8192"),
      "textus.ai.execution-classes.advanced-thinking.timeout-seconds" -> ConfigurationValue.StringValue("180"),
      "textus.ai.execution-classes.advanced-thinking.record-retry-limit" -> ConfigurationValue.StringValue("0")
    )), ConfigurationTrace.empty)
    val subsystem = new Subsystem(name = "scoped-quality-standalone-test", configuration = configuration)
    val factory = Class.forName("org.simplemodeling.textus.ai.ComponentFactory")
      .getConstructor().newInstance().asInstanceOf[Component.Factory]
    val ai = factory.createPrimary(ComponentCreate(subsystem, ComponentOrigin.Main))
    val cbd = new impl.ComponentFactory().create(ComponentCreate(subsystem, ComponentOrigin.Main))
    val cwd = Path.of(args(1)).toAbsolutePath.getParent.resolve("host-runtime")
    val plan = Json.obj(
      "schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
      "baseUrl" -> Json.fromString(args(2)),
      "limits" -> Json.obj("connectTimeoutMillis" -> Json.fromInt(2000),
        "readTimeoutMillis" -> Json.fromInt(180000), "responseBytes" -> Json.fromLong(262144)),
      "grants" -> Json.arr(Json.obj("componentId" -> Json.fromString(ai.componentId.name),
        "origins" -> Json.arr(Json.fromString(args(2))), "methods" -> Json.arr(Json.fromString("POST")))))
    Files.writeString(Files.createDirectories(cwd.resolve(".textus")).resolve("config.json"),
      Json.obj("textus.subsystem.http-execution.plan" -> Json.fromString(plan.noSpaces)).noSpaces)
    val descriptor = cwd.resolve("test.yaml")
    Files.writeString(descriptor,
      """kind: test-descriptor
        |execution:
        |  profile: controlled
        |  key: scoped-quality-standalone
        |  time:
        |    mode: manual
        |    start-at: '2026-10-27T00:00:00Z'
        |  random:
        |    mode: seeded
        |    seed: scoped-quality-standalone
        |  ids:
        |    mode: deterministic
        |  scheduler:
        |    mode: manual
        |  ordering:
        |    mode: deterministic
        |""".stripMargin)
    val host = new CncfRuntime()
    try {
      val handle = valueOf(host.initializeHandle(BootstrapConfig(cwd = cwd,
        args = Array(s"--textus.test.descriptor=$descriptor"))))
      try {
        (ai +: cbd.participants).foreach(_.withScopeContext(handle.subsystem.globalRuntimeContext))
        val context = _context(args(2), ai)
        given ExecutionContext = context
        valueOf(SpiResolver.resolve(ai +: cbd.participants, Vector.empty))
        val envelope = Request.of(component = "CbdSupport", service = "CbdReviewAdmin", operation = "reviewScopedQuality",
          properties = input.fields.map(field => Property(field.key, field.value.single, None)).toList)
        val action = valueOf(CbdSupportComponent.CbdReviewAdminService.ReviewScopedQualityOperation.createOperationRequest(envelope))
        val output = valueOf(action.createCall(ActionCall.Core(action, context, Some(cbd.primary), None)).execute()) match {
          case OperationResponse.RecordResponse(record) => record
          case _ => throw new IllegalStateException("Expected a normal standalone Operation record response")
        }
        Files.writeString(Path.of(args(1)), RecordEncoder.json(output))
        Files.writeString(Path.of(args(4)), context.observability.callTreeContext.build()
          .map(tree => RecordEncoder.json(tree.toRecord)).getOrElse("{}"))
        val result = valueOf(cbdvalue.ScopedQualityReviewResult.createC(output))
        println(s"[scoped-review-standalone] status=${result.status} provider=${result.providerId} model=${result.model.getOrElse("")}")
        require(result.status == "completed" && result.execution.exists(_.route == "standalone"),
          "Actual standalone execution did not cover the selected request; retain its result as a gap")
      } finally handle.close()
    } finally host.closeEmbedding()
  }

  private def _context(endpoint: String, component: Component): ExecutionContext = {
    val base = ExecutionContext.withFrameworkCallTreeEnabled(ExecutionContext.create(), enabled = true)
    lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      RuntimeContext.core("scoped-quality-standalone-test", Some(component.scopeContext), base.observability,
        Some(new UrlConnectionHttpDriver(endpoint))),
      () => unitofwork,
      new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
      },
      _ => (), _ => (), _ => (), "scoped-quality-standalone-test")
    context
  }
}
