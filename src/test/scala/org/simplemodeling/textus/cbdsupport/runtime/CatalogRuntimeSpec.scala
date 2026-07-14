package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.time.{Clock, Duration, Instant, ZoneId, ZoneOffset}

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
      profile.selectedVersion shouldBe Some("1.2.0")
      profile.dependencyMetadataVersion shouldBe Some("1.2.0")
      profile.versionEvidence.map(_.version) shouldBe Vector("1.2.0")
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
      val provider = new SimpleModelingPublicationCatalogProvider()

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

    "refresh a catalog only after its bounded cache lifetime expires" in {
      Given("a five-minute cache policy and a controllable clock")
      val source = CatalogSource("switchable", URI.create("https://switchable.example/"), 100, true)
      val provider = new SwitchableCatalogProvider(_component_profile(source.id))
      val clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"))
      val runtime = CbdRuntime.create(
        Vector(source),
        provider,
        CatalogCachePolicy(Duration.ofMinutes(5)),
        clock
      )

      When("readiness is checked before and at the cache expiry boundary")
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      clock.advance(Duration.ofMinutes(4))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      provider.readCount shouldBe 1
      clock.advance(Duration.ofMinutes(1))
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      val state = runtime.sourceStates(includeDisabled = false).head

      Then("the fresh snapshot is reused and the expired snapshot is refreshed with observable times")
      provider.readCount shouldBe 2
      state.status shouldBe "ready"
      state.cacheStatus shouldBe "fresh"
      state.refreshedAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:10:00Z"))
      state.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
    }

    "preserve a stale last-known-good snapshot when automatic refresh fails" in {
      Given("a provider that fails after its first snapshot reaches cache expiry")
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

      When("readiness automatically retries the expired catalog")
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true
      val state = runtime.sourceStates(includeDisabled = false).head

      Then("the source exposes stale degraded state while the previous component remains searchable")
      provider.readCount shouldBe 2
      state.status shouldBe "degraded"
      state.cacheStatus shouldBe "stale"
      state.componentCount shouldBe 1
      state.refreshedAt shouldBe Some(Instant.parse("2026-07-14T00:00:00Z"))
      state.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.warning.exists(_.contains("Catalog unavailable")) shouldBe true
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
        ))
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
        new InMemoryComponentCatalogProvider(Vector(root, branch, leaf))
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
        new InMemoryComponentCatalogProvider(Vector(root, latestonly))
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
        dependencies = Vector(newdependency),
        artifactUri = Some(newartifact),
        versionEvidence = Vector(
          ComponentVersionEvidence("1.0.0", Some("0.4.0"), Vector(olddependency), Some(oldartifact), None, true),
          ComponentVersionEvidence("2.0.0", Some("0.5.0"), Vector(newdependency), Some(newartifact), None, true)
        ),
        warnings = Vector("Catalog entry does not publish an artifact path for the selected version.")
      )
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(profile))
      )
      runtime.ensureReady(EmptyCatalogFetcher).isSuccess shouldBe true

      When("the older detailed version and the metadata-free listed version are selected")
      val selected = runtime.get("textus-order", None, Some("car"), Some("1.0.0"), None).get
      val matches = runtime.search("order", None, Some("car"), Some("1.0.0"), Some("0.4.0"), 10)
      val incompatible = runtime.search("order", None, Some("car"), Some("1.0.0"), Some("0.3.0"), 10)
      val missing = runtime.get("textus-order", None, Some("car"), Some("0.9.0"), None).get

      Then("each result uses only evidence belonging to its explicit version")
      selected.selectedVersion shouldBe Some("1.0.0")
      selected.runtimeMinimum shouldBe Some("0.4.0")
      selected.dependencies shouldBe Vector(olddependency)
      selected.artifactUri shouldBe Some(oldartifact)
      selected.warnings shouldBe empty
      matches.map(_.profile.selectedVersion) shouldBe Vector(Some("1.0.0"))
      incompatible shouldBe empty
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

  private final class MapCatalogFetcher(values: Map[URI, String]) extends CatalogFetcher {
    def get(uri: URI): Consequence[String] =
      values.get(uri).map(Consequence.success).getOrElse(Consequence.serviceUnavailable(s"Missing fixture: $uri"))
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
}
