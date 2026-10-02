package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}
import java.util.{Arrays, Base64}
import scala.jdk.CollectionConverters.*
import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 29, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
/** Executable specification of complete typed review subjects and lossless evidence. */
final class InternalModelCandidateReviewBindingValidatorSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  private final case class FixtureOptions(
    bindingchange: InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding = identity,
    reviewbyteschange: Array[Byte] => Array[Byte] = identity,
    missingrequiredpath: Option[String] = None,
    extraartifact: Option[(String, InternalModelArtifactRole, String, Array[Byte])] = None,
    includeapproval: Boolean = true,
    includereview: Boolean = true,
    reviewrole: InternalModelArtifactRole = InternalModelArtifactRole.Validation,
    reviewdependencies: Option[Vector[InternalModelArtifactReference]] = None,
    selectedreview: InternalModelArtifactReference = _artifact_reference("review-main"),
    includeoptionalreview: Boolean = false,
    reviewstate: String = "reviewed",
    allnullablefacets: Boolean = false,
    permuteencounter: Boolean = false,
    continuityprofile: String = "ccdm-projection-binding-v3",
    continuityschema: String = "3.0",
    realizationprofile: String = "ccdm-realization-v3",
    realizationschema: String = "3.0",
    carrierrevision: Long = 43L,
    missingsourceversion: Boolean = false
  )

  private val _printer = Printer.noSpacesSortKeys
  private val _fixture_parent = Path.of("target", "P104-TYPED-REVIEW-BINDING-001")
  private val _project_yaml = "project:\n  namespace: org.example\n  id: candidate-review-sample\n"
  private val _package_id = "01234567-89ab-cdef-0123-456789abcdef"
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_alpha_raw = "alpha cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _cml_beta_raw = "beta cml baseline\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _model_source = _source("model-authority", "model-source", Some("catalog/model-source"), Some("revision-1"))
  private val _cml_alpha_source = _source("cml-authority", "cml-alpha", Some("catalog/cml-alpha"), Some("revision-alpha"))
  private val _cml_beta_source = _source("cml-authority", "cml-beta", Some("catalog/cml-beta"), Some("revision-beta"))
  private val _artifact_versions = Map(
    "candidate-main" -> (17L, InternalModelArtifactRole.Projection),
    "decision-main" -> (47L, InternalModelArtifactRole.Decision),
    "open-issue-main" -> (53L, InternalModelArtifactRole.OpenIssue),
    "evidence-alpha" -> (59L, InternalModelArtifactRole.Validation),
    "evidence-beta" -> (61L, InternalModelArtifactRole.Validation),
    "projection-main" -> (29L, InternalModelArtifactRole.Projection),
    "realization-main" -> (13L, InternalModelArtifactRole.Realization),
    "resume-main" -> (67L, InternalModelArtifactRole.Resume),
    "semantic-diff-main" -> (31L, InternalModelArtifactRole.Projection),
    "snapshot-cml-alpha" -> (11L, InternalModelArtifactRole.SourceSnapshot),
    "snapshot-cml-beta" -> (37L, InternalModelArtifactRole.SourceSnapshot),
    "snapshot-model" -> (41L, InternalModelArtifactRole.SourceSnapshot),
    "snapshot-optional" -> (71L, InternalModelArtifactRole.SourceSnapshot),
    "review-main" -> (73L, InternalModelArtifactRole.Validation),
    "review-optional-absent" -> (79L, InternalModelArtifactRole.Validation),
    "approval-extra" -> (83L, InternalModelArtifactRole.Approval),
    "validation-inert" -> (89L, InternalModelArtifactRole.Validation),
    "dependency-evidence" -> (97L, InternalModelArtifactRole.Validation)
  )

  "Candidate-review portable admission" should {
    "complete typed subject and carrier distinction" which {
      "admit a real two-target subject in a newer carrier with explicit review selection" in {
        Given("a current two-target package, independent logical/artifact revisions, raw decision/open-issue inputs and an absent optional snapshot")
        _with_fixture() { root =>
          _prove_upstream(root)
          When("the exact external review reference and independent rule/provider basis cross admission")
          val result = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
          Then("the complete subject and carrier retain their distinct identities and every snapshot/evidence binding")
          withClue(result.show) {
            val admission = result.toOption.get
            admission.binding.subject shouldBe _binding_value().subject
            admission.carrierPackageContext.revision shouldBe 43L
            admission.binding.subject.subjectRevision.value shouldBe 101L
            admission.carrierPackageContext.artifacts.find(_.reference.artifactId.value == "snapshot-optional").map(value => (value.required, value.present)) shouldBe Some((false, false))
            admission.binding.subject.artifacts.map(_.artifactId.value) should not contain "snapshot-optional"
            admission.binding.targets shouldBe _binding_value().targets
            admission.reviewArtifactReference shouldBe _artifact_reference("review-main")
            admission.reviewArtifactPackageRelativePath shouldBe "reviews/candidate-review.json"
            admission.binding.subject.artifacts.map(_.artifactId.value) should contain allOf ("decision-main", "open-issue-main")
          }
        }
      }

      "admit an approved attributed snapshot with absent nullable facets without producing human approval" in {
        Given("current two-target evidence with approved state and null optional facets")
        _with_fixture(FixtureOptions(reviewstate = "approved", allnullablefacets = true)) { root =>
          _prove_upstream(root)
          When("the exact selected review crosses admission")
          val result = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
          Then("state and every nullable facet are retained as review evidence")
          result.toOption.map(_.binding.targets) shouldBe Some(_binding_value(reviewstate = "approved", allnullablefacets = true).targets)
          _json_text(InternalModelCandidateReviewBindingCodec.encode(result.toOption.get.binding)) should not include "humanApproval"
        }
      }

      "retain generated opaque Unicode identities, ties, nullable facets and limitation multiplicity" in {
        Given("generated valid Unicode and positive Long revisions in independently supplied logical domains")
        val unicode = Gen.nonEmptyListOf(Gen.oneOf(Gen.alphaNumChar.map(_.toString), Gen.const("漢"), Gen.const("😀"), Gen.const("é"))).map(_.mkString)
        forAll(unicode, Gen.chooseNum(1L, Long.MaxValue), Gen.oneOf(true, false)) { (generated, revision, nullable) =>
          val basis = _basis()
          val binding = _binding_value(allnullablefacets = nullable).copy(
            reviewReference = _record_reference(generated, revision),
            subject = _binding_value().subject.copy(subjectId = InternalModelRecordId.from(generated).toOption.get, subjectRevision = InternalModelRecordRevision.from(revision).toOption.get),
            rules = basis.rules.map(value => value.copy(ruleId = generated + value.ruleId, ruleVersion = " " + generated + " ")),
            providers = basis.providers.map(value => value.copy(providerId = generated + value.providerId, providerVersion = generated)),
            targets = _binding_value(allnullablefacets = nullable).targets.map { target =>
              val snapshot = target.reviewSnapshot
              target.copy(reviewSnapshot = snapshot.copy(
                stableTieKey = if nullable then None else Some(generated),
                limitations = Vector(generated, generated),
                condition = snapshot.condition.copy(limitations = Vector(generated, generated))
              ))
            }
          )
          When("the typed writer and strict reader interpret supplied evidence")
          val decoded = InternalModelCandidateReviewBindingCodec.decode(InternalModelCandidateReviewBindingCodec.encode(binding).toVector)
          Then("opaque values, supplied array order and repeated limitations survive without normalization")
          decoded shouldBe Right(binding)
          decoded.toOption.get.targets.head.reviewSnapshot.limitations shouldBe Vector(generated, generated)
        }
      }

      "admit permuted topological fixture encounter while retaining exact subject order" in {
        Given("normal and reversed encounters of the same typed inventory")
        var normal = Option.empty[InternalModelReviewSubject]
        var permuted = Option.empty[InternalModelReviewSubject]
        When("both real fixture packages cross typed admission")
        _with_fixture() { root => normal = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis()).toOption.map(_.binding.subject) }
        _with_fixture(FixtureOptions(permuteencounter = true)) { root => permuted = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis()).toOption.map(_.binding.subject) }
        Then("encounter order leaves the exact logical subject unchanged")
        normal shouldBe Some(_binding_value().subject)
        permuted shouldBe normal
      }
    }
  }

  "Candidate-review closed grammar" should {
    "strict structure independent of JSON presentation" which {
      "reject missing extra null wrong-scalar bare hash and old fields at every nested object" in {
        Given("one current binding and every root/subject/package/artifact/record/rule/provider/target/snapshot/condition boundary")
        val binding = _binding_value()
        val json = _binding_json(binding)
        val nullablekeys = Set("stableTieKey", "ambiguity", "conflict", "explicitAbsence", "malformedEvidence", "redaction", "staleness")
        val variants = _object_paths(json).flatMap { path =>
          val objectvalue = _at_path(json, path).asObject.get
          val fields = objectvalue.keys.toVector.flatMap { key =>
            Vector(
              (path.mkString("/") + "/missing-" + key) -> _replace_path(json, path, value => value.mapObject(_.remove(key))),
              (path.mkString("/") + "/null-" + key) -> _replace_path(json, path :+ key, _ => Json.Null),
              (path.mkString("/") + "/wrong-type-" + key) -> _replace_path(json, path :+ key, _ => Json.True)
            ).filterNot { case (name, value) => value == json || (nullablekeys.contains(key) && name.endsWith("/null-" + key)) }
          }
          fields ++ Vector(
            (path.mkString("/") + "/extra") -> _replace_path(json, path, _.mapObject(_.add("unexpected", Json.True))),
            (path.mkString("/") + "/hash") -> _replace_path(json, path, _.mapObject(_.add("sha256", Json.fromString("old-control"))))
          )
        } ++ Vector("candidateIdentity", "candidateRevision", "reviewIdentity", "reviewRevision", "realizationIdentity", "semanticDiffIdentity", "semanticDiffRevision", "reviewedPackageManifest", "candidateArtifact").map(key => ("old-" + key) -> json.mapObject(_.add(key, Json.fromString("old")))) ++
          Vector("candidateArtifactReference", "candidateReference", "subject").map(key => ("bare-" + key) -> json.mapObject(_.add(key, Json.fromString("bare-id"))))
        When("every named mutation crosses the review reader")
        val results = variants.map { case (name, value) => name -> InternalModelCandidateReviewBindingCodec.decode(_json_bytes(value).toVector).isLeft }
        Then("the current control succeeds and all malformed boundaries reject")
        InternalModelCandidateReviewBindingCodec.decode(_json_bytes(json).toVector) shouldBe Right(binding)
        withClue(results.filterNot(_._2).map(_._1).mkString(", ")) { results.forall(_._2) shouldBe true }
      }

      "reject duplicate members malformed UTF8 BOM trailing content and invalid escaped Unicode" in {
        Given("one current binding with transport and duplicate mutations throughout its closed structure")
        val bytes = InternalModelCandidateReviewBindingCodec.encode(_binding_value())
        val text = _json_text(bytes)
        val duplicates = Vector("candidateModelIdentity", "subjectId", "packageId", "artifactId", "recordId", "ruleId", "providerId", "targetId", "patchId", "sourceId", "availability").map(key => key -> _duplicate_member(bytes, key))
        val unicode = _json_text(_json_bytes(_replace_path(_binding_json(_binding_value()), Vector("candidateModelIdentity"), _ => Json.fromString("INVALIDUNICODE")))).replace("\"INVALIDUNICODE\"", "\"\\ud800\"")
        val variants = duplicates ++ Vector(
          "utf8" -> Array[Byte](0xc3.toByte, 0x28.toByte),
          "bom" -> (Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ bytes),
          "trailing" -> (text + "{}").getBytes(StandardCharsets.UTF_8),
          "invalid-surrogate" -> unicode.getBytes(StandardCharsets.UTF_8)
        )
        When("each transport mutation crosses the reader")
        val results = variants.map { case (name, value) => name -> InternalModelCandidateReviewBindingCodec.decode(value.toVector).isLeft }
        Then("every ambiguous or malformed transport rejects")
        withClue(results.filterNot(_._2).map(_._1).mkString(", ")) { results.forall(_._2) shouldBe true }
      }

      "accept harmless key order whitespace and equivalent escaping" in {
        Given("a current binding with recursively reversed object keys and equivalent escaping")
        val binding = _binding_value()
        val json = _reverse_keys(_binding_json(binding))
        val text = Printer.spaces2.print(json)
        val variants = Vector(text, " \n" + text + " \n", text.replace("candidate-model-order", "candidate-model-\\u006frder"), _json_text(InternalModelCandidateReviewBindingCodec.encode(binding)).trim)
        When("presentation variants cross strict structural admission")
        val results = variants.map(value => InternalModelCandidateReviewBindingCodec.decode(value.getBytes(StandardCharsets.UTF_8).toVector))
        Then("all variants retain exactly the same typed evidence")
        results shouldBe Vector.fill(variants.size)(Right(binding))
      }

      "reject lexical revisions in every artifact record and subject slot" in {
        Given("all positive revision slots and independently altered lexical tokens")
        val json = _binding_json(_binding_value())
        val paths = _object_paths(json).flatMap(path => _at_path(json, path).asObject.get.keys.filter(key => Set("artifactRevision", "recordRevision", "subjectRevision").contains(key)).map(path :+ _).toVector)
        val lexemes = Vector("0", "-1", "1.0", "1e0", "\"1\"", "null", "9223372036854775808")
        val original = _json_text(_json_bytes(json))
        val variants = paths.flatMap { path =>
          lexemes.map { lexeme =>
            val tokenized = _json_text(_json_bytes(_replace_path(json, path, _ => Json.fromString("LEXICALREVISION"))))
            val changed = tokenized.replace("\"LEXICALREVISION\"", lexeme)
            changed should not be original
            (path.mkString("/") + ":" + lexeme) -> changed
          }
        }
        When("each actually changed lexical revision crosses the reader")
        val results = variants.map { case (name, value) => name -> InternalModelCandidateReviewBindingCodec.decode(value.getBytes(StandardCharsets.UTF_8).toVector).isLeft }
        Then("zero negative fraction exponent string null and overflow never become positive producer revisions")
        withClue(results.filterNot(_._2).map(_._1).mkString(", ")) { results.forall(_._2) shouldBe true }
      }

      "retain independently allocated one and maximum Long revisions" in {
        Given("producer references at both positive Long boundaries")
        val binding = _binding_value().copy(
          candidateArtifactReference = _artifact_reference("candidate-main").copy(artifactRevision = InternalModelArtifactRevision.from(1L).toOption.get),
          candidateReference = _record_reference("candidate-order-v1", Long.MaxValue),
          reviewReference = _record_reference("review-identity", 1L),
          subject = _binding_value().subject.copy(subjectRevision = InternalModelRecordRevision.from(Long.MaxValue).toOption.get)
        )
        When("the explicitly supplied references are written and read")
        val decoded = InternalModelCandidateReviewBindingCodec.decode(InternalModelCandidateReviewBindingCodec.encode(binding).toVector)
        Then("the independent domains retain both boundary values")
        decoded shouldBe Right(binding)
      }

      "reject empty duplicate and unordered semantic arrays and malformed facets" in {
        Given("current complete arrays and independent semantic array/facet mutations")
        val json = _binding_json(_binding_value())
        val paths = Vector(Vector("evidenceArtifacts"), Vector("rules"), Vector("providers"), Vector("targets"), Vector("subject", "artifacts"), Vector("targets", "0", "evidenceArtifactIds"))
        val variants = paths.flatMap { path =>
          val values = _at_path(json, path).asArray.get
          Vector(
            _replace_path(json, path, _ => Json.arr()),
            _replace_path(json, path, _ => Json.fromValues(values :+ values.head))
          ) ++ Option.when(values.size > 1)(_replace_path(json, path, _ => Json.fromValues(values.reverse)))
        } ++ Vector(
          _replace_path(json, Vector("targets", "1", "reviewSnapshot", "id"), _ => _at_path(json, Vector("targets", "0", "reviewSnapshot", "id"))),
          _replace_path(json, Vector("targets", "0", "reviewSnapshot", "stableTieKey"), _ => Json.fromString(" ")),
          _replace_path(json, Vector("targets", "0", "reviewSnapshot", "limitations"), _ => Json.arr(Json.fromString(" "))),
          _replace_path(json, Vector("targets", "0", "reviewSnapshot", "condition", "limitations"), _ => Json.arr(Json.fromString(" "))),
          _replace_path(json, Vector("targets", "0", "reviewSnapshot", "condition", "ambiguity"), _ => Json.fromString(" ")),
          _replace_path(json, Vector("profile"), _ => Json.fromString("ccdm-candidate-review-binding-v1")),
          _replace_path(json, Vector("schemaVersion"), _ => Json.fromString("1.0")),
          _replace_path(json, Vector("candidateReference", "recordId"), _ => Json.fromString(" ")),
          _replace_path(json, Vector("subject", "artifacts", "0", "role"), _ => Json.fromString("resume")),
          _replace_path(json, Vector("evidenceArtifacts", "0", "role"), _ => Json.fromString("projection"))
        )
        When("the supplied semantic arrays and facets cross the reader")
        val results = variants.map(value => InternalModelCandidateReviewBindingCodec.decode(_json_bytes(value).toVector).isLeft)
        Then("the writer never repairs order duplicates emptiness or malformed semantic values")
        results shouldBe Vector.fill(variants.size)(true)
      }
    }
  }

  "Candidate-review complete subject" should {
    "semantic roots and carrier controls" which {
      "reject a missing contributing root changed exact input unknown duplicate unordered package or scope subject" in {
        Given("a current complete subject and independent exact-reference changes")
        val changes: Vector[(String, InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding)] = Vector(
          "missing-root" -> (value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.filterNot(_.artifactId.value == "decision-main")))),
          "changed-input-revision" -> (value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.map(reference => if reference.artifactId.value == "decision-main" then reference.copy(artifactRevision = InternalModelArtifactRevision.from(48L).toOption.get) else reference)))),
          "changed-input-role" -> (value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.map(reference => if reference.artifactId.value == "decision-main" then reference.copy(role = InternalModelArtifactRole.OpenIssue) else reference)))),
          "unlisted-selected-evidence" -> (value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.filterNot(_.artifactId.value == "evidence-alpha")))),
          "unknown-input" -> (value => value.copy(subject = value.subject.copy(artifacts = _sorted(value.subject.artifacts :+ _new_artifact_reference("unknown-input", InternalModelArtifactRole.Decision))))),
          "duplicate-input" -> (value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts :+ value.subject.artifacts.head))),
          "unordered-input" -> (value => value.copy(subject = value.subject.copy(artifacts = value.subject.artifacts.reverse))),
          "subject-package" -> (value => value.copy(subject = value.subject.copy(packageReference = value.subject.packageReference.copy(projectId = InternalModelProjectToken.from("other-project").toOption.get)))),
          "subject-package-id" -> (value => value.copy(subject = value.subject.copy(packageReference = value.subject.packageReference.copy(packageId = InternalModelPackageId.from("fedcba98-7654-3210-fedc-ba9876543210").toOption.get)))),
          "subject-namespace" -> (value => value.copy(subject = value.subject.copy(packageReference = value.subject.packageReference.copy(projectNamespace = InternalModelProjectToken.from("org.other").toOption.get)))),
          "subject-scope" -> (value => value.copy(subject = value.subject.copy(scope = value.subject.scope.copy(componentIdentity = "other-component")))),
          "subject-context" -> (value => value.copy(subject = value.subject.copy(scope = value.subject.scope.copy(projectionContextIdentity = "other-context")))),
          "subject-usecase" -> (value => value.copy(subject = value.subject.copy(scope = value.subject.scope.copy(selectedUseCaseElementIdentity = "other-usecase")))),
          "review-self" -> (value => value.copy(subject = value.subject.copy(artifacts = _sorted(value.subject.artifacts :+ _artifact_reference("review-main"))))),
          "approval-input" -> (value => value.copy(subject = value.subject.copy(artifacts = _sorted(value.subject.artifacts :+ _artifact_reference("approval-extra"))))),
          "resume-input" -> (value => value.copy(subject = value.subject.copy(artifacts = _sorted(value.subject.artifacts :+ _artifact_reference("resume-main")))))
        )
        When("each subject crosses review admission over independently admitted upstream fixtures")
        val results = changes.map { case (name, change) => name -> _validate(change, FixtureOptions(reviewdependencies = Some(_binding_value().subject.artifacts))) }
        Then("every incomplete inconsistent or forbidden subject rejects")
        withClue(results.filter(_._2).map(_._1).mkString(", ")) { results.forall(!_._2) shouldBe true }
      }

      "observe every additional present source decision open-issue and projection" in {
        Given("current upstream inputs plus one additional inventoried semantic entry outside the stored subject")
        val extras = Vector(
          ("source-extra", InternalModelArtifactRole.SourceSnapshot, "extras/source.json", _model_snapshot()),
          ("decision-extra", InternalModelArtifactRole.Decision, "extras/decision.json", "raw decision\n".getBytes(StandardCharsets.UTF_8)),
          ("issue-extra", InternalModelArtifactRole.OpenIssue, "extras/issue.json", "raw open issue\n".getBytes(StandardCharsets.UTF_8))
        )
        When("each declared present semantic entry is added to an otherwise current real fixture")
        val results = extras.map(extra => _validate(identity, FixtureOptions(extraartifact = Some(extra)))) :+ {
          var admitted = false
          _with_fixture() { root =>
            _prove_upstream(root)
            val base = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, _artifact_reference("review-main")).toOption.get
            val extra = InternalModelVerifiedArtifactContext(_new_artifact_reference("projection-extra", InternalModelArtifactRole.Projection), "extras/projection.json", true, Vector.empty, true)
            val handoff = _carrier_handoff(base, base.carrierPackageContext.artifacts :+ extra, _binding_value(), _binding_value().subject.artifacts)
            _prove_capture(handoff)
            admitted = InternalModelCandidateReviewBindingValidator.validateVerified(handoff, _basis()).isSuccess
          }
          admitted
        }
        Then("the original smaller subject cannot hide any additional semantic input")
        results shouldBe Vector(false, false, false, false)
      }

      "retain control-only revision approval resume and unselected Validation changes outside the subject" in {
        Given("an unchanged complete subject and independently advanced carrier controls")
        val options = Vector(
          FixtureOptions(carrierrevision = 107L),
          FixtureOptions(includeapproval = false),
          FixtureOptions(extraartifact = Some(("resume-extra", InternalModelArtifactRole.Resume, "extras/resume.json", "inert resume\\n".getBytes(StandardCharsets.UTF_8)))),
          FixtureOptions(extraartifact = Some(("approval-other", InternalModelArtifactRole.Approval, "extras/approval.json", "inert approval\\n".getBytes(StandardCharsets.UTF_8)))),
          FixtureOptions(extraartifact = Some(("review-other", InternalModelArtifactRole.Validation, "extras/review.json", "unselected review\\n".getBytes(StandardCharsets.UTF_8)))),
          FixtureOptions(extraartifact = Some(("validation-inert", InternalModelArtifactRole.Validation, "extras/inert.json", "unselected validation\n".getBytes(StandardCharsets.UTF_8))))
        )
        When("each real control-only carrier crosses admission")
        val results = options.map { option =>
          var result = Option.empty[InternalModelCandidateReviewBindingAdmission]
          _with_fixture(option) { root =>
            _prove_upstream(root)
            result = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis()).toOption
          }
          result.map(_.binding.subject)
        }
        Then("carrier controls and unselected validation never change the unchanged subject")
        results shouldBe Vector.fill(options.size)(Some(_binding_value().subject))
      }

      "admit complete transitive validation evidence while rejecting missing cycles and forbidden dependencies" in {
        Given("a current capture and a raw decision's explicit dependency on another Validation artifact")
        _with_fixture() { root =>
          _prove_upstream(root)
          val base = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, _artifact_reference("review-main")).toOption.get
          val dependency = InternalModelVerifiedArtifactContext(_artifact_reference("dependency-evidence"), "evidence/dependency.json", true, Vector.empty, true)
          val entries = base.carrierPackageContext.artifacts.map(entry => if entry.reference.artifactId.value == "decision-main" then entry.copy(dependencies = Vector(dependency.reference)) else entry) :+ dependency
          val artifacts = _sorted(_binding_value().subject.artifacts :+ dependency.reference)
          val binding = _binding_value().copy(subject = _binding_value().subject.copy(artifacts = artifacts))
          val complete = _carrier_handoff(base, entries, binding, artifacts)
          _prove_capture(complete)
          val missing = _carrier_handoff(base, entries.filterNot(_.reference == dependency.reference), binding, artifacts)
          val cycleentries = entries.map(entry => if entry.reference == dependency.reference then entry.copy(dependencies = Vector(_artifact_reference("decision-main"))) else entry)
          val cycle = _carrier_handoff(base, cycleentries, binding, artifacts)
          val forbidden = Vector("review-main", "approval-extra", "resume-main", "snapshot-optional").map { id =>
            val altered = base.carrierPackageContext.artifacts.map(entry => if entry.reference.artifactId.value == "decision-main" then entry.copy(dependencies = Vector(_artifact_reference(id))) else entry)
            _carrier_handoff(base, altered, _binding_value(), _binding_value().subject.artifacts)
          }
          When("complete and independently broken closures cross the pure review boundary")
          val successful = InternalModelCandidateReviewBindingValidator.validateVerified(complete, _basis())
          val failures = (Vector(missing, cycle) ++ forbidden).map(value => InternalModelCandidateReviewBindingValidator.validateVerified(value, _basis()).isSuccess)
          Then("the complete exact transitive input is retained and every missing cyclic forbidden or absent dependency rejects")
          successful.toOption.map(_.binding.subject.artifacts) shouldBe Some(artifacts)
          failures shouldBe Vector.fill(6)(false)
        }
      }

      "reject changed captured semantic revision or role while preserving current upstream admission" in {
        Given("one captured current candidate/diff and a raw decision selected by exact producer reference")
        _with_fixture() { root =>
          _prove_upstream(root)
          val base = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, _artifact_reference("review-main")).toOption.get
          val changes = Vector(
            _artifact_reference("decision-main").copy(artifactRevision = InternalModelArtifactRevision.from(109L).toOption.get),
            _artifact_reference("decision-main").copy(role = InternalModelArtifactRole.OpenIssue)
          ).map { reference =>
            val entries = base.carrierPackageContext.artifacts.map(entry => if entry.reference.artifactId.value == "decision-main" then entry.copy(reference = reference) else entry)
            val dependencies = _sorted(_binding_value().subject.artifacts.map(value => if value.artifactId.value == "decision-main" then reference else value))
            val handoff = _carrier_handoff(base, entries, _binding_value(), dependencies)
            _prove_capture(handoff)
            handoff
          }
          When("the old stored subject meets changed current exact semantic references")
          val results = changes.map(value => InternalModelCandidateReviewBindingValidator.validateVerified(value, _basis()).isSuccess)
          Then("declared revision and role changes are observable without comparing payload bytes")
          results shouldBe Vector(false, false)
        }
      }

      "require the selected review dependencies to equal exactly the ordered subject" in {
        Given("a current subject with missing extra absent reordered and self review dependency claims")
        val exact = _binding_value().subject.artifacts
        val variants = Vector(
          exact.dropRight(1),
          _sorted(exact :+ _artifact_reference("approval-extra")),
          _sorted(exact :+ _artifact_reference("snapshot-optional")),
          exact.reverse,
          _sorted(exact :+ _artifact_reference("review-main"))
        )
        When("each dependency vector crosses the real capture boundary")
        val results = variants.map(value => _validate(identity, FixtureOptions(reviewdependencies = Some(value)), proveupstream = false))
        Then("a set-equivalent reorder or any missing extra absent or self reference rejects")
        results shouldBe Vector.fill(variants.size)(false)
      }
    }

    "explicit external selection" which {
      "accept explicit review selection and inert approval without promoting review state" in {
        Given("one exact selected review and a raw approval control in a current carrier")
        _with_fixture() { root =>
          _prove_upstream(root)
          When("the caller supplies the complete review artifact reference")
          val result = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
          Then("the admission retains review evidence and inert carrier controls")
          result.isSuccess shouldBe true
          result.toOption.get.binding.targets.map(_.reviewSnapshot.state) shouldBe Vector("reviewed", "reviewed")
          result.toOption.get.binding.subject.artifacts.map(_.artifactId.value) should not contain "approval-extra"
        }
      }

      "accept the selected review with absent optional approval" in {
        Given("a current carrier retaining an optional approval inventory entry whose file is absent")
        _with_fixture(FixtureOptions(missingrequiredpath = Some("carrier/approval-extra.json"))) { root =>
          _prove_upstream(root)
          When("the exact selected review is admitted")
          val result = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
          Then("review admission does not infer or require human approval")
          result.isSuccess shouldBe true
          result.toOption.get.carrierPackageContext.artifacts.find(_.reference.artifactId.value == "approval-extra").map(_.present) shouldBe Some(false)
        }
      }

      "reject missing absent wrong-role wrong-revision and wrong-profile explicit selections" in {
        Given("otherwise current fixtures with one external review selection defect")
        val options = Vector(
          FixtureOptions(includereview = false),
          FixtureOptions(selectedreview = _new_artifact_reference("review-absent", InternalModelArtifactRole.Validation)),
          FixtureOptions(includereview = false, includeoptionalreview = true, selectedreview = _artifact_reference("review-optional-absent")),
          FixtureOptions(reviewrole = InternalModelArtifactRole.Decision),
          FixtureOptions(selectedreview = _artifact_reference("review-main").copy(artifactRevision = InternalModelArtifactRevision.from(74L).toOption.get)),
          FixtureOptions(selectedreview = _artifact_reference("review-main").copy(role = InternalModelArtifactRole.Decision))
        )
        When("each explicit selection and unsupported review profile crosses admission")
        val results = options.map(value => _validate(identity, value, proveupstream = false)) :+
          _validate(_.copy(profile = "ccdm-candidate-review-next"), proveupstream = false)
        Then("no ID-only alternate latest role or profile selection succeeds")
        results shouldBe Vector.fill(results.size)(false)
      }

      "reject missing required source and selected evidence files" in {
        Given("current fixtures whose own required source or selected evidence path is absent")
        val options = Vector("snapshots/cml-alpha.json", "evidence/alpha.json").map(path => FixtureOptions(missingrequiredpath = Some(path)))
        When("fresh admission reads each incomplete fixture")
        val results = options.map(value => _validate(identity, value, proveupstream = false))
        Then("required presence cannot be inferred from the subject declaration")
        results shouldBe Vector(false, false)
      }
    }
  }
  "Candidate-review selected binding matrix" should {
    "admit two targets sharing one selected evidence artifact with a complete global union" in {
      Given("a real two-target binding whose target-local nonempty sorted evidence IDs share evidence-alpha and whose global union names that one artifact")
      val sharedchange = (value: InternalModelCandidateReviewBinding) => value.copy(
        evidenceArtifacts = Vector(_artifact_reference("evidence-alpha")),
        subject = value.subject.copy(artifacts = value.subject.artifacts.filterNot(_.artifactId.value == "evidence-beta")),
        targets = value.targets.map(_.copy(evidenceArtifactIds = Vector("evidence-alpha")))
      )
      val shared = sharedchange(_binding_value())
      var admission = false

      When("the shared-evidence binding is decoded and admitted through the actual package validator")
      val decoded = InternalModelCandidateReviewBindingCodec.decode(InternalModelCandidateReviewBindingCodec.encode(shared).toVector)
      _with_fixture(FixtureOptions(bindingchange = sharedchange)) { root =>
        admission = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis()).isSuccess
      }

      Then("the typed codec and filesystem admission preserve the original union contract without requiring one occurrence per target")
      decoded.isRight shouldBe true
      admission shouldBe true
    }

    "reject every selected artifact, identity, revision, scope, target, snapshot, and evidence mismatch independently" in {
      Given("a valid candidate-review package and one-field binding changes on independently admitted current upstream evidence")
      val changes: Vector[(String, InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding)] = Vector(
        "candidate-id" -> (value => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(artifactId = InternalModelArtifactId.from("candidate-other").toOption.get))),
                "continuity-id" -> (value => value.copy(continuityArtifactReference = value.continuityArtifactReference.copy(artifactId = InternalModelArtifactId.from("projection-other").toOption.get))),
                "realization-id" -> (value => value.copy(realizationArtifactReference = value.realizationArtifactReference.copy(artifactId = InternalModelArtifactId.from("realization-other").toOption.get))),
                "diff-id" -> (value => value.copy(semanticDiffArtifactReference = value.semanticDiffArtifactReference.copy(artifactId = InternalModelArtifactId.from("semantic-diff-other").toOption.get))),
                "candidate-revision" -> (value => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(18L).toOption.get))),
        "candidate-role" -> (value => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(role = InternalModelArtifactRole.Realization))),
        "continuity-revision" -> (value => value.copy(continuityArtifactReference = value.continuityArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(30L).toOption.get))),
        "continuity-role" -> (value => value.copy(continuityArtifactReference = value.continuityArtifactReference.copy(role = InternalModelArtifactRole.Realization))),
        "realization-revision" -> (value => value.copy(realizationArtifactReference = value.realizationArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(14L).toOption.get))),
        "realization-role" -> (value => value.copy(realizationArtifactReference = value.realizationArtifactReference.copy(role = InternalModelArtifactRole.Projection))),
        "diff-artifact-revision" -> (value => value.copy(semanticDiffArtifactReference = value.semanticDiffArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(32L).toOption.get))),
        "diff-role" -> (value => value.copy(semanticDiffArtifactReference = value.semanticDiffArtifactReference.copy(role = InternalModelArtifactRole.Realization))),
        "evidence-revision" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts.updated(0, value.evidenceArtifacts.head.copy(artifactRevision = InternalModelArtifactRevision.from(60L).toOption.get)))),
        "evidence-selects-review" -> (value => value.copy(evidenceArtifacts = Vector(_artifact_reference("review-main")), targets = value.targets.map(_.copy(evidenceArtifactIds = Vector("review-main"))))),
        "realization-identity" -> (value => value.copy(realizationReference = _record_reference("realization-other", 19L))),
        "candidate-identity" -> (value => value.copy(candidateReference = _record_reference("candidate-other", 7L))),
        "candidate-model" -> (value => value.copy(candidateModelIdentity = "candidate-model-other")),
        "candidate-logical-revision" -> (value => value.copy(candidateReference = _record_reference("candidate-order-v1", 8L))),
        "diff-identity" -> (value => value.copy(semanticDiffReference = _record_reference("semantic-diff-other", 3L))),
        "diff-revision" -> (value => value.copy(semanticDiffReference = _record_reference("semantic-diff-order", 8L))),
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
        "evidence-id" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts.updated(0, value.evidenceArtifacts.head.copy(artifactId = InternalModelArtifactId.from("evidence-other").toOption.get)))),
                "evidence-missing" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts.dropRight(1))),
        "evidence-role" -> (value => value.copy(evidenceArtifacts = Vector(_artifact_reference("candidate-main")), targets = value.targets.map(_.copy(evidenceArtifactIds = Vector("candidate-main"))))),
        "evidence-dangling" -> (value => value.copy(evidenceArtifacts = Vector(_new_artifact_reference("evidence-dangling", InternalModelArtifactRole.Validation)), targets = value.targets.map(_.copy(evidenceArtifactIds = Vector("evidence-dangling"))))),
        "evidence-unused" -> (value => value.copy(evidenceArtifacts = value.evidenceArtifacts :+ _new_artifact_reference("evidence-unused", InternalModelArtifactRole.Validation))),
        "target-local-duplicate-evidence" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(evidenceArtifactIds = Vector("evidence-alpha", "evidence-alpha"))))),
        "evidence-coverage" -> (value => value.copy(targets = value.targets.updated(0, value.targets.head.copy(evidenceArtifactIds = Vector("evidence-beta")))))
      )

      When("each mutation is encoded into the selected review artifact and admitted through validate")
      val results = changes.map { case (name, change) => name -> _validate(change) }

      Then("the truthful control is the only admission and every selected mismatch rejects independently")
      results.forall(_._2 == false) shouldBe true
    }

    "reject independent rule/provider IDs versions missing duplicate order null and malformed basis values" in {
      Given("a valid package plus independently malformed caller-admitted execution bases")
      val valid = _basis()
      val cases = Vector(
        "missing-rules" -> valid.copy(rules = Vector.empty),
        "missing-providers" -> valid.copy(providers = Vector.empty),
        "rule-id" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleId = "rule-other"))),
        "second-rule-id" -> valid.copy(rules = valid.rules.updated(1, valid.rules(1).copy(ruleId = "rule-other"))),
        "rule-version" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleVersion = "2.0"))),
        "second-rule-version" -> valid.copy(rules = valid.rules.updated(1, valid.rules(1).copy(ruleVersion = "3.0"))),
        "provider-id" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(providerId = "provider-other"))),
        "second-provider-id" -> valid.copy(providers = valid.providers.updated(1, valid.providers(1).copy(providerId = "provider-other"))),
        "provider-version" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(providerVersion = "2.0"))),
        "second-provider-version" -> valid.copy(providers = valid.providers.updated(1, valid.providers(1).copy(providerVersion = "3.0"))),
        "duplicate-rule" -> valid.copy(rules = Vector(valid.rules.head, valid.rules.head)),
        "duplicate-provider" -> valid.copy(providers = Vector(valid.providers.head, valid.providers.head)),
        "unordered-rules" -> valid.copy(rules = valid.rules.reverse),
        "unordered-provider" -> valid.copy(providers = valid.providers.reverse),
        "malformed-rule" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleId = " "))),
        "blank-rule-version" -> valid.copy(rules = valid.rules.updated(0, valid.rules.head.copy(ruleVersion = " "))),
        "blank-provider-id" -> valid.copy(providers = valid.providers.updated(0, valid.providers.head.copy(providerId = " "))),
        "null-basis" -> null,
        "null-rules" -> valid.copy(rules = null),
        "null-providers" -> valid.copy(providers = null),
        "null-rule-entry" -> valid.copy(rules = Vector(null)),
        "null-provider-entry" -> valid.copy(providers = Vector(null)),
        "null-rule-id" -> valid.copy(rules = Vector(valid.rules.head.copy(ruleId = null))),
        "null-rule-version" -> valid.copy(rules = Vector(valid.rules.head.copy(ruleVersion = null))),
        "null-provider-id" -> valid.copy(providers = Vector(valid.providers.head.copy(providerId = null))),
        "null-provider-version" -> valid.copy(providers = Vector(valid.providers.head.copy(providerVersion = null))),
        "blank-provider-version" -> valid.copy(providers = Vector(valid.providers.head.copy(providerVersion = " "))),
        "invalid-rule-unicode" -> valid.copy(rules = Vector(valid.rules.head.copy(ruleVersion = "\uD800"))),
        "invalid-provider-unicode" -> valid.copy(providers = Vector(valid.providers.head.copy(providerId = "\uDC00")))
      )

      When("the actual package is validated against each expected basis")
      val results = cases.map { case (name, basis) => name -> _validate(identity, FixtureOptions(), basis) }

      Then("only the complete exact ordered basis can be admitted")
      results.forall(_._2 == false) shouldBe true
    }
  }


  "Candidate-review captured handoff and current predecessor" should {
    "single-capture portability" which {
      "admit only current V3 realization continuity and reject both old families" in {
        Given("a current V3 control and explicit old V1/V2 profile/schema claims")
        val families = Vector(
          ("ccdm-projection-binding-v3", "3.0", "ccdm-realization-v3", "3.0"),
          ("ccdm-projection-binding-v1", "1.0", "ccdm-realization-v3", "3.0"),
          ("ccdm-projection-binding-v2", "2.0", "ccdm-realization-v3", "3.0"),
          ("ccdm-projection-binding-v3", "3.0", "ccdm-realization-v1", "1.0"),
          ("ccdm-projection-binding-v3", "3.0", "ccdm-realization-v2", "2.0")
        )
        When("each real predecessor family crosses complete review admission")
        val results = families.map { case (profile, schema, realizationprofile, realizationschema) =>
          _validate(identity, FixtureOptions(continuityprofile = profile, continuityschema = schema, realizationprofile = realizationprofile, realizationschema = realizationschema), proveupstream = false)
        }
        Then("the current control succeeds and old families reject without compatibility")
        results shouldBe Vector(true, false, false, false, false)
      }

      "retain raw binary CML and all eight projection sidecar families" in {
        Given("current realization assertions conditions and a two-target candidate whose alpha proposal is raw binary")
        _with_fixture() { root =>
          _prove_upstream(root)
          When("the complete review is admitted over reconstructed continuity")
          val result = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
          Then("ordinary binary payload and every projection/sidecar family remain retained")
          val candidate = result.toOption.get.semanticDiffAdmission.candidateAdmission
          candidate.targetBytes.map(_.proposedRawBytes) shouldBe Vector(Vector[Byte](0, -1, 10), _cml_beta_raw)
          candidate.continuity.binding.views.map(_.family) shouldBe Vector("MonoKotoProjection", "UseCaseCommunicationProjection", "EntityModelProjection", "EventModelProjection", "StructureViewProjection", "ClassificationViewProjection", "WorkflowProjection", "StateMachineProjection")
          Vector[Any](candidate.continuity.monoKoto, candidate.continuity.useCaseCommunication, candidate.continuity.entityModel, candidate.continuity.eventModel, candidate.continuity.structureView, candidate.continuity.classificationView, candidate.continuity.workflow, candidate.continuity.stateMachine).forall(_ != null) shouldBe true
          candidate.continuity.realization.conditions.map(_.conditionId) shouldBe Vector("c-opaque-element", "c-opaque-relationship")
          candidate.continuity.realization.enrichmentAssertions.map(_.assertionId) shouldBe Vector("z-opaque-element-enrichment", "z-opaque-relationship-enrichment")
        }
      }

      "preserve unknown source version independently of explicit artifact revision" in {
        Given("current source snapshots whose source-owned alpha revision is explicitly absent")
        _with_fixture(FixtureOptions(missingsourceversion = true)) { root =>
          _prove_upstream(root)
          When("review admission reconstructs that source evidence")
          val result = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
          Then("the source version remains unknown while the separate artifact version is retained")
          val baseline = result.toOption.get.semanticDiffAdmission.candidateAdmission.targetBytes.head.baseline
          baseline.source.revision shouldBe None
          baseline.reference.artifactRevision.value shouldBe 11L
        }
      }

      "admit a retained capture after actual fixture paths disappear while fresh admission fails" in {
        Given("one complete verified review capture whose private paths can be removed")
        _with_fixture() { root =>
          _prove_upstream(root)
          val handoff = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, _artifact_reference("review-main")).toOption.get
          When("the fixture package and project paths disappear before pure captured admission")
          _delete_tree(root.resolve("src/main/internal-model"))
          Files.delete(root.resolve("project.yaml"))
          val captured = InternalModelCandidateReviewBindingValidator.validateVerified(handoff, _basis())
          val fresh = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
          Then("the capture retains IDs subject evidence projections and binary CML while a new read fails")
          captured.toOption.map(_.binding) shouldBe Some(_binding_value())
          captured.toOption.get.semanticDiffAdmission.candidateAdmission.targetBytes.map(_.proposedRawBytes) shouldBe Vector(Vector[Byte](0, -1, 10), _cml_beta_raw)
          fresh.isSuccess shouldBe false
        }
      }

      "admit an actual directory copy at an independent root" in {
        Given("a current private fixture and a separately allocated task-private destination")
        _with_fixture() { root =>
          _prove_upstream(root)
          val destination = _temporary_root()
          try {
            _copy_tree(root, destination)
            When("both independent roots receive the same external review reference and independent basis")
            val original = InternalModelCandidateReviewBindingValidator.validate(root, _artifact_reference("review-main"), _basis())
            val copied = InternalModelCandidateReviewBindingValidator.validate(destination, _artifact_reference("review-main"), _basis())
            Then("typed identity scope evidence projections and relative paths survive actual relocation")
            original.isSuccess shouldBe true
            copied.toOption shouldBe original.toOption
          } finally _delete_tree(destination)
        }
      }

      "reject malformed null capture metadata without exceptions" in {
        Given("one current captured handoff and independently absent outer carrier candidate continuity review or selected metadata")
        _with_fixture() { root =>
          _prove_upstream(root)
          val handoff = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, _artifact_reference("review-main")).toOption.get
          val diff = handoff.semanticDiffPackage
          val candidate = diff.candidatepackage
          val continuity = candidate.continuitypackage
          val variants = Vector(
            null,
            handoff.copy(carrierPackageContext = null),
            handoff.copy(semanticDiffPackage = null),
            handoff.copy(reviewArtifact = null),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = null)),
            handoff.copy(semanticDiffPackage = diff.copy(semanticdiff = null)),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(packagecontext = null))),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(candidate = null))),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(continuitypackage = null))),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(continuitypackage = continuity.copy(realizationpackage = null)))),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(continuitypackage = continuity.copy(projection = null)))),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(continuitypackage = continuity.copy(realizationpackage = continuity.realizationpackage.copy(realization = null))))),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(continuitypackage = continuity.copy(realizationpackage = continuity.realizationpackage.copy(sourcesnapshots = null))))),
            handoff.copy(semanticDiffPackage = diff.copy(candidatepackage = candidate.copy(continuitypackage = continuity.copy(realizationpackage = continuity.realizationpackage.copy(sourcesnapshots = Vector(null)))))),
            handoff.copy(reviewArtifact = handoff.reviewArtifact.copy(reference = null)),
            handoff.copy(reviewArtifact = handoff.reviewArtifact.copy(path = null)),
            handoff.copy(reviewArtifact = handoff.reviewArtifact.copy(dependencies = null)),
            handoff.copy(reviewArtifact = handoff.reviewArtifact.copy(bytes = null)),
            handoff.copy(carrierPackageContext = handoff.carrierPackageContext.copy(artifacts = null)),
            handoff.copy(carrierPackageContext = handoff.carrierPackageContext.copy(artifacts = Vector(null)))
          )
          When("each absent capture crosses pure admission")
          val results = variants.map(value => InternalModelCandidateReviewBindingValidator.validateVerified(value, _basis()).isSuccess)
          Then("every malformed capture yields a structured failure without an escaping exception")
          results shouldBe Vector.fill(variants.size)(false)
        }
      }

      "reject mismatched selected capture reference path required dependencies and carrier metadata" in {
        Given("one current admitted upstream capture and independently altered review capture metadata")
        _with_fixture() { root =>
          _prove_upstream(root)
          val handoff = InternalModelPackageValidator.verifiedCandidateReviewBinding(root, _artifact_reference("review-main")).toOption.get
          val review = handoff.reviewArtifact
          val variants = Vector(
            handoff.copy(reviewArtifact = review.copy(reference = review.reference.copy(artifactRevision = InternalModelArtifactRevision.from(103L).toOption.get))),
            handoff.copy(reviewArtifact = review.copy(path = "other/review.json")),
            handoff.copy(reviewArtifact = review.copy(required = !review.required)),
            handoff.copy(reviewArtifact = review.copy(dependencies = review.dependencies.dropRight(1))),
            handoff.copy(carrierPackageContext = handoff.carrierPackageContext.copy(revision = 113L))
          )
          variants.foreach(_prove_capture)
          When("each mismatched selected capture crosses pure review admission")
          val results = variants.map(value => InternalModelCandidateReviewBindingValidator.validateVerified(value, _basis()).isSuccess)
          Then("all selected metadata must remain exact and the carrier must be the candidate's same capture")
          results shouldBe Vector.fill(variants.size)(false)
        }
      }
    }
  }

  private def _validate(
    change: InternalModelCandidateReviewBinding => InternalModelCandidateReviewBinding,
    options: FixtureOptions = FixtureOptions(),
    basis: InternalModelCandidateReviewExecutionBasis = _basis(),
    proveupstream: Boolean = true
  ): Boolean = {
    var succeeded = false
    _with_fixture(options.copy(bindingchange = change)) { root =>
      Given("an independently captured current package/candidate/diff/continuity for this review-specific variant")
      if (proveupstream) _prove_upstream(root)
      When("the supplied review and caller basis cross review admission")
      succeeded = InternalModelCandidateReviewBindingValidator.validate(root, options.selectedreview, basis).isSuccess
    }
    succeeded
  }

  private def _prove_upstream(root: Path): Unit = {
    InternalModelPackageValidator.validateStructure(root).isSuccess shouldBe true
    InternalModelProjectionContinuityValidator.validate(root).isSuccess shouldBe true
    InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess shouldBe true
    InternalModelSemanticDiffValidator.validate(root).isSuccess shouldBe true
  }

  private def _prove_capture(handoff: InternalModelVerifiedCandidateReviewBindingPackage): Unit = {
    InternalModelProjectionContinuityValidator.validateVerified(handoff.semanticDiffPackage.candidatepackage.continuitypackage).isSuccess shouldBe true
    InternalModelCandidateCmlProjectionValidator.validateVerified(handoff.semanticDiffPackage.candidatepackage).isSuccess shouldBe true
    InternalModelSemanticDiffValidator.validateVerified(handoff.semanticDiffPackage).isSuccess shouldBe true
  }

  private def _carrier_handoff(
    handoff: InternalModelVerifiedCandidateReviewBindingPackage,
    entries: Vector[InternalModelVerifiedArtifactContext],
    binding: InternalModelCandidateReviewBinding,
    dependencies: Vector[InternalModelArtifactReference]
  ): InternalModelVerifiedCandidateReviewBindingPackage = {
    val review = handoff.reviewArtifact.copy(dependencies = dependencies, bytes = InternalModelCandidateReviewBindingCodec.encode(binding).toVector)
    val updated = entries.map(entry => if entry.reference.artifactId == review.reference.artifactId then entry.copy(dependencies = dependencies) else entry)
    val encounter = _topological(updated.map(entry => _entry(entry.reference, entry.path, entry.required, entry.dependencies)))
      .map(_.hcursor.get[String]("artifactId").toOption.get).zipWithIndex.toMap
    val carrier = handoff.carrierPackageContext.copy(artifacts = updated.sortBy(entry => encounter(entry.reference.artifactId.value)))
    handoff.copy(
      carrierPackageContext = carrier,
      semanticDiffPackage = handoff.semanticDiffPackage.copy(candidatepackage = handoff.semanticDiffPackage.candidatepackage.copy(packagecontext = carrier)),
      reviewArtifact = review
    )
  }

  private def _with_fixture(options: FixtureOptions = FixtureOptions())(action: Path => Unit): Unit = {
    val source = if options.missingsourceversion then _cml_alpha_source.copy(revision = None) else _cml_alpha_source
    val initial = _projection()
    val projection = initial.copy(targets = initial.targets.updated(0, initial.targets.head.copy(source = source)))
    val candidate = InternalModelCandidateCmlProjectionCodec.encode(projection)
    val diff = InternalModelSemanticDiffCodec.encode(_semantic_diff(projection))
    val binding = options.bindingchange(_binding_value(reviewstate = options.reviewstate, allnullablefacets = options.allnullablefacets))
    val reviewbytes = options.reviewbyteschange(InternalModelCandidateReviewBindingCodec.encode(binding))
    val basefiles = Vector(
      ("evidence-alpha", "evidence/alpha.json", "sanitized validation evidence alpha\n".getBytes(StandardCharsets.UTF_8), Vector.empty[String]),
      ("evidence-beta", "evidence/beta.json", "sanitized validation evidence beta\n".getBytes(StandardCharsets.UTF_8), Vector.empty[String]),
      ("snapshot-cml-alpha", "snapshots/cml-alpha.json", _cml_snapshot(source, "cml/alpha.cml", _cml_alpha_raw), Vector.empty[String]),
      ("snapshot-cml-beta", "snapshots/cml-beta.json", _cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw), Vector.empty[String]),
      ("snapshot-model", "snapshots/model.json", _model_snapshot(), Vector.empty[String]),
      ("realization-main", "realizations/main.json", _realization(options.realizationprofile, options.realizationschema), Vector("snapshot-model")),
      ("decision-main", "decisions/main.json", "raw decision record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      ("open-issue-main", "open-issues/main.json", "raw open issue record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      ("projection-main", "projections/continuity.json", _binding(options.continuityprofile, options.continuityschema), Vector("realization-main")),
      ("resume-main", "resumes/main.json", "raw resume record\n".getBytes(StandardCharsets.UTF_8), Vector("realization-main")),
      ("candidate-main", "projections/candidate.json", candidate, Vector("projection-main", "realization-main", "snapshot-cml-alpha", "snapshot-cml-beta")),
      ("semantic-diff-main", "projections/semantic-diff.json", diff, Vector("candidate-main"))
    )
    val baseentries = basefiles.map { case (id, path, _, dependencies) => _entry(_artifact_reference(id), path, true, dependencies.map(_artifact_reference)) } :+
      _entry(_artifact_reference("snapshot-optional"), "snapshots/optional.json", false, Vector.empty)
    val reviewreference = _artifact_reference("review-main").copy(role = options.reviewrole)
    val entries = baseentries ++
      Option.when(options.includereview)(_entry(reviewreference, "reviews/candidate-review.json", true, options.reviewdependencies.getOrElse(binding.subject.artifacts))).toVector ++
      Option.when(options.includeoptionalreview)(_entry(_artifact_reference("review-optional-absent"), "reviews/optional-review.json", false, Vector.empty)).toVector ++
      Option.when(options.includeapproval)(_entry(_artifact_reference("approval-extra"), "carrier/approval-extra.json", false, Vector.empty)).toVector ++
      options.extraartifact.map { case (id, role, path, _) => _entry(_new_artifact_reference(id, role), path, true, Vector.empty) }.toVector
    val encountered = if options.permuteencounter then entries.reverse else entries
    val root = _temporary_root()
    try {
      _write(root.resolve("project.yaml"), _project_yaml.getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(_topological(encountered), options.carrierrevision))
      basefiles.foreach { case (_, path, bytes, _) => _write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      if (options.includeapproval) _write(root.resolve("src/main/internal-model/carrier/approval-extra.json"), "inert approval metadata\n".getBytes(StandardCharsets.UTF_8))
      if (options.includereview) _write(root.resolve("src/main/internal-model/reviews/candidate-review.json"), reviewbytes)
      options.extraartifact.foreach { case (_, _, path, bytes) => _write(root.resolve("src/main/internal-model").resolve(path), bytes) }
      options.missingrequiredpath.foreach(path => Files.deleteIfExists(root.resolve("src/main/internal-model").resolve(path)))
      action(root)
    } finally _delete_tree(root)
  }

  private def _binding_value(reviewstate: String = "reviewed", allnullablefacets: Boolean = false): InternalModelCandidateReviewBinding = {
    val projection = _projection()
    val basis = _basis()
    val artifacts = _sorted(Vector("candidate-main", "decision-main", "open-issue-main", "evidence-alpha", "evidence-beta", "projection-main", "realization-main", "semantic-diff-main", "snapshot-cml-alpha", "snapshot-cml-beta", "snapshot-model").map(_artifact_reference))
    val packagereference = InternalModelPackageReference(InternalModelPackageId.from(_package_id).toOption.get, InternalModelProjectToken.from("org.example").toOption.get, InternalModelProjectToken.from("candidate-review-sample").toOption.get)
    InternalModelCandidateReviewBinding(
      _artifact_reference("candidate-main"), projection.candidateReference, projection.candidateModelIdentity,
      _artifact_reference("projection-main"), Vector(_artifact_reference("evidence-alpha"), _artifact_reference("evidence-beta")),
      "ccdm-candidate-review-binding-v2", basis.providers, _artifact_reference("realization-main"), _record_reference("realization-order", 19L),
      _record_reference("review-identity", 3L), basis.rules, "2.0", projection.scope,
      _artifact_reference("semantic-diff-main"), _record_reference("semantic-diff-order", 5L),
      InternalModelReviewSubject(InternalModelRecordId.from("subject-order").toOption.get, InternalModelRecordRevision.from(101L).toOption.get, packagereference, projection.scope, artifacts),
      Vector(_review_target("target-alpha", "patch-alpha", "evidence-alpha", reviewstate, allnullablefacets), _review_target("target-beta", "patch-beta", "evidence-beta", reviewstate, allnullablefacets))
    )
  }

  private def _basis(): InternalModelCandidateReviewExecutionBasis =
    InternalModelCandidateReviewExecutionBasis(
      Vector(InternalModelCandidateReviewRule("rule-alpha", "1.0"), InternalModelCandidateReviewRule("rule-beta", "2.0")),
      Vector(InternalModelCandidateReviewProvider("provider-alpha", "1.0"), InternalModelCandidateReviewProvider("provider-beta", "2.0"))
    )

  private def _artifact_reference(id: String): InternalModelArtifactReference = {
    val (revision, role) = _artifact_versions(id)
    InternalModelArtifactReference(InternalModelArtifactId.from(id).toOption.get, InternalModelArtifactRevision.from(revision).toOption.get, role)
  }

  private def _new_artifact_reference(id: String, role: InternalModelArtifactRole): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from(id).toOption.get, InternalModelArtifactRevision.from(127L).toOption.get, role)

  private def _record_reference(id: String, revision: Long): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(id).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)

  private def _artifact_json(reference: InternalModelArtifactReference): Json =
    io.circe.parser.parse(_json_text(InternalModelTypedControlCodec.encodeArtifactReference(reference).toArray)).toOption.get

  private def _record_json(reference: InternalModelRecordReference): Json =
    io.circe.parser.parse(_json_text(InternalModelTypedControlCodec.encodeRecordReference(reference).toArray)).toOption.get

  private def _sorted(values: Vector[InternalModelArtifactReference]): Vector[InternalModelArtifactReference] =
    values.sortWith((left, right) => Arrays.compareUnsigned(left.artifactId.value.getBytes(StandardCharsets.UTF_8), right.artifactId.value.getBytes(StandardCharsets.UTF_8)) < 0)

  private def _entry(reference: InternalModelArtifactReference, path: String, required: Boolean, dependencies: Vector[InternalModelArtifactReference]): Json =
    Json.obj("artifactId" -> Json.fromString(reference.artifactId.value), "artifactRevision" -> Json.fromLong(reference.artifactRevision.value), "dependsOn" -> Json.fromValues(dependencies.map(_artifact_json)), "path" -> Json.fromString(path), "required" -> Json.fromBoolean(required), "role" -> Json.fromString(reference.role.wireValue))

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

  private def _manifest(entries: Vector[Json], revision: Long): Array[Byte] =
    _json_bytes(Json.obj("artifacts" -> Json.fromValues(entries), "lifecycleState" -> Json.fromString("draft"), "packageId" -> Json.fromString(_package_id), "projectId" -> Json.fromString("candidate-review-sample"), "projectNamespace" -> Json.fromString("org.example"), "revision" -> Json.fromLong(revision), "schemaVersion" -> Json.fromString("2.0")))

  private def _binding_json(value: InternalModelCandidateReviewBinding): Json =
    io.circe.parser.parse(_json_text(InternalModelCandidateReviewBindingCodec.encode(value))).toOption.get

  private def _at_path(value: Json, path: Vector[String]): Json = {
    if (path.isEmpty) value
    else value.asObject match {
      case Some(fields) => _at_path(fields(path.head).get, path.tail)
      case None => _at_path(value.asArray.get(path.head.toInt), path.tail)
    }
  }

  private def _replace_path(value: Json, path: Vector[String], change: Json => Json): Json = {
    if (path.isEmpty) change(value)
    else value.asObject match {
      case Some(fields) => Json.fromJsonObject(fields.add(path.head, _replace_path(fields(path.head).get, path.tail, change)))
      case None =>
        val values = value.asArray.get
        Json.fromValues(values.updated(path.head.toInt, _replace_path(values(path.head.toInt), path.tail, change)))
    }
  }

  private def _object_paths(value: Json, path: Vector[String] = Vector.empty): Vector[Vector[String]] =
    value.asObject match {
      case Some(fields) => Vector(path) ++ fields.toVector.flatMap { case (key, item) => _object_paths(item, path :+ key) }
      case None => value.asArray.map(_.zipWithIndex.flatMap { case (item, index) => _object_paths(item, path :+ index.toString) }.toVector).getOrElse(Vector.empty)
    }

  private def _reverse_keys(value: Json): Json =
    value.asObject match {
      case Some(fields) => Json.fromJsonObject(JsonObject.fromIterable(fields.toVector.reverse.map { case (key, item) => key -> _reverse_keys(item) }))
      case None => value.asArray.map(values => Json.fromValues(values.map(_reverse_keys))).getOrElse(value)
    }

  private def _duplicate_member(bytes: Array[Byte], member: String): Array[Byte] = {
    val text = _json_text(bytes)
    val token = "\"" + member + "\":"
    val start = text.indexOf(token)
    val colon = start + token.length
    val valueend = text.indexOf('"', colon + 1) + 1
    val field = text.substring(start, valueend)
    (text.substring(0, valueend) + "," + field + text.substring(valueend)).getBytes(StandardCharsets.UTF_8)
  }

  private def _temporary_root(): Path = {
    Files.createDirectories(_fixture_parent)
    Files.createTempDirectory(_fixture_parent, "fixture-")
  }

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _copy_tree(source: Path, destination: Path): Unit = {
    val stream = Files.walk(source)
    try stream.iterator.asScala.foreach { path =>
      val target = destination.resolve(source.relativize(path).toString)
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(target)
      else Files.copy(path, target, LinkOption.NOFOLLOW_LINKS)
    } finally stream.close()
  }

  private def _delete_tree(root: Path): Unit = {
    require(root.toAbsolutePath.normalize.startsWith(_fixture_parent.toAbsolutePath.normalize) && root.toAbsolutePath.normalize != _fixture_parent.toAbsolutePath.normalize)
    if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      val stream = Files.walk(root)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.deleteIfExists(path))
      finally stream.close()
    }
  }

  private def _json_text(bytes: Array[Byte]): String = new String(bytes, StandardCharsets.UTF_8)
  private def _json_bytes(value: Json): Array[Byte] = (_printer.print(value) + "\n").getBytes(StandardCharsets.UTF_8)
  private def _canonical(value: Json): Array[Byte] = _json_bytes(value)
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

  private def _source(authority: String, identity: String, locator: Option[String], revision: Option[String]): InternalModelSemanticSource = InternalModelSemanticSource(authority, identity, locator, revision)
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority), "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null))
  private def _scope_json: Json = Json.obj("componentIdentity" -> Json.fromString("component-order"), "projectionContextIdentity" -> Json.fromString("context-order"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase"))

  private def _model_snapshot(): Array[Byte] = {
    val facts = Vector(_fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono"), _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case"), _fact("element", "opaque-shared", "anchor-opaque-element-enrichment", "enrichment:shared"), _fact("element", "opaque-shared", "anchor-opaque-element-kind-Mono", "kind:Mono"), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-enrichment", "enrichment:shared"), _fact("relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain", "role:StructuralDomain"), _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain")).sortBy(value => (value.hcursor.get[String]("semanticIdentityKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("sourceAnchor").toOption.get))
    _canonical(Json.obj("basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)), "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source_json(_model_source)))
  }
  private def _cml_snapshot(source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte]): Array[Byte] = _canonical(Json.obj("basis" -> Json.obj("byteLength" -> Json.fromLong(rawbytes.length.toLong), "projectRelativePath" -> Json.fromString(path), "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes.toArray))), "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("cml-baseline"), "source" -> _source_json(source)))

  private def _projection(): InternalModelCandidateCmlProjection = {
    val alpha = _target("snapshot-cml-alpha", "patch-alpha", "target-alpha", _cml_alpha_source, "cml/alpha.cml", Vector[Byte](0, -1, 10), "mapping-alpha", "opaque-shared", "element", "effect-alpha", "ref-opaque-element-kind-Mono")
    val beta = _target("snapshot-cml-beta", "patch-beta", "target-beta", _cml_beta_source, "cml/beta.cml", _cml_beta_raw, "mapping-beta", "opaque-shared", "relationship", "effect-beta", "ref-opaque-relationship-role-StructuralDomain")
    InternalModelCandidateCmlProjection(_record_reference("candidate-order-v1", 7L), "candidate-model-order", _artifact_reference("projection-main"), "ccdm-candidate-cml-projection-v2", _artifact_reference("realization-main"), "2.0", InternalModelSemanticScope("component-order", "context-order", "e-usecase"), Vector(alpha, beta))
  }
  private def _target(baselineid: String, patchid: String, targetid: String, source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte], mappingid: String, identity: String, kind: String, effectprefix: String, referenceid: String): InternalModelCandidateCmlTarget = { val assertionid = if kind == "element" then "a-opaque-element-kind-Mono" else "a-opaque-relationship-role-StructuralDomain"; val conditionid = if kind == "element" then "c-opaque-element" else "c-opaque-relationship"; val enrichmentid = if kind == "element" then "z-opaque-element-enrichment" else "z-opaque-relationship-enrichment"; val mapping = InternalModelCandidateCmlMapping(Vector(assertionid), "anchor-" + targetid, "cml-" + targetid, Vector(conditionid), Vector(enrichmentid), mappingid, identity, kind); val effects = Vector(InternalModelCandidateCmlEffect("unknown", "supplied compatibility expectation", effectprefix + "-compatibility", "compatibility", Vector(mappingid), referenceid), InternalModelCandidateCmlEffect("unknown", "supplied migration expectation", effectprefix + "-migration", "migration", Vector(mappingid), referenceid)); InternalModelCandidateCmlTarget(_artifact_reference(baselineid), effects, Vector(mapping), patchid, path, InternalModelCandidateCmlContent(_record_reference("content-" + targetid, if targetid == "target-alpha" then 131L else 137L), rawbytes.length.toLong, Base64.getEncoder.encodeToString(rawbytes.toArray)), source, targetid) }

  private def _realization(profile: String, schema: String): Array[Byte] = {
    val v2 = true
    val assertions = Vector(_assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", Vector.empty, v2), _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", Vector.empty, v2), _assertion("a-opaque-element-kind-Mono", "element", "opaque-shared", "ref-opaque-element-kind-Mono", "kind:Mono", Vector("c-opaque-element"), v2), _assertion("a-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "ref-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", Vector("c-opaque-relationship"), v2), _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", Vector.empty, v2)).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val enrichment = Vector(_assertion("z-opaque-element-enrichment", "element", "opaque-shared", "ref-opaque-element-enrichment", "enrichment:shared", Vector("c-opaque-element"), v2), _assertion("z-opaque-relationship-enrichment", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "enrichment:shared", Vector("c-opaque-relationship"), v2)).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    _canonical(Json.obj("canonicalAssertions" -> Json.fromValues(assertions), "conditions" -> Json.fromValues(Vector(_condition_json("c-opaque-element", "limitation", "element", "opaque-shared", "ref-opaque-element-enrichment", "element evidence remains bounded"), _condition_json("c-opaque-relationship", "limitation", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "relationship evidence remains bounded"))), "elements" -> Json.fromValues(Vector(_element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")), _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")), _element("opaque-shared", "Mono", "Opaque shared element", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element")))), "enrichmentAssertions" -> Json.fromValues(enrichment), "profile" -> Json.fromString(profile), "realizationReference" -> _record_json(_record_reference("realization-order", 19L)), "relationships" -> Json.fromValues(Vector(_relationship("opaque-shared", "StructuralDomain", "e-mono", "e-usecase", Vector("a-opaque-relationship-role-StructuralDomain"), Vector("z-opaque-relationship-enrichment"), Vector("c-opaque-relationship")), _relationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase", Vector("a-r-mono-domain-role-StructuralDomain")))), "schemaVersion" -> Json.fromString(schema), "scope" -> _scope_json, "sourceReferences" -> Json.fromValues(Vector(_reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"), _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"), _reference("ref-opaque-element-enrichment", "element", "opaque-shared", "anchor-opaque-element-enrichment"), _reference("ref-opaque-element-kind-Mono", "element", "opaque-shared", "anchor-opaque-element-kind-Mono"), _reference("ref-opaque-relationship-enrichment", "relationship", "opaque-shared", "anchor-opaque-relationship-enrichment"), _reference("ref-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain"), _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain")).sortBy(_.hcursor.get[String]("referenceId").toOption.get)), "successorLinks" -> Json.arr(), "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(_artifact_json(_artifact_reference("snapshot-model"))))))
  }

  private def _binding(profile: String, schema: String): Array[Byte] = {
    def _record_(kind: String, identity: String, role: String, ids: Vector[String]): Json = Json.obj("canonicalAssertionIds" -> Json.fromValues(ids.map(Json.fromString)), "conditionIds" -> Json.arr(), "enrichmentAssertionIds" -> Json.arr(), "recordKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "sequenceAssertionId" -> Json.Null, "viewRole" -> Json.fromString(role))
    val families = Vector("MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono")), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))), "UseCaseCommunicationProjection" -> Vector(_record_("element", "e-usecase", "use-case", Vector("a-e-usecase-kind-use-case"))), "EntityModelProjection" -> Vector.empty[Json], "EventModelProjection" -> Vector.empty[Json], "StructureViewProjection" -> Vector.empty[Json], "ClassificationViewProjection" -> Vector.empty[Json], "WorkflowProjection" -> Vector.empty[Json], "StateMachineProjection" -> Vector.empty[Json])
    _canonical(Json.obj("profile" -> Json.fromString(profile), "bindingReference" -> _record_json(_record_reference("binding-order", 23L)), "realizationArtifactReference" -> _artifact_json(_artifact_reference("realization-main")), "schemaVersion" -> Json.fromString(schema), "scope" -> _scope_json, "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })))
  }

  private def _semantic_diff(projection: InternalModelCandidateCmlProjection): InternalModelSemanticDiff = {
    val targets = projection.targets.map { target =>
      val mapping = target.mappings.head
      val condition = _condition("available", "authorized", None, None, None, None, None, None, Vector("condition limitation"))
      val entry = CandidateDesignSemanticDiffEntry("entry-" + target.targetId, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.patchIdentity, projection.candidateModelIdentity, "category", "action", mapping.semanticIdentity, Some("before"), Some("after"), "relationship", _attribution("entry-authority-" + target.targetId), condition, Vector("entry limitation"), Some("entry tie"))
      val patch = CandidateDesignProposedCmlPatchTrace(target.patchIdentity, MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity), ComponentDashboardComponentIdentity(projection.scope.componentIdentity), target.source.authority, target.source.locator.getOrElse("patch-locator-" + target.targetId), target.baselineArtifactReference, target.proposedContent.contentReference, _attribution("patch-authority-" + target.targetId), condition, Vector("patch limitation"), Some("patch tie"))
      InternalModelSemanticDiffTarget(Vector(InternalModelSemanticDiffMappedEntry(entry, mapping.mappingId, mapping.semanticIdentityKind)), patch, target.targetId)
    }
    InternalModelSemanticDiff(_artifact_reference("candidate-main"), projection.candidateReference, projection.candidateModelIdentity, "ccdm-semantic-diff-v2", "2.0", projection.scope, _record_reference("semantic-diff-order", 5L), targets)
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
  private def _reference(id: String, kind: String, identity: String, anchor: String): Json = Json.obj("referenceId" -> Json.fromString(id), "snapshotReference" -> _artifact_json(_artifact_reference("snapshot-model")), "source" -> _source_json(_model_source), "sourceAnchor" -> Json.fromString(anchor), "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind)))
}
