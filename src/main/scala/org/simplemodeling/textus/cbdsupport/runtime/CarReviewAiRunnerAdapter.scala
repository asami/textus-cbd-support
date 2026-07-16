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
        CarReviewAiReviewResponse(response.record, response.model, safeExecutionFacts(request, response.metadata))
      }
    }

  private def _validate(request: CarReviewAiReviewRequest): Consequence[Unit] =
    if !purposes.contains(request.purpose) then Consequence.operationInvalid("car-review-ai-purpose-invalid")
    else if request.evidence.isEmpty || request.evidence.size > maxEvidence then Consequence.operationInvalid("car-review-ai-evidence-bound-invalid")
    else if request.evidence.exists(value => value.evidenceId.value.isEmpty || value.subject.length > maxSubject || value.summary.isEmpty || value.summary.length > maxSummary) then
      Consequence.operationInvalid("car-review-ai-evidence-invalid")
    else Consequence.unit

  private def _prompt(request: CarReviewAiReviewRequest): String =
    request.evidence.sortBy(_.evidenceId.value).map { value =>
      s"evidence=${value.evidenceId.value}; subject=${value.subject}; summary=${value.summary}"
    }.mkString(s"CAR Review purpose=${request.purpose}\n", "\n", "")
}

object CarReviewAiRunnerAdapter {
  val purposes: Set[String] = Set(
    "car-review.documentation-clarity",
    "car-review.semantic-consistency",
    "car-review.requirement-traceability"
  )
  val maxEvidence = 100
  val maxSubject = 160
  val maxSummary = 1024
  val safeMetadataKeys: Set[String] = Set(
    "ai.execution.provider",
    "ai.execution.mode",
    "ai.execution.engine",
    "ai.execution.model",
    "ai.execution.purpose",
    "ai.execution.normalization_mode",
    "ai.execution.finish_reason",
    "ai.execution.input_digest",
    "ai.execution.output_digest",
    "ai.execution.retry_count",
    "ai.usage.input_tokens",
    "ai.usage.output_tokens",
    "ai.usage.total_tokens",
    "ai.usage.request_count",
    "ai.limitation.codes"
  )

  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _counter_pattern = "0|[1-9][0-9]{0,11}".r
  private val _identifier_pattern = "[A-Za-z0-9][A-Za-z0-9._:/-]{0,159}".r

  def safeExecutionFacts(request: CarReviewAiReviewRequest, metadata: Map[String, String]): Map[String, String] =
    metadata.collect {
      case (key, value) if safeMetadataKeys.contains(key) && _safe_value(key, value, request.purpose) => key -> value.trim
    }

  private def _safe_value(key: String, value: String, purpose: String): Boolean = {
    val normalized = Option(value).map(_.trim).getOrElse("")
    key match {
      case "ai.execution.input_digest" | "ai.execution.output_digest" => _digest_pattern.matches(normalized)
      case "ai.execution.retry_count" | "ai.usage.input_tokens" | "ai.usage.output_tokens" | "ai.usage.total_tokens" | "ai.usage.request_count" => _counter_pattern.matches(normalized)
      case "ai.execution.purpose" => normalized == purpose
      case "ai.limitation.codes" => normalized.nonEmpty && normalized.length <= 1024 && normalized.split(",").forall(x => _identifier_pattern.matches(x.trim))
      case _ => normalized.nonEmpty && normalized.length <= 160 && _identifier_pattern.matches(normalized)
    }
  }
}
