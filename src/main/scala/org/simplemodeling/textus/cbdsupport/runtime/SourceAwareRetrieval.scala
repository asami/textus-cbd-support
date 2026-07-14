package org.simplemodeling.textus.cbdsupport.runtime

import java.util.Locale

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final case class SourceAwareComponentSearchQuery(
  requirement: String,
  organization: Option[String],
  componentKind: Option[String],
  version: Option[String],
  runtimeVersion: Option[String],
  sourceId: Option[String],
  sourceKind: Option[String],
  freshness: Option[String],
  versionState: Option[String],
  conflictCode: Option[String],
  purpose: Option[String],
  limit: Int
)

final case class SourceAwareComponentSearchResult(
  matches: Vector[ComponentMatch],
  report: ObservationReconciliationReport,
  semanticEvidence: Vector[SemanticRequirementEvidence],
  warnings: Vector[String]
)

object SourceAwareRetrieval {
  val MAXIMUM_RESULTS = 100

  def search(
    query: SourceAwareComponentSearchQuery,
    catalogentries: Vector[(ComponentMatch, ReconciliationObservation)],
    localobservations: Vector[LocalComponentObservation],
    semanticevidence: Vector[SemanticRequirementEvidence] = Vector.empty
  ): SourceAwareComponentSearchResult = {
    val normalizedpurpose = query.purpose.filter(ReconciliationPurpose.ALL.contains)
      .getOrElse(ReconciliationPurpose.PUBLISHED_REUSE)
    val warnings = _filter_warnings(query)
    val catalogobservations = catalogentries.map(_._2)
    val localmatches = localobservations.filter(_matches_requirement(_, query.requirement))
      .map(ReconciliationObservation.fromLocal)
    val candidateobservations = (catalogobservations ++ localmatches)
      .filter(_matches_filters(_, query))
    val report = ObservationReconciler.reconcile(
      candidateobservations,
      normalizedpurpose,
      query.version,
      query.runtimeVersion
    )
    val filteredreport = query.conflictCode match {
      case Some(code) if ReconciliationIssueCode.ALL.contains(code) =>
        val issues = report.issues.filter(_.code == code)
        val participants = issues.flatMap(_.participants).toSet
        report.copy(
          observations = report.observations.filter(participants.contains),
          issues = issues
        )
      case Some(_) => report.copy(observations = Vector.empty, issues = Vector.empty)
      case None => report
    }
    val boundedreport = filteredreport.copy(
      observations = filteredreport.observations.take(query.limit.max(1).min(MAXIMUM_RESULTS))
    )
    val returnedobservations = boundedreport.observations.toSet
    val matches = catalogentries.collect {
      case (result, observation) if returnedobservations.contains(observation) => result
    }
    SourceAwareComponentSearchResult(
      matches,
      boundedreport,
      semanticevidence.take(query.limit.max(1).min(MAXIMUM_RESULTS)),
      warnings
    )
  }

  private def _matches_requirement(
    observation: LocalComponentObservation,
    requirement: String
  ): Boolean = {
    val normalizedrequirement = _normalize(requirement)
    val exact = observation.componentName.exists(_normalize(_) == normalizedrequirement)
    val requirementtokens = _tokens(requirement)
    val evidence = Vector(
      observation.componentName,
      observation.organization,
      observation.componentKind,
      observation.version
    ).flatten.mkString(" ")
    exact || requirementtokens.intersect(_tokens(evidence)).nonEmpty
  }

  private def _matches_filters(
    observation: ReconciliationObservation,
    query: SourceAwareComponentSearchQuery
  ): Boolean =
    query.organization.forall(value => observation.organization.exists(_.equalsIgnoreCase(value))) &&
      query.componentKind.forall(value => observation.componentKind.exists(_.equalsIgnoreCase(value))) &&
      query.version.forall(value => observation.version.contains(value)) &&
      query.sourceId.forall(_ == observation.sourceId) &&
      query.sourceKind.forall(_ == observation.sourceKind) &&
      query.freshness.forall(_ == observation.freshness) &&
      query.versionState.forall(value => observation.versionState.contains(value))

  private def _filter_warnings(query: SourceAwareComponentSearchQuery): Vector[String] =
    Vector(
      query.sourceKind.filterNot(InformationSourceKind.ALL.contains).map(value => s"Unsupported sourceKind filter: $value."),
      query.freshness.filterNot(SourceAwareRetrieval.FRESHNESS_VALUES.contains).map(value => s"Unsupported freshness filter: $value."),
      query.versionState.filterNot(VersionAvailabilityState.ALL.contains).map(value => s"Unsupported versionState filter: $value."),
      query.conflictCode.filterNot(ReconciliationIssueCode.ALL.contains).map(value => s"Unsupported conflictCode filter: $value."),
      query.purpose.filterNot(ReconciliationPurpose.ALL.contains).map(value => s"Unsupported purpose filter: $value; published-reuse was used.")
    ).flatten

  private def _tokens(value: String): Set[String] =
    _normalize(value).split("[^\\p{L}\\p{N}._:-]+").toSet.map(_.trim).filter(_.nonEmpty)

  private def _normalize(value: String): String =
    value.trim.toLowerCase(Locale.ROOT)

  val FRESHNESS_VALUES: Vector[String] = Vector("fresh", "stale", "observed")
}
