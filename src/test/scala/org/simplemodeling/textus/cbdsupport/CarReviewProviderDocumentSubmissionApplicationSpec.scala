package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewProviderDocumentSubmissionApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD provider-document submission application" should {
    "construct the canonical response from CBD-owned policy rather than a client report template" in {
      Given("a path-free provider document set and a recording CBD template provider")
      val provider = new RecordingTemplateProvider(_template)
      val application = new CarReviewProviderDocumentSubmissionApplication(provider)

      When("the authorized client submits only its provider documents")
      val response = application.submit(SuppliedProviderBundleSet(Vector(_submission)), Set("reviewer")).fold(_fail, identity)

      Then("CBD resolves the report policy from the submitted Review and Target")
      provider.requested shouldBe Some(_submission.reviewId -> _submission.target)
      response.report.reviewId shouldBe _submission.reviewId
      response.report.gate shouldBe response.gate
    }

    "deny an unauthorized client before CBD resolves report policy" in {
      Given("a provider document set from a caller without submission authority")
      val provider = new RecordingTemplateProvider(_template)
      val application = new CarReviewProviderDocumentSubmissionApplication(provider)

      When("the caller submits its documents")
      val response = application.submit(SuppliedProviderBundleSet(Vector(_submission)), Set("viewer"))

      Then("CBD does not resolve a report template or admit provider evidence")
      response.isFaillure shouldBe true
      provider.requested shouldBe None
    }
  }

  private val _template = CarReviewReportCodec.decode(_document("car-review-report-v1.json")).fold(_fail_codec, identity).copy(baseline = None)
  private val _submission = SuppliedProviderBundleSubmission(
    ReviewId("review-example-001"),
    ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:" + ("a" * 64))),
    ProviderBundleAvailability.Enabled,
    _document("car-review-provider-descriptor-v1.json"),
    _document("car-review-provider-request-v1.json"),
    _document("car-review-evidence-bundle-v1.json")
  )

  private final class RecordingTemplateProvider(value: CarReviewReport) extends CarReviewCanonicalTemplateProvider {
    var requested: Option[(ReviewId, ReviewTarget)] = None

    def template(reviewId: ReviewId, target: ReviewTarget): Consequence[CarReviewReport] = {
      requested = Some(reviewId -> target)
      Consequence.success(value)
    }
  }

  private def _document(name: String): String =
    Files.readString(Path.of("docs", "spec", "examples", name))

  private def _fail(error: org.goldenport.Conclusion): Nothing =
    fail(error.toString)

  private def _fail_codec(error: CarReviewCodecFailure): Nothing =
    fail(s"${error.code}: ${error.message}")
}
