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
final class CarReviewPairedBundleReviewApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD paired provider-bundle Review application" should {
    "admit supplied provider documents and return one CBD-owned report and gate" in {
      Given("a canonical report template and a path-free local-client provider submission")
      val template = CarReviewReportCodec.decode(_document("car-review-report-v1.json")).fold(_fail, identity).copy(baseline = None)
      val submission = SuppliedReviewBundleSet(template, Vector(_bundle_submission()))

      When("the authorized client submits its bounded provider documents")
      val response = new CarReviewPairedBundleReviewApplication().submit(submission, Set("reviewer")).fold(_fail, identity)

      Then("CBD returns the canonical report and its matching gate without a workspace reference")
      response.report.gate shouldBe response.gate
      response.report.evidence.map(_.id.value) should contain ("cozy:evidence-project-yaml")
      response.report.execution.providers.map(_.provider.id.value) shouldBe Vector("cozy")
      submission.bundles.flatMap(value => Vector(value.descriptor, value.providerRequest, value.bundle)).mkString should not include "workspace"
    }

    "reject a supplied bundle that is not bound to the requested canonical report" in {
      Given("a provider submission with a different Review identity")
      val template = CarReviewReportCodec.decode(_document("car-review-report-v1.json")).fold(_fail, identity).copy(baseline = None)
      val submission = SuppliedReviewBundleSet(template, Vector(_bundle_submission().copy(reviewId = ReviewId("different-review"))))

      When("the client submits documents for the different Review")
      val response = new CarReviewPairedBundleReviewApplication().submit(submission, Set("reviewer"))

      Then("CBD refuses it before provider documents can be admitted for the wrong report")
      response.isFaillure shouldBe true
    }
  }

  private def _bundle_submission(): SuppliedProviderBundleSubmission =
    SuppliedProviderBundleSubmission(
      ReviewId("review-example-001"),
      ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:" + ("a" * 64))),
      ProviderBundleAvailability.Enabled,
      _document("car-review-provider-descriptor-v1.json"),
      _document("car-review-provider-request-v1.json"),
      _document("car-review-evidence-bundle-v1.json")
    )

  private def _document(name: String): String =
    Files.readString(Path.of("docs", "spec", "examples", name))

  private def _fail(error: org.goldenport.Conclusion): Nothing =
    fail(error.toString)

  private def _fail(error: CarReviewCodecFailure): Nothing =
    fail(s"${error.code}: ${error.message}")
}
