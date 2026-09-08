#!/usr/bin/env bash
set -euo pipefail

ENDPOINT="${CBD_SUPPORT_MCP_ENDPOINT:-http://127.0.0.1:19534/mcp}"
MCP_PROTOCOL_VERSION="2025-11-25"
CONNECT_TIMEOUT_SECONDS="${CBD_SUPPORT_MCP_CONNECT_TIMEOUT_SECONDS:-5}"
TOOL_TIMEOUT_SECONDS="${CBD_SUPPORT_MCP_TOOL_TIMEOUT_SECONDS:-60}"
MAX_RESPONSE_BYTES="${CBD_SUPPORT_MCP_MAX_RESPONSE_BYTES:-16777216}"

require_bounded_positive_integer() {
  local name="$1"
  local value="$2"
  local maximum="$3"

  if [[ ! "$value" =~ ^[1-9][0-9]{0,8}$ ]] || (( value > maximum )); then
    echo "$name must be a positive integer no greater than $maximum: $value" >&2
    exit 2
  fi
}

require_bounded_positive_integer \
  CBD_SUPPORT_MCP_CONNECT_TIMEOUT_SECONDS "$CONNECT_TIMEOUT_SECONDS" 300
require_bounded_positive_integer \
  CBD_SUPPORT_MCP_TOOL_TIMEOUT_SECONDS "$TOOL_TIMEOUT_SECONDS" 3600
require_bounded_positive_integer \
  CBD_SUPPORT_MCP_MAX_RESPONSE_BYTES "$MAX_RESPONSE_BYTES" 67108864

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <tool-name> [json-arguments]" >&2
  echo "Example: $0 CbdSupport.CbdRetrieval.status '{\"detail\":\"summary\"}'" >&2
  exit 2
fi

tool_name="$1"
if [[ $# -ge 2 ]]; then
  arguments="$2"
else
  arguments="{}"
fi

payload="$(
  python3 - "$tool_name" "$arguments" <<'PY'
import json
import sys

tool_name, arguments_json = sys.argv[1], sys.argv[2]

try:
    arguments = json.loads(arguments_json)
except json.JSONDecodeError as exc:
    raise SystemExit(f"Invalid JSON arguments: {exc}") from exc

payload = {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
        "name": tool_name,
        "arguments": arguments,
    },
}

print(json.dumps(payload))
PY
)"

response_file="$(mktemp "${TMPDIR:-/tmp}/textus-cbd-mcp-response.XXXXXX")"
cleanup() {
  rm -f "$response_file"
}
trap cleanup EXIT

if ! curl --fail-with-body -sS \
    --connect-timeout "$CONNECT_TIMEOUT_SECONDS" \
    --max-time "$TOOL_TIMEOUT_SECONDS" \
    --max-filesize "$MAX_RESPONSE_BYTES" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -H "MCP-Protocol-Version: $MCP_PROTOCOL_VERSION" \
    -d "$payload" \
    --output "$response_file" \
    "$ENDPOINT"
then
  if [[ -s "$response_file" ]]; then
    cat "$response_file" >&2
  fi
  exit 1
fi

python3 - "$response_file" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as stream:
    document = json.load(stream)

if "error" in document:
    print(json.dumps(document["error"], ensure_ascii=False, indent=2))
    raise SystemExit(1)

result = document.get("result", {})
content = result.get("content", [])
if result.get("isError") is True:
    structured = result.get("structuredContent")
    if structured is not None:
        print(json.dumps(structured, ensure_ascii=False, indent=2))
    elif content:
        print(json.dumps(content, ensure_ascii=False, indent=2))
    else:
        print("MCP tool call failed without error content", file=sys.stderr)
    raise SystemExit(1)

if len(content) == 1 and content[0].get("type") == "text":
    text = content[0].get("text", "")
    try:
        print(json.dumps(json.loads(text), ensure_ascii=False, indent=2))
    except json.JSONDecodeError:
        print(text)
else:
    print(json.dumps(result or document, ensure_ascii=False, indent=2))
PY
