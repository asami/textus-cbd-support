package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, JsonObject, Printer}
import org.goldenport.Consequence

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewRegisteredProviderSelection(
  descriptorDocument: String,
  request: ProviderBundleExecutionRequest
)

final case class CarReviewRegisteredReviewExecution(
  plan: CarReviewExecutionPlan,
  template: CarReviewReport,
  providers: Vector[CarReviewRegisteredProviderSelection]
)

/**
 * Binds the selectable provider's actual availability and registered quality
 * policy to the provider-selection digest frozen in a diagnosis plan.
 */
object CarReviewProviderSelectionPolicy {
  val DEFINITION_ID = "textus.cbd.review-provider-selection-policy.v2"
  val SCHEMA_VERSION = "textus.cbd.review-provider-selection-policy.v2"
  val REUSE_DEFINITION_ID = "textus.cbd.review-provider-selection-reuse-policy.v1"
  val REUSE_SCHEMA_VERSION = "textus.cbd.review-provider-selection-reuse-policy.v1"

  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  def digest(
    descriptorDigest: ReviewDigest,
    requestDigest: ReviewDigest,
    availability: ProviderBundleAvailability,
    qualityPolicy: CarReviewQualityProviderPolicy
  ): ReviewDigest =
    ReviewDigest(_sha256(_printer.print(_document(descriptorDigest, requestDigest, availability, qualityPolicy))))

  /**
   * Freezes conclusion-affecting request semantics for a reusable diagnosis
   * without including the per-Run Review ID needed by the executed provider
   * request. The original document remains the sole execution input.
   */
  def reuseDigest(
    descriptorDigest: ReviewDigest,
    providerRequestDocument: String,
    availability: ProviderBundleAvailability,
    qualityPolicy: CarReviewQualityProviderPolicy
  ): Either[String, ReviewDigest] =
    for {
      _ <- CarReviewProviderBundleAdmission.requestDigest(providerRequestDocument)
        .left.map(_ => "registered-provider-reuse-request-invalid")
      request <- _reuse_request(providerRequestDocument)
    } yield ReviewDigest(_sha256(_printer.print(
      _reuse_document(descriptorDigest, request, availability, qualityPolicy)
    )))

  private def _document(
    descriptordigest: ReviewDigest,
    requestdigest: ReviewDigest,
    availability: ProviderBundleAvailability,
    qualitypolicy: CarReviewQualityProviderPolicy
  ): Json =
    Json.obj(
      "schemaVersion" -> Json.fromString(SCHEMA_VERSION),
      "definitionId" -> Json.fromString(DEFINITION_ID),
      "descriptorDigest" -> Json.fromString(descriptordigest.value),
      "requestDigest" -> Json.fromString(requestdigest.value),
      "availability" -> Json.fromString(_availability(availability)),
      "qualityPolicy" -> Json.obj(
        "authority" -> Json.fromString(qualitypolicy.authority.value),
        "declaredCostUnits" -> Json.fromLong(qualitypolicy.declaredCostUnits),
        "maximumCostUnits" -> Json.fromLong(qualitypolicy.maximumCostUnits)
      )
    )

  private def _reuse_request(document: String): Either[String, Json] =
    io.circe.parser.parse(document)
      .left.map(_ => "registered-provider-reuse-request-invalid")
      .flatMap(_.asObject.toRight("registered-provider-reuse-request-invalid"))
      .flatMap(_remove_review_id)
      .map(Json.fromJsonObject)

  private def _remove_review_id(fields: JsonObject): Either[String, JsonObject] =
    fields("reviewId")
      .flatMap(_.asString)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_ => fields.remove("reviewId"))
      .toRight("registered-provider-reuse-request-invalid")

  private def _reuse_document(
    descriptordigest: ReviewDigest,
    request: Json,
    availability: ProviderBundleAvailability,
    qualitypolicy: CarReviewQualityProviderPolicy
  ): Json =
    Json.obj(
      "schemaVersion" -> Json.fromString(REUSE_SCHEMA_VERSION),
      "definitionId" -> Json.fromString(REUSE_DEFINITION_ID),
      "descriptorDigest" -> Json.fromString(descriptordigest.value),
      "request" -> request,
      "availability" -> Json.fromString(_availability(availability)),
      "qualityPolicy" -> Json.obj(
        "authority" -> Json.fromString(qualitypolicy.authority.value),
        "declaredCostUnits" -> Json.fromLong(qualitypolicy.declaredCostUnits),
        "maximumCostUnits" -> Json.fromLong(qualitypolicy.maximumCostUnits)
      )
    )

  private def _availability(value: ProviderBundleAvailability): String = value match {
    case ProviderBundleAvailability.Enabled => "enabled"
    case ProviderBundleAvailability.Unavailable => "unavailable"
    case ProviderBundleAvailability.Disabled => "disabled"
    case ProviderBundleAvailability.Failed => "failed"
  }

  private def _sha256(value: String): String = {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
    "sha256:" + bytes.map(byte => f"${byte & 0xff}%02x").mkString
  }
}

/**
 * Executes only the provider selections already frozen in a diagnosis plan.
 * Every selection is verified against the registry before any runner can work,
 * then every admitted bundle crosses the canonical response boundary exactly once.
 */
final class CarReviewRegisteredProviderReviewApplication(
  registry: CarReviewProviderRegistry,
  coordinator: CarReviewProviderExecutionCoordinator = new CarReviewProviderExecutionCoordinator(),
  canonical: CarReviewCanonicalResponseApplication = new CarReviewCanonicalResponseApplication()
) {
  def execute(
    execution: CarReviewRegisteredReviewExecution,
    actorRoles: Set[String]
  ): Consequence[CarReviewCanonicalResponse] =
    for {
      _ <- CarReviewAuthorization.authorize("review.submit-bundle", actorRoles)
      _ <- _consequence(_preflight(execution))
      inputs <- _consequence(_execute(execution))
      response <- canonical.build(execution.template, inputs, actorRoles)
    } yield response

  private def _consequence[A](value: Either[String, A]): Consequence[A] = value match {
    case Right(result) => Consequence.success(result)
    case Left(error) => Consequence.operationInvalid(error)
  }

  private def _preflight(execution: CarReviewRegisteredReviewExecution): Either[String, Unit] =
    for {
      _ <- _template_matches_plan(execution)
      _ <- _provider_bound(execution.providers)
      _ <- _unique_provider_identities(execution.providers)
      _ <- _selection_sets_match_plan(execution)
      _ <- _preflight_selections(execution)
    } yield ()

  private def _template_matches_plan(execution: CarReviewRegisteredReviewExecution): Either[String, Unit] = {
    val template = execution.template
    val request = execution.plan.request
    Either.cond(
      template.reviewId == request.reviewId &&
        template.target == request.target &&
        template.profile == request.profile &&
        template.execution.startedAt == request.startedAt,
      (),
      _failure("review-template-plan-mismatch")
    )
  }

  private def _provider_bound(providers: Vector[CarReviewRegisteredProviderSelection]): Either[String, Unit] =
    Either.cond(
      providers.nonEmpty && providers.size <= 8,
      (),
      _failure("registered-provider-selection-bound-invalid")
    )

  private def _unique_provider_identities(providers: Vector[CarReviewRegisteredProviderSelection]): Either[String, Unit] =
    Either.cond(
      providers.map(_.request.provider).distinct.size == providers.size,
      (),
      _failure("registered-provider-selection-duplicate")
    )

  private def _selection_sets_match_plan(execution: CarReviewRegisteredReviewExecution): Either[String, Unit] = {
    execution.providers.foldLeft[Either[String, Vector[(ReviewProviderIdentity, ReviewRuleIdentity)]]](Right(Vector.empty)) { (result, selection) =>
      for {
        values <- result
        descriptor <- CarReviewProviderBundleAdmission.describeDescriptor(selection.descriptorDocument).left.map(_ => _failure("registered-provider-descriptor-invalid"))
      } yield values :+ (selection.request.provider -> descriptor.ruleSet)
    }.flatMap { values =>
      val actual = values.toSet
      val selected = execution.plan.reuseInput.providerSelections.map(value => value.provider -> value.ruleSet).toSet
      val actualrules = actual.map(_._2)
      Either.cond(
        actual == selected && actualrules == execution.plan.reuseInput.ruleSets.toSet,
        (),
        _failure("registered-provider-selection-plan-mismatch")
      )
    }
  }

  private def _preflight_selections(execution: CarReviewRegisteredReviewExecution): Either[String, Unit] =
    execution.providers.foldLeft[Either[String, Unit]](Right(())) { (result, selection) =>
      result.flatMap(_ => _preflight_selection(execution.plan, selection))
    }

  private def _preflight_selection(
    plan: CarReviewExecutionPlan,
    selection: CarReviewRegisteredProviderSelection
  ): Either[String, Unit] =
    for {
      descriptor <- CarReviewProviderBundleAdmission.describeDescriptor(selection.descriptorDocument).left.map(_ => _failure("registered-provider-descriptor-invalid"))
      descriptordigest <- CarReviewProviderBundleAdmission.descriptorDigest(selection.descriptorDocument).left.map(_ => _failure("registered-provider-descriptor-invalid"))
      requestdescriptor <- CarReviewProviderBundleAdmission.describeDescriptor(selection.request.descriptor).left.map(_ => _failure("registered-provider-request-descriptor-invalid"))
      requestdescriptordigest <- CarReviewProviderBundleAdmission.descriptorDigest(selection.request.descriptor).left.map(_ => _failure("registered-provider-request-descriptor-invalid"))
      _ <- Either.cond(descriptor == requestdescriptor && descriptordigest == requestdescriptordigest, (), _failure("registered-provider-request-descriptor-mismatch"))
      _ <- CarReviewProviderBundleAdmission.requestDigest(selection.request.providerRequest).left.map(_ => _failure("registered-provider-request-invalid"))
      _ <- CarReviewProviderBundleAdmission.requestBinding(selection.request.providerRequest, selection.request.reviewId, selection.request.target).left.map {
        case "review-id-mismatch" => _failure("registered-provider-request-review-id-mismatch")
        case "request-target-mismatch" => _failure("registered-provider-request-target-mismatch")
        case _ => _failure("registered-provider-request-binding-invalid")
      }
      _ <- Either.cond(selection.request.reviewId == plan.request.reviewId && selection.request.target == plan.request.target, (), _failure("registered-provider-request-plan-mismatch"))
      registration <- registry.registrationFor(selection.request.provider).toRight(_failure("provider-not-registered"))
      _ <- Either.cond(descriptor == registration.descriptor && descriptordigest == registration.descriptorDigest, (), _failure("provider-registration-mismatch"))
      _ <- Either.cond(descriptor.provider == selection.request.provider && _planned_rule(plan, selection.request.provider).contains(descriptor.ruleSet), (), _failure("registered-provider-identity-rule-mismatch"))
      policy <- CarReviewProviderSelectionPolicy.reuseDigest(registration.descriptorDigest, selection.request.providerRequest, selection.request.availability, registration.qualityPolicy)
        .left.map(_ => _failure("registered-provider-reuse-request-invalid"))
      _ <- Either.cond(_planned_policy(plan, selection.request.provider).contains(policy), (), _failure("registered-provider-policy-digest-mismatch"))
    } yield ()

  private def _planned_rule(plan: CarReviewExecutionPlan, provider: ReviewProviderIdentity): Option[ReviewRuleIdentity] =
    plan.reuseInput.providerSelections.find(_.provider == provider).map(_.ruleSet)

  private def _planned_policy(plan: CarReviewExecutionPlan, provider: ReviewProviderIdentity): Option[ReviewDigest] =
    plan.reuseInput.providerSelections.find(_.provider == provider).map(_.availabilityPolicyDigest)

  private def _execute(execution: CarReviewRegisteredReviewExecution): Either[String, Vector[AdmittedProviderBundleInput]] =
    execution.providers.sortBy(value => (value.request.provider.id.value, value.request.provider.version.value)).foldLeft[Either[String, Vector[AdmittedProviderBundleInput]]](Right(Vector.empty)) { (result, selection) =>
      result.flatMap { inputs =>
        coordinator.execute(selection.request, registry) match {
          case ProviderBundleExecutionOutcome.Admitted(value, _, bundle) => Right(inputs :+ AdmittedProviderBundleInput(value, bundle))
          case ProviderBundleExecutionOutcome.Refused(value) => Left(_failure(value.limitation.code))
        }
      }
    }

  private def _failure(code: String): String =
    _validated_code(code) match {
      case Some(validated) => s"textus.cbd.review.failure.v1:$validated"
      case None => "textus.cbd.review.failure.v1:review-provider-execution-failed"
    }

  private def _validated_code(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.matches("[a-z][a-z0-9.-]{0,127}"))
}
