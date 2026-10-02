package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import com.sun.jna.{Native, NativeLong, Platform}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Fresh actual-human application with exact evidence and pending validation.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
final class InternalModelCmlChangeApplicationSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelCmlChangeApplication.*
  import InternalModelCandidateHumanApprovalValidatorSpec.{artifactReference, recordReference}
  import InternalModelCmlChangeEligibility.*

  private val _application_reference = recordReference("explicit-cml-application", 601L)
  private val _paths = Vector("cml/alpha.cml", "cml/beta.cml")
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(minSuccessful = 8)

  "Rooted CML application" should {
    "explicit application reference" which {
      "return structured failure for missing null-field malformed-Unicode or nonpositive reference without effects" in {
        Given("an actual owned fixture and independently supplied malformed explicit application references")
        InternalModelCmlChangeFixture.withFixture() { fixture =>
          val before = _bytes(fixture.root)
          val references = Vector[InternalModelRecordReference](null,
            InternalModelRecordReference(null.asInstanceOf[InternalModelRecordId], _application_reference.recordRevision),
            InternalModelRecordReference(InternalModelRecordId.from("\ud800").toOption.get, _application_reference.recordRevision),
            InternalModelRecordReference(_application_reference.recordId, 0L.asInstanceOf[InternalModelRecordRevision]))
          When("each malformed reference reaches the private rooted application entry")
          val results = references.map(applyApproved(fixture.root, fixture.request, _))
          Then("every result is a structured failure with no physical target effect")
          results.foreach(_.isSuccess shouldBe false)
          results.foreach(_.toOption shouldBe None)
          _bytes(fixture.root) shouldBe before
        }
      }
    }
  }

  if (Platform.isMac && Native.POINTER_SIZE == 8 && NativeLong.SIZE == 8) {
    "Rooted CML application" should {
      "exact actual-human and source-owner inputs" which {
        "write both files and retain the complete freshly evaluated gate as pending validation" in {
          Given("actual independent human approval and exact owner authority for two existing canonical targets")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val expected = InternalModelCmlChangeGate.evaluate(fixture.root, fixture.request).toOption.get
            val cursorpath = fixture.root.resolve("src/main/internal-model/resume.yaml")
            val cursor = Files.readAllBytes(cursorpath).toVector
            When("the private rooted application entry freshly admits and applies the exact candidate")
            val result = applyApproved(fixture.root, fixture.request, _application_reference)
            Then("both real payloads are applied with complete evidence and no claim of validation or cursor acceptance")
            result.isSuccess shouldBe true
            val report = result.toOption.get
            report.applicationreference shouldBe _application_reference
            report.disposition shouldBe Disposition.AppliedPendingValidation
            report.gate shouldBe expected
            report.gate.originalapproval shouldBe fixture.request.originalapproval
            report.gate.currentreview shouldBe fixture.request.currentreview
            val plan = report.gate.plan.get
            plan.subject shouldBe fixture.request.currentreview.binding.subject
            plan.authority shouldBe fixture.request.mutationauthority.get
            plan.packagereference shouldBe fixture.request.currentrequest.packagereference
            plan.scope shouldBe fixture.request.currentrequest.scope
            plan.candidateartifactreference shouldBe fixture.request.currentrequest.candidateartifact.get
            plan.candidatereference shouldBe fixture.data.expected.basis.candidateReference
            plan.realizationartifactreference shouldBe fixture.data.reviewbinding.realizationArtifactReference
            plan.realizationreference shouldBe fixture.request.currentrequest.realizationreference
            plan.continuityartifactreference shouldBe fixture.data.reviewbinding.continuityArtifactReference
            plan.continuityreference shouldBe fixture.request.currentreview.semanticDiffAdmission.candidateAdmission.continuity.binding.bindingReference
            plan.semanticdiffartifactreference shouldBe fixture.request.currentrequest.semanticdiffartifact.get
            plan.semanticdiffreference shouldBe fixture.data.expected.basis.semanticDiffReference
            plan.reviewartifactreference shouldBe fixture.request.currentrequest.reviewartifact.get
            plan.reviewreference shouldBe fixture.data.expected.basis.reviewReference
            plan.approvalartifactreference shouldBe fixture.request.currentrequest.approvalartifact.get
            plan.approvalreference shouldBe fixture.data.expected.approvalReference
            plan.actualapproval.record.approval shouldBe fixture.data.expected
            plan.targets.map(_.target) shouldBe fixture.request.currentreview.semanticDiffAdmission.candidateAdmission.projection.targets
            plan.targets.map(_.payload) shouldBe fixture.request.currentreview.semanticDiffAdmission.candidateAdmission.targetBytes
            plan.targets.map(_.nextsourcerevision) shouldBe Vector("approved-next-alpha", "approved-next-beta")
            plan.actualapproval.reviewAdmission.binding.targets.head.reviewSnapshot.limitations shouldBe Vector("snapshot limitation", "snapshot limitation")
            plan.actualapproval.reviewAdmission.semanticDiffAdmission.candidateAdmission.continuity.binding.views.size shouldBe 8
            report.writes.get.failure shouldBe None
            report.writes.get.cleanupfailures shouldBe empty
            report.writes.get.outcomes.map(_.disposition) shouldBe Vector.fill(2)(NativeCmlFileWriter.Disposition.Applied)
            _bytes(fixture.root) shouldBe plan.targets.map(_.payload.proposedRawBytes)
            Files.readAllBytes(cursorpath).toVector shouldBe cursor
          }
        }

        "retain independently supplied opaque source versions and unrelated positive reference revisions" in {
          Given("independent candidate artifact, logical record, subject and application revisions plus opaque owner next versions")
          forAll(Gen.chooseNum(1L, 1000000L), Gen.chooseNum(1L, 1000000L),
            Gen.chooseNum(1L, 1000000L), Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)) { (artifactrevision, recordrevision, applicationrevision, identity) =>
            val options = InternalModelDurableHandoffFixture.approvedOptions.copy(candidateartifactrevision = artifactrevision,
              candidaterevision = recordrevision, subjectrevision = applicationrevision)
            InternalModelCmlChangeFixture.withFixture(options) { fixture =>
              val versions = Vector("owner-z-" + identity, "owner-a-" + identity)
              val authority = fixture.request.mutationauthority.get.copy(targets = fixture.request.mutationauthority.get.targets.zip(versions)
                .map { case (target, version) => target.copy(nextsourcerevision = version) })
              val reference = recordReference("application-" + identity, applicationrevision)
              val request = fixture.request.copy(mutationauthority = Some(authority))
              When("the real rooted application receives the explicit unrelated reference and owner versions")
              val report = applyApproved(fixture.root, request, reference).toOption.get
              Then("all supplied values survive exactly without numeric ordering or source-version allocation")
              report.disposition shouldBe Disposition.AppliedPendingValidation
              report.applicationreference shouldBe reference
              report.gate.plan.get.targets.map(_.nextsourcerevision) shouldBe versions
              report.gate.plan.get.candidateartifactreference.artifactRevision.value shouldBe artifactrevision
              report.gate.plan.get.candidatereference.recordRevision.value shouldBe recordrevision
              report.gate.plan.get.subject.subjectRevision.value shouldBe applicationrevision
              _bytes(fixture.root) shouldBe report.gate.plan.get.targets.map(_.payload.proposedRawBytes)
            }
          }
        }
      }

      "fresh noneligible admission" which {
        "reject missing or nonexact actual human and retain every gate cause without writes" in {
          Given("a stored actual approval and independent current human fields that are missing or do not match")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val before = _bytes(fixture.root)
            val variants = Vector(fixture.request.currentrequest.copy(humandecision = None),
              fixture.request.currentrequest.copy(humandecision = Some(fixture.data.expected.copy(actor = fixture.data.expected.actor.copy(identity = "another-human")))),
              fixture.request.currentrequest.copy(humandecision = Some(fixture.data.expected.copy(decision = InternalModelCandidateHumanApprovalDecision.Rejected))))
            val requests = variants.map(current => fixture.request.copy(currentrequest = current))
            val expected = requests.map(InternalModelCmlChangeGate.evaluate(fixture.root, _).toOption.get)
            When("rooted application independently readmits each current human request")
            val reports = requests.map(applyApproved(fixture.root, _, _application_reference).toOption.get)
            Then("all fresh causes survive as rejected application and stored approval grants no write")
            reports.map(_.gate) shouldBe expected
            reports.map(_.gate.eligibility) shouldBe Vector(Incomplete, Inconsistent, Inconsistent)
            reports.foreach(_rejected)
            reports.head.gate.continuation.problems.map(_.dimension) should contain("humandecision")
            reports.tail.foreach(_.gate.continuation.problems.map(_.dimension) should contain("approval.admission"))
            _bytes(fixture.root) shouldBe before
          }
        }

        "reject actual denied decisions and blocking issues without discarding admitted evidence" in {
          Given("actual rejected or changes-requested human decisions and a separately blocking issue")
          Vector(InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested).foreach { decision =>
            InternalModelCmlChangeFixture.withFixture(InternalModelDurableHandoffFixture.approvedOptions.copy(decision = decision)) { fixture =>
              val before = _bytes(fixture.root)
              When("the rooted application admits an actual decision withholding approval")
              val report = applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
              Then("the exact denied decision remains blocked and the files remain original")
              _rejected(report)
              report.gate.eligibility shouldBe Blocked
              report.gate.originalapproval.record.approval.decision shouldBe decision
              report.gate.continuation.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.HumanDecisionNotApproved)
              _bytes(fixture.root) shouldBe before
            }
          }
          InternalModelCmlChangeFixture.withFixture(blocking = true) { fixture =>
            val before = _bytes(fixture.root)
            When("the rooted application admits the current semantic-approval-blocking issue")
            val report = applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
            Then("the complete blocking issue remains observable and no target is written")
            _rejected(report)
            report.gate.eligibility shouldBe Blocked
            report.gate.continuation.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.BlockingIssue)
            report.gate.continuation.openissues.head.admission.ledger.issues.head.conditions shouldBe Vector("condition", "condition")
            _bytes(fixture.root) shouldBe before
          }
        }

        "reject missing decisions mappings and superseded required decision with complete causes" in {
          Given("explicit missing decision/mapping prerequisites and an actual superseded decision ledger in a newly owned fixture")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val before = _bytes(fixture.root)
            val missing = fixture.request.copy(currentrequest = fixture.request.currentrequest.copy(requireddecisions = None, requiredmappings = None))
            When("the rooted application admits missing current decisions and mappings")
            val incomplete = applyApproved(fixture.root, missing, _application_reference).toOption.get
            Then("both prerequisites survive as rejected with no file effects")
            _rejected(incomplete)
            incomplete.gate.eligibility shouldBe Incomplete
            incomplete.gate.continuation.problems.map(_.dimension) should contain allOf("requireddecisions", "requiredmappings")
            _bytes(fixture.root) shouldBe before
            Given("the actual package decision ledger explicitly supersedes the required current decision")
            val ledger = InternalModelDurableHandoffFixture.decisionLedger(fixture.data)
            val predecessor = ledger.records.head.copy(state = InternalModelDecisionState.Superseded)
            val successor = ledger.records.head.copy(decisionReference = recordReference("decision-successor", 521L), supersedes = Some(predecessor.decisionReference))
            val path = fixture.handoff.artifacts.find(_.context.reference == artifactReference("decision-main")).get.context.path
            Files.write(fixture.root.resolve("src/main/internal-model").resolve(path),
              InternalModelDecisionRecordCodec.encode(ledger.copy(records = Vector(predecessor, successor))))
            When("the fresh rooted application reads the current actual decision ledger")
            val blocked = applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
            Then("the explicit supersession rejects application without selecting a sibling decision")
            _rejected(blocked)
            blocked.gate.continuation.problems.map(_.kind) should contain(InternalModelContinuationProblemKind.DecisionMismatch)
            _bytes(fixture.root) shouldBe before
          }
        }

        "reject the explicit superseded original approval without selecting its successor" in {
          Given("an actual separately admitted successor and explicit approval supersession link")
          InternalModelCmlChangeFixture.withPair(InternalModelDurableHandoffFixture.approvedOptions.copy(approvalartifactid = "approval-successor",
            approvalartifactrevision = 383L, approvalidentity = "approval-successor", approvalrevision = 389L)) { (original, successor) =>
            val before = _bytes(original.root)
            val link = InternalModelCandidateApprovalSupersessionInput(InternalModelCandidateApprovalSupersession(
              original.request.originalapproval.approvalArtifactReference, successor.request.originalapproval.approvalArtifactReference,
              "ccdm-candidate-approval-supersession-v2", "2.0"), successor.request.originalapproval)
            When("the rooted application freshly evaluates the explicitly superseded predecessor")
            val report = applyApproved(original.root, original.request.copy(supersession = Some(link)), _application_reference).toOption.get
            Then("the complete link and predecessor remain blocked with no physical writes")
            _rejected(report)
            report.gate.eligibility shouldBe Blocked
            report.gate.supersession shouldBe Some(link)
            report.gate.lifecycle.get.supersession shouldBe Some(link)
            report.gate.originalapproval shouldBe original.request.originalapproval
            _bytes(original.root) shouldBe before
          }
        }

        "reject missing authority and root owner path or target-set contradictions" in {
          Given("a complete current human request and explicitly missing or contradictory owner authority")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val before = _bytes(fixture.root)
            val authority = fixture.request.mutationauthority.get
            val variants = Vector(None, Some(authority.copy(projectroot = fixture.root.resolve("other").toString)),
              Some(authority.copy(targets = authority.targets.tail)),
              Some(authority.copy(targets = authority.targets.updated(0, authority.targets.head.copy(sourceidentity = "foreign-owner")))),
              Some(authority.copy(targets = authority.targets.updated(0, authority.targets.head.copy(projectrelativepath = "cml/renamed.cml")))))
            val requests = variants.map(value => fixture.request.copy(mutationauthority = value))
            val expected = requests.map(InternalModelCmlChangeGate.evaluate(fixture.root, _).toOption.get)
            When("the private rooted entry freshly checks exact authority ownership")
            val reports = requests.map(applyApproved(fixture.root, _, _application_reference).toOption.get)
            Then("every complete cause remains rejected and no authority is inferred or repaired")
            reports.map(_.gate) shouldBe expected
            reports.map(_.gate.eligibility) shouldBe Vector(Incomplete, Inconsistent, Inconsistent, Inconsistent, Inconsistent)
            reports.foreach(_rejected)
            reports.flatMap(_.gate.problems.map(_.kind)) should contain allOf(InternalModelCmlChangeProblemKind.MissingAuthority,
              InternalModelCmlChangeProblemKind.AuthorityRootMismatch, InternalModelCmlChangeProblemKind.AuthorityTargetMismatch,
              InternalModelCmlChangeProblemKind.AuthorityOwnerMismatch)
            _bytes(fixture.root) shouldBe before
          }
        }

        "reject changed and missing declared owner versions after a previous eligible capture" in {
          Given("a previous eligible report and independently supplied changed or missing current owner versions")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val before = _bytes(fixture.root)
            val previous = InternalModelCmlChangeGate.evaluate(fixture.root, fixture.request).toOption.get
            val live = Vector(Some("changed-owner-version"), None).map(version =>
              fixture.request.livesources.updated(artifactReference("snapshot-cml-alpha"),
                InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", version, "cml/alpha.cml")))
            When("application reevaluates each explicit current owner request instead of consuming previous eligibility")
            val reports = live.map(sources => applyApproved(fixture.root, fixture.request.copy(livesources = sources), _application_reference).toOption.get)
            Then("changed and unknown source versions remain separate causes and neither reuses cached permission")
            previous.eligibility shouldBe EligibleForSkillApplication
            reports.map(_.gate.eligibility) shouldBe Vector(ReReviewRequired, Incomplete)
            reports.foreach(_rejected)
            reports.head.gate.lifecycle.get.invalidations.map(_.kind) should contain(InternalModelCandidateApprovalInvalidationKind.SourceChanged)
            reports(1).gate.livesources.collect { case entry if entry.reference == artifactReference("snapshot-cml-alpha") => entry.result }
              .head.asInstanceOf[InternalModelPackageFreshnessResult.Compared].report.missingDimensionNames should contain("observed.source.revision")
            _bytes(fixture.root) shouldBe before
          }
        }

        "reject target deletion or symlink substitution after previous eligibility" in {
          Given("a prior eligible report and an actual later deletion or symbolic target substitution")
          Vector("deleted", "symbolic").foreach { kind =>
            InternalModelCmlChangeFixture.withFixture() { fixture =>
              val before = _bytes(fixture.root)
              val previous = InternalModelCmlChangeGate.evaluate(fixture.root, fixture.request).toOption.get
              val target = fixture.root.resolve("cml/beta.cml")
              val outside = fixture.root.resolve("outside.cml")
              Files.write(outside, before(1).toArray)
              Files.delete(target)
              if (kind == "symbolic") Files.createSymbolicLink(target, outside)
              When("application takes a fresh rooted capture and observes current physical CML availability")
              val report = applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
              Then("prior eligibility cannot authorize a write and outside plus first-target bytes survive")
              previous.eligibility shouldBe EligibleForSkillApplication
              _rejected(report)
              report.gate.eligibility shouldBe (if (kind == "deleted") Incomplete else Inconsistent)
              report.gate.livesources.find(_.reference == artifactReference("snapshot-cml-beta")).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.status shouldBe (if (kind == "deleted") InternalModelSnapshotFreshnessStatus.Unavailable else InternalModelSnapshotFreshnessStatus.Malformed)
                case _ => fail("fresh native source result is required")
              }
              Files.readAllBytes(fixture.root.resolve("cml/alpha.cml")).toVector shouldBe before.head
              Files.readAllBytes(outside).toVector shouldBe before(1)
              if (kind == "deleted") Files.exists(target) shouldBe false
            }
          }
        }
      }

      "real physical failure evidence" which {
        "report failed all-target native preflight with zero effects despite eligible semantic admission" in {
          Given("eligible semantic source ownership and a second target with an additional physical link")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val before = _bytes(fixture.root)
            val alias = fixture.root.resolve("outside.cml")
            Files.createLink(alias, fixture.root.resolve("cml/beta.cml"))
            When("fresh semantic admission is followed by complete native all-target preflight")
            val report = applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
            Then("application is Failed with a complete eligible gate and no attempted file change")
            report.applicationreference shouldBe _application_reference
            report.disposition shouldBe Disposition.Failed
            report.gate.eligibility shouldBe EligibleForSkillApplication
            report.gate.plan should not be empty
            report.writes.get.failure.get.kind shouldBe NativeCmlFileWriter.FailureKind.Malformed
            report.writes.get.outcomes.map(_.disposition) shouldBe Vector.fill(2)(NativeCmlFileWriter.Disposition.NotAttempted)
            report.writes.get.outcomes.forall(outcome => !outcome.mayhavechanged && outcome.byteswritten == 0L) shouldBe true
            _bytes(fixture.root) shouldBe before
            Files.readAllBytes(alias).toVector shouldBe before(1)
          }
        }

        "report a real applied prefix and failed second write without pending validation or cursor mutation" in {
          Given("a reviewed actual request and a forwarding decorator failing only the second target write")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            var calls = 0
            val failure = NativeCmlFileWriter.Failure(NativeCmlFileWriter.FailureKind.IoFailure, Some(5), "second-write")
            val operations = new NativeCmlFileWriter.WriteOperations {
              override def truncate(fd: Int): Either[NativeCmlFileWriter.Failure, Unit] = NativeCmlFileWriter.nativeOperations.truncate(fd)
              override def write(fd: Int, bytes: Vector[Byte]): Either[NativeCmlFileWriter.Failure, Long] = {
                calls += 1
                if (calls == 2) Left(failure) else NativeCmlFileWriter.nativeOperations.write(fd, bytes)
              }
              override def flush(fd: Int): Either[NativeCmlFileWriter.Failure, Unit] = NativeCmlFileWriter.nativeOperations.flush(fd)
            }
            val cursorpath = fixture.root.resolve("src/main/internal-model/resume.yaml")
            val cursor = Files.readAllBytes(cursorpath).toVector
            When("the actual rooted gate and native writer forward the first effect then encounter the second syscall failure")
            val report = applyApprovedWithOperations(fixture.root, fixture.request, _application_reference, operations).toOption.get
            Then("the exact plan, primary failure and actual partial bytes survive as Failed with unchanged cursor")
            report.disposition shouldBe Disposition.Failed
            report.applicationreference shouldBe _application_reference
            report.gate.plan.get.actualapproval.record.approval shouldBe fixture.data.expected
            report.gate.plan.get.targets.map(_.nextsourcerevision) shouldBe Vector("approved-next-alpha", "approved-next-beta")
            report.writes.get.failure shouldBe Some(failure)
            report.writes.get.cleanupfailures shouldBe empty
            report.writes.get.outcomes.map(_.disposition) shouldBe Vector(NativeCmlFileWriter.Disposition.Applied, NativeCmlFileWriter.Disposition.Failed)
            report.writes.get.outcomes.map(_.byteswritten) shouldBe Vector(3L, 0L)
            report.writes.get.outcomes.map(_.mayhavechanged) shouldBe Vector(true, true)
            _bytes(fixture.root) shouldBe Vector(report.gate.plan.get.targets.head.payload.proposedRawBytes, Vector.empty)
            Files.readAllBytes(cursorpath).toVector shouldBe cursor
          }
        }

        "propagate malformed rooted request failure without physical effects" in {
          Given("an actual fixture with a malformed current authority reference")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val before = _bytes(fixture.root)
            val malformed = fixture.request.copy(mutationauthority = Some(fixture.request.mutationauthority.get.copy(requestreference = null)))
            When("fresh rooted gate validation rejects the structurally malformed request")
            val result = applyApproved(fixture.root, malformed, _application_reference)
            Then("the structured gate failure propagates without manufacturing an application report")
            result.isSuccess shouldBe false
            result.toOption shouldBe None
            _bytes(fixture.root) shouldBe before
          }
        }
      }
    }
  } else {
    "Rooted CML application" should {
      "unsupported physical source boundary" which {
        "reject current CML observation explicitly without physical effects on this unsupported host" in {
          Given("the actual host cannot supply the Darwin LP64 physical source boundary")
          InternalModelCmlChangeFixture.withFixture() { fixture =>
            val before = _bytes(fixture.root)
            When("the rooted application requires fresh physical CML observation")
            val report = applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
            Then("malformed current source evidence rejects application and no native write is claimed")
            _rejected(report)
            report.gate.eligibility shouldBe Inconsistent
            _bytes(fixture.root) shouldBe before
          }
        }
      }
    }
  }

  private def _bytes(root: Path): Vector[Vector[Byte]] = _paths.map(path => Files.readAllBytes(root.resolve(path)).toVector)
  private def _rejected(report: ApplicationReport): Unit = {
    report.disposition shouldBe Disposition.Rejected
    report.writes shouldBe None
    report.gate.plan shouldBe None
  }
}
