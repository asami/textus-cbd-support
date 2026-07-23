package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}
import java.util.Base64

import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewArtifactBundleSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CBD Review artifact bundle" should {
    "render CBD-owned Markdown and PDF from one exact canonical response without provider work" in {
      Given("one canonical Review response")
      val response = _response

      When("the private submission boundary materializes its artifact bundle")
      val bundle = CarReviewArtifactBundle.render(response).fold(error => fail(error), identity)
      val json = parse(bundle).fold(error => fail(error.message), identity)

      Then("the bundle retains the report digest and CBD-rendered Markdown/PDF bytes")
      json.hcursor.get[String]("documentType").toOption shouldBe Some("review-artifact-bundle")
      json.hcursor.get[String]("reportDigest").toOption shouldBe Some(_report.reportDigest.value)
      json.hcursor.get[String]("markdown").toOption.getOrElse(fail("markdown missing")) should include("CBD CAR Review")
      new String(Base64.getDecoder.decode(json.hcursor.get[String]("pdfBase64").toOption.getOrElse(fail("pdf missing"))).take(4), "ISO-8859-1") shouldBe "%PDF"
    }

    "refuse a response whose outer gate no longer matches its canonical Report" in {
      Given("one canonical response with a substituted outer gate")
      val response = _response.replace(s"\"gateResult\":\"${_report.gate.result.value}\"", "\"gateResult\":\"pass\"")

      When("CBD checks the response before rendering")
      val result = CarReviewArtifactBundle.render(response)

      Then("no renderer-local conclusion is created")
      result shouldBe Left("cbd-review-artifact-gate-mismatch")
    }

    "refuse an envelope whose declared document type is not canonical Review response" in {
      Given("one otherwise complete response with an incompatible document type")
      val response = _response.replace("\"documentType\":\"canonical-review-response\"", "\"documentType\":\"another-document\"")

      When("CBD checks the response before rendering")
      val result = CarReviewArtifactBundle.render(response)

      Then("the renderer does not accept a same-shaped but differently declared document")
      result shouldBe Left("cbd-review-artifact-response-document-type-invalid")
    }

    "retain a delivery-safe provider limitation in the CI artifact bundle" in {
      Given("one canonical Report with an attributable provider limitation containing an unsafe path")
      val report = CarReviewReportCodec.withCalculatedDigest(_report.copy(
        limitations = Vector(ReviewLimitation(
          "provider-timeout",
          ReviewLimitationScope("provider"),
          Some("cozy"),
          "The provider timed out while reading /private/review-input.",
          retryable = true
        ))
      )).fold(error => fail(error.message), identity)

      When("CBD renders the bounded delivery bundle")
      val bundle = CarReviewArtifactBundle.render(_response_for(report)).fold(error => fail(error), identity)
      val json = parse(bundle).fold(error => fail(error.message), identity)

      Then("the manifest input retains attribution while using the delivery redaction boundary")
      json.hcursor.get[Vector[String]]("limitations").toOption shouldBe Some(Vector(
        "provider:provider-timeout [cozy] The provider timed out while reading [redacted-path]"
      ))
    }
  }

  private lazy val _report =
    CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)

  private lazy val _response = _response_for(_report)

  private def _response_for(report: CarReviewReport): String = {
    val attestation = CarReviewAttestationCodec.fromReport(report).flatMap(CarReviewAttestationCodec.encode).fold(error => fail(error.message), identity)
    Printer.noSpaces.print(Json.obj(
      "schemaVersion" -> Json.fromString("textus.cbd.review-submission.v1"),
      "documentType" -> Json.fromString("canonical-review-response"),
      "report" -> parse(CarReviewReportCodec.encode(report).fold(error => fail(error.message), identity)).fold(error => fail(error.message), identity),
      "attestation" -> parse(attestation).fold(error => fail(error.message), identity),
      "gateResult" -> Json.fromString(report.gate.result.value)
    ))
  }
}
