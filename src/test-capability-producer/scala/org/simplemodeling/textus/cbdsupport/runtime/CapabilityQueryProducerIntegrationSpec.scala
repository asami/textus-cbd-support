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
import CapabilityComponentUsageJsonPart._items

/*
 * Actual normal products -> public Factory -> original readCurrent -> queries.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityQueryProducerIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Actual normal producer Capability queries" should {
    "consume the original selected CBD Snapshot without further acquisition" which {
      "C11Q25 retain real Store/Remote ComponentUsage traces and Store/Archive direct demand gaps without activation" in {
        withIntake { fixture =>
          Given("source-free normal First Store/Remote products, configured compiled accessors and owned Store, UsageOnly and Remote ComponentUsage")
          val storeprovider = provider("Store")
          val remoteprovider = provider("Remote")
          val selected = fixture.selection(Vector(fixture.directory("Store", "First", storeprovider),
            fixture.directory("Remote", "First", remoteprovider),
            fixture.selected("UsageOnly", "First", None, ComponentCapabilityPackageSource.Location.Directory(fixture.bundle("UsageOnly", "First")))))
          val reference = snapshot("query-real-primary-snapshot", 94)
          val componentref = CapabilityReference(ModelElementId("Model:能力/A", "Component:Store|α"), "contract:二")
          val usecase = ModelElementId("Model:能力/A", "usecase:one")
          val storeusage = fixture.usageProjection("Store", "First")
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
          query.catalog.projection.usage shouldBe accepted.usage
          available(accepted.usage, "Store").projection shouldBe storeusage
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
          Vector("ComponentUsage:One", "ComponentUsage:Two").foreach { element =>
            Given("the original owned Store, UsageOnly and Remote ComponentUsage " + element + " and its Local plus External mappings")
            val appref = ComponentUsageCapabilityReference(ModelElementId("Model:能力/A", element), "app:v1")
            val original = storeusage.documents.head.componentUsageCapabilities.find(_.identity == appref.identity).get
            When("the exact original ComponentUsage reference is traced and diagnosed")
            val traced = _value(query.traceComponentUsageCapability(appref))
            val diagnosed = _value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(appref)))
            Then("its full definition, exact Required/provider branch and External evidence remain original with no role gap")
            traced.size shouldBe 1
            traced.head.usage.definition shouldBe original
            traced.head.paths.map(_.path.mapping) shouldBe _items(original.componentMappings)
            traced.head.paths.map(_.required.size) shouldBe Vector(1, 0)
            traced.head.paths.head.required.head.required shouldBe lookup.records(1)
            traced.head.paths.head.required.head.providers shouldBe providers
            traced.head.paths(1).path.requiredEndpoint shouldBe RequiredEndpoint.Unresolved(
              CapabilityReference(ModelElementId("Other:能力/B", "Component:Remote"), "contract:二"), ReferenceBoundary.External)
            traced.head.paths(1).path.mapping.origin shouldBe _items(original.componentMappings)(1).origin
            diagnosed.usages shouldBe traced
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
          _items(query.catalog.usages.head.definition.demandReferences).map(_.target.identity) should contain (usecase)
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

          Given("separate normally produced Contracts Store/Archive packages with explicitly selected absent Usage")
          val combinedproviders = Vector("Store", "Archive").map(provider)
          val combinedselection = fixture.selection(Vector("Store", "Archive").zip(combinedproviders)
            .map((id, p) => fixture.directory(id, "Contracts", p)))
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
          combined.catalog.resources.map(_.resource.projection) shouldBe Vector("Store", "Archive").map(accessor(_, "Contracts"))
          matches.map(_.provider.key.context.component) shouldBe Vector(coordinate("Store"), coordinate("Archive"))
          matches.map(_.basis) shouldBe Vector.fill(2)(MatchBasis.SameVersion)
          matches.map(_.provider.realizations.size) shouldBe Vector(3, 0)
          matches.head.provider.realizations.map(_.target) shouldBe bindings.map(_.target)
          matches.head.provider.realizations.map(_.owner) shouldBe Vector.fill(3)(matches.head.provider.key)
          archive.provider.declaration.realizations shouldBe Present(Vector.empty)
          direct.usages shouldBe Vector.empty
          direct.required.map(_.required) shouldBe Vector(storerequired)
          direct.gaps shouldBe Vector(CapabilityGap.MissingRealization(combined.scope,
            DemandPath(None, None, storerequired, Some(archive)), Present(Vector.empty)))
          Given("the combined original ComponentUsage family is explicitly unavailable")
          val appref = ComponentUsageCapabilityReference(ModelElementId("Model:能力/A", "ComponentUsage:One"), "app:v1")
          val family = combinedsnapshot.usage.head.family.asInstanceOf[ComponentUsageFamily.Unavailable]
          When("the ComponentUsage is traced and diagnosed without borrowing the primary intake")
          val missingtrace = combined.traceComponentUsageCapability(appref)
          val missingdiagnosis = _value(combined.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(appref)))
          Then("trace remains original unavailable and diagnosis retains only the known missing ComponentUsage boundary")
          missingtrace shouldBe Left(QueryFailure.UnavailableProjection(family.value))
          missingdiagnosis shouldBe DemandDiagnosis(DemandSelection.ComponentUsage(appref), Vector.empty, Vector.empty,
            Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(combined.scope, appref, MissingComponentUsageReason.ProjectionNotSupplied, combinedsnapshot.usage)))
          combinedproviders.map(_.reads) shouldBe Vector(1, 1)
          Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
          fixture.cmlInputs shouldBe Vector.empty
        }
      }

      "C11Q27 preserve Directory and normal CAR Usage/query meaning with distinct acquisition witnesses" in {
        withIntake { fixture =>
          Given("the same normally generated First Store/Remote images and independent native providers in a source-free intake")
          val ids = Vector("Store", "Remote")
          val directoryselection = fixture.directories(ids, "First")
          val directoryreference = snapshot("query-directory-equivalence", 96)
          val directorytransition = right(CapabilityIngestion.ingest(State.empty,
            attempt("query-directory-equivalence-attempt", 96, directoryselection, directoryreference)))
          val directorysnapshot = directorytransition.state.snapshots.head
          val directorylive = right(CapabilityCatalog.fromSnapshot(directorysnapshot, 1))
          val directory = right(CapabilityQueries.open(Right(directorylive.catalog.projection),
            CatalogSelection(directoryreference, directorysnapshot.provenance)))
          fixture.cmlInputs shouldBe Vector.empty
          When("normal package-car packages those original images and public CNCF acquisition feeds accepted CBD catalog and queries")
          val archives = fixture.packageCars()
          val carselection = fixture.selection(ids.zip(archives).map { (id, archive) =>
            fixture.selected(id, "First", Some(provider(id)), ComponentCapabilityPackageSource.Location.Car(archive))
          })
          val carreference = snapshot("query-car-equivalence", 97)
          val cartransition = right(CapabilityIngestion.ingest(State.empty,
            attempt("query-car-equivalence-attempt", 97, carselection, carreference)))
          val carsnapshot = cartransition.state.snapshots.head
          val carlive = right(CapabilityCatalog.fromSnapshot(carsnapshot, 1))
          val carcontext = CatalogSelection(carreference, carsnapshot.provenance)
          val carencoded = right(CapabilityCatalogProjectionCodec.encode(carlive.catalog.projection, carcontext))
          val carportable = right(CapabilityCatalogProjectionCodec.decode(carencoded, carcontext))
          val car = right(CapabilityQueries.open(Right(carportable.projection), carcontext))
          val componentref = CapabilityReference(ModelElementId("Model:能力/A", "Component:Store|α"), "contract:二")
          val directoryproviders = _value(directory.findProviders(componentref))
          val carproviders = _value(car.findProviders(componentref))
          Then("semantic definitions, attributed Required/provider paths, gaps, sources and versions agree while locations remain Directory versus CAR")
          directorytransition.outcome shouldBe Outcome.Accepted(directoryreference)
          cartransition.outcome shouldBe Outcome.Accepted(carreference)
          directorysnapshot.usage.map(_.input.location) shouldBe ids.map(id => ComponentCapabilityPackageSource.Location.Directory(fixture.bundle(id, "First")))
          carsnapshot.usage.map(_.input.location) shouldBe archives.map(ComponentCapabilityPackageSource.Location.Car(_))
          carsnapshot.usage.map(_.input.location) should not be directorysnapshot.usage.map(_.input.location)
          carsnapshot.usage.map(pkg => (pkg.input.component, pkg.input.packageCoordinate, pkg.resourcePath, pkg.family)) shouldBe
            directorysnapshot.usage.map(pkg => (pkg.input.component, pkg.input.packageCoordinate, pkg.resourcePath, pkg.family))
          car.catalog.usages.map(record => (record.key, record.definition, record.documentSource)) shouldBe
            directory.catalog.usages.map(record => (record.key, record.definition, record.documentSource))
          carproviders.required shouldBe directoryproviders.required
          carproviders.matches.map(matched => (matched.provider.key, matched.basis, matched.provider.definition,
            matched.provider.realizations.map(realization => (realization.edge, realization.target, realization.observations)))) shouldBe
            directoryproviders.matches.map(matched => (matched.provider.key, matched.basis, matched.provider.definition,
              matched.provider.realizations.map(realization => (realization.edge, realization.target, realization.observations))))
          carproviders.incompatible.map(_.key) shouldBe directoryproviders.incompatible.map(_.key)
          carproviders.consideredCompatibility shouldBe directoryproviders.consideredCompatibility
          _value(car.findRequirements(coordinate("Store"))).records.map(_.key) shouldBe
            _value(directory.findRequirements(coordinate("Store"))).records.map(_.key)
          Vector("ComponentUsage:One", "ComponentUsage:Two").foreach { element =>
            Given("the same explicit Store Usage reference " + element + " in both accepted acquisition contexts")
            val reference = ComponentUsageCapabilityReference(ModelElementId("Model:能力/A", element), "app:v1")
            When("both sessions trace the owned Usage and diagnose its explicit mappings")
            val directorytrace = _value(directory.traceComponentUsageCapability(reference))
            val cartrace = _value(car.traceComponentUsageCapability(reference))
            val directorydiagnosis = _value(directory.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(reference)))
            val cardiagnosis = _value(car.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(reference)))
            Then("the complete mapping and Required/provider witnesses agree and both diagnoses retain the same absence of gaps")
            cartrace.map(_.usage.key) shouldBe directorytrace.map(_.usage.key)
            cartrace.map(_.usage.definition) shouldBe directorytrace.map(_.usage.definition)
            cartrace.map(_.paths.map(_.path)) shouldBe directorytrace.map(_.paths.map(_.path))
            cartrace.flatMap(_.paths.flatMap(_.required.map(_.required.key))) shouldBe
              directorytrace.flatMap(_.paths.flatMap(_.required.map(_.required.key)))
            cartrace.flatMap(_.paths.flatMap(_.required.flatMap(_.providers.matches.map(matched => (matched.provider.key, matched.basis))))) shouldBe
              directorytrace.flatMap(_.paths.flatMap(_.required.flatMap(_.providers.matches.map(matched => (matched.provider.key, matched.basis)))))
            directorydiagnosis.gaps shouldBe Vector.empty
            cardiagnosis.gaps shouldBe Vector.empty
          }
          carlive.nativeObservations.map(_.resolved.snapshot).zip(snapshots).foreach { (observed, original) =>
            observed should be theSameInstanceAs original
          }
          carselection.packages.map(_.provider.get.asInstanceOf[CapabilityProducerNativeFixtures.CountingProvider].reads) shouldBe Vector(1, 1)
          directoryselection.packages.map(_.provider.get.asInstanceOf[CapabilityProducerNativeFixtures.CountingProvider].reads) shouldBe Vector(1, 1)
          car.traceUseCaseDemand(ModelElementId("Model:能力/A", "usecase:one")) shouldBe Left(QueryFailure.UnavailableProjection(useCase))
          directory.traceUseCaseDemand(ModelElementId("Model:能力/A", "usecase:one")) shouldBe Left(QueryFailure.UnavailableProjection(useCase))
          fixture.cmlInputs shouldBe Vector.empty
        }
      }

    }
  }
  private def _value[A](result: Either[QueryFailure, QueryResult[A]]): A =
    result.fold(error => throw new IllegalArgumentException(error.toString), _.value)
}
