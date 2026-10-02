package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import scala.util.control.NonFatal
import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 *  version Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits an actual independent human input without inferring applicability or write authority. */
private[runtime] object InternalModelCandidateHumanApprovalValidator {
  def validate(
    projectRoot: Path,
    approvalArtifact: InternalModelArtifactReference,
    reviewArtifact: InternalModelArtifactReference,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis,
    expectedHumanDecision: InternalModelCandidateHumanApprovalInput
  ): Consequence[InternalModelCandidateHumanApprovalAdmission] = {
    try InternalModelPackageValidator.verifiedCandidateHumanApproval(projectRoot, approvalArtifact, reviewArtifact).flatMap(
      validateVerified(_, expectedExecutionBasis, expectedHumanDecision)
    )
    catch { case NonFatal(error) => _failure(error) }
  }

  /** Pure admission from one actual immutable capture; no path is reopened. */
  private[runtime] def validateVerified(
    handoff: InternalModelVerifiedCandidateHumanApprovalPackage,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis,
    expectedHumanDecision: InternalModelCandidateHumanApprovalInput
  ): Consequence[InternalModelCandidateHumanApprovalAdmission] = {
    try {
      if (handoff == null || handoff.reviewPackage == null || handoff.approvalArtifact == null)
        Consequence.operationInvalid("candidate human approval capture, review and selected approval must be present")
      else InternalModelCandidateReviewBindingValidator.validateVerified(handoff.reviewPackage, expectedExecutionBasis).flatMap { reviewadmission =>
        _admission(handoff, reviewadmission, expectedHumanDecision).fold(Consequence.operationInvalid, Consequence.success)
      }
    } catch { case NonFatal(error) => _failure(error) }
  }

  private def _admission(
    handoff: InternalModelVerifiedCandidateHumanApprovalPackage,
    reviewadmission: InternalModelCandidateReviewBindingAdmission,
    expectedhuman: InternalModelCandidateHumanApprovalInput
  ): Either[String, InternalModelCandidateHumanApprovalAdmission] = {
    val artifact = handoff.approvalArtifact
    for {
      _ <- Either.cond(artifact.reference != null && artifact.path != null && artifact.dependencies != null && artifact.dependencies.forall(_ != null) && artifact.bytes != null, (), "selected approval metadata and bytes must be present")
      record <- InternalModelCandidateHumanApprovalCodec.decode(artifact.bytes)
      _ <- InternalModelCandidateHumanApprovalCodec.validateValue(expectedhuman)
      _ <- Either.cond(record.approval == expectedhuman, (), "candidate human approval does not equal the independently supplied actual human input")
      admission = InternalModelCandidateHumanApprovalAdmission(record, artifact.reference, artifact.path, reviewadmission)
      _ <- validateAdmission(admission)
      selected <- _selected(admission)
      _ <- Either.cond(selected.required == artifact.required && selected.dependencies == artifact.dependencies, (), "selected approval capture does not equal entire inventory metadata")
    } yield admission
  }

  /** Structural and cross-binding consistency only; callers retain original human admission ownership. */
  private[runtime] def validateAdmission(admission: InternalModelCandidateHumanApprovalAdmission): Either[String, Unit] = {
    try {
      for {
        _ <- Either.cond(admission != null && admission.record != null && admission.approvalArtifactReference != null && admission.approvalArtifactPackageRelativePath != null && admission.reviewAdmission != null, (), "candidate human approval admission is missing")
        _ <- InternalModelCandidateHumanApprovalCodec.validateValue(admission.record)
        _ <- InternalModelCandidateReviewBindingValidator.validateAdmission(admission.reviewAdmission)
        _ <- _selected(admission)
        _ <- _binding(admission.record.approval.basis, admission.reviewAdmission)
      } yield ()
    } catch { case NonFatal(_) => Left("candidate human approval admission contains a null or invalid object graph") }
  }

  private def _selected(admission: InternalModelCandidateHumanApprovalAdmission): Either[String, InternalModelVerifiedArtifactContext] = {
    for {
      _ <- Either.cond(admission.approvalArtifactReference.role == InternalModelArtifactRole.Approval, (), "selected approval must have Approval role")
      selected <- admission.reviewAdmission.carrierPackageContext.artifacts.filter(_.reference.artifactId == admission.approvalArtifactReference.artifactId) match {
        case Vector(value) => Right(value)
        case _ => Left("selected approval must resolve to exactly one inventory entry")
      }
      _ <- Either.cond(selected.reference == admission.approvalArtifactReference && selected.present && selected.path == admission.approvalArtifactPackageRelativePath && selected.dependencies == Vector(admission.reviewAdmission.reviewArtifactReference), (), "selected approval reference, presence, path or exact review dependency is inconsistent")
    } yield selected
  }

  private def _binding(basis: InternalModelCandidateHumanApprovalBasis, reviewadmission: InternalModelCandidateReviewBindingAdmission): Either[String, Unit] = {
    val binding = reviewadmission.binding
    val diff = reviewadmission.semanticDiffAdmission
    val candidate = diff.candidateAdmission
    for {
      _ <- Either.cond(basis.reviewArtifactReference == reviewadmission.reviewArtifactReference && basis.reviewReference == binding.reviewReference, (), "candidate human approval review artifact or logical reference does not equal the selected admitted review")
      _ <- Either.cond(basis.candidateArtifactReference == candidate.candidateArtifactReference && basis.candidateReference == candidate.projection.candidateReference && basis.candidateModelIdentity == candidate.projection.candidateModelIdentity, (), "candidate human approval candidate artifact, logical reference or model does not equal the admitted candidate")
      _ <- Either.cond(basis.semanticDiffArtifactReference == diff.diffArtifactReference && basis.semanticDiffReference == diff.diff.semanticDiffReference, (), "candidate human approval semantic diff artifact or logical reference does not equal the admitted diff")
      _ <- Either.cond(basis.scope == binding.scope && basis.scope == candidate.projection.scope && basis.scope == diff.diff.scope, (), "candidate human approval scope does not equal the complete admitted review basis")
      _ <- Either.cond(basis.subject == binding.subject, (), "candidate human approval subject does not equal the complete admitted review subject")
    } yield ()
  }

  private def _failure(error: Throwable): Consequence[InternalModelCandidateHumanApprovalAdmission] =
    Consequence.operationInvalid("internal-model candidate human approval validation failed: " + Option(error.getMessage).getOrElse(error.getClass.getSimpleName))
}
