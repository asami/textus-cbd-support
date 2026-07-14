# Textus CBD Support User Guide

## Purpose

Use CBD Support when a generative AI developer needs to find another CAR/SAR,
understand its contract, and determine how to reuse it. Use SIE separately for
BoK terminology grounding and for discovering that a BoK-managed component
exists.

## Prerequisites

- Java 21
- sbt 1.9.7 or later
- Cozy/sbt-cozy `0.3.0-SNAPSHOT` development environment
- CNCF `0.5.1-SNAPSHOT`
- Network access to simplemodeling.org or one configured component catalog

## First Success

1. Run `sbt --batch test cozyBuildCAR`.
2. Start the CAR through the CNCF server runtime used by the target SAR.
3. Inspect generated component Help for `CbdSupport`.
4. Call `CbdRetrieval.status` and confirm at least one source is `ready`.
5. Call `CbdRetrieval.searchComponents` with a concrete requirement.
6. Pass the returned `ComponentReference` to `getComponent`, `getUsage`, or
   `resolveDependencies` by using its identity fields, including `kind`.

## Normal Workflow

1. Search with a requirement such as `account authentication` and optional
   identity, version, source, freshness, availability, conflict, or purpose
   filters.
2. Inspect catalog-backed `results` separately from source-preserving
   `observations`, `issues`, and `precedence`.
3. Resolve the exact identity with `getComponent`.
4. Read operation and documentation evidence with `getUsage`. Supply an
   optional concrete `intent`, such as `retrieve an order`, when operation
   guidance is useful.
5. Read direct and transitive dependency evidence with `resolveDependencies`.
   Use `maxDepth` when a bound other than the default 8 is required, and inspect
   every resolution status and conflict rather than assuming a selected winner.
6. Select a component only when the returned catalog evidence supports the
   requested version and runtime. A runtime-constrained search excludes
   profiles that do not publish runtime compatibility evidence or whose
   inclusive minimum/maximum range excludes the requested runtime.

CBD Support never invents missing versions, dependencies, operations, or
documentation. Missing optional data is returned as warnings.

`getUsage` reports `selectedSourceId`, `selectedSourceKind`, and
`selectedVersion` for the catalog profile whose usage was actually read. Its
`guidance` keeps the selected-source statement as `observed-fact`. An operation
is recommended only when its observed service, operation, kind, or description
shares an explicit token with the bounded intent; that recommendation is
`deterministic-inference` with a score, rationale, and catalog/model-metadata
evidence URIs. An unrelated intent produces no operation recommendation. The
`model-inference` kind is reserved for generative advice; this runtime does not
invoke a generative model and never relabels deterministic matching as model
inference. Missing source context withholds guidance rather than inventing a
published source.

Dependency traversal stays inside the selected component's catalog. An
unresolved dependency is not silently taken from another configured source.
Conflicting explicit versions are returned with their evidence paths; CBD
Support does not choose which version should win.
`selectedVersion` identifies the component version chosen by the catalog, while
`dependencyMetadataVersion` identifies the version owning parsed dependency
data. If an explicit request differs, the affected dependency path stops and a
warning is returned instead of applying another version's metadata.
When `version` is explicit, search and exact lookup project only evidence for
that version. If the catalog lists the version without detail, identity remains
available but artifact, runtime, dependency, and model-metadata fields are
absent with a warning.

Rich Cozy profiles retain the selected version's channel, status, component,
publication time, runtime minimum/maximum/tested versions, artifact SHA-256,
and model-metadata sidecar. `runtimeTested` is supporting evidence, not an
exclusive allowlist. Cozy repository diagnostics remain source warnings even
when the affected catalog entry can still be used.

Catalog snapshots have a 15-minute lifetime. Retrieval operations reuse a
fresh snapshot and automatically attempt refresh at or after `expiresAt`. A
failed attempt retains the stale last-known-good snapshot. Use `listCatalogs`
to inspect `cacheStatus`, `refreshedAt`, `expiresAt`,
`lastRefreshAttemptAt`, and any warning.
Catalog source/origin configuration, index and metadata bytes, and discovered
profile count are bounded; truncation remains visible as a warning.

Additional catalog configuration is authorized in two steps. Put candidate
base URIs in `TEXTUS_CBD_CATALOGS` and their permitted network origins in
`TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS`. Origin matching uses scheme, host, and
effective port; it does not authorize another scheme or port. The built-in
simplemodeling.org source does not require an allowlist entry. Rejected entries
are excluded from network access and returned as warnings.

Additional BoK sites use `TEXTUS_CBD_BOK_SITES` and
`TEXTUS_CBD_BOK_ALLOWED_ORIGINS` with the same `[id=]base-uri` and exact-origin
authorization model. A supported site must publish
`metadata/cncf/knowledge-source.json` using
`schemaVersion=cncf.knowledge-source.v1`; CBD Support follows only bounded
manifest-declared `glossary-terms` JSON and never scrapes rendered pages.
`listCatalogs` retains its compatibility name but reports both catalog and BoK
source identity, kind, readiness, freshness, and diagnostics. BoK terms remain
semantic evidence and are not converted into component catalog facts. BoK
snapshots also have a 15-minute default lifetime; failed expiry refresh retains
stale terms with the original observation/expiry and the latest attempt time.

To retrieve BoK terminology through SIE, authorize the SIE origin with
`TEXTUS_CBD_SIE_ALLOWED_ORIGINS` and configure one or more `[id=].../mcp`
routes in `TEXTUS_CBD_SIE_BOK_ROUTES`. CBD accepts only the public `/mcp` route
and invokes the typed
`SemanticIntegrationEngine.SemanticRetrieval.searchTerms` operation. Each term
must include the SIE contract's identity, definition, dataset, match, rationale,
score, and absolute `evidence_uri`; an incomplete term response is rejected as
a source failure. Query/category characters, response body, and result count
are bounded. Results are query scoped and are never reused for another query;
the last successful observation remains only as degraded source-state evidence
after a later failure.
`searchComponents` triggers this SIE lookup with the same requirement, while
its component matches continue to come only from CBD-owned catalog evidence.

Configure working directories with comma-separated `[id=]path` entries in
`TEXTUS_CBD_DEVELOPMENT_DIRECTORIES`. Optional
`TEXTUS_CBD_LOCAL_CAR_ROOT` and `TEXTUS_CBD_CACHE_CAR_ROOT` settings override
the canonical `~/.cncf/local` and `~/.cncf/cache` roots. These inputs are
inspected read-only and within explicit bounds whenever retrieval inputs are
initialized. They appear in `listCatalogs` and `status` with `observed`
freshness and their own diagnostics.

Source-aware `searchComponents` adds `sourceId`, `sourceKind`, `freshness`,
`versionState`, `conflictCode`, and `purpose` filters. Its `results` contain
only catalog-backed profiles. Working, local-published, and cached evidence is
returned separately in `observations`; `issues` cites conflicts and
`precedence` explains purpose-specific authority. `selectedObservation`
remains absent because CBD Support does not choose a hidden winner.

Matching BoK-site and current-query SIE terms appear separately in
`semanticEvidence`, including source, term, match rationale, freshness, and
evidence URI. A component result lists the applicable citation IDs in
`semanticEvidenceIds` only when its catalog explicitly publishes the same term
or tag. Semantic evidence can help discover that catalog profile, but it never
adds component versions, runtime constraints, dependencies, operations, or
artifact facts. An older retained SIE response is never reused for a different
query.

An authorized SIE source appears as `sie-bok` with
`component-route-allowlist` authorization. Before retrieval it is
`not-started`; a valid response makes it `ready`, while transport, MCP, schema,
or evidence failures make it `degraded`. Retrieved terms remain SIE-owned
observations. They do not add versions, dependencies, operations, artifacts,
or usage statements to CBD component profiles.

## SIE Handoff

SIE component discovery returns only `ComponentReference`. The shared fields
are `sourceId`, `catalogId`, `organization`, `name`, `title`, `kind`, `version`,
and `evidenceUri`. SIE normally supplies `sourceId`; CBD Support supplies
`catalogId`. The stable handoff fields are component identity, kind, version,
and evidence URI.

Do not expect SIE to explain component dependencies or usage. CBD accesses
only SIE's public read-only component/MCP contract, never its RDF/vector stores
or private ingestion operations. Do not load CBD catalog data into SIE merely
to make component development tools available.

## Troubleshooting

- `not-started`: no catalog has been loaded yet. Invoke a retrieval operation
  or run the administrative refresh command.
- `degraded` with retained components: the latest refresh failed and the last
  known good snapshot remains active, or a snapshot is stale. Inspect
  `cacheStatus`, expiry, last attempt, and warning together.
- `degraded` with zero components: every initial catalog load failed. Check the
  configured base URI and the two metadata index paths.
- `no-match`: no catalog evidence satisfied the identity and filters. Remove
  optional filters only when broader discovery is acceptable.
- Missing operations: the catalog did not publish a model-metadata sidecar or
  it could not be read. Use the warning and evidence URI to diagnose the
  publisher.
- Cozy archive diagnostic: inspect the warning code, artifact, and version.
  Missing or JSON `null` descriptor/ABI metadata means dependency evidence is
  unavailable; an explicit empty dependency array means authoritative no
  dependencies.
- Partial publication failure: successfully loaded components remain available,
  but the source is degraded and lists each unreadable component entry.
- Rejected configured source: inspect catalog warnings for an invalid source
  ID/base URI, missing exact origin permission, or duplicate source ID. Keep
  credentials, query strings, and fragments out of catalog base URIs.
- Publication compatibility mode: identity, version, artifact, and
  documentation are authoritative, but operation and dependency details wait
  for the publisher's Cozy repository index/model-metadata sidecars. The
  publisher gap and acceptance criteria are tracked in
  `docs/future/default-catalog-rich-metadata.md`.
