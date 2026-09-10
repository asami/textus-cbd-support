package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait EntityModelProjectionRole

case object EntityModelEntity extends EntityModelProjectionRole

case object EntityModelValue extends EntityModelProjectionRole

case object EntityModelAggregate extends EntityModelProjectionRole

case object IdentityMetadata extends EntityModelProjectionRole

case object OwnershipMetadata extends EntityModelProjectionRole

case object LifecycleMetadata extends EntityModelProjectionRole

case object AggregateBoundaryMetadata extends EntityModelProjectionRole

final case class EntityModelProjectionSubject(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: EntityModelProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  metadata: Vector[EntityModelProjectionMetadata],
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EntityModelProjectionMetadata(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticSubjectId: ComponentDashboardSemanticTargetIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  role: EntityModelProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EntityModelProjectionGap(
  id: String,
  component: ComponentDashboardComponentIdentity,
  requestedRole: EntityModelProjectionRole,
  boundedFieldOrScope: String,
  affectedSemanticTargetId: Option[ComponentDashboardSemanticTargetIdentity],
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EntityModelProjectedMetadata(
  sourceMetadata: EntityModelProjectionMetadata,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EntityModelProjectedSubject(
  sourceSubject: EntityModelProjectionSubject,
  metadata: Vector[EntityModelProjectedMetadata],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EntityModelProjectedGap(
  sourceGap: EntityModelProjectionGap,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EntityModelProjectionFailure(violations: Vector[String])

final case class EntityModelProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[EntityModelProjectedSubject],
  gaps: Vector[EntityModelProjectedGap]
)

object EntityModelProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[EntityModelProjectionSubject],
    gaps: Vector[EntityModelProjectionGap]
  ): Either[EntityModelProjectionFailure, EntityModelProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_subject_ids(subjects, violations)
    _validate_duplicate_subject_semantic_target_ids(subjects, violations)
    subjects.foreach(subject => _validate_subject(subject, component, violations))
    _validate_duplicate_metadata_ids(subjects, violations)
    _validate_duplicate_metadata_relationship_ids(subjects, violations)
    _validate_metadata_targets(subjects, violations)
    _validate_duplicate_gap_ids(gaps, violations)
    gaps.foreach(gap => _validate_gap(gap, component, subjects, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(EntityModelProjectionFailure(result))
    else
      Right(
        EntityModelProjection(
          context,
          component,
          _order_subjects(subjects).map(_project_subject),
          _order_gaps(gaps).map(_project_gap)
        )
      )
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Entity Model Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Entity Model Projection Component identity", violations)

  private def _validate_duplicate_subject_ids(
    subjects: Vector[EntityModelProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.map(_.id)).foreach { id =>
      violations += s"Duplicate Entity Model subject identity '$id'."
    }

  private def _validate_duplicate_subject_semantic_target_ids(
    subjects: Vector[EntityModelProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.map(_.semanticTargetId.value)).foreach { id =>
      violations += s"Duplicate Entity Model subject semantic target identity '$id'."
    }

  private def _validate_subject(
    subject: EntityModelProjectionSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(subject.id, "Entity Model subject identity", violations)
    _require_nonblank(subject.component.value, s"Entity Model subject '${subject.id}' Component identity", violations)
    if (subject.component != component)
      violations += s"Entity Model subject '${subject.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(subject.semanticTargetId.value, s"Entity Model subject '${subject.id}' semantic target identity", violations)
    if (!_subject_role(subject.role))
      violations += s"Entity Model subject '${subject.id}' has metadata role ${subject.role}."
    _validate_attribution(subject.attribution, s"Entity Model subject '${subject.id}'", violations)
    _validate_condition(subject.condition, s"Entity Model subject '${subject.id}'", violations)
    _validate_tie_key(subject.stableTieKey, s"Entity Model subject '${subject.id}'", violations)
    _validate_navigation_target(
      Some(subject.semanticTargetId),
      subject.component,
      subject.navigationTarget,
      s"Entity Model subject '${subject.id}'",
      violations
    )
    subject.metadata.foreach(metadata => _validate_metadata(metadata, subject, violations))
  }

  private def _validate_metadata(
    metadata: EntityModelProjectionMetadata,
    subject: EntityModelProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(metadata.id, s"Entity Model subject '${subject.id}' metadata identity", violations)
    _require_nonblank(metadata.component.value, s"Entity Model metadata '${metadata.id}' Component identity", violations)
    if (metadata.component != subject.component)
      violations += s"Entity Model metadata '${metadata.id}' is outside subject '${subject.id}' exact Component scope."
    _require_nonblank(metadata.semanticSubjectId.value, s"Entity Model metadata '${metadata.id}' semantic subject identity", violations)
    if (metadata.semanticSubjectId != subject.semanticTargetId)
      violations += s"Entity Model metadata '${metadata.id}' must retain subject '${subject.id}' exact semantic identity."
    _require_nonblank(metadata.semanticTargetId.value, s"Entity Model metadata '${metadata.id}' semantic target identity", violations)
    _require_nonblank(metadata.semanticRelationshipId.value, s"Entity Model metadata '${metadata.id}' semantic relationship identity", violations)
    if (!_metadata_role(metadata.role))
      violations += s"Entity Model metadata '${metadata.id}' has subject role ${metadata.role}."
    if ((metadata.role == IdentityMetadata || metadata.role == LifecycleMetadata) && metadata.semanticTargetId != metadata.semanticSubjectId)
      violations += s"Entity Model metadata '${metadata.id}' ${metadata.role} must retain its exact subject semantic identity."
    _validate_attribution(metadata.attribution, s"Entity Model metadata '${metadata.id}'", violations)
    _validate_condition(metadata.condition, s"Entity Model metadata '${metadata.id}'", violations)
    _validate_tie_key(metadata.stableTieKey, s"Entity Model metadata '${metadata.id}'", violations)
    _validate_navigation_target(
      Some(metadata.semanticTargetId),
      metadata.component,
      metadata.navigationTarget,
      s"Entity Model metadata '${metadata.id}'",
      violations
    )
  }

  private def _validate_duplicate_metadata_ids(
    subjects: Vector[EntityModelProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.flatMap(_.metadata.map(_.id))).foreach { id =>
      violations += s"Duplicate Entity Model metadata identity '$id'."
    }

  private def _validate_duplicate_metadata_relationship_ids(
    subjects: Vector[EntityModelProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.flatMap(_.metadata.map(_.semanticRelationshipId.value))).foreach { id =>
      violations += s"Duplicate Entity Model metadata relationship identity '$id'."
    }

  private def _validate_metadata_targets(
    subjects: Vector[EntityModelProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val subjectids = subjects.map(_.semanticTargetId).toSet
    subjects.flatMap(_.metadata).foreach { metadata =>
      if (!subjectids.contains(metadata.semanticTargetId))
        violations += s"Entity Model metadata '${metadata.id}' target '${metadata.semanticTargetId.value}' is not a retained exact subject identity."
      if (metadata.role == AggregateBoundaryMetadata && !subjects.exists(subject =>
        subject.semanticTargetId == metadata.semanticTargetId && subject.role == EntityModelAggregate
      ))
        violations += s"Entity Model aggregate-boundary metadata '${metadata.id}' target '${metadata.semanticTargetId.value}' is not a retained Aggregate identity."
    }
  }

  private def _validate_duplicate_gap_ids(
    gaps: Vector[EntityModelProjectionGap],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(gaps.map(_.id)).foreach { id =>
      violations += s"Duplicate Entity Model gap identity '$id'."
    }

  private def _validate_gap(
    gap: EntityModelProjectionGap,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[EntityModelProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "Entity Model gap identity", violations)
    _require_nonblank(gap.component.value, s"Entity Model gap '${gap.id}' Component identity", violations)
    if (gap.component != component)
      violations += s"Entity Model gap '${gap.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(gap.boundedFieldOrScope, s"Entity Model gap '${gap.id}' bounded unsupported field or scope", violations)
    gap.affectedSemanticTargetId.foreach { identity =>
      _require_nonblank(identity.value, s"Entity Model gap '${gap.id}' affected semantic identity", violations)
      if (!_known_identity(identity, subjects))
        violations += s"Entity Model gap '${gap.id}' affected semantic identity '${identity.value}' is not retained in this Projection."
    }
    _validate_attribution(gap.attribution, s"Entity Model gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"Entity Model gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"Entity Model gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"Entity Model gap '${gap.id}'", violations)
    _validate_navigation_target(
      gap.affectedSemanticTargetId,
      gap.component,
      gap.navigationTarget,
      s"Entity Model gap '${gap.id}'",
      violations
    )
  }

  private def _known_identity(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[EntityModelProjectionSubject]
  ): Boolean =
    subjects.exists(_.semanticTargetId == identity) ||
      subjects.flatMap(_.metadata).exists(_.semanticRelationshipId == identity)

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
        violations += s"$subject navigation target must retain its exact semantic target identity."
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

  private def _project_subject(
    subject: EntityModelProjectionSubject
  ): EntityModelProjectedSubject =
    EntityModelProjectedSubject(
      subject,
      _order_metadata(subject.metadata).map { metadata =>
        EntityModelProjectedMetadata(
          metadata,
          _admitted_navigation_target(
            Some(metadata.semanticTargetId),
            metadata.component,
            metadata.navigationTarget,
            Vector(subject.condition, metadata.condition)
          )
        )
      },
      _admitted_navigation_target(
        Some(subject.semanticTargetId),
        subject.component,
        subject.navigationTarget,
        Vector(subject.condition)
      )
    )

  private def _project_gap(
    gap: EntityModelProjectionGap
  ): EntityModelProjectedGap =
    EntityModelProjectedGap(
      gap,
      _admitted_navigation_target(
        gap.affectedSemanticTargetId,
        gap.component,
        gap.navigationTarget,
        Vector(gap.condition)
      )
    )

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

  private def _order_subjects(
    subjects: Vector[EntityModelProjectionSubject]
  ): Vector[EntityModelProjectionSubject] =
    subjects.sortBy { subject =>
      (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))
    }

  private def _order_metadata(
    metadata: Vector[EntityModelProjectionMetadata]
  ): Vector[EntityModelProjectionMetadata] =
    metadata.sortBy { value =>
      (
        value.semanticSubjectId.value,
        value.semanticRelationshipId.value,
        value.semanticTargetId.value,
        value.id,
        value.stableTieKey.getOrElse("")
      )
    }

  private def _order_gaps(
    gaps: Vector[EntityModelProjectionGap]
  ): Vector[EntityModelProjectionGap] =
    gaps.sortBy { gap =>
      (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse(""))
    }

  private def _subject_role(role: EntityModelProjectionRole): Boolean =
    role == EntityModelEntity || role == EntityModelValue || role == EntityModelAggregate

  private def _metadata_role(role: EntityModelProjectionRole): Boolean =
    role == IdentityMetadata ||
      role == OwnershipMetadata ||
      role == LifecycleMetadata ||
      role == AggregateBoundaryMetadata

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
