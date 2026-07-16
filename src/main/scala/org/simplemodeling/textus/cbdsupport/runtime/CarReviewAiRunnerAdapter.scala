package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.spi.ai.runner.{AiRecordRequest, AiRunner, AiRunnerRequirement}
import org.goldenport.record.Record

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewAiEvidence(
  evidenceId: ReviewEvidenceId,
  subject: String,
  summary: String
)

final case class CarReviewAiReviewRequest(
  reviewId: ReviewId,
  purpose: String,
  evidence: Vector[CarReviewAiEvidence]
)

final case class CarReviewAiReviewResponse(
  candidate: Record,
  model: Option[String],
  executionFacts: Map[String, String]
)

/**
 * Opt-in, provider-neutral structured AI adapter. CBD supplies only bounded
 * admitted summaries and delegates provider/model selection to Textus AI's
 * `AiRunner` purpose profile. It contains no provider wire or credential API.
 */
final class CarReviewAiRunnerAdapter(runner: AiRunner) {
  import CarReviewAiRunnerAdapter.*

  def review(request: CarReviewAiReviewRequest)(using context: ExecutionContext): Consequence[CarReviewAiReviewResponse] =
    _validate(request).flatMap { _ =>
      val prompt = _prompt(request)
      val schema = Record.dataAuto("required" -> Vector("findings"))
      val aiRequest = AiRecordRequest(
        prompt = prompt,
        schema = schema,
        requirement = AiRunnerRequirement(purpose = Some(request.purpose), tools = Vector.empty),
        metadata = Map("cbd.review.id" -> request.reviewId.value)
      )
      runner.generateRecord(aiRequest).map { response =>
        CarReviewAiReviewResponse(response.record, response.model, response.metadata.filter { case (key, _) => SafeMetadataKeys.contains(key) })
      }
    }

  private def _validate(request: CarReviewAiReviewRequest): Consequence[Unit] =
    if !Purposes.contains(request.purpose) then Consequence.operationInvalid("car-review-ai-purpose-invalid")
    else if request.evidence.isEmpty || request.evidence.size > MaxEvidence then Consequence.operationInvalid("car-review-ai-evidence-bound-invalid")
    else if request.evidence.exists(value => value.evidenceId.value.isEmpty || value.subject.length > MaxSubject || value.summary.isEmpty || value.summary.length > MaxSummary) then
      Consequence.operationInvalid("car-review-ai-evidence-invalid")
    else Consequence.unit

  private def _prompt(request: CarReviewAiReviewRequest): String =
    request.evidence.sortBy(_.evidenceId.value).map { value =>
      s"evidence=${value.evidenceId.value}; subject=${value.subject}; summary=${value.summary}"
    }.mkString(s"CAR Review purpose=${request.purpose}\n", "\n", "")
}

object CarReviewAiRunnerAdapter {
  val Purposes: Set[String] = Set(
    "car-review.documentation-clarity",
    "car-review.semantic-consistency",
    "car-review.requirement-traceability"
  )
  val MaxEvidence = 100
  val MaxSubject = 160
  val MaxSummary = 1024
  val SafeMetadataKeys: Set[String] = Set(
    "ai.execution.provider",
    "ai.execution.mode",
    "ai.execution.engine",
    "ai.execution.model",
    "ai.execution.retry_count",
    "ai.usage.request_count",
    "ai.limitation.codes"
  )
}
