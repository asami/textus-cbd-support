package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import io.circe.{Json, JsonObject, Printer}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewReportCodecSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CarReviewReportCodec" should {
    "decode the canonical contract into distinct domain value types" in {
      Given("the representative P5-03 canonical Review Report")
      val body = Files.readString(_report_path)

      When("the strict runtime codec decodes it")
      val report = CarReviewReportCodec.decode(body).fold(_fail_codec, identity)

      Then("Review, report, digest, provider, Evidence, Observation, and capability identities remain typed")
      report.reviewId shouldBe ReviewId("review-example-001")
      report.reportId shouldBe ReviewReportId("report-example-001")
      report.reportDigest shouldBe
        ReviewDigest("sha256:bcdb18448fcafbb5ed6e220518172768d0c50076fc6574b5ce3ddeff11b1c89f")
      report.schemaVersion shouldBe ReviewSchemaVersion("textus.cbd.review-report.v1")
      report.documentType shouldBe ReviewDocumentType("review-report")
      report.target.kind shouldBe ReviewTargetKind("project")
      report.execution.providers.head.provider.id shouldBe ReviewProviderId("cozy")
      report.execution.providers.head.state shouldBe ReviewProviderState("completed")
      report.evidence.map(_.id).toSet should contain(ReviewEvidenceId("report-evidence-project-yaml"))
      report.observations.map(_.id).toSet should contain(ReviewObservationId("report-finding-missing-rationale"))
      report.observations.map(_.`type`).toSet shouldBe Set(
        ReviewObservationType("finding"), ReviewObservationType("assurance"), ReviewObservationType("unknown")
      )
      report.assessments.map(_.capabilityId) shouldBe Vector(
        ReviewCapabilityId("quality.domain.identity-consistency")
      )
      report.assessments.head.maturity shouldBe ReviewMaturity("partial")
      report.gate.result shouldBe ReviewGateResult("fail")

      And("canonical encoding is stable and re-admissible")
      val first = CarReviewReportCodec.encode(report).fold(_fail_codec, identity)
      val second = CarReviewReportCodec.encode(report).fold(_fail_codec, identity)
      first shouldBe second
      CarReviewReportCodec.decode(first).fold(_fail_codec, _.reportDigest) shouldBe report.reportDigest
    }

    "preserve one digest across volatile execution identity and array arrival order" in {
      Given("the same report with different Run metadata and reversed arrays")
      val original = _load_json(_report_path)
      val alternative = _alternative_run_metadata(_reverse_arrays(original))

      When("both wire documents are admitted")
      val originalreport = CarReviewReportCodec.decode(_printer.print(original)).fold(_fail_codec, identity)
      val alternativereport = CarReviewReportCodec.decode(_printer.print(alternative)).fold(_fail_codec, identity)

      Then("their deterministic content digest is identical")
      originalreport.reportId should not be alternativereport.reportId
      originalreport.reviewId should not be alternativereport.reviewId
      CarReviewReportCodec.calculateDigest(originalreport) shouldBe Right(originalreport.reportDigest)
      CarReviewReportCodec.calculateDigest(alternativereport) shouldBe Right(originalreport.reportDigest)

      And("canonical encoding removes arrival-order differences without removing execution identity")
      val encoded = CarReviewReportCodec.encode(alternativereport).fold(_fail_codec, identity)
      encoded should include("another-report")
      encoded should include("2030-01-01T00:00:04Z")
      CarReviewReportCodec.encode(alternativereport).fold(_fail_codec, identity) shouldBe encoded
    }

    "reject incompatible, stale, or internally inconsistent reports" in {
      Given("one valid canonical report and seven contract violations")
      val report = _load_json(_report_path)
      val unknownevidencefield = report.mapObject { root =>
        val evidence = _array(root("evidence").getOrElse(fail("Missing evidence")))
        root.add("evidence", Json.fromValues(
          evidence.updated(0, evidence.head.mapObject(_.add("secret", Json.fromString("not-admitted"))))
        ))
      }
      val staledigest = report.mapObject(
        _.add("reportDigest", Json.fromString("sha256:0000000000000000000000000000000000000000000000000000000000000000"))
      )
      val unresolvedreference = report.mapObject { root =>
        val observations = _array(root("observations").getOrElse(fail("Missing observations")))
        val changed = observations.head.mapObject(
          _.add("evidenceIds", Json.arr(Json.fromString("missing-evidence")))
        )
        root.add("observations", Json.fromValues(observations.updated(0, changed)))
      }
      val invalidcoverage = report.mapObject { root =>
        val assessments = _array(root("assessments").getOrElse(fail("Missing assessments")))
        val changed = assessments.head.mapObject { assessment =>
          val coverage = assessment("coverage").getOrElse(fail("Missing coverage")).mapObject(
            _.add("basisPoints", Json.fromInt(9000))
          )
          assessment.add("coverage", coverage)
        }
        root.add("assessments", Json.fromValues(assessments.updated(0, changed)))
      }
      val duplicateprovider = report.mapObject { root =>
        val execution = root("execution").getOrElse(fail("Missing execution")).mapObject { value =>
          val providers = _array(value("providers").getOrElse(fail("Missing providers")))
          value.add("providers", Json.fromValues(providers ++ providers.headOption))
        }
        root.add("execution", execution)
      }
      val unsafelocation = report.mapObject { root =>
        val evidence = _array(root("evidence").getOrElse(fail("Missing evidence")))
        val changed = evidence.head.mapObject(
          _.add("location", Json.obj("path" -> Json.fromString("/Users/example/secret/project.yaml")))
        )
        root.add("evidence", Json.fromValues(evidence.updated(0, changed)))
      }
      val nonnormalizedlocation = report.mapObject { root =>
        val evidence = _array(root("evidence").getOrElse(fail("Missing evidence")))
        val changed = evidence.head.mapObject(
          _.add("location", Json.obj("path" -> Json.fromString("src/main/../secret.scala")))
        )
        root.add("evidence", Json.fromValues(evidence.updated(0, changed)))
      }

      When("each invalid document reaches strict decoding")
      val failures = Vector(
        unknownevidencefield, staledigest, unresolvedreference, invalidcoverage, duplicateprovider, unsafelocation,
        nonnormalizedlocation
      ).map(
        json => CarReviewReportCodec.decode(_printer.print(json)).left.map(_.code)
      )

      Then("wire, digest, reference, coverage, provider, and location violations fail distinctly")
      failures shouldBe Vector(
        Left("unknown-field"),
        Left("digest-mismatch"),
        Left("unresolved-reference"),
        Left("invalid-coverage"),
        Left("duplicate-provider"),
        Left("unsafe-location"),
        Left("unsafe-location")
      )
    }

    "calculate a replacement digest only from validated deterministic content" in {
      Given("a decoded report whose gate explanation changes")
      val report = CarReviewReportCodec.decode(Files.readString(_report_path)).fold(_fail_codec, identity)
      val changed = report.copy(
        gate = report.gate.copy(reasons = Vector("The deterministic gate explanation changed."))
      )

      When("the codec recalculates and encodes the report")
      val recalculated = CarReviewReportCodec.withCalculatedDigest(changed).fold(_fail_codec, identity)
      val encoded = CarReviewReportCodec.encode(recalculated).fold(_fail_codec, identity)

      Then("the digest changes and the resulting wire document is self-verifying")
      recalculated.reportDigest should not be report.reportDigest
      CarReviewReportCodec.decode(encoded).fold(_fail_codec, _.reportDigest) shouldBe recalculated.reportDigest
    }
  }

  private val _report_path = Path.of("docs", "spec", "examples", "car-review-report-v1.json")
  private val _printer = Printer.noSpaces.copy(dropNullValues = true, sortKeys = true)

  private def _load_json(path: Path): Json =
    parse(Files.readString(path)).fold(error => fail(error.message), identity)

  private def _array(json: Json): Vector[Json] =
    json.asArray.getOrElse(fail("Expected JSON array"))

  private def _alternative_run_metadata(report: Json): Json =
    report.mapObject { root =>
      val execution = root("execution").getOrElse(fail("Missing execution")).mapObject { value =>
        val providers = _array(value("providers").getOrElse(fail("Missing providers"))).map(
          _.mapObject(
            _.add("startedAt", Json.fromString("2030-01-01T00:00:01Z"))
              .add("completedAt", Json.fromString("2030-01-01T00:00:02Z"))
          )
        )
        value
          .add("startedAt", Json.fromString("2030-01-01T00:00:00Z"))
          .add("completedAt", Json.fromString("2030-01-01T00:00:03Z"))
          .add("providers", Json.fromValues(providers))
      }
      val baseline = root("baseline").map(_.mapObject(_.add("reportId", Json.fromString("another-baseline"))))
      val changed = root
        .add("reportId", Json.fromString("another-report"))
        .add("reviewId", Json.fromString("another-review"))
        .add("createdAt", Json.fromString("2030-01-01T00:00:04Z"))
        .add("execution", execution)
      baseline.fold(changed)(changed.add("baseline", _))
    }

  private def _reverse_arrays(json: Json): Json =
    json.arrayOrObject(
      json,
      values => Json.fromValues(values.reverse.map(_reverse_arrays)),
      fields => Json.fromJsonObject(
        JsonObject.fromIterable(fields.toVector.map { case (key, value) => key -> _reverse_arrays(value) })
      )
    )

  private def _fail_codec(error: CarReviewCodecFailure): Nothing =
    fail(s"${error.code} at ${error.path}: ${error.message}")
}
