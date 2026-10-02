package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 11, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
final class CandidateDesignSemanticDiffIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _context = MonoKotoProjectionContextIdentity("context-sales")
  private val _component = ComponentDashboardComponentIdentity("component-sales")
  private val _condition = ComponentDashboardCondition(
    "available",
    "authorized",
    None,
    None,
    None,
    None,
    None,
    None,
    Vector("caller-admitted evidence only")
  )
  private val _absence_condition = _condition.copy(explicitAbsence = Some("no prior semantic assertion"))
  private val _attribution = ComponentDashboardSourceAttribution(
    "source-analysis",
    "analysis-scope",
    "analysis://proposal-1"
  )

  "Candidate design semantic-diff integration" should {
    "retain only the exact selected direct impacts for an admitted projection" in {
      Given("a projection and caller-admitted links for one proposal")
      val projection = _projection()
      When("the integration retains the selected direct impacts")
      val result = _result(projection, Vector("link-2", "link-1"))

      Then("the result retains only the exact selected proposal, records, links, and semantic diffs")
      result shouldBe a[CandidateDesignSemanticDiffProjectedIntegration]
      val integration = result.asInstanceOf[CandidateDesignSemanticDiffProjectedIntegration].sourceIntegration
      integration.selectedProposal.sourceProposal.id shouldBe "proposal-1"
      integration.selectedImpactRecords.map(_.sourceRecord.id) shouldBe Vector("record-1", "record-2")
      integration.selectedImpactLinks.map(_.sourceLink.id) shouldBe Vector("link-1", "link-2")
      integration.semanticDiffEntries.map(_.id) shouldBe Vector("diff-1", "diff-2")
    }

    "not infer an unselected projection record" in {
      Given("a projection with an unselected direct impact")
      val projection = _projection()
      When("only link-1 is selected for integration")
      val result = _result(projection, Vector("link-1"))

      Then("no unselected record or link is retained in the integration")
      result shouldBe a[CandidateDesignSemanticDiffProjectedIntegration]
      val integration = result.asInstanceOf[CandidateDesignSemanticDiffProjectedIntegration].sourceIntegration
      integration.selectedImpactRecords.map(_.sourceRecord.id) shouldBe Vector("record-1")
      integration.selectedImpactLinks.map(_.sourceLink.id) shouldBe Vector("link-1")
      integration.productElementNames.toSet should not contain "sourceProjection"
    }

    "return a typed all-or-nothing rejection for invalid admitted evidence" in {
      Given("an integration request with invalid caller-admitted evidence")
      val projection = _projection()
      val invalidpatch = _patch().copy(
        cmlOwner = " ",
        cmlLocator = " ",
        baselineArtifactReference = null,
        proposedContentReference = null,
        condition = _condition.copy(availability = " "),
        limitations = Vector(" ")
      )
      val invalidreview = _review().copy(state = " ")
      val invalidgovernance = _governance().copy(reference = " ")
      When("the invalid evidence is submitted")
      val result = CandidateDesignSemanticDiffIntegration.create(
        projection,
        "missing-proposal",
        Vector("missing-link"),
        invalidpatch,
        _candidate().copy(context = MonoKotoProjectionContextIdentity("other-context")),
        Vector(_diff().copy(patchId = "other-patch", before = None, condition = _condition)),
        invalidreview,
        invalidgovernance
      )

      Then("it produces a typed rejection with every observable admission violation")
      result shouldBe a[CandidateDesignSemanticDiffRejectedIntegration]
      val failure = result.asInstanceOf[CandidateDesignSemanticDiffRejectedIntegration].failure
      failure.violations.nonEmpty shouldBe true
      failure.violations.exists(_.contains("unknown proposal")) shouldBe true
      failure.violations.exists(_.contains("unknown selected impact link")) shouldBe true
      failure.violations.exists(_.contains("CML owner")) shouldBe true
      failure.violations.exists(_.contains("baseline artifact reference")) shouldBe true
      failure.violations.exists(_.contains("proposed content reference")) shouldBe true
      failure.violations.exists(_.contains("CML locator")) shouldBe true
      failure.violations.exists(_.contains("condition availability")) shouldBe true
      failure.violations.exists(_.contains("limitations")) shouldBe true
      failure.violations.exists(_.contains("candidate context")) shouldBe true
      failure.violations.exists(_.contains("semantic diff patch")) shouldBe true
      failure.violations.exists(_.contains("explicit absence")) shouldBe true
      failure.violations.exists(_.contains("review state")) shouldBe true
      failure.violations.exists(_.contains("Git governance reference")) shouldBe true
    }

    "reject duplicate identities or selected direct links without a partial projection" in {
      Given("a request containing duplicate selected-link and semantic-diff identities")
      val projection = _projection()
      When("the duplicate evidence is submitted")
      val result = CandidateDesignSemanticDiffIntegration.create(
        projection,
        "proposal-1",
        Vector("link-1", "link-1"),
        _patch(),
        _candidate(),
        Vector(_diff("diff-1"), _diff("diff-1")),
        _review(),
        _governance()
      )

      Then("the request is rejected without a partial projected integration")
      result shouldBe a[CandidateDesignSemanticDiffRejectedIntegration]
      result.isInstanceOf[CandidateDesignSemanticDiffProjectedIntegration] shouldBe false
    }

    "retain review and Git governance traceability as references only" in {
      Given("valid caller-admitted review and Git governance references")
      val projection = _projection()
      When("the integration is created")
      val result = _result(projection, Vector("link-1"))

      Then("the references remain traceability evidence only")
      result shouldBe a[CandidateDesignSemanticDiffProjectedIntegration]
      val integration = result.asInstanceOf[CandidateDesignSemanticDiffProjectedIntegration].sourceIntegration
      integration.candidateReview.id shouldBe "review-1"
      integration.gitGovernanceReference.reference shouldBe "git://governance/change-1"
      integration.gitGovernanceReference.reviewSnapshotId shouldBe integration.candidateReview.id
    }

    "preserve identity-first deterministic ordering across permutations" in {
      val ids = Vector("link-1", "link-2")
      forAll(Gen.pick(ids.size, ids).map(_.toVector).suchThat(_.size == ids.size)) { permutation =>
        Given("a permutation of the same admitted links and semantic diff entries")
        val projection = _projection()
        val diffs = permutation.zipWithIndex.map { case (_, index) =>
          if (index == 0) _diff("diff-2") else _diff("diff-1")
        }
        When("the permutation is submitted for integration")
        val result = CandidateDesignSemanticDiffIntegration.create(
          projection,
          "proposal-1",
          permutation,
          _patch(),
          _candidate(),
          diffs,
          _review(),
          _governance()
        )

        Then("the retained link and semantic-diff order is deterministic")
        result shouldBe a[CandidateDesignSemanticDiffProjectedIntegration]
        val integration = result.asInstanceOf[CandidateDesignSemanticDiffProjectedIntegration].sourceIntegration
        integration.selectedImpactLinks.map(_.sourceLink.id) shouldBe ids
        integration.semanticDiffEntries.map(_.id) shouldBe Vector("diff-1", "diff-2")
      }
    }

    "reject empty semantic-diff evidence without a partial integration" in {
      Given("an otherwise valid integration request with no semantic diff entries")
      val projection = _projection()
      When("the empty semantic-diff vector is submitted")
      val result = CandidateDesignSemanticDiffIntegration.create(
        projection,
        "proposal-1",
        Vector("link-1"),
        _patch(),
        _candidate(),
        Vector.empty,
        _review(),
        _governance()
      )

      Then("the request is rejected without a partial projected integration")
      result shouldBe a[CandidateDesignSemanticDiffRejectedIntegration]
      result.isInstanceOf[CandidateDesignSemanticDiffProjectedIntegration] shouldBe false
    }

    "reject a candidate model whose explicit patch association differs from the proposed patch" in {
      Given("an otherwise valid candidate model associated with another patch")
      val projection = _projection()
      val mismatchedcandidate = _candidate().copy(patchId = "patch-other")
      When("the mismatched candidate model is submitted")
      val result = CandidateDesignSemanticDiffIntegration.create(
        projection,
        "proposal-1",
        Vector("link-1"),
        _patch(),
        mismatchedcandidate,
        Vector(_diff()),
        _review(),
        _governance()
      )

      Then("the request is rejected without a partial projected integration")
      result shouldBe a[CandidateDesignSemanticDiffRejectedIntegration]
      result.isInstanceOf[CandidateDesignSemanticDiffProjectedIntegration] shouldBe false
    }

    "retain independently supplied artifact and content revisions through the full positive Long domain" in {
      forAll(Gen.oneOf(1L, Long.MaxValue, 37L), Gen.oneOf(1L, Long.MaxValue, 73L)) { (artifactrevision, recordrevision) =>
        Given("a source-snapshot reference and an independent proposed-content record reference")
        val baseline = _artifact_reference("baseline-source", artifactrevision)
        val content = _record_reference("proposed-content-漢", recordrevision)
        val patch = _patch().copy(baselineArtifactReference = baseline, proposedContentReference = content)
        val projection = _projection()
        When("the caller supplies those references to actual integration creation")
        val result = _result_with_patch(projection, patch)
        Then("the integration preserves both supplied domains and all patch metadata")
        result shouldBe a[CandidateDesignSemanticDiffProjectedIntegration]
        result.asInstanceOf[CandidateDesignSemanticDiffProjectedIntegration].sourceIntegration.proposedCmlPatch shouldBe patch
        baseline.artifactRevision.value shouldBe artifactrevision
        content.recordRevision.value shouldBe recordrevision
      }
    }

    "collect malformed reference domains, roles and revisions without partial integration" in {
      Given("otherwise valid evidence with independently malformed supplied baseline or content metadata")
      val patch = _patch()
      val baseline = patch.baselineArtifactReference
      val content = patch.proposedContentReference
      val cases = Vector(
        patch.copy(baselineArtifactReference = null),
        patch.copy(proposedContentReference = null),
        patch.copy(baselineArtifactReference = baseline.copy(artifactId = "bad id".asInstanceOf[InternalModelArtifactId])),
        patch.copy(baselineArtifactReference = baseline.copy(artifactId = null.asInstanceOf[InternalModelArtifactId])),
        patch.copy(baselineArtifactReference = baseline.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision])),
        patch.copy(baselineArtifactReference = baseline.copy(artifactRevision = (-1L).asInstanceOf[InternalModelArtifactRevision])),
        patch.copy(baselineArtifactReference = baseline.copy(role = InternalModelArtifactRole.Projection)),
        patch.copy(baselineArtifactReference = baseline.copy(role = null)),
        patch.copy(proposedContentReference = content.copy(recordId = " ".asInstanceOf[InternalModelRecordId])),
        patch.copy(proposedContentReference = content.copy(recordId = null.asInstanceOf[InternalModelRecordId])),
        patch.copy(proposedContentReference = content.copy(recordId = "\uD800".asInstanceOf[InternalModelRecordId])),
        patch.copy(proposedContentReference = content.copy(recordRevision = 0L.asInstanceOf[InternalModelRecordRevision])),
        patch.copy(proposedContentReference = content.copy(recordRevision = (-1L).asInstanceOf[InternalModelRecordRevision])),
        patch.copy(
          baselineArtifactReference = baseline.copy(artifactId = "bad id".asInstanceOf[InternalModelArtifactId], artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision], role = null),
          proposedContentReference = content.copy(recordId = " ".asInstanceOf[InternalModelRecordId], recordRevision = 0L.asInstanceOf[InternalModelRecordRevision])
        )
      )
      val projection = _projection()
      When("actual creation admits the positive control and each malformed variant")
      val control = _result_with_patch(projection, patch)
      val results = cases.map(value => _result_with_patch(projection, value))
      Then("the control succeeds, every malformed reference rejects and independent errors remain collected")
      control shouldBe a[CandidateDesignSemanticDiffProjectedIntegration]
      results.foreach(result => result shouldBe a[CandidateDesignSemanticDiffRejectedIntegration])
      results.last.asInstanceOf[CandidateDesignSemanticDiffRejectedIntegration].failure.violations.size shouldBe 5
    }
  }

  private def _result_with_patch(projection: AnalysisDesignImpactProjection, patch: CandidateDesignProposedCmlPatchTrace): CandidateDesignSemanticDiffIntegrationResult =
    CandidateDesignSemanticDiffIntegration.create(projection, "proposal-1", Vector("link-1"), patch, _candidate(), Vector(_diff()), _review(), _governance())

  private def _artifact_reference(id: String, revision: Long): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from(id).toOption.get, InternalModelArtifactRevision.from(revision).toOption.get, InternalModelArtifactRole.SourceSnapshot)

  private def _record_reference(id: String, revision: Long): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(id).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)

  private def _result(
    projection: AnalysisDesignImpactProjection,
    linkids: Vector[String]
  ): CandidateDesignSemanticDiffIntegrationResult =
    CandidateDesignSemanticDiffIntegration.create(
      projection,
      "proposal-1",
      linkids,
      _patch(),
      _candidate(),
      Vector(_diff("diff-2"), _diff("diff-1")),
      _review(),
      _governance()
    )

  private def _projection(): AnalysisDesignImpactProjection = {
    val proposal = AnalysisDesignImpactProposal(
      "proposal-1",
      _context,
      _component,
      ComponentDashboardSemanticTargetIdentity("origin-1"),
      "origin-record-1",
      AnalysisDesignMonoKotoOrigin,
      AnalysisDesignMonoKotoSubject,
      _attribution,
      _condition,
      "analyst",
      "propose a candidate",
      Vector.empty,
      Some("proposal-tie")
    )
    val record1 = AnalysisDesignImpactRecord(
      "record-1",
      _context,
      _component,
      Some(ComponentDashboardSemanticTargetIdentity("target-1")),
      AnalysisDesignImpactEntity,
      AnalysisDesignImpactEntitySubject,
      _attribution,
      _condition,
      Some("record-1-tie")
    )
    val record2 = AnalysisDesignImpactRecord(
      "record-2",
      _context,
      _component,
      Some(ComponentDashboardSemanticTargetIdentity("target-2")),
      AnalysisDesignImpactEvent,
      AnalysisDesignImpactEventSubject,
      _attribution,
      _condition,
      Some("record-2-tie")
    )
    val link1 = AnalysisDesignImpactLink(
      "link-1",
      _context,
      _component,
      "proposal-1",
      "record-1",
      ComponentDashboardSemanticTargetIdentity("relationship-1"),
      _attribution,
      _condition,
      Vector.empty,
      Some("link-1-tie")
    )
    val link2 = AnalysisDesignImpactLink(
      "link-2",
      _context,
      _component,
      "proposal-1",
      "record-2",
      ComponentDashboardSemanticTargetIdentity("relationship-2"),
      _attribution,
      _condition,
      Vector.empty,
      Some("link-2-tie")
    )
    val projectedproposal = AnalysisDesignImpactProjectedProposal(proposal)
    val projectedrecord1 = AnalysisDesignImpactProjectedRecord(record1)
    val projectedrecord2 = AnalysisDesignImpactProjectedRecord(record2)
    AnalysisDesignImpactProjection(
      _context,
      _component,
      Vector(projectedproposal),
      Vector(projectedrecord1, projectedrecord2),
      Vector(
        AnalysisDesignImpactProjectedLink(link1, proposal, record1),
        AnalysisDesignImpactProjectedLink(link2, proposal, record2)
      ),
      Vector.empty
    )
  }

  private def _patch(): CandidateDesignProposedCmlPatchTrace =
    CandidateDesignProposedCmlPatchTrace(
      "patch-1",
      _context,
      _component,
      "cml-owner",
      "cml://candidate-1",
      _artifact_reference("baseline-source", 11L),
      _record_reference("proposed-content", 31L),
      _attribution,
      _condition,
      Vector("proposal evidence"),
      Some("patch-tie")
    )

  private def _candidate(): CandidateDesignCandidateModelIdentity =
    CandidateDesignCandidateModelIdentity(
      "candidate-1",
      _context,
      _component,
      "patch-1",
      _attribution,
      _condition,
      Some("candidate-tie")
    )

  private def _diff(id: String = "diff-1"): CandidateDesignSemanticDiffEntry =
    CandidateDesignSemanticDiffEntry(
      id,
      _context,
      _component,
      "patch-1",
      "candidate-1",
      "caller-category",
      "caller-action",
      "caller-subject",
      Some("caller-before"),
      Some("caller-after"),
      "caller-relationship",
      _attribution,
      _absence_condition,
      Vector("diff evidence"),
      Some(s"$id-tie")
    )

  private def _review(): CandidateDesignReviewSnapshot =
    CandidateDesignReviewSnapshot(
      "review-1",
      _context,
      _component,
      "patch-1",
      "candidate-1",
      "recorded",
      _attribution,
      _condition,
      Vector("review trace only"),
      Some("review-tie")
    )

  private def _governance(): CandidateDesignGitGovernanceReference =
    CandidateDesignGitGovernanceReference(
      "governance-1",
      _context,
      _component,
      "patch-1",
      "candidate-1",
      "review-1",
      "git://governance/change-1",
      "recorded",
      _attribution,
      _condition,
      Vector("reference only"),
      Some("governance-tie")
    )
}
