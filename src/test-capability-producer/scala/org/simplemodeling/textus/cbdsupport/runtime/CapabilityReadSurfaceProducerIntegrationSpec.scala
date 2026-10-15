package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import org.goldenport.cncf.component.capability.ComponentCapabilityPackageSource.Location
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityProducerIntegrationFixtures.*
import CapabilityReadSurfaceFixtures.*

/*
 * Normal producer -> CNCF Factory -> CBD Snapshot -> production native surfaces.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityReadSurfaceProducerIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Real producer Capability read surfaces" should {
    "P01 retain the original positive Usage and three realization kinds across generated API, MCP and native forms" in {
      withIntake { fixture =>
        Given("normal source-free First Store/Remote products and original public Factory metadata providers")
        val storeprovider = provider("Store")
        val remoteprovider = provider("Remote")
        val live = admitted(fixture.selection(Vector(fixture.directory("Store", "First", storeprovider),
          fixture.directory("Remote", "First", remoteprovider))), "surface-real-primary", 111)
        val native = new Native(live.catalog)
        val query = right(CapabilityQueries.open(Right(live.catalog.projection), CatalogSelection(live.snapshot.reference, live.snapshot.provenance)))
        val componentref = CapabilityReference(ModelElementId("Model:能力/A", "Component:Store|α"), "contract:二")
        val usageref = ComponentUsageCapabilityReference(ModelElementId("Model:能力/A", "ComponentUsage:One"), "app:v1")
        val capselector = Json.obj("reference" -> CapabilityCatalogJsonEncodingPart._capability_reference(componentref))
        val usageselector = Json.obj("reference" -> CapabilityCatalogJsonEncodingPart._usage_reference(usageref))
        val usecaseselector = Json.obj("reference" -> Json.obj("modelId" -> Json.fromString("Model:能力/A"), "elementId" -> Json.fromString("usecase:one")))
        val cases = Vector(
          "getCapabilityOverview" -> Json.obj(),
          "getCapability" -> capselector.mapObject(_.add("kind", Json.fromString("EXACT"))),
          "getCapability" -> Json.obj("kind" -> Json.fromString("SEARCH"), "text" -> Json.fromString("STORE CAPABILITY")),
          "findProviders" -> capselector,
          "findRequirements" -> Json.obj("component" -> CapabilityComponentUsageJsonEncodingPart._component_coordinate(coordinate("Store"))),
          "traceComponentUsageCapability" -> usageselector,
          "traceUseCaseDemand" -> usecaseselector,
          "diagnoseCapabilityDemand" -> usageselector.mapObject(_.add("kind", Json.fromString("COMPONENT_USAGE"))))
        When("each operation is dispatched through real generated API, tools/call and validated native operation forms")
        val results = cases.map { (operation, selector) =>
          Given("the actual generated " + operation + " operation with an explicit selector")
          When("the three native surfaces consume the same server-owned admitted projection and independent selection")
          val result = native.read(operation, selector)
          Then("semantic JSON and actual rendered response HTML agree across all three native surfaces")
          _parity(result)
          result.formhtml should include("name=\"selector\"")
          _envelope(result).hcursor.get[String]("operation") shouldBe Right(operation)
          _envelope(result).hcursor.downField("scope").focus shouldBe Some(CapabilityCatalogJsonEncodingPart.encodeSelection(CatalogSelection(live.snapshot.reference, live.snapshot.provenance)))
          result
        }
        Then("the original qualified Usage -> Required -> Provided path owns only its own three realization targets")
        val trace = _envelope(results(5)).hcursor.downField("value").downField("result").downField("value").downArray
        val usage = trace.downField("usage")
        val required = trace.downField("paths").downArray.downField("required").downArray
        val matched = required.downField("providers").downField("matches").downArray
        val providerrecord = matched.downField("provider")
        val realizations = providerrecord.get[Vector[Json]]("realizations").toOption.get
        usage.downField("key").downField("reference").downField("identity").get[String]("modelId") shouldBe Right("Model:能力/A")
        usage.downField("key").downField("reference").get[String]("contractVersion") shouldBe Right("app:v1")
        usage.downField("definition").focus shouldBe Some(CapabilityComponentUsageJsonEncodingPart._definition(
          fixture.usageProjection("Store", "First").documents.head.componentUsageCapabilities.head))
        required.downField("required").downField("declaration").get[String]("direction") shouldBe Right("REQUIRED")
        matched.downField("basis").get[String]("kind") shouldBe Right("SAME_VERSION")
        realizations.map(_.hcursor.downField("edge").get[String]("targetKind").toOption.get) shouldBe Vector("OPERATION", "WORKFLOW", "STATE_MACHINE")
        realizations.map(_.hcursor.downField("owner").focus) shouldBe Vector.fill(3)(providerrecord.downField("key").focus)
        realizations.map(_.hcursor.downField("target").focus) shouldBe CapabilityProducerNativeFixtures.bindings.map(b => Some(CapabilityCatalogJsonEncodingPart._target_contract(b.target)))
        _envelope(results(7)).hcursor.downField("value").get[Vector[Json]]("gaps") shouldBe Right(Vector.empty)
        results(5).api.hcursor.get[String]("html") shouldBe Right(CapabilityWebView.usageTrace(CapabilityViews.usageTrace(query, usageref).toOption.get))
        _envelope(results(2)).hcursor.downField("value").get[String]("designation") shouldBe Right("DISCOVERY_ONLY")
        _envelope(results(6)).hcursor.get[String]("status") shouldBe Right("UNAVAILABLE_PROJECTION")
        _envelope(results(6)).hcursor.downField("failure").downField("value").get[String]("detail") shouldBe Right(live.snapshot.useCase.detail)
        native.toolNames.toSet should contain allElementsOf cases.map((operation, _) => CbdSupportComponent.name + ".CbdRetrieval." + operation)
        native.toolNames.count(_.startsWith(CbdSupportComponent.name + ".CbdRetrieval.")) shouldBe 19
        native.toolNames.exists(_.contains("CbdReviewAdmin")) shouldBe false
        Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
        fixture.cmlInputs shouldBe Vector.empty

        Given("an anonymous native context and a separate ordinary ingress without a read role")
        val untrusted = new Native(live.catalog, authenticated = false)
        When("anonymous generated dispatch and unauthenticated tools/call attempt a read")
        val anonymous = native.anonymous("getCapabilityOverview", Json.obj())
        val deniedmcp = untrusted.mcpOutcome("getCapabilityOverview", Json.obj())
        val deniedapi = untrusted.api("getCapabilityOverview", Json.obj())
        val errorhtml = native.errorHtml("getCapabilityOverview", anonymous.map(_ => "unexpected success").fold(_.display, identity))
        Then("all actual calls refuse access and the native result template retains visible structured errors")
        anonymous.isSuccess shouldBe false
        deniedapi.isSuccess shouldBe false
        deniedmcp.hcursor.downField("result").get[Boolean]("isError") shouldBe Right(true)
        errorhtml should include("role=\"alert\"")
        errorhtml should include("capability.read requires a read role")
        Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
      }
    }

    "P02 retain actual Contracts Archive MissingRealization and unavailable Usage with no borrowed realization" in {
      withIntake { fixture =>
        Given("normal Contracts Store/Archive products admitted through original Factory, ingestion and readCurrent")
        val live = admitted(fixture.directories(Vector("Store", "Archive"), "Contracts"), "surface-real-contracts", 112)
        val native = new Native(live.catalog)
        val required = live.catalog.capabilities.find(r => r.key.context.component == coordinate("Store") && r.declaration.direction == Direction.Required).get
        val selector = Json.obj("kind" -> Json.fromString("REQUIRED"), "key" -> CapabilityReadJsonPart.recordKey(required.key))
        When("the actual full Required key is diagnosed through all native read surfaces")
        val result = native.read("diagnoseCapabilityDemand", selector)
        val diagnosis = _envelope(result).hcursor.downField("value")
        val gaps = diagnosis.get[Vector[Json]]("gaps").toOption.get
        val matches = diagnosis.downField("required").downArray.downField("providers").get[Vector[Json]]("matches").toOption.get
        Then("only Archive's own empty realization Presence creates the typed gap and Store retains its own targets")
        _parity(result)
        gaps.map(_.hcursor.get[String]("kind").toOption.get) shouldBe Vector("MISSING_REALIZATION")
        gaps.head.hcursor.downField("path").downField("provider").downField("provider").downField("key").downField("context").downField("component").get[String]("id") shouldBe Right("Archive")
        gaps.head.hcursor.downField("original").get[String]("state") shouldBe Right("PRESENT")
        gaps.head.hcursor.downField("original").get[Vector[Json]]("value") shouldBe Right(Vector.empty)
        matches.map(_.hcursor.downField("provider").get[Vector[Json]]("realizations").toOption.get.size) shouldBe Vector(3, 0)
        matches.head.hcursor.downField("provider").get[Vector[Json]]("realizations").toOption.get.map(_.hcursor.downField("owner").focus) shouldBe
          Vector.fill(3)(matches.head.hcursor.downField("provider").downField("key").focus)
        result.formresulthtml should include("MissingRealization")
        Given("the actual Contracts product's explicitly unavailable Usage family")
        val reference = ComponentUsageCapabilityReference(ModelElementId("Model:能力/A", "ComponentUsage:One"), "app:v1")
        When("the unavailable family is traced on every native surface")
        val unavailable = native.read("traceComponentUsageCapability", Json.obj("reference" -> CapabilityCatalogJsonEncodingPart._usage_reference(reference)))
        Then("the original unavailability is explicit and never a successful empty collection")
        _parity(unavailable)
        _envelope(unavailable).hcursor.get[String]("status") shouldBe Right("UNAVAILABLE_PROJECTION")
        _envelope(unavailable).hcursor.downField("value").focus shouldBe Some(Json.Null)
      }
    }

    "P03 preserve Directory and normally packaged CAR semantics with original distinct acquisition provenance" in {
      withIntake { fixture =>
        Given("actual normally generated Store/Remote Directory products and explicit owning acquisition inputs")
        val ids = Vector("Store", "Remote")
        val directory = admitted(fixture.directories(ids, "First"), "surface-directory", 113)
        val reference = ComponentUsageCapabilityReference(ModelElementId("Model:能力/A", "ComponentUsage:One"), "app:v1")
        val selector = Json.obj("reference" -> CapabilityCatalogJsonEncodingPart._usage_reference(reference))
        When("normal packaging produces CARs, public CNCF acquisition admits them and each native surface reads both snapshots")
        val archives = fixture.packageCars()
        val car = admitted(fixture.selection(ids.zip(archives).map((id, archive) =>
          fixture.selected(id, "First", Some(provider(id)), Location.Car(archive)))), "surface-car", 114)
        val directoryresult = new Native(directory.catalog).read("traceComponentUsageCapability", selector)
        val carresult = new Native(car.catalog).read("traceComponentUsageCapability", selector)
        val directorytrace = _envelope(directoryresult).hcursor.downField("value").downField("result").downField("value").downArray
        val cartrace = _envelope(carresult).hcursor.downField("value").downField("result").downField("value").downArray
        Then("route parity holds within each scope and original qualified path semantics agree across acquisition locations")
        _parity(directoryresult)
        _parity(carresult)
        directorytrace.downField("usage").downField("key").focus shouldBe cartrace.downField("usage").downField("key").focus
        directorytrace.downField("usage").downField("definition").focus shouldBe cartrace.downField("usage").downField("definition").focus
        directorytrace.downField("usage").downField("input").downField("location").get[String]("kind") shouldBe Right("DIRECTORY")
        cartrace.downField("usage").downField("input").downField("location").get[String]("kind") shouldBe Right("CAR")
        directorytrace.downField("usage").downField("input").downField("location").focus should not be cartrace.downField("usage").downField("input").downField("location").focus
        directorytrace.downField("paths").downArray.downField("path").focus shouldBe cartrace.downField("paths").downArray.downField("path").focus
        val directoryprovider = directorytrace.downField("paths").downArray.downField("required").downArray.downField("providers").downField("matches").downArray.downField("provider")
        val carprovider = cartrace.downField("paths").downArray.downField("required").downArray.downField("providers").downField("matches").downArray.downField("provider")
        directoryprovider.downField("key").focus shouldBe carprovider.downField("key").focus
        directoryprovider.downField("definition").focus shouldBe carprovider.downField("definition").focus
        directoryprovider.downField("realizations").focus shouldBe carprovider.downField("realizations").focus
        _envelope(directoryresult).hcursor.downField("scope").focus should not be _envelope(carresult).hcursor.downField("scope").focus
        directory.snapshot.provenance.packages.map(_.input.location) shouldBe ids.map(id => Location.Directory(fixture.bundle(id, "First")))
        car.snapshot.provenance.packages.map(_.input.location) shouldBe archives.map(Location.Car(_))
      }
    }
    "P04 retain raw blank selector diagnostics after authorization across all seven generated API and MCP operations" in {
      withIntake { fixture =>
        Given("normal First Store/Remote products admitted once through their original metadata providers")
        val storeprovider = provider("Store")
        val remoteprovider = provider("Remote")
        val live = admitted(fixture.selection(Vector(fixture.directory("Store", "First", storeprovider),
          fixture.directory("Remote", "First", remoteprovider))), "surface-raw-blank", 115)
        val readsafteringestion = Vector(storeprovider.reads, remoteprovider.reads)
        val operations = Vector("getCapabilityOverview", "getCapability", "findProviders", "findRequirements",
          "traceComponentUsageCapability", "traceUseCaseDemand", "diagnoseCapabilityDemand")
        operations.foreach { operation =>
          Vector("", " \t\n").foreach { selector =>
            Given("an authorized native reader and a raw empty or whitespace selector for " + operation)
            val native = new Native(live.catalog)
            When("the actual generated API and MCP tools/call dispatch that raw selector string")
            val apioutcome = native.apiRaw(operation, selector)
            val mcpoutcome = native.mcpOutcomeRaw(operation, selector)
            Then("both transports retain result_json with the same typed Source(Malformed) response")
            apioutcome.isSuccess shouldBe true
            val api = apioutcome.fold(c => fail(c.display), identity)
            val mcp = parse(mcpoutcome.hcursor.downField("result").downField("content").downArray.get[String]("text").toOption.get)
              .fold(e => fail(e.message), identity)
            api.hcursor.get[String]("status") shouldBe Right("SOURCE_FAILURE")
            api.hcursor.get[String]("result_json") shouldBe mcp.hcursor.get[String]("result_json")
            api.hcursor.get[String]("status") shouldBe mcp.hcursor.get[String]("status")
            api.hcursor.get[String]("html") shouldBe mcp.hcursor.get[String]("html")
            val envelope = parse(api.hcursor.get[String]("result_json").toOption.get).fold(e => fail(e.message), identity)
            envelope.hcursor.get[String]("operation") shouldBe Right(operation)
            envelope.hcursor.get[String]("status") shouldBe Right("SOURCE_FAILURE")
            envelope.hcursor.downField("value").focus shouldBe Some(Json.Null)
            envelope.hcursor.downField("failure").get[String]("kind") shouldBe Right("SOURCE")
            envelope.hcursor.downField("failure").downField("diagnostic").get[String]("kind") shouldBe Right("Malformed")

            Given("the same admitted catalog and raw selector on a role-free native ingress")
            val untrusted = new Native(live.catalog, authenticated = false)
            When("the role-free generated API and MCP tools/call dispatch the raw selector")
            val deniedapi = untrusted.apiRaw(operation, selector)
            val deniedmcp = untrusted.mcpOutcomeRaw(operation, selector)
            Then("authorization denies both calls before malformed-selector decoding")
            deniedapi.isSuccess shouldBe false
            deniedapi.map(_ => "unexpected success").fold(_.display, identity) should include("capability.read requires a read role")
            deniedmcp.hcursor.downField("result").get[Boolean]("isError") shouldBe Right(true)
            deniedmcp.hcursor.downField("result").downField("content").downArray.get[String]("text").toOption.get should
              include("capability.read requires a read role")
          }
        }
        Then("neither malformed reads nor denials acquire the original providers again")
        readsafteringestion shouldBe Vector(1, 1)
        Vector(storeprovider.reads, remoteprovider.reads) shouldBe readsafteringestion
        fixture.cmlInputs shouldBe Vector.empty
      }
    }
  }

  private def _envelope(result: NativeRead): Json = parse(result.api.hcursor.get[String]("result_json").toOption.get).fold(e => fail(e.message), identity)
  private def _parity(result: NativeRead): Unit = {
    result.api.hcursor.get[String]("result_json") shouldBe result.mcp.hcursor.get[String]("result_json")
    result.api.hcursor.get[String]("result_json") shouldBe result.form.hcursor.get[String]("result_json")
    result.api.hcursor.get[String]("html") shouldBe result.mcp.hcursor.get[String]("html")
    result.api.hcursor.get[String]("html") shouldBe result.form.hcursor.get[String]("html")
    result.formresulthtml should include(result.api.hcursor.get[String]("html").toOption.get)
    result.formresulthtml should not include "<textus-html-field"
    _envelope(result).hcursor.get[String]("status") shouldBe result.api.hcursor.get[String]("status")
  }
}
