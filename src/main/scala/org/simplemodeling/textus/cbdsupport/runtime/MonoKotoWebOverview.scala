package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final case class MonoKotoWebOverviewReferencePresentation(
  sourceSubjectId: String,
  sourceReferenceId: String,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  relationshipLabel: String,
  targetLabel: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition
)

final case class MonoKotoWebOverviewReference(
  sourceReferenceId: String,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  relationshipLabel: String,
  targetLabel: String,
  sourceAttribution: ComponentDashboardSourceAttribution,
  sourceCondition: ComponentDashboardCondition,
  presentationAttribution: ComponentDashboardSourceAttribution,
  presentationCondition: ComponentDashboardCondition,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoWebOverviewSubject(
  sourceSubjectId: String,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  stakeholderLabel: String,
  sourceAttribution: ComponentDashboardSourceAttribution,
  sourceCondition: ComponentDashboardCondition,
  relationships: Vector[MonoKotoWebOverviewReference],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class MonoKotoWebOverviewFailure(violations: Vector[String])

final case class MonoKotoWebOverview(
  component: ComponentDashboardComponentIdentity,
  subjects: Vector[MonoKotoWebOverviewSubject]
)

object MonoKotoWebOverview {
  def create(
    webEntry: ComponentDashboardWebEntry,
    projection: MonoKotoProjection,
    presentations: Vector[MonoKotoWebOverviewReferencePresentation]
  ): Either[MonoKotoWebOverviewFailure, MonoKotoWebOverview] = {
    val violations = Vector.newBuilder[String]
    val deliveredcomponent = webEntry.dashboard.component

    _validate_component(deliveredcomponent, "Mono-Koto Web overview delivered Component identity", violations)
    _validate_component(projection.component, "Mono-Koto Web overview Projection Component identity", violations)
    if (deliveredcomponent != projection.component)
      violations += s"Mono-Koto Web overview delivered Component '${deliveredcomponent.value}' does not match Projection Component '${projection.component.value}'."

    _validate_projection_integrity(projection, violations)
    _validate_presentations(projection, presentations, violations)
    val result = violations.result()
    if (result.nonEmpty)
      Left(MonoKotoWebOverviewFailure(result))
    else {
      val presentationindex = presentations.map { presentation =>
        (presentation.sourceSubjectId, presentation.sourceReferenceId) -> presentation
      }.toMap
      Right(
        MonoKotoWebOverview(
          deliveredcomponent,
          projection.subjects.map { subject =>
            _assemble_subject(subject, deliveredcomponent, presentationindex)
          }
        )
      )
    }
  }

  private def _validate_projection_integrity(
    projection: MonoKotoProjection,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val projectedsubjects = projection.subjects
    _duplicate_ids(projectedsubjects.map(_.sourceSubject.id)).foreach { id =>
      violations += s"Duplicate Mono-Koto subject identity '$id'."
    }
    _duplicate_ids(projectedsubjects.map(_.sourceSubject.semanticTargetId.value)).foreach { id =>
      violations += s"Duplicate Mono-Koto subject semantic target identity '$id'."
    }
    projectedsubjects.foreach { projectedsubject =>
      val subject = projectedsubject.sourceSubject
      _require_nonblank(subject.id, "Mono-Koto subject identity", violations)
      _require_nonblank(subject.component.value, s"Mono-Koto subject '${subject.id}' Component identity", violations)
      if (subject.component != projection.component)
        violations += s"Mono-Koto subject '${subject.id}' is outside Projection Component '${projection.component.value}'."
      _require_nonblank(subject.semanticTargetId.value, s"Mono-Koto subject '${subject.id}' semantic target identity", violations)
      _require_nonblank(subject.stakeholderLabel, s"Mono-Koto subject '${subject.id}' stakeholder label", violations)
      _validate_attribution(subject.attribution, s"Mono-Koto subject '${subject.id}'", violations)
      _validate_condition(subject.condition, s"Mono-Koto subject '${subject.id}'", violations)
      subject.presentationTieKey.foreach { tiekey =>
        _require_nonblank(tiekey, s"Mono-Koto subject '${subject.id}' presentation tie key", violations)
      }
      _validate_projection_navigation(
        subject.navigationTarget,
        projectedsubject.navigationTarget,
        subject.semanticTargetId,
        subject.component,
        subject.condition,
        s"Mono-Koto subject '${subject.id}'",
        violations
      )

      val references = subject.references
      _duplicate_ids(references.map(_.id)).foreach { id =>
        violations += s"Mono-Koto subject '${subject.id}' has duplicate reference identity '$id'."
      }
      _duplicate_ids(references.map(_.semanticTargetId.value)).foreach { id =>
        violations += s"Mono-Koto subject '${subject.id}' has duplicate reference semantic target identity '$id'."
      }
      if (projectedsubject.references.size != references.size)
        violations += s"Mono-Koto subject '${subject.id}' projection must retain every source reference exactly once."
      references.foreach { reference =>
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
        reference.navigationTarget.foreach { target =>
          _validate_projection_navigation_target(
            target,
            reference.semanticTargetId,
            reference.component,
            s"Mono-Koto reference '${reference.id}'",
            violations
          )
        }
      }
      val projectedsourcereferences = projectedsubject.references.map(_.sourceReference)
      projectedsubject.references.foreach { projectedreference =>
        val reference = projectedreference.sourceReference
        if (references.count(_ == reference) != 1)
          violations += s"Mono-Koto subject '${subject.id}' projection must retain every source reference exactly once."
        else
          _validate_projection_navigation(
            reference.navigationTarget,
            projectedreference.navigationTarget,
            reference.semanticTargetId,
            reference.component,
            reference.condition,
            s"Mono-Koto reference '${reference.id}'",
            violations
          )
      }
      references.foreach { reference =>
        if (projectedsourcereferences.count(_ == reference) != 1)
          violations += s"Mono-Koto subject '${subject.id}' projection must retain every source reference exactly once."
      }
      _validate_reference_order(projectedsubject, violations)
    }
    _validate_subject_order(projectedsubjects, violations)
  }

  private def _validate_projection_navigation(
    sourcetarget: Option[ComponentDashboardNavigationTarget],
    projectedtarget: Option[ComponentDashboardNavigationTarget],
    semantictargetid: ComponentDashboardSemanticTargetIdentity,
    component: ComponentDashboardComponentIdentity,
    condition: ComponentDashboardCondition,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    sourcetarget.foreach { target =>
      _validate_projection_navigation_target(target, semantictargetid, component, subject, violations)
    }
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

  private def _validate_projection_navigation_target(
    target: ComponentDashboardNavigationTarget,
    semantictargetid: ComponentDashboardSemanticTargetIdentity,
    component: ComponentDashboardComponentIdentity,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(target.targetIdentity.value, s"$subject navigation target identity", violations)
    if (target.targetIdentity != semantictargetid)
      violations += s"$subject navigation target must retain its exact semantic target identity."
    _require_nonblank(target.targetComponent.value, s"$subject navigation target Component identity", violations)
    if (target.targetComponent != component)
      violations += s"$subject navigation target must remain within its exact Component scope."
    _validate_attribution(target.targetAttribution, s"$subject navigation target", violations)
    _validate_condition(target.targetCondition, s"$subject navigation target", violations)
  }

  private def _validate_subject_order(
    subjects: Vector[MonoKotoProjectedSubject],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    subjects.sliding(2).foreach {
      case Vector(previous, current) if !_subject_precedes(previous.sourceSubject, current.sourceSubject) =>
        violations += "Mono-Koto Projection subjects must retain their admitted Mono-then-Koto identity order."
      case _ =>
    }

  private def _validate_reference_order(
    subject: MonoKotoProjectedSubject,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    subject.references.sliding(2).foreach {
      case Vector(previous, current) if !_reference_precedes(previous.sourceReference, current.sourceReference) =>
        violations += s"Mono-Koto subject '${subject.sourceSubject.id}' references must retain their admitted identity order."
      case _ =>
    }

  private def _subject_precedes(
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

  private def _reference_precedes(
    previous: MonoKotoProjectionReference,
    current: MonoKotoProjectionReference
  ): Boolean =
    previous.semanticTargetId.value < current.semanticTargetId.value ||
      (previous.semanticTargetId.value == current.semanticTargetId.value && (
        previous.id < current.id ||
          (previous.id == current.id && previous.presentationTieKey.getOrElse("") <= current.presentationTieKey.getOrElse(""))
      ))

  private def _compatible_reference_kind(
    projectionkind: MonoKotoProjectionKind,
    referencekind: MonoKotoProjectionReferenceKind
  ): Boolean =
    (projectionkind == Mono && referencekind == StructuralDomain) ||
      (projectionkind == Koto && referencekind == BehavioralTemporal)

  private def _duplicate_ids(ids: Vector[String]): Vector[String] =
    ids.groupBy(identity).collect { case (id, values) if values.size > 1 => id }.toVector.sorted

  private def _assemble_subject(
    subject: MonoKotoProjectedSubject,
    component: ComponentDashboardComponentIdentity,
    presentationindex: Map[(String, String), MonoKotoWebOverviewReferencePresentation]
  ): MonoKotoWebOverviewSubject = {
    val sourcesubject = subject.sourceSubject
    MonoKotoWebOverviewSubject(
      sourcesubject.id,
      sourcesubject.semanticTargetId,
      sourcesubject.stakeholderLabel,
      sourcesubject.attribution,
      sourcesubject.condition,
      subject.references.map { reference =>
        val sourcereference = reference.sourceReference
        val presentation = presentationindex((sourcesubject.id, sourcereference.id))
        MonoKotoWebOverviewReference(
          sourcereference.id,
          sourcereference.semanticTargetId,
          presentation.relationshipLabel,
          presentation.targetLabel,
          sourcereference.attribution,
          sourcereference.condition,
          presentation.attribution,
          presentation.condition,
          _admitted_reference_navigation(reference, presentation, component)
        )
      },
      _admitted_subject_navigation(subject, component)
    )
  }

  private def _validate_presentations(
    projection: MonoKotoProjection,
    presentations: Vector[MonoKotoWebOverviewReferencePresentation],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val projectedreferences = projection.subjects.flatMap { subject =>
      subject.references.map { reference =>
        (subject.sourceSubject.id, reference.sourceReference.id) -> (subject, reference)
      }
    }.toMap
    val presentationpairs = presentations.map { presentation =>
      (presentation.sourceSubjectId, presentation.sourceReferenceId)
    }

    _duplicate_pairs(presentationpairs).foreach { pair =>
      violations += s"Duplicate Mono-Koto Web overview presentation for subject '${pair._1}' reference '${pair._2}'."
    }
    presentations.foreach { presentation =>
      _validate_presentation(presentation, violations)
      projectedreferences.get((presentation.sourceSubjectId, presentation.sourceReferenceId)) match {
        case Some((_, reference)) =>
          val sourcereference = reference.sourceReference
          if (presentation.component != sourcereference.component)
            violations += s"Mono-Koto Web overview presentation reference '${presentation.sourceReferenceId}' must retain its exact Component identity."
          if (presentation.semanticTargetId != sourcereference.semanticTargetId)
            violations += s"Mono-Koto Web overview presentation reference '${presentation.sourceReferenceId}' must retain its exact semantic target identity."
        case None =>
          violations += s"Mono-Koto Web overview presentation subject '${presentation.sourceSubjectId}' reference '${presentation.sourceReferenceId}' does not match a projected source reference."
      }
    }
    projectedreferences.keys.toVector.sortBy(pair => (pair._1, pair._2)).foreach { pair =>
      if (!presentationpairs.contains(pair))
        violations += s"Mono-Koto Web overview projected subject '${pair._1}' reference '${pair._2}' requires exactly one presentation."
    }
  }

  private def _validate_presentation(
    presentation: MonoKotoWebOverviewReferencePresentation,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val subject = s"Mono-Koto Web overview presentation subject '${presentation.sourceSubjectId}' reference '${presentation.sourceReferenceId}'"
    _require_nonblank(presentation.sourceSubjectId, "Mono-Koto Web overview presentation source subject identity", violations)
    _require_nonblank(presentation.sourceReferenceId, "Mono-Koto Web overview presentation source reference identity", violations)
    _validate_component(presentation.component, s"$subject Component identity", violations)
    _require_nonblank(presentation.semanticTargetId.value, s"$subject semantic target identity", violations)
    _require_nonblank(presentation.relationshipLabel, s"$subject relationship label", violations)
    _require_nonblank(presentation.targetLabel, s"$subject target label", violations)
    _validate_attribution(presentation.attribution, subject, violations)
    _validate_condition(presentation.condition, subject, violations)
  }

  private def _admitted_subject_navigation(
    subject: MonoKotoProjectedSubject,
    component: ComponentDashboardComponentIdentity
  ): Option[ComponentDashboardNavigationTarget] =
    subject.navigationTarget.filter { target =>
      val sourcesubject = subject.sourceSubject
      sourcesubject.semanticTargetId == target.targetIdentity &&
        sourcesubject.component == component &&
        target.targetComponent == component &&
        sourcesubject.component == target.targetComponent &&
        target.implementedAndUsable &&
        _usable_condition(sourcesubject.condition) &&
        _usable_condition(target.targetCondition)
    }

  private def _admitted_reference_navigation(
    reference: MonoKotoProjectedReference,
    presentation: MonoKotoWebOverviewReferencePresentation,
    component: ComponentDashboardComponentIdentity
  ): Option[ComponentDashboardNavigationTarget] =
    reference.navigationTarget.filter { target =>
      val sourcereference = reference.sourceReference
      sourcereference.semanticTargetId == target.targetIdentity &&
        sourcereference.component == component &&
        target.targetComponent == component &&
        sourcereference.component == target.targetComponent &&
        target.implementedAndUsable &&
        _usable_condition(sourcereference.condition) &&
        _usable_condition(target.targetCondition) &&
        _usable_condition(presentation.condition)
    }

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
    condition.redaction.foreach(_require_nonblank(_, s"$subject condition redaction", violations))
    condition.explicitAbsence.foreach(_require_nonblank(_, s"$subject condition explicit absence", violations))
    condition.ambiguity.foreach(_require_nonblank(_, s"$subject condition ambiguity", violations))
    condition.conflict.foreach(_require_nonblank(_, s"$subject condition conflict", violations))
    condition.staleness.foreach(_require_nonblank(_, s"$subject condition staleness", violations))
    condition.malformedEvidence.foreach(_require_nonblank(_, s"$subject condition malformed evidence", violations))
    condition.limitations.foreach(_require_nonblank(_, s"$subject condition limitation", violations))
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

  private def _duplicate_pairs(pairs: Vector[(String, String)]): Vector[(String, String)] =
    pairs.groupBy(identity).collect { case (pair, values) if values.size > 1 => pair }.toVector.sortBy(pair => (pair._1, pair._2))

  private def _require_nonblank(
    value: String,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (value.trim.isEmpty)
      violations += s"$subject must not be blank."
}
