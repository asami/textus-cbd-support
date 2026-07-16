package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.security.MessageDigest

import io.circe.{Json, JsonObject, Printer}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewReportContractSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review Report v1 contract" should {
    "define the canonical vocabulary" which {
      "publishes strict Run, Report, and attestation documents with explicit assessment terms" in {
        Given("the normative CBD-owned Review Report JSON Schema")
        val schema = _load_json(_schema_path)

        When("the document definitions and controlled vocabularies are inspected")
        val definitions = _definitions(schema)

        Then("one immutable schema identity owns all three document shapes")
        _string(schema, "$id") shouldBe "https://simplemodeling.org/schema/textus/cbd/car-review-report-v1.schema.json"
        Vector("reviewRun", "reviewReport", "reviewAttestation").foreach { name =>
          val definition = definitions(name).getOrElse(fail(s"Missing schema definition: $name"))
          definition.hcursor.get[Boolean]("additionalProperties").toOption shouldBe Some(false)
        }

        And("Finding, Assurance, Unknown, disposition, applicability, and maturity remain distinct")
        _enum(definitions, "observation", "type") shouldBe Set("finding", "assurance", "unknown")
        _enum(definitions, "observation", "severity") shouldBe Set("info", "low", "medium", "high", "critical")
        _enum(definitions, "observation", "confidence") shouldBe Set("low", "medium", "high")
        _enum(definitions, "disposition", "state") shouldBe Set("active", "accepted", "suppressed", "deferred")
        _enum(definitions, "assessment", "applicability") shouldBe Set("applicable", "not-applicable", "unknown")
        _enum(definitions, "assessment", "maturity") shouldBe Set(
          "unassessed", "missing", "ad-hoc", "partial", "established", "verified", "operational"
        )

        And("schema conditions forbid ambiguous Observation, coverage, provider, and Run states")
        val findingcondition = _condition(definitions, "observation", "type", "finding")
        _string_array(_field(findingcondition, "then"), "required") should contain("severity")
        _string_array(_field(_field(findingcondition, "else"), "not"), "required") should contain("severity")
        val assurancecondition = _condition(definitions, "observation", "type", "assurance")
        val assuranceproperties = _field(_field(assurancecondition, "then"), "properties")
        _integer(_field(assuranceproperties, "evidenceIds"), "minItems") shouldBe 1
        val coveragecondition = _condition(definitions, "assessment", "applicability", "applicable")
        val applicablecoverage = _field(_field(_field(coveragecondition, "then"), "properties"), "coverage")
        val inapplicablecoverage = _field(_field(_field(coveragecondition, "else"), "properties"), "coverage")
        _string(applicablecoverage, "$ref") shouldBe "#/$defs/coverage"
        _string(inapplicablecoverage, "type") shouldBe "null"
        val providercondition = _condition(definitions, "providerExecution", "state", "completed")
        _string_array(_field(providercondition, "then"), "required").toSet shouldBe
          Set("bundleDigest", "startedAt", "completedAt")
        val runcondition = _condition(definitions, "reviewRun", "state", "completed")
        _string_array(_field(runcondition, "then"), "required").toSet shouldBe
          Set("completedAt", "reportId", "reportDigest")
        val forbiddenrunfields = _json_array(
          _field(_field(runcondition, "else"), "not"),
          "anyOf"
        ).flatMap(_string_array(_, "required")).toSet
        forbiddenrunfields shouldBe Set("reportId", "reportDigest")
      }
    }

    "preserve canonical report integrity" which {
      "keeps every Observation and assessment reference local and attributable" in {
        Given("a report containing one Finding, one Assurance, and one Unknown")
        val report = _load_json(_report_path)

        When("canonical Evidence, Observation, gate, baseline, and assessment references are inspected")
        val evidence = _json_array(report, "evidence")
        val observations = _json_array(report, "observations")
        val assessments = _json_array(report, "assessments")
        val evidenceids = evidence.map(_string(_, "id"))
        val observationids = observations.map(_string(_, "id"))
        val evidenceidset = evidenceids.toSet
        val observationidset = observationids.toSet
        val assessmentids = assessments.map(_string(_, "capabilityId"))
        val findingids = observations.filter(_string(_, "type") == "finding").map(_string(_, "id")).toSet
        val reportproviders = _provider_bindings(_json_array(_field(report, "execution"), "providers"))
        val observationevidence = observations.flatMap(_string_array(_, "evidenceIds")).toSet
        val assessmentevidence = assessments.flatMap(_string_array(_, "evidenceIds")).toSet
        val assessmentobservations = assessments.flatMap(_string_array(_, "observationIds")).toSet
        val blockingids = _string_array(_field(report, "gate"), "blockingObservationIds").toSet

        Then("IDs are unique and all local references resolve without a hidden provider winner")
        evidenceids.distinct shouldBe evidenceids
        observationids.distinct shouldBe observationids
        assessmentids.distinct shouldBe assessmentids
        observationevidence.subsetOf(evidenceidset) shouldBe true
        assessmentevidence.subsetOf(evidenceidset) shouldBe true
        assessmentobservations.subsetOf(observationidset) shouldBe true
        blockingids.subsetOf(findingids) shouldBe true
        observations.map(_string(_, "type")).toSet shouldBe Set("finding", "assurance", "unknown")
        evidence.map { item =>
          val providerid = _string(item, "providerId")
          val bundledigest = _string(item, "bundleDigest")
          reportproviders.exists(binding => binding.providerid == providerid && binding.bundledigest == bundledigest)
        }.forall(identity) shouldBe true
        observations.map(item => _provider_binding(_field(item, "provider"))).toSet.subsetOf(reportproviders) shouldBe true

        And("baseline current-report sets resolve and remain disjoint")
        val baseline = _field(report, "baseline")
        val addedids = _string_array(baseline, "addedObservationIds").toSet
        val unchangedids = _string_array(baseline, "unchangedObservationIds").toSet
        addedids.subsetOf(observationidset) shouldBe true
        unchangedids.subsetOf(observationidset) shouldBe true
        addedids.intersect(unchangedids) shouldBe empty

        And("only Findings carry severity and non-active dispositions retain reason and author")
        observations.foreach { observation =>
          val observationtype = _string(observation, "type")
          val severity = observation.hcursor.get[String]("severity").toOption
          if observationtype == "finding" then severity should not be empty
          else severity shouldBe empty
          if observationtype == "assurance" then _string_array(observation, "evidenceIds") should not be empty
          val disposition = _field(observation, "disposition")
          if _string(disposition, "state") != "active" then {
            _string(disposition, "reason") should not be empty
            _string(disposition, "author") should not be empty
          }
        }
      }

      "uses exact integer coverage and an immutable report digest" in {
        Given("one applicable capability assessment in the canonical report")
        val report = _load_json(_report_path)
        val assessments = _json_array(report, "assessments")

        When("coverage counts and normalized report content are evaluated")
        val normalizedreport = _normalized_report_content(report)
        val alternatereport = _alternative_run_metadata(_reverse_arrays(report))

        Then("each applicable coverage record preserves denominator and Unknown accounting")
        assessments.foreach { assessment =>
          _string(assessment, "applicability") shouldBe "applicable"
          val coverage = _field(assessment, "coverage")
          val applicable = _integer(coverage, "applicableSubjects")
          val assessed = _integer(coverage, "assessedSubjects")
          val unknown = _integer(coverage, "unknownSubjects")
          assessed + unknown shouldBe applicable
          _integer(coverage, "basisPoints") shouldBe assessed * 10000 / applicable
        }

        And("the report digest binds deterministic canonical report content")
        _string(report, "reportDigest") shouldBe _sha256_json(normalizedreport)
        _sha256_json(_normalized_report_content(alternatereport)) shouldBe _string(report, "reportDigest")
        _string(report, "reportDigest") should fullyMatch regex _digest_pattern
      }
    }

    "bind execution and CI identity" which {
      "links the completed Run and attestation to exactly one report, target, profile, providers, and gate" in {
        Given("one completed Review Run, canonical report, and Review attestation")
        val run = _load_json(_run_path)
        val report = _load_json(_report_path)
        val attestation = _load_json(_attestation_path)

        When("their cross-document identities are compared")
        val reportproviderdocuments = _json_array(_field(report, "execution"), "providers")
        val runproviderdocuments = _json_array(run, "providers")
        val attestedproviderdocuments = _json_array(attestation, "providers")
        val reportproviders = _provider_bindings(reportproviderdocuments)
        val runproviders = _provider_bindings(runproviderdocuments)
        val attestedproviders = _provider_bindings(attestedproviderdocuments)
        val normalizedattestation = attestation.mapObject(_.remove("attestationDigest"))

        Then("completed execution and CI evidence cannot drift from the canonical report")
        _string(run, "state") shouldBe "completed"
        _string(run, "reviewId") shouldBe _string(report, "reviewId")
        _string(run, "reportId") shouldBe _string(report, "reportId")
        _string(run, "reportDigest") shouldBe _string(report, "reportDigest")
        _field(run, "target") shouldBe _field(report, "target")
        _string(run, "profile") shouldBe _string(report, "profile")
        runproviderdocuments shouldBe reportproviderdocuments
        runproviders shouldBe reportproviders
        _string(attestation, "reviewId") shouldBe _string(report, "reviewId")
        _string(attestation, "reportId") shouldBe _string(report, "reportId")
        _string(attestation, "reportDigest") shouldBe _string(report, "reportDigest")
        _string(attestation, "targetDigest") shouldBe _string(_field(report, "target"), "digest")
        _string(attestation, "profile") shouldBe _string(report, "profile")
        attestedproviders shouldBe reportproviders
        _field(attestation, "gate") shouldBe _field(report, "gate")

        And("the attestation digest binds the exact CI artifact content")
        _string(attestation, "attestationDigest") shouldBe _sha256_json(normalizedattestation)
        _string(attestation, "attestationDigest") should fullyMatch regex _digest_pattern
      }
    }
  }

  private val _schema_path = Path.of("docs", "spec", "schema", "car-review-report-v1.schema.json")
  private val _run_path = Path.of("docs", "spec", "examples", "car-review-run-v1.json")
  private val _report_path = Path.of("docs", "spec", "examples", "car-review-report-v1.json")
  private val _attestation_path = Path.of("docs", "spec", "examples", "car-review-attestation-v1.json")
  private val _canonical_printer = Printer.noSpaces.copy(sortKeys = true)
  private val _digest_pattern = "sha256:[0-9a-f]{64}"

  private def _load_json(path: Path): Json =
    parse(Files.readString(path)).fold(
      error => fail(s"Invalid JSON at $path: ${error.message}"),
      identity
    )

  private def _definitions(schema: Json): JsonObject =
    schema.hcursor.downField("$defs").focus.flatMap(_.asObject).getOrElse {
      fail("The report schema has no $defs object.")
    }

  private def _enum(definitions: JsonObject, definitionname: String, field: String): Set[String] = {
    val definition = definitions(definitionname).getOrElse(fail(s"Missing schema definition: $definitionname"))
    val property = definition.hcursor.downField("properties").downField(field).focus.getOrElse {
      fail(s"Missing schema property: $definitionname.$field")
    }
    _string_array(property, "enum").toSet
  }

  private def _condition(
    definitions: JsonObject,
    definitionname: String,
    field: String,
    value: String
  ): Json = {
    val definition = definitions(definitionname).getOrElse(fail(s"Missing schema definition: $definitionname"))
    _json_array(definition, "allOf").find { condition =>
      condition.hcursor
        .downField("if")
        .downField("properties")
        .downField(field)
        .get[String]("const")
        .toOption
        .contains(value)
    }.getOrElse(fail(s"Missing schema condition: $definitionname.$field=$value"))
  }

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

  private final case class ProviderBinding(
    providerid: String,
    providerversion: String,
    rulesetid: String,
    rulesetversion: String,
    bundledigest: String
  )

  private def _provider_bindings(providers: Vector[Json]): Set[ProviderBinding] =
    providers.map(_provider_binding).toSet

  private def _provider_binding(provider: Json): ProviderBinding = {
    val provideridentity = _field(provider, "provider")
    val rulesetidentity = _field(provider, "ruleSet")
    ProviderBinding(
      _string(provideridentity, "id"),
      _string(provideridentity, "version"),
      _string(rulesetidentity, "id"),
      _string(rulesetidentity, "version"),
      _string(provider, "bundleDigest")
    )
  }

  private def _normalized_report_content(report: Json): Json = {
    val withoutrootvolatile = report.mapObject(
      _.remove("reportDigest").remove("reportId").remove("reviewId").remove("createdAt")
    )
    val withoutexecutionvolatile = withoutrootvolatile.mapObject { root =>
      val execution = _field(withoutrootvolatile, "execution").mapObject { value =>
        val providers = _json_array(_field(withoutrootvolatile, "execution"), "providers").map(
          _.mapObject(_.remove("startedAt").remove("completedAt"))
        )
        value
          .remove("startedAt")
          .remove("completedAt")
          .add("providers", Json.fromValues(providers))
      }
      val withoutbaselineidentity = root("baseline").map(_.mapObject(_.remove("reportId")))
      val withexecution = root.add("execution", execution)
      withoutbaselineidentity.fold(withexecution)(withexecution.add("baseline", _))
    }
    _canonicalize_arrays(withoutexecutionvolatile)
  }

  private def _alternative_run_metadata(report: Json): Json =
    report.mapObject { root =>
      val execution = _field(report, "execution").mapObject { value =>
        val providers = _json_array(_field(report, "execution"), "providers").map(
          _.mapObject(
            _.add("startedAt", Json.fromString("2030-01-01T00:00:01Z"))
              .add("completedAt", Json.fromString("2030-01-01T00:00:02Z"))
          )
        )
        value
          .add("startedAt", Json.fromString("2030-01-01T00:00:00Z"))
          .add("completedAt", Json.fromString("2030-01-01T00:00:03Z"))
          .add("providers", Json.fromValues(providers))
      }
      val baseline = root("baseline").map(_.mapObject(_.add("reportId", Json.fromString("another-baseline"))))
      val changed = root
        .add("reportId", Json.fromString("another-report"))
        .add("reviewId", Json.fromString("another-review"))
        .add("createdAt", Json.fromString("2030-01-01T00:00:04Z"))
        .add("execution", execution)
      baseline.fold(changed)(changed.add("baseline", _))
    }

  private def _canonicalize_arrays(json: Json): Json =
    json.arrayOrObject(
      json,
      values => {
        val normalized = values.map(_canonicalize_arrays)
        Json.fromValues(normalized.sortBy(_canonical_printer.print))
      },
      fields => Json.fromJsonObject(
        JsonObject.fromIterable(fields.toVector.map { case (key, value) => key -> _canonicalize_arrays(value) })
      )
    )

  private def _reverse_arrays(json: Json): Json =
    json.arrayOrObject(
      json,
      values => Json.fromValues(values.reverse.map(_reverse_arrays)),
      fields => Json.fromJsonObject(
        JsonObject.fromIterable(fields.toVector.map { case (key, value) => key -> _reverse_arrays(value) })
      )
    )

  private def _sha256_json(json: Json): String = {
    val bytes = _canonical_printer.print(json).getBytes(StandardCharsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }
}
