package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait MonoKotoSemanticBridgeTargetCategory

case object Aggregate extends MonoKotoSemanticBridgeTargetCategory

case object Entity extends MonoKotoSemanticBridgeTargetCategory

case object Value extends MonoKotoSemanticBridgeTargetCategory

case object StructuralRelation extends MonoKotoSemanticBridgeTargetCategory

case object Command extends MonoKotoSemanticBridgeTargetCategory

case object Event extends MonoKotoSemanticBridgeTargetCategory

case object WorkflowActivity extends MonoKotoSemanticBridgeTargetCategory

case object StateEffect extends MonoKotoSemanticBridgeTargetCategory

final case class MonoKotoSemanticBridgeTarget(
  identity: ComponentDashboardSemanticTargetIdentity,
  category: MonoKotoSemanticBridgeTargetCategory,
  component: ComponentDashboardComponentIdentity,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoSemanticBridgeAdmission(
  id: String,
  sourceSubjectId: String,
  sourceReferenceId: String,
  target: MonoKotoSemanticBridgeTarget,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class MonoKotoSemanticBridgeRelation(
  id: String,
  sourceSubject: MonoKotoProjectedSubject,
  sourceReference: MonoKotoProjectedReference,
  target: MonoKotoSemanticBridgeTarget,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoSemanticBridgeForwardGroup(
  sourceSubject: MonoKotoProjectedSubject,
  relations: Vector[MonoKotoSemanticBridgeRelation]
)

final case class MonoKotoSemanticBridgeReverseGroup(
  target: MonoKotoSemanticBridgeTarget,
  relations: Vector[MonoKotoSemanticBridgeRelation]
)

final case class MonoKotoSemanticBridgeFailure(violations: Vector[String])

final case class MonoKotoSemanticBridge(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  relations: Vector[MonoKotoSemanticBridgeRelation],
  forwardGroups: Vector[MonoKotoSemanticBridgeForwardGroup],
  reverseGroups: Vector[MonoKotoSemanticBridgeReverseGroup]
)

object MonoKotoSemanticBridge {
  def create(
    projection: MonoKotoProjection,
    admissions: Vector[MonoKotoSemanticBridgeAdmission]
  ): Either[MonoKotoSemanticBridgeFailure, MonoKotoSemanticBridge] = {
    val violations = Vector.newBuilder[String]

    _validate_projection(projection, violations)
    _validate_duplicate_admission_ids(admissions, violations)
    admissions.foreach(_validate_admission(_, projection, violations))
    _validate_conflicting_target_descriptors(admissions, violations)

    val result = violations.result()
    if (result.nonEmpty)
      Left(MonoKotoSemanticBridgeFailure(result))
    else {
      val relations = _order_relations(admissions.map(_relation_from(_, projection)))
      Right(
        MonoKotoSemanticBridge(
          projection.context,
          projection.component,
          relations,
          _forward_groups(relations),
          _reverse_groups(relations)
        )
      )
    }
  }

  private def _validate_projection(
    projection: MonoKotoProjection,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(projection.context.value, "Mono-Koto semantic bridge bounded CCDM context identity", violations)
    _require_nonblank(projection.component.value, "Mono-Koto semantic bridge Component identity", violations)
    _duplicate_ids(projection.subjects.map(_.sourceSubject.id)).foreach { id =>
      violations += s"Duplicate Mono-Koto semantic bridge source-subject identity '$id'."
    }
    _duplicate_ids(projection.subjects.map(_.sourceSubject.semanticTargetId.value)).foreach { id =>
      violations += s"Duplicate Mono-Koto semantic bridge source-subject semantic target identity '$id'."
    }
    projection.subjects.foreach(_validate_source_subject(_, projection.component, violations))
    _validate_source_subject_order(projection.subjects, violations)
  }

  private def _validate_source_subject(
    subject: MonoKotoProjectedSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val source = subject.sourceSubject
    _require_nonblank(source.id, "Mono-Koto semantic bridge source-subject identity", violations)
    _require_nonblank(source.component.value, s"Mono-Koto semantic bridge source subject '${source.id}' Component identity", violations)
    if (source.component != component)
      violations += s"Mono-Koto semantic bridge source subject '${source.id}' is outside its exact Projection Component scope."
    _require_nonblank(source.semanticTargetId.value, s"Mono-Koto semantic bridge source subject '${source.id}' semantic target identity", violations)
    _require_nonblank(source.stakeholderLabel, s"Mono-Koto semantic bridge source subject '${source.id}' stakeholder label", violations)
    _validate_attribution(source.attribution, s"Mono-Koto semantic bridge source subject '${source.id}'", violations)
    _validate_condition(source.condition, s"Mono-Koto semantic bridge source subject '${source.id}'", violations)
    _validate_tie_key(source.presentationTieKey, s"Mono-Koto semantic bridge source subject '${source.id}'", violations)
    _validate_navigation_target(source.semanticTargetId, source.component, source.navigationTarget, s"Mono-Koto semantic bridge source subject '${source.id}'", violations)
    _validate_projected_navigation(
      source.navigationTarget,
      subject.navigationTarget,
      source.semanticTargetId,
      source.component,
      source.condition,
      s"Mono-Koto semantic bridge source subject '${source.id}'",
      violations
    )

    val references = source.references
    _duplicate_ids(references.map(_.id)).foreach { id =>
      violations += s"Mono-Koto semantic bridge source subject '${source.id}' has duplicate source-reference identity '$id'."
    }
    _duplicate_ids(references.map(_.semanticTargetId.value)).foreach { id =>
      violations += s"Mono-Koto semantic bridge source subject '${source.id}' has duplicate source-reference semantic target identity '$id'."
    }
    references.foreach(_validate_source_reference(_, source, violations))
    if (subject.references.size != references.size)
      violations += s"Mono-Koto semantic bridge source subject '${source.id}' projection must retain every source reference exactly once."
    subject.references.foreach { projectedreference =>
      val reference = projectedreference.sourceReference
      if (references.count(_ == reference) != 1)
        violations += s"Mono-Koto semantic bridge source subject '${source.id}' projection must retain every source reference exactly once."
      else
        _validate_projected_navigation(
          reference.navigationTarget,
          projectedreference.navigationTarget,
          reference.semanticTargetId,
          reference.component,
          reference.condition,
          s"Mono-Koto semantic bridge source reference '${reference.id}'",
          violations
        )
    }
    references.foreach { reference =>
      if (subject.references.map(_.sourceReference).count(_ == reference) != 1)
        violations += s"Mono-Koto semantic bridge source subject '${source.id}' projection must retain every source reference exactly once."
    }
    _validate_source_reference_order(subject, violations)
  }

  private def _validate_source_reference(
    source: MonoKotoProjectionReference,
    subject: MonoKotoProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(source.id, s"Mono-Koto semantic bridge source subject '${subject.id}' source-reference identity", violations)
    _require_nonblank(source.component.value, s"Mono-Koto semantic bridge source reference '${source.id}' Component identity", violations)
    if (source.component != subject.component)
      violations += s"Mono-Koto semantic bridge source reference '${source.id}' is outside source subject '${subject.id}' exact Component scope."
    _require_nonblank(source.semanticTargetId.value, s"Mono-Koto semantic bridge source reference '${source.id}' semantic target identity", violations)
    _validate_attribution(source.attribution, s"Mono-Koto semantic bridge source reference '${source.id}'", violations)
    _validate_condition(source.condition, s"Mono-Koto semantic bridge source reference '${source.id}'", violations)
    _validate_tie_key(source.presentationTieKey, s"Mono-Koto semantic bridge source reference '${source.id}'", violations)
    if (!_compatible_reference_kind(subject.projectionKind, source.referenceKind))
      violations += s"Mono-Koto semantic bridge source subject '${subject.id}' reference '${source.id}' is outside its structural or behavioral partition."
    _validate_navigation_target(source.semanticTargetId, source.component, source.navigationTarget, s"Mono-Koto semantic bridge source reference '${source.id}'", violations)
  }

  private def _validate_projected_navigation(
    sourcetarget: Option[ComponentDashboardNavigationTarget],
    projectedtarget: Option[ComponentDashboardNavigationTarget],
    semantictargetid: ComponentDashboardSemanticTargetIdentity,
    component: ComponentDashboardComponentIdentity,
    condition: ComponentDashboardCondition,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val sourcetargetisadmitted = sourcetarget.exists { target =>
      target.targetIdentity == semantictargetid &&
        target.targetComponent == component &&
        target.implementedAndUsable &&
        _usable_condition(condition) &&
        _usable_condition(target.targetCondition)
    }
    if (sourcetargetisadmitted) {
      projectedtarget match {
        case Some(target) if target == sourcetarget.get =>
        case Some(_) =>
          violations += s"$subject projected navigation target must retain the exact admitted target."
        case None =>
          violations += s"$subject projected navigation target must retain the admitted target."
      }
    } else if (projectedtarget.nonEmpty) {
      violations += s"$subject projected navigation target must not admit a disqualified target."
    }
  }

  private def _validate_source_subject_order(
    subjects: Vector[MonoKotoProjectedSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    subjects.sliding(2).foreach {
      case Vector(previous, current) if !_source_subject_precedes(previous.sourceSubject, current.sourceSubject) =>
        violations += "Mono-Koto semantic bridge source subjects must retain their admitted Mono-then-Koto identity order."
      case _ =>
    }

  private def _validate_source_reference_order(
    subject: MonoKotoProjectedSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    subject.references.sliding(2).foreach {
      case Vector(previous, current) if !_source_reference_precedes(previous.sourceReference, current.sourceReference) =>
        violations += s"Mono-Koto semantic bridge source subject '${subject.sourceSubject.id}' references must retain their admitted identity order."
      case _ =>
    }

  private def _source_subject_precedes(
    previous: MonoKotoProjectionSubject,
    current: MonoKotoProjectionSubject
  ): Boolean = {
    val previouskind = MonoKotoProjectionKind.presentationSequence.indexOf(previous.projectionKind)
    val currentkind = MonoKotoProjectionKind.presentationSequence.indexOf(current.projectionKind)
    previouskind < currentkind ||
      (previouskind == currentkind && (
        previous.semanticTargetId.value < current.semanticTargetId.value ||
          (previous.semanticTargetId.value == current.semanticTargetId.value && (
            previous.id < current.id ||
              (previous.id == current.id && previous.presentationTieKey.getOrElse("") <= current.presentationTieKey.getOrElse(""))
          ))
      ))
  }

  private def _source_reference_precedes(
    previous: MonoKotoProjectionReference,
    current: MonoKotoProjectionReference
  ): Boolean =
    previous.semanticTargetId.value < current.semanticTargetId.value ||
      (previous.semanticTargetId.value == current.semanticTargetId.value && (
        previous.id < current.id ||
          (previous.id == current.id && previous.presentationTieKey.getOrElse("") <= current.presentationTieKey.getOrElse(""))
      ))

  private def _validate_duplicate_admission_ids(
    admissions: Vector[MonoKotoSemanticBridgeAdmission],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(admissions.map(_.id)).foreach { id =>
      violations += s"Duplicate Mono-Koto semantic bridge admission identity '$id'."
    }

  private def _validate_admission(
    admission: MonoKotoSemanticBridgeAdmission,
    projection: MonoKotoProjection,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(admission.id, "Mono-Koto semantic bridge admission identity", violations)
    _require_nonblank(admission.sourceSubjectId, s"Mono-Koto semantic bridge admission '${admission.id}' source-subject identity", violations)
    _require_nonblank(admission.sourceReferenceId, s"Mono-Koto semantic bridge admission '${admission.id}' source-reference identity", violations)
    _validate_target(admission.target, s"Mono-Koto semantic bridge admission '${admission.id}' target", violations)
    _validate_attribution(admission.attribution, s"Mono-Koto semantic bridge admission '${admission.id}' relation", violations)
    _validate_condition(admission.condition, s"Mono-Koto semantic bridge admission '${admission.id}' relation", violations)
    _validate_tie_key(admission.stableTieKey, s"Mono-Koto semantic bridge admission '${admission.id}' relation", violations)

    _source_subject(projection, admission.sourceSubjectId) match {
      case None =>
        violations += s"Mono-Koto semantic bridge admission '${admission.id}' does not name an admitted exact source subject."
      case Some(subject) =>
        _source_reference(subject, admission.sourceReferenceId) match {
          case None =>
            violations += s"Mono-Koto semantic bridge admission '${admission.id}' does not name an admitted exact source reference."
          case Some(reference) =>
            if (admission.target.identity != reference.sourceReference.semanticTargetId)
              violations += s"Mono-Koto semantic bridge admission '${admission.id}' target must retain its exact source-reference semantic target identity."
            if (admission.target.component != reference.sourceReference.component)
              violations += s"Mono-Koto semantic bridge admission '${admission.id}' target must remain within its exact source-reference Component scope."
            if (!_compatible_category(subject.sourceSubject.projectionKind, admission.target.category))
              violations += s"Mono-Koto semantic bridge admission '${admission.id}' target category is incompatible with source subject '${subject.sourceSubject.id}'."
        }
    }
  }

  private def _validate_target(
    target: MonoKotoSemanticBridgeTarget,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(target.identity.value, s"$subject identity", violations)
    _require_nonblank(target.component.value, s"$subject Component identity", violations)
    _validate_attribution(target.attribution, subject, violations)
    _validate_condition(target.condition, subject, violations)
    _validate_tie_key(target.stableTieKey, subject, violations)
    _validate_navigation_target(target.identity, target.component, target.navigationTarget, subject, violations)
  }

  private def _validate_conflicting_target_descriptors(
    admissions: Vector[MonoKotoSemanticBridgeAdmission],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    admissions.groupBy(_.target.identity).foreach { case (identity, values) =>
      if (values.map(_.target).distinct.size > 1)
        violations += s"Mono-Koto semantic bridge target identity '${identity.value}' has contradictory admitted descriptors."
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
    condition.redaction.foreach(_require_nonblank(_, s"$subject condition redaction", violations))
    condition.explicitAbsence.foreach(_require_nonblank(_, s"$subject condition explicit absence", violations))
    condition.ambiguity.foreach(_require_nonblank(_, s"$subject condition ambiguity", violations))
    condition.conflict.foreach(_require_nonblank(_, s"$subject condition conflict", violations))
    condition.staleness.foreach(_require_nonblank(_, s"$subject condition staleness", violations))
    condition.malformedEvidence.foreach(_require_nonblank(_, s"$subject condition malformed evidence", violations))
    condition.limitations.foreach(_require_nonblank(_, s"$subject condition limitation", violations))
  }

  private def _validate_tie_key(
    tiekey: Option[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    tiekey.foreach(_require_nonblank(_, s"$subject stable non-semantic tie key", violations))

  private def _compatible_reference_kind(
    projectionkind: MonoKotoProjectionKind,
    referencekind: MonoKotoProjectionReferenceKind
  ): Boolean =
    (projectionkind == Mono && referencekind == StructuralDomain) ||
      (projectionkind == Koto && referencekind == BehavioralTemporal)

  private def _compatible_category(
    projectionkind: MonoKotoProjectionKind,
    category: MonoKotoSemanticBridgeTargetCategory
  ): Boolean =
    (projectionkind == Mono && Vector(Aggregate, Entity, Value, StructuralRelation).contains(category)) ||
      (projectionkind == Koto && Vector(Command, Event, WorkflowActivity, StateEffect).contains(category))

  private def _source_subject(
    projection: MonoKotoProjection,
    subjectid: String
  ): Option[MonoKotoProjectedSubject] =
    projection.subjects.find(_.sourceSubject.id == subjectid)

  private def _source_reference(
    subject: MonoKotoProjectedSubject,
    referenceid: String
  ): Option[MonoKotoProjectedReference] =
    subject.references.find(_.sourceReference.id == referenceid)

  private def _relation_from(
    admission: MonoKotoSemanticBridgeAdmission,
    projection: MonoKotoProjection
  ): MonoKotoSemanticBridgeRelation = {
    val subject = _source_subject(projection, admission.sourceSubjectId).get
    val reference = _source_reference(subject, admission.sourceReferenceId).get
    MonoKotoSemanticBridgeRelation(
      admission.id,
      subject,
      reference,
      admission.target,
      admission.attribution,
      admission.condition,
      admission.stableTieKey,
      _admitted_navigation_target(subject, reference, admission)
    )
  }

  private def _admitted_navigation_target(
    subject: MonoKotoProjectedSubject,
    reference: MonoKotoProjectedReference,
    admission: MonoKotoSemanticBridgeAdmission
  ): Option[ComponentDashboardNavigationTarget] =
    admission.target.navigationTarget.filter { target =>
      target.targetIdentity == admission.target.identity &&
      target.targetComponent == admission.target.component &&
      target.implementedAndUsable &&
      _usable_condition(subject.sourceSubject.condition) &&
      _usable_condition(reference.sourceReference.condition) &&
      _usable_condition(admission.target.condition) &&
      _usable_condition(admission.condition) &&
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
    relations: Vector[MonoKotoSemanticBridgeRelation]
  ): Vector[MonoKotoSemanticBridgeRelation] =
    relations.sortBy { relation =>
      (
        relation.sourceSubject.sourceSubject.id,
        relation.target.identity.value,
        relation.id,
        relation.stableTieKey.getOrElse("")
      )
    }

  private def _forward_groups(
    relations: Vector[MonoKotoSemanticBridgeRelation]
  ): Vector[MonoKotoSemanticBridgeForwardGroup] =
    relations.map(_.sourceSubject.sourceSubject.id).distinct.sorted.map { subjectid =>
      val groupedrelations = relations.filter(_.sourceSubject.sourceSubject.id == subjectid)
      MonoKotoSemanticBridgeForwardGroup(groupedrelations.head.sourceSubject, groupedrelations)
    }

  private def _reverse_groups(
    relations: Vector[MonoKotoSemanticBridgeRelation]
  ): Vector[MonoKotoSemanticBridgeReverseGroup] =
    relations.map(_.target.identity).distinct.sortBy(_.value).map { identity =>
      val groupedrelations = _order_reverse_relations(relations.filter(_.target.identity == identity))
      MonoKotoSemanticBridgeReverseGroup(groupedrelations.head.target, groupedrelations)
    }

  private def _order_reverse_relations(
    relations: Vector[MonoKotoSemanticBridgeRelation]
  ): Vector[MonoKotoSemanticBridgeRelation] =
    relations.sortBy { relation =>
      (
        relation.target.identity.value,
        relation.sourceSubject.sourceSubject.id,
        relation.id,
        relation.stableTieKey.getOrElse("")
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
