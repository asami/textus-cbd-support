package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait EventModelProjectionRole

case object EventModelCommand extends EventModelProjectionRole

case object EventModelEvent extends EventModelProjectionRole

case object EventModelCausalAssertion extends EventModelProjectionRole

case object EventModelConsequenceAssertion extends EventModelProjectionRole

case object EventModelAffectedDomainElementAssertion extends EventModelProjectionRole

case object EventModelGeneratedStateEffectAssertion extends EventModelProjectionRole

final case class EventModelAssertionEndpointRole(value: String)

final case class EventModelAssertionDirection(value: String)

final case class EventModelProjectionSubject(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: EventModelProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EventModelAssertionEndpoint(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: EventModelAssertionEndpointRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EventModelProjectionAssertion(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticAssertionId: ComponentDashboardSemanticTargetIdentity,
  role: EventModelProjectionRole,
  direction: EventModelAssertionDirection,
  sourceEndpoint: EventModelAssertionEndpoint,
  targetEndpoint: EventModelAssertionEndpoint,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class EventModelProjectionGap(
  id: String,
  component: ComponentDashboardComponentIdentity,
  requestedRole: EventModelProjectionRole,
  boundedFieldOrScope: String,
  affectedSemanticTargetId: Option[ComponentDashboardSemanticTargetIdentity],
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EventModelProjectedSubject(
  sourceSubject: EventModelProjectionSubject,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EventModelProjectedAssertion(
  sourceAssertion: EventModelProjectionAssertion,
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EventModelProjectedGap(
  sourceGap: EventModelProjectionGap,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class EventModelProjectionFailure(violations: Vector[String])

final case class EventModelProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[EventModelProjectedSubject],
  assertions: Vector[EventModelProjectedAssertion],
  gaps: Vector[EventModelProjectedGap],
  forwardAssertions: Vector[EventModelProjectedAssertion],
  reverseAssertions: Vector[EventModelProjectedAssertion]
)

object EventModelProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[EventModelProjectionSubject],
    assertions: Vector[EventModelProjectionAssertion],
    gaps: Vector[EventModelProjectionGap]
  ): Either[EventModelProjectionFailure, EventModelProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_record_ids(subjects, assertions, gaps, violations)
    _validate_duplicate_semantic_ids(subjects, assertions, violations)
    subjects.foreach(subject => _validate_subject(subject, component, violations))
    assertions.foreach(assertion => _validate_assertion(assertion, component, violations))
    _validate_duplicate_endpoint_ids(assertions, violations)
    gaps.foreach(gap => _validate_gap(gap, component, subjects, assertions, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(EventModelProjectionFailure(result))
    else {
      val projectedsubjects = _order_subjects(subjects).map(_project_subject)
      val projectedassertions = _order_assertions(assertions).map(_project_assertion)
      val projectedgaps = _order_gaps(gaps).map(_project_gap)
      Right(
        EventModelProjection(
          context,
          component,
          projectedsubjects,
          projectedassertions,
          projectedgaps,
          _order_forward_assertions(projectedassertions),
          _order_reverse_assertions(projectedassertions)
        )
      )
    }
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Event Model Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Event Model Projection Component identity", violations)

  private def _validate_duplicate_record_ids(
    subjects: Vector[EventModelProjectionSubject],
    assertions: Vector[EventModelProjectionAssertion],
    gaps: Vector[EventModelProjectionGap],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val endpointids = assertions.flatMap(assertion => Vector(assertion.sourceEndpoint.id, assertion.targetEndpoint.id))
    _duplicate_ids(subjects.map(_.id) ++ assertions.map(_.id) ++ endpointids ++ gaps.map(_.id)).foreach { id =>
      violations += s"Duplicate Event Model subject, assertion, or gap identity '$id'."
    }
  }

  private def _validate_duplicate_semantic_ids(
    subjects: Vector[EventModelProjectionSubject],
    assertions: Vector[EventModelProjectionAssertion],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.map(_.semanticTargetId.value) ++ assertions.map(_.semanticAssertionId.value)).foreach { id =>
      violations += s"Duplicate Event Model subject or assertion semantic identity '$id'."
    }

  private def _validate_subject(
    subject: EventModelProjectionSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(subject.id, "Event Model subject identity", violations)
    _require_nonblank(subject.component.value, s"Event Model subject '${subject.id}' Component identity", violations)
    if (subject.component != component)
      violations += s"Event Model subject '${subject.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(subject.semanticTargetId.value, s"Event Model subject '${subject.id}' semantic identity", violations)
    if (!_subject_role(subject.role))
      violations += s"Event Model subject '${subject.id}' has assertion role ${subject.role}."
    _validate_attribution(subject.attribution, s"Event Model subject '${subject.id}'", violations)
    _validate_condition(subject.condition, s"Event Model subject '${subject.id}'", violations)
    _validate_tie_key(subject.stableTieKey, s"Event Model subject '${subject.id}'", violations)
    _validate_navigation_target(
      Some(subject.semanticTargetId),
      subject.component,
      subject.attribution,
      subject.condition,
      subject.navigationTarget,
      s"Event Model subject '${subject.id}'",
      violations
    )
  }

  private def _validate_assertion(
    assertion: EventModelProjectionAssertion,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(assertion.id, "Event Model assertion identity", violations)
    _require_nonblank(assertion.component.value, s"Event Model assertion '${assertion.id}' Component identity", violations)
    if (assertion.component != component)
      violations += s"Event Model assertion '${assertion.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(assertion.semanticAssertionId.value, s"Event Model assertion '${assertion.id}' semantic identity", violations)
    if (!_assertion_role(assertion.role))
      violations += s"Event Model assertion '${assertion.id}' has subject role ${assertion.role}."
    _require_nonblank(assertion.direction.value, s"Event Model assertion '${assertion.id}' direction", violations)
    _validate_attribution(assertion.attribution, s"Event Model assertion '${assertion.id}'", violations)
    _validate_condition(assertion.condition, s"Event Model assertion '${assertion.id}'", violations)
    _validate_tie_key(assertion.stableTieKey, s"Event Model assertion '${assertion.id}'", violations)
    _validate_endpoint(assertion.sourceEndpoint, assertion, "source", violations)
    _validate_endpoint(assertion.targetEndpoint, assertion, "target", violations)
  }

  private def _validate_endpoint(
    endpoint: EventModelAssertionEndpoint,
    assertion: EventModelProjectionAssertion,
    position: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(endpoint.id, s"Event Model assertion '${assertion.id}' $position endpoint identity", violations)
    _require_nonblank(endpoint.component.value, s"Event Model assertion '${assertion.id}' $position endpoint Component identity", violations)
    if (endpoint.component != assertion.component)
      violations += s"Event Model assertion '${assertion.id}' $position endpoint '${endpoint.id}' is outside its exact Component scope."
    _require_nonblank(endpoint.semanticTargetId.value, s"Event Model assertion '${assertion.id}' $position endpoint '${endpoint.id}' semantic identity", violations)
    _require_nonblank(endpoint.role.value, s"Event Model assertion '${assertion.id}' $position endpoint '${endpoint.id}' role", violations)
    _validate_attribution(endpoint.attribution, s"Event Model assertion '${assertion.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_condition(endpoint.condition, s"Event Model assertion '${assertion.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_tie_key(endpoint.stableTieKey, s"Event Model assertion '${assertion.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_navigation_target(
      Some(endpoint.semanticTargetId),
      endpoint.component,
      endpoint.attribution,
      endpoint.condition,
      endpoint.navigationTarget,
      s"Event Model assertion '${assertion.id}' $position endpoint '${endpoint.id}'",
      violations
    )
  }

  private def _validate_duplicate_endpoint_ids(
    assertions: Vector[EventModelProjectionAssertion],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(assertions.flatMap(assertion => Vector(assertion.sourceEndpoint.id, assertion.targetEndpoint.id))).foreach { id =>
      violations += s"Duplicate Event Model assertion endpoint identity '$id'."
    }

  private def _validate_gap(
    gap: EventModelProjectionGap,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[EventModelProjectionSubject],
    assertions: Vector[EventModelProjectionAssertion],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "Event Model gap identity", violations)
    _require_nonblank(gap.component.value, s"Event Model gap '${gap.id}' Component identity", violations)
    if (gap.component != component)
      violations += s"Event Model gap '${gap.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(gap.boundedFieldOrScope, s"Event Model gap '${gap.id}' bounded unsupported field or scope", violations)
    gap.affectedSemanticTargetId.foreach { identity =>
      _require_nonblank(identity.value, s"Event Model gap '${gap.id}' affected semantic identity", violations)
      if (!_known_identity(identity, subjects, assertions))
        violations += s"Event Model gap '${gap.id}' affected semantic identity '${identity.value}' is not retained in this Projection."
    }
    _validate_attribution(gap.attribution, s"Event Model gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"Event Model gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"Event Model gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"Event Model gap '${gap.id}'", violations)
    _validate_navigation_target(
      gap.affectedSemanticTargetId,
      gap.component,
      gap.attribution,
      gap.condition,
      gap.navigationTarget,
      s"Event Model gap '${gap.id}'",
      violations
    )
  }

  private def _known_identity(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[EventModelProjectionSubject],
    assertions: Vector[EventModelProjectionAssertion]
  ): Boolean =
    subjects.exists(_.semanticTargetId == identity) ||
      assertions.exists(_.semanticAssertionId == identity)

  private def _validate_navigation_target(
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    attribution: ComponentDashboardSourceAttribution,
    condition: ComponentDashboardCondition,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    navigationtarget.foreach { target =>
      _require_nonblank(target.targetIdentity.value, s"$subject navigation target identity", violations)
      if (!identity.contains(target.targetIdentity))
        violations += s"$subject navigation target must retain its exact receiving endpoint identity."
      _require_nonblank(target.targetComponent.value, s"$subject navigation target Component identity", violations)
      if (target.targetComponent != component)
        violations += s"$subject navigation target must remain within its exact Component scope."
      _validate_attribution(target.targetAttribution, s"$subject navigation target", violations)
      if (target.targetAttribution != attribution)
        violations += s"$subject navigation target must retain its exact receiving endpoint attribution."
      _validate_condition(target.targetCondition, s"$subject navigation target", violations)
      if (target.targetCondition != condition)
        violations += s"$subject navigation target must retain its exact receiving endpoint condition."
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
    subject: EventModelProjectionSubject
  ): EventModelProjectedSubject =
    EventModelProjectedSubject(
      subject,
      _admitted_navigation_target(
        Some(subject.semanticTargetId),
        subject.component,
        subject.attribution,
        subject.condition,
        subject.navigationTarget,
        Vector(subject.condition)
      )
    )

  private def _project_assertion(
    assertion: EventModelProjectionAssertion
  ): EventModelProjectedAssertion = {
    val conditions = Vector(
      assertion.condition,
      assertion.sourceEndpoint.condition,
      assertion.targetEndpoint.condition
    )
    EventModelProjectedAssertion(
      assertion,
      _admitted_navigation_target(
        Some(assertion.targetEndpoint.semanticTargetId),
        assertion.targetEndpoint.component,
        assertion.targetEndpoint.attribution,
        assertion.targetEndpoint.condition,
        assertion.targetEndpoint.navigationTarget,
        conditions
      ),
      _admitted_navigation_target(
        Some(assertion.sourceEndpoint.semanticTargetId),
        assertion.sourceEndpoint.component,
        assertion.sourceEndpoint.attribution,
        assertion.sourceEndpoint.condition,
        assertion.sourceEndpoint.navigationTarget,
        conditions
      )
    )
  }

  private def _project_gap(
    gap: EventModelProjectionGap
  ): EventModelProjectedGap =
    EventModelProjectedGap(
      gap,
      _admitted_navigation_target(
        gap.affectedSemanticTargetId,
        gap.component,
        gap.attribution,
        gap.condition,
        gap.navigationTarget,
        Vector(gap.condition)
      )
    )

  private def _admitted_navigation_target(
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    attribution: ComponentDashboardSourceAttribution,
    condition: ComponentDashboardCondition,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    conditions: Vector[ComponentDashboardCondition]
  ): Option[ComponentDashboardNavigationTarget] =
    navigationtarget.filter { target =>
      identity.contains(target.targetIdentity) &&
        target.targetComponent == component &&
        target.targetAttribution == attribution &&
        target.targetCondition == condition &&
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
    subjects: Vector[EventModelProjectionSubject]
  ): Vector[EventModelProjectionSubject] =
    subjects.sortBy { subject =>
      (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))
    }

  private def _order_assertions(
    assertions: Vector[EventModelProjectionAssertion]
  ): Vector[EventModelProjectionAssertion] =
    assertions.sortBy { assertion =>
      (
        assertion.semanticAssertionId.value,
        assertion.sourceEndpoint.semanticTargetId.value,
        assertion.targetEndpoint.semanticTargetId.value,
        assertion.id,
        assertion.stableTieKey.getOrElse("")
      )
    }

  private def _order_gaps(
    gaps: Vector[EventModelProjectionGap]
  ): Vector[EventModelProjectionGap] =
    gaps.sortBy { gap =>
      (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse(""))
    }

  private def _order_forward_assertions(
    assertions: Vector[EventModelProjectedAssertion]
  ): Vector[EventModelProjectedAssertion] =
    assertions.sortBy { assertion =>
      val source = assertion.sourceAssertion
      (
        source.semanticAssertionId.value,
        source.sourceEndpoint.semanticTargetId.value,
        source.targetEndpoint.semanticTargetId.value,
        source.id,
        source.stableTieKey.getOrElse("")
      )
    }

  private def _order_reverse_assertions(
    assertions: Vector[EventModelProjectedAssertion]
  ): Vector[EventModelProjectedAssertion] =
    assertions.sortBy { assertion =>
      val source = assertion.sourceAssertion
      (
        source.semanticAssertionId.value,
        source.targetEndpoint.semanticTargetId.value,
        source.sourceEndpoint.semanticTargetId.value,
        source.id,
        source.stableTieKey.getOrElse("")
      )
    }

  private def _subject_role(role: EventModelProjectionRole): Boolean =
    role == EventModelCommand || role == EventModelEvent

  private def _assertion_role(role: EventModelProjectionRole): Boolean =
    role == EventModelCausalAssertion ||
      role == EventModelConsequenceAssertion ||
      role == EventModelAffectedDomainElementAssertion ||
      role == EventModelGeneratedStateEffectAssertion

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
