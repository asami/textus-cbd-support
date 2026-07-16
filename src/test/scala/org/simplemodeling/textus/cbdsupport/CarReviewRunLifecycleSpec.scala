package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.cncf.job.JobStatus
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewRunLifecycleSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The typed CAR Review Run lifecycle" should {
    "admit the canonical v1 wire document without collapsing lifecycle identities" in {
      Given("the representative completed Review Run from the P5-03 contract")
      val body = Files.readString(_run_path)

      When("the strict runtime codec decodes and re-encodes it")
      val run = CarReviewRunCodec.decode(body).fold(_fail_codec, identity)
      val encoded = CarReviewRunCodec.encode(run).fold(_fail_codec, identity)

      Then("Review, state, target, provider, limitation, report, and failure concepts stay typed")
      run.reviewId shouldBe ReviewId("review-example-001")
      run.state shouldBe ReviewRunState("completed")
      run.target.kind shouldBe ReviewTargetKind("project")
      run.providers.head.provider.id shouldBe ReviewProviderId("cozy")
      run.limitations.head.scope shouldBe ReviewLimitationScope("run")
      run.reportId shouldBe Some(ReviewReportId("report-example-001"))
      run.failureCode shouldBe None

      And("canonical encoding remains strict and re-admissible")
      CarReviewRunCodec.decode(encoded).fold(_fail_codec, identity) shouldBe run
    }

    "project admitted work through queued, running, and completed CNCF Job states" in {
      Given("an admitted Review Run and one future canonical report identity")
      val admitted = _admitted_run()
      val completion = ReviewRunCompletion(
        ReviewReportId("report-run-001"),
        ReviewDigest("sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
      )

      When("CNCF reports submission, execution, and successful settlement")
      val queued = _project(admitted, ReviewRunJobUpdate(JobStatus.Submitted, _instant(1)))
      val running = _project(queued, ReviewRunJobUpdate(JobStatus.Running, _instant(2)))
      val completed = _project(running, ReviewRunJobUpdate(
        JobStatus.Succeeded,
        _instant(3),
        completion = Some(completion)
      ))

      Then("the Review Run follows the same progress and binds exactly one report at completion")
      queued.state shouldBe ReviewRunState("queued")
      running.state shouldBe ReviewRunState("running")
      completed.state shouldBe ReviewRunState("completed")
      completed.completedAt shouldBe Some(_instant(3))
      completed.reportId shouldBe Some(completion.reportId)
      completed.reportDigest shouldBe Some(completion.reportDigest)
      CarReviewRunCodec.validate(completed) shouldBe Right(())

      And("repeated CNCF readback is idempotent while terminal content stays immutable")
      _project(completed, ReviewRunJobUpdate(
        JobStatus.Succeeded,
        _instant(4),
        completion = Some(completion)
      )) shouldBe completed
      CarReviewRunLifecycle.projectJob(completed, ReviewRunJobUpdate(
        JobStatus.Succeeded,
        _instant(4),
        completion = Some(completion),
        limitations = Vector(ReviewLimitation(
          "late-change",
          ReviewLimitationScope("run"),
          None,
          "Terminal content cannot be changed by later polling.",
          retryable = false
        ))
      )).left.map(_.code) shouldBe Left("terminal-run")
    }

    "preserve cancellation intent and attributable limitations through terminal cancellation" in {
      Given("a running Review whose CNCF Job can still be cancelled")
      val queued = _project(_admitted_run(), ReviewRunJobUpdate(JobStatus.Submitted, _instant(1)))
      val running = _project(queued, ReviewRunJobUpdate(JobStatus.Running, _instant(2)))
      val limitation = ReviewLimitation(
        "provider-cancelled",
        ReviewLimitationScope("provider"),
        Some("cozy"),
        "Cozy provider work stopped when the Review Run was cancelled.",
        retryable = true
      )

      When("cancellation is requested and CNCF confirms terminal cancellation")
      val cancelling = CarReviewRunLifecycle.requestCancellation(running, _instant(3)).fold(_fail_lifecycle, identity)
      val cancelled = _project(cancelling, ReviewRunJobUpdate(
        JobStatus.Cancelled,
        _instant(4),
        limitations = Vector(limitation)
      ))

      Then("cancelling remains observable before one immutable cancelled result")
      cancelling.state shouldBe ReviewRunState("cancelling")
      cancelled.state shouldBe ReviewRunState("cancelled")
      cancelled.completedAt shouldBe Some(_instant(4))
      cancelled.limitations should contain(limitation)
      cancelled.reportId shouldBe None
      cancelled.failureCode shouldBe None
    }

    "turn CNCF failure or missing successful output into explicit failed Review state" in {
      Given("two running Review Runs, one failed and one settled without a report")
      val running = _running_run()
      val explicitlimitation = ReviewLimitation(
        "provider-timeout",
        ReviewLimitationScope("provider"),
        Some("cozy"),
        "The admitted provider exceeded its execution bound.",
        retryable = true
      )

      When("CNCF reports failure and success without canonical report completion")
      val failed = _project(running, ReviewRunJobUpdate(
        JobStatus.Failed,
        _instant(3),
        failureCode = Some(ReviewFailureCode("provider-timeout")),
        limitations = Vector(explicitlimitation)
      ))
      val missingreport = _project(_running_run(), ReviewRunJobUpdate(JobStatus.Succeeded, _instant(3)))

      Then("both outcomes fail safely with distinct attributable reasons")
      failed.state shouldBe ReviewRunState("failed")
      failed.failureCode shouldBe Some(ReviewFailureCode("provider-timeout"))
      failed.limitations should contain(explicitlimitation)
      missingreport.state shouldBe ReviewRunState("failed")
      missingreport.failureCode shouldBe Some(ReviewFailureCode("review-report-missing"))
      missingreport.limitations.map(_.code) should contain("review-report-missing")
    }

    "reject stale, malformed, and semantically impossible lifecycle input" in {
      Given("a running Review, an unknown wire field, and incompatible Job update payloads")
      val running = _running_run()
      val unknown = _json(Files.readString(_run_path)).mapObject(
        _.add("providerPayload", Json.fromString("not-admitted"))
      )

      When("the inputs cross the codec and lifecycle boundaries")
      val unknowndecoding = CarReviewRunCodec.decode(unknown.noSpaces).left.map(_.code)
      val stale = CarReviewRunLifecycle.projectJob(
        running,
        ReviewRunJobUpdate(JobStatus.Running, _instant(1))
      ).left.map(_.code)
      val impossible = CarReviewRunLifecycle.projectJob(
        running,
        ReviewRunJobUpdate(
          JobStatus.Running,
          _instant(3),
          completion = Some(ReviewRunCompletion(
            ReviewReportId("unexpected-report"),
            ReviewDigest("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
          ))
        )
      ).left.map(_.code)

      Then("unknown fields, regressing time, and non-success completion fail distinctly")
      unknowndecoding shouldBe Left("unknown-field")
      stale shouldBe Left("stale-update")
      impossible shouldBe Left("unexpected-completion")
    }
  }

  private val _run_path = Path.of("docs", "spec", "examples", "car-review-run-v1.json")

  private def _admitted_run(): CarReviewRun =
    CarReviewRunLifecycle.admitted(
      ReviewId("review-run-001"),
      ReviewTarget(
        ReviewTargetKind("project"),
        Some("org.textus"),
        "textus-user-account",
        Some(ReviewVersion("0.2.0-SNAPSHOT")),
        ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
      ),
      ReviewProfile("development"),
      _instant(0)
    ).fold(_fail_codec, identity)

  private def _running_run(): CarReviewRun = {
    val queued = _project(_admitted_run(), ReviewRunJobUpdate(JobStatus.Submitted, _instant(1)))
    _project(queued, ReviewRunJobUpdate(JobStatus.Running, _instant(2)))
  }

  private def _project(run: CarReviewRun, update: ReviewRunJobUpdate): CarReviewRun =
    CarReviewRunLifecycle.projectJob(run, update).fold(_fail_lifecycle, identity)

  private def _instant(second: Int): ReviewInstant =
    ReviewInstant(f"2026-07-16T00:00:$second%02dZ")

  private def _json(value: String): Json =
    parse(value).fold(error => fail(error.message), identity)

  private def _fail_codec(error: CarReviewCodecFailure): Nothing =
    fail(s"${error.code} at ${error.path}: ${error.message}")

  private def _fail_lifecycle(error: ReviewRunLifecycleFailure): Nothing =
    fail(s"${error.code} from ${error.state.value}: ${error.message}")
}
