package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 27, 2026
 * @version Sep. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelSourceSnapshotFreshnessSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpaces.copy(sortKeys = true)
  private val _authority = "catalog-authority"
  private val _identity = "source-identity"
  private val _revision = "source-revision"
  private val _scenario_raw = "scenario raw source\n".getBytes(StandardCharsets.UTF_8)
  private val _model_raw = "model raw source\n".getBytes(StandardCharsets.UTF_8)
  private val _glossary_raw = "glossary raw source\n".getBytes(StandardCharsets.UTF_8)
  private val _cml_raw = "entity Customer\n".getBytes(StandardCharsets.UTF_8)

  "Internal-model source snapshot freshness" should {
    "accept each closed V1 basis as an unchanged exact live observation" which {
      "retain only comparison metadata for Scenario, model-context, glossary/BoK, and CML baselines" in {
        Given("four independently canonical snapshot fixtures and exact source-owned observations")
        val cases = Vector(
          (
            _snapshot("scenario", _scenario_basis, _scenario_raw),
            _observed(_scenario_raw)
          ),
          (
            _snapshot("model-context", _model_context_basis, _model_raw),
            _observed(_model_raw)
          ),
          (
            _snapshot("glossary-bok", _glossary_basis, _glossary_raw),
            _observed(_glossary_raw)
          ),
          (
            _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"), _cml_raw),
            _observed(_cml_raw, projectrelativepath = Some("model/customer.cml"))
          )
        )

        cases.foreach { case (snapshot, observation) =>
          When("the exact observed source is compared with its recorded baseline")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("the report is unchanged without retaining raw source content")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
          report.changedDimensionNames shouldBe Vector.empty
          report.reason shouldBe None
          report.toString should not include "scenario body"
          report.toString should not include "entity Customer"
        }
      }

      "treat an explicit null revision as equal only to an explicit null revision" in {
        Given("a canonical Scenario snapshot whose source has no revision")
        val snapshot = _snapshot("scenario", _scenario_basis, _scenario_raw, revision = Json.Null)
        val observation = _observed(_scenario_raw, revision = None)

        When("the equal null-revision observation is compared")
        val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

        Then("the null value remains explicit rather than inferred")
        report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
        report.baselineSourceRevision shouldBe None
        report.observedSourceRevision shouldBe None
      }

      "report a null baseline revision and a source-owned observed revision as changed" in {
        Given("a canonical Scenario snapshot with null revision and equal non-revision source evidence")
        val snapshot = _snapshot("scenario", _scenario_basis, _scenario_raw, revision = Json.Null)
        val observation = _observed(_scenario_raw, revision = Some("source-revision-2"))

        When("the source-owned nonempty revision is compared with the null baseline revision")
        val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

        Then("the revision transition is retained as the sole changed dimension")
        report.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
        report.changedDimensionNames shouldBe Vector("source.revision")
        report.baselineSourceRevision shouldBe None
        report.observedSourceRevision shouldBe Some("source-revision-2")
      }
    }

    "retain every observed difference deterministically" which {
      "report authority, identity, revision, raw bytes, CML path, and byte-length differences together" in {
        Given("a CML baseline and one different source-owned CML observation")
        val snapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"), _cml_raw)
        val currentraw = "entity Customer\nattribute name\n".getBytes(StandardCharsets.UTF_8)
        val observation = _observed(
          currentraw,
          authority = "other-authority",
          identity = "other-identity",
          revision = Some("other-revision"),
          projectrelativepath = Some("model/customer-v2.cml")
        )

        When("all live metadata and bytes differ from the recorded CML baseline")
        val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

        Then("Changed contains every lexicographically ordered difference rather than the first one")
        report.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
        report.changedDimensionNames shouldBe Vector(
          "basis.byteLength",
          "basis.projectRelativePath",
          "basis.rawBytesBase64",
          "source.authority",
          "source.identity",
          "source.revision",
          "source.sha256"
        )
        report.currentCmlProjectRelativePath shouldBe Some("model/customer-v2.cml")
      }

      "treat a changed source identity or CML path as changed even when raw bytes have the same digest" in {
        Given("a CML baseline with exact current bytes but a different identity and target path")
        val snapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"), _cml_raw)
        val observation = _observed(
          _cml_raw,
          identity = "different-source-identity",
          projectrelativepath = Some("model/customer-renamed.cml")
        )

        When("the equal bytes are compared with changed source identity and path")
        val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

        Then("the path and identity remain independent equality dimensions")
        report.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
        report.changedDimensionNames shouldBe Vector("basis.projectRelativePath", "source.identity")
        report.observedRawBytesSha256 shouldBe report.baselineRawBytesSha256
      }

      "surface generated identity and byte perturbations without changing the supplied snapshot" in {
        Given("a Scenario snapshot and generated nonempty identity and byte perturbations")
        val snapshot = _snapshot("scenario", _scenario_basis, _scenario_raw)
        val original = snapshot.clone()
        val suffixes = for {
          identitysuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
          bytessuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
        } yield identitysuffix -> bytessuffix

        forAll(suffixes) { case (identitysuffix, bytessuffix) =>
          When("one generated source identity and one exact raw-byte sequence differ")
          val observation = _observed(
            ("different raw source " + bytessuffix).getBytes(StandardCharsets.UTF_8),
            identity = "different-source-" + identitysuffix
          )
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("both dimensions are reported and the caller-owned bytes remain unchanged")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
          report.changedDimensionNames shouldBe Vector("source.identity", "source.sha256")
          snapshot should contain theSameElementsInOrderAs original
        }
      }
    }

    "preserve source-observation availability states" which {
      "return every non-observed status with its own evidence rather than manufacturing an observed source" in {
        Given("one valid Scenario baseline and each closed non-observed live source state")
        val snapshot = _snapshot("scenario", _scenario_basis, _scenario_raw)
        val cases = Vector(
          InternalModelLiveSourceObservation.Unavailable("provider is offline") -> InternalModelSnapshotFreshnessStatus.Unavailable,
          InternalModelLiveSourceObservation.Unauthorized("source access is denied") -> InternalModelSnapshotFreshnessStatus.Unauthorized,
          InternalModelLiveSourceObservation.Malformed("provider response is malformed") -> InternalModelSnapshotFreshnessStatus.Malformed,
          InternalModelLiveSourceObservation.AmbiguousOrConflicting("two source-owned revisions conflict") -> InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting
        )

        cases.foreach { case (observation, status) =>
          When("the baseline is compared with that non-observed source state")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("the distinct status and evidence are retained without an observed digest")
          report.status shouldBe status
          report.observedRawBytesSha256 shouldBe None
          report.reason should not be None
        }
      }

      "reject a non-CML target-path claim and require a safe CML target path" in {
        Given("a Scenario observation that claims CML and a CML observation that omits its path")
        val scenariosnapshot = _snapshot("scenario", _scenario_basis, _scenario_raw)
        val cmlsnapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"), _cml_raw)

        When("the malformed observations are compared")
        val noncml = InternalModelSourceSnapshotFreshness.compare(
          scenariosnapshot,
          _observed(_scenario_raw, projectrelativepath = Some("model/customer.cml"))
        )
        val missingpath = InternalModelSourceSnapshotFreshness.compare(cmlsnapshot, _observed(_cml_raw))

        Then("neither malformed observation can claim unchanged equality")
        noncml.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
        missingpath.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
      }

      "reject CML paths containing a non-breaking space in either comparison input" in {
        Given("CML baseline and observed paths whose otherwise safe segment contains U+00A0")
        val nonbreakingspacepath = "model/customer\u00a0draft.cml"
        val baselinesnapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, nonbreakingspacepath), _cml_raw)
        val observedsnapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"), _cml_raw)

        When("each CML path is compared as supplied baseline or source-owned observation evidence")
        val malformedbaseline = InternalModelSourceSnapshotFreshness.compare(
          baselinesnapshot,
          _observed(_cml_raw, projectrelativepath = Some("model/customer.cml"))
        )
        val malformedobserved = InternalModelSourceSnapshotFreshness.compare(
          observedsnapshot,
          _observed(_cml_raw, projectrelativepath = Some(nonbreakingspacepath))
        )

        Then("both non-breaking-space paths are malformed rather than unchanged or changed")
        malformedbaseline.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
        malformedobserved.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
      }
    }

    "fail closed before comparing a live source" which {
      "give malformed snapshot bytes precedence over an otherwise distinct live observation" in {
        Given("BOM, invalid JSON, duplicate-member, and noncanonical source-snapshot byte variants")
        val canonical = _snapshot("scenario", _scenario_basis, _scenario_raw)
        val duplicate = ("{" +
          "\"basis\":{},\"basis\":{},\"schemaVersion\":\"1.0\",\"snapshotKind\":\"scenario\",\"source\":{}" +
          "}\n").getBytes(StandardCharsets.UTF_8)
        val variants = Vector(
          Array(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ canonical,
          "{not-json}\n".getBytes(StandardCharsets.UTF_8),
          duplicate,
          (canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8))
        )

        forAll(Gen.oneOf(variants)) { snapshot =>
          When("the malformed baseline is offered with an unauthorized live observation")
          val report = InternalModelSourceSnapshotFreshness.compare(
            snapshot,
            InternalModelLiveSourceObservation.Unauthorized("not consulted before baseline validation")
          )

          Then("Malformed is returned before the live status is considered")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          report.snapshotKind shouldBe None
          report.observedSourceIdentity shouldBe None
        }
      }

      "reject invalid kind, basis, ordering, Base64, CML path, and raw digest without repair" in {
        Given("independently canonical JSON envelopes whose V1 values are structurally invalid")
        val wrongkind = _snapshot("unsupported-kind", _scenario_basis, _scenario_raw)
        val wrongbasis = _snapshot("scenario", _cml_basis(_scenario_raw, "model/customer.cml"), _scenario_raw)
        val unordered = _snapshot("scenario", _scenario_basis_with_unordered_traces, _scenario_raw)
        val invalidbase64 = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml", encoded = "not canonical"), _cml_raw)
        val unsafepath = _snapshot("cml-baseline", _cml_basis(_cml_raw, "../customer.cml"), _cml_raw)
        val wronghash = _snapshot(
          "cml-baseline",
          _cml_basis(_cml_raw, "model/customer.cml"),
          _cml_raw,
          sourcehash = "sha256:" + ("0" * 64)
        )
        val invalid = Vector(wrongkind, wrongbasis, unordered, invalidbase64, unsafepath, wronghash)

        forAll(Gen.oneOf(invalid)) { snapshot =>
          When("an invalid V1 value is compared without modifying its supplied bytes")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, _observed(_scenario_raw))

          Then("the comparator reports malformed baseline data instead of reserializing it into acceptance")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          report.changedDimensionNames shouldBe Vector.empty
        }
      }

      "treat a supplied CML path as comparison evidence and never as a filesystem target" in {
        Given("a valid CML baseline whose path does not name a local file")
        val snapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "missing/read-only-model.cml"), _cml_raw)
        val original = snapshot.clone()
        val observation = _observed(_cml_raw, projectrelativepath = Some("missing/read-only-model.cml"))

        When("the supplied raw bytes and path are compared")
        val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

        Then("no input bytes are mutated and no filesystem presence is required for equality")
        report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
        snapshot should contain theSameElementsInOrderAs original
      }
    }
  }

  private def _snapshot(
    kind: String,
    basis: Json,
    rawbytes: Array[Byte],
    authority: String = _authority,
    identity: String = _identity,
    revision: Json = Json.fromString(_revision),
    sourcehash: String = ""
  ): Array[Byte] = {
    val hash = Option(sourcehash).filter(_.nonEmpty).getOrElse(_sha256(rawbytes))
    _canonical(Json.fromJsonObject(JsonObject.fromIterable(Vector(
      "basis" -> basis,
      "schemaVersion" -> Json.fromString("1.0"),
      "snapshotKind" -> Json.fromString(kind),
      "source" -> Json.obj(
        "authority" -> Json.fromString(authority),
        "identity" -> Json.fromString(identity),
        "locator" -> Json.Null,
        "revision" -> revision,
        "sha256" -> Json.fromString(hash)
      )
    ))))
  }

  private def _observed(
    rawbytes: Array[Byte],
    authority: String = _authority,
    identity: String = _identity,
    revision: Option[String] = Some(_revision),
    projectrelativepath: Option[String] = None
  ): InternalModelLiveSourceObservation =
    InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes.toVector, projectrelativepath)

  private def _scenario_basis: Json =
    Json.obj(
      "content" -> Json.fromString("scenario body\n"),
      "scenarioId" -> Json.fromString("scenario-identity"),
      "traceLinks" -> Json.arr(_trace_link("scenario-anchor"))
    )

  private def _scenario_basis_with_unordered_traces: Json =
    Json.obj(
      "content" -> Json.fromString("scenario body\n"),
      "scenarioId" -> Json.fromString("scenario-identity"),
      "traceLinks" -> Json.arr(_trace_link("z-anchor"), _trace_link("a-anchor"))
    )

  private def _trace_link(anchor: String): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-identity"),
      "projectionContextIdentity" -> Json.fromString("projection-context-identity"),
      "scenarioAnchor" -> Json.fromString(anchor),
      "semanticIdentity" -> Json.fromString("semantic-identity"),
      "semanticIdentityKind" -> Json.fromString("element")
    )

  private def _model_context_basis: Json =
    Json.obj(
      "contextIdentity" -> Json.fromString("model-context-identity"),
      "facts" -> Json.arr(Json.obj(
        "componentIdentity" -> Json.fromString("component-identity"),
        "content" -> Json.fromString("model fact\n"),
        "limitations" -> Json.arr(Json.fromString("source limitation")),
        "projectionContextIdentity" -> Json.fromString("projection-context-identity"),
        "semanticIdentity" -> Json.fromString("semantic-identity"),
        "semanticIdentityKind" -> Json.fromString("relationship"),
        "sourceAnchor" -> Json.fromString("model-source-anchor")
      ))
    )

  private def _glossary_basis: Json =
    Json.obj(
      "entries" -> Json.arr(Json.obj(
        "definition" -> Json.fromString("glossary definition\n"),
        "limitations" -> Json.arr(Json.fromString("source limitation")),
        "sourceAnchor" -> Json.fromString("glossary-source-anchor"),
        "termIdentity" -> Json.fromString("term-identity"),
        "termLabel" -> Json.fromString("Term")
      ))
    )

  private def _cml_basis(rawbytes: Array[Byte], path: String, encoded: String = ""): Json = {
    val base64 = Option(encoded).filter(_.nonEmpty).getOrElse(Base64.getEncoder.encodeToString(rawbytes))
    Json.obj(
      "byteLength" -> Json.fromInt(rawbytes.length),
      "projectRelativePath" -> Json.fromString(path),
      "rawBytesBase64" -> Json.fromString(base64)
    )
  }

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
}
