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
final class CarReviewQualityCoverageProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review total quality coverage projection" should {
    "make every supported capability observed or explicitly Unknown without running a provider" in {
      Given("one canonical Report with only its initial domain assessment and mapped observations")
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(s"${error.code}: ${error.message}"), identity)

      When("CBD projects capability coverage from the immutable Report")
      val coverage = CarReviewQualityCoverageProjection.project(report)

      Then("the projection is total, retains canonical observed IDs, and exposes missing providers as rule-bound Unknown")
      coverage.map(_.rule.capabilityId) shouldBe CarReviewCapabilityCatalog.definitions.map(_.id).sortBy(_.value)
      coverage.map(_.rule.capabilityId).distinct should have size CarReviewCapabilityCatalog.definitions.size
      val domain = coverage.find(_.rule.capabilityId.value == "quality.domain.identity-consistency").getOrElse(fail("domain coverage missing"))
      domain.state shouldBe CarReviewQualityCoverageState.Observed
      domain.observationIds should not be empty
      val security = coverage.find(_.rule.capabilityId.value == "quality.security.authorization").getOrElse(fail("security coverage missing"))
      security.state shouldBe CarReviewQualityCoverageState.Unknown
      security.limitation.map(_.code) shouldBe Some("cbd.car-review.quality.security.authorization.evidence-unavailable")
      security.limitation.map(_.retryable) shouldBe Some(true)
    }
  }
}
