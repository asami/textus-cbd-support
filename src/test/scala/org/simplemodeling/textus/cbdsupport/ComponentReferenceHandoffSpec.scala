package org.simplemodeling.textus.cbdsupport

import java.net.URI
import java.time.{Clock, Instant, ZoneOffset}

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * Failing-first executable acceptance specification for DOC-07
 * (Phase 59.7 / Step P597-S2 / Slice P597-S2A).
 *
 * CBD independently owns catalog detail and usage selection. A BoK-like
 * handoff source identity is evidence only; it is never used as a catalog
 * selector. All inputs are deterministic in-memory catalog values.
 *
 * @since   Aug. 27, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentReferenceHandoffSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _clock = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneOffset.UTC)

  "DOC-07 CBD component-reference handoff" should {
    "select catalog detail and usage from handoff coordinates without using the BoK source id" in {
      Given("one deterministic published catalog and a BoK-like existence handoff coordinate")
      val catalogSource = CatalogSource("catalog-primary", URI.create("https://catalog.example/"), 100, enabled = true)
      val runtime = CbdRuntime.create(
        Vector(catalogSource),
        new InMemoryComponentCatalogProvider(
          Vector(_component_profile),
          Map("example-textus-order" -> Vector(ComponentOperation(Some("OrderQuery"), "getOrder", Some("query"), Some("Return one order.")))),
          _clock
        ),
        _clock
      )
      runtime.ensureReady(_empty_fetcher).isSuccess shouldBe true
      val handoff = BokLikeCoordinate(
        "example-textus-order",
        "car",
        Some("org.example"),
        Some("1.2.0"),
        "bok-source-a",
        URI.create("urn:textus:bok:source-a:component:example-textus-order")
      )

      When("CBD selects the exact catalog identity and reads its catalog-owned usage")
      val selection = runtime.selectComponent(
        handoff.name,
        handoff.organization,
        Some(handoff.kind),
        handoff.version,
        None
      )
      val profile = selection.selectedProfile.get
      val usage = runtime.usage(profile, Some("retrieve order"), _empty_fetcher).TAKE

      Then("the catalog is the selected owner and the BoK source id remains non-selecting evidence")
      selection.status shouldBe "matched"
      selection.selectedProfile.map(_.catalogId) shouldBe Some("catalog-primary")
      selection.selectedProfile.map(_.catalogId) should not contain handoff.sourceId
      selection.selectedProfile.map(_.name) shouldBe Some("example-textus-order")
      selection.selectedProfile.flatMap(_.organization) shouldBe Some("org.example")
      selection.selectedProfile.map(_.kind) shouldBe Some("car")
      selection.selectedProfile.flatMap(_.selectedVersion) shouldBe Some("1.2.0")
      selection.selectedProfile.map(_.evidenceUri) shouldBe Some(URI.create("https://catalog.example/example-textus-order.index"))
      usage.profile.catalogId shouldBe "catalog-primary"
      usage.profile.selectedVersion shouldBe Some("1.2.0")
      usage.selectedSourceId shouldBe Some("catalog-primary")
      usage.selectedVersion shouldBe Some("1.2.0")
      usage.operations.map(_.operation) shouldBe Vector("getOrder")
      usage.guidance.map(_.evidenceUris).flatten should contain(URI.create("https://catalog.example/example-textus-order.index"))
      usage.references shouldBe Vector(("catalog", URI.create("https://catalog.example/example-textus-order.index"), true))
    }

    "report missing and ambiguous catalog ownership without inventing a detail winner" in {
      Given("two deterministic catalogs publishing the same exact component identity")
      val primary = CatalogSource("catalog-primary", URI.create("https://primary.example/"), 100, enabled = true)
      val secondary = CatalogSource("catalog-secondary", URI.create("https://secondary.example/"), 200, enabled = true)
      val runtime = CbdRuntime.create(
        Vector(primary, secondary),
        new InMemoryComponentCatalogProvider(Vector(_component_profile), clock = _clock),
        _clock
      )
      runtime.ensureReady(_empty_fetcher).isSuccess shouldBe true

      When("CBD resolves an absent version and an unqualified exact identity")
      val missing = runtime.selectComponent("example-textus-order", Some("org.example"), Some("car"), Some("9.9.9"), None)
      val ambiguous = runtime.selectComponent("example-textus-order", Some("org.example"), Some("car"), Some("1.2.0"), None)

      Then("each unresolved state remains explicit and has no selected detail or usage owner")
      missing.status shouldBe "no-match"
      missing.selectedProfile shouldBe empty
      missing.candidateCount shouldBe 0
      ambiguous.status shouldBe "ambiguous"
      ambiguous.selectedProfile shouldBe empty
      ambiguous.candidateCount shouldBe 2
      ambiguous.alternatives.map(_.catalogId) shouldBe Vector("catalog-primary", "catalog-secondary")
      ambiguous.absences.map(_.code) should contain(ExactComponentSelection.AMBIGUOUS_SELECTION)
    }
  }

  private final case class BokLikeCoordinate(
    name: String,
    kind: String,
    organization: Option[String],
    version: Option[String],
    sourceId: String,
    evidenceUri: URI
  )

  private def _component_profile: ComponentProfile = ComponentProfile(
    catalogId = "catalog-fixture",
    organization = Some("org.example"),
    name = "example-textus-order",
    title = "Textus Order",
    summary = Some("Order retrieval catalog fixture."),
    kind = "car",
    versions = Vector("1.1.0", "1.2.0"),
    selectedVersion = Some("1.1.0"),
    dependencyMetadataVersion = None,
    latestStable = Some("1.2.0"),
    latestSnapshot = None,
    runtimeMinimum = None,
    tags = Vector("order"),
    terms = Vector("retrieve", "order"),
    dependencies = Vector.empty,
    artifactUri = Some(URI.create("https://catalog.example/example-textus-order.car")),
    evidenceUri = URI.create("https://catalog.example/example-textus-order.index"),
    modelMetadataUri = None,
    documentationUri = None,
    versionEvidence = Vector(
      ComponentVersionEvidence(
        version = "1.1.0",
        runtimeMinimum = None,
        dependencies = Vector.empty,
        artifactUri = Some(URI.create("https://catalog.example/example-textus-order-1.1.0.car")),
        modelMetadataUri = None,
        hasDependencyMetadata = false
      ),
      ComponentVersionEvidence(
        version = "1.2.0",
        runtimeMinimum = None,
        dependencies = Vector.empty,
        artifactUri = Some(URI.create("https://catalog.example/example-textus-order-1.2.0.car")),
        modelMetadataUri = None,
        hasDependencyMetadata = false,
        status = Some("active"),
        component = Some("org.example.TextusOrder")
      )
    ),
    warnings = Vector.empty
  )

  private val _empty_fetcher = new CatalogFetcher {
    override def get(uri: URI): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected catalog fetch: $uri")
  }
}
