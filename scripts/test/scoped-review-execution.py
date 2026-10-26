#!/usr/bin/env -S uv run --no-cache --managed-python --no-python-downloads --script
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""Run compiled scoped-review Operations without SBT.

External prepare/accept never execute AI. Explicit standalone mode owns one
loopback Ollama session and calls the existing Textus AI provider through SPI.
The helper never creates review findings or downloads models.
@since   Oct. 27, 2026
@version Oct. 27, 2026
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import time
import urllib.error
import urllib.request


REPOSITORY = Path(__file__).resolve().parents[2]
CLASSPATH = REPOSITORY / "target/streams/test/fullClasspath/_global/streams/export"
BRIDGE = "org.simplemodeling.textus.cbdsupport.ScopedQualityReviewExternalExecutionMain"
STANDALONE = "org.simplemodeling.textus.cbdsupport.ScopedQualityReviewStandaloneExecutionMain"


def standalone(args: argparse.Namespace) -> int:
    def interrupt(signum: int, frame: object) -> None:
        raise KeyboardInterrupt(f"Interrupted by signal {signum}")

    signal.signal(signal.SIGTERM, interrupt)
    endpoint = f"http://127.0.0.1:{args.port}"
    clienthttp = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    classpath = os.pathsep.join([
        CLASSPATH.read_text().strip(),
        str(args.ai_root / "target/scala-3.3.8/classes"),
        (args.ai_root / "target/streams/compile/dependencyClasspath/_global/streams/export").read_text().strip(),
    ])
    trace = args.output.with_suffix(".trace.json")
    log = args.output.with_suffix(".ollama.log")
    command = [str(args.java), "-cp", classpath, STANDALONE,
               str(args.input.resolve()), str(args.output.resolve()), endpoint,
               args.model, str(trace.resolve())]
    receipt = {
        "runDirectory": str(args.receipt.parent.resolve()),
        "operation": "reviewScopedQuality", "endpoint": endpoint,
        "model_selection": args.model, "input": str(args.input.resolve()),
        "output": str(args.output.resolve()), "trace": str(trace.resolve()),
        "server_log": str(log.resolve()), "started_at": datetime.now(timezone.utc).isoformat(),
        "ready": False, "clientSucceeded": False, "cleanup": False,
        "processCompletion": "not-started", "client_exit_code": None, "server_pid": None,
    }
    server = None
    exitcode = 1
    try:
        with socket.socket() as available:
            available.bind(("127.0.0.1", args.port))
        environment = dict(os.environ, OLLAMA_HOST=f"127.0.0.1:{args.port}",
                           OLLAMA_NO_CLOUD="1", OLLAMA_NOPRUNE="1")
        with log.open("wb") as stream:
            server = subprocess.Popen([str(args.ollama), "serve"], cwd=REPOSITORY,
                                      env=environment, stdout=stream, stderr=subprocess.STDOUT,
                                      start_new_session=True)
        receipt["server_pid"] = server.pid
        (args.receipt.parent / "server-process.json").write_text(json.dumps({
            "baseUrl": endpoint, "pid": server.pid,
            "startedAt": datetime.now(timezone.utc).isoformat(),
        }, indent=2) + "\n")
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            if server.poll() is not None:
                raise RuntimeError("Owned Ollama server exited before readiness")
            try:
                with clienthttp.open(endpoint + "/api/version", timeout=1) as response:
                    receipt["readiness"] = json.load(response)
                receipt["ready"] = True
                break
            except (urllib.error.URLError, OSError):
                time.sleep(0.25)
        if not receipt["ready"]:
            raise RuntimeError("Owned Ollama server did not become ready within 30 seconds")
        client = subprocess.run(command, cwd=REPOSITORY, timeout=args.timeout_seconds, check=False)
        receipt["client_exit_code"] = client.returncode
        receipt["clientSucceeded"] = client.returncode == 0
        exitcode = client.returncode
    except Exception as error:
        receipt["error"] = str(error)
    finally:
        if server is not None:
            try:
                os.killpg(server.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                server.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(server.pid, signal.SIGKILL)
                server.wait(timeout=5)
            try:
                os.killpg(server.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            with socket.socket() as probe:
                probe.settimeout(1)
                closed = probe.connect_ex(("127.0.0.1", args.port)) != 0
            receipt["cleanup_details"] = {"server_exit_code": server.returncode, "endpoint_closed": closed}
            receipt["cleanup"] = closed and server.returncode is not None
            receipt["processCompletion"] = "terminal"
            if not closed:
                exitcode = 1
        receipt["ended_at"] = datetime.now(timezone.utc).isoformat()
        args.receipt.write_text(json.dumps(receipt, indent=2) + "\n")
    return exitcode


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", required=True, type=Path)
    parser.add_argument("--ai-root", type=Path)
    parser.add_argument("--ollama", type=Path)
    parser.add_argument("--port", type=int)
    parser.add_argument("--model")
    parser.add_argument("--receipt", type=Path)
    parser.add_argument("--timeout-seconds", type=int, default=240)
    parser.add_argument("operation", choices=("prepareScopedQuality", "acceptScopedQuality", "reviewScopedQuality"))
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    if args.operation == "reviewScopedQuality":
        if any(value is None for value in (args.ai_root, args.ollama, args.port, args.model, args.receipt)):
            parser.error("reviewScopedQuality requires --ai-root --ollama --port --model --receipt")
        return standalone(args)
    return subprocess.run(
        [str(args.java), "-cp", CLASSPATH.read_text().strip(), BRIDGE,
         args.operation, str(args.input.resolve()), str(args.output.resolve())],
        cwd=REPOSITORY,
        check=False,
    ).returncode


if __name__ == "__main__":
    raise SystemExit(main())
