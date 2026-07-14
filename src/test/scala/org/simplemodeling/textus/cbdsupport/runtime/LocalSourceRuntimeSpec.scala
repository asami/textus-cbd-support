package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.jdk.CollectionConverters.*

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class LocalSourceRuntimeSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "LocalInformationSourceConfig" should {
    "accept only explicit canonical directories and reject a symlink root" in {
      Given("one development directory, canonical CAR roots, and a symlink to an unrelated directory")
      val work = _reset_work_area("configuration")
      val development = Files.createDirectories(work.resolve("textus-order"))
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))
      val outside = Files.createDirectories(work.resolve("outside"))
      val link = work.resolve("linked-project")
      Files.createSymbolicLink(link, outside)

      When("the local source configuration is parsed")
      val configuration = LocalInformationSourceConfig.parse(
        Some(s"order=$development,linked=$link"),
        Some(localroot.toString),
        Some(cacheroot.toString),
        work
      )

      Then("only canonical allowlisted paths become read-only source descriptors")
      configuration.developmentSources.map(_.descriptor.id) shouldBe Vector("order")
      configuration.developmentSources.head.descriptor.authorization shouldBe InformationSourceAuthorization.EXPLICIT_PATH_ALLOWLIST
      configuration.carStorageSources.map(_.descriptor.id) shouldBe Vector("local-car", "cache-car")
      configuration.carStorageSources.map(_.descriptor.authorization).distinct shouldBe Vector(InformationSourceAuthorization.EXPLICIT_PATH_ALLOWLIST)
      configuration.warnings.exists(_.contains("symbolic-link roots are not allowed")) shouldBe true
    }

    "authorize default CAR roots through canonical storage policy" in {
      Given("a home root containing the canonical local warehouse and managed cache directories")
      val home = _reset_work_area("canonical-storage-authorization")
      val localroot = Files.createDirectories(home.resolve(".cncf/local"))
      val cacheroot = Files.createDirectories(home.resolve(".cncf/cache"))

      When("local source configuration uses both default CAR roots")
      val configuration = LocalInformationSourceConfig.parse(None, None, None, home)

      Then("the canonical roots are distinct from explicitly configured path authority")
      configuration.carStorageSources.map(_.root) shouldBe Vector(localroot, cacheroot)
      configuration.carStorageSources.map(_.descriptor.authorization).distinct shouldBe
        Vector(InformationSourceAuthorization.CANONICAL_STORAGE_ROOT)
      configuration.warnings shouldBe empty
    }

    "reserve stable CAR source IDs against development-directory collisions" in {
      Given("a development directory configured with the stable local CAR source ID")
      val work = _reset_work_area("reserved-source-id")
      val development = Files.createDirectories(work.resolve("textus-order"))
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))

      When("the local source configuration is parsed")
      val configuration = LocalInformationSourceConfig.parse(
        Some(s"local-car=$development"),
        Some(localroot.toString),
        Some(cacheroot.toString),
        work
      )

      Then("the conflicting development source is rejected and all accepted IDs remain unique")
      configuration.developmentSources shouldBe empty
      configuration.sources.map(_.descriptor.id).distinct shouldBe configuration.sources.map(_.descriptor.id)
      configuration.warnings.exists(_.contains("reserved or duplicated")) shouldBe true
    }
  }

  "LocalInformationSourceInventory" should {
    "inspect a configured development project as working evidence" in {
      Given("a canonical Cozy project directory with project identity and version metadata")
      val work = _reset_work_area("development")
      val development = Files.createDirectories(work.resolve("textus-order"))
      Files.writeString(
        development.resolve("project.yaml"),
        """project:
          |  name: "textus-order"
          |  kind: car
          |  organization: "org.textus"
          |  component:
          |    name: "textus-order"
          |    version: "0.2.0-SNAPSHOT"
          |""".stripMargin,
        StandardCharsets.UTF_8
      )
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))
      val configuration = LocalInformationSourceConfig.parse(
        Some(s"order=$development"),
        Some(localroot.toString),
        Some(cacheroot.toString),
        work
      )

      When("the explicitly configured sources are inspected")
      val inventory = LocalInformationSourceInventory.inspect(configuration)
      val observation = inventory.observations.find(_.sourceId == "order").get

      Then("project.yaml remains attributable as working version evidence")
      observation.componentName shouldBe Some("textus-order")
      observation.organization shouldBe Some("org.textus")
      observation.componentKind shouldBe Some("car")
      observation.version shouldBe Some("0.2.0-SNAPSHOT")
      observation.versionEvidence shouldBe "project-yaml"
      observation.versionState shouldBe "working"
      observation.artifactChecksumSha256 shouldBe None
      observation.diagnostics shouldBe empty
    }

    "distinguish locally published and cached CAR version evidence" in {
      Given("one current local CAR and one legacy cached CAR whose descriptor omits version")
      val work = _reset_work_area("car-storage")
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))
      val localcar = localroot.resolve("repository/car/textus-order/1.2.0/textus-order-1.2.0.car")
      val cachecar = cacheroot.resolve("car/textus-order/1.1.0/textus-order-1.1.0.car")
      _write_car(localcar, """{"name":"textus-order","version":"1.2.0","component":"textus-order"}""")
      _write_car(cachecar, """{"component":{"name":"textus-order"},"componentlets":[]}""")
      val configuration = LocalInformationSourceConfig.parse(
        None,
        Some(localroot.toString),
        Some(cacheroot.toString),
        work
      )

      When("the local and cache storage roots are inventoried")
      val inventory = LocalInformationSourceInventory.inspect(configuration)
      val local = inventory.observations.find(_.sourceId == "local-car").get
      val cached = inventory.observations.find(_.sourceId == "cache-car").get

      Then("storage state and descriptor/path version authority remain separate")
      local.componentName shouldBe Some("textus-order")
      local.version shouldBe Some("1.2.0")
      local.descriptorVersion shouldBe Some("1.2.0")
      local.pathVersion shouldBe Some("1.2.0")
      local.versionEvidence shouldBe "component-descriptor"
      local.versionState shouldBe "local-published"
      local.artifactChecksumSha256.exists(_.length == 64) shouldBe true
      cached.version shouldBe Some("1.1.0")
      cached.descriptorVersion shouldBe None
      cached.pathVersion shouldBe Some("1.1.0")
      cached.versionEvidence shouldBe "repository-path"
      cached.versionState shouldBe "cached"
      cached.diagnostics.exists(_.contains("no version")) shouldBe true
      cached.artifactChecksumSha256 should not be local.artifactChecksumSha256
    }

    "stop discovery at the configured artifact bound" in {
      Given("a storage root with two CARs and an artifact bound of one")
      val work = _reset_work_area("bounded")
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))
      _write_car(localroot.resolve("repository/car/a/1.0.0/a-1.0.0.car"), """{"name":"a","version":"1.0.0","component":"a"}""")
      _write_car(localroot.resolve("repository/car/b/1.0.0/b-1.0.0.car"), """{"name":"b","version":"1.0.0","component":"b"}""")
      val configuration = LocalInformationSourceConfig.parse(None, Some(localroot.toString), Some(cacheroot.toString), work)
      val policy = LocalInspectionPolicy(maxCarArtifacts = 1)

      When("the bounded inventory is collected")
      val inventory = LocalInformationSourceInventory.inspect(configuration, policy)

      Then("only one CAR is inspected and truncation remains observable")
      inventory.observations.count(_.sourceId == "local-car") shouldBe 1
      inventory.warnings.exists(_.contains("CAR discovery was truncated")) shouldBe true
    }

    "report directories omitted at the configured depth bound" in {
      Given("a CAR stored below the configured directory traversal depth")
      val work = _reset_work_area("depth-bound")
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))
      _write_car(localroot.resolve("repository/car/a/1.0.0/a-1.0.0.car"), """{"name":"a","version":"1.0.0","component":"a"}""")
      val configuration = LocalInformationSourceConfig.parse(None, Some(localroot.toString), Some(cacheroot.toString), work)
      val policy = LocalInspectionPolicy(maxDepth = 1)

      When("the bounded inventory reaches a directory at the depth limit")
      val inventory = LocalInformationSourceInventory.inspect(configuration, policy)

      Then("the omitted CAR remains absent and depth truncation is observable")
      inventory.observations.count(_.sourceId == "local-car") shouldBe 0
      inventory.warnings.exists(_.contains("Directory depth limit reached")) shouldBe true
      inventory.warnings.exists(_.contains("CAR discovery was truncated")) shouldBe true
    }

    "reject checksum work beyond the configured artifact byte bound" in {
      Given("one CAR larger than the allowed checksum inspection size")
      val work = _reset_work_area("artifact-size")
      val localroot = Files.createDirectories(work.resolve("local"))
      val cacheroot = Files.createDirectories(work.resolve("cache"))
      _write_car(localroot.resolve("repository/car/a/1.0.0/a-1.0.0.car"), """{"name":"a","version":"1.0.0","component":"a"}""")
      val configuration = LocalInformationSourceConfig.parse(None, Some(localroot.toString), Some(cacheroot.toString), work)
      val policy = LocalInspectionPolicy(maxArtifactBytes = 32)

      When("the bounded inventory reaches the oversized CAR")
      val inventory = LocalInformationSourceInventory.inspect(configuration, policy)

      Then("the CAR is not hashed and the byte-limit diagnostic remains visible")
      inventory.observations.count(_.sourceId == "local-car") shouldBe 0
      inventory.warnings.exists(_.contains("exceeds 32 bytes")) shouldBe true
    }
  }

  private def _reset_work_area(name: String): Path = {
    val root = Path.of("target", "test-work", "local-source-runtime", name).toAbsolutePath.normalize()
    if (Files.exists(root)) {
      val stream = Files.walk(root)
      try stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
      finally stream.close()
    }
    Files.createDirectories(root)
  }

  private def _write_car(path: Path, descriptor: String): Unit = {
    Files.createDirectories(path.getParent)
    val output = new ZipOutputStream(Files.newOutputStream(path))
    try {
      output.putNextEntry(new ZipEntry("component-descriptor.json"))
      output.write(descriptor.getBytes(StandardCharsets.UTF_8))
      output.closeEntry()
    } finally output.close()
  }
}
