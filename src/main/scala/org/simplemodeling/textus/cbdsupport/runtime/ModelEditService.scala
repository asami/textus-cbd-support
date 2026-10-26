/**
 * @since   Oct. 26, 2026
 * @version Oct. 26, 2026
 */
package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*

/** Pure application under caller-admitted authorization; owns no persistence or CML writer. */
object ModelEditService {
  def applyOperation(
    session: ModelEditSession,
    operation: Operation,
    successorCandidate: InternalModelRecordReference,
    authorization: Authorization,
    resolutions: Vector[ResolutionEvidence]
  ): Either[ValidationResult, ModelEditSession] = {
    val candidate = session.candidate
    val violations = Vector.newBuilder[Violation]
    if (authorization.session != session.reference || authorization.scope != session.scope ||
      !authorization.permitted.contains(ModelEditSession.commandKind(operation.command)))
      violations += Violation.AuthorizationDenied
    if (successorCandidate.recordId != candidate.reference.recordId ||
      successorCandidate.recordRevision.value <= candidate.reference.recordRevision.value)
      violations += Violation.InvalidSuccessor
    if (session.history.exists(_.operation.reference.recordId == operation.reference.recordId))
      violations += Violation.OperationIdentityOccupied
    operation.command match {
      case Command.AddGoal(_, _, _, None) => violations += Violation.GoalReferenceRequired
      case _ => ()
    }
    val problems = violations.result()
    if (problems.nonEmpty) Left(ValidationResult(problems))
    else ModelEditSession.admit(session, operation, resolutions).map { admitted =>
      val updated = _apply_command(candidate, operation).copy(reference = successorCandidate)
      session.copy(candidate = updated, history = session.history :+ AppliedOperation(operation,
        candidate.reference, successorCandidate, authorization, admitted.resolutionEvidence))
    }
  }

  private def _apply_command(candidate: Candidate, operation: Operation): Candidate = operation.command match {
    case Command.CreateUseCaseSlice(proposed) => candidate.copy(slices = candidate.slices :+ proposed)
    case Command.RefineUseCaseSlice(existing, proposed) =>
      candidate.copy(slices = candidate.slices.map(slice => if (slice.reference == existing) proposed else slice))
    case Command.LinkSliceSpecification(relationship) => candidate.copy(specificationLinks = candidate.specificationLinks :+ relationship)
    case Command.UnlinkSliceSpecification(relationship) => candidate.copy(specificationLinks = candidate.specificationLinks.filterNot(_ == relationship))
    case Command.LinkSliceTicket(relationship) => candidate.copy(ticketLinks = candidate.ticketLinks :+ relationship)
    case Command.UnlinkSliceTicket(relationship) => candidate.copy(ticketLinks = candidate.ticketLinks.filterNot(_ == relationship))
    case Command.AddGoal(actor, _, intent, Some(reference)) =>
      candidate.copy(goals = candidate.goals :+ ActorGoal(reference, Some(actor), intent, operation.attribution))
    case Command.AddGoal(_, _, _, None) => candidate // Rejected before application.
    case Command.RefineGoal(goal, intent) =>
      val existing = candidate.goals.find(_.reference == goal)
      val updated = existing.fold(ActorGoal(goal, None, intent, operation.attribution))(_.copy(intent = intent, attribution = operation.attribution))
      candidate.copy(goals = candidate.goals.filterNot(_.reference == goal) :+ updated)
    case Command.LinkGoalUseCase(relationship) => candidate.copy(goalLinks = candidate.goalLinks :+ relationship)
    case Command.UnlinkGoalUseCase(relationship) => candidate.copy(goalLinks = candidate.goalLinks.filterNot(_ == relationship))
  }
}
