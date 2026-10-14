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
import CapabilityQueryModel.*
import CapabilityProducerIntegrationFixtures.*
import CapabilityProducerNativeFixtures.{operation, workflow, stateMachine, snapshots, bindings}
import CapabilityApplicationJsonPart._items

/*
 * Actual normal products -> public Factory -> original readCurrent -> queries.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityQueryProducerIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Actual normal producer Capability queries" should {
    "consume the original selected CBD Snapshot without further acquisition" which {
      "C11Q25 retain real Store/Remote Application traces and Store/Archive direct demand gaps without activation" in {
        withIntake { fixture =>
          Given("source-free normal First Store/Remote products, configured compiled accessors and full primary Application")
          val storeprovider = provider("Store")
          val remoteprovider = provider("Remote")
          val selected = fixture.selection(Vector(fixture.directory("Store", "First", storeprovider),
            fixture.directory("Remote", "First", remoteprovider)), fixture.application("primary", "First"))
          val reference = snapshot("query-real-primary-snapshot", 94)
          val componentref = CapabilityReference(ModelElementId("Model:能力/A", "Component:Store|α"), "contract:二")
          val usecase = ModelElementId("Model:能力/A", "usecase:one")
          val fullapp = fixture.decodedApplication("primary", "First")
          fixture.cmlInputs shouldBe Vector.empty
          When("original ingestion acquires via public Factory, readCurrent selects the original Snapshot and open validates its catalog readout")
          val transition = right(CapabilityIngestion.ingest(State.empty, attempt("query-real-primary-attempt", 94, selected, reference)))
          val accepted = right(CapabilityIngestion.readCurrent(transition.state,
            CurrentSelection(reference, transition.state.snapshots.head.provenance, Right(()))))
          val live = right(CapabilityCatalog.fromSnapshot(accepted, 1))
          val query = right(CapabilityQueries.open(Right(live.catalog.projection), CatalogSelection(reference, accepted.provenance)))
          val lookup = _value(query.getCapability(CapabilityLookup.Exact(componentref)))
          val discovered = _value(query.getCapability(CapabilityLookup.Search("STORE CAPABILITY")))
          val providers = _value(query.findProviders(componentref))
          val requirements = _value(query.findRequirements(coordinate("Store")))
          Then("the original accepted Snapshot, accessor semantics and exact scoped query values survive with each provider read once")
          transition.outcome shouldBe Outcome.Accepted(reference)
          accepted should be theSameInstanceAs transition.state.snapshots.head
          live.snapshot should be theSameInstanceAs accepted
          query.catalog.projection.snapshotReference shouldBe reference
          query.scope shouldBe QueryScope(reference, accepted.provenance)
          query.catalog.resources.map(_.resource.projection) shouldBe Vector(accessor("Store", "First"), accessor("Remote", "First"))
          query.catalog.projection.application shouldBe accepted.application
          available(accepted.application).projection shouldBe fullapp
          lookup.designation shouldBe LookupDesignation.ExactIdentity
          lookup.records.map(_.declaration.direction) shouldBe Vector(Direction.Provided, Direction.Required)
          lookup.records.map(_.declaration.capability) shouldBe Vector.fill(2)(componentref)
          discovered.designation shouldBe LookupDesignation.DiscoveryOnly
          discovered.records shouldBe lookup.records
          providers.matches.map(_.provider) shouldBe Vector(lookup.records.head)
          providers.matches.map(_.basis) shouldBe Vector(MatchBasis.SameVersion)
          providers.incompatible shouldBe Vector.empty
          providers.consideredCompatibility.map(_.edge) shouldBe _items(accessor("Store", "First").compatibility)
          requirements.records shouldBe Vector(lookup.records(1))
          providers.matches.head.provider.realizations.map(_.edge.targetKind) shouldBe TargetKind.values.toVector
          providers.matches.head.provider.realizations.map(_.target) shouldBe bindings.map(_.target)
          providers.matches.head.provider.realizations.map(_.owner) shouldBe Vector.fill(3)(lookup.records.head.key)
          Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
          Vector("Application:One", "Application:Two").foreach { element =>
            Given("the original full primary Application " + element + " and its Local plus External mappings")
            val appref = ApplicationCapabilityReference(ModelElementId("Model:能力/A", element), "app:v1")
            val original = fullapp.documents.head.applicationCapabilities.find(_.identity == appref.identity).get
            When("the exact original Application reference is traced and diagnosed")
            val traced = _value(query.traceApplicationCapability(appref))
            val diagnosed = _value(query.diagnoseCapabilityDemand(DemandSelection.Application(appref)))
            Then("its full definition, exact Required/provider branch and External evidence remain original with no role gap")
            traced.size shouldBe 1
            traced.head.application.definition shouldBe original
            traced.head.paths.map(_.path.mapping) shouldBe _items(original.componentMappings)
            traced.head.paths.map(_.required.size) shouldBe Vector(1, 0)
            traced.head.paths.head.required.head.required shouldBe lookup.records(1)
            traced.head.paths.head.required.head.providers shouldBe providers
            traced.head.paths(1).path.requiredEndpoint shouldBe RequiredEndpoint.Unresolved(
              CapabilityReference(ModelElementId("Other:能力/B", "Component:Remote"), "contract:二"), ReferenceBoundary.External)
            traced.head.paths(1).path.mapping.origin shouldBe _items(original.componentMappings)(1).origin
            diagnosed.applications shouldBe traced
            diagnosed.required shouldBe Vector.empty
            diagnosed.gaps shouldBe Vector.empty
          }
          Given("the original unavailable UseCase projection despite actual reverse UseCase links")
          When("forward UseCase trace and diagnosis are requested from the real admitted session")
          val forward = query.traceUseCaseDemand(usecase)
          val forwarddiagnosis = query.diagnoseCapabilityDemand(DemandSelection.UseCase(usecase))
          Then("both preserve the original not-yet-admitted value without forward facts")
          forward shouldBe Left(QueryFailure.UnavailableProjection(accepted.useCase))
          forwarddiagnosis shouldBe Left(QueryFailure.UnavailableProjection(accepted.useCase))
          _items(query.catalog.applications(1).definition.demandReferences).map(_.target.identity) should contain (usecase)
          Given("the original native metadata contains poison procedures, guards and actions")
          When("the live native observations and semantic Unknown values are read without invocation")
          val native = live.nativeObservations.map(_.resolved)
          val observations = providers.matches.head.provider.realizations.map(_.observations)
          Then("native originals remain the same instances while all four runtime dimensions stay Unknown")
          native.zip(snapshots.zip(bindings)).foreach { (observed, originals) =>
            Given("one original native snapshot and binding for " + observed.target.targetKind.tag)
            When("the selected live observation is read without activation")
            val selectedsnapshot = observed.snapshot
            val selectedbinding = observed.binding
            Then("both objects remain the original admitted instances")
            selectedsnapshot should be theSameInstanceAs originals._1
            selectedbinding should be theSameInstanceAs originals._2
          }
          native(0).contract match {
            case NativeContract.Operation(value) =>
              value should be theSameInstanceAs operation
              value.implementation shouldBe Some("must.never.execute.PoisonSaveOperation")
              value.jobDefinitionRef shouldBe Some("PoisonJobDefinition")
            case _ => fail("The original native Operation metadata is required.")
          }
          native(1).contract match {
            case NativeContract.Workflow(value) => value should be theSameInstanceAs workflow
            case _ => fail("The original native Workflow metadata is required.")
          }
          native(2).contract match {
            case NativeContract.StateMachine(value) => value should be theSameInstanceAs stateMachine
            case _ => fail("The original native StateMachine metadata is required.")
          }
          observations.map(_.availability) shouldBe Vector.fill(3)(AvailabilityObservation.Unknown("No runtime observation supplied."))
          observations.map(_.authorization) shouldBe Vector.fill(3)(AuthorizationObservation.Unknown("No runtime observation supplied."))
          observations.map(_.guard) shouldBe Vector.fill(3)(GuardObservation.Unknown("No runtime observation supplied."))
          observations.map(_.executionResult) shouldBe Vector.fill(3)(ExecutionResultObservation.Unknown("No runtime observation supplied."))
          Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)

          Given("separate real First Store/Archive/Remote packages with explicitly unavailable Application input")
          val combinedproviders = Vector("Store", "Archive", "Remote").map(provider)
          val combinedselection = fixture.selection(Vector("Store", "Archive", "Remote").zip(combinedproviders)
            .map((id, p) => fixture.directory(id, "First", p)), applicationUnavailable)
          val combinedreference = snapshot("query-real-combined-snapshot", 95)
          When("original ingestion accepts this explicit selection, readCurrent selects its Snapshot and open consumes only its original catalog readout")
          val combinedtransition = right(CapabilityIngestion.ingest(State.empty,
            attempt("query-real-combined-attempt", 95, combinedselection, combinedreference)))
          val combinedsnapshot = right(CapabilityIngestion.readCurrent(combinedtransition.state,
            CurrentSelection(combinedreference, combinedtransition.state.snapshots.head.provenance, Right(()))))
          val combinedlive = right(CapabilityCatalog.fromSnapshot(combinedsnapshot, 1))
          val combined = right(CapabilityQueries.open(Right(combinedlive.catalog.projection), CatalogSelection(combinedreference, combinedsnapshot.provenance)))
          val storerequired = _value(combined.findRequirements(coordinate("Store"))).records.head
          val direct = _value(combined.diagnoseCapabilityDemand(DemandSelection.Required(storerequired.key)))
          val matches = direct.required.head.providers.matches
          val archive = matches(1)
          Then("direct Store Required diagnosis retains both providers and only Archive's original Present(empty) MissingRealization")
          combinedtransition.outcome shouldBe Outcome.Accepted(combinedreference)
          combinedsnapshot should be theSameInstanceAs combinedtransition.state.snapshots.head
          combinedlive.snapshot should be theSameInstanceAs combinedsnapshot
          combined.catalog.resources.map(_.resource.projection) shouldBe Vector("Store", "Archive", "Remote").map(accessor(_, "First"))
          matches.map(_.provider.key.context.component) shouldBe Vector(coordinate("Store"), coordinate("Archive"))
          matches.map(_.basis) shouldBe Vector.fill(2)(MatchBasis.SameVersion)
          matches.map(_.provider.realizations.size) shouldBe Vector(3, 0)
          matches.head.provider.realizations.map(_.target) shouldBe bindings.map(_.target)
          matches.head.provider.realizations.map(_.owner) shouldBe Vector.fill(3)(matches.head.provider.key)
          archive.provider.declaration.realizations shouldBe Present(Vector.empty)
          direct.applications shouldBe Vector.empty
          direct.required.map(_.required) shouldBe Vector(storerequired)
          direct.gaps shouldBe Vector(CapabilityGap.MissingRealization(combined.scope,
            DemandPath(None, None, storerequired, Some(archive)), Present(Vector.empty)))
          Given("the combined original Application family is explicitly unavailable")
          val appref = ApplicationCapabilityReference(ModelElementId("Model:能力/A", "Application:One"), "app:v1")
          val family = combinedsnapshot.application.asInstanceOf[ApplicationFamily.Unavailable]
          When("the Application is traced and diagnosed without borrowing the primary intake")
          val missingtrace = combined.traceApplicationCapability(appref)
          val missingdiagnosis = _value(combined.diagnoseCapabilityDemand(DemandSelection.Application(appref)))
          Then("trace remains original unavailable and diagnosis retains only the known missing Application boundary")
          missingtrace shouldBe Left(QueryFailure.UnavailableProjection(family.value))
          missingdiagnosis shouldBe DemandDiagnosis(DemandSelection.Application(appref), Vector.empty, Vector.empty,
            Vector(CapabilityGap.MissingApplicationCapabilityProjection(combined.scope, appref, MissingApplicationReason.ProjectionNotSupplied, family)))
          combinedproviders.map(_.reads) shouldBe Vector(1, 1, 1)
          Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
          fixture.cmlInputs shouldBe Vector.empty
        }
      }
    }
  }
  private def _value[A](result: Either[QueryFailure, QueryResult[A]]): A =
    result.fold(error => throw new IllegalArgumentException(error.toString), _.value)
}
