package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import com.sun.jna.{Native, NativeLong, Platform}
import io.circe.Json
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Command admission and actual source-backed all-eight rehydration after real application.
 * Synthetic unit command observations are not real Cozy integration evidence.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  3, 2026
 */
final class InternalModelCmlChangePostValidationSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelCmlChangePostValidation.*
  import InternalModelCandidateHumanApprovalValidatorSpec.{recordReference, reference}
  private val _support = InternalModelCmlChangeAcceptanceSupport
  private val _core = InternalModelContinuationFixture
  private val _application_reference = recordReference("unit-actual-application", 601L)
  private val _post_reference = recordReference("unit-post-validation", 619L)
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(minSuccessful = 6)
  private final case class Scenario(fixture: InternalModelCmlChangeFixture.Fixture,
    application: InternalModelCmlChangeApplication.ApplicationReport,
    refreshed: InternalModelCmlChangeAcceptanceSupport.Refreshed, request: Request)

  "Post-change validation" should {
    "structural input admission" which {
      "return structured failure before rooted effects for null containers and malformed references" in {
        Given("a normalized root and a null request or typed containers without an application graph")
        val root = Path.of("").toAbsolutePath.normalize
        val inputs = Vector[Request](null, Request(null, _post_reference, Vector.empty, None),
          Request(null, _post_reference, null, None), Request(null, _post_reference, Vector.empty, null))
        When("each malformed request reaches the private post-validator")
        val results = inputs.map(validate(root, _))
        Then("no successful report can be manufactured from a malformed graph")
        results.foreach(_.isSuccess shouldBe false)
        results.foreach(_.toOption shouldBe None)
      }
    }
  }

  if (Platform.isMac && Native.POINTER_SIZE == 8 && NativeLong.SIZE == 8) {
    "Post-change validation" should {
      "independently refreshed owners and synthetic unit commands" which {
        "retain actual writes shared realization all eight DTOs and complete sidecars as pending acceptance" in {
          Given("real B writes, fresh independently declared owner revisions and synthetic terminal unit observations")
          _with_applied { scenario =>
            val targets = scenario.application.gate.plan.get.targets
            val before = targets.map(target => Files.readAllBytes(scenario.fixture.root.resolve(target.target.projectRelativePath)).toVector)
            When("the real rooted package freshness and projection owners validate the declared basis")
            val result = validate(scenario.fixture.root, scenario.request)
            Then("the actual full constructor evidence remains pending acceptance with no cursor or CML effect")
            withClue(result.show) { result.isSuccess shouldBe true }
            val report = result.toOption.get
            _pending(report, scenario)
            targets.map(target => Files.readAllBytes(scenario.fixture.root.resolve(target.target.projectRelativePath)).toVector) shouldBe before
            before shouldBe targets.map(_.payload.proposedRawBytes)
            Files.readAllBytes(scenario.fixture.root.resolve("src/main/internal-model/resume.yaml")).toVector shouldBe scenario.refreshed.resumebytes
            val actual = InternalModelProjectionContinuityValidator.validate(scenario.fixture.root).toOption.get
            report.continuity shouldBe Some(actual)
          }
        }

        "retain warnings and complete stdout stderr and exit observations" in {
          Given("a complete real application and a synthetic warning finding with full terminal observation")
          _with_applied { scenario =>
            val stdout = _finding_output("WARN", "cml.unit-warning")
            val commands = scenario.request.commands.updated(0, scenario.request.commands.head.copy(outcome = CommandOutcome.Terminal(0, stdout, "complete diagnostic\n日本語")))
            When("post-validation admits the exact warning output")
            val report = validate(scenario.fixture.root, scenario.request.copy(commands = commands)).toOption.get
            Then("warning evidence survives and does not become a failed validation or acceptance")
            report.disposition shouldBe Disposition.ReprojectedPendingAcceptance
            report.commands shouldBe commands
            report.findings shouldBe Vector(Finding("WARN", "cml.unit-warning", "actual finding text", "cml/alpha.cml", 3))
            report.continuity.nonEmpty shouldBe true
          }
        }

        "preserve independently supplied lower revisions opaque source versions Unicode labels and input permutations" in {
          Given("independent positive references below prior versions, unrelated source-owner revisions and harmless input order")
          forAll(Gen.chooseNum(1L, 10L), Gen.chooseNum(1L, 10L), Gen.chooseNum(1L, 10L),
            Gen.oneOf("表示名 Ω", "é\u0301 構造", "label 😀"), Gen.oneOf(true, false)) { (artifactrevision, recordrevision, snapshotrevision, label, reverse) =>
            _support.withFixture() { fixture =>
              val authority = fixture.request.mutationauthority.get
              val versions = Vector("owner-z-" + recordrevision, "owner-a-" + snapshotrevision)
              val changed = fixture.request.copy(mutationauthority = Some(authority.copy(targets = authority.targets.zip(versions)
                .map { case (target, version) => target.copy(nextsourcerevision = version) })))
              val application = InternalModelCmlChangeApplication.applyApproved(fixture.root, changed, recordReference("generated-application", recordrevision)).toOption.get
              val refreshed = _support.refreshOwner(fixture, application, label, carrierRevision = recordrevision,
                realizationRevision = artifactrevision, realizationRecordRevision = recordrevision, bindingRevision = recordrevision,
                bindingRecordRevision = artifactrevision, modelRevision = snapshotrevision, alphaRevision = artifactrevision,
                betaRevision = snapshotrevision, modelSourceRevision = "model-independent-" + artifactrevision)
              val commands = _support.syntheticCommands(fixture.root, application)
              val owner = if (reverse) refreshed.owner.copy(targetbaselines = refreshed.owner.targetbaselines.reverse) else refreshed.owner
              val request = Request(application, recordReference("generated-post", snapshotrevision), if (reverse) commands.reverse else commands, Some(owner))
              When("the explicit unordered owner and command graph reaches actual rooted rehydration")
              val report = validate(fixture.root, request).toOption.get
              Then("exact supplied bindings and all sidecars survive without numeric ordering or content permission")
              report.disposition shouldBe Disposition.ReprojectedPendingAcceptance
              report.application shouldBe application
              report.commands shouldBe request.commands
              report.owner shouldBe Some(owner)
              report.continuity.get.realization.realizationReference.recordRevision.value shouldBe recordrevision
              report.continuity.get.binding.bindingReference.recordRevision.value shouldBe artifactrevision
              report.continuity.get.realization.elements.find(_.identity == "post-e-entity").get.label shouldBe label
              report.freshness.get.entries.filter(entry => Set("snapshot-cml-alpha", "snapshot-cml-beta").contains(entry.reference.artifactId.value))
                .map(_.result).foreach { case InternalModelPackageFreshnessResult.Compared(value) => value.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
                  case _ => fail("actual CML source comparison is required") }
              _sidecars(report.continuity.get)
            }
          }
        }
      }

      "missing facts and deterministic failure precedence" which {
        "retain missing indeterminate owner and required source-version facts as incomplete" in {
          Given("a complete application whose current command or independent owner facts are unavailable")
          _with_applied { scenario =>
            val head = scenario.request.commands.head
            val owner = scenario.refreshed.owner
            val missingmodel = owner.livesources(owner.livesources.keys.find(_.artifactId.value == "snapshot-model").get) match {
              case InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, _, bytes, path)) =>
                InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, None, bytes, path))
              case _ => fail("declared model observation required")
            }
            val variants = Vector(scenario.request.copy(commands = scenario.request.commands.tail),
              scenario.request.copy(commands = scenario.request.commands.updated(0, head.copy(outcome = CommandOutcome.Missing("runner not admitted")))),
              scenario.request.copy(commands = scenario.request.commands.updated(0, head.copy(outcome = CommandOutcome.Indeterminate("no terminal process result")))),
              scenario.request.copy(owner = None), scenario.request.copy(owner = Some(owner.copy(livesources = owner.livesources - _support.alphaSnapshot))),
              scenario.request.copy(owner = Some(owner.copy(livesources = owner.livesources.updated(_support.modelSnapshot, missingmodel)))),
              scenario.request.copy(owner = Some(owner.copy(targetbaselines = owner.targetbaselines.tail))))
            When("each well-formed incomplete request is evaluated")
            val reports = variants.map(validate(scenario.fixture.root, _).toOption.get)
            Then("every missing cause remains explicit and no successful continuity appears")
            reports.foreach { report => report.disposition shouldBe Disposition.Incomplete; report.problems should not be empty; report.continuity shouldBe None }
            reports.head.problems.map(_.kind) should contain(ProblemKind.CommandMissing)
            reports(2).problems.map(_.kind) should contain(ProblemKind.CommandIndeterminate)
            reports(5).freshness.get.entries.find(_.reference == _support.modelSnapshot).get.result match {
              case InternalModelPackageFreshnessResult.Compared(value) => value.missingDimensionNames should contain("observed.source.revision")
              case _ => fail("required source freshness evidence must survive")
            }
          }
        }

        "retain simultaneous nonzero FAIL malformed-output and missing causes with Failed precedence" in {
          Given("synthetic nonzero and FAIL output beside an indeterminate command and absent owner")
          _with_applied { scenario =>
            val first = scenario.request.commands.head.copy(outcome = CommandOutcome.Terminal(1, _finding_output("FAIL", "cml.domain.string-attribute"), "full native stderr"))
            val second = scenario.request.commands.last.copy(outcome = CommandOutcome.Indeterminate("terminal observation unavailable"))
            val request = scenario.request.copy(commands = Vector(first, second), owner = None)
            When("the closed post-validator observes failed and missing evidence together")
            val failed = validate(scenario.fixture.root, request).toOption.get
            val malformed = validate(scenario.fixture.root, request.copy(commands = Vector(first.copy(outcome = CommandOutcome.Terminal(0, "{", "raw")), second))).toOption.get
            Then("all raw inputs and simultaneous causes survive and failed evidence dominates incomplete evidence")
            failed.disposition shouldBe Disposition.Failed
            failed.commands shouldBe request.commands
            failed.owner shouldBe None
            failed.application shouldBe scenario.application
            failed.findings.head.code shouldBe "cml.domain.string-attribute"
            failed.problems.map(_.kind).toSet shouldBe Set(ProblemKind.CommandFailed, ProblemKind.CommandIndeterminate, ProblemKind.OwnerMissing)
            malformed.disposition shouldBe Disposition.Failed
            malformed.problems.map(_.kind).toSet shouldBe Set(ProblemKind.CommandOutputMalformed, ProblemKind.CommandIndeterminate, ProblemKind.OwnerMissing)
            failed.continuity shouldBe None
          }
        }

        "refuse exit-zero FAIL and malformed closed finding schemas without discarding observations" in {
          Given("synthetic terminal stdout with FAIL or invalid root findings level text line and duplicate fields")
          _with_applied { scenario =>
            val values = Vector(_finding_output("FAIL", "cml.domain.string-attribute"), "[]", "{\"findings\":[],\"extra\":true}",
              "{\"findings\":[],\"findings\":[]}", "{\"findings\":{}}", _finding_output("INFO", "code"),
              _finding_output("WARN", "code").replace("\"line\":3", "\"line\":0"),
              _finding_output("WARN", "code").replace("actual finding text", "   "),
              _finding_output("WARN", "code").replace("\"line\":3", "\"line\":1.5"))
            val requests = values.map(value => scenario.request.copy(commands = scenario.request.commands.updated(0,
              scenario.request.commands.head.copy(outcome = CommandOutcome.Terminal(0, value, "retained stderr")))))
            When("each actual JSON admission rule examines stdout rather than exit code alone")
            val reports = requests.map(validate(scenario.fixture.root, _).toOption.get)
            Then("all variants fail with complete terminal inputs and no successful continuity")
            reports.zip(requests).foreach { case (report, request) => report.disposition shouldBe Disposition.Failed; report.commands shouldBe request.commands; report.continuity shouldBe None }
            reports.head.problems.map(_.kind) should contain(ProblemKind.CommandFailed)
            reports.tail.foreach(_.problems.map(_.kind) should contain(ProblemKind.CommandOutputMalformed))
          }
        }
      }

      "exact command and owner bindings" which {
        "refuse wrong application root target path source version runtime argv duplicates and extras" in {
          Given("synthetic commands changing one exact attribution or target selection dimension")
          _with_applied { scenario =>
            val first = scenario.request.commands.head
            val wrong = Vector(first.copy(applicationreference = recordReference("other-application", 1L)),
              first.copy(commandreference = scenario.application.applicationreference), first.copy(projectroot = scenario.fixture.root.resolve("other").toString),
              first.copy(targetid = "unknown-target"), first.copy(projectrelativepath = "cml/other.cml"), first.copy(sourceauthority = "other-authority"),
              first.copy(sourceidentity = "other-identity"), first.copy(sourcerevision = "unapproved-next"), first.copy(runtimeversion = "0.3.3"),
              first.copy(argv = first.argv :+ "--extra"))
            val variants = wrong.map(value => scenario.request.copy(commands = scenario.request.commands.updated(0, value))) ++ Vector(
              scenario.request.copy(commands = scenario.request.commands :+ first), scenario.request.copy(commands = scenario.request.commands.updated(1,
                scenario.request.commands.last.copy(commandreference = first.commandreference))),
              scenario.request.copy(postvalidationreference = scenario.application.applicationreference))
            When("the declared exact command graph reaches post-validation")
            val reports = variants.map(validate(scenario.fixture.root, _).toOption.get)
            Then("every contradiction remains a closed failure preserving the complete caller inputs")
            reports.zip(variants).foreach { case (report, request) => report.disposition shouldBe Disposition.Failed
              report.problems.map(_.kind) should contain(ProblemKind.CommandBindingMismatch)
              report.commands shouldBe request.commands; report.application shouldBe scenario.application; report.continuity shouldBe None }
          }
        }

        "deny malformed opaque references Unicode containers and unsafe paths structurally" in {
          Given("a real complete application with malformed independently supplied post or command fields")
          _with_applied { scenario =>
            val command = scenario.request.commands.head
            val malformed = Vector(scenario.request.copy(postvalidationreference = null),
              scenario.request.copy(postvalidationreference = InternalModelRecordReference(null.asInstanceOf[InternalModelRecordId], _post_reference.recordRevision)),
              scenario.request.copy(postvalidationreference = InternalModelRecordReference(InternalModelRecordId.from("\ud800").toOption.get, _post_reference.recordRevision)),
              scenario.request.copy(postvalidationreference = InternalModelRecordReference(_post_reference.recordId, 0L.asInstanceOf[InternalModelRecordRevision])),
              scenario.request.copy(commands = null), scenario.request.copy(commands = Vector(null)),
              scenario.request.copy(commands = Vector(command.copy(argv = null))), scenario.request.copy(commands = Vector(command.copy(projectrelativepath = "../escape.cml"))),
              scenario.request.copy(owner = Some(scenario.refreshed.owner.copy(realizationartifactreference = InternalModelArtifactReference(
                null.asInstanceOf[InternalModelArtifactId], _support.realizationArtifact.artifactRevision, InternalModelArtifactRole.Realization)))))
            When("malformed graph admission runs before any rooted owner read")
            val results = malformed.map(validate(scenario.fixture.root, _))
            Then("every result is a structured operation failure with no ordinary report")
            results.foreach(_.isSuccess shouldBe false)
            results.foreach(_.toOption shouldBe None)
          }
        }

        "refuse old wrong-role duplicate unknown and mismatched independently selected owners" in {
          Given("owner evidence substituting old references other package or scope and unbound source keys")
          _with_applied { scenario =>
            val owner = scenario.refreshed.owner
            val plan = scenario.application.gate.plan.get
            val variants = Vector(owner.copy(realizationartifactreference = plan.realizationartifactreference), owner.copy(realizationreference = plan.realizationreference),
              owner.copy(continuityartifactreference = plan.continuityartifactreference), owner.copy(continuityreference = plan.continuityreference),
              owner.copy(realizationartifactreference = owner.realizationartifactreference.copy(role = InternalModelArtifactRole.Projection)),
              owner.copy(scope = owner.scope.copy(componentIdentity = "other-component")),
              owner.copy(packagereference = owner.packagereference.copy(projectId = InternalModelProjectToken.from("other-project").toOption.get)),
              owner.copy(targetbaselines = owner.targetbaselines.updated(0, owner.targetbaselines.head.copy(snapshotreference = plan.targets.head.target.baselineArtifactReference))),
              owner.copy(targetbaselines = owner.targetbaselines :+ owner.targetbaselines.head),
              owner.copy(targetbaselines = owner.targetbaselines :+ TargetBaseline("unknown-target", reference("snapshot-other", 1L, InternalModelArtifactRole.SourceSnapshot))),
              owner.copy(livesources = owner.livesources + (owner.realizationartifactreference -> owner.livesources(_support.modelSnapshot))),
              owner.copy(livesources = owner.livesources + (reference("snapshot-model", 41L, InternalModelArtifactRole.SourceSnapshot) -> owner.livesources(_support.modelSnapshot))))
            When("actual carrier selections are compared to independent owner inputs")
            val reports = variants.map(value => validate(scenario.fixture.root, scenario.request.copy(owner = Some(value))).toOption.get)
            Then("each mismatch is Failed and cannot become source freshness or projection permission")
            reports.foreach { report => report.disposition shouldBe Disposition.Failed
              report.problems.map(_.kind) should contain(ProblemKind.OwnerBindingMismatch); report.continuity shouldBe None }
          }
        }
      }

      "actual application and current native source evidence" which {
        "fail identity revision and authority drift of an optional consumed model" in {
          forAll(Gen.oneOf("source.identity", "source.revision", "source.authority")) { dimension =>
            Given("real applied targets and a present consumed model marked optional with one changed live source dimension")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-model"),
                _.mapObject(_.add("required", Json.False))))
              val owner = scenario.refreshed.owner
              val changed = owner.livesources(_support.modelSnapshot) match {
                case InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, revision, bytes, path)) =>
                  InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(
                    if (dimension == "source.authority") authority + "-changed" else authority,
                    if (dimension == "source.identity") identity + "-changed" else identity,
                    if (dimension == "source.revision") revision.map(_ + "-changed") else revision, bytes, path))
                case _ => fail("the independently supplied model observation must be present")
              }
              When("post-validation compares the optional consumed model against its independent current observation")
              val report = validate(scenario.fixture.root, scenario.request.copy(owner = Some(owner.copy(
                livesources = owner.livesources.updated(_support.modelSnapshot, changed))))).toOption.get
              Then("the changed source dimension fails with observable freshness and no successful continuity")
              report.disposition shouldBe Disposition.Failed
              report.problems.map(_.kind) should contain(ProblemKind.SourceNotFresh)
              report.continuity shouldBe None
              report.freshness.get.entries.find(_.reference == _support.modelSnapshot).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
                  value.changedDimensionNames shouldBe Vector(dimension)
                  value.missingDimensionNames shouldBe empty
                case _ => fail("the optional consumed model comparison must remain observable")
              }
            }
          }
        }

        "retain missing unavailable and revision-unknown optional consumed model evidence as incomplete" in {
          forAll(Gen.oneOf("missing", "unavailable", "revision-unknown")) { variant =>
            Given("real applied targets and a present optional consumed model with incomplete independent live evidence")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-model"),
                _.mapObject(_.add("required", Json.False))))
              val owner = scenario.refreshed.owner
              val unknown = owner.livesources(_support.modelSnapshot) match {
                case InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, _, bytes, path)) =>
                  InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, None, bytes, path))
                case _ => fail("the independently supplied model observation must be present")
              }
              val sources = variant match {
                case "missing" => owner.livesources - _support.modelSnapshot
                case "unavailable" => owner.livesources.updated(_support.modelSnapshot,
                  InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Unavailable("model owner unavailable")))
                case _ => owner.livesources.updated(_support.modelSnapshot, unknown)
              }
              When("post-validation evaluates the optional consumed model with the incomplete live evidence")
              val report = validate(scenario.fixture.root, scenario.request.copy(owner = Some(owner.copy(livesources = sources)))).toOption.get
              Then("missing evidence remains incomplete with an explicit owner problem and no successful continuity")
              report.disposition shouldBe Disposition.Incomplete
              report.problems.map(_.kind) should contain(ProblemKind.OwnerMissing)
              report.continuity shouldBe None
              report.freshness.get.entries.find(_.reference == _support.modelSnapshot).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.observedSourceRevision shouldBe None
                  if (variant == "revision-unknown") {
                    value.status shouldBe InternalModelSnapshotFreshnessStatus.Incomplete
                    value.missingDimensionNames shouldBe Vector("observed.source.revision")
                    value.changedDimensionNames shouldBe Vector("source.revision")
                  } else {
                    value.status shouldBe InternalModelSnapshotFreshnessStatus.Unavailable
                    value.observedSourceAuthority shouldBe None
                    value.observedSourceIdentity shouldBe None
                    value.reason.nonEmpty shouldBe true
                    value.missingDimensionNames shouldBe empty
                  }
                case _ => fail("the optional consumed model comparison must remain observable")
              }
            }
          }
        }

        "fail unauthorized malformed and ambiguous optional consumed model observations" in {
          forAll(Gen.oneOf("unauthorized", "malformed", "ambiguous")) { variant =>
            Given("real applied targets and a present optional consumed model with incompatible independent live evidence")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-model"),
                _.mapObject(_.add("required", Json.False))))
              val owner = scenario.refreshed.owner
              val (observation, status) = variant match {
                case "unauthorized" => (InternalModelLiveSourceObservation.Unauthorized("model owner denied access"), InternalModelSnapshotFreshnessStatus.Unauthorized)
                case "malformed" => (InternalModelLiveSourceObservation.Malformed("model observation malformed"), InternalModelSnapshotFreshnessStatus.Malformed)
                case _ => (InternalModelLiveSourceObservation.AmbiguousOrConflicting("model observations conflict"), InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting)
              }
              When("post-validation evaluates the incompatible optional consumed model observation")
              val report = validate(scenario.fixture.root, scenario.request.copy(owner = Some(owner.copy(livesources =
                owner.livesources.updated(_support.modelSnapshot, InternalModelPackageFreshnessInput.SourceObservation(observation)))))).toOption.get
              Then("the exact existing freshness status fails without successful continuity")
              report.disposition shouldBe Disposition.Failed
              report.problems.map(_.kind) should contain(ProblemKind.SourceNotFresh)
              report.continuity shouldBe None
              report.freshness.get.entries.find(_.reference == _support.modelSnapshot).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.status shouldBe status
                  value.reason.nonEmpty shouldBe true
                case _ => fail("the optional consumed model comparison must remain observable")
              }
            }
          }
        }

        "retain all eight projections for an unchanged optional or required consumed model" in {
          forAll(Gen.oneOf(false, true)) { required =>
            Given("real applied targets and unchanged independent live evidence for a present consumed model with either inventory flag")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-model"),
                _.mapObject(_.add("required", Json.fromBoolean(required)))))
              When("post-validation compares the consumed model with its unchanged independent live evidence")
              val report = validate(scenario.fixture.root, scenario.request).toOption.get
              Then("the shared realization rich sidecars and all eight projections remain pending acceptance")
              _pending(report, scenario)
              report.freshness.get.entries.find(_.reference == _support.modelSnapshot).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
                  value.missingDimensionNames shouldBe empty
                  value.changedDimensionNames shouldBe empty
                case _ => fail("the consumed model comparison must remain observable")
              }
            }
          }
        }

        "propagate the existing owner failure for an absent optional consumed model baseline" in {
          Given("real applied targets and an optional consumed model absent from disk with every dependency and source reference retained")
          _with_applied { scenario =>
            _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
              _.hcursor.get[String]("artifactId").toOption.contains("snapshot-model"),
              _.mapObject(_.add("required", Json.False))))
            Files.delete(_core.packageRoot(scenario.fixture.root).resolve("snapshots/model.json"))
            When("the existing continuation package owner and post-validator examine the absent consumed baseline")
            val expected = InternalModelPackageValidator.verifiedContinuation(scenario.fixture.root)
            val result = validate(scenario.fixture.root, scenario.request)
            Then("the same structured package-owner failure propagates without a fabricated report or continuity")
            expected.isSuccess shouldBe false
            result.isSuccess shouldBe false
            result.toOption shouldBe None
            result.show shouldBe expected.show
          }
        }

        "fail an optional selected changed-target baseline with a wrong source revision" in {
          forAll(Gen.oneOf("alpha", "beta")) { target =>
            Given("real applied targets and correct independent owner inputs with an optional selected baseline at a wrong revision")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-cml-" + target),
                _.mapObject(_.add("required", Json.False))))
              _edit(scenario.fixture.root, "snapshots/cml-" + target + ".json", value => value.mapObject(_.add("source",
                value.hcursor.downField("source").focus.get.mapObject(_.add("revision", Json.fromString("wrong-baseline-version"))))))
              When("post-validation compares the optional selected baseline against its approved next source revision")
              val report = validate(scenario.fixture.root, scenario.request).toOption.get
              Then("changed selected source evidence fails despite its optional inventory flag")
              report.disposition shouldBe Disposition.Failed
              report.problems.map(_.kind) should contain(ProblemKind.SourceNotFresh)
              report.continuity shouldBe None
              report.freshness.get.entries.find(_.reference.artifactId.value == "snapshot-cml-" + target).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.status shouldBe InternalModelSnapshotFreshnessStatus.Changed
                  value.changedDimensionNames should contain("source.revision")
                case _ => fail("the selected baseline source comparison must remain observable")
              }
            }
          }
        }

        "fail an optional selected changed-target baseline with a missing source revision" in {
          forAll(Gen.oneOf("alpha", "beta")) { target =>
            Given("real applied targets and correct independent owner inputs with an optional selected baseline lacking its revision")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-cml-" + target),
                _.mapObject(_.add("required", Json.False))))
              _edit(scenario.fixture.root, "snapshots/cml-" + target + ".json", value => value.mapObject(_.add("source",
                value.hcursor.downField("source").focus.get.mapObject(_.add("revision", Json.Null)))))
              When("post-validation compares the revisionless optional selected baseline against the approved next revision")
              val report = validate(scenario.fixture.root, scenario.request).toOption.get
              Then("missing baseline evidence and changed revision evidence both survive the Failed disposition")
              report.disposition shouldBe Disposition.Failed
              report.problems.map(_.kind) should contain(ProblemKind.OwnerMissing)
              report.problems.map(_.kind) should contain(ProblemKind.SourceNotFresh)
              report.continuity shouldBe None
              report.freshness.get.entries.find(_.reference.artifactId.value == "snapshot-cml-" + target).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.status shouldBe InternalModelSnapshotFreshnessStatus.Incomplete
                  value.missingDimensionNames should contain("baseline.source.revision")
                  value.changedDimensionNames should contain("source.revision")
                case _ => fail("the selected baseline source comparison must remain observable")
              }
            }
          }
        }

        "retain an absent optional selected changed-target baseline as incomplete" in {
          forAll(Gen.oneOf("alpha", "beta")) { target =>
            Given("real applied targets and correct independent owner inputs with an optional selected baseline absent from disk")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-cml-" + target),
                _.mapObject(_.add("required", Json.False))))
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts", _ => true,
                artifact => artifact.mapObject(_.add("dependsOn", Json.fromValues(
                  artifact.hcursor.get[Vector[Json]]("dependsOn").toOption.get.filterNot(
                    _.hcursor.get[String]("artifactId").toOption.contains("snapshot-cml-" + target)))))))
              Files.delete(_core.packageRoot(scenario.fixture.root).resolve("snapshots/cml-" + target + ".json"))
              When("post-validation evaluates the absent optional selected baseline")
              val report = validate(scenario.fixture.root, scenario.request).toOption.get
              Then("selected baseline absence remains mandatory missing evidence without successful continuity")
              report.disposition shouldBe Disposition.Incomplete
              report.problems.map(_.kind) should contain(ProblemKind.OwnerMissing)
              report.problems.map(_.dimension) should contain("freshness.baseline")
              report.continuity shouldBe None
              report.freshness.get.entries.find(_.reference.artifactId.value == "snapshot-cml-" + target).get.result shouldBe
                InternalModelPackageFreshnessResult.MissingBaseline
            }
          }
        }

        "retain all eight projections for a fresh optional selected changed-target baseline" in {
          forAll(Gen.oneOf("alpha", "beta")) { target =>
            Given("real applied targets and correct independent owner inputs with an unchanged optional selected baseline")
            _with_applied { scenario =>
              _edit(scenario.fixture.root, "manifest.yaml", value => _array_change(value, "artifacts",
                _.hcursor.get[String]("artifactId").toOption.contains("snapshot-cml-" + target),
                _.mapObject(_.add("required", Json.False))))
              When("post-validation compares the optional selected baseline to its approved next source revision")
              val report = validate(scenario.fixture.root, scenario.request).toOption.get
              Then("unchanged selected evidence retains the complete all-eight pending-acceptance result")
              _pending(report, scenario)
              report.freshness.get.entries.find(_.reference.artifactId.value == "snapshot-cml-" + target).get.result match {
                case InternalModelPackageFreshnessResult.Compared(value) =>
                  value.status shouldBe InternalModelSnapshotFreshnessStatus.Unchanged
                  value.missingDimensionNames shouldBe empty
                case _ => fail("the selected baseline source comparison must remain observable")
              }
            }
          }
        }

        "prevent rejected partial and relabeled incomplete native reports from progressing" in {
          Given("real rooted rejected and partial applications and an attempted complete relabel")
          _support.withFixture() { fixture =>
            val rejected = InternalModelCmlChangeApplication.applyApproved(fixture.root,
              fixture.request.copy(currentrequest = fixture.request.currentrequest.copy(humandecision = None)), _application_reference).toOption.get
            val operations = new NativeCmlFileWriter.WriteOperations {
              private var _flush_count = 0
              override def truncate(fd: Int): Either[NativeCmlFileWriter.Failure, Unit] = NativeCmlFileWriter.nativeOperations.truncate(fd)
              override def write(fd: Int, bytes: Vector[Byte]): Either[NativeCmlFileWriter.Failure, Long] = NativeCmlFileWriter.nativeOperations.write(fd, bytes)
              override def flush(fd: Int): Either[NativeCmlFileWriter.Failure, Unit] = {
                _flush_count += 1
                if (_flush_count == 2) Left(NativeCmlFileWriter.Failure(NativeCmlFileWriter.FailureKind.IoFailure, Some(5), "explicit unit flush failure"))
                else NativeCmlFileWriter.nativeOperations.flush(fd)
              }
            }
            val partial = InternalModelCmlChangeApplication.applyApprovedWithOperations(fixture.root, fixture.request, _application_reference, operations).toOption.get
            val relabeled = partial.copy(disposition = InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation)
            When("post-validation receives these actual outcomes or the contradictory complete label")
            val reports = Vector(rejected, partial, relabeled).map(application => validate(fixture.root,
              Request(application, _post_reference, Vector.empty, None)).toOption.get)
            Then("actual native failures and incomplete targets cannot become a successful post-validation")
            rejected.disposition shouldBe InternalModelCmlChangeApplication.Disposition.Rejected
            partial.disposition shouldBe InternalModelCmlChangeApplication.Disposition.Failed
            partial.writes.get.outcomes.map(_.disposition) shouldBe Vector(NativeCmlFileWriter.Disposition.Applied, NativeCmlFileWriter.Disposition.Failed)
            reports.foreach { report => report.disposition shouldBe Disposition.Failed; report.problems.map(_.kind) should contain(ProblemKind.ApplicationNotComplete); report.continuity shouldBe None }
          }
        }

        "refuse an old actual carrier despite claimed refreshed owner selectors" in {
          Given("actual B application with its original unchanged carrier and explicit new owner selectors")
          _support.withFixture() { fixture =>
            val application = InternalModelCmlChangeApplication.applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
            val owner = OwnerEvidence(application.gate.plan.get.packagereference, application.gate.plan.get.scope,
              _support.realizationArtifact, _support.realizationRecord, _support.continuityArtifact, _support.continuityRecord,
              Vector(TargetBaseline("target-alpha", _support.alphaSnapshot), TargetBaseline("target-beta", _support.betaSnapshot)), Map.empty)
            When("rooted reload observes old source realization and binding references")
            val report = validate(fixture.root, Request(application, _post_reference, _support.syntheticCommands(fixture.root, application), Some(owner))).toOption.get
            Then("owner claims cannot replace actual refreshed carrier evidence")
            report.disposition shouldBe Disposition.Failed
            report.problems.map(_.dimension) should contain("current.realizationartifactreference")
            report.problems.map(_.dimension) should contain("current.continuityartifactreference")
            report.continuity shouldBe None
          }
        }

        "retain drift unknown next versions and raw-byte CML substitution as explicit denials" in {
          Given("independent source observations with changed model identity or missing/wrong CML revision or byte substitutes")
          _with_applied { scenario =>
            val owner = scenario.refreshed.owner
            val alpha = InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", None, "cml/alpha.cml")
            val drift = InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed("model-authority", "different-model", None, Vector.empty, None))
            val substitute = InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed("cml-authority", "cml-alpha", Some("approved-next-alpha"), Vector[Byte](1, 2), Some("cml/alpha.cml")))
            val variants = Vector(owner.copy(livesources = owner.livesources.updated(_support.alphaSnapshot, alpha)),
              owner.copy(livesources = owner.livesources.updated(_support.alphaSnapshot, InternalModelPackageFreshnessInput.CmlObserved("cml-authority", "cml-alpha", Some("unknown-next"), "cml/alpha.cml"))),
              owner.copy(livesources = owner.livesources.updated(_support.modelSnapshot, drift)),
              owner.copy(livesources = owner.livesources.updated(_support.alphaSnapshot, substitute)))
            When("freshness reads current files and compares declared owner versions and source dimensions")
            val reports = variants.map(value => validate(scenario.fixture.root, scenario.request.copy(owner = Some(value))).toOption.get)
            Then("unknown evidence stays incomplete while incompatible evidence fails and retains missing dimensions")
            reports.map(_.disposition) shouldBe Vector(Disposition.Incomplete, Disposition.Failed, Disposition.Failed, Disposition.Failed)
            reports.foreach(_.continuity shouldBe None)
            reports(2).problems.map(_.kind) should contain(ProblemKind.SourceNotFresh)
            reports(2).problems.map(_.kind) should contain(ProblemKind.OwnerMissing)
            reports(3).problems.map(_.dimension) should contain("owner.livecml")
          }
        }

        "retain unavailable actual CML reads and optional missing baselines without inventing observations" in {
          Given("an optional unmaterialized snapshot and a later missing actual applied CML target")
          _with_applied({ scenario =>
            When("the existing owner evaluates optional absence and then a missing actual canonical file")
            val optional = validate(scenario.fixture.root, scenario.request).toOption.get
            Files.delete(scenario.fixture.root.resolve("cml/alpha.cml"))
            val missing = validate(scenario.fixture.root, scenario.request).toOption.get
            Then("optional MissingBaseline survives successful rehydration but a required unavailable native read cannot progress")
            optional.disposition shouldBe Disposition.ReprojectedPendingAcceptance
            optional.freshness.get.entries.find(_.reference.artifactId.value == "snapshot-optional").get.result shouldBe InternalModelPackageFreshnessResult.MissingBaseline
            missing.disposition shouldBe Disposition.Incomplete
            missing.problems.map(_.kind) should contain(ProblemKind.OwnerMissing)
            missing.continuity shouldBe None
            missing.freshness.get.entries.find(_.reference == _support.alphaSnapshot).get.result match {
              case InternalModelPackageFreshnessResult.Compared(value) => value.status shouldBe InternalModelSnapshotFreshnessStatus.Unavailable
              case _ => fail("the actual native read outcome must remain observable")
            }
          }, options = InternalModelDurableHandoffFixture.approvedOptions.copy(includeoptionalsnapshot = true))
        }
      }

      "existing semantic owner failures and mapping retention" which {
        "propagate malformed current package realization binding missing anchors and unsupported projection witnesses" in {
          Given("a current rooted carrier with one malformed semantic owner artifact or unsupported direct witness")
          val mutations: Vector[(String, Json => Json)] = Vector(
            "manifest.yaml" -> (value => value.mapObject(_.add("unexpected", Json.True))),
            "realizations/main.json" -> (value => value.mapObject(_.add("profile", Json.fromString("unsupported")))),
            "projections/continuity.json" -> (value => value.mapObject(_.add("profile", Json.fromString("unsupported")))),
            "realizations/main.json" -> (value => _array_change(value, "sourceReferences", _.hcursor.get[String]("referenceId").toOption.contains("ref-opaque-element-kind-Mono"),
              _.mapObject(_.add("sourceAnchor", Json.fromString("missing-source-anchor"))))),
            "projections/continuity.json" -> (value => _array_change(value, "views", _.hcursor.get[String]("family").toOption.contains("EntityModelProjection"),
              view => _array_change(view, "records", _ => true, _.mapObject(_.add("viewRole", Json.fromString("UnsupportedEntityWitness")))))))
          mutations.foreach { case (path, mutation) => _with_applied { scenario =>
            _edit(scenario.fixture.root, path, mutation)
            When("the real selected package or projection owner encounters the malformed artifact")
            val expected = if (path == "manifest.yaml") InternalModelPackageValidator.verifiedContinuation(scenario.fixture.root)
              else InternalModelPackageValidator.verifiedContinuation(scenario.fixture.root).flatMap(handoff =>
                InternalModelProjectionContinuityValidator.validateVerified(handoff.continuityPackage))
            val result = validate(scenario.fixture.root, scenario.request)
            Then("the owner's structured failure propagates without a fabricated report")
            expected.isSuccess shouldBe false
            result.isSuccess shouldBe false
            result.toOption shouldBe None
            result.show shouldBe expected.show
          }}
        }

        "propagate a malformed source snapshot through its existing semantic owner" in {
          Given("a real applied package whose current declared model snapshot has malformed source data")
          _with_applied { scenario =>
            _edit(scenario.fixture.root, "snapshots/model.json", _.mapObject(_.add("snapshotKind", Json.fromString("unsupported-source"))))
            When("the real source-backed semantic owner examines the malformed actual baseline")
            val expected = InternalModelProjectionContinuityValidator.validate(scenario.fixture.root)
            val result = validate(scenario.fixture.root, scenario.request)
            Then("the complete structured source-owner failure propagates without a fabricated report")
            expected.isSuccess shouldBe false
            result.isSuccess shouldBe false
            result.toOption shouldBe None
            result.show shouldBe expected.show
          }
        }

        "refuse semantically valid removed mapped links swapped lanes and relabeled condition detail" in {
          Given("current source-backed graphs that remain valid but remove mapping coverage or change its semantic lane or conditions")
          val variants = Vector("removed-link", "swapped-lane", "changed-condition")
          variants.foreach { variant => _with_applied { scenario =>
            val captured = InternalModelProjectionContinuityValidator.validate(scenario.fixture.root).toOption.get
            val realization = captured.realization
            val changed = variant match {
              case "removed-link" => realization.copy(elements = realization.elements.filterNot(_.identity == "opaque-shared"),
                canonicalAssertions = realization.canonicalAssertions.filterNot(_.assertionId == "a-opaque-element-kind-Mono"),
                enrichmentAssertions = realization.enrichmentAssertions.filterNot(_.assertionId == "z-opaque-element-enrichment"),
                conditions = realization.conditions.filterNot(_.conditionId == "c-opaque-element"),
                sourceReferences = realization.sourceReferences.filterNot(_.target.contains(InternalModelSemanticTarget("element", "opaque-shared"))))
              case "swapped-lane" =>
                val moved = realization.enrichmentAssertions.find(_.assertionId == "z-opaque-element-enrichment").get
                realization.copy(canonicalAssertions = (realization.canonicalAssertions :+ moved).sortBy(_.assertionId),
                  enrichmentAssertions = realization.enrichmentAssertions.filterNot(_.assertionId == moved.assertionId),
                  elements = realization.elements.map(value => if (value.identity == "opaque-shared") value.copy(
                    canonicalAssertionIds = (value.canonicalAssertionIds :+ moved.assertionId).sorted, enrichmentAssertionIds = Vector.empty) else value))
              case _ => realization.copy(conditions = realization.conditions.map(value =>
                if (value.conditionId == "c-opaque-element") value.copy(detail = "different owner-admitted limitation") else value))
            }
            _core.replaceArtifact(scenario.fixture.root, "realization-main", InternalModelSemanticRealizationEncoder.encode(changed).toVector)
            When("post-validation compares mapped semantics after the existing owners admit the changed graph")
            val ownerresult = InternalModelProjectionContinuityValidator.validate(scenario.fixture.root)
            val report = validate(scenario.fixture.root, scenario.request).toOption.get
            Then("valid rehydration alone cannot erase mapped identities lanes or full condition meanings")
            ownerresult.isSuccess shouldBe true
            report.disposition shouldBe Disposition.Failed
            report.problems.map(_.kind) should contain(ProblemKind.MappingNotRetained)
            report.problems.map(_.targetid) should contain(Some("target-alpha"))
            report.continuity shouldBe None
          }}
        }
      }
    }
  }

  private def _with_applied(body: Scenario => Unit,
    options: InternalModelCandidateHumanApprovalValidatorSpec.FixtureOptions = InternalModelDurableHandoffFixture.approvedOptions): Unit =
    _support.withFixture(options = options.copy(includeoptionalsnapshot = false)) { fixture =>
      val application = InternalModelCmlChangeApplication.applyApproved(fixture.root, fixture.request, _application_reference).toOption.get
      val refreshed = _support.refreshOwner(fixture, application, includeOptionalSnapshot = options.includeoptionalsnapshot)
      body(Scenario(fixture, application, refreshed, Request(application, _post_reference,
        _support.syntheticCommands(fixture.root, application), Some(refreshed.owner))))
    }

  private def _pending(report: Report, scenario: Scenario): Unit = {
    report.disposition shouldBe Disposition.ReprojectedPendingAcceptance
    report.application shouldBe scenario.application
    report.commands shouldBe scenario.request.commands
    report.owner shouldBe Some(scenario.refreshed.owner)
    report.postvalidationreference shouldBe _post_reference
    report.findings shouldBe empty
    report.problems shouldBe empty
    _support.requiredUnchanged(report) shouldBe true
    val continuity = report.continuity.get
    continuity.realization.realizationReference shouldBe scenario.refreshed.owner.realizationreference
    continuity.binding.bindingReference shouldBe scenario.refreshed.owner.continuityreference
    continuity.realization.consumedSnapshotReferences shouldBe Vector(_support.modelSnapshot)
    continuity.binding.realizationArtifactReference shouldBe _support.realizationArtifact
    continuity.realization.scope shouldBe scenario.refreshed.owner.scope
    continuity.binding.views.map(_.family) shouldBe _support.families
    _support.projectionCounts(continuity).size shouldBe 8
    _support.projectionCounts(continuity).forall(_ > 0) shouldBe true
    _sidecars(continuity)
  }

  private def _sidecars(continuity: InternalModelProjectionContinuity): Unit = {
    val realization = continuity.realization
    val element = realization.elements.find(_.identity == "opaque-shared").get
    val relationship = realization.relationships.find(_.identity == "opaque-shared").get
    element.canonicalAssertionIds shouldBe Vector("a-opaque-element-kind-Mono")
    relationship.canonicalAssertionIds shouldBe Vector("a-opaque-relationship-role-StructuralDomain")
    element.enrichmentAssertionIds shouldBe Vector("z-opaque-element-enrichment")
    relationship.enrichmentAssertionIds shouldBe Vector("z-opaque-relationship-enrichment")
    element.conditionIds shouldBe Vector("c-opaque-element")
    relationship.conditionIds shouldBe Vector("c-opaque-relationship")
    realization.conditions.find(_.conditionId == "c-opaque-element").get.detail shouldBe "element evidence remains bounded"
    realization.conditions.find(_.conditionId == "c-opaque-relationship").get.detail shouldBe "relationship evidence remains bounded"
    realization.sourceReferences.filter(_.target.exists(_.semanticIdentity == "opaque-shared")).foreach { source =>
      source.snapshotReference.artifactId.value shouldBe "snapshot-model"
      source.source.authority shouldBe "model-authority"
      source.source.identity shouldBe "model-source"
      source.source.locator shouldBe Some("catalog/model-source")
      source.sourceAnchor should startWith("anchor-opaque-")
    }
    realization.canonicalAssertions.find(_.assertionId == "post-a-r-structure-assertion-association-owner-relationship-r-structure").get.association shouldBe
      Some(InternalModelSemanticAssociation("owner", "post-r-structure", "relationship"))
    continuity.workflow.flows.head.sourceFlow.sourceOwnedSequenceKey.value shouldBe "flow-1"
    continuity.workflow.flows.head.sourceFlow.condition.conflict shouldBe Some("recorded conflict")
    continuity.useCaseCommunication.subjects.head.flows.head.steps.head.sourceFlowStep.sequenceKey shouldBe "step-1"
    continuity.entityModel.subjects.head.navigationTarget shouldBe None
    continuity.entityModel.subjects.head.sourceSubject.condition.redaction shouldBe None
    continuity.entityModel.subjects.head.sourceSubject.condition.explicitAbsence shouldBe None
    realization.conditions.find(_.conditionId == "post-c-workflow-conflict").get.affectedIdentity shouldBe "post-r-workflow-flow"
  }

  private def _finding_output(level: String, code: String): String = Json.obj("findings" -> Json.arr(Json.obj(
    "level" -> Json.fromString(level), "code" -> Json.fromString(code), "message" -> Json.fromString("actual finding text"),
    "path" -> Json.fromString("cml/alpha.cml"), "line" -> Json.fromInt(3)))).noSpaces
  private def _edit(root: Path, relativepath: String, change: Json => Json): Unit = {
    val path = _core.packageRoot(root).resolve(relativepath)
    _core.write(path, _core.canonical(change(_core.json(Files.readAllBytes(path).toVector))))
  }
  private def _array_change(value: Json, field: String, selected: Json => Boolean, change: Json => Json): Json =
    value.mapObject(_.add(field, Json.fromValues(value.hcursor.get[Vector[Json]](field).toOption.get.map(item => if (selected(item)) change(item) else item))))
}
