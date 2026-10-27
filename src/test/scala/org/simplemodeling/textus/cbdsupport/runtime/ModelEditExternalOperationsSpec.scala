package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.mcp.McpToolCatalog
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.{CbdSupportComponent, ManagedEffectReviewSpecSupport, impl}
import ModelEditOperationJson.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditExternalOperationsSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ModelEditApplicationSpecFixture.*
  import ModelEditExternalOperationsSpec.*
  import ModelEditSession.*

  "Source-owned ordinary Model Edit Operations" should {
    "give ChatGPT and Codex the same generated dispatch, exact edit and subsequent inspection semantics" in {
      Given("two ordinary authorized clients with the same server-admitted model and permissions")
      When("each client uses generated ActionCalls and its host-owned application through the normal provider UoW")
      val results = Vector("chatgpt", "codex").map { principal =>
        val fixture = create()
        val runtime = host(Some(fixture.application), principal)
        val request = refinement(fixture.current)
        val palette = invoke("listModelEditOperations", ModelEditOperationJson.context(ContextRequest(request.selection, request.context)), runtime)
        val applied = invoke("applyModelEdit", edit(request), runtime)
        val next = Selection(fixture.current.reference, request.successorCandidate)
        val reads = Vector("inspectModelEdit", "getModelEditCandidate", "getModelEditView", "getModelEditDiff", "getModelEditTraces", "getModelEditReview")
          .map(name => name -> invoke(name, ModelEditOperationJson.selection(next), runtime)).toMap
        val tools = McpToolCatalog.toolsForComponent(runtime.component).map(_.name).toSet
        val privatepaths = Vector("startReview", "submitReviewDocuments", "prepareScopedQuality", "acceptScopedQuality")
          .map(name => runtime.component.isMcpReady("CbdReviewAdmin", name))
        (palette, applied, reads, tools, privatepaths, runtime.interpreted())
      }
      Then("client branding provides no additional privilege or altered identity resolution")
      results.head._1 shouldBe results.last._1
      results.head._2 shouldBe results.last._2
      results.head._3 shouldBe results.last._3
      results.foreach { result =>
        result._1.getString("resultJson").get should include("RefineGoal")
        result._2.getString("status") shouldBe Some("OK")
        result._3.values.map(_.getString("status").get).toSet shouldBe Set("OK", "UNAVAILABLE")
        ModelEditApplication.operations.forall(name => result._4.contains(s"${CbdSupportComponent.name}.CbdModelEdit.$name")) shouldBe true
        result._4.exists(name => name.endsWith(".promoteCanonicalModel") || name.endsWith(".updateExternalTicket")) shouldBe false
        result._5 shouldBe Vector.fill(4)(false)
      }
    }

    "retain private server inputs and reject unauthorized or stale generated requests" in {
      Given("a real component with one admitted application and another without a host selection")
      When("unknown/read-only principals, stale refs and unknown admission fields reach generated Operations")
      val fixture = create()
      val request = refinement(fixture.current)
      val unknown = host(Some(fixture.application), "unconfigured-client")
      val reader = host(Some(fixture.application), "reader")
      val authorized = host(Some(fixture.application), "codex")
      val default = host(None, "codex")
      val denied = Vector(executeOperation("getModelEditCandidate", "not JSON", unknown), executeOperation("applyModelEdit", edit(request), reader),
        executeOperation("getModelEditCandidate", ModelEditOperationJson.selection(request.selection.copy(candidate = ModelEditProjectionSpecFixture.record("candidate", 500))), authorized),
        executeOperation("getModelEditCandidate", "{\"authorization\":{}}", authorized))
      val unavailable = invoke("getModelEditCandidate", ModelEditOperationJson.selection(request.selection), default)
      val read = invoke("getModelEditCandidate", ModelEditOperationJson.selection(request.selection), reader)
      Then("authentication precedes payload interpretation and the default plugin fabricates no session")
      denied.map(_.isSuccess) shouldBe Vector.fill(4)(false)
      unavailable.getString("status") shouldBe Some("UNAVAILABLE")
      read.getString("resultJson").get should not include "principalId"
      read.getString("resultJson").get should not include "dispositions"
      read.getString("status") shouldBe Some("OK")
    }

    "offer and apply stable Slice refinement while retaining exact independent Specification and Ticket links" in {
      Given("one stable Slice with independent existing external reference links")
      When("an ordinary generated client discovers Slice operations, refines its structure and reads its trace")
      val fixture = create()
      val runtime = host(Some(fixture.application), "codex")
      val original = fixture.current.candidate.slices.head
      val proposed = original.copy(reference = original.reference.copy(recordRevision = InternalModelRecordRevision.from(2).toOption.get),
        title = "Refined stable Slice", intent = "Refine behavior without changing external identities")
      val context = ModelEditPalette.Context(ModelEditPalette.Surface.EventStorming, fixture.current.scope, Subject.Slice(original.identity))
      val palette = invoke("listModelEditOperations", ModelEditOperationJson.context(ContextRequest(selected(fixture.current), context)), runtime)
      val operation = ModelEditSpecFixture.operationFor(fixture.current, Command.RefineUseCaseSlice(original.reference, proposed))
      val successor = fixture.current.candidate.reference.copy(recordRevision = InternalModelRecordRevision.from(2).toOption.get)
      val applied = invoke("applyModelEdit", edit(EditRequest(selected(fixture.current), context, operation, successor)), runtime)
      val trace = invoke("getModelEditTraces", ModelEditOperationJson.selection(Selection(fixture.current.reference, successor)), runtime)
      val tools = McpToolCatalog.toolsForComponent(runtime.component).filter(_.name.contains(".CbdModelEdit."))
      val schemas = tools.map(tool => tool.inputSchema.hcursor.get[Vector[String]]("required"))
      val kinds = runtime.component.operationDefinitions.filter(_.name == "applyModelEdit").map(_.kind).toSet
      Then("the normal generated contract retains stable semantic identity and exposes no ticket-system effect")
      palette.getString("resultJson").get should include("RefineUseCaseSlice")
      palette.getString("resultJson").get should include("LinkSliceSpecification")
      palette.getString("resultJson").get should include("LinkSliceTicket")
      applied.getString("status") shouldBe Some("OK")
      trace.getString("resultJson").get should include("Refined stable Slice")
      trace.getString("resultJson").get should include(original.identity.value)
      trace.getString("resultJson").get should include("OrderSubmissionSpec")
      trace.getString("resultJson").get should include("spec-r7")
      trace.getString("resultJson").get should include("I_kwDOOrderIssue42")
      schemas shouldBe Vector.fill(8)(Right(Vector("requestJson")))
      kinds shouldBe Set("COMMAND")
    }
  }
}

private[runtime] object ModelEditExternalOperationsSpec {
  import ModelEditApplicationSpecFixture.*
  import CbdSupportComponent.CbdModelEditService.*
  final case class Host(component: Component, context: ExecutionContext, interpreted: () => Int)

  def host(application: Option[ModelEditApplication], principal: String): Host = {
    val managed = ManagedEffectReviewSpecSupport.contextFixture()
    val selected = ExecutionContext.withSecurityContext(managed.context, principalContext(principal).security)
    val factory = application.fold(new impl.ComponentFactory())(value => new impl.ComponentFactory(value))
    val component = factory.create(ComponentCreate(DefaultSubsystemFactory.default(Some("server")), ComponentOrigin.Embed)).primary
    Host(component, selected, managed.interpreted)
  }
  def executeOperation(operation: String, document: String, host: Host): Consequence[OperationResponse] = {
    val request = Request.of(component = "CbdSupport", service = "CbdModelEdit", operation = operation,
      properties = List(Property("requestJson", document, None)))
    val action: Consequence[Action] = operation match {
      case "inspectModelEdit" => InspectModelEditOperation.createOperationRequest(request)
      case "listModelEditOperations" => ListModelEditOperationsOperation.createOperationRequest(request)
      case "getModelEditCandidate" => GetModelEditCandidateOperation.createOperationRequest(request)
      case "applyModelEdit" => ApplyModelEditOperation.createOperationRequest(request)
      case "getModelEditView" => GetModelEditViewOperation.createOperationRequest(request)
      case "getModelEditDiff" => GetModelEditDiffOperation.createOperationRequest(request)
      case "getModelEditTraces" => GetModelEditTracesOperation.createOperationRequest(request)
      case "getModelEditReview" => GetModelEditReviewOperation.createOperationRequest(request)
      case _ => Consequence.operationInvalid("Unknown Model Edit Operation")
    }
    action.flatMap(value => value.createCall(ActionCall.Core(value, host.context, Some(host.component), None)).execute())
  }
  def invoke(operation: String, document: String, host: Host): Record = successfulValue(executeOperation(operation, document, host)) match {
    case OperationResponse.RecordResponse(record) => record
    case _ => throw new IllegalStateException("Expected generated Model Edit record response")
  }
}
