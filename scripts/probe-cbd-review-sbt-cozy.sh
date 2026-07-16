#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SBT_COZY_ROOT="${SBT_COZY_ROOT:-/Users/asami/src/dev2026/sbt-cozy}"
COZY_ROOT="${COZY_ROOT:-/Users/asami/src/dev2025/cozy}"
FIXTURE_ROOT="$SBT_COZY_ROOT/src/sbt-test/cozy/review-submit"
COZY_CLASSPATH_FILE="$COZY_ROOT/target/classpath.txt"
PLUGIN_VERSION="${SBT_COZY_PLUGIN_VERSION:-$(sed -nE 's/^ThisBuild \/ version := "([^"]+)"/\1/p' "$SBT_COZY_ROOT/build.sbt")}"

usage() {
  echo "usage: $0 --base-url http://127.0.0.1:<port>" >&2
}

if [[ "${1:-}" != "--base-url" || -z "${2:-}" || $# -ne 2 ]]; then
  usage
  exit 2
fi

BASE_URL="${2%/}"
case "$BASE_URL" in
  http://127.0.0.1:* | http://localhost:*) ;;
  *)
    echo "CBD Review sbt-cozy probe requires a loopback endpoint: $BASE_URL" >&2
    exit 2
    ;;
esac

for required in "$SBT_COZY_ROOT/build.sbt" "$FIXTURE_ROOT/build.sbt" "$COZY_CLASSPATH_FILE"; do
  if [[ ! -f "$required" ]]; then
    echo "CBD Review sbt-cozy probe input is missing: $required" >&2
    exit 2
  fi
done
if [[ -z "$PLUGIN_VERSION" ]]; then
  echo "Could not resolve the sbt-cozy SNAPSHOT version." >&2
  exit 2
fi

(cd "$SBT_COZY_ROOT" && sbt --batch publishLocal)
(cd "$FIXTURE_ROOT" && sbt --batch \
  "-Dplugin.version=$PLUGIN_VERSION" \
  cozyReviewSbtEvidence)
python3 "$SCRIPT_DIR/probe-cbd-review-sbt-cozy-direct.py" \
  --base-url "$BASE_URL" \
  --evidence-dir "$FIXTURE_ROOT/target/cbd-review/sbt-cozy" \
  --project-root "$FIXTURE_ROOT" \
  --cozy-classpath "$(cat "$COZY_CLASSPATH_FILE")" \
  --save "$FIXTURE_ROOT/target/cbd-review/sbt-cozy/canonical-response-raw.json"
(cd "$FIXTURE_ROOT" && sbt --batch \
  "-Dplugin.version=$PLUGIN_VERSION" \
  "-Dcbd.review.endpoint=$BASE_URL/rest/v1/cbd-support/cbd-review-admin/post" \
  "-Dcozy.review.classpath=$(cat "$COZY_CLASSPATH_FILE")" \
  cozyReviewSubmit \
  verifyReviewSubmission)

python3 - "$FIXTURE_ROOT/target/cbd-review/sbt-cozy/canonical-response.json" <<'PY'
import json
import sys
from pathlib import Path

path = Path(sys.argv[1])
payload = json.loads(path.read_text(encoding="utf-8"))
if payload.get("documentType") != "canonical-review-response-artifact":
    raise SystemExit(f"unexpected canonical response document: {payload}")
report = payload.get("report")
if not isinstance(report, dict) or not report.get("reviewId"):
    raise SystemExit(f"canonical report identity is missing: {payload}")
providers = {
    entry.get("provider", {}).get("id")
    for entry in report.get("execution", {}).get("providers", [])
}
if providers != {"cozy", "sbt-cozy"}:
    raise SystemExit(f"canonical report providers are not the actual pair: {providers}")
if payload.get("gateResult") != report.get("gate", {}).get("result"):
    raise SystemExit(f"canonical gate is inconsistent: {payload}")
print(f"CBD_SBT_COZY_REVIEW_SUBMISSION_OK review_id={report['reviewId']} gate={payload['gateResult']}")
PY
