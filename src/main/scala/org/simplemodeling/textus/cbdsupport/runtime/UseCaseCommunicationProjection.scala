package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait UseCaseCommunicationReferenceKind

case object Actor extends UseCaseCommunicationReferenceKind

case object Goal extends UseCaseCommunicationReferenceKind

case object Trigger extends UseCaseCommunicationReferenceKind

case object Postcondition extends UseCaseCommunicationReferenceKind

case object DomainElement extends UseCaseCommunicationReferenceKind

case object Collaborator extends UseCaseCommunicationReferenceKind

case object RealizingWorkflow extends UseCaseCommunicationReferenceKind

case object MonoKoto extends UseCaseCommunicationReferenceKind

final case class UseCaseCommunicationReference(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  referenceKind: UseCaseCommunicationReferenceKind,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationFlowStep(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  sequenceKey: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationFlow(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  steps: Vector[UseCaseCommunicationFlowStep],
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationProjectionSubject(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  references: Vector[UseCaseCommunicationReference],
  flows: Vector[UseCaseCommunicationFlow],
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationProjectedReference(
  sourceReference: UseCaseCommunicationReference,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationProjectedFlowStep(
  sourceFlowStep: UseCaseCommunicationFlowStep,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationProjectedFlow(
  sourceFlow: UseCaseCommunicationFlow,
  steps: Vector[UseCaseCommunicationProjectedFlowStep],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationProjectedSubject(
  sourceSubject: UseCaseCommunicationProjectionSubject,
  references: Vector[UseCaseCommunicationProjectedReference],
  flows: Vector[UseCaseCommunicationProjectedFlow],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class UseCaseCommunicationProjectionFailure(violations: Vector[String])

final case class UseCaseCommunicationProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[UseCaseCommunicationProjectedSubject]
)

object UseCaseCommunicationProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[UseCaseCommunicationProjectionSubject]
  ): Either[UseCaseCommunicationProjectionFailure, UseCaseCommunicationProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_subject_ids(subjects, violations)
    subjects.foreach(subject => _validate_subject(subject, component, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(UseCaseCommunicationProjectionFailure(result))
    else
      Right(
        UseCaseCommunicationProjection(
          context,
          component,
          _order_subjects(subjects).map(_project_subject)
        )
      )
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Use Case communication Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Use Case communication Projection Component identity", violations)

  private def _validate_duplicate_subject_ids(
    subjects: Vector[UseCaseCommunicationProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.map(_.id)).foreach { id =>
      violations += s"Duplicate Use Case communication subject identity '$id'."
    }

  private def _validate_subject(
    subject: UseCaseCommunicationProjectionSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(subject.id, "Use Case communication subject identity", violations)
    _require_nonblank(subject.component.value, s"Use Case communication subject '${subject.id}' Component identity", violations)
    if (subject.component != component)
      violations += s"Use Case communication subject '${subject.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(subject.semanticTargetId.value, s"Use Case communication subject '${subject.id}' semantic target identity", violations)
    _validate_attribution(subject.attribution, s"Use Case communication subject '${subject.id}'", violations)
    _validate_condition(subject.condition, s"Use Case communication subject '${subject.id}'", violations)
    _validate_tie_key(subject.stableTieKey, s"Use Case communication subject '${subject.id}'", violations)
    _validate_navigation_target(
      subject.semanticTargetId,
      subject.component,
      subject.navigationTarget,
      s"Use Case communication subject '${subject.id}'",
      violations
    )
    _validate_duplicate_reference_ids(subject, violations)
    subject.references.foreach(reference => _validate_reference(reference, subject, violations))
    _validate_duplicate_flow_ids(subject, violations)
    subject.flows.foreach(flow => _validate_flow(flow, subject, violations))
  }

  private def _validate_duplicate_reference_ids(
    subject: UseCaseCommunicationProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subject.references.map(_.id)).foreach { id =>
      violations += s"Use Case communication subject '${subject.id}' has duplicate reference identity '$id'."
    }

  private def _validate_reference(
    reference: UseCaseCommunicationReference,
    subject: UseCaseCommunicationProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(reference.id, s"Use Case communication subject '${subject.id}' reference identity", violations)
    _require_nonblank(reference.component.value, s"Use Case communication reference '${reference.id}' Component identity", violations)
    if (reference.component != subject.component)
      violations += s"Use Case communication reference '${reference.id}' is outside subject '${subject.id}' exact Component scope."
    _require_nonblank(reference.semanticTargetId.value, s"Use Case communication reference '${reference.id}' semantic target identity", violations)
    _require_nonblank(reference.semanticRelationshipId.value, s"Use Case communication reference '${reference.id}' semantic relationship identity", violations)
    _validate_attribution(reference.attribution, s"Use Case communication reference '${reference.id}'", violations)
    _validate_condition(reference.condition, s"Use Case communication reference '${reference.id}'", violations)
    _validate_tie_key(reference.stableTieKey, s"Use Case communication reference '${reference.id}'", violations)
    _validate_navigation_target(
      reference.semanticTargetId,
      reference.component,
      reference.navigationTarget,
      s"Use Case communication reference '${reference.id}'",
      violations
    )
  }

  private def _validate_duplicate_flow_ids(
    subject: UseCaseCommunicationProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subject.flows.map(_.id)).foreach { id =>
      violations += s"Use Case communication subject '${subject.id}' has duplicate flow identity '$id'."
    }

  private def _validate_flow(
    flow: UseCaseCommunicationFlow,
    subject: UseCaseCommunicationProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(flow.id, s"Use Case communication subject '${subject.id}' flow identity", violations)
    _require_nonblank(flow.component.value, s"Use Case communication flow '${flow.id}' Component identity", violations)
    if (flow.component != subject.component)
      violations += s"Use Case communication flow '${flow.id}' is outside subject '${subject.id}' exact Component scope."
    _require_nonblank(flow.semanticTargetId.value, s"Use Case communication flow '${flow.id}' semantic target identity", violations)
    _require_nonblank(flow.semanticRelationshipId.value, s"Use Case communication flow '${flow.id}' semantic relationship identity", violations)
    _validate_attribution(flow.attribution, s"Use Case communication flow '${flow.id}'", violations)
    _validate_condition(flow.condition, s"Use Case communication flow '${flow.id}'", violations)
    _validate_tie_key(flow.stableTieKey, s"Use Case communication flow '${flow.id}'", violations)
    _validate_navigation_target(
      flow.semanticTargetId,
      flow.component,
      flow.navigationTarget,
      s"Use Case communication flow '${flow.id}'",
      violations
    )
    _validate_duplicate_flow_step_ids(flow, violations)
    _validate_duplicate_sequence_keys(flow, violations)
    flow.steps.foreach(step => _validate_flow_step(step, flow, subject, violations))
  }

  private def _validate_duplicate_flow_step_ids(
    flow: UseCaseCommunicationFlow,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(flow.steps.map(_.id)).foreach { id =>
      violations += s"Use Case communication flow '${flow.id}' has duplicate flow-step identity '$id'."
    }

  private def _validate_duplicate_sequence_keys(
    flow: UseCaseCommunicationFlow,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(flow.steps.map(_.sequenceKey)).foreach { key =>
      violations += s"Use Case communication flow '${flow.id}' has unresolved duplicate source-owned sequence key '$key'."
    }

  private def _validate_flow_step(
    step: UseCaseCommunicationFlowStep,
    flow: UseCaseCommunicationFlow,
    subject: UseCaseCommunicationProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(step.id, s"Use Case communication flow '${flow.id}' flow-step identity", violations)
    _require_nonblank(step.component.value, s"Use Case communication flow-step '${step.id}' Component identity", violations)
    if (step.component != subject.component || step.component != flow.component)
      violations += s"Use Case communication flow-step '${step.id}' is outside flow '${flow.id}' exact Component scope."
    _require_nonblank(step.semanticTargetId.value, s"Use Case communication flow-step '${step.id}' semantic target identity", violations)
    _require_nonblank(step.semanticRelationshipId.value, s"Use Case communication flow-step '${step.id}' semantic relationship identity", violations)
    _require_nonblank(step.sequenceKey, s"Use Case communication flow-step '${step.id}' source-owned sequence key", violations)
    _validate_attribution(step.attribution, s"Use Case communication flow-step '${step.id}'", violations)
    _validate_condition(step.condition, s"Use Case communication flow-step '${step.id}'", violations)
    _validate_tie_key(step.stableTieKey, s"Use Case communication flow-step '${step.id}'", violations)
    _validate_navigation_target(
      step.semanticTargetId,
      step.component,
      step.navigationTarget,
      s"Use Case communication flow-step '${step.id}'",
      violations
    )
  }

  private def _validate_navigation_target(
    identity: ComponentDashboardSemanticTargetIdentity,
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    navigationtarget.foreach { target =>
      _require_nonblank(target.targetIdentity.value, s"$subject navigation target identity", violations)
      if (target.targetIdentity != identity)
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
    subject: UseCaseCommunicationProjectionSubject
  ): UseCaseCommunicationProjectedSubject =
    UseCaseCommunicationProjectedSubject(
      subject,
      _order_references(subject.references).map { reference =>
        UseCaseCommunicationProjectedReference(
          reference,
          _admitted_navigation_target(
            reference.semanticTargetId,
            reference.component,
            reference.navigationTarget,
            Vector(subject.condition, reference.condition)
          )
        )
      },
      _order_flows(subject.flows).map { flow =>
        UseCaseCommunicationProjectedFlow(
          flow,
          _order_flow_steps(flow.steps).map { step =>
            UseCaseCommunicationProjectedFlowStep(
              step,
              _admitted_navigation_target(
                step.semanticTargetId,
                step.component,
                step.navigationTarget,
                Vector(subject.condition, flow.condition, step.condition)
              )
            )
          },
          _admitted_navigation_target(
            flow.semanticTargetId,
            flow.component,
            flow.navigationTarget,
            Vector(subject.condition, flow.condition)
          )
        )
      },
      _admitted_navigation_target(
        subject.semanticTargetId,
        subject.component,
        subject.navigationTarget,
        Vector(subject.condition)
      )
    )

  private def _admitted_navigation_target(
    identity: ComponentDashboardSemanticTargetIdentity,
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    conditions: Vector[ComponentDashboardCondition]
  ): Option[ComponentDashboardNavigationTarget] =
    navigationtarget.filter { target =>
      target.targetIdentity == identity &&
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
    subjects: Vector[UseCaseCommunicationProjectionSubject]
  ): Vector[UseCaseCommunicationProjectionSubject] =
    subjects.sortBy { subject =>
      (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))
    }

  private def _order_references(
    references: Vector[UseCaseCommunicationReference]
  ): Vector[UseCaseCommunicationReference] =
    references.sortBy { reference =>
      (
        reference.semanticTargetId.value,
        reference.semanticRelationshipId.value,
        reference.id,
        reference.stableTieKey.getOrElse("")
      )
    }

  private def _order_flows(
    flows: Vector[UseCaseCommunicationFlow]
  ): Vector[UseCaseCommunicationFlow] =
    flows.sortBy { flow =>
      (
        flow.semanticTargetId.value,
        flow.semanticRelationshipId.value,
        flow.id,
        flow.stableTieKey.getOrElse("")
      )
    }

  private def _order_flow_steps(
    steps: Vector[UseCaseCommunicationFlowStep]
  ): Vector[UseCaseCommunicationFlowStep] =
    steps.sortBy { step =>
      (
        step.sequenceKey,
        step.semanticRelationshipId.value,
        step.id,
        step.stableTieKey.getOrElse("")
      )
    }

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
