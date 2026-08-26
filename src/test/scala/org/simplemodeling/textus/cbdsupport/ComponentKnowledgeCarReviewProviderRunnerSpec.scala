package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/** DOC-06 executable boundary for CAR Review over value-only carrier detail. */
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
      reconciled.limitations.map(_.code) should contain("cbd.car-review.component-knowledge.manual-completeness.metadata-insufficient")
      reconciled.limitations.map(_.code) should contain("cbd.car-review.component-knowledge.bok-publication-readiness.metadata-insufficient")
    }

    "reject a malformed source digest before producing a review conclusion" in {
      Given("an otherwise valid value-only profile without a bounded digest")
      val profile = _profile.copy(sourceDigest = "not-a-digest")
      val descriptor = ComponentKnowledgeCarReviewProviderRunner.descriptorDocument(profile)

      When("the provider executes")
      val result = new ComponentKnowledgeCarReviewProviderRunner(profile).execute(_request(profile, descriptor))

      Then("it fails rather than fabricating evidence")
      result should matchPattern {
        case ProviderBundleRunnerResult.Failed("component-knowledge-provider-source-digest-invalid", _, _) =>
      }
    }
  }

  private val _profile = ComponentKnowledgeCarReviewProviderProfile(
    ReviewProviderIdentity(ReviewProviderId("cbd-component-knowledge"), ReviewVersion("1.0.0")),
    ReviewRuleIdentity(ReviewRuleId("cbd.component-knowledge.car-review"), ReviewVersion("1.0.0")),
    "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    ComponentKnowledgeDetail(
      "local-car",
      "car-storage",
      "car:org.example:textus-order:1.2.0",
      Some("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
      "org.example.Order",
      "1.2.0",
      "cncf.component-knowledge-carrier.v1",
      "component-knowledge.json",
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
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
