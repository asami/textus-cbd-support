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
 * @version Jul. 26, 2026
 * @author  ASAMI, Tomoharu
 */
final class ReviewDiagnosisPersistenceSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "CAR Review diagnosis persistence" should {
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
        observabilityContext = base.cncfCore.observability,
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
        "CbdSupport",
        ComponentId("CbdSupport"),
        ComponentInstanceId.default(ComponentId("CbdSupport")),
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
