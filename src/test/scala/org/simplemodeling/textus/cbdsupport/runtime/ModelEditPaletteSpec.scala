package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*
import ModelEditPalette.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditPaletteSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  import ModelEditSpecFixture.authorization
  "Contextual Update Palette" should {
    "offer exact subject operations only within the caller's admitted authorization" in {
      forAll(Gen.someOf(CommandKind.values.toVector)) { permitted =>
        Given("a candidate with independent Goal, Slice, Specification and Ticket identities")
        val publication = proposedPublication()
        When("its Slice Palette is inspected with an arbitrary explicitly permitted subset")
        val current = session(publication)
        val subject = Subject.Slice(current.candidate.slices.head.identity)
        val grant = authorization(current).copy(permitted = permitted.toVector)
        val palette = ModelEditPalette.inspect(current, Context(Surface.UseCaseDetail, current.scope, subject), grant)
        val kinds = Vector(CommandKind.RefineUseCaseSlice, CommandKind.LinkSliceSpecification, CommandKind.UnlinkSliceSpecification,
          CommandKind.LinkSliceTicket, CommandKind.UnlinkSliceTicket)
        val expected = CommandKind.values.toVector.filter(kind => kinds.contains(kind) && permitted.contains(kind))
        Then("permission filtering and ordering retain exact candidate and scope without inventing a role policy")
        palette.toOption.get.entries.map(_.kind) shouldBe expected
        palette.toOption.get.candidate shouldBe current.candidate.reference
        palette.toOption.get.authorization shouldBe grant.reference
        palette.toOption.get.context.subject shouldBe subject
      }
    }

    "derive Mono-Koto and Event Storming contexts from actual exact projected identities" in {
      Given("the same admitted Use Case in both typed projections")
      val publication = proposedPublication()
      When("each projection selects that exact Use Case, an unknown identity and a different candidate")
      val current = session(publication)
      val subject = Subject.UseCase(current.candidate.useCases.head.semanticTargetId)
      val mono = ModelEditPaletteSpecFixture.monoKoto(current)
      val storming = EventStormingProjection.project(current, selection(publication)).toOption.get
      val monocontext = ModelEditPalette.monoKotoContext(current, mono, subject)
      val eventcontext = ModelEditPalette.eventStormingContext(current, storming, subject)
      val monomissing = ModelEditPalette.monoKotoContext(current, mono, Subject.UseCase(identity("same-label-different-identity")))
      val eventstale = ModelEditPalette.eventStormingContext(current, storming.copy(candidate = record("candidate", 9)), subject)
      val palettes = Vector(monocontext, eventcontext).map(_.flatMap(value => ModelEditPalette.inspect(current, value, authorization(current))))
      Then("surfaces share applicable semantic operations but cannot repair a mismatched identity or scope")
      monocontext.toOption.get.surface shouldBe Surface.MonoKoto
      eventcontext.toOption.get.surface shouldBe Surface.EventStorming
      palettes.head.toOption.get.entries shouldBe palettes.last.toOption.get.entries
      palettes.head.toOption.get.entries.map(_.kind) should contain(CommandKind.CreateUseCaseSlice)
      monomissing shouldBe Left(Failure.ProjectionMismatch)
      eventstale shouldBe Left(Failure.ProjectionMismatch)
    }

    "reject inactive, out-of-scope and unknown subjects while exposing only exact relationship removal" in {
      Given("an active candidate and an exact Specification relationship")
      val publication = proposedPublication()
      When("the Palette is queried for the exact edge and bounded invalid contexts")
      val current = session(publication)
      val edge = current.candidate.specificationLinks.head.reference
      val context = Context(Surface.EventStorming, current.scope, Subject.Relationship(edge))
      val grant = authorization(current)
      val exact = ModelEditPalette.inspect(current, context, grant)
      val stale = ModelEditPalette.inspect(current, context.copy(subject = Subject.Relationship(edge.copy(recordRevision = record("edge", 2).recordRevision))), grant)
      val abandoned = ModelEditPalette.inspect(current.abandon, context, grant)
      val outside = ModelEditPalette.inspect(current, context.copy(scope = current.scope.copy(component = ComponentDashboardComponentIdentity("other"))), grant)
      val unauthorized = ModelEditPalette.inspect(current, context, grant.copy(session = record("another-session")))
      Then("inspection cannot choose a nearby edge, acquire authority or re-open an abandoned session")
      exact.toOption.get.entries.map(_.kind) shouldBe Vector(CommandKind.UnlinkSliceSpecification)
      stale.left.toOption.get shouldBe a[Failure.UnknownSubject]
      abandoned shouldBe Left(Failure.SessionInactive)
      outside shouldBe Left(Failure.ScopeMismatch)
      unauthorized shouldBe Left(Failure.AuthorizationMismatch)
    }

    "render applicable inputs and escaped Goal and Slice confirmation without asserting approval" in {
      Given("candidate presentation text that includes markup characters")
      val publication = proposedPublication()
      When("the contextual Palette and shared candidate are rendered")
      val imported = session(publication)
      val current = imported.copy(candidate = imported.candidate.copy(
        goals = imported.candidate.goals.map(_.copy(intent = "<Goal & intent>")),
        slices = imported.candidate.slices.map(_.copy(title = "<Slice & title>"))))
      val palette = ModelEditPalette.inspect(current, Context(Surface.UseCaseDetail, current.scope,
        Subject.UseCase(current.candidate.useCases.head.semanticTargetId)), authorization(current)).toOption.get
      val view = ModelEditViewProjection.project(current, selection(publication), Map.empty).toOption.get
      val controls = ModelEditWebView.palette(palette)
      val confirmation = ModelEditWebView.confirmation(view)
      Then("presentation preserves exact external links and displays text as text")
      controls should include("Update Palette")
      controls should include("Link ticket reference")
      controls should include("Exact provider, owner and ticket identity")
      confirmation should include("&lt;Goal &amp; intent&gt;")
      confirmation should include("&lt;Slice &amp; title&gt;")
      confirmation should not include "<Goal & intent>"
      confirmation should include("Specification")
      confirmation should include("Ticket reference")
      confirmation should include("Canonical source")
    }
  }
}

private[runtime] object ModelEditPaletteSpecFixture {
  def monoKoto(session: ModelEditSession): MonoKotoProjection = {
    val usecase = session.candidate.useCases.head
    val subject = MonoKotoProjectionSubject("explicit-use-case-koto", session.scope.component, usecase.semanticTargetId,
      Koto, "Use Case context", usecase.attribution, usecase.condition, Vector.empty, None, None)
    MonoKotoProjection.create(session.scope.context, session.scope.component, Vector(subject)).toOption.get
  }
}
