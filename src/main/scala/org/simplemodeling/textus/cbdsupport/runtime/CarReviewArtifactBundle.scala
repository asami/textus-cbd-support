package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.circe.{Json, Printer}
import io.circe.parser.parse

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
/** CBD-owned Markdown/PDF bundle over an already admitted canonical response. */
object CarReviewArtifactBundle {
  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  def render(canonicalresponse: String): Either[String, String] =
    for {
      response <- parse(canonicalresponse).left.map(_ => "cbd-review-artifact-response-invalid")
      fields <- _fields(response)
      _ <- fields.get("schemaVersion").flatMap(_.asString).filter(_ == "textus.cbd.review-submission.v1").toRight("cbd-review-artifact-response-schema-invalid")
      _ <- fields.get("documentType").flatMap(_.asString).filter(_ == "canonical-review-response").toRight("cbd-review-artifact-response-document-type-invalid")
      reportjson <- fields.get("report").toRight("cbd-review-artifact-report-missing")
      report <- CarReviewReportCodec.decode(_printer.print(reportjson)).left.map(_ => "cbd-review-artifact-report-invalid")
      gate <- fields.get("gateResult").flatMap(_.asString).filter(CarReviewVocabulary.GATE_RESULTS).toRight("cbd-review-artifact-gate-invalid")
      _ <- Either.cond(gate == report.gate.result.value, (), "cbd-review-artifact-gate-mismatch")
      _ <- fields.get("attestation").filter(_.isObject).toRight("cbd-review-artifact-attestation-missing")
      rendered = CarReviewDeliveryArtifactRenderer.render(CarReviewDeliveryProjection.project(report))
    } yield _printer.print(Json.obj(
      "documentType" -> Json.fromString("review-artifact-bundle"),
      "limitations" -> Json.fromValues(rendered.limitations.map(Json.fromString)),
      "markdown" -> Json.fromString(rendered.markdown),
      "pdfBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rendered.pdf)),
      "reportDigest" -> Json.fromString(report.reportDigest.value),
      "schemaVersion" -> Json.fromString("textus.cbd.review-artifact-bundle.v1")
    ))

  private def _fields(value: Json): Either[String, Map[String, Json]] =
    value.asObject.map(_.toMap).filter(_.keySet == Set("schemaVersion", "documentType", "report", "attestation", "gateResult"))
      .toRight("cbd-review-artifact-response-shape-invalid")
}
