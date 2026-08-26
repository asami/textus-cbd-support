package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, JsonObject, Printer}

/**
 * Deterministic CAR Review provider over CBD's admitted Component knowledge
 * detail.  It consumes metadata only: it never reads a documented resource,
 * source file, archive entry, or BoK publication.
 */
final case class ComponentKnowledgeCarReviewProviderProfile(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  sourceDigest: String,
  detail: ComponentKnowledgeDetail
)

final class ComponentKnowledgeCarReviewProviderRunner(
  profile: ComponentKnowledgeCarReviewProviderProfile
) extends CarReviewProviderRunner {
  import ComponentKnowledgeCarReviewProviderRunner.*

  @volatile private var _cancelled = Set.empty[ReviewId]

  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
    if (request.provider != profile.provider) _failed("component-knowledge-provider-identity-mismatch", request)
    else if (request.cancellationRequested || _cancelled.contains(request.reviewId)) _failed("component-knowledge-provider-cancelled", request)
    else if (!isAdmittedRequest(request.providerRequest)) _failed("component-knowledge-provider-request-incompatible", request)
    else if (!_valid_digest(profile.sourceDigest)) _failed("component-knowledge-provider-source-digest-invalid", request)
    else ProviderBundleRunnerResult.Completed(_bundle(request, profile), request.startedAtMillis)

  def cancel(request: ProviderBundleExecutionRequest): Unit =
    _cancelled = _cancelled + request.reviewId
}

object ComponentKnowledgeCarReviewProviderRunner {
  val schemaVersion = "textus.cbd.review-provider.v1"
  val evidenceKind = "component-knowledge-metadata"

  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  private enum Outcome(val value: String) {
    case Assurance extends Outcome("assurance")
    case Finding extends Outcome("finding")
    case Unknown extends Outcome("unknown")
  }

  private final case class Result(
    outcome: Outcome,
    message: String,
    paths: Vector[String],
    limitation: Option[String]
  )

  private final case class Rule(
    id: String,
    capability: ReviewCapabilityId,
    focus: String,
    evaluate: ComponentKnowledgeDetail => Result
  )

  private val _rules = Vector(
    Rule("manual-completeness", ReviewCapabilityId("quality.documentation.rationale"), "manual completeness", _manual),
    Rule("resource-integrity", ReviewCapabilityId("quality.security.integrity"), "declared resource integrity", _integrity),
    Rule("scaladoc", ReviewCapabilityId("quality.maintainability.understandability"), "Scaladoc", _scaladoc),
    Rule("source-policy", ReviewCapabilityId("quality.security.authorization"), "source disclosure policy", _source_policy),
    Rule("help-discovery", ReviewCapabilityId("quality.ux.cli"), "Help discovery", _help_discovery),
    Rule("bok-publication-readiness", ReviewCapabilityId("quality.ai-readiness"), "BoK publication readiness", _bok_readiness)
  )

  /**
   * Creates the CAR Review provider input only from an already admitted CBD
   * detail projection.  The detail's artifact checksum remains an identity
   * binding; this conversion neither opens an artifact nor obtains a resolver.
   */
  def fromDetail(
    provider: ReviewProviderIdentity,
    ruleSet: ReviewRuleIdentity,
    detail: ComponentKnowledgeDetail
  ): Option[ComponentKnowledgeCarReviewProviderProfile] =
    Option.when(detail.carrierSha256.matches("[0-9a-f]{64}"))(detail.carrierSha256)
      .map(digest => ComponentKnowledgeCarReviewProviderProfile(provider, ruleSet, "sha256:" + digest, detail))

  def descriptorDocument(profile: ComponentKnowledgeCarReviewProviderProfile): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-descriptor"),
      "provider" -> _identity(profile.provider),
      "ruleSet" -> _rule_identity(profile.ruleSet),
      "supportedSchemaVersions" -> Json.arr(Json.fromString(schemaVersion)),
      "capabilities" -> Json.fromValues(_rules.map(_capability)),
      "limitations" -> Json.arr(Json.obj(
        "code" -> Json.fromString("component-knowledge-content-not-read"),
        "scope" -> Json.fromString("provider"),
        "subjectId" -> Json.fromString(profile.detail.componentId),
        "message" -> Json.fromString("The provider evaluates admitted metadata only; resource content and BoK publication are not read."),
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
      "rules" -> Json.obj("include" -> Json.arr(Json.fromString("cbd.car-review.component-knowledge.*")), "exclude" -> Json.arr()),
      "limits" -> Json.obj("maxEvidenceItems" -> Json.fromInt(12), "maxObservations" -> Json.fromInt(12), "maxInputBytes" -> Json.fromLong(65536L), "timeoutMillis" -> Json.fromLong(1000L))
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

  private def _bundle(request: ProviderBundleExecutionRequest, profile: ComponentKnowledgeCarReviewProviderProfile): String = {
    val entries = _rules.map(rule => _entry(rule, profile, request.target))
    val content = Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("evidence-bundle"),
      "reviewId" -> Json.fromString(request.reviewId.value),
      "target" -> _target(request.target),
      "provider" -> _identity(profile.provider),
      "ruleSet" -> _rule_identity(profile.ruleSet),
      "requestDigest" -> Json.fromString(CarReviewProviderBundleAdmission.requestDigest(request.providerRequest).toOption.get.value),
      "evidence" -> Json.fromValues(entries.map(_.evidence)),
      "observations" -> Json.fromValues(entries.map(_.observation)),
      "limitations" -> Json.fromValues(entries.flatMap(_.limitation))
    )
    _printer.print(content.deepMerge(Json.obj("bundleDigest" -> Json.fromString(_sha256(content)))))
  }

  private final case class Entry(evidence: Json, observation: Json, limitation: Option[Json])

  private def _entry(rule: Rule, profile: ComponentKnowledgeCarReviewProviderProfile, target: ReviewTarget): Entry = {
    val result = rule.evaluate(profile.detail)
    val evidenceid = s"component-knowledge-${rule.id}"
    Entry(
      _evidence(evidenceid, rule, profile, target, result),
      _observation(rule, target, result, Vector(evidenceid)),
      result.limitation.map(_limitation(s"cbd.car-review.component-knowledge.${rule.id}.metadata-insufficient", rule.capability.value, _))
    )
  }

  private def _evidence(
    id: String,
    rule: Rule,
    profile: ComponentKnowledgeCarReviewProviderProfile,
    target: ReviewTarget,
    result: Result
  ): Json =
    Json.obj(
      "id" -> Json.fromString(id),
      "kind" -> Json.fromString(evidenceKind),
      "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
      "origin" -> Json.obj("providerId" -> Json.fromString(profile.provider.id.value), "sourceType" -> Json.fromString("admitted-component-knowledge-metadata")),
      "facts" -> Json.fromJsonObject(JsonObject.fromIterable(Vector(
        "sourceDigest" -> Json.fromString(profile.sourceDigest),
        "checkId" -> Json.fromString(rule.id),
        "capabilityId" -> Json.fromString(rule.capability.value),
        "outcome" -> Json.fromString(result.outcome.value),
        "componentId" -> Json.fromString(profile.detail.componentId),
        "logicalRelease" -> Json.fromString(profile.detail.logicalRelease),
        "logicalPaths" -> Json.fromValues(result.paths.map(Json.fromString)),
        "metadataOnly" -> Json.fromBoolean(true)
      )))
    )

  private def _observation(rule: Rule, target: ReviewTarget, result: Result, evidenceids: Vector[String]): Json = {
    val fields = Vector(
      "id" -> Json.fromString(s"component-knowledge-${rule.id}"),
      "type" -> Json.fromString(result.outcome.value),
      "ruleId" -> Json.fromString(s"cbd.car-review.component-knowledge.${rule.id}"),
      "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
      "message" -> Json.fromString(result.message),
      "confidence" -> Json.fromString(if result.outcome == Outcome.Unknown then "low" else "high"),
      "evidenceIds" -> Json.fromValues(evidenceids.map(Json.fromString)),
      "mappings" -> Json.obj(
        "cncfFeatures" -> Json.arr(),
        "implementationSubjects" -> Json.arr(Json.fromString(s"component:${target.name}")),
        "qualityCapabilities" -> Json.arr(Json.fromString(rule.capability.value))
      )
    ) ++ Option.when(result.outcome == Outcome.Finding)("severity" -> Json.fromString("medium"))
    Json.fromJsonObject(JsonObject.fromIterable(fields))
  }

  private def _manual(detail: ComponentKnowledgeDetail): Result = {
    val paths = _resources(detail, "documentation")
    if (paths.isEmpty) Result(Outcome.Finding, "No admitted documentation resource is declared for the Component.", paths, None)
    else Result(Outcome.Unknown, "Documentation resource metadata is admitted, but manual completeness requires content review which this provider does not perform.", paths, Some("manual-content-not-read"))
  }

  private def _integrity(detail: ComponentKnowledgeDetail): Result = {
    val unverified = detail.resources.filter(_.integrity != "verified").map(_.logicalPath)
    if (detail.resources.isEmpty) Result(Outcome.Finding, "No admitted resources are available for integrity evidence.", Vector.empty, None)
    else if (unverified.isEmpty) Result(Outcome.Assurance, "Every admitted resource declares verified integrity metadata.", detail.resources.map(_.logicalPath), None)
    else Result(Outcome.Finding, "One or more admitted resources lack verified integrity metadata.", unverified, None)
  }

  private def _scaladoc(detail: ComponentKnowledgeDetail): Result = {
    val paths = _resources(detail, "source-code")
    if (paths.isEmpty) Result(Outcome.Finding, "No admitted Scala source resource is declared for Scaladoc inspection.", paths, None)
    else Result(Outcome.Unknown, "Scala source metadata is admitted, but Scaladoc requires source-content inspection which this provider does not perform.", paths, Some("source-content-not-read"))
  }

  private def _source_policy(detail: ComponentKnowledgeDetail): Result = {
    val resources = detail.resources.filter(_.kind == "source-code")
    if (resources.isEmpty) Result(Outcome.Unknown, "No admitted source resource exists from which to evaluate source disclosure policy.", Vector.empty, Some("source-evidence-absent"))
    else {
      val restricted = resources.filter(resource => resource.availability != "available" || resource.authorization != "granted")
      if (restricted.isEmpty) Result(Outcome.Assurance, "Every admitted source resource is available and authorization-granted by declared policy.", resources.map(_.logicalPath), None)
      else Result(Outcome.Finding, "One or more admitted source resources are restricted, unavailable, or not authorization-granted.", restricted.map(_.logicalPath), None)
    }
  }

  private def _help_discovery(detail: ComponentKnowledgeDetail): Result =
    Result(Outcome.Unknown, "The consumer contract does not establish a Component Help route; Help discovery remains unverified.", _resources(detail, "documentation"), Some("help-route-not-derived"))

  private def _bok_readiness(detail: ComponentKnowledgeDetail): Result =
    Result(Outcome.Unknown, "BoK publication is independent from admitted Component knowledge and remains unverified.", Vector.empty, Some("bok-publication-not-inspected"))

  private def _resources(detail: ComponentKnowledgeDetail, kind: String): Vector[String] =
    detail.resources.filter(_.kind == kind).map(_.logicalPath).sorted

  private def _capability(rule: Rule): Json = Json.obj(
    "id" -> Json.fromString(rule.capability.value),
    "version" -> Json.fromString("1.0"),
    "evidenceKinds" -> Json.arr(Json.fromString(evidenceKind)),
    "observationKinds" -> Json.arr(Json.fromString("finding"), Json.fromString("assurance"), Json.fromString("unknown"))
  )

  private def _limitation(code: String, subjectid: String, message: String): Json = Json.obj(
    "code" -> Json.fromString(code), "scope" -> Json.fromString("capability"), "subjectId" -> Json.fromString(subjectid), "message" -> Json.fromString(message), "retryable" -> Json.fromBoolean(true)
  )

  private def _identity(value: ReviewProviderIdentity): Json = Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))
  private def _rule_identity(value: ReviewRuleIdentity): Json = Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))
  private def _target(value: ReviewTarget): Json = Json.fromJsonObject(JsonObject.fromIterable(Vector("kind" -> Json.fromString(value.kind.value), "name" -> Json.fromString(value.name), "digest" -> Json.fromString(value.digest.value)) ++ value.organization.map(x => "organization" -> Json.fromString(x)) ++ value.version.map(x => "version" -> Json.fromString(x.value))))
  private def _sha256(value: Json): String = "sha256:" + MessageDigest.getInstance("SHA-256").digest(_printer.print(value).getBytes(StandardCharsets.UTF_8)).map(byte => f"${byte & 0xff}%02x").mkString
  private def _valid_digest(value: String): Boolean = Option(value).exists(_.matches("sha256:[0-9a-f]{64}"))
  private def _failed(code: String, request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult.Failed = ProviderBundleRunnerResult.Failed(code, "Component knowledge CAR Review provider did not produce an admissible review bundle.", request.startedAtMillis)
}
