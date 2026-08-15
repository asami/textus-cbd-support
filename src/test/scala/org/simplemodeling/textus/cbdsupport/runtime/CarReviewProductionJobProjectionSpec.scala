package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.context.{ExecutionContext, SecurityContext}
import org.goldenport.cncf.job.{InMemoryJobEngine, JobId, JobStatus}
import org.goldenport.protocol.operation.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewProductionJobProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The production Review Job projection" should {
    "reject a validly encoded response whose created-at no longer equals its execution completion" in {
      Given("one completed production Job model and its strict persisted result")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = CarReviewProductionExecution.create(_request).fold(_fail, identity)
      val binding = CarReviewProductionJobBinding.from("diagnosis-projection-created-at", execution).fold(_fail, identity)
      val gateway = new CncfCarReviewJobGateway(engine)

      try {
        When("the canonical document changes only its valid report created-at instant")
        val jobid = gateway.submit(binding, execution).fold(_fail, identity)
        engine.drainAll()
        val model = engine.query(JobId.parse(jobid.value).fold(_fail, identity)).getOrElse(fail("Expected completed Job model."))
        val record = model.result.collect { case OperationResponse.RecordResponse(value) => value }
          .getOrElse(fail("Expected strict result record."))
        val response = CarReviewProductionJobProjection.decodeResult(record, binding).fold(_fail, identity)
        val document = CarReviewReportCodec.encode(response.report.copy(
          createdAt = ReviewInstant(java.time.Instant.parse(response.report.execution.completedAt.value).plusSeconds(1).toString)
        )).fold(_fail, identity)
        val tampered = model.copy(result = Some(OperationResponse(record.upsertSingle("reportDocument", document))))
        val discovered = CarReviewProductionJobProjection.discover(tampered, binding)

        Then("strict result validation refuses the timestamp mismatch before a lease can be issued")
        discovered.fold(error => error.toString should include ("review-job-result-report-mismatch"), _ => fail("Unexpected terminal lease."))
      } finally {
        engine.shutdown()
      }
    }

    "reject result-bearing nonterminal and failed or cancelled job statuses before projection" in {
      Given("one completed production Job result used only as an unexpected payload")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = CarReviewProductionExecution.create(_request.copy(reviewId = ReviewId("review-projection-status"))).fold(_fail, identity)
      val binding = CarReviewProductionJobBinding.from("diagnosis-projection-status", execution).fold(_fail, identity)
      val gateway = new CncfCarReviewJobGateway(engine)

      try {
        When("Submitted, Running, Suspended, Failed, and Cancelled models carry an unexpected result record")
        val jobid = gateway.submit(binding, execution).fold(_fail, identity)
        engine.drainAll()
        val model = engine.query(JobId.parse(jobid.value).fold(_fail, identity)).getOrElse(fail("Expected completed Job model."))
        val discovered = Vector(
          JobStatus.Submitted,
          JobStatus.Running,
          JobStatus.Suspended,
          JobStatus.Failed,
          JobStatus.Cancelled
        ).map(status => CarReviewProductionJobProjection.discover(model.copy(status = status), binding))

        Then("every status mismatch is rejected before any run or terminal lease is exposed")
        discovered.foreach(_.fold(
          error => error.toString should include ("review-job-result-status-mismatch"),
          _ => fail("Unexpected projected run or terminal lease.")
        ))
      } finally {
        engine.shutdown()
      }
    }
  }

  private val _request = ReviewStartRequest(
    ReviewId("review-projection-created-at"),
    ReviewTarget(
      ReviewTargetKind("car"),
      Some("org.simplemodeling"),
      "textus-cbd-support",
      Some(ReviewVersion("0.1.0-SNAPSHOT")),
      ReviewDigest("sha256:" + ("a" * 64))
    ),
    ReviewProfile("development"),
    ReviewInstant("2026-08-15T00:00:00Z")
  )

  private def _fail(error: Any): Nothing = fail(error.toString)
}
