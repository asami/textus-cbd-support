/**
 * @since   Oct. 26, 2026
 * @version Oct. 26, 2026
 */
package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import io.circe.Json
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ModelEditSession.*

/** Typed payload closure and actual package/rehydration wiring, without a second store. */
final class ModelEditSessionCodecSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  import ModelEditSpecFixture.*

  "Captured model edit payload" should {
    "round-trip independent Goals, external edges, observations, review findings and exact applied history" in {
      forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { intent =>
        Given("a candidate with Slice, external links, independently attributed Goal and retained historical review")
        val base = session()
        val actor = base.candidate.useCases.head.references.head
        val goal = communicationReference("new-goal", Goal)
        When("candidate edits are applied and the complete closed payload is encoded and decoded")
        val sliced = applyCommand(base, Command.CreateUseCaseSlice(sliceRecord()))
        val spec = specificationLink()
        val ticket = ticketLink()
        val linked = applyCommand(applyCommand(sliced, Command.LinkSliceSpecification(spec),
          Vector(resolved(ExternalReference.Specification(spec.specification)))), Command.LinkSliceTicket(ticket),
          Vector(resolved(ExternalReference.Ticket(ticket.ticket))))
        val added = applyCommand(linked, Command.AddGoal(actor, goal.semanticTargetId, intent, Some(goal)))
        val findings = FindingKind.values.toVector.map(kind => UseCaseReviewFinding(kind, Subject.Goal(goal.semanticTargetId),
          Vector(Subject.UseCase(identity("use-case"))), record("policy"), FindingDisposition(Severity.Warning, false),
          added.candidate.artifacts, RelationshipScopeEvidence.Complete(Subject.Goal(goal.semanticTargetId), artifact())))
        val review = RetainedReview(ApprovalDependency(ValidationDependency(added.reference, added.candidate.reference,
          added.history.map(_.operation.reference), added.candidate.artifacts), record("validation"), record("diff"), record("review")), findings)
        val complete = added.copy(reviews = Vector(review))
        val result = ModelEditSessionCodec.decode(ModelEditSessionCodec.encode(complete))
        Then("all semantic records and consumed evidence remain exact rather than flattened or reconstructed")
        result shouldBe Right(complete)
        result.toOption.get.candidate.ticketLinks.head.observation shouldBe ticket.observation
        result.toOption.get.candidate.goals.head.intent shouldBe intent
        result.toOption.get.reviews.head.findings.map(_.kind) shouldBe FindingKind.values.toVector
      }
    }

    "reject unknown fields, alternatives, missing optional fields, duplicate members and invalid declared references" in {
      Given("a valid complete edit payload")
      val initial = session()
      When("the payload is encoded and each malformed variant is decoded")
      val bytes = ModelEditSessionCodec.encode(initial)
      val json = InternalModelContinuationFixture.json(bytes)
      val sessionjson = json.hcursor.downField("session").focus.get
      val candidatejson = sessionjson.hcursor.downField("candidate").focus.get
      val changed = Vector(
        json.mapObject(_.add("opaque", Json.fromString("unowned"))),
        json.mapObject(_.add("session", sessionjson.mapObject(_.remove("reviews")))),
        json.mapObject(_.add("session", sessionjson.mapObject(_.add("candidate", candidatejson.mapObject(_.add("hidden", Json.True)))))),
        json.mapObject(_.add("session", sessionjson.mapObject(_.add("state", Json.fromString("Unknown"))))),
        json.mapObject(_.add("session", sessionjson.mapObject(_.add("reference", Json.obj("recordId" -> Json.fromString("session"), "recordRevision" -> Json.fromInt(0))))))
      )
      val results = changed.map(value => ModelEditSessionCodec.decode(InternalModelContinuationFixture.canonical(value)))
      val duplicate = "{\"profile\":\"ccdm-model-edit-session-v1\",\"profile\":\"ccdm-model-edit-session-v1\"}".getBytes(StandardCharsets.UTF_8).toVector
      val duplicated = ModelEditSessionCodec.decode(duplicate)
      val invalidutf8 = ModelEditSessionCodec.decode(Vector(0xc3.toByte, 0x28.toByte))
      Then("closed decoding does not silently discard data or select a duplicate value")
      results.foreach(_.isLeft shouldBe true)
      duplicated.isLeft shouldBe true
      invalidutf8.isLeft shouldBe true
    }

    "close every command alternative and reject nested extra fields" in {
      Given("a typed Slice command")
      val command = Command.CreateUseCaseSlice(sliceRecord())
      When("the command is encoded and valid, extended and unknown alternatives are decoded")
      val encoded = ModelEditCommandCodec.encode(command)
      val added = encoded.mapObject { root =>
        root.add("CreateUseCaseSlice", root("CreateUseCaseSlice").get.mapObject(_.add("extra", Json.True)))
      }
      val valid = ModelEditCommandCodec.decode(encoded)
      val invalid = ModelEditCommandCodec.decode(added)
      val unknown = ModelEditCommandCodec.decode(Json.obj("UnknownCommand" -> Json.obj()))
      Then("the command retains its exact proposal and an unknown nested parameter is rejected")
      valid shouldBe Right(command)
      invalid.isLeft shouldBe true
      unknown.isLeft shouldBe true
    }

    "use the real closed package and captured-byte rehydration without changing canonical CML" in {
      Given("a substantive all-projection package with a typed edited candidate in its declared inventory")
      When("the package is prepared, captured and rehydrated while its canonical source is observed")
      ModelEditPackageFixture.withEditPackage { (root, edit, reference) =>
        val canonical = Files.readAllBytes(root.resolve("src/main/cml/main.cml")).toVector
        val captured = InternalModelPackageValidator.verifiedContinuation(root)
        val restored = captured.flatMap(InternalModelRehydrationValidator.validateVerified)
        val observedcanonical = Files.readAllBytes(root.resolve("src/main/cml/main.cml")).toVector
        Then("the exact edited candidate and history are exposed alongside the unchanged continuity and canonical source")
        withClue(restored.show) { restored.isSuccess shouldBe true }
        restored.toOption.get.editSessions shouldBe Vector(edit)
        restored.toOption.get.cursor.selectedArtifacts should contain(reference)
        restored.toOption.get.artifacts.find(_.context.reference == reference).get.context.dependencies shouldBe edit.candidate.artifacts
        observedcanonical shouldBe canonical
        When("the source package is removed and the retained capture is rehydrated again")
        InternalModelContinuationFixture.removeTree(root)
        val afterremoval = InternalModelRehydrationValidator.validateVerified(captured.toOption.get)
        Then("the captured bytes still restore the exact edited candidate without reopening its source")
        afterremoval.toOption.get.editSessions shouldBe Vector(edit)
      }
    }

    "require selection and exact inventory dependencies for the entire typed edit payload" in {
      Given("one valid captured edit session")
      When("the package is captured and the cursor omits the edit payload or its inventory declares different dependencies")
      ModelEditPackageFixture.withEditPackage { (root, edit, reference) =>
        val captured = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
        val resume = captured.artifacts.find(_.context.reference.role == InternalModelArtifactRole.Resume).get
        val cursor = InternalModelResumeCursorCodec.decode(resume.bytes.get).toOption.get
        val omitted = resume.copy(bytes = Some(InternalModelResumeCursorCodec.encode(cursor.copy(selectedArtifacts = cursor.selectedArtifacts.filterNot(_ == reference)))))
        val changed = InternalModelContinuationFixture.withCapturedInventory(captured,
          captured.artifacts.map(artifact => if (artifact.context.reference == resume.context.reference) omitted else artifact))
        val unselected = InternalModelRehydrationValidator.validateVerified(changed)
        val projection = InternalModelVerifiedProjection(reference, "projections/edit.json", true, Vector.empty, ModelEditSessionCodec.encode(edit))
        val incomplete = ModelEditSessionCodec.decodeCaptured(projection)
        Then("neither unselected edit data nor an incomplete artifact dependency basis is admitted")
        unselected.isSuccess shouldBe false
        incomplete.isLeft shouldBe true
      }
    }

    "include every edit payload and dependency in the existing complete review subject with exact declared revisions" in {
      Given("a complete inventoried edit projection outside any previously stored review subject")
      When("the package is captured, its semantic subject is closed and the producer declares a new edit revision")
      ModelEditPackageFixture.withEditPackage { (root, edit, reference) =>
        val captured = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
        val review = artifact("review-selected", 1, InternalModelArtifactRole.Validation)
        val subject = InternalModelCandidateReviewBindingValidator.completeSubject(captured.packageContext, Vector.empty, review).toOption.get
        val revised = reference.copy(artifactRevision = InternalModelArtifactRevision.from(9).toOption.get)
        val carrier = captured.packageContext.copy(artifacts = captured.packageContext.artifacts.map(entry =>
          if (entry.reference == reference) entry.copy(reference = revised) else entry))
        val successor = InternalModelCandidateReviewBindingValidator.completeSubject(carrier, Vector.empty, review).toOption.get
        Then("the edit and all its dependencies are mandatory and an earlier exact subject cannot equal the successor")
        subject should contain(reference)
        edit.candidate.artifacts.foreach(reference => subject should contain(reference))
        successor should contain(revised)
        successor should not contain reference
        successor should not be subject
      }
    }
  }
}

private[runtime] object ModelEditPackageFixture {
  def withEditPackage(body: (Path, ModelEditSession, InternalModelArtifactReference) => Unit): Unit =
    InternalModelContinuationFixture.withFixture() { root =>
      val fixture = InternalModelContinuationFixture
      val continuity = InternalModelProjectionContinuityValidator.validate(root).toOption.get
      val scope = Scope(MonoKotoProjectionContextIdentity(continuity.realization.scope.projectionContextIdentity),
        ComponentDashboardComponentIdentity(continuity.realization.scope.componentIdentity))
      val base = ModelEditSpecFixture.session().copy(scope = scope,
        candidate = ModelEditSpecFixture.session().candidate.copy(scope = scope,
          useCases = continuity.useCaseCommunication.subjects.map(_.sourceSubject), artifacts = Vector(fixture.snapshotReference)))
      val slice = ModelEditSpecFixture.sliceRecord().copy(useCaseIdentity = ComponentDashboardSemanticTargetIdentity("e-usecase"))
      val edit = ModelEditSpecFixture.applyCommand(base, Command.CreateUseCaseSlice(slice))
      val reference = fixture.reference("edit-session", 5, InternalModelArtifactRole.Projection)
      val entry = fixture.artifact("edit-session", 5, "projections/edit.json", "projection", edit.candidate.artifacts)
      fixture.replaceInventory(root, fixture.inventory(root) :+ entry)
      fixture.write(fixture.packageRoot(root).resolve("projections/edit.json"), ModelEditSessionCodec.encode(edit))
      val cursor = fixture.cursor(root)
      fixture.replaceArtifact(root, "resume-main", InternalModelResumeCursorCodec.encode(cursor.copy(
        selectedArtifacts = (cursor.selectedArtifacts :+ reference).sortBy(_.artifactId.value))))
      body(root, edit, reference)
    }
}
