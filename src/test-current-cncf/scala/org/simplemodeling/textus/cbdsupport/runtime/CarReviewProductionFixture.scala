package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import java.security.MessageDigest
import cats.~>
import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.goldenport.Consequence
import org.goldenport.configuration.{Configuration, ConfigurationValue}
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.*
import org.goldenport.cncf.datastore.DataStore
import org.goldenport.cncf.entity.EntityPersistent
import org.goldenport.cncf.entity.runtime.EntitySpace
import org.goldenport.cncf.event.{EventEngine, EventStore}
import org.goldenport.cncf.job.{JobId, JobSubmitter, NativeCbdReviewJobFixtures}
import org.goldenport.cncf.processexecution.*
import org.goldenport.cncf.unitofwork.*
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.{CbdReviewAdminService, CbdRetrievalService}
import org.simplemodeling.textus.cbdsupport.impl.{ComponentFactory, ReviewJobProgram}

/*
 * Full CLI bootstrap, generated Component/Entity registration and admitted process
 * capability; only the external Cozy process result is a controlled test double.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] final class CarReviewProductionFixture(root: Path, jobs: Vector[JobId] = Vector.empty) {
  import CarReviewJobExecutionFixtures.{value, load}
  private val _factory = new ComponentFactory
  private var _component: Component = null
  val native = value(NativeCbdReviewJobFixtures.openRuntime(root, jobs, JobSubmitter("review-owner", "User"), selected => {
    _component = value(_factory.createPrimaryC(ComponentCreate(selected, ComponentOrigin.Builtin)))
    Vector(value(new org.goldenport.cncf.component.ComponentFactory().bootstrapC(_component)))
  }))
  val template = CarReviewReportCodec.withCalculatedDigest(
    CarReviewReportCodec.decode(load("car-review-report-v1.json")).toOption.get.copy(baseline = None)).toOption.get
  val descriptor = load("car-review-provider-descriptor-v1.json")
  private val _descriptor = CarReviewProviderBundleAdmission.describeDescriptor(descriptor).toOption.get
  private val _selection = CarReviewReuseProviderSelection(_descriptor.provider, _descriptor.ruleSet,
    CarReviewDeclaredReference("availability:cozy", 1L))
  private val _input = CarReviewReuseKeyInput(CarReviewReuseKey.DEFINITION_ID, template.schemaVersion,
    CarReviewDeclaredReference("target:server", 1L), template.target.kind, template.target.organization,
    template.target.name, template.target.version, template.profile, None, Vector(_selection.ruleSet), Vector(_selection),
    Vector.empty, Vector("profile", "gate", "reconciliation", "suppression").map(scope =>
      CarReviewReusePolicyBinding(scope, if (scope == "gate") template.gate.policyId else scope + "-default",
        if (scope == "gate") template.gate.policyVersion else ReviewVersion("1.0"), 1L)))
  val configuration: String = Json.obj(
    "schemaVersion" -> Json.fromString("textus.cbd.review-server.v1"),
    "diagnosisId" -> Json.fromString("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-productionPlan"),
    "revision" -> Json.fromLong(1L),
    "selection" -> parse(CarReviewExecutionSelectionCodec.encode(_input).toOption.get).toOption.get,
    "template" -> parse(CarReviewReportCodec.encode(template).toOption.get).toOption.get,
    "providers" -> Json.arr(Json.obj("descriptor" -> parse(descriptor).toOption.get,
      "request" -> parse(load("car-review-provider-request-v1.json")).toOption.get))).noSpaces
  configure(Some(configuration))
  private val _driver = new ProviderDriver
  private val _limits = ProcessExecutionLimits(Some(1000L), Some(120000L), Some(1000L), Some(65536L),
    Some(4096L), Some(65536L), Some(1L), Some(4096L), Some(1L), Some(4096L), Some(65536L))
  private val _definition = value(ProcessProgramDefinition.fromRuntimeC(CozyCarReviewProviderProcess.capability,
    "cozy-car-review", "test-runtime-owned-cozy", Vector.empty, ProcessArgumentPolicy(Vector.empty), _limits, Set.empty))
  private val _admission = value(ProcessExecutionAdmission.createC(value(ProcessExecutionPolicy.createC(Vector(_definition))),
    Vector(ProcessExecutionGrant(CozyCarReviewProviderProcess.capability))))
  private val _contexts = scala.collection.mutable.ArrayBuffer.empty[ExecutionContext]
  private val _entity_space = new EntitySpace
  (native.context.entitySpace.entityCollections ++ _component.entitySpace.entityCollections).foreach { collection =>
    _entity_space.registerEntity(collection.descriptor.plan.entityName, collection)
  }

  def executions: Int = _driver.executions
  var beforeProvider: () => Unit = () => ()
  def configure(document: Option[String]): Unit = {
    _component.withApplicationConfig(Component.ApplicationConfig(config = Some(Configuration(
      document.map(value => Map(CarReviewServerExecution.configurationKey -> ConfigurationValue.StringValue(value)))
        .getOrElse(Map.empty)))))
    ()
  }

  def context(owner: String = "review-owner", permitted: Boolean = true): ExecutionContext = {
    val principal = new Principal { val id = PrincipalId(owner); val attributes = Map.empty[String, String] }
    val base = ExecutionContext.withSecurityContext(native.context, SecurityContext(principal,
      if (permitted) Set(Capability("operator")) else Set.empty, SecurityLevel(if (permitted) "operator" else "anonymous"), SubjectKind.User))
    lazy val ctx: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
    lazy val uow = native.unitOfWork(ctx, EventEngine.noop(DataStore.noop(), CommitRecorder.noop, EventStore.inMemory), CommitRecorder.noop)
    lazy val interpreter = new UnitOfWorkInterpreter(uow)
    lazy val runtime: RuntimeContext = new RuntimeContext(ScopeContext.Core(ScopeKind.Runtime, "cbd-production", Some(native.subsystem.globalRuntimeContext),
      base.observability, None, Some(DataStoreContext(native.context.dataStoreSpace)), Some(EntityStoreContext(native.entityStoreSpace)),
      Some(EntitySpaceContext(_entity_space)), processExecutionDriverOption = Some(_driver), processExecutionAdmissionOption = Some(_admission)),
      () => uow, new (UnitOfWorkOp ~> Consequence) { def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation) },
      unit => { unit.commit(); () }, unit => { unit.abort(); () }, unit => { unit.dispose(); () }, "cbd-production")
    _contexts += ctx
    ctx
  }

  def start(ctx: ExecutionContext = context()): Consequence[Record] = {
    val target = template.target
    val properties = Vector("targetKind" -> target.kind.value, "name" -> target.name, "targetDigest" -> target.digest.value,
      "profile" -> template.profile.value) ++ target.organization.map("organization" -> _) ++ target.version.map(value => "version" -> value.value)
    val request = Request.of(component = "CbdSupport", service = "CbdReviewAdmin", operation = "startReview",
      properties = properties.map { case (name, value) => Property(name, value, None) }.toList)
    CbdReviewAdminService.StartReviewOperation.createOperationRequest(request).flatMap(action => execute(action, ctx))
  }

  def get(reviewid: String, ctx: ExecutionContext = context()): Consequence[Record] =
    CbdRetrievalService.GetReviewRunOperation.createOperationRequest(Request.of(component = "CbdSupport", service = "CbdRetrieval",
      operation = "getReviewRun", properties = List(Property("reviewId", reviewid, None)))).flatMap(action => execute(action, ctx))

  def cancel(reviewid: String, ctx: ExecutionContext = context()): Consequence[Record] =
    CbdReviewAdminService.CancelReviewOperation.createOperationRequest(Request.of(component = "CbdSupport", service = "CbdReviewAdmin",
      operation = "cancelReview", properties = List(Property("reviewId", reviewid, None)))).flatMap(action => execute(action, ctx))

  def report(reportid: String, ctx: ExecutionContext = context()): Consequence[Record] =
    CbdRetrievalService.GetReviewReportOperation.createOperationRequest(Request.of(component = "CbdSupport", service = "CbdRetrieval",
      operation = "getReviewReport", properties = List(Property("reportId", reportid, None)))).flatMap(action => execute(action, ctx))

  def count[E: EntityPersistent](collectionid: org.simplemodeling.model.datatype.EntityCollectionId): Int = rows[E](collectionid).size
  def rows[E: EntityPersistent](collectionid: org.simplemodeling.model.datatype.EntityCollectionId): Vector[E] = {
    val ctx = context()
    value(native.managed(new UnitOfWorkInterpreter(ctx.unitOfWork).interpret(UnitOfWorkOp.EntityStoreSearchInternal(
      org.goldenport.cncf.entity.EntityQuery[E](collectionid, org.goldenport.cncf.directive.Query(
        org.goldenport.cncf.directive.Query.Plan(Record.empty, limit = Some(10)))), summon[EntityPersistent[E]])))).data
  }

  def execute(action: Action, ctx: ExecutionContext): Consequence[Record] = native.managed {
    _component.actionEngine.execute(action.createCall(ActionCall.Core(action, ctx, Some(_component), None))).flatMap {
      case OperationResponse.RecordResponse(record) => Consequence.success(record)
      case _ => Consequence.operationInvalid("production-fixture-response-not-record")
    }
  }
  def close(): Unit = { _contexts.foreach(_.unitOfWork.dispose()); native.close() }

  private final class ProviderDriver extends ProcessExecutionDriver {
    var executions = 0
    val safeIdentity = "controlled-cozy-provider"
    def startC(execution: ResolvedProcessExecution): Consequence[ProcessExecutionHandle] = {
      executions += 1
      beforeProvider()
      val bytes = execution.request.input match {
        case ProcessExecutionInput.Bytes(value) => value
        case _ => throw new IllegalStateException("provider request bytes required")
      }
      val request = parse(new String(bytes.toArray, UTF_8)).toOption.get
      val bundle = parse(load("car-review-evidence-bundle-v1.json")).toOption.get.mapObject(_.remove("bundleDigest")
        .add("reviewId", request.hcursor.downField("reviewId").focus.get)
        .add("target", request.hcursor.downField("target").focus.get)
        .add("requestDigest", Json.fromString(_wire_digest(request))))
      val output = Printer.noSpacesSortKeys.print(bundle.mapObject(_.add("bundleDigest", Json.fromString(_wire_digest(bundle))))).getBytes(UTF_8).toVector
      Consequence.success(new ProcessExecutionHandle {
        def awaitC = Consequence.success(ProcessExecutionResult(ProcessExecutionTermination.Exited(0),
          ProcessExecutionCapture(output, output.size.toLong, false), ProcessExecutionCapture(Vector.empty, 0L, false), Vector.empty, 0L, safeIdentity))
        def cancelC = Consequence.unit
      })
    }
    // Existing provider wire integrity only; never management identity or revision.
    private def _wire_digest(json: Json): String = "sha256:" + MessageDigest.getInstance("SHA-256")
      .digest(Printer.noSpacesSortKeys.print(json).getBytes(UTF_8)).map(byte => f"${byte & 0xff}%02x").mkString
  }
}
