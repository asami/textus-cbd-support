package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditViewProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  "Shared model-edit view" should {
    "distinguish canonical and candidate state while retaining independent Specification and Ticket traces" in {
      Given("a canonical basis and five accumulated candidate commands")
      val basis = basisPublication()
      val proposed = proposedPublication()
      When("the canonical and candidate views are projected from their exact selections")
      val canonical = ModelEditViewProjection.project(session(basis), selection(basis), Map.empty)
      val edited = editedSession(basis, proposed)
      val candidate = ModelEditViewProjection.project(edited, selection(proposed), Map.empty)
      Then("all views identify the same successor and the two external link families remain independent")
      canonical.toOption.get.confirmation.state shouldBe ModelEditViewProjection.State.CanonicalBasis
      candidate.toOption.get.confirmation.state shouldBe ModelEditViewProjection.State.Candidate
      candidate.toOption.get.confirmation.operations shouldBe edited.history.map(_.operation.reference)
      candidate.toOption.get.actorGoals.candidate shouldBe candidate.toOption.get.eventStorming.candidate
      candidate.toOption.get.traces.head.specificationLinks.size shouldBe 1
      candidate.toOption.get.traces.head.ticketLinks.size shouldBe 1
    }
    "order explicit subjects deterministically for arbitrary input collection order" in {
      forAll(Gen.oneOf(true, false)) { reversed =>
        Given("equivalent explicitly identified records in different collection orders")
        val publication = proposedPublication()
        val changed = if (reversed) publication.copy(document = publication.document.copy(elements = publication.document.elements.reverse,
          relationships = publication.document.relationships.reverse)) else publication
        When("the same candidate is displayed from both source orders")
        val current = session(publication)
        val first = ModelEditViewProjection.project(current, selection(publication), Map.empty)
        val second = ModelEditViewProjection.project(current, selection(changed), Map.empty)
        Then("view identity and presentation order do not depend on input collection position")
        second shouldBe first
      }
    }
    "retain absent, explicitly unavailable, complete-empty and available capability observations beside their references" in {
      Given("one exact Use Case and read-only demand evidence supplied independently of its editing session")
      val publication = proposedPublication()
      When("the same shared View is projected for each capability observation")
      val current = session(publication)
      val usecase = current.candidate.useCases.head.semanticTargetId
      val subject = ModelEditSession.Subject.UseCase(usecase)
      val unresolved = ModelEditSession.RelationshipScopeEvidence.Unresolved(subject, ModelEditSession.ResolutionState.Unavailable)
      val complete = ModelEditSession.RelationshipScopeEvidence.Complete(subject, snapshot)
      val references = Vector(record("read-only-demand-reference"))
      val absent = ModelEditViewProjection.project(current, selection(publication), Map.empty).toOption.get
      val unavailable = ModelEditViewProjection.project(current, selection(publication),
        Map(usecase -> ModelEditReview.CapabilityDemand(usecase, references, unresolved))).toOption.get
      val empty = ModelEditViewProjection.project(current, selection(publication),
        Map(usecase -> ModelEditReview.CapabilityDemand(usecase, Vector.empty, complete))).toOption.get
      val available = ModelEditViewProjection.project(current, selection(publication),
        Map(usecase -> ModelEditReview.CapabilityDemand(usecase, references, complete))).toOption.get
      Then("empty references do not hide unavailable evidence and supplied observations retain their exact refs and source")
      absent.traces.head.capabilityDemandRefs shouldBe empty.traces.head.capabilityDemandRefs
      absent.capabilityDemands shouldBe Vector(ModelEditReview.CapabilityDemand(usecase, Vector.empty, unresolved))
      empty.capabilityDemands shouldBe Vector(ModelEditReview.CapabilityDemand(usecase, Vector.empty, complete))
      unavailable.capabilityDemands shouldBe Vector(ModelEditReview.CapabilityDemand(usecase, references, unresolved))
      available.capabilityDemands shouldBe Vector(ModelEditReview.CapabilityDemand(usecase, references, complete))
      unavailable.traces.head.capabilityDemandRefs shouldBe references
      available.traces.head.capabilityDemandRefs shouldBe references
      available.confirmation shouldBe absent.confirmation
      available.actorGoals shouldBe empty.actorGoals
      available.eventStorming shouldBe unavailable.eventStorming
    }
  }
}
