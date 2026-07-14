package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.time.Instant

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class CatalogRuntimeSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CozyComponentCatalogProvider" should {
    "consume Cozy's generated CAR index and model metadata contract" in {
      Given("a Cozy CAR index with a selected runtime, sidecar, and artifact")
      val source = CatalogSource("fixture", URI.create("https://catalog.example/"), 100, true)
      val carindex =
        """{
          |  "entries": [{
          |    "artifact_id": "textus-order",
          |    "aliases": ["order-component"],
          |    "tags": ["business.order"],
          |    "terms": ["受注管理"],
          |    "recommended": "1.2.0",
          |    "latest_stable": "1.2.0",
          |    "sidecars": {
          |      "model_metadata_json": "repository/catalog/car/textus-order.model-metadata.json"
          |    },
          |    "versions": [{
          |      "version": "1.2.0",
          |      "file": "repository/car/textus-order/1.2.0/textus-order-1.2.0.car",
          |      "runtime": {"cncf": {"minimum": "0.5.1"}},
          |      "component_descriptor": {
          |        "dependencies": [{"name": "textus-identity", "version": "0.4.0", "kind": "car"}]
          |      }
          |    }]
          |  }]
          |} """.stripMargin
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
        source.baseUri.resolve("repository/catalog/car/textus-order.model-metadata.json") -> modelmetadata
      ))
      val provider = new CozyComponentCatalogProvider()

      When("the catalog and selected component usage are read")
      val snapshot = provider.read(source, fetcher).toOption.get
      val profile = snapshot.profiles.head
      val usage = provider.readUsage(profile, fetcher).toOption.get

      Then("the provider preserves authoritative version, runtime, dependency, and operation evidence")
      profile.name shouldBe "textus-order"
      profile.latestStable shouldBe Some("1.2.0")
      profile.runtimeMinimum shouldBe Some("0.5.1")
      profile.tags should contain allOf ("business.order", "order-component")
      profile.dependencies should contain(ComponentDependency("textus-identity", Some("0.4.0"), Some("car")))
      profile.artifactUri.map(_.toString) shouldBe Some("https://catalog.example/repository/car/textus-order/1.2.0/textus-order-1.2.0.car")
      usage.operations shouldBe Vector(ComponentOperation(Some("OrderQuery"), "getOrder", Some("query"), Some("Return one order.")))
    }
  }

  "CompatibleComponentCatalogProvider" should {
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
        new CozyComponentCatalogProvider(),
        new SimpleModelingPublicationCatalogProvider()
      )

      When("the publication catalog is read")
      val snapshot = provider.read(source, fetcher).toOption.get
      val profile = snapshot.profiles.head
      val usage = provider.readUsage(profile, fetcher).toOption.get

      Then("the CAR evidence is explicit and non-component catalog projects are ignored")
      profile.identity shouldBe "org.textus:textus-order"
      profile.versions shouldBe Vector("1.2.0")
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

    "classify snapshots separately and report publication entry failures" in {
      Given("a publication catalog with one snapshot component and one unreadable entry")
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
      val provider = new SimpleModelingPublicationCatalogProvider()

      When("the partially available publication catalog is read")
      val snapshot = provider.read(source, fetcher).toOption.get
      val profile = snapshot.profiles.head

      Then("the snapshot remains non-stable and the missing entry is observable")
      profile.latestStable shouldBe None
      profile.latestSnapshot shouldBe Some("2.0.0-SNAPSHOT")
      snapshot.warning.exists(_.contains("broken-component")) shouldBe true
    }
  }

  "CbdRuntime" should {
    "search Japanese catalog terms through an in-memory provider" in {
      Given("an in-memory catalog profile tagged with a Japanese CBD term")
      val source = CatalogSource("memory", URI.create("https://memory.example/"), 100, true)
      val profile = _component_profile(source.id).copy(terms = Vector("受注管理"))
      val runtime = CbdRuntime.create(Vector(source), new InMemoryComponentCatalogProvider(Vector(profile)))

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
        new InMemoryComponentCatalogProvider(Vector(supported, unknown))
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("search constrains results to a concrete CNCF runtime version")
      val results = runtime.search("order", None, Some("car"), None, Some("0.5.1"), 10)

      Then("only the component with affirmative compatibility evidence is returned")
      results.map(_.profile.name) shouldBe Vector("textus-order")
    }

    "preserve a last-known-good snapshot when refresh fails" in {
      Given("a provider that succeeds once and fails on the next refresh")
      val source = CatalogSource("switchable", URI.create("https://switchable.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      val runtime = CbdRuntime.create(Vector(source), provider)
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      provider.fail = true

      When("the catalog refresh fails")
      runtime.refresh(None, EmptyCatalogFetcher).isSuccess shouldBe true
      val state = runtime.sourceStates(includeDisabled = false).head

      Then("the source is degraded while the previous component remains searchable")
      state.status shouldBe "degraded"
      state.componentCount shouldBe 1
      runtime.get("textus-order", None, None, None, None) should not be empty
    }

    "fail readiness when every initial catalog load fails" in {
      Given("an enabled catalog whose provider is unavailable")
      val source = CatalogSource("failed", URI.create("https://failed.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      provider.fail = true
      val runtime = CbdRuntime.create(Vector(source), provider)

      When("initial readiness is requested")
      val consequence = runtime.ensureReady(EmptyCatalogFetcher)

      Then("the operation fails and exposes degraded source state")
      consequence.toOption shouldBe None
      runtime.overallStatus shouldBe "degraded"
      runtime.componentCount shouldBe 0
    }
  }

  private def _component_profile(catalogid: String): ComponentProfile =
    ComponentProfile(
      catalogid,
      Some("org.textus"),
      "textus-order",
      "Textus Order",
      Some("Order component for CBD reuse."),
      "car",
      Vector("1.2.0"),
      Some("1.2.0"),
      None,
      Some("0.5.1"),
      Vector("business.order"),
      Vector.empty,
      Vector(ComponentDependency("textus-identity", Some("0.4.0"), Some("car"))),
      Some(URI.create("https://catalog.example/textus-order.car")),
      URI.create("https://catalog.example/metadata/repository/car/index.json"),
      None,
      None,
      Vector.empty
    )

  private final class MapCatalogFetcher(values: Map[URI, String]) extends CatalogFetcher {
    def get(uri: URI): Consequence[String] =
      values.get(uri).map(Consequence.success).getOrElse(Consequence.serviceUnavailable(s"Missing fixture: $uri"))
  }

  private object EmptyCatalogFetcher extends CatalogFetcher {
    def get(uri: URI): Consequence[String] = Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
  }

  private final class SwitchableCatalogProvider(profile: ComponentProfile) extends ComponentCatalogProvider {
    var fail = false

    def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
      if (fail) Consequence.serviceUnavailable(s"Catalog unavailable: ${source.id}")
      else Consequence.success(CatalogSnapshot(source, Vector(profile), Instant.now(), None))

    def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
      Consequence.success(ComponentUsage(profile, Vector.empty, Vector.empty, Vector.empty))
  }
}
