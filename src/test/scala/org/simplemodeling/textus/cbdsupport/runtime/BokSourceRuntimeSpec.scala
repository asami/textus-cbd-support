package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant, ZoneOffset}
import scala.collection.mutable.ArrayBuffer

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokSourceRuntimeSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BokSourceConfig" should {
    "admit only bounded explicitly allowlisted BoK origins with unique source IDs" in {
      Given("allowlisted, duplicate, credential-bearing, reserved, and non-allowlisted BoK candidates")
      val sites = Some(
        "knowledgehub=https://bok.example/knowledge/," +
          "knowledgehub=https://bok.example/duplicate/," +
          "secret=https://user:password@bok.example/private/," +
          "local-car=https://bok.example/local/," +
          "catalog-source=https://bok.example/catalog/," +
          "outside=https://outside.example/"
      )

      When("the bounded BoK source configuration is parsed")
      val configuration = BokSourceConfig.parse(
        sites,
        Some("https://bok.example"),
        reservedsourceids = Set("simplemodeling", "local-car", "cache-car", "catalog-source"),
        policy = BokInspectionPolicy(maxSources = 6)
      )

      Then("only the first unique authorized source becomes a BoK descriptor")
      configuration.sources.map(_.id) shouldBe Vector("knowledgehub")
      configuration.sources.head.descriptor.sourceKind shouldBe InformationSourceKind.BOK_SITE
      configuration.sources.head.descriptor.authorization shouldBe InformationSourceAuthorization.EXACT_ORIGIN_ALLOWLIST
      configuration.warnings.exists(_.contains("reserved or duplicated")) shouldBe true
      configuration.warnings.exists(_.contains("not allowlisted")) shouldBe true
      configuration.warnings.mkString should not include "password"
    }
  }

  "BokKnowledgeSourceProvider" should {
    "read the Cozy KnowledgeSource manifest and preserve configured and publisher identities separately" in {
      Given("an authorized BoK site with one manifest-declared glossary resource")
      val source = _source("configured-bok")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val termsuri = source.baseUri.resolve("metadata/glossary/terms.json")
      val manifest =
        """{
          |  "schemaVersion":"cncf.knowledge-source.v1",
          |  "kind":"bok-site",
          |  "id":"publisher-bok",
          |  "label":"Publisher BoK",
          |  "sourceRef":{"kind":"bok-site","value":"publisher-bok","uri":"https://bok.example/knowledge/"},
          |  "resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"}]
          |}""".stripMargin
      val terms =
        """{"terms":[{"id":"bounded-context","slug":"bounded-context","title":"Bounded Context","summary":"A model boundary.","aliases":["BC"],"term_type":"concept","term_refs":["domain-model"]}]}"""
      val fetcher = new MemoryBokFetcher(Map(manifesturi -> manifest, termsuri -> terms))
      val provider = new BokKnowledgeSourceProvider(_clock)

      When("the machine-readable KnowledgeSource is read")
      val snapshot = provider.read(source, fetcher).toOption.get

      Then("the term retains configured source, publisher manifest, and resource evidence without identity merging")
      snapshot.source.id shouldBe "configured-bok"
      snapshot.manifestId shouldBe "publisher-bok"
      snapshot.sourceRefValue shouldBe Some("publisher-bok")
      snapshot.observedAt shouldBe Instant.parse("2026-07-14T00:00:00Z")
      snapshot.warnings.exists(_.contains("both identities remain separate")) shouldBe true
      snapshot.terms should have size 1
      snapshot.terms.head.sourceId shouldBe "configured-bok"
      snapshot.terms.head.manifestId shouldBe "publisher-bok"
      snapshot.terms.head.termId shouldBe "bounded-context"
      snapshot.terms.head.termRefs shouldBe Vector("domain-model")
      snapshot.terms.head.evidenceLocation shouldBe s"$termsuri#/terms/0"

      And("only the fixed manifest and its declared machine-readable resource are fetched with byte bounds")
      fetcher.requests.map(_._1) shouldBe Vector(manifesturi, termsuri)
      fetcher.requests.map(_._2) shouldBe Vector(BokInspectionPolicy.DEFAULT.maxManifestBytes, BokInspectionPolicy.DEFAULT.maxResourceBytes)
      fetcher.requestedSourceIds.distinct shouldBe Vector(source.id)
    }

    "reject unsafe or non-JSON resource declarations before fetching them" in {
      Given("a valid manifest containing one glossary resource and unsafe, cross-origin, and HTML declarations")
      val source = _source("safe-bok")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val termsuri = source.baseUri.resolve("metadata/glossary/terms.json")
      val manifest =
        """{
          |  "schemaVersion":"cncf.knowledge-source.v1",
          |  "kind":"bok-site",
          |  "id":"safe-bok",
          |  "sourceRef":{"kind":"bok-site","value":"safe-bok"},
          |  "resources":[
          |    {"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"},
          |    {"kind":"glossary-terms","href":"../secret.json","mediaType":"application/json"},
          |    {"kind":"glossary-terms","href":"https://outside.example/terms.json","mediaType":"application/json"},
          |    {"kind":"glossary-terms","href":"terms.html","mediaType":"text/html"}
          |  ]
          |}""".stripMargin
      val fetcher = new MemoryBokFetcher(Map(manifesturi -> manifest, termsuri -> "{\"terms\":[]}"))

      When("the site is inspected")
      val snapshot = new BokKnowledgeSourceProvider(_clock).read(source, fetcher).toOption.get

      Then("only the safe JSON resource is fetched and every rejected declaration is observable")
      fetcher.requests.map(_._1) shouldBe Vector(manifesturi, termsuri)
      snapshot.resources.map(_.uri) shouldBe Vector(termsuri)
      snapshot.warnings.count(_.contains("resource")) should be >= 3
    }

    "fail an incompatible manifest without scraping a rendered page or guessing a glossary path" in {
      Given("a BoK site whose fixed manifest path returns an unsupported schema")
      val source = _source("legacy-bok")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val fetcher = new MemoryBokFetcher(Map(
        manifesturi -> "{\"schemaVersion\":\"legacy.v0\",\"kind\":\"bok-site\",\"id\":\"legacy\",\"resources\":[]}"
      ))

      When("the provider reads the configured site")
      val result = new BokKnowledgeSourceProvider(_clock).read(source, fetcher)

      Then("the source fails explicitly after only the canonical manifest request")
      result.toOption shouldBe None
      fetcher.requests.map(_._1) shouldBe Vector(manifesturi)
    }

    "reject a manifest whose sourceRef contradicts the canonical BoK identity contract" in {
      Given("a v1 manifest whose source reference kind is not bok-site")
      val source = _source("wrong-reference")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val manifest =
        """{"schemaVersion":"cncf.knowledge-source.v1","kind":"bok-site","id":"wrong-reference","sourceRef":{"kind":"catalog","value":"wrong-reference"},"resources":[]}"""
      val fetcher = new MemoryBokFetcher(Map(manifesturi -> manifest))

      When("the provider validates the canonical manifest")
      val result = new BokKnowledgeSourceProvider(_clock).read(source, fetcher)

      Then("the incompatible publisher identity fails before any resource request")
      result.toOption shouldBe None
      fetcher.requests.map(_._1) shouldBe Vector(manifesturi)
    }

    "bound resource and term discovery while keeping truncation observable" in {
      Given("a manifest with two resources and a glossary containing two terms")
      val source = _source("bounded-bok")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val firsturi = source.baseUri.resolve("metadata/glossary/first.json")
      val manifest =
        """{"schemaVersion":"cncf.knowledge-source.v1","kind":"bok-site","id":"bounded-bok","sourceRef":{"kind":"bok-site","value":"bounded-bok"},"resources":[
          |{"kind":"glossary-terms","href":"metadata/glossary/first.json","mediaType":"application/json"},
          |{"kind":"glossary-terms","href":"metadata/glossary/second.json","mediaType":"application/json"}
          |]}""".stripMargin
      val terms = """{"terms":[{"id":"a","title":"A"},{"id":"b","title":"B"}]}"""
      val fetcher = new MemoryBokFetcher(Map(manifesturi -> manifest, firsturi -> terms))
      val policy = BokInspectionPolicy(maxResources = 1, maxTerms = 1)

      When("the bounded provider reads the source")
      val snapshot = new BokKnowledgeSourceProvider(_clock).read(source, fetcher, policy).toOption.get

      Then("only the admitted resource and term are read and both bounds are reported")
      fetcher.requests.map(_._1) shouldBe Vector(manifesturi, firsturi)
      snapshot.resources should have size 1
      snapshot.terms.map(_.termId) shouldBe Vector("a")
      snapshot.warnings.exists(_.contains("resources were truncated")) shouldBe true
      snapshot.warnings.exists(_.contains("terms were truncated")) shouldBe true
    }

    "preserve duplicate term observations without selecting a hidden winner" in {
      Given("one machine-readable glossary with duplicate case-insensitive term identities")
      val source = _source("duplicate-bok")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val termsuri = source.baseUri.resolve("metadata/glossary/terms.json")
      val manifest =
        """{"schemaVersion":"cncf.knowledge-source.v1","kind":"bok-site","id":"duplicate-bok","sourceRef":{"kind":"bok-site","value":"duplicate-bok"},"resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"}]}"""
      val terms = """{"terms":[{"id":"DDD","title":"Domain-Driven Design"},{"id":"ddd","title":"Duplicate"}]}"""
      val fetcher = new MemoryBokFetcher(Map(manifesturi -> manifest, termsuri -> terms))

      When("the glossary is read")
      val snapshot = new BokKnowledgeSourceProvider(_clock).read(source, fetcher).toOption.get

      Then("both observations and an unresolved-duplicate diagnostic remain visible")
      snapshot.terms.map(_.termId) shouldBe Vector("DDD", "ddd")
      snapshot.terms.forall(_.diagnostics.exists(_.contains("without selecting a winner"))) shouldBe true
      snapshot.warnings.exists(_.contains("Duplicate glossary term identity")) shouldBe true
    }

    "surface a fetch-enforced resource byte limit without losing the valid manifest" in {
      Given("a valid manifest whose glossary response exceeds the configured fetch limit")
      val source = _source("oversized-bok")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val termsuri = source.baseUri.resolve("metadata/glossary/terms.json")
      val manifest =
        """{"schemaVersion":"cncf.knowledge-source.v1","kind":"bok-site","id":"oversized-bok","sourceRef":{"kind":"bok-site","value":"oversized-bok"},"resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"}]}"""
      val fetcher = new MemoryBokFetcher(Map(manifesturi -> manifest, termsuri -> "{\"terms\":[{\"id\":\"too-large\"}]}"))
      val policy = BokInspectionPolicy(maxResourceBytes = 16)

      When("the bounded fetcher refuses the glossary body")
      val snapshot = new BokKnowledgeSourceProvider(_clock).read(source, fetcher, policy).toOption.get

      Then("the source snapshot survives with explicit missing term evidence and no unbounded retry")
      snapshot.terms shouldBe empty
      snapshot.warnings.exists(_.contains("exceeds 16 bytes")) shouldBe true
      fetcher.requests should contain(termsuri -> 16)
    }
  }

  "CbdRuntime" should {
    "initialize configured BoK sources through the federated production boundary" in {
      Given("one catalog runtime and one authorized BoK source with machine-readable glossary evidence")
      val catalog = CatalogSource("catalog", URI.create("https://catalog.example/"), 100, true)
      val source = _source("runtime-bok")
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val termsuri = source.baseUri.resolve("metadata/glossary/terms.json")
      val manifest =
        """{"schemaVersion":"cncf.knowledge-source.v1","kind":"bok-site","id":"runtime-bok","sourceRef":{"kind":"bok-site","value":"runtime-bok"},"resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"}]}"""
      val terms = """{"terms":[{"id":"domain-model","title":"Domain Model"}]}"""
      val fetcher = new MemoryBokFetcher(Map(manifesturi -> manifest, termsuri -> terms))
      val runtime = CbdRuntime.createFederated(
        Vector(catalog),
        new InMemoryComponentCatalogProvider(Vector.empty),
        CatalogCachePolicy.DEFAULT,
        _clock,
        Vector(source),
        new BokKnowledgeSourceProvider(_clock)
      )

      When("the federated input boundary is initialized")
      val result = runtime.ensureInputsReady(fetcher)

      Then("BoK terms and source readiness become part of the live CBD runtime")
      result.isSuccess shouldBe true
      runtime.bokTerms.map(_.termId) shouldBe Vector("domain-model")
      runtime.informationSourceStates(includeDisabled = false).map(_.descriptor.id) shouldBe Vector("catalog", "runtime-bok")
      runtime.bokSourceStates(includeDisabled = false).head.status shouldBe "ready"
      runtime.overallStatus shouldBe "ready"
    }
  }

  private val _clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC)

  private def _source(id: String): BokSource =
    BokSource(id, URI.create("https://bok.example/knowledge/"), 600, true)

  private final class MemoryBokFetcher(responses: Map[URI, String]) extends CatalogFetcher with BokFetcher {
    val requests = ArrayBuffer.empty[(URI, Int)]
    var requestedSourceIds = Vector.empty[String]

    def get(uri: URI): Consequence[String] =
      responses.get(uri).map(Consequence.success).getOrElse(Consequence.serviceUnavailable(s"No fixture for $uri"))

    override def get(uri: URI, maxbytes: Int): Consequence[String] = {
      requests += uri -> maxbytes
      responses.get(uri) match {
        case None => Consequence.serviceUnavailable(s"No fixture for $uri")
        case Some(body) if body.getBytes(StandardCharsets.UTF_8).length > maxbytes =>
          Consequence.serviceUnavailable(s"Response exceeds $maxbytes bytes.")
        case Some(body) => Consequence.success(body)
      }
    }

    override def get(source: BokSource, uri: URI, maxbytes: Int): Consequence[String] = {
      requestedSourceIds = requestedSourceIds :+ source.id
      get(uri, maxbytes)
    }
  }
}
