package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalacheck.{Gen, Prop, Test}
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityQueryFixtures.*
import CapabilityApplicationJsonPart._items

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityQuerySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Selected finite query admission" should {
    "retain source outcomes and explicit input boundaries" which {
      "C11Q01 propagate all seven original diagnostic kinds and fields before opening" in {
        Given("an independent selection and seven original source diagnostics")
        val input = scenario()
        val selected = selection(input)
        val originals = DiagnosticKind.values.toVector.map(k => Diagnostic(k, "original:" + k,
          Some("original-scope|É"), Some(ComponentId("org.example.Original")),
          Some(Source("original:authority", "source/original.cml", "revision:É|7", Some(39))), "/original/" + k))
        When("each source outcome reaches open")
        val results = originals.map { original =>
          Given("the complete original " + original.kind + " diagnostic")
          When("open consumes the original Left with independently supplied selection")
          val result = CapabilityQueries.open(Left(original), selected)
          Then("the exact original diagnostic survives with no session or diagnosis")
          result shouldBe Left(original)
          result.isLeft shouldBe true
          result
        }
        Then("all seven failures remain their original values")
        results shouldBe originals.map(Left(_))
        results.size shouldBe 7
      }
      "C11Q02 reject independently stale reference and provenance through the existing validator" in {
        Given("one valid finite projection and two independently stale selections")
        val input = scenario()
        val selected = selection(input)
        val selections = Vector(selected.copy(snapshotReference = CapabilityIngestionFixtures.snapshot("different-snapshot", 91)),
          selected.copy(provenance = input.provenance.copy(scope = input.provenance.scope.copy(
            source = input.provenance.scope.source.copy(sourceRevision = "independent:stale")))))
        When("the unchanged validator and query entry consume each stale selection")
        val outcomes = selections.map { independent =>
          Given("one explicitly different snapshot or provenance selection")
          When("the existing validator and open read the same supplied carrier")
          val original = CapabilityCatalogProjectionCodec.validate(input, independent).swap.toOption.get
          val result = CapabilityQueries.open(Right(input), independent)
          Then("the exact original Stale diagnostic survives without fallback")
          original.kind shouldBe DiagnosticKind.Stale
          result shouldBe Left(original)
          original
        }
        Then("both independent selection domains retain their original locations")
        outcomes.map(_.location) shouldBe Vector("/snapshotReference", "/scope/source/sourceRevision")
      }
      "C11Q05 reject missing qualifiers, versions and malformed Required keys without default selection" in {
        Given("an admitted session and explicit malformed qualified inputs")
        val input = scenario()
        val query = session(input)
        val key = keys(input, Direction.Required).head
        val cases: Vector[(String, () => QueryFailure)] = Vector(
          "/query/capability/identity/modelId" -> (() => failure(query.getCapability(CapabilityLookup.Exact(reference.copy(identity = reference.identity.copy(modelId = "")))))),
          "/query/capability/identity/elementId" -> (() => failure(query.findProviders(reference.copy(identity = reference.identity.copy(elementId = ""))))),
          "/query/capability/contractVersion" -> (() => failure(query.findProviders(reference.copy(contractVersion = "")))),
          "/query/application/identity/modelId" -> (() => failure(query.traceApplicationCapability(applicationReference.copy(identity = applicationReference.identity.copy(modelId = ""))))),
          "/query/application/contractVersion" -> (() => failure(query.traceApplicationCapability(applicationReference.copy(contractVersion = "")))),
          "/query/component/version" -> (() => failure(query.findRequirements(key.context.component.copy(version = "")))),
          "/query/useCase/elementId" -> (() => failure(query.traceUseCaseDemand(ModelElementId("model:facts", "")))),
          "/query/search" -> (() => failure(query.getCapability(CapabilityLookup.Search("")))),
          "/query/required/direction" -> (() => failure(query.diagnoseCapabilityDemand(DemandSelection.Required(key.copy(declaration = key.declaration.copy(direction = Direction.Provided)))))),
          "/query/required/capability/identity/modelId" -> (() => failure(query.diagnoseCapabilityDemand(DemandSelection.Required(key.copy(declaration = key.declaration.copy(capability = reference.copy(identity = reference.identity.copy(modelId = "")))))))),
          "/query/required/context/packageCoordinate/version" -> (() => failure(query.diagnoseCapabilityDemand(DemandSelection.Required(key.copy(context = key.context.copy(packageCoordinate = key.context.packageCoordinate.copy(version = ""))))))),
          "/query/required/context/resourcePath" -> (() => failure(query.diagnoseCapabilityDemand(DemandSelection.Required(key.copy(context = key.context.copy(resourcePath = "../invalid")))))),
          "/query/required/context/source/sourceRevision" -> (() => failure(query.diagnoseCapabilityDemand(DemandSelection.Required(key.copy(context = key.context.copy(source = key.context.source.copy(sourceRevision = ""))))))),
          "/query/required/component" -> (() => failure(query.diagnoseCapabilityDemand(DemandSelection.Required(key.copy(declaration = key.declaration.copy(component = ComponentId("org.example.Other"))))))))
        When("each declared malformed input invokes its semantic query")
        val outcomes = cases.map { (path, action) =>
          Given("the explicit missing or malformed field " + path)
          When("the corresponding operation validates its declared input")
          val diagnostic = sourceDiagnostic(action())
          Then("Malformed retains the selected scope/source and precise input location")
          diagnostic.kind shouldBe DiagnosticKind.Malformed
          diagnostic.location shouldBe path
          diagnostic.scopeId shouldBe Some(input.provenance.scope.scopeId)
          diagnostic.source shouldBe Some(input.provenance.scope.source)
          diagnostic
        }
        Then("all fourteen declared input failures remain distinct")
        outcomes.size shouldBe 14
        outcomes(5).component shouldBe Some(key.context.component.componentId)
        outcomes.drop(8).map(_.component) shouldBe Vector.fill(6)(Some(key.context.component.componentId))
        Given("a complete valid Required key absent from the selected catalog")
        val missing = key.copy(context = key.context.copy(source = key.context.source.copy(sourceRevision = "absent:revision")))
        When("the exact Required selector is diagnosed")
        val unavailable = sourceDiagnostic(failure(query.diagnoseCapabilityDemand(DemandSelection.Required(missing))))
        Then("only the selected record is Unavailable and no NoProvider gap is fabricated")
        unavailable.kind shouldBe DiagnosticKind.Unavailable
        unavailable.location shouldBe "/query/required"
        unavailable.component shouldBe Some(key.context.component.componentId)
        unavailable.scopeId shouldBe Some(input.provenance.scope.scopeId)
        unavailable.source shouldBe Some(input.provenance.scope.source)
      }
    }
  }
  "Qualified lookup and discovery assistance" should {
    "preserve original records without authorizing semantic text matching" which {
      "C11Q03 retain both exact directions and every distinct context while excluding another identity or version" in {
        Given("four ordered packages with multiple exact contexts, another version and an equal-label other identity")
        val otherversion = reference.copy(contractVersion = "version:OTHER|É")
        val otheridentity = reference.copy(identity = reference.identity.copy(modelId = "Other:model"))
        val selectedscope = scope(Vector(reference, otherversion, otheridentity).map(definition(_)))
        val packages = Vector(packageFor("One", "first", selectedscope, reference, Direction.values.toVector),
          packageFor("Two", "second", selectedscope, reference, Direction.values.toVector),
          packageFor("Three", "third", selectedscope, otherversion, Direction.values.toVector),
          packageFor("Four", "fourth", selectedscope, otheridentity, Direction.values.toVector))
        val input = assemble(packages, selectedscope, None)
        val query = session(input)
        val lookup = CapabilityLookup.Exact(reference)
        When("exact lookup uses the original qualified reference and opaque version")
        val result = CapabilityQueryFixtures.value(query.getCapability(lookup))
        Then("all four exact records and original context values survive in catalog order")
        result.lookup shouldBe lookup
        result.designation shouldBe LookupDesignation.ExactIdentity
        result.records.map(_.key) shouldBe packages.take(2).flatMap(p => p.resources.flatMap(r => r.projection.declarations.map(CapabilityCatalogFixtures.declarationkey(p, r, _))))
        result.records.map(_.declaration.direction) shouldBe Vector(Direction.Provided, Direction.Required, Direction.Provided, Direction.Required)
        result.records shouldBe query.catalog.capabilities.take(4)
        query.getCapability(lookup).toOption.get.scope shouldBe QueryScope(input.snapshotReference, input.provenance)
      }
      "C11Q04 keep literal Locale.ROOT discovery separate from provider matching" in {
        Given("an equal-label provider of another identity and absent summary/description on the Required definition")
        val other = reference.copy(identity = reference.identity.copy(modelId = "Other:discovery"))
        val definitions = Vector(definition(reference).copy(summary = Absent(AbsenceReason.NotDeclared, "Alpha absence detail is not searchable."),
          description = Absent(AbsenceReason.NotRepresented, "Description absence detail is not searchable.")), definition(other))
        val selectedscope = scope(definitions)
        val input = assemble(Vector(packageFor("Need", "need", selectedscope, reference, Vector(Direction.Required), 0),
          packageFor("Other", "other", selectedscope, other, Vector(Direction.Provided))), selectedscope, None)
        val query = session(input)
        val searches = Vector("SAME CAPABILITY É" -> 2, "SUMMARY LITERAL" -> 1, "DESCRIPTION HAS" -> 1, " padded " -> 1, "  padded  " -> 1, "   padded   " -> 0, "absence detail" -> 0)
        When("literal nonempty substring queries read original metadata")
        searches.foreach { (text, count) =>
          Given("the original metadata and literal discovery text '" + text + "'")
          When("Search performs case-insensitive literal assistance")
          val result = CapabilityQueryFixtures.value(query.getCapability(CapabilityLookup.Search(text)))
          Then("the original records have DiscoveryOnly designation and literal cardinality")
          result.designation shouldBe LookupDesignation.DiscoveryOnly
          result.records.size shouldBe count
          result.records.foreach(r => query.catalog.capabilities should contain (r))
          result.lookup shouldBe CapabilityLookup.Search(text)
        }
        Then("same display text and discovery matches do not become identity-compatible providers")
        CapabilityQueryFixtures.value(query.findProviders(reference)).matches shouldBe Vector.empty
        CapabilityQueryFixtures.value(query.getCapability(CapabilityLookup.Search("SUMMARY LITERAL"))).records.head.definition shouldBe definitions(1)
      }
    }
  }
  "Directed provider and exact requirement searches" should {
    "retain all owning declarations and only explicit compatibility" which {
      "C11Q06 retain multiple providers and their own three realization kinds without a winner" in {
        Given("two original Provided Components with three and two own realization references")
        val input = scenario(Vector(ProviderInput(reference, 3), ProviderInput(reference, 2)))
        val query = session(input)
        When("providers are searched by the exact qualified Capability version")
        val result = CapabilityQueryFixtures.value(query.findProviders(reference))
        Then("both providers retain original owners, own edge order and four separate Unknown values")
        result.matches.map(_.provider.key) shouldBe keys(input, Direction.Provided)
        result.matches.map(_.basis) shouldBe Vector.fill(2)(MatchBasis.SameVersion)
        result.matches.map(_.provider.realizations.size) shouldBe Vector(3, 2)
        result.matches.map(_.provider.realizations.map(_.edge.targetKind)) shouldBe Vector(TargetKind.values.toVector, TargetKind.values.toVector.take(2))
        result.matches.zip(input.packages.tail).foreach { (matched, pkg) =>
          Given("the exact provider's original resource-local edge and target vector")
          When("its matched record is read")
          val refs = matched.provider.realizations
          Then("only that owner's originals and all four Unknown observations remain")
          refs.map(_.owner) shouldBe Vector.fill(refs.size)(matched.provider.key)
          refs.map(_.edge) shouldBe pkg.resources.head.realizations.map(_.edge)
          refs.map(_.target) shouldBe pkg.resources.head.realizations.map(_.target)
          refs.map(_.observations.availability) shouldBe Vector.fill(refs.size)(AvailabilityObservation.Unknown("No runtime observation supplied."))
          refs.map(_.observations.authorization) shouldBe Vector.fill(refs.size)(AuthorizationObservation.Unknown("No runtime observation supplied."))
          refs.map(_.observations.guard) shouldBe Vector.fill(refs.size)(GuardObservation.Unknown("No runtime observation supplied."))
          refs.map(_.observations.executionResult) shouldBe Vector.fill(refs.size)(ExecutionResultObservation.Unknown("No runtime observation supplied."))
        }
      }
      "C11Q07 keep Provided and Required separate and select complete Component coordinates across contexts" in {
        Given("the same Component in two distinct resource/package sources with both directions")
        val selectedscope = scope(Vector(definition(reference)))
        val packages = Vector(packageFor("Same", "first", selectedscope, reference, Direction.values.toVector, 0),
          packageFor("Same", "second", selectedscope, reference, Direction.values.toVector, 0))
        val input = assemble(packages, selectedscope, None)
        val query = session(input)
        val component = packages.head.input.component
        When("findRequirements selects that complete Component coordinate")
        val result = CapabilityQueryFixtures.value(query.findRequirements(component))
        Then("only the two original Required records survive while Provided stays distinct")
        result.component shouldBe component
        result.records.map(_.key) shouldBe keys(input, Direction.Required)
        result.records.map(_.declaration.direction) shouldBe Vector.fill(2)(Direction.Required)
        CapabilityQueryFixtures.value(query.findProviders(reference)).matches.size shouldBe 2
        Given("an absent exact Component and an absent exact Component version")
        val absent = Vector(component.copy(componentId = ComponentId("org.example.Absent")), component.copy(version = "other:version"))
        When("the exact coordinates are searched in the admitted finite scope")
        val empty = absent.map(c => query.findRequirements(c))
        Then("each result is scoped success-empty without diagnosis")
        empty shouldBe absent.map(c => Right(QueryResult(query.scope, RequirementSearch(c, Vector.empty))))
      }
      "C11Q08 retain every original directed compatibility witness while matching each provider once" in {
        Given("a different opaque version with two directed witnesses in independent contexts")
        val provided = reference.copy(contractVersion = "provided:opaque/É")
        val first = compatibility(reference, provided.contractVersion, "witness:first")
        val second = first.copy(source = first.source.copy(sourceRevision = "witness:second"), origin = Declared)
        val base = scenario(Vector(ProviderInput(provided)), witnesses = Vector(first))
        val input = base.copy(packages = base.packages.updated(1, withCompatibility(base.packages(1), Vector(second))))
        val query = session(input)
        val expected = Vector(CompatibilityRecord(CapabilityCatalogFixtures.contextkey(input.packages.head, input.packages.head.resources.head), first),
          CompatibilityRecord(CapabilityCatalogFixtures.contextkey(input.packages(1), input.packages(1).resources.head), second))
        When("the required opaque version is searched")
        val result = CapabilityQueryFixtures.value(query.findProviders(reference))
        Then("the provider appears once with both ordered original context/Source/Origin witnesses")
        result.matches.size shouldBe 1
        result.matches.head.basis shouldBe MatchBasis.ExplicitCompatibility(expected)
        result.consideredCompatibility shouldBe expected
        result.incompatible shouldBe Vector.empty
        result.matches.head.provider shouldBe query.catalog.capabilities(1)
      }
      "C11Q09 reject reverse-only, transitive-only, semver-like and equal-label other identities" in {
        Given("only reverse/transitive edges and semver-looking versions plus another qualified identity")
        val required = reference.copy(contractVersion = "1.0.0")
        val middle = required.copy(contractVersion = "1.1.0")
        val provided = required.copy(contractVersion = "2.0.0")
        val other = provided.copy(identity = provided.identity.copy(modelId = "Other:same-label"))
        val edges = Vector(compatibility(provided, required.contractVersion, "reverse"), compatibility(required, middle.contractVersion, "to-middle"),
          compatibility(middle, provided.contractVersion, "from-middle"))
        val input = scenario(Vector(ProviderInput(provided), ProviderInput(other)), witnesses = edges,
          extraDefinitions = Vector(middle), mappings = Vector(mapping(required)), requiredReference = required)
        val query = session(input)
        When("matching consumes only exact directed witnesses for the selected pair")
        val result = CapabilityQueryFixtures.value(query.findProviders(required))
        Then("none matches and only the same-identity original wrong-version provider is a candidate")
        result.matches shouldBe Vector.empty
        result.candidates.map(_.key) shouldBe keys(input, Direction.Provided).take(1)
        result.incompatible shouldBe result.candidates
        result.consideredCompatibility.map(_.edge) shouldBe edges
        CapabilityQueryFixtures.value(query.getCapability(CapabilityLookup.Search("Same capability"))).records.size shouldBe 3
      }
      "C11Q10 establish successful empty provider search only in the admitted scope" in {
        Given("a real Required declaration and no Provided identity candidate in a valid finite projection")
        val input = scenario(Vector.empty)
        val query = session(input)
        When("findProviders searches the real Required reference")
        val result = query.findProviders(reference)
        Then("success retains exact selected provenance and an empty search without a synthesized gap")
        result shouldBe Right(QueryResult(QueryScope(input.snapshotReference, input.provenance),
          ProviderSearch(reference, Vector.empty, Vector.empty, Vector.empty, Vector.empty)))
        keys(input, Direction.Required).size shouldBe 1
      }
      "C11Q24 actively sample exact, directed, wrong-version and other-identity finite scopes with zero discards" in {
        Given("valid-by-construction tags, literal Unicode/case/delimiters and independent provider, own realization and repeated mapping counts")
        val samples = for {
          token <- Gen.oneOf("É|Case::A", "É|case::a", "支払/契約", "Cap:cap|#")
          status <- Gen.oneOf(SampleStatus.values.toVector)
          count <- Gen.choose(0, 3)
          targets <- Gen.listOfN(count, Gen.choose(0, 3)).map(_.toVector)
          mappings <- Gen.choose(0, 3)
        } yield Sample(token, status, count, targets, mappings)
        When("ScalaCheck executes at least one hundred admitted samples")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { sample =>
          Given("one explicit sample tag and its own provider/realization/mapping counts")
          val input = generated(sample)
          val required = input.packages.head.resources.head.projection.declarations.head.capability
          val expectedkeys = keys(input, Direction.Provided)
          val compatible = sample.status == SampleStatus.Exact || sample.status == SampleStatus.Directed
          val candidatecount = if (sample.status == SampleStatus.OtherIdentity) 0 else sample.providercount
          val matchcount = if (compatible) sample.providercount else 0
          val expectedwitnesses = input.packages.flatMap(p => p.resources.flatMap(r => _items(r.projection.compatibility)
            .map(CompatibilityRecord(CapabilityCatalogFixtures.contextkey(p, r), _))))
          When("open admits the full carrier and queries its providers and original Application paths")
          val query = session(input)
          val search = CapabilityQueryFixtures.value(query.findProviders(required))
          val traced = CapabilityQueryFixtures.value(query.traceApplicationCapability(applicationReference))
          Then("sample-derived exact/directed cardinalities, original witnesses and own realization isolation hold")
          search.candidates.size shouldBe candidatecount
          search.matches.size shouldBe matchcount
          search.incompatible.size shouldBe candidatecount - matchcount
          search.consideredCompatibility shouldBe expectedwitnesses
          search.matches.map(_.provider.key) shouldBe (if (compatible) expectedkeys else Vector.empty)
          search.matches.map(_.basis) shouldBe (sample.status match {
            case SampleStatus.Exact => Vector.fill(matchcount)(MatchBasis.SameVersion)
            case SampleStatus.Directed => Vector.fill(matchcount)(MatchBasis.ExplicitCompatibility(expectedwitnesses))
            case _ => Vector.empty
          })
          search.matches.map(_.provider.realizations.size) shouldBe (if (compatible) sample.targets else Vector.empty)
          search.matches.zip(input.packages.tail).foreach { (matched, pkg) =>
            Given("one generated compatible provider's original resource-local facts")
            When("its realization vector is read")
            val refs = matched.provider.realizations
            Then("only its own edges, targets and original owner occur")
            refs.map(_.edge) shouldBe pkg.resources.head.realizations.map(_.edge)
            refs.map(_.target) shouldBe pkg.resources.head.realizations.map(_.target)
            refs.map(_.owner) shouldBe Vector.fill(refs.size)(matched.provider.key)
          }
          traced.size shouldBe 1
          traced.head.paths.size shouldBe sample.mappingcount
          traced.head.paths.map(_.path.mapping) shouldBe Vector.fill(sample.mappingcount)(mapping(required))
          traced.head.paths.map(_.required.size) shouldBe Vector.fill(sample.mappingcount)(1)
          traced.head.paths.flatMap(_.required).map(_.required.key) shouldBe Vector.fill(sample.mappingcount)(keys(input, Direction.Required).head)
          traced.head.paths.flatMap(_.required).map(_.providers.matches.size) shouldBe Vector.fill(sample.mappingcount)(matchcount)
          traced.head.paths.flatMap(_.required.flatMap(_.providers.matches.map(_.provider.realizations.size))) shouldBe
            Vector.fill(sample.mappingcount)(if (compatible) sample.targets else Vector.empty).flatten
          query.scope shouldBe QueryScope(input.snapshotReference, input.provenance)
          true
        })
        Then("at least one hundred applicable samples pass with exactly zero discarded cases")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
        info(s"C11Q24 generated query samples: succeeded=${checked.succeeded}, discarded=${checked.discarded}")
      }
    }
  }
}
