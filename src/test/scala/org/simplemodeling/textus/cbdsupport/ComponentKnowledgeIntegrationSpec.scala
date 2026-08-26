package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.knowledge.{
  ComponentKnowledgeCarrier,
  ComponentKnowledgeManifestConsumerContractCodec
}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.ComponentKnowledgeIntegration

/**
 * DOC-06 failing-first specification for the protected Component knowledge
 * carrier admission boundary.
 *
 * The admitted value is deliberately the frozen value-only consumer contract;
 * raw consumer-contract bytes are input evidence and are not projected.
 */
final class ComponentKnowledgeIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _component_id = ComponentId("org.example.fixture.ComponentKnowledgeIntegration")
  private val _release = "0.1.0-SNAPSHOT"
  private val _other_component_id = ComponentId("org.example.fixture.OtherComponent")
  private val _other_release = "0.2.0-SNAPSHOT"

  private val _consumer_contract_fixture =
    """{
      |  "schema": "cncf.component-knowledge-consumer.v1",
      |  "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |  "logicalRelease": "0.1.0-SNAPSHOT",
      |  "resources": [
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |        "logicalRelease": "0.1.0-SNAPSHOT",
      |        "parentComponentId": null,
      |        "childRole": "Documentation",
      |        "logicalResource": "urn:cncf:resource:fixture:component-knowledge-integration"
      |      },
      |      "logicalPath": "documentation/fixture.md",
      |      "kind": "documentation",
      |      "role": "documentation",
      |      "language": "en",
      |      "mediaType": "text/markdown",
      |      "size": 1,
      |      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      |      "metadata": {
      |        "authority": "component",
      |        "stability": "stable",
      |        "source": "component-declared",
      |        "license": "Apache-2.0",
      |        "disclosure": "metadata-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "granted",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example.fixture:component-knowledge-integration:0.1.0-SNAPSHOT",
      |        "logicalSource": "component-car:fixture",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
      |      }
      |    }
      |  ]
      |}""".stripMargin

  private val _consumer_contract =
    ComponentKnowledgeManifestConsumerContractCodec.decodeC(_consumer_contract_fixture).toOption.get
  private val _consumer_contract_bytes =
    ComponentKnowledgeManifestConsumerContractCodec.encode(_consumer_contract).getBytes(StandardCharsets.UTF_8).toVector
  private val _carrier_digest = _sha256(_consumer_contract_bytes)
  private val _carrier = ComponentKnowledgeCarrier.createC(_carrier_digest).toOption.get

  "DOC06 Component knowledge integration" should {
    "admit matching declared carrier and contract into value-only evidence" in {
      Given("a canonical component-knowledge carrier and its canonical raw consumer-contract bytes")

      When("CBD admits the carrier-backed contract for the exact expected Component and release")
      val result = ComponentKnowledgeIntegration.admit(
        ComponentKnowledgeIntegration.Input(
          carrier = Some(_carrier),
          consumerContractBytes = _consumer_contract_bytes,
          expectedComponentId = _component_id,
          expectedLogicalRelease = _release
        )
      )

      Then("the matching contract is admitted as frozen value-only evidence without projecting raw bytes")
      result match {
        case ComponentKnowledgeIntegration.Admitted(value, carrier) =>
          value shouldBe _consumer_contract
          value.componentId shouldBe _component_id
          value.logicalRelease shouldBe _release
          carrier shouldBe _carrier
        case other => fail(s"expected admitted value-only evidence, got $other")
      }
    }

    "make an absent carrier explicit without falling back to a scan" in {
      Given("valid consumer-contract bytes but no declared Component knowledge carrier")

      When("CBD attempts admission with no carrier declaration")
      val result = ComponentKnowledgeIntegration.admit(
        ComponentKnowledgeIntegration.Input(
          carrier = None,
          consumerContractBytes = _consumer_contract_bytes,
          expectedComponentId = _component_id,
          expectedLogicalRelease = _release
        )
      )

      Then("the result is explicit absence rather than a fallback discovery")
      result shouldBe ComponentKnowledgeIntegration.Absent
    }

    "reject path and digest mismatches before projecting consumer bytes" in {
      Given("canonical consumer-contract bytes paired with a carrier path or digest mismatch")
      val mismatched_carriers = Vector(
        _carrier.copy(logicalPath = "component-knowledge-alias.json"),
        _carrier.copy(sha256 = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210")
      )

      When("CBD evaluates each mismatched declaration")
      val results = mismatched_carriers.map { carrier =>
        ComponentKnowledgeIntegration.admit(
          ComponentKnowledgeIntegration.Input(
            carrier = Some(carrier),
            consumerContractBytes = _consumer_contract_bytes,
            expectedComponentId = _component_id,
            expectedLogicalRelease = _release
          )
        )
      }

      Then("every path or digest mismatch is rejected and no raw consumer bytes are admitted")
      results.foreach(_ shouldBe a[ComponentKnowledgeIntegration.Rejected])
    }

    "reject a consumer component or release identity mismatch" in {
      Given("a matching carrier and contract with an incorrect expected Component or release identity")
      val mismatched_expectations = Vector(
        ComponentKnowledgeIntegration.Input(Some(_carrier), _consumer_contract_bytes, _other_component_id, _release),
        ComponentKnowledgeIntegration.Input(Some(_carrier), _consumer_contract_bytes, _component_id, _other_release)
      )

      When("CBD evaluates each exact-identity mismatch")
      val results = mismatched_expectations.map(ComponentKnowledgeIntegration.admit)

      Then("component and release mismatches are rejected rather than silently selecting another identity")
      results.foreach(_ shouldBe a[ComponentKnowledgeIntegration.Rejected])
    }
  }

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(byte => f"${byte & 0xff}%02x").mkString
}
