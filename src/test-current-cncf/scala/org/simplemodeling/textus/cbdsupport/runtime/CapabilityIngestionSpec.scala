package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Files
import io.circe.Json
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityMetadataProvider.*
import ComponentCapabilityPackageSource.*
import CapabilityIngestionModel.*
import CapabilityIngestionFixtures.*

/*
 * Public Factory integration of synthetic kernel inputs. Normal producer
 * interoperability is a distinct mandatory later same-Step obligation.
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityIngestionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _ingestion = CapabilityIngestion
  private def _resources(pkg: PackageSelection)(change: Vector[ResourceExpectation] => Vector[ResourceExpectation]): PackageSelection =
    pkg.copy(expected = pkg.expected.copy(resources = change(pkg.expected.resources)))
  "Public complete Component intake" should {
    "C11K01 retain original public Factory native objects through the CBD snapshot" in {
      Given("C11I-D03/10: a canonical synthetic package and independently supplied actual Operation metadata")
      withDirectory { root =>
        val input = native()
        val p = projection(input)
        val pkg = selected(root, Vector("z.json" -> p), input = input)
        val provider = pkg.provider.get.asInstanceOf[CountingProvider]
        val selectedinput = selection(Vector(pkg))
        When("CBD acquires only through public Factory and transitions the original candidate")
        val candidate = right(_ingestion.acquire(selectedinput))
        val acquiredtarget = candidate.components.head.entries.head.realizations.head.target
        val next = right(_ingestion.transition(State.empty, attempt("attempt:original", selectedinput, snapshot("snapshot:original")), Right(candidate)))
        Then("original Discovered, Entry, ResolvedTarget, NativeInput, binding and native contract objects remain retained, with one provider read")
        val stored = next.state.snapshots.head
        stored.candidate should be theSameInstanceAs candidate
        stored.components.head should be theSameInstanceAs candidate.components.head
        val discovered = stored.components.head
        discovered.nativeInput should be theSameInstanceAs input
        discovered.entries.head should be theSameInstanceAs candidate.components.head.entries.head
        val target = discovered.entries.head.realizations.head
        target should be theSameInstanceAs candidate.components.head.entries.head.realizations.head
        target.binding should be theSameInstanceAs input.bindings.head
        target.snapshot should be theSameInstanceAs input.acquisition.asInstanceOf[NativeAcquisition.Acquired].snapshots.head
        target.contract match {
          case NativeContract.Operation(definition) => definition should be theSameInstanceAs target.snapshot.operations.head
          case _ => fail("Expected independent Operation metadata.")
        }
        target.target should be theSameInstanceAs acquiredtarget
        target.target shouldBe input.bindings.head.target
        discovered.entries.head.admitted.projection should be theSameInstanceAs discovered.entries.head.loaded.projection
        provider.reads shouldBe 1
        target.realization.edge.source.sourceRevision shouldBe "edge-r4"
        target.snapshot.source.sourceRevision shouldBe "native-r12"
        discovered.entries.head.loaded.projection.source.sourceRevision shouldBe "header-r2"
        target.target.contractVersion shouldBe "target-v3"
      }
    }
    "C11K02 require complete literal resource coverage and exact author order" in {
      Given("C11I-D04: two actual canonical resources in reverse filename order, with independent source paths")
      withDirectory { root =>
        val values = Vector("z.json" -> emptyProjection("first"), "a.json" -> emptyProjection("second"))
        val pkg = selected(root, values)
        val missing = _resources(pkg)(_.take(1))
        val extra = _resources(pkg)(xs => xs :+ xs.head.copy(resourcePath = metadataDirectory + "/extra.json"))
        val reordered = _resources(pkg)(_.reverse)
        When("the complete selection and incomplete or reordered declarations are acquired")
        val original = right(_ingestion.acquire(selection(Vector(pkg))))
        val errors = Vector(missing, extra, reordered).map(x => left(_ingestion.acquire(selection(Vector(x)))))
        Then("coverage omissions are Malformed, complete reordering is Incompatible, and resource names do not supply Source")
        errors.map(_.kind) shouldBe Vector(DiagnosticKind.Malformed, DiagnosticKind.Malformed, DiagnosticKind.Incompatible)
        original.components.head.entries.map(_.loaded.resourcePath) shouldBe Vector(metadataDirectory + "/z.json", metadataDirectory + "/a.json")
        original.components.head.entries.map(_.loaded.projection.source.path) shouldBe Vector("models/first.cml", "models/second.cml")
      }
    }
    "C11K03 compare independent coordinates, finite scope and producer domains" in {
      Given("C11I-D02/04: declared Component/package/scope/producer differences, unsupported headers and malformed finite facts")
      withDirectory { root =>
        val observedscope = scope.copy(modelFacts = Vector(ModelFact(ModelElementId("model:facts", "goal"), ModelKind.Goal, Source("model:facts", "facts/goal.cml", "fact-r1", None))))
        val input = native(selectedScope = observedscope)
        val pkg = selected(root, Vector("first.json" -> projection(input)), observedscope, input)
        val base = selection(Vector(pkg), observedscope)
        val changedcomponent = pkg.copy(expected = pkg.expected.copy(input = pkg.expected.input.copy(component = component.copy(version = "3.8.0-SNAPSHOT"))))
        val changedpackage = pkg.copy(expected = pkg.expected.copy(input = pkg.expected.input.copy(packageCoordinate = packageCoordinate.copy(version = "package-v22"))))
        val changedproducer = _resources(pkg)(xs => xs.map(x => x.copy(producer = x.producer.copy(version = "other-producer"))))
        val scopes = Vector(observedscope.copy(scopeId = "scope:other"),
          observedscope.copy(modelFacts = observedscope.modelFacts.map(_.copy(kind = ModelKind.Component))))
        val incompatible = Vector(base.copy(packages = Vector(changedcomponent)), base.copy(packages = Vector(changedpackage)), base.copy(packages = Vector(changedproducer)),
          base.copy(scope = scopes.head), base.copy(scope = scopes(1)))
        val unsupported = Vector(_resources(pkg)(_.map(_.copy(schemaVersion = 2))), _resources(pkg)(_.map(_.copy(generatedAbiVersion = "future:abi"))))
        When("each explicit declaration is compared with the unchanged package/native observation")
        val errors = incompatible.map(x => left(_ingestion.acquire(x)))
        val headers = unsupported.map(x => left(_ingestion.acquire(base.copy(packages = Vector(x)))))
        val malformed = left(_ingestion.acquire(base.copy(scope = observedscope.copy(modelFacts = Vector(null)))))
        val incomplete = left(_ingestion.acquire(base.copy(scope = observedscope.copy(modelFacts = Vector.empty))))
        Then("independent semantic mismatches are Incompatible, unsupported contracts stay Unsupported and missing/invalid facts are Malformed")
        errors.map(_.kind) shouldBe Vector.fill(5)(DiagnosticKind.Incompatible)
        headers.map(_.kind) shouldBe Vector.fill(2)(DiagnosticKind.Unsupported)
        malformed.kind shouldBe DiagnosticKind.Malformed
        incomplete.kind shouldBe DiagnosticKind.Malformed
      }
    }
    "C11K04 classify opaque Source revision differences separately from path and line" in {
      Given("C11I-D04: original header/definition/edge/native revisions and independently changed expected resource/scope Sources")
      withDirectory { root =>
        val pkg = selected(root, Vector("first.json" -> projection()))
        val base = selection(Vector(pkg))
        def _expected_source_(source: Source): Selection =
          base.copy(packages = Vector(_resources(pkg)(xs => xs.map(x => x.copy(source = source)))))
        val observedsource = pkg.expected.resources.head.source
        val variants = Vector(_expected_source_(observedsource.copy(sourceRevision = "old")),
          base.copy(scope = scope.copy(source = scope.source.copy(sourceRevision = "old-scope"))),
          _expected_source_(observedsource.copy(line = Some(99))), _expected_source_(observedsource.copy(path = "models/other.cml")))
        When("the independent expected Sources are compared with acquired literal headers")
        val errors = variants.map(x => left(_ingestion.acquire(x)))
        val original = right(_ingestion.acquire(base))
        Then("revision changes are Stale, path/line changes Incompatible, and authored definition/native domains survive")
        errors.map(_.kind) shouldBe Vector(DiagnosticKind.Stale, DiagnosticKind.Stale, DiagnosticKind.Incompatible, DiagnosticKind.Incompatible)
        errors.head.source shouldBe Some(pkg.expected.resources.head.source)
        original.components.head.entries.head.loaded.projection.capabilities.head.source.sourceRevision shouldBe "definition-r9"
        original.components.head.entries.head.realizations.head.target.source.line shouldBe Some(11)
      }
    }
    "C11K05 reject complete repeated contexts and global source snapshot keys" in {
      Given("C11I-D04: equal selected contexts and independent locations observing one identical snapshot with different lines")
      withDirectory { first => withDirectory { second =>
        val p = projection()
        val one = selected(first, Vector("first.json" -> p))
        val two = selected(second, Vector("second.json" -> p.copy(source = p.source.copy(line = Some(99)))))
        val provider = one.provider.get.asInstanceOf[CountingProvider]
        When("duplicate selections, global snapshots and a repeated original public declaration are acquired")
        val selectederror = left(_ingestion.acquire(selection(Vector(one, one))))
        val readsafterduplicate = provider.reads
        val snapshoterror = left(_ingestion.acquire(selection(Vector(one, two))))
        val encoded = right(ComponentCapabilityProjectionCodec.encode(p))
        val parsed = io.circe.jawn.JawnParser(allowDuplicateKeys = false).parse(encoded).toOption.get
        val declaration = parsed.hcursor.downField("declarations").focus.get.asArray.get.head
        write(first, metadataDirectory + "/first.json", parsed.mapObject(_.add("declarations", Json.arr(declaration, declaration))).noSpaces)
        val publicerror = left(_ingestion.acquire(selection(Vector(one))))
        Then("all duplicates are Ambiguous with no winner or partial result and public declaration diagnosis remains authoritative")
        Vector(selectederror, snapshoterror, publicerror).map(_.kind) shouldBe Vector.fill(3)(DiagnosticKind.Ambiguous)
        readsafterduplicate shouldBe 0
        snapshoterror.source shouldBe Some(p.source.copy(line = Some(99)))
        snapshoterror.location shouldBe "/meta/component-capability/second.json/source"
        publicerror.location shouldBe "/meta/component-capability/first.json/declarations/1"
      } }
    }
    "C11K06 retain distinct Component contexts witnessing one exact Capability" in {
      Given("C11I-D04/10: separately acquired Components sharing one exact definition and explicit unavailable ComponentUsage")
      withDirectory { first => withDirectory { second =>
        val other = component.copy(componentId = ComponentId("org.other.Payments"))
        val othernative = native(other.componentId)
        val one = selected(first, Vector("a.json" -> projection()))
        val two = selected(second, Vector("b.json" -> projection(othernative, "second", other)), input = othernative)
        When("both original public packages are acquired in the explicit order")
        val candidate = right(_ingestion.acquire(selection(Vector(one, two))))
        Then("definition witnesses retain their own Component/package/location/native context without flattening")
        candidate.components.map(_.acquired.input.component) shouldBe Vector(component, other)
        candidate.components.map(_.entries.head.loaded.projection.capabilities.head) shouldBe Vector(capability, capability)
        candidate.components.map(_.acquired.input.location) shouldBe Vector(Location.Directory(first), Location.Directory(second))
        candidate.components.map(_.entries.head.realizations.head.snapshot.component) shouldBe Vector(component.componentId, other.componentId)
        candidate.usage.map(_.family).foreach(_ shouldBe a[ComponentUsageFamily.Unavailable])
      } }
    }
    "C11K07 preserve original later native/scope failures and reject the whole candidate" in {
      Given("C11I-D03/09: a successful first package followed by original Unavailable, Unauthorized or Stale observations")
      withDirectory { first => withDirectory { second =>
        val one = selected(first, Vector("first.json" -> emptyProjection("first")))
        val two = selected(second, Vector("second.json" -> emptyProjection("second")))
        val originals = Vector(DiagnosticKind.Unavailable, DiagnosticKind.Unauthorized, DiagnosticKind.Stale).map(rejected)
        val nativefailures = originals.map(d => two.copy(provider = Some(new CountingProvider(NativeInput(NativeAcquisition.Rejected(d), null, null)))))
        val scopefailure = two.copy(provider = Some(new CountingProvider(native().copy(scopeInput = ScopeInput.Rejected(originals(1))))))
        When("CBD requests each ordered complete batch")
        val errors = (nativefailures :+ scopefailure).map(x => left(_ingestion.acquire(selection(Vector(one, x)))))
        Then("the complete original Diagnostic object survives and no partial Component snapshot is returned")
        errors.zip(originals :+ originals(1)).foreach { (actual, original) =>
          actual should be theSameInstanceAs original
          actual shouldBe original
        }
        one.provider.get.asInstanceOf[CountingProvider].reads shouldBe 4
      } }
    }
    "C11K08 distinguish acquired empty scope from missing acquisition and provider" in {
      Given("C11I-D03/04: canonical acquired empty index with Ready native input and explicit missing branches")
      withDirectory { root => withDirectory { absent =>
        val pkg = selected(root, Vector.empty)
        val base = selection(Vector(pkg))
        val resourcepkg = selected(absent, Vector("first.json" -> projection()))
        val noprovider = resourcepkg.copy(provider = None)
        val nullprovider = resourcepkg.copy(provider = Some(null))
        val missingroot = pkg.copy(expected = pkg.expected.copy(input = pkg.expected.input.copy(location = Location.Directory(absent.resolve("missing")))))
        When("empty intake, explicit empty package vector and independently missing sources are acquired")
        val empty = right(_ingestion.acquire(base))
        val nopackages = right(_ingestion.acquire(selection(Vector.empty)))
        val providererror = left(_ingestion.acquire(base.copy(packages = Vector(noprovider))))
        val malformed = left(_ingestion.acquire(base.copy(packages = Vector(nullprovider))))
        val rooterror = left(_ingestion.acquire(base.copy(packages = Vector(missingroot))))
        Files.delete(root.resolve(indexPath))
        val indexerror = left(_ingestion.acquire(base))
        val missingresource = selected(root, Vector("first.json" -> projection()))
        Files.delete(root.resolve(metadataDirectory + "/first.json"))
        val resourceerror = left(_ingestion.acquire(selection(Vector(missingresource))))
        Then("only acquired complete empty inputs succeed; missing sources are Unavailable and Some(null) remains malformed")
        empty.components shouldBe Vector.empty
        empty.componentPackages.head match {
          case ComponentPackage.MetadataOnly(acquired) =>
            acquired.index shouldBe Vector.empty
            acquired.projections shouldBe Vector.empty
          case _ => fail("Expected the original metadata-only acquisition.")
        }
        pkg.provider.get.asInstanceOf[CountingProvider].reads shouldBe 0
        nopackages.components shouldBe Vector.empty
        Vector(providererror, rooterror, indexerror, resourceerror).map(_.kind) shouldBe Vector.fill(4)(DiagnosticKind.Unavailable)
        malformed.kind shouldBe DiagnosticKind.Malformed
      } }
    }
  }
  "Independent ComponentUsage acquisition" should {
    "C11K09 retain optional absence and reject present-invalid or unselected Usage" in {
      Given("C11I-D17: a canonical Component package with an absent optional family and independent explicit expectations")
      withDirectory { root =>
        val selectedscope = CapabilityComponentUsageProjectionFixtures.scope
        val input = native(selectedScope = selectedscope)
        val pkg = selected(root, Vector("first.json" -> projection(input)), selectedscope, input)
        val absent = selection(Vector(pkg), selectedscope)
        val expected = pkg.copy(expected = pkg.expected.copy(usage = Some(CapabilityComponentUsageProjectionFixtures.expectation)))
        When("absence is acquired, then the same package supplies malformed and complete present metadata")
        val accepted = right(_ingestion.acquire(absent))
        val missing = left(_ingestion.acquire(selection(Vector(expected), selectedscope)))
        write(root, "meta/component-usage-capability/index.json", """{"schemaVersion":1,"projections":["usage.json"]}""")
        write(root, CapabilityComponentUsageAcquisitionPart.resourcePath, "{")
        val invalid = left(_ingestion.acquire(selection(Vector(expected), selectedscope)))
        val present = withUsage(pkg, CapabilityComponentUsageProjectionFixtures.projection)
        val unexpected = left(_ingestion.acquire(absent))
        val observed = right(_ingestion.acquire(selection(Vector(present), selectedscope)))
        Then("only expected absence is unavailable; missing required, malformed present and unexpected present retain their typed outcomes")
        accepted.components.head.entries.size shouldBe 1
        accepted.usage.head.family shouldBe a[ComponentUsageFamily.Unavailable]
        Vector(missing.kind, invalid.kind, unexpected.kind) shouldBe Vector(DiagnosticKind.Unavailable, DiagnosticKind.Malformed, DiagnosticKind.Incompatible)
        observed.usage.head.input shouldBe pkg.expected.input
        observed.usage.head.resourcePath shouldBe "meta/component-usage-capability/usage.json"
        observed.usage.head.family shouldBe a[ComponentUsageFamily.Available]
      }
    }
    "C11K10 compare separately selected full ComponentUsage provenance" in {
      Given("C11I-D04/05: complete ordered ComponentUsage headers, independently selected producer and valid empty/app-only documents")
      withDirectory { root =>
        val s = CapabilityComponentUsageProjectionFixtures.scope
        val input = native(selectedScope = s)
        val pkg = withUsage(selected(root, Vector("first.json" -> projection(input)), s, input), CapabilityComponentUsageProjectionFixtures.projection)
        val e = CapabilityComponentUsageProjectionFixtures.expectation
        val variants = Vector(e.copy(documents = e.documents.take(2)), e.copy(producer = e.producer.copy(version = "other")),
          e.copy(documents = e.documents.updated(0, e.documents.head.copy(path = "models/wrong.cml"))),
          e.copy(documents = e.documents.updated(0, e.documents.head.copy(line = Some(7)))),
          e.copy(documents = e.documents.updated(0, e.documents.head.copy(sourceRevision = "old"))), e.copy(documents = e.documents.reverse),
          e.copy(schemaVersion = 2), e.copy(generatedAbiVersion = "future-abi"))
        val base = selection(Vector(pkg), s)
        When("the full ComponentUsage is admitted and every independent expected provenance variant is compared")
        val accepted = right(_ingestion.acquire(base))
        val errors = variants.map(x => left(_ingestion.acquire(base.copy(packages = Vector(pkg.copy(expected = pkg.expected.copy(usage = Some(x))))))))
        Then("complete ComponentUsage remains independent while omissions/mismatches/stale/unsupported domains keep their typed outcomes")
        errors.map(_.kind) shouldBe Vector(DiagnosticKind.Malformed, DiagnosticKind.Incompatible, DiagnosticKind.Incompatible, DiagnosticKind.Incompatible,
          DiagnosticKind.Stale, DiagnosticKind.Incompatible, DiagnosticKind.Unsupported, DiagnosticKind.Unsupported)
        accepted.usage.head.family match {
          case ComponentUsageFamily.Available(p, external) =>
            p.documents.map(_.componentUsageCapabilities.size) shouldBe Vector(1, 0, 1)
            p.documents.map(_.source) shouldBe e.documents
            external.size shouldBe 2
          case _ => fail("Expected independently available ComponentUsage.")
        }
      }
    }
    "C11K11 acquire genuine Usage-only metadata without a provider or native realization" in {
      Given("a canonical empty Component index and owned Usage evidence with independently selected finite facts")
      withDirectory { root =>
        val selectedscope = CapabilityComponentUsageProjectionFixtures.scope.copy(capabilities = Vector(capability))
        val pkg = withUsage(selected(root, Vector.empty, selectedscope), CapabilityComponentUsageProjectionFixtures.projection).copy(provider = None)
        When("CBD acquires the package through CNCF's metadata-only and Usage facades")
        val candidate = right(_ingestion.acquire(selection(Vector(pkg), selectedscope)))
        Then("the actual metadata witness and Usage survive without fabricated native objects, contracts or providers")
        candidate.components shouldBe Vector.empty
        candidate.componentPackages.size shouldBe 1
        candidate.componentPackages.head shouldBe a[ComponentPackage.MetadataOnly]
        candidate.usage.head.family shouldBe a[ComponentUsageFamily.Available]
        candidate.provenance.packages.head.resources shouldBe Vector.empty
        candidate.provenance.packages.head.usage shouldBe pkg.expected.usage
      }
    }
  }
}
