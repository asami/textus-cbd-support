package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class Phase4ExecutableCoverageSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The Phase 4 executable coverage manifest" should {
    "cover every behavioral checklist item exactly once" in {
      Given("the authoritative Phase 4 checklist and executable coverage manifest")
      val checklistids = _behavioral_checklist_ids
      val coverage = _load_coverage

      When("the behavioral contract IDs and coverage entries are compared")
      val coverageids = coverage.map(_.id)

      Then("all authentication, refresh/cache, composition, and compatibility IDs are covered once")
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

      Then("Scala scenarios and executable gates exist at their declared anchors")
      evidence.foreach { case (id, entry) =>
        val path = Path.of(entry.sourcepath)
        withClue(s"$id ${entry.sourcepath}: ") {
          path.isAbsolute shouldBe false
          path.normalize shouldBe path
          Files.isRegularFile(path) shouldBe true
          entry.evidencekind match {
            case "scala-spec" =>
              path.startsWith(Path.of("src", "test", "scala")) shouldBe true
              path.toString should endWith(".scala")
              Files.readString(path) should include(s"\"${entry.anchor}\" in {")
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

  private val _coverage_path = Path.of("docs", "spec", "phase-4-executable-coverage.json")
  private val _checklist_path = Path.of("docs", "phase", "phase-4-checklist.md")
  private val _expected_ids = Vector(
    "P4-01", "P4-02", "P4-03", "P4-04",
    "P4-10", "P4-11", "P4-12", "P4-13",
    "P4-20", "P4-21", "P4-22",
    "P4-30", "P4-31", "P4-32"
  )
  private val _expected_areas = Set("authentication", "refresh-cache", "sar-composition", "compatibility")
  private val _expected_area_by_id = Map(
    "P4-01" -> "authentication",
    "P4-02" -> "authentication",
    "P4-03" -> "authentication",
    "P4-04" -> "authentication",
    "P4-10" -> "refresh-cache",
    "P4-11" -> "refresh-cache",
    "P4-12" -> "refresh-cache",
    "P4-13" -> "refresh-cache",
    "P4-20" -> "sar-composition",
    "P4-21" -> "sar-composition",
    "P4-22" -> "sar-composition",
    "P4-30" -> "compatibility",
    "P4-31" -> "compatibility",
    "P4-32" -> "compatibility"
  )

  private def _behavioral_checklist_ids: Vector[String] = {
    val text = Files.readString(_checklist_path)
    val behavioral = text.split("## Documentation, Release, and Verification", 2).head
    "`(P4-[0-9]{2})`".r.findAllMatchIn(behavioral).map(_.group(1)).toVector
  }

  private def _load_coverage: Vector[CoverageItem] = {
    val json = parse(Files.readString(_coverage_path)).fold(
      error => fail(s"Invalid Phase 4 executable coverage JSON: ${error.message}"),
      identity
    )
    val cursor = json.hcursor
    cursor.get[String]("schemaVersion").toOption shouldBe Some("textus-cbd-support.phase-4-executable-coverage.v1")
    val items = cursor.get[Vector[Json]]("items").fold(
      error => fail(s"Invalid Phase 4 executable coverage items: ${error.message}"),
      identity
    )
    items.map(_coverage_item)
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

  private final case class CoverageItem(
    id: String,
    area: String,
    evidence: Vector[CoverageEvidence]
  )

  private final case class CoverageEvidence(
    evidencekind: String,
    sourcepath: String,
    anchor: String
  )
}
