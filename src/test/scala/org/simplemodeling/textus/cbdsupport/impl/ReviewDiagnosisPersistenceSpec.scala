package org.simplemodeling.textus.cbdsupport.impl

import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.{
  Callable,
  CountDownLatch,
  Executors,
  TimeUnit
}
import java.util.concurrent.atomic.AtomicInteger

import cats.{Id, ~>}
import cats.data.State
import cats.effect.Ref
import org.goldenport.Consequence
import org.goldenport.ConsequenceT
import org.goldenport.Conclusion
import org.goldenport.cncf.Program
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.component.{
  Component,
  ComponentId,
  ComponentInstanceId
}
import org.goldenport.cncf.config.ResolvedParameters
import org.goldenport.cncf.context.{DataStoreContext, EntitySpaceContext, EntityStoreContext, ExecutionContext, RuntimeContext, ScopeContext, ScopeKind}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.datastore.sql.SqlDataStore
import org.goldenport.cncf.entity.{
  EntityPersistent,
  EntityPersistentCreate,
  EntityRevisionBinding,
  EntityRevisionRepresentation,
  EntityStore,
  EntityStoreSpace
}
import org.goldenport.cncf.entity.runtime.{
  EntityCollection,
  EntityDescriptor,
  EntityLoader,
  EntityMemoryPolicy,
  EntityRealm,
  EntityRealmState,
  EntityRuntimePlan,
  EntityStorage,
  PartitionStrategy
}
import org.goldenport.cncf.job.{ActionId, InMemoryJobEngine, JobPersistencePolicy, JobRunMode, JobSubmitOption, JobTask, TaskFailed, TaskOutcome}
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.cncf.unitofwork.UnitOfWorkOp.EntityStoreCreate
import org.goldenport.protocol.{Protocol, Request}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.runtime.*
import org.simplemodeling.textus.cbdsupport.entity.{
  ReviewAttestationSnapshot as ReviewAttestationSnapshotEntity,
  ReviewDiagnosis as ReviewDiagnosisEntity,
  ReviewReportSnapshot as ReviewReportSnapshotEntity,
  ReviewRetentionEvent as ReviewRetentionEventEntity,
  ReviewTargetSnapshot as ReviewTargetSnapshotEntity,
  ReviewRunSnapshot as ReviewRunSnapshotEntity
}
import org.simplemodeling.textus.cbdsupport.entity.create.{
  ReviewReportSnapshot as ReviewReportSnapshotCreate
}

/*
 * @since   Jul. 23, 2026
 *  version Jul. 26, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class ReviewDiagnosisPersistenceSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {
  "CAR Review diagnosis persistence" should {
    "admit and retain diagnosis aggregates" which {
    "claim one stable Entity diagnosis and join a duplicate reusable execution" in {
      Given("two executions with the same server-derived reuse identity")
      val factory = new ComponentFactory()
      val context = _sqlite_context()
      val firstplan = _plan("review-owner")
      val duplicateplan = _plan("review-joiner")

      When("both executions are admitted through separate UnitOfWork boundaries")
      val first = _run(factory, context, firstplan)
      val duplicate = _run(factory, context, duplicateplan)
      val owner = _owner(first)

      Then("the first execution owns the Aggregate and the duplicate joins it")
      first shouldBe a[CarReviewDiagnosisAdmission.Owner]
      owner.reviewId shouldBe ReviewId("review-owner")
      duplicate shouldBe CarReviewDiagnosisAdmission.Joined(first.diagnosisId, ReviewId("review-owner"))
    }

    "retain immutable completion snapshots and reuse the completed canonical Report" in {
      Given("an owned diagnosis and its canonical completed response")
      val factory = new ComponentFactory()
      val context = _context()
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)
      val attestation = CarReviewAttestationCodec.fromReport(report).fold(error => fail(error.message), identity)
      val ownerplan = _plan_for(report, report.reviewId.value)
      val repeatedplan = _plan_for(report, "review-reused-request")

      When("completion and a later same-key request cross fresh UnitOfWork reads")
      val owner = _owner(_run(factory, context, ownerplan))
      _value(org.simplemodeling.model.datatype.EntityId.parse(owner.diagnosisId)) shouldBe _diagnosis_id(ownerplan)
      val completion = _run_completion(factory, context, owner, ownerplan, CarReviewCanonicalResponse(report, report.gate, attestation))
      val repeated = _run(factory, context, repeatedplan)

      Then("both responses reuse the persisted canonical Report identity")
      completion match {
        case CarReviewDiagnosisAdmission.Reused(diagnosisid, reviewid, reportid, digest) =>
          diagnosisid shouldBe owner.diagnosisId
          reviewid shouldBe report.reviewId
          reportid shouldBe report.reportId
          digest shouldBe report.reportDigest
        case other => fail(s"Expected completed reuse, got $other")
      }
      repeated match {
        case CarReviewDiagnosisAdmission.Reused(diagnosisid, reviewid, reportid, digest) =>
          diagnosisid shouldBe owner.diagnosisId
          reviewid shouldBe report.reviewId
          reportid shouldBe report.reportId
          digest shouldBe report.reportDigest
        case other => fail(s"Expected persisted reuse, got $other")
      }
    }

    "read one completed Report from the persisted Entity boundary by exact Report ID" in {
      Given("a completed diagnosis persisted through the Aggregate")
      val factory = new ComponentFactory()
      val context = _context()
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)
      val attestation = CarReviewAttestationCodec.fromReport(report).fold(error => fail(error.message), identity)
      val plan = _plan_for(report, report.reviewId.value)
      val owner = _owner(_run(factory, context, plan))
      _run_completion(factory, context, owner, plan, CarReviewCanonicalResponse(report, report.gate, attestation))
      val viewercontext = _viewer_context(context)

      When("an authorized reader requests that exact Report through a fresh UnitOfWork")
      val loaded = _value(new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._load_persisted_review_report(_core(viewercontext), report.reportId)
      ))
      val missing = new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._load_persisted_review_report(_core(viewercontext), ReviewReportId("report-missing"))
      )
      val denied = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._load_persisted_review_report(_core(context), report.reportId)
      )
      val dashboard = _value(new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._review_dashboard(_core(viewercontext), report.reportId, Set("viewer"))
      ))
      val diagnosis = _value(new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._review_diagnosis(
          _core(viewercontext),
          report.reportId,
          CarReviewWebDiagnosisKind.OBSERVATION,
          "report-finding-missing-rationale",
          Set("viewer")
        )
      ))

      Then("the Entity record, not an in-memory repository, is the exact bounded source")
      loaded.reportId shouldBe report.reportId
      loaded.reportDigest shouldBe report.reportDigest
      loaded.reviewId shouldBe report.reviewId
      missing shouldBe a[Consequence.Failure[_]]
      denied shouldBe a[Consequence.Failure[_]]
      dashboard.dashboard.reportId shouldBe report.reportId
      dashboard.dashboard.reportDigest shouldBe report.reportDigest
      diagnosis.diagnosis.kind shouldBe CarReviewWebDiagnosisKind.OBSERVATION
      diagnosis.diagnosis.itemId shouldBe "report-finding-missing-rationale"
      diagnosis.diagnosis.reportId shouldBe report.reportId
      diagnosis.diagnosis.reportDigest shouldBe report.reportDigest
    }

    "refuse an otherwise valid Report snapshot while its root remains claimed" in {
      Given("a claimed diagnosis and a canonical Report snapshot whose root was never settled")
      val factory = new ComponentFactory()
      val context = _operator_context(_context())
      given ExecutionContext = context
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)
      val attestation = CarReviewAttestationCodec.fromReport(report).fold(error => fail(error.message), identity)
      val plan = _plan_for(report, report.reviewId.value)
      val owner = _owner(_run(factory, context, plan))
      val snapshot = _report_snapshot_for_claimed_root(_diagnosis_id(plan), report, attestation)
      context.entityStoreSpace.create(
        EntityStoreCreate(snapshot, summon[EntityPersistentCreate[ReviewReportSnapshotCreate]])
      ).isSuccess shouldBe true
      val viewercontext = _viewer_context(context)

      When("an authorized reader requests the snapshot by its exact Report ID")
      val result = new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._load_persisted_review_report(_core(viewercontext), report.reportId)
      )
      val root = _load_snapshot[ReviewDiagnosisEntity](context, _value(org.simplemodeling.model.datatype.EntityId.parse(owner.diagnosisId)))

      Then("the root visibility gate refuses the Report and leaves the root claimed")
      result shouldBe a[Consequence.Failure[_]]
      root.state.value shouldBe "claimed"
      root.report_id shouldBe None
      root.report_digest shouldBe None
    }

    "expire a due persisted Report with an attributable tombstone and admit a fresh successor" in {
      Given("a completed Report in the SQLite-backed datastore older than the fixed Entity retention policy")
      val factory = new ComponentFactory()
      val context = _sqlite_context()
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)
      val attestation = CarReviewAttestationCodec.fromReport(report).fold(error => fail(error.message), identity)
      val plan = _plan_for(report, report.reviewId.value)
      val owner = _owner(_run(factory, context, plan))
      _run_completion(factory, context, owner, plan, CarReviewCanonicalResponse(report, report.gate, attestation))
      val viewercontext = _viewer_context(context)
      val operatorcontext = _operator_context(context)

      When("an operator expires the exact payload after its retention age")
      val denied = new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._expire_persisted_review_report(_core(viewercontext), report.reportId, ReviewInstant("2026-12-16T00:02:01Z"))
      )
      val event = _value(new UnitOfWorkInterpreter(new UnitOfWork(operatorcontext)).run(
        factory._expire_persisted_review_report(_core(operatorcontext), report.reportId, ReviewInstant("2026-12-16T00:02:01Z"))
      ))
      val afterexpiry = new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._load_persisted_review_report(_core(viewercontext), report.reportId)
      )
      val repeated = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._admit_review_execution(_core(context), _plan_for(report, "review-after-expiry"))
      )

      Then("only an authorized tombstone remains, payload reads fail, and a fresh successor owns the next execution")
      denied shouldBe a[Consequence.Failure[_]]
      event.action.value shouldBe "expired"
      event.record_type.value shouldBe "report"
      event.record_digest.value shouldBe report.reportDigest.value
      event.report_id.map(_.value) shouldBe Some(report.reportId.value)
      event.report_digest.map(_.value) shouldBe Some(report.reportDigest.value)
      afterexpiry shouldBe a[Consequence.Failure[_]]
      _owner(_value(repeated)).reviewId shouldBe ReviewId("review-after-expiry")
    }

    "retain every non-success terminal Run while admitting a fresh successor" in {
      Given("independent owned diagnoses for every non-success terminal state")
      val factory = new ComponentFactory()
      val context = _context()
      val states = Vector(
        CarReviewDiagnosisTerminalState.Failed,
        CarReviewDiagnosisTerminalState.Cancelled,
        CarReviewDiagnosisTerminalState.Expired,
        CarReviewDiagnosisTerminalState.Incompatible
      ).zip(Vector('e', 'f', 'b', 'c'))

      When("each diagnosis records its terminal Run through the Aggregate boundary")
      val results = states.map { case (state, targetdigest) =>
        val plan = _plan(s"review-${state.value}", targetdigest)
        val owner = _owner(_run(factory, context, plan))
        _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          factory._record_terminal_review_execution(_core(context), owner, plan, state, s"{\"state\":\"${state.value}\"}", ReviewInstant("2026-07-23T01:00:00Z"))
        )) shouldBe ()
        val successorid = ReviewId(s"review-after-${state.value}")
        state -> (successorid -> new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          factory._admit_review_execution(_core(context), _plan(successorid.value, targetdigest))
        ))
      }

      Then("no terminal diagnosis is reused and every request owns a fresh successor")
      results.foreach { case (state, (successorid, result)) =>
        withClue(s"${state.value} must admit the requested successor") {
          _owner(_value(result)).reviewId shouldBe successorid
        }
      }
    }

    "install one authoritative successor and start its work once" in {
      Given(
        "a retained terminal Run and two through eight simultaneous same-key successor requests"
      )

      forAll(Gen.choose(2, 8)) { generatedcallercount =>
        val callercount = generatedcallercount.max(2)
        val factory = new ComponentFactory()
        val context = _context()
        val terminalplan = _plan("review-terminal-predecessor", '9')
        val terminalowner = _owner(_run(factory, context, terminalplan))
        _value(
          new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
            factory._record_terminal_review_execution(
              _core(context),
              terminalowner,
              terminalplan,
              CarReviewDiagnosisTerminalState.Failed,
              """{"state":"failed","failure":"provider"}""",
              ReviewInstant("2026-07-23T01:00:00Z")
            )
          )
        ) shouldBe ()
        val plans =
          Vector.tabulate(callercount)(index =>
            _plan(s"review-successor-$index", '9')
          )
        val continuationcount = new AtomicInteger(0)

        When("independent UnitOfWork callers attempt the terminal successor transition")
        val admissions =
          _run_concurrently(
            factory,
            context,
            plans,
            continuationcount
          )

        Then("one caller owns the authoritative successor and every loser joins it")
        val owners = admissions.collect {
          case owner: CarReviewDiagnosisAdmission.Owner => owner
        }
        val joined = admissions.collect {
          case value: CarReviewDiagnosisAdmission.Joined => value
        }
        owners should have size 1
        joined should have size callercount - 1
        joined.map(_.reviewId).distinct shouldBe Vector(owners.head.reviewId)
        continuationcount.get shouldBe 1

        And("the immutable terminal predecessor and one admitted successor both remain")
        val snapshots =
          _run_snapshots(context, terminalplan, owners.head.reviewId)
        snapshots.map(_.state.value).sorted shouldBe Vector("admitted", "failed")
        snapshots.map(_.review_id.value).toSet shouldBe
          Set(terminalplan.request.reviewId.value, owners.head.reviewId.value)
      }
    }

    "require the issued Owner lease before a claimed Aggregate can transition" in {
      Given("an Aggregate claimed by one issued Owner lease")
      val factory = new ComponentFactory()
      val context = _context()
      val ownerplan = _plan("review-lease-owner", 'd')
      val otherplan = _plan("review-lease-other", 'd')
      val owner = _owner(_run(factory, context, ownerplan))

      When("another execution plan attempts the terminal transition")
      val result = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._record_terminal_review_execution(
          _core(context),
          owner,
          otherplan,
          CarReviewDiagnosisTerminalState.Cancelled,
          "{\"state\":\"cancelled\"}",
          ReviewInstant("2026-07-23T01:00:00Z")
        )
      )

      Then("the Aggregate boundary rejects the non-owner transition")
      result shouldBe a[Consequence.Failure[_]]
    }

    "read the persisted terminal Aggregate state before a repeat transition" in {
      Given("an owned diagnosis that will be retained as cancelled")
      val factory = new ComponentFactory()
      val context = _context()
      val plan = _plan("review-terminal-replay", '0')
      val owner = _owner(_run(factory, context, plan))

      When("the same terminal transition is attempted through two UnitOfWork boundaries")
      val first = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._record_terminal_review_execution(
          _core(context),
          owner,
          plan,
          CarReviewDiagnosisTerminalState.Cancelled,
          "{\"state\":\"cancelled\"}",
          ReviewInstant("2026-07-23T01:00:00Z")
        )
      )
      val repeated = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._record_terminal_review_execution(
          _core(context),
          owner,
          plan,
          CarReviewDiagnosisTerminalState.Cancelled,
          "{\"state\":\"cancelled\"}",
          ReviewInstant("2026-07-23T01:00:01Z")
        )
      )

      Then("the persisted first transition succeeds and the replay is rejected")
      _value(first) shouldBe ()
      repeated shouldBe a[Consequence.Failure[_]]
    }

    }

    "settle opaque production Job leases" which {
    "settle one gateway-issued completed lease exactly once and replay it safely" in {
      Given("an operator-owned production execution, Entity claim, and completed CNCF Job")
      val factory = new ComponentFactory()
      val context = _operator_context(_context())
      given ExecutionContext = context
      val execution = _production_execution("review-job-settlement-completed", '1')
      val owner = _owner(_run(factory, context, execution.plan))
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))

      try {
        When("the discovered gateway lease settles the claimed Entity twice through fresh UnitOfWork boundaries")
        val lease = _completed_lease(engine, owner, execution)
        val first = _settle_completed(context, lease)
        val repeated = _settle_completed(context, lease)
        val diagnosisid = _diagnosis_id(execution.plan)
        val target = _load_snapshot[ReviewTargetSnapshotEntity](context, _snapshot_id("target", diagnosisid, execution.plan.request.target.digest.value, ReviewTargetSnapshotEntity.collectionId))
        val run = _load_snapshot[ReviewRunSnapshotEntity](context, _snapshot_id("run", diagnosisid, execution.plan.request.reviewId.value, ReviewRunSnapshotEntity.collectionId))
        val report = _load_snapshot[ReviewReportSnapshotEntity](context, _snapshot_id("report", diagnosisid, lease.response.report.reportId.value, ReviewReportSnapshotEntity.collectionId))
        val attestation = _load_snapshot[ReviewAttestationSnapshotEntity](context, _snapshot_id("attestation", diagnosisid, lease.response.attestation.attestationId.value, ReviewAttestationSnapshotEntity.collectionId))
        val viewercontext = _viewer_context(context)
        val loaded = _value(new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
          factory._load_persisted_review_report(_core(viewercontext), lease.response.report.reportId)
        ))

        Then("both outcomes and all deterministic snapshots bind the one canonical gateway report")
        first shouldBe repeated
        first.reportId shouldBe lease.response.report.reportId
        loaded.reportId shouldBe lease.response.report.reportId
        loaded.reportDigest shouldBe lease.response.report.reportDigest
        target.target_digest.value shouldBe execution.plan.request.target.digest.value
        CarReviewRunCodec.encode(lease.run).fold(error => fail(error.message), identity) shouldBe run.run_document.value.value
        CarReviewReportCodec.encode(lease.response.report).fold(error => fail(error.message), identity) shouldBe report.report_document.value.value
        CarReviewAttestationCodec.encode(lease.response.attestation).fold(error => fail(error.message), identity) shouldBe attestation.attestation_document.value.value
      } finally engine.shutdown()
    }

    "concurrently replay one gateway-issued completed lease without duplicate deterministic snapshots" in {
      Given("one separately claimed production execution and its completed real gateway lease")
      val factory = new ComponentFactory()
      val context = _operator_context(_context())
      given ExecutionContext = context
      val execution = _production_execution("review-job-settlement-concurrent", '2')
      val owner = _owner(_run(factory, context, execution.plan))
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))

      try {
        When("two UnitOfWork settlements start together with the same opaque lease")
        val lease = _completed_lease(engine, owner, execution)
        val outcomes = _settle_completed_concurrently(context, lease)
        val diagnosisid = _diagnosis_id(execution.plan)

        Then("both calls reuse the same result and the deterministic target, run, report, and attestation exist")
        outcomes.distinct should have size 1
        _load_snapshot[ReviewTargetSnapshotEntity](context, _snapshot_id("target", diagnosisid, execution.plan.request.target.digest.value, ReviewTargetSnapshotEntity.collectionId)).id shouldBe _snapshot_id("target", diagnosisid, execution.plan.request.target.digest.value, ReviewTargetSnapshotEntity.collectionId)
        _load_snapshot[ReviewRunSnapshotEntity](context, _snapshot_id("run", diagnosisid, execution.plan.request.reviewId.value, ReviewRunSnapshotEntity.collectionId)).id shouldBe _snapshot_id("run", diagnosisid, execution.plan.request.reviewId.value, ReviewRunSnapshotEntity.collectionId)
        _load_snapshot[ReviewReportSnapshotEntity](context, _snapshot_id("report", diagnosisid, lease.response.report.reportId.value, ReviewReportSnapshotEntity.collectionId)).id shouldBe _snapshot_id("report", diagnosisid, lease.response.report.reportId.value, ReviewReportSnapshotEntity.collectionId)
        _load_snapshot[ReviewAttestationSnapshotEntity](context, _snapshot_id("attestation", diagnosisid, lease.response.attestation.attestationId.value, ReviewAttestationSnapshotEntity.collectionId)).id shouldBe _snapshot_id("attestation", diagnosisid, lease.response.attestation.attestationId.value, ReviewAttestationSnapshotEntity.collectionId)
      } finally engine.shutdown()
    }

    "settle a real completed lease on both configured Entity stores" in {
      Given("one claimed production execution in each supported persistence configuration")
      Vector(
        "memory" -> _operator_context(_context()),
        "sqlite" -> _operator_context(_sqlite_context())
      ).zipWithIndex.foreach { case ((store, context), index) =>
        val factory = new ComponentFactory()
        val execution = _production_execution(s"review-job-settlement-$store", (('7'.toInt + index).toChar))(using context)
        val owner = _owner(_run(factory, context, execution.plan))
        val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))

        try {
          When(s"the $store Entity store settles its gateway-issued completed lease")
          val lease = _completed_lease(engine, owner, execution)(using context)
          _settle_completed(context, lease)
          val diagnosisid = _diagnosis_id(execution.plan)
          val root = _load_snapshot[ReviewDiagnosisEntity](context, diagnosisid)
          val target = _load_snapshot[ReviewTargetSnapshotEntity](context, _snapshot_id("target", diagnosisid, execution.plan.request.target.digest.value, ReviewTargetSnapshotEntity.collectionId))
          val run = _load_snapshot[ReviewRunSnapshotEntity](context, _snapshot_id("run", diagnosisid, execution.plan.request.reviewId.value, ReviewRunSnapshotEntity.collectionId))
          val report = _load_snapshot[ReviewReportSnapshotEntity](context, _snapshot_id("report", diagnosisid, lease.response.report.reportId.value, ReviewReportSnapshotEntity.collectionId))
          val attestation = _load_snapshot[ReviewAttestationSnapshotEntity](context, _snapshot_id("attestation", diagnosisid, lease.response.attestation.attestationId.value, ReviewAttestationSnapshotEntity.collectionId))
          val viewercontext = _viewer_context(context)
          val readable = _value(new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
            factory._load_persisted_review_report(_core(viewercontext), lease.response.report.reportId)
          ))

          Then(s"the $store root and all deterministic members bind the exact completed lease")
          root.state.value shouldBe "completed"
          root.report_id.map(_.value) shouldBe Some(lease.response.report.reportId.value)
          root.report_digest.map(_.value) shouldBe Some(lease.response.report.reportDigest.value)
          target.target_digest.value shouldBe execution.plan.request.target.digest.value
          CarReviewRunCodec.encode(lease.run).fold(error => fail(error.message), identity) shouldBe run.run_document.value.value
          CarReviewReportCodec.encode(lease.response.report).fold(error => fail(error.message), identity) shouldBe report.report_document.value.value
          CarReviewAttestationCodec.encode(lease.response.attestation).fold(error => fail(error.message), identity) shouldBe attestation.attestation_document.value.value
          readable shouldBe lease.response.report
        } finally engine.shutdown()
      }
    }

    "reject a different real completed lease without mutating the claimed Aggregate" in {
      Given("one claimed production root and a different production execution bound to its diagnosis ID")
      val factory = new ComponentFactory()
      val context = _operator_context(_context())
      given ExecutionContext = context
      val firstexecution = _production_execution("review-job-settlement-mismatch-a", '3')(using context)
      val secondexecution = _production_execution("review-job-settlement-mismatch-b", '4')(using context)
      val firstowner = _owner(_run(factory, context, firstexecution.plan))
      val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))

      try {
        When("a real gateway lease has the claimed diagnosis ID but different reuse and target bindings")
        val binding = CarReviewProductionJobBinding.from(firstowner.diagnosisId, secondexecution).fold(error => fail(error.toString), identity)
        val gateway = new CncfCarReviewJobGateway(engine)
        _value(gateway.submit(binding, secondexecution))
        engine.drainAll()
        val lease = _value(gateway.findByReviewId(binding.reviewId)).flatMap(_.terminalLease) match {
          case Some(value: CarReviewProductionTerminalLease.Completed) => value
          case _ => fail("Expected a gateway-issued completed terminal lease.")
        }
        val outcome = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          new ReviewDiagnosisAdmissionProgram(_core(context)).completeFromJob(lease)
        )
        val firstdiagnosisid = _diagnosis_id(firstexecution.plan)
        val firstroot = _load_snapshot[ReviewDiagnosisEntity](context, firstdiagnosisid)

        Then("root binding validation refuses before any pending completion member is created")
        outcome shouldBe a[Consequence.Failure[_]]
        firstroot.state.value shouldBe "claimed"
        firstroot.report_id shouldBe None
        _find_snapshot[ReviewTargetSnapshotEntity](context, _snapshot_id("target", firstdiagnosisid, firstexecution.plan.request.target.digest.value, ReviewTargetSnapshotEntity.collectionId)) shouldBe empty
        _find_snapshot[ReviewRunSnapshotEntity](context, _snapshot_id("run", firstdiagnosisid, firstexecution.plan.request.reviewId.value, ReviewRunSnapshotEntity.collectionId)) shouldBe empty
        _find_snapshot[ReviewReportSnapshotEntity](context, _snapshot_id("report", firstdiagnosisid, lease.response.report.reportId.value, ReviewReportSnapshotEntity.collectionId)) shouldBe empty
        _find_snapshot[ReviewAttestationSnapshotEntity](context, _snapshot_id("attestation", firstdiagnosisid, lease.response.attestation.attestationId.value, ReviewAttestationSnapshotEntity.collectionId)) shouldBe empty
      } finally engine.shutdown()
    }

    "retain failed and cancelled gateway leases idempotently before admitting Entity successors" in {
      Given("separate claimed production executions that fail and cancel through real CNCF Jobs")
      val factory = new ComponentFactory()
      val context = _operator_context(_context())
      given ExecutionContext = context
      val failedexecution = _production_execution("review-job-settlement-failed", '5')
      val cancelledexecution = _production_execution("review-job-settlement-cancelled", '6')
      val failedowner = _owner(_run(factory, context, failedexecution.plan))
      val cancelledowner = _owner(_run(factory, context, cancelledexecution.plan))
      val failedengine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
      val cancelledengine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))

      try {
        When("gateway discovery issues opaque failed and cancelled terminal leases and each is settled twice")
        val failedlease = _failed_lease(failedengine, failedowner, failedexecution)
        val cancelledlease = _cancelled_lease(cancelledengine, cancelledowner, cancelledexecution)
        _settle_failed(context, failedlease) shouldBe ()
        _settle_failed(context, failedlease) shouldBe ()
        _settle_cancelled(context, cancelledlease) shouldBe ()
        _settle_cancelled(context, cancelledlease) shouldBe ()
        val failedrun = _load_snapshot[ReviewRunSnapshotEntity](context, _snapshot_id("terminal-failed", _diagnosis_id(failedexecution.plan), failedexecution.plan.request.reviewId.value, ReviewRunSnapshotEntity.collectionId))
        val cancelledrun = _load_snapshot[ReviewRunSnapshotEntity](context, _snapshot_id("terminal-cancelled", _diagnosis_id(cancelledexecution.plan), cancelledexecution.plan.request.reviewId.value, ReviewRunSnapshotEntity.collectionId))
        val failedsuccessor = _same_reuse_successor(failedexecution.plan, "review-job-settlement-failed-successor")
        val cancelledsuccessor = _same_reuse_successor(cancelledexecution.plan, "review-job-settlement-cancelled-successor")

        Then("each exact terminal run remains once and a same-reuse request receives a fresh Owner")
        CarReviewRunCodec.decode(failedrun.run_document.value.value).fold(error => fail(error.message), identity) shouldBe failedlease.run
        failedrun.state.value shouldBe "failed"
        CarReviewRunCodec.decode(cancelledrun.run_document.value.value).fold(error => fail(error.message), identity) shouldBe cancelledlease.run
        cancelledrun.state.value shouldBe "cancelled"
        _owner(_run(factory, context, failedsuccessor)).reviewId shouldBe failedsuccessor.request.reviewId
        _owner(_run(factory, context, cancelledsuccessor)).reviewId shouldBe cancelledsuccessor.request.reviewId
      } finally {
        failedengine.shutdown()
        cancelledengine.shutdown()
      }
    }
    }
  }

  private def _run(
    factory: ComponentFactory,
    context: ExecutionContext,
    plan: CarReviewExecutionPlan
  ): CarReviewDiagnosisAdmission =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
      factory._admit_review_execution(_core(context), plan)
    ))

  private def _run_completion(
    factory: ComponentFactory,
    context: ExecutionContext,
    owner: CarReviewDiagnosisAdmission.Owner,
    plan: CarReviewExecutionPlan,
    response: CarReviewCanonicalResponse
  ): CarReviewDiagnosisAdmission =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
    factory._complete_review_execution(_core(context), owner, plan, response)
    ))

  private def _report_snapshot_for_claimed_root(
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    report: CarReviewReport,
    attestation: CarReviewAttestation
  ): ReviewReportSnapshotCreate = {
    val document = CarReviewReportCodec.encode(report).fold(error => fail(error.message), identity)
    ReviewReportSnapshotCreate.Builder()
      .withDiagnosis_id(diagnosisid)
      .withReview_id(org.simplemodeling.textus.cbdsupport.value.ReviewId(report.reviewId.value))
      .withAttestation_id(org.simplemodeling.textus.cbdsupport.value.ReviewAttestationId(attestation.attestationId.value))
      .withReport_id(org.simplemodeling.textus.cbdsupport.value.ReviewReportId(report.reportId.value))
      .withReport_digest(org.simplemodeling.textus.cbdsupport.value.ReviewDigest(report.reportDigest.value))
      .withReport_document(ReviewDiagnosisEntityPrograms._json_document(document))
      .withCreated_at(org.simplemodeling.textus.cbdsupport.value.ReviewInstant(report.createdAt.value))
      .buildC().fold(error => fail(error.toString), identity)
      .copy(id = Some(_snapshot_id("report", diagnosisid, report.reportId.value, ReviewReportSnapshotEntity.collectionId)))
  }

  private def _production_execution(
    reviewid: String,
    targetdigest: Char
  )(using context: ExecutionContext): CarReviewProductionExecution =
    CarReviewProductionExecution.create(
      ReviewStartRequest(
        ReviewId(reviewid),
        ReviewTarget(
          ReviewTargetKind("car"),
          Some("org.simplemodeling"),
          "textus-cbd-support",
          Some(ReviewVersion("0.1.0-SNAPSHOT")),
          _digest(targetdigest)
        ),
        ReviewProfile("development"),
        ReviewInstant("2026-08-15T00:00:00Z")
      )
    ).fold(error => fail(error.toString), identity)

  private def _completed_lease(
    engine: InMemoryJobEngine,
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution
  )(using context: ExecutionContext): CarReviewProductionTerminalLease.Completed = {
    val binding = CarReviewProductionJobBinding.from(owner.diagnosisId, execution).fold(error => fail(error.toString), identity)
    val gateway = new CncfCarReviewJobGateway(engine)
    _value(gateway.submit(binding, execution))
    engine.drainAll()
    _value(gateway.findByReviewId(binding.reviewId)).flatMap(_.terminalLease) match {
      case Some(lease: CarReviewProductionTerminalLease.Completed) => lease
      case _ => fail("Expected a gateway-issued completed terminal lease.")
    }
  }

  private def _failed_lease(
    engine: InMemoryJobEngine,
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution
  )(using context: ExecutionContext): CarReviewProductionTerminalLease.Failed = {
    val binding = CarReviewProductionJobBinding.from(owner.diagnosisId, execution).fold(error => fail(error.toString), identity)
    val actionid = ActionId.create("cbd.review.persistence-spec", context.clock.instant(), context.idGeneration)
    _value(engine.submit(
      List(ReviewFailureJobTask(actionid, TaskFailed(Conclusion.simple("textus.cbd.review.failure.v1:provider-contract-failed")))),
      context,
      _job_option(binding)
    ))
    engine.drainAll()
    _value(new CncfCarReviewJobGateway(engine).findByReviewId(binding.reviewId)).flatMap(_.terminalLease) match {
      case Some(lease: CarReviewProductionTerminalLease.Failed) => lease
      case _ => fail("Expected a gateway-issued failed terminal lease.")
    }
  }

  private def _cancelled_lease(
    engine: InMemoryJobEngine,
    owner: CarReviewDiagnosisAdmission.Owner,
    execution: CarReviewProductionExecution
  )(using context: ExecutionContext): CarReviewProductionTerminalLease.Cancelled = {
    val binding = CarReviewProductionJobBinding.from(owner.diagnosisId, execution).fold(error => fail(error.toString), identity)
    val gateway = new CncfCarReviewJobGateway(engine)
    val jobid = _value(gateway.submit(binding, execution))
    _value(gateway.cancel(jobid))
    _value(gateway.findByReviewId(binding.reviewId)).flatMap(_.terminalLease) match {
      case Some(lease: CarReviewProductionTerminalLease.Cancelled) => lease
      case _ => fail("Expected a gateway-issued cancelled terminal lease.")
    }
  }

  private def _settle_completed(
    context: ExecutionContext,
    lease: CarReviewProductionTerminalLease.Completed
  ): CarReviewDiagnosisAdmission.Reused =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
      new ReviewDiagnosisAdmissionProgram(_core(context)).completeFromJob(lease)
    ))

  private def _settle_failed(
    context: ExecutionContext,
    lease: CarReviewProductionTerminalLease.Failed
  ): Unit =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
      new ReviewDiagnosisAdmissionProgram(_core(context)).recordTerminalFromJob(lease)
    ))

  private def _settle_cancelled(
    context: ExecutionContext,
    lease: CarReviewProductionTerminalLease.Cancelled
  ): Unit =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
      new ReviewDiagnosisAdmissionProgram(_core(context)).recordTerminalFromJob(lease)
    ))

  private def _settle_completed_concurrently(
    context: ExecutionContext,
    lease: CarReviewProductionTerminalLease.Completed
  ): Vector[CarReviewDiagnosisAdmission.Reused] = {
    val ready = new CountDownLatch(2)
    val start = new CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(2)
    try {
      val calls = Vector.fill(2)(executor.submit(new Callable[CarReviewDiagnosisAdmission.Reused] {
        def call(): CarReviewDiagnosisAdmission.Reused = {
          ready.countDown()
          if !start.await(10, TimeUnit.SECONDS) then
            throw new IllegalStateException("completion callers did not receive the start signal")
          _settle_completed(context, lease)
        }
      }))
      if !ready.await(10, TimeUnit.SECONDS) then
        throw new IllegalStateException("completion callers did not reach the concurrency barrier")
      start.countDown()
      calls.map(_.get(30, TimeUnit.SECONDS))
    } finally {
      start.countDown()
      executor.shutdownNow()
      executor.awaitTermination(10, TimeUnit.SECONDS)
    }
  }

  private def _same_reuse_successor(
    plan: CarReviewExecutionPlan,
    reviewid: String
  ): CarReviewExecutionPlan =
    CarReviewExecutionPlan.create(
      plan.request.copy(reviewId = ReviewId(reviewid)),
      plan.reuseInput
    ).fold(error => fail(error.message), identity)

  private def _job_option(binding: CarReviewProductionJobBinding): JobSubmitOption =
    JobSubmitOption(
      persistence = JobPersistencePolicy.Persistent,
      runMode = JobRunMode.Async,
      parameters = CarReviewProductionJobBinding.parameters(binding)
    )

  private def _load_snapshot[E](
    context: ExecutionContext,
    id: org.simplemodeling.model.datatype.EntityId
  )(using persistent: EntityPersistent[E]): E =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).interpret(
      UnitOfWorkOp.EntityStoreLoadDirect[E](id, persistent)
    )).headOption.getOrElse(fail(s"Expected persisted snapshot: ${id.print}"))

  private def _find_snapshot[E](
    context: ExecutionContext,
    id: org.simplemodeling.model.datatype.EntityId
  )(using persistent: EntityPersistent[E]): Option[E] =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).interpret(
      UnitOfWorkOp.EntityStoreLoadDirect[E](id, persistent)
    ))

  private def _run_concurrently(
    factory: ComponentFactory,
    context: ExecutionContext,
    plans: Vector[CarReviewExecutionPlan],
    continuationcount: AtomicInteger
  ): Vector[CarReviewDiagnosisAdmission] = {
    val ready = new CountDownLatch(plans.size)
    val start = new CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(plans.size)
    try {
      val calls = plans.map { plan =>
        executor.submit(new Callable[CarReviewDiagnosisAdmission] {
          def call(): CarReviewDiagnosisAdmission = {
            ready.countDown()
            if (!start.await(10, TimeUnit.SECONDS))
              throw new IllegalStateException(
                "successor callers did not receive the start signal"
              )
            _value(
              new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
                factory._admit_and_start_review_execution(
                  _core(context),
                  plan
                ) { _ =>
                  continuationcount.incrementAndGet()
                  ConsequenceT.pure[
                    [X] =>> Program[UnitOfWorkOp, X],
                    Unit
                  ](())
                }
              )
            )
          }
        })
      }
      if (!ready.await(10, TimeUnit.SECONDS))
        throw new IllegalStateException(
          "successor callers did not reach the concurrency barrier"
        )
      start.countDown()
      calls.map(_.get(30, TimeUnit.SECONDS))
    } finally {
      start.countDown()
      executor.shutdownNow()
      executor.awaitTermination(10, TimeUnit.SECONDS)
    }
  }

  private def _run_snapshots(
    context: ExecutionContext,
    plan: CarReviewExecutionPlan,
    successorid: ReviewId
  ): Vector[ReviewRunSnapshotEntity] = {
    val diagnosisid = _diagnosis_id(plan)
    Vector(
      _snapshot_id(
        "terminal-failed",
        diagnosisid,
        plan.request.reviewId.value,
        ReviewRunSnapshotEntity.collectionId
      ),
      _snapshot_id(
        "successor",
        diagnosisid,
        successorid.value,
        ReviewRunSnapshotEntity.collectionId
      )
    ).flatMap { id =>
      val operation =
        UnitOfWorkOp.EntityStoreLoadDirect[ReviewRunSnapshotEntity](
          id,
          summon[EntityPersistent[ReviewRunSnapshotEntity]]
        )
      _value(
        new UnitOfWorkInterpreter(new UnitOfWork(context))
          .interpret(operation)
      )
    }
  }

  private def _owner(admission: CarReviewDiagnosisAdmission): CarReviewDiagnosisAdmission.Owner =
    admission match {
      case owner: CarReviewDiagnosisAdmission.Owner => owner
      case other => fail(s"Expected owner admission, got $other")
    }

  private def _plan_for(report: CarReviewReport, reviewid: String): CarReviewExecutionPlan = {
    val provider = CarReviewReuseProviderSelection(
      ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
      ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")),
      _digest('b')
    )
    val input = CarReviewReuseKeyInput(
      CarReviewReuseKey.DEFINITION_ID,
      report.schemaVersion,
      report.target,
      report.profile,
      None,
      Vector(provider.ruleSet),
      Vector(provider),
      Vector(CarReviewReuseEvidenceSnapshot("runtime", "runtime-snapshot-main", ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")), _digest('c'))),
      Vector(
        CarReviewReusePolicyBinding("profile", "profile-development", ReviewVersion("1.0"), _digest('1')),
        CarReviewReusePolicyBinding("gate", "gate-default", ReviewVersion("1.0"), _digest('2')),
        CarReviewReusePolicyBinding("reconciliation", "reconciliation-default", ReviewVersion("1.0"), _digest('3')),
        CarReviewReusePolicyBinding("suppression", "suppression-default", ReviewVersion("1.0"), _digest('4'))
      )
    )
    CarReviewExecutionPlan.create(ReviewStartRequest(ReviewId(reviewid), report.target, report.profile, report.execution.startedAt), input).fold(error => fail(error.message), identity)
  }

  private def _sqlite_context(): ExecutionContext =
    _context(SqlDataStore.sqlite(":memory:"))

  private def _context(datastore: DataStore = DataStore.inMemorySearchable()): ExecutionContext = {
    val base = ExecutionContext.create()
    val component = _component()
    val datastorespace = new DataStoreSpace().useDataStore(datastore)
    val entitystorespace = new EntityStoreSpace().addEntityStore(EntityStore.standard())
    lazy val context: ExecutionContext = ExecutionContext.create(runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = ScopeContext.Core(
        kind = ScopeKind.Runtime,
        name = "cbd-review-diagnosis-persistence-spec",
        parent = None,
        observabilityContext = base.observability,
        httpDriverOption = None,
        datastore = Some(DataStoreContext(datastorespace)),
        entitystore = Some(EntityStoreContext(entitystorespace)),
        entityspace = Some(EntitySpaceContext(component.entitySpace)),
        aggregateInternalRead = false,
        processExecutionDriverOption = None,
        processExecutionAdmissionOption = None,
        scopedConcurrencyAdmissionOption = None
      ),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
      },
      commitAction = _ => (),
      abortAction = _ => (),
      disposeAction = _ => (),
      token = "cbd-review-diagnosis-persistence-spec"
    )
    runtime.setResolvedParameters(ResolvedParameters.empty())
    require(context.dataStoreSpace eq datastorespace, "review persistence test must use its configured DataStoreSpace")
    context
  }

  private def _core(context: ExecutionContext): ActionCall.Core =
    ActionCall.Core(
      TestAction(Request.ofOperation("cbd-review-diagnosis-persistence")),
      context,
      Some(_component()),
      None
    )

  private def _component(): Component = {
    val component = new Component {
      override val core: Component.Core = Component.Core.create(
        CbdSupportComponent.name,
        CbdSupportComponent.componentId,
        ComponentInstanceId.default(CbdSupportComponent.componentId),
        Protocol.empty
      )

      override def coreOption: Option[Component.Core] =
        Some(core)
    }
    component.entitySpace.registerEntity(
      ReviewDiagnosisEntity.collectionId.name,
      _simple_entity_collection(
        ReviewDiagnosisEntity.collectionId,
        summon[EntityPersistent[ReviewDiagnosisEntity]]
      )
    )
    component.entitySpace.registerEntity(
      ReviewTargetSnapshotEntity.collectionId.name,
      _simple_entity_collection(
        ReviewTargetSnapshotEntity.collectionId,
        summon[EntityPersistent[ReviewTargetSnapshotEntity]]
      )
    )
    component.entitySpace.registerEntity(
      ReviewRunSnapshotEntity.collectionId.name,
      _simple_entity_collection(
        ReviewRunSnapshotEntity.collectionId,
        summon[EntityPersistent[ReviewRunSnapshotEntity]]
      )
    )
    component.entitySpace.registerEntity(
      ReviewReportSnapshotEntity.collectionId.name,
      _simple_entity_collection(
        ReviewReportSnapshotEntity.collectionId,
        summon[EntityPersistent[ReviewReportSnapshotEntity]]
      )
    )
    component.entitySpace.registerEntity(
      ReviewAttestationSnapshotEntity.collectionId.name,
      _simple_entity_collection(
        ReviewAttestationSnapshotEntity.collectionId,
        summon[EntityPersistent[ReviewAttestationSnapshotEntity]]
      )
    )
    component.entitySpace.registerEntity(
      ReviewRetentionEventEntity.collectionId.name,
      _simple_entity_collection(
        ReviewRetentionEventEntity.collectionId,
        summon[EntityPersistent[ReviewRetentionEventEntity]]
      )
    )
    component
  }

  private def _simple_entity_collection[E](
    collectionid: org.simplemodeling.model.datatype.EntityCollectionId,
    persistent: EntityPersistent[E]
  ): EntityCollection[E] = {
    given EntityPersistent[E] = persistent
    val storerealm = new EntityRealm[E](
      entityName = collectionid.name,
      loader = EntityLoader[E](_ => None),
      state = new IdRef(EntityRealmState(Map.empty))
    )
    val descriptor = EntityDescriptor(
      collectionId = collectionid,
      plan = EntityRuntimePlan(
        entityName = collectionid.name,
        memoryPolicy = EntityMemoryPolicy.StoreOnly,
        workingSet = None,
        partitionStrategy = PartitionStrategy.byEntityId,
        maxPartitions = 1,
        maxEntitiesPerPartition = 1
      ),
      persistent = persistent,
      revisionBinding = Some(
        EntityRevisionBinding(EntityRevisionRepresentation.Embedded)
      )
    )
    new EntityCollection(
      descriptor,
      EntityStorage(storerealm)
    )
  }

  private def _viewer_context(context: ExecutionContext): ExecutionContext =
    ExecutionContext.withSecurityContext(
      context,
      context.security.copy(
        capabilities = context.security.capabilities + org.goldenport.cncf.context.Capability("viewer"),
        level = org.goldenport.cncf.context.SecurityLevel("viewer")
      )
    )

  private def _operator_context(context: ExecutionContext): ExecutionContext =
    ExecutionContext.withSecurityContext(
      context,
      context.security.copy(
        capabilities = context.security.capabilities + org.goldenport.cncf.context.Capability("operator"),
        level = org.goldenport.cncf.context.SecurityLevel("operator")
      )
    )

  private def _plan(reviewid: String, targetdigest: Char = 'a'): CarReviewExecutionPlan = {
    val target = ReviewTarget(
      ReviewTargetKind("car"),
      Some("org.textus"),
      "textus-cbd-support",
      Some(ReviewVersion("0.1.0-SNAPSHOT")),
      _digest(targetdigest)
    )
    val provider = CarReviewReuseProviderSelection(
      ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
      ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")),
      _digest('b')
    )
    val input = CarReviewReuseKeyInput(
      CarReviewReuseKey.DEFINITION_ID,
      ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      target,
      ReviewProfile("development"),
      None,
      Vector(provider.ruleSet),
      Vector(provider),
      Vector(CarReviewReuseEvidenceSnapshot(
        "runtime",
        "runtime-snapshot-main",
        ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")),
        _digest('c')
      )),
      Vector(
        CarReviewReusePolicyBinding("profile", "profile-development", ReviewVersion("1.0"), _digest('1')),
        CarReviewReusePolicyBinding("gate", "gate-default", ReviewVersion("1.0"), _digest('2')),
        CarReviewReusePolicyBinding("reconciliation", "reconciliation-default", ReviewVersion("1.0"), _digest('3')),
        CarReviewReusePolicyBinding("suppression", "suppression-default", ReviewVersion("1.0"), _digest('4'))
      )
    )
    CarReviewExecutionPlan.create(
      ReviewStartRequest(ReviewId(reviewid), target, ReviewProfile("development"), ReviewInstant("2026-07-23T00:00:00Z")),
      input
    ).fold(error => fail(s"${error.code}: ${error.message}"), identity)
  }

  private def _digest(character: Char): ReviewDigest =
    ReviewDigest("sha256:" + character.toString * 64)

  private def _diagnosis_id(plan: CarReviewExecutionPlan): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${plan.reuseKey.definitionId}:${plan.reuseKey.digest.value}"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    val collection =
      org.simplemodeling.textus.cbdsupport.entity.ReviewDiagnosis.collectionId
    org.simplemodeling.model.datatype.EntityId(
      collection.major,
      collection.minor,
      collection,
      timestamp = Some(org.goldenport.id.UniversalId.StableTimestamp),
      entropy = Some(key)
    )
  }

  private def _snapshot_id(
    kind: String,
    diagnosis: org.simplemodeling.model.datatype.EntityId,
    identity: String,
    collection: org.simplemodeling.model.datatype.EntityCollectionId
  ): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${diagnosis.value}:$kind:$identity"
    val key =
      "d" + UUID
        .nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8))
        .toString
        .replace("-", "")
    org.simplemodeling.model.datatype.EntityId(
      collection.major,
      collection.minor,
      collection,
      timestamp = Some(org.goldenport.id.UniversalId.StableTimestamp),
      entropy = Some(key)
    )
  }

  private def _value[A](consequence: Consequence[A]): A = consequence match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => fail(s"${conclusion.display} / ${conclusion.show} / $conclusion")
  }

  private final class IdRef[A](initial: A) extends Ref[Id, A] {
    private var _value: A = initial

    def get: A =
      synchronized(_value)

    def set(value: A): Unit =
      synchronized {
        _value = value
      }

    override def getAndSet(value: A): A =
      synchronized {
        val previous = _value
        _value = value
        previous
      }

    def access: (A, A => Boolean) =
      synchronized {
        val snapshot = _value
        val setter: A => Boolean = next =>
          synchronized {
            if (_value == snapshot) {
              _value = next
              true
            } else {
              false
            }
          }
        snapshot -> setter
      }

    override def tryUpdate(f: A => A): Boolean =
      synchronized {
        _value = f(_value)
        true
      }

    override def tryModify[B](f: A => (A, B)): Option[B] =
      synchronized {
        val (next, result) = f(_value)
        _value = next
        Some(result)
      }

    def update(f: A => A): Unit =
      synchronized {
        _value = f(_value)
      }

    def modify[B](f: A => (A, B)): B =
      synchronized {
        val (next, result) = f(_value)
        _value = next
        result
      }

    override def modifyState[B](state: State[A, B]): B =
      synchronized {
        val (next, result) = state.run(_value).value
        _value = next
        result
      }

    override def tryModifyState[B](state: State[A, B]): Option[B] =
      synchronized {
        val (next, result) = state.run(_value).value
        _value = next
        Some(result)
      }
  }

  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall = {
      val _ = core
      throw new UnsupportedOperationException("Test fixture exposes ActionCall.Core only.")
    }
  }
}

private final case class ReviewFailureJobTask(
  actionId: ActionId,
  outcome: TaskOutcome
) extends JobTask {
  override def componentName: Option[String] = Some("CbdSupportSpec")
  override def serviceName: Option[String] = Some("CbdReviewPersistenceSpec")
  override def operationName: Option[String] = Some("fixed-failure")

  def run(context: ExecutionContext): TaskOutcome = {
    val _ = context
    outcome
  }
}
