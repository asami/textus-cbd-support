package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence

/**
 * Structural audit admission, independent of current artifact presence or approval.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object InternalModelRetainedHistoryValidator {
  def validate(record: InternalModelRetainedHistoryRecord): Consequence[InternalModelRetainedHistoryRecord] = {
    if (record == null) {
      _invalid("record")
    } else {
      Consequence.zipN(Vector(
        _record_reference(record.reference), _subject(record.subject), _actor(record.actor),
        _check(record.occurredAt != null, "occurred-at"),
        _payload(record.payload, record.subject), _evidence(record.evidence)
      )).map(_ => record)
    }
  }

  def validateTombstone(
    tombstone: InternalModelRetainedHistoryTombstone
  ): Consequence[InternalModelRetainedHistoryTombstone] = {
    if (tombstone == null) {
      _invalid("tombstone")
    } else {
      Consequence.zipN(Vector(
        _record_reference(tombstone.reference), _package_reference(tombstone.packageReference),
        _scope(tombstone.scope), _check(tombstone.kind != null, "kind"),
        _check(tombstone.action != null, "retention-action"), _check(tombstone.effectiveAt != null, "effective-at")
      )).map(_ => tombstone)
    }
  }

  /** Verifies only the exact supplied relation; no endpoint lookup or head selection. */
  def validateSupersession(
    record: InternalModelRetainedHistoryRecord,
    previous: InternalModelRetainedHistoryRecord,
    successor: InternalModelRetainedHistoryRecord
  ): Consequence[InternalModelRetainedHistoryRecord] = {
    Consequence.zip3(validate(record), validate(previous), validate(successor)).flatMap { _ =>
      val relation = record.payload match {
        case InternalModelRetainedHistoryPayload.Supersession(before, after) =>
          before == previous.reference && after == successor.reference
        case _ => false
      }
      _checks(Vector(
        relation -> "supersession-endpoints",
        (Vector(record.reference, previous.reference, successor.reference).distinct.size == 3) -> "supersession-self",
        (previous.payload.kind != InternalModelRetainedHistoryKind.Supersession &&
          previous.payload.kind == successor.payload.kind) -> "supersession-kind",
        (record.subject.packageReference == previous.subject.packageReference &&
          record.subject.packageReference == successor.subject.packageReference) -> "supersession-package",
        (record.subject.scope == previous.subject.scope && record.subject.scope == successor.subject.scope) -> "supersession-scope"
      )).map(_ => record)
    }
  }

  private def _subject(subject: InternalModelReviewSubject): Consequence[Unit] = {
    if (subject == null) {
      _invalid("subject")
    } else {
      Consequence.zipN(Vector(
        _check(InternalModelRecordId.from(subject.subjectId.value).isRight, "subject-id"),
        _check(InternalModelRecordRevision.from(subject.subjectRevision.value).isRight, "subject-revision"),
        _package_reference(subject.packageReference), _scope(subject.scope), _artifacts(subject.artifacts)
      )).map(_ => ())
    }
  }

  private def _package_reference(reference: InternalModelPackageReference): Consequence[Unit] = {
    if (reference == null) {
      _invalid("package")
    } else {
      _checks(Vector(
        InternalModelPackageId.from(reference.packageId.value).isRight -> "package-id",
        InternalModelProjectToken.from(reference.projectNamespace.value).isRight -> "project-namespace",
        InternalModelProjectToken.from(reference.projectId.value).isRight -> "project-id"
      ))
    }
  }

  private def _scope(scope: InternalModelSemanticScope): Consequence[Unit] = {
    if (scope == null) {
      _invalid("scope")
    } else {
      _checks(Vector(
        _nonblank(scope.componentIdentity) -> "scope-component",
        _nonblank(scope.projectionContextIdentity) -> "scope-projection",
        _nonblank(scope.selectedUseCaseElementIdentity) -> "scope-usecase"
      ))
    }
  }

  private def _actor(actor: InternalModelDecisionActor): Consequence[Unit] = {
    if (actor == null) {
      _invalid("actor")
    } else {
      _checks(Vector(_nonblank(actor.kind) -> "actor-kind", _nonblank(actor.identity) -> "actor-identity",
        _nonblank(actor.role) -> "actor-role"))
    }
  }

  private def _artifacts(artifacts: Vector[InternalModelArtifactReference]): Consequence[Unit] = {
    if (artifacts == null) {
      _invalid("artifacts")
    } else {
      _check(artifacts.size <= 512, "artifact-count").flatMap { _ =>
        Consequence.zipN(artifacts.map(_artifact_reference)).flatMap { _ =>
          _check(artifacts.map(_.artifactId).distinct.size == artifacts.size, "artifact-ids")
        }
      }
    }
  }

  private def _artifact_reference(reference: InternalModelArtifactReference): Consequence[Unit] = {
    if (reference == null) {
      _invalid("artifact")
    } else {
      _checks(Vector(
        InternalModelArtifactId.from(reference.artifactId.value).isRight -> "artifact-id",
        InternalModelArtifactRevision.from(reference.artifactRevision.value).isRight -> "artifact-revision",
        (reference.role != null) -> "artifact-role"
      ))
    }
  }

  private def _record_reference(reference: InternalModelRecordReference): Consequence[Unit] = {
    if (reference == null) {
      _invalid("reference")
    } else {
      _checks(Vector(InternalModelRecordId.from(reference.recordId.value).isRight -> "record-id",
        InternalModelRecordRevision.from(reference.recordRevision.value).isRight -> "record-revision"))
    }
  }

  private def _evidence(evidence: Vector[InternalModelRetainedEvidence]): Consequence[Unit] = {
    if (evidence == null) {
      _invalid("evidence")
    } else {
      _check(evidence.size <= 64, "evidence-count").flatMap { _ =>
        Consequence.zipN(evidence.map(value => InternalModelEvidencePolicy.validate(value).map(_ => ()))).flatMap { _ =>
          _check(evidence.map(_.reference).distinct.size == evidence.size, "evidence-references")
        }
      }
    }
  }

  private def _payload(
    payload: InternalModelRetainedHistoryPayload,
    subject: InternalModelReviewSubject
  ): Consequence[Unit] = {
    if (payload == null) {
      _invalid("payload")
    } else {
      import InternalModelRetainedHistoryPayload.*
      payload match {
        case Review(review, candidate, semanticdiff, outcome) =>
          Consequence.zipN(Vector(_record_reference(review), _record_reference(candidate),
            _record_reference(semanticdiff), _check(outcome != null, "review-outcome"),
            _check(Vector(review, candidate, semanticdiff).distinct.size == 3, "review-references"))).map(_ => ())
        case Proposal(candidate, projection) =>
          val selected = subject != null && subject.artifacts != null && subject.artifacts.contains(projection)
          Consequence.zipN(Vector(_record_reference(candidate), _artifact_reference(projection),
            _check(projection != null && projection.role == InternalModelArtifactRole.Projection, "projection-role"),
            _check(selected, "projection-selection"))).map(_ => ())
        case Alternative(proposal, alternative, disposition) =>
          Consequence.zipN(Vector(_record_reference(proposal), _record_reference(alternative),
            _check(disposition != null, "alternative-disposition"),
            _check(proposal != alternative, "alternative-references"))).map(_ => ())
        case Supersession(previous, successor) =>
          Consequence.zip3(_record_reference(previous), _record_reference(successor),
            _check(previous != successor, "supersession-references")).map(_ => ())
        case Evidence(reference) => _record_reference(reference)
      }
    }
  }

  private def _nonblank(value: String): Boolean = value != null && !value.isBlank

  private def _checks(checks: Vector[(Boolean, String)]): Consequence[Unit] = {
    Consequence.zipN(checks.map { case (valid, dimension) => _check(valid, dimension) }).map(_ => ())
  }

  private def _check(valid: Boolean, dimension: String): Consequence[Unit] = {
    if (valid) Consequence.unit else _invalid(dimension)
  }

  private def _invalid[A](dimension: String): Consequence[A] = {
    Consequence.operationInvalid(s"internal-model retained history invalid: $dimension")
  }
}
