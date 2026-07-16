package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
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
          Files.isRegularFile(path) shouldBe true
          entry.evidencekind match {
            case "scala-spec" => _validate_scala_spec(path, entry.anchor)
            case "sibling-scala-spec" =>
              path.startsWith(Path.of("..")) shouldBe true
              _validate_scala_spec(path, entry.anchor)
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
