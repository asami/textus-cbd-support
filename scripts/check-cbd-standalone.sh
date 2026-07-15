#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CNCF_BIN="${CNCF_BIN:-$(command -v cncf || true)}"
CNCF_VERSION_FILE="${CNCF_VERSION_FILE:-/Users/asami/src/dev2026/cncf-samples/versions/cncf-version.conf}"
CNCF_VERSION="${CNCF_VERSION:-$(tr -d '[:space:]' < "$CNCF_VERSION_FILE")}"
CNCF_RUNTIME_ARGS=(--runtime "$CNCF_VERSION")
RUNTIME_SOURCE="resolved-coordinate"
RUNTIME_REVISION="coordinate"
RUNTIME_WORKTREE_STATE="not-applicable"
if [[ -n "${CNCF_RUNTIME_DEV_DIR:-}" ]]; then
  CNCF_RUNTIME_ARGS+=(--runtime-dev-dir "$CNCF_RUNTIME_DEV_DIR")
  RUNTIME_SOURCE="development-directory"
  if RUNTIME_REVISION="$(git -C "$CNCF_RUNTIME_DEV_DIR" rev-parse --verify HEAD 2>/dev/null)"; then
    if RUNTIME_STATUS="$(git -C "$CNCF_RUNTIME_DEV_DIR" status --porcelain 2>/dev/null)"; then
      if [[ -n "$RUNTIME_STATUS" ]]; then
        RUNTIME_WORKTREE_STATE="dirty"
      else
        RUNTIME_WORKTREE_STATE="clean"
      fi
    else
      RUNTIME_WORKTREE_STATE="unknown"
    fi
  else
    RUNTIME_REVISION="unversioned"
    RUNTIME_WORKTREE_STATE="unversioned"
  fi
fi
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-19538}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
FIXTURE_PORT="${CBD_STANDALONE_FIXTURE_PORT:-19539}"
FIXTURE_BASEURL="${CBD_STANDALONE_FIXTURE_BASEURL:-http://127.0.0.1:$FIXTURE_PORT}"
STARTUP_TIMEOUT_SECONDS="${CBD_STANDALONE_STARTUP_TIMEOUT_SECONDS:-120}"
SHUTDOWN_TIMEOUT_SECONDS="${CBD_STANDALONE_SHUTDOWN_TIMEOUT_SECONDS:-30}"
CBD_CAR="$PROJECT_ROOT/target/textus-cbd-support-0.1.0-SNAPSHOT.car"
SAR_DESCRIPTOR="$PROJECT_ROOT/examples/cbd-standalone-sar/subsystem-descriptor.yaml"
FIXTURE_ROOT="$PROJECT_ROOT/examples/cbd-sie-sar/fixtures"

"$SCRIPT_DIR/check-runtime-compatibility.py" \
  --runtime "$CNCF_VERSION" \
  --evidence representative-sar

case "$CNCF_HTTP_BASEURL" in
  http://127.0.0.1:* | http://localhost:*) ;;
  *)
    echo "The standalone CBD check requires a loopback HTTP base URL: $CNCF_HTTP_BASEURL" >&2
    exit 1
    ;;
esac
case "$FIXTURE_BASEURL" in
  http://127.0.0.1:* | http://localhost:*) ;;
  *)
    echo "The standalone CBD fixture requires a loopback HTTP base URL: $FIXTURE_BASEURL" >&2
    exit 1
    ;;
esac

if [[ ! -x "$CNCF_BIN" ]]; then
  echo "CNCF launcher is missing: $CNCF_BIN" >&2
  exit 1
fi
for required_file in \
  "$SAR_DESCRIPTOR" \
  "$FIXTURE_ROOT/catalog/metadata/repository/car/index.json" \
  "$FIXTURE_ROOT/catalog/repository/catalog/car/textus-runtime.model-metadata.json"; do
  if [[ ! -f "$required_file" ]]; then
    echo "Standalone CBD input is missing: $required_file" >&2
    exit 1
  fi
done
if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
  echo "A server already responds at $CNCF_HTTP_BASEURL; refusing to reuse it." >&2
  exit 1
fi
if lsof -nP -iTCP:"$FIXTURE_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "The standalone CBD fixture port is already in use: $FIXTURE_PORT" >&2
  exit 1
fi

(cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)

runtime_dir="$(mktemp -d "${TMPDIR:-/tmp}/textus-cbd-standalone.XXXXXX")"
runtime_dir="$(cd "$runtime_dir" && pwd -P)"
component_dir="$runtime_dir/component.d"
sar_root="$runtime_dir/textus-cbd-standalone.sar.d"
sar_file="$component_dir/textus-cbd-standalone.sar"
local_car_root="$runtime_dir/local"
cache_car_root="$runtime_dir/cache"
empty_development_root="$runtime_dir/development"
server_log="$runtime_dir/server.log"
fixture_log="$runtime_dir/fixture.log"
server_pid=""
server_listener_pid=""
fixture_pid=""

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
      echo "Timed out waiting for the standalone CBD server to stop." >&2
      return 1
    fi
    sleep 0.25
  done
}

cleanup() {
  stop_server || true
  if [[ -n "$fixture_pid" ]] && kill -0 "$fixture_pid" >/dev/null 2>&1; then
    kill "$fixture_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "$fixture_pid" ]]; then
    wait "$fixture_pid" >/dev/null 2>&1 || true
  fi
  rm -rf "$runtime_dir"
}

show_server_log() {
  if [[ -s "$server_log" ]]; then
    echo "CNCF server log:" >&2
    tail -n 300 "$server_log" >&2
  fi
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mkdir -p \
  "$component_dir" \
  "$sar_root" \
  "$local_car_root/repository/car" \
  "$cache_car_root/car" \
  "$empty_development_root"
cp "$CBD_CAR" "$component_dir/"
cp "$SAR_DESCRIPTOR" "$sar_root/subsystem-descriptor.yaml"
(cd "$sar_root" && zip -qr "$sar_file" subsystem-descriptor.yaml)

component_archives="$(find "$component_dir" -maxdepth 1 -type f -name '*.car' -print)"
if [[ "$(printf '%s\n' "$component_archives" | sed '/^$/d' | wc -l | tr -d '[:space:]')" != "1" ]]; then
  echo "Standalone component directory must contain exactly one CAR." >&2
  exit 1
fi
if [[ "$component_archives" == *"semantic-integration-engine"* ]]; then
  echo "Standalone component directory unexpectedly contains SIE." >&2
  exit 1
fi
echo "component_archives=textus-cbd-support sie_archives=0"

python3 -m http.server "$FIXTURE_PORT" \
  --bind 127.0.0.1 \
  --directory "$FIXTURE_ROOT" >"$fixture_log" 2>&1 &
fixture_pid=$!

fixture_deadline=$((SECONDS + 10))
while ! curl -fsS "$FIXTURE_BASEURL/catalog/metadata/repository/car/index.json" >/dev/null 2>&1; do
  if ! kill -0 "$fixture_pid" >/dev/null 2>&1; then
    echo "The standalone CBD fixture server exited before readiness." >&2
    exit 1
  fi
  if ((SECONDS >= fixture_deadline)); then
    echo "Timed out waiting for the standalone CBD fixture server." >&2
    exit 1
  fi
  sleep 0.25
done

env \
  -u TEXTUS_CBD_SIE_BOK_ROUTES \
  -u TEXTUS_CBD_SIE_ALLOWED_ORIGINS \
  -u TEXTUS_CBD_BOK_SITES \
  -u TEXTUS_CBD_BOK_ALLOWED_ORIGINS \
  CNCF_SERVER_PORT="$CNCF_SERVER_PORT" \
  CNCF_HTTP_BASEURL="$CNCF_HTTP_BASEURL" \
  TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS="$FIXTURE_BASEURL" \
  TEXTUS_CBD_CATALOGS="fixture-catalog=$FIXTURE_BASEURL/catalog/" \
  TEXTUS_CBD_DEVELOPMENT_DIRECTORIES="empty=$empty_development_root" \
  TEXTUS_CBD_LOCAL_CAR_ROOT="$local_car_root" \
  TEXTUS_CBD_CACHE_CAR_ROOT="$cache_car_root" \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL" \
  "$CNCF_BIN" \
    "${CNCF_RUNTIME_ARGS[@]}" \
    server \
    --no-project-classpath \
    --component-dir "$component_dir" \
    --textus.subsystem=textus-cbd-standalone >"$server_log" 2>&1 &
server_pid=$!

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
server_ready=false
while ((SECONDS < deadline)); do
  if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
    server_ready=true
    break
  fi
  if ! kill -0 "$server_pid" >/dev/null 2>&1; then
    echo "The standalone CBD server exited before readiness." >&2
    show_server_log
    exit 1
  fi
  sleep 0.5
done
if [[ "$server_ready" != "true" ]]; then
  echo "Timed out waiting for standalone CBD at $CNCF_HTTP_BASEURL." >&2
  show_server_log
  exit 1
fi

server_listener_pid="$(lsof -tiTCP:"$CNCF_SERVER_PORT" -sTCP:LISTEN 2>/dev/null || true)"
server_listener_pid="${server_listener_pid%%$'\n'*}"
if [[ ! "$server_listener_pid" =~ ^[0-9]+$ ]]; then
  echo "Could not identify the owned standalone CBD listener process." >&2
  show_server_log
  exit 1
fi

if ! "$SCRIPT_DIR/probe-cbd-standalone.py" --base-url "$CNCF_HTTP_BASEURL"; then
  show_server_log
  exit 1
fi
if ! stop_server; then
  show_server_log
  exit 1
fi

echo "RUNTIME_COMPATIBILITY_EXECUTION_OK runtime=$CNCF_VERSION evidence=representative-sar source=$RUNTIME_SOURCE revision=$RUNTIME_REVISION worktree=$RUNTIME_WORKTREE_STATE"
echo "CBD_STANDALONE_SAR_OK"
