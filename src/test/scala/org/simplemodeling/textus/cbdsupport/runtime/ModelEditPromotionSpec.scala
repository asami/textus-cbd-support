package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditPromotionSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  "Exact Model Edit product confirmation" should {
    "select every accumulated creation and independent trace in the complete review subject" in {
      Given("an explicitly confirmed candidate and a producer-independent ordinary specification fixture")
      When("the accumulated diff and exact product review request are prepared")
      val prepared = _preparation()
      val diff = prepared.diff.diff.targets.flatMap(_.entries)
      val view = ModelEditViewProjection.projectConfirmed(prepared.session, prepared.confirmed, Map.empty).toOption.get
      val html = ModelEditWebView.confirmation(view)
      Then("the semantic diff contains all seven source additions and the review subject includes every edit dependency")
      diff.map(_.entry.subject).toSet shouldBe newIds.toSet
      diff.count(_.semanticIdentityKind == "relationship") shouldBe 5
      prepared.subject.artifacts.map(_.artifactId.value) should contain allOf (
        "model-edit-session", "model-edit-source-mapping", "phase12-existing-specification")
      prepared.reviewRequest.checks should not be empty
      html should include("Event Storming")
      html should include("submit-causes-placed")
      html should include("submit-repeat")
      html should include("repeat-spec")
      html should include("repeat-ticket")
    }
    "reject a stale candidate revision without generating approval or changing the confirmed subject" in {
      forAll(Gen.choose(20L, 100000L)) { revision =>
        Given("one exact confirmed candidate and an unrelated explicitly supplied successor")
        When("confirmation is requested for the stale source mapping")
        val prepared = _preparation()
        val stale = prepared.session.copy(candidate = prepared.session.candidate.copy(reference = record("candidate", revision)))
        val rejected = ModelEditConfirmation.prepare(stale, prepared.confirmed, prepared.mapping, prepared.diff,
          record("product-subject"), ModelEditSpecFixture.artifact("product-review", role = InternalModelArtifactRole.Validation),
          record("product-review"), Vector("candidate.cml"), Vector(ModelEditIntegrationFixture.specificationArtifact))
        Then("the changed candidate cannot reuse the earlier exact confirmation")
        rejected.isLeft shouldBe true
        prepared.session.candidate.reference.recordRevision.value should not be revision
      }
    }
    "reject unrelated fixture approval before any canonical effect" in {
      Given("a confirmed Model Edit target and an independently admitted but unrelated Phase10 fixture approval")
      When("that unrelated approval is presented to the normal promotion connection")
      val prepared = _preparation()
      var rejected = false
      var unchanged = false
      InternalModelCmlChangeFixture.withFixture() { fixture =>
        val before = Files.readAllBytes(fixture.root.resolve("cml/alpha.cml")).toVector
        val result = ModelEditPromotion.applyApproved(fixture.root, prepared, fixture.request, record("model-edit-application"))
        val after = Files.readAllBytes(fixture.root.resolve("cml/alpha.cml")).toVector
        rejected = result.toOption.isEmpty
        unchanged = before == after
      }
      Then("fixture approval cannot authorize this product subject and the canonical file is unchanged")
      rejected shouldBe true
      unchanged shouldBe true
    }
  }

  private def _preparation(): ModelEditConfirmation.Prepared = {
    val basis = basisPublication()
    val proposed = proposedPublication()
    val edited = editedSession(basis, proposed)
    val plan = ModelEditCmlProjection.prepare(selection(basis), edited, successor, proposed.document, selection(proposed).bindings).toOption.get
    val rendering = ModelEditSourceProjection.Rendering(plan.request.schemaVersion, source, producer, successor, "fixture proposed CML", Vector.empty)
    val confirmed = ModelEditCmlProjection.confirm(plan, rendering, proposed).toOption.get
    val phase10 = ModelEditIntegrationFixture.completeDiff(ModelEditPhase10SpecFixture.prepare(confirmed, edited,
      "fixture canonical CML".getBytes(StandardCharsets.UTF_8).toVector).toOption.get, basis, proposed)
    ModelEditConfirmation.prepare(edited, confirmed, phase10.mapping, phase10.diff, record("product-subject"),
      ModelEditSpecFixture.artifact("product-review", role = InternalModelArtifactRole.Validation), record("product-review"),
      Vector("candidate.cml"), Vector(ModelEditIntegrationFixture.specificationArtifact)).toOption.get
  }
}
