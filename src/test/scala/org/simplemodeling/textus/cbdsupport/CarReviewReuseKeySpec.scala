package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 23, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewReuseKeySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Declared CAR Review execution selections" should {
    "restore typed references and accept reordered selection vectors" in {
      Given("a complete selection with positive target, baseline, policy and evidence revisions")
      val input = _input
      val reordered = input.copy(ruleSets = input.ruleSets.reverse, providerSelections = input.providerSelections.reverse,
        evidenceSnapshots = input.evidenceSnapshots.reverse, policyBindings = input.policyBindings.reverse)

      When("production validation and the persistence codec encode, decode and compare the selection")
      val validation = CarReviewReuseKey.validate(input)
      val restored = CarReviewExecutionSelectionCodec.encode(input).flatMap(CarReviewExecutionSelectionCodec.decode)
      val compatibility = CarReviewExecutionSelectionCodec.compatible(input, reordered)
      val optional = input.copy(baselineReference = None, evidenceSnapshots = Vector.empty)
      val optionalrestored = CarReviewExecutionSelectionCodec.encode(optional).flatMap(CarReviewExecutionSelectionCodec.decode)

      Then("all declared identities and revisions survive and vector order adds no identity")
      validation shouldBe Right(())
      restored shouldBe Right(input)
      restored.map(_.targetReference.revision) shouldBe Right(3L)
      compatibility shouldBe Right(())
      optionalrestored shouldBe Right(optional)
    }

    "refuse missing or invalid revisions and ambiguous selections with attributed failures" in {
      Given("missing, Boolean, decimal, exponent, overflowing or otherwise invalid revisions, duplicate selections and incomplete policy scopes")
      val input = _input
      val cases = Vector(
        input.copy(targetReference = CarReviewDeclaredReference("", 1L)) -> "invalid-reuse-target",
        input.copy(targetReference = CarReviewDeclaredReference("target:main", 0L)) -> "invalid-reuse-target",
        input.copy(baselineReference = Some(CarReviewDeclaredReference("baseline:main", -1L))) -> "invalid-reuse-baseline",
        input.copy(ruleSets = input.ruleSets :+ input.ruleSets.head) -> "duplicate-reuse-rule-set",
        input.copy(providerSelections = input.providerSelections :+ input.providerSelections.head) -> "duplicate-reuse-provider",
        input.copy(evidenceSnapshots = input.evidenceSnapshots :+ input.evidenceSnapshots.head) -> "duplicate-reuse-evidence",
        input.copy(policyBindings = input.policyBindings :+ input.policyBindings.head) -> "invalid-reuse-policy-scopes",
        input.copy(policyBindings = input.policyBindings.dropRight(1)) -> "invalid-reuse-policy-scopes",
        input.copy(providerSelections = Vector(input.providerSelections.head.copy(availabilityPolicy = CarReviewDeclaredReference("", 1L)))) -> "invalid-reuse-provider",
        input.copy(evidenceSnapshots = Vector(input.evidenceSnapshots.head.copy(snapshot = CarReviewDeclaredReference("evidence:main", 0L)))) -> "invalid-reuse-evidence",
        input.copy(policyBindings = input.policyBindings.updated(0, input.policyBindings.head.copy(revision = -1L))) -> "invalid-reuse-policy",
        input.copy(providerSelections = Vector(input.providerSelections.head.copy(ruleSet = ReviewRuleIdentity(ReviewRuleId("absent"), ReviewVersion("1"))))) -> "unbound-reuse-provider-rule",
        input.copy(reviewSchemaVersion = ReviewSchemaVersion("unsupported")) -> "incompatible-reuse-schema"
      )
      val target = _json.hcursor.downField("targetReference").focus.get
      val revisions = Vector("0", "-1", "true", "1.0", "1e0", "1.5", "9223372036854775808", "-9223372036854775809", "\"1\"", "null")
        .map(raw => io.circe.parser.parse(raw).fold(error => fail(error.message), identity))
      val documents = Vector(
        _json.mapObject(_.remove("targetReference")) -> "review-plan-selection-incomplete",
        _json.mapObject(_.add("targetReference", target.mapObject(_.remove("id")))) -> "review-plan-selection-incomplete",
        _json.mapObject(_.add("targetReference", target.mapObject(_.remove("revision")))) -> "review-plan-selection-incomplete"
      ) ++ revisions.map { revision =>
        _json.mapObject(_.add("targetReference", target.mapObject(_.add("revision", revision)))) -> "invalid-reuse-target"
      }

      When("the production validator and closed decoder admit each malformed selection")
      val validated = cases.map { case (selection, code) => CarReviewReuseKey.validate(selection).left.toOption.map(_.code) -> code }
      val decoded = documents.map { case (document, code) => CarReviewExecutionSelectionCodec.decode(document.noSpaces).left.toOption.map(_.code) -> code }

      Then("every refusal keeps its cause and no required declaration is inferred")
      validated.foreach { case (actual, code) => actual shouldBe Some(code) }
      decoded.foreach { case (actual, code) => actual shouldBe Some(code) }
    }

    "refuse independently changed typed bindings at the same retained selection" in {
      Given("a complete selection and independent target, profile, baseline, rule, provider, evidence and policy changes")
      val input = _input
      val changedrule = input.ruleSets.head.copy(version = ReviewVersion("2.0"))
      val changes = Vector(
        input.copy(targetReference = input.targetReference.copy(id = "target:other")),
        input.copy(targetReference = input.targetReference.copy(revision = 4L)),
        input.copy(profile = ReviewProfile("release")),
        input.copy(baselineReference = Some(CarReviewDeclaredReference("baseline:other", 2L))),
        input.copy(ruleSets = input.ruleSets.updated(0, changedrule), providerSelections = input.providerSelections.updated(0, input.providerSelections.head.copy(ruleSet = changedrule))),
        input.copy(providerSelections = input.providerSelections.updated(0, input.providerSelections.head.copy(provider = input.providerSelections.head.provider.copy(version = ReviewVersion("2.0"))))),
        input.copy(evidenceSnapshots = input.evidenceSnapshots.updated(0, input.evidenceSnapshots.head.copy(snapshot = CarReviewDeclaredReference("evidence:other", 2L)))),
        input.copy(evidenceSnapshots = input.evidenceSnapshots.updated(0, input.evidenceSnapshots.head.copy(snapshot = input.evidenceSnapshots.head.snapshot.copy(revision = 3L)))),
        input.copy(providerSelections = input.providerSelections.updated(0, input.providerSelections.head.copy(availabilityPolicy = CarReviewDeclaredReference("availability:other", 2L)))),
        input.copy(providerSelections = input.providerSelections.updated(0, input.providerSelections.head.copy(availabilityPolicy = input.providerSelections.head.availabilityPolicy.copy(revision = 3L)))),
        input.copy(policyBindings = input.policyBindings.updated(0, input.policyBindings.head.copy(revision = 2L)))
      )

      When("production compatibility compares each changed binding against the retained selection")
      val results = changes.map(CarReviewExecutionSelectionCodec.compatible(_, input))
      val reordered = CarReviewExecutionSelectionCodec.compatible(input.copy(policyBindings = input.policyBindings.reverse), input)

      Then("each changed binding is inconsistent and order alone is compatible")
      results.foreach(_.left.toOption.map(_.code) shouldBe Some("review-plan-selection-inconsistent"))
      reordered shouldBe Right(())
      input.targetReference shouldBe CarReviewDeclaredReference("target:main", 3L)
    }

    "refuse legacy, incomplete and malformed closed persisted selections" in {
      Given("legacy documents, omitted optionals or arrays, unknown nested fields and duplicate object keys")
      val target = _json.hcursor.downField("targetReference").focus.get
      val cases = Vector(
        "" -> "review-plan-selection-incomplete",
        "{}" -> "review-plan-selection-incomplete",
        "{\"keyDefinitionId\":\"textus.cbd.review-reuse-key.v1\",\"reuseKeyDigest\":\"legacy\"}" -> "review-plan-selection-incomplete",
        _json.mapObject(_.remove("baselineReference")).noSpaces -> "review-plan-selection-incomplete",
        _json.mapObject(_.remove("evidenceSnapshots")).noSpaces -> "review-plan-selection-incomplete",
        _json.mapObject(_.add("schemaVersion", Json.fromString("textus.cbd.review-reuse-key.v1"))).noSpaces -> "unsupported-reuse-key-definition",
        _json.mapObject(_.add("unknown", Json.True)).noSpaces -> "review-plan-selection-invalid",
        _json.mapObject(_.add("targetReference", target.mapObject(_.add("digest", Json.fromString("legacy"))))).noSpaces -> "invalid-reuse-target",
        _json.mapObject(_.add("policyBindings", Json.fromValues(Vector(Json.obj("scope" -> Json.fromString("profile")))))).noSpaces -> "review-plan-selection-incomplete",
        _json.noSpaces.replace("\"id\":\"target:main\"", "\"id\":\"target:main\",\"id\":\"target:other\"") -> "review-plan-selection-invalid"
      )

      When("the current production decoder restores each persisted declaration")
      val results = cases.map { case (document, code) => CarReviewExecutionSelectionCodec.decode(document).left.toOption.map(_.code) -> code }

      Then("every document is refused without a legacy identity adapter or default revision")
      results.foreach { case (actual, code) => actual shouldBe Some(code) }
    }
  }

  private def _input: CarReviewReuseKeyInput = {
    val first = CarReviewReuseProviderSelection(ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
      ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")), CarReviewDeclaredReference("availability:cozy", 2L))
    val second = CarReviewReuseProviderSelection(ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")),
      ReviewRuleIdentity(ReviewRuleId("rule-runtime"), ReviewVersion("1.0")), CarReviewDeclaredReference("availability:runtime", 1L))
    CarReviewReuseKeyInput(CarReviewReuseKey.DEFINITION_ID, ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      CarReviewDeclaredReference("target:main", 3L), ReviewTargetKind("car"), Some("org.textus"), "textus-cbd-support",
      Some(ReviewVersion("0.1.0-SNAPSHOT")), ReviewProfile("development"), Some(CarReviewDeclaredReference("baseline:main", 2L)),
      Vector(first.ruleSet, second.ruleSet), Vector(first, second),
      Vector(CarReviewReuseEvidenceSnapshot("runtime", CarReviewDeclaredReference("evidence:a:b", 2L), second.provider),
        CarReviewReuseEvidenceSnapshot("runtime:a", CarReviewDeclaredReference("evidence:b", 1L), second.provider)),
      Vector("profile", "gate", "reconciliation", "suppression").map(scope => CarReviewReusePolicyBinding(scope, scope + "-default", ReviewVersion("1.0"), 1L)))
  }

  private def _json: Json =
    io.circe.parser.parse(CarReviewExecutionSelectionCodec.encode(_input).fold(error => fail(error.message), identity)).fold(error => fail(error.message), identity)
}
