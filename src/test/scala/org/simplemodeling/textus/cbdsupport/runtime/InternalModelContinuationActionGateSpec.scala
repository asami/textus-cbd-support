package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path}
import scala.jdk.CollectionConverters.*
import io.circe.{Json, JsonObject, Printer}
import io.circe.parser.parse
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Executable recorded-action contract; authoring does not establish validation.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  2, 2026
 */
final class InternalModelContinuationActionGateSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelCandidateHumanApprovalValidatorSpec.{FixtureOptions, FixtureData, artifactReference, recordReference, reference, approvalRecord}
  import InternalModelContinuationEligibility.*
  import InternalModelContinuationProblemKind.*

  private val _rows = Vector(
    "model-ready" -> InternalModelContinuationAction.InspectProjections,
    "candidate-ready" -> InternalModelContinuationAction.ReviewCandidate,
    "review-ready" -> InternalModelContinuationAction.RequestHumanDecision,
    "human-decision-recorded" -> InternalModelContinuationAction.HandoffApprovedCandidate
  )

  "Recorded continuation eligibility" should {
    "the exact closed matrix" which {
      "A1 return only the recorded action in each complete independently admitted row" in {
        Given("one complete two-target capture and independently supplied requirements, review basis and actual human input")
        _with_rich() { (root, data, request) =>
          val captured = _capture(root)
          _rows.foreach { case (stage, action) =>
            val input = _cursor_change(captured, _.copy(currentStage = stage, nextPermittedAction = Some(action.token)))
            When("each exact stage/action row is evaluated without performing its action")
            val report = _report(input, request)
            Then("only that action is eligible and all complete admitted evidence remains available")
            report.eligibility shouldBe Eligible
            report.action shouldBe Some(action)
            report.state.continuity shouldBe InternalModelRehydrationValidator.validateVerified(input).toOption.get.continuity
            report.candidate.get.projection.targets.map(_.targetId) shouldBe Vector("target-alpha", "target-beta")
            report.review.get.binding shouldBe data.reviewbinding
            report.approval.get.record shouldBe approvalRecord(data.expected)
            report.decisions.map(_.artifactreference) shouldBe Vector(artifactReference("decision-main"))
            report.openissues.map(_.artifactreference) shouldBe Vector(artifactReference("open-issue-main"))
            report.problems shouldBe empty
          }
        }
      }

      "A2 preserve independent producer IDs revisions source versions mapping IDs and presentation" in {
        forAll(Gen.choose(1L, 9999L), Gen.choose(10001L, 19999L), Gen.choose(20001L, 29999L), Gen.alphaNumStr.suchThat(_.nonEmpty)) { (logical, artifact, carrier, suffix) =>
          Given("independently allocated logical/artifact/carrier references and a known source-owned version")
          val options = FixtureOptions(candidateidentity = "candidate-" + suffix, candidaterevision = logical,
            candidateartifactid = "candidate-" + suffix, candidateartifactrevision = artifact, carrierrevision = carrier,
            diffrevision = logical + 1, reviewrevision = logical + 2, approvalrevision = logical + 3,
            diffartifactrevision = artifact + 1, reviewartifactrevision = artifact + 2, approvalartifactrevision = artifact + 3,
            approvalchange = value => value.copy(unresolvedItems = Vector.empty))
          _with_rich(options) { (root, _, request) =>
            val versioned = _source_versions(_capture(root), "source-version-" + suffix)
            val mappingids = Map("mapping-alpha" -> ("mapping-alpha-" + suffix), "mapping-beta" -> ("mapping-beta-" + suffix))
            val renamed = _map_artifacts(versioned, Set(options.candidateartifactid, options.diffartifactid), value => _rename_mappings(value, mappingids))
            val independent = request.copy(requiredmappings = request.requiredmappings.map(_.map(value => value.copy(mappingid = mappingids(value.mappingid)))))
            val presented = _map_artifacts(renamed, renamed.artifacts.filter(_.bytes.nonEmpty).map(_.context.reference.artifactId.value).toSet,
              InternalModelCandidateHumanApprovalValidatorSpec.reverseKeys, pretty = true)
            When("the pure gate admits changed JSON layout and declared versions with independent requirements")
            val report = _report(presented, independent)
            Then("declared domains stay independent and layout or prose grants no additional permission")
            report.eligibility shouldBe Eligible
            report.action shouldBe Some(InternalModelContinuationAction.HandoffApprovedCandidate)
            report.state.packageContext.revision shouldBe carrier
            report.candidate.get.projection.candidateReference.recordRevision.value shouldBe logical
            report.candidate.get.candidateArtifactReference.artifactRevision.value shouldBe artifact
            report.candidate.get.projection.targets.flatMap(_.mappings).map(_.mappingId) shouldBe mappingids.values.toVector.sorted
          }
        }
      }

      "A2 preserve exact semantic identities across generated display and encounter-order variation" in {
        forAll(Gen.alphaNumStr.suchThat(_.nonEmpty), Gen.oneOf(true, false)) { (label, reversed) =>
          Given("the substantive all-eight factory with varied labels and source encounter order")
          InternalModelContinuationFixture.withFixture(label, reverseInputs = reversed) { root =>
            val captured = _core_capture(root)
            val independent = _core_request(captured)
            When("its exact model-ready action is independently evaluated")
            val report = _report(captured, independent)
            Then("display and encounter order neither replace identity nor grant a different capability")
            report.eligibility shouldBe Eligible
            report.action shouldBe Some(InternalModelContinuationAction.InspectProjections)
            report.state.continuity.realization.realizationReference shouldBe recordReference("realization-all-eight", 5L)
            report.state.continuity.realization.elements.map(_.identity) should contain("e-workflow")
          }
        }
      }

      "A3 reject every mismatched unsupported missing and unsupported-last action without substitution" in {
        Given("all sixteen recorded pairs plus unsupported and missing tokens")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          _rows.foreach { case (stage, expected) => _rows.map(_._2).foreach { action =>
            When("a recorded pair crosses the exact matrix")
            val report = _report(_cursor_change(captured, _.copy(currentStage = stage, nextPermittedAction = Some(action.token))), request)
            Then("only a same-row pair can yield its exact action")
            report.eligibility shouldBe (if (action == expected) Eligible else Inconsistent)
            report.action shouldBe (if (action == expected) Some(expected) else None)
            if (action != expected) report.problems.map(_.kind) should contain(StageActionMismatch)
          }}
          Vector(
            ((value: InternalModelResumeCursor) => value.copy(currentStage = "future-stage"), UnsupportedStage),
            ((value: InternalModelResumeCursor) => value.copy(nextPermittedAction = Some("future-action")), UnsupportedAction),
            ((value: InternalModelResumeCursor) => value.copy(lastCompletedAction = Some("future-action")), UnsupportedAction),
            ((value: InternalModelResumeCursor) => value.copy(nextPermittedAction = None), MissingPrerequisite)
          ).foreach { case (change, kind) =>
            When("a missing or unsupported recorded token is evaluated")
            val report = _report(_cursor_change(captured, change), request)
            Then("the typed reason remains and no preceding or substitute action is invented")
            report.problems.map(_.kind) should contain(kind)
            report.action shouldBe None
          }
        }
      }
    }

    "independent identity and exact selection" which {
      "A4 retain every independent package project carrier realization and scope contradiction" in {
        Given("a valid capture and seven independently supplied contradictory dimensions")
        _with_rich() { (root, _, request) =>
          val independent = request.copy(packagereference = request.packagereference.copy(
            packageId = InternalModelPackageId.from("ffffffff-ffff-ffff-ffff-ffffffffffff").toOption.get,
            projectNamespace = InternalModelProjectToken.from("other.namespace").toOption.get,
            projectId = InternalModelProjectToken.from("other-project").toOption.get), carrierrevision = request.carrierrevision + 1,
            realizationreference = recordReference("other-realization", 999L), scope = request.scope.copy(componentIdentity = "other-component"))
          When("the independent expected basis is cross-bound with actual capture")
          val report = _report(_capture(root), independent)
          Then("all contradictions retain dimensions and logical/full references with no action")
          report.eligibility shouldBe Inconsistent
          report.action shouldBe None
          report.problems.map(_.dimension) should contain allOf("packageId", "projectNamespace", "projectId", "carrierrevision", "realizationreference.recordId", "realizationreference.recordRevision", "scope")
          report.problems.find(_.dimension == "realizationreference.recordRevision").get.recordreference shouldBe Some(independent.realizationreference)
        }
      }

      "A5 reject wrong versions IDs absent selections and every gate-consumed cursor omission" in {
        Given("exact independent selections and the complete transitive captured basis")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          val changed = Vector(
            request.copy(candidateartifact = request.candidateartifact.map(value => value.copy(artifactRevision = InternalModelArtifactRevision.from(999L).toOption.get))),
            request.copy(candidateartifact = Some(reference("candidate-unknown", 17L, InternalModelArtifactRole.Projection))),
            request.copy(reviewartifact = request.reviewartifact.map(value => value.copy(artifactRevision = InternalModelArtifactRevision.from(999L).toOption.get)))
          )
          changed.foreach { independent =>
            When("a well-formed full independent selection differs from inventory")
            val report = _report(captured, independent)
            Then("a sibling or latest version is never inferred")
            report.eligibility shouldBe Inconsistent
            report.action shouldBe None
            report.problems.exists(value => Set(RevisionMismatch, SelectionMismatch).contains(value.kind)) shouldBe true
          }
          Vector("candidate-main", "semantic-diff-main", "review-main", "approval-main", "decision-main", "open-issue-main",
            "evidence-alpha", "evidence-beta", "snapshot-cml-alpha", "snapshot-cml-beta", "snapshot-scenario", "snapshot-glossary").foreach { id =>
            val omitted = _cursor_change(captured, value => value.copy(selectedArtifacts = value.selectedArtifacts.filterNot(_.artifactId.value == id)))
            When("one non-core consumed full reference is omitted from cursor selections")
            val report = _report(omitted, request)
            Then("the exact omitted reference remains attributed even for transitive review evidence")
            report.eligibility shouldBe Inconsistent
            report.problems.filter(_.dimension == "cursor.selectedArtifacts").flatMap(_.artifactreference).map(_.artifactId.value) should contain(id)
          }
          Vector("snapshot-model", "realization-main", "projection-main").foreach { id =>
            When("a reconstruction-core full reference is omitted")
            val result = InternalModelContinuationActionGate.evaluateVerified(_cursor_change(captured, value => value.copy(selectedArtifacts = value.selectedArtifacts.filterNot(_.artifactId.value == id))), request)
            Then("the existing structured core failure propagates without a fabricated report")
            result.isSuccess shouldBe false
          }
        }
        _with_rich(FixtureOptions(approvalpresent = false, approvalrequired = false)) { (root, _, request) =>
          When("an independently selected approval contradicts its optional absence")
          val report = _report(_capture(root), request)
          Then("absence contradicting an explicit selection remains inconsistent")
          report.problems.map(_.kind) should contain(SelectionMismatch)
        }
      }
    }

    "independently declared decision and mapping requirements" which {
      "A6 distinguish unsupplied empty missing and contradictory exact decision requirements" in {
        Given("actual admitted decision accounting and separately supplied requirement sets")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          val requirement = request.requireddecisions.get.head
          When("None and an explicitly empty owner-supplied set are evaluated")
          val missing = _report(captured, request.copy(requireddecisions = None))
          val empty = _report(captured, request.copy(requireddecisions = Some(Vector.empty)))
          Then("an empty persisted ledger never supplies the missing independent set")
          missing.eligibility shouldBe Incomplete
          empty.eligibility shouldBe Eligible
          val variants = Vector(
            requirement.copy(artifactreference = reference("missing-decision", 47L, InternalModelArtifactRole.Decision)) -> MissingPrerequisite,
            requirement.copy(recordreference = recordReference("missing-record", 211L)) -> MissingPrerequisite,
            requirement.copy(topicidentity = "missing-topic") -> MissingPrerequisite,
            requirement.copy(recordreference = recordReference(requirement.recordreference.recordId.value, 999L)) -> RevisionMismatch,
            requirement.copy(choiceidentity = "other-choice") -> DecisionMismatch,
            requirement.copy(affectedtargets = Vector(InternalModelSemanticTarget("element", "e-usecase"))) -> DecisionMismatch
          )
          variants.foreach { case (changed, kind) =>
            When("one exact required decision dimension is absent or contradictory")
            val report = _report(captured, request.copy(requireddecisions = Some(Vector(changed))))
            Then("the full required artifact and logical record remain attributed without choosing a sibling")
            report.problems.map(_.kind) should contain(kind)
            report.problems.filter(_.kind == kind).flatMap(_.artifactreference) should contain(changed.artifactreference)
            report.problems.filter(_.kind == kind).flatMap(_.recordreference) should contain(changed.recordreference)
          }
        }
      }

      "A6 retain valid supersession history while refusing a historical or superseded required record" in {
        Given("valid current terminal decisions with a retained current or historical predecessor")
        _with_rich() { (root, data, request) =>
          Vector(false, true).foreach { historical =>
            val current = _decision(data)
            val old = current.copy(decisionReference = recordReference("decision-old", 197L), state = InternalModelDecisionState.Superseded,
              basis = if (historical) current.basis.copy(realizationArtifactReference = reference("realization-old", 12L, InternalModelArtifactRole.Realization),
                realizationReference = recordReference("realization-old", 18L), status = InternalModelDecisionBasisStatus.HistoricalUnverified) else current.basis)
            val successor = current.copy(supersedes = Some(old.decisionReference))
            val ledger = _decision_ledger(data).copy(records = Vector(old, successor))
            val captured = _bytes_change(_capture(root), "decision-main", InternalModelDecisionRecordCodec.encode(ledger).toVector)
            val requirement = request.requireddecisions.get.head.copy(recordreference = old.decisionReference)
            When("the independently required record points to the retained predecessor")
            val report = _report(captured, request.copy(requireddecisions = Some(Vector(requirement))))
            Then("history and rejected alternatives survive but are not promoted to current acceptance")
            report.eligibility shouldBe Inconsistent
            report.problems.map(_.kind) should contain(DecisionMismatch)
            report.decisions.head.admission.ledger.records should contain(old)
            report.decisions.head.admission.ledger.records.flatMap(_.rejectedAlternatives) should contain allElementsOf current.rejectedAlternatives
          }
          val contradictory = _decision_ledger(data).copy(records = Vector(_decision(data).copy(basis = _decision(data).basis.copy(status = InternalModelDecisionBasisStatus.HistoricalUnverified))))
          When("a current record claims a contradictory historical basis status")
          val report = _report(_bytes_change(_capture(root), "decision-main", InternalModelDecisionRecordCodec.encode(contradictory).toVector), request)
          Then("the original ledger owner diagnostic is retained as semantic failure")
          report.problems.map(_.kind) should contain(SemanticAdmissionFailed)
          report.problems.exists(_.diagnostic.contains("basis status")) shouldBe true
        }
      }

      "A7 distinguish unsupplied empty missing and contradictory exact mapping requirements" in {
        Given("an actually complete admitted two-target candidate and diff")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          When("the independent requirement set is absent or explicitly empty")
          val missing = _report(captured, request.copy(requiredmappings = None))
          val empty = _report(captured, request.copy(requiredmappings = Some(Vector.empty)))
          Then("explicit emptiness preserves actual candidate/diff admission while None is incomplete")
          missing.eligibility shouldBe Incomplete
          empty.eligibility shouldBe Eligible
          val original = request.requiredmappings.get.head
          Vector(
            original.copy(targetid = "missing-target") -> MissingPrerequisite,
            original.copy(mappingid = "missing-mapping") -> MissingPrerequisite,
            original.copy(semantictarget = InternalModelSemanticTarget("relationship", "opaque-shared")) -> MappingMismatch,
            original.copy(semantictarget = InternalModelSemanticTarget("element", "e-mono")) -> MappingMismatch
          ).foreach { case (requirement, kind) =>
            When("one independently required target mapping kind or identity is missing or different")
            val report = _report(captured, request.copy(requiredmappings = Some(Vector(requirement))))
            Then("a typed missing/contradictory mapping remains without a guessed mapping")
            report.problems.map(_.kind) should contain(kind)
            report.action shouldBe None
          }
          val malformed = _json_change(captured, "candidate-main", value => InternalModelCandidateHumanApprovalValidatorSpec.replacePath(value,
            Vector("targets", "0", "mappings", "0", "canonicalAssertionIds"), Json.arr()))
          When("actual upstream assertion links are incomplete even with an empty independent set")
          val report = _report(malformed, request.copy(requiredmappings = Some(Vector.empty)))
          Then("existing semantic owners reject rather than treating empty requirements as a shortcut")
          report.problems.map(_.kind) should contain(SemanticAdmissionFailed)
        }
      }
    }

    "all-present ledgers and source-owned versions" which {
      "A8 reject malformed present ledgers and hidden extra blocking issues" in {
        Given("every present decision/open-issue role is in the captured inventory")
        _with_rich() { (root, data, request) =>
          val captured = _capture(root)
          Vector("decision-main", "open-issue-main").foreach { id =>
            When("a present ledger contains a raw placeholder instead of current typed accounting")
            val report = _report(_bytes_change(captured, id, "raw ledger".getBytes(StandardCharsets.UTF_8).toVector), request)
            Then("its original semantic failure survives instead of ignoring the ledger")
            report.problems.filter(_.kind == SemanticAdmissionFailed).flatMap(_.artifactreference).map(_.artifactId.value) should contain(id)
          }
          val extra = reference("issue-hidden", 307L, InternalModelArtifactRole.OpenIssue)
          val ledger = _issue_ledger(data, blocking = true)
          val context = InternalModelVerifiedArtifactContext(extra, "open-issues/hidden.json", false, Vector(artifactReference("realization-main")), true)
          val artifacts = captured.artifacts :+ InternalModelVerifiedContinuationArtifact(context, Some(InternalModelOpenIssueRecordCodec.encode(ledger).toVector))
          val augmented = _inventory_change(captured, artifacts)
          val inspection = _cursor_change(augmented, _.copy(currentStage = "model-ready", nextPermittedAction = Some("inspect-projections")))
          val independent = request.copy(candidateartifact = None, semanticdiffartifact = None, reviewartifact = None, approvalartifact = None, executionbasis = None, humandecision = None, requiredmappings = None)
          When("an extra present issue is not selected while an inspection row is requested")
          val report = _report(inspection, independent)
          Then("the exact extra ledger is admitted and its hidden selection contradiction cannot be concealed")
          report.eligibility shouldBe Inconsistent
          report.openissues.map(_.artifactreference) should contain(extra)
          report.openissues.find(_.artifactreference == extra).get.admission.ledger.issues.head.blocking.semanticApproval shouldBe true
          report.problems.filter(_.kind == SelectionMismatch).flatMap(_.artifactreference) should contain(extra)
        }
      }

      "A9 require source.revision for every consumed snapshot despite positive control revisions" in {
        Vector("snapshot-model", "snapshot-scenario", "snapshot-glossary", "snapshot-cml-alpha", "snapshot-cml-beta").foreach { id =>
          Given("one consumed source snapshot with missing source-owned version and unchanged positive carrier/artifact versions")
          _with_rich(FixtureOptions(missingsourceversions = Set(id), approvalchange = value => value.copy(unresolvedItems = Vector.empty))) { (root, _, request) =>
            When("the gate checks captured source completeness without a live observation")
            val report = _report(_capture(root), request)
            Then("the exact snapshot and missing source.revision yield Incomplete")
            report.eligibility shouldBe Incomplete
            report.action shouldBe None
            report.problems.filter(_.dimension == "source.revision").flatMap(_.artifactreference).map(_.artifactId.value) shouldBe Vector(id)
            report.state.packageContext.revision shouldBe 43L
          }
        }
      }
    }

    "separate review and actual human evidence" which {
      "A10 require independent review versions and exact complete subject dependencies" in {
        Given("stored review evidence including provider-approved targets but separate independent execution basis")
        _with_rich(FixtureOptions(reviewstate = "approved", approvalchange = value => value.copy(unresolvedItems = Vector.empty))) { (root, data, request) =>
          val captured = _capture(root)
          When("independent rule/provider input is missing or different")
          val missing = _report(captured, request.copy(executionbasis = None))
          val changed = _report(captured, request.copy(executionbasis = Some(data.executionbasis.copy(rules = data.executionbasis.rules.map(value => value.copy(ruleVersion = "changed"))))))
          Then("persisted rules and provider approved state cannot supply it")
          missing.eligibility shouldBe Incomplete
          changed.eligibility shouldBe Inconsistent
          changed.problems.filter(_.dimension == "review.admission").head.diagnostic should include("independently admitted execution basis")
          Vector(
            ((value: InternalModelCandidateReviewBinding) => value.copy(subject = value.subject.copy(subjectRevision = InternalModelRecordRevision.from(777L).toOption.get))),
            ((value: InternalModelCandidateReviewBinding) => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.filterNot(_.artifactId.value == "snapshot-glossary")))),
            ((value: InternalModelCandidateReviewBinding) => value.copy(candidateReference = recordReference("wrong-candidate", 7L)))
          ).foreach { change =>
            val bytes = InternalModelCandidateReviewBindingCodec.encode(change(data.reviewbinding)).toVector
            When("the selected stored review subject or input set is changed")
            val report = _report(_bytes_change(captured, "review-main", bytes), request)
            Then("exact review or independent human binding detects the contradiction")
            report.eligibility shouldBe Inconsistent
            report.problems.map(_.kind) should contain(SemanticAdmissionFailed)
          }
          val artifact = captured.artifacts.find(_.context.reference.artifactId.value == "review-main").get
          val dependencies = artifact.context.dependencies.filterNot(_.artifactId.value == "snapshot-glossary")
          val changedinventory = _inventory_change(captured, captured.artifacts.map(value => if (value == artifact) value.copy(context = value.context.copy(dependencies = dependencies)) else value))
          When("actual review dependencies omit a complete subject member")
          val report = _report(changedinventory, request)
          Then("complete subject ownership rejects a subset")
          report.problems.map(_.kind) should contain(SemanticAdmissionFailed)
          val revision = captured.packageContext.revision + 1
          val recursor = _cursor_change(captured.copy(packageContext = captured.packageContext.copy(revision = revision)), _.copy(packageRevision = revision))
          When("only the carrier/cursor revision changes and independent caller admits it")
          val carried = _report(recursor, request.copy(carrierrevision = revision))
          Then("the unchanged exact subject remains admitted independently of carrier control")
          carried.eligibility shouldBe Eligible
          carried.review.get.binding.subject shouldBe data.reviewbinding.subject
        }
      }

      "A11 reject missing mismatched rejected changes-requested and unresolved actual human input" in {
        Given("actual separately supplied human input, not a decoded approval expectation")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          When("independent actual human input is missing or disagrees with stored provenance")
          val missing = _report(captured, request.copy(humandecision = None))
          val changed = _report(captured, request.copy(humandecision = request.humandecision.map(value => value.copy(actor = value.actor.copy(identity = "other-human")))))
          Then("the approval cannot authenticate itself or synthesize a human")
          missing.eligibility shouldBe Incomplete
          changed.eligibility shouldBe Inconsistent
          changed.problems.filter(_.dimension == "approval.admission").head.diagnostic should include("independently supplied actual human input")
        }
        Vector(InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested).foreach { decision =>
          Given("a matching persisted and independently supplied actual non-Approved decision")
          _with_rich(FixtureOptions(decision = decision, approvalchange = value => value.copy(unresolvedItems = Vector.empty))) { (root, _, request) =>
            When("the approved handoff row is evaluated")
            val report = _report(_capture(root), request)
            Then("the original non-Approved decision remains admitted and blocks the handoff")
            report.eligibility shouldBe Inconsistent
            report.approval.get.record.approval.decision shouldBe decision
            report.problems.map(_.kind) should contain(HumanDecisionNotApproved)
          }
        }
        Given("the original rich Approved fixture with ordered duplicate unresolved items")
        _with_rich(FixtureOptions()) { (root, data, request) =>
          When("the gate independently admits its actual human input")
          val report = _report(_capture(root), request)
          Then("unresolved items are retained and not recast as empty approval")
          report.eligibility shouldBe Inconsistent
          report.approval.get.record.approval.unresolvedItems shouldBe data.expected.unresolvedItems
          report.problems.map(_.kind) should contain(HumanUnresolvedItems)
        }
      }

      "A12 block semantic approval only at approved handoff while retaining every other issue facet" in {
        Given("an admitted issue with all four independent blocking facets and an independently Approved human")
        _with_rich(blocking = true) { (root, _, request) =>
          val captured = _capture(root)
          When("approved handoff is evaluated")
          val blocked = _report(captured, request)
          Then("semanticApproval blocks with full issue attribution and other facets preserved")
          blocked.eligibility shouldBe Inconsistent
          blocked.problems.map(_.kind) should contain(BlockingIssue)
          blocked.openissues.head.admission.ledger.issues.head.blocking shouldBe InternalModelOpenIssueBlocking(true, true, true, true)
          Vector(_rows.head, _rows(2)).foreach { case (stage, action) =>
            When("inspection or requesting a human decision uses the same unresolved evidence")
            val report = _report(_cursor_change(captured, _.copy(currentStage = stage, nextPermittedAction = Some(action.token))), request)
            Then("the non-writing row retains unresolved issues without resolving or clearing them")
            report.eligibility shouldBe Eligible
            report.openissues shouldBe blocked.openissues
          }
        }
      }
    }

    "closed independently derived condition meaning" which {
      "A13 interpret all nine codes in their proper lists with exact owners and prose inert" in {
        forAll(Gen.alphaNumStr, Gen.oneOf(InternalModelContinuationConditionList.Preconditions,
          InternalModelContinuationConditionList.InvalidationChecks, InternalModelContinuationConditionList.AcceptanceCriteria)) { (detail, origin) =>
          Given("the seven positive derived predicates in a positive list, and two cleared blockers")
          _with_rich() { (root, _, request) =>
            val captured = _capture(root)
            val positive = InternalModelContinuationCondition.values.filterNot(_.blocker).toVector.map(value =>
              InternalModelResumeCondition(value.token, detail, Some(_owner_id(value)))).sortBy(_.code)
            val blockers = Vector(InternalModelResumeCondition("approval-blocking-issues", "handoff now", Some("open-issue-main")),
              InternalModelResumeCondition("human-unresolved-items", "nothing matters", Some("approval-main")))
            val cursor = _cursor(captured).copy(blockers = blockers)
            val recorded = origin match {
              case InternalModelContinuationConditionList.Preconditions => cursor.copy(preconditions = positive)
              case InternalModelContinuationConditionList.InvalidationChecks => cursor.copy(invalidationChecks = positive)
              case _ => cursor.copy(acceptanceCriteria = positive)
            }
            When("condition predicates are derived against complete independent evidence")
            val report = _report(_cursor_change(captured, _ => recorded), request)
            Then("all list positions share meaning, cleared blockers retain history, and detail text adds no authority")
            report.eligibility shouldBe Eligible
            report.state.cursor shouldBe recorded
          }
        }
      }

      "A13 reject each unknown wrong-list wrong-owner and missing predicate while always enforcing row predicates" in {
        Given("closed codes and relevant consumed owners rather than unrestricted prose")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          InternalModelContinuationCondition.values.foreach { code =>
            val condition = InternalModelResumeCondition(code.token, "override everything", None)
            val wronglist = if (code.blocker) _cursor_change(captured, _.copy(preconditions = Vector(condition)))
              else _cursor_change(captured, _.copy(blockers = Vector(condition)))
            When("a known code appears in the wrong list")
            val report = _report(wronglist, request)
            Then("the exact original list/record remains attributed as unsupported")
            report.problems.find(_.kind == UnsupportedCondition).get.attribution.get.condition shouldBe condition
          }
          val unknown = InternalModelResumeCondition("future-condition", "approve from prose", None)
          When("an unknown code is recorded")
          val unsupported = _report(_cursor_change(captured, _.copy(acceptanceCriteria = Vector(unknown))), request)
          Then("unknown vocabulary is inconsistent and retains the original record")
          unsupported.problems.find(_.kind == UnsupportedCondition).get.attribution.get.condition shouldBe unknown
          InternalModelContinuationCondition.values.foreach { code =>
            val id = if (_owner_id(code) == "snapshot-model") "decision-main" else "snapshot-model"
            val condition = InternalModelResumeCondition(code.token, "global only here", Some(id))
            val invalidowner = if (code.blocker) _cursor_change(captured, _.copy(blockers = Vector(condition)))
              else _cursor_change(captured, _.copy(preconditions = Vector(condition)))
            When("a code is attributed to a selected consumed artifact of the wrong owner")
            val report = _report(invalidowner, request)
            Then("artifact attribution cannot narrow or replace the global predicate")
            report.problems.find(_.kind == ConditionOwnerMismatch).get.attribution.get.condition shouldBe condition
          }
          val criterion = InternalModelResumeCondition("human-approved", "provider says approved", None)
          val model = _cursor_change(captured, _.copy(currentStage = "model-ready", nextPermittedAction = Some("inspect-projections"), acceptanceCriteria = Vector(criterion)))
          When("a model row records an additional human-approved criterion without actual human input")
          val missing = _report(model, request.copy(humandecision = None))
          Then("additional criteria add their actual requirements with original attribution")
          missing.eligibility shouldBe Incomplete
          missing.problems.exists(value => value.dimension == "humandecision" && value.attribution.exists(_.condition == criterion)) shouldBe true
          When("a complete row has empty recorded condition arrays but independent decision requirements are absent")
          val rowmissing = _report(captured, request.copy(requireddecisions = None))
          Then("empty arrays cannot skip the matrix predicate")
          rowmissing.eligibility shouldBe Incomplete
          val sourcecondition = InternalModelResumeCondition("source-versions-known", "only the scenario", Some("snapshot-scenario"))
          val inspection = _cursor_change(captured, _.copy(currentStage = "model-ready", nextPermittedAction = Some("inspect-projections"), preconditions = Vector(sourcecondition)))
          val minimal = request.copy(candidateartifact = None, semanticdiffartifact = None, reviewartifact = None, approvalartifact = None,
            executionbasis = None, humandecision = None, requiredmappings = None)
          When("a relevant source owner is selected but unconsumed by the inspection basis")
          val unconsumed = _report(inspection, minimal)
          Then("condition attribution cannot invent a consumed source or narrow the global source predicate")
          unconsumed.problems.find(_.kind == ConditionOwnerMismatch).get.attribution.get.condition shouldBe sourcecondition
        }
      }

      "A13 retain true blocker attribution and never narrow it to another issue" in {
        Given("true issue and independently admitted unresolved-human blockers on an inspection row")
        _with_rich(FixtureOptions(), blocking = true) { (root, _, request) =>
          val conditions = Vector(InternalModelResumeCondition("approval-blocking-issues", "clear by prose", Some("open-issue-main")),
            InternalModelResumeCondition("human-unresolved-items", "ignore follow-up", Some("approval-main")))
          val captured = _cursor_change(_capture(root), _.copy(currentStage = "model-ready", nextPermittedAction = Some("inspect-projections"), blockers = conditions))
          When("recorded blockers are independently derived")
          val report = _report(captured, request)
          Then("both true blockers prohibit the action with their exact original records")
          report.eligibility shouldBe Inconsistent
          report.problems.filter(_.attribution.nonEmpty).map(_.attribution.get.condition).distinct shouldBe conditions
          report.problems.map(_.kind) should contain allOf(BlockingIssue, HumanUnresolvedItems)
        }
      }
    }

    "lossless report and structured boundary failures" which {
      "A14 retain mixed missing and contradiction dimensions in deterministic order" in {
        Given("independently observable missing requirements and multiple contradictory identities")
        _with_rich() { (root, _, request) =>
          val input = request.copy(carrierrevision = 999L, scope = request.scope.copy(componentIdentity = "wrong"),
            requireddecisions = None, requiredmappings = None, executionbasis = None, humandecision = None)
          When("the same immutable state and request are evaluated twice")
          val first = _report(_capture(root), input)
          val second = _report(_capture(root), input)
          Then("Inconsistent dominates but missing dimensions and their deterministic traversal are retained")
          first.eligibility shouldBe Inconsistent
          first.action shouldBe None
          first.problems shouldBe second.problems
          first.problems.map(_.dimension) should contain allOf("carrierrevision", "scope", "requireddecisions", "requiredmappings", "executionbasis", "humandecision")
          first.problems.filter(_.kind == MissingPrerequisite) should not be empty
          first.problems.take(2).map(_.dimension) shouldBe Vector("carrierrevision", "scope")
        }
      }

      "A15 reject null malformed invalid and duplicate request graphs with structured failure" in {
        Given("a valid capture and malformed request objects")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          val decision = request.requireddecisions.get.head
          val mapping = request.requiredmappings.get.head
          val inputs = Vector(null, request.copy(packagereference = null), request.copy(carrierrevision = 0L),
            request.copy(realizationreference = null), request.copy(scope = request.scope.copy(componentIdentity = " ")),
            request.copy(candidateartifact = null), request.copy(candidateartifact = Some(null)),
            request.copy(reviewartifact = request.candidateartifact), request.copy(semanticdiffartifact = request.candidateartifact),
            request.copy(executionbasis = Some(InternalModelCandidateReviewExecutionBasis(null, Vector.empty))),
            request.copy(humandecision = request.humandecision.map(_.copy(unresolvedItems = null))),
            request.copy(requireddecisions = Some(Vector(decision, decision))), request.copy(requireddecisions = Some(Vector(null))),
            request.copy(requiredmappings = Some(Vector(mapping, mapping))), request.copy(requiredmappings = Some(Vector(mapping.copy(mappingid = "\uD800")))))
          inputs.foreach { input =>
            When("request syntax crosses the structured gate boundary")
            val result = InternalModelContinuationActionGate.evaluateVerified(captured, input)
            Then("operationInvalid is returned without a success report or thrown domain exception")
            result.isSuccess shouldBe false
            result.toOption shouldBe None
            result.show should not be empty
          }
          When("null or malformed actual capture graphs cross the existing core admission")
          val failures = Vector(InternalModelContinuationActionGate.evaluateVerified(null, request),
            InternalModelContinuationActionGate.evaluateVerified(captured.copy(artifacts = null), request),
            InternalModelContinuationActionGate.evaluateVerified(captured.copy(packageContext = null), request))
          Then("existing core structured failures propagate")
          failures.map(_.isSuccess) shouldBe Vector(false, false, false)
        }
      }
    }

    "retained capture and complete evidence boundaries" which {
      "A16 preserve captured results after actual root removal and reject a fresh malformed replacement" in {
        Given("one actual capture with original canonical CML and an independently supplied request")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          val original = _report(captured, request)
          val originalcml = Files.readAllBytes(root.resolve("src/main/cml/original.cml")).toVector
          When("the rooted gate is evaluated and then the exact test-owned root is removed")
          val rooted = InternalModelContinuationActionGate.evaluate(root, request)
          rooted.toOption.get shouldBe original
          Files.readAllBytes(root.resolve("src/main/cml/original.cml")).toVector shouldBe originalcml
          _remove_owned_root(root)
          val retained = _report(captured, request)
          Then("pure admission retains the original capture and performs no filesystem provider chat or DB recovery")
          retained shouldBe original
          Files.exists(root, LinkOption.NOFOLLOW_LINKS) shouldBe false
          Given("a malformed replacement at that same test-owned root")
          _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: candidate-review-sample\n".getBytes(StandardCharsets.UTF_8).toVector)
          _write(root.resolve("src/main/internal-model/manifest.yaml"), "{}".getBytes(StandardCharsets.UTF_8).toVector)
          When("the rooted gate captures the replacement while the pure gate uses the original capture")
          val replacement = InternalModelContinuationActionGate.evaluate(root, request)
          val recaptured = _report(captured, request)
          Then("fresh malformed admission fails and retained pure semantics remain unchanged")
          replacement.isSuccess shouldBe false
          recaptured shouldBe original
        }
      }

      "A17 retain full binary targets diff review facets human provenance and ledger annotations" in {
        Given("nullable review facets, ordered duplicate limitations and full decision/issue annotations")
        _with_rich(FixtureOptions(allnullablefacets = true, approvalchange = value => value.copy(unresolvedItems = Vector.empty))) { (root, data, request) =>
          val captured = _capture(root)
          When("the complete approved handoff is independently admitted")
          val report = _report(captured, request)
          Then("full semantic values survive rather than counts strings or cleared conditions")
          report.eligibility shouldBe Eligible
          val candidate = report.candidate.get
          candidate.targetBytes.map(_.proposedRawBytes) shouldBe Vector(Vector[Byte](0, -1, 10), "beta cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector)
          candidate shouldBe report.semanticdiff.get.candidateAdmission
          report.semanticdiff.get shouldBe report.review.get.semanticDiffAdmission
          report.review.get.binding shouldBe data.reviewbinding
          report.approval.get.record.approval shouldBe data.expected
          report.decisions.head.admission.ledger shouldBe _decision_ledger(data)
          report.openissues.head.admission.ledger shouldBe _issue_ledger(data, blocking = false)
          report.state.artifacts shouldBe captured.artifacts
          report.state.continuity shouldBe candidate.continuity
          report.review.get.binding.targets.head.reviewSnapshot.limitations shouldBe Vector("snapshot limitation", "snapshot limitation")
        }
        Given("the unchanged substantive existing all-eight continuity fixture and explicit empty requirements")
        InternalModelContinuationFixture.withFixture() { root =>
          val captured = _core_capture(root)
          val independent = _core_request(captured)
          val original = InternalModelRehydrationValidator.validateVerified(captured).toOption.get
          When("its model-ready row crosses the gate")
          val report = _report(captured, independent)
          Then("the complete all-eight continuity source and condition values remain unchanged")
          report.eligibility shouldBe Eligible
          report.state.continuity shouldBe original.continuity
          report.state.continuity.binding.views.map(_.family) shouldBe Vector("MonoKotoProjection", "UseCaseCommunicationProjection", "EntityModelProjection", "EventModelProjection", "StructureViewProjection", "ClassificationViewProjection", "WorkflowProjection", "StateMachineProjection")
          report.state.continuity.realization.conditions shouldBe original.continuity.realization.conditions
        }
      }

      "A18 admit harmless current JSON presentation reject old records and infer no lifecycle or successor permission" in {
        Given("current typed records with harmless ordinary JSON presentation changes")
        _with_rich() { (root, _, request) =>
          val captured = _capture(root)
          val ids = captured.artifacts.filter(entry => entry.bytes.exists(bytes => parse(new String(bytes.toArray, StandardCharsets.UTF_8)).isRight)).map(_.context.reference.artifactId.value).toSet
          val presented = _map_artifacts(captured, ids, InternalModelCandidateHumanApprovalValidatorSpec.reverseKeys, pretty = true)
          def _old_schema_(id: String): InternalModelVerifiedContinuationPackage = {
            val artifacts = captured.artifacts.map { entry =>
              if (entry.context.reference.artifactId.value == id) {
                val value = parse(new String(entry.bytes.get.toArray, StandardCharsets.UTF_8)).fold(error => fail(error.message), identity)
                entry.copy(bytes = Some(_json_bytes(value.mapObject(_.add("schemaVersion", Json.fromString("1.0"))))))
              } else entry
            }
            captured.copy(artifacts = artifacts)
          }
          When("the current pure gate admits presentation and each selected review approval decision or issue is separately replaced by an old schema")
          val admitted = _report(presented, request)
          val old = Vector("review-main", "approval-main", "decision-main", "open-issue-main").map(id => _report(_old_schema_(id), request))
          Then("presentation remains eligible and unsupported records fail without compatibility migration or extra capability")
          admitted.eligibility shouldBe Eligible
          old.map(_.eligibility) shouldBe Vector.fill(old.size)(Inconsistent)
          old.foreach(_.action shouldBe None)
          admitted.action shouldBe Some(InternalModelContinuationAction.HandoffApprovedCandidate)
          admitted.state.cursor.currentStage shouldBe "human-decision-recorded"
          Files.exists(root.resolve("target")) shouldBe false
          Files.exists(root.resolve("chat")) shouldBe false
          Files.exists(root.resolve("provider")) shouldBe false
          Vector("resume-main", "snapshot-model", "realization-main", "projection-main", "candidate-main", "semantic-diff-main").foreach { id =>
            Given("one old schema in the authoritative captured payload with all context and sidecar metadata unchanged")
            val oldcore = _old_schema_(id)
            When("an old cursor source realization continuity candidate or diff schema is supplied")
            val rejected = InternalModelContinuationActionGate.evaluateVerified(oldcore, request)
            Then("the original strict core owner fails without a migrated record or success report")
            rejected.isSuccess shouldBe false
            rejected.toOption shouldBe None
            if (Set("candidate-main", "semantic-diff-main").contains(id))
              rejected.show should include(s"projection artifact $id has an unknown profile/schemaVersion pair")
          }
        }
      }
    }
  }

  private def _with_rich(
    options: FixtureOptions = FixtureOptions(approvalchange = value => value.copy(unresolvedItems = Vector.empty)),
    blocking: Boolean = false
  )(body: (Path, FixtureData, InternalModelContinuationRequest) => Unit): Unit = {
    InternalModelCandidateHumanApprovalValidatorSpec.withFixture(options) { (root, data) =>
      val packagepath = root.resolve("src/main/internal-model")
      _write(packagepath.resolve("decisions/main.json"), InternalModelDecisionRecordCodec.encode(_decision_ledger(data)).toVector)
      _write(packagepath.resolve("open-issues/main.json"), InternalModelOpenIssueRecordCodec.encode(_issue_ledger(data, blocking)).toVector)
      val manifest = parse(new String(Files.readAllBytes(packagepath.resolve("manifest.yaml")), StandardCharsets.UTF_8)).toOption.get
      val entries = manifest.hcursor.get[Vector[Json]]("artifacts").toOption.get.map { value =>
        if (value.hcursor.get[String]("artifactId").toOption.contains("resume-main")) value.mapObject(_.add("path", Json.fromString("resume.yaml"))) else value
      }
      _write(packagepath.resolve("manifest.yaml"), _json_bytes(manifest.mapObject(_.add("artifacts", Json.fromValues(entries)))))
      Files.delete(packagepath.resolve("resumes/main.json"))
      val selected = entries.filter(value => value.hcursor.get[String]("role").toOption.get != "resume" &&
        Files.exists(packagepath.resolve(value.hcursor.get[String]("path").toOption.get), LinkOption.NOFOLLOW_LINKS)).map { value =>
          reference(value.hcursor.get[String]("artifactId").toOption.get, value.hcursor.get[Long]("artifactRevision").toOption.get,
            InternalModelArtifactRole.fromWire(value.hcursor.get[String]("role").toOption.get).toOption.get)
        }.sortBy(_.artifactId.value)
      val cursor = InternalModelResumeCursor("2.0", "ccdm-resume-v2", InternalModelPackageId.from(options.packageid).toOption.get,
        options.carrierrevision, selected, "human-decision-recorded", Some("request-human-decision"), Some("handoff-approved-candidate"),
        Vector.empty, Vector.empty, Vector.empty, Vector.empty)
      _write(packagepath.resolve("resume.yaml"), InternalModelResumeCursorCodec.encode(cursor))
      _write(root.resolve("src/main/cml/original.cml"), Vector[Byte](0, -1, 10))
      val request = InternalModelContinuationRequest(data.reviewbinding.subject.packageReference, options.carrierrevision,
        data.reviewbinding.realizationReference, data.expected.basis.scope, Some(artifactReference("candidate-main", options)),
        Some(artifactReference("semantic-diff-main", options)), Some(artifactReference("review-main", options)),
        Some(artifactReference("approval-main", options)), Some(data.executionbasis), Some(data.expected),
        Some(Vector(InternalModelContinuationDecisionRequirement(artifactReference("decision-main", options), recordReference("decision-current", 211L),
          "topic-primary", "choice-primary", Vector(InternalModelSemanticTarget("element", "e-mono"))))),
        Some(Vector(InternalModelContinuationMappingRequirement("target-alpha", "mapping-alpha", InternalModelSemanticTarget("element", "opaque-shared")),
          InternalModelContinuationMappingRequirement("target-beta", "mapping-beta", InternalModelSemanticTarget("relationship", "opaque-shared")))))
      body(root, data, request)
    }
  }

  private def _decision(data: FixtureData): InternalModelDecisionRecord = {
    InternalModelDecisionRecord(recordReference("decision-current", 211L), "topic-primary", InternalModelDecisionState.Accepted,
      InternalModelDecisionActor("human", "decision-owner", "designer"), InternalModelSemanticSource("human-decision", "decision-owner", Some("decision/source"), Some("decision-version")),
      InternalModelDecisionChoice("choice-primary", "Explicit chosen option"), "Explicit rationale", Vector(InternalModelSemanticTarget("element", "e-mono")),
      Vector(InternalModelDecisionEvidence("evidence-human", InternalModelDecisionEvidenceKind.ExternalHuman,
        InternalModelSemanticSource("human-evidence", "external-human", None, Some("evidence-version")), None, Vector.empty, Vector("evidence condition"), Vector("evidence limitation"))),
      Vector("assumption"), Vector("condition", "condition"), Vector("limitation", "limitation"), Vector.empty,
      Vector(InternalModelDecisionAlternative("choice-other", "Alternative", "Explicitly rejected")),
      InternalModelDecisionBasis(artifactReference("realization-main", data.options), data.reviewbinding.realizationReference, data.expected.basis.scope, InternalModelDecisionBasisStatus.Current), None)
  }

  private def _decision_ledger(data: FixtureData): InternalModelDecisionLedger =
    InternalModelDecisionLedger("ccdm-decision-records-v2", "2.0", recordReference("decision-ledger", 223L), data.expected.basis.scope, Vector(_decision(data)))

  private def _issue_ledger(data: FixtureData, blocking: Boolean): InternalModelOpenIssueLedger = {
    val issue = InternalModelOpenIssueRecord(recordReference("issue-primary", 227L), InternalModelOpenIssueState.Open,
      "What remains unresolved?", "designer", Some("issue-owner"), "Bounded impact", Vector(InternalModelSemanticTarget("element", "e-mono")),
      Vector(InternalModelOpenIssueEvidence("issue-evidence", InternalModelOpenIssueEvidenceKind.ExternalHuman,
        InternalModelSemanticSource("human-evidence", "external-human", None, Some("issue-evidence-version")), None, Vector.empty, Vector("evidence condition"), Vector("evidence limitation"))),
      Vector(InternalModelOpenIssueOption("option-one", "Retained option", Vector("issue-evidence"), Vector("option assumption"), Vector("option condition"), Vector("option limitation"))),
      Vector("assumption"), Vector("condition", "condition"), Vector("limitation", "limitation"), Vector.empty, InternalModelOpenIssueBlocking(blocking, true, true, true))
    InternalModelOpenIssueLedger("ccdm-open-issue-records-v2", "2.0", recordReference("issue-ledger", 229L), data.expected.basis.scope,
      InternalModelOpenIssueBasis(artifactReference("realization-main", data.options), data.reviewbinding.realizationReference), Vector(issue))
  }

  private def _capture(root: Path): InternalModelVerifiedContinuationPackage = {
    val result = InternalModelPackageValidator.verifiedContinuation(root)
    withClue(result.show) { result.isSuccess shouldBe true }
    result.toOption.get
  }

  private def _report(captured: InternalModelVerifiedContinuationPackage, request: InternalModelContinuationRequest): InternalModelContinuationReport = {
    val result = InternalModelContinuationActionGate.evaluateVerified(captured, request)
    withClue(result.show) { result.isSuccess shouldBe true }
    result.toOption.get
  }

  private def _cursor(captured: InternalModelVerifiedContinuationPackage): InternalModelResumeCursor =
    InternalModelResumeCursorCodec.decode(captured.artifacts.find(_.context.reference.role == InternalModelArtifactRole.Resume).get.bytes.get).toOption.get

  private def _cursor_change(captured: InternalModelVerifiedContinuationPackage, change: InternalModelResumeCursor => InternalModelResumeCursor): InternalModelVerifiedContinuationPackage =
    _bytes_change(captured, "resume-main", InternalModelResumeCursorCodec.encode(change(_cursor(captured))))

  private def _bytes_change(captured: InternalModelVerifiedContinuationPackage, id: String, bytes: Vector[Byte]): InternalModelVerifiedContinuationPackage =
    _inventory_change(captured, captured.artifacts.map(value => if (value.context.reference.artifactId.value == id) value.copy(bytes = Some(bytes)) else value))

  private def _inventory_change(captured: InternalModelVerifiedContinuationPackage, artifacts: Vector[InternalModelVerifiedContinuationArtifact]): InternalModelVerifiedContinuationPackage = {
    val ordered = _ordered_artifacts(artifacts)
    captured.copy(packageContext = captured.packageContext.copy(artifacts = ordered.map(_.context)), artifacts = ordered,
      continuityPackage = InternalModelPackageValidator.selectCapturedContinuity(ordered).toOption.get)
  }

  private def _ordered_artifacts(artifacts: Vector[InternalModelVerifiedContinuationArtifact]): Vector[InternalModelVerifiedContinuationArtifact] = {
    def _order_(pending: Vector[InternalModelVerifiedContinuationArtifact], done: Vector[InternalModelVerifiedContinuationArtifact]): Vector[InternalModelVerifiedContinuationArtifact] = {
      if (pending.isEmpty) done else {
        val completed = done.map(_.context.reference.artifactId).toSet
        val next = pending.filter(_.context.dependencies.forall(value => completed.contains(value.artifactId))).sortBy(_.context.reference.artifactId.value).head
        _order_(pending.filterNot(_ == next), done :+ next)
      }
    }
    _order_(artifacts, Vector.empty)
  }

  private def _json_change(captured: InternalModelVerifiedContinuationPackage, id: String, change: Json => Json): InternalModelVerifiedContinuationPackage = {
    val entry = captured.artifacts.find(_.context.reference.artifactId.value == id).get
    _bytes_change(captured, id, _json_bytes(change(parse(new String(entry.bytes.get.toArray, StandardCharsets.UTF_8)).toOption.get)))
  }

  private def _map_artifacts(captured: InternalModelVerifiedContinuationPackage, ids: Set[String], change: Json => Json, pretty: Boolean = false): InternalModelVerifiedContinuationPackage = {
    val artifacts = captured.artifacts.map { entry =>
      if (ids.contains(entry.context.reference.artifactId.value)) {
        entry.bytes.flatMap(bytes => parse(new String(bytes.toArray, StandardCharsets.UTF_8)).toOption) match {
          case Some(value) =>
            val changed = change(value)
            val bytes = if (pretty) (Printer.spaces2.print(changed) + " \n\t ").getBytes(StandardCharsets.UTF_8).toVector else _json_bytes(changed)
            entry.copy(bytes = Some(bytes))
          case None => entry
        }
      } else entry
    }
    _inventory_change(captured, artifacts)
  }

  private def _source_versions(captured: InternalModelVerifiedContinuationPackage, version: String): InternalModelVerifiedContinuationPackage = {
    def _change_(value: Json): Json = value.asObject match {
      case Some(fields) if fields.keys.toSet == Set("authority", "identity", "locator", "revision") => value.mapObject(_.add("revision", Json.fromString(version)))
      case Some(fields) => Json.fromJsonObject(JsonObject.fromIterable(fields.toVector.map { case (key, child) => key -> _change_(child) }))
      case None => value.asArray.map(values => Json.fromValues(values.map(_change_))).getOrElse(value)
    }
    val ids = captured.artifacts.filter(entry => Set(InternalModelArtifactRole.SourceSnapshot, InternalModelArtifactRole.Realization, InternalModelArtifactRole.Projection).contains(entry.context.reference.role)).map(_.context.reference.artifactId.value).toSet
    _map_artifacts(captured, ids, _change_)
  }

  private def _rename_mappings(value: Json, ids: Map[String, String]): Json = value.asObject match {
    case Some(fields) => Json.fromJsonObject(JsonObject.fromIterable(fields.toVector.map { case (key, child) =>
      key -> (if (key == "mappingId") Json.fromString(ids(child.asString.get))
        else if (key == "mappingIds") Json.fromValues(child.asArray.get.map(item => Json.fromString(ids(item.asString.get))))
        else _rename_mappings(child, ids))
    }))
    case None => value.asArray.map(values => Json.fromValues(values.map(_rename_mappings(_, ids)))).getOrElse(value)
  }

  private def _owner_id(code: InternalModelContinuationCondition): String = code match {
    case InternalModelContinuationCondition.ModelAdmitted => "realization-main"
    case InternalModelContinuationCondition.SourceVersionsKnown => "snapshot-model"
    case InternalModelContinuationCondition.DecisionsComplete => "decision-main"
    case InternalModelContinuationCondition.MappingsComplete => "candidate-main"
    case InternalModelContinuationCondition.ReviewAdmitted => "review-main"
    case InternalModelContinuationCondition.HumanApproved | InternalModelContinuationCondition.HumanUnresolvedItems => "approval-main"
    case InternalModelContinuationCondition.NoApprovalBlockingIssues | InternalModelContinuationCondition.ApprovalBlockingIssues => "open-issue-main"
  }

  private def _core_capture(root: Path): InternalModelVerifiedContinuationPackage = {
    val captured = _capture(root)
    _cursor_change(captured, _.copy(currentStage = "model-ready", nextPermittedAction = Some("inspect-projections"), preconditions = Vector.empty))
  }

  private def _core_request(captured: InternalModelVerifiedContinuationPackage): InternalModelContinuationRequest = {
    val state = InternalModelRehydrationValidator.validateVerified(captured).toOption.get
    InternalModelContinuationRequest(state.packageContext.reference, state.packageContext.revision, state.continuity.realization.realizationReference,
      state.continuity.realization.scope, None, None, None, None, None, None, Some(Vector.empty), None)
  }

  private def _json_bytes(value: Json): Vector[Byte] = (Printer.noSpacesSortKeys.print(value) + "\n").getBytes(StandardCharsets.UTF_8).toVector

  private def _write(path: Path, bytes: Vector[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes.toArray)
  }

  private def _remove_owned_root(root: Path): Unit = {
    val parent = Path.of("target", "P104-TYPED-APPROVAL-LIFECYCLE-001").toAbsolutePath.normalize
    root.toAbsolutePath.normalize.startsWith(parent) shouldBe true
    root.toAbsolutePath.normalize should not be parent
    if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      val stream = Files.walk(root)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      finally stream.close()
    }
  }
}
