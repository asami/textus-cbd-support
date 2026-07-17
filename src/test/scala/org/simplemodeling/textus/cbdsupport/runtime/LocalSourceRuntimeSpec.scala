package org.simplemodeling.textus.cbdsupport.runtime

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant, ZoneOffset}
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.goldenport.cncf.resource.{ResourceTreeAccess, ResourceTreeEntry, ResourceTreeLimits, ResourceTreeReference}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 17, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class LocalSourceRuntimeSpec extends AnyWordSpec with Matchers {
  private val _clock = Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC)

  "LocalInformationSourceInventory" should {
    "read development evidence only from an admitted snapshot" in {
      val reference = ResourceTreeReference.parseC("development").TAKE
      val snapshot = ResourceTreeAccess.inMemory(Map(reference -> Vector(
        ResourceTreeEntry.createC(
          "project.yaml",
          "project:\n  component:\n    name: textus-order\n    version: 1.2.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8).toVector
        ).TAKE
      ))).snapshot(reference, ResourceTreeLimits.default).TAKE

      val inventory = LocalInformationSourceInventory.inspectDevelopmentSnapshot(
        _source("development", InformationSourceKind.DEVELOPMENT_DIRECTORY),
        snapshot,
        VersionAvailabilityState.WORKING,
        LocalInspectionPolicy.DEFAULT,
        _clock
      )

      inventory.observations.map(_.componentName) shouldBe Vector(Some("textus-order"))
      inventory.observations.map(_.evidenceLocation) shouldBe Vector("resource-tree:development/project.yaml")
    }

    "read CAR evidence and checksum only from an admitted snapshot" in {
      val reference = ResourceTreeReference.parseC("local-car").TAKE
      val snapshot = ResourceTreeAccess.inMemory(Map(reference -> Vector(
        ResourceTreeEntry.createC(
          "textus-order/1.2.0/textus-order-1.2.0.car",
          _car("""{"name":"textus-order","version":"1.2.0"}""")
        ).TAKE
      ))).snapshot(reference, ResourceTreeLimits.default).TAKE

      val inventory = LocalInformationSourceInventory.inspectCarStorageSnapshot(
        _source("local-car", InformationSourceKind.CAR_STORAGE),
        snapshot,
        VersionAvailabilityState.LOCAL_PUBLISHED,
        LocalInspectionPolicy.DEFAULT,
        _clock
      )

      val observation = inventory.observations.head
      observation.version shouldBe Some("1.2.0")
      observation.evidenceLocation shouldBe "resource-tree:local-car/textus-order/1.2.0/textus-order-1.2.0.car"
      observation.artifactChecksumSha256.exists(_.length == 64) shouldBe true
    }

    "reject descriptor and path version conflicts instead of selecting either input" in {
      val reference = ResourceTreeReference.parseC("local-car").TAKE
      val snapshot = ResourceTreeAccess.inMemory(Map(reference -> Vector(
        ResourceTreeEntry.createC(
          "textus-order/1.2.0/textus-order-1.2.0.car",
          _car("""{"name":"textus-order","version":"2.0.0"}""")
        ).TAKE
      ))).snapshot(reference, ResourceTreeLimits.default).TAKE

      val inventory = LocalInformationSourceInventory.inspectCarStorageSnapshot(
        _source("local-car", InformationSourceKind.CAR_STORAGE),
        snapshot,
        VersionAvailabilityState.LOCAL_PUBLISHED,
        LocalInspectionPolicy.DEFAULT,
        _clock
      )

      inventory.observations shouldBe empty
      inventory.warnings.mkString(" ") should include("Component version conflicts: descriptor=2.0.0, path=1.2.0.")
    }
  }

  private def _source(id: String, kind: String): InformationSourceDescriptor =
    InformationSourceDescriptor(id, kind, s"resource-tree:$id", 1, true, InformationSourceAuthorization.EXPLICIT)

  private def _car(descriptor: String): Vector[Byte] = {
    val bytes = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(bytes)
    try {
      zip.putNextEntry(new ZipEntry("component-descriptor.json"))
      zip.write(descriptor.getBytes(StandardCharsets.UTF_8))
      zip.closeEntry()
    } finally zip.close()
    bytes.toByteArray.toVector
  }
}
