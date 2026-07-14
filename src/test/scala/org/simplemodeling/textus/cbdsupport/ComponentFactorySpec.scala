package org.simplemodeling.textus.cbdsupport

import org.goldenport.cncf.component.{ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.mcp.McpToolCatalog
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.protocol.{Property, Request}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentFactorySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentFactory" should {
    "expose the generated CBD service factories" in {
      Given("a freshly constructed handwritten component factory")
      val factory = new impl.ComponentFactory()

      When("the generated service accessors are inspected")
      val retrieval = factory.CbdRetrieval
      val admin = factory.CbdCatalogAdmin

      Then("both the retrieval and administrative boundaries are available")
      factory.primaryFactory should not be null
      retrieval should not be null
      admin should not be null
    }

    "publish only the retrieval service through MCP" in {
      Given("the component instance constructed by the handwritten factory")
      val component = new impl.ComponentFactory().createUninitializedComponent()

      When("the service-level MCP publication policy is evaluated")
      val searchready = component.isMcpReady("CbdRetrieval", "searchComponents")
      val refreshready = component.isMcpReady("CbdCatalogAdmin", "refreshCatalog")

      Then("CBD read operations are ready and catalog mutation remains private")
      searchready shouldBe true
      refreshready shouldBe false
    }

    "project the CBD detail operations as distinct CNCF MCP tools" in {
      Given("an initialized CBD component")
      val subsystem = DefaultSubsystemFactory.default(Some("server"))
      val component = new impl.ComponentFactory().create(
        ComponentCreate(subsystem, ComponentOrigin.Repository("cbd-support-spec"))
      ).primary

      When("CNCF projects the component MCP catalog")
      val tools = McpToolCatalog.toolsForComponent(component)
      val names = tools.map(_.name).toSet
      val dependencyschema = tools.find(_.name.endsWith(".resolveDependencies")).map(_.inputSchema).get

      Then("all retrieval tools are visible and administrative refresh is absent")
      names shouldBe Set(
        "CbdSupport.CbdRetrieval.searchComponents",
        "CbdSupport.CbdRetrieval.getComponent",
        "CbdSupport.CbdRetrieval.getUsage",
        "CbdSupport.CbdRetrieval.resolveDependencies",
        "CbdSupport.CbdRetrieval.listCatalogs",
        "CbdSupport.CbdRetrieval.status"
      )
      tools.map(x => x.name -> x.description).toMap shouldBe Map(
        "CbdSupport.CbdRetrieval.searchComponents" -> "CbdSupport.CbdRetrieval.searchComponents",
        "CbdSupport.CbdRetrieval.getComponent" -> "CbdSupport.CbdRetrieval.getComponent",
        "CbdSupport.CbdRetrieval.getUsage" -> "CbdSupport.CbdRetrieval.getUsage",
        "CbdSupport.CbdRetrieval.resolveDependencies" -> "CbdSupport.CbdRetrieval.resolveDependencies",
        "CbdSupport.CbdRetrieval.listCatalogs" -> "CbdSupport.CbdRetrieval.listCatalogs",
        "CbdSupport.CbdRetrieval.status" -> "CbdSupport.CbdRetrieval.status"
      )
      dependencyschema.hcursor.downField("properties").downField("maxDepth").get[String]("type") shouldBe Right("integer")
    }

    "extract canonical scalar values from generated CML value types" in {
      Given("a request record containing generated component identity and kind values")
      val factory = new impl.ComponentFactory()
      val record = Record.data(
        "name" -> org.simplemodeling.textus.cbdsupport.value.ComponentName("textus-semantic-integration-engine"),
        "kind" -> Record.data("value" -> "car"),
        "organization" -> Map("value" -> "org.textus")
      )

      When("the handwritten runtime reads the typed request fields")
      val name = factory._optional_string(record, "name")
      val kind = factory._optional_string(record, "kind")
      val organization = factory._optional_string(record, "organization")

      Then("the domain values are unwrapped without exposing case-class rendering")
      name shouldBe Some("textus-semantic-integration-engine")
      kind shouldBe Some("car")
      organization shouldBe Some("org.textus")
    }

    "extract canonical scalars from generated operation request records" in {
      Given("an MCP-equivalent request normalized into CNCF properties")
      val request = Request.of(
        component = "CbdSupport",
        service = "CbdRetrieval",
        operation = "searchComponents",
        properties = List(
          Property("requirement", "semantic integration", None),
          Property("kind", "car", None)
        )
      )
      val action = CbdSupportComponent.CbdRetrievalService.SearchComponentsOperation
        .createOperationRequest(request).toOption.get
      val factory = new impl.ComponentFactory()

      When("the handwritten runtime reads the generated request record")
      val requirement = factory._optional_string(action.record, "requirement")
      val kind = factory._optional_string(action.record, "kind")

      Then("the original MCP scalar values are preserved")
      requirement shouldBe Some("semantic integration")
      kind shouldBe Some("car")
    }

    "preserve CAR or SAR kind during usage and dependency handoff" in {
      Given("MCP-equivalent usage and dependency requests for one SAR")
      val usagerequest = Request.of(
        component = "CbdSupport",
        service = "CbdRetrieval",
        operation = "getUsage",
        properties = List(
          Property("name", "textus-application", None),
          Property("kind", "sar", None)
        )
      )
      val dependencyrequest = Request.of(
        component = "CbdSupport",
        service = "CbdRetrieval",
        operation = "resolveDependencies",
        properties = List(
          Property("name", "textus-application", None),
          Property("kind", "sar", None)
        )
      )
      val usageaction = CbdSupportComponent.CbdRetrievalService.GetUsageOperation
        .createOperationRequest(usagerequest).toOption.get
      val dependencyaction = CbdSupportComponent.CbdRetrievalService.ResolveDependenciesOperation
        .createOperationRequest(dependencyrequest).toOption.get
      val factory = new impl.ComponentFactory()

      When("the generated request records are read by the handwritten runtime")
      val usagekind = factory._optional_string(usageaction.record, "kind")
      val dependencykind = factory._optional_string(dependencyaction.record, "kind")

      Then("both operations retain the disambiguating component kind")
      usagekind shouldBe Some("sar")
      dependencykind shouldBe Some("sar")
    }
  }
}
