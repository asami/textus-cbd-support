package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import java.security.MessageDigest

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits a recorded human decision without inferring lifecycle applicability or write authority. */
private[runtime] object InternalModelCandidateHumanApprovalValidator {
  def validate(
    projectRoot: Path,
    approvalArtifactId: String,
    reviewArtifactId: String,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis,
    expectedHumanDecision: InternalModelCandidateHumanApprovalInput
  ): Consequence[InternalModelCandidateHumanApprovalAdmission] =
    try {
      InternalModelPackageValidator.verifiedCandidateHumanApproval(projectRoot, approvalArtifactId, reviewArtifactId).flatMap(
        validateVerified(_, expectedExecutionBasis, expectedHumanDecision)
      )
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model candidate human approval validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  /** Pure same-capture admission; retained handoffs are independent of later filesystem changes. */
  private[runtime] def validateVerified(
    handoff: InternalModelVerifiedCandidateHumanApprovalPackage,
    expectedExecutionBasis: InternalModelCandidateReviewExecutionBasis,
    expectedHumanDecision: InternalModelCandidateHumanApprovalInput
  ): Consequence[InternalModelCandidateHumanApprovalAdmission] =
    try {
      InternalModelCandidateReviewBindingValidator.validateVerified(handoff.reviewPackage, expectedExecutionBasis).flatMap { reviewadmission =>
        _admission(handoff, reviewadmission, expectedHumanDecision).fold(Consequence.operationInvalid, Consequence.success)
      }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model candidate human approval validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _admission(
    handoff: InternalModelVerifiedCandidateHumanApprovalPackage,
    reviewadmission: InternalModelCandidateReviewBindingAdmission,
    expectedhuman: InternalModelCandidateHumanApprovalInput
  ): Either[String, InternalModelCandidateHumanApprovalAdmission] =
    for {
      record <- InternalModelCandidateHumanApprovalCodec.decode(handoff.approvalArtifact.bytes)
      expected <- _validated_expected(expectedhuman)
      _ <- Either.cond(record.approval == expected, (), "candidate human approval does not equal the independently admitted expected human input")
      _ <- _binding(record.approval.basis, reviewadmission)
    } yield InternalModelCandidateHumanApprovalAdmission(
      record = record,
      approvalArtifactId = handoff.approvalArtifact.artifactId,
      approvalArtifactSha256 = _sha256(handoff.approvalArtifact.bytes),
      approvalArtifactPackageRelativePath = handoff.approvalArtifact.packageRelativePath,
      reviewAdmission = reviewadmission
    )

  private def _validated_expected(value: InternalModelCandidateHumanApprovalInput): Either[String, InternalModelCandidateHumanApprovalInput] =
    InternalModelCandidateHumanApprovalCodec.decode(
      InternalModelCandidateHumanApprovalCodec.encode(
        InternalModelCandidateHumanApproval(value, "ccdm-candidate-human-approval-v1", "1.0", Vector.empty)
      ).toVector
    ).map(_.approval)

  private def _binding(
    basis: InternalModelCandidateHumanApprovalBasis,
    reviewadmission: InternalModelCandidateReviewBindingAdmission
  ): Either[String, Unit] = {
    val binding = reviewadmission.binding
    val semanticdiff = reviewadmission.semanticDiffAdmission
    val candidate = semanticdiff.candidateAdmission
    val reviewed = reviewadmission.reviewedPackageContext
    for {
      _ <- Either.cond(basis.reviewArtifact == InternalModelCandidateReviewArtifact(reviewadmission.reviewArtifactId, reviewadmission.reviewArtifactSha256), (), "candidate human approval review artifact does not equal the selected admitted review artifact")
      _ <- Either.cond(basis.reviewIdentity == binding.reviewIdentity && basis.reviewRevision == binding.reviewRevision, (), "candidate human approval review identity or revision does not equal the admitted review")
      _ <- Either.cond(basis.candidateArtifact == InternalModelCandidateReviewArtifact(candidate.candidateArtifactId, candidate.candidateArtifactSha256), (), "candidate human approval candidate artifact does not equal the admitted candidate artifact")
      _ <- Either.cond(basis.candidateIdentity == candidate.projection.candidateIdentity && basis.candidateModelIdentity == candidate.projection.candidateModelIdentity && basis.candidateRevision == candidate.projection.candidateRevision, (), "candidate human approval candidate identity, model identity, or revision does not equal the admitted candidate")
      _ <- Either.cond(basis.semanticDiffArtifact == InternalModelCandidateReviewArtifact(semanticdiff.diffArtifactId, semanticdiff.diffArtifactSha256), (), "candidate human approval semantic diff artifact does not equal the admitted semantic diff artifact")
      _ <- Either.cond(basis.semanticDiffIdentity == semanticdiff.diff.semanticDiffIdentity && basis.semanticDiffRevision == semanticdiff.diff.semanticDiffRevision, (), "candidate human approval semantic diff identity or revision does not equal the admitted semantic diff")
      _ <- Either.cond(basis.scope == binding.scope && basis.scope == candidate.projection.scope && basis.scope == semanticdiff.diff.scope, (), "candidate human approval scope does not equal the complete admitted review basis")
      _ <- Either.cond(basis.reviewedPackage == InternalModelCandidateHumanApprovalPackageBasis(reviewed.packageDigest, reviewed.packageId, reviewed.projectId, reviewed.projectNamespace, reviewed.revision, reviewed.schemaVersion), (), "candidate human approval reviewed package does not equal the complete historical reviewed package")
    } yield ()
  }

  private def _sha256(bytes: Vector[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(byte => f"${byte & 0xff}%02x").mkString
}
