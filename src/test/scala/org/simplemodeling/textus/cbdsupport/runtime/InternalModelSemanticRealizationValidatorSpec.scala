package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest

import scala.jdk.CollectionConverters.*

import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelSemanticRealizationValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8)
  private val _source = Json.obj(
    "authority" -> Json.fromString("model-authority"),
    "identity" -> Json.fromString("model-source"),
    "locator" -> Json.fromString("catalog/model-source"),
    "revision" -> Json.fromString("revision-1"),
    "sha256" -> Json.fromString(_sha256(_model_raw))
  )

  "Internal-model semantic realization validation" should {
    "admit one exact package/snapshot/realization basis and return its canonical bytes" in {
      Given("a project-private package with one model-context snapshot, one realization, both assertion lanes, and an explicit limitation")
      val realization = _realization()

      _with_fixture(realization) { root =>
        When("the selected realization is admitted through the one structural inventory pass")
        val result = InternalModelSemanticRealizationValidator.validate(root)

        Then("the immutable candidate retains its exact scope, dependency set, separate assertion lanes, and byte-exact re-encoding")
        result.isSuccess shouldBe true
        result.toOption.map { candidate =>
          (
            candidate.scope,
            candidate.canonicalAssertions.map(_.assertionId),
            candidate.enrichmentAssertions.map(_.assertionId),
            candidate.consumedSnapshotArtifactIds,
            candidate.canonicalBytes,
            InternalModelSemanticRealizationValidator.encode(candidate)
          )
        } shouldBe Some((
          InternalModelSemanticScope("component-order", "context-order", "e-usecase"),
          Vector("a-customer", "a-relationship", "a-usecase"),
          Vector("z-enrichment"),
          Vector("snapshot-model"),
          realization.toVector,
          realization.toVector
        ))
      }
    }

    "ignore an unrelated source-snapshot whose content is not Phase 10.1-valid" in {
      Given("a structurally valid package with one selected model-context snapshot and one unrelated source-snapshot artifact")
      val realization = _realization()
      val unrelated = "{\"unrelated\":true}\n".getBytes(StandardCharsets.UTF_8)

      _with_fixture(realization, unrelatedsnapshot = Some(unrelated)) { root =>
        When("the realization admits only its manifest-declared source-snapshot dependency")
        val result = InternalModelSemanticRealizationValidator.validate(root)

        Then("the unrelated snapshot remains structurally inventory-checked but is not semantically parsed or allowed to reject this realization")
        result.isSuccess shouldBe true
      }
    }

    "fail closed for canonical bytes, profile, package dependency, and source-basis mutations" which {
      "reject duplicate/noncanonical bytes, unsupported profile, missing dependency, changed source metadata, and a CML-style assertion path" in {
        Given("otherwise complete candidate bytes with independently bounded V1 mutations")
        val canonical = _realization()
        val duplicate = Array('{'.toByte) ++ "\"canonicalAssertions\":[],".getBytes(StandardCharsets.UTF_8) ++ canonical.drop(1)
        val cases = Vector(
          duplicate -> Vector("snapshot-model"),
          (canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8)) -> Vector("snapshot-model"),
          _realization(profilevalue = "unsupported-profile") -> Vector("snapshot-model"),
          _realization(sourcehash = "sha256:" + ("0" * 64)) -> Vector("snapshot-model"),
          _realization(customeranchor = "model/customer.cml") -> Vector("snapshot-model"),
          canonical -> Vector.empty
        )

        forAll(Gen.oneOf(cases)) { case (realization, dependencies) =>
          _with_fixture(realization, dependencies) { root =>
            When("a byte, profile, source-envelope/anchor, or manifest dependency invariant is changed")
            val result = InternalModelSemanticRealizationValidator.validate(root)

            Then("admission rejects the supplied candidate without normalizing, inferring, or repairing it")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject missing or duplicated semantic identity, wrong scope/endpoints/role/direction, source-kind mismatch, lane collision, hidden limitation, and unknown successor link" in {
        Given("canonical candidates with a focused semantic or visibility invariant mutation")
        val cases = Vector(
          _realization(customeridentity = "e-customer-missing"),
          _realization(contextidentity = "other-context"),
          _realization(relationshiptarget = "e-missing"),
          _realization(rolevalue = ""),
          _realization(directionvalue = "diagonal"),
          _realization(customerreferencekind = "relationship"),
          _realization(enrichmentid = "a-customer"),
          _realization(includecondition = false),
          _realization(successor = Some(_successor("element", "e-customer", "element", "e-unknown", "ref-customer"))),
          _realization(
            successor = Some(_successor("element", "e-customer", "element", "e-usecase", "ref-successor")),
            successortargetless = true
          )
        )

        forAll(Gen.oneOf(cases)) { realization =>
          _with_fixture(realization) { root =>
            When("an exact identity, scope, endpoint, role/direction, source target, lane, condition, or successor invariant is invalid")
            val result = InternalModelSemanticRealizationValidator.validate(root)

            Then("the structural/source-basis boundary fails closed")
            result.isSuccess shouldBe false
          }
        }
      }
    }

    "preserve opaque semantic identity while allowing presentation variation" which {
      "accept generated label changes but reject generated identity and anchor changes" in {
        Given("generated nonempty presentation labels, semantic ID suffixes, and source-anchor suffixes")
        val generated = for {
          label <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
          identitysuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
          anchorsuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
        } yield (label, identitysuffix, anchorsuffix)

        forAll(generated) { case (label, identitysuffix, anchorsuffix) =>
          _with_fixture(_realization(customerlabel = label, usecaselabel = label.reverse)) { acceptedroot =>
            When("only explanatory labels vary under the same exact source identity")
            val accepted = InternalModelSemanticRealizationValidator.validate(acceptedroot)

            Then("presentation variation does not replace opaque semantic identity")
            accepted.isSuccess shouldBe true
          }
          _with_fixture(_realization(customeridentity = "e-customer-" + identitysuffix)) { identityroot =>
            When("a generated semantic identity no longer matches the source-owned snapshot witness")
            val rejected = InternalModelSemanticRealizationValidator.validate(identityroot)

            Then("the changed identity is rejected rather than reconstructed from its label")
            rejected.isSuccess shouldBe false
          }
          _with_fixture(_realization(customeranchor = "anchor-customer-" + anchorsuffix)) { anchorroot =>
            When("a generated source anchor is not source-owned by the selected snapshot")
            val rejected = InternalModelSemanticRealizationValidator.validate(anchorroot)

            Then("the changed anchor is rejected without path or hash inference")
            rejected.isSuccess shouldBe false
          }
        }
      }
    }
  }

  private def _realization(
    profilevalue: String = "ccdm-realization-v1",
    contextidentity: String = "context-order",
    customeridentity: String = "e-customer",
    customeranchor: String = "anchor-customer",
    relationshiptarget: String = "e-customer",
    rolevalue: String = "uses",
    directionvalue: String = "source-to-target",
    customerreferencekind: String = "element",
    enrichmentid: String = "z-enrichment",
    includecondition: Boolean = true,
    sourcehash: String = _sha256(_model_raw),
    customerlabel: String = "Customer",
    usecaselabel: String = "Place order",
    successor: Option[Json] = None,
    successortargetless: Boolean = false
  ): Array[Byte] = {
    val initialreferences = Vector(
      _reference("ref-customer", customerreferencekind, customeridentity, customeranchor, sourcehash),
      _reference("ref-relationship", "relationship", "r-uses", "anchor-relationship", sourcehash),
      _reference("ref-usecase", "element", "e-usecase", "anchor-usecase", sourcehash)
    )
    val successorreferences = if successortargetless then Vector(_targetless_reference("ref-successor", "anchor-customer", sourcehash)) else Vector.empty
    val references = (initialreferences ++ successorreferences).sortBy(_.noSpaces)
    val relationshipconditions = if includecondition then Vector("c-limitation") else Vector.empty
    val conditions = if includecondition then Vector(_condition("c-limitation", "limitation", "relationship", "r-uses", "ref-relationship", "source limitation")) else Vector.empty
    val canonical = Vector(
      _assertion("a-customer", "element", customeridentity, "ref-customer", "Customer is a party", Vector.empty),
      _assertion("a-relationship", "relationship", "r-uses", "ref-relationship", "Use case uses Customer", relationshipconditions),
      _assertion("a-usecase", "element", "e-usecase", "ref-usecase", "Place an order", Vector.empty)
    )
    val enrichment = Vector(_assertion(enrichmentid, "element", customeridentity, "ref-customer", "Customer is a party", Vector.empty))
    val elements = Vector(
      _element(customeridentity, "customer", customerlabel, Vector("a-customer"), Vector(enrichmentid), Vector.empty),
      _element("e-usecase", "use-case", usecaselabel, Vector("a-usecase"), Vector.empty, Vector.empty)
    ).sortBy(_.noSpaces)
    val relationships = Vector(_relationship("r-uses", "uses", "e-usecase", relationshiptarget, rolevalue, directionvalue, Vector("a-relationship"), Vector.empty, relationshipconditions))
    val links = successor.toVector
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(canonical.sortBy(_.noSpaces)),
      "conditions" -> Json.fromValues(conditions),
      "elements" -> Json.fromValues(elements),
      "enrichmentAssertions" -> Json.fromValues(enrichment),
      "profile" -> Json.fromString(profilevalue),
      "realizationIdentity" -> Json.fromString("realization-order"),
      "relationships" -> Json.fromValues(relationships),
      "schemaVersion" -> Json.fromString("1.0"),
      "scope" -> Json.obj(
        "componentIdentity" -> Json.fromString("component-order"),
        "projectionContextIdentity" -> Json.fromString(contextidentity),
        "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")
      ),
      "sourceReferences" -> Json.fromValues(references),
      "successorLinks" -> Json.fromValues(links),
      "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
    ))
  }

  private def _source_snapshot: Array[Byte] =
    _canonical(Json.obj(
      "basis" -> Json.obj(
        "contextIdentity" -> Json.fromString("model-context"),
        "facts" -> Json.fromValues(Vector(
          _fact("element", "e-customer", "anchor-customer", "Customer is a party", Vector.empty),
          _fact("element", "e-usecase", "anchor-usecase", "Place an order", Vector.empty),
          _fact("relationship", "r-uses", "anchor-relationship", "Use case uses Customer", Vector("source limitation"))
        ))
      ),
      "schemaVersion" -> Json.fromString("1.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> _source
    ))

  private def _fact(kindvalue: String, identity: String, anchor: String, content: String, limitations: Vector[String]): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-order"),
      "content" -> Json.fromString(content),
      "limitations" -> Json.fromValues(limitations.map(Json.fromString)),
      "projectionContextIdentity" -> Json.fromString("context-order"),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceAnchor" -> Json.fromString(anchor)
    )

  private def _reference(referenceid: String, kindvalue: String, identity: String, anchor: String, sourcehash: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotArtifactId" -> Json.fromString("snapshot-model"),
      "source" -> _source.mapObject(_.add("sha256", Json.fromString(sourcehash))),
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue))
    )

  private def _targetless_reference(referenceid: String, anchor: String, sourcehash: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotArtifactId" -> Json.fromString("snapshot-model"),
      "source" -> _source.mapObject(_.add("sha256", Json.fromString(sourcehash))),
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.Null
    )

  private def _assertion(assertionid: String, kindvalue: String, identity: String, referenceid: String, content: String, conditionids: Vector[String]): Json =
    Json.obj(
      "assertionId" -> Json.fromString(assertionid),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _condition(conditionid: String, kindvalue: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj(
      "affectedIdentity" -> Json.fromString(affectedidentity),
      "affectedKind" -> Json.fromString(affectedkind),
      "conditionId" -> Json.fromString(conditionid),
      "detail" -> Json.fromString(detail),
      "kind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _element(identity: String, kindvalue: String, label: String, canonicalids: Vector[String], enrichmentids: Vector[String], conditionids: Vector[String]): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)),
      "identity" -> Json.fromString(identity),
      "kind" -> Json.fromString(kindvalue),
      "label" -> Json.fromString(label)
    )

  private def _relationship(identity: String, label: String, sourceidentity: String, targetidentity: String, rolevalue: String, directionvalue: String, canonicalids: Vector[String], enrichmentids: Vector[String], conditionids: Vector[String]): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "direction" -> Json.fromString(directionvalue),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)),
      "identity" -> Json.fromString(identity),
      "label" -> Json.fromString(label),
      "role" -> Json.fromString(rolevalue),
      "sourceElementIdentity" -> Json.fromString(sourceidentity),
      "targetElementIdentity" -> Json.fromString(targetidentity)
    )

  private def _successor(priorkind: String, prioridentity: String, successorkind: String, successoridentity: String, referenceid: String): Json =
    Json.obj(
      "priorIdentity" -> Json.fromString(prioridentity),
      "priorKind" -> Json.fromString(priorkind),
      "sourceReferenceId" -> Json.fromString(referenceid),
      "successorIdentity" -> Json.fromString(successoridentity),
      "successorKind" -> Json.fromString(successorkind)
    )

  private def _with_fixture(
    realization: Array[Byte],
    dependencies: Vector[String] = Vector("snapshot-model"),
    unrelatedsnapshot: Option[Array[Byte]] = None
  )(f: Path => Unit): Unit = {
    val snapshot = _source_snapshot
    val sourceartifact = _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty)
    val realizationartifact = _artifact("realization-main", "realizations/main.json", "realization", realization, dependencies)
    val unrelatedartifact = unrelatedsnapshot.map(bytes => _artifact("snapshot-unrelated", "snapshots/unrelated.json", "source-snapshot", bytes, Vector.empty)).toVector
    val artifacts = if dependencies.isEmpty then Vector(realizationartifact, sourceartifact) ++ unrelatedartifact else Vector(sourceartifact, realizationartifact) ++ unrelatedartifact
    val root = Files.createTempDirectory("internal-model-semantic-realization-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: semantic-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      unrelatedsnapshot.foreach(bytes => _write(root.resolve("src/main/internal-model/snapshots/unrelated.json"), bytes))
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      f(root)
    } finally _delete_tree(root)
  }

  private def _artifact(id: String, path: String, rolevalue: String, bytes: Array[Byte], dependencies: Vector[String]): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "dependsOn" -> Json.fromValues(dependencies.map(Json.fromString)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(true),
      "role" -> Json.fromString(rolevalue),
      "sha256" -> Json.fromString(_sha256(bytes))
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("semantic-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(1),
      "schemaVersion" -> Json.fromString("1.0")
    ))
    val digest = _sha256(_canonical(root.remove("packageDigest").toJson))
    _canonical(root.add("packageDigest", Json.fromString(digest)).toJson)
  }

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root) then Files.walk(root).iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
}
