package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ActorGoalProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ModelEditProjectionSpecFixture.*
  "Actor Goal list" should {
    "show an unlinked shared Actor and an independently editable Goal intent" in {
      Given("a source basis and the accumulated creation candidate")
      val basis = basisPublication()
      val proposed = proposedPublication()
      When("both selections are projected")
      val original = ActorGoalProjection.project(session(basis), selection(basis))
      val edited = editedSession(basis, proposed)
      val view = ActorGoalProjection.project(edited, selection(basis))
      Then("one shared Actor survives and the Goal retains its explicit Actor and Use Case links")
      original.toOption.get.actors.map(_.identity.value) shouldBe Vector("customer")
      original.toOption.get.goals shouldBe empty
      view.toOption.get.candidate shouldBe edited.candidate.reference
      view.toOption.get.actors.size shouldBe 1
      view.toOption.get.goals.head.intent shouldBe "Place another order"
      view.toOption.get.goals.head.actors.map(_.value) shouldBe Vector("customer")
      view.toOption.get.goals.head.useCases.map(_.value) shouldBe Vector("place-order")
    }
  }
}
