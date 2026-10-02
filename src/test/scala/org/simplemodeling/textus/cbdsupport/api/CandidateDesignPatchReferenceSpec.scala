package org.simplemodeling.textus.cbdsupport.api

import scala.compiletime.testing.typeCheckErrors

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import org.simplemodeling.textus.cbdsupport.runtime.{CandidateDesignProposedCmlPatchTrace, ComponentDashboardComponentIdentity, ComponentDashboardCondition, ComponentDashboardSourceAttribution, InternalModelArtifactId, InternalModelArtifactReference, InternalModelArtifactRevision, InternalModelArtifactRole, InternalModelRecordId, InternalModelRecordReference, InternalModelRecordRevision, MonoKotoProjectionContextIdentity}

/*
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
final class CandidateDesignPatchReferenceSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  "An external caller of the shared proposed-CML patch trace" should {
    "construct public references and retain independently supplied IDs, Long revisions and metadata" in {
      forAll(Gen.oneOf(1L, Long.MaxValue, 17L), Gen.oneOf(1L, Long.MaxValue, 31L), Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)) { (artifactrevision, recordrevision, suffix) =>
        Given("public source-snapshot and proposed-content domains supplied by an external caller")
        val baseline = InternalModelArtifactReference(
          InternalModelArtifactId.from("baseline-" + suffix).toOption.get,
          InternalModelArtifactRevision.from(artifactrevision).toOption.get,
          InternalModelArtifactRole.SourceSnapshot
        )
        val content = InternalModelRecordReference(
          InternalModelRecordId.from("proposed-content-漢-" + suffix).toOption.get,
          InternalModelRecordRevision.from(recordrevision).toOption.get
        )
        val attribution = ComponentDashboardSourceAttribution("source-owner", "declared-scope", "evidence/patch")
        val condition = ComponentDashboardCondition("available", "authorized", None, None, Some("bounded ambiguity"), None, None, None, Vector("condition one", "condition one"))
        When("the caller constructs the public shared patch using its canonical named parameters")
        val patch = CandidateDesignProposedCmlPatchTrace(
          id = "patch-" + suffix,
          context = MonoKotoProjectionContextIdentity("context"),
          component = ComponentDashboardComponentIdentity("component"),
          cmlOwner = "source-owner",
          cmlLocator = "cml/supplied-locator",
          baselineArtifactReference = baseline,
          proposedContentReference = content,
          attribution = attribution,
          condition = condition,
          limitations = Vector("first", "first", "第二"),
          stableTieKey = Some("independent-tie")
        )
        Then("the patch exposes the same distinct references and retains every supplied metadata value")
        patch.baselineArtifactReference shouldBe baseline
        patch.proposedContentReference shouldBe content
        patch.baselineArtifactReference.artifactId.value shouldBe "baseline-" + suffix
        patch.baselineArtifactReference.artifactRevision.value shouldBe artifactrevision
        patch.proposedContentReference.recordId.value shouldBe "proposed-content-漢-" + suffix
        patch.proposedContentReference.recordRevision.value shouldBe recordrevision
        patch.id shouldBe "patch-" + suffix
        patch.context.value shouldBe "context"
        patch.component.value shouldBe "component"
        patch.cmlOwner shouldBe "source-owner"
        patch.cmlLocator shouldBe "cml/supplied-locator"
        patch.attribution shouldBe attribution
        patch.condition shouldBe condition
        patch.limitations shouldBe Vector("first", "first", "第二")
        patch.stableTieKey shouldBe Some("independent-tie")
      }
    }

    "admit only producer-declared valid IDs and positive revisions through the public factories" in {
      Given("invalid artifact IDs, blank logical IDs and nonpositive values in the two revision domains")
      val artifactids = Vector(null, "", "bad id", "漢")
      val recordids = Vector(null, "", " ")
      val revisions = Vector(0L, -1L, Long.MinValue)
      When("the external caller asks the existing public factories to admit those values")
      val artifactresults = artifactids.map(InternalModelArtifactId.from)
      val recordresults = recordids.map(InternalModelRecordId.from)
      val artifactrevisionresults = revisions.map(InternalModelArtifactRevision.from)
      val recordrevisionresults = revisions.map(InternalModelRecordRevision.from)
      Then("each invalid domain value is an explicit failure without a generated identity or default revision")
      artifactresults.foreach(_.isLeft shouldBe true)
      recordresults.foreach(_.isLeft shouldBe true)
      artifactrevisionresults.foreach(_.isLeft shouldBe true)
      recordrevisionresults.foreach(_.isLeft shouldBe true)
    }

    "reject raw scalar and cross-domain substitutions at the external compile-time boundary" in {
      Given("external source expressions that substitute raw scalars or another identity, revision or reference domain")
      When("Scala typechecks each incompatible external expression")
      val errors = Vector(
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelArtifactId = "raw""""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelRecordId = "raw""""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelArtifactRevision = 1L"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelRecordRevision = 1L"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelArtifactId = InternalModelRecordId.from("logical").toOption.get"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelRecordId = InternalModelArtifactId.from("artifact").toOption.get"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelArtifactRevision = InternalModelRecordRevision.from(1L).toOption.get"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelRecordRevision = InternalModelArtifactRevision.from(1L).toOption.get"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelArtifactReference = InternalModelRecordReference(InternalModelRecordId.from("logical").toOption.get, InternalModelRecordRevision.from(1L).toOption.get)"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; val value: InternalModelRecordReference = InternalModelArtifactReference(InternalModelArtifactId.from("artifact").toOption.get, InternalModelArtifactRevision.from(1L).toOption.get, InternalModelArtifactRole.SourceSnapshot)"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; def invalid(patch: CandidateDesignProposedCmlPatchTrace) = patch.copy(baselineArtifactReference = patch.proposedContentReference)"""),
        typeCheckErrors("""import org.simplemodeling.textus.cbdsupport.runtime.*; def invalid(patch: CandidateDesignProposedCmlPatchTrace) = patch.copy(proposedContentReference = patch.baselineArtifactReference)""")
      )
      Then("every substitution has a real compiler error in the external package")
      errors.foreach(_.nonEmpty shouldBe true)
    }
  }
}
