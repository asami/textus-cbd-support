package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 23, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewDeclaredReference(id: String, revision: Long)

final case class CarReviewReuseProviderSelection(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  availabilityPolicy: CarReviewDeclaredReference
)

final case class CarReviewReuseEvidenceSnapshot(
  evidenceClass: String,
  snapshot: CarReviewDeclaredReference,
  provider: ReviewProviderIdentity
)

final case class CarReviewReusePolicyBinding(
  scope: String,
  policyId: String,
  policyVersion: ReviewVersion,
  revision: Long
)

final case class CarReviewReuseKeyInput(
  definitionId: String,
  reviewSchemaVersion: ReviewSchemaVersion,
  targetReference: CarReviewDeclaredReference,
  targetKind: ReviewTargetKind,
  organization: Option[String],
  name: String,
  version: Option[ReviewVersion],
  profile: ReviewProfile,
  baselineReference: Option[CarReviewDeclaredReference],
  ruleSets: Vector[ReviewRuleIdentity],
  providerSelections: Vector[CarReviewReuseProviderSelection],
  evidenceSnapshots: Vector[CarReviewReuseEvidenceSnapshot],
  policyBindings: Vector[CarReviewReusePolicyBinding]
)

final case class CarReviewReuseKeyFailure(code: String, message: String)

/** Validates declared selection data; dependency publishers own its revisions. */
object CarReviewReuseKey {
  val DEFINITION_ID = "textus.cbd.review-execution-selection.v2"

  private val _policy_scopes = Set("profile", "gate", "reconciliation", "suppression")

  def validate(input: CarReviewReuseKeyInput): Either[CarReviewReuseKeyFailure, Unit] =
    if (input == null || input.reviewSchemaVersion == null || input.targetKind == null ||
        input.organization == null || input.version == null || input.profile == null ||
        input.baselineReference == null || input.ruleSets == null || input.providerSelections == null ||
        input.evidenceSnapshots == null || input.policyBindings == null)
      _failure("review-plan-selection-incomplete", "Required selection declarations are absent.")
    else if (input.definitionId != DEFINITION_ID)
      _failure("unsupported-reuse-key-definition", "Execution-selection definition is not supported.")
    else if (!_reference(input.targetReference) || !CarReviewVocabulary.TARGET_KINDS.contains(input.targetKind.value) ||
        !input.organization.forall(_identifier) || !_identifier(input.name) ||
        !input.version.forall(value => value != null && _version(value.value)))
      _failure("invalid-reuse-target", "Declared target identity or revision is invalid.")
    else if (input.reviewSchemaVersion.value != CarReviewVocabulary.SCHEMA_VERSION)
      _failure("incompatible-reuse-schema", "Review report schema is not supported.")
    else if (!_identifier(input.profile.value))
      _failure("invalid-reuse-profile", "Review profile is invalid.")
    else if (!input.baselineReference.forall(_reference))
      _failure("invalid-reuse-baseline", "Declared baseline identity or revision is invalid.")
    else if (!input.ruleSets.forall(_rule))
      _failure("invalid-reuse-rule-set", "Rule-set identities are invalid.")
    else if (!input.providerSelections.forall(selection => selection != null && _provider(selection.provider) && _rule(selection.ruleSet) && _reference(selection.availabilityPolicy)))
      _failure("invalid-reuse-provider", "Provider selection or availability-policy reference is invalid.")
    else if (!input.evidenceSnapshots.forall(snapshot => snapshot != null && _identifier(snapshot.evidenceClass) && _reference(snapshot.snapshot) && _provider(snapshot.provider)))
      _failure("invalid-reuse-evidence", "Evidence snapshot identity or revision is invalid.")
    else if (!input.policyBindings.forall(binding => binding != null && _identifier(binding.scope) && _identifier(binding.policyId) && binding.policyVersion != null && _version(binding.policyVersion.value) && binding.revision > 0))
      _failure("invalid-reuse-policy", "Policy identity or revision is invalid.")
    else if (_duplicate(input.ruleSets.map(_.id.value)))
      _failure("duplicate-reuse-rule-set", "Rule-set IDs must be unique.")
    else if (_duplicate(input.providerSelections.map(_.provider.id.value)))
      _failure("duplicate-reuse-provider", "Provider IDs must be unique.")
    else if (!input.providerSelections.forall(selection => input.ruleSets.contains(selection.ruleSet)))
      _failure("unbound-reuse-provider-rule", "Selected provider rules must belong to the declared rule selection.")
    else if (_duplicate(input.evidenceSnapshots.map(snapshot => (snapshot.evidenceClass, snapshot.snapshot.id, snapshot.provider.id.value, snapshot.provider.version.value))))
      _failure("duplicate-reuse-evidence", "Evidence snapshot identities must be unique.")
    else if (_duplicate(input.policyBindings.map(_.scope)) || input.policyBindings.map(_.scope).toSet != _policy_scopes)
      _failure("invalid-reuse-policy-scopes", "Policies must contain profile, gate, reconciliation, and suppression exactly once.")
    else Right(())

  private def _reference(value: CarReviewDeclaredReference): Boolean =
    value != null && _identifier(value.id) && value.revision > 0

  private def _provider(value: ReviewProviderIdentity): Boolean =
    value != null && value.id != null && value.version != null && _identifier(value.id.value) && _version(value.version.value)

  private def _rule(value: ReviewRuleIdentity): Boolean =
    value != null && value.id != null && value.version != null && _identifier(value.id.value) && _version(value.version.value)

  private def _identifier(value: String): Boolean =
    value != null && value.nonEmpty && value.length <= 180 && value.forall(character => character.isLetterOrDigit || "._-:/".contains(character))

  private def _version(value: String): Boolean =
    value != null && value.nonEmpty && value.length <= 80

  private def _duplicate[A](values: Vector[A]): Boolean =
    values.distinct.size != values.size

  private def _failure(code: String, message: String): Left[CarReviewReuseKeyFailure, Nothing] =
    Left(CarReviewReuseKeyFailure(code, message))
}
