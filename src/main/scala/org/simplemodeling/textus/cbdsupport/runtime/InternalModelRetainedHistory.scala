package org.simplemodeling.textus.cbdsupport.runtime

import java.time.Instant

/**
 * Immutable audit vocabulary; recorded opinions and relationships grant no action.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] enum InternalModelRetainedHistoryKind {
  case Review, Proposal, Alternative, Supersession, Evidence
}

private[runtime] enum InternalModelRetainedReviewOutcome {
  case Recorded, ChangesRequested, Rejected, AcceptedAsReview
}

private[runtime] enum InternalModelRetainedAlternativeDisposition {
  case Considered, Rejected, SelectedAsProposal
}

private[runtime] enum InternalModelRetainedHistoryPayload {
  case Review(
    reviewReference: InternalModelRecordReference,
    candidateReference: InternalModelRecordReference,
    semanticDiffReference: InternalModelRecordReference,
    outcome: InternalModelRetainedReviewOutcome
  )
  case Proposal(
    candidateReference: InternalModelRecordReference,
    projectionArtifactReference: InternalModelArtifactReference
  )
  case Alternative(
    proposalReference: InternalModelRecordReference,
    alternativeReference: InternalModelRecordReference,
    disposition: InternalModelRetainedAlternativeDisposition
  )
  case Supersession(
    previousReference: InternalModelRecordReference,
    successorReference: InternalModelRecordReference
  )
  case Evidence(evidenceReference: InternalModelRecordReference)

  def kind: InternalModelRetainedHistoryKind = this match {
    case Review(_, _, _, _) => InternalModelRetainedHistoryKind.Review
    case Proposal(_, _) => InternalModelRetainedHistoryKind.Proposal
    case Alternative(_, _, _) => InternalModelRetainedHistoryKind.Alternative
    case Supersession(_, _) => InternalModelRetainedHistoryKind.Supersession
    case Evidence(_) => InternalModelRetainedHistoryKind.Evidence
  }
}

private[runtime] final case class InternalModelRetainedHistoryRecord(
  reference: InternalModelRecordReference,
  subject: InternalModelReviewSubject,
  actor: InternalModelDecisionActor,
  occurredAt: Instant,
  payload: InternalModelRetainedHistoryPayload,
  evidence: Vector[InternalModelRetainedEvidence]
)

private[runtime] enum InternalModelHistoryRetentionAction {
  case Expired, Deleted
}

/** Body-free retention account, separate from an audit payload and its evidence. */
private[runtime] final case class InternalModelRetainedHistoryTombstone(
  reference: InternalModelRecordReference,
  packageReference: InternalModelPackageReference,
  scope: InternalModelSemanticScope,
  kind: InternalModelRetainedHistoryKind,
  action: InternalModelHistoryRetentionAction,
  effectiveAt: Instant
)
