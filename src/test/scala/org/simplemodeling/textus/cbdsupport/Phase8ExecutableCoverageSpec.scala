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
final class Phase8ExecutableCoverageSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The Phase 8 executable coverage manifest" should {
    "cover every completed behavioral checklist item exactly once" in {
      Given("the authoritative Phase 8 checklist and executable coverage manifest")
      val checklistids = _completed_checklist_ids
      val coverage = _load_coverage

      When("completed behavioral IDs and coverage entries are compared")
      val coverageids = coverage.map(_.id)

      Then("every completed delivery, Web, and artifact item has exactly one navigable evidence entry")
      coverageids.distinct shouldBe coverageids
      coverageids.toSet shouldBe checklistids.toSet
      coverage.foreach { item =>
        item.evidence should not be empty
        item.evidence.foreach { entry =>
          val path = Path.of(entry.sourcepath)
          path.isAbsolute shouldBe false
          path.normalize shouldBe path
          path.toString should endWith(".scala")
          Files.isRegularFile(path) shouldBe true
          Files.readString(path) should include(s"\"${entry.anchor}\"")
        }
      }
    }
  }

  private val _coverage_path = Path.of("docs", "spec", "phase-8-executable-coverage.json")
  private val _checklist_path = Path.of("docs", "phase", "phase-8-checklist.md")

  private def _completed_checklist_ids: Vector[String] =
    "- \\[x\\] `?(P8-[0-9]{2})`?".r.findAllMatchIn(Files.readString(_checklist_path)).map(_.group(1)).toVector

  private def _load_coverage: Vector[CoverageItem] = {
    val json = parse(Files.readString(_coverage_path)).fold(error => fail(error.message), identity)
    json.hcursor.get[String]("schemaVersion").toOption shouldBe Some("textus-cbd-support.phase-8-executable-coverage.v1")
    json.hcursor.get[Vector[Json]]("items").fold(error => fail(error.message), identity).map { value =>
      val cursor = value.hcursor
      CoverageItem(
        cursor.get[String]("id").fold(error => fail(error.message), identity),
        cursor.get[Vector[Json]]("evidence").fold(error => fail(error.message), identity).map { evidence =>
          val entry = evidence.hcursor
          CoverageEvidence(
            entry.get[String]("kind").fold(error => fail(error.message), identity),
            entry.get[String]("path").fold(error => fail(error.message), identity),
            entry.get[String]("anchor").fold(error => fail(error.message), identity)
          )
        }
      )
    }
  }

  private final case class CoverageItem(id: String, evidence: Vector[CoverageEvidence])
  private final case class CoverageEvidence(evidencekind: String, sourcepath: String, anchor: String)
}
