#!/usr/bin/env python3

import argparse
import json
import urllib.error
import urllib.parse
import urllib.request


def _request(request: urllib.request.Request, timeout: float) -> dict:
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            if response.headers.get_content_type() != "application/json":
                raise RuntimeError(
                    f"Unexpected content type from {request.full_url}: "
                    f"{response.headers.get_content_type()}"
                )
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"HTTP {error.code} from {request.full_url}: {detail}"
        ) from error


def _post_json(base_url: str, path: str, body: dict, timeout: float) -> dict:
    return _request(
        urllib.request.Request(
            f"{base_url}{path}",
            data=json.dumps(body).encode("utf-8"),
            headers={"Content-Type": "application/json"},
        ),
        timeout,
    )


def _post_form(base_url: str, path: str, body: dict, timeout: float) -> dict:
    return _request(
        urllib.request.Request(
            f"{base_url}{path}",
            data=urllib.parse.urlencode(body).encode("utf-8"),
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        ),
        timeout,
    )


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
    envelope = _post_json(
        base_url,
        "/mcp",
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


def _find_source(sources: list[dict], source_id: str) -> dict:
    matches = [source for source in sources if source.get("id") == source_id]
    _require(len(matches) == 1, f"Expected one {source_id} source: {sources}")
    return matches[0]


def _version_conflict(response: dict) -> dict:
    matches = [
        issue
        for issue in response.get("issues", [])
        if issue.get("code") == "version-conflict"
    ]
    _require(len(matches) == 1, f"Expected one version conflict: {response}")
    return matches[0]


def _search(base_url: str, request_id: str, limit: int, timeout: float) -> dict:
    return _call_tool(
        base_url,
        request_id,
        "CbdSupport.CbdRetrieval.searchComponents",
        {
            "requirement": "textus-runtime runtime",
            "organization": "org.textus",
            "kind": "car",
            "purpose": "published-reuse",
            "limit": limit,
        },
        timeout,
    )


def _run(
    base_url: str,
    fixture_url: str,
    sie_bok_base_uri: str,
    timeout: float,
) -> None:
    ingestion = _post_form(
        base_url,
        "/rest/v1/semantic-integration-engine/knowledge-store-admin/ingest-bok-knowledge-source",
        {
            "baseUri": sie_bok_base_uri,
            "registerKnowledgeSpace": "true",
            "includeKnowledgeFrame": "false",
        },
        timeout,
    )
    _require(
        ingestion.get("knowledge_space_state") == "ready",
        f"SIE fixture ingestion is not ready: {ingestion}",
    )
    _require(ingestion.get("term_count") == 1, f"Unexpected SIE term count: {ingestion}")

    full = _search(base_url, "p4-22-full", 10, timeout)
    _require(full.get("status") == "matched", f"Composed retrieval did not match: {full}")
    observations = full.get("observations", [])
    observation_sources = {item.get("source_id"): item for item in observations}
    _require(
        observation_sources.get("fixture-catalog", {}).get("source_kind")
        == "published-catalog",
        f"Catalog observation is missing or misowned: {full}",
    )
    _require(
        observation_sources.get("fixture-catalog", {}).get("version") == "1.0.0",
        f"Catalog version evidence is missing: {full}",
    )
    _require(
        observation_sources.get("working", {}).get("source_kind")
        == "development-directory",
        f"Development observation is missing or misowned: {full}",
    )
    _require(
        observation_sources.get("working", {}).get("version")
        == "1.1.0-SNAPSHOT",
        f"Development version evidence is missing: {full}",
    )

    semantic = full.get("semantic_evidence", [])
    _require(len(semantic) == 1, f"Expected one semantic citation: {full}")
    citation = semantic[0]
    _require(
        citation.get("source_id") == "semantic"
        and citation.get("source_kind") == "sie-bok"
        and citation.get("term_id") == "architecture:runtime",
        f"SIE semantic evidence is missing or misowned: {citation}",
    )
    _require(
        _is_descendant_uri(
            str(citation.get("evidence_uri", "")),
            sie_bok_base_uri,
        ),
        f"SIE evidence does not cite the fixture source: {citation}",
    )
    _require(
        "semantic" not in observation_sources,
        f"Semantic evidence was merged into component observations: {full}",
    )
    _require(
        full.get("selected_observation") is None,
        f"Composed retrieval selected a hidden source winner: {full}",
    )
    conflict = _version_conflict(full)
    _require(
        {"fixture-catalog", "working"}.issubset(set(conflict.get("source_ids", []))),
        f"Version conflict lost source participants: {conflict}",
    )

    before = _call_tool(
        base_url,
        "p4-22-sources-before",
        "CbdSupport.CbdRetrieval.listCatalogs",
        {},
        timeout,
    )
    missing_before = _find_source(before.get("sources", []), "missing-catalog")
    _require(
        missing_before.get("status") == "degraded"
        and missing_before.get("next_refresh_attempt_at") is not None,
        f"Missing catalog has no bounded retry state: {missing_before}",
    )
    _require(
        0 < len(missing_before.get("diagnostics", [])) <= 4,
        f"Missing catalog diagnostics are absent or unbounded: {missing_before}",
    )

    limited = _search(base_url, "p4-22-limited", 1, timeout)
    _require(len(limited.get("observations", [])) <= 1, f"Observation limit failed: {limited}")
    _require(len(limited.get("semantic_evidence", [])) <= 1, f"Semantic limit failed: {limited}")
    _require(limited.get("selected_observation") is None, f"Limited search selected a winner: {limited}")
    limited_conflict = _version_conflict(limited)
    _require(
        {"fixture-catalog", "working"}.issubset(
            set(limited_conflict.get("source_ids", []))
        ),
        f"Bounded response hid conflict participants: {limited_conflict}",
    )

    after = _call_tool(
        base_url,
        "p4-22-sources-after",
        "CbdSupport.CbdRetrieval.listCatalogs",
        {},
        timeout,
    )
    missing_after = _find_source(after.get("sources", []), "missing-catalog")
    _require(
        missing_after.get("last_refresh_attempt_at")
        == missing_before.get("last_refresh_attempt_at")
        and missing_after.get("next_refresh_attempt_at")
        == missing_before.get("next_refresh_attempt_at"),
        f"Immediate repeated search bypassed bounded retry: {missing_after}",
    )

    print(
        "source_owned_observations="
        f"catalog:{observation_sources['fixture-catalog']['version']},"
        f"working:{observation_sources['working']['version']} "
        f"semantic={citation['source_id']}:{citation['term_id']}"
    )
    print("bounded_observations=1 conflict_participants=fixture-catalog,working")
    print("CBD_SIE_SOURCE_AWARE_OK")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify live source ownership through the composed CBD and SIE SAR."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:19535")
    parser.add_argument("--fixture-url", default="http://127.0.0.1:19537")
    parser.add_argument("--sie-bok-base-uri", required=True)
    parser.add_argument("--timeout", type=float, default=30.0)
    arguments = parser.parse_args()

    try:
        _run(
            arguments.base_url.rstrip("/"),
            arguments.fixture_url.rstrip("/"),
            arguments.sie_bok_base_uri.rstrip("/") + "/",
            arguments.timeout,
        )
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, RuntimeError) as error:
        print(f"CBD_SIE_SOURCE_AWARE_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
