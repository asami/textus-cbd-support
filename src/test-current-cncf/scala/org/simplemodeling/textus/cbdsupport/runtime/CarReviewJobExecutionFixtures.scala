package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import java.lang.reflect.{InvocationHandler, InvocationTargetException, Method, Proxy}
import scala.collection.mutable.ArrayBuffer
import cats.{Id, ~>}
import cats.data.State
import cats.effect.Ref
import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall, ActionEngine}
import org.goldenport.cncf.component.{Component, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.*
import org.goldenport.cncf.datastore.DataStore
import org.goldenport.cncf.entity.*
import org.goldenport.cncf.entity.runtime.*
import org.goldenport.cncf.event.{EventEngine, EventStore}
import org.goldenport.cncf.job.*
import org.goldenport.cncf.unitofwork.*
import org.goldenport.protocol.{Protocol, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityId}
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.impl.{ComponentFactory, ReviewJobProgram}
import org.simplemodeling.textus.cbdsupport.entity.{ReviewDiagnosis, ReviewTargetSnapshot, ReviewRunSnapshot,
  ReviewReportSnapshot, ReviewAttestationSnapshot, ReviewRetentionEvent, ReviewJobBinding}

/*
 * @since   Oct.  6, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CarReviewJobExecutionFixtures {
  def value[A](result: Consequence[A]): A = result match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => throw new IllegalStateException(conclusion.display)
  }

  def failure[A](result: Consequence[A]): String = result match {
    case Consequence.Failure(conclusion) => conclusion.display
    case Consequence.Success(_) => throw new IllegalStateException("expected structured refusal")
  }

  def load(name: String): String = Files.readString(Path.of("docs/spec/examples", name))
  def withFixture[A](body: Fixture => A): A =
    NativeCbdReviewJobFixtures.withFixture { native =>
      value(native.start())
      val fixture = value(native.managed(Consequence.success(new Fixture(native))))
      try body(fixture) finally fixture.close()
    }

  final class Runner extends CarReviewProviderRunner {
    private var _executions = 0
    private var _cancellations = 0
    var execution: ProviderBundleExecutionRequest => ProviderBundleRunnerResult =
      request => ProviderBundleRunnerResult.Completed(load("car-review-evidence-bundle-v1.json"), request.startedAtMillis + 1000L)
    var cancellation: () => Unit = () => ()
    def executions: Int = _executions
    def cancellations: Int = _cancellations
    def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult = {
      _executions += 1
      execution(request)
    }
    def cancel(request: ProviderBundleExecutionRequest): Unit = {
      _cancellations += 1
      cancellation()
    }
  }

  final class Recorder extends CommitRecorder {
    private val _entries = ArrayBuffer.empty[String]
    def entries: Vector[String] = _entries.toVector
    def record(message: String): Unit = { _entries += message; () }
  }

  final class Fixture(val native: NativeCbdReviewJobFixtures.Fixture) {
    val runner = new Runner
    val registry = new CarReviewProviderRegistry
    val application = new CarReviewProviderExecutionApplication(registry, new CarReviewProviderExecutionCoordinator)
    val gateway = new CncfCarReviewJobGateway(native.engine)
    val factory = new ComponentFactory
    val actionEngine = ActionEngine.create()
    val descriptor = load("car-review-provider-descriptor-v1.json")
    val selectedDescriptor = CarReviewProviderBundleAdmission.describeDescriptor(descriptor).fold(error => throw new IllegalStateException(error), identity)
    val template = CarReviewReportCodec.decode(load("car-review-report-v1.json")).fold(error => throw new IllegalStateException(error.message), identity).copy(baseline = None)
    val request = ProviderBundleExecutionRequest(template.reviewId, template.target, selectedDescriptor.provider,
      ProviderBundleAvailability.Enabled, descriptor, load("car-review-provider-request-v1.json"), 0L)
    val recorder = new Recorder
    var fault: String = "none"
    private var _issued_ids = 0
    def issuedIds: Int = _issued_ids
    private val _contexts = ArrayBuffer.empty[ExecutionContext]
    private val _events = _event_engine
    val component: Component = {
      val instance = new CbdSupportComponent
      instance.initialize(ComponentInit(native.subsystem,
        Component.Core.create(CbdSupportComponent.name, CbdSupportComponent.componentId,
          ComponentInstanceId.default(CbdSupportComponent.componentId), Protocol.empty, native.engine), ComponentOrigin.Builtin))
      native.context.entitySpace.entityCollections.foreach { collection =>
        instance.entitySpace.registerEntity(collection.descriptor.plan.entityName, collection)
      }
      _register(instance, ReviewDiagnosis.collectionId, summon[EntityPersistent[ReviewDiagnosis]])
      _register(instance, ReviewTargetSnapshot.collectionId, summon[EntityPersistent[ReviewTargetSnapshot]])
      _register(instance, ReviewRunSnapshot.collectionId, summon[EntityPersistent[ReviewRunSnapshot]])
      _register(instance, ReviewReportSnapshot.collectionId, summon[EntityPersistent[ReviewReportSnapshot]])
      _register(instance, ReviewAttestationSnapshot.collectionId, summon[EntityPersistent[ReviewAttestationSnapshot]])
      _register(instance, ReviewRetentionEvent.collectionId, summon[EntityPersistent[ReviewRetentionEvent]])
      _register(instance, ReviewJobBinding.collectionId, summon[EntityPersistent[ReviewJobBinding]])
      instance
    }
    val outer: ExecutionContext = freshContext()
    val core: ActionCall.Core = actionCore(outer)
    registry.register(descriptor, runner).fold(error => throw new IllegalStateException(error.code), identity)

    def freshContext(): ExecutionContext = {
      val base = ExecutionContext.withSecurityContext(
        ExecutionContext.withFrameworkCallTreeEnabled(native.context, enabled = true),
        native.context.security.copy(capabilities = native.context.security.capabilities + Capability("operator"), level = SecurityLevel("operator")))
      lazy val bound: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
      lazy val context: ExecutionContext = ExecutionContext.withIdGenerationContext(bound,
        new CountingIds(bound.idGeneration, () => { _issued_ids += 1; () }))
      lazy val unitofwork = native.unitOfWork(context, _events, recorder)
      lazy val interpreter = new UnitOfWorkInterpreter(unitofwork)
      lazy val runtime: RuntimeContext = new RuntimeContext(
        core = ScopeContext.Core(ScopeKind.Runtime, "cbd-native-review-job-spec", Some(native.subsystem.globalRuntimeContext),
          base.observability, None, Some(DataStoreContext(native.context.dataStoreSpace)),
          Some(EntityStoreContext(native.entityStoreSpace)), Some(EntitySpaceContext(component.entitySpace))),
        unitofworksupplier = () => unitofwork,
        unitofworkinterpreterfn = new (UnitOfWorkOp ~> Consequence) {
          def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
        },
        commitaction = uow => { val _ = uow.commit(); () },
        abortaction = uow => { val _ = uow.abort(); () },
        disposeaction = uow => { val _ = uow.dispose(); () },
        token = "cbd-native-review-job-spec")
      _contexts += context
      context
    }

    def actionCore(context: ExecutionContext): ActionCall.Core =
      ActionCall.Core(ServerAction(Request.ofOperation("executeReview")), context, Some(component), None)

    def plan(revision: Long = 1L, reversePolicies: Boolean = false): CarReviewExecutionPlan = {
      val selection = CarReviewReuseProviderSelection(selectedDescriptor.provider, selectedDescriptor.ruleSet,
        CarReviewDeclaredReference("availability:cozy", revision))
      val policies = Vector("profile", "gate", "reconciliation", "suppression").map { scope =>
        CarReviewReusePolicyBinding(scope, if scope == "gate" then template.gate.policyId else scope + "-default",
          if scope == "gate" then template.gate.policyVersion else ReviewVersion("1.0"), revision)
      }
      val input = CarReviewReuseKeyInput(CarReviewReuseKey.DEFINITION_ID, template.schemaVersion,
        CarReviewDeclaredReference("target:server", revision), template.target.kind, template.target.organization,
        template.target.name, template.target.version, template.profile, None, Vector(selection.ruleSet),
        Vector(selection), Vector.empty, if reversePolicies then policies.reverse else policies)
      val reference = CarReviewExecutionPlanReference(value(EntityId.parse(
        "major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-serverPlan")), revision)
      CarReviewExecutionPlan.create(ReviewStartRequest(template.reviewId, template.target, template.profile,
        ReviewInstant(outer.clock.instant().toString)), reference, input).fold(error => throw new IllegalStateException(error.code), identity)
    }

    def claim(selectedPlan: CarReviewExecutionPlan): CarReviewDiagnosisAdmission.Owner =
      value(native.managed(new UnitOfWorkInterpreter(outer.unitOfWork).evaluateInActiveUnitOfWorkC(
        factory._admit_review_execution(core, selectedPlan)))) match {
        case owner: CarReviewDiagnosisAdmission.Owner => owner
        case _ => throw new IllegalStateException("native Entity must issue an owner")
      }

    def work(selectedPlan: CarReviewExecutionPlan, owner: CarReviewDiagnosisAdmission.Owner): CarReviewJobExecutionWork =
      CarReviewJobExecutionWork(selectedPlan, owner, template, Vector(CarReviewServerProviderCall(selectedPlan.reuseInput.providerSelections.head, request)))

    def stage(work: CarReviewJobExecutionWork, selectedApplication: CarReviewProviderExecutionApplication = application): Consequence[ReviewJobId] =
      native.managed(gateway.stageExecution(core, work, selectedApplication, factory, actionEngine).flatMap { id =>
        new UnitOfWorkInterpreter(outer.unitOfWork).evaluateInActiveUnitOfWorkC(
          new ReviewJobProgram(core, factory).bind(work.plan, id)).map(_ => id)
      })
    def commit(): Consequence[Unit] = native.managed(native.commit(outer).map(_ => ()))
    def abort(): Consequence[Unit] = native.managed(native.abort(outer).map(_ => ()))
    def dispose(): Unit = native.dispose(outer)
    def drain(): Int = value(native.drain())
    def jobId(id: ReviewJobId): JobId = value(JobId.parse(id.value))
    def query(id: ReviewJobId): Option[JobQueryReadModel] = value(native.managed(Consequence.success(native.engine.query(jobId(id)))))
    def signedResult(id: ReviewJobId): Option[OperationResponse] = value(native.signedResult(jobId(id)))
    def readReport(id: ReviewReportId): Consequence[CarReviewReport] = {
      val context = freshContext()
      native.managed(new UnitOfWorkInterpreter(context.unitOfWork).run(
        new ComponentFactory()._load_persisted_review_report(actionCore(context), id)))
    }
    def terminal(work: CarReviewJobExecutionWork): Unit = {
      val context = freshContext()
      value(native.managed(new UnitOfWorkInterpreter(context.unitOfWork).run(
        new ComponentFactory()._record_terminal_review_execution(actionCore(context), work.owner, work.plan,
          CarReviewDiagnosisTerminalState.Failed, "{\"state\":\"failed\"}", ReviewInstant(context.clock.instant().toString)))))
      value(native.managed(native.commit(context)))
    }
    def cancel(id: ReviewJobId): Consequence[Unit] = native.managed(gateway.cancel(id)(using outer))
    def reportCount: Int = _count[ReviewReportSnapshot](ReviewReportSnapshot.collectionId)
    def attestationCount: Int = _count[ReviewAttestationSnapshot](ReviewAttestationSnapshot.collectionId)
    def diagnosis(plan: CarReviewExecutionPlan): ReviewDiagnosis = value(native.managed(
      new UnitOfWorkInterpreter(freshContext().unitOfWork).interpret(
        UnitOfWorkOp.EntityStoreLoadDirect[ReviewDiagnosis](plan.reference.diagnosisId, summon[EntityPersistent[ReviewDiagnosis]])))).get
    def trace(context: ExecutionContext = outer): String =
      (Vector(context) ++ native.observedContexts.filter(_.jobContext.jobId.nonEmpty)).distinct
        .flatMap(_.observability.callTreeContext.build().map(_.toRecord.print)).mkString("\n")
    def close(): Unit = { _contexts.foreach(native.dispose); () }

    private def _count[E: EntityPersistent](collectionid: EntityCollectionId): Int = {
      val context = freshContext()
      value(native.managed(new UnitOfWorkInterpreter(context.unitOfWork).interpret(
        UnitOfWorkOp.EntityStoreSearchInternal(EntityQuery[E](collectionid, org.goldenport.cncf.directive.Query(
          org.goldenport.cncf.directive.Query.Plan(org.goldenport.record.Record.empty, limit = Some(10)))), summon[EntityPersistent[E]])))).data.size
    }

    private def _register[E](component: Component, collectionid: EntityCollectionId, persistent: EntityPersistent[E]): Unit = {
      given EntityPersistent[E] = persistent
      val realm = new EntityRealm[E](collectionid.name, EntityLoader[E](_ => None), new IdRef(EntityRealmState(Map.empty)))
      val descriptor = EntityDescriptor(collectionid, EntityRuntimePlan[E](
        entityName = collectionid.name,
        memoryPolicy = EntityMemoryPolicy.StoreOnly,
        workingSet = None,
        partitionStrategy = PartitionStrategy.byEntityId,
        maxPartitions = 1,
        maxEntitiesPerPartition = 1), persistent,
        revisionBinding = Some(EntityRevisionBinding(EntityRevisionRepresentation.Embedded)))
      component.entitySpace.registerEntity(collectionid.name, new EntityCollection(descriptor, EntityStorage(realm)))
    }

    private def _event_engine: EventEngine = {
      val delegate = EventEngine.noop(DataStore.noop(), recorder, EventStore.inMemory)
      Proxy.newProxyInstance(classOf[EventEngine].getClassLoader, Array(classOf[EventEngine]), new InvocationHandler {
        def invoke(proxy: Object, method: Method, arguments: Array[Object]): Object =
          (fault, method.getName) match {
            case ("prepare", "prepare") =>
              recorder.record("EventEngine.prepare.rejected")
              PrepareResult.Rejected("controlled-task-prepare-rejection")
            case ("commit", "commit") =>
              recorder.record("EventEngine.commit.failed")
              throw new IllegalStateException("controlled-task-commit-failure")
            case _ =>
              try method.invoke(delegate, Option(arguments).getOrElse(Array.empty[Object])*)
              catch { case exception: InvocationTargetException => throw exception.getCause }
          }
      }).asInstanceOf[EventEngine]
    }
  }

  private final case class ServerAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall =
      throw new UnsupportedOperationException("The native task supplies the functional call.")
  }

  /** Observe issuance while delegating every value to the actual configured profile. */
  private final class CountingIds(delegate: IdGenerationContext, issued: () => Unit) extends IdGenerationContext {
    def namespace: IdGenerationContext.IdNamespace = delegate.namespace
    def entityId(collection: EntityCollectionId, purpose: String): EntityId = {
      issued()
      delegate.entityId(collection, purpose)
    }
    def opaqueId(purpose: String): String = {
      issued()
      delegate.opaqueId(purpose)
    }
  }

  private final class IdRef[A](initial: A) extends Ref[Id, A] {
    private var _value = initial
    def get: A = synchronized(_value)
    def set(value: A): Unit = synchronized { _value = value }
    override def getAndSet(value: A): A = synchronized { val previous = _value; _value = value; previous }
    def access: (A, A => Boolean) = synchronized {
      val snapshot = _value
      snapshot -> (next => synchronized { if _value == snapshot then { _value = next; true } else false })
    }
    override def tryUpdate(f: A => A): Boolean = synchronized { _value = f(_value); true }
    override def tryModify[B](f: A => (A, B)): Option[B] = Some(modify(f))
    def update(f: A => A): Unit = synchronized { _value = f(_value) }
    def modify[B](f: A => (A, B)): B = synchronized { val (next, result) = f(_value); _value = next; result }
    override def modifyState[B](state: State[A, B]): B = modify(value => state.run(value).value)
    override def tryModifyState[B](state: State[A, B]): Option[B] = Some(modifyState(state))
  }
}
