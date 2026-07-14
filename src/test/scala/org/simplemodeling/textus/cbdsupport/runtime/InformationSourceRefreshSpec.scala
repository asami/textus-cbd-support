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
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class InformationSourceRefreshSpec extends AnyWordSpec with Matchers with GivenWhenThen {
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
      val policy = BokInspectionPolicy(refreshTtl = Duration.ofMinutes(5))
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
      clock.advance(Duration.ofMinutes(4))
      runtime.ensureInputsReady(fetcher).isSuccess shouldBe true
      fetcher.fail = true
      clock.advance(Duration.ofMinutes(1))
      runtime.ensureInputsReady(fetcher).isSuccess shouldBe true

      Then("fresh reuse performs no work and stale evidence remains attributable to the failed refresh")
      fetcher.requests should have size 3
      runtime.bokTerms.map(_.termId) shouldBe Vector("runtime")
      val state = runtime.bokSourceStates(includeDisabled = false).head
      state.status shouldBe "degraded"
      state.cacheStatus shouldBe "stale"
      state.observedAt shouldBe Some(Instant.parse("2026-07-14T00:00:00Z"))
      state.expiresAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.lastRefreshAttemptAt shouldBe Some(Instant.parse("2026-07-14T00:05:00Z"))
      state.diagnostics.mkString(" ") should include("unavailable")
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

  private final class MutableClock(private var _current: Instant) extends Clock {
    override def getZone: ZoneId = ZoneOffset.UTC

    override def withZone(zone: ZoneId): Clock = Clock.fixed(_current, zone)

    override def instant(): Instant = _current

    def advance(duration: Duration): Unit =
      _current = _current.plus(duration)
  }
}
