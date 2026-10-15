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
import CapabilityViewModel.*
import CapabilityQueryFixtures.*
import CapabilityComponentUsageJsonPart._items

/*
 * Synthetic semantic inputs only; real producer acceptance belongs to CAP-11-05.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityViewsSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Shared Capability views" should {
    "retain original admitted values" which {
      "C11V01 preserve overview order, qualified references, provenance and separate families" in {
        Given("an admitted synthetic scope with two equal-label qualified identities and two providers")
        val other = reference.copy(identity = reference.identity.copy(modelId = "Other:equal-label"))
        val query = session(scenario(Vector(ProviderInput(other, 1), ProviderInput(reference, 2))))
        When("the session is mapped to overview and rendered")
        val view = CapabilityViews.overview(query)
        val html = CapabilityWebView.overview(view)
        Then("the original vectors and scope survive without sorting or label deduplication")
        view shouldBe Overview(query.scope, query.catalog.capabilities, query.catalog.usages, query.catalog.projection.useCase)
        view.contracts.map(_.key) shouldBe query.catalog.capabilities.map(_.key)
        val positions = view.contracts.map(r => html.indexOf("data-reference=\"" + r.key.toString + "\""))
        positions.forall(_ >= 0) shouldBe true
        positions shouldBe positions.sorted
        html should include ("Component contract")
        html should include ("Component Usage observation")
        html should include (query.scope.snapshotReference.toString)
        html should include (query.scope.provenance.scope.source.toString)
        html should include (other.identity.modelId)
        html should include (reference.contractVersion)
        html should include ("data-context=\"" + view.contracts.head.key.context.toString + "\"")
        html should include (view.useCase.reason)
      }
      "C11V02 delegate exact detail and discovery-only detail without changing semantics" in {
        Given("equal display labels on different identities and an admitted session")
        val other = reference.copy(identity = reference.identity.copy(modelId = "Other:discovery"))
        val query = session(scenario(Vector(ProviderInput(reference), ProviderInput(other))))
        val exact = CapabilityLookup.Exact(reference)
        val search = CapabilityLookup.Search("Same capability")
        When("both lookups pass through the shared detail operation")
        val exactview = CapabilityViews.detail(query, exact).toOption.get
        val searchview = CapabilityViews.detail(query, search).toOption.get
        val exacthtml = CapabilityWebView.detail(exactview)
        val searchhtml = CapabilityWebView.detail(searchview)
        Then("the complete original results and their distinct designations remain visible")
        exactview.result shouldBe query.getCapability(exact).toOption.get
        searchview.result shouldBe query.getCapability(search).toOption.get
        exactview.result.value.records.size shouldBe 2
        searchview.result.value.records.size shouldBe 3
        exacthtml should include ("ExactIdentity")
        searchhtml should include ("DiscoveryOnly")
        searchhtml should include ("discovery assistance")
        searchhtml should include (other.identity.modelId)
        CapabilityQueryFixtures.value(query.findProviders(reference)).matches.size shouldBe 1
      }
      "C11V03 retain repeated many-to-many paths and each provider's own realizations" in {
        Given("two Required declarations, two providers with different own targets and two repeated mappings")
        val input = scenario(Vector(ProviderInput(reference, 3), ProviderInput(reference, 1)), requiredcount = 2,
          mappings = Vector(mapping(), mapping()))
        val query = session(input)
        When("the original trace and diagnosis are mapped and rendered")
        val view = CapabilityViews.usageTrace(query, usageReference).toOption.get
        val html = CapabilityWebView.usageTrace(view)
        val rendered = _provider_sections(html)
        Then("both complete results survive and each nested branch contains only its own target vector")
        view.result shouldBe query.traceComponentUsageCapability(usageReference).toOption.get
        view.diagnosis shouldBe query.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(usageReference)).toOption.get
        rendered.size shouldBe 8
        val expected = keys(input, Direction.Provided).zip(input.packages.drop(2)).map { (key, pkg) =>
          key.toString -> pkg.resources.head.realizations.map(_.target.toString)
        }.toMap
        rendered.foreach { (owner, body) =>
          _realization_attributes(body) shouldBe expected(owner).map(target => owner -> target)
        }
        view.result.value.head.paths.map(_.path.mapping) shouldBe Vector(mapping(), mapping())
      }
    }
  }
  "Capability Web presentation" should {
    "show attributed facts and known gaps independently" which {
      "C11V04 show Specification and Evidence targets with original owner, Source and Origin" in {
        Given("original declared role links on the Usage and both Component declaration owners")
        val query = session(scenario())
        val view = CapabilityViews.usageTrace(query, usageReference).toOption.get
        val original = traces("model:app", roles)
        When("the admitted Usage trace is rendered")
        val html = CapabilityWebView.usageTrace(view)
        Then("both role families retain exact target, boundary, source and derived origin attribution")
        original.foreach { edge =>
          html should include (edge.role.toString)
          html should include (edge.target.toString)
          html should include (edge.source.toString)
          html should include (edge.origin.toString)
          html should include ("data-reference=\"" + edge.target.toString + "\"")
        }
        html should include (view.result.value.head.usage.key.toString)
        html should include ("Attributed facts")
        html should not include "href=\"http"
      }
      "C11V05 present all seven gap case names with original scope and owning path context" in {
        Given("explicit absent, incompatible, empty-realization and unresolved Required branches without role coverage")
        val missing = reference.copy(identity = reference.identity.copy(elementId = "Missing:providers"))
        val versioned = reference.copy(identity = reference.identity.copy(elementId = "Wrong:version"))
        val wrong = versioned.copy(contractVersion = "wrong:v7")
        val orphan = reference.copy(identity = reference.identity.copy(elementId = "Missing:required"))
        val selectedscope = scope(Vector(reference, missing, versioned, wrong, orphan).map(definition(_)))
        val packages = Vector(packageFor("NoProvider", "no-provider", selectedscope, missing, Vector(Direction.Required), 0, Vector.empty),
          packageFor("VersionRequired", "version-required", selectedscope, versioned, Vector(Direction.Required), 0, Vector.empty),
          packageFor("RealRequired", "real-required", selectedscope, reference, Vector(Direction.Required), 0, Vector.empty),
          packageFor("WrongProvider", "wrong-provider", selectedscope, wrong, Vector(Direction.Provided), 0, Vector.empty),
          packageFor("EmptyProvider", "empty-provider", selectedscope, reference, Vector(Direction.Provided), 0, Vector.empty))
        val maps = Vector(mapping(missing), mapping(versioned), mapping(), mapping(orphan))
        val input = assemble(packages, selectedscope, Some(usages(maps, selectedroles = Vector.empty)))
        val query = session(input)
        val absent = usageReference.copy(contractVersion = "absent:usage")
        When("both the selected trace and an absent exact Usage trace are rendered")
        val view = CapabilityViews.usageTrace(query, usageReference).toOption.get
        val absentview = CapabilityViews.usageTrace(query, absent).toOption.get
        val html = CapabilityWebView.usageTrace(view) + CapabilityWebView.usageTrace(absentview)
        Then("each closed variant displays its distinct case name and original owning facts")
        val names = Vector("MissingComponentUsageCapabilityProjection", "MissingRequiredComponentCapability", "NoProvider",
          "IncompatibleProviderVersion", "MissingRealization", "MissingSpecification", "MissingEvidence")
        (view.diagnosis.value.gaps ++ absentview.diagnosis.value.gaps).map(_.productPrefix).distinct.toSet shouldBe names.toSet
        names.foreach(name => html should include ("data-gap=\"" + name + "\""))
        html should include (query.scope.snapshotReference.toString)
        maps.foreach(edge => html should include (edge.toString))
        keys(input, Direction.Required).foreach(key => html should include (key.toString))
        html should include ("AbsentSelectedReference")
        html should include ("Original Presence")
        html should include ("Role coverage")
      }
      "C11V06 keep four Unknown runtime observations and their reasons separate from missing roles" in {
        Given("an admitted realized provider with no Specification or Evidence coverage and four distinct Unknown reasons")
        val query = session(scenario(Vector(ProviderInput(reference, 1, Vector.empty)),
          requiredroles = Vector.empty, usageroles = Vector.empty))
        val original = CapabilityViews.usageTrace(query, usageReference).toOption.get
        val usage = original.result.value.head
        val path = usage.paths.head
        val required = path.required.head
        val provider = required.providers.matches.head
        val realization = provider.provider.realizations.head
        val reasons = Vector("availability:unobserved", "authorization:unobserved", "guard:unobserved", "execution:unobserved")
        val observations = RuntimeObservations(AvailabilityObservation.Unknown(reasons(0)), AuthorizationObservation.Unknown(reasons(1)),
          GuardObservation.Unknown(reasons(2)), ExecutionResultObservation.Unknown(reasons(3)))
        val changedprovider = provider.copy(provider = provider.provider.copy(realizations = Vector(realization.copy(observations = observations))))
        val changedrequired = required.copy(providers = required.providers.copy(matches = Vector(changedprovider)))
        val changedpath = path.copy(required = Vector(changedrequired))
        val changedusage = usage.copy(paths = Vector(changedpath))
        val view = original.copy(result = original.result.copy(value = Vector(changedusage)))
        When("the immutable view's runtime observations are rendered")
        val html = CapabilityWebView.usageTrace(view)
        Then("each Unknown keeps its own reason while known Specification/Evidence gaps stay distinct")
        Vector("Availability", "Authorization", "Guard", "Execution result").zip(reasons).foreach { (label, reason) =>
          html should include (label + "</dt><dd>Unknown: " + reason)
        }
        html should include ("MissingSpecification")
        html should include ("MissingEvidence")
        html should include (realization.edge.targetKind.toString)
        html should include (realization.edge.targetContractVersion)
        html should include (realization.edge.generatedAbiVersion)
        html should include (realization.edge.source.toString)
        html should include (realization.edge.origin.toString)
        html should include (realization.target.source.toString)
      }
    }
    "retain success and failure boundaries" which {
      "C11V07 distinguish admitted empty results from unavailable Usage and UseCase" in {
        Given("an available empty Usage family, an absent Usage family and an unavailable forward UseCase")
        val input = scenario()
        val available = session(CapabilityCatalogFixtures.withUsagePackages(input,
          input.packages.map(_ => Some(usages(Vector.empty).copy(documents = Vector.empty)))))
        val unavailable = session(scenario(usageAvailable = false))
        val absent = usageReference.copy(contractVersion = "absent:exact")
        val usecase = ModelElementId("model:usecase", "requested:usecase")
        When("empty and unavailable selections pass through the views")
        val empty = CapabilityViews.usageTrace(available, absent).toOption.get
        val emptydetail = CapabilityViews.detail(available, CapabilityLookup.Exact(reference.copy(contractVersion = "absent:contract"))).toOption.get
        val missing = CapabilityViews.usageTrace(unavailable, usageReference)
        val forward = CapabilityViews.useCaseTrace(available, usecase)
        val overview = CapabilityWebView.overview(CapabilityViews.overview(available))
        Then("empty is scoped success while failures retain exact original unavailable values")
        empty.result.value shouldBe Vector.empty
        CapabilityViews.overview(available).usages shouldBe Vector.empty
        CapabilityWebView.usageTrace(empty) should include ("Successful empty collection")
        CapabilityWebView.detail(emptydetail) should include ("Successful empty collection")
        missing.swap.toOption.get shouldBe unavailable.traceComponentUsageCapability(usageReference).swap.toOption.get
        missing.isLeft shouldBe true
        forward shouldBe available.traceUseCaseDemand(usecase)
        forward shouldBe Left(QueryFailure.UnavailableProjection(available.catalog.projection.useCase))
        overview should include ("UseCase unavailable")
        overview should include (available.catalog.projection.useCase.reason)
        CapabilityWebView.failure(missing.swap.toOption.get) should include ("ComponentUsage")
        CapabilityWebView.failure(forward.swap.toOption.get) should include ("UseCase")
      }
      "C11V08 preserve every Source diagnostic kind and exact query input failures" in {
        Given("all original diagnostic kinds and a malformed exact query reference")
        val query = session(scenario())
        val originals = DiagnosticKind.values.toVector.map(kind => Diagnostic(kind, "original:" + kind,
          Some("original:scope"), Some(ComponentId("org.example.Original")),
          Some(Source("original:authority", "source/original.cml", "original:v7", Some(39))), "/original/" + kind))
        val lookup = CapabilityLookup.Exact(reference.copy(contractVersion = ""))
        When("failures are rendered and the malformed query is delegated")
        val rendered = originals.map(d => d -> CapabilityWebView.failure(QueryFailure.Source(d)))
        val result = CapabilityViews.detail(query, lookup)
        Then("all diagnostic fields remain visible and the query failure is unchanged")
        rendered.size shouldBe 7
        rendered.foreach { (diagnostic, html) =>
          html should include (diagnostic.kind.toString)
          html should include (diagnostic.detail)
          html should include (diagnostic.location)
          html should include (diagnostic.scopeId.get)
          html should include (diagnostic.component.get.toString)
          html should include (diagnostic.source.get.toString)
          html should not include "Successful empty collection"
        }
        result shouldBe query.getCapability(lookup).map(Detail(_))
      }
      "C11V09 escape dynamic text and exact reference attributes across every renderer" in {
        Given("typed view facts containing markup, ampersands and both quote characters")
        val query = session(scenario())
        val original = CapabilityViews.overview(query)
        val attack = "<script a=\"x\" b='y'>&danger</script>"
        val escaped = "&lt;script a=&quot;x&quot; b=&#39;y&#39;&gt;&amp;danger&lt;/script&gt;"
        val record = original.contracts.head
        val changedkey = record.key.copy(context = record.key.context.copy(resourcePath = attack))
        val changed = record.copy(key = changedkey, definition = record.definition.copy(name = attack, summary = Present(attack),
          description = Absent(AbsenceReason.NotDeclared, attack)))
        val overview = original.copy(contracts = Vector(changed), useCase = original.useCase.copy(reason = attack, detail = attack))
        val detail = Detail(QueryResult(query.scope, CapabilityLookupResult(CapabilityLookup.Search(attack), LookupDesignation.DiscoveryOnly, Vector(changed))))
        val trace = CapabilityViews.usageTrace(query, usageReference).toOption.get
        val usage = trace.result.value.head
        val changedusage = usage.copy(usage = usage.usage.copy(definition = usage.usage.definition.copy(name = attack)))
        val diagnostic = Diagnostic(DiagnosticKind.Unauthorized, attack, Some(attack), None,
          Some(Source(attack, attack, attack, None)), attack)
        When("each Web renderer consumes the dynamic values")
        val outputs = Vector(CapabilityWebView.overview(overview), CapabilityWebView.detail(detail),
          CapabilityWebView.usageTrace(trace.copy(result = trace.result.copy(value = Vector(changedusage)))),
          CapabilityWebView.failure(QueryFailure.Source(diagnostic)),
          CapabilityWebView.failure(QueryFailure.UnavailableProjection(original.useCase.copy(reason = attack, detail = attack))))
        Then("all values are escaped and no dynamic markup or executable external link is introduced")
        outputs.foreach { html =>
          html should include (escaped)
          html should not include attack
          html should not include "<script"
          html should not include "href="
        }
        outputs.head should include ("Resource path")
        outputs.head should include ("data-context=\"")
        outputs.head should include (escaped + ",SourceKey")
      }
    }
  }
  "Rendered finite generated traces" should {
    "preserve source-owned edges" which {
      "C11V10 sample at least one hundred provider-owned target/cardinality cases with zero discards" in {
        Given("finite valid-by-construction query fixture samples with independent provider, target and repeated mapping counts")
        val samples = for {
          token <- Gen.oneOf("É|Case::A", "É|case::a", "支払/契約", "Cap:cap|#")
          status <- Gen.oneOf(SampleStatus.values.toVector)
          count <- Gen.choose(0, 3)
          targets <- Gen.listOfN(count, Gen.choose(0, 3)).map(_.toVector)
          mappings <- Gen.choose(0, 3)
        } yield Sample(token, status, count, targets, mappings)
        When("ScalaCheck interprets at least one hundred finite samples")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { sample =>
          Given("one generated source projection and its explicit matching status and original mapping vector")
          val input = generated(sample)
          val compatible = sample.status == SampleStatus.Exact || sample.status == SampleStatus.Directed
          val sourceusage = input.usage.head.family.asInstanceOf[ComponentUsageFamily.Available].projection
          val sourcemappings = _items(sourceusage.documents.head.componentUsageCapabilities.head.componentMappings)
          val sourceproviders = keys(input, Direction.Provided).zip(input.packages.tail).map { (key, pkg) =>
            key.toString -> pkg.resources.head.realizations.map(_.target.toString)
          }
          val expected = sourcemappings.flatMap(_ => if (compatible) sourceproviders else Vector.empty)
          val expectededges = expected.flatMap((owner, targets) => targets.map(owner -> _))
          When("the admitted session's original nested Usage trace is rendered")
          val html = CapabilityWebView.usageTrace(CapabilityViews.usageTrace(session(input), usageReference).toOption.get)
          val rendered = _provider_sections(html)
          Then("only source-compatible providers occur per mapping with their original own target order and no manufactured edge")
          rendered.map(_._1) shouldBe expected.map(_._1)
          rendered.map((owner, body) => owner -> _realization_attributes(body).map(_._2)) shouldBe expected
          _realization_attributes(html) shouldBe expectededges
          rendered.size shouldBe (if (compatible) sourcemappings.size * sourceproviders.size else 0)
          true
        })
        Then("at least one hundred applicable cases pass with zero discards")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
        info(s"C11V10 rendered trace samples: succeeded=${checked.succeeded}, discarded=${checked.discarded}")
      }
    }
  }
  private def _provider_sections(html: String): Vector[(String, String)] =
    "(?s)<section class=\"provider\" data-reference=\"([^\"]*)\">(.*?)</section>".r.findAllMatchIn(html)
      .map(m => m.group(1) -> m.group(2)).toVector
  private def _realization_attributes(html: String): Vector[(String, String)] =
    "<article class=\"realization\" data-owner=\"([^\"]*)\" data-target=\"([^\"]*)\">".r.findAllMatchIn(html)
      .map(m => m.group(1) -> m.group(2)).toVector
}
