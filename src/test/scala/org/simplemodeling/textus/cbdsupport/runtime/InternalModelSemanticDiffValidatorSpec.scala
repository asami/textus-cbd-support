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
final class InternalModelSemanticDiffValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8)
  private val _cml_alpha_raw = Vector[Byte](0, 0x7f, -1, 10)
  private val _cml_beta_raw = "Domain { Order }\n".getBytes(StandardCharsets.UTF_8).toVector
  private val _model_source = _source("model-authority", "model-source", Some("catalog/model-source"), Some("revision-1"), _sha256(_model_raw))
  private val _cml_alpha_source = _source("cml-authority", "cml-alpha", Some("cml/alpha.cml"), Some("revision-a"), _sha256(_cml_alpha_raw.toArray))
  private val _cml_beta_source = _source("cml-authority", "cml-beta", Some("cml/beta.cml"), Some("revision-b"), _sha256(_cml_beta_raw.toArray))

  "Internal-model semantic-diff admission" should {
    "admit the complete portable two-target package and reconstruct exact Phase 9 values" in {
      Given("a truthful package manifest, V1 continuity, two retained source snapshots, one candidate projection, and one semantic-diff projection")
      val expectedcandidate = InternalModelCandidateCmlProjectionCodec.encode(_default_projection()).toVector
      val expecteddiff = InternalModelSemanticDiffCodec.encode(_valid_diff()).toVector
      var admission = Option.empty[InternalModelSemanticDiffAdmission]

      When("the project root crosses the one-pass semantic-diff validator")
      _with_fixture() { root =>
        admission = InternalModelSemanticDiffValidator.validate(root).toOption
      }

      Then("candidate and diff identities, hashes, paths, captured inventory, raw bytes, mappings, traces, and typed entries remain exact")
      admission.map { value =>
        (
          value.diff.canonicalBytes,
          value.diff.copy(canonicalBytes = Vector.empty),
          value.diff.candidateArtifactId,
          value.diff.candidateArtifactSha256,
          value.diff.targets.map(target =>
            (
              target.targetId,
              target.patchTrace.id,
              target.patchTrace.baseDigest,
              target.patchTrace.proposedDigest,
              target.patchTrace.cmlLocator,
              target.entries.map(entry => (entry.entry.id, entry.mappingId, entry.semanticIdentityKind, entry.entry.subject))
            )
          ),
          value.diffArtifactId,
          value.diffArtifactSha256,
          value.diffPackageRelativePath,
          value.candidateAdmission.projection.canonicalBytes,
          value.candidateAdmission.targetBytes.map(bytes => (bytes.targetId, bytes.baseline.rawBytes, bytes.proposedRawBytes)),
          value.candidateAdmission.packageContext.artifacts.map(artifact => (artifact.artifactId, artifact.role, artifact.present))
        )
      } shouldBe Some((
        expecteddiff,
        _valid_diff().copy(canonicalBytes = Vector.empty),
        "candidate-main",
        _sha256(expectedcandidate.toArray),
        Vector(
          ("target-alpha", "patch-alpha", _sha256(_cml_alpha_raw.toArray), _sha256(Vector[Byte](0, -1, 10).toArray), "cml/alpha.cml", Vector(("entry-target-alpha", "mapping-alpha", "element", "opaque-shared"))),
          ("target-beta", "patch-beta", _sha256(_cml_beta_raw.toArray), _sha256(_cml_beta_raw.toArray), "cml/beta.cml", Vector(("entry-target-beta", "mapping-beta", "relationship", "opaque-shared")))
        ),
        "semantic-diff-main",
        _sha256(expecteddiff.toArray),
        "projections/semantic-diff.json",
        expectedcandidate,
        Vector(
          ("target-alpha", _cml_alpha_raw, Vector[Byte](0, -1, 10)),
          ("target-beta", _cml_beta_raw, _cml_beta_raw)
        ),
        Vector(
          ("snapshot-cml-alpha", "source-snapshot", true),
          ("snapshot-cml-beta", "source-snapshot", true),
          ("snapshot-model", "source-snapshot", true),
          ("realization-main", "realization", true),
          ("projection-main", "projection", true),
          ("candidate-main", "projection", true),
          ("semantic-diff-main", "projection", true)
        )
      ))
    }
  }

  "Internal-model semantic-diff evidence roundtrip" should {
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

          Then("all supplied typed evidence and byte-level canonical roundtrip values remain exact without inferred category or action")
          admission.map { value =>
            (
              value.diff.targets.head.entries.head.entry,
              value.diff.targets.head.patchTrace.condition,
              value.diff.targets.head.patchTrace.limitations,
              value.diff.targets.head.patchTrace.stableTieKey,
              InternalModelSemanticDiffCodec.encode(value.diff).toVector == value.diff.canonicalBytes
            )
          } shouldBe Some((expectedentry, condition, Vector("patch first", "patch first", "patch 第二"), patchtie, true))
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

      Then("raw bytes and their separate baseline/proposal digests remain exact traceability values")
      admission.map { value =>
        (
          value.candidateAdmission.targetBytes.map(bytes => (bytes.targetId, bytes.baseline.rawBytes, bytes.proposedRawBytes)),
          value.diff.targets.map(target => (target.targetId, target.patchTrace.baseDigest, target.patchTrace.proposedDigest))
        )
      } shouldBe Some((
        Vector(
          ("target-alpha", _cml_alpha_raw, alphabytes),
          ("target-beta", _cml_beta_raw, betabytes)
        ),
        Vector(
          ("target-alpha", _sha256(_cml_alpha_raw.toArray), _sha256(alphabytes.toArray)),
          ("target-beta", _sha256(_cml_beta_raw.toArray), _sha256(betabytes.toArray))
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
      val expectedcandidate = InternalModelCandidateCmlProjectionCodec.encode(_default_projection()).toVector
      val expecteddiff = InternalModelSemanticDiffCodec.encode(_valid_diff()).toVector
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

      Then("the original exact candidate/diff bytes and raw evidence survive capture while a fresh path read fails")
      capturedadmission.map(value => (
        value.candidateAdmission.projection.canonicalBytes,
        value.diff.canonicalBytes,
        value.candidateAdmission.targetBytes.map(bytes => (bytes.baseline.rawBytes, bytes.proposedRawBytes))
      )) shouldBe Some((
        expectedcandidate,
        expecteddiff,
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
    "admit V1 and V2 continuity families together with the selected semantic diff" in {
      Given("portable V1 and V2 realization/continuity fixtures, each with the same valid candidate and semantic-diff projections")
      val families = Vector(
        ("ccdm-projection-binding-v1", "1.0"),
        ("ccdm-projection-binding-v2", "2.0")
      )

      When("each predecessor family is selected before semantic-diff admission")
      val results = families.map { case (profile, schema) =>
        var succeeded = false
        _with_fixture(continuityprofile = profile, continuityschema = schema) { root =>
          succeeded = InternalModelSemanticDiffValidator.validate(root).isSuccess
        }
        succeeded
      }

      Then("both exact predecessor families remain compatible with the diff boundary")
      results shouldBe Vector(true, true)
    }

    "retain legacy candidate and continuity admission without a diff and reject absent or duplicate diff selections" in {
      Given("one legacy package without a semantic diff and two packages with respectively absent and duplicate semantic-diff projections")
      var candidateok = false
      var continuityok = false
      var absent = true
      var duplicate = true

      When("legacy readers and the requesting semantic-diff reader are applied")
      _with_fixture(includesemanticdiff = false) { root =>
        candidateok = InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess
        continuityok = InternalModelProjectionContinuityValidator.validate(root).isSuccess
      }
      absent = _admission_success(includesemanticdiff = false)
      duplicate = _admission_success(duplicatesemanticdiff = true)
      val legacy = (candidateok, continuityok)

      Then("legacy readers succeed, but a requested absent or duplicate diff fails closed")
      legacy shouldBe (true, true)
      absent shouldBe false
      duplicate shouldBe false
    }

    "fail unknown or wrong profile/schema headers even when the malformed family is not requested" in {
      Given("recognized continuity plus semantic-diff artifacts carrying unknown and wrong schema header pairs")
      val variants = Vector(
        "unknown profile" -> ((value: InternalModelSemanticDiff) => value.copy(profile = "ccdm-semantic-diff-next")),
        "wrong schema" -> ((value: InternalModelSemanticDiff) => value.copy(schemaVersion = "2.0"))
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
      Given("a canonical semantic-diff header whose candidateArtifactId is semantically wrong")
      var continuityok = false
      var diffok = true

      When("continuity selection and semantic-diff admission consume the same captured package")
      _with_fixture(diffchange = _.copy(candidateArtifactId = "candidate-not-selected")) { root =>
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
      Given("a successful control and a complete matrix of one-field semantic binding defects")
      val cases: Vector[(String, InternalModelSemanticDiff => InternalModelSemanticDiff)] = Vector(
        "control" -> ((value: InternalModelSemanticDiff) => value),
        "candidate artifact id" -> ((value: InternalModelSemanticDiff) => value.copy(candidateArtifactId = "candidate-other")),
        "candidate artifact hash" -> ((value: InternalModelSemanticDiff) => value.copy(candidateArtifactSha256 = _digest("candidate-other"))),
        "candidate identity" -> ((value: InternalModelSemanticDiff) => value.copy(candidateIdentity = "candidate-other")),
        "candidate model" -> ((value: InternalModelSemanticDiff) => value.copy(candidateModelIdentity = "candidate-model-other")),
        "candidate revision" -> ((value: InternalModelSemanticDiff) => value.copy(candidateRevision = 8)),
        "candidate scope" -> ((value: InternalModelSemanticDiff) => value.copy(scope = value.scope.copy(componentIdentity = "component-other"))),
        "target missing" -> ((value: InternalModelSemanticDiff) => value.copy(targets = value.targets.take(1))),
        "target extra" -> ((value: InternalModelSemanticDiff) => value.copy(targets = value.targets :+ value.targets.head.copy(targetId = "target-gamma"))),
        "target duplicate" -> ((value: InternalModelSemanticDiff) => value.copy(targets = Vector(value.targets.head, value.targets.head.copy(targetId = value.targets.head.targetId)))),
        "target order" -> ((value: InternalModelSemanticDiff) => value.copy(targets = value.targets.reverse)),
        "patch id" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(id = "patch-other"))),
        "patch context" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(context = MonoKotoProjectionContextIdentity("context-other")))),
        "patch component" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(component = ComponentDashboardComponentIdentity("component-other")))),
        "patch base raw-vs-snapshot digest" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(baseDigest = _sha256(_cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw))))),
        "patch proposed digest" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(proposedDigest = _digest("proposal-other")))),
        "patch source owner" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(cmlOwner = "owner-other"))),
        "patch available locator" -> ((value: InternalModelSemanticDiff) => _update_first_patch(value)(_.copy(cmlLocator = "locator-other"))),
        "entry context" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(context = MonoKotoProjectionContextIdentity("context-other")))),
        "entry component" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(component = ComponentDashboardComponentIdentity("component-other")))),
        "entry patch" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(patchId = "patch-other"))),
        "entry candidate model" -> ((value: InternalModelSemanticDiff) => _update_first_entry(value)(_.copy(candidateModelId = "candidate-model-other"))),
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

      When("each case is encoded with a freshly repinned manifest and admitted")
      val results = cases.map { case (name, change) => name -> _admission_success(change) }

      Then("only the truthful control succeeds; every semantic mismatch rejects without relying on a stale hash")
      results.head._2 shouldBe true
      results.tail.map(_._2) shouldBe Vector.fill(results.size - 1)(false)
    }

    "require the semantic-diff dependency set to be exactly the selected candidate" in {
      Given("a successful dependency control plus missing, extra, and reordered dependency declarations")
      val cases = Vector(
        None,
        Some(Vector.empty[String]),
        Some(Vector("candidate-main", "unexpected")),
        Some(Vector("unexpected", "candidate-main"))
      )

      When("each manifest dependency declaration is captured and admitted")
      val results = cases.map(dependencies => _admission_success(semanticdiffdependencies = dependencies))

      Then("only the exact selected-candidate dependency set succeeds")
      results shouldBe Vector(true, false, false, false)
    }
  }

  "Internal-model semantic-diff closed grammar" should {
    "reject transport, numeric, digest, profile, schema, and nested closed-grammar defects while preserving a canonical control" in {
      Given("one canonical diff plus deliberately raw noncanonical bytes and canonical re-rendered JSON mutations at every closed object depth")
      val value = _valid_diff()
      val control = InternalModelSemanticDiffCodec.encode(value)
      val transport = Vector(
        control,
        Array[Byte](0xc3.toByte, 0x28.toByte),
        Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ control,
        (" " + new String(control, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),
        control ++ Array('\n'.toByte),
        _swap_root_members(control),
        _duplicate_root_member(control)
      )
      val numeric = Vector(
        _bytes_replace(control, "\"candidateRevision\":7", "\"candidateRevision\":0"),
        _bytes_replace(control, "\"candidateRevision\":7", "\"candidateRevision\":-1"),
        _bytes_replace(control, "\"candidateRevision\":7", "\"candidateRevision\":2147483648"),
        _bytes_replace(control, "\"candidateRevision\":7", "\"candidateRevision\":\"7\""),
        _bytes_replace(control, "\"candidateRevision\":7", "\"candidateRevision\":7.0"),
        _bytes_replace(control, "\"candidateRevision\":7", "\"candidateRevision\":7e0")
      )
      val digests = Vector(
        _root_mutation(value)(_.add("candidateArtifactSha256", Json.fromString("SHA256:" + ("0" * 64)))),
        _root_mutation(value)(_.add("candidateArtifactSha256", Json.fromString("sha256:0"))),
        _root_mutation(value)(_.add("candidateArtifactSha256", Json.fromString("md5:" + ("0" * 64))))
      )
      val headers = Vector(
        _root_mutation(value)(_.add("profile", Json.fromString("ccdm-semantic-diff-next"))),
        _root_mutation(value)(_.add("schemaVersion", Json.fromString("2.0")))
      )
      val nested = _grammar_variants(value).map(_._2)

      When("the control and each raw or canonicalized mutation crosses package capture and semantic validation")
      val results = transport.map(_admission_bytes) ++ numeric.map(_admission_bytes) ++ digests.map(_admission_bytes) ++ headers.map(_admission_bytes) ++ nested.map(_admission_bytes)

      Then("only the first canonical control is admitted and every malformed, noncanonical, typed, digest, header, or closed-field variant rejects")
      results.head shouldBe true
      results.tail shouldBe Vector.fill(results.size - 1)(false)
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

  private def _default_projection(
    alphasource: InternalModelSemanticSource = _cml_alpha_source,
    betasource: InternalModelSemanticSource = _cml_beta_source
  ): InternalModelCandidateCmlProjection =
    _projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw, alphasource = alphasource, betasource = betasource)

  private def _default_candidate_bytes(): Array[Byte] =
    InternalModelCandidateCmlProjectionCodec.encode(_default_projection())

  private def _valid_diff(): InternalModelSemanticDiff =
    _semantic_diff(_default_projection(), _default_candidate_bytes())

  private def _semantic_diff(
    projection: InternalModelCandidateCmlProjection,
    candidatebytes: Array[Byte]
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
        target.source.sha256,
        target.proposedContent.sha256,
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
      "candidate-main",
      _sha256(candidatebytes),
      projection.candidateIdentity,
      projection.candidateModelIdentity,
      projection.candidateRevision,
      "ccdm-semantic-diff-v1",
      "1.0",
      InternalModelSemanticScope(
        projection.scope.componentIdentity,
        projection.scope.projectionContextIdentity,
        projection.scope.selectedUseCaseElementIdentity
      ),
      "semantic-diff-order",
      3,
      targets,
      Vector.empty
    )
  }

  private def _admission_success(
    diffchange: InternalModelSemanticDiff => InternalModelSemanticDiff = (value: InternalModelSemanticDiff) => value,
    semanticdiffdependencies: Option[Vector[String]] = None,
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
      succeeded = InternalModelSemanticDiffValidator.validate(root).isSuccess
    }
    succeeded
  }

  private def _admission_bytes(bytes: Array[Byte]): Boolean = {
    var succeeded = false
    _with_fixture(semanticdiffbytes = Some(bytes)) { root =>
      succeeded = InternalModelSemanticDiffValidator.validate(root).isSuccess
    }
    succeeded
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
      "root missing" -> _root_mutation(value)(_.remove("candidateIdentity")),
      "root type" -> _root_mutation(value)(_.add("candidateRevision", Json.fromString("7"))),
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
    val content = new String(bytes, StandardCharsets.UTF_8).stripSuffix("\n")
    val remainder = content.substring(1)
    val firstcomma = remainder.indexOf(',')
    val first = remainder.substring(0, firstcomma)
    val afterfirst = remainder.substring(firstcomma + 1)
    val secondcomma = afterfirst.indexOf(',')
    val second = afterfirst.substring(0, secondcomma)
    val rest = afterfirst.substring(secondcomma + 1)
    ("{" + second + "," + first + "," + rest + "\n").getBytes(StandardCharsets.UTF_8)
  }

  private def _duplicate_root_member(bytes: Array[Byte]): Array[Byte] = {
    val content = new String(bytes, StandardCharsets.UTF_8).stripSuffix("\n")
    val remainder = content.substring(1)
    val firstcomma = remainder.indexOf(',')
    val first = remainder.substring(0, firstcomma)
    val rest = remainder.substring(firstcomma + 1)
    ("{" + first + "," + first + "," + rest + "\n").getBytes(StandardCharsets.UTF_8)
  }

  private def _bytes_replace(bytes: Array[Byte], before: String, after: String): Array[Byte] =
    new String(bytes, StandardCharsets.UTF_8).replace(before, after).getBytes(StandardCharsets.UTF_8)

  private def _with_fixture(
    includecandidate: Boolean = true,
    includesemanticdiff: Boolean = true,
    duplicatesemanticdiff: Boolean = false,
    semanticdiffdependencies: Option[Vector[String]] = None,
    semanticdiffbytes: Option[Array[Byte]] = None,
    diffchange: InternalModelSemanticDiff => InternalModelSemanticDiff = (value: InternalModelSemanticDiff) => value,
    continuityprofile: String = "ccdm-projection-binding-v1",
    continuityschema: String = "1.0",
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
    val realizationprofile = if continuityprofile == "ccdm-projection-binding-v2" then "ccdm-realization-v2" else "ccdm-realization-v1"
    val realization = _realization(realizationprofile)
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
    val diffvalue = diffchange(_semantic_diff(candidateprojection, candidate))
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
      val dependencies = semanticdiffdependencies.getOrElse(Vector(candidateid))
      val primary = _artifact("semantic-diff-main", "projections/semantic-diff.json", "projection", diff, dependencies)
      if duplicatesemanticdiff then
        Vector(_artifact("semantic-diff-copy", "projections/semantic-diff-copy.json", "projection", diff, dependencies), primary)
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
      candidateIdentity = candidateidentity,
      candidateModelIdentity = "candidate-model-" + label,
      candidateRevision = 7,
      continuityArtifactId = continuityid,
      profile = "ccdm-candidate-cml-projection-v1",
      realizationArtifactId = "realization-main",
      schemaVersion = "1.0",
      scope = InternalModelSemanticScope("component-order", "context-order", "e-usecase"),
      targets = Vector(alpha, beta),
      canonicalBytes = Vector.empty
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
    InternalModelCandidateCmlTarget(baselineid, effects, Vector(mapping), patchidentity, path, _content(rawbytes), source, targetid)
  }

  private def _content(bytes: Vector[Byte]): InternalModelCandidateCmlContent =
    InternalModelCandidateCmlContent(bytes.length.toLong, Base64.getEncoder.encodeToString(bytes.toArray), _sha256(bytes.toArray))

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

  private def _source(authority: String, identity: String, locator: Option[String], revision: Option[String], sha256: String): InternalModelSemanticSource =
    InternalModelSemanticSource(authority, identity, locator, revision, sha256)

  private def _digest(seed: String): String =
    "sha256:" + seed.hashCode.toLong.abs.toHexString.reverse.padTo(64, '0').reverse

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
      "schemaVersion" -> Json.fromString("1.0"),
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
      "schemaVersion" -> Json.fromString("1.0"),
      "snapshotKind" -> Json.fromString("cml-baseline"),
      "source" -> _source_json(source)
    ))

  private def _realization(profile: String): Array[Byte] = {
    val schema = if profile == "ccdm-realization-v2" then "2.0" else "1.0"
    val v2 = profile == "ccdm-realization-v2"
    val assertions = Vector(
      _assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", Vector.empty, v2),
      _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", Vector.empty, v2),
      _assertion("a-opaque-element-kind-Mono", "element", "opaque-shared", "ref-opaque-element-kind-Mono", "kind:Mono", Vector("c-opaque-element"), v2),
      _assertion("a-opaque-relationship-role-StructuralDomain", "relationship", "opaque-shared", "ref-opaque-relationship-role-StructuralDomain", "role:StructuralDomain", Vector("c-opaque-relationship"), v2),
      _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", Vector.empty, v2)
    ).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val enrichment = Vector(
      _assertion("z-opaque-element-enrichment", "element", "opaque-shared", "ref-opaque-element-enrichment", "enrichment:shared", Vector("c-opaque-element"), v2),
      _assertion("z-opaque-relationship-enrichment", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "enrichment:shared", Vector("c-opaque-relationship"), v2)
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
      "realizationIdentity" -> Json.fromString("realization-" + schema),
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
      "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
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
      "realizationArtifactId" -> Json.fromString("realization-main"),
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

  private def _assertion(id: String, kind: String, identity: String, referenceid: String, content: String, conditionids: Vector[String], v2: Boolean): Json = {
    val fields = Vector(
      "assertionId" -> Json.fromString(id),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kind),
      "sourceReferenceId" -> Json.fromString(referenceid)
    ) ++ Option.when(v2)("association" -> Json.Null)
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
      "snapshotArtifactId" -> Json.fromString("snapshot-model"),
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
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "dependsOn" -> Json.fromValues(dependencies.sorted.map(Json.fromString)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(true),
      "role" -> Json.fromString(role),
      "sha256" -> Json.fromString(_sha256(bytes))
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("candidate-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(1),
      "schemaVersion" -> Json.fromString("1.0")
    ))
    _canonical(root.add("packageDigest", Json.fromString(_sha256(_canonical(root.remove("packageDigest").toJson)))).toJson)
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
      "revision" -> source.revision.map(Json.fromString).getOrElse(Json.Null),
      "sha256" -> Json.fromString(source.sha256)
    )

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map { byte =>
      val text = Integer.toHexString(byte & 0xff)
      if text.length == 1 then "0" + text else text
    }.mkString

  private def _temporary_root(): Path = {
    val target = Path.of("target")
    Files.createDirectories(target)
    Files.createTempDirectory(target, "internal-model-semantic-diff-")
  }

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root) then {
      val stream = Files.walk(root)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      finally stream.close()
    }
}
