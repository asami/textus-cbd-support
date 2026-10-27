package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
object ModelEditViewProjection {
  enum State { case CanonicalBasis, Candidate }
  final case class Confirmation(state: State, canonicalSource: ModelEditSourceProjection.Source,
    candidate: InternalModelRecordReference, operations: Vector[InternalModelRecordReference], abandoned: Boolean)
  final case class View(confirmation: Confirmation, actorGoals: ActorGoalProjection.View,
    eventStorming: EventStormingProjection.View, traces: Vector[ModelEditSession.UseCaseDevelopmentTrace],
    capabilityDemands: Vector[ModelEditReview.CapabilityDemand])

  def project(session: ModelEditSession, source: ModelEditSourceProjection.Selection,
    capabilityDemands: Map[ComponentDashboardSemanticTargetIdentity, ModelEditReview.CapabilityDemand]): Either[String, View] =
    _project(session, source, source, capabilityDemands)

  def projectConfirmed(session: ModelEditSession, confirmed: ModelEditCmlProjection.Confirmed,
    capabilityDemands: Map[ComponentDashboardSemanticTargetIdentity, ModelEditReview.CapabilityDemand]): Either[String, View] = {
    val plan = confirmed.plan
    for {
      _ <- Either.cond(plan.session == session.reference && plan.candidate == session.candidate.reference &&
        plan.operations == session.history.map(_.operation.reference), (), "View confirmation belongs to a different candidate history")
      _ <- ModelEditCmlProjection.confirm(plan, confirmed.rendering, confirmed.reproduced)
      view <- _project(session, plan.selection, plan.selection.copy(publication = confirmed.reproduced, bindings = plan.proposedBindings), capabilityDemands)
    } yield view
  }

  private def _project(session: ModelEditSession, canonical: ModelEditSourceProjection.Selection,
    source: ModelEditSourceProjection.Selection,
    capabilitydemands: Map[ComponentDashboardSemanticTargetIdentity, ModelEditReview.CapabilityDemand]): Either[String, View] =
    for {
      _ <- Either.cond(session.candidate.artifacts.contains(canonical.sourceArtifact), (), "View must select an exact candidate source dependency")
      _ <- Either.cond(capabilitydemands.forall { case (identity, demand) =>
        identity == demand.useCase && session.candidate.useCases.exists(_.semanticTargetId == identity) && (demand.evidence match {
          case ModelEditSession.RelationshipScopeEvidence.Complete(subject, _) => subject == ModelEditSession.Subject.UseCase(identity)
          case ModelEditSession.RelationshipScopeEvidence.Unresolved(subject, _) => subject == ModelEditSession.Subject.UseCase(identity)
        })
      }, (), "View capability evidence must retain its exact selected Use Case subject")
      actors <- ActorGoalProjection.project(session, source)
      storming <- EventStormingProjection.project(session, source)
    } yield {
      val candidate = session.candidate
      val usecases = candidate.useCases.sortBy(_.semanticTargetId.value)
      val demands = usecases.map { usecase =>
        capabilitydemands.getOrElse(usecase.semanticTargetId, ModelEditReview.CapabilityDemand(usecase.semanticTargetId, Vector.empty,
          ModelEditSession.RelationshipScopeEvidence.Unresolved(ModelEditSession.Subject.UseCase(usecase.semanticTargetId),
            ModelEditSession.ResolutionState.Unavailable)))
      }
      val traces = usecases.zip(demands).map { case (usecase, demand) =>
        val slices = candidate.slices.filter(_.useCaseIdentity == usecase.semanticTargetId).sortBy(_.identity.value)
        val identities = slices.map(_.identity).toSet
        val goals = candidate.goalLinks.filter(_.useCaseIdentity == usecase.semanticTargetId).map(_.goalIdentity).toSet
        ModelEditSession.UseCaseDevelopmentTrace(session.scope, usecase.semanticTargetId,
          candidate.goals.filter(value => goals(value.reference.semanticTargetId)).map(_.reference).sortBy(_.semanticTargetId.value), slices,
          candidate.specificationLinks.filter(value => identities(value.sliceIdentity)).sortBy(_.reference.recordId.value),
          candidate.ticketLinks.filter(value => identities(value.sliceIdentity)).sortBy(_.reference.recordId.value),
          demand.references)
      }
      View(Confirmation(if (session.history.isEmpty) State.CanonicalBasis else State.Candidate,
        canonical.publication.source, candidate.reference, session.history.map(_.operation.reference),
        session.state == ModelEditSession.SessionState.Abandoned), actors, storming, traces, demands)
    }
}
