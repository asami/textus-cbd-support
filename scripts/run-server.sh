#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=cncf-common.sh
source "$SCRIPT_DIR/cncf-common.sh"

if [[ ! -s "$CNCF_RUNTIME_CLASSPATH_FILE" ]]; then
  echo "Runtime classpath is not prepared." >&2
  echo "Run: $SCRIPT_DIR/update-runtime-classpath.sh" >&2
  exit 1
fi

runtime_classpath="$(cat "$CNCF_RUNTIME_CLASSPATH_FILE")"

# CNCF resolves runtime configuration before dispatching the `server` command.
# Keep runtime options ahead of the command while preserving server-specific
# arguments (for example `--component-dir`) after it.
runtime_options=()
server_options=()
for argument in "$@"; do
  case "$argument" in
    --textus.*|--cncf.*)
      runtime_options+=("$argument")
      ;;
    *)
      server_options+=("$argument")
      ;;
  esac
done

# Bash treats an empty array expansion as unset under `set -u` on the macOS
# shell. The command construction below intentionally permits either array to
# be empty.
set +u

exec java \
  -Dcncf.server.port="$CNCF_SERVER_PORT" \
  -Dcncf.http.baseurl="$CNCF_HTTP_BASEURL" \
  -cp "$runtime_classpath" \
  "$CNCF_MAIN_CLASS" \
  "${CNCF_COMMON_ARGS[@]}" \
  "${runtime_options[@]}" \
  server \
  "${server_options[@]}"
