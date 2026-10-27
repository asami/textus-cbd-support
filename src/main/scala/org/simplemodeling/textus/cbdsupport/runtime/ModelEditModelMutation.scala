package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Direct editing adapter; no source writer, identity allocator or external client. */
object ModelEditModelMutation {
  enum Failure {
    case Palette(value: ModelEditPalette.Failure)
    case Validation(value: ValidationResult)
    case Projection(detail: String)
  }
  final case class Applied(session: ModelEditSession, context: ModelEditPalette.Context,
    operation: Operation, confirmation: ModelEditViewProjection.View)

  def applyOperation(session: ModelEditSession, context: ModelEditPalette.Context,
    operation: Operation, successorCandidate: InternalModelRecordReference, authorization: Authorization,
    resolutions: Vector[ResolutionEvidence], source: ModelEditSourceProjection.Selection,
    capabilityDemands: Map[ComponentDashboardSemanticTargetIdentity, ModelEditReview.CapabilityDemand]): Either[Failure, Applied] =
    for {
      palette <- ModelEditPalette.inspect(session, context, authorization).left.map(Failure.Palette(_))
      kind = ModelEditSession.commandKind(operation.command)
      _ <- Either.cond(palette.entries.exists(_.kind == kind), (), Failure.Palette(ModelEditPalette.Failure.CommandUnavailable(kind)))
      _ <- Either.cond(matchesSubject(session.candidate, context.subject, operation.command), (),
        Failure.Palette(ModelEditPalette.Failure.SubjectMismatch(context.subject)))
      updated <- ModelEditService.applyOperation(session, operation, successorCandidate, authorization, resolutions).left.map(Failure.Validation(_))
      view <- ModelEditViewProjection.project(updated, source, capabilityDemands).left.map(Failure.Projection(_))
    } yield Applied(updated, context, operation, view)

  /** Shared read-only contextual predicate; it supplies no mutation or authorization. */
  def matchesSubject(candidate: Candidate, subject: Subject, command: Command): Boolean = {
    def _slice_(identity: ComponentDashboardSemanticTargetIdentity): Boolean = subject match {
      case Subject.Slice(selected) => selected == identity
      case Subject.UseCase(selected) => candidate.slices.exists(value => value.identity == identity && value.useCaseIdentity == selected)
      case _ => false
    }
    def _edge_(reference: InternalModelRecordReference, slice: ComponentDashboardSemanticTargetIdentity): Boolean = subject match {
      case Subject.Relationship(selected) => selected == reference
      case _ => _slice_(slice)
    }
    def _goal_edge_(edge: GoalUseCaseRelationship): Boolean = subject match {
      case Subject.Goal(selected) => selected == edge.goalIdentity
      case Subject.UseCase(selected) => selected == edge.useCaseIdentity
      case Subject.Relationship(selected) => selected == edge.reference
      case _ => false
    }
    command match {
      case Command.AddGoal(actor, _, _, _) => subject == Subject.Actor(actor.semanticTargetId)
      case Command.RefineGoal(goal, _) => subject == Subject.Goal(goal.semanticTargetId)
      case Command.LinkGoalUseCase(edge) => _goal_edge_(edge)
      case Command.UnlinkGoalUseCase(edge) => _goal_edge_(edge)
      case Command.CreateUseCaseSlice(proposed) => subject == Subject.UseCase(proposed.useCaseIdentity)
      case Command.RefineUseCaseSlice(_, proposed) => _slice_(proposed.identity)
      case Command.LinkSliceSpecification(edge) => _slice_(edge.sliceIdentity)
      case Command.UnlinkSliceSpecification(edge) => _edge_(edge.reference, edge.sliceIdentity)
      case Command.LinkSliceTicket(edge) => _slice_(edge.sliceIdentity)
      case Command.UnlinkSliceTicket(edge) => _edge_(edge.reference, edge.sliceIdentity)
    }
  }
}
