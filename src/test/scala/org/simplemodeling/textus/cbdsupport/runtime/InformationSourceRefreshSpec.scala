package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.file.{Files, Path}
import java.time.{Clock, Duration, Instant, ZoneId, ZoneOffset}
import scala.jdk.CollectionConverters.*

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class InformationSourceRefreshSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Production refresh schedule" should {
    "admit only finite intervals from one minute through 24 hours and no later than source expiry" in {
      Given("the inclusive production interval bounds and source TTL constraints")
      val minimum = InformationSourceRefreshPolicy.MINIMUM_INTERVAL
      val maximum = InformationSourceRefreshPolicy.MAXIMUM_INTERVAL

      When("boundary and out-of-bound policies are constructed")
      val accepted = Vector(
        InformationSourceRefreshPolicy(minimum),
        InformationSourceRefreshPolicy(maximum)
      )
      val production = InformationSourceRefreshPolicy(
        Duration.ofMinutes(15),
        Duration.ofMinutes(1),
        Duration.ofMinutes(15),
        2
      )

      Then("schedule, retry, and concurrency boundaries admit production values and reject unbounded work")
      accepted.map(_.interval) shouldBe Vector(minimum, maximum)
      production.retryInitialInterval shouldBe Duration.ofMinutes(1)
      production.retryMaximumInterval shouldBe Duration.ofMinutes(15)
      production.maxConcurrentRefreshes shouldBe 2
      an[IllegalArgumentException] should be thrownBy InformationSourceRefreshPolicy(minimum.minusSeconds(1))
      an[IllegalArgumentException] should be thrownBy InformationSourceRefreshPolicy(maximum.plusSeconds(1))
      an[IllegalArgumentException] should be thrownBy InformationSourceRefreshPolicy(
        Duration.ofMinutes(15),
        Duration.ofSeconds(59),
        Duration.ofMinutes(15),
        2
      )
      an[IllegalArgumentException] should be thrownBy InformationSourceRefreshPolicy(
        Duration.ofMinutes(15),
        Duration.ofMinutes(2),
        Duration.ofMinutes(1),
        2
      )
      an[IllegalArgumentException] should be thrownBy InformationSourceRefreshPolicy(
        Duration.ofMinutes(15),
        Duration.ofMinutes(1),
        Duration.ofMinutes(16),
        2
      )
      an[IllegalArgumentException] should be thrownBy InformationSourceRefreshPolicy(
        Duration.ofMinutes(15),
        Duration.ofMinutes(1),
        Duration.ofMinutes(15),
        0
      )
      an[IllegalArgumentException] should be thrownBy InformationSourceRefreshPolicy(
        Duration.ofMinutes(15),
        Duration.ofMinutes(1),
        Duration.ofMinutes(15),
        9
      )
      an[IllegalArgumentException] should be thrownBy CatalogCachePolicy(
        Duration.ofMinutes(5),
        InformationSourceRefreshPolicy(Duration.ofMinutes(6))
      )
      an[IllegalArgumentException] should be thrownBy BokInspectionPolicy(
        refreshTtl = Duration.ofMinutes(5),
        refreshPolicy = InformationSourceRefreshPolicy(Duration.ofMinutes(6))
      )
    }
  }

  "Runtime snapshot retention" should {
    "preserve the runtime construction signatures published before retention policy" in {
      Given("the compiled CbdRuntime class and companion object")
      val constructorarities = classOf[CbdRuntime].getConstructors.map(_.getParameterCount).toSet
      val federatedarities = CbdRuntime.getClass.getMethods
        .filter(_.getName == "createFederated").map(_.getParameterCount).toSet

      When("the public JVM construction surfaces are inspected")
      val oldconstructoravailable = constructorarities.contains(13)
      val oldfactoryavailable = federatedarities.contains(12)

      Then("the old signatures remain available beside the retention-aware variants")
      oldconstructoravailable shouldBe true
      oldfactoryavailable shouldBe true
      constructorarities should contain(14)
      federatedarities should contain(13)
    }

    "admit only positive retention limits at or below the production hard caps" in {
      Given("the inclusive source and observation retention maxima")
      val maximum = InformationSourceRetentionPolicy.DEFAULT

      When("the production maxima and values outside each boundary are constructed")
      val accepted = InformationSourceRetentionPolicy(
        maximum.maxSources,
        maximum.maxCatalogObservations,
        maximum.maxBokObservations,
        maximum.maxSieBokObservations,
        maximum.maxLocalObservations
      )

      Then("the production maxima are finite and every zero or excessive limit is rejected")
      accepted shouldBe maximum
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxSources = 0)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxCatalogObservations = 0)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxBokObservations = 0)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxSieBokObservations = 0)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxLocalObservations = 0)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxSources = 65)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxCatalogObservations = 20001)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxBokObservations = 20001)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxSieBokObservations = 801)
      an[IllegalArgumentException] should be thrownBy InformationSourceRetentionPolicy(maxLocalObservations = 513)
    }

    "reject a configured source set larger than the retained source bound" in {
      Given("two catalog sources and a runtime policy that admits only one retained source")
      val sources = Vector(
        CatalogSource("first", URI.create("https://first.example/"), 100, true),
        CatalogSource("second", URI.create("https://second.example/"), 200, true)
      )

      When("the federated runtime is constructed")
      val construction = () => CbdRuntime.createFederated(
        sources,
        new InMemoryComponentCatalogProvider(Vector.empty),
        CatalogCachePolicy.DEFAULT,
        Clock.systemUTC(),
        Vector.empty,
        new BokKnowledgeSourceProvider(),
        retentionpolicy = InformationSourceRetentionPolicy(maxSources = 1)
      )

      Then("configuration fails before any source work can exceed the source-count bound")
      an[IllegalArgumentException] should be thrownBy construction()
    }
  }

  "Published catalog input" should {
    "bound configured sources, authorized origins, response bytes, and discovered profiles" in {
      Given("two configured catalogs and a repository index containing more profiles than the policy admits")
      val policy = CatalogInspectionPolicy(
        maxConfiguredSources = 1,
        maxAllowedOrigins = 1,
        maxProfiles = 1,
        maxIndexBytes = 4096,
        maxMetadataBytes = 1024
      )
      val configuration = CatalogSourceConfig.parse(
        Some("first=https://first.example/,second=https://second.example/"),
        Some("https://first.example,https://second.example"),
        policy
      )
      val source = CatalogSource("bounded", URI.create("https://catalog.example/"), 100, true)
      val caruri = source.baseUri.resolve("metadata/repository/car/index.json")
      val fetcher = new RecordingCatalogFetcher(Map(
        caruri -> """{"entries":[{"artifact_id":"one","versions":["1.0.0"]},{"artifact_id":"two","versions":["1.0.0"]}]}"""
      ))

      When("the configured boundary and catalog document are inspected")
      val snapshot = new CozyComponentCatalogProvider(policy).read(source, fetcher).toOption.get

      Then("only bounded work becomes source state and every truncation remains diagnostic")
      configuration.sources.map(_.id) shouldBe Vector("simplemodeling", "first")
      configuration.warnings.exists(_.contains("allowed-origin configuration exceeds")) shouldBe true
      configuration.warnings.exists(_.contains("source configuration exceeds")) shouldBe true
      fetcher.requests.map(_._2) shouldBe Vector(4096, 4096)
      snapshot.profiles.map(_.name) shouldBe Vector("one")
      snapshot.warning.get should include("truncated at 1 entries")
    }
  }

  "BoK site input" should {
    "refresh at finite expiry and retain stale last-known-good evidence after failure" in {
      Given("one BoK snapshot, a five-minute TTL, and a controllable runtime clock")
      val clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"))
      val catalog = CatalogSource("catalog", URI.create("https://catalog.example/"), 100, true)
      val source = BokSource("bok", URI.create("https://bok.example/"), 600, true)
      val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
      val termsuri = source.baseUri.resolve("metadata/glossary/terms.json")
      val fetcher = new SwitchableBokFetcher(Map(
        manifesturi -> """{"schemaVersion":"cncf.knowledge-source.v1","kind":"bok-site","id":"bok","sourceRef":{"kind":"bok-site","value":"bok"},"resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"}]}""",
        termsuri -> """{"terms":[{"id":"runtime","title":"Runtime"}]}"""
      ))
      val policy = BokInspectionPolicy(
        refreshTtl = Duration.ofMinutes(5),
        refreshPolicy = InformationSourceRefreshPolicy(Duration.ofMinutes(5))
      )
      val runtime = CbdRuntime.createFederated(
        Vector(catalog),
        new InMemoryComponentCatalogProvider(Vector.empty),
        CatalogCachePolicy.DEFAULT,
        clock,
        Vector(source),
        new BokKnowledgeSourceProvider(clock),
        policy
      )

      When("readiness is checked before expiry and again at expiry after the BoK source fails")
      runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
      runtime.bokSourceStates(includeDisabled = false).head.nextRefreshAttemptAt shouldBe
        Some(Instant.parse("2026-07-14T00:05:00Z"))
      clock.advance(Duration.ofMinutes(4))
      runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
      fetcher.fail = true
      clock.advance(Duration.ofMinutes(1))
      runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
      runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
      runtime.bokSourceStates(includeDisabled = false).head.nextRefreshAttemptAt shouldBe
        Some(Instant.parse("2026-07-14T00:06:00Z"))
      clock.advance(Duration.ofMinutes(1))
      runtime.ensureInputsReady(fetcher).isSuccess shouldBe true

      Then("fresh reuse performs no work and repeated failure backs off while stale evidence remains attributable")
      fetcher.requests should have size 4
      runtime.bokTerms.map(_.termId) shouldBe Vector("runtime")
      val state = runtime.bokSourceStates(includeDisabled = false).head
      state.status shouldBe "degraded"
      state.cacheStatus shouldBe "stale"
      state.observedAt shouldBe Some(Instant.parse("2026-07-14T00:00:00Z"))
      state.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:06:00Z"))
      state.nextRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:08:00Z"))
      state.diagnostics.mkString(" ") should include("unavailable")
    }

    "preserve stale evidence across authentication, transport, parse, and compatibility failures until recovery" in {
      Given("one valid BoK snapshot and four provider-boundary failure outcomes")
      val scenarios = Vector(
        FailureTransition("authentication", "source-credential-expired"),
        FailureTransition("transport", "transport-unavailable"),
        FailureTransition("parse", "not valid JSON"),
        FailureTransition("compatibility", "unsupported schemaVersion")
      )

      scenarios.foreach { scenario =>
        val clock = new MutableClock(Instant.parse("2026-07-14T03:00:00Z"))
        val source = BokSource(
          s"bok-${scenario.mode}",
          URI.create(s"https://${scenario.mode}.bok.example/"),
          600,
          true,
          Some(SourceAuthentication(SourceAuthentication.BEARER, s"config-key/${scenario.mode}-credential"))
        )
        val fetcher = new TransitionBokFetcher(source)
        val policy = BokInspectionPolicy(
          refreshTtl = Duration.ofMinutes(5),
          refreshPolicy = InformationSourceRefreshPolicy(Duration.ofMinutes(5))
        )
        val runtime = CbdRuntime.createFederated(
          Vector.empty,
          new InMemoryComponentCatalogProvider(Vector.empty),
          CatalogCachePolicy.DEFAULT,
          clock,
          Vector(source),
          new BokKnowledgeSourceProvider(clock),
          policy
        )
        runtime.ensureInputsReady(fetcher).isSuccess shouldBe true

        When(s"the ${scenario.mode} failure occurs at expiry and readiness is checked again before retry")
        clock.advance(Duration.ofMinutes(5))
        fetcher.failWith(scenario.mode)
        runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
        val requestcountafterfailure = fetcher.requestCount
        runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
        val failedstate = runtime.bokSourceStates(includeDisabled = false).head

        Then(s"the ${scenario.mode} failure retains attributable stale evidence without presenting it as current")
        withClue(s"${scenario.mode}: ") {
          fetcher.requestCount shouldBe requestcountafterfailure
          runtime.bokTerms.map(_.termId) shouldBe Vector("runtime-initial")
          failedstate.status shouldBe "degraded"
          failedstate.cacheStatus shouldBe "stale"
          failedstate.observedAt shouldBe Some(Instant.parse("2026-07-14T03:00:00Z"))
          failedstate.expiresAt shouldBe Some(Instant.parse("2026-07-14T03:05:00Z"))
          failedstate.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T03:05:00Z"))
          failedstate.nextRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T03:06:00Z"))
          failedstate.diagnostics.mkString(" ") should include(scenario.diagnosticfragment)
        }

        When(s"the ${scenario.mode} source succeeds at its bounded retry time")
        clock.advance(Duration.ofMinutes(1))
        fetcher.recover()
        runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
        val recoveredstate = runtime.bokSourceStates(includeDisabled = false).head

        Then("only the successful retry replaces observation time and clears the failure")
        withClue(s"${scenario.mode}: ") {
          runtime.bokTerms.map(_.termId) shouldBe Vector("runtime-recovered")
          recoveredstate.status shouldBe "ready"
          recoveredstate.cacheStatus shouldBe "fresh"
          recoveredstate.observedAt shouldBe Some(Instant.parse("2026-07-14T03:06:00Z"))
          recoveredstate.expiresAt shouldBe Some(Instant.parse("2026-07-14T03:11:00Z"))
          recoveredstate.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T03:06:00Z"))
          recoveredstate.nextRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T03:11:00Z"))
          recoveredstate.diagnostics shouldBe empty
        }
      }
    }
  }

  "SIE-mediated BoK input" should {
    "reject oversized query work before transport and keep responses query scoped" in {
      Given("a query longer than the adapter policy and a transport that records calls")
      val policy = SieBokPolicy(maxQueryCharacters = 8)
      val transport = new RecordingSieBokTransport
      val provider = new SieBokProvider(Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC))

      When("the query reaches the public SIE adapter")
      val result = provider.searchTerms(
        SieBokSource("sie", URI.create("https://sie.example/mcp"), 700, true),
        "a-query-that-is-too-long",
        None,
        10,
        transport,
        policy
      )

      Then("the bounded request fails without network work or a reusable response cache")
      result.isFaillure shouldBe true
      transport.callCount shouldBe 0
    }
  }

  "Local development and CAR storage input" should {
    "record each bounded inspection time and read current evidence without a retained cache" in {
      Given("one explicitly authorized development project and a controllable inspection clock")
      val root = _reset_work_area("local-observation")
      val development = Files.createDirectories(root.resolve("development"))
      val local = Files.createDirectories(root.resolve("local"))
      val cache = Files.createDirectories(root.resolve("cache"))
      Files.writeString(development.resolve("project.yaml"), _project_yaml("1.0.0"))
      val configuration = LocalInformationSourceConfig.parse(
        Some(s"working=$development"),
        Some(local.toString),
        Some(cache.toString),
        root
      )
      val clock = new MutableClock(Instant.parse("2026-07-14T01:00:00Z"))

      When("the project changes between two independent bounded inspections")
      val first = LocalInformationSourceInventory.inspect(configuration, LocalInspectionPolicy.DEFAULT, clock)
      Files.writeString(development.resolve("project.yaml"), _project_yaml("1.1.0-SNAPSHOT"))
      clock.advance(Duration.ofMinutes(1))
      val second = LocalInformationSourceInventory.inspect(configuration, LocalInspectionPolicy.DEFAULT, clock)

      Then("each inventory exposes its own observation time and no last-known snapshot masks the change")
      first.observedAt shouldBe Instant.parse("2026-07-14T01:00:00Z")
      first.observations.flatMap(_.version) should contain("1.0.0")
      second.observedAt shouldBe Instant.parse("2026-07-14T01:01:00Z")
      second.observations.flatMap(_.version) should contain("1.1.0-SNAPSHOT")
    }

    "bound the latest local inventory across configured source roots" in {
      Given("two development directories and a runtime retention limit of one local observation")
      val root = _reset_work_area("bounded-local-observations")
      val first = Files.createDirectories(root.resolve("first"))
      val second = Files.createDirectories(root.resolve("second"))
      Files.writeString(first.resolve("project.yaml"), _project_yaml("1.0.0"))
      Files.writeString(second.resolve("project.yaml"), _project_yaml("2.0.0-SNAPSHOT"))
      val configuration = LocalInformationSourceConfig.parse(
        Some(s"first=$first,second=$second"),
        None,
        None,
        root
      )
      val clock = Clock.fixed(Instant.parse("2026-07-14T02:00:00Z"), ZoneOffset.UTC)
      val runtime = CbdRuntime.createFederated(
        Vector.empty,
        new InMemoryComponentCatalogProvider(Vector.empty),
        CatalogCachePolicy.DEFAULT,
        clock,
        Vector.empty,
        new BokKnowledgeSourceProvider(clock),
        localconfiguration = configuration,
        retentionpolicy = InformationSourceRetentionPolicy(maxLocalObservations = 1)
      )

      When("the runtime replaces its no-cache local inventory")
      runtime.ensureInputsReady(new SwitchableBokFetcher(Map.empty)).isSuccess shouldBe true
      val states = runtime.localSourceStates(includedisabled = false)

      Then("one deterministic observation is retained and the affected source reports truncation")
      runtime.localInventory.toVector.flatMap(_.observations).flatMap(_.version) shouldBe Vector("1.0.0")
      states.map(_.observationCount).sum shouldBe 1
      states.find(_.descriptor.id == "second").toVector.flatMap(_.diagnostics).mkString(" ") should include(
        "runtime total policy limit of 1"
      )
    }
  }

  private def _project_yaml(version: String): String =
    s"""project:
       |  name: textus-order
       |  kind: car
       |  component:
       |    name: textus-order
       |    version: $version
       |""".stripMargin

  private def _reset_work_area(name: String): Path = {
    val root = Path.of("target", "test-work", "information-source-refresh", name).toAbsolutePath.normalize()
    if (Files.exists(root)) {
      val stream = Files.walk(root)
      try stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
      finally stream.close()
    }
    Files.createDirectories(root)
  }

  private final class RecordingCatalogFetcher(values: Map[URI, String]) extends CatalogFetcher {
    var requests = Vector.empty[(URI, Int)]

    def get(uri: URI): Consequence[String] =
      values.get(uri).map(Consequence.success).getOrElse(Consequence.serviceUnavailable(s"Missing fixture: $uri"))

    override def get(uri: URI, maxbytes: Int): Consequence[String] = {
      requests = requests :+ (uri -> maxbytes)
      super.get(uri, maxbytes)
    }
  }

  private final class SwitchableBokFetcher(values: Map[URI, String]) extends CatalogFetcher with BokFetcher {
    var fail = false
    var requests = Vector.empty[(URI, Int)]

    def get(uri: URI): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected catalog fetch: $uri")

    override def get(uri: URI, maxbytes: Int): Consequence[String] = {
      requests = requests :+ (uri -> maxbytes)
      if (fail) Consequence.serviceUnavailable("BoK source is unavailable.")
      else values.get(uri).map(Consequence.success).getOrElse(Consequence.serviceUnavailable(s"Missing fixture: $uri"))
    }
  }

  private final class RecordingSieBokTransport extends SieBokTransport {
    var callCount = 0

    def postJson(endpoint: URI, body: String, maxbytes: Int): Consequence[String] = {
      callCount += 1
      Consequence.serviceUnavailable("Unexpected SIE transport call.")
    }
  }

  private final case class FailureTransition(mode: String, diagnosticfragment: String)

  private final class TransitionBokFetcher(source: BokSource) extends CatalogFetcher with BokFetcher {
    private val _manifest_uri = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
    private val _terms_uri = source.baseUri.resolve("metadata/glossary/terms.json")
    private var _mode = "ready"
    private var _request_count = 0

    def requestCount: Int = _request_count

    def failWith(mode: String): Unit =
      _mode = mode

    def recover(): Unit =
      _mode = "recovered"

    def get(uri: URI): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected unbounded fetch: $uri")

    override def get(uri: URI, maxbytes: Int): Consequence[String] = {
      _request_count += 1
      if (uri == _manifest_uri) _manifest_response
      else if (uri == _terms_uri) Consequence.success(_terms_response)
      else Consequence.serviceUnavailable(s"Unexpected BoK fetch: $uri")
    }

    private def _manifest_response: Consequence[String] =
      _mode match {
        case "authentication" =>
          SourceAuthenticationFailure.expired(SourceAuthenticationRequest.from(source)).consequence
        case "transport" => Consequence.serviceUnavailable("transport-unavailable: BoK manifest request failed.")
        case "parse" => Consequence.success("{")
        case "compatibility" => Consequence.success(_manifest("cncf.knowledge-source.v2"))
        case _ => Consequence.success(_manifest("cncf.knowledge-source.v1"))
      }

    private def _manifest(schemaversion: String): String =
      s"""{"schemaVersion":"$schemaversion","kind":"bok-site","id":"${source.id}","sourceRef":{"kind":"bok-site","value":"${source.id}"},"resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"}]}"""

    private def _terms_response: String = {
      val termid = if (_mode == "recovered") "runtime-recovered" else "runtime-initial"
      s"""{"terms":[{"id":"$termid","title":"Runtime"}]}"""
    }
  }

  private final class MutableClock(private var _current: Instant) extends Clock {
    override def getZone: ZoneId = ZoneOffset.UTC

    override def withZone(zone: ZoneId): Clock = Clock.fixed(_current, zone)

    override def instant(): Instant = _current

    def advance(duration: Duration): Unit =
      _current = _current.plus(duration)
  }
}
