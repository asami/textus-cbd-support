package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Json, JsonObject, Printer}

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Deterministic renderers of a CBD-owned canonical Report. */
final case class CarReviewReportProjection(
  text: String,
  canonicalJson: String,
  html: String,
  sarif: String
)

object CarReviewReportProjection {
  def render(report: CarReviewReport): Either[CarReviewCodecFailure, CarReviewReportProjection] =
    CarReviewReportCodec.encode(report).map { canonical =>
      val text = _text(report)
      CarReviewReportProjection(text, canonical, _html(text), _sarif(report))
    }

  private def _text(report: CarReviewReport): String = {
    val observations = report.observations.sortBy(_.id.value).map { observation =>
      val severity = observation.severity.map(_.value).getOrElse("none")
      s"- ${observation.id.value} | ${observation.`type`.value} | $severity | ${observation.rule.id.value} | ${observation.message}"
    }
    Vector(
      "CBD CAR Review",
      s"Review: ${report.reviewId.value}",
      s"Report: ${report.reportId.value}",
      s"Target: ${report.target.kind.value}:${report.target.name}@${report.target.digest.value}",
      s"Profile: ${report.profile.value}",
      s"Gate: ${report.gate.result.value}",
      "Observations:"
    ).++(observations).mkString("\n") + "\n"
  }

  private def _html(text: String): String =
    s"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>CBD CAR Review</title></head>
<body><main><h1>CBD CAR Review</h1><pre id="canonical-review-text">${_escape(text)}</pre></main></body></html>
"""

  private def _sarif(report: CarReviewReport): String = {
    val findings = report.observations.filter(_.`type`.value == "finding").sortBy(_.id.value)
    val results = findings.flatMap { finding =>
      finding.locations.flatMap(_location).headOption.map { location =>
        Json.obj(
          "level" -> Json.fromString(_sarif_level(finding.severity.map(_.value).getOrElse("info"))),
          "locations" -> Json.arr(location),
          "message" -> Json.obj("text" -> Json.fromString(finding.message)),
          "properties" -> Json.obj("observationId" -> Json.fromString(finding.id.value)),
          "ruleId" -> Json.fromString(finding.rule.id.value)
        )
      }
    }
    Printer.noSpaces.copy(sortKeys = true).print(Json.obj(
      "$$schema" -> Json.fromString("https://json.schemastore.org/sarif-2.1.0.json"),
      "runs" -> Json.arr(Json.obj(
        "invocations" -> Json.arr(Json.obj("executionSuccessful" -> Json.fromBoolean(report.gate.result.value == "pass"))),
        "properties" -> Json.obj(
          "gateResult" -> Json.fromString(report.gate.result.value),
          "omittedFindingCount" -> Json.fromInt(findings.size - results.size),
          "projection" -> Json.fromString("location-bearing-findings-only")
        ),
        "results" -> Json.fromValues(results),
        "tool" -> Json.obj("driver" -> Json.obj(
          "informationUri" -> Json.fromString("https://simplemodeling.org/"),
          "name" -> Json.fromString("textus-cbd-support")
        ))
      )),
      "version" -> Json.fromString("2.1.0")
    ))
  }

  private def _location(value: ReviewLocation): Option[Json] =
    value.uri.orElse(value.path).filter(_.nonEmpty).map { location =>
      val region = value.line.map(line => Json.obj("startLine" -> Json.fromInt(line)))
      val physical = Json.fromJsonObject(JsonObject.fromIterable(
        Vector("artifactLocation" -> Json.obj("uri" -> Json.fromString(location))) ++ region.map("region" -> _)
      ))
      Json.obj("physicalLocation" -> physical)
    }

  private def _sarif_level(severity: String): String = severity match {
    case "critical" | "high" => "error"
    case "medium" => "warning"
    case _ => "note"
  }

  private def _escape(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
}
