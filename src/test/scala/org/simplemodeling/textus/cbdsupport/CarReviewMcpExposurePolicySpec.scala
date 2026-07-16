package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.CarReviewMcpExposurePolicy

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewMcpExposurePolicySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review MCP exposure policy" should {
    "admit only bounded read projections and keep execution or cost-bearing operations private" in {
      Given("the fixed Review MCP operation table")

      Then("read projections alone are eligible and every unsafe or unknown action is private")
      CarReviewMcpExposurePolicy.ReadOnlyOperations.foreach(CarReviewMcpExposurePolicy.isMcpReadable(_) shouldBe true)
      CarReviewMcpExposurePolicy.PrivateOperations.foreach(CarReviewMcpExposurePolicy.isMcpPrivate(_) shouldBe true)
      CarReviewMcpExposurePolicy.isMcpPrivate("arbitraryReviewHistory") shouldBe true
    }
  }
}
