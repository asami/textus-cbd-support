package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, Printer}

/*
 * @since   Jul. 23, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewReuseProviderSelection(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  availabilityPolicyDigest: ReviewDigest
)

final case class CarReviewReuseEvidenceSnapshot(
  evidenceClass: String,
  snapshotId: String,
  provider: ReviewProviderIdentity,
  digest: ReviewDigest
)

final case class CarReviewReusePolicyBinding(
  scope: String,
  policyId: String,
  policyVersion: ReviewVersion,
  policyDigest: ReviewDigest
)

final case class CarReviewReuseKeyInput(
  definitionId: String,
  reviewSchemaVersion: ReviewSchemaVersion,
  target: ReviewTarget,
  profile: ReviewProfile,
  baselineDigest: Option[ReviewDigest],
  ruleSets: Vector[ReviewRuleIdentity],
  providerSelections: Vector[CarReviewReuseProviderSelection],
  evidenceSnapshots: Vector[CarReviewReuseEvidenceSnapshot],
  policyBindings: Vector[CarReviewReusePolicyBinding]
)

final case class CarReviewReuseKey(
  definitionId: String,
  digest: ReviewDigest
)

final case class CarReviewReuseKeyFailure(code: String, message: String)

/**
 * Canonical identity for a diagnosis request before any provider work starts.
 * It deliberately excludes Run IDs, times, credentials, rendered output, and
 * raw provider payloads. P8-42 owns persistence and actual reuse decisions.
 */
object CarReviewReuseKey {
  val DEFINITION_ID = "textus.cbd.review-reuse-key.v1"

  private val _policy_scopes = Set("profile", "gate", "reconciliation", "suppression")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  def calculate(input: CarReviewReuseKeyInput): Either[CarReviewReuseKeyFailure, CarReviewReuseKey] =
    canonicalDocument(input).map(document => CarReviewReuseKey(DEFINITION_ID, ReviewDigest(_sha256(document))))

  def canonicalDocument(input: CarReviewReuseKeyInput): Either[CarReviewReuseKeyFailure, String] =
    _validate(input).map(_ => _printer.print(_canonical_json(input)))

  private def _validate(input: CarReviewReuseKeyInput): Either[CarReviewReuseKeyFailure, Unit] =
    if (input.definitionId != DEFINITION_ID)
      Left(CarReviewReuseKeyFailure("unsupported-reuse-key-definition", "Reuse-key definition ID is not supported."))
    else if (!_target(input.target))
      Left(CarReviewReuseKeyFailure("invalid-reuse-target", "Reuse-key target identity or digest is invalid."))
    else if (input.reviewSchemaVersion.value != CarReviewVocabulary.SCHEMA_VERSION)
      Left(CarReviewReuseKeyFailure("incompatible-reuse-schema", "Reuse-key report schema version is not supported."))
    else if (!_identifier(input.profile.value))
      Left(CarReviewReuseKeyFailure("invalid-reuse-profile", "Reuse-key profile is invalid."))
    else if (input.baselineDigest.exists(digest => !_digest(digest)))
      Left(CarReviewReuseKeyFailure("invalid-reuse-baseline", "Reuse-key baseline digest is invalid."))
    else if (_duplicate(input.ruleSets.map(_.id.value)))
      Left(CarReviewReuseKeyFailure("duplicate-reuse-rule-set", "Reuse-key rule-set IDs must be unique."))
    else if (_duplicate(input.providerSelections.map(_.provider.id.value)))
      Left(CarReviewReuseKeyFailure("duplicate-reuse-provider", "Reuse-key provider IDs must be unique."))
    else if (!input.providerSelections.forall(selection => input.ruleSets.contains(selection.ruleSet)))
      Left(CarReviewReuseKeyFailure("unbound-reuse-provider-rule", "Every selected provider rule-set must be present in the frozen rule-set selection."))
    else if (_duplicate(input.evidenceSnapshots.map(snapshot => (snapshot.evidenceClass, snapshot.snapshotId, snapshot.provider.id.value, snapshot.provider.version.value))))
      Left(CarReviewReuseKeyFailure("duplicate-reuse-evidence", "Reuse-key evidence snapshot identities must be unique."))
    else if (_duplicate(input.policyBindings.map(_.scope)) || input.policyBindings.map(_.scope).toSet != _policy_scopes)
      Left(CarReviewReuseKeyFailure("invalid-reuse-policy-scopes", "Reuse-key policy scopes must contain profile, gate, reconciliation, and suppression exactly once."))
    else if (!input.ruleSets.forall(rule => _identifier(rule.id.value) && _version(rule.version.value)))
      Left(CarReviewReuseKeyFailure("invalid-reuse-rule-set", "Reuse-key rule-set identities are invalid."))
    else if (!input.providerSelections.forall(selection => _provider(selection.provider) && _rule(selection.ruleSet) && _digest(selection.availabilityPolicyDigest)))
      Left(CarReviewReuseKeyFailure("invalid-reuse-provider", "Reuse-key provider selection is invalid."))
    else if (!input.evidenceSnapshots.forall(snapshot => _identifier(snapshot.evidenceClass) && _identifier(snapshot.snapshotId) && _provider(snapshot.provider) && _digest(snapshot.digest)))
      Left(CarReviewReuseKeyFailure("invalid-reuse-evidence", "Reuse-key evidence snapshot is invalid."))
    else if (!input.policyBindings.forall(binding => _identifier(binding.policyId) && _version(binding.policyVersion.value) && _digest(binding.policyDigest)))
      Left(CarReviewReuseKeyFailure("invalid-reuse-policy", "Reuse-key policy binding is invalid."))
    else Right(())

  private def _canonical_json(input: CarReviewReuseKeyInput): Json =
    Json.obj(
      "definitionId" -> Json.fromString(input.definitionId),
      "reviewSchemaVersion" -> Json.fromString(input.reviewSchemaVersion.value),
      "target" -> _target_json(input.target),
      "profile" -> Json.fromString(input.profile.value),
      "baselineDigest" -> input.baselineDigest.fold(Json.Null)(digest => Json.fromString(digest.value)),
      "ruleSets" -> _sorted_json(input.ruleSets.map(_rule_json)),
      "providerSelections" -> _sorted_json(input.providerSelections.map(_provider_selection_json)),
      "evidenceSnapshots" -> _sorted_json(input.evidenceSnapshots.map(_evidence_snapshot_json)),
      "policyBindings" -> _sorted_json(input.policyBindings.map(_policy_binding_json))
    )

  private def _target_json(target: ReviewTarget): Json =
    Json.obj(
      "kind" -> Json.fromString(target.kind.value),
      "organization" -> target.organization.fold(Json.Null)(Json.fromString),
      "name" -> Json.fromString(target.name),
      "version" -> target.version.fold(Json.Null)(version => Json.fromString(version.value)),
      "digest" -> Json.fromString(target.digest.value)
    )

  private def _provider_selection_json(selection: CarReviewReuseProviderSelection): Json =
    Json.obj(
      "provider" -> _provider_json(selection.provider),
      "ruleSet" -> _rule_json(selection.ruleSet),
      "availabilityPolicyDigest" -> Json.fromString(selection.availabilityPolicyDigest.value)
    )

  private def _evidence_snapshot_json(snapshot: CarReviewReuseEvidenceSnapshot): Json =
    Json.obj(
      "evidenceClass" -> Json.fromString(snapshot.evidenceClass),
      "snapshotId" -> Json.fromString(snapshot.snapshotId),
      "provider" -> _provider_json(snapshot.provider),
      "digest" -> Json.fromString(snapshot.digest.value)
    )

  private def _policy_binding_json(binding: CarReviewReusePolicyBinding): Json =
    Json.obj(
      "scope" -> Json.fromString(binding.scope),
      "policyId" -> Json.fromString(binding.policyId),
      "policyVersion" -> Json.fromString(binding.policyVersion.value),
      "policyDigest" -> Json.fromString(binding.policyDigest.value)
    )

  private def _provider_json(provider: ReviewProviderIdentity): Json =
    Json.obj("id" -> Json.fromString(provider.id.value), "version" -> Json.fromString(provider.version.value))

  private def _rule_json(rule: ReviewRuleIdentity): Json =
    Json.obj("id" -> Json.fromString(rule.id.value), "version" -> Json.fromString(rule.version.value))

  private def _sorted_json(values: Vector[Json]): Json =
    Json.fromValues(values.sortBy(_printer.print))

  private def _provider(value: ReviewProviderIdentity): Boolean =
    _identifier(value.id.value) && _version(value.version.value)

  private def _rule(value: ReviewRuleIdentity): Boolean =
    _identifier(value.id.value) && _version(value.version.value)

  private def _target(value: ReviewTarget): Boolean =
    CarReviewVocabulary.TARGET_KINDS.contains(value.kind.value) &&
      value.organization.forall(_identifier) &&
      _identifier(value.name) &&
      value.version.forall(version => _version(version.value)) &&
      _digest(value.digest)

  private def _identifier(value: String): Boolean =
    value.nonEmpty && value.length <= 180 && value.forall(character => character.isLetterOrDigit || "._-:/".contains(character))

  private def _version(value: String): Boolean =
    value.nonEmpty && value.length <= 80

  private def _digest(value: ReviewDigest): Boolean =
    _digest_pattern.matches(value.value)

  private def _duplicate[A](values: Vector[A]): Boolean =
    values.distinct.size != values.size

  private def _sha256(value: String): String = {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
    "sha256:" + bytes.map(byte => f"${byte & 0xff}%02x").mkString
  }
}
