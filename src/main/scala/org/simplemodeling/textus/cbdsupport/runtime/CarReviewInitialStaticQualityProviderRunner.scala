package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, JsonObject, Printer}

/*
 * @since   Jul. 24, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * Bounded result set emitted by a static analyzer such as Cozy. `None` means
 * the analyzer supplied no admissible fact; it never means a passing check.
 */
final case class CarReviewInitialStaticQualityEvidence(
  sourceDigest: String,
  authorizationPolicy: Option[Boolean],
  boundedTextDatatypes: Option[Boolean],
  domainIdentityConsistency: Option[Boolean],
  documentationRationale: Option[Boolean],
  resilienceContract: Option[Boolean],
  executableTestContract: Option[Boolean],
  evaluationCorpus: Option[Boolean],
  structuredLoggingSchema: Option[Boolean],
  webTaskContract: Option[Boolean],
  cliTaskContract: Option[Boolean],
  skillTaskContract: Option[Boolean],
  crossSurfaceContract: Option[Boolean]
)

final case class CarReviewInitialStaticQualityProviderProfile(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  evidence: CarReviewInitialStaticQualityEvidence
)

/**
 * Fixed initial deterministic CAR-quality provider. It is deliberately not a
 * generic assertion adapter: each accepted field is bound to one CBD rule,
 * Evidence kind, and catalog capability before a provider bundle is made.
 */
final class CarReviewInitialStaticQualityProviderRunner(
  profile: CarReviewInitialStaticQualityProviderProfile
) extends CarReviewProviderRunner {
  import CarReviewInitialStaticQualityProviderRunner.*

  @volatile private var _cancelled = Set.empty[ReviewId]

  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
    if request.provider != profile.provider then _failed("provider-identity-mismatch", request)
    else if request.cancellationRequested || _cancelled.contains(request.reviewId) then _failed("provider-cancelled", request)
    else if !isAdmittedRequest(request.providerRequest) then _failed("initial-static-quality-request-incompatible", request)
    else if !_valid_digest(profile.evidence.sourceDigest) then _failed("initial-static-quality-source-digest-invalid", request)
    else ProviderBundleRunnerResult.Completed(_bundle(request, profile), request.startedAtMillis)

  def cancel(request: ProviderBundleExecutionRequest): Unit =
    _cancelled = _cancelled + request.reviewId
}

object CarReviewInitialStaticQualityProviderRunner {
  val schemaVersion = "textus.cbd.review-provider.v1"
  val evidenceKind = "static-quality-check"

  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  private final case class _Rule(
    id: String,
    capability: ReviewCapabilityId,
    focus: String,
    outcome: CarReviewInitialStaticQualityEvidence => Option[Boolean]
  )

  private val _rules = Vector(
    _Rule("security-authorization", ReviewCapabilityId("quality.security.authorization"), "authorization policy", _.authorizationPolicy),
    _Rule("security-bounded-text", ReviewCapabilityId("quality.security.domain.bounded-text-datatype"), "bounded text datatype", _.boundedTextDatatypes),
    _Rule("domain-identity", ReviewCapabilityId("quality.domain.identity-consistency"), "domain identity consistency", _.domainIdentityConsistency),
    _Rule("documentation-rationale", ReviewCapabilityId("quality.documentation.rationale"), "documentation rationale", _.documentationRationale),
    _Rule("resilience-contract", ReviewCapabilityId("quality.resilience"), "resilience and failure contract", _.resilienceContract),
    _Rule("testability-contract", ReviewCapabilityId("quality.testability"), "executable test contract", _.executableTestContract),
    _Rule("evaluability-corpus", ReviewCapabilityId("quality.evaluability.corpus-first-experiment"), "evaluation corpus", _.evaluationCorpus),
    _Rule("observability-logging-schema", ReviewCapabilityId("quality.observability.structured-logging"), "structured logging schema", _.structuredLoggingSchema),
    _Rule("ux-web", ReviewCapabilityId("quality.ux.web"), "Web task contract", _.webTaskContract),
    _Rule("ux-cli", ReviewCapabilityId("quality.ux.cli"), "CLI task contract", _.cliTaskContract),
    _Rule("ux-skill", ReviewCapabilityId("quality.ux.skill-assisted"), "Skill task contract", _.skillTaskContract),
    _Rule("ux-cross-surface", ReviewCapabilityId("quality.ux.cross-surface-consistency"), "cross-surface contract", _.crossSurfaceContract)
  )

  def descriptorDocument(profile: CarReviewInitialStaticQualityProviderProfile): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-descriptor"),
      "provider" -> _identity(profile.provider),
      "ruleSet" -> _rule_identity(profile.ruleSet),
      "supportedSchemaVersions" -> Json.arr(Json.fromString(schemaVersion)),
      "capabilities" -> Json.fromValues(_rules.map(_capability)),
      "limitations" -> Json.arr(Json.obj(
        "code" -> Json.fromString("initial-static-quality-evidence-unavailable"),
        "scope" -> Json.fromString("provider"),
        "subjectId" -> Json.fromString(profile.provider.id.value),
        "message" -> Json.fromString("Missing static analyzer facts remain Unknown and do not imply a passing quality check."),
        "retryable" -> Json.fromBoolean(true)
      ))
    ))

  def requestDocument(reviewId: ReviewId, target: ReviewTarget): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-request"),
      "reviewId" -> Json.fromString(reviewId.value),
      "target" -> _target(target),
      "requestedCapabilities" -> Json.fromValues(_rules.map(rule => Json.fromString(rule.capability.value))),
      "requestedEvidenceKinds" -> Json.arr(Json.fromString(evidenceKind)),
      "rules" -> Json.obj("include" -> Json.arr(Json.fromString("cbd.car-review.initial-static.*")), "exclude" -> Json.arr()),
      "limits" -> Json.obj("maxEvidenceItems" -> Json.fromInt(64), "maxObservations" -> Json.fromInt(64), "maxInputBytes" -> Json.fromLong(65536L), "timeoutMillis" -> Json.fromLong(1000L))
    ))

  private[runtime] def isAdmittedRequest(value: String): Boolean =
    io.circe.parser.parse(value).toOption.exists { json =>
      val cursor = json.hcursor
      cursor.get[String]("schemaVersion").toOption.contains(schemaVersion) &&
        cursor.get[String]("documentType").toOption.contains("provider-request") &&
        cursor.get[Vector[String]]("requestedCapabilities").toOption.contains(_rules.map(_.capability.value)) &&
        cursor.get[Vector[String]]("requestedEvidenceKinds").toOption.contains(Vector(evidenceKind)) &&
        CarReviewProviderBundleAdmission.requestDigest(value).isRight
    }

  private def _bundle(request: ProviderBundleExecutionRequest, profile: CarReviewInitialStaticQualityProviderProfile): String = {
    val entries = _rules.map(rule => _entry(rule, profile.evidence, profile.provider, request.target))
    val content = Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("evidence-bundle"),
      "reviewId" -> Json.fromString(request.reviewId.value),
      "target" -> _target(request.target),
      "provider" -> _identity(profile.provider),
      "ruleSet" -> _rule_identity(profile.ruleSet),
      "requestDigest" -> Json.fromString(CarReviewProviderBundleAdmission.requestDigest(request.providerRequest).toOption.get.value),
      "evidence" -> Json.fromValues(entries.flatMap(_.evidence)),
      "observations" -> Json.fromValues(entries.map(_.observation)),
      "limitations" -> Json.fromValues(entries.flatMap(_.limitations))
    )
    _printer.print(content.deepMerge(Json.obj("bundleDigest" -> Json.fromString(_sha256(content)))))
  }

  private final case class _Entry(evidence: Option[Json], observation: Json, limitations: Vector[Json])

  private def _entry(rule: _Rule, evidence: CarReviewInitialStaticQualityEvidence, provider: ReviewProviderIdentity, target: ReviewTarget): _Entry =
    rule.outcome(evidence) match {
      case Some(passed) =>
        val evidenceid = s"initial-static-${rule.id}"
        _Entry(
          Some(_evidence(evidenceid, rule, evidence, provider, target, passed)),
          _observation(rule, target, if passed then "assurance" else "finding", if passed then s"Static ${rule.focus} check passed." else s"Static ${rule.focus} check failed.", Vector(evidenceid), if passed then None else Some("medium")),
          Vector.empty
        )
      case None =>
        _Entry(
          None,
          _observation(rule, target, "unknown", s"No admitted static evidence is available for ${rule.focus}.", Vector.empty, None),
          Vector(_limitation(s"cbd.car-review.initial-static.${rule.id}.evidence-unavailable", "capability", rule.capability.value, s"Static ${rule.focus} evidence is unavailable.", true))
        )
    }

  private def _evidence(id: String, rule: _Rule, source: CarReviewInitialStaticQualityEvidence, provider: ReviewProviderIdentity, target: ReviewTarget, passed: Boolean): Json =
    Json.obj(
      "id" -> Json.fromString(id), "kind" -> Json.fromString(evidenceKind),
      "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
      "origin" -> Json.obj("providerId" -> Json.fromString(provider.id.value), "sourceType" -> Json.fromString("static-analysis")),
      "facts" -> Json.fromJsonObject(JsonObject.fromIterable(Vector(
        "sourceDigest" -> Json.fromString(source.sourceDigest), "checkId" -> Json.fromString(rule.id),
        "capabilityId" -> Json.fromString(rule.capability.value), "passed" -> Json.fromBoolean(passed)
      )))
    )

  private def _observation(rule: _Rule, target: ReviewTarget, outcome: String, message: String, evidenceids: Vector[String], severity: Option[String]): Json = {
    val fields = Vector(
      "id" -> Json.fromString(s"initial-static-${rule.id}"),
      "type" -> Json.fromString(outcome),
      "ruleId" -> Json.fromString(s"cbd.car-review.initial-static.${rule.id}"),
      "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
      "message" -> Json.fromString(message),
      "confidence" -> Json.fromString(if outcome == "unknown" then "low" else "high"),
      "evidenceIds" -> Json.fromValues(evidenceids.map(Json.fromString)),
      "mappings" -> Json.obj(
        "cncfFeatures" -> Json.arr(),
        "implementationSubjects" -> Json.arr(Json.fromString(s"component:${target.name}")),
        "qualityCapabilities" -> Json.arr(Json.fromString(rule.capability.value))
      )
    ) ++ severity.map(value => "severity" -> Json.fromString(value))
    Json.fromJsonObject(JsonObject.fromIterable(fields))
  }

  private def _capability(rule: _Rule): Json = Json.obj(
    "id" -> Json.fromString(rule.capability.value), "version" -> Json.fromString("1.0"),
    "evidenceKinds" -> Json.arr(Json.fromString(evidenceKind)),
    "observationKinds" -> Json.arr(Json.fromString("finding"), Json.fromString("assurance"), Json.fromString("unknown"))
  )
  private def _limitation(code: String, scope: String, subjectid: String, message: String, retryable: Boolean): Json = Json.obj(
    "code" -> Json.fromString(code), "scope" -> Json.fromString(scope), "subjectId" -> Json.fromString(subjectid), "message" -> Json.fromString(message), "retryable" -> Json.fromBoolean(retryable)
  )
  private def _identity(value: ReviewProviderIdentity): Json = Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))
  private def _rule_identity(value: ReviewRuleIdentity): Json = Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))
  private def _target(value: ReviewTarget): Json = Json.fromJsonObject(JsonObject.fromIterable(Vector("kind" -> Json.fromString(value.kind.value), "name" -> Json.fromString(value.name), "digest" -> Json.fromString(value.digest.value)) ++ value.organization.map(x => "organization" -> Json.fromString(x)) ++ value.version.map(x => "version" -> Json.fromString(x.value))))
  private def _sha256(value: Json): String = {
    val digest = MessageDigest.getInstance("SHA-256").digest(_printer.print(value).getBytes(StandardCharsets.UTF_8))
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }
  private def _valid_digest(value: String): Boolean = Option(value).exists(_.matches("sha256:[0-9a-f]{64}"))
  private def _failed(code: String, request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult.Failed = ProviderBundleRunnerResult.Failed(code, "Initial static quality provider did not produce an admissible review bundle.", request.startedAtMillis)
}
