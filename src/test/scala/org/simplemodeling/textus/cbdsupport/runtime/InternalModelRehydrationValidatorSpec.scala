package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import io.circe.{Json, Printer}
import org.goldenport.Consequence
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.Inspectors.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Executable specification for one-capture typed internal-model rehydration.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  2, 2026
 */
final class InternalModelRehydrationValidatorSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  private val _fixture = InternalModelContinuationFixture

  "One-capture internal-model rehydration" should {
    "preserve the complete admitted basis" which {
      "R1 retain all eight projections, full ledger, attribution, conditions and sidecars" in {
        Given("a substantive current package with separate canonical and enrichment assertions")
        _fixture.withFixture() { root =>
          val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
          val existing = InternalModelProjectionContinuityValidator.validate(root).toOption.get
          When("the captured inventory is rehydrated")
          val result = InternalModelRehydrationValidator.validateVerified(handoff)
          Then("the independently admitted continuity and complete raw capture remain exact")
          withClue(result.show) { result.isSuccess shouldBe true }
          val state = result.toOption.get
          state.continuity shouldBe existing
          state.packageContext shouldBe handoff.packageContext
          state.artifacts shouldBe handoff.artifacts
          state.cursor shouldBe _fixture.cursor(root)
          _all_eight(state.continuity)
          state.continuity.realization.scope shouldBe InternalModelSemanticScope("component-all-eight", "context-all-eight", "e-usecase")
          state.continuity.realization.relationships.map(value =>
            (value.identity, value.sourceElementIdentity, value.targetElementIdentity, value.role, value.direction)) shouldBe
            existing.realization.relationships.map(value =>
              (value.identity, value.sourceElementIdentity, value.targetElementIdentity, value.role, value.direction))
          state.continuity.realization.canonicalAssertions.flatMap(_.association).toSet should contain(InternalModelSemanticAssociation("owner", "e-workflow", "element"))
          state.continuity.realization.enrichmentAssertions.map(_.content) shouldBe Vector("display-note:enrichment-only")
          state.continuity.realization.canonicalAssertions.map(_.content) should not contain "display-note:enrichment-only"
          state.continuity.realization.conditions.map(value => (value.conditionId, value.affectedIdentity, value.sourceReferenceId, value.detail)) shouldBe
            Vector(("c-workflow-conflict", "r-workflow-flow", "ref-r-workflow-flow-role-WorkflowProjectionFlowRelation", "recorded conflict"))
          state.continuity.workflow.flows.head.sourceFlow.condition.conflict shouldBe Some("recorded conflict")
          state.continuity.useCaseCommunication.subjects.head.sourceSubject.flows.head.steps.head.sequenceKey shouldBe "step-1"
          state.continuity.workflow.flows.head.sourceFlow.attribution shouldBe
            ComponentDashboardSourceAttribution("model-source", "model-authority", "anchor-r-workflow-flow-role-WorkflowProjectionFlowRelation")
          state.continuity.workflow.flows.head.sourceFlow.sourceEndpoint.attribution.sourceLocator shouldBe "anchor-e-workflow-kind-WorkflowProjectionWorkflow"
          state.continuity.realization.sourceReferences.map(_.snapshotReference).toSet shouldBe Set(_fixture.snapshotReference)
          state.continuity.realization.sourceReferences.map(_.source.revision).distinct shouldBe Vector(Some("source-version-nine"))
          state.continuity.realization.realizationReference.recordRevision.value shouldBe 5L
          state.continuity.binding.bindingReference.recordRevision.value shouldBe 7L
          state.packageContext.revision shouldBe 31L
          InternalModelSemanticRealizationValidator.encode(state.continuity.realization) shouldBe handoff.continuityPackage.realizationpackage.realization.bytes
          InternalModelProjectionContinuityValidator.encode(state.continuity.binding) shouldBe handoff.continuityPackage.projection.bytes
        }
      }

      "R2 preserve opaque identities and every semantic value across generated labels and input permutations" in {
        forAll(Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString), Gen.oneOf(true, false)) { (label, reverse) =>
          Given("a generated label and independent opposite input permutations")
          _fixture.withFixture(label, reverseInputs = reverse) { root =>
            _fixture.withFixture(label, reverseInputs = !reverse) { sibling =>
              val independent = InternalModelProjectionContinuityValidator.validate(root).toOption.get
              When("the two packages are rehydrated")
              val left = InternalModelRehydrationValidator.validate(root).toOption.get
              val right = InternalModelRehydrationValidator.validate(sibling).toOption.get
              Then("complete continuity, typed references and all identities survive the presentation variation")
              left.continuity shouldBe independent
              right.continuity shouldBe independent
              left.cursor shouldBe right.cursor
              left.artifacts shouldBe right.artifacts
              left.continuity.realization.elements.map(_.label).distinct shouldBe Vector(label)
              left.continuity.realization.elements.map(_.identity).sorted shouldBe Vector("e-activity", "e-command", "e-dimension", "e-entity",
                "e-event", "e-flow", "e-koto", "e-mono", "e-state-a", "e-state-b", "e-state-machine", "e-step", "e-trigger", "e-usecase", "e-workflow")
              _all_eight(left.continuity)
            }
          }
        }
      }

      "R3 reuse captured state after actual root deletion while fresh admission rejects the missing root" in {
        Given("an admitted capture whose private test root is removed")
        _fixture.withFixture() { root =>
          val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
          val before = InternalModelRehydrationValidator.validateVerified(handoff).toOption.get
          _fixture.removeTree(root)
          When("pure reconstruction and fresh path admission are requested")
          val pure = InternalModelRehydrationValidator.validateVerified(handoff)
          val fresh = InternalModelRehydrationValidator.validate(root)
          Then("the retained state is exact and a missing live root cannot be newly admitted")
          pure.toOption shouldBe Some(before)
          _rejected(fresh, "project root must be a non-symbolic directory")
          Files.exists(root) shouldBe false
        }
      }

      "R4 reuse captured state after filesystem replacement while rejecting malformed newly captured payload" in {
        Given("a complete capture followed by malformed replacement snapshot bytes at the same declared revision")
        _fixture.withFixture() { root =>
          val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
          val expected = InternalModelRehydrationValidator.validateVerified(handoff).toOption.get
          _fixture.replaceArtifact(root, "snapshot-model", _bytes("replaced malformed evidence\n"))
          When("the original capture and a fresh capture are separately admitted")
          val pure = InternalModelRehydrationValidator.validateVerified(handoff)
          val fresh = InternalModelRehydrationValidator.validate(root)
          Then("pure reconstruction retains its source and the semantic owner rejects the malformed new basis")
          pure.toOption shouldBe Some(expected)
          _rejected(fresh, "source-snapshot artifact snapshot-model is invalid")
        }
      }

      "R5 write nothing and retain selected raw approval and unrelated optional records without admitting them" in {
        Given("a core cursor extended by a present raw approval and unrelated absent optional inventory")
        _fixture.withFixture() { root =>
          val original = _fixture.treeBytes(root)
          val cursor = _fixture.cursor(root)
          val approval = _fixture.entryReference(_fixture.inventory(root).find(_.hcursor.get[String]("artifactId").toOption.contains("raw-approval")).get)
          _fixture.replaceCursor(root, cursor.copy(selectedArtifacts = (cursor.selectedArtifacts :+ approval).sortBy(_.artifactId.value)))
          val before = _fixture.treeBytes(root)
          When("the package is reconstructed")
          val result = InternalModelRehydrationValidator.validate(root)
          val after = _fixture.treeBytes(root)
          Then("ordinary CML/payload remains unchanged and raw records establish no action or human approval")
          withClue(result.show) { result.isSuccess shouldBe true }
          after shouldBe before
          after("src/main/cml/main.cml") shouldBe original("src/main/cml/main.cml")
          val state = result.toOption.get
          state.artifacts.find(_.context.reference.artifactId.value == "raw-approval").flatMap(_.bytes) shouldBe Some(_bytes("raw unadmitted approval evidence\n"))
          state.artifacts.find(_.context.reference.artifactId.value == "optional-source").flatMap(_.bytes) shouldBe None
          state.artifacts.find(_.context.reference.artifactId.value == "raw-decision").flatMap(_.bytes) shouldBe None
          state.cursor.nextPermittedAction shouldBe Some("future-gate-action")
          state.continuity.realization.consumedSnapshotReferences shouldBe Vector(_fixture.snapshotReference)
        }
      }

      "R18 delegate one package capture without CML, DB, provider, chat or target recovery" in {
        Given("a complete package capture and independent semantic admission without auxiliary state")
        _fixture.withFixture() { root =>
          val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
          val independent = InternalModelProjectionContinuityValidator.validateVerified(handoff.continuityPackage).toOption.get
          _fixture.removeTree(root.resolve("src/main/cml"))
          When("the filesystem entry delegates and the capture is independently reconstructed")
          val rooted = InternalModelRehydrationValidator.validate(root)
          val pure = InternalModelRehydrationValidator.validateVerified(handoff)
          Then("both entries preserve package-only values and consuming CML or auxiliary state is not created")
          rooted.toOption shouldBe pure.toOption
          rooted.toOption.map(_.continuity) shouldBe Some(independent)
          Vector("src/main/cml", "target", "retainedDB", "provider", "chat").foreach(name => Files.exists(root.resolve(name)) shouldBe false)
        }
      }
    }

    "reject incomplete or contradictory declared state" which {
      "R6 reject package/cursor/project identity and carrier revision mismatches" in {
        Vector("cursor-package", "cursor-revision", "manifest-package", "manifest-revision", "project-id", "context-id").foreach { variation =>
          Given(s"a valid package with one changed $variation declaration")
          _fixture.withFixture() { root =>
            val alternate = InternalModelPackageId.from("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee").toOption.get
            val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
            variation match {
              case "cursor-package" => _fixture.replaceCursor(root, _fixture.cursor(root).copy(packageId = alternate))
              case "cursor-revision" => _fixture.replaceCursor(root, _fixture.cursor(root).copy(packageRevision = 32L))
              case "manifest-package" => _fixture.replaceInventory(root, _fixture.inventory(root), identity = alternate)
              case "manifest-revision" => _fixture.replaceInventory(root, _fixture.inventory(root), revision = 32L)
              case "project-id" => _fixture.write(root.resolve("project.yaml"), _bytes("project:\n  namespace: org.example\n  id: alternate-project\n"))
              case _ => ()
            }
            val changed = handoff.copy(packageContext = handoff.packageContext.copy(reference = handoff.packageContext.reference.copy(packageId = alternate)))
            When("the declared carrier and consuming project are cross-bound")
            val result = if variation == "context-id" then InternalModelRehydrationValidator.validateVerified(changed) else InternalModelRehydrationValidator.validate(root)
            Then("alternate valid identities and revisions cannot substitute the cursor basis")
            _rejected(result, if variation == "project-id" then "manifest identity does not match project.yaml" else "resume package identity or revision")
          }
        }
      }

      "R7 reject wrong selected ID/revision/role and omission of every consumed core reference" in {
        val mutations: Vector[InternalModelResumeCursor => InternalModelResumeCursor] = Vector[InternalModelResumeCursor => InternalModelResumeCursor](
          cursor => cursor.copy(selectedArtifacts = cursor.selectedArtifacts.map(reference =>
            if reference == _fixture.projectionReference then reference.copy(artifactId = InternalModelArtifactId.from("unknown-artifact").toOption.get) else reference).sortBy(_.artifactId.value)),
          cursor => cursor.copy(selectedArtifacts = cursor.selectedArtifacts.map(reference =>
            if reference == _fixture.projectionReference then reference.copy(role = InternalModelArtifactRole.Decision) else reference)),
          cursor => cursor.copy(selectedArtifacts = cursor.selectedArtifacts.map(reference =>
            if reference == _fixture.projectionReference then reference.copy(artifactRevision = InternalModelArtifactRevision.from(24L).toOption.get) else reference))
        ) ++ Vector(_fixture.projectionReference, _fixture.realizationReference, _fixture.snapshotReference).map(reference =>
          (cursor: InternalModelResumeCursor) => cursor.copy(selectedArtifacts = cursor.selectedArtifacts.filterNot(_ == reference), preconditions = Vector.empty))
        mutations.foreach { change =>
          Given("a wrong full reference or an omitted continuity, realization or source dependency")
          _fixture.withFixture() { root =>
            _fixture.replaceCursor(root, change(_fixture.cursor(root)))
            When("the selected references are resolved against complete captured inventory")
            val result = InternalModelRehydrationValidator.validate(root)
            Then("exact versions, roles and the full consumed set are required")
            _rejected(result, "resume selected")
          }
        }
      }

      "R8 reject selected absent optional evidence without requiring unrelated absent optional records" in {
        Given("an optional absent decision added to the core selection")
        _fixture.withFixture() { root =>
          val cursor = _fixture.cursor(root)
          val missing = _fixture.entryReference(_fixture.inventory(root).find(_.hcursor.get[String]("artifactId").toOption.contains("raw-decision")).get)
          _fixture.replaceCursor(root, cursor.copy(selectedArtifacts = (cursor.selectedArtifacts :+ missing).sortBy(_.artifactId.value)))
          When("the explicitly selected absent record is resolved")
          val result = InternalModelRehydrationValidator.validate(root)
          Then("selected absence is attributed without requiring other optional records")
          _rejected(result, "resume selected artifact raw-decision is absent")
        }
      }

      "R9 reject duplicate, misplaced, absent and omitted Resume entries without changing structural multiplicity" in {
        Vector("duplicate", "misplaced", "absent", "omitted").foreach { variation =>
          Given(s"a structurally valid package with a $variation Resume entry")
          _fixture.withFixture() { root =>
            val entries = _fixture.inventory(root)
            val bytes = Files.readAllBytes(_fixture.packageRoot(root).resolve("resume.yaml")).toVector
            val changed = variation match {
              case "duplicate" =>
                _fixture.write(_fixture.packageRoot(root).resolve("resume-other.yaml"), bytes)
                entries :+ _fixture.artifact("resume-other", 47L, "resume-other.yaml", "resume")
              case "misplaced" =>
                Files.move(_fixture.packageRoot(root).resolve("resume.yaml"), _fixture.packageRoot(root).resolve("cursor.yaml"))
                entries.map(entry => if entry.hcursor.get[String]("role").toOption.contains("resume") then entry.mapObject(_.add("path", Json.fromString("cursor.yaml"))) else entry)
              case "absent" =>
                Files.delete(_fixture.packageRoot(root).resolve("resume.yaml"))
                entries.map(entry => if entry.hcursor.get[String]("role").toOption.contains("resume") then entry.mapObject(_.add("required", Json.False)) else entry)
              case _ =>
                Files.delete(_fixture.packageRoot(root).resolve("resume.yaml"))
                entries.filterNot(_.hcursor.get[String]("role").toOption.contains("resume"))
            }
            _fixture.replaceInventory(root, changed)
            When("structural and continuation admission are requested separately")
            val structure = InternalModelPackageValidator.validateStructure(root)
            val result = InternalModelRehydrationValidator.validate(root)
            Then("structural policy remains valid while the one-present-resume content gate rejects")
            structure.isSuccess shouldBe true
            _rejected(result, "resume")
          }
        }
      }

      "R10 reject malformed witnesses, continuity mapping and cursor syntax while accepting harmless presentation" in {
        Vector("witness", "continuity", "cursor", "presentation").foreach { variation =>
          Given(s"a current capture with a $variation payload replacement and unchanged declared revisions")
          _fixture.withFixture() { root =>
            val expected = InternalModelProjectionContinuityValidator.validate(root).toOption.get
            variation match {
              case "witness" =>
                val path = _fixture.packageRoot(root).resolve("snapshots/model.json")
                val content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\"content\":\"kind:Mono\"", "\"content\":\"kind:Koto\"")
                _fixture.replaceArtifact(root, "snapshot-model", _bytes(content))
              case "continuity" =>
                val content = new String(Files.readAllBytes(_fixture.packageRoot(root).resolve("projections/main.json")), StandardCharsets.UTF_8)
                  .replace("\"viewRole\":\"Mono\"", "\"viewRole\":\"Koto\"")
                _fixture.replaceArtifact(root, "projection-main", _bytes(content))
              case "cursor" => _fixture.replaceArtifact(root, "resume-main", _bytes("{}"))
              case _ =>
                val bytes = Files.readAllBytes(_fixture.packageRoot(root).resolve("resume.yaml")).toVector
                _fixture.replaceArtifact(root, "resume-main", _bytes(" " + Printer.spaces2.print(_fixture.json(bytes)) + "\n\n"))
            }
            When("syntax and existing semantic owners admit the actual payload")
            val structure = InternalModelPackageValidator.validateStructure(root)
            val result = InternalModelRehydrationValidator.validate(root)
            Then("structure does not authenticate content; malformed semantics reject and equivalent layout preserves meaning")
            structure.isSuccess shouldBe true
            if variation == "presentation" then result.toOption.map(_.continuity) shouldBe Some(expected)
            else result.isSuccess shouldBe false
          }
        }
      }

      "R11 reject old manifest schemas and legacy semantic/resume profiles without migration" in {
        Vector("manifest", "realization", "continuity", "resume").foreach { variation =>
          Given(s"a current package with a legacy $variation schema or profile")
          _fixture.withFixture() { root =>
            val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
            val changed = handoff.copy(packageContext = handoff.packageContext.copy(schemaversion = "1.0"))
            if variation != "manifest" then {
              val (id, path, profile) = variation match {
                case "realization" => ("realization-main", "realizations/main.json", "ccdm-realization-v1")
                case "continuity" => ("projection-main", "projections/main.json", "ccdm-projection-binding-v1")
                case _ => ("resume-main", "resume.yaml", "ccdm-resume-v1")
              }
              val value = _fixture.json(Files.readAllBytes(_fixture.packageRoot(root).resolve(path)).toVector)
              _fixture.replaceArtifact(root, id, _fixture.canonical(value.mapObject(_.add("profile", Json.fromString(profile)).add("schemaVersion", Json.fromString("1.0")))))
            }
            When("pure metadata or actual serialized payload is admitted")
            val result = if variation == "manifest" then InternalModelRehydrationValidator.validateVerified(changed) else InternalModelRehydrationValidator.validate(root)
            Then("no old reader or inferred version substitutes the current contract")
            result.isSuccess shouldBe false
            if variation == "manifest" then result.fold(conclusion => _operation_invalid(conclusion, "captured schemaVersion must be 2.0"), _ => fail("old schema was admitted"))
          }
        }
      }

      "R12 reject generated malformed lifecycle/project grammar and nonpositive carrier revisions" in {
        forAll(Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString), Gen.choose(-1000L, 0L)) { (token, revision) =>
          Vector("lifecycle", "namespace", "project", "revision").foreach { variation =>
            Given(s"a typed caller graph with an invalid $variation declaration")
            _fixture.withFixture() { root =>
              val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
              val context = handoff.packageContext
              val changed = variation match {
                case "lifecycle" => context.copy(lifecyclestate = token + " invalid")
                case "namespace" => context.copy(reference = context.reference.copy(projectNamespace = (token + " invalid").asInstanceOf[InternalModelProjectToken]))
                case "project" => context.copy(reference = context.reference.copy(projectId = (token + " invalid").asInstanceOf[InternalModelProjectToken]))
                case _ => context.copy(revision = revision)
              }
              When("the pure boundary admits the caller's complete typed metadata")
              val result = InternalModelRehydrationValidator.validateVerified(handoff.copy(packageContext = changed))
              Then("typed value names cannot bypass grammar or positive carrier revision admission")
              result.isSuccess shouldBe false
              if variation == "revision" then result.fold(conclusion => _operation_invalid(conclusion, "captured carrier revision must be positive"), _ => fail("nonpositive revision was admitted"))
            }
          }
        }
      }

      "R13 reject complete raw inventory ID/role/revision/path/dependency/order/presence/cycle violations" in {
        val variations = Vector("id", "role", "revision", "path", "duplicate-id", "duplicate-path", "dependency-id",
          "dependency-version", "dependency-role", "dependency-duplicate", "dependency-order", "dependency-missing",
          "dependency-self", "dependency-cycle", "inventory-order", "dependency-absent", "presence", "required-absent")
        variations.foreach { variation =>
          Given(s"a complete capture with an explicit raw-inventory $variation violation")
          _fixture.withFixture() { root =>
            val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
            val changed = _invalid_inventory_capture(handoff, variation)
            When("the pure boundary validates every declared entry, including raw unselected evidence")
            val result = InternalModelRehydrationValidator.validateVerified(changed)
            Then("the invalid inventory rejects without semantic admission of raw records")
            result.isSuccess shouldBe false
            changed.continuityPackage shouldBe handoff.continuityPackage
          }
        }
      }

      "R14 reject two present realizations and two recognized continuity projections" in {
        Vector(InternalModelArtifactRole.Realization, InternalModelArtifactRole.Projection).foreach { role =>
          Given("a complete capture with a distinct second present core artifact")
          _fixture.withFixture() { root =>
            val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
            val original = handoff.artifacts.find(_.context.reference.role == role).get
            val duplicate = original.copy(context = original.context.copy(
              reference = original.context.reference.copy(artifactId = InternalModelArtifactId.from("zz-duplicate").toOption.get),
              path = if role == InternalModelArtifactRole.Realization then "realizations/duplicate.json" else "projections/duplicate.json"))
            val changed = _fixture.withCapturedInventory(handoff, handoff.artifacts :+ duplicate)
            When("continuity is selected from the entire captured inventory")
            val result = InternalModelRehydrationValidator.validateVerified(changed)
            Then("no preferred sibling silently wins")
            _rejected(result, if role == InternalModelArtifactRole.Realization then "multiple present realization" else "multiple present continuity")
          }
        }
      }

      "R15 retain current candidate/diff projection families solely as raw evidence" in {
        Vector("candidate" -> "ccdm-candidate-cml-projection-v2", "semantic-diff" -> "ccdm-semantic-diff-v2").foreach { case (family, profile) =>
          Given(s"a genuine capture with a recognized $family projection and exact dependency")
          _fixture.withFixture() { root =>
            val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
            val independent = InternalModelProjectionContinuityValidator.validateVerified(handoff.continuityPackage).toOption.get
            val original = handoff.artifacts.find(_.context.reference == _fixture.projectionReference).get
            val bytes = _fixture.canonical(Json.obj("profile" -> Json.fromString(profile), "schemaVersion" -> Json.fromString("2.0")))
            val additional = original.copy(context = original.context.copy(
              reference = _fixture.reference(s"zz-$family", 53L, InternalModelArtifactRole.Projection), path = s"projections/$family.json"), bytes = Some(bytes))
            val changed = _fixture.withCapturedInventory(handoff, handoff.artifacts :+ additional)
            When("the actual captured continuity family is reconstructed")
            val result = InternalModelRehydrationValidator.validateVerified(changed)
            Then("all-eight continuity remains independent and the other family is retained without content admission")
            withClue(result.show) { result.isSuccess shouldBe true }
            result.toOption.map(_.continuity) shouldBe Some(independent)
            result.toOption.map(_.artifacts) shouldBe Some(changed.artifacts)
            _all_eight(result.toOption.get.continuity)
          }
        }
      }

      "R16 reject caller metadata/source-inventory/core-handoff contradictions and null typed graphs" in {
        Given("a genuine capture with explicitly malformed or contradictory caller-built metadata")
        _fixture.withFixture() { root =>
          val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
          val continuity = handoff.continuityPackage
          val realization = continuity.realizationpackage
          val projection = continuity.projection
          val substitutions = Vector(
            null, handoff.copy(packageContext = null), handoff.copy(artifacts = null), handoff.copy(continuityPackage = null),
            handoff.copy(packageContext = handoff.packageContext.copy(reference = null)),
            handoff.copy(packageContext = handoff.packageContext.copy(artifacts = Vector(null))),
            handoff.copy(artifacts = Vector(null)),
            handoff.copy(artifacts = handoff.artifacts.map(value => value.copy(context = null))),
            handoff.copy(artifacts = handoff.artifacts :+ handoff.artifacts.head),
            handoff.copy(packageContext = handoff.packageContext.copy(artifacts = Vector.empty)),
            handoff.copy(continuityPackage = continuity.copy(projection = projection.copy(reference = projection.reference.copy(artifactRevision = InternalModelArtifactRevision.from(24L).toOption.get)))),
            handoff.copy(continuityPackage = continuity.copy(projection = projection.copy(path = "projections/other.json"))),
            handoff.copy(continuityPackage = continuity.copy(projection = projection.copy(required = !projection.required))),
            handoff.copy(continuityPackage = continuity.copy(projection = projection.copy(dependencies = Vector.empty))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(realization = realization.realization.copy(path = "realizations/other.json")))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(realization = realization.realization.copy(reference = realization.realization.reference.copy(artifactRevision = InternalModelArtifactRevision.from(18L).toOption.get))))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(realization = realization.realization.copy(required = !realization.realization.required)))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(realization = realization.realization.copy(dependencies = Vector.empty)))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(sourcesnapshots = Vector.empty))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(sourcesnapshots = realization.sourcesnapshots.reverse))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(sourcesnapshots = realization.sourcesnapshots.map(value => if value.reference == _fixture.snapshotReference then value.copy(reference = value.reference.copy(artifactRevision = InternalModelArtifactRevision.from(12L).toOption.get)) else value)))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(sourcesnapshots = realization.sourcesnapshots.map(value => if value.reference == _fixture.snapshotReference then value.copy(path = "snapshots/other.json") else value)))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(sourcesnapshots = realization.sourcesnapshots.map(value => if value.reference == _fixture.snapshotReference then value.copy(required = !value.required) else value)))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(sourcesnapshots = realization.sourcesnapshots.map(value => if value.reference == _fixture.snapshotReference then value.copy(dependencies = Vector(_fixture.realizationReference)) else value)))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = realization.copy(sourcesnapshots = realization.sourcesnapshots.map(value => if value.reference == _fixture.snapshotReference then value.copy(bytes = None) else value)))),
            handoff.copy(continuityPackage = continuity.copy(realizationpackage = null)),
            handoff.copy(artifacts = handoff.artifacts.map(value => value.copy(bytes = null))),
            handoff.copy(artifacts = handoff.artifacts.map(value => value.copy(bytes = Some(null))))
          )
          When("the pure entry point admits each supplied graph")
          val results = substitutions.map(InternalModelRehydrationValidator.validateVerified)
          Then("matching type names do not grant metadata authority or permit null graphs")
          all(results.map(_.isSuccess)) shouldBe false
        }
      }

      "R17 derive semantics from captured inventory rather than duplicate caller payload sidecars" in {
        Given("a genuine capture with matching handoff metadata and unrelated duplicate payload replacements")
        _fixture.withFixture() { root =>
          val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
          val continuity = handoff.continuityPackage
          val realization = continuity.realizationpackage
          val independent = InternalModelProjectionContinuityValidator.validateVerified(continuity).toOption.get
          val duplicate = continuity.copy(projection = continuity.projection.copy(bytes = _bytes("duplicate malformed projection")),
            realizationpackage = realization.copy(realization = realization.realization.copy(bytes = Vector.empty),
              sourcesnapshots = realization.sourcesnapshots.map(value => value.copy(bytes = value.bytes.map(_ => _bytes("duplicate malformed source"))))))
          val changed = handoff.copy(continuityPackage = duplicate)
          val changedraw = _fixture.withCapturedInventory(handoff, handoff.artifacts.map(value =>
            if value.context.reference.artifactId.value == "raw-approval" then value.copy(bytes = Some(_bytes("changed raw evidence"))) else value))
          val malformed = _fixture.withCapturedInventory(handoff, handoff.artifacts.map(value =>
            if value.context.reference == _fixture.snapshotReference then value.copy(bytes = Some(_bytes("malformed actual captured source"))) else value))
          When("the pure boundary admits duplicate sidecars, changed raw evidence and malformed captured semantic input")
          val result = InternalModelRehydrationValidator.validateVerified(changed)
          val rawresult = InternalModelRehydrationValidator.validateVerified(changedraw)
          val malformedresult = InternalModelRehydrationValidator.validateVerified(malformed)
          Then("duplicate bytes cannot replace the captured basis; no authenticity check is claimed for raw content")
          result.toOption.map(_.continuity) shouldBe Some(independent)
          rawresult.toOption.map(_.continuity) shouldBe Some(independent)
          rawresult.toOption.map(_.artifacts) shouldBe Some(changedraw.artifacts)
          _rejected(malformedresult, "source-snapshot artifact snapshot-model is invalid")
        }
      }
    }
  }

  private def _rejected(result: Consequence[InternalModelRehydratedState], diagnostic: String): Unit = {
    withClue(result.show) { result.isSuccess shouldBe false }
    result.show should include(diagnostic)
  }

  private def _bytes(value: String): Vector[Byte] = value.getBytes(StandardCharsets.UTF_8).toVector

  private def _invalid_inventory_capture(handoff: InternalModelVerifiedContinuationPackage, variation: String): InternalModelVerifiedContinuationPackage = {
    val optional = handoff.artifacts.find(_.context.reference.artifactId.value == "optional-source").get.context.reference
    val approval = handoff.artifacts.find(_.context.reference.artifactId.value == "raw-approval").get.context.reference
    val decision = handoff.artifacts.find(_.context.reference.artifactId.value == "raw-decision").get.context.reference
    val artifacts = handoff.artifacts.map { artifact =>
      val context = artifact.context
      val changed = if context.reference == approval then variation match {
        case "id" => context.copy(reference = context.reference.copy(artifactId = "bad id".asInstanceOf[InternalModelArtifactId]))
        case "role" => context.copy(reference = context.reference.copy(role = null))
        case "revision" => context.copy(reference = context.reference.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision]))
        case "path" => context.copy(path = "../raw.json")
        case "duplicate-id" => context.copy(reference = context.reference.copy(artifactId = decision.artifactId))
        case "duplicate-path" => context.copy(path = "decisions/raw.json")
        case "dependency-id" => context.copy(dependencies = Vector(optional.copy(artifactId = "bad token".asInstanceOf[InternalModelArtifactId])))
        case "dependency-version" => context.copy(dependencies = Vector(optional.copy(artifactRevision = InternalModelArtifactRevision.from(99L).toOption.get)))
        case "dependency-role" => context.copy(dependencies = Vector(optional.copy(role = InternalModelArtifactRole.Decision)))
        case "dependency-duplicate" => context.copy(dependencies = Vector(optional, optional))
        case "dependency-order" => context.copy(dependencies = Vector(_fixture.snapshotReference, optional))
        case "dependency-missing" => context.copy(dependencies = Vector(_fixture.reference("unknown-artifact", 59L, InternalModelArtifactRole.Decision)))
        case "dependency-self" => context.copy(dependencies = Vector(approval))
        case "dependency-cycle" => context.copy(dependencies = Vector(decision))
        case "dependency-absent" => context.copy(dependencies = Vector(optional))
        case "presence" => context.copy(present = false)
        case _ => context
      } else if context.reference == decision then variation match {
        case "dependency-cycle" => context.copy(dependencies = Vector(approval))
        case "required-absent" => context.copy(required = true)
        case _ => context
      } else context
      artifact.copy(context = changed)
    }
    _fixture.withCapturedInventory(handoff, if variation == "inventory-order" then artifacts.reverse else artifacts)
  }

  private def _operation_invalid(conclusion: org.goldenport.Conclusion, message: String): Unit = {
    val observation = conclusion.observation
    val cause = observation.cause
    val descriptor = cause.descriptor.copy(facets = cause.descriptor.facets.filterNot(_.isInstanceOf[org.goldenport.observation.Descriptor.Facet.SrcPos]))
    conclusion.copy(observation = observation.copy(cause = cause.copy(descriptor = descriptor))).isMatch(org.goldenport.Conclusion.operationInvalid(message)) shouldBe true
  }

  private def _all_eight(value: InternalModelProjectionContinuity): Unit = {
    value.monoKoto.subjects.map(_.sourceSubject.semanticTargetId.value) shouldBe Vector("e-mono", "e-koto")
    value.useCaseCommunication.subjects.head.sourceSubject.flows.head.steps.map(_.semanticRelationshipId.value) shouldBe Vector("r-usecase-step")
    value.entityModel.subjects.head.metadata.map(_.sourceMetadata.semanticRelationshipId.value) shouldBe Vector("r-entity-metadata")
    value.eventModel.assertions.map(_.sourceAssertion.semanticAssertionId.value) shouldBe Vector("r-event-cause")
    value.structureView.relations.head.assertions.map(_.sourceAssertion.semanticRelationshipId.value) shouldBe Vector("r-structure")
    value.classificationView.dimensions.head.assertions.map(_.sourceAssertion.semanticAssertionId.value) shouldBe Vector("r-classification-assertion")
    value.workflow.flows.map(_.sourceFlow.semanticFlowId.value) shouldBe Vector("r-workflow-flow")
    value.stateMachine.transitions.map(_.sourceTransition.semanticTransitionId.value) shouldBe Vector("r-state-transition")
    value.stateMachine.transitionAdjuncts.map(_.sourceTransitionAdjunct.semanticAdjunctRelationId.value) shouldBe Vector("r-state-adjunct")
  }
}
