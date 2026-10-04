#!/usr/bin/env -S uv run --no-cache --managed-python --no-python-downloads --script
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""Own one terminal producer/fresh consumer test pair and its associated clients.

Only the committed Java Probe is launched. Cozy executes independently; observe
transports its parent-admitted terminal result to the existing semantic owner.
@since   Oct.  4, 2026
@version Oct.  4, 2026
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
from typing import Any


REPOSITORY = Path(__file__).resolve().parents[2]
WORK = REPOSITORY / "target/internal-model-end-to-end/work"
JAVA = Path("/Users/asami/.sdkman/candidates/java/21.0.2-tem/bin/java")
COZY = "/Users/asami/Library/Application Support/Coursier/bin/cozy"
CLASSPATH = REPOSITORY / "target/streams/test/fullClasspath/_global/streams/export"
MANAGED_PYTHON = Path("/Users/asami/.local/share/uv/python")
PROBE = "org.simplemodeling.textus.cbdsupport.runtime.InternalModelEndToEndProbe"
RUNTIME = "0.3.3-SNAPSHOT"
TARGETS = {"target-alpha": "alpha", "target-beta": "beta"}


@dataclass(frozen=True)
class RunArguments:
    run_root: Path
    java: Path
    classpath: str
    label: str
    reverse: str
    carrier: str
    cml_mode: str


@dataclass
class Child:
    name: str
    process: subprocess.Popen[bytes]
    argv: list[str]
    cwd: Path
    home: Path
    temporary: Path
    stdout: Path
    stderr: Path
    started_at: str
    cleanup: str = "already-terminal"

    def evidence(self, run_root: Path) -> dict[str, Any]:
        return {
            "pid": self.process.pid, "argv": self.argv,
            "cwd": str(self.cwd), "home": str(self.home),
            "tmp": str(self.temporary),
            "environment": {"HOME": str(self.home), "TMPDIR": str(self.temporary)},
            "startedAt": self.started_at, "exitCode": self.process.poll(),
            "terminated": self.process.returncode is not None,
            "cleanup": self.cleanup,
            "stdout": str(self.stdout.relative_to(run_root)),
            "stderr": str(self.stderr.relative_to(run_root)),
        }


class SessionStopped(Exception):
    pass


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _absolute(value: str, *, existing: bool = True, concrete: bool = True) -> Path:
    path = Path(value)
    _require(path.is_absolute() and str(path) == value and os.path.normpath(value) == value,
             "path must be an absolute normalized locator")
    resolved = path.resolve(strict=existing)
    if concrete:
        _require(resolved == path, "redirected work paths are not admitted")
    return path


def _run_root(value: str, *, existing: bool) -> Path:
    path = _absolute(value, existing=existing)
    _require(path.parent == WORK, "run must be directly below the repository test work root")
    if existing:
        _require(path.is_dir(), "associated run directory is missing")
    else:
        _require(not path.exists(), "run must be new; previous runs are retained")
    return path


def _unique_members(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        _require(key not in result, f"duplicate JSON member: {key}")
        result[key] = value
    return result


def _read_json(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=_unique_members)
    _require(type(value) is dict, f"JSON object required: {path.name}")
    return value


def _publish(path: Path, value: dict[str, Any]) -> None:
    # The concurrent Probe must see a complete new JSON file, never a partial
    # body or replacement. A same-directory link publishes exclusively/atomically.
    descriptor, name = tempfile.mkstemp(prefix=f".{path.name}-", dir=path.parent)
    temporary = Path(name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            json.dump(value, output, ensure_ascii=False, allow_nan=False, indent=2)
            output.write("\n")
        os.link(temporary, path)
    finally:
        temporary.unlink()


def _emit(value: dict[str, Any]) -> None:
    print(json.dumps(value, ensure_ascii=False, allow_nan=False), flush=True)


def _start(arguments: RunArguments, name: str, probe_argv: list[str],
           children: list[Child]) -> Child:
    operation = arguments.run_root / name
    operation.mkdir()
    cwd, home, temporary = (operation / item for item in ("cwd", "home", "tmp"))
    for directory in (cwd, home, temporary):
        directory.mkdir()
    stdout, stderr = operation / "stdout.json", operation / "stderr.txt"
    argv = [str(arguments.java), f"-Duser.home={home}", f"-Djava.io.tmpdir={temporary}",
            "-cp", arguments.classpath, PROBE, *probe_argv]
    started_at = _now()
    with stdout.open("xb") as out, stderr.open("xb") as err:
        process = subprocess.Popen(argv, cwd=cwd, env={"HOME": str(home), "TMPDIR": str(temporary)},
                                   stdin=subprocess.DEVNULL, stdout=out, stderr=err, shell=False)
    child = Child(name, process, argv, cwd, home, temporary, stdout, stderr, started_at)
    children.append(child)
    _publish(arguments.run_root / f"{name}-process.json", child.evidence(arguments.run_root))
    return child


def _cleanup(child: Child) -> None:
    if child.process.poll() is None:
        child.cleanup = "terminated"
        child.process.terminate()
        try:
            child.process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            child.cleanup = "forced-termination"
            child.process.kill()
            child.process.wait(timeout=5)


def _interruption(run_root: Path, interrupted: list[int]) -> None:
    if interrupted:
        raise SessionStopped(f"wrapper received signal {interrupted[0]}")
    request = run_root / "stop-request.json"
    if request.exists():
        value = _read_json(request)
        _require(set(value) == {"wrapperPid"} and type(value["wrapperPid"]) is int
                 and value["wrapperPid"] == os.getpid(), "stop request must name this wrapper PID")
        raise SessionStopped("associated stop request received")


def _wait(child: Child, deadline: float, run_root: Path, interrupted: list[int]) -> None:
    while child.process.poll() is None:
        _interruption(run_root, interrupted)
        _require(time.monotonic() < deadline, f"{child.name} exceeded its explicit ceiling")
        time.sleep(0.1)
    _interruption(run_root, interrupted)


def _wait_readiness(run_root: Path, consumer: Child, deadline: float,
                    interrupted: list[int]) -> dict[str, Any]:
    path = run_root / "ready.json"
    while True:
        _interruption(run_root, interrupted)
        _require(consumer.process.poll() is None, "consumer terminated before readiness")
        _require(time.monotonic() < deadline, "consumer exceeded wrapper ceiling before readiness")
        if path.exists():
            try:
                return _read_json(path)
            except (json.JSONDecodeError, UnicodeDecodeError):
                # The Probe writes directly to ready.json; existence may precede
                # complete JSON or even a complete UTF-8 code point.
                pass
        time.sleep(0.1)


def _ready(run_root: Path, consumer_pid: int, producer: dict[str, Any],
           value: dict[str, Any] | None = None) -> dict[str, Any]:
    if value is None:
        value = _read_json(run_root / "ready.json")
    _require(value.get("schema") == "textus.internal-model-e2e-ready.v1"
             and type(value.get("consumerPid")) is int and value["consumerPid"] == consumer_pid
             and value.get("waitCeilingSeconds") == 1800, "ready process/schema/ceiling differs")
    _require(value["projectRoot"] == producer["projectRoot"]
             and value["inputPath"] == producer["inputPath"], "ready producer locators differ")
    caller = _read_json(_absolute(producer["inputPath"]))
    _require(value["applicationReference"] == caller["applicationReference"],
             "ready application reference differs from independent caller input")
    targets = value["targets"]
    _require(type(targets) is list and len(targets) == 2
             and {target["targetId"] for target in targets} == set(TARGETS),
             "ready must declare the exact two target IDs")
    for target in targets:
        lane = TARGETS[target["targetId"]]
        relative = f"cml/{lane}.cml"
        absolute = str(Path(value["projectRoot"]) / relative)
        _require(target["projectRelativePath"] == relative and target["absolutePath"] == absolute
                 and target["runtimeVersion"] == RUNTIME
                 and target["argv"] == ["--runtime", RUNTIME, "lint", "cml", absolute, "--format", "json"]
                 and target["observationPath"] == str(run_root / f"{lane}-cozy-observation.json"),
                 "ready target path/runtime/argv/observation binding differs")
        for key in ("sourceAuthority", "sourceIdentity", "sourceRevision"):
            _require(type(target[key]) is str and bool(target[key].strip()), f"missing ready {key}")
        _require(type(target["commandReference"]) is dict, "ready command reference is missing")
    return value


def _field(value: dict[str, Any], key: str) -> Any:
    return value["fields"][key]


def _nonempty_counts(value: Any) -> bool:
    return type(value) is list and len(value) == 8 and all(type(item) is int and item > 0 for item in value)


def _expectations(arguments: RunArguments, producer: Child, consumer: Child) -> tuple[dict[str, bool], dict[str, Any]]:
    before = _read_json(arguments.run_root / "producer.json")
    after = _read_json(arguments.run_root / "consumer-result.json")
    checks = {
        "terminalPair": producer.process.returncode == 0 and consumer.process.returncode == 0,
        "distinctNativePids": producer.process.pid != consumer.process.pid
        and os.getpid() not in (producer.process.pid, consumer.process.pid),
        "reportedPids": before["pid"] == producer.process.pid and after["pid"] == consumer.process.pid
        and after["producerPid"] == producer.process.pid,
        "externalMode": after["mode"] == "external",
        "fullBeforeEquality": after["before"] == before["before"],
        "fullSelectionEquality": after["selection"] == before["selection"],
        "fullPhase9Equality": after["phase9"] == before["phase9"],
        "allEightBefore": _nonempty_counts(before["counts"]) and _nonempty_counts(after["beforeCounts"]),
        "beforeSidecars": before["sidecars"] is True and after["beforeSidecars"] is True,
        "preApplicationSources": after["preApplicationSourcesUnchanged"] is True,
        "physicalEffectsRetained": after["physicalEffectsRetained"] is True,
        "cursorUnchanged": after["cursorUnchanged"] is True,
        "separateDirectories": all(getattr(producer, key) != getattr(consumer, key)
                                   for key in ("cwd", "home", "temporary")),
    }
    required = {"HOME", "TMPDIR"}
    allowed = required | ({"__CF_USER_TEXT_ENCODING"} if sys.platform == "darwin" else set())
    keys = after["environmentKeys"]
    checks["isolatedObservedEnvironment"] = type(keys) is list and required <= set(keys) <= allowed
    paths = before["paths"]
    checks["retainedInventory"] = set(("project.yaml", "src/main/internal-model/manifest.yaml",
        "cml/alpha.cml", "cml/beta.cml", "src/main/cml/original.cml")) <= set(paths) and "caller-input.json" not in paths
    application, post = after["applicationReport"], after["postValidationReport"]
    checks["actualApplicationRetained"] = _field(post, "application") == application
    gate = _field(application, "gate")
    approval = _field(_field(gate, "continuation"), "approval")["value"]
    checks["freshApprovalMeaning"] = _field(approval, "record") == _field(_field(gate, "originalapproval"), "record")
    commands = _field(post, "commands")
    observed = [_read_json(arguments.run_root / f"{lane}-cozy-observation.json") for lane in ("alpha", "beta")]
    checks["actualCommandOutcomes"] = len(commands) == 2 and all(
        _field(_field(command, "outcome"), "exitcode") == observation["outcome"]["exitCode"]
        and _field(_field(command, "outcome"), "stdout")["value"] == observation["outcome"]["stdout"]
        and _field(_field(command, "outcome"), "stderr")["value"] == observation["outcome"]["stderr"]
        for command, observation in zip(commands, observed))
    exits = [item["outcome"]["exitCode"] for item in observed]
    if arguments.cml_mode == "success":
        checks.update({
            "pendingAcceptanceOnly": after["status"] == "ReprojectedPendingAcceptance",
            "allEightAfter": _nonempty_counts(after["counts"]),
            "retainedSidecars": after["sidecars"] is True,
            "requiredSourcesUnchanged": after["requiredSourcesUnchanged"] is True,
            "noProblems": _field(post, "problems") == [],
            "successfulActualLint": exits == [0, 0],
        })
    else:
        checks.update({
            "failedValidation": after["status"] == "Failed",
            "noSuccessfulContinuity": _field(post, "continuity") == {"type": "None"} and after["counts"] == [],
            "reportedProblems": bool(_field(post, "problems")),
            "actualNegativeAlphaLint": exits[0] != 0 and exits[1] == 0,
        })
    return checks, {"status": after["status"], "beforeCounts": after["beforeCounts"],
                   "counts": after["counts"], "lintExitCodes": exits}


def _run(arguments: RunArguments) -> int:
    WORK.mkdir(parents=True, exist_ok=True)
    arguments.run_root.mkdir()
    children: list[Child] = []
    interrupted: list[int] = []
    prior_handlers = {number: signal.signal(number, lambda number, frame: interrupted.append(number))
                      for number in (signal.SIGINT, signal.SIGTERM)}
    summary: dict[str, Any] = {"wrapperPid": os.getpid(), "startedAt": _now(),
        "inputs": {"label": arguments.label, "reverse": arguments.reverse,
                   "carrier": arguments.carrier, "cmlMode": arguments.cml_mode},
        "status": "harness-error", "conditions": {}, "semantic": None}
    exit_code = 3
    try:
        _publish(arguments.run_root / "wrapper.json", {"wrapperPid": os.getpid(), "startedAt": summary["startedAt"]})
        producer = _start(arguments, "producer", ["producer", str(arguments.run_root), arguments.label,
                          arguments.reverse, arguments.carrier, arguments.cml_mode], children)
        _wait(producer, time.monotonic() + 60, arguments.run_root, interrupted)
        _publish(arguments.run_root / "producer-terminal.json", producer.evidence(arguments.run_root))
        _require(producer.process.returncode == 0, "producer did not exit successfully")
        produced = _read_json(arguments.run_root / "producer.json")
        _require(produced["pid"] == producer.process.pid, "producer reported PID differs")
        root = _absolute(produced["projectRoot"])
        caller = _absolute(produced["inputPath"])
        _require(root == arguments.run_root / "project" and caller == arguments.run_root / "caller-input.json"
                 and root.is_dir() and caller.is_file(), "producer project/caller locations differ")
        consumer = _start(arguments, "consumer", ["consumer", str(arguments.run_root), str(root), str(caller), "external"], children)
        _require(producer.process.pid != consumer.process.pid, "observed child PIDs must differ")
        deadline = time.monotonic() + 1860
        decoded = _wait_readiness(arguments.run_root, consumer, deadline, interrupted)
        ready = _ready(arguments.run_root, consumer.process.pid, produced, decoded)
        _require(consumer.process.poll() is None and producer.process.poll() == 0,
                 "readiness requires a live consumer and terminal producer")
        session = {"wrapperPid": os.getpid(), "producerPid": producer.process.pid,
                   "consumerPid": consumer.process.pid, "ready": "ready.json",
                   "producer": producer.evidence(arguments.run_root),
                   "consumer": consumer.evidence(arguments.run_root),
                   "projectRoot": ready["projectRoot"], "targets": ready["targets"],
                   "stopArgv": ["stop", "--run-root", str(arguments.run_root), "--wrapper-pid", str(os.getpid())]}
        _publish(arguments.run_root / "session-ready.json", session)
        _emit({"event": "ready", **session})
        _wait(consumer, deadline, arguments.run_root, interrupted)
        checks, semantic = _expectations(arguments, producer, consumer)
        summary.update(conditions=checks, semantic=semantic,
                       status="expectations-met" if all(checks.values()) else "expectation-failure")
        exit_code = 0 if all(checks.values()) else 3
    except SessionStopped as error:
        summary.update(status="stopped", diagnostic=str(error))
        exit_code = 130
    except (OSError, ValueError, KeyError, TypeError, subprocess.SubprocessError) as error:
        summary.update(status="harness-error", errorType=type(error).__name__, diagnostic=str(error))
    finally:
        cleanup_errors: list[str] = []
        for child in children:
            try:
                _cleanup(child)
            except (OSError, subprocess.SubprocessError) as error:
                cleanup_errors.append(f"{child.name}: {error}")
        summary.update(endedAt=_now(), children={child.name: child.evidence(arguments.run_root) for child in children},
                       cleanupComplete=all(child.process.poll() is not None for child in children),
                       cleanupErrors=cleanup_errors,
                       evidence={"producer": "producer.json", "consumer": "consumer-result.json",
                                 "ready": "ready.json", "observations": [f"{lane}-cozy-observation.json" for lane in ("alpha", "beta")]})
        if cleanup_errors or not summary["cleanupComplete"]:
            summary["status"] = "cleanup-failure"
            exit_code = 3
        summary["wrapperExitCode"] = exit_code
        _publish(arguments.run_root / "terminal-summary.json", summary)
        _emit({"event": "terminal", **summary})
        for number, handler in prior_handlers.items():
            signal.signal(number, handler)
    return exit_code


def _observe(run_root: Path, target_id: str, receipt: Path) -> int:
    _require(not (run_root / "terminal-summary.json").exists(), "session is already terminal")
    session = _read_json(run_root / "session-ready.json")
    wrapper = _read_json(run_root / "wrapper.json")
    _require(session["wrapperPid"] == wrapper["wrapperPid"], "associated wrapper ownership differs")
    ready = _ready(run_root, session["consumerPid"], _read_json(run_root / "producer.json"))
    target = next(item for item in ready["targets"] if item["targetId"] == target_id)
    result = _read_json(receipt)
    _require(result.get("schema") == "cozy.exact-cli-command-result.v6" and result.get("cli_kind") == "cozy",
             "actual terminal Cozy v6 result required")
    _require(result.get("command_completion") == "terminal" and result.get("timed_out") is False
             and type(result.get("exit_code")) is int and -(2**31) <= result["exit_code"] < 2**31,
             "result must contain a determinate native integer exit without timeout")
    _require(result["working_directory"] == ready["projectRoot"] and result["executable"] == COZY
             and result["argv"] == [COZY, *target["argv"]], "actual cwd/executable/argv differs from ready binding")
    stdout = _absolute(result["stdout_file"], concrete=False).read_bytes().decode("utf-8")
    stderr = _absolute(result["stderr_file"], concrete=False).read_bytes().decode("utf-8")
    observation = {"schema": "textus.cml-change-command-observation.v1",
        "commandReference": target["commandReference"], "applicationReference": ready["applicationReference"],
        "projectRoot": ready["projectRoot"], "targetId": target["targetId"],
        "projectRelativePath": target["projectRelativePath"], "sourceAuthority": target["sourceAuthority"],
        "sourceIdentity": target["sourceIdentity"], "sourceRevision": target["sourceRevision"],
        "runtimeVersion": target["runtimeVersion"], "argv": target["argv"],
        "outcome": {"kind": "terminal", "exitCode": result["exit_code"], "stdout": stdout, "stderr": stderr}}
    path = Path(target["observationPath"])
    _publish(path, observation)
    _emit({"event": "observation-published", "targetId": target_id,
           "observation": str(path.relative_to(run_root)), "nativeExitCode": result["exit_code"],
           "receipt": str(receipt), "stdoutFile": result["stdout_file"], "stderrFile": result["stderr_file"]})
    return 0


def _stop(run_root: Path, wrapper_pid: int) -> int:
    _require(not (run_root / "terminal-summary.json").exists(), "wrapper is already terminal")
    wrapper = _read_json(run_root / "wrapper.json")
    _require(wrapper["wrapperPid"] == wrapper_pid, "stop PID must match observed wrapper ownership")
    _publish(run_root / "stop-request.json", {"wrapperPid": wrapper_pid})
    _emit({"event": "stop-requested", "wrapperPid": wrapper_pid})
    return 0


def main() -> int:
    _require(sys.implementation.name == "cpython" and sys.version_info >= (3, 12)
             and sys.version_info.releaselevel == "final"
             and Path(sys.base_prefix).resolve().is_relative_to(MANAGED_PYTHON.resolve()),
             "stable uv-managed CPython >=3.12 is required")
    parser = argparse.ArgumentParser(description=__doc__, allow_abbrev=False)
    operations = parser.add_subparsers(dest="operation", required=True)
    run = operations.add_parser("run", allow_abbrev=False)
    for name in ("run-root", "java", "classpath-file", "label"):
        run.add_argument(f"--{name}", required=True)
    run.add_argument("--reverse", required=True, choices=("true", "false"))
    run.add_argument("--carrier", required=True, choices=("43", "79"))
    run.add_argument("--cml-mode", required=True, choices=("success", "lint-failure"))
    observe = operations.add_parser("observe", allow_abbrev=False)
    observe.add_argument("--run-root", required=True)
    observe.add_argument("--target-id", required=True, choices=tuple(TARGETS))
    observe.add_argument("--receipt", required=True)
    stop = operations.add_parser("stop", allow_abbrev=False)
    stop.add_argument("--run-root", required=True)
    stop.add_argument("--wrapper-pid", required=True, type=int)
    args = parser.parse_args()
    if args.operation == "run":
        run_root = _run_root(args.run_root, existing=False)
        java, classpath_file = _absolute(args.java), _absolute(args.classpath_file)
        _require(java == JAVA and java.is_file() and os.access(java, os.X_OK), "only the selected Java is admitted")
        _require(classpath_file == CLASSPATH and classpath_file.is_file(), "existing SBT Test fullClasspath export required")
        classpath = classpath_file.read_text(encoding="utf-8").strip()
        entries = classpath.split(os.pathsep)
        _require(bool(classpath) and all(item and Path(item).is_absolute() and Path(item).exists() for item in entries),
                 "classpath must contain existing absolute entries")
        _require(bool(args.label.strip()) and "\0" not in args.label, "label must be nonblank text")
        return _run(RunArguments(run_root, java, classpath, args.label, args.reverse, args.carrier, args.cml_mode))
    run_root = _run_root(args.run_root, existing=True)
    if args.operation == "observe":
        return _observe(run_root, args.target_id, _absolute(args.receipt, concrete=False))
    _require(args.wrapper_pid > 0, "wrapper PID must be positive")
    return _stop(run_root, args.wrapper_pid)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError, StopIteration) as error:
        print(f"INPUT_OR_ENVIRONMENT_ERROR: {error}", file=sys.stderr)
        sys.exit(3)
