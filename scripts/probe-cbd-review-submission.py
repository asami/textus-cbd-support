#!/usr/bin/env python3

import argparse
import json
from pathlib import Path
import urllib.error
import urllib.request


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def _document(examples_dir: Path, name: str) -> str:
    path = examples_dir / name
    _require(path.is_file(), f"Review provider document is missing: {path}")
    return path.read_text(encoding="utf-8").strip()


def _post(base_url: str, body: dict, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}/rest/v1/cbd-support/cbd-review-admin/post",
        data=json.dumps(body, separators=(",", ":")).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            _require(
                response.headers.get_content_type() == "application/json",
                f"Review gateway returned unexpected content type: {response.headers.get_content_type()}",
            )
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {error.code} from {request.full_url}: {detail}") from error


def _run(base_url: str, examples_dir: Path, timeout: float) -> None:
    descriptor = _document(examples_dir, "car-review-provider-descriptor-v1.json")
    provider_request = _document(examples_dir, "car-review-provider-request-v1.json")
    bundle = _document(examples_dir, "car-review-evidence-bundle-v1.json")
    request_json = json.loads(provider_request)
    review_id = request_json["reviewId"]
    target = request_json["target"]
    submission = {
        "schemaVersion": "textus.cbd.review-submission.v1",
        "documentType": "provider-document-submission",
        "reviewId": review_id,
        "target": target,
        "providers": [
            {
                "availability": "enabled",
                "descriptor": descriptor,
                "providerRequest": provider_request,
                "bundle": bundle,
            }
        ],
    }
    _require("workspace" not in json.dumps(submission).lower(), "Submission unexpectedly carries workspace authority")
    outer = _post(base_url, {"submissionDocument": json.dumps(submission, separators=(",", ":"))}, timeout)
    _require(set(outer) == {"canonical_response", "artifact_bundle"}, f"Unexpected Review gateway response envelope: {outer}")
    canonical = outer.get("canonical_response")
    artifact_bundle = outer.get("artifact_bundle")
    _require(isinstance(canonical, str) and canonical, f"Review gateway omitted canonical response: {outer}")
    _require(isinstance(artifact_bundle, str) and artifact_bundle, f"Review gateway omitted artifact bundle: {outer}")
    response = json.loads(canonical)
    _require(response.get("documentType") == "canonical-review-response", f"Unexpected canonical response: {response}")
    report = response.get("report", {})
    _require(report.get("reviewId") == review_id, f"Canonical report lost Review binding: {response}")
    _require(response.get("gateResult") == report.get("gate", {}).get("result"), f"Canonical gate mismatch: {response}")
    _require(isinstance(response.get("attestation"), dict), f"Canonical response omitted attestation: {response}")
    bundle = json.loads(artifact_bundle)
    _require(bundle.get("schemaVersion") == "textus.cbd.review-artifact-bundle.v1", f"Unexpected Review artifact bundle: {bundle}")
    _require(bundle.get("documentType") == "review-artifact-bundle", f"Unexpected Review artifact bundle: {bundle}")
    _require(bundle.get("reportDigest") == report.get("reportDigest"), f"Review artifact bundle lost Report binding: {bundle}")
    _require(isinstance(bundle.get("markdown"), str) and bundle["markdown"], f"Review artifact bundle omitted Markdown: {bundle}")
    _require(isinstance(bundle.get("pdfBase64"), str) and bundle["pdfBase64"], f"Review artifact bundle omitted PDF: {bundle}")
    print(f"CBD_REVIEW_SUBMISSION_OK review_id={review_id} gate={response['gateResult']}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Probe the running private CBD CAR Review submission gateway.")
    parser.add_argument("--base-url", default="http://127.0.0.1:19538")
    parser.add_argument("--examples-dir", required=True)
    parser.add_argument("--timeout", type=float, default=30.0)
    arguments = parser.parse_args()
    try:
        _run(arguments.base_url.rstrip("/"), Path(arguments.examples_dir), arguments.timeout)
        return 0
    except (AssertionError, json.JSONDecodeError, KeyError, OSError, RuntimeError) as error:
        print(f"CBD_REVIEW_SUBMISSION_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
