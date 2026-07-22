package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewDeliveryProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review delivery projection" should {
    "retain one canonical Report identity across dashboard, document, and item diagnosis without rerunning a provider" in {
      Given("one canonical Report containing Finding, Assurance, Unknown, baseline, and report limitation")
      val source = _report
      val report = CarReviewReportCodec.withCalculatedDigest(source.copy(
        baseline = Some(ReviewBaseline(ReviewReportId("report-baseline"), ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), Vector(ReviewObservationId("report-finding-missing-rationale")), Vector.empty, Vector(ReviewObservationId("report-unknown-runtime")))),
        limitations = source.limitations :+ ReviewLimitation("report-limit", ReviewLimitationScope("report"), None, "token=delivery-secret /private/repository", false),
        gate = source.gate.copy(reasons = Vector("token=gate-secret /private/gate"))
      )).fold(_fail, identity)

      When("CBD projects the common delivery document and diagnoses one Finding and capability")
      val first = CarReviewDeliveryProjection.project(report)
      val second = CarReviewDeliveryProjection.project(report)
      val finding = CarReviewDeliveryProjection.diagnoseObservation(report, ReviewObservationId("report-finding-missing-rationale")).getOrElse(fail("Finding diagnosis missing"))
      val capability = CarReviewDeliveryProjection.diagnoseCapability(report, ReviewCapabilityId("quality.domain.identity-consistency")).getOrElse(fail("Capability diagnosis missing"))

      Then("all surfaces keep Report, Observation, Evidence, capability, provider, gate, and baseline identities")
      first shouldBe second
      first.dashboard.reportId shouldBe report.reportId
      first.dashboard.reportDigest shouldBe report.reportDigest
      first.dashboard.target.name shouldBe report.target.name
      first.dashboard.gate.result shouldBe report.gate.result
      first.dashboard.baseline.map(_.reportDigest) shouldBe Some(ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
      first.observations.map(_.id) shouldBe report.observations.map(_.id).sortBy(_.value)
      finding.reportId shouldBe report.reportId
      finding.reportDigest shouldBe report.reportDigest
      finding.observationIds shouldBe Vector(ReviewObservationId("report-finding-missing-rationale"))
      finding.evidenceIds should not be empty
      finding.providerIds should not be empty
      capability.capabilityIds shouldBe Vector(ReviewCapabilityId("quality.domain.identity-consistency"))
      capability.observationIds should not be empty
      capability.locations should contain("project.yaml")

      And("the projection is read-only and redacts diagnostic content without inventing conclusions")
      report.reportDigest shouldBe CarReviewReportCodec.withCalculatedDigest(report).fold(_fail, identity).reportDigest
      first.limitations.map(_.message).mkString(" ") should not include "delivery-secret"
      first.limitations.map(_.message).mkString(" ") should not include "/private/repository"
      first.dashboard.gate.reasons.mkString(" ") should not include "gate-secret"
      first.dashboard.gate.reasons.mkString(" ") should not include "/private/gate"
      finding.kind shouldBe "observation"
      capability.kind shouldBe "capability"
      finding.disposition should not be empty
      capability.disposition shouldBe empty
    }

    "return no diagnosis for a canonical item that is absent from the exact Report" in {
      Given("one canonical Report")
      val report = _report

      When("a delivery client asks for an absent Observation or capability")
      val observation = CarReviewDeliveryProjection.diagnoseObservation(report, ReviewObservationId("report-absent"))
      val capability = CarReviewDeliveryProjection.diagnoseCapability(report, ReviewCapabilityId("quality.absent"))

      Then("the projection has no fallback, synthetic diagnosis, or implicit history lookup")
      observation shouldBe empty
      capability shouldBe empty
    }
  }

  private val _report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
