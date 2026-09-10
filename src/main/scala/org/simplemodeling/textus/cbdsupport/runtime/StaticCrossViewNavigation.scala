package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait StaticCrossViewProjectionCategory

case object StaticMonoKoto extends StaticCrossViewProjectionCategory

case object StaticEntity extends StaticCrossViewProjectionCategory

case object StaticStructure extends StaticCrossViewProjectionCategory

case object StaticClassification extends StaticCrossViewProjectionCategory

object StaticCrossViewProjectionCategory {
  val presentationSequence: Vector[StaticCrossViewProjectionCategory] =
    Vector(StaticMonoKoto, StaticEntity, StaticStructure, StaticClassification)
}

sealed trait StaticCrossViewRecordRole

case object StaticMonoKotoSubject extends StaticCrossViewRecordRole

case object StaticMonoKotoReference extends StaticCrossViewRecordRole

case object StaticEntitySubject extends StaticCrossViewRecordRole

case object StaticEntityMetadata extends StaticCrossViewRecordRole

case object StaticStructureRelation extends StaticCrossViewRecordRole

case object StaticStructureAssertion extends StaticCrossViewRecordRole

case object StaticStructureEndpoint extends StaticCrossViewRecordRole

case object StaticClassificationRelation extends StaticCrossViewRecordRole

case object StaticClassificationSubject extends StaticCrossViewRecordRole

case object StaticClassificationEndpoint extends StaticCrossViewRecordRole

case object StaticClassificationDimension extends StaticCrossViewRecordRole

case object StaticClassificationAssertion extends StaticCrossViewRecordRole

object StaticCrossViewRecordRole {
  def presentationSequence(category: StaticCrossViewProjectionCategory): Vector[StaticCrossViewRecordRole] =
    category match {
      case StaticMonoKoto => Vector(StaticMonoKotoSubject, StaticMonoKotoReference)
      case StaticEntity => Vector(StaticEntitySubject, StaticEntityMetadata)
      case StaticStructure => Vector(StaticStructureRelation, StaticStructureAssertion, StaticStructureEndpoint)
      case StaticClassification => Vector(
        StaticClassificationRelation,
        StaticClassificationSubject,
        StaticClassificationEndpoint,
        StaticClassificationDimension,
        StaticClassificationAssertion
      )
    }
}

final case class StaticCrossViewIdentityRecord(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: ComponentDashboardSemanticTargetIdentity,
  category: StaticCrossViewProjectionCategory,
  role: StaticCrossViewRecordRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class StaticCrossViewMapping(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  sourceRecordId: String,
  counterpartRecordId: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String],
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StaticCrossViewGap(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  sourceRecordId: String,
  requestedCategory: StaticCrossViewProjectionCategory,
  requestedRole: StaticCrossViewRecordRole,
  knownCounterpartSemanticTargetId: Option[ComponentDashboardSemanticTargetIdentity],
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String]
)

final case class StaticCrossViewProjectedMapping(
  sourceMapping: StaticCrossViewMapping,
  sourceRecord: StaticCrossViewIdentityRecord,
  counterpartRecord: StaticCrossViewIdentityRecord,
  forwardNavigationTarget: Option[ComponentDashboardNavigationTarget],
  reverseNavigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class StaticCrossViewProjectedGap(sourceGap: StaticCrossViewGap)

final case class StaticCrossViewNavigationFailure(violations: Vector[String])

final case class StaticCrossViewNavigationIndex(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  records: Vector[StaticCrossViewIdentityRecord],
  mappings: Vector[StaticCrossViewProjectedMapping],
  gaps: Vector[StaticCrossViewProjectedGap]
)

object StaticCrossViewNavigation {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    records: Vector[StaticCrossViewIdentityRecord],
    mappings: Vector[StaticCrossViewMapping],
    gaps: Vector[StaticCrossViewGap]
  ): Either[StaticCrossViewNavigationFailure, StaticCrossViewNavigationIndex] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_unique_ids(records.map(_.id), mappings.map(_.id), gaps.map(_.id), violations)
    records.foreach(record => _validate_record(record, context, component, violations))

    val recordbyid = records.map(record => record.id -> record).toMap
    mappings.foreach(mapping => _validate_mapping(mapping, context, component, recordbyid, violations))
    gaps.foreach(gap => _validate_gap(gap, context, component, recordbyid, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(StaticCrossViewNavigationFailure(result))
    else {
      val orderedrecords = _order_records(records)
      val orderedmappings = _order_mappings(mappings, recordbyid)
      val orderedgaps = _order_gaps(gaps, recordbyid)
      Right(
        StaticCrossViewNavigationIndex(
          context,
          component,
          orderedrecords,
          orderedmappings.map(mapping => _project_mapping(mapping, recordbyid)),
          orderedgaps.map(StaticCrossViewProjectedGap.apply)
        )
      )
    }
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Static cross-view bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Static cross-view Component identity", violations)

  private def _validate_unique_ids(
    recordids: Vector[String],
    mappingids: Vector[String],
    gapids: Vector[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(recordids ++ mappingids ++ gapids).foreach { id =>
      violations += s"Static cross-view record, mapping, or gap identity '$id' is duplicate."
    }

  private def _validate_record(
    record: StaticCrossViewIdentityRecord,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(record.id, "Static cross-view identity-record identity", violations)
    _validate_scope(record.context, record.component, context, component, s"Static cross-view identity record '${record.id}'", violations)
    _require_nonblank(record.semanticTargetId.value, s"Static cross-view identity record '${record.id}' semantic target identity", violations)
    if (!_compatible_role(record.category, record.role))
      violations += s"Static cross-view identity record '${record.id}' role ${record.role} is incompatible with category ${record.category}."
    _validate_attribution(record.attribution, s"Static cross-view identity record '${record.id}'", violations)
    _validate_condition(record.condition, s"Static cross-view identity record '${record.id}'", violations)
    _validate_tie_key(record.stableTieKey, s"Static cross-view identity record '${record.id}'", violations)
  }

  private def _validate_mapping(
    mapping: StaticCrossViewMapping,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    recordbyid: Map[String, StaticCrossViewIdentityRecord],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(mapping.id, "Static cross-view mapping identity", violations)
    _validate_scope(mapping.context, mapping.component, context, component, s"Static cross-view mapping '${mapping.id}'", violations)
    _require_nonblank(mapping.sourceRecordId, s"Static cross-view mapping '${mapping.id}' source record identity", violations)
    _require_nonblank(mapping.counterpartRecordId, s"Static cross-view mapping '${mapping.id}' counterpart record identity", violations)
    _validate_attribution(mapping.attribution, s"Static cross-view mapping '${mapping.id}'", violations)
    _validate_condition(mapping.condition, s"Static cross-view mapping '${mapping.id}'", violations)
    _validate_tie_key(mapping.stableTieKey, s"Static cross-view mapping '${mapping.id}'", violations)

    val source = recordbyid.get(mapping.sourceRecordId)
    val counterpart = recordbyid.get(mapping.counterpartRecordId)
    if (source.isEmpty)
      violations += s"Static cross-view mapping '${mapping.id}' source record '${mapping.sourceRecordId}' is unresolved."
    if (counterpart.isEmpty)
      violations += s"Static cross-view mapping '${mapping.id}' counterpart record '${mapping.counterpartRecordId}' is unresolved."
    if (mapping.sourceRecordId == mapping.counterpartRecordId)
      violations += s"Static cross-view mapping '${mapping.id}' must not map one record to itself."
    for {
      sourcerecord <- source
      counterpartrecord <- counterpart
    } {
      if (sourcerecord.category == counterpartrecord.category)
        violations += s"Static cross-view mapping '${mapping.id}' must relate distinct projection categories."
      if (sourcerecord.semanticTargetId != counterpartrecord.semanticTargetId)
        violations += s"Static cross-view mapping '${mapping.id}' must retain one exact shared semantic target identity."
      _validate_navigation_target(
        mapping.forwardNavigationTarget,
        counterpartrecord,
        s"Static cross-view mapping '${mapping.id}' forward navigation target",
        violations
      )
      _validate_navigation_target(
        mapping.reverseNavigationTarget,
        sourcerecord,
        s"Static cross-view mapping '${mapping.id}' reverse navigation target",
        violations
      )
    }
  }

  private def _validate_gap(
    gap: StaticCrossViewGap,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    recordbyid: Map[String, StaticCrossViewIdentityRecord],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "Static cross-view gap identity", violations)
    _validate_scope(gap.context, gap.component, context, component, s"Static cross-view gap '${gap.id}'", violations)
    _require_nonblank(gap.sourceRecordId, s"Static cross-view gap '${gap.id}' source record identity", violations)
    _validate_attribution(gap.attribution, s"Static cross-view gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"Static cross-view gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"Static cross-view gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"Static cross-view gap '${gap.id}'", violations)
    if (!_compatible_role(gap.requestedCategory, gap.requestedRole))
      violations += s"Static cross-view gap '${gap.id}' requested role ${gap.requestedRole} is incompatible with category ${gap.requestedCategory}."

    val source = recordbyid.get(gap.sourceRecordId)
    if (source.isEmpty)
      violations += s"Static cross-view gap '${gap.id}' source record '${gap.sourceRecordId}' is unresolved."
    source.foreach { sourcerecord =>
      if (sourcerecord.category == gap.requestedCategory)
        violations += s"Static cross-view gap '${gap.id}' must request a distinct counterpart category."
      gap.knownCounterpartSemanticTargetId.foreach { identity =>
        _require_nonblank(identity.value, s"Static cross-view gap '${gap.id}' known counterpart semantic target identity", violations)
        if (identity != sourcerecord.semanticTargetId)
          violations += s"Static cross-view gap '${gap.id}' known counterpart identity must retain its source record exact semantic target identity."
      }
    }
  }

  private def _validate_scope(
    recordcontext: MonoKotoProjectionContextIdentity,
    recordcomponent: ComponentDashboardComponentIdentity,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(recordcontext.value, s"$subject bounded CCDM context identity", violations)
    if (recordcontext != context)
      violations += s"$subject is outside static cross-view bounded CCDM context '${context.value}'."
    _require_nonblank(recordcomponent.value, s"$subject Component identity", violations)
    if (recordcomponent != component)
      violations += s"$subject is outside static cross-view Component '${component.value}'."
  }

  private def _validate_navigation_target(
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    receivingrecord: StaticCrossViewIdentityRecord,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    navigationtarget.foreach { target =>
      _require_nonblank(target.targetIdentity.value, s"$subject identity", violations)
      if (target.targetIdentity != receivingrecord.semanticTargetId)
        violations += s"$subject must retain the exact receiving semantic target identity."
      _require_nonblank(target.targetComponent.value, s"$subject Component identity", violations)
      if (target.targetComponent != receivingrecord.component)
        violations += s"$subject must retain the exact receiving Component identity."
      _validate_attribution(target.targetAttribution, subject, violations)
      if (target.targetAttribution != receivingrecord.attribution)
        violations += s"$subject must retain the receiving record exact attribution and locator."
      _validate_condition(target.targetCondition, subject, violations)
      if (target.targetCondition != receivingrecord.condition)
        violations += s"$subject must retain the receiving record exact condition."
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
    tiekey.foreach(_require_nonblank(_, s"$subject stable tie key", violations))

  private def _compatible_role(
    category: StaticCrossViewProjectionCategory,
    role: StaticCrossViewRecordRole
  ): Boolean =
    StaticCrossViewRecordRole.presentationSequence(category).contains(role)

  private def _project_mapping(
    mapping: StaticCrossViewMapping,
    recordbyid: Map[String, StaticCrossViewIdentityRecord]
  ): StaticCrossViewProjectedMapping = {
    val source = recordbyid(mapping.sourceRecordId)
    val counterpart = recordbyid(mapping.counterpartRecordId)
    StaticCrossViewProjectedMapping(
      mapping,
      source,
      counterpart,
      _admitted_navigation_target(mapping.forwardNavigationTarget, source, counterpart, mapping),
      _admitted_navigation_target(mapping.reverseNavigationTarget, counterpart, source, mapping)
    )
  }

  private def _admitted_navigation_target(
    navigationtarget: Option[ComponentDashboardNavigationTarget],
    source: StaticCrossViewIdentityRecord,
    receivingrecord: StaticCrossViewIdentityRecord,
    mapping: StaticCrossViewMapping
  ): Option[ComponentDashboardNavigationTarget] =
    navigationtarget.filter { target =>
      target.targetIdentity == receivingrecord.semanticTargetId &&
      target.targetComponent == receivingrecord.component &&
      target.targetAttribution == receivingrecord.attribution &&
      target.targetCondition == receivingrecord.condition &&
      target.implementedAndUsable &&
      _usable_condition(source.condition) &&
      _usable_condition(receivingrecord.condition) &&
      _usable_condition(mapping.condition)
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

  private def _order_records(
    records: Vector[StaticCrossViewIdentityRecord]
  ): Vector[StaticCrossViewIdentityRecord] =
    records.sortBy { record =>
      (
        record.semanticTargetId.value,
        _category_index(record.category),
        _role_index(record.category, record.role),
        record.id,
        record.stableTieKey.getOrElse("")
      )
    }

  private def _order_mappings(
    mappings: Vector[StaticCrossViewMapping],
    recordbyid: Map[String, StaticCrossViewIdentityRecord]
  ): Vector[StaticCrossViewMapping] =
    mappings.sortBy { mapping =>
      val source = recordbyid(mapping.sourceRecordId)
      val counterpart = recordbyid(mapping.counterpartRecordId)
      (
        source.semanticTargetId.value,
        _category_index(source.category),
        _role_index(source.category, source.role),
        _category_index(counterpart.category),
        _role_index(counterpart.category, counterpart.role),
        source.id,
        counterpart.id,
        mapping.id,
        mapping.stableTieKey.getOrElse("")
      )
    }

  private def _order_gaps(
    gaps: Vector[StaticCrossViewGap],
    recordbyid: Map[String, StaticCrossViewIdentityRecord]
  ): Vector[StaticCrossViewGap] =
    gaps.sortBy { gap =>
      val source = recordbyid(gap.sourceRecordId)
      (
        source.semanticTargetId.value,
        _category_index(source.category),
        _role_index(source.category, source.role),
        _category_index(gap.requestedCategory),
        _role_index(gap.requestedCategory, gap.requestedRole),
        gap.id,
        gap.stableTieKey.getOrElse("")
      )
    }

  private def _category_index(category: StaticCrossViewProjectionCategory): Int =
    StaticCrossViewProjectionCategory.presentationSequence.indexOf(category)

  private def _role_index(
    category: StaticCrossViewProjectionCategory,
    role: StaticCrossViewRecordRole
  ): Int =
    StaticCrossViewRecordRole.presentationSequence(category).indexOf(role)

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
