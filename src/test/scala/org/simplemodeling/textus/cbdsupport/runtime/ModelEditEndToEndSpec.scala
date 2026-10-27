package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import ModelEditSession.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Native execution prepares once, then consumes the separately reviewed exact target. */
final class ModelEditEndToEndSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Representative Model Edit integration" should {
    "promote the concrete shared candidate only through separately supplied independent review and exact actual-human input" in {
      Given("the selected delivered producer, declared task root and optional retained target with independent inputs")
      val producerroot = Path.of(sys.props.getOrElse("textus.cbd.modelEditProducerRoot", fail("actual producer selection required")))
      val outputroot = Path.of(sys.props.getOrElse("textus.cbd.modelEditE2eRoot", fail("task-owned output root required")))
      val retainedroot = sys.props.get("textus.cbd.modelEditE2ePreparedRoot").map(Path.of(_))
      val expectedversion = sys.props.getOrElse("textus.cbd.modelEditProducerVersion", fail("qualified producer version required"))
      val expectedrevision = sys.props.getOrElse("textus.cbd.modelEditProducerRevision", fail("qualified producer revision required"))
      When("actual production/read, shared edits, Review/refinement and confirmed source rendering execute or are retained and reobserved")
      val prepared = retainedroot.fold(ModelEditIntegrationFixture.prepare(producerroot, outputroot))(root =>
        ModelEditIntegrationFixture.retained(producerroot, root))
      val requestexists = Files.isRegularFile(prepared.root.resolve("review-request.json"))
      val confirmation = io.circe.parser.parse(Files.readString(prepared.root.resolve("confirmation.json"))).toOption.get
      val missingobserved = confirmation.hcursor.get[Boolean]("missingTicketFindingObserved").toOption.get
      val clearedobserved = confirmation.hcursor.get[Boolean]("missingTicketFindingCleared").toOption.get
      val captured = InternalModelSemanticDiffValidator.validate(prepared.root).toOption.get
      val inputs = ModelEditIntegrationFixture.suppliedInputs(prepared)
      Then("the candidate is exact, reviewable and provisionally linked without claiming a live Ticket or positive unavailable demand")
      prepared.producerEntriesPresent shouldBe true
      prepared.basis.producer.version shouldBe expectedversion
      prepared.basis.producer.revision shouldBe expectedrevision
      prepared.basis.producer.modified shouldBe false
      prepared.reread shouldBe prepared.basis
      prepared.sharedActor shouldBe true
      prepared.session.history.size shouldBe 9
      prepared.session.candidate.slices.head.reference.recordRevision.value shouldBe 2
      prepared.session.history.map(_.operation.command).exists(_.isInstanceOf[Command.UnlinkSliceTicket]) shouldBe true
      missingobserved shouldBe true
      clearedobserved shouldBe true
      prepared.view.actorGoals.candidate shouldBe prepared.view.eventStorming.candidate
      prepared.view.traces.head.specificationLinks.size shouldBe 1
      prepared.view.traces.head.ticketLinks.size shouldBe 1
      prepared.view.capabilityDemands.head.evidence shouldBe RelationshipScopeEvidence.Unresolved(
        Subject.UseCase(prepared.view.traces.head.useCaseIdentity), ResolutionState.Unavailable)
      captured.diff.targets.flatMap(_.entries).map(_.entry.subject).toSet shouldBe ModelEditProjectionSpecFixture.newIds.toSet
      prepared.afterPreparation shouldBe prepared.original
      requestexists shouldBe true
      withClue(inputs.left.toOption) { inputs.isRight shouldBe true }
      When("the independent review and actual Human input pass existing Phase10 admission and canonical application")
      val admitted = inputs.fold(message => fail(message + "; no canonical application executed; whole E2E is not accepted"), identity)
      val executable = Path.of(sys.props.getOrElse("textus.cbd.modelEditCozyExecutable", fail("actual Cozy validation executable required before canonical application")))
      require(executable.isAbsolute && Files.isExecutable(executable), "explicit actual Cozy validation executable must exist before canonical application")
      val applied = ModelEditPromotion.admitAndApply(prepared.root, prepared.confirmation, admitted, ModelEditProjectionSpecFixture.record("phase12-canonical-application"))
      val application = applied.toOption.getOrElse(fail(applied.show))
      val validated = if (application.disposition == InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation)
        Some(ModelEditAppliedIntegration.validate(producerroot, executable, prepared, application)) else None
      val canonical = Files.readString(prepared.root.resolve("models/order.cml"))
      val retainededit = validated.map(value => ModelEditSessionCodec.decode(
        Files.readAllBytes(value.reviewedProject.resolve("src/main/internal-model/projections/edit.json")).toVector).toOption.get)
      Then("actual lint and refreshed source owners validate the applied candidate, preserving its independently reviewed edit history")
      application.disposition shouldBe InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation
      validated.map(_.publication.document) shouldBe Some(prepared.reproduced.document)
      validated.map(_.publication.producer) shouldBe Some(prepared.reproduced.producer)
      validated.map(_.report.application) shouldBe Some(application)
      validated.map(_.report.disposition) shouldBe Some(InternalModelCmlChangePostValidation.Disposition.ReprojectedPendingAcceptance)
      validated.get.report.problems shouldBe empty
      validated.get.report.commands.forall(_.outcome match {
        case InternalModelCmlChangePostValidation.CommandOutcome.Terminal(exitcode, _, _) => exitcode == 0
        case _ => false
      }) shouldBe true
      validated.get.report.continuity.get.binding.views.size shouldBe 8
      retainededit shouldBe Some(prepared.session)
      canonical shouldBe prepared.rendering.text
      validated.get.publication.document.relationships.filter(_.kind == "slice-ticket").map(_.to) shouldBe
        prepared.reproduced.document.relationships.filter(_.kind == "slice-ticket").map(_.to)
    }
  }
}
