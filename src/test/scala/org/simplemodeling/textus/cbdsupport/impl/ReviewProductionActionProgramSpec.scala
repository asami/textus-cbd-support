package org.simplemodeling.textus.cbdsupport.impl

import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import cats.{Id, ~>}
import cats.data.State
import cats.effect.Ref
import org.goldenport.Consequence
import org.goldenport.cncf.Program
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInstanceId}
import org.goldenport.cncf.config.ResolvedParameters
import org.goldenport.cncf.context.{Capability, DataStoreContext, EntitySpaceContext, EntityStoreContext, ExecutionContext, RuntimeContext, ScopeContext, ScopeKind, SecurityLevel}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntityRevisionBinding, EntityRevisionRepresentation, EntityStore, EntityStoreSpace}
import org.goldenport.cncf.entity.runtime.{EntityCollection, EntityDescriptor, EntityLoader, EntityMemoryPolicy, EntityRealm, EntityRealmState, EntityRuntimePlan, EntityStorage, PartitionStrategy}
import org.goldenport.cncf.job.{InMemoryJobEngine, JobStatus}
import org.goldenport.cncf.unitofwork.{ExecUowM, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.protocol.{Protocol, Request}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.entity.{ReviewAttestationSnapshot as ReviewAttestationSnapshotEntity, ReviewDiagnosis as ReviewDiagnosisEntity, ReviewReportSnapshot as ReviewReportSnapshotEntity, ReviewRunSnapshot as ReviewRunSnapshotEntity, ReviewTargetSnapshot as ReviewTargetSnapshotEntity}
import org.simplemodeling.textus.cbdsupport.entity.create.{ReviewRunSnapshot as ReviewRunSnapshotCreate}
import org.simplemodeling.textus.cbdsupport.runtime.*
import org.simplemodeling.textus.cbdsupport.value.{ReviewId as EntityReviewId, ReviewInstant as EntityReviewInstant, ReviewProfile as EntityReviewProfile, ReviewRunState as EntityReviewRunState}

import scala.concurrent.{Await, ExecutionContext as ScalaExecutionContext, Future}
import scala.concurrent.duration.Duration

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class ReviewProductionActionProgramSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  "The production Review action program" should {
    "admit, settle, and retry production Jobs" which {
    "submit one owner Job and bind duplicate reuse to its exact queued Job" in {
      Given("an operator Entity context and a stopped persistent CNCF Job engine")
      val fixture = _fixture("operator")

      try {
        When("two server-generated Review identities start the same production definition")
        val first = _value(_run(fixture, _program(fixture).start(_request("review-production-owner"))))
        val duplicate = _value(_run(fixture, _program(fixture).start(_request("review-production-joined"))))

        Then("only the first caller submits and the duplicate joins the exact Job binding")
        first.run.state shouldBe ReviewRunState("queued")
        duplicate.run.state shouldBe ReviewRunState("queued")
        duplicate.run.reviewId shouldBe first.run.reviewId
        duplicate.binding.jobId shouldBe first.binding.jobId
        _job_count(fixture) shouldBe 1
      } finally fixture.engine.shutdown()
    }

    "lazily settle a completed Job for get and reuse the original completed binding" in {
      Given("one queued operator Review Job and a viewer sharing its Entity state")
      val fixture = _fixture("operator")

      try {
        val owner = _value(_run(fixture, _program(fixture).start(_request("review-production-completed"))))
        fixture.engine.drainAll()
        val viewer = _with_role(fixture.context, "viewer")
        val viewerfixture = fixture.copy(context = viewer)

        When("the viewer gets the terminal Job and a same-key caller starts again")
        val completed = _value(_run(viewerfixture, _program(viewerfixture).get(owner.run.reviewId)))
        val reused = _value(_run(fixture, _program(fixture).start(_request("review-production-reused"))))
        val record = new ComponentFactory()._review_run_record(reused)

        Then("get settles the Entity Report and every public binding still names the original Job")
        completed.run.state shouldBe ReviewRunState("completed")
        reused.run.state shouldBe ReviewRunState("completed")
        reused.binding.jobId shouldBe owner.binding.jobId
        _job_count(fixture) shouldBe 1
        record.keySet shouldBe Set(
          "schemaVersion", "documentType", "reviewId", "jobId", "targetKind",
          "organization", "name", "version", "targetDigest", "profile", "state",
          "limitations", "startedAt", "updatedAt", "completedAt", "reportId",
          "reportDigest"
        )
        record.getString("reviewId") shouldBe Some(owner.run.reviewId.value)
        record.getString("jobId") shouldBe Some(owner.binding.jobId.value)
        record.getString("state") shouldBe Some("completed")
      } finally fixture.engine.shutdown()
    }

    "settle a cancelled joined Job once and submit exactly one successor" in {
      Given("one claimed production Job whose worker has not run")
      val fixture = _fixture("operator")

      try {
        val owner = _value(_run(fixture, _program(fixture).start(_request("review-production-cancelled-owner"))))
        val gateway = new CncfCarReviewJobGateway(fixture.engine)
        given ExecutionContext = fixture.context
        _value(gateway.cancel(owner.binding.jobId))

        When("a same-key caller joins after the exact terminal cancellation")
        val successor = _value(_run(fixture, _program(fixture).start(_request("review-production-cancelled-successor"))))

        Then("the stale terminal lease is retained and only the successor is newly queued")
        successor.run.reviewId shouldBe ReviewId("review-production-cancelled-successor")
        successor.run.state shouldBe ReviewRunState("queued")
        successor.binding.jobId should not be owner.binding.jobId
        _job_count(fixture) shouldBe 2
      } finally fixture.engine.shutdown()
    }

    "make a duplicate claim retryable while the owner is still submitting its Job" in {
      Given("one owner blocked by the production Job port immediately after the Entity claim")
      val fixture = _fixture("operator")
      val gateway = new CncfCarReviewJobGateway(fixture.engine)
      val entered = new CountDownLatch(1)
      val release = new CountDownLatch(1)
      val port = new _BlockingSubmitPort(gateway, entered, release)
      val ownerrequest = _request("review-production-pending-owner")
      val duplicaterequest = _request("review-production-pending-joined")
      given ScalaExecutionContext = ScalaExecutionContext.global

      try {
        When("the duplicate arrives before the owner has made its Job discoverable")
        val ownercall = Future(_run(fixture, _program(fixture, port).start(ownerrequest)))
        entered.await(30, TimeUnit.SECONDS) shouldBe true
        val pending = _run(fixture, _program(fixture, port).start(duplicaterequest))
        release.countDown()
        val owner = _value(Await.result(ownercall, Duration(30, TimeUnit.SECONDS)))
        val joined = _value(_run(fixture, _program(fixture, port).start(duplicaterequest)))

        Then("the transient caller receives the explicit pending result and its retry joins one queued Job")
        pending shouldBe a[Consequence.Failure[_]]
        pending.fold(
          error => error.toString should include ("review-job-submission-pending"),
          _ => fail("duplicate unexpectedly bypassed the pending submission boundary")
        )
        owner.run.state shouldBe ReviewRunState("queued")
        joined.run.state shouldBe ReviewRunState("queued")
        joined.binding.jobId shouldBe owner.binding.jobId
        port.submitcount.get shouldBe 1
        _job_count(fixture) shouldBe 1
      } finally {
        release.countDown()
        fixture.engine.shutdown()
      }
    }

    "reconcile an accepted Job when the submit acknowledgement is lost" in {
      Given("a port that accepts through the real gateway and then returns a synthetic submit failure")
      val fixture = _fixture("operator")
      val gateway = new CncfCarReviewJobGateway(fixture.engine)
      val port = new _AcceptedThenFailurePort(gateway)
      val request = _request("review-production-submit-ack-lost")
      given ExecutionContext = fixture.context
      val execution = _value(CarReviewProductionExecution.create(request))

      try {
        When("the owner starts the production Review")
        val admitted = _value(_run(fixture, _program(fixture, port).start(request)))
        val root = _load_snapshot[ReviewDiagnosisEntity](fixture.context, _diagnosis_id(execution.plan))

        Then("exact reuse discovery returns the already accepted Job and preserves the claim")
        admitted.run.state shouldBe ReviewRunState("queued")
        admitted.run.reviewId shouldBe request.reviewId
        root.state.value shouldBe "claimed"
        port.submitcount.get shouldBe 1
        _job_count(fixture) shouldBe 1
      } finally fixture.engine.shutdown()
    }

    "retain a legal failed Run only when submit reconciliation reliably finds no Job" in {
      Given("a claimed owner whose port returns a stable submit failure and reliable empty discovery")
      val fixture = _fixture("operator")
      val port = new _SubmitFailureNoDiscoveryPort
      val request = _request("review-production-submit-failed")
      given ExecutionContext = fixture.context
      val execution = _value(CarReviewProductionExecution.create(request))

      try {
        When("the owner cannot submit and reconciliation proves that no Job exists")
        val failed = _run(fixture, _program(fixture, port).start(request))
        val diagnosisid = _diagnosis_id(execution.plan)
        val root = _load_snapshot[ReviewDiagnosisEntity](fixture.context, diagnosisid)
        val terminal = _load_snapshot[ReviewRunSnapshotEntity](
          fixture.context,
          _snapshot_id("terminal-failed", diagnosisid, request.reviewId.value, ReviewRunSnapshotEntity.collectionId)
        )
        val decoded = CarReviewRunCodec.decode(terminal.run_document.value.value).fold(error => fail(error.message), identity)
        val successor = _value(_run(
          fixture,
          _program(fixture).start(_request("review-production-submit-failed-successor"))
        ))

        Then("the original failure remains primary, the retained terminal Run is complete, and a successor can be admitted")
        failed shouldBe a[Consequence.Failure[_]]
        failed.fold(
          error => error.toString should include ("synthetic-review-job-submit-failed"),
          _ => fail("synthetic submit failure unexpectedly succeeded")
        )
        root.state.value shouldBe "failed"
        terminal.state.value shouldBe "failed"
        decoded.state shouldBe ReviewRunState("failed")
        decoded.failureCode shouldBe Some(ReviewFailureCode("review-job-submit-failed"))
        decoded.reportId shouldBe None
        decoded.reportDigest shouldBe None
        decoded.completedAt should not be empty
        java.time.Instant.parse(decoded.completedAt.get.value)
          .isBefore(java.time.Instant.parse(decoded.startedAt.value)) shouldBe false
        successor.run.state shouldBe ReviewRunState("queued")
        _job_count(fixture) shouldBe 1
      } finally fixture.engine.shutdown()
    }

    "retain the original submit failure when terminal cleanup also fails" in {
      Given("a failed owner submit whose reliable empty discovery pre-seeds its terminal Run identity")
      val fixture = _fixture("operator")
      val port = new _SubmitFailureCleanupCollisionPort(fixture.context)
      val request = _request("review-production-submit-cleanup-failed")
      given ExecutionContext = fixture.context
      val execution = _value(CarReviewProductionExecution.create(request))

      try {
        When("the reliable no-Job recovery attempts its Owner terminal settlement")
        val result = _run(fixture, _program(fixture, port).start(request))
        val diagnosisid = _diagnosis_id(execution.plan)
        val root = _load_snapshot[ReviewDiagnosisEntity](fixture.context, diagnosisid)
        val collision = _find_snapshot[ReviewRunSnapshotEntity](
          fixture.context,
          _snapshot_id("terminal-failed", diagnosisid, request.reviewId.value, ReviewRunSnapshotEntity.collectionId)
        )

        Then("the primary submit Conclusion remains authoritative and its one cleanup cause is retained")
        result match {
          case Consequence.Failure(conclusion) =>
            conclusion.displayMessage should include ("synthetic-review-job-submit-failed")
            conclusion.causes.last.displayMessage should include ("synthetic-review-job-submit-failed")
            conclusion.causes should have size 2
            conclusion.causes.head.displayMessage should not be conclusion.causes.last.displayMessage
          case Consequence.Success(_) =>
            fail("cleanup collision unexpectedly fabricated a successful Review admission")
        }
        root.state.value shouldBe "claimed"
        root.report_id shouldBe None
        root.report_digest shouldBe None
        collision should not be empty
      } finally fixture.engine.shutdown()
    }

    "leave a claimed diagnosis untouched when submit reconciliation is unreliable or mismatched" in {
      Given("one failed reconciliation and one exact-review but wrong-binding reconciliation")
      val fixture = _fixture("operator")
      val gateway = new CncfCarReviewJobGateway(fixture.engine)
      given ExecutionContext = fixture.context
      val unavailablerequest = _request("review-production-submit-reconcile-unavailable", 'b')
      val unavailableexecution = _value(CarReviewProductionExecution.create(unavailablerequest))
      val mismatchrequest = _request("review-production-submit-reconcile-mismatch", 'c')
      val mismatchexecution = _value(CarReviewProductionExecution.create(mismatchrequest))
      val mismatchsource = _value(CarReviewProductionExecution.create(_request(mismatchrequest.reviewId.value, 'd')))
      val mismatchbinding = CarReviewProductionJobBinding
        .from("diagnosis-mismatch-source", mismatchsource)
        .fold(error => fail(error), identity)

      try {
        val mismatchjob = _value(gateway.submit(mismatchbinding, mismatchsource))
        val discovered = _value(gateway.findByReviewId(mismatchrequest.reviewId)).getOrElse(fail("expected seeded mismatched Job"))
        val unavailable = _run(
          fixture,
          _program(fixture, new _SubmitFailureUnavailablePort(gateway)).start(unavailablerequest)
        )
        val mismatch = _run(
          fixture,
          _program(fixture, new _SubmitFailureMismatchPort(gateway, discovered)).start(mismatchrequest)
        )
        val unavailableroot = _load_snapshot[ReviewDiagnosisEntity](fixture.context, _diagnosis_id(unavailableexecution.plan))
        val mismatchroot = _load_snapshot[ReviewDiagnosisEntity](fixture.context, _diagnosis_id(mismatchexecution.plan))

        When("both primary submit failures are reconciled without a trustworthy exact Job")

        Then("each original failure is returned and neither claimed root receives a terminal Run")
        unavailable shouldBe a[Consequence.Failure[_]]
        mismatch shouldBe a[Consequence.Failure[_]]
        unavailableroot.state.value shouldBe "claimed"
        mismatchroot.state.value shouldBe "claimed"
        _find_snapshot[ReviewRunSnapshotEntity](
          fixture.context,
          _snapshot_id("terminal-failed", _diagnosis_id(unavailableexecution.plan), unavailablerequest.reviewId.value, ReviewRunSnapshotEntity.collectionId)
        ) shouldBe empty
        _find_snapshot[ReviewRunSnapshotEntity](
          fixture.context,
          _snapshot_id("terminal-failed", _diagnosis_id(mismatchexecution.plan), mismatchrequest.reviewId.value, ReviewRunSnapshotEntity.collectionId)
        ) shouldBe empty
        mismatchjob.value should not be empty
      } finally fixture.engine.shutdown()
    }

    "join the one successor after concurrent callers settle the same terminal Job" in {
      Given("a cancelled owner Job and two same-key callers held at its exact terminal discovery")
      val fixture = _fixture("operator")
      val gateway = new CncfCarReviewJobGateway(fixture.engine)
      val owner = _value(_run(fixture, _program(fixture).start(_request("review-production-terminal-race-owner"))))
      given ExecutionContext = fixture.context
      _value(gateway.cancel(owner.binding.jobId))
      val entered = new CountDownLatch(2)
      val release = new CountDownLatch(1)
      val port = new _TerminalDiscoveryLatchPort(gateway, owner.binding.jobId, entered, release)
      val firstrequest = _request("review-production-terminal-race-first")
      val secondrequest = _request("review-production-terminal-race-second")
      given ScalaExecutionContext = ScalaExecutionContext.global

      try {
        When("both callers observe and settle the old terminal Job concurrently")
        val calls = Vector(firstrequest, secondrequest).map(request =>
          Future(_run(fixture, _program(fixture, port).start(request)))
        )
        entered.await(30, TimeUnit.SECONDS) shouldBe true
        release.countDown()
        val outcomes = calls.map(Await.result(_, Duration(30, TimeUnit.SECONDS)))
        val immediate = outcomes.collect { case Consequence.Success(value) => value }
        val retried = outcomes.zipWithIndex.collect { case (Consequence.Failure(error), index) =>
          error.toString should include ("review-job-submission-pending")
          _value(_run(fixture, _program(fixture, port).start(Vector(firstrequest, secondrequest)(index))))
        }
        val admissions = immediate ++ retried

        Then("one successor is submitted and every completed caller names its exact queued binding")
        admissions should not be empty
        admissions.map(_.binding.jobId).distinct should have size 1
        admissions.map(_.run.state).distinct shouldBe Vector(ReviewRunState("queued"))
        _job_count(fixture) shouldBe 2
      } finally {
        release.countDown()
        fixture.engine.shutdown()
      }
    }

    }
    "enforce Review read and cancellation controls" which {
    "authorize cancellation before lookup while allowing viewer reads and valid operator cancellation" in {
      Given("one shared queued production Review and both viewer and operator contexts")
      val fixture = _fixture("operator")

      try {
        val owner = _value(_run(fixture, _program(fixture).start(_request("review-production-cancel"))))
        val viewerfixture = fixture.copy(context = _with_role(fixture.context, "viewer"))

        When("a viewer requests a real run, probes cancellation, and an operator cancels it")
        val viewed = _run(viewerfixture, _program(viewerfixture).get(owner.run.reviewId))
        val denied = _run(viewerfixture, _program(viewerfixture).cancel(
          ReviewId("review-production-not-found"),
          ReviewInstant("2026-08-15T00:01:00Z")
        ))
        val cancelled = _value(_run(fixture, _program(fixture).cancel(
          owner.run.reviewId,
          ReviewInstant("2026-08-15T00:01:00Z")
        )))

        Then("read authorization is independent, cancellation cannot probe existence, and the operator sees the Job lifecycle")
        _value(viewed).binding.jobId shouldBe owner.binding.jobId
        denied.fold(
          error => {
            error.toString should include ("required role: admin|operator")
            error.toString should not include "review-production-not-found"
            error.toString should not include "operation.not-found"
          },
          _ => fail("viewer unexpectedly cancelled a Review Job")
        )
        cancelled.binding.jobId shouldBe owner.binding.jobId
        cancelled.run.state.value should (be ("cancelled") or be ("cancelling"))
      } finally fixture.engine.shutdown()
    }

    "reject cancellation of an already completed Job before lazy read settlement" in {
      Given("one completed production Job whose Entity Report has not yet been read")
      val fixture = _fixture("operator")

      try {
        val owner = _value(_run(fixture, _program(fixture).start(_request("review-production-terminal-cancel"))))
        fixture.engine.drainAll()
        val viewerfixture = fixture.copy(context = _with_role(fixture.context, "viewer"))

        When("the operator cancels the terminal Job before either actor gets it")
        val rejected = _run(fixture, _program(fixture).cancel(
          owner.run.reviewId,
          ReviewInstant("2026-08-15T00:01:00Z")
        ))
        val completed = _value(_run(viewerfixture, _program(viewerfixture).get(owner.run.reviewId)))
        val report = _value(_run(
          viewerfixture,
          new ComponentFactory()._load_persisted_review_report(
            _core(viewerfixture),
            completed.run.reportId.getOrElse(fail("completed Review Run did not retain Report identity"))
          )
        ))

        Then("control rejects the terminal Job while get settles and exposes its original Report binding")
        rejected.fold(
          _ => (),
          _ => fail("terminal Job cancellation unexpectedly succeeded")
        )
        completed.run.state shouldBe ReviewRunState("completed")
        completed.binding.jobId shouldBe owner.binding.jobId
        report.reportId shouldBe completed.run.reportId.get
        _job_count(fixture) shouldBe 1
      } finally fixture.engine.shutdown()
    }
    }
  }

  private final case class Fixture(
    component: Component,
    engine: InMemoryJobEngine,
    context: ExecutionContext
  )

  private def _program(fixture: Fixture): ReviewProductionActionProgram =
    new ReviewProductionActionProgram(_core(fixture))

  private def _program(
    fixture: Fixture,
    port: CarReviewProductionJobPort
  ): ReviewProductionActionProgram =
    new ReviewProductionActionProgram(_core(fixture), Some(port))

  private def _run[A](
    fixture: Fixture,
    program: ExecUowM[A]
  ): Consequence[A] =
    new UnitOfWorkInterpreter(new UnitOfWork(fixture.context)).run(program)

  private def _value[A](result: Consequence[A]): A =
    result.fold(error => fail(error.toString), identity)

  private def _job_count(fixture: Fixture): Int =
    fixture.engine.listJobs(100, persistentOnly = true).size

  private def _request(
    reviewid: String,
    digestcharacter: Char = 'a'
  ): ReviewStartRequest =
    ReviewStartRequest(
      ReviewId(reviewid),
      ReviewTarget(
        ReviewTargetKind("car"),
        Some("org.simplemodeling"),
        "textus-cbd-support",
        Some(ReviewVersion("0.1.0-SNAPSHOT")),
        ReviewDigest("sha256:" + (digestcharacter.toString * 64))
      ),
      ReviewProfile("development"),
      ReviewInstant("2026-08-15T00:00:00Z")
    )

  private def _fixture(role: String): Fixture = {
    val base = ExecutionContext.create()
    val engine = InMemoryJobEngine.create(InMemoryJobEngine.SchedulerConfig(autoStartWorkers = false))
    val component = _component(engine)
    val datastorespace = new DataStoreSpace().useDataStore(DataStore.inMemorySearchable())
    val entitystorespace = new EntityStoreSpace().addEntityStore(EntityStore.standard())
    lazy val context: ExecutionContext = ExecutionContext.create(runtime)
    lazy val unitofwork = new UnitOfWork(context)
    lazy val interpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = ScopeContext.Core(
        kind = ScopeKind.Runtime,
        name = "review-production-action-program-spec",
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
      token = "review-production-action-program-spec"
    )
    runtime.setResolvedParameters(ResolvedParameters.empty())
    Fixture(component, engine, _with_role(context, role))
  }

  private def _with_role(context: ExecutionContext, role: String): ExecutionContext =
    ExecutionContext.withSecurityContext(
      context,
      context.security.copy(
        capabilities = Set(Capability(role)),
        level = SecurityLevel(role)
      )
    )

  private def _core(fixture: Fixture): ActionCall.Core =
    ActionCall.Core(
      TestAction(Request.ofOperation("review-production-action-program-spec")),
      fixture.context,
      Some(fixture.component),
      None
    )

  private def _component(engine: InMemoryJobEngine): Component = {
    val component = new Component {
      override val core: Component.Core = Component.Core.create(
        CbdSupportComponent.name,
        CbdSupportComponent.componentId,
        ComponentInstanceId.default(CbdSupportComponent.componentId),
        Protocol.empty,
        engine
      )

      override def coreOption: Option[Component.Core] = Some(core)
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
      revisionBinding = Some(EntityRevisionBinding(EntityRevisionRepresentation.Embedded))
    )
    new EntityCollection(descriptor, EntityStorage(storerealm))
  }

  private class _DelegatingProductionJobPort(
    delegate: CncfCarReviewJobGateway
  ) extends CarReviewProductionJobPort {
    def submit(
      binding: CarReviewProductionJobBinding,
      execution: CarReviewProductionExecution
    )(using context: ExecutionContext): Consequence[ReviewJobId] =
      delegate.submit(binding, execution)

    def findByReviewId(
      reviewId: ReviewId
    )(using context: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] =
      delegate.findByReviewId(reviewId)

    def findByReviewReuse(
      reviewId: ReviewId,
      reuseKeyDefinition: String,
      reuseKeyDigest: ReviewDigest
    )(using context: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] =
      delegate.findByReviewReuse(reviewId, reuseKeyDefinition, reuseKeyDigest)

    def cancel(
      jobId: ReviewJobId
    )(using context: ExecutionContext): Consequence[Unit] =
      delegate.cancel(jobId)
  }

  private final class _BlockingSubmitPort(
    delegate: CncfCarReviewJobGateway,
    entered: CountDownLatch,
    release: CountDownLatch
  ) extends _DelegatingProductionJobPort(delegate) {
    val submitcount = new AtomicInteger(0)

    override def submit(
      binding: CarReviewProductionJobBinding,
      execution: CarReviewProductionExecution
    )(using context: ExecutionContext): Consequence[ReviewJobId] = {
      submitcount.incrementAndGet()
      entered.countDown()
      if (release.await(30, TimeUnit.SECONDS))
        super.submit(binding, execution)
      else
        Consequence.serviceUnavailable("review-job-submit-latch-timeout")
    }
  }

  private final class _AcceptedThenFailurePort(
    delegate: CncfCarReviewJobGateway
  ) extends _DelegatingProductionJobPort(delegate) {
    val submitcount = new AtomicInteger(0)

    override def submit(
      binding: CarReviewProductionJobBinding,
      execution: CarReviewProductionExecution
    )(using context: ExecutionContext): Consequence[ReviewJobId] = {
      submitcount.incrementAndGet()
      super.submit(binding, execution) match {
        case Consequence.Success(_) =>
          Consequence.serviceUnavailable("synthetic-review-job-submit-ack-lost")
        case failure: Consequence.Failure[?] => failure
      }
    }
  }

  private class _SubmitFailureNoDiscoveryPort extends CarReviewProductionJobPort {
    def submit(
      binding: CarReviewProductionJobBinding,
      execution: CarReviewProductionExecution
    )(using context: ExecutionContext): Consequence[ReviewJobId] = {
      val _ = binding
      val _ = execution
      Consequence.serviceUnavailable("synthetic-review-job-submit-failed")
    }

    def findByReviewId(
      reviewId: ReviewId
    )(using context: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] = {
      val _ = reviewId
      Consequence.success(None)
    }

    def findByReviewReuse(
      reviewId: ReviewId,
      reuseKeyDefinition: String,
      reuseKeyDigest: ReviewDigest
    )(using context: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] = {
      val _ = reviewId
      val _ = reuseKeyDefinition
      val _ = reuseKeyDigest
      Consequence.success(None)
    }

    def cancel(
      jobId: ReviewJobId
    )(using context: ExecutionContext): Consequence[Unit] = {
      val _ = jobId
      Consequence.operationInvalid("synthetic-review-job-cancel-unavailable")
    }
  }

  private final class _SubmitFailureCleanupCollisionPort(
    context: ExecutionContext
  ) extends _SubmitFailureNoDiscoveryPort {
    private val _seeded = new AtomicBoolean(false)
    private var _binding: Option[CarReviewProductionJobBinding] = None

    override def submit(
      binding: CarReviewProductionJobBinding,
      execution: CarReviewProductionExecution
    )(using executioncontext: ExecutionContext): Consequence[ReviewJobId] = {
      _binding = Some(binding)
      super.submit(binding, execution)
    }

    override def findByReviewReuse(
      reviewid: ReviewId,
      reusekeydefinition: String,
      reusekeydigest: ReviewDigest
    )(using executioncontext: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] = {
      if (_seeded.compareAndSet(false, true))
        _binding.foreach(_seed_terminal_cleanup_collision(context, _))
      super.findByReviewReuse(reviewid, reusekeydefinition, reusekeydigest)
    }
  }

  private final class _SubmitFailureUnavailablePort(
    delegate: CncfCarReviewJobGateway
  ) extends _DelegatingProductionJobPort(delegate) {
    override def submit(
      binding: CarReviewProductionJobBinding,
      execution: CarReviewProductionExecution
    )(using context: ExecutionContext): Consequence[ReviewJobId] = {
      val _ = binding
      val _ = execution
      Consequence.serviceUnavailable("synthetic-review-job-submit-unavailable")
    }

    override def findByReviewReuse(
      reviewId: ReviewId,
      reuseKeyDefinition: String,
      reuseKeyDigest: ReviewDigest
    )(using context: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] = {
      val _ = reviewId
      val _ = reuseKeyDefinition
      val _ = reuseKeyDigest
      Consequence.serviceUnavailable("synthetic-review-job-reconcile-unavailable")
    }
  }

  private final class _SubmitFailureMismatchPort(
    delegate: CncfCarReviewJobGateway,
    discovered: CarReviewDiscoveredProductionJob
  ) extends _DelegatingProductionJobPort(delegate) {
    override def submit(
      binding: CarReviewProductionJobBinding,
      execution: CarReviewProductionExecution
    )(using context: ExecutionContext): Consequence[ReviewJobId] = {
      val _ = binding
      val _ = execution
      Consequence.serviceUnavailable("synthetic-review-job-submit-mismatch")
    }

    override def findByReviewReuse(
      reviewId: ReviewId,
      reuseKeyDefinition: String,
      reuseKeyDigest: ReviewDigest
    )(using context: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] = {
      val _ = reviewId
      val _ = reuseKeyDefinition
      val _ = reuseKeyDigest
      Consequence.success(Some(discovered))
    }
  }

  private final class _TerminalDiscoveryLatchPort(
    delegate: CncfCarReviewJobGateway,
    terminaljobid: ReviewJobId,
    entered: CountDownLatch,
    release: CountDownLatch
  ) extends _DelegatingProductionJobPort(delegate) {
    private val _terminal_reads = new AtomicInteger(0)

    override def findByReviewReuse(
      reviewId: ReviewId,
      reuseKeyDefinition: String,
      reuseKeyDigest: ReviewDigest
    )(using context: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] = {
      val result = super.findByReviewReuse(reviewId, reuseKeyDefinition, reuseKeyDigest)
      result match {
        case Consequence.Success(Some(discovered))
            if discovered.jobId == terminaljobid && _terminal_reads.incrementAndGet() <= 2 =>
          entered.countDown()
          if (release.await(30, TimeUnit.SECONDS)) result
          else Consequence.serviceUnavailable("review-job-terminal-discovery-latch-timeout")
        case _ => result
      }
    }
  }

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

  private def _diagnosis_id(
    plan: CarReviewExecutionPlan
  ): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${plan.reuseKey.definitionId}:${plan.reuseKey.digest.value}"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    val collection = ReviewDiagnosisEntity.collectionId
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
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    org.simplemodeling.model.datatype.EntityId(
      collection.major,
      collection.minor,
      collection,
      timestamp = Some(org.goldenport.id.UniversalId.StableTimestamp),
      entropy = Some(key)
    )
  }

  private def _seed_terminal_cleanup_collision(
    context: ExecutionContext,
    binding: CarReviewProductionJobBinding
  ): Unit = {
    given ExecutionContext = context
    val diagnosisid = _value(org.simplemodeling.model.datatype.EntityId.parse(binding.diagnosisId))
    val admitted = CarReviewRunLifecycle.admitted(
      binding.reviewId,
      binding.target,
      binding.profile,
      binding.startedAt
    ).fold(error => fail(error.message), identity)
    val queued = CarReviewRunLifecycle.projectJob(
      admitted,
      ReviewRunJobUpdate(JobStatus.Submitted, binding.startedAt)
    ).fold(error => fail(error.message), identity)
    val failed = CarReviewRunLifecycle.projectJob(
      queued,
      ReviewRunJobUpdate(
        JobStatus.Failed,
        binding.startedAt,
        failureCode = Some(ReviewFailureCode("review-job-cleanup-collision"))
      )
    ).fold(error => fail(error.message), identity)
    val document = CarReviewRunCodec.encode(failed).fold(error => fail(error.message), identity)
    val collision = _value(
      ReviewRunSnapshotCreate.Builder()
        .withDiagnosis_id(diagnosisid)
        .withReview_id(EntityReviewId(binding.reviewId.value))
        .withState(EntityReviewRunState("failed"))
        .withProfile(EntityReviewProfile(binding.profile.value))
        .withRun_document(ReviewDiagnosisEntityPrograms._json_document(document))
        .withStarted_at(EntityReviewInstant(binding.startedAt.value))
        .buildC()
    ).copy(
      id = Some(_snapshot_id(
        "terminal-failed",
        diagnosisid,
        binding.reviewId.value,
        ReviewRunSnapshotCreate.collectionId
      )),
      completed_at = Some(EntityReviewInstant(binding.startedAt.value))
    )
    _value(context.entityStoreSpace.create(
      UnitOfWorkOp.EntityStoreCreate(
        collision,
        summon[EntityPersistentCreate[ReviewRunSnapshotCreate]]
      )
    ))
    ()
  }

  private final class IdRef[A](initial: A) extends Ref[Id, A] {
    private var _value: A = initial

    def get: A = synchronized(_value)

    def set(value: A): Unit = synchronized {
      _value = value
    }

    override def getAndSet(value: A): A = synchronized {
      val previous = _value
      _value = value
      previous
    }

    def access: (A, A => Boolean) = synchronized {
      val snapshot = _value
      val setter: A => Boolean = next => synchronized {
        if (_value == snapshot) {
          _value = next
          true
        } else {
          false
        }
      }
      snapshot -> setter
    }

    override def tryUpdate(f: A => A): Boolean = synchronized {
      _value = f(_value)
      true
    }

    override def tryModify[B](f: A => (A, B)): Option[B] = synchronized {
      val (next, result) = f(_value)
      _value = next
      Some(result)
    }

    def update(f: A => A): Unit = synchronized {
      _value = f(_value)
    }

    def modify[B](f: A => (A, B)): B = synchronized {
      val (next, result) = f(_value)
      _value = next
      result
    }

    override def modifyState[B](state: State[A, B]): B = synchronized {
      val (next, result) = state.run(_value).value
      _value = next
      result
    }

    override def tryModifyState[B](state: State[A, B]): Option[B] = synchronized {
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
