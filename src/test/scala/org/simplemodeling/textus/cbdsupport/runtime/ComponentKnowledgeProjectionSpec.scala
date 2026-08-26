package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Clock, Instant, ZoneOffset}

import org.goldenport.Consequence
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.knowledge.{ComponentKnowledgeCarrier, ComponentKnowledgeManifestConsumerContractCodec}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * DOC-06 specification for bounded CBD detail and usage projection.
 *
 * An admitted carrier contributes typed metadata only.  It is never a
 * resource reader, operation registry, or authority grant.
 */
final class ComponentKnowledgeProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _clock = Clock.fixed(Instant.parse("2026-08-26T00:00:00Z"), ZoneOffset.UTC)
  private val _component_id = ComponentId("org.example.Order")
  private val _release = "1.2.0"

  "CbdRuntimeInvocation Component knowledge projection" should {
    "select an admitted exact CAR profile and expose only declared metadata" in {
      Given("a local invocation inventory carrying an admitted two-resource contract")
      val invocation = _runtime.invocation(_inventory(ComponentKnowledgeIntegration.Admitted(_contract, _carrier)))
      invocation.ensureInputsReady(EmptyFetcher).isSuccess shouldBe true

      When("CBD selects the exact local component and projects its detail")
      val selection = invocation.selectComponent("Order", Some("org.example"), Some("car"), Some(_release), Some("local-car"))
      val profile = selection.selectedProfile.getOrElse(fail("expected exact local profile"))
      val detail = invocation.componentKnowledge(profile).getOrElse(fail("expected admitted Component knowledge"))
      val usage = _value(invocation.usage(profile, Some("documentation"), EmptyFetcher))

      Then("the response retains the declared identity, integrity and provenance values without content or authority")
      selection.status shouldBe "matched"
      profile.selectedComponent shouldBe Some(_component_id.name)
      detail.componentId shouldBe _component_id.name
      detail.logicalRelease shouldBe _release
      detail.carrierSchema shouldBe "cncf.component-knowledge-carrier.v1"
      detail.carrierLogicalPath shouldBe "component-knowledge.json"
      detail.carrierSha256 shouldBe _carrier_digest
      detail.resources.map(_.logicalPath) shouldBe Vector("documentation/order.md", "source/order.scala")
      detail.resources.map(_.license) shouldBe Vector("Apache-2.0", "LicenseRef-Internal")
      detail.resources.map(_.disclosure) shouldBe Vector("metadata-only", "reference-only")
      detail.resources.map(_.authorization) shouldBe Vector("granted", "denied")
      detail.resources.map(_.resolutionStep) shouldBe Vector("expanded-car:2", "expanded-car:2")
      detail.truncatedResourceCount shouldBe 0
      invocation.componentKnowledgeCarReviewProvider(
        profile,
        ReviewProviderIdentity(ReviewProviderId("cbd-component-knowledge"), ReviewVersion("1.0.0")),
        ReviewRuleIdentity(ReviewRuleId("cbd.component-knowledge.car-review"), ReviewVersion("1.0.0"))
      ).map(_.sourceDigest) shouldBe Some("sha256:" + _carrier_digest)
      usage.operations shouldBe empty
      usage.references.map(_._2.toString) shouldBe Vector("urn:cncf:resource:example:order-documentation")
      usage.guidance.map(_.statementKind) shouldBe Vector("declared-resource")
      usage.absences.map(_.code) shouldBe Vector("component-knowledge-reference-withheld")
      usage.warnings.mkString(" ") should include("does not define executable operations")
    }

    "report a declared-carrier absence instead of scanning unadmitted local data" in {
      Given("a local generic CAR observation without a Component knowledge declaration")
      val invocation = _runtime.invocation(_inventory(ComponentKnowledgeIntegration.Absent))
      invocation.ensureInputsReady(EmptyFetcher).isSuccess shouldBe true

      When("the exact local component is requested")
      val selection = invocation.selectComponent("Order", Some("org.example"), Some("car"), Some(_release), Some("local-car"))

      Then("the normal no-match remains explicit and the carrier absence is attributable")
      selection.status shouldBe "no-match"
      selection.absences.map(_.code) should contain allOf (
        ExactComponentSelection.COMPONENT_NOT_FOUND,
        ComponentKnowledgeProjection.KNOWLEDGE_ABSENT
      )
    }

    "report rejection without treating the rejected carrier as usable metadata" in {
      Given("a local generic CAR observation whose declared carrier was rejected")
      val invocation = _runtime.invocation(_inventory(ComponentKnowledgeIntegration.Rejected("digest mismatch")))
      invocation.ensureInputsReady(EmptyFetcher).isSuccess shouldBe true

      When("the exact local component is requested")
      val selection = invocation.selectComponent("Order", Some("org.example"), Some("car"), Some(_release), Some("local-car"))

      Then("no profile is selected and the rejection remains a bounded diagnostic")
      selection.status shouldBe "no-match"
      selection.absences.map(_.code) should contain(ComponentKnowledgeProjection.KNOWLEDGE_REJECTED)
    }

    "bound a local metadata projection without reading additional resources" in {
      Given("an admitted contract whose declared resource count exceeds the public response limit")
      val base = _contract.resources.head
      val resources = Vector.tabulate(ComponentKnowledgeProjection.MAXIMUM_RESOURCES + 1) { index =>
        base.copy(
          logicalIdentity = base.logicalIdentity.copy(logicalResource = s"urn:cncf:resource:example:order-documentation-$index"),
          logicalPath = s"documentation/order-$index.md"
        )
      }

      When("CBD projects its value-only Component detail")
      val detail = ComponentKnowledgeProjection.fromObservation(
        _inventory(ComponentKnowledgeIntegration.Admitted(_contract.copy(resources = resources), _carrier)).observations.head
      ).map(_.detail).getOrElse(fail("expected admitted Component knowledge detail"))

      Then("the bounded response reports omission without opening omitted resources")
      detail.resources.size shouldBe ComponentKnowledgeProjection.MAXIMUM_RESOURCES
      detail.truncatedResourceCount shouldBe 1
      detail.resources.last.logicalPath shouldBe "documentation/order-99.md"
    }

    "admit only the selected catalog profile's declared version-scoped consumer-contract endpoint" in {
      Given("a catalog profile with one exact carrier declaration and one same-origin version-scoped endpoint")
      val source = CatalogSource("catalog", URI.create("https://catalog.example/"), 1, enabled = true)
      val carrier = ComponentKnowledgeCarrier.createC(_sha256(_catalog_contract.getBytes(StandardCharsets.UTF_8).toVector)).toOption.getOrElse(fail("carrier"))
      val endpoint = URI.create("https://catalog.example/repository/car/textus-order/1.2.0/component-knowledge.json")
      val profile = _catalog_profile(source, carrier, endpoint)
      val runtime = CbdRuntime.create(Vector(source), new InMemoryComponentCatalogProvider(Vector(profile), clock = _clock), _clock)
      val invocation = runtime.invocation(LocalInformationInventory(Vector.empty, Vector.empty, Vector.empty, _clock.instant(), Map.empty))
      val fetcher = new ContractFetcher(endpoint, _catalog_contract)
      invocation.ensureInputsReady(fetcher).isSuccess shouldBe true

      When("the exact profile is selected and Component knowledge is requested")
      val selected = invocation.selectComponent("Order", Some("org.example"), Some("car"), Some(_release), Some(source.id)).selectedProfile.getOrElse(fail("catalog profile"))
      val admission = invocation.ensureComponentKnowledge(selected, fetcher)
      val detail = invocation.componentKnowledge(selected).getOrElse(fail("catalog detail"))
      val usage = _value(invocation.usage(selected, Some("documentation"), fetcher))

      Then("CBD fetches only that declared URI and projects values without archive access")
      admission shouldBe a[ComponentKnowledgeIntegration.Admitted]
      invocation.componentKnowledgeAbsence(selected) shouldBe None
      fetcher.requests shouldBe Vector(endpoint)
      detail.sourceKind shouldBe InformationSourceKind.PUBLISHED_CATALOG
      detail.evidenceLocation shouldBe endpoint.toString
      detail.carrierSha256 shouldBe carrier.sha256
      usage.operations shouldBe empty
      usage.references.map(_._2.toString) shouldBe Vector("urn:cncf:resource:example:order-documentation")
    }

    "reject an off-origin catalog Component knowledge endpoint before it is fetched" in {
      Given("an otherwise valid profile whose declared endpoint changes origin")
      val source = CatalogSource("catalog", URI.create("https://catalog.example/"), 1, enabled = true)
      val carrier = ComponentKnowledgeCarrier.createC(_sha256(_catalog_contract.getBytes(StandardCharsets.UTF_8).toVector)).toOption.getOrElse(fail("carrier"))
      val profile = _catalog_profile(
        source,
        carrier,
        URI.create("https://other.example/repository/car/textus-order/1.2.0/component-knowledge.json")
      )
      val runtime = CbdRuntime.create(Vector(source), new InMemoryComponentCatalogProvider(Vector(profile), clock = _clock), _clock)
      val invocation = runtime.invocation(LocalInformationInventory(Vector.empty, Vector.empty, Vector.empty, _clock.instant(), Map.empty))
      val fetcher = new ContractFetcher(URI.create("https://catalog.example/unused"), _catalog_contract)
      invocation.ensureInputsReady(fetcher).isSuccess shouldBe true
      val selected = invocation.selectComponent("Order", Some("org.example"), Some("car"), Some(_release), Some(source.id)).selectedProfile.getOrElse(fail("catalog profile"))

      When("CBD attempts carrier admission")
      val admission = invocation.ensureComponentKnowledge(selected, fetcher)

      Then("the endpoint is rejected before any non-catalog request occurs")
      admission shouldBe a[ComponentKnowledgeIntegration.Rejected]
      invocation.componentKnowledgeAbsence(selected).map(_.code) shouldBe Some(
        ComponentKnowledgeProjection.KNOWLEDGE_REJECTED
      )
      fetcher.requests shouldBe empty
    }
  }

  private def _runtime: CbdRuntime =
    CbdRuntime.create(Vector.empty, new InMemoryComponentCatalogProvider(Vector.empty, clock = _clock), _clock)

  private def _inventory(knowledge: ComponentKnowledgeIntegration.Result): LocalInformationInventory =
    LocalInformationInventory(
      Vector(InformationSourceDescriptor(
        "local-car",
        InformationSourceKind.CAR_STORAGE,
        "resource-tree:local-car",
        1,
        true,
        InformationSourceAuthorization.EXPLICIT
      )),
      Vector(LocalComponentObservation(
        "local-car",
        "car-storage",
        Some("Order"),
        Some("org.example"),
        Some("car"),
        Some(_release),
        "descriptor",
        VersionAvailabilityState.LOCAL_PUBLISHED,
        "car:org.example:textus-order:1.2.0",
        Some("3"),
        Some(_release),
        Some("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
        Vector.empty,
        knowledge
      )),
      Vector.empty,
      _clock.instant(),
      Map.empty
    )

  private val _contract = ComponentKnowledgeManifestConsumerContractCodec.decodeC(
    s"""{
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
       |      "provenance": {"sourceKind": "expanded-car", "artifactCoordinate": "org.example:textus-order:$_release", "logicalSource": "component-car:example-order", "resolutionStep": "expanded-car:2", "externalDeploymentRequired": false, "matchingDigest": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"}
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
       |      "provenance": {"sourceKind": "expanded-car", "artifactCoordinate": "org.example:textus-order:$_release", "logicalSource": "component-car:example-order", "resolutionStep": "expanded-car:2", "externalDeploymentRequired": false, "matchingDigest": "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"}
       |    }
       |  ]
       |}""".stripMargin
  ).toOption.getOrElse(fail("Component knowledge projection fixture must satisfy the v1 consumer contract"))

  private val _carrier_digest = "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd"
  private val _carrier = ComponentKnowledgeCarrier.createC(_carrier_digest).toOption.getOrElse(
    fail("Component knowledge projection carrier fixture must satisfy the v1 declaration"))

  /**
   * Catalog transport is version-specific.  Deliberately place the carrier in
   * the selected version evidence rather than the profile-wide fallback so
   * selection cannot accidentally retain a carrier for another release.
   */
  private def _catalog_profile(
    source: CatalogSource,
    carrier: ComponentKnowledgeCarrier,
    endpoint: URI
  ): ComponentProfile = {
    val base = ComponentKnowledgeProjection.fromObservation(
      _inventory(ComponentKnowledgeIntegration.Admitted(_contract, carrier)).observations.head
    ).getOrElse(fail("local projection")).profile.copy(
      catalogId = source.id,
      evidenceUri = URI.create("https://catalog.example/metadata/repository/car/index.json"),
      artifactUri = Some(URI.create("https://catalog.example/repository/car/textus-order/1.2.0/textus-order-1.2.0.car")),
      componentKnowledge = None
    )
    base.copy(
      versionEvidence = Vector(ComponentVersionEvidence(
        version = _release,
        runtimeMinimum = None,
        dependencies = Vector.empty,
        artifactUri = base.artifactUri,
        modelMetadataUri = None,
        hasDependencyMetadata = false,
        component = Some(_component_id.name),
        componentKnowledge = Some(ComponentKnowledgeCatalogEvidence(carrier, endpoint))
      )),
      componentKnowledge = None
    )
  }

  private def _value[A](value: Consequence[A]): A = value match {
    case Consequence.Success(result) => result
    case Consequence.Failure(conclusion) => fail(conclusion.display)
  }

  private val _catalog_contract = ComponentKnowledgeManifestConsumerContractCodec.encode(_contract)

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(byte => f"${byte & 0xff}%02x").mkString

  private final class ContractFetcher(endpoint: URI, contract: String) extends CatalogFetcher with BokFetcher {
    var requests = Vector.empty[URI]
    def get(uri: URI): Consequence[String] = {
      requests = requests :+ uri
      if (uri == endpoint) Consequence.success(contract)
      else Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
    }
  }

  private object EmptyFetcher extends CatalogFetcher with BokFetcher {
    def get(uri: URI): Consequence[String] = Consequence.serviceUnavailable(s"Unexpected fetch: $uri")
  }
}
