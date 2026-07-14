package org.simplemodeling.textus.cbdsupport

import java.net.URI
import java.nio.file.{Files, Path}
import java.time.Instant

import org.goldenport.cncf.component.{ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.mcp.McpToolCatalog
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.protocol.{Property, Request}
import org.goldenport.record.Record
import org.simplemodeling.textus.cbdsupport.runtime.{ComponentEvidenceAbsence, ExactComponentSelection, InformationSourceKind, SemanticRequirementEvidence}
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
    "publish the component boundary" which {
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
      val searchschema = tools.find(_.name.endsWith(".searchComponents")).map(_.inputSchema).get
      val usageschema = tools.find(_.name.endsWith(".getUsage")).map(_.inputSchema).get
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
      searchschema.hcursor.downField("properties").keys.get.toSet should contain allOf (
        "sourceId",
        "sourceKind",
        "freshness",
        "versionState",
        "conflictCode",
        "purpose"
      )
      usageschema.hcursor.downField("properties").keys.get.toSet should contain("intent")
      dependencyschema.hcursor.downField("properties").downField("maxDepth").get[String]("type") shouldBe Right("integer")
    }

    "align static Web forms with generated service ownership" in {
      Given("a Web form exposing retrieval search and administrative refresh")
      val form = Files.readString(Path.of("src/main/web-inf/form.yaml"))

      When("the form operation identifiers are compared with the generated service contract")
      val retrievalsearch = "textus-cbd-support.cbd-retrieval.search-components"
      val adminrefresh = "textus-cbd-support.cbd-catalog-admin.refresh-catalog"

      Then("each form targets its owning service without assuming an entity result")
      form should include(retrievalsearch)
      form should include(adminrefresh)
      form should not include "textus-cbd-support.cbd-retrieval.refresh-catalog"
      form should not include "${result.id}"
    }
    }

    "project generated records" which {
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

    "project semantic requirement evidence as an independent MCP record" in {
      Given("one source-owned semantic citation")
      val evidence = SemanticRequirementEvidence(
        "semantic::runtime::urn:bok:runtime",
        "semantic",
        InformationSourceKind.SIE_BOK,
        "architecture:runtime",
        Some("Execution Runtime"),
        Some("Runtime definition."),
        Some("architecture"),
        Vector.empty,
        Some("bok-main"),
        "semantic",
        0.9,
        "SIE matched the runtime intent.",
        "observed",
        Instant.parse("2026-07-14T08:00:00Z"),
        "urn:bok:runtime",
        Vector.empty
      )

      When("the handwritten MCP projection renders the citation")
      val record = new impl.ComponentFactory()._semantic_evidence_record(evidence)

      Then("semantic ownership and evidence remain explicit without component fields")
      record.getString("sourceId") shouldBe Some("semantic")
      record.getString("sourceKind") shouldBe Some(InformationSourceKind.SIE_BOK)
      record.getString("evidenceUri") shouldBe Some("urn:bok:runtime")
      record.getString("observedAt") shouldBe Some("2026-07-14T08:00:00Z")
      record.getAny("component") shouldBe empty
    }

    "project explicit evidence absence as an attributable MCP record" in {
      Given("one absence caused by ambiguous catalog selection")
      val absence = ComponentEvidenceAbsence(
        "ambiguous-selection",
        "component-selection",
        "Multiple catalog components satisfy the exact constraints.",
        Vector("primary", "secondary"),
        Vector("1.0.0"),
        Vector(URI.create("https://catalog.example/index.json"))
      )

      When("the handwritten MCP projection renders the absence")
      val record = new impl.ComponentFactory()._absence_record(absence)

      Then("the reason and its participating evidence remain machine readable")
      record.getString("code") shouldBe Some("ambiguous-selection")
      record.getString("subject") shouldBe Some("component-selection")
      record.getString("message") shouldBe Some("Multiple catalog components satisfy the exact constraints.")
      record.getAny("sourceIds") should not be empty
      record.getAny("evidenceUris") should not be empty
    }

    "withhold the requested intent when exact component selection fails" in {
      Given("an exact usage request with no selected catalog component")
      val selection = ExactComponentSelection.fromCandidates(Vector.empty)

      When("the handwritten MCP projection renders the unselected usage response")
      val record = new impl.ComponentFactory()._unselected_usage_record(selection)

      Then("the unvalidated intent is withheld while bounded response fields remain present")
      record.getAny("intent") shouldBe empty
      record.getAny("guidance") shouldBe empty
      record.getAny("absences") should not be empty
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
          Property("kind", "sar", None),
          Property("intent", "compose an application", None)
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
      val usageintent = factory._optional_string(usageaction.record, "intent")
      val dependencykind = factory._optional_string(dependencyaction.record, "kind")

      Then("both operations retain the disambiguating component kind")
      usagekind shouldBe Some("sar")
      usageintent shouldBe Some("compose an application")
      dependencykind shouldBe Some("sar")
    }
    }
  }
}
