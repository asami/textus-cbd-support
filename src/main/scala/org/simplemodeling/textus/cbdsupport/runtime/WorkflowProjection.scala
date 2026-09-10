package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait WorkflowProjectionRole

case object WorkflowProjectionWorkflow extends WorkflowProjectionRole

case object WorkflowProjectionActivity extends WorkflowProjectionRole

case object WorkflowProjectionParticipant extends WorkflowProjectionRole

case object WorkflowProjectionDomainElement extends WorkflowProjectionRole

case object WorkflowProjectionOperation extends WorkflowProjectionRole

case object WorkflowProjectionEvent extends WorkflowProjectionRole

case object WorkflowProjectionPublishedStateEffect extends WorkflowProjectionRole

case object WorkflowProjectionFlowRelation extends WorkflowProjectionRole

final case class WorkflowProjectionFlowEndpointRole(value: String)

final case class WorkflowProjectionFlowDirection(value: String)

final case class WorkflowProjectionSourceOwnedSequenceKey(value: String)

final case class WorkflowProjectionSubject(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: WorkflowProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowProjectionFlowEndpoint(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: WorkflowProjectionFlowEndpointRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowProjectionFlow(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticFlowId: ComponentDashboardSemanticTargetIdentity,
  workflowSemanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: WorkflowProjectionRole,
  direction: WorkflowProjectionFlowDirection,
  sourceOwnedSequenceKey: WorkflowProjectionSourceOwnedSequenceKey,
  sourceEndpoint: WorkflowProjectionFlowEndpoint,
  targetEndpoint: WorkflowProjectionFlowEndpoint,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class WorkflowProjectionGap(
  id: String,
  component: ComponentDashboardComponentIdentity,
  requestedRole: WorkflowProjectionRole,
  boundedFieldOrScope: String,
  affectedSemanticTargetId: Option[ComponentDashboardSemanticTargetIdentity],
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowProjectedSubject(
  sourceSubject: WorkflowProjectionSubject,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowProjectedFlow(
  sourceFlow: WorkflowProjectionFlow,
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowProjectedGap(
  sourceGap: WorkflowProjectionGap,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowProjectionFailure(violations: Vector[String])

final case class WorkflowProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[WorkflowProjectedSubject],
  flows: Vector[WorkflowProjectedFlow],
  gaps: Vector[WorkflowProjectedGap],
  forwardFlows: Vector[WorkflowProjectedFlow],
  reverseFlows: Vector[WorkflowProjectedFlow]
)

object WorkflowProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[WorkflowProjectionSubject],
    flows: Vector[WorkflowProjectionFlow],
    gaps: Vector[WorkflowProjectionGap]
  ): Either[WorkflowProjectionFailure, WorkflowProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_record_ids(subjects, flows, gaps, violations)
    _validate_duplicate_semantic_ids(subjects, flows, violations)
    subjects.foreach(subject => _validate_subject(subject, component, violations))
    flows.foreach(flow => _validate_flow(flow, component, subjects, violations))
    gaps.foreach(gap => _validate_gap(gap, component, subjects, flows, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(WorkflowProjectionFailure(result))
    else {
      val projectedsubjects = _order_subjects(subjects).map(_project_subject)
      val projectedflows = _order_flows(flows).map(_project_flow)
      val projectedgaps = _order_gaps(gaps).map(_project_gap)
      Right(
        WorkflowProjection(
          context,
          component,
          projectedsubjects,
          projectedflows,
          projectedgaps,
          _order_projected_flows(projectedflows),
          _order_projected_flows(projectedflows)
        )
      )
    }
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Workflow Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Workflow Projection Component identity", violations)

  private def _validate_duplicate_record_ids(
    subjects: Vector[WorkflowProjectionSubject],
    flows: Vector[WorkflowProjectionFlow],
    gaps: Vector[WorkflowProjectionGap],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val endpointids = flows.flatMap(flow => Vector(flow.sourceEndpoint.id, flow.targetEndpoint.id))
    _duplicate_ids(subjects.map(_.id) ++ flows.map(_.id) ++ endpointids ++ gaps.map(_.id)).foreach { id =>
      violations += s"Duplicate Workflow subject, flow, endpoint, or gap identity '$id'."
    }
  }

  private def _validate_duplicate_semantic_ids(
    subjects: Vector[WorkflowProjectionSubject],
    flows: Vector[WorkflowProjectionFlow],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.map(_.semanticTargetId.value) ++ flows.map(_.semanticFlowId.value)).foreach { id =>
      violations += s"Duplicate Workflow subject or flow semantic identity '$id'."
    }

  private def _validate_subject(
    subject: WorkflowProjectionSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(subject.id, "Workflow subject identity", violations)
    _require_nonblank(subject.component.value, s"Workflow subject '${subject.id}' Component identity", violations)
    if (subject.component != component)
      violations += s"Workflow subject '${subject.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(subject.semanticTargetId.value, s"Workflow subject '${subject.id}' semantic identity", violations)
    if (!_subject_role(subject.role))
      violations += s"Workflow subject '${subject.id}' has flow relation role ${subject.role}."
    _validate_attribution(subject.attribution, s"Workflow subject '${subject.id}'", violations)
    _validate_condition(subject.condition, s"Workflow subject '${subject.id}'", violations)
    _validate_tie_key(subject.stableTieKey, s"Workflow subject '${subject.id}'", violations)
    _validate_navigation_target(
      Some(subject.semanticTargetId),
      subject.component,
      subject.attribution,
      subject.condition,
      subject.navigationTarget,
      s"Workflow subject '${subject.id}'",
      violations
    )
  }

  private def _validate_flow(
    flow: WorkflowProjectionFlow,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[WorkflowProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(flow.id, "Workflow flow identity", violations)
    _require_nonblank(flow.component.value, s"Workflow flow '${flow.id}' Component identity", violations)
    if (flow.component != component)
      violations += s"Workflow flow '${flow.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(flow.semanticFlowId.value, s"Workflow flow '${flow.id}' semantic identity", violations)
    _require_nonblank(flow.workflowSemanticTargetId.value, s"Workflow flow '${flow.id}' Workflow semantic identity", violations)
    if (!_flow_role(flow.role))
      violations += s"Workflow flow '${flow.id}' has subject role ${flow.role}."
    if (!_retained_workflow(flow.workflowSemanticTargetId, subjects))
      violations += s"Workflow flow '${flow.id}' Workflow semantic identity '${flow.workflowSemanticTargetId.value}' is not a retained direct Workflow subject."
    _require_nonblank(flow.direction.value, s"Workflow flow '${flow.id}' direction", violations)
    _require_nonblank(flow.sourceOwnedSequenceKey.value, s"Workflow flow '${flow.id}' source-owned sequence key", violations)
    _validate_attribution(flow.attribution, s"Workflow flow '${flow.id}'", violations)
    _validate_condition(flow.condition, s"Workflow flow '${flow.id}'", violations)
    _validate_tie_key(flow.stableTieKey, s"Workflow flow '${flow.id}'", violations)
    _validate_endpoint(flow.sourceEndpoint, flow, "source", violations)
    _validate_endpoint(flow.targetEndpoint, flow, "target", violations)
  }

  private def _validate_endpoint(
    endpoint: WorkflowProjectionFlowEndpoint,
    flow: WorkflowProjectionFlow,
    position: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(endpoint.id, s"Workflow flow '${flow.id}' $position endpoint identity", violations)
    _require_nonblank(endpoint.component.value, s"Workflow flow '${flow.id}' $position endpoint Component identity", violations)
    if (endpoint.component != flow.component)
      violations += s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}' is outside its exact Component scope."
    _require_nonblank(endpoint.semanticTargetId.value, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}' semantic identity", violations)
    _require_nonblank(endpoint.role.value, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}' role", violations)
    _validate_attribution(endpoint.attribution, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_condition(endpoint.condition, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_tie_key(endpoint.stableTieKey, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_navigation_target(
      Some(endpoint.semanticTargetId),
      endpoint.component,
      endpoint.attribution,
      endpoint.condition,
      endpoint.navigationTarget,
      s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}'",
      violations
    )
  }

  private def _validate_gap(
    gap: WorkflowProjectionGap,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[WorkflowProjectionSubject],
    flows: Vector[WorkflowProjectionFlow],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "Workflow gap identity", violations)
    _require_nonblank(gap.component.value, s"Workflow gap '${gap.id}' Component identity", violations)
    if (gap.component != component)
      violations += s"Workflow gap '${gap.id}' is outside Projection Component '${component.value}'."
    if (!_gap_role(gap.requestedRole))
      violations += s"Workflow gap '${gap.id}' has invalid requested role ${gap.requestedRole}."
    _require_nonblank(gap.boundedFieldOrScope, s"Workflow gap '${gap.id}' bounded unsupported field or scope", violations)
    gap.affectedSemanticTargetId.foreach { identity =>
      _require_nonblank(identity.value, s"Workflow gap '${gap.id}' affected semantic identity", violations)
      if (!_known_identity(identity, subjects, flows))
        violations += s"Workflow gap '${gap.id}' affected semantic identity '${identity.value}' is not retained in this Projection."
    }
    _validate_attribution(gap.attribution, s"Workflow gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"Workflow gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"Workflow gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"Workflow gap '${gap.id}'", violations)
    _validate_navigation_target(
      gap.affectedSemanticTargetId,
      gap.component,
      gap.attribution,
      gap.condition,
      gap.navigationTarget,
      s"Workflow gap '${gap.id}'",
      violations
    )
  }

  private def _retained_workflow(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[WorkflowProjectionSubject]
  ): Boolean =
    subjects.exists(subject => subject.semanticTargetId == identity && subject.role == WorkflowProjectionWorkflow)

  private def _known_identity(
    identity: ComponentDashboardSemanticTargetIdentity,
    subjects: Vector[WorkflowProjectionSubject],
    flows: Vector[WorkflowProjectionFlow]
  ): Boolean =
    subjects.exists(_.semanticTargetId == identity) ||
      flows.exists(_.semanticFlowId == identity) ||
      flows.exists(flow =>
        flow.sourceEndpoint.semanticTargetId == identity || flow.targetEndpoint.semanticTargetId == identity
      )

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
    subject: WorkflowProjectionSubject
  ): WorkflowProjectedSubject =
    WorkflowProjectedSubject(
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

  private def _project_flow(
    flow: WorkflowProjectionFlow
  ): WorkflowProjectedFlow = {
    val conditions = Vector(flow.condition, flow.sourceEndpoint.condition, flow.targetEndpoint.condition)
    WorkflowProjectedFlow(
      flow,
      _admitted_navigation_target(
        Some(flow.targetEndpoint.semanticTargetId),
        flow.targetEndpoint.component,
        flow.targetEndpoint.attribution,
        flow.targetEndpoint.condition,
        flow.targetEndpoint.navigationTarget,
        conditions
      ),
      _admitted_navigation_target(
        Some(flow.sourceEndpoint.semanticTargetId),
        flow.sourceEndpoint.component,
        flow.sourceEndpoint.attribution,
        flow.sourceEndpoint.condition,
        flow.sourceEndpoint.navigationTarget,
        conditions
      )
    )
  }

  private def _project_gap(
    gap: WorkflowProjectionGap
  ): WorkflowProjectedGap =
    WorkflowProjectedGap(
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
    subjects: Vector[WorkflowProjectionSubject]
  ): Vector[WorkflowProjectionSubject] =
    subjects.sortBy(subject => (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse("")))

  private def _order_flows(
    flows: Vector[WorkflowProjectionFlow]
  ): Vector[WorkflowProjectionFlow] =
    flows.sortBy { flow =>
      (
        flow.workflowSemanticTargetId.value,
        flow.sourceOwnedSequenceKey.value,
        flow.semanticFlowId.value,
        flow.sourceEndpoint.semanticTargetId.value,
        flow.targetEndpoint.semanticTargetId.value,
        flow.id,
        flow.stableTieKey.getOrElse("")
      )
    }

  private def _order_gaps(
    gaps: Vector[WorkflowProjectionGap]
  ): Vector[WorkflowProjectionGap] =
    gaps.sortBy(gap => (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse("")))

  private def _order_projected_flows(
    flows: Vector[WorkflowProjectedFlow]
  ): Vector[WorkflowProjectedFlow] =
    flows.sortBy { flow =>
      val source = flow.sourceFlow
      (
        source.workflowSemanticTargetId.value,
        source.sourceOwnedSequenceKey.value,
        source.semanticFlowId.value,
        source.sourceEndpoint.semanticTargetId.value,
        source.targetEndpoint.semanticTargetId.value,
        source.id,
        source.stableTieKey.getOrElse("")
      )
    }

  private def _subject_role(role: WorkflowProjectionRole): Boolean =
    role == WorkflowProjectionWorkflow ||
      role == WorkflowProjectionActivity ||
      role == WorkflowProjectionParticipant ||
      role == WorkflowProjectionDomainElement ||
      role == WorkflowProjectionOperation ||
      role == WorkflowProjectionEvent ||
      role == WorkflowProjectionPublishedStateEffect

  private def _flow_role(role: WorkflowProjectionRole): Boolean =
    role == WorkflowProjectionFlowRelation

  private def _gap_role(role: WorkflowProjectionRole): Boolean =
    _subject_role(role) || _flow_role(role)

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
