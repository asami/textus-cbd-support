package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
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
      InternalModelCandidateCmlProjectionValidator.validateVerified(handoff.candidatePackage).flatMap { candidate =>
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
      diff <- InternalModelSemanticDiffCodec.decode(handoff.semanticDiff)
      artifact <- handoff.candidatePackage.packageContext.artifacts.find(value => value.artifactId == handoff.semanticDiff.artifactId && value.present).toRight("selected semantic diff artifact is absent from captured package context")
      _ <- _binding(diff, handoff, candidate, artifact)
      _ <- _targets(diff, candidate)
    } yield InternalModelSemanticDiffAdmission(diff, artifact.artifactId, artifact.sha256, artifact.packageRelativePath, candidate)

  private def _binding(
    diff: InternalModelSemanticDiff,
    handoff: InternalModelVerifiedSemanticDiffPackage,
    candidate: InternalModelCandidateCmlAdmission,
    artifact: InternalModelVerifiedArtifactContext
  ): Either[String, Unit] =
    for {
      _ <- Either.cond(diff.candidateArtifactId == candidate.candidateArtifactId, (), "semantic diff candidateArtifactId is not the selected candidate artifact")
      _ <- Either.cond(diff.candidateArtifactSha256 == candidate.candidateArtifactSha256, (), "semantic diff candidateArtifactSha256 is not the selected candidate artifact hash")
      _ <- Either.cond(diff.candidateIdentity == candidate.projection.candidateIdentity, (), "semantic diff candidateIdentity does not equal selected candidate")
      _ <- Either.cond(diff.candidateModelIdentity == candidate.projection.candidateModelIdentity, (), "semantic diff candidateModelIdentity does not equal selected candidate")
      _ <- Either.cond(diff.candidateRevision == candidate.projection.candidateRevision, (), "semantic diff candidateRevision does not equal selected candidate")
      _ <- Either.cond(diff.scope == candidate.projection.scope, (), "semantic diff scope does not equal selected candidate")
      _ <- Either.cond(handoff.semanticDiff.dependencies.toSet == Set(candidate.candidateArtifactId), (), "semantic diff artifact dependencies do not equal the selected candidate artifact")
      _ <- Either.cond(artifact.role == "projection", (), "semantic diff captured artifact role is not projection")
    } yield ()

  private def _targets(diff: InternalModelSemanticDiff, candidate: InternalModelCandidateCmlAdmission): Either[String, Unit] = {
    val candidateids = candidate.projection.targets.map(_.targetId)
    if diff.targets.map(_.targetId).toSet != candidateids.toSet || diff.targets.size != candidateids.size then
      Left("semantic diff targets do not equal the complete selected candidate target set")
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
      _ <- Either.cond(patch.baseDigest == candidate.source.sha256, (), s"semantic diff target ${target.targetId} baseDigest does not equal candidate raw baseline digest")
      _ <- Either.cond(patch.proposedDigest == candidate.proposedContent.sha256, (), s"semantic diff target ${target.targetId} proposedDigest does not equal candidate raw proposed digest")
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
