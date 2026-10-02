package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits one captured semantic diff without reopening the package or source paths. */
private[runtime] object InternalModelSemanticDiffValidator {
  def validate(projectRoot: Path): Consequence[InternalModelSemanticDiffAdmission] =
    try {
      InternalModelPackageValidator.verifiedSemanticDiff(projectRoot).flatMap(validateVerified)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model semantic diff validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def validateVerified(handoff: InternalModelVerifiedSemanticDiffPackage): Consequence[InternalModelSemanticDiffAdmission] =
    try {
      if (handoff == null || handoff.candidatepackage == null || handoff.semanticdiff == null)
        Consequence.operationInvalid("semantic diff capture, candidate package and selected diff must be present")
      else InternalModelCandidateCmlProjectionValidator.validateVerified(handoff.candidatepackage).flatMap { candidate =>
          _admission(handoff, candidate).fold(Consequence.operationInvalid, Consequence.success)
        }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model semantic diff validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _admission(
    handoff: InternalModelVerifiedSemanticDiffPackage,
    candidate: InternalModelCandidateCmlAdmission
  ): Either[String, InternalModelSemanticDiffAdmission] =
    for {
      diff <- InternalModelSemanticDiffCodec.decode(handoff.semanticdiff)
      artifact <- _selected_artifact(handoff, candidate)
      _ <- _binding(diff, handoff, candidate)
      _ <- _targets(diff, candidate)
    } yield InternalModelSemanticDiffAdmission(diff, artifact.reference, artifact.path, candidate)

  private def _selected_artifact(
    handoff: InternalModelVerifiedSemanticDiffPackage,
    candidate: InternalModelCandidateCmlAdmission
  ): Either[String, InternalModelVerifiedArtifactContext] = {
    val selected = handoff.semanticdiff
    for {
      _ <- Either.cond(candidate.packageContext == handoff.candidatepackage.packagecontext, (), "candidate and diff must use the same captured package basis")
      artifact <- candidate.packageContext.artifacts.filter(_.reference.artifactId == selected.reference.artifactId) match {
        case Vector(value) => Right(value)
        case _ => Left("selected semantic diff artifact must resolve to exactly one captured inventory entry")
      }
      _ <- Either.cond(artifact.reference == selected.reference && artifact.path == selected.path && artifact.required == selected.required && artifact.dependencies == selected.dependencies && artifact.present, (), "selected semantic diff capture does not equal its entire inventory entry")
    } yield artifact
  }

  private def _binding(
    diff: InternalModelSemanticDiff,
    handoff: InternalModelVerifiedSemanticDiffPackage,
    candidate: InternalModelCandidateCmlAdmission
  ): Either[String, Unit] =
    for {
      _ <- Either.cond(diff.candidateArtifactReference == candidate.candidateArtifactReference, (), "semantic diff candidateArtifactReference is not the exact selected candidate artifact")
      _ <- Either.cond(diff.candidateReference == candidate.projection.candidateReference, (), "semantic diff candidateReference does not equal selected candidate")
      _ <- Either.cond(diff.candidateModelIdentity == candidate.projection.candidateModelIdentity, (), "semantic diff candidateModelIdentity does not equal selected candidate")
      _ <- Either.cond(diff.scope == candidate.projection.scope, (), "semantic diff scope does not equal selected candidate")
      _ <- Either.cond(handoff.semanticdiff.dependencies == Vector(candidate.candidateArtifactReference), (), "semantic diff artifact dependencies do not equal exactly the selected candidate artifact reference")
    } yield ()

  private def _targets(diff: InternalModelSemanticDiff, candidate: InternalModelCandidateCmlAdmission): Either[String, Unit] = {
    val candidateids = candidate.projection.targets.map(_.targetId)
    if diff.targets.map(_.targetId) != candidateids then
      Left("semantic diff targets do not equal the complete ordered selected candidate targets")
    else diff.targets.foldLeft[Either[String, Unit]](Right(())) { (result, target) =>
      for {
        _ <- result
        candidatetarget <- candidate.projection.targets.find(_.targetId == target.targetId).toRight(s"semantic diff target ${target.targetId} is not a selected candidate target")
        _ <- _patch(target, candidatetarget, diff.scope)
        _ <- _entries(target, candidatetarget, diff.scope, candidate.projection.candidateModelIdentity)
      } yield ()
    }
  }

  private def _patch(
    target: InternalModelSemanticDiffTarget,
    candidate: InternalModelCandidateCmlTarget,
    scope: InternalModelSemanticScope
  ): Either[String, Unit] = {
    val patch = target.patchTrace
    for {
      _ <- Either.cond(patch.id == candidate.patchIdentity, (), s"semantic diff target ${target.targetId} patch identity does not equal candidate target")
      _ <- Either.cond(patch.component.value == scope.componentIdentity && patch.context.value == scope.projectionContextIdentity, (), s"semantic diff target ${target.targetId} patch scope does not equal candidate scope")
      _ <- Either.cond(patch.baselineArtifactReference == candidate.baselineArtifactReference, (), s"semantic diff target ${target.targetId} baselineArtifactReference does not equal candidate baseline")
      _ <- Either.cond(patch.proposedContentReference == candidate.proposedContent.contentReference, (), s"semantic diff target ${target.targetId} proposedContentReference does not equal candidate proposed content")
      _ <- Either.cond(patch.cmlOwner == candidate.source.authority, (), s"semantic diff target ${target.targetId} cmlOwner does not equal candidate source authority")
      _ <- candidate.source.locator match {
        case Some(locator) => Either.cond(patch.cmlLocator == locator, (), s"semantic diff target ${target.targetId} cmlLocator does not equal candidate source locator")
        case None => Right(())
      }
    } yield ()
  }

  private def _entries(
    target: InternalModelSemanticDiffTarget,
    candidate: InternalModelCandidateCmlTarget,
    scope: InternalModelSemanticScope,
    candidatemodelidentity: String
  ): Either[String, Unit] =
    target.entries.foldLeft[Either[String, Unit]](Right(())) { (result, mapped) =>
      val entry = mapped.entry
      for {
        _ <- result
        mapping <- candidate.mappings.find(_.mappingId == mapped.mappingId).toRight(s"semantic diff entry ${entry.id} mapping is unknown or belongs to another target")
        _ <- Either.cond(mapped.semanticIdentityKind == mapping.semanticIdentityKind, (), s"semantic diff entry ${entry.id} mapping kind does not equal candidate mapping kind")
        _ <- Either.cond(entry.subject == mapping.semanticIdentity, (), s"semantic diff entry ${entry.id} subject does not equal candidate mapping identity")
        _ <- Either.cond(entry.context.value == scope.projectionContextIdentity && entry.component.value == scope.componentIdentity, (), s"semantic diff entry ${entry.id} scope does not equal candidate scope")
        _ <- Either.cond(entry.patchId == target.patchTrace.id, (), s"semantic diff entry ${entry.id} patchId does not equal target patch")
        _ <- Either.cond(entry.candidateModelId == candidatemodelidentity, (), s"semantic diff entry ${entry.id} candidateModelId does not equal selected candidate model")
      } yield ()
    }
}
