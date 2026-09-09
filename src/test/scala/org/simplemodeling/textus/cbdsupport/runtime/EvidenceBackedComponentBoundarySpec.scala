package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Clock, Instant, ZoneOffset}

import org.goldenport.Consequence
import org.goldenport.cncf.knowledge.ComponentKnowledgeCarrier
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class EvidenceBackedComponentBoundarySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _clock = Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC)
  private val _release = "1.2.0"

  "Evidence-backed component boundaries" should {
    "keep semantic candidates attributable and exact selection explicitly absent" in {
      Given("a catalog-owned Component term and an independent current-query semantic candidate")
      val source = CatalogSource("catalog", URI.create("https://catalog.example/"), 100, true)
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(_semantic_profile), clock = _clock),
        _clock
      )
      runtime.ensureReady(EmptyFetcher).isSuccess shouldBe true

      When("the semantic candidate is searched and its phrase is submitted for exact Component selection")
      val result = runtime.searchSourceAware(_source_query("run environment"), Vector(_sie_snapshot))
      val selection = runtime.selectComponent("run environment", None, None, None, None)

      Then("catalog and semantic evidence remain separate while the nonmatching exact selection stays absent")
      result.matches.map(_.matchKind) shouldBe Vector("semantic")
      result.matches.map(_.profile.catalogId) shouldBe Vector(source.id)
      result.matches.flatMap(_.semanticEvidenceIds) shouldBe result.semanticEvidence.map(_.id)
      result.report.observations.map(_.sourceKind) shouldBe Vector(InformationSourceKind.PUBLISHED_CATALOG)
      result.semanticEvidence.map(_.sourceKind) shouldBe Vector(InformationSourceKind.SIE_BOK)
      result.matches.head.profile.summary shouldBe None
      selection.status shouldBe "no-match"
      selection.selectedProfile shouldBe None
      selection.alternatives shouldBe empty
      selection.absences.map(_.code) shouldBe Vector(ExactComponentSelection.COMPONENT_NOT_FOUND)
    }

    "project an exact selected Component with only authorized knowledge references" in {
      Given("a catalog profile whose selected version admits one usable and one withheld knowledge resource")
      val source = CatalogSource("catalog", URI.create("https://catalog.example/"), 100, true)
      val endpoint = URI.create("https://catalog.example/repository/car/Order/1.2.0/component-knowledge.json")
      val carrier = ComponentKnowledgeCarrier.createC(
        _sha256(_catalog_contract.getBytes(StandardCharsets.UTF_8).toVector)
      ).toOption.getOrElse(fail("expected a valid Component knowledge carrier"))
      val profile = _catalog_profile(source, endpoint, carrier)
      val runtime = CbdRuntime.create(
        Vector(source),
        new InMemoryComponentCatalogProvider(Vector(profile), clock = _clock),
        _clock
      )
      val invocation = runtime.invocation(
        LocalInformationInventory(Vector.empty, Vector.empty, Vector.empty, _clock.instant(), Map.empty)
      )
      val fetcher = new ContractFetcher(endpoint, _catalog_contract)
      invocation.ensureInputsReady(fetcher).isSuccess shouldBe true

      When("the exact catalog Component is selected, admitted, and projected for usage")
      val selection = invocation.selectComponent("Order", Some("org.example"), Some("car"), Some(_release), Some(source.id))
      val selected = selection.selectedProfile.getOrElse(fail("expected the exact catalog profile"))
      val admission = invocation.ensureComponentKnowledge(selected, fetcher)
      val detail = invocation.componentKnowledge(selected).getOrElse(fail("expected admitted Component knowledge detail"))
      val usage = _value(invocation.usage(selected, Some("documentation"), fetcher))

      Then("the selected identity is preserved, only the granted logical reference is visible, and withholding is explicit")
      selection.status shouldBe "matched"
      selected.catalogId shouldBe source.id
      selected.selectedComponent shouldBe Some("org.example.Order")
      admission shouldBe a[ComponentKnowledgeIntegration.Admitted]
      detail.componentId shouldBe "org.example.Order"
      detail.logicalRelease shouldBe _release
      usage.profile.catalogId shouldBe source.id
      usage.selectedSourceId shouldBe Some(source.id)
      usage.selectedVersion shouldBe Some(_release)
      usage.operations shouldBe empty
      usage.references.map(_._2.toString) shouldBe Vector("urn:cncf:resource:example:order-documentation")
      usage.references.map(_._3) shouldBe Vector(true)
      usage.absences.map(_.code) shouldBe Vector("component-knowledge-reference-withheld")
    }
  }

  private val _semantic_profile = ComponentProfile(
    "catalog",
    Some("org.textus"),
    "textus-executor",
    "Textus Executor",
    None,
    "car",
    Vector("1.0.0"),
    Some("1.0.0"),
    None,
    Some("1.0.0"),
    None,
    Some("0.5.1"),
    Vector.empty,
    Vector("Execution Runtime"),
    Vector.empty,
    None,
    URI.create("https://catalog.example/index.json"),
    None,
    None,
    Vector(ComponentVersionEvidence("1.0.0", Some("0.5.1"), Vector.empty, None, None, false)),
    Vector.empty
  )

  private def _source_query(requirement: String): SourceAwareComponentSearchQuery =
    SourceAwareComponentSearchQuery(
      requirement,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some(ReconciliationPurpose.PUBLISHED_REUSE),
      10
    )

  private val _sie_snapshot = SieBokSnapshot(
    InformationSourceDescriptor(
      "semantic",
      InformationSourceKind.SIE_BOK,
      "https://sie.example/mcp",
      700,
      true,
      InformationSourceAuthorization.COMPONENT_ROUTE_ALLOWLIST
    ),
    "matched",
    "run environment",
    Vector(SieBokTermEvidence(
      "semantic",
      "execution:runtime",
      "Execution Runtime",
      "Runtime definition from SIE.",
      Some("architecture"),
      "term",
      "bok-main",
      "semantic",
      0.9,
      "SIE matched the runtime intent.",
      URI.create("https://sie.example/terms/runtime")
    )),
    _clock.instant(),
    Vector.empty
  )

  private val _catalog_contract = s"""{
       |  "schema": "cncf.component-knowledge-consumer.v1",
       |  "componentId": "org.example.Order",
       |  "logicalRelease": "$_release",
       |  "resources": [
       |    {
       |      "logicalIdentity": {"componentId": "org.example.Order", "logicalRelease": "$_release", "parentComponentId": null, "childRole": "Documentation", "logicalResource": "urn:cncf:resource:example:order-documentation"},
       |      "logicalPath": "documentation/order.md",
       |      "kind": "documentation",
       |      "role": "documentation",
       |      "language": "en",
       |      "mediaType": "text/markdown",
       |      "size": 11,
       |      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
       |      "metadata": {"authority": "component", "stability": "stable", "source": "component-declared", "license": "Apache-2.0", "disclosure": "metadata-only"},
       |      "availability": "available",
       |      "integrity": "verified",
       |      "authorization": "granted",
       |      "provenance": {"sourceKind": "expanded-car", "artifactCoordinate": "org.example:order:$_release", "logicalSource": "component-car:example-order", "resolutionStep": "expanded-car:2", "externalDeploymentRequired": false, "matchingDigest": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"}
       |    },
       |    {
       |      "logicalIdentity": {"componentId": "org.example.Order", "logicalRelease": "$_release", "parentComponentId": null, "childRole": "Source", "logicalResource": "urn:cncf:resource:example:order-source"},
       |      "logicalPath": "source/order.scala",
       |      "kind": "source-code",
       |      "role": "source-code",
       |      "language": "scala",
       |      "mediaType": "text/x-scala",
       |      "size": 17,
       |      "sha256": "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210",
       |      "metadata": {"authority": "component", "stability": "stable", "source": "component-declared", "license": "LicenseRef-Internal", "disclosure": "reference-only"},
       |      "availability": "restricted",
       |      "integrity": "verified",
       |      "authorization": "denied",
       |      "provenance": {"sourceKind": "expanded-car", "artifactCoordinate": "org.example:order:$_release", "logicalSource": "component-car:example-order", "resolutionStep": "expanded-car:2", "externalDeploymentRequired": false, "matchingDigest": "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"}
       |    }
       |  ]
       |}""".stripMargin

  private def _catalog_profile(
    source: CatalogSource,
    endpoint: URI,
    carrier: ComponentKnowledgeCarrier
  ): ComponentProfile =
    ComponentProfile(
      source.id,
      Some("org.example"),
      "Order",
      "Order",
      Some("Order component."),
      "car",
      Vector(_release),
      Some(_release),
      None,
      Some(_release),
      None,
      None,
      Vector.empty,
      Vector.empty,
      Vector.empty,
      Some(URI.create("https://catalog.example/repository/car/Order/1.2.0/order.car")),
      URI.create("https://catalog.example/metadata/repository/car/index.json"),
      None,
      None,
      Vector(ComponentVersionEvidence(
        _release,
        None,
        Vector.empty,
        Some(URI.create("https://catalog.example/repository/car/Order/1.2.0/order.car")),
        None,
        false,
        component = Some("org.example.Order"),
        componentKnowledge = Some(ComponentKnowledgeCatalogEvidence(carrier, endpoint))
      )),
      Vector.empty
    )

  private def _value[A](value: Consequence[A]): A = value match {
    case Consequence.Success(result) => result
    case Consequence.Failure(conclusion) => fail(conclusion.display)
  }

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(byte => f"${byte & 0xff}%02x").mkString

  private final class ContractFetcher(endpoint: URI, contract: String) extends CatalogFetcher with BokFetcher {
    def get(uri: URI): Consequence[String] =
      if (uri == endpoint) Consequence.success(contract)
      else Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
  }

  private object EmptyFetcher extends CatalogFetcher with BokFetcher {
    def get(uri: URI): Consequence[String] = Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
  }
}
