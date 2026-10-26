package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.Inspectors.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ModelEditSession.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 26, 2026
 */
final class ModelEditSessionSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "ModelEditSession" should {
    "construct candidate-only operations" which {
      "retain explicitly proposed Slice identity and independently chosen versions without applying the operation" in {
        Given("an admitted Use Case basis and producer-selected Slice and operation versions")
        forAll(Gen.choose(1L, 100000L), Gen.choose(1L, 100000L)) { (sliceversion, operationversion) =>
          val session = _session()
          val proposed = _slice("slice-new", sliceversion)
          val operation = _operation(session, Command.CreateUseCaseSlice(proposed), operationversion)

          When("pure admission constructs the operation")
          val result = ModelEditSession.admit(session, operation, Vector.empty).toOption.get

          Then("the exact proposal and versions survive while the original candidate remains unchanged")
          result.operation shouldBe operation
          result.effect.subject shouldBe Subject.Slice(proposed.identity)
          result.effect.command shouldBe Command.CreateUseCaseSlice(proposed)
          session.candidate.slices shouldBe Vector(_slice())
          result.resolutionEvidence shouldBe empty
        }
      }

      "require refinement to preserve the Slice identity, owning Use Case and logical record identity" in {
        Given("one stable Slice and an explicitly higher producer revision")
        val session = _session()
        val existing = session.candidate.slices.head
        val revised = existing.copy(reference = _record("slice-1", 9L), title = "Refined", lifecycle = SliceLifecycle.InDevelopment)

        When("refinement changes presentation and lifecycle but retains semantic ownership")
        val admitted = ModelEditSession.admit(session, _operation(session, Command.RefineUseCaseSlice(existing.reference, revised)), Vector.empty)
        val moved = ModelEditSession.admit(session, _operation(session, Command.RefineUseCaseSlice(existing.reference,
          revised.copy(useCaseIdentity = _identity("other-use-case")))), Vector.empty)
        val unchanged = ModelEditSession.admit(session, _operation(session, Command.RefineUseCaseSlice(existing.reference, existing)), Vector.empty)

        Then("the higher-revision proposal is admitted while a silent Use Case move and an unchanged revision are rejected")
        admitted.isRight shouldBe true
        moved.left.toOption.get.violations should contain(Violation.InvalidRefinement(existing.reference))
        unchanged.isLeft shouldBe true
      }

      "reject a similarly titled but unknown Use Case and an occupied semantic identity" in {
        Given("a Slice title that repeats a real Use Case label but supplies a different exact endpoint")
        val session = _session()
        val proposed = _slice("slice-new").copy(title = "Place Order", useCaseIdentity = _identity("unknown-use-case"))

        When("admission receives explicit mismatched or occupied endpoints")
        val unknown = ModelEditSession.admit(session, _operation(session, Command.CreateUseCaseSlice(proposed)), Vector.empty)
        val occupied = ModelEditSession.admit(session, _operation(session, Command.CreateUseCaseSlice(_slice("use-case-1"))), Vector.empty)

        Then("labels cannot repair either operation")
        unknown.left.toOption.get.violations should contain(Violation.UnknownSubject(Subject.UseCase(_identity("unknown-use-case"))))
        occupied.left.toOption.get.violations should contain(Violation.IdentityOccupied(Subject.Slice(_identity("use-case-1"))))
      }
    }

    "preserve explicit external trace semantics" which {
      "admit one-to-many independent Specification and Ticket links regardless of basis iteration" in {
        Given("one Slice with two independently attributed Specification edges and two Ticket edges")
        val first = _specification_link("spec-edge-1", "spec-1")
        val second = _specification_link("spec-edge-2", "spec-2")
        val ticket = _ticket_link("ticket-edge-1", "ticket-1")
        val otherticket = _ticket_link("ticket-edge-2", "ticket-2")
        val evidence = Vector(_resolved(ExternalReference.Specification(second.specification)), _resolved(ExternalReference.Ticket(otherticket.ticket)))
        forAll(Gen.pick(2, evidence)) { permuted =>
          val session = _session().copy(candidate = _session().candidate.copy(specificationLinks = Vector(first), ticketLinks = Vector(ticket)))

          When("the two separate edge proposals are admitted against exact target evidence")
          val specificationresult = ModelEditSession.admit(session, _operation(session, Command.LinkSliceSpecification(second)), permuted.toVector).toOption.get
          val ticketresult = ModelEditSession.admit(session, _operation(session, Command.LinkSliceTicket(otherticket)), permuted.toVector).toOption.get

          Then("each operation retains its own endpoints and attribution without inferring a Specification-to-Ticket pair")
          specificationresult.effect.command shouldBe Command.LinkSliceSpecification(second)
          ticketresult.effect.command shouldBe Command.LinkSliceTicket(otherticket)
          specificationresult.resolutionEvidence shouldBe Vector(evidence.head)
          ticketresult.resolutionEvidence shouldBe Vector(evidence(1))
          session.candidate.specificationLinks shouldBe Vector(first)
          session.candidate.ticketLinks shouldBe Vector(ticket)
        }
      }

      "require the exact Specification owner, stable identity and source revision" in {
        Given("resolver evidence for the same stable specification at another source revision or project")
        val session = _session()
        val relationship = _specification_link()
        val target = ExternalReference.Specification(relationship.specification)
        val otherrevision = relationship.specification.copy(sourceRevision = _id("source-revision-2"))
        val otherowner = relationship.specification.copy(owner = _owner.copy(project = _id("other-project")))

        When("the link consumes neither exact target")
        val results = Vector(otherrevision, otherowner).map { reference =>
          ModelEditSession.admit(session, _operation(session, Command.LinkSliceSpecification(relationship)),
            Vector(_resolved(ExternalReference.Specification(reference))))
        }

        Then("both remain unresolved rather than finding a nearby target")
        all(results.map(_.left.toOption.get.violations)) should contain(Violation.ResolutionAbsent(target))
      }

      "distinguish proven dangling from unavailable, unauthorized, redacted and unsupported sources" in {
        Given("bounded exact target evidence with each non-resolved source outcome")
        val session = _session()
        val relationship = _ticket_link()
        val target = ExternalReference.Ticket(relationship.ticket)
        val states = Vector(ResolutionState.Missing, ResolutionState.Unavailable, ResolutionState.Unauthorized,
          ResolutionState.Redacted, ResolutionState.Unsupported)
        forAll(Gen.oneOf(states)) { state =>
          When("a new Ticket edge is proposed using that evidence")
          val result = ModelEditSession.admit(session, _operation(session, Command.LinkSliceTicket(relationship)),
            Vector(_resolved(target).copy(state = state)))

          Then("only bounded Missing evidence proves dangling and no partial operation is admitted")
          val expected = if (state == ResolutionState.Missing) Violation.ProvenDangling(target)
            else Violation.ReferenceUnresolved(target, state)
          result.left.toOption.get.violations shouldBe Vector(expected)
          result.toOption shouldBe empty
          session.candidate.ticketLinks shouldBe empty
        }
      }

      "reject ambiguous resolver evidence and evidence omitted from the operation dependencies" in {
        Given("two exact target claims or an otherwise resolved artifact not declared as a dependency")
        val session = _session()
        val relationship = _specification_link()
        val target = ExternalReference.Specification(relationship.specification)
        val evidence = _resolved(target)
        val operation = _operation(session, Command.LinkSliceSpecification(relationship))

        When("admission consumes those incomplete or ambiguous claims")
        val ambiguous = ModelEditSession.admit(session, operation, Vector(evidence, evidence.copy(state = ResolutionState.Missing)))
        val omitted = ModelEditSession.admit(session, operation.copy(preconditions = operation.preconditions.copy(requiredArtifacts = Vector.empty)), Vector(evidence))

        Then("neither a hidden resolver winner nor an undeclared dependency is selected")
        ambiguous.left.toOption.get.violations shouldBe Vector(Violation.ResolutionAmbiguous(target))
        omitted.left.toOption.get.violations shouldBe Vector(Violation.ArtifactDependencyMismatch)
      }

      "unlink only the exact attributed relationship version while retaining independent ticket observations" in {
        Given("one Specification edge and a Ticket identity with optional observed state/version")
        val relationship = _specification_link()
        val ticket = _ticket_link().copy(observation = Some(TicketObservation(Some(_id("open")), Some(_id("version-3")))))
        val session = _session().copy(candidate = _session().candidate.copy(specificationLinks = Vector(relationship), ticketLinks = Vector(ticket)))

        When("the caller selects an exact edge, another version, or an altered endpoint")
        val exact = ModelEditSession.admit(session, _operation(session, Command.UnlinkSliceSpecification(relationship)), Vector.empty)
        val changed = Vector(relationship.copy(reference = _record("spec-edge-1", 2L)),
          relationship.copy(specification = relationship.specification.copy(specificationIdentity = _id("other"))))
          .map(edge => ModelEditSession.admit(session, _operation(session, Command.UnlinkSliceSpecification(edge)), Vector.empty))
        val exactticket = ModelEditSession.admit(session, _operation(session, Command.UnlinkSliceTicket(ticket)), Vector.empty)

        Then("only the exact selections admit and observations never replace immutable Ticket identity")
        exact.isRight shouldBe true
        exactticket.isRight shouldBe true
        all(changed.map(_.isLeft)) shouldBe true
        ticket.copy(observation = None).ticket shouldBe ticket.ticket
        session.candidate.ticketLinks shouldBe Vector(ticket)
      }
    }

    "reuse Actor and Goal identity and exact control dependencies" which {
      "accept existing communication roles and reject a role-substituted Actor or fabricated Goal" in {
        Given("the existing Use Case communication Actor and Goal references")
        val session = _session()
        val actor = session.candidate.useCases.head.references.head
        val goal = session.candidate.useCases.head.references(1)
        val relationship = GoalUseCaseRelationship(_record("goal-edge"), goal.semanticTargetId, _identity("use-case-1"), _attribution)

        When("representative Goal operations use the shared identities")
        val commands = Vector(Command.AddGoal(actor, _identity("proposed-goal"), "New intent"),
          Command.RefineGoal(goal, "Refined intent"), Command.LinkGoalUseCase(relationship))
        val results = commands.map(command => ModelEditSession.admit(session, _operation(session, command), Vector.empty))
        val substituted = ModelEditSession.admit(session, _operation(session, Command.AddGoal(goal, _identity("new"), "intent")), Vector.empty)
        val fabricated = ModelEditSession.admit(session, _operation(session,
          Command.LinkGoalUseCase(relationship.copy(goalIdentity = _identity("unknown-goal")))), Vector.empty)

        Then("actual role references are retained and no new view-local Actor or inferred Goal is admitted")
        all(results.map(_.isRight)) shouldBe true
        substituted.isLeft shouldBe true
        fabricated.isLeft shouldBe true
        results.head.toOption.get.operation.command shouldBe commands.head
      }

      "reject stale candidate or artifact versions and a different bounded CCDM context" in {
        Given("one exact session/candidate and operation dependency basis")
        val session = _session()
        val operation = _operation(session, Command.CreateUseCaseSlice(_slice("slice-new")))

        When("the supplied control versions or scope differ")
        val candidatechanged = ModelEditSession.admit(session.copy(candidate = session.candidate.copy(reference = _record("candidate", 2L))), operation, Vector.empty)
        val artifactchanged = ModelEditSession.admit(session.copy(candidate = session.candidate.copy(artifacts = Vector(_artifact(2L)))), operation, Vector.empty)
        val contextchanged = ModelEditSession.admit(session.copy(scope = _scope.copy(context = MonoKotoProjectionContextIdentity("other-context"))), operation, Vector.empty)

        Then("declared exact dependencies cannot silently rebind")
        candidatechanged.left.toOption.get.violations should contain(Violation.CandidateMismatch)
        artifactchanged.left.toOption.get.violations should contain(Violation.ArtifactDependencyMismatch)
        contextchanged.left.toOption.get.violations should contain(Violation.ScopeMismatch)
      }

      "abandon a session without altering its candidate and reject further operations" in {
        Given("an active immutable session with a pending candidate-only operation")
        val session = _session()
        val operation = _operation(session, Command.CreateUseCaseSlice(_slice("slice-new")))

        When("the user abandons the session")
        val abandoned = session.abandon
        val result = ModelEditSession.admit(abandoned, operation, Vector.empty)

        Then("the original candidate remains available as provenance and cannot receive another admitted operation")
        abandoned.candidate shouldBe session.candidate
        session.state shouldBe SessionState.Active
        result.left.toOption.get.violations shouldBe Vector(Violation.SessionAbandoned)
      }

    }
  }

  private val _scope = Scope(MonoKotoProjectionContextIdentity("ccdm-order"), ComponentDashboardComponentIdentity("order-component"))
  private val _attribution = ComponentDashboardSourceAttribution("source-order", "project-order", "source:order")
  private val _owner = RepositoryProjectReference(_id("repository-order"), _id("project-order"))
  private val _condition = ComponentDashboardCondition("available", "admitted", None, None, None, None, None, None, Vector.empty)

  private def _id(value: String): InternalModelRecordId = InternalModelRecordId.from(value).toOption.get
  private def _identity(value: String): ComponentDashboardSemanticTargetIdentity = ComponentDashboardSemanticTargetIdentity(value)
  private def _record(value: String, revision: Long = 1L): InternalModelRecordReference =
    InternalModelRecordReference(_id(value), InternalModelRecordRevision.from(revision).toOption.get)
  private def _artifact(revision: Long = 1L): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from("bounded-source").toOption.get,
      InternalModelArtifactRevision.from(revision).toOption.get, InternalModelArtifactRole.SourceSnapshot)
  private def _slice(identity: String = "slice-1", revision: Long = 1L): UseCaseSlice =
    UseCaseSlice(_record(identity, revision), _identity(identity), _identity("use-case-1"), "Submit order", "Validate and submit",
      Vector(_record("scope-order")), Vector(_record("accept-order")), SliceLifecycle.Proposed, _attribution)
  private def _specification_link(edge: String = "spec-edge-1", specification: String = "spec-1"): SliceSpecificationRelationship =
    SliceSpecificationRelationship(_record(edge), _identity("slice-1"),
      SpecificationReference(_owner, _id(specification), _id("source-revision-1")), _attribution.copy(sourceId = edge))
  private def _ticket_link(edge: String = "ticket-edge-1", ticket: String = "ticket-1"): SliceTicketRelationship =
    SliceTicketRelationship(_record(edge), _identity("slice-1"), TicketRef(_id("tracker"), _owner, _id(ticket)), None,
      _attribution.copy(sourceId = edge))
  private def _resolved(target: ExternalReference): ResolutionEvidence =
    ResolutionEvidence(target, ResolutionState.Resolved, _artifact(), _attribution)
  private def _session(): ModelEditSession = {
    val references = Vector(Actor, Goal).zip(Vector("actor-buyer", "goal-order")).map { (kind, identity) =>
      UseCaseCommunicationReference(identity, _scope.component, _identity(identity), _identity(s"relationship-$identity"),
        kind, _attribution, _condition, None, None)
    }
    val usecase = UseCaseCommunicationProjectionSubject("use-case-1", _scope.component, _identity("use-case-1"),
      _attribution, _condition, references, Vector.empty, None, None)
    ModelEditSession(_record("session"), _scope,
      Candidate(_record("candidate"), _scope, Vector(usecase), Vector(_slice()), Vector.empty, Vector.empty, Vector.empty, Vector(_artifact())),
      SessionState.Active)
  }
  private def _operation(session: ModelEditSession, command: Command, revision: Long = 1L): Operation =
    Operation(_record("operation", revision), session.reference, Preconditions(session.candidate.reference, session.candidate.artifacts), command, _attribution)
}
