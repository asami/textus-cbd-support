package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.{Json, Printer}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewCliSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CBD Support Review CLI" should {
    "submit local stdin through the canonical Review Application and retain run, response, gate, and exit semantics" in {
      Given("one process-authorized local CLI and provider-document submission")
      val cli = new CarReviewCli(new CarReviewSubmissionCliAdapter(_wire), _unused_server)

      When("the local command submits stdin")
      val result = cli.submitLocal(_submission_document, Set("reviewer")).fold(error => fail(error), identity)

      Then("the result identifies the canonical Review and gate without a workspace path")
      result.runId shouldBe "review-example-001"
      result.gate shouldBe "unknown"
      result.exitCode shouldBe 3
      result.render should include("review-cli-result")
      result.render should include("canonical-review-response")
      result.render should not include "workspace"
    }

    "delegate server-backed submission to the same canonical response contract without forwarding a local role" in {
      Given("a server transport whose authentication boundary returns CBD's canonical response")
      val response = new CarReviewSubmissionCliAdapter(_wire).submitStdin(_submission_document, Set("reviewer")).fold(error => fail(error.toString), identity)
      var submitted = ""
      val server = new CarReviewCliServerTransport {
        def submit(document: String): Either[String, String] = { submitted = document; Right(response) }
      }
      val cli = new CarReviewCli(new CarReviewSubmissionCliAdapter(_wire), server)

      When("the CLI selects its configured server route")
      val result = cli.submitServer(_submission_document).fold(error => fail(error), identity)

      Then("the server receives only the path-free provider submission and returns the same Review result")
      submitted shouldBe _submission_document
      result.runId shouldBe "review-example-001"
      result.gate shouldBe "unknown"
      result.exitCode shouldBe 3
    }

    "refuse a server response which omits the canonical Report run identity" in {
      val cli = new CarReviewCli(new CarReviewSubmissionCliAdapter(_wire), new CarReviewCliServerTransport {
        def submit(document: String): Either[String, String] = Right("""{"gateResult":"pass","report":{}}""")
      })

      cli.submitServer(_submission_document) shouldBe Left("cbd-review-cli-run-id-missing")
    }
  }

  private lazy val _wire =
    new CarReviewSubmissionWireApplication(new CarReviewProviderDocumentSubmissionApplication(
      new CarReviewDevelopmentTemplateProvider(
        ReviewInstant("2026-07-16T00:00:00Z"),
        () => ReviewReportId("report-cli-spec")
      )
    ))

  private lazy val _unused_server = new CarReviewCliServerTransport {
    def submit(document: String): Either[String, String] = Left("not-used")
  }

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
}
