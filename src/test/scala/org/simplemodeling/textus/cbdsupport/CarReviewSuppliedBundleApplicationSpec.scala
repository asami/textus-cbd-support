package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
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
final class CarReviewSuppliedBundleApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD supplied provider-bundle admission" should {
    "admit a bounded local-client bundle without accepting a workspace path" in {
      Given("one authorized client submission containing only provider documents")
      val application = new CarReviewSuppliedBundleApplication()
      val submission = _submission()

      When("CBD admits the evidence submitted by the client")
      val outcome = application.submit(submission, Set("reviewer")).fold(_fail, identity)

      Then("the exact provider identity is admitted through the generic v1 boundary")
      outcome match {
        case ProviderBundleAdmissionOutcome.Admitted(value) =>
          value.provider.id.value shouldBe "cozy"
          value.requestDigest.value should startWith ("sha256:")
        case ProviderBundleAdmissionOutcome.Refused(value) =>
          fail(s"unexpected refusal: ${value.limitation.code}")
      }
    }

    "refuse an unauthorized caller before it can submit provider evidence" in {
      Given("a valid-looking client submission from a viewer")
      val application = new CarReviewSuppliedBundleApplication()

      When("the caller submits the evidence")
      val outcome = application.submit(_submission(), Set("viewer"))

      Then("CBD keeps evidence admission private to Review submitters")
      outcome.isFaillure shouldBe true
    }

    "reject an oversized document without a server filesystem lookup" in {
      Given("a client request above the provider-document bound")
      val application = new CarReviewSuppliedBundleApplication()
      val submission = _submission().copy(bundle = "x" * ((16 * 1024 * 1024) + 1))

      When("the client submits it to CBD")
      val outcome = application.submit(submission, Set("reviewer"))

      Then("the boundary rejects its bytes before provider admission")
      outcome.isFaillure shouldBe true
    }
  }

  private def _submission(): SuppliedProviderBundleSubmission =
    SuppliedProviderBundleSubmission(
      ReviewId("review-example-001"),
      ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:" + ("a" * 64))),
      ProviderBundleAvailability.Enabled,
      _document("car-review-provider-descriptor-v1.json"),
      _document("car-review-provider-request-v1.json"),
      _document("car-review-evidence-bundle-v1.json")
    )

  private def _document(name: String): String =
    Files.readString(Path.of("docs", "spec", "examples", name), StandardCharsets.UTF_8)

  private def _fail(error: org.goldenport.Conclusion): Nothing =
    fail(error.toString)
}
