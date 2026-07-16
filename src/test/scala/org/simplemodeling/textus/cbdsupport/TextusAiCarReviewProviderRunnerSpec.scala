package org.simplemodeling.textus.cbdsupport

import io.circe.{Json, JsonObject, Printer}
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
final class TextusAiCarReviewProviderRunnerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The Textus AI CAR Review provider runner" should {
    "normalize one structured AI candidate into an attributable advisory bundle" in {
      Given("one injected provider-neutral AI runner and bounded admitted evidence")
      given ExecutionContext = ExecutionContext.create()
      val recording = new RecordingRunner(_candidate)
      val profile = _profile()
      val runner = new TextusAiCarReviewProviderRunner(profile, new CarReviewAiRunnerAdapter(recording))
      val descriptor = TextusAiCarReviewProviderRunner.descriptorDocument(profile)
      val request = _request(descriptor)

      When("CBD executes the admitted Textus AI provider")
      val result = runner.execute(request)

      Then("the candidate is a bounded advisory Finding and the exact bundle is admitted")
      val bundle = result match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case value => fail(s"Expected bundle completion but got $value")
      }
      val admitted = CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(
        request.reviewId,
        request.target,
        ProviderBundleAvailability.Enabled,
        descriptor,
        request.providerRequest,
        bundle
      ))
      admitted shouldBe a[ProviderBundleAdmissionOutcome.Admitted]
      val reconciled = CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(
        admitted.asInstanceOf[ProviderBundleAdmissionOutcome.Admitted].value,
        bundle
      ))).toOption.get
      val registry = new CarReviewProviderRegistry()
      registry.register(descriptor, runner).isRight shouldBe true
      val coordinated = new CarReviewProviderExecutionCoordinator().execute(request, registry)
      reconciled.observations.map(_.`type`.value) shouldBe Vector("finding")
      reconciled.observations.map(_.rule.id.value) shouldBe Vector("ai.advisory.documentation.clarity")
      reconciled.limitations.map(_.code) should contain("ai-advisory-only")
      coordinated shouldBe a[ProviderBundleExecutionOutcome.Admitted]
      bundle should include("candidateDigest")
      bundle should not include "provider.raw.response"
      recording.request.map(_.requirement.purpose) shouldBe Some(Some("car-review.documentation-clarity"))
      recording.request.map(_.requirement.tools) shouldBe Some(Vector.empty)
    }

    "refuse malformed structured output and cancellation without a provider fallback" in {
      Given("a provider whose structured result is malformed")
      given ExecutionContext = ExecutionContext.create()
      val profile = _profile()
      val malformed = new TextusAiCarReviewProviderRunner(profile, new CarReviewAiRunnerAdapter(new RecordingRunner(_unsupported_candidate)))
      val descriptor = TextusAiCarReviewProviderRunner.descriptorDocument(profile)
      val malformedresult = malformed.execute(_request(descriptor))

      When("the Review is cancelled before invoking an otherwise usable provider")
      val recording = new RecordingRunner(_candidate)
      val cancelled = new TextusAiCarReviewProviderRunner(profile, new CarReviewAiRunnerAdapter(recording))
      val request = _request(descriptor, reviewId = "review-ai-cancelled")
      cancelled.cancel(request)
      val cancelledresult = cancelled.execute(request)

      Then("both outcomes are attributable failures and no alternate AI provider is selected")
      malformedresult should matchPattern {
        case ProviderBundleRunnerResult.Failed("ai-structured-output-invalid", _, _) =>
      }
      cancelledresult should matchPattern {
        case ProviderBundleRunnerResult.Failed("provider-cancelled", _, _) =>
      }
      recording.request shouldBe None
    }
  }

  private val _candidate = Record.dataAuto("findings" -> Vector(Record.dataAuto(
    "rule_id" -> "documentation.clarity",
    "severity" -> "medium",
    "message" -> "Describe the operation input and output."
  )))

  private val _unsupported_candidate = Record.dataAuto("findings" -> Vector(Record.dataAuto(
    "rule_id" -> "documentation.clarity",
    "severity" -> "warning",
    "message" -> "This severity is outside the CBD Review v1 vocabulary."
  )))

  private def _profile(): TextusAiCarReviewProviderProfile =
    TextusAiCarReviewProviderProfile(
      ReviewProviderIdentity(ReviewProviderId("textus-ai"), ReviewVersion("0.2.1")),
      ReviewRuleIdentity(ReviewRuleId("textus-ai.car-review"), ReviewVersion("1.0.0")),
      "car-review.documentation-clarity",
      Vector(CarReviewAiEvidence(ReviewEvidenceId("cozy:documentation"), "component:account", "The public guide omits operation rationale."))
    )

  private def _request(descriptor: String, reviewId: String = "review-ai-provider-001"): ProviderBundleExecutionRequest = {
    val target = ReviewTarget(
      ReviewTargetKind("project"),
      Some("org.textus"),
      "textus-user-account",
      Some(ReviewVersion("0.2.0-SNAPSHOT")),
      ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
    )
    ProviderBundleExecutionRequest(
      ReviewId(reviewId),
      target,
      ReviewProviderIdentity(ReviewProviderId("textus-ai"), ReviewVersion("0.2.1")),
      ProviderBundleAvailability.Enabled,
      descriptor,
      _provider_request(reviewId, target),
      startedAtMillis = 0L
    )
  }

  private def _provider_request(reviewId: String, target: ReviewTarget): String =
    Printer.noSpaces.copy(sortKeys = true).print(Json.obj(
      "schemaVersion" -> Json.fromString(TextusAiCarReviewProviderRunner.schemaVersion),
      "documentType" -> Json.fromString("provider-request"),
      "reviewId" -> Json.fromString(reviewId),
      "target" -> Json.fromJsonObject(JsonObject.fromIterable(Vector(
        "kind" -> Json.fromString(target.kind.value),
        "organization" -> Json.fromString(target.organization.get),
        "name" -> Json.fromString(target.name),
        "version" -> Json.fromString(target.version.get.value),
        "digest" -> Json.fromString(target.digest.value)
      ))),
      "requestedCapabilities" -> Json.arr(Json.fromString(TextusAiCarReviewProviderRunner.capabilityId.value)),
      "requestedEvidenceKinds" -> Json.arr(Json.fromString(TextusAiCarReviewProviderRunner.evidenceKind)),
      "rules" -> Json.obj("include" -> Json.arr(Json.fromString("ai.advisory.*")), "exclude" -> Json.arr()),
      "limits" -> Json.obj(
        "maxEvidenceItems" -> Json.fromInt(10),
        "maxObservations" -> Json.fromInt(10),
        "maxInputBytes" -> Json.fromLong(65536L),
        "timeoutMillis" -> Json.fromLong(120000L)
      )
    ))

  private final class RecordingRunner(candidate: Record) extends AiRunner {
    var request: Option[AiRecordRequest] = None
    def generate(req: AiGenerateRequest)(using ExecutionContext): Consequence[AiGenerateResponse] = Consequence.operationInvalid("not-used")
    def chat(req: AiChatRequest)(using ExecutionContext): Consequence[AiChatResponse] = Consequence.operationInvalid("not-used")
    def generateRecord(req: AiRecordRequest)(using ExecutionContext): Consequence[AiRecordResponse] = {
      request = Some(req)
      Consequence.success(AiRecordResponse(candidate, Some("fixture-model"), Map(
        "ai.execution.provider" -> "fixture",
        "ai.execution.mode" -> "deterministic",
        "provider.raw.response" -> "never-retained"
      )))
    }
  }
}
