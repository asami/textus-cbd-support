package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Contextual client metadata; authorization and mutation retain their existing owners. */
object ModelEditPalette {
  enum Surface { case ActorGoal, MonoKoto, EventStorming, UseCaseDetail }
  final case class Context(surface: Surface, scope: Scope, subject: Subject)
  final case class Entry(kind: CommandKind, label: String, inputs: Vector[String])
  final case class View(session: InternalModelRecordReference, candidate: InternalModelRecordReference,
    context: Context, authorization: InternalModelRecordReference, entries: Vector[Entry])
  enum Failure {
    case SessionInactive, ScopeMismatch, AuthorizationMismatch, ProjectionMismatch
    case UnknownSubject(subject: Subject)
    case CommandUnavailable(kind: CommandKind)
    case SubjectMismatch(subject: Subject)
  }

  def inspect(session: ModelEditSession, context: Context, authorization: Authorization): Either[Failure, View] =
    for {
      _ <- Either.cond(session.state == SessionState.Active, (), Failure.SessionInactive)
      _ <- Either.cond(context.scope == session.scope && session.candidate.scope == session.scope, (), Failure.ScopeMismatch)
      _ <- Either.cond(authorization.session == session.reference && authorization.scope == session.scope, (), Failure.AuthorizationMismatch)
      _ <- Either.cond(_known(session.candidate, context.subject), (), Failure.UnknownSubject(context.subject))
    } yield {
      val applicable = _kinds(session.candidate, context.subject)
      val entries = CommandKind.values.toVector.filter(kind => applicable(kind) && authorization.permitted.contains(kind)).map(_entry)
      View(session.reference, session.candidate.reference, context, authorization.reference, entries)
    }

  def monoKotoContext(session: ModelEditSession, projection: MonoKotoProjection, subject: Subject): Either[Failure, Context] = {
    val identities = projection.subjects.flatMap(value => value.sourceSubject.semanticTargetId +:
      value.references.map(_.sourceReference.semanticTargetId)).toSet
    Either.cond(projection.context == session.scope.context && projection.component == session.scope.component &&
      _identity(subject).exists(identities.contains) && _known(session.candidate, subject),
      Context(Surface.MonoKoto, session.scope, subject), Failure.ProjectionMismatch)
  }

  def eventStormingContext(session: ModelEditSession, projection: EventStormingProjection.View, subject: Subject): Either[Failure, Context] =
    Either.cond(projection.candidate == session.candidate.reference && projection.scope == session.scope &&
      _identity(subject).exists(identity => projection.nodes.exists(_.identity == identity)) && _known(session.candidate, subject),
      Context(Surface.EventStorming, session.scope, subject), Failure.ProjectionMismatch)

  private def _identity(subject: Subject): Option[ComponentDashboardSemanticTargetIdentity] = subject match {
    case Subject.Actor(identity) => Some(identity)
    case Subject.Goal(identity) => Some(identity)
    case Subject.UseCase(identity) => Some(identity)
    case Subject.Slice(identity) => Some(identity)
    case _: Subject.Relationship => None
  }

  private def _known(candidate: Candidate, subject: Subject): Boolean = subject match {
    case Subject.Actor(identity) => (candidate.actors ++ candidate.useCases.flatMap(_.references))
      .exists(value => value.referenceKind == Actor && value.semanticTargetId == identity)
    case Subject.Goal(identity) => candidate.goals.exists(_.reference.semanticTargetId == identity) ||
      candidate.useCases.flatMap(_.references).exists(value => value.referenceKind == Goal && value.semanticTargetId == identity)
    case Subject.UseCase(identity) => candidate.useCases.exists(_.semanticTargetId == identity)
    case Subject.Slice(identity) => candidate.slices.exists(_.identity == identity)
    case Subject.Relationship(reference) => candidate.specificationLinks.exists(_.reference == reference) ||
      candidate.ticketLinks.exists(_.reference == reference) || candidate.goalLinks.exists(_.reference == reference)
  }

  private def _kinds(candidate: Candidate, subject: Subject): Set[CommandKind] = {
    import CommandKind.*
    def _slice_kinds_(identities: Set[ComponentDashboardSemanticTargetIdentity]): Set[CommandKind] =
      (if (identities.nonEmpty) Set(RefineUseCaseSlice, LinkSliceSpecification, LinkSliceTicket) else Set.empty[CommandKind]) ++
        (if (candidate.specificationLinks.exists(value => identities(value.sliceIdentity))) Set(UnlinkSliceSpecification) else Set.empty[CommandKind]) ++
        (if (candidate.ticketLinks.exists(value => identities(value.sliceIdentity))) Set(UnlinkSliceTicket) else Set.empty[CommandKind])
    subject match {
      case _: Subject.Actor => Set(AddGoal)
      case Subject.Goal(identity) => Set(RefineGoal) ++
        (if (candidate.useCases.nonEmpty) Set(LinkGoalUseCase) else Set.empty[CommandKind]) ++
        (if (candidate.goalLinks.exists(_.goalIdentity == identity)) Set(UnlinkGoalUseCase) else Set.empty[CommandKind])
      case Subject.UseCase(identity) => Set(CreateUseCaseSlice) ++ _slice_kinds_(candidate.slices.filter(_.useCaseIdentity == identity).map(_.identity).toSet) ++
        (if (candidate.goals.nonEmpty || candidate.useCases.flatMap(_.references).exists(_.referenceKind == Goal)) Set(LinkGoalUseCase) else Set.empty[CommandKind]) ++
        (if (candidate.goalLinks.exists(_.useCaseIdentity == identity)) Set(UnlinkGoalUseCase) else Set.empty[CommandKind])
      case Subject.Slice(identity) => _slice_kinds_(Set(identity))
      case Subject.Relationship(reference) =>
        (if (candidate.specificationLinks.exists(_.reference == reference)) Set(UnlinkSliceSpecification) else Set.empty[CommandKind]) ++
          (if (candidate.ticketLinks.exists(_.reference == reference)) Set(UnlinkSliceTicket) else Set.empty[CommandKind]) ++
          (if (candidate.goalLinks.exists(_.reference == reference)) Set(UnlinkGoalUseCase) else Set.empty[CommandKind])
    }
  }

  private def _entry(kind: CommandKind): Entry = {
    val (label, inputs) = kind match {
      case CommandKind.AddGoal => ("Add Goal", Vector("Explicit Goal reference", "Intent"))
      case CommandKind.RefineGoal => ("Refine Goal", Vector("Goal reference", "Intent"))
      case CommandKind.LinkGoalUseCase => ("Link Goal to Use Case", Vector("Goal reference", "Use Case reference", "Relationship reference"))
      case CommandKind.UnlinkGoalUseCase => ("Unlink Goal from Use Case", Vector("Exact relationship"))
      case CommandKind.CreateUseCaseSlice => ("Create Slice", Vector("Explicit Slice identity and record", "Use Case reference", "Slice details"))
      case CommandKind.RefineUseCaseSlice => ("Refine Slice", Vector("Current Slice record", "Successor Slice record and details"))
      case CommandKind.LinkSliceSpecification => ("Link specification", Vector("Slice reference", "Exact specification and source revision", "Relationship reference"))
      case CommandKind.UnlinkSliceSpecification => ("Unlink specification", Vector("Exact relationship"))
      case CommandKind.LinkSliceTicket => ("Link ticket reference", Vector("Slice reference", "Exact provider, owner and ticket identity", "Relationship reference"))
      case CommandKind.UnlinkSliceTicket => ("Unlink ticket reference", Vector("Exact relationship"))
    }
    Entry(kind, label, inputs)
  }
}
