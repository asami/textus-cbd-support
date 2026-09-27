package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest
import java.util.Base64

import scala.jdk.CollectionConverters.*

import com.sun.jna.{Library, Native, Platform}
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
final class InternalModelPackageFreshnessSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private trait DarwinTestLibC extends Library {
    def mkfifo(pathname: String, mode: Int): Int
  }

  private val _printer = Printer.noSpacesSortKeys
  private val _project_yaml = """project:
    |  namespace: org.example
    |  id: sample
    |""".stripMargin
  private val _package_id = "01234567-89ab-cdef-0123-456789abcdef"
  private val _authority = "catalog-authority"
  private val _identity = "source-identity"
  private val _revision = "source-revision"
  private val _scenario_raw = "scenario raw source\n".getBytes(StandardCharsets.UTF_8)
  private val _model_raw = "model raw source\n".getBytes(StandardCharsets.UTF_8)
  private val _glossary_raw = "glossary raw source\n".getBytes(StandardCharsets.UTF_8)
  private val _cml_raw = "entity Customer\n".getBytes(StandardCharsets.UTF_8)

  "Internal-model package freshness" should {
    "derive its checked inventory from the verified manifest pass" which {
      "compare every source kind and same-kind artifact in manifest order regardless of caller map insertion order" in {
        Given("a manifest-order inventory with four source kinds, two Scenario artifacts, and one non-source artifact")
        val cmlsnapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "models/current.cml"), _cml_raw)
        val glossarysnapshot = _snapshot("glossary-bok", _glossary_basis, _glossary_raw)
        val modelsnapshot = _snapshot("model-context", _model_context_basis, _model_raw)
        val scenariosnapshot = _snapshot("scenario", _scenario_basis("first Scenario body\n"), _scenario_raw)
        val secondscenario = _snapshot("scenario", _scenario_basis("second Scenario body\n"), "second Scenario raw\n".getBytes(StandardCharsets.UTF_8), identity = "second-source")
        val decisionbytes = "decision\n".getBytes(StandardCharsets.UTF_8)
        val artifacts = Vector(
          _artifact("decision", "decision.json", required = true, decisionbytes),
          _artifact("source-cml", "snapshots/cml.json", required = true, cmlsnapshot, role = "source-snapshot"),
          _artifact("source-glossary", "snapshots/glossary.json", required = true, glossarysnapshot, role = "source-snapshot"),
          _artifact("source-model", "snapshots/model.json", required = true, modelsnapshot, role = "source-snapshot"),
          _artifact("source-scenario", "snapshots/scenario.json", required = true, scenariosnapshot, role = "source-snapshot"),
          _artifact("source-scenario-second", "snapshots/scenario-second.json", required = true, secondscenario, role = "source-snapshot")
        )
        val inputs = Vector(
          "source-scenario-second" -> InternalModelPackageFreshnessInput.SourceObservation(_observed("second Scenario raw\n".getBytes(StandardCharsets.UTF_8), identity = "second-source")),
          "source-model" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_model_raw)),
          "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml"),
          "source-glossary" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_glossary_raw)),
          "source-scenario" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_scenario_raw))
        )

        _with_fixture(
          _manifest(artifacts),
          Map(
            "decision.json" -> decisionbytes,
            "snapshots/cml.json" -> cmlsnapshot,
            "snapshots/glossary.json" -> glossarysnapshot,
            "snapshots/model.json" -> modelsnapshot,
            "snapshots/scenario.json" -> scenariosnapshot,
            "snapshots/scenario-second.json" -> secondscenario
          ),
          Map("models/current.cml" -> _cml_raw)
        ) { root =>
          When("the source-owner inputs are supplied in a different map insertion order")
          val result = InternalModelPackageFreshness.check(root, Map.from(inputs))

          Then("only source-snapshot entries retain deterministic manifest order and every independently observed kind is unchanged")
          val report = _report(result)
          report.entries.map(_.artifactId) shouldBe Vector("source-cml", "source-glossary", "source-model", "source-scenario", "source-scenario-second")
          report.entries.map(_.packageRelativePath) shouldBe Vector("snapshots/cml.json", "snapshots/glossary.json", "snapshots/model.json", "snapshots/scenario.json", "snapshots/scenario-second.json")
          report.entries.map(entry => _freshness(entry).status) shouldBe Vector.fill(5)(InternalModelSnapshotFreshnessStatus.Unchanged)
          report.toString should not include "first Scenario body"
          report.toString should not include "entity Customer"
        }
      }

      "reject unknown and non-source input IDs and fail closed when package integrity is invalid" in {
        Given("a manifest with one source snapshot and one ordinary decision artifact")
        val snapshot = _snapshot("scenario", _scenario_basis("Scenario body\n"), _scenario_raw)
        val decisionbytes = "decision\n".getBytes(StandardCharsets.UTF_8)
        val source = _artifact("source-scenario", "snapshots/scenario.json", required = true, snapshot, role = "source-snapshot")
        val decision = _artifact("decision", "decision.json", required = true, decisionbytes)

        _with_fixture(_manifest(Vector(decision, source)), Map("decision.json" -> decisionbytes, "snapshots/scenario.json" -> snapshot)) { root =>
          When("an input names an unknown ID or an inventory artifact outside the source-snapshot role")
          val unknown = InternalModelPackageFreshness.check(root, Map("missing" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_scenario_raw))))
          val nonsource = InternalModelPackageFreshness.check(root, Map("decision" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_scenario_raw))))

          Then("neither ID is silently ignored or selected as a snapshot")
          unknown.isSuccess shouldBe false
          nonsource.isSuccess shouldBe false
        }
        _with_fixture(_manifest(Vector(source)), Map("snapshots/scenario.json" -> "tampered\n".getBytes(StandardCharsets.UTF_8))) { root =>
          When("the manifest-declared artifact digest does not match the stored snapshot bytes")
          val result = InternalModelPackageFreshness.check(root, Map("source-scenario" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_scenario_raw))))

          Then("the package-wide entry returns no partial freshness report")
          result.isSuccess shouldBe false
        }
        _with_fixture(_manifest(Vector(source)), Map("snapshots/scenario.json" -> snapshot)) { root =>
          Given("an unlisted regular package file after the manifest has been created")
          _write(root.resolve("src/main/internal-model/unlisted.json"), "unlisted\n".getBytes(StandardCharsets.UTF_8))
          When("the package-wide entry requests its validator-owned inventory")
          val result = InternalModelPackageFreshness.check(root, Map("source-scenario" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_scenario_raw))))

          Then("the closed inventory rejection prevents a partial freshness report")
          result.isSuccess shouldBe false
        }
      }

      "return an empty report for an empty verified source-snapshot inventory without claiming completeness" in {
        Given("a valid package manifest that contains only one ordinary decision artifact")
        val decisionbytes = "decision\n".getBytes(StandardCharsets.UTF_8)
        val decision = _artifact("decision", "decision.json", required = true, decisionbytes)

        _with_fixture(_manifest(Vector(decision)), Map("decision.json" -> decisionbytes)) { root =>
          When("the caller supplies no source-snapshot inputs")
          val report = _report(InternalModelPackageFreshness.check(root, Map.empty))

          Then("the deterministic report is empty and carries no synthetic completeness result")
          report.entries shouldBe Vector.empty
        }
      }
    }

    "retain explicit baseline and source-owner observation states" which {
      "report missing present input, absent optional baseline, and all closed non-observed states without source selection" in {
        Given("a present Scenario baseline, an absent optional baseline, and independently supplied non-observed source states")
        val scenario = _snapshot("scenario", _scenario_basis("Scenario body\n"), _scenario_raw)
        val optionalbytes = _snapshot("scenario", _scenario_basis("optional Scenario body\n"), _scenario_raw)
        val artifacts = Vector(
          _artifact("source-optional", "snapshots/optional.json", required = false, optionalbytes, role = "source-snapshot"),
          _artifact("source-scenario", "snapshots/scenario.json", required = true, scenario, role = "source-snapshot")
        )
        val states = Vector(
          InternalModelLiveSourceObservation.Unavailable("provider offline") -> InternalModelSnapshotFreshnessStatus.Unavailable,
          InternalModelLiveSourceObservation.Unauthorized("source access denied") -> InternalModelSnapshotFreshnessStatus.Unauthorized,
          InternalModelLiveSourceObservation.Malformed("provider response malformed") -> InternalModelSnapshotFreshnessStatus.Malformed,
          InternalModelLiveSourceObservation.AmbiguousOrConflicting("two source revisions conflict") -> InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting
        )

        _with_fixture(_manifest(artifacts), Map("snapshots/scenario.json" -> scenario)) { root =>
          When("a present baseline has no input")
          val missing = _report(InternalModelPackageFreshness.check(root, Map.empty))

          Then("the present baseline is unavailable while the optional absent baseline is distinct and does not read a target")
          missing.entries.map(entry => entry.artifactId -> entry.result) shouldBe Vector(
            "source-optional" -> InternalModelPackageFreshnessResult.MissingBaseline,
            "source-scenario" -> InternalModelPackageFreshnessResult.Compared(_freshness(missing.entries.last))
          )
          _freshness(missing.entries.last).status shouldBe InternalModelSnapshotFreshnessStatus.Unavailable
          _freshness(missing.entries.last).reason should not be None

          When("an absent optional baseline is offered a CML-specific input with an unsafe path")
          val absent = _report(InternalModelPackageFreshness.check(root, Map(
            "source-optional" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "../outside/customer.cml")
          )))

          Then("the absent baseline remains MissingBaseline before any CML path is validated or read")
          absent.entries.head.result shouldBe InternalModelPackageFreshnessResult.MissingBaseline

          states.foreach { case (observation, status) =>
            When("the source owner reports one closed non-observed state")
            val report = _report(InternalModelPackageFreshness.check(root, Map("source-scenario" -> InternalModelPackageFreshnessInput.SourceObservation(observation))))

            Then("the package report preserves that state instead of manufacturing observed bytes")
            _freshness(report.entries.last).status shouldBe status
            _freshness(report.entries.last).observedRawBytesSha256 shouldBe None
          }
        }
      }

      "give a malformed source-snapshot baseline precedence over supplied live input" in {
        Given("a package-integrity-valid source-snapshot artifact whose own bytes are not a valid V1 snapshot")
        val malformed = "{not-json}\n".getBytes(StandardCharsets.UTF_8)
        val artifact = _artifact("source-scenario", "snapshots/scenario.json", required = true, malformed, role = "source-snapshot")

        _with_fixture(_manifest(Vector(artifact)), Map("snapshots/scenario.json" -> malformed)) { root =>
          When("the source owner reports unauthorized access")
          val report = _report(InternalModelPackageFreshness.check(root, Map("source-scenario" -> InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Unauthorized("not consulted")))))

          Then("the malformed baseline result is retained before the live status is considered")
          _freshness(report.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          _freshness(report.entries.head).snapshotKind shouldBe None
        }
      }
    }

    "compare raw bytes and source dimensions without caller-order effects" which {
      "surface generated raw-byte perturbations in the matching Scenario entry while preserving the immutable baseline" in {
        Given("two Scenario baselines and generated raw-byte suffixes with alternate caller input map orders")
        val first = _snapshot("scenario", _scenario_basis("first Scenario body\n"), _scenario_raw)
        val secondraw = "second Scenario raw\n".getBytes(StandardCharsets.UTF_8)
        val second = _snapshot("scenario", _scenario_basis("second Scenario body\n"), secondraw, identity = "second-source")
        val original = first.clone()
        val artifacts = Vector(
          _artifact("source-first", "snapshots/first.json", required = true, first, role = "source-snapshot"),
          _artifact("source-second", "snapshots/second.json", required = true, second, role = "source-snapshot")
        )
        val generated = for {
          suffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
          reverse <- Gen.oneOf(true, false)
        } yield suffix -> reverse

        _with_fixture(_manifest(artifacts), Map("snapshots/first.json" -> first, "snapshots/second.json" -> second)) { root =>
          forAll(generated) { case (suffix, reverse) =>
            When("one caller-owned raw-byte sequence changes and the input map is inserted in either order")
            val firstinput = "source-first" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(("changed raw " + suffix).getBytes(StandardCharsets.UTF_8)))
            val secondinput = "source-second" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(secondraw, identity = "second-source"))
            val inputs = if reverse then Map.from(Vector(secondinput, firstinput)) else Map.from(Vector(firstinput, secondinput))
            val report = _report(InternalModelPackageFreshness.check(root, inputs))

            Then("the manifest-order first entry reports only its raw digest drift and neither supplied nor baseline bytes are retained")
            report.entries.map(_.artifactId) shouldBe Vector("source-first", "source-second")
            _freshness(report.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Changed
            _freshness(report.entries.head).changedDimensionNames shouldBe Vector("source.sha256")
            _freshness(report.entries.last).status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
            first should contain theSameElementsInOrderAs original
            report.toString should not include ("changed raw " + suffix)
          }
        }
      }

      "retain every CML source and target difference when a source-owner current target has changed" in {
        Given("a CML baseline and a different source-owner target below the consuming project root")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "recorded/customer.cml"), _cml_raw)
        val current = "entity Customer\nattribute name\n".getBytes(StandardCharsets.UTF_8)
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")

        _with_fixture(_manifest(Vector(artifact)), Map("snapshots/cml.json" -> baseline), Map("models/current-customer.cml" -> current)) { root =>
          When("the CML owner supplies a different current target, identity, revision, and exact file bytes")
          val report = _report(InternalModelPackageFreshness.check(root, Map(
            "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved("other-authority", "other-identity", Some("other-revision"), "models/current-customer.cml")
          )))

          Then("the pure comparator receives only captured bytes and reports every deterministic difference")
          _freshness(report.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Changed
          _freshness(report.entries.head).changedDimensionNames shouldBe Vector(
            "basis.byteLength",
            "basis.projectRelativePath",
            "basis.rawBytesBase64",
            "source.authority",
            "source.identity",
            "source.revision",
            "source.sha256"
          )
          _freshness(report.entries.head).currentCmlProjectRelativePath shouldBe Some("models/current-customer.cml")
          report.toString should not include "attribute name"

          When("the CML source owner reports an unavailable state instead of an observed target")
          val unavailable = _report(InternalModelPackageFreshness.check(root, Map(
            "source-cml" -> InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Unavailable("CML source is offline"))
          )))

          Then("the non-observed CML status is retained explicitly without a CML read")
          _freshness(unavailable.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Unavailable
          _freshness(unavailable.entries.head).observedRawBytesSha256 shouldBe None
        }
      }
    }

    "enforce the project-bound CML read boundary" which {
      "use the source-owner current target rather than the recorded path or locator and reject a supplied generic CML observation" in {
        Given("a CML snapshot whose recorded basis path and locator do not name the source-owner current target")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "recorded/never-read.cml"), _cml_raw, locator = Json.fromString("https://example.invalid/never-read"))
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")

        _with_fixture(_manifest(Vector(artifact)), Map("snapshots/cml.json" -> baseline), Map("models/current.cml" -> _cml_raw)) { root =>
          When("the owner supplies the actual current target with equal bytes")
          val equal = _report(InternalModelPackageFreshness.check(root, Map(
            "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
          )))
          When("a caller instead tries to inject a generic observed CML byte value")
          val injected = _report(InternalModelPackageFreshness.check(root, Map(
            "source-cml" -> InternalModelPackageFreshnessInput.SourceObservation(_observed(_cml_raw, projectrelativepath = Some("models/current.cml")))
          )))

          Then("only the owner target is read, so its changed path drifts and the injected observation is malformed")
          _freshness(equal.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Changed
          _freshness(equal.entries.head).changedDimensionNames shouldBe Vector("basis.projectRelativePath")
          _freshness(injected.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
        }
      }

      "preserve missing, unsafe, symbolic, and nonregular target outcomes without an unsafe read" in {
        Given("one CML baseline and owner-supplied target paths covering each project-bound read outcome")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "recorded/customer.cml"), _cml_raw)
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")

        _with_fixture(_manifest(Vector(artifact)), Map("snapshots/cml.json" -> baseline)) { root =>
          Files.createDirectories(root.resolve("models/directory.cml"))
          Files.createDirectories(root.resolve("outside"))
          _write(root.resolve("outside/customer.cml"), _cml_raw)
          Files.createDirectories(root.resolve("links"))
          Files.createSymbolicLink(root.resolve("links/component"), root.resolve("outside"))
          Files.createSymbolicLink(root.resolve("links/final.cml"), root.resolve("outside/customer.cml"))
          val requests = Vector(
            "missing" -> "models/missing.cml" -> InternalModelSnapshotFreshnessStatus.Unavailable,
            "traversal" -> "../outside/customer.cml" -> InternalModelSnapshotFreshnessStatus.Malformed,
            "symbolic-component" -> "links/component/customer.cml" -> InternalModelSnapshotFreshnessStatus.Malformed,
            "symbolic-final" -> "links/final.cml" -> InternalModelSnapshotFreshnessStatus.Malformed,
            "directory" -> "models/directory.cml" -> InternalModelSnapshotFreshnessStatus.Malformed
          )

          requests.foreach { case ((label, path), status) =>
            When("the CML owner supplies the " + label + " current target path")
            val report = _report(InternalModelPackageFreshness.check(root, Map(
              "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), path)
            )))

            Then("the closed outcome is reported without using the recorded baseline path as read authority")
            _freshness(report.entries.head).status shouldBe status
            _freshness(report.entries.head).observedRawBytesSha256 shouldBe None
          }
        }
      }

      "fail closed without exposing external raw bytes when an in-root directory becomes a symlink between owner checks" in {
        Given("a CML baseline with a regular in-root current-target directory and an external file with distinct bytes")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "models/current/customer.cml"), _cml_raw)
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")
        val outside = Files.createTempDirectory("internal-model-package-freshness-outside-")

        try {
          _write(outside.resolve("customer.cml"), "entity Outside\n".getBytes(StandardCharsets.UTF_8))
          _with_fixture(
            _manifest(Vector(artifact)),
            Map("snapshots/cml.json" -> baseline),
            Map("models/current/customer.cml" -> _cml_raw)
          ) { root =>
            When("the source owner checks the regular in-root target")
            val before = _report(InternalModelPackageFreshness.check(root, Map(
              "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current/customer.cml")
            )))

            Then("the equal current target remains unchanged")
            _freshness(before.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged

            When("the target directory is replaced by an in-root symbolic-link component before the next owner check")
            val current = root.resolve("models/current")
            Files.delete(current.resolve("customer.cml"))
            Files.delete(current)
            Files.createSymbolicLink(current, outside)
            val after = _report(InternalModelPackageFreshness.check(root, Map(
              "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current/customer.cml")
            )))

            Then("the second check is malformed and retains no external raw-byte evidence")
            _freshness(after.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
            _freshness(after.entries.head).observedRawBytesSha256 shouldBe None
            after.toString should not include "entity Outside"
          }
        } finally _delete_tree(outside)
      }

      "fail closed without raw-byte evidence when a regular current target is replaced by a FIFO before the next owner check" in {
        Given("a CML baseline and a regular source-owner current target")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "models/current.cml"), _cml_raw)
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")

        _with_fixture(_manifest(Vector(artifact)), Map("snapshots/cml.json" -> baseline), Map("models/current.cml" -> _cml_raw)) { root =>
          if (Platform.isMac) {
            When("the source owner checks the regular current target")
            val before = _report(InternalModelPackageFreshness.check(root, Map(
              "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
            )))

            Then("the initial package-entry observation remains unchanged")
            _freshness(before.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged

            Given("the current target is deterministically replaced by a FIFO before the next owner check")
            val target = root.resolve("models/current.cml")
            Files.delete(target)
            _darwin_libc.mkfifo(target.toString, 0x1a4) shouldBe 0

            When("the source owner repeats the package-entry CML check")
            val after = _report(InternalModelPackageFreshness.check(root, Map(
              "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
            )))

            Then("the final nonregular descriptor is malformed without a blocking read or retained raw bytes")
            _freshness(after.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
            _freshness(after.entries.head).observedRawBytesSha256 shouldBe None
            after.toString should not include "entity Customer"
          } else {
            When("the source owner requests the CML check on a platform without the descriptor-safe reader")
            val after = _report(InternalModelPackageFreshness.check(root, Map(
              "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
            )))

            Then("the package entry remains fail-closed without raw-byte evidence")
            _freshness(after.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
            _freshness(after.entries.head).observedRawBytesSha256 shouldBe None
          }
        }
      }

      "read the source-owner target through the physical root for equal and drifted CML bytes without retaining payloads" in {
        Given("a CML baseline whose recorded path is not the owner-supplied equal current target")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "recorded/customer.cml"), _cml_raw)
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")

        _with_fixture(_manifest(Vector(artifact)), Map("snapshots/cml.json" -> baseline), Map("models/current.cml" -> _cml_raw)) { root =>
          When("the CML source owner supplies the equal current path below the consuming project root")
          val equal = _report(InternalModelPackageFreshness.check(root, Map(
            "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
          )))

          Then("the owner path is compared as a path difference while its exact equal bytes remain non-reportable")
          _freshness(equal.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Changed
          _freshness(equal.entries.head).changedDimensionNames shouldBe Vector("basis.projectRelativePath")
          equal.toString should not include "entity Customer"

          Given("the same source-owner target is replaced with a regular file containing distinct raw bytes")
          val changed = "entity Customer\nattribute name\n".getBytes(StandardCharsets.UTF_8)
          _write(root.resolve("models/current.cml"), changed)
          When("the source owner repeats the same current-target request")
          val drifted = _report(InternalModelPackageFreshness.check(root, Map(
            "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
          )))

          Then("only the deterministic CML byte dimensions drift and the changed raw content remains absent from the report")
          _freshness(drifted.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Changed
          _freshness(drifted.entries.head).changedDimensionNames shouldBe Vector(
            "basis.byteLength",
            "basis.projectRelativePath",
            "basis.rawBytesBase64",
            "source.sha256"
          )
          drifted.toString should not include "attribute name"
        }
      }

      "reject a symbolic final consuming-project root before its owner target can be read" in {
        Given("a symbolic project-root final component and a regular external-looking target beneath its destination")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "models/current.cml"), _cml_raw)
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")
        val parent = Files.createTempDirectory("internal-model-package-freshness-root-link-")
        try {
          val actual = parent.resolve("actual")
          val alias = parent.resolve("alias")
          _write(actual.resolve("project.yaml"), _project_yaml.getBytes(StandardCharsets.UTF_8))
          _write(actual.resolve("src/main/internal-model/manifest.yaml"), _manifest(Vector(artifact)).getBytes(StandardCharsets.UTF_8))
          _write(actual.resolve("src/main/internal-model/snapshots/cml.json"), baseline)
          _write(actual.resolve("models/current.cml"), "entity Outside\n".getBytes(StandardCharsets.UTF_8))
          Files.createSymbolicLink(alias, actual)

          When("the package freshness entry is given the symbolic project-root path")
          val result = InternalModelPackageFreshness.check(alias, Map(
            "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
          ))

          Then("the existing package boundary rejects the symbolic root before any CML observation is captured")
          result.isSuccess shouldBe false
        } finally _delete_tree(parent)
      }

      "reject a symbolic ancestor of a regular consuming-project root without exposing external CML bytes" in {
        Given("a regular consuming-project root reached through a symbolic ancestor with external-looking CML bytes")
        val baseline = _snapshot("cml-baseline", _cml_basis(_cml_raw, "models/current.cml"), _cml_raw)
        val artifact = _artifact("source-cml", "snapshots/cml.json", required = true, baseline, role = "source-snapshot")
        val parent = Files.createTempDirectory("internal-model-package-freshness-root-ancestor-")
        try {
          val external = parent.resolve("external")
          val alias = parent.resolve("alias")
          val project = external.resolve("project")
          _write(project.resolve("project.yaml"), _project_yaml.getBytes(StandardCharsets.UTF_8))
          _write(project.resolve("src/main/internal-model/manifest.yaml"), _manifest(Vector(artifact)).getBytes(StandardCharsets.UTF_8))
          _write(project.resolve("src/main/internal-model/snapshots/cml.json"), baseline)
          _write(project.resolve("models/current.cml"), "entity Outside\n".getBytes(StandardCharsets.UTF_8))
          Files.createSymbolicLink(alias, external)

          When("the package freshness entry receives the regular final root below the symbolic ancestor")
          val report = _report(InternalModelPackageFreshness.check(alias.resolve("project"), Map(
            "source-cml" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
          )))

          Then("the descriptor-root policy reports a malformed CML observation without retaining external bytes")
          _freshness(report.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          _freshness(report.entries.head).observedRawBytesSha256 shouldBe None
          report.toString should not include "entity Outside"
        } finally _delete_tree(parent)
      }

      "report a CML request for a non-CML baseline as malformed without consulting a project target" in {
        Given("a valid Scenario baseline and an owner-supplied CML current target request")
        val scenario = _snapshot("scenario", _scenario_basis("Scenario body\n"), _scenario_raw)
        val artifact = _artifact("source-scenario", "snapshots/scenario.json", required = true, scenario, role = "source-snapshot")

        _with_fixture(_manifest(Vector(artifact)), Map("snapshots/scenario.json" -> scenario), Map("models/current.cml" -> _cml_raw)) { root =>
          When("the CML-specific request is applied to the Scenario artifact")
          val report = _report(InternalModelPackageFreshness.check(root, Map(
            "source-scenario" -> InternalModelPackageFreshnessInput.CmlObserved(_authority, _identity, Some(_revision), "models/current.cml")
          )))

          Then("the type mismatch is malformed and no CML bytes appear in the report")
          _freshness(report.entries.head).status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          report.toString should not include "entity Customer"
        }
      }
    }
  }

  private def _freshness(entry: InternalModelPackageFreshnessEntry): InternalModelSnapshotFreshnessReport =
    entry.result match {
      case InternalModelPackageFreshnessResult.Compared(report) => report
      case InternalModelPackageFreshnessResult.MissingBaseline => fail("freshness report is absent for an optional baseline")
    }

  private def _report(result: org.goldenport.Consequence[InternalModelPackageFreshnessReport]): InternalModelPackageFreshnessReport =
    result.toOption.getOrElse(fail("package freshness report is missing"))

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

  private def _manifest(artifacts: Vector[Json]): String =
    _canonical_manifest(JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)),
      "packageId" -> Json.fromString(_package_id),
      "projectId" -> Json.fromString("sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(1),
      "schemaVersion" -> Json.fromString("1.0")
    )))

  private def _canonical_manifest(root: JsonObject): String = {
    val digest = _sha256(_canonical(root.remove("packageDigest").toJson))
    _canonical(root.add("packageDigest", Json.fromString(digest)).toJson).map(_.toChar).mkString
  }

  private def _snapshot(
    kind: String,
    basis: Json,
    rawbytes: Array[Byte],
    authority: String = _authority,
    identity: String = _identity,
    revision: Json = Json.fromString(_revision),
    locator: Json = Json.Null
  ): Array[Byte] =
    _canonical(Json.fromJsonObject(JsonObject.fromIterable(Vector(
      "basis" -> basis,
      "schemaVersion" -> Json.fromString("1.0"),
      "snapshotKind" -> Json.fromString(kind),
      "source" -> Json.obj(
        "authority" -> Json.fromString(authority),
        "identity" -> Json.fromString(identity),
        "locator" -> locator,
        "revision" -> revision,
        "sha256" -> Json.fromString(_sha256(rawbytes))
      )
    ))))

  private def _observed(
    rawbytes: Array[Byte],
    authority: String = _authority,
    identity: String = _identity,
    revision: Option[String] = Some(_revision),
    projectrelativepath: Option[String] = None
  ): InternalModelLiveSourceObservation =
    InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes.toVector, projectrelativepath)

  private def _scenario_basis(content: String): Json =
    Json.obj(
      "content" -> Json.fromString(content),
      "scenarioId" -> Json.fromString("scenario-identity"),
      "traceLinks" -> Json.arr(_trace_link)
    )

  private def _trace_link: Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-identity"),
      "projectionContextIdentity" -> Json.fromString("projection-context-identity"),
      "scenarioAnchor" -> Json.fromString("scenario-anchor"),
      "semanticIdentity" -> Json.fromString("semantic-identity"),
      "semanticIdentityKind" -> Json.fromString("element")
    )

  private def _model_context_basis: Json =
    Json.obj(
      "contextIdentity" -> Json.fromString("model-context-identity"),
      "facts" -> Json.arr(Json.obj(
        "componentIdentity" -> Json.fromString("component-identity"),
        "content" -> Json.fromString("model fact\n"),
        "limitations" -> Json.arr(Json.fromString("source limitation")),
        "projectionContextIdentity" -> Json.fromString("projection-context-identity"),
        "semanticIdentity" -> Json.fromString("semantic-identity"),
        "semanticIdentityKind" -> Json.fromString("relationship"),
        "sourceAnchor" -> Json.fromString("model-source-anchor")
      ))
    )

  private def _glossary_basis: Json =
    Json.obj(
      "entries" -> Json.arr(Json.obj(
        "definition" -> Json.fromString("glossary definition\n"),
        "limitations" -> Json.arr(Json.fromString("source limitation")),
        "sourceAnchor" -> Json.fromString("glossary-source-anchor"),
        "termIdentity" -> Json.fromString("term-identity"),
        "termLabel" -> Json.fromString("Term")
      ))
    )

  private def _cml_basis(rawbytes: Array[Byte], path: String): Json =
    Json.obj(
      "byteLength" -> Json.fromInt(rawbytes.length),
      "projectRelativePath" -> Json.fromString(path),
      "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes))
    )

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString

  private lazy val _darwin_libc: DarwinTestLibC = Native.load("c", classOf[DarwinTestLibC])

  private def _with_fixture(
    manifest: String,
    packagefiles: Map[String, Array[Byte]],
    projectfiles: Map[String, Array[Byte]] = Map.empty
  )(f: Path => Unit): Unit = {
    val root = Files.createTempDirectory("internal-model-package-freshness-")
    try {
      _write(root.resolve("project.yaml"), _project_yaml.getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), manifest.getBytes(StandardCharsets.UTF_8))
      packagefiles.foreach { case (path, bytes) => _write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      projectfiles.foreach { case (path, bytes) => _write(root.resolve(path), bytes) }
      f(root)
    } finally _delete_tree(root)
  }

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root) then Files.walk(root).iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
}
