package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Exact actual-human/source/ownership admission and lossless explicit drift.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelCmlChangeGateSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelCandidateHumanApprovalValidatorSpec.{FixtureOptions, artifactReference, recordReference,
    reference, unchangedSources, sourceDimensionObservation}
  import InternalModelCmlChangeFixture.*
  import InternalModelCmlChangeEligibility.*
  import InternalModelCmlChangeProblemKind.*

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(minSuccessful = 8)

  "CML change eligibility" should {
    "complete independently admitted authority" which {
      "retain both complete targets and exact references without applying their payloads" in {
        Given("rich actual human approval, complete owner versions and two existing regular CML files")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val paths = Vector("cml/alpha.cml", "cml/beta.cml")
          val before = paths.map(path => Files.readAllBytes(fixture.root.resolve(path)).toVector)
          When("captured and rooted entries evaluate the explicit request")
          val captured = evaluate(fixture, fixture.request)
          val rooted = InternalModelCmlChangeGate.evaluate(fixture.root, fixture.request)
          Then("both produce the same complete plan and leave ordinary target payload untouched")
          captured.isSuccess shouldBe true
          rooted.toOption shouldBe captured.toOption
          val report = captured.toOption.get
          report.eligibility shouldBe EligibleForSkillApplication
          report.problems shouldBe empty
          report.continuation.problems shouldBe empty
          report.lifecycle.get.invalidations shouldBe empty
          report.livesources.size shouldBe fixture.request.originalsnapshots.size
          val plan = report.plan.get
          plan.authority shouldBe fixture.request.mutationauthority.get
          plan.subject shouldBe fixture.request.currentreview.binding.subject
          plan.packagereference shouldBe fixture.request.currentrequest.packagereference
          plan.scope shouldBe fixture.request.currentrequest.scope
          plan.candidateartifactreference shouldBe fixture.request.currentrequest.candidateartifact.get
          plan.candidatereference shouldBe fixture.data.expected.basis.candidateReference
          plan.candidatemodelidentity shouldBe fixture.data.expected.basis.candidateModelIdentity
          plan.realizationartifactreference shouldBe fixture.data.reviewbinding.realizationArtifactReference
          plan.realizationreference shouldBe fixture.request.currentrequest.realizationreference
          plan.continuityartifactreference shouldBe fixture.data.reviewbinding.continuityArtifactReference
          plan.continuityreference shouldBe fixture.request.currentreview.semanticDiffAdmission.candidateAdmission.continuity.binding.bindingReference
          plan.semanticdiffartifactreference shouldBe fixture.request.currentrequest.semanticdiffartifact.get
          plan.semanticdiffreference shouldBe fixture.data.expected.basis.semanticDiffReference
          plan.reviewartifactreference shouldBe fixture.request.currentrequest.reviewartifact.get
          plan.reviewreference shouldBe fixture.data.expected.basis.reviewReference
          plan.approvalartifactreference shouldBe fixture.request.currentrequest.approvalartifact.get
          plan.approvalreference shouldBe fixture.data.expected.approvalReference
          plan.actualapproval.record.approval shouldBe fixture.data.expected
          plan.targets.map(_.target) shouldBe fixture.request.currentreview.semanticDiffAdmission.candidateAdmission.projection.targets
          plan.targets.map(_.payload) shouldBe fixture.request.currentreview.semanticDiffAdmission.candidateAdmission.targetBytes
          plan.targets.map(_.nextsourcerevision) shouldBe Vector("approved-next-alpha", "approved-next-beta")
          plan.targets.head.payload.proposedRawBytes shouldBe Vector[Byte](0, -1, 10)
          plan.actualapproval.reviewAdmission.binding.targets.head.reviewSnapshot.limitations shouldBe Vector("snapshot limitation", "snapshot limitation")
          plan.actualapproval.reviewAdmission.semanticDiffAdmission.candidateAdmission.continuity.binding.views.size shouldBe 8
          paths.map(path => Files.readAllBytes(fixture.root.resolve(path)).toVector) shouldBe before
        }
      }

      "preserve semantic identity when source bytes change without a declared version change" in {
        Given("an unchanged explicit owner version and an independently supplied proposed payload")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          writeCml(fixture.root, "cml/alpha.cml", Vector[Byte](7, 8, 9))
          val noncml = fixture.request.livesources.map { case (reference, input) =>
            val changed = input match {
              case InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, revision, _, path)) =>
                InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, revision, Vector[Byte](5), path))
              case other => other
            }
            reference -> changed
          }
          When("the declared source references are evaluated with different ordinary payload")
          val result = evaluate(fixture, fixture.request.copy(livesources = noncml)).toOption.get
          Then("content is not an undeclared-mutation permission or drift token")
          result.eligibility shouldBe EligibleForSkillApplication
          result.lifecycle.get.invalidations shouldBe empty
          result.plan.get.subject shouldBe fixture.data.expected.basis.subject
          result.plan.get.targets.head.payload.proposedRawBytes shouldBe Vector[Byte](0, -1, 10)
        }
      }
    }

    "current actual-human and action admission" which {
      "refuse provider-approved evidence when actual independent human input is missing" in {
        Given("provider-approved snapshots and a stored human record with no independent current input")
        InternalModelCmlChangeFixture.withFixture(InternalModelDurableHandoffFixture.approvedOptions.copy(reviewstate = "approved")) { fixture =>
          val request = fixture.request.copy(currentrequest = fixture.request.currentrequest.copy(humandecision = None))
          When("current action admission checks the actual human prerequisite")
          val report = evaluate(fixture, request).toOption.get
          Then("stored approval and provider flags cannot supply an application plan")
          report.eligibility shouldBe Incomplete
          report.plan shouldBe None
          report.continuation.problems.map(_.dimension) should contain("humandecision")
        }
      }

      "retain contradictions for independently changed actor provenance rationale decision and unresolved input" in {
        Given("one actually admitted human record and separately changed valid human fields")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val changes: Vector[InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput] = Vector(
            value => value.copy(actor = value.actor.copy(identity = "other-human")),
            value => value.copy(actor = value.actor.copy(role = "other-role")),
            value => value.copy(provenance = value.provenance.copy(identity = "other-owner")),
            value => value.copy(rationale = "Different actual rationale"),
            value => value.copy(decision = InternalModelCandidateHumanApprovalDecision.Rejected),
            value => value.copy(unresolvedItems = Vector("unresolved", "unresolved")))
          When("each changed independent input is checked against the captured approval")
          val reports = changes.map(change => evaluate(fixture, fixture.request.copy(currentrequest =
            fixture.request.currentrequest.copy(humandecision = Some(change(fixture.data.expected))))).toOption.get)
          Then("every exact input mismatch remains inconsistent with its owner diagnostic")
          reports.map(_.eligibility) shouldBe Vector.fill(changes.size)(Inconsistent)
          reports.forall(_.plan.isEmpty) shouldBe true
          reports.forall(_.continuation.problems.exists(_.dimension == "approval.admission")) shouldBe true
        }
      }

      "block actually admitted Rejected and ChangesRequested decisions while retaining the original input" in {
        Given("actual independently admitted decisions that explicitly withhold approval")
        val decisions = Vector(InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested)
        decisions.foreach { decision =>
          InternalModelCmlChangeFixture.withFixture(InternalModelDurableHandoffFixture.approvedOptions.copy(decision = decision)) { fixture =>
            When("current handoff and original applicability are evaluated")
            val report = evaluate(fixture, fixture.request).toOption.get
            Then("the actual decision blocks and is not replaced by provider approval")
            report.eligibility shouldBe Blocked
            report.plan shouldBe None
            report.lifecycle.get.approval.record.approval.decision shouldBe decision
            report.continuation.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.HumanDecisionNotApproved)
          }
        }
      }

      "block ordered duplicate unresolved human items without discarding them" in {
        Given("an actual Approved input retaining two unresolved occurrences")
        InternalModelCmlChangeFixture.withFixture(FixtureOptions(approvalchange = value => value.copy(unresolvedItems = Vector("pending", "pending")))) { fixture =>
          When("the human handoff is evaluated")
          val report = evaluate(fixture, fixture.request).toOption.get
          Then("unresolved approval remains blocked with the exact original multiplicity")
          report.eligibility shouldBe Blocked
          report.plan shouldBe None
          report.lifecycle.get.approval.record.approval.unresolvedItems shouldBe Vector("pending", "pending")
          report.continuation.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.HumanUnresolvedItems)
        }
      }

      "block semantic-approval issues and retain the complete current issue ledger" in {
        Given("an admitted unresolved issue explicitly blocking semantic approval")
        InternalModelCmlChangeFixture.withFixture(blocking = true) { fixture =>
          When("all present issue ledgers are independently admitted by the current action gate")
          val report = evaluate(fixture, fixture.request).toOption.get
          Then("the issue blocks application while its conditions and evidence remain inspectable")
          report.eligibility shouldBe Blocked
          report.plan shouldBe None
          report.continuation.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.BlockingIssue)
          report.continuation.openissues.head.admission.ledger.issues.head.conditions shouldBe Vector("condition", "condition")
        }
      }

      "distinguish missing decision and mapping sets from contradictory choices and mapping identities" in {
        Given("exact current choice/mapping declarations and explicit missing or contradictory variants")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val current = fixture.request.currentrequest
          val missing = current.copy(requireddecisions = None, requiredmappings = None)
          val contradicted = current.copy(requireddecisions = current.requireddecisions.map(_.map(_.copy(choiceidentity = "other-choice"))),
            requiredmappings = current.requiredmappings.map(_.map(_.copy(semantictarget = InternalModelSemanticTarget("element", "e-mono")))))
          When("missing and contradicted requirements are independently checked")
          val incomplete = evaluate(fixture, fixture.request.copy(currentrequest = missing)).toOption.get
          val inconsistent = evaluate(fixture, fixture.request.copy(currentrequest = contradicted)).toOption.get
          Then("all decision/mapping problems survive and no variant grants a plan")
          incomplete.eligibility shouldBe Incomplete
          incomplete.continuation.problems.map(_.dimension) should contain allOf("requireddecisions", "requiredmappings")
          inconsistent.eligibility shouldBe Inconsistent
          inconsistent.continuation.problems.map(_.kind) should contain allOf(InternalModelContinuationProblemKind.DecisionMismatch,
            InternalModelContinuationProblemKind.MappingMismatch)
          Vector(incomplete, inconsistent).forall(_.plan.isEmpty) shouldBe true
        }
      }

      "block an eligible earlier action and reject mismatched recorded stage/action" in {
        Given("a structurally valid cursor selecting an earlier row or a contradictory pair")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val earlier = withCursor(fixture, _.copy(currentStage = "review-ready", nextPermittedAction = Some("request-human-decision")))
          val wrong = withCursor(fixture, _.copy(nextPermittedAction = Some("inspect-projections")))
          When("both recorded actions are evaluated for canonical CML application")
          val blocked = InternalModelCmlChangeGate.evaluateVerified(fixture.root, earlier, fixture.request).toOption.get
          val inconsistent = InternalModelCmlChangeGate.evaluateVerified(fixture.root, wrong, fixture.request).toOption.get
          Then("an earlier eligible continuation action grants no CML application and wrong pairs remain contradictory")
          blocked.eligibility shouldBe Blocked
          blocked.problems.map(_.kind) should contain(MissingApprovedHandoff)
          inconsistent.eligibility shouldBe Inconsistent
          inconsistent.continuation.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.StageActionMismatch)
          Vector(blocked, inconsistent).forall(_.plan.isEmpty) shouldBe true
        }
      }

      "block a superseded required decision without replacing it with a sibling choice" in {
        Given("an independently required decision retained as explicitly Superseded in the current ledger")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val ledger = InternalModelDurableHandoffFixture.decisionLedger(fixture.data)
          val predecessor = ledger.records.head.copy(state = InternalModelDecisionState.Superseded)
          val successor = ledger.records.head.copy(decisionReference = recordReference("decision-successor", 521L),
            supersedes = Some(predecessor.decisionReference))
          val superseded = ledger.copy(records = Vector(predecessor, successor))
          val handoff = replacePayload(fixture.handoff, artifactReference("decision-main"),
            InternalModelDecisionRecordCodec.encode(superseded).toVector)
          When("the exact current required decision crosses its semantic owner and the CML gate")
          val report = InternalModelCmlChangeGate.evaluateVerified(fixture.root, handoff, fixture.request).toOption.get
          Then("the explicit obsolete decision blocks application and retains its complete original choice")
          report.eligibility shouldBe Blocked
          report.continuation.problems.map(_.dimension) should contain("decision.currentAccepted")
          report.continuation.decisions.head.admission.ledger.records.head.state shouldBe InternalModelDecisionState.Superseded
          report.continuation.decisions.head.admission.ledger.records.head.selectedChoice shouldBe ledger.records.head.selectedChoice
          report.continuation.decisions.head.admission.ledger.records shouldBe Vector(predecessor, successor)
          report.plan shouldBe None
        }
      }
    }

    "explicit applicability drift" which {
      "retain rule and provider invalidations even when changed expected execution basis prevents current review admission" in {
        Given("original/current review evidence and a new independently supplied rule/provider version")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val basis = fixture.data.executionbasis.copy(rules = fixture.data.executionbasis.rules.map(_.copy(ruleVersion = "current-rule")),
            providers = fixture.data.executionbasis.providers.map(_.copy(providerVersion = "current-provider")))
          val request = fixture.request.copy(currentrequest = fixture.request.currentrequest.copy(executionbasis = Some(basis)))
          When("current admission and independent applicability both evaluate their own evidence")
          val report = evaluate(fixture, request).toOption.get
          Then("both owner failures and complete original/current basis invalidations survive re-review precedence")
          report.eligibility shouldBe ReReviewRequired
          report.continuation.problems.map(_.dimension) should contain("review.admission")
          report.lifecycle.get.invalidations.map(_.kind) should contain allOf(InternalModelCandidateApprovalInvalidationKind.RulesChanged,
            InternalModelCandidateApprovalInvalidationKind.ProvidersChanged)
          report.lifecycle.get.invalidations.flatMap(_.changedDimensionNames) should contain allOf(
            "originalReview.rules[rule-alpha].version", "currentReview.rules[rule-alpha].version",
            "originalReview.providers[provider-alpha].version", "currentReview.providers[provider-alpha].version")
          report.lifecycle.get.approval shouldBe fixture.request.originalapproval
          report.plan shouldBe None
        }
      }

      "require re-review for exact candidate diff review full-subject and scope changes without rebasing history" in {
        Given("independently admitted current carriers with declared semantic/control basis changes")
        val base = InternalModelDurableHandoffFixture.approvedOptions
        val options = Vector(base.copy(candidateartifactrevision = 317L, candidaterevision = 331L),
          base.copy(diffartifactrevision = 337L, diffrevision = 347L), base.copy(reviewartifactrevision = 349L, reviewrevision = 353L),
          base.copy(subjectidentity = "subject-current", subjectrevision = 359L),
          base.copy(componentidentity = "component-current", projectioncontextidentity = "context-current"))
        options.foreach { currentoptions => withPair(currentoptions) { (original, current) =>
          val request = fromOriginal(original, current)
          When("the original actual approval is evaluated against the separately admitted current review")
          val report = evaluate(current, request).toOption.get
          Then("declared drift requires re-review and preserves the exact original subject/decision")
          report.eligibility shouldBe ReReviewRequired
          report.plan shouldBe None
          report.lifecycle.get.invalidations should not be empty
          report.lifecycle.get.approval shouldBe original.request.originalapproval
          report.lifecycle.get.approval.record.approval.basis.subject shouldBe original.data.expected.basis.subject
        }}
      }

      "require re-review for explicitly revised realization and continuity records" in {
        Given("the original human approval and explicit new artifact/logical/subject versions")
        Vector(("realization-main", "realization-order"), ("projection-main", "binding-order")).foreach { case (artifactid, logicalid) =>
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val versions = Map("candidate-main" -> 457L, "semantic-diff-main" -> 461L, "review-main" -> 463L,
              "approval-main" -> 467L, "projection-main" -> 453L, artifactid -> 367L)
            val records = Map("candidate-order-v1" -> 479L, "semantic-diff-order" -> 487L, "review-identity" -> 491L,
              "approval-main" -> 499L, logicalid -> 373L)
            val current = reviseBasis(fixture, versions, records, 379L, 449L)
            When("the new actual capture and separately admitted review evaluate against old approval")
            val report = evaluate(current, current.request).toOption.get
            Then("realization/projection reference drift remains explicit and old approval cannot authorize the plan")
            report.eligibility shouldBe ReReviewRequired
            report.problems.map(_.kind) should contain(SemanticBasisChanged)
            report.lifecycle.get.invalidations.map(_.kind) should contain(InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged)
            report.lifecycle.get.approval shouldBe fixture.request.originalapproval
            report.plan shouldBe None
          }
        }
      }

      "observe independent Scenario model and glossary/BoK owner authority identity and revision changes" in {
        Given("all original contributing source snapshots and separately attributed current source versions")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          for { id <- Vector("snapshot-scenario", "snapshot-model", "snapshot-glossary"); dimension <- Vector("authority", "identity", "revision") } {
            val reference = artifactReference(id)
            val live = fixture.request.livesources.updated(reference,
              InternalModelPackageFreshnessInput.SourceObservation(sourceDimensionObservation(id, dimension)))
            When("one independently supplied source dimension changes")
            val report = evaluate(fixture, fixture.request.copy(livesources = live)).toOption.get
            Then("the exact source reference and dimension require re-review without an inferred winner")
            report.eligibility shouldBe ReReviewRequired
            report.lifecycle.get.invalidations.exists(value => value.artifactReference.contains(reference) &&
              value.changedDimensionNames.contains("source." + dimension)) shouldBe true
            report.plan shouldBe None
          }
        }
      }

      "retain changed and missing source dimensions together with deterministic precedence" in {
        Given("a changed model source identity and explicitly unknown current owner version plus missing authority")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val reference = artifactReference("snapshot-model")
          val live = fixture.request.livesources.updated(reference,
            InternalModelPackageFreshnessInput.SourceObservation(sourceDimensionObservation("snapshot-model", "missing-and-changed")))
          val request = fixture.request.copy(livesources = live, mutationauthority = None)
          When("every drift and missing prerequisite is accumulated")
          val report = evaluate(fixture, request).toOption.get
          Then("re-review wins while missing versions and ownership are retained")
          report.eligibility shouldBe ReReviewRequired
          val invalidation = report.lifecycle.get.invalidations.find(_.artifactReference.contains(reference)).get
          invalidation.kind shouldBe InternalModelCandidateApprovalInvalidationKind.SourceIncomplete
          invalidation.changedDimensionNames should contain allOf("source.identity", "source.revision")
          invalidation.missingDimensionNames shouldBe Vector("observed.source.revision")
          report.problems.map(_.kind) should contain(MissingAuthority)
          report.plan shouldBe None
        }
      }

      "retain missing original source versions without substituting positive artifact versions" in {
        Given("explicitly absent source versions in every source family, with known positive control versions")
        InternalModelCmlChangeFixture.withFixture(InternalModelDurableHandoffFixture.approvedOptions.copy(missingsourceversions =
          Set("snapshot-model", "snapshot-scenario", "snapshot-glossary", "snapshot-cml-alpha", "snapshot-cml-beta"))) { fixture =>
          val live = fixture.request.livesources.map { case (reference, input) => reference -> (input match {
            case InternalModelPackageFreshnessInput.CmlObserved(authority, identity, _, path) =>
              InternalModelPackageFreshnessInput.CmlObserved(authority, identity, None, path)
            case InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, _, bytes, path)) =>
              InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, None, bytes, path))
            case other => other
          }) }
          When("original and live unknown versions are evaluated")
          val report = evaluate(fixture, fixture.request.copy(livesources = live)).toOption.get
          Then("all five sources retain both missing dimensions and application stays incomplete")
          report.eligibility shouldBe Incomplete
          report.lifecycle.get.invalidations.size shouldBe 5
          report.lifecycle.get.invalidations.foreach(_.missingDimensionNames shouldBe Vector("baseline.source.revision", "observed.source.revision"))
          report.continuation.problems.count(_.dimension == "source.revision") should be > 0
          report.plan shouldBe None
        }
      }

      "supersede only the explicit predecessor without choosing a latest successor or suppressing source drift" in {
        Given("an actual independently admitted successor and one explicit exact replacement link")
        withPair(InternalModelDurableHandoffFixture.approvedOptions.copy(approvalartifactid = "approval-successor", approvalartifactrevision = 383L,
          approvalidentity = "approval-successor", approvalrevision = 389L)) { (original, successor) =>
          val link = InternalModelCandidateApprovalSupersessionInput(InternalModelCandidateApprovalSupersession(
            original.request.originalapproval.approvalArtifactReference, successor.request.originalapproval.approvalArtifactReference,
            "ccdm-candidate-approval-supersession-v2", "2.0"), successor.request.originalapproval)
          val request = original.request.copy(supersession = Some(link))
          When("the unchanged predecessor and its explicit lineage are evaluated")
          val blocked = evaluate(original, request).toOption.get
          val changed = evaluate(original, request.copy(livesources = request.livesources.updated(artifactReference("snapshot-scenario"),
            InternalModelPackageFreshnessInput.SourceObservation(sourceDimensionObservation("snapshot-scenario", "revision"))))).toOption.get
          Then("supersession blocks only the predecessor and independent source drift still requires re-review")
          blocked.eligibility shouldBe Blocked
          blocked.lifecycle.get.state shouldBe InternalModelCandidateApprovalLifecycleState.Superseded
          blocked.lifecycle.get.supersession shouldBe Some(link)
          changed.eligibility shouldBe ReReviewRequired
          changed.lifecycle.get.invalidations.map(_.kind) should contain(InternalModelCandidateApprovalInvalidationKind.SourceChanged)
          Vector(blocked, changed).forall(_.plan.isEmpty) shouldBe true
        }
      }
    }

    "current live CML and complete inventory" which {
      "require re-review for independently declared CML authority identity version and current path drift" in {
        Given("the original CML basis and explicit current owner/path declarations")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          writeCml(fixture.root, "cml/current.cml", "alpha cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector)
          val reference = artifactReference("snapshot-cml-alpha")
          val inputs = Vector(
            InternalModelPackageFreshnessInput.CmlObserved("current-authority", "cml-alpha", Some("revision-alpha"), "cml/alpha.cml"),
            InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "current-identity", Some("revision-alpha"), "cml/alpha.cml"),
            InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", Some("current-revision"), "cml/alpha.cml"),
            InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", Some("revision-alpha"), "cml/current.cml"))
          inputs.zip(Vector("source.authority", "source.identity", "source.revision", "basis.projectRelativePath")).foreach { case (input, dimension) =>
            When("the current CML file is physically read under one changed declared dimension")
            val report = evaluate(fixture, fixture.request.copy(livesources = fixture.request.livesources.updated(reference, input))).toOption.get
            Then("the exact declared CML dimension survives and requires explicit reconciliation/re-review")
            report.eligibility shouldBe ReReviewRequired
            report.lifecycle.get.invalidations.find(_.artifactReference.contains(reference)).get.changedDimensionNames should contain(dimension)
            report.plan shouldBe None
          }
        }
      }

      "retain missing current CML versions and changed-plus-missing owner/path dimensions" in {
        Given("current regular CML reads whose owner cannot supply a revision")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          writeCml(fixture.root, "cml/current.cml", Vector[Byte](1, 2))
          val reference = artifactReference("snapshot-cml-alpha")
          val missing = InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", None, "cml/alpha.cml")
          val changed = InternalModelPackageFreshnessInput.CmlObserved("other-authority", "other-identity", None, "cml/current.cml")
          When("current CML owner evidence is compared against its original declared source basis")
          val reports = Vector(missing, changed).map(input => evaluate(fixture,
            fixture.request.copy(livesources = fixture.request.livesources.updated(reference, input))).toOption.get)
          Then("both missing and changed dimensions survive even when re-review outranks incompleteness")
          reports.map(_.eligibility) shouldBe Vector(Incomplete, ReReviewRequired)
          reports.head.lifecycle.get.invalidations.head.missingDimensionNames shouldBe Vector("observed.source.revision")
          reports.last.lifecycle.get.invalidations.head.changedDimensionNames should contain allOf(
            "source.authority", "source.identity", "source.revision", "basis.projectRelativePath")
          reports.forall(_.plan.isEmpty) shouldBe true
        }
      }

      "never use injected CML Observed payload or a CML request for another source kind" in {
        Given("caller-supplied raw CML bytes and a cross-kind live-source request")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val cmlreference = artifactReference("snapshot-cml-alpha")
          val modelreference = artifactReference("snapshot-model")
          val injected = fixture.request.livesources.updated(cmlreference,
            InternalModelPackageFreshnessInput.SourceObservation(unchangedSources("snapshot-cml-alpha"))).updated(modelreference,
            InternalModelPackageFreshnessInput.CmlObserved("model-authority", "model-source", Some("revision-1"), "cml/alpha.cml"))
          When("the observation seam checks source kind and current filesystem-evidence ownership")
          val report = evaluate(fixture, fixture.request.copy(livesources = injected)).toOption.get
          Then("both malformed outcomes remain distinct from a physical CML read and no plan is granted")
          report.eligibility shouldBe Inconsistent
          report.lifecycle.get.invalidations.count(_.kind == InternalModelCandidateApprovalInvalidationKind.SourceMalformed) shouldBe 2
          report.livesources.filter(entry => Set(cmlreference, modelreference).contains(entry.reference)).foreach { entry =>
            entry.result match {
              case InternalModelPackageFreshnessResult.Compared(freshness) =>
                freshness.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
                freshness.reason should not be empty
              case other => fail("expected malformed owner comparison, got " + other)
            }
          }
          report.plan shouldBe None
        }
      }

      "retain absent nonregular symlink and unsafe-path failures from the no-follow native boundary" in {
        Given("only test-owned existing files plus explicit absent/nonregular/symbolic/unsafe target requests")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          Files.createDirectory(fixture.root.resolve("cml/directory.cml"))
          Files.createSymbolicLink(fixture.root.resolve("cml/symbolic.cml"), fixture.root.resolve("cml/alpha.cml"))
          Files.createSymbolicLink(fixture.root.resolve("linked-cml"), fixture.root.resolve("cml"))
          val reference = artifactReference("snapshot-cml-alpha")
          val paths = Vector("cml/absent.cml", "cml/directory.cml", "cml/symbolic.cml", "linked-cml/alpha.cml", "../outside.cml")
          When("each explicit current target request crosses NativeCmlFileReader without fallback")
          val reports = paths.map(path => evaluate(fixture, fixture.request.copy(livesources = fixture.request.livesources.updated(reference,
            InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", Some("revision-alpha"), path)))).toOption.get)
          Then("all exact read outcomes and reasons survive and none permits application")
          reports.head.eligibility shouldBe Incomplete
          reports.tail.map(_.eligibility) shouldBe Vector.fill(4)(Inconsistent)
          reports.foreach { report =>
            report.plan shouldBe None
            report.livesources.find(_.reference == reference).get.result match {
              case InternalModelPackageFreshnessResult.Compared(freshness) => freshness.reason should not be empty
              case other => fail("expected native read failure, got " + other)
            }
          }
        }
      }

      "preserve unreadable target authorization failure without converting it to an observation" in {
        Given("one fixture-owned target with read permission withheld")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val path = fixture.root.resolve("cml/alpha.cml")
          val permissions = Files.getPosixFilePermissions(path)
          Files.setPosixFilePermissions(path, java.util.Collections.emptySet[java.nio.file.attribute.PosixFilePermission]())
          try {
            When("the native reader attempts that exact current target")
            val report = evaluate(fixture, fixture.request).toOption.get
            Then("the original source-authorization outcome remains blocked with no plan")
            report.eligibility shouldBe Blocked
            report.lifecycle.get.invalidations.map(_.kind) should contain(InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized)
            report.plan shouldBe None
          } finally Files.setPosixFilePermissions(path, permissions)
        }
      }

      "retain every absent observation and distinct source-owner availability outcome" in {
        Given("known original inventory with one missing input and explicit unavailable/unauthorized/ambiguous owner outcomes")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val live = fixture.request.livesources - artifactReference("snapshot-scenario")
          val inputs = live.updated(artifactReference("snapshot-model"), InternalModelPackageFreshnessInput.SourceObservation(
            InternalModelLiveSourceObservation.Unavailable("model owner is unavailable"))).updated(artifactReference("snapshot-glossary"),
            InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Unauthorized("glossary owner withheld evidence")))
            .updated(artifactReference("snapshot-cml-beta"), InternalModelPackageFreshnessInput.SourceObservation(
              InternalModelLiveSourceObservation.AmbiguousOrConflicting("canonical owner remains ambiguous")))
          When("all original snapshots are observed without source ranking")
          val report = evaluate(fixture, fixture.request.copy(livesources = inputs)).toOption.get
          Then("blocked precedence retains all missing and other source-owner failures")
          report.eligibility shouldBe Blocked
          report.livesources.size shouldBe fixture.request.originalsnapshots.size
          report.lifecycle.get.invalidations.map(_.kind) should contain allOf(
            InternalModelCandidateApprovalInvalidationKind.SourceUnavailable,
            InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized,
            InternalModelCandidateApprovalInvalidationKind.SourceAmbiguousOrConflicting)
          report.lifecycle.get.invalidations.count(_.kind == InternalModelCandidateApprovalInvalidationKind.SourceUnavailable) shouldBe 2
          report.plan shouldBe None
        }
      }

      "reject unknown stale and wrong-role full source keys while retaining missing known observations" in {
        Given("a known exact source reference and supplied full-key substitutions")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val reference = artifactReference("snapshot-model")
          val variants = Vector(InternalModelCandidateHumanApprovalValidatorSpec.reference("unknown-source", 397L, InternalModelArtifactRole.SourceSnapshot),
            reference.copy(artifactRevision = InternalModelArtifactRevision.from(401L).toOption.get),
            reference.copy(role = InternalModelArtifactRole.Projection))
          variants.foreach { key =>
            val live = (fixture.request.livesources - reference).updated(key, fixture.request.livesources(reference))
            When("a substituted full source key is checked before lifecycle locator conversion")
            val report = evaluate(fixture, fixture.request.copy(livesources = live)).toOption.get
            Then("the contradiction cannot be admitted by bare ID and the known source remains unavailable")
            report.eligibility shouldBe Inconsistent
            report.problems.find(_.kind == UnknownSourceReference).get.artifactreference shouldBe Some(key)
            report.lifecycle.get.invalidations.exists(value => value.artifactReference.contains(reference) &&
              value.kind == InternalModelCandidateApprovalInvalidationKind.SourceUnavailable) shouldBe true
            report.plan shouldBe None
          }
        }
      }

      "retain optional missing baselines without inventing a zero-byte existing target" in {
        Given("an original carrier containing one explicitly optional absent source baseline")
        InternalModelCmlChangeFixture.withFixture(InternalModelDurableHandoffFixture.approvedOptions.copy(includeoptionalsnapshot = true)) { fixture =>
          When("the complete original inventory is evaluated")
          val report = evaluate(fixture, fixture.request).toOption.get
          Then("the missing baseline remains incomplete even though required candidate CML exists")
          report.eligibility shouldBe Incomplete
          report.livesources.find(_.reference == artifactReference("snapshot-optional")).get.result shouldBe InternalModelPackageFreshnessResult.MissingBaseline
          report.lifecycle.get.invalidations.map(_.kind) should contain(InternalModelCandidateApprovalInvalidationKind.MissingBaseline)
          report.plan shouldBe None
        }
      }

      "consume captured package evidence after its files disappear while deliberately rereading live CML" in {
        Given("one actual complete capture and a current unchanged CML source")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          Files.delete(fixture.root.resolve("src/main/internal-model/manifest.yaml"))
          Files.delete(fixture.root.resolve("src/main/internal-model/projections/candidate.json"))
          When("captured evaluation runs while rooted recapture would be unavailable")
          val captured = evaluate(fixture, fixture.request).toOption.get
          val rooted = InternalModelCmlChangeGate.evaluate(fixture.root, fixture.request)
          Then("pure package consumers use retained evidence and the rooted entry reports current package absence")
          captured.eligibility shouldBe EligibleForSkillApplication
          captured.plan.get.targets.size shouldBe 2
          rooted.isSuccess shouldBe false
        }
      }
    }

    "independent exact canonical mutation authority" which {
      "keep missing authority and missing current execution evidence incomplete" in {
        Given("a complete actual approval with separately omitted ownership and execution evidence")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val request = fixture.request.copy(mutationauthority = None,
            currentrequest = fixture.request.currentrequest.copy(executionbasis = None))
          When("all remaining independent checks are evaluated")
          val report = evaluate(fixture, request).toOption.get
          Then("neither stored rules nor provider approval fills the missing authorities")
          report.eligibility shouldBe Incomplete
          report.problems.map(_.kind) should contain allOf(MissingAuthority, MissingExecutionBasis)
          report.lifecycle shouldBe None
          report.originalapproval shouldBe fixture.request.originalapproval
          report.currentreview shouldBe fixture.request.currentreview
          report.livesources.size shouldBe 5
          report.plan shouldBe None
        }
      }

      "reject root package scope exact-target-set path owner and current-version reuse contradictions" in {
        Given("an explicit independent authority with one semantic ownership field contradicted at a time")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val authority = fixture.request.mutationauthority.get
          val target = authority.targets.head
          val variants = Vector(
            authority.copy(projectroot = fixture.root.resolve("other-project").toAbsolutePath.normalize.toString),
            authority.copy(packagereference = authority.packagereference.copy(projectId = InternalModelProjectToken.from("other-project").toOption.get)),
            authority.copy(scope = authority.scope.copy(componentIdentity = "other-component")),
            authority.copy(targets = Vector.empty), authority.copy(targets = authority.targets.tail),
            authority.copy(targets = authority.targets :+ target.copy(targetid = "extra-target", projectrelativepath = "cml/extra.cml")),
            authority.copy(targets = authority.targets :+ target),
            authority.copy(targets = target.copy(projectrelativepath = "cml/moved.cml") +: authority.targets.tail),
            authority.copy(targets = target.copy(sourceauthority = "other-authority") +: authority.targets.tail),
            authority.copy(targets = target.copy(sourceidentity = "other-identity") +: authority.targets.tail),
            authority.copy(targets = target.copy(nextsourcerevision = "revision-alpha") +: authority.targets.tail))
          When("each independently supplied authority is checked against exact current candidate ownership")
          val reports = variants.map(value => evaluate(fixture, fixture.request.copy(mutationauthority = Some(value))).toOption.get)
          Then("every contradiction remains inconsistent with no subset extra rename or version allocation")
          reports.map(_.eligibility) shouldBe Vector.fill(variants.size)(Inconsistent)
          reports.forall(_.plan.isEmpty) shouldBe true
          reports.flatMap(_.problems.map(_.kind)) should contain allOf(AuthorityRootMismatch, AuthorityPackageMismatch,
            AuthorityScopeMismatch, AuthorityTargetMismatch, AuthorityOwnerMismatch, NextSourceRevisionMismatch)
        }
      }

      "reject reuse of an independently observed changed current source version and retain its drift" in {
        Given("a changed live owner revision and an authority proposing that already current revision again")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val authority = fixture.request.mutationauthority.get
          val live = fixture.request.livesources.updated(artifactReference("snapshot-cml-alpha"),
            InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", Some("current-source-version"), "cml/alpha.cml"))
          val changed = authority.copy(targets = authority.targets.head.copy(nextsourcerevision = "current-source-version") +: authority.targets.tail)
          When("next-version ownership and current-source drift are accumulated")
          val report = evaluate(fixture, fixture.request.copy(livesources = live, mutationauthority = Some(changed))).toOption.get
          Then("ownership inconsistency outranks but preserves re-review evidence")
          report.eligibility shouldBe Inconsistent
          report.problems.map(_.kind) should contain(NextSourceRevisionMismatch)
          report.lifecycle.get.invalidations.map(_.kind) should contain(InternalModelCandidateApprovalInvalidationKind.SourceChanged)
          report.plan shouldBe None
        }
      }

      "keep a nonblank explicitly supplied next source revision mandatory for every target" in {
        Given("complete current source versions and one missing next source-owned value")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val authority = fixture.request.mutationauthority.get
          val changed = authority.copy(targets = authority.targets.head.copy(nextsourcerevision = "\u3000") +: authority.targets.tail)
          When("the source-owner request is evaluated without inventing a next version")
          val report = evaluate(fixture, fixture.request.copy(mutationauthority = Some(changed))).toOption.get
          Then("next source revision remains incomplete and no plan is emitted")
          report.eligibility shouldBe Incomplete
          report.problems.map(_.kind) should contain(MissingNextSourceRevision)
          report.plan shouldBe None
        }
      }

      "reject different selected candidate diff review realization package and scope while retaining exact full references" in {
        Given("well-formed independent request selections that contradict the actual current capture")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val current = fixture.request.currentrequest
          val changedrevision = InternalModelArtifactRevision.from(409L).toOption.get
          val variants = Vector(
            current.copy(candidateartifact = current.candidateartifact.map(_.copy(artifactRevision = changedrevision))),
            current.copy(semanticdiffartifact = current.semanticdiffartifact.map(_.copy(artifactRevision = changedrevision))),
            current.copy(reviewartifact = current.reviewartifact.map(_.copy(artifactRevision = changedrevision))),
            current.copy(realizationreference = recordReference("other-realization", 419L)),
            current.copy(packagereference = current.packagereference.copy(projectId = InternalModelProjectToken.from("other-project").toOption.get)),
            current.copy(scope = current.scope.copy(componentIdentity = "other-component")))
          When("each full independent selection is matched to capture and separately admitted review")
          val reports = variants.map(value => evaluate(fixture, fixture.request.copy(currentrequest = value)).toOption.get)
          Then("no stale version or foreign semantic context can grant canonical mutation")
          reports.map(_.eligibility) shouldBe Vector.fill(variants.size)(Inconsistent)
          reports.forall(_.plan.isEmpty) shouldBe true
          reports.forall(_.problems.exists(_.kind == CurrentReviewMismatch)) shouldBe true
          reports.forall(_.lifecycle.nonEmpty) shouldBe true
        }
      }

      "reject a separately admitted current review from another semantic carrier" in {
        Given("two actual admitted reviews with distinct full semantic subjects and candidate versions")
        withPair(InternalModelDurableHandoffFixture.approvedOptions.copy(candidateartifactrevision = 421L,
          candidaterevision = 431L, subjectidentity = "other-subject", subjectrevision = 433L)) { (original, other) =>
          val request = original.request.copy(currentreview = other.request.currentreview)
          When("an unrelated but structurally valid actual admission is supplied as the current review")
          val report = evaluate(original, request).toOption.get
          Then("metadata inconsistency wins while all original-to-supplied drift remains visible")
          report.eligibility shouldBe Inconsistent
          report.problems.map(_.kind) should contain(CurrentReviewMismatch)
          report.lifecycle.get.invalidations.map(_.kind) should contain allOf(
            InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged)
          report.plan shouldBe None
        }
      }

      "return structured failure for null malformed and internally contradictory retained graphs" in {
        Given("an actual control and malformed typed admission/source/request graphs")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val request = fixture.request
          val variants = Vector(null, request.copy(currentreview = null), request.copy(originalapproval = null),
            request.copy(originalsnapshots = null), request.copy(livesources = Map(artifactReference("snapshot-model") -> null)),
            request.copy(originalsnapshots = request.originalsnapshots :+ request.originalsnapshots.head),
            request.copy(currentrequest = request.currentrequest.copy(candidateartifact = Some(artifactReference("snapshot-model")))),
            request.copy(currentreview = request.currentreview.copy(binding = request.currentreview.binding.copy(candidateReference = recordReference("contradiction", 439L)))))
          When("each malformed graph crosses the structured boundary")
          val results = variants.map(value => evaluate(fixture, value))
          Then("invalid structure is rejected as an operation failure rather than partial eligibility or an exception")
          results.forall(!_.isSuccess) shouldBe true
          results.forall(_.toOption.isEmpty) shouldBe true
        }
      }

      "reject malformed serialized original snapshots and current selected approval before partial eligibility" in {
        Given("actual original/current admissions with a malformed serialized boundary supplied separately")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val malformed = Vector[Byte](0xc3.toByte, 0x28.toByte)
          val original = fixture.request.copy(originalsnapshots = fixture.request.originalsnapshots.map(snapshot =>
            if (snapshot.reference == artifactReference("snapshot-model")) snapshot.copy(bytes = Some(malformed)) else snapshot))
          val captured = replacePayload(fixture.handoff, fixture.request.currentrequest.approvalartifact.get, malformed)
          When("original snapshot and selected current approval syntax are structurally admitted")
          val originalfailure = evaluate(fixture, original)
          val currentfailure = InternalModelCmlChangeGate.evaluateVerified(fixture.root, captured, fixture.request)
          Then("neither malformed transport becomes an ordinary drift or a partial application plan")
          originalfailure.isSuccess shouldBe false
          currentfailure.isSuccess shouldBe false
          originalfailure.toOption shouldBe None
          currentfailure.toOption shouldBe None
        }
      }

      "retain explicit supersession and blocked history even when independent current execution basis is missing" in {
        Given("an independently actually admitted successor and no current rule/provider basis")
        withPair(InternalModelDurableHandoffFixture.approvedOptions.copy(approvalartifactid = "approval-successor",
          approvalartifactrevision = 503L, approvalidentity = "approval-successor", approvalrevision = 509L)) { (original, successor) =>
          val link = InternalModelCandidateApprovalSupersessionInput(InternalModelCandidateApprovalSupersession(
            original.request.originalapproval.approvalArtifactReference, successor.request.originalapproval.approvalArtifactReference,
            "ccdm-candidate-approval-supersession-v2", "2.0"), successor.request.originalapproval)
          val request = original.request.copy(supersession = Some(link),
            currentrequest = original.request.currentrequest.copy(executionbasis = None))
          When("all possible independent causes are accumulated without a fallback execution basis")
          val report = evaluate(original, request).toOption.get
          Then("the explicit predecessor remains blocked and both history and missing evidence survive")
          report.eligibility shouldBe Blocked
          report.supersession shouldBe Some(link)
          report.originalapproval shouldBe original.request.originalapproval
          report.lifecycle shouldBe None
          report.problems.map(_.kind) should contain allOf(SupersededApproval, MissingExecutionBasis)
          report.plan shouldBe None
        }
      }
    }
  }

  "Explicit CML change properties" should {
    "producer-owned opaque identities and versions" which {
      "retain generated positive references and next source versions without interpreting numeric order" in {
        Given("independently allocated positive revisions and nonempty opaque identities")
        forAll(Gen.chooseNum(1L, 1000000L), Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)) { (revision, identity) =>
          val options = InternalModelDurableHandoffFixture.approvedOptions.copy(candidateidentity = "candidate-" + identity,
            candidateartifactrevision = revision, candidaterevision = revision, subjectidentity = "subject-" + identity,
            subjectrevision = revision, approvalidentity = "approval-" + identity, approvalrevision = revision)
          InternalModelCmlChangeFixture.withFixture(options) { fixture =>
            val authority = fixture.request.mutationauthority.get.copy(requestreference = recordReference("request-" + identity, revision),
              targets = fixture.request.mutationauthority.get.targets.map(target => target.copy(nextsourcerevision = "source-next-" + identity)))
            When("actual independent source/control references are admitted together")
            val report = evaluate(fixture, fixture.request.copy(mutationauthority = Some(authority))).toOption.get
            Then("generated IDs and independent versions remain exact while ordinary binary output is retained")
            report.eligibility shouldBe EligibleForSkillApplication
            report.plan.get.candidatereference shouldBe recordReference("candidate-" + identity, revision)
            report.plan.get.authority.requestreference shouldBe recordReference("request-" + identity, revision)
            report.plan.get.targets.map(_.nextsourcerevision) shouldBe Vector.fill(2)("source-next-" + identity)
            report.plan.get.targets.head.payload.proposedRawBytes shouldBe Vector[Byte](0, -1, 10)
          }
        }
      }

      "require explicit re-review for generated different declared live versions and retain missing versions" in {
        Given("generated source-owned opaque versions and independently selected source families")
        forAll(Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString), Gen.oneOf("snapshot-model", "snapshot-scenario", "snapshot-glossary")) { (version, id) =>
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val reference = artifactReference(id)
            val original = unchangedSources(id).asInstanceOf[InternalModelLiveSourceObservation.Observed]
            val changed = InternalModelLiveSourceObservation.Observed(original.authority, original.identity, Some("current-" + version), original.rawBytes, None)
            val missing = InternalModelLiveSourceObservation.Observed(original.authority, original.identity, None, original.rawBytes, None)
            When("different and missing declared versions are evaluated independently")
            val reports = Vector(changed, missing).map(observation => evaluate(fixture, fixture.request.copy(livesources =
              fixture.request.livesources.updated(reference, InternalModelPackageFreshnessInput.SourceObservation(observation)))).toOption.get)
            Then("explicit version drift cannot authorize application and unknown current versions stay observable")
            reports.map(_.eligibility) shouldBe Vector(ReReviewRequired, Incomplete)
            reports.head.lifecycle.get.invalidations.head.changedDimensionNames should contain("source.revision")
            reports.last.lifecycle.get.invalidations.head.missingDimensionNames shouldBe Vector("observed.source.revision")
            reports.forall(_.plan.isEmpty) shouldBe true
          }
        }
      }

      "retain all ordering-independent source results and complete target payload under harmless permutations" in {
        Given("a complete unchanged semantic basis with reversed observation/snapshot/authority encounter order")
        forAll(Gen.oneOf(true, false)) { reversed =>
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val request = fixture.request
            val changed = if (reversed) request.copy(originalsnapshots = request.originalsnapshots.reverse,
              livesources = scala.collection.immutable.ListMap.from(request.livesources.toVector.reverse),
              mutationauthority = request.mutationauthority.map(value => value.copy(targets = value.targets.reverse))) else request
            When("harmless encounter permutations are evaluated")
            val report = evaluate(fixture, changed).toOption.get
            val control = evaluate(fixture, request).toOption.get
            Then("semantic identity all owner results and candidate-declared target order remain stable")
            report.eligibility shouldBe EligibleForSkillApplication
            report.livesources shouldBe control.livesources
            report.lifecycle shouldBe control.lifecycle
            report.plan.get.subject shouldBe control.plan.get.subject
            report.plan.get.targets shouldBe control.plan.get.targets
          }
        }
      }

      "leave unchanged subject applicable under generated carrier-only control additions and revision changes" in {
        Given("explicit current carriers with inert unselected controls and an independently supplied carrier revision")
        forAll(Gen.chooseNum(1L, 1000000L)) { revision =>
          val extra = reference("unselected-control", 443L, InternalModelArtifactRole.Approval)
          val options = InternalModelDurableHandoffFixture.approvedOptions.copy(carrierrevision = revision,
            extraartifact = Some((extra, "approvals/unselected.json", "inert unselected control\n".getBytes(StandardCharsets.UTF_8), Vector.empty)))
          withPair(options) { (original, current) =>
            val request = fromOriginal(original, current)
            When("current action and applicability independently evaluate the unchanged complete subject")
            val report = evaluate(current, request).toOption.get
            Then("carrier controls transport evidence without changing the reviewed semantics")
            report.eligibility shouldBe EligibleForSkillApplication
            report.lifecycle.get.invalidations shouldBe empty
            report.plan.get.subject shouldBe original.data.expected.basis.subject
            report.plan.get.actualapproval.record.approval shouldBe original.data.expected
          }
        }
      }

      "refuse generated stale selections missing authority and malformed authority graphs" in {
        Given("explicit positive foreign revisions and independently omitted or malformed authority")
        forAll(Gen.chooseNum(1000L, 1000000L)) { revision =>
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val current = fixture.request.currentrequest
            val stale = fixture.request.copy(currentrequest = current.copy(candidateartifact = current.candidateartifact.map(_.copy(
              artifactRevision = InternalModelArtifactRevision.from(revision).toOption.get))))
            val missing = fixture.request.copy(mutationauthority = None)
            val malformed = fixture.request.copy(mutationauthority = Some(fixture.request.mutationauthority.get.copy(requestreference = null)))
            When("all three explicit authorization failures cross their owners")
            val inconsistent = evaluate(fixture, stale).toOption.get
            val incomplete = evaluate(fixture, missing).toOption.get
            val failure = evaluate(fixture, malformed)
            Then("stale selection missing ownership and invalid structure remain distinct without an inferred plan")
            inconsistent.eligibility shouldBe Inconsistent
            incomplete.eligibility shouldBe Incomplete
            failure.isSuccess shouldBe false
            Vector(inconsistent, incomplete).forall(_.plan.isEmpty) shouldBe true
          }
        }
      }
    }
  }
}
