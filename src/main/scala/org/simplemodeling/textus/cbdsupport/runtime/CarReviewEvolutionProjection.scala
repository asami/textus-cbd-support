package org.simplemodeling.textus.cbdsupport.runtime

/**
 * Read-only CAR evolution View.  Its inputs are already-retained immutable
 * Report snapshots supplied by the Entity Aggregate adapter; this projection
 * neither loads storage nor starts review/provider work.
 */
final case class CarReviewLineageId(value: String)
final case class CarReviewConfigurationCompatibilityId(value: String)

final case class CarReviewHistoryEntry(
  lineageId: CarReviewLineageId,
  configurationCompatibilityId: CarReviewConfigurationCompatibilityId,
  report: CarReviewReport
)

final case class CarReviewEvolutionFailure(code: String, message: String)

final case class CarReviewEvolutionDelta(
  baselineReportId: ReviewReportId,
  baselineReportDigest: ReviewDigest,
  currentReportId: ReviewReportId,
  currentReportDigest: ReviewDigest,
  baselineTarget: ReviewTarget,
  currentTarget: ReviewTarget,
  baselineGate: ReviewGateResult,
  currentGate: ReviewGateResult,
  addedObservationIds: Vector[ReviewObservationId],
  removedObservationIds: Vector[ReviewObservationId],
  unchangedObservationIds: Vector[ReviewObservationId],
  addedCapabilityIds: Vector[ReviewCapabilityId],
  removedCapabilityIds: Vector[ReviewCapabilityId],
  changedCapabilityIds: Vector[ReviewCapabilityId]
)

object CarReviewEvolutionProjection {
  def compare(
    baseline: CarReviewHistoryEntry,
    current: CarReviewHistoryEntry
  ): Either[CarReviewEvolutionFailure, CarReviewEvolutionDelta] =
    if baseline.lineageId != current.lineageId then
      Left(CarReviewEvolutionFailure("review-history-lineage-mismatch", "CAR lineage differs."))
    else if baseline.configurationCompatibilityId != current.configurationCompatibilityId then
      Left(CarReviewEvolutionFailure("review-history-configuration-incompatible", "Review configuration compatibility identity differs."))
    else if !_same_car_identity(baseline.report.target, current.report.target) then
      Left(CarReviewEvolutionFailure("review-history-target-identity-mismatch", "CAR identity differs."))
    else
      Right(_delta(baseline.report, current.report))

  private def _delta(
    baseline: CarReviewReport,
    current: CarReviewReport
  ): CarReviewEvolutionDelta = {
    val previousobservations = baseline.observations.map(_.id).toSet
    val currentobservations = current.observations.map(_.id).toSet
    val previouscapabilities = baseline.assessments.map(value => value.capabilityId -> value).toMap
    val currentcapabilities = current.assessments.map(value => value.capabilityId -> value).toMap
    CarReviewEvolutionDelta(
      baseline.reportId,
      baseline.reportDigest,
      current.reportId,
      current.reportDigest,
      baseline.target,
      current.target,
      baseline.gate.result,
      current.gate.result,
      _ids(currentobservations -- previousobservations),
      _ids(previousobservations -- currentobservations),
      _ids(previousobservations intersect currentobservations),
      _capability_ids(currentcapabilities.keySet -- previouscapabilities.keySet),
      _capability_ids(previouscapabilities.keySet -- currentcapabilities.keySet),
      _capability_ids(currentcapabilities.keySet.intersect(previouscapabilities.keySet).filter { id =>
        currentcapabilities(id) != previouscapabilities(id)
      })
    )
  }

  private def _same_car_identity(left: ReviewTarget, right: ReviewTarget): Boolean =
    left.kind == right.kind &&
      left.organization == right.organization &&
      left.name == right.name

  private def _ids(values: Set[ReviewObservationId]): Vector[ReviewObservationId] =
    values.toVector.sortBy(_.value)

  private def _capability_ids(values: Set[ReviewCapabilityId]): Vector[ReviewCapabilityId] =
    values.toVector.sortBy(_.value)
}
