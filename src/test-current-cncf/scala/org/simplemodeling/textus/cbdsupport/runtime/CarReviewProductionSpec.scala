package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Files
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.entity.{ReviewJobBinding, ReviewReportSnapshot, ReviewAttestationSnapshot}

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewProductionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import CarReviewJobExecutionFixtures.{value as accepted, failure}

  "Generated production Review operations" should {
    "select server work and join or reuse the exact durable Job (CPR1)" in {
      Given("a complete bootstrapped CBD runtime, SQLite and a registered Cozy process capability")
      val root = Files.createTempDirectory(Files.createDirectories(java.nio.file.Path.of("target/cbd-production")), "runtime-").toAbsolutePath
      val fixture = new CarReviewProductionFixture(root)
      try {
        When("the actual generated StartReview operation commits and is repeated before and after native dispatch")
        val first = accepted(fixture.start())
        val joined = accepted(fixture.start())
        joined.getString("reviewId") shouldBe first.getString("reviewId")
        joined.getString("jobId") shouldBe first.getString("jobId")
        fixture.executions shouldBe 0
        accepted(fixture.native.drain())
        val completed = accepted(fixture.get(first.getString("reviewId").get))
        val reused = accepted(fixture.start())

        Then("one Provider execution and one binding retain canonical Report and attestation identities")
        completed.getString("state") shouldBe Some("completed")
        reused.getString("reviewId") shouldBe first.getString("reviewId")
        reused.getString("reportId") shouldBe completed.getString("reportId")
        accepted(fixture.report(completed.getString("reportId").get))
        fixture.executions shouldBe 1
        fixture.count[ReviewJobBinding](ReviewJobBinding.collectionId) shouldBe 1
        fixture.count[ReviewReportSnapshot](ReviewReportSnapshot.collectionId) shouldBe 1
        fixture.count[ReviewAttestationSnapshot](ReviewAttestationSnapshot.collectionId) shouldBe 1

        And("another operator and an unprivileged owner cannot read, cancel or retrieve the Report")
        val reviewid = first.getString("reviewId").get
        failure(fixture.get(reviewid, fixture.context("other-operator"))) should include("review-job-not-owned")
        failure(fixture.cancel(reviewid, fixture.context("other-operator"))) should include("review-job-not-owned")
        failure(fixture.report(completed.getString("reportId").get, fixture.context("other-operator"))) should include("review-job-not-owned")
        failure(fixture.get(reviewid, fixture.context(permitted = false))) should include("required role: admin|operator|reviewer|viewer")
        fixture.executions shouldBe 1
      } finally fixture.close()
    }

    "refuse missing and malformed server configuration before dispatch (CPR2)" in {
      Given("an authorized generated request with no server execution plan")
      val root = Files.createTempDirectory(Files.createDirectories(java.nio.file.Path.of("target/cbd-production")), "refusal-").toAbsolutePath
      val fixture = new CarReviewProductionFixture(root)
      try {
        fixture.configure(None)
        When("StartReview selects its server-owned work")
        failure(fixture.start()) should include("review-execution-plan-required")
        fixture.configure(Some("{}"))
        failure(fixture.start()) should include("review-server-plan-invalid")
        Then("no binding, Job dispatch or Report is created")
        fixture.executions shouldBe 0
        fixture.count[ReviewJobBinding](ReviewJobBinding.collectionId) shouldBe 0
        fixture.count[ReviewReportSnapshot](ReviewReportSnapshot.collectionId) shouldBe 0
      } finally fixture.close()
    }
  }
}
