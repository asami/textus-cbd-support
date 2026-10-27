package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Shared Actors and independent Goals; no edge is inferred from labels. */
object ActorGoalProjection {
  final case class ActorRecord(identity: ComponentDashboardSemanticTargetIdentity, reference: UseCaseCommunicationReference)
  final case class GoalRecord(identity: ComponentDashboardSemanticTargetIdentity, intent: String,
    actors: Vector[ComponentDashboardSemanticTargetIdentity], useCases: Vector[ComponentDashboardSemanticTargetIdentity])
  final case class View(candidate: InternalModelRecordReference, scope: ModelEditSession.Scope,
    actors: Vector[ActorRecord], goals: Vector[GoalRecord], coverage: Vector[ModelEditSourceProjection.Coverage])

  def project(session: ModelEditSession, source: ModelEditSourceProjection.Selection): Either[String, View] = {
    import ModelEditSourceProjection.Endpoint
    val candidate = session.candidate
    val actorreferences = candidate.actors ++ candidate.useCases.flatMap(_.references).filter(_.referenceKind == Actor) ++ candidate.goals.flatMap(_.actor)
    val actors = actorreferences.groupBy(_.semanticTargetId).toVector.sortBy(_._1.value).map { case (identity, references) =>
      ActorRecord(identity, references.sortBy(_.id).head)
    }
    val original = source.publication.document.relationships.filter(_.kind == "actor-goal")
    val edges = original.foldLeft[Either[String, Vector[(ComponentDashboardSemanticTargetIdentity, ComponentDashboardSemanticTargetIdentity)]]](Right(Vector.empty)) {
      (result, edge) => for {
        collected <- result
        pair <- (edge.from, edge.to) match {
          case (Endpoint.Local(from), Endpoint.Local(to)) => for { actor <- source.binding(from); goal <- source.binding(to) } yield (actor.semanticIdentity, goal.semanticIdentity)
          case _ => Left("Actor-Goal source endpoints must be local")
        }
      } yield collected :+ pair
    }
    edges.map { links =>
      val goals = candidate.goals.sortBy(_.reference.semanticTargetId.value).map { goal =>
        val identity = goal.reference.semanticTargetId
        val explicit = links.collect { case (actor, `identity`) => actor } ++ goal.actor.map(_.semanticTargetId)
        GoalRecord(identity, goal.intent, explicit.distinct.sortBy(_.value),
          candidate.goalLinks.filter(_.goalIdentity == identity).map(_.useCaseIdentity).distinct.sortBy(_.value))
      }
      View(candidate.reference, candidate.scope, actors, goals, source.publication.document.coverage)
    }
  }
}
