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
/** Executable specification of closed portable candidate-review evidence. */
final class InternalModelCandidateReviewBindingValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private final case class FixtureOptions(
    bindingchange: InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding = identity,
    candidatebyteschange: Array[Byte] => Array[Byte] = identity,
    changedsource: Boolean = false,
    changedevidence: Boolean = false,
    changeddecision: Boolean = false,
    changedresume: Boolean = false,
    missingrequiredpath: Option[String] = None,
    extraartifact: Option[(String, String, String, Array[Byte])] = None,
    includeapproval: Boolean = true,
    includereview: Boolean = true,
    reviewrole: String = "validation",
    reviewid: String = "review-main",
    reviewpath: String = "reviews/candidate-review.json",
    reviewdependencies: Option[Vector[String]] = None,
    selectedreviewid: String = "review-main",
    includeoptionalreview: Boolean = false,
    reviewstate: String = "reviewed",
    allnullablefacets: Boolean = false,
    permuteencounter: Boolean = false,
    continuityprofile: String = "ccdm-projection-binding-v1",
    continuityschema: String = "1.0"
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

  "Candidate-review portable admission" should {
    "admit a real two-target historical package and a newer carrier with explicit review selection" in {
      Given("two private filesystem targets sharing one complete historical V1 manifest, two CML baselines, decision/resume records, and an absent optional inventory entry")
      var admission = Option.empty[InternalModelCandidateReviewBindingAdmission]
      var diagnostic = ""
      When("the selected validation review is admitted through the filesystem validator with an independently supplied rule/provider basis")
      _with_fixture() { root =>
        val result = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis())
        diagnostic = result.show
        admission = result.toOption
      }
      Then("historical and carrier identities remain distinct while every selected artifact, target, snapshot facet, and evidence binding is retained")
      withClue(diagnostic) {
        admission.map { value =>
          (
            value.reviewedPackageContext.revision,
            value.carrierPackageContext.revision,
            value.reviewedPackageContext.manifestBytes != value.carrierPackageContext.manifestBytes,
            value.reviewedPackageContext.artifacts.find(_.artifactId == "snapshot-optional").map(entry => (entry.required, entry.present)),
            value.binding.targets.map(target => (target.targetId, target.reviewSnapshot)),
            value.reviewedPackageContext.artifacts.filter(_.role == "validation").map(_.artifactId),
            value.reviewArtifactId,
            value.binding.canonicalBytes.nonEmpty
          )
        } shouldBe Some((
          1L,
          2L,
          true,
          Some((false, false)),
          _binding_value().targets.map(target => (target.targetId, target.reviewSnapshot)),
          Vector("evidence-alpha", "evidence-beta"),
          "review-main",
          true
        ))
      }
    }

    "admit an approved attributed snapshot with all nullable facets absent without producing an approval result" in {
      Given("a complete private two-target package whose review evidence uses approved state and null optional attribution facets")
      var admission = Option.empty[InternalModelCandidateReviewBindingAdmission]
      When("the approved attributed review is selected through the actual filesystem validator")
      _with_fixture(FixtureOptions(reviewstate = "approved", allnullablefacets = true)) { root =>
        admission = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).toOption
      }
      Then("approved remains attributable evidence, every nullable facet is preserved as absent, and no approval conclusion is emitted")
      admission.map { value =>
        (
          value.binding.targets.map(_.reviewSnapshot),
          new String(value.binding.canonicalBytes.toArray, StandardCharsets.UTF_8).contains("approval")
        )
      } shouldBe Some((
        _binding_value(reviewstate = "approved", allnullablefacets = true).targets.map(_.reviewSnapshot),
        false
      ))
    }

    "retain canonical codec evidence for opaque Unicode identities and independently sorted basis vectors" in {
      Given("generated opaque review identities and a real binding whose rules/providers are already in canonical UTF-8 order")
      forAll(Gen.nonEmptyListOf(Gen.oneOf(Gen.alphaNumChar.map(_.toString), Gen.const("漢"), Gen.const("😀"), Gen.const("é"))).map(_.mkString)) { generated =>
        val binding = _binding_value().copy(candidateIdentity = generated)

        When("portable review bytes are encoded and decoded without filesystem or provider state")
        val decoded = InternalModelCandidateReviewBindingCodec.decode(InternalModelCandidateReviewBindingCodec.encode(binding).toVector)

        Then("the opaque identity and complete basis survive without normalization")
        decoded.toOption.map(value => (value.candidateIdentity, value.rules, value.providers)) shouldBe Some((generated, binding.rules, binding.providers))
      }
    }

    "admit the same package after an order-independent fixture encounter while retaining canonical inventory order" in {
      Given("one complete private package whose artifact declarations are encountered in reverse dependency order")
      var admission = false
      var normalmanifest = ""
      var permutedmanifest = ""
      When("the fixture topological construction canonicalizes the reversed encounter and the actual validator admits it")
      _with_fixture() { root =>
        normalmanifest = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).toOption.map(_.binding.reviewedPackageManifest.rawBytesBase64).getOrElse("")
      }
      _with_fixture(FixtureOptions(permuteencounter = true)) { root =>
        val result = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).toOption
        admission = result.isDefined
        permutedmanifest = result.map(_.binding.reviewedPackageManifest.rawBytesBase64).getOrElse("")
      }
      Then("encounter order does not change the canonical reviewed manifest or admission result")
      admission shouldBe true
      permutedmanifest shouldBe normalmanifest
    }
  }

  "Candidate-review closed grammar" should {
    "reject root and nested missing, extra, duplicate, transport, layout, integer, digest, Base64, ordering, facet, and null-string variants" in {
      Given("one canonical real review artifact and independent mutations at every closed object boundary")
      val binding = _binding_value()
      val canonical = InternalModelCandidateReviewBindingCodec.encode(binding)
      val variants = Vector(
        "invalid-utf8" -> Array[Byte](0xc3.toByte, 0x28.toByte),
        "bom" -> (Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical),
        "leading-space" -> (" " + new String(canonical, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),
        "trailing-space" -> (canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8)),
        "missing-root" -> _root_mutation(binding)(_.remove("candidateIdentity")),
        "extra-root" -> _root_mutation(binding)(_.add("unexpected", Json.True)),
        "duplicate-root" -> _duplicate_member(canonical, "candidateIdentity"),
        "missing-artifact-member" -> _artifact_mutation(binding)(_.remove("sha256")),
        "extra-artifact-member" -> _artifact_mutation(binding)(_.add("unexpected", Json.True)),
        "duplicate-artifact-member" -> _duplicate_member(canonical, "artifactId"),
        "missing-evidence-artifact-member" -> _evidence_artifact_mutation(binding)(_.remove("sha256")),
        "extra-evidence-artifact-member" -> _evidence_artifact_mutation(binding)(_.add("unexpected", Json.True)),
        "missing-rule-member" -> _array_object_mutation(binding, "rules")(_.remove("ruleVersion")),
        "extra-rule-member" -> _array_object_mutation(binding, "rules")(_.add("unexpected", Json.True)),
        "duplicate-rule-member" -> _duplicate_member(canonical, "ruleId"),
        "missing-provider-member" -> _array_object_mutation(binding, "providers")(_.remove("providerVersion")),
        "extra-provider-member" -> _array_object_mutation(binding, "providers")(_.add("unexpected", Json.True)),
        "duplicate-provider-member" -> _duplicate_member(canonical, "providerId"),
        "duplicate-condition-member" -> _duplicate_member(canonical, "availability"),
        "malformed-provider-digest" -> _array_object_mutation(binding, "providers")(_.add("sha256", Json.fromString("sha256:0"))),
        "empty-rules" -> _root_mutation(binding)(_.add("rules", Json.arr())),
        "empty-providers" -> _root_mutation(binding)(_.add("providers", Json.arr())),
        "empty-evidence-artifacts" -> _root_mutation(binding)(_.add("evidenceArtifacts", Json.arr())),
        "extra-scope-member" -> _scope_mutation(binding)(_.add("unexpected", Json.True)),
        "missing-scope-member" -> _scope_mutation(binding)(_.remove("componentIdentity")),
        "wrong-scope-member-type" -> _scope_mutation(binding)(_.add("componentIdentity", Json.arr())),
        "missing-target-member" -> _target_mutation(binding)(_.remove("targetId")),
        "extra-target-member" -> _target_mutation(binding)(_.add("unexpected", Json.True)),
        "missing-target-evidence-member" -> _target_mutation(binding)(_.remove("evidenceArtifactIds")),
        "missing-target-snapshot-member" -> _target_mutation(binding)(_.remove("reviewSnapshot")),
        "duplicate-target-evidence" -> _target_mutation(binding)(target => target.add("evidenceArtifactIds", Json.arr(Json.fromString("evidence-alpha"), Json.fromString("evidence-alpha")))),
        "empty-target-evidence" -> _target_mutation(binding)(_.add("evidenceArtifactIds", Json.arr())),
        "blank-target-evidence" -> _target_mutation(binding)(target => target.add("evidenceArtifactIds", Json.arr(Json.fromString(" ")))),
        "wrong-target-evidence-order" -> _target_mutation(binding)(target => target.add("evidenceArtifactIds", Json.arr(Json.fromString("evidence-beta"), Json.fromString("evidence-alpha")))),
        "missing-snapshot-member" -> _snapshot_mutation(binding)(_.remove("patchId")),
        "extra-snapshot-member" -> _snapshot_mutation(binding)(_.add("unexpected", Json.True)),
        "missing-attribution-member" -> _snapshot_attribution_mutation(binding)(_.remove("sourceId")),
        "extra-attribution-member" -> _snapshot_attribution_mutation(binding)(_.add("unexpected", Json.True)),
        "missing-condition-member" -> _condition_mutation(binding)(_.remove("availability")),
        "blank-snapshot-tie" -> _snapshot_mutation(binding)(_.add("stableTieKey", Json.fromString(" "))),
        "blank-snapshot-limitation" -> _snapshot_mutation(binding)(_.add("limitations", Json.arr(Json.fromString(" ")))),
        "extra-condition-member" -> _condition_mutation(binding)(_.add("unexpected", Json.True)),
        "blank-facet" -> _condition_mutation(binding)(_.add("availability", Json.fromString(" "))),
        "blank-nullable-facet" -> _condition_mutation(binding)(_.add("ambiguity", Json.fromString(" "))),
        "wrong-nullable-facet-type" -> _condition_mutation(binding)(_.add("ambiguity", Json.fromInt(1))),
        "wrong-stable-tie-type" -> _snapshot_mutation(binding)(_.add("stableTieKey", Json.fromInt(1))),
        "wrong-profile" -> _root_mutation(binding)(_.add("profile", Json.fromString("ccdm-candidate-review-next"))),
        "wrong-schema" -> _root_mutation(binding)(_.add("schemaVersion", Json.fromString("2.0"))),
        "zero-revision" -> _root_mutation(binding)(_.add("candidateRevision", Json.fromInt(0))),
        "overflow-revision" -> _root_mutation(binding)(_.add("candidateRevision", Json.fromLong(Int.MaxValue.toLong + 1L))),
        "fractional-revision" -> _root_mutation(binding)(_.add("candidateRevision", Json.fromDoubleOrNull(7.0))),
        "bad-digest" -> _root_mutation(binding)(_.add("candidateArtifact", _artifact_json(InternalModelCandidateReviewArtifact("candidate-main", "SHA256:" + ("0" * 64))))),
        "bad-base64" -> _root_mutation(binding)(_.add("reviewedPackageManifest", _content_json(binding.reviewedPackageManifest.copy(rawBytesBase64 = "!")))),
        "missing-content-member" -> _content_mutation(binding)(_.remove("sha256")),
        "extra-content-member" -> _content_mutation(binding)(_.add("unexpected", Json.True)),
        "wrong-content-length-type" -> _content_mutation(binding)(_.add("byteLength", Json.fromString("1"))),
        "wrong-content-base64-type" -> _content_mutation(binding)(_.add("rawBytesBase64", Json.Null)),
        "blank-content-base64" -> _content_mutation(binding)(_.add("rawBytesBase64", Json.fromString(" "))),
        "wrong-content-digest-type" -> _content_mutation(binding)(_.add("sha256", Json.fromInt(1))),
        "content-length" -> _root_mutation(binding)(_.add("reviewedPackageManifest", _content_json(binding.reviewedPackageManifest.copy(byteLength = binding.reviewedPackageManifest.byteLength + 1L)))),
        "content-hash" -> _root_mutation(binding)(_.add("reviewedPackageManifest", _content_json(binding.reviewedPackageManifest.copy(sha256 = _digest("manifest-other"))))),
        "noncanonical-base64" -> _root_mutation(binding)(_.add("reviewedPackageManifest", _content_json(binding.reviewedPackageManifest.copy(rawBytesBase64 = binding.reviewedPackageManifest.rawBytesBase64.dropRight(1))))),
        "missing-terminal-newline" -> canonical.dropRight(1),
        "wrong-artifact-order" -> _root_mutation(binding)(root => root.add("evidenceArtifacts", Json.fromValues(root("evidenceArtifacts").flatMap(_.asArray).getOrElse(Vector.empty).reverse))),
        "wrong-rule-order" -> _root_mutation(binding)(root => root.add("rules", Json.fromValues(root("rules").flatMap(_.asArray).getOrElse(Vector.empty).reverse))),
        "wrong-target-order" -> _root_mutation(binding)(root => root.add("targets", Json.fromValues(root("targets").flatMap(_.asArray).getOrElse(Vector.empty).reverse))),
        "duplicate-target-object" -> _root_mutation(binding)(root => root.add("targets", Json.fromValues(root("targets").flatMap(_.asArray).getOrElse(Vector.empty).updated(1, root("targets").flatMap(_.asArray).getOrElse(Vector.empty).head)))),
        "duplicate-evidence-object" -> _root_mutation(binding)(root => root.add("evidenceArtifacts", Json.fromValues(root("evidenceArtifacts").flatMap(_.asArray).getOrElse(Vector.empty) :+ root("evidenceArtifacts").flatMap(_.asArray).getOrElse(Vector.empty).head))),
        "duplicate-snapshot-id" -> _root_mutation(binding) { root =>
          val targets = root("targets").flatMap(_.asArray).getOrElse(Vector.empty)
          val first = targets.head.asObject.get
          val second = targets(1).asObject.get.add("reviewSnapshot", first("reviewSnapshot").get)
          root.add("targets", Json.fromValues(Vector(targets.head, Json.fromJsonObject(second))))
        }
      ) ++ _root_missing_variants(binding)

      When("each named variant crosses the strict candidate-review codec")
      val results = variants.map { case (name, bytes) => name -> InternalModelCandidateReviewBindingCodec.decode(bytes.toVector).isLeft }

      Then("the canonical control is accepted and every independently malformed boundary fails closed")
      InternalModelCandidateReviewBindingCodec.decode(canonical.toVector).isRight shouldBe true
      results.forall(_._2) shouldBe true
    }
  }

  "Candidate-review package, basis, and carrier boundaries" should {
    "reject historical manifest, inventory, closure, required-presence, reviewed-source, decision/resume, evidence, and nonapproval-extra mutations after outer digests are recomputed" in {
      Given("a real valid carrier whose fixture-owned package manifest is regenerated for each independently changed file or historical claim")
      val historicalvariants = Vector(
        "historical-revision" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest)(_.add("revision", Json.fromInt(3)))))) ,
        "historical-project" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest)(_.add("projectId", Json.fromString("other-project")))))) ,
        "historical-schema" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest)(_.add("schemaVersion", Json.fromString("2.0")))))) ,
        "historical-package" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest)(_.add("packageId", Json.fromString("fedcba98-7654-3210-fedc-ba9876543210")))))) ,
        "historical-package-digest" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_package_digest_mismatch(value.reviewedPackageManifest)))),
        "historical-manifest-missing-field" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest)(_.remove("artifacts"))))),
        "historical-manifest-extra-field" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest)(_.add("unexpected", Json.True))))),
        "historical-manifest-duplicate-field" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_duplicate_member(value.reviewedPackageManifest, "packageId")))),
        "historical-missing-artifact" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest) { root => root.add("artifacts", Json.fromValues(root("artifacts").flatMap(_.asArray).get.filterNot(_.hcursor.get[String]("artifactId").toOption.contains("candidate-main")))) }))),
        "historical-extra-artifact" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation_topological(value.reviewedPackageManifest) { root => root.add("artifacts", Json.fromValues(root("artifacts").flatMap(_.asArray).get :+ _entry("historical-extra", "projection", "extras/historical-extra.json", true, "extra\n".getBytes(StandardCharsets.UTF_8), Vector.empty))) }))),
        "historical-duplicate-artifact" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest) { root => val artifacts = root("artifacts").flatMap(_.asArray).get; root.add("artifacts", Json.fromValues(artifacts :+ artifacts.head)) }))),
        "historical-reordered-artifact" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest) { root => root.add("artifacts", Json.fromValues(root("artifacts").flatMap(_.asArray).get.reverse)) }))),
        "historical-artifact-digest" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "evidence-alpha")(_.add("sha256", Json.fromString(_digest("historical-evidence"))))))),
        "historical-artifact-id" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "evidence-alpha")(_.add("artifactId", Json.fromString("evidence-other")))))),
        "historical-artifact-role" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "evidence-alpha")(_.add("role", Json.fromString("projection")))))),
        "historical-artifact-path" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "evidence-alpha")(_.add("path", Json.fromString("other/evidence.json")))))),
        "historical-artifact-required" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "evidence-alpha")(_.add("required", Json.False))))),
        "historical-artifact-dependency" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "candidate-main")(_.add("dependsOn", Json.arr(Json.fromString("unresolved-artifact"))))))),
        "historical-artifact-self-dependency" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "candidate-main")(_.add("dependsOn", Json.arr(Json.fromString("candidate-main"))))))),
        "historical-artifact-unsorted-dependency" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_artifact_mutation(value.reviewedPackageManifest, "candidate-main")(_.add("dependsOn", Json.arr(Json.fromString("snapshot-cml-beta"), Json.fromString("realization-main"), Json.fromString("projection-main"), Json.fromString("snapshot-cml-alpha"))))))),
        "historical-artifact-cycle" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation(value.reviewedPackageManifest) { root =>
          val artifacts = root("artifacts").flatMap(_.asArray).get.map { artifact =>
            val objectvalue = artifact.asObject.get
            val id = objectvalue("artifactId").flatMap(_.asString).getOrElse("")
            if id == "candidate-main" then Json.fromJsonObject(objectvalue.add("dependsOn", Json.arr(Json.fromString("semantic-diff-main"))))
            else if id == "semantic-diff-main" then Json.fromJsonObject(objectvalue.add("dependsOn", Json.arr(Json.fromString("candidate-main"))))
            else Json.fromJsonObject(objectvalue)
          }
          root.add("artifacts", Json.fromValues(artifacts))
        }))),
        "historical-review-self-id" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation_topological(value.reviewedPackageManifest) { root => root.add("artifacts", Json.fromValues(root("artifacts").flatMap(_.asArray).get :+ _entry("review-main", "validation", "reviews/historical-review.json", true, "review\n".getBytes(StandardCharsets.UTF_8), Vector.empty))) }))),
        "historical-review-self-path" -> ((value: InternalModelCandidateReviewBinding) => value.copy(reviewedPackageManifest = _content(_manifest_mutation_topological(value.reviewedPackageManifest) { root => root.add("artifacts", Json.fromValues(root("artifacts").flatMap(_.asArray).get :+ _entry("review-historical", "validation", "reviews/candidate-review.json", true, "review\n".getBytes(StandardCharsets.UTF_8), Vector.empty))) })))
      )
      var historicalresults = Vector.empty[(String, Boolean)]
      var changedsource = false
      val changedcandidatebytes = InternalModelCandidateCmlProjectionCodec.encode(_projection("ccdm-projection-binding-v1").copy(candidateIdentity = "candidate-semantic-other"))
      var changedsemantic = false
      var changeddecision = false
      var changedresume = false
      var changedevidencefile = false
      var changedevidence = false
      var missingrequired = false
      var missingevidencepath = false
      var nonapprovalextra = false
      var missingreviewdependency = false
      var extrareviewdependency = false
      var absentoptionalreviewdependency = false

      When("the real validator compares each changed historical or carrier boundary")
      historicalresults = historicalvariants.map { case (name, change) => name -> _validate(change) }
      changedsource = _validate(identity, FixtureOptions(changedsource = true))
      changedsemantic = _validate(value => _rebind_candidate_manifest(value, changedcandidatebytes), FixtureOptions(candidatebyteschange = _ => changedcandidatebytes))
      changeddecision = _validate(identity, FixtureOptions(changeddecision = true))
      changedresume = _validate(identity, FixtureOptions(changedresume = true))
      changedevidencefile = _validate(identity, FixtureOptions(changedevidence = true))
      changedevidence = _validate(value => value.copy(evidenceArtifacts = value.evidenceArtifacts.updated(0, value.evidenceArtifacts.head.copy(sha256 = _digest("evidence-changed")))))
      missingrequired = _validate(identity, FixtureOptions(missingrequiredpath = Some("snapshots/cml-alpha.json")))
      missingevidencepath = _validate(identity, FixtureOptions(missingrequiredpath = Some("evidence/alpha.json")))
      nonapprovalextra = _validate(identity, FixtureOptions(extraartifact = Some(("new-decision", "decision", "extras/new-decision.json", "new input\n".getBytes(StandardCharsets.UTF_8)))))
      missingreviewdependency = _validate(identity, FixtureOptions(reviewdependencies = Some(_review_dependencies().dropRight(1))))
      extrareviewdependency = _validate(identity, FixtureOptions(reviewdependencies = Some(_review_dependencies() :+ "approval-extra")))
      absentoptionalreviewdependency = _validate(identity, FixtureOptions(reviewdependencies = Some(_review_dependencies() :+ "snapshot-optional")))
      Then("every boundary rejects without being masked by a stale outer package digest")
      historicalresults.forall(_._2 == false) shouldBe true
      Vector(changedsource, changedsemantic, changeddecision, changedresume, changedevidencefile, changedevidence, missingrequired, missingevidencepath, nonapprovalextra, missingreviewdependency, extrareviewdependency, absentoptionalreviewdependency).forall(_ == false) shouldBe true
    }

    "accept only explicit review selection and structurally valid inert approval extras while keeping the review state as evidence" in {
      Given("one carrier with an explicit validation review and one inert approval-role carrier extra")
      var admission = Option.empty[InternalModelCandidateReviewBindingAdmission]
      When("the caller selects review-main by exact ID")
      _with_fixture() { root =>
        admission = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).toOption
      }
      Then("the selected review is admitted as evidence and no approval field or approval conclusion is produced")
      admission.isDefined shouldBe true
      new String(admission.get.binding.canonicalBytes.toArray, StandardCharsets.UTF_8) should not include "approval"
      admission.get.binding.targets.map(_.reviewSnapshot.state) shouldBe Vector("reviewed", "reviewed")
    }

    "accept the selected review when the optional approval carrier file is absent" in {
      Given("one complete private package that declares no approval-role carrier extra")
      var admission = false
      When("the selected review is validated without creating an approval-extra path")
      _with_fixture(FixtureOptions(includeapproval = false)) { root =>
        admission = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).isSuccess
      }
      Then("approval absence is not converted into an unlisted filesystem artifact and review admission remains successful")
      admission shouldBe true
    }

    "reject missing, absent, wrong-role, and wrong-profile explicit selections" in {
      Given("otherwise identical private packages with one independently selected review failure at a time")
      var results = Vector.empty[Boolean]

      When("the real package validator receives each non-admissible selection")
      val missing = _validate(identity, FixtureOptions(includereview = false))
      val absent = _validate(identity, FixtureOptions(selectedreviewid = "review-absent"))
      val optionalabsent = _validate(identity, FixtureOptions(includereview = false, includeoptionalreview = true, selectedreviewid = "review-optional-absent"))
      val wrongrole = _validate(identity, FixtureOptions(reviewrole = "decision"))
      val wrongprofile = _validate(value => value.copy(profile = "ccdm-candidate-review-next"))
      results = Vector(missing, absent, optionalabsent, wrongrole, wrongprofile)
      Then("no absent artifact, nonvalidation role, or unsupported profile is interpreted as an explicit review")
      results shouldBe Vector(false, false, false, false, false)
    }
  }

  "Candidate-review selected binding matrix" should {
    "admit two targets that share one evidence artifact when the global evidence union remains complete" in {
      Given("a real two-target binding whose target-local nonempty sorted evidence IDs share evidence-alpha and whose global union names that one artifact")
      val sharedchange = (value: InternalModelCandidateReviewBinding) => value.copy(
        evidenceArtifacts = Vector(_review_artifact("evidence-alpha", "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8))),
        targets = value.targets.map(_.copy(evidenceArtifactIds = Vector("evidence-alpha")))
      )
      val shared = sharedchange(_binding_value())
      var admission = false

      When("the shared-evidence binding is decoded and admitted through the actual package validator")
      val decoded = InternalModelCandidateReviewBindingCodec.decode(InternalModelCandidateReviewBindingCodec.encode(shared).toVector)
      _with_fixture(FixtureOptions(bindingchange = sharedchange)) { root =>
        admission = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).isSuccess
      }

      Then("the canonical codec and filesystem admission preserve the original union contract without requiring one occurrence per target")
      decoded.isRight shouldBe true
      admission shouldBe true
    }

    "reject every selected artifact, identity, revision, scope, target, snapshot, and evidence mismatch independently" in {
      Given("a valid candidate-review package and one-field binding changes whose carrier manifest is always recomputed")
      val changes: Vector[(String, InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding)] = Vector(
        "candidate-id" -> (value => value.copy(candidateArtifact = value.candidateArtifact.copy(artifactId = "candidate-other"))),
        "candidate-hash" -> (value => value.copy(candidateArtifact = value.candidateArtifact.copy(sha256 = _digest("candidate-other")))),
        "continuity-id" -> (value => value.copy(continuityArtifact = value.continuityArtifact.copy(artifactId = "projection-other"))),
        "continuity-hash" -> (value => value.copy(continuityArtifact = value.continuityArtifact.copy(sha256 = _digest("projection-other")))),
        "realization-id" -> (value => value.copy(realizationArtifact = value.realizationArtifact.copy(artifactId = "realization-other"))),
        "realization-hash" -> (value => value.copy(realizationArtifact = value.realizationArtifact.copy(sha256 = _digest("realization-other")))),
        "diff-id" -> (value => value.copy(semanticDiffArtifact = value.semanticDiffArtifact.copy(artifactId = "semantic-diff-other"))),
        "diff-hash" -> (value => value.copy(semanticDiffArtifact = value.semanticDiffArtifact.copy(sha256 = _digest("semantic-diff-other")))),
        "realization-identity" -> (value => value.copy(realizationIdentity = "realization-other")),
        "candidate-identity" -> (value => value.copy(candidateIdentity = "candidate-other")),
        "candidate-model" -> (value => value.copy(candidateModelIdentity = "candidate-model-other")),
        "candidate-revision" -> (value => value.copy(candidateRevision = 8)),
        "diff-identity" -> (value => value.copy(semanticDiffIdentity = "semantic-diff-other")),
        "diff-revision" -> (value => value.copy(semanticDiffRevision = 8)),
        "scope-component" -> (value => value.copy(scope = value.scope.copy(componentIdentity = "component-other"))),
        "scope-context" -> (value => value.copy(scope = value.scope.copy(projectionContextIdentity = "context-other"))),
        "scope-usecase" -> (value => value.copy(scope = value.scope.copy(selectedUseCaseElementIdentity = "usecase-other"))),
        "target-missing" -> (value => value.copy(targets = value.targets.take(1))),
        "target-extra" -> (value => value.copy(targets = value.targets :+ value.targets.head.copy(targetId = "target-gamma"))),
        "target-order" -> (value => value.copy(targets = value.targets.reverse)),
        "snapshot-patch" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(reviewSnapshot = value.targets.head.reviewSnapshot.copy(patchId = "patch-other"))))),
        "snapshot-model" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(reviewSnapshot = value.targets.head.reviewSnapshot.copy(candidateModelId = "candidate-model-other"))))),
        "snapshot-component" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(reviewSnapshot = value.targets.head.reviewSnapshot.copy(component = ComponentDashboardComponentIdentity("component-other")))))),
        "snapshot-context" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(reviewSnapshot = value.targets.head.reviewSnapshot.copy(context = MonoKotoProjectionContextIdentity("context-other")))))),
        "snapshot-duplicate-id" -> (value => value.copy(targets = value.targets.updated(1, value.targets(1).copy(reviewSnapshot = value.targets.head.reviewSnapshot.copy(id = value.targets.head.reviewSnapshot.id))))),
        "evidence-id" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts.updated(0, value.evidenceArtifacts.head.copy(artifactId = "evidence-other")))),
        "evidence-hash" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts.updated(0, value.evidenceArtifacts.head.copy(sha256 = _digest("evidence-other"))))),
        "evidence-missing" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts.dropRight(1))),
        "evidence-role" -> (value => value.copy(evidenceArtifacts = Vector(InternalModelCandidateReviewArtifact("candidate-main", value.candidateArtifact.sha256)), targets = value.targets.map(_.copy(evidenceArtifactIds = Vector("candidate-main"))))),
        "evidence-dangling" -> (value => value.copy(evidenceArtifacts = Vector(_review_artifact("evidence-dangling", _cml_alpha_raw.toArray)), targets = value.targets.map(_.copy(evidenceArtifactIds = Vector("evidence-dangling"))))),
        "evidence-unused" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts :+ _review_artifact("evidence-unused", _cml_alpha_raw.toArray))),
        "target-local-duplicate-evidence" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(evidenceArtifactIds = Vector("evidence-alpha", "evidence-alpha"))))),
        "evidence-coverage" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(evidenceArtifactIds = Vector("evidence-beta")))))
      )

      When("each mutation is encoded into the selected review artifact and admitted through validate")
      val results = changes.map { case (name, change) => name -> _validate(change) }

      Then("the truthful control is the only admission and every selected mismatch rejects independently")
      results.forall(_._2 == false) shouldBe true
    }

    "reject every rule/provider ID, version, hash, missing, duplicate, order, and malformed expected-basis mutation" in {
      Given("a valid package plus independently malformed caller-admitted execution bases")
      val valid = _basis()
      val cases = Vector(
        "missing-rules" -> valid.copy(rules = Vector.empty),
        "missing-providers" -> valid.copy(providers = Vector.empty),
        "rule-id" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleId = "rule-other"))),
        "second-rule-id" -> valid.copy(rules = valid.rules.updated(1, valid.rules(1).copy(ruleId = "rule-other"))),
        "rule-version" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleVersion = "2.0"))),
        "second-rule-version" -> valid.copy(rules = valid.rules.updated(1, valid.rules(1).copy(ruleVersion = "3.0"))),
        "rule-hash" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(sha256 = _digest("rule-other")))),
        "second-rule-hash" -> valid.copy(rules = valid.rules.updated(1, valid.rules(1).copy(sha256 = _digest("second-rule-other")))),
        "provider-id" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(providerId = "provider-other"))),
        "second-provider-id" -> valid.copy(providers = valid.providers.updated(1, valid.providers(1).copy(providerId = "provider-other"))),
        "provider-version" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(providerVersion = "2.0"))),
        "second-provider-version" -> valid.copy(providers = valid.providers.updated(1, valid.providers(1).copy(providerVersion = "3.0"))),
        "provider-hash" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(sha256 = _digest("provider-other")))),
        "second-provider-hash" -> valid.copy(providers = valid.providers.updated(1, valid.providers(1).copy(sha256 = _digest("second-provider-other")))),
        "duplicate-rule" -> valid.copy(rules = Vector(valid.rules.head, valid.rules.head)),
        "duplicate-provider" -> valid.copy(providers = Vector(valid.providers.head, valid.providers.head)),
        "unordered-rules" -> valid.copy(rules = valid.rules.reverse),
        "unordered-provider" -> valid.copy(providers = valid.providers.reverse),
        "malformed-rule" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleId = " "))),
        "blank-rule-version" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleVersion = " "))),
        "blank-provider-id" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(providerId = " "))),
        "malformed-rule-digest" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(sha256 = "sha256:0"))),
        "malformed-provider-digest" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(sha256 = "sha256:0")))
      )

      When("the actual package is validated against each expected basis")
      val results = cases.map { case (name, basis) => name -> _validate(identity, FixtureOptions(), basis) }

      Then("only the complete exact ordered basis can be admitted")
      results.forall(_._2 == false) shouldBe true
    }
  }

  "Candidate-review captured handoff and predecessor compatibility" should {
    "admit V1 and V2 predecessor realization/continuity families with the selected semantic diff" in {
      Given("portable predecessor fixtures carrying the same candidate and semantic-diff closure")
      val families = Vector(("ccdm-projection-binding-v1", "1.0"), ("ccdm-projection-binding-v2", "2.0"))

      When("each predecessor family is selected through the complete filesystem validator")
      val results = families.map { case (profile, schema) =>
        var review = false
        var predecessor = false
        _with_fixture(FixtureOptions(continuityprofile = profile, continuityschema = schema)) { root =>
          review = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).isSuccess
          predecessor = InternalModelPackageValidator.verifiedProjectionContinuity(root).isSuccess
        }
        (review, predecessor)
      }

      Then("both unchanged predecessor contracts remain compatible with candidate-review admission")
      results shouldBe Vector((true, true), (true, true))
    }

    "validate a captured package after every fixture-owned path changes or disappears, while fresh validation fails" in {
      Given("one verified candidate-review package whose manifest, source, projection, review, and project paths are private to this behavior")
      var captured = Option.empty[InternalModelCandidateReviewBindingAdmission]
      var fresh = true

      When("the package is captured once, every fixture path is removed, and only validateVerified receives the retained handoff")
      _with_fixture() { root =>
        val handoff = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, "review-main")
        val stream = Files.walk(root)
        val packagepaths = try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse finally stream.close()
        packagepaths.filterNot(_ == root).foreach { path => Files.deleteIfExists(path) }
        captured = handoff.flatMap(value => InternalModelCandidateReviewBindingValidator.validateVerified(value, _basis())).toOption
        fresh = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).isSuccess
      }

      Then("the pure retained admission preserves both target bytes while a new path-based read cannot reopen the deleted fixture")
      captured.map(value => (value.binding.targets.map(_.targetId), value.semanticDiffAdmission.candidateAdmission.targetBytes.map(_.proposedRawBytes), value.binding.canonicalBytes.nonEmpty)) shouldBe
        Some((Vector("target-alpha", "target-beta"), Vector(Vector[Byte](0, -1, 10), _cml_beta_raw), true))
      fresh shouldBe false
    }

    "admit an independently copied package at a separate filesystem root" in {
      Given("one complete private package and a byte-for-byte directory relocation created by the test itself")
      var original = Option.empty[(InternalModelVerifiedPackageContext, String, String, String, Vector[Byte])]
      var relocated = Option.empty[(InternalModelVerifiedPackageContext, String, String, String, Vector[Byte])]

      When("the copied root is validated with the same explicit review and execution basis")
      _with_fixture() { root =>
        original = InternalModelCandidateReviewBindingValidator.validate(root, "review-main", _basis()).toOption.map { value =>
          (value.reviewedPackageContext, value.reviewArtifactId, value.reviewArtifactSha256, value.reviewArtifactPackageRelativePath, value.binding.canonicalBytes)
        }
        val copy = Files.createTempDirectory(Path.of("target"), "internal-model-candidate-review-copy-")
        try {
          _copy_tree(root, copy)
          relocated = InternalModelCandidateReviewBindingValidator.validate(copy, "review-main", _basis()).toOption.map { value =>
            (value.reviewedPackageContext, value.reviewArtifactId, value.reviewArtifactSha256, value.reviewArtifactPackageRelativePath, value.binding.canonicalBytes)
          }
        } finally _delete_tree(copy)
      }

      Then("relocation preserves the exact reviewed identity, raw manifest, digest, selected record, and canonical bytes without retained path, CBD, or provider state")
      original.isDefined shouldBe true
      relocated shouldBe original
    }
  }

  private def _validate(
    change: InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding,
    options: FixtureOptions = FixtureOptions(),
    basis: InternalModelCandidateReviewExecutionBasis = _basis()
  ): Boolean = {
    var succeeded = false
    val configured = options.copy(bindingchange = change)
    _with_fixture(configured) { root =>
      succeeded = InternalModelCandidateReviewBindingValidator.validate(root, configured.selectedreviewid, basis).isSuccess
    }
    succeeded
  }

  private def _with_fixture(options: FixtureOptions = FixtureOptions())(action: Path => Unit): Unit = {
    val projection = _projection(options.continuityprofile)
    val realizationprofile = if options.continuityprofile == "ccdm-projection-binding-v2" then "ccdm-realization-v2" else "ccdm-realization-v1"
    val realization = _realization(realizationprofile)
    val continuity = _binding(options.continuityprofile, options.continuityschema)
    val candidate = InternalModelCandidateCmlProjectionCodec.encode(projection)
    val candidatecarrier = options.candidatebyteschange(candidate)
    val diff = InternalModelSemanticDiffCodec.encode(_semantic_diff(projection, candidate))
    val model = _model_snapshot()
    val alpha = _cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw)
    val beta = _cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw)
    val sourcebytes = if options.changedsource then alpha.map(byte => (byte ^ 1).toByte) else alpha
    val decisionbytes = if options.changeddecision then "changed decision\n".getBytes(StandardCharsets.UTF_8) else "decision record\n".getBytes(StandardCharsets.UTF_8)
    val resumebytes = if options.changedresume then "changed resume\n".getBytes(StandardCharsets.UTF_8) else "resume record\n".getBytes(StandardCharsets.UTF_8)
    val evidencealpha = "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8)
    val evidencebeta = "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8)
    val evidencealphacarrier = if options.changedevidence then evidencealpha.map(byte => (byte ^ 1).toByte) else evidencealpha
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
    val encountered = if options.permuteencounter then historicalentries.reverse else historicalentries
    val reviewedmanifest = _manifest(_topological(encountered), 1)
    val basebinding = _binding_value(reviewedmanifest, realization, continuity, candidate, diff, if options.continuityprofile == "ccdm-projection-binding-v2" then "realization-2.0" else "realization-1.0", options.reviewstate, options.allnullablefacets)
    val binding = options.bindingchange(basebinding)
    val reviewbytes = InternalModelCandidateReviewBindingCodec.encode(binding)
    val carrierentries = _topological(historicalentries.map { value =>
      val id = value.hcursor.get[String]("artifactId").toOption.get
      if id == "snapshot-cml-alpha" then _entry(id, "source-snapshot", "snapshots/cml-alpha.json", true, sourcebytes, Vector.empty)
      else if id == "evidence-alpha" then _entry(id, "validation", "evidence/alpha.json", true, evidencealphacarrier, Vector.empty)
      else if id == "decision-main" then _entry(id, "decision", "decisions/main.json", true, decisionbytes, Vector("realization-main"))
      else if id == "resume-main" then _entry(id, "resume", "resumes/main.json", true, resumebytes, Vector("realization-main"))
      else if id == "candidate-main" then _entry(id, "projection", "projections/candidate.json", true, candidatecarrier, Vector("projection-main", "realization-main", "snapshot-cml-alpha", "snapshot-cml-beta"))
      else value
    }) ++ Option.when(options.includereview)(_entry(options.reviewid, options.reviewrole, options.reviewpath, true, reviewbytes, options.reviewdependencies.getOrElse(historicalentries.filter(_.hcursor.get[Boolean]("required").toOption.get).map(_.hcursor.get[String]("artifactId").toOption.get).sorted))).toVector ++ Option.when(options.includeoptionalreview)(_entry("review-optional-absent", "validation", "reviews/optional-review.json", false, reviewbytes, Vector.empty)).toVector ++ Option.when(options.includeapproval)(_entry("approval-extra", "approval", "carrier/approval-extra.json", true, "inert carrier metadata\n".getBytes(StandardCharsets.UTF_8), Vector.empty)).toVector ++ options.extraartifact.map { case (id, role, path, bytes) => _entry(id, role, path, true, bytes, Vector.empty) }.toVector
    val carrierbytes = _manifest(_topological(carrierentries), 2)
    val root = _temporary_root()
    try {
      _write(root.resolve("project.yaml"), _project_yaml.getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), carrierbytes)
      _write(root.resolve("src/main/internal-model/evidence/alpha.json"), evidencealphacarrier)
      _write(root.resolve("src/main/internal-model/evidence/beta.json"), evidencebeta)
      _write(root.resolve("src/main/internal-model/snapshots/cml-alpha.json"), sourcebytes)
      _write(root.resolve("src/main/internal-model/snapshots/cml-beta.json"), beta)
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), model)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/decisions/main.json"), decisionbytes)
      _write(root.resolve("src/main/internal-model/projections/continuity.json"), continuity)
      _write(root.resolve("src/main/internal-model/projections/candidate.json"), candidatecarrier)
      _write(root.resolve("src/main/internal-model/projections/semantic-diff.json"), diff)
      _write(root.resolve("src/main/internal-model/resumes/main.json"), resumebytes)
      if options.includeapproval then _write(root.resolve("src/main/internal-model/carrier/approval-extra.json"), "inert carrier metadata\n".getBytes(StandardCharsets.UTF_8))
      options.extraartifact.foreach { case (_, _, path, bytes) => _write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      if options.includereview then _write(root.resolve("src/main/internal-model").resolve(options.reviewpath), reviewbytes)
      options.missingrequiredpath.foreach(path => Files.deleteIfExists(root.resolve("src/main/internal-model").resolve(path)))
      action(root)
    } finally _delete_tree(root)
  }

  private def _binding_value(
    reviewedmanifest: Array[Byte] = _manifest(Vector.empty, 1),
    realization: Array[Byte] = _realization("ccdm-realization-v1"),
    continuity: Array[Byte] = _binding("ccdm-projection-binding-v1", "1.0"),
    candidate: Array[Byte] = InternalModelCandidateCmlProjectionCodec.encode(_projection("ccdm-projection-binding-v1")),
    diff: Array[Byte] = InternalModelSemanticDiffCodec.encode(_semantic_diff(_projection("ccdm-projection-binding-v1"), InternalModelCandidateCmlProjectionCodec.encode(_projection("ccdm-projection-binding-v1")))),
    realizationidentity: String = "realization-1.0",
    reviewstate: String = "reviewed",
    allnullablefacets: Boolean = false
  ): InternalModelCandidateReviewBinding = {
    val projection = _projection("ccdm-projection-binding-v1")
    val semanticdiff = _semantic_diff(projection, candidate)
    val basis = _basis()
    InternalModelCandidateReviewBinding(
      _review_artifact("candidate-main", candidate), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision,
      _review_artifact("projection-main", continuity),
      Vector(_review_artifact("evidence-alpha", "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8)), _review_artifact("evidence-beta", "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8))),
      "ccdm-candidate-review-binding-v1", basis.providers, _review_artifact("realization-main", realization), realizationidentity, "review-identity", 3,
      _content(reviewedmanifest), basis.rules, "1.0", projection.scope, _review_artifact("semantic-diff-main", diff), semanticdiff.semanticDiffIdentity, semanticdiff.semanticDiffRevision,
      Vector(_review_target("target-alpha", "patch-alpha", "evidence-alpha", reviewstate, allnullablefacets), _review_target("target-beta", "patch-beta", "evidence-beta", reviewstate, allnullablefacets)), Vector.empty
    )
  }

  private def _review_target(targetid: String, patchid: String, evidenceid: String, reviewstate: String = "reviewed", allnullablefacets: Boolean = false): InternalModelCandidateReviewTarget = {
    val nullable: (Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String]) = if allnullablefacets then (None, None, None, None, None, None, None) else (Some("redacted"), Some("explicit absence"), Some("ambiguous"), Some("conflicting"), Some("stale"), Some("malformed"), Some("tie-" + targetid))
    InternalModelCandidateReviewTarget(
      Vector(evidenceid),
      CandidateDesignReviewSnapshot(
        "snapshot-review-" + targetid,
        MonoKotoProjectionContextIdentity("context-order"),
        ComponentDashboardComponentIdentity("component-order"),
        patchid,
        "candidate-model-order",
        reviewstate,
        ComponentDashboardSourceAttribution("review-source", "review-authority", "review-locator"),
        ComponentDashboardCondition("available", "authorized", nullable._1, nullable._2, nullable._3, nullable._4, nullable._5, nullable._6, Vector("condition limitation", "condition limitation")),
        Vector("snapshot limitation", "snapshot limitation"),
        nullable._7
      ),
      targetid
    )
  }

  private def _basis(): InternalModelCandidateReviewExecutionBasis = InternalModelCandidateReviewExecutionBasis(Vector(InternalModelCandidateReviewRule("rule-alpha", "1.0", _digest("rule-alpha")), InternalModelCandidateReviewRule("rule-beta", "2.0", _digest("rule-beta"))), Vector(InternalModelCandidateReviewProvider("provider-alpha", "1.0", _digest("provider-alpha")), InternalModelCandidateReviewProvider("provider-beta", "2.0", _digest("provider-beta"))))
  private def _review_dependencies(): Vector[String] = Vector("candidate-main", "decision-main", "evidence-alpha", "evidence-beta", "projection-main", "realization-main", "resume-main", "semantic-diff-main", "snapshot-cml-alpha", "snapshot-cml-beta", "snapshot-model")
  private def _review_artifact(id: String, bytes: Array[Byte]): InternalModelCandidateReviewArtifact = InternalModelCandidateReviewArtifact(id, _sha256(bytes))
  private def _entry(id: String, role: String, path: String, required: Boolean, bytes: Array[Byte], dependencies: Vector[String]): Json = Json.obj("artifactId" -> Json.fromString(id), "dependsOn" -> Json.fromValues(dependencies.sorted.map(Json.fromString)), "path" -> Json.fromString(path), "required" -> Json.fromBoolean(required), "role" -> Json.fromString(role), "sha256" -> Json.fromString(_sha256(bytes)))

  private def _topological(entries: Vector[Json]): Vector[Json] = {
    val byid = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value).toMap
    val dependencies = entries.map(value => value.hcursor.get[String]("artifactId").toOption.get -> value.hcursor.get[Vector[String]]("dependsOn").toOption.get.toSet).toMap
    @annotation.tailrec
    def _loop_(pending: Map[String, Set[String]], completed: Vector[String]): Vector[String] =
      if pending.isEmpty then completed
      else {
        val next = pending.collect { case (id, deps) if deps.isEmpty => id }.toVector.sorted.head
        _loop_(pending.removed(next).view.mapValues(_ - next).toMap, completed :+ next)
      }
    _loop_(dependencies, Vector.empty).map(byid)
  }

  private def _manifest(entries: Vector[Json], revision: Long): Array[Byte] = {
    val root = JsonObject.fromIterable(Vector("artifacts" -> Json.fromValues(entries), "lifecycleState" -> Json.fromString("draft"), "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)), "packageId" -> Json.fromString(_package_id), "projectId" -> Json.fromString("candidate-review-sample"), "projectNamespace" -> Json.fromString("org.example"), "revision" -> Json.fromLong(revision), "schemaVersion" -> Json.fromString("1.0")))
    val digest = _sha256(_canonical(root.remove("packageDigest").toJson))
    _canonical(root.add("packageDigest", Json.fromString(digest)).toJson)
  }

  private def _manifest_mutation(content: InternalModelCandidateCmlContent)(change: JsonObject => JsonObject): Array[Byte] = {
    val json = io.circe.parser.parse(new String(Base64.getDecoder.decode(content.rawBytesBase64), StandardCharsets.UTF_8)).toOption.get
    val changed = change(json.asObject.get).remove("packageDigest")
    _canonical(changed.add("packageDigest", Json.fromString(_sha256(_canonical(changed.toJson)))).toJson)
  }

  private def _manifest_mutation_topological(content: InternalModelCandidateCmlContent)(change: JsonObject => JsonObject): Array[Byte] = {
    val json = io.circe.parser.parse(new String(Base64.getDecoder.decode(content.rawBytesBase64), StandardCharsets.UTF_8)).toOption.get
    val changed = change(json.asObject.get).remove("packageDigest")
    val ordered = _topological(changed("artifacts").flatMap(_.asArray).get.toVector)
    val canonical = changed.add("artifacts", Json.fromValues(ordered))
    _canonical(canonical.add("packageDigest", Json.fromString(_sha256(_canonical(canonical.toJson)))).toJson)
  }

  private def _manifest_artifact_mutation(content: InternalModelCandidateCmlContent, artifactid: String)(change: JsonObject => JsonObject): Array[Byte] =
    _manifest_mutation(content) { root =>
      val artifacts = root("artifacts").flatMap(_.asArray).get.map { artifact =>
        val objectvalue = artifact.asObject.get
        if objectvalue("artifactId").flatMap(_.asString).contains(artifactid) then Json.fromJsonObject(change(objectvalue)) else Json.fromJsonObject(objectvalue)
      }
      root.add("artifacts", Json.fromValues(artifacts))
    }

  private def _manifest_package_digest_mismatch(content: InternalModelCandidateCmlContent): Array[Byte] = {
    val json = io.circe.parser.parse(new String(Base64.getDecoder.decode(content.rawBytesBase64), StandardCharsets.UTF_8)).toOption.get
    _canonical(json.asObject.get.add("packageDigest", Json.fromString(_digest("wrong-package-digest"))).toJson)
  }

  private def _manifest_duplicate_member(content: InternalModelCandidateCmlContent, member: String): Array[Byte] =
    _duplicate_member(Base64.getDecoder.decode(content.rawBytesBase64), member)

  private def _rebind_candidate_manifest(value: InternalModelCandidateReviewBinding, candidatebytes: Array[Byte]): InternalModelCandidateReviewBinding = {
    val changed = _manifest_mutation(value.reviewedPackageManifest) { root =>
      val artifacts = root("artifacts").flatMap(_.asArray).get.map { artifact =>
        val objectvalue = artifact.asObject.get
        if objectvalue("artifactId").flatMap(_.asString).contains("candidate-main") then Json.fromJsonObject(objectvalue.add("sha256", Json.fromString(_sha256(candidatebytes)))) else Json.fromJsonObject(objectvalue)
      }
      root.add("artifacts", Json.fromValues(artifacts))
    }
    value.copy(reviewedPackageManifest = _content(changed), candidateArtifact = value.candidateArtifact.copy(sha256 = _sha256(candidatebytes)))
  }

  private def _content(bytes: Array[Byte]): InternalModelCandidateCmlContent = InternalModelCandidateCmlContent(bytes.length.toLong, Base64.getEncoder.encodeToString(bytes), _sha256(bytes))
  private def _content_json(value: InternalModelCandidateCmlContent): Json = Json.obj("byteLength" -> Json.fromLong(value.byteLength), "rawBytesBase64" -> Json.fromString(value.rawBytesBase64), "sha256" -> Json.fromString(value.sha256))
  private def _artifact_json(value: InternalModelCandidateReviewArtifact): Json = Json.obj("artifactId" -> Json.fromString(value.artifactId), "sha256" -> Json.fromString(value.sha256))
  private def _binding_json(value: InternalModelCandidateReviewBinding): Json = io.circe.parser.parse(new String(InternalModelCandidateReviewBindingCodec.encode(value), StandardCharsets.UTF_8)).toOption.get
  private def _root_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _canonical(Json.fromJsonObject(change(_binding_json(value).asObject.get)))
  private def _artifact_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _root_mutation(value)(root => root.add("candidateArtifact", Json.fromJsonObject(change(root("candidateArtifact").flatMap(_.asObject).get))))
  private def _root_missing_variants(value: InternalModelCandidateReviewBinding): Vector[(String, Array[Byte])] = Vector("candidateArtifact", "candidateIdentity", "candidateModelIdentity", "candidateRevision", "continuityArtifact", "evidenceArtifacts", "profile", "providers", "realizationArtifact", "realizationIdentity", "reviewIdentity", "reviewRevision", "reviewedPackageManifest", "rules", "schemaVersion", "scope", "semanticDiffArtifact", "semanticDiffIdentity", "semanticDiffRevision", "targets").map(key => ("missing-root-" + key) -> _root_mutation(value)(_.remove(key)))
  private def _evidence_artifact_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _root_mutation(value)(root => root.add("evidenceArtifacts", Json.fromValues(root("evidenceArtifacts").flatMap(_.asArray).get.updated(0, Json.fromJsonObject(change(root("evidenceArtifacts").flatMap(_.asArray).get.head.asObject.get))))))
  private def _content_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _root_mutation(value)(root => root.add("reviewedPackageManifest", Json.fromJsonObject(change(root("reviewedPackageManifest").flatMap(_.asObject).get))))
  private def _scope_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _root_mutation(value)(root => root.add("scope", Json.fromJsonObject(change(root("scope").flatMap(_.asObject).get))))
  private def _target_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _root_mutation(value) { root => val targets = root("targets").flatMap(_.asArray).get; root.add("targets", Json.fromValues(targets.updated(0, Json.fromJsonObject(change(targets.head.asObject.get)))))}
  private def _snapshot_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _target_mutation(value)(target => target.add("reviewSnapshot", Json.fromJsonObject(change(target("reviewSnapshot").flatMap(_.asObject).get))))
  private def _snapshot_attribution_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _snapshot_mutation(value)(snapshot => snapshot.add("attribution", Json.fromJsonObject(change(snapshot("attribution").flatMap(_.asObject).get))))
  private def _condition_mutation(value: InternalModelCandidateReviewBinding)(change: JsonObject => JsonObject): Array[Byte] = _snapshot_mutation(value)(snapshot => snapshot.add("condition", Json.fromJsonObject(change(snapshot("condition").flatMap(_.asObject).get))))
  private def _array_object_mutation(value: InternalModelCandidateReviewBinding, key: String)(change: JsonObject => JsonObject): Array[Byte] = _root_mutation(value) { root => val values = root(key).flatMap(_.asArray).get; root.add(key, Json.fromValues(values.updated(0, Json.fromJsonObject(change(values.head.asObject.get))))) }
  private def _duplicate_member(bytes: Array[Byte], member: String): Array[Byte] = { val text = new String(bytes, StandardCharsets.UTF_8); val token = "\"" + member + "\":"; val start = text.indexOf(token); val end = text.indexOf(',', start); val field = text.substring(start, end); (text.substring(0, end) + "," + field + text.substring(end)).getBytes(StandardCharsets.UTF_8) }

  private def _temporary_root(): Path = { Files.createDirectories(Path.of("target")); Files.createTempDirectory(Path.of("target"), "internal-model-candidate-review-") }
  private def _write(path: Path, bytes: Array[Byte]): Unit = { Files.createDirectories(path.getParent); Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) }
  private def _copy_tree(source: Path, destination: Path): Unit = {
    val stream = Files.walk(source)
    try stream.iterator.asScala.foreach { path => val target = destination.resolve(source.relativize(path).toString); if Files.isDirectory(path) then Files.createDirectories(target) else Files.copy(path, target) }
    finally stream.close()
  }
  private def _delete_tree(root: Path): Unit = if Files.exists(root) then { val stream = Files.walk(root); try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists) finally stream.close() }

  private def _sha256(bytes: Array[Byte]): String = "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
  private def _digest(seed: String): String = _sha256(seed.getBytes(StandardCharsets.UTF_8))
  private def _canonical(json: Json): Array[Byte] = (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
  private def _source(authority: String, identity: String, locator: Option[String], revision: Option[String], sha256: String): InternalModelSemanticSource = InternalModelSemanticSource(authority, identity, locator, revision, sha256)
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority), "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null), "sha256" -> Json.fromString(value.sha256))
  private def _scope_json: Json = Json.obj("componentIdentity" -> Json.fromString("component-order"), "projectionContextIdentity" -> Json.fromString("context-order"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase"))

  private def _model_snapshot(): Array[Byte] = {
    val facts = Vector(_fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono"), _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case"), _fact("element", "opaque-shared", "anchor-opaque-element-enrichment", "enrichment:shared"), _fact("element", "opaque-shared", "anchor-opaque-element-kind-Mono", "kind:Mono"), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-enrichment", "enrichment:shared"), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain", "role:StructuralDomain"), _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain")).sortBy(value => (value.hcursor.get[String]("semanticIdentityKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("sourceAnchor").toOption.get))
    _canonical(Json.obj("basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source_json(_model_source)))
  }
  private def _cml_snapshot(source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte]): Array[Byte] = _canonical(Json.obj("basis" -> Json.obj("byteLength" -> Json.fromLong(rawbytes.length.toLong), "projectRelativePath" -> Json.fromString(path), "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes.toArray))), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("cml-baseline"), "source" -> _source_json(source)))

  private def _projection(continuityprofile: String): InternalModelCandidateCmlProjection = {
    val alpha = _target("snapshot-cml-alpha", "patch-alpha", "target-alpha", _cml_alpha_source, "cml/alpha.cml", Vector[Byte](0, -1, 10), "mapping-alpha", "opaque-shared", "element", "effect-alpha", "ref-opaque-element-kind-Mono")
    val beta = _target("snapshot-cml-beta", "patch-beta", "target-beta", _cml_beta_source, "cml/beta.cml", _cml_beta_raw, "mapping-beta", "opaque-shared", "relationship", "effect-beta", "ref-opaque-relationship-role-StructuralDomain")
    InternalModelCandidateCmlProjection("candidate-order-v1", "candidate-model-order", 7, "projection-main", "ccdm-candidate-cml-projection-v1", "realization-main", "1.0", InternalModelSemanticScope("component-order", "context-order", "e-usecase"), Vector(alpha, beta), Vector.empty)
  }
  private def _target(baselineid: String, patchid: String, targetid: String, source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte], mappingid: String, identity: String, kind: String, effectprefix: String, referenceid: String): InternalModelCandidateCmlTarget = { val assertionid = if kind == "element" then "a-opaque-element-kind-Mono" else "a-opaque-relationship-role-StructuralDomain"; val conditionid = if kind == "element" then "c-opaque-element" else "c-opaque-relationship"; val enrichmentid = if kind == "element" then "z-opaque-element-enrichment" else "z-opaque-relationship-enrichment"; val mapping = InternalModelCandidateCmlMapping(Vector(assertionid), "anchor-" + targetid, "cml-" + targetid, Vector(conditionid), Vector(enrichmentid), mappingid, identity, kind); val effects = Vector(InternalModelCandidateCmlEffect("unknown", "supplied compatibility expectation", effectprefix + "-compatibility", "compatibility", Vector(mappingid), referenceid), InternalModelCandidateCmlEffect("unknown", "supplied migration expectation", effectprefix + "-migration", "migration", Vector(mappingid), referenceid)); InternalModelCandidateCmlTarget(baselineid, effects, Vector(mapping), patchid, path, _content(rawbytes.toArray), source, targetid) }

  private def _realization(profile: String): Array[Byte] = {
    val schema = if profile == "ccdm-realization-v2" then "2.0" else "1.0"
    val v2 = profile == "ccdm-realization-v2"
    val assertions = Vector(_assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", Vector.empty, v2), _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", Vector.empty, v2), _assertion("a-opaque-element-kind-Mono", "element", "opaque-shared", "ref-opaque-element-kind-Mono", "kind:Mono", Vector("c-opaque-element"), v2), _assertion("a-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "ref-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", Vector("c-opaque-relationship"), v2), _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", Vector.empty, v2)).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val enrichment = Vector(_assertion("z-opaque-element-enrichment", "element", "opaque-shared", "ref-opaque-element-enrichment", "enrichment:shared", Vector("c-opaque-element"), v2), _assertion("z-opaque-relationship-enrichment", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "enrichment:shared", Vector("c-opaque-relationship"), v2)).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    _canonical(Json.obj("canonicalAssertions" -> Json.fromValues(assertions), "conditions" -> Json.fromValues(Vector(_condition_json("c-opaque-element", "limitation", "element", "opaque-shared", "ref-opaque-element-enrichment", "element evidence remains bounded"), _condition_json("c-opaque-relationship", "limitation", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "relationship evidence remains bounded"))), "elements" -> Json.fromValues(Vector(_element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")), _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")), _element("opaque-shared", "Mono", "Opaque shared element", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element")))), "enrichmentAssertions" -> Json.fromValues(enrichment), "profile" -> Json.fromString(profile), "realizationIdentity" -> Json.fromString("realization-" + schema), "relationships" -> Json.fromValues(Vector(_relationship("opaque-shared", "StructuralDomain", "e-mono", "e-usecase", Vector("a-opaque-relationship-role-StructuralDomain"), Vector("z-opaque-relationship-enrichment"), Vector("c-opaque-relationship")), _relationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase", Vector("a-r-mono-domain-role-StructuralDomain")))), "schemaVersion" -> Json.fromString(schema), "scope" -> _scope_json, "sourceReferences" -> Json.fromValues(Vector(_reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"), _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"), _reference("ref-opaque-element-enrichment", "element", "opaque-shared", "anchor-opaque-element-enrichment"), _reference("ref-opaque-element-kind-Mono", "element", "opaque-shared", "anchor-opaque-element-kind-Mono"), _reference("ref-opaque-relationship-enrichment", "relationship", "opaque-shared", "anchor-opaque-relationship-enrichment"), _reference("ref-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain"), _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain")).sortBy(_.hcursor.get[String]("referenceId").toOption.get)), "successorLinks" -> Json.arr(), "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))))
  }

  private def _binding(profile: String, schema: String): Array[Byte] = {
    def _record_(kind: String, identity: String, role: String, ids: Vector[String]): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(ids.map(Json.fromString)), "conditionIds" -> Json.arr(), "enrichmentAssertionIds" -> Json.arr(), "recordKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "sequenceAssertionId" -> Json.Null, "viewRole" -> Json.fromString(role))
    val families = Vector("MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono")), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))), "UseCaseCommunicationProjection" -> Vector(_record_("element", "e-usecase", "use-case", Vector("a-e-usecase-kind-use-case"))), "EntityModelProjection" -> Vector.empty[Json], "EventModelProjection" -> Vector.empty[Json], "StructureViewProjection" -> Vector.empty[Json], "ClassificationViewProjection" -> Vector.empty[Json], "WorkflowProjection" -> Vector.empty[Json], "StateMachineProjection" -> Vector.empty[Json])
    _canonical(Json.obj("profile" -> Json.fromString(profile), "realizationArtifactId" -> Json.fromString("realization-main"), "schemaVersion" -> Json.fromString(schema), "scope" -> _scope_json, "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })))
  }

  private def _semantic_diff(projection: InternalModelCandidateCmlProjection, candidatebytes: Array[Byte]): InternalModelSemanticDiff = {
    val targets = projection.targets.map { target =>
      val mapping = target.mappings.head
      val condition = _condition("available", "authorized", None, None, None, None, None, None, Vector("condition limitation"))
      val entry = CandidateDesignSemanticDiffEntry("entry-" + target.targetId, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.patchIdentity, projection.candidateModelIdentity, "category", "action", mapping.semanticIdentity, Some("before"), Some("after"), "relationship", _attribution("entry-authority-" + target.targetId), condition, Vector("entry limitation"), Some("entry tie"))
      val patch = CandidateDesignProposedCmlPatchTrace(target.patchIdentity, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.source.authority, target.source.locator.getOrElse("patch-locator-" + target.targetId), target.source.sha256, target.proposedContent.sha256, _attribution("patch-authority-" + target.targetId), condition, Vector("patch limitation"), Some("patch tie"))
      InternalModelSemanticDiffTarget(Vector(InternalModelSemanticDiffMappedEntry(entry, mapping.mappingId, mapping.semanticIdentityKind)), patch, target.targetId)
    }
    InternalModelSemanticDiff("candidate-main", _sha256(candidatebytes), projection.candidateIdentity, projection.candidateModelIdentity, projection.candidateRevision, "ccdm-semantic-diff-v1", "1.0", projection.scope, "semantic-diff-order", 3, targets, Vector.empty)
  }

  private def _attribution(authority: String): ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution("source-id", authority, "source-locator")
  private def _condition(availability: String, authorization: String, redaction: Option[String], absence: Option[String], ambiguity: Option[String], conflict: Option[String], staleness: Option[String], malformed: Option[String], limitations: Vector[String]): ComponentDashboardCondition = ComponentDashboardCondition(availability, authorization, redaction, absence, ambiguity, conflict, staleness, malformed, limitations)
  private def _condition_json(id: String, kind: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json = Json.obj("affectedIdentity" -> Json.fromString(affectedidentity), "affectedKind" -> Json.fromString(affectedkind), "conditionId" -> Json.fromString(id), "detail" -> Json.fromString(detail), "kind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid))
  private def _fact(kind: String, identity: String, anchor: String, content: String): Json = Json.obj("componentIdentity" -> Json.fromString("component-order"), "content" -> Json.fromString(content), "limitations" -> Json.arr(), "projectionContextIdentity" -> Json.fromString("context-order"), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceAnchor" -> Json.fromString(anchor))
  private def _assertion(id: String, kind: String, identity: String, referenceid: String, content: String, conditionids: Vector[String], v2: Boolean): Json = {
    val fields = Vector("assertionId" -> Json.fromString(id), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "content" -> Json.fromString(content), "semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind), "sourceReferenceId" -> Json.fromString(referenceid)) ++ Option.when(v2)("association" -> Json.Null)
    Json.obj(fields*)
  }
  private def _element(identity: String, kind: String, label: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "kind" -> Json.fromString(kind), "label" -> Json.fromString(label))
  private def _relationship(identity: String, role: String, source: String, target: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)), "direction" -> Json.fromString("source-to-target"), "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)), "identity" -> Json.fromString(identity), "label" -> Json.fromString(identity), "role" -> Json.fromString(role), "sourceElementIdentity" -> Json.fromString(source), "targetElementIdentity" -> Json.fromString(target))
  private def _reference(id: String, kind: String, identity: String, anchor: String): Json = Json.obj("referenceId" -> Json.fromString(id), "snapshotArtifactId" -> Json.fromString("snapshot-model"), "source" -> _source_json(_model_source), "sourceAnchor" -> Json.fromString(anchor), "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind)))
}
