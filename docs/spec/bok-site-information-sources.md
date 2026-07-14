# BoK Site Information Source Contract

## Supported Publication Contract

An additional BoK site is consumed only through Cozy's machine-readable
KnowledgeSource contract:

```text
<site-base>/metadata/cncf/knowledge-source.json
schemaVersion = cncf.knowledge-source.v1
kind = bok-site
```

CBD Support does not scrape rendered HTML, probe alternate well-known paths, or
guess a glossary location when this manifest is absent or incompatible. The
first semantic resource supported in this slice is a manifest-declared
`glossary-terms` resource using `application/json` and the existing top-level
`{"terms": [...]}` contract. Other valid resource kinds remain visible as
manifest evidence but are not interpreted by this adapter.

## Configuration and Authorization

`TEXTUS_CBD_BOK_SITES` accepts comma-separated `[id=]base-uri` entries.
`TEXTUS_CBD_BOK_ALLOWED_ORIGINS` contains the exact HTTP(S) origins permitted
for those entries. A candidate is rejected before fetch when its ID is invalid,
reserved, or duplicated, its URI contains credentials/query/fragment, or its
normalized origin is not explicitly allowlisted. Configuration source and
origin counts are bounded. Runtime loading reserves every configured catalog
ID plus the stable built-in/local/cache IDs before admitting BoK sources, so
the unified runtime never contains two source descriptors with the same ID.

The configured source ID remains the CBD runtime identity. The manifest `id`
and `sourceRef.value` remain publisher evidence and never silently replace or
merge with the configured identity. A difference between configured and
publisher identity is diagnostic, not an automatic winner. Within the v1
publisher contract, `sourceRef.kind` must be `bok-site`, `sourceRef.value` is
required, and manifest `id` and `sourceRef.value` must agree.

## Resource Boundary

Manifest resource references must be safe relative paths below the configured
site base. Absolute, cross-origin, root-relative, traversal, query-bearing, and
fragment-bearing references are rejected before fetch. A `sourceRef.uri` is
retained only when it remains on the configured origin and never expands fetch
authority.

The fetch contract receives a byte limit for each request, and the CNCF HTTP
adapter rejects an oversized response before it reaches manifest/resource
parsing or runtime state. Manifest bytes, resource bytes, resource count, and
term count are bounded. Truncation,
malformed documents, incompatible schemas, rejected resources, and failed
resource fetches remain observable through bounded, credential-redacted
diagnostics. A valid manifest can produce a partial
snapshot when one declared glossary resource fails; cache and last-known-good
policy are later runtime-integration work.

## Term Evidence

Each glossary observation retains configured source ID, publisher manifest ID,
term identity, selected SmartDox/Cozy fields, and an exact JSON-pointer-like
resource location. Duplicate term identities are preserved with diagnostics;
the adapter does not choose or synthesize a winner. Requirement matching and
MCP projection are later Phase 3 slices and must cite this BoK evidence
separately from CBD catalog or local CAR facts.

`CbdRuntime.create` loads the authorized BoK configuration alongside catalog
configuration. Read-only retrieval initialization refreshes catalog and BoK
inputs through the CNCF HTTP provider boundary, retains BoK snapshots and term
evidence, and includes BoK readiness and diagnostics in the unified information
source state. BoK resource failure degrades that source without converting its
terms into catalog component facts.

## Executable Evidence

`BokSourceRuntimeSpec` covers explicit origin authorization, sanitized
rejection, fixed-path v1 manifest ingestion, identity separation, relative
resource safety, byte/resource/term bounds, incompatible-schema failure, and
duplicate preservation without rendered-page scraping.
