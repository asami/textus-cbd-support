package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

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

      Then("only provider documents can enter the request and CBD returns its report/gate result")
      schema.hcursor.get[String]("$id").toOption shouldBe Some("https://simplemodeling.org/schema/textus/cbd/car-review-submission-v1.schema.json")
      request.hcursor.get[Boolean]("additionalProperties").toOption shouldBe Some(false)
      response.hcursor.get[Boolean]("additionalProperties").toOption shouldBe Some(false)
      _properties(request).intersect(Set("workspacePath", "projectRoot", "command", "environment", "template", "report", "gate")) shouldBe empty
      _properties(response) should contain allOf ("report", "gateResult")
      _properties(definitions("providerDocuments").getOrElse(fail("Missing provider documents definition."))) shouldBe Set("availability", "descriptor", "providerRequest", "bundle")
    }
  }

  private def _json(path: Path): Json =
    parse(Files.readString(path)).fold(error => fail(error.message), identity)

  private def _properties(json: Json): Set[String] =
    json.hcursor.downField("properties").focus.flatMap(_.asObject).map(_.keys.toSet).getOrElse(fail("Missing properties."))
}
