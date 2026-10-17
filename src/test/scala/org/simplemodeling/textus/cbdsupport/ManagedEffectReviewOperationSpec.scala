package org.simplemodeling.textus.cbdsupport

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.mcp.McpToolCatalog
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.{value => cbdvalue}

/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 */
final class ManagedEffectReviewOperationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Generated managed effect Review operation" should {
    "E1 connect the actual ComponentFactory hook" should
      afterWord("in spec:car-review-managed-effect-contract, example:E1, rules:R1,R3, phase:16") {
      "consume a normal typed request and return typed attributable observations" in {
        Given("docs/spec/car-review-managed-effect-contract.md R1,R3 E1 with the ordinary component factory and generated typed input")
        val factory = new impl.ComponentFactory()
        val component = factory.create(ComponentCreate(DefaultSubsystemFactory.default(Some("server")),
          ComponentOrigin.Repository("managed-effect-operation-spec"))).primary
        val fixture = ManagedEffectReviewSpecSupport.contextFixture()
        val input = cbdvalue.ManagedEffectReviewRequest("operation-review", 1L,
          Vector(cbdvalue.ManagedEffectReviewSubject("original-file-use", "src/ObserveSelectedDocuments.scala", "process", None, Some("cat"))))
        val request = _request(input.toRecord())
        val action = _value(CbdSupportComponent.CbdReviewAdminService.ReviewManagedEffectsOperation.createOperationRequest(request))
        val core = ActionCall.Core(action, fixture.context, Some(component), None)
        When("the generated action resolves the handwritten factory hook and executes through UnitOfWork")
        val response = _value(action.createCall(core).execute())
        val result = response match {
          case OperationResponse.RecordResponse(record) => _value(cbdvalue.ManagedEffectReviewResult.createC(record))
          case _ => fail("Expected the ordinary generated result record")
        }
        Then("the typed result preserves caller identity and attributes the original Process-to-file finding")
        result.reviewId shouldBe input.reviewId
        result.revision shouldBe input.revision
        result.observations.size shouldBe 1
        result.observations.head.identity shouldBe "original-file-use"
        result.observations.head.source shouldBe "src/ObserveSelectedDocuments.scala"
        result.observations.head.effectKinds shouldBe Vector("FileRead")
        result.observations.head.outcome shouldBe "finding"
        result.observations.head.ruleId shouldBe "cbd.car-review.managed-effects.capability-laundering"
        fixture.context.unitOfWork.executionContext should be theSameInstanceAs fixture.context
        fixture.interpretedOperationCount shouldBe 0
        fixture.providerStepCount shouldBe 1
      }
    }

    "E2 refuse incompatible normal request input" should
      afterWord("in spec:car-review-managed-effect-contract, example:E2, rules:R1,R2, phase:16") {
      "reject missing required fields and incompatible revision values" in {
        Given("docs/spec/car-review-managed-effect-contract.md R1,R2 E2 with ordinary requests missing a review ID or carrying a non-long revision")
        val component = new impl.ComponentFactory().create(ComponentCreate(DefaultSubsystemFactory.default(Some("server")),
          ComponentOrigin.Repository("managed-effect-operation-spec"))).primary
        val fixture = ManagedEffectReviewSpecSupport.contextFixture()
        val requests = Vector(_request(Record.dataAuto("revision" -> 1L)),
          _request(Record.dataAuto("reviewId" -> "review", "revision" -> "incompatible")))
        When("the ordinary generated envelope and typed value decoding interpret these requests")
        val results = requests.map { request =>
          CbdSupportComponent.CbdReviewAdminService.ReviewManagedEffectsOperation.createOperationRequest(request).flatMap { action =>
            action.createCall(ActionCall.Core(action, fixture.context, Some(component), None)).execute()
          }
        }
        Then("the normal generated envelope or ordinary typed value decoding rejects each request before provider evaluation")
        results.foreach { result =>
          result.isFaillure shouldBe true
          result.toOption shouldBe None
        }
        fixture.interpretedOperationCount shouldBe 0
        fixture.providerStepCount shouldBe 0
      }
    }

    "E3 retain private diagnostic exposure" should
      afterWord("in spec:car-review-managed-effect-contract, example:E3, rules:R1, phase:16") {
      "keep the QUERY available internally and absent from the MCP tool catalog" in {
        Given("docs/spec/car-review-managed-effect-contract.md R1 E3 with a normally constructed CBD component")
        val component = new impl.ComponentFactory().create(ComponentCreate(DefaultSubsystemFactory.default(Some("server")),
          ComponentOrigin.Repository("managed-effect-operation-spec"))).primary
        When("the ordinary operation and MCP projections inspect its service boundary")
        val definition = component.operationDefinitions.find(_.name == "reviewManagedEffects")
        val ready = component.isMcpReady("CbdReviewAdmin", "reviewManagedEffects")
        val tools = McpToolCatalog.toolsForComponent(component)
        Then("the typed QUERY is present internally while the whole Review administration service remains non-MCP")
        definition.map(_.kind) shouldBe Some("QUERY")
        definition.map(_.inputType) shouldBe Some("ManagedEffectReviewRequest")
        definition.map(_.outputType) shouldBe Some("ManagedEffectReviewResult")
        ready shouldBe false
        tools.exists(_.name.contains(".CbdReviewAdmin.")) shouldBe false
      }
    }
  }

  private def _request(record: Record): Request = Request.of(component = "CbdSupport", service = "CbdReviewAdmin",
    operation = "reviewManagedEffects", properties = record.fields.map(field => Property(field.key, field.value.single, None)).toList)

  private def _value[A](result: Consequence[A]): A = result match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => fail(conclusion.display)
  }
}
