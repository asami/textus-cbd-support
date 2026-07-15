#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIE_ROOT="${TEXTUS_SIE_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-semantic-integration-engine}"
CNCF_BIN="${CNCF_BIN:-$(command -v cncf || true)}"
CNCF_VERSION_FILE="${CNCF_VERSION_FILE:-/Users/asami/src/dev2026/cncf-samples/versions/cncf-version.conf}"
CNCF_VERSION="${CNCF_VERSION:-$(tr -d '[:space:]' < "$CNCF_VERSION_FILE")}"
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-19535}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
STARTUP_TIMEOUT_SECONDS="${CBD_SIE_SAR_STARTUP_TIMEOUT_SECONDS:-120}"
SHUTDOWN_TIMEOUT_SECONDS="${CBD_SIE_SAR_SHUTDOWN_TIMEOUT_SECONDS:-30}"
CBD_CAR="$PROJECT_ROOT/target/textus-cbd-support-0.1.0-SNAPSHOT.car"
SIE_CAR="$SIE_ROOT/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car"
SAR_DESCRIPTOR="$PROJECT_ROOT/examples/cbd-sie-sar/subsystem-descriptor.yaml"
PROFILE_DIR="$PROJECT_ROOT/examples/cbd-sie-sar/profiles"
PROFILES=(
  "baseline"
  "global-disabled"
  "sie-service-disabled"
  "operation-disabled"
)
PROFILE_DESCRIPTORS=(
  "$SAR_DESCRIPTOR"
  "$PROFILE_DIR/global-disabled.yaml"
  "$PROFILE_DIR/sie-service-disabled.yaml"
  "$PROFILE_DIR/operation-disabled.yaml"
)

case "$CNCF_HTTP_BASEURL" in
  http://127.0.0.1:* | http://localhost:*) ;;
  *)
    echo "The representative SAR check requires a loopback HTTP base URL: $CNCF_HTTP_BASEURL" >&2
    exit 1
    ;;
esac

if [[ ! -x "$CNCF_BIN" ]]; then
  echo "CNCF launcher is missing: $CNCF_BIN" >&2
  exit 1
fi
if [[ ! -f "$SIE_ROOT/project.yaml" ]]; then
  echo "SIE project is missing: $SIE_ROOT" >&2
  exit 1
fi
for descriptor in "${PROFILE_DESCRIPTORS[@]}"; do
  if [[ ! -f "$descriptor" ]]; then
    echo "Representative SAR descriptor is missing: $descriptor" >&2
    exit 1
  fi
done
if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
  echo "A server already responds at $CNCF_HTTP_BASEURL; refusing to reuse an unowned process." >&2
  exit 1
fi

(cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)
(cd "$SIE_ROOT" && sbt --batch cozyBuildCAR)

runtime_dir="$(mktemp -d "${TMPDIR:-/tmp}/textus-cbd-sie-sar.XXXXXX")"
server_pid=""
server_listener_pid=""
server_log=""

stop_server() {
  local shutdown_deadline

  if [[ -n "$server_listener_pid" ]] && kill -0 "$server_listener_pid" >/dev/null 2>&1; then
    kill "$server_listener_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" >/dev/null 2>&1; then
    kill "$server_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "$server_pid" ]]; then
    wait "$server_pid" >/dev/null 2>&1 || true
  fi
  server_pid=""
  server_listener_pid=""
  shutdown_deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
  while curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; do
    if ((SECONDS >= shutdown_deadline)); then
      echo "Timed out waiting for the representative SAR server to stop." >&2
      return 1
    fi
    sleep 0.25
  done
}

cleanup() {
  stop_server || true
  rm -rf "$runtime_dir"
}

show_server_log() {
  if [[ -s "$server_log" ]]; then
    echo "CNCF server log:" >&2
    tail -n 100 "$server_log" >&2
  fi
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

run_profile() {
  local profile="$1"
  local descriptor="$2"
  local profile_root="$runtime_dir/$profile"
  local component_dir="$profile_root/component.d"
  local sar_root="$profile_root/textus-cbd-sie.sar.d"
  local sar_file="$component_dir/textus-cbd-sie.sar"
  local deadline
  local server_ready=false

  server_log="$profile_root/server.log"
  mkdir -p "$component_dir" "$sar_root"
  cp "$CBD_CAR" "$SIE_CAR" "$component_dir/"
  cp "$descriptor" "$sar_root/subsystem-descriptor.yaml"
  (cd "$sar_root" && zip -qr "$sar_file" subsystem-descriptor.yaml)

  if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
    echo "A server responds before the $profile profile starts; refusing to reuse it." >&2
    exit 1
  fi

  env \
    CNCF_SERVER_PORT="$CNCF_SERVER_PORT" \
    CNCF_HTTP_BASEURL="$CNCF_HTTP_BASEURL" \
    TEXTUS_SIE_RDF_DB="in-memory" \
    TEXTUS_SIE_VECTOR_DB="in-memory" \
    JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL -Dtextus.sie.rdf-db=in-memory -Dtextus.sie.vector-db=in-memory" \
    "$CNCF_BIN" \
      --runtime "$CNCF_VERSION" \
      server \
      --no-project-classpath \
      --component-dir "$component_dir" \
      --textus.subsystem=textus-cbd-sie >"$server_log" 2>&1 &
  server_pid=$!

  deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
  while ((SECONDS < deadline)); do
    if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
      server_ready=true
      break
    fi
    if ! kill -0 "$server_pid" >/dev/null 2>&1; then
      echo "The representative SAR server exited before $profile readiness." >&2
      show_server_log
      exit 1
    fi
    sleep 0.5
  done

  if [[ "$server_ready" != "true" ]]; then
    echo "Timed out waiting for the $profile SAR at $CNCF_HTTP_BASEURL." >&2
    show_server_log
    exit 1
  fi

  server_listener_pid="$(
    lsof -tiTCP:"$CNCF_SERVER_PORT" -sTCP:LISTEN 2>/dev/null || true
  )"
  server_listener_pid="${server_listener_pid%%$'\n'*}"
  if [[ ! "$server_listener_pid" =~ ^[0-9]+$ ]]; then
    echo "Could not identify the owned $profile listener process." >&2
    show_server_log
    exit 1
  fi

  if ! "$SCRIPT_DIR/probe-cbd-sie-sar.py" \
    --base-url "$CNCF_HTTP_BASEURL" \
    --profile "$profile"; then
    show_server_log
    exit 1
  fi
  if ! stop_server; then
    show_server_log
    exit 1
  fi
}

for index in "${!PROFILES[@]}"; do
  run_profile "${PROFILES[$index]}" "${PROFILE_DESCRIPTORS[$index]}"
done

echo "CBD_SIE_SAR_POLICY_MATRIX_OK"
