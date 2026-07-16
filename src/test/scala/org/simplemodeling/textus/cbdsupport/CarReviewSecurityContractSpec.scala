package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.{Json, JsonObject}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewSecurityContractSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review Security Policy v1 contract" should {
    "publish a deny-by-default bounded vocabulary" in {
      Given("the normative CBD-owned Review Security JSON Schema")
      val schema = _load_json(_schema_path)
      val definitions = _definitions(schema)

      When("the root and security boundary definitions are inspected")

      Then("one strict immutable schema owns the policy")
      _string(schema, "$id") shouldBe
        "https://simplemodeling.org/schema/textus/cbd/car-review-security-policy-v1.schema.json"
      _boolean(schema, "additionalProperties") shouldBe false
      _enum(_field(schema, "properties"), "profile") shouldBe Set("development", "ci", "release", "server")
      Vector(
        "targetMode",
        "targetAdmission",
        "authorizationRule",
        "authorization",
        "filesystem",
        "process",
        "network",
        "credentials",
        "redaction",
        "aiInput",
        "mcp",
        "retention",
        "reproducibility"
      ).foreach { name =>
        _boolean(definitions(name).getOrElse(fail(s"Missing schema definition: $name")), "additionalProperties") shouldBe false
      }

      And("target, authorization, network, and MCP states are closed vocabularies")
      _enum(_properties(definitions, "targetMode"), "mode") shouldBe Set(
        "local-development-directory", "local-car", "server-development-root", "uploaded-car"
      )
      _enum(_properties(definitions, "authorizationRule"), "mcpExposure") shouldBe Set("ready", "private")
      _enum(_properties(definitions, "network"), "mode") shouldBe Set("disabled", "exact-origin-allowlist")
      _enum(_properties(definitions, "process"), "mode") shouldBe Set("disabled", "exact-command-allowlist")
    }

    "contain targets, filesystems, processes, network, and credentials" in {
      Given("the representative development security policy")
      val policy = _load_json(_development_policy_path)

      When("every resource-bearing boundary is inspected")
      val modes = _json_array(_field(policy, "targetAdmission"), "modes")
      val modeids = modes.map(_string(_, "mode"))
      val servermodes = modes.filter(_string(_, "executor") == "server")
      val filesystem = _field(policy, "filesystem")
      val process = _field(policy, "process")
      val network = _field(policy, "network")
      val credentials = _field(policy, "credentials")

      Then("all four target modes are digest-bound and only local clients accept bounded caller paths")
      modeids.toSet shouldBe Set(
        "local-development-directory", "local-car", "server-development-root", "uploaded-car"
      )
      modeids.distinct shouldBe modeids
      servermodes.map(_string(_, "mode")).toSet shouldBe Set("server-development-root", "uploaded-car")
      modes.foreach { mode =>
        _boolean(mode, "canonicalRealPath") shouldBe true
        _boolean(mode, "followSymbolicLinks") shouldBe false
        _boolean(mode, "requireDigest") shouldBe true
        _positive(mode, "maxArtifactBytes")
        _string_array(mode, "rootRefs") should not be empty
      }
      servermodes.foreach(mode => _boolean(mode, "acceptsCallerPath") shouldBe false)
      modes.filter(_string(_, "executor") == "client").foreach { mode =>
        _boolean(mode, "acceptsCallerPath") shouldBe true
      }

      And("filesystem and process work is finite and cannot inherit ambient authority")
      _string(filesystem, "mode") shouldBe "bounded-read-only"
      _string_array(filesystem, "readRootRefs") should not be empty
      _string(filesystem, "writeRootRef") should startWith("output-root/")
      Vector("maxDepth", "maxFiles", "maxTotalBytes", "maxFileBytes").foreach(_positive(filesystem, _))
      _boolean(filesystem, "followSymbolicLinks") shouldBe false
      _string(process, "mode") shouldBe "exact-command-allowlist"
      _string_array(process, "commands").toSet shouldBe Set("cozy", "sbt")
      _string(process, "argumentMode") shouldBe "fixed-operation-templates"
      _string_array(process, "operationTemplates").toSet shouldBe Set(
        "cozy.inspect-evidence", "sbt.review-evidence"
      )
      _boolean(process, "inheritEnvironment") shouldBe false
      Vector("maxInvocations", "timeoutMilliseconds", "maxStdoutBytes", "maxStderrBytes").foreach(
        _positive(process, _)
      )

      And("default development performs no network or credential resolution")
      _string(network, "mode") shouldBe "disabled"
      _string_array(network, "origins") shouldBe empty
      _boolean(network, "followRedirects") shouldBe false
      Vector("maxRequests", "maxResponseBytes", "timeoutMilliseconds").foreach(_positive(network, _))
      _string_array(credentials, "referenceSchemes") shouldBe Vector("config-key")
      _string(credentials, "resolveAt") shouldBe "outbound-provider-boundary"
      Vector("persistReferences", "persistValues", "projectReferences", "projectValues").foreach { field =>
        _boolean(credentials, field) shouldBe false
      }
    }

    "keep projected, AI, MCP, and retained data safe" in {
      Given("the same development policy")
      val policy = _load_json(_development_policy_path)

      When("redaction, AI input, MCP publication, authorization, and retention are inspected")
      val redaction = _field(policy, "redaction")
      val aiinput = _field(policy, "aiInput")
      val mcp = _field(policy, "mcp")
      val authorizationrules = _json_array(_field(policy, "authorization"), "rules")
      val retention = _field(policy, "retention")

      Then("every persisted or projected surface shares one redaction boundary")
      _string_array(redaction, "surfaces").toSet shouldBe Set(
        "report", "attestation", "text", "html", "sarif", "log", "calltree", "mcp", "ai-input"
      )
      _string_array(redaction, "forbiddenContent").toSet should contain allOf (
        "credential", "credential-reference", "raw-source", "absolute-local-path", "ambient-environment",
        "provider-wire-payload"
      )
      Vector("stripUriUserInfo", "stripUriQuery", "stripUriFragment").foreach { field =>
        _boolean(redaction, field) shouldBe true
      }
      Vector("maxTextLength", "maxLocations").foreach(_positive(redaction, _))

      And("AI receives only bounded structured evidence and cannot override deterministic conclusions")
      _boolean(aiinput, "enabledByDefault") shouldBe false
      _string(aiinput, "mode") shouldBe "structured-evidence-only"
      _boolean(aiinput, "includeRawSource") shouldBe false
      _boolean(aiinput, "includeCredentials") shouldBe false
      _boolean(aiinput, "webSearch") shouldBe false
      _boolean(aiinput, "urlContext") shouldBe false
      _boolean(aiinput, "deterministicFindingsAuthority") shouldBe true
      _boolean(aiinput, "mayEstablishFinalAssurance") shouldBe false
      Vector("maxEvidenceItems", "maxInputBytes", "maxOutputBytes", "maxCostUnits").foreach(_positive(aiinput, _))

      And("MCP readiness is exactly the authorized read-only candidate set")
      val readyqueries = _string_array(mcp, "readyQueries").toSet
      val privateoperations = _string_array(mcp, "privateOperations").toSet
      readyqueries shouldBe Set(
        "getReviewRun", "getReviewSummary", "getReviewReport", "listReviewFindings", "listReviewAssurances"
      )
      privateoperations shouldBe Set(
        "startReview", "cancelReview", "deleteReview", "configureRetention", "enableExternalProvider",
        "enableAiProvider", "configureFilesystem"
      )
      _positive(mcp, "maxPageSize")
      Vector("reportQueriesRequireCompletedRun", "requireAuthorization", "requireRedaction").foreach { field =>
        _boolean(mcp, field) shouldBe true
      }
      authorizationrules.filter(_string(_, "mcpExposure") == "ready").map(_string(_, "action")).toSet shouldBe Set(
        "review.read-run", "review.read-summary", "review.read-report", "review.list-findings",
        "review.list-assurances"
      )

      And("retention is finite, immutable, and administratively deleted with audit")
      Vector(
        "runDays", "reportDays", "evidenceBundleDays", "uploadedArtifactDays", "maxRunsPerTarget",
        "maxBundlesPerRun"
      ).foreach(_positive(retention, _))
      _string(retention, "immutability") shouldBe "digest-bound-until-expiry-or-authorized-deletion"
      _string_array(retention, "deletionRoles") shouldBe Vector("admin")
      _boolean(retention, "auditDeletion") shouldBe true
    }

    "make the standard CI profile offline and reproducible" in {
      Given("the representative standard CI policy")
      val policy = _load_json(_ci_policy_path)

      When("its execution and reproducibility boundaries are inspected")
      val network = _field(policy, "network")
      val reproducibility = _field(policy, "reproducibility")
      val targetmodes = _json_array(_field(policy, "targetAdmission"), "modes")

      Then("CI uses only client-local digest-bound targets and deterministic providers")
      _string(policy, "profile") shouldBe "ci"
      targetmodes.map(_string(_, "executor")).toSet shouldBe Set("client")
      targetmodes.foreach(mode => _boolean(mode, "requireDigest") shouldBe true)
      _boolean(reproducibility, "offline") shouldBe true
      _boolean(reproducibility, "deterministic") shouldBe true
      _string_array(reproducibility, "enabledProviderClasses") shouldBe Vector("deterministic")
      _string_array(reproducibility, "disabledProviderClasses").toSet shouldBe Set(
        "heuristic", "external", "runtime-network", "ai"
      )
      _boolean(reproducibility, "pinnedProviderVersions") shouldBe true
      _boolean(reproducibility, "pinnedRuleSetVersions") shouldBe true

      And("network, credentials, locale, time, randomness, and report normalization are fixed")
      _string(network, "mode") shouldBe "disabled"
      _string_array(network, "origins") shouldBe empty
      _boolean(reproducibility, "resolveCredentials") shouldBe false
      _string(reproducibility, "locale") shouldBe "C"
      _string(reproducibility, "timezone") shouldBe "UTC"
      _integer(reproducibility, "randomSeed") shouldBe 0
      _string(reproducibility, "arrayOrdering") shouldBe "canonical-json"
      _boolean(reproducibility, "volatileReportFieldsExcludedFromDigest") shouldBe true
    }
  }

  private val _schema_path = Path.of("docs", "spec", "schema", "car-review-security-policy-v1.schema.json")
  private val _development_policy_path =
    Path.of("docs", "spec", "examples", "car-review-security-policy-development-v1.json")
  private val _ci_policy_path = Path.of("docs", "spec", "examples", "car-review-security-policy-ci-v1.json")

  private def _load_json(path: Path): Json =
    parse(Files.readString(path)).fold(
      error => fail(s"Invalid JSON at $path: ${error.message}"),
      identity
    )

  private def _definitions(schema: Json): JsonObject =
    _field(schema, "$defs").asObject.getOrElse(fail("The security policy schema has no $defs object."))

  private def _properties(definitions: JsonObject, definitionname: String): Json =
    _field(definitions(definitionname).getOrElse(fail(s"Missing schema definition: $definitionname")), "properties")

  private def _enum(properties: Json, field: String): Set[String] =
    _string_array(_field(properties, field), "enum").toSet

  private def _field(json: Json, field: String): Json =
    json.hcursor.downField(field).focus.getOrElse(fail(s"Missing JSON field: $field"))

  private def _string(json: Json, field: String): String =
    json.hcursor.get[String](field).fold(
      error => fail(s"Invalid string field $field: ${error.message}"),
      identity
    )

  private def _integer(json: Json, field: String): Int =
    json.hcursor.get[Int](field).fold(
      error => fail(s"Invalid integer field $field: ${error.message}"),
      identity
    )

  private def _boolean(json: Json, field: String): Boolean =
    json.hcursor.get[Boolean](field).fold(
      error => fail(s"Invalid boolean field $field: ${error.message}"),
      identity
    )

  private def _json_array(json: Json, field: String): Vector[Json] =
    json.hcursor.get[Vector[Json]](field).fold(
      error => fail(s"Invalid JSON array $field: ${error.message}"),
      identity
    )

  private def _string_array(json: Json, field: String): Vector[String] =
    json.hcursor.get[Vector[String]](field).fold(
      error => fail(s"Invalid string array $field: ${error.message}"),
      identity
    )

  private def _positive(json: Json, field: String): Unit =
    _integer(json, field) should be > 0
}
