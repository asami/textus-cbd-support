package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ComponentDashboardComponentIdentity(value: String)

final case class ComponentDashboardSemanticTargetIdentity(value: String)

final case class ComponentDashboardSourceAttribution(
  sourceId: String,
  authorityScope: String,
  sourceLocator: String
)

final case class ComponentDashboardCondition(
  availability: String,
  authorization: String,
  redaction: Option[String],
  explicitAbsence: Option[String],
  ambiguity: Option[String],
  conflict: Option[String],
  staleness: Option[String],
  malformedEvidence: Option[String],
  limitations: Vector[String]
)

final case class ComponentDashboardNavigationTarget(
  targetIdentity: ComponentDashboardSemanticTargetIdentity,
  targetComponent: ComponentDashboardComponentIdentity,
  targetAttribution: ComponentDashboardSourceAttribution,
  targetCondition: ComponentDashboardCondition,
  implementedAndUsable: Boolean
)

sealed trait ComponentDashboardConcern

case object Content extends ComponentDashboardConcern

case object Usage extends ComponentDashboardConcern

case object Operation extends ComponentDashboardConcern

case object QualityReview extends ComponentDashboardConcern

object ComponentDashboardConcern {
  val presentationSequence: Vector[ComponentDashboardConcern] = Vector(Content, Usage, Operation, QualityReview)
}

final case class ComponentDashboardInventoryRecord(
  id: String,
  component: ComponentDashboardComponentIdentity,
  sourceRecordId: String,
  attribution: ComponentDashboardSourceAttribution,
  statementRepresentation: String,
  condition: ComponentDashboardCondition,
  concern: Option[ComponentDashboardConcern],
  semanticTargetId: Option[ComponentDashboardSemanticTargetIdentity],
  presentationTieKey: Option[String],
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ComponentDashboardProjectedRecord(
  sourceRecord: ComponentDashboardInventoryRecord,
  navigationTarget: Option[ComponentDashboardNavigationTarget]
)

final case class ComponentDashboardConcernGroup(
  concern: ComponentDashboardConcern,
  records: Vector[ComponentDashboardProjectedRecord]
)

final case class ComponentDashboardProjectionFailure(violations: Vector[String])

final case class ComponentDashboard(
  component: ComponentDashboardComponentIdentity,
  concernGroups: Vector[ComponentDashboardConcernGroup],
  unclassifiedRecords: Vector[ComponentDashboardProjectedRecord]
)

object ComponentDashboard {
  def create(
    component: ComponentDashboardComponentIdentity,
    records: Vector[ComponentDashboardInventoryRecord]
  ): Either[ComponentDashboardProjectionFailure, ComponentDashboard] = {
    val violations = Vector.newBuilder[String]

    _validate_component(component, violations)
    _validate_duplicate_inventory_ids(records, violations)
    _validate_duplicate_source_record_ids(records, violations)
    records.foreach(_validate_record(_, component, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(ComponentDashboardProjectionFailure(result))
    else {
      val projectedrecords = records.map { record =>
        ComponentDashboardProjectedRecord(record, _admitted_navigation_target(record))
      }
      val groups = ComponentDashboardConcern.presentationSequence.map { concern =>
        ComponentDashboardConcernGroup(
          concern,
          _order_records(projectedrecords.filter(_.sourceRecord.concern.contains(concern)))
        )
      }
      val unclassifiedrecords = _order_records(projectedrecords.filter(_.sourceRecord.concern.isEmpty))
      Right(ComponentDashboard(component, groups, unclassifiedrecords))
    }
  }

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Dashboard Component identity", violations)

  private def _validate_duplicate_inventory_ids(
    records: Vector[ComponentDashboardInventoryRecord],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(records.map(_.id)).foreach { id =>
      violations += s"Duplicate Dashboard inventory record identity '$id'."
    }

  private def _validate_duplicate_source_record_ids(
    records: Vector[ComponentDashboardInventoryRecord],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(records.map(_.sourceRecordId)).foreach { id =>
      violations += s"Duplicate Dashboard source-record identity '$id' is unresolved as an ordering key."
    }

  private def _validate_record(
    record: ComponentDashboardInventoryRecord,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(record.id, "Dashboard inventory record identity", violations)
    _require_nonblank(record.component.value, s"Dashboard inventory record '${record.id}' Component identity", violations)
    if (record.component != component)
      violations += s"Dashboard inventory record '${record.id}' is outside Dashboard Component '${component.value}'."
    _require_nonblank(record.sourceRecordId, s"Dashboard inventory record '${record.id}' source-record identity", violations)
    _validate_attribution(record.attribution, s"Dashboard inventory record '${record.id}'", violations)
    _require_nonblank(record.statementRepresentation, s"Dashboard inventory record '${record.id}' statement representation", violations)
    _validate_condition(record.condition, s"Dashboard inventory record '${record.id}'", violations)
    record.semanticTargetId.foreach { targetid =>
      _require_nonblank(targetid.value, s"Dashboard inventory record '${record.id}' semantic target identity", violations)
    }
    record.presentationTieKey.foreach { tiekey =>
      _require_nonblank(tiekey, s"Dashboard inventory record '${record.id}' presentation tie key", violations)
    }
    record.navigationTarget.foreach { target =>
      _require_nonblank(target.targetIdentity.value, s"Dashboard inventory record '${record.id}' navigation target identity", violations)
      if (!record.semanticTargetId.contains(target.targetIdentity))
        violations += s"Dashboard inventory record '${record.id}' navigation target must retain its exact semantic target identity."
      _require_nonblank(target.targetComponent.value, s"Dashboard inventory record '${record.id}' navigation target Component identity", violations)
      if (target.targetComponent != record.component)
        violations += s"Dashboard inventory record '${record.id}' navigation target must remain within its exact Component scope."
      _validate_attribution(target.targetAttribution, s"Dashboard inventory record '${record.id}' navigation target", violations)
      _validate_condition(target.targetCondition, s"Dashboard inventory record '${record.id}' navigation target", violations)
    }
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

  private def _admitted_navigation_target(
    record: ComponentDashboardInventoryRecord
  ): Option[ComponentDashboardNavigationTarget] =
    record.navigationTarget.filter { target =>
      record.semanticTargetId.contains(target.targetIdentity) &&
      target.targetComponent == record.component &&
      target.implementedAndUsable &&
      _usable_condition(record.condition) &&
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

  private def _order_records(
    records: Vector[ComponentDashboardProjectedRecord]
  ): Vector[ComponentDashboardProjectedRecord] =
    records.sortBy { record =>
      val source = record.sourceRecord
      val identitykey = source.semanticTargetId.map(_.value).getOrElse(source.sourceRecordId)
      (identitykey, source.id, source.presentationTieKey.getOrElse(""))
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
