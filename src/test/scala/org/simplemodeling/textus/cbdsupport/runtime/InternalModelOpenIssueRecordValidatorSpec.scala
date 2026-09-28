package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest

import scala.jdk.CollectionConverters.*

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelOpenIssueRecordValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private val _scope = InternalModelSemanticScope("component-order", "context-order", "e-usecase")
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8)
  private val _source = InternalModelSemanticSource("model-authority", "model-source", Some("catalog/model-source"), Some("revision-1"), _sha256(_model_raw))
  private val _identity_gen = Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars => s"id-${chars.mkString}-日本語-𩸽")
  private val _prose_gen = Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars => s"説明-${chars.mkString}-𩸽")

  "Internal-model open-issue record validation" should {
    "retain complete current accounts and compatible non-authority declarations" which {
      "retain OI-01 complete portable accounts without promoting a choice or canonical fact" in {
        Given("a captured package with one exact current realization, Japanese question and impact, all evidence kinds, local options, and duplicate prose")
        val realization = _realization()
        val ledger = _ledger(realization)

        _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger))) { root =>
          When("the selected open-issue artifact is admitted through the one package handoff")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("the result retains the complete typed ledger, exact basis and bytes, ordered duplicate prose, and complete selected realization")
          result.isSuccess shouldBe true
          result.toOption.map(_.ledger) shouldBe Some(ledger.copy(canonicalBytes = InternalModelOpenIssueRecordCodec.encode(ledger).toVector))
          result.toOption.map(_.realization.conditions.map(_.detail)) shouldBe Some(Vector("source limitation", "source limitation secondary"))
          result.toOption.map(_.ledger.issues.head.consideredEvidence.map(_.kind)) shouldBe Some(Vector(InternalModelOpenIssueEvidenceKind.ExternalHuman, InternalModelOpenIssueEvidenceKind.ExternalOther, InternalModelOpenIssueEvidenceKind.ProviderProposal, InternalModelOpenIssueEvidenceKind.RealizationSource))
          result.toOption.map(_.ledger.issues.head.options.map(_.optionIdentity)) shouldBe Some(Vector("option-a", "option-b"))
        }
      }

      "retain OI-02 independent issue-local attribution without inferring owners or choices" in {
        Given("two nonempty issues with independent local evidence and option collections that deliberately reuse identifiers across owners and record kinds, plus one explicit-empty account")
        val realization = _realization()
        val first = _ledger(realization).issues.head
        val second = first.copy(
          issueIdentity = "issue-second",
          ownerIdentity = None,
          affectedTargets = Vector.empty,
          consideredEvidence = first.consideredEvidence,
          options = Vector(first.options.head.copy(optionIdentity = "e-realization"), first.options.head),
          assumptions = Vector("second assumption"),
          conditions = Vector("second condition"),
          limitations = Vector("second limitation"),
          realizationConditionIds = Vector("c-limitation", "c-source-secondary")
        )
        val empty = first.copy(
          issueIdentity = "issue-empty",
          ownerIdentity = None,
          affectedTargets = Vector.empty,
          consideredEvidence = Vector.empty,
          options = Vector.empty,
          assumptions = Vector.empty,
          conditions = Vector.empty,
          limitations = Vector.empty,
          realizationConditionIds = Vector("c-limitation", "c-source-secondary")
        )
        val ledger = _ledger(realization).copy(issues = Vector(second, empty, first))

        _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger))) { root =>
          When("the two locally scoped records are admitted")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("each issue retains its own nullable owner, reused local identities, explicit empties, and no inferred choice")
          result.toOption.map(_.ledger.issues.map(issue => (issue.issueIdentity, issue.ownerIdentity, issue.options.map(_.optionIdentity), issue.consideredEvidence.map(_.evidenceIdentity)))) shouldBe Some(Vector(
            ("issue-current", Some("architect-1"), Vector("option-a", "option-b"), Vector("e-external", "e-other", "e-provider", "e-realization")),
            ("issue-empty", None, Vector.empty, Vector.empty),
            ("issue-second", None, Vector("e-realization", "option-a"), Vector("e-external", "e-other", "e-provider", "e-realization"))
          ))
        }
      }

      "admit OI-03 only explicitly empty ledgers with the exact current root basis" in {
        Given("an explicit empty issue list and variants with absent artifacts or changed root scope, artifact, identity, and digest")
        val realization = _realization()
        val empty = _ledger(realization).copy(issues = Vector.empty)

        _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(empty))) { root =>
          When("an empty current ledger is admitted")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("it means only that no questions were recorded in the exact selected artifact")
          result.toOption.map(_.ledger.issues) shouldBe Some(Vector.empty)
        }
        Vector(
          "scope" -> empty.copy(scope = empty.scope.copy(componentIdentity = "other-component")),
          "artifact" -> empty.copy(basis = empty.basis.copy(realizationArtifactId = "other-realization")),
          "identity" -> empty.copy(basis = empty.basis.copy(realizationIdentity = "old-realization")),
          "digest" -> empty.copy(basis = empty.basis.copy(sha256 = "sha256:" + ("f" * 64)))
        ).foreach { case (row, ledger) =>
          _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger))) { root =>
            When("one empty-ledger current-basis value differs from the selected realization")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("admission rejects rather than rebinding the empty ledger")
            withClue(row) { result.isSuccess shouldBe false }
          }
        }
        _with_fixture(realization, None) { root =>
          When("the optional open-issue artifact is absent")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("absence is not interpreted as an admitted empty ledger")
          result.isSuccess shouldBe false
        }
      }

      "preserve OI-04 exact target and scope-wide condition visibility" in {
        Given("targeted and scope-wide issues with the full selected realization, including non-issue limitations")
        val realization = _realization()
        val targeted = _ledger(realization).issues.head
        val scopewide = targeted.copy(issueIdentity = "issue-scope", affectedTargets = Vector.empty)
        val conditionfree = targeted.copy(
          issueIdentity = "issue-element",
          affectedTargets = Vector(InternalModelSemanticTarget("element", "e-customer")),
          consideredEvidence = Vector.empty,
          options = Vector.empty,
          realizationConditionIds = Vector.empty
        )
        val ledger = _ledger(realization).copy(issues = Vector(targeted, scopewide, conditionfree))

        _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger))) { root =>
          When("the records retain all targeted, source-owned, and scope-wide condition identifiers")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("admission preserves the complete realization and allows known additional condition identifiers")
          result.isSuccess shouldBe true
          result.toOption.map(_.realization.sourceReferences.map(_.referenceId)) shouldBe Some(Vector("ref-customer", "ref-relationship", "ref-usecase"))
          result.toOption.map(_.realization.conditions.map(_.detail)) shouldBe Some(Vector("source limitation", "source limitation secondary"))
        }
        val hidden = targeted.copy(realizationConditionIds = Vector.empty)
        val unknown = targeted.copy(realizationConditionIds = Vector("c-unknown"))
        Vector("hidden target condition" -> hidden, "unknown condition" -> unknown).foreach { case (row, issue) =>
          _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(_ledger(realization).copy(issues = Vector(issue)))) ) { root =>
            When("a required or known realization-condition reference is hidden or replaced")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("admission fails without making a targeted question less visible")
            withClue(row) { result.isSuccess shouldBe false }
          }
        }
      }

      "retain OI-05 all sixteen independent blocking declarations as non-authority metadata" in {
        Given("every tuple of the four required blocking declarations in an otherwise complete current account")
        val realization = _realization()
        val base = _ledger(realization)

        (0 until 16).foreach { bits =>
          val blocking = InternalModelOpenIssueBlocking((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0)
          val ledger = base.copy(issues = Vector(base.issues.head.copy(blocking = blocking)))
          _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger))) { root =>
            When("one explicit Boolean tuple is decoded and admitted")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("the exact tuple round-trips without a readiness, approval, or permission result")
            result.toOption.map(_.ledger.issues.head.blocking) shouldBe Some(blocking)
          }
        }
      }

      "preserve OI-06 realization-version and manifest-path compatibility" in {
        Given("portable V1 and V2 selected realizations and conventional or safe nonconventional open-issue paths")
        forAll(Gen.oneOf(false, true), Gen.oneOf("open-issues.yaml", "records/current-issues.json")) { (v2, path) =>
          val realization = _realization(v2)
          val ledger = _ledger(realization)

          _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger)), openpath = path) { root =>
            When("the current artifact is admitted from its manifest-listed path")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("the compatible realization profile and its unchanged source content remain retained")
            result.toOption.map(_.realization.profile) shouldBe Some(if v2 then "ccdm-realization-v2" else "ccdm-realization-v1")
          }
        }
      }
    }

    "reject closed-grammar and semantic-authority violations" which {
      "reject OI-07 closed object, scalar, nullable, and enum grammar mutations" in {
        Given("one canonical account and independent missing, unknown, scalar, profile, version, state, kind, and nullable-field mutations")
        val base = _ledger(_realization())
        val handoff = _open_issue_handoff(base)
        val malformed = Vector(
          "missing root issues" -> _canonical(_update_root(base, _.remove("issues"))),
          "unknown root selected" -> _canonical(_update_root(base, _.add("selected", Json.fromBoolean(true)))),
          "missing issue question" -> _canonical(_update_issue(base, _.remove("question"))),
          "unknown issue score" -> _canonical(_update_issue(base, _.add("score", Json.fromInt(1)))),
          "unsupported profile" -> InternalModelOpenIssueRecordCodec.encode(base.copy(profile = "unsupported")),
          "unsupported schema" -> InternalModelOpenIssueRecordCodec.encode(base.copy(schemaVersion = "2.0")),
          "blank ledger identity" -> InternalModelOpenIssueRecordCodec.encode(base.copy(ledgerIdentity = " ")),
          "blank question" -> InternalModelOpenIssueRecordCodec.encode(base.copy(issues = Vector(base.issues.head.copy(question = "\t")))),
          "unsupported state" -> _canonical(_update_issue(base, _.add("state", Json.fromString("resolved"))))
        )
        malformed.foreach { case (row, bytes) =>
          When("a closed grammar member, scalar, profile, state, or deferred-resolution field is invalid")
          val result = InternalModelOpenIssueRecordCodec.decode(handoff.copy(bytes = bytes.toVector))

          Then("the codec rejects without prose interpretation or schema fallback")
          withClue(row) { result.isLeft shouldBe true }
        }
        val nullowner = base.copy(issues = Vector(base.issues.head.copy(ownerIdentity = None)))
        When("the required nullable owner and source navigation keys are explicitly null")
        val accepted = InternalModelOpenIssueRecordCodec.decode(_open_issue_handoff(nullowner))

        Then("the codec retains typed absence while omission remains invalid")
        accepted.toOption.flatMap(_.issues.head.ownerIdentity) shouldBe None
        accepted.toOption.map(_.issues.head.consideredEvidence.head).map(evidence => (evidence.source.locator, evidence.source.revision, evidence.sourceReferenceId)) shouldBe Some((None, None, None))
        InternalModelOpenIssueRecordCodec.decode(handoff.copy(bytes = _canonical(_update_issue(base, _.remove("ownerIdentity"))).toVector)).isLeft shouldBe true

        val closedboundaries = Vector(
          _canonical(_update_root(base, _.remove("profile"))), _canonical(_update_root(base, _.add("unknown", Json.True))), _canonical(_update_root(base, _.add("scope", Json.True))),
          _canonical(_update_scope(base, _.remove("componentIdentity"))), _canonical(_update_scope(base, _.add("unknown", Json.True))), _canonical(_update_scope(base, _.add("componentIdentity", Json.True))),
          _canonical(_update_basis(base, _.remove("sha256"))), _canonical(_update_basis(base, _.add("unknown", Json.True))), _canonical(_update_basis(base, _.add("realizationIdentity", Json.True))),
          _canonical(_update_issue(base, _.remove("impact"))), _canonical(_update_issue(base, _.add("unknown", Json.True))), _canonical(_update_issue(base, _.add("question", Json.True))),
          _canonical(_update_target(base, _.remove("semanticIdentity"))), _canonical(_update_target(base, _.add("unknown", Json.True))), _canonical(_update_target(base, _.add("semanticIdentityKind", Json.True))),
          _canonical(_update_evidence(base, 3, _.remove("kind"))), _canonical(_update_evidence(base, 3, _.add("unknown", Json.True))), _canonical(_update_evidence(base, 3, _.add("kind", Json.True))), _canonical(_update_evidence(base, 3, _.add("kind", Json.fromString("other")))),
          _canonical(_update_evidence_source(base, 3, _.remove("identity"))), _canonical(_update_evidence_source(base, 3, _.add("unknown", Json.True))), _canonical(_update_evidence_source(base, 3, _.add("authority", Json.True))), _canonical(_update_evidence_source(base, 3, _.add("sha256", Json.fromString("sha256:ABC")))),
          _canonical(_update_option(base, _.remove("description"))), _canonical(_update_option(base, _.add("unknown", Json.True))), _canonical(_update_option(base, _.add("description", Json.True))),
          _canonical(_update_blocking(base, _.remove("validation"))), _canonical(_update_blocking(base, _.add("unknown", Json.True))), _canonical(_update_blocking(base, _.add("validation", Json.fromString("false"))))
        )
        closedboundaries.zipWithIndex.foreach { case (bytes, index) =>
          When("a required, unknown, or wrong-type member is supplied at a root, scope, basis, issue, target, evidence, source, option, or blocking boundary")
          val result = InternalModelOpenIssueRecordCodec.decode(handoff.copy(bytes = bytes.toVector))

          Then("the nine closed object boundaries reject the mutation")
          withClue(s"closed boundary row $index") { result.isLeft shouldBe true }
        }
        val scalarinvalid = Vector(
          base.copy(ledgerIdentity = " "),
          base.copy(basis = base.basis.copy(sha256 = "sha256:ABC")),
          base.copy(issues = Vector(base.issues.head.copy(issueIdentity = ""))),
          base.copy(issues = Vector(base.issues.head.copy(question = " "))),
          base.copy(issues = Vector(base.issues.head.copy(decisionRole = "\t"))),
          base.copy(issues = Vector(base.issues.head.copy(ownerIdentity = Some(" ")))),
          base.copy(issues = Vector(base.issues.head.copy(impact = ""))),
          base.copy(issues = Vector(base.issues.head.copy(affectedTargets = Vector(InternalModelSemanticTarget("other", "target"))))),
          base.copy(issues = Vector(base.issues.head.copy(options = Vector(base.issues.head.options.head.copy(description = "")))))
        )
        scalarinvalid.zipWithIndex.foreach { case (ledger, index) =>
          When("a required identity, question, role, owner claim, impact, target kind, option description, or digest is blank or invalid")
          val result = InternalModelOpenIssueRecordCodec.decode(_open_issue_handoff(ledger))

          Then("the codec fails closed without text normalization")
          withClue(s"scalar row $index") { result.isLeft shouldBe true }
        }
        val exhaustiverows = Vector(
          "blank scope component" -> _canonical(_update_scope(base, _.add("componentIdentity", Json.fromString("")))),
          "blank scope context" -> _canonical(_update_scope(base, _.add("projectionContextIdentity", Json.fromString(" ")))),
          "blank scope use case" -> _canonical(_update_scope(base, _.add("selectedUseCaseElementIdentity", Json.fromString("\t")))),
          "blank basis artifact" -> _canonical(_update_basis(base, _.add("realizationArtifactId", Json.fromString("")))),
          "blank basis realization" -> _canonical(_update_basis(base, _.add("realizationIdentity", Json.fromString(" ")))),
          "blank target identity" -> _canonical(_update_target(base, _.add("semanticIdentity", Json.fromString("")))),
          "blank evidence identity" -> _canonical(_update_evidence(base, 3, _.add("evidenceIdentity", Json.fromString("")))),
          "blank option identity" -> _canonical(_update_option(base, _.add("optionIdentity", Json.fromString("")))),
          "blank evidence authority" -> _canonical(_update_evidence_source(base, 3, _.add("authority", Json.fromString(" ")))),
          "blank evidence source identity" -> _canonical(_update_evidence_source(base, 3, _.add("identity", Json.fromString("")))),
          "blank source locator" -> _canonical(_update_evidence_source(base, 3, _.add("locator", Json.fromString(" ")))),
          "blank source revision" -> _canonical(_update_evidence_source(base, 3, _.add("revision", Json.fromString("\t")))),
          "missing source locator" -> _canonical(_update_evidence_source(base, 3, _.remove("locator"))),
          "missing source revision" -> _canonical(_update_evidence_source(base, 3, _.remove("revision"))),
          "blank source reference" -> _canonical(_update_evidence(base, 3, _.add("sourceReferenceId", Json.fromString(" ")))),
          "missing source reference" -> _canonical(_update_evidence(base, 3, _.remove("sourceReferenceId"))),
          "blank issue prose" -> _canonical(_update_issue(base, _.add("assumptions", Json.arr(Json.fromString(" "))))),
          "blank evidence prose" -> _canonical(_update_evidence(base, 3, _.add("conditions", Json.arr(Json.fromString("\t"))))),
          "blank option prose" -> _canonical(_update_option(base, _.add("limitations", Json.arr(Json.fromString(""))))),
          "root non-object" -> _canonical(Json.True),
          "scope non-object" -> _canonical(_update_root(base, _.add("scope", Json.True))),
          "basis non-object" -> _canonical(_update_root(base, _.add("basis", Json.True))),
          "issues non-array" -> _canonical(_update_root(base, _.add("issues", Json.True))),
          "target non-object" -> _canonical(_update_issue(base, _.add("affectedTargets", Json.arr(Json.True)))),
          "evidence non-object" -> _canonical(_update_issue(base, _.add("consideredEvidence", Json.arr(Json.True)))),
          "source non-object" -> _canonical(_update_evidence(base, 3, _.add("source", Json.True))),
          "option non-object" -> _canonical(_update_issue(base, _.add("options", Json.arr(Json.True)))),
          "blocking non-object" -> _canonical(_update_issue(base, _.add("blocking", Json.True))),
          "deferred selected issue" -> _canonical(_update_issue(base, _.add("selected", Json.True))),
          "deferred resolved issue" -> _canonical(_update_issue(base, _.add("resolved", Json.True))),
          "deferred history issue" -> _canonical(_update_issue(base, _.add("history", Json.arr()))),
          "deferred ranked option" -> _canonical(_update_option(base, _.add("rank", Json.fromInt(1)))),
          "deferred selected option" -> _canonical(_update_option(base, _.add("selected", Json.True))),
          "deferred history option" -> _canonical(_update_option(base, _.add("history", Json.arr())))
        )
        exhaustiverows.foreach { case (row, bytes) =>
          When("a named scalar, nullable, prose, object-type, or deferred-workflow grammar row is supplied")
          val result = InternalModelOpenIssueRecordCodec.decode(handoff.copy(bytes = bytes.toVector))

          Then("the exact closed grammar row rejects without fallback or inferred lifecycle meaning")
          withClue(row) { result.isLeft shouldBe true }
        }
      }

      "reject OI-08 every non-Boolean or omitted blocking declaration" in {
        Given("the four independently required blocking keys and all invalid JSON representations for each one")
        val base = _ledger(_realization())
        val handoff = _open_issue_handoff(base)
        val invalidvalues = Vector(Json.Null, Json.fromString("true"), Json.fromString("false"), Json.fromString("unknown"), Json.fromInt(1), Json.obj("value" -> Json.True))

        Vector("semanticApproval", "cmlProjection", "application", "validation").foreach { key =>
          (invalidvalues.map(value => _canonical(_update_blocking(base, _.add(key, value)))) :+ _canonical(_update_blocking(base, _.remove(key)))).zipWithIndex.foreach { case (bytes, index) =>
            When("one required blocking declaration is omitted or represented by a non-Boolean JSON value")
            val result = InternalModelOpenIssueRecordCodec.decode(handoff.copy(bytes = bytes.toVector))

            Then("the closed codec fails without defaulting the declaration to false")
            withClue(s"$key invalid Boolean row $index") { result.isLeft shouldBe true }
          }
        }
        val duplicate = new String(InternalModelOpenIssueRecordCodec.encode(base), StandardCharsets.UTF_8)
          .replace("\"application\":true,\"cmlProjection\"", "\"application\":true,\"application\":true,\"cmlProjection\"")
          .getBytes(StandardCharsets.UTF_8)
        When("a closed blocking object repeats a Boolean member")
        val duplicateresult = InternalModelOpenIssueRecordCodec.decode(handoff.copy(bytes = duplicate.toVector))

        Then("the duplicate-key parser rejects the record without accepting either declaration")
        duplicateresult.isLeft shouldBe true
      }

      "reject OI-09 every populated and empty-ledger mismatch to the current semantic basis" in {
        Given("populated and empty ledgers with mutations to each scope, selected artifact, identity, and raw digest value")
        val realization = _realization()
        val base = _ledger(realization)
        val mutations = Vector[InternalModelOpenIssueLedger => InternalModelOpenIssueLedger](
          ledger => ledger.copy(scope = ledger.scope.copy(componentIdentity = "other-component")),
          ledger => ledger.copy(scope = ledger.scope.copy(projectionContextIdentity = "other-context")),
          ledger => ledger.copy(scope = ledger.scope.copy(selectedUseCaseElementIdentity = "other-usecase")),
          ledger => ledger.copy(basis = ledger.basis.copy(realizationArtifactId = "other-artifact")),
          ledger => ledger.copy(basis = ledger.basis.copy(realizationIdentity = "other-realization")),
          ledger => ledger.copy(basis = ledger.basis.copy(sha256 = "sha256:" + ("0" * 64)))
        )

        Vector("populated" -> base, "empty" -> base.copy(issues = Vector.empty)).foreach { case (kind, ledger) =>
          mutations.zipWithIndex.foreach { case (mutate, index) =>
            _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(mutate(ledger)))) { root =>
              When("one root current-basis value differs from the captured realization")
              val result = InternalModelOpenIssueRecordValidator.validate(root)

              Then("admission rejects without historical bypass or rebinding")
              withClue(s"$kind current-basis row $index") { result.isSuccess shouldBe false }
            }
          }
        }
      }

      "reject OI-10 unknown source, target, and required-condition authority changes" in {
        Given("a complete current record and mutations to targets, source metadata, source conditions, and evidence authority")
        val realization = _realization()
        val base = _ledger(realization)
        val issue = base.issues.head
        val changedsource = issue.consideredEvidence.map { evidence =>
          if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(identity = "other-source")) else evidence
        }
        val sourcefieldmutations = Vector(
          issue.consideredEvidence.map(evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(authority = "other-authority")) else evidence),
          changedsource,
          issue.consideredEvidence.map(evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(locator = Some("other-locator"))) else evidence),
          issue.consideredEvidence.map(evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(revision = Some("other-revision"))) else evidence),
          issue.consideredEvidence.map(evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(sha256 = "sha256:" + ("e" * 64))) else evidence)
        )
        val mutations = Vector(
          base.copy(issues = Vector(issue.copy(affectedTargets = Vector(InternalModelSemanticTarget("element", "e-unknown"))))),
          base.copy(issues = Vector(issue.copy(affectedTargets = Vector(InternalModelSemanticTarget("relationship", "r-unknown"))))),
          base.copy(issues = Vector(issue.copy(consideredEvidence = changedsource))),
          base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence.map { evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(sourceReferenceId = Some("ref-unknown")) else evidence }))),
          base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence.map { evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(conditionIds = Vector("c-limitation")) else evidence }))),
          base.copy(issues = Vector(issue.copy(realizationConditionIds = Vector("c-limitation")))),
          base.copy(issues = Vector(issue.copy(affectedTargets = Vector(InternalModelSemanticTarget("element", "e-customer")), realizationConditionIds = Vector("c-limitation")))) ,
          base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence.map { evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(conditionIds = Vector("c-extra", "c-limitation", "c-source-secondary")) else evidence })))
        ) ++ sourcefieldmutations.map(evidence => base.copy(issues = Vector(issue.copy(consideredEvidence = evidence))))
        mutations.zipWithIndex.foreach { case (ledger, index) =>
          _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger))) { root =>
            When("a source, target, or complete required condition reference no longer matches the admitted realization")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("admission fails without promoting external authority or hiding the condition")
            withClue(s"source-target-condition row $index") { result.isSuccess shouldBe false }
          }
        }
        val externalcanonical = base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence.map { evidence =>
          if evidence.kind == InternalModelOpenIssueEvidenceKind.ExternalHuman then evidence.copy(sourceReferenceId = Some("ref-relationship"), conditionIds = Vector("c-limitation")) else evidence
        })))
        val scopewidehidden = base.copy(issues = Vector(issue.copy(affectedTargets = Vector.empty, consideredEvidence = Vector.empty, options = Vector.empty, realizationConditionIds = Vector("c-limitation"))))
        Vector("external canonical link" -> externalcanonical, "scope-wide hidden condition" -> scopewidehidden).foreach { case (row, ledger) =>
          _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(ledger))) { root =>
            When("external evidence claims a canonical link or a scope-wide issue hides a selected realization condition")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("admission rejects the authority violation without external promotion or condition omission")
            withClue(row) { result.isSuccess shouldBe false }
          }
        }
        Vector(InternalModelOpenIssueEvidenceKind.ExternalHuman, InternalModelOpenIssueEvidenceKind.ProviderProposal, InternalModelOpenIssueEvidenceKind.ExternalOther).flatMap { kind =>
          Vector(
            s"${kind.wireValue} source reference only" -> base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence.map(evidence => if evidence.kind == kind then evidence.copy(sourceReferenceId = Some("ref-relationship")) else evidence)))),
            s"${kind.wireValue} condition IDs only" -> base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence.map(evidence => if evidence.kind == kind then evidence.copy(conditionIds = Vector("c-limitation")) else evidence))))
          )
        }.foreach { case (row, ledger) =>
          When("an external evidence kind independently attempts one canonical source or condition link")
          val result = InternalModelOpenIssueRecordCodec.decode(_open_issue_handoff(ledger))

          Then("each external evidence kind rejects canonical-link promotion")
          withClue(row) { result.isLeft shouldBe true }
        }
      }

      "reject OI-11 duplicate owning identities and cross-issue option-evidence links" in {
        Given("a canonical two-issue account with local identities plus duplicate and dangling variants")
        val realization = _realization()
        val base = _ledger(realization)
        val issue = base.issues.head
        val duplicateissue = base.copy(issues = Vector(issue, issue))
        val duplicatetarget = base.copy(issues = Vector(issue.copy(affectedTargets = issue.affectedTargets :+ issue.affectedTargets.head)))
        val duplicateevidence = base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence :+ issue.consideredEvidence.head)))
        val duplicateoption = base.copy(issues = Vector(issue.copy(options = issue.options :+ issue.options.head)))
        val duplicateoptionlink = base.copy(issues = Vector(issue.copy(options = Vector(issue.options.head.copy(evidenceIds = Vector("e-external", "e-external"))))) )
        val duplicateissuecondition = base.copy(issues = Vector(issue.copy(realizationConditionIds = Vector("c-limitation", "c-limitation"))))
        val duplicateevidencecondition = base.copy(issues = Vector(issue.copy(consideredEvidence = issue.consideredEvidence.map { evidence => if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(conditionIds = Vector("c-limitation", "c-limitation")) else evidence })))
        val danglinglink = base.copy(issues = Vector(issue.copy(options = Vector(issue.options.head.copy(evidenceIds = Vector("e-absent"))))))

        Vector("issue" -> duplicateissue, "target" -> duplicatetarget, "evidence" -> duplicateevidence, "option" -> duplicateoption, "option evidence" -> duplicateoptionlink, "issue condition" -> duplicateissuecondition, "evidence condition" -> duplicateevidencecondition).foreach { case (row, ledger) =>
          When("an issue, target, evidence, option, option-evidence, issue-condition, or evidence-condition identity repeats inside its owning collection")
          val result = InternalModelOpenIssueRecordCodec.decode(_open_issue_handoff(ledger))

          Then("the codec rejects the duplicate rather than selecting by order")
          withClue(s"duplicate $row") { result.isLeft shouldBe true }
        }
        _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(danglinglink))) { root =>
          When("an option points only to an evidence identity outside its owning issue")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("admission fails without using another issue or provider option as a choice")
          result.isSuccess shouldBe false
        }
        val crossissueevidence = issue.consideredEvidence.head.copy(evidenceIdentity = "e-cross")
        val second = issue.copy(issueIdentity = "issue-second", affectedTargets = Vector.empty, consideredEvidence = Vector(crossissueevidence), options = Vector.empty, realizationConditionIds = Vector("c-limitation", "c-source-secondary"))
        val crossissueonly = base.copy(issues = Vector(issue.copy(options = Vector(issue.options.head.copy(evidenceIds = Vector("e-cross")))), second))
        _with_fixture(realization, Some(InternalModelOpenIssueRecordCodec.encode(crossissueonly))) { root =>
          When("an option references an evidence ID present only in another issue")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("the owning issue boundary rejects the cross-issue-only link")
          result.isSuccess shouldBe false
        }
      }

      "reject OI-12 noncanonical bytes, duplicate members, and unsorted raw identity arrays" in {
        Given("one canonical account with byte, object-order, duplicate-member, and each identity-array raw-order mutation")
        val base = _ledger(_realization()).copy(issues = Vector(_ledger(_realization()).issues.head.copy(issueIdentity = "issue-a"), _ledger(_realization()).issues.head.copy(issueIdentity = "issue-b")))
        val canonical = InternalModelOpenIssueRecordCodec.encode(base)
        val handoff = _open_issue_handoff(base)
        val canonicaltext = new String(canonical, StandardCharsets.UTF_8)
        val rawmutations = Vector(
          Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical,
          Array[Byte](0xc3.toByte, 0x28.toByte),
          "{\"issues\":[],\"issues\":[]}\n".getBytes(StandardCharsets.UTF_8),
          canonicaltext.replace("\"ledgerIdentity\":\"ledger-order\",", "\"ledgerIdentity\":\"ledger-order\",\"ledgerIdentity\":\"ledger-order\",").getBytes(StandardCharsets.UTF_8),
          canonicaltext.replace("\"state\":\"open\"", "\"state\":\"\\u006fpen\"").getBytes(StandardCharsets.UTF_8),
          canonicaltext.replace("\"profile\":\"ccdm-open-issue-records-v1\",\"schemaVersion\":\"1.0\"", "\"schemaVersion\":\"1.0\",\"profile\":\"ccdm-open-issue-records-v1\"").getBytes(StandardCharsets.UTF_8),
          canonicaltext.replace("\"question\":\"利用者の関係は一意か\",", "\"question\":\"利用者の関係は一意か\",\"question\":\"利用者の関係は一意か\",").getBytes(StandardCharsets.UTF_8),
          canonicaltext.replace("\"question\":\"利用者の関係は一意か\"", "\"question\":\"\\ud800\"").getBytes(StandardCharsets.UTF_8),
          canonical.dropRight(1),
          canonical ++ "\n".getBytes(StandardCharsets.UTF_8),
          canonical ++ "x".getBytes(StandardCharsets.UTF_8),
          canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8)
        ) ++ _unsorted_identity_mutations(base)
        rawmutations.zipWithIndex.foreach { case (bytes, index) =>
          When("a noncanonical byte form, duplicate member, or unsorted identity collection is supplied directly")
          val result = InternalModelOpenIssueRecordCodec.decode(handoff.copy(bytes = bytes.toVector))

          Then("the decoder fails closed and never sorts, repairs, or deduplicates supplied bytes")
          withClue(s"noncanonical byte row $index") { result.isLeft shouldBe true }
        }
      }
    }

    "preserve captured-package continuity and structural compatibility" which {
      "preserve OI-13 structural compatibility while proving captured-handoff continuity" in {
        Given("optional-absent, duplicate, dependency-invalid, tampered, and already captured package fixtures")
        val realization = _realization()
        val issue = InternalModelOpenIssueRecordCodec.encode(_ledger(realization))

        _with_fixture(realization, None, includeabsentissue = false) { root =>
          When("the V1 inventory contains no open-issue entry")
          val structural = InternalModelPackageValidator.validateStructure(root)
          val admission = InternalModelOpenIssueRecordValidator.validate(root)

          Then("unchanged structure remains valid while issue admission is unavailable")
          structural.isSuccess shouldBe true
          admission.isSuccess shouldBe false
        }
        _with_fixture(realization, Some(issue), openissuesdependencies = Vector("snapshot-model")) { root =>
          When("the selected issue has no direct dependency on the selected realization")
          val structural = InternalModelPackageValidator.validateStructure(root)
          val admission = InternalModelOpenIssueRecordValidator.validate(root)

          Then("structural role compatibility remains unchanged while the issue handoff fails")
          structural.isSuccess shouldBe true
          admission.isSuccess shouldBe false
        }
        _with_fixture(realization, Some(issue)) { root =>
          Given("an issue handoff captured before its issue, realization, snapshot, and manifest files are removed")
          val handoff = InternalModelPackageValidator.verifiedOpenIssueRecords(root)
          Files.delete(root.resolve("src/main/internal-model/open-issues.yaml"))
          Files.delete(root.resolve("src/main/internal-model/realizations/main.json"))
          Files.delete(root.resolve("src/main/internal-model/snapshots/model.json"))
          Files.delete(root.resolve("src/main/internal-model/manifest.yaml"))

          When("the captured handoff and a fresh package admission are requested")
          val captured = handoff.flatMap(InternalModelOpenIssueRecordValidator.validateVerified)
          val fresh = InternalModelOpenIssueRecordValidator.validate(root)

          Then("the captured immutable handoff remains sufficient while a fresh reader fails")
          captured.isSuccess shouldBe true
          fresh.isSuccess shouldBe false
        }
        Vector("open-issues.yaml", "realizations/main.json").foreach { relative =>
          _with_fixture(realization, Some(issue)) { root =>
            Files.delete(root.resolve("src/main/internal-model").resolve(relative))

            When("the required issue artifact or selected realization artifact is missing")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("fresh admission fails rather than treating the missing artifact as an empty or substituted selection")
            withClue(relative) { result.isSuccess shouldBe false }
          }
        }
        Vector(
          ("open-issue-main", Vector("missing-artifact")),
          ("snapshot-model", Vector("open-issue-main"))
        ).foreach { case (artifactid, dependencies) =>
          _with_fixture(realization, Some(issue)) { root =>
            _rewrite_manifest_artifact(root.resolve("src/main/internal-model/manifest.yaml"), artifactid, _.add("dependsOn", Json.fromValues(dependencies.map(Json.fromString))))

            When("the verified manifest has an unresolved dependency or a cycle while retaining a valid package digest")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("fresh admission fails at the closed package dependency boundary")
            withClue(s"$artifactid -> ${dependencies.mkString(",")}") { result.isSuccess shouldBe false }
          }
        }
        _with_fixture(realization, Some(issue)) { root =>
          Given("a valid captured handoff before the issue, realization, snapshot, and manifest files are substituted")
          val handoff = InternalModelPackageValidator.verifiedOpenIssueRecords(root)
          Vector("open-issues.yaml", "realizations/main.json", "snapshots/model.json", "manifest.yaml").foreach { relative =>
            Files.write(root.resolve("src/main/internal-model").resolve(relative), "substituted\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)
          }

          When("captured admission and a fresh package read occur after substitution")
          val captured = handoff.flatMap(InternalModelOpenIssueRecordValidator.validateVerified)
          val fresh = InternalModelOpenIssueRecordValidator.validate(root)

          Then("only the captured authority admits; the fresh reader rejects the substituted package")
          captured.isSuccess shouldBe true
          fresh.isSuccess shouldBe false
        }
        _with_fixture(realization, Some(issue), additionalissue = Some(issue)) { root =>
          When("two present open-issue artifacts are structurally listed")
          val structural = InternalModelPackageValidator.validateStructure(root)
          val admission = InternalModelOpenIssueRecordValidator.validate(root)

          Then("unchanged structural multiplicity remains valid while issue selection is ambiguous")
          structural.isSuccess shouldBe true
          admission.isSuccess shouldBe false
        }
        _with_fixture(realization, Some(issue), additionalrealization = Some(realization)) { root =>
          When("two present realization artifacts are structurally listed")
          val structural = InternalModelPackageValidator.validateStructure(root)
          val admission = InternalModelOpenIssueRecordValidator.validate(root)

          Then("unchanged structural multiplicity remains valid while the selected current realization is ambiguous")
          structural.isSuccess shouldBe true
          admission.isSuccess shouldBe false
        }
        Vector("open-issues.yaml", "realizations/main.json", "snapshots/model.json", "manifest.yaml").foreach { relative =>
          _with_fixture(realization, Some(issue)) { root =>
            Files.write(root.resolve("src/main/internal-model").resolve(relative), "tampered\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)

            When("one manifest-verified issue, realization, snapshot, or manifest byte sequence is tampered")
            val result = InternalModelOpenIssueRecordValidator.validate(root)

            Then("fresh admission fails before accepting substituted authority bytes")
            withClue(relative) { result.isSuccess shouldBe false }
          }
        }
        _with_fixture(realization, Some(issue)) { root =>
          val path = root.resolve("src/main/internal-model/manifest.yaml")
          val changed = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
            .replace("\"lifecycleState\":\"draft\"", "\"lifecycleState\":\"changed\"")
            .getBytes(StandardCharsets.UTF_8)
          Files.write(path, changed, StandardOpenOption.TRUNCATE_EXISTING)

          When("one canonical JSON manifest field changes without recomputing packageDigest")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("fresh admission rejects the valid-JSON manifest digest mismatch")
          result.isSuccess shouldBe false
        }
        _with_fixture(realization, Some(issue)) { root =>
          _write(root.resolve("src/main/internal-model/unlisted.json"), "unlisted\n".getBytes(StandardCharsets.UTF_8))

          When("an unlisted package file is introduced after manifest construction")
          val result = InternalModelOpenIssueRecordValidator.validate(root)

          Then("fresh admission preserves the closed package inventory boundary")
          result.isSuccess shouldBe false
        }
      }
    }

    "canonicalize identities while preserving Unicode prose" which {
      "canonicalize OI-14 identity permutations while preserving Unicode prose and no-choice semantics" in {
        Given("generated collision-free ASCII, Japanese, supplementary, and UTF-8-versus-UTF-16 edge identities with every nested identity collection permuted")
        val realization = _realization()
        val base = _ledger(realization)
        val propertyinput = for {
          identity <- _identity_gen
          prose <- _prose_gen
          issueorder <- _permutation(Vector(0, 1))
          targetorder <- _permutation(Vector(0, 1))
          evidenceorder <- _permutation(Vector(0, 1, 2, 3))
          optionorder <- _permutation(Vector(0, 1))
          evidenceidorder <- _permutation(Vector(0, 1))
          conditionorder <- _permutation(Vector(0, 1))
          evidenceconditionorder <- _permutation(Vector(0, 1))
        } yield (identity, prose, issueorder, targetorder, evidenceorder, optionorder, evidenceidorder, conditionorder, evidenceconditionorder)

        forAll(propertyinput) { case (identity, prose, issueorder, targetorder, evidenceorder, optionorder, evidenceidorder, conditionorder, evidenceconditionorder) =>
          val edgesupplementary = "edge-𐀀"
          val edgeprivateuse = "edge-"
          val sourceevidence = base.issues.head.consideredEvidence.map { evidence =>
            if evidence.kind == InternalModelOpenIssueEvidenceKind.RealizationSource then evidence.copy(conditionIds = evidenceconditionorder.map(index => Vector("c-limitation", "c-source-secondary")(index)))
            else if evidence.evidenceIdentity == "e-external" then evidence.copy(evidenceIdentity = edgesupplementary)
            else if evidence.evidenceIdentity == "e-other" then evidence.copy(evidenceIdentity = edgeprivateuse)
            else evidence
          }
          val record = base.issues.head.copy(
            issueIdentity = s"issue-$identity-a",
            question = prose,
            affectedTargets = targetorder.map(index => base.issues.head.affectedTargets(index)),
            consideredEvidence = evidenceorder.map(index => sourceevidence(index)),
            options = optionorder.map(index => base.issues.head.options(index).copy(evidenceIds = if index == 0 then evidenceidorder.map(number => Vector(edgesupplementary, "e-realization")(number)) else Vector("e-provider"))),
            assumptions = Vector(prose, "duplicate", "duplicate"),
            conditions = Vector("condition", prose, "condition"),
            limitations = Vector(prose, prose),
            realizationConditionIds = conditionorder.map(index => Vector("c-limitation", "c-source-secondary")(index))
          )
          val second = record.copy(issueIdentity = s"issue-$identity-b", ownerIdentity = None)
          val permuted = issueorder.map(index => Vector(record, second)(index))
          val reference = record.copy(
            affectedTargets = Vector(InternalModelSemanticTarget("element", "e-customer"), InternalModelSemanticTarget("relationship", "r-uses")),
            consideredEvidence = Vector(sourceevidence(2), sourceevidence(3).copy(conditionIds = Vector("c-limitation", "c-source-secondary")), sourceevidence(1), sourceevidence(0).copy(conditionIds = Vector.empty)),
            options = Vector(record.options.find(_.optionIdentity == "option-a").getOrElse(record.options.head).copy(evidenceIds = Vector("e-realization", edgesupplementary)), record.options.find(_.optionIdentity == "option-b").getOrElse(record.options.last).copy(evidenceIds = Vector("e-provider"))),
            realizationConditionIds = Vector("c-limitation", "c-source-secondary")
          )
          val referencesecond = reference.copy(issueIdentity = s"issue-$identity-b", ownerIdentity = None)

          When("the pure codec encodes every one of the seven identity arrays in generated nested permutations")
          val encoded = InternalModelOpenIssueRecordCodec.encode(base.copy(issues = permuted))
          val decoded = InternalModelOpenIssueRecordCodec.decode(_open_issue_handoff(base.copy(issues = permuted)).copy(bytes = encoded.toVector))
          val referencebytes = InternalModelOpenIssueRecordCodec.encode(base.copy(issues = Vector(reference, referencesecond)))
          val changedprose = record.copy(question = s"changed-$prose", assumptions = Vector(s"changed-$prose", "duplicate", "duplicate"), conditions = Vector("condition", s"changed-$prose", "condition"), limitations = Vector(s"changed-$prose", s"changed-$prose"))
          val changeddecoded = InternalModelOpenIssueRecordCodec.decode(_open_issue_handoff(base.copy(issues = Vector(changedprose, changedprose.copy(issueIdentity = s"issue-$identity-b", ownerIdentity = None)))) )

          Then("canonical bytes equal the independent canonical reference; edge identities and changed prose retain exact values without a winner, owner, or authority inference")
          encoded.toVector shouldBe referencebytes.toVector
          decoded.isRight shouldBe true
          decoded.toOption.map(_.issues.map(_.issueIdentity)) shouldBe Some(Vector(s"issue-$identity-a", s"issue-$identity-b"))
          decoded.toOption.map(_.issues.head.assumptions) shouldBe Some(Vector(prose, "duplicate", "duplicate"))
          decoded.toOption.map(_.issues.head.options.map(_.optionIdentity)) shouldBe Some(Vector("option-a", "option-b"))
          decoded.toOption.map(_.issues.head.consideredEvidence.map(_.evidenceIdentity)) shouldBe Some(Vector("e-provider", "e-realization", edgeprivateuse, edgesupplementary))
          changeddecoded.toOption.map(_.issues.map(issue => (issue.issueIdentity, issue.question, issue.assumptions, issue.conditions, issue.limitations))) shouldBe Some(Vector((s"issue-$identity-a", s"changed-$prose", Vector(s"changed-$prose", "duplicate", "duplicate"), Vector("condition", s"changed-$prose", "condition"), Vector(s"changed-$prose", s"changed-$prose")), (s"issue-$identity-b", s"changed-$prose", Vector(s"changed-$prose", "duplicate", "duplicate"), Vector("condition", s"changed-$prose", "condition"), Vector(s"changed-$prose", s"changed-$prose"))))
        }
      }
    }
  }

  private def _ledger(realization: Array[Byte]): InternalModelOpenIssueLedger = {
    val evidence = Vector(
      InternalModelOpenIssueEvidence("e-external", InternalModelOpenIssueEvidenceKind.ExternalHuman, InternalModelSemanticSource("external-human", "external-1", None, None, "sha256:" + ("3" * 64)), None, Vector.empty, Vector("external condition"), Vector("external limitation")),
      InternalModelOpenIssueEvidence("e-other", InternalModelOpenIssueEvidenceKind.ExternalOther, InternalModelSemanticSource("external-other", "external-other-1", None, None, "sha256:" + ("5" * 64)), None, Vector.empty, Vector("other condition"), Vector("other limitation")),
      InternalModelOpenIssueEvidence("e-provider", InternalModelOpenIssueEvidenceKind.ProviderProposal, InternalModelSemanticSource("provider", "provider-1", Some("provider/result"), None, "sha256:" + ("4" * 64)), None, Vector.empty, Vector.empty, Vector("proposal limitation")),
      InternalModelOpenIssueEvidence("e-realization", InternalModelOpenIssueEvidenceKind.RealizationSource, _source, Some("ref-relationship"), Vector("c-limitation", "c-source-secondary"), Vector("source condition", "source condition duplicate", "source condition duplicate"), Vector("source limitation", "source limitation duplicate"))
    )
    val issue = InternalModelOpenIssueRecord(
      "issue-current", InternalModelOpenIssueState.Open, "利用者の関係は一意か", "component architect", Some("architect-1"), "利用者の注文経路に影響する",
      Vector(InternalModelSemanticTarget("element", "e-customer"), InternalModelSemanticTarget("relationship", "r-uses")), evidence,
      Vector(
        InternalModelOpenIssueOption("option-a", "現行の関係を維持する", Vector("e-external", "e-realization"), Vector("option assumption", "option assumption"), Vector("option condition"), Vector("option limitation")),
        InternalModelOpenIssueOption("option-b", "別の関係を検討する", Vector("e-provider"), Vector.empty, Vector.empty, Vector("provider proposal is not selected"))
      ),
      Vector("assumption retained", "assumption duplicate", "assumption duplicate"),
      Vector("issue condition", "issue condition duplicate", "issue condition duplicate"),
      Vector("issue limitation", "issue limitation duplicate", "issue limitation duplicate"),
      Vector("c-limitation", "c-source-secondary"), InternalModelOpenIssueBlocking(true, false, true, false)
    )
    InternalModelOpenIssueLedger("ccdm-open-issue-records-v1", "1.0", "ledger-order", _scope, InternalModelOpenIssueBasis("realization-main", "realization-order", _sha256(realization)), Vector(issue), Vector.empty)
  }

  private def _realization(v2: Boolean = false): Array[Byte] = {
    val references = Vector(_reference("ref-customer", "element", "e-customer", "anchor-customer"), _reference("ref-relationship", "relationship", "r-uses", "anchor-relationship"), _reference("ref-usecase", "element", "e-usecase", "anchor-usecase"))
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(Vector(_assertion("a-customer", "element", "e-customer", "ref-customer", "Customer is a party", Vector.empty, v2), _assertion("a-relationship", "relationship", "r-uses", "ref-relationship", "Use case uses Customer", Vector("c-limitation", "c-source-secondary"), v2), _assertion("a-usecase", "element", "e-usecase", "ref-usecase", "Place an order", Vector.empty, v2))),
      "conditions" -> Json.fromValues(Vector(_condition("c-limitation", "relationship", "r-uses", "ref-relationship", "source limitation"), _condition("c-source-secondary", "relationship", "r-uses", "ref-relationship", "source limitation secondary"))),
      "elements" -> Json.fromValues(Vector(_element("e-customer", "customer", "Customer", Vector("a-customer")), _element("e-usecase", "use-case", "Place order", Vector("a-usecase")))),
      "enrichmentAssertions" -> Json.arr(), "profile" -> Json.fromString(if v2 then "ccdm-realization-v2" else "ccdm-realization-v1"), "realizationIdentity" -> Json.fromString("realization-order"),
      "relationships" -> Json.arr(_relationship()), "schemaVersion" -> Json.fromString(if v2 then "2.0" else "1.0"), "scope" -> _scope_json,
      "sourceReferences" -> Json.fromValues(references), "successorLinks" -> Json.arr(), "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
    ))
  }

  private def _source_snapshot(): Array[Byte] = _canonical(Json.obj(
    "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(Vector(_fact("element", "e-customer", "anchor-customer", "Customer is a party", Vector.empty), _fact("element", "e-usecase", "anchor-usecase", "Place an order", Vector.empty), _fact("relationship", "r-uses", "anchor-relationship", "Use case uses Customer", Vector("source limitation", "source limitation secondary"))))),
    "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source_json
  ))

  private def _reference(referenceid: String, kindvalue: String, identity: String, anchor: String): Json = Json.obj("referenceId" -> Json.fromString(referenceid), "snapshotArtifactId" -> Json.fromString("snapshot-model"), "source" -> _source_json, "sourceAnchor" -> Json.fromString(anchor), "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue)))
  private def _assertion(assertionid: String, kindvalue: String, identity: String, referenceid: String, content: String, conditionids: Vector[String], v2: Boolean): Json = { val fields = Vector("assertionId" -> Json.fromString(assertionid), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "content" -> Json.fromString(content), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue), "sourceReferenceId" -> Json.fromString(referenceid)); Json.obj((if v2 then fields :+ ("association" -> Json.Null) else fields)*) }
  private def _condition(conditionid: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json = Json.obj("affectedIdentity" -> Json.fromString(affectedidentity), "affectedKind" -> Json.fromString(affectedkind), "conditionId" -> Json.fromString(conditionid), "detail" -> Json.fromString(detail), "kind" -> Json.fromString("limitation"), "sourceReferenceId" -> Json.fromString(referenceid))
  private def _element(identity: String, kindvalue: String, label: String, canonicalids: Vector[String]): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.arr(), "enrichmentAssertionIds" -> Json.arr(), "identity" -> Json.fromString(identity), "kind" -> Json.fromString(kindvalue), "label" -> Json.fromString(label))
  private def _relationship(): Json = Json.obj("canonicalAssertionIds" -> Json.arr(Json.fromString("a-relationship")), "conditionIds" -> Json.fromValues(Vector("c-limitation", "c-source-secondary").map(Json.fromString)), "direction" -> Json.fromString("source-to-target"), "enrichmentAssertionIds" -> Json.arr(), "identity" -> Json.fromString("r-uses"), "label" -> Json.fromString("uses"), "role" -> Json.fromString("uses"), "sourceElementIdentity" -> Json.fromString("e-usecase"), "targetElementIdentity" -> Json.fromString("e-customer"))
  private def _fact(kindvalue: String, identity: String, anchor: String, content: String, limitations: Vector[String]): Json = Json.obj("componentIdentity" -> Json.fromString("component-order"), "content" -> Json.fromString(content), "limitations" -> Json.fromValues(limitations.map(Json.fromString)), "projectionContextIdentity" -> Json.fromString("context-order"), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue), "sourceAnchor" -> Json.fromString(anchor))

  private def _with_fixture(
    realization: Array[Byte],
    issue: Option[Array[Byte]],
    openissuesdependencies: Vector[String] = Vector("realization-main"),
    openpath: String = "open-issues.yaml",
    includeabsentissue: Boolean = true,
    additionalissue: Option[Array[Byte]] = None,
    additionalrealization: Option[Array[Byte]] = None
  )(f: Path => Unit): Unit = {
    val snapshot = _source_snapshot()
    val artifacts = Vector(_artifact("snapshot-model", "snapshots/model.json", "source-snapshot", true, snapshot, Vector.empty), _artifact("realization-main", "realizations/main.json", "realization", true, realization, Vector("snapshot-model"))) ++ additionalrealization.map(bytes => _artifact("realization-other", "realizations/other.json", "realization", true, bytes, Vector("snapshot-model"))).toVector ++ issue.map(bytes => _artifact("open-issue-main", openpath, "open-issue", true, bytes, openissuesdependencies)).toVector ++ additionalissue.map(bytes => _artifact("open-issue-other", "records/other.json", "open-issue", true, bytes, openissuesdependencies)).toVector ++ (if issue.isEmpty && includeabsentissue then Vector(_artifact("open-issue-main", openpath, "open-issue", false, "unused\n".getBytes(StandardCharsets.UTF_8), openissuesdependencies)) else Vector.empty)
    val root = Files.createTempDirectory("internal-model-open-issue-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: issue-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      issue.foreach(bytes => _write(root.resolve("src/main/internal-model").resolve(openpath), bytes))
      additionalrealization.foreach(bytes => _write(root.resolve("src/main/internal-model/realizations/other.json"), bytes))
      additionalissue.foreach(bytes => _write(root.resolve("src/main/internal-model/records/other.json"), bytes))
      f(root)
    } finally _delete_tree(root)
  }

  private def _open_issue_handoff(ledger: InternalModelOpenIssueLedger): InternalModelVerifiedOpenIssue = InternalModelVerifiedOpenIssue("open-issue-main", "open-issue", "open-issues.yaml", true, Vector("realization-main"), InternalModelOpenIssueRecordCodec.encode(ledger).toVector)
  private def _json(ledger: InternalModelOpenIssueLedger): Json = _parser.parse(new String(InternalModelOpenIssueRecordCodec.encode(ledger), StandardCharsets.UTF_8)).toOption.getOrElse(Json.Null)
  private def _update_root(ledger: InternalModelOpenIssueLedger, update: JsonObject => JsonObject): Json = _json(ledger).asObject.map(root => Json.fromJsonObject(update(root))).getOrElse(Json.Null)
  private def _update_issue(ledger: InternalModelOpenIssueLedger, update: JsonObject => JsonObject): Json = _update_root(ledger, root => root("issues").flatMap(_.asArray).flatMap(_.headOption).flatMap(_.asObject).map(issue => root.add("issues", Json.fromValues(Json.fromJsonObject(update(issue)) +: root("issues").flatMap(_.asArray).map(_.toVector.drop(1)).getOrElse(Vector.empty)))).getOrElse(root))
  private def _update_blocking(ledger: InternalModelOpenIssueLedger, update: JsonObject => JsonObject): Json = _update_issue(ledger, issue => issue("blocking").flatMap(_.asObject).map(blocking => issue.add("blocking", Json.fromJsonObject(update(blocking)))).getOrElse(issue))
  private def _update_scope(ledger: InternalModelOpenIssueLedger, update: JsonObject => JsonObject): Json = _update_root(ledger, root => root("scope").flatMap(_.asObject).map(scope => root.add("scope", Json.fromJsonObject(update(scope)))).getOrElse(root))
  private def _update_basis(ledger: InternalModelOpenIssueLedger, update: JsonObject => JsonObject): Json = _update_root(ledger, root => root("basis").flatMap(_.asObject).map(basis => root.add("basis", Json.fromJsonObject(update(basis)))).getOrElse(root))
  private def _update_target(ledger: InternalModelOpenIssueLedger, update: JsonObject => JsonObject): Json = _update_issue(ledger, issue => issue("affectedTargets").flatMap(_.asArray).flatMap(_.headOption).flatMap(_.asObject).map(target => issue.add("affectedTargets", Json.fromValues(Json.fromJsonObject(update(target)) +: issue("affectedTargets").flatMap(_.asArray).map(_.toVector.drop(1)).getOrElse(Vector.empty)))).getOrElse(issue))
  private def _update_evidence(ledger: InternalModelOpenIssueLedger, index: Int, update: JsonObject => JsonObject): Json = _update_issue(ledger, issue => issue("consideredEvidence").flatMap(_.asArray).map(entries => issue.add("consideredEvidence", Json.fromValues(entries.toVector.zipWithIndex.map { case (entry, entryindex) => if entryindex == index then entry.asObject.map(value => Json.fromJsonObject(update(value))).getOrElse(entry) else entry }))).getOrElse(issue))
  private def _update_evidence_source(ledger: InternalModelOpenIssueLedger, index: Int, update: JsonObject => JsonObject): Json = _update_evidence(ledger, index, evidence => evidence("source").flatMap(_.asObject).map(source => evidence.add("source", Json.fromJsonObject(update(source)))).getOrElse(evidence))
  private def _update_option(ledger: InternalModelOpenIssueLedger, update: JsonObject => JsonObject): Json = _update_issue(ledger, issue => issue("options").flatMap(_.asArray).flatMap(_.headOption).flatMap(_.asObject).map(option => issue.add("options", Json.fromValues(Json.fromJsonObject(update(option)) +: issue("options").flatMap(_.asArray).map(_.toVector.drop(1)).getOrElse(Vector.empty)))).getOrElse(issue))
  private def _rewrite_manifest_artifact(path: Path, artifactid: String, update: JsonObject => JsonObject): Unit = {
    val root = _parser.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).toOption.flatMap(_.asObject).getOrElse(JsonObject.empty)
    val artifacts = root("artifacts").flatMap(_.asArray).map(_.toVector).getOrElse(Vector.empty).map { artifact =>
      artifact.asObject.flatMap(_("artifactId")).flatMap(_.asString) match {
        case Some(`artifactid`) => artifact.asObject.map(value => Json.fromJsonObject(update(value))).getOrElse(artifact)
        case _ => artifact
      }
    }
    val revised = root.add("artifacts", Json.fromValues(artifacts)).remove("packageDigest")
    val complete = revised.add("packageDigest", Json.fromString(_sha256(_canonical(revised.toJson))))
    _write(path, _canonical(complete.toJson))
  }

  private def _unsorted_identity_mutations(ledger: InternalModelOpenIssueLedger): Vector[Array[Byte]] = {
    def _reverse_issue_(key: String): Array[Byte] = _canonical(_update_issue(ledger, issue => issue(key).flatMap(_.asArray).map(values => issue.add(key, Json.fromValues(values.reverse))).getOrElse(issue)))
    val issues = _canonical(_update_root(ledger, root => root("issues").flatMap(_.asArray).map(values => root.add("issues", Json.fromValues(values.reverse))).getOrElse(root)))
    val targets = _reverse_issue_("affectedTargets")
    val evidence = _reverse_issue_("consideredEvidence")
    val options = _reverse_issue_("options")
    val conditions = _reverse_issue_("realizationConditionIds")
    val evidenceconditions = _canonical(_update_evidence(ledger, 3, evidence => evidence("conditionIds").flatMap(_.asArray).map(values => evidence.add("conditionIds", Json.fromValues(values.reverse))).getOrElse(evidence)))
    val optionevidence = _canonical(_update_option(ledger, option => option("evidenceIds").flatMap(_.asArray).map(values => option.add("evidenceIds", Json.fromValues(values.reverse))).getOrElse(option)))
    Vector(issues, targets, evidence, options, conditions, evidenceconditions, optionevidence)
  }

  private def _permutation[T](values: Vector[T]): Gen[Vector[T]] = { def _loop_(remaining: Vector[T], collected: Vector[T]): Gen[Vector[T]] = if remaining.isEmpty then Gen.const(collected) else Gen.choose(0, remaining.size - 1).flatMap(index => _loop_(remaining.patch(index, Vector.empty, 1), collected :+ remaining(index))); _loop_(values, Vector.empty) }
  private def _artifact(id: String, path: String, rolevalue: String, required: Boolean, bytes: Array[Byte], dependencies: Vector[String]): Json = Json.obj("artifactId" -> Json.fromString(id), "dependsOn" -> Json.fromValues(dependencies.map(Json.fromString)), "path" -> Json.fromString(path), "required" -> Json.fromBoolean(required), "role" -> Json.fromString(rolevalue), "sha256" -> Json.fromString(_sha256(bytes)))
  private def _manifest(artifacts: Vector[Json]): Array[Byte] = { def _order_(remaining: Vector[Json], collected: Vector[Json]): Vector[Json] = if remaining.isEmpty then collected else { val ready = remaining.filter(artifact => artifact.hcursor.get[Vector[String]]("dependsOn").toOption.getOrElse(Vector.empty).forall(id => collected.exists(_.hcursor.get[String]("artifactId").toOption.contains(id)))).sortBy(_.hcursor.get[String]("artifactId").toOption.getOrElse("")).head; _order_(remaining.filterNot(_ == ready), collected :+ ready) }; val root = JsonObject.fromIterable(Vector("artifacts" -> Json.fromValues(_order_(artifacts, Vector.empty)), "lifecycleState" -> Json.fromString("draft"), "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)), "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"), "projectId" -> Json.fromString("issue-sample"), "projectNamespace" -> Json.fromString("org.example"), "revision" -> Json.fromInt(1), "schemaVersion" -> Json.fromString("1.0"))); _canonical(root.add("packageDigest", Json.fromString(_sha256(_canonical(root.remove("packageDigest").toJson)))).toJson) }
  private def _scope_json: Json = Json.obj("componentIdentity" -> Json.fromString(_scope.componentIdentity), "projectionContextIdentity" -> Json.fromString(_scope.projectionContextIdentity), "selectedUseCaseElementIdentity" -> Json.fromString(_scope.selectedUseCaseElementIdentity))
  private def _source_json: Json = Json.obj("authority" -> Json.fromString(_source.authority), "identity" -> Json.fromString(_source.identity), "locator" -> _source.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> _source.revision.map(Json.fromString).getOrElse(Json.Null), "sha256" -> Json.fromString(_source.sha256))
  private def _canonical(json: Json): Array[Byte] = (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
  private def _sha256(bytes: Array[Byte]): String = "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
  private def _write(path: Path, bytes: Array[Byte]): Unit = { Files.createDirectories(path.getParent); Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) }
  private def _delete_tree(root: Path): Unit = if Files.exists(root) then Files.walk(root).iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
}
