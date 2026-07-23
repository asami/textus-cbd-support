package org.simplemodeling.textus.cbdsupport

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

final class CarReviewTerminalHistorySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review terminal history" should {
    "admit only non-success terminal states" in {
      Given("the four retained non-success terminal names and two non-terminal-history names")
      val terminalnames = Vector("failed", "cancelled", "expired", "incompatible")

      When("each name is interpreted as a terminal-history state")
      val terminalresults = terminalnames.map(CarReviewDiagnosisTerminalState.parse)
      val completed = CarReviewDiagnosisTerminalState.parse("completed")
      val running = CarReviewDiagnosisTerminalState.parse("running")

      Then("only failed, cancelled, expired, and incompatible are admitted")
      terminalresults.forall(_.isRight) shouldBe true
      completed.left.toOption.map(_.code) shouldBe Some("review-terminal-state-invalid")
      running.left.toOption.map(_.code) shouldBe Some("review-terminal-state-invalid")
    }
  }
}
