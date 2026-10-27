package org.simplemodeling.textus.cbdsupport.runtime

import org.simplemodeling.textus.cbdsupport.value.ScopedQualityReviewRequest

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** An exact reviewable product subject; no review result, approval or write authority. */
private[runtime] object ModelEditConfirmation {
  final case class Prepared(session: ModelEditSession, confirmed: ModelEditCmlProjection.Confirmed,
    mapping: ModelEditCmlProjection.Mapping, diff: InternalModelSemanticDiffAdmission,
    subject: InternalModelReviewSubject, reviewArtifact: InternalModelArtifactReference,
    reviewReference: InternalModelRecordReference, reviewRequest: ScopedQualityReviewRequest)

  def prepare(session: ModelEditSession, confirmed: ModelEditCmlProjection.Confirmed,
    mapping: ModelEditCmlProjection.Mapping, diff: InternalModelSemanticDiffAdmission,
    subjectReference: InternalModelRecordReference, reviewArtifact: InternalModelArtifactReference,
    reviewReference: InternalModelRecordReference, paths: Vector[String],
    evidenceArtifacts: Vector[InternalModelArtifactReference] = Vector.empty): Either[String, Prepared] = for {
    _ <- ModelEditCmlProjection.validateMapping(mapping, session, diff.candidateAdmission)
    _ <- Either.cond(mapping.confirmed == confirmed && diff.diff.candidateReference == session.candidate.reference &&
      diff.diff.candidateArtifactReference == mapping.candidateArtifact && reviewArtifact.role == InternalModelArtifactRole.Validation &&
      paths.nonEmpty && paths.distinct.size == paths.size, (), "confirmation requires the exact candidate/source/diff and explicit review selection")
    artifacts <- InternalModelCandidateReviewBindingValidator.completeSubject(diff.candidateAdmission.packageContext, evidenceArtifacts, reviewArtifact)
    scope = diff.diff.scope
    subject = InternalModelReviewSubject(subjectReference.recordId, subjectReference.recordRevision,
      diff.candidateAdmission.packageContext.reference, scope, artifacts)
    request = ScopedQualityReviewRequest(reviewReference.recordId.value, reviewReference.recordRevision.value,
      subject.subjectId.value, subject.subjectRevision.value.toString, paths,
      Vector(scope.componentIdentity, scope.selectedUseCaseElementIdentity, session.candidate.reference.recordId.value),
      Vector("semantic-consistency", "implementation-quality", "excessive-checks", "integration"),
      Vector("Review the exact complete typed candidate/dependency/source-mapping/semantic-diff subject and retained actual producer/check observations.",
        "Assess all source target compatibility/migration effects and unresolved product findings; unknown assessments cannot imply safe canonical application.",
        "Keep product review independent of actual-human approval. No candidate, provider status or broad Phase authorization supplies a human decision."), Vector.empty)
  } yield Prepared(session, confirmed, mapping, diff, subject, reviewArtifact, reviewReference, request)
}
