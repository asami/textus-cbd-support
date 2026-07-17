#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIE_ROOT="${TEXTUS_SIE_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-semantic-integration-engine}"
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
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-19535}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
FIXTURE_PORT="${CBD_SIE_SAR_FIXTURE_PORT:-19537}"
FIXTURE_BASEURL="${CBD_SIE_SAR_FIXTURE_BASEURL:-http://127.0.0.1:$FIXTURE_PORT}"
STARTUP_TIMEOUT_SECONDS="${CBD_SIE_SAR_STARTUP_TIMEOUT_SECONDS:-120}"
SHUTDOWN_TIMEOUT_SECONDS="${CBD_SIE_SAR_SHUTDOWN_TIMEOUT_SECONDS:-30}"
CBD_CAR="$PROJECT_ROOT/target/textus-cbd-support-0.1.0-SNAPSHOT.car"
SIE_CAR="$SIE_ROOT/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car"
SAR_DESCRIPTOR="$PROJECT_ROOT/examples/cbd-sie-sar/subsystem-descriptor.yaml"
PROFILE_DIR="$PROJECT_ROOT/examples/cbd-sie-sar/profiles"
FIXTURE_ROOT="$PROJECT_ROOT/examples/cbd-sie-sar/fixtures"
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
SELECTED_PROFILES=("${PROFILES[@]}")
SELECTED_PROFILE_DESCRIPTORS=("${PROFILE_DESCRIPTORS[@]}")

if (($# > 0)); then
  if (($# != 2)) || [[ "$1" != "--profile" ]]; then
    echo "Usage: $0 [--profile baseline|global-disabled|sie-service-disabled|operation-disabled]" >&2
    exit 2
  fi
  selected_profile="$2"
  selected_descriptor=""
  for index in "${!PROFILES[@]}"; do
    if [[ "${PROFILES[$index]}" == "$selected_profile" ]]; then
      selected_descriptor="${PROFILE_DESCRIPTORS[$index]}"
      break
    fi
  done
  if [[ -z "$selected_descriptor" ]]; then
    echo "Unknown representative SAR profile: $selected_profile" >&2
    exit 2
  fi
  SELECTED_PROFILES=("$selected_profile")
  SELECTED_PROFILE_DESCRIPTORS=("$selected_descriptor")
fi

"$SCRIPT_DIR/check-runtime-compatibility.py" \
  --runtime "$CNCF_VERSION" \
  --evidence representative-sar

case "$CNCF_HTTP_BASEURL" in
  http://127.0.0.1:* | http://localhost:*) ;;
  *)
    echo "The representative SAR check requires a loopback HTTP base URL: $CNCF_HTTP_BASEURL" >&2
    exit 1
    ;;
esac
case "$FIXTURE_BASEURL" in
  http://127.0.0.1:* | http://localhost:*) ;;
  *)
    echo "The representative SAR fixture requires a loopback HTTP base URL: $FIXTURE_BASEURL" >&2
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
for descriptor in "${SELECTED_PROFILE_DESCRIPTORS[@]}"; do
  if [[ ! -f "$descriptor" ]]; then
    echo "Representative SAR descriptor is missing: $descriptor" >&2
    exit 1
  fi
done
for fixture in \
  "$FIXTURE_ROOT/catalog/metadata/repository/car/index.json" \
  "$FIXTURE_ROOT/development/project.yaml" \
  "$FIXTURE_ROOT/bok/metadata/cncf/knowledge-source.json" \
  "$FIXTURE_ROOT/bok/metadata/glossary/terms.json" \
  "$FIXTURE_ROOT/bok/metadata/repository/car/index.json" \
  "$FIXTURE_ROOT/catalog/repository/catalog/car/textus-runtime.model-metadata.json"; do
  if [[ ! -f "$fixture" ]]; then
    echo "Representative SAR fixture is missing: $fixture" >&2
    exit 1
  fi
done
if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
  echo "A server already responds at $CNCF_HTTP_BASEURL; refusing to reuse an unowned process." >&2
  exit 1
fi
if lsof -nP -iTCP:"$FIXTURE_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "The representative SAR fixture port is already in use: $FIXTURE_PORT" >&2
  exit 1
fi

(cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)
(cd "$SIE_ROOT" && sbt --batch cozyBuildCAR)

if ! sie_dependency_manifest="$(unzip -p "$SIE_CAR" component-dependencies.yaml 2>/dev/null)"; then
  echo "The SIE CAR is missing component-dependencies.yaml." >&2
  exit 1
fi
if [[ "$sie_dependency_manifest" != *$'  local:\n    - "org.jsoup:jsoup:1.18.1"'* ]]; then
  echo "The SIE CAR does not declare its component-local jsoup dependency." >&2
  exit 1
fi
if ! sie_archive_entries="$(jar tf "$SIE_CAR")"; then
  echo "The SIE CAR archive listing could not be read." >&2
  exit 1
fi
sie_bundled_jsoup=""
while IFS= read -r entry; do
  if [[ "$entry" =~ ^lib/jsoup-[^/]*\.jar$ ]]; then
    sie_bundled_jsoup="$entry"
  fi
done <<< "$sie_archive_entries"
if [[ -n "$sie_bundled_jsoup" ]]; then
  echo "The SIE CAR bundles jsoup instead of resolving its declared local dependency." >&2
  exit 1
fi
echo "SIE_CAR_LOCAL_DEPENDENCY_OK coordinate=org.jsoup:jsoup:1.18.1 bundled=false"

runtime_dir="$(mktemp -d "${TMPDIR:-/tmp}/textus-cbd-sie-sar.XXXXXX")"
runtime_dir="$(cd "$runtime_dir" && pwd -P)"
server_pid=""
server_listener_pid=""
server_log=""
fixture_pid=""
fixture_log="$runtime_dir/fixture.log"

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

python3 -m http.server "$FIXTURE_PORT" \
  --bind 127.0.0.1 \
  --directory "$FIXTURE_ROOT" >"$fixture_log" 2>&1 &
fixture_pid=$!

fixture_deadline=$((SECONDS + 10))
while ! curl -fsS "$FIXTURE_BASEURL/bok/metadata/cncf/knowledge-source.json" >/dev/null 2>&1; do
  if ! kill -0 "$fixture_pid" >/dev/null 2>&1; then
    echo "The representative SAR fixture server exited before readiness." >&2
    exit 1
  fi
  if ((SECONDS >= fixture_deadline)); then
    echo "Timed out waiting for the representative SAR fixture server." >&2
    exit 1
  fi
  sleep 0.25
done

run_profile() {
  local profile="$1"
  local descriptor="$2"
  local profile_root="$runtime_dir/$profile"
  local component_dir="$profile_root/component.d"
  local sar_root="$profile_root/textus-cbd-sie.sar.d"
  local sar_file="$component_dir/textus-cbd-sie.sar"
  local local_car_root="$profile_root/local"
  local cache_car_root="$profile_root/cache"
  local deadline
  local server_ready=false

  server_log="$profile_root/server.log"
  mkdir -p \
    "$component_dir" \
    "$sar_root" \
    "$local_car_root/repository/car" \
    "$cache_car_root/car"
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
    TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS="$FIXTURE_BASEURL" \
    TEXTUS_CBD_CATALOGS="fixture-catalog=$FIXTURE_BASEURL/catalog/,missing-catalog=$FIXTURE_BASEURL/missing/" \
    TEXTUS_CBD_DEVELOPMENT_DIRECTORIES="working=$FIXTURE_ROOT/development" \
    TEXTUS_CBD_LOCAL_CAR_ROOT="$local_car_root" \
    TEXTUS_CBD_CACHE_CAR_ROOT="$cache_car_root" \
    TEXTUS_CBD_SIE_ALLOWED_ORIGINS="$CNCF_HTTP_BASEURL" \
    TEXTUS_CBD_SIE_BOK_ROUTES="semantic=$CNCF_HTTP_BASEURL/mcp" \
    JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL -Dtextus.sie.rdf-db=in-memory -Dtextus.sie.vector-db=in-memory" \
    "$CNCF_BIN" \
      "${CNCF_RUNTIME_ARGS[@]}" \
      "--textus.resource.url.file.roots=$FIXTURE_ROOT" \
      "--textus.subsystem=textus-cbd-sie" \
      server \
      --no-project-classpath \
      --component-dir "$component_dir" >"$server_log" 2>&1 &
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
  if [[ "$profile" == "baseline" ]] && ! "$SCRIPT_DIR/probe-cbd-sie-source-aware.py" \
    --base-url "$CNCF_HTTP_BASEURL" \
    --fixture-url "$FIXTURE_BASEURL" \
    --sie-bok-base-uri "$(cd "$FIXTURE_ROOT/bok" && pwd -P | sed 's#^#file://#')/"; then
    show_server_log
    exit 1
  fi
  if [[ "$profile" == "baseline" ]] && ! "$SCRIPT_DIR/probe-cbd-sie-design-flow.py" \
    --base-url "$CNCF_HTTP_BASEURL" \
    --sie-bok-base-uri "$(cd "$FIXTURE_ROOT/bok" && pwd -P | sed 's#^#file://#')/"; then
    show_server_log
    exit 1
  fi
  if ! stop_server; then
    show_server_log
    exit 1
  fi
}

for index in "${!SELECTED_PROFILES[@]}"; do
  run_profile "${SELECTED_PROFILES[$index]}" "${SELECTED_PROFILE_DESCRIPTORS[$index]}"
done

if ((${#SELECTED_PROFILES[@]} == ${#PROFILES[@]})); then
  echo "CBD_SIE_SAR_POLICY_MATRIX_OK"
else
  echo "CBD_SIE_SAR_PROFILE_OK profile=${SELECTED_PROFILES[0]}"
fi
echo "RUNTIME_COMPATIBILITY_EXECUTION_OK runtime=$CNCF_VERSION evidence=representative-sar source=$RUNTIME_SOURCE revision=$RUNTIME_REVISION worktree=$RUNTIME_WORKTREE_STATE"
