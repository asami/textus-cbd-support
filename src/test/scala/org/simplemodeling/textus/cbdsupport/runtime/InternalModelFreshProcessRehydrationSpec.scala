package org.simplemodeling.textus.cbdsupport.runtime

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Executable specification for package-only reconstruction in a distinct JVM.
 * Execute under Test / fork := true so java.class.path names the Test runtime.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
final class InternalModelFreshProcessRehydrationSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  private val _fixture = InternalModelContinuationFixture
  private val _probe = InternalModelFreshProcessProbe
  private final case class ChildEvidence(report: Json, pid: Long, exitcode: Int, terminated: Boolean, stderr: String)

  "Package-only reconstruction in a fresh process" should {
    "preserve complete current state" which {
      "F1 reconstruct all eight values, complete ledger, typed cursor/context and raw payload" in {
        Given("a current source-backed package with explicit recorded conditions")
        _fixture.withFixture() { root =>
          val cursor = _fixture.cursor(root)
          _fixture.replaceCursor(root, cursor.copy(lastCompletedAction = Some("recorded-action"),
            blockers = Vector(InternalModelResumeCondition("recorded-blocker", "Recorded unresolved condition", Some("projection-main"))),
            invalidationChecks = Vector(InternalModelResumeCondition("recorded-invalidation", "Recorded basis check", Some("realization-main"))),
            acceptanceCriteria = Vector(InternalModelResumeCondition("recorded-criterion", "Recorded criterion only", Some("snapshot-model")))))
          val original = _fixture.treeBytes(root)
          val expected = _expected_state(root)
          _with_consuming_copy(root, expected) { (copy, work) =>
            val before = _fixture.treeBytes(copy)
            When("an isolated distinct JVM admits the copied project and inventoried package")
            val child = _run_probe(copy, work)
            val after = _fixture.treeBytes(copy)
            val originalafter = _fixture.treeBytes(root)
            Then("the full structural state equals independent semantic admission and ordinary payload is preserved")
            _successful_child(child, expected)
            _current_witnesses(expected.continuity)
            expected.artifacts.find(_.context.reference.artifactId.value == "raw-approval").flatMap(_.bytes) shouldBe
              Some("raw unadmitted approval evidence\n".getBytes(StandardCharsets.UTF_8).toVector)
            expected.artifacts.find(_.context.reference.artifactId.value == "optional-source").flatMap(_.bytes) shouldBe None
            after shouldBe before
            originalafter shouldBe original
            _isolated_copy(copy, before.keySet)
          }
        }
      }

      "F2 preserve every value across four generated labels and opposite input permutations" in {
        val labels = Gen.choose(1, 16).flatMap(size => Gen.listOfN(size, Gen.alphaNumChar).map(_.mkString))
        forAll(labels, Gen.oneOf(true, false), minSuccessful(4)) { (label, reverse) =>
          Given("a generated label and independent opposite input permutations with fixed producer references")
          _fixture.withFixture(label, reverseInputs = reverse) { root =>
            _fixture.withFixture(label, reverseInputs = !reverse) { sibling =>
              val original = _fixture.treeBytes(root)
              val siblingbefore = _fixture.treeBytes(sibling)
              val expected = _expected_state(root)
              val independent = _expected_state(sibling)
              _with_consuming_copy(root, expected) { (copy, work) =>
                val before = _fixture.treeBytes(copy)
                When("a fresh JVM reconstructs the generated package")
                val child = _run_probe(copy, work)
                val after = _fixture.treeBytes(copy)
                val originalafter = _fixture.treeBytes(root)
                val siblingafter = _fixture.treeBytes(sibling)
                Then("complete values and all opaque identities agree with both independent admissions")
                _successful_child(child, independent)
                _probe.stateReport(expected) shouldBe _probe.stateReport(independent)
                expected.continuity.realization.elements.map(_.label).distinct shouldBe Vector(label)
                expected.continuity.realization.elements.map(_.identity).sorted shouldBe Vector(
                  "e-activity", "e-command", "e-dimension", "e-entity", "e-event", "e-flow", "e-koto", "e-mono",
                  "e-state-a", "e-state-b", "e-state-machine", "e-step", "e-trigger", "e-usecase", "e-workflow")
                _current_witnesses(expected.continuity)
                after shouldBe before
                originalafter shouldBe original
                siblingafter shouldBe siblingbefore
                _isolated_copy(copy, before.keySet)
              }
            }
          }
        }
      }

      "F4 accept harmless cursor whitespace, key order and missing final LF" in {
        Given("a consuming copy with equivalent pretty JSON and unchanged declared identities/revisions")
        _fixture.withFixture() { root =>
          val expected = _expected_state(root)
          val original = _fixture.treeBytes(root)
          _with_consuming_copy(root, expected) { (copy, work) =>
            val value = _fixture.json(Files.readAllBytes(_fixture.packageRoot(copy).resolve("resume.yaml")).toVector)
            val reordered = Json.fromJsonObject(io.circe.JsonObject.fromIterable(value.asObject.get.toVector.reverse))
            val bytes = (" \t" + Printer.spaces2.print(reordered) + " \r\t").getBytes(StandardCharsets.UTF_8).toVector
            _fixture.replaceArtifact(copy, "resume-main", bytes)
            val independent = _expected_state(copy)
            val before = _fixture.treeBytes(copy)
            When("the fresh JVM admits this alternative cursor presentation")
            val child = _run_probe(copy, work)
            val after = _fixture.treeBytes(copy)
            val originalafter = _fixture.treeBytes(root)
            Then("typed cursor and complete semantics agree while the actual raw cursor payload is retained")
            _successful_child(child, independent)
            independent.cursor shouldBe expected.cursor
            independent.continuity shouldBe expected.continuity
            independent.artifacts.find(_.context.reference.role == InternalModelArtifactRole.Resume).flatMap(_.bytes) shouldBe Some(bytes)
            after shouldBe before
            originalafter shouldBe original
            _isolated_copy(copy, before.keySet)
          }
        }
      }
    }

    "attribute production rejection separately from harness failure" which {
      "F3 reject an absent required source snapshot in the consuming copy" in {
        Given("an independently admitted original and a consuming copy lacking its required source snapshot")
        _fixture.withFixture() { root =>
          val expected = _expected_state(root)
          val original = _fixture.treeBytes(root)
          _with_consuming_copy(root, expected) { (copy, work) =>
            Files.delete(_fixture.packageRoot(copy).resolve("snapshots/model.json"))
            val before = _fixture.treeBytes(copy)
            When("the isolated child admits the incomplete package")
            val child = _run_probe(copy, work)
            val after = _fixture.treeBytes(copy)
            val originalafter = _fixture.treeBytes(root)
            Then("the child terminates with attributed required-source rejection and no package writes")
            _rejected_child(child, "required artifact snapshot-model is absent")
            after shouldBe before
            originalafter shouldBe original
            _isolated_copy(copy, before.keySet)
          }
        }
      }

      "F5 reject malformed cursor syntax and legacy package or semantic profiles without compatibility" in {
        Vector("syntax", "legacy-resume", "legacy-realization", "legacy-continuity", "legacy-manifest").foreach { variation =>
          Given(s"a consuming package with a $variation serialized input")
          _fixture.withFixture() { root =>
            val expected = _expected_state(root)
            val original = _fixture.treeBytes(root)
            _with_consuming_copy(root, expected) { (copy, work) =>
              val diagnostic = variation match {
                case "syntax" =>
                  _fixture.replaceArtifact(copy, "resume-main", "{}".getBytes(StandardCharsets.UTF_8).toVector)
                  "resume root fields"
                case "legacy-manifest" =>
                  val path = _fixture.packageRoot(copy).resolve("manifest.yaml")
                  val value = _fixture.json(Files.readAllBytes(path).toVector)
                  _fixture.write(path, _fixture.canonical(value.mapObject(_.add("schemaVersion", Json.fromString("1.0")))))
                  "manifest schemaVersion must be 2.0"
                case _ =>
                  val (id, path, profile, reason) = variation match {
                    case "legacy-realization" => ("realization-main", "realizations/main.json", "ccdm-realization-v1", "realization profile and schemaVersion are unsupported")
                    case "legacy-continuity" => ("projection-main", "projections/main.json", "ccdm-projection-binding-v1", "unknown profile/schemaVersion pair")
                    case _ => ("resume-main", "resume.yaml", "ccdm-resume-v1", "resume profile must be ccdm-resume-v2")
                  }
                  val value = _fixture.json(Files.readAllBytes(_fixture.packageRoot(copy).resolve(path)).toVector)
                  val changed = value.mapObject(_.add("profile", Json.fromString(profile)))
                  _fixture.replaceArtifact(copy, id, _fixture.canonical(changed))
                  reason
              }
              val before = _fixture.treeBytes(copy)
              When("the isolated child admits the actual current package payload")
              val child = _run_probe(copy, work)
              val after = _fixture.treeBytes(copy)
              val originalafter = _fixture.treeBytes(root)
              Then("production rejects the malformed or old format with status two")
              _rejected_child(child, diagnostic)
              after shouldBe before
              originalafter shouldBe original
              _isolated_copy(copy, before.keySet)
            }
          }
        }
      }

      "F6 reject wrong selected artifact version/role and carrier conflicts in the actual child" in {
        Vector("version", "role", "carrier").foreach { variation =>
          Given(s"a consuming cursor with a contradictory $variation declaration")
          _fixture.withFixture() { root =>
            val expected = _expected_state(root)
            _with_consuming_copy(root, expected) { (copy, work) =>
              val changed = if variation == "carrier" then expected.cursor.copy(packageRevision = 32L) else
                expected.cursor.copy(selectedArtifacts = expected.cursor.selectedArtifacts.map(reference =>
                  if reference == _fixture.projectionReference then {
                    if variation == "version" then reference.copy(artifactRevision = InternalModelArtifactRevision.from(24L).toOption.get)
                    else reference.copy(role = InternalModelArtifactRole.Decision)
                  } else reference))
              _fixture.replaceCursor(copy, changed)
              val before = _fixture.treeBytes(copy)
              When("the isolated JVM resolves the full selected references against the captured carrier")
              val child = _run_probe(copy, work)
              val after = _fixture.treeBytes(copy)
              Then("declared contradictions terminate as attributed production rejection")
              _rejected_child(child, if variation == "carrier" then "resume package identity or revision" else "ID, revision or role does not match inventory")
              after shouldBe before
              _isolated_copy(copy, before.keySet)
            }
          }
        }
      }

      "F7 distinguish harness status three while preserving ordinary source and consuming isolation" in {
        Given("an admitted original and an isolated copy with no CML or retained state")
        _fixture.withFixture() { root =>
          val expected = _expected_state(root)
          val original = _fixture.treeBytes(root)
          _with_consuming_copy(root, expected) { (copy, work) =>
            val before = _fixture.treeBytes(copy)
            When("the owned child is intentionally invoked without the required project argument")
            val child = _run_probe(copy, work, arguments = Some(Vector.empty))
            val after = _fixture.treeBytes(copy)
            val originalafter = _fixture.treeBytes(root)
            Then("harness failure is attributed distinctly from success zero and production rejection two")
            _child_envelope(child, "harness-error", 3)
            child.report.hcursor.downField("state").focus shouldBe Some(Json.Null)
            val failure = child.report.hcursor.downField("failure")
            failure.focus.get.asObject.get.keys.toSet shouldBe Set("diagnostic", "errorType", "origin")
            failure.get[String]("origin").toOption shouldBe Some("InternalModelFreshProcessProbe")
            failure.get[String]("diagnostic").toOption.get should include("exactly one consuming project root")
            after shouldBe before
            originalafter shouldBe original
            originalafter("src/main/cml/main.cml") shouldBe original("src/main/cml/main.cml")
            _isolated_copy(copy, before.keySet)
          }
        }
      }
    }
  }

  /** Expected semantics come from independent Phase 9 admission, never the rehydration validator. */
  private def _expected_state(root: Path): InternalModelRehydratedState = {
    val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
    val continuity = InternalModelProjectionContinuityValidator.validate(root).toOption.get
    val cursor = InternalModelResumeCursorCodec.decode(handoff.artifacts.find(_.context.reference.role == InternalModelArtifactRole.Resume).get.bytes.get).toOption.get
    InternalModelRehydratedState(handoff.packageContext, cursor, continuity, handoff.artifacts)
  }

  private def _with_consuming_copy(root: Path, state: InternalModelRehydratedState)(body: (Path, Path) => Unit): Unit = {
    val base = Files.createDirectories(Path.of("target/internal-model-fresh-process/work").toAbsolutePath)
    val work = Files.createTempDirectory(base, "child-")
    try {
      val copy = Files.createDirectory(work.resolve("project"))
      Files.copy(root.resolve("project.yaml"), copy.resolve("project.yaml"))
      val packagecopy = Files.createDirectories(_fixture.packageRoot(copy))
      Files.copy(_fixture.packageRoot(root).resolve("manifest.yaml"), packagecopy.resolve("manifest.yaml"))
      state.artifacts.filter(_.context.present).foreach { artifact =>
        val relative = artifact.context.path
        val destination = packagecopy.resolve(relative)
        Files.createDirectories(destination.getParent)
        Files.copy(_fixture.packageRoot(root).resolve(relative), destination)
      }
      body(copy, work)
    } finally {
      _fixture.removeTree(work)
    }
  }

  private def _run_probe(root: Path, work: Path, arguments: Option[Vector[String]] = None): ChildEvidence = {
    val cwd = Files.createDirectory(work.resolve("cwd"))
    val home = Files.createDirectory(work.resolve("home"))
    val temporary = Files.createDirectory(work.resolve("tmp"))
    val output = work.resolve("probe.stdout")
    val errors = work.resolve("probe.stderr")
    val java = Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath.normalize()
    require(Files.isExecutable(java), "java.home does not provide an executable JVM")
    val entries = System.getProperty("java.class.path", "").split(Pattern.quote(File.pathSeparator), -1).toVector
    require(entries.nonEmpty && entries.forall(_.nonEmpty), "forked Test runtime classpath is required")
    val paths = entries.map(entry => Path.of(entry).toAbsolutePath.normalize())
    require(paths.forall(Files.exists(_)), "forked Test classpath has an unavailable entry")
    val classpath = paths.map(_.toString).mkString(File.pathSeparator)
    val command = Vector(java.toString, s"-Duser.home=$home", s"-Djava.io.tmpdir=$temporary",
      "-cp", classpath, "org.simplemodeling.textus.cbdsupport.runtime.InternalModelFreshProcessProbe") ++
      arguments.getOrElse(Vector(root.toAbsolutePath.toString))
    val builder = new ProcessBuilder(command*)
    builder.directory(cwd.toFile)
    builder.environment().clear()
    builder.redirectOutput(output.toFile)
    builder.redirectError(errors.toFile)
    val child = builder.start()
    try {
      if (!child.waitFor(30, TimeUnit.SECONDS)) {
        fail("owned probe JVM did not terminate within 30 seconds")
      }
      val bytes = Files.readAllBytes(output).toVector
      val stderr = Files.readString(errors, StandardCharsets.UTF_8)
      val report = parse(new String(bytes.toArray, StandardCharsets.UTF_8)).fold(
        error => fail(s"probe report is missing or malformed: ${error.message}; stderr: $stderr"), identity)
      ChildEvidence(report, child.pid(), child.exitValue(), !child.isAlive, stderr)
    } finally {
      if (child.isAlive) {
        child.destroy()
        if (!child.waitFor(2, TimeUnit.SECONDS)) {
          child.destroyForcibly()
          require(child.waitFor(5, TimeUnit.SECONDS), "owned probe JVM could not be terminated")
        }
      }
    }
  }

  private def _child_envelope(child: ChildEvidence, status: String, exitcode: Int): Unit = withClue(child.stderr) {
    child.terminated shouldBe true
    child.pid should not be ProcessHandle.current().pid()
    child.report.asObject.get.keys.toSet shouldBe Set("failure", "pid", "state", "status")
    child.report.hcursor.get[Long]("pid").toOption shouldBe Some(child.pid)
    child.report.hcursor.get[String]("status").toOption shouldBe Some(status)
    child.exitcode shouldBe exitcode
  }

  private def _successful_child(child: ChildEvidence, state: InternalModelRehydratedState): Unit = {
    _child_envelope(child, "success", 0)
    child.report.hcursor.downField("failure").focus shouldBe Some(Json.Null)
    child.report.hcursor.downField("state").focus shouldBe Some(_probe.stateReport(state))
  }

  private def _rejected_child(child: ChildEvidence, diagnostic: String): Unit = {
    _child_envelope(child, "validator-rejected", 2)
    child.report.hcursor.downField("state").focus shouldBe Some(Json.Null)
    val failure = child.report.hcursor.downField("failure")
    failure.focus.get.asObject.get.keys.toSet shouldBe Set("conclusionType", "diagnostic", "origin")
    failure.get[String]("origin").toOption shouldBe Some("InternalModelRehydrationValidator.validate")
    failure.get[String]("conclusionType").toOption.get should not be empty
    failure.get[String]("diagnostic").toOption.get should include(diagnostic)
  }

  private def _isolated_copy(root: Path, paths: Set[String]): Unit = {
    paths should contain("project.yaml")
    paths.filterNot(_ == "project.yaml").forall(_.startsWith("src/main/internal-model/")) shouldBe true
    Files.exists(root.resolve("src/main/cml")) shouldBe false
    Vector("target", "chat", "provider", "retainedDB", ".codex", ".textus").foreach { name =>
      Files.exists(root.resolve(name)) shouldBe false
    }
  }

  private def _current_witnesses(value: InternalModelProjectionContinuity): Unit = {
    value.realization.profile shouldBe "ccdm-realization-v3"
    value.binding.profile shouldBe "ccdm-projection-binding-v3"
    value.monoKoto.subjects.map(_.sourceSubject.semanticTargetId.value) shouldBe Vector("e-mono", "e-koto")
    value.useCaseCommunication.subjects.head.sourceSubject.flows.head.steps.map(_.semanticRelationshipId.value) shouldBe Vector("r-usecase-step")
    value.entityModel.subjects.head.metadata.map(_.sourceMetadata.semanticRelationshipId.value) shouldBe Vector("r-entity-metadata")
    value.eventModel.assertions.map(_.sourceAssertion.semanticAssertionId.value) shouldBe Vector("r-event-cause")
    value.structureView.relations.head.assertions.map(_.sourceAssertion.semanticRelationshipId.value) shouldBe Vector("r-structure")
    value.classificationView.dimensions.head.assertions.map(_.sourceAssertion.semanticAssertionId.value) shouldBe Vector("r-classification-assertion")
    value.workflow.flows.map(_.sourceFlow.semanticFlowId.value) shouldBe Vector("r-workflow-flow")
    value.stateMachine.transitions.map(_.sourceTransition.semanticTransitionId.value) shouldBe Vector("r-state-transition")
    value.stateMachine.transitionAdjuncts.map(_.sourceTransitionAdjunct.semanticAdjunctRelationId.value) shouldBe Vector("r-state-adjunct")
    value.realization.canonicalAssertions.flatMap(_.association).toSet should contain(InternalModelSemanticAssociation("owner", "e-workflow", "element"))
    value.realization.enrichmentAssertions.map(_.content) shouldBe Vector("display-note:enrichment-only")
    value.realization.canonicalAssertions.map(_.content) should not contain "display-note:enrichment-only"
    value.realization.conditions.map(condition => (condition.conditionId, condition.affectedIdentity, condition.sourceReferenceId, condition.detail)) shouldBe
      Vector(("c-workflow-conflict", "r-workflow-flow", "ref-r-workflow-flow-role-WorkflowProjectionFlowRelation", "recorded conflict"))
    value.workflow.flows.head.sourceFlow.condition.conflict shouldBe Some("recorded conflict")
    value.useCaseCommunication.subjects.head.sourceSubject.flows.head.steps.head.sequenceKey shouldBe "step-1"
    value.workflow.flows.head.sourceFlow.attribution shouldBe
      ComponentDashboardSourceAttribution("model-source", "model-authority", "anchor-r-workflow-flow-role-WorkflowProjectionFlowRelation")
    value.workflow.flows.head.sourceFlow.sourceEndpoint.attribution.sourceLocator shouldBe "anchor-e-workflow-kind-WorkflowProjectionWorkflow"
    value.realization.sourceReferences.map(_.snapshotReference).toSet shouldBe Set(_fixture.snapshotReference)
  }
}
