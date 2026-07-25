package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

final class CarReviewEvolutionProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review evolution projection" should {
    "compare retained Reports only within one lineage and compatible configuration" in {
      Given("two canonical Reports for successive CAR versions in one compatible lineage")
      val baseline = _entry(_report, "lineage-cbd", "configuration-a")
      val currentreport = CarReviewReportCodec.withCalculatedDigest(_report.copy(
        reportId = ReviewReportId("report-evolution-current"),
        target = _report.target.copy(version = Some(ReviewVersion("0.2.0")), digest = _digest('c')),
        gate = _report.gate.copy(result = ReviewGateResult("fail")),
        assessments = _report.assessments.map(_.copy(maturity = ReviewMaturity("verified")))
      )).fold(error => fail(error.message), identity)

      When("the immutable snapshots are projected")
      val result = CarReviewEvolutionProjection.compare(baseline, _entry(currentreport, "lineage-cbd", "configuration-a"))

      Then("the View retains identities and reports only canonical deltas")
      CarReviewReportCodec.encode(currentreport).isRight shouldBe true
      result.toOption.map(_.baselineReportId) shouldBe Some(_report.reportId)
      result.toOption.map(_.currentReportId) shouldBe Some(currentreport.reportId)
      result.toOption.map(_.baselineTarget.digest) shouldBe Some(_report.target.digest)
      result.toOption.map(_.currentTarget.digest) shouldBe Some(currentreport.target.digest)
      result.toOption.map(_.baselineGate) shouldBe Some(_report.gate.result)
      result.toOption.map(_.currentGate) shouldBe Some(ReviewGateResult("fail"))
      result.toOption.map(_.unchangedObservationIds).get should contain theSameElementsAs _report.observations.map(_.id)
      result.toOption.map(_.changedCapabilityIds).get should contain theSameElementsAs _report.assessments.map(_.capabilityId)
    }

    "refuse cross-lineage, configuration-incompatible, and different-CAR comparisons" in {
      Given("one baseline and three incompatible retained snapshot candidates")
      val baseline = _entry(_report, "lineage-cbd", "configuration-a")
      val changedlineage = _entry(_report, "lineage-other", "configuration-a")
      val changedconfiguration = _entry(_report, "lineage-cbd", "configuration-b")
      val changedcar = _entry(_report.copy(target = _report.target.copy(name = "other-car")), "lineage-cbd", "configuration-a")

      When("each invalid comparison is projected")
      Then("the View refuses rather than manufacturing a cross-CAR delta")
      CarReviewEvolutionProjection.compare(baseline, changedlineage).left.toOption.map(_.code) shouldBe Some("review-history-lineage-mismatch")
      CarReviewEvolutionProjection.compare(baseline, changedconfiguration).left.toOption.map(_.code) shouldBe Some("review-history-configuration-incompatible")
      CarReviewEvolutionProjection.compare(baseline, changedcar).left.toOption.map(_.code) shouldBe Some("review-history-target-identity-mismatch")
    }

    "retain added and removed Finding and capability identities across compatible CAR evolution" in {
      Given("two immutable compatible snapshots with independently added and removed review material")
      val sourceobservation = _report.observations.head
      val sourceassessment = _report.assessments.head
      val baseline = CarReviewReportCodec.withCalculatedDigest(_report.copy(
        reportId = ReviewReportId("report-evolution-baseline-delta"),
        target = _report.target.copy(version = Some(ReviewVersion("0.1.0")), digest = _digest('d')),
        observations = _report.observations :+ sourceobservation.copy(id = ReviewObservationId("finding-baseline-only")),
        assessments = _report.assessments :+ sourceassessment.copy(capabilityId = ReviewCapabilityId("quality.documentation.rationale"))
      )).fold(error => fail(error.message), identity)
      val current = CarReviewReportCodec.withCalculatedDigest(_report.copy(
        reportId = ReviewReportId("report-evolution-current-delta"),
        target = _report.target.copy(version = Some(ReviewVersion("0.2.0")), digest = _digest('e')),
        observations = _report.observations :+ sourceobservation.copy(id = ReviewObservationId("finding-current-only")),
        assessments = _report.assessments :+ sourceassessment.copy(capabilityId = ReviewCapabilityId("quality.testability"))
      )).fold(error => fail(error.message), identity)

      When("CBD compares the two retained compatible histories")
      val delta = CarReviewEvolutionProjection.compare(_entry(baseline, "lineage-cbd", "configuration-a"), _entry(current, "lineage-cbd", "configuration-a")).fold(error => fail(error.message), identity)

      Then("the View retains distinct version/digest identities and does not collapse added or removed material")
      delta.baselineTarget.version shouldBe Some(ReviewVersion("0.1.0"))
      delta.currentTarget.version shouldBe Some(ReviewVersion("0.2.0"))
      delta.addedObservationIds should contain(ReviewObservationId("finding-current-only"))
      delta.removedObservationIds should contain(ReviewObservationId("finding-baseline-only"))
      delta.addedCapabilityIds should contain(ReviewCapabilityId("quality.testability"))
      delta.removedCapabilityIds should contain(ReviewCapabilityId("quality.documentation.rationale"))
    }
  }

  private def _entry(report: CarReviewReport, lineage: String, configuration: String): CarReviewHistoryEntry =
    CarReviewHistoryEntry(CarReviewLineageId(lineage), CarReviewConfigurationCompatibilityId(configuration), report)

  private val _report =
    CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)

  private def _digest(character: Char): ReviewDigest =
    ReviewDigest("sha256:" + character.toString * 64)
}
