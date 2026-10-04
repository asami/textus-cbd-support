package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, LinkOption, Path}
import java.util.concurrent.{CompletableFuture, Executors, TimeUnit, TimeoutException}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import io.circe.Json
import org.goldenport.Consequence

/**
 * Uninstalled test-only terminal producer and fresh consuming application adapter.
 *
 * @since   Oct.  4, 2026
 * @version Oct.  4, 2026
 */
object InternalModelEndToEndProbe {
  import InternalModelCmlChangePostValidation.*
  import InternalModelCandidateHumanApprovalValidatorSpec.recordReference
  private val _support = InternalModelEndToEndSupport
  private val _fixture = InternalModelEndToEndFixture
  private val _acceptance = InternalModelCmlChangeAcceptanceSupport
  private val _command_references = Vector(recordReference("cozy-alpha", 613L), recordReference("cozy-beta", 617L))

  def main(args: Array[String]): Unit = {
    val pid = ProcessHandle.current().pid()
    val result = try {
      require((args.length == 6 && args(0) == "producer") || (args.length == 5 && args(0) == "consumer"),
        "probe needs producer <run> <label> <reverse> <carrier> <success|lint-failure> or consumer <run> <root> <caller> <mode>")
      val work = Path.of(args(1))
      require(work.isAbsolute && work.normalize == work && Files.isDirectory(work), "run must be an existing normalized absolute directory")
      require(Path.of("").toAbsolutePath.normalize.startsWith(work), "fixture cwd must remain inside the admitted run")
      require(Path.of(System.getProperty("user.home")).startsWith(work) && Path.of(System.getProperty("java.io.tmpdir")).startsWith(work),
        "isolated home and tmp must remain inside the run")
      val report = args(0) match {
        case "producer" =>
          val produced = _fixture.produce(work, args(2), args(3).toBoolean, args(4).toLong, args(5))
          val envelope = _set(produced, "pid", Json.fromLong(pid))
          _support.writeJson(work.resolve("producer.json"), envelope)
          envelope
        case "consumer" => _consume(work, Path.of(args(2)), Path.of(args(3)), args(4), pid)
        case _ => throw new IllegalArgumentException("unknown probe mode")
      }
      (report, 0)
    } catch {
      case NonFatal(error) => (Json.obj("pid" -> Json.fromLong(pid), "status" -> Json.fromString("harness-error"),
        "errorType" -> Json.fromString(error.getClass.getName), "diagnostic" -> Json.fromString(Option(error.getMessage).getOrElse("no diagnostic"))), 3)
    }
    System.out.write(InternalModelFreshProcessProbe.canonicalBytes(result._1).toArray)
    System.out.flush()
    System.exit(result._2)
  }

  private def _consume(work: Path, root: Path, inputpath: Path, mode: String, pid: Long): Json = {
    require(Set("inspect", "synthetic-success", "synthetic-failure", "synthetic-missing", "external").contains(mode), "consumer observation mode must be explicit")
    InternalModelDurableHandoffSupport.requireOwned(work, root)
    InternalModelDurableHandoffSupport.requireOwned(work, inputpath)
    require(!inputpath.startsWith(root), "caller input cannot be a package/project artifact")
    val input = _support.readInput(inputpath)
    // No producer fixture factory recreates this package or project in the consumer.
    val fixture = _fixture.load(root, input)
    val targets = fixture.request.currentreview.semanticDiffAdmission.candidateAdmission.projection.targets
    val sourcebytes = targets.map(target => Files.readAllBytes(root.resolve(target.projectRelativePath)).toVector)
    val cursorpath = root.resolve("src/main/internal-model/resume.yaml")
    val cursorbytes = Files.readAllBytes(cursorpath).toVector
    val before = InternalModelContinuationActionGate.evaluate(root, input.original).toOption.get
    val beforeevidence = InternalModelFreshProcessProbe.actionReport(before)
    val producer = _support.readJson(work.resolve("producer.json"))
    val sourcesunchanged = targets.zip(sourcebytes).forall { case (target, bytes) =>
      Files.readAllBytes(root.resolve(target.projectRelativePath)).toVector == bytes
    }
    val initial = Json.obj("pid" -> Json.fromLong(pid), "producerPid" -> producer.hcursor.get[Json]("pid").toOption.get,
      "mode" -> Json.fromString(mode), "before" -> beforeevidence,
      "selection" -> _fixture.selection(before),
      "phase9" -> _fixture.phase9Evidence(fixture.request.currentreview),
      "beforeCounts" -> Json.fromValues(_acceptance.projectionCounts(before.state.continuity).map(Json.fromInt)),
      "beforeSidecars" -> Json.fromBoolean(_fixture.retained(before.state.continuity)),
      "preApplicationSourcesUnchanged" -> Json.fromBoolean(sourcesunchanged),
      "environmentKeys" -> Json.fromValues(System.getenv().keySet().asScala.toVector.sorted.map(Json.fromString)))
    // Complete result equality is asserted by the specification, never used as write permission.
    if (mode == "inspect") {
      _set(_set(initial, "status", Json.fromString("Inspected")), "cursorUnchanged",
        Json.fromBoolean(Files.readAllBytes(cursorpath).toVector == cursorbytes))
    } else {
      val applicationresult = InternalModelCmlChangeApplication.applyApproved(root, fixture.request, input.application)
      applicationresult match {
        case Consequence.Failure(conclusion) => _set(_set(_set(initial, "status", Json.fromString("AdmissionRejected")),
          "diagnostic", Json.fromString(applicationresult.show)), "sourcesUnchanged", Json.fromBoolean(_unchanged(root, targets, sourcebytes)))
        case Consequence.Success(application) => {
        val applied = _set(initial, "applicationReport", _support.evidence(application))
        if (application.disposition != InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation) {
          _set(_set(_set(applied, "status", Json.fromString(application.disposition.toString)),
            "gateEligibility", Json.fromString(application.gate.eligibility.toString)), "sourcesUnchanged",
            Json.fromBoolean(_unchanged(root, targets, sourcebytes)))
        } else {
          val commands = _commands(work, root, inputpath, mode, pid, application)
          val physicaleffects = application.gate.plan.get.targets.forall(target =>
            Files.readAllBytes(root.resolve(target.target.projectRelativePath)).toVector == target.payload.proposedRawBytes)
          val refreshed = _fixture.refresh(fixture, application, input.label)
          val report = InternalModelCmlChangePostValidation.validate(root,
            Request(application, input.postvalidation, commands, Some(refreshed.owner))).toOption.get
          val result = _set(_set(_set(_set(_set(_set(_set(applied, "status", Json.fromString(report.disposition.toString)),
            "postValidationReport", _support.evidence(report)), "physicalEffectsRetained", Json.fromBoolean(physicaleffects)),
            "cursorUnchanged", Json.fromBoolean(Files.readAllBytes(cursorpath).toVector == cursorbytes)),
            "counts", Json.fromValues(report.continuity.map(_acceptance.projectionCounts).getOrElse(Vector.empty).map(Json.fromInt))),
            "sidecars", Json.fromBoolean(report.continuity.exists(_fixture.retained))),
            "requiredSourcesUnchanged", Json.fromBoolean(_acceptance.requiredUnchanged(report)))
          _support.writeJson(work.resolve("consumer-result.json"), result)
          result
        }
        }
      }
    }
  }

  private def _commands(work: Path, root: Path, inputpath: Path, mode: String, pid: Long,
    application: InternalModelCmlChangeApplication.ApplicationReport): Vector[CommandObservation] = {
    val templates = application.gate.plan.get.targets.zip(_command_references).map { case (target, reference) =>
      CommandObservation(reference, application.applicationreference, root.toString, target.target.targetId,
        target.target.projectRelativePath, target.target.source.authority, target.target.source.identity,
        target.nextsourcerevision, "0.3.3-SNAPSHOT", Vector("--runtime", "0.3.3-SNAPSHOT", "lint", "cml",
          root.resolve(target.target.projectRelativePath).normalize.toString, "--format", "json"),
        CommandOutcome.Missing("no independently admitted terminal observation"))
    }
    if (mode == "external") {
      val paths = Vector(work.resolve("alpha-cozy-observation.json"), work.resolve("beta-cozy-observation.json"))
      val ready = Json.obj("schema" -> Json.fromString("textus.internal-model-e2e-ready.v1"),
        "consumerPid" -> Json.fromLong(pid), "projectRoot" -> Json.fromString(root.toString), "inputPath" -> Json.fromString(inputpath.toString),
        "applicationReference" -> _support.recordJson(application.applicationreference), "waitCeilingSeconds" -> Json.fromInt(1800),
        "targets" -> Json.fromValues(templates.zip(paths).map { case (command, path) => Json.obj(
          "targetId" -> Json.fromString(command.targetid), "projectRelativePath" -> Json.fromString(command.projectrelativepath),
          "absolutePath" -> Json.fromString(root.resolve(command.projectrelativepath).toString),
          "sourceAuthority" -> Json.fromString(command.sourceauthority), "sourceIdentity" -> Json.fromString(command.sourceidentity),
          "sourceRevision" -> Json.fromString(command.sourcerevision), "commandReference" -> _support.recordJson(command.commandreference),
          "runtimeVersion" -> Json.fromString(command.runtimeversion), "argv" -> Json.fromValues(command.argv.map(Json.fromString)),
          "observationPath" -> Json.fromString(path.toString)) }))
      _support.writeJson(work.resolve("ready.json"), ready)
      _await_observations_blocking(paths)
      paths.zip(templates).map { case (path, template) =>
        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
          _acceptance.readObservation(path).fold(reason => throw new IllegalArgumentException(reason), identity)
        } else template
      }
    } else {
      templates.zipWithIndex.map { case (command, index) =>
        val outcome = if (mode == "synthetic-missing" && index == 0) CommandOutcome.Missing("synthetic missing unit observation")
        else if (mode == "synthetic-failure" && index == 0) CommandOutcome.Terminal(1,
          "{\"findings\":[{\"level\":\"FAIL\",\"code\":\"e2e.synthetic-failure\",\"message\":\"unit failure\",\"path\":\"cml/alpha.cml\",\"line\":1}]}",
          "synthetic unit observation")
        else CommandOutcome.Terminal(0, "{\"findings\":[]}", "synthetic unit observation")
        command.copy(outcome = outcome)
      }
    }
  }

  /** Session-owned coordination only; no Cozy/SBT/Git command is executed here. */
  private def _await_observations_blocking(paths: Vector[Path]): Unit = {
    val ready = new CompletableFuture[Unit]()
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val polling = scheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        if (paths.forall(path => Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))) ready.complete(())
        ()
      }
    }, 0L, 100L, TimeUnit.MILLISECONDS)
    try {
      ready.get(1800L, TimeUnit.SECONDS)
      ()
    } catch {
      case _: TimeoutException => () // Missing observations remain Missing values after the explicit ceiling.
    } finally {
      polling.cancel(false)
      scheduler.shutdown()
    }
  }

  private def _unchanged(root: Path, targets: Vector[InternalModelCandidateCmlTarget], bytes: Vector[Vector[Byte]]): Boolean =
    targets.zip(bytes).forall { case (target, baseline) => Files.readAllBytes(root.resolve(target.projectRelativePath)).toVector == baseline }
  private def _set(value: Json, key: String, item: Json): Json = value.mapObject(_.add(key, item))
}
