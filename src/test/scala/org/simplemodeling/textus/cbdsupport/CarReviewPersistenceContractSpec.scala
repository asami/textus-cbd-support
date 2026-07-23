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
final class CarReviewPersistenceContractSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review persistence entity model" should {
    "retain every lineage, target, Run, Report, attestation, reuse, comparison, and retention identity immutably" in {
      Given("the P8 database-mappable entity model")
      val model = _load_model

      When("the declared entities and their fields are read")
      val entities = _entities(model)
      val names = _entity_names(model)
      val tables = entities.map(entity => _entity_value(entity, "table"))
      val primarykeys = entities.map(entity => _entity_value(entity, "primaryKey"))
      val relationships = entities.flatMap(_entity_relationships)

      Then("every retained identity has one distinct database table, declared key, and valid relation")
      names.distinct shouldBe names
      tables.distinct shouldBe tables
      names.toSet shouldBe Set(
        "car-lineage",
        "review-target",
        "review-run",
        "review-report",
        "review-attestation",
        "diagnosis-reuse-identity",
        "review-comparison",
        "review-retention-event"
      )
      entities.zip(primarykeys).foreach { case (entity, primarykey) =>
        _entity_fields(entity) should contain (primarykey)
      }
      relationships.forall(names.contains) shouldBe true
      _entity_value(model, "car-lineage", "immutability") shouldBe "append-only"
      _entity_value(model, "review-run", "immutability") shouldBe "terminal-snapshot"
      _entity_value(model, "review-report", "immutability") shouldBe "append-only"
      _entity_value(model, "review-attestation", "immutability") shouldBe "append-only"
      _entity_value(model, "review-retention-event", "immutability") shouldBe "append-only"
      _entity_fields(model, "review-target") should contain allOf ("lineageId", "componentVersion", "artifactIdentity", "targetDigest")
      _entity_fields(model, "review-run") should contain allOf ("targetId", "reportId", "reportDigest", "attestationId", "reuseIdentityId")
      _entity_fields(model, "review-report") should contain allOf ("reviewId", "targetId", "reportDigest", "canonicalPayload")
      _entity_fields(model, "review-attestation") should contain allOf ("reviewId", "reportId", "targetId", "reportDigest", "attestationDigest", "canonicalPayload")
      _entity_fields(model, "review-comparison") should contain allOf ("lineageId", "baselineReportId", "currentReportId", "configurationCompatibilityId")
      _entity_fields(model, "review-retention-event") should contain allOf ("action", "recordType", "recordId", "recordDigest", "reportDigest", "targetDigest", "effectiveAt")
    }

    "reserve an opaque reuse identity and append-only retention audit without inventing P8-41 inputs" in {
      Given("the persistence model and its normative contract")
      val model = _load_model
      val contract = Files.readString(_contract_path)

      When("reuse and retention boundaries are inspected")
      val reusefields = _entity_fields(model, "diagnosis-reuse-identity")
      val invariants = _string_vector(model, "invariants")
      val retentionrelationships = _entity_relationships(model, "review-retention-event")

      Then("the key remains opaque while immutable retention remains attributable and content-safe")
      reusefields should contain allOf ("keyDefinitionId", "reuseKeyDigest")
      reusefields should not contain "providerSelection"
      reusefields should not contain "runtimeEvidence"
      invariants.toSet should contain allOf ("atomic-completed-binding", "opaque-reuse-key", "append-only-retention-event", "no-sensitive-persistence")
      retentionrelationships.toSet shouldBe (_entity_names(model).toSet - "review-retention-event")
      contract should include("P8-41 owns the exact reuse-key input set")
      contract should include("must never construct it from report text")
    }
  }

  private val _model_path = Path.of("docs", "spec", "examples", "car-review-persistence-model-v1.json")
  private val _contract_path = Path.of("docs", "spec", "car-review-persistence-contract.md")

  private def _load_model: Json = {
    val model = parse(Files.readString(_model_path)).fold(error => fail(error.message), identity)
    model.hcursor.get[String]("schemaVersion").toOption shouldBe Some("textus.cbd.car-review-persistence-model.v1")
    model.hcursor.get[String]("documentType").toOption shouldBe Some("car-review-persistence-model")
    model
  }

  private def _entity_names(model: Json): Vector[String] =
    _entities(model).map(_.hcursor.get[String]("name").fold(error => fail(error.message), identity))

  private def _entity_fields(model: Json, name: String): Vector[String] =
    _entity_fields(_entity(model, name))

  private def _entity_fields(entity: Json): Vector[String] =
    entity.hcursor.get[Vector[String]]("fields").fold(error => fail(error.message), identity)

  private def _entity_value(model: Json, name: String, field: String): String =
    _entity_value(_entity(model, name), field)

  private def _entity_value(entity: Json, field: String): String =
    entity.hcursor.get[String](field).fold(error => fail(error.message), identity)

  private def _entity_relationships(model: Json, name: String): Vector[String] =
    _entity_relationships(_entity(model, name))

  private def _entity_relationships(entity: Json): Vector[String] =
    entity.hcursor.get[Vector[String]]("relationships").fold(error => fail(error.message), identity)

  private def _entity(model: Json, name: String): Json =
    _entities(model).find(_.hcursor.get[String]("name").toOption.contains(name)).getOrElse(fail(s"Missing persistence entity: $name"))

  private def _entities(model: Json): Vector[Json] =
    model.hcursor.get[Vector[Json]]("entities").fold(error => fail(error.message), identity)

  private def _string_vector(model: Json, field: String): Vector[String] =
    model.hcursor.get[Vector[String]](field).fold(error => fail(error.message), identity)
}
