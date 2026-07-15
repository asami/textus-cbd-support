package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.security.MessageDigest

import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewProviderContractSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review Provider v1 contract" should {
    "define the static schema" which {
      "publishes three strict document shapes under one immutable identity" in {
        Given("the normative CAR Review Provider JSON Schema")
        val schema = _load_json(_schema_path)

        When("its root identity and document definitions are inspected")
        val definitions = schema.hcursor.downField("$defs").focus.flatMap(_.asObject).getOrElse {
          fail("The provider schema has no $defs object.")
        }

        Then("descriptor, request, and evidence bundle remain explicit strict documents")
        _string(schema, "$id") shouldBe "https://simplemodeling.org/schema/textus/cbd/car-review-provider-v1.schema.json"
        definitions.keys.toSet should contain allOf (
          "providerDescriptor",
          "providerRequest",
          "evidenceBundle",
          "capability",
          "limitation",
          "digest",
          "ruleSelector"
        )
        Vector("providerDescriptor", "providerRequest", "evidenceBundle").foreach { name =>
          val definition = definitions(name).getOrElse(fail(s"Missing schema definition: $name"))
          definition.hcursor.get[Boolean]("additionalProperties").toOption shouldBe Some(false)
        }

        And("rule selectors admit exact IDs or a bounded namespace wildcard")
        val selectorpattern = _string(definitions("ruleSelector").getOrElse(fail("Missing ruleSelector")), "pattern").r
        "cozy.car.identity-consistency" should fullyMatch regex selectorpattern
        "cozy.car.*" should fullyMatch regex selectorpattern
        "*" should not fullyMatch regex(selectorpattern)
      }
    }

    "bind one representative provider exchange" which {
      "keeps schema, capability, provider, rule set, review, and target identities consistent" in {
        Given("one descriptor, one CBD request, and one provider evidence bundle")
        val descriptor = _load_json(_descriptor_path)
        val request = _load_json(_request_path)
        val bundle = _load_json(_bundle_path)

        When("the exchange identities and requested capabilities are compared")
        val supportedversions = _string_array(descriptor, "supportedSchemaVersions")
        val advertisedcapabilities = _json_array(descriptor, "capabilities").map(_string(_, "id")).toSet
        val requestedcapabilities = _string_array(request, "requestedCapabilities").toSet

        Then("all three documents use the admitted v1 contract without an implicit substitute")
        Vector(descriptor, request, bundle).map(_string(_, "schemaVersion")) should contain only
          "textus.cbd.review-provider.v1"
        _string(descriptor, "documentType") shouldBe "provider-descriptor"
        _string(request, "documentType") shouldBe "provider-request"
        _string(bundle, "documentType") shouldBe "evidence-bundle"
        supportedversions should contain("textus.cbd.review-provider.v1")
        requestedcapabilities.subsetOf(advertisedcapabilities) shouldBe true
        _field(descriptor, "provider") shouldBe _field(bundle, "provider")
        _field(descriptor, "ruleSet") shouldBe _field(bundle, "ruleSet")
        _string(request, "reviewId") shouldBe _string(bundle, "reviewId")
        _field(request, "target") shouldBe _field(bundle, "target")

        And("rule selection and execution limits are explicit and bounded")
        val rules = _field(request, "rules")
        val includedrules = _string_array(rules, "include").toSet
        val excludedrules = _string_array(rules, "exclude").toSet
        includedrules.intersect(excludedrules) shouldBe empty
        val limits = _field(request, "limits")
        _integer(limits, "maxEvidenceItems") should be > 0
        _integer(limits, "maxObservations") should be > 0
        _integer(limits, "maxInputBytes") should be > 0
        _integer(limits, "timeoutMillis") should be > 0
      }

      "preserves local evidence references and recomputable request and bundle digests" in {
        Given("the representative request and its attributable evidence bundle")
        val request = _load_json(_request_path)
        val bundle = _load_json(_bundle_path)

        When("evidence IDs, observation references, and canonical digests are evaluated")
        val evidenceids = _json_array(bundle, "evidence").map(_string(_, "id"))
        val observationids = _json_array(bundle, "observations").map(_string(_, "id"))
        val evidenceidset = evidenceids.toSet
        val references = _json_array(bundle, "observations").flatMap(_string_array(_, "evidenceIds"))
        val normalizedbundle = bundle.mapObject(_.remove("bundleDigest"))

        Then("identities remain unique and every observation reference resolves locally")
        evidenceids.distinct shouldBe evidenceids
        observationids.distinct shouldBe observationids
        references.toSet.subsetOf(evidenceidset) shouldBe true

        And("the bundle is bound to the exact request and to its own normalized content")
        _string(bundle, "requestDigest") shouldBe _sha256_json(request)
        _string(bundle, "bundleDigest") shouldBe _sha256_json(normalizedbundle)
        _string(bundle, "requestDigest") should fullyMatch regex _digest_pattern
        _string(bundle, "bundleDigest") should fullyMatch regex _digest_pattern
      }
    }
  }

  private val _schema_path = Path.of("docs", "spec", "schema", "car-review-provider-v1.schema.json")
  private val _descriptor_path = Path.of("docs", "spec", "examples", "car-review-provider-descriptor-v1.json")
  private val _request_path = Path.of("docs", "spec", "examples", "car-review-provider-request-v1.json")
  private val _bundle_path = Path.of("docs", "spec", "examples", "car-review-evidence-bundle-v1.json")
  private val _canonical_printer = Printer.noSpaces.copy(sortKeys = true)
  private val _digest_pattern = "sha256:[0-9a-f]{64}"

  private def _load_json(path: Path): Json =
    parse(Files.readString(path)).fold(
      error => fail(s"Invalid JSON at $path: ${error.message}"),
      identity
    )

  private def _field(json: Json, field: String): Json =
    json.hcursor.downField(field).focus.getOrElse(fail(s"Missing JSON field: $field"))

  private def _string(json: Json, field: String): String =
    json.hcursor.get[String](field).fold(
      error => fail(s"Invalid string field $field: ${error.message}"),
      identity
    )

  private def _json_array(json: Json, field: String): Vector[Json] =
    json.hcursor.get[Vector[Json]](field).fold(
      error => fail(s"Invalid JSON array $field: ${error.message}"),
      identity
    )

  private def _integer(json: Json, field: String): Int =
    json.hcursor.get[Int](field).fold(
      error => fail(s"Invalid integer field $field: ${error.message}"),
      identity
    )

  private def _string_array(json: Json, field: String): Vector[String] =
    json.hcursor.get[Vector[String]](field).fold(
      error => fail(s"Invalid string array $field: ${error.message}"),
      identity
    )

  private def _sha256_json(json: Json): String = {
    val bytes = _canonical_printer.print(json).getBytes(StandardCharsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }
}
