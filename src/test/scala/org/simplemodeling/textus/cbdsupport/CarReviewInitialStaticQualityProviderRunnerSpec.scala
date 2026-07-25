package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 24, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewInitialStaticQualityProviderRunnerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The initial static CAR quality provider" should {
    "map fixed Security, Domain, Documentation, Resilience, Testability, Evaluability, Observability, and UX checks into attributable canonical results" in {
      Given("a bounded static-analysis result with pass, fail, and missing facts")
      val profile = _profile(_evidence(
        authorization = Some(true),
        documentation = Some(false),
        observability = None,
        uxSkill = None
      ))
      val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor)

      When("CBD executes and admits the fixed deterministic rules")
      val bundle = new CarReviewInitialStaticQualityProviderRunner(profile).execute(request) match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case value => fail(s"Expected completed static-quality bundle but got $value")
      }
      val reconciled = _reconcile(request, descriptor, bundle)

      Then("only supplied facts become Assurance or Finding; missing facts remain attributable Unknown")
      reconciled.observations.find(_.id.value.endsWith("security-authorization")).map(_.`type`.value) shouldBe Some("assurance")
      reconciled.observations.find(_.id.value.endsWith("documentation-rationale")).map(value => (value.`type`.value, value.severity.map(_.value))) shouldBe Some(("finding", Some("medium")))
      reconciled.observations.find(_.id.value.endsWith("observability-logging-schema")).map(_.`type`.value) shouldBe Some("unknown")
      reconciled.observations.find(_.id.value.endsWith("ux-skill")).map(_.`type`.value) shouldBe Some("unknown")
      reconciled.observations.find(_.id.value.endsWith("domain-identity")).map(_.mappings.qualityCapabilities.map(_.value)) shouldBe Some(Vector("quality.domain.identity-consistency"))
      reconciled.observations.find(_.id.value.endsWith("ux-web")).map(_.mappings.qualityCapabilities.map(_.value)) shouldBe Some(Vector("quality.ux.web"))
      reconciled.evidence.map(_.facts("sourceDigest").flatMap(_.asString)).distinct shouldBe Vector(Some("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
      reconciled.limitations.map(_.code) should contain("cbd.car-review.initial-static.observability-logging-schema.evidence-unavailable")
      reconciled.limitations.map(_.code) should contain("cbd.car-review.initial-static.ux-skill.evidence-unavailable")
    }

    "reject a provider profile without a bounded static source digest" in {
      Given("an otherwise complete static-analysis profile with an invalid source identity")
      val profile = _profile(_evidence().copy(sourceDigest = "not-a-digest"))
      val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor, "review-initial-static-invalid")

      When("the provider attempts to execute")
      val result = new CarReviewInitialStaticQualityProviderRunner(profile).execute(request)

      Then("it fails before fabricating Evidence or a quality conclusion")
      result should matchPattern {
        case ProviderBundleRunnerResult.Failed("initial-static-quality-source-digest-invalid", _, _) =>
      }
    }
  }

  private def _reconcile(request: ProviderBundleExecutionRequest, descriptor: String, bundle: String): CarReviewReconciliationResult =
    CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(request.reviewId, request.target, ProviderBundleAvailability.Enabled, descriptor, request.providerRequest, bundle)) match {
      case ProviderBundleAdmissionOutcome.Admitted(value) => CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(value, bundle))).toOption.get
      case ProviderBundleAdmissionOutcome.Refused(value) => fail(value.limitation.code)
    }

  private def _profile(evidence: CarReviewInitialStaticQualityEvidence): CarReviewInitialStaticQualityProviderProfile =
    CarReviewInitialStaticQualityProviderProfile(
      ReviewProviderIdentity(ReviewProviderId("cozy-static-quality"), ReviewVersion("0.1.0")),
      ReviewRuleIdentity(ReviewRuleId("cozy-static-quality.car-review"), ReviewVersion("1.0.0")),
      evidence
    )

  private def _evidence(
    authorization: Option[Boolean] = Some(true),
    documentation: Option[Boolean] = Some(true),
    observability: Option[Boolean] = Some(true),
    uxSkill: Option[Boolean] = Some(true)
  ): CarReviewInitialStaticQualityEvidence =
    CarReviewInitialStaticQualityEvidence(
      "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      authorization,
      Some(true),
      Some(true),
      documentation,
      Some(true),
      Some(true),
      Some(true),
      observability,
      Some(true),
      Some(true),
      uxSkill,
      Some(true)
    )

  private def _request(profile: CarReviewInitialStaticQualityProviderProfile, descriptor: String, reviewid: String = "review-initial-static-001"): ProviderBundleExecutionRequest = {
    val target = ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"))
    ProviderBundleExecutionRequest(ReviewId(reviewid), target, profile.provider, ProviderBundleAvailability.Enabled, descriptor, CarReviewInitialStaticQualityProviderRunner.requestDocument(ReviewId(reviewid), target), startedAtMillis = 0L)
  }
}
