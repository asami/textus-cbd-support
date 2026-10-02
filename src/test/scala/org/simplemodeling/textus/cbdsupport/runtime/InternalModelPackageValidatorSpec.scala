package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}

import scala.jdk.CollectionConverters.*

import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 27, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelPackageValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _project_yaml = "project:\n  namespace: org.example\n  id: sample\n"
  private val _package_id = "01234567-89ab-cdef-0123-456789abcdef"
  private val _payload = Vector[Byte](0, 0xff.toByte, 10, 0xc3.toByte)

  "Internal-model V2 package admission" should {
    "explicit project identity, revisions and roles" which {
      "accept project-bound packages across the positive Long revision domain" in {
        Given("producer-supplied positive carrier and artifact revisions, including both Long boundaries")
        val boundaries = Vector(1L, Long.MaxValue)
        val revisions = Gen.choose(1L, Long.MaxValue)
        def _admit_revision_(revision: Long): Unit = {
          val alpha = _reference("alpha", revision, InternalModelArtifactRole.SourceSnapshot)
          val beta = _reference("beta", revision, InternalModelArtifactRole.Decision)
          val artifacts = Vector(_artifact(alpha, "alpha.bin"), _artifact(beta, "beta.bin", dependencies = Vector(alpha)))
          _with_fixture(_manifest(artifacts, Json.fromLong(revision)), _files(artifacts)) { root =>
            When("the actual V2 producer envelope is admitted")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("its declared positive revisions and exact dependency are structurally valid")
            result.isSuccess shouldBe true
          }
        }
        boundaries.foreach(_admit_revision_)
        forAll(revisions)(revision => _admit_revision_(revision))
      }

      "admit each of the eight closed roles without granting content readiness" in {
        Given("one explicit versioned artifact for each closed role, with arbitrary ordinary payload")
        val artifacts = InternalModelArtifactRole.values.toVector.zipWithIndex.map { case (role, index) =>
          _artifact(_reference(s"artifact-$index", 1L, role), s"records/$index.bin")
        }
        _with_fixture(_manifest(artifacts), _files(artifacts)) { root =>
          When("structural admission observes all role declarations")
          val structural = InternalModelPackageValidator.validateStructure(root)
          val continuity = InternalModelPackageValidator.verifiedProjectionContinuity(root)
          Then("all roles are valid metadata while undecoded content supplies no continuity readiness")
          structural.isSuccess shouldBe true
          continuity.isSuccess shouldBe false
        }
      }

      "reject missing, malformed or mismatching source project identity" in {
        Given("a V2 manifest whose identity must come from the consuming project's project.yaml")
        val sources = Vector(
          "project:\n  namespace: org.example\n",
          "project:\n  id: sample\n",
          "project:\n  namespace: 'not valid'\n  id: sample\n",
          "project:\n  namespace: org.other\n  id: sample\n",
          "project:\n  namespace: org.example\n  id: other\n",
          "project:\n  namespace: org.example\n  namespace: org.other\n  id: sample\n",
          "project: []\n"
        )
        sources.foreach { source =>
          _with_fixture(_manifest(Vector.empty), Map.empty, source) { root =>
            When("project-bound admission reads the actual source identity")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("absent, invalid or contradictory source identity fails without inference")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject invalid package, project, lifecycle and schema declarations" in {
        Given("individually invalid envelope identities and unsupported schema versions")
        val root = _root(Vector.empty)
        val mutations = Vector(
          root.add("packageId", Json.fromString("not-a-uuid")),
          root.add("packageId", Json.fromString(_package_id.toUpperCase)),
          root.add("projectNamespace", Json.fromString("org example")),
          root.add("projectId", Json.fromString("日本語")),
          root.add("lifecycleState", Json.fromString("")),
          root.add("schemaVersion", Json.fromString("1.0")),
          root.add("schemaVersion", Json.fromString("3.0"))
        )
        mutations.foreach { manifest =>
          _with_fixture(_json(manifest.toJson), Map.empty) { projectroot =>
            When("the declared envelope is interpreted")
            val result = InternalModelPackageValidator.validateStructure(projectroot)
            Then("the exact V2 identity and token contract fails closed")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject nonpositive, overflowing and noninteger revision spellings at every reference depth" in {
        Given("carrier, artifact and dependency revisions with separately substituted forbidden JSON spellings")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 2L, InternalModelArtifactRole.Decision)
        val marker = Json.fromString("REVISION")
        val artifact = _artifact(alpha, "alpha.bin").mapObject(_.add("artifactRevision", marker))
        val dependent = _artifact(beta, "beta.bin").mapObject(_.add("dependsOn",
          Json.arr(_reference_json(alpha).mapObject(_.add("artifactRevision", marker)))))
        val fixtures = Vector(
          _manifest(Vector.empty, marker) -> Map.empty[String, Vector[Byte]],
          _manifest(Vector(artifact)) -> _files(Vector(artifact)),
          _manifest(Vector(_artifact(alpha, "alpha.bin"), dependent)) -> _files(Vector(_artifact(alpha, "alpha.bin"), dependent))
        )
        val spellings = Vector("0", "-1", "9223372036854775808", "1.0", "1e0", "+1", "01", "\"1\"", "null", "true")
        fixtures.foreach { case (manifest, files) =>
          spellings.foreach { spelling =>
            _with_fixture(manifest.replace("\"REVISION\"", spelling), files) { root =>
              When("the explicit revision is supplied with a forbidden lexical form")
              val result = InternalModelPackageValidator.validateStructure(root)
              Then("no revision is inferred, normalized or truncated into admission")
              result.isSuccess shouldBe false
            }
          }
        }
      }
    }

    "strict JSON with presentation-independent decoded values" which {
      "accept equivalent key order, whitespace and JSON escaping" in {
        Given("a valid V2 envelope and semantically equivalent JSON presentations")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 2L, InternalModelArtifactRole.Decision)
        val artifacts = Vector(_artifact(alpha, "alpha.bin"), _artifact(beta, "beta.bin", dependencies = Vector(alpha)))
        val root = _root(artifacts)
        val reversed = Json.fromJsonObject(JsonObject.fromIterable(root.toIterable.toVector.reverse))
        val presentations = Vector(
          _json(root.toJson),
          Printer.spaces2.print(reversed),
          " \r\n\t" + reversed.noSpaces + " \n\t",
          _json(root.toJson).replace("org.example", "org.\\u0065xample").replace("alpha", "\\u0061lpha")
        )
        presentations.foreach { manifest =>
          _with_fixture(manifest, _files(artifacts)) { projectroot =>
            When("the actual package reader decodes the presentation")
            val result = InternalModelPackageValidator.validateStructure(projectroot)
            Then("the same decoded identities and exact references are admitted")
            result.isSuccess shouldBe true
          }
        }
      }

      "reject encoding failures, non-JSON input and duplicate members at every depth" in {
        Given("valid V2 text with root, artifact and dependency duplicate-member mutations")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 1L, InternalModelArtifactRole.Decision)
        val artifacts = Vector(_artifact(alpha, "alpha.bin"), _artifact(beta, "beta.bin", dependencies = Vector(alpha)))
        val valid = _manifest(artifacts)
        val texts = Vector(
          "schemaVersion: '2.0'\nartifacts: []\n",
          valid + "true",
          "[]",
          valid.replace("\"schemaVersion\":\"2.0\"", "\"schemaVersion\":\"2.0\",\"schemaVersion\":\"2.0\""),
          valid.replace("\"required\":true", "\"required\":true,\"required\":true"),
          valid.replace("\"dependsOn\":[{\"artifactId\":\"alpha\"", "\"dependsOn\":[{\"artifactId\":\"alpha\",\"artifactId\":\"alpha\"")
        )
        val variants = texts.map(_bytes) ++ Vector(
          Vector(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ _bytes(valid),
          Vector(0xc3.toByte, 0x28.toByte)
        )
        variants.foreach { manifest =>
          _with_fixture_bytes(manifest, _files(artifacts)) { root =>
            When("strict UTF-8 JSON admission observes malformed input")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("the malformed serialized structure rejects without repair")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject missing, extra and wrongly typed fields in every closed object shape" in {
        Given("closed V2 root, artifact and dependency objects with one field removed, added or wrongly typed")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 1L, InternalModelArtifactRole.Decision)
        val root = _root(Vector.empty)
        val artifact = _artifact(alpha, "alpha.bin").asObject.get
        val dependency = _reference_json(alpha).asObject.get
        val roots = _shape_mutations(root) :+ root.add("packageDigest", Json.Null)
        val artifacts = _shape_mutations(artifact) :+ artifact.add("sha256", Json.Null)
        val dependencies = _shape_mutations(dependency) :+ dependency.add("sha256", Json.Null)
        val fixtures = roots.map(value => _json(value.toJson) -> Map.empty[String, Vector[Byte]]) ++
          artifacts.map(value => _manifest(Vector(value.toJson)) -> Map("alpha.bin" -> _payload)) ++
          dependencies.map { value =>
            val inventory = Vector(_artifact(alpha, "alpha.bin"),
              _artifact(beta, "beta.bin").mapObject(_.add("dependsOn", Json.arr(value.toJson))))
            _manifest(inventory) -> _files(inventory)
          } ++ Vector(
            _manifest(Vector(Json.Null)) -> Map.empty[String, Vector[Byte]],
            _manifest(Vector(_artifact(alpha, "alpha.bin").mapObject(_.add("dependsOn", Json.arr(Json.fromString("alpha")))))) -> Map("alpha.bin" -> _payload)
          )
        fixtures.foreach { case (manifest, files) =>
          _with_fixture(manifest, files) { projectroot =>
            When("an exact closed shape or field type is violated")
            val result = InternalModelPackageValidator.validateStructure(projectroot)
            Then("root, inventory and dependency objects reject legacy or incomplete structure")
            result.isSuccess shouldBe false
          }
        }
      }
    }

    "unique metadata and exact dependency graph" which {
      "reject duplicate IDs or paths, malformed artifact identities and unsupported roles" in {
        Given("otherwise valid versioned artifacts with individually invalid inventory metadata")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 1L, InternalModelArtifactRole.Decision)
        val first = _artifact(alpha, "alpha.bin")
        val second = _artifact(beta, "beta.bin")
        val inventories = Vector(
          Vector(first, _artifact(alpha, "beta.bin")),
          Vector(first, _artifact(beta, "alpha.bin")),
          Vector(first.mapObject(_.add("artifactId", Json.fromString("bad id")))),
          Vector(second.mapObject(_.add("role", Json.fromString("unknown"))))
        )
        inventories.foreach { inventory =>
          _with_fixture(_manifest(inventory), _files(inventory)) { root =>
            When("the declared inventory is admitted")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("unique identities, unique paths and closed roles are mandatory")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject dependency mismatches in ID, revision or role" in {
        Given("a source and dependency references that each contradict one exact reference facet")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 1L, InternalModelArtifactRole.Decision)
        val mismatches = Vector(
          _reference("missing", 1L, InternalModelArtifactRole.SourceSnapshot),
          _reference("alpha", 2L, InternalModelArtifactRole.SourceSnapshot),
          _reference("alpha", 1L, InternalModelArtifactRole.Decision)
        )
        mismatches.foreach { dependency =>
          val artifacts = Vector(_artifact(alpha, "alpha.bin"), _artifact(beta, "beta.bin", dependencies = Vector(dependency)))
          _with_fixture(_manifest(artifacts), _files(artifacts)) { root =>
            When("the declared dependency is resolved against the inventory")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("ID lookup cannot admit a missing or mismatching exact version and role")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject self-reference, cycles, duplicate dependency IDs and unsorted arrays" in {
        Given("explicit references for graph structure violations")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 1L, InternalModelArtifactRole.Decision)
        val gamma = _reference("gamma", 1L, InternalModelArtifactRole.Validation)
        val inventories = Vector(
          Vector(_artifact(alpha, "alpha.bin", dependencies = Vector(alpha))),
          Vector(_artifact(alpha, "alpha.bin", dependencies = Vector(beta)), _artifact(beta, "beta.bin", dependencies = Vector(alpha))),
          Vector(_artifact(alpha, "alpha.bin"), _artifact(beta, "beta.bin", dependencies = Vector(alpha, alpha))),
          Vector(_artifact(alpha, "alpha.bin"), _artifact(beta, "beta.bin", dependencies = Vector(alpha, _reference("alpha", 2L, InternalModelArtifactRole.SourceSnapshot)))),
          Vector(_artifact(alpha, "alpha.bin"), _artifact(beta, "beta.bin"), _artifact(gamma, "gamma.bin", dependencies = Vector(beta, alpha)))
        )
        inventories.foreach { inventory =>
          _with_fixture(_manifest(inventory), _files(inventory)) { root =>
            When("the graph's declared dependencies are admitted")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("the graph rejects without breaking cycles or reordering supplied arrays")
            result.isSuccess shouldBe false
          }
        }
      }

      "admit only the deterministic topological permutation regardless of creation order" in {
        Given("two independent inputs and one consumer with sorted exact dependencies")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 1L, InternalModelArtifactRole.SourceSnapshot)
        val gamma = _reference("gamma", 1L, InternalModelArtifactRole.Realization)
        val expected = Vector(_artifact(alpha, "z/alpha.bin"), _artifact(beta, "a/beta.bin"), _artifact(gamma, "gamma.bin", dependencies = Vector(alpha, beta)))
        val permutations = expected.permutations.toVector
        def _admit_order_(order: Vector[Json]): Unit =
          _with_fixture(_manifest(order), _files(expected)) { root =>
            When("the supplied topological permutation is interpreted")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("only the dependency-first ascending-ID order is admitted")
            result.isSuccess shouldBe (order == expected)
          }
        permutations.foreach(_admit_order_)
        forAll(Gen.oneOf(permutations))(order => _admit_order_(order))
      }

      "reject a present consumer of an absent optional dependency" in {
        Given("an absent optional source and a present decision depending on its exact reference")
        val alpha = _reference("alpha", 1L, InternalModelArtifactRole.SourceSnapshot)
        val beta = _reference("beta", 1L, InternalModelArtifactRole.Decision)
        val artifacts = Vector(_artifact(alpha, "alpha.bin", required = false), _artifact(beta, "beta.bin", dependencies = Vector(alpha)))
        _with_fixture(_manifest(artifacts), Map("beta.bin" -> _payload)) { root =>
          When("the captured graph checks actual dependency presence")
          val result = InternalModelPackageValidator.validateStructure(root)
          Then("the absent optional source cannot satisfy the present consumer")
          result.isSuccess shouldBe false
        }
      }
    }

    "safe filesystem closure and ordinary payload" which {
      "reject unsafe path spellings and the reserved manifest path" in {
        Given("optional paths that violate the package-relative POSIX grammar")
        val reference = _reference("optional", 1L, InternalModelArtifactRole.SourceSnapshot)
        val paths = Vector("", "/escape", "escape/", "a//b", ".", "..", "a/../b", "a/./b", "a\\b", "a:b", "a b", "a\nb", "日本語", "manifest.yaml")
        paths.foreach { path =>
          _with_fixture(_manifest(Vector(_artifact(reference, path, required = false))), Map.empty) { root =>
            When("the unsafe declared path is interpreted even though no payload is present")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("path syntax cannot be bypassed by optional absence")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject missing required manifests, source identity files and artifact files" in {
        Given("a valid project with one required ordinary artifact")
        val reference = _reference("record", 1L, InternalModelArtifactRole.SourceSnapshot)
        val paths = Vector("project.yaml", "src/main/internal-model/manifest.yaml", "src/main/internal-model/record.bin")
        paths.foreach { path =>
          _with_fixture(_manifest(Vector(_artifact(reference, "record.bin"))), Map("record.bin" -> _payload)) { root =>
            Files.delete(root.resolve(path))
            When("project-bound admission observes a missing required regular file")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("neither manifest, source identity nor required artifact absence is admitted")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject unlisted regular files and symbolic file or directory entries" in {
        Given("a valid closed package with independently introduced filesystem boundary violations")
        val reference = _reference("record", 1L, InternalModelArtifactRole.SourceSnapshot)
        val mutations: Vector[Path => Unit] = Vector(
          root => _write(root.resolve("src/main/internal-model/unlisted.bin"), _payload),
          root => { Files.createSymbolicLink(root.resolve("src/main/internal-model/link.bin"), Path.of("record.bin")); () },
          root => { Files.createSymbolicLink(root.resolve("src/main/internal-model/link-dir"), root); () },
          root => {
            val path = root.resolve("src/main/internal-model/record.bin")
            Files.delete(path)
            Files.createSymbolicLink(path, root.resolve("project.yaml"))
            ()
          }
        )
        mutations.foreach { mutation =>
          _with_fixture(_manifest(Vector(_artifact(reference, "record.bin"))), Map("record.bin" -> _payload)) { root =>
            mutation(root)
            When("the package inventory is walked without following symbolic entries")
            val result = InternalModelPackageValidator.validateStructure(root)
            Then("unlisted payload and symbolic objects fail closed")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject symbolic project and package roots and nonregular manifest or artifact paths" in {
        Given("valid roots with symbolic or directory objects substituted at regular-file boundaries")
        val reference = _reference("optional", 1L, InternalModelArtifactRole.SourceSnapshot)
        _with_fixture(_manifest(Vector(_artifact(reference, "optional.bin", required = false))), Map.empty) { root =>
          Files.createDirectory(root.resolve("src/main/internal-model/optional.bin"))
          When("an optional artifact names an existing directory")
          val result = InternalModelPackageValidator.validateStructure(root)
          Then("an existing nonregular path is not silently declared absent")
          result.isSuccess shouldBe false
        }
        _with_fixture(_manifest(Vector.empty), Map.empty) { root =>
          Given("a directory substituted for manifest.yaml")
          val manifest = root.resolve("src/main/internal-model/manifest.yaml")
          Files.delete(manifest)
          Files.createDirectory(manifest)
          When("manifest.yaml is a directory instead of a regular file")
          val result = InternalModelPackageValidator.validateStructure(root)
          Then("the nonregular manifest is rejected")
          result.isSuccess shouldBe false
        }
        _with_fixture(_manifest(Vector.empty), Map.empty) { root =>
          Given("a symbolic alias for the consuming project root")
          val symbolic = root.resolve("project-link")
          Files.createSymbolicLink(symbolic, root)
          When("the supplied project root itself is symbolic")
          val result = InternalModelPackageValidator.validateStructure(symbolic)
          Then("the symbolic project root is rejected")
          result.isSuccess shouldBe false
        }
        _with_fixture(_manifest(Vector.empty), Map.empty) { root =>
          Given("a symbolic package root pointing at an ordinary stored directory")
          val packagepath = root.resolve("src/main/internal-model")
          val moved = root.resolve("stored-package")
          Files.move(packagepath, moved)
          Files.createSymbolicLink(packagepath, moved)
          When("the package root itself is symbolic")
          val result = InternalModelPackageValidator.validateStructure(root)
          Then("the symbolic package root is rejected")
          result.isSuccess shouldBe false
        }
      }

      "accept ordinary empty directories and explicit optional absence" in {
        Given("an absent optional artifact and ordinary containing directories, including empty directories")
        val reference = _reference("optional", 1L, InternalModelArtifactRole.SourceSnapshot)
        _with_fixture(_manifest(Vector(_artifact(reference, "snapshots/optional.bin", required = false))), Map.empty) { root =>
          Files.createDirectories(root.resolve("src/main/internal-model/empty/nested"))
          Files.createDirectories(root.resolve("src/main/internal-model/snapshots"))
          When("structural admission inventories regular files only")
          val structural = InternalModelPackageValidator.validateStructure(root)
          val sources = InternalModelPackageValidator.verifiedSourceSnapshots(root)
          Then("ordinary directories are not artifacts and optional absence remains explicit")
          structural.isSuccess shouldBe true
          sources.toOption shouldBe Some(Vector(InternalModelVerifiedSourceSnapshot(reference, "snapshots/optional.bin", false, Vector.empty, None)))
        }
      }

      "retain typed source references, dependencies and exact ordinary payload in inventory order" in {
        Given("two source snapshots, one dependent non-source artifact and one absent optional source")
        val first = _reference("a-source", 4L, InternalModelArtifactRole.SourceSnapshot)
        val second = _reference("b-source", 7L, InternalModelArtifactRole.SourceSnapshot)
        val decision = _reference("c-decision", 9L, InternalModelArtifactRole.Decision)
        val optional = _reference("d-optional", 2L, InternalModelArtifactRole.SourceSnapshot)
        val artifacts = Vector(
          _artifact(first, "snapshots/first.bin"),
          _artifact(second, "snapshots/second.bin", dependencies = Vector(first)),
          _artifact(decision, "decision.bin", dependencies = Vector(second)),
          _artifact(optional, "snapshots/optional.bin", required = false)
        )
        val secondbytes = _bytes("ordinary source payload\n")
        _with_fixture(_manifest(artifacts), Map("snapshots/first.bin" -> _payload, "snapshots/second.bin" -> secondbytes, "decision.bin" -> _payload)) { root =>
          When("the project-bound capture selects its source-snapshot role")
          val result = InternalModelPackageValidator.verifiedSourceSnapshots(root)
          Then("typed identity, path, requiredness, exact dependencies, payload and absence are preserved")
          result.toOption shouldBe Some(Vector(
            InternalModelVerifiedSourceSnapshot(first, "snapshots/first.bin", true, Vector.empty, Some(_payload)),
            InternalModelVerifiedSourceSnapshot(second, "snapshots/second.bin", true, Vector(first), Some(secondbytes)),
            InternalModelVerifiedSourceSnapshot(optional, "snapshots/optional.bin", false, Vector.empty, None)
          ))
        }
      }

      "admit changed ordinary payload without claiming checksum freshness or authenticity" in {
        Given("an unchanged explicit reference whose ordinary source payload is changed by its producer")
        val reference = _reference("source", 1L, InternalModelArtifactRole.SourceSnapshot)
        val changed = _bytes("changed ordinary content\n")
        _with_fixture(_manifest(Vector(_artifact(reference, "source.bin"))), Map("source.bin" -> _payload)) { root =>
          _write(root.resolve("src/main/internal-model/source.bin"), changed)
          When("structural capture reads current ordinary payload under the declared version")
          val structural = InternalModelPackageValidator.validateStructure(root)
          val sources = InternalModelPackageValidator.verifiedSourceSnapshots(root)
          Then("admission retains payload but proves no undeclared-content freshness or semantic approval")
          structural.isSuccess shouldBe true
          sources.toOption.flatMap(_.headOption).map(entry => entry.reference -> entry.bytes) shouldBe Some(reference -> Some(changed))
        }
      }
    }

    "pure captured metadata and same-capture selection" which {
      "validate typed V2 context without path access or payload control" in {
        Given("a complete typed context with required presence and an optional absent entry")
        val context = _context
        When("pure captured-context admission checks only declared metadata")
        val result = InternalModelPackageValidator.validateCapturedContext(context)
        Then("the V2 context is admitted without filesystem or manifest content")
        result shouldBe Right(())
      }

      "reject inconsistent captured schema, revision, metadata, graph, order and presence" in {
        Given("a valid typed context with one declared invariant contradicted at a time")
        val context = _context
        val first = context.artifacts.head
        val second = context.artifacts(1)
        val optional = context.artifacts.last
        val invalid = Vector(
          context.copy(schemaversion = "1.0"),
          context.copy(revision = 0L),
          context.copy(lifecyclestate = "bad state"),
          context.copy(artifacts = context.artifacts :+ first.copy(path = "other.bin")),
          context.copy(artifacts = context.artifacts.updated(1, second.copy(path = first.path))),
          context.copy(artifacts = context.artifacts.updated(0, first.copy(path = "../escape"))),
          context.copy(artifacts = context.artifacts.updated(0, first.copy(path = "manifest.yaml"))),
          context.copy(artifacts = context.artifacts.updated(1, second.copy(dependencies = Vector(_reference("a-source", 8L, InternalModelArtifactRole.SourceSnapshot))))),
          context.copy(artifacts = context.artifacts.updated(1, second.copy(dependencies = Vector(first.reference, first.reference)))),
          context.copy(artifacts = context.artifacts.updated(0, first.copy(dependencies = Vector(first.reference)))),
          context.copy(artifacts = context.artifacts.updated(0, first.copy(dependencies = Vector(second.reference)))),
          context.copy(artifacts = context.artifacts.reverse),
          context.copy(artifacts = context.artifacts.updated(0, first.copy(present = false))),
          context.copy(artifacts = context.artifacts.updated(1, second.copy(dependencies = Vector(optional.reference))))
        )
        invalid.foreach { value =>
          When("pure context admission interprets inconsistent metadata")
          val result = InternalModelPackageValidator.validateCapturedContext(value)
          Then("the contradiction rejects without replacements or payload consultation")
          result.isLeft shouldBe true
        }
      }

      "preserve every captured inventory entry and payload while selecting new-profile continuity" in {
        Given("a V2 inventory with source, realization, new-profile continuity and an absent optional entry")
        val source = _reference("a-source", 4L, InternalModelArtifactRole.SourceSnapshot)
        val realization = _reference("b-realization", 6L, InternalModelArtifactRole.Realization)
        val projection = _reference("c-continuity", 9L, InternalModelArtifactRole.Projection)
        val optional = _reference("d-optional", 3L, InternalModelArtifactRole.OpenIssue)
        val projectionbytes = _bytes(" { \"schemaVersion\" : \"3.0\", \"profile\" : \"ccdm-projection-binding-v3\" } \n")
        val artifacts = Vector(
          _artifact(source, "source.bin"),
          _artifact(realization, "realization.bin", dependencies = Vector(source)),
          _artifact(projection, "continuity.json", dependencies = Vector(realization)),
          _artifact(optional, "optional.bin", required = false)
        )
        _with_fixture(_manifest(artifacts, Json.fromLong(11L)), Map("source.bin" -> _payload, "realization.bin" -> _payload, "continuity.json" -> projectionbytes)) { root =>
          When("one actual package capture produces the continuation handoff and its pure selection")
          val result = InternalModelPackageValidator.verifiedContinuation(root)
          val captured = result.toOption
          val contextresult = captured.map(value => InternalModelPackageValidator.validateCapturedContext(value.packageContext))
          val selected = captured.map(value => InternalModelPackageValidator.selectCapturedContinuity(value.artifacts))
          Then("typed metadata, references, ordinary payload and explicit absence share that capture")
          result.isSuccess shouldBe true
          val value = captured.get
          value.packageContext.reference shouldBe _package_reference
          value.packageContext.revision shouldBe 11L
          value.packageContext.schemaversion shouldBe "2.0"
          value.artifacts.map(_.context) shouldBe value.packageContext.artifacts
          value.artifacts.map(entry => entry.context.reference -> entry.bytes) shouldBe Vector(
            source -> Some(_payload), realization -> Some(_payload), projection -> Some(projectionbytes), optional -> None
          )
          value.continuityPackage.realizationpackage.realization.reference shouldBe realization
          value.continuityPackage.projection.reference shouldBe projection
          value.continuityPackage.projection.dependencies shouldBe Vector(realization)
          value.continuityPackage.projection.bytes shouldBe projectionbytes
          contextresult shouldBe Some(Right(()))
          selected shouldBe Some(Right(value.continuityPackage))
        }
      }

      "reject a captured presence claim that contradicts payload availability" in {
        Given("supplied presence claims that contradict captured payload options")
        val first = _context.artifacts.head
        val inconsistent = Vector(
          Vector(InternalModelVerifiedContinuationArtifact(first, None)),
          Vector(InternalModelVerifiedContinuationArtifact(first.copy(required = false, present = false), Some(_payload)))
        )
        inconsistent.foreach { inventory =>
          When("pure continuity selection consumes the supplied capture")
          val result = InternalModelPackageValidator.selectCapturedContinuity(inventory)
          Then("selection fails without inventing bytes or silently changing presence")
          result.left.toOption shouldBe Some("captured artifact presence does not match supplied payload availability")
        }
      }

      "select exact typed review and approval references with only the new projection profiles" in {
        Given("new-profile continuity, candidate and diff payloads plus exact review and approval metadata")
        val fixtures = _selection_fixture
        val artifacts = fixtures._1
        val files = fixtures._2
        val review = _reference("f-review", 5L, InternalModelArtifactRole.Validation)
        val approval = _reference("g-approval", 7L, InternalModelArtifactRole.Approval)
        _with_fixture(_manifest(artifacts), files) { root =>
          When("the same-capture APIs select the explicitly supplied review and approval versions")
          val selectedreview = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, review)
          val selectedapproval = InternalModelPackageValidator.verifiedCandidateHumanApproval(root, approval, review)
          Then("exact references and review-to-approval dependencies remain available to later semantic admission")
          selectedreview.toOption.map(_.reviewArtifact.reference) shouldBe Some(review)
          selectedreview.toOption.map(_.semanticDiffPackage.semanticdiff.reference.artifactId.value) shouldBe Some("e-diff")
          selectedapproval.toOption.map(_.approvalArtifact.reference) shouldBe Some(approval)
          selectedapproval.toOption.map(_.approvalArtifact.dependencies) shouldBe Some(Vector(review))
        }
      }

      "reject ID, revision, role, presence or dependency mismatches in explicit review and approval selections" in {
        Given("a valid carrier plus individually inconsistent selection references and approval dependencies")
        val fixtures = _selection_fixture
        val review = _reference("f-review", 5L, InternalModelArtifactRole.Validation)
        val approval = _reference("g-approval", 7L, InternalModelArtifactRole.Approval)
        val badreviews = Vector(
          _reference("missing", 5L, InternalModelArtifactRole.Validation),
          _reference("f-review", 6L, InternalModelArtifactRole.Validation),
          _reference("f-review", 5L, InternalModelArtifactRole.Approval)
        )
        val badapprovals = Vector(
          _reference("missing", 7L, InternalModelArtifactRole.Approval),
          _reference("g-approval", 8L, InternalModelArtifactRole.Approval),
          _reference("g-approval", 7L, InternalModelArtifactRole.Validation)
        )
        _with_fixture(_manifest(fixtures._1), fixtures._2) { root =>
          When("selectors are given an ID, revision or role that does not equal the inventoried reference")
          val reviews = badreviews.map(value => InternalModelPackageValidator.verifiedCandidateReviewBinding(root, value).isSuccess)
          val approvals = badapprovals.map(value => InternalModelPackageValidator.verifiedCandidateHumanApproval(root, value, review).isSuccess)
          Then("no ID-only or newest-version selection substitutes an inventoried reference")
          reviews shouldBe Vector(false, false, false)
          approvals shouldBe Vector(false, false, false)
        }
        val mutations = Vector(
          fixtures._1.updated(5, fixtures._1(5).mapObject(_.add("required", Json.False))).dropRight(1) -> (fixtures._2 -- Set("f-review.bin", "g-approval.bin")),
          fixtures._1.updated(6, fixtures._1(6).mapObject(_.add("required", Json.False))) -> (fixtures._2 - "g-approval.bin"),
          fixtures._1.updated(6, fixtures._1(6).mapObject(_.add("dependsOn", Json.arr()))) -> fixtures._2
        )
        mutations.zipWithIndex.foreach { case ((artifacts, files), index) =>
          _with_fixture(_manifest(artifacts), files) { root =>
            Given("a structurally admitted carrier with an absent selected record or incorrect approval dependency set")
            When("the explicitly selected review or approval handoff is requested")
            val structural = InternalModelPackageValidator.validateStructure(root)
            val admitted = if index == 0 then InternalModelPackageValidator.verifiedCandidateReviewBinding(root, review).isSuccess
              else InternalModelPackageValidator.verifiedCandidateHumanApproval(root, approval, review).isSuccess
            Then("structural admission cannot supply absent selections or a substituted review dependency")
            structural.isSuccess shouldBe true
            admitted shouldBe false
          }
        }
      }

      "reject old, unknown and malformed projection discriminators without canonical-byte admission" in {
        Given("a V2 carrier whose continuity payload has individually unsupported or malformed profile data")
        val fixtures = _selection_fixture
        val invalid = Vector(
          "{\"profile\":\"ccdm-projection-binding-v1\",\"schemaVersion\":\"1.0\"}",
          "{\"profile\":\"ccdm-projection-binding-v2\",\"schemaVersion\":\"2.0\"}",
          "{\"profile\":\"ccdm-projection-binding-v3\",\"schemaVersion\":\"2.0\"}",
          "{\"profile\":\"unknown\",\"schemaVersion\":\"3.0\"}",
          "{\"profile\":\"ccdm-projection-binding-v3\",\"profile\":\"ccdm-projection-binding-v3\",\"schemaVersion\":\"3.0\"}",
          "{\"profile\":1,\"schemaVersion\":\"3.0\"}",
          "{\"profile\":\"ccdm-projection-binding-v3\"}",
          "profile: ccdm-projection-binding-v3\nschemaVersion: '3.0'\n"
        ).map(_bytes) ++ Vector(
          Vector(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ _profile("ccdm-projection-binding-v3", "3.0"),
          Vector(0xc3.toByte, 0x28.toByte)
        )
        invalid.foreach { payload =>
          _with_fixture(_manifest(fixtures._1), fixtures._2.updated("c-continuity.json", payload)) { root =>
            When("continuity selection decodes the ordinary strict JSON discriminator")
            val structural = InternalModelPackageValidator.validateStructure(root)
            val selected = InternalModelPackageValidator.verifiedProjectionContinuity(root)
            Then("structural validity supplies no legacy or malformed projection-family readiness")
            structural.isSuccess shouldBe true
            selected.isSuccess shouldBe false
          }
        }
        val legacy = Vector(
          "d-candidate.json" -> _profile("ccdm-candidate-cml-projection-v1", "1.0"),
          "e-diff.json" -> _profile("ccdm-semantic-diff-v1", "1.0")
        )
        legacy.foreach { case (path, payload) =>
          _with_fixture(_manifest(fixtures._1), fixtures._2.updated(path, payload)) { root =>
            Given("a V2 carrier containing an old candidate or semantic-diff profile pair")
            When("the semantic-diff handoff classifies all present ordinary projection payloads")
            val structural = InternalModelPackageValidator.validateStructure(root)
            val selected = InternalModelPackageValidator.verifiedSemanticDiff(root)
            Then("old candidate and diff pairs provide no compatibility selection")
            structural.isSuccess shouldBe true
            selected.isSuccess shouldBe false
          }
        }
      }
    }
  }

  private def _reference(id: String, revision: Long, role: InternalModelArtifactRole): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from(id).toOption.get, InternalModelArtifactRevision.from(revision).toOption.get, role)

  private def _reference_json(reference: InternalModelArtifactReference): Json =
    Json.obj(
      "artifactId" -> Json.fromString(reference.artifactId.value),
      "artifactRevision" -> Json.fromLong(reference.artifactRevision.value),
      "role" -> Json.fromString(reference.role.wireValue)
    )

  private def _artifact(
    reference: InternalModelArtifactReference,
    path: String,
    required: Boolean = true,
    dependencies: Vector[InternalModelArtifactReference] = Vector.empty
  ): Json =
    _reference_json(reference).mapObject(_.add("path", Json.fromString(path))
      .add("required", Json.fromBoolean(required))
      .add("dependsOn", Json.fromValues(dependencies.map(_reference_json))))

  private def _root(artifacts: Vector[Json], revision: Json = Json.fromLong(1L)): JsonObject =
    JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageId" -> Json.fromString(_package_id),
      "projectId" -> Json.fromString("sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> revision,
      "schemaVersion" -> Json.fromString("2.0")
    ))

  private def _manifest(artifacts: Vector[Json], revision: Json = Json.fromLong(1L)): String =
    _json(_root(artifacts, revision).toJson)

  private def _json(value: Json): String = _printer.print(value) + "\n"

  private def _bytes(value: String): Vector[Byte] = value.getBytes(StandardCharsets.UTF_8).toVector

  private def _files(artifacts: Vector[Json]): Map[String, Vector[Byte]] =
    artifacts.flatMap(_.hcursor.get[String]("path").toOption.map(path => path -> _payload)).toMap

  private def _shape_mutations(value: JsonObject): Vector[JsonObject] =
    value.keys.toVector.flatMap { key =>
      val wrongtype = if value(key).exists(_.isString) then Json.False else Json.fromString("wrong-type")
      Vector(value.remove(key), value.add(key, wrongtype))
    } :+ value.add("unexpected", Json.Null)

  private def _package_reference: InternalModelPackageReference =
    InternalModelPackageReference(
      InternalModelPackageId.from(_package_id).toOption.get,
      InternalModelProjectToken.from("org.example").toOption.get,
      InternalModelProjectToken.from("sample").toOption.get
    )

  private def _context: InternalModelVerifiedPackageContext = {
    val source = _reference("a-source", 4L, InternalModelArtifactRole.SourceSnapshot)
    val realization = _reference("b-realization", 6L, InternalModelArtifactRole.Realization)
    val optional = _reference("c-optional", 3L, InternalModelArtifactRole.OpenIssue)
    InternalModelVerifiedPackageContext(_package_reference, "2.0", 11L, "draft", Vector(
      InternalModelVerifiedArtifactContext(source, "source.bin", true, Vector.empty, true),
      InternalModelVerifiedArtifactContext(realization, "realization.bin", true, Vector(source), true),
      InternalModelVerifiedArtifactContext(optional, "optional.bin", false, Vector.empty, false)
    ))
  }

  private def _profile(profile: String, schema: String): Vector[Byte] =
    _bytes(" \n" + Printer.spaces2.print(Json.obj("schemaVersion" -> Json.fromString(schema), "profile" -> Json.fromString(profile))) + "\n")

  private def _selection_fixture: (Vector[Json], Map[String, Vector[Byte]]) = {
    val source = _reference("a-source", 1L, InternalModelArtifactRole.SourceSnapshot)
    val realization = _reference("b-realization", 2L, InternalModelArtifactRole.Realization)
    val continuity = _reference("c-continuity", 3L, InternalModelArtifactRole.Projection)
    val candidate = _reference("d-candidate", 4L, InternalModelArtifactRole.Projection)
    val semanticdiff = _reference("e-diff", 6L, InternalModelArtifactRole.Projection)
    val review = _reference("f-review", 5L, InternalModelArtifactRole.Validation)
    val approval = _reference("g-approval", 7L, InternalModelArtifactRole.Approval)
    val artifacts = Vector(
      _artifact(source, "a-source.bin"),
      _artifact(realization, "b-realization.bin", dependencies = Vector(source)),
      _artifact(continuity, "c-continuity.json", dependencies = Vector(realization)),
      _artifact(candidate, "d-candidate.json", dependencies = Vector(continuity)),
      _artifact(semanticdiff, "e-diff.json", dependencies = Vector(candidate)),
      _artifact(review, "f-review.bin", dependencies = Vector(semanticdiff)),
      _artifact(approval, "g-approval.bin", dependencies = Vector(review))
    )
    val files = _files(artifacts)
      .updated("c-continuity.json", _profile("ccdm-projection-binding-v3", "3.0"))
      .updated("d-candidate.json", _profile("ccdm-candidate-cml-projection-v2", "2.0"))
      .updated("e-diff.json", _profile("ccdm-semantic-diff-v2", "2.0"))
    artifacts -> files
  }

  private def _with_fixture(
    manifest: String,
    files: Map[String, Vector[Byte]],
    projectyaml: String = _project_yaml
  )(f: Path => Unit): Unit =
    _with_fixture_bytes(_bytes(manifest), files, projectyaml)(f)

  private def _with_fixture_bytes(
    manifest: Vector[Byte],
    files: Map[String, Vector[Byte]],
    projectyaml: String = _project_yaml
  )(f: Path => Unit): Unit = {
    val workroot = Path.of("target/internal-model-package-validator/work").toAbsolutePath.normalize
    Files.createDirectories(workroot)
    val root = Files.createTempDirectory(workroot, "capture-")
    try {
      _write(root.resolve("project.yaml"), _bytes(projectyaml))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), manifest)
      files.foreach { case (path, bytes) => _write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      f(root)
    } finally _delete_tree(root)
  }

  private def _write(path: Path, bytes: Vector[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes.toArray, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    ()
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root, LinkOption.NOFOLLOW_LINKS) then {
      val paths = Files.walk(root)
      try paths.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
      finally paths.close()
    }
}
