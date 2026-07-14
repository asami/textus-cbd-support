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

Additional BoK sites use a separate exact-origin boundary:

```text
TEXTUS_CBD_BOK_SITES=[id=]https://host/bok/
TEXTUS_CBD_BOK_ALLOWED_ORIGINS=https://host
```

CBD Support reads only
`metadata/cncf/knowledge-source.json` with schema
`cncf.knowledge-source.v1`, followed by bounded manifest-declared
`glossary-terms` JSON resources. It does not scrape rendered BoK pages or guess
alternate metadata paths. Configured source identity and publisher manifest
identity remain separate evidence. Invalid, duplicate, traversal,
cross-origin, non-JSON, or oversized inputs remain diagnostic.

SIE-mediated BoK knowledge uses a separate public component-route boundary:

```text
TEXTUS_CBD_SIE_ALLOWED_ORIGINS=https://sie.example
TEXTUS_CBD_SIE_BOK_ROUTES=semantic=https://sie.example/mcp
```

Only exact-origin-authorized `/mcp` endpoints are accepted. CBD invokes
`SemanticIntegrationEngine.SemanticRetrieval.searchTerms`, bounds both result
count and response bytes, and requires the typed SIE term fields including an
absolute evidence URI. A transport, MCP, schema, or evidence failure degrades
that SIE source. CBD does not access SIE storage or administration routes, and
does not merge SIE-owned terminology into CBD-owned component details.
`searchComponents` invokes the SIE lookup with the same requirement before it
performs independent catalog matching.

CNCF MCP publication can be narrowed with:

- `cncf.mcp.enabled=false`
- `cncf.mcp.disabled-services=CbdRetrieval`
- `cncf.mcp.disabled-operations=CbdRetrieval.getUsage`

## CbdRetrieval Operations

### searchComponents

Requires `requirement`. Optional identity, version, source, freshness,
availability, conflict, purpose, and limit fields constrain results. `results`
contains only catalog-backed component profiles. `observations` also preserves
matching development-directory and local/cache CAR evidence without
fabricating a profile. `issues` cites conflicting sources, `precedence`
explains purpose-specific authority, and `selectedObservation` remains absent
because retrieval does not select a hidden winner. A runtime constraint still
requires affirmative catalog runtime evidence. `semanticEvidence` separately
returns matching BoK-site and current-query SIE terms with source, rationale,
freshness, and evidence URI. A catalog result uses `semanticEvidenceIds` only
for explicitly equal published terms/tags; semantic fields never complete the
component profile.

### getComponent

Requires exact `name`; optional organization, kind, version, and catalog ID
disambiguate. An explicit version returns only its version-specific detail; a
listed version without detail clears version-sensitive fields and adds a
warning. Returns `no-match` rather than fabricating a component.

### getUsage

Accepts the component identity fields, including optional CAR/SAR `kind`, and
an optional bounded `intent`.
Returns the selected profile, service/operation summaries from model metadata,
and authoritative catalog, artifact, model-metadata, and documentation links.
The response identifies `selectedSourceId`, `selectedSourceKind`, and
`selectedVersion`. Guidance records cite that source/version and their evidence
URIs. `observed-fact` records report selection evidence;
`deterministic-inference` records recommend only observed operations with an
explicit intent-token overlap. `model-inference` is reserved for generative
advice and is not emitted by the current runtime. No token overlap produces no
operation recommendation. Missing source attribution withholds guidance, and
an unavailable sidecar yields a warning and the remaining references.

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

Returns authorized information-source identity, base URI or path, kind, priority,
readiness, observation count, freshness, latest refresh-attempt time, and
diagnostics for catalog, BoK, SIE, development-directory, local warehouse, and
cache inputs. A successfully inspected local source uses `observed` freshness;
its diagnostics make that source degraded. The response-level `warnings`
field also reports rejected remote and local configuration. The operation name
remains `listCatalogs` for Phase 2 compatibility.

### status

Returns aggregate state and counts across catalog, BoK-site, SIE, development,
local warehouse, and cache inputs. Local inspection is bounded, read-only, and
non-cached.

## CbdCatalogAdmin Operation

### refreshCatalog

Refreshes one source ID or every enabled source. A failed refresh records the
failure and preserves any previous snapshot. The operation is excluded from
MCP because it changes runtime state and may cause external traffic.

Snapshots have a default finite TTL of 15 minutes and the runtime cache policy
rejects non-positive lifetimes or lifetimes over 24 hours. Retrieval readiness
reuses a fresh snapshot and automatically refreshes a missing or expired
snapshot. If automatic refresh fails, the stale last-known-good snapshot stays
available and the source becomes `degraded`. The same lifetime and stale
last-known-good rule applies to BoK snapshots; `refreshCatalog` itself remains
catalog-only administration.

## Catalog Contract

CBD Support first attempts the Cozy repository contract for each kind:

```text
metadata/repository/car/index.json
metadata/repository/sar/index.json
```

It follows `sidecars.model_metadata_json` and preserves the selected version's
channel, status, component, publication time, artifact path and SHA-256, plus
`runtime.cncf.minimum`, `maximum`, and `tested`. Runtime filtering treats the
minimum/maximum range as inclusive; `tested` remains supporting evidence rather
than an exclusive allowlist. ABI dependencies under
`abi_manifest.abi.dependencies` are retained. Missing or JSON `null` archive
metadata is distinguished from an authoritative empty dependency array.
Repository diagnostics are returned as source warnings. A catalog remains useful
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

As verified on 2026-07-14, the default public compatibility catalog and its
repository-artifact metadata are available, while the rich CAR/SAR index
endpoints are not publicly accessible. Generating and deploying those indexes
and same-origin model-metadata sidecars is publisher-owned future work, not a
fallback responsibility of this CAR. The tracked candidate is
`FUTURE-CATALOG-PUBLISHER-01`; it requires both rich indexes to return valid
JSON, real CAR/SAR evidence and same-origin model metadata to be published, and
the compatibility fallback to remain verified until an explicit migration.

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
