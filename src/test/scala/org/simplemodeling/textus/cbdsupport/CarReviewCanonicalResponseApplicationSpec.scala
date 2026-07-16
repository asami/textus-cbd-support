package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewCanonicalResponseApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD canonical Review response" should {
    "return one canonical report and its CBD-owned gate from an admitted provider bundle" in {
      Given("a decoded report template and an admitted Cozy bundle")
      val template = CarReviewReportCodec.decode(_report).fold(_fail, identity).copy(baseline = None)
      val admitted = CarReviewProviderBundleAdmission.admit(ProviderBundleAdmissionContext(template.reviewId, template.target, ProviderBundleAvailability.Enabled, _descriptor, _request, _bundle)) match {
        case ProviderBundleAdmissionOutcome.Admitted(value) => value
        case ProviderBundleAdmissionOutcome.Refused(value) => fail(value.limitation.code)
      }

      When("CBD reconciles, assesses, and assembles the submitted evidence")
      val response = new CarReviewCanonicalResponseApplication().build(template, Vector(AdmittedProviderBundleInput(admitted, _bundle)), Set("reviewer")).fold(_fail, identity)

      Then("the report and gate share the one canonical CBD conclusion")
      response.report.gate shouldBe response.gate
      response.report.evidence.nonEmpty shouldBe true
      response.gate.result.value shouldBe "unknown"
      response.report.execution.providers.map(_.provider.id.value) shouldBe Vector("cozy")
      response.attestation.reviewId shouldBe response.report.reviewId
      response.attestation.reportId shouldBe response.report.reportId
      response.attestation.reportDigest shouldBe response.report.reportDigest
      response.attestation.targetDigest shouldBe response.report.target.digest
      response.attestation.profile shouldBe response.report.profile
      response.attestation.gate shouldBe response.report.gate
      response.attestation.providers.map(_.provider.id.value) shouldBe Vector("cozy")
      CarReviewAttestationCodec.encode(response.attestation).isRight shouldBe true
    }

    "refuse an ambiguous capability template instead of silently discarding an assessment" in {
      Given("a report template with no configured capability assessment")
      val template = CarReviewReportCodec.decode(_report).fold(_fail, identity).copy(assessments = Vector.empty, baseline = None)

      When("CBD is asked to construct its canonical response")
      val response = new CarReviewCanonicalResponseApplication().build(template, Vector.empty, Set("reviewer"))

      Then("the response boundary refuses the incomplete assessment policy")
      response.isFaillure shouldBe true
    }

    "refuse a stale template baseline instead of discarding it during response construction" in {
      Given("a report template carrying a baseline that was not recalculated for submitted evidence")
      val template = CarReviewReportCodec.decode(_report).fold(_fail, identity)

      When("CBD is asked to construct its canonical response")
      val response = new CarReviewCanonicalResponseApplication().build(template, Vector.empty, Set("reviewer"))

      Then("the response boundary preserves baseline integrity by refusing the stale template")
      response.isFaillure shouldBe true
    }
  }

  private val _descriptor = Files.readString(Path.of("docs", "spec", "examples", "car-review-provider-descriptor-v1.json"))
  private val _request = Files.readString(Path.of("docs", "spec", "examples", "car-review-provider-request-v1.json"))
  private val _bundle = Files.readString(Path.of("docs", "spec", "examples", "car-review-evidence-bundle-v1.json"))
  private val _report = Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))

  private def _fail(error: org.goldenport.Conclusion): Nothing = fail(error.toString)
  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
