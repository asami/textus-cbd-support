package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.{Json, Printer}
import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewSubmissionTransportAdaptersSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD Review submission transport adapters" should {
    "reject a non-JSON HTTP request before the Review Application" in {
      Given("one private HTTP adapter")
      val adapter = new CarReviewSubmissionHttpAdapter(_wire_application)

      When("a caller posts a non-JSON content type")
      val response = adapter.postJson("text/plain", "{}", Set("reviewer"))

      Then("the shared Review Application is not invoked")
      response.isFaillure shouldBe true
    }

    "apply the same bounded input policy to CLI stdin" in {
      Given("one local CLI adapter")
      val adapter = new CarReviewSubmissionCliAdapter(_wire_application)

      When("a caller supplies bytes beyond the submission bound")
      val response = adapter.submitStdin("x" * (CarReviewSubmissionTransportAdapters.MAX_REQUEST_BYTES + 1), Set("reviewer"))

      Then("the command refuses input before JSON admission")
      response.isFaillure shouldBe true
    }

    "return one canonical response through either supported transport" in {
      Given("one valid provider-document submission and both adapters")
      val http = new CarReviewSubmissionHttpAdapter(_wire_application)
      val cli = new CarReviewSubmissionCliAdapter(_wire_application)

      When("the same document is submitted through HTTP and local stdin")
      val httpresponse = http.postJson("application/json", _submission_document, Set("reviewer")).fold(_fail_conclusion, identity)
      val cliresponse = cli.submitStdin(_submission_document, Set("reviewer")).fold(_fail_conclusion, identity)

      Then("both transports preserve the one CBD-owned canonical result")
      httpresponse shouldBe cliresponse
      httpresponse should include ("canonical-review-response")
      httpresponse should not include "workspace"
    }
  }

  private lazy val _wire_application =
    new CarReviewSubmissionWireApplication(new CarReviewProviderDocumentSubmissionApplication(new CarReviewCanonicalTemplateProvider {
      def template(reviewId: ReviewId, target: ReviewTarget): Consequence[CarReviewReport] =
        Consequence.success(CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity).copy(baseline = None))
    }))

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")

  private lazy val _submission_document =
    Printer.noSpaces.print(Json.obj(
      "schemaVersion" -> Json.fromString("textus.cbd.review-submission.v1"),
      "documentType" -> Json.fromString("provider-document-submission"),
      "reviewId" -> Json.fromString("review-example-001"),
      "target" -> Json.obj(
        "kind" -> Json.fromString("project"),
        "organization" -> Json.fromString("org.textus"),
        "name" -> Json.fromString("textus-user-account"),
        "version" -> Json.fromString("0.2.0-SNAPSHOT"),
        "digest" -> Json.fromString("sha256:" + ("a" * 64))
      ),
      "providers" -> Json.arr(Json.obj(
        "availability" -> Json.fromString("enabled"),
        "descriptor" -> Json.fromString(_document("car-review-provider-descriptor-v1.json")),
        "providerRequest" -> Json.fromString(_document("car-review-provider-request-v1.json")),
        "bundle" -> Json.fromString(_document("car-review-evidence-bundle-v1.json"))
      ))
    ))

  private def _document(name: String): String =
    Files.readString(Path.of("docs", "spec", "examples", name))

  private def _fail_conclusion(error: org.goldenport.Conclusion): Nothing =
    fail(error.toString)
}
