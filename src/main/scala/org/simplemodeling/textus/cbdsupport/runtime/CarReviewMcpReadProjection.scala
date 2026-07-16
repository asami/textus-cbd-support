package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewMcpSummary(
  reviewId: ReviewId,
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  target: String,
  profile: ReviewProfile,
  gate: ReviewGateResult,
  findingCount: Int,
  assuranceCount: Int,
  unknownCount: Int
)

final case class CarReviewMcpObservation(
  id: ReviewObservationId,
  `type`: ReviewObservationType,
  ruleId: ReviewRuleId,
  message: String,
  severity: Option[ReviewSeverity],
  providerId: ReviewProviderId,
  locations: Vector[String]
)

final case class CarReviewMcpReport(
  summary: CarReviewMcpSummary,
  providers: Vector[ReviewProviderExecution],
  observations: Vector[CarReviewMcpObservation],
  limitations: Vector[ReviewLimitation]
)

/** Bounded MCP-safe Report read model. It deliberately excludes Evidence facts and rationale. */
final class CarReviewMcpReadApplication(repository: CarReviewRepository) {
  import CarReviewMcpReadApplication.MAX_OBSERVATIONS

  def summary(reportid: ReviewReportId, roles: Set[String]): Consequence[CarReviewMcpSummary] =
    _authorized(roles).flatMap(_ => _report(reportid).map(_summary))

  def report(reportid: ReviewReportId, roles: Set[String]): Consequence[CarReviewMcpReport] =
    _authorized(roles).flatMap(_ => _report(reportid).map(_report_projection))

  def views(reportid: ReviewReportId, roles: Set[String]): Consequence[CarReviewViewProjection] =
    _authorized(roles).flatMap(_ => _report(reportid).map(CarReviewViewProjection.project))

  def findings(reportid: ReviewReportId, roles: Set[String], limit: Int): Consequence[Vector[CarReviewMcpObservation]] =
    _observations(reportid, roles, "finding", limit)

  def assurances(reportid: ReviewReportId, roles: Set[String], limit: Int): Consequence[Vector[CarReviewMcpObservation]] =
    _observations(reportid, roles, "assurance", limit)

  private def _observations(reportid: ReviewReportId, roles: Set[String], kind: String, limit: Int): Consequence[Vector[CarReviewMcpObservation]] =
    if (limit <= 0 || limit > MAX_OBSERVATIONS) Consequence.operationInvalid("review-mcp-observation-limit-invalid")
    else _authorized(roles).flatMap(_ => _report(reportid).map(_.observations.filter(_.`type`.value == kind).sortBy(_.id.value).take(limit).map(_observation)))

  private def _authorized(roles: Set[String]): Consequence[Unit] =
    CarReviewAuthorization.authorize("review.read-run", roles)

  private def _report(reportid: ReviewReportId): Consequence[CarReviewReport] =
    repository.report(reportid).map(Consequence.success).getOrElse(Consequence.operationNotFound(s"review report: ${reportid.value}"))

  private def _summary(report: CarReviewReport): CarReviewMcpSummary =
    CarReviewMcpSummary(
      report.reviewId,
      report.reportId,
      report.reportDigest,
      s"${report.target.kind.value}:${report.target.name}",
      report.profile,
      report.gate.result,
      report.observations.count(_.`type`.value == "finding"),
      report.observations.count(_.`type`.value == "assurance"),
      report.observations.count(_.`type`.value == "unknown")
    )

  private def _report_projection(report: CarReviewReport): CarReviewMcpReport =
    CarReviewMcpReport(
      _summary(report),
      report.execution.providers.sortBy(value => (value.provider.id.value, value.provider.version.value)),
      report.observations.sortBy(_.id.value).take(MAX_OBSERVATIONS).map(_observation),
      report.limitations.map(_limitation)
    )

  private def _observation(value: ReviewObservation): CarReviewMcpObservation =
    CarReviewMcpObservation(
      value.id,
      value.`type`,
      value.rule.id,
      InformationSourceDiagnosticPolicy.sanitize(value.message),
      value.severity,
      value.provider.provider.id,
      value.locations.flatMap(_location).distinct.sorted
    )

  private def _limitation(value: ReviewLimitation): ReviewLimitation =
    value.copy(message = InformationSourceDiagnosticPolicy.sanitize(value.message))

  private def _location(value: ReviewLocation): Option[String] =
    CarReviewMcpReadApplication.renderLocation(value)

  private def _basename(value: String): String =
    value.replace('\\', '/').split('/').filter(_.nonEmpty).lastOption.getOrElse("[redacted-location]")
}

object CarReviewMcpReadApplication {
  val MAX_OBSERVATIONS = 100

  def renderLocation(value: ReviewLocation): Option[String] =
    value.uri.map(uri => scala.util.Try(URI.create(uri)).toOption.map(InformationSourceDiagnosticPolicy.renderUri).getOrElse("[redacted-uri]")).orElse(
      value.path.map(_basename)
    )

  private def _basename(value: String): String =
    value.replace('\\', '/').split('/').filter(_.nonEmpty).lastOption.getOrElse("[redacted-location]")
}
