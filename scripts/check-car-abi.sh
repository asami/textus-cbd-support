#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COZY_BIN="${COZY_BIN:-$(command -v cozy || true)}"
CURRENT_MANIFEST="$PROJECT_ROOT/src/main/car/abi-manifest.json"
FIXTURE_ROOT="$PROJECT_ROOT/src/test/resources/abi"
BASELINE="$FIXTURE_ROOT/transition-baseline-0.1.0.json"
COMPATIBLE="$FIXTURE_ROOT/compatible-addition-0.2.0.json"
BREAKING_MINOR="$FIXTURE_ROOT/breaking-minor-0.2.0.json"
INTENTIONAL_MAJOR="$FIXTURE_ROOT/intentional-major-1.0.0.json"
CAR="$PROJECT_ROOT/target/textus-cbd-support-0.1.0-SNAPSHOT.car"

if [[ ! -x "$COZY_BIN" ]]; then
  echo "Cozy launcher is missing: $COZY_BIN" >&2
  exit 1
fi
for file in "$CURRENT_MANIFEST" "$BASELINE" "$COMPATIBLE" "$BREAKING_MINOR" "$INTENTIONAL_MAJOR"; do
  if [[ ! -f "$file" ]]; then
    echo "CAR ABI input is missing: $file" >&2
    exit 1
  fi
done

_require_marker() {
  local output="$1"
  local marker="$2"
  local scenario="$3"
  if ! grep -Fq "$marker" <<<"$output"; then
    echo "CAR ABI $scenario did not report $marker" >&2
    echo "$output" >&2
    exit 1
  fi
}

(cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)

MODEL_METADATA="$PROJECT_ROOT/target/cozy/model-metadata.json"
if [[ ! -f "$MODEL_METADATA" ]]; then
  MODEL_METADATA="$PROJECT_ROOT/target/sbt-cozy/delegate-work/run-0/target/cozy/model-metadata.json"
fi
if [[ ! -f "$MODEL_METADATA" ]]; then
  echo "Generated CML model metadata is missing." >&2
  exit 1
fi
if [[ ! -f "$CAR" ]]; then
  echo "Built CAR is missing: $CAR" >&2
  exit 1
fi

"$SCRIPT_DIR/check-car-abi-surface.py" \
  --manifest "$CURRENT_MANIFEST" \
  --model-metadata "$MODEL_METADATA" \
  --archive "$CAR"

if ! current_output="$("$COZY_BIN" lint abi "$PROJECT_ROOT" --strict 2>&1)"; then
  echo "$current_output" >&2
  exit 1
fi
_require_marker "$current_output" "OK abi.manifest" "current manifest"
_require_marker "$current_output" "WARN abi.baseline.missing" "first-release baseline"

if ! compatible_output="$("$COZY_BIN" lint abi "$COMPATIBLE" --baseline "$BASELINE" --strict 2>&1)"; then
  echo "$compatible_output" >&2
  exit 1
fi
_require_marker "$compatible_output" "OK abi.operation.added" "compatible minor addition"

if breaking_output="$("$COZY_BIN" lint abi "$BREAKING_MINOR" --baseline "$BASELINE" --strict 2>&1)"; then
  echo "CAR ABI breaking minor change was accepted." >&2
  echo "$breaking_output" >&2
  exit 1
fi
_require_marker "$breaking_output" "FAIL abi.operation.removed" "breaking minor removal"

if ! major_output="$("$COZY_BIN" lint abi "$INTENTIONAL_MAJOR" --baseline "$BASELINE" --strict 2>&1)"; then
  echo "$major_output" >&2
  exit 1
fi
_require_marker "$major_output" "OK abi.operation.removed" "intentional major removal"
_require_marker "$major_output" "Major version permits breaking ABI change" "intentional major transition"

echo "CAR_ABI_CURRENT_BASELINE_PENDING first_release=0.1.0"
echo "CAR_ABI_COMPATIBLE_ADDITION_OK baseline=0.1.0 current=0.2.0"
echo "CAR_ABI_BREAKING_MINOR_REJECTED baseline=0.1.0 current=0.2.0"
echo "CAR_ABI_INTENTIONAL_MAJOR_OK baseline=0.1.0 current=1.0.0"
echo "CAR_ABI_GOVERNANCE_OK"
