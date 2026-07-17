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
final class SemanticRequirementMatchingSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "SemanticRequirementMatcher" should {
    "preserve BoK-site and SIE match ownership as separate requirement citations" in {
      Given("one locally matched BoK term and one query-scoped SIE semantic match")
      val boksnapshot = _bok_snapshot
      val siesnapshot = _sie_snapshot("run environment")

      When("the requirement evidence is normalized")
      val evidence = SemanticRequirementMatcher.matchEvidence(
        "run environment",
        Vector(boksnapshot),
        Vector(siesnapshot),
        10,
        _clock
      )

      Then("both citations retain their own source, rationale, dataset, and evidence location")
      evidence.map(_.sourceKind).toSet shouldBe Set(InformationSourceKind.BOK_SITE, InformationSourceKind.SIE_BOK)
      evidence.map(_.freshness).toSet shouldBe Set("fresh", "observed")
      evidence.find(_.sourceKind == InformationSourceKind.BOK_SITE).map(_.rationale) shouldBe
        Some("Exact published BoK label match for architecture:runtime.")
      evidence.find(_.sourceKind == InformationSourceKind.SIE_BOK).flatMap(_.datasetId) shouldBe Some("bok-main")
      evidence.map(_.evidenceLocation).toSet shouldBe Set(
        "https://bok.example/terms.json#/terms/0",
        "https://sie.example/terms/runtime"
      )

      When("the response is limited to the SIE source after matching all bounded candidates")
      val selected = SemanticRequirementMatcher.matchEvidence(
        "run environment",
        Vector(boksnapshot),
        Vector(siesnapshot),
        1,
        _clock,
        BokInspectionPolicy.DEFAULT.refreshTtl,
        sourceid = Some("semantic")
      )

      Then("another source cannot consume the requested source's result limit")
      selected.map(_.sourceId) shouldBe Vector("semantic")
    }

    "ignore a retained SIE observation from another query" in {
      Given("one SIE snapshot whose query differs from the current requirement")
      val retained = _sie_snapshot("old query")

      When("the current query is matched")
      val evidence = SemanticRequirementMatcher.matchEvidence("new query", Vector.empty, Vector(retained), 10, _clock)

      Then("query-scoped evidence is absent rather than reused")
      evidence shouldBe empty
    }
  }

  "Source-aware component search" should {
    "link an explicit catalog term to semantic evidence without merging BoK fields into the profile" in {
      Given("one catalog profile declaring a term and one SIE match for that same term")
      val source = CatalogSource("catalog", URI.create("https://catalog.example/"), 100, true)
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(_profile), clock = _clock),
        CatalogCachePolicy.DEFAULT,
        _clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("a requirement with no direct catalog tokens is searched with current semantic evidence")
      val result = runtime.searchSourceAware(_query("run environment"), Vector(_sie_snapshot("run environment")))

      Then("the catalog match cites the independent semantic record and remains catalog-owned")
      result.matches.map(_.matchKind) shouldBe Vector("semantic")
      result.matches.flatMap(_.semanticEvidenceIds) shouldBe result.semanticEvidence.map(_.id)
      result.report.observations.map(_.sourceKind) shouldBe Vector(InformationSourceKind.PUBLISHED_CATALOG)
      result.semanticEvidence.map(_.sourceKind) shouldBe Vector(InformationSourceKind.SIE_BOK)
      result.matches.head.profile.summary shouldBe None
      result.semanticEvidence.head.definition shouldBe Some("Runtime definition from SIE.")
    }
  }

  private val _clock = Clock.fixed(Instant.parse("2026-07-14T08:00:00Z"), ZoneOffset.UTC)

  private def _bok_snapshot: BokSourceSnapshot = {
    val descriptor = InformationSourceDescriptor(
      "architecture-bok",
      InformationSourceKind.BOK_SITE,
      "https://bok.example/",
      600,
      true,
      InformationSourceAuthorization.EXACT_ORIGIN_ALLOWLIST
    )
    BokSourceSnapshot(
      descriptor,
      "architecture-bok",
      Some("Architecture BoK"),
      Some("architecture-bok"),
      None,
      URI.create("https://bok.example/metadata/cncf/knowledge-source.json"),
      Vector.empty,
      Vector(BokTermObservation(
        "architecture-bok",
        "architecture-bok",
        "architecture:runtime",
        Some("execution-runtime"),
        Some("Execution Runtime"),
        None,
        Some("architecture"),
        Some("Runtime definition from the BoK site."),
        Vector("run environment"),
        Some("term"),
        Vector.empty,
        Vector.empty,
        Vector.empty,
        Vector.empty,
        "https://bok.example/terms.json#/terms/0",
        Vector.empty
      )),
      _clock.instant(),
      Vector.empty
    )
  }

  private def _sie_snapshot(query: String): SieBokSnapshot =
    SieBokSnapshot(
      InformationSourceDescriptor(
        "semantic",
        InformationSourceKind.SIE_BOK,
        "https://sie.example/mcp",
        700,
        true,
        InformationSourceAuthorization.COMPONENT_ROUTE_ALLOWLIST
      ),
      "matched",
      query,
      Vector(SieBokTermEvidence(
        "semantic",
        "architecture:runtime",
        "Execution Runtime",
        "Runtime definition from SIE.",
        Some("architecture"),
        "term",
        "bok-main",
        "semantic",
        0.9,
        "SIE matched the runtime intent.",
        URI.create("https://sie.example/terms/runtime")
      )),
      _clock.instant(),
      Vector.empty
    )

  private def _query(requirement: String): SourceAwareComponentSearchQuery =
    SourceAwareComponentSearchQuery(
      requirement,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some(ReconciliationPurpose.PUBLISHED_REUSE),
      10
    )

  private def _profile: ComponentProfile =
    ComponentProfile(
      "catalog",
      Some("org.textus"),
      "textus-executor",
      "Textus Executor",
      None,
      "car",
      Vector("1.0.0"),
      Some("1.0.0"),
      None,
      Some("1.0.0"),
      None,
      Some("0.5.1"),
      Vector.empty,
      Vector("Execution Runtime"),
      Vector.empty,
      None,
      URI.create("https://catalog.example/index.json"),
      None,
      None,
      Vector(ComponentVersionEvidence("1.0.0", Some("0.5.1"), Vector.empty, None, None, false)),
      Vector.empty
    )

  private object EmptyCatalogFetcher extends CatalogFetcher {
    def get(uri: URI): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
  }
}
