package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.{Clock, Instant, ZoneId, ZoneOffset}
import cats.{Id, ~>}
import cats.data.State
import cats.effect.Ref
import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall, ActionCallEntityStorePart}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.config.ResolvedParameters
import org.goldenport.cncf.context.{Capability, DataStoreContext, EntitySpaceContext, EntityStoreContext,
  ExecutionContext, Principal, PrincipalId, RuntimeContext, ScopeContext, ScopeKind, SecurityLevel, SessionContext, SubjectKind}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.datastore.sql.SqlDataStore
import org.goldenport.cncf.entity.{EntityPersistent, EntityRevisionBinding, EntityRevisionRepresentation, EntitySnapshot, EntityStore, EntityStoreSpace}
import org.goldenport.cncf.entity.runtime.{EntityCollection, EntityDescriptor, EntityLoader, EntityMemoryPolicy,
  EntityRealm, EntityRealmState, EntityRuntimePlan, EntityStorage, PartitionStrategy}
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.cncf.resource.{ResourceTreeAccess, ResourceTreeLimits, ResourceTreeQuery, ResourceTreeQueryResult,
  ResourceTreeReference, ResourceTreeSnapshot}
import org.goldenport.cncf.unitofwork.{ExecUowM, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ResolvedConfiguration}
import org.goldenport.protocol.{Protocol, Request}
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.model.directive.Update
import org.simplemodeling.model.value.ContentBody
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.impl.ComponentFactory
import org.simplemodeling.textus.cbdsupport.entity.{InternalModelHistoryEntry as HistoryEntity}
import org.simplemodeling.textus.cbdsupport.entity.update.{InternalModelHistoryEntry as HistoryUpdate}
import org.simplemodeling.textus.cbdsupport.value.{InternalModelHistoryDocument as HistoryValue}

/**
 * Task-private SQLite and trusted server witnesses; each open installs fresh storage spaces.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 */
private[runtime] object InternalModelRetainedHistoryFixture {
  val time: Instant = Instant.parse("2026-10-03T01:23:45.123456789Z")
  val principalId: String = "history-server-principal"
  val packageReference: InternalModelPackageReference = InternalModelPackageReference(
    InternalModelPackageId.from("01234567-89ab-cdef-0123-456789abcdef").toOption.get,
    InternalModelProjectToken.from("org.example").toOption.get,
    InternalModelProjectToken.from("continuity-sample").toOption.get)
  val scope: InternalModelSemanticScope = InternalModelSemanticScope("component-all-eight", "context-all-eight", "e-usecase")
  val projection: InternalModelArtifactReference = InternalModelContinuationFixture.projectionReference
  val subject: InternalModelReviewSubject = InternalModelReviewSubject(reference("  対象 λ e\u0301🧭\n  ", 47L).recordId,
    reference("independent subject", 47L).recordRevision, packageReference, scope,
    Vector(projection, InternalModelContinuationFixture.realizationReference, InternalModelContinuationFixture.snapshotReference))
  val redactionReference: InternalModelRecordReference = reference("redaction:accepted λ", 89L)
  val evidence: Vector[InternalModelRetainedEvidence] = Vector(
    InternalModelRetainedEvidence(reference("provider", 19L), InternalModelEvidenceKind.ProviderIdentity,
      InternalModelRetainedEvidencePayload.Identity("provider:one")),
    InternalModelRetainedEvidence(reference("model", 23L), InternalModelEvidenceKind.ModelIdentity,
      InternalModelRetainedEvidencePayload.Identity("model-2026")),
    InternalModelRetainedEvidence(reference("tool", 29L), InternalModelEvidenceKind.ToolIdentity,
      InternalModelRetainedEvidencePayload.Identity("tool.v1")),
    InternalModelRetainedEvidence(reference("narrative", 41L), InternalModelEvidenceKind.Narrative,
      InternalModelRetainedEvidencePayload.RedactedText("  秘匿済み e\u0301 🧭\n  ", redactionReference))) ++
    InternalModelEvidenceKind.values.toVector.flatMap(kind => InternalModelEvidenceOmission.values.toVector.map(reason =>
      InternalModelRetainedEvidence(reference("omitted " + kind.toString + reason.toString, 59L), kind,
        InternalModelRetainedEvidencePayload.Omitted(reason))))

  def reference(identity: String, revision: Long): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(identity).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)
  def storageId(index: Long): InternalModelHistoryStorageId =
    value(InternalModelHistoryStorageId.from("00000000-0000-0000-0000-" + f"$index%012x"))
  def selection(index: Long = 1L, recordReference: InternalModelRecordReference = reference("  記録 λ\n🧭  ", 101L)): InternalModelHistorySelection =
    InternalModelHistorySelection(storageId(index), recordReference, packageReference, scope)
  def history(payload: InternalModelRetainedHistoryPayload = InternalModelRetainedHistoryPayload.Evidence(reference("logical evidence", 97L)),
    recordReference: InternalModelRecordReference = reference("  記録 λ\n🧭  ", 101L),
    recordSubject: InternalModelReviewSubject = subject,
    retainedEvidence: Vector[InternalModelRetainedEvidence] = evidence): InternalModelRetainedHistoryRecord =
    InternalModelRetainedHistoryRecord(recordReference, recordSubject, InternalModelDecisionActor("service", principalId, "reviewer"),
      time, payload, retainedEvidence)
  def document(index: Long = 1L, record: InternalModelRetainedHistoryRecord = history(),
    retainedAt: Instant = time): InternalModelHistoryDocument =
    InternalModelHistoryDocument(InternalModelHistorySelection(storageId(index), record.reference,
      record.subject.packageReference, record.subject.scope), InternalModelHistoryDocumentState.Retained(record, retainedAt))
  def input(index: Long = 1L, payload: InternalModelRetainedHistoryPayload = InternalModelRetainedHistoryPayload.Evidence(reference("input evidence", 97L)),
    entries: Vector[InternalModelEvidenceInput] = Vector.empty): InternalModelHistoryInput =
    InternalModelHistoryInput(storageId(index), reference("input record λ " + index, 103L), payload, entries)
  def payloads: Vector[InternalModelRetainedHistoryPayload] = {
    import InternalModelRetainedHistoryPayload.*
    InternalModelRetainedReviewOutcome.values.toVector.map(outcome => Review(reference("review", 7L),
      reference("candidate", 11L), reference("diff", 17L), outcome)) ++
      Vector(Proposal(reference("candidate", 11L), projection)) ++
      InternalModelRetainedAlternativeDisposition.values.toVector.map(disposition => Alternative(reference("proposal", 23L),
        reference("alternative", 31L), disposition)) ++ Vector(Supersession(reference("previous", 43L), reference("successor", 61L)),
        Evidence(reference("evidence", 97L)))
  }
  def value[A](result: Consequence[A]): A = result match {
    case Consequence.Success(admitted) => admitted
    case Consequence.Failure(conclusion) => throw new IllegalStateException("fixture admission failed: " + conclusion.show)
  }
  def bytes(text: String): Vector[Byte] = text.getBytes(StandardCharsets.UTF_8).toVector
  def text(bytes: Vector[Byte]): String = new String(bytes.toArray, StandardCharsets.UTF_8)

  def withDatabase(body: Path => Unit): Unit = {
    val work = Files.createDirectories(Path.of("target/internal-model-retained-history/work").toAbsolutePath)
    val root = Files.createTempDirectory(work, "sqlite-")
    try body(root.resolve("history.sqlite"))
    finally InternalModelContinuationFixture.removeTree(root)
  }

  final class TestClock(initial: Instant) extends Clock {
    private var _instant = initial
    def set(value: Instant): Unit = { _instant = value }
    override def instant(): Instant = _instant
    override def getZone(): ZoneId = ZoneOffset.UTC
    override def withZone(zone: ZoneId): Clock = Clock.fixed(_instant, zone)
  }

  final class Database(val path: Path, val clock: TestClock, roles: Set[String]) {
    private val _datastore = SqlDataStore.sqlite(path.toString)
    val providers: ProviderProbe = new ProviderProbe()
    val sources: SourceProbe = new SourceProbe()
    val component: Component = _component()
    val context: ExecutionContext = _context(_datastore, component, clock, roles, providers, sources)
    val core: ActionCall.Core = actionCore(context, Some(component))
    val store: InternalModelRetainedHistoryStore = InternalModelRetainedHistoryStore.entityBacked(core)
    def close(): Unit = { value(_datastore.closeC()); () }
    def run[A](program: ExecUowM[A]): Consequence[A] = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(program)
    def entity(selection: InternalModelHistorySelection): Option[HistoryEntity] =
      value(run(new EntityProbe(core).load(value(InternalModelRetainedHistoryStore.entityId(selection)))))
    def snapshot(selection: InternalModelHistorySelection): EntitySnapshot[HistoryEntity] =
      value(run(new EntityProbe(core).snapshot(value(InternalModelRetainedHistoryStore.entityId(selection)))))
    def replace(selection: InternalModelHistorySelection, document: InternalModelHistoryDocument,
      snapshot: EntitySnapshot[HistoryEntity]): Consequence[Unit] = {
      val documentbytes = value(InternalModelRetainedHistoryCodec.encode(document))
      val patch = value(HistoryUpdate.Builder().withHistory_document(Update.set(HistoryValue(ContentBody(text(documentbytes))))).buildC())
      run(new EntityProbe(core).replace(value(InternalModelRetainedHistoryStore.entityId(selection)), patch, snapshot)).map(_ => ())
    }
  }

  def database(path: Path, now: Instant = time, roles: Set[String] = Set("admin")): Database =
    new Database(path, new TestClock(now), roles)

  def actionCore(context: ExecutionContext, component: Option[Component] = None): ActionCall.Core =
    ActionCall.Core(TestAction(Request.ofOperation("internal-model-retained-history-spec")), context, component, None)

  def roleContext(context: ExecutionContext, roles: Set[String],
    kind: SubjectKind = SubjectKind.User, identity: String = principalId,
    session: Option[SessionContext] = None): ExecutionContext =
    ExecutionContext.withSecurityContext(context, context.security.copy(principal = TestPrincipal(PrincipalId(identity), Map("roles" -> roles.toVector.sorted.mkString(","))),
      capabilities = roles.map(Capability.apply), level = SecurityLevel(roles.toVector.sorted.headOption.getOrElse("user")),
      subjectKind = kind, session = session))

  def api(core: ActionCall.Core, capture: InternalModelVerifiedContinuationPackage,
    store: Option[InternalModelRetainedHistoryStore], selectedSubject: InternalModelReviewSubject = subject): InternalModelContinuationApi =
    value(InternalModelContinuationApi.create(core, capture, selectedSubject, principalId, Set(redactionReference), store))

  def withCapture(body: (Path, InternalModelVerifiedContinuationPackage, InternalModelContinuationRequest) => Unit): Unit = {
    InternalModelContinuationFixture.withFixture() { root =>
      val cursor = InternalModelContinuationFixture.cursor(root)
      InternalModelContinuationFixture.replaceCursor(root, cursor.copy(currentStage = "model-ready",
        nextPermittedAction = Some("inspect-projections"), preconditions = Vector.empty))
      val capture = value(InternalModelPackageValidator.verifiedContinuation(root))
      val request = InternalModelContinuationRequest(packageReference, 31L, reference("realization-all-eight", 5L), scope,
        None, None, None, None, None, None, Some(Vector.empty), None)
      body(root, capture, request)
    }
  }

  final class StoreProbe(result: InternalModelHistoryResult = InternalModelHistoryResult.Unavailable)
      extends InternalModelRetainedHistoryStore {
    private var _calls = Vector.empty[String]
    private var _documents = Vector.empty[InternalModelHistoryDocument]
    def calls: Vector[String] = _calls
    def documents: Vector[InternalModelHistoryDocument] = _documents
    def read(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] = _visit("read")
    def record(document: InternalModelHistoryDocument,
      endpoints: Option[InternalModelHistorySupersessionEndpoints]): Consequence[InternalModelHistoryResult] = {
      _documents = _documents :+ document
      _visit("record")
    }
    def expire(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] = _visit("expire")
    def delete(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] = _visit("delete")
    private def _visit(operation: String): Consequence[InternalModelHistoryResult] = {
      _calls = _calls :+ operation
      Consequence.success(result)
    }
  }

  final class ProviderProbe extends HttpDriver {
    private var _calls = 0
    def calls: Int = _calls
    def get(path: String, headers: Map[String, String], properties: Vector[org.goldenport.protocol.Property]): org.goldenport.http.HttpResponse = _unexpected()
    def post(path: String, body: Option[String], headers: Map[String, String], properties: Vector[org.goldenport.protocol.Property]): org.goldenport.http.HttpResponse = _unexpected()
    def put(path: String, body: Option[String], headers: Map[String, String], properties: Vector[org.goldenport.protocol.Property]): org.goldenport.http.HttpResponse = _unexpected()
    private def _unexpected(): org.goldenport.http.HttpResponse = {
      _calls += 1
      throw new IllegalStateException("API must not invoke a provider")
    }
  }

  final class SourceProbe extends ResourceTreeAccess {
    private var _calls = 0
    def calls: Int = _calls
    def snapshot(reference: ResourceTreeReference, limits: ResourceTreeLimits): Consequence[ResourceTreeSnapshot] = {
      _calls += 1
      Consequence.operationInvalid("API must not acquire a source")
    }
    override def query(query: ResourceTreeQuery): Consequence[ResourceTreeQueryResult] = {
      _calls += 1
      Consequence.operationInvalid("API must not query a source")
    }
  }

  def withActualComponent(body: Component => Unit): Unit = {
    val subsystem = new Subsystem("internal-model-protocol-spec", configuration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty))
    try body(value(new ComponentFactory().createPrimaryC(ComponentCreate(subsystem, ComponentOrigin.Embed))))
    finally { value(subsystem.shutdownC()); () }
  }

  private final case class TestPrincipal(id: PrincipalId, attributes: Map[String, String]) extends Principal
  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall =
      throw new UnsupportedOperationException("Fixture supplies the server Core only")
  }
  private final class EntityProbe(val core: ActionCall.Core) extends ActionCall.Core.Holder with ActionCallEntityStorePart {
    def load(id: EntityId): ExecUowM[Option[HistoryEntity]] = entity_load_option_internal[HistoryEntity](id)
    def snapshot(id: EntityId): ExecUowM[EntitySnapshot[HistoryEntity]] = entity_load_snapshot_internal[HistoryEntity](id)
    def replace(id: EntityId, patch: HistoryUpdate, snapshot: EntitySnapshot[HistoryEntity]): ExecUowM[org.goldenport.cncf.entity.EntityRecordSnapshot] =
      entity_update_internal(id, patch, snapshot.revision)
  }

  private def _component(): Component = {
    val component = new Component {
      override val core: Component.Core = Component.Core.create(CbdSupportComponent.name, CbdSupportComponent.componentId,
        ComponentInstanceId.default(CbdSupportComponent.componentId), Protocol.empty)
      override def coreOption: Option[Component.Core] = Some(core)
    }
    val persistent = summon[EntityPersistent[HistoryEntity]]
    val collection = HistoryEntity.collectionId
    val realm = new EntityRealm[HistoryEntity](collection.name, EntityLoader[HistoryEntity](_ => None),
      new IdRef(EntityRealmState(Map.empty)))
    val descriptor = EntityDescriptor(collection, EntityRuntimePlan(entityName = collection.name,
      memoryPolicy = EntityMemoryPolicy.StoreOnly, workingSet = None, partitionStrategy = PartitionStrategy.byEntityId,
      maxPartitions = 1, maxEntitiesPerPartition = 1), persistent,
      revisionBinding = Some(EntityRevisionBinding(EntityRevisionRepresentation.Embedded)))
    component.entitySpace.registerEntity(collection.name, new EntityCollection(descriptor, EntityStorage(realm)))
    component
  }

  private def _context(datastore: DataStore, component: Component, clock: Clock, roles: Set[String],
    providers: ProviderProbe, sources: SourceProbe): ExecutionContext = {
    val base = roleContext(ExecutionContext.create(clock), roles, SubjectKind.Service)
    val datastorespace = new DataStoreSpace().useDataStore(datastore)
    val entitystorespace = new EntityStoreSpace().addEntityStore(EntityStore.standard())
    lazy val context: ExecutionContext = ExecutionContext.withResourceTreeAccess(ExecutionContext.withRuntimeContext(base, runtime), sources)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = ScopeContext.Core(kind = ScopeKind.Runtime, name = "internal-model-retained-history-spec", parent = None,
        observabilityContext = base.observability, httpDriverOption = Some(providers),
        datastore = Some(DataStoreContext(datastorespace)), entitystore = Some(EntityStoreContext(entitystorespace)),
        entityspace = Some(EntitySpaceContext(component.entitySpace)), aggregateInternalRead = false,
        processExecutionDriverOption = None, processExecutionAdmissionOption = None, scopedConcurrencyAdmissionOption = None),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
      }, commitAction = _ => (), abortAction = _ => (), disposeAction = _ => (), token = "internal-model-retained-history-spec")
    runtime.setResolvedParameters(ResolvedParameters.empty())
    context
  }

  private final class IdRef[A](initial: A) extends Ref[Id, A] {
    private var _value = initial
    def get: A = synchronized(_value)
    def set(value: A): Unit = synchronized { _value = value }
    override def getAndSet(value: A): A = synchronized { val previous = _value; _value = value; previous }
    def access: (A, A => Boolean) = synchronized {
      val snapshot = _value
      val setter: A => Boolean = next => synchronized {
        if (_value == snapshot) { _value = next; true } else false
      }
      snapshot -> setter
    }
    override def tryUpdate(f: A => A): Boolean = synchronized { _value = f(_value); true }
    override def tryModify[B](f: A => (A, B)): Option[B] = synchronized {
      val (next, result) = f(_value); _value = next; Some(result)
    }
    def update(f: A => A): Unit = synchronized { _value = f(_value) }
    def modify[B](f: A => (A, B)): B = synchronized { val (next, result) = f(_value); _value = next; result }
    override def modifyState[B](state: State[A, B]): B = synchronized {
      val (next, result) = state.run(_value).value; _value = next; result
    }
    override def tryModifyState[B](state: State[A, B]): Option[B] = synchronized {
      val (next, result) = state.run(_value).value; _value = next; Some(result)
    }
  }
}
