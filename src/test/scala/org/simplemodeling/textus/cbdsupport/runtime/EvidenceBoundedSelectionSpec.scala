package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.time.{Clock, Instant, ZoneOffset}

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class EvidenceBoundedSelectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC)

  "Exact component selection" should {
    "preserve conflicting catalog candidates" which {
      "returns bounded alternatives instead of a priority winner" in {
        Given("two catalogs publishing the same exact component identity and version")
        val primary = CatalogSource("primary", URI.create("https://primary.example/"), 100, true)
        val secondary = CatalogSource("secondary", URI.create("https://secondary.example/"), 200, true)
        val provider = new PerSourceProvider(Map(
          primary.id -> Vector(_profile(primary.id)),
          secondary.id -> Vector(_profile(secondary.id))
        ))
        val runtime = CbdRuntime.create(Vector(primary, secondary), provider, _clock)
        runtime.ensureReady(EmptyFetcher).isSuccess shouldBe true

        When("the exact identity is requested without a catalog selector")
        val selection = runtime.selectComponent("textus-order", Some("org.textus"), Some("car"), Some("1.0.0"), None)

        Then("no hidden winner is exposed and every catalog alternative is attributable")
        selection.status shouldBe "ambiguous"
        selection.selectedProfile shouldBe None
        selection.candidateCount shouldBe 2
        selection.alternatives.map(_.catalogId) shouldBe Vector("primary", "secondary")
        selection.absences.map(_.code) shouldBe Vector(ExactComponentSelection.AMBIGUOUS_SELECTION)
        selection.absences.head.sourceIds shouldBe Vector("primary", "secondary")
        runtime.get("textus-order", Some("org.textus"), Some("car"), Some("1.0.0"), None) shouldBe None
      }

      "selects a single source only when the caller supplies its catalog identity" in {
        Given("two catalogs publishing the same exact component and one explicit catalog selector")
        val primary = CatalogSource("primary", URI.create("https://primary.example/"), 100, true)
        val secondary = CatalogSource("secondary", URI.create("https://secondary.example/"), 200, true)
        val provider = new PerSourceProvider(Map(
          primary.id -> Vector(_profile(primary.id)),
          secondary.id -> Vector(_profile(secondary.id))
        ))
        val runtime = CbdRuntime.create(Vector(primary, secondary), provider, _clock)
        runtime.ensureReady(EmptyFetcher).isSuccess shouldBe true

        When("the caller explicitly selects one catalog")
        val selected = runtime.selectComponent("textus-order", Some("org.textus"), Some("car"), Some("1.0.0"), Some("secondary"))

        Then("only that explicit source becomes the selected profile")
        selected.status shouldBe "matched"
        selected.selectedProfile.map(_.catalogId) shouldBe Some("secondary")
        selected.alternatives shouldBe empty
        selected.absences shouldBe empty
      }

      "reports the full candidate count when alternatives are truncated" in {
        Given("more exact candidates than the response alternative bound")
        val candidates = (1 to 25).map(x => _profile(s"catalog-$x")).toVector

        When("the candidates enter exact selection")
        val selection = ExactComponentSelection.fromCandidates(candidates)

        Then("the bounded alternatives and unbounded count make truncation explicit")
        selection.candidateCount shouldBe 25
        selection.alternatives should have size ExactComponentSelection.MAXIMUM_ALTERNATIVES
        selection.warnings.exists(_.contains("20 of 25")) shouldBe true
        selection.absences.head.evidenceUris should have size ExactComponentSelection.MAXIMUM_ALTERNATIVES
      }
    }

    "make insufficient evidence explicit" which {
      "returns component-not-found without a synthetic reference" in {
        Given("an empty exact candidate set")
        val selection = ExactComponentSelection.fromCandidates(Vector.empty)

        When("the empty set is interpreted")
        val absence = selection.absences.head

        Then("selection remains absent with a stable absence code")
        selection.status shouldBe "no-match"
        selection.selectedProfile shouldBe None
        selection.alternatives shouldBe empty
        selection.candidateCount shouldBe 0
        absence.code shouldBe ExactComponentSelection.COMPONENT_NOT_FOUND
        absence.sourceIds shouldBe empty
        absence.evidenceUris shouldBe empty
      }

      "distinguishes missing dependency metadata from an authoritative empty dependency set" in {
        Given("one selected profile whose catalog publishes no dependency metadata")
        val profile = _profile("catalog").copy(dependencyMetadataVersion = None, dependencies = Vector.empty)
        val runtime = CbdRuntime.create(
          Vector(CatalogSource("catalog", URI.create("https://catalog.example/"), 100, true)),
          new PerSourceProvider(Map("catalog" -> Vector(profile))),
          _clock
        )

        When("dependencies are resolved for the selected version")
        val resolution = runtime.resolveDependencies(profile, None, 8)

        Then("the empty list is accompanied by explicit metadata absence")
        resolution.directDependencies shouldBe empty
        resolution.resolutions shouldBe empty
        resolution.absences.map(_.code) shouldBe Vector(ExactComponentSelection.DEPENDENCY_METADATA_ABSENT)
        resolution.absences.head.versions shouldBe Vector("1.0.0")
      }
    }
  }

  private def _profile(catalogid: String): ComponentProfile =
    ComponentProfile(
      catalogid,
      Some("org.textus"),
      "textus-order",
      "Textus Order",
      Some("Order component."),
      "car",
      Vector("1.0.0"),
      Some("1.0.0"),
      Some("1.0.0"),
      Some("1.0.0"),
      None,
      Some("0.5.1"),
      Vector.empty,
      Vector.empty,
      Vector.empty,
      Some(URI.create(s"https://$catalogid.example/textus-order.car")),
      URI.create(s"https://$catalogid.example/index.json"),
      None,
      None,
      Vector.empty,
      Vector.empty
    )

  private final class PerSourceProvider(profilesbysource: Map[String, Vector[ComponentProfile]])
    extends ComponentCatalogProvider {
    def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
      Consequence.success(CatalogSnapshot(
        source,
        profilesbysource.getOrElse(source.id, Vector.empty),
        Instant.parse("2026-07-14T08:00:00Z"),
        None
      ))

    def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
      Consequence.success(ComponentUsage(profile, Vector.empty, Vector.empty, Vector.empty))
  }

  private object EmptyFetcher extends CatalogFetcher {
    def get(uri: URI): Consequence[String] = Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
  }
}
