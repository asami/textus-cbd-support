#!/usr/bin/env python3

import argparse
import json
from pathlib import Path
import subprocess
import urllib.error
import urllib.request


def _load(path: Path) -> str:
    if not path.is_file():
        raise ValueError(f"Review provider document is missing: {path}")
    return path.read_text(encoding="utf-8").strip()


def _cozy_request(sbt_request: str) -> str:
    request = json.loads(sbt_request)
    return json.dumps(
        {
            "schemaVersion": "textus.cbd.review-provider.v1",
            "documentType": "provider-request",
            "reviewId": request["reviewId"],
            "target": request["target"],
            "limits": {
                "maxEvidenceItems": 256,
                "maxObservations": 256,
                "maxInputBytes": 16777216,
                "timeoutMillis": 120000,
            },
            "requestedCapabilities": ["cozy.car-analysis"],
            "requestedEvidenceKinds": ["car-project", "cml-model", "build", "car-package", "abi", "documentation"],
            "rules": {"include": [], "exclude": []},
        },
        separators=(",", ":"),
    )


def _cozy_document(command: list[str], request: str, project_root: str) -> tuple[str, str]:
    descriptor = subprocess.run(
        command + ["review", "car-descriptor", "--provider-version", "0.3.0-SNAPSHOT", "--descriptor"],
        check=True,
        text=True,
        capture_output=True,
    ).stdout.strip()
    bundle = subprocess.run(
        command + ["review", "car-evidence", "--project-root", project_root, "--provider-version", "0.3.0-SNAPSHOT", "--request-stdin"],
        check=True,
        text=True,
        input=request,
        capture_output=True,
    ).stdout.strip()
    return descriptor, bundle


def _post(base_url: str, submission: dict) -> dict:
    request = urllib.request.Request(
        f"{base_url}/rest/v1/cbd-support/cbd-review-admin/post",
        data=json.dumps({"submissionDocument": json.dumps(submission, separators=(",", ":"))}, separators=(",", ":")).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def _run(arguments: argparse.Namespace) -> None:
    directory = Path(arguments.evidence_dir)
    sbt_descriptor = _load(directory / "provider-descriptor.json")
    sbt_request = _load(directory / "provider-request.json")
    sbt_bundle = _load(directory / "evidence-bundle.json")
    request = _cozy_request(sbt_request)
    cozy_descriptor, cozy_bundle = _cozy_document(
        ["java", "-cp", arguments.cozy_classpath, "cozy.Cozy"],
        request,
        arguments.project_root,
    )
    source = json.loads(sbt_request)
    response = _post(arguments.base_url.rstrip("/"), {
        "schemaVersion": "textus.cbd.review-submission.v1",
        "documentType": "provider-document-submission",
        "reviewId": source["reviewId"],
        "target": source["target"],
        "providers": [
            {"availability": "enabled", "descriptor": cozy_descriptor, "providerRequest": request, "bundle": cozy_bundle},
            {"availability": "enabled", "descriptor": sbt_descriptor, "providerRequest": sbt_request, "bundle": sbt_bundle},
        ],
    })
    canonical = response.get("canonical_response")
    if set(response) != {"canonical_response"} or not isinstance(canonical, str):
        raise ValueError(f"Unexpected Review response envelope: {response}")
    Path(arguments.save).write_text(canonical + "\n", encoding="utf-8")
    payload = json.loads(canonical)
    print(f"CBD_SBT_COZY_DIRECT_SUBMISSION_OK review_id={payload['report']['reviewId']} gate={payload['gateResult']}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Capture CBD's actual response to the paired Cozy and sbt-cozy provider documents.")
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--evidence-dir", required=True)
    parser.add_argument("--project-root", required=True)
    parser.add_argument("--cozy-classpath", required=True)
    parser.add_argument("--save", required=True)
    arguments = parser.parse_args()
    try:
        _run(arguments)
        return 0
    except (KeyError, OSError, ValueError, json.JSONDecodeError, subprocess.CalledProcessError, urllib.error.URLError) as error:
        print(f"CBD_SBT_COZY_DIRECT_SUBMISSION_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
