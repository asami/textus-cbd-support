package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewSubmissionContractSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review submission v1 contract" should {
    "publish strict path-free request and CBD-owned response shapes" in {
      Given("the normative provider-document submission schema")
      val schema = _json(Path.of("docs", "spec", "schema", "car-review-submission-v1.schema.json"))

      When("the request and response definitions are inspected")
      val definitions = schema.hcursor.downField("$defs").focus.flatMap(_.asObject).getOrElse(fail("Missing schema definitions."))
      val request = definitions("providerDocumentSubmission").getOrElse(fail("Missing provider-document submission definition."))
      val response = definitions("canonicalReviewResponse").getOrElse(fail("Missing canonical response definition."))

      Then("only provider documents can enter the request and CBD returns its report, attestation, and gate result")
      schema.hcursor.get[String]("$id").toOption shouldBe Some("https://simplemodeling.org/schema/textus/cbd/car-review-submission-v1.schema.json")
      request.hcursor.get[Boolean]("additionalProperties").toOption shouldBe Some(false)
      response.hcursor.get[Boolean]("additionalProperties").toOption shouldBe Some(false)
      _properties(request).intersect(Set("workspacePath", "projectRoot", "command", "environment", "template", "report", "gate")) shouldBe empty
      _properties(response) should contain allOf ("report", "attestation", "gateResult")
      _properties(definitions("providerDocuments").getOrElse(fail("Missing provider documents definition."))) shouldBe Set("availability", "descriptor", "providerRequest", "bundle")
    }

    "decode one bounded provider-document request without a client template" in {
      Given("one submission document containing only identity and provider JSON")
      val request = Json.obj(
        "schemaVersion" -> Json.fromString("textus.cbd.review-submission.v1"),
        "documentType" -> Json.fromString("provider-document-submission"),
        "reviewId" -> Json.fromString("review-example-001"),
        "target" -> Json.obj("kind" -> Json.fromString("project"), "organization" -> Json.fromString("org.textus"), "name" -> Json.fromString("textus-user-account"), "version" -> Json.fromString("0.2.0-SNAPSHOT"), "digest" -> Json.fromString("sha256:" + ("a" * 64))),
        "providers" -> Json.arr(Json.obj("availability" -> Json.fromString("enabled"), "descriptor" -> Json.fromString(_document("car-review-provider-descriptor-v1.json")), "providerRequest" -> Json.fromString(_document("car-review-provider-request-v1.json")), "bundle" -> Json.fromString(_document("car-review-evidence-bundle-v1.json"))))
      )

      When("CBD decodes the transport-neutral request")
      val decoded = CarReviewSubmissionWireCodec.decodeRequest(_printer.print(request))

      Then("provider documents retain their Review/Target binding without a template field")
      decoded.map(_.bundles.map(_.reviewId)) shouldBe Right(Vector(ReviewId("review-example-001")))
    }
  }

  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  private def _json(path: Path): Json =
    parse(Files.readString(path)).fold(error => fail(error.message), identity)

  private def _document(name: String): String =
    Files.readString(Path.of("docs", "spec", "examples", name))

  private def _properties(json: Json): Set[String] =
    json.hcursor.downField("properties").focus.flatMap(_.asObject).map(_.keys.toSet).getOrElse(fail("Missing properties."))
}
