package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 27, 2026
 * @version Oct. 1, 2026
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
    "compare complete declared source references" which {
      "admit all four V2 bases with matching versions and retain comparison metadata only" in {
        Given("four complete closed V2 bases and source-owned observations")
        val cases = Vector(
          (_snapshot("scenario", _scenario_basis), _observed(_scenario_raw), "scenario", None),
          (_snapshot("model-context", _model_context_basis), _observed(_model_raw), "model-context", None),
          (_snapshot("glossary-bok", _glossary_basis), _observed(_glossary_raw), "glossary-bok", None),
          (
            _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml")),
            _observed(_cml_raw, projectrelativepath = Some("model/customer.cml")),
            "cml-baseline",
            Some("model/customer.cml")
          )
        )

        cases.foreach { case (snapshot, observation, kind, path) =>
          When("the complete matching declared source references are compared")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)
          val admittedkind = InternalModelSourceSnapshotFreshness.validatedSnapshotKind(snapshot)

          Then("agreement reports Unchanged and exposes only declared comparison evidence")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
          admittedkind shouldBe Right(kind)
          report.snapshotKind shouldBe Some(kind)
          report.baselineSourceAuthority shouldBe Some(_authority)
          report.observedSourceAuthority shouldBe Some(_authority)
          report.baselineSourceIdentity shouldBe Some(_identity)
          report.observedSourceIdentity shouldBe Some(_identity)
          report.baselineSourceRevision shouldBe Some(_revision)
          report.observedSourceRevision shouldBe Some(_revision)
          report.baselineCmlProjectRelativePath shouldBe path
          report.currentCmlProjectRelativePath shouldBe path
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
          report.reason shouldBe None
          report.toString should not include "scenario body"
          report.toString should not include "model fact"
          report.toString should not include "glossary definition"
          report.toString should not include "entity Customer"
          report.toString should not include "navigation-only"
        }
      }

      "retain every known metadata and CML-path difference in sorted order" in {
        Given("a complete CML baseline and an observation with four different declared dimensions")
        val snapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"))
        val observation = _observed(
          "different ordinary payload\n".getBytes(StandardCharsets.UTF_8),
          authority = "other-authority",
          identity = "other-identity",
          revision = Some("other-revision"),
          projectrelativepath = Some("model/customer-v2.cml")
        )

        When("all declared source metadata and the target path are compared")
        val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

        Then("Changed retains all four dimensions and no payload dimensions")
        report.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
        report.changedDimensionNames shouldBe Vector(
          "basis.projectRelativePath", "source.authority", "source.identity", "source.revision"
        )
        report.missingDimensionNames shouldBe Vector.empty
        report.observedSourceAuthority shouldBe Some("other-authority")
        report.observedSourceIdentity shouldBe Some("other-identity")
        report.observedSourceRevision shouldBe Some("other-revision")
        report.currentCmlProjectRelativePath shouldBe Some("model/customer-v2.cml")
        report.reason.get should include ("declared source references differ")
      }

      "surface generated identity and revision perturbations without mutating caller inputs" in {
        Given("a complete Scenario baseline and generated nonempty source-owned identity and revision suffixes")
        val snapshot = _snapshot("scenario", _scenario_basis)
        val original = snapshot.clone()
        val suffixes = for {
          identitysuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
          revisionsuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
        } yield identitysuffix -> revisionsuffix

        forAll(suffixes) { case (identitysuffix, revisionsuffix) =>
          And("the generated observation contains independently changed identity and revision")
          val observation = _observed(
            _scenario_raw,
            identity = "different-source-" + identitysuffix,
            revision = Some("different-revision-" + revisionsuffix)
          )

          When("the generated declared references are compared")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("both reference differences are reported and the supplied baseline remains untouched")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
          report.changedDimensionNames shouldBe Vector("source.identity", "source.revision")
          report.missingDimensionNames shouldBe Vector.empty
          snapshot should contain theSameElementsInOrderAs original
        }
      }

      "leave ordinary payload and CML length changes outside control comparison while retaining path changes" in {
        Given("complete declared references with different raw payloads, selected text, and admitted CML lengths")
        val otherraw = "entity Order\nattribute total\n".getBytes(StandardCharsets.UTF_8)
        val path = "model/customer.cml"
        val cases = Vector(
          (_snapshot("scenario", _scenario_basis), _observed(otherraw)),
          (_snapshot("scenario", _set(_scenario_basis, "content", Json.fromString("other full scenario\n"))), _observed(_scenario_raw)),
          (_snapshot("model-context", _model_context_basis), _observed(otherraw)),
          (_snapshot("glossary-bok", _glossary_basis), _observed(otherraw)),
          (_snapshot("cml-baseline", _cml_basis(_cml_raw, path)), _observed(otherraw, projectrelativepath = Some(path))),
          (_snapshot("cml-baseline", _cml_basis(otherraw, path)), _observed(_cml_raw, projectrelativepath = Some(path)))
        )
        val cmlsnapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, path))
        val moved = _observed(otherraw, projectrelativepath = Some("model/renamed.cml"))

        When("the supplied references are compared independently of all payload variation")
        val reports = cases.map { case (snapshot, observation) =>
          InternalModelSourceSnapshotFreshness.compare(snapshot, observation)
        }
        val pathreport = InternalModelSourceSnapshotFreshness.compare(cmlsnapshot, moved)

        Then("matching complete references agree without claiming content freshness and the path remains independent")
        reports.foreach { report =>
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
          report.reason shouldBe None
        }
        pathreport.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
        pathreport.changedDimensionNames shouldBe Vector("basis.projectRelativePath")
        pathreport.missingDimensionNames shouldBe Vector.empty
      }
    }

    "preserve unknown source versions" which {
      "report None/None, None/Some, and Some/None as Incomplete with exact missing and changed dimensions" in {
        Given("the three source-owned revision combinations containing at least one explicit absence")
        val cases = Vector(
          (Json.Null, None, Vector("baseline.source.revision", "observed.source.revision"), Vector.empty[String]),
          (Json.Null, Some("observed-version"), Vector("baseline.source.revision"), Vector("source.revision")),
          (Json.fromString(_revision), None, Vector("observed.source.revision"), Vector("source.revision"))
        )

        cases.foreach { case (baseline, observed, missing, changed) =>
          And("a structurally valid Scenario baseline and observation preserve that revision combination")
          val snapshot = _snapshot("scenario", _scenario_basis, revision = baseline)
          val observation = _observed(_scenario_raw, revision = observed)

          When("the supplied revisions are compared without substituting an artifact version")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("the missing source-owned versions remain incomplete and any transition remains observable")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Incomplete
          report.missingDimensionNames shouldBe missing
          report.changedDimensionNames shouldBe changed
          report.baselineSourceRevision shouldBe baseline.asString
          report.observedSourceRevision shouldBe observed
          report.reason.get should include ("missing source-owned revision")
          missing.foreach(dimension => report.reason.get should include (dimension))
        }
      }

      "retain simultaneous authority, identity, revision, and CML-path contradictions while incomplete" in {
        Given("a CML baseline with no source revision and contradictory declared observation metadata")
        val snapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"), revision = Json.Null)
        val revisions = Vector(None, Some("observed-version"))

        revisions.foreach { revision =>
          And("the observation changes authority, identity, and path with the selected optional revision")
          val observation = _observed(
            _cml_raw,
            authority = "other-authority",
            identity = "other-identity",
            revision = revision,
            projectrelativepath = Some("model/other.cml")
          )

          When("the incomplete references and all independently observable contradictions are compared")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("Incomplete retains every declared contradiction rather than hiding it")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Incomplete
          report.changedDimensionNames shouldBe
            (Vector("basis.projectRelativePath", "source.authority", "source.identity") ++
              revision.toVector.map(_ => "source.revision"))
          report.missingDimensionNames shouldBe
            (Vector("baseline.source.revision") ++ Option.when(revision.isEmpty)("observed.source.revision").toVector)
          report.observedSourceAuthority shouldBe Some("other-authority")
          report.observedSourceIdentity shouldBe Some("other-identity")
          report.currentCmlProjectRelativePath shouldBe Some("model/other.cml")
          report.reason.get should include ("missing source-owned revision")
        }
      }
    }

    "preserve attributed observation outcomes" which {
      "retain all four non-observed states without inventing an observation or overriding them with incompleteness" in {
        Given("a valid baseline with unknown revision and each closed non-observed state with its own evidence")
        val snapshot = _snapshot("scenario", _scenario_basis, revision = Json.Null)
        val cases = Vector(
          (InternalModelLiveSourceObservation.Unavailable("provider is offline"), InternalModelSnapshotFreshnessStatus.Unavailable, "provider is offline"),
          (InternalModelLiveSourceObservation.Unauthorized("source access is denied"), InternalModelSnapshotFreshnessStatus.Unauthorized, "source access is denied"),
          (InternalModelLiveSourceObservation.Malformed("provider response is malformed"), InternalModelSnapshotFreshnessStatus.Malformed, "provider response is malformed"),
          (InternalModelLiveSourceObservation.AmbiguousOrConflicting("two revisions conflict"), InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting, "two revisions conflict")
        )

        cases.foreach { case (observation, status, evidence) =>
          When("the valid baseline is offered with that non-observed source state")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("the exact status and evidence remain attributed with no comparable observation")
          report.status shouldBe status
          report.reason shouldBe Some(evidence)
          report.baselineSourceAuthority shouldBe Some(_authority)
          report.baselineSourceIdentity shouldBe Some(_identity)
          report.baselineSourceRevision shouldBe None
          report.observedSourceAuthority shouldBe None
          report.observedSourceIdentity shouldBe None
          report.observedSourceRevision shouldBe None
          report.currentCmlProjectRelativePath shouldBe None
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
        }
      }

      "apply the existing nonempty evidence rule without trimming opaque evidence" in {
        Given("null and empty evidence for each non-observed form plus a nonempty whitespace reason")
        val snapshot = _snapshot("scenario", _scenario_basis)
        val invalid = Vector(null, "").flatMap { evidence =>
          Vector(
            InternalModelLiveSourceObservation.Unavailable(evidence),
            InternalModelLiveSourceObservation.Unauthorized(evidence),
            InternalModelLiveSourceObservation.Malformed(evidence),
            InternalModelLiveSourceObservation.AmbiguousOrConflicting(evidence)
          )
        }
        val whitespace = InternalModelLiveSourceObservation.Unavailable(" ")

        When("the evidence is interpreted under the unchanged nonempty-string rule")
        val reports = invalid.map(observation => InternalModelSourceSnapshotFreshness.compare(snapshot, observation))
        val preserved = InternalModelSourceSnapshotFreshness.compare(snapshot, whitespace)

        Then("missing evidence is malformed while nonempty evidence retains its exact spelling")
        reports.foreach { report =>
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          report.missingDimensionNames shouldBe Vector.empty
        }
        preserved.status shouldBe InternalModelSnapshotFreshnessStatus.Unavailable
        preserved.reason shouldBe Some(" ")
      }

      "reject missing or malformed observed metadata, options, payloads, and incompatible CML-path claims" in {
        Given("well-formed baselines with unknown revisions and malformed caller observations")
        val scenariosnapshot = _snapshot("scenario", _scenario_basis, revision = Json.Null)
        val cmlsnapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml"), revision = Json.Null)
        val invalidscenario = Vector(
          null,
          _observed(_scenario_raw, authority = null),
          _observed(_scenario_raw, authority = ""),
          _observed(_scenario_raw, identity = null),
          _observed(_scenario_raw, identity = ""),
          _observed(_scenario_raw, revision = null),
          _observed(_scenario_raw, revision = Some(null)),
          _observed(_scenario_raw, revision = Some("")),
          InternalModelLiveSourceObservation.Observed(_authority, _identity, None, null, None),
          _observed(_scenario_raw, projectrelativepath = null),
          _observed(_scenario_raw, projectrelativepath = Some(null)),
          _observed(_scenario_raw, projectrelativepath = Some("model/customer.cml"))
        )
        val invalidcml = Vector(
          _observed(_cml_raw),
          _observed(_cml_raw, projectrelativepath = null),
          _observed(_cml_raw, projectrelativepath = Some(null)),
          _observed(_cml_raw, projectrelativepath = Some("")),
          _observed(_cml_raw, projectrelativepath = Some("../customer.cml")),
          _observed(_cml_raw, projectrelativepath = Some("model/customer\u00a0draft.cml"))
        )

        When("each malformed observation is compared before deciding version completeness")
        val reports = invalidscenario.map(observation =>
          InternalModelSourceSnapshotFreshness.compare(scenariosnapshot, observation)
        ) ++ invalidcml.map(observation =>
          InternalModelSourceSnapshotFreshness.compare(cmlsnapshot, observation)
        )

        Then("every malformed caller value rejects without an invented observation or Incomplete result")
        reports.foreach { report =>
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          report.observedSourceAuthority shouldBe None
          report.observedSourceIdentity shouldBe None
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
        }
      }

      "treat safe supplied missing-local CML paths as evidence without requiring filesystem presence" in {
        Given("a complete CML baseline and observation whose supplied safe path names no required local file")
        val snapshot = _snapshot("cml-baseline", _cml_basis(_cml_raw, "missing/read-only-model.cml"))
        val original = snapshot.clone()
        val observation = _observed(_cml_raw, projectrelativepath = Some("missing/read-only-model.cml"))

        When("the supplied declared references and path are compared")
        val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

        Then("complete reference agreement requires no file read and leaves caller inputs untouched")
        report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
        report.changedDimensionNames shouldBe Vector.empty
        report.missingDimensionNames shouldBe Vector.empty
        snapshot should contain theSameElementsInOrderAs original
      }
    }

    "admit strict structure before considering source observations" which {
      "give malformed baseline precedence over observed and all non-observed outcomes" in {
        Given("a closed envelope using the rejected old schema and every observation form including missing input")
        val snapshot = _bytes(_set(_envelope("scenario", _scenario_basis), "schemaVersion", Json.fromString("1.0")))
        val observations = Vector(
          _observed(_scenario_raw),
          InternalModelLiveSourceObservation.Unavailable("offline"),
          InternalModelLiveSourceObservation.Unauthorized("denied"),
          InternalModelLiveSourceObservation.Malformed("bad response"),
          InternalModelLiveSourceObservation.AmbiguousOrConflicting("conflict"),
          null
        )

        observations.foreach { observation =>
          When("the malformed baseline is offered with that observation")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)
          val admittedkind = InternalModelSourceSnapshotFreshness.validatedSnapshotKind(snapshot)

          Then("baseline rejection precedes the supplied live outcome and exposes no fabricated basis")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          admittedkind.isLeft shouldBe true
          report.snapshotKind shouldBe None
          report.baselineSourceAuthority shouldBe None
          report.observedSourceIdentity shouldBe None
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
          report.reason.get should include ("schemaVersion must be 2.0")
        }
      }

      "admit equivalent whitespace, key order, and JSON escaping across envelope and nested bases" in {
        Given("all four V2 envelopes with reversed object order, harmless whitespace, and equivalent escaped strings")
        val cases = Vector(
          (_envelope("scenario", _scenario_basis), _observed(_scenario_raw)),
          (_envelope("model-context", _model_context_basis), _observed(_model_raw)),
          (_envelope("glossary-bok", _glossary_basis), _observed(_glossary_raw)),
          (_envelope("cml-baseline", _cml_basis(_cml_raw, "model/customer.cml")), _observed(_cml_raw, projectrelativepath = Some("model/customer.cml")))
        )

        cases.foreach { case (envelope, observation) =>
          And("the alternate JSON presentation preserves the decoded source and basis values")
          val representation = ("\n \t" + _reverse_objects(envelope).spaces2 + " \n\t")
            .replace("catalog-authority", "\\u0063atalog-authority")
            .replace("source-identity", "source-\\u0069dentity")
            .replace("component-identity", "\\u0063omponent-identity")
            .replace("glossary definition", "\\u0067lossary definition")
            .replace("model/customer.cml", "model\\/customer.cml")
          val snapshot = representation.getBytes(StandardCharsets.UTF_8)

          When("that equivalent presentation is admitted and compared")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, observation)

          Then("presentation variation preserves complete declared-reference agreement")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
          report.reason shouldBe None
        }
      }

      "reject invalid UTF-8, BOM, non-JSON, and duplicate members at every object depth" in {
        Given("malformed serialization and duplicate root, source, basis, trace, fact, and definition members")
        val scenario = _printer.print(_envelope("scenario", _scenario_basis))
        val model = _printer.print(_envelope("model-context", _model_context_basis))
        val glossary = _printer.print(_envelope("glossary-bok", _glossary_basis))
        val variants = Vector(
          Array(0xc3.toByte, 0x28.toByte),
          Array(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ scenario.getBytes(StandardCharsets.UTF_8),
          "{not-json}".getBytes(StandardCharsets.UTF_8),
          "schemaVersion: 2.0\nsnapshotKind: scenario\n".getBytes(StandardCharsets.UTF_8),
          (scenario + "{}").getBytes(StandardCharsets.UTF_8),
          scenario.replace("\"schemaVersion\":\"2.0\"", "\"schemaVersion\":\"2.0\",\"schemaVersion\":\"2.0\"").getBytes(StandardCharsets.UTF_8),
          scenario.replace("\"authority\":\"catalog-authority\"", "\"authority\":\"catalog-authority\",\"authority\":\"catalog-authority\"").getBytes(StandardCharsets.UTF_8),
          scenario.replace("\"scenarioId\":\"scenario-identity\"", "\"scenarioId\":\"scenario-identity\",\"scenarioId\":\"scenario-identity\"").getBytes(StandardCharsets.UTF_8),
          scenario.replace("\"scenarioAnchor\":\"scenario-anchor\"", "\"scenarioAnchor\":\"scenario-anchor\",\"scenarioAnchor\":\"scenario-anchor\"").getBytes(StandardCharsets.UTF_8),
          model.replace("\"sourceAnchor\":\"model-source-anchor\"", "\"sourceAnchor\":\"model-source-anchor\",\"sourceAnchor\":\"model-source-anchor\"").getBytes(StandardCharsets.UTF_8),
          glossary.replace("\"termIdentity\":\"term-identity\"", "\"termIdentity\":\"term-identity\",\"termIdentity\":\"term-identity\"").getBytes(StandardCharsets.UTF_8)
        )

        forAll(Gen.oneOf(variants)) { snapshot =>
          When("the malformed serialization is offered without repair")
          val report = InternalModelSourceSnapshotFreshness.compare(snapshot, _observed(_scenario_raw))

          Then("the baseline is malformed before any observed source can supply meaning")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          report.snapshotKind shouldBe None
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
        }
      }

      "reject old schemas, hash fields, wrong or missing types and fields, and empty required content or identities" in {
        Given("V2 JSON values violating closed envelopes or required source and selected-basis values")
        val scenario = _envelope("scenario", _scenario_basis)
        val source = _source()
        val fact = _model_fact("model-source-anchor")
        val definition = _glossary_entry("glossary-source-anchor")
        val invalid = Vector(
          Json.Null,
          _set(scenario, "schemaVersion", Json.fromString("1.0")),
          _set(scenario, "schemaVersion", Json.fromInt(2)),
          _set(scenario, "extra", Json.True),
          _remove(scenario, "source"),
          _set(scenario, "source", Json.arr()),
          _set(scenario, "snapshotKind", Json.fromString("unsupported")),
          _set(scenario, "basis", _cml_basis(_cml_raw, "model/customer.cml")),
          _set(scenario, "basis", Json.Null),
          _set(scenario, "source", _set(source, "sha256", Json.fromString("forbidden-extra-field"))),
          _set(scenario, "basis", _set(_scenario_basis, "sha256", Json.fromString("forbidden-extra-field"))),
          _set(scenario, "source", _remove(source, "locator")),
          _set(scenario, "source", _remove(source, "revision")),
          _set(scenario, "source", _set(source, "authority", Json.Null)),
          _set(scenario, "source", _set(source, "authority", Json.fromString(""))),
          _set(scenario, "source", _set(source, "identity", Json.fromInt(1))),
          _set(scenario, "source", _set(source, "identity", Json.fromString(""))),
          _set(scenario, "source", _set(source, "locator", Json.fromString(""))),
          _set(scenario, "source", _set(source, "locator", Json.arr())),
          _set(scenario, "source", _set(source, "revision", Json.fromString(""))),
          _set(scenario, "source", _set(source, "revision", Json.fromInt(1))),
          _envelope("scenario", _set(_scenario_basis, "content", Json.fromString(""))),
          _envelope("scenario", _remove(_scenario_basis, "content")),
          _envelope("scenario", _set(_scenario_basis, "scenarioId", Json.fromString(""))),
          _envelope("scenario", _set(_scenario_basis, "traceLinks", Json.Null)),
          _envelope("scenario", _set(_scenario_basis, "traceLinks", Json.arr(_set(_trace_link("anchor"), "semanticIdentityKind", Json.fromString("label"))))),
          _envelope("scenario", _set(_scenario_basis, "traceLinks", Json.arr(_remove(_trace_link("anchor"), "semanticIdentity")))),
          _envelope("model-context", _set(_model_context_basis, "contextIdentity", Json.fromString(""))),
          _envelope("model-context", _set(_model_context_basis, "facts", Json.arr())),
          _envelope("model-context", _remove(_model_context_basis, "facts")),
          _envelope("model-context", _set(_model_context_basis, "facts", Json.arr(_set(fact, "content", Json.fromString(""))))),
          _envelope("model-context", _set(_model_context_basis, "facts", Json.arr(_set(fact, "sourceAnchor", Json.fromString(""))))),
          _envelope("model-context", _set(_model_context_basis, "facts", Json.arr(_set(fact, "limitations", Json.arr(Json.fromString("")))))),
          _envelope("glossary-bok", Json.obj("entries" -> Json.arr())),
          _envelope("glossary-bok", Json.obj()),
          _envelope("glossary-bok", Json.obj("entries" -> Json.arr(_set(definition, "definition", Json.fromString(""))))),
          _envelope("glossary-bok", Json.obj("entries" -> Json.arr(_set(definition, "termIdentity", Json.Null)))),
          _envelope("glossary-bok", Json.obj("entries" -> Json.arr(_set(definition, "termLabel", Json.fromString("")))))
        )

        invalid.foreach { envelope =>
          When("the structurally invalid source or basis value is admitted")
          val report = InternalModelSourceSnapshotFreshness.compare(_bytes(envelope), _observed(_scenario_raw))

          Then("closed-shape and required-value failures reject without defaults or normalization")
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed
          report.snapshotKind shouldBe None
          report.changedDimensionNames shouldBe Vector.empty
          report.missingDimensionNames shouldBe Vector.empty
        }
      }

      "retain full LF-normalized content and reject non-normalized Scenario, fact, and definition text" in {
        Given("complete content with preserved Unicode and whitespace plus CR or leading-BOM variants")
        val content = "  日本語 e\u0301\ncomplete second line \t\n"
        val valid = Vector(
          _snapshot("scenario", _set(_scenario_basis, "content", Json.fromString(content))),
          _snapshot("model-context", _set(_model_context_basis, "facts", Json.arr(_model_fact("anchor", content = content)))),
          _snapshot("glossary-bok", Json.obj("entries" -> Json.arr(_glossary_entry("anchor", definition = content))))
        )
        val invalid = Vector("line\r\n", "line\r", "\ufeffbody").flatMap { text =>
          Vector(
            _snapshot("scenario", _set(_scenario_basis, "content", Json.fromString(text))),
            _snapshot("model-context", _set(_model_context_basis, "facts", Json.arr(_model_fact("anchor", content = text)))),
            _snapshot("glossary-bok", Json.obj("entries" -> Json.arr(_glossary_entry("anchor", definition = text))))
          )
        }

        When("complete selected text is interpreted exactly as supplied")
        val admitted = valid.map(snapshot => InternalModelSourceSnapshotFreshness.compare(snapshot, _observed(_scenario_raw)))
        val rejected = invalid.map(snapshot => InternalModelSourceSnapshotFreshness.compare(snapshot, _observed(_scenario_raw)))

        Then("normalized complete text is admitted and non-normalized text rejects without repair")
        admitted.foreach(_.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged)
        rejected.foreach(_.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed)
      }

      "preserve tuple uniqueness and UTF-8 ordering while keeping distinct conflicting anchored claims" in {
        Given("ordered distinct traces, conflicting anchored facts and definitions, and ordered limitations")
        val limitations = Json.arr(Json.fromString("\ue000"), Json.fromString("\ud800\udc00"))
        val facts = Json.arr(
          _model_fact("a-anchor", content = "claim one\n", limitations = limitations),
          _model_fact("z-anchor", content = "conflicting claim\n", limitations = limitations)
        )
        val definitions = Json.arr(
          _glossary_entry("a-anchor", definition = "definition one\n", limitations = limitations),
          _glossary_entry("z-anchor", definition = "conflicting definition\n", limitations = limitations)
        )
        val valid = Vector(
          _snapshot("scenario", _set(_scenario_basis, "traceLinks", Json.arr())),
          _snapshot("scenario", _set(_scenario_basis, "traceLinks", Json.arr(_trace_link("a-anchor"), _trace_link("z-anchor")))),
          _snapshot("model-context", _set(_model_context_basis, "facts", facts)),
          _snapshot("glossary-bok", Json.obj("entries" -> definitions))
        )
        val invalid = Vector(
          _snapshot("scenario", _set(_scenario_basis, "traceLinks", Json.arr(_trace_link("z-anchor"), _trace_link("a-anchor")))),
          _snapshot("scenario", _set(_scenario_basis, "traceLinks", Json.arr(_trace_link("a-anchor"), _trace_link("a-anchor")))),
          _snapshot("model-context", _set(_model_context_basis, "facts", Json.arr(_model_fact("z"), _model_fact("a")))),
          _snapshot("model-context", _set(_model_context_basis, "facts", Json.arr(_model_fact("a"), _model_fact("a")))),
          _snapshot("glossary-bok", Json.obj("entries" -> Json.arr(_glossary_entry("z"), _glossary_entry("a")))),
          _snapshot("glossary-bok", Json.obj("entries" -> Json.arr(_glossary_entry("a"), _glossary_entry("a"))))
        ) ++ Vector(
          Json.arr(Json.fromString("z"), Json.fromString("a")),
          Json.arr(Json.fromString("a"), Json.fromString("a")),
          Json.arr(Json.fromString("\ud800\udc00"), Json.fromString("\ue000"))
        ).flatMap { unordered =>
          Vector(
            _snapshot("model-context", _set(_model_context_basis, "facts", Json.arr(_model_fact("anchor", limitations = unordered)))),
            _snapshot("glossary-bok", Json.obj("entries" -> Json.arr(_glossary_entry("anchor", limitations = unordered))))
          )
        }

        When("the original text-order and complete identity-and-anchor tuples are admitted")
        val admitted = valid.map(snapshot => InternalModelSourceSnapshotFreshness.compare(snapshot, _observed(_scenario_raw)))
        val rejected = invalid.map(snapshot => InternalModelSourceSnapshotFreshness.compare(snapshot, _observed(_scenario_raw)))

        Then("distinct conflicting anchors survive while duplicate or unordered tuples and limitations reject")
        admitted.foreach(_.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged)
        rejected.foreach(_.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed)
      }

      "admit ordinary padded CML payload and reject encoding, lexical length, mismatch, and unsafe paths" in {
        Given("valid ordinary CML payloads and invalid Base64, length, and POSIX-path variants")
        val path = "model/customer.cml"
        val basis = _cml_basis(_cml_raw, path)
        val observation = _observed(_cml_raw, projectrelativepath = Some(path))
        val valid = Vector(
          _snapshot("cml-baseline", basis),
          _snapshot("cml-baseline", _cml_basis(Array.emptyByteArray, path)),
          _snapshot("cml-baseline", _cml_basis(Array(0xff.toByte, 0x00.toByte), path))
        )
        val invalidencoding = Vector("Zg", "Zh==", "Zg==\n", "_w==", "Zg===", "not base64")
          .map(encoded => _snapshot("cml-baseline", _set(_cml_basis(Array('f'.toByte), path), "rawBytesBase64", Json.fromString(encoded))))
        val invalidlength = Vector(Json.fromInt(-1), Json.fromString("15"), Json.Null, Json.fromInt(_cml_raw.length + 1))
          .map(length => _snapshot("cml-baseline", _set(basis, "byteLength", length)))
        val lexical = Vector("-0", _cml_raw.length.toString + ".0", _cml_raw.length.toString + "e0",
          "0" + _cml_raw.length.toString, "9223372036854775808").map { length =>
          _printer.print(_envelope("cml-baseline", basis))
            .replace("\"byteLength\":" + _cml_raw.length, "\"byteLength\":" + length)
            .getBytes(StandardCharsets.UTF_8)
        }
        val paths = Vector("", "/model/customer.cml", "model//customer.cml", "./customer.cml", "model/../customer.cml",
          "model\\customer.cml", "model/customer draft.cml", "model/customer\tdraft.cml",
          "model/customer\u0000draft.cml", "model/customer\u00a0draft.cml", "model/customer\u2003draft.cml")
        val invalidpaths = paths.map(unsafe => _snapshot("cml-baseline", _cml_basis(_cml_raw, unsafe)))
        val invalid = invalidencoding ++ invalidlength ++ lexical ++ invalidpaths

        When("only ordinary encoding, decoded length, and declared path syntax are admitted")
        val admitted = valid.map(snapshot => InternalModelSourceSnapshotFreshness.compare(snapshot, observation))
        val rejected = invalid.map(snapshot => InternalModelSourceSnapshotFreshness.compare(snapshot, observation))
        val safepaths = paths.map(InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath)

        Then("payload syntax remains strict without providing a checksum or content-comparison control")
        admitted.foreach { report =>
          report.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
          report.changedDimensionNames shouldBe Vector.empty
        }
        rejected.foreach(_.status shouldBe InternalModelSnapshotFreshnessStatus.Malformed)
        safepaths should contain only (false)
      }
    }
  }

  private def _snapshot(
    kind: String,
    basis: Json,
    authority: String = _authority,
    identity: String = _identity,
    revision: Json = Json.fromString(_revision)
  ): Array[Byte] =
    _bytes(_envelope(kind, basis, authority, identity, revision))

  private def _envelope(
    kind: String,
    basis: Json,
    authority: String = _authority,
    identity: String = _identity,
    revision: Json = Json.fromString(_revision)
  ): Json =
    Json.obj(
      "basis" -> basis,
      "schemaVersion" -> Json.fromString("2.0"),
      "snapshotKind" -> Json.fromString(kind),
      "source" -> _source(authority, identity, revision)
    )

  private def _source(
    authority: String = _authority,
    identity: String = _identity,
    revision: Json = Json.fromString(_revision)
  ): Json =
    Json.obj(
      "authority" -> Json.fromString(authority),
      "identity" -> Json.fromString(identity),
      "locator" -> Json.fromString("navigation-only"),
      "revision" -> revision
    )

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
      "facts" -> Json.arr(_model_fact("model-source-anchor"))
    )

  private def _model_fact(
    anchor: String,
    content: String = "model fact\n",
    limitations: Json = Json.arr(Json.fromString("source limitation"))
  ): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-identity"),
      "content" -> Json.fromString(content),
      "limitations" -> limitations,
      "projectionContextIdentity" -> Json.fromString("projection-context-identity"),
      "semanticIdentity" -> Json.fromString("semantic-identity"),
      "semanticIdentityKind" -> Json.fromString("relationship"),
      "sourceAnchor" -> Json.fromString(anchor)
    )

  private def _glossary_basis: Json =
    Json.obj("entries" -> Json.arr(_glossary_entry("glossary-source-anchor")))

  private def _glossary_entry(
    anchor: String,
    definition: String = "glossary definition\n",
    limitations: Json = Json.arr(Json.fromString("source limitation"))
  ): Json =
    Json.obj(
      "definition" -> Json.fromString(definition),
      "limitations" -> limitations,
      "sourceAnchor" -> Json.fromString(anchor),
      "termIdentity" -> Json.fromString("term-identity"),
      "termLabel" -> Json.fromString("Term")
    )

  private def _cml_basis(rawbytes: Array[Byte], path: String): Json =
    Json.obj(
      "byteLength" -> Json.fromInt(rawbytes.length),
      "projectRelativePath" -> Json.fromString(path),
      "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(rawbytes))
    )

  private def _set(json: Json, key: String, value: Json): Json =
    json.mapObject(_.add(key, value))

  private def _remove(json: Json, key: String): Json =
    json.mapObject(_.remove(key))

  private def _reverse_objects(json: Json): Json = {
    json.asObject match {
      case Some(objectvalue) =>
        Json.fromJsonObject(JsonObject.fromIterable(objectvalue.toVector.reverse.map { case (key, value) =>
          key -> _reverse_objects(value)
        }))
      case None =>
        json.asArray match {
          case Some(values) => Json.fromValues(values.map(_reverse_objects))
          case None => json
        }
    }
  }

  private def _bytes(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
}
