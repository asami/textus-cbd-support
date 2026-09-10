package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final case class WorkflowWebOverviewSubject(
  sourceSubjectId: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: WorkflowProjectionRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowWebOverviewFlowEndpoint(
  sourceEndpointId: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: WorkflowProjectionFlowEndpointRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class WorkflowWebOverviewFlow(
  sourceFlowId: String,
  component: ComponentDashboardComponentIdentity,
  semanticFlowId: ComponentDashboardSemanticTargetIdentity,
  workflowSemanticTargetId: ComponentDashboardSemanticTargetIdentity,
  role: WorkflowProjectionRole,
  direction: WorkflowProjectionFlowDirection,
  sourceOwnedSequenceKey: WorkflowProjectionSourceOwnedSequenceKey,
  sourceEndpoint: WorkflowWebOverviewFlowEndpoint,
  targetEndpoint: WorkflowWebOverviewFlowEndpoint,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class WorkflowWebOverviewGap(
  sourceGapId: String,
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

final case class WorkflowWebOverviewFailure(violations: Vector[String])

final case class WorkflowWebOverview(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[WorkflowWebOverviewSubject],
  flows: Vector[WorkflowWebOverviewFlow],
  gaps: Vector[WorkflowWebOverviewGap],
  forwardFlows: Vector[WorkflowWebOverviewFlow],
  reverseFlows: Vector[WorkflowWebOverviewFlow]
)

object WorkflowWebOverview {
  def create(
    webEntry: ComponentDashboardWebEntry,
    projection: WorkflowProjection
  ): Either[WorkflowWebOverviewFailure, WorkflowWebOverview] = {
    val violations = Vector.newBuilder[String]
    val deliveredcomponent = webEntry.dashboard.component

    _validate_component(deliveredcomponent, "Workflow Web overview delivered Component identity", violations)
    _validate_component(projection.component, "Workflow Web overview Projection Component identity", violations)
    if (deliveredcomponent != projection.component)
      violations += s"Workflow Web overview delivered Component '${deliveredcomponent.value}' does not match Projection Component '${projection.component.value}'."

    _validate_projection_integrity(projection, violations)
    val result = violations.result()
    if (result.nonEmpty)
      Left(WorkflowWebOverviewFailure(result))
    else
      Right(
        WorkflowWebOverview(
          projection.context,
          deliveredcomponent,
          projection.subjects.map(_assemble_subject),
          projection.flows.map(_assemble_flow),
          projection.gaps.map(_assemble_gap),
          projection.forwardFlows.map(_assemble_flow),
          projection.reverseFlows.map(_assemble_flow)
        )
      )
  }

  private def _validate_projection_integrity(
    projection: WorkflowProjection,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(projection.context.value, "Workflow Projection bounded CCDM context identity", violations)
    val projectedsubjects = projection.subjects
    val projectedflows = projection.flows
    val projectedgaps = projection.gaps
    val subjects = projectedsubjects.map(_.sourceSubject)
    val flows = projectedflows.map(_.sourceFlow)
    val gaps = projectedgaps.map(_.sourceGap)

    _validate_duplicate_record_ids(subjects, flows, gaps, violations)
    _validate_duplicate_semantic_ids(subjects, flows, violations)
    projectedsubjects.foreach(_validate_subject(_, projection.component, violations))
    projectedflows.foreach(_validate_flow(_, projection.component, subjects, violations))
    projectedgaps.foreach(_validate_gap(_, projection.component, subjects, flows, violations))
    _validate_subject_order(projectedsubjects, violations)
    _validate_flow_order(projectedflows, violations)
    _validate_gap_order(projectedgaps, violations)
    if (projection.forwardFlows != projectedflows)
      violations += "Workflow Projection forward flow index must retain every projected flow in canonical order."
    if (projection.reverseFlows != projectedflows)
      violations += "Workflow Projection reverse flow index must retain every projected flow in canonical order."
  }

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
    projectedsubject: WorkflowProjectedSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val subject = projectedsubject.sourceSubject
    _require_nonblank(subject.id, "Workflow subject identity", violations)
    _validate_component(subject.component, s"Workflow subject '${subject.id}' Component identity", violations)
    if (subject.component != component)
      violations += s"Workflow subject '${subject.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(subject.semanticTargetId.value, s"Workflow subject '${subject.id}' semantic identity", violations)
    if (!_subject_role(subject.role))
      violations += s"Workflow subject '${subject.id}' has flow relation role ${subject.role}."
    _validate_attribution(subject.attribution, s"Workflow subject '${subject.id}'", violations)
    _validate_condition(subject.condition, s"Workflow subject '${subject.id}'", violations)
    _validate_tie_key(subject.stableTieKey, s"Workflow subject '${subject.id}'", violations)
    _validate_projection_navigation(
      subject.navigationTarget,
      projectedsubject.navigationTarget,
      Some(subject.semanticTargetId),
      subject.component,
      subject.attribution,
      subject.condition,
      Vector(subject.condition),
      s"Workflow subject '${subject.id}'",
      violations
    )
  }

  private def _validate_flow(
    projectedflow: WorkflowProjectedFlow,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[WorkflowProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val flow = projectedflow.sourceFlow
    _require_nonblank(flow.id, "Workflow flow identity", violations)
    _validate_component(flow.component, s"Workflow flow '${flow.id}' Component identity", violations)
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
    val conditions = Vector(flow.condition, flow.sourceEndpoint.condition, flow.targetEndpoint.condition)
    _validate_projection_navigation(
      flow.targetEndpoint.navigationTarget,
      projectedflow.forwardNavigationTarget,
      Some(flow.targetEndpoint.semanticTargetId),
      flow.targetEndpoint.component,
      flow.targetEndpoint.attribution,
      flow.targetEndpoint.condition,
      conditions,
      s"Workflow flow '${flow.id}' forward endpoint '${flow.targetEndpoint.id}'",
      violations
    )
    _validate_projection_navigation(
      flow.sourceEndpoint.navigationTarget,
      projectedflow.reverseNavigationTarget,
      Some(flow.sourceEndpoint.semanticTargetId),
      flow.sourceEndpoint.component,
      flow.sourceEndpoint.attribution,
      flow.sourceEndpoint.condition,
      conditions,
      s"Workflow flow '${flow.id}' reverse endpoint '${flow.sourceEndpoint.id}'",
      violations
    )
  }

  private def _validate_endpoint(
    endpoint: WorkflowProjectionFlowEndpoint,
    flow: WorkflowProjectionFlow,
    position: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(endpoint.id, s"Workflow flow '${flow.id}' $position endpoint identity", violations)
    _validate_component(endpoint.component, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}' Component identity", violations)
    if (endpoint.component != flow.component)
      violations += s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}' is outside its exact Component scope."
    _require_nonblank(endpoint.semanticTargetId.value, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}' semantic identity", violations)
    _require_nonblank(endpoint.role.value, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}' role", violations)
    _validate_attribution(endpoint.attribution, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_condition(endpoint.condition, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}'", violations)
    _validate_tie_key(endpoint.stableTieKey, s"Workflow flow '${flow.id}' $position endpoint '${endpoint.id}'", violations)
  }

  private def _validate_gap(
    projectedgap: WorkflowProjectedGap,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[WorkflowProjectionSubject],
    flows: Vector[WorkflowProjectionFlow],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val gap = projectedgap.sourceGap
    _require_nonblank(gap.id, "Workflow gap identity", violations)
    _validate_component(gap.component, s"Workflow gap '${gap.id}' Component identity", violations)
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
    _validate_projection_navigation(
      gap.navigationTarget,
      projectedgap.navigationTarget,
      gap.affectedSemanticTargetId,
      gap.component,
      gap.attribution,
      gap.condition,
      Vector(gap.condition),
      s"Workflow gap '${gap.id}'",
      violations
    )
  }

  private def _validate_projection_navigation(
    sourcetarget: Option[ComponentDashboardNavigationTarget],
    projectedtarget: Option[ComponentDashboardNavigationTarget],
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    attribution: ComponentDashboardSourceAttribution,
    condition: ComponentDashboardCondition,
    conditions: Vector[ComponentDashboardCondition],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    sourcetarget.foreach { target =>
      _validate_navigation_target(target, identity, component, attribution, condition, subject, violations)
    }
    val admittedtarget = sourcetarget.filter { target =>
      identity.contains(target.targetIdentity) &&
        target.targetComponent == component &&
        target.targetAttribution == attribution &&
        target.targetCondition == condition &&
        target.implementedAndUsable &&
        conditions.forall(_usable_condition) &&
        _usable_condition(target.targetCondition)
    }
    if (projectedtarget != admittedtarget)
      violations += s"$subject projected navigation target must retain only the exact admitted target."
  }

  private def _validate_navigation_target(
    target: ComponentDashboardNavigationTarget,
    identity: Option[ComponentDashboardSemanticTargetIdentity],
    component: ComponentDashboardComponentIdentity,
    attribution: ComponentDashboardSourceAttribution,
    condition: ComponentDashboardCondition,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(target.targetIdentity.value, s"$subject navigation target identity", violations)
    if (!identity.contains(target.targetIdentity))
      violations += s"$subject navigation target must retain its exact receiving endpoint identity."
    _validate_component(target.targetComponent, s"$subject navigation target Component identity", violations)
    if (target.targetComponent != component)
      violations += s"$subject navigation target must remain within its exact Component scope."
    _validate_attribution(target.targetAttribution, s"$subject navigation target", violations)
    if (target.targetAttribution != attribution)
      violations += s"$subject navigation target must retain its exact receiving endpoint attribution."
    _validate_condition(target.targetCondition, s"$subject navigation target", violations)
    if (target.targetCondition != condition)
      violations += s"$subject navigation target must retain its exact receiving endpoint condition."
  }

  private def _validate_subject_order(
    subjects: Vector[WorkflowProjectedSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (subjects != subjects.sortBy { subject =>
      val source = subject.sourceSubject
      (source.semanticTargetId.value, source.id, source.stableTieKey.getOrElse(""))
    })
      violations += "Workflow Projection subjects must retain their admitted identity order."

  private def _validate_flow_order(
    flows: Vector[WorkflowProjectedFlow],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (flows != flows.sortBy { flow =>
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
    })
      violations += "Workflow Projection flows must retain their admitted Workflow and source-owned sequence order."

  private def _validate_gap_order(
    gaps: Vector[WorkflowProjectedGap],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (gaps != gaps.sortBy { gap =>
      val source = gap.sourceGap
      (source.affectedSemanticTargetId.map(_.value).getOrElse(""), source.id, source.stableTieKey.getOrElse(""))
    })
      violations += "Workflow Projection gaps must retain their admitted identity order."

  private def _assemble_subject(subject: WorkflowProjectedSubject): WorkflowWebOverviewSubject = {
    val source = subject.sourceSubject
    WorkflowWebOverviewSubject(
      source.id,
      source.component,
      source.semanticTargetId,
      source.role,
      source.attribution,
      source.condition,
      source.stableTieKey,
      subject.navigationTarget
    )
  }

  private def _assemble_flow_endpoint(endpoint: WorkflowProjectionFlowEndpoint): WorkflowWebOverviewFlowEndpoint =
    WorkflowWebOverviewFlowEndpoint(
      endpoint.id,
      endpoint.component,
      endpoint.semanticTargetId,
      endpoint.role,
      endpoint.attribution,
      endpoint.condition,
      endpoint.stableTieKey
    )

  private def _assemble_flow(flow: WorkflowProjectedFlow): WorkflowWebOverviewFlow = {
    val source = flow.sourceFlow
    WorkflowWebOverviewFlow(
      source.id,
      source.component,
      source.semanticFlowId,
      source.workflowSemanticTargetId,
      source.role,
      source.direction,
      source.sourceOwnedSequenceKey,
      _assemble_flow_endpoint(source.sourceEndpoint),
      _assemble_flow_endpoint(source.targetEndpoint),
      source.attribution,
      source.condition,
      source.stableTieKey,
      flow.forwardNavigationTarget,
      flow.reverseNavigationTarget
    )
  }

  private def _assemble_gap(gap: WorkflowProjectedGap): WorkflowWebOverviewGap = {
    val source = gap.sourceGap
    WorkflowWebOverviewGap(
      source.id,
      source.component,
      source.requestedRole,
      source.boundedFieldOrScope,
      source.affectedSemanticTargetId,
      source.attribution,
      source.condition,
      source.limitationReason,
      source.stableTieKey,
      gap.navigationTarget
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

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, subject, violations)

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
