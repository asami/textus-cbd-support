package org.simplemodeling.textus.cbdsupport.impl

import java.nio.file.{Files, Path}
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
import org.goldenport.cncf.directive.Query
import org.simplemodeling.model.directive.Condition
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
  EntityQuery,
  EntityPersistent,
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
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
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

/*
 * @since   Jul. 23, 2026
 *  version Jul. 26, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class ReviewDiagnosisPersistenceSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "CAR Review diagnosis persistence" should {
    "claim one stable Entity diagnosis and join a duplicate reusable execution" in {
      Given("two executions with one explicitly declared native plan reference and complete selection")
      val factory = new ComponentFactory()
      val context = _sqlite_context()
      val firstplan = _plan("review-owner")
      val duplicateplan = _plan("review-joiner")
      val reorderedplan = _plan("review-reordered").copy(reuseInput = firstplan.reuseInput.copy(policyBindings = firstplan.reuseInput.policyBindings.reverse))
      val startcount = new AtomicInteger(0)

      When("both executions are admitted through separate UnitOfWork boundaries")
      val first = _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._admit_and_start_review_execution(_core(context), firstplan) { _ =>
          startcount.incrementAndGet()
          ConsequenceT.pure[[X] =>> Program[UnitOfWorkOp, X], Unit](())
        }))
      val duplicate = _run(new ComponentFactory(), context, duplicateplan)
      val reordered = _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        new ComponentFactory()._admit_and_start_review_execution(_core(context), reorderedplan) { _ =>
          startcount.incrementAndGet()
          ConsequenceT.pure[[X] =>> Program[UnitOfWorkOp, X], Unit](())
        }))
      val owner = _owner(first)

      Then("the first execution owns the Aggregate and the duplicate joins it")
      first shouldBe a[CarReviewDiagnosisAdmission.Owner]
      owner.reviewId shouldBe ReviewId("review-owner")
      duplicate shouldBe CarReviewDiagnosisAdmission.Joined(first.diagnosisId, ReviewId("review-owner"))
      reordered shouldBe duplicate
      owner.reference shouldBe firstplan.reference
      owner.diagnosisId shouldBe firstplan.reference.diagnosisId.print
      startcount.get shouldBe 1
    }

    "refuse persisted plan revision and typed dependency conflicts before joining or starting" in {
      Given("a claimed root with its declared revision and complete persisted selection")
      val factory = new ComponentFactory()
      val context = _sqlite_context()
      val plan = _plan("review-conflict-owner")
      val owner = _owner(_run(factory, context, plan))
      val before = _root(context, plan)
      val changedrevision = plan.copy(reference = plan.reference.copy(revision = 2L))
      val changedselection = plan.copy(reuseInput = plan.reuseInput.copy(targetReference = plan.reuseInput.targetReference.copy(revision = 2L)))
      val wrongcollection = plan.copy(reference = CarReviewExecutionPlanReference(
        _value(org.simplemodeling.model.datatype.EntityId.parse("major-minor-entity-ec1_5_major_5_minor_19_review_run_snapshot-0-foreignPlan")), 1L))
      val starts = new AtomicInteger(0)

      When("fresh factories and UnitOfWorks attempt conflicting revision, selection or collection admission")
      val results = Vector(changedrevision, changedselection, wrongcollection).map { selected =>
        new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          new ComponentFactory()._admit_and_start_review_execution(_core(context), selected) { _ =>
            starts.incrementAndGet()
            ConsequenceT.pure[[X] =>> Program[UnitOfWorkOp, X], Unit](())
          })
      }
      val after = _root(context, plan)
      val joined = _run(new ComponentFactory(), context, _plan("review-after-conflicts"))

      Then("attributed conflicts leave the original root, issued owner and selected data unchanged")
      results.zip(Vector("review-plan-revision-conflict", "review-plan-selection-inconsistent", "review-plan-reference-invalid")).foreach { case (result, code) =>
        _failure_text(result) should include(code)
      }
      starts.get shouldBe 0
      after shouldBe before
      joined shouldBe CarReviewDiagnosisAdmission.Joined(owner.diagnosisId, owner.reviewId)
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
      val completion = _run_completion(factory, context, owner, ownerplan, CarReviewCanonicalResponse(report, report.gate, attestation))
      val repeated = _run(new ComponentFactory(), context, repeatedplan)
      val targets = _targets(context, ownerplan)
      val runs = _runs(context, ownerplan)
      val reports = _reports(context, ownerplan)
      val attestations = _attestations(context, ownerplan)

      Then("both responses reuse the persisted canonical Report identity")
      _value(org.simplemodeling.model.datatype.EntityId.parse(owner.diagnosisId)) shouldBe ownerplan.reference.diagnosisId
      targets should have size 1
      runs should have size 1
      reports should have size 1
      attestations should have size 1
      Vector(targets.head.id, runs.head.id, reports.head.id, attestations.head.id).distinct should have size 4
      Vector(targets.head.id, runs.head.id, reports.head.id, attestations.head.id) should not contain ownerplan.reference.diagnosisId
      targets.head.diagnosis_id shouldBe ownerplan.reference.diagnosisId
      runs.head.diagnosis_id shouldBe ownerplan.reference.diagnosisId
      reports.head.diagnosis_id shouldBe ownerplan.reference.diagnosisId
      attestations.head.diagnosis_id shouldBe ownerplan.reference.diagnosisId
      targets.head.id.collection shouldBe ReviewTargetSnapshotEntity.collectionId
      runs.head.id.collection shouldBe ReviewRunSnapshotEntity.collectionId
      reports.head.id.collection shouldBe ReviewReportSnapshotEntity.collectionId
      attestations.head.id.collection shouldBe ReviewAttestationSnapshotEntity.collectionId
      reports.head.review_id.value shouldBe report.reviewId.value
      reports.head.attestation_id.value shouldBe attestation.attestationId.value
      attestations.head.report_id.value shouldBe report.reportId.value
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
        new ComponentFactory()._load_persisted_review_report(_core(viewercontext), report.reportId)
      ))
      val missing = new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._load_persisted_review_report(_core(viewercontext), ReviewReportId("report-missing"))
      )
      val denied = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._load_persisted_review_report(_core(context), report.reportId)
      )

      Then("the Entity record, not an in-memory repository, is the exact bounded source")
      loaded.reportId shouldBe report.reportId
      loaded.reportDigest shouldBe report.reportDigest
      loaded.reviewId shouldBe report.reviewId
      missing shouldBe a[Consequence.Failure[_]]
      denied shouldBe a[Consequence.Failure[_]]
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

      val beforeattestations = _attestations(context, plan)
      val beforereports = _reports(context, plan)

      When("an operator expires the exact payload after its retention age")
      val denied = new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        factory._expire_persisted_review_report(_core(viewercontext), report.reportId, ReviewInstant("2026-12-16T00:02:01Z"))
      )
      val early = new UnitOfWorkInterpreter(new UnitOfWork(operatorcontext)).run(
        factory._expire_persisted_review_report(_core(operatorcontext), report.reportId, report.createdAt)
      )
      val event = _value(new UnitOfWorkInterpreter(new UnitOfWork(operatorcontext)).run(
        factory._expire_persisted_review_report(_core(operatorcontext), report.reportId, ReviewInstant("2026-12-16T00:02:01Z"))
      ))
      val afterexpiry = new UnitOfWorkInterpreter(new UnitOfWork(viewercontext)).run(
        new ComponentFactory()._load_persisted_review_report(_core(viewercontext), report.reportId)
      )
      val repeated = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._admit_review_execution(_core(context), _plan_for(report, "review-after-expiry"))
      )

      Then("only an authorized tombstone remains, payload reads fail, and a fresh successor owns the next execution")
      denied shouldBe a[Consequence.Failure[_]]
      _failure_text(early) should include("review-retention-not-due")
      event.id shouldBe None
      event.record_id shouldBe beforereports.head.id
      _attestations(context, plan) shouldBe Vector.empty
      _reports(context, plan) shouldBe Vector.empty
      val tombstones = _retention_events(context, plan)
      tombstones should have size 2
      tombstones.map(_.record_id).toSet shouldBe (beforeattestations.map(_.id) ++ beforereports.map(_.id)).toSet
      tombstones.map(_.id).distinct should have size 2
      tombstones.foreach(_.diagnosis_id shouldBe plan.reference.diagnosisId)
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
      ).zip(Vector(_failed_reference, _cancelled_reference, _expired_reference, _incompatible_reference))

      When("each diagnosis records its terminal Run through the Aggregate boundary")
      val results = states.map { case (state, reference) =>
        val plan = _plan(s"review-${state.value}", reference)
        val owner = _owner(_run(factory, context, plan))
        val terminal = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          factory._record_terminal_review_execution(_core(context), owner, plan, state, s"{\"state\":\"${state.value}\"}", ReviewInstant("2026-07-23T01:00:00Z")))
        val successorid = ReviewId(s"review-after-${state.value}")
        val result = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          factory._admit_review_execution(_core(context), _plan(successorid.value, reference)))
        (state, plan, terminal, successorid, result)
      }

      Then("no terminal diagnosis is reused and every request owns a fresh successor")
      results.foreach { case (state, plan, terminal, successorid, result) =>
        withClue(s"${state.value} must retain its history and admit the requested successor") {
          _value(terminal) shouldBe ()
          _owner(_value(result)).reviewId shouldBe successorid
          val snapshots = _run_snapshots(context, plan, successorid)
          snapshots.map(_.state.value).toSet shouldBe Set(state.value, "admitted")
          snapshots.map(_.id).distinct should have size 2
        }
      }
    }

    "install one authoritative successor and start its work once" in {
      forAll(Gen.choose(2, 8)) { generatedcallercount =>
        Given("a retained terminal Run and two through eight simultaneous declared same-plan successor requests")
        val callercount = generatedcallercount.max(2)
        val factory = new ComponentFactory()
        val context = _context()
        val terminalplan = _plan("review-terminal-predecessor", _successor_reference)
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
            _plan(s"review-successor-$index", _successor_reference)
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
        snapshots.map(_.id).distinct should have size 2
        snapshots.foreach(_.diagnosis_id shouldBe terminalplan.reference.diagnosisId)
        snapshots.find(_.state.value == "failed").map(_.run_document.value.value) shouldBe Some("""{"state":"failed","failure":"provider"}""")
        owners.head.reference shouldBe terminalplan.reference
      }
    }

    "require the issued Owner lease before a claimed Aggregate can transition" in {
      Given("an Aggregate claimed by one issued Owner lease")
      val factory = new ComponentFactory()
      val context = _context()
      val ownerplan = _plan("review-lease-owner", _lease_reference)
      val otherplan = _plan("review-lease-other", _lease_reference)
      val owner = _owner(_run(factory, context, ownerplan))

      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)
      val reportplan = _plan_for(report, report.reviewId.value)
      val reportowner = _owner(_run(factory, context, reportplan))
      val attestation = CarReviewAttestationCodec.fromReport(report).fold(error => fail(error.message), identity)
      val foreignowner = CarReviewDiagnosisAdmission.Owner.issue(reportowner.diagnosisId, report.reviewId, _lease_reference)

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

      val completion = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._complete_review_execution(_core(context), foreignowner, reportplan, CarReviewCanonicalResponse(report, report.gate, attestation)))

      Then("the Aggregate boundary rejects foreign Review IDs and reference leases without writing snapshots")
      _failure_text(result) should include("review-diagnosis-terminal-not-owner")
      _failure_text(completion) should include("review-diagnosis-completion-not-owner")
      _runs(context, ownerplan) shouldBe Vector.empty
      _reports(context, reportplan) shouldBe Vector.empty
      _attestations(context, reportplan) shouldBe Vector.empty
      _targets(context, reportplan) shouldBe Vector.empty
      _runs(context, reportplan) shouldBe Vector.empty
    }

    "read the persisted terminal Aggregate state before a repeat transition" in {
      Given("an owned diagnosis that will be retained as cancelled")
      val factory = new ComponentFactory()
      val context = _context()
      val plan = _plan("review-terminal-replay", _replay_reference)
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

      val successorplan = _plan("review-after-replay", _replay_reference)
      val successor = _owner(_run(new ComponentFactory(), context, successorplan))
      val stale = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._record_terminal_review_execution(_core(context), owner, plan, CarReviewDiagnosisTerminalState.Cancelled,
          "{\"state\":\"cancelled\"}", ReviewInstant("2026-07-23T01:00:02Z")))

      Then("persisted state refuses repeat and stale-owner transitions and retains only predecessor and successor")
      _value(first) shouldBe ()
      _failure_text(repeated) should include("review-diagnosis-terminal-not-owner")
      _failure_text(stale) should include("review-diagnosis-terminal-not-owner")
      _runs(context, plan) should have size 2
      successor.reference shouldBe plan.reference
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
    Vector(plan.request.reviewId, successorid).flatMap { reviewid =>
      val condition = org.simplemodeling.textus.cbdsupport.entity.query.ReviewRunSnapshot.create()
        .withDiagnosis_id(Condition.is(diagnosisid))
        .withReview_id(Condition.is(org.simplemodeling.textus.cbdsupport.value.ReviewId(reviewid.value)))
      // Native Eq preserves both declared IDs when the generated ReviewId serializes as a nested record.
      val records = _search(context, EntityQuery[ReviewRunSnapshotEntity](ReviewRunSnapshotEntity.collectionId, Query(Query.Plan(condition.toRecord(), where = Query.And(Vector(Query.Eq("diagnosis_id", diagnosisid), Query.Eq("review_id", org.simplemodeling.textus.cbdsupport.value.ReviewId(reviewid.value)))), limit = Some(2)))))
      records should have size 1
      records
    }
  }


  private def _owner(admission: CarReviewDiagnosisAdmission): CarReviewDiagnosisAdmission.Owner =
    admission match {
      case owner: CarReviewDiagnosisAdmission.Owner => owner
      case other => fail(s"Expected owner admission, got $other")
    }

  private def _plan_for(report: CarReviewReport, reviewid: String): CarReviewExecutionPlan =
    _make_plan(ReviewStartRequest(ReviewId(reviewid), report.target, report.profile, report.execution.startedAt), _report_reference)

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

  private def _plan(reviewid: String, reference: CarReviewExecutionPlanReference = _main_reference): CarReviewExecutionPlan =
    _make_plan(ReviewStartRequest(ReviewId(reviewid), ReviewTarget(ReviewTargetKind("car"), Some("org.textus"), "textus-cbd-support",
      Some(ReviewVersion("0.1.0-SNAPSHOT")), _digest('a')), ReviewProfile("development"), ReviewInstant("2026-07-23T00:00:00Z")), reference)

  private def _make_plan(request: ReviewStartRequest, reference: CarReviewExecutionPlanReference): CarReviewExecutionPlan = {
    val provider = CarReviewReuseProviderSelection(ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
      ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")), CarReviewDeclaredReference("availability:cozy", 1L))
    val input = CarReviewReuseKeyInput(CarReviewReuseKey.DEFINITION_ID, ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      CarReviewDeclaredReference("target:main", 1L), request.target.kind, request.target.organization, request.target.name, request.target.version,
      request.profile, None, Vector(provider.ruleSet), Vector(provider),
      Vector(CarReviewReuseEvidenceSnapshot("runtime", CarReviewDeclaredReference("runtime:main", 1L), ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")))),
      Vector("profile", "gate", "reconciliation", "suppression").map(scope => CarReviewReusePolicyBinding(scope, scope + "-default", ReviewVersion("1.0"), 1L)))
    CarReviewExecutionPlan.create(request, reference, input).fold(error => fail(error.message), identity)
  }

  private def _digest(character: Char): ReviewDigest =
    ReviewDigest("sha256:" + character.toString * 64)

  private def _diagnosis_id(plan: CarReviewExecutionPlan): org.simplemodeling.model.datatype.EntityId =
    plan.reference.diagnosisId

  private def _declared_reference(value: String): CarReviewExecutionPlanReference =
    CarReviewExecutionPlanReference(_value(org.simplemodeling.model.datatype.EntityId.parse(value)), 1L)

  private def _main_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planMain")
  private def _report_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planReport")
  private def _failed_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planFailed")
  private def _cancelled_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planCancelled")
  private def _expired_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planExpired")
  private def _incompatible_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planIncompatible")
  private def _successor_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planSuccessor")
  private def _lease_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planLease")
  private def _replay_reference = _declared_reference("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-planReplay")

  private def _root(context: ExecutionContext, plan: CarReviewExecutionPlan): ReviewDiagnosisEntity =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).interpret(
      UnitOfWorkOp.EntityStoreLoadDirect[ReviewDiagnosisEntity](plan.reference.diagnosisId, summon[EntityPersistent[ReviewDiagnosisEntity]]))).getOrElse(fail("Declared root is missing"))

  private def _search[E: EntityPersistent](context: ExecutionContext, query: EntityQuery[E]): Vector[E] =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).interpret(
      UnitOfWorkOp.EntityStoreSearchInternal(query, summon[EntityPersistent[E]]))).data

  private def _targets(context: ExecutionContext, plan: CarReviewExecutionPlan): Vector[ReviewTargetSnapshotEntity] = {
    val condition = org.simplemodeling.textus.cbdsupport.entity.query.ReviewTargetSnapshot.create().withDiagnosis_id(Condition.is(plan.reference.diagnosisId))
    _search(context, EntityQuery[ReviewTargetSnapshotEntity](ReviewTargetSnapshotEntity.collectionId, Query(Query.Plan(condition.toRecord(), limit = Some(3)))))
  }

  private def _runs(context: ExecutionContext, plan: CarReviewExecutionPlan): Vector[ReviewRunSnapshotEntity] = {
    val condition = org.simplemodeling.textus.cbdsupport.entity.query.ReviewRunSnapshot.create().withDiagnosis_id(Condition.is(plan.reference.diagnosisId))
    _search(context, EntityQuery[ReviewRunSnapshotEntity](ReviewRunSnapshotEntity.collectionId, Query(Query.Plan(condition.toRecord(), limit = Some(3)))))
  }

  private def _reports(context: ExecutionContext, plan: CarReviewExecutionPlan): Vector[ReviewReportSnapshotEntity] = {
    val condition = org.simplemodeling.textus.cbdsupport.entity.query.ReviewReportSnapshot.create().withDiagnosis_id(Condition.is(plan.reference.diagnosisId))
    _search(context, EntityQuery[ReviewReportSnapshotEntity](ReviewReportSnapshotEntity.collectionId, Query(Query.Plan(condition.toRecord(), limit = Some(3)))))
  }

  private def _attestations(context: ExecutionContext, plan: CarReviewExecutionPlan): Vector[ReviewAttestationSnapshotEntity] = {
    val condition = org.simplemodeling.textus.cbdsupport.entity.query.ReviewAttestationSnapshot.create().withDiagnosis_id(Condition.is(plan.reference.diagnosisId))
    _search(context, EntityQuery[ReviewAttestationSnapshotEntity](ReviewAttestationSnapshotEntity.collectionId, Query(Query.Plan(condition.toRecord(), limit = Some(3)))))
  }

  private def _retention_events(context: ExecutionContext, plan: CarReviewExecutionPlan): Vector[ReviewRetentionEventEntity] = {
    val condition = org.simplemodeling.textus.cbdsupport.entity.query.ReviewRetentionEvent.create().withDiagnosis_id(Condition.is(plan.reference.diagnosisId))
    _search(context, EntityQuery[ReviewRetentionEventEntity](ReviewRetentionEventEntity.collectionId, Query(Query.Plan(condition.toRecord(), limit = Some(3)))))
  }

  private def _failure_text[A](result: Consequence[A]): String = result match {
    case Consequence.Failure(conclusion) => conclusion.display
    case Consequence.Success(_) => fail("Expected attributed refusal")
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
