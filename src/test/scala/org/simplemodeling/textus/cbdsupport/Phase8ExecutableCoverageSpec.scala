package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class Phase8ExecutableCoverageSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The Phase 8 executable coverage manifest" should {
    "cover every completed Phase 8 checklist item once with a navigable executable anchor" in {
      Given("the Phase 8 checklist and executable-coverage manifest")
      val completed = _completed_checklist_ids
      val coverage = _load_coverage

      When("CBD resolves every recorded evidence anchor")

      Then("each completed item is covered exactly once by a readable executable specification")
      coverage.map(_.id).distinct shouldBe coverage.map(_.id)
      coverage.map(_.id).toSet shouldBe completed.toSet
      coverage.foreach { item =>
        item.evidence should not be empty
        item.evidence.foreach { evidence =>
          withClue(s"${item.id} ${evidence.path}: ") {
            val path = Path.of(evidence.path)
            path.isAbsolute shouldBe false
            path.normalize shouldBe path
            Files.isRegularFile(path) shouldBe true
            evidence.kind should (be("scala-spec") or be("sibling-scala-spec"))
            Files.readString(path) should include(s"\"${evidence.anchor}\"")
          }
        }
      }
    }
  }

  private val _coverage_path = Path.of("docs", "spec", "phase-8-executable-coverage.json")
  private val _checklist_path = Path.of("docs", "phase", "phase-8-checklist.md")

  private def _completed_checklist_ids: Vector[String] =
    "(?m)^- \\[x\\] `(P8-[0-9]{2})`".r
      .findAllMatchIn(Files.readString(_checklist_path))
      .map(_.group(1))
      .toVector

  private def _load_coverage: Vector[CoverageItem] = {
    val json = parse(Files.readString(_coverage_path)).fold(error => fail(error.message), identity)
    val cursor = json.hcursor
    cursor.get[String]("schemaVersion").toOption shouldBe Some("textus-cbd-support.phase-8-executable-coverage.v1")
    cursor.get[Vector[Json]]("items").fold(error => fail(error.message), identity).map { item =>
      val itemcursor = item.hcursor
      CoverageItem(
        itemcursor.get[String]("id").fold(error => fail(error.message), identity),
        itemcursor.get[Vector[Json]]("evidence").fold(error => fail(error.message), identity).map { evidence =>
          val evidencecursor = evidence.hcursor
          CoverageEvidence(
            evidencecursor.get[String]("kind").fold(error => fail(error.message), identity),
            evidencecursor.get[String]("path").fold(error => fail(error.message), identity),
            evidencecursor.get[String]("anchor").fold(error => fail(error.message), identity)
          )
        }
      )
    }
  }

  private final case class CoverageItem(id: String, evidence: Vector[CoverageEvidence])
  private final case class CoverageEvidence(kind: String, path: String, anchor: String)
}
