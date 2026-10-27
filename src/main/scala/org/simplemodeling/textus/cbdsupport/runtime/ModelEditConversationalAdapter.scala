package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Client-interpreted intent with host-owned authority and bounded reference evidence. */
object ModelEditConversationalAdapter {
  enum Intent {
    case AddGoal(proposed: UseCaseCommunicationReference, intent: String, actorReference: Option[UseCaseCommunicationReference] = None)
    case RefineGoal(intent: String, goalReference: Option[UseCaseCommunicationReference] = None)
    case CreateSlice(proposed: UseCaseSlice)
    case RefineSlice(proposed: UseCaseSlice)
    case LinkGoalUseCase(relationship: GoalUseCaseRelationship)
    case LinkSpecification(relationship: SliceSpecificationRelationship)
    case LinkTicket(relationship: SliceTicketRelationship)
    case UnlinkRelationship(reference: InternalModelRecordReference)
    case Clarify(detail: String)
  }
  final case class Request(text: String, intent: Intent)
  enum Reason {
    case Clarification(detail: String)
    case Admission(violation: Violation)
  }
  final case class UnresolvedIntent(request: Request, context: ModelEditPalette.Context, reasons: Vector[Reason])
  final case class Proposal(request: Request, context: ModelEditPalette.Context, operation: Operation,
    successorCandidate: InternalModelRecordReference)
  enum Failure {
    case Unresolved(value: UnresolvedIntent)
    case Palette(value: ModelEditPalette.Failure)
    case Mutation(value: ModelEditModelMutation.Failure)
  }

  def propose(session: ModelEditSession, context: ModelEditPalette.Context, request: Request,
    operationReference: InternalModelRecordReference, successorCandidate: InternalModelRecordReference,
    attribution: ComponentDashboardSourceAttribution, authorization: Authorization,
    resolutions: Vector[ResolutionEvidence]): Either[Failure, Proposal] =
    for {
      palette <- ModelEditPalette.inspect(session, context, authorization).left.map {
        case ModelEditPalette.Failure.UnknownSubject(subject) => _unresolved(request, context, Reason.Admission(Violation.UnknownSubject(subject)))
        case value => Failure.Palette(value)
      }
      _ <- Either.cond(request.text.trim.nonEmpty, (), _unresolved(request, context, Reason.Clarification("Original conversational text is required.")))
      command <- _translate(session.candidate, context, request)
      kind = ModelEditSession.commandKind(command)
      _ <- Either.cond(palette.entries.exists(_.kind == kind), (), Failure.Palette(ModelEditPalette.Failure.CommandUnavailable(kind)))
      _ <- Either.cond(ModelEditModelMutation.matchesSubject(session.candidate, context.subject, command), (),
        Failure.Palette(ModelEditPalette.Failure.SubjectMismatch(context.subject)))
      operation = Operation(operationReference, session.reference, Preconditions(session.candidate.reference, session.candidate.artifacts), command, attribution)
      _ <- ModelEditSession.admit(session, operation, resolutions).left.map(value =>
        Failure.Unresolved(UnresolvedIntent(request, context, value.violations.map(Reason.Admission(_)))))
    } yield Proposal(request, context, operation, successorCandidate)

  def applyProposal(session: ModelEditSession, proposal: Proposal, authorization: Authorization,
    resolutions: Vector[ResolutionEvidence], source: ModelEditSourceProjection.Selection,
    capabilityDemands: Map[ComponentDashboardSemanticTargetIdentity, ModelEditReview.CapabilityDemand]): Either[Failure, ModelEditModelMutation.Applied] =
    ModelEditModelMutation.applyOperation(session, proposal.context, proposal.operation, proposal.successorCandidate,
      authorization, resolutions, source, capabilityDemands).left.map(Failure.Mutation(_))

  private def _translate(candidate: Candidate, context: ModelEditPalette.Context, request: Request): Either[Failure, Command] = {
    def _unknown_(subject: Subject): Failure = _unresolved(request, context, Reason.Admission(Violation.UnknownSubject(subject)))
    def _reference_(references: Vector[UseCaseCommunicationReference], explicit: Option[UseCaseCommunicationReference]): Either[Failure, UseCaseCommunicationReference] =
      explicit match {
        case Some(reference) => Either.cond(references.contains(reference), reference, _unknown_(context.subject))
        case None => references.distinct match {
          case Vector(reference) => Right(reference)
          case Vector() => Left(_unknown_(context.subject))
          case _ => Left(_unresolved(request, context, Reason.Clarification("Select one exact existing communication reference for this subject.")))
        }
      }
    request.intent match {
      case Intent.Clarify(detail) => Left(_unresolved(request, context, Reason.Clarification(detail)))
      case Intent.AddGoal(proposed, intent, explicit) => context.subject match {
        case Subject.Actor(identity) => _reference_((candidate.actors ++ candidate.useCases.flatMap(_.references))
          .filter(value => value.referenceKind == Actor && value.semanticTargetId == identity), explicit)
          .map(actor => Command.AddGoal(actor, proposed.semanticTargetId, intent, Some(proposed)))
        case _ => Left(_unresolved(request, context, Reason.Clarification("Select the exact existing Actor for the new Goal.")))
      }
      case Intent.RefineGoal(intent, explicit) => context.subject match {
        case Subject.Goal(identity) => _reference_((candidate.goals.map(_.reference) ++ candidate.useCases.flatMap(_.references))
          .filter(value => value.referenceKind == Goal && value.semanticTargetId == identity), explicit)
          .map(goal => Command.RefineGoal(goal, intent))
        case _ => Left(_unresolved(request, context, Reason.Clarification("Select the exact existing Goal to refine.")))
      }
      case Intent.CreateSlice(proposed) => Right(Command.CreateUseCaseSlice(proposed))
      case Intent.RefineSlice(proposed) => candidate.slices.find(_.identity == proposed.identity)
        .toRight(_unknown_(Subject.Slice(proposed.identity))).map(existing => Command.RefineUseCaseSlice(existing.reference, proposed))
      case Intent.LinkGoalUseCase(relationship) => Right(Command.LinkGoalUseCase(relationship))
      case Intent.LinkSpecification(relationship) => Right(Command.LinkSliceSpecification(relationship))
      case Intent.LinkTicket(relationship) => Right(Command.LinkSliceTicket(relationship))
      case Intent.UnlinkRelationship(reference) =>
        candidate.specificationLinks.find(_.reference == reference).map(value => Command.UnlinkSliceSpecification(value))
          .orElse(candidate.ticketLinks.find(_.reference == reference).map(value => Command.UnlinkSliceTicket(value)))
          .orElse(candidate.goalLinks.find(_.reference == reference).map(value => Command.UnlinkGoalUseCase(value)))
          .toRight(_unresolved(request, context, Reason.Admission(Violation.RelationshipMismatch(reference))))
    }
  }

  private def _unresolved(request: Request, context: ModelEditPalette.Context, reason: Reason): Failure =
    Failure.Unresolved(UnresolvedIntent(request, context, Vector(reason)))
}
