package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Composed continuation/application semantics across terminal independent JVMs.
 * Synthetic command observations document admission, not real Cozy execution.
 *
 * @since   Oct.  4, 2026
 * @version Oct.  4, 2026
 */
final class InternalModelEndToEndSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  private val _support = InternalModelEndToEndSupport
  private final case class GeneratedVariant(label: String, reverse: Boolean, carrier: Long)

  "Internal-model end-to-end continuation" should {
    "terminal producer and independent consumer" which {
      "E1 preserve complete selected realization, all eight DTOs and rich candidate facts before application" in {
        Given("an explicit rich/all-eight test-owner graph, two canonical CML baselines and separate caller input")
        val label = "continuity 日本語"
        When("the producer reaches terminal exit and a fresh isolated consumer reloads the existing package")
        val produced = _support.produce(label)
        val consumer = _support.consume(produced, "inspect")
        Then("the complete source-backed semantic result and Phase 9 impact/diff values survive without source or cursor mutation")
        _terminal_pair(produced, consumer)
        _text(consumer.report, "status") shouldBe "Inspected"
        _json(consumer.report, "before") shouldBe _json(produced.producer.report, "before")
        _json(consumer.report, "phase9") shouldBe _json(produced.producer.report, "phase9")
        _json(consumer.report, "selection") shouldBe _json(produced.producer.report, "selection")
        _counts(consumer.report, "beforeCounts") should have size 8
        _counts(consumer.report, "beforeCounts").forall(_ > 0) shouldBe true
        _flag(consumer.report, "beforeSidecars") shouldBe true
        _flag(consumer.report, "preApplicationSourcesUnchanged") shouldBe true
        _flag(consumer.report, "cursorUnchanged") shouldBe true
        produced.producer.report.hcursor.get[Vector[String]]("paths").toOption.get should contain allOf (
          "project.yaml", "src/main/internal-model/manifest.yaml", "cml/alpha.cml", "cml/beta.cml", "src/main/cml/original.cml")
        produced.producer.report.hcursor.get[Vector[String]]("paths").toOption.get should not contain "caller-input.json"
      }
      "E2 apply only exact fresh independent human and source-owner input, pending separate acceptance" in {
        Given("explicit Approved human input and exact two-target canonical-source ownership outside the package")
        val label = "actual human and owner"
        When("a new consumer freshly admits those inputs, performs native replacement and composes synthetic successful validation")
        val produced = _support.produce(label)
        val consumer = _support.consume(produced)
        Then("all eight complete projections return ReprojectedPendingAcceptance, retaining actual effects and the unchanged cursor")
        _terminal_pair(produced, consumer)
        _text(consumer.report, "status") shouldBe "ReprojectedPendingAcceptance"
        _flag(consumer.report, "physicalEffectsRetained") shouldBe true
        _flag(consumer.report, "requiredSourcesUnchanged") shouldBe true
        _flag(consumer.report, "cursorUnchanged") shouldBe true
        _flag(consumer.report, "sidecars") shouldBe true
        _counts(consumer.report, "counts") should have size 8
        _counts(consumer.report, "counts").forall(_ > 0) shouldBe true
        val application = _json(consumer.report, "applicationReport")
        val post = _json(consumer.report, "postValidationReport")
        _field(post, "application") shouldBe application
        _field(post, "problems").asArray.get shouldBe empty
        val observations = _field(post, "commands").asArray.get.toVector
        observations should have size 2
        observations.map(value => _field(value, "outcome").hcursor.downField("fields").get[Int]("exitcode").toOption.get) shouldBe Vector(0, 0)
        observations.map(value => _field(_field(value, "outcome"), "stderr").hcursor.get[String]("value").toOption.get) shouldBe
          Vector.fill(2)("synthetic unit observation")
        val gate = _field(application, "gate")
        val currentapproval = _field(_field(gate, "continuation"), "approval").hcursor.get[Json]("value").toOption.get
        _field(currentapproval, "record") shouldBe _field(_field(gate, "originalapproval"), "record")
      }
    }
    "fresh authorization and source-version refusal" which {
      "E3 retain no-write refusal for missing/rejected human input, wrong exact candidate revision or missing ownership" in {
        Given("a package with provider-approved flags and stored human claims, plus four independently supplied refusals")
        val changes: Vector[Json => Json] = Vector(
          value => _replace(value, Vector("current", "humandecision"), Json.Null),
          value => _replace(value, Vector("current", "humandecision", "approval", "decision"), Json.fromString("rejected")),
          value => _replace(value, Vector("current", "candidateartifact", "artifactRevision"), Json.fromLong(19L)),
          value => _replace(value, Vector("authority"), Json.Null))
        When("each fresh consumer receives only its changed independent input and asks for native application")
        val results = changes.map { change =>
          val produced = _support.produce()
          _support.replaceCaller(produced, change)
          val consumer = _support.consume(produced)
          (produced, consumer)
        }
        Then("a valid fresh process cannot promote stored approval or provider flags into mutation permission")
        results.foreach { case (produced, consumer) =>
          _terminal_pair(produced, consumer)
          Set("Rejected", "AdmissionRejected") should contain(_text(consumer.report, "status"))
          _flag(consumer.report, "sourcesUnchanged") shouldBe true
          consumer.report.hcursor.downField("postValidationReport").succeeded shouldBe false
        }
      }
      "E4 require re-review for explicit owner-version drift and remain incomplete for a missing version" in {
        Given("independent alpha source-owner input with either a declared changed revision or an explicit missing revision")
        val variants = Vector(Json.fromString("owner-drift-alpha") -> "ReReviewRequired", Json.Null -> "Incomplete")
        When("each consuming gate evaluates current source evidence immediately before application")
        val results = variants.map { case (revision, expected) =>
          val produced = _support.produce()
          _support.replaceCaller(produced, value => {
            val entries = value.hcursor.get[Vector[Json]]("liveSources").toOption.get.map { entry =>
              if (entry.hcursor.downField("reference").get[String]("artifactId").toOption.contains("snapshot-cml-alpha"))
                _replace(entry, Vector("source", "revision"), revision) else entry
            }
            _replace(value, Vector("liveSources"), Json.fromValues(entries))
          })
          (produced, _support.consume(produced), expected)
        }
        Then("both explicit outcomes retain original source effects and grant no application plan")
        results.foreach { case (produced, consumer, expected) =>
          _terminal_pair(produced, consumer)
          _text(consumer.report, "status") shouldBe "Rejected"
          _text(consumer.report, "gateEligibility") shouldBe expected
          _flag(consumer.report, "sourcesUnchanged") shouldBe true
          val gate = _field(_json(consumer.report, "applicationReport"), "gate")
          _field(gate, "plan").hcursor.get[String]("type").toOption.get shouldBe "None"
        }
      }
    }
    "post-application failed or unavailable validation" which {
      "E5 preserve physical effects and failed/incomplete state when a required command fails or is missing" in {
        Given("actual native two-file application followed by explicitly synthetic failed or missing required lint observations")
        val modes = Vector("synthetic-failure" -> "Failed", "synthetic-missing" -> "Incomplete")
        When("fresh consumers compose actual application reports with those unsuccessful observations and the refreshed owner graph")
        val results = modes.map { case (mode, expected) =>
          val produced = _support.produce()
          (produced, _support.consume(produced, mode), expected)
        }
        Then("neither unsuccessful validation returns successful continuity, rollback, cursor advance or acceptance")
        results.foreach { case (produced, consumer, expected) =>
          _terminal_pair(produced, consumer)
          _text(consumer.report, "status") shouldBe expected
          _flag(consumer.report, "physicalEffectsRetained") shouldBe true
          _flag(consumer.report, "cursorUnchanged") shouldBe true
          _counts(consumer.report, "counts") shouldBe empty
          val post = _json(consumer.report, "postValidationReport")
          _field(post, "continuity").hcursor.get[String]("type").toOption.get shouldBe "None"
          _field(post, "problems").asArray.get should not be empty
          _field(post, "application") shouldBe _json(consumer.report, "applicationReport")
        }
      }
    }
    "identity independent of display and carrier control" which {
      "E6 preserve exact selected semantic identities and approval meaning over at least four generated variants" in {
        Given("bounded ScalaCheck labels, independent encounter order and explicit carrier/control-only additions")
        val variant = for {
          label <- Gen.oneOf("display α", "日本語", "renamed 😀", "independent display")
          reverse <- Gen.oneOf(true, false)
          carrier <- Gen.oneOf(43L, 79L)
        } yield GeneratedVariant(label, reverse, carrier)
        val batch = for {
          first <- variant
          second <- variant
          third <- variant
          fourth <- variant
        } yield (first, second, third, fourth)
        forAll(batch, minSuccessful(1)) { case (first, second, third, fourth) =>
          val variants = Vector(first, second, third, fourth)
          When("each generated declaration crosses terminal producer and fresh application/validation owners")
          val results = variants.zipWithIndex.map { case (GeneratedVariant(label, reverse, carrier), index) =>
            // Ensure the generated batch also contains both explicit carrier/control cases.
            val selectedcarrier = if (index % 2 == 0) 43L else carrier.max(79L)
            val produced = _support.produce(label, reverse, selectedcarrier)
            (produced, _support.consume(produced))
          }
          Then("four substantive realizations keep the exact identities, source links, mappings, accepted decision and human meaning")
          results should have size 4
          val selected = _json(results.head._1.producer.report, "selection")
          results.foreach { case (produced, consumer) =>
            _terminal_pair(produced, consumer)
            _json(produced.producer.report, "selection") shouldBe selected
            _json(consumer.report, "selection") shouldBe selected
            _json(consumer.report, "before") shouldBe _json(produced.producer.report, "before")
            _text(consumer.report, "status") shouldBe "ReprojectedPendingAcceptance"
            _counts(consumer.report, "counts").forall(_ > 0) shouldBe true
            _flag(consumer.report, "sidecars") shouldBe true
            _flag(consumer.report, "cursorUnchanged") shouldBe true
          }
        }
      }
    }
  }

  private def _terminal_pair(produced: InternalModelEndToEndSupport.Produced, consumer: InternalModelEndToEndSupport.Child): Unit = {
    withClue(consumer.stdout + "\n" + consumer.stderr) { consumer.exitcode shouldBe 0 }
    produced.producer.terminated shouldBe true
    consumer.terminated shouldBe true
    produced.producer.pid should not be consumer.pid
    produced.producer.pid should not be ProcessHandle.current().pid()
    consumer.pid should not be ProcessHandle.current().pid()
    consumer.report.hcursor.get[Long]("pid").toOption.get shouldBe consumer.pid
    consumer.report.hcursor.get[Long]("producerPid").toOption.get shouldBe produced.producer.pid
    consumer.cwd should not be produced.producer.cwd
    consumer.home should not be produced.producer.home
    consumer.temporary should not be produced.producer.temporary
    consumer.environment.keySet shouldBe Set("HOME", "TMPDIR")
    produced.producer.environment.keySet shouldBe Set("HOME", "TMPDIR")
    val requiredkeys = Set("HOME", "TMPDIR")
    val allowedkeys = if (System.getProperty("os.name") == "Mac OS X") {
      requiredkeys + "__CF_USER_TEXT_ENCODING"
    } else {
      requiredkeys
    }
    val observedkeys = consumer.report.hcursor.get[Vector[String]]("environmentKeys").toOption.get.toSet
    requiredkeys.subsetOf(observedkeys) shouldBe true
    observedkeys.subsetOf(allowedkeys) shouldBe true
  }
  private def _text(value: Json, key: String): String = value.hcursor.get[String](key).toOption.get
  private def _json(value: Json, key: String): Json = value.hcursor.get[Json](key).toOption.get
  private def _flag(value: Json, key: String): Boolean = value.hcursor.get[Boolean](key).toOption.get
  private def _counts(value: Json, key: String): Vector[Int] = value.hcursor.get[Vector[Int]](key).toOption.get
  private def _field(value: Json, name: String): Json = value.hcursor.downField("fields").get[Json](name).toOption.get
  private def _replace(value: Json, path: Vector[String], replacement: Json): Json =
    InternalModelCandidateHumanApprovalValidatorSpec.replacePath(value, path, replacement)
}
