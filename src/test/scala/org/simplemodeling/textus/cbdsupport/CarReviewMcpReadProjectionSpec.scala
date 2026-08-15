package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 *  version Jul. 16, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewMcpReadProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The bounded MCP Review read projection" should {
    "return one authorized redacted Report summary, report, Finding, and Assurance collection" in {
      Given("one retained canonical Report with a credential-shaped message")
      val original = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
      val report = CarReviewReportCodec.withCalculatedDigest(original.copy(
        observations = original.observations.updated(0, original.observations.head.copy(message = "token=secret-value /private/repository/project.yaml"))
      )).fold(_fail, identity)
      val repository = new CarReviewRepository()
      repository.retain(report) shouldBe Right(report)
      val reads = new CarReviewMcpReadApplication(repository)

      When("an authorized reader requests the bounded projections")
      val summary = reads.summary(report.reportId, Set("viewer")).fold(_fail_conclusion, identity)
      val projected = reads.report(report.reportId, Set("viewer")).fold(_fail_conclusion, identity)
      val findings = reads.findings(report.reportId, Set("viewer"), 1).fold(_fail_conclusion, identity)
      val assurances = reads.assurances(report.reportId, Set("viewer"), 1).fold(_fail_conclusion, identity)

      Then("canonical identities remain while raw facts, credentials, and full paths are withheld")
      summary.reportDigest shouldBe report.reportDigest
      projected.observations.map(_.id).toSet shouldBe report.observations.map(_.id).toSet
      projected.observations.map(_.message).mkString(" ") should not include "secret-value"
      projected.observations.map(_.locations).flatten.mkString(" ") should not include "/private/repository"
      projected.qualityCoverage.size shouldBe CarReviewCapabilityCatalog.definitions.size
      projected.qualityCoverage.map(_.capabilityId.value) shouldBe projected.qualityCoverage.map(_.capabilityId.value).sorted
      projected.qualityCoverage.find(_.capabilityId == ReviewCapabilityId("quality.domain.identity-consistency")).map(_.observationIds) shouldBe Some(Vector(ReviewObservationId("report-assurance-component-identity")))
      projected.qualityCoverage.find(_.capabilityId == ReviewCapabilityId("quality.ai.operability.skill")).flatMap(_.limitation).map(_.code) shouldBe Some("cbd.car-review.quality.ai.operability.skill.evidence-unavailable")
      findings.size shouldBe 1
      assurances.size shouldBe 1
      projected.toString should not include "facts"
    }

    "refuse unauthorized, unbounded, and absent Report reads" in {
      Given("an empty bounded reader")
      val reads = new CarReviewMcpReadApplication(new CarReviewRepository())

      Then("MCP cannot enumerate arbitrary Review history")
      reads.summary(ReviewReportId("report-missing"), Set.empty).isFaillure shouldBe true
      reads.findings(ReviewReportId("report-missing"), Set("viewer"), CarReviewMcpReadApplication.MAX_OBSERVATIONS + 1).isFaillure shouldBe true
      reads.report(ReviewReportId("report-missing"), Set("viewer")).isFaillure shouldBe true
    }
  }

  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
  private def _fail_conclusion(error: org.goldenport.Conclusion): Nothing = fail(error.toString)
}
