package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityCatalogFixtures.*
import CapabilityComponentUsageJsonPart._items

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityCatalogModelSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Context-owned Capability records" should {
    "retain qualified witnesses and declaration cardinality" which {
      "C11C01 keep Provided and Required owner keys and all independent provenance" in {
        Given("one explicit semantic resource with both directions of the same qualified Capability")
        val input = complete
        val pkg = input.packages.head
        val resource = pkg.resources.head
        When("the independently selected finite carrier binds")
        val result = catalog(input)
        Then("both exact direction keys and definition/context/source/version witnesses survive separately")
        result.capabilities.map(_.key) shouldBe resource.projection.declarations.map(declarationkey(pkg, resource, _))
        result.capabilities.map(_.declaration.direction) shouldBe Vector(Direction.Provided, Direction.Required)
        result.capabilities.map(_.definition) shouldBe Vector(definition, definition)
        result.resources.head.key shouldBe ComponentContextKey(scope.scopeId, pkg.input.component, pkg.input.packageCoordinate,
          "meta/component-capability/Payments.json", SourceKey("model:component", "models/first.cml", "header-r2"))
        result.capabilities.map(_.declaration.source.sourceRevision) shouldBe Vector("provided:first", "required:first")
        result.capabilities.head.context.resource.projection.generatedAbiVersion shouldBe "cncf.component-capability.v1"
        result.capabilities.head.context.input shouldBe pkg.input
        result.capabilities.head.definition.source shouldBe definition.source
        result.capabilities(1).declaration.realizations shouldBe Absent(AbsenceReason.NotRepresented, "No required realization representation.")
      }
      "C11C02 retain every Component witness and equal-label distinct identities" in {
        Given("two distinct Components witnessing one identity and a third equally named distinct qualified identity")
        val other = definition.copy(identity = definition.identity.copy(elementId = "other|É::cap"))
        val selectedscope = scope.copy(capabilities = Vector(definition, other))
        val packages = Vector(packageprojection("Payments", "first", selectedscope), packageprojection("Orders", "second", selectedscope),
          packageprojection("Ledger", "third", selectedscope, other))
        val input = assemble(packages, selectedscope, None)
        When("records derive in original package/resource/declaration order")
        val result = catalog(input)
        Then("all six records retain their exact contexts and equal display labels do not merge identities")
        result.resources.map(_.key.component) shouldBe packages.map(_.input.component)
        result.capabilities.size shouldBe 6
        result.capabilities.map(_.declaration.capability.identity) shouldBe Vector(definition.identity, definition.identity,
          definition.identity, definition.identity, other.identity, other.identity)
        result.capabilities.map(_.definition.name).distinct shouldBe Vector(definition.name)
        result.capabilities.map(_.key).distinct.size shouldBe 6
        result.resources.map(_.resource.resourcePath) shouldBe Vector("meta/component-capability/Payments.json", "meta/component-capability/Orders.json", "meta/component-capability/Ledger.json")
      }
      "C11C03 retain three explicit target kinds under their exact declaration owner" in {
        Given("one Provided declaration with explicit Operation, Workflow and StateMachine semantic contracts")
        val input = complete
        val facts = input.packages.head.resources.head.realizations
        When("the resource-local declaration realization references bind")
        val result = catalog(input)
        val provided = result.capabilities.head
        Then("exactly three owned edge/contract facts retain all Sources and Derived origins")
        provided.realizations.map(_.owner) shouldBe Vector.fill(3)(provided.key)
        provided.realizations.map(_.edge.targetKind) shouldBe Vector(TargetKind.Operation, TargetKind.Workflow, TargetKind.StateMachine)
        provided.realizations.map(_.edge) shouldBe facts.map(_.edge)
        provided.realizations.map(_.target) shouldBe facts.map(_.target)
        provided.realizations.map(_.edge.origin) shouldBe facts.map(_.edge.origin)
        provided.realizations.map(_.target.source.sourceRevision) shouldBe Vector.fill(3)("native:first")
        result.capabilities(1).realizations shouldBe Vector.empty
      }
    }
  }
  "Complete ComponentUsage and demand facts" should {
    "preserve every authored branch and owned path" which {
      "C11C04 retain complete documents, Presence, dependencies and External evidence" in {
        Given("a complete separately admitted three-document ComponentUsage including an empty document, Present(empty), Absent and external links")
        val input = complete
        val original = input.usage.head.family.asInstanceOf[ComponentUsageFamily.Available]
        When("the catalog binds complete ComponentUsage semantics")
        val result = catalog(input)
        Then("complete ordered documents, full definitions and external Source/Origin/location values remain unchanged")
        result.projection.usage shouldBe input.usage
        original.projection.documents.map(_.source) shouldBe usage.documents.map(_.source)
        original.projection.documents(1).componentUsageCapabilities shouldBe Vector.empty
        result.usages.map(_.definition) shouldBe usage.documents.flatMap(_.componentUsageCapabilities)
        result.usages.map(_.documentSource) shouldBe Vector(usage.documents.head.source, usage.documents(2).source)
        result.usages.head.key shouldBe ComponentUsageCapabilityContextKey(input.provenance.scope.scopeId, input.packages.head.input.component,
          input.packages.head.input.packageCoordinate, "meta/component-usage-capability/usage.json", sourcekey(usage.documents.head.source),
          ComponentUsageCapabilityReference(usage.documents.head.componentUsageCapabilities.head.identity, "app-v17"))
        result.usages.head.definition.summary shouldBe Absent(AbsenceReason.NotDeclared, "Summary undeclared.")
        result.usages(1).definition.componentMappings shouldBe Present(Vector.empty)
        _items(result.usages.head.definition.dependencies).size shouldBe 2
        _items(result.usages.head.definition.componentMappings).size shouldBe 2
        result.projection.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].unresolvedExternal shouldBe original.unresolvedExternal
        original.unresolvedExternal.map(_.target) should contain (ExternalTarget.ComponentCapability(CapabilityReference(ModelElementId("external:cap", "É::cap"), "external-v6")))
      }
      "C11C05 retain repeated mappings, every matching Required context and explicit missing endpoints" in {
        Given("two owning ComponentUsages, repeated genuine mapping edges, two Required contexts, a definition without Required and an External mapping")
        val input = multiplepaths
        When("each original mapping derives its own exact Required endpoint")
        val result = catalog(input)
        val paths = result.demandTrace.usagePaths
        val required = result.capabilities.filter(_.declaration.direction == Direction.Required).map(_.key)
        Then("only exact Local Required keys join and every missing or External endpoint stays explicit")
        paths.size shouldBe 5
        required.size shouldBe 2
        paths.map(_.owner) shouldBe Vector.fill(4)(result.usages.head.key) :+ result.usages(1).key
        paths(0).requiredEndpoint shouldBe RequiredEndpoint.Resolved(required)
        paths(1) shouldBe paths(0)
        paths(2).requiredEndpoint shouldBe RequiredEndpoint.Unresolved(CapabilityReference(ModelElementId("model:component", "no-required"), "no-required:v13"), ReferenceBoundary.Local)
        paths(3).requiredEndpoint shouldBe RequiredEndpoint.Unresolved(CapabilityReference(ModelElementId("external:cap", "É::cap"), "external-v6"), ReferenceBoundary.External)
        paths(4).requiredEndpoint shouldBe RequiredEndpoint.Resolved(required)
        paths.map(_.mapping) shouldBe result.usages.flatMap(a => _items(a.definition.componentMappings))
      }
      "C11C06 keep each provider realization attached to its own declaration" in {
        Given("two distinct Provided declarations in distinct Component contexts with different explicit target identities")
        val packages = Vector(packageprojection("Payments", "first", targetcount = 1, bothdirections = false),
          packageprojection("Orders", "second", targetcount = 1, bothdirections = false))
        val input = assemble(packages, usage = None)
        When("the catalog derives resource-local owned realization vectors")
        val result = catalog(input)
        Then("the two vectors contain exactly their own edge and target and no cross-attached combinations")
        result.capabilities.map(_.realizations.size) shouldBe Vector(1, 1)
        result.capabilities.map(c => c.realizations.head.owner) shouldBe result.capabilities.map(_.key)
        result.capabilities.map(_.realizations.head.target.identity) shouldBe Vector(ModelElementId("model:native", "first:OPERATION"), ModelElementId("model:native", "second:OPERATION"))
        result.capabilities.map(_.realizations.head.edge) shouldBe packages.map(_.resources.head.realizations.head.edge)
        result.capabilities.map(_.realizations.head.target.component) shouldBe packages.map(_.input.component.componentId)
      }
      "C11C07 preserve unavailable forward UseCase and useful authored reverse paths" in {
        Given("authored reverse Goal/UseCase links, Local Required mappings and the exact unavailable forward UseCase value")
        val input = complete
        When("the semantic demand trace is derived")
        val result = catalog(input)
        Then("forward demand stays unavailable while all original reverse links and ComponentUsage paths survive")
        result.demandTrace.useCase shouldBe usecase
        result.projection.provenance.useCase shouldBe usecase
        result.demandTrace.useCase.family shouldBe ProjectionFamily.UseCase
        result.demandTrace.useCase.reason shouldBe "not-yet-admitted"
        result.demandTrace.usage shouldBe input.usage
        _items(result.usages.head.definition.demandReferences).map(_.role) shouldBe Vector(ModelRole.Goal, ModelRole.UseCase, ModelRole.Scenario, ModelRole.UseCaseSlice)
        result.demandTrace.usagePaths.head.requiredEndpoint shouldBe RequiredEndpoint.Resolved(Vector(result.capabilities(1).key))
        result.demandTrace.usagePaths.size shouldBe 2
      }
    }
  }
  "Attributed Specification and Evidence" should {
    "remain separate from runtime observations" which {
      "C11C08 keep each role and owner while all four observations stay distinctly Unknown" in {
        Given("independent definition, declaration and ComponentUsage Specification/Evidence edges and statically resolved targets")
        val input = complete
        When("the catalog attributes original per-owner traceability and creates static observation values")
        val result = catalog(input)
        val traces = result.traceability
        val owner = result.capabilities.head
        Then("the six role-bearing edges retain exact owners/source/origin/boundary and imply no positive runtime fact")
        traces.map(_.edge.role) shouldBe Vector.fill(3)(Vector(TraceabilityRole.Specification, TraceabilityRole.Evidence)).flatten
        traces.map(_.owner) shouldBe Vector.fill(2)(TraceabilityOwner.CapabilityDefinition(result.resources.head.key, reference(definition))) ++
          Vector.fill(2)(TraceabilityOwner.Declaration(owner.key)) ++ Vector.fill(2)(TraceabilityOwner.ComponentUsage(result.usages.head.key))
        traces.map(_.edge) shouldBe _items(definition.traceability) ++ _items(owner.declaration.traceability) ++ _items(result.usages.head.definition.traceability)
        traces.map(_.edge.target.boundary) shouldBe Vector.fill(6)(ReferenceBoundary.Local)
        owner.realizations.map(_.observations.availability) shouldBe Vector.fill(3)(AvailabilityObservation.Unknown("No runtime observation supplied."))
        owner.realizations.map(_.observations.authorization) shouldBe Vector.fill(3)(AuthorizationObservation.Unknown("No runtime observation supplied."))
        owner.realizations.map(_.observations.guard) shouldBe Vector.fill(3)(GuardObservation.Unknown("No runtime observation supplied."))
        owner.realizations.map(_.observations.executionResult) shouldBe Vector.fill(3)(ExecutionResultObservation.Unknown("No runtime observation supplied."))
      }
    }
  }
}
