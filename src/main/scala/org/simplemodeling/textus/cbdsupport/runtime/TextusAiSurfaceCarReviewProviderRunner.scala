package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, JsonObject, Printer}

/*
 * @since   Jul. 24, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
/** A bounded, non-secret description of a published MCP or Skill surface. */
final case class TextusAiSurfacePublication(
  id: String,
  version: String,
  contentDigest: String,
  summary: Option[String],
  authorityBoundary: Option[String],
  limitations: Option[String],
  operationIds: Vector[String]
)

/**
 * Evidence gathered by the Textus/CNCF integration boundary.  It intentionally
 * contains no endpoint, credential, invocation payload, or raw Skill content.
 */
final case class TextusAiSurfaceSupportProfile(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  compatibleTextusRuntime: Boolean,
  mcpProjectionEnabled: Boolean,
  standardSkillSet: Option[TextusAiSurfacePublication],
  componentMcp: Option[TextusAiSurfacePublication],
  componentSkill: Option[TextusAiSurfacePublication]
)

/**
 * Deterministic AI View provider for framework-level MCP and standard Skill
 * support.  It does not invoke MCP, install a Skill, or use an AI model.  The
 * caller supplies already admitted compatibility and publication metadata;
 * this runner converts it into the same provider-bundle boundary used by Cozy
 * and other CAR Review providers.
 */
final class TextusAiSurfaceCarReviewProviderRunner(
  profile: TextusAiSurfaceSupportProfile
) extends CarReviewProviderRunner {
  import TextusAiSurfaceCarReviewProviderRunner.*

  @volatile private var _cancelled = Set.empty[ReviewId]

  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
    if request.provider != profile.provider then
      _failed("provider-identity-mismatch", request)
    else if request.cancellationRequested || _cancelled.contains(request.reviewId) then
      _failed("provider-cancelled", request)
    else if !isAdmittedRequest(request.providerRequest, profile) then
      _failed("textus-ai-surface-request-incompatible", request)
    else
      bundle(request, profile) match {
        case Right(value) => ProviderBundleRunnerResult.Completed(value, request.startedAtMillis)
        case Left(code) => _failed(code, request)
      }

  def cancel(request: ProviderBundleExecutionRequest): Unit =
    _cancelled = _cancelled + request.reviewId
}

object TextusAiSurfaceCarReviewProviderRunner {
  val schemaVersion = "textus.cbd.review-provider.v1"
  val mcpCapabilityId = ReviewCapabilityId("quality.ai.operability.mcp")
  val skillCapabilityId = ReviewCapabilityId("quality.ai.operability.skill")
  val mcpCatalogEvidenceKind = "mcp-catalog"
  val mcpDescriptionEvidenceKind = "mcp-description"
  val operationContractEvidenceKind = "operation-contract"
  val skillBundleEvidenceKind = "skill-bundle"
  val skillManifestEvidenceKind = "skill-manifest"
  val skillValidationEvidenceKind = "skill-validation"

  private val _printer = Printer.noSpaces.copy(sortKeys = true)
  private val _capabilities = Vector(mcpCapabilityId, skillCapabilityId)
  private val _evidence_kinds = Vector(
    mcpCatalogEvidenceKind,
    mcpDescriptionEvidenceKind,
    operationContractEvidenceKind,
    skillBundleEvidenceKind,
    skillManifestEvidenceKind,
    skillValidationEvidenceKind
  )

  def descriptorDocument(profile: TextusAiSurfaceSupportProfile): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-descriptor"),
      "provider" -> _identity(profile.provider),
      "ruleSet" -> _rule_identity(profile.ruleSet),
      "supportedSchemaVersions" -> Json.arr(Json.fromString(schemaVersion)),
      "capabilities" -> Json.fromValues(Vector(
        _capability(mcpCapabilityId, Vector(mcpCatalogEvidenceKind, mcpDescriptionEvidenceKind, operationContractEvidenceKind)),
        _capability(skillCapabilityId, Vector(skillBundleEvidenceKind, skillManifestEvidenceKind, skillValidationEvidenceKind))
      )),
      "limitations" -> Json.arr(Json.obj(
        "code" -> Json.fromString("textus-ai-surface-semantic-review-pending"),
        "scope" -> Json.fromString("provider"),
        "subjectId" -> Json.fromString(profile.provider.id.value),
        "message" -> Json.fromString("MCP and Skill semantic usefulness requires admitted advisory or human corroboration."),
        "retryable" -> Json.fromBoolean(false)
      ))
    ))

  def requestDocument(
    reviewId: ReviewId,
    target: ReviewTarget
  ): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-request"),
      "reviewId" -> Json.fromString(reviewId.value),
      "target" -> _target(target),
      "requestedCapabilities" -> Json.fromValues(_capabilities.map(value => Json.fromString(value.value))),
      "requestedEvidenceKinds" -> Json.fromValues(_evidence_kinds.map(Json.fromString)),
      "rules" -> Json.obj(
        "include" -> Json.fromValues(Vector(
          s"cbd.car-review.${mcpCapabilityId.value}",
          s"cbd.car-review.${mcpCapabilityId.value}.content",
          s"cbd.car-review.${skillCapabilityId.value}",
          s"cbd.car-review.${skillCapabilityId.value}.content"
        ).map(Json.fromString)),
        "exclude" -> Json.arr()
      ),
      "limits" -> Json.obj(
        "maxEvidenceItems" -> Json.fromInt(16),
        "maxObservations" -> Json.fromInt(16),
        "maxInputBytes" -> Json.fromLong(65536L),
        "timeoutMillis" -> Json.fromLong(1000L)
      )
    ))

  private[runtime] def isAdmittedRequest(value: String, profile: TextusAiSurfaceSupportProfile): Boolean =
    io.circe.parser.parse(value).toOption.exists { json =>
      val cursor = json.hcursor
      cursor.get[String]("schemaVersion").toOption.contains(schemaVersion) &&
        cursor.get[String]("documentType").toOption.contains("provider-request") &&
        cursor.get[Vector[String]]("requestedCapabilities").toOption.contains(_capabilities.map(_.value)) &&
        cursor.get[Vector[String]]("requestedEvidenceKinds").toOption.contains(_evidence_kinds) &&
        CarReviewProviderBundleAdmission.requestDigest(value).isRight
    }

  private[runtime] def bundle(
    request: ProviderBundleExecutionRequest,
    profile: TextusAiSurfaceSupportProfile
  ): Either[String, String] =
    CarReviewProviderBundleAdmission.requestDigest(request.providerRequest).map { requestdigest =>
      val mcpsupported = profile.compatibleTextusRuntime && profile.mcpProjectionEnabled
      val skillsupported = profile.compatibleTextusRuntime && profile.standardSkillSet.nonEmpty
      val mcp = _surface(
        "mcp", mcpCapabilityId, mcpsupported, profile.componentMcp, profile.provider, request.target,
        Vector(
          "compatibleTextusRuntime" -> Json.fromBoolean(profile.compatibleTextusRuntime),
          "mcpProjectionEnabled" -> Json.fromBoolean(profile.mcpProjectionEnabled)
        )
      )
      val skill = _surface(
        "skill", skillCapabilityId, skillsupported, profile.componentSkill.orElse(profile.standardSkillSet), profile.provider, request.target,
        Vector(
          "compatibleTextusRuntime" -> Json.fromBoolean(profile.compatibleTextusRuntime),
          "standardSkillSetAdmitted" -> Json.fromBoolean(profile.standardSkillSet.nonEmpty)
        )
      )
      val evidence = (mcp.evidence ++ skill.evidence).sortBy(value => value.hcursor.get[String]("id").getOrElse(""))
      val observations = (mcp.observations ++ skill.observations).sortBy(value => value.hcursor.get[String]("id").getOrElse(""))
      val limitations = (mcp.limitations ++ skill.limitations :+ _semantic_limitation(profile)).sortBy(value => value.hcursor.get[String]("code").getOrElse(""))
      val content = Json.obj(
        "schemaVersion" -> Json.fromString(schemaVersion),
        "documentType" -> Json.fromString("evidence-bundle"),
        "reviewId" -> Json.fromString(request.reviewId.value),
        "target" -> _target(request.target),
        "provider" -> _identity(profile.provider),
        "ruleSet" -> _rule_identity(profile.ruleSet),
        "requestDigest" -> Json.fromString(requestdigest.value),
        "evidence" -> Json.fromValues(evidence),
        "observations" -> Json.fromValues(observations),
        "limitations" -> Json.fromValues(limitations)
      )
      _printer.print(content.deepMerge(Json.obj("bundleDigest" -> Json.fromString(_sha256(content)))))
    }

  private final case class _SurfaceBundle(
    evidence: Vector[Json],
    observations: Vector[Json],
    limitations: Vector[Json]
  )

  private def _surface(
    surface: String,
    capability: ReviewCapabilityId,
    supported: Boolean,
    publication: Option[TextusAiSurfacePublication],
    provider: ReviewProviderIdentity,
    target: ReviewTarget,
    supportfacts: Vector[(String, Json)]
  ): _SurfaceBundle = {
    val supportevidenceid = s"$surface-support"
    // A negative compatibility result is still admissible metadata evidence.
    // Keep it so an Unknown is attributable rather than a silent absence.
    val supportevidence = _evidence(
      supportevidenceid,
      if surface == "mcp" then mcpCatalogEvidenceKind else skillBundleEvidenceKind,
      provider,
      target,
      publication,
      surface,
      "framework-support",
      supportfacts
    )
    val supportobservation = _observation(
      s"$surface-support",
      if supported then "assurance" else "unknown",
      s"cbd.car-review.${capability.value}",
      target,
      if supported then s"Textus framework support is admitted for $surface." else s"Textus framework support for $surface is not admitted from supplied compatibility evidence.",
      Vector(supportevidenceid),
      capability,
      None
    )
    val unsupportedlimitation = Option.when(!supported)(_limitation(
      s"cbd.car-review.${capability.value}.evidence-unavailable",
      "capability",
      capability.value,
      s"No compatible Textus support evidence was admitted for $surface.",
      retryable = true
    ))
    val content = publication match {
      case None =>
        _SurfaceBundle(Vector.empty, Vector(_observation(
          s"$surface-content-unknown",
          "unknown",
          s"cbd.car-review.${capability.value}.content",
          target,
          s"No component-published $surface content was supplied for content review.",
          Vector.empty,
          capability,
          None
        )), Vector(_limitation(
          s"cbd.car-review.${capability.value}.content.evidence-unavailable",
          "capability",
          capability.value,
          s"No $surface publication content was admitted; semantic review remains Unknown.",
          retryable = true
        )))
      case Some(value) if !_complete_content(value) =>
        _SurfaceBundle(Vector.empty, Vector(_observation(
          s"$surface-content-incomplete",
          "finding",
          s"cbd.car-review.${capability.value}.content",
          target,
          s"Published $surface content omits required summary, authority, limitation, operation, version, or digest metadata.",
          Vector(supportevidenceid),
          capability,
          Some("medium")
        )), Vector.empty)
      case Some(value) =>
        val descriptionid = s"$surface-description"
        val contractid = s"$surface-contract"
        val evidence = Vector(
          _evidence(descriptionid, if surface == "mcp" then mcpDescriptionEvidenceKind else skillManifestEvidenceKind, provider, target, Some(value), surface, "content-description", Vector.empty),
          _evidence(contractid, if surface == "mcp" then operationContractEvidenceKind else skillValidationEvidenceKind, provider, target, Some(value), surface, "content-contract", Vector.empty)
        )
        _SurfaceBundle(evidence, Vector(_observation(
          s"$surface-content-unknown",
          "unknown",
          s"cbd.car-review.${capability.value}.content",
          target,
          s"$surface metadata is structurally complete; semantic usefulness and safety still require advisory or human review.",
          Vector(descriptionid, contractid),
          capability,
          None
        )), Vector.empty)
    }
    _SurfaceBundle(Vector(supportevidence) ++ content.evidence, Vector(supportobservation) ++ content.observations, unsupportedlimitation.toVector ++ content.limitations)
  }

  private def _complete_content(value: TextusAiSurfacePublication): Boolean =
    _identifier(value.id) && _nonempty(value.version) && _digest(value.contentDigest) &&
      value.summary.exists(_nonempty) && value.authorityBoundary.exists(_nonempty) &&
      value.limitations.exists(_nonempty) && value.operationIds.nonEmpty &&
      value.operationIds.distinct.size == value.operationIds.size && value.operationIds.forall(_identifier)

  private def _capability(id: ReviewCapabilityId, evidence: Vector[String]): Json =
    Json.obj(
      "id" -> Json.fromString(id.value),
      "version" -> Json.fromString("1.0"),
      "evidenceKinds" -> Json.fromValues(evidence.map(Json.fromString)),
      "observationKinds" -> Json.arr(Json.fromString("finding"), Json.fromString("assurance"), Json.fromString("unknown"))
    )

  private def _evidence(
    id: String,
    kind: String,
    provider: ReviewProviderIdentity,
    target: ReviewTarget,
    publication: Option[TextusAiSurfacePublication],
    surface: String,
    role: String,
    extrafacts: Vector[(String, Json)]
  ): Json =
    Json.obj(
      "id" -> Json.fromString(id),
      "kind" -> Json.fromString(kind),
      "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
      "origin" -> Json.obj("providerId" -> Json.fromString(provider.id.value), "sourceType" -> Json.fromString("textus-metadata")),
      "facts" -> Json.fromJsonObject(JsonObject.fromIterable(
        Vector("surface" -> Json.fromString(surface), "role" -> Json.fromString(role)) ++ extrafacts ++
          publication.toVector.flatMap(value => Vector(
            "publicationId" -> Json.fromString(value.id),
            "publicationVersion" -> Json.fromString(value.version),
            "contentDigest" -> Json.fromString(value.contentDigest)
          ))
      ))
    )

  private def _observation(
    id: String,
    observationtype: String,
    ruleid: String,
    target: ReviewTarget,
    message: String,
    evidenceids: Vector[String],
    capability: ReviewCapabilityId,
    severity: Option[String]
  ): Json =
    Json.fromJsonObject(JsonObject.fromIterable(
      Vector(
        "id" -> Json.fromString(id),
        "type" -> Json.fromString(observationtype),
        "ruleId" -> Json.fromString(ruleid),
        "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
        "message" -> Json.fromString(message),
        "confidence" -> Json.fromString(if observationtype == "unknown" then "low" else "high"),
        "evidenceIds" -> Json.fromValues(evidenceids.map(Json.fromString)),
        "mappings" -> Json.obj(
          "cncfFeatures" -> Json.arr(),
          "implementationSubjects" -> Json.arr(Json.fromString(s"component:${target.name}")),
          "qualityCapabilities" -> Json.arr(Json.fromString(capability.value))
        )
      ) ++ severity.map(value => "severity" -> Json.fromString(value))
    ))

  private def _semantic_limitation(profile: TextusAiSurfaceSupportProfile): Json =
    _limitation(
      "textus-ai-surface-semantic-review-pending",
      "provider",
      profile.provider.id.value,
      "MCP and Skill semantic usefulness requires admitted advisory or human corroboration.",
      retryable = false
    )

  private def _limitation(code: String, scope: String, subjectid: String, message: String, retryable: Boolean): Json =
    Json.obj(
      "code" -> Json.fromString(code),
      "scope" -> Json.fromString(scope),
      "subjectId" -> Json.fromString(subjectid),
      "message" -> Json.fromString(message),
      "retryable" -> Json.fromBoolean(retryable)
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

  private def _sha256(value: Json): String = {
    val digest = MessageDigest.getInstance("SHA-256").digest(_printer.print(value).getBytes(StandardCharsets.UTF_8))
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }

  private def _identifier(value: String): Boolean = value.matches("^[A-Za-z0-9][A-Za-z0-9._:/-]*$") && value.length <= 160
  private def _nonempty(value: String): Boolean = Option(value).exists(_.trim.nonEmpty)
  private def _digest(value: String): Boolean = Option(value).exists(_.matches("sha256:[0-9a-f]{64}"))

  private def _failed(code: String, request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult.Failed =
    ProviderBundleRunnerResult.Failed(code, "Textus AI surface provider did not produce an admissible review bundle.", request.startedAtMillis)
}
