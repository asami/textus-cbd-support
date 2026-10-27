package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
object EventStormingProjection {
  final case class Node(identity: ComponentDashboardSemanticTargetIdentity, kind: String, title: String,
    intent: Option[String], fields: Vector[ModelEditSourceProjection.Field])
  final case class Edge(reference: ModelEditSourceProjection.RecordRef, kind: String,
    from: ModelEditSourceProjection.Endpoint, to: ModelEditSourceProjection.Endpoint)
  final case class View(candidate: InternalModelRecordReference, scope: ModelEditSession.Scope,
    nodes: Vector[Node], edges: Vector[Edge], coverage: Vector[ModelEditSourceProjection.Coverage],
    limitations: Vector[ModelEditSourceProjection.Limitation])

  def project(session: ModelEditSession, source: ModelEditSourceProjection.Selection): Either[String, View] = {
    val document = source.publication.document
    val base = document.elements.filterNot(value => Set("goal", "slice")(value.kind))
    val nodes = base.foldLeft[Either[String, Vector[Node]]](Right(Vector.empty)) { (result, element) =>
      for { collected <- result; binding <- source.binding(element.ref) }
      yield collected :+ Node(binding.semanticIdentity, element.kind, element.title, element.intent, element.fields)
    }
    nodes.map { original =>
      val goals = session.candidate.goals.map { value =>
        val declared = document.elements.find(element => source.binding(element.ref).exists(_.semanticIdentity == value.reference.semanticTargetId))
        Node(value.reference.semanticTargetId, "goal", declared.map(_.title).getOrElse(value.reference.id), Some(value.intent), declared.toVector.flatMap(_.fields))
      }
      val slices = session.candidate.slices.map { value =>
        val fields = value.fields.map(field => ModelEditSourceProjection.Field(field.name, field.datatype, field.multiplicity))
        Node(value.identity, "slice", value.title, Some(value.intent), fields)
      }
      val edges = document.relationships.filter(value => Set("cause", "reaction", "dependency", "context")(value.kind))
        .sortBy(value => (value.ref.owner, value.ref.identity, value.ref.revision))
        .map(value => Edge(value.ref, value.kind, value.from, value.to))
      View(session.candidate.reference, session.scope, (original ++ goals ++ slices).sortBy(value => (value.kind, value.identity.value)),
        edges, document.coverage, source.publication.limitations)
    }
  }
}
