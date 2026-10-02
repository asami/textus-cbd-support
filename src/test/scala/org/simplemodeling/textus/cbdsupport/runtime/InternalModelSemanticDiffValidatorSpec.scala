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
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelSemanticDiffValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _cml_alpha_raw = Vector[Byte](0, 0x7f, -1, 10)
  private val _cml_beta_raw = "Domain { Order }\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _model_source = _source("model-authority", "model-source", Some("catalog/model-source"), Some("revision-1"))
  private val _cml_alpha_source = _source("cml-authority", "cml-alpha", Some("cml/alpha.cml"), Some("revision-a"))
  private val _cml_beta_source = _source("cml-authority", "cml-beta", Some("cml/beta.cml"), Some("revision-b"))

  "Internal-model semantic-diff admission" should {
    "admit the complete portable two-target package and reconstruct exact Phase 9 values" in {
      Given("a current V2 package/source/candidate, V3 realization/continuity and a complete two-target V2 semantic diff")
      var admission = Option.empty[InternalModelSemanticDiffAdmission]

      When("the project root crosses the one-pass semantic-diff validator")
      _with_fixture() { root =>
        admission = InternalModelSemanticDiffValidator.validate(root).toOption
      }

      Then("candidate and diff references, paths, captured inventory, raw payload, mappings, traces, and exact Phase 9 entries survive")
      admission.map { value =>
        (
          value.diff,
          value.diff.candidateArtifactReference,
          value.diff.candidateReference,
          value.diff.targets.map(target =>
            (
              target.targetId,
              target.patchTrace.id,
              target.patchTrace.baselineArtifactReference,
              target.patchTrace.proposedContentReference,
              target.patchTrace.cmlLocator,
              target.entries.map(entry => (entry.entry.id, entry.mappingId, entry.semanticIdentityKind, entry.entry.subject))
            )
          ),
          value.diffArtifactReference,
          value.diffPackageRelativePath,
          value.candidateAdmission.projection,
          value.candidateAdmission.targetBytes.map(bytes => (bytes.targetId, bytes.baseline.rawBytes, bytes.proposedRawBytes)),
          value.candidateAdmission.packageContext.artifacts.map(artifact => (artifact.reference, artifact.present))
        )
      } shouldBe Some((
        _valid_diff(),
        _artifact_reference("candidate-main"),
        _record_reference("candidate-order-v1", 7L),
        Vector(
          ("target-alpha", "patch-alpha", _artifact_reference("snapshot-cml-alpha"), _record_reference("content-target-alpha", 41L), "cml/alpha.cml", Vector(("entry-target-alpha", "mapping-alpha", "element", "opaque-shared"))),
          ("target-beta", "patch-beta", _artifact_reference("snapshot-cml-beta"), _record_reference("content-target-beta", 41L), "cml/beta.cml", Vector(("entry-target-beta", "mapping-beta", "relationship", "opaque-shared")))
        ),
        _artifact_reference("semantic-diff-main"),
        "projections/semantic-diff.json",
        _default_projection(),
        Vector(
          ("target-alpha", _cml_alpha_raw, Vector[Byte](0, -1, 10)),
          ("target-beta", _cml_beta_raw, _cml_beta_raw)
        ),
        Vector(
          (_artifact_reference("snapshot-cml-alpha"), true),
          (_artifact_reference("snapshot-cml-beta"), true),
          (_artifact_reference("snapshot-model"), true),
          (_artifact_reference("realization-main"), true),
          (_artifact_reference("projection-main"), true),
          (_artifact_reference("candidate-main"), true),
          (_artifact_reference("semantic-diff-main"), true)
        )
      ))
    }
  }

  "Internal-model semantic-diff evidence roundtrip" should {
    "retain exact semantic values through generated JSON member permutations, whitespace and equivalent escapes" in {
      val expected = _valid_diff()
      val members = _semantic_diff_json(expected).asObject.get.toVector
      val permutations = Gen.pick(members.size, members).map(_.toVector)
      forAll(permutations, Gen.oneOf(" ", "\n\t", "\r\n")) { (permutation, whitespace) =>
        Given("the same complete typed diff with a supplied root permutation and reversed nested JSON member order")
        def _nested_(json: Json): Json = json.arrayOrObject(json, values => Json.fromValues(values.map(_nested_)), objectvalue => Json.fromJsonObject(JsonObject.fromIterable(objectvalue.toVector.reverse.map { case (key, value) => key -> _nested_(value) })))
        val nested = permutation.map { case (key, value) => key -> _nested_(value) }
        val presented = whitespace + Printer.noSpaces.print(Json.fromJsonObject(JsonObject.fromIterable(nested))) + whitespace
        val escaped = _bytes_replace(presented.getBytes(StandardCharsets.UTF_8), "category", "\\u0063ategory")
        _with_fixture() { root =>
          _given_upstream(root)
          val handoff = InternalModelPackageValidator.verifiedSemanticDiff(root).toOption.get
          val supplied = handoff.copy(semanticdiff = handoff.semanticdiff.copy(bytes = escaped.toVector))
          When("the presentation variant is decoded and admitted on the exact current captured candidate basis")
          val decoded = InternalModelSemanticDiffCodec.decode(supplied.semanticdiff)
          val admitted = InternalModelSemanticDiffValidator.validateVerified(supplied)
          Then("the same typed diff and its ordinary semantic roundtrip survive without any byte-equality condition")
          decoded shouldBe Right(expected)
          admitted.toOption.map(_.diff) shouldBe Some(expected)
          InternalModelSemanticDiffCodec.decode(_diff_capture(InternalModelSemanticDiffCodec.encode(decoded.toOption.get))) shouldBe Right(expected)
        }
      }
    }

    "preserve generated non-ASCII and supplementary UTF-8 evidence with independent optional ties and ordered facets" in {
      val nonblankutf8 = Gen.nonEmptyListOf(
        Gen.oneOf(
          Gen.alphaNumChar.map(_.toString),
          Gen.const("é"),
          Gen.const("漢"),
          Gen.const("😀")
        )
      ).map(_.mkString)
      val pbtcase = for {
        category <- nonblankutf8
        action <- nonblankutf8
        relationship <- nonblankutf8
        before <- nonblankutf8
        after <- nonblankutf8
        authority <- nonblankutf8
        ties <- for {
          entrytie <- Gen.option(nonblankutf8)
          patchtie <- Gen.option(nonblankutf8)
        } yield (entrytie, patchtie)
      } yield (category, action, relationship, before, after, authority, ties)

      forAll(pbtcase) { generated =>
          val (category, action, relationship, before, after, authority, ties) = generated
          val (entrytie, patchtie) = ties
          Given("generated valid UTF-8 category, action, relationship, attribution, before/after, independent tie keys, and repeated ordered condition limitations")
          val condition = _condition(
            redaction = Some("redacted"),
            absence = Some("explicit absence evidence"),
            ambiguity = Some("ambiguous"),
            conflict = Some("conflict"),
            staleness = Some("stale"),
            malformed = Some("malformed"),
            limitations = Vector("first limitation", "first limitation", "第二制約")
          )
          val expectedentry = _first_entry(_valid_diff()).copy(
            category = category,
            action = action,
            relationship = relationship,
            before = Some(before),
            after = Some(after),
            attribution = _attribution(authority),
            condition = condition,
            limitations = Vector("entry first", "entry first", "entry 第二"),
            stableTieKey = entrytie
          )
          val change = (value: InternalModelSemanticDiff) => _update_first_target(value) { target =>
            target.copy(
              patchTrace = target.patchTrace.copy(
                attribution = _attribution(authority + "-patch"),
                condition = condition,
                limitations = Vector("patch first", "patch first", "patch 第二"),
                stableTieKey = patchtie
              ),
              entries = Vector(target.entries.head.copy(entry = expectedentry))
            )
          }
          var admission = Option.empty[InternalModelSemanticDiffAdmission]

          When("the generated typed diff is encoded, admitted, and decoded from the portable package")
          _with_fixture(diffchange = change) { root =>
            admission = InternalModelSemanticDiffValidator.validate(root).toOption
          }

          Then("all supplied typed evidence and semantic roundtrip values remain exact without inferred category or action")
          admission.map { value =>
            (
              value.diff.targets.head.entries.head.entry,
              value.diff.targets.head.patchTrace.condition,
              value.diff.targets.head.patchTrace.limitations,
              value.diff.targets.head.patchTrace.stableTieKey,
              InternalModelSemanticDiffCodec.decode(_diff_capture(InternalModelSemanticDiffCodec.encode(value.diff))).toOption
            )
          } shouldBe admission.map(value => (expectedentry, condition, Vector("patch first", "patch first", "patch 第二"), patchtie, Some(value.diff)))
          admission.isDefined shouldBe true
      }
    }
  }

  "Internal-model semantic-diff absence and required-value grammar" should {
    "admit explicit null before and after only with a supplied explicitAbsence facet" in {
      Given("a valid two-target diff whose first entry carries null before and after plus explicit absence evidence")
      val condition = _condition(absence = Some("source states no prior value"))
      val change = (value: InternalModelSemanticDiff) => _update_first_target(value) { target =>
        target.copy(
          patchTrace = target.patchTrace.copy(condition = condition),
          entries = Vector(target.entries.head.copy(entry = target.entries.head.entry.copy(before = None, after = None, condition = condition)))
        )
      }
      var admission = Option.empty[InternalModelSemanticDiffAdmission]

      When("the explicit-absence package crosses the validator")
      _with_fixture(diffchange = change) { root =>
        admission = InternalModelSemanticDiffValidator.validate(root).toOption
      }

      Then("the typed nulls and exact explicit absence condition remain admitted evidence")
      admission.map { value =>
        val entry = value.diff.targets.head.entries.head.entry
        (entry.before, entry.after, entry.condition.explicitAbsence)
      } shouldBe Some((None, None, Some("source states no prior value")))
    }

    "reject unavailable, unauthorized, or redacted null evidence and blank supplied values while retaining valid controls" in {
      Given("valid controls plus otherwise complete variants with missing explicit absence, blank required strings, blank optional facets, and blank limitations")
      val nullvariants = Vector(
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(before = None)),
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(before = None, condition = _condition(availability = "unavailable"))),
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(before = None, condition = _condition(authorization = "unauthorized"))),
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(before = None, condition = _condition(redaction = Some("redacted"))))
      )
      val blankvariants = Vector(
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(category = "")),
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(subject = " ")),
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(stableTieKey = Some(" "))),
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(entry => entry.copy(condition = entry.condition.copy(ambiguity = Some(" ")))),
        (value: InternalModelSemanticDiff) => _update_first_entry(value)(entry => entry.copy(limitations = Vector(" "))),
        (value: InternalModelSemanticDiff) => _update_first_patch(value)(patch => patch.copy(limitations = Vector(" ")))
      )

      When("each control and invalid condition/value variant is admitted independently")
      val control = _admission_success()
      val nullresults = nullvariants.map(change => _admission_success(change))
      val blankresults = blankvariants.map(change => _admission_success(change))

      Then("the successful control admits and every absence or blank-value defect rejects")
      control shouldBe true
      nullresults shouldBe Vector.fill(nullresults.size)(false)
      blankresults shouldBe Vector.fill(blankresults.size)(false)
    }
  }

  "Internal-model semantic-diff mapping identity" should {
    "retain shared opaque subjects only through their explicit element and relationship mapping kinds" in {
      Given("a valid two-target diff whose element and relationship mappings share opaque subject text")
      var admitted = Option.empty[InternalModelSemanticDiffAdmission]

      When("the complete candidate package is admitted")
      _with_fixture() { root =>
        admitted = InternalModelSemanticDiffValidator.validate(root).toOption
      }

      Then("each supplied mapping kind and identity is reconstructed without subject inference")
      admitted.map(_.diff.targets.flatMap(_.entries.map(entry => (entry.entry.subject, entry.mappingId, entry.semanticIdentityKind)))) shouldBe
        Some(Vector(("opaque-shared", "mapping-alpha", "element"), ("opaque-shared", "mapping-beta", "relationship")))
    }

    "reject wrong kinds, unknown subjects, unknown mappings, and target-local cross-target mappings" in {
      Given("a valid control plus four semantic-diff variants that alter only mapping locality, kind, or opaque identity")
      val cases = Vector(
        "control" -> ((value: InternalModelSemanticDiff) => value),
        "wrong kind" -> ((value: InternalModelSemanticDiff) => _update_first_mapped(value)(_.copy(semanticIdentityKind = "relationship"))),
        "unknown subject" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(subject = "opaque-unknown"))),
        "unknown mapping" -> ((value: InternalModelSemanticDiff) => _update_first_mapped(value)(_.copy(mappingId = "mapping-unknown"))),
        "cross-target mapping" -> ((value: InternalModelSemanticDiff) => _update_first_mapped(value)(_.copy(mappingId = "mapping-beta")))
      )

      When("each mapping case is admitted through the actual candidate binding")
      val results = cases.map { case (name, change) => name -> _admission_success(change) }

      Then("the control succeeds and every explicit mapping violation rejects")
      results.head._2 shouldBe true
      results.tail.map(_._2) shouldBe Vector(false, false, false, false)
    }
  }

  "Internal-model semantic-diff traceability" should {
    "retain equal, unequal, binary, and non-CML raw bytes without parsing or deriving a textual diff" in {
      Given("arbitrary binary alpha proposal bytes, an unequal binary beta proposal, and valid source snapshots carrying the original raw bytes")
      val alphabytes = Vector[Byte](0, -1, 10, 127, 1)
      val betabytes = Vector[Byte](-128, 1, 2, 3, 10)
      var admission = Option.empty[InternalModelSemanticDiffAdmission]

      When("the candidate and semantic-diff package is admitted")
      _with_fixture(alphaproposal = Some(alphabytes), betaproposal = Some(betabytes)) { root =>
        admission = InternalModelSemanticDiffValidator.validate(root).toOption
      }

      Then("raw payload and independently allocated baseline/content references remain exact")
      admission.map { value =>
        (
          value.candidateAdmission.targetBytes.map(bytes => (bytes.targetId, bytes.baseline.rawBytes, bytes.proposedRawBytes)),
          value.diff.targets.map(target => (target.targetId, target.patchTrace.baselineArtifactReference, target.patchTrace.proposedContentReference))
        )
      } shouldBe Some((
        Vector(
          ("target-alpha", _cml_alpha_raw, alphabytes),
          ("target-beta", _cml_beta_raw, betabytes)
        ),
        Vector(
          ("target-alpha", _artifact_reference("snapshot-cml-alpha"), _record_reference("content-target-alpha", 41L)),
          ("target-beta", _artifact_reference("snapshot-cml-beta"), _record_reference("content-target-beta", 41L))
        )
      ))
    }

    "retain an independent patch locator when the inherited baseline source locator is explicitly null" in {
      Given("a valid alpha source snapshot and target descriptor with locator None plus a supplied nonblank patch locator")
      val alphasource = _cml_alpha_source.copy(locator = None)
      val change = (value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(cmlLocator = "independent/patch-locator"))
      var admission = Option.empty[InternalModelSemanticDiffAdmission]

      When("the nullable-source package is admitted")
      _with_fixture(alphasource = alphasource, diffchange = change) { root =>
        admission = InternalModelSemanticDiffValidator.validate(root).toOption
      }

      Then("the source snapshot remains nullable while the independent patch locator survives unchanged")
      admission.map { value =>
        (
          value.candidateAdmission.targetBytes.head.baseline.source.locator,
          value.diff.targets.head.patchTrace.cmlLocator,
          value.candidateAdmission.targetBytes.head.baseline.source.authority
        )
      } shouldBe Some((None, "independent/patch-locator", "cml-authority"))
    }
  }

  "Internal-model semantic-diff captured handoff" should {
    "validate one captured semantic-diff handoff after fixture-owned package and project paths are removed" in {
      Given("one verified semantic-diff handoff whose manifest, candidate, diff, continuity, realization, and source paths are fixture-owned")
      var capturedadmission = Option.empty[InternalModelSemanticDiffAdmission]
      var freshsuccess = true

      When("the verified handoff is captured once, all fixture paths are removed, and only validateVerified receives the capture")
      _with_fixture() { root =>
        val captured = InternalModelPackageValidator.verifiedSemanticDiff(root)
        _delete_tree(root.resolve("src/main/internal-model"))
        Files.delete(root.resolve("project.yaml"))
        capturedadmission = captured.flatMap(value => InternalModelSemanticDiffValidator.validateVerified(value)).toOption
        freshsuccess = InternalModelSemanticDiffValidator.validate(root).isSuccess
      }

      Then("the supplied candidate/diff values and raw evidence survive capture while a fresh path read fails")
      capturedadmission.map(value => (
        value.candidateAdmission.projection,
        value.diff,
        value.candidateAdmission.targetBytes.map(bytes => (bytes.baseline.rawBytes, bytes.proposedRawBytes))
      )) shouldBe Some((
        _default_projection(),
        _valid_diff(),
        Vector(
          (_cml_alpha_raw, Vector[Byte](0, -1, 10)),
          (_cml_beta_raw, _cml_beta_raw)
        )
      ))
      capturedadmission.isDefined shouldBe true
      freshsuccess shouldBe false
    }
  }

  "Internal-model semantic-diff predecessor continuity" should {
    "admit only current V3 continuity and explicitly reject predecessor V1 and V2 families" in {
      Given("one current V3 continuity control and explicit old V1/V2 header variants with the same candidate and diff")
      val families = Vector(
        ("ccdm-projection-binding-v3", "3.0"),
        ("ccdm-projection-binding-v1", "1.0"),
        ("ccdm-projection-binding-v2", "2.0")
      )

      When("each family crosses the current package selector before semantic-diff admission")
      val results = families.map { case (profile, schema) =>
        var succeeded = false
        _with_fixture(continuityprofile = profile, continuityschema = schema) { root =>
          succeeded = InternalModelSemanticDiffValidator.validate(root).isSuccess
        }
        succeeded
      }

      Then("the current control succeeds and both old families reject without fallback")
      results shouldBe Vector(true, false, false)
    }

    "retain independent current candidate and continuity admission without a diff and reject absent or duplicate diff selections" in {
      Given("one current package without a semantic diff and two packages with respectively absent and duplicate diff projections")
      var candidateok = false
      var continuityok = false
      var absent = true
      var duplicate = true

      When("current family-specific readers and the requesting semantic-diff reader are applied")
      _with_fixture(includesemanticdiff = false) { root =>
        candidateok = InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess
        continuityok = InternalModelProjectionContinuityValidator.validate(root).isSuccess
      }
      absent = _admission_success(includesemanticdiff = false)
      duplicate = _admission_success(duplicatesemanticdiff = true)
      val independent = (candidateok, continuityok)

      Then("independent readers succeed, but a requested absent or duplicate diff fails closed")
      independent shouldBe (true, true)
      absent shouldBe false
      duplicate shouldBe false
    }

    "fail unknown or wrong profile/schema headers even when the malformed family is not requested" in {
      Given("recognized continuity plus semantic-diff artifacts carrying unknown and wrong schema header pairs")
      val variants = Vector(
        "unknown profile" -> ((value: InternalModelSemanticDiff) => value.copy(profile = "ccdm-semantic-diff-next")),
        "wrong schema" -> ((value: InternalModelSemanticDiff) => value.copy(schemaVersion = "1.0"))
      )

      When("continuity selection encounters each nonrequested header")
      val results = variants.map { case (name, change) =>
        var continuityok = true
        var diffok = true
        _with_fixture(diffchange = change) { root =>
          continuityok = InternalModelProjectionContinuityValidator.validate(root).isSuccess
          diffok = InternalModelSemanticDiffValidator.validate(root).isSuccess
        }
        (name, continuityok, diffok)
      }

      Then("both readers fail closed rather than silently ignoring an unknown or wrong profile/schema pair")
      results.map(value => (value._2, value._3)) shouldBe Vector((false, false), (false, false))
    }

    "skip only recognized semantically invalid nonrequested diff content during predecessor selection" in {
      Given("a current semantic-diff header whose exact candidate artifact reference names another artifact")
      var continuityok = false
      var diffok = true

      When("continuity selection and semantic-diff admission consume the same captured package")
      _with_fixture(diffchange = _.copy(candidateArtifactReference = _artifact_reference("candidate-main").copy(artifactId = InternalModelArtifactId.from("candidate-not-selected").toOption.get))) { root =>
        InternalModelPackageValidator.validateStructure(root).isSuccess shouldBe true
        InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess shouldBe true
        continuityok = InternalModelProjectionContinuityValidator.validate(root).isSuccess
        diffok = InternalModelSemanticDiffValidator.validate(root).isSuccess
      }

      Then("predecessor continuity succeeds independently while the semantic-diff validator rejects its binding")
      continuityok shouldBe true
      diffok shouldBe false
    }
  }

  "Internal-model semantic-diff binding matrix" should {
    "retain truthful manifests while rejecting candidate, target, patch, entry, mapping, and ordering mismatches" in {
      Given("a current independently admitted candidate basis and a complete matrix of supplied-reference and semantic binding defects")
      val cases: Vector[(String, InternalModelSemanticDiff => InternalModelSemanticDiff)] = Vector(
        "control" -> ((value: InternalModelSemanticDiff) => value),
        "candidate artifact id" -> ((value: InternalModelSemanticDiff) => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(artifactId = InternalModelArtifactId.from("candidate-other").toOption.get))),
        "candidate artifact revision" -> ((value: InternalModelSemanticDiff) => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(19L).toOption.get))),
        "candidate artifact role" -> ((value: InternalModelSemanticDiff) => value.copy(candidateArtifactReference = value.candidateArtifactReference.copy(role = InternalModelArtifactRole.Realization))),
        "candidate identity" -> ((value: InternalModelSemanticDiff) => value.copy(candidateReference = _record_reference("candidate-other", 7L))),
        "candidate model" -> ((value: InternalModelSemanticDiff) => value.copy(candidateModelIdentity = "candidate-model-other")),
        "candidate revision" -> ((value: InternalModelSemanticDiff) => value.copy(candidateReference = _record_reference("candidate-order-v1", 8L))),
        "candidate scope" -> ((value: InternalModelSemanticDiff) => value.copy(scope = value.scope.copy(componentIdentity = "component-other"))),
        "candidate context" -> ((value: InternalModelSemanticDiff) => value.copy(scope = value.scope.copy(projectionContextIdentity = "context-other"))),
        "candidate usecase" -> ((value: InternalModelSemanticDiff) => value.copy(scope = value.scope.copy(selectedUseCaseElementIdentity = "usecase-other"))),
        "targets empty" -> ((value: InternalModelSemanticDiff) => value.copy(targets = Vector.empty)),
        "target missing" -> ((value: InternalModelSemanticDiff) => value.copy(targets = value.targets.take(1))),
        "target extra" -> ((value: InternalModelSemanticDiff) => value.copy(targets = value.targets :+ value.targets.head.copy(targetId = "target-gamma"))),
        "target duplicate" -> ((value: InternalModelSemanticDiff) => value.copy(targets = Vector(value.targets.head, value.targets.head.copy(targetId = value.targets.head.targetId)))),
        "target order" -> ((value: InternalModelSemanticDiff) => value.copy(targets = value.targets.reverse)),
        "patch id" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(id = "patch-other"))),
        "patch context" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(context = MonoKotoProjectionContextIdentity("context-other")))),
        "patch component" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(component = ComponentDashboardComponentIdentity("component-other")))),
        "patch baseline id" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(baselineArtifactReference = _artifact_reference("snapshot-cml-beta")))),
        "patch baseline revision" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(patch => patch.copy(baselineArtifactReference = patch.baselineArtifactReference.copy(artifactRevision = InternalModelArtifactRevision.from(12L).toOption.get)))),
        "patch baseline role" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(patch => patch.copy(baselineArtifactReference = patch.baselineArtifactReference.copy(role = InternalModelArtifactRole.Projection)))),
        "patch proposed content id" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(proposedContentReference = _record_reference("proposal-other", 41L)))),
        "patch proposed content revision" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(proposedContentReference = _record_reference("content-target-alpha", 42L)))),
        "patch source owner" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(cmlOwner = "owner-other"))),
        "patch available locator" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(cmlLocator = "locator-other"))),
        "entry context" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(context = MonoKotoProjectionContextIdentity("context-other")))),
        "entry component" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(component = ComponentDashboardComponentIdentity("component-other")))),
        "entry patch" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(patchId = "patch-other"))),
        "entry candidate model" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(candidateModelId = "candidate-model-other"))),
        "entries empty" -> ((value: InternalModelSemanticDiff) => _update_first_target(value)(_.copy(entries = Vector.empty))),
        "mapping locality" -> ((value: InternalModelSemanticDiff) => _update_first_mapped(value)(_.copy(mappingId = "mapping-beta"))),
        "mapping kind" -> ((value: InternalModelSemanticDiff) => _update_first_mapped(value)(_.copy(semanticIdentityKind = "relationship"))),
        "mapping subject" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(subject = "subject-other"))),
        "global entry collision" -> ((value: InternalModelSemanticDiff) => {
          val target = value.targets(1)
          val mapped = target.entries.head
          value.copy(targets = value.targets.updated(1, target.copy(entries = Vector(mapped.copy(entry = mapped.entry.copy(id = value.targets.head.entries.head.entry.id))))))
        }),
        "entry order" -> ((value: InternalModelSemanticDiff) => {
          val target = value.targets.head
          val second = target.entries.head.copy(entry = target.entries.head.entry.copy(id = "entry-z"))
          value.copy(targets = value.targets.updated(0, target.copy(entries = Vector(second, target.entries.head))))
        })
      )

      When("each supplied semantic variant crosses the diff boundary on that current basis")
      val results = cases.map { case (name, change) => name -> _admission_success(change) }

      Then("only the current control succeeds and every explicit reference, semantic or ordering mismatch rejects")
      results.head._2 shouldBe true
      results.tail.map(_._2) shouldBe Vector.fill(results.size - 1)(false)
    }

    "require the semantic-diff dependency vector to be exactly the selected candidate reference" in {
      Given("a successful dependency control plus missing and extra declarations on otherwise admitted inventories")
      val cases = Vector(
        None,
        Some(Vector.empty[InternalModelArtifactReference]),
        Some(Vector(_artifact_reference("candidate-main"), _artifact_reference("snapshot-model")))
      )

      When("each manifest dependency declaration is captured and admitted")
      val results = cases.map(dependencies => _admission_success(semanticdiffdependencies = dependencies))

      Then("only the exact one-element selected-candidate dependency vector succeeds")
      results shouldBe Vector(true, false, false)
    }
  }

  "Internal-model semantic-diff closed grammar" should {
    "reject malformed transport, numeric, old-field, header and nested grammar while admitting harmless presentation" in {
      Given("one current diff, harmless presentations and strict malformed transport or closed-shape mutations")
      val value = _valid_diff()
      val control = InternalModelSemanticDiffCodec.encode(value)
      val presentation = Vector(
        control,
        (" " + new String(control, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),
        control ++ Array('\n'.toByte),
        _swap_root_members(control),
        _bytes_replace(control, "category", "\\u0063ategory")
      )
      val transport = Vector(
        Array[Byte](0xc3.toByte, 0x28.toByte),
        Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ control,
        _duplicate_root_member(control),
        control ++ Array('x'.toByte),
        control ++ "{}".getBytes(StandardCharsets.UTF_8)
      )
      val numeric = Vector(
        "0", "-1", "9223372036854775808", "\"7\"", "7.0", "7e0", "+7", "07", "null", "true"
      ).map(token => _bytes_replace(control, "\"recordRevision\":7", "\"recordRevision\":" + token))
      val oldfields = Vector(
        "candidateArtifactId", "candidateArtifactSha256", "candidateIdentity", "candidateRevision", "semanticDiffIdentity", "semanticDiffRevision", "canonicalBytes"
      ).map(key => _root_mutation(value)(_.add(key, Json.fromString("old-value")))) ++ Vector(
        _patch_mutation(value)(_.add("baseDigest", Json.fromString("old-value"))),
        _patch_mutation(value)(_.add("proposedDigest", Json.fromString("old-value")))
      )
      val headers = Vector(
        _root_mutation(value)(_.add("profile", Json.fromString("ccdm-semantic-diff-next"))),
        _root_mutation(value)(_.add("schemaVersion", Json.fromString("1.0"))),
        _root_mutation(value)(_.add("profile", Json.fromString("ccdm-semantic-diff-v1")))
      )
      val nested = _grammar_variants(value).map(_._2)

      When("each presentation or defect crosses semantic admission on independently current captured evidence")
      val positive = presentation.map(_admission_bytes)
      val results = transport.map(_admission_bytes) ++ numeric.map(_admission_bytes) ++ oldfields.map(_admission_bytes) ++ headers.map(_admission_bytes) ++ nested.map(_admission_bytes)

      Then("every harmless presentation admits and every malformed transport, old field, header or closed-shape defect rejects")
      positive shouldBe Vector.fill(positive.size)(true)
      results shouldBe Vector.fill(results.size)(false)
    }
  }

  "Internal-model semantic-diff evidence facets" should {
    "preserve every independent condition, limitation, attribution, and opaque identity without promoting approval or canonical fact" in {
      Given("a valid two-target diff carrying all condition facets, repeated limitations, independent attributions, and shared opaque identities")
      val condition = _condition(
        redaction = Some("redacted"),
        absence = Some("explicit absence"),
        ambiguity = Some("ambiguous"),
        conflict = Some("conflict"),
        staleness = Some("stale"),
        malformed = Some("malformed"),
        limitations = Vector("condition one", "condition one", "condition 二")
      )
      val change = (value: InternalModelSemanticDiff) => value.copy(targets = value.targets.map { target =>
        target.copy(
          patchTrace = target.patchTrace.copy(
            attribution = _attribution("patch-authority-" + target.targetId),
            condition = condition,
            limitations = Vector("patch one", "patch one", "patch 二")
          ),
          entries = target.entries.map { mapped =>
            mapped.copy(entry = mapped.entry.copy(
              subject = "opaque-shared",
              attribution = _attribution("entry-authority-" + target.targetId),
              condition = condition,
              limitations = Vector("entry one", "entry one", "entry 二"),
              before = None,
              after = None
            ))
          }
        )
      })
      var admission = Option.empty[InternalModelSemanticDiffAdmission]

      When("the evidence-rich package is admitted")
      _with_fixture(diffchange = change) { root =>
        admission = InternalModelSemanticDiffValidator.validate(root).toOption
      }

      Then("all facets and multiplicities remain attributed and bounded, with no local approval or canonical winner")
      admission.map { value =>
        (
          value.diff.targets.map(target => (
            target.patchTrace.attribution,
            target.patchTrace.condition,
            target.patchTrace.limitations,
            target.entries.map(entry => (
              entry.entry.subject,
              entry.semanticIdentityKind,
              entry.entry.attribution,
              entry.entry.condition,
              entry.entry.limitations
            ))
          )),
          value.diff.targets.flatMap(_.entries).map(_.semanticIdentityKind)
        )
      } shouldBe Some((
        Vector(
          (
            _attribution("patch-authority-target-alpha"),
            condition,
            Vector("patch one", "patch one", "patch 二"),
            Vector(("opaque-shared", "element", _attribution("entry-authority-target-alpha"), condition, Vector("entry one", "entry one", "entry 二")))
          ),
          (
            _attribution("patch-authority-target-beta"),
            condition,
            Vector("patch one", "patch one", "patch 二"),
            Vector(("opaque-shared", "relationship", _attribution("entry-authority-target-beta"), condition, Vector("entry one", "entry one", "entry 二")))
          )
        ),
        Vector("element", "relationship")
      ))
    }
  }

  "Internal-model semantic-diff typed references" should {
    "reject missing, scalar, cross-domain, role, Unicode and lexical revision defects at every required reference" in {
      Given("all five required root and patch references on a independently valid current basis")
      val value = _valid_diff()
      val positions: Vector[(String, Boolean, (Json => Json) => Array[Byte])] = Vector(
        ("candidateArtifactReference", false, change => _root_mutation(value)(root => root.add("candidateArtifactReference", change(root("candidateArtifactReference").get)))),
        ("candidateReference", true, change => _root_mutation(value)(root => root.add("candidateReference", change(root("candidateReference").get)))),
        ("semanticDiffReference", true, change => _root_mutation(value)(root => root.add("semanticDiffReference", change(root("semanticDiffReference").get)))),
        ("baselineArtifactReference", false, change => _patch_mutation(value)(patch => patch.add("baselineArtifactReference", change(patch("baselineArtifactReference").get)))),
        ("proposedContentReference", true, change => _patch_mutation(value)(patch => patch.add("proposedContentReference", change(patch("proposedContentReference").get))))
      )
      val variants = positions.flatMap { case (key, record, edit) =>
        val idkey = if record then "recordId" else "artifactId"
        val revisionkey = if record then "recordRevision" else "artifactRevision"
        val crossdomain = if record then _artifact_json(_artifact_reference("candidate-main")) else _record_json(_record_reference("record-only", 1L))
        val changes = Vector[Json => Json](
          _ => Json.Null, _ => Json.fromString("bare-id"), _ => Json.True, _ => Json.arr(), _ => crossdomain,
          _.mapObject(_.remove(idkey)), _.mapObject(_.remove(revisionkey)),
          _.mapObject(_.add("extra", Json.True)), _.mapObject(_.add("sha256", Json.fromString("removed"))),
          _.mapObject(_.add(idkey, Json.Null)), _.mapObject(_.add(idkey, Json.True)),
          _.mapObject(_.add(idkey, Json.fromString(" ")))
        ) ++ Vector(Json.Null, Json.fromString("1"), Json.True, Json.fromLong(0L), Json.fromLong(-1L), Json.fromDoubleOrNull(1.5)).map(token => (reference: Json) => reference.mapObject(_.add(revisionkey, token))) ++
          (if record then Vector.empty[Json => Json] else Vector[Json => Json](
            _.mapObject(_.remove("role")), _.mapObject(_.add("role", Json.Null)), _.mapObject(_.add("role", Json.True)),
            _.mapObject(_.add("role", Json.fromString("unknown"))), _.mapObject(_.add("role", Json.fromString("decision"))),
            _.mapObject(_.add(idkey, Json.fromString("漢"))), _.mapObject(_.add(idkey, Json.fromString("bad id")))
          ))
        val base = edit(identity)
        val root = io.circe.parser.parse(new String(base, StandardCharsets.UTF_8)).toOption.get
        val reference = if key == "baselineArtifactReference" || key == "proposedContentReference" then root.hcursor.downField("targets").downArray.downField("patchTrace").downField(key).focus.get else root.hcursor.downField(key).focus.get
        val original = reference.hcursor.get[Long](revisionkey).toOption.get
        val lexical = Vector("+1", "01", "1.0", "1e0", "9223372036854775808").map(token => _bytes_replace(base, s"\"$revisionkey\":$original", s"\"$revisionkey\":$token"))
        val malformedunicode = _bytes_replace(base, Json.fromString(reference.hcursor.get[String](idkey).toOption.get).noSpaces, "\"\\uD800\"")
        changes.map(edit) ++ lexical :+ malformedunicode
      }
      val missing = Vector(
        _root_mutation(value)(_.remove("candidateArtifactReference")),
        _root_mutation(value)(_.remove("candidateReference")),
        _root_mutation(value)(_.remove("semanticDiffReference")),
        _patch_mutation(value)(_.remove("baselineArtifactReference")),
        _patch_mutation(value)(_.remove("proposedContentReference"))
      )
      When("each malformed supplied reference reaches semantic admission on current captured evidence")
      val results = (variants ++ missing).map(_admission_bytes)
      Then("every required reference remains closed, explicit, role-specific and within its identity and positive Long domain")
      results shouldBe Vector.fill(results.size)(false)
    }

    "preserve independently supplied artifact, candidate, diff and carrier Long revisions including endpoints" in {
      forAll(Gen.oneOf(1L, Long.MaxValue, 17L), Gen.oneOf(1L, Long.MaxValue, 7L), Gen.oneOf(1L, Long.MaxValue, 31L)) { (artifactrevision, candidaterevision, diffrevision) =>
        Given("a current capture with explicitly producer-supplied independent positive revisions")
        _with_fixture() { root =>
          _given_upstream(root)
          val original = InternalModelPackageValidator.verifiedSemanticDiff(root).toOption.get
          val supplied = _versions(original, artifactrevision, candidaterevision, diffrevision, Long.MaxValue)
          InternalModelCandidateCmlProjectionValidator.validateVerified(supplied.candidatepackage).isSuccess shouldBe true
          InternalModelProjectionContinuityValidator.validateVerified(supplied.candidatepackage.continuitypackage).isSuccess shouldBe true
          When("the complete typed diff is admitted on those exact supplied versions")
          val result = InternalModelSemanticDiffValidator.validateVerified(supplied)
          Then("all domains retain their independent values without narrowing or deriving revisions from payload")
          result.isSuccess shouldBe true
          val admission = result.toOption.get
          admission.diff.candidateArtifactReference.artifactRevision.value shouldBe artifactrevision
          admission.diffArtifactReference.artifactRevision.value shouldBe artifactrevision
          admission.diff.candidateReference.recordRevision.value shouldBe candidaterevision
          admission.diff.semanticDiffReference.recordRevision.value shouldBe diffrevision
          admission.candidateAdmission.packageContext.revision shouldBe Long.MaxValue
          admission.diff.targets.head.patchTrace.proposedContentReference.recordRevision.value shouldBe 41L
        }
      }
    }

    "retain an unchanged diff subject when only the captured carrier revision changes" in {
      Given("one valid captured candidate and semantic diff plus a separately advanced carrier revision")
      _with_fixture() { root =>
        _given_upstream(root)
        val original = InternalModelPackageValidator.verifiedSemanticDiff(root).toOption.get
        val candidate = original.candidatepackage
        val changed = original.copy(candidatepackage = candidate.copy(packagecontext = candidate.packagecontext.copy(revision = Long.MaxValue)))
        InternalModelCandidateCmlProjectionValidator.validateVerified(changed.candidatepackage).isSuccess shouldBe true
        When("both captures cross semantic-diff admission")
        val first = InternalModelSemanticDiffValidator.validateVerified(original)
        val second = InternalModelSemanticDiffValidator.validateVerified(changed)
        Then("semantic references and all evidence remain equal while the external carrier revision remains independent")
        first.isSuccess shouldBe true
        second.isSuccess shouldBe true
        second.toOption.map(_.diff) shouldBe first.toOption.map(_.diff)
        second.toOption.map(_.candidateAdmission.packageContext.revision) shouldBe Some(Long.MaxValue)
      }
    }

    "retain explicitly unknown source revisions beside known control versions" in {
      Given("an alpha source snapshot and candidate descriptor with revision None and exact known artifact/content references")
      val alphasource = _cml_alpha_source.copy(revision = None)
      _with_fixture(alphasource = alphasource) { root =>
        _given_upstream(root)
        When("that recorded evidence crosses semantic-diff admission")
        val result = InternalModelSemanticDiffValidator.validate(root)
        Then("source revision stays unknown without substituting any artifact, content or carrier revision")
        result.isSuccess shouldBe true
        val admission = result.toOption.get
        admission.candidateAdmission.targetBytes.head.baseline.source.revision shouldBe None
        admission.diff.targets.head.patchTrace.baselineArtifactReference shouldBe _artifact_reference("snapshot-cml-alpha")
        admission.diff.targets.head.patchTrace.proposedContentReference shouldBe _record_reference("content-target-alpha", 41L)
      }
    }
  }

  "Internal-model semantic-diff captured metadata" should {
    "reject null, malformed and contradictory selected, context and candidate metadata without partial success" in {
      Given("a complete current package, candidate and continuity admitted independently before metadata variants are supplied")
      _with_fixture() { root =>
        _given_upstream(root)
        val original = InternalModelPackageValidator.verifiedSemanticDiff(root).toOption.get
        val selected = original.semanticdiff
        val candidate = original.candidatepackage
        val context = candidate.packagecontext
        val selectedentry = context.artifacts.find(_.reference == selected.reference).get
        def _context_(change: InternalModelVerifiedPackageContext => InternalModelVerifiedPackageContext): InternalModelVerifiedSemanticDiffPackage =
          original.copy(candidatepackage = candidate.copy(packagecontext = change(context)))
        def _entry_(change: InternalModelVerifiedArtifactContext => InternalModelVerifiedArtifactContext): InternalModelVerifiedSemanticDiffPackage =
          _context_(value => value.copy(artifacts = value.artifacts.map(entry => if entry.reference == selected.reference then change(entry) else entry)))
        val variants = Vector(
          null, original.copy(candidatepackage = null), original.copy(semanticdiff = null),
          original.copy(candidatepackage = candidate.copy(candidate = null)),
          original.copy(candidatepackage = candidate.copy(continuitypackage = null)),
          original.copy(candidatepackage = candidate.copy(candidate = candidate.candidate.copy(bytes = null))),
          _context_(_ => null), _context_(_.copy(reference = null)), _context_(_.copy(artifacts = null)),
          _context_(_.copy(revision = 0L)), _context_(_.copy(schemaversion = "1.0")),
          _context_(_.copy(artifacts = context.artifacts :+ selectedentry)),
          _context_(_.copy(artifacts = context.artifacts.filterNot(_.reference == selected.reference))),
          _context_(_.copy(artifacts = context.artifacts.updated(0, null))),
          _context_(_.copy(artifacts = context.artifacts.reverse)),
          _entry_(_.copy(present = false)), _entry_(_.copy(required = false)),
          _entry_(_.copy(path = "projections/substitute.json")),
          _entry_(_.copy(reference = selected.reference.copy(artifactRevision = InternalModelArtifactRevision.from(1L).toOption.get))),
          original.copy(semanticdiff = selected.copy(reference = null)),
          original.copy(semanticdiff = selected.copy(reference = selected.reference.copy(artifactId = "bad id".asInstanceOf[InternalModelArtifactId]))),
          original.copy(semanticdiff = selected.copy(reference = selected.reference.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision]))),
          original.copy(semanticdiff = selected.copy(reference = selected.reference.copy(role = null))),
          original.copy(semanticdiff = selected.copy(reference = selected.reference.copy(role = InternalModelArtifactRole.Decision))),
          original.copy(semanticdiff = selected.copy(reference = selected.reference.copy(artifactRevision = InternalModelArtifactRevision.from(1L).toOption.get))),
          original.copy(semanticdiff = selected.copy(path = "../diff.json")),
          original.copy(semanticdiff = selected.copy(path = "projections/another.json")),
          original.copy(semanticdiff = selected.copy(required = false)),
          original.copy(semanticdiff = selected.copy(bytes = null)),
          original.copy(semanticdiff = selected.copy(dependencies = null)),
          original.copy(semanticdiff = selected.copy(dependencies = Vector(null))),
          original.copy(semanticdiff = selected.copy(dependencies = selected.dependencies ++ selected.dependencies)),
          original.copy(semanticdiff = selected.copy(dependencies = Vector(selected.reference))),
          original.copy(semanticdiff = selected.copy(dependencies = Vector(_artifact_reference("snapshot-model"), _artifact_reference("candidate-main")))),
          original.copy(semanticdiff = selected.copy(dependencies = Vector(_artifact_reference("candidate-main").copy(artifactRevision = InternalModelArtifactRevision.from(1L).toOption.get)))),
          original.copy(semanticdiff = selected.copy(dependencies = Vector(_artifact_reference("candidate-main").copy(role = InternalModelArtifactRole.Decision))))
        )
        val malformedselected = Vector(null, selected.copy(reference = null), selected.copy(path = "../diff.json"), selected.copy(bytes = null), selected.copy(dependencies = null), selected.copy(dependencies = Vector(null)))
        When("the complete captures and directly selected codec metadata cross their respective admission boundaries")
        val results = variants.map(InternalModelSemanticDiffValidator.validateVerified)
        val decoded = malformedselected.map(InternalModelSemanticDiffCodec.decode)
        Then("every malformed or contradictory capture is a failure without a crash or partial admitted diff")
        results.foreach(_.isSuccess shouldBe false)
        decoded.foreach(_.isLeft shouldBe true)
      }
    }
  }

  private def _default_projection(
    alphasource: InternalModelSemanticSource = _cml_alpha_source,
    betasource: InternalModelSemanticSource = _cml_beta_source
  ): InternalModelCandidateCmlProjection =
    _projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw, alphasource = alphasource, betasource = betasource)

  private def _valid_diff(): InternalModelSemanticDiff =
    _semantic_diff(_default_projection())

  private def _semantic_diff(
    projection: InternalModelCandidateCmlProjection
  ): InternalModelSemanticDiff = {
    val targets = projection.targets.map { target =>
      val mapping = target.mappings.head
      val condition = _condition()
      val entry = CandidateDesignSemanticDiffEntry(
        "entry-" + target.targetId,
        MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity),
        ComponentDashboardComponentIdentity(projection.scope.componentIdentity),
        target.patchIdentity,
        projection.candidateModelIdentity,
        "category",
        "action",
        mapping.semanticIdentity,
        Some("before"),
        Some("after"),
        "relationship",
        _attribution("entry-authority-" + target.targetId),
        condition,
        Vector("entry limitation"),
        Some("entry tie")
      )
      val patch = CandidateDesignProposedCmlPatchTrace(
        target.patchIdentity,
        MonoKotoProjectionContextIdentity(projection.scope.projectionContextIdentity),
        ComponentDashboardComponentIdentity(projection.scope.componentIdentity),
        target.source.authority,
        target.source.locator.getOrElse("supplied-patch-locator-" + target.targetId),
        target.baselineArtifactReference,
        target.proposedContent.contentReference,
        _attribution("patch-authority-" + target.targetId),
        condition,
        Vector("patch limitation"),
        Some("patch tie")
      )
      InternalModelSemanticDiffTarget(
        Vector(InternalModelSemanticDiffMappedEntry(entry, mapping.mappingId, mapping.semanticIdentityKind)),
        patch,
        target.targetId
      )
    }
    InternalModelSemanticDiff(
      _artifact_reference("candidate-main"),
      projection.candidateReference,
      projection.candidateModelIdentity,
      "ccdm-semantic-diff-v2",
      "2.0",
      InternalModelSemanticScope(
        projection.scope.componentIdentity,
        projection.scope.projectionContextIdentity,
        projection.scope.selectedUseCaseElementIdentity
      ),
      _record_reference("semantic-diff-order", 31L),
      targets
    )
  }

  private def _admission_success(
    diffchange: InternalModelSemanticDiff => InternalModelSemanticDiff = (value: InternalModelSemanticDiff) => value,
    semanticdiffdependencies: Option[Vector[InternalModelArtifactReference]] = None,
    includesemanticdiff: Boolean = true,
    duplicatesemanticdiff: Boolean = false
  ): Boolean = {
    var succeeded = false
    _with_fixture(
      diffchange = diffchange,
      semanticdiffdependencies = semanticdiffdependencies,
      includesemanticdiff = includesemanticdiff,
      duplicatesemanticdiff = duplicatesemanticdiff
    ) { root =>
      _given_upstream(root)
      When("the semantic diff is admitted against that independently current candidate capture")
      succeeded = InternalModelSemanticDiffValidator.validate(root).isSuccess
    }
    succeeded
  }

  private def _admission_bytes(bytes: Array[Byte]): Boolean = {
    var succeeded = false
    _with_fixture() { root =>
      _given_upstream(root)
      val handoff = InternalModelPackageValidator.verifiedSemanticDiff(root).toOption.get
      val changed = handoff.copy(semanticdiff = handoff.semanticdiff.copy(bytes = bytes.toVector))
      When("the supplied diff bytes are decoded on the independently current captured basis")
      succeeded = InternalModelSemanticDiffValidator.validateVerified(changed).isSuccess
    }
    succeeded
  }

  private def _given_upstream(root: Path): Unit = {
    Given("a V2 package/source/candidate and V3 continuity basis independently admitted before the selected diff action")
    InternalModelPackageValidator.validateStructure(root).isSuccess shouldBe true
    InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess shouldBe true
    InternalModelProjectionContinuityValidator.validate(root).isSuccess shouldBe true
  }

  private def _diff_capture(bytes: Array[Byte]): InternalModelVerifiedProjection =
    InternalModelVerifiedProjection(_artifact_reference("semantic-diff-main"), "projections/semantic-diff.json", true, Vector(_artifact_reference("candidate-main")), bytes.toVector)

  private def _versions(
    handoff: InternalModelVerifiedSemanticDiffPackage,
    artifactrevision: Long,
    candidaterevision: Long,
    diffrevision: Long,
    carrierrevision: Long
  ): InternalModelVerifiedSemanticDiffPackage = {
    def _reference_(reference: InternalModelArtifactReference): InternalModelArtifactReference =
      reference.copy(artifactRevision = InternalModelArtifactRevision.from(artifactrevision).toOption.get)
    def _json_(json: Json): Json = json.arrayOrObject(json, values => Json.fromValues(values.map(_json_)), objectvalue => {
      val children = JsonObject.fromIterable(objectvalue.toVector.map { case (key, value) => key -> _json_(value) })
      val artifact = if children.contains("artifactRevision") then children.add("artifactRevision", Json.fromLong(artifactrevision)) else children
      val record = artifact("recordId").flatMap(_.asString) match {
        case Some("candidate-order-v1") => artifact.add("recordRevision", Json.fromLong(candidaterevision))
        case Some("semantic-diff-order") => artifact.add("recordRevision", Json.fromLong(diffrevision))
        case _ => artifact
      }
      Json.fromJsonObject(record)
    })
    def _bytes_(bytes: Vector[Byte]): Vector[Byte] =
      _canonical(_json_(io.circe.parser.parse(new String(bytes.toArray, StandardCharsets.UTF_8)).toOption.get)).toVector
    def _projection_(projection: InternalModelVerifiedProjection): InternalModelVerifiedProjection =
      projection.copy(reference = _reference_(projection.reference), dependencies = projection.dependencies.map(_reference_), bytes = _bytes_(projection.bytes))
    val candidate = handoff.candidatepackage
    val continuity = candidate.continuitypackage
    val realization = continuity.realizationpackage
    val retained = realization.realization
    val context = candidate.packagecontext
    val changedcontext = context.copy(revision = carrierrevision, artifacts = context.artifacts.map(entry => entry.copy(reference = _reference_(entry.reference), dependencies = entry.dependencies.map(_reference_))))
    val changedrealization = realization.copy(
      realization = retained.copy(reference = _reference_(retained.reference), dependencies = retained.dependencies.map(_reference_), bytes = _bytes_(retained.bytes)),
      sourcesnapshots = realization.sourcesnapshots.map(snapshot => snapshot.copy(reference = _reference_(snapshot.reference), dependencies = snapshot.dependencies.map(_reference_)))
    )
    handoff.copy(
      candidatepackage = candidate.copy(
        packagecontext = changedcontext,
        continuitypackage = continuity.copy(realizationpackage = changedrealization, projection = _projection_(continuity.projection)),
        candidate = _projection_(candidate.candidate)
      ),
      semanticdiff = _projection_(handoff.semanticdiff)
    )
  }

  private def _first_entry(value: InternalModelSemanticDiff): CandidateDesignSemanticDiffEntry =
    value.targets.head.entries.head.entry

  private def _update_first_target(
    value: InternalModelSemanticDiff
  )(
    change: InternalModelSemanticDiffTarget => InternalModelSemanticDiffTarget
  ): InternalModelSemanticDiff =
    value.copy(targets = value.targets.updated(0, change(value.targets.head)))

  private def _update_first_patch(
    value: InternalModelSemanticDiff
  )(
    change: CandidateDesignProposedCmlPatchTrace => CandidateDesignProposedCmlPatchTrace
  ): InternalModelSemanticDiff =
    _update_first_target(value)(target => target.copy(patchTrace = change(target.patchTrace)))

  private def _update_first_mapped(
    value: InternalModelSemanticDiff
  )(
    change: InternalModelSemanticDiffMappedEntry => InternalModelSemanticDiffMappedEntry
  ): InternalModelSemanticDiff =
    _update_first_target(value)(target => target.copy(entries = Vector(change(target.entries.head))))

  private def _update_first_entry(
    value: InternalModelSemanticDiff
  )(
    change: CandidateDesignSemanticDiffEntry => CandidateDesignSemanticDiffEntry
  ): InternalModelSemanticDiff =
    _update_first_mapped(value)(mapped => mapped.copy(entry = change(mapped.entry)))

  private def _grammar_variants(value: InternalModelSemanticDiff): Vector[(String, Array[Byte])] =
    Vector(
      "root extra" -> _root_mutation(value)(_.add("extra", Json.True)),
      "root missing" -> _root_mutation(value)(_.remove("candidateReference")),
      "root type" -> _root_mutation(value)(_.add("candidateModelIdentity", Json.True)),
      "scope extra" -> _scope_mutation(value)(_.add("extra", Json.True)),
      "scope missing" -> _scope_mutation(value)(_.remove("componentIdentity")),
      "scope type" -> _scope_mutation(value)(_.add("componentIdentity", Json.True)),
      "target extra" -> _target_mutation(value)(_.add("extra", Json.True)),
      "target missing" -> _target_mutation(value)(_.remove("targetId")),
      "target type" -> _target_mutation(value)(_.add("targetId", Json.True)),
      "mapped-entry extra" -> _mapped_entry_mutation(value)(_.add("extra", Json.True)),
      "mapped-entry missing" -> _mapped_entry_mutation(value)(_.remove("mappingId")),
      "mapped-entry type" -> _mapped_entry_mutation(value)(_.add("mappingId", Json.True)),
      "entry extra" -> _entry_mutation(value)(_.add("extra", Json.True)),
      "entry missing" -> _entry_mutation(value)(_.remove("action")),
      "entry type" -> _entry_mutation(value)(_.add("action", Json.True)),
      "patch extra" -> _patch_mutation(value)(_.add("extra", Json.True)),
      "patch missing" -> _patch_mutation(value)(_.remove("id")),
      "patch type" -> _patch_mutation(value)(_.add("id", Json.True)),
      "attribution extra" -> _entry_attribution_mutation(value)(_.add("extra", Json.True)),
      "attribution missing" -> _entry_attribution_mutation(value)(_.remove("sourceId")),
      "attribution type" -> _entry_attribution_mutation(value)(_.add("sourceId", Json.True)),
      "condition extra" -> _entry_condition_mutation(value)(_.add("extra", Json.True)),
      "condition missing" -> _entry_condition_mutation(value)(_.remove("availability")),
      "condition type" -> _entry_condition_mutation(value)(_.add("availability", Json.True))
    )

  private def _root_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _canonical(Json.fromJsonObject(change(_semantic_diff_json(value).asObject.getOrElse(JsonObject.empty))))

  private def _scope_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _root_mutation(value) { root =>
      val scope = root("scope").flatMap(_.asObject).getOrElse(JsonObject.empty)
      root.add("scope", Json.fromJsonObject(change(scope)))
    }

  private def _target_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _root_mutation(value) { root =>
      val targets = root("targets").flatMap(_.asArray).getOrElse(Vector.empty)
      val target = targets.head.asObject.getOrElse(JsonObject.empty)
      root.add("targets", Json.fromValues(targets.updated(0, Json.fromJsonObject(change(target)))))
    }

  private def _mapped_entry_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _target_mutation(value) { target =>
      val entries = target("entries").flatMap(_.asArray).getOrElse(Vector.empty)
      val mapped = entries.head.asObject.getOrElse(JsonObject.empty)
      target.add("entries", Json.fromValues(entries.updated(0, Json.fromJsonObject(change(mapped)))))
    }

  private def _entry_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _mapped_entry_mutation(value) { mapped =>
      val entry = mapped("entry").flatMap(_.asObject).getOrElse(JsonObject.empty)
      mapped.add("entry", Json.fromJsonObject(change(entry)))
    }

  private def _patch_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _target_mutation(value) { target =>
      val patch = target("patchTrace").flatMap(_.asObject).getOrElse(JsonObject.empty)
      target.add("patchTrace", Json.fromJsonObject(change(patch)))
    }

  private def _entry_attribution_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _entry_mutation(value) { entry =>
      val attribution = entry("attribution").flatMap(_.asObject).getOrElse(JsonObject.empty)
      entry.add("attribution", Json.fromJsonObject(change(attribution)))
    }

  private def _entry_condition_mutation(
    value: InternalModelSemanticDiff
  )(
    change: JsonObject => JsonObject
  ): Array[Byte] =
    _entry_mutation(value) { entry =>
      val condition = entry("condition").flatMap(_.asObject).getOrElse(JsonObject.empty)
      entry.add("condition", Json.fromJsonObject(change(condition)))
    }

  private def _semantic_diff_json(value: InternalModelSemanticDiff): Json =
    io.circe.parser.parse(new String(InternalModelSemanticDiffCodec.encode(value), StandardCharsets.UTF_8)).fold(
      error => throw IllegalArgumentException(error.message),
      identity
    )

  private def _swap_root_members(bytes: Array[Byte]): Array[Byte] = {
    val root = io.circe.parser.parse(new String(bytes, StandardCharsets.UTF_8)).toOption.get.asObject.get
    val members = root.toVector
    val changed = Json.fromJsonObject(JsonObject.fromIterable(Vector(members(1), members(0)) ++ members.drop(2)))
    val result = (Printer.noSpaces.print(changed) + "\n").getBytes(StandardCharsets.UTF_8)
    result.toVector should not be bytes.toVector
    result
  }

  private def _duplicate_root_member(bytes: Array[Byte]): Array[Byte] = {
    val content = new String(bytes, StandardCharsets.UTF_8).stripSuffix("\n")
    val root = io.circe.parser.parse(content).toOption.get.asObject.get
    val (key, value) = root.toVector.head
    val member = Json.fromString(key).noSpaces + ":" + value.noSpaces
    ("{" + member + "," + content.substring(1) + "\n").getBytes(StandardCharsets.UTF_8)
  }

  private def _bytes_replace(bytes: Array[Byte], before: String, after: String): Array[Byte] = {
    val original = new String(bytes, StandardCharsets.UTF_8)
    original should include (before)
    val changed = original.replace(before, after)
    changed should not be original
    changed.getBytes(StandardCharsets.UTF_8)
  }

  private def _with_fixture(
    includecandidate: Boolean = true,
    includesemanticdiff: Boolean = true,
    duplicatesemanticdiff: Boolean = false,
    semanticdiffdependencies: Option[Vector[InternalModelArtifactReference]] = None,
    semanticdiffbytes: Option[Array[Byte]] = None,
    diffchange: InternalModelSemanticDiff => InternalModelSemanticDiff = (value: InternalModelSemanticDiff) => value,
    continuityprofile: String = "ccdm-projection-binding-v3",
    continuityschema: String = "3.0",
    continuityid: String = "projection-main",
    continuitypath: String = "projections/continuity.json",
    candidateid: String = "candidate-main",
    candidatepath: String = "projections/candidate.json",
    candidatebytes: Option[Array[Byte]] = None,
    alphasource: InternalModelSemanticSource = _cml_alpha_source,
    betasource: InternalModelSemanticSource = _cml_beta_source,
    alphaproposal: Option[Vector[Byte]] = None,
    betaproposal: Option[Vector[Byte]] = None
  )(
    f: Path => Unit
  ): Unit = {
    val model = _model_snapshot()
    val alpha = _cml_snapshot(alphasource, "cml/alpha.cml", _cml_alpha_raw)
    val beta = _cml_snapshot(betasource, "cml/beta.cml", _cml_beta_raw)
    val realization = _realization("ccdm-realization-v3")
    val continuity = _binding(continuityprofile, continuityschema)
    val candidateprojection = _projection(
      alphaproposal.getOrElse(Vector[Byte](0, -1, 10)),
      "é",
      betaproposal.getOrElse(_cml_beta_raw),
      continuityid = continuityid,
      alphasource = alphasource,
      betasource = betasource
    )
    val candidate = candidatebytes.getOrElse(InternalModelCandidateCmlProjectionCodec.encode(candidateprojection))
    val diffvalue = diffchange(_semantic_diff(candidateprojection))
    val diff = semanticdiffbytes.getOrElse(InternalModelSemanticDiffCodec.encode(diffvalue))
    val snapshots = Vector(
      _artifact("snapshot-cml-alpha", "snapshots/cml-alpha.json", "source-snapshot", alpha, Vector.empty),
      _artifact("snapshot-cml-beta", "snapshots/cml-beta.json", "source-snapshot", beta, Vector.empty),
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", model, Vector.empty)
    )
    val realizations = Vector(_artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")))
    val continuityartifacts = Vector(_artifact(continuityid, continuitypath, "projection", continuity, Vector("realization-main")))
    val candidateartifacts = if includecandidate then {
      val dependencies = Vector("realization-main", continuityid, "snapshot-cml-alpha", "snapshot-cml-beta")
      Vector(_artifact(candidateid, candidatepath, "projection", candidate, dependencies))
    } else Vector.empty
    val semanticdiffartifacts = if includesemanticdiff then {
      val dependencies = semanticdiffdependencies.getOrElse(Vector(_artifact_reference(candidateid)))
      val primary = _artifact_with_dependencies("semantic-diff-main", "projections/semantic-diff.json", "projection", dependencies)
      if duplicatesemanticdiff then
        Vector(_artifact_with_dependencies("semantic-diff-copy", "projections/semantic-diff-copy.json", "projection", dependencies), primary)
      else Vector(primary)
    } else Vector.empty
    val artifacts = snapshots ++ realizations ++ continuityartifacts ++ candidateartifacts ++ semanticdiffartifacts
    val root = _temporary_root()
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: candidate-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      _write(root.resolve("src/main/internal-model/snapshots/cml-alpha.json"), alpha)
      _write(root.resolve("src/main/internal-model/snapshots/cml-beta.json"), beta)
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), model)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model").resolve(continuitypath), continuity)
      if includecandidate then _write(root.resolve("src/main/internal-model").resolve(candidatepath), candidate)
      if includesemanticdiff then {
        _write(root.resolve("src/main/internal-model/projections/semantic-diff.json"), diff)
        if duplicatesemanticdiff then _write(root.resolve("src/main/internal-model/projections/semantic-diff-copy.json"), diff)
      }
      f(root)
    } finally _delete_tree(root)
  }

  private def _projection(
    rawbytes: Vector[Byte],
    destinationidentity: String,
    baseline: Vector[Byte],
    candidateidentity: String = "candidate-order-v1",
    label: String = "order",
    continuityid: String = "projection-main",
    alphasource: InternalModelSemanticSource = _cml_alpha_source,
    betasource: InternalModelSemanticSource = _cml_beta_source
  ): InternalModelCandidateCmlProjection = {
    val alpha = _target("snapshot-cml-alpha", "patch-alpha", "target-alpha", alphasource, "cml/alpha.cml", rawbytes, "mapping-alpha", "opaque-shared", "element", "effect-alpha", "ref-opaque-element-kind-Mono").copy(
      mappings = Vector(InternalModelCandidateCmlMapping(Vector("a-opaque-element-kind-Mono"), "anchor-target-alpha", "cml-" + destinationidentity, Vector("c-opaque-element"), Vector("z-opaque-element-enrichment"), "mapping-alpha", "opaque-shared", "element")),
      effects = Vector(
        InternalModelCandidateCmlEffect("unknown", "first attributable expectation", "effect-alpha-compatibility", "compatibility", Vector("mapping-alpha"), "ref-opaque-element-kind-Mono"),
        InternalModelCandidateCmlEffect("required", "migration expectation remains separate", "effect-alpha-migration", "migration", Vector("mapping-alpha"), "ref-opaque-element-enrichment")
      )
    )
    val beta = _target("snapshot-cml-beta", "patch-beta", "target-beta", betasource, "cml/beta.cml", baseline, "mapping-beta", "opaque-shared", "relationship", "effect-beta", "ref-opaque-relationship-role-StructuralDomain").copy(
      mappings = Vector(InternalModelCandidateCmlMapping(Vector("a-opaque-relationship-role-StructuralDomain"), "anchor-target-beta", "cml-" + destinationidentity, Vector("c-opaque-relationship"), Vector("z-opaque-relationship-enrichment"), "mapping-beta", "opaque-shared", "relationship")),
      effects = Vector(
        InternalModelCandidateCmlEffect("breaking", "disagreeing attributable expectation", "effect-beta-compatibility", "compatibility", Vector("mapping-beta"), "ref-opaque-relationship-role-StructuralDomain"),
        InternalModelCandidateCmlEffect("unknown", "unknown migration expectation remains visible", "effect-beta-migration", "migration", Vector("mapping-beta"), "ref-opaque-relationship-enrichment")
      )
    )
    InternalModelCandidateCmlProjection(
      candidateReference = _record_reference(candidateidentity, 7L),
      candidateModelIdentity = "candidate-model-" + label,
      continuityArtifactReference = _artifact_reference(continuityid),
      profile = "ccdm-candidate-cml-projection-v2",
      realizationArtifactReference = _artifact_reference("realization-main"),
      schemaVersion = "2.0",
      scope = InternalModelSemanticScope("component-order", "context-order", "e-usecase"),
      targets = Vector(alpha, beta)
    )
  }

  private def _target(
    baselineid: String,
    patchidentity: String,
    targetid: String,
    source: InternalModelSemanticSource,
    path: String,
    rawbytes: Vector[Byte],
    mappingid: String,
    semanticidentity: String,
    kind: String,
    effectprefix: String,
    referenceid: String
  ): InternalModelCandidateCmlTarget = {
    val canonicalids = if kind == "element" then Vector("a-e-mono-kind-Mono") else Vector("a-r-mono-domain-role-StructuralDomain")
    val mapping = InternalModelCandidateCmlMapping(canonicalids, "anchor-" + targetid, "cml-" + targetid, Vector.empty, Vector.empty, mappingid, semanticidentity, kind)
    val effects = Vector(
      InternalModelCandidateCmlEffect("unknown", "supplied compatibility expectation", effectprefix + "-compatibility", "compatibility", Vector(mappingid), referenceid),
      InternalModelCandidateCmlEffect("unknown", "supplied migration expectation", effectprefix + "-migration", "migration", Vector(mappingid), referenceid)
    )
    InternalModelCandidateCmlTarget(_artifact_reference(baselineid), effects, Vector(mapping), patchidentity, path, _content(rawbytes, "content-" + targetid), source, targetid)
  }

  private def _content(bytes: Vector[Byte], id: String): InternalModelCandidateCmlContent =
    InternalModelCandidateCmlContent(_record_reference(id, 41L), bytes.length.toLong, Base64.getEncoder.encodeToString(bytes.toArray))

  private def _condition(
    availability: String = "available",
    authorization: String = "authorized",
    redaction: Option[String] = None,
    absence: Option[String] = None,
    ambiguity: Option[String] = None,
    conflict: Option[String] = None,
    staleness: Option[String] = None,
    malformed: Option[String] = None,
    limitations: Vector[String] = Vector("condition limitation")
  ): ComponentDashboardCondition =
    ComponentDashboardCondition(availability, authorization, redaction, absence, ambiguity, conflict, staleness, malformed, limitations)

  private def _attribution(authority: String): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution("source-id", authority, "source-locator")

  private def _source(authority: String, identity: String, locator: Option[String], revision: Option[String]): InternalModelSemanticSource =
    InternalModelSemanticSource(authority, identity, locator, revision)

  private def _model_snapshot(): Array[Byte] = {
    val facts = Vector(
      _fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono"),
      _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case"),
      _fact("element", "opaque-shared", "anchor-opaque-element-enrichment", "enrichment:shared"),
      _fact("element", "opaque-shared", "anchor-opaque-element-kind-Mono", "kind:Mono"),
      _fact("relationship", "opaque-shared", "anchor-opaque-relationship-enrichment", "enrichment:shared"),
      _fact("relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain", "role:StructuralDomain"),
      _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain")
    ).sortBy(value => (value.hcursor.get[String]("semanticIdentityKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("sourceAnchor").toOption.get))
    _canonical(Json.obj(
      "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)),
      "schemaVersion" -> Json.fromString("2.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> _source_json(_model_source)
    ))
  }

  private def _cml_snapshot(source: InternalModelSemanticSource, path: String, rawbytes: Vector[Byte]): Array[Byte] =
    _canonical(Json.obj(
      "basis" -> Json.obj(
        "byteLength" -> Json.fromLong(rawbytes.length.toLong),
        "projectRelativePath" -> Json.fromString(path),
        "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes.toArray))
      ),
      "schemaVersion" -> Json.fromString("2.0"),
      "snapshotKind" -> Json.fromString("cml-baseline"),
      "source" -> _source_json(source)
    ))

  private def _realization(profile: String): Array[Byte] = {
    val schema = "3.0"
    val assertions = Vector(
      _assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", Vector.empty),
      _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", Vector.empty),
      _assertion("a-opaque-element-kind-Mono", "element", "opaque-shared", "ref-opaque-element-kind-Mono", "kind:Mono", Vector("c-opaque-element")),
      _assertion("a-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "ref-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", Vector("c-opaque-relationship")),
      _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", Vector.empty)
    ).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val enrichment = Vector(
      _assertion("z-opaque-element-enrichment", "element", "opaque-shared", "ref-opaque-element-enrichment", "enrichment:shared", Vector("c-opaque-element")),
      _assertion("z-opaque-relationship-enrichment", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "enrichment:shared", Vector("c-opaque-relationship"))
    ).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(assertions),
      "conditions" -> Json.fromValues(Vector(
        _condition_json("c-opaque-element", "limitation", "element", "opaque-shared", "ref-opaque-element-enrichment", "element evidence remains bounded"),
        _condition_json("c-opaque-relationship", "limitation", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "relationship evidence remains bounded")
      )),
      "elements" -> Json.fromValues(Vector(
        _element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")),
        _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")),
        _element("opaque-shared", "Mono", "Opaque shared element", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element"))
      )),
      "enrichmentAssertions" -> Json.fromValues(enrichment),
      "profile" -> Json.fromString(profile),
      "realizationReference" -> _record_json(_record_reference("realization-order", 19L)),
      "relationships" -> Json.fromValues(Vector(
        _relationship("opaque-shared", "StructuralDomain", "e-mono", "e-usecase", Vector("a-opaque-relationship-role-StructuralDomain"), Vector("z-opaque-relationship-enrichment"), Vector("c-opaque-relationship")),
        _relationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase", Vector("a-r-mono-domain-role-StructuralDomain"))
      )),
      "schemaVersion" -> Json.fromString(schema),
      "scope" -> _scope_json,
      "sourceReferences" -> Json.fromValues(Vector(
        _reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"),
        _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"),
        _reference("ref-opaque-element-enrichment", "element", "opaque-shared", "anchor-opaque-element-enrichment"),
        _reference("ref-opaque-element-kind-Mono", "element", "opaque-shared", "anchor-opaque-element-kind-Mono"),
        _reference("ref-opaque-relationship-enrichment", "relationship", "opaque-shared", "anchor-opaque-relationship-enrichment"),
        _reference("ref-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "anchor-opaque-relationship-role-StructuralDomain"),
        _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain")
      ).sortBy(_.hcursor.get[String]("referenceId").toOption.get)),
      "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(_artifact_json(_artifact_reference("snapshot-model"))))
    ))
  }

  private def _binding(profile: String, schema: String): Array[Byte] = {
    def _record_(kind: String, identity: String, role: String, ids: Vector[String]): Json = Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(ids.map(Json.fromString)),
      "conditionIds" -> Json.arr(),
      "enrichmentAssertionIds" -> Json.arr(),
      "recordKind" -> Json.fromString(kind),
      "semanticIdentity" -> Json.fromString(identity),
      "sequenceAssertionId" -> Json.Null,
      "viewRole" -> Json.fromString(role)
    )
    val families = Vector(
      "MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono")), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))),
      "UseCaseCommunicationProjection" -> Vector(_record_("element", "e-usecase", "use-case", Vector("a-e-usecase-kind-use-case"))),
      "EntityModelProjection" -> Vector.empty[Json],
      "EventModelProjection" -> Vector.empty[Json],
      "StructureViewProjection" -> Vector.empty[Json],
      "ClassificationViewProjection" -> Vector.empty[Json],
      "WorkflowProjection" -> Vector.empty[Json],
      "StateMachineProjection" -> Vector.empty[Json]
    )
    _canonical(Json.obj(
      "profile" -> Json.fromString(profile),
      "bindingReference" -> _record_json(_record_reference("binding-order", 23L)),
      "realizationArtifactReference" -> _artifact_json(_artifact_reference("realization-main")),
      "schemaVersion" -> Json.fromString(schema),
      "scope" -> _scope_json,
      "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })
    ))
  }

  private def _fact(kind: String, identity: String, anchor: String, content: String): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-order"),
      "content" -> Json.fromString(content),
      "limitations" -> Json.arr(),
      "projectionContextIdentity" -> Json.fromString("context-order"),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kind),
      "sourceAnchor" -> Json.fromString(anchor)
    )

  private def _assertion(id: String, kind: String, identity: String, referenceid: String, content: String, conditionids: Vector[String]): Json = {
    val fields = Vector(
      "assertionId" -> Json.fromString(id),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kind),
      "sourceReferenceId" -> Json.fromString(referenceid),
      "association" -> Json.Null
    )
    Json.obj(fields*)
  }

  private def _element(identity: String, kind: String, label: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)),
      "identity" -> Json.fromString(identity),
      "kind" -> Json.fromString(kind),
      "label" -> Json.fromString(label)
    )

  private def _relationship(identity: String, role: String, source: String, target: String, canonicalids: Vector[String], enrichmentids: Vector[String] = Vector.empty, conditionids: Vector[String] = Vector.empty): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "direction" -> Json.fromString("source-to-target"),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)),
      "identity" -> Json.fromString(identity),
      "label" -> Json.fromString(identity),
      "role" -> Json.fromString(role),
      "sourceElementIdentity" -> Json.fromString(source),
      "targetElementIdentity" -> Json.fromString(target)
    )

  private def _reference(id: String, kind: String, identity: String, anchor: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(id),
      "snapshotReference" -> _artifact_json(_artifact_reference("snapshot-model")),
      "source" -> _source_json(_model_source),
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind))
    )

  private def _condition_json(id: String, kind: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj(
      "affectedIdentity" -> Json.fromString(affectedidentity),
      "affectedKind" -> Json.fromString(affectedkind),
      "conditionId" -> Json.fromString(id),
      "detail" -> Json.fromString(detail),
      "kind" -> Json.fromString(kind),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _artifact(id: String, path: String, role: String, bytes: Array[Byte], dependencies: Vector[String]): Json =
    _artifact_with_dependencies(id, path, role, dependencies.sorted.map(_artifact_reference))

  private def _artifact_with_dependencies(id: String, path: String, role: String, dependencies: Vector[InternalModelArtifactReference]): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "artifactRevision" -> Json.fromLong(_artifact_reference(id).artifactRevision.value),
      "dependsOn" -> Json.fromValues(dependencies.map(_artifact_json)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(true),
      "role" -> Json.fromString(role)
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    def _order_(remaining: Vector[Json], preceding: Set[String], ordered: Vector[Json]): Vector[Json] = {
      if remaining.isEmpty then ordered
      else {
        val ready = remaining.filter { artifact =>
          artifact.hcursor.get[Vector[Json]]("dependsOn").toOption.get.forall(reference => preceding.contains(reference.hcursor.get[String]("artifactId").toOption.get))
        }.sortBy(_.hcursor.get[String]("artifactId").toOption.get)
        val selected = ready.head
        _order_(remaining.filterNot(_ == selected), preceding + selected.hcursor.get[String]("artifactId").toOption.get, ordered :+ selected)
      }
    }
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(_order_(artifacts, Set.empty, Vector.empty)),
      "lifecycleState" -> Json.fromString("draft"),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("candidate-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromLong(43L),
      "schemaVersion" -> Json.fromString("2.0")
    ))
    _canonical(root.toJson)
  }

  private val _scope_json: Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-order"),
      "projectionContextIdentity" -> Json.fromString("context-order"),
      "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")
    )

  private def _source_json(source: InternalModelSemanticSource): Json =
    Json.obj(
      "authority" -> Json.fromString(source.authority),
      "identity" -> Json.fromString(source.identity),
      "locator" -> source.locator.map(Json.fromString).getOrElse(Json.Null),
      "revision" -> source.revision.map(Json.fromString).getOrElse(Json.Null)
    )

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private val _artifact_versions = Map(
    "snapshot-cml-alpha" -> (11L, InternalModelArtifactRole.SourceSnapshot),
    "snapshot-cml-beta" -> (37L, InternalModelArtifactRole.SourceSnapshot),
    "snapshot-model" -> (41L, InternalModelArtifactRole.SourceSnapshot),
    "realization-main" -> (13L, InternalModelArtifactRole.Realization),
    "projection-main" -> (29L, InternalModelArtifactRole.Projection),
    "candidate-main" -> (17L, InternalModelArtifactRole.Projection),
    "semantic-diff-main" -> (47L, InternalModelArtifactRole.Projection),
    "semantic-diff-copy" -> (53L, InternalModelArtifactRole.Projection)
  )

  private def _artifact_reference(id: String): InternalModelArtifactReference = {
    val (revision, role) = _artifact_versions(id)
    InternalModelArtifactReference(InternalModelArtifactId.from(id).toOption.get, InternalModelArtifactRevision.from(revision).toOption.get, role)
  }

  private def _record_reference(id: String, revision: Long): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(id).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)

  private def _record_json(reference: InternalModelRecordReference): Json =
    Json.obj("recordId" -> Json.fromString(reference.recordId.value), "recordRevision" -> Json.fromLong(reference.recordRevision.value))

  private def _artifact_json(reference: InternalModelArtifactReference): Json =
    Json.obj("artifactId" -> Json.fromString(reference.artifactId.value), "artifactRevision" -> Json.fromLong(reference.artifactRevision.value), "role" -> Json.fromString(reference.role.wireValue))

  private def _temporary_root(): Path = {
    val target = Path.of("target/internal-model-semantic-diff/work")
    Files.createDirectories(target)
    Files.createTempDirectory(target, "fixture-")
  }

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root, LinkOption.NOFOLLOW_LINKS) then {
      val stream = Files.walk(root)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      finally stream.close()
    }
}
