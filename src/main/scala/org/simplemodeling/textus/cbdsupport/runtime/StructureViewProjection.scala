package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait StructureViewProjectionRole

sealed trait StructureViewRelationRole extends StructureViewProjectionRole

case object StructureComposition extends StructureViewRelationRole

case object StructureAggregation extends StructureViewRelationRole

case object StructureAssociation extends StructureViewRelationRole

case object StructureContainment extends StructureViewRelationRole

case object StructureOwnership extends StructureViewRelationRole

sealed trait StructureViewAssertionRole extends StructureViewProjectionRole

case object IndependentExistence extends StructureViewAssertionRole

case object Reassignment extends StructureViewAssertionRole

case object DeletionLifecycle extends StructureViewAssertionRole

case object Cardinality extends StructureViewAssertionRole

case object Navigability extends StructureViewAssertionRole

final case class StructureViewAssertion(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  affectedEndpointId: Option[ComponentDashboardSemanticTargetIdentity],
  role: StructureViewProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StructureViewRelation(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  sourceEndpointId: ComponentDashboardSemanticTargetIdentity,
  targetEndpointId: ComponentDashboardSemanticTargetIdentity,
  role: StructureViewProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  assertions: Vector[StructureViewAssertion],
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StructureViewGap(
  id: String,
  component: ComponentDashboardComponentIdentity,
  requestedRole: StructureViewProjectionRole,
  boundedFieldOrScope: String,
  affectedSemanticRelationshipId: Option[ComponentDashboardSemanticTargetIdentity],
  affectedEndpointId: Option[ComponentDashboardSemanticTargetIdentity],
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StructureViewProjectedAssertion(
  sourceAssertion: StructureViewAssertion,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StructureViewProjectedRelation(
  sourceRelation: StructureViewRelation,
  assertions: Vector[StructureViewProjectedAssertion],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StructureViewProjectedGap(
  sourceGap: StructureViewGap,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StructureViewProjectionFailure(violations: Vector[String])

final case class StructureViewProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  relations: Vector[StructureViewProjectedRelation],
  gaps: Vector[StructureViewProjectedGap]
)

object StructureViewProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    relations: Vector[StructureViewRelation],
    gaps: Vector[StructureViewGap]
  ): Either[StructureViewProjectionFailure, StructureViewProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_relation_ids(relations, violations)
    _validate_duplicate_relationship_ids(relations, violations)
    relations.foreach(relation => _validate_relation(relation, component, violations))
    _validate_duplicate_assertion_ids(relations, violations)
    _validate_duplicate_gap_ids(gaps, violations)
    gaps.foreach(gap => _validate_gap(gap, component, relations, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(StructureViewProjectionFailure(result))
    else
      Right(
        StructureViewProjection(
          context,
          component,
          _order_relations(relations).map(_project_relation),
          _order_gaps(gaps).map(gap => _project_gap(gap, relations))
        )
      )
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Structure View Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Structure View Projection Component identity", violations)

  private def _validate_duplicate_relation_ids(
    relations: Vector[StructureViewRelation],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(relations.map(_.id)).foreach { id =>
      violations += s"Duplicate Structure View relation identity '$id'."
    }

  private def _validate_duplicate_relationship_ids(
    relations: Vector[StructureViewRelation],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(relations.map(_.semanticRelationshipId.value)).foreach { id =>
      violations += s"Duplicate Structure View semantic relationship identity '$id'."
    }

  private def _validate_relation(
    relation: StructureViewRelation,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(relation.id, "Structure View relation identity", violations)
    _require_nonblank(relation.component.value, s"Structure View relation '${relation.id}' Component identity", violations)
    if (relation.component != component)
      violations += s"Structure View relation '${relation.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(relation.semanticRelationshipId.value, s"Structure View relation '${relation.id}' semantic relationship identity", violations)
    _require_nonblank(relation.sourceEndpointId.value, s"Structure View relation '${relation.id}' source endpoint identity", violations)
    _require_nonblank(relation.targetEndpointId.value, s"Structure View relation '${relation.id}' target endpoint identity", violations)
    if (!_relation_role(relation.role))
      violations += s"Structure View relation '${relation.id}' has assertion role ${relation.role}."
    _validate_attribution(relation.attribution, s"Structure View relation '${relation.id}'", violations)
    _validate_condition(relation.condition, s"Structure View relation '${relation.id}'", violations)
    _validate_tie_key(relation.stableTieKey, s"Structure View relation '${relation.id}'", violations)
    _validate_navigation_target(
      Some(relation.targetEndpointId),
      relation.component,
      relation.navigationTarget,
      s"Structure View relation '${relation.id}'",
      violations
    )
    relation.assertions.foreach(assertion => _validate_assertion(assertion, relation, violations))
  }

  private def _validate_assertion(
    assertion: StructureViewAssertion,
    relation: StructureViewRelation,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(assertion.id, s"Structure View relation '${relation.id}' assertion identity", violations)
    _require_nonblank(assertion.component.value, s"Structure View assertion '${assertion.id}' Component identity", violations)
    if (assertion.component != relation.component)
      violations += s"Structure View assertion '${assertion.id}' is outside relation '${relation.id}' exact Component scope."
    _require_nonblank(assertion.semanticRelationshipId.value, s"Structure View assertion '${assertion.id}' semantic relationship identity", violations)
    if (assertion.semanticRelationshipId != relation.semanticRelationshipId)
      violations += s"Structure View assertion '${assertion.id}' must retain relation '${relation.id}' exact semantic relationship identity."
    assertion.affectedEndpointId.foreach { endpointid =>
      _require_nonblank(endpointid.value, s"Structure View assertion '${assertion.id}' affected endpoint identity", violations)
      if (endpointid != relation.sourceEndpointId && endpointid != relation.targetEndpointId)
        violations += s"Structure View assertion '${assertion.id}' affected endpoint must be retained by relation '${relation.id}'."
    }
    if (!_assertion_role(assertion.role))
      violations += s"Structure View assertion '${assertion.id}' has relation role ${assertion.role}."
    _validate_attribution(assertion.attribution, s"Structure View assertion '${assertion.id}'", violations)
    _validate_condition(assertion.condition, s"Structure View assertion '${assertion.id}'", violations)
    _validate_tie_key(assertion.stableTieKey, s"Structure View assertion '${assertion.id}'", violations)
    _validate_navigation_target(
      assertion.affectedEndpointId,
      assertion.component,
      assertion.navigationTarget,
      s"Structure View assertion '${assertion.id}'",
      violations
    )
  }

  private def _validate_duplicate_assertion_ids(
    relations: Vector[StructureViewRelation],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(relations.flatMap(_.assertions.map(_.id))).foreach { id =>
      violations += s"Duplicate Structure View assertion identity '$id'."
    }

  private def _validate_duplicate_gap_ids(
    gaps: Vector[StructureViewGap],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(gaps.map(_.id)).foreach { id =>
      violations += s"Duplicate Structure View gap identity '$id'."
    }

  private def _validate_gap(
    gap: StructureViewGap,
    component: ComponentDashboardComponentIdentity,
    relations: Vector[StructureViewRelation],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "Structure View gap identity", violations)
    _require_nonblank(gap.component.value, s"Structure View gap '${gap.id}' Component identity", violations)
    if (gap.component != component)
      violations += s"Structure View gap '${gap.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(gap.boundedFieldOrScope, s"Structure View gap '${gap.id}' bounded unsupported field or scope", violations)
    gap.affectedSemanticRelationshipId.foreach { relationshipid =>
      _require_nonblank(relationshipid.value, s"Structure View gap '${gap.id}' affected semantic relationship identity", violations)
      if (!_relation_for(relationshipid, relations).isDefined)
        violations += s"Structure View gap '${gap.id}' affected semantic relationship identity '${relationshipid.value}' is not retained in this Projection."
    }
    gap.affectedEndpointId.foreach { endpointid =>
      _require_nonblank(endpointid.value, s"Structure View gap '${gap.id}' affected endpoint identity", violations)
      if (!_known_endpoint(endpointid, relations))
        violations += s"Structure View gap '${gap.id}' affected endpoint identity '${endpointid.value}' is not retained in this Projection."
      gap.affectedSemanticRelationshipId.foreach { relationshipid =>
        _relation_for(relationshipid, relations).foreach { relation =>
          if (endpointid != relation.sourceEndpointId && endpointid != relation.targetEndpointId)
            violations += s"Structure View gap '${gap.id}' affected endpoint must be retained by its exact relationship."
        }
      }
    }
    _validate_attribution(gap.attribution, s"Structure View gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"Structure View gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"Structure View gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"Structure View gap '${gap.id}'", violations)
    _validate_navigation_target(
      gap.affectedEndpointId,
      gap.component,
      gap.navigationTarget,
      s"Structure View gap '${gap.id}'",
      violations
    )
  }

  private def _validate_navigation_target(
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    navigationtarget.foreach { target =>
      _require_nonblank(target.targetIdentity.value, s"$subject navigation target identity", violations)
      if (!identity.contains(target.targetIdentity))
        violations += s"$subject navigation target must retain its exact target endpoint identity."
      _require_nonblank(target.targetComponent.value, s"$subject navigation target Component identity", violations)
      if (target.targetComponent != component)
        violations += s"$subject navigation target must remain within its exact Component scope."
      _validate_attribution(target.targetAttribution, s"$subject navigation target", violations)
      _validate_condition(target.targetCondition, s"$subject navigation target", violations)
    }

  private def _validate_attribution(
    attribution: ComponentDashboardSourceAttribution,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(attribution.sourceId, s"$subject source identity", violations)
    _require_nonblank(attribution.authorityScope, s"$subject source authority scope", violations)
    _require_nonblank(attribution.sourceLocator, s"$subject source locator", violations)
  }

  private def _validate_condition(
    condition: ComponentDashboardCondition,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(condition.availability, s"$subject condition availability", violations)
    _require_nonblank(condition.authorization, s"$subject condition authorization", violations)
    condition.redaction.foreach(value => _require_nonblank(value, s"$subject condition redaction", violations))
    condition.explicitAbsence.foreach(value => _require_nonblank(value, s"$subject condition explicit absence", violations))
    condition.ambiguity.foreach(value => _require_nonblank(value, s"$subject condition ambiguity", violations))
    condition.conflict.foreach(value => _require_nonblank(value, s"$subject condition conflict", violations))
    condition.staleness.foreach(value => _require_nonblank(value, s"$subject condition staleness", violations))
    condition.malformedEvidence.foreach(value => _require_nonblank(value, s"$subject condition malformed evidence", violations))
    condition.limitations.foreach(value => _require_nonblank(value, s"$subject condition limitation", violations))
  }

  private def _validate_tie_key(
    tiekey: Option[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    tiekey.foreach(value => _require_nonblank(value, s"$subject stable non-semantic tie key", violations))

  private def _project_relation(
    relation: StructureViewRelation
  ): StructureViewProjectedRelation =
    StructureViewProjectedRelation(
      relation,
      _order_assertions(relation.assertions).map { assertion =>
        StructureViewProjectedAssertion(
          assertion,
          _admitted_navigation_target(
            assertion.affectedEndpointId,
            assertion.component,
            assertion.navigationTarget,
            Vector(relation.condition, assertion.condition)
          )
        )
      },
      _admitted_navigation_target(
        Some(relation.targetEndpointId),
        relation.component,
        relation.navigationTarget,
        Vector(relation.condition)
      )
    )

  private def _project_gap(
    gap: StructureViewGap,
    relations: Vector[StructureViewRelation]
  ): StructureViewProjectedGap = {
    val conditions = gap.affectedSemanticRelationshipId.flatMap(_relation_for(_, relations)) match {
      case Some(relation) => Vector(relation.condition, gap.condition)
      case None => Vector(gap.condition)
    }
    StructureViewProjectedGap(
      gap,
      _admitted_navigation_target(
        gap.affectedEndpointId,
        gap.component,
        gap.navigationTarget,
        conditions
      )
    )
  }

  private def _admitted_navigation_target(
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    conditions: Vector[ComponentDashboardCondition]
  ): Option[ComponentDashboardNavigationTarget] =
    navigationtarget.filter { target =>
      identity.contains(target.targetIdentity) &&
        target.targetComponent == component &&
        target.implementedAndUsable &&
        conditions.forall(_usable_condition) &&
        _usable_condition(target.targetCondition)
    }

  private def _usable_condition(condition: ComponentDashboardCondition): Boolean =
    condition.availability == "available" &&
      condition.authorization == "admitted" &&
      condition.redaction.isEmpty &&
      condition.explicitAbsence.isEmpty &&
      condition.ambiguity.isEmpty &&
      condition.conflict.isEmpty &&
      condition.staleness.isEmpty &&
      condition.malformedEvidence.isEmpty &&
      condition.limitations.isEmpty

  private def _order_relations(
    relations: Vector[StructureViewRelation]
  ): Vector[StructureViewRelation] =
    relations.sortBy { relation =>
      (
        relation.semanticRelationshipId.value,
        relation.sourceEndpointId.value,
        relation.targetEndpointId.value,
        relation.id,
        relation.stableTieKey.getOrElse("")
      )
    }

  private def _order_assertions(
    assertions: Vector[StructureViewAssertion]
  ): Vector[StructureViewAssertion] =
    assertions.sortBy { assertion =>
      (
        assertion.semanticRelationshipId.value,
        assertion.id,
        assertion.affectedEndpointId.map(_.value).getOrElse(""),
        assertion.stableTieKey.getOrElse("")
      )
    }

  private def _order_gaps(
    gaps: Vector[StructureViewGap]
  ): Vector[StructureViewGap] =
    gaps.sortBy(gap => (gap.id, gap.stableTieKey.getOrElse("")))

  private def _relation_for(
    relationshipid: ComponentDashboardSemanticTargetIdentity,
    relations: Vector[StructureViewRelation]
  ): Option[StructureViewRelation] =
    relations.find(_.semanticRelationshipId == relationshipid)

  private def _known_endpoint(
    endpointid: ComponentDashboardSemanticTargetIdentity,
    relations: Vector[StructureViewRelation]
  ): Boolean =
    relations.exists(relation => relation.sourceEndpointId == endpointid || relation.targetEndpointId == endpointid)

  private def _relation_role(role: StructureViewProjectionRole): Boolean =
    role == StructureComposition ||
      role == StructureAggregation ||
      role == StructureAssociation ||
      role == StructureContainment ||
      role == StructureOwnership

  private def _assertion_role(role: StructureViewProjectionRole): Boolean =
    role == IndependentExistence ||
      role == Reassignment ||
      role == DeletionLifecycle ||
      role == Cardinality ||
      role == Navigability

  private def _duplicate_ids(ids: Vector[String]): Vector[String] =
    ids.groupBy(identity).collect { case (id, values) if values.size > 1 => id }.toVector.sorted

  private def _require_nonblank(
    value: String,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (value.trim.isEmpty)
      violations += s"$subject must not be blank."
}
