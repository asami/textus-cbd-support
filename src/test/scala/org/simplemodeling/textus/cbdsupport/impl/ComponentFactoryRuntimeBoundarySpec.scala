package org.simplemodeling.textus.cbdsupport.impl

import java.net.URI
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext as ScalaExecutionContext, Future}

import cats.~>
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, Action}
import org.goldenport.cncf.component.Component
import org.goldenport.cncf.context.{ExecutionContext, RuntimeContext}
import org.goldenport.cncf.resource.{ResourceTreeAccess, ResourceTreeEntry, ResourceTreeReference}
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.configuration.{Configuration, ConfigurationValue}
import org.goldenport.protocol.Request
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.runtime.{BokFetcher, CatalogFetcher, CbdRuntimeInvocation}

/*
 * @since   Jul. 18, 2026
 * @version Jul. 20, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentFactoryRuntimeBoundarySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD component runtime ownership" should {
    "reuse configuration state while retaining each ActionCall's local source view" in {
      Given("one configured component factory and two contexts with distinct admitted working trees")
      val factory = new ComponentFactory()
      val basecontext = ExecutionContext.create()
      val component = _component(factory)
      val firstcontext = _context(basecontext, "alpha-component")
      val secondcontext = _context(basecontext, "beta-component")

      When("the factory resolves and initializes both ActionCalls in sequence")
      val first = _value(factory._runtime_for(_core(firstcontext, Some(component))))
      val repeated = _value(factory._runtime_for(_core(firstcontext, Some(component))))
      val second = _value(factory._runtime_for(_core(secondcontext, Some(component))))
      first.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true
      second.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true

      Then("remote state is shared while each view retains only its own admitted observation")
      (repeated eq first) shouldBe false
      (second eq first) shouldBe false
      (repeated._shared_runtime eq first._shared_runtime) shouldBe true
      (second._shared_runtime eq first._shared_runtime) shouldBe true
      _local_names(first) shouldBe Vector("alpha-component")
      _local_names(second) shouldBe Vector("beta-component")
      first.informationSourceStates(includeDisabled = false).find(_.descriptor.id == "working").map(_.status) shouldBe Some("ready")
      second.informationSourceStates(includeDisabled = false).find(_.descriptor.id == "working").map(_.status) shouldBe Some("ready")
    }

    "retain distinct ActionCall-local inventories when factory resolution and initialization overlap" in {
      Given("one factory, one configuration boundary, and two independently admitted working trees")
      given ScalaExecutionContext = ScalaExecutionContext.global
      val factory = new ComponentFactory()
      val basecontext = ExecutionContext.create()
      val component = _component(factory)

      When("both ActionCalls resolve and initialize concurrently")
      val results = Await.result(Future.sequence(Vector(
        Future(_initialized(factory, _context(basecontext, "concurrent-alpha"), component)),
        Future(_initialized(factory, _context(basecontext, "concurrent-beta"), component))
      )), 10.seconds)

      Then("both views retain their own tree while using one configuration-scoped runtime")
      results.map(_._2.isSuccess).forall(identity) shouldBe true
      _local_names(results.head._1) shouldBe Vector("concurrent-alpha")
      _local_names(results(1)._1) shouldBe Vector("concurrent-beta")
      (results.head._1._shared_runtime eq results(1)._1._shared_runtime) shouldBe true
    }

    "diagnose a declared resource tree that the ActionCall has not been admitted" in {
      Given("a component declaration for one development tree and an execution context with no admitted tree")
      val factory = new ComponentFactory()
      val component = factory._create_uninitialized_component().withApplicationConfig(
        Component.ApplicationConfig(config = Some(Configuration(Map(
          "textus.cbd.development.trees" -> ConfigurationValue.StringValue("working=missing-tree")
        ))))
      )
      val context = ExecutionContext.withResourceTreeAccess(
        ExecutionContext.create(),
        ResourceTreeAccess.inMemory(Map.empty)
      )
      val invocation = _value(factory._runtime_for(_core(context, Some(component))))

      When("the ActionCall initializes its declared local inputs")
      val initialized = invocation.ensureInputsReady(EmptyFederatedFetcher)
      val states = invocation.informationSourceStates(includeDisabled = false)

      Then("the missing tree remains an attributable degraded source without host fallback")
      val state = states.find(_.descriptor.id == "working").getOrElse(fail("Declared local source was not projected."))
      initialized.isSuccess shouldBe true
      state.status shouldBe "degraded"
      state.observationCount shouldBe 0
      state.diagnostics.mkString("\n") should include("Resource tree missing-tree is unavailable.")
      state.diagnostics.mkString("\n") should include("configured logical resource tree is not available")
    }

    "discover every bounded project descriptor for a configured working source" in {
      Given("a configured working source with two nested project descriptors and an unrelated file")
      val factory = new ComponentFactory()
      val component = _component(factory)
      val reference = _value(ResourceTreeReference.parseC("working"))
      val context = ExecutionContext.withResourceTreeAccess(
        ExecutionContext.create(),
        ResourceTreeAccess.inMemory(Map(reference -> Vector(
          _project_entry("alpha/project.yaml", "alpha-component"),
          _project_entry("beta/project.yaml", "beta-component"),
          _value(ResourceTreeEntry.createC("beta/readme.md", Vector(1.toByte)))
        )))
      )
      val invocation = _value(factory._runtime_for(_core(context, Some(component))))

      When("the ActionCall initializes its admitted local inputs")
      val initialized = invocation.ensureInputsReady(EmptyFederatedFetcher)

      Then("each matching descriptor becomes working evidence without broad snapshot discovery")
      initialized.isSuccess shouldBe true
      _local_names(invocation) shouldBe Vector("alpha-component", "beta-component")
      invocation.informationSourceStates(includeDisabled = false)
        .find(_.descriptor.id == "working")
        .map(_.status) shouldBe Some("ready")
    }

    "initialize admitted local inputs before catalog and status response projection" in {
      Given("a configured working source whose descriptor has not yet been initialized")
      val factory = new ComponentFactory()
      val component = _component(factory)
      val context = _context(_executable_context(), "ready-component")
      val core = _core(context, Some(component))
      val catalogrequest = CbdSupportComponent.CbdRetrievalService.CatalogListRequest.unsafeForTest(
        Request.ofOperation("listCatalogs"),
        Record.empty
      )
      val statusrequest = CbdSupportComponent.CbdRetrievalService.CbdStatusRequest.unsafeForTest(
        Request.ofOperation("status"),
        Record.empty
      )

      When("catalog and status ActionCalls execute through their normal FunctionalActionCall path")
      val catalog = factory.CbdRetrieval.createListCatalogsActionCall(core, catalogrequest).execute()
      val status = factory.CbdRetrieval.createStatusActionCall(core, statusrequest).execute()

      Then("both read-only responses project the initialized working source as ready")
      catalog.toOption.map(_.display).getOrElse("") should include("working")
      status.toOption.map(_.display).getOrElse("") should include("working=ready")
    }

    "return malformed declared configuration as a structured failure" in {
      Given("a component whose catalog declaration has a non-string value")
      val factory = new ComponentFactory()
      val component = factory._create_uninitialized_component().withApplicationConfig(
        Component.ApplicationConfig(config = Some(Configuration(Map(
          "textus.cbd.catalogs" -> ConfigurationValue.NumberValue(BigDecimal(1))
        ))))
      )

      When("the factory resolves the ActionCall runtime boundary")
      val result = factory._runtime_for(_core(ExecutionContext.create(), Some(component)))

      Then("configuration decoding remains a normal Consequence failure")
      val conclusion = result match {
        case Consequence.Failure(value) => value
        case Consequence.Success(_) => fail("Malformed component configuration was accepted.")
      }
      conclusion.display should include("declared component configuration requires a string value")
    }
  }

  private def _core(
    context: ExecutionContext,
    component: Option[Component] = None
  ): ActionCall.Core =
    ActionCall.Core(TestAction(Request.ofOperation("cbd-runtime-boundary")), context, component, None)

  private def _component(factory: ComponentFactory): Component =
    factory._create_uninitialized_component().withApplicationConfig(
      Component.ApplicationConfig(
        config = Some(Configuration(Map(
          "textus.cbd.development.trees" -> ConfigurationValue.StringValue("working=working")
        )))
      )
    )

  private def _context(base: ExecutionContext, componentname: String): ExecutionContext = {
    val reference = _value(ResourceTreeReference.parseC("working"))
    val entry = _project_entry("project.yaml", componentname)
    ExecutionContext.withResourceTreeAccess(
      base,
      ResourceTreeAccess.inMemory(Map(reference -> Vector(entry)))
    )
  }

  private def _project_entry(
    path: String,
    componentname: String
  ): ResourceTreeEntry =
    _value(ResourceTreeEntry.createC(
      path,
      s"""project:
         |  name: $componentname
         |  component:
         |    name: $componentname
         |    version: 1.0.0-SNAPSHOT
         |  organization: org.example
         |  kind: car
         |""".stripMargin.getBytes(StandardCharsets.UTF_8).toVector
    ))

  private def _initialized(
    factory: ComponentFactory,
    context: ExecutionContext,
    component: Component
  ): (CbdRuntimeInvocation, Consequence[Unit]) = {
    val invocation = _value(factory._runtime_for(_core(context, Some(component))))
    invocation -> invocation.ensureInputsReady(EmptyFederatedFetcher)
  }

  private def _executable_context(): ExecutionContext = {
    val base = ExecutionContext.create()
    lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = RuntimeContext.core("cbd-component-factory-runtime-boundary-spec", None, base.cncfCore.observability),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
      },
      commitAction = _ => (),
      abortAction = _ => (),
      disposeAction = _ => (),
      token = "cbd-component-factory-runtime-boundary-spec"
    )
    context
  }

  private def _local_names(invocation: CbdRuntimeInvocation): Vector[String] =
    invocation._local_inventory_snapshot.toVector.flatMap(_.observations).flatMap(_.componentName).distinct.sorted

  private def _value[A](consequence: Consequence[A]): A = consequence match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => fail(conclusion.display)
  }

  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall = {
      val _ = core
      throw new UnsupportedOperationException("TestAction is an ActionCall.Core fixture only.")
    }
  }

  private object EmptyFederatedFetcher extends CatalogFetcher with BokFetcher {
    def get(uri: URI): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected catalog fetch: $uri")

    override def get(uri: URI, maxbytes: Int): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected bounded fetch: $uri")
  }
}
