package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import io.circe.parser.parse
import org.goldenport.Consequence

/**
 * Pure composition of existing semantic owners over actual captured payload.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelContinuationEvidenceAdmission {
  import InternalModelContinuationCondition.*
  import InternalModelContinuationProblemKind.{HumanUnresolvedItems => HumanUnresolvedItemsProblem, *}

  final case class PredicateProblem(
    predicate: InternalModelContinuationCondition,
    problem: InternalModelContinuationProblem
  )

  final case class Evidence(
    candidate: Option[InternalModelCandidateCmlAdmission],
    semanticdiff: Option[InternalModelSemanticDiffAdmission],
    review: Option[InternalModelCandidateReviewBindingAdmission],
    approval: Option[InternalModelCandidateHumanApprovalAdmission],
    decisions: Vector[InternalModelContinuationDecisionEvidence],
    openissues: Vector[InternalModelContinuationOpenIssueEvidence],
    consumed: Vector[InternalModelArtifactReference],
    problems: Vector[PredicateProblem]
  )

  /** Request syntax is separate from contradictory, well-formed independent evidence. */
  def validateRequest(request: InternalModelContinuationRequest): Either[String, Unit] = {
    for {
      _ <- Either.cond(_present_graph(request), (), "continuation request contains a null graph, option, collection or element")
      _ <- InternalModelPackageId.from(request.packagereference.packageId.value)
      _ <- InternalModelProjectToken.from(request.packagereference.projectNamespace.value)
      _ <- InternalModelProjectToken.from(request.packagereference.projectId.value)
      _ <- Either.cond(request.carrierrevision > 0, (), "continuation carrierrevision must be positive")
      _ <- _record_reference(request.realizationreference)
      _ <- _scope(request.scope)
      selected = Vector(request.candidateartifact, request.semanticdiffartifact, request.reviewartifact, request.approvalartifact)
      roles = Vector(InternalModelArtifactRole.Projection, InternalModelArtifactRole.Projection, InternalModelArtifactRole.Validation, InternalModelArtifactRole.Approval)
      _ <- selected.zip(roles).foldLeft[Either[String, Unit]](Right(())) { case (result, (reference, role)) =>
        result.flatMap(_ => reference.fold[Either[String, Unit]](Right(()))(value =>
          _artifact_reference(value).flatMap(_ => Either.cond(value.role == role, (), "continuation selected reference has the wrong role"))))
      }
      references = selected.flatten
      _ <- Either.cond(references.map(_.artifactId).distinct.size == references.size, (), "continuation selected reference roles must not alias")
      _ <- request.humandecision.fold[Either[String, Unit]](Right(()))(InternalModelCandidateHumanApprovalCodec.validateValue)
      _ <- request.requireddecisions.fold[Either[String, Unit]](Right(())) { requirements =>
        for {
          _ <- Either.cond(requirements.distinct.size == requirements.size && requirements.map(value => (value.artifactreference, value.recordreference.recordId)).distinct.size == requirements.size, (), "duplicate continuation decision requirements")
          _ <- requirements.foldLeft[Either[String, Unit]](Right(())) { (result, requirement) =>
            for {
              _ <- result
              _ <- _artifact_reference(requirement.artifactreference)
              _ <- Either.cond(requirement.artifactreference.role == InternalModelArtifactRole.Decision, (), "decision requirement must select Decision role")
              _ <- _record_reference(requirement.recordreference)
              _ <- _text(requirement.topicidentity)
              _ <- _text(requirement.choiceidentity)
              _ <- Either.cond(requirement.affectedtargets.nonEmpty && requirement.affectedtargets.distinct.size == requirement.affectedtargets.size, (), "decision requirement targets must be nonempty and distinct")
              _ <- requirement.affectedtargets.foldLeft[Either[String, Unit]](Right(()))((result, target) => result.flatMap(_ => _target(target)))
            } yield ()
          }
        } yield ()
      }
      _ <- request.requiredmappings.fold[Either[String, Unit]](Right(())) { requirements =>
        for {
          _ <- Either.cond(requirements.distinct.size == requirements.size && requirements.map(value => (value.targetid, value.mappingid)).distinct.size == requirements.size, (), "duplicate continuation mapping requirements")
          _ <- requirements.foldLeft[Either[String, Unit]](Right(())) { (result, requirement) =>
            for {
              _ <- result
              _ <- _text(requirement.targetid)
              _ <- _text(requirement.mappingid)
              _ <- _target(requirement.semantictarget)
            } yield ()
          }
        } yield ()
      }
    } yield ()
  }

  def admit(
    state: InternalModelRehydratedState,
    continuityPackage: InternalModelVerifiedProjectionContinuityPackage,
    request: InternalModelContinuationRequest,
    required: Set[InternalModelContinuationCondition]
  ): Evidence = {
    val problems = Vector.newBuilder[PredicateProblem]
    val consumed = scala.collection.mutable.LinkedHashSet.empty[InternalModelArtifactReference]
    val realizationpackage = continuityPackage.realizationpackage
    val realization = state.continuity.realization

    def _problem_(predicate: InternalModelContinuationCondition, kind: InternalModelContinuationProblemKind, dimension: String,
      diagnostic: String, artifact: Option[InternalModelArtifactReference] = None, record: Option[InternalModelRecordReference] = None): Unit = {
      problems += PredicateProblem(predicate, InternalModelContinuationProblem(kind, dimension, artifact, record, diagnostic, None))
    }
    def _consume_(reference: InternalModelArtifactReference, predicate: InternalModelContinuationCondition): Unit = {
      if (!consumed.contains(reference)) {
        consumed += reference
        if (!state.cursor.selectedArtifacts.contains(reference)) {
          _problem_(predicate, SelectionMismatch, "cursor.selectedArtifacts", "consumed full reference is not cursor-selected", Some(reference))
        }
        state.artifacts.find(_.context.reference == reference).foreach(_.context.dependencies.foreach(_consume_(_, predicate)))
      }
    }
    def _select_(reference: Option[InternalModelArtifactReference], needed: Boolean, predicate: InternalModelContinuationCondition, dimension: String): Option[InternalModelVerifiedProjection] = {
      reference match {
        case None =>
          if (needed) _problem_(predicate, MissingPrerequisite, dimension, "independent selected artifact is missing")
          None
        case Some(value) =>
          state.artifacts.filter(_.context.reference.artifactId == value.artifactId) match {
            case Vector(entry) if entry.context.reference == value && entry.context.present && entry.bytes.nonEmpty =>
              _consume_(value, predicate)
              val context = entry.context
              Some(InternalModelVerifiedProjection(value, context.path, context.required, context.dependencies, entry.bytes.get))
            case Vector(entry) =>
              val kind = if (entry.context.reference.artifactRevision != value.artifactRevision) RevisionMismatch else SelectionMismatch
              _problem_(predicate, kind, dimension, "independent selection contradicts captured revision, role or presence", Some(value))
              None
            case _ =>
              _problem_(predicate, SelectionMismatch, dimension, "independent selected ID does not resolve uniquely in capture", Some(value))
              None
          }
      }
    }
    def _admit_[A](result: Consequence[A], predicate: InternalModelContinuationCondition, dimension: String,
      artifact: InternalModelArtifactReference): Option[A] = {
      result.toOption match {
        case Some(value) => Some(value)
        case None =>
          _problem_(predicate, SemanticAdmissionFailed, dimension, result.show, Some(artifact))
          None
      }
    }

    val expected = request.packagereference
    val actual = state.packageContext.reference
    Vector("packageId" -> (expected.packageId == actual.packageId),
      "projectNamespace" -> (expected.projectNamespace == actual.projectNamespace),
      "projectId" -> (expected.projectId == actual.projectId)).foreach { case (dimension, matches) =>
      if (!matches) _problem_(ModelAdmitted, IdentityMismatch, dimension, "independent package/project identity differs from actual capture")
    }
    if (request.carrierrevision != state.packageContext.revision) {
      _problem_(ModelAdmitted, RevisionMismatch, "carrierrevision", "independent carrier revision differs from actual capture")
    }
    if (request.realizationreference.recordId != realization.realizationReference.recordId) {
      _problem_(ModelAdmitted, IdentityMismatch, "realizationreference.recordId", "independent realization logical identity differs", Some(realizationpackage.realization.reference), Some(request.realizationreference))
    }
    if (request.realizationreference.recordRevision != realization.realizationReference.recordRevision) {
      _problem_(ModelAdmitted, RevisionMismatch, "realizationreference.recordRevision", "independent realization logical revision differs", Some(realizationpackage.realization.reference), Some(request.realizationreference))
    }
    if (request.scope != realization.scope) {
      _problem_(ModelAdmitted, ScopeMismatch, "scope", "independent scope differs from admitted realization", Some(realizationpackage.realization.reference))
    }
    _consume_(continuityPackage.projection.reference, ModelAdmitted)

    val needhuman = required.contains(HumanApproved) || required.contains(HumanUnresolvedItems) || request.approvalartifact.nonEmpty || request.humandecision.nonEmpty
    val needreview = required.contains(ReviewAdmitted) || needhuman || request.reviewartifact.nonEmpty
    val needdiff = required.contains(MappingsComplete) || needreview || request.semanticdiffartifact.nonEmpty
    val needcandidate = needdiff || request.candidateartifact.nonEmpty
    val candidateartifact = _select_(request.candidateartifact, needcandidate, MappingsComplete, "candidateartifact")
    val diffartifact = _select_(request.semanticdiffartifact, needdiff, MappingsComplete, "semanticdiffartifact")
    val reviewartifact = _select_(request.reviewartifact, needreview, ReviewAdmitted, "reviewartifact")
    val approvalartifact = _select_(request.approvalartifact, needhuman, HumanApproved, "approvalartifact")
    val candidatepackage = candidateartifact.map(InternalModelVerifiedCandidateCmlProjectionPackage(state.packageContext, continuityPackage, _))
    val diffpackage = for { candidate <- candidatepackage; diff <- diffartifact } yield InternalModelVerifiedSemanticDiffPackage(candidate, diff)
    val diff = diffpackage.flatMap(value => _admit_(InternalModelSemanticDiffValidator.validateVerified(value), MappingsComplete, "semanticdiff.admission", value.semanticdiff.reference))
    val candidate = diff.map(_.candidateAdmission).orElse(candidatepackage.flatMap(value =>
      _admit_(InternalModelCandidateCmlProjectionValidator.validateVerified(value), MappingsComplete, "candidate.admission", value.candidate.reference)))
    val reviewpackage = for { diff <- diffpackage; review <- reviewartifact } yield InternalModelVerifiedCandidateReviewBindingPackage(state.packageContext, diff, review)
    if (needreview && request.executionbasis.isEmpty) {
      _problem_(ReviewAdmitted, MissingPrerequisite, "executionbasis", "independent review execution basis is missing", request.reviewartifact)
    }
    request.executionbasis.foreach { basis =>
      InternalModelCandidateReviewBindingCodec.validateExecutionBasis(basis).left.foreach { diagnostic =>
        _problem_(ReviewAdmitted, SemanticAdmissionFailed, "executionbasis", diagnostic, request.reviewartifact)
      }
    }
    val review = for {
      captured <- reviewpackage
      basis <- request.executionbasis
      _ <- diff
      admitted <- _admit_(InternalModelCandidateReviewBindingValidator.validateVerified(captured, basis), ReviewAdmitted, "review.admission", captured.reviewArtifact.reference)
    } yield admitted
    if (needhuman && request.humandecision.isEmpty) {
      _problem_(HumanApproved, MissingPrerequisite, "humandecision", "independent actual human input is missing", request.approvalartifact)
    }
    val approval = for {
      captured <- reviewpackage
      selected <- approvalartifact
      basis <- request.executionbasis
      human <- request.humandecision
      _ <- review
      admitted <- _admit_(InternalModelCandidateHumanApprovalValidator.validateVerified(
        InternalModelVerifiedCandidateHumanApprovalPackage(captured, selected), basis, human), HumanApproved, "approval.admission", selected.reference)
    } yield admitted

    // Every present ledger is independently admitted even when selection is contradictory.
    val decisions = state.artifacts.filter(entry => entry.context.present && entry.context.reference.role == InternalModelArtifactRole.Decision).flatMap { entry =>
      val context = entry.context
      _consume_(context.reference, DecisionsComplete)
      _admit_(InternalModelDecisionRecordValidator.validateVerified(InternalModelVerifiedDecisionPackage(realizationpackage,
        InternalModelVerifiedDecision(context.reference, context.path, context.required, context.dependencies, entry.bytes.get))),
        DecisionsComplete, "decision.admission", context.reference).map(InternalModelContinuationDecisionEvidence(context.reference, _))
    }
    val openissues = state.artifacts.filter(entry => entry.context.present && entry.context.reference.role == InternalModelArtifactRole.OpenIssue).flatMap { entry =>
      val context = entry.context
      _consume_(context.reference, NoApprovalBlockingIssues)
      _admit_(InternalModelOpenIssueRecordValidator.validateVerified(InternalModelVerifiedOpenIssuePackage(realizationpackage,
        InternalModelVerifiedOpenIssue(context.reference, context.path, context.required, context.dependencies, entry.bytes.get))),
        NoApprovalBlockingIssues, "openissue.admission", context.reference).map(InternalModelContinuationOpenIssueEvidence(context.reference, _))
    }

    request.requireddecisions match {
      case None => _problem_(DecisionsComplete, MissingPrerequisite, "requireddecisions", "independent decision requirement set is missing")
      case Some(requirements) => requirements.foreach { requirement =>
        val artifact = Some(requirement.artifactreference)
        val record = Some(requirement.recordreference)
        state.artifacts.find(_.context.reference.artifactId == requirement.artifactreference.artifactId) match {
          case None => _problem_(DecisionsComplete, MissingPrerequisite, "decision.artifact", "required decision artifact is missing", artifact, record)
          case Some(entry) if entry.context.reference != requirement.artifactreference =>
            _problem_(DecisionsComplete, SelectionMismatch, "decision.artifact", "required decision artifact reference differs from capture", artifact, record)
          case Some(entry) if !entry.context.present =>
            _problem_(DecisionsComplete, MissingPrerequisite, "decision.artifact", "required decision artifact is absent", artifact, record)
          case Some(_) => decisions.find(_.artifactreference == requirement.artifactreference).foreach { evidence =>
            evidence.admission.ledger.records.find(_.decisionReference.recordId == requirement.recordreference.recordId) match {
              case None => _problem_(DecisionsComplete, MissingPrerequisite, "decision.record", "required decision record is missing", artifact, record)
              case Some(value) =>
                if (value.decisionReference.recordRevision != requirement.recordreference.recordRevision) {
                  _problem_(DecisionsComplete, RevisionMismatch, "decision.recordRevision", "required logical decision revision differs", artifact, record)
                }
                if (value.topicIdentity != requirement.topicidentity) {
                  _problem_(DecisionsComplete, MissingPrerequisite, "decision.topic", "required topic is not present in the exact required record", artifact, record)
                }
                if (value.selectedChoice.choiceIdentity != requirement.choiceidentity) {
                  _problem_(DecisionsComplete, DecisionMismatch, "decision.choice", "required selected choice differs", artifact, record)
                }
                if (value.affectedTargets != requirement.affectedtargets) {
                  _problem_(DecisionsComplete, DecisionMismatch, "decision.affectedTargets", "required exact affected targets differ", artifact, record)
                }
                if (value.basis.status != InternalModelDecisionBasisStatus.Current || value.state != InternalModelDecisionState.Accepted) {
                  _problem_(DecisionsComplete, DecisionMismatch, "decision.currentAccepted", "required record is historical or superseded", artifact, record)
                }
            }
          }
        }
      }
    }
    if (needdiff || request.requiredmappings.nonEmpty) {
      request.requiredmappings match {
        case None => _problem_(MappingsComplete, MissingPrerequisite, "requiredmappings", "independent mapping requirement set is missing", request.candidateartifact)
        case Some(requirements) => candidate.foreach { admission => requirements.foreach { requirement =>
          admission.projection.targets.find(_.targetId == requirement.targetid).flatMap(_.mappings.find(_.mappingId == requirement.mappingid)) match {
            case None => _problem_(MappingsComplete, MissingPrerequisite, "mapping.targetOrMapping", s"required target ${requirement.targetid} / mapping ${requirement.mappingid} is missing", request.candidateartifact)
            case Some(value) if InternalModelSemanticTarget(value.semanticIdentityKind, value.semanticIdentity) != requirement.semantictarget =>
              _problem_(MappingsComplete, MappingMismatch, "mapping.semanticTarget", s"required mapping ${requirement.mappingid} kind or identity differs", request.candidateartifact)
            case _ => ()
          }
        }}
      }
    }
    if (required.contains(HumanApproved)) approval.foreach { admission =>
      val human = admission.record.approval
      if (human.decision != InternalModelCandidateHumanApprovalDecision.Approved) {
        _problem_(HumanApproved, HumanDecisionNotApproved, "human.decision", "independently admitted human decision is not Approved", Some(admission.approvalArtifactReference), Some(human.approvalReference))
      }
      if (human.unresolvedItems.nonEmpty) {
        _problem_(HumanApproved, HumanUnresolvedItemsProblem, "human.unresolvedItems", "independently admitted human input retains unresolved items", Some(admission.approvalArtifactReference), Some(human.approvalReference))
      }
    }
    if (required.contains(NoApprovalBlockingIssues)) openissues.foreach { evidence =>
      evidence.admission.ledger.issues.filter(_.blocking.semanticApproval).foreach { issue =>
        _problem_(NoApprovalBlockingIssues, BlockingIssue, "issue.blocking.semanticApproval", "admitted issue blocks semantic approval", Some(evidence.artifactreference), Some(issue.issueReference))
      }
    }

    consumed.toVector.filter(_.role == InternalModelArtifactRole.SourceSnapshot).foreach { reference =>
      state.artifacts.find(_.context.reference == reference).foreach { entry =>
        InternalModelSourceSnapshotFreshness.validatedSnapshotKind(entry.bytes.get.toArray) match {
          case Left(diagnostic) => _problem_(SourceVersionsKnown, SemanticAdmissionFailed, "source.admission", diagnostic, Some(reference))
          case Right(_) =>
            // The source owner has admitted this explicit envelope; this reads no live source.
            val sourceversion = parse(new String(entry.bytes.get.toArray, StandardCharsets.UTF_8)).toOption
              .flatMap(_.hcursor.downField("source").get[Option[String]]("revision").toOption).flatten
            if (sourceversion.isEmpty) _problem_(SourceVersionsKnown, MissingPrerequisite, "source.revision", "consumed snapshot has no declared source.revision", Some(reference))
        }
      }
    }
    Evidence(candidate, diff, review, approval, decisions, openissues, consumed.toVector, problems.result())
  }

  private def _present_graph(value: Any): Boolean = value match {
    case null => false
    case values: Iterable[?] => values.forall(_present_graph)
    case value: Product => value.productIterator.forall(_present_graph)
    case _ => true
  }

  private def _artifact_reference(reference: InternalModelArtifactReference): Either[String, Unit] = {
    for {
      _ <- InternalModelArtifactId.from(reference.artifactId.value)
      _ <- InternalModelArtifactRevision.from(reference.artifactRevision.value)
      _ <- Either.cond(reference.role != null, (), "artifact role must be present")
    } yield ()
  }

  private def _record_reference(reference: InternalModelRecordReference): Either[String, Unit] = {
    for {
      _ <- InternalModelRecordId.from(reference.recordId.value)
      _ <- InternalModelRecordRevision.from(reference.recordRevision.value)
    } yield ()
  }

  private def _scope(scope: InternalModelSemanticScope): Either[String, Unit] = {
    for {
      _ <- _text(scope.componentIdentity)
      _ <- _text(scope.projectionContextIdentity)
      _ <- _text(scope.selectedUseCaseElementIdentity)
    } yield ()
  }

  private def _target(target: InternalModelSemanticTarget): Either[String, Unit] = {
    for {
      _ <- Either.cond(Set("element", "relationship").contains(target.semanticIdentityKind), (), "required semantic target kind is unsupported")
      _ <- _text(target.semanticIdentity)
    } yield ()
  }

  private def _text(value: String): Either[String, Unit] =
    Either.cond(value != null && StandardCharsets.UTF_8.newEncoder().canEncode(value) &&
      value.codePoints().anyMatch(character => !Character.isWhitespace(character) && !Character.isSpaceChar(character)), (), "required text must be nonblank valid Unicode")
}
