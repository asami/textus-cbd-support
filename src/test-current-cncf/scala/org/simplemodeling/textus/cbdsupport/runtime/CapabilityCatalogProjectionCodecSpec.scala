package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.jawn.JawnParser
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityCatalogFixtures.*
import CapabilityIngestionFixtures.{right, left}
import CapabilityComponentUsageJsonPart._items

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityCatalogProjectionCodecSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _codec = CapabilityCatalogProjectionCodec
  private val _parser = JawnParser(allowDuplicateKeys = false)
  "Portable catalog semantics" should {
    "C11C17 retain equal Usage identities in distinct owners and explicit per-package absence" in {
      Given("three explicit package contexts and repeated Usage identities with no context-free Local Usage dependency")
      val packages = Vector(packageprojection("Payments", "first"), packageprojection("Orders", "second"), packageprojection("Archive", "third"))
      val projection = usage.copy(documents = usage.documents.map(doc => doc.copy(componentUsageCapabilities =
        doc.componentUsageCapabilities.map(_.copy(dependencies = Present(Vector.empty))))))
      val input = withUsagePackages(assemble(packages, usage = None), Vector(Some(projection), Some(projection), None))
      When("all selected package families roundtrip through the semantic catalog codec")
      val result = right(_codec.decode(right(_codec.encode(input, selection(input))), selection(input)))
      Then("owner/package/resource/source keys retain every witness and unavailable differs from acquired empty content")
      result.projection shouldBe input
      result.usages.size shouldBe 4
      result.usages.map(_.key).distinct.size shouldBe 4
      result.usages.map(_.key.component) shouldBe Vector.fill(2)(packages(0).input.component) ++ Vector.fill(2)(packages(1).input.component)
      result.usages.map(_.input) shouldBe Vector.fill(2)(packages(0).input) ++ Vector.fill(2)(packages(1).input)
      result.usages.map(_.key.resourcePath).distinct shouldBe Vector("meta/component-usage-capability/usage.json")
      result.projection.usage.last.family shouldBe ComponentUsageFamily.Unavailable(unavailable)
      result.projection.provenance.packages.map(_.usage.isDefined) shouldBe Vector(true, true, false)
      result.usages.map(_.definition.component) shouldBe result.usages.map(_.key.component.componentId)
    }
    "roundtrip complete qualified values and reject ambiguous ownership" which {
      "C11C09 preserve literal Unicode, full contexts, Presence and Source/Origin without native objects" in {
        Given("a complete composed/decomposed Unicode, case/delimiter carrier with two Components and two ComponentUsages")
        val sample = Sample("É|É::Case/支払", "contract:Case|v7", 91, 2, 2, 3, false, true)
        val input = generated(sample)
        val selected = selection(input)
        When("the semantic projection encodes and decodes with the independently supplied selection")
        val encoded = right(_codec.encode(input, selected))
        val result = right(_codec.decode(encoded, selected))
        val json = _parser.parse(encoded).toOption.get
        Then("complete typed equality and concrete cardinalities/ownership/source/origin survive in embedded objects only")
        result.projection shouldBe input
        result.capabilities.size shouldBe 4
        result.capabilities.map(_.key.context) shouldBe input.packages.flatMap(p => Vector.fill(2)(contextkey(p, p.resources.head)))
        result.usages.size shouldBe 2
        result.demandTrace.usagePaths.size shouldBe 6
        result.capabilities.head.definition.identity shouldBe ModelElementId("Model:É|É::Case/支払", "Capability:É|É::Case/支払")
        result.capabilities.head.definition.summary shouldBe Absent(AbsenceReason.NotDeclared, "Absent:É|É::Case/支払")
        result.capabilities.head.realizations.head.edge.origin shouldBe Derived("edge:É|É::Case/支払", Vector(result.capabilities.head.definition.source))
        result.usages.head.documentSource.sourceRevision shouldBe "app-header:91"
        result.usages.head.definition.source.sourceRevision shouldBe "app-definition:91"
        result.usages.head.definition.discoveryReferences shouldBe Absent(AbsenceReason.NotDeclared, "Discovery absent.")
        json.asObject.get.keys.toVector shouldBe Vector("schemaVersion", "snapshotReference", "provenance", "packages", "usage", "useCase")
        json.hcursor.downField("packages").downArray.downField("resources").downArray.downField("projection").focus.get.isObject shouldBe true
        encoded should not include "nativeObservations"
        encoded should not include "executionResult"
        encoded should not include "snapshotSource"
        encoded should not include "selector"
      }
      "C11C10 reject repeated owner keys and ambiguous exact candidates without a winner" in {
        Given("table-selected semantic carriers repeating resource, declaration, ComponentUsage, definition or target candidates")
        val baseline = complete
        val resource = baseline.packages.head.resources.head
        val appprojection = baseline.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].projection
        val repeatedapp = appprojection.copy(documents = appprojection.documents.updated(0, appprojection.documents.head.copy(
          componentUsageCapabilities = appprojection.documents.head.componentUsageCapabilities ++ appprojection.documents.head.componentUsageCapabilities)))
        val repeatedresource = baseline.copy(packages = baseline.packages.updated(0, baseline.packages.head.copy(resources = Vector(resource, resource))))
        val repeateddeclaration = withresource(baseline, r => r.copy(projection = r.projection.copy(declarations = r.projection.declarations :+ r.projection.declarations.head)))
        val repeateddefinition = withresource(baseline, r => r.copy(resolvedDefinitions = r.resolvedDefinitions :+ r.resolvedDefinitions.head))
        val repeatedtarget = baseline.copy(packages = baseline.packages.updated(0, baseline.packages.head.copy(targetContracts = baseline.packages.head.targetContracts :+ baseline.packages.head.targetContracts.head)))
        val competing = baseline.copy(provenance = baseline.provenance.copy(scope = baseline.provenance.scope.copy(capabilities = scope.capabilities :+ definition.copy(name = "Competing exact candidate"))))
        val cases = Vector(
          ("resource", repeatedresource, "/meta/component-capability/Payments.json/source", resource.projection.source),
          ("declaration", repeateddeclaration, "/packages/0/resources/0/projection/declarations/2", resource.projection.declarations.head.source),
          ("ComponentUsage", withusage(baseline, repeatedapp), "/usage/0/family/projection/documents/0/componentUsageCapabilities/1", appprojection.documents.head.componentUsageCapabilities.head.source),
          ("definition", repeateddefinition, "/packages/0/resources/0/resolvedDefinitions/1", definition.source),
          ("target", repeatedtarget, "/packages/0/targetContracts/3", baseline.packages.head.targetContracts.head.source),
          ("scope candidate", competing, "/packages/0/resources/0/projection/scope/capabilities/1", definition.source))
        When("each independently selected invalid carrier reaches the shared semantic binder")
        val outcomes = cases.map { (label, input, location, source) =>
          Given("the repeated " + label + " candidate and its original provenance")
          When("the catalog validator binds that complete carrier")
          val rejected = left(_codec.validate(input, selection(input)))
          Then("Ambiguous identifies the exact repeated location/source without accepting any winner")
          rejected.kind shouldBe DiagnosticKind.Ambiguous
          rejected.location shouldBe location
          rejected.source shouldBe Some(source)
          rejected.scopeId shouldBe Some(scope.scopeId)
          rejected
        }
        Then("all six duplicate cases reject with their distinct field locations")
        outcomes.size shouldBe 6
        outcomes.map(_.location).distinct.size shouldBe 6
      }
      "C11C11 reject dangling Local and owner references while retaining explicit External facts" in {
        Given("dangling Local model/declaration facts and a conflicting realization-fact owner, alongside valid External evidence")
        val baseline = complete
        val missingscope = scope.copy(modelFacts = scope.modelFacts.filterNot(_.identity.elementId == "spec"))
        val missingmodel = baseline.copy(provenance = baseline.provenance.copy(scope = missingscope))
        val missingdeclaration = withresource(baseline, r => r.copy(projection = r.projection.copy(declarations = r.projection.declarations.updated(1,
          r.projection.declarations(1).copy(capability = CapabilityReference(ModelElementId("absent:Local", "missing"), "missing:v1"))))))
        val missingowner = withfact(baseline, f => f.copy(edge = f.edge.copy(declarationKey = f.edge.declarationKey.copy(direction = Direction.Required))))
        val cases = Vector(("Local model", missingmodel, "/target/identity"), ("Local declaration", missingdeclaration, "/declarations/1/capability/identity"),
          ("fact owner", missingowner, "/realizations/0/edge/declarationKey"))
        When("each dangling declared link is validated")
        val outcomes = cases.map { (label, input, field) =>
          Given("the dangling " + label + " reference in an otherwise complete semantic carrier")
          When("the finite semantic binder validates its declared links")
          val rejected = left(_codec.validate(input, selection(input)))
          Then("Malformed retains the original scope/source and distinguishing reference field")
          rejected.kind shouldBe DiagnosticKind.Malformed
          rejected.location should endWith (field)
          rejected.scopeId shouldBe Some(scope.scopeId)
          rejected.source should not be empty
          rejected
        }
        Then("all three dangling cases reject independently")
        outcomes.size shouldBe 3
        Given("an explicit External Specification target with no Local candidate and its original source/origin")
        val externaltrace = _items(definition.traceability).head.copy(target = ModelReference(ModelElementId("external:model", "outside-spec"), ReferenceBoundary.External))
        val externaldefinition = definition.copy(traceability = Present(Vector(externaltrace, _items(definition.traceability)(1))))
        val externalscope = scope.copy(capabilities = Vector(externaldefinition))
        val external = assemble(Vector(packageprojection("Payments", "first", externalscope, externaldefinition)), externalscope)
        When("the valid external carrier encodes and decodes")
        val retained = right(_codec.decode(right(_codec.encode(external, selection(external))), selection(external)))
        Then("External remains ordered attributed evidence and missing Required alone is a valid unresolved path")
        retained.traceability.head.edge shouldBe externaltrace
        retained.projection.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].unresolvedExternal shouldBe external.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].unresolvedExternal
        catalog(multiplepaths).demandTrace.usagePaths(2).requiredEndpoint shouldBe RequiredEndpoint.Unresolved(CapabilityReference(ModelElementId("model:component", "no-required"), "no-required:v13"), ReferenceBoundary.Local)
      }
      "C11C12 preserve owner/target mismatch kinds, field paths and independent Stale provenance" in {
        Given("explicit owner, target identity/Component/kind/contract/ABI mismatches and ambiguous target candidates")
        val baseline = complete
        val cases = Vector(
          ("declared owner", withdeclarededge(baseline, e => e.copy(declarationKey = e.declarationKey.copy(direction = Direction.Required))), DiagnosticKind.Incompatible, "/projection/declarations/0/realizations/value/0/declarationKey"),
          ("fact owner", withfact(baseline, f => f.copy(edge = f.edge.copy(declarationKey = f.edge.declarationKey.copy(direction = Direction.Required)))), DiagnosticKind.Malformed, "/realizations/0/edge/declarationKey"),
          ("fact identity", withfact(baseline, f => f.copy(target = f.target.copy(identity = f.target.identity.copy(elementId = "missing")))), DiagnosticKind.Malformed, "/realizations/0/target/identity"),
          ("fact Component", withfact(baseline, f => f.copy(target = f.target.copy(component = ComponentId("org.example.Other")))), DiagnosticKind.Malformed, "/realizations/0/target/component"),
          ("fact kind", withfact(baseline, f => f.copy(target = f.target.copy(targetKind = TargetKind.Workflow))), DiagnosticKind.Incompatible, "/realizations/0/target/targetKind"),
          ("fact contract", withfact(baseline, f => f.copy(target = f.target.copy(contractVersion = "different:v7"))), DiagnosticKind.Incompatible, "/realizations/0/target/contractVersion"),
          ("fact ABI", withfact(baseline, f => f.copy(target = f.target.copy(generatedAbiVersion = "different:abi"))), DiagnosticKind.Incompatible, "/realizations/0/target/generatedAbiVersion"),
          ("declared identity", withdeclarededge(baseline, e => e.copy(target = e.target.copy(identity = e.target.identity.copy(elementId = "absent")))), DiagnosticKind.Malformed, "/projection/declarations/0/realizations/value/0/target"),
          ("declared Component", withdeclarededge(baseline, e => e.copy(targetComponent = ComponentId("org.example.Other"))), DiagnosticKind.Malformed, "/projection/declarations/0/realizations/value/0/target"),
          ("declared kind", withdeclarededge(baseline, e => e.copy(targetKind = TargetKind.Workflow)), DiagnosticKind.Incompatible, "/projection/declarations/0/realizations/value/0/targetKind"),
          ("declared contract", withdeclarededge(baseline, e => e.copy(targetContractVersion = "different:v7")), DiagnosticKind.Incompatible, "/projection/declarations/0/realizations/value/0/targetContractVersion"),
          ("declared ABI", withdeclarededge(baseline, e => e.copy(generatedAbiVersion = "different:abi")), DiagnosticKind.Incompatible, "/projection/declarations/0/realizations/value/0/generatedAbiVersion"))
        When("public Component validation/admission and the catalog fact binder handle each exact mismatch")
        val outcomes = cases.map { (label, input, kind, field) =>
          Given("the explicit " + label + " mismatch and original resource owner")
          When("the complete carrier reaches the public-bound validator")
          val rejected = left(_codec.validate(input, selection(input)))
          Then("the original diagnostic kind and distinguishing field retain their owner provenance")
          rejected.kind shouldBe kind
          rejected.location should endWith (field)
          rejected.scopeId shouldBe Some(scope.scopeId)
          rejected.component shouldBe Some(baseline.packages.head.input.component.componentId)
          rejected.source shouldBe Some(baseline.packages.head.resources.head.realizations.head.edge.source)
          rejected
        }
        Then("all twelve ownership/target mismatches remain distinct rejections")
        outcomes.size shouldBe 12
        Given("independently supplied selection provenance with a different opaque scope source revision")
        val selected = selection(baseline).copy(provenance = baseline.provenance.copy(scope = scope.copy(source = scope.source.copy(sourceRevision = "different:opaque-source"))))
        When("the valid projection is compared with that independent selection")
        val stale = left(_codec.validate(baseline, selected))
        Then("Stale retains the original observed source and exact revision field instead of relabeling it")
        stale.kind shouldBe DiagnosticKind.Stale
        stale.location shouldBe "/scope/source/sourceRevision"
        stale.source shouldBe Some(scope.source)
        Given("duplicate exact target contracts in the finite package")
        val ambiguous = baseline.copy(packages = baseline.packages.updated(0, baseline.packages.head.copy(targetContracts = baseline.packages.head.targetContracts :+ baseline.packages.head.targetContracts.head)))
        When("the catalog binds explicit target candidates")
        val duplicate = left(_codec.validate(ambiguous, selection(ambiguous)))
        Then("Ambiguous names the repeated contract without choosing a target")
        duplicate.kind shouldBe DiagnosticKind.Ambiguous
        duplicate.location shouldBe "/packages/0/targetContracts/3"
      }
      "C11C13 preserve every independent opaque version and reject malformed or unsupported wire fields" in {
        Given("independent schema, record, source, producer, component, package, definition, target, ABI and ComponentUsage versions")
        val input = generated(Sample("Case::É|É", "definition:opaque/v29", 177, 1, 1, 3, true, false))
        val selected = selection(input)
        When("the complete portable projection roundtrips")
        val encoded = right(_codec.encode(input, selected))
        val result = right(_codec.decode(encoded, selected))
        Then("each domain remains literal and independent")
        result.projection shouldBe input
        result.projection.schemaVersion shouldBe 1
        result.projection.snapshotReference.value.recordRevision.value shouldBe 177L
        result.resources.head.resource.projection.component.version shouldBe "4.6.0"
        result.resources.head.resource.projection.packageCoordinate.version shouldBe "package:Case::É|É:177"
        result.capabilities.head.definition.contractVersion shouldBe "definition:opaque/v29"
        result.capabilities.head.realizations.head.target.contractVersion shouldBe "target:OPERATION:190"
        result.capabilities.head.realizations.head.target.generatedAbiVersion shouldBe "native-abi:OPERATION:Case::É|É"
        result.usages.head.definition.contractVersion shouldBe "app:definition:opaque/v29"
        Given("closed internal JSON keys/tags and strictly integral positive Long management revisions")
        val json = _parser.parse(encoded).toOption.get
        val cases = Vector(
          ("unknown key", Json.fromJsonObject(json.asObject.get.add("unknown", Json.True)).noSpaces, DiagnosticKind.Malformed, "/unknown"),
          ("missing schema", Json.fromJsonObject(json.asObject.get.remove("schemaVersion")).noSpaces, DiagnosticKind.Malformed, "/schemaVersion"),
          ("null reference", update(json, Vector("snapshotReference"), Json.Null).noSpaces, DiagnosticKind.Malformed, "/snapshotReference"),
          ("fractional revision", update(json, Vector("snapshotReference", "recordRevision"), Json.fromDoubleOrNull(1.5)).noSpaces, DiagnosticKind.Malformed, "/snapshotReference/recordRevision"),
          ("zero revision", update(json, Vector("snapshotReference", "recordRevision"), Json.fromLong(0)).noSpaces, DiagnosticKind.Malformed, "/snapshotReference/recordRevision"),
          ("unsupported schema", update(json, Vector("schemaVersion"), Json.fromInt(2)).noSpaces, DiagnosticKind.Unsupported, "/schemaVersion"),
          ("unsupported family", update(json, Vector("useCase", "family"), Json.fromString("FUTURE")).noSpaces, DiagnosticKind.Unsupported, "/useCase/family"),
          ("duplicate key", encoded.dropRight(1) + ",\"schemaVersion\":1}", DiagnosticKind.Malformed, "/"))
        When("each malformed or future wire value reaches the strict closed decoder")
        val outcomes = cases.map { (label, wire, kind, path) =>
          Given("the " + label + " catalog wire input")
          When("the codec decodes with an independently supplied valid selection")
          val rejected = left(_codec.decode(wire, selected))
          Then("the precise malformed/unsupported distinction and location are preserved")
          rejected.kind shouldBe kind
          rejected.location shouldBe path
          rejected
        }
        Then("all eight closed-wire cases reject and no implicit schema or version default appears")
        outcomes.size shouldBe 8
        left(_codec.validate(input.copy(schemaVersion = 2), selected)).kind shouldBe DiagnosticKind.Unsupported
      }
      "C11C14 retain exactly the original directed compatibility fact and owner" in {
        Given("two exact reference definitions and one original directed compatibility edge with Source and Derived origin")
        val input = compatible
        val original = input.packages.head.resources.head.projection.compatibility
        When("catalog semantics bind and roundtrip")
        val result = right(_codec.decode(right(_codec.encode(input, selection(input))), selection(input)))
        Then("precisely the one original direction and its context/source/origin survive")
        result.projection shouldBe input
        result.compatibility shouldBe Vector(CompatibilityRecord(result.resources.head.key, _items(original).head))
        result.compatibility.head.edge.requiredVersion shouldBe "contract:opaque-earlier"
        result.compatibility.head.edge.providedVersion shouldBe "cap-v7"
        result.compatibility.head.edge.source shouldBe Source("model:component", "edges/compatibility.cml", "compatibility:r4", Some(19))
        result.compatibility.head.edge.origin shouldBe Derived("compatibility:declared", Vector(definition.source))
        result.resources.head.resource.projection.compatibility shouldBe original
        result.compatibility.size shouldBe 1
      }
      "C11C16 actively roundtrip generated semantic carriers with at least one hundred successes and zero discards" in {
        Given("valid-by-construction carriers varying qualified Unicode/case/delimiters, independent versions/sources/origins, Presence, Components, ComponentUsages, mappings and one to three target kinds")
        val samples = for {
          token <- Gen.oneOf("É|Case::A", "É|case::a", "支払/契約", "Cap:cap|#")
          contract <- Gen.oneOf("contract:opaque/7", "VERSION|lower", "version:É")
          revision <- Gen.choose(1, 9999)
          componentcount <- Gen.choose(1, 3)
          usagecount <- Gen.choose(1, 3)
          targetcount <- Gen.choose(1, 3)
          present <- Gen.oneOf(true, false)
          repeated <- Gen.oneOf(true, false)
        } yield Sample(token, contract, revision, componentcount, usagecount, targetcount, present, repeated)
        When("ScalaCheck executes the full semantic roundtrip with a minimum of 100 successes and explicit zero-discard assertions")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { (sample: Sample) =>
          Given("one generated complete valid semantic carrier with its independently selected finite provenance")
          val input = generated(sample)
          val selected = selection(input)
          val expectedkeys = input.packages.flatMap(p => p.resources.flatMap(r => r.projection.declarations.map(declarationkey(p, r, _))))
          val expectedrequired = expectedkeys.filter(_.declaration.direction == Direction.Required)
          When("the codec encodes and decodes the entire semantic value")
          val result = right(_codec.decode(right(_codec.encode(input, selected)), selected))
          Then("complete projection equality and independently expected owner/path/cardinality semantics survive")
          result.projection shouldBe input
          result.capabilities.map(_.key) shouldBe expectedkeys
          result.capabilities.size shouldBe sample.componentcount * 2
          result.capabilities.map(_.realizations.size) shouldBe Vector.fill(sample.componentcount)(Vector(sample.targetcount, 0)).flatten
          result.capabilities.flatMap(_.realizations).map(_.owner) shouldBe expectedkeys.filter(_.declaration.direction == Direction.Provided).flatMap(k => Vector.fill(sample.targetcount)(k))
          result.usages.size shouldBe sample.usagecount
          result.demandTrace.usagePaths.size shouldBe sample.usagecount * (if (sample.repeated) 3 else 2)
          result.demandTrace.usagePaths.filter(_.mapping.boundary == ReferenceBoundary.Local).map(_.requiredEndpoint) shouldBe
            Vector.fill(sample.usagecount * (if (sample.repeated) 2 else 1))(RequiredEndpoint.Resolved(expectedrequired))
          result.demandTrace.usagePaths.map(_.mapping) shouldBe result.usages.flatMap(a => _items(a.definition.componentMappings))
          result.capabilities.head.definition.source.sourceRevision shouldBe "definition:" + sample.revision
          result.usages.head.documentSource.sourceRevision shouldBe "app-header:" + sample.revision
          result.usages.head.definition.source.sourceRevision shouldBe "app-definition:" + sample.revision
          result.demandTrace.useCase shouldBe usecase
          true
        })
        Then("at least 100 applicable cases succeed with exactly zero discards and no filtered or ignored samples")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
        info(s"C11C16 generated semantic roundtrips: succeeded=${checked.succeeded}, discarded=${checked.discarded}")
      }
    }
  }
}
