package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, JsonObject, Printer}

/*
 * @since   Jul. 24, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
enum CarReviewCostScenarioKind(val value: String) {
  case StaticWebApp extends CarReviewCostScenarioKind("static-web-app")
  case GemmaMcp extends CarReviewCostScenarioKind("gemma-mcp")
}

final case class CarReviewCostReduction(value: BigDecimal, unit: String)

/**
 * A bounded cost optimization declaration supplied by an admitted evidence
 * provider.  A measured reduction has no meaning unless comparison period and
 * normalized unit are supplied alongside it.
 */
final case class CarReviewCostScenario(
  id: String,
  kind: CarReviewCostScenarioKind,
  currentArchitecture: String,
  costDriver: String,
  optimization: String,
  expectedReduction: Option[CarReviewCostReduction],
  measuredReduction: Option[CarReviewCostReduction],
  comparisonPeriod: Option[String],
  normalizedUnit: Option[String],
  qualityConstraints: Vector[String],
  operationalTradeoffs: Vector[String],
  confidence: ReviewConfidence
)

final case class CarReviewCostScenarioProviderProfile(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  scenarios: Vector[CarReviewCostScenario]
)

/**
 * Deterministic provider for the initial Cost View scenarios. It receives
 * bounded metrics already gathered by a deployment, runtime, AI, or billing
 * integration and never fetches price data, runs a model, or infers savings.
 */
final class CarReviewCostScenarioProviderRunner(
  profile: CarReviewCostScenarioProviderProfile
) extends CarReviewProviderRunner {
  import CarReviewCostScenarioProviderRunner.*

  @volatile private var _cancelled = Set.empty[ReviewId]

  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
    if request.provider != profile.provider then _failed("provider-identity-mismatch", request)
    else if request.cancellationRequested || _cancelled.contains(request.reviewId) then _failed("provider-cancelled", request)
    else if !isAdmittedRequest(request.providerRequest) then _failed("cost-scenario-request-incompatible", request)
    else bundle(request, profile) match {
      case Right(value) => ProviderBundleRunnerResult.Completed(value, request.startedAtMillis)
      case Left(code) => _failed(code, request)
    }

  def cancel(request: ProviderBundleExecutionRequest): Unit =
    _cancelled = _cancelled + request.reviewId
}

object CarReviewCostScenarioProviderRunner {
  val schemaVersion = "textus.cbd.review-provider.v1"
  val infrastructureCapability = ReviewCapabilityId("quality.cost-efficiency.infrastructure")
  val operationsCapability = ReviewCapabilityId("quality.cost-efficiency.operations")
  val developmentCapability = ReviewCapabilityId("quality.cost-efficiency.development")
  val resourceCapability = ReviewCapabilityId("quality.performance.resource-efficiency")
  val workAvoidanceCapability = ReviewCapabilityId("quality.sustainability.work-avoidance")
  val optimizationEvidenceKind = "cost-optimization"
  val costRecordEvidenceKind = "cost-record"
  val workloadEvidenceKind = "workload-analysis"

  private val _printer = Printer.noSpaces.copy(sortKeys = true)
  private val _capabilities = Vector(infrastructureCapability, operationsCapability, developmentCapability, resourceCapability, workAvoidanceCapability)
  private val _evidence_kinds = Vector(optimizationEvidenceKind, costRecordEvidenceKind, workloadEvidenceKind)

  def descriptorDocument(profile: CarReviewCostScenarioProviderProfile): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-descriptor"),
      "provider" -> _identity(profile.provider),
      "ruleSet" -> _rule_identity(profile.ruleSet),
      "supportedSchemaVersions" -> Json.arr(Json.fromString(schemaVersion)),
      "capabilities" -> Json.fromValues(_capabilities.map(_capability)),
      "limitations" -> Json.arr(Json.obj(
        "code" -> Json.fromString("cost-measurement-not-automatic"),
        "scope" -> Json.fromString("provider"),
        "subjectId" -> Json.fromString(profile.provider.id.value),
        "message" -> Json.fromString("Cost reductions remain Unknown without normalized measured evidence and comparison context."),
        "retryable" -> Json.fromBoolean(true)
      ))
    ))

  def requestDocument(reviewId: ReviewId, target: ReviewTarget): String =
    _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(schemaVersion),
      "documentType" -> Json.fromString("provider-request"),
      "reviewId" -> Json.fromString(reviewId.value),
      "target" -> _target(target),
      "requestedCapabilities" -> Json.fromValues(_capabilities.map(value => Json.fromString(value.value))),
      "requestedEvidenceKinds" -> Json.fromValues(_evidence_kinds.map(Json.fromString)),
      "rules" -> Json.obj(
        "include" -> Json.arr(Json.fromString("cbd.car-review.cost.static-web-app"), Json.fromString("cbd.car-review.cost.gemma-mcp")),
        "exclude" -> Json.arr()
      ),
      "limits" -> Json.obj(
        "maxEvidenceItems" -> Json.fromInt(64),
        "maxObservations" -> Json.fromInt(64),
        "maxInputBytes" -> Json.fromLong(65536L),
        "timeoutMillis" -> Json.fromLong(1000L)
      )
    ))

  private[runtime] def isAdmittedRequest(value: String): Boolean =
    io.circe.parser.parse(value).toOption.exists { json =>
      val cursor = json.hcursor
      cursor.get[String]("schemaVersion").toOption.contains(schemaVersion) &&
        cursor.get[String]("documentType").toOption.contains("provider-request") &&
        cursor.get[Vector[String]]("requestedCapabilities").toOption.contains(_capabilities.map(_.value)) &&
        cursor.get[Vector[String]]("requestedEvidenceKinds").toOption.contains(_evidence_kinds) &&
        CarReviewProviderBundleAdmission.requestDigest(value).isRight
    }

  private[runtime] def bundle(request: ProviderBundleExecutionRequest, profile: CarReviewCostScenarioProviderProfile): Either[String, String] =
    CarReviewProviderBundleAdmission.requestDigest(request.providerRequest).map { requestdigest =>
      val entries = profile.scenarios.sortBy(_.id).map(value => _entry(value, profile.provider, request.target))
      val content = Json.obj(
        "schemaVersion" -> Json.fromString(schemaVersion),
        "documentType" -> Json.fromString("evidence-bundle"),
        "reviewId" -> Json.fromString(request.reviewId.value),
        "target" -> _target(request.target),
        "provider" -> _identity(profile.provider),
        "ruleSet" -> _rule_identity(profile.ruleSet),
        "requestDigest" -> Json.fromString(requestdigest.value),
        "evidence" -> Json.fromValues(entries.map(_.evidence)),
        "observations" -> Json.fromValues(entries.map(_.observation)),
        "limitations" -> Json.fromValues(entries.flatMap(_.limitations))
      )
      _printer.print(content.deepMerge(Json.obj("bundleDigest" -> Json.fromString(_sha256(content)))))
    }

  private final case class _Entry(evidence: Json, observation: Json, limitations: Vector[Json])

  private def _entry(scenario: CarReviewCostScenario, provider: ReviewProviderIdentity, target: ReviewTarget): _Entry = {
    val evidenceid = s"cost-${scenario.id}"
    val validstructure = _valid_structure(scenario)
    val measurementcontext = scenario.measuredReduction.isEmpty || (scenario.comparisonPeriod.exists(_nonempty) && scenario.normalizedUnit.exists(_nonempty))
    val state =
      if !validstructure then "finding"
      else if !measurementcontext then "finding"
      else if scenario.measuredReduction.nonEmpty then "assurance"
      else "unknown"
    val message = state match {
      case "assurance" => s"${scenario.kind.value} has normalized, period-bound measured cost evidence."
      case "unknown" => s"${scenario.kind.value} has an optimization declaration but no normalized measured reduction."
      case _ if !validstructure => s"${scenario.kind.value} cost declaration omits bounded architecture, driver, optimization, quality, trade-off, reduction, or confidence data."
      case _ => s"${scenario.kind.value} claims a measured reduction without comparison period and normalized unit."
    }
    val limitations = Option.when(state == "unknown")(_limitation(
      "cost-measurement-unavailable", "capability", scenario.id,
      "Expected savings are retained separately; measured savings require comparison period and normalized unit.", true
    )).toVector
    _Entry(
      _evidence(evidenceid, scenario, provider, target),
      _observation(scenario, target, evidenceid, state, message),
      limitations
    )
  }

  private def _valid_structure(value: CarReviewCostScenario): Boolean =
    _identifier(value.id) && _nonempty(value.currentArchitecture) && _nonempty(value.costDriver) && _nonempty(value.optimization) &&
      value.qualityConstraints.nonEmpty && value.qualityConstraints.forall(_nonempty) &&
      value.operationalTradeoffs.nonEmpty && value.operationalTradeoffs.forall(_nonempty) &&
      CarReviewVocabulary.CONFIDENCES.contains(value.confidence.value) &&
      value.expectedReduction.forall(_valid_reduction) && value.measuredReduction.forall(_valid_reduction)

  private def _valid_reduction(value: CarReviewCostReduction): Boolean = value.value >= 0 && _nonempty(value.unit)

  private def _evidence(id: String, scenario: CarReviewCostScenario, provider: ReviewProviderIdentity, target: ReviewTarget): Json = {
    val kind = if scenario.measuredReduction.nonEmpty then costRecordEvidenceKind else optimizationEvidenceKind
    Json.obj(
      "id" -> Json.fromString(id),
      "kind" -> Json.fromString(kind),
      "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
      "origin" -> Json.obj("providerId" -> Json.fromString(provider.id.value), "sourceType" -> Json.fromString("cost-metadata")),
      "facts" -> Json.obj("costOptimization" -> _scenario_facts(scenario))
    )
  }

  private def _scenario_facts(value: CarReviewCostScenario): Json =
    Json.fromJsonObject(JsonObject.fromIterable(
      Vector(
        "id" -> Json.fromString(value.id),
        "kind" -> Json.fromString(value.kind.value),
        "currentArchitecture" -> Json.fromString(value.currentArchitecture),
        "costDriver" -> Json.fromString(value.costDriver),
        "optimization" -> Json.fromString(value.optimization),
        "qualityConstraints" -> Json.fromValues(value.qualityConstraints.map(Json.fromString)),
        "operationalTradeoffs" -> Json.fromValues(value.operationalTradeoffs.map(Json.fromString)),
        "confidence" -> Json.fromString(value.confidence.value)
      ) ++ value.expectedReduction.map(x => "expectedReduction" -> _reduction(x)) ++
        value.measuredReduction.map(x => "measuredReduction" -> _reduction(x)) ++
        value.comparisonPeriod.map(x => "comparisonPeriod" -> Json.fromString(x)) ++
        value.normalizedUnit.map(x => "normalizedUnit" -> Json.fromString(x))
    ))

  private def _reduction(value: CarReviewCostReduction): Json =
    Json.obj("value" -> Json.fromBigDecimal(value.value), "unit" -> Json.fromString(value.unit))

  private def _observation(scenario: CarReviewCostScenario, target: ReviewTarget, evidenceid: String, state: String, message: String): Json =
    Json.fromJsonObject(JsonObject.fromIterable(
      Vector(
        "id" -> Json.fromString(s"cost-${scenario.id}"),
        "type" -> Json.fromString(state),
        "ruleId" -> Json.fromString(s"cbd.car-review.cost.${scenario.kind.value}"),
        "subject" -> Json.obj("kind" -> Json.fromString("review-target"), "id" -> Json.fromString(target.name)),
        "message" -> Json.fromString(message),
        "confidence" -> Json.fromString(scenario.confidence.value),
        "evidenceIds" -> Json.arr(Json.fromString(evidenceid)),
        "mappings" -> Json.obj(
          "cncfFeatures" -> Json.arr(),
          "implementationSubjects" -> Json.arr(Json.fromString(s"component:${target.name}")),
          "qualityCapabilities" -> Json.fromValues(_capabilities_for(scenario.kind).map(value => Json.fromString(value.value)))
        )
      ) ++ Option.when(state == "finding")("severity" -> Json.fromString("medium"))
    ))

  private def _capabilities_for(kind: CarReviewCostScenarioKind): Vector[ReviewCapabilityId] = kind match {
    case CarReviewCostScenarioKind.StaticWebApp => Vector(infrastructureCapability, operationsCapability, resourceCapability, workAvoidanceCapability)
    case CarReviewCostScenarioKind.GemmaMcp => Vector(operationsCapability, developmentCapability, resourceCapability, workAvoidanceCapability)
  }

  private def _capability(id: ReviewCapabilityId): Json = Json.obj(
    "id" -> Json.fromString(id.value),
    "version" -> Json.fromString("1.0"),
    "evidenceKinds" -> Json.fromValues(_evidence_kinds.map(Json.fromString)),
    "observationKinds" -> Json.arr(Json.fromString("finding"), Json.fromString("assurance"), Json.fromString("unknown"))
  )

  private def _limitation(code: String, scope: String, subjectid: String, message: String, retryable: Boolean): Json = Json.obj(
    "code" -> Json.fromString(code), "scope" -> Json.fromString(scope), "subjectId" -> Json.fromString(subjectid),
    "message" -> Json.fromString(message), "retryable" -> Json.fromBoolean(retryable)
  )

  private def _identity(value: ReviewProviderIdentity): Json = Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))
  private def _rule_identity(value: ReviewRuleIdentity): Json = Json.obj("id" -> Json.fromString(value.id.value), "version" -> Json.fromString(value.version.value))
  private def _target(value: ReviewTarget): Json = Json.fromJsonObject(JsonObject.fromIterable(
    Vector("kind" -> Json.fromString(value.kind.value), "name" -> Json.fromString(value.name), "digest" -> Json.fromString(value.digest.value)) ++
      value.organization.map(x => "organization" -> Json.fromString(x)) ++ value.version.map(x => "version" -> Json.fromString(x.value))
  ))
  private def _sha256(value: Json): String = {
    val digest = MessageDigest.getInstance("SHA-256").digest(_printer.print(value).getBytes(StandardCharsets.UTF_8))
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }
  private def _identifier(value: String): Boolean = Option(value).exists(_.matches("^[A-Za-z0-9][A-Za-z0-9._:/-]*$"))
  private def _nonempty(value: String): Boolean = Option(value).exists(_.trim.nonEmpty)
  private def _failed(code: String, request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult.Failed =
    ProviderBundleRunnerResult.Failed(code, "Cost scenario provider did not produce an admissible review bundle.", request.startedAtMillis)
}
