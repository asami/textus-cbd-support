package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.util.Arrays
import io.circe.Json
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 29, 2026
 *  version Sep. 29, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
/** Executable specification of retained approval, typed drift and explicit supersession. */
final class InternalModelCandidateApprovalLifecycleSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelCandidateHumanApprovalValidatorSpec.*

  "Candidate approval lifecycle" should {
    "derive each original human decision from exact current evidence" which {
      "preserve Approved Rejected and ChangesRequested with ordered unresolved items" in {
        Given("three actual two-target V2 admissions with independent human decisions and complete unchanged source evidence")
        val decisions = Vector(InternalModelCandidateHumanApprovalDecision.Approved, InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested)
        decisions.zip(Vector(InternalModelCandidateApprovalLifecycleState.Approved, InternalModelCandidateApprovalLifecycleState.Rejected, InternalModelCandidateApprovalLifecycleState.ChangesRequested)).foreach { case (decision, state) =>
          InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(decision = decision)) { (root, data) =>
            val evidence = admit(root, data)
            When("the captured original review and independent execution basis are evaluated")
            val result = _evaluate(evidence, evidence.review, data.executionbasis)
            Then("the decision state and all retained semantic evidence remain exact")
            result.map(_.state) shouldBe Some(state)
            result.map(_.approval.record.approval.unresolvedItems) shouldBe Some(Vector("follow-up", "later", "follow-up"))
            result.map(_.approval) shouldBe Some(evidence.approval)
            result.map(_.invalidations) shouldBe Some(Vector.empty)
            result.map(_.supersession) shouldBe Some(None)
          }
        }
      }
    }

    "apply an explicit portable link only" which {
      "derive Superseded while retaining original and successor records without guessing a sibling" in {
        Given("separately actual-admitted approvals with distinct external artifact revisions and advancing logical approval revision")
        withFixturePair(FixtureOptions(approvalartifactrevision = 113L, approvalrevision = 127L)) { (original, successor, data) =>
          val link = _link(original.approval, successor.approval)
          When("the exact typed link and separately admitted successor are supplied")
          val result = _evaluate(original, original.review, data.executionbasis, supersession = Some(InternalModelCandidateApprovalSupersessionInput(link, successor.approval)))
          Then("Superseded retains both records and the full external references without granting successor applicability")
          result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
          result.map(_.approval) shouldBe Some(original.approval)
          result.flatMap(_.supersession).map(_.successorApproval) shouldBe Some(successor.approval)
          result.flatMap(_.supersession).map(_.record) shouldBe Some(link)
        }
      }

      "derive Superseded for every separately admitted successor decision and never infer a sibling when the explicit link is absent" in {
        Given("each independent successor decision under the same logical identity with an explicitly higher revision")
        Vector(InternalModelCandidateHumanApprovalDecision.Approved, InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested).foreach { decision =>
          withFixturePair(FixtureOptions(decision = decision, approvalartifactrevision = 113L, approvalrevision = 127L)) { (original, successor, data) =>
            When("the same evidence is evaluated once with its exact link and once without a link")
            val linked = _evaluate(original, original.review, data.executionbasis, supersession = Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval)))
            val unlinked = _evaluate(original, original.review, data.executionbasis)
            Then("only the explicit link retires the predecessor regardless of successor decision")
            linked.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
            linked.flatMap(_.supersession).map(_.successorApproval.record.approval.decision) shouldBe Some(decision)
            unlinked.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Approved)
            unlinked.flatMap(_.supersession) shouldBe None
          }
        }
      }

      "reject mismatched full references self lineage and same-identity revision reuse" in {
        Given("actual original and successor admissions plus exact-ID revision and role link mutations")
        withFixturePair(FixtureOptions(approvalartifactrevision = 113L, approvalrevision = 127L)) { (original, successor, data) =>
          val valid = _link(original.approval, successor.approval)
          val variants = Vector(
            valid.copy(predecessorApproval = reference("other-predecessor", 107L, InternalModelArtifactRole.Approval)),
            valid.copy(predecessorApproval = valid.predecessorApproval.copy(artifactRevision = InternalModelArtifactRevision.from(108L).toOption.get)),
            valid.copy(predecessorApproval = valid.predecessorApproval.copy(role = InternalModelArtifactRole.Validation)),
            valid.copy(successorApproval = reference("other-successor", 113L, InternalModelArtifactRole.Approval)),
            valid.copy(successorApproval = valid.successorApproval.copy(artifactRevision = InternalModelArtifactRevision.from(114L).toOption.get)),
            valid.copy(successorApproval = valid.successorApproval.copy(role = InternalModelArtifactRole.Projection)),
            valid.copy(successorApproval = valid.predecessorApproval)
          )
          When("each declaration is evaluated against the actual separately admitted successor")
          val outcomes = variants.map(value => _evaluate(original, original.review, data.executionbasis, supersession = Some(InternalModelCandidateApprovalSupersessionInput(value, successor.approval))))
          Then("neither a mismatched full reference nor self-link becomes supersession")
          outcomes shouldBe Vector.fill(variants.size)(None)
        }
      }

      "reject cross-subject rollback collision and same-identity revision misuse while supporting explicit revised and distinct-identity successors" in {
        Given("one original carrier and actual successor fixtures for every frozen lineage boundary")
        val rejected = Vector(
          FixtureOptions(projectnamespace = "org.other", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(projectid = "other-project", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(packageid = "abcdefab-cdef-abcd-efab-cdefabcdefab", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(componentidentity = "component-current", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(projectioncontextidentity = "context-current", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(selectedusecaseelementidentity = "e-usecase-current", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(candidateidentity = "candidate-current", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(candidatemodelidentity = "model-current", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(candidaterevision = 6L, approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(candidateartifactrevision = 19L, approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(candidateartifactid = "candidate-current", approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(approvalartifactrevision = 113L),
          FixtureOptions(approvalartifactrevision = 113L, approvalrevision = 103L)
        )
        rejected.foreach { options =>
          withFixturePair(options) { (original, successor, data) =>
            When("the exact explicit link crosses its independently admitted lineage")
            val result = _evaluate(original, original.review, data.executionbasis, supersession = Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval)))
            Then("every crossing rollback same-candidate-revision collision and nonadvancing logical approval fails")
            result shouldBe None
          }
        }
        val supported = Vector(
          FixtureOptions(candidaterevision = 8L, candidateartifactrevision = 19L, approvalartifactrevision = 113L, approvalrevision = 127L),
          FixtureOptions(candidaterevision = 8L, candidateartifactrevision = 19L, approvalidentity = "approval-distinct", approvalrevision = 1L, approvalartifactid = "approval-distinct", approvalartifactrevision = 2L, decision = InternalModelCandidateHumanApprovalDecision.ChangesRequested)
        )
        supported.foreach { options =>
          withFixturePair(options) { (original, successor, data) =>
            When("an exact higher candidate logical revision or distinct approval logical identity is explicitly linked")
            val result = _evaluate(original, original.review, data.executionbasis, supersession = Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval)))
            Then("declared lineage supports supersession without numeric ranking across distinct identities")
            result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
          }
        }
      }
    }

    "retain complete currentness evidence" which {
      "invalidate every declared captured source comparison without treating a changed authority or path as unchanged" in {
        Given("a real complete original capture and an independently supplied changed model source revision")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val evidence = admit(root, data)
          When("the source owner declares a new revision against all retained baselines")
          val result = _evaluate(evidence, evidence.review, data.executionbasis, unchangedSources.updated("snapshot-model", sourceDimensionObservation("snapshot-model", "revision")))
          Then("the original full source reference and revision dimension explain invalidation")
          result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Invalidated)
          result.map(_.invalidations) shouldBe Some(Vector(_reason(InternalModelCandidateApprovalInvalidationKind.SourceChanged, "snapshot-model", Vector("source.revision"))))
        }
      }

      "retain every independently admitted candidate review diff scope subject rule and provider dimension" in {
        Given("one actual original approval and independently admitted current carriers differing in typed and logical versions")
        val cases = Vector(
          (FixtureOptions(candidateartifactid = "candidate-current"), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateArtifactReference.artifactId"),
          (FixtureOptions(candidateartifactrevision = 19L), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateArtifactReference.artifactRevision"),
          (FixtureOptions(candidateidentity = "candidate-current"), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateReference.recordId"),
          (FixtureOptions(candidaterevision = 8L), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateReference.recordRevision"),
          (FixtureOptions(candidatemodelidentity = "model-current"), InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, "candidateModelIdentity"),
          (FixtureOptions(reviewartifactid = "review-current"), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewArtifactReference.artifactId"),
          (FixtureOptions(reviewartifactrevision = 79L), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewArtifactReference.artifactRevision"),
          (FixtureOptions(reviewidentity = "review-current"), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewReference.recordId"),
          (FixtureOptions(reviewrevision = 4L), InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, "reviewReference.recordRevision"),
          (FixtureOptions(diffartifactid = "diff-current"), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffArtifactReference.artifactId"),
          (FixtureOptions(diffartifactrevision = 37L), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffArtifactReference.artifactRevision"),
          (FixtureOptions(diffidentity = "diff-current"), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffReference.recordId"),
          (FixtureOptions(diffrevision = 6L), InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, "semanticDiffReference.recordRevision"),
          (FixtureOptions(componentidentity = "component-current"), InternalModelCandidateApprovalInvalidationKind.ScopeChanged, "scope.componentIdentity"),
          (FixtureOptions(projectioncontextidentity = "context-current"), InternalModelCandidateApprovalInvalidationKind.ScopeChanged, "scope.projectionContextIdentity"),
          (FixtureOptions(selectedusecaseelementidentity = "e-usecase-current"), InternalModelCandidateApprovalInvalidationKind.ScopeChanged, "scope.selectedUseCaseElementIdentity"),
          (FixtureOptions(subjectidentity = "subject-current"), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.subjectId"),
          (FixtureOptions(subjectrevision = 103L), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.subjectRevision"),
          (FixtureOptions(packageid = "abcdefab-cdef-abcd-efab-cdefabcdefab"), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.packageReference.packageId"),
          (FixtureOptions(projectnamespace = "org.current"), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.packageReference.projectNamespace"),
          (FixtureOptions(projectid = "current-project"), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.packageReference.projectId"),
          (FixtureOptions(candidateartifactrevision = 19L), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.artifacts"),
          (FixtureOptions(componentidentity = "component-current"), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.scope.componentIdentity"),
          (FixtureOptions(projectioncontextidentity = "context-current"), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.scope.projectionContextIdentity"),
          (FixtureOptions(selectedusecaseelementidentity = "e-usecase-current"), InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, "subject.scope.selectedUseCaseElementIdentity"),
          (FixtureOptions(ruleversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "originalReview.rules[rule-alpha].version"),
          (FixtureOptions(providerversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "originalReview.providers[provider-alpha].version")
        )
        cases.foreach { case (options, kind, dimension) =>
          withFixturePair(options) { (original, current, data) =>
            When("the independent current execution basis and its actually admitted review are compared to historical approval")
            val result = _evaluate(original, current.review, data.executionbasis)
            Then("each representable semantic difference remains attributed without rewriting the historical decision")
            withClue(dimension) { result.map(_.invalidations.exists(value => value.kind == kind && value.changedDimensionNames.contains(dimension))) shouldBe Some(true) }
            result.map(_.approval) shouldBe Some(original.approval)
          }
        }
      }

      "treat carrier-only control transport changes as unchanged historical subject evidence" in {
        Given("an independently captured newer carrier with a different approval control revision and inert optional control")
        val controls = Vector(("inert-approval", InternalModelArtifactRole.Approval), ("inert-resume", InternalModelArtifactRole.Resume), ("inert-validation", InternalModelArtifactRole.Validation))
        controls.foreach { case (artifactid, role) =>
          val extra = reference(artifactid, 151L, role)
          withFixturePair(FixtureOptions(carrierrevision = 157L, approvalartifactrevision = 113L, approvalrevision = 127L, extraartifact = Some((extra, "controls/" + artifactid + ".json", jsonBytes(Json.obj("profile" -> Json.fromString("unknown-control"))), Vector.empty)))) { (original, current, data) =>
          When("the evaluator compares unchanged subjects despite carrier and unselected-control revisions")
          val result = _evaluate(original, current.review, data.executionbasis)
          Then("the historical approval remains Approved and no carrier transport dimension appears")
          result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Approved)
          result.map(_.invalidations) shouldBe Some(Vector.empty)
          current.review.carrierPackageContext.revision should not be original.review.carrierPackageContext.revision
          current.review.binding.subject shouldBe original.review.binding.subject
          }
        }
      }

      "retain every independently supplied current rule and provider ID and version deviation" in {
        Given("actual current reviews admitted against their own explicit basis and a separately supplied original expected basis")
        val cases = Vector(
          (FixtureOptions(ruleid = "rule-aardvark"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "currentReview.rules.ids"),
          (FixtureOptions(ruleversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.RulesChanged, "currentReview.rules[rule-alpha].version"),
          (FixtureOptions(providerid = "provider-aardvark"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "currentReview.providers.ids"),
          (FixtureOptions(providerversion = "1.1"), InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, "currentReview.providers[provider-alpha].version")
        )
        cases.foreach { case (options, kind, dimension) =>
          withFixturePair(options) { (original, current, _) =>
            When("the immutable original execution basis is independently passed to lifecycle")
            val result = _evaluate(original, current.review, basis())
            Then("ID membership and version differences retain original-versus-expected and current-versus-expected attribution")
            result.map(_.invalidations.exists(value => value.kind == kind && value.changedDimensionNames.contains(dimension))) shouldBe Some(true)
          }
        }
      }

      "fail closed for malformed missing duplicate unknown and metadata-inconsistent snapshot or observed-key topology" in {
        Given("actual source inventory metadata including an optional absent baseline")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(includeoptionalsnapshot = true)) { (root, data) =>
          val evidence = admit(root, data)
          val first = evidence.snapshots.find(_.bytes.isDefined).get
          val optional = evidence.snapshots.find(_.bytes.isEmpty).get
          val invalid = Vector(
            evidence.snapshots.drop(1),
            evidence.snapshots :+ first,
            evidence.snapshots :+ first.copy(reference = reference("snapshot-foreign", 163L, InternalModelArtifactRole.SourceSnapshot)),
            evidence.snapshots.map(value => if (value == first) value.copy(path = "snapshots/other.json") else value),
            evidence.snapshots.map(value => if (value == first) value.copy(reference = value.reference.copy(artifactRevision = InternalModelArtifactRevision.from(167L).toOption.get)) else value),
            evidence.snapshots.map(value => if (value == first) value.copy(reference = value.reference.copy(role = InternalModelArtifactRole.Validation)) else value),
            evidence.snapshots.map(value => if (value == first) value.copy(required = false) else value),
            evidence.snapshots.map(value => if (value == first) value.copy(dependencies = Vector(artifactReference("snapshot-glossary"))) else value),
            evidence.snapshots.map(value => if (value == first) value.copy(bytes = Some("malformed".getBytes(StandardCharsets.UTF_8).toVector)) else value),
            evidence.snapshots.map(value => if (value == first) value.copy(bytes = None) else value),
            evidence.snapshots.map(value => if (value == optional) value.copy(required = true) else value),
            evidence.snapshots.map(value => if (value == optional) value.copy(bytes = first.bytes) else value)
          )
          When("each structurally invalid snapshot inventory and map topology crosses the pure boundary")
          val snapshotresults = invalid.map(values => InternalModelCandidateApprovalLifecycleEvaluator.evaluate(evidence.approval, evidence.review, data.executionbasis, values, unchangedSources, None).isSuccess)
          val mapresults = Vector(
            unchangedSources.updated("unknown", InternalModelLiveSourceObservation.Unavailable("unknown")),
            Map("snapshot-model" -> null.asInstanceOf[InternalModelLiveSourceObservation]),
            Map(null.asInstanceOf[String] -> InternalModelLiveSourceObservation.Unavailable("null locator"))
          ).map(values => InternalModelCandidateApprovalLifecycleEvaluator.evaluate(evidence.approval, evidence.review, data.executionbasis, evidence.snapshots, values, None).isSuccess)
          Then("no reference path required dependency presence malformed payload or unknown locator contradiction produces a report")
          snapshotresults shouldBe Vector.fill(invalid.size)(false)
          mapresults shouldBe Vector(false, false, false)
        }
      }

      "map every source owner status across scenario model glossary and both CML baselines while retaining optional absence" in {
        Given("all five present source kinds and an actually absent optional source baseline")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(includeoptionalsnapshot = true)) { (root, data) =>
          val evidence = admit(root, data)
          _sort_ids(unchangedSources.keys.toVector).foreach { artifactid =>
            val variants = Vector(
              (Some(InternalModelCandidateApprovalInvalidationKind.SourceChanged), unchangedSources.updated(artifactid, sourceDimensionObservation(artifactid, "revision"))),
              (Some(InternalModelCandidateApprovalInvalidationKind.SourceIncomplete), unchangedSources.updated(artifactid, sourceDimensionObservation(artifactid, "missing"))),
              (Some(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable), unchangedSources.removed(artifactid)),
              (Some(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable), unchangedSources.updated(artifactid, InternalModelLiveSourceObservation.Unavailable("offline"))),
              (Some(InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized), unchangedSources.updated(artifactid, InternalModelLiveSourceObservation.Unauthorized("denied"))),
              (Some(InternalModelCandidateApprovalInvalidationKind.SourceMalformed), unchangedSources.updated(artifactid, InternalModelLiveSourceObservation.Malformed("malformed"))),
              (Some(InternalModelCandidateApprovalInvalidationKind.SourceAmbiguousOrConflicting), unchangedSources.updated(artifactid, InternalModelLiveSourceObservation.AmbiguousOrConflicting("conflicting"))),
              (None, unchangedSources)
            )
            variants.foreach { case (kind, observations) =>
              When("one source owner reports a closed freshness status")
              val result = _evaluate(evidence, evidence.review, data.executionbasis, observations)
              Then("the exact reference receives the supplied status and optional absence stays MissingBaseline")
              result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Invalidated)
              result.map(_.invalidations.exists(value => value.kind == InternalModelCandidateApprovalInvalidationKind.MissingBaseline && value.artifactReference.contains(artifactReference("snapshot-optional")))) shouldBe Some(true)
              result.map(_.invalidations.filter(_.artifactReference.contains(artifactReference(artifactid))).map(_.kind)) shouldBe Some(kind.toVector)
            }
          }
        }
      }

      "order mixed source statuses by closed kind before unsigned UTF-8 artifact ID for every complete input permutation" in {
        Given("mixed declared changes incomplete observations unavailable evidence and optional absence")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(includeoptionalsnapshot = true)) { (root, data) =>
          val evidence = admit(root, data)
          val observations = unchangedSources.updated("snapshot-model", sourceDimensionObservation("snapshot-model", "revision")).updated("snapshot-scenario", sourceDimensionObservation("snapshot-scenario", "revision")).updated("snapshot-cml-alpha", sourceDimensionObservation("snapshot-cml-alpha", "missing-and-changed")).updated("snapshot-cml-beta", InternalModelLiveSourceObservation.Unavailable("offline")).updated("snapshot-glossary", InternalModelLiveSourceObservation.Unauthorized("denied"))
          val expected = Vector(
            _reason(InternalModelCandidateApprovalInvalidationKind.SourceChanged, "snapshot-model", Vector("source.revision")),
            _reason(InternalModelCandidateApprovalInvalidationKind.SourceChanged, "snapshot-scenario", Vector("source.revision")),
            _reason(InternalModelCandidateApprovalInvalidationKind.SourceIncomplete, "snapshot-cml-alpha", Vector("basis.projectRelativePath", "source.identity", "source.revision"), Vector("observed.source.revision")),
            _reason(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable, "snapshot-cml-beta"),
            _reason(InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized, "snapshot-glossary"),
            _reason(InternalModelCandidateApprovalInvalidationKind.MissingBaseline, "snapshot-optional")
          )
          val permutations = for { snapshotorder <- Gen.pick(evidence.snapshots.size, evidence.snapshots); observationorder <- Gen.pick(observations.size, observations.toVector) } yield (snapshotorder.toVector, observationorder.toVector)
          forAll(permutations) { case (snapshotorder, observationorder) =>
            When("generated complete permutations cross the pure evaluation boundary")
            val result = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(evidence.approval, evidence.review, data.executionbasis, snapshotorder, Map.from(observationorder), None)
            Then("the complete reason vector is kind-first and artifact-ID-second without losing either dimension set")
            result.toOption.map(_.invalidations) shouldBe Some(expected)
            result.toOption.map(_.approval) shouldBe Some(evidence.approval)
          }
        }
      }

      "retain every declared source metadata and CML target-path difference independently across the complete captured inventory" in {
        Given("independent authority identity revision and both CML path observations over every present source")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val evidence = admit(root, data)
          val dimensions = _sort_ids(unchangedSources.keys.toVector).flatMap(id => Vector("authority", "identity", "revision").map(dimension => (id, dimension)) ++ Option.when(id.startsWith("snapshot-cml-"))((id, "path")).toVector)
          dimensions.foreach { case (artifactid, dimension) =>
            When("one source-owned declared dimension changes")
            val result = _evaluate(evidence, evidence.review, data.executionbasis, unchangedSources.updated(artifactid, sourceDimensionObservation(artifactid, dimension)))
            Then("the source reason names that exact declared field and full baseline reference")
            val expected = if (dimension == "path") "basis.projectRelativePath" else "source." + dimension
            result.map(_.invalidations) shouldBe Some(Vector(_reason(InternalModelCandidateApprovalInvalidationKind.SourceChanged, artifactid, Vector(expected))))
          }
        }
      }

      "retain unknown baseline and observed revisions as SourceIncomplete with both changed and missing dimensions" in {
        Given("one actual original capture whose source owners explicitly supplied no revision")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(missingsourceversions = unchangedSources.keySet)) { (root, data) =>
          val evidence = admit(root, data)
          unchangedSources.keys.foreach { artifactid =>
            When("a complete observation cannot supply the missing baseline-owned revision")
            val baselineonly = _evaluate(evidence, evidence.review, data.executionbasis)
            Then("an unknown baseline revision alone remains incomplete")
            baselineonly.toVector.flatMap(_.invalidations).find(_.artifactReference.contains(artifactReference(artifactid))).map(_.missingDimensionNames) shouldBe Some(Vector("baseline.source.revision"))
            When("a source owner supplies a missing revision alongside changed identity and CML path")
            val result = _evaluate(evidence, evidence.review, data.executionbasis, unchangedSources.updated(artifactid, sourceDimensionObservation(artifactid, "missing-and-changed")))
            Then("unknown source revision never becomes the artifact revision and both dimension vectors remain observable")
            val reason = result.toVector.flatMap(_.invalidations).find(_.artifactReference.contains(artifactReference(artifactid))).get
            reason.kind shouldBe InternalModelCandidateApprovalInvalidationKind.SourceIncomplete
            reason.changedDimensionNames should contain ("source.identity")
            reason.missingDimensionNames shouldBe Vector("baseline.source.revision", "observed.source.revision")
            if (artifactid.startsWith("snapshot-cml-")) reason.changedDimensionNames should contain ("basis.projectRelativePath")
          }
        }
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val evidence = admit(root, data)
          unchangedSources.keys.foreach { artifactid =>
            When("only the observation-owned revision is unknown alongside changed identity or path")
            val result = _evaluate(evidence, evidence.review, data.executionbasis, unchangedSources.updated(artifactid, sourceDimensionObservation(artifactid, "missing-and-changed")))
            Then("an unknown observation stays SourceIncomplete and retains changed dimensions")
            val reason = result.toVector.flatMap(_.invalidations).find(_.artifactReference.contains(artifactReference(artifactid))).get
            reason.kind shouldBe InternalModelCandidateApprovalInvalidationKind.SourceIncomplete
            reason.changedDimensionNames should contain ("source.identity")
            reason.missingDimensionNames shouldBe Vector("observed.source.revision")
          }
        }
      }

      "not detect undeclared same-reference payload changes or substitute content for source-owned revisions" in {
        Given("the same declared typed references and source metadata with separately changed ordinary raw content")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val evidence = admit(root, data)
          unchangedSources.keys.foreach { artifactid =>
            When("only ordinary live raw payload differs under unchanged source-owned identity and revision")
            val result = _evaluate(evidence, evidence.review, data.executionbasis, unchangedSources.updated(artifactid, sourceDimensionObservation(artifactid, "content")))
            Then("no content-derived token or whole-file equality grants or invalidates applicability")
            result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Approved)
            result.map(_.invalidations) shouldBe Some(Vector.empty)
          }
          val first = evidence.snapshots.find(_.reference == artifactReference("snapshot-model")).get
          val parsed = io.circe.parser.parse(new String(first.bytes.get.toArray, StandardCharsets.UTF_8)).toOption.get
          val changed = replacePath(parsed, Vector("basis", "contextIdentity"), Json.fromString("producer content changed without revision"))
          val snapshots = evidence.snapshots.map(value => if (value == first) value.copy(bytes = Some(jsonBytes(changed).toVector)) else value)
          When("a structurally valid historical payload changes without a declared reference change")
          val result = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(evidence.approval, evidence.review, data.executionbasis, snapshots, unchangedSources, None)
          Then("structural parsing remains required but this boundary does not claim undeclared content mutation detection")
          result.isSuccess shouldBe true
          result.toOption.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Approved)
        }
      }
    }

    "enforce the portable link grammar" which {
      "accept ordinary strict JSON and reject root reference Unicode profile version role and integer variants" in {
        Given("one complete typed V2 link and closed structural mutations at every nested object")
        val valid = InternalModelCandidateApprovalSupersession(reference("predecessor", Long.MaxValue, InternalModelArtifactRole.Approval), reference("successor", 173L, InternalModelArtifactRole.Approval), "ccdm-candidate-approval-supersession-v2", "2.0")
        val json = _link_json(valid)
        val required = objectPaths(json).flatMap { path =>
          val fields = atPath(json, path).asObject.get
          fields.keys.toVector.flatMap { key =>
            Vector(replacePath(json, path, Json.fromJsonObject(fields.remove(key))), replacePath(json, path, Json.fromJsonObject(fields.add(key, Json.Null))), replacePath(json, path, Json.fromJsonObject(fields.add(key, Json.arr()))))
          } :+ replacePath(json, path, Json.fromJsonObject(fields.add("extra", Json.Null)))
        }
        val integers = Vector("0", "-1", "1.0", "1e1", "\"1\"", "null", "9223372036854775808").flatMap(token => Vector("predecessorApproval", "successorApproval").map(name => new String(jsonBytes(json), StandardCharsets.UTF_8).replace("\"" + name + "\":{\"artifactId\":", "\"" + name + "\":{\"artifactId\":").replace("\"artifactRevision\":" + (if (name == "predecessorApproval") Long.MaxValue.toString else "173"), "\"artifactRevision\":" + token).getBytes(StandardCharsets.UTF_8)))
        val variants = required.map(jsonBytes) ++ objectPaths(json).map(path => duplicateAtPath(json, path)) ++ integers ++ Vector(
          Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ jsonBytes(json),
          Array[Byte](0xc3.toByte),
          (new String(jsonBytes(json), StandardCharsets.UTF_8) + "trailing").getBytes(StandardCharsets.UTF_8),
          new String(jsonBytes(json), StandardCharsets.UTF_8).replace("predecessor", "\\ud800").getBytes(StandardCharsets.UTF_8),
          jsonBytes(replacePath(json, Vector("profile"), Json.fromString("ccdm-candidate-approval-supersession-v1"))),
          jsonBytes(replacePath(json, Vector("schemaVersion"), Json.fromString("1.0"))),
          jsonBytes(replacePath(json, Vector("predecessorApproval", "role"), Json.fromString("validation"))),
          jsonBytes(replacePath(json, Vector("successorApproval", "role"), Json.fromString("unknown"))),
          jsonBytes(replacePath(json, Vector("predecessorApproval"), Json.fromString("predecessor"))),
          jsonBytes(replacePath(json, Vector("predecessorApproval"), Json.obj("artifactId" -> Json.fromString("predecessor"), "sha256" -> Json.fromString("legacy")))),
          jsonBytes(replacePath(json, Vector("predecessorApproval", "artifactId"), Json.fromString("\u00a0"))),
          jsonBytes(replacePath(json, Vector("successorApproval", "artifactId"), Json.fromString("後任"))),
          jsonBytes(replacePath(json, Vector("successorApproval"), atPath(json, Vector("predecessorApproval"))))
        )
        When("the strict codec consumes complete ordinary JSON and every prohibited mutation")
        val decoded = InternalModelCandidateApprovalSupersessionCodec.decode(jsonBytes(json).toVector)
        val outcomes = variants.map { bytes =>
          Given("one negative serialized control derived from the complete V2 link")
          Then("the lexical or structural mutation actually changes its source bytes")
          bytes.toVector should not equal jsonBytes(json).toVector
          When("the changed strict JSON is decoded")
          InternalModelCandidateApprovalSupersessionCodec.decode(bytes.toVector).isRight
        }
        val harmless = Vector(Json.fromJsonObject(json.asObject.get.toVector.reverse.foldLeft(io.circe.JsonObject.empty) { case (fields, (key, value)) => fields.add(key, value) }).spaces2.getBytes(StandardCharsets.UTF_8), jsonBytes(json).dropRight(1), (" \r\n" + json.noSpaces + "\t").getBytes(StandardCharsets.UTF_8))
        Then("grammar rejects every variant while harmless ordering whitespace and LF differences preserve semantics")
        decoded shouldBe Right(valid)
        outcomes shouldBe Vector.fill(variants.size)(false)
        harmless.map(bytes => InternalModelCandidateApprovalSupersessionCodec.decode(bytes.toVector)) shouldBe Vector.fill(harmless.size)(Right(valid))
        InternalModelCandidateApprovalSupersessionCodec.validateValue(valid) shouldBe Right(())
        InternalModelCandidateApprovalSupersessionCodec.validateValue(null).isLeft shouldBe true
        InternalModelCandidateApprovalSupersessionCodec.validateValue(valid.copy(predecessorApproval = null)).isLeft shouldBe true
      }

      "preserve explicit reference domains positive Long values and deterministic writer bytes under property generation" in {
        Given("generated package-owned ASCII artifact IDs and explicitly positive producer revisions")
        val values = for { token <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString); revision <- Gen.oneOf(Gen.const(1L), Gen.const(Long.MaxValue), Gen.chooseNum(1L, Long.MaxValue)) } yield (token, revision)
        forAll(values) { case (token, revision) =>
          val value = InternalModelCandidateApprovalSupersession(reference("pre-" + token, revision, InternalModelArtifactRole.Approval), reference("post-" + token, revision, InternalModelArtifactRole.Approval), "ccdm-candidate-approval-supersession-v2", "2.0")
          When("the typed value is encoded twice and decoded without byte equality as admission")
          val first = InternalModelCandidateApprovalSupersessionCodec.encode(value).toVector
          val second = InternalModelCandidateApprovalSupersessionCodec.encode(value).toVector
          val decoded = InternalModelCandidateApprovalSupersessionCodec.decode(first)
          Then("all explicit ID revision and role fields round-trip and writer formatting is deterministic")
          first shouldBe second
          decoded shouldBe Right(value)
          decoded.map(_.predecessorApproval.artifactRevision.value) shouldBe Right(revision)
        }
      }
    }

    "keep precedence and capture boundaries explicit" which {
      "retain all source invalidations when a valid supersession takes precedence" in {
        Given("an original actual admission and separately admitted Rejected successor with unavailable model evidence")
        withFixturePair(FixtureOptions(approvalartifactrevision = 113L, approvalrevision = 127L, decision = InternalModelCandidateHumanApprovalDecision.Rejected)) { (original, successor, data) =>
          When("the explicit link and source-owner unavailability are evaluated together")
          val result = _evaluate(original, original.review, data.executionbasis, unchangedSources.updated("snapshot-model", InternalModelLiveSourceObservation.Unavailable("offline")), Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval)))
          Then("Superseded preserves the immutable Approved predecessor decision and SourceUnavailable reason")
          result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
          result.map(_.approval.record.approval.decision) shouldBe Some(InternalModelCandidateHumanApprovalDecision.Approved)
          result.map(_.invalidations) shouldBe Some(Vector(_reason(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable, "snapshot-model")))
        }
      }

      "retain multiple independent current-basis and source reasons when valid portable supersession takes precedence" in {
        Given("a separately admitted successor/current review with rule provider versions and current subject revision changed")
        withFixturePair(FixtureOptions(approvalartifactrevision = 113L, approvalrevision = 127L, ruleversion = "1.1", providerversion = "1.1", subjectrevision = 103L)) { (original, successor, data) =>
          When("all current evidence and the exact portable link are supplied")
          val result = _evaluate(original, successor.review, data.executionbasis, unchangedSources.updated("snapshot-model", sourceDimensionObservation("snapshot-model", "missing-and-changed")), Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval)))
          Then("Superseded retains both admissions and every subject rules providers and source reason")
          result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
          result.map(_.approval) shouldBe Some(original.approval)
          result.flatMap(_.supersession).map(_.successorApproval) shouldBe Some(successor.approval)
          result.map(_.invalidations.map(_.kind)) shouldBe Some(Vector(InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, InternalModelCandidateApprovalInvalidationKind.RulesChanged, InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, InternalModelCandidateApprovalInvalidationKind.SourceIncomplete))
          result.map(_.invalidations.last.missingDimensionNames) shouldBe Some(Vector("observed.source.revision"))
          result.map(_.invalidations.last.changedDimensionNames) shouldBe Some(Vector("source.identity", "source.revision"))
        }
      }

      "remain stable after the original fixture filesystem is mutated because only captured evidence is evaluated" in {
        Given("actual approval review and snapshots retained before the fixture paths change")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val evidence = admit(root, data)
          write(root.resolve("src/main/internal-model/manifest.yaml"), "changed\n".getBytes(StandardCharsets.UTF_8))
          write(root.resolve("src/main/internal-model/approvals/main.json"), "changed\n".getBytes(StandardCharsets.UTF_8))
          When("only captured values cross lifecycle after filesystem mutation")
          val result = _evaluate(evidence, evidence.review, data.executionbasis)
          val fresh = InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main"), artifactReference("review-main"), data.executionbasis, data.expected)
          Then("retained evaluation remains Approved while actual fresh admission fails")
          result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Approved)
          fresh.isSuccess shouldBe false
        }
      }

      "preserve an explicitly decoded portable link across independently captured task-private package carriers" in {
        Given("separate actual original and successor captures and transported ordinary JSON")
        withFixturePair(FixtureOptions(approvalartifactrevision = 113L, approvalrevision = 127L)) { (original, successor, data) =>
          val declared = _link(original.approval, successor.approval)
          val transported = Json.fromJsonObject(_link_json(declared).asObject.get.toVector.reverse.foldLeft(io.circe.JsonObject.empty) { case (fields, (key, value)) => fields.add(key, value) }).spaces2.getBytes(StandardCharsets.UTF_8).toVector
          When("a reconstructed typed link is evaluated without path identity or process-local cache")
          val decoded = InternalModelCandidateApprovalSupersessionCodec.decode(transported).toOption.get
          val result = _evaluate(original, original.review, data.executionbasis, supersession = Some(InternalModelCandidateApprovalSupersessionInput(decoded, successor.approval)))
          Then("the exact external full-reference selection and separately admitted record survive transport")
          decoded shouldBe declared
          result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
          result.flatMap(_.supersession).map(_.record) shouldBe Some(declared)
        }
      }

      "remain independent of source-observation map insertion order under property generation" in {
        Given("one actual capture and generated complete observation insertion orders")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val evidence = admit(root, data)
          forAll(Gen.pick(unchangedSources.size, unchangedSources.toVector)) { generated =>
            When("the same complete observation map is constructed in forward and reverse order")
            val first = _evaluate(evidence, evidence.review, data.executionbasis, Map.from(generated))
            val second = _evaluate(evidence, evidence.review, data.executionbasis, Map.from(generated.reverse))
            Then("state retained admissions and ordered invalidations are identical")
            first shouldBe second
          }
        }
      }

      "give valid supersession precedence over generated source drift without mutating retained history" in {
        Given("an original actual approval and successor plus generated declared source changes or unavailable evidence")
        withFixturePair(FixtureOptions(approvalartifactrevision = 113L, approvalrevision = 127L)) { (original, successor, data) =>
          forAll(Gen.oneOf(sourceDimensionObservation("snapshot-model", "revision"), sourceDimensionObservation("snapshot-model", "missing-and-changed"), InternalModelLiveSourceObservation.Unavailable("offline"))) { observation =>
            When("the exact link accompanies generated source-owner evidence")
            val result = _evaluate(original, original.review, data.executionbasis, unchangedSources.updated("snapshot-model", observation), Some(InternalModelCandidateApprovalSupersessionInput(_link(original.approval, successor.approval), successor.approval)))
            Then("Superseded retains nonempty complete invalidations and both original decisions without mutation")
            result.map(_.state) shouldBe Some(InternalModelCandidateApprovalLifecycleState.Superseded)
            result.map(_.invalidations.nonEmpty) shouldBe Some(true)
            result.map(_.approval) shouldBe Some(original.approval)
            result.flatMap(_.supersession).map(_.successorApproval) shouldBe Some(successor.approval)
          }
        }
      }

      "reject null approval inputs instead of manufacturing an applicability report" in {
        Given("a null original approval admission")
        When("the pure evaluator is invoked with missing evidence")
        val result = InternalModelCandidateApprovalLifecycleEvaluator.evaluate(null, null, null, null, null, null)
        Then("a structured unsuccessful result replaces any fabricated report")
        result.isSuccess shouldBe false
      }

      "reject null containers invalid execution basis and structurally forged approval review or carrier values" in {
        Given("actual captures followed by one contradictory typed semantic field or outer container")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val evidence = admit(root, data)
          val approval = evidence.approval
          val review = evidence.review
          val malformedapproval = approval.copy(record = approval.record.copy(profile = "unsupported"))
          val malformedreview = review.copy(binding = review.binding.copy(subject = review.binding.subject.copy(artifacts = review.binding.subject.artifacts.drop(1))))
          val wrongrole = review.copy(reviewArtifactReference = review.reviewArtifactReference.copy(role = InternalModelArtifactRole.Approval))
          val carrier = review.carrierPackageContext
          val malformedcarrier = review.copy(carrierPackageContext = carrier.copy(schemaversion = "1.0"))
          val malformedlifecycle = review.copy(carrierPackageContext = carrier.copy(lifecyclestate = "not a token"))
          val approvedbasis = approval.record.approval.basis
          val mismatchedapproval = approval.copy(record = approval.record.copy(approval = approval.record.approval.copy(basis = approvedbasis.copy(candidateReference = recordReference("other-candidate", 7L)))))
          When("each malformed outer graph or inconsistent nested semantic admission crosses the pure evaluator")
          val outcomes = Vector(
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, null, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, evidence.snapshots, null, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, InternalModelCandidateReviewExecutionBasis(Vector.empty, Vector.empty), evidence.snapshots, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(malformedapproval, review, data.executionbasis, evidence.snapshots, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(mismatchedapproval, review, data.executionbasis, evidence.snapshots, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, malformedreview, data.executionbasis, evidence.snapshots, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, wrongrole, data.executionbasis, evidence.snapshots, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, malformedcarrier, data.executionbasis, evidence.snapshots, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, malformedlifecycle, data.executionbasis, evidence.snapshots, unchangedSources, None),
            InternalModelCandidateApprovalLifecycleEvaluator.evaluate(approval, review, data.executionbasis, evidence.snapshots, unchangedSources, null)
          ).map(_.isSuccess)
          Then("structural helpers reject malformed graph schema lifecycle syntax and cross-record contradictions instead of reauthenticating human input")
          outcomes shouldBe Vector.fill(outcomes.size)(false)
        }
      }
    }
  }

  private def _evaluate(
    original: FixtureEvidence,
    current: InternalModelCandidateReviewBindingAdmission,
    executionbasis: InternalModelCandidateReviewExecutionBasis,
    observations: Map[String, InternalModelLiveSourceObservation] = unchangedSources,
    supersession: Option[InternalModelCandidateApprovalSupersessionInput] = None
  ): Option[InternalModelCandidateApprovalLifecycleReport] =
    InternalModelCandidateApprovalLifecycleEvaluator.evaluate(original.approval, current, executionbasis, original.snapshots, observations, supersession).toOption

  private def _link(predecessor: InternalModelCandidateHumanApprovalAdmission, successor: InternalModelCandidateHumanApprovalAdmission): InternalModelCandidateApprovalSupersession =
    InternalModelCandidateApprovalSupersession(predecessor.approvalArtifactReference, successor.approvalArtifactReference, "ccdm-candidate-approval-supersession-v2", "2.0")

  private def _reason(kind: InternalModelCandidateApprovalInvalidationKind, artifactid: String, changed: Vector[String] = Vector.empty, missing: Vector[String] = Vector.empty): InternalModelCandidateApprovalInvalidation =
    InternalModelCandidateApprovalInvalidation(kind, Some(artifactReference(artifactid)), changed, missing)

  private def _link_json(value: InternalModelCandidateApprovalSupersession): Json =
    io.circe.parser.parse(new String(InternalModelCandidateApprovalSupersessionCodec.encode(value), StandardCharsets.UTF_8)).toOption.get

  private def _sort_ids(values: Vector[String]): Vector[String] =
    values.sortWith((left, right) => Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)) < 0)
}
