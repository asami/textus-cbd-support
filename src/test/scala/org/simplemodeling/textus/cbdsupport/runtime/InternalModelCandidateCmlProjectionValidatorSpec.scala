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
final class InternalModelCandidateCmlProjectionValidatorSpec
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

  "Internal-model candidate CML projection admission" should {
    "reopen multi-target exact proposal bytes and retained realization evidence" in {
      Given("a portable package with V1 continuity, two CML baselines, element and relationship mappings, and both expected-effect kinds")
      _with_fixture(includecandidate = true) { root =>
        When("the candidate is admitted from one project-bound package handoff")
        val result = InternalModelCandidateCmlProjectionValidator.validate(root)

        Then("candidate/model/revision/patch identities, exact baseline and proposed bytes, mapping lanes, effects, and source evidence remain supplied and distinct")
        result.toOption.map { admission =>
          (
            admission.projection.candidateIdentity,
            admission.projection.candidateModelIdentity,
            admission.projection.candidateRevision,
            admission.targetBytes.map(bytes => (bytes.targetId, bytes.baseline.artifactId, bytes.baseline.rawBytes, bytes.proposedRawBytes)),
            admission.projection.targets.map(target => (target.patchIdentity, target.mappings.map(mapping => (mapping.semanticIdentityKind, mapping.semanticIdentity, mapping.cmlSemanticIdentity, mapping.canonicalAssertionIds, mapping.enrichmentAssertionIds, mapping.conditionIds)), target.effects.map(effect => (effect.kind, effect.assessment, effect.sourceReferenceId)))),
            admission.continuity.realization.sourceReferences.map(_.referenceId)
          )
        } shouldBe Some((
          "candidate-order-v1",
          "candidate-model-order",
          7,
          Vector(
            ("target-alpha", "snapshot-cml-alpha", _cml_alpha_raw, Vector[Byte](0, -1, 10)),
            ("target-beta", "snapshot-cml-beta", _cml_beta_raw, _cml_beta_raw)
          ),
          Vector(
            ("patch-alpha", Vector(("element", "opaque-shared", "cml-é", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element"))), Vector(("compatibility", "unknown", "ref-opaque-element-kind-Mono"), ("migration", "required", "ref-opaque-element-enrichment"))),
            ("patch-beta", Vector(("relationship", "opaque-shared", "cml-é", Vector("a-opaque-relationship-role-StructuralDomain"), Vector("z-opaque-relationship-enrichment"), Vector("c-opaque-relationship"))), Vector(("compatibility", "breaking", "ref-opaque-relationship-role-StructuralDomain"), ("migration", "unknown", "ref-opaque-relationship-enrichment")))
          ),
          Vector("ref-e-mono-kind-Mono", "ref-e-usecase-kind-use-case", "ref-opaque-element-enrichment", "ref-opaque-element-kind-Mono", "ref-opaque-relationship-enrichment", "ref-opaque-relationship-role-StructuralDomain", "ref-r-mono-domain-role-StructuralDomain")
        ))
      }
    }

    "retain exact candidate canonical bytes and external package identity without self-referential JSON digests" in {
      Given("a canonical candidate profile and its separately verified package inventory")
      _with_fixture(includecandidate = true) { root =>
        When("the candidate codec and package admission run")
        val result = InternalModelCandidateCmlProjectionValidator.validate(root)

        Then("the decoder re-encodes exact candidate bytes while package revision, digest, manifest bytes, paths, and hashes stay external captured evidence")
        result.toOption.map { admission =>
          (
            InternalModelCandidateCmlProjectionCodec.encode(admission.projection).toVector == admission.projection.canonicalBytes,
            admission.packageContext,
            admission.candidateArtifactId,
            admission.candidatePackageRelativePath,
            admission.candidateArtifactSha256,
            admission.targetBytes.map(bytes => (bytes.baseline.artifactId, bytes.baseline.source.sha256, bytes.baseline.snapshotSha256, bytes.baseline.rawBytes, bytes.proposedRawBytes)),
            new String(admission.projection.canonicalBytes.toArray, StandardCharsets.UTF_8).contains("candidateArtifactSha256")
          )
        } shouldBe Some((
          true,
          _expected_context(),
          "candidate-main",
          "projections/candidate.json",
          _sha256(InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw))),
          Vector(
            ("snapshot-cml-alpha", _sha256(_cml_alpha_raw.toArray), _sha256(_cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw)), _cml_alpha_raw, Vector[Byte](0, -1, 10)),
            ("snapshot-cml-beta", _sha256(_cml_beta_raw.toArray), _sha256(_cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw)), _cml_beta_raw, _cml_beta_raw)
          ),
          false
        ))
      }
    }

    "preserve continuity-family selection when a recognized candidate family is added or absent" in {
      Given("otherwise matching V1 continuity fixtures with and without one canonical candidate projection")
      _with_fixture(includecandidate = true) { candidateroot =>
        _with_fixture(includecandidate = false) { continuityroot =>
          When("continuity is selected independently of the candidate family")
          val withcandidate = InternalModelProjectionContinuityValidator.validate(candidateroot)
          val withoutcandidate = InternalModelProjectionContinuityValidator.validate(continuityroot)

          Then("both paths retain the same V1 binding and realization bytes and construct the same eight continuity values")
          (for {
            left <- withcandidate.toOption
            right <- withoutcandidate.toOption
          } yield (
            left.realization.canonicalBytes == right.realization.canonicalBytes,
            left.binding.canonicalBytes == right.binding.canonicalBytes,
            (
              left.monoKoto == right.monoKoto,
              left.useCaseCommunication == right.useCaseCommunication,
              left.entityModel == right.entityModel,
              left.eventModel == right.eventModel,
              left.structureView == right.structureView,
              left.classificationView == right.classificationView,
              left.workflow == right.workflow,
              left.stateMachine == right.stateMachine
            )
          )) shouldBe Some((true, true, (true, true, true, true, true, true, true, true)))
        }
      }
    }

    "select exact V1 and V2 continuity-family bytes without using candidate path or encounter order" in {
      Given("portable V1 and V2 package inventories, each with matching candidate-present and candidate-absent variants")
      _with_fixture(includecandidate = true, continuityprofile = "ccdm-projection-binding-v1", continuityschema = "1.0") { v1candidateroot =>
        _with_fixture(includecandidate = false, continuityprofile = "ccdm-projection-binding-v1", continuityschema = "1.0") { v1continuityroot =>
          _with_fixture(includecandidate = true, continuityprofile = "ccdm-projection-binding-v2", continuityschema = "2.0") { v2candidateroot =>
            _with_fixture(includecandidate = false, continuityprofile = "ccdm-projection-binding-v2", continuityschema = "2.0") { v2continuityroot =>
              When("each family is admitted through candidate and continuity readers before its exact selected bytes are compared")
              val selected = Vector(
                InternalModelCandidateCmlProjectionValidator.validate(v1candidateroot).toOption.map(value => (value.continuity.realization.profile, value.continuity.binding.profile, value.continuity.realization.canonicalBytes, value.continuity.binding.canonicalBytes)),
                InternalModelProjectionContinuityValidator.validate(v1continuityroot).toOption.map(value => (value.realization.profile, value.binding.profile, value.realization.canonicalBytes, value.binding.canonicalBytes)),
                InternalModelCandidateCmlProjectionValidator.validate(v2candidateroot).toOption.map(value => (value.continuity.realization.profile, value.continuity.binding.profile, value.continuity.realization.canonicalBytes, value.continuity.binding.canonicalBytes)),
                InternalModelProjectionContinuityValidator.validate(v2continuityroot).toOption.map(value => (value.realization.profile, value.binding.profile, value.realization.canonicalBytes, value.binding.canonicalBytes))
              )

              Then("V1/V2 profile and canonical realization/binding bytes remain exact and candidate presence does not alter the constructed continuity")
              selected shouldBe Vector(
                Some(("ccdm-realization-v1", "ccdm-projection-binding-v1", _realization("ccdm-realization-v1").toVector, _binding("ccdm-projection-binding-v1", "1.0").toVector)),
                Some(("ccdm-realization-v1", "ccdm-projection-binding-v1", _realization("ccdm-realization-v1").toVector, _binding("ccdm-projection-binding-v1", "1.0").toVector)),
                Some(("ccdm-realization-v2", "ccdm-projection-binding-v2", _realization("ccdm-realization-v2").toVector, _binding("ccdm-projection-binding-v2", "2.0").toVector)),
                Some(("ccdm-realization-v2", "ccdm-projection-binding-v2", _realization("ccdm-realization-v2").toVector, _binding("ccdm-projection-binding-v2", "2.0").toVector))
              )
            }
          }
        }
      }
    }

    "select family content despite misleading projection IDs and swapped-looking safe paths" in {
      Given("a canonical inventory whose candidate has a lexically early ID but late dependency and whose continuity/candidate paths suggest the opposite family")
      _with_fixture(
        includecandidate = true,
        continuityid = "z-continuity",
        continuitypath = "projections/aaa-candidate-looking.json",
        candidateid = "a-candidate",
        candidatepath = "projections/zzz-continuity-looking.json"
      ) { root =>
        When("the candidate package handoff classifies canonical profile/schema bytes rather than IDs, paths, or inventory encounter")
        val result = InternalModelPackageValidator.verifiedCandidateCmlProjection(root)

        Then("the exact selected family IDs, paths, and bytes are those carried by their recognized headers")
        result.toOption.map(value => (
          value.continuityPackage.projection.artifactId,
          value.continuityPackage.projection.packageRelativePath,
          value.continuityPackage.projection.bytes,
          value.candidate.artifactId,
          value.candidate.packageRelativePath,
          value.candidate.bytes
        )) shouldBe Some((
          "z-continuity",
          "projections/aaa-candidate-looking.json",
          _binding("ccdm-projection-binding-v1", "1.0").toVector,
          "a-candidate",
          "projections/zzz-continuity-looking.json",
          InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw, continuityid = "z-continuity")).toVector
        ))
      }
    }

    "reuse one captured handoff after fixture-owned paths change" in {
      Given("a verified candidate handoff whose manifest, project descriptor, and selected artifacts are all fixture-owned")
      _with_fixture(includecandidate = true) { root =>
        val captured = InternalModelPackageValidator.verifiedCandidateCmlProjection(root)

        When("only the mutable fixture paths are removed after capture")
        _delete_tree(root.resolve("src/main/internal-model"))
        Files.delete(root.resolve("project.yaml"))
        val capturedadmission = captured.flatMap(InternalModelCandidateCmlProjectionValidator.validateVerified)
        val freshadmission = InternalModelCandidateCmlProjectionValidator.validate(root)

        Then("captured candidate, realization, binding, baseline, inventory, and manifest bytes remain the original captured values while a fresh admission cannot reuse removed paths")
        capturedadmission.toOption.map { value =>
          (
            value.projection.canonicalBytes,
            value.continuity.realization.canonicalBytes,
            value.continuity.binding.canonicalBytes,
            value.packageContext,
            value.targetBytes.map(bytes => (bytes.baseline.rawBytes, bytes.baseline.snapshotSha256, bytes.proposedRawBytes))
          )
        } shouldBe Some((
          InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw)).toVector,
          _realization("ccdm-realization-v1").toVector,
          _binding("ccdm-projection-binding-v1", "1.0").toVector,
          _expected_context(),
          Vector(
            (_cml_alpha_raw, _sha256(_cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw)), Vector[Byte](0, -1, 10)),
            (_cml_beta_raw, _sha256(_cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw)), _cml_beta_raw)
          )
        ))
        freshadmission.isSuccess shouldBe false
      }
    }

    "distinguish captured original bytes from an independently coherent replacement package" in {
      Given("one captured original handoff and a separately created portable replacement with a new supplied candidate identity")
      _with_fixture(includecandidate = true) { originalroot =>
        val captured = InternalModelPackageValidator.verifiedCandidateCmlProjection(originalroot)
        _with_fixture(includecandidate = true, candidatebytes = Some(InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](9), "replacement", _cml_beta_raw, "candidate-replacement")))) { replacementroot =>
          When("the original fixture package is removed and the independent replacement is freshly admitted")
          _delete_tree(originalroot.resolve("src/main/internal-model"))
          Files.delete(originalroot.resolve("project.yaml"))
          val original = captured.flatMap(InternalModelCandidateCmlProjectionValidator.validateVerified)
          val replacement = InternalModelCandidateCmlProjectionValidator.validate(replacementroot)

          Then("captured admission retains its original identity/bytes while fresh admission observes only the coherent replacement")
          original.toOption.map(_.projection.candidateIdentity) shouldBe Some("candidate-order-v1")
          original.toOption.map(_.projection.canonicalBytes) shouldBe Some(InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw)).toVector)
          replacement.toOption.map(value => (value.projection.candidateIdentity, value.targetBytes.head.proposedRawBytes)) shouldBe Some(("candidate-replacement", Vector[Byte](9)))
        }
      }
    }

    "retain arbitrary proposed bytes and supplied opaque identities without inferring a CML change" in {
      Given("bounded generated raw bytes, independent opaque candidate/model/destination identities, Unicode effect detail, and normal or reverse precanonical encounter order for the two ASCII target records")
      forAll(
        Gen.listOf(Gen.chooseNum(-128, 127)).map(_.take(32).map(_.toByte).toVector),
        Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString),
        Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString),
        Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString),
        Gen.oneOf("表示の詳細", "Δ detail", "Å human detail"),
        Gen.oneOf(false, true)
      ) { (rawbytes, candidateidentity, modelidentity, destinationidentity, humandetail, reverseorder) =>
        val base = _projection(rawbytes, destinationidentity, _cml_alpha_raw, candidateidentity).copy(candidateModelIdentity = modelidentity)
        val fixturetargets = base.targets.map { target =>
          target.copy(effects = target.effects.updated(0, target.effects.head.copy(detail = humandetail)))
        }
        val encountertargets = if reverseorder then fixturetargets.reverse else fixturetargets
        val oppositeencountertargets = encountertargets.reverse
        val firstfixture = _canonical_fixture_targets(base.copy(targets = encountertargets))
        val secondfixture = _canonical_fixture_targets(base.copy(targets = oppositeencountertargets))

        When("only the fixture target encounter arrays are canonicalized before the strict codec decodes both opposite supplied orders")
        val firstencoded = InternalModelCandidateCmlProjectionCodec.encode(firstfixture)
        val secondencoded = InternalModelCandidateCmlProjectionCodec.encode(secondfixture)
        val decoded = InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(firstencoded))

        Then("both fixture encounter orders yield equal canonical bytes, and decoding retains exact raw bytes, independently supplied candidate/model/destination identities, selected semantic identity, and Unicode human detail")
        decoded.toOption.map { value =>
          (
            value.candidateIdentity,
            value.candidateModelIdentity,
            Base64.getDecoder.decode(value.targets.head.proposedContent.rawBytesBase64).toVector,
            value.targets.head.mappings.head.cmlSemanticIdentity,
            value.targets.head.mappings.head.semanticIdentity,
            value.targets.head.effects.head.detail,
            firstencoded.toVector == secondencoded.toVector,
            InternalModelCandidateCmlProjectionCodec.encode(value).toVector == firstencoded.toVector
          )
        } shouldBe Some((candidateidentity, modelidentity, rawbytes, s"cml-$destinationidentity", "opaque-shared", humandetail, true, true))
      }
    }

    "admit explicit empty, arbitrary non-UTF8, and equal-baseline proposal bytes" in {
      Given("three otherwise coherent candidate artifacts whose supplied alpha proposal bytes are empty, non-UTF8 binary, or exactly its baseline")
      val proposals = Vector(Vector.empty[Byte], Vector[Byte](0, -1, 0x7f), _cml_alpha_raw)
      val results = proposals.map { rawbytes =>
        var result: org.goldenport.Consequence[InternalModelCandidateCmlAdmission] = null
        _with_fixture(includecandidate = true, candidatebytes = Some(InternalModelCandidateCmlProjectionCodec.encode(_projection(rawbytes, "raw", _cml_beta_raw)))) { root =>
          When("the portable candidate is admitted without parsing or normalizing CML")
          result = InternalModelCandidateCmlProjectionValidator.validate(root)
        }
        result
      }

      Then("each exact supplied byte vector is retained and no byte relationship infers a CML syntax, deletion, or semantic result")
      results.map(_.toOption.map(_.targetBytes.head.proposedRawBytes)) shouldBe proposals.map(Some(_))
    }

    "use unsigned UTF-8 byte order rather than JVM UTF-16 order for supplied ID arrays" in {
      Given("one canonical candidate whose assertion-ID array is BMP-before-supplementary in unsigned UTF-8 order and its reversed counterpart")
      val bmp = "\uE000"
      val supplementary = "\uD800\uDC00"
      val ordered = _projection(Vector[Byte](1), "order", _cml_beta_raw).copy(targets = _projection(Vector[Byte](1), "order", _cml_beta_raw).targets.map { target =>
        target.copy(mappings = target.mappings.map(mapping => mapping.copy(canonicalAssertionIds = Vector(bmp, supplementary))) )
      })
      val reversed = ordered.copy(targets = ordered.targets.map(target => target.copy(mappings = target.mappings.map(mapping => mapping.copy(canonicalAssertionIds = Vector(supplementary, bmp))))) )

      When("both supplied canonical encodings reach the strict codec")
      val orderedresult = InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(InternalModelCandidateCmlProjectionCodec.encode(ordered)))
      val reversedresult = InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(InternalModelCandidateCmlProjectionCodec.encode(reversed)))

      Then("the UTF-8 ordered array is retained and the UTF-16-only reversed array is rejected")
      orderedresult.toOption.map(_.targets.head.mappings.head.canonicalAssertionIds) shouldBe Some(Vector(bmp, supplementary))
      reversedresult.isLeft shouldBe true
    }

    "fail closed for the candidate profile's structural, selection, byte, and reference matrix" which {
      "reject noncanonical headers and all closed schema/value forms before semantic admission" in {
        Given("candidate bytes with BOM, duplicate, whitespace, unknown-field, fractional, empty, Base64, digest, and ordering mutations")
        val canonical = InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](1, 2), "é", _cml_alpha_raw))
        val mutations = Vector(
          Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical,
          canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8),
          "{\"candidateIdentity\":\"x\",\"candidateIdentity\":\"y\"}\n".getBytes(StandardCharsets.UTF_8),
          _canonical(_candidate_json(_projection(Vector[Byte](1), "é", _cml_alpha_raw)).mapObject(_.add("extra", Json.fromString("x")))),
          _canonical(_candidate_json(_projection(Vector[Byte](1), "é", _cml_alpha_raw)).mapObject(_.add("candidateRevision", Json.fromDoubleOrNull(1.5)))),
          _canonical(_candidate_json(_projection(Vector[Byte](1), "é", _cml_alpha_raw)).mapObject(_.add("candidateIdentity", Json.fromString(""))))
        )

        When("each bytes/schema mutation reaches the strict decoder")
        val results = mutations.map(bytes => InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(bytes)))

        Then("every invalid boundary is a structured decode failure rather than a repaired model or crash")
        results.forall(_.isLeft) shouldBe true
      }

      "admit only the exact canonical candidate header and byte spelling at the codec boundary" in {
        Given("one valid candidate admission control and named byte forms with missing, mistyped, unsupported, malformed, duplicate, reordered, or whitespace-bearing candidate headers")
        val candidate = _projection(Vector[Byte](1), "header", _cml_beta_raw)
        val canonical = InternalModelCandidateCmlProjectionCodec.encode(candidate)
        val cases = Vector(
          "profile-missing" -> _root_mutation(candidate)(_.remove("profile")),
          "profile-wrong-type" -> _root_mutation(candidate)(_.add("profile", Json.fromInt(1))),
          "profile-unknown" -> _root_mutation(candidate)(_.add("profile", Json.fromString("ccdm-candidate-cml-projection-next"))),
          "schema-missing" -> _root_mutation(candidate)(_.remove("schemaVersion")),
          "schema-wrong-type" -> _root_mutation(candidate)(_.add("schemaVersion", Json.fromInt(1))),
          "profile-schema-wrong-pair" -> _root_mutation(candidate)(_.add("schemaVersion", Json.fromString("2.0"))),
          "schema-unknown-version" -> _root_mutation(candidate)(_.add("schemaVersion", Json.fromString("1.1"))),
          "invalid-utf8" -> (Array[Byte](0xc3.toByte, 0x28.toByte) ++ canonical),
          "utf8-bom" -> (Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical),
          "malformed-json" -> "{not-json}\n".getBytes(StandardCharsets.UTF_8),
          "duplicate-mapping-member" -> _bytes_replace(canonical, "\"mappingId\":\"mapping-alpha\"", "\"mappingId\":\"mapping-alpha\",\"mappingId\":\"mapping-shadow\""),
          "duplicate-source-member" -> _bytes_replace(canonical, "\"authority\":\"cml-authority\",\"identity\":\"cml-alpha\"", "\"authority\":\"cml-authority\",\"authority\":\"other-authority\",\"identity\":\"cml-alpha\""),
          "duplicate-effect-member" -> _bytes_replace(canonical, "\"effectId\":\"effect-alpha-compatibility\"", "\"effectId\":\"effect-alpha-compatibility\",\"effectId\":\"effect-shadow\""),
          "unsorted-root-keys" -> _bytes_replace(canonical, "\"candidateIdentity\":\"candidate-order-v1\",\"candidateModelIdentity\":\"candidate-model-order\"", "\"candidateModelIdentity\":\"candidate-model-order\",\"candidateIdentity\":\"candidate-order-v1\""),
          "unsorted-mapping-keys" -> _bytes_replace(canonical, "\"canonicalAssertionIds\":[\"a-opaque-element-kind-Mono\"],\"cmlAnchor\":\"anchor-target-alpha\"", "\"cmlAnchor\":\"anchor-target-alpha\",\"canonicalAssertionIds\":[\"a-opaque-element-kind-Mono\"]"),
          "missing-final-lf" -> canonical.dropRight(1),
          "excess-final-lf" -> (canonical ++ "\n".getBytes(StandardCharsets.UTF_8)),
          "insignificant-whitespace" -> _bytes_replace(canonical, "\"profile\":", "\"profile\" : ")
        )

        When("the valid package is admitted and each named byte form reaches the strict candidate codec without an inventory reader")
        val admitted = _fixture_control()
        val control = InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(canonical))
        val results = cases.map { case (name, bytes) => name -> InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(bytes)) }

        Then("the exact canonical control decodes and admits, while each header, nested duplicate, UTF-8, JSON, key-order, and final-byte variation is a structured decode failure")
        admitted shouldBe true
        control.isRight shouldBe true
        results.forall(_._2.isLeft) shouldBe true
      }

      "repin each malformed or unknown candidate header while continuity selection fails after structure succeeds" in {
        Given("a canonical candidate control and independently repinned profile/schema, UTF-8, JSON, duplicate-member, ordering, and final-byte header variants")
        val candidate = _projection(Vector[Byte](1), "header-family", _cml_beta_raw)
        val canonical = InternalModelCandidateCmlProjectionCodec.encode(candidate)
        val cases = Vector(
          "profile-missing" -> _root_mutation(candidate)(_.remove("profile")),
          "profile-wrong-type" -> _root_mutation(candidate)(_.add("profile", Json.fromInt(1))),
          "profile-unknown" -> _root_mutation(candidate)(_.add("profile", Json.fromString("ccdm-candidate-cml-projection-next"))),
          "schema-missing" -> _root_mutation(candidate)(_.remove("schemaVersion")),
          "schema-wrong-type" -> _root_mutation(candidate)(_.add("schemaVersion", Json.fromInt(1))),
          "profile-schema-wrong-pair" -> _root_mutation(candidate)(_.add("schemaVersion", Json.fromString("2.0"))),
          "schema-unknown-version" -> _root_mutation(candidate)(_.add("schemaVersion", Json.fromString("1.1"))),
          "invalid-utf8" -> (Array[Byte](0xc3.toByte, 0x28.toByte) ++ canonical),
          "utf8-bom" -> (Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical),
          "malformed-json" -> "{not-json}\n".getBytes(StandardCharsets.UTF_8),
          "duplicate-nested-mapping-member" -> _bytes_replace(canonical, "\"mappingId\":\"mapping-alpha\"", "\"mappingId\":\"mapping-alpha\",\"mappingId\":\"mapping-shadow\""),
          "duplicate-nested-source-member" -> _bytes_replace(canonical, "\"authority\":\"cml-authority\",\"identity\":\"cml-alpha\"", "\"authority\":\"cml-authority\",\"authority\":\"other-authority\",\"identity\":\"cml-alpha\""),
          "duplicate-nested-effect-member" -> _bytes_replace(canonical, "\"effectId\":\"effect-alpha-compatibility\"", "\"effectId\":\"effect-alpha-compatibility\",\"effectId\":\"effect-shadow\""),
          "unsorted-root-keys" -> _bytes_replace(canonical, "\"candidateIdentity\":\"candidate-order-v1\",\"candidateModelIdentity\":\"candidate-model-order\"", "\"candidateModelIdentity\":\"candidate-model-order\",\"candidateIdentity\":\"candidate-order-v1\""),
          "unsorted-mapping-keys" -> _bytes_replace(canonical, "\"canonicalAssertionIds\":[\"a-opaque-element-kind-Mono\"],\"cmlAnchor\":\"anchor-target-alpha\"", "\"cmlAnchor\":\"anchor-target-alpha\",\"canonicalAssertionIds\":[\"a-opaque-element-kind-Mono\"]"),
          "missing-final-lf" -> canonical.dropRight(1),
          "excess-final-lf" -> (canonical ++ "\n".getBytes(StandardCharsets.UTF_8)),
          "insignificant-whitespace" -> _bytes_replace(canonical, "\"profile\":", "\"profile\" : ")
        )

        When("each candidate-byte variant is written as its own digest-repinned projection artifact and continuity selection follows the successful package structure check")
        val admitted = _fixture_control()
        val results = cases.map { case (name, bytes) => name -> _header_family_rejection(bytes) }

        Then("the normal package admits, and every separately repinned malformed or unknown header passes package structure but fails closed at continuity-family classification")
        admitted shouldBe true
        results.forall(_._2 == (true, true)) shouldBe true
      }

      "reject each structurally valid candidate scope and selected-artifact binding mismatch" in {
        Given("a normal rich package control and canonical candidate values that differ in exactly one scope or selected realization/continuity artifact identity")
        val candidate = _projection(Vector[Byte](1), "binding", _cml_beta_raw)
        val cases = Vector(
          "component-scope" -> candidate.copy(scope = candidate.scope.copy(componentIdentity = "component-other")),
          "projection-context-scope" -> candidate.copy(scope = candidate.scope.copy(projectionContextIdentity = "context-other")),
          "selected-use-case-scope" -> candidate.copy(scope = candidate.scope.copy(selectedUseCaseElementIdentity = "e-other")),
          "realization-artifact" -> candidate.copy(realizationArtifactId = "realization-other"),
          "continuity-artifact" -> candidate.copy(continuityArtifactId = "projection-other")
        )

        When("the valid control is admitted and each otherwise canonical candidate crosses structure before candidate binding validation")
        val admitted = _fixture_control()
        val results = cases.map { case (name, value) => name -> _candidate_rejection(value) }

        Then("the valid control succeeds, and every individual scope or selected-artifact mismatch passes structure but fails closed at candidate binding")
        admitted shouldBe true
        results.forall(_._2) shouldBe true
      }

      "reject complete-but-different mapping references and each semantic lane disagreement after package structure succeeds" in {
        Given("a normal rich package control and alpha mappings whose sorted unique reference sets differ by identity, kind, lane omission, lane exchange, lane promotion, or dangling link")
        val candidate = _projection(Vector[Byte](1), "lanes", _cml_beta_raw)
        val mapping = candidate.targets.head.mappings.head
        val cases = Vector(
          "unknown-semantic-identity" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(semanticIdentity = "opaque-missing")))),
          "cross-scope-semantic-identity" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(semanticIdentity = "e-cross-scope")))),
          "identity-present-only-as-other-kind" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(semanticIdentity = "r-mono-domain")))),
          "shared-identity-explicit-wrong-kind-lanes" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(semanticIdentityKind = "relationship")))),
          "canonical-lane-omitted" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(canonicalAssertionIds = Vector.empty)))),
          "enrichment-lane-omitted" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(enrichmentAssertionIds = Vector.empty)))),
          "condition-lane-omitted" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(conditionIds = Vector.empty)))),
          "canonical-enrichment-lanes-swapped" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(canonicalAssertionIds = mapping.enrichmentAssertionIds, enrichmentAssertionIds = mapping.canonicalAssertionIds)))),
          "enrichment-promoted-into-canonical-lane" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(canonicalAssertionIds = mapping.canonicalAssertionIds ++ mapping.enrichmentAssertionIds)))),
          "dangling-canonical-link" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(canonicalAssertionIds = Vector("a-dangling"))))),
          "dangling-enrichment-link" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(enrichmentAssertionIds = Vector("z-dangling"))))),
          "dangling-condition-link" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(conditionIds = Vector("c-dangling")))))
        )

        When("the normal control and every otherwise canonical sorted mapping are admitted through structure before realization-reference validation")
        val admitted = _fixture_control()
        val results = cases.map { case (name, value) => name -> _candidate_rejection(value) }

        Then("the normal candidate succeeds, while every changed identity or complete-lane disagreement passes package structure and fails its semantic mapping admission")
        admitted shouldBe true
        results.forall(_._2) shouldBe true
      }

      "reject effect-local references while preserving a normal distinct compatibility and migration control" in {
        Given("a normal rich package control plus alpha effects that name an unknown source, unknown local mapping, or the existing beta mapping across target boundaries")
        val candidate = _projection(Vector[Byte](1), "effects", _cml_beta_raw)
        val alpha = candidate.targets.head
        val beta = candidate.targets(1)
        val cases = Vector(
          "unknown-retained-source" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(0, target.effects.head.copy(sourceReferenceId = "ref-missing")))),
          "unknown-local-mapping" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(0, target.effects.head.copy(mappingIds = Vector("mapping-missing"))))),
          "existing-cross-target-mapping" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(0, target.effects.head.copy(mappingIds = Vector(beta.mappings.head.mappingId)))))
        )

        When("the normal distinct-effect package and each sorted, unique, otherwise canonical effect case cross package structure before realization/effect validation")
        val admitted = _fixture_control()
        val results = cases.map { case (name, value) => name -> _candidate_rejection(value) }

        Then("the normal distinct unknown/disagreeing effects remain admitted, and every unknown or cross-target effect reference passes structure but fails local semantic admission")
        admitted shouldBe true
        results.forall(_._2) shouldBe true
      }

      "reject missing effect kinds and kind-specific assessments at the codec boundary with a valid package control" in {
        Given("a normal rich package and canonical alpha-effect DTOs missing compatibility or migration records, using an unsupported kind, or assigning an assessment from the other kind")
        val candidate = _projection(Vector[Byte](1), "effect-codec", _cml_beta_raw)
        val cases = Vector(
          "missing-compatibility" -> _alpha(candidate)(target => target.copy(effects = target.effects.filterNot(_.kind == "compatibility"))),
          "missing-migration" -> _alpha(candidate)(target => target.copy(effects = target.effects.filterNot(_.kind == "migration"))),
          "unsupported-kind" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(0, target.effects.head.copy(kind = "unsupported")))),
          "compatibility-assessment-from-migration" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(0, target.effects.head.copy(assessment = "required")))),
          "migration-assessment-from-compatibility" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(1, target.effects(1).copy(assessment = "breaking"))))
        )

        When("the normal package is admitted and each named effect DTO is encoded then decoded without allowing semantic validation to hide its profile violation")
        val admitted = _fixture_control()
        val results = cases.map { case (name, value) => name -> InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(InternalModelCandidateCmlProjectionCodec.encode(value))) }

        Then("the normal package succeeds while every missing kind, unsupported kind, or wrong kind-specific assessment is a structured codec failure")
        admitted shouldBe true
        results.forall(_._2.isLeft) shouldBe true
      }

      "distinguish nullable source evidence from empty or malformed source/content values and duplicate or unsorted identity links" in {
        Given("a normal rich package control, one nullable source codec control, and separately canonicalized source/content/link variants that change only one required value, digest, length, duplicate, or order fact")
        val candidate = _projection(Vector[Byte](1), "links", _cml_beta_raw)
        val alpha = candidate.targets.head
        val beta = candidate.targets(1)
        val mapping = alpha.mappings.head
        val effect = alpha.effects.head
        val nullable = _alpha(candidate)(target => target.copy(source = target.source.copy(locator = None, revision = None)))
        val cases = Vector(
          "empty-source-authority" -> _alpha(candidate)(target => target.copy(source = target.source.copy(authority = ""))),
          "empty-source-identity" -> _alpha(candidate)(target => target.copy(source = target.source.copy(identity = ""))),
          "empty-source-locator" -> _alpha(candidate)(target => target.copy(source = target.source.copy(locator = Some("")))),
          "empty-source-revision" -> _alpha(candidate)(target => target.copy(source = target.source.copy(revision = Some("")))),
          "empty-source-digest" -> _alpha(candidate)(target => target.copy(source = target.source.copy(sha256 = ""))),
          "malformed-source-digest" -> _alpha(candidate)(target => target.copy(source = target.source.copy(sha256 = "sha256:not-a-digest"))),
          "uppercase-source-digest" -> _alpha(candidate)(target => target.copy(source = target.source.copy(sha256 = "sha256:" + ("A" * 64)))),
          "malformed-proposed-content-digest" -> _alpha(candidate)(target => target.copy(proposedContent = target.proposedContent.copy(sha256 = "sha256:not-a-digest"))),
          "uppercase-proposed-content-digest" -> _alpha(candidate)(target => target.copy(proposedContent = target.proposedContent.copy(sha256 = "sha256:" + ("A" * 64)))),
          "proposed-decoded-length-mismatch" -> _alpha(candidate)(target => target.copy(proposedContent = target.proposedContent.copy(byteLength = target.proposedContent.byteLength + 1))),
          "duplicate-canonical-links" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(canonicalAssertionIds = Vector(mapping.canonicalAssertionIds.head, mapping.canonicalAssertionIds.head))))),
          "duplicate-enrichment-links" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(enrichmentAssertionIds = Vector(mapping.enrichmentAssertionIds.head, mapping.enrichmentAssertionIds.head))))),
          "duplicate-condition-links" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(conditionIds = Vector(mapping.conditionIds.head, mapping.conditionIds.head))))),
          "duplicate-effect-mapping-links" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(0, effect.copy(mappingIds = Vector(mapping.mappingId, mapping.mappingId))))),
          "unsorted-canonical-links" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(canonicalAssertionIds = Vector("z-link", "a-link"))))),
          "unsorted-enrichment-links" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(enrichmentAssertionIds = Vector("z-link", "a-link"))))),
          "unsorted-condition-links" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping.copy(conditionIds = Vector("z-link", "a-link"))))),
          "unsorted-effect-mapping-links" -> _alpha(candidate)(target => target.copy(effects = target.effects.updated(0, effect.copy(mappingIds = Vector("z-link", "a-link"))))),
          "duplicate-target-id" -> candidate.copy(targets = Vector(alpha, beta.copy(targetId = alpha.targetId))),
          "unsorted-target-ids" -> candidate.copy(targets = candidate.targets.reverse),
          "duplicate-mapping-id-within-target" -> _alpha(candidate)(target => target.copy(mappings = Vector(mapping, mapping))),
          "duplicate-mapping-id-across-targets" -> candidate.copy(targets = Vector(alpha, beta.copy(mappings = Vector(beta.mappings.head.copy(mappingId = mapping.mappingId))))),
          "duplicate-effect-id-within-target" -> _alpha(candidate)(target => target.copy(effects = Vector(effect, effect))),
          "duplicate-effect-id-across-targets" -> candidate.copy(targets = Vector(alpha, beta.copy(effects = beta.effects.updated(0, beta.effects.head.copy(effectId = effect.effectId)))))
        )
        val sourcebytes = Vector(
          "locator-wrong-type" -> _source_mutation(candidate)(_.add("locator", Json.True)),
          "revision-wrong-type" -> _source_mutation(candidate)(_.add("revision", Json.True))
        )

        When("the normal package admits, the nullable source form reaches only the codec, and each named required-value/link variant is encoded then decoded at the strict profile boundary")
        val admitted = _fixture_control()
        val nullablesource = InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(InternalModelCandidateCmlProjectionCodec.encode(nullable)))
        val results = cases.map { case (name, value) => name -> InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(InternalModelCandidateCmlProjectionCodec.encode(value))) } ++
          sourcebytes.map { case (name, bytes) => name -> InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(bytes)) }

        Then("the normal package and null locator/revision codec form succeed, while every empty, malformed, mismatched-length, duplicate, or unsorted form is a structured decoder rejection")
        admitted shouldBe true
        nullablesource.isRight shouldBe true
        results.forall(_._2.isLeft) shouldBe true
      }

      "reject closed-field and wrong-type mutations at root, scope, target, source, content, mapping, and effect depth" in {
        Given("one canonical candidate and table-driven extra, missing, and wrong-type mutations at every closed object depth")
        val candidate = _projection(Vector[Byte](1), "depth", _cml_beta_raw)
        val cases = Vector(
          "root-extra" -> _root_mutation(candidate)(_.add("extra", Json.True)),
          "root-missing" -> _root_mutation(candidate)(_.remove("candidateIdentity")),
          "root-type" -> _root_mutation(candidate)(_.add("candidateIdentity", Json.fromInt(1))),
          "scope-extra" -> _scope_mutation(candidate)(_.add("extra", Json.True)),
          "scope-missing" -> _scope_mutation(candidate)(_.remove("componentIdentity")),
          "scope-type" -> _scope_mutation(candidate)(_.add("componentIdentity", Json.fromInt(1))),
          "target-extra" -> _target_mutation(candidate)(_.add("extra", Json.True)),
          "target-missing" -> _target_mutation(candidate)(_.remove("baselineArtifactId")),
          "target-type" -> _target_mutation(candidate)(_.add("targetId", Json.fromInt(1))),
          "content-extra" -> _content_mutation(candidate)(_.add("extra", Json.True)),
          "content-missing" -> _content_mutation(candidate)(_.remove("rawBytesBase64")),
          "content-type" -> _content_mutation(candidate)(_.add("byteLength", Json.fromString("1"))),
          "source-extra" -> _source_mutation(candidate)(_.add("extra", Json.True)),
          "source-missing" -> _source_mutation(candidate)(_.remove("authority")),
          "source-type" -> _source_mutation(candidate)(_.add("locator", Json.True)),
          "mapping-extra" -> _mapping_mutation(candidate)(_.add("extra", Json.True)),
          "mapping-missing" -> _mapping_mutation(candidate)(_.remove("mappingId")),
          "mapping-type" -> _mapping_mutation(candidate)(_.add("conditionIds", Json.fromString("x"))),
          "effect-extra" -> _effect_mutation(candidate)(_.add("extra", Json.True)),
          "effect-missing" -> _effect_mutation(candidate)(_.remove("effectId")),
          "effect-type" -> _effect_mutation(candidate)(_.add("mappingIds", Json.fromString("x")))
        )

        When("each canonicalized mutation is decoded before any package or semantic reader can hide its cause")
        val results = cases.map { case (_, bytes) => InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(bytes)) }

        Then("every object-depth field mutation fails as a structured closed-schema decode result")
        results.forall(_.isLeft) shouldBe true
      }

      "reject canonical number, Base64, digest, duplicate, and unsigned-order edge forms" in {
        Given("a canonical candidate with zero/negative/large/fractional/exponent/string numeric variants, Base64 variants, raw digest mismatch, and duplicate/unsorted candidate-wide identities")
        val candidate = _projection(Vector[Byte](1, 2), "edge", _cml_beta_raw)
        val cases = Vector(
          _root_mutation(candidate)(_.add("candidateRevision", Json.fromInt(0))),
          _root_mutation(candidate)(_.add("candidateRevision", Json.fromInt(-1))),
          _root_mutation(candidate)(_.add("candidateRevision", Json.fromLong(2147483648L))),
          _root_mutation(candidate)(_.add("candidateRevision", Json.fromString("1"))),
          _bytes_replace(InternalModelCandidateCmlProjectionCodec.encode(candidate), "\"candidateRevision\":7", "\"candidateRevision\":1.0"),
          _bytes_replace(InternalModelCandidateCmlProjectionCodec.encode(candidate), "\"candidateRevision\":7", "\"candidateRevision\":1e0"),
          _content_mutation(candidate)(_.add("byteLength", Json.fromInt(-1))),
          _content_mutation(candidate)(_.add("byteLength", Json.fromString("1"))),
          _content_mutation(candidate)(_.add("byteLength", Json.fromLong(Long.MaxValue))),
          _bytes_replace(InternalModelCandidateCmlProjectionCodec.encode(candidate), "\"byteLength\":2", "\"byteLength\":2.0"),
          _bytes_replace(InternalModelCandidateCmlProjectionCodec.encode(candidate), "\"byteLength\":2", "\"byteLength\":2e0"),
          _content_mutation(candidate)(_.add("rawBytesBase64", Json.fromString("AA"))),
          _content_mutation(candidate)(_.add("rawBytesBase64", Json.fromString("AA==\n"))),
          _content_mutation(candidate)(_.add("rawBytesBase64", Json.fromString("-_=="))),
          _content_mutation(candidate)(_.add("sha256", Json.fromString("sha256:" + ("0" * 64)))),
          _mapping_mutation(candidate)(_.add("mappingId", Json.fromString("mapping-beta"))),
          _effect_mutation(candidate)(_.add("effectId", Json.fromString("effect-beta-compatibility")))
        )

        When("every supplied edge form reaches canonical codec validation")
        val results = cases.map(bytes => InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(bytes)))

        Then("numeric, Base64, digest, candidate-wide identity, and canonical-order defects reject without repair")
        results.forall(_.isLeft) shouldBe true
      }

      "reject empty required values and every target/mapping/effect/link ordering or candidate-wide identity collision" in {
        Given("one rich canonical candidate and bounded DTO variants that retain canonical JSON while violating one closed value, array order, or global uniqueness invariant")
        val candidate = _projection(Vector[Byte](1), "arrays", _cml_beta_raw)
        val first = candidate.targets.head
        val second = candidate.targets(1)
        val extramapping = first.mappings.head.copy(mappingId = "aaa-mapping")
        val extraeffect = first.effects.head.copy(effectId = "aaa-effect")
        val cases = Vector(
          candidate.copy(candidateIdentity = ""),
          candidate.copy(candidateModelIdentity = ""),
          candidate.copy(targets = Vector.empty),
          candidate.copy(targets = candidate.targets.map(_.copy(targetId = ""))),
          candidate.copy(targets = candidate.targets.map(_.copy(mappings = Vector.empty))),
          candidate.copy(targets = candidate.targets.map(_.copy(effects = Vector.empty))),
          candidate.copy(targets = candidate.targets.reverse),
          candidate.copy(targets = Vector(first, second.copy(targetId = first.targetId))),
          candidate.copy(targets = Vector(first, second.copy(patchIdentity = first.patchIdentity))),
          candidate.copy(targets = Vector(first, second.copy(projectRelativePath = first.projectRelativePath))),
          candidate.copy(targets = Vector(first, second.copy(baselineArtifactId = first.baselineArtifactId))),
          candidate.copy(targets = Vector(first.copy(mappings = Vector(first.mappings.head, extramapping)), second)),
          candidate.copy(targets = Vector(first.copy(effects = Vector(first.effects.head, extraeffect)), second)),
          candidate.copy(targets = Vector(first, second.copy(mappings = second.mappings.map(_.copy(mappingId = first.mappings.head.mappingId))))),
          candidate.copy(targets = Vector(first, second.copy(effects = second.effects.map(_.copy(effectId = first.effects.head.effectId))))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(mappings = target.mappings.map(_.copy(canonicalAssertionIds = Vector("z", "a")))))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(mappings = target.mappings.map(_.copy(enrichmentAssertionIds = Vector("z", "a")))))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(mappings = target.mappings.map(_.copy(conditionIds = Vector("z", "a")))))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(effects = target.effects.map(_.copy(mappingIds = Vector("z", "a"))))) ),
          candidate.copy(targets = candidate.targets.map(target => target.copy(effects = target.effects.filter(_.kind == "compatibility")))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(effects = target.effects.filter(_.kind == "migration")))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(effects = target.effects.map(_.copy(kind = "unsupported"))))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(effects = target.effects.map(_.copy(assessment = "required"))))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(effects = target.effects.map(_.copy(mappingIds = Vector.empty))))),
          candidate.copy(targets = candidate.targets.map(target => target.copy(source = target.source.copy(authority = ""))))
        )

        When("each otherwise canonical DTO is encoded and then decoded by the strict profile boundary")
        val results = cases.map(value => InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(InternalModelCandidateCmlProjectionCodec.encode(value))))

        Then("empty required values, empty collections, unsorted arrays, and every candidate-wide duplicate identity reject without deduplication or sorting")
        results.forall(_.isLeft) shouldBe true
      }

      "reject Base64 pad-bit and padding variants independently of decoded byte length and hash" in {
        Given("a one-byte proposal whose canonical padded RFC4648 spelling is AQ== and noncanonical pad-bit, unpadded, whitespace, URL-alphabet, and invalid-character variants")
        val candidate = _projection(Vector[Byte](1), "base64", _cml_beta_raw)
        val cases = Vector("AR==", "AQ", "AQ== ", "_Q==", "AQ$=").map(value => _content_mutation(candidate)(_.add("rawBytesBase64", Json.fromString(value))))

        When("the forms are decoded with the original byteLength and digest still retained")
        val results = cases.map(bytes => InternalModelCandidateCmlProjectionCodec.decode(_projection_handoff(bytes)))

        Then("canonical padded Base64 spelling is required even where a decoder could otherwise recover bytes")
        results.forall(_.isLeft) shouldBe true
      }

      "reject absent or duplicate family selections and candidate semantic dependency mismatches" in {
        Given("portable manifests with no candidate, duplicate candidate, V1-plus-V2 continuity, candidate-only, omitted dependency, and extra dependency variants")
        val variants = Vector(
          _fixture_result(includecandidate = false, duplicatecandidate = false, duplicatecontinuity = false, candidatedependencies = None),
          _fixture_result(includecandidate = true, duplicatecandidate = true, duplicatecontinuity = false, candidatedependencies = None),
          _fixture_result(includecandidate = true, duplicatecandidate = false, duplicatecontinuity = true, candidatedependencies = None),
          _fixture_result(includecandidate = true, duplicatecandidate = false, duplicatecontinuity = false, candidatedependencies = Some(Vector("realization-main", "projection-main"))),
          _fixture_result(includecandidate = true, duplicatecandidate = false, duplicatecontinuity = false, candidatedependencies = Some(Vector("realization-main", "projection-main", "snapshot-cml-alpha", "snapshot-cml-beta", "snapshot-model")))
        )

        When("candidate admission selects the closed families and exact dependency set")
        val results = variants.map(_())

        Then("zero/multiple family or continuity matches and omitted/extra dependencies reject without path, ID, or encounter-order fallback")
        results.forall(_.isSuccess == false) shouldBe true
      }

      "skip only a recognized canonical candidate header for continuity selection while candidate admission rejects its bad semantics" in {
        Given("a canonical candidate profile whose mapping names an unknown selected-realization semantic identity")
        val malformedsemantics = _projection(Vector[Byte](1), "bad", _cml_beta_raw).copy(targets = _projection(Vector[Byte](1), "bad", _cml_beta_raw).targets.map(target => target.copy(mappings = target.mappings.map(_.copy(semanticIdentity = "opaque-missing")))))
        _with_fixture(includecandidate = true, candidatebytes = Some(InternalModelCandidateCmlProjectionCodec.encode(malformedsemantics))) { root =>
          When("continuity selection sees the recognized header and candidate admission subsequently resolves its mappings")
          val continuity = InternalModelProjectionContinuityValidator.validate(root)
          val candidate = InternalModelCandidateCmlProjectionValidator.validate(root)

          Then("continuity remains independent of candidate semantics while candidate admission fails closed on the unknown reference")
          continuity.isSuccess shouldBe true
          candidate.isSuccess shouldBe false
        }
      }

      "reject malformed or unknown candidate headers even when a continuity family is otherwise valid" in {
        Given("two candidate-family bytes: one noncanonical and one canonical with an unknown profile/version pair")
        val canonical = InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](1), "header", _cml_beta_raw))
        val unknown = InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](1), "header", _cml_beta_raw).copy(profile = "ccdm-candidate-cml-projection-next"))
        val candidates = Vector(canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8), unknown)

        When("each repinned candidate header is encountered by the continuity family reader")
        val results = candidates.map { bytes =>
          var result: org.goldenport.Consequence[InternalModelProjectionContinuity] = null
          _with_fixture(includecandidate = true, candidatebytes = Some(bytes)) { root =>
            result = InternalModelProjectionContinuityValidator.validate(root)
          }
          result
        }

        Then("continuity selection fails closed instead of ignoring malformed or future candidate headers")
        results.forall(_.isSuccess == false) shouldBe true
      }

      "reject baseline/source, mapping-lane, and expected-effect reference disagreements" in {
        Given("canonical candidate DTOs changed to a wrong baseline source/path, wrong semantic kind, incomplete lane, foreign mapping ID, or missing source reference")
        val candidates = Vector(
          _projection(Vector[Byte](1), "é", _cml_alpha_raw).copy(targets = _projection(Vector[Byte](1), "é", _cml_alpha_raw).targets.map(_.copy(projectRelativePath = "cml/other.cml"))),
          _projection(Vector[Byte](1), "é", _cml_alpha_raw).copy(targets = _projection(Vector[Byte](1), "é", _cml_alpha_raw).targets.map(target => target.copy(mappings = target.mappings.map(_.copy(semanticIdentityKind = "relationship"))))),
          _projection(Vector[Byte](1), "é", _cml_alpha_raw).copy(targets = _projection(Vector[Byte](1), "é", _cml_alpha_raw).targets.map(target => target.copy(mappings = target.mappings.map(_.copy(canonicalAssertionIds = Vector.empty))))),
          _projection(Vector[Byte](1), "é", _cml_alpha_raw).copy(targets = _projection(Vector[Byte](1), "é", _cml_alpha_raw).targets.map(target => target.copy(effects = target.effects.map(_.copy(mappingIds = Vector("mapping-other")))))),
          _projection(Vector[Byte](1), "é", _cml_alpha_raw).copy(targets = _projection(Vector[Byte](1), "é", _cml_alpha_raw).targets.map(target => target.copy(effects = target.effects.map(_.copy(sourceReferenceId = "ref-missing")))))
        )

        When("each canonical candidate replaces the fixture candidate with a repinned manifest digest")
        val results = candidates.map(value => _candidate_rejection(value))

        Then("admission rejects every source, kind, lane, local-mapping, and retained-source disagreement without calculating a winner")
        results.forall(_ == true) shouldBe true
      }

      "separate structural baseline-file failures from structurally valid CML-kind and source-envelope admission failures" in {
        Given("a normal rich fixture, structurally absent required/optional CML baseline files, an unreachable unknown baseline dependency, and repinned candidate source-envelope variants")
        val candidate = _projection(Vector[Byte](1), "baseline", _cml_beta_raw)
        val semanticcases = Vector(
          "non-cml-source-snapshot" -> (_alpha(candidate)(_.copy(baselineArtifactId = "snapshot-model")), Vector("realization-main", "projection-main", "snapshot-model", "snapshot-cml-beta")),
          "wrong-role-present-artifact" -> (_alpha(candidate)(_.copy(baselineArtifactId = "realization-main")), Vector("realization-main", "projection-main", "snapshot-cml-beta")),
          "source-authority" -> (_alpha(candidate)(target => target.copy(source = target.source.copy(authority = "other-authority"))), Vector.empty[String]),
          "source-identity" -> (_alpha(candidate)(target => target.copy(source = target.source.copy(identity = "other-identity"))), Vector.empty[String]),
          "source-locator" -> (_alpha(candidate)(target => target.copy(source = target.source.copy(locator = Some("other/locator")))), Vector.empty[String]),
          "source-revision" -> (_alpha(candidate)(target => target.copy(source = target.source.copy(revision = Some("other-revision")))), Vector.empty[String]),
          "snapshot-artifact-digest-is-not-raw-source-digest" -> (_alpha(candidate)(target => target.copy(source = target.source.copy(sha256 = _sha256(_cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw))))), Vector.empty[String]),
          "baseline-project-relative-path" -> (_alpha(candidate)(_.copy(projectRelativePath = "cml/other-alpha.cml")), Vector.empty[String])
        )

        When("the normal control, closed structural variants, and structurally valid semantic variants cross their intended package or candidate boundary")
        val normal = _fixture_control()
        val structural = Vector(
          "missing-required-cml-baseline" -> _fixture_structural(alpharequired = true, writealpha = false),
          "present-candidate-depends-on-absent-optional-cml-baseline" -> _fixture_structural(alpharequired = false, writealpha = false),
          "unknown-baseline-is-an-unresolved-package-dependency" -> _fixture_structural(
            candidatebytes = Some(InternalModelCandidateCmlProjectionCodec.encode(_alpha(candidate)(_.copy(baselineArtifactId = "snapshot-cml-unknown")))),
            candidatedependencies = Some(Vector("projection-main", "realization-main", "snapshot-cml-beta", "snapshot-cml-unknown"))
          )
        )
        val semantic = semanticcases.map { case (name, (value, dependencies)) =>
          name -> _candidate_rejection(value, dependencies)
        }

        Then("the rich control succeeds; missing and unknown inventory facts reject structurally; and each present wrong-kind/role/source/path variant passes structure before its structured candidate rejection")
        normal shouldBe true
        structural.forall(_._2) shouldBe true
        semantic.forall(_._2) shouldBe true
      }

      "retain package structure while zero or multiple selected families reject at captured handoff selection" in {
        Given("normal and otherwise topologically valid portable inventories with absent realization, multiple realizations, absent continuity, or candidate-only projection families")
        val variants = Vector(
          "absent-realization" -> (() => _continuity_selection_rejection(includerealization = false, duplicaterealization = false, includecontinuity = true, includecandidate = false)),
          "multiple-realization" -> (() => _continuity_selection_rejection(includerealization = true, duplicaterealization = true, includecontinuity = true, includecandidate = false)),
          "absent-continuity" -> (() => _continuity_selection_rejection(includerealization = true, duplicaterealization = false, includecontinuity = false, includecandidate = false)),
          "candidate-only" -> (() => _candidate_selection_rejection(includerealization = true, includecontinuity = false))
        )

        When("each inventory first passes its structural manifest/filesystem boundary and then attempts its named continuity or candidate captured handoff")
        val results = variants.map { case (name, action) => name -> action() }

        Then("every named zero/multiple-family inventory remains structurally valid but the required captured selection is a structured failure")
        results shouldBe Vector(
          "absent-realization" -> (true, true),
          "multiple-realization" -> (true, true),
          "absent-continuity" -> (true, true),
          "candidate-only" -> (true, true)
        )
      }
    }
  }

  private def _candidate_rejection(candidate: InternalModelCandidateCmlProjection, dependencies: Vector[String] = Vector.empty): Boolean = {
    var rejected = false
    _with_fixture(
      includecandidate = true,
      candidatebytes = Some(InternalModelCandidateCmlProjectionCodec.encode(candidate)),
      candidatedependencies = Option.when(dependencies.nonEmpty)(dependencies)
    ) { root =>
      rejected = InternalModelPackageValidator.validateStructure(root).isSuccess && !InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess
    }
    rejected
  }

  private def _header_family_rejection(candidatebytes: Array[Byte]): (Boolean, Boolean) = {
    var structure = false
    var rejected = false
    _with_fixture(includecandidate = true, candidatebytes = Some(candidatebytes)) { root =>
      structure = InternalModelPackageValidator.validateStructure(root).isSuccess
      rejected = !InternalModelProjectionContinuityValidator.validate(root).isSuccess
    }
    structure -> rejected
  }

  private def _fixture_control(): Boolean = {
    var admitted = false
    _with_fixture(includecandidate = true) { root =>
      admitted = InternalModelPackageValidator.validateStructure(root).isSuccess &&
        InternalModelCandidateCmlProjectionValidator.validate(root).isSuccess
    }
    admitted
  }

  private def _fixture_structural(
    alpharequired: Boolean = true,
    writealpha: Boolean = true,
    candidatebytes: Option[Array[Byte]] = None,
    candidatedependencies: Option[Vector[String]] = None
  ): Boolean = {
    var rejected = false
    _with_fixture(
      includecandidate = true,
      candidatebytes = candidatebytes,
      candidatedependencies = candidatedependencies,
      alpharequired = alpharequired,
      writealpha = writealpha
    ) { root =>
      rejected = !InternalModelPackageValidator.validateStructure(root).isSuccess
    }
    rejected
  }

  private def _continuity_selection_rejection(
    includerealization: Boolean,
    duplicaterealization: Boolean,
    includecontinuity: Boolean,
    includecandidate: Boolean
  ): (Boolean, Boolean) = {
    var structure = false
    var rejected = false
    _with_fixture(
      includecandidate = includecandidate,
      includerealization = includerealization,
      duplicaterealization = duplicaterealization,
      includecontinuity = includecontinuity
    ) { root =>
      structure = InternalModelPackageValidator.validateStructure(root).isSuccess
      rejected = !InternalModelPackageValidator.verifiedProjectionContinuity(root).isSuccess
    }
    structure -> rejected
  }

  private def _candidate_selection_rejection(
    includerealization: Boolean,
    includecontinuity: Boolean
  ): (Boolean, Boolean) = {
    var structure = false
    var rejected = false
    _with_fixture(
      includecandidate = true,
      includerealization = includerealization,
      includecontinuity = includecontinuity
    ) { root =>
      structure = InternalModelPackageValidator.validateStructure(root).isSuccess
      rejected = !InternalModelPackageValidator.verifiedCandidateCmlProjection(root).isSuccess
    }
    structure -> rejected
  }

  private def _alpha(projection: InternalModelCandidateCmlProjection)(change: InternalModelCandidateCmlTarget => InternalModelCandidateCmlTarget): InternalModelCandidateCmlProjection =
    projection.copy(targets = projection.targets.updated(0, change(projection.targets.head)))

  private def _fixture_result(
    includecandidate: Boolean,
    duplicatecandidate: Boolean,
    duplicatecontinuity: Boolean,
    candidatedependencies: Option[Vector[String]]
  ): () => org.goldenport.Consequence[InternalModelCandidateCmlAdmission] =
    () => {
      var result: org.goldenport.Consequence[InternalModelCandidateCmlAdmission] = null
      _with_fixture(includecandidate, duplicatecandidate, duplicatecontinuity, candidatedependencies) { root =>
        result = InternalModelCandidateCmlProjectionValidator.validate(root)
      }
      result
    }

  private def _expected_context(): InternalModelVerifiedPackageContext = {
    val model = _model_snapshot()
    val alpha = _cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw)
    val beta = _cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw)
    val realization = _realization("ccdm-realization-v1")
    val continuity = _binding("ccdm-projection-binding-v1", "1.0")
    val candidate = InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw))
    val entries = Vector(
      ("snapshot-cml-alpha", "source-snapshot", "snapshots/cml-alpha.json", alpha, Vector.empty[String]),
      ("snapshot-cml-beta", "source-snapshot", "snapshots/cml-beta.json", beta, Vector.empty[String]),
      ("snapshot-model", "source-snapshot", "snapshots/model.json", model, Vector.empty[String]),
      ("realization-main", "realization", "realizations/main.json", realization, Vector("snapshot-model")),
      ("projection-main", "projection", "projections/continuity.json", continuity, Vector("realization-main")),
      ("candidate-main", "projection", "projections/candidate.json", candidate, Vector("projection-main", "realization-main", "snapshot-cml-alpha", "snapshot-cml-beta"))
    )
    val artifacts = entries.map { case (id, role, path, bytes, dependencies) => _artifact(id, path, role, bytes, dependencies) }
    val manifest = _manifest(artifacts)
    InternalModelVerifiedPackageContext(
      schemaVersion = "1.0",
      packageId = "01234567-89ab-cdef-0123-456789abcdef",
      projectNamespace = "org.example",
      projectId = "candidate-sample",
      revision = 1L,
      lifecycleState = "draft",
      packageDigest = io.circe.parser.parse(new String(manifest, StandardCharsets.UTF_8)).toOption.flatMap(_.hcursor.get[String]("packageDigest").toOption).getOrElse(""),
      manifestBytes = manifest.toVector,
      artifacts = entries.map { case (id, role, path, bytes, dependencies) =>
        InternalModelVerifiedArtifactContext(id, role, path, required = true, dependencies.sorted, _sha256(bytes), present = true)
      }
    )
  }

  private def _with_fixture(
    includecandidate: Boolean,
    duplicatecandidate: Boolean = false,
    duplicatecontinuity: Boolean = false,
    candidatedependencies: Option[Vector[String]] = None,
    candidatebytes: Option[Array[Byte]] = None,
    continuityprofile: String = "ccdm-projection-binding-v1",
    continuityschema: String = "1.0",
    continuityid: String = "projection-main",
    continuitypath: String = "projections/continuity.json",
    candidateid: String = "candidate-main",
    candidatepath: String = "projections/candidate.json",
    alpharequired: Boolean = true,
    writealpha: Boolean = true,
    includerealization: Boolean = true,
    duplicaterealization: Boolean = false,
    includecontinuity: Boolean = true
  )(f: Path => Unit): Unit = {
    val model = _model_snapshot()
    val alpha = _cml_snapshot(_cml_alpha_source, "cml/alpha.cml", _cml_alpha_raw)
    val beta = _cml_snapshot(_cml_beta_source, "cml/beta.cml", _cml_beta_raw)
    val realization = _realization(
      if continuityprofile == "ccdm-projection-binding-v2" then "ccdm-realization-v2" else "ccdm-realization-v1"
    )
    val continuity = _binding(continuityprofile, continuityschema)
    val candidate = candidatebytes.getOrElse(InternalModelCandidateCmlProjectionCodec.encode(_projection(Vector[Byte](0, -1, 10), "é", _cml_beta_raw, continuityid = continuityid)))
    val snapshots = Vector(
      _artifact("snapshot-cml-alpha", "snapshots/cml-alpha.json", "source-snapshot", alpha, Vector.empty, alpharequired),
      _artifact("snapshot-cml-beta", "snapshots/cml-beta.json", "source-snapshot", beta, Vector.empty),
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", model, Vector.empty)
    )
    val realizations = if includerealization then {
      val main = _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model"))
      if duplicaterealization then Vector(
        _artifact("realization-extra", "realizations/extra.json", "realization", realization, Vector("snapshot-model")),
        main
      ) else Vector(main)
    } else Vector.empty
    val continuityartifacts = if includecontinuity then {
      val dependencies = if includerealization then Vector("realization-main") else Vector.empty
      val primary = _artifact(continuityid, continuitypath, "projection", continuity, dependencies)
      if duplicatecontinuity then primary +: Vector(_artifact("projection-v2", "projections/continuity-v2.json", "projection", _binding("ccdm-projection-binding-v2", "2.0"), dependencies)) else Vector(primary)
    } else Vector.empty
    val candidates = if includecandidate then {
      val defaultdependencies = Vector("realization-main") ++
        Option.when(includecontinuity)(continuityid).toVector ++
        Vector("snapshot-cml-alpha", "snapshot-cml-beta")
      val dependencies = candidatedependencies.getOrElse(defaultdependencies)
      val primary = _artifact(candidateid, candidatepath, "projection", candidate, dependencies)
      if duplicatecandidate then Vector(_artifact("candidate-copy", "projections/candidate-copy.json", "projection", candidate, dependencies), primary) else Vector(primary)
    } else Vector.empty
    val artifacts =
      if !includerealization && includecontinuity then continuityartifacts ++ snapshots ++ candidates
      else snapshots ++ realizations ++ continuityartifacts ++ candidates
    val root = _temporary_root()
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: candidate-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      if writealpha then _write(root.resolve("src/main/internal-model/snapshots/cml-alpha.json"), alpha)
      _write(root.resolve("src/main/internal-model/snapshots/cml-beta.json"), beta)
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), model)
      if includerealization then _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      if duplicaterealization then _write(root.resolve("src/main/internal-model/realizations/extra.json"), realization)
      if includecontinuity then _write(root.resolve("src/main/internal-model").resolve(continuitypath), continuity)
      if includecontinuity && duplicatecontinuity then _write(root.resolve("src/main/internal-model/projections/continuity-v2.json"), _binding("ccdm-projection-binding-v2", "2.0"))
      if includecandidate then _write(root.resolve("src/main/internal-model").resolve(candidatepath), candidate)
      if duplicatecandidate then _write(root.resolve("src/main/internal-model/projections/candidate-copy.json"), candidate)
      f(root)
    } finally _delete_tree(root)
  }

  private def _temporary_root(): Path = {
    val target = Path.of("target")
    Files.createDirectories(target)
    Files.createTempDirectory(target, "internal-model-candidate-cml-")
  }

  private def _projection(rawbytes: Vector[Byte], destinationidentity: String, baseline: Vector[Byte], candidateidentity: String = "candidate-order-v1", label: String = "order", continuityid: String = "projection-main"): InternalModelCandidateCmlProjection = {
    val alpha = _target("snapshot-cml-alpha", "patch-alpha", "target-alpha", _cml_alpha_source, "cml/alpha.cml", rawbytes, "mapping-alpha", "opaque-shared", "element", "effect-alpha", "ref-opaque-element-kind-Mono").copy(
      mappings = Vector(InternalModelCandidateCmlMapping(Vector("a-opaque-element-kind-Mono"), "anchor-target-alpha", s"cml-$destinationidentity", Vector("c-opaque-element"), Vector("z-opaque-element-enrichment"), "mapping-alpha", "opaque-shared", "element")),
      effects = Vector(
        InternalModelCandidateCmlEffect("unknown", "first attributable expectation", "effect-alpha-compatibility", "compatibility", Vector("mapping-alpha"), "ref-opaque-element-kind-Mono"),
        InternalModelCandidateCmlEffect("required", "migration expectation remains separate", "effect-alpha-migration", "migration", Vector("mapping-alpha"), "ref-opaque-element-enrichment")
      )
    )
    val beta = _target("snapshot-cml-beta", "patch-beta", "target-beta", _cml_beta_source, "cml/beta.cml", baseline, "mapping-beta", "opaque-shared", "relationship", "effect-beta", "ref-opaque-relationship-role-StructuralDomain").copy(
      mappings = Vector(InternalModelCandidateCmlMapping(Vector("a-opaque-relationship-role-StructuralDomain"), "anchor-target-beta", s"cml-$destinationidentity", Vector("c-opaque-relationship"), Vector("z-opaque-relationship-enrichment"), "mapping-beta", "opaque-shared", "relationship")),
      effects = Vector(
        InternalModelCandidateCmlEffect("breaking", "disagreeing attributable expectation", "effect-beta-compatibility", "compatibility", Vector("mapping-beta"), "ref-opaque-relationship-role-StructuralDomain"),
        InternalModelCandidateCmlEffect("unknown", "unknown migration expectation remains visible", "effect-beta-migration", "migration", Vector("mapping-beta"), "ref-opaque-relationship-enrichment")
      )
    )
    InternalModelCandidateCmlProjection(
      candidateIdentity = candidateidentity,
      candidateModelIdentity = s"candidate-model-$label",
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

  private def _canonical_fixture_targets(projection: InternalModelCandidateCmlProjection): InternalModelCandidateCmlProjection =
    projection.copy(targets = projection.targets.sortBy(_.targetId))

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
    val mapping = InternalModelCandidateCmlMapping(canonicalids, s"anchor-$targetid", s"cml-$targetid", Vector.empty, Vector.empty, mappingid, semanticidentity, kind)
    val effects = Vector(
      InternalModelCandidateCmlEffect("unknown", "supplied compatibility expectation", s"$effectprefix-compatibility", "compatibility", Vector(mappingid), referenceid),
      InternalModelCandidateCmlEffect("unknown", "supplied migration expectation", s"$effectprefix-migration", "migration", Vector(mappingid), referenceid)
    )
    InternalModelCandidateCmlTarget(baselineid, effects, Vector(mapping), patchidentity, path, _content(rawbytes), source, targetid)
  }

  private def _content(bytes: Vector[Byte]): InternalModelCandidateCmlContent =
    InternalModelCandidateCmlContent(bytes.length.toLong, Base64.getEncoder.encodeToString(bytes.toArray), _sha256(bytes.toArray))

  private def _projection_handoff(bytes: Array[Byte]): InternalModelVerifiedProjection =
    InternalModelVerifiedProjection("candidate-main", "projection", "projections/candidate.json", required = true, Vector.empty, bytes.toVector)

  private def _candidate_json(projection: InternalModelCandidateCmlProjection): Json =
    io.circe.parser.parse(new String(InternalModelCandidateCmlProjectionCodec.encode(projection), StandardCharsets.UTF_8)).fold(error => throw IllegalArgumentException(error.message), identity)

  private def _root_mutation(projection: InternalModelCandidateCmlProjection)(change: JsonObject => JsonObject): Array[Byte] =
    _canonical(Json.fromJsonObject(change(_candidate_json(projection).asObject.getOrElse(JsonObject.empty))))

  private def _scope_mutation(projection: InternalModelCandidateCmlProjection)(change: JsonObject => JsonObject): Array[Byte] =
    _root_mutation(projection) { root =>
      val scope = root("scope").flatMap(_.asObject).getOrElse(JsonObject.empty)
      root.add("scope", Json.fromJsonObject(change(scope)))
    }

  private def _target_mutation(projection: InternalModelCandidateCmlProjection)(change: JsonObject => JsonObject): Array[Byte] =
    _targets_mutation(projection) { targets =>
      targets.updated(0, Json.fromJsonObject(change(targets.head.asObject.getOrElse(JsonObject.empty))))
    }

  private def _content_mutation(projection: InternalModelCandidateCmlProjection)(change: JsonObject => JsonObject): Array[Byte] =
    _target_mutation(projection) { target =>
      val content = target("proposedContent").flatMap(_.asObject).getOrElse(JsonObject.empty)
      target.add("proposedContent", Json.fromJsonObject(change(content)))
    }

  private def _source_mutation(projection: InternalModelCandidateCmlProjection)(change: JsonObject => JsonObject): Array[Byte] =
    _target_mutation(projection) { target =>
      val source = target("source").flatMap(_.asObject).getOrElse(JsonObject.empty)
      target.add("source", Json.fromJsonObject(change(source)))
    }

  private def _mapping_mutation(projection: InternalModelCandidateCmlProjection)(change: JsonObject => JsonObject): Array[Byte] =
    _target_mutation(projection) { target =>
      val mappings = target("mappings").flatMap(_.asArray).getOrElse(Vector.empty)
      target.add("mappings", Json.fromValues(mappings.updated(0, Json.fromJsonObject(change(mappings.head.asObject.getOrElse(JsonObject.empty))))))
    }

  private def _effect_mutation(projection: InternalModelCandidateCmlProjection)(change: JsonObject => JsonObject): Array[Byte] =
    _target_mutation(projection) { target =>
      val effects = target("effects").flatMap(_.asArray).getOrElse(Vector.empty)
      target.add("effects", Json.fromValues(effects.updated(0, Json.fromJsonObject(change(effects.head.asObject.getOrElse(JsonObject.empty))))))
    }

  private def _targets_mutation(projection: InternalModelCandidateCmlProjection)(change: Vector[Json] => Vector[Json]): Array[Byte] =
    _root_mutation(projection) { root =>
      root.add("targets", Json.fromValues(change(root("targets").flatMap(_.asArray).getOrElse(Vector.empty))))
    }

  private def _bytes_replace(bytes: Array[Byte], before: String, after: String): Array[Byte] =
    new String(bytes, StandardCharsets.UTF_8).replace(before, after).getBytes(StandardCharsets.UTF_8)

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
        _condition("c-opaque-element", "limitation", "element", "opaque-shared", "ref-opaque-element-enrichment", "element evidence remains bounded"),
        _condition("c-opaque-relationship", "limitation", "relationship", "opaque-shared", "ref-opaque-relationship-enrichment", "relationship evidence remains bounded")
      )),
      "elements" -> Json.fromValues(Vector(
        _element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")),
        _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")),
        _element("opaque-shared", "Mono", "Opaque shared element", Vector("a-opaque-element-kind-Mono"), Vector("z-opaque-element-enrichment"), Vector("c-opaque-element"))
      )),
      "enrichmentAssertions" -> Json.fromValues(enrichment),
      "profile" -> Json.fromString(profile),
      "realizationIdentity" -> Json.fromString(s"realization-$schema"),
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

  private def _condition(id: String, kind: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj(
      "affectedIdentity" -> Json.fromString(affectedidentity),
      "affectedKind" -> Json.fromString(affectedkind),
      "conditionId" -> Json.fromString(id),
      "detail" -> Json.fromString(detail),
      "kind" -> Json.fromString(kind),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _artifact(id: String, path: String, role: String, bytes: Array[Byte], dependencies: Vector[String], required: Boolean = true): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "dependsOn" -> Json.fromValues(dependencies.sorted.map(Json.fromString)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(required),
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

  private def _scope_json: Json =
    Json.obj("componentIdentity" -> Json.fromString("component-order"), "projectionContextIdentity" -> Json.fromString("context-order"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase"))

  private def _source(authority: String, identity: String, locator: Option[String], revision: Option[String], sha256: String): InternalModelSemanticSource =
    InternalModelSemanticSource(authority, identity, locator, revision, sha256)

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
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString

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
