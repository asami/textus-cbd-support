package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.repository.{
  ComponentResourceAuthorization,
  ComponentResourceAvailability,
  ComponentResourceIntegrity
}
import org.goldenport.cncf.knowledge.{
  ComponentKnowledgeCarrier,
  ComponentKnowledgeManifestConsumerContractCodec
}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.ComponentKnowledgeIntegration

/*
 * Executable acceptance specification for Phase 59.8 / Step P598-S2.
 *
 * The CBD boundary admits only the canonical value-only consumer contract
 * behind its exact carrier digest. No server, catalog runtime, CAR archive,
 * source access, external I/O, or MCP authority participates in these cases.
 *
 * @since   Aug. 27, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class RepresentativeDocumentationProfileCarrierSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _component_id = ComponentId("org.example.RepresentativeDocumentationProfile")
  private val _release = "0.1.0"

  "P598-S2 representative documentation profile CBD carrier" should {
    "admit exact value-only framework, Directive, Skill Catalog, and source authorization metadata" in {
      Given("canonical encoded representative consumer-contract bytes and a carrier declaring their exact digest")

      When("CBD admits the carrier-backed contract for the exact Component and release")
      val result = ComponentKnowledgeIntegration.admit(
        ComponentKnowledgeIntegration.Input(
          carrier = Some(_carrier),
          consumerContractBytes = _consumer_contract_bytes,
          expectedComponentId = _component_id,
          expectedLogicalRelease = _release
        )
      )

      Then("the admitted value preserves identity, digest, descriptive metadata, and source authorization without authority-bearing projection")
      result match {
        case ComponentKnowledgeIntegration.Admitted(value, carrier) =>
          value.resources.map { resource =>
            resource.logicalIdentity.logicalResource -> (
              resource.logicalPath,
              resource.kind.code,
              resource.role.code,
              resource.authorization,
              resource.sha256
            )
          }.toMap shouldBe _consumer_contract.resources.map { resource =>
            resource.logicalIdentity.logicalResource -> (
              resource.logicalPath,
              resource.kind.code,
              resource.role.code,
              resource.authorization,
              resource.sha256
            )
          }.toMap
          carrier shouldBe _carrier
          value.componentId shouldBe _component_id
          value.logicalRelease shouldBe _release
          value.frameworkPublication.map(_.product) shouldBe Some("simplemodeling")
          value.frameworkPublication.map(_.version) shouldBe Some("0.1.0")
          value.frameworkPublication.map(_.canonicalUrl) shouldBe Some("https://simplemodeling.org/framework/0.1.0")
          value.frameworkPublication.map(_.documentId) shouldBe Some("urn:cncf:framework:document:simplemodeling:0.1.0")
          value.frameworkPublication.flatMap(_.sectionId) shouldBe Some("urn:cncf:framework:section:representative")
          value.frameworkPublication.map(_.availability.code) shouldBe Some("online")
          value.frameworkPublication.map(_.sha256) shouldBe Some(_sha256("framework:publication:simplemodeling:0.1.0"))
          value.frameworkPublication.map(_.sourceSha256) shouldBe Some(_sha256("framework:source:simplemodeling:0.1.0"))
          value.publicDirective.map(_.logicalIdentity.logicalResource) shouldBe Some("urn:cncf:resource:representative:directive")
          value.publicDirective.map(_.directiveId) shouldBe Some("mounted-directive")
          value.publicDirective.map(_.profileId) shouldBe Some("public-framework")
          value.publicDirective.map(_.ruleId) shouldBe Some("public-framework-rule")
          value.publicDirective.map(_.authority.code) shouldBe Some("mounted-directive-remains-authoritative")
          value.publicDirective.map(_.visibility.code) shouldBe Some("public")
          value.publicDirective.map(_.sourceSha256) shouldBe Some(_sha256("directive:public:simplemodeling"))
          value.skillCatalog.map(_.logicalIdentity.logicalResource) shouldBe Some("urn:cncf:resource:representative:skill-catalog")
          value.skillCatalog.map(_.catalogId) shouldBe Some("public-skill-catalog")
          value.skillCatalog.map(_.owner) shouldBe Some("simplemodeling")
          value.skillCatalog.map(_.purpose) shouldBe Some("descriptive public Skill Catalog metadata")
          value.skillCatalog.map(_.visibility.code) shouldBe Some("ecosystem")
          value.skillCatalog.map(_.version) shouldBe Some("1.0.0")
          value.skillCatalog.map(_.sideEffects) shouldBe Some(Vector("none"))
          value.skillCatalog.map(_.mcpRequirements) shouldBe Some(Vector("descriptive only"))
          value.skillCatalog.map(_.installationReference) shouldBe Some("https://simplemodeling.org/skills/public")
          value.skillCatalog.map(_.sourceSha256) shouldBe Some(_sha256("skill:catalog:simplemodeling"))
          val source = value.resources.find(_.kind.code == "source-code").getOrElse(fail("SourceCode resource"))
          source.logicalIdentity.logicalResource shouldBe "urn:cncf:resource:representative:source-code"
          source.availability shouldBe ComponentResourceAvailability.Available
          source.integrity shouldBe ComponentResourceIntegrity.Verified
          source.authorization shouldBe ComponentResourceAuthorization.Denied
          source.sha256 shouldBe _sha256("source:representative:withheld")
          source.provenance.matchingDigest shouldBe source.sha256
          value.extensions shouldBe empty
          value.resources.foreach { resource =>
            resource.extensions shouldBe empty
            resource.metadata.extensions shouldBe empty
            resource.provenance.extensions shouldBe empty
          }
          carrier.carrierSchema shouldBe "cncf.component-knowledge-carrier.v1"
          carrier.consumerContractSchema shouldBe "cncf.component-knowledge-consumer.v1"
          carrier.logicalPath shouldBe "component-knowledge.json"
          carrier.sha256 shouldBe _carrier_digest
        case other => fail(s"expected admitted value-only evidence, got $other")
      }
    }

    "reject an exact carrier digest mismatch before admitting any contract value" in {
      Given("canonical representative consumer-contract bytes paired with a carrier declaring a different digest")
      val mismatchedcarrier = _carrier.copy(
        sha256 = "0000000000000000000000000000000000000000000000000000000000000000"
      )

      When("CBD evaluates the mismatched carrier declaration")
      val result = ComponentKnowledgeIntegration.admit(
        ComponentKnowledgeIntegration.Input(
          carrier = Some(mismatchedcarrier),
          consumerContractBytes = _consumer_contract_bytes,
          expectedComponentId = _component_id,
          expectedLogicalRelease = _release
        )
      )

      Then("the digest boundary rejects the contract without a value-level admission")
      result shouldBe a[ComponentKnowledgeIntegration.Rejected]
    }
  }

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest.getInstance("SHA-256")
      .digest(bytes.toArray)
      .map(byte => String.format("%02x", Integer.valueOf(byte & 0xff)))
      .mkString

  private def _sha256(value: String): String =
    _sha256(value.getBytes(StandardCharsets.UTF_8).toVector)

  private lazy val _consumer_contract =
    ComponentKnowledgeManifestConsumerContractCodec.decodeC(_consumer_contract_fixture).TAKE

  private lazy val _consumer_contract_bytes =
    ComponentKnowledgeManifestConsumerContractCodec
      .encode(_consumer_contract)
      .getBytes(StandardCharsets.UTF_8)
      .toVector

  private lazy val _carrier_digest = _sha256(_consumer_contract_bytes)
  private lazy val _carrier = ComponentKnowledgeCarrier.createC(_carrier_digest).toOption.getOrElse(fail("carrier"))

  private lazy val _consumer_contract_fixture =
    """{
      |  "schema": "cncf.component-knowledge-consumer.v1",
      |  "componentId": "org.example.RepresentativeDocumentationProfile",
      |  "logicalRelease": "0.1.0",
      |  "frameworkPublication": {
      |    "product": "simplemodeling",
      |    "version": "0.1.0",
      |    "canonicalUrl": "https://simplemodeling.org/framework/0.1.0",
      |    "publicationGeneration": "2026-08-27",
      |    "documentId": "urn:cncf:framework:document:simplemodeling:0.1.0",
      |    "sectionId": "urn:cncf:framework:section:representative",
      |    "sha256": "__FRAMEWORK_PUBLICATION_DIGEST__",
      |    "availability": "online",
      |    "sourceIdentity": "urn:cncf:framework:source:simplemodeling:0.1.0",
      |    "sourceSha256": "__FRAMEWORK_SOURCE_DIGEST__"
      |  },
      |  "publicDirective": {
      |    "logicalIdentity": {
      |      "componentId": "org.example.RepresentativeDocumentationProfile",
      |      "logicalRelease": "0.1.0",
      |      "parentComponentId": null,
      |      "childRole": "Directive",
      |      "logicalResource": "urn:cncf:resource:representative:directive"
      |    },
      |    "directiveId": "mounted-directive",
      |    "profileId": "public-framework",
      |    "ruleId": "public-framework-rule",
      |    "origin": "urn:cncf:directive:simplemodeling:public",
      |    "version": "1.0.0",
      |    "authority": "mounted-directive-remains-authoritative",
      |    "visibility": "public",
      |    "sourceSha256": "__DIRECTIVE_DIGEST__",
      |    "redaction": "source-and-rule-content-withheld",
      |    "guideReference": "https://simplemodeling.org/directive/public"
      |  },
      |  "skillCatalog": {
      |    "logicalIdentity": {
      |      "componentId": "org.example.RepresentativeDocumentationProfile",
      |      "logicalRelease": "0.1.0",
      |      "parentComponentId": null,
      |      "childRole": "SkillCatalog",
      |      "logicalResource": "urn:cncf:resource:representative:skill-catalog"
      |    },
      |    "catalogId": "public-skill-catalog",
      |    "owner": "simplemodeling",
      |    "purpose": "descriptive public Skill Catalog metadata",
      |    "trigger": "explicit user request",
      |    "requirements": ["component knowledge manifest"],
      |    "permissions": ["metadata visibility"],
      |    "sideEffects": ["none"],
      |    "mcpRequirements": ["descriptive only"],
      |    "installationReference": "https://simplemodeling.org/skills/public",
      |    "visibility": "ecosystem",
      |    "version": "1.0.0",
      |    "sourceSha256": "__SKILL_CATALOG_DIGEST__"
      |  },
      |  "resources": [
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "FrameworkDocumentation",
      |        "logicalResource": "urn:cncf:resource:representative:framework"
      |      },
      |      "logicalPath": "framework/simplemodeling-0.1.0.md",
      |      "kind": "framework-documentation",
      |      "role": "framework-documentation",
      |      "language": "en",
      |      "mediaType": "text/markdown",
      |      "size": 1,
      |      "sha256": "__FRAMEWORK_SOURCE_DIGEST__",
      |      "metadata": {
      |        "authority": "framework",
      |        "stability": "stable",
      |        "source": "supplied-phase58",
      |        "license": "Apache-2.0",
      |        "disclosure": "metadata-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "granted",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__FRAMEWORK_SOURCE_DIGEST__"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "Directive",
      |        "logicalResource": "urn:cncf:resource:representative:directive"
      |      },
      |      "logicalPath": "directive/public.yaml",
      |      "kind": "directive",
      |      "role": "directive",
      |      "language": "en",
      |      "mediaType": "application/yaml",
      |      "size": 1,
      |      "sha256": "__DIRECTIVE_DIGEST__",
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
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__DIRECTIVE_DIGEST__"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "SkillCatalog",
      |        "logicalResource": "urn:cncf:resource:representative:skill-catalog"
      |      },
      |      "logicalPath": "skills/catalog.json",
      |      "kind": "skill-catalog",
      |      "role": "skill-catalog",
      |      "language": "en",
      |      "mediaType": "application/json",
      |      "size": 1,
      |      "sha256": "__SKILL_CATALOG_DIGEST__",
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
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__SKILL_CATALOG_DIGEST__"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "SourceCode",
      |        "logicalResource": "urn:cncf:resource:representative:source-code"
      |      },
      |      "logicalPath": "source/RepresentativeDocumentationProfile.scala",
      |      "kind": "source-code",
      |      "role": "source-code",
      |      "language": "scala",
      |      "mediaType": "text/x-scala",
      |      "size": 1,
      |      "sha256": "__SOURCE_DIGEST__",
      |      "metadata": {
      |        "authority": "component",
      |        "stability": "stable",
      |        "source": "component-declared",
      |        "license": "LicenseRef-Internal",
      |        "disclosure": "reference-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "denied",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__SOURCE_DIGEST__"
      |      }
      |    }
      |  ]
      |}""".stripMargin
      .replace("__FRAMEWORK_PUBLICATION_DIGEST__", _sha256("framework:publication:simplemodeling:0.1.0"))
      .replace("__FRAMEWORK_SOURCE_DIGEST__", _sha256("framework:source:simplemodeling:0.1.0"))
      .replace("__DIRECTIVE_DIGEST__", _sha256("directive:public:simplemodeling"))
      .replace("__SKILL_CATALOG_DIGEST__", _sha256("skill:catalog:simplemodeling"))
      .replace("__SOURCE_DIGEST__", _sha256("source:representative:withheld"))
}
