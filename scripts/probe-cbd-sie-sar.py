#!/usr/bin/env python3

import argparse
import json
import urllib.error
import urllib.request


CBD_TOOLS = {
    "CbdSupport.CbdRetrieval.searchComponents",
    "CbdSupport.CbdRetrieval.getComponent",
    "CbdSupport.CbdRetrieval.getUsage",
    "CbdSupport.CbdRetrieval.resolveDependencies",
    "CbdSupport.CbdRetrieval.listCatalogs",
    "CbdSupport.CbdRetrieval.status",
}
SIE_TOOLS = {
    "SemanticIntegrationEngine.SemanticRetrieval.query",
    "SemanticIntegrationEngine.SemanticRetrieval.explain",
    "SemanticIntegrationEngine.SemanticRetrieval.status",
    "SemanticIntegrationEngine.SemanticRetrieval.searchTerms",
    "SemanticIntegrationEngine.SemanticRetrieval.explainTerm",
    "SemanticIntegrationEngine.SemanticRetrieval.searchComponentReferences",
    "SemanticIntegrationEngine.SemanticRetrieval.getComponentReference",
}
EXPECTED_TOOLS = CBD_TOOLS | SIE_TOOLS
EXPECTED_TOOLS_BY_PROFILE = {
    "baseline": EXPECTED_TOOLS,
    "global-disabled": set(),
    "sie-service-disabled": CBD_TOOLS,
    "operation-disabled": EXPECTED_TOOLS
    - {
        "CbdSupport.CbdRetrieval.status",
        "SemanticIntegrationEngine.SemanticRetrieval.status",
    },
}
REJECTED_TOOLS_BY_PROFILE = {
    "baseline": set(),
    "global-disabled": {
        "CbdSupport.CbdRetrieval.status",
        "SemanticIntegrationEngine.SemanticRetrieval.status",
    },
    "sie-service-disabled": {"SemanticIntegrationEngine.SemanticRetrieval.status"},
    "operation-disabled": {
        "CbdSupport.CbdRetrieval.status",
        "SemanticIntegrationEngine.SemanticRetrieval.status",
    },
}


def _request(base_url: str, payload: dict, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}/mcp",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            if response.headers.get_content_type() != "application/json":
                raise RuntimeError(
                    f"MCP returned unexpected content type: {response.headers.get_content_type()}"
                )
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"HTTP {error.code} from {request.full_url}: {detail}"
        ) from error


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def _require_rejected_tool(
    base_url: str, tool_name: str, profile: str, timeout: float
) -> None:
    called = _request(
        base_url,
        {
            "jsonrpc": "2.0",
            "id": f"cbd-sie-sar-{profile}-{tool_name}",
            "method": "tools/call",
            "params": {"name": tool_name, "arguments": {}},
        },
        timeout,
    )
    error = called.get("error")
    _require(
        isinstance(error, dict) and error.get("code") == -32602,
        f"Disabled tool call was not rejected as invalid params: {called}",
    )


def _run(base_url: str, profile: str, timeout: float) -> None:
    listed = _request(
        base_url,
        {
            "jsonrpc": "2.0",
            "id": "cbd-sie-sar-tools",
            "method": "tools/list",
            "params": {},
        },
        timeout,
    )
    _require("error" not in listed, f"MCP tools/list failed: {listed}")
    tools = listed.get("result", {}).get("tools", [])
    tool_names = {tool.get("name") for tool in tools}
    expected_tools = EXPECTED_TOOLS_BY_PROFILE[profile]
    _require(
        tool_names == expected_tools,
        f"Unexpected composed MCP tool catalog for {profile}: {sorted(tool_names)}",
    )
    _require(
        all(isinstance(tool.get("inputSchema"), dict) for tool in tools),
        f"One or more composed MCP tools omit inputSchema: {tools}",
    )

    for tool_name in sorted(REJECTED_TOOLS_BY_PROFILE[profile]):
        _require_rejected_tool(base_url, tool_name, profile, timeout)

    cbd_count = len(tool_names & CBD_TOOLS)
    sie_count = len(tool_names & SIE_TOOLS)
    print(f"profile={profile} endpoint={base_url}/mcp protocol=json-rpc")
    print(f"cbd_read_tools={cbd_count} sie_read_tools={sie_count}")
    print("administration_tools=0 unexpected_tools=0")
    print("CBD_SIE_SAR_MCP_OK")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify the representative CBD Support and SIE SAR MCP surface."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:19535")
    parser.add_argument(
        "--profile",
        choices=sorted(EXPECTED_TOOLS_BY_PROFILE),
        default="baseline",
    )
    parser.add_argument("--timeout", type=float, default=10.0)
    arguments = parser.parse_args()

    try:
        _run(arguments.base_url.rstrip("/"), arguments.profile, arguments.timeout)
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, RuntimeError) as error:
        print(f"CBD_SIE_SAR_MCP_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
