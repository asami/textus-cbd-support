package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class EventStormingProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ModelEditProjectionSpecFixture.*
  "Cross-model Event Storming" should {
    "keep cause, reaction, external dependency and context separate in the selected candidate" in {
      Given("one candidate with source-owned behavioral and contextual edges")
      val publication = proposedPublication()
      When("the candidate is projected across the source model families")
      val selected = session(publication)
      val view = EventStormingProjection.project(selected, selection(publication))
      Then("all declared model families coexist while context never becomes a cause")
      view.toOption.get.candidate shouldBe selected.candidate.reference
      view.toOption.get.nodes.map(_.kind).toSet shouldBe ModelEditSourceProjection.elementKinds.toSet
      view.toOption.get.edges.count(_.kind == "cause") shouldBe 1
      view.toOption.get.edges.count(_.kind == "context") shouldBe 1
      view.toOption.get.edges.find(_.kind == "dependency").get.to shouldBe ModelEditSourceProjection.Endpoint.External(
        ModelEditSourceProjection.RecordRef("logistics", "warehouse", 3))
      view.toOption.get.nodes.find(_.kind == "slice").get.fields.map(_.name) shouldBe Vector("requestId")
    }
  }
}
