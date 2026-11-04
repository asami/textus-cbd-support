package org.simplemodeling.textus.cbdsupport

import io.circe.Json
import io.circe.syntax.*
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
import org.simplemodeling.textus.cbdsupport.runtime.*
import FindingResolutionApplication.*
import FindingResolutionApplication.JsonProtocol.given
import FindingResolutionApplicationSpecFixture.*

/*
 * @since   Nov.  5, 2026
 * @version Nov.  5, 2026
 */
final class FindingResolutionOperationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import FindingResolutionOperationSpec.*

  "Ordinary generated finding resolution Operations" should {
    "host-owned preparation and admission" which {
      "dispatch all five operations through generated ActionCalls without writing canonical source" in {
        Given("an actual ComponentFactory embedding with independent host inspection and developer direction fixtures")
        withCanonicalSource { root =>
          val before = sourceBytes(root)
          val fixture = createFindingFixture()
          val runtime = host(new impl.ComponentFactory(fixture.application))
          When("normal generated requests import, group, propose, admit selected scope and read retained state")
          val imported = invoke("importFindingInspection", Selection(fixture.imported.reference).asJson.noSpaces, runtime)
          val grouped = invoke("groupFindingResolutions", GroupingRequest(fixture.imported.reference,
            fixture.keys.map(key => FindingResolution.Remediation(key, "Intentional host adapter"))).asJson.noSpaces, runtime)
          val proposed = invoke("proposeFindingResolution", proposal(fixture).asJson.noSpaces, runtime)
          val admitted = invoke("decideFindingResolution", decision(fixture, fixture.keys.take(1)).asJson.noSpaces, runtime)
          val retained = invoke("getFindingResolution", Selection(fixture.imported.reference).asJson.noSpaces, runtime)
          val tools = McpToolCatalog.toolsForComponent(runtime.component).filter(_.name.contains(".CbdFindingResolution."))
          Then("the generated service retains exact evidence and scope and publishes only the declared safe operation inventory")
          Vector(imported, grouped, proposed, admitted, retained).foreach(_.getString("status") shouldBe Some("OK"))
          val result = io.circe.parser.parse(retained.getString("resultJson").get).toOption.get
          result.hcursor.get[Int]("rawFindingCount") shouldBe Right(5)
          result.hcursor.get[Int]("actionableCount") shouldBe Right(3)
          result.hcursor.downField("decisions").downArray.get[Vector[org.goldenport.cncf.finding.FindingDispositionV1.Key]]("keys") shouldBe Right(fixture.keys.take(1))
          result.hcursor.downField("decisions").downArray.get[String]("principalId") shouldBe Right("developer")
          tools.map(_.name).toSet shouldBe FindingResolutionApplication.operations.map(name => s"${CbdSupportComponent.name}.CbdFindingResolution.$name")
          tools.map(_.inputSchema.hcursor.get[Vector[String]]("required")) shouldBe Vector.fill(5)(Right(Vector("requestJson")))
          runtime.component.operationDefinitions.filter(value => Set("importFindingInspection", "proposeFindingResolution", "decideFindingResolution")(value.name))
            .map(_.kind).toSet shouldBe Set("COMMAND")
          runtime.component.core.protocol.services.services.flatMap(_.operations.operations.toVector)
            .exists(_.name.contains("InternalModelHistoryEntry")) shouldBe false
          sourceBytes(root) shouldBe before
        }
      }

      "retain authenticated rejection and deny forged authority through the same generated boundary" in {
        Given("a normal generated component and a proposal with independent host direction")
        val fixture = createFindingFixture()
        val runtime = host(new impl.ComponentFactory(fixture.application))
        invoke("importFindingInspection", Selection(fixture.imported.reference).asJson.noSpaces, runtime)
        invoke("proposeFindingResolution", proposal(fixture).asJson.noSpaces, runtime)
        When("a forged client direction, wrong principal, and then an authorized rejection reach ordinary ActionCalls")
        val request = decision(fixture, fixture.keys.take(1)).copy(choice = FindingResolution.Choice.Reject).asJson
        val forged = executeOperation("decideFindingResolution", request.deepMerge(Json.obj("direction" -> record("forged").asJson)).noSpaces, runtime)
        val unknown = host(new impl.ComponentFactory(fixture.application), "unknown")
        val denied = executeOperation("decideFindingResolution", request.noSpaces, unknown)
        val rejected = invoke("decideFindingResolution", request.noSpaces, runtime)
        Then("the client cannot grant authority and the retained decision records the actual authenticated rejection")
        forged.isSuccess shouldBe false
        denied.isSuccess shouldBe false
        io.circe.parser.parse(rejected.getString("resultJson").get).toOption.get.hcursor
          .downField("decisions").downArray.get[String]("choice") shouldBe Right("Reject")
      }
    }

    "explicit embedding availability and Phase12 compatibility" which {
      "keep unconfigured construction truthful and retain both admitted applications independently" in {
        Given("normal unconfigured, existing Model Edit-only, finding-only and both-application factory constructors")
        val fixture = createFindingFixture()
        val (model, modelselection) = modelEdit()
        val plain = host(new impl.ComponentFactory())
        val modelonly = host(new impl.ComponentFactory(model), "reader")
        val both = host(new impl.ComponentFactory(model, fixture.application), "reader")
        val findingonly = host(new impl.ComponentFactory(fixture.application))
        val selection = Selection(fixture.imported.reference).asJson.noSpaces
        When("clients inspect availability and import findings through the shared factory without changing Model Edit")
        val unavailable = Vector(plain, modelonly).map(value => invoke("getFindingResolution", selection, value))
        val before = modelRead(modelselection, both)
        val pending = invoke("getFindingResolution", selection, findingonly)
        val imported = invoke("importFindingInspection", selection, both)
        val after = modelRead(modelselection, both)
        val forgery = executeOperation("getFindingResolution", "{\"principalId\":\"developer\"}", findingonly)
        Then("absence is explicit and finding preparation leaves the existing Phase12 candidate and unsafe-operation exclusion intact")
        unavailable.foreach(_.getString("status") shouldBe Some("UNAVAILABLE"))
        pending.getString("status") shouldBe Some("UNAVAILABLE")
        imported.getString("status") shouldBe Some("OK")
        before shouldBe after
        before.getString("status") shouldBe Some("OK")
        forgery.isSuccess shouldBe false
        both.component.core.protocol.services.services.flatMap(_.operations.operations.toVector)
          .exists(_.name.contains("InternalModelHistoryEntry")) shouldBe false
        both.component.isMcpReady("CbdReviewAdmin", "startReview") shouldBe false
      }
    }
  }
}

private object FindingResolutionOperationSpec {
  import CbdSupportComponent.CbdFindingResolutionService.*
  final case class Host(component: Component, context: ExecutionContext)

  def host(factory: impl.ComponentFactory, principal: String = "developer"): Host = {
    val managed = ManagedEffectReviewSpecSupport.contextFixture()
    val selected = ExecutionContext.withSecurityContext(managed.context, FindingResolutionApplicationSpecFixture.context(principal).security)
    val component = factory.create(ComponentCreate(DefaultSubsystemFactory.default(Some("server")), ComponentOrigin.Embed)).primary
    Host(component, selected)
  }
  def executeOperation(operation: String, document: String, host: Host): Consequence[OperationResponse] = {
    val request = Request.of(component = "CbdSupport", service = "CbdFindingResolution", operation = operation,
      properties = List(Property("requestJson", document, None)))
    val action: Consequence[Action] = operation match {
      case "importFindingInspection" => ImportFindingInspectionOperation.createOperationRequest(request)
      case "groupFindingResolutions" => GroupFindingResolutionsOperation.createOperationRequest(request)
      case "proposeFindingResolution" => ProposeFindingResolutionOperation.createOperationRequest(request)
      case "decideFindingResolution" => DecideFindingResolutionOperation.createOperationRequest(request)
      case "getFindingResolution" => GetFindingResolutionOperation.createOperationRequest(request)
      case _ => Consequence.operationInvalid("Unknown finding resolution Operation")
    }
    action.flatMap(value => value.createCall(ActionCall.Core(value, host.context, Some(host.component), None)).execute())
  }
  def invoke(operation: String, document: String, host: Host): Record =
    _record_response(executeOperation(operation, document, host))

  def modelRead(document: String, host: Host): Record = {
    val request = Request.of(component = "CbdSupport", service = "CbdModelEdit", operation = "getModelEditCandidate",
      properties = List(Property("requestJson", document, None)))
    val result = CbdSupportComponent.CbdModelEditService.GetModelEditCandidateOperation.createOperationRequest(request)
      .flatMap(value => value.createCall(ActionCall.Core(value, host.context, Some(host.component), None)).execute())
    _record_response(result)
  }
  private def _record_response(result: Consequence[OperationResponse]): Record = result.toOption.get match {
    case OperationResponse.RecordResponse(record) => record
    case _ => throw new IllegalStateException("Expected generated finding record response")
  }
}
