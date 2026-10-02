package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, LinkOption, Path}
import java.util.concurrent.{CompletableFuture, Executors, TimeUnit}
import io.circe.Json
import org.scalatest.matchers.should.Matchers

/**
 * Same-JVM real application and owner validation around parent-executed Cozy commands.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  3, 2026
 */
object InternalModelCmlChangeAcceptanceProbe extends Matchers {
  import InternalModelCmlChangePostValidation.*
  private val _support = InternalModelCmlChangeAcceptanceSupport

  def main(args: Array[String]): Unit = {
    require(args.length == 2, "probe requires exact absolute fresh workdir and success|lint-failure")
    val workdir = Path.of(args(0))
    val mode = args(1)
    require(Set("success", "lint-failure").contains(mode), "probe mode is unsupported")
    val processid = ProcessHandle.current().pid()
    require(processid > 0, "actual JVM process identity must be positive")
    _support.withFixture(mode) { temporary =>
      val fixture = _support.copyForProbe(temporary, workdir)
      val applicationreference = InternalModelCandidateHumanApprovalValidatorSpec.recordReference(
        "cml-application-C-" + mode, if (mode == "success") 601L else 607L)
      val postreference = InternalModelCandidateHumanApprovalValidatorSpec.recordReference(
        "cml-post-validation-C-" + mode, if (mode == "success") 619L else 631L)
      // This complete actual result remains in this JVM throughout external execution.
      val application = InternalModelCmlChangeApplication.applyApproved(fixture.root, fixture.request, applicationreference).toOption.get
      application.disposition shouldBe InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation
      val targets = application.gate.plan.get.targets
      val commandreferences = Vector(InternalModelCandidateHumanApprovalValidatorSpec.recordReference("cozy-alpha", 613L),
        InternalModelCandidateHumanApprovalValidatorSpec.recordReference("cozy-beta", 617L))
      val ready = Json.obj("schema" -> Json.fromString("textus.cml-change-probe-ready.v1"),
        "processId" -> Json.fromLong(processid), "workdir" -> Json.fromString(workdir.toString),
        "projectRoot" -> Json.fromString(fixture.root.toString), "applicationReference" -> _support.recordJson(applicationreference),
        "mode" -> Json.fromString(mode), "targets" -> Json.fromValues(targets.zip(commandreferences).map { case (target, reference) =>
          Json.obj("targetId" -> Json.fromString(target.target.targetId), "projectRelativePath" -> Json.fromString(target.target.projectRelativePath),
            "absolutePath" -> Json.fromString(fixture.root.resolve(target.target.projectRelativePath).toString),
            "sourceAuthority" -> Json.fromString(target.target.source.authority), "sourceIdentity" -> Json.fromString(target.target.source.identity),
            "sourceRevision" -> Json.fromString(target.nextsourcerevision), "commandReference" -> _support.recordJson(reference))
        }))
      _support.writeOnce(workdir.resolve("ready.json"), ready)
      val observationpaths = Vector(workdir.resolve("alpha-cozy-observation.json"), workdir.resolve("beta-cozy-observation.json"))
      _await_observations_blocking(observationpaths)
      val commands = observationpaths.map(path => _support.readObservation(path).fold(reason => throw new IllegalArgumentException(reason), identity))
      commands.map(_.commandreference) shouldBe commandreferences
      val actualwrites = targets.forall(target => Files.readAllBytes(fixture.root.resolve(target.target.projectRelativePath)).toVector == target.payload.proposedRawBytes)
      val refreshed = _support.refreshOwner(fixture, application)
      val result = InternalModelCmlChangePostValidation.validate(fixture.root, Request(application, postreference, commands, Some(refreshed.owner)))
      val report = result.toOption.get
      val exits = commands.map(_.outcome match {
        case CommandOutcome.Terminal(exitcode, _, _) => exitcode
        case _ => throw new IllegalStateException("probe requires actual terminal Cozy observations")
      })
      val counts = report.continuity.map(_support.projectionCounts).getOrElse(Vector.empty)
      val retained = report.continuity.exists(_retained)
      val cursorunchanged = Files.readAllBytes(fixture.root.resolve("src/main/internal-model/resume.yaml")).toVector == refreshed.resumebytes
      val output = Json.obj("schema" -> Json.fromString("textus.cml-change-probe-result.v1"), "mode" -> Json.fromString(mode),
        "processId" -> Json.fromLong(processid), "applicationReference" -> _support.recordJson(applicationreference),
        "postValidationReference" -> _support.recordJson(postreference), "disposition" -> Json.fromString(report.disposition.toString),
        "commandExitCodes" -> Json.fromValues(exits.map(Json.fromInt)), "requiredSourcesUnchanged" -> Json.fromBoolean(_support.requiredUnchanged(report)),
        "projectionFamilies" -> Json.fromValues(report.continuity.toVector.flatMap(_.binding.views.map(_.family)).map(Json.fromString)),
        "nonemptyFamilies" -> Json.fromInt(counts.count(_ > 0)), "opaqueLaneAndConditionsRetained" -> Json.fromBoolean(retained),
        "cursorUnchanged" -> Json.fromBoolean(cursorunchanged))
      println(output.noSpaces)
      actualwrites shouldBe true
      cursorunchanged shouldBe true
      report.application shouldBe application
      report.commands shouldBe commands
      report.postvalidationreference shouldBe postreference
      if (mode == "success") {
        exits shouldBe Vector(0, 0)
        report.disposition shouldBe Disposition.ReprojectedPendingAcceptance
        report.problems shouldBe empty
        _support.requiredUnchanged(report) shouldBe true
        report.continuity.get.binding.views.map(_.family) shouldBe _support.families
        counts.size shouldBe 8
        counts.forall(_ > 0) shouldBe true
        retained shouldBe true
        report.continuity.get.realization.realizationReference shouldBe refreshed.owner.realizationreference
        report.continuity.get.binding.bindingReference shouldBe refreshed.owner.continuityreference
        refreshed.owner.targetbaselines.foreach(baseline => report.freshness.get.entries.map(_.reference) should contain(baseline.snapshotreference))
      } else {
        exits shouldBe Vector(1, 0)
        report.findings.exists(finding => finding.level == "FAIL" && finding.code == "cml.domain.string-attribute") shouldBe true
        report.disposition shouldBe Disposition.Failed
        report.problems.map(_.kind) should contain(ProblemKind.CommandFailed)
        report.continuity shouldBe None
      }
    }
  }

  /** Each scheduled existence check is nonblocking; the single coordination wait is bounded. */
  private def _await_observations_blocking(paths: Vector[Path]): Unit = {
    val ready = new CompletableFuture[Unit]()
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val check = new Runnable {
      override def run(): Unit = {
        if (paths.forall(path => Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))) ready.complete(())
        ()
      }
    }
    val polling = scheduler.scheduleAtFixedRate(check, 0L, 100L, TimeUnit.MILLISECONDS)
    try ready.get(1800L, TimeUnit.SECONDS)
    finally {
      polling.cancel(false)
      scheduler.shutdown()
    }
  }

  private def _retained(continuity: InternalModelProjectionContinuity): Boolean = {
    val realization = continuity.realization
    val element = realization.elements.find(_.identity == "opaque-shared")
    val relationship = realization.relationships.find(_.identity == "opaque-shared")
    element.exists(value => value.canonicalAssertionIds == Vector("a-opaque-element-kind-Mono") &&
      value.enrichmentAssertionIds == Vector("z-opaque-element-enrichment") && value.conditionIds == Vector("c-opaque-element")) &&
      relationship.exists(value => value.canonicalAssertionIds == Vector("a-opaque-relationship-role-StructuralDomain") &&
        value.enrichmentAssertionIds == Vector("z-opaque-relationship-enrichment") && value.conditionIds == Vector("c-opaque-relationship")) &&
      realization.conditions.exists(value => value.conditionId == "c-opaque-element" && value.kind == "limitation" &&
        value.affectedKind == "element" && value.affectedIdentity == "opaque-shared" && value.detail == "element evidence remains bounded") &&
      realization.conditions.exists(value => value.conditionId == "c-opaque-relationship" && value.kind == "limitation" &&
        value.affectedKind == "relationship" && value.affectedIdentity == "opaque-shared" && value.detail == "relationship evidence remains bounded") &&
      realization.canonicalAssertions.exists(value => value.assertionId == "post-a-r-usecase-step-sequence-key-step-1" && value.content == "sequence-key:step-1") &&
      realization.canonicalAssertions.exists(value => value.assertionId == "post-a-r-workflow-flow-sequence-key-flow-1" && value.content == "sequence-key:flow-1") &&
      realization.canonicalAssertions.exists(value => value.association.exists(association => association.associationRole == "owner" &&
        association.relatedSemanticIdentity == "post-r-structure" && association.relatedSemanticIdentityKind == "relationship")) &&
      realization.conditions.exists(value => value.conditionId == "post-c-workflow-conflict" && value.kind == "conflict" && value.detail == "recorded conflict") &&
      continuity.workflow.flows.exists(_.sourceFlow.condition.conflict.contains("recorded conflict")) &&
      continuity.entityModel.subjects.exists(_.navigationTarget.isEmpty)
  }
}
