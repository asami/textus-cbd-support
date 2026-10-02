package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Arrays
import scala.util.control.NonFatal
import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits one complete typed subject from one capture; review state is evidence. */
private[runtime] object InternalModelCandidateReviewBindingValidator {
  def validate(
    projectRoot: Path,
    reviewArtifact: InternalModelArtifactReference,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis
  ): Consequence[InternalModelCandidateReviewBindingAdmission] = {
    try {
      InternalModelCandidateReviewBindingCodec.validateExecutionBasis(expectedExecutionBasis) match {
        case Left(message) => Consequence.operationInvalid(message)
        case Right(_) => InternalModelPackageValidator.verifiedCandidateReviewBinding(projectRoot, reviewArtifact).flatMap(
          validateVerified(_, expectedExecutionBasis)
        )
      }
    } catch {
      case NonFatal(error) => _failure(error)
    }
  }

  /** Pure same-capture admission; no source, provider, package or path is reopened. */
  private[runtime] def validateVerified(
    handoff: InternalModelVerifiedCandidateReviewBindingPackage,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis
  ): Consequence[InternalModelCandidateReviewBindingAdmission] = {
    try {
      val preconditions = for {
        _ <- InternalModelCandidateReviewBindingCodec.validateExecutionBasis(expectedExecutionBasis)
        _ <- _capture(handoff)
      } yield ()
      preconditions match {
        case Left(message) => Consequence.operationInvalid(message)
        case Right(_) => InternalModelSemanticDiffValidator.validateVerified(handoff.semanticDiffPackage).flatMap { semanticdiff =>
          _admission(handoff, semanticdiff, expectedExecutionBasis).fold(Consequence.operationInvalid, Consequence.success)
        }
      }
    } catch {
      case NonFatal(error) => _failure(error)
    }
  }

  private def _capture(handoff: InternalModelVerifiedCandidateReviewBindingPackage): Either[String, Unit] = {
    for {
      _ <- Either.cond(handoff != null && handoff.carrierPackageContext != null && handoff.semanticDiffPackage != null && handoff.reviewArtifact != null, (), "review capture, carrier, diff and selected review must be present")
      diff = handoff.semanticDiffPackage
      _ <- Either.cond(diff.candidatepackage != null && diff.semanticdiff != null, (), "review candidate handoff and selected diff must be present")
      candidate = diff.candidatepackage
      _ <- Either.cond(candidate.packagecontext != null && candidate.continuitypackage != null && candidate.candidate != null, (), "review candidate capture and continuity must be present")
      continuity = candidate.continuitypackage
      _ <- Either.cond(continuity.projection != null && continuity.realizationpackage != null && continuity.realizationpackage.realization != null && continuity.realizationpackage.sourcesnapshots != null, (), "review continuity, realization and snapshots must be present")
      carrier = handoff.carrierPackageContext
      _ <- Either.cond(carrier.reference != null && carrier.artifacts != null && carrier.artifacts.forall(value => value != null && _reference_present(value.reference) && value.path != null && _dependencies_present(value.dependencies)), (), "review carrier metadata must be present")
      _ <- Either.cond(Vector(handoff.reviewArtifact, diff.semanticdiff, candidate.candidate, continuity.projection).forall(value => _reference_present(value.reference) && value.path != null && _dependencies_present(value.dependencies) && value.bytes != null), (), "review selected projection metadata must be present")
      realization = continuity.realizationpackage.realization
      _ <- Either.cond(_reference_present(realization.reference) && realization.path != null && _dependencies_present(realization.dependencies) && realization.bytes != null, (), "review realization metadata must be present")
      _ <- Either.cond(continuity.realizationpackage.sourcesnapshots.forall(value => value != null && _reference_present(value.reference) && value.path != null && _dependencies_present(value.dependencies) && value.bytes != null), (), "review source metadata must be present")
    } yield ()
  }

  private def _reference_present(reference: InternalModelArtifactReference): Boolean =
    reference != null && InternalModelArtifactId.from(reference.artifactId.value).isRight && InternalModelArtifactRevision.from(reference.artifactRevision.value).isRight && reference.role != null

  private def _dependencies_present(dependencies: Vector[InternalModelArtifactReference]): Boolean =
    dependencies != null && dependencies.forall(_reference_present)

  private def _admission(handoff: InternalModelVerifiedCandidateReviewBindingPackage, semanticdiff: InternalModelSemanticDiffAdmission, expectedbasis: InternalModelCandidateReviewExecutionBasis): Either[String, InternalModelCandidateReviewBindingAdmission] =
    for {
      binding <- InternalModelCandidateReviewBindingCodec.decode(handoff.reviewArtifact.bytes)
      _ <- Either.cond(binding.rules == expectedbasis.rules && binding.providers == expectedbasis.providers, (), "review rules/providers do not equal the independently admitted execution basis")
      _ <- _binding(binding, handoff, semanticdiff)
    } yield InternalModelCandidateReviewBindingAdmission(binding, handoff.reviewArtifact.reference, handoff.reviewArtifact.path, handoff.carrierPackageContext, semanticdiff)

  /**
   * Checks structural and cross-binding consistency of already admitted semantic inputs.
   * Upstream validators own their semantic admission and original captured authenticity;
   * this helper neither reconstructs a capture nor detects undeclared payload mutations.
   */
  private[runtime] def validateAdmission(admission: InternalModelCandidateReviewBindingAdmission): Either[String, Unit] = {
    try {
      for {
        _ <- Either.cond(_present_graph(admission), (), "review admission and every nested model, reference and vector must be present")
        _ <- InternalModelCandidateReviewBindingCodec.validateValue(admission.binding)
        carrier = admission.carrierPackageContext
        _ <- InternalModelTypedControlCodec.decodePackageReference(InternalModelTypedControlCodec.encodePackageReference(carrier.reference)).map(_ => ())
        _ <- Either.cond(carrier.artifacts.forall(entry => _reference_present(entry.reference) && _dependencies_present(entry.dependencies)), (), "review carrier contains an invalid typed reference")
        _ <- InternalModelPackageValidator.validateCapturedContext(carrier)
        _ <- _binding_value(admission.binding, carrier, admission.reviewArtifactReference, admission.reviewArtifactPackageRelativePath, admission.semanticDiffAdmission)
      } yield ()
    } catch { case NonFatal(_) => Left("review admission contains a null or invalid object graph") }
  }

  private def _present_graph(value: Any): Boolean = value match {
    case null => false
    case reference: InternalModelArtifactReference => _reference_present(reference)
    case reference: InternalModelRecordReference => InternalModelRecordId.from(reference.recordId.value).isRight && InternalModelRecordRevision.from(reference.recordRevision.value).isRight
    case reference: InternalModelPackageReference => InternalModelPackageId.from(reference.packageId.value).isRight && InternalModelProjectToken.from(reference.projectNamespace.value).isRight && InternalModelProjectToken.from(reference.projectId.value).isRight
    case values: Iterable[?] => values.forall(_present_graph)
    case value: Product => value.productIterator.forall(_present_graph)
    case _ => true
  }

  private def _binding(binding: InternalModelCandidateReviewBinding, handoff: InternalModelVerifiedCandidateReviewBindingPackage, semanticdiff: InternalModelSemanticDiffAdmission): Either[String, Unit] = {
    val candidate = semanticdiff.candidateAdmission
    val carrier = handoff.carrierPackageContext
    val continuity = handoff.semanticDiffPackage.candidatepackage.continuitypackage
    val realization = continuity.realizationpackage.realization
    for {
      _ <- Either.cond(carrier == candidate.packageContext, (), "review and candidate must use the same captured carrier")
      review <- _selected(carrier, handoff.reviewArtifact.reference, handoff.reviewArtifact.path, handoff.reviewArtifact.required, handoff.reviewArtifact.dependencies)
      _ <- Either.cond(review.reference.role == InternalModelArtifactRole.Validation, (), "selected review must have Validation role")
      _ <- _selected(carrier, handoff.semanticDiffPackage.semanticdiff.reference, handoff.semanticDiffPackage.semanticdiff.path, handoff.semanticDiffPackage.semanticdiff.required, handoff.semanticDiffPackage.semanticdiff.dependencies)
      _ <- _selected(carrier, handoff.semanticDiffPackage.candidatepackage.candidate.reference, handoff.semanticDiffPackage.candidatepackage.candidate.path, handoff.semanticDiffPackage.candidatepackage.candidate.required, handoff.semanticDiffPackage.candidatepackage.candidate.dependencies)
      _ <- _selected(carrier, continuity.projection.reference, continuity.projection.path, continuity.projection.required, continuity.projection.dependencies)
      _ <- _selected(carrier, realization.reference, realization.path, realization.required, realization.dependencies)
      _ <- continuity.realizationpackage.sourcesnapshots.foldLeft[Either[String, Unit]](Right(())) { (result, source) =>
        for {
          _ <- result
          entry <- _inventory(carrier, source.reference, source.bytes.isDefined)
          _ <- Either.cond(entry.path == source.path && entry.required == source.required && entry.dependencies == source.dependencies && entry.present == source.bytes.isDefined, (), "review source capture does not equal inventory metadata")
        } yield ()
      }
      _ <- validateAdmission(InternalModelCandidateReviewBindingAdmission(binding, review.reference, review.path, carrier, semanticdiff))
    } yield ()
  }

  private def _binding_value(
    binding: InternalModelCandidateReviewBinding,
    carrier: InternalModelVerifiedPackageContext,
    reviewreference: InternalModelArtifactReference,
    reviewpath: String,
    semanticdiff: InternalModelSemanticDiffAdmission
  ): Either[String, Unit] = {
    val candidate = semanticdiff.candidateAdmission
    val continuity = candidate.continuity
    val realization = continuity.realization
    for {
      _ <- Either.cond(carrier == candidate.packageContext, (), "review and candidate must use the same captured carrier")
      _ <- Either.cond(candidate.projection.profile == "ccdm-candidate-cml-projection-v2" && candidate.projection.schemaVersion == "2.0" && semanticdiff.diff.profile == "ccdm-semantic-diff-v2" && semanticdiff.diff.schemaVersion == "2.0" && realization.profile == "ccdm-realization-v3" && realization.schemaVersion == "3.0" && continuity.binding.profile == "ccdm-projection-binding-v3" && continuity.binding.schemaVersion == "3.0", (), "review semantic inputs must use current profiles and schemas")
      review <- _inventory(carrier, reviewreference, true)
      _ <- Either.cond(review.reference.role == InternalModelArtifactRole.Validation && review.path == reviewpath, (), "selected review reference role or path is inconsistent")
      candidateentry <- _inventory(carrier, candidate.candidateArtifactReference, true)
      _ <- Either.cond(candidateentry.reference.role == InternalModelArtifactRole.Projection && candidateentry.path == candidate.candidatePackageRelativePath, (), "review candidate reference role or path is inconsistent")
      diffentry <- _inventory(carrier, semanticdiff.diffArtifactReference, true)
      _ <- Either.cond(diffentry.reference.role == InternalModelArtifactRole.Projection && diffentry.path == semanticdiff.diffPackageRelativePath && diffentry.dependencies == Vector(candidate.candidateArtifactReference), (), "review diff selection or dependency is inconsistent")
      continuityentry <- _inventory(carrier, binding.continuityArtifactReference, true)
      realizationentry <- _inventory(carrier, binding.realizationArtifactReference, true)
      _ <- Either.cond(binding.continuityArtifactReference == candidate.projection.continuityArtifactReference && binding.realizationArtifactReference == candidate.projection.realizationArtifactReference && binding.realizationArtifactReference == continuity.binding.realizationArtifactReference && continuityentry.dependencies == Vector(binding.realizationArtifactReference), (), "review continuity and realization topology is inconsistent")
      _ <- Either.cond(realizationentry.reference.role == InternalModelArtifactRole.Realization && binding.realizationReference == realization.realizationReference, (), "review realization reference is inconsistent")
      _ <- Either.cond(realizationentry.dependencies == realization.consumedSnapshotReferences, (), "review realization dependencies do not equal its consumed snapshots")
      _ <- realization.consumedSnapshotReferences.foldLeft[Either[String, Unit]](Right(())) { (result, reference) =>
        result.flatMap(_ => Either.cond(reference.role == InternalModelArtifactRole.SourceSnapshot, (), "review realization consumes a nonsource reference")).flatMap(_ => _inventory(carrier, reference, true).map(_ => ()))
      }
      _ <- Either.cond(realization.sourceReferences.forall(reference => realization.consumedSnapshotReferences.contains(reference.snapshotReference)), (), "review realization source witness reference is not consumed")
      _ <- Either.cond(semanticdiff.diff.candidateArtifactReference == candidate.candidateArtifactReference && semanticdiff.diff.candidateReference == candidate.projection.candidateReference && semanticdiff.diff.candidateModelIdentity == candidate.projection.candidateModelIdentity && semanticdiff.diff.scope == candidate.projection.scope, (), "review admitted candidate/diff binding is inconsistent")
      _ <- Either.cond(candidateentry.dependencies == (Vector(binding.continuityArtifactReference, binding.realizationArtifactReference) ++ candidate.projection.targets.map(_.baselineArtifactReference)).distinct.sortBy(_.artifactId.value), (), "review candidate dependencies do not equal its complete typed basis")
      _ <- candidate.targetBytes.foldLeft[Either[String, Unit]](Right(())) { (result, target) =>
        for {
          _ <- result
          _ <- _inventory(carrier, target.baseline.reference, true)
          declared <- candidate.projection.targets.find(_.targetId == target.targetId).toRight("review retained candidate target is unknown")
          _ <- Either.cond(target.baseline.reference == declared.baselineArtifactReference && target.baseline.source == declared.source && target.baseline.projectRelativePath == declared.projectRelativePath, (), "review retained target source or baseline binding is inconsistent")
        } yield ()
      }
      _ <- Either.cond(candidate.targetBytes.map(_.targetId) == candidate.projection.targets.map(_.targetId), (), "review retained candidate target order is incomplete")
      _ <- Either.cond(binding.candidateArtifactReference == candidate.candidateArtifactReference && binding.candidateReference == candidate.projection.candidateReference && binding.candidateModelIdentity == candidate.projection.candidateModelIdentity, (), "review candidate artifact, logical reference or model does not equal admitted candidate")
      _ <- Either.cond(binding.realizationReference == realization.realizationReference, (), "review realization logical reference does not equal admitted selection")
      _ <- Either.cond(binding.semanticDiffArtifactReference == semanticdiff.diffArtifactReference && binding.semanticDiffReference == semanticdiff.diff.semanticDiffReference, (), "review semantic diff references do not equal admitted diff")
      _ <- Either.cond(binding.subject.packageReference == carrier.reference, (), "review subject packageReference does not equal stable carrier identity")
      _ <- Either.cond(binding.scope == candidate.projection.scope && binding.scope == semanticdiff.diff.scope && binding.scope == candidate.continuity.realization.scope && binding.subject.scope == binding.scope, (), "review subject/root scope does not equal admitted semantic scope")
      continuityscope = candidate.continuity.binding.scope
      _ <- Either.cond(continuityscope.componentIdentity == binding.scope.componentIdentity && continuityscope.projectionContextIdentity == binding.scope.projectionContextIdentity && continuityscope.selectedUseCaseElementIdentity == binding.scope.selectedUseCaseElementIdentity, (), "review scope does not equal admitted continuity scope")
      _ <- binding.evidenceArtifacts.foldLeft[Either[String, Unit]](Right(())) { (result, reference) =>
        for {
          _ <- result
          _ <- Either.cond(reference.role == InternalModelArtifactRole.Validation && reference.artifactId != review.reference.artifactId, (), "review evidence cannot select the review itself or a nonvalidation artifact")
          _ <- _inventory(carrier, reference, true)
        } yield ()
      }
      subject <- _complete_subject(carrier, binding.evidenceArtifacts, review.reference)
      _ <- Either.cond(binding.subject.artifacts == subject, (), "review subject does not equal the complete exact semantic dependency basis")
      _ <- Either.cond(Vector(binding.candidateArtifactReference, binding.continuityArtifactReference, binding.realizationArtifactReference, binding.semanticDiffArtifactReference).forall(subject.contains), (), "review root artifacts must be present in its subject")
      _ <- Either.cond(review.dependencies == binding.subject.artifacts, (), "selected review dependencies must equal the exact ordered subject artifacts")
      _ <- _targets(binding.targets, binding.evidenceArtifacts, candidate, semanticdiff)
    } yield ()
  }

  private def _selected(carrier: InternalModelVerifiedPackageContext, reference: InternalModelArtifactReference, path: String, required: Boolean, dependencies: Vector[InternalModelArtifactReference]): Either[String, InternalModelVerifiedArtifactContext] =
    for {
      artifact <- _inventory(carrier, reference, true)
      _ <- Either.cond(artifact.path == path && artifact.required == required && artifact.dependencies == dependencies, (), "selected review capture does not equal entire inventory metadata")
    } yield artifact

  private def _inventory(carrier: InternalModelVerifiedPackageContext, reference: InternalModelArtifactReference, present: Boolean): Either[String, InternalModelVerifiedArtifactContext] =
    for {
      entry <- carrier.artifacts.filter(_.reference.artifactId == reference.artifactId) match {
        case Vector(value) => Right(value)
        case _ => Left("consumed review reference must resolve to exactly one inventory entry")
      }
      _ <- Either.cond(entry.reference == reference && (!present || entry.present), (), "consumed review reference has absent or mismatched revision/role")
    } yield entry

  private def _complete_subject(carrier: InternalModelVerifiedPackageContext, evidence: Vector[InternalModelArtifactReference], review: InternalModelArtifactReference): Either[String, Vector[InternalModelArtifactReference]] = {
    val roles = Set(InternalModelArtifactRole.SourceSnapshot, InternalModelArtifactRole.Realization, InternalModelArtifactRole.Projection, InternalModelArtifactRole.Decision, InternalModelArtifactRole.OpenIssue)
    val roots = carrier.artifacts.filter(value => value.present && roles.contains(value.reference.role)).map(_.reference) ++ evidence
    def _visit_(reference: InternalModelArtifactReference, active: Set[InternalModelArtifactId], completed: Set[InternalModelArtifactReference]): Either[String, Set[InternalModelArtifactReference]] = {
      for {
        _ <- Either.cond(reference.artifactId != review.artifactId && reference.role != InternalModelArtifactRole.Resume && reference.role != InternalModelArtifactRole.Approval, (), "review semantic dependency cannot consume selected review, Resume or Approval")
        _ <- Either.cond(!active.contains(reference.artifactId), (), "review semantic dependency cycle")
        entry <- _inventory(carrier, reference, true)
        result <- if (completed.contains(reference)) Right(completed) else {
          entry.dependencies.foldLeft[Either[String, Set[InternalModelArtifactReference]]](Right(completed)) { (result, dependency) =>
            result.flatMap(values => _visit_(dependency, active + reference.artifactId, values))
          }.map(_ + reference)
        }
      } yield result
    }
    roots.foldLeft[Either[String, Set[InternalModelArtifactReference]]](Right(Set.empty)) { (result, reference) =>
      result.flatMap(values => _visit_(reference, Set.empty, values))
    }.map(_.toVector.sortWith((left, right) => Arrays.compareUnsigned(left.artifactId.value.getBytes(StandardCharsets.UTF_8), right.artifactId.value.getBytes(StandardCharsets.UTF_8)) < 0))
  }

  private def _targets(
    targets: Vector[InternalModelCandidateReviewTarget],
    evidence: Vector[InternalModelArtifactReference],
    candidate: InternalModelCandidateCmlAdmission,
    semanticdiff: InternalModelSemanticDiffAdmission
  ): Either[String, Unit] = {
    val expectedids = candidate.projection.targets.map(_.targetId)
    if targets.map(_.targetId) != expectedids || targets.map(_.targetId) != semanticdiff.diff.targets.map(_.targetId) then
      Left("candidate review targets do not equal the complete selected candidate target set")
    else {
      val evidenceids = evidence.map(_.artifactId.value).toSet
      for {
        _ <- Either.cond(targets.flatMap(_.evidenceArtifactIds).toSet == evidenceids, (), "candidate review target evidence IDs do not cover the admitted evidence set")
        _ <- targets.foldLeft[Either[String, Unit]](Right(())) { (result, target) =>
          for {
            _ <- result
            candidatetarget <- candidate.projection.targets.find(_.targetId == target.targetId).toRight(s"candidate review target ${target.targetId} is not selected by the candidate")
            difftarget <- semanticdiff.diff.targets.find(_.targetId == target.targetId).toRight(s"candidate review target ${target.targetId} is not selected by the semantic diff")
            snapshot = target.reviewSnapshot
            _ <- Either.cond(snapshot.patchId == candidatetarget.patchIdentity && snapshot.patchId == difftarget.patchTrace.id, (), s"candidate review target ${target.targetId} snapshot patchId does not equal the exact candidate target")
            _ <- Either.cond(snapshot.candidateModelId == candidate.projection.candidateModelIdentity, (), s"candidate review target ${target.targetId} snapshot candidateModelId does not equal the selected candidate model")
            _ <- Either.cond(snapshot.component.value == candidate.projection.scope.componentIdentity && snapshot.context.value == candidate.projection.scope.projectionContextIdentity, (), s"candidate review target ${target.targetId} snapshot component/context does not equal the exact candidate target scope")
            _ <- Either.cond(target.evidenceArtifactIds.forall(evidenceids.contains), (), s"candidate review target ${target.targetId} names evidence outside the admitted evidence set")
          } yield ()
        }
      } yield ()
    }
  }

  private def _failure(error: Throwable): Consequence[InternalModelCandidateReviewBindingAdmission] =
    Consequence.operationInvalid(s"internal-model candidate review binding validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
}
