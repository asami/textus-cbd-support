package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.cncf.config.{ComponentConfigurationAccess, ComponentConfigurationSources}
import org.goldenport.cncf.context.{ExecutionContext, Capability}
import org.goldenport.configuration.{Configuration, ConfigurationValue}
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.ModelElementId
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import CapabilityCatalogModel.*
import CapabilityQueryFixtures.*
import CapabilityIngestionFixtures.right

/*
 * Configured public reads retain original query semantics and authority.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityReadApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Configured public Capability reads" should {
    "S01 distinguish exact identity, discovery-only and successful finite empty with original scope" in {
      Given("a valid original finite query projection and independently selected server inputs")
      val input = scenario()
      val access = _access(input)
      When("exact, literal discovery and absent exact references pass through the public bridge")
      val exact = _read("getCapability", _exact(reference), access)
      val discovery = _read("getCapability", Json.obj("kind" -> Json.fromString("SEARCH"), "text" -> Json.fromString("same CAPABILITY")), access)
      val empty = _read("getCapability", _exact(reference.copy(contractVersion = "absent:opaque")), access)
      val overview = _read("getCapabilityOverview", Json.obj(), access)
      Then("designation, original records, complete provenance and explicit successful empty remain distinct")
      _json(exact).hcursor.downField("value").get[String]("designation") shouldBe Right("EXACT_IDENTITY")
      _json(discovery).hcursor.downField("value").get[String]("designation") shouldBe Right("DISCOVERY_ONLY")
      _records(exact).size shouldBe 2
      _records(discovery) shouldBe _records(exact)
      _records(empty) shouldBe Vector.empty
      empty.status shouldBe "OK"
      empty.html should include("Successful empty collection")
      _json(exact).hcursor.downField("scope").focus shouldBe Some(CapabilityCatalogJsonEncodingPart.encodeSelection(selection(input)))
      _json(overview).hcursor.downField("value").downField("useCase").get[String]("reason") shouldBe Right(input.useCase.reason)
    }

    "S02 preserve every provider, explicit compatibility witness and provider-owned realization observation" in {
      Given("two compatible providers with three and one realizations and an incompatible candidate")
      val directed = reference.copy(contractVersion = "directed:v9")
      val wrong = reference.copy(contractVersion = "wrong:v8")
      val witness = compatibility(reference, directed.contractVersion, "witness:v3")
      val input = scenario(Vector(ProviderInput(reference, 3), ProviderInput(directed, 1), ProviderInput(wrong)), witnesses = Vector(witness))
      When("provider search, requirements and nested Usage are read through configured public operations")
      val providers = _read("findProviders", _reference(reference), _access(input))
      val matches = _json(providers).hcursor.downField("value").get[Vector[Json]]("matches").toOption.get
      val requiredcomponent = input.packages.head.input.component
      val requirements = _read("findRequirements", Json.obj("component" -> CapabilityComponentUsageJsonEncodingPart._component_coordinate(requiredcomponent)), _access(input))
      val usage = _read("traceComponentUsageCapability", _usage_reference, _access(input))
      Then("all candidates and full per-owner edges, targets, source/origin and four Unknown dimensions survive")
      _json(providers).hcursor.downField("value").get[Vector[Json]]("candidates").toOption.get.size shouldBe 3
      matches.map(_.hcursor.downField("basis").get[String]("kind").toOption.get) shouldBe Vector("SAME_VERSION", "EXPLICIT_COMPATIBILITY")
      matches(1).hcursor.downField("basis").get[Vector[Json]]("records").toOption.get.head.hcursor.downField("edge").get[String]("requiredVersion") shouldBe Right(reference.contractVersion)
      matches.map(_.hcursor.downField("provider").get[Vector[Json]]("realizations").toOption.get.size) shouldBe Vector(3, 1)
      matches.foreach { matched =>
        val provider = matched.hcursor.downField("provider")
        provider.get[Vector[Json]]("realizations").toOption.get.foreach { realization =>
          realization.hcursor.downField("owner").focus shouldBe provider.downField("key").focus
          realization.hcursor.downField("edge").downField("origin").focus should not be empty
          realization.hcursor.downField("target").downField("source").focus should not be empty
          val observations = realization.hcursor.downField("observations")
          Vector("availability", "authorization", "guard", "executionResult").foreach { dimension =>
            observations.downField(dimension).get[String]("kind") shouldBe Right("UNKNOWN")
            observations.downField(dimension).get[String]("reason") shouldBe Right("No runtime observation supplied.")
          }
        }
      }
      _records(requirements).size shouldBe 1
      _records(requirements).head.hcursor.downField("declaration").get[String]("direction") shouldBe Right("REQUIRED")
      val nested = _json(usage).hcursor.downField("value").downField("result").get[Vector[Json]]("value").toOption.get
      val branch = nested.head.hcursor.downField("paths").downArray.downField("required").downArray
      branch.downField("providers").get[Vector[Json]]("matches") shouldBe Right(matches)
      branch.downField("required").focus shouldBe Some(_records(requirements).head)
    }

    "S03 preserve seven distinct static gaps with their owning evidence and keep runtime Unknown separate" in {
      Given("finite accepted scenarios for each static missing boundary")
      val orphan = reference.copy(identity = reference.identity.copy(elementId = "orphan:required"))
      val cases = Vector(
        scenario(usageAvailable = false) -> Set("MISSING_COMPONENT_USAGE_CAPABILITY_PROJECTION"),
        scenario(mappings = Vector(mapping(orphan)), extraDefinitions = Vector(orphan)) -> Set("MISSING_REQUIRED_COMPONENT_CAPABILITY"),
        scenario(Vector.empty, requiredroles = Vector.empty, usageroles = Vector.empty) -> Set("NO_PROVIDER", "MISSING_SPECIFICATION", "MISSING_EVIDENCE"),
        scenario(Vector(ProviderInput(reference.copy(contractVersion = "wrong:v2")))) -> Set("INCOMPATIBLE_PROVIDER_VERSION"),
        scenario(Vector(ProviderInput(reference, 0))) -> Set("MISSING_REALIZATION"))
      When("each actual Usage demand is diagnosed by the public operation")
      val outcomes = cases.map { (input, expected) =>
        Given("one admitted finite scenario and its expected static boundary tags")
        val access = _access(input)
        When("the Usage demand is diagnosed")
        val response = _read("diagnoseCapabilityDemand", _usage_demand, access)
        val gaps = _json(response).hcursor.downField("value").get[Vector[Json]]("gaps").toOption.get
        Then("the exact original gap tags and scope remain encoded")
        response.status shouldBe "OK"
        gaps.map(_.hcursor.get[String]("kind").toOption.get).toSet shouldBe expected
        gaps.foreach { gap =>
          gap.hcursor.downField("scope").focus shouldBe Some(CapabilityCatalogJsonEncodingPart.encodeSelection(selection(input)))
          gap.asObject.get.keys.toSet should contain allOf ("kind", "scope")
        }
        gaps
      }
      Then("all seven gap variants remain typed with variant-specific owner/path/family/search/coverage")
      outcomes.flatten.map(_.hcursor.get[String]("kind").toOption.get).toSet.size shouldBe 7
      outcomes(0).head.hcursor.get[Vector[Json]]("families").toOption.get should not be empty
      outcomes(1).head.hcursor.downField("usage").downField("definition").focus should not be empty
      outcomes(2).head.hcursor.downField("path").downField("required").focus should not be empty
      outcomes(3).head.hcursor.downField("search").get[Vector[Json]]("candidates").toOption.get.size shouldBe 1
      outcomes(4).head.hcursor.downField("original").get[Vector[Json]]("value") shouldBe Right(Vector.empty)
      outcomes(2).last.hcursor.downField("coverage").get[Vector[Json]]("owners").toOption.get should not be empty
    }

    "S04 keep missing source, malformed, stale, ambiguous and unavailable family separate" in {
      Given("missing server inputs, a malformed selection, a separately stale selection and duplicate target facts")
      val input = scenario()
      val stale = selection(input).copy(provenance = input.provenance.copy(scope = input.provenance.scope.copy(source = input.provenance.scope.source.copy(sourceRevision = "different:v2"))))
      val pkg = input.packages(1)
      val ambiguous = input.copy(packages = input.packages.updated(1, pkg.copy(targetContracts = pkg.targetContracts :+ pkg.targetContracts.head)))
      val emptyaccess = ComponentConfigurationAccess(ComponentConfigurationSources())
      When("the original codecs and query admission receive those inputs through the bridge")
      val missing = _read("getCapabilityOverview", Json.obj(), emptyaccess)
      val malformed = _read("getCapabilityOverview", Json.obj(), _raw(_projection(input), "{}"))
      val changed = _read("getCapabilityOverview", Json.obj(), _raw(_projection(input), CapabilityCatalogJsonEncodingPart.encodeSelection(stale).noSpaces))
      val duplicate = _read("getCapabilityOverview", Json.obj(), _raw(right(CapabilityCatalogJsonEncodingPart.encode(ambiguous)).noSpaces, CapabilityCatalogJsonEncodingPart.encodeSelection(selection(ambiguous)).noSpaces))
      val usecase = _read("traceUseCaseDemand", Json.obj("reference" -> Json.obj("modelId" -> Json.fromString("model:facts"), "elementId" -> Json.fromString("usecase"))), _access(input))
      val unavailable = _read("traceComponentUsageCapability", _usage_reference, _access(scenario(usageAvailable = false)))
      Then("original diagnostics never become empty and forward UseCase keeps original unavailability and session scope")
      Vector(missing, malformed, changed, duplicate).map(_diagnostic_kind) shouldBe Vector("Unavailable", "Malformed", "Stale", "Ambiguous")
      Vector(missing, malformed, changed, duplicate).map(_.status).distinct shouldBe Vector("SOURCE_FAILURE")
      _json(missing).hcursor.downField("scope").focus shouldBe Some(Json.Null)
      _json(changed).hcursor.downField("failure").downField("diagnostic").downField("source").focus shouldBe Some(CapabilityComponentUsageJsonEncodingPart._source(input.provenance.scope.source))
      usecase.status shouldBe "UNAVAILABLE_PROJECTION"
      unavailable.status shouldBe "UNAVAILABLE_PROJECTION"
      _json(usecase).hcursor.downField("scope").focus shouldBe Some(CapabilityCatalogJsonEncodingPart.encodeSelection(selection(input)))
      _json(usecase).hcursor.downField("failure").downField("value").get[String]("detail") shouldBe Right(input.useCase.detail)
      _json(usecase).hcursor.downField("value").focus shouldBe Some(Json.Null)
    }

    "S05 reject unknown and duplicate selector fields without defaulting qualifiers" in {
      Given("configured input and closed operation selectors including attempted caller authority/configuration")
      val access = _access(scenario())
      val cases = Vector("{\"roles\":[\"admin\"]}", "{\"projection\":{}}", "{\"selection\":{}}", "{\"x\":1,\"x\":2}", "[]")
      When("overview selectors and incomplete exact references reach the public bridge")
      val invalid = cases.map(s => CapabilityReadApplication.read("getCapabilityOverview", s, access)(using _context("viewer")).toOption.get)
      val incomplete = _read("findRequirements", Json.obj("component" -> Json.obj("namespace" -> Json.fromString("org.example"), "id" -> Json.fromString("Store"))), access)
      Then("every malformed request remains Source(Malformed), with no successful empty or inferred version")
      invalid.map(_diagnostic_kind).distinct shouldBe Vector("Malformed")
      _diagnostic_kind(incomplete) shouldBe "Malformed"
      CapabilityReadApplication.read("refreshCapability", "{}", access)(using _context("viewer")).isSuccess shouldBe false
    }

    "S06 authorize actual context roles before malformed configuration or selector and escape the same typed view" in {
      Given("a malformed source/selector and contexts with no read role or an actual viewer role")
      val invalid = _raw("broken", "broken")
      val input = scenario()
      val definition = input.provenance.scope.capabilities.head
      val escapeddefinition = definition.copy(name = "<script>attack & \"quote\"</script>")
      val escapedinput = assemble(input.packages.map(p => p.copy(resources = p.resources.map(r => r.copy(resolvedDefinitions = r.resolvedDefinitions.map(_ => escapeddefinition))))),
        input.provenance.scope.copy(capabilities = Vector(escapeddefinition)), Some(input.usage.head.family.asInstanceOf[CapabilityIngestionModel.ComponentUsageFamily.Available].projection))
      When("authorization precedes decoding and the viewer renders a valid original escaped name")
      val denied = CapabilityReadApplication.read("getCapabilityOverview", "broken", invalid)(using _context("unknown-caller-role"))
      val allowed = _read("getCapability", _exact(reference), _access(escapedinput))
      Then("denial is structured permission failure while authorized JSON retains text and HTML escapes it")
      denied.isSuccess shouldBe false
      denied.map(_ => "unexpected success").fold(_.display, identity) should include("capability.read requires a read role")
      _records(allowed).head.hcursor.downField("definition").get[String]("name") shouldBe Right(escapeddefinition.name)
      allowed.html should include("&lt;script&gt;attack &amp; &quot;quote&quot;&lt;/script&gt;")
      allowed.html should not include "<script>"
    }
  }

  private def _context(role: String): ExecutionContext = {
    val base = ExecutionContext.create()
    ExecutionContext.withSecurityContext(base, base.security.copy(capabilities = Set(Capability(role))))
  }
  private def _raw(projection: String, selection: String): ComponentConfigurationAccess = ComponentConfigurationAccess(
    ComponentConfigurationSources(component = Configuration(Map(
      "textus.cbd.capability.projection" -> ConfigurationValue.StringValue(projection),
      "textus.cbd.capability.selection" -> ConfigurationValue.StringValue(selection)))))
  private def _projection(input: PortableProjection): String = right(CapabilityCatalogProjectionCodec.encode(input, selection(input)))
  private def _access(input: PortableProjection): ComponentConfigurationAccess = _raw(_projection(input), CapabilityCatalogJsonEncodingPart.encodeSelection(selection(input)).noSpaces)
  private def _read(operation: String, selector: Json, access: ComponentConfigurationAccess): CapabilityReadApplication.Response =
    CapabilityReadApplication.read(operation, selector.noSpaces, access)(using _context("viewer")).fold(c => fail(c.display), identity)
  private def _json(response: CapabilityReadApplication.Response): Json = parse(response.resultJson).fold(e => fail(e.message), identity)
  private def _records(response: CapabilityReadApplication.Response): Vector[Json] = _json(response).hcursor.downField("value").get[Vector[Json]]("records").toOption.get
  private def _diagnostic_kind(response: CapabilityReadApplication.Response): String = _json(response).hcursor.downField("failure").downField("diagnostic").get[String]("kind").toOption.get
  private def _reference(ref: CapabilityReference): Json = Json.obj("reference" -> CapabilityCatalogJsonEncodingPart._capability_reference(ref))
  private def _exact(ref: CapabilityReference): Json = _reference(ref).mapObject(_.add("kind", Json.fromString("EXACT")))
  private def _usage_reference: Json = Json.obj("reference" -> CapabilityCatalogJsonEncodingPart._usage_reference(usageReference))
  private def _usage_demand: Json = _usage_reference.mapObject(_.add("kind", Json.fromString("COMPONENT_USAGE")))
}
