package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser

/*
 * @since   Oct.  6, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Ordinary typed persistence of a closed selection, without content identity. */
object CarReviewExecutionSelectionCodec {
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private val _fields = Set("schemaVersion", "definitionId", "reviewSchemaVersion", "targetReference", "targetKind", "organization", "name", "version", "profile", "baselineReference", "ruleSets", "providerSelections", "evidenceSnapshots", "policyBindings")

  def encode(input: CarReviewReuseKeyInput): Either[CarReviewReuseKeyFailure, String] =
    CarReviewReuseKey.validate(input).map { _ =>
      Json.obj(
        "schemaVersion" -> Json.fromString(CarReviewReuseKey.DEFINITION_ID),
        "definitionId" -> Json.fromString(input.definitionId),
        "reviewSchemaVersion" -> Json.fromString(input.reviewSchemaVersion.value),
        "targetReference" -> _reference_json(input.targetReference),
        "targetKind" -> Json.fromString(input.targetKind.value),
        "organization" -> input.organization.fold(Json.Null)(Json.fromString),
        "name" -> Json.fromString(input.name),
        "version" -> input.version.fold(Json.Null)(value => Json.fromString(value.value)),
        "profile" -> Json.fromString(input.profile.value),
        "baselineReference" -> input.baselineReference.fold(Json.Null)(_reference_json),
        "ruleSets" -> Json.fromValues(input.ruleSets.map(value => _identity_json(value.id.value, value.version.value))),
        "providerSelections" -> Json.fromValues(input.providerSelections.map(value => Json.obj(
          "provider" -> _identity_json(value.provider.id.value, value.provider.version.value),
          "ruleSet" -> _identity_json(value.ruleSet.id.value, value.ruleSet.version.value),
          "availabilityPolicy" -> _reference_json(value.availabilityPolicy)))),
        "evidenceSnapshots" -> Json.fromValues(input.evidenceSnapshots.map(value => Json.obj(
          "evidenceClass" -> Json.fromString(value.evidenceClass),
          "snapshot" -> _reference_json(value.snapshot),
          "provider" -> _identity_json(value.provider.id.value, value.provider.version.value)))),
        "policyBindings" -> Json.fromValues(input.policyBindings.map(value => Json.obj(
          "scope" -> Json.fromString(value.scope), "policyId" -> Json.fromString(value.policyId),
          "policyVersion" -> Json.fromString(value.policyVersion.value), "revision" -> Json.fromLong(value.revision))))
      ).noSpaces
    }

  def decode(document: String): Either[CarReviewReuseKeyFailure, CarReviewReuseKeyInput] =
    for {
      json <- Option(document).filter(_.trim.nonEmpty).toRight(CarReviewReuseKeyFailure("review-plan-selection-incomplete", "Persisted selection is absent."))
        .flatMap(value => _parser.parse(value).left.map(_ => CarReviewReuseKeyFailure("review-plan-selection-invalid", "Selection must be JSON without duplicate fields.")))
      root <- _object(json, _fields, "review-plan-selection-invalid")
      schema <- _string(root("schemaVersion").get, "unsupported-reuse-key-definition")
      _ <- Either.cond(schema == CarReviewReuseKey.DEFINITION_ID, (), CarReviewReuseKeyFailure("unsupported-reuse-key-definition", "Selection schema is not supported."))
      definition <- _string(root("definitionId").get, "unsupported-reuse-key-definition")
      reviewschema <- _string(root("reviewSchemaVersion").get, "incompatible-reuse-schema")
      target <- _reference(root("targetReference").get, "invalid-reuse-target")
      kind <- _string(root("targetKind").get, "invalid-reuse-target")
      organization <- _optional(root("organization").get, value => _string(value, "invalid-reuse-target"))
      name <- _string(root("name").get, "invalid-reuse-target")
      version <- _optional(root("version").get, value => _string(value, "invalid-reuse-target").map(ReviewVersion.apply))
      profile <- _string(root("profile").get, "invalid-reuse-profile")
      baseline <- _optional(root("baselineReference").get, value => _reference(value, "invalid-reuse-baseline"))
      rules <- _vector(root("ruleSets").get, "invalid-reuse-rule-set", _rule)
      providers <- _vector(root("providerSelections").get, "invalid-reuse-provider", _provider_selection)
      evidence <- _vector(root("evidenceSnapshots").get, "invalid-reuse-evidence", _evidence)
      policies <- _vector(root("policyBindings").get, "invalid-reuse-policy", _policy)
      input = CarReviewReuseKeyInput(definition, ReviewSchemaVersion(reviewschema), target, ReviewTargetKind(kind), organization, name, version, ReviewProfile(profile), baseline, rules, providers, evidence, policies)
      _ <- CarReviewReuseKey.validate(input)
    } yield input

  def compatible(expected: CarReviewReuseKeyInput, retained: CarReviewReuseKeyInput): Either[CarReviewReuseKeyFailure, Unit] =
    for {
      _ <- CarReviewReuseKey.validate(expected)
      _ <- CarReviewReuseKey.validate(retained)
      _ <- Either.cond(
        expected.definitionId == retained.definitionId && expected.reviewSchemaVersion == retained.reviewSchemaVersion &&
          expected.targetReference == retained.targetReference && expected.targetKind == retained.targetKind &&
          expected.organization == retained.organization && expected.name == retained.name && expected.version == retained.version &&
          expected.profile == retained.profile && expected.baselineReference == retained.baselineReference &&
          expected.ruleSets.toSet == retained.ruleSets.toSet && expected.providerSelections.toSet == retained.providerSelections.toSet &&
          expected.evidenceSnapshots.toSet == retained.evidenceSnapshots.toSet && expected.policyBindings.toSet == retained.policyBindings.toSet,
        (), CarReviewReuseKeyFailure("review-plan-selection-inconsistent", "Retained declared selection differs from the selected immutable plan.")
      )
    } yield ()

  private def _object(json: Json, fields: Set[String], code: String): Either[CarReviewReuseKeyFailure, JsonObject] =
    json.asObject.toRight(CarReviewReuseKeyFailure(code, "Expected a closed typed object.")).flatMap { value =>
      if (!fields.subsetOf(value.keys.toSet)) _failure("review-plan-selection-incomplete", "Required persisted declaration is missing.")
      else if (value.keys.toSet != fields) _failure(code, "Unknown typed selection fields are refused.")
      else Right(value)
    }

  private def _string(json: Json, code: String): Either[CarReviewReuseKeyFailure, String] =
    json.asString.toRight(CarReviewReuseKeyFailure(code, "Expected a declared string."))

  private def _revision(json: Json, code: String): Either[CarReviewReuseKeyFailure, Long] =
    json.asNumber.flatMap(number => number.toLong.filter(value => value > 0 && number.toString == value.toString))
      .toRight(CarReviewReuseKeyFailure(code, "Expected a positive integer revision."))

  private def _reference(json: Json, code: String): Either[CarReviewReuseKeyFailure, CarReviewDeclaredReference] =
    for {
      value <- _object(json, Set("id", "revision"), code)
      id <- _string(value("id").get, code)
      revision <- _revision(value("revision").get, code)
    } yield CarReviewDeclaredReference(id, revision)

  private def _identity(json: Json, code: String): Either[CarReviewReuseKeyFailure, (String, String)] =
    for {
      value <- _object(json, Set("id", "version"), code)
      id <- _string(value("id").get, code)
      version <- _string(value("version").get, code)
    } yield (id, version)

  private def _rule(json: Json): Either[CarReviewReuseKeyFailure, ReviewRuleIdentity] =
    _identity(json, "invalid-reuse-rule-set").map { case (id, version) => ReviewRuleIdentity(ReviewRuleId(id), ReviewVersion(version)) }

  private def _provider(json: Json): Either[CarReviewReuseKeyFailure, ReviewProviderIdentity] =
    _identity(json, "invalid-reuse-provider").map { case (id, version) => ReviewProviderIdentity(ReviewProviderId(id), ReviewVersion(version)) }

  private def _provider_selection(json: Json): Either[CarReviewReuseKeyFailure, CarReviewReuseProviderSelection] =
    for {
      value <- _object(json, Set("provider", "ruleSet", "availabilityPolicy"), "invalid-reuse-provider")
      provider <- _provider(value("provider").get)
      rule <- _rule(value("ruleSet").get)
      policy <- _reference(value("availabilityPolicy").get, "invalid-reuse-provider")
    } yield CarReviewReuseProviderSelection(provider, rule, policy)

  private def _evidence(json: Json): Either[CarReviewReuseKeyFailure, CarReviewReuseEvidenceSnapshot] =
    for {
      value <- _object(json, Set("evidenceClass", "snapshot", "provider"), "invalid-reuse-evidence")
      evidenceclass <- _string(value("evidenceClass").get, "invalid-reuse-evidence")
      snapshot <- _reference(value("snapshot").get, "invalid-reuse-evidence")
      provider <- _provider(value("provider").get)
    } yield CarReviewReuseEvidenceSnapshot(evidenceclass, snapshot, provider)

  private def _policy(json: Json): Either[CarReviewReuseKeyFailure, CarReviewReusePolicyBinding] =
    for {
      value <- _object(json, Set("scope", "policyId", "policyVersion", "revision"), "invalid-reuse-policy")
      scope <- _string(value("scope").get, "invalid-reuse-policy")
      id <- _string(value("policyId").get, "invalid-reuse-policy")
      version <- _string(value("policyVersion").get, "invalid-reuse-policy")
      revision <- _revision(value("revision").get, "invalid-reuse-policy")
    } yield CarReviewReusePolicyBinding(scope, id, ReviewVersion(version), revision)

  private def _optional[A](json: Json, decode: Json => Either[CarReviewReuseKeyFailure, A]): Either[CarReviewReuseKeyFailure, Option[A]] =
    if (json.isNull) Right(None) else decode(json).map(Some.apply)

  private def _vector[A](json: Json, code: String, decode: Json => Either[CarReviewReuseKeyFailure, A]): Either[CarReviewReuseKeyFailure, Vector[A]] =
    json.asArray.toRight(CarReviewReuseKeyFailure(code, "Expected an explicit array.")).flatMap { values =>
      values.foldLeft[Either[CarReviewReuseKeyFailure, Vector[A]]](Right(Vector.empty)) { (result, value) =>
        for { previous <- result; decoded <- decode(value) } yield previous :+ decoded
      }
    }

  private def _reference_json(value: CarReviewDeclaredReference): Json =
    Json.obj("id" -> Json.fromString(value.id), "revision" -> Json.fromLong(value.revision))

  private def _identity_json(id: String, version: String): Json =
    Json.obj("id" -> Json.fromString(id), "version" -> Json.fromString(version))

  private def _failure(code: String, message: String): Left[CarReviewReuseKeyFailure, Nothing] =
    Left(CarReviewReuseKeyFailure(code, message))
}
