package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.{Clock, Instant, ZoneOffset}
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.collection.mutable.ArrayBuffer
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
final class InformationSourceSecuritySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "InformationSourceDiagnosticPolicy" should {
    "redact URI credentials, query data, authorization values, secret assignments, controls, and excess text" in {
      Given("one external failure containing every prohibited diagnostic value")
      val raw =
        "Fetch failed at https://alice:swordfish@api.example/private?token=query-secret#fragment " +
          "Authorization: Bearer bearer-secret password=hunter2 " +
          "access_token=oauth-secret client_secret=client-value db_password=db-value\n" + "x" * 3000

      When("the failure crosses the information-source diagnostic boundary")
      val sanitized = InformationSourceDiagnosticPolicy.sanitize(raw)

      Then("the diagnostic remains useful and bounded without exposing credential material")
      sanitized should include("https://api.example/private")
      sanitized should include("[redacted]")
      sanitized should include("[truncated]")
      sanitized.length should be <= 2060
      sanitized should not include "alice"
      sanitized should not include "swordfish"
      sanitized should not include "query-secret"
      sanitized should not include "bearer-secret"
      sanitized should not include "hunter2"
      sanitized should not include "oauth-secret"
      sanitized should not include "client-value"
      sanitized should not include "db-value"
      sanitized should not include "\n"
    }

    "sanitize provider failures before they enter unified source state" in {
      Given("an authorized catalog whose provider fails with credential-bearing transport text")
      val source = CatalogSource("secure", URI.create("https://catalog.example/"), 100, true)
      val runtime = CbdRuntime.create(
        Vector(source),
        new FailingCatalogProvider(
          "Transport failed at https://user:password@catalog.example/index?token=query-secret Authorization=Bearer bearer-secret"
        ),
        CatalogCachePolicy.DEFAULT,
        _clock
      )

      When("the failed refresh is recorded")
      val result = runtime.ensureReady(EmptyCatalogFetcher)
      val warning = runtime.sourceStates(includeDisabled = false).head.warning.get
      val failure = result match {
        case Consequence.Failure(conclusion) => conclusion.display
        case _ => fail("The unavailable catalog must fail readiness.")
      }

      Then("both operation failure and retained source diagnostic are sanitized")
      warning should include("https://catalog.example/index")
      warning should not include "password"
      warning should not include "query-secret"
      warning should not include "bearer-secret"
      failure shouldBe warning
    }
  }

  "CatalogUriPolicy" should {
    "reject a credential-bearing same-origin sidecar without fetching it or echoing credentials" in {
      Given("catalog evidence whose model metadata URI embeds credentials and query data on the catalog host")
      val unsafeuri = URI.create(
        "https://alice:swordfish@catalog.example/model-metadata.json?token=query-secret#fragment"
      )
      val profile = _profile(unsafeuri)
      val fetcher = new RecordingCatalogFetcher

      When("usage evidence is requested")
      val usage = new CozyComponentCatalogProvider().readUsage(profile, fetcher).toOption.get

      Then("same-origin comparison does not authorize credentials and the diagnostic URI is safe")
      CatalogUriPolicy.sameOrigin(profile.evidenceUri, unsafeuri) shouldBe true
      CatalogUriPolicy.isAuthorizedFetch(profile.evidenceUri, unsafeuri) shouldBe false
      fetcher.requests shouldBe empty
      usage.warnings.mkString(" ") should include("contains credentials")
      usage.warnings.mkString(" ") should include("https://catalog.example/model-metadata.json")
      usage.warnings.mkString(" ") should not include "alice"
      usage.warnings.mkString(" ") should not include "swordfish"
      usage.warnings.mkString(" ") should not include "query-secret"
    }

    "reject credential-bearing SIE configuration without reflecting its secret" in {
      Given("one SIE component route containing user information and query credentials")

      When("the route is checked against an otherwise matching exact-origin allowlist")
      val configuration = SieBokConfig.parse(
        Some("secret=https://user:password@sie.example/mcp?token=query-secret"),
        Some("https://sie.example")
      )

      Then("no source authority is granted and the indexed rejection remains sanitized")
      configuration.sources shouldBe empty
      configuration.warnings.exists(_.contains("endpoint is invalid")) shouldBe true
      configuration.warnings.mkString(" ") should not include "password"
      configuration.warnings.mkString(" ") should not include "query-secret"
    }
  }

  "LocalInformationSourceInventory" should {
    "ignore a nested symbolic-link escape while retaining canonical authorized roots" in {
      Given("an explicitly authorized CAR root containing a symlink to an artifact outside that root")
      val work = _reset_work_area("nested-symlink")
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))
      val storage = Files.createDirectories(localroot.resolve("repository/car"))
      val outside = Files.createDirectories(work.resolve("outside"))
      _write_car(outside.resolve("escaped/1.0.0/escaped-1.0.0.car"))
      Files.createSymbolicLink(storage.resolve("escaped"), outside.resolve("escaped"))
      val configuration = LocalInformationSourceConfig.parse(
        None,
        Some(localroot.resolve(".").toString),
        Some(cacheroot.resolve(".").toString),
        work
      )

      When("the authorized local and cache roots are inventoried without following links")
      val inventory = LocalInformationSourceInventory.inspect(configuration)

      Then("the roots are canonical and the escaped CAR never becomes an observation")
      configuration.carStorageSources.map(_.root) shouldBe Vector(
        localroot.toRealPath(),
        cacheroot.toRealPath()
      )
      inventory.observations shouldBe empty
      inventory.observations.exists(_.evidenceLocation.contains("outside")) shouldBe false
    }
  }

  private val _clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC)

  private def _profile(modelmetadatauri: URI): ComponentProfile =
    ComponentProfile(
      "secure",
      Some("org.textus"),
      "textus-order",
      "Textus Order",
      Some("Order component."),
      "car",
      Vector("1.0.0"),
      Some("1.0.0"),
      None,
      Some("1.0.0"),
      None,
      None,
      Vector.empty,
      Vector.empty,
      Vector.empty,
      None,
      URI.create("https://catalog.example/order"),
      Some(modelmetadatauri),
      None,
      Vector.empty,
      Vector.empty
    )

  private def _reset_work_area(name: String): Path = {
    val root = Path.of("target", "test-work", "information-source-security", name).toAbsolutePath.normalize()
    if (Files.exists(root)) {
      val stream = Files.walk(root)
      try stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
      finally stream.close()
    }
    Files.createDirectories(root)
  }

  private def _write_car(path: Path): Unit = {
    Files.createDirectories(path.getParent)
    val output = new ZipOutputStream(Files.newOutputStream(path))
    try {
      output.putNextEntry(new ZipEntry("component-descriptor.json"))
      output.write("{\"name\":\"escaped\",\"version\":\"1.0.0\"}".getBytes(StandardCharsets.UTF_8))
      output.closeEntry()
    } finally output.close()
  }

  private final class RecordingCatalogFetcher extends CatalogFetcher {
    val requests = ArrayBuffer.empty[URI]

    def get(uri: URI): Consequence[String] = {
      requests += uri
      Consequence.serviceUnavailable("Unexpected fetch")
    }
  }

  private final class FailingCatalogProvider(message: String) extends ComponentCatalogProvider {
    def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
      Consequence.serviceUnavailable(message)

    def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
      Consequence.serviceUnavailable(message)
  }

  private object EmptyCatalogFetcher extends CatalogFetcher {
    def get(uri: URI): Consequence[String] = Consequence.serviceUnavailable("Unexpected fetch")
  }
}
