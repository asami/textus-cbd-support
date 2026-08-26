package org.simplemodeling.textus.cbdsupport.runtime

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
final class ComponentKnowledgeLocalSourceSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _clock = Clock.fixed(Instant.parse("2026-08-26T00:00:00Z"), ZoneOffset.UTC)
  private val _component_id = ComponentId("org.example.Order")
  private val _release = "1.2.0"

  "LocalInformationSourceInventory" should {
    "admit descriptor-declared development evidence only when its runtime manifest records the same carrier digest" in {
      Given("a prepared development root with a generated descriptor, fixed carrier path, and matching runtime evidence")
      val contract = _consumer_contract_bytes
      val carrier = ComponentKnowledgeCarrier.createC(_sha256(contract)).toOption.get
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
        ResourceTreeEntry.createC("target/cncf.d/car-runtime-manifest.json", _runtime_manifest(contract).getBytes(StandardCharsets.UTF_8).toVector).TAKE
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
      observation.componentKnowledge match {
        case ComponentKnowledgeIntegration.Admitted(value, carrier) =>
          value.componentId shouldBe _component_id
          value.logicalRelease shouldBe _release
          carrier.sha256 shouldBe _sha256(contract)
        case other => fail(s"expected admitted development Component knowledge, got $other")
      }
    }

    "reject declared development knowledge when no generated runtime evidence names the carrier" in {
      Given("a prepared development root with a descriptor declaration but no runtime manifest leaf")
      val contract = _consumer_contract_bytes
      val carrier = ComponentKnowledgeCarrier.createC(_sha256(contract)).toOption.get
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

    "admit only the descriptor-declared CAR knowledge carrier as value-only evidence" in {
      Given("a canonical descriptor and a carrier whose digest identifies its one consumer-contract entry")
      val contract = _consumer_contract_bytes
      val carrier = ComponentKnowledgeCarrier.createC(_sha256(contract)).toOption.get
      val inventory = _inventory(_car(Vector(
        "component-descriptor.json" -> _descriptor(Some(carrier)).getBytes(StandardCharsets.UTF_8).toVector,
        "component-knowledge.json" -> contract
      )))

      When("the admitted CAR snapshot is inspected")
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
          carrier.sha256 shouldBe _sha256(contract)
        case other => fail(s"expected admitted Component knowledge, got $other")
      }
    }

    "not discover an archive carrier that the descriptor does not declare" in {
      Given("a canonical descriptor with no componentKnowledge declaration and an arbitrary matching-path archive entry")
      val inventory = _inventory(_car(Vector(
        "component-descriptor.json" -> _descriptor(None).getBytes(StandardCharsets.UTF_8).toVector,
        "component-knowledge.json" -> "not a consumer contract".getBytes(StandardCharsets.UTF_8).toVector
      )))

      When("the CAR is inspected")
      val observation = inventory.observations.head

      Then("the entry remains absent rather than becoming an ambient discovery input")
      observation.componentKnowledge shouldBe ComponentKnowledgeIntegration.Absent
      observation.diagnostics should not contain "Component knowledge carrier was rejected"
    }

    "retain generic CAR observation while rejecting a declared carrier whose archive entry is not intact" in {
      Given("a canonical descriptor that declares a digest different from the named archive entry")
      val carrier = ComponentKnowledgeCarrier.createC("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef").toOption.get
      val inventory = _inventory(_car(Vector(
        "component-descriptor.json" -> _descriptor(Some(carrier)).getBytes(StandardCharsets.UTF_8).toVector,
        "component-knowledge.json" -> _consumer_contract_bytes
      )))

      When("the CAR is inspected")
      val observation = inventory.observations.head

      Then("the generic coordinate is retained but no consumer content is admitted")
      observation.componentName shouldBe Some("Order")
      observation.componentKnowledge shouldBe a[ComponentKnowledgeIntegration.Rejected]
      observation.diagnostics.mkString(" ") should include("Component knowledge carrier was rejected")
    }
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

  private def _runtime_manifest(carrier: Vector[Byte]): String =
    s"""{"schemaVersion":"cncf.car-development-runtime-manifest.v2","evidence":[{"path":"target/cncf.d/component-knowledge.json","sha256":"${_sha256(carrier)}"}]}"""

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(byte => f"${byte & 0xff}%02x").mkString
}
