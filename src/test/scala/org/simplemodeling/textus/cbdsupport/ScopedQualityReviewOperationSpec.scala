package org.simplemodeling.textus.cbdsupport

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.mcp.McpToolCatalog
import org.goldenport.cncf.spi.SpiResolver
import org.goldenport.cncf.spi.ai.runner.{AiRunnerApplicationPurpose, AiRunnerApplicationPurposePolicy, AiRunnerApplicationPurposeRegistrationSocketSet, AiRunnerSocketSet}
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.protocol.{Property, Protocol, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.{value => cbdvalue}

/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class ScopedQualityReviewOperationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ScopedQualityReviewSpecSupport.*

  "Generated scoped quality operation (docs/spec/scoped-quality-review.md)" should {
    "O1 connect the normal generated Operation, factory, handwritten ActionCall and provider UoW" in {
      Given("R1,R3,R5: normal typed input and a CBD component with its optional input installed by SPI bootstrap")
      val input = request()
      val runner = new RecordingAiRunner(Consequence.success(response(input)))
      val fixture = runtime(Some(runner))
      val operation = CbdSupportComponent.CbdReviewAdminService.ReviewScopedQualityOperation
      val action = valueOf(operation.createOperationRequest(_request(input.toRecord())))
      val core = ActionCall.Core(action, fixture.context, Some(fixture.component), None)
      When("the normal generated action resolves its factory hook and executes")
      val call = action.createCall(core)
      val output = valueOf(call.execute())
      val result = output match {
        case OperationResponse.RecordResponse(record) => valueOf(cbdvalue.ScopedQualityReviewResult.createC(record))
        case _ => fail("Expected generated typed result record")
      }
      Then("the handwritten call returns a typed covered target with actual provider provenance")
      call shouldBe a[impl.ScopedQualityReviewActionCall]
      result.status shouldBe "completed"
      result.reviewId shouldBe input.reviewId
      result.revision shouldBe input.revision
      result.subjectId shouldBe input.subjectId
      result.subjectRevision shouldBe input.subjectRevision
      result.providerId shouldBe "recording-provider"
      result.findings shouldBe Vector.empty
      runner.requests.size shouldBe 1
      runner.contexts.head should be theSameInstanceAs fixture.context
      fixture.trace should include("review-scoped-quality")
    }

    "O2 reject missing normal required input before AI" in {
      Given("R1,R5: ordinary operation envelopes missing required review or subject references")
      val input = request()
      val runner = new RecordingAiRunner(Consequence.success(response(input)))
      val fixture = runtime(Some(runner))
      val records = Vector(Record.dataAuto("revision" -> 1L), Record.dataAuto("reviewId" -> "review-1", "revision" -> 1L))
      When("normal generated input decoding and ActionCall execution receive the incomplete requests")
      val results = records.map { record =>
        CbdSupportComponent.CbdReviewAdminService.ReviewScopedQualityOperation.createOperationRequest(_request(record)).flatMap { action =>
          action.createCall(ActionCall.Core(action, fixture.context, Some(fixture.component), None)).execute()
        }
      }
      Then("each request fails through the ordinary input contract before the provider calls AI")
      results.foreach(_.isFaillure shouldBe true)
      runner.requests shouldBe Vector.empty
      fixture.trace should not include "review-scoped-quality"
    }

    "O3 register the semantic purpose through SPI while keeping the QUERY private and managed effects independent of AI" in {
      Given("R3,R5: factory-created CBD instances without AI and an ordinary application-purpose registration consumer")
      val fixture = runtime(None)
      val other = runtime(None)
      val component = fixture.component
      given ExecutionContext = fixture.context
      val registrationset = new AiRunnerApplicationPurposeRegistrationSocketSet {}
      val consumer = new Component() {}.withPort(Component.Port.input(registrationset))
      val identity = ComponentId("org.simplemodeling.textus.scopedquality.SpecPurposeConsumer")
      consumer.initialize(ComponentInit(subsystem = DefaultSubsystemFactory.default(Some("server")),
        core = Component.Core.create(identity.name, identity, ComponentInstanceId.default(identity), Protocol.empty),
        origin = ComponentOrigin.Builtin))
      val input = cbdvalue.ManagedEffectReviewRequest("managed-review", 1L,
        Vector(cbdvalue.ManagedEffectReviewSubject("resource", "src/Example.scala", "resource-read", None, None)))
      val envelope = Request.of(component = "CbdSupport", service = "CbdReviewAdmin", operation = "reviewManagedEffects",
        properties = input.toRecord().fields.map(field => Property(field.key, field.value.single, None)).toList)
      When("normal SPI collects purpose registrations, projections inspect the component and the managed-effects query executes")
      val resolved = SpiResolver.resolve(Vector(component, consumer))
      val definition = component.operationDefinitions.find(_.name == "reviewScopedQuality")
      val tools = McpToolCatalog.toolsForComponent(component)
      val output = CbdSupportComponent.CbdReviewAdminService.ReviewManagedEffectsOperation.createOperationRequest(envelope).flatMap { action =>
        action.createCall(ActionCall.Core(action, fixture.context, Some(component), None)).execute()
      }
      Then("exactly one semantic purpose has its standard purpose and default policy, while the QUERY stays private and managed effects run")
      resolved.isSuccess shouldBe true
      registrationset.registrations.size shouldBe 1
      registrationset.registrations.flatMap(_.purposes) shouldBe Vector(
        AiRunnerApplicationPurpose(
          name = "car-review.semantic-consistency",
          defaultStandardPurpose = "software-analysis",
          defaultPolicy = AiRunnerApplicationPurposePolicy()
        )
      )
      definition.map(_.kind) shouldBe Some("QUERY")
      definition.map(_.inputType) shouldBe Some("ScopedQualityReviewRequest")
      definition.map(_.outputType) shouldBe Some("ScopedQualityReviewResult")
      component.isMcpReady("CbdReviewAdmin", "reviewScopedQuality") shouldBe false
      tools.exists(_.name.contains(".CbdReviewAdmin.")) shouldBe false
      component.port.get[AiRunnerSocketSet].get should not be theSameInstanceAs(other.component.port.get[AiRunnerSocketSet].get)
      component.port.get[AiRunnerSocketSet].get.spiRequired shouldBe false
      output.isSuccess shouldBe true
    }
  }

  private def _request(record: Record): Request = Request.of(component = "CbdSupport", service = "CbdReviewAdmin",
    operation = "reviewScopedQuality", properties = record.fields.map(field => Property(field.key, field.value.single, None)).toList)
}
