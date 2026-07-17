#!/usr/bin/env python3

import argparse
import json
import urllib.error
import urllib.parse
import urllib.request


TERM_ID = "architecture:runtime"
COMPONENT_NAME = "textus-runtime"
COMPONENT_KIND = "car"
COMPONENT_VERSION = "1.0.0"
COMPONENT_ORGANIZATION = "org.textus"
SIE_REFERENCE_FIELDS = {
    "source_id",
    "catalog_id",
    "organization",
    "name",
    "title",
    "kind",
    "version",
    "evidence_uri",
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
                    f"MCP returned unexpected content type: "
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


def _is_descendant_uri(uri: str, base_uri: str) -> bool:
    value = urllib.parse.urlparse(uri)
    base = urllib.parse.urlparse(base_uri)
    base_path = base.path.rstrip("/") + "/"
    return value.scheme == base.scheme and value.path.startswith(base_path)


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
    _require(len(content) == 1, f"MCP {request_id} returned unexpected content: {envelope}")
    text = content[0].get("text")
    _require(isinstance(text, str), f"MCP {request_id} returned no text: {envelope}")
    return json.loads(text)


def _matching_result(response: dict, identity: str) -> dict:
    matches = [item for item in response.get("results", []) if item.get("id") == identity]
    _require(len(matches) == 1, f"Expected one {identity} result: {response}")
    return matches[0]


def _run(base_url: str, sie_bok_base_uri: str, timeout: float) -> None:
    term_search = _call_tool(
        base_url,
        "p5-23-term-search",
        "SemanticIntegrationEngine.SemanticRetrieval.searchTerms",
        {"query": "Execution Runtime", "limit": 10},
        timeout,
    )
    term = _matching_result(term_search, TERM_ID)
    _require(term_search.get("status") == "matched", f"Term search did not match: {term_search}")
    _require(term.get("match_kind") == "exact", f"Term search was not exact: {term}")
    _require(
        _is_descendant_uri(
            str(term.get("evidence_uri", "")),
            sie_bok_base_uri,
        ),
        f"Term search lost BoK evidence: {term}",
    )

    term_explanation = _call_tool(
        base_url,
        "p5-23-term-explain",
        "SemanticIntegrationEngine.SemanticRetrieval.explainTerm",
        {"term": "Execution Runtime"},
        timeout,
    )
    explained_term = _matching_result(term_explanation, TERM_ID)
    _require(
        term_explanation.get("status") == "matched"
        and bool(explained_term.get("definition")),
        f"Term explanation did not ground the definition: {term_explanation}",
    )
    reference_query = term.get("title")
    _require(
        isinstance(reference_query, str) and bool(reference_query.strip()),
        f"Grounded term supplied no component-search query: {term}",
    )

    reference_search = _call_tool(
        base_url,
        "p5-23-reference-search",
        "SemanticIntegrationEngine.SemanticRetrieval.searchComponentReferences",
        {"query": reference_query, "kind": COMPONENT_KIND, "limit": 10},
        timeout,
    )
    reference_matches = reference_search.get("results", [])
    _require(
        reference_search.get("status") == "matched" and len(reference_matches) == 1,
        f"Component reference search was not exact and unique: {reference_search}",
    )
    matched = reference_matches[0]
    reference = matched.get("reference", {})
    _require(
        matched.get("match_kind") == "candidate" and bool(matched.get("rationale")),
        f"Grounded-term reference discovery was not an evidenced candidate: {matched}",
    )
    _require(
        reference.get("name") == COMPONENT_NAME
        and reference.get("kind") == COMPONENT_KIND
        and reference.get("version") == COMPONENT_VERSION
        and reference.get("organization") == COMPONENT_ORGANIZATION,
        f"Reference search returned the wrong identity: {reference_search}",
    )
    _require(
        _is_descendant_uri(
            str(reference.get("evidence_uri", "")),
            sie_bok_base_uri,
        ),
        f"Reference search lost BoK evidence: {reference}",
    )
    _require(
        set(reference).issubset(SIE_REFERENCE_FIELDS),
        f"SIE reference leaked fields outside the handoff contract: {reference}",
    )

    lookup_arguments = {
        "name": reference["name"],
        "kind": reference["kind"],
        "version": reference["version"],
    }
    exact_reference = _call_tool(
        base_url,
        "p5-23-reference-lookup",
        "SemanticIntegrationEngine.SemanticRetrieval.getComponentReference",
        lookup_arguments,
        timeout,
    )
    looked_up = exact_reference.get("reference", {})
    _require(
        exact_reference.get("status") == "matched"
        and looked_up.get("name") == reference.get("name")
        and looked_up.get("kind") == reference.get("kind")
        and looked_up.get("version") == reference.get("version"),
        f"Exact SIE reference lookup did not preserve the handoff: {exact_reference}",
    )
    _require(
        set(looked_up).issubset(SIE_REFERENCE_FIELDS),
        f"Exact SIE lookup leaked fields outside the handoff contract: {looked_up}",
    )

    cbd_arguments = {
        **lookup_arguments,
        "organization": reference["organization"],
    }
    detail = _call_tool(
        base_url,
        "p5-23-component-detail",
        "CbdSupport.CbdRetrieval.getComponent",
        cbd_arguments,
        timeout,
    )
    component = detail.get("component", {})
    _require(detail.get("status") == "matched", f"CBD detail did not match: {detail}")
    _require(
        component.get("catalog_id") == "fixture-catalog"
        and component.get("name") == COMPONENT_NAME
        and component.get("kind") == COMPONENT_KIND
        and component.get("selected_version") == COMPONENT_VERSION,
        f"CBD detail did not resolve the exact catalog component: {detail}",
    )

    usage = _call_tool(
        base_url,
        "p5-23-component-usage",
        "CbdSupport.CbdRetrieval.getUsage",
        {**cbd_arguments, "intent": "inspect runtime execution"},
        timeout,
    )
    operations = usage.get("operations", [])
    guidance = usage.get("guidance", [])
    _require(usage.get("status") == "matched", f"CBD usage did not match: {usage}")
    _require(
        any(
            operation.get("service") == "RuntimeInspection"
            and operation.get("operation") == "inspectRuntime"
            for operation in operations
        ),
        f"CBD usage omitted observed operation evidence: {usage}",
    )
    _require(
        {item.get("statement_kind") for item in guidance}
        >= {"observed-fact", "deterministic-inference"},
        f"CBD usage omitted attributable intent guidance: {usage}",
    )
    _require(
        usage.get("selected_source_id") == "fixture-catalog"
        and usage.get("selected_version") == COMPONENT_VERSION,
        f"CBD usage lost selected-source attribution: {usage}",
    )

    print(
        f"term_grounding={TERM_ID} match=exact evidence=bok "
        f"reference_query={reference_query}"
    )
    print(
        f"component_handoff={COMPONENT_ORGANIZATION}:{COMPONENT_NAME}:"
        f"{COMPONENT_KIND}@{COMPONENT_VERSION} sie_detail=excluded"
    )
    print(
        "cbd_detail=fixture-catalog "
        "usage=RuntimeInspection.inspectRuntime guidance=attributable"
    )
    print("CBD_SIE_DESIGN_FLOW_OK")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify a live SIE-to-CBD component design handoff."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:19535")
    parser.add_argument("--sie-bok-base-uri", required=True)
    parser.add_argument("--timeout", type=float, default=30.0)
    arguments = parser.parse_args()

    try:
        _run(
            arguments.base_url.rstrip("/"),
            arguments.sie_bok_base_uri.rstrip("/") + "/",
            arguments.timeout,
        )
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, RuntimeError) as error:
        print(f"CBD_SIE_DESIGN_FLOW_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
