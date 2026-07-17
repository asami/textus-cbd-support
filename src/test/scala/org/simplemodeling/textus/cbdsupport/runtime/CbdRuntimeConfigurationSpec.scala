package org.simplemodeling.textus.cbdsupport.runtime

import java.io.ByteArrayOutputStream
import java.time.{Clock, Instant, ZoneOffset}
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.goldenport.cncf.resource.{ResourceTreeAccess, ResourceTreeEntry, ResourceTreeLimits, ResourceTreeReference}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 17, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class CbdRuntimeConfigurationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CbdRuntime.Configuration" should {
    "construct the runtime from explicit configuration and bound time" in {
      Given("a configuration value with no ambient environment dependency and a fixed execution clock")
      val clock = Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC)
      val configuration = CbdRuntime.Configuration()

      When("the CBD runtime is constructed")
      val runtime = CbdRuntime.create(configuration, clock)

      Then("the default published source remains available without consulting ambient configuration")
      runtime.informationSourceStates(includeDisabled = false).map(_.descriptor.id) should contain ("simplemodeling")
    }

    "inspect development metadata from an admitted resource-tree snapshot" in {
      Given("an in-memory admitted development tree and a fixed clock")
      val reference = ResourceTreeReference.parseC("development").TAKE
      val entry = ResourceTreeEntry.createC(
        "project.yaml",
        """project:
          |  name: sample
          |  component:
          |    name: sample-component
          |    version: 1.2.3
          |  organization: org.example
          |  kind: car
          |""".stripMargin.getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector
      ).TAKE
      val snapshot = ResourceTreeAccess.inMemory(Map(reference -> Vector(entry))).snapshot(reference, ResourceTreeLimits.default).TAKE
      val source = InformationSourceDescriptor("development", InformationSourceKind.DEVELOPMENT_DIRECTORY, "resource-tree:development", 1, true, InformationSourceAuthorization.EXPLICIT)

      When("the local inventory consumes only the snapshot")
      val inventory = LocalInformationSourceInventory.inspectDevelopmentSnapshot(
        source,
        snapshot,
        VersionAvailabilityState.WORKING,
        LocalInspectionPolicy.DEFAULT,
        Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC)
      )

      Then("the observation has logical rather than host-path evidence")
      inventory.observations.map(_.componentName) shouldBe Vector(Some("sample-component"))
      inventory.observations.map(_.evidenceLocation) shouldBe Vector("resource-tree:development/project.yaml")
    }

    "inspect CAR metadata from an admitted resource-tree snapshot" in {
      Given("an in-memory CAR archive under an admitted storage tree")
      val reference = ResourceTreeReference.parseC("local-car").TAKE
      val entry = ResourceTreeEntry.createC(
        "textus-order/1.2.3/textus-order-1.2.3.car",
        _car_bytes("""{"name":"textus-order","version":"1.2.3"}""")
      ).TAKE
      val snapshot = ResourceTreeAccess.inMemory(Map(reference -> Vector(entry))).snapshot(reference, ResourceTreeLimits.default).TAKE
      val source = InformationSourceDescriptor("local-car", InformationSourceKind.CAR_STORAGE, "resource-tree:local-car", 1, true, InformationSourceAuthorization.EXPLICIT)

      When("the local inventory consumes only the snapshot")
      val inventory = LocalInformationSourceInventory.inspectCarStorageSnapshot(
        source,
        snapshot,
        VersionAvailabilityState.LOCAL_PUBLISHED,
        LocalInspectionPolicy.DEFAULT,
        Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC)
      )

      Then("the observation retains version evidence and logical provenance")
      val observation = inventory.observations.head
      observation.componentName shouldBe Some("textus-order")
      observation.version shouldBe Some("1.2.3")
      observation.evidenceLocation shouldBe "resource-tree:local-car/textus-order/1.2.3/textus-order-1.2.3.car"
      observation.artifactChecksumSha256.exists(_.length == 64) shouldBe true
    }
  }

  private def _car_bytes(descriptor: String): Vector[Byte] = {
    val output = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(output)
    try {
      zip.putNextEntry(new ZipEntry("component-descriptor.json"))
      zip.write(descriptor.getBytes(java.nio.charset.StandardCharsets.UTF_8))
      zip.closeEntry()
    } finally zip.close()
    output.toByteArray.toVector
  }
}
