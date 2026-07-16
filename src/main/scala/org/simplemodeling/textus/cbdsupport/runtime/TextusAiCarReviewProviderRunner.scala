package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.record.Record

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class TextusAiCarReviewProviderProfile(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  purpose: String,
  evidence: Vector[CarReviewAiEvidence]
)

/**
 * Adapts the provider-neutral Textus AI SPI to CBD's v1 evidence-bundle
 * contract. It accepts only one configured provider identity and purpose.
 * Provider selection, credentials, transport, retries, and model aliases stay
 * behind `AiRunner`; CBD retains only an advisory structured candidate digest
 * and allowlisted execution facts.
 */
final class TextusAiCarReviewProviderRunner(
  profile: TextusAiCarReviewProviderProfile,
  adapter: CarReviewAiRunnerAdapter
)(using context: ExecutionContext) extends CarReviewProviderRunner {
  import TextusAiCarReviewProviderRunner.*

  @volatile private var _cancelled = Set.empty[ReviewId]

  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
    if request.provider != profile.provider then
      _failed("provider-identity-mismatch", request)
    else if request.cancellationRequested || _cancelled.contains(request.reviewId) then
      _failed("provider-cancelled", request)
    else if !isAdmittedRequest(request.providerRequest, profile) then
      _failed("ai-provider-request-incompatible", request)
    else {
      try {
        adapter.review(CarReviewAiReviewRequest(request.reviewId, profile.purpose, profile.evidence)).toOption match {
          case Some(response) if _cancelled.contains(request.reviewId) => _failed("provider-cancelled", request)
          case Some(response) =>
            bundle(request, profile, response) match {
              case Right(value) => ProviderBundleRunnerResult.Completed(value, request.startedAtMillis)
              case Left(code) => _failed(code, request)
            }
          case None => _failed("ai-structured-review-failed", request)
        }
      } catch {
        case NonFatal(_) => _failed("ai-structured-review-failed", request)
      }
    }

  def cancel(request: ProviderBundleExecutionRequest): Unit =
    _cancelled = _cancelled + request.reviewId
}

object TextusAiCarReviewProviderRunner {
  val capabilityId = ReviewCapabilityId("ai.semantic-review")
  val evidenceKind = "ai-structured-review"
  val schemaVersion = "textus.cbd.review-provider.v1"
  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  def descriptorDocument(profile: TextusAiCarReviewProviderProfile): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-descriptor"),
      "provider" -> _identity(profile.provider),
      "ruleSet" -> _rule_identity(profile.ruleSet),
      "supportedSchemaVersions" -> Json.arr(Json.fromString(schemaVersion)),
      "capabilities" -> Json.arr(Json.obj(
        "id" -> Json.fromString(capabilityId.value),
        "version" -> Json.fromString("1.0"),
        "evidenceKinds" -> Json.arr(Json.fromString(evidenceKind)),
        "observationKinds" -> Json.arr(Json.fromString("finding"), Json.fromString("unknown"))
      )),
      "limitations" -> Json.arr(Json.obj(
        "code" -> Json.fromString("ai-advisory-only"),
        "scope" -> Json.fromString("capability"),
        "subjectId" -> Json.fromString(capabilityId.value),
        "message" -> Json.fromString("AI observations are advisory and cannot override deterministic Review findings."),
        "retryable" -> Json.fromBoolean(false)
      ))
    ))

  private[runtime] def isAdmittedRequest(value: String, profile: TextusAiCarReviewProviderProfile): Boolean =
    io.circe.parser.parse(value).toOption.exists { json =>
      val cursor = json.hcursor
      cursor.get[String]("schemaVersion").toOption.contains(schemaVersion) &&
        cursor.get[String]("documentType").toOption.contains("provider-request") &&
        cursor.get[Vector[String]]("requestedCapabilities").toOption.contains(Vector(capabilityId.value)) &&
        cursor.get[Vector[String]]("requestedEvidenceKinds").toOption.contains(Vector(evidenceKind)) &&
        CarReviewProviderBundleAdmission.requestDigest(value).isRight &&
        CarReviewAiRunnerAdapter.purposes.contains(profile.purpose)
    }

  private[runtime] def bundle(
    request: ProviderBundleExecutionRequest,
    profile: TextusAiCarReviewProviderProfile,
    response: CarReviewAiReviewResponse
  ): Either[String, String] =
    for {
      requestdigest <- CarReviewProviderBundleAdmission.requestDigest(request.providerRequest).left.map(identity)
      candidates <- _candidates(response.candidate)
      candidatejson = _printer.print(response.candidate.toJsonString match {
        case value if value.nonEmpty => io.circe.parser.parse(value).getOrElse(Json.obj())
        case _ => Json.obj()
      })
      candidatedigest = _sha256(candidatejson)
      evidenceid = s"ai-candidate-${candidatedigest.stripPrefix("sha256:").take(16)}"
      evidence = Json.obj(
        "id" -> Json.fromString(evidenceid),
        "kind" -> Json.fromString(evidenceKind),
        "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(request.target.name)),
        "origin" -> Json.obj("providerId" -> Json.fromString(profile.provider.id.value), "sourceType" -> Json.fromString("ai")),
        "facts" -> Json.fromJsonObject(_facts(candidatedigest, response))
      )
      observations = candidates.zipWithIndex.map { case (candidate, index) =>
        Json.obj(
          "id" -> Json.fromString(s"ai-candidate-finding-${index + 1}"),
          "type" -> Json.fromString("finding"),
          "ruleId" -> Json.fromString(s"ai.advisory.${candidate.ruleId}"),
          "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(request.target.name)),
          "message" -> Json.fromString(candidate.message),
          "severity" -> Json.fromString(candidate.severity),
          "confidence" -> Json.fromString("low"),
          "evidenceIds" -> Json.arr(Json.fromString(evidenceid))
        )
      }
      content = Json.obj(
        "schemaVersion" -> Json.fromString(schemaVersion),
        "documentType" -> Json.fromString("evidence-bundle"),
        "reviewId" -> Json.fromString(request.reviewId.value),
        "target" -> _target(request.target),
        "provider" -> _identity(profile.provider),
        "ruleSet" -> _rule_identity(profile.ruleSet),
        "requestDigest" -> Json.fromString(requestdigest.value),
        "evidence" -> Json.fromValues(Vector(evidence)),
        "observations" -> Json.fromValues(observations),
        "limitations" -> Json.arr(_advisory_limitation)
      )
      bundledigest = _sha256(_printer.print(content))
      result = content.deepMerge(Json.obj("bundleDigest" -> Json.fromString(bundledigest)))
    } yield _printer.print(result)

  private final case class _Candidate(ruleId: String, severity: String, message: String)

  private def _candidates(record: Record): Either[String, Vector[_Candidate]] =
    record.getAny("findings") match {
      case Some(values: Iterable[?]) =>
        val candidates = values.toVector.collect { case value: Record => _candidate(value) }
        if candidates.size == values.size && candidates.nonEmpty then candidates.foldLeft[Either[String, Vector[_Candidate]]](Right(Vector.empty)) { (z, value) =>
          for { xs <- z; candidate <- value } yield xs :+ candidate
        }
        else Left("ai-structured-output-invalid")
      case _ => Left("ai-structured-output-invalid")
    }

  private def _candidate(value: Record): Either[String, _Candidate] =
    for {
      ruleid <- _string(value, "rule_id").orElse(_string(value, "ruleId")).filter(_identifier).toRight("ai-structured-output-invalid")
      severity <- _string(value, "severity").filter(CarReviewVocabulary.SEVERITIES.contains).toRight("ai-structured-output-invalid")
      message <- _string(value, "message").filter(value => value.nonEmpty && value.length <= 2048).toRight("ai-structured-output-invalid")
    } yield _Candidate(ruleid, severity, message)

  private def _facts(candidateDigest: String, response: CarReviewAiReviewResponse): JsonObject =
    JsonObject.fromMap(
      (Map("candidateDigest" -> Json.fromString(candidateDigest)) ++
        response.model.map(value => "model" -> Json.fromString(value)) ++
        response.executionFacts.map { case (key, value) => key -> Json.fromString(value) })
    )

  private def _identity(value: ReviewProviderIdentity): Json =
    Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))

  private def _rule_identity(value: ReviewRuleIdentity): Json =
    Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))

  private def _target(value: ReviewTarget): Json =
    Json.fromJsonObject(JsonObject.fromIterable(
      Vector(
        "kind" -> Json.fromString(value.kind.value),
        "name" -> Json.fromString(value.name),
        "digest" -> Json.fromString(value.digest.value)
      ) ++ value.organization.map(x => "organization" -> Json.fromString(x)) ++ value.version.map(x => "version" -> Json.fromString(x.value))
    ))

  private def _advisory_limitation: Json =
    Json.obj(
      "code" -> Json.fromString("ai-advisory-only"),
      "scope" -> Json.fromString("capability"),
      "subjectId" -> Json.fromString(capabilityId.value),
      "message" -> Json.fromString("AI candidate findings are advisory and cannot override deterministic Review findings."),
      "retryable" -> Json.fromBoolean(false)
    )

  private def _string(record: Record, key: String): Option[String] =
    record.getAny(key).flatMap {
      case null => None
      case value => Some(value.toString.trim).filter(_.nonEmpty)
    }

  private def _identifier(value: String): Boolean =
    value.matches("^[A-Za-z0-9][A-Za-z0-9._:/-]*$") && value.length <= 160

  private def _sha256(value: String): String = {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }

  private def _failed(code: String, request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult.Failed =
    ProviderBundleRunnerResult.Failed(code, "Textus AI did not produce an admissible structured review bundle.", request.startedAtMillis)
}
