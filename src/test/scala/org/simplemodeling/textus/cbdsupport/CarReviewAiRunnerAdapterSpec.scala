package org.simplemodeling.textus.cbdsupport

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.spi.ai.runner.{AiChatRequest, AiChatResponse, AiGenerateRequest, AiGenerateResponse, AiRecordRequest, AiRecordResponse, AiRunner}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewAiRunnerAdapterSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CBD CAR Review AiRunner adapter" should {
    "use only an opt-in purpose profile, bounded admitted evidence, and safe structured facts" in {
      Given("a provider-neutral AiRunner")
      given ExecutionContext = ExecutionContext.create()
      val runner = new RecordingRunner
      val adapter = new CarReviewAiRunnerAdapter(runner)

      When("CBD submits one bounded documentation Review request")
      val response = adapter.review(CarReviewAiReviewRequest(
        ReviewId("review-ai-001"),
        "car-review.documentation-clarity",
        Vector(CarReviewAiEvidence(ReviewEvidenceId("cozy:documentation"), "component:account", "The public guide omits operation rationale."))
      )).toOption.get

      Then("Textus AI receives a purpose-selected structured request without provider tools or raw credentials")
      runner.request.map(_.requirement.purpose) shouldBe Some(Some("car-review.documentation-clarity"))
      runner.request.map(_.requirement.tools) shouldBe Some(Vector.empty)
      runner.request.map(_.prompt) shouldBe Some("CAR Review purpose=car-review.documentation-clarity\nevidence=cozy:documentation; subject=component:account; summary=The public guide omits operation rationale.")
      response.executionFacts shouldBe Map("ai.execution.provider" -> "fixture", "ai.limitation.codes" -> "usage_unavailable")
      response.executionFacts should not contain "provider.raw.response"
    }

    "refuse unconfigured purposes and unbounded evidence before AI execution" in {
      given ExecutionContext = ExecutionContext.create()
      val runner = new RecordingRunner
      val adapter = new CarReviewAiRunnerAdapter(runner)
      val invalidPurpose = adapter.review(CarReviewAiReviewRequest(ReviewId("review-ai-002"), "car-review.web-search", Vector(CarReviewAiEvidence(ReviewEvidenceId("evidence"), "component", "summary"))))
      val tooMany = adapter.review(CarReviewAiReviewRequest(ReviewId("review-ai-003"), "car-review.semantic-consistency", Vector.fill(CarReviewAiRunnerAdapter.MaxEvidence + 1)(CarReviewAiEvidence(ReviewEvidenceId("evidence"), "component", "summary"))))

      invalidPurpose.isSuccess shouldBe false
      tooMany.isSuccess shouldBe false
      runner.request shouldBe None
    }
  }

  private final class RecordingRunner extends AiRunner {
    var request: Option[AiRecordRequest] = None
    def generate(req: AiGenerateRequest)(using ExecutionContext): Consequence[AiGenerateResponse] = Consequence.operationInvalid("not-used")
    def chat(req: AiChatRequest)(using ExecutionContext): Consequence[AiChatResponse] = Consequence.operationInvalid("not-used")
    def generateRecord(req: AiRecordRequest)(using ExecutionContext): Consequence[AiRecordResponse] = {
      request = Some(req)
      Consequence.success(AiRecordResponse(Record.dataAuto("findings" -> Vector.empty[Record]), Some("fixture-model"), Map(
        "ai.execution.provider" -> "fixture",
        "ai.limitation.codes" -> "usage_unavailable",
        "provider.raw.response" -> "credential=not-public"
      )))
    }
  }
}
