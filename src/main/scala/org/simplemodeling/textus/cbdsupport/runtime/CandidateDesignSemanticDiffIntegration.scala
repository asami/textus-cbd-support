package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.{CodingErrorAction, StandardCharsets}

/*
 * @since   Sep. 11, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * Caller-admitted evidence for a proposed CML change.  This value deliberately
 * carries trace information only: it never reads, parses, or applies CML.
 */
final case class CandidateDesignProposedCmlPatchTrace(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  cmlOwner: String,
  cmlLocator: String,
  baselineArtifactReference: InternalModelArtifactReference,
  proposedContentReference: InternalModelRecordReference,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

/** An exact, externally supplied identity for a candidate model. */
final case class CandidateDesignCandidateModelIdentity(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  patchId: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

/**
 * A caller-admitted semantic difference.  Category, action, subject,
 * before/after, and relationship remain open evidence values; this boundary
 * does not infer a vocabulary or perform text differencing.
 */
final case class CandidateDesignSemanticDiffEntry(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  patchId: String,
  candidateModelId: String,
  category: String,
  action: String,
  subject: String,
  before: Option[String],
  after: Option[String],
  relationship: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

/** An externally supplied review snapshot; review execution is outside this boundary. */
final case class CandidateDesignReviewSnapshot(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  patchId: String,
  candidateModelId: String,
  state: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

/**
 * A reference to external Git governance.  It is traceability evidence only;
 * it neither invokes Git nor represents an approval decision.
 */
final case class CandidateDesignGitGovernanceReference(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  patchId: String,
  candidateModelId: String,
  reviewSnapshotId: String,
  reference: String,
  state: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

/** A typed rejection containing every detected admission violation. */
final case class CandidateDesignSemanticDiffIntegrationFailure(
  violations: Vector[String]
)

/**
 * The immutable, caller-admitted integration.  Its selected impact evidence
 * can only originate from the supplied AnalysisDesignImpactProjection.
 */
final case class CandidateDesignSemanticDiffIntegration(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  selectedProposal: AnalysisDesignImpactProjectedProposal,
  selectedImpactRecords: Vector[AnalysisDesignImpactProjectedRecord],
  selectedImpactLinks: Vector[AnalysisDesignImpactProjectedLink],
  proposedCmlPatch: CandidateDesignProposedCmlPatchTrace,
  candidateModel: CandidateDesignCandidateModelIdentity,
  semanticDiffEntries: Vector[CandidateDesignSemanticDiffEntry],
  candidateReview: CandidateDesignReviewSnapshot,
  gitGovernanceReference: CandidateDesignGitGovernanceReference
)

sealed trait CandidateDesignSemanticDiffIntegrationResult

final case class CandidateDesignSemanticDiffProjectedIntegration(
  sourceIntegration: CandidateDesignSemanticDiffIntegration
) extends CandidateDesignSemanticDiffIntegrationResult

final case class CandidateDesignSemanticDiffRejectedIntegration(
  failure: CandidateDesignSemanticDiffIntegrationFailure
) extends CandidateDesignSemanticDiffIntegrationResult

object CandidateDesignSemanticDiffIntegration {
  def create(
    projection: AnalysisDesignImpactProjection,
    proposalId: String,
    selectedImpactLinkIds: Vector[String],
    proposedCmlPatch: CandidateDesignProposedCmlPatchTrace,
    candidateModel: CandidateDesignCandidateModelIdentity,
    semanticDiffEntries: Vector[CandidateDesignSemanticDiffEntry],
    candidateReview: CandidateDesignReviewSnapshot,
    gitGovernanceReference: CandidateDesignGitGovernanceReference
  ): CandidateDesignSemanticDiffIntegrationResult = {
    val proposal = projection.proposals.find(_.sourceProposal.id == proposalId)
    val selectedlinks = selectedImpactLinkIds.flatMap { linkid =>
      projection.links.find(_.sourceLink.id == linkid)
    }
    val baseviolations = _projection_violations(
      projection,
      proposalId,
      selectedImpactLinkIds,
      proposal,
      selectedlinks
    )
    val admittedviolations = _admitted_violations(
      projection.context,
      projection.component,
      proposedCmlPatch,
      candidateModel,
      semanticDiffEntries,
      candidateReview,
      gitGovernanceReference
    )
    val associationviolations = _association_violations(
      proposalId,
      selectedlinks,
      proposedCmlPatch,
      candidateModel,
      semanticDiffEntries,
      candidateReview,
      gitGovernanceReference
    )
    val violations = baseviolations ++ admittedviolations ++ associationviolations

    if (violations.nonEmpty)
      CandidateDesignSemanticDiffRejectedIntegration(
        CandidateDesignSemanticDiffIntegrationFailure(violations.distinct)
      )
    else {
      val orderedlinks = _order_links(selectedlinks)
      val selectedrecordids = orderedlinks.map(_.impactRecord.id).toSet
      val orderedrecords = _order_records(
        projection.records.filter(record => selectedrecordids.contains(record.sourceRecord.id))
      )
      val ordereddiffs = _order_diffs(semanticDiffEntries)
      CandidateDesignSemanticDiffProjectedIntegration(
        CandidateDesignSemanticDiffIntegration(
          projection.context,
          projection.component,
          proposal.get,
          orderedrecords,
          orderedlinks,
          proposedCmlPatch,
          candidateModel,
          ordereddiffs,
          candidateReview,
          gitGovernanceReference
        )
      )
    }
  }

  private def _projection_violations(
    projection: AnalysisDesignImpactProjection,
    proposalid: String,
    selectedlinkids: Vector[String],
    proposal: Option[AnalysisDesignImpactProjectedProposal],
    selectedlinks: Vector[AnalysisDesignImpactProjectedLink]
  ): Vector[String] = {
    val emptyproposal = if (_nonblank(proposalid)) Vector.empty else Vector("proposal id must be nonblank")
    val emptyselction =
      if (selectedlinkids.nonEmpty) Vector.empty else Vector("selected impact link ids must be nonempty")
    val duplicateids = _duplicate_ids(selectedlinkids).map(id => s"duplicate selected impact link id: $id")
    val missingproposal = if (proposal.isDefined) Vector.empty else Vector(s"unknown proposal id: $proposalid")
    val missinglinks = selectedlinkids.filterNot(id => projection.links.exists(_.sourceLink.id == id))
      .map(id => s"unknown selected impact link id: $id")
    val wrongproposal = proposal.toVector.flatMap { selectedproposal =>
      selectedlinks.filterNot(_.proposal.id == selectedproposal.sourceProposal.id)
        .map(link => s"selected impact link does not belong to proposal: ${link.sourceLink.id}")
    }

    emptyproposal ++ emptyselction ++ duplicateids ++ missingproposal ++ missinglinks ++ wrongproposal
  }

  private def _admitted_violations(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    patch: CandidateDesignProposedCmlPatchTrace,
    candidate: CandidateDesignCandidateModelIdentity,
    diffs: Vector[CandidateDesignSemanticDiffEntry],
    review: CandidateDesignReviewSnapshot,
    governance: CandidateDesignGitGovernanceReference
  ): Vector[String] = {
    (if (diffs.nonEmpty) Vector.empty else Vector("semantic diff entries must be nonempty")) ++
      _patch_violations(context, component, patch) ++
      _candidate_violations(context, component, candidate) ++
      diffs.flatMap(_diff_violations(context, component, _)) ++
      _review_violations(context, component, review) ++
      _governance_violations(context, component, governance) ++
      _duplicate_ids(diffs.map(_.id)).map(id => s"duplicate semantic diff id: $id") ++
      _duplicate_ids(Vector(patch.id, candidate.id, review.id, governance.id))
        .map(id => s"duplicate admitted identity: $id")
  }

  private def _association_violations(
    proposalid: String,
    selectedlinks: Vector[AnalysisDesignImpactProjectedLink],
    patch: CandidateDesignProposedCmlPatchTrace,
    candidate: CandidateDesignCandidateModelIdentity,
    diffs: Vector[CandidateDesignSemanticDiffEntry],
    review: CandidateDesignReviewSnapshot,
    governance: CandidateDesignGitGovernanceReference
  ): Vector[String] = {
    val patchcandidate =
      if (_nonblank(patch.id) && _nonblank(candidate.id)) Vector.empty
      else Vector("patch and candidate identities must be nonblank")
    val diffassociations = diffs.flatMap { diff =>
      _association_violation(diff.patchId, patch.id, "semantic diff patch") ++
        _association_violation(diff.candidateModelId, candidate.id, "semantic diff candidate")
    }
    val reviewassociations =
      _association_violation(review.patchId, patch.id, "review patch") ++
        _association_violation(review.candidateModelId, candidate.id, "review candidate")
    val governanceassociations =
      _association_violation(governance.patchId, patch.id, "governance patch") ++
        _association_violation(governance.candidateModelId, candidate.id, "governance candidate") ++
        _association_violation(governance.reviewSnapshotId, review.id, "governance review")
    val linkownership = selectedlinks.filterNot(_.proposal.id == proposalid)
      .map(link => s"selected impact link has incompatible proposal association: ${link.sourceLink.id}")

    patchcandidate ++
      _association_violation(candidate.patchId, patch.id, "candidate patch") ++
      diffassociations ++ reviewassociations ++ governanceassociations ++ linkownership
  }

  private def _patch_violations(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    patch: CandidateDesignProposedCmlPatchTrace
  ): Vector[String] = {
    _identity_violations("patch", patch.id, patch.context, patch.component, context, component) ++
      _required_violations(
        Vector(
          "CML owner" -> patch.cmlOwner,
          "CML locator" -> patch.cmlLocator
        )
      ) ++
      _baseline_reference_violations(patch.baselineArtifactReference) ++
      _content_reference_violations(patch.proposedContentReference) ++
      _attribution_violations("patch", patch.attribution) ++
      _condition_violations("patch", patch.condition) ++
      _limitations_violations("patch", patch.limitations)
  }

  private def _baseline_reference_violations(reference: InternalModelArtifactReference): Vector[String] =
    if (reference == null) Vector("patch baseline artifact reference must be present")
    else {
      val idviolations = InternalModelArtifactId.from(reference.artifactId.value).fold(reason => Vector(s"patch baseline $reason"), _ => Vector.empty)
      val revisionviolations = InternalModelArtifactRevision.from(reference.artifactRevision.value).fold(reason => Vector(s"patch baseline $reason"), _ => Vector.empty)
      val roleviolations = if (reference.role == InternalModelArtifactRole.SourceSnapshot) Vector.empty else Vector("patch baseline artifact role must be source-snapshot")
      idviolations ++ revisionviolations ++ roleviolations
    }

  private def _content_reference_violations(reference: InternalModelRecordReference): Vector[String] =
    if (reference == null) Vector("patch proposed content reference must be present")
    else {
      val id = reference.recordId.value
      val encoder = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
      val idviolations = if (InternalModelRecordId.from(id).isRight && encoder.canEncode(id)) Vector.empty else Vector("patch proposed content recordId must be nonblank valid Unicode")
      val revisionviolations = InternalModelRecordRevision.from(reference.recordRevision.value).fold(reason => Vector(s"patch proposed content $reason"), _ => Vector.empty)
      idviolations ++ revisionviolations
    }

  private def _candidate_violations(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    candidate: CandidateDesignCandidateModelIdentity
  ): Vector[String] = {
    _identity_violations("candidate", candidate.id, candidate.context, candidate.component, context, component) ++
      _required_violations(Vector("candidate patch id" -> candidate.patchId)) ++
      _attribution_violations("candidate", candidate.attribution) ++
      _condition_violations("candidate", candidate.condition)
  }

  private def _diff_violations(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    diff: CandidateDesignSemanticDiffEntry
  ): Vector[String] = {
    _identity_violations("semantic diff", diff.id, diff.context, diff.component, context, component) ++
      _required_violations(
        Vector(
          "semantic diff patch id" -> diff.patchId,
          "semantic diff candidate id" -> diff.candidateModelId,
          "semantic diff category" -> diff.category,
          "semantic diff action" -> diff.action,
          "semantic diff subject" -> diff.subject,
          "semantic diff relationship" -> diff.relationship
        )
      ) ++
      _optional_evidence_violations("semantic diff before", diff.before, diff.condition) ++
      _optional_evidence_violations("semantic diff after", diff.after, diff.condition) ++
      _attribution_violations("semantic diff", diff.attribution) ++
      _condition_violations("semantic diff", diff.condition) ++
      _limitations_violations("semantic diff", diff.limitations)
  }

  private def _review_violations(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    review: CandidateDesignReviewSnapshot
  ): Vector[String] = {
    _identity_violations("review", review.id, review.context, review.component, context, component) ++
      _required_violations(
        Vector(
          "review patch id" -> review.patchId,
          "review candidate id" -> review.candidateModelId,
          "review state" -> review.state
        )
      ) ++
      _attribution_violations("review", review.attribution) ++
      _condition_violations("review", review.condition) ++
      _limitations_violations("review", review.limitations)
  }

  private def _governance_violations(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    governance: CandidateDesignGitGovernanceReference
  ): Vector[String] = {
    _identity_violations(
      "Git governance reference",
      governance.id,
      governance.context,
      governance.component,
      context,
      component
    ) ++
      _required_violations(
        Vector(
          "governance patch id" -> governance.patchId,
          "governance candidate id" -> governance.candidateModelId,
          "governance review id" -> governance.reviewSnapshotId,
          "Git governance reference" -> governance.reference,
          "Git governance state" -> governance.state
        )
      ) ++
      _attribution_violations("Git governance reference", governance.attribution) ++
      _condition_violations("Git governance reference", governance.condition) ++
      _limitations_violations("Git governance reference", governance.limitations)
  }

  private def _identity_violations(
    label: String,
    id: String,
    actualcontext: MonoKotoProjectionContextIdentity,
    actualcomponent: ComponentDashboardComponentIdentity,
    expectedcontext: MonoKotoProjectionContextIdentity,
    expectedcomponent: ComponentDashboardComponentIdentity
  ): Vector[String] = {
    val emptyid = if (_nonblank(id)) Vector.empty else Vector(s"$label id must be nonblank")
    val contextscope =
      if (actualcontext == expectedcontext) Vector.empty else Vector(s"$label context must match projection")
    val componentscope =
      if (actualcomponent == expectedcomponent) Vector.empty else Vector(s"$label component must match projection")
    emptyid ++ contextscope ++ componentscope
  }

  private def _attribution_violations(
    label: String,
    attribution: ComponentDashboardSourceAttribution
  ): Vector[String] =
    _required_violations(
      Vector(
        s"$label attribution source id" -> attribution.sourceId,
        s"$label attribution authority scope" -> attribution.authorityScope,
        s"$label attribution locator" -> attribution.sourceLocator
      )
    )

  private def _condition_violations(
    label: String,
    condition: ComponentDashboardCondition
  ): Vector[String] = {
    _required_violations(
      Vector(
        s"$label condition availability" -> condition.availability,
        s"$label condition authorization" -> condition.authorization
      )
    ) ++
      _optional_nonblank_violations(s"$label condition redaction", condition.redaction) ++
      _optional_nonblank_violations(s"$label condition explicit absence", condition.explicitAbsence) ++
      _optional_nonblank_violations(s"$label condition ambiguity", condition.ambiguity) ++
      _optional_nonblank_violations(s"$label condition conflict", condition.conflict) ++
      _optional_nonblank_violations(s"$label condition staleness", condition.staleness) ++
      _optional_nonblank_violations(s"$label condition malformed evidence", condition.malformedEvidence) ++
      _limitations_violations(s"$label condition", condition.limitations)
  }

  private def _optional_evidence_violations(
    label: String,
    evidence: Option[String],
    condition: ComponentDashboardCondition
  ): Vector[String] = evidence match {
    case Some(value) if _nonblank(value) => Vector.empty
    case Some(_) => Vector(s"$label must be nonblank when supplied")
    case None if condition.explicitAbsence.exists(_nonblank) => Vector.empty
    case None => Vector(s"$label may be absent only with an explicit absence condition")
  }

  private def _optional_nonblank_violations(label: String, value: Option[String]): Vector[String] =
    value match {
      case Some(actual) if !_nonblank(actual) => Vector(s"$label must be nonblank when supplied")
      case _ => Vector.empty
    }

  private def _limitations_violations(label: String, limitations: Vector[String]): Vector[String] =
    limitations.filterNot(_nonblank).map(_ => s"$label limitations must be nonblank")

  private def _required_violations(values: Vector[(String, String)]): Vector[String] =
    values.filterNot { case (_, value) => _nonblank(value) }.map { case (label, _) => s"$label must be nonblank" }

  private def _association_violation(actual: String, expected: String, label: String): Vector[String] =
    if (actual == expected && _nonblank(actual)) Vector.empty else Vector(s"$label association is invalid")

  private def _duplicate_ids(ids: Vector[String]): Vector[String] =
    ids.groupBy(identity).collect { case (id, occurrences) if occurrences.size > 1 => id }.toVector.sorted

  private def _order_links(
    links: Vector[AnalysisDesignImpactProjectedLink]
  ): Vector[AnalysisDesignImpactProjectedLink] =
    links.sortBy(link => (link.sourceLink.id, link.sourceLink.stableTieKey.getOrElse("")))

  private def _order_records(
    records: Vector[AnalysisDesignImpactProjectedRecord]
  ): Vector[AnalysisDesignImpactProjectedRecord] =
    records.groupBy(_.sourceRecord.id).values.map(_.head).toVector
      .sortBy(record => (record.sourceRecord.id, record.sourceRecord.stableTieKey.getOrElse("")))

  private def _order_diffs(
    diffs: Vector[CandidateDesignSemanticDiffEntry]
  ): Vector[CandidateDesignSemanticDiffEntry] =
    diffs.sortBy(diff => (diff.id, diff.stableTieKey.getOrElse("")))

  private def _nonblank(value: String): Boolean =
    Option(value).exists(_.trim.nonEmpty)
}
