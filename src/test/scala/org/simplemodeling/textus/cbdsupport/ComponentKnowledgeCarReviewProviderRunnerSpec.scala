package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/** DOC-06 executable boundary for CAR Review over value-only carrier detail. */
/*
 * @since   Aug. 26, 2026
 * @version Oct. 17, 2026
 */
final class ComponentKnowledgeCarReviewProviderRunnerSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  "The Component knowledge CAR Review provider" should {
    "admit deterministic metadata checks while keeping content-only and BoK checks explicitly unknown" in {
      Given("admitted value-only detail containing one documentation resource and one restricted source resource")
      val profile = _profile
      val descriptor = ComponentKnowledgeCarReviewProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor)

      When("the bounded provider emits and CBD admits its evidence bundle")
      val bundle = new ComponentKnowledgeCarReviewProviderRunner(profile).execute(request) match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case other => fail(s"expected Component knowledge review bundle, got $other")
      }
      val reconciled = _reconcile(request, descriptor, bundle)

      Then("integrity and source policy use declared metadata, while content and independent publication remain unknown")
      _outcome(reconciled, "resource-integrity") shouldBe Some("assurance")
      _outcome(reconciled, "source-policy") shouldBe Some("finding")
      _outcome(reconciled, "manual-completeness") shouldBe Some("unknown")
      _outcome(reconciled, "scaladoc") shouldBe Some("unknown")
      _outcome(reconciled, "help-discovery") shouldBe Some("unknown")
      _outcome(reconciled, "bok-publication-readiness") shouldBe Some("unknown")
      reconciled.evidence.flatMap(_.facts("metadataOnly").flatMap(_.asBoolean)).distinct shouldBe Vector(true)
      reconciled.evidence.flatMap(_.facts.keys).toSet should not contain "content"
      reconciled.evidence.foreach { evidence =>
        evidence.facts.keys.toSet shouldBe Set("sourceId", "evidenceLocation", "checkId", "capabilityId", "outcome", "componentId", "logicalRelease", "logicalPaths", "metadataOnly")
        evidence.facts("componentId").flatMap(_.asString) shouldBe Some(profile.detail.componentId)
        evidence.facts("logicalRelease").flatMap(_.asString) shouldBe Some(profile.detail.logicalRelease)
        evidence.facts("sourceId").flatMap(_.asString) shouldBe Some(profile.detail.sourceId)
        evidence.facts("evidenceLocation").flatMap(_.asString) shouldBe Some(profile.detail.evidenceLocation)
      }
      reconciled.limitations.map(_.code) should contain("cbd.car-review.component-knowledge.manual-completeness.metadata-insufficient")
      reconciled.limitations.map(_.code) should contain("cbd.car-review.component-knowledge.bok-publication-readiness.metadata-insufficient")
    }

    "derive the provider profile from admitted detail with explicit source attribution" in {
      Given("admitted value-only detail with its explicit Component, release and source location")
      val detail = _profile.detail.copy(artifactChecksumSha256 = None)

      When("the provider input is projected from that detail")
      val result = ComponentKnowledgeCarReviewProviderRunner.fromDetail(_profile.provider, _profile.ruleSet, detail)

      Then("the original detail and rule identity are retained without requiring an artifact checksum")
      result.map(_.detail) shouldBe Some(detail)
      result.map(_.provider) shouldBe Some(_profile.provider)
      result.map(_.ruleSet) shouldBe Some(_profile.ruleSet)
    }
  }

  private val _profile = ComponentKnowledgeCarReviewProviderProfile(
    ReviewProviderIdentity(ReviewProviderId("cbd-component-knowledge"), ReviewVersion("1.0.0")),
    ReviewRuleIdentity(ReviewRuleId("cbd.component-knowledge.car-review"), ReviewVersion("1.0.0")),
    ComponentKnowledgeDetail(
      "local-car",
      "car-storage",
      "car:org.example:textus-order:1.2.0",
      Some("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
      "org.example.Order",
      "1.2.0",
      "cncf.component-knowledge-carrier.v1",
      "component-knowledge.json",
      Vector(
        ComponentKnowledgeResourceDetail("org.example.Order", "1.2.0", None, "Documentation", "urn:cncf:resource:example:order-documentation", "documentation/order.md", "documentation", "documentation", Some("en"), "text/markdown", 11L, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", "component", "stable", "component-declared", "Apache-2.0", "metadata-only", "available", "verified", "granted", "expanded-car", "org.example:textus-order:1.2.0", "component-car:example-order", "expanded-car:2", false, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
        ComponentKnowledgeResourceDetail("org.example.Order", "1.2.0", None, "Source", "urn:cncf:resource:example:order-source", "source/order.scala", "source-code", "source-code", Some("scala"), "text/x-scala", 17L, "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210", "component", "stable", "component-declared", "LicenseRef-Internal", "reference-only", "restricted", "verified", "denied", "expanded-car", "org.example:textus-order:1.2.0", "component-car:example-order", "expanded-car:2", false, "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210")
      ),
      0
    )
  )

  private def _request(profile: ComponentKnowledgeCarReviewProviderProfile, descriptor: String): ProviderBundleExecutionRequest = {
    val reviewid = ReviewId("review-component-knowledge-001")
    val target = ReviewTarget(ReviewTargetKind("project"), Some("org.example"), "textus-order", Some(ReviewVersion("1.2.0")), ReviewDigest("sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"))
    ProviderBundleExecutionRequest(reviewid, target, profile.provider, ProviderBundleAvailability.Enabled, descriptor, ComponentKnowledgeCarReviewProviderRunner.requestDocument(reviewid, target), 0L)
  }

  private def _reconcile(request: ProviderBundleExecutionRequest, descriptor: String, bundle: String): CarReviewReconciliationResult =
    CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(request.reviewId, request.target, ProviderBundleAvailability.Enabled, descriptor, request.providerRequest, bundle)) match {
      case ProviderBundleAdmissionOutcome.Admitted(value) => CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(value, bundle))).toOption.get
      case ProviderBundleAdmissionOutcome.Refused(value) => fail(value.limitation.code)
    }

  private def _outcome(value: CarReviewReconciliationResult, suffix: String): Option[String] =
    value.observations.find(_.id.value.endsWith(suffix)).map(_.`type`.value)
}
