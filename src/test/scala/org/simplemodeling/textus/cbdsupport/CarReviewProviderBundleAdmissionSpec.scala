package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.security.MessageDigest

import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewProviderBundleAdmissionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review Provider bundle admission" should {
    "admit one exact descriptor, request, and bundle exchange" in {
      Given("the canonical Cozy descriptor, request, and evidence bundle")
      val context = _context(ProviderBundleAvailability.Enabled)

      When("CBD validates schema, capabilities, target, digests, and local references")
      val outcome = CarReviewProviderBundleAdmission.admit(context)

      Then("the bundle is admitted with its provider identity and deterministic digests")
      outcome shouldBe ProviderBundleAdmissionOutcome.Admitted(
        AdmittedProviderBundle(
          ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14")),
          ReviewRuleIdentity(ReviewRuleId("cozy.car-review"), ReviewVersion("1.0.0")),
          ReviewDigest(_request_digest),
          ReviewDigest(_bundle_digest),
          Vector("evidence-project-yaml", "evidence-cml-model"),
          Vector("observation-component-identity", "observation-runtime-unknown"),
          Vector(ReviewLimitation("runtime-evidence-not-supported", ReviewLimitationScope("capability"), Some("cozy.car-analysis"), "Operational maturity cannot be assessed from Cozy static evidence.", false))
        )
      )
    }

    "refuse incompatible and unavailable provider outcomes without fabricating assurance" in {
      Given("a bundle whose target digest does not match the admitted Review target")
      val mismatched = _context(ProviderBundleAvailability.Enabled, _replace(_bundle, _target_digest, _other_digest))

      When("CBD admits the incompatible bundle and an unavailable provider")
      val incompatible = CarReviewProviderBundleAdmission.admit(mismatched)
      val unavailable = CarReviewProviderBundleAdmission.admit(_context(ProviderBundleAvailability.Unavailable))

      Then("both outcomes preserve provider attribution as an Unknown-shaped refusal")
      incompatible should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(Some(ReviewProviderIdentity(ReviewProviderId("cozy"), _)), ReviewProviderState("incompatible"), ReviewLimitation("bundle-target-mismatch", _, _, _, _), false)) =>
      }
      unavailable should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(Some(ReviewProviderIdentity(ReviewProviderId("cozy"), _)), ReviewProviderState("unavailable"), ReviewLimitation("provider-unavailable", _, _, _, true), false)) =>
      }
    }

    "refuse a schema-shaped exchange when either deterministic digest no longer binds it" in {
      Given("a request and a bundle changed after their canonical digests were created")
      val changedrequest = _replace(_request, "120000", "120001")
      val changedbundle = _replace(_bundle, "Project and CML component identities agree.", "Project and CML component identities agree only partially.")

      When("CBD evaluates the two digest bindings independently")
      val requestoutcome = CarReviewProviderBundleAdmission.admit(_context(ProviderBundleAvailability.Enabled, request = changedrequest))
      val bundleoutcome = CarReviewProviderBundleAdmission.admit(_context(ProviderBundleAvailability.Enabled, bundle = changedbundle))

      Then("each changed input is refused with its attributable deterministic reason")
      requestoutcome should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, _, ReviewLimitation("request-digest-mismatch", _, _, _, _), false)) =>
      }
      bundleoutcome should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, _, ReviewLimitation("bundle-digest-mismatch", _, _, _, _), false)) =>
      }
    }

    "refuse unknown provider-document fields before they enter review reconciliation" in {
      Given("an otherwise canonical bundle with an unrecognized root member")
      val unknownbundle = _replace(
        _bundle,
        "\"documentType\": \"evidence-bundle\",",
        "\"documentType\": \"evidence-bundle\", \"unexpected\": true,"
      )

      When("CBD applies the strict provider-document shape")
      val outcome = CarReviewProviderBundleAdmission.admit(_context(ProviderBundleAvailability.Enabled, bundle = unknownbundle))

      Then("the provider result is refused rather than repaired or silently projected")
      outcome should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("incompatible"), ReviewLimitation("bundle-contains-unknown-field", _, _, _, _), false)) =>
      }
    }

    "preserve disabled and failed providers without attempting an implicit fallback" in {
      Given("the same descriptor and input documents under disabled and failed availability")
      val disabled = _context(ProviderBundleAvailability.Disabled)
      val failed = _context(ProviderBundleAvailability.Failed)

      When("CBD evaluates provider availability before parsing the bundle into review evidence")
      val disabledoutcome = CarReviewProviderBundleAdmission.admit(disabled)
      val failedoutcome = CarReviewProviderBundleAdmission.admit(failed)

      Then("disabled becomes attributable Unknown while failed provider work is an attributable run failure")
      disabledoutcome should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("disabled"), _, false)) =>
      }
      failedoutcome should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, ReviewProviderState("failed"), _, true)) =>
      }
    }
  }

  private val _descriptor = _load("car-review-provider-descriptor-v1.json")
  private val _request = _load("car-review-provider-request-v1.json")
  private val _bundle = _load("car-review-evidence-bundle-v1.json")
  private val _target_digest = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  private val _other_digest = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
  private val _request_digest = "sha256:d88fe085924cc9d234d233963bb624e584826bb8f02f19fc47253888a7c21d97"
  private val _bundle_digest = "sha256:6feabc3d18f6b5dd9c62d9dcbd478b22aacd46fa50a0c8415950ce26b39ba875"

  private def _context(
    availability: ProviderBundleAvailability,
    bundle: String = _bundle,
    request: String = _request
  ): ProviderBundleAdmissionContext =
    ProviderBundleAdmissionContext(
      ReviewId("review-example-001"),
      ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest(_target_digest)),
      availability,
      _descriptor,
      request,
      bundle
    )

  private def _load(name: String): String = Files.readString(Path.of("docs", "spec", "examples", name))
  private def _replace(value: String, from: String, to: String): String = value.replace(from, to)
}
