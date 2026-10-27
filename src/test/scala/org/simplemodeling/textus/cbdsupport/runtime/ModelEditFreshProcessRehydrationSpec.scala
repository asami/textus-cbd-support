package org.simplemodeling.textus.cbdsupport.runtime

import java.io.File
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import io.circe.Json
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** One native-selected fresh JVM reads the actual accepted edit and canonical carriers. */
final class ModelEditFreshProcessRehydrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private final case class ChildEvidence(report: Json, pid: Long, exitcode: Int, terminated: Boolean, stderr: String)

  "Actual Model Edit continuation after approved promotion" should {
    "preserve the complete historical handoff and exact promoted source meaning in a distinct read-only JVM" in {
      Given("the explicitly selected accepted Stage12.7 root and its independently supplied actual review/Human input")
      val root = Path.of(sys.props.getOrElse("textus.cbd.modelEditE2ePreparedRoot", fail("actual accepted Stage12.7 root is required"))).toAbsolutePath.normalize()
      When("existing owners admit the original cursor and a fresh JVM independently reopens both actual carriers")
      val before = InternalModelContinuationFixture.treeBytes(root)
      val request = _request(root)
      val expected = ModelEditFreshProcessProbe.observe(root, request)
      val workroot = Path.of("target/model-edit-continuation").toAbsolutePath.normalize()
      Files.createDirectories(workroot)
      val work = Files.createTempDirectory(workroot, "fresh-")
      val input = work.resolve("independent-request.json")
      Files.write(input, InternalModelDurableHandoffInput.encode(request).toArray)
      val child = _run_probe(root, input, work)
      val after = InternalModelContinuationFixture.treeBytes(root)
      val historical = expected.reading.historical
      val session = historical.state.editSessions.head
      val canonical = expected.reading.canonical
      val review = historical.review.get.binding
      val actualhuman = InternalModelCandidateHumanApprovalCodec.decode(Files.readAllBytes(root.resolve("admitted-inputs/human-input.json")).toVector).toOption.get.approval
      val currentresume = InternalModelRehydrationValidator.validate(root)
      val specificationhistory = session.history.flatMap(_.operation.command match {
        case ModelEditSession.Command.LinkSliceSpecification(relationship) => Some(relationship)
        case _ => None
      })
      val tickethistory = session.history.flatMap(_.operation.command match {
        case ModelEditSession.Command.LinkSliceTicket(relationship) => Some(relationship)
        case _ => None
      })
      Then("the actual child preserves stable identities, independent attribution, product Review and the genuine recorded decision without new source effects")
      withClue(child.stderr) {
        child.terminated shouldBe true
        child.exitcode shouldBe 0
        child.pid should not be ProcessHandle.current().pid()
        child.report.hcursor.get[Long]("pid").toOption shouldBe Some(child.pid)
        child.report.hcursor.get[String]("status").toOption shouldBe Some("success")
        child.report.hcursor.downField("failure").focus shouldBe Some(Json.Null)
        child.report.hcursor.downField("observation").focus shouldBe Some(ModelEditFreshProcessProbe.reportValue(expected))
      }
      historical.eligibility shouldBe InternalModelContinuationEligibility.Eligible
      historical.action shouldBe Some(InternalModelContinuationAction.HandoffApprovedCandidate)
      historical.problems shouldBe empty
      historical.state.packageContext.revision shouldBe 1
      historical.state.cursor.currentStage shouldBe "human-decision-recorded"
      session.candidate.reference shouldBe ModelEditProjectionSpecFixture.record("candidate", 10)
      session.history.size shouldBe 9
      session.candidate.useCases.map(_.semanticTargetId.value) should contain("place-order")
      session.candidate.slices.head.identity.value shouldBe "submit-repeat"
      session.candidate.slices.head.reference.recordRevision.value shouldBe 2
      session.candidate.specificationLinks.head.specification.specificationIdentity.value shouldBe
        "org.simplemodeling.textus.cbdsupport.runtime.ModelEditServiceSpec"
      session.candidate.ticketLinks.head.ticket.ticketIdentity.value shouldBe "I_kwDOOrderIssue42"
      session.candidate.specificationLinks.head.sliceIdentity shouldBe session.candidate.ticketLinks.head.sliceIdentity
      session.candidate.specificationLinks.head.reference should not be session.candidate.ticketLinks.head.reference
      specificationhistory shouldBe session.candidate.specificationLinks
      tickethistory.distinct shouldBe session.candidate.ticketLinks
      session.candidate.specificationLinks.head.attribution.sourceId shouldBe "models/order.cml"
      session.candidate.ticketLinks.head.attribution.sourceId shouldBe "models/order.cml"
      historical.approval.get.record.approval shouldBe actualhuman
      actualhuman.actor.kind shouldBe "human"
      actualhuman.decision shouldBe InternalModelCandidateHumanApprovalDecision.Approved
      review.targets.map(_.reviewSnapshot.state) shouldBe Vector("reviewed-with-limitations")
      review.rules.map(_.ruleId).toSet shouldBe Set("semantic-consistency", "implementation-quality", "excessive-checks", "integration")
      review.subject shouldBe actualhuman.basis.subject
      review.subject.artifacts.map(_.artifactId.value) should contain allOf ("model-edit-session", "model-edit-source-mapping", "phase12-existing-specification")
      expected.publication shouldBe expected.mapping.confirmed.reproduced
      expected.baselinematches shouldBe true
      expected.canonicaltext shouldBe expected.mapping.confirmed.rendering.text
      expected.reading.canonicalpackage.revision shouldBe 2
      canonical.realization.realizationReference should not be historical.state.continuity.realization.realizationReference
      canonical.binding.bindingReference should not be historical.state.continuity.binding.bindingReference
      canonical.realization.elements shouldBe historical.state.continuity.realization.elements
      canonical.realization.relationships shouldBe historical.state.continuity.realization.relationships
      canonical.realization.sourceReferences.map(value => (value.referenceId, value.sourceAnchor, value.source, value.target)) shouldBe
        historical.state.continuity.realization.sourceReferences.map(value => (value.referenceId, value.sourceAnchor, value.source, value.target))
      canonical.binding.views.map(_.family).toSet shouldBe historical.state.continuity.binding.views.map(_.family).toSet
      canonical.binding.views.size shouldBe 8
      session.candidate.artifacts.map(_.artifactRevision.value) shouldBe Vector(1)
      expected.reading.canonicalpackage.artifacts.filter(_.reference.role == InternalModelArtifactRole.SourceSnapshot)
        .map(_.reference.artifactRevision.value).distinct shouldBe Vector(2)
      expected.reading.canonicalhasresume shouldBe false
      currentresume.toOption shouldBe None
      expected.resumediagnostic should include("continuation requires exactly one resume inventory entry")
      expected.postvalidation.hcursor.get[String]("state").toOption shouldBe Some("ReprojectedPendingAcceptance")
      expected.postvalidation.hcursor.get[Vector[Json]]("problems").toOption shouldBe Some(Vector.empty)
      after shouldBe before
    }
  }

  /** Read separately supplied inputs; recorded package claims cannot fill a missing actual Human input. */
  private def _request(root: Path): InternalModelContinuationRequest = {
    val historical = root.resolve("review-materials/approved-candidate")
    val state = InternalModelRehydrationValidator.validate(historical).toOption.get
    val candidate = InternalModelCandidateCmlProjectionValidator.validate(historical).toOption.get
    val review = InternalModelCandidateReviewBindingCodec.decode(Files.readAllBytes(root.resolve("admitted-inputs/review.json")).toVector).toOption.get
    val human = InternalModelCandidateHumanApprovalCodec.decode(Files.readAllBytes(root.resolve("admitted-inputs/human-input.json")).toVector).toOption.get.approval
    val basis = io.circe.parser.parse(Files.readString(root.resolve("admitted-inputs/execution-basis.json"))).toOption.get.hcursor
    val rules = basis.get[Vector[Json]]("rules").toOption.get.map { value =>
      InternalModelCandidateReviewRule(value.hcursor.get[String]("ruleId").toOption.get, value.hcursor.get[String]("ruleVersion").toOption.get)
    }
    val providers = basis.get[Vector[Json]]("providers").toOption.get.map { value =>
      InternalModelCandidateReviewProvider(value.hcursor.get[String]("providerId").toOption.get, value.hcursor.get[String]("providerVersion").toOption.get)
    }
    val required = candidate.projection.targets.flatMap(target => target.mappings.map(mapping =>
      InternalModelContinuationMappingRequirement(target.targetId, mapping.mappingId, InternalModelSemanticTarget(mapping.semanticIdentityKind, mapping.semanticIdentity))))
    InternalModelContinuationRequest(state.packageContext.reference, state.packageContext.revision, review.realizationReference,
      review.scope, Some(review.candidateArtifactReference), Some(review.semanticDiffArtifactReference), Some(human.basis.reviewArtifactReference),
      Some(ModelEditSpecFixture.artifact("phase12-actual-human-approval", role = InternalModelArtifactRole.Approval)),
      Some(InternalModelCandidateReviewExecutionBasis(rules, providers)), Some(human), Some(Vector.empty), Some(required))
  }

  private def _run_probe(root: Path, input: Path, work: Path): ChildEvidence = {
    val output = work.resolve("probe.stdout.json")
    val errors = work.resolve("probe.stderr")
    val java = Path.of(sys.props("java.home"), "bin", "java").toAbsolutePath.normalize()
    val entries = sys.props("java.class.path").split(Pattern.quote(File.pathSeparator), -1).toVector.map(value => Path.of(value).toAbsolutePath.normalize())
    require(entries.nonEmpty && entries.forall(Files.exists(_)), "the selected forked Test runtime must supply the actual classpath")
    val command = Vector(java.toString, "-Xmx1024m", "-cp", entries.mkString(File.pathSeparator),
      "org.simplemodeling.textus.cbdsupport.runtime.ModelEditFreshProcessProbe", root.toString, input.toString)
    val builder = new ProcessBuilder(command*).directory(work.toFile).redirectOutput(output.toFile).redirectError(errors.toFile)
    builder.environment().clear()
    val process = builder.start()
    try {
      require(process.waitFor(60, TimeUnit.SECONDS), "actual read-only fresh JVM exceeded its 60 second execution bound")
      val report = io.circe.parser.parse(Files.readString(output)).fold(error => fail(error.message), value => value)
      ChildEvidence(report, process.pid(), process.exitValue(), !process.isAlive, Files.readString(errors))
    } finally if (process.isAlive) { process.destroyForcibly(); process.waitFor(10, TimeUnit.SECONDS); () }
  }
}
