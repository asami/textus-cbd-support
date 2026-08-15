package org.simplemodeling.textus.cbdsupport

import org.goldenport.cncf.context.{ExecutionContext, SecurityContext}
import org.goldenport.Conclusion
import org.goldenport.cncf.job.{ActionId, InMemoryJobEngine, JobPersistencePolicy, JobRunMode, JobStatus, JobSubmitOption, JobTask, TaskFailed, TaskOutcome, TaskSucceeded}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CncfCarReviewJobGatewaySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The production CNCF Review Job gateway" should {
    "persist and discover exact production bindings" which {
    "persist only canonical binding metadata and let another gateway discover the completed canonical report" in {
      Given("one persistent in-memory CNCF Job engine, operator context, and a server-owned execution")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = CarReviewProductionExecution.create(_request).fold(_fail, identity)
      val binding = CarReviewProductionJobBinding.from("diagnosis-production-job-001", execution).fold(_fail, identity)
      val gateway = new CncfCarReviewJobGateway(engine)

      try {
        When("CBD submits and drains the production job, then reads it through a fresh gateway")
        val actionid = ActionId.create("cbd.review", summon[ExecutionContext].clock.instant(), summon[ExecutionContext].idGeneration)
        ProductionReviewPipelineTask(actionid, binding, execution).run(summon[ExecutionContext]) match {
          case TaskSucceeded(_) => ()
          case TaskFailed(conclusion) => fail(conclusion.observation.getEffectiveMessage.getOrElse(conclusion.toString))
        }
        val jobid = gateway.submit(binding, execution).fold(_fail, identity)
        engine.drainAll()
        val found = new CncfCarReviewJobGateway(engine).findByReviewReuse(
          binding.reviewId,
          binding.reuseKeyDefinition,
          binding.reuseKeyDigest
        ).fold(_fail, identity)
        val foundbyid = new CncfCarReviewJobGateway(engine).findByReviewId(binding.reviewId).fold(_fail, identity)
        val model = engine.query(org.goldenport.cncf.job.JobId.parse(jobid.value).fold(_fail, identity)).get

        Then("the persisted safe binding has an exact parameter set and the completed report is canonical")
        val jobdiagnostic = s"resultSummary=${model.resultSummary}; tasks=${model.tasks}; timeline=${model.timeline}"
        withClue(jobdiagnostic) {
          model.status shouldBe JobStatus.Succeeded
        }
        withClue(jobdiagnostic) {
          found.map(_.update.status) shouldBe Some(JobStatus.Succeeded)
        }
        found.flatMap(_.update.failureCode) shouldBe None
        val bindingkeys = Set("bindingSchema", "diagnosisId", "reviewId", "reuseKeyDefinition", "reuseKeyDigest", "target", "profile", "startedAt")
        model.debug.parameters.keySet should contain allElementsOf bindingkeys
        model.debug.parameters.keySet.diff(bindingkeys).forall(_.startsWith("cncf.")) shouldBe true
        model.debug.parameters.values.exists(value => Vector("provider-descriptor", "provider-request", "review-job-result", "reportDocument", "credential").exists(value.contains)) shouldBe false
        found.map(_.binding) shouldBe Some(binding)
        foundbyid.map(_.jobId) shouldBe found.map(_.jobId)
        foundbyid.map(_.binding) shouldBe found.map(_.binding)
        foundbyid.flatMap(_.canonicalResponse).map(_.report.reportId) shouldBe found.flatMap(_.canonicalResponse).map(_.report.reportId)
        found.flatMap(_.canonicalResponse).map(_.report.reportId.value).exists(value => value.nonEmpty && value.length <= 180) shouldBe true
        found.flatMap(_.canonicalResponse).map(_.report.execution.providers.map(_.provider.id.value)) shouldBe Some(Vector("cbd-initial-static-quality"))
        found.map(_.run.state) shouldBe Some(ReviewRunState("completed"))
        found.map(_.run.startedAt) shouldBe Some(binding.startedAt)
        found.flatMap(_.run.completedAt).exists(value => !java.time.Instant.parse(value.value).isBefore(java.time.Instant.parse(binding.startedAt.value))) shouldBe true
        found.flatMap(_.terminalLease).map(_.isInstanceOf[CarReviewProductionTerminalLease.Completed]) shouldBe Some(true)
        found.flatMap(_.terminalLease).map(_.run.reportId) shouldBe found.map(_.run.reportId)
      } finally {
        engine.shutdown()
      }
    }

    "fail closed when a same-review persistent job has malformed binding identity metadata" in {
      Given("one valid production binding and one persistent job with a malformed review identity")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = _execution("review-malformed-binding")
      val binding = _binding("diagnosis-malformed-binding", execution)

      try {
        When("CBD discovers jobs by the shared Review identity")
        val malformed = CarReviewProductionJobBinding.parameters(binding).updated("diagnosisId", "raw request=credential")
        engine.submit(List(SpecFixedJobTask(_action_id, TaskSucceeded(OperationResponse(Record.dataAuto("status" -> "fixed"))))), summon[ExecutionContext], _option(malformed)).isSuccess shouldBe true
        val result = new CncfCarReviewJobGateway(engine).findByReviewId(binding.reviewId)

        Then("the malformed candidate is not silently treated as absent")
        result.fold(error => error.toString should include ("review-job-binding-identity-invalid"), _ => fail("Unexpected discovered job."))
      } finally {
        engine.shutdown()
      }
    }

    "reject bounded-invalid diagnosis and review identities before production submission" in {
      Given("one valid execution and its valid binding")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val execution = _execution("review-identity-bound")
      val binding = _binding("diagnosis-identity-bound", execution)

      When("CBD receives credential-like or oversized diagnosis/review identities")
      val invaliddiagnosis = CarReviewProductionJobBinding.from("diagnosis=credential", execution)
      val oversizeddiagnosis = CarReviewProductionJobBinding.from("a" * 181, execution)
      val invalidreview = binding.copy(reviewId = ReviewId("a" * 181)).validate(execution)

      Then("both identity boundaries reject the request before any Job submission")
      invaliddiagnosis.left.toOption shouldBe Some("review-job-binding-identity-invalid")
      oversizeddiagnosis.left.toOption shouldBe Some("review-job-binding-identity-invalid")
      invalidreview.left.toOption shouldBe Some("review-job-binding-identity-invalid")
    }

    "fail closed when two persistent jobs have the same exact production binding" in {
      Given("one engine and two submissions of exactly the same frozen binding")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = _execution("review-duplicate-binding")
      val binding = _binding("diagnosis-duplicate-binding", execution)
      val gateway = new CncfCarReviewJobGateway(engine)

      try {
        When("CBD searches by the frozen Review reuse identity before either worker runs")
        gateway.submit(binding, execution).isSuccess shouldBe true
        gateway.submit(binding, execution).isSuccess shouldBe true
        val result = gateway.findByReviewReuse(binding.reviewId, binding.reuseKeyDefinition, binding.reuseKeyDigest)

        Then("the ambiguous durable binding is rejected")
        result.fold(error => error.toString should include ("review-job-binding-ambiguous"), _ => fail("Unexpected discovered job."))
      } finally {
        engine.shutdown()
      }
    }

    "fail closed when the non-paginated persistent search reaches its configured bound" in {
      Given("one persistent production job and a search limit of one")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = _execution("review-search-bound")
      val binding = _binding("diagnosis-search-bound", execution)
      val submitter = new CncfCarReviewJobGateway(engine)

      try {
        When("CBD searches the bounded persistent JobEngine state")
        submitter.submit(binding, execution).isSuccess shouldBe true
        val result = new CncfCarReviewJobGateway(engine, persistentSearchLimit = 1).findByReviewId(binding.reviewId)

        Then("a candidate cannot hide possible older duplicates beyond the bound")
        result.fold(error => error.toString should include ("review-job-search-bound-exceeded"), _ => fail("Unexpected discovered job."))
      } finally {
        engine.shutdown()
      }
    }

    }

    "decode and project terminal Job results" which {
    "decode only an exact untampered production result record" in {
      Given("one canonical production response and its frozen binding")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = _execution("review-result-tamper")
      val binding = _binding("diagnosis-result-tamper", execution)
      val response = execution.execute(Set("operator"), _completed_at).fold(_fail, identity)
      val document = CarReviewReportCodec.encode(response.report).fold(_fail, identity)
      val gateway = new CncfCarReviewJobGateway(engine)
      val valid = _result_record(binding, response.report, document)

      try {
        When("CBD decodes a valid record and then records with altered digest or binding values")
        val decoded = gateway._decode_result(valid, binding)
        val digestmismatch = gateway._decode_result(valid.upsertSingle("reportDigest", "sha256:" + ("f" * 64)), binding)
        val bindingmismatch = gateway._decode_result(valid.upsertSingle("diagnosisId", "another-diagnosis"), binding)

        Then("only the exact record yields the canonical response")
        decoded.toOption.map(_.report.reportId) shouldBe Some(response.report.reportId)
        digestmismatch.fold(error => error.toString should include ("review-job-result-report-mismatch"), _ => fail("Unexpected decoded result."))
        bindingmismatch.fold(error => error.toString should include ("review-job-result-binding-mismatch"), _ => fail("Unexpected decoded result."))
      } finally {
        engine.shutdown()
      }
    }

    "fail discovery without issuing a terminal lease when a succeeded response disagrees with binding time" in {
      Given("one valid canonical response and a persisted binding with a different started-at value")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = _execution("review-result-time-mismatch")
      val original = _binding("diagnosis-result-time-mismatch", execution)
      val binding = original.copy(startedAt = ReviewInstant("2026-08-15T00:02:00Z"))
      val response = execution.execute(Set("operator"), _completed_at).fold(_fail, identity)
      val document = CarReviewReportCodec.encode(response.report).fold(_fail, identity)

      try {
        When("the persistent succeeded job carries the otherwise exact response record")
        engine.submit(
          List(SpecFixedJobTask(_action_id, TaskSucceeded(OperationResponse(_result_record(binding, response.report, document))))),
          summon[ExecutionContext],
          _option(CarReviewProductionJobBinding.parameters(binding))
        ).isSuccess shouldBe true
        engine.drainAll()
        val discovered = new CncfCarReviewJobGateway(engine).findByReviewId(binding.reviewId)

        Then("the mismatch is rejected before discovery can expose a completed lease")
        discovered.fold(error => error.toString should include ("review-job-result-started-at-mismatch"), _ => fail("Unexpected terminal lease."))
      } finally {
        engine.shutdown()
      }
    }

    "project validated task failure sentinels and reject arbitrary failure prose as a code" in {
      Given("separate persistent engines with valid bindings and fixed failed tasks")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val sentinelengine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val proseengine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val sentinelexecution = _execution("review-failure-sentinel")
      val proseexecution = _execution("review-failure-prose")
      val sentinelbinding = _binding("diagnosis-failure-sentinel", sentinelexecution)
      val prosebinding = _binding("diagnosis-failure-prose", proseexecution)
      val invalidmessages = Vector(
        "operator note operation:credential-leaked",
        "provider-contract-failed: prose",
        "prefix textus.cbd.review.failure.v1:credential-leaked"
      )
      val invalids = invalidmessages.zipWithIndex.map { case (message, index) =>
        val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
        val execution = _execution(s"review-failure-invalid-$index")
        val binding = _binding(s"diagnosis-failure-invalid-$index", execution)
        (engine, binding, message)
      }

      try {
        When("the workers fail with an anchored sentinel, free prose, and code-like but non-sentinel prose")
        sentinelengine.submit(List(SpecFixedJobTask(_action_id, TaskFailed(Conclusion.simple("textus.cbd.review.failure.v1:provider-contract-failed")))), summon[ExecutionContext], _option(CarReviewProductionJobBinding.parameters(sentinelbinding))).isSuccess shouldBe true
        proseengine.submit(List(SpecFixedJobTask(_action_id, TaskFailed(Conclusion.simple("unexpected human prose must never become a failure code")))), summon[ExecutionContext], _option(CarReviewProductionJobBinding.parameters(prosebinding))).isSuccess shouldBe true
        invalids.foreach { case (engine, binding, message) =>
          engine.submit(List(SpecFixedJobTask(_action_id, TaskFailed(Conclusion.simple(message)))), summon[ExecutionContext], _option(CarReviewProductionJobBinding.parameters(binding))).isSuccess shouldBe true
        }
        sentinelengine.drainAll()
        proseengine.drainAll()
        invalids.foreach(_._1.drainAll())
        val sentinel = new CncfCarReviewJobGateway(sentinelengine).findByReviewId(sentinelbinding.reviewId).fold(_fail, identity)
        val prose = new CncfCarReviewJobGateway(proseengine).findByReviewId(prosebinding.reviewId).fold(_fail, identity)
        val invalidupdates = invalids.map { case (engine, binding, _) =>
          new CncfCarReviewJobGateway(engine).findByReviewId(binding.reviewId).fold(_fail, identity)
        }

        Then("only the exact full sentinel is projected and every prose variation falls back to the lifecycle code")
        sentinel.map(_.update.status) shouldBe Some(JobStatus.Failed)
        sentinel.flatMap(_.canonicalResponse) shouldBe None
        sentinel.flatMap(_.update.failureCode) shouldBe Some(ReviewFailureCode("provider-contract-failed"))
        sentinel.flatMap(_.terminalLease).map(_.isInstanceOf[CarReviewProductionTerminalLease.Failed]) shouldBe Some(true)
        prose.flatMap(_.update.failureCode) shouldBe Some(ReviewFailureCode("cncf-job-failed"))
        invalidupdates.foreach { update =>
          update.flatMap(_.update.failureCode) shouldBe Some(ReviewFailureCode("cncf-job-failed"))
        }
      } finally {
        sentinelengine.shutdown()
        proseengine.shutdown()
        invalids.foreach(_._1.shutdown())
      }
    }

    "preserve cancellation through the production binding without a canonical response" in {
      Given("one held production job and an authorized operator")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = _execution("review-production-cancel")
      val binding = _binding("diagnosis-production-cancel", execution)
      val gateway = new CncfCarReviewJobGateway(engine)

      try {
        When("the operator submits and cancels the exact persistent production job")
        val jobid = gateway.submit(binding, execution).fold(_fail, identity)
        gateway.cancel(jobid).isSuccess shouldBe true
        val found = gateway.findByReviewId(binding.reviewId).fold(_fail, identity)

        Then("the job remains a cancelled lifecycle entry and never produces a response")
        found.map(_.update.status) shouldBe Some(JobStatus.Cancelled)
        found.flatMap(_.canonicalResponse) shouldBe None
        found.map(_.run.state) shouldBe Some(ReviewRunState("cancelled"))
        found.flatMap(_.terminalLease).map(_.isInstanceOf[CarReviewProductionTerminalLease.Cancelled]) shouldBe Some(true)
      } finally {
        engine.shutdown()
      }
    }

    }

    "project non-terminal Job state" which {
    "project a held submitted job to a queued non-terminal Review Run" in {
      Given("one persistent production job whose worker has not been drained")
      given ExecutionContext = ExecutionContext.test(SecurityContext.Privilege.Operator)
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val execution = _execution("review-production-held")
      val binding = _binding("diagnosis-production-held", execution)
      val gateway = new CncfCarReviewJobGateway(engine)

      try {
        When("CBD discovers the submitted job before execution")
        gateway.submit(binding, execution).fold(_fail, identity)
        val found = gateway.findByReviewId(binding.reviewId).fold(_fail, identity)
          .getOrElse(fail("Expected submitted production job."))

        Then("the legal projection is queued and cannot issue a terminal lease")
        found.update.status shouldBe JobStatus.Submitted
        found.run.state shouldBe ReviewRunState("queued")
        found.run.startedAt shouldBe binding.startedAt
        found.terminalLease shouldBe None
        found.canonicalResponse shouldBe None
      } finally {
        engine.shutdown()
      }
    }
    }
  }

  private val _request = ReviewStartRequest(
    ReviewId("review-production-job-001"),
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

  private val _completed_at = ReviewInstant("2026-08-15T00:01:00Z")

  private def _fail(error: Any): Nothing = fail(error.toString)

  private def _execution(reviewid: String)(using ExecutionContext): CarReviewProductionExecution =
    CarReviewProductionExecution.create(_request.copy(reviewId = ReviewId(reviewid))).fold(_fail, identity)

  private def _binding(
    diagnosisid: String,
    execution: CarReviewProductionExecution
  ): CarReviewProductionJobBinding =
    CarReviewProductionJobBinding.from(diagnosisid, execution).fold(_fail, identity)

  private def _option(parameters: Map[String, String]): JobSubmitOption =
    JobSubmitOption(
      persistence = JobPersistencePolicy.Persistent,
      runMode = JobRunMode.Async,
      parameters = parameters
    )

  private def _action_id(using ctx: ExecutionContext): ActionId =
    ActionId.create("cbd.review.spec", ctx.clock.instant(), ctx.idGeneration)

  private def _result_record(
    binding: CarReviewProductionJobBinding,
    report: CarReviewReport,
    document: String
  ): Record =
    Record.dataAuto(
      "schemaVersion" -> "textus.cbd.review-job-result.v1",
      "documentType" -> "review-job-result",
      "status" -> "completed",
      "diagnosisId" -> binding.diagnosisId,
      "reviewId" -> binding.reviewId.value,
      "reuseKeyDefinition" -> binding.reuseKeyDefinition,
      "reuseKeyDigest" -> binding.reuseKeyDigest.value,
      "reportId" -> report.reportId.value,
      "reportDigest" -> report.reportDigest.value,
      "reportDocument" -> document
    )
}

private final case class SpecFixedJobTask(
  actionId: ActionId,
  outcome: TaskOutcome
) extends JobTask {
  override def componentName: Option[String] = Some("CbdSupportSpec")
  override def serviceName: Option[String] = Some("CbdReviewSpec")
  override def operationName: Option[String] = Some("fixed")

  def run(ctx: ExecutionContext): TaskOutcome = {
    val _ = ctx
    outcome
  }
}
