package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityMetadataProvider.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityProducerIntegrationFixtures.*
import CapabilityProducerNativeFixtures.{operation, workflow, stateMachine, snapshots, bindings}
import CapabilityComponentUsageJsonPart._items

/*
 * Actual configured normal producer -> public Factory ingestion -> catalog.
 * No pure synthetic carrier can substitute for this accepted Snapshot behavior.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityCatalogProducerIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Actual accepted producer Capability catalog" should {
    "retain original native observations in the live wrapper" which {
      "C11C15 preserve accepted Store/Remote semantics, complete ComponentUsage paths and exact native instances without rereading or execution" in {
        withIntake { fixture =>
          Given("source-free real normal First Store/Remote products, configured compiled accessors, independent original native inputs and owned Store, UsageOnly and Remote ComponentUsage")
          val storeprovider = provider("Store")
          val remoteprovider = provider("Remote")
          val selected = fixture.selection(Vector(fixture.directory("Store", "First", storeprovider),
            fixture.directory("Remote", "First", remoteprovider),
            fixture.selected("UsageOnly", "First", None, ComponentCapabilityPackageSource.Location.Directory(fixture.bundle("UsageOnly", "First")))))
          val reference = snapshot("producer-catalog-first-snapshot", 93)
          val compiled = Vector(accessor("Store", "First"), accessor("Remote", "First"))
          val storeusage = fixture.usageProjection("Store", "First")
          val independentusage = fixture.usageProjection("UsageOnly", "First")
          val remoteusage = fixture.usageProjection("Remote", "First")
          fixture.cmlInputs shouldBe Vector.empty
          When("original ingestion uses public Factory and readCurrent selects its accepted Snapshot, then catalog capture and semantic roundtrip consume only that value")
          val transition = right(CapabilityIngestion.ingest(State.empty, attempt("producer-catalog-first-attempt", 93, selected, reference)))
          val accepted = right(CapabilityIngestion.readCurrent(transition.state, CurrentSelection(reference, transition.state.snapshots.head.provenance, Right(()))))
          val live = right(CapabilityCatalog.fromSnapshot(accepted, 1))
          val context = CatalogSelection(reference, accepted.provenance)
          val encoded = right(CapabilityCatalogProjectionCodec.encode(live.catalog.projection, context))
          val portable = right(CapabilityCatalogProjectionCodec.decode(encoded, context))
          Then("the original accepted value and all public projection/accessor semantics survive without another metadata read")
          transition.outcome shouldBe Outcome.Accepted(reference)
          live.snapshot should be theSameInstanceAs accepted
          accepted should be theSameInstanceAs transition.state.snapshots.head
          live.snapshot.components should be theSameInstanceAs accepted.components
          Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
          live.snapshot.components.head.nativeInput should be theSameInstanceAs storeprovider.input
          live.snapshot.components(1).nativeInput should be theSameInstanceAs remoteprovider.input
          live.catalog.projection.provenance shouldBe accepted.provenance
          live.catalog.resources.map(_.resource.projection) shouldBe compiled
          live.catalog.resources.map(_.input.component) shouldBe Vector(coordinate("Store"), coordinate("Remote"))
          live.catalog.resources.map(_.resource.projection.source) shouldBe Vector(header("Store", "First"), header("Remote", "First"))
          live.catalog.resources.map(_.resource.projection.producer) shouldBe Vector.fill(2)(Producer("cozy", fixture.products.cozyVersion))
          live.catalog.resources.map(_.resource.projection.packageCoordinate) shouldBe Vector(packageCoordinate("Store"), packageCoordinate("Remote"))
          live.catalog.resources.map(_.resource.resolvedDefinitions) shouldBe accepted.components.flatMap(_.entries.map(_.admitted.capabilities))
          val store = live.catalog.capabilities.filter(_.context.key.component == coordinate("Store"))
          store.map(_.declaration.direction) shouldBe Vector(Direction.Provided, Direction.Required)
          store.map(_.declaration.capability) shouldBe Vector.fill(2)(CapabilityReference(ModelElementId("Model:能力/A", "Component:Store|α"), "contract:二"))
          store.head.realizations.map(_.owner) shouldBe Vector.fill(3)(store.head.key)
          store.head.realizations.map(_.edge.targetKind) shouldBe Vector(TargetKind.Operation, TargetKind.Workflow, TargetKind.StateMachine)
          store.head.realizations.map(_.target) shouldBe bindings.map(_.target)
          store(1).declaration.realizations shouldBe Absent(AbsenceReason.NotRepresented, "No required realization representation.")
          store(1).realizations shouldBe Vector.empty
          val originaltargets = accepted.components.head.entries.head.realizations
          live.nativeObservations.size shouldBe 3
          live.nativeObservations.map(_.owner) shouldBe Vector.fill(3)(store.head.key)
          live.nativeObservations.zip(originaltargets).zipWithIndex.foreach { case ((observation, original), i) =>
            Given("the original admitted native target, snapshot, binding and procedure for kind " + original.target.targetKind.tag)
            When("the live catalog's attributed observation is read without activation")
            val observed = observation.resolved
            Then("each native/public object is the original instance rather than a reconstructed alias")
            observed should be theSameInstanceAs original
            observed.realization should be theSameInstanceAs original.realization
            observed.target should be theSameInstanceAs original.target
            observed.contract should be theSameInstanceAs original.contract
            observed.snapshot should be theSameInstanceAs snapshots(i)
            observed.binding should be theSameInstanceAs bindings(i)
          }
          originaltargets(0).contract match {
            case NativeContract.Operation(value) =>
              value should be theSameInstanceAs operation
              value.implementation shouldBe Some("must.never.execute.PoisonSaveOperation")
              value.jobDefinitionRef shouldBe Some("PoisonJobDefinition")
            case _ => fail("The original Operation contract is required.")
          }
          originaltargets(1).contract match {
            case NativeContract.Workflow(value) =>
              value should be theSameInstanceAs workflow
              value.registrations should be theSameInstanceAs workflow.registrations
            case _ => fail("The original Workflow contract is required.")
          }
          originaltargets(2).contract match {
            case NativeContract.StateMachine(value) =>
              value should be theSameInstanceAs stateMachine
              value.normalized.get should be theSameInstanceAs stateMachine.normalized.get
              value.normalized.get.transitions.head should be theSameInstanceAs stateMachine.normalized.get.transitions.head
            case _ => fail("The original StateMachine contract is required.")
          }
          val usage = available(accepted.usage, "Store")
          portable shouldBe live.catalog
          portable.projection.usage shouldBe accepted.usage
          usage.projection shouldBe storeusage
          accepted.componentPackages.last shouldBe a[ComponentPackage.MetadataOnly]
          accepted.provenance.packages.last.resources shouldBe Vector.empty
          selected.packages.last.provider shouldBe None
          portable.projection.packages.last.resources shouldBe Vector.empty
          portable.projection.packages.last.targetContracts shouldBe Vector.empty
          portable.usages.map(_.definition) shouldBe Vector(storeusage, remoteusage, independentusage).flatMap(_.documents.flatMap(_.componentUsageCapabilities))
          portable.usages.map(_.key.component) shouldBe Vector(coordinate("Store"), coordinate("Store"), coordinate("Remote"), coordinate("UsageOnly"))
          portable.usages.map(_.key.packageCoordinate) shouldBe Vector(packageCoordinate("Store"), packageCoordinate("Store"), packageCoordinate("Remote"), packageCoordinate("UsageOnly"))
          portable.usages.map(_.key.resourcePath).distinct shouldBe Vector("meta/component-usage-capability/usage.json")
          portable.usages.map(_.input) shouldBe Vector(selected.packages(0).expected.input, selected.packages(0).expected.input,
            selected.packages(1).expected.input, selected.packages(2).expected.input)
          portable.usages.map(_.key.document) shouldBe Vector.fill(2)(SourceKey("Model:能力/A", "model/capability-model.cml", "header:primary-r42")) ++
            Vector(SourceKey("Other:能力/B", "model/capability-model-secondary.cml", "header:secondary-r43"),
              SourceKey("Model:能力/A", "model/capability-model.cml", "header:primary-r42"))
          portable.usages.map(_.definition.identity) shouldBe Vector("ComponentUsage:One", "ComponentUsage:Two").map(ModelElementId("Model:能力/A", _)) ++
            Vector(ModelElementId("Other:能力/B", "ComponentUsage:Remote"), ModelElementId("Model:能力/A", "ComponentUsage:Independent"))
          portable.usages.last.definition.componentMappings shouldBe Present(Vector.empty)
          portable.usages.last.definition.dependencies shouldBe Absent(AbsenceReason.NotDeclared, "No dependency declared.")
          portable.usages.take(2).map(_.definition.source.sourceRevision) shouldBe Vector.fill(2)("definition-r1")
          val paths = portable.demandTrace.usagePaths
          paths.size shouldBe 4
          paths.map(_.owner) shouldBe Vector.fill(2)(portable.usages(0).key) ++ Vector.fill(2)(portable.usages(1).key)
          paths.map(_.requiredEndpoint) shouldBe Vector(RequiredEndpoint.Resolved(Vector(store(1).key)),
            RequiredEndpoint.Unresolved(CapabilityReference(ModelElementId("Other:能力/B", "Component:Remote"), "contract:二"), ReferenceBoundary.External),
            RequiredEndpoint.Resolved(Vector(store(1).key)),
            RequiredEndpoint.Unresolved(CapabilityReference(ModelElementId("Other:能力/B", "Component:Remote"), "contract:二"), ReferenceBoundary.External))
          paths.map(_.mapping) shouldBe storeusage.documents.flatMap(_.componentUsageCapabilities.flatMap(a => _items(a.componentMappings)))
          available(portable.projection.usage, "Store").unresolvedExternal shouldBe usage.unresolvedExternal
          usage.unresolvedExternal.map(_.target) should contain allOf (
            ExternalTarget.ComponentCapability(CapabilityReference(ModelElementId("Other:能力/B", "Component:Remote"), "contract:二")),
            ExternalTarget.ComponentUsageCapability(ComponentUsageCapabilityReference(ModelElementId("Other:能力/B", "ComponentUsage:Remote"), "app:v1")),
            ExternalTarget.Term(TermId("BoK:語彙", "term:注文|α")))
          portable.projection.useCase shouldBe accepted.useCase
          portable.demandTrace.useCase shouldBe useCase
          portable.demandTrace.useCase.reason shouldBe "not-yet-admitted"
          store.head.realizations.map(_.observations.availability) shouldBe Vector.fill(3)(AvailabilityObservation.Unknown("No runtime observation supplied."))
          store.head.realizations.map(_.observations.authorization) shouldBe Vector.fill(3)(AuthorizationObservation.Unknown("No runtime observation supplied."))
          store.head.realizations.map(_.observations.guard) shouldBe Vector.fill(3)(GuardObservation.Unknown("No runtime observation supplied."))
          store.head.realizations.map(_.observations.executionResult) shouldBe Vector.fill(3)(ExecutionResultObservation.Unknown("No runtime observation supplied."))
          encoded should not include "PoisonSaveOperation"
          encoded should not include "PoisonJobDefinition"
          encoded should not include "PoisonDeclaredGuard"
          encoded should not include "PoisonFinishAction"
          encoded should not include "implementation"
          encoded should not include "nativeObservations"
          encoded should not include "selector"
          Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
          fixture.cmlInputs shouldBe Vector.empty
        }
      }
    }
  }
}
