package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Arrays
import java.util.Base64

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Pure point-in-time applicability evaluation over explicit admitted evidence. */
private[runtime] object InternalModelCandidateApprovalLifecycleEvaluator {
  def evaluate(
    approval: InternalModelCandidateHumanApprovalAdmission,
    currentReview: InternalModelCandidateReviewBindingAdmission,
    expectedCurrentExecutionBasis: InternalModelCandidateReviewExecutionBasis,
    sourceSnapshots: Vector[InternalModelVerifiedSourceSnapshot],
    liveSources: Map[String, InternalModelLiveSourceObservation],
    supersession: Option[InternalModelCandidateApprovalSupersessionInput]
  ): Consequence[InternalModelCandidateApprovalLifecycleReport] =
    try {
      _evaluate(approval, currentReview, expectedCurrentExecutionBasis, sourceSnapshots, liveSources, supersession).fold(
        Consequence.operationInvalid,
        Consequence.success
      )
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"candidate approval lifecycle evaluation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _evaluate(
    approval: InternalModelCandidateHumanApprovalAdmission,
    currentreview: InternalModelCandidateReviewBindingAdmission,
    expectedbasis: InternalModelCandidateReviewExecutionBasis,
    snapshots: Vector[InternalModelVerifiedSourceSnapshot],
    livesources: Map[String, InternalModelLiveSourceObservation],
    supersession: Option[InternalModelCandidateApprovalSupersessionInput]
  ): Either[String, InternalModelCandidateApprovalLifecycleReport] =
    for {
      _ <- _approval_admission(approval, "original approval")
      _ <- _review_admission(currentreview, "current review")
      _ <- _expected_execution_basis(expectedbasis)
      _ <- _source_snapshot_inventory(approval, snapshots)
      _ <- _live_source_container(snapshots, livesources)
      link <- _supersession(approval, supersession)
      invalidations = _basis_invalidations(approval, currentreview, expectedbasis) ++ _source_invalidations(approval, snapshots, livesources)
      state = _state(approval.record.approval.decision, link, invalidations)
    } yield InternalModelCandidateApprovalLifecycleReport(state, approval, invalidations, link)

  private def _approval_admission(
    approval: InternalModelCandidateHumanApprovalAdmission,
    label: String
  ): Either[String, Unit] =
    if approval == null || approval.record == null || approval.reviewAdmission == null then Left(s"$label is missing")
    else
      for {
        decoded <- InternalModelCandidateHumanApprovalCodec.decode(approval.record.canonicalBytes)
        _ <- Either.cond(decoded == approval.record, (), s"$label record does not equal its canonical bytes")
        _ <- Either.cond(_sha256(approval.record.canonicalBytes) == approval.approvalArtifactSha256, (), s"$label artifact hash does not equal its canonical bytes")
        _ <- _nonblank(approval.approvalArtifactId, s"$label artifact ID")
        _ <- _review_admission(approval.reviewAdmission, s"$label retained review")
        _ <- _approval_review_binding(approval.record.approval.basis, approval.reviewAdmission, label)
      } yield ()

  private def _review_admission(
    review: InternalModelCandidateReviewBindingAdmission,
    label: String
  ): Either[String, Unit] =
    if review == null || review.binding == null || review.semanticDiffAdmission == null || review.reviewedPackageContext == null then Left(s"$label is missing")
    else
      for {
        decodedbinding <- InternalModelCandidateReviewBindingCodec.decode(review.binding.canonicalBytes)
        _ <- Either.cond(decodedbinding == review.binding, (), s"$label binding does not equal its canonical bytes")
        _ <- Either.cond(_sha256(review.binding.canonicalBytes) == review.reviewArtifactSha256, (), s"$label artifact hash does not equal its canonical bytes")
        _ <- _nonblank(review.reviewArtifactId, s"$label artifact ID")
        _ <- _reviewed_package_context(review, label)
        _ <- _semantic_diff_admission(review.semanticDiffAdmission, label)
        candidate = review.semanticDiffAdmission.candidateAdmission
        diff = review.semanticDiffAdmission
        _ <- Either.cond(review.binding.candidateArtifact == InternalModelCandidateReviewArtifact(candidate.candidateArtifactId, candidate.candidateArtifactSha256), (), s"$label candidate tuple does not equal its admitted candidate")
        _ <- Either.cond(review.binding.candidateIdentity == candidate.projection.candidateIdentity && review.binding.candidateModelIdentity == candidate.projection.candidateModelIdentity && review.binding.candidateRevision == candidate.projection.candidateRevision, (), s"$label candidate identity does not equal its admitted candidate")
        _ <- Either.cond(review.binding.semanticDiffArtifact == InternalModelCandidateReviewArtifact(diff.diffArtifactId, diff.diffArtifactSha256), (), s"$label semantic diff tuple does not equal its admitted semantic diff")
        _ <- Either.cond(review.binding.semanticDiffIdentity == diff.diff.semanticDiffIdentity && review.binding.semanticDiffRevision == diff.diff.semanticDiffRevision, (), s"$label semantic diff identity does not equal its admitted semantic diff")
        _ <- Either.cond(review.binding.scope == candidate.projection.scope && review.binding.scope == diff.diff.scope, (), s"$label scope does not equal its admitted candidate and diff")
      } yield ()

  private def _reviewed_package_context(
    review: InternalModelCandidateReviewBindingAdmission,
    label: String
  ): Either[String, Unit] = {
    val binding = review.binding
    val context = review.reviewedPackageContext
    if binding.reviewedPackageManifest == null || context == null || context.manifestBytes == null then Left(s"$label reviewed package context is missing")
    else
      for {
        bytes <- try Right(Base64.getDecoder.decode(binding.reviewedPackageManifest.rawBytesBase64).toVector) catch { case NonFatal(_) => Left(s"$label reviewed package manifest bytes are malformed") }
        _ <- Either.cond(bytes == context.manifestBytes, (), s"$label reviewed package context bytes do not equal its binding manifest")
        _ <- Either.cond(_sha256(bytes) == binding.reviewedPackageManifest.sha256, (), s"$label reviewed package manifest hash does not equal its bytes")
        json <- io.circe.parser.parse(new String(bytes.toArray, StandardCharsets.UTF_8)).left.map(_ => s"$label reviewed package manifest is not valid JSON")
        root <- json.asObject.toRight(s"$label reviewed package manifest root is not an object")
        schema <- _manifest_string(root, "schemaVersion", label)
        packageid <- _manifest_string(root, "packageId", label)
        namespace <- _manifest_string(root, "projectNamespace", label)
        projectid <- _manifest_string(root, "projectId", label)
        revision <- root("revision").flatMap(_.asNumber).flatMap(_.toLong).toRight(s"$label reviewed package manifest revision is missing")
        lifecycle <- _manifest_string(root, "lifecycleState", label)
        digest <- _manifest_string(root, "packageDigest", label)
        _ <- Either.cond(schema == context.schemaVersion && packageid == context.packageId && namespace == context.projectNamespace && projectid == context.projectId && revision == context.revision && lifecycle == context.lifecycleState && digest == context.packageDigest, (), s"$label reviewed package context fields do not equal its binding manifest")
      } yield ()
  }

  private def _manifest_string(
    root: io.circe.JsonObject,
    key: String,
    label: String
  ): Either[String, String] =
    root(key).flatMap(_.asString).toRight(s"$label reviewed package manifest $key is missing")

  private def _semantic_diff_admission(
    admission: InternalModelSemanticDiffAdmission,
    label: String
  ): Either[String, Unit] =
    if admission == null || admission.diff == null || admission.candidateAdmission == null then Left(s"$label semantic diff admission is missing")
    else
      for {
        decodeddiff <- InternalModelSemanticDiffCodec.decode(InternalModelVerifiedProjection(admission.diffArtifactId, "projection", admission.diffPackageRelativePath, true, Vector.empty, admission.diff.canonicalBytes))
        _ <- Either.cond(decodeddiff == admission.diff, (), s"$label semantic diff does not equal its canonical bytes")
        _ <- Either.cond(_sha256(admission.diff.canonicalBytes) == admission.diffArtifactSha256, (), s"$label semantic diff artifact hash does not equal its canonical bytes")
        _ <- _nonblank(admission.diffArtifactId, s"$label semantic diff artifact ID")
        _ <- _candidate_admission(admission.candidateAdmission, label)
        candidate = admission.candidateAdmission
        _ <- Either.cond(admission.diff.candidateArtifactId == candidate.candidateArtifactId && admission.diff.candidateArtifactSha256 == candidate.candidateArtifactSha256, (), s"$label semantic diff candidate tuple does not equal its admitted candidate")
        _ <- Either.cond(admission.diff.candidateIdentity == candidate.projection.candidateIdentity && admission.diff.candidateModelIdentity == candidate.projection.candidateModelIdentity && admission.diff.candidateRevision == candidate.projection.candidateRevision && admission.diff.scope == candidate.projection.scope, (), s"$label semantic diff candidate basis does not equal its admitted candidate")
      } yield ()

  private def _candidate_admission(
    admission: InternalModelCandidateCmlAdmission,
    label: String
  ): Either[String, Unit] =
    if admission == null || admission.projection == null || admission.continuity == null then Left(s"$label candidate admission is missing")
    else
      for {
        decodedcandidate <- InternalModelCandidateCmlProjectionCodec.decode(InternalModelVerifiedProjection(admission.candidateArtifactId, "projection", admission.candidatePackageRelativePath, true, Vector.empty, admission.projection.canonicalBytes))
        _ <- Either.cond(decodedcandidate == admission.projection, (), s"$label candidate does not equal its canonical bytes")
        _ <- Either.cond(_sha256(admission.projection.canonicalBytes) == admission.candidateArtifactSha256, (), s"$label candidate artifact hash does not equal its canonical bytes")
        _ <- _nonblank(admission.candidateArtifactId, s"$label candidate artifact ID")
      } yield ()

  private def _approval_review_binding(
    basis: InternalModelCandidateHumanApprovalBasis,
    review: InternalModelCandidateReviewBindingAdmission,
    label: String
  ): Either[String, Unit] = {
    val binding = review.binding
    val diff = review.semanticDiffAdmission
    val candidate = diff.candidateAdmission
    val reviewed = review.reviewedPackageContext
    if basis == null || reviewed == null then Left(s"$label basis or reviewed package is missing")
    else
      for {
        _ <- Either.cond(basis.reviewArtifact == InternalModelCandidateReviewArtifact(review.reviewArtifactId, review.reviewArtifactSha256), (), s"$label review tuple does not equal its retained review")
        _ <- Either.cond(basis.reviewIdentity == binding.reviewIdentity && basis.reviewRevision == binding.reviewRevision, (), s"$label review identity does not equal its retained review")
        _ <- Either.cond(basis.candidateArtifact == InternalModelCandidateReviewArtifact(candidate.candidateArtifactId, candidate.candidateArtifactSha256), (), s"$label candidate tuple does not equal its retained candidate")
        _ <- Either.cond(basis.candidateIdentity == candidate.projection.candidateIdentity && basis.candidateModelIdentity == candidate.projection.candidateModelIdentity && basis.candidateRevision == candidate.projection.candidateRevision, (), s"$label candidate identity does not equal its retained candidate")
        _ <- Either.cond(basis.semanticDiffArtifact == InternalModelCandidateReviewArtifact(diff.diffArtifactId, diff.diffArtifactSha256), (), s"$label semantic diff tuple does not equal its retained diff")
        _ <- Either.cond(basis.semanticDiffIdentity == diff.diff.semanticDiffIdentity && basis.semanticDiffRevision == diff.diff.semanticDiffRevision, (), s"$label semantic diff identity does not equal its retained diff")
        _ <- Either.cond(basis.scope == binding.scope && basis.scope == candidate.projection.scope && basis.scope == diff.diff.scope, (), s"$label scope does not equal the complete retained review basis")
        _ <- Either.cond(basis.reviewedPackage == InternalModelCandidateHumanApprovalPackageBasis(reviewed.packageDigest, reviewed.packageId, reviewed.projectId, reviewed.projectNamespace, reviewed.revision, reviewed.schemaVersion), (), s"$label reviewed package does not equal its retained historical package")
      } yield ()
  }

  private def _expected_execution_basis(value: InternalModelCandidateReviewExecutionBasis): Either[String, Unit] =
    if value == null then Left("expected current review execution basis is missing")
    else InternalModelCandidateReviewBindingCodec.validateExecutionBasis(value)

  private def _source_snapshot_inventory(
    approval: InternalModelCandidateHumanApprovalAdmission,
    snapshots: Vector[InternalModelVerifiedSourceSnapshot]
  ): Either[String, Unit] =
    if snapshots == null then Left("historical source snapshots are missing")
    else if snapshots.exists(_ == null) then Left("historical source snapshots contain a null entry")
    else {
      val expected = approval.reviewAdmission.reviewedPackageContext.artifacts.filter(_.role == "source-snapshot")
      val ids = snapshots.map(_.artifactId)
      for {
        _ <- Either.cond(ids.distinct.size == ids.size, (), "historical source snapshots contain duplicate artifact IDs")
        _ <- Either.cond(ids.toSet == expected.map(_.artifactId).toSet && snapshots.size == expected.size, (), "historical source snapshots do not exactly cover the reviewed source-snapshot inventory")
        _ <- snapshots.foldLeft[Either[String, Unit]](Right(())) { (result, snapshot) =>
          for {
            _ <- result
            artifact <- expected.find(_.artifactId == snapshot.artifactId).toRight(s"historical source snapshot ${snapshot.artifactId} is unknown")
            _ <- Either.cond(snapshot.packageRelativePath == artifact.packageRelativePath && snapshot.required == artifact.required, (), s"historical source snapshot ${snapshot.artifactId} does not equal reviewed inventory metadata")
            _ <- Either.cond(snapshot.bytes != null, (), s"historical source snapshot ${snapshot.artifactId} bytes option is missing")
            _ <- snapshot.bytes match {
              case Some(bytes) =>
                for {
                  _ <- Either.cond(artifact.present, (), s"historical source snapshot ${snapshot.artifactId} is present outside reviewed inventory")
                  _ <- Either.cond(_sha256(bytes) == artifact.sha256, (), s"historical source snapshot ${snapshot.artifactId} hash does not equal reviewed inventory")
                  _ <- InternalModelSourceSnapshotFreshness.validatedSnapshotKind(bytes.toArray).map(_ => ())
                } yield ()
              case None => Either.cond(!artifact.present && !artifact.required, (), s"historical source snapshot ${snapshot.artifactId} absence does not equal reviewed inventory")
            }
          } yield ()
        }
      } yield ()
    }

  private def _live_source_container(
    snapshots: Vector[InternalModelVerifiedSourceSnapshot],
    livesources: Map[String, InternalModelLiveSourceObservation]
  ): Either[String, Unit] =
    if livesources == null then Left("live source observation map is missing")
    else {
      val known = snapshots.map(_.artifactId).toSet
      for {
        _ <- Either.cond(!livesources.keys.exists(_ == null), (), "live source observation map contains a null artifact ID")
        _ <- Either.cond((livesources.keySet -- known).isEmpty, (), "live source observation map contains an unknown source-snapshot artifact ID")
        _ <- Either.cond(livesources.values.forall(_ != null), (), "live source observation map contains a null observation")
      } yield ()
    }

  private def _supersession(
    approval: InternalModelCandidateHumanApprovalAdmission,
    value: Option[InternalModelCandidateApprovalSupersessionInput]
  ): Either[String, Option[InternalModelCandidateApprovalSupersessionInput]] =
    if value == null then Left("candidate approval supersession option is missing")
    else
      value match {
        case None => Right(None)
        case Some(input) if input == null || input.record == null || input.successorApproval == null => Left("candidate approval supersession input is missing")
        case Some(input) =>
          for {
            decoded <- InternalModelCandidateApprovalSupersessionCodec.decode(input.record.canonicalBytes)
            _ <- Either.cond(decoded == input.record, (), "candidate approval supersession does not equal its canonical bytes")
            _ <- _approval_admission(input.successorApproval, "successor approval")
            _ <- _supersession_binding(approval, input)
          } yield Some(input)
      }

  private def _supersession_binding(
    predecessor: InternalModelCandidateHumanApprovalAdmission,
    input: InternalModelCandidateApprovalSupersessionInput
  ): Either[String, Unit] = {
    val link = input.record
    val prior = predecessor.record.approval
    val successor = input.successorApproval.record.approval
    for {
      _ <- Either.cond(link.predecessorApproval == InternalModelCandidateReviewArtifact(predecessor.approvalArtifactId, predecessor.approvalArtifactSha256), (), "candidate approval supersession predecessor tuple does not equal original approval")
      _ <- Either.cond(link.successorApproval == InternalModelCandidateReviewArtifact(input.successorApproval.approvalArtifactId, input.successorApproval.approvalArtifactSha256), (), "candidate approval supersession successor tuple does not equal separately admitted successor approval")
      _ <- Either.cond(prior.basis.reviewedPackage.projectNamespace == successor.basis.reviewedPackage.projectNamespace && prior.basis.reviewedPackage.projectId == successor.basis.reviewedPackage.projectId && prior.basis.reviewedPackage.packageId == successor.basis.reviewedPackage.packageId, (), "candidate approval supersession crosses package or project identity")
      _ <- Either.cond(prior.basis.scope == successor.basis.scope, (), "candidate approval supersession crosses scope")
      _ <- Either.cond(prior.basis.candidateIdentity == successor.basis.candidateIdentity && prior.basis.candidateModelIdentity == successor.basis.candidateModelIdentity, (), "candidate approval supersession crosses candidate identity")
      _ <- Either.cond(successor.basis.candidateRevision >= prior.basis.candidateRevision, (), "candidate approval supersession rolls candidate revision back")
      _ <- Either.cond(successor.basis.candidateRevision != prior.basis.candidateRevision || successor.basis.candidateArtifact == prior.basis.candidateArtifact, (), "candidate approval supersession changes candidate artifact at the same candidate revision")
      _ <- Either.cond(prior.approvalIdentity != successor.approvalIdentity || successor.approvalRevision > prior.approvalRevision, (), "candidate approval supersession does not advance the same approval identity")
    } yield ()
  }

  private def _basis_invalidations(
    approval: InternalModelCandidateHumanApprovalAdmission,
    current: InternalModelCandidateReviewBindingAdmission,
    expected: InternalModelCandidateReviewExecutionBasis
  ): Vector[InternalModelCandidateApprovalInvalidation] = {
    val original = approval.record.approval.basis
    val review = current.binding
    val candidate = current.semanticDiffAdmission.candidateAdmission
    val diff = current.semanticDiffAdmission.diff
    val packagebasis = current.reviewedPackageContext
    val candidatechanges = Vector(
      Option.when(original.candidateArtifact.artifactId != candidate.candidateArtifactId)("candidateArtifact.artifactId"),
      Option.when(original.candidateArtifact.sha256 != candidate.candidateArtifactSha256)("candidateArtifact.sha256"),
      Option.when(original.candidateIdentity != candidate.projection.candidateIdentity)("candidateIdentity"),
      Option.when(original.candidateModelIdentity != candidate.projection.candidateModelIdentity)("candidateModelIdentity"),
      Option.when(original.candidateRevision != candidate.projection.candidateRevision)("candidateRevision")
    ).flatten
    val reviewchanges = Vector(
      Option.when(original.reviewArtifact.artifactId != current.reviewArtifactId)("reviewArtifact.artifactId"),
      Option.when(original.reviewArtifact.sha256 != current.reviewArtifactSha256)("reviewArtifact.sha256"),
      Option.when(original.reviewIdentity != review.reviewIdentity)("reviewIdentity"),
      Option.when(original.reviewRevision != review.reviewRevision)("reviewRevision")
    ).flatten
    val diffchanges = Vector(
      Option.when(original.semanticDiffArtifact.artifactId != current.semanticDiffAdmission.diffArtifactId)("semanticDiffArtifact.artifactId"),
      Option.when(original.semanticDiffArtifact.sha256 != current.semanticDiffAdmission.diffArtifactSha256)("semanticDiffArtifact.sha256"),
      Option.when(original.semanticDiffIdentity != diff.semanticDiffIdentity)("semanticDiffIdentity"),
      Option.when(original.semanticDiffRevision != diff.semanticDiffRevision)("semanticDiffRevision")
    ).flatten
    val packagechanges = Vector(
      Option.when(original.reviewedPackage.packageDigest != packagebasis.packageDigest)("reviewedPackage.packageDigest"),
      Option.when(original.reviewedPackage.packageId != packagebasis.packageId)("reviewedPackage.packageId"),
      Option.when(original.reviewedPackage.projectId != packagebasis.projectId)("reviewedPackage.projectId"),
      Option.when(original.reviewedPackage.projectNamespace != packagebasis.projectNamespace)("reviewedPackage.projectNamespace"),
      Option.when(original.reviewedPackage.revision != packagebasis.revision)("reviewedPackage.revision"),
      Option.when(original.reviewedPackage.schemaVersion != packagebasis.schemaVersion)("reviewedPackage.schemaVersion")
    ).flatten
    val scopechanges = Vector(
      Option.when(original.scope.componentIdentity != review.scope.componentIdentity)("scope.componentIdentity"),
      Option.when(original.scope.projectionContextIdentity != review.scope.projectionContextIdentity)("scope.projectionContextIdentity"),
      Option.when(original.scope.selectedUseCaseElementIdentity != review.scope.selectedUseCaseElementIdentity)("scope.selectedUseCaseElementIdentity")
    ).flatten
    _invalidation(InternalModelCandidateApprovalInvalidationKind.CandidateBasisChanged, candidatechanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ReviewBasisChanged, reviewchanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.SemanticDiffBasisChanged, diffchanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ReviewedPackageChanged, packagechanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ScopeChanged, scopechanges) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.RulesChanged, _basis_changes("rules", approval.reviewAdmission.binding.rules.map(value => (value.ruleId, value.ruleVersion, value.sha256)), review.rules.map(value => (value.ruleId, value.ruleVersion, value.sha256)), expected.rules.map(value => (value.ruleId, value.ruleVersion, value.sha256)))) ++
      _invalidation(InternalModelCandidateApprovalInvalidationKind.ProvidersChanged, _basis_changes("providers", approval.reviewAdmission.binding.providers.map(value => (value.providerId, value.providerVersion, value.sha256)), review.providers.map(value => (value.providerId, value.providerVersion, value.sha256)), expected.providers.map(value => (value.providerId, value.providerVersion, value.sha256))))
  }

  private def _basis_changes(
    prefix: String,
    original: Vector[(String, String, String)],
    current: Vector[(String, String, String)],
    expected: Vector[(String, String, String)]
  ): Vector[String] =
    _one_basis_changes(s"originalReview.$prefix", original, expected) ++ _one_basis_changes(s"currentReview.$prefix", current, expected)

  private def _one_basis_changes(
    prefix: String,
    actual: Vector[(String, String, String)],
    expected: Vector[(String, String, String)]
  ): Vector[String] = {
    val actualbyid = actual.map(value => value._1 -> (value._2, value._3)).toMap
    val expectedbyid = expected.map(value => value._1 -> (value._2, value._3)).toMap
    val ids = _sort_ids((actualbyid.keySet ++ expectedbyid.keySet).toVector)
    val membership = Option.when(actualbyid.keySet != expectedbyid.keySet)(s"$prefix.ids").toVector
    membership ++ ids.flatMap { id =>
      (actualbyid.get(id), expectedbyid.get(id)) match {
        case (Some(left), Some(right)) => Vector(
          Option.when(left._1 != right._1)(s"$prefix[$id].version"),
          Option.when(left._2 != right._2)(s"$prefix[$id].sha256")
        ).flatten
        case _ => Vector.empty
      }
    }
  }

  private def _source_invalidations(
    approval: InternalModelCandidateHumanApprovalAdmission,
    snapshots: Vector[InternalModelVerifiedSourceSnapshot],
    livesources: Map[String, InternalModelLiveSourceObservation]
  ): Vector[InternalModelCandidateApprovalInvalidation] =
    _sort_ids(snapshots.map(_.artifactId)).flatMap { artifactid =>
      snapshots.find(_.artifactId == artifactid).toVector.flatMap { snapshot =>
        snapshot.bytes match {
          case None => Vector(InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.MissingBaseline, Some(artifactid), Vector.empty))
          case Some(bytes) =>
            val observation = livesources.getOrElse(artifactid, InternalModelLiveSourceObservation.Unavailable("no source-owner live observation was supplied for this source-snapshot artifact"))
            val report = InternalModelSourceSnapshotFreshness.compare(bytes.toArray, observation)
            report.status match {
              case InternalModelSnapshotFreshnessStatus.Unchanged => Vector.empty
              case InternalModelSnapshotFreshnessStatus.Changed => Vector(InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceChanged, Some(artifactid), report.changedDimensionNames))
              case InternalModelSnapshotFreshnessStatus.Unavailable => Vector(InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceUnavailable, Some(artifactid), report.changedDimensionNames))
              case InternalModelSnapshotFreshnessStatus.Unauthorized => Vector(InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceUnauthorized, Some(artifactid), report.changedDimensionNames))
              case InternalModelSnapshotFreshnessStatus.Malformed => Vector(InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceMalformed, Some(artifactid), report.changedDimensionNames))
              case InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting => Vector(InternalModelCandidateApprovalInvalidation(InternalModelCandidateApprovalInvalidationKind.SourceAmbiguousOrConflicting, Some(artifactid), report.changedDimensionNames))
            }
        }
      }
    }

  private def _state(
    decision: InternalModelCandidateHumanApprovalDecision,
    link: Option[InternalModelCandidateApprovalSupersessionInput],
    invalidations: Vector[InternalModelCandidateApprovalInvalidation]
  ): InternalModelCandidateApprovalLifecycleState =
    if link.nonEmpty then InternalModelCandidateApprovalLifecycleState.Superseded
    else if invalidations.nonEmpty then InternalModelCandidateApprovalLifecycleState.Invalidated
    else
      decision match {
        case InternalModelCandidateHumanApprovalDecision.Approved => InternalModelCandidateApprovalLifecycleState.Approved
        case InternalModelCandidateHumanApprovalDecision.Rejected => InternalModelCandidateApprovalLifecycleState.Rejected
        case InternalModelCandidateHumanApprovalDecision.ChangesRequested => InternalModelCandidateApprovalLifecycleState.ChangesRequested
      }

  private def _invalidation(
    kind: InternalModelCandidateApprovalInvalidationKind,
    dimensions: Vector[String]
  ): Vector[InternalModelCandidateApprovalInvalidation] =
    Option.when(dimensions.nonEmpty)(InternalModelCandidateApprovalInvalidation(kind, None, dimensions)).toVector

  private def _sort_ids(values: Vector[String]): Vector[String] =
    values.sortWith((left, right) => Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)) < 0)

  private def _nonblank(value: String, label: String): Either[String, Unit] =
    Either.cond(Option(value).exists(_.nonEmpty), (), s"$label must be nonempty")

  private def _sha256(bytes: Vector[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(byte => f"${byte & 0xff}%02x").mkString
}
