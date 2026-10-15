package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, NoSuchFileException, Path}

import scala.jdk.CollectionConverters._

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class Phase5ExecutableCoverageSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The Phase 5 executable coverage manifest" should {
    "cover every behavioral checklist item exactly once" in {
      Given("the authoritative Phase 5 checklist and executable coverage manifest")
      val checklistids = _behavioral_checklist_ids
      val coverage = _load_coverage

      When("the behavioral contract IDs and coverage entries are compared")
      val coverageids = coverage.map(_.id)

      Then("all contract, Review, provider, CI, surface, quality, AI, and runtime IDs are covered once")
      checklistids shouldBe _expected_ids
      coverageids.distinct shouldBe coverageids
      coverageids.toSet shouldBe checklistids.toSet
      coverage.map(_.area).toSet shouldBe _expected_areas
      coverage.foreach { item =>
        item.area shouldBe _expected_area_by_id(item.id)
        withClue(s"${item.id} has no executable evidence: ") {
          item.evidence should not be empty
        }
      }
    }

    "bind every item to an existing executable scenario or gate anchor" in {
      Given("a coverage entry whose evidence must stay navigable and executable")
      val coverage = _load_coverage

      When("every repository-relative evidence path and exact anchor is inspected")
      val evidence = coverage.flatMap(item => item.evidence.map(item.id -> _))

      Then("CBD, sibling-provider, and integration-gate evidence exist at their declared anchors")
      evidence.foreach { case (id, entry) =>
        val path = Path.of(entry.sourcepath)
        withClue(s"$id ${entry.sourcepath}: ") {
          path.isAbsolute shouldBe false
          path.normalize shouldBe path
          val physicalpath = _physical_source_path(entry.evidencekind, path, sys.env)
          Files.isRegularFile(physicalpath) shouldBe true
          entry.evidencekind match {
            case "scala-spec" => _validate_scala_spec(path, entry.anchor)
            case "sibling-scala-spec" =>
              path.startsWith(Path.of("..")) shouldBe true
              _validate_scala_spec(physicalpath, entry.anchor)
            case "script-gate" =>
              path.startsWith(Path.of("scripts")) shouldBe true
              Files.isExecutable(path) shouldBe true
              Files.readString(path) should include(entry.anchor)
            case other => fail(s"$id uses unsupported executable evidence kind: $other")
          }
        }
      }
    }
  }

  "The Phase 5 sibling source resolver" should {
    "preserve each exact suffix under its selected root and leave other paths unchanged" in {
      Given("resolver-only directories for all three explicit source roots")
      _with_resolver_fixture { fixture =>
        val roots = _sibling_source_roots.map { case (_, variable) =>
          variable -> Files.createDirectory(fixture.resolve(variable)).toString
        }.toMap
        val suffix = Path.of("src", "test", "scala", "nested", "CoverageSpec.scala")
        val unchanged = Vector(
          "scala-spec" -> Path.of("src/test/scala/LocalSpec.scala"),
          "script-gate" -> Path.of("scripts/probe.sh"),
          "sibling-scala-spec" -> Path.of("../cozy-other/src/test/scala/OtherSpec.scala"),
          "sibling-scala-spec" -> Path.of("../textus-ai-other/src/test/scala/OtherSpec.scala")
        )

        When("configured sibling prefixes and repository-local paths are resolved")
        val selected = _sibling_source_roots.map { case (prefix, variable) =>
          _physical_source_path("sibling-scala-spec", prefix.resolve(suffix), roots) ->
            Path.of(roots(variable)).resolve(suffix)
        }
        val preserved = unchanged.map { case (kind, path) =>
          _physical_source_path(kind, path, roots) -> path
        }
        val defaults = _sibling_source_roots.map { case (prefix, _) =>
          val path = prefix.resolve(suffix)
          _physical_source_path("sibling-scala-spec", path, Map.empty) -> path
        }

        Then("only exact sibling components are remapped and every suffix is retained")
        (selected ++ preserved ++ defaults).foreach { case (actual, expected) =>
          actual shouldBe expected
        }
      }
    }

    "reject blank, relative, nonnormalized, missing and nondirectory configured roots" in {
      Given("resolver-only root configuration that violates the explicit directory contract")
      _with_resolver_fixture { fixture =>
        val regularfile = Files.writeString(fixture.resolve("not-a-directory"), "resolver-only")
        val invalidroots = Vector(
          "", " ", "relative/root", fixture.resolve("child/..").toString,
          fixture.resolve("missing").toString, regularfile.toString
        )
        _sibling_source_roots.foreach { case (prefix, variable) =>
          invalidroots.foreach { root =>
            When(s"$variable explicitly selects the invalid root '$root'")
            val resolve = () => _physical_source_path(
              "sibling-scala-spec", prefix.resolve("src/test/scala/CoverageSpec.scala"), Map(variable -> root)
            )

            Then("resolution fails rather than using historical relative evidence")
            intercept[IllegalArgumentException] { resolve() }
          }
        }
      }
    }

    "use the explicit root exclusively while missing sources and exact anchors still fail" in {
      Given("a resolver-only selected directory with an absent source and a wrong-anchor source")
      _with_resolver_fixture { fixture =>
        val wronganchor = Files.writeString(fixture.resolve("WrongSpec.scala"), "\"another anchor\"")
        _sibling_source_roots.foreach { case (prefix, variable) =>
          val roots = Map(variable -> fixture.toString)

          When("the selected root resolves both declared sibling sources")
          val missing = _physical_source_path("sibling-scala-spec", prefix.resolve("MissingSpec.scala"), roots)
          val wrong = _physical_source_path("sibling-scala-spec", prefix.resolve("WrongSpec.scala"), roots)

          Then("the selected paths are exclusive and real source and exact-anchor checks remain mandatory")
          missing shouldBe fixture.resolve("MissingSpec.scala")
          Files.isRegularFile(missing) shouldBe false
          intercept[NoSuchFileException] { _validate_scala_spec(missing, "required anchor") }
          wrong shouldBe wronganchor
          intercept[TestFailedException] { _validate_scala_spec(wrong, "required anchor") }
        }
      }
    }
  }

  private def _sibling_source_roots: Vector[(Path, String)] = Vector(
    Path.of("../../dev2025/cozy") -> "TEXTUS_CBD_SUPPORT_COVERAGE_COZY_ROOT",
    Path.of("../sbt-cozy") -> "TEXTUS_CBD_SUPPORT_COVERAGE_SBT_COZY_ROOT",
    Path.of("../textus-ai") -> "TEXTUS_CBD_SUPPORT_COVERAGE_TEXTUS_AI_ROOT"
  )

  private def _physical_source_path(kind: String, path: Path, roots: Map[String, String]): Path = {
    if (kind != "sibling-scala-spec") path
    else _sibling_source_roots.find { case (prefix, _) => path.startsWith(prefix) } match {
      case Some((prefix, variable)) => roots.get(variable) match {
        case None => path
        case Some(value) =>
          require(value.trim.nonEmpty, s"$variable must select a nonempty source root")
          val root = Path.of(value)
          require(root.isAbsolute && root.normalize == root, s"$variable must be a normalized absolute path")
          require(Files.isDirectory(root), s"$variable must select an existing directory")
          root.resolve(prefix.relativize(path))
      }
      case None => path
    }
  }

  private def _with_resolver_fixture(body: Path => Unit): Unit = {
    val target = Files.createDirectories(Path.of("target").toAbsolutePath.normalize)
    val fixture = Files.createTempDirectory(target, "phase5-resolver-only-")
    try body(fixture)
    finally {
      val paths = Files.walk(fixture)
      try paths.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
      finally paths.close()
    }
  }

  private val _coverage_path = Path.of("docs", "spec", "phase-5-executable-coverage.json")
  private val _checklist_path = Path.of("docs", "phase", "phase-5-checklist.md")
  private val _expected_ids = Vector(
    "P5-01", "P5-02", "P5-03", "P5-04",
    "P5-10", "P5-11", "P5-12", "P5-13", "P5-14",
    "P5-20", "P5-21", "P5-22", "P5-23", "P5-24",
    "P5-30", "P5-31", "P5-32", "P5-33", "P5-34", "P5-35",
    "P5-40", "P5-41", "P5-42", "P5-43", "P5-44", "P5-45",
    "P5-50", "P5-51", "P5-52", "P5-53", "P5-54", "P5-55"
  )
  private val _expected_areas = Set("contract", "review-core", "provider", "ci-bridge", "surface", "quality-ai-runtime")
  private val _expected_area_by_id = _expected_ids.map { id =>
    val area = id match {
      case value if value < "P5-10" => "contract"
      case value if value < "P5-20" => "review-core"
      case value if value < "P5-30" => "provider"
      case value if value < "P5-40" => "ci-bridge"
      case value if value < "P5-50" => "surface"
      case _ => "quality-ai-runtime"
    }
    id -> area
  }.toMap

  private def _behavioral_checklist_ids: Vector[String] = {
    val text = Files.readString(_checklist_path)
    val behavioral = text.split("## Documentation, Verification, and Closure", 2).head
    "`(P5-[0-9]{2})`".r.findAllMatchIn(behavioral).map(_.group(1)).toVector
  }

  private def _load_coverage: Vector[CoverageItem] = {
    val json = parse(Files.readString(_coverage_path)).fold(error => fail(error.message), identity)
    val cursor = json.hcursor
    cursor.get[String]("schemaVersion").toOption shouldBe Some("textus-cbd-support.phase-5-executable-coverage.v1")
    cursor.get[Vector[Json]]("items").fold(error => fail(error.message), identity).map(_coverage_item)
  }

  private def _coverage_item(json: Json): CoverageItem = {
    val cursor = json.hcursor
    CoverageItem(
      cursor.get[String]("id").fold(error => fail(error.message), identity),
      cursor.get[String]("area").fold(error => fail(error.message), identity),
      cursor.get[Vector[Json]]("evidence").fold(error => fail(error.message), identity).map(_coverage_evidence)
    )
  }

  private def _coverage_evidence(json: Json): CoverageEvidence = {
    val cursor = json.hcursor
    CoverageEvidence(
      cursor.get[String]("kind").fold(error => fail(error.message), identity),
      cursor.get[String]("path").fold(error => fail(error.message), identity),
      cursor.get[String]("anchor").fold(error => fail(error.message), identity)
    )
  }

  private def _validate_scala_spec(path: Path, anchor: String): Unit = {
    path.toString should endWith(".scala")
    Files.readString(path) should include(s"\"$anchor\"")
  }

  private final case class CoverageItem(id: String, area: String, evidence: Vector[CoverageEvidence])
  private final case class CoverageEvidence(evidencekind: String, sourcepath: String, anchor: String)
}
