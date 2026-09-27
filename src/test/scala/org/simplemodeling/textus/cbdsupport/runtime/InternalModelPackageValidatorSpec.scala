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
 * @since   Sep. 27, 2026
 * @version Sep. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelPackageValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _project_yaml = """project:
    |  namespace: org.example
    |  id: sample
    |""".stripMargin
  private val _package_id = "01234567-89ab-cdef-0123-456789abcdef"

  "Internal-model package structural validation" should {
    "project identity, canonical manifest bytes, and closed root schema" which {
      "accept one independently canonical, project-bound package with ordered artifacts" in {
        Given("a consuming project whose JSON manifest and raw artifact bytes are independently canonical")
        val alphabytes = "alpha\n".getBytes(StandardCharsets.UTF_8)
        val betabytes = "beta\n".getBytes(StandardCharsets.UTF_8)
        val artifacts = Vector(
          _artifact("alpha", "records/alpha.txt", required = true, alphabytes),
          _artifact("beta", "records/beta.txt", required = true, betabytes, Vector("alpha"))
        )

        _with_fixture(_manifest(artifacts), Map("records/beta.txt" -> betabytes, "records/alpha.txt" -> alphabytes)) { root =>
          When("the project-bound package structure is validated")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the valid closed inventory is accepted without inferring any semantic approval")
          result.isSuccess shouldBe true
        }
      }

      "reject missing or malformed project identity and manifest roots" in {
        Given("otherwise valid package bytes with an absent source identity or absent manifest")
        val bytes = "record\n".getBytes(StandardCharsets.UTF_8)
        val manifest = _manifest(Vector(_artifact("record", "record.txt", required = true, bytes)))

        _with_fixture(manifest, Map("record.txt" -> bytes), "project:\n  namespace: org.example\n") { root =>
          When("the source identity lacks project.id")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("project-bound structural validation rejects it")
          result.isSuccess shouldBe false
        }
        _with_fixture(manifest, Map("record.txt" -> bytes)) { root =>
          Given("a consuming project from which manifest.yaml is removed")
          Files.delete(root.resolve("src/main/internal-model/manifest.yaml"))
          When("the package root is validated")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the missing regular manifest is rejected")
          result.isSuccess shouldBe false
        }
      }

      "reject byte forms that are not exact canonical UTF-8 JSON" in {
        Given("a valid manifest fixture and byte-level malformed alternatives")
        val bytes = "record\n".getBytes(StandardCharsets.UTF_8)
        val canonical = _manifest(Vector(_artifact("record", "record.txt", required = true, bytes)))
        val variants = Vector(
          (Array(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical.getBytes(StandardCharsets.UTF_8)),
          Array[Byte](0xc3.toByte, 0x28.toByte),
          "{not-json}\n".getBytes(StandardCharsets.UTF_8),
          (canonical.dropRight(1) + " \n").getBytes(StandardCharsets.UTF_8),
          (
            "{" + "\"artifacts\":[],\"artifacts\":[],\"lifecycleState\":\"draft\",\"packageDigest\":\"sha256:" + ("0" * 64) + "\",\"packageId\":\"" + _package_id + "\",\"projectId\":\"sample\",\"projectNamespace\":\"org.example\",\"revision\":1,\"schemaVersion\":\"1.0\"}\n"
          ).getBytes(StandardCharsets.UTF_8)
        )

        forAll(Gen.oneOf(variants)) { manifestbytes =>
          _with_fixture_bytes(manifestbytes, Map("record.txt" -> bytes)) { root =>
            When("a BOM, invalid encoding, invalid JSON, noncanonical whitespace, or duplicate member is supplied")
            val result = InternalModelPackageValidator.validateStructure(root)

            Then("the validator rejects bytes rather than normalizing them")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject closed-schema, token, UUID, revision, and project-identity violations" in {
        Given("canonical manifests whose structural envelope is individually invalid")
        val bytes = "record\n".getBytes(StandardCharsets.UTF_8)
        val artifact = _artifact("record", "record.txt", required = true, bytes)
        val invalid = Vector(
          _manifest(Vector(artifact), schema = "2.0"),
          _manifest(Vector(artifact), packageid = "not-a-uuid"),
          _manifest(Vector(artifact), namespace = "not valid"),
          _manifest(Vector(artifact), revision = Json.fromInt(0)),
          _manifest(Vector(artifact), namespace = "org.other"),
          _manifest_with_extra_root_field(Vector(artifact))
        )

        forAll(Gen.oneOf(invalid)) { manifest =>
          _with_fixture(manifest, Map("record.txt" -> bytes)) { root =>
            When("a schema, identity token, UUID, revision, project match, or root-field requirement is violated")
            val result = InternalModelPackageValidator.validateStructure(root)

            Then("the V1 root schema fails closed")
            result.isSuccess shouldBe false
          }
        }
      }

    }
    "artifact identity, dependency graph, and deterministic topological order" which {
      "reject duplicate inventory identities, unsafe paths, and unsupported artifact shapes" in {
        Given("canonical manifests with closed-artifact schema violations")
        val bytes = "record\n".getBytes(StandardCharsets.UTF_8)
        val record = _artifact("record", "record.txt", required = true, bytes)
        val duplicateid = _artifact("record", "other.txt", required = true, bytes)
        val duplicatepath = _artifact("other", "record.txt", required = true, bytes)
        val unsafepath = _artifact("escape", "../escape.txt", required = false, bytes)
        val badrole = _artifact("other", "other.txt", required = true, bytes, role = "unknown")
        val invalid = Vector(
          _manifest(Vector(record, duplicateid)),
          _manifest(Vector(record, duplicatepath)),
          _manifest(Vector(unsafepath)),
          _manifest(Vector(badrole))
        )

        forAll(Gen.oneOf(invalid)) { manifest =>
          _with_fixture(manifest, Map("record.txt" -> bytes, "other.txt" -> bytes)) { root =>
            When("inventory IDs, paths, path syntax, or roles break the closed V1 rules")
            val result = InternalModelPackageValidator.validateStructure(root)

            Then("the inventory is rejected without repair or inferred replacement data")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject unresolved, self-referential, cyclic, duplicate, and noncanonical dependencies" in {
        Given("valid raw files with independently canonical dependency graph mutations")
        val alphabytes = "alpha\n".getBytes(StandardCharsets.UTF_8)
        val betabytes = "beta\n".getBytes(StandardCharsets.UTF_8)
        val alpha = _artifact("alpha", "alpha.txt", required = true, alphabytes)
        val beta = _artifact("beta", "beta.txt", required = true, betabytes, Vector("alpha"))
        val unresolved = _artifact("beta", "beta.txt", required = true, betabytes, Vector("missing"))
        val self = _artifact("alpha", "alpha.txt", required = true, alphabytes, Vector("alpha"))
        val cyclealpha = _artifact("alpha", "alpha.txt", required = true, alphabytes, Vector("beta"))
        val cyclebeta = _artifact("beta", "beta.txt", required = true, betabytes, Vector("alpha"))
        val duplicatedeps = _artifact("beta", "beta.txt", required = true, betabytes, Vector("alpha", "alpha"))
        val invalid = Vector(
          _manifest(Vector(alpha, unresolved)),
          _manifest(Vector(self)),
          _manifest(Vector(cyclealpha, cyclebeta)),
          _manifest(Vector(alpha, duplicatedeps)),
          _manifest(Vector(beta, alpha))
        )

        forAll(Gen.oneOf(invalid)) { manifest =>
          _with_fixture(manifest, Map("alpha.txt" -> alphabytes, "beta.txt" -> betabytes)) { root =>
            When("dependency resolution, uniqueness, acyclicity, or deterministic order is violated")
            val result = InternalModelPackageValidator.validateStructure(root)

            Then("the validator rejects the graph instead of choosing a dependency order")
            result.isSuccess shouldBe false
          }
        }
      }

      "accept only the canonical topological permutation independent of creation order" in {
        val alphabytes = "alpha\n".getBytes(StandardCharsets.UTF_8)
        val betabytes = "beta\n".getBytes(StandardCharsets.UTF_8)
        val alpha = _artifact("alpha", "z/alpha.txt", required = true, alphabytes)
        val beta = _artifact("beta", "a/beta.txt", required = true, betabytes, Vector("alpha"))
        val permutations = Gen.oneOf(Vector(alpha, beta), Vector(beta, alpha))

        forAll(permutations) { order =>
          Given("a filesystem whose artifact directories are created in a different order from its manifest")
          _with_fixture(_manifest(order), Map("a/beta.txt" -> betabytes, "z/alpha.txt" -> alphabytes)) { root =>
            When("a topological artifact permutation is validated")
            val result = InternalModelPackageValidator.validateStructure(root)

            Then("only the ascending deterministic topological order is accepted")
            result.isSuccess shouldBe (order == Vector(alpha, beta))
          }
        }
      }

    }
    "filesystem closure, optional presence, and raw/package digests" which {
      "reject path escapes, symbolic links, unlisted files, and required absence" in {
        Given("a valid package fixture with filesystem boundary mutations")
        val bytes = "record\n".getBytes(StandardCharsets.UTF_8)
        val artifact = _artifact("record", "records/record.txt", required = true, bytes)
        val manifest = _manifest(Vector(artifact))

        _with_fixture(manifest, Map("records/record.txt" -> bytes)) { root =>
          Given("an extra regular file below the package root")
          _write(root.resolve("src/main/internal-model/unlisted.txt"), "unlisted\n".getBytes(StandardCharsets.UTF_8))
          When("the closed filesystem inventory is validated")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the unlisted file is rejected")
          result.isSuccess shouldBe false
        }
        _with_fixture(manifest, Map("records/record.txt" -> bytes)) { root =>
          Given("an unlisted empty directory below the package root")
          Files.createDirectory(root.resolve("src/main/internal-model/empty"))
          When("the closed filesystem inventory is validated")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the directory that contains no listed regular artifact is rejected")
          result.isSuccess shouldBe false
        }
        _with_fixture(manifest, Map("records/record.txt" -> bytes)) { root =>
          Given("the required artifact is absent")
          Files.delete(root.resolve("src/main/internal-model/records/record.txt"))
          When("the closed filesystem inventory is validated")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("required absence is rejected")
          result.isSuccess shouldBe false
        }
        _with_fixture(manifest, Map("records/record.txt" -> bytes)) { root =>
          Given("a symbolic link below the package root")
          Files.createSymbolicLink(root.resolve("src/main/internal-model/records/link.txt"), Path.of("record.txt"))
          When("the package walk observes the symbolic link")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the package is rejected without following the link")
          result.isSuccess shouldBe false
        }
      }

      "accept an absent optional artifact that has no present dependent" in {
        Given("a canonical manifest that explicitly declares one optional artifact")
        val bytes = "optional\n".getBytes(StandardCharsets.UTF_8)
        val optional = _artifact("optional", "optional.txt", required = false, bytes)

        _with_fixture(_manifest(Vector(optional)), Map.empty) { root =>
          When("the optional artifact is absent and no present artifact depends on it")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the closed inventory accepts the explicit optional absence")
          result.isSuccess shouldBe true
        }
      }

      "return only verified source-snapshot entries in manifest order with exact present bytes and explicit optional absence" in {
        Given("a closed manifest containing present source snapshots, one non-source artifact, and one absent optional source snapshot")
        val alphabytes = "snapshot alpha\n".getBytes(StandardCharsets.UTF_8)
        val decisionbytes = "decision\n".getBytes(StandardCharsets.UTF_8)
        val optionalbytes = "snapshot optional\n".getBytes(StandardCharsets.UTF_8)
        val alpha = _artifact("snapshot-alpha", "snapshots/alpha.json", required = true, alphabytes, role = "source-snapshot")
        val decision = _artifact("decision", "decision.json", required = true, decisionbytes, Vector("snapshot-alpha"))
        val optional = _artifact("snapshot-optional", "snapshots/optional.json", required = false, optionalbytes, role = "source-snapshot")

        _with_fixture(_manifest(Vector(alpha, decision, optional)), Map("snapshots/alpha.json" -> alphabytes, "decision.json" -> decisionbytes)) { root =>
          When("the existing project-bound validation pass exposes its verified source-snapshot inventory")
          val result = InternalModelPackageValidator.verifiedSourceSnapshots(root)

          Then("manifest order, role filtering, exact immutable bytes, and optional absence are retained without weakening validation")
          result.isSuccess shouldBe true
          result.toOption.map(_.map(entry => (entry.artifactId, entry.packageRelativePath, entry.required, entry.bytes))) shouldBe Some(Vector(
            ("snapshot-alpha", "snapshots/alpha.json", true, Some(alphabytes.toVector)),
            ("snapshot-optional", "snapshots/optional.json", false, None)
          ))
        }
      }

      "reject optional-presence dependency, raw artifact digest, and package-digest mismatches" in {
        Given("canonical manifests whose integrity claims do not match their package contents")
        val alphabytes = "alpha\n".getBytes(StandardCharsets.UTF_8)
        val betabytes = "beta\n".getBytes(StandardCharsets.UTF_8)
        val optionalalpha = _artifact("alpha", "alpha.txt", required = false, alphabytes)
        val beta = _artifact("beta", "beta.txt", required = true, betabytes, Vector("alpha"))
        val wrongdigest = _artifact("beta", "beta.txt", required = true, "other\n".getBytes(StandardCharsets.UTF_8))

        _with_fixture(_manifest(Vector(optionalalpha, beta)), Map("beta.txt" -> betabytes)) { root =>
          When("a present artifact depends on an absent optional artifact")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the dependency is rejected")
          result.isSuccess shouldBe false
        }
        _with_fixture(_manifest(Vector(wrongdigest)), Map("beta.txt" -> betabytes)) { root =>
          When("the raw artifact bytes differ from their declared digest")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the artifact digest mismatch is rejected")
          result.isSuccess shouldBe false
        }
        _with_fixture(_manifest_with_wrong_package_digest(Vector(beta)), Map("beta.txt" -> betabytes)) { root =>
          When("the package digest is changed without recomputing the canonical package object")
          val result = InternalModelPackageValidator.validateStructure(root)

          Then("the package digest mismatch is rejected")
          result.isSuccess shouldBe false
        }
      }
    }
  }

  private def _artifact(
    id: String,
    path: String,
    required: Boolean,
    bytes: Array[Byte],
    dependencies: Vector[String] = Vector.empty,
    role: String = "decision"
  ): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "dependsOn" -> Json.fromValues(dependencies.map(Json.fromString)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(required),
      "role" -> Json.fromString(role),
      "sha256" -> Json.fromString(_sha256(bytes))
    )

  private def _manifest(
    artifacts: Vector[Json],
    schema: String = "1.0",
    packageid: String = _package_id,
    namespace: String = "org.example",
    projectid: String = "sample",
    revision: Json = Json.fromInt(1)
  ): String =
    _canonical_manifest(_root(artifacts, schema, packageid, namespace, projectid, revision))

  private def _manifest_with_extra_root_field(artifacts: Vector[Json]): String =
    _canonical_manifest(_root(artifacts, "1.0", _package_id, "org.example", "sample", Json.fromInt(1)).add("unexpected", Json.fromString("value")))

  private def _manifest_with_wrong_package_digest(artifacts: Vector[Json]): String =
    _canonical(_root(artifacts, "1.0", _package_id, "org.example", "sample", Json.fromInt(1))
      .add("packageDigest", Json.fromString("sha256:" + ("f" * 64))).toJson).map(_.toChar).mkString

  private def _root(
    artifacts: Vector[Json],
    schema: String,
    packageid: String,
    namespace: String,
    projectid: String,
    revision: Json
  ): JsonObject =
    JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)),
      "packageId" -> Json.fromString(packageid),
      "projectId" -> Json.fromString(projectid),
      "projectNamespace" -> Json.fromString(namespace),
      "revision" -> revision,
      "schemaVersion" -> Json.fromString(schema)
    ))

  private def _canonical_manifest(root: JsonObject): String = {
    val withoutdigest = root.remove("packageDigest").toJson
    val digest = _sha256(_canonical(withoutdigest))
    _canonical(root.add("packageDigest", Json.fromString(digest)).toJson).map(_.toChar).mkString
  }

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString

  private def _with_fixture(
    manifest: String,
    files: Map[String, Array[Byte]],
    projectyaml: String = _project_yaml
  )(f: Path => Unit): Unit =
    _with_fixture_bytes(manifest.getBytes(StandardCharsets.UTF_8), files, projectyaml)(f)

  private def _with_fixture_bytes(
    manifest: Array[Byte],
    files: Map[String, Array[Byte]],
    projectyaml: String = _project_yaml
  )(f: Path => Unit): Unit = {
    val root = Files.createTempDirectory("internal-model-package-validator-")
    try {
      _write(root.resolve("project.yaml"), projectyaml.getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), manifest)
      files.foreach { case (path, bytes) => _write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      f(root)
    } finally _delete_tree(root)
  }

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root) then
      Files.walk(root).iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
}
