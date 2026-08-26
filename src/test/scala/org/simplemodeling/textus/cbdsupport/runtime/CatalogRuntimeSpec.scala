package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.time.{Clock, Duration, Instant, ZoneId, ZoneOffset}
import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.io.Source

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class CatalogRuntimeSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC)

  "CatalogSourceConfig" should {
    "authorize configured catalogs only through an exact origin allowlist" in {
      Given("configured HTTPS, HTTP, credential-bearing, and duplicate sources with a mixed allowlist")
      val catalogs = Some(
        "team=https://catalog.example/team/," +
          "insecure=http://catalog.example/team/," +
          "secret=https://user:password@catalog.example/private/," +
          "team=https://catalog.example/duplicate/"
      )
      val allowedorigins = Some(
        "https://catalog.example/," +
          "https://catalog.example/not-an-origin/," +
          "not-a-uri"
      )

      When("the source configuration is parsed")
      val configuration = CatalogSourceConfig.parse(catalogs, allowedorigins)

      Then("only the first source on the allowlisted origin is accepted and every rejection is observable")
      configuration.sources.map(_.id) shouldBe Vector("simplemodeling", "team")
      configuration.sources.last.baseUri.toString shouldBe "https://catalog.example/team/"
      configuration.sources.head.descriptor shouldBe InformationSourceDescriptor(
        "simplemodeling",
        InformationSourceKind.PUBLISHED_CATALOG,
        "https://www.simplemodeling.org/",
        100,
        true,
        InformationSourceAuthorization.BUILT_IN
      )
      configuration.sources.last.authorization shouldBe InformationSourceAuthorization.EXACT_ORIGIN_ALLOWLIST
      InformationSourceKind.ALL shouldBe Vector(
        "published-catalog",
        "bok-site",
        "sie-bok",
        "development-directory",
        "car-storage"
      )
      configuration.warnings.exists(_.contains("not an origin without a path")) shouldBe true
      configuration.warnings.exists(_.contains("not a valid HTTP(S) origin")) shouldBe true
      configuration.warnings.exists(_.contains("origin http://catalog.example is not allowlisted")) shouldBe true
      configuration.warnings.exists(_.contains("base URI is invalid")) shouldBe true
      configuration.warnings.exists(_.contains("source ID is duplicated")) shouldBe true
      configuration.warnings.mkString(" ") should not include "password"
      val defaultsources: Vector[CatalogSource] = CatalogSourceConfig.parse(None, None).sources
      defaultsources.map(_.id) should contain("simplemodeling")
    }
  }

  "CozyComponentCatalogProvider" should {
    "refuse to fetch model metadata outside the catalog origin" in {
      Given("an authorized catalog whose selected version points to a cross-origin model-metadata sidecar")
      val source = CatalogSource("fixture", URI.create("https://catalog.example/"), 100, true)
      val sidecar = URI.create("https://untrusted.example/textus-order.model-metadata.json")
      val carindex =
        """{
          |  "entries": [{
          |    "artifact_id": "textus-order",
          |    "recommended": "1.2.0",
          |    "versions": [{
          |      "version": "1.2.0",
          |      "sidecars": {"model_metadata_json": "https://untrusted.example/textus-order.model-metadata.json"}
          |    }]
          |  }]
          |}""".stripMargin
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("metadata/repository/car/index.json") -> carindex,
        source.baseUri.resolve("metadata/repository/sar/index.json") -> """{"entries": []}"""
      ))
      val provider = new CozyComponentCatalogProvider(clock = _clock)
      val profile = provider.read(source, fetcher).toOption.get.profiles.head

      When("usage evidence is read")
      val usage = provider.readUsage(source, profile, fetcher).toOption.get

      Then("the sidecar remains visible as evidence but no cross-origin request is made")
      profile.modelMetadataUri shouldBe Some(sidecar)
      fetcher.requestedUris should not contain sidecar
      usage.operations shouldBe empty
      usage.warnings.exists(_.contains("origin differs from the catalog")) shouldBe true
    }

    "consume Cozy's generated CAR index and model metadata contract" in {
      Given("a byte-for-byte Cozy publisher index capture with version, runtime, archive, sidecar, and diagnostic evidence")
      val source = CatalogSource("fixture", URI.create("https://catalog.example/"), 100, true)
      val carindex = _resource_text("/catalog/cozy-repository-car-index.json")
      val sarindex = """{"entries": []}"""
      val modelmetadata =
        """{
          |  "surface": {
          |    "component": {
          |      "services": [{
          |        "name": "OrderQuery",
          |        "operations": [{
          |          "name": "getOrder",
          |          "kind": "query",
          |          "description": "Return one order."
          |        }]
          |      }]
          |    }
          |  }
          |} """.stripMargin
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("metadata/repository/car/index.json") -> carindex,
        source.baseUri.resolve("metadata/repository/sar/index.json") -> sarindex,
        source.baseUri.resolve("repository/catalog/car/textus-georesolver.model-metadata.json") -> modelmetadata
      ))
      val provider = new CozyComponentCatalogProvider(clock = _clock)

      When("the catalog and selected component usage are read")
      val snapshot = provider.read(source, fetcher).toOption.get
      val profile = snapshot.profiles.head
      val detailed = profile.selectVersion("0.2.0")
      val usage = provider.readUsage(profile, fetcher).toOption.get

      Then("the provider preserves authoritative version, runtime, dependency, and operation evidence")
      profile.name shouldBe "textus-georesolver"
      profile.selectedVersion shouldBe Some("0.1.0")
      profile.dependencyMetadataVersion shouldBe None
      profile.versionEvidence.map(_.version) shouldBe Vector("0.1.0", "0.2.0")
      profile.latestStable shouldBe Some("0.2.0")
      profile.selectedChannel shouldBe Some("stable")
      profile.selectedStatus shouldBe Some("active")
      profile.selectedComponent shouldBe Some("textus-georesolver")
      profile.selectedPublishedAt shouldBe Some("2026-07-05T20:54:28.506716+09:00")
      profile.runtimeMinimum shouldBe Some("0.4.13")
      profile.runtimeMaximum shouldBe None
      profile.runtimeTested shouldBe Vector("0.4.13")
      profile.tags shouldBe empty
      profile.dependencies shouldBe empty
      profile.artifactUri.map(_.toString) shouldBe Some(
        "https://catalog.example/repository/car/textus-georesolver/0.1.0/textus-georesolver-0.1.0.car"
      )
      profile.artifactChecksumSha256 shouldBe Some(
        "7757268b5f0c986e2cb902310a2eb9ecbbf4091bbc48ad54d5293d2a5b7074d9"
      )
      profile.versionEvidence.find(_.version == "0.1.0").exists(_.hasDependencyMetadata) shouldBe false
      detailed.dependencyMetadataVersion shouldBe Some("0.2.0")
      detailed.runtimeMinimum shouldBe Some("0.5.0")
      detailed.dependencies shouldBe empty
      detailed.artifactChecksumSha256 shouldBe Some(
        "5561f32790e4edb211f5dce85050b2407439bf06ebe9052acfed7a979c6d975e"
      )
      snapshot.warning.exists(_.contains("catalog-without-project for textus-georesolver")) shouldBe true
      usage.operations shouldBe Vector(ComponentOperation(Some("OrderQuery"), "getOrder", Some("query"), Some("Return one order.")))
      fetcher.requestedSourceIds.distinct shouldBe Vector(source.id)
    }

    "admit Component knowledge only from the selected version's canonical catalog route" in {
      Given("one CAR version with a canonical carrier and one version with a noncanonical contract path")
      val source = CatalogSource("fixture", URI.create("https://catalog.example/"), 100, true)
      val digest = "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd"
      val carindex =
        s"""{
           |  "entries": [{
           |    "artifact_id": "textus-order",
           |    "recommended": "1.2.0",
           |    "versions": [
           |      {
           |        "version": "1.2.0",
           |        "component": "org.example.Order",
           |        "component_knowledge": {
           |          "carrier": {
           |            "carrierSchema": "cncf.component-knowledge-carrier.v1",
           |            "consumerContractSchema": "cncf.component-knowledge-consumer.v1",
           |            "logicalPath": "component-knowledge.json",
           |            "sha256": "$digest"
           |          },
           |          "consumer_contract": "repository/car/textus-order/1.2.0/component-knowledge.json"
           |        }
           |      },
           |      {
           |        "version": "1.3.0",
           |        "component": "org.example.Order",
           |        "component_knowledge": {
           |          "carrier": {
           |            "carrierSchema": "cncf.component-knowledge-carrier.v1",
           |            "consumerContractSchema": "cncf.component-knowledge-consumer.v1",
           |            "logicalPath": "component-knowledge.json",
           |            "sha256": "$digest"
           |          },
           |          "consumer_contract": "repository/car/textus-order/component-knowledge.json"
           |        }
           |      }
           |    ]
           |  }]
           |}""".stripMargin
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("metadata/repository/car/index.json") -> carindex,
        source.baseUri.resolve("metadata/repository/sar/index.json") -> """{"entries": []}"""
      ))

      When("the Cozy catalog is parsed and each version is selected")
      val profile = new CozyComponentCatalogProvider(clock = _clock).read(source, fetcher).toOption.get.profiles.head
      val canonical = profile.selectVersion("1.2.0")
      val generic = profile.selectVersion("1.3.0")

      Then("only the exact selected CAR version supplies a same-origin consumer-contract endpoint")
      canonical.componentKnowledge.map(_.consumerContractUri) shouldBe Some(
        URI.create("https://catalog.example/repository/car/textus-order/1.2.0/component-knowledge.json")
      )
      canonical.componentKnowledge.map(_.carrier.sha256) shouldBe Some(digest)
      generic.componentKnowledge shouldBe None
    }
  }

  "CompatibleComponentCatalogProvider" should {
    "reject an incompatible rich index without reinterpreting it as a publication catalog" in {
      Given("a declared but unsupported rich-index schema beside an otherwise usable publication page")
      val source = CatalogSource("incompatible", URI.create("https://catalog.example/"), 100, true)
      val carindex = """{"schemaVersion":"cozy.repository-index.v2","entries":[]}"""
      val publication = """<a href="textus-order/metadata.html">Metadata</a>"""
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("metadata/repository/car/index.json") -> carindex,
        source.baseUri.resolve("en/catalog/index.html") -> publication
      ))
      val provider = new CompatibleComponentCatalogProvider(
        new CozyComponentCatalogProvider(clock = _clock),
        new SimpleModelingPublicationCatalogProvider(clock = _clock)
      )

      When("the compatibility provider classifies the available rich contract")
      val result = provider.read(source, fetcher)

      Then("the source fails closed and the older publication contract is not probed")
      result match {
        case Consequence.Failure(conclusion) => conclusion.display should include("publication fallback was not attempted")
        case Consequence.Success(_) => fail("An incompatible rich catalog was accepted.")
      }
      fetcher.requestedUris should not contain source.baseUri.resolve("en/catalog/index.html")
    }

    "consume the deployed publication catalog and repository artifact metadata" in {
      Given("a publication catalog page linked to one CAR project")
      val source = CatalogSource("simplemodeling", URI.create("https://www.simplemodeling.org/"), 100, true)
      val catalog =
        """<html><body>
          |<a href="textus-order/metadata.html">Metadata</a>
          |<a href="textus-tutorial/metadata.html">Metadata</a>
          |<a href="maven-repository/metadata.html">Metadata</a>
          |</body></html>""".stripMargin
      val catalogproject =
        """{
          |  "schema": "cozy.publish-project.v1",
          |  "type": "catalog-project",
          |  "project": {"name": "textus-order", "kind": "car"}
          |}""".stripMargin
      val artifact =
        """{
          |  "schema": "cozy.publish-project.v1",
          |  "type": "repository-artifact",
          |  "project": {
          |    "name": "textus-order",
          |    "title": "Textus Order",
          |    "kind": "car",
          |    "organization": "org.textus",
          |    "version": "1.2.0",
          |    "summary": "Order component for CBD reuse."
          |  },
          |  "artifact": {
          |    "kinds": [{"type": "car", "versions": ["1.2.0"], "latestRelease": "1.2.0"}],
          |    "files": [{
          |      "type": "car",
          |      "version": "1.2.0",
          |      "publicPath": "repository/car/textus-order/1.2.0/textus-order-1.2.0.car"
          |    }]
          |  }
          |}""".stripMargin
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("en/catalog/index.html") -> catalog,
        source.baseUri.resolve("metadata/catalog/projects/textus-order.json") -> catalogproject,
        source.baseUri.resolve("metadata/catalog/projects/textus-tutorial.json") ->
          """{"project":{"name":"textus-tutorial","kind":"documentation"}}""",
        source.baseUri.resolve("metadata/artifacts/repository/textus-order.json") -> artifact
      ))
      val provider = new CompatibleComponentCatalogProvider(
        new CozyComponentCatalogProvider(clock = _clock),
        new SimpleModelingPublicationCatalogProvider(clock = _clock)
      )

      When("the publication catalog is read")
      val snapshot = provider.read(source, fetcher).toOption.get
      val profile = snapshot.profiles.head
      val usage = provider.readUsage(profile, fetcher).toOption.get

      Then("the CAR evidence is explicit and non-component catalog projects are ignored")
      profile.identity shouldBe "org.textus:textus-order"
      profile.versions shouldBe Vector("1.2.0")
      profile.selectedVersion shouldBe Some("1.2.0")
      profile.dependencyMetadataVersion shouldBe None
      profile.versionEvidence.map(_.version) shouldBe Vector("1.2.0")
      profile.artifactUri.map(_.toString) shouldBe Some(
        "https://www.simplemodeling.org/repository/car/textus-order/1.2.0/textus-order-1.2.0.car"
      )
      profile.documentationUri.map(_.toString) shouldBe Some(
        "https://www.simplemodeling.org/en/catalog/textus-order/index.html"
      )
      snapshot.warning shouldBe None
      usage.operations shouldBe empty
      usage.warnings.exists(_.contains("operation metadata")) shouldBe true
    }

    "preserve the supported unversioned publication contract and report entry failures" in {
      Given("a legacy unversioned publication catalog with one snapshot component and one unreadable entry")
      val source = CatalogSource("publication", URI.create("https://catalog.example/"), 100, true)
      val catalog =
        """<html><body>
          |<a href="textus-snapshot/metadata.html">Metadata</a>
          |<a href="broken-component/metadata.html">Metadata</a>
          |</body></html>""".stripMargin
      val catalogproject =
        """{
          |  "project": {
          |    "name": "textus-snapshot",
          |    "kind": "car"
          |  }
          |}""".stripMargin
      val artifact =
        """{
          |  "project": {
          |    "name": "textus-snapshot",
          |    "kind": "car",
          |    "version": "2.0.0-SNAPSHOT"
          |  },
          |  "artifact": {
          |    "kinds": [{"type": "car", "versions": ["2.0.0-SNAPSHOT"]}],
          |    "files": [{
          |      "type": "car",
          |      "version": "2.0.0-SNAPSHOT",
          |      "publicPath": "repository/car/textus-snapshot/2.0.0-SNAPSHOT/textus-snapshot-2.0.0-SNAPSHOT.car"
          |    }]
          |  }
          |}""".stripMargin
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("en/catalog/index.html") -> catalog,
        source.baseUri.resolve("metadata/catalog/projects/textus-snapshot.json") -> catalogproject,
        source.baseUri.resolve("metadata/artifacts/repository/textus-snapshot.json") -> artifact
      ))
      val provider = new SimpleModelingPublicationCatalogProvider(clock = _clock)

      When("the partially available publication catalog is read")
      val snapshot = provider.read(source, fetcher).toOption.get
      val profile = snapshot.profiles.head

      Then("the snapshot remains non-stable and the missing entry is observable")
      profile.latestStable shouldBe None
      profile.latestSnapshot shouldBe Some("2.0.0-SNAPSHOT")
      snapshot.warning.exists(_.contains("broken-component")) shouldBe true
    }

    "reject a declared unsupported publication schema" in {
      Given("a publication catalog entry whose project document declares an unknown schema")
      val source = CatalogSource("publication", URI.create("https://catalog.example/"), 100, true)
      val catalog = """<a href="textus-order/metadata.html">Metadata</a>"""
      val catalogproject =
        """{"schema":"cozy.publish-project.v2","type":"catalog-project","project":{"name":"textus-order","kind":"car"}}"""
      val artifacturi = source.baseUri.resolve("metadata/artifacts/repository/textus-order.json")
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("en/catalog/index.html") -> catalog,
        source.baseUri.resolve("metadata/catalog/projects/textus-order.json") -> catalogproject
      ))

      When("the publication provider validates the declared contract")
      val result = new SimpleModelingPublicationCatalogProvider(clock = _clock).read(source, fetcher)

      Then("the entry is rejected before artifact metadata can be guessed")
      result.toOption shouldBe None
      fetcher.requestedUris should not contain artifacturi
    }

    "avoid reusing another publication version's artifact" in {
      Given("a publication profile whose selected release has no matching artifact file")
      val source = CatalogSource("publication", URI.create("https://catalog.example/"), 100, true)
      val catalog = """<a href="textus-versioned/metadata.html">Metadata</a>"""
      val catalogproject = """{"project":{"name":"textus-versioned","kind":"car"}}"""
      val artifact =
        """{
          |  "project": {"name":"textus-versioned","kind":"car","version":"2.0.0"},
          |  "artifact": {
          |    "kinds": [{"type":"car","versions":["1.0.0","2.0.0"],"latestRelease":"2.0.0"}],
          |    "files": [{
          |      "type":"car",
          |      "version":"1.0.0",
          |      "publicPath":"repository/car/textus-versioned/1.0.0/textus-versioned-1.0.0.car"
          |    }]
          |  }
          |}""".stripMargin
      val fetcher = new MapCatalogFetcher(Map(
        source.baseUri.resolve("en/catalog/index.html") -> catalog,
        source.baseUri.resolve("metadata/catalog/projects/textus-versioned.json") -> catalogproject,
        source.baseUri.resolve("metadata/artifacts/repository/textus-versioned.json") -> artifact
      ))
      val provider = new SimpleModelingPublicationCatalogProvider(clock = _clock)

      When("the selected release profile is read")
      val profile = provider.read(source, fetcher).toOption.get.profiles.head

      Then("the older artifact is not attributed to the selected release")
      profile.selectedVersion shouldBe Some("2.0.0")
      profile.artifactUri shouldBe None
      profile.warnings.exists(_.contains("does not publish an artifact path")) shouldBe true
      profile.selectVersion("1.0.0").artifactUri.map(_.toString) shouldBe Some(
        "https://catalog.example/repository/car/textus-versioned/1.0.0/textus-versioned-1.0.0.car"
      )
    }
  }

  "CbdRuntime" should {
    "search Japanese catalog terms through an in-memory provider" in {
      Given("an in-memory catalog profile tagged with a Japanese CBD term")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val profile = _component_profile(source.id).copy(terms = Vector("受注管理"))
      val runtime = CbdRuntime.create(Vector(source), new InMemoryComponentCatalogProvider(Vector(profile), clock = _clock), _clock)

      When("the catalog is loaded and searched by that term")
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      val results = runtime.search("受注管理", None, Some("car"), None, Some("0.5.1"), 10)

      Then("the matching component is returned with catalog evidence")
      results.map(_.profile.name) shouldBe Vector("textus-order")
      results.head.matchKind shouldBe "candidate"
    }

    "exclude components without runtime compatibility evidence from constrained search" in {
      Given("one catalog profile with runtime evidence and one without it")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val supported = _component_profile(source.id).copy(terms = Vector("order"))
      val unknown = _component_profile(source.id).copy(
        name = "textus-order-unknown",
        title = "Textus Order Unknown",
        terms = Vector("order"),
        runtimeMinimum = None
      )
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(supported, unknown), clock = _clock),
        _clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("search constrains results to a concrete CNCF runtime version")
      val results = runtime.search("order", None, Some("car"), None, Some("0.5.1"), 10)

      Then("only the component with affirmative compatibility evidence is returned")
      results.map(_.profile.name) shouldBe Vector("textus-order")
    }

    "refresh a catalog when its explicit bounded schedule becomes due" in {
      Given("a ten-minute cache lifetime, a five-minute refresh interval, and a controllable clock")
      val source = CatalogSource("switchable", URI.create("https://switchable.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      val clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"))
      val runtime = CbdRuntime.create(
        Vector(source),
        provider,
        CatalogCachePolicy(
          Duration.ofMinutes(10),
          InformationSourceRefreshPolicy(Duration.ofMinutes(5))
        ),
        clock
      )

      When("readiness is checked before and at the refresh boundary while the snapshot remains fresh")
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      clock.advance(Duration.ofMinutes(4))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      provider.readCount shouldBe 1
      clock.advance(Duration.ofMinutes(1))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      val state = runtime.sourceStates(includeDisabled = false).head

      Then("the snapshot is reused before the schedule and refreshed when the explicit interval is due")
      provider.readCount shouldBe 2
      state.status shouldBe "ready"
      state.cacheStatus shouldBe "fresh"
      state.refreshedAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:15:00Z"))
      state.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.nextRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:10:00Z"))
    }

    "allow an administrative refresh to bypass the normal schedule" in {
      Given("a fresh catalog whose next normal attempt is still four minutes away")
      val source = CatalogSource("switchable", URI.create("https://switchable.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      val clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"))
      val runtime = CbdRuntime.create(
        Vector(source),
        provider,
        CatalogCachePolicy(
          Duration.ofMinutes(10),
          InformationSourceRefreshPolicy(Duration.ofMinutes(5))
        ),
        clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      clock.advance(Duration.ofMinutes(1))

      When("the selected source is explicitly refreshed through administration")
      runtime.refresh(Some(source.id), EmptyCatalogFetcher).isSuccess shouldBe true
      val state = runtime.sourceStates(includeDisabled = false).head

      Then("the source is read immediately and its normal schedule restarts from that observation")
      provider.readCount shouldBe 2
      state.refreshedAt shouldBe Some(Instant.parse("2026-07-14T00:01:00Z"))
      state.nextRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:06:00Z"))
    }

    "back off repeated automatic failures while preserving a stale last-known-good snapshot" in {
      Given("a provider that fails repeatedly after its first snapshot reaches cache expiry")
      val source = CatalogSource("switchable", URI.create("https://switchable.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      val clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"))
      val runtime = CbdRuntime.create(
        Vector(source),
        provider,
        CatalogCachePolicy(Duration.ofMinutes(5)),
        clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      clock.advance(Duration.ofMinutes(5))
      provider.fail = true

      When("readiness reaches each exponentially deferred retry boundary")
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      runtime.sourceStates(includeDisabled = false).head.nextRefreshAttemptAt shouldBe
        Some(Instant.parse("2026-07-14T00:06:00Z"))
      clock.advance(Duration.ofMinutes(1))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      runtime.sourceStates(includeDisabled = false).head.nextRefreshAttemptAt shouldBe
        Some(Instant.parse("2026-07-14T00:08:00Z"))
      clock.advance(Duration.ofMinutes(2))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      runtime.sourceStates(includeDisabled = false).head.nextRefreshAttemptAt shouldBe
        Some(Instant.parse("2026-07-14T00:12:00Z"))
      clock.advance(Duration.ofMinutes(4))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      runtime.sourceStates(includeDisabled = false).head.nextRefreshAttemptAt shouldBe
        Some(Instant.parse("2026-07-14T00:17:00Z"))
      clock.advance(Duration.ofMinutes(5))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      val state = runtime.sourceStates(includeDisabled = false).head

      Then("retry delays double from one through four minutes and remain capped at five")
      provider.readCount shouldBe 6
      state.status shouldBe "degraded"
      state.cacheStatus shouldBe "stale"
      state.componentCount shouldBe 1
      state.refreshedAt shouldBe Some(Instant.parse("2026-07-14T00:00:00Z"))
      state.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:17:00Z"))
      state.nextRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:22:00Z"))
      state.warning.exists(_.contains("Catalog unavailable")) shouldBe true
      runtime.get("textus-order", None, None, None, None) should not be empty
    }

    "bound retained profiles across sources without evicting attributable last-known-good evidence" in {
      Given("two catalog snapshots whose combined profiles exceed the runtime retention limit")
      val sources = Vector(
        CatalogSource("retained-a", URI.create("https://retained-a.example/"), 100, true),
        CatalogSource("retained-b", URI.create("https://retained-b.example/"), 200, true)
      )
      val profiles = Map(
        sources.head.id -> Vector(
          _component_profile(sources.head.id).copy(name = "a-one", title = "A One"),
          _component_profile(sources.head.id).copy(name = "a-two", title = "A Two")
        ),
        sources.last.id -> Vector(
          _component_profile(sources.last.id).copy(name = "b-one", title = "B One"),
          _component_profile(sources.last.id).copy(name = "b-two", title = "B Two")
        )
      )
      val provider = new FailingPerSourceCatalogProvider(profiles)
      val runtime = CbdRuntime.createFederated(
        sources,
        provider,
        CatalogCachePolicy.DEFAULT,
        Clock.systemUTC(),
        Vector.empty,
        new BokKnowledgeSourceProvider(_clock),
        siebokprovider = new SieBokProvider(_clock),
        retentionpolicy = InformationSourceRetentionPolicy(maxCatalogObservations = 3)
      )

      When("initial readiness fills the bound and the truncated source later fails to refresh")
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      val retainedstates = runtime.sourceStates(includeDisabled = false)
      provider.failSource(sources.last.id)
      runtime.refresh(Some(sources.last.id), EmptyCatalogFetcher).isSuccess shouldBe true

      Then("the first source keeps two profiles and the second keeps one attributable last-known-good profile")
      retainedstates.map(_.componentCount) shouldBe Vector(2, 1)
      retainedstates.last.warning.get should include("runtime total policy limit of 3 observations")
      runtime.componentCount shouldBe 3
      Vector("a-one", "a-two", "b-one").forall(name => runtime.get(name, None, None, None, None).nonEmpty) shouldBe true
      runtime.get("b-two", None, None, None, None) shouldBe None
      val failedstate = runtime.sourceStates(includeDisabled = false).last
      failedstate.status shouldBe "degraded"
      failedstate.componentCount shouldBe 1
      failedstate.warning.get should include("Catalog unavailable: retained-b")
      failedstate.warning.get should include("runtime total policy limit of 3 observations")
      val retainedobservationdiagnostics = runtime.get("b-one", None, None, None, None).toVector
        .flatMap(_.observationContext).flatMap(_.diagnostics).mkString(" ")
      retainedobservationdiagnostics should include(
        "runtime total policy limit of 3 observations"
      )
    }

    "coalesce concurrent automatic refreshes for the same source" in {
      Given("four readiness callers blocked behind one source read")
      val source = CatalogSource("single-flight", URI.create("https://single-flight.example/"), 100, true)
      val provider = new CoordinatedCatalogProvider(_component_profile(source.id))
      val runtime = CbdRuntime.create(Vector(source), provider, _clock)
      val executor = Executors.newFixedThreadPool(4)
      given executioncontext: ExecutionContext = ExecutionContext.fromExecutorService(executor)
      val ready = new CountDownLatch(4)
      val start = new CountDownLatch(1)

      try {
        When("all callers request initial readiness together")
        val calls = Vector.fill(4)(Future {
          ready.countDown()
          start.await()
          runtime.ensureReady(EmptyCatalogFetcher)
        })
        val callersready = ready.await(1, TimeUnit.SECONDS)
        start.countDown()
        val firststarted = provider.awaitReadCount(1, 1.second)
        val secondstarted = provider.awaitReadCount(2, 200.millis)
        provider.release()
        val results = calls.map(Await.result(_, 5.seconds))

        Then("one leader performs the read and every follower receives its completed source state")
        callersready shouldBe true
        firststarted shouldBe true
        secondstarted shouldBe false
        provider.readCount shouldBe 1
        results.map(_.isSuccess) shouldBe Vector.fill(4)(true)
      } finally {
        provider.release()
        executor.shutdownNow()
      }
    }

    "bound synchronized refresh bursts across distinct sources" in {
      Given("three administrative source refreshes and a runtime-wide concurrency limit of two")
      val sources = Vector(
        CatalogSource("burst-a", URI.create("https://burst-a.example/"), 100, true),
        CatalogSource("burst-b", URI.create("https://burst-b.example/"), 200, true),
        CatalogSource("burst-c", URI.create("https://burst-c.example/"), 300, true)
      )
      val provider = new CoordinatedCatalogProvider(_component_profile(sources.head.id))
      val policy = InformationSourceRefreshPolicy(
        Duration.ofMinutes(15),
        Duration.ofMinutes(1),
        Duration.ofMinutes(15),
        2
      )
      val runtime = CbdRuntime.create(
        sources,
        provider,
        CatalogCachePolicy(Duration.ofMinutes(15), policy),
        Clock.systemUTC()
      )
      val executor = Executors.newFixedThreadPool(3)
      given executioncontext: ExecutionContext = ExecutionContext.fromExecutorService(executor)
      val ready = new CountDownLatch(3)
      val start = new CountDownLatch(1)

      try {
        When("all three distinct sources are refreshed together")
        val calls = sources.map { source =>
          Future {
            ready.countDown()
            start.await()
            runtime.refresh(Some(source.id), EmptyCatalogFetcher)
          }
        }
        val callersready = ready.await(1, TimeUnit.SECONDS)
        start.countDown()
        val twostarted = provider.awaitReadCount(2, 1.second)
        val threestarted = provider.awaitReadCount(3, 200.millis)
        provider.release()
        val results = calls.map(Await.result(_, 5.seconds))

        Then("two reads run concurrently and the third waits for an admitted refresh slot")
        callersready shouldBe true
        twostarted shouldBe true
        threestarted shouldBe false
        provider.maximumActive shouldBe 2
        provider.readCount shouldBe 3
        results.forall(_.isSuccess) shouldBe true
      } finally {
        provider.release()
        executor.shutdownNow()
      }
    }

    "preserve source, version, freshness, and checksum in a component observation" in {
      Given("one catalog profile loaded through a source-aware bounded cache")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val profile = _component_profile(source.id).copy(artifactChecksumSha256 = Some("abc123"))
      val clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"))
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(profile), clock = _clock),
        CatalogCachePolicy(Duration.ofMinutes(5)),
        clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("the selected component is projected as an evidence-bearing observation")
      val selected = runtime.get("textus-order", None, Some("car"), None, None).get
      val observation = runtime.observation(selected).get

      Then("the observation remains attributable without copying fields from another source")
      observation shouldBe ComponentObservation(
        "memory",
        InformationSourceKind.PUBLISHED_CATALOG,
        "https://catalog.example/metadata/repository/car/index.json",
        Some("1.2.0"),
        "fresh",
        Some(Instant.parse("2026-07-14T00:00:00Z")),
        Some(Instant.parse("2026-07-14T00:05:00Z")),
        Some("abc123"),
        Vector.empty
      )
    }

    "keep an observation tied to the snapshot that supplied its component profile" in {
      Given("one retained profile followed by a successful refresh of the same source")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      val clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"))
      val runtime = CbdRuntime.create(
        Vector(source),
        provider,
        CatalogCachePolicy(Duration.ofMinutes(5)),
        clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      val retained = runtime.get("textus-order", None, Some("car"), None, None).get
      clock.advance(Duration.ofMinutes(5))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("the retained and current profiles are projected after refresh")
      val retainedobservation = runtime.observation(retained).get
      val current = runtime.get("textus-order", None, Some("car"), None, None).get
      val currentobservation = runtime.observation(current).get

      Then("each observation reports its own snapshot time instead of mixing refresh state")
      retainedobservation.observedAt shouldBe Some(Instant.parse("2026-07-14T00:00:00Z"))
      retainedobservation.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      retainedobservation.freshness shouldBe "stale"
      currentobservation.observedAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      currentobservation.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:10:00Z"))
      currentobservation.freshness shouldBe "fresh"
    }

    "report explicit observation absence when a profile has no source context" in {
      Given("a profile that was not loaded through a runtime source snapshot")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val runtime = CbdRuntime.create(Vector(source), new InMemoryComponentCatalogProvider(Vector.empty, clock = _clock), _clock)
      val unboundprofile = _component_profile("missing-source")

      When("the unbound profile is projected as an observation")
      val observation = runtime.observation(unboundprofile)

      Then("the runtime exposes absence instead of fabricating a published catalog kind")
      observation shouldBe None
    }

    "fail readiness when every initial catalog load fails" in {
      Given("an enabled catalog whose provider is unavailable")
      val source = CatalogSource("failed", URI.create("https://failed.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      provider.fail = true
      val runtime = CbdRuntime.create(Vector(source), provider, _clock)

      When("initial readiness is requested")
      val consequence = runtime.ensureReady(EmptyCatalogFetcher)

      Then("the operation fails and exposes degraded source state")
      consequence.toOption shouldBe None
      runtime.overallStatus shouldBe "degraded"
      runtime.componentCount shouldBe 0
    }

    "resolve a same-catalog dependency graph without hiding conflicts or incomplete edges" in {
      Given("a root whose graph contains conflicting versions, an unresolved edge, an ambiguous edge, and a cycle")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val othersource = CatalogSource("other", URI.create("https://other.example/"), 200, true)
      val rootdependencies = Vector(
        ComponentDependency("branch", Some("1.0.0"), Some("car")),
        ComponentDependency("shared", Some("1.0.0"), Some("car")),
        ComponentDependency("ambiguous", Some("1.0.0"), Some("car"))
      )
      val root = _component_profile(source.id).copy(
        name = "root",
        title = "Root",
        versions = Vector("1.0.0"),
        selectedVersion = Some("1.0.0"),
        dependencyMetadataVersion = Some("1.0.0"),
        latestStable = Some("1.0.0"),
        dependencies = rootdependencies,
        versionEvidence = Vector(ComponentVersionEvidence("1.0.0", None, rootdependencies, None, None, true))
      )
      val branchdependencies = Vector(
        ComponentDependency("shared", Some("2.0.0"), Some("car")),
        ComponentDependency("missing", Some("1.0.0"), Some("car")),
        ComponentDependency("root", Some("1.0.0"), Some("car"))
      )
      val branch = _component_profile(source.id).copy(
        name = "branch",
        title = "Branch",
        versions = Vector("1.0.0"),
        selectedVersion = Some("1.0.0"),
        dependencyMetadataVersion = Some("1.0.0"),
        latestStable = Some("1.0.0"),
        dependencies = branchdependencies,
        versionEvidence = Vector(ComponentVersionEvidence("1.0.0", None, branchdependencies, None, None, true))
      )
      val shared = _component_profile(source.id).copy(
        name = "shared",
        title = "Shared",
        versions = Vector("1.0.0", "2.0.0"),
        selectedVersion = Some("2.0.0"),
        dependencyMetadataVersion = Some("2.0.0"),
        latestStable = Some("2.0.0"),
        dependencies = Vector(ComponentDependency("selected-child", Some("1.0.0"), Some("car"))),
        versionEvidence = Vector(
          ComponentVersionEvidence(
            "1.0.0",
            None,
            Vector(ComponentDependency("legacy-child", Some("1.0.0"), Some("car"))),
            None,
            None,
            true
          ),
          ComponentVersionEvidence(
            "2.0.0",
            None,
            Vector(ComponentDependency("selected-child", Some("1.0.0"), Some("car"))),
            None,
            None,
            true
          )
        )
      )
      val selectedchild = _component_profile(source.id).copy(
        name = "selected-child",
        title = "Selected Child",
        versions = Vector("1.0.0"),
        selectedVersion = Some("1.0.0"),
        dependencyMetadataVersion = Some("1.0.0"),
        latestStable = Some("1.0.0"),
        dependencies = Vector.empty,
        versionEvidence = Vector(ComponentVersionEvidence("1.0.0", None, Vector.empty, None, None, true))
      )
      val legacychild = selectedchild.copy(name = "legacy-child", title = "Legacy Child")
      val ambiguousone = _component_profile(source.id).copy(
        organization = Some("org.one"),
        name = "ambiguous",
        title = "Ambiguous One",
        versions = Vector("1.0.0"),
        selectedVersion = Some("1.0.0"),
        dependencyMetadataVersion = Some("1.0.0"),
        latestStable = Some("1.0.0"),
        dependencies = Vector.empty,
        versionEvidence = Vector(ComponentVersionEvidence("1.0.0", None, Vector.empty, None, None, true))
      )
      val ambiguoustwo = ambiguousone.copy(organization = Some("org.two"), title = "Ambiguous Two")
      val othermissing = _component_profile(othersource.id).copy(
        name = "missing",
        title = "Missing In Root Catalog",
        versions = Vector("1.0.0"),
        selectedVersion = Some("1.0.0"),
        dependencyMetadataVersion = Some("1.0.0"),
        latestStable = Some("1.0.0"),
        dependencies = Vector.empty
      )
      val runtime = CbdRuntime.create(
        Vector(source, othersource),
        new PerSourceCatalogProvider(Map(
          source.id -> Vector(root, branch, shared, selectedchild, legacychild, ambiguousone, ambiguoustwo),
          othersource.id -> Vector(othermissing)
        )),
        _clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("the dependency graph is resolved from the selected catalog")
      val resolution = runtime.resolveDependencies(root, CbdRuntime.DEFAULT_DEPENDENCY_DEPTH)

      Then("each incomplete edge and cycle remains observable and distinct version requests become a conflict")
      resolution.resolutions.map(x => (x.dependency.name, x.dependency.version, x.status)) should contain allOf (
        (("branch", Some("1.0.0"), "resolved")),
        (("shared", Some("2.0.0"), "resolved")),
        (("missing", Some("1.0.0"), "unresolved")),
        (("root", Some("1.0.0"), "cycle")),
        (("shared", Some("1.0.0"), "resolved")),
        (("ambiguous", Some("1.0.0"), "ambiguous"))
      )
      resolution.conflicts.map(_.name) shouldBe Vector("shared")
      resolution.conflicts.head.versions shouldBe Vector("1.0.0", "2.0.0")
      resolution.resolutions.count(_.dependency.name == "selected-child") shouldBe 1
      resolution.resolutions.find(_.dependency.name == "selected-child").map(_.path) should contain("car:org.textus:root@1.0.0 -> car:branch@1.0.0 -> car:shared@2.0.0 -> car:selected-child@1.0.0")
      resolution.resolutions.count(_.dependency.name == "legacy-child") shouldBe 1
      resolution.resolutions.find(_.dependency.name == "legacy-child").map(_.path) should contain("car:org.textus:root@1.0.0 -> car:shared@1.0.0 -> car:legacy-child@1.0.0")
      resolution.warnings.exists(_.contains("not published")) shouldBe true
      resolution.warnings.exists(_.contains("multiple profiles")) shouldBe true
      resolution.warnings.exists(_.contains("cycle detected")) shouldBe true
      resolution.warnings.exists(_.contains("Conflicting dependency versions")) shouldBe true
      resolution.warnings.exists(_.contains("metadata is unavailable for requested version 1.0.0")) shouldBe false
    }

    "stop transitive traversal at the requested bounded depth" in {
      Given("a two-edge dependency chain and a direct-edge depth limit")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val root = _component_profile(source.id).copy(
        name = "root",
        title = "Root",
        dependencies = Vector(ComponentDependency("branch", Some("1.2.0"), Some("car")))
      )
      val branch = _component_profile(source.id).copy(
        name = "branch",
        title = "Branch",
        dependencies = Vector(ComponentDependency("leaf", Some("1.2.0"), Some("car")))
      )
      val leaf = _component_profile(source.id).copy(name = "leaf", title = "Leaf", dependencies = Vector.empty)
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(root, branch, leaf), clock = _clock),
        _clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("resolution is limited to depth one")
      val resolution = runtime.resolveDependencies(root, 1)

      Then("the direct edge is retained and deeper traversal is reported as truncated")
      resolution.resolutions.map(_.dependency.name) shouldBe Vector("branch")
      resolution.resolutions.map(_.depth) shouldBe Vector(1)
      resolution.warnings.exists(_.contains("maxDepth=1")) shouldBe true
    }

    "withhold selected-version dependency metadata from a different explicit root version" in {
      Given("a multi-version root whose dependency metadata belongs only to the selected latest version")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val root = _component_profile(source.id).copy(
        name = "root",
        title = "Root",
        versions = Vector("1.0.0", "2.0.0"),
        selectedVersion = Some("2.0.0"),
        dependencyMetadataVersion = Some("2.0.0"),
        latestStable = Some("2.0.0"),
        dependencies = Vector(ComponentDependency("latest-only", Some("1.0.0"), Some("car")))
      )
      val latestonly = _component_profile(source.id).copy(
        name = "latest-only",
        title = "Latest Only",
        dependencies = Vector.empty
      )
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(root, latestonly), clock = _clock),
        _clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("dependencies are requested for the older root version")
      val resolution = runtime.resolveDependencies(root, Some("1.0.0"), CbdRuntime.DEFAULT_DEPENDENCY_DEPTH)

      Then("the selected latest-version dependency metadata is not returned or traversed")
      resolution.directDependencies shouldBe empty
      resolution.resolutions shouldBe empty
      resolution.warnings.exists(_.contains("requested root version 1.0.0")) shouldBe true
    }

    "project explicit version evidence without reusing selected-version details" in {
      Given("a component with two detailed versions and one version listed without detail")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val olddependency = ComponentDependency("old-support", Some("1.0.0"), Some("car"))
      val newdependency = ComponentDependency("new-support", Some("2.0.0"), Some("car"))
      val oldartifact = URI.create("https://memory.example/repository/car/order/1.0.0/order-1.0.0.car")
      val newartifact = URI.create("https://memory.example/repository/car/order/2.0.0/order-2.0.0.car")
      val profile = _component_profile(source.id).copy(
        versions = Vector("0.9.0", "1.0.0", "2.0.0"),
        selectedVersion = Some("2.0.0"),
        dependencyMetadataVersion = Some("2.0.0"),
        latestStable = Some("2.0.0"),
        runtimeMinimum = Some("0.5.0"),
        runtimeMaximum = Some("0.5.9"),
        dependencies = Vector(newdependency),
        artifactUri = Some(newartifact),
        versionEvidence = Vector(
          ComponentVersionEvidence(
            "1.0.0",
            Some("0.4.0"),
            Vector(olddependency),
            Some(oldartifact),
            None,
            true,
            runtimeMaximum = Some("0.4.9")
          ),
          ComponentVersionEvidence("2.0.0", Some("0.5.0"), Vector(newdependency), Some(newartifact), None, true)
        ),
        warnings = Vector("Catalog entry does not publish an artifact path for the selected version.")
      )
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(profile), clock = _clock),
        _clock
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("the older detailed version and the metadata-free listed version are selected")
      val selected = runtime.get("textus-order", None, Some("car"), Some("1.0.0"), None).get
      val matches = runtime.search("order", None, Some("car"), Some("1.0.0"), Some("0.4.0"), 10)
      val incompatible = runtime.search("order", None, Some("car"), Some("1.0.0"), Some("0.3.0"), 10)
      val abovemaximum = runtime.search("order", None, Some("car"), Some("1.0.0"), Some("0.5.0"), 10)
      val missing = runtime.get("textus-order", None, Some("car"), Some("0.9.0"), None).get

      Then("each result uses only evidence belonging to its explicit version")
      selected.selectedVersion shouldBe Some("1.0.0")
      selected.runtimeMinimum shouldBe Some("0.4.0")
      selected.dependencies shouldBe Vector(olddependency)
      selected.artifactUri shouldBe Some(oldartifact)
      selected.warnings shouldBe empty
      matches.map(_.profile.selectedVersion) shouldBe Vector(Some("1.0.0"))
      incompatible shouldBe empty
      abovemaximum shouldBe empty
      missing.selectedVersion shouldBe Some("0.9.0")
      missing.runtimeMinimum shouldBe None
      missing.dependencies shouldBe empty
      missing.artifactUri shouldBe None
      missing.warnings.exists(_.contains("Catalog entry does not publish an artifact path")) shouldBe false
      missing.warnings.exists(_.contains("without version-specific metadata")) shouldBe true
    }
  }

  private def _component_profile(catalogid: String): ComponentProfile = {
    val dependencies = Vector(ComponentDependency("textus-identity", Some("0.4.0"), Some("car")))
    val artifacturi = URI.create("https://catalog.example/textus-order.car")
    ComponentProfile(
      catalogid,
      Some("org.textus"),
      "textus-order",
      "Textus Order",
      Some("Order component for CBD reuse."),
      "car",
      Vector("1.2.0"),
      Some("1.2.0"),
      Some("1.2.0"),
      Some("1.2.0"),
      None,
      Some("0.5.1"),
      Vector("business.order"),
      Vector.empty,
      dependencies,
      Some(artifacturi),
      URI.create("https://catalog.example/metadata/repository/car/index.json"),
      None,
      None,
      Vector(ComponentVersionEvidence("1.2.0", Some("0.5.1"), dependencies, Some(artifacturi), None, true)),
      Vector.empty
    )
  }

  private def _resource_text(path: String): String = {
    val stream = Option(getClass.getResourceAsStream(path)).getOrElse {
      fail(s"Missing test resource: $path")
    }
    val source = Source.fromInputStream(stream, "UTF-8")
    try source.mkString
    finally source.close()
  }

  private final class MapCatalogFetcher(values: Map[URI, String]) extends CatalogFetcher {
    var requestedUris = Vector.empty[URI]
    var requestedSourceIds = Vector.empty[String]

    def get(uri: URI): Consequence[String] = {
      requestedUris = requestedUris :+ uri
      values.get(uri).map(Consequence.success).getOrElse(Consequence.serviceUnavailable(s"Missing fixture: $uri"))
    }

    override def get(source: CatalogSource, uri: URI, maxbytes: Int): Consequence[String] = {
      requestedSourceIds = requestedSourceIds :+ source.id
      get(uri, maxbytes)
    }
  }

  private object EmptyCatalogFetcher extends CatalogFetcher {
    def get(uri: URI): Consequence[String] = Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
  }

  private final class SwitchableCatalogProvider(profile: ComponentProfile) extends ComponentCatalogProvider {
    var fail = false
    var readCount = 0

    def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] = {
      readCount += 1
      if (fail) Consequence.serviceUnavailable(s"Catalog unavailable: ${source.id}")
      else Consequence.success(CatalogSnapshot(source, Vector(profile), Instant.now(), None))
    }

    def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
      Consequence.success(ComponentUsage(profile, Vector.empty, Vector.empty, Vector.empty))
  }

  private final class CoordinatedCatalogProvider(profile: ComponentProfile) extends ComponentCatalogProvider {
    private val _read_count = new AtomicInteger(0)
    private val _active_count = new AtomicInteger(0)
    private val _maximum_active = new AtomicInteger(0)
    private val _release = new CountDownLatch(1)
    private val _first_read = new CountDownLatch(1)
    private val _second_read = new CountDownLatch(2)
    private val _third_read = new CountDownLatch(3)

    def readCount: Int = _read_count.get()

    def maximumActive: Int = _maximum_active.get()

    def awaitReadCount(count: Int, timeout: scala.concurrent.duration.Duration): Boolean = {
      val latch = count match {
        case 1 => _first_read
        case 2 => _second_read
        case 3 => _third_read
        case _ => throw new IllegalArgumentException(s"Unsupported coordinated read count: $count")
      }
      latch.await(timeout.toMillis, TimeUnit.MILLISECONDS)
    }

    def release(): Unit =
      _release.countDown()

    def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] = {
      _read_count.incrementAndGet()
      _first_read.countDown()
      _second_read.countDown()
      _third_read.countDown()
      val active = _active_count.incrementAndGet()
      _maximum_active.accumulateAndGet(active, Math.max)
      try {
        _release.await()
        Consequence.success(CatalogSnapshot(
          source,
          Vector(profile.copy(catalogId = source.id)),
          Instant.now(),
          None
        ))
      } finally {
        _active_count.decrementAndGet()
      }
    }

    def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
      Consequence.success(ComponentUsage(profile, Vector.empty, Vector.empty, Vector.empty))
  }

  private final class MutableClock(private var _current: Instant) extends Clock {
    override def getZone: ZoneId = ZoneOffset.UTC

    override def withZone(zone: ZoneId): Clock = Clock.fixed(_current, zone)

    override def instant(): Instant = _current

    def advance(duration: Duration): Unit =
      _current = _current.plus(duration)
  }

  private final class PerSourceCatalogProvider(profiles: Map[String, Vector[ComponentProfile]])
    extends ComponentCatalogProvider {
    def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
      Consequence.success(CatalogSnapshot(
        source,
        profiles.getOrElse(source.id, Vector.empty).map(_.copy(catalogId = source.id)),
        Instant.now(),
        None
      ))

    def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
      Consequence.success(ComponentUsage(profile, Vector.empty, Vector.empty, Vector.empty))
  }

  private final class FailingPerSourceCatalogProvider(profiles: Map[String, Vector[ComponentProfile]])
    extends ComponentCatalogProvider {
    private var _failed_source_ids = Set.empty[String]

    def failSource(sourceid: String): Unit =
      _failed_source_ids = _failed_source_ids + sourceid

    def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
      if (_failed_source_ids.contains(source.id)) Consequence.serviceUnavailable(s"Catalog unavailable: ${source.id}")
      else Consequence.success(CatalogSnapshot(
        source,
        profiles.getOrElse(source.id, Vector.empty).map(_.copy(catalogId = source.id)),
        Instant.EPOCH,
        None
      ))

    def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
      Consequence.success(ComponentUsage(profile, Vector.empty, Vector.empty, Vector.empty))
  }
}
