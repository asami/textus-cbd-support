package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.util.Arrays
import scala.util.control.NonFatal
import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 *  version Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Pure applicability over retained semantic admissions and independently supplied evidence. */
private[runtime] object InternalModelCandidateApprovalLifecycleEvaluator {
  def evaluate(
    approval: InternalModelCandidateHumanApprovalAdmission,
    currentReview: InternalModelCandidateReviewBindingAdmission,
    expectedCurrentExecutionBasis: InternalModelCandidateReviewExecutionBasis,
    sourceSnapshots: Vector[InternalModelVerifiedSourceSnapshot],
    liveSources: Map[String, InternalModelLiveSourceObservation],
    supersession: Option[InternalModelCandidateApprovalSupersessionInput]
  ): Consequence[InternalModelCandidateApprovalLifecycleReport] = {
    try _evaluate(approval, currentReview, expectedCurrentExecutionBasis, sourceSnapshots, liveSources, supersession).fold(Consequence.operationInvalid, Consequence.success)
    catch { case NonFatal(error) => Consequence.operationInvalid("candidate approval lifecycle evaluation failed: " + Option(error.getMessage).getOrElse(error.getClass.getSimpleName)) }
  }

  private def _evaluate(
    approval: InternalModelCandidateHumanApprovalAdmission,
    currentreview: InternalModelCandidateReviewBindingAdmission,
    expectedbasis: InternalModelCandidateReviewExecutionBasis,
    snapshots: Vector[InternalModelVerifiedSourceSnapshot],
    livesources: Map[String, InternalModelLiveSourceObservation],
    supersession: Option[InternalModelCandidateApprovalSupersessionInput]
  ): Either[String, InternalModelCandidateApprovalLifecycleReport] = {
    for {
      _ <- InternalModelCandidateHumanApprovalValidator.validateAdmission(approval)
      _ <- InternalModelCandidateReviewBindingValidator.validateAdmission(currentreview)
      _ <- InternalModelCandidateReviewBindingCodec.validateExecutionBasis(expectedbasis)
      _ <- _source_snapshot_inventory(approval, snapshots)
      _ <- _live_source_container(snapshots, livesources)
      link <- _supersession(approval, supersession)
      reasons = _basis_invalidations(approval, currentreview, expectedbasis) ++ _source_invalidations(snapshots, livesources)
      state = _state(approval.record.approval.decision, link, reasons)
    } yield InternalModelCandidateApprovalLifecycleReport(state, approval, reasons, link)
  }

  private def _source_snapshot_inventory(approval: InternalModelCandidateHumanApprovalAdmission, snapshots: Vector[InternalModelVerifiedSourceSnapshot]): Either[String, Unit] = {
    for {
      _ <- Either.cond(snapshots != null && snapshots.forall(_ != null), (), "original source snapshot inventory must be present")
      expected = approval.reviewAdmission.carrierPackageContext.artifacts.filter(_.reference.role == InternalModelArtifactRole.SourceSnapshot)
      _ <- Either.cond(snapshots.forall(value => value.reference != null && value.path != null && value.dependencies != null && value.dependencies.forall(_ != null) && value.bytes != null && value.bytes.forall(_ != null)), (), "source snapshot metadata or payload option is null")
      ids = snapshots.map(_.reference.artifactId)
      _ <- Either.cond(ids.distinct.size == ids.size && ids.toSet == expected.map(_.reference.artifactId).toSet && snapshots.size == expected.size, (), "source snapshots must exactly cover the original carrier source inventory without duplicates")
      _ <- snapshots.foldLeft[Either[String, Unit]](Right(())) { (result, snapshot) =>
        for {
          _ <- result
          entry <- expected.find(_.reference.artifactId == snapshot.reference.artifactId).toRight("source snapshot reference is unknown")
          _ <- Either.cond(snapshot.reference == entry.reference && snapshot.path == entry.path && snapshot.required == entry.required && snapshot.dependencies == entry.dependencies && snapshot.bytes.isDefined == entry.present, (), "source snapshot reference, path, required, dependencies or presence does not equal original inventory")
          _ <- snapshot.bytes match {
            case Some(bytes) => InternalModelSourceSnapshotFreshness.validatedSnapshotKind(bytes.toArray).map(_ => ())
            case None => Either.cond(!entry.required && !entry.present, (), "absent source snapshot must be an optional absent inventory entry")
          }
        } yield ()
      }
    } yield ()
  }

  private def _live_source_container(snapshots: Vector[InternalModelVerifiedSourceSnapshot], livesources: Map[String, InternalModelLiveSourceObservation]): Either[String, Unit] = {
    for {
      _ <- Either.cond(livesources != null, (), "live source observation map is missing")
      _ <- Either.cond(livesources.keys.forall(_ != null) && livesources.values.forall(_ != null), (), "live source observation keys and values must be present")
      _ <- Either.cond(livesources.keySet.subsetOf(snapshots.map(_.reference.artifactId.value).toSet), (), "live source map contains an unknown source inventory locator")
    } yield ()
  }

  private def _supersession(approval: InternalModelCandidateHumanApprovalAdmission, value: Option[InternalModelCandidateApprovalSupersessionInput]): Either[String, Option[InternalModelCandidateApprovalSupersessionInput]] = {
    if (value == null) Left("candidate approval supersession option is missing")
    else value match {
      case None => Right(None)
      case Some(input) if input == null || input.record == null || input.successorApproval == null => Left("candidate approval supersession input is missing")
      case Some(input) =>
        for {
          _ <- InternalModelCandidateApprovalSupersessionCodec.validateValue(input.record)
          _ <- InternalModelCandidateHumanApprovalValidator.validateAdmission(input.successorApproval)
          _ <- _supersession_binding(approval, input)
        } yield Some(input)
    }
  }

  private def _supersession_binding(predecessor: InternalModelCandidateHumanApprovalAdmission, input: InternalModelCandidateApprovalSupersessionInput): Either[String, Unit] = {
    val link = input.record
    val prior = predecessor.record.approval
    val successor = input.successorApproval.record.approval
    for {
      _ <- Either.cond(link.predecessorApproval == predecessor.approvalArtifactReference, (), "supersession predecessor reference does not equal the original approval")
      _ <- Either.cond(link.successorApproval == input.successorApproval.approvalArtifactReference, (), "supersession successor reference does not equal the separately admitted approval")
      _ <- Either.cond(prior.basis.subject.packageReference == successor.basis.subject.packageReference, (), "supersession crosses package or project identity")
      _ <- Either.cond(prior.basis.scope == successor.basis.scope, (), "supersession crosses scope")
      _ <- Either.cond(prior.basis.candidateReference.recordId == successor.basis.candidateReference.recordId && prior.basis.candidateModelIdentity == successor.basis.candidateModelIdentity, (), "supersession crosses candidate logical identity or model")
      _ <- Either.cond(successor.basis.candidateReference.recordRevision.value >= prior.basis.candidateReference.recordRevision.value, (), "supersession rolls candidate logical revision back")
      _ <- Either.cond(successor.basis.candidateReference.recordRevision != prior.basis.candidateReference.recordRevision || successor.basis.candidateArtifactReference == prior.basis.candidateArtifactReference, (), "supersession changes candidate artifact at the same logical revision")
      _ <- Either.cond(prior.approvalReference.recordId != successor.approvalReference.recordId || successor.approvalReference.recordRevision.value > prior.approvalReference.recordRevision.value, (), "supersession does not advance the same approval logical identity")
    } yield ()
  }

  private def _basis_invalidations(approval: InternalModelCandidateHumanApprovalAdmission, current: InternalModelCandidateReviewBindingAdmission, expected: InternalModelCandidateReviewExecutionBasis): Vector[InternalModelCandidateApprovalInvalidation] = {
    val original = approval.record.approval.basis
    val review = current.binding
    val candidate = current.semanticDiffAdmission.candidateAdmission
    val diff = current.semanticDiffAdmission
    val subject = review.subject
    val candidatechanges = _artifact_changes("candidateArtifactReference", original.candidateArtifactReference, candidate.candidateArtifactReference) ++
      _record_changes("candidateReference", original.candidateReference, candidate.projection.candidateReference) ++
      Option.when(original.candidateModelIdentity != candidate.projection.candidateModelIdentity)("candidateModelIdentity").toVector
    val reviewchanges = _artifact_changes("reviewArtifactReference", original.reviewArtifactReference, current.reviewArtifactReference) ++
      _record_changes("reviewReference", original.reviewReference, review.reviewReference)
    val diffchanges = _artifact_changes("semanticDiffArtifactReference", original.semanticDiffArtifactReference, diff.diffArtifactReference) ++
      _record_changes("semanticDiffReference", original.semanticDiffReference, diff.diff.semanticDiffReference)
    val subjectchanges = Vector(
      Option.when(original.subject.subjectId != subject.subjectId)("subject.subjectId"),
      Option.when(original.subject.subjectRevision != subject.subjectRevision)("subject.subjectRevision"),
      Option.when(original.subject.packageReference.projectNamespace != subject.packageReference.projectNamespace)("subject.packageReference.projectNamespace"),
      Option.when(original.subject.packageReference.projectId != subject.packageReference.projectId)("subject.packageReference.projectId"),
      Option.when(original.subject.packageReference.packageId != subject.packageReference.packageId)("subject.packageReference.packageId")
    ).flatten ++ _scope_changes("subject.scope", original.subject.scope, subject.scope) ++
      Option.when(original.subject.artifacts != subject.artifacts)("subject.artifacts").toVector
    _invalidation(InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, candidatechanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, reviewchanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, diffchanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ReviewSubjectChanged, subjectchanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ScopeChanged, _scope_changes("scope", original.scope, review.scope)) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.RulesChanged, _basis_changes("rules", approval.reviewAdmission.binding.rules.map(value => (value.ruleId, value.ruleVersion)), review.rules.map(value => (value.ruleId, value.ruleVersion)), expected.rules.map(value => (value.ruleId, value.ruleVersion)))) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, _basis_changes("providers", approval.reviewAdmission.binding.providers.map(value => (value.providerId, value.providerVersion)), review.providers.map(value => (value.providerId, value.providerVersion)), expected.providers.map(value => (value.providerId, value.providerVersion))))
  }

  private def _artifact_changes(prefix: String, original: InternalModelArtifactReference, current: InternalModelArtifactReference): Vector[String] = Vector(
    Option.when(original.artifactId != current.artifactId)(prefix + ".artifactId"),
    Option.when(original.artifactRevision != current.artifactRevision)(prefix + ".artifactRevision"),
    Option.when(original.role != current.role)(prefix + ".role")
  ).flatten

  private def _record_changes(prefix: String, original: InternalModelRecordReference, current: InternalModelRecordReference): Vector[String] = Vector(
    Option.when(original.recordId != current.recordId)(prefix + ".recordId"),
    Option.when(original.recordRevision != current.recordRevision)(prefix + ".recordRevision")
  ).flatten

  private def _scope_changes(prefix: String, original: InternalModelSemanticScope, current: InternalModelSemanticScope): Vector[String] = Vector(
    Option.when(original.componentIdentity != current.componentIdentity)(prefix + ".componentIdentity"),
    Option.when(original.projectionContextIdentity != current.projectionContextIdentity)(prefix + ".projectionContextIdentity"),
    Option.when(original.selectedUseCaseElementIdentity != current.selectedUseCaseElementIdentity)(prefix + ".selectedUseCaseElementIdentity")
  ).flatten

  private def _basis_changes(prefix: String, original: Vector[(String, String)], current: Vector[(String, String)], expected: Vector[(String, String)]): Vector[String] =
    _one_basis_changes("originalReview." + prefix, original, expected) ++ _one_basis_changes("currentReview." + prefix, current, expected)

  private def _one_basis_changes(prefix: String, actual: Vector[(String, String)], expected: Vector[(String, String)]): Vector[String] = {
    val actualbyid = actual.toMap
    val expectedbyid = expected.toMap
    val membership = Option.when(actualbyid.keySet != expectedbyid.keySet)(prefix + ".ids").toVector
    membership ++ _sort_ids((actualbyid.keySet ++ expectedbyid.keySet).toVector).flatMap { id =>
      (actualbyid.get(id), expectedbyid.get(id)) match {
        case (Some(left), Some(right)) if left != right => Vector(prefix + "[" + id + "].version")
        case _ => Vector.empty
      }
    }
  }

  private def _source_invalidations(snapshots: Vector[InternalModelVerifiedSourceSnapshot], livesources: Map[String, InternalModelLiveSourceObservation]): Vector[InternalModelCandidateApprovalInvalidation] = {
    snapshots.flatMap { snapshot =>
      snapshot.bytes match {
        case None => Vector(InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.MissingBaseline, Some(snapshot.reference), Vector.empty, Vector.empty))
        case Some(bytes) =>
          val observation = livesources.getOrElse(snapshot.reference.artifactId.value, InternalModelLiveSourceObservation.Unavailable("no source-owner live observation was supplied"))
          val report = InternalModelSourceSnapshotFreshness.compare(bytes.toArray, observation)
          val kind = report.status match {
            case InternalModelSnapshotFreshnessStatus.Unchanged => None
            case InternalModelSnapshotFreshnessStatus.Changed => Some(InternalModelCandidateApprovalInvalidationKind.SourceChanged)
            case InternalModelSnapshotFreshnessStatus.Incomplete => Some(InternalModelCandidateApprovalInvalidationKind.SourceIncomplete)
            case InternalModelSnapshotFreshnessStatus.Unavailable => Some(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable)
            case InternalModelSnapshotFreshnessStatus.Unauthorized => Some(InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized)
            case InternalModelSnapshotFreshnessStatus.Malformed => Some(InternalModelCandidateApprovalInvalidationKind.SourceMalformed)
            case InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting => Some(InternalModelCandidateApprovalInvalidationKind.SourceAmbiguousOrConflicting)
          }
          kind.map(value => InternalModelCandidateApprovalInvalidation(value, Some(snapshot.reference), report.changedDimensionNames, report.missingDimensionNames)).toVector
      }
    }.sortWith { (left, right) =>
      val kindcomparison = left.kind.ordinal.compare(right.kind.ordinal)
      kindcomparison < 0 || (kindcomparison == 0 && _compare_ids(left.artifactReference.get.artifactId.value, right.artifactReference.get.artifactId.value) < 0)
    }
  }

  private def _state(decision: InternalModelCandidateHumanApprovalDecision, link: Option[InternalModelCandidateApprovalSupersessionInput], invalidations: Vector[InternalModelCandidateApprovalInvalidation]): InternalModelCandidateApprovalLifecycleState = {
    if (link.nonEmpty) InternalModelCandidateApprovalLifecycleState.Superseded
    else if (invalidations.nonEmpty) InternalModelCandidateApprovalLifecycleState.Invalidated
    else decision match {
      case InternalModelCandidateHumanApprovalDecision.Approved => InternalModelCandidateApprovalLifecycleState.Approved
      case InternalModelCandidateHumanApprovalDecision.Rejected => InternalModelCandidateApprovalLifecycleState.Rejected
      case InternalModelCandidateHumanApprovalDecision.ChangesRequested => InternalModelCandidateApprovalLifecycleState.ChangesRequested
    }
  }

  private def _invalidation(kind: InternalModelCandidateApprovalInvalidationKind, dimensions: Vector[String]): Vector[InternalModelCandidateApprovalInvalidation] =
    Option.when(dimensions.nonEmpty)(InternalModelCandidateApprovalInvalidation(kind, None, dimensions, Vector.empty)).toVector

  private def _sort_ids(values: Vector[String]): Vector[String] = values.sortWith((left, right) => _compare_ids(left, right) < 0)
  private def _compare_ids(left: String, right: String): Int = Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))
}
