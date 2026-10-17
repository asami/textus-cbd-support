package org.simplemodeling.textus.cbdsupport.runtime

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant, ZoneOffset}
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.knowledge.{
  ComponentKnowledgeCarrier,
  ComponentKnowledgeCarrierCodec,
  ComponentKnowledgeManifestConsumerContractCodec
}
import org.goldenport.cncf.resource.{ResourceTreeAccess, ResourceTreeEntry, ResourceTreeLimits, ResourceTreeQuery, ResourceTreeReference}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * DOC-06 failing-first specification for CAR-local carrier admission.
 *
 * The inventory is permitted to read only the descriptor-declared logical
 * entry.  It retains an admitted value contract, never the raw entry bytes.
 */
/*
 * @since   Aug. 26, 2026
 * @version Oct. 17, 2026
 */
final class ComponentKnowledgeLocalSourceSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _clock = Clock.fixed(Instant.parse("2026-08-26T00:00:00Z"), ZoneOffset.UTC)
  private val _component_id = ComponentId("org.example.Order")
  private val _release = "1.2.0"

  "LocalInformationSourceInventory" should {
    "admit descriptor-declared development evidence when its supported runtime manifest names the carrier path" in {
      Given("a prepared development root with a generated descriptor, fixed carrier path, and matching runtime evidence")
      val contract = _consumer_contract_bytes
      val carrier = ComponentKnowledgeCarrier.createC().toOption.get
      val reference = ResourceTreeReference.parseC("development").TAKE
      val entries = Vector(
        ResourceTreeEntry.createC("project.yaml", s"""project:
          |  organization: org.example
          |  kind: car
          |  component:
          |    name: Order
          |    version: $_release
          |""".stripMargin.getBytes(StandardCharsets.UTF_8).toVector).TAKE,
        ResourceTreeEntry.createC("target/cncf.d/component-descriptor.json", _descriptor(Some(carrier)).getBytes(StandardCharsets.UTF_8).toVector).TAKE,
        ResourceTreeEntry.createC("target/cncf.d/component-knowledge.json", contract).TAKE,
        ResourceTreeEntry.createC("target/cncf.d/car-runtime-manifest.json", _runtime_manifest.getBytes(StandardCharsets.UTF_8).toVector).TAKE
      )
      val access = ResourceTreeAccess.inMemory(Map(reference -> entries))

      When("the fixed development evidence leaves are joined at the project root")
      val inventory = LocalInformationSourceInventory.inspectDevelopmentEvidenceQueries(
        InformationSourceDescriptor("development", InformationSourceKind.DEVELOPMENT_DIRECTORY, "resource-tree:development", 1, true, InformationSourceAuthorization.EXPLICIT),
        _query(access, reference, "project.yaml"),
        _query(access, reference, "component-descriptor.json"),
        _query(access, reference, "component-knowledge.json"),
        _query(access, reference, "car-runtime-manifest.json"),
        VersionAvailabilityState.WORKING,
        LocalInspectionPolicy.DEFAULT,
        _clock
      )

      Then("only the contract value is admitted; the development resource bytes do not become an output")
      val observation = inventory.observations.head
      observation.componentName shouldBe Some("Order")
      observation.artifactChecksumSha256 shouldBe None
      observation.componentKnowledge match {
        case ComponentKnowledgeIntegration.Admitted(value, carrier) =>
          value.componentId shouldBe _component_id
          value.logicalRelease shouldBe _release
          carrier.logicalPath shouldBe "component-knowledge.json"
        case other => fail(s"expected admitted development Component knowledge, got $other")
      }
    }

    "reject declared development knowledge when no generated runtime evidence names the carrier" in {
      Given("a prepared development root with a descriptor declaration but no runtime manifest leaf")
      val contract = _consumer_contract_bytes
      val carrier = ComponentKnowledgeCarrier.createC().toOption.get
      val reference = ResourceTreeReference.parseC("development").TAKE
      val entries = Vector(
        ResourceTreeEntry.createC("project.yaml", s"""project:
          |  organization: org.example
          |  kind: car
          |  component:
          |    name: Order
          |    version: $_release
          |""".stripMargin.getBytes(StandardCharsets.UTF_8).toVector).TAKE,
        ResourceTreeEntry.createC("target/cncf.d/component-descriptor.json", _descriptor(Some(carrier)).getBytes(StandardCharsets.UTF_8).toVector).TAKE,
        ResourceTreeEntry.createC("target/cncf.d/component-knowledge.json", contract).TAKE
      )
      val access = ResourceTreeAccess.inMemory(Map(reference -> entries))

      When("the fixed development evidence leaves are joined")
      val inventory = LocalInformationSourceInventory.inspectDevelopmentEvidenceQueries(
        InformationSourceDescriptor("development", InformationSourceKind.DEVELOPMENT_DIRECTORY, "resource-tree:development", 1, true, InformationSourceAuthorization.EXPLICIT),
        _query(access, reference, "project.yaml"),
        _query(access, reference, "component-descriptor.json"),
        _query(access, reference, "component-knowledge.json"),
        _query(access, reference, "car-runtime-manifest.json"),
        VersionAvailabilityState.WORKING,
        LocalInspectionPolicy.DEFAULT,
        _clock
      )

      Then("the generic development observation remains, while Component knowledge is explicitly rejected")
      val observation = inventory.observations.head
      observation.componentKnowledge shouldBe a[ComponentKnowledgeIntegration.Rejected]
      observation.diagnostics.mkString(" ") should include("no target/cncf.d/car-runtime-manifest.json evidence")
    }

    "reject development runtime evidence with a wrong or missing declared carrier path" in {
      Given("a valid descriptor and consumer contract with an alternate runtime path or no carrier evidence entry")
      val manifests = Vector(
        _runtime_manifest.replace("target/cncf.d/component-knowledge.json", "target/cncf.d/alternate-knowledge.json"),
        """{"schemaVersion":"cncf.car-development-runtime-manifest.v2","evidence":[]}"""
      )

      When("the configured development evidence is admitted")
      val observations = manifests.map(manifest => _development_inventory(_consumer_contract_bytes, manifest).observations.head)

      Then("generic observations remain while neither path declaration admits Component knowledge")
      observations.foreach { observation =>
        observation.componentName shouldBe Some("Order")
        observation.artifactChecksumSha256 shouldBe None
        observation.componentKnowledge shouldBe a[ComponentKnowledgeIntegration.Rejected]
        observation.diagnostics.mkString(" ") should include("does not declare the Component knowledge carrier")
      }
    }

    "reject development runtime evidence outside the supported schema and path-array shape" in {
      Given("a generated carrier declaration with an unsupported manifest schema or non-array evidence")
      val manifests = Vector(
        _runtime_manifest.replace("cncf.car-development-runtime-manifest.v2", "cncf.car-development-runtime-manifest.v3"),
        """{"schemaVersion":"cncf.car-development-runtime-manifest.v2","evidence":{}}"""
      )

      When("the development carrier gate consumes each manifest")
      val observations = manifests.map(manifest => _development_inventory(_consumer_contract_bytes, manifest).observations.head)

      Then("unsupported schema and malformed shape are rejected without losing the generic Component")
      observations.foreach { observation =>
        observation.componentName shouldBe Some("Order")
        observation.componentKnowledge shouldBe a[ComponentKnowledgeIntegration.Rejected]
      }
      observations.head.diagnostics.mkString(" ") should include("schema is unsupported")
      observations(1).diagnostics.mkString(" ") should include("evidence must be an array")
    }

    "reject development contracts for a different declared Component or release" in {
      Given("a canonical runtime path declaration with otherwise valid contracts for another Component or release")
      val text = new String(_consumer_contract_bytes.toArray, StandardCharsets.UTF_8)
      val contracts = Vector(
        text.replace("org.example.Order", "org.example.Other"),
        text.replace(_release, "9.9.9")
      ).map(_.getBytes(StandardCharsets.UTF_8).toVector)

      When("the declared consumer contracts are compared to the descriptor's explicit identity")
      val observations = contracts.map(contract => _development_inventory(contract, _runtime_manifest).observations.head)

      Then("both mismatches are rejected while the descriptor's generic identity remains available")
      observations.foreach { observation =>
        observation.componentName shouldBe Some("Order")
        observation.componentKnowledge shouldBe a[ComponentKnowledgeIntegration.Rejected]
      }
      observations.head.diagnostics.mkString(" ") should include("component identity does not match")
      observations(1).diagnostics.mkString(" ") should include("logical release does not match")
    }

    "admit only the descriptor-declared CAR knowledge carrier as value-only evidence" in {
      Given("a canonical descriptor and a carrier declaring its one consumer-contract path")
      val contract = _consumer_contract_bytes
      val carrier = ComponentKnowledgeCarrier.createC().toOption.get
      val archive = _car(Vector(
        "component-descriptor.json" -> _descriptor(Some(carrier)).getBytes(StandardCharsets.UTF_8).toVector,
        "component-knowledge.json" -> contract
      ))

      When("the admitted CAR snapshot is inspected")
      val inventory = _inventory(archive)
      val observation = inventory.observations.head

      Then("the canonical coordinate and value-only contract are retained without raw resource bytes")
      observation.componentName shouldBe Some("Order")
      observation.organization shouldBe Some("org.example")
      observation.version shouldBe Some(_release)
      observation.componentKnowledge match {
        case ComponentKnowledgeIntegration.Admitted(value, carrier) =>
          value.componentId shouldBe _component_id
          value.logicalRelease shouldBe _release
          value.resources.map(_.logicalPath) shouldBe Vector("documentation/order.md")
          carrier.logicalPath shouldBe "component-knowledge.json"
        case other => fail(s"expected admitted Component knowledge, got $other")
      }
    }

    "not discover an archive carrier that the descriptor does not declare" in {
      Given("a canonical descriptor with no componentKnowledge declaration and an arbitrary matching-path archive entry")
      val archive = _car(Vector(
        "component-descriptor.json" -> _descriptor(None).getBytes(StandardCharsets.UTF_8).toVector,
        "component-knowledge.json" -> "not a consumer contract".getBytes(StandardCharsets.UTF_8).toVector
      ))

      When("the CAR is inspected")
      val inventory = _inventory(archive)
      val observation = inventory.observations.head

      Then("the entry remains absent rather than becoming an ambient discovery input")
      observation.componentKnowledge shouldBe ComponentKnowledgeIntegration.Absent
      observation.diagnostics should not contain "Component knowledge carrier was rejected"
    }

    "retain generic CAR observation while rejecting malformed declared consumer-contract bytes" in {
      Given("a canonical carrier declaration whose named archive entry is malformed UTF-8")
      val carrier = ComponentKnowledgeCarrier.createC().toOption.get
      val archive = _car(Vector(
        "component-descriptor.json" -> _descriptor(Some(carrier)).getBytes(StandardCharsets.UTF_8).toVector,
        "component-knowledge.json" -> Vector(0xc3.toByte, 0x28.toByte)
      ))

      When("the CAR is inspected")
      val inventory = _inventory(archive)
      val observation = inventory.observations.head

      Then("the generic coordinate is retained but no consumer content is admitted")
      observation.componentName shouldBe Some("Order")
      observation.componentKnowledge shouldBe a[ComponentKnowledgeIntegration.Rejected]
      observation.diagnostics.mkString(" ") should include("Component knowledge carrier was rejected")
    }
  }

  private def _development_inventory(contract: Vector[Byte], manifest: String): LocalInformationInventory = {
    val carrier = ComponentKnowledgeCarrier.createC().TAKE
    val reference = ResourceTreeReference.parseC("development").TAKE
    val entries = Vector(
      ResourceTreeEntry.createC("project.yaml", s"""project:
        |  organization: org.example
        |  kind: car
        |  component:
        |    name: Order
        |    version: $_release
        |""".stripMargin.getBytes(StandardCharsets.UTF_8).toVector).TAKE,
      ResourceTreeEntry.createC("target/cncf.d/component-descriptor.json", _descriptor(Some(carrier)).getBytes(StandardCharsets.UTF_8).toVector).TAKE,
      ResourceTreeEntry.createC("target/cncf.d/component-knowledge.json", contract).TAKE,
      ResourceTreeEntry.createC("target/cncf.d/car-runtime-manifest.json", manifest.getBytes(StandardCharsets.UTF_8).toVector).TAKE
    )
    val access = ResourceTreeAccess.inMemory(Map(reference -> entries))
    LocalInformationSourceInventory.inspectDevelopmentEvidenceQueries(
      InformationSourceDescriptor("development", InformationSourceKind.DEVELOPMENT_DIRECTORY, "resource-tree:development", 1, true, InformationSourceAuthorization.EXPLICIT),
      _query(access, reference, "project.yaml"),
      _query(access, reference, "component-descriptor.json"),
      _query(access, reference, "component-knowledge.json"),
      _query(access, reference, "car-runtime-manifest.json"),
      VersionAvailabilityState.WORKING,
      LocalInspectionPolicy.DEFAULT,
      _clock
    )
  }

  private def _inventory(car: Vector[Byte]): LocalInformationInventory = {
    val reference = ResourceTreeReference.parseC("local-car").TAKE
    val entry = ResourceTreeEntry.createC("textus-order/1.2.0/textus-order-1.2.0.car", car).TAKE
    val snapshot = ResourceTreeAccess.inMemory(Map(reference -> Vector(entry))).snapshot(reference, ResourceTreeLimits.default).TAKE
    LocalInformationSourceInventory.inspectCarStorageSnapshot(
      InformationSourceDescriptor("local-car", InformationSourceKind.CAR_STORAGE, "resource-tree:local-car", 1, true, InformationSourceAuthorization.EXPLICIT),
      snapshot,
      VersionAvailabilityState.LOCAL_PUBLISHED,
      LocalInspectionPolicy.DEFAULT,
      _clock
    )
  }

  private def _query(
    access: ResourceTreeAccess,
    reference: ResourceTreeReference,
    leaf: String
  ): org.goldenport.cncf.resource.ResourceTreeQueryResult =
    access.query(ResourceTreeQuery.exactLeafNameC(reference, leaf).TAKE).TAKE

  private def _descriptor(carrier: Option[ComponentKnowledgeCarrier]): String = {
    val declaration = carrier.map(value => ",\"componentKnowledge\":" + ComponentKnowledgeCarrierCodec.encode(value)).getOrElse("")
    s"""{"schemaVersion":3,"component":{"namespace":"org.example","id":"Order","version":"$_release"},"extensions":{"artifact":"textus-order"}$declaration}"""
  }

  private def _car(entries: Vector[(String, Vector[Byte])]): Vector[Byte] = {
    val output = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(output)
    try entries.foreach { case (path, bytes) =>
      zip.putNextEntry(new ZipEntry(path))
      zip.write(bytes.toArray)
      zip.closeEntry()
    } finally zip.close()
    output.toByteArray.toVector
  }

  private def _consumer_contract_bytes: Vector[Byte] = {
    val text = s"""{
      |  "schema": "cncf.component-knowledge-consumer.v1",
      |  "componentId": "org.example.Order",
      |  "logicalRelease": "$_release",
      |  "resources": [{
      |    "logicalIdentity": {
      |      "componentId": "org.example.Order",
      |      "logicalRelease": "$_release",
      |      "parentComponentId": null,
      |      "childRole": "Documentation",
      |      "logicalResource": "urn:cncf:resource:example:order"
      |    },
      |    "logicalPath": "documentation/order.md",
      |    "kind": "documentation",
      |    "role": "documentation",
      |    "language": "en",
      |    "mediaType": "text/markdown",
      |    "size": 1,
      |    "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      |    "metadata": {
      |      "authority": "component",
      |      "stability": "stable",
      |      "source": "component-declared",
      |      "license": "Apache-2.0",
      |      "disclosure": "metadata-only"
      |    },
      |    "availability": "available",
      |    "integrity": "verified",
      |    "authorization": "granted",
      |    "provenance": {
      |      "sourceKind": "expanded-car",
      |      "artifactCoordinate": "org.example:textus-order:$_release",
      |      "logicalSource": "component-car:example-order",
      |      "resolutionStep": "expanded-car:2",
      |      "externalDeploymentRequired": false,
      |      "matchingDigest": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
      |    }
      |  }]
      |}""".stripMargin
    ComponentKnowledgeManifestConsumerContractCodec.encode(
      ComponentKnowledgeManifestConsumerContractCodec.decodeC(text).toOption.get
    ).getBytes(StandardCharsets.UTF_8).toVector
  }

  private def _runtime_manifest: String =
    """{"schemaVersion":"cncf.car-development-runtime-manifest.v2","evidence":[{"path":"target/cncf.d/component-knowledge.json"}]}"""

}
