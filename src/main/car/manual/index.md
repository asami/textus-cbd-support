# Textus CBD Support Reference Manual

## Component Contract

`CbdSupport` reads external Cozy component catalogs and serves read-only CBD
information. It owns no catalog publication and does not write into SIE.

The component contains two services:

- `CbdRetrieval`: read-only, MCP-ready discovery and inspection.
- `CbdCatalogAdmin`: explicit refresh administration, not MCP ready.

## Configuration

`TEXTUS_CBD_CATALOGS` adds catalog base URIs to the default
`https://www.simplemodeling.org/` source. The syntax is:

```text
[id=]https://host/base/,[id=]https://other/base/
```

Every additional source also requires an exact allowed origin:

```text
TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS=https://host,https://other:8443
```

Origin authorization compares normalized scheme, host, and effective port.
Allowlist entries must be origins without paths. Catalog base URIs may contain
paths but must be absolute HTTP(S), must not contain user information, query,
or fragment, and must use an alphanumeric/dot/underscore/hyphen source ID.
Invalid, non-allowlisted, and duplicate-ID entries are rejected before network
access. The first accepted ID is retained, and every rejection is returned as
a warning without echoing credential-bearing input. The default
simplemodeling.org source is built-in and does not require allowlisting.

CNCF MCP publication can be narrowed with:

- `cncf.mcp.enabled=false`
- `cncf.mcp.disabled-services=CbdRetrieval`
- `cncf.mcp.disabled-operations=CbdRetrieval.getUsage`

## CbdRetrieval Operations

### searchComponents

Requires `requirement`. Optional `organization`, `kind`, `version`,
`runtimeVersion`, and `limit` fields constrain results. Returns typed
`ComponentMatch` records with `ComponentReference`, detailed profile, match
classification, score, rationale, and warnings. A runtime constraint requires
affirmative catalog runtime evidence; an absent runtime minimum does not match.
An explicit version projects that version's artifact, runtime, dependency, and
model-metadata evidence before compatibility filtering.

### getComponent

Requires exact `name`; optional organization, kind, version, and catalog ID
disambiguate. An explicit version returns only its version-specific detail; a
listed version without detail clears version-sensitive fields and adds a
warning. Returns `no-match` rather than fabricating a component.

### getUsage

Accepts the component identity fields, including optional CAR/SAR `kind`.
Returns the selected profile, service/operation summaries from model metadata,
and authoritative catalog, artifact, model-metadata, and documentation links.
An unavailable sidecar yields a warning and the remaining references.

### resolveDependencies

Accepts the component identity fields, including optional CAR/SAR `kind`, and
an optional `maxDepth` from 1 through 32 (default 8). The compatible
`dependencies` field remains the selected root's direct published evidence.
`resolutions` adds bounded transitive paths resolved only inside the selected
catalog. Each path is `resolved`, `unresolved`, `ambiguous`, or `cycle`.
`conflicts` reports distinct explicit versions requested for the same
dependency together with their evidence paths; CBD Support never chooses a
winner. `selectedVersion` and `dependencyMetadataVersion` are independent.
When an explicit requested version differs from the dependency metadata
version, the resolved edge remains visible but traversal stops; a root mismatch
also withholds the compatible direct `dependencies` field. Missing dependency
data is an empty evidence set, not an assertion of no dependencies.

### listCatalogs

Returns source identity, base URI, priority, readiness, component count,
cache status, refresh time, expiry time, latest refresh-attempt time, and
warning. `cacheStatus` is `fresh`, `stale`, `empty`, or `disabled`. Disabled
sources are included only when requested. The response-level `warnings` field
also reports rejected source configuration.

### status

Returns aggregate state and counts. `ready` means at least one current source
is ready; `degraded` means a source failed, a retained snapshot is stale, or
all initial loads failed; `not-started` means no enabled source has been
attempted.

## CbdCatalogAdmin Operation

### refreshCatalog

Refreshes one source ID or every enabled source. A failed refresh records the
failure and preserves any previous snapshot. The operation is excluded from
MCP because it changes runtime state and may cause external traffic.

Snapshots have a default finite TTL of 15 minutes and the runtime cache policy
rejects non-positive lifetimes or lifetimes over 24 hours. Retrieval readiness
reuses a fresh snapshot and automatically refreshes a missing or expired
snapshot. If automatic refresh fails, the stale last-known-good snapshot stays
available and the source becomes `degraded`.

## Catalog Contract

CBD Support first attempts the Cozy repository contract for each kind:

```text
metadata/repository/car/index.json
metadata/repository/sar/index.json
```

It follows `sidecars.model_metadata_json`, selected version artifact paths, and
the `runtime.cncf.minimum` field generated by Cozy. A catalog remains useful
when one kind index is absent and the other succeeds; the absent side becomes
a source warning. Model metadata is fetched only from the same origin as the
catalog evidence. A cross-origin sidecar URI remains visible as an unfetched
reference and produces a warning.

When those indexes are unavailable, the provider can consume the deployed
simplemodeling.org publication catalog at `en/catalog/index.html` and follow
its `cozy.publish-project.v1` project and repository-artifact JSON. This
compatibility contract supplies component identity, release version, CAR/SAR
artifact, and documentation evidence. It does not claim operation,
dependency, or runtime compatibility facts that the publication catalog does
not expose. Unreadable component entries degrade the source without hiding
successfully loaded entries. Snapshot project versions are never labeled as
`latestStable`.

## Failure and Limitation Semantics

- All enabled sources failing initial load causes retrieval operations to fail.
- Refresh failure with an existing snapshot returns degraded state and keeps
  serving the old snapshot.
- Cache expiry is inclusive: a snapshot is stale at `expiresAt`, and the next
  retrieval readiness check attempts refresh.
- Optional missing fields produce warnings where the contract can continue.
- Search is deterministic lexical metadata matching in Phase 1; it is not a
  claim of semantic compatibility.
- CBD Support does not install components, choose a winner for a transitive
  version conflict, or validate a target SAR composition.

## Example Requests

Search input:

```json
{"requirement":"account authentication","kind":"car","runtimeVersion":"0.5.1","limit":5}
```

Exact usage input:

```json
{"name":"textus-user-account","kind":"car","catalogId":"simplemodeling"}
```

The returned `ComponentReference` can be retained as evidence while the
detailed `ComponentProfile`, operations, dependencies, and references are used
for implementation planning.
