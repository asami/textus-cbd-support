package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest
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
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Executable specification of the closed, independently admitted human approval record. */
final class InternalModelCandidateHumanApprovalValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private final case class FixtureOptions(
    bindingchange: InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding = identity,
    approvalchange: InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput = identity,
    approvalbyteschange: Array[Byte] => Array[Byte] = identity,
    approvalmanifesthashstale: Boolean = false,
    includeapproval: Boolean = true,
    approvalrequired: Boolean = true,
    approvalrole: String = "approval",
    approvaldependencies: Option[Vector[String]] = None,
    siblingapprovals: Vector[(String, Array[Byte])] = Vector.empty,
    includereview: Boolean = true,
    reviewrole: String = "validation",
    reviewid: String = "review-main",
    reviewpath: String = "reviews/candidate-review.json",
    reviewdependencies: Option[Vector[String]] = None,
    reviewstate: String = "reviewed",
    allnullablefacets: Boolean = false,
    continuityprofile: String = "ccdm-projection-binding-v1",
    continuityschema: String = "1.0"
  )

  private final case class FixtureData(
    expected: InternalModelCandidateHumanApprovalInput,
    executionbasis: InternalModelCandidateReviewExecutionBasis,
    approvalbytes: Array[Byte],
    originalapprovalbytes: Array[Byte],
    carriermanifest: Array[Byte],
    historicalmanifest: Array[Byte],
    reviewbytes: Array[Byte],
    reviewbinding: InternalModelCandidateReviewBinding
  )

  private val _printer = Printer.noSpacesSortKeys
  private val _project_yaml = "project:\n  namespace: org.example\n  id: candidate-review-sample\n"
  private val _package_id = "01234567-89ab-cdef-0123-456789abcdef"
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_alpha_raw = "alpha cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_beta_raw = "beta cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _model_source = _source("model-authority", "model-source", Some("catalog/model-source"), Some("revision-1"), _sha256(_model_raw.toArray))
  private val _cml_alpha_source = _source("cml-authority", "cml-alpha", Some("catalog/cml-alpha"), Some("revision-alpha"), _sha256(_cml_alpha_raw.toArray))
  private val _cml_beta_source = _source("cml-authority", "cml-beta", Some("catalog/cml-beta"), Some("revision-beta"), _sha256(_cml_beta_raw.toArray))

  "Candidate human approval admission" should {
    "filesystem admission" which {
      "retain a complete independently supplied Approved record and its exact full review basis" in {
        Given("a complete private two-target package with a historical V1 manifest, a newer carrier, and independently authored human input")
        var admission = Option.empty[InternalModelCandidateHumanApprovalAdmission]
        var expected = Option.empty[InternalModelCandidateHumanApprovalInput]
        var approvalhash = ""
        var reviewhash = ""
        var historicalbytes = Vector.empty[Byte]
        var carrierbytes = Vector.empty[Byte]
        var historicalbasis = Option.empty[InternalModelCandidateHumanApprovalPackageBasis]
        var carrierbasis = Option.empty[InternalModelCandidateHumanApprovalPackageBasis]
        var reviewbinding = Option.empty[InternalModelCandidateReviewBinding]
        var diagnostic = ""
        When("the selected approval and review are admitted through the actual filesystem validator")
        _with_fixture() { (root, fixture) =>
          expected = Some(fixture.expected)
          approvalhash = _sha256(fixture.approvalbytes)
          reviewhash = _sha256(fixture.reviewbytes)
          historicalbytes = fixture.historicalmanifest.toVector
          carrierbytes = fixture.carriermanifest.toVector
          historicalbasis = Some(_package_basis(fixture.historicalmanifest))
          carrierbasis = Some(_package_basis(fixture.carriermanifest))
          reviewbinding = Some(fixture.reviewbinding)
          val result = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected)
          diagnostic = result.show
          admission = result.toOption
        }
        Then("the selected approval, review hash, all basis tuples, two target snapshots, historical manifest, rationale, and duplicate items survive exactly")
        withClue(diagnostic) {
          admission should not be empty
          val value = admission.get
          value.record.approval shouldBe expected.get
          value.approvalArtifactId shouldBe "approval-main"
          value.approvalArtifactSha256 shouldBe approvalhash
          value.approvalArtifactPackageRelativePath shouldBe "approvals/main.json"
          value.reviewAdmission.reviewArtifactSha256 shouldBe reviewhash
          value.reviewAdmission.reviewedPackageContext.manifestBytes shouldBe historicalbytes
          value.reviewAdmission.carrierPackageContext.manifestBytes shouldBe carrierbytes
          value.reviewAdmission.carrierPackageContext.manifestBytes should not equal historicalbytes
          value.reviewAdmission.reviewedPackageContext.packageDigest shouldBe historicalbasis.get.packageDigest
          value.reviewAdmission.carrierPackageContext.packageDigest shouldBe carrierbasis.get.packageDigest
          value.reviewAdmission.reviewedPackageContext.revision shouldBe 1L
          value.reviewAdmission.carrierPackageContext.revision shouldBe 3L
          value.reviewAdmission.reviewedPackageContext.packageDigest should not equal value.reviewAdmission.carrierPackageContext.packageDigest
          value.reviewAdmission.binding.targets shouldBe reviewbinding.get.targets
          value.reviewAdmission.binding.evidenceArtifacts shouldBe reviewbinding.get.evidenceArtifacts
          value.reviewAdmission.semanticDiffAdmission.candidateAdmission.projection shouldBe _projection("ccdm-projection-binding-v1").copy(canonicalBytes = InternalModelCandidateCmlProjectionCodec.encode(_projection("ccdm-projection-binding-v1")).toVector)
          value.reviewAdmission.binding.targets.map(value => value.targetId) shouldBe Vector("target-alpha", "target-beta")
          value.reviewAdmission.semanticDiffAdmission.candidateAdmission.targetBytes.map(value => value.proposedRawBytes) shouldBe Vector(Vector[Byte](0, -1, 10), _cml_beta_raw)
          value.reviewAdmission.reviewedPackageContext.artifacts.map(value => (value.artifactId, value.required, value.present)).find(_._1 == "snapshot-optional") shouldBe Some(("snapshot-optional", false, false))
        }
      }

      "retain Rejected and ChangesRequested as explicit decisions" in {
        Given("two complete filesystem packages whose persisted decisions are independently authored Rejected and ChangesRequested records")
        val decisions = Vector(InternalModelCandidateHumanApprovalDecision.Rejected, InternalModelCandidateHumanApprovalDecision.ChangesRequested)
        var admissions = Vector.empty[Option[InternalModelCandidateHumanApprovalAdmission]]
        When("each selected record is admitted through the actual filesystem validator")
        admissions = decisions.map { decision =>
          var admission = Option.empty[InternalModelCandidateHumanApprovalAdmission]
          _with_fixture(FixtureOptions(approvalchange = value => value.copy(decision = decision))) { (root, fixture) =>
            admission = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).toOption
          }
          admission
        }
        Then("the declared decision, rationale, and ordered duplicate unresolved items remain recorded without conversion to approval")
        admissions.map(_.map(value => (value.record.approval.decision, value.record.approval.rationale, value.record.approval.unresolvedItems))) shouldBe
          decisions.map(value => Some((value, "The basis was considered.", Vector("follow-up", "later", "follow-up"))))
      }
    }

    "provider and attribution boundary" which {
      "not manufacture human approval from provider evidence or missing human input" in {
        Given("a complete package with provider-approved review evidence and no selected human approval artifact")
        var reviewadmitted = false
        var humanadmitted = true
        When("the review and human-approval validators are invoked against the same real package")
        _with_fixture(FixtureOptions(includeapproval = false, reviewstate = "approved", allnullablefacets = true)) { (root, fixture) =>
          reviewadmitted = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", fixture.executionbasis).isSuccess
          humanadmitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
        }
        Then("provider approval evidence remains review evidence and cannot establish a human approval result")
        reviewadmitted shouldBe true
        humanadmitted shouldBe false
      }

      "reject provider-shaped actor and provenance through actual admission" in {
        Given("a valid package whose selected approval bytes are changed to provider actor and provider provenance variants while manifest hashes are recomputed")
        var actualinput = _input()
        _with_fixture() { (_, fixture) =>
          actualinput = fixture.expected
        }
        val values = Vector(
          "actor-kind" -> _approval_mutation(actualinput)(approval => approval.add("actor", Json.fromJsonObject(approval("actor").flatMap(_.asObject).get.add("kind", Json.fromString("provider"))))),
          "provenance-authority" -> _source_mutation(actualinput)(source => source.add("authority", Json.fromString("provider")))
        )
        When("each provider-shaped record crosses the actual human-approval validator")
        val results = values.map { case (_, bytes) =>
          var admitted = true
          _with_fixture(FixtureOptions(approvalbyteschange = _ => bytes)) { (root, fixture) =>
            admitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
          }
          admitted
        }
        Then("neither provider actor nor provider provenance can pass the human decision boundary")
        results shouldBe Vector(false, false)
      }
    }
  }

  "Candidate human approval closed grammar" should {
    "closed root and nested boundaries" which {
      "reject every missing, extra, duplicate, wrong/null, transport, layout, Unicode, enum, digest, and integer variant through actual admission" in {
        Given("one valid captured package and a mutation table covering root, approval, actor, basis, artifact, package, scope, and provenance boundaries")
        var input = _input()
        var canonical = Array.empty[Byte]
        _with_fixture() { (_, fixture) =>
          input = fixture.expected
          canonical = fixture.originalapprovalbytes
        }
        val variants = Vector(
          "invalid-utf8" -> Array[Byte](0xc3.toByte, 0x28.toByte),
          "bom" -> (Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical),
          "leading-space" -> (" " + new String(canonical, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),
          "trailing-space" -> (canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8)),
          "crlf" -> new String(canonical, StandardCharsets.UTF_8).replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8),
          "extra-final-lf" -> (canonical ++ Array('\n'.toByte)),
          "missing-final-lf" -> canonical.dropRight(1),
          "invalid-surrogate" -> _invalid_surrogate(canonical),
          "root-missing-approval" -> _root_mutation(input)(_.remove("approval")),
          "root-extra-member" -> _root_mutation(input)(_.add("unexpected", Json.True)),
          "root-duplicate-member" -> _duplicate_member(canonical, "profile"),
          "root-wrong-approval-object" -> _root_mutation(input)(_.add("approval", Json.Null)),
          "root-wrong-profile-type" -> _root_mutation(input)(_.add("profile", Json.fromInt(1))),
          "root-wrong-schema-type" -> _root_mutation(input)(_.add("schemaVersion", Json.Null)),
          "approval-missing-member" -> _approval_mutation(input)(_.remove("actor")),
          "approval-extra-member" -> _approval_mutation(input)(_.add("unexpected", Json.True)),
          "approval-duplicate-member" -> _duplicate_member(canonical, "rationale"),
          "approval-wrong-actor-object" -> _approval_mutation(input)(_.add("actor", Json.Null)),
          "approval-wrong-basis-type" -> _approval_mutation(input)(_.add("basis", Json.arr())),
          "approval-null-required-scalar" -> _approval_mutation(input)(_.add("rationale", Json.Null)),
          "approval-wrong-array-type" -> _approval_mutation(input)(_.add("unresolvedItems", Json.fromString("item"))),
          "approval-invalid-enum" -> _approval_mutation(input)(_.add("decision", Json.fromString("superseded"))),
          "actor-missing-member" -> _actor_mutation(input)(_.remove("identity")),
          "actor-extra-member" -> _actor_mutation(input)(_.add("unexpected", Json.True)),
          "actor-duplicate-member" -> _duplicate_member(canonical, "role"),
          "actor-wrong-required-type" -> _actor_mutation(input)(_.add("identity", Json.fromInt(1))),
          "actor-null-required-type" -> _actor_mutation(input)(_.add("kind", Json.Null)),
          "basis-missing-member" -> _basis_mutation(input)(_.remove("candidateArtifact")),
          "basis-extra-member" -> _basis_mutation(input)(_.add("unexpected", Json.True)),
          "basis-duplicate-member" -> _duplicate_member(canonical, "candidateIdentity"),
          "basis-wrong-artifact-object" -> _basis_mutation(input)(_.add("candidateArtifact", Json.Null)),
          "basis-wrong-required-type" -> _basis_mutation(input)(_.add("candidateRevision", Json.fromString("7"))),
          "artifact-missing-member" -> _artifact_mutation(input)(_.remove("artifactId")),
          "artifact-extra-member" -> _artifact_mutation(input)(_.add("unexpected", Json.True)),
          "artifact-duplicate-member" -> _duplicate_member_at(canonical, "sha256", 0),
          "artifact-wrong-required-type" -> _artifact_mutation(input)(_.add("artifactId", Json.fromInt(1))),
          "artifact-null-required-type" -> _artifact_mutation(input)(_.add("sha256", Json.Null)),
          "artifact-invalid-digest" -> _artifact_mutation(input)(_.add("sha256", Json.fromString("SHA256:" + ("0" * 64)))),
          "review-artifact-missing-member" -> _review_artifact_mutation(input)(_.remove("artifactId")),
          "review-artifact-extra-member" -> _review_artifact_mutation(input)(_.add("unexpected", Json.True)),
          "review-artifact-duplicate-member" -> _duplicate_member_at(canonical, "sha256", 1),
          "review-artifact-wrong-object-type" -> _basis_mutation(input)(_.add("reviewArtifact", Json.Null)),
          "review-artifact-wrong-required-type" -> _review_artifact_mutation(input)(_.add("artifactId", Json.fromInt(1))),
          "review-artifact-null-required-type" -> _review_artifact_mutation(input)(_.add("sha256", Json.Null)),
          "review-artifact-invalid-digest" -> _review_artifact_mutation(input)(_.add("sha256", Json.fromString("SHA256:" + ("0" * 64)))),
          "diff-artifact-missing-member" -> _diff_artifact_mutation(input)(_.remove("artifactId")),
          "diff-artifact-extra-member" -> _diff_artifact_mutation(input)(_.add("unexpected", Json.True)),
          "diff-artifact-duplicate-member" -> _duplicate_member_at(canonical, "sha256", 2),
          "diff-artifact-wrong-object-type" -> _basis_mutation(input)(_.add("semanticDiffArtifact", Json.Null)),
          "diff-artifact-wrong-required-type" -> _diff_artifact_mutation(input)(_.add("artifactId", Json.fromInt(1))),
          "diff-artifact-null-required-type" -> _diff_artifact_mutation(input)(_.add("sha256", Json.Null)),
          "diff-artifact-invalid-digest" -> _diff_artifact_mutation(input)(_.add("sha256", Json.fromString("SHA256:" + ("0" * 64)))),
          "package-missing-member" -> _package_mutation(input)(_.remove("packageId")),
          "package-extra-member" -> _package_mutation(input)(_.add("unexpected", Json.True)),
          "package-duplicate-member" -> _duplicate_member(canonical, "projectId"),
          "package-wrong-object-type" -> _basis_mutation(input)(_.add("reviewedPackage", Json.Null)),
          "package-null-required-type" -> _package_mutation(input)(_.add("packageId", Json.Null)),
          "package-invalid-integer" -> _package_mutation(input)(_.add("revision", Json.fromDoubleOrNull(1.0))),
          "package-overflow-integer" -> _package_mutation(input)(_.add("revision", io.circe.parser.parse((BigInt(Long.MaxValue) + 1).toString).toOption.get)),
          "scope-missing-member" -> _scope_mutation(input)(_.remove("componentIdentity")),
          "scope-extra-member" -> _scope_mutation(input)(_.add("unexpected", Json.True)),
          "scope-duplicate-member" -> _duplicate_member(canonical, "projectionContextIdentity"),
          "scope-wrong-object-type" -> _basis_mutation(input)(_.add("scope", Json.Null)),
          "scope-null-required-type" -> _scope_mutation(input)(_.add("componentIdentity", Json.Null)),
          "scope-wrong-required-type" -> _scope_mutation(input)(_.add("selectedUseCaseElementIdentity", Json.arr())),
          "provenance-missing-member" -> _source_mutation(input)(_.remove("authority")),
          "provenance-missing-locator" -> _source_mutation(input)(_.remove("locator")),
          "provenance-missing-revision" -> _source_mutation(input)(_.remove("revision")),
          "provenance-extra-member" -> _source_mutation(input)(_.add("unexpected", Json.True)),
          "provenance-duplicate-member" -> _duplicate_member(canonical, "locator"),
          "provenance-wrong-object-type" -> _approval_mutation(input)(_.add("provenance", Json.Null)),
          "provenance-null-required-type" -> _source_mutation(input)(_.add("identity", Json.Null)),
          "provenance-wrong-nullable-type" -> _source_mutation(input)(_.add("locator", Json.fromInt(1))),
          "provenance-wrong-nullable-revision-type" -> _source_mutation(input)(_.add("revision", Json.fromInt(1))),
          "provenance-invalid-digest" -> _source_mutation(input)(_.add("sha256", Json.fromString("sha256:0"))),
          "approval-revision-zero" -> _approval_mutation(input)(_.add("approvalRevision", Json.fromInt(0))),
          "approval-revision-overflow" -> _approval_mutation(input)(_.add("approvalRevision", Json.fromLong(Int.MaxValue.toLong + 1L))),
          "approval-revision-fraction" -> _approval_mutation(input)(_.add("approvalRevision", Json.fromDoubleOrNull(1.0))),
          "wrong-profile" -> _root_mutation(input)(_.add("profile", Json.fromString("ccdm-candidate-human-approval-next"))),
          "wrong-schema" -> _root_mutation(input)(_.add("schemaVersion", Json.fromString("2.0")))
        )
        When("each canonical mutation is written with its recomputed manifest entry hash and admitted by the actual validator")
        val results = variants.map { case (name, bytes) =>
          val codecresult = InternalModelCandidateHumanApprovalCodec.decode(bytes.toVector)
          var admitted = true
          _with_fixture(FixtureOptions(approvalbyteschange = _ => bytes)) { (root, fixture) =>
            admitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
          }
          name -> (codecresult.isLeft, admitted)
        }
        Then("the valid control succeeds and every closed-boundary mutation fails without lexical-only placeholder proof")
        InternalModelCandidateHumanApprovalCodec.decode(canonical.toVector).isRight shouldBe true
        _with_fixture() { (root, fixture) =>
          InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess shouldBe true
        }
        variants.filter(_._1.contains("duplicate")).foreach { case (_, bytes) =>
          new io.circe.jawn.JawnParser(maxValueSize = None, allowDuplicateKeys = true).parse(new String(bytes, StandardCharsets.UTF_8)).isRight shouldBe true
        }
        results.forall(value => value._2._1 && !value._2._2) shouldBe true
      }
    }

    "scalar fidelity" which {
      "reject Unicode-whitespace-only required and nullable human approval fields through codec and actual filesystem admission" in {
        Given("independently authored approval input variants containing one Unicode-whitespace-only field at a time")
        val whitespacevalues = Vector(
          (0 to 0x20).map(_.toChar).mkString,
          "\u0085",
          "\u00a0",
          "\u1680",
          "\u2000",
          "\u2001",
          "\u2002",
          "\u2003",
          "\u2004",
          "\u2005",
          "\u2006",
          "\u2007",
          "\u2008",
          "\u2009",
          "\u200a",
          "\u2028",
          "\u2029",
          "\u202f",
          "\u205f",
          "\u3000",
          "\u0000\u0085\u00a0\u2003\u2028\u3000"
        )
        val typedfields = whitespacevalues.flatMap { whitespace =>
          Vector(
            "actor.kind" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(actor = value.actor.copy(kind = whitespace))),
            "actor.identity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(actor = value.actor.copy(identity = whitespace))),
            "actor.role" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(actor = value.actor.copy(role = whitespace))),
            "approvalIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(approvalIdentity = whitespace)),
            "provenance.authority" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(provenance = value.provenance.copy(authority = whitespace))),
            "provenance.identity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(provenance = value.provenance.copy(identity = whitespace))),
            "provenance.locator" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(provenance = value.provenance.copy(locator = Some(whitespace)))),
            "provenance.revision" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(provenance = value.provenance.copy(revision = Some(whitespace)))),
            "rationale" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(rationale = whitespace)),
            "unresolvedItems[0]" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(unresolvedItems = Vector(whitespace, "later", "follow-up"))),
            "basis.candidateArtifact.artifactId" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(candidateArtifact = value.basis.candidateArtifact.copy(artifactId = whitespace)))),
            "basis.candidateIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(candidateIdentity = whitespace))),
            "basis.candidateModelIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(candidateModelIdentity = whitespace))),
            "basis.reviewArtifact.artifactId" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(reviewArtifact = value.basis.reviewArtifact.copy(artifactId = whitespace)))),
            "basis.reviewIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(reviewIdentity = whitespace))),
            "basis.reviewedPackage.schemaVersion" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(schemaVersion = whitespace)))),
            "basis.scope.componentIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(componentIdentity = whitespace)))),
            "basis.scope.projectionContextIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(projectionContextIdentity = whitespace)))),
            "basis.scope.selectedUseCaseElementIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(selectedUseCaseElementIdentity = whitespace)))),
            "basis.semanticDiffArtifact.artifactId" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(semanticDiffArtifact = value.basis.semanticDiffArtifact.copy(artifactId = whitespace)))),
            "basis.semanticDiffIdentity" -> ((value: InternalModelCandidateHumanApprovalInput) => value.copy(basis = value.basis.copy(semanticDiffIdentity = whitespace)))
          )
        }
        val directfields = whitespacevalues.flatMap { whitespace =>
          Vector(
            "profile" -> ((bytes: Array[Byte]) => _root_bytes_mutation(bytes)(root => root.add("profile", Json.fromString(whitespace)))),
            "schemaVersion" -> ((bytes: Array[Byte]) => _root_bytes_mutation(bytes)(root => root.add("schemaVersion", Json.fromString(whitespace)))),
            "decision" -> ((bytes: Array[Byte]) => _approval_bytes_mutation(bytes)(approval => approval.add("decision", Json.fromString(whitespace))))
          )
        }
        val variants = typedfields.map { case (name, change) => name -> FixtureOptions(approvalchange = change) } ++ directfields.map { case (name, change) => name -> FixtureOptions(approvalbyteschange = change) }
        var controlcodecaccepted = false
        var controladmitted = false
        When("the valid control and each one-field mutation are encoded, stored with recomputed hashes, and passed to actual admission")
        _with_fixture() { (root, fixture) =>
          controlcodecaccepted = InternalModelCandidateHumanApprovalCodec.decode(fixture.approvalbytes.toVector).isRight
          controladmitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
        }
        val failures = variants.map { case (name, options) =>
          var codecaccepted = true
          var admitted = true
          _with_fixture(options) { (root, fixture) =>
            codecaccepted = InternalModelCandidateHumanApprovalCodec.decode(fixture.approvalbytes.toVector).isRight
            admitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
          }
          name -> (codecaccepted, admitted)
        }
        Then("the valid control remains accepted and every required, nullable, and unresolved-item whitespace-only field is rejected by both boundaries")
        controlcodecaccepted shouldBe true
        controladmitted shouldBe true
        failures should have size (whitespacevalues.size * 24)
        failures.forall(value => !value._2._1 && !value._2._2) shouldBe true
      }

      "round-trip Unicode identities, prose, ordered duplicates, and deterministic bytes" in {
        Given("generated Unicode scalar prose used independently as identity, rationale, and duplicate unresolved items")
        forAll(Gen.nonEmptyListOf(Gen.oneOf(Gen.alphaNumChar.map(_.toString), Gen.const("漢"), Gen.const("😀"), Gen.const("é"), Gen.const("e\u0301"))).map(_.mkString)) { generated =>
          val spaced = s" \u00a0${generated}\u2003\u2028\u3000"
          val input = _input().copy(
            approvalIdentity = spaced,
            rationale = spaced,
            provenance = _input().provenance.copy(locator = Some(s"\u0085${generated}\u202f"), revision = Some(s"\u205f${generated}")),
            unresolvedItems = Vector(spaced, "distinct item", spaced)
          )
          When("the same input is encoded twice and decoded through the strict codec")
          val first = _approval_bytes(input)
          val second = _approval_bytes(input)
          val decoded = InternalModelCandidateHumanApprovalCodec.decode(first)
          Then("the supplied Unicode, order, duplicate items, and canonical bytes remain unchanged")
          first shouldBe second
          decoded.map(_.approval) shouldBe Right(input)
        }
      }

      "accept explicitly null nullable provenance locator and revision values" in {
        Given("a real valid package whose independently authored human input omits both nullable provenance values explicitly as null")
        var admitted = false
        When("the null-valued provenance record is encoded, stored, and passed to actual admission")
        _with_fixture(FixtureOptions(approvalchange = value => value.copy(provenance = value.provenance.copy(locator = None, revision = None)))) { (root, fixture) =>
          admitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
        }
        Then("explicit null locator and revision remain valid nullable values")
        admitted shouldBe true
      }
    }
  }

  "Candidate human approval independent-input boundary" should {
    "reject every independently supplied expected-input mismatch and invalid typed value" which {
      "compare actor, decision, provenance, rationale, items, and every basis dimension exactly" in {
        Given("a real valid package and a separately authored expected-input mismatch matrix")
        val cases: Vector[(String, InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput)] = Vector(
          "actor-kind" -> (value => value.copy(actor = value.actor.copy(kind = "provider"))),
          "actor-identity" -> (value => value.copy(actor = value.actor.copy(identity = "human-other"))),
          "actor-role" -> (value => value.copy(actor = value.actor.copy(role = "approver"))),
          "approval-identity" -> (value => value.copy(approvalIdentity = "approval-other")),
          "approval-revision" -> (value => value.copy(approvalRevision = 2)),
          "decision" -> (value => value.copy(decision = InternalModelCandidateHumanApprovalDecision.Rejected)),
          "provenance-authority" -> (value => value.copy(provenance = value.provenance.copy(authority = "human-record"))),
          "provenance-identity" -> (value => value.copy(provenance = value.provenance.copy(identity = "human-other"))),
          "provenance-locator" -> (value => value.copy(provenance = value.provenance.copy(locator = Some("other-locator")))),
          "provenance-revision" -> (value => value.copy(provenance = value.provenance.copy(revision = Some("2")))),
          "provenance-digest" -> (value => value.copy(provenance = value.provenance.copy(sha256 = _digest("other-provenance")))),
          "rationale" -> (value => value.copy(rationale = "A different rationale.")),
          "items-order" -> (value => value.copy(unresolvedItems = Vector("later", "follow-up", "follow-up"))),
          "items-duplicates" -> (value => value.copy(unresolvedItems = Vector("follow-up", "later"))),
          "candidate-artifact-id" -> (value => value.copy(basis = value.basis.copy(candidateArtifact = InternalModelCandidateReviewArtifact("candidate-other", value.basis.candidateArtifact.sha256)))),
          "candidate-artifact-hash" -> (value => value.copy(basis = value.basis.copy(candidateArtifact = value.basis.candidateArtifact.copy(sha256 = _digest("candidate-other"))))),
          "candidate-identity" -> (value => value.copy(basis = value.basis.copy(candidateIdentity = "candidate-other"))),
          "candidate-model-identity" -> (value => value.copy(basis = value.basis.copy(candidateModelIdentity = "candidate-model-other"))),
          "candidate-revision" -> (value => value.copy(basis = value.basis.copy(candidateRevision = 8))),
          "review-artifact-id" -> (value => value.copy(basis = value.basis.copy(reviewArtifact = InternalModelCandidateReviewArtifact("review-other", value.basis.reviewArtifact.sha256)))),
          "review-artifact-hash" -> (value => value.copy(basis = value.basis.copy(reviewArtifact = value.basis.reviewArtifact.copy(sha256 = _digest("review-other"))))),
          "review-identity" -> (value => value.copy(basis = value.basis.copy(reviewIdentity = "review-other"))),
          "review-revision" -> (value => value.copy(basis = value.basis.copy(reviewRevision = 4))),
          "diff-artifact-id" -> (value => value.copy(basis = value.basis.copy(semanticDiffArtifact = InternalModelCandidateReviewArtifact("diff-other", value.basis.semanticDiffArtifact.sha256)))),
          "diff-artifact-hash" -> (value => value.copy(basis = value.basis.copy(semanticDiffArtifact = value.basis.semanticDiffArtifact.copy(sha256 = _digest("diff-other"))))),
          "diff-identity" -> (value => value.copy(basis = value.basis.copy(semanticDiffIdentity = "diff-other"))),
          "diff-revision" -> (value => value.copy(basis = value.basis.copy(semanticDiffRevision = 4))),
          "scope-component" -> (value => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(componentIdentity = "component-other")))),
          "scope-context" -> (value => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(projectionContextIdentity = "context-other")))),
          "scope-usecase" -> (value => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(selectedUseCaseElementIdentity = "usecase-other")))),
          "package-digest" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(packageDigest = _digest("package-other"))))),
          "package-id" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(packageId = "fedcba98-7654-3210-fedc-ba9876543210")))),
          "package-project" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(projectId = "other-project")))),
          "package-namespace" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(projectNamespace = "other.example")))),
          "package-revision" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(revision = 2L)))),
          "package-schema" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(schemaVersion = "2.0")))),
          "invalid-actor" -> (value => value.copy(actor = value.actor.copy(identity = " "))),
          "invalid-revision" -> (value => value.copy(approvalRevision = 0)),
          "invalid-provenance-digest" -> (value => value.copy(provenance = value.provenance.copy(sha256 = "sha256:0"))),
          "invalid-rationale" -> (value => value.copy(rationale = " ")),
          "invalid-item" -> (value => value.copy(unresolvedItems = Vector(" ")))
        )
        When("each mismatch is passed as the independently supplied expected input to actual admission")
        val results = cases.map { case (name, change) =>
          var admitted = true
          _with_fixture() { (root, fixture) =>
            admitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, change(fixture.expected)).isSuccess
          }
          name -> admitted
        }
        Then("no mismatching or invalid typed expected input can weaken exact admission equality")
        results.forall(_._2 == false) shouldBe true
      }
    }

    "separate stored-basis binding" which {
      "reject stored and independently expected basis changes after equality succeeds" in {
        Given("a real package and paired persisted/expected changes for candidate, review, diff, scope, and all six historical package fields")
        val cases: Vector[(String, InternalModelCandidateHumanApprovalInput => InternalModelCandidateHumanApprovalInput)] = Vector(
          "candidate-tuple-id" -> (value => value.copy(basis = value.basis.copy(candidateArtifact = InternalModelCandidateReviewArtifact("candidate-other", value.basis.candidateArtifact.sha256)))),
          "candidate-tuple-hash" -> (value => value.copy(basis = value.basis.copy(candidateArtifact = value.basis.candidateArtifact.copy(sha256 = _digest("candidate-other"))))),
          "candidate-identity" -> (value => value.copy(basis = value.basis.copy(candidateIdentity = "candidate-other"))),
          "candidate-model" -> (value => value.copy(basis = value.basis.copy(candidateModelIdentity = "candidate-model-other"))),
          "candidate-revision" -> (value => value.copy(basis = value.basis.copy(candidateRevision = 8))),
          "review-tuple-id" -> (value => value.copy(basis = value.basis.copy(reviewArtifact = InternalModelCandidateReviewArtifact("review-other", value.basis.reviewArtifact.sha256)))),
          "review-tuple-hash" -> (value => value.copy(basis = value.basis.copy(reviewArtifact = value.basis.reviewArtifact.copy(sha256 = _digest("review-other"))))),
          "review-identity" -> (value => value.copy(basis = value.basis.copy(reviewIdentity = "review-other"))),
          "review-revision" -> (value => value.copy(basis = value.basis.copy(reviewRevision = 4))),
          "diff-tuple-id" -> (value => value.copy(basis = value.basis.copy(semanticDiffArtifact = InternalModelCandidateReviewArtifact("diff-other", value.basis.semanticDiffArtifact.sha256)))),
          "diff-tuple-hash" -> (value => value.copy(basis = value.basis.copy(semanticDiffArtifact = value.basis.semanticDiffArtifact.copy(sha256 = _digest("diff-other"))))),
          "diff-identity" -> (value => value.copy(basis = value.basis.copy(semanticDiffIdentity = "diff-other"))),
          "diff-revision" -> (value => value.copy(basis = value.basis.copy(semanticDiffRevision = 4))),
          "scope-component" -> (value => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(componentIdentity = "component-other")))),
          "scope-context" -> (value => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(projectionContextIdentity = "context-other")))),
          "scope-usecase" -> (value => value.copy(basis = value.basis.copy(scope = value.basis.scope.copy(selectedUseCaseElementIdentity = "usecase-other")))),
          "package-digest" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(packageDigest = _digest("package-other"))))),
          "package-id" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(packageId = "fedcba98-7654-3210-fedc-ba9876543210")))),
          "package-project" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(projectId = "other-project")))),
          "package-namespace" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(projectNamespace = "other.example")))),
          "package-revision" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(revision = 2L)))),
          "package-schema" -> (value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(schemaVersion = "2.0"))))
        )
        When("each changed stored record and independently authored expected input is admitted against the unchanged captured review")
        val results = cases.map { case (name, change) =>
          var admitted = true
          _with_fixture(FixtureOptions(approvalchange = change)) { (root, fixture) =>
            admitted = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
          }
          name -> admitted
        }
        Then("equality cannot masquerade as independent candidate-review binding and every changed basis dimension fails closed")
        results.forall(_._2 == false) shouldBe true
      }
    }
  }

  "Candidate human approval selection and capture boundary" should {
    "explicit selection and lexical capture" which {
      "reject wrong selectors, absence, role, dependency, tampering, and captured syntax while accepting a valid control" in {
        Given("a valid real package and a table of explicit approval/review selection and captured-artifact failures")
        var actualapprovalbytes = Array.empty[Byte]
        _with_fixture() { (_, fixture) => actualapprovalbytes = fixture.originalapprovalbytes }
        val staleapprovalbytes = actualapprovalbytes.updated(0, ' '.toByte)
        staleapprovalbytes should not equal actualapprovalbytes
        val cases = Vector[(String, FixtureOptions, String, String)](
          ("wrong-approval-id", FixtureOptions(), "approval-missing", "review-main"),
          ("wrong-review-id", FixtureOptions(), "approval-main", "review-missing"),
          ("missing-approval", FixtureOptions(includeapproval = false), "approval-main", "review-main"),
          ("optional-approval-absent", FixtureOptions(approvalrequired = false), "approval-main", "review-main"),
          ("wrong-approval-role", FixtureOptions(approvalrole = "validation"), "approval-main", "review-main"),
          ("wrong-approval-dependency", FixtureOptions(approvaldependencies = Some(Vector("candidate-main"))), "approval-main", "review-main"),
          ("stale-approval-hash", FixtureOptions(approvalmanifesthashstale = true, approvalbyteschange = _ => staleapprovalbytes), "approval-main", "review-main"),
          ("captured-syntax", FixtureOptions(approvalbyteschange = bytes => bytes.dropRight(1)), "approval-main", "review-main")
        )
        When("each explicit selector or captured artifact is sent to the actual validator")
        val results = cases.map { case (name, options, approvalid, reviewid) =>
          var admitted = true
          _with_fixture(options) { (root, fixture) =>
            admitted = InternalModelCandidateHumanApprovalValidator.validate(root, approvalid, reviewid, fixture.executionbasis, fixture.expected).isSuccess
          }
          name -> admitted
        }
        Then("only a present approval-role artifact with exactly the selected review dependency and intact bytes can pass")
        results.forall(_._2 == false) shouldBe true
        _with_fixture() { (root, fixture) =>
          InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess shouldBe true
        }
      }

      "select one valid approval among inert unknown-profile siblings without ordinal or latest heuristics" in {
        Given("a valid selected approval and two inert sibling approval records with unknown profiles and exact review dependencies")
        val siblings = Vector(
          "approval-sibling-a" -> _unknown_profile_bytes(_input().copy(approvalIdentity = "approval-sibling-a")),
          "approval-sibling-b" -> _unknown_profile_bytes(_input().copy(approvalIdentity = "approval-sibling-b"))
        )
        var selected = false
        var unknownselected = true
        When("the selected valid ID and then an explicit unknown sibling ID are passed to actual admission")
        _with_fixture(FixtureOptions(siblingapprovals = siblings)) { (root, fixture) =>
          selected = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
          unknownselected = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-sibling-a", "review-main", fixture.executionbasis, fixture.expected).isSuccess
        }
        Then("the valid selection succeeds despite inert siblings and selecting an unknown profile fails")
        selected shouldBe true
        unknownselected shouldBe false
      }
    }
  }

  "Candidate human approval review binding" should {
    "independent basis and historical package boundaries" which {
      "reject rules/providers drift and incomplete or mismatched review evidence" in {
        Given("a complete package plus independently drifted rule/provider bases and malformed selected review bindings")
        var driftedrules = true
        var driftedproviders = true
        var missingtarget = true
        var historicalcarrier = true
        When("each changed review basis is admitted through the actual human validator")
        _with_fixture() { (root, fixture) =>
          driftedrules = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis.copy(rules = Vector(InternalModelCandidateReviewRule("rule-other", "9.0", _digest("rule-other")))), fixture.expected).isSuccess
          driftedproviders = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis.copy(providers = Vector(InternalModelCandidateReviewProvider("provider-other", "9.0", _digest("provider-other")))), fixture.expected).isSuccess
        }
        _with_fixture(FixtureOptions(bindingchange = value => value.copy(targets = value.targets.take(1)))) { (root, fixture) =>
          missingtarget = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
        }
        _with_fixture(FixtureOptions(approvalchange = value => value.copy(basis = value.basis.copy(reviewedPackage = value.basis.reviewedPackage.copy(revision = 2L))))) { (root, fixture) =>
          historicalcarrier = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
        }
        Then("independent execution drift, incomplete review target closure, and rebinding the historical subject to carrier revision fail")
        driftedrules shouldBe false
        driftedproviders shouldBe false
        missingtarget shouldBe false
        historicalcarrier shouldBe false
      }

      "retain the complete historical reviewed package distinct from its newer carrier" in {
        Given("a valid package whose review artifact binds historical revision one while the carrier manifest is revision three")
        var revisions = Option.empty[(Long, Long)]
        When("the selected review and human approval are admitted through the actual validator")
        _with_fixture() { (root, fixture) =>
          revisions = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).toOption.map(value => (value.reviewAdmission.reviewedPackageContext.revision, value.reviewAdmission.carrierPackageContext.revision))
        }
        Then("the retained human basis remains attached to historical revision one and never substitutes carrier revision three")
        revisions shouldBe Some((1L, 3L))
      }
    }
  }

  "Candidate human approval captured handoff" should {
    "pure validation after filesystem mutation" which {
      "preserve a captured valid handoff while fresh path validation fails" in {
        Given("one valid package with known manifest and approval paths captured before those paths are changed")
        var captured = Option.empty[InternalModelCandidateHumanApprovalAdmission]
        var fresh = true
        When("the package is captured once, its manifest and approval bytes are changed, and only validateVerified receives the retained handoff")
        _with_fixture() { (root, fixture) =>
          val handoff = InternalModelPackageValidator.verifiedCandidateHumanApproval(root, "approval-main", "review-main")
          _write(root.resolve("src/main/internal-model/manifest.yaml"), "changed manifest\n".getBytes(StandardCharsets.UTF_8))
          _write(root.resolve("src/main/internal-model/approvals/main.json"), "changed approval\n".getBytes(StandardCharsets.UTF_8))
          captured = handoff.flatMap(value => InternalModelCandidateHumanApprovalValidator.validateVerified(value, fixture.executionbasis, fixture.expected)).toOption
          fresh = InternalModelCandidateHumanApprovalValidator.validate(root, "approval-main", "review-main", fixture.executionbasis, fixture.expected).isSuccess
        }
        Then("the pure captured handoff retains both proposed target bytes and complete context while fresh I/O cannot pass")
        captured.map(value => (value.record.approval.basis.reviewedPackage.revision, value.reviewAdmission.binding.targets.map(_.targetId), value.reviewAdmission.semanticDiffAdmission.candidateAdmission.targetBytes.map(_.proposedRawBytes), value.record.canonicalBytes.nonEmpty)) shouldBe
          Some((1L, Vector("target-alpha", "target-beta"), Vector(Vector[Byte](0, -1, 10), _cml_beta_raw), true))
        fresh shouldBe false
      }
    }
  }

  "Candidate human approval predecessor compatibility" should {
    "legacy readers" which {
      "admit a structurally valid package without approvals through the existing package, candidate, diff, and review readers" in {
        Given("a complete legacy P10-32 package with two targets and no approval artifact")
        var results = Vector.empty[Boolean]
        When("the existing review, package, candidate, and semantic-diff readers are invoked against the real filesystem")
        _with_fixture(FixtureOptions(includeapproval = false)) { (root, fixture) =>
          results = Vector(
            InternalModelCandidateReviewBindingValidator.validate(root, "review-main", fixture.executionbasis).isSuccess,
            InternalModelPackageValidator.verifiedCandidateReviewBinding(root, "review-main").isSuccess,
            InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess,
            InternalModelSemanticDiffValidator.validate(root).isSuccess
          )
        }
        Then("legacy readers retain the full candidate, diff, review, target, and provider evidence contracts without a human approval")
        results shouldBe Vector(true, true, true, true)
      }
    }
  }

  private def _with_fixture(options: FixtureOptions = FixtureOptions())(action: (Path, FixtureData) => Unit): Unit = {
    val projection = _projection(options.continuityprofile)
    val realizationprofile = if options.continuityprofile == "ccdm-projection-binding-v2" then "ccdm-realization-v2" else "ccdm-realization-v1"
    val realization = _realization(realizationprofile)
    val continuity = _binding(options.continuityprofile, options.continuityschema)
    val candidate = InternalModelCandidateCmlProjectionCodec.encode(projection)
    val diff = InternalModelSemanticDiffCodec.encode(_semantic_diff(projection, candidate))
    val model = _model_snapshot()
    val alpha = _cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw)
    val beta = _cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw)
    val evidencealpha = "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8)
    val evidencebeta = "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8)
    val optionalbytes = "optional inventory evidence\n".getBytes(StandardCharsets.UTF_8)
    val historicalentries = Vector(
      _entry("evidence-alpha", "validation", "evidence/alpha.json", true, evidencealpha, Vector.empty),
      _entry("evidence-beta", "validation", "evidence/beta.json", true, evidencebeta, Vector.empty),
      _entry("snapshot-cml-alpha", "source-snapshot", "snapshots/cml-alpha.json", true, alpha, Vector.empty),
      _entry("snapshot-cml-beta", "source-snapshot", "snapshots/cml-beta.json", true, beta, Vector.empty),
      _entry("snapshot-model", "source-snapshot", "snapshots/model.json", true, model, Vector.empty),
      _entry("snapshot-optional", "source-snapshot", "snapshots/optional.json", false, optionalbytes, Vector.empty),
      _entry("realization-main", "realization", "realizations/main.json", true, realization, Vector("snapshot-model")),
      _entry("decision-main", "decision", "decisions/main.json", true, "decision record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      _entry("projection-main", "projection", "projections/continuity.json", true, continuity, Vector("realization-main")),
      _entry("resume-main", "resume", "resumes/main.json", true, "resume record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      _entry("candidate-main", "projection", "projections/candidate.json", true, candidate, Vector("projection-main", "realization-main", "snapshot-cml-alpha", "snapshot-cml-beta")),
      _entry("semantic-diff-main", "projection", "projections/semantic-diff.json", true, diff, Vector("candidate-main"))
    )
    val reviewedmanifest = _manifest(_topological(historicalentries), 1)
    val basebinding = options.bindingchange(_binding_value(reviewedmanifest, realization, continuity, candidate, diff, if options.continuityprofile == "ccdm-projection-binding-v2" then "realization-2.0" else "realization-1.0", options.reviewstate, options.allnullablefacets))
    val reviewbytes = InternalModelCandidateReviewBindingCodec.encode(basebinding)
    val expected = options.approvalchange(_human_input(reviewedmanifest, candidate, reviewbytes, diff))
    val originalapprovalbytes = _approval_bytes(expected).toArray
    val approvalbytes = options.approvalbyteschange(originalapprovalbytes)
    val approvalentrybytes = if options.approvalmanifesthashstale then originalapprovalbytes else approvalbytes
    val carrierentries = _topological(historicalentries) ++ Option.when(options.includereview)(_entry(options.reviewid, options.reviewrole, options.reviewpath, true, reviewbytes, options.reviewdependencies.getOrElse(historicalentries.filter(_.hcursor.get[Boolean]("required").toOption.get).map(_.hcursor.get[String]("artifactId").toOption.get).sorted))).toVector ++ Option.when(options.includeapproval)(_entry("approval-main", options.approvalrole, "approvals/main.json", options.approvalrequired, approvalentrybytes, options.approvaldependencies.getOrElse(Vector("review-main")))).toVector ++ options.siblingapprovals.map { case (id, bytes) => _entry(id, "approval", "approvals/" + id + ".json", true, bytes, Vector("review-main")) }
    val carriermanifest = _manifest(_topological(carrierentries), 3)
    val root = _temporary_root()
    val data = FixtureData(expected, _basis(), approvalbytes, originalapprovalbytes, carriermanifest, reviewedmanifest, reviewbytes, basebinding)
    try {
      _write(root.resolve("project.yaml"), _project_yaml.getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), carriermanifest)
      _write(root.resolve("src/main/internal-model/evidence/alpha.json"), evidencealpha)
      _write(root.resolve("src/main/internal-model/evidence/beta.json"), evidencebeta)
      _write(root.resolve("src/main/internal-model/snapshots/cml-alpha.json"), alpha)
      _write(root.resolve("src/main/internal-model/snapshots/cml-beta.json"), beta)
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), model)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/decisions/main.json"), "decision record\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/projections/continuity.json"), continuity)
      _write(root.resolve("src/main/internal-model/projections/candidate.json"), candidate)
      _write(root.resolve("src/main/internal-model/projections/semantic-diff.json"), diff)
      _write(root.resolve("src/main/internal-model/resumes/main.json"), "resume record\n".getBytes(StandardCharsets.UTF_8))
      if options.includeapproval && options.approvalrequired then _write(root.resolve("src/main/internal-model/approvals/main.json"), approvalbytes)
      options.siblingapprovals.foreach { case (id, bytes) => _write(root.resolve("src/main/internal-model/approvals/" + id + ".json"), bytes) }
      if options.includereview then _write(root.resolve("src/main/internal-model").resolve(options.reviewpath), reviewbytes)
      action(root, data)
    } finally _delete_tree(root)
  }

  private def _human_input(reviewedmanifest: Array[Byte], candidatebytes: Array[Byte], reviewbytes: Array[Byte], diffbytes: Array[Byte]): InternalModelCandidateHumanApprovalInput = {
    val projection = _projection("ccdm-projection-binding-v1")
    val semanticdiff = _semantic_diff(projection, candidatebytes)
    val packagebasis = _package_basis(reviewedmanifest)
    InternalModelCandidateHumanApprovalInput(
      InternalModelDecisionActor("human", "human-1", "reviewer"), "approval-main", 1,
      InternalModelCandidateHumanApprovalBasis(
        _review_artifact("candidate-main", candidatebytes), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision,
        _review_artifact("review-main", reviewbytes), "review-identity", 3, packagebasis, projection.scope,
        _review_artifact("semantic-diff-main", diffbytes), semanticdiff.semanticDiffIdentity, semanticdiff.semanticDiffRevision
      ), InternalModelCandidateHumanApprovalDecision.Approved,
      _source("human-decision", "human-1", Some("review/approval"), Some("1"), _digest("human-decision")),
      "The basis was considered.", Vector("follow-up", "later", "follow-up")
    )
  }

  private def _input(decision: InternalModelCandidateHumanApprovalDecision = InternalModelCandidateHumanApprovalDecision.Approved): InternalModelCandidateHumanApprovalInput = {
    val digest = _digest("basis")
    InternalModelCandidateHumanApprovalInput(
      InternalModelDecisionActor("human", "human-1", "reviewer"), "approval-main", 1,
      InternalModelCandidateHumanApprovalBasis(
        InternalModelCandidateReviewArtifact("candidate-main", digest), "candidate-order-v1", "candidate-model-order", 7,
        InternalModelCandidateReviewArtifact("review-main", digest), "review-identity", 3,
        InternalModelCandidateHumanApprovalPackageBasis(digest, _package_id, "candidate-review-sample", "org.example", 1L, "1.0"),
        InternalModelSemanticScope("component-order", "context-order", "e-usecase"),
        InternalModelCandidateReviewArtifact("semantic-diff-main", digest), "candidate-main", 3
      ), decision, _source("human-decision", "human-1", Some("review/approval"), Some("1"), digest),
      "The basis was considered.", Vector("follow-up", "later", "follow-up")
    )
  }

  private def _package_basis(bytes: Array[Byte]): InternalModelCandidateHumanApprovalPackageBasis = {
    val root = io.circe.parser.parse(new String(bytes, StandardCharsets.UTF_8)).toOption.flatMap(_.asObject).get
    InternalModelCandidateHumanApprovalPackageBasis(root("packageDigest").flatMap(_.asString).get, root("packageId").flatMap(_.asString).get, root("projectId").flatMap(_.asString).get, root("projectNamespace").flatMap(_.asString).get, root("revision").flatMap(_.asNumber).flatMap(_.toLong).get, root("schemaVersion").flatMap(_.asString).get)
  }
  private def _approval_bytes(input: InternalModelCandidateHumanApprovalInput): Vector[Byte] = InternalModelCandidateHumanApprovalCodec.encode(InternalModelCandidateHumanApproval(input, "ccdm-candidate-human-approval-v1", "1.0", Vector.empty)).toVector
  private def _unknown_profile_bytes(input: InternalModelCandidateHumanApprovalInput): Array[Byte] = InternalModelCandidateHumanApprovalCodec.encode(InternalModelCandidateHumanApproval(input, "ccdm-candidate-human-approval-unknown", "1.0", Vector.empty))
  private def _record_json(input: InternalModelCandidateHumanApprovalInput): Json = io.circe.parser.parse(new String(_approval_bytes(input).toArray, StandardCharsets.UTF_8)).toOption.get
  private def _root_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _canonical(Json.fromJsonObject(change(_record_json(input).asObject.get)))
  private def _approval_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _root_mutation(input)(root => root.add("approval", Json.fromJsonObject(change(root("approval").flatMap(_.asObject).get))))
  private def _actor_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _approval_mutation(input)(approval => approval.add("actor", Json.fromJsonObject(change(approval("actor").flatMap(_.asObject).get))))
  private def _basis_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _approval_mutation(input)(approval => approval.add("basis", Json.fromJsonObject(change(approval("basis").flatMap(_.asObject).get))))
  private def _artifact_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _basis_mutation(input)(basis => basis.add("candidateArtifact", Json.fromJsonObject(change(basis("candidateArtifact").flatMap(_.asObject).get))))
  private def _review_artifact_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _basis_mutation(input)(basis => basis.add("reviewArtifact", Json.fromJsonObject(change(basis("reviewArtifact").flatMap(_.asObject).get))))
  private def _diff_artifact_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _basis_mutation(input)(basis => basis.add("semanticDiffArtifact", Json.fromJsonObject(change(basis("semanticDiffArtifact").flatMap(_.asObject).get))))
  private def _package_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _basis_mutation(input)(basis => basis.add("reviewedPackage", Json.fromJsonObject(change(basis("reviewedPackage").flatMap(_.asObject).get))))
  private def _scope_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _basis_mutation(input)(basis => basis.add("scope", Json.fromJsonObject(change(basis("scope").flatMap(_.asObject).get))))
  private def _source_mutation(input: InternalModelCandidateHumanApprovalInput)(change: JsonObject => JsonObject): Array[Byte] = _approval_mutation(input)(approval => approval.add("provenance", Json.fromJsonObject(change(approval("provenance").flatMap(_.asObject).get))))
  private def _root_bytes_mutation(bytes: Array[Byte])(change: JsonObject => JsonObject): Array[Byte] = {
    val root = io.circe.parser.parse(new String(bytes, StandardCharsets.UTF_8)).toOption.flatMap(_.asObject).get
    _canonical(Json.fromJsonObject(change(root)))
  }
  private def _approval_bytes_mutation(bytes: Array[Byte])(change: JsonObject => JsonObject): Array[Byte] = _root_bytes_mutation(bytes) { root =>
    root.add("approval", Json.fromJsonObject(change(root("approval").flatMap(_.asObject).get)))
  }
  private def _invalid_surrogate(bytes: Array[Byte]): Array[Byte] = new String(bytes, StandardCharsets.UTF_8).replace("\"The basis was considered.\"", "\"\\uD800\"").getBytes(StandardCharsets.UTF_8)
  private def _duplicate_member(bytes: Array[Byte], member: String): Array[Byte] = _duplicate_member_at(bytes, member, 0)
  private def _duplicate_member_at(bytes: Array[Byte], member: String, occurrence: Int): Array[Byte] = { val text = new String(bytes, StandardCharsets.UTF_8); val token = "\"" + member + "\":"; var search = 0; var found = -1; var count = 0; while count <= occurrence do { val position = text.indexOf(token, search); if position < 0 then count = occurrence + 1 else { found = position; search = position + token.length; count += 1 } }; val comma = text.indexOf(',', found); val closingbrace = text.indexOf('}', found); val end = Vector(comma, closingbrace).filter(_ >= 0).min; (text.substring(0, end) + "," + text.substring(found, end) + text.substring(end)).getBytes(StandardCharsets.UTF_8) }
  private def _canonical(json: Json): Array[Byte] = (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
  private def _content(bytes: Array[Byte]): InternalModelCandidateCmlContent = InternalModelCandidateCmlContent(bytes.length.toLong, Base64.getEncoder.encodeToString(bytes), _sha256(bytes))

  private def _temporary_root(): Path = { Files.createDirectories(Path.of("target")); Files.createTempDirectory(Path.of("target"), "internal-model-candidate-human-approval-") }
  private def _write(path: Path, bytes: Array[Byte]): Unit = { Files.createDirectories(path.getParent); Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) }
  private def _delete_tree(root: Path): Unit = if Files.exists(root) then { val stream = Files.walk(root); try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists) finally stream.close() }
  private def _sha256(bytes: Array[Byte]): String = "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
  private def _digest(seed: String): String = _sha256(seed.getBytes(StandardCharsets.UTF_8))
  private def _source(authority: String, identity: String, locator: Option[String], revision: Option[String], sha256: String): InternalModelSemanticSource = InternalModelSemanticSource(authority, identity, locator, revision, sha256)
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority), "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null), "sha256" -> Json.fromString(value.sha256))
  private def _scope_json: Json = Json.obj("componentIdentity" -> Json.fromString("component-order"), "projectionContextIdentity" -> Json.fromString("context-order"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase"))

  private def _model_snapshot(): Array[Byte] = { val facts = Vector(_fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono"), _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case"), _fact("element", "opaque-shared", "anchor-opaque-element-enrichment", "enrichment:shared"), _fact("element", "opaque-shared", "anchor-opaque-element-kind-Mono", "kind:Mono"), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-enrichment", "enrichment:shared"), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain", "role:StructuralDomain"), _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain")).sortBy(value => (value.hcursor.get[String]("semanticIdentityKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("sourceAnchor").toOption.get)); _canonical(Json.obj("basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source_json(_model_source))) }
  private def _cml_snapshot(source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte]): Array[Byte] = _canonical(Json.obj("basis" -> Json.obj("byteLength" -> Json.fromLong(rawbytes.length.toLong), "projectRelativePath" -> Json.fromString(path), "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes.toArray))), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("cml-baseline"), "source" -> _source_json(source)))
  private def _projection(continuityprofile: String): InternalModelCandidateCmlProjection = { val alpha = _target("snapshot-cml-alpha", "patch-alpha", "target-alpha", _cml_alpha_source, "cml/alpha.cml", Vector[Byte](0, -1, 10), "mapping-alpha", "opaque-shared", "element", "effect-alpha", "ref-opaque-element-kind-Mono"); val beta = _target("snapshot-cml-beta", "patch-beta", "target-beta", _cml_beta_source, "cml/beta.cml", _cml_beta_raw, "mapping-beta", "opaque-shared", "relationship", "effect-beta", "ref-opaque-relationship-role-StructuralDomain"); InternalModelCandidateCmlProjection("candidate-order-v1", "candidate-model-order", 7, "projection-main", "ccdm-candidate-cml-projection-v1", "realization-main", "1.0", InternalModelSemanticScope("component-order", "context-order", "e-usecase"), Vector(alpha, beta), Vector.empty) }
  private def _target(baselineid: String, patchid: String, targetid: String, source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte], mappingid: String, identity: String, kind: String, effectprefix: String, referenceid: String): InternalModelCandidateCmlTarget = { val assertionid = if kind == "element" then "a-opaque-element-kind-Mono" else "a-opaque-relationship-role-StructuralDomain"; val conditionid = if kind == "element" then "c-opaque-element" else "c-opaque-relationship"; val enrichmentid = if kind == "element" then "z-opaque-element-enrichment" else "z-opaque-relationship-enrichment"; val mapping = InternalModelCandidateCmlMapping(Vector(assertionid), "anchor-" + targetid, "cml-" + targetid, Vector(conditionid), Vector(enrichmentid), mappingid, identity, kind); val effects = Vector(InternalModelCandidateCmlEffect("unknown", "supplied compatibility expectation", effectprefix + "-compatibility", "compatibility", Vector(mappingid), referenceid), InternalModelCandidateCmlEffect("unknown", "supplied migration expectation", effectprefix + "-migration", "migration", Vector(mappingid), referenceid)); InternalModelCandidateCmlTarget(baselineid, effects, Vector(mapping), patchid, path, _content(rawbytes.toArray), source, targetid) }

  private def _realization(profile: String): Array[Byte] = { val schema = if profile == "ccdm-realization-v2" then "2.0" else "1.0"; val v2 = profile == "ccdm-realization-v2"; val assertions = Vector(_assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", Vector.empty, v2), _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", Vector.empty, v2), _assertion("a-opaque-element-kind-Mono", "element", "opaque-shared", "ref-opaque-element-kind-Mono", "kind:Mono", Vector("c-opaque-element"), v2), _assertion("a-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "ref-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", Vector("c-opaque-relationship"), v2), _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", Vector.empty, v2)).sortBy(_.hcursor.get[String]("assertionId").toOption.get); val enrichment = Vector(_assertion("z-opaque-element-enrichment", "element", "opaque-shared", "ref-opaque-element-enrichment", "enrichment:shared", Vector("c-opaque-element"), v2), _assertion("z-opaque-relationship-enrichment", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "enrichment:shared", Vector("c-opaque-relationship"), v2)).sortBy(_.hcursor.get[String]("assertionId").toOption.get); _canonical(Json.obj("canonicalAssertions" -> Json.fromValues(assertions), "conditions" -> Json.fromValues(Vector(_condition_json("c-opaque-element", "limitation", "element", "opaque-shared", "ref-opaque-element-enrichment", "element evidence remains bounded"), _condition_json("c-opaque-relationship", "limitation", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "relationship evidence remains bounded"))), "elements" -> Json.fromValues(Vector(_element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")), _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")), _element("opaque-shared", "Mono", "Opaque shared element", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element")))), "enrichmentAssertions" -> Json.fromValues(enrichment), "profile" -> Json.fromString(profile), "realizationIdentity" -> Json.fromString("realization-" + schema), "relationships" -> Json.fromValues(Vector(_relationship("opaque-shared", "StructuralDomain", "e-mono", "e-usecase", Vector("a-opaque-relationship-role-StructuralDomain"), Vector("z-opaque-relationship-enrichment"), Vector("c-opaque-relationship")), _relationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase", Vector("a-r-mono-domain-role-StructuralDomain")))), "schemaVersion" -> Json.fromString(schema), "scope" -> _scope_json, "sourceReferences" -> Json.fromValues(Vector(_reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"), _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"), _reference("ref-opaque-element-enrichment", "element", "opaque-shared", "anchor-opaque-element-enrichment"), _reference("ref-opaque-element-kind-Mono", "element", "opaque-shared", "anchor-opaque-element-kind-Mono"), _reference("ref-opaque-relationship-enrichment", "relationship", "opaque-shared", "anchor-opaque-relationship-enrichment"), _reference("ref-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain"), _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain")).sortBy(_.hcursor.get[String]("referenceId").toOption.get)), "successorLinks" -> Json.arr(), "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model"))))) }
  private def _binding(profile: String, schema: String): Array[Byte] = { def _record_(kind: String, identity: String, role: String, ids: Vector[String]): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(ids.map(Json.fromString)), "conditionIds" -> Json.arr(), "enrichmentAssertionIds" -> Json.arr(), "recordKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "sequenceAssertionId" -> Json.Null, "viewRole" -> Json.fromString(role)); val families = Vector("MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono")), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))), "UseCaseCommunicationProjection" -> Vector(_record_("element", "e-usecase", "use-case", Vector("a-e-usecase-kind-use-case"))), "EntityModelProjection" -> Vector.empty[Json], "EventModelProjection" -> Vector.empty[Json], "StructureViewProjection" -> Vector.empty[Json], "ClassificationViewProjection" -> Vector.empty[Json], "WorkflowProjection" -> Vector.empty[Json], "StateMachineProjection" -> Vector.empty[Json]); _canonical(Json.obj("profile" -> Json.fromString(profile), "realizationArtifactId" -> Json.fromString("realization-main"), "schemaVersion" -> Json.fromString(schema), "scope" -> _scope_json, "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) }))) }
  private def _semantic_diff(projection: InternalModelCandidateCmlProjection, candidatebytes: Array[Byte]): InternalModelSemanticDiff = { val targets = projection.targets.map { target => val mapping = target.mappings.head; val condition = _condition("available", "authorized", None, None, None, None, None, None, Vector("condition limitation")); val entry = CandidateDesignSemanticDiffEntry("entry-" + target.targetId, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.patchIdentity, projection.candidateModelIdentity, "category", "action", mapping.semanticIdentity, Some("before"), Some("after"), "relationship", _attribution("entry-authority-" + target.targetId), condition, Vector("entry limitation"), Some("entry tie")); val patch = CandidateDesignProposedCmlPatchTrace(target.patchIdentity, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.source.authority, target.source.locator.getOrElse("patch-locator-" + target.targetId), target.source.sha256, target.proposedContent.sha256, _attribution("patch-authority-" + target.targetId), condition, Vector("patch limitation"), Some("patch tie")); InternalModelSemanticDiffTarget(Vector(InternalModelSemanticDiffMappedEntry(entry, mapping.mappingId, mapping.semanticIdentityKind)), patch, target.targetId) }; InternalModelSemanticDiff("candidate-main", _sha256(candidatebytes), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision, "ccdm-semantic-diff-v1", "1.0", projection.scope, "semantic-diff-order", 3, targets, Vector.empty) }

  private def _binding_value(reviewedmanifest: Array[Byte], realization: Array[Byte], continuity: Array[Byte], candidate: Array[Byte], diff: Array[Byte], realizationidentity: String, reviewstate: String, allnullablefacets: Boolean): InternalModelCandidateReviewBinding = { val projection = _projection("ccdm-projection-binding-v1"); val semanticdiff = _semantic_diff(projection, candidate); val basis = _basis(); InternalModelCandidateReviewBinding(_review_artifact("candidate-main", candidate), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision, _review_artifact("projection-main", continuity), Vector(_review_artifact("evidence-alpha", "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8)), _review_artifact("evidence-beta", "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8))), "ccdm-candidate-review-binding-v1", basis.providers, _review_artifact("realization-main", realization), realizationidentity, "review-identity", 3, _content(reviewedmanifest), basis.rules, "1.0", projection.scope, _review_artifact("semantic-diff-main", diff), semanticdiff.semanticDiffIdentity, semanticdiff.semanticDiffRevision, Vector(_review_target("target-alpha", "patch-alpha", "evidence-alpha", reviewstate, allnullablefacets), _review_target("target-beta", "patch-beta", "evidence-beta", reviewstate, allnullablefacets)), Vector.empty) }
  private def _review_target(targetid: String, patchid: String, evidenceid: String, reviewstate: String, allnullablefacets: Boolean): InternalModelCandidateReviewTarget = { val nullable: (Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String]) = if allnullablefacets then (None, None, None, None, None, None, None) else (Some("redacted"), Some("explicit absence"), Some("ambiguous"), Some("conflicting"), Some("stale"), Some("malformed"), Some("tie-" + targetid)); InternalModelCandidateReviewTarget(Vector(evidenceid), CandidateDesignReviewSnapshot("snapshot-review-" + targetid, MonoKotoProjectionContextIdentity("context-order"), ComponentDashboardComponentIdentity("component-order"), patchid, "candidate-model-order", reviewstate, ComponentDashboardSourceAttribution("review-source", "review-authority", "review-locator"), ComponentDashboardCondition("available", "authorized", nullable._1, nullable._2, nullable._3, nullable._4, nullable._5, nullable._6, Vector("condition limitation", "condition limitation")), Vector("snapshot limitation", "snapshot limitation"), nullable._7), targetid) }
  private def _basis(): InternalModelCandidateReviewExecutionBasis = InternalModelCandidateReviewExecutionBasis(Vector(InternalModelCandidateReviewRule("rule-alpha", "1.0", _digest("rule-alpha")), InternalModelCandidateReviewRule("rule-beta", "2.0", _digest("rule-beta"))), Vector(InternalModelCandidateReviewProvider("provider-alpha", "1.0", _digest("provider-alpha")), InternalModelCandidateReviewProvider("provider-beta", "2.0", _digest("provider-beta"))))
  private def _review_artifact(id: String, bytes: Array[Byte]): InternalModelCandidateReviewArtifact = InternalModelCandidateReviewArtifact(id, _sha256(bytes))
  private def _entry(id: String, role: String, path: String, required: Boolean, bytes: Array[Byte], dependencies: Vector[String]): Json = Json.obj("artifactId" -> Json.fromString(id), "dependsOn" -> Json.fromValues(dependencies.sorted.map(Json.fromString)), "path" -> Json.fromString(path), "required" -> Json.fromBoolean(required), "role" -> Json.fromString(role), "sha256" -> Json.fromString(_sha256(bytes)))
  private def _topological(entries: Vector[Json]): Vector[Json] = { val byid = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value).toMap; val dependencies = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value.hcursor.get[Vector[String]]("dependsOn").toOption.get.toSet).toMap; @annotation.tailrec def _loop_(pending: Map[String, Set[String]], completed: Vector[String]): Vector[String] = if pending.isEmpty then completed else { val next = pending.collect { case (id, deps) if deps.isEmpty => id }.toVector.sorted.head; _loop_(pending.removed(next).view.mapValues(_ - next).toMap, completed :+ next) }; _loop_(dependencies, Vector.empty).map(byid) }
  private def _manifest(entries: Vector[Json], revision: Long): Array[Byte] = { val root = JsonObject.fromIterable(Vector("artifacts" -> Json.fromValues(entries), "lifecycleState" -> Json.fromString("draft"), "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)), "packageId" -> Json.fromString(_package_id), "projectId" -> Json.fromString("candidate-review-sample"), "projectNamespace" -> Json.fromString("org.example"), "revision" -> Json.fromLong(revision), "schemaVersion" -> Json.fromString("1.0"))); val digest = _sha256(_canonical(root.remove("packageDigest").toJson)); _canonical(root.add("packageDigest", Json.fromString(digest)).toJson) }
  private def _fact(kind: String, identity: String, anchor: String, content: String): Json = Json.obj("componentIdentity" -> Json.fromString("component-order"), "content" -> Json.fromString(content), "limitations" -> Json.arr(), "projectionContextIdentity" -> Json.fromString("context-order"), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceAnchor" -> Json.fromString(anchor))
  private def _assertion(id: String, kind: String, identity: String, referenceid: String, content: String, conditionids: Vector[String], v2: Boolean): Json = { val fields = Vector("assertionId" -> Json.fromString(id), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "content" -> Json.fromString(content), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid)) ++ Option.when(v2)("association" -> Json.Null); Json.obj(fields*) }
  private def _element(identity: String, kind: String, label: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "kind" -> Json.fromString(kind), "label" -> Json.fromString(label))
  private def _relationship(identity: String, role: String, source: String, target: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "direction" -> Json.fromString("source-to-target"), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "label" -> Json.fromString(identity), "role" -> Json.fromString(role), "sourceElementIdentity" -> Json.fromString(source), "targetElementIdentity" -> Json.fromString(target))
  private def _reference(id: String, kind: String, identity: String, anchor: String): Json = Json.obj("referenceId" -> Json.fromString(id), "snapshotArtifactId" -> Json.fromString("snapshot-model"), "source" -> _source_json(_model_source), "sourceAnchor" -> Json.fromString(anchor), "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind)))
  private def _attribution(authority: String): ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution("source-id", authority, "source-locator")
  private def _condition(availability: String, authorization: String, redaction: Option[String], absence: Option[String], ambiguity: Option[String], conflict: Option[String], staleness: Option[String], malformed: Option[String], limitations: Vector[String]): ComponentDashboardCondition = ComponentDashboardCondition(availability, authorization, redaction, absence, ambiguity, conflict, staleness, malformed, limitations)
  private def _condition_json(id: String, kind: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json = Json.obj("affectedIdentity" -> Json.fromString(affectedidentity), "affectedKind" -> Json.fromString(affectedkind), "conditionId" -> Json.fromString(id), "detail" -> Json.fromString(detail), "kind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid))
}
