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
CBD_CAR="$PROJECT_ROOT/target/textus-cbd-support-0.1.0-SNAPSHOT.car"
SIE_CAR="$SIE_ROOT/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car"
SAR_DESCRIPTOR="$PROJECT_ROOT/examples/cbd-sie-sar/subsystem-descriptor.yaml"

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
if [[ ! -f "$SAR_DESCRIPTOR" ]]; then
  echo "Representative SAR descriptor is missing: $SAR_DESCRIPTOR" >&2
  exit 1
fi
if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
  echo "A server already responds at $CNCF_HTTP_BASEURL; refusing to reuse an unowned process." >&2
  exit 1
fi

(cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)
(cd "$SIE_ROOT" && sbt --batch cozyBuildCAR)

runtime_dir="$(mktemp -d "${TMPDIR:-/tmp}/textus-cbd-sie-sar.XXXXXX")"
component_dir="$runtime_dir/component.d"
sar_root="$runtime_dir/textus-cbd-sie.sar.d"
sar_file="$component_dir/textus-cbd-sie.sar"
server_log="$runtime_dir/server.log"
server_pid=""

cleanup() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" >/dev/null 2>&1; then
    kill "$server_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "$server_pid" ]]; then
    wait "$server_pid" >/dev/null 2>&1 || true
  fi
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

mkdir -p "$component_dir" "$sar_root"
cp "$CBD_CAR" "$SIE_CAR" "$component_dir/"
cp "$SAR_DESCRIPTOR" "$sar_root/subsystem-descriptor.yaml"
(cd "$sar_root" && zip -qr "$sar_file" subsystem-descriptor.yaml)

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
server_ready=false
while ((SECONDS < deadline)); do
  if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
    server_ready=true
    break
  fi
  if ! kill -0 "$server_pid" >/dev/null 2>&1; then
    echo "The representative SAR server exited before readiness." >&2
    show_server_log
    exit 1
  fi
  sleep 0.5
done

if [[ "$server_ready" != "true" ]]; then
  echo "Timed out waiting for the representative SAR at $CNCF_HTTP_BASEURL." >&2
  show_server_log
  exit 1
fi

if ! "$SCRIPT_DIR/probe-cbd-sie-sar.py" --base-url "$CNCF_HTTP_BASEURL"; then
  show_server_log
  exit 1
fi

echo "CBD_SIE_SAR_LIVE_CHECK_OK"
