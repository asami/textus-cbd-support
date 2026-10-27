package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Complete-scope deterministic findings. Query evidence is read-only and never synthesizes a path. */
object ModelEditReview {
  final case class SubjectScope(subject: Subject, family: String, evidence: RelationshipScopeEvidence)
  final case class BehaviorCoverage(useCase: ComponentDashboardSemanticTargetIdentity, uncovered: Vector[Subject],
    evidence: RelationshipScopeEvidence)
  final case class CapabilityDemand(useCase: ComponentDashboardSemanticTargetIdentity,
    references: Vector[InternalModelRecordReference], evidence: RelationshipScopeEvidence)
  final case class Result(dependency: ValidationDependency, policy: InternalModelRecordReference,
    findings: Vector[UseCaseReviewFinding], unresolved: Vector[RelationshipScopeEvidence.Unresolved])
  private[runtime] final case class Phase11Demand(selection: CapabilityQueryModel.DemandSelection,
    result: Either[CapabilityQueryModel.QueryFailure, CapabilityQueryModel.QueryResult[CapabilityQueryModel.DemandDiagnosis]])

  private[runtime] def readCapabilityDemand(session: CapabilityQueries.Session,
    selection: CapabilityQueryModel.DemandSelection): Phase11Demand =
    Phase11Demand(selection, session.diagnoseCapabilityDemand(selection))

  private[runtime] final case class CapabilitySelection(useCase: ComponentDashboardSemanticTargetIdentity,
    selection: CapabilityQueryModel.DemandSelection, references: Vector[InternalModelRecordReference],
    evidence: RelationshipScopeEvidence.Complete)
  private[runtime] final case class CapabilityReview(review: Result, observations: Vector[Phase11Demand],
    demands: Vector[CapabilityDemand])

  /** Caller supplies the explicit Use Case-to-demand selection. Existing Phase 11 owns query meaning. */
  private[runtime] def reviewWithCapabilityDemand(session: ModelEditSession, policy: FindingPolicy,
    scopes: Vector[SubjectScope], resolutions: Vector[ResolutionEvidence], behavior: Vector[BehaviorCoverage],
    capabilitySession: CapabilityQueries.Session, selections: Vector[CapabilitySelection]): Either[String, CapabilityReview] = {
    val observations = selections.map(value => readCapabilityDemand(capabilitySession, value.selection))
    val demands = selections.zip(observations).map { case (selection, observation) =>
      observation.result match {
        case Right(result) if !result.value.gaps.exists {
          case CapabilityQueryModel.CapabilityGap.MissingComponentUsageCapabilityProjection(_, _,
            CapabilityQueryModel.MissingComponentUsageReason.ProjectionNotSupplied, _) => true
          case _ => false
        } => CapabilityDemand(selection.useCase, selection.references, selection.evidence)
        case _ => CapabilityDemand(selection.useCase, selection.references,
          RelationshipScopeEvidence.Unresolved(Subject.UseCase(selection.useCase), ResolutionState.Unavailable))
      }
    }
    review(session, policy, scopes, resolutions, behavior, demands).map(CapabilityReview(_, observations, demands))
  }

  def reviewSource(session: ModelEditSession, policy: FindingPolicy, source: ModelEditSourceProjection.Selection,
    resolutions: Vector[ResolutionEvidence], behavior: Vector[BehaviorCoverage],
    demands: Vector[CapabilityDemand]): Either[String, Result] = {
    val candidate = session.candidate
    val subjects = candidate.goals.map(value => (Subject.Goal(value.reference.semanticTargetId), "goal-use-case")) ++
      candidate.useCases.flatMap(value => Vector((Subject.UseCase(value.semanticTargetId), "goal-use-case"), (Subject.UseCase(value.semanticTargetId), "use-case-slice"))) ++
      candidate.slices.flatMap(value => Vector((Subject.Slice(value.identity), "slice-specification"), (Subject.Slice(value.identity), "slice-ticket")))
    val scopes = subjects.map { case (subject, family) =>
      val evidence = if (source.complete(family)) RelationshipScopeEvidence.Complete(subject, source.sourceArtifact)
      else RelationshipScopeEvidence.Unresolved(subject, if (source.publication.document.coverage.exists(value =>
        value.family == family && value.status == "unsupported")) ResolutionState.Unsupported else ResolutionState.Unavailable)
      SubjectScope(subject, family, evidence)
    }
    ModelEditSourceProjection.validate(source.publication).flatMap(_ => review(session, policy, scopes, resolutions, behavior, demands))
  }

  def review(session: ModelEditSession, policy: FindingPolicy, scopes: Vector[SubjectScope],
    resolutions: Vector[ResolutionEvidence], behavior: Vector[BehaviorCoverage],
    demands: Vector[CapabilityDemand]): Either[String, Result] = {
    val candidate = session.candidate
    val findings = Vector.newBuilder[UseCaseReviewFinding]
    val unresolved = Vector.newBuilder[RelationshipScopeEvidence.Unresolved]
    def _scope_(subject: Subject, family: String): Option[RelationshipScopeEvidence] =
      scopes.find(value => value.subject == subject && value.family == family).map(_.evidence)
    def _finding_(kind: FindingKind, subject: Subject, related: Vector[Subject], evidence: RelationshipScopeEvidence): Unit = evidence match {
      case value: RelationshipScopeEvidence.Unresolved => unresolved += value
      case value: RelationshipScopeEvidence.Complete =>
        findings += UseCaseReviewFinding(kind, subject, related.distinct.sortBy(_.toString), policy.reference, policy.dispositions(kind), Vector(value.source), value)
    }
    def _missing_(kind: FindingKind, subject: Subject, family: String, absent: Boolean): Unit =
      _scope_(subject, family) match {
        case Some(value: RelationshipScopeEvidence.Unresolved) => unresolved += value
        case Some(value) if absent => _finding_(kind, subject, Vector.empty, value)
        case None => unresolved += RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable)
        case _ => ()
      }
    val supplied = scopes.map(value => (value.subject, value.family))
    val evidence = scopes.map(_.evidence) ++ behavior.map(_.evidence) ++ demands.map(_.evidence)
    val dependencies = candidate.artifacts.toSet
    val scopesvalid = scopes.forall(value => value.evidence match {
      case RelationshipScopeEvidence.Complete(subject, _) => subject == value.subject
      case RelationshipScopeEvidence.Unresolved(subject, _) => subject == value.subject
    })
    val knownsubjects: Set[Subject] = (candidate.goals.map(value => Subject.Goal(value.reference.semanticTargetId)) ++
      candidate.useCases.map(value => Subject.UseCase(value.semanticTargetId)) ++ candidate.slices.map(value => Subject.Slice(value.identity))).toSet
    for {
      _ <- Either.cond(policy.dispositions.keySet == FindingKind.values.toSet, (), "project policy must declare every finding disposition")
      _ <- Either.cond(supplied.distinct.size == supplied.size && scopesvalid && scopes.forall(value => knownsubjects(value.subject)) &&
        evidence.forall { case RelationshipScopeEvidence.Complete(_, source) => dependencies(source); case _ => true },
        (), "finding scope must be unique, exact and depend on the selected candidate artifacts")
      _ <- Either.cond(behavior.map(_.useCase).distinct.size == behavior.size && demands.map(_.useCase).distinct.size == demands.size &&
        resolutions.map(_.target).distinct.size == resolutions.size && resolutions.forall(value => dependencies(value.source)) &&
        behavior.forall(value => knownsubjects(Subject.UseCase(value.useCase)) && (value.evidence match {
          case RelationshipScopeEvidence.Complete(subject, _) => subject == Subject.UseCase(value.useCase)
          case RelationshipScopeEvidence.Unresolved(subject, _) => subject == Subject.UseCase(value.useCase)
        })) && demands.forall(value => knownsubjects(Subject.UseCase(value.useCase)) && (value.evidence match {
          case RelationshipScopeEvidence.Complete(subject, _) => subject == Subject.UseCase(value.useCase)
          case RelationshipScopeEvidence.Unresolved(subject, _) => subject == Subject.UseCase(value.useCase)
        })),
        (), "review observations must be unambiguous and reference candidate dependencies")
    } yield {
      candidate.goals.foreach { goal =>
        val identity = goal.reference.semanticTargetId
        _missing_(FindingKind.GoalWithoutUseCase, Subject.Goal(identity), "goal-use-case", !candidate.goalLinks.exists(_.goalIdentity == identity))
      }
      candidate.useCases.foreach { usecase =>
        val identity = usecase.semanticTargetId
        val subject = Subject.UseCase(identity)
        _missing_(FindingKind.UseCaseWithoutGoal, subject, "goal-use-case", !candidate.goalLinks.exists(_.useCaseIdentity == identity))
        _missing_(FindingKind.UseCaseWithoutSlice, subject, "use-case-slice", !candidate.slices.exists(_.useCaseIdentity == identity))
        behavior.find(_.useCase == identity) match {
          case Some(value) if value.uncovered.nonEmpty => _finding_(FindingKind.UncoveredUseCaseBehavior, subject, value.uncovered, value.evidence)
          case Some(value: BehaviorCoverage) => value.evidence match { case unresolvedvalue: RelationshipScopeEvidence.Unresolved => unresolved += unresolvedvalue; case _ => () }
          case None => unresolved += RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable)
        }
        demands.find(_.useCase == identity) match {
          case Some(value) if value.references.isEmpty => _finding_(FindingKind.MissingCapabilityDemandTrace, subject, Vector.empty, value.evidence)
          case Some(value) => value.evidence match { case unresolvedvalue: RelationshipScopeEvidence.Unresolved => unresolved += unresolvedvalue; case _ => () }
          case None => unresolved += RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable)
        }
      }
      candidate.slices.foreach { slice =>
        _missing_(FindingKind.SliceWithoutExecutableSpecification, Subject.Slice(slice.identity), "slice-specification", !candidate.specificationLinks.exists(_.sliceIdentity == slice.identity))
        _missing_(FindingKind.SliceWithoutTicket, Subject.Slice(slice.identity), "slice-ticket", !candidate.ticketLinks.exists(_.sliceIdentity == slice.identity))
      }
      val externals = candidate.specificationLinks.map(value => (ExternalReference.Specification(value.specification), value.reference,
        value.sliceIdentity, FindingKind.DanglingExecutableSpecification)) ++ candidate.ticketLinks.map(value =>
        (ExternalReference.Ticket(value.ticket), value.reference, value.sliceIdentity, FindingKind.DanglingTicket))
      externals.foreach { case (target, reference, slice, kind) =>
        val subject = Subject.Relationship(reference)
        resolutions.find(_.target == target) match {
          case Some(value) if value.state == ResolutionState.Missing =>
            _finding_(kind, subject, Vector(Subject.Slice(slice)), RelationshipScopeEvidence.Complete(subject, value.source))
          case Some(value) if value.state != ResolutionState.Resolved => unresolved += RelationshipScopeEvidence.Unresolved(subject, value.state)
          case None => unresolved += RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable)
          case _ => ()
        }
      }
      Result(ValidationDependency(session.reference, candidate.reference, session.history.map(_.operation.reference), candidate.artifacts),
        policy.reference, findings.result().sortBy(value => (value.subject.toString, value.kind.toString)),
        unresolved.result().distinct.sortBy(value => (value.subject.toString, value.state.toString)))
    }
  }
}
