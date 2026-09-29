package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import java.util.Base64

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * Admits attributable candidate-review evidence without treating a review state
 * as approval, reopening a captured package, or authenticating a provider.
 */
private[runtime] object InternalModelCandidateReviewBindingValidator {
  def validate(
    projectRoot: Path,
    reviewArtifactId: String,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis
  ): Consequence[InternalModelCandidateReviewBindingAdmission] =
    try {
      InternalModelPackageValidator.verifiedCandidateReviewBinding(projectRoot, reviewArtifactId).flatMap(
        validateVerified(_, expectedExecutionBasis)
      )
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model candidate review binding validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  /** Pure same-capture admission; callers may retain its values after paths disappear. */
  private[runtime] def validateVerified(
    handoff: InternalModelVerifiedCandidateReviewBindingPackage,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis
  ): Consequence[InternalModelCandidateReviewBindingAdmission] =
    try {
      InternalModelSemanticDiffValidator.validateVerified(handoff.semanticDiffPackage).flatMap { semanticdiff =>
        _admission(handoff, semanticdiff, expectedExecutionBasis).fold(Consequence.operationInvalid, Consequence.success)
      }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model candidate review binding validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _admission(
    handoff: InternalModelVerifiedCandidateReviewBindingPackage,
    semanticdiff: InternalModelSemanticDiffAdmission,
    expectedbasis: InternalModelCandidateReviewExecutionBasis
  ): Either[String, InternalModelCandidateReviewBindingAdmission] =
    for {
      binding <- InternalModelCandidateReviewBindingCodec.decode(handoff.reviewArtifact.bytes)
      _ <- InternalModelCandidateReviewBindingCodec.validateExecutionBasis(expectedbasis)
      _ <- Either.cond(binding.rules == expectedbasis.rules && binding.providers == expectedbasis.providers, (), "candidate review rules/providers do not equal the independently admitted execution basis")
      reviewedbytes <- _reviewed_manifest_bytes(binding.reviewedPackageManifest)
      reviewed <- InternalModelPackageValidator.reviewedPackageContext(reviewedbytes, handoff, handoff.reviewArtifact.artifactId)
      _ <- _binding(binding, handoff, reviewed, semanticdiff)
    } yield InternalModelCandidateReviewBindingAdmission(
      binding = binding,
      reviewArtifactId = handoff.reviewArtifact.artifactId,
      reviewArtifactSha256 = _sha256(handoff.reviewArtifact.bytes),
      reviewArtifactPackageRelativePath = handoff.reviewArtifact.packageRelativePath,
      reviewedPackageContext = reviewed,
      carrierPackageContext = handoff.carrierPackageContext,
      semanticDiffAdmission = semanticdiff
    )

  private def _binding(
    binding: InternalModelCandidateReviewBinding,
    handoff: InternalModelVerifiedCandidateReviewBindingPackage,
    reviewed: InternalModelVerifiedPackageContext,
    semanticdiff: InternalModelSemanticDiffAdmission
  ): Either[String, Unit] = {
    val candidate = semanticdiff.candidateAdmission
    val continuity = handoff.semanticDiffPackage.candidatePackage.continuityPackage.projection
    val realization = handoff.semanticDiffPackage.candidatePackage.continuityPackage.realizationPackage.realization
    val diffartifact = handoff.semanticDiffPackage.semanticDiff
    for {
      _ <- _artifact(binding.candidateArtifact, candidate.candidateArtifactId, candidate.candidateArtifactSha256, "projection", reviewed, "candidate")
      _ <- _artifact(binding.continuityArtifact, continuity.artifactId, _captured_sha256(handoff.carrierPackageContext, continuity.artifactId), "projection", reviewed, "continuity")
      _ <- _artifact(binding.realizationArtifact, realization.artifactId, _captured_sha256(handoff.carrierPackageContext, realization.artifactId), "realization", reviewed, "realization")
      _ <- _artifact(binding.semanticDiffArtifact, semanticdiff.diffArtifactId, semanticdiff.diffArtifactSha256, "projection", reviewed, "semantic diff")
      _ <- Either.cond(binding.realizationIdentity == candidate.continuity.realization.realizationIdentity, (), "candidate review realizationIdentity does not equal the selected realization")
      _ <- Either.cond(binding.candidateIdentity == candidate.projection.candidateIdentity, (), "candidate review candidateIdentity does not equal the selected candidate")
      _ <- Either.cond(binding.candidateModelIdentity == candidate.projection.candidateModelIdentity, (), "candidate review candidateModelIdentity does not equal the selected candidate")
      _ <- Either.cond(binding.candidateRevision == candidate.projection.candidateRevision, (), "candidate review candidateRevision does not equal the selected candidate")
      _ <- Either.cond(binding.scope == candidate.projection.scope && binding.scope == semanticdiff.diff.scope, (), "candidate review scope does not equal the selected candidate and semantic diff")
      _ <- Either.cond(binding.semanticDiffIdentity == semanticdiff.diff.semanticDiffIdentity, (), "candidate review semanticDiffIdentity does not equal the selected semantic diff")
      _ <- Either.cond(binding.semanticDiffRevision == semanticdiff.diff.semanticDiffRevision, (), "candidate review semanticDiffRevision does not equal the selected semantic diff")
      _ <- Either.cond(diffartifact.artifactId == semanticdiff.diffArtifactId, (), "candidate review selected semantic diff artifact changed within its capture")
      _ <- _evidence(binding.evidenceArtifacts, reviewed)
      _ <- _targets(binding.targets, binding.evidenceArtifacts, candidate, semanticdiff)
    } yield ()
  }

  private def _artifact(
    binding: InternalModelCandidateReviewArtifact,
    expectedid: String,
    expectedsha: String,
    expectedrole: String,
    reviewed: InternalModelVerifiedPackageContext,
    label: String
  ): Either[String, Unit] =
    for {
      _ <- Either.cond(binding.artifactId == expectedid && binding.sha256 == expectedsha, (), s"candidate review $label artifact does not equal the selected admitted artifact")
      artifact <- reviewed.artifacts.filter(_.artifactId == binding.artifactId) match {
        case Vector(value) => Right(value)
        case Vector() => Left(s"candidate review $label artifact is absent from the reviewed package inventory")
        case _ => Left(s"candidate review $label artifact is ambiguous in the reviewed package inventory")
      }
      _ <- Either.cond(artifact.present && artifact.role == expectedrole && artifact.sha256 == binding.sha256, (), s"candidate review $label artifact does not match the exact reviewed package inventory")
    } yield ()

  private def _evidence(
    evidence: Vector[InternalModelCandidateReviewArtifact],
    reviewed: InternalModelVerifiedPackageContext
  ): Either[String, Unit] =
    evidence.foldLeft[Either[String, Unit]](Right(())) { (result, value) =>
      for {
        _ <- result
        artifact <- reviewed.artifacts.filter(_.artifactId == value.artifactId) match {
          case Vector(entry) => Right(entry)
          case Vector() => Left(s"candidate review evidence artifact ${value.artifactId} is absent from the reviewed package inventory")
          case _ => Left(s"candidate review evidence artifact ${value.artifactId} is ambiguous in the reviewed package inventory")
        }
        _ <- Either.cond(artifact.present && artifact.role == "validation" && artifact.sha256 == value.sha256, (), s"candidate review evidence artifact ${value.artifactId} is not an exact present validation-role reviewed artifact")
      } yield ()
    }

  private def _targets(
    targets: Vector[InternalModelCandidateReviewTarget],
    evidence: Vector[InternalModelCandidateReviewArtifact],
    candidate: InternalModelCandidateCmlAdmission,
    semanticdiff: InternalModelSemanticDiffAdmission
  ): Either[String, Unit] = {
    val expectedids = candidate.projection.targets.map(_.targetId)
    if targets.map(_.targetId).toSet != expectedids.toSet || targets.size != expectedids.size then
      Left("candidate review targets do not equal the complete selected candidate target set")
    else {
      val evidenceids = evidence.map(_.artifactId).toSet
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

  private def _captured_sha256(
    carrier: InternalModelVerifiedPackageContext,
    artifactid: String
  ): String =
    carrier.artifacts.find(_.artifactId == artifactid).map(_.sha256).getOrElse("")

  private def _reviewed_manifest_bytes(content: InternalModelCandidateCmlContent): Either[String, Vector[Byte]] =
    try Right(Base64.getDecoder.decode(content.rawBytesBase64).toVector)
    catch {
      case NonFatal(_) => Left("candidate review reviewedPackageManifest cannot be decoded after codec validation")
    }

  private def _sha256(bytes: Vector[Byte]): String =
    "sha256:" + java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(byte => f"${byte & 0xff}%02x").mkString
}
