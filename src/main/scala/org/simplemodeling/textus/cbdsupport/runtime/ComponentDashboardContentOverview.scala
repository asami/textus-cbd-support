package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ComponentDashboardContentOverviewFamily

case object Purpose extends ComponentDashboardContentOverviewFamily

case object Responsibility extends ComponentDashboardContentOverviewFamily

case object DomainSummary extends ComponentDashboardContentOverviewFamily

case object Capabilities extends ComponentDashboardContentOverviewFamily

case object RepresentativeAnalysisModelEntryPoints extends ComponentDashboardContentOverviewFamily

case object Rules extends ComponentDashboardContentOverviewFamily

case object InterfacesEvents extends ComponentDashboardContentOverviewFamily

case object RelatedKnowledge extends ComponentDashboardContentOverviewFamily

object ComponentDashboardContentOverviewFamily {
  val presentationSequence: Vector[ComponentDashboardContentOverviewFamily] = Vector(
    Purpose,
    Responsibility,
    DomainSummary,
    Capabilities,
    RepresentativeAnalysisModelEntryPoints,
    Rules,
    InterfacesEvents,
    RelatedKnowledge
  )
}

final case class ComponentDashboardContentOverviewAssignment(
  sourceRecordId: String,
  family: ComponentDashboardContentOverviewFamily
)

final case class ComponentDashboardContentOverviewItem(
  family: ComponentDashboardContentOverviewFamily,
  dashboardRecord: ComponentDashboardProjectedRecord
)

final case class ComponentDashboardContentOverviewGroup(
  family: ComponentDashboardContentOverviewFamily,
  items: Vector[ComponentDashboardContentOverviewItem]
)

final case class ComponentDashboardContentOverviewFailure(violations: Vector[String])

final case class ComponentDashboardContentOverview(
  component: ComponentDashboardComponentIdentity,
  groups: Vector[ComponentDashboardContentOverviewGroup],
  unassignedContentRecords: Vector[ComponentDashboardProjectedRecord]
)

object ComponentDashboardContentOverview {
  def create(
    dashboard: ComponentDashboard,
    assignments: Vector[ComponentDashboardContentOverviewAssignment]
  ): Either[ComponentDashboardContentOverviewFailure, ComponentDashboardContentOverview] =
    ComponentDashboard.create(dashboard.component, _source_records(dashboard)) match {
      case Left(failure) =>
        Left(ComponentDashboardContentOverviewFailure(failure.violations))
      case Right(admitteddashboard) =>
        val contentrecords = _content_records(admitteddashboard)
        val violations = Vector.newBuilder[String]

        _validate_assignments(admitteddashboard, contentrecords, assignments, violations)

        val result = violations.result()
        if (result.nonEmpty)
          Left(ComponentDashboardContentOverviewFailure(result))
        else {
          val groups = ComponentDashboardContentOverviewFamily.presentationSequence.map { family =>
            ComponentDashboardContentOverviewGroup(
              family,
              contentrecords.collect {
                case record if _has_assignment(record, family, assignments) =>
                  ComponentDashboardContentOverviewItem(family, record)
              }
            )
          }
          val unassignedrecords = contentrecords.filterNot { record =>
            assignments.exists(_.sourceRecordId == record.sourceRecord.sourceRecordId)
          }
          Right(ComponentDashboardContentOverview(admitteddashboard.component, groups, unassignedrecords))
        }
    }

  private def _source_records(dashboard: ComponentDashboard): Vector[ComponentDashboardInventoryRecord] =
    dashboard.concernGroups.flatMap(_.records.map(_.sourceRecord)) ++
      dashboard.unclassifiedRecords.map(_.sourceRecord)

  private def _content_records(dashboard: ComponentDashboard): Vector[ComponentDashboardProjectedRecord] =
    dashboard.concernGroups.find(_.concern == Content).map(_.records).getOrElse(Vector.empty)

  private def _validate_assignments(
    dashboard: ComponentDashboard,
    contentrecords: Vector[ComponentDashboardProjectedRecord],
    assignments: Vector[ComponentDashboardContentOverviewAssignment],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    val allrecords = _source_records(dashboard)
    val contentrecordids = contentrecords.map(_.sourceRecord.sourceRecordId).toSet

    assignments.foreach { assignment =>
      _require_nonblank(assignment.sourceRecordId, "Content Overview assignment source-record identity", violations)
      if (assignment.sourceRecordId.trim.nonEmpty) {
        if (!allrecords.exists(_.sourceRecordId == assignment.sourceRecordId))
          violations += s"Content Overview assignment source-record identity '${assignment.sourceRecordId}' is not admitted by the supplied Dashboard."
        else if (!contentrecordids.contains(assignment.sourceRecordId))
          violations += s"Content Overview assignment source-record identity '${assignment.sourceRecordId}' is not an admitted Content record."
      }
    }

    _duplicate_assignments(assignments).foreach { case (sourceid, family) =>
      violations += s"Duplicate Content Overview assignment for source-record identity '$sourceid' and family '${family.toString}'."
    }
  }

  private def _duplicate_assignments(
    assignments: Vector[ComponentDashboardContentOverviewAssignment]
  ): Vector[(String, ComponentDashboardContentOverviewFamily)] =
    assignments
      .groupBy(assignment => (assignment.sourceRecordId, assignment.family))
      .collect { case (identity, values) if values.size > 1 => identity }
      .toVector
      .sortBy { case (sourceid, family) =>
        (sourceid, ComponentDashboardContentOverviewFamily.presentationSequence.indexOf(family))
      }

  private def _has_assignment(
    record: ComponentDashboardProjectedRecord,
    family: ComponentDashboardContentOverviewFamily,
    assignments: Vector[ComponentDashboardContentOverviewAssignment]
  ): Boolean =
    assignments.exists { assignment =>
      assignment.sourceRecordId == record.sourceRecord.sourceRecordId && assignment.family == family
    }

  private def _require_nonblank(
    value: String,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (value.trim.isEmpty)
      violations += s"$subject must not be blank."
}
