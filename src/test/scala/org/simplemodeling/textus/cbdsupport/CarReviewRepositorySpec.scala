package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewRepositorySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review repository" should {
    "retain an immutable canonical report and reject a conflicting reuse of its report ID" in {
      Given("one valid canonical report and one changed report sharing its ID")
      val repository = new CarReviewRepository()
      val report = _report
      val conflicting = CarReviewReportCodec.withCalculatedDigest(report.copy(gate = report.gate.copy(reasons = Vector("changed")))).fold(_fail, identity)

      When("both reports are retained")
      val first = repository.retain(report)
      val second = repository.retain(conflicting)

      Then("only the original immutable content is accepted")
      first.toOption shouldBe Some(report)
      second.left.toOption.map(_.code) shouldBe Some("immutable-report-conflict")
    }

    "retain a completed Review Run and report only when their complete attribution agrees" in {
      Given("the matching canonical completed Run and Report")
      val repository = new CarReviewRepository()

      When("the completed pair is retained atomically")
      val result = repository.retain(_run, _report)

      Then("the immutable binding retains every gate-relevant identity")
      result shouldBe Right(CarReviewRunReportBinding(_run.reviewId, _run.target, _report.reportId, _report.reportDigest, _report.gate.result))
      repository.binding(_run.reviewId) shouldBe result.toOption
    }

    "reject stale gate evidence after a retained report is deleted" in {
      Given("one retained completed Review pair and exact gate evidence")
      val repository = new CarReviewRepository()
      repository.retain(_run, _report).isRight shouldBe true
      val evidence = CarReviewGateEvidence(_run.reviewId, _report.target, _report.reportId, _report.reportDigest, _report.gate.result)

      When("the report is deleted and the old evidence is offered")
      val deletion = repository.delete(_report.reportId, ReviewInstant("2026-07-17T00:00:00Z"))
      val validation = repository.validateGateEvidence(evidence)

      Then("deletion has no report content audit and the stale gate is rejected")
      deletion.toOption.map(_.action) shouldBe Some(CarReviewRetentionAudit.DELETED)
      deletion.toOption.flatMap(_.reportDigest) shouldBe Some(_report.reportDigest)
      validation.left.toOption.map(_.code) shouldBe Some("stale-gate-evidence")
    }

    "enforce finite per-target retention without silently deleting immutable reports" in {
      Given("a repository that retains one report per target")
      val repository = new CarReviewRepository(CarReviewRetentionPolicy(30, 1, 2))
      val alternate = CarReviewReportCodec.withCalculatedDigest(_report.copy(reportId = ReviewReportId("report-example-002"))).fold(_fail, identity)
      repository.retain(_report) shouldBe Right(_report)

      When("a second report for the same target is retained")
      val result = repository.retain(alternate)

      Then("the explicit capacity failure preserves the earlier report")
      result.left.toOption.map(_.code) shouldBe Some("retention-capacity-exceeded")
      repository.compare(_report, _report.reportId).toOption.map(_.reportId) shouldBe Some(_report.reportId)
    }

    "enforce finite terminal Run retention for each target" in {
      Given("a repository that retains one terminal Run per target")
      val repository = new CarReviewRepository(CarReviewRetentionPolicy(30, 2, 1))
      val alternate = _run.copy(reviewId = ReviewId("review-example-002"))
      repository.retain(_run) shouldBe Right(_run)

      When("a second terminal Run for the target is retained")
      val result = repository.retain(alternate)

      Then("the explicit Run capacity failure preserves the first Run")
      result.left.toOption.map(_.code) shouldBe Some("run-retention-capacity-exceeded")
      repository.retain(_run) shouldBe Right(_run)
    }

    "expire reports from an injected retention time and retain only a content-free audit record" in {
      Given("a report older than the finite configured retention age")
      val repository = new CarReviewRepository(CarReviewRetentionPolicy(1, 2, 2))
      val old = _report
      repository.retain(old) shouldBe Right(old)

      When("the repository receives a deterministic retention time")
      val expired = repository.purgeExpired(ReviewInstant("2026-07-18T00:00:00Z"))

      Then("the report expires and the audit preserves attribution without report content")
      expired.toOption.flatMap(_.headOption).map(_.action) shouldBe Some(CarReviewRetentionAudit.EXPIRED)
      expired.toOption.flatMap(_.headOption).flatMap(_.reportDigest) shouldBe Some(old.reportDigest)
      repository.compare(old, old.reportId).left.toOption.map(_.code) shouldBe Some("baseline-not-found")
    }

    "reject an invalid retention policy before expiry can remove immutable records" in {
      Given("a repository configured with an unbounded retention age")
      val repository = new CarReviewRepository(CarReviewRetentionPolicy(0, 2, 2))

      When("the retention scheduler requests an expiry pass")
      val result = repository.purgeExpired(ReviewInstant("2026-07-18T00:00:00Z"))

      Then("no retention mutation occurs under the invalid policy")
      result.left.toOption.map(_.code) shouldBe Some("invalid-retention-policy")
      repository.retentionAudit shouldBe empty
    }

    "expire terminal Runs and their gate bindings from an injected retention time" in {
      Given("one retained completed Review pair")
      val repository = new CarReviewRepository(CarReviewRetentionPolicy(1, 2, 2))
      repository.retain(_run, _report).isRight shouldBe true

      When("the finite retention age passes")
      val expired = repository.purgeExpired(ReviewInstant("2026-07-18T00:00:00Z"))

      Then("the Run and Report records are audited and the binding is removed")
      expired.toOption.map(_.map(_.recordType).toSet) shouldBe Some(Set(CarReviewRetentionAudit.REPORT, CarReviewRetentionAudit.RUN))
      repository.binding(_run.reviewId) shouldBe None
    }

    "compare only retained reports with the exact same target attribution" in {
      Given("a retained baseline and one report with another target digest")
      val repository = new CarReviewRepository()
      repository.retain(_report) shouldBe Right(_report)
      val changed = CarReviewReportCodec.withCalculatedDigest(_report.copy(target = _report.target.copy(digest = ReviewDigest("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")))).fold(_fail, identity)

      When("the mismatched target requests a baseline comparison")
      val result = repository.compare(changed, _report.reportId)

      Then("comparison is rejected rather than producing stale gate evidence")
      result.left.toOption.map(_.code) shouldBe Some("baseline-target-mismatch")
    }
  }

  private val _report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
  private val _run = CarReviewRunCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-run-v1.json"))).fold(_fail, identity)
  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
