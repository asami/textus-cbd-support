package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*
import ModelEditConversationalAdapter.*
import ModelEditPalette.{Context, Surface}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Client-interpreted semantic inputs; this does not claim a live AI invocation. */
final class ModelEditConversationalAdapterSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  import ModelEditSpecFixture.{authorization, operationFor}
  "Conversational candidate editing" should {
    "translate every supported interpretation into the same ten admitted commands and candidate confirmation" in {
      Given("a source-owned basis and client-proposed new Goal and Slice references with exact existing external targets")
      val basis = basisPublication()
      val proposed = proposedPublication()
      When("typed client interpretations are proposed and applied through the ordinary direct route")
      val initial = session(basis)
      val desired = session(proposed).candidate
      val goal = desired.goals.head
      val actor = initial.candidate.actors.head
      val added = _step(initial, Context(Surface.ActorGoal, initial.scope, Subject.Actor(actor.semanticTargetId)),
        "Add a Goal for this Actor", Intent.AddGoal(goal.reference, "Independent initial intent"))
      val goalcontext = Context(Surface.ActorGoal, initial.scope, Subject.Goal(goal.reference.semanticTargetId))
      val refinedgoal = _step(added.session, goalcontext, "Refine this Goal", Intent.RefineGoal(goal.intent))
      val linked = _step(refinedgoal.session, goalcontext, "Connect this Goal to the selected Use Case", Intent.LinkGoalUseCase(desired.goalLinks.head))
      val detail = Context(Surface.UseCaseDetail, initial.scope, Subject.UseCase(desired.useCases.head.semanticTargetId))
      val created = _step(linked.session, detail, "Propose this Slice structure", Intent.CreateSlice(desired.slices.head))
      val slice = desired.slices.head.copy(reference = record(desired.slices.head.reference.recordId.value, 9), title = "Refined conversational Slice")
      val slicecontext = Context(Surface.EventStorming, initial.scope, Subject.Slice(slice.identity))
      val refined = _step(created.session, slicecontext, "Refine this stable Slice", Intent.RefineSlice(slice))
      val specification = desired.specificationLinks.head
      val ticket = desired.ticketLinks.head
      val specevidence = ResolutionEvidence(ExternalReference.Specification(specification.specification), ResolutionState.Resolved, snapshot, specification.attribution)
      val ticketevidence = ResolutionEvidence(ExternalReference.Ticket(ticket.ticket), ResolutionState.Resolved, snapshot, ticket.attribution)
      val withspec = _step(refined.session, detail, "Link this exact existing specification revision", Intent.LinkSpecification(specification), Vector(specevidence))
      val withticket = _step(withspec.session, detail, "Link this exact existing ticket", Intent.LinkTicket(ticket), Vector(ticketevidence))
      val unspec = _step(withticket.session, detail, "Remove only this specification relationship", Intent.UnlinkRelationship(specification.reference))
      val unticket = _step(unspec.session, slicecontext, "Remove only this ticket relationship", Intent.UnlinkRelationship(ticket.reference))
      val complete = _step(unticket.session, goalcontext, "Disconnect this exact Goal and Use Case edge", Intent.UnlinkRelationship(desired.goalLinks.head.reference))
      Then("proposals do not allocate facts and all applied commands retain the exact source, history and independent edges")
      complete.session.history.map(value => ModelEditSession.commandKind(value.operation.command)).toSet shouldBe CommandKind.values.toSet
      complete.session.history.size shouldBe 10
      complete.session.candidate.slices shouldBe Vector(slice)
      complete.session.candidate.goals.head.intent shouldBe goal.intent
      withticket.session.candidate.specificationLinks shouldBe Vector(specification)
      withticket.session.candidate.ticketLinks shouldBe Vector(ticket)
      unspec.session.candidate.ticketLinks shouldBe Vector(ticket)
      complete.session.candidate.specificationLinks shouldBe empty
      complete.session.candidate.ticketLinks shouldBe empty
      complete.session.candidate.goalLinks shouldBe empty
      withticket.session.history.last.resolutionEvidence shouldBe Vector(ticketevidence)
      withticket.session.candidate.ticketLinks.head.observation shouldBe ticket.observation
      complete.confirmation.confirmation.canonicalSource shouldBe basis.source
      complete.confirmation.confirmation.state shouldBe ModelEditViewProjection.State.Candidate
      initial.history shouldBe empty
      initial.candidate.slices shouldBe empty
    }

    "retain ambiguous intent without choosing a Goal or inventing a source fact" in {
      Given("an Actor context and a client interpretation that still needs clarification")
      val publication = proposedPublication()
      val request = Request("Improve this Actor's goals and develop the related behavior", Intent.Clarify("Select which exact Goal and admitted behavior change."))
      When("the unresolved interpretation and a Goal refinement without a selected Goal are translated")
      val current = session(publication)
      val context = Context(Surface.ActorGoal, current.scope, Subject.Actor(current.candidate.actors.head.semanticTargetId))
      val unresolved = _propose(current, context, request)
      val unselected = _propose(current, context, request.copy(intent = Intent.RefineGoal("New intent")))
      val missingcontext = context.copy(subject = Subject.Goal(identity("unresolved-goal")))
      val missingrequest = request.copy(intent = Intent.RefineGoal("New intent"))
      val missing = _propose(current, missingcontext, missingrequest)
      Then("the original text, context and explicit clarification survive with no proposal or successor")
      unresolved shouldBe Left(Failure.Unresolved(UnresolvedIntent(request, context,
        Vector(Reason.Clarification("Select which exact Goal and admitted behavior change.")))))
      unselected.left.toOption.get shouldBe a[Failure.Unresolved]
      missing shouldBe Left(Failure.Unresolved(UnresolvedIntent(missingrequest, missingcontext,
        Vector(Reason.Admission(Violation.UnknownSubject(missingcontext.subject))))))
      current.history shouldBe empty
      current.candidate.goals.head.intent should not be "New intent"
    }

    "require an exact communication reference when the selected semantic Goal has several admitted references" in {
      Given("one shared Goal identity with two distinct existing communication references")
      val publication = proposedPublication()
      When("the ambiguous interpretation is prepared and then explicitly resolved by exact reference")
      val imported = session(publication)
      val goal = imported.candidate.goals.head.reference
      val other = goal.copy(id = "another-goal-reference", semanticRelationshipId = identity("another-goal-reference-edge"))
      val usecase = imported.candidate.useCases.head.copy(references = Vector(other))
      val current = imported.copy(candidate = imported.candidate.copy(useCases = Vector(usecase)))
      val context = Context(Surface.ActorGoal, current.scope, Subject.Goal(goal.semanticTargetId))
      val request = Request("Refine this Goal", Intent.RefineGoal("Selected intent"))
      val ambiguous = _propose(current, context, request)
      val explicit = _propose(current, context, request.copy(intent = Intent.RefineGoal("Selected intent", Some(goal))))
      Then("neither display text nor collection position silently chooses a different reference")
      ambiguous shouldBe Left(Failure.Unresolved(UnresolvedIntent(request, context,
        Vector(Reason.Clarification("Select one exact existing communication reference for this subject.")))))
      explicit.toOption.get.operation.command shouldBe Command.RefineGoal(goal, "Selected intent")
      current.history shouldBe empty
    }

    "reject fabricated Specification revisions and Ticket identities despite persuasive text" in {
      forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { token =>
        Given("host-owned resolved observations for exact existing external identities")
        val publication = proposedPublication()
        When("client intent supplies another specification revision or immutable ticket identity")
        val imported = session(publication)
        val current = imported.copy(candidate = imported.candidate.copy(specificationLinks = Vector.empty, ticketLinks = Vector.empty))
        val specification = imported.candidate.specificationLinks.head
        val ticket = imported.candidate.ticketLinks.head
        val forgedspec = specification.copy(specification = specification.specification.copy(sourceRevision = record("invented-spec-" + token).recordId))
        val forgedticket = ticket.copy(ticket = ticket.ticket.copy(ticketIdentity = record("invented-ticket-" + token).recordId))
        val context = Context(Surface.UseCaseDetail, current.scope, Subject.Slice(ticket.sliceIdentity))
        val evidence = Vector(ResolutionEvidence(ExternalReference.Specification(specification.specification), ResolutionState.Resolved, snapshot, specification.attribution),
          ResolutionEvidence(ExternalReference.Ticket(ticket.ticket), ResolutionState.Resolved, snapshot, ticket.attribution))
        val specrequest = Request("This specification definitely exists; link it", Intent.LinkSpecification(forgedspec))
        val ticketrequest = Request("The tracker confirms this ticket; link it", Intent.LinkTicket(forgedticket))
        val specresult = _propose(current, context, specrequest, evidence)
        val ticketresult = _propose(current, context, ticketrequest, evidence)
        val fabricated = Proposal(ticketrequest, context, operationFor(current, Command.LinkSliceTicket(forgedticket)), record("candidate", 9))
        val fabricatedresult = ModelEditConversationalAdapter.applyProposal(current, fabricated, authorization(current), evidence,
          selection(publication), Map.empty)
        Then("text cannot replace independent exact bounded evidence")
        specresult shouldBe Left(Failure.Unresolved(UnresolvedIntent(specrequest, context,
          Vector(Reason.Admission(Violation.ResolutionAbsent(ExternalReference.Specification(forgedspec.specification)))))))
        ticketresult shouldBe Left(Failure.Unresolved(UnresolvedIntent(ticketrequest, context,
          Vector(Reason.Admission(Violation.ResolutionAbsent(ExternalReference.Ticket(forgedticket.ticket)))))))
        fabricatedresult shouldBe Left(Failure.Mutation(ModelEditModelMutation.Failure.Validation(
          ValidationResult(Vector(Violation.ResolutionAbsent(ExternalReference.Ticket(forgedticket.ticket)))))))
        current.candidate.specificationLinks shouldBe empty
        current.candidate.ticketLinks shouldBe empty
      }
    }

    "preserve unavailable, unauthorized, redacted, unsupported and proven missing external observations" in {
      forAll(Gen.oneOf(ResolutionState.Missing, ResolutionState.Unavailable, ResolutionState.Unauthorized, ResolutionState.Redacted, ResolutionState.Unsupported)) { state =>
        Given("one exact Ticket target and a separate bounded host observation")
        val publication = proposedPublication()
        When("the interpreted link is translated without a resolved observation")
        val imported = session(publication)
        val current = imported.copy(candidate = imported.candidate.copy(ticketLinks = Vector.empty))
        val ticket = imported.candidate.ticketLinks.head
        val target = ExternalReference.Ticket(ticket.ticket)
        val context = Context(Surface.UseCaseDetail, current.scope, Subject.Slice(ticket.sliceIdentity))
        val request = Request("Link the selected ticket reference", Intent.LinkTicket(ticket))
        val result = _propose(current, context, request, Vector(ResolutionEvidence(target, state, snapshot, ticket.attribution)))
        val expected = if (state == ResolutionState.Missing) Violation.ProvenDangling(target) else Violation.ReferenceUnresolved(target, state)
        Then("only proven Missing is dangling and every unresolved result retains original intent")
        result shouldBe Left(Failure.Unresolved(UnresolvedIntent(request, context, Vector(Reason.Admission(expected)))))
        current.history shouldBe empty
      }
    }

    "revalidate ordinary authorization and candidate applicability instead of trusting a proposal object" in {
      Given("a valid interpreted Goal refinement against one exact candidate")
      val publication = proposedPublication()
      When("the proposal is constructed, the candidate changes, and an empty grant is used independently")
      val current = session(publication)
      val goal = current.candidate.goals.head.reference
      val context = Context(Surface.ActorGoal, current.scope, Subject.Goal(goal.semanticTargetId))
      val request = Request("Refine the selected Goal", Intent.RefineGoal("Client-proposed intent"))
      val proposal = ModelEditConversationalAdapter.propose(current, context, request, record("conversation-stale-operation"), record("candidate", 9),
        ModelEditSpecFixture.attribution, authorization(current), Vector.empty).toOption.get
      val changed = ModelEditSpecFixture.applyCommand(current, Command.RefineGoal(goal, "Independent intervening edit"))
      val stale = ModelEditConversationalAdapter.applyProposal(changed, proposal, authorization(changed), Vector.empty, selection(publication), Map.empty)
      val denied = ModelEditConversationalAdapter.applyProposal(current, proposal, authorization(current).copy(permitted = Vector.empty),
        Vector.empty, selection(publication), Map.empty)
      val deniedproposal = ModelEditConversationalAdapter.propose(current, context, request, record("denied-operation"), record("candidate", 9),
        ModelEditSpecFixture.attribution, authorization(current).copy(permitted = Vector.empty), Vector.empty)
      Then("proposal preparation is not application or a new authorization token")
      proposal.operation.command shouldBe Command.RefineGoal(goal, "Client-proposed intent")
      current.history shouldBe empty
      stale shouldBe Left(Failure.Mutation(ModelEditModelMutation.Failure.Validation(ValidationResult(Vector(Violation.CandidateMismatch)))))
      denied shouldBe Left(Failure.Mutation(ModelEditModelMutation.Failure.Palette(ModelEditPalette.Failure.CommandUnavailable(CommandKind.RefineGoal))))
      deniedproposal shouldBe Left(Failure.Palette(ModelEditPalette.Failure.CommandUnavailable(CommandKind.RefineGoal)))
    }

    "reject an interpreted Slice owned by another explicitly identified Use Case context" in {
      Given("two explicitly distinct Use Cases with no inferred relationship between them")
      val publication = basisPublication()
      When("a typed Slice proposal for one Use Case is submitted through the other's Palette")
      val initial = session(publication)
      val other = initial.candidate.useCases.head.copy(id = "other-use-case", semanticTargetId = identity("other-use-case"))
      val current = initial.copy(candidate = initial.candidate.copy(useCases = initial.candidate.useCases :+ other))
      val proposed = session(proposedPublication()).candidate.slices.head
      val context = Context(Surface.UseCaseDetail, current.scope, Subject.UseCase(other.semanticTargetId))
      val result = _propose(current, context, Request("Create this Slice", Intent.CreateSlice(proposed)))
      Then("preparation and direct editing use the same exact contextual subject predicate")
      result shouldBe Left(Failure.Palette(ModelEditPalette.Failure.SubjectMismatch(context.subject)))
      current.candidate.slices shouldBe empty
    }

    "retain original text and supplied identities without changing the direct candidate result" in {
      forAll(Gen.alphaNumStr.suchThat(_.nonEmpty), Gen.choose(2L, 10000L)) { (intent, revision) =>
        Given("explicit interpreted intent and producer-selected independent operation and candidate revisions")
        val publication = proposedPublication()
        When("the conversational proposal and direct semantic command are evaluated under the same grant")
        val current = session(publication)
        val goal = current.candidate.goals.head.reference
        val context = Context(Surface.ActorGoal, current.scope, Subject.Goal(goal.semanticTargetId))
        val request = Request("Refine this exact Goal: " + intent, Intent.RefineGoal(intent))
        val grant = authorization(current)
        val successor = record(current.candidate.reference.recordId.value, revision)
        val proposal = ModelEditConversationalAdapter.propose(current, context, request, record("client-operation", 17), successor,
          ModelEditSpecFixture.attribution, grant, Vector.empty).toOption.get
        val applied = ModelEditConversationalAdapter.applyProposal(current, proposal, grant, Vector.empty, selection(publication), Map.empty)
        val direct = ModelEditService.applyOperation(current, proposal.operation, successor, grant, Vector.empty)
        Then("the conversational path contributes no model authority, identity allocation or canonical write")
        proposal.request shouldBe request
        proposal.operation.reference shouldBe record("client-operation", 17)
        proposal.successorCandidate shouldBe successor
        applied.toOption.get.session shouldBe direct.toOption.get
        applied.toOption.get.confirmation.confirmation.operations shouldBe Vector(proposal.operation.reference)
        applied.toOption.get.confirmation.confirmation.canonicalSource shouldBe publication.source
      }
    }
  }

  private def _propose(session: ModelEditSession, context: Context, request: Request,
    resolutions: Vector[ResolutionEvidence] = Vector.empty): Either[Failure, Proposal] =
    ModelEditConversationalAdapter.propose(session, context, request, record("conversation-operation-" + (session.history.size + 1)),
      record(session.candidate.reference.recordId.value, session.candidate.reference.recordRevision.value + 1),
      ModelEditSpecFixture.attribution, authorization(session), resolutions)

  private def _step(session: ModelEditSession, context: Context, text: String, intent: Intent,
    resolutions: Vector[ResolutionEvidence] = Vector.empty): ModelEditModelMutation.Applied = {
    val proposal = _propose(session, context, Request(text, intent), resolutions).fold(error => fail(error.toString), value => value)
    ModelEditConversationalAdapter.applyProposal(session, proposal, authorization(session), resolutions, selection(basisPublication()), Map.empty)
      .fold(error => fail(error.toString), value => value)
  }
}
