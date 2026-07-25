package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 24, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewCostScenarioProviderRunnerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review Cost View provider" should {
    "keep Static Web estimates distinct from normalized Gemma plus MCP measurements" in {
      Given("a Static Web App opportunity and a period-normalized Gemma plus MCP measurement")
      val profile = _profile(Vector(
        _static().copy(expectedReduction = Some(CarReviewCostReduction(BigDecimal(40), "percent-origin-requests"))),
        _gemma().copy(
          expectedReduction = Some(CarReviewCostReduction(BigDecimal(25), "percent-commercial-tokens")),
          measuredReduction = Some(CarReviewCostReduction(BigDecimal(21), "percent-commercial-tokens")),
          comparisonPeriod = Some("2026-07-01/2026-07-14"),
          normalizedUnit = Some("requests-per-day")
        )
      ))
      val descriptor = CarReviewCostScenarioProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor)

      When("CBD admits the cost provider bundle and reconciles it into canonical records")
      val bundle = new CarReviewCostScenarioProviderRunner(profile).execute(request) match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case value => fail(s"Expected completed cost bundle but got $value")
      }
      val reconciled = _reconcile(request, descriptor, bundle)

      Then("the expected-only Static Web opportunity is Unknown, while the normalized Gemma measurement is Assurance")
      reconciled.observations.find(_.id.value.endsWith("cost-static-web")).map(_.`type`.value) shouldBe Some("unknown")
      reconciled.observations.find(_.id.value.endsWith("cost-gemma-mcp")).map(_.`type`.value) shouldBe Some("assurance")
      reconciled.limitations.map(_.code) should contain("cost-measurement-unavailable")
      reconciled.observations.find(_.id.value.endsWith("cost-static-web")).map(_.mappings.qualityCapabilities.map(_.value)) shouldBe Some(Vector(
        "quality.cost-efficiency.infrastructure", "quality.cost-efficiency.operations", "quality.performance.resource-efficiency", "quality.sustainability.work-avoidance"
      ))
      reconciled.observations.find(_.id.value.endsWith("cost-gemma-mcp")).map(_.mappings.qualityCapabilities.map(_.value)) shouldBe Some(Vector(
        "quality.cost-efficiency.operations", "quality.cost-efficiency.development", "quality.performance.resource-efficiency", "quality.sustainability.work-avoidance"
      ))
      val original = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(s"${error.code}: ${error.message}"), identity)
      val costview = CarReviewCostViewProjection.project(original.copy(evidence = reconciled.evidence, observations = reconciled.observations))
      costview.map(value => (value.kind.value, value.expectedReduction.map(_.value), value.measuredReduction.map(_.value), value.measurementIsComparable)) shouldBe Vector(
        ("gemma-mcp", Some(BigDecimal(25)), Some(BigDecimal(21)), true),
        ("static-web-app", Some(BigDecimal(40)), None, false)
      )
      costview.flatMap(_.operationalTradeoffs).distinct should contain("Cache freshness, local model operations, and fallback quality are measured separately.")
      bundle should include("expectedReduction")
      bundle should include("measuredReduction")
      bundle should include("comparisonPeriod")
      bundle should include("normalizedUnit")
    }

    "refuse a measured-saving claim without comparison period and normalized unit as a Finding" in {
      Given("a Gemma plus MCP scenario that supplies a numeric result but not its measurement context")
      val profile = _profile(Vector(_gemma().copy(
        measuredReduction = Some(CarReviewCostReduction(BigDecimal(30), "percent-commercial-tokens"))
      )))
      val descriptor = CarReviewCostScenarioProviderRunner.descriptorDocument(profile)
      val request = _request(profile, descriptor, "review-cost-invalid-measurement")

      When("CBD evaluates the deterministic measurement contract")
      val bundle = new CarReviewCostScenarioProviderRunner(profile).execute(request) match {
        case ProviderBundleRunnerResult.Completed(value, _) => value
        case value => fail(s"Expected completed cost bundle but got $value")
      }
      val reconciled = _reconcile(request, descriptor, bundle)

      Then("the provider retains the raw number as Evidence but makes the conclusion a medium Finding")
      reconciled.observations.map(value => (value.`type`.value, value.severity.map(_.value))) shouldBe Vector(("finding", Some("medium")))
      reconciled.evidence.map(_.facts("costOptimization").flatMap(_.hcursor.get[String]("kind").toOption)) shouldBe Vector(Some("gemma-mcp"))
    }
  }

  private def _reconcile(request: ProviderBundleExecutionRequest, descriptor: String, bundle: String): CarReviewReconciliationResult =
    CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(request.reviewId, request.target, ProviderBundleAvailability.Enabled, descriptor, request.providerRequest, bundle)) match {
      case ProviderBundleAdmissionOutcome.Admitted(value) => CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(value, bundle))).toOption.get
      case ProviderBundleAdmissionOutcome.Refused(value) => fail(value.limitation.code)
    }

  private def _profile(scenarios: Vector[CarReviewCostScenario]): CarReviewCostScenarioProviderProfile =
    CarReviewCostScenarioProviderProfile(
      ReviewProviderIdentity(ReviewProviderId("textus-cost"), ReviewVersion("0.1.0")),
      ReviewRuleIdentity(ReviewRuleId("textus-cost.car-review"), ReviewVersion("1.0.0")),
      scenarios
    )

  private def _static(): CarReviewCostScenario = _scenario("static-web", CarReviewCostScenarioKind.StaticWebApp, "Dynamic application server delivers cacheable public content.", "Origin requests and web-server compute.", "Deliver immutable public assets through a static web application and CDN.")
  private def _gemma(): CarReviewCostScenario = _scenario("gemma-mcp", CarReviewCostScenarioKind.GemmaMcp, "All AI tasks use a commercial general-purpose model.", "Commercial-model token usage and fallback volume.", "Route suitable bounded work through Gemma plus MCP before commercial fallback.")

  private def _scenario(id: String, kind: CarReviewCostScenarioKind, current: String, driver: String, optimization: String): CarReviewCostScenario =
    CarReviewCostScenario(id, kind, current, driver, optimization, None, None, None, None,
      Vector("Latency and availability objectives remain explicit."),
      Vector("Cache freshness, local model operations, and fallback quality are measured separately."),
      ReviewConfidence("medium")
    )

  private def _request(profile: CarReviewCostScenarioProviderProfile, descriptor: String, reviewid: String = "review-cost-001"): ProviderBundleExecutionRequest = {
    val target = ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"))
    ProviderBundleExecutionRequest(ReviewId(reviewid), target, profile.provider, ProviderBundleAvailability.Enabled, descriptor, CarReviewCostScenarioProviderRunner.requestDocument(ReviewId(reviewid), target), startedAtMillis = 0L)
  }
}
