package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*
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
final class ModelEditModelMutationSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  import ModelEditSpecFixture.{authorization, operationFor}
  "Direct contextual candidate mutation" should {
    "apply all ten commands across Actor Goal, Mono-Koto and Event Storming through the same service" in {
      Given("a source-owned basis, shared Actor and explicitly supplied Goal, Slice and independent external edges")
      val basis = basisPublication()
      val proposed = proposedPublication()
      When("Actor Goal and Event Storming actions add and link a Goal")
      val initial = session(basis)
      val desired = session(proposed).candidate
      val goal = desired.goals.head
      val added = _apply(initial, Context(Surface.ActorGoal, initial.scope, Subject.Actor(initial.candidate.actors.head.semanticTargetId)),
        Command.AddGoal(initial.candidate.actors.head, goal.reference.semanticTargetId, goal.intent, Some(goal.reference)))
      val eventgoal = ModelEditPalette.eventStormingContext(added.session,
        EventStormingProjection.project(added.session, selection(basis)).toOption.get, Subject.Goal(goal.reference.semanticTargetId)).toOption.get
      val linked = _apply(added.session, eventgoal, Command.LinkGoalUseCase(desired.goalLinks.head))
      Then("shared identity and accumulated candidate history agree across the source-owned views")
      linked.confirmation.actorGoals.goals.head.useCases shouldBe Vector(desired.useCases.head.semanticTargetId)
      linked.confirmation.confirmation.state shouldBe ModelEditViewProjection.State.Candidate
      linked.confirmation.confirmation.operations shouldBe linked.session.history.map(_.operation.reference)

      When("Mono-Koto creates a stable Slice and Event Storming refines its supplied successor")
      val monocontext = ModelEditPalette.monoKotoContext(linked.session, ModelEditPaletteSpecFixture.monoKoto(linked.session),
        Subject.UseCase(desired.useCases.head.semanticTargetId)).toOption.get
      val created = _apply(linked.session, monocontext, Command.CreateUseCaseSlice(desired.slices.head))
      val revised = desired.slices.head.copy(reference = record(desired.slices.head.reference.recordId.value, 7),
        title = "Refined submission", lifecycle = SliceLifecycle.InDevelopment)
      val eventcontext = ModelEditPalette.eventStormingContext(created.session,
        EventStormingProjection.project(created.session, selection(basis)).toOption.get, Subject.Slice(revised.identity)).toOption.get
      val refined = _apply(created.session, eventcontext, Command.RefineUseCaseSlice(desired.slices.head.reference, revised))
      Then("only the explicit successor changes and Slice identity and owner remain stable")
      refined.session.candidate.slices shouldBe Vector(revised)
      created.session.candidate.slices shouldBe desired.slices
      refined.confirmation.eventStorming.nodes.find(_.identity == revised.identity).get.title shouldBe revised.title

      Given("two exact external target observations from the selected candidate source artifact")
      val specification = desired.specificationLinks.head
      val ticket = desired.ticketLinks.head
      val specresolution = ResolutionEvidence(ExternalReference.Specification(specification.specification), ResolutionState.Resolved, snapshot, specification.attribution)
      val ticketresolution = ResolutionEvidence(ExternalReference.Ticket(ticket.ticket), ResolutionState.Resolved, snapshot, ticket.attribution)
      When("Use Case detail links both edges and exact subject actions independently unlink them")
      val detail = Context(Surface.UseCaseDetail, initial.scope, Subject.UseCase(revised.useCaseIdentity))
      val withspec = _apply(refined.session, detail, Command.LinkSliceSpecification(specification), Vector(specresolution))
      val withticket = _apply(withspec.session, detail, Command.LinkSliceTicket(ticket), Vector(ticketresolution))
      val unspec = _apply(withticket.session, Context(Surface.UseCaseDetail, initial.scope, Subject.Relationship(specification.reference)),
        Command.UnlinkSliceSpecification(specification))
      val unticket = _apply(unspec.session, eventcontext, Command.UnlinkSliceTicket(ticket))
      val goalcontext = Context(Surface.ActorGoal, initial.scope, Subject.Goal(goal.reference.semanticTargetId))
      val goalrefined = _apply(unticket.session, goalcontext, Command.RefineGoal(goal.reference, "Refined independent intent"))
      val complete = _apply(goalrefined.session, goalcontext, Command.UnlinkGoalUseCase(desired.goalLinks.head))
      val rendered = ModelEditWebView.confirmation(complete.confirmation)
      Then("reference linking changes only the candidate and all ten commands share one exact chain")
      withticket.session.candidate.specificationLinks shouldBe Vector(specification)
      withticket.session.candidate.ticketLinks shouldBe Vector(ticket)
      unspec.session.candidate.ticketLinks shouldBe Vector(ticket)
      complete.session.candidate.ticketLinks shouldBe empty
      complete.session.candidate.specificationLinks shouldBe empty
      complete.session.candidate.goalLinks shouldBe empty
      complete.session.candidate.goals.head.intent shouldBe "Refined independent intent"
      complete.session.history.map(value => ModelEditSession.commandKind(value.operation.command)).toSet shouldBe CommandKind.values.toSet
      complete.session.history.size shouldBe 10
      complete.confirmation.confirmation.canonicalSource shouldBe basis.source
      complete.session.history.forall(_.authorization.session == initial.reference) shouldBe true
      complete.session.history.find(_.operation.command == Command.LinkSliceTicket(ticket)).get.resolutionEvidence shouldBe Vector(ticketresolution)
      initial.candidate.goals shouldBe empty
      initial.history shouldBe empty
      rendered should include("Candidate changes")
      rendered should include("Refined independent intent")
    }

    "reject a permitted command targeting another context and retain the service's exact unresolved-link result" in {
      Given("a Slice candidate with a similarly presented but distinct selected Use Case")
      val publication = proposedPublication()
      When("a contextual mismatch, missing target resolution and a restricted grant are submitted")
      val initial = session(publication)
      val other = initial.candidate.useCases.head.copy(id = "another-use-case", semanticTargetId = identity("another-use-case"))
      val otherslice = initial.candidate.slices.head.copy(reference = record("another-slice"), identity = identity("another-slice"),
        useCaseIdentity = other.semanticTargetId)
      val current = initial.copy(candidate = initial.candidate.copy(useCases = initial.candidate.useCases :+ other,
        slices = initial.candidate.slices :+ otherslice, specificationLinks = Vector.empty, ticketLinks = Vector.empty))
      val ticket = initial.candidate.ticketLinks.head
      val command = Command.LinkSliceTicket(ticket)
      val operation = operationFor(current, command)
      val wrong = Context(Surface.MonoKoto, current.scope, Subject.UseCase(other.semanticTargetId))
      val detail = Context(Surface.UseCaseDetail, current.scope, Subject.Slice(ticket.sliceIdentity))
      val wrongsubject = _submit(current, wrong, operation, authorization(current), Vector.empty)
      val unresolved = _submit(current, detail, operation, authorization(current), Vector.empty)
      val denied = _submit(current, detail, operation, authorization(current).copy(permitted = Vector.empty), Vector.empty)
      Then("neither the surface nor grant creates a different subject, target or partial successor")
      wrongsubject shouldBe Left(ModelEditModelMutation.Failure.Palette(ModelEditPalette.Failure.SubjectMismatch(wrong.subject)))
      unresolved shouldBe Left(ModelEditModelMutation.Failure.Validation(ValidationResult(Vector(Violation.ResolutionAbsent(ExternalReference.Ticket(ticket.ticket))))))
      denied shouldBe Left(ModelEditModelMutation.Failure.Palette(ModelEditPalette.Failure.CommandUnavailable(CommandKind.LinkSliceTicket)))
      current.candidate.ticketLinks shouldBe empty
      current.history shouldBe empty
    }

    "retain supplied operation and candidate revisions for arbitrary direct Goal intent" in {
      forAll(Gen.choose(2L, 10000L), Gen.alphaNumStr.suchThat(_.nonEmpty)) { (revision, intent) =>
        Given("a caller-selected successor revision and independent intent")
        val publication = proposedPublication()
        When("the direct adapter and candidate service receive the same authorized exact operation")
        val current = session(publication)
        val goal = current.candidate.goals.head.reference
        val context = Context(Surface.ActorGoal, current.scope, Subject.Goal(goal.semanticTargetId))
        val operation = operationFor(current, Command.RefineGoal(goal, intent))
        val successor = record(current.candidate.reference.recordId.value, revision)
        val grant = authorization(current)
        val direct = ModelEditModelMutation.applyOperation(current, context, operation, successor, grant, Vector.empty, selection(publication), Map.empty)
        val semantic = ModelEditService.applyOperation(current, operation, successor, grant, Vector.empty)
        Then("the adapter is the same mutation route and does not allocate a competing identity")
        direct.toOption.get.session shouldBe semantic.toOption.get
        direct.toOption.get.confirmation.confirmation.candidate shouldBe successor
        direct.toOption.get.session.history.last.operation shouldBe operation
      }
    }
  }

  private def _apply(session: ModelEditSession, context: Context, command: Command,
    resolutions: Vector[ResolutionEvidence] = Vector.empty): ModelEditModelMutation.Applied =
    _submit(session, context, operationFor(session, command), authorization(session), resolutions)
      .fold(error => fail(error.toString), value => value)

  private def _submit(session: ModelEditSession, context: Context, operation: Operation, authorization: Authorization,
    resolutions: Vector[ResolutionEvidence]): Either[ModelEditModelMutation.Failure, ModelEditModelMutation.Applied] =
    ModelEditModelMutation.applyOperation(session, context, operation,
      record(session.candidate.reference.recordId.value, session.candidate.reference.recordRevision.value + 1),
      authorization, resolutions, selection(basisPublication()), Map.empty)
}
