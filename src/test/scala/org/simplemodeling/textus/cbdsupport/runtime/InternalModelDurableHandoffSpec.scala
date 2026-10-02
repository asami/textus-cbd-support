package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path}
import scala.jdk.CollectionConverters.*
import io.circe.{Json, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Actual private local Git stop/transfer/fresh-JVM recorded-action specification.
 * Requires the parent's selected Test / fork := true runtime classpath.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
final class InternalModelDurableHandoffSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelContinuationEligibility.*
  import InternalModelContinuationProblemKind.*
  import InternalModelCandidateHumanApprovalValidatorSpec.{FixtureOptions, artifactReference, approvalRecord, recordReference}
  import InternalModelDurableHandoffSupport.{ChildEvidence, Transfer}
  private val _fixture = InternalModelDurableHandoffFixture
  private val _core = InternalModelContinuationFixture
  private val _support = InternalModelDurableHandoffSupport
  private val _transport = InternalModelDurableHandoffInput
  private val _reports = InternalModelFreshProcessProbe

  "Durable recorded-action handoff" should {
    "retain substantive recorded models across actual Git transfer" which {
      "D1 stop a producer and rehydrate exact source-backed all-eight inspection in an independent checkout" in {
        Given("a model-ready package and independently declared package scope and empty decision requirements")
        _fixture.withCore() { (root, request) =>
          val before = _core.treeBytes(root)
          val continuity = InternalModelProjectionContinuityValidator.validate(root).toOption.get
          val expected = _report(root, request)
          When("a terminal producer JVM commits its package and transfers the native revision before removal and fresh reevaluation")
          _support.withTransfer(root, request) { transfer =>
            val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the exact Git revision and paths and complete producer/consumer reports preserve independent all-eight meaning")
            _git_evidence(transfer)
            _child(transfer.initial, "eligible", 0)
            _complete_child(child, expected)
            child.pid should not be transfer.initial.pid
            _result(child) shouldBe _result(transfer.initial)
            expected.eligibility shouldBe Eligible
            expected.action shouldBe Some(InternalModelContinuationAction.InspectProjections)
            expected.state.continuity shouldBe continuity
            _core_witnesses(continuity)
            expected.state.artifacts.find(_.context.reference.artifactId.value == "raw-approval").flatMap(_.bytes) shouldBe
              Some("raw unadmitted approval evidence\n".getBytes(StandardCharsets.UTF_8).toVector)
            expected.state.artifacts.find(_.context.reference.artifactId.value == "optional-source").flatMap(_.bytes) shouldBe None
            _package_only(transfer)
            _core.treeBytes(root) shouldBe before
          }
        }
      }

      "D2 retain exact opaque identities and all-eight values through four generated labels and opposite encounter permutations" in {
        val labels = Gen.choose(1, 16).flatMap(size => Gen.listOfN(size, Gen.alphaNumChar).map(_.mkString))
        forAll(labels, Gen.oneOf(true, false), minSuccessful(4)) { (label, reverse) =>
          Given("a generated display label and opposite source encounter permutations with fixed independently declared references")
          _fixture.withCore(label, reverseInputs = reverse) { (root, request) =>
            _fixture.withCore(label, reverseInputs = !reverse) { (sibling, independent) =>
              val expected = _report(sibling, independent)
              val continuity = InternalModelProjectionContinuityValidator.validate(sibling).toOption.get
              When("the generated package crosses actual Git commit and distinct producer/consumer processes")
              _support.withTransfer(root, request) { transfer =>
                val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
                Then("every complete value agrees with independent opposite-order admission while labels never become identities")
                _git_evidence(transfer)
                _complete_child(child, expected)
                child.pid should not be transfer.initial.pid
                _result(child) shouldBe _result(transfer.initial)
                expected.eligibility shouldBe Eligible
                expected.action shouldBe Some(InternalModelContinuationAction.InspectProjections)
                expected.state.continuity shouldBe continuity
                continuity.realization.elements.map(_.label).distinct shouldBe Vector(label)
                continuity.realization.elements.map(_.identity).sorted shouldBe Vector("e-activity", "e-command", "e-dimension", "e-entity", "e-event",
                  "e-flow", "e-koto", "e-mono", "e-state-a", "e-state-b", "e-state-machine", "e-step", "e-trigger", "e-usecase", "e-workflow")
                _core_witnesses(continuity)
              }
            }
          }
        }
      }

      "D3 preserve rich two-target binary candidate diff review human and ledger evidence for exact approved handoff" in {
        Given("independent approved human input with empty unresolved items and a complete two-target review basis")
        val options = _fixture.approvedOptions.copy(allnullablefacets = true, reviewstate = "approved", includeoptionalsnapshot = true)
        _fixture.withRich(options) { (root, data, request) =>
          val before = _core.treeBytes(root)
          val admission = InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main", options),
            artifactReference("review-main", options), data.executionbasis, data.expected).toOption.get
          val expected = _report(root, request)
          When("only the inventoried package is committed and transferred after the producer JVM stops")
          _support.withTransfer(root, request) { transfer =>
            val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the complete fresh gate report preserves exact independent human admission and ordinary evidence without CML effects")
            _complete_child(child, expected)
            _result(child) shouldBe _result(transfer.initial)
            expected.action shouldBe Some(InternalModelContinuationAction.HandoffApprovedCandidate)
            expected.approval shouldBe Some(admission)
            expected.review.get.binding shouldBe data.reviewbinding
            val candidate = expected.candidate.get
            candidate.targetBytes.map(_.proposedRawBytes) shouldBe Vector(Vector[Byte](0, -1, 10), "beta cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector)
            candidate.projection.targets.map(_.targetId) shouldBe Vector("target-alpha", "target-beta")
            candidate.projection.targets.flatMap(_.mappings).map(value => (value.mappingId, value.semanticIdentityKind, value.semanticIdentity)) shouldBe
              Vector(("mapping-alpha", "element", "opaque-shared"), ("mapping-beta", "relationship", "opaque-shared"))
            candidate.continuity.binding.views.map(_.family) shouldBe _families
            expected.decisions.head.admission.ledger shouldBe _fixture.decisionLedger(data)
            expected.openissues.head.admission.ledger shouldBe _fixture.issueLedger(data, blocking = false)
            expected.review.get.binding.targets.map(_.reviewSnapshot.condition.ambiguity) shouldBe Vector(None, None)
            expected.review.get.binding.targets.head.reviewSnapshot.limitations shouldBe Vector("snapshot limitation", "snapshot limitation")
            _git_evidence(transfer)
            _package_only(transfer)
            _core.treeBytes(root) shouldBe before
            before("src/main/cml/original.cml") shouldBe Vector[Byte](0, -1, 10)
          }
        }
      }
    }

    "reevaluate actual consumer changes and independent inputs" which {
      "D4 reject missing required snapshot and retain null consumed source revision as attributed Incomplete" in {
        Given("a positive producer package with independently known source and control versions")
        _fixture.withCore() { (root, request) =>
          When("the transferred consumer first loses a required snapshot and then receives explicit null source versions")
          _support.withTransfer(root, request) { transfer =>
            val path = _core.packageRoot(transfer.consumer).resolve("snapshots/model.json")
            val snapshot = Files.readAllBytes(path).toVector
            _support.removeOwned(transfer.work, path)
            val absent = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            _support.writeOwned(transfer.work, path, snapshot)
            _change_json(transfer, "snapshot-model", _null_source_versions)
            _change_json(transfer, "realization-main", _null_source_versions)
            val expected = _report(transfer.consumer, request)
            val unknown = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("absence remains original structured rejection while missing source.revision retains the exact snapshot reference")
            _rejected(absent, "required artifact snapshot-model is absent")
            _complete_child(unknown, expected)
            expected.eligibility shouldBe Incomplete
            expected.action shouldBe None
            expected.problems.filter(_.dimension == "source.revision").map(value => (value.kind, value.artifactreference)) shouldBe
              Vector((MissingPrerequisite, Some(_core.snapshotReference)))
            expected.state.packageContext.revision shouldBe 31L
            _core.snapshotReference.artifactRevision.value shouldBe 11L
          }
        }
      }

      "D5 refuse unsupported mismatched or missing actions and independently true condition or blocker claims" in {
        Given("a positive transferred inspection cursor whose prose and previous report cannot grant a different action")
        _fixture.withCore() { (root, request) =>
          When("the consumer records unsupported mismatched absent actions and a true predicate with the wrong owner")
          _support.withTransfer(root, request) { transfer =>
            val cursor = _core.cursor(transfer.consumer)
            val variants = Vector(
              (cursor.copy(nextPermittedAction = Some("execute-cml")), Inconsistent, UnsupportedAction, "nextPermittedAction"),
              (cursor.copy(nextPermittedAction = Some("review-candidate")), Inconsistent, StageActionMismatch, "stage/action"),
              (cursor.copy(nextPermittedAction = None), Incomplete, MissingPrerequisite, "nextPermittedAction"),
              (cursor.copy(preconditions = Vector(InternalModelResumeCondition("model-admitted", "approved by prose", Some("snapshot-model")))), Inconsistent, ConditionOwnerMismatch, "condition.artifactId"))
            variants.foreach { case (changed, eligibility, kind, dimension) =>
              _replace_cursor(transfer, changed)
              When("a new consumer JVM reevaluates this exact recorded action or owner variation")
              val expected = _report(transfer.consumer, request)
              val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
              Then("each actual fresh consumer reports its exact noneligible action or condition dimension")
              _complete_child(child, expected)
              _problem(expected, eligibility, kind, dimension)
              expected.state.cursor shouldBe changed
            }
          }
        }
        Given("a transferred approved human package with a newly true semantic-approval blocker")
        _fixture.withRich() { (root, data, request) =>
          When("the consumer ledger and recorded blocker name actual independent issue evidence")
          _support.withTransfer(root, request) { transfer =>
            _replace(transfer, "open-issue-main", InternalModelOpenIssueRecordCodec.encode(_fixture.issueLedger(data, blocking = true)).toVector)
            val condition = InternalModelResumeCondition("approval-blocking-issues", "retained blocker", Some("open-issue-main"))
            _replace_cursor(transfer, _core.cursor(transfer.consumer).copy(blockers = Vector(condition)))
            val expected = _report(transfer.consumer, request)
            val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the actual true blocker suppresses action and retains exact condition/list attribution")
            _complete_child(child, expected)
            _problem(expected, Inconsistent, BlockingIssue, "issue.blocking.semanticApproval")
            expected.problems.flatMap(_.attribution) should contain(InternalModelContinuationConditionAttribution(InternalModelContinuationConditionList.Blockers, condition))
          }
        }
      }

      "D6 reject carrier and full selected artifact revision or role contradictions with original core diagnostics" in {
        Given("a valid stopped and transferred source-backed package with fixed declared selections")
        _fixture.withCore() { (root, request) =>
          When("the actual consumer cursor contradicts carrier or full artifact metadata")
          _support.withTransfer(root, request) { transfer =>
            val cursor = _core.cursor(transfer.consumer)
            val variants = Vector(
              cursor.copy(packageRevision = 32L) -> "resume package identity or revision",
              cursor.copy(selectedArtifacts = cursor.selectedArtifacts.map(value => if (value == _core.projectionReference)
                value.copy(artifactRevision = InternalModelArtifactRevision.from(24L).toOption.get) else value)) -> "ID, revision or role does not match inventory",
              cursor.copy(selectedArtifacts = cursor.selectedArtifacts.map(value => if (value == _core.projectionReference)
                value.copy(role = InternalModelArtifactRole.Decision) else value)) -> "ID, revision or role does not match inventory")
            variants.foreach { case (changed, diagnostic) =>
              _replace_cursor(transfer, changed)
              When("a new consumer JVM resolves this exact contradictory full declaration")
              val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
              Then("exact core admission rejects without selecting siblings latest records or defaults")
              _rejected(child, diagnostic)
            }
          }
        }
      }

      "D7 invalidate changed complete review subject or caller binding while allowing independent carrier-only advancement" in {
        Given("an approved rich package whose subject and caller execution basis are independently declared")
        _fixture.withRich() { (root, data, request) =>
          When("the transferred review subject changes and the original caller approval is reevaluated")
          _support.withTransfer(root, request) { transfer =>
            val changed = data.reviewbinding.copy(subject = data.reviewbinding.subject.copy(subjectRevision = InternalModelRecordRevision.from(777L).toOption.get))
            _replace(transfer, "review-main", InternalModelCandidateReviewBindingCodec.encode(changed).toVector)
            val subjectreport = _report(transfer.consumer, request)
            val subjectchild = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the original semantic owner retains the contradictory approval basis and no fallback action")
            _complete_child(subjectchild, subjectreport)
            _problem(subjectreport, Inconsistent, SemanticAdmissionFailed, "approval.admission")
            subjectreport.problems.find(_.dimension == "approval.admission").get.diagnostic should not be empty
            And("independent rules also cannot be supplied by stored provider evidence")
            _replace(transfer, "review-main", InternalModelCandidateReviewBindingCodec.encode(data.reviewbinding).toVector)
            val changedrequest = request.copy(executionbasis = Some(data.executionbasis.copy(rules = data.executionbasis.rules.map(_.copy(ruleVersion = "changed")))))
            _input(transfer, changedrequest)
            When("the consumer receives a valid but contradictory external execution basis")
            val bindingreport = _report(transfer.consumer, changedrequest)
            val bindingchild = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the review owner reports its original execution-basis diagnostic")
            _complete_child(bindingchild, bindingreport)
            _problem(bindingreport, Inconsistent, SemanticAdmissionFailed, "review.admission")
            bindingreport.problems.find(_.dimension == "review.admission").get.diagnostic should include("independently admitted execution basis")
            And("a separately allocated new carrier does not change the complete semantic subject")
            val manifestpath = _core.packageRoot(transfer.consumer).resolve("manifest.yaml")
            val manifest = _core.json(Files.readAllBytes(manifestpath).toVector)
            _support.writeOwned(transfer.work, manifestpath, _core.canonical(manifest.mapObject(_.add("revision", Json.fromLong(44L)))))
            _replace_cursor(transfer, _core.cursor(transfer.consumer).copy(packageRevision = 44L))
            val advanced = request.copy(carrierrevision = 44L)
            _input(transfer, advanced)
            When("the caller independently admits that carrier-only advancement")
            val expected = _report(transfer.consumer, advanced)
            val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the unchanged subject remains eligible without rebasing human approval")
            _complete_child(child, expected)
            expected.eligibility shouldBe Eligible
            expected.approval.get.record.approval shouldBe data.expected
            expected.review.get.binding.subject shouldBe data.reviewbinding.subject
          }
        }
      }

      "D8 retain independent None versus supplied empty requirements and reject contradictory actual human input" in {
        Given("valid stored approval with independent human basis decision and mapping inputs outside the package")
        _fixture.withRich() { (root, data, request) =>
          When("the transferred consumer receives missing independently required input fields")
          _support.withTransfer(root, request) { transfer =>
            val variants = Vector(request.copy(humandecision = None) -> "humandecision", request.copy(executionbasis = None) -> "executionbasis",
              request.copy(requireddecisions = None) -> "requireddecisions", request.copy(requiredmappings = None) -> "requiredmappings")
            variants.foreach { case (independent, dimension) =>
              _input(transfer, independent)
              When("a new consumer JVM evaluates this separately supplied incomplete caller request")
              val expected = _report(transfer.consumer, independent)
              val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
              Then("stored approval cannot fill the missing independent dimension")
              _complete_child(child, expected)
              _problem(expected, Incomplete, MissingPrerequisite, dimension)
            }
            And("a valid human rationale contradiction remains independent actual input")
            val mismatched = request.copy(humandecision = Some(data.expected.copy(rationale = "A different actual human statement.")))
            _input(transfer, mismatched)
            When("that well-formed contradictory human input crosses fresh production admission")
            val mismatchreport = _report(transfer.consumer, mismatched)
            val mismatchchild = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the human semantic owner rejects without using stored approval as input")
            _complete_child(mismatchchild, mismatchreport)
            _problem(mismatchreport, Inconsistent, SemanticAdmissionFailed, "approval.admission")
            And("explicitly supplied empty requirement sets retain all actual ledger and candidate owners")
            val empty = request.copy(requireddecisions = Some(Vector.empty), requiredmappings = Some(Vector.empty))
            _input(transfer, empty)
            When("empty requirements are independently supplied to the same rich consumer")
            val emptyreport = _report(transfer.consumer, empty)
            val emptychild = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("empty differs from None while complete successful evidence remains admitted")
            _complete_child(emptychild, emptyreport)
            emptyreport.eligibility shouldBe Eligible
            emptyreport.decisions.head.admission.ledger shouldBe _fixture.decisionLedger(data)
            emptyreport.candidate.get.projection.targets.map(_.targetId) shouldBe Vector("target-alpha", "target-beta")
            emptyreport.approval.get.record.approval shouldBe data.expected
          }
        }
      }

      "D9 retain matched nonapproved decisions duplicate unresolved items and semantic-approval blocking evidence" in {
        val decisions = Vector(InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested,
          InternalModelCandidateHumanApprovalDecision.Approved)
        decisions.foreach { decision =>
          Given("a matched actual human record with ordered duplicate unresolved items and provider-approved review")
          _fixture.withRich(FixtureOptions(decision = decision, reviewstate = "approved")) { (root, data, request) =>
            val expected = _report(root, request)
            When("this noneligible recorded evidence transfers through Git and a fresh consumer reevaluates it")
            _support.withTransfer(root, request) { transfer =>
              val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
              Then("complete human admission survives while provider state cannot manufacture an eligible action")
              _complete_child(child, expected)
              _problem(expected, Inconsistent, InternalModelContinuationProblemKind.HumanUnresolvedItems, "human.unresolvedItems")
              if (decision != InternalModelCandidateHumanApprovalDecision.Approved) {
                _problem(expected, Inconsistent, HumanDecisionNotApproved, "human.decision")
              }
              expected.approval.get.record shouldBe approvalRecord(data.expected)
              expected.approval.get.record.approval.unresolvedItems shouldBe Vector("follow-up", "later", "follow-up")
              _result(child) shouldBe _result(transfer.initial)
            }
          }
        }
        Given("an independently approved package with an actual semantic-approval blocking issue")
        _fixture.withRich(blocking = true) { (root, data, request) =>
          val expected = _report(root, request)
          When("the blocked package transfers and fresh admission retains its complete issue evidence")
          _support.withTransfer(root, request) { transfer =>
            val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("semanticApproval alone blocks the action while all issue annotations and other facets survive")
            _complete_child(child, expected)
            _problem(expected, Inconsistent, BlockingIssue, "issue.blocking.semanticApproval")
            expected.approval.get.record.approval shouldBe data.expected
            expected.openissues.head.admission.ledger shouldBe _fixture.issueLedger(data, blocking = true)
          }
        }
      }

      "D10 reject malformed or old cursor package and semantic evidence while admitting harmless current layout" in {
        Given("a positive transferred core package with current closed schemas")
        _fixture.withCore() { (root, request) =>
          When("the actual consumer receives malformed cursor or unsupported old core schemas")
          _support.withTransfer(root, request) { transfer =>
            val ids = Vector("resume-main", "snapshot-model", "realization-main", "projection-main")
            ids.foreach { id =>
              val original = _artifact_bytes(transfer, id)
              _replace(transfer, id, _core.canonical(_core.json(original).mapObject(_.add("schemaVersion", Json.fromString("1.0")))))
              When("a new consumer JVM admits this selected core artifact with an old schema")
              val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
              Then("the fresh core owner rejects unsupported schemas without a legacy reader")
              _rejected(child)
              _replace(transfer, id, original)
            }
            val cursorbytes = _artifact_bytes(transfer, "resume-main")
            _replace(transfer, "resume-main", "{malformed".getBytes(StandardCharsets.UTF_8).toVector)
            When("a new consumer JVM admits the actual malformed cursor")
            val malformed = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("malformed strict cursor evidence is production rejection rather than input harness failure")
            _rejected(malformed)
            _replace(transfer, "resume-main", cursorbytes)
            val path = _core.packageRoot(transfer.consumer).resolve("manifest.yaml")
            val manifest = Files.readAllBytes(path).toVector
            _support.writeOwned(transfer.work, path, _core.canonical(_core.json(manifest).mapObject(_.add("schemaVersion", Json.fromString("1.0")))))
            When("the actual manifest declares an unsupported old package schema")
            val oldpackage = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("the original package owner rejects the declared schema")
            _rejected(oldpackage)
            _support.writeOwned(transfer.work, path, manifest)
            And("current harmless whitespace and reversed key order retain their own raw payload")
            val layout = (" \t" + Printer.spaces2.print(InternalModelCandidateHumanApprovalValidatorSpec.reverseKeys(_core.json(cursorbytes))) + " \r\t")
              .getBytes(StandardCharsets.UTF_8).toVector
            _replace(transfer, "resume-main", layout)
            When("a new JVM reevaluates the current cursor presentation")
            val expected = _report(transfer.consumer, request)
            val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("layout is accepted with complete semantics and retained ordinary payload without byte controls")
            _complete_child(child, expected)
            expected.eligibility shouldBe Eligible
            expected.state.cursor shouldBe _core.cursor(root)
            expected.state.continuity shouldBe InternalModelProjectionContinuityValidator.validate(root).toOption.get
            expected.state.artifacts.find(_.context.reference.role == InternalModelArtifactRole.Resume).flatMap(_.bytes) shouldBe Some(layout)
          }
        }
        Given("a rich transferred package whose candidate diff review approval decision and issue owners have current schemas")
        _fixture.withRich() { (root, _, request) =>
          When("each actual rich semantic payload independently declares an unsupported old schema")
          _support.withTransfer(root, request) { transfer =>
            Vector("candidate-main" -> "semanticdiff.admission", "semantic-diff-main" -> "semanticdiff.admission",
              "review-main" -> "review.admission", "approval-main" -> "approval.admission",
              "decision-main" -> "decision.admission", "open-issue-main" -> "openissue.admission").foreach { case (id, dimension) =>
              val original = _artifact_bytes(transfer, id)
              _change_json(transfer, id, value => value.mapObject(_.add("schemaVersion", Json.fromString("1.0"))))
              When("a new consumer JVM invokes this selected rich artifact's actual semantic owner")
              val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
              if (id == "candidate-main" || id == "semantic-diff-main") {
                Then("the package owner rejects this projection artifact's unknown profile/schemaVersion pair")
                _rejected(child, s"projection artifact $id has an unknown profile/schemaVersion pair")
              } else {
                val expected = _report(transfer.consumer, request)
                Then("the original rich owner reports its explicit semantic admission failure and no action")
                _complete_child(child, expected)
                _problem(expected, Inconsistent, SemanticAdmissionFailed, dimension)
              }
              _replace(transfer, id, original)
            }
          }
        }
      }
    }

    "keep caller transport and process ownership explicit" which {
      "D11 roundtrip all twelve independent fields and separate harness three from production two and eligible zero" in {
        Given("independently constructed rich input and explicit None and empty requirement variants")
        _fixture.withRich() { (root, _, request) =>
          val variants = Vector(request, request.copy(humandecision = None, executionbasis = None, requireddecisions = None, requiredmappings = None),
            request.copy(requireddecisions = Some(Vector.empty), requiredmappings = Some(Vector.empty)))
          When("the closed test transport encodes and decodes each complete request")
          val decoded = variants.map(value => _transport.decode(_transport.encode(value)))
          Then("all twelve fields and None versus Some(empty) survive exactly without stored input synthesis")
          decoded shouldBe variants.map(Right(_))
          _core.json(_transport.encode(request)).asObject.get.keys.toSet shouldBe Set("packagereference", "carrierrevision", "realizationreference", "scope",
            "candidateartifact", "semanticdiffartifact", "reviewartifact", "approvalartifact", "executionbasis", "humandecision", "requireddecisions", "requiredmappings")
          And("missing extra duplicate wrong-type and malformed UTF-8 input are syntactic harness faults")
          val json = _core.json(_transport.encode(request))
          val invalid = Vector(
            _transport.bytes(json.mapObject(_.remove("humandecision"))),
            _transport.bytes(json.mapObject(_.add("extra", Json.True))),
            _transport.bytes(json.mapObject(_.add("requireddecisions", Json.obj()))),
            ("{\"carrierrevision\":43," + new String(_transport.encode(request).toArray, StandardCharsets.UTF_8).drop(1)).getBytes(StandardCharsets.UTF_8).toVector,
            Vector[Byte](0xc3.toByte, 0x28.toByte))
          When("each malformed external input is decoded and then presented to an actual fresh child")
          val transportresults = invalid.map(_transport.decode)
          _support.withTransfer(root, request) { transfer =>
            val eligible = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            val faults = invalid.map { bytes =>
              _support.writeOwned(transfer.work, transfer.input, bytes)
              _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            }
            _support.removeOwned(transfer.work, transfer.input)
            val missing = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            _input(transfer, request.copy(humandecision = None))
            val incomplete = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            Then("malformed or absent input produces attributed harness three while a valid missing human is production two")
            transportresults.foreach(_.isLeft shouldBe true)
            _child(eligible, "eligible", 0)
            (faults :+ missing).foreach { child =>
              _child(child, "harness-error", 3)
              _result(child) shouldBe Json.Null
              child.report.hcursor.downField("failure").get[String]("origin").toOption shouldBe Some("InternalModelDurableHandoffProbe")
              child.report.hcursor.downField("failure").get[String]("errorType").toOption.get should not be empty
              child.report.hcursor.downField("failure").get[String]("diagnostic").toOption.get should not be empty
            }
            _child(incomplete, "incomplete", 2)
            transfer.input.startsWith(transfer.consumer) shouldBe false
            transfer.paths should not contain "independent-input.json"
            transfer.consumerpaths shouldBe transfer.paths
          }
        }
      }

      "D12 consume only the independent checkout after actual producer absence with bounded private process ownership" in {
        Given("a substantive package whose source CML and retained caller state remain outside the committed inventory")
        _fixture.withCore() { (root, request) =>
          val before = _core.treeBytes(root)
          val expected = _report(root, request)
          When("the producer terminates and disappears before a fresh private consumer evaluates the transferred package")
          _support.withTransfer(root, request) { transfer =>
            val consumerbefore = _package_bytes(transfer.consumer)
            val child = _support.runProbe(transfer.work, transfer.consumer, transfer.input)
            val escape = intercept[IllegalArgumentException] {
              _support.writeOwned(transfer.work, root.resolve("out-of-scope"), Vector.empty)
            }
            Then("terminal distinct children and exact package-only paths establish the explicit state source without out-of-root mutation")
            _complete_child(child, expected)
            _git_evidence(transfer)
            _package_only(transfer)
            Vector(transfer.initial, child).foreach { evidence =>
              evidence.terminated shouldBe true
              ProcessHandle.of(evidence.pid).isPresent shouldBe false
              evidence.environment shouldBe Map.empty
              evidence.cwd.startsWith(transfer.work) shouldBe true
              evidence.home.startsWith(transfer.work) shouldBe true
              evidence.temporary.startsWith(transfer.work) shouldBe true
              Vector(evidence.cwd, evidence.home, evidence.temporary).distinct.size shouldBe 3
              Vector(evidence.cwd, evidence.home, evidence.temporary).foreach { path =>
                val files = Files.list(path)
                try files.count() shouldBe 0L finally files.close()
              }
            }
            child.pid should not be transfer.initial.pid
            child.cwd should not be transfer.initial.cwd
            child.home should not be transfer.initial.home
            child.temporary should not be transfer.initial.temporary
            escape.getMessage should include("strict descendant")
            _package_bytes(transfer.consumer) shouldBe consumerbefore
            _core.treeBytes(root) shouldBe before
            Files.exists(root.resolve("out-of-scope")) shouldBe false
          }
        }
      }
    }
  }

  private val _families = Vector("MonoKotoProjection", "UseCaseCommunicationProjection", "EntityModelProjection", "EventModelProjection",
    "StructureViewProjection", "ClassificationViewProjection", "WorkflowProjection", "StateMachineProjection")

  private def _report(root: Path, request: InternalModelContinuationRequest): InternalModelContinuationReport = {
    val result = InternalModelContinuationActionGate.evaluate(root, request)
    withClue(result.show) { result.isSuccess shouldBe true }
    result.toOption.get
  }
  private def _result(child: ChildEvidence): Json = child.report.hcursor.downField("result").focus.get
  private def _child(child: ChildEvidence, status: String, exitcode: Int): Unit = withClue(child.stderr) {
    child.terminated shouldBe true
    child.pid should not be ProcessHandle.current().pid()
    child.report.asObject.get.keys.toSet shouldBe Set("status", "pid", "result", "failure")
    child.report.hcursor.get[Long]("pid").toOption shouldBe Some(child.pid)
    child.report.hcursor.get[String]("status").toOption shouldBe Some(status)
    child.exitcode shouldBe exitcode
  }
  private def _complete_child(child: ChildEvidence, expected: InternalModelContinuationReport): Unit = {
    val status = expected.eligibility match { case Eligible => "eligible"; case Incomplete => "incomplete"; case Inconsistent => "inconsistent" }
    _child(child, status, if (expected.eligibility == Eligible) 0 else 2)
    child.report.hcursor.downField("failure").focus shouldBe Some(Json.Null)
    _result(child) shouldBe _reports.actionReport(expected)
  }
  private def _problem(report: InternalModelContinuationReport, eligibility: InternalModelContinuationEligibility,
    kind: InternalModelContinuationProblemKind, dimension: String): Unit = {
    report.eligibility shouldBe eligibility
    report.action shouldBe None
    report.problems.map(value => (value.kind, value.dimension)) should contain((kind, dimension))
  }
  private def _rejected(child: ChildEvidence, diagnostic: String = ""): Unit = {
    _child(child, "validator-rejected", 2)
    _result(child) shouldBe Json.Null
    val failure = child.report.hcursor.downField("failure")
    failure.focus.get.asObject.get.keys.toSet shouldBe Set("origin", "conclusionType", "diagnostic")
    failure.get[String]("origin").toOption shouldBe Some("InternalModelContinuationActionGate.evaluate")
    failure.get[String]("conclusionType").toOption.get should not be empty
    failure.get[String]("diagnostic").toOption.get should not be empty
    if (diagnostic.nonEmpty) failure.get[String]("diagnostic").toOption.get should include(diagnostic)
  }
  private def _git_evidence(transfer: Transfer): Unit = {
    transfer.commit should not be empty
    transfer.consumercommit shouldBe transfer.commit
    transfer.consumerpaths shouldBe transfer.paths
    transfer.alternatesabsent shouldBe true
    Files.exists(transfer.producer, LinkOption.NOFOLLOW_LINKS) shouldBe false
    transfer.paths should contain("project.yaml")
    transfer.paths should contain("src/main/internal-model/manifest.yaml")
    transfer.paths should contain("src/main/internal-model/resume.yaml")
  }
  private def _package_only(transfer: Transfer): Unit = {
    _package_bytes(transfer.consumer).keySet shouldBe transfer.paths.toSet
    transfer.paths.filterNot(_ == "project.yaml").forall(_.startsWith("src/main/internal-model/")) shouldBe true
    Vector("src/main/cml", "target", "chat", "provider", "retainedDB", ".codex", ".textus").foreach { name =>
      Files.exists(transfer.consumer.resolve(name)) shouldBe false
    }
  }
  private def _package_bytes(root: Path): Map[String, Vector[Byte]] = {
    val stream = Files.walk(root)
    try stream.iterator.asScala.filter(path => Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !path.startsWith(root.resolve(".git")))
      .map(path => root.relativize(path).toString -> Files.readAllBytes(path).toVector).toMap
    finally stream.close()
  }
  private def _input(transfer: Transfer, request: InternalModelContinuationRequest): Unit =
    _support.writeOwned(transfer.work, transfer.input, _transport.encode(request))
  private def _artifact_path(transfer: Transfer, id: String): Path = {
    val entry = _core.inventory(transfer.consumer).find(_.hcursor.get[String]("artifactId").toOption.contains(id)).get
    _core.packageRoot(transfer.consumer).resolve(entry.hcursor.get[String]("path").toOption.get)
  }
  private def _artifact_bytes(transfer: Transfer, id: String): Vector[Byte] = Files.readAllBytes(_artifact_path(transfer, id)).toVector
  private def _replace(transfer: Transfer, id: String, bytes: Vector[Byte]): Unit =
    _support.writeOwned(transfer.work, _artifact_path(transfer, id), bytes)
  private def _replace_cursor(transfer: Transfer, cursor: InternalModelResumeCursor): Unit =
    _replace(transfer, "resume-main", InternalModelResumeCursorCodec.encode(cursor))
  private def _change_json(transfer: Transfer, id: String, change: Json => Json): Unit =
    _replace(transfer, id, _core.canonical(change(_core.json(_artifact_bytes(transfer, id)))))
  private def _null_source_versions(value: Json): Json = value.asObject match {
    case Some(fields) => Json.fromFields(fields.toVector.map { case (key, item) =>
      key -> (if (key == "source" && item.isObject) item.mapObject(_.add("revision", Json.Null)) else _null_source_versions(item))
    })
    case None => value.asArray.map(items => Json.fromValues(items.map(_null_source_versions))).getOrElse(value)
  }
  private def _core_witnesses(value: InternalModelProjectionContinuity): Unit = {
    value.binding.views.map(_.family) shouldBe _families
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
    value.workflow.flows.head.sourceFlow.attribution shouldBe ComponentDashboardSourceAttribution("model-source", "model-authority", "anchor-r-workflow-flow-role-WorkflowProjectionFlowRelation")
    value.workflow.flows.head.sourceFlow.sourceEndpoint.attribution.sourceLocator shouldBe "anchor-e-workflow-kind-WorkflowProjectionWorkflow"
    value.realization.sourceReferences.map(_.snapshotReference).toSet shouldBe Set(_core.snapshotReference)
  }
}
