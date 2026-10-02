package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, LinkOption, Path}
import io.circe.Json

/**
 * Separate substantive core and rich fixture families for durable action proof.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelDurableHandoffFixture {
  import InternalModelCandidateHumanApprovalValidatorSpec.*
  private val _core = InternalModelContinuationFixture

  def coreRequest: InternalModelContinuationRequest = InternalModelContinuationRequest(
    InternalModelPackageReference(InternalModelPackageId.from("01234567-89ab-cdef-0123-456789abcdef").toOption.get,
      InternalModelProjectToken.from("org.example").toOption.get, InternalModelProjectToken.from("continuity-sample").toOption.get),
    31L, recordReference("realization-all-eight", 5L),
    InternalModelSemanticScope("component-all-eight", "context-all-eight", "e-usecase"),
    None, None, None, None, None, None, Some(Vector.empty), None)

  def withCore(label: String = "display-one", reverseInputs: Boolean = false)(body: (Path, InternalModelContinuationRequest) => Unit): Unit =
    _core.withFixture(label, reverseInputs) { root =>
      val cursor = _core.cursor(root)
      _core.replaceCursor(root, cursor.copy(currentStage = "model-ready", lastCompletedAction = None,
        nextPermittedAction = Some("inspect-projections"), preconditions = Vector.empty,
        invalidationChecks = Vector.empty, acceptanceCriteria = Vector.empty, blockers = Vector.empty))
      body(root, coreRequest)
    }

  def approvedOptions: FixtureOptions = FixtureOptions(approvalchange = value => value.copy(unresolvedItems = Vector.empty))

  def withRich(options: FixtureOptions = approvedOptions, blocking: Boolean = false)(
    body: (Path, FixtureData, InternalModelContinuationRequest) => Unit
  ): Unit = withFixture(options) { (root, data) =>
    val packagepath = _core.packageRoot(root)
    _core.write(packagepath.resolve("decisions/main.json"), InternalModelDecisionRecordCodec.encode(decisionLedger(data)).toVector)
    _core.write(packagepath.resolve("open-issues/main.json"), InternalModelOpenIssueRecordCodec.encode(issueLedger(data, blocking)).toVector)
    val manifest = _core.json(Files.readAllBytes(packagepath.resolve("manifest.yaml")).toVector)
    val entries = manifest.hcursor.get[Vector[Json]]("artifacts").toOption.get.map { value =>
      if (value.hcursor.get[String]("artifactId").toOption.contains("resume-main")) value.mapObject(_.add("path", Json.fromString("resume.yaml"))) else value
    }
    _core.write(packagepath.resolve("manifest.yaml"), _core.canonical(manifest.mapObject(_.add("artifacts", Json.fromValues(entries)))))
    Files.delete(packagepath.resolve("resumes/main.json"))
    val selected = entries.filter(value => value.hcursor.get[String]("role").toOption.get != "resume" &&
      Files.exists(packagepath.resolve(value.hcursor.get[String]("path").toOption.get), LinkOption.NOFOLLOW_LINKS))
      .map(_core.entryReference).sortBy(_.artifactId.value)
    val cursor = InternalModelResumeCursor("2.0", "ccdm-resume-v2", InternalModelPackageId.from(options.packageid).toOption.get,
      options.carrierrevision, selected, "human-decision-recorded", Some("request-human-decision"), Some("handoff-approved-candidate"),
      Vector.empty, Vector.empty, Vector.empty, Vector.empty)
    _core.write(packagepath.resolve("resume.yaml"), InternalModelResumeCursorCodec.encode(cursor))
    _core.write(root.resolve("src/main/cml/original.cml"), Vector[Byte](0, -1, 10))
    body(root, data, richRequest(data))
  }

  /** All expected inputs originate in the independent fixture declarations, not stored claims. */
  def richRequest(data: FixtureData): InternalModelContinuationRequest = {
    val options = data.options
    InternalModelContinuationRequest(data.reviewbinding.subject.packageReference, options.carrierrevision,
      data.reviewbinding.realizationReference, data.expected.basis.scope, Some(artifactReference("candidate-main", options)),
      Some(artifactReference("semantic-diff-main", options)), Some(artifactReference("review-main", options)),
      Some(artifactReference("approval-main", options)), Some(data.executionbasis), Some(data.expected),
      Some(Vector(InternalModelContinuationDecisionRequirement(artifactReference("decision-main", options), recordReference("decision-current", 211L),
        "topic-primary", "choice-primary", Vector(InternalModelSemanticTarget("element", "e-mono"))))),
      Some(Vector(InternalModelContinuationMappingRequirement("target-alpha", "mapping-alpha", InternalModelSemanticTarget("element", "opaque-shared")),
        InternalModelContinuationMappingRequirement("target-beta", "mapping-beta", InternalModelSemanticTarget("relationship", "opaque-shared")))))
  }

  def decisionLedger(data: FixtureData): InternalModelDecisionLedger = {
    val decision = InternalModelDecisionRecord(recordReference("decision-current", 211L), "topic-primary", InternalModelDecisionState.Accepted,
      InternalModelDecisionActor("human", "decision-owner", "designer"), InternalModelSemanticSource("human-decision", "decision-owner", Some("decision/source"), Some("decision-version")),
      InternalModelDecisionChoice("choice-primary", "Explicit chosen option"), "Explicit rationale", Vector(InternalModelSemanticTarget("element", "e-mono")),
      Vector(InternalModelDecisionEvidence("evidence-human", InternalModelDecisionEvidenceKind.ExternalHuman,
        InternalModelSemanticSource("human-evidence", "external-human", None, Some("evidence-version")), None, Vector.empty, Vector("evidence condition"), Vector("evidence limitation"))),
      Vector("assumption"), Vector("condition", "condition"), Vector("limitation", "limitation"), Vector.empty,
      Vector(InternalModelDecisionAlternative("choice-other", "Alternative", "Explicitly rejected")),
      InternalModelDecisionBasis(artifactReference("realization-main", data.options), data.reviewbinding.realizationReference, data.expected.basis.scope, InternalModelDecisionBasisStatus.Current), None)
    InternalModelDecisionLedger("ccdm-decision-records-v2", "2.0", recordReference("decision-ledger", 223L), data.expected.basis.scope, Vector(decision))
  }

  def issueLedger(data: FixtureData, blocking: Boolean): InternalModelOpenIssueLedger = {
    val issue = InternalModelOpenIssueRecord(recordReference("issue-primary", 227L), InternalModelOpenIssueState.Open,
      "What remains unresolved?", "designer", Some("issue-owner"), "Bounded impact", Vector(InternalModelSemanticTarget("element", "e-mono")),
      Vector(InternalModelOpenIssueEvidence("issue-evidence", InternalModelOpenIssueEvidenceKind.ExternalHuman,
        InternalModelSemanticSource("human-evidence", "external-human", None, Some("issue-evidence-version")), None, Vector.empty, Vector("evidence condition"), Vector("evidence limitation"))),
      Vector(InternalModelOpenIssueOption("option-one", "Retained option", Vector("issue-evidence"), Vector("option assumption"), Vector("option condition"), Vector("option limitation"))),
      Vector("assumption"), Vector("condition", "condition"), Vector("limitation", "limitation"), Vector.empty, InternalModelOpenIssueBlocking(blocking, true, true, true))
    InternalModelOpenIssueLedger("ccdm-open-issue-records-v2", "2.0", recordReference("issue-ledger", 229L), data.expected.basis.scope,
      InternalModelOpenIssueBasis(artifactReference("realization-main", data.options), data.reviewbinding.realizationReference), Vector(issue))
  }

  /** Copy only the admitted present inventory, preserving optional raw evidence. */
  def copyPackage(sourceRoot: Path, destinationRoot: Path, workRoot: Path): Vector[String] = {
    InternalModelDurableHandoffSupport.requireOwned(workRoot, destinationRoot)
    val captured = InternalModelPackageValidator.verifiedContinuation(sourceRoot).toOption.get
    val paths = (Vector("project.yaml", "src/main/internal-model/manifest.yaml") ++ captured.artifacts.filter(_.context.present)
      .map(item => "src/main/internal-model/" + item.context.path)).sorted
    paths.foreach { relative =>
      val source = sourceRoot.resolve(relative)
      val destination = destinationRoot.resolve(relative).normalize()
      InternalModelDurableHandoffSupport.requireOwned(workRoot, destination)
      require(destination.startsWith(destinationRoot), "inventoried destination escapes producer")
      require(Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS), "inventoried source must be a regular file")
      Files.createDirectories(destination.getParent)
      Files.copy(source, destination)
    }
    paths
  }
}
