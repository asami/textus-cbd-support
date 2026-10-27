package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*
import ModelEditPalette.Surface
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditCrossViewSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  import ModelEditSpecFixture.{authorization, operationFor}

  "One candidate across Mono-Koto and Structure" should {
    "retain stable traces, typed audit and historical review while both contexts use the same service" in {
      forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { title =>
        Given("one candidate and independently admitted host Mono-Koto/Structure projections")
        val fixture = _fixture()
        val current = fixture.current
        val slice = current.candidate.slices.head
        val subject = Subject.UseCase(slice.useCaseIdentity)
        When("Mono-Koto refines the Slice and Structure refines the same stable successor")
        val firstslice = slice.copy(reference = record(slice.reference.recordId.value, 2), title = title)
        val firstoperation = operationFor(current, Command.RefineUseCaseSlice(slice.reference, firstslice))
        val first = _apply(current, fixture.view, Surface.MonoKoto, subject, firstoperation, record("candidate", 2), fixture.source)
        val secondslice = firstslice.copy(reference = record(slice.reference.recordId.value, 3), intent = "Refined through Structure")
        val secondoperation = operationFor(first.session, Command.RefineUseCaseSlice(firstslice.reference, secondslice))
        val second = _apply(first.session, first.view, Surface.Structure, Subject.Slice(slice.identity), secondoperation, record("candidate", 3), fixture.source)
        val stale = ModelEditCrossView.applyOperation(first.session, fixture.view, Surface.Structure, subject,
          secondoperation, record("candidate", 3), authorization(first.session), Vector.empty, fixture.source, Map.empty)
        Then("the shared candidate retains exact independent edges and only current Views can edit it")
        second.session.candidate.slices shouldBe Vector(secondslice)
        second.session.candidate.specificationLinks shouldBe current.candidate.specificationLinks
        second.session.candidate.ticketLinks shouldBe current.candidate.ticketLinks
        second.view.editing.traces.head.slices shouldBe Vector(secondslice)
        second.view.editing.actorGoals.candidate shouldBe second.view.editing.eventStorming.candidate
        second.view.audit shouldBe second.session.history
        second.view.audit.last.authorization shouldBe authorization(first.session)
        second.view.audit.last.operation shouldBe secondoperation
        second.view.audit.last.previousCandidate shouldBe first.session.candidate.reference
        second.view.audit.last.successorCandidate shouldBe second.session.candidate.reference
        fixture.view.reviews.head.applies shouldBe true
        second.view.reviews.head.review shouldBe fixture.view.reviews.head.review
        second.view.reviews.head.applies shouldBe false
        stale shouldBe Left(ModelEditModelMutation.Failure.Palette(ModelEditPalette.Failure.ProjectionMismatch))
        current.candidate.slices shouldBe Vector(slice)
        current.history shouldBe empty
      }
    }

    "retain ordinary authorization, bounded failure and explicit abandonment before a fresh-session restart" in {
      Given("one active cross-view session and a caller-proposed successor")
      val fixture = _fixture()
      val current = fixture.current
      val slice = current.candidate.slices.head
      val refined = slice.copy(reference = record(slice.reference.recordId.value, 2), title = "Fresh attempt")
      val operation = operationFor(current, Command.RefineUseCaseSlice(slice.reference, refined))
      val subject = Subject.Slice(slice.identity)
      When("a restricted caller is denied, the session is abandoned and an independently declared fresh session starts")
      val denied = ModelEditCrossView.applyOperation(current, fixture.view, Surface.Structure, subject, operation,
        record("candidate", 2), authorization(current).copy(permitted = Vector.empty), Vector.empty, fixture.source, Map.empty)
      val abandoned = current.abandon
      val stoppedview = ModelEditCrossView.project(abandoned, fixture.source, fixture.view.monoKoto, fixture.view.structure, Map.empty).toOption.get
      val stopped = ModelEditCrossView.applyOperation(abandoned, stoppedview, Surface.Structure, subject, operation,
        record("candidate", 2), authorization(abandoned), Vector.empty, fixture.source, Map.empty)
      val restarted = ModelEditSession(record("fresh-session"), current.scope,
        ModelEditSourceProjection.importCandidate(fixture.source, record("fresh-candidate"), current.scope).toOption.get, SessionState.Active)
      val restartview = ModelEditCrossView.project(restarted, fixture.source, fixture.view.monoKoto, fixture.view.structure, Map.empty).toOption.get
      val restartoperation = operationFor(restarted, Command.RefineUseCaseSlice(slice.reference, refined))
      val success = _apply(restarted, restartview, Surface.Structure, subject, restartoperation, record("fresh-candidate", 2), fixture.source)
      Then("failure and abandonment grant no mutation and fresh work has its own exact provenance without replay")
      denied shouldBe Left(ModelEditModelMutation.Failure.Palette(ModelEditPalette.Failure.CommandUnavailable(CommandKind.RefineUseCaseSlice)))
      stopped shouldBe Left(ModelEditModelMutation.Failure.Palette(ModelEditPalette.Failure.SessionInactive))
      stoppedview.editing.confirmation.abandoned shouldBe true
      stoppedview.reviews.head.applies shouldBe false
      abandoned.history shouldBe empty
      current.candidate.slices shouldBe Vector(slice)
      success.session.reference shouldBe record("fresh-session")
      success.session.history.map(_.operation.reference) shouldBe Vector(restartoperation.reference)
      success.session.history.head.authorization.session shouldBe restarted.reference
      success.session.reviews shouldBe empty
    }

    "keep a same-titled new Slice unlinked and refuse unsupported source promotion" in {
      Given("a linked Slice and an explicitly distinct new Slice with the same presentation title")
      val fixture = _fixture()
      val current = fixture.current
      val original = current.candidate.slices.head
      val proposed = original.copy(reference = record("other-slice"), identity = identity("other-slice"))
      When("Mono-Koto creates it without copying external relationships and Review inspects complete source scope")
      val operation = operationFor(current, Command.CreateUseCaseSlice(proposed))
      val changed = _apply(current, fixture.view, Surface.MonoKoto, Subject.UseCase(proposed.useCaseIdentity),
        operation, record("candidate", 2), fixture.source)
      val policy = FindingPolicy(record("cross-view-policy"), FindingKind.values.map(kind => kind -> FindingDisposition(Severity.Warning, false)).toMap)
      val reviewed = ModelEditReview.reviewSource(changed.session, policy, fixture.source, Vector.empty, Vector.empty, Vector.empty).toOption.get
      val unsupported = basisPublication().copy(document = basisPublication().document.copy(coverage = basisPublication().document.coverage.map(value =>
        if (value.family == "slice") value.copy(status = "unsupported", detail = Some("no Slice source mapping")) else value)))
      val blocked = ModelEditCmlProjection.prepare(selection(unsupported), changed.session, successor,
        proposedPublication().document, selection(proposedPublication()).bindings)
      Then("names do not create trace edges, and unavailable source meaning cannot pass canonical planning")
      proposed.title shouldBe original.title
      changed.session.candidate.specificationLinks shouldBe current.candidate.specificationLinks
      changed.session.candidate.ticketLinks shouldBe current.candidate.ticketLinks
      reviewed.findings.filter(_.subject == Subject.Slice(proposed.identity)).map(_.kind).toSet should contain allOf (
        FindingKind.SliceWithoutExecutableSpecification, FindingKind.SliceWithoutTicket)
      blocked.isLeft shouldBe true
      changed.view.editing.capabilityDemands.head.evidence shouldBe RelationshipScopeEvidence.Unresolved(
        Subject.UseCase(proposed.useCaseIdentity), ResolutionState.Unavailable)
    }

    "invalidate an old exact approval dependency when a cross-view trace edge changes" in {
      Given("one confirmed candidate, its exact source mapping and an old complete product subject")
      val basis = basisPublication()
      val publication = proposedPublication()
      When("source confirmation captures that subject and Structure subsequently removes exactly the Ticket edge")
      val edited = editedSession(basis, publication)
      val plan = ModelEditCmlProjection.prepare(selection(basis), edited, successor, publication.document, selection(publication).bindings).toOption.get
      val rendering = ModelEditSourceProjection.Rendering(plan.request.schemaVersion, source, producer, successor, "fixture proposed CML", Vector.empty)
      val confirmed = ModelEditCmlProjection.confirm(plan, rendering, publication).toOption.get
      val prepared = ModelEditPhase10SpecFixture.prepare(confirmed, edited, "fixture baseline".getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector).toOption.get
      val projections = _projections(edited, selection(publication))
      val view = ModelEditCrossView.project(edited, selection(basis), projections._1, projections._2, Map.empty).toOption.get
      val command = Command.UnlinkSliceTicket(edited.candidate.ticketLinks.head)
      val changed = _apply(edited, view, Surface.Structure, Subject.Slice(edited.candidate.slices.head.identity), operationFor(edited, command), record("candidate", 20), selection(basis))
      val stale = ModelEditConfirmation.prepare(changed.session, confirmed, prepared.mapping, prepared.diff,
        record("stale-product-subject"), ModelEditSpecFixture.artifact("stale-review", role = InternalModelArtifactRole.Validation),
        record("stale-review"), Vector("candidate.cml"))
      Then("the actual edge change cannot reuse the old source-confirmed approval subject")
      changed.session.candidate.ticketLinks shouldBe empty
      changed.session.candidate.specificationLinks shouldBe edited.candidate.specificationLinks
      changed.session.history.last.operation.command shouldBe command
      stale.isLeft shouldBe true
    }
  }

  private final case class Fixture(current: ModelEditSession, source: ModelEditSourceProjection.Selection, view: ModelEditCrossView.View)
  private def _fixture(): Fixture = {
    val selected = selection(proposedPublication())
    val initial = session(selected.publication)
    val review = RetainedReview(ApprovalDependency(ValidationDependency(initial.reference, initial.candidate.reference,
      Vector.empty, initial.candidate.artifacts), record("validation"), record("diff"), record("review")), Vector.empty)
    val current = initial.copy(reviews = Vector(review))
    val projections = _projections(current, selected)
    Fixture(current, selected, ModelEditCrossView.project(current, selected, projections._1, projections._2, Map.empty).toOption.get)
  }
  private def _projections(current: ModelEditSession, selected: ModelEditSourceProjection.Selection): (MonoKotoProjection, StructureViewProjection) = {
    val slice = current.candidate.slices.head
    val edge = selected.publication.document.relationships.find(_.kind == "use-case-slice").get
    val relation = StructureViewRelation("explicit-slice-containment", current.scope.component,
      selected.binding(edge.ref).toOption.get.semanticIdentity, slice.useCaseIdentity, slice.identity,
      StructureContainment, slice.attribution, selected.condition, Vector.empty, None, None)
    (ModelEditPaletteSpecFixture.monoKoto(current), StructureViewProjection.create(current.scope.context,
      current.scope.component, Vector(relation), Vector.empty).toOption.get)
  }
  private def _apply(current: ModelEditSession, view: ModelEditCrossView.View, surface: Surface, subject: Subject,
    operation: Operation, successor: InternalModelRecordReference, selected: ModelEditSourceProjection.Selection): ModelEditCrossView.Applied =
    ModelEditCrossView.applyOperation(current, view, surface, subject, operation, successor, authorization(current), Vector.empty, selected, Map.empty)
      .fold(error => fail(error.toString), value => value)
}
