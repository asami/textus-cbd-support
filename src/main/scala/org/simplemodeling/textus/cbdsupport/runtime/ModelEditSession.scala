package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Immutable candidate and exact declared editing history; no canonical writing. */
final case class ModelEditSession(
  reference: InternalModelRecordReference,
  scope: ModelEditSession.Scope,
  candidate: ModelEditSession.Candidate,
  state: ModelEditSession.SessionState,
  history: Vector[ModelEditSession.AppliedOperation] = Vector.empty,
  reviews: Vector[ModelEditSession.RetainedReview] = Vector.empty
) {
  def abandon: ModelEditSession = copy(state = ModelEditSession.SessionState.Abandoned)
}

object ModelEditSession {
  final case class Scope(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity
  )

  enum SessionState { case Active, Abandoned }
  enum SliceLifecycle { case Proposed, InDevelopment, Implemented, Abandoned }

  final case class ActorGoal(
    reference: UseCaseCommunicationReference,
    actor: Option[UseCaseCommunicationReference],
    intent: String,
    attribution: ComponentDashboardSourceAttribution
  )

  final case class RepositoryProjectReference(
    repository: InternalModelRecordId,
    project: InternalModelRecordId
  )

  final case class SpecificationReference(
    owner: RepositoryProjectReference,
    specificationIdentity: InternalModelRecordId,
    sourceRevision: InternalModelRecordId
  )

  final case class TicketRef(
    provider: InternalModelRecordId,
    owner: RepositoryProjectReference,
    ticketIdentity: InternalModelRecordId
  )

  final case class TicketObservation(
    state: Option[InternalModelRecordId],
    version: Option[InternalModelRecordId]
  )

  final case class SliceField(name: String, datatype: String, multiplicity: String)

  final case class UseCaseSlice(
    reference: InternalModelRecordReference,
    identity: ComponentDashboardSemanticTargetIdentity,
    useCaseIdentity: ComponentDashboardSemanticTargetIdentity,
    title: String,
    intent: String,
    scopeRefs: Vector[InternalModelRecordReference],
    acceptanceRefs: Vector[InternalModelRecordReference],
    lifecycle: SliceLifecycle,
    attribution: ComponentDashboardSourceAttribution,
    fields: Vector[SliceField] = Vector.empty
  )

  final case class SliceSpecificationRelationship(
    reference: InternalModelRecordReference,
    sliceIdentity: ComponentDashboardSemanticTargetIdentity,
    specification: SpecificationReference,
    attribution: ComponentDashboardSourceAttribution
  )

  final case class SliceTicketRelationship(
    reference: InternalModelRecordReference,
    sliceIdentity: ComponentDashboardSemanticTargetIdentity,
    ticket: TicketRef,
    observation: Option[TicketObservation],
    attribution: ComponentDashboardSourceAttribution
  )

  final case class GoalUseCaseRelationship(
    reference: InternalModelRecordReference,
    goalIdentity: ComponentDashboardSemanticTargetIdentity,
    useCaseIdentity: ComponentDashboardSemanticTargetIdentity,
    attribution: ComponentDashboardSourceAttribution
  )

  /** The caller supplies already admitted same-scope basis records, never a name index. */
  final case class Candidate(
    reference: InternalModelRecordReference,
    scope: Scope,
    useCases: Vector[UseCaseCommunicationProjectionSubject],
    slices: Vector[UseCaseSlice],
    specificationLinks: Vector[SliceSpecificationRelationship],
    ticketLinks: Vector[SliceTicketRelationship],
    goalLinks: Vector[GoalUseCaseRelationship],
    artifacts: Vector[InternalModelArtifactReference],
    goals: Vector[ActorGoal] = Vector.empty,
    actors: Vector[UseCaseCommunicationReference] = Vector.empty
  )

  final case class UseCaseDevelopmentTrace(
    scope: Scope,
    useCaseIdentity: ComponentDashboardSemanticTargetIdentity,
    actorGoalRefs: Vector[UseCaseCommunicationReference],
    slices: Vector[UseCaseSlice],
    specificationLinks: Vector[SliceSpecificationRelationship],
    ticketLinks: Vector[SliceTicketRelationship],
    capabilityDemandRefs: Vector[InternalModelRecordReference]
  )

  enum ExternalReference {
    case Specification(reference: SpecificationReference)
    case Ticket(reference: TicketRef)
  }

  enum ResolutionState { case Resolved, Missing, Unavailable, Unauthorized, Redacted, Unsupported }

  final case class ResolutionEvidence(
    target: ExternalReference,
    state: ResolutionState,
    source: InternalModelArtifactReference,
    attribution: ComponentDashboardSourceAttribution
  )

  enum Subject {
    case UseCase(identity: ComponentDashboardSemanticTargetIdentity)
    case Actor(identity: ComponentDashboardSemanticTargetIdentity)
    case Goal(identity: ComponentDashboardSemanticTargetIdentity)
    case Slice(identity: ComponentDashboardSemanticTargetIdentity)
    case Relationship(reference: InternalModelRecordReference)
  }

  enum Command {
    case CreateUseCaseSlice(proposed: UseCaseSlice)
    case RefineUseCaseSlice(existing: InternalModelRecordReference, proposed: UseCaseSlice)
    case LinkSliceSpecification(relationship: SliceSpecificationRelationship)
    case UnlinkSliceSpecification(relationship: SliceSpecificationRelationship)
    case LinkSliceTicket(relationship: SliceTicketRelationship)
    case UnlinkSliceTicket(relationship: SliceTicketRelationship)
    case AddGoal(actor: UseCaseCommunicationReference, proposed: ComponentDashboardSemanticTargetIdentity, intent: String,
      proposedReference: Option[UseCaseCommunicationReference] = None)
    case RefineGoal(goal: UseCaseCommunicationReference, intent: String)
    case LinkGoalUseCase(relationship: GoalUseCaseRelationship)
    case UnlinkGoalUseCase(relationship: GoalUseCaseRelationship)
  }

  final case class Preconditions(
    expectedCandidate: InternalModelRecordReference,
    requiredArtifacts: Vector[InternalModelArtifactReference]
  )

  final case class Operation(
    reference: InternalModelRecordReference,
    sessionReference: InternalModelRecordReference,
    preconditions: Preconditions,
    command: Command,
    attribution: ComponentDashboardSourceAttribution
  )

  /** Intent for a later semantic diff, not a claim of applied before/after state. */
  final case class ProposedEffect(subject: Subject, command: Command)
  final case class AdmittedOperation(
    operation: Operation,
    effect: ProposedEffect,
    resolutionEvidence: Vector[ResolutionEvidence]
  )

  enum CommandKind {
    case CreateUseCaseSlice, RefineUseCaseSlice, LinkSliceSpecification, UnlinkSliceSpecification
    case LinkSliceTicket, UnlinkSliceTicket, AddGoal, RefineGoal, LinkGoalUseCase, UnlinkGoalUseCase
  }
  /** An upstream authorization decision, not authentication or a built-in role policy. */
  final case class Authorization(
    reference: InternalModelRecordReference,
    session: InternalModelRecordReference,
    scope: Scope,
    permitted: Vector[CommandKind]
  )
  final case class AppliedOperation(
    operation: Operation,
    previousCandidate: InternalModelRecordReference,
    successorCandidate: InternalModelRecordReference,
    authorization: Authorization,
    resolutionEvidence: Vector[ResolutionEvidence]
  )

  enum Violation {
    case SessionAbandoned, SessionMismatch, CandidateMismatch, ArtifactDependencyMismatch, ScopeMismatch
    case AuthorizationDenied, InvalidSuccessor, GoalReferenceRequired, InvalidGoalReference, OperationIdentityOccupied
    case UnknownSubject(subject: Subject)
    case IdentityOccupied(subject: Subject)
    case RelationshipMismatch(reference: InternalModelRecordReference)
    case InvalidRefinement(reference: InternalModelRecordReference)
    case InvalidSliceFields(identity: ComponentDashboardSemanticTargetIdentity)
    case ResolutionAbsent(target: ExternalReference)
    case ResolutionAmbiguous(target: ExternalReference)
    case ProvenDangling(target: ExternalReference)
    case ReferenceUnresolved(target: ExternalReference, state: ResolutionState)
  }

  final case class ValidationResult(violations: Vector[Violation])

  final case class ValidationDependency(
    session: InternalModelRecordReference,
    candidate: InternalModelRecordReference,
    operations: Vector[InternalModelRecordReference],
    artifacts: Vector[InternalModelArtifactReference]
  )

  final case class ApprovalDependency(
    validation: ValidationDependency,
    validationReference: InternalModelRecordReference,
    semanticDiffReference: InternalModelRecordReference,
    reviewReference: InternalModelRecordReference
  )

  enum FindingKind {
    case GoalWithoutUseCase, UseCaseWithoutGoal, UseCaseWithoutSlice
    case SliceWithoutExecutableSpecification, SliceWithoutTicket
    case DanglingExecutableSpecification, DanglingTicket
    case UncoveredUseCaseBehavior, MissingCapabilityDemandTrace
  }
  enum Severity { case Information, Warning, Error }
  final case class FindingDisposition(severity: Severity, blocking: Boolean)
  final case class FindingPolicy(
    reference: InternalModelRecordReference,
    dispositions: Map[FindingKind, FindingDisposition]
  )
  enum RelationshipScopeEvidence {
    case Complete(subject: Subject, source: InternalModelArtifactReference)
    case Unresolved(subject: Subject, state: ResolutionState)
  }
  final case class UseCaseReviewFinding(
    kind: FindingKind,
    subject: Subject,
    relatedRefs: Vector[Subject],
    policy: InternalModelRecordReference,
    disposition: FindingDisposition,
    evidenceRefs: Vector[InternalModelArtifactReference],
    relationshipScope: RelationshipScopeEvidence
  )

  final case class RetainedReview(dependency: ApprovalDependency, findings: Vector[UseCaseReviewFinding])

  def commandKind(command: Command): CommandKind = command match {
    case Command.CreateUseCaseSlice(_) => CommandKind.CreateUseCaseSlice
    case Command.RefineUseCaseSlice(_, _) => CommandKind.RefineUseCaseSlice
    case Command.LinkSliceSpecification(_) => CommandKind.LinkSliceSpecification
    case Command.UnlinkSliceSpecification(_) => CommandKind.UnlinkSliceSpecification
    case Command.LinkSliceTicket(_) => CommandKind.LinkSliceTicket
    case Command.UnlinkSliceTicket(_) => CommandKind.UnlinkSliceTicket
    case Command.AddGoal(_, _, _, _) => CommandKind.AddGoal
    case Command.RefineGoal(_, _) => CommandKind.RefineGoal
    case Command.LinkGoalUseCase(_) => CommandKind.LinkGoalUseCase
    case Command.UnlinkGoalUseCase(_) => CommandKind.UnlinkGoalUseCase
  }

  def reviewApplies(session: ModelEditSession, review: RetainedReview): Boolean = {
    val dependency = review.dependency.validation
    session.state == SessionState.Active && dependency.session == session.reference &&
      dependency.candidate == session.candidate.reference && dependency.operations == session.history.map(_.operation.reference) &&
      dependency.artifacts == session.candidate.artifacts
  }

  def admit(
    session: ModelEditSession,
    operation: Operation,
    resolutions: Vector[ResolutionEvidence]
  ): Either[ValidationResult, AdmittedOperation] = {
    val candidate = session.candidate
    val violations = Vector.newBuilder[Violation]
    if (session.state == SessionState.Abandoned) violations += Violation.SessionAbandoned
    if (operation.sessionReference != session.reference) violations += Violation.SessionMismatch
    if (operation.preconditions.expectedCandidate != candidate.reference) violations += Violation.CandidateMismatch
    if (!operation.preconditions.requiredArtifacts.forall(candidate.artifacts.contains))
      violations += Violation.ArtifactDependencyMismatch
    if (candidate.scope != session.scope || !candidate.useCases.forall(_.component == session.scope.component))
      violations += Violation.ScopeMismatch
    val subject = _command_subject(operation.command)
    violations ++= _command_violations(candidate, operation.command, resolutions, operation.preconditions.requiredArtifacts)
    val result = violations.result()
    if (result.nonEmpty) Left(ValidationResult(result))
    else Right(AdmittedOperation(operation, ProposedEffect(subject, operation.command), _consumed_resolutions(operation.command, resolutions)))
  }

  private def _command_subject(command: Command): Subject = command match {
    case Command.CreateUseCaseSlice(proposed) => Subject.Slice(proposed.identity)
    case Command.RefineUseCaseSlice(_, proposed) => Subject.Slice(proposed.identity)
    case Command.LinkSliceSpecification(relationship) => Subject.Relationship(relationship.reference)
    case Command.UnlinkSliceSpecification(relationship) => Subject.Relationship(relationship.reference)
    case Command.LinkSliceTicket(relationship) => Subject.Relationship(relationship.reference)
    case Command.UnlinkSliceTicket(relationship) => Subject.Relationship(relationship.reference)
    case Command.AddGoal(_, proposed, _, _) => Subject.Goal(proposed)
    case Command.RefineGoal(goal, _) => Subject.Goal(goal.semanticTargetId)
    case Command.LinkGoalUseCase(relationship) => Subject.Relationship(relationship.reference)
    case Command.UnlinkGoalUseCase(relationship) => Subject.Relationship(relationship.reference)
  }

  private def _command_violations(
    candidate: Candidate,
    command: Command,
    resolutions: Vector[ResolutionEvidence],
    artifacts: Vector[InternalModelArtifactReference]
  ): Vector[Violation] = {
    def _use_case_(identity: ComponentDashboardSemanticTargetIdentity): Vector[Violation] =
      _require(candidate.useCases.exists(_.semanticTargetId == identity), Violation.UnknownSubject(Subject.UseCase(identity)))
    def _slice_(identity: ComponentDashboardSemanticTargetIdentity): Vector[Violation] =
      _require(candidate.slices.exists(_.identity == identity), Violation.UnknownSubject(Subject.Slice(identity)))
    def _reference_(reference: UseCaseCommunicationReference, kind: UseCaseCommunicationReferenceKind): Vector[Violation] =
      _require(reference.referenceKind == kind && (candidate.actors.contains(reference) || candidate.useCases.exists(_.references.contains(reference)) ||
        (kind == Goal && candidate.goals.exists(_.reference == reference))),
        Violation.UnknownSubject(if (kind == Actor) Subject.Actor(reference.semanticTargetId) else Subject.Goal(reference.semanticTargetId)))
    def _new_edge_(reference: InternalModelRecordReference): Vector[Violation] =
      _require(!candidate.specificationLinks.exists(_.reference.recordId == reference.recordId) &&
        !candidate.ticketLinks.exists(_.reference.recordId == reference.recordId) &&
        !candidate.goalLinks.exists(_.reference.recordId == reference.recordId),
        Violation.IdentityOccupied(Subject.Relationship(reference)))

    def _slice_fields_(slice: UseCaseSlice): Vector[Violation] =
      _require(slice.fields.map(_.name).distinct.size == slice.fields.size && slice.fields.forall(field =>
        field.name.trim.nonEmpty && field.datatype.trim.nonEmpty && Set("1", "?", "*", "+")(field.multiplicity)), Violation.InvalidSliceFields(slice.identity))

    command match {
      case Command.CreateUseCaseSlice(proposed) =>
        _slice_fields_(proposed) ++ _use_case_(proposed.useCaseIdentity) ++
          _require(!_occupied(candidate, proposed.identity) && !candidate.slices.exists(_.reference.recordId == proposed.reference.recordId),
            Violation.IdentityOccupied(Subject.Slice(proposed.identity)))
      case Command.RefineUseCaseSlice(existing, proposed) =>
        _slice_fields_(proposed) ++ _require(candidate.slices.exists(slice => slice.reference == existing && slice.identity == proposed.identity &&
          slice.useCaseIdentity == proposed.useCaseIdentity) && proposed.reference.recordId == existing.recordId &&
          proposed.reference.recordRevision.value > existing.recordRevision.value,
          Violation.InvalidRefinement(existing))
      case Command.LinkSliceSpecification(relationship) =>
        _slice_(relationship.sliceIdentity) ++ _new_edge_(relationship.reference) ++
          _resolve(ExternalReference.Specification(relationship.specification), resolutions, artifacts)
      case Command.UnlinkSliceSpecification(relationship) =>
        _require(candidate.specificationLinks.contains(relationship), Violation.RelationshipMismatch(relationship.reference))
      case Command.LinkSliceTicket(relationship) =>
        _slice_(relationship.sliceIdentity) ++ _new_edge_(relationship.reference) ++
          _resolve(ExternalReference.Ticket(relationship.ticket), resolutions, artifacts)
      case Command.UnlinkSliceTicket(relationship) =>
        _require(candidate.ticketLinks.contains(relationship), Violation.RelationshipMismatch(relationship.reference))
      case Command.AddGoal(actor, proposed, _, proposedreference) =>
        _reference_(actor, Actor) ++ _require(!_occupied(candidate, proposed), Violation.IdentityOccupied(Subject.Goal(proposed))) ++
          _require(proposedreference.forall(reference => reference.referenceKind == Goal &&
            reference.semanticTargetId == proposed && reference.component == candidate.scope.component &&
            reference.id.trim.nonEmpty && !candidate.actors.exists(existing => existing.id == reference.id ||
              existing.semanticRelationshipId == reference.semanticRelationshipId) && !candidate.useCases.flatMap(_.references).exists(existing =>
              existing.id == reference.id || existing.semanticRelationshipId == reference.semanticRelationshipId) &&
            !candidate.goals.exists(existing => existing.reference.id == reference.id ||
              existing.reference.semanticRelationshipId == reference.semanticRelationshipId)), Violation.InvalidGoalReference)
      case Command.RefineGoal(goal, _) => _reference_(goal, Goal)
      case Command.LinkGoalUseCase(relationship) =>
        _use_case_(relationship.useCaseIdentity) ++ _new_edge_(relationship.reference) ++
          _require(candidate.goals.exists(_.reference.semanticTargetId == relationship.goalIdentity) ||
            candidate.useCases.flatMap(_.references).exists(reference => reference.referenceKind == Goal &&
            reference.semanticTargetId == relationship.goalIdentity), Violation.UnknownSubject(Subject.Goal(relationship.goalIdentity)))
      case Command.UnlinkGoalUseCase(relationship) =>
        _require(candidate.goalLinks.contains(relationship), Violation.RelationshipMismatch(relationship.reference))
    }
  }

  private def _occupied(candidate: Candidate, identity: ComponentDashboardSemanticTargetIdentity): Boolean =
    candidate.goals.exists(_.reference.semanticTargetId == identity) ||
      candidate.slices.exists(_.identity == identity) || candidate.useCases.exists(_.semanticTargetId == identity) ||
      candidate.actors.exists(_.semanticTargetId == identity) || candidate.useCases.flatMap(_.references).exists(_.semanticTargetId == identity)

  private def _consumed_resolutions(command: Command, resolutions: Vector[ResolutionEvidence]): Vector[ResolutionEvidence] =
    command match {
      case Command.LinkSliceSpecification(relationship) =>
        resolutions.filter(_.target == ExternalReference.Specification(relationship.specification))
      case Command.LinkSliceTicket(relationship) =>
        resolutions.filter(_.target == ExternalReference.Ticket(relationship.ticket))
      case _ => Vector.empty
    }

  private def _resolve(
    target: ExternalReference,
    resolutions: Vector[ResolutionEvidence],
    artifacts: Vector[InternalModelArtifactReference]
  ): Vector[Violation] = resolutions.filter(_.target == target) match {
    case Vector() => Vector(Violation.ResolutionAbsent(target))
    case Vector(evidence) if !artifacts.contains(evidence.source) => Vector(Violation.ArtifactDependencyMismatch)
    case Vector(evidence) => evidence.state match {
      case ResolutionState.Resolved => Vector.empty
      case ResolutionState.Missing => Vector(Violation.ProvenDangling(target))
      case state => Vector(Violation.ReferenceUnresolved(target, state))
    }
    case _ => Vector(Violation.ResolutionAmbiguous(target))
  }

  private def _require(condition: Boolean, violation: Violation): Vector[Violation] =
    if (condition) Vector.empty else Vector(violation)
}
