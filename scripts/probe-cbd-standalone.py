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
    "CbdSupport.CbdRetrieval.getReviewRun",
    "CbdSupport.CbdRetrieval.getReviewSummary",
    "CbdSupport.CbdRetrieval.getReviewReport",
    "CbdSupport.CbdRetrieval.listReviewFindings",
    "CbdSupport.CbdRetrieval.listReviewAssurances",
    "CbdSupport.CbdRetrieval.getReviewViews",
}
RUNTIME_TOOLS = {
    "tool.decimal.calculate",
    "tool.resource.read",
    "tool.time.now",
    "tool.web.fetch",
    "tool.web.head",
    "tool.web.search",
}
MCP_PROTOCOL_VERSION = "2025-11-25"
SIE_REFERENCE_TOOL = (
    "SemanticIntegrationEngine.SemanticRetrieval.searchComponentReferences"
)
COMPONENT_ARGUMENTS = {
    "name": "textus-runtime",
    "organization": "org.textus",
    "kind": "car",
    "version": "1.0.0",
    "catalogId": "fixture-catalog",
}


def _request(base_url: str, payload: dict, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}/mcp",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "MCP-Protocol-Version": MCP_PROTOCOL_VERSION,
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            if response.headers.get_content_type() != "application/json":
                raise RuntimeError(
                    "MCP returned unexpected content type: "
                    f"{response.headers.get_content_type()}"
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


def _call_tool(
    base_url: str,
    request_id: str,
    name: str,
    arguments: dict,
    timeout: float,
) -> dict:
    envelope = _request(
        base_url,
        {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments},
        },
        timeout,
    )
    _require("error" not in envelope, f"MCP {request_id} failed: {envelope}")
    content = envelope.get("result", {}).get("content", [])
    _require(
        len(content) == 1,
        f"MCP {request_id} returned unexpected content: {envelope}",
    )
    value = content[0].get("text")
    _require(isinstance(value, str), f"MCP {request_id} returned no text: {envelope}")
    return json.loads(value)


def _run(base_url: str, timeout: float) -> None:
    listed = _request(
        base_url,
        {
            "jsonrpc": "2.0",
            "id": "cbd-standalone-tools",
            "method": "tools/list",
            "params": {},
        },
        timeout,
    )
    _require("error" not in listed, f"MCP tools/list failed: {listed}")
    tool_names = [
        tool.get("name") for tool in listed.get("result", {}).get("tools", [])
    ]
    _require(
        tool_names == sorted(CBD_TOOLS | RUNTIME_TOOLS),
        f"Standalone MCP surface is not CBD retrieval plus the fixed runtime tools: {tool_names}",
    )

    catalogs = _call_tool(
        base_url,
        "cbd-standalone-catalogs",
        "CbdSupport.CbdRetrieval.listCatalogs",
        {},
        timeout,
    )
    sources = catalogs.get("sources", [])
    _require(
        any(
            source.get("id") == "fixture-catalog"
            and source.get("source_kind") == "published-catalog"
            for source in sources
        ),
        f"Standalone catalog source is missing: {catalogs}",
    )
    _require(
        all(source.get("source_kind") != "sie-bok" for source in sources),
        f"Standalone CBD unexpectedly loaded an SIE BoK source: {catalogs}",
    )

    detail = _call_tool(
        base_url,
        "cbd-standalone-detail",
        "CbdSupport.CbdRetrieval.getComponent",
        COMPONENT_ARGUMENTS,
        timeout,
    )
    component = detail.get("component", {})
    _require(
        detail.get("status") == "matched"
        and component.get("catalog_id") == "fixture-catalog"
        and component.get("name") == "textus-runtime"
        and component.get("selected_version") == "1.0.0",
        f"Standalone CBD detail did not resolve the fixture: {detail}",
    )

    usage = _call_tool(
        base_url,
        "cbd-standalone-usage",
        "CbdSupport.CbdRetrieval.getUsage",
        {**COMPONENT_ARGUMENTS, "intent": "inspect runtime execution"},
        timeout,
    )
    _require(usage.get("status") == "matched", f"Standalone usage failed: {usage}")
    _require(
        any(
            operation.get("service") == "RuntimeInspection"
            and operation.get("operation") == "inspectRuntime"
            for operation in usage.get("operations", [])
        ),
        f"Standalone usage omitted operation evidence: {usage}",
    )
    _require(
        usage.get("selected_source_id") == "fixture-catalog"
        and usage.get("selected_source_kind") == "published-catalog",
        f"Standalone usage lost catalog attribution: {usage}",
    )

    rejected = _request(
        base_url,
        {
            "jsonrpc": "2.0",
            "id": "cbd-standalone-sie-rejection",
            "method": "tools/call",
            "params": {"name": SIE_REFERENCE_TOOL, "arguments": {"query": "runtime"}},
        },
        timeout,
    )
    error = rejected.get("error")
    _require(
        isinstance(error, dict) and error.get("code") == -32602,
        f"Standalone SAR accepted an SIE tool: {rejected}",
    )

    print("mcp_tools=12 components=CbdSupport sie_components=0")
    print("information_sources=fixture-catalog sie_bok_sources=0")
    print("detail=textus-runtime:car@1.0.0 usage=RuntimeInspection.inspectRuntime")
    print("CBD_STANDALONE_INDEPENDENCE_OK")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify CBD Support operation without SIE storage or routes."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:19538")
    parser.add_argument("--timeout", type=float, default=30.0)
    arguments = parser.parse_args()

    try:
        _run(arguments.base_url.rstrip("/"), arguments.timeout)
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, RuntimeError) as error:
        print(f"CBD_STANDALONE_INDEPENDENCE_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
