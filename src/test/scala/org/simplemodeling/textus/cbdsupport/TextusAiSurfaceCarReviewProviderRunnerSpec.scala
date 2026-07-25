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
final class TextusAiSurfaceCarReviewProviderRunnerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The Textus AI surface CAR Review provider" should {
    "admit Textus MCP and standard Skill support while retaining content as a separate Unknown" in {
      Given("compatible Textus metadata plus complete published MCP and Skill contracts")
      val profile = _profile()
      val descriptor = TextusAiSurfaceCarReviewProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor)
      val runner = new TextusAiSurfaceCarReviewProviderRunner(profile)

      When("CBD executes the deterministic provider through the normal admission boundary")
      val bundle = runner.execute(request) match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case value => fail(s"Expected completed Textus AI surface bundle but got $value")
      }
      val admitted = CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(
        request.reviewId,
        request.target,
        ProviderBundleAvailability.Enabled,
        descriptor,
        request.providerRequest,
        bundle
      ))
      val reconciled = admitted match {
        case ProviderBundleAdmissionOutcome.Admitted(value) =>
          CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(value, bundle))).toOption.get
        case ProviderBundleAdmissionOutcome.Refused(value) => fail(value.limitation.code)
      }

      Then("framework support is attributable Assurance, but content usefulness is not promoted to Assurance")
      reconciled.observations.filter(_.`type`.value == "assurance").map(_.mappings.qualityCapabilities.map(_.value)) shouldBe Vector(
        Vector("quality.ai.operability.mcp"),
        Vector("quality.ai.operability.skill")
      )
      reconciled.observations.filter(_.rule.id.value.endsWith(".content")).map(_.`type`.value) shouldBe Vector("unknown", "unknown")
      reconciled.observations.flatMap(_.mappings.implementationSubjects).distinct shouldBe Vector("component:textus-user-account")
      reconciled.evidence.map(_.kind).toSet should contain allOf (
        TextusAiSurfaceCarReviewProviderRunner.mcpCatalogEvidenceKind,
        TextusAiSurfaceCarReviewProviderRunner.skillBundleEvidenceKind,
        TextusAiSurfaceCarReviewProviderRunner.mcpDescriptionEvidenceKind,
        TextusAiSurfaceCarReviewProviderRunner.skillManifestEvidenceKind
      )
      reconciled.limitations.map(_.code) should contain("textus-ai-surface-semantic-review-pending")
      bundle should not include "endpoint"
      bundle should not include "credential"
    }

    "retain unsupported MCP and Skill as explicit Unknown instead of fabricating Textus support" in {
      Given("a target without admitted compatible Textus runtime evidence")
      val profile = _profile().copy(compatibleTextusRuntime = false)
      val descriptor = TextusAiSurfaceCarReviewProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor, "review-ai-surface-unsupported")
      val runner = new TextusAiSurfaceCarReviewProviderRunner(profile)

      When("the support provider reports the bounded compatibility result")
      val bundle = runner.execute(request) match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case value => fail(s"Expected completed Textus AI surface bundle but got $value")
      }
      val admitted = CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(request.reviewId, request.target, ProviderBundleAvailability.Enabled, descriptor, request.providerRequest, bundle))
      val reconciled = admitted match {
        case ProviderBundleAdmissionOutcome.Admitted(value) => CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(value, bundle))).toOption.get
        case ProviderBundleAdmissionOutcome.Refused(value) => fail(value.limitation.code)
      }

      Then("both support rows remain mapped Unknown with retryable missing-evidence limitations and attributable negative metadata")
      reconciled.observations.filter(_.id.value.endsWith("-support")).map(_.`type`.value) shouldBe Vector("unknown", "unknown")
      reconciled.observations.filter(_.id.value.endsWith("-support")).flatMap(_.evidenceIds.map(_.value)) shouldBe Vector("textus-ai-surface:mcp-support", "textus-ai-surface:skill-support")
      reconciled.limitations.filter(_.code.endsWith(".evidence-unavailable")).map(_.retryable) shouldBe Vector(true, true)
      bundle should include("compatibleTextusRuntime")
    }

    "make incomplete published content a deterministic finding without withdrawing independent framework support" in {
      Given("a compatible Textus component with an incomplete component-specific MCP publication")
      val valid = _publication("textus-standard-skills", "1.0.0")
      val incomplete = _publication("textus-user-account-mcp", "0.2.0").copy(authorityBoundary = None, operationIds = Vector.empty)
      val profile = _profile(componentmcp = Some(incomplete), componentskill = Some(valid))
      val descriptor = TextusAiSurfaceCarReviewProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor, "review-ai-surface-incomplete")
      val runner = new TextusAiSurfaceCarReviewProviderRunner(profile)

      When("the deterministic publication metadata check runs")
      val bundle = runner.execute(request) match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case value => fail(s"Expected completed Textus AI surface bundle but got $value")
      }
      val admitted = CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(request.reviewId, request.target, ProviderBundleAvailability.Enabled, descriptor, request.providerRequest, bundle))
      val reconciled = admitted match {
        case ProviderBundleAdmissionOutcome.Admitted(value) => CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(value, bundle))).toOption.get
        case ProviderBundleAdmissionOutcome.Refused(value) => fail(value.limitation.code)
      }

      Then("the MCP support row remains Assurance while the MCP content row is a mapped Finding")
      reconciled.observations.find(_.id.value.endsWith("mcp-support")).map(_.`type`.value) shouldBe Some("assurance")
      reconciled.observations.find(_.id.value.endsWith("mcp-content-incomplete")).map(value => (value.`type`.value, value.severity.map(_.value), value.mappings.qualityCapabilities.map(_.value))) shouldBe Some(
        ("finding", Some("medium"), Vector("quality.ai.operability.mcp"))
      )
    }
  }

  private def _profile(
    componentmcp: Option[TextusAiSurfacePublication] = Some(_publication("textus-user-account-mcp", "0.2.0")),
    componentskill: Option[TextusAiSurfacePublication] = Some(_publication("textus-user-account-skill", "0.2.0"))
  ): TextusAiSurfaceSupportProfile =
    TextusAiSurfaceSupportProfile(
      ReviewProviderIdentity(ReviewProviderId("textus-ai-surface"), ReviewVersion("0.1.0")),
      ReviewRuleIdentity(ReviewRuleId("textus-ai-surface.car-review"), ReviewVersion("1.0.0")),
      compatibleTextusRuntime = true,
      mcpProjectionEnabled = true,
      standardSkillSet = Some(_publication("textus-standard-skills", "1.0.0")),
      componentmcp,
      componentskill
    )

  private def _publication(id: String, version: String): TextusAiSurfacePublication =
    TextusAiSurfacePublication(
      id,
      version,
      "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      Some("Bounded component guidance and operation discovery."),
      Some("Read-only discovery is allowed; mutations require the component authority policy."),
      Some("The surface returns bounded, redacted results and reports unsupported operations."),
      Vector("catalog", "component-get")
    )

  private def _request(
    profile: TextusAiSurfaceSupportProfile,
    descriptor: String,
    reviewid: String = "review-ai-surface-001"
  ): ProviderBundleExecutionRequest = {
    val target = ReviewTarget(
      ReviewTargetKind("project"),
      Some("org.textus"),
      "textus-user-account",
      Some(ReviewVersion("0.2.0-SNAPSHOT")),
      ReviewDigest("sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
    )
    ProviderBundleExecutionRequest(
      ReviewId(reviewid),
      target,
      profile.provider,
      ProviderBundleAvailability.Enabled,
      descriptor,
      TextusAiSurfaceCarReviewProviderRunner.requestDocument(ReviewId(reviewid), target),
      startedAtMillis = 0L
    )
  }
}
