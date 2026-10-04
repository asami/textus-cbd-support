package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import io.circe.Json
import io.circe.parser.parse
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/** Controlled reader fixtures, not actual Cozy execution or AI evidence.
 * @since   Oct.  4, 2026
 * @version Oct.  4, 2026
 */
final class InternalModelEndToEndReadinessSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  "Internal-model readiness acquisition" should {
    "work-artifact placement" which {
      "accept valid readiness without creating wrapper bytecode in the source tree" in {
        Given("the existing valid readiness fixture and the actual repository wrapper import")
        val mode = "valid-decoded"
        When("the controlled probe imports the wrapper and acquires valid readiness")
        val report = _probe(mode)
        Then("readiness remains accepted and the actual wrapper bytecode artifact is absent")
        _accepted(report, 0, 1)
        val bytecodepath = report.hcursor.get[String]("bytecodePath").toOption.get
        withClue(s"wrapper bytecode artifact: $bytecodepath") {
          report.hcursor.get[Boolean]("bytecodeExists").toOption.get shouldBe false
        }
      }
    }
    "in-progress publication" which {
      "retain the exact complete object across at least twelve generated partial byte offsets" in {
        Given("a complete Unicode readiness declaration and generated incomplete byte prefixes")
        val offsets = Gen.choose(1, 4096)
        forAll(offsets, minSuccessful(12)) { cut =>
          When("the controlled publisher completes the file on the next readiness poll")
          val report = _probe("delayed-property", cut)
          Then("acquisition returns the exact declaration and checks it without reading readiness again")
          _accepted(report, 1, 2)
          report.hcursor.get[Int]("cut").toOption.get should be > 0
          report.hcursor.get[Int]("cut").toOption.get should be < report.hcursor.get[Int]("byteLength").toOption.get
        }
      }
      "accept explicitly empty, JSON-partial and split-UTF8 publication once each completes" in {
        Given("three explicitly selected incomplete publication boundaries")
        val modes = Vector("delayed-empty", "delayed-json", "delayed-utf8")
        When("each boundary completes during one controlled polling interval")
        val reports = modes.map(mode => _probe(mode))
        Then("all three return the complete Unicode object through the existing semantic checks")
        reports.foreach(report => _accepted(report, 1, 2))
        reports.last.hcursor.get[String]("initialError").toOption.get shouldBe "UnicodeDecodeError"
        reports.take(2).foreach(_.hcursor.get[String]("initialError").toOption.get shouldBe "JSONDecodeError")
      }
      "expire permanently partial, malformed and missing readiness at the same deadline" in {
        Given("a live consumer with three permanently unavailable complete readiness declarations")
        val modes = Vector("timeout-partial", "timeout-malformed", "timeout-missing")
        When("controlled polling reaches the existing caller-supplied ceiling")
        val reports = modes.map(mode => _probe(mode))
        Then("every case fails with the explicit ceiling diagnostic after three bounded polling intervals")
        reports.foreach { report =>
          _rejected(report, "ValueError", "consumer exceeded wrapper ceiling before readiness")
          report.hcursor.get[Int]("sleeps").toOption.get shouldBe 3
        }
      }
    }
    "lifecycle guards before parsing" which {
      "reject a terminal consumer even with complete readable readiness" in {
        Given("complete readiness published by a consumer that is already terminal")
        val mode = "terminal"
        When("readiness acquisition observes the consumer")
        val report = _probe(mode)
        Then("consumer termination is rejected before any readiness parsing or polling sleep")
        _rejected(report, "ValueError", "consumer terminated before readiness")
        _unread(report)
      }
      "reject an expired deadline even with complete readable readiness" in {
        Given("complete readiness and an already expired monotonic deadline")
        val mode = "timeout-readable"
        When("acquisition evaluates the deadline")
        val report = _probe(mode)
        Then("the explicit ceiling applies before parsing the existing file")
        _rejected(report, "ValueError", "consumer exceeded wrapper ceiling before readiness")
        _unread(report)
      }
      "honor SIGINT, SIGTERM and an owned stop request before parsing incomplete readiness" in {
        Given("incomplete readiness with each admitted cancellation input")
        val modes = Vector("sigint", "sigterm", "stop-request")
        When("acquisition checks interruption at the first poll")
        val reports = modes.map(mode => _probe(mode))
        Then("signals and the stop request interrupt immediately without parsing readiness")
        reports.take(2).foreach(report => _rejected(report, "SessionStopped", "wrapper received signal"))
        _rejected(reports.last, "SessionStopped", "associated stop request received")
        reports.foreach(_unread)
      }
    }
    "strict decoding and semantic admission" which {
      "preserve duplicate-member, non-object and OS errors without retry" in {
        Given("three complete strict-decoding failures rather than in-progress JSON")
        val modes = Vector("duplicate", "non-object", "os-error")
        When("the actual wrapper reads each fixture")
        val reports = modes.map(mode => _probe(mode))
        Then("strict failures escape on the first read without a polling sleep")
        _rejected(reports(0), "ValueError", "duplicate JSON member")
        _rejected(reports(1), "ValueError", "JSON object required")
        _rejected(reports(2), "IsADirectoryError", "ready.json")
        reports.foreach { report =>
          report.hcursor.get[Int]("sleeps").toOption.get shouldBe 0
          report.hcursor.get[Int]("readinessReads").toOption.get shouldBe 1
        }
      }
      "retain existing schema, PID, application-reference and target argv/path refusals" in {
        Given("complete declarations with one invalid semantic binding each")
        val modes = Vector("invalid-schema", "invalid-pid", "invalid-application", "invalid-argv", "invalid-path")
        When("acquired objects pass to the unchanged readiness semantic checks")
        val reports = modes.map(mode => _probe(mode))
        Then("every invalid binding is rejected immediately rather than retried as partial publication")
        _rejected(reports(0), "ValueError", "ready process/schema/ceiling differs")
        _rejected(reports(1), "ValueError", "ready process/schema/ceiling differs")
        _rejected(reports(2), "ValueError", "ready application reference differs")
        reports.drop(3).foreach(report => _rejected(report, "ValueError", "ready target path/runtime/argv/observation binding differs"))
        reports.foreach { report =>
          report.hcursor.get[Int]("sleeps").toOption.get shouldBe 0
          report.hcursor.get[Int]("readinessReads").toOption.get shouldBe 1
        }
      }
      "accept valid two-target readiness both as an acquired object and through observe's file reader" in {
        Given("a complete caller application reference and exact two-target declaration")
        val modes = Vector("valid-decoded", "valid-file")
        When("each existing readiness admission entry receives that declaration")
        val reports = modes.map(mode => _probe(mode))
        Then("both retain the exact object and its Unicode source bindings without polling")
        reports.foreach(report => _accepted(report, 0, 1))
      }
    }
  }

  private def _accepted(report: Json, sleeps: Int, reads: Int): Unit = {
    report.hcursor.get[String]("status").toOption.get shouldBe "accepted"
    report.hcursor.get[Json]("result").toOption.get shouldBe report.hcursor.get[Json]("expected").toOption.get
    report.hcursor.get[Int]("sleeps").toOption.get shouldBe sleeps
    report.hcursor.get[Int]("readinessReads").toOption.get shouldBe reads
  }

  private def _rejected(report: Json, kind: String, diagnostic: String): Unit = {
    report.hcursor.get[String]("status").toOption.get shouldBe "rejected"
    report.hcursor.get[String]("errorType").toOption.get shouldBe kind
    report.hcursor.get[String]("diagnostic").toOption.get should include(diagnostic)
  }

  private def _unread(report: Json): Unit = {
    report.hcursor.get[Int]("readinessReads").toOption.get shouldBe 0
    report.hcursor.get[Int]("sleeps").toOption.get shouldBe 0
  }

  private def _probe(mode: String, cut: Int = 0): Json = {
    val repository = Path.of("").toAbsolutePath.normalize
    val storage = repository.resolve("target/internal-model-end-to-end")
    Files.createDirectories(storage)
    val work = Files.createTempDirectory(storage, "readiness-")
    val script = work.resolve("probe.py")
    val output = work.resolve("stdout.json")
    val errors = work.resolve("stderr.txt")
    Files.writeString(script, _probe_source, StandardCharsets.UTF_8)
    val builder = new ProcessBuilder("/Users/asami/bin/uv", "run", "--no-cache", "--managed-python",
      "--no-python-downloads", "--script", script.toString,
      repository.resolve("scripts/test/run-internal-model-end-to-end-session.py").toString, mode, cut.toString)
    builder.directory(work.toFile)
    builder.redirectOutput(output.toFile)
    builder.redirectError(errors.toFile)
    val process = builder.start()
    try {
      val completed = process.waitFor(55, TimeUnit.SECONDS)
      withClue(s"owned readiness probe exceeded its ceiling; retained evidence: $work") {
        completed shouldBe true
      }
      val stderr = Files.readString(errors, StandardCharsets.UTF_8)
      val stdout = Files.readString(output, StandardCharsets.UTF_8)
      withClue(s"readiness probe $mode failed; evidence: $work; stderr: $stderr; stdout: $stdout") {
        process.exitValue() shouldBe 0
      }
      parse(stdout).fold(error => fail(s"invalid probe JSON at $output: ${error.message}"), identity)
    } finally {
      if (process.isAlive) {
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)
      }
    }
  }

  private val _probe_source: String = """# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
import importlib.util
import json
import os
from pathlib import Path
import signal
import sys
from types import SimpleNamespace

# Controlled reader regression only; no actual Cozy command or AI evidence.
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("readiness_wrapper", sys.argv[1])
module = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = module
spec.loader.exec_module(module)
mode, requestedcut = sys.argv[2], int(sys.argv[3])
root = Path.cwd() / "publication"
root.mkdir()
project = root / "project"
(project / "cml").mkdir(parents=True)
callerpath = root / "caller-input.json"
application = {"recordId": "application-日本語", "recordRevision": 701}
caller = {"schema": "textus.internal-model-e2e-caller.v1", "label": "表示 α 😀",
          "applicationReference": application}
callerpath.write_text(json.dumps(caller, ensure_ascii=False), encoding="utf-8")
producer = {"projectRoot": str(project), "inputPath": str(callerpath)}
fixture = {"schema": "textus.internal-model-e2e-ready.v1", "consumerPid": 901,
           "projectRoot": str(project), "inputPath": str(callerpath),
           "applicationReference": application, "waitCeilingSeconds": 1800, "targets": []}
for lane in ("alpha", "beta"):
    absolute = str(project / "cml" / (lane + ".cml"))
    Path(absolute).write_text("controlled reader fixture", encoding="utf-8")
    fixture["targets"].append({
        "targetId": "target-" + lane, "projectRelativePath": "cml/" + lane + ".cml",
        "absolutePath": absolute, "sourceAuthority": "source-owner-日本語",
        "sourceIdentity": "source-" + lane, "sourceRevision": "next-" + lane,
        "commandReference": {"recordId": "lint-" + lane, "recordRevision": 1},
        "runtimeVersion": "0.3.3-SNAPSHOT",
        "argv": ["--runtime", "0.3.3-SNAPSHOT", "lint", "cml", absolute, "--format", "json"],
        "observationPath": str(root / (lane + "-cozy-observation.json"))})
complete = json.dumps(fixture, ensure_ascii=False).encode("utf-8")
readypath = root / "ready.json"
cut = 0
initialerror = None
ticks = 0
reads = 0
interrupted = []
deadline = 0.3

def _check(condition, message):
    if not condition:
        raise RuntimeError(message)

class FakeProcess:
    pid = 901

    def poll(self):
        return 0 if mode == "terminal" else None

consumer = module.Child("consumer", FakeProcess(), [], root, root, root,
                        root / "consumer.stdout", root / "consumer.stderr", "controlled")
originalread = module._read_json

def _read(path):
    global reads
    if path == readypath:
        reads += 1
    return originalread(path)

def _monotonic():
    return ticks / 10

def _sleep(seconds):
    global ticks
    _check(seconds == 0.1, "poll interval must remain 0.1 seconds")
    ticks += 1
    if mode.startswith("delayed-"):
        readypath.write_bytes(complete)

module._read_json = _read
module.time = SimpleNamespace(monotonic=_monotonic, sleep=_sleep)
if mode.startswith("delayed-"):
    if mode == "delayed-property":
        cut = 1 + requestedcut % (len(complete) - 1)
    elif mode == "delayed-json":
        cut = len(complete) // 2
    elif mode == "delayed-utf8":
        cut = complete.index("日".encode("utf-8")) + 1
    elif mode != "delayed-empty":
        raise RuntimeError("unknown delayed mode")
    prefix = complete[:cut]
    try:
        json.loads(prefix.decode("utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError) as error:
        initialerror = type(error).__name__
    else:
        raise RuntimeError("selected prefix must be incomplete")
    readypath.write_bytes(prefix)
elif mode == "timeout-missing":
    pass
elif mode == "timeout-partial":
    readypath.write_bytes(complete[:len(complete) // 2])
elif mode == "timeout-malformed":
    readypath.write_bytes(b'{"broken": ???}')
elif mode in ("sigint", "sigterm", "stop-request"):
    readypath.write_bytes(b'{')
    if mode == "stop-request":
        (root / "stop-request.json").write_text(json.dumps({"wrapperPid": os.getpid()}), encoding="utf-8")
    else:
        interrupted.append(signal.SIGINT if mode == "sigint" else signal.SIGTERM)
elif mode == "duplicate":
    readypath.write_bytes(b'{"schema": "first", "schema": "second"}')
elif mode == "non-object":
    readypath.write_bytes(b'[]')
elif mode == "os-error":
    readypath.mkdir()
else:
    value = json.loads(complete)
    if mode == "invalid-schema":
        value["schema"] = "wrong-schema"
    elif mode == "invalid-pid":
        value["consumerPid"] = 902
    elif mode == "invalid-application":
        value["applicationReference"]["recordRevision"] = 702
    elif mode == "invalid-argv":
        value["targets"][0]["argv"][-1] = "text"
    elif mode == "invalid-path":
        value["targets"][0]["absolutePath"] = str(project / "wrong.cml")
    elif mode == "timeout-readable":
        deadline = 0.0
    elif mode not in ("terminal", "valid-decoded", "valid-file"):
        raise RuntimeError("unknown mode: " + mode)
    readypath.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")

try:
    if mode == "valid-file":
        result = module._ready(root, consumer.process.pid, producer)
    else:
        decoded = module._wait_readiness(root, consumer, deadline, interrupted)
        if mode.startswith("delayed-") or mode == "valid-decoded":
            _check(decoded == fixture, "acquisition changed the exact declaration")
            # A second readiness read would fail: validate only the acquired object.
            readypath.write_bytes(b'{')
        result = module._ready(root, consumer.process.pid, producer, decoded)
    report = {"status": "accepted", "result": result, "expected": fixture}
except (ValueError, OSError, module.SessionStopped) as error:
    report = {"status": "rejected", "errorType": type(error).__name__, "diagnostic": str(error)}
report.update(sleeps=ticks, readinessReads=reads, cut=cut, byteLength=len(complete), initialError=initialerror)
report.update(bytecodePath=spec.cached, bytecodeExists=Path(spec.cached).exists())
print(json.dumps(report, ensure_ascii=False))
"""
}
