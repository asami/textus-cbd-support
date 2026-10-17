package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.effect.{ExternalEffectClassification, ExternalEffectRoute, ExternalEffectUsage}
import org.goldenport.cncf.processexecution.{GitOperationKind, ProcessExternalEffectPolicy, SbtOperation}
import org.goldenport.cncf.provider.{ProviderCall, ProviderEngine, ProviderRequest}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.simplemodeling.textus.cbdsupport.value

/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 */
/** Fixed diagnostic for supplied source-use observations; never executes them. */
object CarReviewManagedEffectProvider {
  def reviewC(
    core: ActionCall.Core,
    input: value.ManagedEffectReviewRequest
  ): Consequence[value.ManagedEffectReviewResult] =
    _validate_c(input).flatMap { usages =>
      val attributes = Map("review_id" -> input.reviewId, "revision" -> input.revision.toString,
        "subject_count" -> input.subjects.size.toString)
      val providercore = ProviderCall.Core(
        ProviderRequest("cbd-car-review-managed-effects", "review", attributes),
        core.executionContext, core.component, core.correlationId)
      ProviderEngine.execute(ReviewCall(providercore, input, usages))
    }

  private def _validate_c(input: value.ManagedEffectReviewRequest): Consequence[Vector[ExternalEffectUsage]] = {
    if (input == null) _invalid_c("request")
    else {
      Consequence.zipN(Vector(
        _check(_valid_identity(input.reviewId), "reviewId"),
        _check(input.revision > 0, "revision"),
        _check(input.subjects != null && input.subjects.nonEmpty && input.subjects.size <= 64 &&
          input.subjects.forall(_ != null), "subjects")
      )).flatMap { _ =>
        _check(input.subjects.map(_.identity).distinct.size == input.subjects.size, "subject-identities")
          .flatMap(_ => Consequence.zipN(input.subjects.map(_usage_c)).map(_.toVector))
      }
    }
  }

  private def _usage_c(subject: value.ManagedEffectReviewSubject): Consequence[ExternalEffectUsage] =
    Consequence.zipN(Vector(
      _check(_valid_identity(subject.identity), "identity"),
      _check(_valid_text(subject.source, 2048), "source"),
      _check(subject.operation != null && subject.commandName != null, "usage-facts")
    )).flatMap { _ =>
      subject.usage match {
        case "process" if subject.operation.isEmpty && subject.commandName.exists(_valid_text(_, 1024)) =>
          Consequence.success(ExternalEffectUsage.ProcessCommand(subject.commandName.get))
        case "git" if subject.commandName.isEmpty =>
          GitOperationKind.values.find(kind => subject.operation.contains(kind.wireName)) match {
            case Some(kind) => Consequence.success(ExternalEffectUsage.TypedGit(kind))
            case None => _invalid_c("operation")
          }
        case "sbt" if subject.commandName.isEmpty =>
          subject.operation match {
            case Some("compile") => Consequence.success(ExternalEffectUsage.TypedSbt(SbtOperation.Compile))
            case Some("test") => Consequence.success(ExternalEffectUsage.TypedSbt(SbtOperation.Test))
            case Some("test-only") => Consequence.success(ExternalEffectUsage.TypedSbt(SbtOperation.TestOnly))
            case _ => _invalid_c("operation")
          }
        case "resource-read" if subject.operation.isEmpty && subject.commandName.isEmpty =>
          Consequence.success(ExternalEffectUsage.ResourceRead)
        case _ => _invalid_c("usage-facts")
      }
    }

  private def _valid_identity(text: String): Boolean =
    text != null && text.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")

  private def _valid_text(text: String, limit: Int): Boolean =
    text != null && !text.isBlank && !text.contains('\u0000') &&
      text.getBytes(StandardCharsets.UTF_8).length <= limit

  private def _check(valid: Boolean, field: String): Consequence[Unit] =
    if (valid) Consequence.unit else _invalid_c(field)

  private def _invalid_c[A](field: String): Consequence[A] =
    Consequence.argumentFormatError(field, "valid bounded managed-effect review input", "invalid")

  private final case class ReviewCall(
    core: ProviderCall.Core,
    input: value.ManagedEffectReviewRequest,
    usages: Vector[ExternalEffectUsage]
  ) extends ProviderCall[value.ManagedEffectReviewResult] {
    override def calltreeResultAttributes(result: value.ManagedEffectReviewResult): Map[String, String] = Map.empty

    protected def build_Program: ExecUowM[value.ManagedEffectReviewResult] =
      provider_step("review-managed-effects", Map("review_id" -> input.reviewId,
        "revision" -> input.revision.toString, "subject_count" -> input.subjects.size.toString)) {
        Consequence.zipN(input.subjects.zip(usages).map { case (subject, usage) =>
          ProcessExternalEffectPolicy.classifyUsageC(usage).map(classification => _observation(subject, classification))
        }).map(observations => value.ManagedEffectReviewResult(input.reviewId, input.revision, observations.toVector))
      }
  }

  private def _observation(
    subject: value.ManagedEffectReviewSubject,
    classification: ExternalEffectClassification
  ): value.ManagedEffectReviewObservation = {
    val (outcome, rule, remediation, detail) = classification.route match {
      case ExternalEffectRoute.CapabilityLaundering =>
        ("finding", "capability-laundering", "use-managed-effect",
          "The supplied process observation names a recognized effect requiring its CNCF-managed route.")
      case ExternalEffectRoute.Direct =>
        ("finding", "direct-process", "use-managed-effect",
          "The supplied process observation names a direct shell, interpreter or wrapper route.")
      case ExternalEffectRoute.Unclassified =>
        ("unknown", "unclassified", "supply-effect-evidence",
          "The supplied process observation is outside bounded recognition; its effects remain unknown.")
      case ExternalEffectRoute.Managed =>
        ("managed-use", "managed-route", "retain-managed-route",
          "The supplied typed observation describes a CNCF-managed route; source behavior is not verified.")
    }
    value.ManagedEffectReviewObservation(subject.identity, subject.source,
      "cbd.car-review.managed-effects." + rule, outcome,
      classification.effectKinds.toVector.map(_.toString).sorted,
      classification.route.diagnosticCode, remediation, detail)
  }
}
