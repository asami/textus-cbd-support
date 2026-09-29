package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest
import java.util.Base64

import scala.jdk.CollectionConverters.*

import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Executable specification for immutable approval applicability and portable supersession. */
final class InternalModelCandidateApprovalLifecycleSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private final case class FixtureOptions(
    decision: InternalModelCandidateHumanApprovalDecision = InternalModelCandidateHumanApprovalDecision.Approved,
    approvalartifactid: String = "approval-main",
    approvalidentity: String = "approval-main",
    approvalrevision: Int = 1,
    includeoptionalsnapshot: Boolean = false,
    candidateartifactid: String = "candidate-main",
    reviewartifactid: String = "review-main",
    diffartifactid: String = "semantic-diff-main",
    candidatecontentmarker: String = "",
    reviewcontentmarker: String = "",
    diffcontentmarker: String = "",
    candidateidentity: String = "candidate-order-v1",
    candidatemodelidentity: String = "candidate-model-order",
    candidaterevision: Int = 7,
    reviewidentity: String = "review-identity",
    reviewrevision: Int = 3,
    diffidentity: String = "semantic-diff-order",
    diffrevision: Int = 3,
    componentidentity: String = "component-order",
    projectioncontextidentity: String = "context-order",
    selectedusecaseelementidentity: String = "e-usecase",
    packageid: String = "01234567-89ab-cdef-0123-456789abcdef",
    projectid: String = "candidate-review-sample",
    projectnamespace: String = "org.example",
    packagerevision: Long = 1L,
    ruleid: String = "rule-alpha",
    ruleversion: String = "1.0",
    rulehashseed: String = "rule-alpha",
    providerid: String = "provider-alpha",
    providerversion: String = "1.0",
    providerhashseed: String = "provider-alpha"
  )

  private final case class FixtureData(
    expected: InternalModelCandidateHumanApprovalInput,
    executionbasis: InternalModelCandidateReviewExecutionBasis
  )

  private final case class FixtureEvidence(
    approval: InternalModelCandidateHumanApprovalAdmission,
    review: InternalModelCandidateReviewBindingAdmission,
    snapshots: Vector[InternalModelVerifiedSourceSnapshot]
  )

  private val _printer = Printer.noSpacesSortKeys
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _scenario_raw = "scenario source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _glossary_raw = "glossary source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_alpha_raw = "alpha cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_beta_raw = "beta cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _model_source = _source("model-authority", "model-source", Some("catalog/model-source"), Some("revision-1"), _sha256(_model_raw.toArray))
  private val _scenario_source = _source("scenario-authority", "scenario-source", Some("catalog/scenario-source"), Some("revision-1"), _sha256(_scenario_raw.toArray))
  private val _glossary_source = _source("glossary-authority", "glossary-source", Some("catalog/glossary-source"), Some("revision-1"), _sha256(_glossary_raw.toArray))
  private val _cml_alpha_source = _source("cml-authority", "cml-alpha", Some("catalog/cml-alpha"), Some("revision-alpha"), _sha256(_cml_alpha_raw.toArray))
  private val _cml_beta_source = _source("cml-authority", "cml-beta", Some("catalog/cml-beta"), Some("revision-beta"), _sha256(_cml_beta_raw.toArray))

  "Candidate approval lifecycle" should {
    "derive each original human decision from exact current evidence" which {
      "preserve Approved, Rejected, and ChangesRequested with ordered unresolved items" in {
        Given("three actual two-target P10-33 admissions with independently authored human decisions and every live source unchanged")
        var reports = Vector.empty[InternalModelCandidateApprovalLifecycleReport]
        When("each captured approval, captured review, complete historical source inventory, and independent execution basis are evaluated")
        reports = Vector(InternalModelCandidateHumanApprovalDecision.Approved, InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested).flatMap { decision =>
          var result = Option.empty[InternalModelCandidateApprovalLifecycleReport]
          _with_fixture(FixtureOptions(decision = decision)) { (root, data) =>
            val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
            val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
            val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
            result = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, _unchanged_sources, None).toOption
          }
          result
        }
        Then("the report state equals the original decision and preserves duplicate unresolved-item order")
        reports.map(_.state) shouldBe Vector(InternalModelCandidateApprovalLifecycleState.Approved, InternalModelCandidateApprovalLifecycleState.Rejected, InternalModelCandidateApprovalLifecycleState.ChangesRequested)
        reports.map(_.approval.record.approval.unresolvedItems) shouldBe Vector.fill(3)(Vector("follow-up", "later", "follow-up"))
      }
    }

    "apply an explicit portable link only" which {
      "derive Superseded while retaining original and successor records without guessing a sibling" in {
        Given("two separately actual-admitted approvals with the same subject and a canonical explicit predecessor-to-successor link")
        var report = Option.empty[InternalModelCandidateApprovalLifecycleReport]
        var original = Option.empty[InternalModelCandidateHumanApprovalAdmission]
          _with_fixture() { (root, data) =>
          original = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          _with_fixture(FixtureOptions(approvalrevision = 2)) { (successorroot, successordata) =>
            val successor = InternalModelCandidateHumanApprovalValidator.validate(successorroot, "approval-main", "review-main", successordata.executionbasis, successordata.expected).toOption.get
            val link = _link(original.get, successor)
            When("the evaluator receives the exact link and separately admitted successor rather than a revision or provider heuristic")
            report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.get, review, data.executionbasis, snapshots, _unchanged_sources, Some(InternalModelCandidateApprovalSupersessionInput(link, successor))).toOption
          }
        }
        Then("Superseded retains original canonical bytes, successor evidence, and no inferred currentness grant")
        report.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
        report.map(_.approval.record.canonicalBytes) shouldBe original.map(_.record.canonicalBytes)
        report.flatMap(_.supersession).map(_.successorApproval.record.approval.approvalRevision) shouldBe Some(2)
      }

      "derive Superseded for every separately admitted successor decision and never infer a sibling when the explicit link is absent" in {
        Given("one original actual admission and three separately actual-admitted higher-revision successors with the same reviewed subject")
        var results = Vector.empty[(InternalModelCandidateHumanApprovalDecision, Option[InternalModelCandidateApprovalLifecycleState], Option[InternalModelCandidateApprovalLifecycleState])]
        When("each canonical link is supplied exactly once, followed by an evaluation with no link at all")
        results = Vector(InternalModelCandidateHumanApprovalDecision.Approved, InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested).map { decision =>
          var linked = Option.empty[InternalModelCandidateApprovalLifecycleState]
          var unlinked = Option.empty[InternalModelCandidateApprovalLifecycleState]
          _with_fixture_pair(FixtureOptions(decision = decision, approvalrevision = 2)) { (original, successor, originaldata) =>
            val link = _link(original.approval, successor.approval)
            linked = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, original.review, originaldata.executionbasis, original.snapshots, _unchanged_sources, Some(InternalModelCandidateApprovalSupersessionInput(link, successor.approval))).toOption.map(_.state)
            unlinked = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, original.review, originaldata.executionbasis, original.snapshots, _unchanged_sources, None).toOption.map(_.state)
          }
          (decision, linked, unlinked)
        }
        Then("the valid link retires only its predecessor while None leaves the original Approved decision without any history or sibling inference")
        results.map(_._2) shouldBe Vector.fill(3)(Some(InternalModelCandidateApprovalLifecycleState.Superseded))
        results.map(_._3) shouldBe Vector.fill(3)(Some(InternalModelCandidateApprovalLifecycleState.Approved))
      }

      "reject mismatched tuples, self lineage, and same-identity revision reuse while permitting a distinct identity only through its exact link" in {
        Given("actual predecessor and successor admissions plus a lineage matrix of explicit portable link variants")
        var outcomes = Vector.empty[Boolean]
        When("each link is decoded and evaluated against separately admitted successor evidence")
        _with_fixture() { (root, data) =>
          val predecessor = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          _with_fixture(FixtureOptions(approvalrevision = 2)) { (successorroot, successordata) =>
            val successor = InternalModelCandidateHumanApprovalValidator.validate(successorroot, "approval-main", "review-main", successordata.executionbasis, successordata.expected).toOption.get
            val valid = _link(predecessor, successor)
            val mismatchpredecessor = valid.copy(predecessorApproval = valid.predecessorApproval.copy(sha256 = _digest("other")))
            val mismatchsuccessor = valid.copy(successorApproval = valid.successorApproval.copy(sha256 = _digest("other-successor")))
            val self = valid.copy(successorApproval = valid.predecessorApproval)
            outcomes = Vector(mismatchpredecessor, mismatchsuccessor, self).map(link => InternalModelCandidateApprovalLifecycleEvaluator.evaluate(predecessor, review, data.executionbasis, snapshots, _unchanged_sources, Some(InternalModelCandidateApprovalSupersessionInput(_canonical_link(link), successor))).isSuccess)
          }
        }
        Then("a link can retire only the exact external predecessor with the exact separately admitted successor tuple")
        outcomes shouldBe Vector(false, false, false)
      }

      "reject every cross-subject, rollback, collision, and same-identity revision misuse while supporting higher revised and distinct-identity successors" in {
        Given("one actual predecessor and a complete lineage matrix of separately actual-admitted successor subjects")
        val rejected = Vector(
          FixtureOptions(projectnamespace = "org.other", approvalrevision = 2),
          FixtureOptions(projectid = "candidate-review-other", approvalrevision = 2),
          FixtureOptions(packageid = "abcdefab-cdef-abcd-efab-cdefabcdefab", approvalrevision = 2),
          FixtureOptions(componentidentity = "component-other", approvalrevision = 2),
          FixtureOptions(candidateidentity = "candidate-other", approvalrevision = 2),
          FixtureOptions(candidatemodelidentity = "candidate-model-other", approvalrevision = 2),
          FixtureOptions(candidaterevision = 6, approvalrevision = 2),
          FixtureOptions(candidatecontentmarker = " same revision collision", approvalrevision = 2),
          FixtureOptions(decision = InternalModelCandidateHumanApprovalDecision.Rejected)
        )
        var outcomes = Vector.empty[Boolean]
        var supported = Vector.empty[InternalModelCandidateApprovalLifecycleState]
        When("each exact link is evaluated only against its separately admitted successor, without a transitive or latest-record selector")
        outcomes = rejected.map { options =>
          var accepted = true
          _with_fixture_pair(options) { (original, successor, originaldata) =>
            accepted = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, original.review, originaldata.executionbasis, original.snapshots, _unchanged_sources, Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval))).isSuccess
          }
          accepted
        }
        supported = Vector(
          FixtureOptions(candidaterevision = 8, candidatecontentmarker = " higher revision", approvalrevision = 2),
          FixtureOptions(candidaterevision = 8, approvalidentity = "approval-distinct", approvalrevision = 1, decision = InternalModelCandidateHumanApprovalDecision.ChangesRequested)
        ).flatMap { options =>
          var state = Option.empty[InternalModelCandidateApprovalLifecycleState]
          _with_fixture_pair(options) { (original, successor, originaldata) =>
            state = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, original.review, originaldata.executionbasis, original.snapshots, _unchanged_sources, Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval))).toOption.map(_.state)
          }
          state
        }
        Then("each prohibited lineage fails closed, while a higher candidate revision and a distinct explicit approval identity remain portable supported replacements")
        outcomes shouldBe Vector.fill(rejected.size)(false)
        supported shouldBe Vector.fill(2)(InternalModelCandidateApprovalLifecycleState.Superseded)
      }
    }

    "retain complete currentness evidence" which {
      "invalidate every captured source comparison rather than treating a changed authority or path as unchanged" in {
        Given("an actual full historical source inventory and a source-owner observation whose model source bytes and revision changed")
        var report = Option.empty[InternalModelCandidateApprovalLifecycleReport]
        When("the pure evaluator compares all supplied source snapshots without reopening a path")
        _with_fixture() { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val changed = _unchanged_sources.updated("snapshot-model", InternalModelLiveSourceObservation.Observed("model-authority", "model-source", Some("revision-2"), "changed model source\n".getBytes(StandardCharsets.UTF_8).toVector, None))
          report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get, changed, None).toOption
        }
        Then("Invalidated retains the changed source artifact and its concrete revision and raw-byte dimensions")
        report.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Invalidated)
        report.map(_.invalidations.exists(value => value.kind == InternalModelCandidateApprovalInvalidationKind.SourceChanged && value.artifactId.contains("snapshot-model") && value.changedDimensionNames.contains("source.revision") && value.changedDimensionNames.contains("source.sha256"))) shouldBe Some(true)
      }

      "retain every independently admitted candidate, review, diff, scope, package, rule, and provider mismatch dimension representable by the fixed supported package schema" in {
        Given("one actual original approval admission and one independently actual-admitted current review for every valid frozen basis dimension under schema 1.0")
        val cases = Vector(
          ("candidate artifact ID", FixtureOptions(candidateartifactid = "candidate-current"), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateArtifact.artifactId"),
          ("candidate artifact hash", FixtureOptions(candidatecontentmarker = " current"), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateArtifact.sha256"),
          ("candidate identity", FixtureOptions(candidateidentity = "candidate-order-current"), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateIdentity"),
          ("candidate model identity", FixtureOptions(candidatemodelidentity = "candidate-model-current"), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateModelIdentity"),
          ("candidate revision", FixtureOptions(candidaterevision = 8), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateRevision"),
          ("review artifact ID", FixtureOptions(reviewartifactid = "review-current"), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewArtifact.artifactId"),
          ("review artifact hash", FixtureOptions(reviewcontentmarker = "current review limitation"), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewArtifact.sha256"),
          ("review identity", FixtureOptions(reviewidentity = "review-current"), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewIdentity"),
          ("review revision", FixtureOptions(reviewrevision = 4), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewRevision"),
          ("diff artifact ID", FixtureOptions(diffartifactid = "semantic-diff-current"), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffArtifact.artifactId"),
          ("diff artifact hash", FixtureOptions(diffcontentmarker = "current diff limitation"), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffArtifact.sha256"),
          ("diff identity", FixtureOptions(diffidentity = "semantic-diff-current"), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffIdentity"),
          ("diff revision", FixtureOptions(diffrevision = 4), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffRevision"),
          ("scope component", FixtureOptions(componentidentity = "component-current"), InternalModelCandidateApprovalInvalidationKind.ScopeChanged, "scope.componentIdentity"),
          ("scope context", FixtureOptions(projectioncontextidentity = "context-current"), InternalModelCandidateApprovalInvalidationKind.ScopeChanged, "scope.projectionContextIdentity"),
          ("scope selected use case", FixtureOptions(selectedusecaseelementidentity = "e-usecase-current"), InternalModelCandidateApprovalInvalidationKind.ScopeChanged, "scope.selectedUseCaseElementIdentity"),
          ("package digest", FixtureOptions(packagerevision = 2L), InternalModelCandidateApprovalInvalidationKind.ReviewedPackageChanged, "reviewedPackage.packageDigest"),
          ("package ID", FixtureOptions(packageid = "abcdefab-cdef-abcd-efab-cdefabcdefab"), InternalModelCandidateApprovalInvalidationKind.ReviewedPackageChanged, "reviewedPackage.packageId"),
          ("project ID", FixtureOptions(projectid = "candidate-review-current"), InternalModelCandidateApprovalInvalidationKind.ReviewedPackageChanged, "reviewedPackage.projectId"),
          ("project namespace", FixtureOptions(projectnamespace = "org.current"), InternalModelCandidateApprovalInvalidationKind.ReviewedPackageChanged, "reviewedPackage.projectNamespace"),
          ("package revision", FixtureOptions(packagerevision = 2L), InternalModelCandidateApprovalInvalidationKind.ReviewedPackageChanged, "reviewedPackage.revision"),
          ("rule ID", FixtureOptions(ruleid = "rule-aardvark"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "originalReview.rules.ids"),
          ("rule version", FixtureOptions(ruleversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "originalReview.rules[rule-alpha].version"),
          ("rule hash", FixtureOptions(rulehashseed = "rule-alpha-current"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "originalReview.rules[rule-alpha].sha256"),
          ("provider ID", FixtureOptions(providerid = "provider-aardvark"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "originalReview.providers.ids"),
          ("provider version", FixtureOptions(providerversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "originalReview.providers[provider-alpha].version"),
          ("provider hash", FixtureOptions(providerhashseed = "provider-alpha-current"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "originalReview.providers[provider-alpha].sha256")
        )
        var reports = Vector.empty[(InternalModelCandidateApprovalInvalidationKind, String, InternalModelCandidateApprovalLifecycleReport)]
        When("the evaluator compares each complete independently admitted current review against the retained original admission")
        reports = cases.flatMap { case (_, options, kind, dimension) =>
          var result = Option.empty[InternalModelCandidateApprovalLifecycleReport]
          _with_fixture_pair(options) { (original, current, currentdata) =>
            result = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, current.review, currentdata.executionbasis, original.snapshots, _unchanged_sources, None).toOption
          }
          result.map(report => (kind, dimension, report))
        }
        Then("every named valid field contributes its typed lossless invalidation and an invalidated derived state, while an inconsistent schema context is separately fail-closed")
        reports should have size cases.size
        reports.forall { case (kind, dimension, report) => report.state == InternalModelCandidateApprovalLifecycleState.Invalidated && report.invalidations.exists(value => value.kind == kind && value.changedDimensionNames.contains(dimension)) } shouldBe true
      }

      "treat carrier-only transport changes as unchanged historical subject evidence" in {
        Given("one complete actual admission whose independent carrier manifest is altered only after all records and source snapshots are captured")
        var report = Option.empty[InternalModelCandidateApprovalLifecycleReport]
        When("the evaluator is given the retained admission rather than any carrier path, digest, revision, or transport addition")
        _with_fixture() { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          _write(root.resolve("src/main/internal-model/manifest.yaml"), "carrier-only-revision-and-transport-change\n".getBytes(StandardCharsets.UTF_8))
          report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, _unchanged_sources, None).toOption
        }
        Then("carrier transport does not become a currentness reason and the retained historical decision remains Approved")
        report.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Approved)
        report.map(_.invalidations) shouldBe Some(Vector.empty)
      }

      "retain every independently supplied current rule and provider ID, version, and hash deviation" in {
        Given("an actual original approval and actual current reviews whose individual rule or provider dimensions differ from a separately valid expected execution basis")
        val cases = Vector(
          (FixtureOptions(ruleid = "rule-aardvark"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "currentReview.rules.ids"),
          (FixtureOptions(ruleversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "currentReview.rules[rule-alpha].version"),
          (FixtureOptions(rulehashseed = "rule-alpha-current"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "currentReview.rules[rule-alpha].sha256"),
          (FixtureOptions(providerid = "provider-aardvark"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "currentReview.providers.ids"),
          (FixtureOptions(providerversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "currentReview.providers[provider-alpha].version"),
          (FixtureOptions(providerhashseed = "provider-alpha-current"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "currentReview.providers[provider-alpha].sha256")
        )
        var reports = Vector.empty[(InternalModelCandidateApprovalInvalidationKind, String, InternalModelCandidateApprovalLifecycleReport)]
        When("the caller supplies the unchanged valid execution basis independently of each changed current review")
        reports = cases.flatMap { case (options, kind, dimension) =>
          var report = Option.empty[InternalModelCandidateApprovalLifecycleReport]
          _with_fixture_pair(options) { (original, current, _) =>
            report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, current.review, _basis(FixtureOptions()), original.snapshots, _unchanged_sources, None).toOption
          }
          report.map(value => (kind, dimension, value))
        }
        Then("each current-review field becomes a deterministic typed reason without erasing the separately retained historical comparison")
        reports should have size cases.size
        reports.forall { case (kind, dimension, report) => report.invalidations.exists(value => value.kind == kind && value.changedDimensionNames.contains(dimension)) } shouldBe true
      }

      "fail closed for tampered, malformed, missing, duplicate, unknown, and metadata-inconsistent snapshot or observed-key topology" in {
        Given("an actual approval admission plus structurally malformed historical snapshot vectors or observation maps")
        var outcomes = Vector.empty[Boolean]
        When("each structurally invalid capture or observation container is supplied")
        _with_fixture() { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          outcomes = Vector(
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots.drop(1), _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots :+ snapshots.head, _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots :+ InternalModelVerifiedSourceSnapshot("snapshot-foreign", "snapshots/foreign.json", false, None), _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots.updated(0, snapshots.head.copy(packageRelativePath = "snapshots/other.json")), _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots.updated(0, snapshots.head.copy(bytes = Some("tampered".getBytes(StandardCharsets.UTF_8).toVector))), _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, _unchanged_sources.updated("unknown", InternalModelLiveSourceObservation.Unavailable("unknown")), None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, Map("snapshot-model" -> null.asInstanceOf[InternalModelLiveSourceObservation]), None).isSuccess
          )
        }
        Then("no malformed capture, topology, metadata, or observed key can be converted into an applicability result")
        outcomes shouldBe Vector.fill(7)(false)
      }

      "map each source owner status across scenario, model, glossary, and both CML baselines while retaining optional absence" in {
        Given("a complete actual historical inventory containing scenario, model-context, glossary/BoK, two CML baselines, and one absent optional baseline")
        var reports = Vector.empty[(String, InternalModelCandidateApprovalInvalidationKind, Option[String], InternalModelCandidateApprovalLifecycleReport)]
        When("every present source receives changed, missing, unavailable, unauthorized, malformed, and ambiguous source-owner evidence")
        _with_fixture(FixtureOptions(includeoptionalsnapshot = true)) { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          reports = _sort_ids(_unchanged_sources.keys.toVector).flatMap { artifactid =>
            val inputs = Vector[(InternalModelCandidateApprovalInvalidationKind, Map[String, InternalModelLiveSourceObservation])](
              InternalModelCandidateApprovalInvalidationKind.SourceChanged -> _unchanged_sources.updated(artifactid, _changed_observation(artifactid)),
              InternalModelCandidateApprovalInvalidationKind.SourceUnavailable -> _unchanged_sources.removed(artifactid),
              InternalModelCandidateApprovalInvalidationKind.SourceUnavailable -> _unchanged_sources.updated(artifactid, InternalModelLiveSourceObservation.Unavailable("unavailable")),
              InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized -> _unchanged_sources.updated(artifactid, InternalModelLiveSourceObservation.Unauthorized("denied")),
              InternalModelCandidateApprovalInvalidationKind.SourceMalformed -> _unchanged_sources.updated(artifactid, InternalModelLiveSourceObservation.Malformed("malformed")),
              InternalModelCandidateApprovalInvalidationKind.SourceAmbiguousOrConflicting -> _unchanged_sources.updated(artifactid, InternalModelLiveSourceObservation.AmbiguousOrConflicting("conflict"))
            )
            inputs.flatMap { case (kind, observations) => InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, observations, None).toOption.map(report => (artifactid, kind, report.invalidations.find(value => value.kind == kind && value.artifactId.contains(artifactid)).flatMap(_.artifactId), report)) }
          }
        }
        Then("every concrete source status invalidates its exact artifact and the absent optional baseline remains MissingBaseline rather than an observation")
        reports should have size (_unchanged_sources.size * 6)
        reports.forall { case (artifactid, kind, found, report) => found.contains(artifactid) && report.state == InternalModelCandidateApprovalLifecycleState.Invalidated && report.invalidations.exists(value => value.kind == InternalModelCandidateApprovalInvalidationKind.MissingBaseline && value.artifactId.contains("snapshot-optional")) } shouldBe true
      }

      "order mixed source statuses by closed kind before unsigned UTF-8 artifact ID for every complete input permutation" in {
        Given("one complete actual capture with earlier-ID CML sources unavailable, later-ID model and scenario sources changed by content, glossary unauthorized, and optional baseline absent")
        val expected = Vector(
          InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceChanged, Some("snapshot-model"), Vector("source.sha256")),
          InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceChanged, Some("snapshot-scenario"), Vector("source.sha256")),
          InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable, Some("snapshot-cml-alpha"), Vector.empty),
          InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable, Some("snapshot-cml-beta"), Vector.empty),
          InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized, Some("snapshot-glossary"), Vector.empty),
          InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.MissingBaseline, Some("snapshot-optional"), Vector.empty)
        )
        _with_fixture(FixtureOptions(includeoptionalsnapshot = true)) { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          val sourceobservations = _unchanged_sources
            .updated("snapshot-model", _source_dimension_observation("snapshot-model", "content"))
            .updated("snapshot-scenario", _source_dimension_observation("snapshot-scenario", "content"))
            .updated("snapshot-cml-alpha", InternalModelLiveSourceObservation.Unavailable("offline"))
            .updated("snapshot-cml-beta", InternalModelLiveSourceObservation.Unavailable("offline"))
            .updated("snapshot-glossary", InternalModelLiveSourceObservation.Unauthorized("denied"))
          val permutations = for {
            snapshotorder <- Gen.pick(snapshots.size, snapshots)
            observationorder <- Gen.pick(sourceobservations.size, sourceobservations.toVector)
          } yield (snapshotorder.toVector, observationorder.toVector)
          When("the evaluator receives generated complete permutations of the captured snapshots and live-source insertion order")
          forAll(permutations) { case (snapshotorder, observationorder) =>
            val report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshotorder, Map.from(observationorder), None).toOption.get
            Then("every permutation yields the exact kind-first and unsigned-ID invalidation vector while retaining the original approval")
            report.invalidations shouldBe expected
            report.state shouldBe InternalModelCandidateApprovalLifecycleState.Invalidated
            report.approval.record.canonicalBytes shouldBe approval.record.canonicalBytes
          }
        }
      }

      "retain every source metadata and CML target-path difference independently across the complete captured inventory" in {
        Given("one complete actual capture and independently supplied observations changing authority, identity, revision, or content for every source, plus each CML path")
        val dimensions = _sort_ids(_unchanged_sources.keys.toVector).flatMap { artifactid =>
          Vector("authority", "identity", "revision", "content").map(dimension => (artifactid, dimension)) ++ Option.when(artifactid.startsWith("snapshot-cml-"))((artifactid, "path")).toVector
        }
        var reports = Vector.empty[(String, String, InternalModelCandidateApprovalLifecycleReport)]
        When("the pure evaluator compares each single changed observation against the full original snapshot inventory")
        _with_fixture() { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          reports = dimensions.flatMap { case (artifactid, dimension) =>
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, _unchanged_sources.updated(artifactid, _source_dimension_observation(artifactid, dimension)), None).toOption.map(report => (artifactid, dimension, report))
          }
        }
        Then("each exact source difference remains a SourceChanged reason for its artifact, including both CML project-relative target paths")
        reports should have size dimensions.size
        reports.forall { case (artifactid, dimension, report) =>
          val expected = dimension match {
            case "authority" => "source.authority"
            case "identity" => "source.identity"
            case "revision" => "source.revision"
            case "content" => "source.sha256"
            case "path" => "basis.projectRelativePath"
          }
          report.invalidations.exists(value => value.kind == InternalModelCandidateApprovalInvalidationKind.SourceChanged && value.artifactId.contains(artifactid) && value.changedDimensionNames.contains(expected))
        } shouldBe true
      }
    }

    "enforce the portable link grammar" which {
      "accept deterministic canonical records and reject root, tuple, layout, Unicode, profile, version, and hash variants" in {
        Given("one valid portable link and closed grammar mutations for the root and nested approval tuples")
        val valid = _link_value("前任", _digest("predecessor"), "後任", _digest("successor"))
        val canonical = InternalModelCandidateApprovalSupersessionCodec.encode(valid)
        val root = io.circe.parser.parse(new String(canonical, StandardCharsets.UTF_8)).toOption.flatMap(_.asObject).get
        val predecessor = root("predecessorApproval").flatMap(_.asObject).get
        val variants = Vector[Array[Byte]](
          canonical.dropRight(1),
          Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical,
          (" " + new String(canonical, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),
          new String(canonical, StandardCharsets.UTF_8).replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8),
          (new String(canonical, StandardCharsets.UTF_8) + "trailing").getBytes(StandardCharsets.UTF_8),
          _canonical(Json.fromJsonObject(root.remove("profile"))),
          _canonical(Json.fromJsonObject(root.add("unexpected", Json.fromString("field")))),
          _canonical(Json.fromJsonObject(root.add("predecessorApproval", Json.fromJsonObject(predecessor.remove("artifactId"))))),
          _canonical(Json.fromJsonObject(root.add("predecessorApproval", Json.fromJsonObject(predecessor.add("unexpected", Json.fromString("field")))))),
          _canonical(Json.fromJsonObject(root.add("predecessorApproval", Json.fromString("not-a-tuple")))),
          _canonical(Json.fromJsonObject(root.add("successorApproval", Json.Null))),
          _canonical(Json.fromJsonObject(root.add("profile", Json.fromInt(1)))),
          new String(canonical, StandardCharsets.UTF_8).replace("\"profile\":\"ccdm-candidate-approval-supersession-v1\",", "\"profile\":\"ccdm-candidate-approval-supersession-v1\",\"profile\":\"ccdm-candidate-approval-supersession-v1\",").getBytes(StandardCharsets.UTF_8),
          new String(canonical, StandardCharsets.UTF_8).replace("\"artifactId\":\"前任\",", "\"artifactId\":\"前任\",\"artifactId\":\"前任\",").getBytes(StandardCharsets.UTF_8),
          new String(canonical, StandardCharsets.UTF_8).replace("前任", "\\ud800").getBytes(StandardCharsets.UTF_8),
          Array[Byte](0xc3.toByte),
          InternalModelCandidateApprovalSupersessionCodec.encode(valid.copy(profile = "other")),
          InternalModelCandidateApprovalSupersessionCodec.encode(valid.copy(schemaVersion = "2.0")),
          InternalModelCandidateApprovalSupersessionCodec.encode(valid.copy(predecessorApproval = valid.predecessorApproval.copy(artifactId = "\u00a0"))),
          InternalModelCandidateApprovalSupersessionCodec.encode(valid.copy(predecessorApproval = valid.predecessorApproval.copy(sha256 = "sha256:" + ("A" * 64)))),
          InternalModelCandidateApprovalSupersessionCodec.encode(valid.copy(successorApproval = valid.predecessorApproval))
        )
        When("the valid bytes and every mutation are decoded through the strict codec")
        val results = variants.map(bytes => InternalModelCandidateApprovalSupersessionCodec.decode(bytes.toVector).isRight)
        Then("only the typed canonical root with distinct nonblank tuples survives re-encoding")
        InternalModelCandidateApprovalSupersessionCodec.decode(canonical.toVector).map(_.canonicalBytes) shouldBe Right(canonical.toVector)
        results shouldBe Vector.fill(variants.size)(false)
      }

      "preserve scalar Unicode and deterministic key bytes under property generation" in {
        Given("generated nonblank Unicode scalar predecessor and successor IDs")
        forAll(Gen.nonEmptyListOf(Gen.oneOf(Gen.alphaNumChar.map(_.toString), Gen.const("漢"), Gen.const("😀"))).map(_.mkString)) { generated =>
          val value = _link_value("前" + generated, _digest("pre-" + generated), "後" + generated, _digest("post-" + generated))
          When("the typed link is encoded twice and decoded")
          val first = InternalModelCandidateApprovalSupersessionCodec.encode(value).toVector
          val second = InternalModelCandidateApprovalSupersessionCodec.encode(value).toVector
          val decoded = InternalModelCandidateApprovalSupersessionCodec.decode(first)
          Then("both scalar IDs and canonical bytes are exactly preserved")
          first shouldBe second
          decoded.map(value => (value.predecessorApproval.artifactId, value.successorApproval.artifactId)) shouldBe Right(("前" + generated, "後" + generated))
        }
      }
    }

    "keep precedence and capture boundaries explicit" which {
      "retain all source invalidations when a valid supersession takes precedence" in {
        Given("an original actual admission, separately admitted successor, canonical link, and a changed source observation")
        var report = Option.empty[InternalModelCandidateApprovalLifecycleReport]
        _with_fixture() { (root, data) =>
          val original = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          _with_fixture(FixtureOptions(approvalrevision = 2, decision = InternalModelCandidateHumanApprovalDecision.Rejected)) { (successorroot, successordata) =>
            val successor = InternalModelCandidateHumanApprovalValidator.validate(successorroot, "approval-main", "review-main", successordata.executionbasis, successordata.expected).toOption.get
            When("the evaluator receives both the exact link and changed source-owner evidence")
            report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original, review, data.executionbasis, snapshots, _unchanged_sources.updated("snapshot-model", InternalModelLiveSourceObservation.Unavailable("offline")), Some(InternalModelCandidateApprovalSupersessionInput(_link(original, successor), successor))).toOption
          }
        }
        Then("Superseded is derived without erasing the original Approved decision or SourceUnavailable evidence")
        report.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
        report.map(_.approval.record.approval.decision) shouldBe Some(InternalModelCandidateHumanApprovalDecision.Approved)
        report.map(_.invalidations.exists(_.kind == InternalModelCandidateApprovalInvalidationKind.SourceUnavailable)) shouldBe Some(true)
      }

      "retain multiple independent current-basis and source reasons when a valid portable supersession takes precedence" in {
        Given("one actual original admission and one separately actual-admitted successor/current review with changed rule hash, provider hash, and unavailable model-source observation")
        var report = Option.empty[InternalModelCandidateApprovalLifecycleReport]
        var originalbytes = Option.empty[Vector[Byte]]
        var successorbytes = Option.empty[Vector[Byte]]
        When("the exact successor link, complete current review basis, and source-owner evidence are evaluated together")
        _with_fixture_pair(FixtureOptions(approvalrevision = 2, rulehashseed = "rule-alpha-current", providerhashseed = "provider-alpha-current")) { (original, successor, currentdata) =>
          originalbytes = Some(original.approval.record.canonicalBytes)
          successorbytes = Some(successor.approval.record.canonicalBytes)
          report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, successor.review, currentdata.executionbasis, original.snapshots, _unchanged_sources.updated("snapshot-model", InternalModelLiveSourceObservation.Unavailable("offline")), Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval))).toOption
        }
        Then("Superseded preserves both canonical admissions and every rule, provider, and source reason rather than short-circuiting on its state")
        report.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
        report.map(_.approval.record.canonicalBytes) shouldBe originalbytes
        report.flatMap(_.supersession).map(_.successorApproval.record.canonicalBytes) shouldBe successorbytes
        report.map(_.invalidations.map(_.kind).toSet.contains(InternalModelCandidateApprovalInvalidationKind.RulesChanged)) shouldBe Some(true)
        report.map(_.invalidations.map(_.kind).toSet.contains(InternalModelCandidateApprovalInvalidationKind.ProvidersChanged)) shouldBe Some(true)
        report.map(_.invalidations.map(_.kind).toSet.contains(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable)) shouldBe Some(true)
      }

      "remain stable after the original fixture filesystem is mutated because only captured evidence is evaluated" in {
        Given("actual captured approval, review, and source snapshots before their source package paths are overwritten")
        var stable = Option.empty[InternalModelCandidateApprovalLifecycleReport]
        When("the fixture manifest and approval path change after capture and the evaluator receives only retained values")
        _with_fixture() { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          _write(root.resolve("src/main/internal-model/manifest.yaml"), "changed\n".getBytes(StandardCharsets.UTF_8))
          stable = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, _unchanged_sources, None).toOption
        }
        Then("the retained point-in-time evaluation still derives Approved without any live reread")
        stable.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Approved)
      }

      "preserve an explicitly decoded portable link across independently captured temporary package carriers" in {
        Given("an original actual capture, a separately captured successor package, and link bytes reconstructed through the portable codec")
        var report = Option.empty[InternalModelCandidateApprovalLifecycleReport]
        var canonical = Option.empty[Vector[Byte]]
        When("the reconstructed typed link is evaluated without selecting either carrier by path, process state, or latest revision")
        _with_fixture() { (root, data) =>
          val original = _admit(root, data, FixtureOptions())
          _with_fixture(FixtureOptions(approvalrevision = 2)) { (successorroot, successordata) =>
            val successor = _admit(successorroot, successordata, FixtureOptions(approvalrevision = 2))
            val decoded = InternalModelCandidateApprovalSupersessionCodec.decode(InternalModelCandidateApprovalSupersessionCodec.encode(_link(original.approval, successor.approval)).toVector).toOption.get
            canonical = Some(decoded.canonicalBytes)
            report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, original.review, data.executionbasis, original.snapshots, _unchanged_sources, Some(InternalModelCandidateApprovalSupersessionInput(decoded, successor.approval))).toOption
          }
        }
        Then("the reconstructed bytes and derived Superseded result preserve exactly the portable external selection evidence")
        report.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
        report.flatMap(_.supersession).map(_.record.canonicalBytes) shouldBe canonical
      }

      "remain independent of source-observation map insertion order under property generation" in {
        Given("a generated ordering of the same complete source-owner observation entries and one actual captured approval")
        _with_fixture() { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          forAll(Gen.someOf(_unchanged_sources.toVector)) { selected =>
            val forward = Map.from(selected ++ _unchanged_sources.filterNot(entry => selected.map(_._1).contains(entry._1)))
            val reverse = Map.from((selected ++ _unchanged_sources.filterNot(entry => selected.map(_._1).contains(entry._1))).reverse)
            When("the same complete observations are supplied in forward and reverse construction order")
            val first = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, forward, None).toOption
            val second = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, reverse, None).toOption
            Then("the immutable report, ordered invalidations, and state are equal")
            first shouldBe second
          }
        }
      }

      "give valid supersession precedence over generated source drift without mutating retained history" in {
        Given("one actual original approval, one separately actual-admitted successor, and generated available or unavailable model-source evidence")
        _with_fixture() { (root, data) =>
          val original = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          _with_fixture(FixtureOptions(approvalrevision = 2)) { (successorroot, successordata) =>
            val successor = InternalModelCandidateHumanApprovalValidator.validate(successorroot, "approval-main", "review-main", successordata.executionbasis, successordata.expected).toOption.get
            val link = _link(original, successor)
            forAll(Gen.oneOf(true, false)) { unavailable =>
              When("the exact link and generated current source-owner observation are evaluated")
              val observations = if unavailable then _unchanged_sources.updated("snapshot-model", InternalModelLiveSourceObservation.Unavailable("offline")) else _unchanged_sources
              val report = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original, review, data.executionbasis, snapshots, observations, Some(InternalModelCandidateApprovalSupersessionInput(link, successor))).toOption
              Then("Superseded has precedence while the immutable original decision and any source reason remain present")
              report.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
              report.map(_.approval.record.canonicalBytes) shouldBe Some(original.record.canonicalBytes)
              report.flatMap(_.supersession).map(_.successorApproval.record.canonicalBytes) shouldBe Some(successor.record.canonicalBytes)
              report.map(_.invalidations.exists(_.kind == InternalModelCandidateApprovalInvalidationKind.SourceUnavailable)) shouldBe Some(unavailable)
            }
          }
        }
      }
    }

    "fail closed for malformed top-level values" which {
      "reject null approval inputs instead of manufacturing an applicability report" in {
        Given("a null original approval admission")
        When("the evaluator is invoked at its public package boundary")
        val result = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(null, null, null, null, null, null)
        Then("the structured operation result is unsuccessful")
        result.isSuccess shouldBe false
      }

      "reject null containers, invalid execution basis, forged approval bytes, and forged current review bytes" in {
        Given("actual captured admissions followed by one malformed top-level boundary value at a time")
        var outcomes = Vector.empty[Boolean]
        When("the evaluator receives each null or canonical-inconsistent input")
        _with_fixture() { (root, data) =>
          val approval = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", data.executionbasis, data.expected).toOption.get
          val review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", data.executionbasis).toOption.get
          val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
          val forgedapproval = approval.copy(record = approval.record.copy(canonicalBytes = Vector.empty))
          val forgedreview = review.copy(binding = review.binding.copy(canonicalBytes = Vector.empty))
          val forgedpackagecontext = review.copy(reviewedPackageContext = review.reviewedPackageContext.copy(schemaVersion = "1.1"))
          outcomes = Vector(
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, null, _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, null, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, InternalModelCandidateReviewExecutionBasis(Vector.empty, Vector.empty), snapshots, _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(forgedapproval, review, data.executionbasis, snapshots, _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, forgedreview, data.executionbasis, snapshots, _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, forgedpackagecontext, data.executionbasis, snapshots, _unchanged_sources, None).isSuccess,
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, snapshots, _unchanged_sources, null).isSuccess
          )
        }
        Then("every malformed top-level or forged canonical admission fails closed rather than becoming an ordinary drift report")
        outcomes shouldBe Vector.fill(7)(false)
      }
    }
  }

  private def _link(
    predecessor: InternalModelCandidateHumanApprovalAdmission,
    successor: InternalModelCandidateHumanApprovalAdmission
  ): InternalModelCandidateApprovalSupersession = {
    val value = _link_value(predecessor.approvalArtifactId, predecessor.approvalArtifactSha256, successor.approvalArtifactId, successor.approvalArtifactSha256)
    InternalModelCandidateApprovalSupersessionCodec.decode(InternalModelCandidateApprovalSupersessionCodec.encode(value).toVector).toOption.get
  }

  private def _link_value(predecessorid: String, predecessorhash: String, successorid: String, successorhash: String): InternalModelCandidateApprovalSupersession =
    InternalModelCandidateApprovalSupersession(InternalModelCandidateReviewArtifact(predecessorid, predecessorhash), InternalModelCandidateReviewArtifact(successorid, successorhash), "ccdm-candidate-approval-supersession-v1", "1.0", Vector.empty)

  private def _canonical_link(value: InternalModelCandidateApprovalSupersession): InternalModelCandidateApprovalSupersession =
    InternalModelCandidateApprovalSupersessionCodec.decode(InternalModelCandidateApprovalSupersessionCodec.encode(value).toVector).toOption.getOrElse(value.copy(canonicalBytes = InternalModelCandidateApprovalSupersessionCodec.encode(value).toVector))

  private def _unchanged_sources: Map[String, InternalModelLiveSourceObservation] = Map(
    "snapshot-model" -> InternalModelLiveSourceObservation.Observed("model-authority", "model-source", Some("revision-1"), _model_raw, None),
    "snapshot-scenario" -> InternalModelLiveSourceObservation.Observed("scenario-authority", "scenario-source", Some("revision-1"), _scenario_raw, None),
    "snapshot-glossary" -> InternalModelLiveSourceObservation.Observed("glossary-authority", "glossary-source", Some("revision-1"), _glossary_raw, None),
    "snapshot-cml-alpha" -> InternalModelLiveSourceObservation.Observed("cml-authority", "cml-alpha", Some("revision-alpha"), _cml_alpha_raw, Some("cml/alpha.cml")),
    "snapshot-cml-beta" -> InternalModelLiveSourceObservation.Observed("cml-authority", "cml-beta", Some("revision-beta"), _cml_beta_raw, Some("cml/beta.cml"))
  )

  private def _changed_observation(artifactid: String): InternalModelLiveSourceObservation =
    artifactid match {
      case "snapshot-model" => InternalModelLiveSourceObservation.Observed("model-authority-other", "model-source", Some("revision-2"), "changed model\n".getBytes(StandardCharsets.UTF_8).toVector, None)
      case "snapshot-scenario" => InternalModelLiveSourceObservation.Observed("scenario-authority-other", "scenario-source", Some("revision-2"), "changed scenario\n".getBytes(StandardCharsets.UTF_8).toVector, None)
      case "snapshot-glossary" => InternalModelLiveSourceObservation.Observed("glossary-authority-other", "glossary-source", Some("revision-2"), "changed glossary\n".getBytes(StandardCharsets.UTF_8).toVector, None)
      case "snapshot-cml-alpha" => InternalModelLiveSourceObservation.Observed("cml-authority-other", "cml-alpha", Some("revision-2"), "changed alpha\n".getBytes(StandardCharsets.UTF_8).toVector, Some("cml/alpha-other.cml"))
      case "snapshot-cml-beta" => InternalModelLiveSourceObservation.Observed("cml-authority-other", "cml-beta", Some("revision-2"), "changed beta\n".getBytes(StandardCharsets.UTF_8).toVector, Some("cml/beta-other.cml"))
      case _ => InternalModelLiveSourceObservation.Malformed("unknown fixture source")
    }

  private def _source_dimension_observation(artifactid: String, dimension: String): InternalModelLiveSourceObservation =
    _unchanged_sources.get(artifactid) match {
      case Some(InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes, path)) =>
        dimension match {
          case "authority" => InternalModelLiveSourceObservation.Observed(authority + "-current", identity, revision, rawbytes, path)
          case "identity" => InternalModelLiveSourceObservation.Observed(authority, identity + "-current", revision, rawbytes, path)
          case "revision" => InternalModelLiveSourceObservation.Observed(authority, identity, Some(revision.getOrElse("0") + "-current"), rawbytes, path)
          case "content" => InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes :+ 0.toByte, path)
          case "path" => InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes, Some("cml/current-" + artifactid + ".cml"))
          case _ => InternalModelLiveSourceObservation.Malformed("unknown source-dimension fixture")
        }
      case _ => InternalModelLiveSourceObservation.Malformed("unknown fixture source")
    }

  private def _with_fixture(options: FixtureOptions = FixtureOptions())(action: (Path, FixtureData) => Unit): Unit = {
    val projection = _projection(options)
    val realization = _realization(options)
    val continuity = _binding(options)
    val candidate = InternalModelCandidateCmlProjectionCodec.encode(projection)
    val diff = InternalModelSemanticDiffCodec.encode(_semantic_diff(projection, candidate, options))
    val model = _model_snapshot(options)
    val scenario = _scenario_snapshot()
    val glossary = _glossary_snapshot()
    val alpha = _cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw)
    val beta = _cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw)
    val evidencealpha = "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8)
    val evidencebeta = "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8)
    val historicalentries = Vector(
      _entry("evidence-alpha", "validation", "evidence/alpha.json", true, evidencealpha, Vector.empty),
      _entry("evidence-beta", "validation", "evidence/beta.json", true, evidencebeta, Vector.empty),
      _entry("snapshot-cml-alpha", "source-snapshot", "snapshots/cml-alpha.json", true, alpha, Vector.empty),
      _entry("snapshot-cml-beta", "source-snapshot", "snapshots/cml-beta.json", true, beta, Vector.empty),
      _entry("snapshot-glossary", "source-snapshot", "snapshots/glossary.json", true, glossary, Vector.empty),
      _entry("snapshot-model", "source-snapshot", "snapshots/model.json", true, model, Vector.empty),
      _entry("snapshot-scenario", "source-snapshot", "snapshots/scenario.json", true, scenario, Vector.empty),
      _entry("realization-main", "realization", "realizations/main.json", true, realization, Vector("snapshot-model")),
      _entry("decision-main", "decision", "decisions/main.json", true, "decision\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      _entry("projection-main", "projection", "projections/continuity.json", true, continuity, Vector("realization-main")),
      _entry("resume-main", "resume", "resumes/main.json", true, "resume\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      _entry(options.candidateartifactid, "projection", "projections/candidate.json", true, candidate, Vector("projection-main", "realization-main", "snapshot-cml-alpha", "snapshot-cml-beta")),
      _entry(options.diffartifactid, "projection", "projections/semantic-diff.json", true, diff, Vector(options.candidateartifactid))
    ) ++ Option.when(options.includeoptionalsnapshot)(_entry("snapshot-optional", "source-snapshot", "snapshots/optional.json", false, "optional".getBytes(StandardCharsets.UTF_8), Vector.empty)).toVector
    val reviewedmanifest = _manifest(_topological(historicalentries), options.packagerevision, options)
    val executionbasis = _basis(options)
    val reviewbytes = InternalModelCandidateReviewBindingCodec.encode(_review_binding(reviewedmanifest, realization, continuity, candidate, diff, executionbasis, options))
    val expected = _human_input(reviewedmanifest, candidate, reviewbytes, diff, options)
    val approvalbytes = InternalModelCandidateHumanApprovalCodec.encode(InternalModelCandidateHumanApproval(expected, "ccdm-candidate-human-approval-v1", "1.0", Vector.empty))
    val carrierentries = _topological(historicalentries) ++ Vector(
      _entry(options.reviewartifactid, "validation", "reviews/candidate-review.json", true, reviewbytes, historicalentries.filter(_.hcursor.get[Boolean]("required").toOption.get).map(_.hcursor.get[String]("artifactId").toOption.get).sorted),
      _entry(options.approvalartifactid, "approval", "approvals/main.json", true, approvalbytes, Vector(options.reviewartifactid))
    )
    val root = _temporary_root()
    try {
      _write(root.resolve("project.yaml"), _project_yaml(options).getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(_topological(carrierentries), 3, options))
      _write(root.resolve("src/main/internal-model/evidence/alpha.json"), evidencealpha)
      _write(root.resolve("src/main/internal-model/evidence/beta.json"), evidencebeta)
      _write(root.resolve("src/main/internal-model/snapshots/cml-alpha.json"), alpha)
      _write(root.resolve("src/main/internal-model/snapshots/cml-beta.json"), beta)
      _write(root.resolve("src/main/internal-model/snapshots/glossary.json"), glossary)
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), model)
      _write(root.resolve("src/main/internal-model/snapshots/scenario.json"), scenario)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/decisions/main.json"), "decision\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/projections/continuity.json"), continuity)
      _write(root.resolve("src/main/internal-model/projections/candidate.json"), candidate)
      _write(root.resolve("src/main/internal-model/projections/semantic-diff.json"), diff)
      _write(root.resolve("src/main/internal-model/resumes/main.json"), "resume\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/reviews/candidate-review.json"), reviewbytes)
      _write(root.resolve("src/main/internal-model/approvals/main.json"), approvalbytes)
      action(root, FixtureData(expected, executionbasis))
    } finally _delete_tree(root)
  }

  private def _with_fixture_pair(currentoptions: FixtureOptions)(action: (FixtureEvidence, FixtureEvidence, FixtureData) => Unit): Unit =
    _with_fixture() { (originalroot, originaldata) =>
      val original = _admit(originalroot, originaldata, FixtureOptions())
      _with_fixture(currentoptions) { (currentroot, currentdata) =>
        action(original, _admit(currentroot, currentdata, currentoptions), currentdata)
      }
    }

  private def _admit(root: Path, data: FixtureData, options: FixtureOptions): FixtureEvidence =
    FixtureEvidence(
      InternalModelCandidateHumanApprovalValidator.validate(root, options.approvalartifactid, options.reviewartifactid, data.executionbasis, data.expected).toOption.get,
      InternalModelCandidateReviewBindingValidator.validate(root, options.reviewartifactid, data.executionbasis).toOption.get,
      InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
    )

  private def _human_input(
    reviewedmanifest: Array[Byte],
    candidatebytes: Array[Byte],
    reviewbytes: Array[Byte],
    diffbytes: Array[Byte],
    options: FixtureOptions
  ): InternalModelCandidateHumanApprovalInput = {
    val projection = _projection(options)
    val semanticdiff = _semantic_diff(projection, candidatebytes, options)
    InternalModelCandidateHumanApprovalInput(
      InternalModelDecisionActor("human", "human-1", "reviewer"), options.approvalidentity, options.approvalrevision,
      InternalModelCandidateHumanApprovalBasis(
        _review_artifact(options.candidateartifactid, candidatebytes), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision,
        _review_artifact(options.reviewartifactid, reviewbytes), options.reviewidentity, options.reviewrevision, _package_basis(reviewedmanifest), projection.scope,
        _review_artifact(options.diffartifactid, diffbytes), semanticdiff.semanticDiffIdentity, semanticdiff.semanticDiffRevision
      ), options.decision,
      _source("human-decision", "human-1", Some("review/approval"), Some("1"), _digest("human-decision")),
      "The basis was considered.", Vector("follow-up", "later", "follow-up")
    )
  }

  private def _package_basis(bytes: Array[Byte]): InternalModelCandidateHumanApprovalPackageBasis = {
    val root = io.circe.parser.parse(new String(bytes, StandardCharsets.UTF_8)).toOption.flatMap(_.asObject).get
    InternalModelCandidateHumanApprovalPackageBasis(root("packageDigest").flatMap(_.asString).get, root("packageId").flatMap(_.asString).get, root("projectId").flatMap(_.asString).get, root("projectNamespace").flatMap(_.asString).get, root("revision").flatMap(_.asNumber).flatMap(_.toLong).get, root("schemaVersion").flatMap(_.asString).get)
  }

  private def _projection(options: FixtureOptions): InternalModelCandidateCmlProjection = {
    val alpha = _target("snapshot-cml-alpha", "patch-alpha", "target-alpha", _cml_alpha_source, "cml/alpha.cml", Vector[Byte](0, -1, 10), "mapping-alpha", "opaque-shared", "element", "effect-alpha", "ref-opaque-element-kind-Mono", options.candidatecontentmarker)
    val beta = _target("snapshot-cml-beta", "patch-beta", "target-beta", _cml_beta_source, "cml/beta.cml", _cml_beta_raw, "mapping-beta", "opaque-shared", "relationship", "effect-beta", "ref-opaque-relationship-role-StructuralDomain", options.candidatecontentmarker)
    InternalModelCandidateCmlProjection(options.candidateidentity, options.candidatemodelidentity, options.candidaterevision, "projection-main", "ccdm-candidate-cml-projection-v1", "realization-main", "1.0", _scope(options), Vector(alpha, beta), Vector.empty)
  }

  private def _target(baselineid: String, patchid: String, targetid: String, source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte], mappingid: String, identity: String, kind: String, effectprefix: String, referenceid: String, marker: String): InternalModelCandidateCmlTarget = {
    val assertionid = if kind == "element" then "a-opaque-element-kind-Mono" else "a-opaque-relationship-role-StructuralDomain"
    val conditionid = if kind == "element" then "c-opaque-element" else "c-opaque-relationship"
    val enrichmentid = if kind == "element" then "z-opaque-element-enrichment" else "z-opaque-relationship-enrichment"
    val mapping = InternalModelCandidateCmlMapping(Vector(assertionid), "anchor-" + targetid, "cml-" + targetid, Vector(conditionid), Vector(enrichmentid), mappingid, identity, kind)
    val effects = Vector(InternalModelCandidateCmlEffect("unknown", "supplied compatibility expectation" + marker, effectprefix + "-compatibility", "compatibility", Vector(mappingid), referenceid), InternalModelCandidateCmlEffect("unknown", "supplied migration expectation" + marker, effectprefix + "-migration", "migration", Vector(mappingid), referenceid))
    InternalModelCandidateCmlTarget(baselineid, effects, Vector(mapping), patchid, path, _content(rawbytes.toArray), source, targetid)
  }

  private def _review_binding(reviewedmanifest: Array[Byte], realization: Array[Byte], continuity: Array[Byte], candidate: Array[Byte], diff: Array[Byte], basis: InternalModelCandidateReviewExecutionBasis, options: FixtureOptions): InternalModelCandidateReviewBinding = {
    val projection = _projection(options)
    val semanticdiff = _semantic_diff(projection, candidate, options)
    InternalModelCandidateReviewBinding(
      _review_artifact(options.candidateartifactid, candidate), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision,
      _review_artifact("projection-main", continuity), Vector(_review_artifact("evidence-alpha", "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8)), _review_artifact("evidence-beta", "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8))),
      "ccdm-candidate-review-binding-v1", basis.providers, _review_artifact("realization-main", realization), "realization-1.0", options.reviewidentity, options.reviewrevision,
      _content(reviewedmanifest), basis.rules, "1.0", projection.scope, _review_artifact(options.diffartifactid, diff), semanticdiff.semanticDiffIdentity, semanticdiff.semanticDiffRevision,
      Vector(_review_target("target-alpha", "patch-alpha", "evidence-alpha", options), _review_target("target-beta", "patch-beta", "evidence-beta", options)), Vector.empty
    )
  }

  private def _review_target(targetid: String, patchid: String, evidenceid: String, options: FixtureOptions): InternalModelCandidateReviewTarget =
    InternalModelCandidateReviewTarget(Vector(evidenceid), CandidateDesignReviewSnapshot("snapshot-review-" + targetid, MonoKotoProjectionContextIdentity(options.projectioncontextidentity), ComponentDashboardComponentIdentity(options.componentidentity), patchid, options.candidatemodelidentity, "reviewed", ComponentDashboardSourceAttribution("review-source", "review-authority", "review-locator"), ComponentDashboardCondition("available", "authorized", Some("redacted"), Some("explicit absence"), Some("ambiguous"), Some("conflicting"), Some("stale"), Some("malformed"), Vector("condition limitation", "condition limitation")), Vector("snapshot limitation", "snapshot limitation") ++ Option.when(options.reviewcontentmarker.nonEmpty)(options.reviewcontentmarker).toVector, Some("tie-" + targetid)), targetid)

  private def _basis(options: FixtureOptions): InternalModelCandidateReviewExecutionBasis =
    InternalModelCandidateReviewExecutionBasis(Vector(InternalModelCandidateReviewRule(options.ruleid, options.ruleversion, _digest(options.rulehashseed)), InternalModelCandidateReviewRule("rule-beta", "2.0", _digest("rule-beta"))), Vector(InternalModelCandidateReviewProvider(options.providerid, options.providerversion, _digest(options.providerhashseed)), InternalModelCandidateReviewProvider("provider-beta", "2.0", _digest("provider-beta"))))

  private def _semantic_diff(projection: InternalModelCandidateCmlProjection, candidatebytes: Array[Byte], options: FixtureOptions): InternalModelSemanticDiff = {
    val targets = projection.targets.map { target =>
      val mapping = target.mappings.head
      val condition = _condition("available", "authorized", None, None, None, None, None, None, Vector("condition limitation"))
      val entry = CandidateDesignSemanticDiffEntry("entry-" + target.targetId, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.patchIdentity, projection.candidateModelIdentity, "category", "action", mapping.semanticIdentity, Some("before"), Some("after"), "relationship", _attribution("entry-authority-" + target.targetId), condition, Vector("entry limitation") ++ Option.when(options.diffcontentmarker.nonEmpty)(options.diffcontentmarker).toVector, Some("entry tie"))
      val trace = CandidateDesignProposedCmlPatchTrace(target.patchIdentity, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.source.authority, target.source.locator.getOrElse("patch-locator-" + target.targetId), target.source.sha256, target.proposedContent.sha256, _attribution("patch-authority-" + target.targetId), condition, Vector("patch limitation") ++ Option.when(options.diffcontentmarker.nonEmpty)(options.diffcontentmarker).toVector, Some("patch tie"))
      InternalModelSemanticDiffTarget(Vector(InternalModelSemanticDiffMappedEntry(entry, mapping.mappingId, mapping.semanticIdentityKind)), trace, target.targetId)
    }
    InternalModelSemanticDiff(options.candidateartifactid, _sha256(candidatebytes), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision, "ccdm-semantic-diff-v1", "1.0", projection.scope, options.diffidentity, options.diffrevision, targets, Vector.empty)
  }

  private def _model_snapshot(options: FixtureOptions): Array[Byte] = {
    val facts = Vector(_fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono", options), _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case", options), _fact("element", "e-usecase-current", "anchor-e-usecase-current-kind-use-case", "kind:use-case", options), _fact("element", "opaque-shared", "anchor-opaque-element-enrichment", "enrichment:shared", options), _fact("element", "opaque-shared", "anchor-opaque-element-kind-Mono", "kind:Mono", options), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-enrichment", "enrichment:shared", options), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", options), _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", options)).sortBy(value => (value.hcursor.get[String]("semanticIdentityKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("sourceAnchor").toOption.get))
    _canonical(Json.obj("basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source_json(_model_source)))
  }

  private def _scenario_snapshot(): Array[Byte] =
    _canonical(Json.obj("basis" -> Json.obj("content" -> Json.fromString("Scenario content"), "scenarioId" -> Json.fromString("scenario-main"), "traceLinks" -> Json.arr()), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("scenario"), "source" -> _source_json(_scenario_source)))

  private def _glossary_snapshot(): Array[Byte] =
    _canonical(Json.obj("basis" -> Json.obj("entries" -> Json.arr(Json.obj("definition" -> Json.fromString("Glossary definition"), "limitations" -> Json.arr(), "sourceAnchor" -> Json.fromString("term-anchor"), "termIdentity" -> Json.fromString("term-main"), "termLabel" -> Json.fromString("Term")))), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("glossary-bok"), "source" -> _source_json(_glossary_source)))

  private def _cml_snapshot(source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte]): Array[Byte] =
    _canonical(Json.obj("basis" -> Json.obj("byteLength" -> Json.fromLong(rawbytes.length.toLong), "projectRelativePath" -> Json.fromString(path), "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes.toArray))), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("cml-baseline"), "source" -> _source_json(source)))

  private def _realization(options: FixtureOptions): Array[Byte] = {
    val assertions = Vector(_assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", Vector.empty), _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", Vector.empty), _assertion("a-e-usecase-current-kind-use-case", "element", "e-usecase-current", "ref-e-usecase-current-kind-use-case", "kind:use-case", Vector.empty), _assertion("a-opaque-element-kind-Mono", "element", "opaque-shared", "ref-opaque-element-kind-Mono", "kind:Mono", Vector("c-opaque-element")), _assertion("a-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "ref-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", Vector("c-opaque-relationship")), _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", Vector.empty)).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val enrichment = Vector(_assertion("z-opaque-element-enrichment", "element", "opaque-shared", "ref-opaque-element-enrichment", "enrichment:shared", Vector("c-opaque-element")), _assertion("z-opaque-relationship-enrichment", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "enrichment:shared", Vector("c-opaque-relationship"))).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    _canonical(Json.obj("canonicalAssertions" -> Json.fromValues(assertions), "conditions" -> Json.fromValues(Vector(_condition_json("c-opaque-element", "limitation", "element", "opaque-shared", "ref-opaque-element-enrichment", "element evidence remains bounded"), _condition_json("c-opaque-relationship", "limitation", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "relationship evidence remains bounded"))), "elements" -> Json.fromValues(Vector(_element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")), _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")), _element("e-usecase-current", "use-case", "Current use case", Vector("a-e-usecase-current-kind-use-case")), _element("opaque-shared", "Mono", "Opaque shared element", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element")))), "enrichmentAssertions" -> Json.fromValues(enrichment), "profile" -> Json.fromString("ccdm-realization-v1"), "realizationIdentity" -> Json.fromString("realization-1.0"), "relationships" -> Json.fromValues(Vector(_relationship("opaque-shared", "StructuralDomain", "e-mono", "e-usecase", Vector("a-opaque-relationship-role-StructuralDomain"), Vector("z-opaque-relationship-enrichment"), Vector("c-opaque-relationship")), _relationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase", Vector("a-r-mono-domain-role-StructuralDomain")))), "schemaVersion" -> Json.fromString("1.0"), "scope" -> _scope_json(options), "sourceReferences" -> Json.fromValues(Vector(_reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"), _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"), _reference("ref-e-usecase-current-kind-use-case", "element", "e-usecase-current", "anchor-e-usecase-current-kind-use-case"), _reference("ref-opaque-element-enrichment", "element", "opaque-shared", "anchor-opaque-element-enrichment"), _reference("ref-opaque-element-kind-Mono", "element", "opaque-shared", "anchor-opaque-element-kind-Mono"), _reference("ref-opaque-relationship-enrichment", "relationship", "opaque-shared", "anchor-opaque-relationship-enrichment"), _reference("ref-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain"), _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain")).sortBy(_.hcursor.get[String]("referenceId").toOption.get)), "successorLinks" -> Json.arr(), "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))))
  }

  private def _binding(options: FixtureOptions): Array[Byte] = {
    def _record_(kind: String, identity: String, role: String, ids: Vector[String]): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(ids.map(Json.fromString)), "conditionIds" -> Json.arr(), "enrichmentAssertionIds" -> Json.arr(), "recordKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "sequenceAssertionId" -> Json.Null, "viewRole" -> Json.fromString(role))
    val families = Vector("MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono")), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))), "UseCaseCommunicationProjection" -> Vector(_record_("element", options.selectedusecaseelementidentity, "use-case", Vector("a-" + options.selectedusecaseelementidentity + "-kind-use-case"))), "EntityModelProjection" -> Vector.empty[Json], "EventModelProjection" -> Vector.empty[Json], "StructureViewProjection" -> Vector.empty[Json], "ClassificationViewProjection" -> Vector.empty[Json], "WorkflowProjection" -> Vector.empty[Json], "StateMachineProjection" -> Vector.empty[Json])
    _canonical(Json.obj("profile" -> Json.fromString("ccdm-projection-binding-v1"), "realizationArtifactId" -> Json.fromString("realization-main"), "schemaVersion" -> Json.fromString("1.0"), "scope" -> _scope_json(options), "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })))
  }

  private def _entry(id: String, role: String, path: String, required: Boolean, bytes: Array[Byte], dependencies: Vector[String]): Json =
    Json.obj("artifactId" -> Json.fromString(id), "dependsOn" -> Json.fromValues(dependencies.sorted.map(Json.fromString)), "path" -> Json.fromString(path), "required" -> Json.fromBoolean(required), "role" -> Json.fromString(role), "sha256" -> Json.fromString(_sha256(bytes)))

  private def _topological(entries: Vector[Json]): Vector[Json] = {
    val byid = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value).toMap
    val dependencies = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value.hcursor.get[Vector[String]]("dependsOn").toOption.get.toSet).toMap
    @annotation.tailrec
    def _loop_(pending: Map[String, Set[String]], completed: Vector[String]): Vector[String] =
      if pending.isEmpty then completed
      else {
        val next = pending.collect { case (id, deps) if deps.isEmpty => id }.toVector.sorted.head
        _loop_(pending.removed(next).view.mapValues(_ - next).toMap, completed :+ next)
      }
    _loop_(dependencies, Vector.empty).map(byid)
  }

  private def _manifest(entries: Vector[Json], revision: Long, options: FixtureOptions): Array[Byte] = {
    val root = JsonObject.fromIterable(Vector("artifacts" -> Json.fromValues(entries), "lifecycleState" -> Json.fromString("draft"), "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)), "packageId" -> Json.fromString(options.packageid), "projectId" -> Json.fromString(options.projectid), "projectNamespace" -> Json.fromString(options.projectnamespace), "revision" -> Json.fromLong(revision), "schemaVersion" -> Json.fromString("1.0")))
    _canonical(root.add("packageDigest", Json.fromString(_sha256(_canonical(root.remove("packageDigest").toJson)))).toJson)
  }

  private def _fact(kind: String, identity: String, anchor: String, content: String, options: FixtureOptions): Json =
    Json.obj("componentIdentity" -> Json.fromString(options.componentidentity), "content" -> Json.fromString(content), "limitations" -> Json.arr(), "projectionContextIdentity" -> Json.fromString(options.projectioncontextidentity), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceAnchor" -> Json.fromString(anchor))

  private def _assertion(id: String, kind: String, identity: String, referenceid: String, content: String, conditionids: Vector[String]): Json =
    Json.obj("assertionId" -> Json.fromString(id), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "content" -> Json.fromString(content), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid))

  private def _element(identity: String, kind: String, label: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json =
    Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "kind" -> Json.fromString(kind), "label" -> Json.fromString(label))

  private def _relationship(identity: String, role: String, source: String, target: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json =
    Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "direction" -> Json.fromString("source-to-target"), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "label" -> Json.fromString(identity), "role" -> Json.fromString(role), "sourceElementIdentity" -> Json.fromString(source), "targetElementIdentity" -> Json.fromString(target))

  private def _reference(id: String, kind: String, identity: String, anchor: String): Json =
    Json.obj("referenceId" -> Json.fromString(id), "snapshotArtifactId" -> Json.fromString("snapshot-model"), "source" -> _source_json(_model_source), "sourceAnchor" -> Json.fromString(anchor), "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind)))

  private def _condition_json(id: String, kind: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj("affectedIdentity" -> Json.fromString(affectedidentity), "affectedKind" -> Json.fromString(affectedkind), "conditionId" -> Json.fromString(id), "detail" -> Json.fromString(detail), "kind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid))

  private def _content(bytes: Array[Byte]): InternalModelCandidateCmlContent =
    InternalModelCandidateCmlContent(bytes.length.toLong, Base64.getEncoder.encodeToString(bytes), _sha256(bytes))

  private def _review_artifact(id: String, bytes: Array[Byte]): InternalModelCandidateReviewArtifact = InternalModelCandidateReviewArtifact(id, _sha256(bytes))
  private def _attribution(authority: String): ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution("source-id", authority, "source-locator")
  private def _condition(availability: String, authorization: String, redaction: Option[String], absence: Option[String], ambiguity: Option[String], conflict: Option[String], staleness: Option[String], malformed: Option[String], limitations: Vector[String]): ComponentDashboardCondition = ComponentDashboardCondition(availability, authorization, redaction, absence, ambiguity, conflict, staleness, malformed, limitations)
  private def _source(authority: String, identity: String, locator: Option[String], revision: Option[String], sha256: String): InternalModelSemanticSource = InternalModelSemanticSource(authority, identity, locator, revision, sha256)
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority), "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null), "sha256" -> Json.fromString(value.sha256))
  private def _scope(options: FixtureOptions): InternalModelSemanticScope =
    InternalModelSemanticScope(options.componentidentity, options.projectioncontextidentity, options.selectedusecaseelementidentity)

  private def _project_yaml(options: FixtureOptions): String =
    s"project:\n  namespace: ${options.projectnamespace}\n  id: ${options.projectid}\n"

  private def _scope_json(options: FixtureOptions): Json =
    Json.obj("componentIdentity" -> Json.fromString(options.componentidentity), "projectionContextIdentity" -> Json.fromString(options.projectioncontextidentity), "selectedUseCaseElementIdentity" -> Json.fromString(options.selectedusecaseelementidentity))
  private def _canonical(json: Json): Array[Byte] = (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
  private def _sort_ids(values: Vector[String]): Vector[String] = values.sortWith((left, right) => java.util.Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)) < 0)
  private def _sha256(bytes: Array[Byte]): String = "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
  private def _digest(seed: String): String = _sha256(seed.getBytes(StandardCharsets.UTF_8))
  private def _temporary_root(): Path = { Files.createDirectories(Path.of("target")); Files.createTempDirectory(Path.of("target"), "internal-model-candidate-approval-lifecycle-") }
  private def _write(path: Path, bytes: Array[Byte]): Unit = { Files.createDirectories(path.getParent); Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) }
  private def _delete_tree(root: Path): Unit = if Files.exists(root) then { val stream = Files.walk(root); try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists) finally stream.close() }
}
