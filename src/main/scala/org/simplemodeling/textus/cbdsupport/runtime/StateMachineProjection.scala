package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait StateMachineProjectionRole

case object StateMachineProjectionStateMachine extends StateMachineProjectionRole

case object StateMachineProjectionState extends StateMachineProjectionRole

case object StateMachineProjectionTrigger extends StateMachineProjectionRole

case object StateMachineProjectionGuard extends StateMachineProjectionRole

case object StateMachineProjectionAction extends StateMachineProjectionRole

case object StateMachineProjectionActivity extends StateMachineProjectionRole

case object StateMachineProjectionOperation extends StateMachineProjectionRole

case object StateMachineProjectionEvent extends StateMachineProjectionRole

case object StateMachineProjectionRule extends StateMachineProjectionRole

case object StateMachineProjectionTransitionRelation extends StateMachineProjectionRole

case object StateMachineProjectionTransitionAdjunctRelation extends StateMachineProjectionRole

final case class StateMachineProjectionTransitionEndpointRole(value: String)

final case class StateMachineProjectionTransitionDirection(value: String)

final case class StateMachineProjectionAdjunctEndpointRole(value: String)

final case class StateMachineProjectionTransitionAdjunctDirection(value: String)

final case class StateMachineProjectionSubject(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: StateMachineProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectionTransitionEndpoint(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: StateMachineProjectionTransitionEndpointRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectionTransition(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTransitionId: ComponentDashboardSemanticTargetIdentity,
  stateMachineSemanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: StateMachineProjectionRole,
  direction: StateMachineProjectionTransitionDirection,
  sourceEndpoint: StateMachineProjectionTransitionEndpoint,
  targetEndpoint: StateMachineProjectionTransitionEndpoint,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class StateMachineProjectionAdjunctEndpoint(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: StateMachineProjectionAdjunctEndpointRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectionTransitionAdjunct(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticAdjunctRelationId: ComponentDashboardSemanticTargetIdentity,
  transitionSemanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: StateMachineProjectionRole,
  direction: StateMachineProjectionTransitionAdjunctDirection,
  adjunctEndpoint: StateMachineProjectionAdjunctEndpoint,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class StateMachineProjectionGap(
  id: String,
  component: ComponentDashboardComponentIdentity,
  requestedRole: StateMachineProjectionRole,
  boundedFieldOrScope: String,
  affectedSemanticTargetId: Option[ComponentDashboardSemanticTargetIdentity],
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectedSubject(
  sourceSubject: StateMachineProjectionSubject,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectedTransition(
  sourceTransition: StateMachineProjectionTransition,
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectedTransitionAdjunct(
  sourceTransitionAdjunct: StateMachineProjectionTransitionAdjunct,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectedGap(
  sourceGap: StateMachineProjectionGap,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StateMachineProjectionFailure(violations: Vector[String])

final case class StateMachineProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[StateMachineProjectedSubject],
  transitions: Vector[StateMachineProjectedTransition],
  transitionAdjuncts: Vector[StateMachineProjectedTransitionAdjunct],
  gaps: Vector[StateMachineProjectedGap],
  forwardTransitions: Vector[StateMachineProjectedTransition],
  reverseTransitions: Vector[StateMachineProjectedTransition]
)

object StateMachineProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[StateMachineProjectionSubject],
    transitions: Vector[StateMachineProjectionTransition],
    transitionAdjuncts: Vector[StateMachineProjectionTransitionAdjunct],
    gaps: Vector[StateMachineProjectionGap]
  ): Either[StateMachineProjectionFailure, StateMachineProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_record_ids(subjects, transitions, transitionAdjuncts, gaps, violations)
    _validate_duplicate_semantic_ids(subjects, transitions, transitionAdjuncts, violations)
    subjects.foreach(subject => _validate_subject(subject, component, violations))
    transitions.foreach(transition => _validate_transition(transition, component, subjects, violations))
    transitionAdjuncts.foreach(adjunct => _validate_transition_adjunct(adjunct, component, subjects, transitions, violations))
    gaps.foreach(gap => _validate_gap(gap, component, subjects, transitions, transitionAdjuncts, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(StateMachineProjectionFailure(result))
    else {
      val projectedsubjects = _order_subjects(subjects).map(_project_subject)
      val projectedtransitions = _order_transitions(transitions).map(_project_transition)
      val projectedadjuncts = _order_transition_adjuncts(transitionAdjuncts).map { adjunct =>
        _project_transition_adjunct(adjunct, transitions)
      }
      val projectedgaps = _order_gaps(gaps).map(_project_gap)
      Right(
        StateMachineProjection(
          context,
          component,
          projectedsubjects,
          projectedtransitions,
          projectedadjuncts,
          projectedgaps,
          _order_projected_transitions(projectedtransitions),
          _order_projected_transitions(projectedtransitions)
        )
      )
    }
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "StateMachine Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "StateMachine Projection Component identity", violations)

  private def _validate_duplicate_record_ids(
    subjects: Vector[StateMachineProjectionSubject],
    transitions: Vector[StateMachineProjectionTransition],
    adjuncts: Vector[StateMachineProjectionTransitionAdjunct],
    gaps: Vector[StateMachineProjectionGap],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val endpointids = transitions.flatMap(transition => Vector(transition.sourceEndpoint.id, transition.targetEndpoint.id)) ++
      adjuncts.map(_.adjunctEndpoint.id)
    _duplicate_ids(subjects.map(_.id) ++ transitions.map(_.id) ++ adjuncts.map(_.id) ++ endpointids ++ gaps.map(_.id)).foreach { id =>
      violations += s"Duplicate StateMachine subject, transition, transition-adjunct, endpoint, or gap identity '$id'."
    }
  }

  private def _validate_duplicate_semantic_ids(
    subjects: Vector[StateMachineProjectionSubject],
    transitions: Vector[StateMachineProjectionTransition],
    adjuncts: Vector[StateMachineProjectionTransitionAdjunct],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(
      subjects.map(_.semanticTargetId.value) ++
        transitions.map(_.semanticTransitionId.value) ++
        adjuncts.map(_.semanticAdjunctRelationId.value)
    ).foreach { id =>
      violations += s"Duplicate StateMachine subject, transition, or transition-adjunct semantic identity '$id'."
    }

  private def _validate_subject(
    subject: StateMachineProjectionSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(subject.id, "StateMachine subject identity", violations)
    _require_nonblank(subject.component.value, s"StateMachine subject '${subject.id}' Component identity", violations)
    if (subject.component != component)
      violations += s"StateMachine subject '${subject.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(subject.semanticTargetId.value, s"StateMachine subject '${subject.id}' semantic identity", violations)
    if (!_subject_role(subject.role))
      violations += s"StateMachine subject '${subject.id}' has relation role ${subject.role}."
    _validate_attribution(subject.attribution, s"StateMachine subject '${subject.id}'", violations)
    _validate_condition(subject.condition, s"StateMachine subject '${subject.id}'", violations)
    _validate_tie_key(subject.stableTieKey, s"StateMachine subject '${subject.id}'", violations)
    _validate_navigation_target(
      Some(subject.semanticTargetId),
      subject.component,
      subject.attribution,
      subject.condition,
      subject.navigationTarget,
      s"StateMachine subject '${subject.id}'",
      violations
    )
  }

  private def _validate_transition(
    transition: StateMachineProjectionTransition,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[StateMachineProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(transition.id, "StateMachine transition identity", violations)
    _require_nonblank(transition.component.value, s"StateMachine transition '${transition.id}' Component identity", violations)
    if (transition.component != component)
      violations += s"StateMachine transition '${transition.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(transition.semanticTransitionId.value, s"StateMachine transition '${transition.id}' semantic identity", violations)
    _require_nonblank(transition.stateMachineSemanticTargetId.value, s"StateMachine transition '${transition.id}' StateMachine semantic identity", violations)
    if (!_transition_role(transition.role))
      violations += s"StateMachine transition '${transition.id}' has subject role ${transition.role}."
    if (!_retained_state_machine(transition.stateMachineSemanticTargetId, subjects))
      violations += s"StateMachine transition '${transition.id}' StateMachine semantic identity '${transition.stateMachineSemanticTargetId.value}' is not a retained direct StateMachine subject."
    _require_nonblank(transition.direction.value, s"StateMachine transition '${transition.id}' direction", violations)
    _validate_attribution(transition.attribution, s"StateMachine transition '${transition.id}'", violations)
    _validate_condition(transition.condition, s"StateMachine transition '${transition.id}'", violations)
    _validate_tie_key(transition.stableTieKey, s"StateMachine transition '${transition.id}'", violations)
    _validate_transition_endpoint(transition.sourceEndpoint, transition, "source", subjects, violations)
    _validate_transition_endpoint(transition.targetEndpoint, transition, "target", subjects, violations)
  }

  private def _validate_transition_endpoint(
    endpoint: StateMachineProjectionTransitionEndpoint,
    transition: StateMachineProjectionTransition,
    position: String,
    subjects: Vector[StateMachineProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(endpoint.id, s"StateMachine transition '${transition.id}' $position endpoint identity", violations)
    _require_nonblank(endpoint.component.value, s"StateMachine transition '${transition.id}' $position endpoint Component identity", violations)
    if (endpoint.component != transition.component)
      violations += s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}' is outside its exact Component scope."
    _require_nonblank(endpoint.semanticTargetId.value, s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}' semantic identity", violations)
    _require_nonblank(endpoint.role.value, s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}' role", violations)
    if (!_retained_state(endpoint.semanticTargetId, subjects))
      violations += s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}' must reference an already-retained direct State subject."
    _validate_attribution(endpoint.attribution, s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_condition(endpoint.condition, s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_tie_key(endpoint.stableTieKey, s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_navigation_target(
      Some(endpoint.semanticTargetId),
      endpoint.component,
      endpoint.attribution,
      endpoint.condition,
      endpoint.navigationTarget,
      s"StateMachine transition '${transition.id}' $position endpoint '${endpoint.id}'",
      violations
    )
  }

  private def _validate_transition_adjunct(
    adjunct: StateMachineProjectionTransitionAdjunct,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[StateMachineProjectionSubject],
    transitions: Vector[StateMachineProjectionTransition],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(adjunct.id, "StateMachine transition-adjunct identity", violations)
    _require_nonblank(adjunct.component.value, s"StateMachine transition-adjunct '${adjunct.id}' Component identity", violations)
    if (adjunct.component != component)
      violations += s"StateMachine transition-adjunct '${adjunct.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(adjunct.semanticAdjunctRelationId.value, s"StateMachine transition-adjunct '${adjunct.id}' semantic identity", violations)
    _require_nonblank(adjunct.transitionSemanticTargetId.value, s"StateMachine transition-adjunct '${adjunct.id}' transition semantic identity", violations)
    if (!_transition_adjunct_role(adjunct.role))
      violations += s"StateMachine transition-adjunct '${adjunct.id}' has subject or transition role ${adjunct.role}."
    if (!_retained_transition(adjunct.transitionSemanticTargetId, transitions))
      violations += s"StateMachine transition-adjunct '${adjunct.id}' must reference an already-retained direct transition."
    _require_nonblank(adjunct.direction.value, s"StateMachine transition-adjunct '${adjunct.id}' direction", violations)
    _validate_attribution(adjunct.attribution, s"StateMachine transition-adjunct '${adjunct.id}'", violations)
    _validate_condition(adjunct.condition, s"StateMachine transition-adjunct '${adjunct.id}'", violations)
    _validate_tie_key(adjunct.stableTieKey, s"StateMachine transition-adjunct '${adjunct.id}'", violations)
    _validate_adjunct_endpoint(adjunct.adjunctEndpoint, adjunct, subjects, violations)
  }

  private def _validate_adjunct_endpoint(
    endpoint: StateMachineProjectionAdjunctEndpoint,
    adjunct: StateMachineProjectionTransitionAdjunct,
    subjects: Vector[StateMachineProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(endpoint.id, s"StateMachine transition-adjunct '${adjunct.id}' endpoint identity", violations)
    _require_nonblank(endpoint.component.value, s"StateMachine transition-adjunct '${adjunct.id}' endpoint Component identity", violations)
    if (endpoint.component != adjunct.component)
      violations += s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}' is outside its exact Component scope."
    _require_nonblank(endpoint.semanticTargetId.value, s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}' semantic identity", violations)
    _require_nonblank(endpoint.role.value, s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}' role", violations)
    if (!_retained_adjunct_subject(endpoint.semanticTargetId, subjects))
      violations += s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}' must reference an already-retained direct Trigger, Guard, Action, Activity, Operation, Event, or Rule subject."
    _validate_attribution(endpoint.attribution, s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}'", violations)
    _validate_condition(endpoint.condition, s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}'", violations)
    _validate_tie_key(endpoint.stableTieKey, s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}'", violations)
    _validate_navigation_target(
      Some(endpoint.semanticTargetId),
      endpoint.component,
      endpoint.attribution,
      endpoint.condition,
      endpoint.navigationTarget,
      s"StateMachine transition-adjunct '${adjunct.id}' endpoint '${endpoint.id}'",
      violations
    )
  }

  private def _validate_gap(
    gap: StateMachineProjectionGap,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[StateMachineProjectionSubject],
    transitions: Vector[StateMachineProjectionTransition],
    adjuncts: Vector[StateMachineProjectionTransitionAdjunct],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "StateMachine gap identity", violations)
    _require_nonblank(gap.component.value, s"StateMachine gap '${gap.id}' Component identity", violations)
    if (gap.component != component)
      violations += s"StateMachine gap '${gap.id}' is outside Projection Component '${component.value}'."
    if (!_gap_role(gap.requestedRole))
      violations += s"StateMachine gap '${gap.id}' has invalid requested role ${gap.requestedRole}."
    _require_nonblank(gap.boundedFieldOrScope, s"StateMachine gap '${gap.id}' bounded unsupported field or scope", violations)
    gap.affectedSemanticTargetId.foreach { identity =>
      _require_nonblank(identity.value, s"StateMachine gap '${gap.id}' affected semantic identity", violations)
      if (!_known_identity(identity, subjects, transitions, adjuncts))
        violations += s"StateMachine gap '${gap.id}' affected semantic identity '${identity.value}' is not retained in this Projection."
    }
    _validate_attribution(gap.attribution, s"StateMachine gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"StateMachine gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"StateMachine gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"StateMachine gap '${gap.id}'", violations)
    _validate_navigation_target(
      gap.affectedSemanticTargetId,
      gap.component,
      gap.attribution,
      gap.condition,
      gap.navigationTarget,
      s"StateMachine gap '${gap.id}'",
      violations
    )
  }

  private def _retained_state_machine(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[StateMachineProjectionSubject]
  ): Boolean =
    subjects.exists(subject => subject.semanticTargetId == identity && subject.role == StateMachineProjectionStateMachine)

  private def _retained_state(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[StateMachineProjectionSubject]
  ): Boolean =
    subjects.exists(subject => subject.semanticTargetId == identity && subject.role == StateMachineProjectionState)

  private def _retained_adjunct_subject(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[StateMachineProjectionSubject]
  ): Boolean =
    subjects.exists(subject => subject.semanticTargetId == identity && _adjunct_subject_role(subject.role))

  private def _retained_transition(
    identity: ComponentDashboardSemanticTargetIdentity,
    transitions: Vector[StateMachineProjectionTransition]
  ): Boolean =
    transitions.exists(_.semanticTransitionId == identity)

  private def _known_identity(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[StateMachineProjectionSubject],
    transitions: Vector[StateMachineProjectionTransition],
    adjuncts: Vector[StateMachineProjectionTransitionAdjunct]
  ): Boolean =
    subjects.exists(_.semanticTargetId == identity) ||
      transitions.exists(_.semanticTransitionId == identity) ||
      adjuncts.exists(_.semanticAdjunctRelationId == identity)

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
        violations += s"$subject navigation target must retain its exact local target identity."
      _require_nonblank(target.targetComponent.value, s"$subject navigation target Component identity", violations)
      if (target.targetComponent != component)
        violations += s"$subject navigation target must remain within its exact Component scope."
      _validate_attribution(target.targetAttribution, s"$subject navigation target", violations)
      if (target.targetAttribution != attribution)
        violations += s"$subject navigation target must retain its exact local target attribution."
      _validate_condition(target.targetCondition, s"$subject navigation target", violations)
      if (target.targetCondition != condition)
        violations += s"$subject navigation target must retain its exact local target condition."
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
    subject: StateMachineProjectionSubject
  ): StateMachineProjectedSubject =
    StateMachineProjectedSubject(
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

  private def _project_transition(
    transition: StateMachineProjectionTransition
  ): StateMachineProjectedTransition = {
    val conditions = Vector(
      transition.condition,
      transition.sourceEndpoint.condition,
      transition.targetEndpoint.condition
    )
    StateMachineProjectedTransition(
      transition,
      _admitted_navigation_target(
        Some(transition.targetEndpoint.semanticTargetId),
        transition.targetEndpoint.component,
        transition.targetEndpoint.attribution,
        transition.targetEndpoint.condition,
        transition.targetEndpoint.navigationTarget,
        conditions
      ),
      _admitted_navigation_target(
        Some(transition.sourceEndpoint.semanticTargetId),
        transition.sourceEndpoint.component,
        transition.sourceEndpoint.attribution,
        transition.sourceEndpoint.condition,
        transition.sourceEndpoint.navigationTarget,
        conditions
      )
    )
  }

  private def _project_transition_adjunct(
    adjunct: StateMachineProjectionTransitionAdjunct,
    transitions: Vector[StateMachineProjectionTransition]
  ): StateMachineProjectedTransitionAdjunct = {
    val transition = transitions.find(_.semanticTransitionId == adjunct.transitionSemanticTargetId).get
    val conditions = Vector(transition.condition, adjunct.condition, adjunct.adjunctEndpoint.condition)
    StateMachineProjectedTransitionAdjunct(
      adjunct,
      _admitted_navigation_target(
        Some(adjunct.adjunctEndpoint.semanticTargetId),
        adjunct.adjunctEndpoint.component,
        adjunct.adjunctEndpoint.attribution,
        adjunct.adjunctEndpoint.condition,
        adjunct.adjunctEndpoint.navigationTarget,
        conditions
      )
    )
  }

  private def _project_gap(
    gap: StateMachineProjectionGap
  ): StateMachineProjectedGap =
    StateMachineProjectedGap(
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
    subjects: Vector[StateMachineProjectionSubject]
  ): Vector[StateMachineProjectionSubject] =
    subjects.sortBy { subject =>
      (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))
    }

  private def _order_transitions(
    transitions: Vector[StateMachineProjectionTransition]
  ): Vector[StateMachineProjectionTransition] =
    transitions.sortBy { transition =>
      (
        transition.stateMachineSemanticTargetId.value,
        transition.sourceEndpoint.semanticTargetId.value,
        transition.targetEndpoint.semanticTargetId.value,
        transition.semanticTransitionId.value,
        transition.id,
        transition.stableTieKey.getOrElse("")
      )
    }

  private def _order_transition_adjuncts(
    adjuncts: Vector[StateMachineProjectionTransitionAdjunct]
  ): Vector[StateMachineProjectionTransitionAdjunct] =
    adjuncts.sortBy { adjunct =>
      (
        adjunct.transitionSemanticTargetId.value,
        adjunct.adjunctEndpoint.semanticTargetId.value,
        adjunct.semanticAdjunctRelationId.value,
        adjunct.id,
        adjunct.stableTieKey.getOrElse("")
      )
    }

  private def _order_gaps(
    gaps: Vector[StateMachineProjectionGap]
  ): Vector[StateMachineProjectionGap] =
    gaps.sortBy { gap =>
      (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse(""))
    }

  private def _order_projected_transitions(
    transitions: Vector[StateMachineProjectedTransition]
  ): Vector[StateMachineProjectedTransition] =
    transitions.sortBy { transition =>
      val source = transition.sourceTransition
      (
        source.stateMachineSemanticTargetId.value,
        source.sourceEndpoint.semanticTargetId.value,
        source.targetEndpoint.semanticTargetId.value,
        source.semanticTransitionId.value,
        source.id,
        source.stableTieKey.getOrElse("")
      )
    }

  private def _subject_role(role: StateMachineProjectionRole): Boolean =
    role == StateMachineProjectionStateMachine ||
      role == StateMachineProjectionState ||
      _adjunct_subject_role(role)

  private def _adjunct_subject_role(role: StateMachineProjectionRole): Boolean =
    role == StateMachineProjectionTrigger ||
      role == StateMachineProjectionGuard ||
      role == StateMachineProjectionAction ||
      role == StateMachineProjectionActivity ||
      role == StateMachineProjectionOperation ||
      role == StateMachineProjectionEvent ||
      role == StateMachineProjectionRule

  private def _transition_role(role: StateMachineProjectionRole): Boolean =
    role == StateMachineProjectionTransitionRelation

  private def _transition_adjunct_role(role: StateMachineProjectionRole): Boolean =
    role == StateMachineProjectionTransitionAdjunctRelation

  private def _gap_role(role: StateMachineProjectionRole): Boolean =
    _subject_role(role) || _transition_role(role) || _transition_adjunct_role(role)

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
