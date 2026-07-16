package org.simplemodeling.textus.cbdsupport

import org.goldenport.Consequence
import org.goldenport.cncf.context.{ExecutionContext, SecurityContext}
import org.goldenport.cncf.job.{InMemoryJobEngine, JobId, JobStatus}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewRunApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CBD-owned Review Run application" should {
    "bind one admitted Review ID to one submitted CNCF Job ID" in {
      Given("a reviewer, a valid digest-bound target, and a recording CNCF Job gateway")
      given ExecutionContext = ExecutionContext.test()
      val application = new CarReviewRunApplication()
      val gateway = new FakeJobGateway()

      When("the reviewer starts the Review")
      val result = application.start(_request, Set("reviewer"), gateway)

      Then("the Run is queued and retains the exact Review-to-Job binding")
      result.toOption.map(_.run.state.value) shouldBe Some("queued")
      result.toOption.map(_.binding) shouldBe Some(ReviewRunJobBinding(
        ReviewId("review-application-001"),
        ReviewJobId("cncf-job-application-001")
      ))
      gateway.submissions shouldBe Vector(_request)
    }

    "project authorized Job progress and canonical completion without changing the binding" in {
      Given("one queued Review whose Job advances through running and succeeded states")
      given ExecutionContext = ExecutionContext.test()
      val application = new CarReviewRunApplication()
      val gateway = new FakeJobGateway()
      application.start(_request, Set("operator"), gateway).isSuccess shouldBe true

      When("authorized viewers read running progress and then the canonical report completion")
      gateway.update = Some(ReviewRunJobUpdate(
        JobStatus.Running,
        ReviewInstant("2026-07-16T00:00:01Z")
      ))
      val running = application.get(_request.reviewId, Set("viewer"), gateway)
      gateway.update = Some(ReviewRunJobUpdate(
        JobStatus.Succeeded,
        ReviewInstant("2026-07-16T00:00:02Z"),
        completion = Some(ReviewRunCompletion(
          ReviewReportId("report-application-001"),
          ReviewDigest("sha256:" + "b" * 64)
        ))
      ))
      val completed = application.get(_request.reviewId, Set("reviewer"), gateway)

      Then("the same application Run exposes progress and one immutable report identity")
      running.toOption.map(_.run.state.value) shouldBe Some("running")
      completed.toOption.map(_.run.state.value) shouldBe Some("completed")
      completed.toOption.flatMap(_.run.reportId).map(_.value) shouldBe Some("report-application-001")
      completed.toOption.map(_.binding.jobId.value) shouldBe Some("cncf-job-application-001")
    }

    "propagate authorized cancellation intent to the bound CNCF Job" in {
      Given("one running Review and an operator")
      given ExecutionContext = ExecutionContext.test()
      val application = new CarReviewRunApplication()
      val gateway = new FakeJobGateway()
      application.start(_request, Set("reviewer"), gateway).isSuccess shouldBe true
      gateway.update = Some(ReviewRunJobUpdate(
        JobStatus.Running,
        ReviewInstant("2026-07-16T00:00:01Z")
      ))
      application.get(_request.reviewId, Set("viewer"), gateway).isSuccess shouldBe true

      When("the operator cancels and a later read observes CNCF terminal cancellation")
      val cancelling = application.cancel(
        _request.reviewId,
        Set("operator"),
        ReviewInstant("2026-07-16T00:00:02Z"),
        gateway
      )
      gateway.update = Some(ReviewRunJobUpdate(
        JobStatus.Cancelled,
        ReviewInstant("2026-07-16T00:00:03Z")
      ))
      val cancelled = application.get(_request.reviewId, Set("viewer"), gateway)

      Then("cancelling stays visible before terminal cancellation and the exact Job is controlled")
      cancelling.toOption.map(_.run.state.value) shouldBe Some("cancelling")
      gateway.cancellations shouldBe Vector(ReviewJobId("cncf-job-application-001"))
      cancelled.toOption.map(_.run.state.value) shouldBe Some("cancelled")
    }

    "deny start, read, and cancellation before touching the CNCF Job boundary" in {
      Given("callers without the action-specific P5-04 roles")
      given ExecutionContext = ExecutionContext.test()
      val application = new CarReviewRunApplication()
      val gateway = new FakeJobGateway()

      When("a viewer starts, an unrelated role reads, and a reviewer cancels")
      val deniedstart = application.start(_request, Set("viewer"), gateway)
      val admitted = application.start(_request, Set("reviewer"), gateway)
      val deniedread = application.get(_request.reviewId, Set("guest"), gateway)
      val deniedcancel = application.cancel(
        _request.reviewId,
        Set("reviewer"),
        ReviewInstant("2026-07-16T00:00:01Z"),
        gateway
      )

      Then("all unauthorized actions fail and only the authorized submission reaches the gateway")
      deniedstart.isSuccess shouldBe false
      admitted.isSuccess shouldBe true
      deniedread.isSuccess shouldBe false
      deniedcancel.isSuccess shouldBe false
      gateway.submissions shouldBe Vector(_request)
      gateway.reads shouldBe Vector.empty
      gateway.cancellations shouldBe Vector.empty
    }

    "submit and cancel the exact bound Job through the CNCF runtime" in {
      Given("an operator and an in-memory CNCF Job engine whose worker is held")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val gateway = new CncfCarReviewJobGateway(engine)
      val application = new CarReviewRunApplication()
      val roles = CarReviewAuthorization.roles(summon[ExecutionContext])

      try {
        When("the application starts and cancels one Review through the production gateway")
        val admitted = application.start(_request, roles, gateway)
        val cancelling = application.cancel(
          _request.reviewId,
          roles,
          ReviewInstant("2026-07-16T00:00:01Z"),
          gateway
        )
        val cancelled = application.get(_request.reviewId, roles, gateway)

        Then("the generated CNCF Job identity remains bound and reaches terminal cancellation")
        admitted.toOption.map(admission => JobId.parse(admission.binding.jobId.value).isSuccess) shouldBe Some(true)
        cancelling.toOption.map(_.run.state.value) shouldBe Some("cancelling")
        cancelled.toOption.map(_.run.state.value) shouldBe Some("cancelled")
        cancelled.toOption.map(_.binding) shouldBe admitted.toOption.map(_.binding)
      } finally {
        engine.shutdown()
      }
    }
  }

  private def _request: ReviewStartRequest = ReviewStartRequest(
    ReviewId("review-application-001"),
    ReviewTarget(
      ReviewTargetKind("car"),
      Some("org.simplemodeling"),
      "textus-cbd-support",
      Some(ReviewVersion("0.1.0-SNAPSHOT")),
      ReviewDigest("sha256:" + "a" * 64)
    ),
    ReviewProfile("development"),
    ReviewInstant("2026-07-16T00:00:00Z")
  )

  private final class FakeJobGateway extends CarReviewJobGateway {
    private var _submissions = Vector.empty[ReviewStartRequest]
    private var _reads = Vector.empty[ReviewJobId]
    private var _cancellations = Vector.empty[ReviewJobId]
    var update: Option[ReviewRunJobUpdate] = Some(ReviewRunJobUpdate(
      JobStatus.Submitted,
      ReviewInstant("2026-07-16T00:00:00Z")
    ))

    def submissions: Vector[ReviewStartRequest] = _submissions
    def reads: Vector[ReviewJobId] = _reads
    def cancellations: Vector[ReviewJobId] = _cancellations

    def submit(request: ReviewStartRequest)(using ExecutionContext): Consequence[ReviewJobId] = {
      _submissions = _submissions :+ request
      Consequence.success(ReviewJobId("cncf-job-application-001"))
    }

    def read(jobid: ReviewJobId)(using ExecutionContext): Consequence[Option[ReviewRunJobUpdate]] = {
      _reads = _reads :+ jobid
      Consequence.success(update)
    }

    def cancel(jobid: ReviewJobId)(using ExecutionContext): Consequence[Unit] = {
      _cancellations = _cancellations :+ jobid
      Consequence.unit
    }
  }
}
