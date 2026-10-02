package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}
import java.util.Base64
import scala.jdk.CollectionConverters.*
import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 29, 2026
 *  version Sep. 29, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
/** Executable specification of independent human input and exact typed review binding. */
final class InternalModelCandidateHumanApprovalValidatorSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelCandidateHumanApprovalValidatorSpec.*

  "Candidate human approval admission" should {
    "filesystem admission" which {
      "retain a complete independently supplied Approved record and its exact full review basis" in {
        Given("an actual two-target V2 carrier with independently authored human input and all current semantic predecessors")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(includeoptionalsnapshot = true)) { (root, data) =>
          When("the exact external approval and review references cross actual admission")
          val result = InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main"), artifactReference("review-main"), data.executionbasis, data.expected)
          Then("all nine basis fields, two targets, ordinary binary CML, eight projections and optional absence survive")
          withClue(result.show) { result.isSuccess shouldBe true }
          val admitted = result.toOption.get
          admitted.record shouldBe approvalRecord(data.expected)
          admitted.approvalArtifactReference shouldBe artifactReference("approval-main")
          admitted.approvalArtifactPackageRelativePath shouldBe "approvals/main.json"
          admitted.reviewAdmission.binding shouldBe data.reviewbinding
          admitted.record.approval.basis.subject shouldBe data.reviewbinding.subject
          val candidate = admitted.reviewAdmission.semanticDiffAdmission.candidateAdmission
          candidate.targetBytes.map(_.proposedRawBytes) shouldBe Vector(Vector[Byte](0, -1, 10), "beta cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector)
          candidate.projection.targets.map(_.targetId) shouldBe Vector("target-alpha", "target-beta")
          candidate.continuity.binding.views.map(_.family) shouldBe Vector("MonoKotoProjection", "UseCaseCommunicationProjection", "EntityModelProjection", "EventModelProjection", "StructureViewProjection", "ClassificationViewProjection", "WorkflowProjection", "StateMachineProjection")
          Vector[Any](candidate.continuity.monoKoto, candidate.continuity.useCaseCommunication, candidate.continuity.entityModel, candidate.continuity.eventModel, candidate.continuity.structureView, candidate.continuity.classificationView, candidate.continuity.workflow, candidate.continuity.stateMachine).forall(_ != null) shouldBe true
          candidate.continuity.realization.conditions.map(_.conditionId) shouldBe Vector("c-opaque-element", "c-opaque-relationship")
          candidate.continuity.realization.enrichmentAssertions.map(_.assertionId) shouldBe Vector("z-opaque-element-enrichment", "z-opaque-relationship-enrichment")
          admitted.reviewAdmission.carrierPackageContext.artifacts.find(_.reference == artifactReference("snapshot-optional")).map(value => (value.required, value.present)) shouldBe Some((false, false))
        }
      }
      "retain Rejected and ChangesRequested as explicit decisions" in {
        Given("independently authored rejected and changes-requested human inputs")
        val decisions = Vector(InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested)
        When("each declared decision crosses real package admission")
        val retained = decisions.map { decision =>
          var result = Option.empty[InternalModelCandidateHumanApprovalInput]
          InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(decision = decision)) { (root, data) => result = _validate(root, data).toOption.map(_.record.approval) }
          result
        }
        Then("decisions rationale and ordered duplicate unresolved items remain exact")
        retained.map(_.map(_.decision)) shouldBe decisions.map(Some(_))
        retained.map(_.map(_.unresolvedItems)) shouldBe Vector.fill(2)(Some(Vector("follow-up", "later", "follow-up")))
        retained.map(_.map(_.rationale)) shouldBe Vector.fill(2)(Some("The basis was considered."))
      }
    }
    "provider and attribution boundary" which {
      "not manufacture human approval from provider evidence or missing human input" in {
        Given("provider-approved review evidence and no selected approval in a current package")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(includeapproval = false, reviewstate = "approved", allnullablefacets = true)) { (root, data) =>
          When("review and human admission receive the actual carrier, then a separate current control receives null human input")
          val review = InternalModelCandidateReviewBindingValidator.validate(root, artifactReference("review-main"), data.executionbasis)
          val missing = _validate(root, data)
          var nullhuman = true
          InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(reviewstate = "approved")) { (controlroot, control) =>
            nullhuman = InternalModelCandidateHumanApprovalValidator.validate(controlroot, artifactReference("approval-main"), artifactReference("review-main"), control.executionbasis, null).isSuccess
          }
          Then("provider state remains evidence and neither absence nor null human input can authorize approval")
          review.isSuccess shouldBe true
          review.toOption.get.binding.targets.forall(_.reviewSnapshot.condition.ambiguity.isEmpty) shouldBe true
          missing.isSuccess shouldBe false
          nullhuman shouldBe false
        }
      }
      "reject provider-shaped actor and provenance through actual admission" in {
        Given("stored and independently supplied provider-shaped actor/provenance claims")
        val changes: Vector[InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput] = Vector(
          value => value.copy(actor = value.actor.copy(kind = "provider")),
          value => value.copy(provenance = value.provenance.copy(authority = "provider"))
        )
        When("the claims cross actual human admission")
        val results = changes.map(change => _fixture_result(FixtureOptions(approvalchange = change)))
        Then("neither provider claim satisfies the human-only grammar")
        results shouldBe Vector(false, false)
      }
    }
  }

  "Candidate human approval closed grammar" should {
    "closed root and nested boundaries" which {
      "reject every missing extra duplicate wrong/null transport Unicode enum old-format and integer variant through actual admission" in {
        Given("a valid V2 record and every object and member boundary in its 3/7/9-field nested grammar")
        val original = recordJson(input())
        val bytes = jsonBytes(original)
        val objectvariants = objectPaths(original).flatMap { path =>
          val fields = atPath(original, path).asObject.get
          fields.keys.toVector.flatMap { key =>
            Vector(
              jsonBytes(replacePath(original, path, value => Json.fromJsonObject(value.asObject.get.remove(key)))),
              jsonBytes(replacePath(original, path :+ key, _ => if (fields(key).get.isArray) Json.fromString("wrong-type") else Json.arr()))
            ) ++ Option.when(!(path.lastOption.contains("provenance") && Set("locator", "revision").contains(key)))(jsonBytes(replacePath(original, path :+ key, _ => Json.Null))).toVector
          } ++ Vector(
            jsonBytes(replacePath(original, path, value => Json.fromJsonObject(value.asObject.get.add("unexpected", Json.True)))),
            jsonBytes(replacePath(original, path, _ => Json.Null)),
            jsonBytes(replacePath(original, path, _ => Json.fromString("wrong-object"))),
            duplicateAtPath(original, path)
          )
        }
        val transport = Vector(
          Array[Byte](0xc3.toByte, 0x28.toByte),
          Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ bytes,
          (new String(bytes, StandardCharsets.UTF_8) + "trailing").getBytes(StandardCharsets.UTF_8),
          new String(bytes, StandardCharsets.UTF_8).replace("The basis was considered.", "\\uD800").getBytes(StandardCharsets.UTF_8)
        )
        val oldformats = Vector(
          jsonBytes(replacePath(original, Vector("profile"), _ => Json.fromString("ccdm-candidate-human-approval-v1"))),
          jsonBytes(replacePath(original, Vector("schemaVersion"), _ => Json.fromString("1.0"))),
          jsonBytes(replacePath(original, Vector("approval", "decision"), _ => Json.fromString("superseded"))),
          jsonBytes(replacePath(original, Vector("approval", "basis", "candidateArtifactReference"), _ => Json.fromString("candidate-main"))),
          jsonBytes(replacePath(original, Vector("approval", "basis", "candidateArtifactReference"), _ => Json.obj("artifactId" -> Json.fromString("candidate-main"), "sha256" -> Json.fromString("legacy"))))
        )
        val variants = objectvariants ++ transport ++ oldformats
        val results = variants.map { variant =>
          Given("one negative serialized control derived from complete independent human input")
          Then("the structural or lexical mutation actually changes the source bytes")
          variant.toVector should not equal bytes.toVector
          When("the changed value crosses structural decode and actual admission")
          (InternalModelCandidateHumanApprovalCodec.decode(variant.toVector).isLeft, _admit_bytes(variant))
        }
        Then("the current control succeeds and every closed-boundary mutation fails")
        _fixture_result(FixtureOptions()) shouldBe true
        results.forall(value => value._1 && !value._2) shouldBe true
      }

      "accept harmless key order whitespace equivalent escapes and absent terminal LF" in {
        Given("one independently supplied human decision with alternate JSON presentation")
        val original = recordJson(input())
        val bytes = jsonBytes(original)
        val variants = Vector(
          bytes.dropRight(1),
          (" \r\n" + reverseKeys(original).spaces2 + "\r\n ").getBytes(StandardCharsets.UTF_8),
          new String(bytes, StandardCharsets.UTF_8).replace("human-1", "\\u0068uman-1").getBytes(StandardCharsets.UTF_8),
          bytes ++ "\n \t".getBytes(StandardCharsets.UTF_8)
        )
        When("the layout variants cross actual admission")
        val results = variants.map(_admit_bytes)
        Then("presentation cannot replace typed semantics as the admission rule")
        results shouldBe Vector.fill(variants.size)(true)
      }

      "reject all lexical revision classes while retaining one maximum Long and generated positive revisions" in {
        Given("all artifact logical and subject revision slots and explicit invalid lexical forms")
        val original = recordJson(input())
        val paths = leafPaths(original).filter(path => Set("artifactRevision", "recordRevision", "subjectRevision").contains(path.last))
        val lexemes = Vector("0", "-1", "1.0", "1.5", "1e0", "1E1", "\"1\"", "null", (BigInt(Long.MaxValue) + 1).toString)
        val variants = for { path <- paths; lexeme <- lexemes } yield {
          jsonBytes(replacePath(original, path, _ => io.circe.parser.parse(lexeme).toOption.get))
        }
        val failures = variants.map { bytes =>
          Given("one explicitly invalid lexical revision substituted into a closed reference slot")
          Then("the lexical mutation actually changes its source bytes")
          bytes.toVector should not equal jsonBytes(original).toVector
          When("the changed lexical revision is decoded")
          InternalModelCandidateHumanApprovalCodec.decode(bytes.toVector).isLeft
        }
        Then("every invalid lexical revision rejects across all nested slots")
        failures shouldBe Vector.fill(variants.size)(true)
        forAll(Gen.oneOf(Gen.const(1L), Gen.const(Long.MaxValue), Gen.chooseNum(1L, Long.MaxValue))) { revision =>
          Given("one independently allocated positive Long used in each serialized revision slot")
          val changed = paths.foldLeft(original)((value, path) => replacePath(value, path, _ => Json.fromLong(revision)))
          When("the structurally valid reference grammar is decoded without claiming semantic admission")
          val result = InternalModelCandidateHumanApprovalCodec.decode(jsonBytes(changed).toVector)
          Then("one maximum and generated positive Longs remain lossless")
          result.isRight shouldBe true
          result.toOption.get.approval.approvalReference.recordRevision.value shouldBe revision
        }
      }
    }
    "scalar fidelity" which {
      "reject Unicode-whitespace-only required and nullable human approval fields through codec and actual filesystem admission" in {
        Given("every scalar string leaf and Unicode whitespace sequences")
        val original = recordJson(input())
        val paths = leafPaths(original).filter(path => atPath(original, path).isString)
        val whitespace = Vector((0 to 0x20).map(_.toChar).mkString, "\u0085", "\u00a0", "\u1680", "\u2000", "\u2003", "\u2007", "\u2028", "\u2029", "\u202f", "\u205f", "\u3000")
        When("one string field at a time is replaced with whitespace and independently supplied to actual admission")
        val results = for { path <- paths; text <- whitespace } yield {
          val bytes = jsonBytes(replacePath(original, path, _ => Json.fromString(text)))
          (InternalModelCandidateHumanApprovalCodec.decode(bytes.toVector).isLeft, _admit_bytes(bytes))
        }
        Then("no required identity prose or nullable present value can be a Unicode blank")
        results.forall(value => value._1 && !value._2) shouldBe true
      }
      "round-trip Unicode identities prose ordered duplicates and deterministic bytes" in {
        Given("generated valid Unicode scalar logical identities and untrimmed prose")
        forAll(Gen.nonEmptyListOf(Gen.oneOf(Gen.alphaNumChar.map(_.toString), Gen.const("漢"), Gen.const("😀"), Gen.const("é"), Gen.const("e\u0301"))).map(_.mkString)) { generated =>
          val spaced = " \u00a0" + generated + "\u2003\u2028\u3000"
          val value = input().copy(approvalReference = recordReference(spaced, Long.MaxValue), rationale = spaced, unresolvedItems = Vector(spaced, "distinct item", spaced))
          When("the independently authored typed input is encoded and decoded")
          val first = InternalModelCandidateHumanApprovalCodec.encode(approvalRecord(value)).toVector
          val second = InternalModelCandidateHumanApprovalCodec.encode(approvalRecord(value)).toVector
          val decoded = InternalModelCandidateHumanApprovalCodec.decode(first)
          Then("opaque Unicode order multiplicity and deterministic writer presentation remain exact")
          first shouldBe second
          decoded.map(_.approval) shouldBe Right(value)
        }
      }
      "accept explicitly null nullable provenance locator and revision values" in {
        Given("an independently supplied human claim with explicit absent optional provenance facts")
        val options = FixtureOptions(approvalchange = value => value.copy(provenance = value.provenance.copy(locator = None, revision = None)))
        When("the current record crosses actual admission")
        val result = _fixture_result(options)
        Then("both nullable values remain represented as None")
        result shouldBe true
      }
    }
  }

  "Candidate human approval independent-input boundary" should {
    "exact human fields" which {
      "compare actor decision provenance rationale items and every basis dimension exactly" in {
        Given("an actual persisted input and independent mismatches for every human and basis field")
        val changes = Vector[InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput](
          value => value.copy(actor = value.actor.copy(kind = "provider")),
          value => value.copy(actor = value.actor.copy(identity = "human-other")),
          value => value.copy(actor = value.actor.copy(role = "approver")),
          value => value.copy(approvalReference = recordReference("approval-other", 109L)),
          value => value.copy(approvalReference = recordReference("approval-main", 113L)),
          value => value.copy(decision = InternalModelCandidateHumanApprovalDecision.Rejected),
          value => value.copy(provenance = value.provenance.copy(authority = "human-other")),
          value => value.copy(provenance = value.provenance.copy(identity = "human-other")),
          value => value.copy(provenance = value.provenance.copy(locator = None)),
          value => value.copy(provenance = value.provenance.copy(revision = None)),
          value => value.copy(rationale = "Different rationale"),
          value => value.copy(unresolvedItems = Vector("later", "follow-up", "follow-up")),
          value => value.copy(unresolvedItems = value.unresolvedItems.distinct),
          value => value.copy(actor = null),
          value => value.copy(rationale = "\uD800"),
          value => value.copy(unresolvedItems = null)
        ) ++ _basis_changes
        When("each separate human input crosses actual admission against the stored control")
        val results = changes.map { change =>
          var result = true
          InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) => result = InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main"), artifactReference("review-main"), data.executionbasis, change(data.expected)).isSuccess }
          result
        }
        Then("only the actual complete matching human input admits the record")
        results shouldBe Vector.fill(changes.size)(false)
      }
    }
    "separate stored-basis binding" which {
      "reject stored and independently expected basis changes after equality succeeds" in {
        Given("identical persisted and independent inputs with contradictory changes to each of the nine basis fields")
        When("the paired inputs cross real admission against the unchanged actual semantic review")
        val results = _basis_changes.map(change => _fixture_result(FixtureOptions(approvalchange = change)))
        Then("human-field equality cannot replace exact semantic binding")
        results shouldBe Vector.fill(_basis_changes.size)(false)
      }
    }
  }

  "Candidate human approval selection and capture boundary" should {
    "explicit selection" which {
      "reject wrong selectors absence role revision dependency and captured syntax while accepting a valid control" in {
        Given("explicit typed selectors and actual carrier metadata variants")
        val options = Vector(
          FixtureOptions(includeapproval = false),
          FixtureOptions(approvalpresent = false, approvalrequired = false),
          FixtureOptions(approvalrole = InternalModelArtifactRole.Validation),
          FixtureOptions(approvaldependencies = Some(Vector(artifactReference("candidate-main")))),
          FixtureOptions(approvaldependencies = Some(Vector.empty)),
          FixtureOptions(approvalbyteschange = _ => "{}".getBytes(StandardCharsets.UTF_8))
        )
        When("selection and captured syntax cross actual admission")
        val metadata = options.map(_fixture_result)
        var selectors = Vector.empty[Boolean]
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          selectors = Vector(
            InternalModelCandidateHumanApprovalValidator.validate(root, reference("approval-missing", 107L, InternalModelArtifactRole.Approval), artifactReference("review-main"), data.executionbasis, data.expected).isSuccess,
            InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main"), reference("review-missing", 73L, InternalModelArtifactRole.Validation), data.executionbasis, data.expected).isSuccess,
            InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main").copy(artifactRevision = InternalModelArtifactRevision.from(113L).toOption.get), artifactReference("review-main"), data.executionbasis, data.expected).isSuccess
          )
        }
        Then("only a present exact Approval reference with the sole exact review dependency admits")
        metadata ++ selectors shouldBe Vector.fill(options.size + 3)(false)
        _fixture_result(FixtureOptions()) shouldBe true
      }
      "select one valid approval among inert unknown-profile siblings without ordinal or latest heuristics" in {
        Given("a selected current approval and an explicitly inventoried unknown-profile approval sibling")
        val sibling = reference("approval-sibling", 211L, InternalModelArtifactRole.Approval)
        val bytes = jsonBytes(replacePath(recordJson(input()), Vector("profile"), _ => Json.fromString("unknown")))
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(extraartifact = Some((sibling, "approvals/sibling.json", bytes, Vector(artifactReference("review-main")))))) { (root, data) =>
          When("the valid selection and then the exact sibling selection cross actual admission")
          val valid = _validate(root, data)
          val unknown = InternalModelCandidateHumanApprovalValidator.validate(root, sibling, artifactReference("review-main"), data.executionbasis, data.expected)
          Then("the inert sibling has no effect and selecting its unknown grammar fails")
          valid.isSuccess shouldBe true
          unknown.isSuccess shouldBe false
        }
      }
    }
  }

  "Candidate human approval review binding" should {
    "independent semantic evidence" which {
      "reject rules/providers drift and incomplete or mismatched review evidence" in {
        Given("independent rule/provider drift plus incomplete review targets and mismatched subject")
        var independent = Vector.empty[Boolean]
        When("the current human validator admits each actual package against the independent execution basis")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          independent = Vector(
            InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main"), artifactReference("review-main"), data.executionbasis.copy(rules = Vector(InternalModelCandidateReviewRule("other-rule", "1.0"))), data.expected).isSuccess,
            InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main"), artifactReference("review-main"), data.executionbasis.copy(providers = Vector(InternalModelCandidateReviewProvider("other-provider", "1.0"))), data.expected).isSuccess
          )
        }
        val missingtarget = _fixture_result(FixtureOptions(bindingchange = value => value.copy(targets = value.targets.take(1))))
        val missingsubject = _fixture_result(FixtureOptions(bindingchange = value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.dropRight(1)))))
        Then("the independent execution basis complete subject and complete target evidence are mandatory")
        independent ++ Vector(missingtarget, missingsubject) shouldBe Vector.fill(4)(false)
      }
      "retain the complete explicit reviewed subject distinct from its newer carrier" in {
        Given("an independently allocated subject revision and two actual carriers differing only in carrier/control transport")
        val inert = reference("validation-inert", 223L, InternalModelArtifactRole.Validation)
        val options = FixtureOptions(carrierrevision = 227L, extraartifact = Some((inert, "carrier/inert.json", "inert".getBytes(StandardCharsets.UTF_8), Vector.empty)))
        When("both actual carriers admit the same independently supplied human input")
        var inputs = Vector.empty[InternalModelCandidateHumanApprovalInput]
        Vector(FixtureOptions(), options).foreach { settings =>
          InternalModelCandidateHumanApprovalValidatorSpec.withFixture(settings) { (root, data) => inputs :+= _validate(root, data).toOption.get.record.approval }
        }
        Then("the exact subject is independent of carrier revision and unselected control inventory")
        inputs.distinct should have size 1
        inputs.head.basis.subject.subjectRevision.value shouldBe 101L
      }
      "retain nullable review facets shared evidence and duplicate limitations" in {
        Given("two targets sharing one selected evidence reference with nullable facets and duplicate limitations")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(sharedevidence = true, allnullablefacets = true, reviewstate = "approved")) { (root, data) =>
          When("actual human admission retains complete target evidence")
          val result = _validate(root, data)
          Then("shared attribution nullable facets and duplicates survive without provider-to-human inference")
          result.isSuccess shouldBe true
          val targets = result.toOption.get.reviewAdmission.binding.targets
          targets.map(_.evidenceArtifactIds) shouldBe Vector.fill(2)(Vector("evidence-alpha"))
          targets.map(_.reviewSnapshot.limitations) shouldBe Vector.fill(2)(Vector("snapshot limitation", "snapshot limitation"))
          targets.forall(_.reviewSnapshot.stableTieKey.isEmpty) shouldBe true
        }
      }
    }
  }

  "Candidate human approval captured handoff" should {
    "pure captured admission" which {
      "preserve a captured valid handoff while fresh path validation fails" in {
        Given("one actual immutable capture before its task-private package paths disappear")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val handoff = InternalModelPackageValidator.verifiedCandidateHumanApproval(root, artifactReference("approval-main"), artifactReference("review-main")).toOption.get
          When("the actual package paths are removed before pure captured admission")
          deleteTree(root.resolve("src/main/internal-model"))
          Files.delete(root.resolve("project.yaml"))
          val captured = InternalModelCandidateHumanApprovalValidator.validateVerified(handoff, data.executionbasis, data.expected)
          val fresh = _validate(root, data)
          Then("the original capture retains the full subject and binary proposal while fresh admission fails")
          captured.toOption.map(_.record.approval) shouldBe Some(data.expected)
          captured.toOption.get.reviewAdmission.semanticDiffAdmission.candidateAdmission.targetBytes.head.proposedRawBytes shouldBe Vector[Byte](0, -1, 10)
          fresh.isSuccess shouldBe false
        }
      }
      "admit an actual copied directory using only explicit references and independent human input" in {
        Given("one current carrier and a separately allocated task-private copy destination")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val destination = temporaryRoot()
          try {
            copyTree(root, destination)
            When("original and copied directories receive the same independent input and exact references")
            val original = _validate(root, data)
            val copied = _validate(destination, data)
            Then("the package-only copy retains the exact admission without process caches")
            copied.toOption shouldBe original.toOption
            copied.isSuccess shouldBe true
          } finally deleteTree(destination)
        }
      }
      "reject null malformed and contradictory captured metadata without exception" in {
        Given("one actual handoff and null or contradictory selected approval metadata")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture() { (root, data) =>
          val handoff = InternalModelPackageValidator.verifiedCandidateHumanApproval(root, artifactReference("approval-main"), artifactReference("review-main")).toOption.get
          val artifact = handoff.approvalArtifact
          val variants = Vector(null, handoff.copy(reviewPackage = null), handoff.copy(approvalArtifact = null),
            handoff.copy(approvalArtifact = artifact.copy(reference = null)), handoff.copy(approvalArtifact = artifact.copy(path = null)),
            handoff.copy(approvalArtifact = artifact.copy(dependencies = null)), handoff.copy(approvalArtifact = artifact.copy(bytes = null)),
            handoff.copy(approvalArtifact = artifact.copy(required = !artifact.required)), handoff.copy(approvalArtifact = artifact.copy(path = "other/approval.json")),
            handoff.copy(approvalArtifact = artifact.copy(dependencies = Vector(artifactReference("candidate-main")))))
          When("every malformed actual captured value crosses pure human admission")
          val results = variants.map(value => InternalModelCandidateHumanApprovalValidator.validateVerified(value, data.executionbasis, data.expected).isSuccess)
          Then("all malformed metadata returns a structured failure")
          results shouldBe Vector.fill(variants.size)(false)
        }
      }
    }
  }

  "Candidate human approval current predecessors" should {
    "no compatibility reader" which {
      "admit a structurally valid current package without approvals through package candidate diff and review readers" in {
        Given("a current two-target package without a human record")
        InternalModelCandidateHumanApprovalValidatorSpec.withFixture(FixtureOptions(includeapproval = false)) { (root, data) =>
          When("the current package candidate diff and selected review readers admit their own semantic boundaries")
          val results = Vector(InternalModelPackageValidator.validateStructure(root).isSuccess, InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess, InternalModelSemanticDiffValidator.validate(root).isSuccess,
            InternalModelCandidateReviewBindingValidator.validate(root, artifactReference("review-main"), data.executionbasis).isSuccess)
          Then("all current predecessors remain admitted without creating human consent")
          results shouldBe Vector.fill(4)(true)
        }
      }
    }
  }

  private def _validate(root: Path, data: FixtureData) =
    InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main", data.options), artifactReference("review-main", data.options), data.executionbasis, data.expected)
  private def _fixture_result(options: FixtureOptions): Boolean = {
    var result = false
    InternalModelCandidateHumanApprovalValidatorSpec.withFixture(options) { (root, data) => result = _validate(root, data).isSuccess }
    result
  }
  private def _admit_bytes(bytes: Array[Byte]): Boolean = _fixture_result(FixtureOptions(approvalbyteschange = _ => bytes))
  private def _basis_changes: Vector[InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput] = {
    def _change_(change: InternalModelCandidateHumanApprovalBasis => InternalModelCandidateHumanApprovalBasis): InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput = value => value.copy(basis = change(value.basis))
    Vector(
      _change_(value => value.copy(candidateArtifactReference = reference("candidate-other", 17L, InternalModelArtifactRole.Projection))),
      _change_(value => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(19L).toOption.get))),
      _change_(value => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(role = InternalModelArtifactRole.Validation))),
      _change_(value => value.copy(candidateReference = recordReference("candidate-other", 7L))),
      _change_(value => value.copy(candidateReference = recordReference("candidate-order-v1", 8L))),
      _change_(value => value.copy(candidateModelIdentity = "model-other")),
      _change_(value => value.copy(reviewArtifactReference = reference("review-other", 73L, InternalModelArtifactRole.Validation))),
      _change_(value => value.copy(reviewArtifactReference = value.reviewArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(79L).toOption.get))),
      _change_(value => value.copy(reviewArtifactReference = value.reviewArtifactReference.copy(role = InternalModelArtifactRole.Projection))),
      _change_(value => value.copy(reviewReference = recordReference("review-other", 3L))),
      _change_(value => value.copy(reviewReference = recordReference("review-identity", 4L))),
      _change_(value => value.copy(semanticDiffArtifactReference = reference("diff-other", 31L, InternalModelArtifactRole.Projection))),
      _change_(value => value.copy(semanticDiffArtifactReference = value.semanticDiffArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(37L).toOption.get))),
      _change_(value => value.copy(semanticDiffArtifactReference = value.semanticDiffArtifactReference.copy(role = InternalModelArtifactRole.Validation))),
      _change_(value => value.copy(semanticDiffReference = recordReference("diff-other", 5L))),
      _change_(value => value.copy(semanticDiffReference = recordReference("semantic-diff-order", 6L))),
      _change_(value => value.copy(subject = value.subject.copy(subjectId = InternalModelRecordId.from("subject-other").toOption.get))),
      _change_(value => value.copy(subject = value.subject.copy(subjectRevision = InternalModelRecordRevision.from(103L).toOption.get))),
      _change_(value => value.copy(subject = value.subject.copy(packageReference = value.subject.packageReference.copy(packageId = InternalModelPackageId.from("abcdefab-cdef-abcd-efab-cdefabcdefab").toOption.get)))),
      _change_(value => value.copy(subject = value.subject.copy(packageReference = value.subject.packageReference.copy(projectNamespace = InternalModelProjectToken.from("org.other").toOption.get)))),
      _change_(value => value.copy(subject = value.subject.copy(packageReference = value.subject.packageReference.copy(projectId = InternalModelProjectToken.from("other-project").toOption.get)))),
      _change_(value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.dropRight(1)))),
      _change_(value => value.copy(subject = value.subject.copy(scope = value.subject.scope.copy(componentIdentity = "component-other")))),
      _change_(value => value.copy(subject = value.subject.copy(scope = value.subject.scope.copy(projectionContextIdentity = "context-other")))),
      _change_(value => value.copy(subject = value.subject.copy(scope = value.subject.scope.copy(selectedUseCaseElementIdentity = "usecase-other")))),
      _change_(value => value.copy(scope = value.scope.copy(componentIdentity = "component-other"))),
      _change_(value => value.copy(scope = value.scope.copy(projectionContextIdentity = "context-other"))),
      _change_(value => value.copy(scope = value.scope.copy(selectedUseCaseElementIdentity = "usecase-other")))
    )
  }
}

/** Task-private actual packages shared by the two owned executable specifications. */
private[runtime] object InternalModelCandidateHumanApprovalValidatorSpec {
  final case class FixtureOptions(
    decision: InternalModelCandidateHumanApprovalDecision = InternalModelCandidateHumanApprovalDecision.Approved,
    approvalartifactid: String = "approval-main",
    approvalartifactrevision: Long = 107L,
    approvalidentity: String = "approval-main",
    approvalrevision: Long = 109L,
    candidateartifactid: String = "candidate-main",
    candidateartifactrevision: Long = 17L,
    reviewartifactid: String = "review-main",
    reviewartifactrevision: Long = 73L,
    diffartifactid: String = "semantic-diff-main",
    diffartifactrevision: Long = 31L,
    candidateidentity: String = "candidate-order-v1",
    candidatemodelidentity: String = "candidate-model-order",
    candidaterevision: Long = 7L,
    reviewidentity: String = "review-identity",
    reviewrevision: Long = 3L,
    diffidentity: String = "semantic-diff-order",
    diffrevision: Long = 5L,
    subjectidentity: String = "subject-order",
    subjectrevision: Long = 101L,
    componentidentity: String = "component-order",
    projectioncontextidentity: String = "context-order",
    selectedusecaseelementidentity: String = "e-usecase",
    packageid: String = "01234567-89ab-cdef-0123-456789abcdef",
    projectid: String = "candidate-review-sample",
    projectnamespace: String = "org.example",
    carrierrevision: Long = 43L,
    ruleid: String = "rule-alpha",
    ruleversion: String = "1.0",
    providerid: String = "provider-alpha",
    providerversion: String = "1.0",
    includeapproval: Boolean = true,
    approvalpresent: Boolean = true,
    approvalrequired: Boolean = true,
    approvalrole: InternalModelArtifactRole = InternalModelArtifactRole.Approval,
    approvaldependencies: Option[Vector[InternalModelArtifactReference]] = None,
    includereview: Boolean = true,
    reviewstate: String = "reviewed",
    allnullablefacets: Boolean = false,
    sharedevidence: Boolean = false,
    includeoptionalsnapshot: Boolean = false,
    missingsourceversions: Set[String] = Set.empty,
    extraartifact: Option[(InternalModelArtifactReference, String, Array[Byte], Vector[InternalModelArtifactReference])] = None,
    bindingchange: InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding = identity,
    approvalchange: InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput = identity,
    approvalbyteschange: Array[Byte] => Array[Byte] = identity
  )
  final case class FixtureData(
    expected: InternalModelCandidateHumanApprovalInput,
    executionbasis: InternalModelCandidateReviewExecutionBasis,
    reviewbinding: InternalModelCandidateReviewBinding,
    options: FixtureOptions
  )
  final case class FixtureEvidence(
    approval: InternalModelCandidateHumanApprovalAdmission,
    review: InternalModelCandidateReviewBindingAdmission,
    snapshots: Vector[InternalModelVerifiedSourceSnapshot]
  )

  private val _printer = Printer.noSpacesSortKeys
  private val _fixture_parent = Path.of("target", "P104-TYPED-APPROVAL-LIFECYCLE-001")
  private val _sources = Map(
    "snapshot-model" -> InternalModelSemanticSource("model-authority", "model-source", Some("catalog/model-source"), Some("revision-1")),
    "snapshot-scenario" -> InternalModelSemanticSource("scenario-authority", "scenario-source", Some("catalog/scenario-source"), Some("revision-1")),
    "snapshot-glossary" -> InternalModelSemanticSource("glossary-authority", "glossary-source", Some("catalog/glossary-source"), Some("revision-1")),
    "snapshot-cml-alpha" -> InternalModelSemanticSource("cml-authority", "cml-alpha", Some("catalog/cml-alpha"), Some("revision-alpha")),
    "snapshot-cml-beta" -> InternalModelSemanticSource("cml-authority", "cml-beta", Some("catalog/cml-beta"), Some("revision-beta"))
  )
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _scenario_raw = "scenario source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _glossary_raw = "glossary source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_alpha_raw = "alpha cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_beta_raw = "beta cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector

  def artifactReference(id: String, options: FixtureOptions = FixtureOptions()): InternalModelArtifactReference = {
    val (selectedid, revision, role) = id match {
      case "candidate-main" => (options.candidateartifactid, options.candidateartifactrevision, InternalModelArtifactRole.Projection)
      case "review-main" => (options.reviewartifactid, options.reviewartifactrevision, InternalModelArtifactRole.Validation)
      case "semantic-diff-main" => (options.diffartifactid, options.diffartifactrevision, InternalModelArtifactRole.Projection)
      case "approval-main" => (options.approvalartifactid, options.approvalartifactrevision, InternalModelArtifactRole.Approval)
      case "realization-main" => (id, 13L, InternalModelArtifactRole.Realization)
      case "projection-main" => (id, 29L, InternalModelArtifactRole.Projection)
      case "evidence-alpha" => (id, 59L, InternalModelArtifactRole.Validation)
      case "evidence-beta" => (id, 61L, InternalModelArtifactRole.Validation)
      case "decision-main" => (id, 47L, InternalModelArtifactRole.Decision)
      case "open-issue-main" => (id, 53L, InternalModelArtifactRole.OpenIssue)
      case "resume-main" => (id, 67L, InternalModelArtifactRole.Resume)
      case "snapshot-model" => (id, 41L, InternalModelArtifactRole.SourceSnapshot)
      case "snapshot-scenario" => (id, 139L, InternalModelArtifactRole.SourceSnapshot)
      case "snapshot-glossary" => (id, 149L, InternalModelArtifactRole.SourceSnapshot)
      case "snapshot-cml-alpha" => (id, 11L, InternalModelArtifactRole.SourceSnapshot)
      case "snapshot-cml-beta" => (id, 37L, InternalModelArtifactRole.SourceSnapshot)
      case "snapshot-optional" => (id, 71L, InternalModelArtifactRole.SourceSnapshot)
    }
    reference(selectedid, revision, role)
  }
  def reference(id: String, revision: Long, role: InternalModelArtifactRole): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from(id).toOption.get, InternalModelArtifactRevision.from(revision).toOption.get, role)
  def recordReference(id: String, revision: Long): InternalModelRecordReference = _record_reference(id, revision)
  def basis(options: FixtureOptions = FixtureOptions()): InternalModelCandidateReviewExecutionBasis =
    InternalModelCandidateReviewExecutionBasis(Vector(InternalModelCandidateReviewRule(options.ruleid, options.ruleversion), InternalModelCandidateReviewRule("rule-beta", "2.0")), Vector(InternalModelCandidateReviewProvider(options.providerid, options.providerversion), InternalModelCandidateReviewProvider("provider-beta", "2.0")))
  def input(options: FixtureOptions = FixtureOptions()): InternalModelCandidateHumanApprovalInput = _human_input(_review_binding(options), options)
  def approvalRecord(value: InternalModelCandidateHumanApprovalInput): InternalModelCandidateHumanApproval = InternalModelCandidateHumanApproval(value, "ccdm-candidate-human-approval-v2", "2.0")
  def jsonBytes(value: Json): Array[Byte] = _canonical(value)
  def recordJson(value: InternalModelCandidateHumanApprovalInput): Json = io.circe.parser.parse(new String(InternalModelCandidateHumanApprovalCodec.encode(approvalRecord(value)), StandardCharsets.UTF_8)).toOption.get

  def withFixture(options: FixtureOptions = FixtureOptions())(action: (Path, FixtureData) => Unit): Unit = {
    val projection = _projection(options)
    val binding = options.bindingchange(_review_binding(options))
    val expected = options.approvalchange(_human_input(_review_binding(options), options))
    val approvalbytes = options.approvalbyteschange(InternalModelCandidateHumanApprovalCodec.encode(approvalRecord(expected)))
    val basefiles = Vector(
      ("evidence-alpha", "evidence/alpha.json", "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8), Vector.empty[String]),
      ("evidence-beta", "evidence/beta.json", "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8), Vector.empty[String]),
      ("snapshot-model", "snapshots/model.json", _model_snapshot(options), Vector.empty[String]),
      ("snapshot-scenario", "snapshots/scenario.json", _scenario_snapshot(options), Vector.empty[String]),
      ("snapshot-glossary", "snapshots/glossary.json", _glossary_snapshot(options), Vector.empty[String]),
      ("snapshot-cml-alpha", "snapshots/cml-alpha.json", _cml_snapshot(_source_value("snapshot-cml-alpha", options), "cml/alpha.cml", _cml_alpha_raw), Vector.empty[String]),
      ("snapshot-cml-beta", "snapshots/cml-beta.json", _cml_snapshot(_source_value("snapshot-cml-beta", options), "cml/beta.cml", _cml_beta_raw), Vector.empty[String]),
      ("realization-main", "realizations/main.json", _realization(options), Vector("snapshot-model")),
      ("projection-main", "projections/continuity.json", _binding(options), Vector("realization-main")),
      ("decision-main", "decisions/main.json", "decision record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      ("open-issue-main", "open-issues/main.json", "open issue record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      ("resume-main", "resumes/main.json", "resume record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      ("candidate-main", "projections/candidate.json", InternalModelCandidateCmlProjectionCodec.encode(projection), Vector("projection-main", "realization-main", "snapshot-cml-alpha", "snapshot-cml-beta")),
      ("semantic-diff-main", "projections/semantic-diff.json", InternalModelSemanticDiffCodec.encode(_semantic_diff(projection, options)), Vector("candidate-main"))
    )
    val entries = basefiles.map { case (id, path, _, dependencies) => _entry(artifactReference(id, options), path, true, dependencies.map(id => artifactReference(id, options)).sortBy(_.artifactId.value)) } ++
      Option.when(options.includeoptionalsnapshot)(_entry(artifactReference("snapshot-optional", options), "snapshots/optional.json", false, Vector.empty)).toVector ++
      Option.when(options.includereview)(_entry(artifactReference("review-main", options), "reviews/candidate-review.json", true, binding.subject.artifacts)).toVector ++
      Option.when(options.includeapproval)(_entry(artifactReference("approval-main", options).copy(role = options.approvalrole), "approvals/main.json", options.approvalrequired, options.approvaldependencies.getOrElse(Vector(artifactReference("review-main", options))))).toVector ++
      options.extraartifact.map { case (ref, path, _, dependencies) => _entry(ref, path, true, dependencies) }.toVector
    val root = temporaryRoot()
    try {
      write(root.resolve("project.yaml"), ("project:\n  namespace: " + options.projectnamespace + "\n  id: " + options.projectid + "\n").getBytes(StandardCharsets.UTF_8))
      write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(_topological(entries), options))
      basefiles.foreach { case (_, path, bytes, _) => write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      if (options.includereview) write(root.resolve("src/main/internal-model/reviews/candidate-review.json"), InternalModelCandidateReviewBindingCodec.encode(binding))
      if (options.includeapproval && options.approvalpresent) write(root.resolve("src/main/internal-model/approvals/main.json"), approvalbytes)
      options.extraartifact.foreach { case (_, path, bytes, _) => write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      action(root, FixtureData(expected, basis(options), binding, options))
    } finally deleteTree(root)
  }

  def admit(root: Path, data: FixtureData): FixtureEvidence = FixtureEvidence(
    InternalModelCandidateHumanApprovalValidator.validate(root, artifactReference("approval-main", data.options), artifactReference("review-main", data.options), data.executionbasis, data.expected).toOption.get,
    InternalModelCandidateReviewBindingValidator.validate(root, artifactReference("review-main", data.options), data.executionbasis).toOption.get,
    InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
  )
  def withFixturePair(options: FixtureOptions)(action: (FixtureEvidence, FixtureEvidence, FixtureData) => Unit): Unit =
    withFixture() { (root, data) =>
      val original = admit(root, data)
      withFixture(options) { (currentroot, currentdata) => action(original, admit(currentroot, currentdata), currentdata) }
    }

  def unchangedSources: Map[String, InternalModelLiveSourceObservation] = Map(
    "snapshot-model" -> InternalModelLiveSourceObservation.Observed("model-authority", "model-source", Some("revision-1"), _model_raw, None),
    "snapshot-scenario" -> InternalModelLiveSourceObservation.Observed("scenario-authority", "scenario-source", Some("revision-1"), _scenario_raw, None),
    "snapshot-glossary" -> InternalModelLiveSourceObservation.Observed("glossary-authority", "glossary-source", Some("revision-1"), _glossary_raw, None),
    "snapshot-cml-alpha" -> InternalModelLiveSourceObservation.Observed("cml-authority", "cml-alpha", Some("revision-alpha"), _cml_alpha_raw, Some("cml/alpha.cml")),
    "snapshot-cml-beta" -> InternalModelLiveSourceObservation.Observed("cml-authority", "cml-beta", Some("revision-beta"), _cml_beta_raw, Some("cml/beta.cml"))
  )
  def sourceDimensionObservation(artifactId: String, dimension: String): InternalModelLiveSourceObservation =
    unchangedSources(artifactId) match {
      case InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes, path) => dimension match {
        case "authority" => InternalModelLiveSourceObservation.Observed(authority + "-current", identity, revision, rawbytes, path)
        case "identity" => InternalModelLiveSourceObservation.Observed(authority, identity + "-current", revision, rawbytes, path)
        case "revision" => InternalModelLiveSourceObservation.Observed(authority, identity, Some("declared-current"), rawbytes, path)
        case "content" => InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes :+ 0.toByte, path)
        case "path" => InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes, Some("cml/current.cml"))
        case "missing" => InternalModelLiveSourceObservation.Observed(authority, identity, None, rawbytes, path)
        case "missing-and-changed" => InternalModelLiveSourceObservation.Observed(authority, identity + "-current", None, rawbytes, path.map(_ => "cml/current.cml"))
      }
      case other => other
    }

  private def _human_input(review: InternalModelCandidateReviewBinding, options: FixtureOptions): InternalModelCandidateHumanApprovalInput =
    InternalModelCandidateHumanApprovalInput(InternalModelDecisionActor("human", "human-1", "reviewer"), _record_reference(options.approvalidentity, options.approvalrevision),
      InternalModelCandidateHumanApprovalBasis(artifactReference("candidate-main", options), _record_reference(options.candidateidentity, options.candidaterevision), options.candidatemodelidentity,
        artifactReference("review-main", options), _record_reference(options.reviewidentity, options.reviewrevision), review.subject, _scope(options),
        artifactReference("semantic-diff-main", options), _record_reference(options.diffidentity, options.diffrevision)),
      options.decision, InternalModelSemanticSource("human-decision", "human-1", Some("review/approval"), Some("1")), "The basis was considered.", Vector("follow-up", "later", "follow-up"))

  private def _review_binding(options: FixtureOptions): InternalModelCandidateReviewBinding = {
    val refs = Vector("candidate-main", "decision-main", "open-issue-main", "projection-main", "realization-main", "semantic-diff-main", "snapshot-model", "snapshot-scenario", "snapshot-glossary", "snapshot-cml-alpha", "snapshot-cml-beta") ++
      (if (options.sharedevidence) Vector("evidence-alpha") else Vector("evidence-alpha", "evidence-beta"))
    val subject = InternalModelReviewSubject(InternalModelRecordId.from(options.subjectidentity).toOption.get, InternalModelRecordRevision.from(options.subjectrevision).toOption.get,
      InternalModelPackageReference(InternalModelPackageId.from(options.packageid).toOption.get, InternalModelProjectToken.from(options.projectnamespace).toOption.get, InternalModelProjectToken.from(options.projectid).toOption.get),
      _scope(options), refs.map(id => artifactReference(id, options)).sortBy(_.artifactId.value))
    InternalModelCandidateReviewBinding(artifactReference("candidate-main", options), _record_reference(options.candidateidentity, options.candidaterevision), options.candidatemodelidentity,
      artifactReference("projection-main", options), (if (options.sharedevidence) Vector("evidence-alpha") else Vector("evidence-alpha", "evidence-beta")).map(id => artifactReference(id, options)),
      "ccdm-candidate-review-binding-v2", basis(options).providers, artifactReference("realization-main", options), _record_reference("realization-order", 19L),
      _record_reference(options.reviewidentity, options.reviewrevision), basis(options).rules, "2.0", _scope(options), artifactReference("semantic-diff-main", options), _record_reference(options.diffidentity, options.diffrevision), subject,
      Vector(_review_target("target-alpha", "patch-alpha", "evidence-alpha", options), _review_target("target-beta", "patch-beta", if (options.sharedevidence) "evidence-alpha" else "evidence-beta", options)))
  }
  private def _review_target(targetid: String, patchid: String, evidenceid: String, options: FixtureOptions): InternalModelCandidateReviewTarget = {
    val facet = if (options.allnullablefacets) None else Some("bounded facet")
    InternalModelCandidateReviewTarget(Vector(evidenceid), CandidateDesignReviewSnapshot("snapshot-review-" + targetid, MonoKotoProjectionContextIdentity(options.projectioncontextidentity), ComponentDashboardComponentIdentity(options.componentidentity), patchid, options.candidatemodelidentity, options.reviewstate,
      ComponentDashboardSourceAttribution("review-source", "review-authority", "review-locator"), ComponentDashboardCondition("available", "authorized", facet, facet, facet, facet, facet, facet, Vector("condition limitation", "condition limitation")), Vector("snapshot limitation", "snapshot limitation"), facet), targetid)
  }
  private def _projection(options: FixtureOptions): InternalModelCandidateCmlProjection = {
    val alpha = _target("snapshot-cml-alpha", "patch-alpha", "target-alpha", "cml/alpha.cml", Vector[Byte](0, -1, 10), "mapping-alpha", "opaque-shared", "element", "effect-alpha", "ref-opaque-element-kind-Mono", options)
    val beta = _target("snapshot-cml-beta", "patch-beta", "target-beta", "cml/beta.cml", _cml_beta_raw, "mapping-beta", "opaque-shared", "relationship", "effect-beta", "ref-opaque-relationship-role-StructuralDomain", options)
    InternalModelCandidateCmlProjection(_record_reference(options.candidateidentity, options.candidaterevision), options.candidatemodelidentity, artifactReference("projection-main", options), "ccdm-candidate-cml-projection-v2", artifactReference("realization-main", options), "2.0", _scope(options), Vector(alpha, beta))
  }
  private def _target(baselineid: String, patchid: String, targetid: String, path: String, rawbytes: Vector[Byte], mappingid: String, identity: String, kind: String, effectprefix: String, referenceid: String, options: FixtureOptions): InternalModelCandidateCmlTarget = {
    val assertionid = if (kind == "element") "a-opaque-element-kind-Mono" else "a-opaque-relationship-role-StructuralDomain"
    val conditionid = if (kind == "element") "c-opaque-element" else "c-opaque-relationship"
    val enrichmentid = if (kind == "element") "z-opaque-element-enrichment" else "z-opaque-relationship-enrichment"
    val mapping = InternalModelCandidateCmlMapping(Vector(assertionid), "anchor-" + targetid, "cml-" + targetid, Vector(conditionid), Vector(enrichmentid), mappingid, identity, kind)
    val effects = Vector(InternalModelCandidateCmlEffect("unknown", "supplied compatibility expectation", effectprefix + "-compatibility", "compatibility", Vector(mappingid), referenceid), InternalModelCandidateCmlEffect("unknown", "supplied migration expectation", effectprefix + "-migration", "migration", Vector(mappingid), referenceid))
    InternalModelCandidateCmlTarget(artifactReference(baselineid, options), effects, Vector(mapping), patchid, path,
      InternalModelCandidateCmlContent(_record_reference("content-" + targetid, if (targetid == "target-alpha") 131L else 137L), rawbytes.length.toLong, Base64.getEncoder.encodeToString(rawbytes.toArray)), _source_value(baselineid, options), targetid)
  }
  private def _semantic_diff(projection: InternalModelCandidateCmlProjection, options: FixtureOptions): InternalModelSemanticDiff = {
    val targets = projection.targets.map { target =>
      val mapping = target.mappings.head
      val condition = ComponentDashboardCondition("available", "authorized", None, None, None, None, None, None, Vector("condition limitation"))
      val attribution = ComponentDashboardSourceAttribution("source-id", "authority", "source-locator")
      val entry = CandidateDesignSemanticDiffEntry("entry-" + target.targetId, MonoKotoProjectionContextIdentity(options.projectioncontextidentity), ComponentDashboardComponentIdentity(options.componentidentity), target.patchIdentity, options.candidatemodelidentity, "category", "action", mapping.semanticIdentity, Some("before"), Some("after"), "relationship", attribution, condition, Vector("entry limitation"), Some("entry tie"))
      val trace = CandidateDesignProposedCmlPatchTrace(target.patchIdentity, MonoKotoProjectionContextIdentity(options.projectioncontextidentity), ComponentDashboardComponentIdentity(options.componentidentity), target.source.authority, target.source.locator.get, target.baselineArtifactReference, target.proposedContent.contentReference, attribution, condition, Vector("patch limitation"), Some("patch tie"))
      InternalModelSemanticDiffTarget(Vector(InternalModelSemanticDiffMappedEntry(entry, mapping.mappingId, mapping.semanticIdentityKind)), trace, target.targetId)
    }
    InternalModelSemanticDiff(artifactReference("candidate-main", options), projection.candidateReference, projection.candidateModelIdentity, "ccdm-semantic-diff-v2", "2.0", projection.scope, _record_reference(options.diffidentity, options.diffrevision), targets)
  }
  private def _model_snapshot(options: FixtureOptions): Array[Byte] = {
    val facts = Vector(_fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono", options), _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case", options), _fact("element", "e-usecase-current", "anchor-e-usecase-current-kind-use-case", "kind:use-case", options), _fact("element", "opaque-shared", "anchor-opaque-element-enrichment", "enrichment:shared", options), _fact("element", "opaque-shared", "anchor-opaque-element-kind-Mono", "kind:Mono", options), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-enrichment", "enrichment:shared", options), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", options), _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", options)).sortBy(value => (value.hcursor.get[String]("semanticIdentityKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("sourceAnchor").toOption.get))
    _canonical(Json.obj("basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)), "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source_json(_source_value("snapshot-model", options))))
  }

  private def _scenario_snapshot(options: FixtureOptions): Array[Byte] =
    _canonical(Json.obj("basis" -> Json.obj("content" -> Json.fromString("Scenario content"), "scenarioId" -> Json.fromString("scenario-main"), "traceLinks" -> Json.arr()), "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("scenario"), "source" -> _source_json(_source_value("snapshot-scenario", options))))

  private def _glossary_snapshot(options: FixtureOptions): Array[Byte] =
    _canonical(Json.obj("basis" -> Json.obj("entries" -> Json.arr(Json.obj("definition" -> Json.fromString("Glossary definition"), "limitations" -> Json.arr(), "sourceAnchor" -> Json.fromString("term-anchor"), "termIdentity" -> Json.fromString("term-main"), "termLabel" -> Json.fromString("Term")))), "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("glossary-bok"), "source" -> _source_json(_source_value("snapshot-glossary", options))))

  private def _cml_snapshot(source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte]): Array[Byte] =
    _canonical(Json.obj("basis" -> Json.obj("byteLength" -> Json.fromLong(rawbytes.length.toLong), "projectRelativePath" -> Json.fromString(path), "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes.toArray))), "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("cml-baseline"), "source" -> _source_json(source)))

  private def _realization(options: FixtureOptions): Array[Byte] = {
    val assertions = Vector(_assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", Vector.empty), _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", Vector.empty), _assertion("a-e-usecase-current-kind-use-case", "element", "e-usecase-current", "ref-e-usecase-current-kind-use-case", "kind:use-case", Vector.empty), _assertion("a-opaque-element-kind-Mono", "element", "opaque-shared", "ref-opaque-element-kind-Mono", "kind:Mono", Vector("c-opaque-element")), _assertion("a-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "ref-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", Vector("c-opaque-relationship")), _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", Vector.empty)).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val enrichment = Vector(_assertion("z-opaque-element-enrichment", "element", "opaque-shared", "ref-opaque-element-enrichment", "enrichment:shared", Vector("c-opaque-element")), _assertion("z-opaque-relationship-enrichment", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "enrichment:shared", Vector("c-opaque-relationship"))).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    _canonical(Json.obj("canonicalAssertions" -> Json.fromValues(assertions), "conditions" -> Json.fromValues(Vector(_condition_json("c-opaque-element", "limitation", "element", "opaque-shared", "ref-opaque-element-enrichment", "element evidence remains bounded"), _condition_json("c-opaque-relationship", "limitation", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "relationship evidence remains bounded"))), "elements" -> Json.fromValues(Vector(_element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")), _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")), _element("e-usecase-current", "use-case", "Current use case", Vector("a-e-usecase-current-kind-use-case")), _element("opaque-shared", "Mono", "Opaque shared element", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element")))), "enrichmentAssertions" -> Json.fromValues(enrichment), "profile" -> Json.fromString("ccdm-realization-v3"), "realizationReference" -> _record_json(_record_reference("realization-order", 19L)), "relationships" -> Json.fromValues(Vector(_relationship("opaque-shared", "StructuralDomain", "e-mono", "e-usecase", Vector("a-opaque-relationship-role-StructuralDomain"), Vector("z-opaque-relationship-enrichment"), Vector("c-opaque-relationship")), _relationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase", Vector("a-r-mono-domain-role-StructuralDomain")))), "schemaVersion" -> Json.fromString("3.0"), "scope" -> _scope_json(options), "sourceReferences" -> Json.fromValues(Vector(_reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono", options), _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case", options), _reference("ref-e-usecase-current-kind-use-case", "element", "e-usecase-current", "anchor-e-usecase-current-kind-use-case", options), _reference("ref-opaque-element-enrichment", "element", "opaque-shared", "anchor-opaque-element-enrichment", options), _reference("ref-opaque-element-kind-Mono", "element", "opaque-shared", "anchor-opaque-element-kind-Mono", options), _reference("ref-opaque-relationship-enrichment", "relationship", "opaque-shared", "anchor-opaque-relationship-enrichment", options), _reference("ref-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain", options), _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", options)).sortBy(_.hcursor.get[String]("referenceId").toOption.get)), "successorLinks" -> Json.arr(), "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(_artifact_json(artifactReference("snapshot-model", options))))))
  }

  private def _binding(options: FixtureOptions): Array[Byte] = {
    def _record_(kind: String, identity: String, role: String, ids: Vector[String]): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(ids.map(Json.fromString)), "conditionIds" -> Json.arr(), "enrichmentAssertionIds" -> Json.arr(), "recordKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "sequenceAssertionId" -> Json.Null, "viewRole" -> Json.fromString(role))
    val families = Vector("MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono")), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))), "UseCaseCommunicationProjection" -> Vector(_record_("element", options.selectedusecaseelementidentity, "use-case", Vector("a-" + options.selectedusecaseelementidentity + "-kind-use-case"))), "EntityModelProjection" -> Vector.empty[Json], "EventModelProjection" -> Vector.empty[Json], "StructureViewProjection" -> Vector.empty[Json], "ClassificationViewProjection" -> Vector.empty[Json], "WorkflowProjection" -> Vector.empty[Json], "StateMachineProjection" -> Vector.empty[Json])
    _canonical(Json.obj("profile" -> Json.fromString("ccdm-projection-binding-v3"), "bindingReference" -> _record_json(_record_reference("binding-order", 23L)), "realizationArtifactReference" -> _artifact_json(artifactReference("realization-main", options)), "schemaVersion" -> Json.fromString("3.0"), "scope" -> _scope_json(options), "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })))
  }


  private def _fact(kind: String, identity: String, anchor: String, content: String, options: FixtureOptions): Json =
    Json.obj("componentIdentity" -> Json.fromString(options.componentidentity), "content" -> Json.fromString(content), "limitations" -> Json.arr(), "projectionContextIdentity" -> Json.fromString(options.projectioncontextidentity), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceAnchor" -> Json.fromString(anchor))

  private def _assertion(id: String, kind: String, identity: String, referenceid: String, content: String, conditionids: Vector[String]): Json =
    Json.obj("assertionId" -> Json.fromString(id), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "content" -> Json.fromString(content), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid), "association" -> Json.Null)

  private def _element(identity: String, kind: String, label: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json =
    Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "kind" -> Json.fromString(kind), "label" -> Json.fromString(label))

  private def _relationship(identity: String, role: String, source: String, target: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json =
    Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "direction" -> Json.fromString("source-to-target"), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "label" -> Json.fromString(identity), "role" -> Json.fromString(role), "sourceElementIdentity" -> Json.fromString(source), "targetElementIdentity" -> Json.fromString(target))

  private def _reference(id: String, kind: String, identity: String, anchor: String, options: FixtureOptions): Json =
    Json.obj("referenceId" -> Json.fromString(id), "snapshotReference" -> _artifact_json(artifactReference("snapshot-model", options)), "source" -> _source_json(_source_value("snapshot-model", options)), "sourceAnchor" -> Json.fromString(anchor), "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind)))

  private def _condition_json(id: String, kind: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj("affectedIdentity" -> Json.fromString(affectedidentity), "affectedKind" -> Json.fromString(affectedkind), "conditionId" -> Json.fromString(id), "detail" -> Json.fromString(detail), "kind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid))


  private def _source_value(id: String, options: FixtureOptions): InternalModelSemanticSource =
    if (options.missingsourceversions.contains(id)) _sources(id).copy(revision = None) else _sources(id)
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority), "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null))
  private def _scope(options: FixtureOptions): InternalModelSemanticScope = InternalModelSemanticScope(options.componentidentity, options.projectioncontextidentity, options.selectedusecaseelementidentity)
  private def _scope_json(options: FixtureOptions): Json = Json.obj("componentIdentity" -> Json.fromString(options.componentidentity), "projectionContextIdentity" -> Json.fromString(options.projectioncontextidentity), "selectedUseCaseElementIdentity" -> Json.fromString(options.selectedusecaseelementidentity))
  private def _record_reference(id: String, revision: Long): InternalModelRecordReference = InternalModelRecordReference(InternalModelRecordId.from(id).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)
  private def _artifact_json(value: InternalModelArtifactReference): Json = io.circe.parser.parse(new String(InternalModelTypedControlCodec.encodeArtifactReference(value).toArray, StandardCharsets.UTF_8)).toOption.get
  private def _record_json(value: InternalModelRecordReference): Json = io.circe.parser.parse(new String(InternalModelTypedControlCodec.encodeRecordReference(value).toArray, StandardCharsets.UTF_8)).toOption.get
  private def _entry(value: InternalModelArtifactReference, path: String, required: Boolean, dependencies: Vector[InternalModelArtifactReference]): Json =
    Json.obj("artifactId" -> Json.fromString(value.artifactId.value), "artifactRevision" -> Json.fromLong(value.artifactRevision.value), "role" -> Json.fromString(value.role.wireValue), "path" -> Json.fromString(path), "required" -> Json.fromBoolean(required), "dependsOn" -> Json.fromValues(dependencies.map(_artifact_json)))
  private def _topological(entries: Vector[Json]): Vector[Json] = {
    val byid = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value).toMap
    val dependencies = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value.hcursor.get[Vector[Json]]("dependsOn").toOption.get.map(_.hcursor.get[String]("artifactId").toOption.get).toSet).toMap
    @annotation.tailrec
    def _loop_(pending: Map[String, Set[String]], completed: Vector[String]): Vector[String] = {
      if (pending.isEmpty) completed
      else {
        val next = pending.collect { case (id, deps) if deps.isEmpty => id }.toVector.sorted.headOption.getOrElse(pending.keys.toVector.sorted.head)
        _loop_(pending.removed(next).view.mapValues(_ - next).toMap, completed :+ next)
      }
    }
    _loop_(dependencies, Vector.empty).map(byid)
  }
  private def _manifest(entries: Vector[Json], options: FixtureOptions): Array[Byte] =
    _canonical(Json.obj("artifacts" -> Json.fromValues(entries), "lifecycleState" -> Json.fromString("draft"), "packageId" -> Json.fromString(options.packageid), "projectId" -> Json.fromString(options.projectid), "projectNamespace" -> Json.fromString(options.projectnamespace), "revision" -> Json.fromLong(options.carrierrevision), "schemaVersion" -> Json.fromString("2.0")))
  private def _canonical(value: Json): Array[Byte] = (_printer.print(value) + "\n").getBytes(StandardCharsets.UTF_8)


  def atPath(value: Json, path: Vector[String]): Json = {
    if (path.isEmpty) value
    else value.asObject match {
      case Some(fields) => atPath(fields(path.head).get, path.tail)
      case None => atPath(value.asArray.get(path.head.toInt), path.tail)
    }
  }
  def replacePath(value: Json, path: Vector[String], change: Json => Json): Json = {
    if (path.isEmpty) change(value)
    else value.asObject match {
      case Some(fields) => Json.fromJsonObject(fields.add(path.head, replacePath(fields(path.head).get, path.tail, change)))
      case None =>
        val values = value.asArray.get
        Json.fromValues(values.updated(path.head.toInt, replacePath(values(path.head.toInt), path.tail, change)))
    }
  }
  def replacePath(value: Json, path: Vector[String], replacement: Json): Json = replacePath(value, path, _ => replacement)
  def objectPaths(value: Json, path: Vector[String] = Vector.empty): Vector[Vector[String]] = value.asObject match {
    case Some(fields) => Vector(path) ++ fields.toVector.flatMap { case (key, item) => objectPaths(item, path :+ key) }
    case None => value.asArray.map(_.zipWithIndex.flatMap { case (item, index) => objectPaths(item, path :+ index.toString) }.toVector).getOrElse(Vector.empty)
  }
  def leafPaths(value: Json, path: Vector[String] = Vector.empty): Vector[Vector[String]] = value.asObject match {
    case Some(fields) => fields.toVector.flatMap { case (key, item) => leafPaths(item, path :+ key) }
    case None => value.asArray match {
      case Some(values) => values.zipWithIndex.flatMap { case (item, index) => leafPaths(item, path :+ index.toString) }.toVector
      case None => Vector(path)
    }
  }
  def reverseKeys(value: Json): Json = value.asObject match {
    case Some(fields) => Json.fromJsonObject(JsonObject.fromIterable(fields.toVector.reverse.map { case (key, item) => key -> reverseKeys(item) }))
    case None => value.asArray.map(values => Json.fromValues(values.map(reverseKeys))).getOrElse(value)
  }
  def duplicateAtPath(value: Json, path: Vector[String]): Array[Byte] = {
    def _render_(item: Json, current: Vector[String]): String = item.asObject match {
      case Some(fields) =>
        val members = fields.toVector.map { case (key, child) => Json.fromString(key).noSpaces + ":" + _render_(child, current :+ key) }
        (members ++ Option.when(current == path)(members.head).toVector).mkString("{", ",", "}")
      case None => item.asArray.map(values => values.zipWithIndex.map { case (child, index) => _render_(child, current :+ index.toString) }.mkString("[", ",", "]")).getOrElse(item.noSpaces)
    }
    _render_(value, Vector.empty).getBytes(StandardCharsets.UTF_8)
  }

  def temporaryRoot(): Path = { Files.createDirectories(_fixture_parent); Files.createTempDirectory(_fixture_parent, "fixture-") }
  def write(path: Path, bytes: Array[Byte]): Unit = { Files.createDirectories(path.getParent); Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) }
  def copyTree(source: Path, destination: Path): Unit = {
    val stream = Files.walk(source)
    try stream.iterator.asScala.foreach { path =>
      val target = destination.resolve(source.relativize(path).toString)
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(target)
      else Files.copy(path, target, LinkOption.NOFOLLOW_LINKS)
    } finally stream.close()
  }
  def deleteTree(root: Path): Unit = {
    require(root.toAbsolutePath.normalize.startsWith(_fixture_parent.toAbsolutePath.normalize) && root.toAbsolutePath.normalize != _fixture_parent.toAbsolutePath.normalize)
    if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      val stream = Files.walk(root)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.deleteIfExists(path))
      finally stream.close()
    }
  }
}
