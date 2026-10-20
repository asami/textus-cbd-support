package org.simplemodeling.textus.cbdsupport

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.spi.SpiResolver
import org.goldenport.cncf.spi.ai.runner.*
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.protocol.Protocol
import org.goldenport.record.Record
import org.goldenport.record.io.RecordEncoder
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.ScopedQualityReviewProvider
import org.simplemodeling.textus.cbdsupport.{value => cbdvalue}

/*
 * @since   Oct. 17, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
final class ScopedQualityReviewProviderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ScopedQualityReviewSpecSupport.*

  "Scoped quality provider (docs/spec/scoped-quality-review.md)" should {
    "S1 evaluate selected semantic checks once inside the actual UoW and retain a clean attributable result" in {
      Given("R1,R3,R4: selected requirements and source, and an installed recording AiRunner through ordinary SPI bootstrap")
      val input = request()
      val reply = response(input)
      val runner = new RecordingAiRunner(Consequence.success(reply))
      val fixture = runtime(Some(runner))
      When("the actual provider program evaluates the selected checks")
      val result = valueOf(ScopedQualityReviewProvider.reviewC(fixture.core, input))
      val encoded = parse(RecordEncoder.json(reply.record)).toOption.get
      val captured = runner.requests.head
      val promptschema = parse(captured.prompt.split("Output schema:\n", 2).last.split("\nPayload:\n", 2).head).toOption.get
      val checkschema = promptschema.hcursor.downField("properties").downField("checks").downField("items")
      Then("the prompt carries the actual required response schema, all checks are covered, and clean findings retain target and provenance")
      promptschema shouldBe parse(RecordEncoder.json(captured.schema)).toOption.get
      promptschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("reviewId", "revision", "subjectId", "subjectRevision", "checks", "findings"))
      checkschema.get[Vector[String]]("required") shouldBe Right(Vector("checkId", "status", "paths", "modelSubjects", "limitations"))
      Vector("checks", "findings").foreach { field =>
        promptschema.hcursor.downField("properties").downField(field).get[String]("type") shouldBe Right("array")
      }
      Vector("paths", "modelSubjects", "limitations").foreach { field =>
        checkschema.downField("properties").downField(field).get[String]("type") shouldBe Right("array")
        checkschema.downField("properties").downField(field).downField("items").get[String]("type") shouldBe Right("string")
      }
      encoded.hcursor.get[Vector[Json]]("findings") shouldBe Right(Vector.empty)
      encoded.hcursor.get[Vector[Json]]("checks").toOption.get.foreach { check =>
        check.hcursor.get[Vector[String]]("modelSubjects") shouldBe Right(Vector.empty)
        check.hcursor.get[Vector[String]]("limitations") shouldBe Right(Vector.empty)
      }
      result.status shouldBe "completed"
      result.findings shouldBe Vector.empty
      result.reviewId shouldBe input.reviewId
      result.revision shouldBe input.revision
      result.subjectId shouldBe input.subjectId
      result.subjectRevision shouldBe input.subjectRevision
      result.providerId shouldBe "recording-provider"
      result.model shouldBe Some("recording-model")
      result.purpose shouldBe Some("car-review.semantic-consistency")
      result.ruleRevision shouldBe "cbd-scoped-quality-v1"
      result.checks.map(_.status) shouldBe Vector.fill(input.checks.size)("covered")
      runner.requests.size shouldBe 1
      runner.contexts.head should be theSameInstanceAs fixture.context
      runner.requests.head.requirement.purposeRequired shouldBe true
      runner.requests.head.requirement.purpose shouldBe result.purpose
      runner.requests.head.requirement.tools shouldBe Vector.empty
      runner.requests.head.prompt should include("requirement: preserve scope")
      runner.requests.head.prompt should include("untrusted review data")
      fixture.context.unitOfWork.executionContext should be theSameInstanceAs fixture.context
      fixture.trace should include("review-scoped-quality")
    }

    "S1a preserve one selected semantic check and exact target scope beside the prompt response contract" in {
      Given("R1,R3,R4: one semantic check over a selected path and model subject, with a valid attributable recording response")
      val input = request().copy(checks = Vector("semantic-consistency"), modelSubjects = Vector("cml:example"))
      val runner = new RecordingAiRunner(Consequence.success(response(input)))
      val fixture = runtime(Some(runner))
      When("the actual provider program sends the bounded request through the installed AiRunner")
      val result = valueOf(ScopedQualityReviewProvider.reviewC(fixture.core, input))
      val captured = runner.requests.head
      val payload = parse(captured.prompt.split("\nPayload:\n", 2).last).toOption.get
      Then("the prompt payload preserves the selected target and scope and the valid clean response remains completed")
      runner.requests.size shouldBe 1
      payload.hcursor.get[String]("subjectId") shouldBe Right(input.subjectId)
      payload.hcursor.get[String]("subjectRevision") shouldBe Right(input.subjectRevision)
      payload.hcursor.get[Vector[String]]("checks") shouldBe Right(input.checks)
      payload.hcursor.get[Vector[String]]("paths") shouldBe Right(input.paths)
      payload.hcursor.get[Vector[String]]("modelSubjects") shouldBe Right(input.modelSubjects)
      result.status shouldBe "completed"
      result.subjectId shouldBe input.subjectId
      result.subjectRevision shouldBe input.subjectRevision
      result.checks.map(_.checkId) shouldBe input.checks
      result.checks.head.paths shouldBe input.paths
      result.checks.head.modelSubjects shouldBe input.modelSubjects
      result.checks.head.limitations shouldBe Vector.empty
      result.findings shouldBe Vector.empty
      result.providerId shouldBe "recording-provider"
      result.model shouldBe Some("recording-model")
    }

    "S2 preserve deterministic structural blockers beside clean semantic findings" in {
      Given("R2,R4: an exact operation term whose operationType is incomplete and clean semantic output")
      val input = request().copy(modelSubjects = Vector("cml:review-operation"), checks = Vector("cml-contract", "semantic-consistency"),
        materials = Vector(metadataMaterial(metadata(operationType = ""))))
      val runner = new RecordingAiRunner(Consequence.success(response(input)))
      val fixture = runtime(Some(runner))
      When("CML structure and semantic quality are evaluated together")
      val result = valueOf(ScopedQualityReviewProvider.reviewC(fixture.core, input))
      Then("completed means coverage and cannot erase the independently attributed blocker")
      result.status shouldBe "completed"
      result.checks.map(_.status) shouldBe Vector("covered", "covered")
      result.checks.head.paths shouldBe Vector.empty
      result.checks.head.modelSubjects shouldBe input.modelSubjects
      result.findings.size shouldBe 1
      result.findings.head.checkId shouldBe "cml-contract"
      result.findings.head.severity shouldBe "blocker"
      result.findings.head.modelSubject shouldBe Some("cml:review-operation")
      result.findings.head.message should include("cml.declaration.incomplete")
      runner.requests.size shouldBe 1
    }

    "S3 retain unsupported CML scope and unavailable SPI or purpose as coverage gaps" in {
      Given("R2,R3,R4: deterministic-only requests with valid, absent, missing-subject and external metadata plus unavailable semantic providers")
      val deterministic = request().copy(checks = Vector("cml-contract"), modelSubjects = Vector("cml:review-operation"),
        materials = Vector(metadataMaterial(metadata())))
      val runner = new RecordingAiRunner(Consequence.success(response(request())))
      val fixture = runtime(Some(runner))
      val inputs = Vector(deterministic, deterministic.copy(materials = Vector.empty),
        deterministic.copy(modelSubjects = Vector("cml:missing")),
        deterministic.copy(materials = Vector(metadataMaterial(metadata(inputType = "ExternalValue")))) )
      val missing = runtime(None)
      val purposerunner = new RecordingAiRunner(Consequence.serviceUnavailable("required AI purpose unavailable"))
      val purposefixture = runtime(Some(purposerunner))
      When("the provider evaluates each bounded structural scope and the unavailable semantic routes")
      val results = inputs.map(input => valueOf(ScopedQualityReviewProvider.reviewC(fixture.core, input)))
      val missingresult = valueOf(ScopedQualityReviewProvider.reviewC(missing.core, request()))
      val purport = valueOf(ScopedQualityReviewProvider.reviewC(purposefixture.core, request()))
      Then("only actual bounded structural coverage completes, deterministic requests never call AI, and unavailable checks remain explicit")
      results.map(_.status) shouldBe Vector("completed", "unavailable", "unavailable", "unavailable")
      results.head.providerId shouldBe "cbd-scoped-quality"
      results.head.purpose shouldBe None
      results.head.model shouldBe None
      results.tail.foreach(_.checks.head.status shouldBe "unsupported")
      runner.requests shouldBe Vector.empty
      missingresult.status shouldBe "unavailable"
      purport.status shouldBe "unavailable"
      purposerunner.requests.size shouldBe 1
    }

    "S4 refuse malformed, contradictory, wrong-target and out-of-scope output and expose partial coverage" in {
      Given("R4: related structured output cases for the same selected semantic target")
      val input = request().copy(checks = Vector("semantic-consistency"))
      val check = cbdvalue.ScopedQualityReviewCheckResult("semantic-consistency", "covered", input.paths, input.modelSubjects, Vector.empty)
      val finding = cbdvalue.ScopedQualityReviewFinding("finding-1", "semantic-consistency", "blocker", Some(input.paths.head), None, "A real defect.")
      val cleanreply = response(input)
      val checkrecord = Record.data("checkId" -> check.checkId, "status" -> check.status, "paths" -> check.paths,
        "modelSubjects" -> check.modelSubjects, "limitations" -> check.limitations)
      val cases = Vector(
        "structured-blocker" -> (response(input, findings = Vector(finding)), "completed"),
        "missing-schema" -> (AiRecordResponse(Record.empty), "failed"),
        "missing-findings-array" -> (cleanreply.copy(record = cleanreply.record.removeKeys(Set("findings"))), "failed"),
        "missing-limitations-array" -> (cleanreply.copy(record = cleanreply.record.upsertSingle("checks",
          Vector(checkrecord.removeKeys(Set("limitations"))))), "failed"),
        "missing-model-subjects-array" -> (cleanreply.copy(record = cleanreply.record.upsertSingle("checks",
          Vector(checkrecord.removeKeys(Set("modelSubjects"))))), "failed"),
        "wrong-target" -> (response(input, subjectId = "other"), "failed"),
        "wrong-revision" -> (response(input, subjectRevision = "other-revision"), "failed"),
        "missing-check" -> (response(input, checks = Some(Vector.empty)), "failed"),
        "duplicate-check" -> (response(input, checks = Some(Vector(check, check))), "failed"),
        "extra-check" -> (response(input, checks = Some(Vector(check.copy(checkId = "integration")))), "failed"),
        "unknown-status" -> (response(input, checks = Some(Vector(check.copy(status = "pass")))), "failed"),
        "outside-path" -> (response(input, checks = Some(Vector(check.copy(paths = Vector("src/Outside.scala"))))), "failed"),
        "outside-model" -> (response(input, checks = Some(Vector(check.copy(modelSubjects = Vector("cml:outside"))))), "failed"),
        "bad-severity" -> (response(input, findings = Vector(finding.copy(severity = "warning"))), "failed"),
        "outside-finding" -> (response(input, findings = Vector(finding.copy(path = Some("src/Outside.scala")))), "failed"),
        "unselected-finding" -> (response(input, findings = Vector(finding.copy(checkId = "integration"))), "failed"),
        "partial" -> (response(input, checks = Some(Vector(check.copy(paths = Vector.empty)))), "unavailable"),
        "unsupported" -> (response(input, checks = Some(Vector(check.copy(status = "unsupported", limitations = Vector("source unavailable"))))), "unavailable"),
        "unattributed" -> (response(input).copy(metadata = Map.empty), "unavailable"))
      When("each related response crosses the actual provider boundary")
      val results = cases.map { case (name, (reply, expected)) =>
        val fixture = runtime(Some(new RecordingAiRunner(Consequence.success(reply))))
        (name, valueOf(ScopedQualityReviewProvider.reviewC(fixture.core, input)), expected)
      }
      val failurefixture = runtime(Some(new RecordingAiRunner(Consequence.operationInvalid("execution failed"))))
      val executionfailure = valueOf(ScopedQualityReviewProvider.reviewC(failurefixture.core, input))
      val malformedcml = valueOf(ScopedQualityReviewProvider.reviewC(runtime(None).core,
        input.copy(checks = Vector("cml-contract"), modelSubjects = Vector("cml:review-operation"),
          materials = Vector(metadataMaterial("{invalid")))))
      Then("invalid evaluation including missing mandatory arrays fails, partial or unsupported coverage stays unavailable, and actual execution failure is failed")
      results.foreach { case (name, result, expected) => withClue(name) { result.status shouldBe expected } }
      results.find(_._1 == "structured-blocker").get._2.findings shouldBe Vector(finding)
      results.find(_._1 == "partial").get._2.checks.head.limitations should contain("ai.coverage.partial")
      executionfailure.status shouldBe "failed"
      malformedcml.status shouldBe "failed"
    }

    "S5 reject invalid or beyond-bound input before AI and keep material out of CallTree" in {
      Given("R1,R3: invalid identities, scope, vocabulary and size boundaries plus a private material sentinel")
      val input = request().copy(materials = Vector(cbdvalue.ScopedQualityReviewMaterial("source-1", "source", Some("src/Example.scala"), None, "PRIVATE-REVIEW-SENTINEL")))
      val material = input.materials.head
      val invalid = Vector(input.copy(reviewId = " "), input.copy(reviewId = "x" * 129), input.copy(revision = 0),
        input.copy(subjectRevision = ""), input.copy(paths = Vector.empty), input.copy(paths = Vector("../escape")),
        input.copy(paths = Vector("/absolute")), input.copy(paths = Vector("C:\\absolute")), input.copy(paths = Vector("x" * 1025)),
        input.copy(checks = Vector.empty), input.copy(checks = Vector("unknown")), input.copy(checks = Vector.fill(65)("integration")),
        input.copy(requirements = Vector.fill(65)("requirement")), input.copy(modelSubjects = Vector.fill(65)("cml:term")),
        input.copy(materials = Vector.fill(65)(material)), input.copy(materials = Vector(material.copy(kind = "unknown"))),
        input.copy(materials = Vector(material.copy(path = Some("src/Outside.scala")))),
        input.copy(materials = Vector(material.copy(modelSubject = Some("cml:outside")))),
        input.copy(materials = Vector(material.copy(content = "あ" * 87382))),
        input.copy(materials = Vector.fill(4)(material.copy(content = "x" * 262144)), requirements = Vector("x")))
      val runner = new RecordingAiRunner(Consequence.success(response(input)))
      val fixture = runtime(Some(runner))
      When("the provider admits invalid requests and then evaluates one valid private-material request")
      val rejected = invalid.map(candidate => ScopedQualityReviewProvider.reviewC(fixture.core, candidate))
      val before = runner.requests.size
      val accepted = ScopedQualityReviewProvider.reviewC(fixture.core, input)
      Then("invalid requests disclose no material or AI call, while the valid program traces safe metadata only")
      rejected.foreach { result =>
        result.isFaillure shouldBe true
        result.toString should not include "PRIVATE-REVIEW-SENTINEL"
      }
      before shouldBe 0
      accepted.isSuccess shouldBe true
      runner.requests.size shouldBe 1
      fixture.trace should include("review_id=review-1")
      fixture.trace should not include "PRIVATE-REVIEW-SENTINEL"
      fixture.trace should not include "requirement: preserve scope"
    }
  }
}

private[cbdsupport] object ScopedQualityReviewSpecSupport {
  final case class RuntimeFixture(component: Component, context: ExecutionContext, core: ActionCall.Core) {
    def trace: String = context.observability.callTreeContext.build().map(_.toRecord.print).getOrElse("")
  }

  final class RecordingAiRunner(reply: Consequence[AiRecordResponse]) extends AiRunner {
    private var _requests = Vector.empty[AiRecordRequest]
    private var _contexts = Vector.empty[ExecutionContext]
    def requests: Vector[AiRecordRequest] = _requests
    def contexts: Vector[ExecutionContext] = _contexts
    def generate(req: AiGenerateRequest)(using ExecutionContext): Consequence[AiGenerateResponse] = Consequence.operationInvalid("unused fixture operation")
    def chat(req: AiChatRequest)(using ExecutionContext): Consequence[AiChatResponse] = Consequence.operationInvalid("unused fixture operation")
    def generateRecord(req: AiRecordRequest)(using context: ExecutionContext): Consequence[AiRecordResponse] = {
      _requests = _requests :+ req
      _contexts = _contexts :+ context
      reply
    }
  }

  def runtime(runner: Option[AiRunner]): RuntimeFixture = {
    val fixture = ManagedEffectReviewSpecSupport.contextFixture()
    given ExecutionContext = fixture.context
    val component = new impl.ComponentFactory().create(ComponentCreate(DefaultSubsystemFactory.default(Some("server")),
      ComponentOrigin.Repository("scoped-quality-spec"))).primary
    val providers = runner.toVector.map { service =>
      val provider = new Component() {}.withPort(Component.Port.output(service))
      val identity = ComponentId("org.simplemodeling.textus.scopedquality.SpecAiProvider")
      provider.initialize(ComponentInit(subsystem = DefaultSubsystemFactory.default(Some("server")),
        core = Component.Core.create(identity.name, identity, ComponentInstanceId.default(identity), Protocol.empty),
        origin = ComponentOrigin.Builtin))
      provider
    }
    valueOf(SpiResolver.resolve(providers :+ component))
    RuntimeFixture(component, fixture.context, fixture.core.copy(component = Some(component)))
  }

  def request(): cbdvalue.ScopedQualityReviewRequest = cbdvalue.ScopedQualityReviewRequest("review-1", 2L,
    "subject-1", "subject-revision-3", Vector("src/Example.scala"), Vector.empty,
    Vector("semantic-consistency", "implementation-quality", "excessive-checks", "integration"),
    Vector("requirement: preserve scope"), Vector(cbdvalue.ScopedQualityReviewMaterial("source-1", "source", Some("src/Example.scala"), None, "class Example")))

  def response(input: cbdvalue.ScopedQualityReviewRequest, subjectId: String = "subject-1", subjectRevision: String = "subject-revision-3",
    checks: Option[Vector[cbdvalue.ScopedQualityReviewCheckResult]] = None,
    findings: Vector[cbdvalue.ScopedQualityReviewFinding] = Vector.empty): AiRecordResponse = {
    val results = checks.getOrElse(input.checks.filterNot(_ == "cml-contract").map(id =>
      cbdvalue.ScopedQualityReviewCheckResult(id, "covered", input.paths, input.modelSubjects, Vector.empty)))
    val checkrecords = results.map(check => Record.data("checkId" -> check.checkId, "status" -> check.status,
      "paths" -> check.paths, "modelSubjects" -> check.modelSubjects, "limitations" -> check.limitations))
    val findingrecords = findings.map(finding => Record.data("identity" -> finding.identity, "checkId" -> finding.checkId,
      "severity" -> finding.severity, "message" -> finding.message) ++
      Record.dataOption("path" -> finding.path, "modelSubject" -> finding.modelSubject))
    AiRecordResponse(Record.data("reviewId" -> input.reviewId, "revision" -> input.revision, "subjectId" -> subjectId, "subjectRevision" -> subjectRevision,
      "checks" -> checkrecords, "findings" -> findingrecords), Some("recording-model"),
      Map("ai.execution.provider" -> "recording-provider"))
  }

  def metadataMaterial(content: String): cbdvalue.ScopedQualityReviewMaterial =
    cbdvalue.ScopedQualityReviewMaterial("metadata-1", "cml-metadata", None, None, content)

  def metadata(operationType: String = "QUERY", inputType: String = "ReviewInput"): String = {
    s"""{"schema":"cozy.cml.model-metadata.v1","surface":{"component":{"name":"Example","termId":"cml:example","services":[
       |{"name":"Review","termId":"cml:review","operations":[{"name":"review","termId":"cml:review-operation",
       |"operationType":"$operationType","inputType":"$inputType","outputType":"string"}]}]}},
       |"modelElements":[{"name":"ReviewInput","termId":"cml:review-input","kind":"value","fields":[{"name":"target","type":"string"}]}]}""".stripMargin
  }

  def valueOf[A](result: Consequence[A]): A = result match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => throw new IllegalStateException(conclusion.display)
  }
}
