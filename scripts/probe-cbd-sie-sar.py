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


def _run(base_url: str, timeout: float) -> None:
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
    _require(
        tool_names == EXPECTED_TOOLS,
        f"Unexpected composed MCP tool catalog: {sorted(tool_names)}",
    )
    _require(
        all(isinstance(tool.get("inputSchema"), dict) for tool in tools),
        f"One or more composed MCP tools omit inputSchema: {tools}",
    )

    print(f"endpoint={base_url}/mcp protocol=json-rpc")
    print(f"cbd_read_tools={len(CBD_TOOLS)} sie_read_tools={len(SIE_TOOLS)}")
    print("administration_tools=0 unexpected_tools=0")
    print("CBD_SIE_SAR_MCP_OK")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify the representative CBD Support and SIE SAR MCP surface."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:19535")
    parser.add_argument("--timeout", type=float, default=10.0)
    arguments = parser.parse_args()

    try:
        _run(arguments.base_url.rstrip("/"), arguments.timeout)
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, RuntimeError) as error:
        print(f"CBD_SIE_SAR_MCP_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
