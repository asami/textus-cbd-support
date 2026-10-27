/**
 * @since   Oct. 26, 2026
 * @version Oct. 27, 2026
 */
package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ModelEditSession.*

/** Executable specification of immutable, authorized application of the ten commands. */
final class ModelEditServiceSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  import ModelEditSpecFixture.*

  "Independent source Actor selection" should {
    "admit AddGoal without inventing a communication edge in a Goal-empty source basis" in {
      Given("a source-owned Actor and Use Case with no Actor-Goal or Goal-UseCase links")
      val publication = ModelEditProjectionSpecFixture.basisPublication()
      When("the source basis is imported and an explicit Goal is added through normal authorization")
      val initial = ModelEditProjectionSpecFixture.session(publication)
      val reference = ModelEditProjectionSpecFixture.session(ModelEditProjectionSpecFixture.proposedPublication()).candidate.goals.head.reference
      val updated = applyCommand(initial, Command.AddGoal(initial.candidate.actors.head, reference.semanticTargetId, "Independent intent", Some(reference)))
      Then("the explicit Actor remains selectable without a fabricated Use Case relationship")
      updated.candidate.goals.head.actor shouldBe Some(initial.candidate.actors.head)
      updated.candidate.useCases shouldBe initial.candidate.useCases
      updated.candidate.goalLinks shouldBe empty
      updated.history.size shouldBe 1
    }
  }

  "Declared Slice fields" should {
    "preserve explicit fields through creation and refinement and reject malformed fields without a successor" in {
      Given("an existing Use Case and a producer-selected Slice field declaration")
      val initial = session()
      val proposed = sliceRecord().copy(fields = Vector(SliceField("requestId", "string", "1")))
      When("the field is created, refined and an invalid duplicate is submitted")
      val created = applyCommand(initial, Command.CreateUseCaseSlice(proposed))
      val revised = proposed.copy(reference = record("slice", 2), fields = Vector(SliceField("requestId", "string", "1"), SliceField("note", "string", "?")))
      val refined = applyCommand(created, Command.RefineUseCaseSlice(proposed.reference, revised))
      val invalid = revised.copy(reference = record("slice", 3), fields = revised.fields :+ revised.fields.head)
      val rejected = ModelEditService.applyOperation(refined, operationFor(refined, Command.RefineUseCaseSlice(revised.reference, invalid)),
        record("candidate", 4), authorization(refined), Vector.empty)
      Then("the declared shape is typed candidate state and invalid fields cannot partially alter it")
      created.candidate.slices.head.fields shouldBe proposed.fields
      refined.candidate.slices.head.fields shouldBe revised.fields
      rejected.left.toOption.get.violations should contain(Violation.InvalidSliceFields(identity("slice")))
      refined.candidate.slices.head.reference shouldBe revised.reference
    }
  }

  "Candidate edit application" should {
    "apply all ten commands through one immutable service and preserve independent Goal intent" in {
      Given("one admitted Use Case, its Actor and imported Goal, and explicit external source evidence")
      val initial = session()
      val slice = sliceRecord()
      When("a Slice is created and refined with producer-selected versions")
      val created = applyCommand(initial, Command.CreateUseCaseSlice(slice))
      val refinedslice = slice.copy(reference = record("slice", 9), title = "Refined slice", lifecycle = SliceLifecycle.InDevelopment)
      val refined = applyCommand(created, Command.RefineUseCaseSlice(slice.reference, refinedslice))
      Then("identity is stable and only the successor contains the changes")
      initial.candidate.slices shouldBe empty
      created.candidate.slices shouldBe Vector(slice)
      refined.candidate.slices shouldBe Vector(refinedslice)

      Given("separate typed Specification and Ticket edges")
      val specification = specificationLink()
      val ticket = ticketLink()
      When("both edges are linked and then independently unlinked")
      val linked = applyCommand(applyCommand(refined, Command.LinkSliceSpecification(specification),
        Vector(resolved(ExternalReference.Specification(specification.specification)))), Command.LinkSliceTicket(ticket),
        Vector(resolved(ExternalReference.Ticket(ticket.ticket))))
      val unspec = applyCommand(linked, Command.UnlinkSliceSpecification(specification))
      val unlinked = applyCommand(unspec, Command.UnlinkSliceTicket(ticket))
      Then("removing one relationship preserves the other and the original linked candidate")
      linked.candidate.specificationLinks shouldBe Vector(specification)
      linked.candidate.ticketLinks shouldBe Vector(ticket)
      unspec.candidate.ticketLinks shouldBe Vector(ticket)
      unlinked.candidate.specificationLinks shouldBe empty
      unlinked.candidate.ticketLinks shouldBe empty

      Given("a producer-selected Goal reference and its explicit Actor")
      val goal = communicationReference("new-goal", Goal)
      val actor = initial.candidate.useCases.head.references.find(_.referenceKind == Actor).get
      When("the Goal is added, refined, linked and unlinked")
      val added = applyCommand(unlinked, Command.AddGoal(actor, goal.semanticTargetId, "Initial intent", Some(goal)))
      val goalrefined = applyCommand(added, Command.RefineGoal(goal, "Independent refined intent"))
      val edge = GoalUseCaseRelationship(record("goal-edge"), goal.semanticTargetId, identity("use-case"), attribution)
      val goallinked = applyCommand(goalrefined, Command.LinkGoalUseCase(edge))
      val complete = applyCommand(goallinked, Command.UnlinkGoalUseCase(edge))
      val commandkinds = complete.history.map(entry => ModelEditSession.commandKind(entry.operation.command)).toSet
      val roundtrip = ModelEditSessionCodec.decode(ModelEditSessionCodec.encode(complete))
      Then("the Goal survives unlinking with exact Actor attribution, intent and all ten history entries")
      complete.candidate.goals shouldBe Vector(ActorGoal(goal, Some(actor), "Independent refined intent", attribution))
      complete.candidate.goalLinks shouldBe empty
      commandkinds shouldBe CommandKind.values.toSet
      complete.history.size shouldBe 10
      complete.history.head.previousCandidate shouldBe initial.candidate.reference
      complete.history.last.successorCandidate shouldBe complete.candidate.reference
      complete.history.sliding(2).forall(pair => pair.head.successorCandidate == pair.last.previousCandidate) shouldBe true
      roundtrip shouldBe Right(complete)
    }

    "leave the actual canonical CML unchanged during successful editing, failed linking and abandonment" in {
      InternalModelContinuationFixture.withFixture() { root =>
        Given("an existing canonical CML file and an independent provisional candidate")
        val path = root.resolve("src/main/cml/main.cml")
        val initial = session()
        When("the canonical basis is observed and a successful Slice edit, unresolved link and explicit abandonment occur")
        val canonical = java.nio.file.Files.readAllBytes(path).toVector
        val edited = applyCommand(initial, Command.CreateUseCaseSlice(sliceRecord()))
        val failed = ModelEditService.applyOperation(edited, operationFor(edited, Command.LinkSliceTicket(ticketLink())),
          record("candidate", 3), authorization(edited), Vector.empty)
        val abandoned = edited.abandon
        val observedcanonical = java.nio.file.Files.readAllBytes(path).toVector
        Then("the provisional successor survives, failure supplies no successor, and the authoritative source stays exact")
        failed.isLeft shouldBe true
        abandoned.candidate shouldBe edited.candidate
        observedcanonical shouldBe canonical
      }
    }

    "retain explicit independent candidate and operation versions under generated producer choices" in {
      forAll(Gen.choose(2L, 10000L), Gen.choose(1L, 10000L)) { (candidateversion, operationversion) =>
        Given("independently chosen versions and an admitted grant")
        val initial = session()
        val command = Command.CreateUseCaseSlice(sliceRecord())
        val operation = operationFor(initial, command).copy(reference = record("operation-independent", operationversion))
        val successor = record("candidate", candidateversion)
        val grant = authorization(initial)
        When("the command is applied")
        val result = ModelEditService.applyOperation(initial, operation, successor, grant, Vector.empty).toOption.get
        Then("the service retains supplied versions and exact operation rather than allocating either")
        result.candidate.reference shouldBe successor
        result.history.head.operation shouldBe operation
        result.history.head.authorization shouldBe grant
        result.history.head.resolutionEvidence shouldBe empty
        initial.history shouldBe empty
      }
    }

    "reject unavailable and proven dangling endpoints atomically with their distinct typed reasons" in {
      Given("an admitted Use Case, a proposed Slice and a proposed Ticket edge")
      val base = session()
      val edge = ticketLink()
      val target = ExternalReference.Ticket(edge.ticket)
      When("the Slice is created and each bounded unresolved result is supplied")
      val initial = applyCommand(base, Command.CreateUseCaseSlice(sliceRecord()))
      val operation = operationFor(initial, Command.LinkSliceTicket(edge))
      val results = Vector(ResolutionState.Missing, ResolutionState.Unavailable, ResolutionState.Unauthorized,
        ResolutionState.Redacted, ResolutionState.Unsupported).map { state =>
        val result = ModelEditService.applyOperation(initial, operation, record("candidate", 10), authorization(initial),
          Vector(resolved(target).copy(state = state)))
        val expected = if (state == ResolutionState.Missing) Violation.ProvenDangling(target)
          else Violation.ReferenceUnresolved(target, state)
        (result, expected)
      }
      Then("there is no successor or partially linked candidate and each reason remains exact")
      results.foreach { case (result, expected) =>
        result.left.toOption.get.violations should contain(expected)
      }
      initial.candidate.ticketLinks shouldBe empty
      initial.history.size shouldBe 1
    }

    "require caller authorization, an explicit successor and a producer-owned Goal reference" in {
      Given("an otherwise admissible operation and independently scoped authorization")
      val initial = session()
      val command = Command.CreateUseCaseSlice(sliceRecord())
      val operation = operationFor(initial, command)
      When("authorization, successor or required creation identity is absent or mismatched")
      val denied = ModelEditService.applyOperation(initial, operation, record("candidate", 2),
        authorization(initial).copy(permitted = Vector.empty), Vector.empty)
      val wrongsession = ModelEditService.applyOperation(initial, operation, record("candidate", 2),
        authorization(initial).copy(session = record("other-session")), Vector.empty)
      val wrongscope = ModelEditService.applyOperation(initial, operation, record("candidate", 2),
        authorization(initial).copy(scope = scope.copy(component = ComponentDashboardComponentIdentity("other"))), Vector.empty)
      val successor = ModelEditService.applyOperation(initial, operation, record("other-candidate", 2), authorization(initial), Vector.empty)
      val actor = initial.candidate.useCases.head.references.head
      val unallocated = ModelEditService.applyOperation(initial,
        operationFor(initial, Command.AddGoal(actor, identity("new-goal"), "Intent")), record("candidate", 2), authorization(initial), Vector.empty)
      Then("each attempt returns a typed failure and leaves the basis untouched")
      Vector(denied, wrongsession, wrongscope).foreach(_.left.toOption.get.violations should contain(Violation.AuthorizationDenied))
      successor.left.toOption.get.violations should contain(Violation.InvalidSuccessor)
      unallocated.left.toOption.get.violations should contain(Violation.GoalReferenceRequired)
      initial.candidate.slices shouldBe empty
      initial.candidate.goals shouldBe empty
    }

    "refine imported Goal intent without guessing an Actor association from co-occurring references" in {
      Given("an imported Goal and an Actor that happen to be referenced by the same Use Case")
      val initial = session()
      val goal = initial.candidate.useCases.head.references.find(_.referenceKind == Goal).get
      When("the imported Goal is refined through its exact existing communication reference")
      val updated = applyCommand(initial, Command.RefineGoal(goal, "Revised imported intent"))
      val roundtrip = ModelEditSessionCodec.decode(ModelEditSessionCodec.encode(updated))
      Then("the independent intent is retained and an unknown Actor association remains explicit")
      updated.candidate.goals shouldBe Vector(ActorGoal(goal, None, "Revised imported intent", attribution))
      updated.candidate.useCases shouldBe initial.candidate.useCases
      roundtrip shouldBe Right(updated)
    }

    "retain historical reviews while invalidating their applicability after candidate editing or abandonment" in {
      Given("a review dependency naming the exact initial candidate, empty operation history and artifact basis")
      val initial = session()
      val review = RetainedReview(ApprovalDependency(ValidationDependency(initial.reference, initial.candidate.reference,
        Vector.empty, initial.candidate.artifacts), record("validation"), record("diff"), record("review")), Vector.empty)
      val reviewed = initial.copy(reviews = Vector(review))
      When("review applicability is evaluated before and after editing and abandonment, and an abandoned edit is attempted")
      val initialapplicability = ModelEditSession.reviewApplies(reviewed, review)
      val updated = applyCommand(reviewed, Command.CreateUseCaseSlice(sliceRecord()))
      val updatedapplicability = ModelEditSession.reviewApplies(updated, review)
      val abandonedreviewed = reviewed.abandon
      val abandonedapplicability = ModelEditSession.reviewApplies(abandonedreviewed, review)
      val abandonedupdated = updated.abandon
      val rejected = ModelEditService.applyOperation(abandonedupdated, operationFor(updated, Command.UnlinkGoalUseCase(
        GoalUseCaseRelationship(record("unknown-edge"), identity("imported-goal"), identity("use-case"), attribution))),
        record("candidate", 3), authorization(updated), Vector.empty)
      Then("the historical record survives but grants no applicability or human approval to the successor")
      initialapplicability shouldBe true
      updated.reviews shouldBe Vector(review)
      updatedapplicability shouldBe false
      abandonedapplicability shouldBe false
      rejected.left.toOption.get.violations should contain(Violation.SessionAbandoned)
    }
  }
}

/** Shared typed source fixture; no canonical serializer or external resolver is fabricated. */
private[runtime] object ModelEditSpecFixture {
  val scope: Scope = Scope(MonoKotoProjectionContextIdentity("context-edit"), ComponentDashboardComponentIdentity("component-edit"))
  val attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution("source-edit", "authority-edit", "source:edit")
  private val _condition = ComponentDashboardCondition("available", "admitted", None, None, None, None, None, None, Vector.empty)
  def identity(value: String): ComponentDashboardSemanticTargetIdentity = ComponentDashboardSemanticTargetIdentity(value)
  def record(value: String, revision: Long = 1): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(value).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)
  def artifact(value: String = "snapshot-edit", revision: Long = 1, role: InternalModelArtifactRole = InternalModelArtifactRole.SourceSnapshot): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from(value).toOption.get, InternalModelArtifactRevision.from(revision).toOption.get, role)
  def communicationReference(value: String, kind: UseCaseCommunicationReferenceKind): UseCaseCommunicationReference =
    UseCaseCommunicationReference(value, scope.component, identity(value), identity("relationship-" + value), kind, attribution, _condition, None, None)
  def session(): ModelEditSession = {
    val usecase = UseCaseCommunicationProjectionSubject("use-case", scope.component, identity("use-case"), attribution, _condition,
      Vector(communicationReference("actor", Actor), communicationReference("imported-goal", Goal)), Vector.empty, None, None)
    ModelEditSession(record("session"), scope, Candidate(record("candidate"), scope, Vector(usecase), Vector.empty,
      Vector.empty, Vector.empty, Vector.empty, Vector(artifact())), SessionState.Active)
  }
  def sliceRecord(): UseCaseSlice = UseCaseSlice(record("slice"), identity("slice"), identity("use-case"), "Submit order", "Complete order",
    Vector(record("scope")), Vector(record("acceptance")), SliceLifecycle.Proposed, attribution)
  def specificationLink(): SliceSpecificationRelationship = SliceSpecificationRelationship(record("spec-edge"), identity("slice"),
    SpecificationReference(RepositoryProjectReference(record("repository").recordId, record("project").recordId),
      record("specification").recordId, record("source-revision").recordId), attribution)
  def ticketLink(): SliceTicketRelationship = SliceTicketRelationship(record("ticket-edge"), identity("slice"),
    TicketRef(record("tracker").recordId, RepositoryProjectReference(record("repository").recordId, record("project").recordId),
      record("ticket").recordId), Some(TicketObservation(Some(record("open").recordId), Some(record("ticket-version").recordId))), attribution)
  def resolved(target: ExternalReference): ResolutionEvidence = ResolutionEvidence(target, ResolutionState.Resolved, artifact(), attribution)
  def authorization(session: ModelEditSession): Authorization = Authorization(record("grant"), session.reference, session.scope, CommandKind.values.toVector)
  def operationFor(session: ModelEditSession, command: Command): Operation = Operation(record("operation-" + (session.history.size + 1)),
    session.reference, Preconditions(session.candidate.reference, session.candidate.artifacts), command, attribution)
  def applyCommand(session: ModelEditSession, command: Command, resolutions: Vector[ResolutionEvidence] = Vector.empty): ModelEditSession =
    ModelEditService.applyOperation(session, operationFor(session, command),
      record("candidate", session.candidate.reference.recordRevision.value + 1), authorization(session), resolutions).toOption.get
}
