package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewCiArtifactContractSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review CI artifact contract" should {
    "bind one immutable CI attempt directory to its attestation and canonical artifacts" in {
      Given("the normative artifact-manifest schema and representative unknown-gate manifest")
      val schema = _load_json(_schema_path)
      val manifest = _load_json(_manifest_path)

      When("the required identity, artifact, retention, and gate fields are inspected")
      val required = _string_array(schema, "required").toSet
      val artifacts = _field(manifest, "artifacts")
      val attestationdigest = _string(manifest, "attestationDigest")

      Then("one attestation digest selects a collision-free target directory and all canonical projections")
      _string(schema, "$id") shouldBe
        "https://simplemodeling.org/schema/textus/cbd/car-review-ci-artifact-manifest-v1.schema.json"
      required should contain allOf (
        "reviewId", "reportId", "reportDigest", "targetDigest", "attestationDigest",
        "profile", "limitations", "gate", "exitCode", "artifactDirectory", "retention", "artifacts"
      )
      _string(manifest, "artifactDirectory") shouldBe s"target/cbd-review/${attestationdigest.replace(':', '-')}"
      _string(manifest, "reportDigest") should startWith("sha256:")
      _string(manifest, "targetDigest") should startWith("sha256:")
      attestationdigest should startWith("sha256:")
      val expectedpaths = Map(
        "canonicalResponse" -> "canonical-response.json",
        "report" -> "report.json",
        "attestation" -> "attestation.json",
        "markdown" -> "report.md",
        "pdf" -> "report.pdf",
        "html" -> "report.html",
        "sarif" -> "report.sarif"
      )
      _artifact_paths(artifacts) shouldBe expectedpaths
      _schema_artifact_path_constants(schema) shouldBe expectedpaths
      _artifact_digests(artifacts).foreach(_ should startWith("sha256:"))
    }

    "make gate, exit, retention, and integration behavior explicit without local policy invention" in {
      Given("the same representative manifest and its normative contract")
      val manifest = _load_json(_manifest_path)
      val contract = Files.readString(_contract_path)
      val gate = _field(manifest, "gate")
      val retention = _field(manifest, "retention")

      When("the CI consumer reads the CBD-owned gate and retention posture")
      val exitcode = _int(manifest, "exitCode")

      Then("unknown remains attributable, retained, and non-publishing with its dedicated process code")
      _string(gate, "result") shouldBe "unknown"
      exitcode shouldBe 3
      Map("pass" -> 0, "fail" -> 2, "unknown" -> 3)(_string(gate, "result")) shouldBe exitcode
      _string(retention, "mode") shouldBe "ci-workspace"
      _string_array(manifest, "limitations") shouldBe Vector("provider:sbt-cozy unavailable in offline profile")
      _string_array(retention, "preserveOn").toSet shouldBe Set("pass", "fail", "unknown")
      Vector("publication", "distribution", "deployment").foreach { field =>
        _string(retention, field) shouldBe "not-triggered"
      }
      contract should include("not rerun a provider")
      contract should include("does not silently")
      contract should include("offline deterministic")
    }
  }

  private val _contract_path = Path.of("docs", "spec", "car-review-ci-artifact-contract.md")
  private val _schema_path = Path.of("docs", "spec", "schema", "car-review-ci-artifact-manifest-v1.schema.json")
  private val _manifest_path = Path.of("docs", "spec", "examples", "car-review-ci-artifact-manifest-v1.json")

  private def _load_json(path: Path): Json =
    parse(Files.readString(path)).fold(error => fail(s"${path}: ${error.message}"), identity)

  private def _field(value: Json, name: String): Json =
    value.hcursor.downField(name).focus.getOrElse(fail(s"Missing JSON field: $name"))

  private def _string(value: Json, name: String): String =
    value.hcursor.get[String](name).fold(error => fail(s"$name: ${error.message}"), identity)

  private def _int(value: Json, name: String): Int =
    value.hcursor.get[Int](name).fold(error => fail(s"$name: ${error.message}"), identity)

  private def _string_array(value: Json, name: String): Vector[String] =
    value.hcursor.get[Vector[String]](name).fold(error => fail(s"$name: ${error.message}"), identity)

  private def _artifact_paths(value: Json): Map[String, String] =
    value.asObject.getOrElse(fail("artifacts must be an object")).toMap.map { case (name, artifact) =>
      name -> _string(artifact, "path")
    }

  private def _artifact_digests(value: Json): Vector[String] =
    value.asObject.getOrElse(fail("artifacts must be an object")).values.toVector.map(_string(_, "sha256"))

  private def _schema_artifact_path_constants(schema: Json): Map[String, String] = {
    val properties = _field(_field(_field(schema, "$defs"), "artifacts"), "properties")
    properties.asObject.getOrElse(fail("schema artifact properties must be an object")).toMap.map { case (name, definition) =>
      val constants = definition.hcursor.get[Vector[Json]]("allOf").fold(error => fail(s"$name: ${error.message}"), identity)
      val path = constants.flatMap(_.hcursor.downField("properties").downField("path").get[String]("const").toOption).headOption
        .getOrElse(fail(s"$name: missing fixed artifact path"))
      name -> path
    }
  }
}
