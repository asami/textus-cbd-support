package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ClassificationViewProjectionRole

sealed trait ClassificationViewRelationshipRole extends ClassificationViewProjectionRole

case object ClassificationGeneralization extends ClassificationViewRelationshipRole

case object ClassificationSpecialization extends ClassificationViewRelationshipRole

case object ClassificationTrait extends ClassificationViewRelationshipRole

case object ClassificationCategory extends ClassificationViewRelationshipRole

sealed trait ClassificationViewDimensionRole extends ClassificationViewProjectionRole

case object ClassificationPowertypeDimension extends ClassificationViewDimensionRole

sealed trait ClassificationViewDimensionAssertionRole extends ClassificationViewProjectionRole

case object ClassificationDimensionValue extends ClassificationViewDimensionAssertionRole

case object ClassificationDimensionQualifier extends ClassificationViewDimensionAssertionRole

case object ClassificationDimensionExclusivity extends ClassificationViewDimensionAssertionRole

case object ClassificationDimensionCoverage extends ClassificationViewDimensionAssertionRole

case object ClassificationDimensionMembership extends ClassificationViewDimensionAssertionRole

final case class ClassificationViewSubject(
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  component: ComponentDashboardComponentIdentity,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition
)

final case class ClassificationViewEndpoint(
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  component: ComponentDashboardComponentIdentity,
  endpointRole: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition
)

final case class ClassificationViewRelationship(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  subject: ClassificationViewSubject,
  sourceEndpoint: ClassificationViewEndpoint,
  targetEndpoint: ClassificationViewEndpoint,
  role: ClassificationViewProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewDimensionAssertion(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticDimensionId: ComponentDashboardSemanticTargetIdentity,
  semanticAssertionId: ComponentDashboardSemanticTargetIdentity,
  affectedSemanticRelationshipId: Option[ComponentDashboardSemanticTargetIdentity],
  affectedSubjectId: Option[ComponentDashboardSemanticTargetIdentity],
  affectedEndpointId: Option[ComponentDashboardSemanticTargetIdentity],
  role: ClassificationViewProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewDimension(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticDimensionId: ComponentDashboardSemanticTargetIdentity,
  boundedSubject: ClassificationViewSubject,
  role: ClassificationViewProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  assertions: Vector[ClassificationViewDimensionAssertion],
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewGap(
  id: String,
  component: ComponentDashboardComponentIdentity,
  requestedRole: ClassificationViewProjectionRole,
  boundedFieldOrScope: String,
  affectedSemanticRelationshipId: Option[ComponentDashboardSemanticTargetIdentity],
  affectedSubjectId: Option[ComponentDashboardSemanticTargetIdentity],
  affectedEndpointId: Option[ComponentDashboardSemanticTargetIdentity],
  affectedDimensionId: Option[ComponentDashboardSemanticTargetIdentity],
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewProjectedRelationship(
  sourceRelationship: ClassificationViewRelationship,
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewProjectedDimensionAssertion(
  sourceAssertion: ClassificationViewDimensionAssertion,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewProjectedDimension(
  sourceDimension: ClassificationViewDimension,
  assertions: Vector[ClassificationViewProjectedDimensionAssertion],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewProjectedGap(
  sourceGap: ClassificationViewGap,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ClassificationViewProjectionFailure(violations: Vector[String])

final case class ClassificationViewProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  relationships: Vector[ClassificationViewProjectedRelationship],
  dimensions: Vector[ClassificationViewProjectedDimension],
  gaps: Vector[ClassificationViewProjectedGap]
)

object ClassificationViewProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    relationships: Vector[ClassificationViewRelationship],
    dimensions: Vector[ClassificationViewDimension],
    gaps: Vector[ClassificationViewGap]
  ): Either[ClassificationViewProjectionFailure, ClassificationViewProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_ids(relationships.map(_.id), "Classification relationship", violations)
    _validate_duplicate_ids(relationships.map(_.semanticRelationshipId.value), "Classification semantic relationship", violations)
    relationships.foreach(relationship => _validate_relationship(relationship, component, violations))
    _validate_duplicate_ids(dimensions.map(_.id), "Classification dimension", violations)
    _validate_duplicate_ids(dimensions.map(_.semanticDimensionId.value), "Classification semantic dimension", violations)
    dimensions.foreach(dimension => _validate_dimension(dimension, component, violations))
    _validate_duplicate_ids(dimensions.flatMap(_.assertions.map(_.id)), "Classification dimension assertion", violations)
    _validate_duplicate_ids(dimensions.flatMap(_.assertions.map(_.semanticAssertionId.value)), "Classification semantic dimension assertion", violations)
    dimensions.foreach(dimension => dimension.assertions.foreach(assertion =>
      _validate_assertion(assertion, dimension, relationships, dimensions, violations)
    ))
    _validate_duplicate_ids(gaps.map(_.id), "Classification gap", violations)
    gaps.foreach(gap => _validate_gap(gap, component, relationships, dimensions, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(ClassificationViewProjectionFailure(result))
    else
      Right(
        ClassificationViewProjection(
          context,
          component,
          _order_relationships(relationships).map(_project_relationship),
          _order_dimensions(dimensions).map(dimension => _project_dimension(dimension, relationships, dimensions)),
          _order_gaps(gaps).map(gap => _project_gap(gap, relationships, dimensions))
        )
      )
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Classification View Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Classification View Projection Component identity", violations)

  private def _validate_relationship(
    relationship: ClassificationViewRelationship,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(relationship.id, "Classification relationship identity", violations)
    _validate_record_component(relationship.component, component, s"Classification relationship '${relationship.id}'", violations)
    _require_nonblank(relationship.semanticRelationshipId.value, s"Classification relationship '${relationship.id}' semantic relationship identity", violations)
    _validate_subject(relationship.subject, relationship.component, s"Classification relationship '${relationship.id}' subject", violations)
    _validate_endpoint(relationship.sourceEndpoint, relationship.component, s"Classification relationship '${relationship.id}' source endpoint", violations)
    _validate_endpoint(relationship.targetEndpoint, relationship.component, s"Classification relationship '${relationship.id}' target endpoint", violations)
    if (!_relationship_role(relationship.role))
      violations += s"Classification relationship '${relationship.id}' has non-relationship role ${relationship.role}."
    _validate_attribution(relationship.attribution, s"Classification relationship '${relationship.id}'", violations)
    _validate_condition(relationship.condition, s"Classification relationship '${relationship.id}'", violations)
    _validate_tie_key(relationship.stableTieKey, s"Classification relationship '${relationship.id}'", violations)
    _validate_navigation_target(
      Some(relationship.targetEndpoint.semanticTargetId),
      relationship.component,
      relationship.forwardNavigationTarget,
      s"Classification relationship '${relationship.id}' forward navigation target",
      violations
    )
    _validate_navigation_target(
      Some(relationship.sourceEndpoint.semanticTargetId),
      relationship.component,
      relationship.reverseNavigationTarget,
      s"Classification relationship '${relationship.id}' reverse navigation target",
      violations
    )
  }

  private def _validate_dimension(
    dimension: ClassificationViewDimension,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(dimension.id, "Classification dimension identity", violations)
    _validate_record_component(dimension.component, component, s"Classification dimension '${dimension.id}'", violations)
    _require_nonblank(dimension.semanticDimensionId.value, s"Classification dimension '${dimension.id}' semantic dimension identity", violations)
    _validate_subject(dimension.boundedSubject, dimension.component, s"Classification dimension '${dimension.id}' bounded subject", violations)
    if (!_dimension_role(dimension.role))
      violations += s"Classification dimension '${dimension.id}' has non-dimension role ${dimension.role}."
    _validate_attribution(dimension.attribution, s"Classification dimension '${dimension.id}'", violations)
    _validate_condition(dimension.condition, s"Classification dimension '${dimension.id}'", violations)
    _validate_tie_key(dimension.stableTieKey, s"Classification dimension '${dimension.id}'", violations)
    _validate_navigation_target(
      Some(dimension.semanticDimensionId),
      dimension.component,
      dimension.navigationTarget,
      s"Classification dimension '${dimension.id}' navigation target",
      violations
    )
  }

  private def _validate_assertion(
    assertion: ClassificationViewDimensionAssertion,
    dimension: ClassificationViewDimension,
    relationships: Vector[ClassificationViewRelationship],
    dimensions: Vector[ClassificationViewDimension],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(assertion.id, s"Classification dimension '${dimension.id}' assertion identity", violations)
    _validate_record_component(assertion.component, dimension.component, s"Classification assertion '${assertion.id}'", violations)
    _require_nonblank(assertion.semanticDimensionId.value, s"Classification assertion '${assertion.id}' semantic dimension identity", violations)
    if (assertion.semanticDimensionId != dimension.semanticDimensionId)
      violations += s"Classification assertion '${assertion.id}' must retain dimension '${dimension.id}' exact semantic dimension identity."
    _require_nonblank(assertion.semanticAssertionId.value, s"Classification assertion '${assertion.id}' semantic assertion identity", violations)
    if (!_assertion_role(assertion.role))
      violations += s"Classification assertion '${assertion.id}' has non-assertion role ${assertion.role}."
    _validate_affected_identity(assertion.affectedSemanticRelationshipId, "semantic relationship", s"Classification assertion '${assertion.id}'", violations)
    _validate_affected_identity(assertion.affectedSubjectId, "subject", s"Classification assertion '${assertion.id}'", violations)
    _validate_affected_identity(assertion.affectedEndpointId, "endpoint", s"Classification assertion '${assertion.id}'", violations)
    assertion.affectedSemanticRelationshipId.foreach { relationshipid =>
      if (!_relationship_for(relationshipid, relationships).isDefined)
        violations += s"Classification assertion '${assertion.id}' affected semantic relationship identity '${relationshipid.value}' is not retained in this Projection."
    }
    assertion.affectedSubjectId.foreach { subjectid =>
      if (!_known_subject(subjectid, relationships, dimensions))
        violations += s"Classification assertion '${assertion.id}' affected subject identity '${subjectid.value}' is not retained in this Projection."
    }
    assertion.affectedEndpointId.foreach { endpointid =>
      if (!_known_endpoint(endpointid, relationships))
        violations += s"Classification assertion '${assertion.id}' affected endpoint identity '${endpointid.value}' is not retained in this Projection."
    }
    _validate_relation_participation(
      assertion.affectedSemanticRelationshipId,
      assertion.affectedSubjectId,
      assertion.affectedEndpointId,
      relationships,
      s"Classification assertion '${assertion.id}'",
      violations
    )
    _validate_attribution(assertion.attribution, s"Classification assertion '${assertion.id}'", violations)
    _validate_condition(assertion.condition, s"Classification assertion '${assertion.id}'", violations)
    _validate_tie_key(assertion.stableTieKey, s"Classification assertion '${assertion.id}'", violations)
    _validate_navigation_target(
      _assertion_affected_identities(assertion),
      assertion.component,
      assertion.navigationTarget,
      s"Classification assertion '${assertion.id}' navigation target",
      violations
    )
  }

  private def _validate_gap(
    gap: ClassificationViewGap,
    component: ComponentDashboardComponentIdentity,
    relationships: Vector[ClassificationViewRelationship],
    dimensions: Vector[ClassificationViewDimension],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "Classification gap identity", violations)
    _validate_record_component(gap.component, component, s"Classification gap '${gap.id}'", violations)
    _require_nonblank(gap.boundedFieldOrScope, s"Classification gap '${gap.id}' bounded unsupported field or scope", violations)
    _validate_affected_identity(gap.affectedSemanticRelationshipId, "semantic relationship", s"Classification gap '${gap.id}'", violations)
    _validate_affected_identity(gap.affectedSubjectId, "subject", s"Classification gap '${gap.id}'", violations)
    _validate_affected_identity(gap.affectedEndpointId, "endpoint", s"Classification gap '${gap.id}'", violations)
    _validate_affected_identity(gap.affectedDimensionId, "dimension", s"Classification gap '${gap.id}'", violations)
    gap.affectedSemanticRelationshipId.foreach { relationshipid =>
      if (!_relationship_for(relationshipid, relationships).isDefined)
        violations += s"Classification gap '${gap.id}' affected semantic relationship identity '${relationshipid.value}' is not retained in this Projection."
    }
    gap.affectedSubjectId.foreach { subjectid =>
      if (!_known_subject(subjectid, relationships, dimensions))
        violations += s"Classification gap '${gap.id}' affected subject identity '${subjectid.value}' is not retained in this Projection."
    }
    gap.affectedEndpointId.foreach { endpointid =>
      if (!_known_endpoint(endpointid, relationships))
        violations += s"Classification gap '${gap.id}' affected endpoint identity '${endpointid.value}' is not retained in this Projection."
    }
    gap.affectedDimensionId.foreach { dimensionid =>
      if (!_dimension_for(dimensionid, dimensions).isDefined)
        violations += s"Classification gap '${gap.id}' affected dimension identity '${dimensionid.value}' is not retained in this Projection."
    }
    _validate_relation_participation(
      gap.affectedSemanticRelationshipId,
      gap.affectedSubjectId,
      gap.affectedEndpointId,
      relationships,
      s"Classification gap '${gap.id}'",
      violations
    )
    _validate_attribution(gap.attribution, s"Classification gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"Classification gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"Classification gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"Classification gap '${gap.id}'", violations)
    _validate_navigation_target(
      _gap_affected_identities(gap),
      gap.component,
      gap.navigationTarget,
      s"Classification gap '${gap.id}' navigation target",
      violations
    )
  }

  private def _validate_record_component(
    recordcomponent: ComponentDashboardComponentIdentity,
    projectioncomponent: ComponentDashboardComponentIdentity,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(recordcomponent.value, s"$subject Component identity", violations)
    if (recordcomponent != projectioncomponent)
      violations += s"$subject is outside Projection Component '${projectioncomponent.value}'."
  }

  private def _validate_subject(
    subject: ClassificationViewSubject,
    component: ComponentDashboardComponentIdentity,
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_record_component(subject.component, component, label, violations)
    _require_nonblank(subject.semanticTargetId.value, s"$label semantic subject identity", violations)
    _validate_attribution(subject.attribution, label, violations)
    _validate_condition(subject.condition, label, violations)
  }

  private def _validate_endpoint(
    endpoint: ClassificationViewEndpoint,
    component: ComponentDashboardComponentIdentity,
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_record_component(endpoint.component, component, label, violations)
    _require_nonblank(endpoint.semanticTargetId.value, s"$label semantic endpoint identity", violations)
    _require_nonblank(endpoint.endpointRole, s"$label role", violations)
    _validate_attribution(endpoint.attribution, label, violations)
    _validate_condition(endpoint.condition, label, violations)
  }

  private def _validate_relation_participation(
    relationshipid: Option[ComponentDashboardSemanticTargetIdentity],
    subjectid: Option[ComponentDashboardSemanticTargetIdentity],
    endpointid: Option[ComponentDashboardSemanticTargetIdentity],
    relationships: Vector[ClassificationViewRelationship],
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    relationshipid.foreach { value =>
      _relationship_for(value, relationships).foreach { relationship =>
        subjectid.foreach { subject =>
          if (subject != relationship.subject.semanticTargetId)
            violations += s"$label affected subject must be retained by its exact relationship."
        }
        endpointid.foreach { endpoint =>
          if (endpoint != relationship.sourceEndpoint.semanticTargetId && endpoint != relationship.targetEndpoint.semanticTargetId)
            violations += s"$label affected endpoint must be retained by its exact relationship."
        }
      }
    }

  private def _validate_navigation_target(
    identities: Vector[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    navigationtarget.foreach { target =>
      _require_nonblank(target.targetIdentity.value, s"$label identity", violations)
      _require_nonblank(target.targetComponent.value, s"$label Component identity", violations)
      _validate_attribution(target.targetAttribution, label, violations)
      _validate_condition(target.targetCondition, label, violations)
    }

  private def _validate_navigation_target(
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _validate_navigation_target(identity.toVector, component, navigationtarget, label, violations)

  private def _validate_attribution(
    attribution: ComponentDashboardSourceAttribution,
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(attribution.sourceId, s"$label source identity", violations)
    _require_nonblank(attribution.authorityScope, s"$label source authority scope", violations)
    _require_nonblank(attribution.sourceLocator, s"$label source locator", violations)
  }

  private def _validate_condition(
    condition: ComponentDashboardCondition,
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(condition.availability, s"$label condition availability", violations)
    _require_nonblank(condition.authorization, s"$label condition authorization", violations)
    condition.redaction.foreach(value => _require_nonblank(value, s"$label condition redaction", violations))
    condition.explicitAbsence.foreach(value => _require_nonblank(value, s"$label condition explicit absence", violations))
    condition.ambiguity.foreach(value => _require_nonblank(value, s"$label condition ambiguity", violations))
    condition.conflict.foreach(value => _require_nonblank(value, s"$label condition conflict", violations))
    condition.staleness.foreach(value => _require_nonblank(value, s"$label condition staleness", violations))
    condition.malformedEvidence.foreach(value => _require_nonblank(value, s"$label condition malformed evidence", violations))
    condition.limitations.foreach(value => _require_nonblank(value, s"$label condition limitation", violations))
  }

  private def _validate_tie_key(
    tiekey: Option[String],
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    tiekey.foreach(value => _require_nonblank(value, s"$label stable non-semantic tie key", violations))

  private def _validate_affected_identity(
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    kind: String,
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    identity.foreach(value => _require_nonblank(value.value, s"$label affected $kind identity", violations))

  private def _project_relationship(
    relationship: ClassificationViewRelationship
  ): ClassificationViewProjectedRelationship =
    ClassificationViewProjectedRelationship(
      relationship,
      _admitted_navigation_target(
        relationship.targetEndpoint.semanticTargetId,
        relationship.component,
        relationship.forwardNavigationTarget,
        Vector(
          relationship.condition,
          relationship.subject.condition,
          relationship.sourceEndpoint.condition,
          relationship.targetEndpoint.condition
        )
      ),
      _admitted_navigation_target(
        relationship.sourceEndpoint.semanticTargetId,
        relationship.component,
        relationship.reverseNavigationTarget,
        Vector(
          relationship.condition,
          relationship.subject.condition,
          relationship.sourceEndpoint.condition,
          relationship.targetEndpoint.condition
        )
      )
    )

  private def _project_dimension(
    dimension: ClassificationViewDimension,
    relationships: Vector[ClassificationViewRelationship],
    dimensions: Vector[ClassificationViewDimension]
  ): ClassificationViewProjectedDimension =
    ClassificationViewProjectedDimension(
      dimension,
      _order_assertions(dimension.assertions).map { assertion =>
        ClassificationViewProjectedDimensionAssertion(
          assertion,
          _admitted_navigation_target(
            _assertion_affected_identities(assertion),
            assertion.component,
            assertion.navigationTarget,
            Vector(dimension.condition, dimension.boundedSubject.condition, assertion.condition) ++
              _affected_conditions(assertion.affectedSemanticRelationshipId, assertion.affectedSubjectId, assertion.affectedEndpointId, None, relationships, dimensions)
          )
        )
      },
      _admitted_navigation_target(
        dimension.semanticDimensionId,
        dimension.component,
        dimension.navigationTarget,
        Vector(dimension.condition, dimension.boundedSubject.condition)
      )
    )

  private def _project_gap(
    gap: ClassificationViewGap,
    relationships: Vector[ClassificationViewRelationship],
    dimensions: Vector[ClassificationViewDimension]
  ): ClassificationViewProjectedGap =
    ClassificationViewProjectedGap(
      gap,
      _admitted_navigation_target(
        _gap_affected_identities(gap),
        gap.component,
        gap.navigationTarget,
        Vector(gap.condition) ++
          _affected_conditions(gap.affectedSemanticRelationshipId, gap.affectedSubjectId, gap.affectedEndpointId, gap.affectedDimensionId, relationships, dimensions)
      )
    )

  private def _admitted_navigation_target(
    identity: ComponentDashboardSemanticTargetIdentity,
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    conditions: Vector[ComponentDashboardCondition]
  ): Option[ComponentDashboardNavigationTarget] =
    _admitted_navigation_target(Vector(identity), component, navigationtarget, conditions)

  private def _admitted_navigation_target(
    identities: Vector[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    conditions: Vector[ComponentDashboardCondition]
  ): Option[ComponentDashboardNavigationTarget] =
    navigationtarget.filter { target =>
      identities.contains(target.targetIdentity) &&
        target.targetComponent == component &&
        target.implementedAndUsable &&
        conditions.forall(_usable_condition) &&
        _usable_condition(target.targetCondition)
    }

  private def _affected_conditions(
    relationshipid: Option[ComponentDashboardSemanticTargetIdentity],
    subjectid: Option[ComponentDashboardSemanticTargetIdentity],
    endpointid: Option[ComponentDashboardSemanticTargetIdentity],
    dimensionid: Option[ComponentDashboardSemanticTargetIdentity],
    relationships: Vector[ClassificationViewRelationship],
    dimensions: Vector[ClassificationViewDimension]
  ): Vector[ComponentDashboardCondition] = {
    val relationshipconditions = relationshipid.toVector.flatMap { value =>
      relationships.filter(_.semanticRelationshipId == value).flatMap { relationship =>
        Vector(
          relationship.condition,
          relationship.subject.condition,
          relationship.sourceEndpoint.condition,
          relationship.targetEndpoint.condition
        )
      }
    }
    val subjectconditions = subjectid.toVector.flatMap { value =>
      relationships.filter(_.subject.semanticTargetId == value).map(_.subject.condition) ++
        dimensions.filter(_.boundedSubject.semanticTargetId == value).map(_.boundedSubject.condition)
    }
    val endpointconditions = endpointid.toVector.flatMap { value =>
      relationships.flatMap { relationship =>
        Vector(relationship.sourceEndpoint, relationship.targetEndpoint)
          .filter(_.semanticTargetId == value)
          .map(_.condition)
      }
    }
    val dimensionconditions = dimensionid.toVector.flatMap { value =>
      dimensions.filter(_.semanticDimensionId == value).flatMap(dimension => Vector(dimension.condition, dimension.boundedSubject.condition))
    }
    relationshipconditions ++ subjectconditions ++ endpointconditions ++ dimensionconditions
  }

  private def _order_relationships(
    relationships: Vector[ClassificationViewRelationship]
  ): Vector[ClassificationViewRelationship] =
    relationships.sortBy { relationship =>
      (
        relationship.semanticRelationshipId.value,
        relationship.subject.semanticTargetId.value,
        relationship.sourceEndpoint.semanticTargetId.value,
        relationship.targetEndpoint.semanticTargetId.value,
        relationship.id,
        relationship.stableTieKey.getOrElse("")
      )
    }

  private def _order_dimensions(
    dimensions: Vector[ClassificationViewDimension]
  ): Vector[ClassificationViewDimension] =
    dimensions.sortBy { dimension =>
      (
        dimension.semanticDimensionId.value,
        dimension.boundedSubject.semanticTargetId.value,
        dimension.id,
        dimension.stableTieKey.getOrElse("")
      )
    }

  private def _order_assertions(
    assertions: Vector[ClassificationViewDimensionAssertion]
  ): Vector[ClassificationViewDimensionAssertion] =
    assertions.sortBy { assertion =>
      (
        assertion.semanticDimensionId.value,
        assertion.semanticAssertionId.value,
        assertion.affectedSemanticRelationshipId.map(_.value).getOrElse(""),
        assertion.affectedSubjectId.map(_.value).getOrElse(""),
        assertion.affectedEndpointId.map(_.value).getOrElse(""),
        assertion.id,
        assertion.stableTieKey.getOrElse("")
      )
    }

  private def _order_gaps(
    gaps: Vector[ClassificationViewGap]
  ): Vector[ClassificationViewGap] =
    gaps.sortBy { gap =>
      (
        gap.id,
        gap.affectedSemanticRelationshipId.map(_.value).getOrElse(""),
        gap.affectedSubjectId.map(_.value).getOrElse(""),
        gap.affectedEndpointId.map(_.value).getOrElse(""),
        gap.affectedDimensionId.map(_.value).getOrElse(""),
        gap.stableTieKey.getOrElse("")
      )
    }

  private def _assertion_affected_identities(
    assertion: ClassificationViewDimensionAssertion
  ): Vector[ComponentDashboardSemanticTargetIdentity] =
    Vector(
      assertion.affectedSemanticRelationshipId,
      assertion.affectedSubjectId,
      assertion.affectedEndpointId
    ).flatten

  private def _gap_affected_identities(
    gap: ClassificationViewGap
  ): Vector[ComponentDashboardSemanticTargetIdentity] =
    Vector(
      gap.affectedSemanticRelationshipId,
      gap.affectedSubjectId,
      gap.affectedEndpointId,
      gap.affectedDimensionId
    ).flatten

  private def _relationship_for(
    relationshipid: ComponentDashboardSemanticTargetIdentity,
    relationships: Vector[ClassificationViewRelationship]
  ): Option[ClassificationViewRelationship] =
    relationships.find(_.semanticRelationshipId == relationshipid)

  private def _dimension_for(
    dimensionid: ComponentDashboardSemanticTargetIdentity,
    dimensions: Vector[ClassificationViewDimension]
  ): Option[ClassificationViewDimension] =
    dimensions.find(_.semanticDimensionId == dimensionid)

  private def _known_subject(
    subjectid: ComponentDashboardSemanticTargetIdentity,
    relationships: Vector[ClassificationViewRelationship],
    dimensions: Vector[ClassificationViewDimension]
  ): Boolean =
    relationships.exists(_.subject.semanticTargetId == subjectid) ||
      dimensions.exists(_.boundedSubject.semanticTargetId == subjectid)

  private def _known_endpoint(
    endpointid: ComponentDashboardSemanticTargetIdentity,
    relationships: Vector[ClassificationViewRelationship]
  ): Boolean =
    relationships.exists { relationship =>
      relationship.sourceEndpoint.semanticTargetId == endpointid ||
        relationship.targetEndpoint.semanticTargetId == endpointid
    }

  private def _relationship_role(role: ClassificationViewProjectionRole): Boolean =
    role == ClassificationGeneralization ||
      role == ClassificationSpecialization ||
      role == ClassificationTrait ||
      role == ClassificationCategory

  private def _dimension_role(role: ClassificationViewProjectionRole): Boolean =
    role == ClassificationPowertypeDimension

  private def _assertion_role(role: ClassificationViewProjectionRole): Boolean =
    role == ClassificationDimensionValue ||
      role == ClassificationDimensionQualifier ||
      role == ClassificationDimensionExclusivity ||
      role == ClassificationDimensionCoverage ||
      role == ClassificationDimensionMembership

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

  private def _duplicate_ids(ids: Vector[String]): Vector[String] =
    ids.groupBy(identity).collect { case (value, occurrences) if occurrences.size > 1 => value }.toVector.sorted

  private def _validate_duplicate_ids(
    ids: Vector[String],
    kind: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(ids).foreach(value => violations += s"Duplicate $kind identity '$value'.")

  private def _require_nonblank(
    value: String,
    label: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (value.trim.isEmpty)
      violations += s"$label must not be blank."
}
