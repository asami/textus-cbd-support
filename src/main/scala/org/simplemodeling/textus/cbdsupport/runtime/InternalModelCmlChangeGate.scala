package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Arrays
import scala.util.control.NonFatal
import org.goldenport.Consequence

/**
 * Current source evidence and exact owned application admission, without mutation.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object InternalModelCmlChangeGate {
  import InternalModelCmlChangeEligibility.*
  import InternalModelCmlChangeProblemKind.*

  def evaluate(projectRoot: Path, request: InternalModelCmlChangeRequest): Consequence[InternalModelCmlChangeReport] =
    InternalModelPackageValidator.verifiedContinuation(projectRoot).flatMap(evaluateVerified(projectRoot, _, request))

  /** Package evidence is captured; only current CML reads deliberately consult the filesystem. */
  def evaluateVerified(projectRoot: Path, handoff: InternalModelVerifiedContinuationPackage,
    request: InternalModelCmlChangeRequest): Consequence[InternalModelCmlChangeReport] = {
    try {
      val structural = for {
        _ <- _validate_request(projectRoot, request)
        _ <- _validate_serialized(handoff, request.currentrequest)
      } yield ()
      structural.fold(Consequence.operationInvalid, _ =>
        InternalModelContinuationActionGate.evaluateVerified(handoff, request.currentrequest).flatMap { continuation =>
          val unknown = request.livesources.keySet -- request.originalsnapshots.map(_.reference).toSet
          val keyproblems = unknown.toVector.sortWith { (left, right) =>
            val ids = _compare_ids(left.artifactId.value, right.artifactId.value)
            if (ids != 0) ids < 0 else if (left.artifactRevision != right.artifactRevision)
              left.artifactRevision.value < right.artifactRevision.value else left.role.ordinal < right.role.ordinal
          }
            .map(reference => _problem(UnknownSourceReference, Inconsistent, "livesources.reference",
              "live input has an unknown, substituted revision or role", Some(reference)))
          val snapshots = request.originalsnapshots.sortWith((left, right) => _compare_ids(left.reference.artifactId.value, right.reference.artifactId.value) < 0)
          val observations = snapshots.map { snapshot =>
            snapshot.reference -> _observation(projectRoot, snapshot, request.livesources.get(snapshot.reference))
          }
          val liveentries = snapshots.zip(observations).map { case (snapshot, (_, observation)) =>
            val result = snapshot.bytes.fold[InternalModelPackageFreshnessResult](InternalModelPackageFreshnessResult.MissingBaseline)(bytes =>
              InternalModelPackageFreshnessResult.Compared(InternalModelSourceSnapshotFreshness.compare(bytes.toArray, observation)))
            InternalModelPackageFreshnessEntry(snapshot.reference, snapshot.path, result)
          }
          val lifecycle: Consequence[Option[InternalModelCandidateApprovalLifecycleReport]] = request.currentrequest.executionbasis match {
            case None => Consequence.success(Option.empty[InternalModelCandidateApprovalLifecycleReport])
            case Some(basis) => InternalModelCandidateApprovalLifecycleEvaluator.evaluate(request.originalapproval,
              request.currentreview, basis, request.originalsnapshots,
              observations.map { case (reference, observation) => reference.artifactId.value -> observation }.toMap,
              request.supersession).map(Some(_))
          }
          lifecycle.map { applicability =>
            val problems = keyproblems ++ _current_review_problems(continuation, request) ++ _semantic_basis_problems(request) ++
              _prerequisite_problems(continuation, request) ++ _authority_problems(projectRoot, request, liveentries)
            val causes = problems.map(_.eligibility) ++ _continuation_causes(continuation, request) ++
              liveentries.flatMap(_source_causes) ++ applicability.toVector.flatMap(_lifecycle_causes)
            val eligibility = _eligibility(causes)
            val plan = Option.when(eligibility == EligibleForSkillApplication)(_plan(request, continuation.approval.get))
            InternalModelCmlChangeReport(eligibility, request.originalapproval, request.currentreview, request.supersession,
              continuation, applicability, liveentries, problems, plan)
          }
        })
    } catch {
      case NonFatal(error) => Consequence.operationInvalid("CML change admission contains an invalid graph: " +
        Option(error.getMessage).getOrElse(error.getClass.getSimpleName))
    }
  }

  private def _validate_serialized(handoff: InternalModelVerifiedContinuationPackage,
    request: InternalModelContinuationRequest): Either[String, Unit] = {
    for {
      _ <- Either.cond(_present_graph(handoff), (), "current continuation capture contains a null graph")
      _ <- handoff.artifacts.filter(_.context.present).foldLeft[Either[String, Unit]](Right(())) { (result, entry) =>
        val context = entry.context
        result.flatMap { _ => entry.bytes.toRight("present captured artifact has no payload").flatMap { bytes =>
          val projection = InternalModelVerifiedProjection(context.reference, context.path, context.required, context.dependencies, bytes)
          if (request.candidateartifact.contains(context.reference)) InternalModelCandidateCmlProjectionCodec.decode(projection).map(_ => ())
          else if (request.semanticdiffartifact.contains(context.reference)) InternalModelSemanticDiffCodec.decode(projection).map(_ => ())
          else if (request.reviewartifact.contains(context.reference)) InternalModelCandidateReviewBindingCodec.decode(bytes).map(_ => ())
          else if (request.approvalartifact.contains(context.reference)) InternalModelCandidateHumanApprovalCodec.decode(bytes).map(_ => ())
          else context.reference.role match {
            case InternalModelArtifactRole.SourceSnapshot => InternalModelSourceSnapshotFreshness.validatedSnapshotKind(bytes.toArray).map(_ => ())
            case InternalModelArtifactRole.Decision => InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision(
              context.reference, context.path, context.required, context.dependencies, bytes)).map(_ => ())
            case InternalModelArtifactRole.OpenIssue => InternalModelOpenIssueRecordCodec.decode(InternalModelVerifiedOpenIssue(
              context.reference, context.path, context.required, context.dependencies, bytes)).map(_ => ())
            case _ => Right(())
          }
        }}
      }
    } yield ()
  }

  private def _validate_request(projectroot: Path, request: InternalModelCmlChangeRequest): Either[String, Unit] = {
    for {
      _ <- Either.cond(projectroot != null && _present_graph(request), (), "CML change root or typed graph contains null")
      _ <- InternalModelContinuationEvidenceAdmission.validateRequest(request.currentrequest)
      _ <- InternalModelCandidateHumanApprovalValidator.validateAdmission(request.originalapproval)
      _ <- InternalModelCandidateReviewBindingValidator.validateAdmission(request.currentreview)
      expected = request.originalapproval.reviewAdmission.carrierPackageContext.artifacts.filter(_.reference.role == InternalModelArtifactRole.SourceSnapshot)
      snapshots = request.originalsnapshots
      _ <- Either.cond(snapshots.map(_.reference.artifactId).distinct.size == snapshots.size && snapshots.size == expected.size,
        (), "original source inventory must be complete and unique")
      _ <- snapshots.foldLeft[Either[String, Unit]](Right(())) { (result, snapshot) =>
        for {
          _ <- result
          entry <- expected.find(_.reference == snapshot.reference).toRight("original snapshot full reference is not inventoried")
          _ <- Either.cond(entry.path == snapshot.path && entry.required == snapshot.required &&
            entry.dependencies == snapshot.dependencies && entry.present == snapshot.bytes.isDefined,
            (), "original source inventory metadata contradicts actual original admission")
          _ <- snapshot.bytes.fold[Either[String, Unit]](Right(()))(bytes =>
            InternalModelSourceSnapshotFreshness.validatedSnapshotKind(bytes.toArray).map(_ => ()))
        } yield ()
      }
      _ <- request.livesources.keys.foldLeft[Either[String, Unit]](Right(()))((result, reference) => result.flatMap(_ => _artifact_reference(reference)))
      _ <- request.supersession.fold[Either[String, Unit]](Right(()))(input => _validate_supersession(request.originalapproval, input))
      _ <- request.mutationauthority.fold[Either[String, Unit]](Right(())) { authority =>
        for {
          _ <- _record_reference(authority.requestreference)
          _ <- InternalModelPackageId.from(authority.packagereference.packageId.value)
          _ <- InternalModelProjectToken.from(authority.packagereference.projectNamespace.value)
          _ <- InternalModelProjectToken.from(authority.packagereference.projectId.value)
          _ <- Either.cond(Vector(authority.scope.componentIdentity, authority.scope.projectionContextIdentity,
            authority.scope.selectedUseCaseElementIdentity, authority.provenance.authority, authority.provenance.identity)
            .forall(_nonblank), (), "mutation authority scope and provenance must be explicit nonblank values")
          _ <- Either.cond(authority.provenance.locator.forall(_nonblank) && authority.provenance.revision.forall(_nonblank),
            (), "mutation authority optional provenance values must be nonblank")
        } yield ()
      }
    } yield ()
  }

  private def _current_review_problems(continuation: InternalModelContinuationReport,
    request: InternalModelCmlChangeRequest): Vector[InternalModelCmlChangeProblem] = {
    val review = request.currentreview
    val binding = review.binding
    val selected = request.currentrequest
    val checks = Vector(
      "currentreview.carrier" -> (review.carrierPackageContext == continuation.state.packageContext),
      "currentreview.packageReference" -> (binding.subject.packageReference == selected.packagereference),
      "currentreview.scope" -> (binding.scope == selected.scope),
      "currentreview.reviewArtifactReference" -> selected.reviewartifact.forall(_ == review.reviewArtifactReference),
      "currentreview.candidateArtifactReference" -> selected.candidateartifact.forall(_ == binding.candidateArtifactReference),
      "currentreview.semanticDiffArtifactReference" -> selected.semanticdiffartifact.forall(_ == binding.semanticDiffArtifactReference),
      "currentreview.realizationReference" -> (binding.realizationReference == selected.realizationreference),
      "currentreview.realizationArtifactReference" -> (binding.realizationArtifactReference ==
        continuation.state.continuity.binding.realizationArtifactReference),
      "currentreview.continuityArtifactReference" -> continuation.state.artifacts.exists(entry =>
        entry.context.reference == binding.continuityArtifactReference && entry.context.present &&
          entry.context.reference.role == InternalModelArtifactRole.Projection)
    )
    val metadata = continuation.state.artifacts.find(_.context.reference == review.reviewArtifactReference)
    val captured = metadata.flatMap(_.bytes).map(InternalModelCandidateReviewBindingCodec.decode)
    val capturechecks = Vector(
      "currentreview.capturedSelection" -> metadata.exists(entry => entry.context.present &&
        entry.context.path == review.reviewArtifactPackageRelativePath && entry.context.dependencies == binding.subject.artifacts),
      "currentreview.capturedBinding" -> captured.exists(_.toOption.contains(binding))
    )
    (checks ++ capturechecks).collect { case (dimension, false) =>
      _problem(CurrentReviewMismatch, Inconsistent, dimension, "separately admitted current review disagrees with current capture or explicit selection",
        Some(review.reviewArtifactReference))
    }
  }

  private def _semantic_basis_problems(request: InternalModelCmlChangeRequest): Vector[InternalModelCmlChangeProblem] = {
    val original = request.originalapproval.reviewAdmission
    val current = request.currentreview
    Vector(
      "realizationReference" -> (original.binding.realizationReference == current.binding.realizationReference),
      "realizationArtifactReference" -> (original.binding.realizationArtifactReference == current.binding.realizationArtifactReference),
      "continuityArtifactReference" -> (original.binding.continuityArtifactReference == current.binding.continuityArtifactReference),
      "continuityReference" -> (original.semanticDiffAdmission.candidateAdmission.continuity.binding.bindingReference ==
        current.semanticDiffAdmission.candidateAdmission.continuity.binding.bindingReference)
    ).collect { case (dimension, false) => _problem(SemanticBasisChanged, ReReviewRequired, dimension,
      "explicit realization or projection basis changed from the original approval") }
  }

  private def _prerequisite_problems(continuation: InternalModelContinuationReport,
    request: InternalModelCmlChangeRequest): Vector[InternalModelCmlChangeProblem] = {
    val action = InternalModelContinuationAction.HandoffApprovedCandidate
    Option.when(request.currentrequest.executionbasis.isEmpty)(_problem(MissingExecutionBasis, Incomplete, "executionbasis",
      "independently admitted current execution basis is required" )).toVector ++
      Option.when(request.originalapproval.record.approval.decision != InternalModelCandidateHumanApprovalDecision.Approved ||
        request.originalapproval.record.approval.unresolvedItems.nonEmpty)(
        _problem(OriginalDecisionBlocked, Blocked, "originalapproval.decision", "original actual decision withholds approval or retains unresolved items")).toVector ++
      Option.when(request.supersession.nonEmpty)(_problem(SupersededApproval, Blocked, "supersession",
        "explicit successor link makes only the exact predecessor unusable")).toVector ++
      Option.when(continuation.eligibility == InternalModelContinuationEligibility.Eligible && !continuation.action.contains(action))(
        _problem(MissingApprovedHandoff, Blocked, "stage/action", "only exact Eligible HandoffApprovedCandidate admits skill application")).toVector ++
      Option.when(continuation.action.contains(action) && continuation.approval.isEmpty)(
        _problem(MissingApprovedHandoff, Incomplete, "approval", "current actual human admission is missing")).toVector
  }

  private def _authority_problems(projectroot: Path, request: InternalModelCmlChangeRequest,
    livesources: Vector[InternalModelPackageFreshnessEntry]): Vector[InternalModelCmlChangeProblem] = {
    request.mutationauthority match {
      case None => Vector(_problem(MissingAuthority, Incomplete, "mutationauthority", "independent canonical-source ownership authority is missing"))
      case Some(authority) =>
        val candidate = request.currentreview.semanticDiffAdmission.candidateAdmission
        val targets = candidate.projection.targets
        val problems = Vector.newBuilder[InternalModelCmlChangeProblem]
        def _add_(condition: Boolean, kind: InternalModelCmlChangeProblemKind, dimension: String, diagnostic: String): Unit =
          if (!condition) problems += _problem(kind, Inconsistent, dimension, diagnostic)
        val root = projectroot.toAbsolutePath.normalize.toString
        _add_(authority.projectroot == root && Path.of(authority.projectroot).isAbsolute &&
          Path.of(authority.projectroot).normalize.toString == authority.projectroot,
          AuthorityRootMismatch, "mutationauthority.projectroot", "authority must name this exact normalized absolute consuming root")
        _add_(authority.packagereference == candidate.packageContext.reference, AuthorityPackageMismatch,
          "mutationauthority.packagereference", "authority package differs from the exact candidate package")
        _add_(authority.scope == candidate.projection.scope, AuthorityScopeMismatch,
          "mutationauthority.scope", "authority scope differs from the exact candidate scope")
        _add_(authority.targets.nonEmpty && authority.targets.map(_.targetid).distinct.size == authority.targets.size &&
          authority.targets.map(_.projectrelativepath).distinct.size == authority.targets.size &&
          authority.targets.map(_.targetid).toSet == targets.map(_.targetId).toSet && authority.targets.size == targets.size,
          AuthorityTargetMismatch, "mutationauthority.targets", "authority must name the unique complete existing candidate target set")
        authority.targets.foreach { owned =>
          targets.find(_.targetId == owned.targetid) match {
            case None => problems += _problem(AuthorityTargetMismatch, Inconsistent, "mutationauthority.targetid",
              "authority names an unknown candidate target", targetid = Some(owned.targetid))
            case Some(target) =>
              val reference = Some(target.baselineArtifactReference)
              if (owned.projectrelativepath != target.projectRelativePath || !InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(owned.projectrelativepath))
                problems += _problem(AuthorityTargetMismatch, Inconsistent, "mutationauthority.projectrelativepath",
                  "authority target path does not equal the existing candidate target", reference, Some(owned.targetid))
              if (owned.sourceauthority != target.source.authority || owned.sourceidentity != target.source.identity)
                problems += _problem(AuthorityOwnerMismatch, Inconsistent, "mutationauthority.sourceowner",
                  "authority owner does not equal candidate baseline ownership", reference, Some(owned.targetid))
              if (!_nonblank(owned.nextsourcerevision)) problems += _problem(MissingNextSourceRevision, Incomplete,
                "mutationauthority.nextsourcerevision", "explicit nonblank next source-owned revision is required", reference, Some(owned.targetid))
              else {
                val liveversion = livesources.find(_.reference == target.baselineArtifactReference).toVector.flatMap { entry =>
                  entry.result match {
                    case InternalModelPackageFreshnessResult.Compared(report) => report.observedSourceRevision.toVector
                    case _ => Vector.empty
                  }
                }
                if ((target.source.revision.toVector ++ liveversion).contains(owned.nextsourcerevision))
                  problems += _problem(NextSourceRevisionMismatch, Inconsistent, "mutationauthority.nextsourcerevision",
                    "next source version repeats a known current source revision", reference, Some(owned.targetid))
              }
          }
        }
        problems.result()
    }
  }

  private def _observation(projectroot: Path, snapshot: InternalModelVerifiedSourceSnapshot,
    input: Option[InternalModelPackageFreshnessInput]): InternalModelLiveSourceObservation = {
    import InternalModelLiveSourceObservation.*
    val kind = snapshot.bytes.flatMap(bytes => InternalModelSourceSnapshotFreshness.validatedSnapshotKind(bytes.toArray).toOption)
    input match {
      case None => Unavailable("no source-owner live observation was supplied")
      case Some(InternalModelPackageFreshnessInput.SourceObservation(observation)) =>
        if (kind.contains("cml-baseline") && observation.isInstanceOf[Observed])
          Malformed("CML baseline requires a source-owner current target request, not caller payload")
        else observation
      case Some(InternalModelPackageFreshnessInput.CmlObserved(authority, identity, revision, path)) =>
        if (!kind.contains("cml-baseline")) Malformed("CML request cannot observe a non-CML snapshot")
        else if (!_nonblank(authority) || !_nonblank(identity) || !revision.forall(_nonblank) ||
          !InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(path)) Malformed("CML source-owner request is malformed")
        else NativeCmlFileReader.read(projectroot, path) match {
          case NativeCmlFileReader.Result.Read(bytes) => Observed(authority, identity, revision, bytes, Some(path))
          case NativeCmlFileReader.Result.Unavailable(reason) => Unavailable(reason)
          case NativeCmlFileReader.Result.Unauthorized(reason) => Unauthorized(reason)
          case NativeCmlFileReader.Result.Malformed(reason) => Malformed(reason)
        }
    }
  }

  private def _continuation_causes(report: InternalModelContinuationReport,
    request: InternalModelCmlChangeRequest): Vector[InternalModelCmlChangeEligibility] = {
    import InternalModelContinuationProblemKind.*
    report.problems.map { problem => problem.kind match {
      case MissingPrerequisite => Incomplete
      case BlockingIssue | HumanDecisionNotApproved | HumanUnresolvedItems => Blocked
      case DecisionMismatch if problem.dimension == "decision.currentAccepted" => Blocked
      case SemanticAdmissionFailed if problem.dimension == "review.admission" && request.currentrequest.executionbasis.exists(basis =>
        basis.rules != request.currentreview.binding.rules || basis.providers != request.currentreview.binding.providers) => ReReviewRequired
      case _ => Inconsistent
    }}
  }

  private def _source_causes(entry: InternalModelPackageFreshnessEntry): Vector[InternalModelCmlChangeEligibility] = entry.result match {
    case InternalModelPackageFreshnessResult.MissingBaseline => Vector(Incomplete)
    case InternalModelPackageFreshnessResult.Compared(report) =>
      // An unknown revision transition stays missing; independent owner/path drift still requires re-review.
      val changed = report.changedDimensionNames.filterNot(dimension =>
        report.status == InternalModelSnapshotFreshnessStatus.Incomplete && dimension == "source.revision")
      val changes = Option.when(changed.nonEmpty)(ReReviewRequired).toVector
      changes ++ (report.status match {
        case InternalModelSnapshotFreshnessStatus.Unchanged | InternalModelSnapshotFreshnessStatus.Changed => Vector.empty
        case InternalModelSnapshotFreshnessStatus.Incomplete | InternalModelSnapshotFreshnessStatus.Unavailable => Vector(Incomplete)
        case InternalModelSnapshotFreshnessStatus.Unauthorized | InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting => Vector(Blocked)
        case InternalModelSnapshotFreshnessStatus.Malformed => Vector(Inconsistent)
      })
  }

  private def _lifecycle_causes(report: InternalModelCandidateApprovalLifecycleReport): Vector[InternalModelCmlChangeEligibility] = {
    val drift = report.invalidations.filter(value => value.changedDimensionNames.exists(dimension =>
      value.kind != InternalModelCandidateApprovalInvalidationKind.SourceIncomplete || dimension != "source.revision")).map(_ => ReReviewRequired)
    val blocked = report.approval.record.approval.decision != InternalModelCandidateHumanApprovalDecision.Approved ||
      report.approval.record.approval.unresolvedItems.nonEmpty || report.supersession.nonEmpty
    drift ++ Option.when(blocked)(Blocked).toVector
  }

  private def _eligibility(causes: Vector[InternalModelCmlChangeEligibility]): InternalModelCmlChangeEligibility =
    Vector(Inconsistent, ReReviewRequired, Blocked, Incomplete).find(causes.contains).getOrElse(EligibleForSkillApplication)

  private def _plan(request: InternalModelCmlChangeRequest,
    approval: InternalModelCandidateHumanApprovalAdmission): InternalModelCmlApplicationPlan = {
    val review = request.currentreview
    val binding = review.binding
    val diff = review.semanticDiffAdmission
    val candidate = diff.candidateAdmission
    val authority = request.mutationauthority.get
    val targets = candidate.projection.targets.map { target =>
      InternalModelCmlApplicationTarget(target, candidate.targetBytes.find(_.targetId == target.targetId).get,
        authority.targets.find(_.targetid == target.targetId).get.nextsourcerevision)
    }
    InternalModelCmlApplicationPlan(authority, binding.subject.packageReference, binding.scope, binding.subject,
      candidate.candidateArtifactReference, candidate.projection.candidateReference, candidate.projection.candidateModelIdentity,
      binding.realizationArtifactReference, binding.realizationReference, binding.continuityArtifactReference,
      candidate.continuity.binding.bindingReference, diff.diffArtifactReference, diff.diff.semanticDiffReference,
      review.reviewArtifactReference, binding.reviewReference, approval.approvalArtifactReference,
      approval.record.approval.approvalReference, approval, targets)
  }

  private def _problem(kind: InternalModelCmlChangeProblemKind, eligibility: InternalModelCmlChangeEligibility,
    dimension: String, diagnostic: String, artifactreference: Option[InternalModelArtifactReference] = None,
    targetid: Option[String] = None): InternalModelCmlChangeProblem =
    InternalModelCmlChangeProblem(kind, eligibility, dimension, artifactreference, targetid, diagnostic)

  private def _present_graph(value: Any): Boolean = value match {
    case null => false
    case values: Iterable[?] => values.forall(_present_graph)
    case value: Product => value.productIterator.forall(_present_graph)
    case _ => true
  }

  private def _artifact_reference(reference: InternalModelArtifactReference): Either[String, Unit] = for {
    _ <- InternalModelArtifactId.from(reference.artifactId.value)
    _ <- InternalModelArtifactRevision.from(reference.artifactRevision.value)
  } yield ()

  private def _record_reference(reference: InternalModelRecordReference): Either[String, Unit] = for {
    _ <- Either.cond(_nonblank(reference.recordId.value), (), "record identity must be nonblank valid Unicode")
    _ <- InternalModelRecordId.from(reference.recordId.value)
    _ <- InternalModelRecordRevision.from(reference.recordRevision.value)
  } yield ()

  private def _validate_supersession(original: InternalModelCandidateHumanApprovalAdmission,
    input: InternalModelCandidateApprovalSupersessionInput): Either[String, Unit] = {
    val prior = original.record.approval
    val next = input.successorApproval.record.approval
    for {
      _ <- InternalModelCandidateApprovalSupersessionCodec.validateValue(input.record)
      _ <- InternalModelCandidateHumanApprovalValidator.validateAdmission(input.successorApproval)
      _ <- Either.cond(input.record.predecessorApproval == original.approvalArtifactReference &&
        input.record.successorApproval == input.successorApproval.approvalArtifactReference, (), "supersession does not name exact actual admissions")
      _ <- Either.cond(prior.basis.subject.packageReference == next.basis.subject.packageReference &&
        prior.basis.scope == next.basis.scope && prior.basis.candidateReference.recordId == next.basis.candidateReference.recordId &&
        prior.basis.candidateModelIdentity == next.basis.candidateModelIdentity, (), "supersession crosses actual candidate ownership")
      _ <- Either.cond(next.basis.candidateReference.recordRevision.value >= prior.basis.candidateReference.recordRevision.value &&
        (next.basis.candidateReference.recordRevision != prior.basis.candidateReference.recordRevision ||
          next.basis.candidateArtifactReference == prior.basis.candidateArtifactReference), (), "supersession contradicts explicit candidate lineage")
      _ <- Either.cond(prior.approvalReference.recordId != next.approvalReference.recordId ||
        next.approvalReference.recordRevision.value > prior.approvalReference.recordRevision.value,
        (), "supersession does not advance the same logical approval identity")
    } yield ()
  }

  private def _nonblank(value: String): Boolean = value != null && {
    val codepoints = value.codePoints().toArray
    codepoints.forall(code => code < 0xd800 || code > 0xdfff) && codepoints.exists(code =>
      !Character.isWhitespace(code) && !Character.isSpaceChar(code) && code != 0x85)
  }
  private def _compare_ids(left: String, right: String): Int =
    Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))
}
