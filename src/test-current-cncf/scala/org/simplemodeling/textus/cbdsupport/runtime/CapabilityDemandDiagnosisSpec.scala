package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityQueryFixtures.*
import CapabilityComponentUsageJsonPart._items

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityDemandDiagnosisSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentUsage-owned demand traces" should {
    "C11Q26 trace equal references by owner and retain mixed availability in missing diagnosis" in {
      Given("two selected package owners with the same exact Usage reference and a separate explicit optional-absence selection")
      val base = scenario()
      val projection = base.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].projection
      val both = CapabilityCatalogFixtures.withUsagePackages(base, Vector(Some(projection), Some(projection)))
      val mixed = CapabilityCatalogFixtures.withUsagePackages(base, Vector(Some(projection), None))
      val empty = CapabilityCatalogFixtures.withUsagePackages(base, Vector(Some(projection.copy(documents = Vector.empty)), Some(projection.copy(documents = Vector.empty))))
      val absent = usageReference.copy(contractVersion = "absent:per-package")
      When("the exact reference is traced and the absent reference is diagnosed across those explicit families")
      val traces = CapabilityQueryFixtures.value(session(both).traceComponentUsageCapability(usageReference))
      val mixedquery = session(mixed)
      val diagnosis = CapabilityQueryFixtures.value(mixedquery.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(absent)))
      val emptyquery = session(empty)
      val emptydiagnosis = CapabilityQueryFixtures.value(emptyquery.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(absent)))
      Then("both owner keys survive, mixed absence remains projection-not-supplied, and acquired empty is scoped absent-reference")
      traces.map(_.usage.key.reference) shouldBe Vector.fill(2)(usageReference)
      traces.map(_.usage.key.component) shouldBe both.packages.map(_.input.component)
      traces.map(_.usage.key).distinct.size shouldBe 2
      traces.flatMap(_.paths).map(_.path.owner) shouldBe traces.map(_.usage.key)
      diagnosis.gaps shouldBe Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(mixedquery.scope, absent,
        MissingComponentUsageReason.ProjectionNotSupplied, mixed.usage))
      CapabilityQueryFixtures.value(mixedquery.traceComponentUsageCapability(absent)) shouldBe Vector.empty
      emptydiagnosis.gaps shouldBe Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(emptyquery.scope, absent,
        MissingComponentUsageReason.AbsentSelectedReference, empty.usage))
      mixedquery.traceUseCaseDemand(ModelElementId("model:facts", "usecase")) shouldBe Left(QueryFailure.UnavailableProjection(mixed.useCase))
    }
    "retain each original context, mapping and Required/provider branch" which {
      "C11Q11 preserve repeated mappings, two distinct exact versioned ComponentUsage references and all nested ownership" in {
        Given("two distinct exact versioned ComponentUsage references and another version of the first, two mappings, two Required contexts and two providers with three and one own realizations")
        val mappings = Vector(mapping(), mapping())
        val input = scenario(Vector(ProviderInput(reference, 3), ProviderInput(reference, 1)), requiredcount = 2,
          mappings = mappings, contexts = 2, otherUsageVersion = true)
        val query = session(input)
        val originalapps = input.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].projection.documents.take(2)
        val references = originalapps.map { document =>
          val definition = document.componentUsageCapabilities.head
          ComponentUsageCapabilityReference(definition.identity, definition.contractVersion)
        }
        When("each exact versioned ComponentUsage reference is traced and diagnosed in its original document order")
        val result = originalapps.zip(references).flatMap { (document, selectedreference) =>
          Given("one original document definition and its exact identity and contract version")
          val selector = DemandSelection.ComponentUsage(selectedreference)
          When("that exact ComponentUsage reference is traced and diagnosed through the public Session APIs")
          val traced = CapabilityQueryFixtures.value(query.traceComponentUsageCapability(selectedreference))
          val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(selector))
          Then("exactly one ComponentUsage retains the selected reference, original documentSource and full definition, excluding the other identity and the same identity at another version")
          traced.size shouldBe 1
          traced.map(_.usage.key.reference) shouldBe Vector(selectedreference)
          traced.map(_.usage.documentSource) shouldBe Vector(document.source)
          traced.map(_.usage.definition) shouldBe Vector(document.componentUsageCapabilities.head)
          Then("diagnosis retains that exact selector and trace with no direct Required results or gaps")
          diagnosis.selection shouldBe selector
          diagnosis.usages shouldBe traced
          diagnosis.required shouldBe Vector.empty
          diagnosis.gaps shouldBe Vector.empty
          traced
        }
        Then("each exact context retains two mappings, each mapping two Required owners and each Required both providers")
        result.size shouldBe 2
        result.map(_.usage.definition) shouldBe originalapps.map(_.componentUsageCapabilities.head)
        result.map(_.usage.documentSource) shouldBe originalapps.map(_.source)
        result.map(_.paths.size) shouldBe Vector(2, 2)
        result.flatMap(_.paths).map(_.path.mapping) shouldBe Vector.fill(2)(mappings).flatten
        result.flatMap(_.paths).map(_.required.map(_.required.key)) shouldBe Vector.fill(4)(keys(input, Direction.Required))
        result.flatMap(_.paths.flatMap(_.required)).map(_.providers.matches.map(_.provider.key)) shouldBe Vector.fill(8)(keys(input, Direction.Provided))
        result.flatMap(_.paths.flatMap(_.required.flatMap(_.providers.matches))).map(_.provider.realizations.size) shouldBe Vector.fill(8)(Vector(3, 1)).flatten
        result.flatMap(_.paths.flatMap(_.required.flatMap(_.providers.matches))).foreach { matched =>
          Given("one original provider under its exact ComponentUsage/mapping/Required branch")
          When("that nested provider's owned realization vector is read")
          val refs = matched.provider.realizations
          Then("every reference retains only that provider's key and original source/origin")
          refs.map(_.owner) shouldBe Vector.fill(refs.size)(matched.provider.key)
          val pkg = input.packages.find(_.input == matched.provider.context.input).get
          refs.map(_.edge) shouldBe pkg.resources.head.realizations.map(_.edge)
          refs.map(_.edge.origin) shouldBe pkg.resources.head.realizations.map(_.edge.origin)
        }
      }
      "C11Q12 retain External unresolved evidence without manufacturing Local branches or gaps" in {
        Given("one original External mapping with a qualified foreign target and Derived source evidence")
        val external = mapping(reference.copy(identity = reference.identity.copy(modelId = "External:contract")), ReferenceBoundary.External)
        val input = scenario(mappings = Vector(external), requiredroles = Vector.empty, usageroles = Vector.empty)
        val query = session(input)
        When("the ComponentUsage trace and diagnosis consume that explicit External occurrence")
        val traced = CapabilityQueryFixtures.value(query.traceComponentUsageCapability(usageReference))
        val diagnosed = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(usageReference)))
        Then("the exact mapping/target/Source/Origin and unresolved boundary survive with no local gap")
        traced.head.paths shouldBe Vector(RequirementPathTrace(
          ComponentUsageRequirementPath(traced.head.usage.key, external, RequiredEndpoint.Unresolved(external.target, ReferenceBoundary.External)), Vector.empty))
        traced.head.paths.head.path.mapping.source shouldBe external.source
        traced.head.paths.head.path.mapping.origin shouldBe external.origin
        diagnosed.usages shouldBe traced
        diagnosed.gaps shouldBe Vector.empty
      }
      "C11Q13 emit only the exact missing Required boundary for a Local unresolved mapping" in {
        Given("a Local mapping to an admitted definition with no Required declaration")
        val orphan = reference.copy(identity = reference.identity.copy(elementId = "orphan:definition"))
        val local = mapping(orphan)
        val input = scenario(mappings = Vector(local), extraDefinitions = Vector(orphan), usageroles = Vector.empty)
        val query = session(input)
        When("the Local unresolved ComponentUsage branch is diagnosed")
        val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(usageReference)))
        val app = diagnosis.usages.head
        Then("only MissingRequiredComponentCapability retains the enclosing full ComponentUsage and original edge")
        app.paths.head.required shouldBe Vector.empty
        app.paths.head.path.requiredEndpoint shouldBe RequiredEndpoint.Unresolved(orphan, ReferenceBoundary.Local)
        diagnosis.gaps shouldBe Vector(CapabilityGap.MissingRequiredComponentCapability(query.scope, app.usage, app.paths.head.path))
        app.paths.head.path.mapping shouldBe local
        app.paths.head.path.mapping.origin shouldBe local.origin
      }
      "C11Q14 distinguish unavailable ComponentUsage trace from its known missing projection diagnosis" in {
        Given("an explicitly unavailable ComponentUsage family admitted with its original provenance")
        val input = scenario(usageAvailable = false)
        val query = session(input)
        val family = input.usage.head.family.asInstanceOf[ComponentUsageFamily.Unavailable]
        When("that ComponentUsage reference is traced and diagnosed")
        val traced = query.traceComponentUsageCapability(usageReference)
        val diagnosed = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(usageReference)))
        Then("trace is original typed unavailable while diagnosis has only projection-not-supplied")
        traced shouldBe Left(QueryFailure.UnavailableProjection(family.value))
        diagnosed shouldBe DemandDiagnosis(DemandSelection.ComponentUsage(usageReference), Vector.empty, Vector.empty,
          Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(query.scope, usageReference, MissingComponentUsageReason.ProjectionNotSupplied, input.usage)))
      }
      "C11Q15 return scoped empty trace and only absent-selected-reference for an absent exact ComponentUsage version" in {
        Given("an Available admitted family and an absent exact versioned ComponentUsage selection")
        val input = scenario()
        val query = session(input)
        val absent = usageReference.copy(contractVersion = "absent:opaque-version")
        When("the absent exact ComponentUsage is traced and diagnosed")
        val traced = query.traceComponentUsageCapability(absent)
        val diagnosed = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(absent)))
        Then("success-empty and the sole selected-reference gap retain the original Available family and scope")
        traced shouldBe Right(QueryResult(query.scope, Vector.empty))
        diagnosed shouldBe DemandDiagnosis(DemandSelection.ComponentUsage(absent), Vector.empty, Vector.empty,
          Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(query.scope, absent, MissingComponentUsageReason.AbsentSelectedReference, input.usage)))
      }
      "C11Q16 keep forward UseCase trace and diagnosis unavailable despite admitted facts and reverse links" in {
        Given("an admitted UseCase model fact and original ComponentUsage reverse UseCase demand link")
        val input = scenario()
        val query = session(input)
        val usecase = ModelElementId("model:facts", "usecase")
        val app = query.catalog.usages.head
        input.provenance.scope.modelFacts.map(_.identity) should contain (usecase)
        _items(app.definition.demandReferences).map(_.target.identity) should contain (usecase)
        When("forward UseCase trace and demand diagnosis are requested")
        val traced = query.traceUseCaseDemand(usecase)
        val diagnosed = query.diagnoseCapabilityDemand(DemandSelection.UseCase(usecase))
        Then("both operations retain the original not-yet-admitted unavailable value and create no positive result")
        traced shouldBe Left(QueryFailure.UnavailableProjection(input.useCase))
        diagnosed shouldBe Left(QueryFailure.UnavailableProjection(input.useCase))
        input.useCase.reason shouldBe "not-yet-admitted"
      }
    }
  }
  "Known Required demand diagnosis" should {
    "distinguish absent, incompatible and compatible providers" which {
      "C11Q17 attach NoProvider only to a real Required owner within the selected finite scope" in {
        Given("a real Required declaration with no exact-identity Provided candidate")
        val input = scenario(Vector.empty, requiredroles = Vector.empty, usageroles = Vector.empty)
        val query = session(input)
        val key = keys(input, Direction.Required).head
        When("the Required key and enclosing ComponentUsage are independently diagnosed")
        val direct = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.Required(key)))
        val app = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(usageReference)))
        Then("NoProvider retains exact scoped full Required and the optional original ComponentUsage mapping")
        direct.required.size shouldBe 1
        direct.usages shouldBe Vector.empty
        val path = DemandPath(None, None, direct.required.head.required, None)
        direct.gaps.head shouldBe CapabilityGap.NoProvider(query.scope, path, direct.required.head.providers)
        direct.required.head.providers.candidates shouldBe Vector.empty
        direct.required.head.required.key shouldBe key
        direct.gaps.map(_.productPrefix) shouldBe Vector("NoProvider", "MissingSpecification", "MissingEvidence")
        val apppath = DemandPath(Some(app.usages.head.usage), Some(app.usages.head.paths.head.path), direct.required.head.required, None)
        app.gaps.head shouldBe CapabilityGap.NoProvider(query.scope, apppath, direct.required.head.providers)
        direct.gaps.collect { case CapabilityGap.MissingSpecification(_, _, coverage) => coverage.boundary } shouldBe Vector(CoverageBoundary.RequiredPathOnly)
        query.scope shouldBe QueryScope(input.snapshotReference, input.provenance)
      }
      "C11Q18 preserve every wrong-version candidate and searched witnesses in the sole provider version gap" in {
        Given("two same-identity wrong-version providers and a reverse-only compatibility witness")
        val first = reference.copy(contractVersion = "wrong:v1")
        val second = reference.copy(contractVersion = "wrong:v2")
        val reverse = compatibility(first, reference.contractVersion, "reverse:witness")
        val input = scenario(Vector(ProviderInput(first), ProviderInput(second)), witnesses = Vector(reverse))
        val query = session(input)
        When("the original Required key is diagnosed")
        val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.Required(keys(input, Direction.Required).head)))
        val branch = diagnosis.required.head
        Then("IncompatibleProviderVersion retains both original candidates and excludes NoProvider")
        diagnosis.gaps shouldBe Vector(CapabilityGap.IncompatibleProviderVersion(query.scope,
          DemandPath(None, None, branch.required, None), branch.providers))
        branch.providers.candidates.map(_.key) shouldBe keys(input, Direction.Provided)
        branch.providers.incompatible.map(_.declaration.capability) shouldBe Vector(first, second)
        branch.providers.consideredCompatibility.map(_.edge) shouldBe Vector(reverse)
        branch.providers.matches shouldBe Vector.empty
      }
      "C11Q19 retain mixed compatible and incompatible originals without a blocking provider gap" in {
        Given("same-version, explicitly compatible and wrong-version Provided declarations")
        val directed = reference.copy(contractVersion = "directed:provided")
        val wrong = reference.copy(contractVersion = "wrong:provided")
        val witness = compatibility(reference, directed.contractVersion, "directed:witness")
        val input = scenario(Vector(ProviderInput(reference), ProviderInput(directed), ProviderInput(wrong)), witnesses = Vector(witness))
        val query = session(input)
        When("the Required demand is diagnosed")
        val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.Required(keys(input, Direction.Required).head)))
        val search = diagnosis.required.head.providers
        Then("both compatible branches remain and the wrong-version candidate stays observable without a gap")
        search.matches.map(_.provider.key) shouldBe keys(input, Direction.Provided).take(2)
        search.incompatible.map(_.key) shouldBe keys(input, Direction.Provided).drop(2)
        search.matches.map(_.basis) shouldBe Vector(MatchBasis.SameVersion, MatchBasis.ExplicitCompatibility(search.consideredCompatibility))
        search.candidates.size shouldBe 3
        diagnosis.gaps shouldBe Vector.empty
      }
      "C11Q20 preserve empty or absent own realization Presence without borrowing a sibling's edges" in {
        Given("compatible providers with Present(empty), Absent and a sibling's three original edges")
        val input = scenario(Vector(ProviderInput(reference, 0), ProviderInput(reference, 0, absent = true), ProviderInput(reference, 3)))
        val query = session(input)
        When("the Required demand traverses every compatible provider")
        val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.Required(keys(input, Direction.Required).head)))
        val branch = diagnosis.required.head
        Then("exactly the two empty owners have MissingRealization with their original Presence and scope")
        diagnosis.gaps shouldBe branch.providers.matches.take(2).map(p => CapabilityGap.MissingRealization(query.scope,
          DemandPath(None, None, branch.required, Some(p)), p.provider.declaration.realizations))
        branch.providers.matches.map(_.provider.realizations.size) shouldBe Vector(0, 0, 3)
        diagnosis.gaps.collect { case CapabilityGap.MissingRealization(_, _, original) => original } shouldBe
          Vector(Present(Vector.empty), Absent(AbsenceReason.NotDeclared, "This provider declared no realization."))
        branch.providers.matches(2).provider.realizations.map(_.owner) shouldBe Vector.fill(3)(branch.providers.matches(2).provider.key)
      }
    }
  }
  "Per-path Specification and Evidence coverage" should {
    "assess only known owners and preserve genuine repetitions" which {
      "C11Q21 independently report opposite missing roles and include a shared definition owner once" in {
        Given("a same-context Required/Provided pair whose shared definition has repeated original facts of one role")
        val cases = Vector(TraceabilityRole.Specification -> "MissingEvidence", TraceabilityRole.Evidence -> "MissingSpecification")
        When("each partial role projection is diagnosed")
        val outcomes = cases.map { (role, missing) =>
          Given("two genuine original " + role.tag + " facts on one shared definition and distinct absent declaration Presence")
          val originaldefinition = definition(reference, Vector(role, role))
          val selectedscope = scope(Vector(originaldefinition))
          val pkg = packageFor("Shared", "shared", selectedscope, reference, Direction.values.toVector, 3, Vector.empty)
          val updated = pkg.copy(resources = pkg.resources.map(r => r.copy(projection = r.projection.copy(declarations = r.projection.declarations.map { d =>
            d.copy(traceability = Absent(AbsenceReason.NotRepresented, "No declaration " + d.direction.tag + " traceability."))
          }))))
          val input = assemble(Vector(updated), selectedscope, None)
          val query = session(input)
          When("the shared Required key is diagnosed with its compatible same-context provider")
          val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.Required(keys(input, Direction.Required).head)))
          val gap = diagnosis.gaps.head
          val coverage = gap match {
            case CapabilityGap.MissingSpecification(_, _, c) => c
            case CapabilityGap.MissingEvidence(_, _, c) => c
            case _ => fail("The independently missing role is required.")
          }
          Then("only the opposite role is missing, shared owner membership occurs once and both genuine facts remain")
          diagnosis.gaps.map(_.productPrefix) shouldBe Vector(missing)
          coverage.boundary shouldBe CoverageBoundary.ProviderPath
          val required = diagnosis.required.head.required
          val provided = diagnosis.required.head.providers.matches.head.provider
          coverage.owners.map(_.owner) shouldBe Vector(TraceabilityOwner.Declaration(required.key),
            TraceabilityOwner.CapabilityDefinition(required.key.context, reference), TraceabilityOwner.Declaration(provided.key))
          coverage.owners.map(_.traceability) shouldBe Vector(required.declaration.traceability, originaldefinition.traceability, provided.declaration.traceability)
          val facts = if (role == TraceabilityRole.Specification) coverage.specification else coverage.evidence
          facts.map(_.edge) shouldBe traces(reference.identity.modelId, Vector(role, role))
          facts.size shouldBe 2
          facts.map(_.edge.target.boundary) shouldBe Vector.fill(2)(ReferenceBoundary.External)
          facts.map(_.edge.source) shouldBe _items(originaldefinition.traceability).map(_.source)
          facts.map(_.edge.origin) shouldBe _items(originaldefinition.traceability).map(_.origin)
          gap
        }
        Then("Specification-only and Evidence-only remain separate diagnoses")
        outcomes.map(_.productPrefix) shouldBe Vector("MissingEvidence", "MissingSpecification")
      }
      "C11Q22 exclude unrelated ComponentUsage, Capability and incompatible-provider role facts from the selected path" in {
        Given("role facts only on an unrelated ComponentUsage, another Capability and an incompatible sibling provider")
        val wrong = reference.copy(contractVersion = "wrong:role-version")
        val other = reference.copy(identity = reference.identity.copy(elementId = "Other:roles"))
        val orphan = reference.copy(identity = reference.identity.copy(elementId = "Orphan:roles"))
        val external = mapping(other, ReferenceBoundary.External)
        val base = scenario(Vector(ProviderInput(reference, 3, Vector.empty), ProviderInput(wrong), ProviderInput(other)),
          requiredroles = Vector.empty, usageroles = Vector.empty, extraDefinitions = Vector(orphan),
          mappings = Vector(mapping(), mapping(orphan), external))
        val originalapp = base.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].projection
        val unrelated = originalapp.documents.head.componentUsageCapabilities.head.copy(identity = usageReference.identity.copy(elementId = "Other:ComponentUsage"),
          componentMappings = Present(Vector.empty), traceability = Present(traces("model:app", roles)))
        val app = originalapp.copy(documents = originalapp.documents.updated(0, originalapp.documents.head.copy(
          componentUsageCapabilities = originalapp.documents.head.componentUsageCapabilities :+ unrelated)))
        val input = assemble(base.packages, base.provenance.scope, Some(app))
        val query = session(input)
        When("only the selected ComponentUsage path and its compatible provider are diagnosed")
        val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(usageReference)))
        Then("unrelated facts cannot cover the two missing roles and unresolved occurrences add only their own boundary")
        diagnosis.gaps.map(_.productPrefix) shouldBe Vector("MissingSpecification", "MissingEvidence", "MissingRequiredComponentCapability")
        val coverage = diagnosis.gaps.head.asInstanceOf[CapabilityGap.MissingSpecification].coverage
        coverage.specification shouldBe Vector.empty
        coverage.evidence shouldBe Vector.empty
        coverage.owners.map(_.owner) should not contain TraceabilityOwner.ComponentUsage(query.catalog.usages(1).key)
        coverage.owners.map(_.owner) should not contain TraceabilityOwner.Declaration(keys(input, Direction.Provided)(1))
        coverage.owners.map(_.owner) should not contain TraceabilityOwner.Declaration(keys(input, Direction.Provided)(2))
        coverage.boundary shouldBe CoverageBoundary.ProviderPath
        diagnosis.usages.head.paths.drop(1).map(_.required) shouldBe Vector(Vector.empty, Vector.empty)
        Given("a real Required branch with no compatible provider and original owner Presence")
        val noinput = scenario(Vector.empty, requiredroles = Vector.empty, usageroles = Vector.empty)
        val noquery = session(noinput)
        When("only known Required owners are assessed")
        val direct = CapabilityQueryFixtures.value(noquery.diagnoseCapabilityDemand(DemandSelection.Required(keys(noinput, Direction.Required).head)))
        val known = direct.gaps(1).asInstanceOf[CapabilityGap.MissingSpecification].coverage
        Then("RequiredPathOnly contains exactly its declaration and definition, with no unknown-provider owner")
        known.boundary shouldBe CoverageBoundary.RequiredPathOnly
        known.owners.map(_.owner) shouldBe Vector(TraceabilityOwner.Declaration(direct.required.head.required.key),
          TraceabilityOwner.CapabilityDefinition(direct.required.head.required.key.context, reference))
      }
      "C11Q23 retain stable seven-gap boundaries, repeated genuine branches and four distinct Unknown observations" in {
        Given("three explicit Required identities with absent, wrong-version and compatible empty providers plus Local/External unresolved mappings")
        val missing = reference.copy(identity = reference.identity.copy(elementId = "Missing:providers"))
        val versioned = reference.copy(identity = reference.identity.copy(elementId = "Wrong:version"))
        val wrong = versioned.copy(contractVersion = "wrong:v7")
        val orphan = reference.copy(identity = reference.identity.copy(elementId = "Missing:required"))
        val selectedscope = scope(Vector(reference, missing, versioned, wrong, orphan).map(definition(_)))
        val packages = Vector(packageFor("NoProvider", "no-provider", selectedscope, missing, Vector(Direction.Required), 0, Vector.empty),
          packageFor("VersionRequired", "version-required", selectedscope, versioned, Vector(Direction.Required), 0, Vector.empty),
          packageFor("RealRequired", "real-required", selectedscope, reference, Vector(Direction.Required), 0, Vector.empty),
          packageFor("WrongProvider", "wrong-provider", selectedscope, wrong, Vector(Direction.Provided), 0, Vector.empty),
          packageFor("EmptyProvider", "empty-provider", selectedscope, reference, Vector(Direction.Provided), 0, Vector.empty),
          packageFor("RealProvider", "real-provider", selectedscope, reference, Vector(Direction.Provided), 3, Vector.empty))
        val maps = Vector(mapping(missing), mapping(versioned), mapping(), mapping(), mapping(orphan), mapping(orphan, ReferenceBoundary.External))
        val input = assemble(packages, selectedscope, Some(usages(maps, selectedroles = Vector.empty)))
        val query = session(input)
        When("the original mapping and provider order drives diagnosis")
        val diagnosis = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(usageReference)))
        Then("provider absence/version comes first and each provider preserves realization then Specification then Evidence order")
        diagnosis.gaps.map(_.productPrefix) shouldBe Vector("NoProvider", "MissingSpecification", "MissingEvidence",
          "IncompatibleProviderVersion", "MissingSpecification", "MissingEvidence",
          "MissingRealization", "MissingSpecification", "MissingEvidence", "MissingSpecification", "MissingEvidence",
          "MissingRealization", "MissingSpecification", "MissingEvidence", "MissingSpecification", "MissingEvidence", "MissingRequiredComponentCapability")
        diagnosis.usages.head.paths.map(_.path.mapping) shouldBe maps
        diagnosis.usages.head.paths(2) shouldBe diagnosis.usages.head.paths(3)
        diagnosis.gaps.slice(6, 11) shouldBe diagnosis.gaps.slice(11, 16)
        val app = diagnosis.usages.head
        val absentpath = DemandPath(Some(app.usage), Some(app.paths.head.path), app.paths.head.required.head.required, None)
        diagnosis.gaps.head shouldBe CapabilityGap.NoProvider(query.scope, absentpath, app.paths.head.required.head.providers)
        val versionpath = DemandPath(Some(app.usage), Some(app.paths(1).path), app.paths(1).required.head.required, None)
        diagnosis.gaps(3) shouldBe CapabilityGap.IncompatibleProviderVersion(query.scope, versionpath, app.paths(1).required.head.providers)
        val provider = app.paths(2).required.head.providers.matches(1).provider
        provider.realizations.map(_.edge) shouldBe packages.last.resources.head.realizations.map(_.edge)
        provider.realizations.map(_.observations.availability) shouldBe Vector.fill(3)(AvailabilityObservation.Unknown("No runtime observation supplied."))
        provider.realizations.map(_.observations.authorization) shouldBe Vector.fill(3)(AuthorizationObservation.Unknown("No runtime observation supplied."))
        provider.realizations.map(_.observations.guard) shouldBe Vector.fill(3)(GuardObservation.Unknown("No runtime observation supplied."))
        provider.realizations.map(_.observations.executionResult) shouldBe Vector.fill(3)(ExecutionResultObservation.Unknown("No runtime observation supplied."))
        diagnosis.gaps.last shouldBe CapabilityGap.MissingRequiredComponentCapability(query.scope, app.usage, app.paths(4).path)
        app.paths(5).required shouldBe Vector.empty
        Given("the same original admitted family and an absent exact ComponentUsage reference")
        val absentapp = usageReference.copy(contractVersion = "absent:all-seven")
        When("the known missing ComponentUsage boundary is selected")
        val missingapp = CapabilityQueryFixtures.value(query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(absentapp)))
        Then("the seventh closed variant retains original family/scope without downstream facts")
        missingapp.gaps shouldBe Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(query.scope, absentapp,
          MissingComponentUsageReason.AbsentSelectedReference, input.usage))
      }
    }
  }
}
