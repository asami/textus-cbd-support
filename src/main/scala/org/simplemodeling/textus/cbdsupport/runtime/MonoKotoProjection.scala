package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait MonoKotoProjectionKind

case object Mono extends MonoKotoProjectionKind

case object Koto extends MonoKotoProjectionKind

object MonoKotoProjectionKind {
  val presentationSequence: Vector[MonoKotoProjectionKind] = Vector(Mono, Koto)
}

sealed trait MonoKotoProjectionReferenceKind

case object StructuralDomain extends MonoKotoProjectionReferenceKind

case object BehavioralTemporal extends MonoKotoProjectionReferenceKind

final case class MonoKotoProjectionReference(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  referenceKind: MonoKotoProjectionReferenceKind,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  presentationTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoProjectionSubject(
  id: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  projectionKind: MonoKotoProjectionKind,
  stakeholderLabel: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  references: Vector[MonoKotoProjectionReference],
  presentationTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoProjectedReference(
  sourceReference: MonoKotoProjectionReference,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoProjectedSubject(
  sourceSubject: MonoKotoProjectionSubject,
  references: Vector[MonoKotoProjectedReference],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoProjectionContextIdentity(value: String)

final case class MonoKotoProjectionFailure(violations: Vector[String])

final case class MonoKotoProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[MonoKotoProjectedSubject]
)

object MonoKotoProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[MonoKotoProjectionSubject]
  ): Either[MonoKotoProjectionFailure, MonoKotoProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_duplicate_subject_ids(subjects, violations)
    _validate_duplicate_subject_semantic_target_ids(subjects, violations)
    subjects.foreach(_validate_subject(_, component, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(MonoKotoProjectionFailure(result))
    else
      Right(
        MonoKotoProjection(
          context,
          component,
          _order_subjects(subjects).map { subject =>
            MonoKotoProjectedSubject(
              subject,
              _order_references(subject.references).map { reference =>
                MonoKotoProjectedReference(reference, _admitted_navigation_target(reference))
              },
              _admitted_navigation_target(subject)
            )
          }
        )
      )
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Mono-Koto Projection bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Mono-Koto Projection Component identity", violations)

  private def _validate_duplicate_subject_ids(
    subjects: Vector[MonoKotoProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.map(_.id)).foreach { id =>
      violations += s"Duplicate Mono-Koto subject identity '$id'."
    }

  private def _validate_duplicate_subject_semantic_target_ids(
    subjects: Vector[MonoKotoProjectionSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subjects.map(_.semanticTargetId.value)).foreach { id =>
      violations += s"Duplicate Mono-Koto subject semantic target identity '$id'."
    }

  private def _validate_subject(
    subject: MonoKotoProjectionSubject,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(subject.id, "Mono-Koto subject identity", violations)
    _require_nonblank(subject.component.value, s"Mono-Koto subject '${subject.id}' Component identity", violations)
    if (subject.component != component)
      violations += s"Mono-Koto subject '${subject.id}' is outside Projection Component '${component.value}'."
    _require_nonblank(subject.semanticTargetId.value, s"Mono-Koto subject '${subject.id}' semantic target identity", violations)
    _require_nonblank(subject.stakeholderLabel, s"Mono-Koto subject '${subject.id}' stakeholder label", violations)
    _validate_attribution(subject.attribution, s"Mono-Koto subject '${subject.id}'", violations)
    _validate_condition(subject.condition, s"Mono-Koto subject '${subject.id}'", violations)
    subject.presentationTieKey.foreach { tiekey =>
      _require_nonblank(tiekey, s"Mono-Koto subject '${subject.id}' presentation tie key", violations)
    }
    _validate_navigation_target(
      subject.semanticTargetId,
      subject.component,
      subject.navigationTarget,
      s"Mono-Koto subject '${subject.id}'",
      violations
    )
    _validate_duplicate_reference_ids(subject, violations)
    _validate_duplicate_reference_semantic_target_ids(subject, violations)
    subject.references.foreach(_validate_reference(_, subject, violations))
  }

  private def _validate_duplicate_reference_ids(
    subject: MonoKotoProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subject.references.map(_.id)).foreach { id =>
      violations += s"Mono-Koto subject '${subject.id}' has duplicate reference identity '$id'."
    }

  private def _validate_duplicate_reference_semantic_target_ids(
    subject: MonoKotoProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(subject.references.map(_.semanticTargetId.value)).foreach { id =>
      violations += s"Mono-Koto subject '${subject.id}' has duplicate reference semantic target identity '$id'."
    }

  private def _validate_reference(
    reference: MonoKotoProjectionReference,
    subject: MonoKotoProjectionSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(reference.id, s"Mono-Koto subject '${subject.id}' reference identity", violations)
    _require_nonblank(reference.component.value, s"Mono-Koto reference '${reference.id}' Component identity", violations)
    if (reference.component != subject.component)
      violations += s"Mono-Koto reference '${reference.id}' is outside subject '${subject.id}' exact Component scope."
    _require_nonblank(reference.semanticTargetId.value, s"Mono-Koto reference '${reference.id}' semantic target identity", violations)
    _validate_attribution(reference.attribution, s"Mono-Koto reference '${reference.id}'", violations)
    _validate_condition(reference.condition, s"Mono-Koto reference '${reference.id}'", violations)
    reference.presentationTieKey.foreach { tiekey =>
      _require_nonblank(tiekey, s"Mono-Koto reference '${reference.id}' presentation tie key", violations)
    }
    if (!_compatible_reference_kind(subject.projectionKind, reference.referenceKind))
      violations += s"Mono-Koto subject '${subject.id}' ${subject.projectionKind} reference '${reference.id}' has incompatible kind ${reference.referenceKind}."
    _validate_navigation_target(
      reference.semanticTargetId,
      reference.component,
      reference.navigationTarget,
      s"Mono-Koto reference '${reference.id}'",
      violations
    )
  }

  private def _validate_navigation_target(
    semanticid: ComponentDashboardSemanticTargetIdentity,
    component: ComponentDashboardComponentIdentity,
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    navigationtarget.foreach { target =>
      _require_nonblank(target.targetIdentity.value, s"$subject navigation target identity", violations)
      if (target.targetIdentity != semanticid)
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

  private def _compatible_reference_kind(
    projectionkind: MonoKotoProjectionKind,
    referencekind: MonoKotoProjectionReferenceKind
  ): Boolean =
    (projectionkind == Mono && referencekind == StructuralDomain) ||
      (projectionkind == Koto && referencekind == BehavioralTemporal)

  private def _admitted_navigation_target(
    reference: MonoKotoProjectionReference
  ): Option[ComponentDashboardNavigationTarget] =
    reference.navigationTarget.filter { target =>
      reference.semanticTargetId == target.targetIdentity &&
      reference.component == target.targetComponent &&
      target.implementedAndUsable &&
      _usable_condition(reference.condition) &&
      _usable_condition(target.targetCondition)
    }

  private def _admitted_navigation_target(
    subject: MonoKotoProjectionSubject
  ): Option[ComponentDashboardNavigationTarget] =
    subject.navigationTarget.filter { target =>
      subject.semanticTargetId == target.targetIdentity &&
      subject.component == target.targetComponent &&
      target.implementedAndUsable &&
      _usable_condition(subject.condition) &&
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
    subjects: Vector[MonoKotoProjectionSubject]
  ): Vector[MonoKotoProjectionSubject] =
    subjects.sortBy { subject =>
      val kindindex = MonoKotoProjectionKind.presentationSequence.indexOf(subject.projectionKind)
      (kindindex, subject.semanticTargetId.value, subject.id, subject.presentationTieKey.getOrElse(""))
    }

  private def _order_references(
    references: Vector[MonoKotoProjectionReference]
  ): Vector[MonoKotoProjectionReference] =
    references.sortBy { reference =>
      (reference.semanticTargetId.value, reference.id, reference.presentationTieKey.getOrElse(""))
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
