# Textus CBD Support

`textus-cbd-support` is a CAR component for Component-Based Development (CBD).
It reads the component catalog published by simplemodeling.org plus explicitly
configured catalogs, then exposes evidence-bearing CAR/SAR discovery and usage
operations through CNCF MCP.

The component does not ingest its catalog into SIE. SIE remains the owner of
BoK terminology and may be configured as a read-only, evidence-bearing input;
CBD Support remains the owner of versions, runtime compatibility, dependencies,
operations, artifacts, manuals, examples, and reuse guidance.

## Build

Project identity, Scala `3.3.8`, component version, dependencies, and runtime
compatibility are declared in `project.yaml`. `build.sbt` only maps that
metadata into sbt-cozy.

```sh
sbt --batch test cozyBuildCAR
```

The CAR is generated at:

```text
target/textus-cbd-support-0.1.0-SNAPSHOT.car
```

## Catalogs

The default source is:

```text
https://www.simplemodeling.org/
```

Additional sources are configured with `TEXTUS_CBD_CATALOGS`. Entries are
comma-separated absolute HTTP(S) base URIs. An optional `id=` prefix gives a
stable source selector. Every additional source must also match an origin in
`TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS`; scheme, host, and effective port must
match, while the catalog base path remains in `TEXTUS_CBD_CATALOGS`.

```sh
export TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS='https://catalog.example'
export TEXTUS_CBD_CATALOGS='team=https://catalog.example/team/,https://catalog.example/shared/'
```

The default simplemodeling.org source is built in. Invalid, non-allowlisted, or
duplicate configured sources are not fetched, and their rejection reasons are
returned as catalog/retrieval warnings.

CBD Support uses two explicitly classified catalog contracts:

- Cozy repository indexes at `metadata/repository/{car|sar}/index.json`.
- The deployed simplemodeling.org publication catalog rooted at
  `en/catalog/index.html` with `cozy.publish-project.v1` JSON evidence.

The Cozy endpoints are attempted first. The publication contract is used only
when both rich index kinds are unavailable. A returned rich document with
invalid JSON, an invalid envelope/entry, or an unknown declared schema is
incompatible and is not reinterpreted as publication metadata.

Cozy indexes provide the richer contract, including selected-version channel
and status, runtime minimum/maximum/tested evidence, nested ABI dependencies,
artifact path and checksum, model-metadata sidecars, and repository
diagnostics. Publication compatibility mode provides
identity, version, artifact, and documentation evidence and reports unavailable
operation metadata from `getUsage`. Failed refreshes preserve the last known
good snapshot and report the source as `degraded`. Snapshots are fresh for 15
minutes; the next retrieval after expiry attempts a refresh before serving.
`listCatalogs` exposes fresh/stale/empty cache state, expiry, and the latest
refresh-attempt time.

Runtime retention admits at most 64 sources and keeps only their latest
snapshots. Combined retained totals are capped at 20,000 Catalog profiles,
20,000 BoK terms, 800 SIE terms, and 512 local observations, using stable
per-source quotas that preserve attribution and prevent one refresh from
evicting another source's last-known-good evidence.
Authentication, transport, parse, and compatibility failures keep expired
evidence explicitly degraded and stale until a successful bounded retry
establishes a new current observation.

The default public source currently serves the compatibility catalog but not
the rich Cozy CAR/SAR indexes. The publisher-side work remains identified as
`FUTURE-CATALOG-PUBLISHER-01`; CBD Support continues to report absent rich
evidence instead of synthesizing it.

A `runtimeVersion` search constraint accepts only profiles that publish an
affirmative runtime minimum and, when present, a compatible maximum. Missing
runtime evidence is not treated as compatibility. Tested versions are evidence,
not an exclusive allowlist. Publication entries that cannot be loaded are retained as source
warnings, while `*-SNAPSHOT` versions remain separate from `latestStable`.
An explicit `version` projects only that version's artifact, runtime,
dependency, and model-metadata evidence; listed versions without detail do not
inherit fields from the catalog-selected version.

## Federated BoK Inputs

Additional BoK sites use `TEXTUS_CBD_BOK_SITES` and
`TEXTUS_CBD_BOK_ALLOWED_ORIGINS`. CBD reads only the canonical
`cncf.knowledge-source.v1` manifest and bounded, manifest-declared JSON glossary
resources.

SIE-mediated BoK inputs use `TEXTUS_CBD_SIE_BOK_ROUTES` and
`TEXTUS_CBD_SIE_ALLOWED_ORIGINS`:

```sh
export TEXTUS_CBD_SIE_ALLOWED_ORIGINS='https://sie.example'
export TEXTUS_CBD_SIE_BOK_ROUTES='semantic=https://sie.example/mcp'
```

Only an exact-origin-authorized `/mcp` route is accepted. CBD calls the typed
public `SemanticIntegrationEngine.SemanticRetrieval.searchTerms` operation with
a bounded response size and requires every returned term to carry a valid
`evidence_uri`. It does not call SIE administration routes or read SIE storage.
SIE terms remain separate observations and are not used to fill or overwrite
CBD component profiles. `searchComponents` performs the read-only SIE term
lookup for the same requirement before applying CBD catalog matching.

## Source Authentication

Remote catalog, BoK-site, and SIE sources may bind one source-owned credential
reference through `TEXTUS_CBD_SOURCE_AUTHENTICATION`:

```sh
export TEXTUS_CBD_SOURCE_AUTHENTICATION='team=bearer:config-key/catalog.team.token,knowledgehub=basic:config-key/bok.basic,semantic=api-key:config-key/sie.api.key'
```

Supported schemes are `bearer`, `basic`, and `api-key`. The value after the
scheme must be a CNCF runtime configuration reference beginning with
`config-key/`; raw tokens, passwords, headers, or credential-bearing URIs are
rejected. Origin/route authorization happens before lookup, and CNCF resolves
only the owning source's reference immediately before its outbound request.

`listCatalogs` source records expose only `authenticationScheme` and
`credentialConfigured`, never the reference or resolved value. Missing,
resolver-unavailable, explicitly expired, and rejected credentials use the
sanitized codes `source-credential-missing`,
`source-credential-unavailable`, `source-credential-expired`, and
`source-credential-rejected`. Authentication never selects another source's
credential or retries internally; the normal bounded refresh policy may
schedule a later source attempt.

## Phase 3 Source Model

Configure read-only working evidence with
`TEXTUS_CBD_DEVELOPMENT_DIRECTORIES=[id=]path`. The local warehouse and managed
cache default to `~/.cncf/local` and `~/.cncf/cache`; override them with
`TEXTUS_CBD_LOCAL_CAR_ROOT` and `TEXTUS_CBD_CACHE_CAR_ROOT`. Development,
local, and cache roots are canonicalized, bounded, and never written by CBD
Support. Development directories and configured CAR-root replacements use
`explicit-path-allowlist`; the two default CAR roots use
`canonical-storage-root` authority.

The five source kinds retain distinct authority: published catalogs own the
component facts they publish; BoK sites own their terminology; SIE owns its
current-query semantic matches; development directories describe current
working state; and CAR storage describes local-published or cached artifact
availability. For `development-work`, working evidence precedes local artifacts
and published comparison. For `local-execution`, local-published precedes
cached availability. For `published-reuse`, published catalogs are the reuse
authority. For `artifact-verification`, checksums are peer evidence and a
disagreement has no winner. These tiers explain authority but never select or
merge an observation automatically.

## MCP Operations

`CbdRetrieval` is MCP ready. `CbdCatalogAdmin` is intentionally private.

- `searchComponents`: find candidates from catalog evidence.
- `getComponent`: resolve one exact component.
- `getUsage`: obtain operations and documentation/artifact references; pass
  `kind` when CAR/SAR identities could overlap and optional `intent` for
  evidence-bounded operation guidance. The response identifies its selected
  source and version. `observed-fact`, `deterministic-inference`, and reserved
  `model-inference` statement kinds are distinct; the current runtime does not
  call a generative model or emit model inference.
- `resolveDependencies`: retain direct published dependencies and return a
  bounded same-catalog dependency graph with unresolved, ambiguous, cyclic,
  and explicit version-conflict evidence. Selected component version and
  dependency-metadata version remain separate, so metadata is not reused for a
  different explicitly requested version.
- `listCatalogs`: inspect source readiness, cache freshness, expiry, and refresh
  attempt state.
- `status`: inspect aggregate readiness.

The three exact operations never use catalog priority as a hidden winner. Zero
candidates return status `no-match` with a `component-not-found` absence;
multiple candidates return status `ambiguous` with an `ambiguous-selection`
absence, the full `candidateCount`, and at most 20 attributable `alternatives`.
Supply `catalogId` or refine the identity to select one source.
Usage and dependency responses also return stable `absences` when operation,
intent, version, source-attribution, or dependency-metadata evidence is
insufficient. An empty evidence list is therefore not presented as an
authoritative catalog fact without its supporting metadata.

`refreshCatalog` is an administrative CNCF command and is not published as an
MCP tool. CAR/SAR runtime configuration may further disable a ready service or
operation through `cncf.mcp.enabled`, `cncf.mcp.disabled-services`, and
`cncf.mcp.disabled-operations`.

## Representative CBD and SIE SAR

Run `scripts/check-cbd-sie-sar.sh` to build both local snapshot CARs, assemble
the representative `textus-cbd-sie` SAR profiles, and verify each through a
separately owned loopback CNCF server. The policy matrix requires exact CBD/SIE
tool counts of `6/7` at baseline, `0/0` under global disable, `6/0` when the SIE
service is disabled, and `5/6` when both status operations are disabled.
Disabled calls must return JSON-RPC `-32602`; any administration, mutation,
legacy facade, or other unexpected tool fails the check. Set
`CNCF_RUNTIME_DEV_DIR` to validate a local CNCF checkout. The baseline profile
also serves repository-owned catalog, development-directory, and BoK fixtures,
ingests the BoK fixture into temporary in-memory SIE, and verifies through live
MCP calls that CBD observations and SIE semantic evidence remain separate. It
requires conflicting versions to retain both source IDs with no hidden winner,
applies result bounds without losing conflict provenance, and checks that a
missing catalog stays degraded without an immediate retry loop. See
[the representative SAR example](examples/cbd-sie-sar/README.md).

The same command is the representative execution gate for the declared CNCF
runtime matrix. It first checks `project.yaml` against the machine-readable
[runtime compatibility matrix](docs/spec/runtime-compatibility-matrix.md) and
refuses unassessed or excluded candidates. Success records the selected runtime
source, Git revision when applicable, and clean/dirty worktree state; it does
not infer compatibility for versions absent from the matrix.

Run `scripts/check-car-abi.sh` to verify the source-managed CAR ABI against
generated CML model metadata, the built CAR, and Cozy's SemVer transition
policy. The current first-release line has no historical release baseline;
compatible-addition, breaking-minor, and intentional-major behavior is covered
by policy fixtures without presenting them as released component versions. See
[CAR ABI Governance](docs/spec/car-abi-governance.md).
Input-version and fallback decisions are fixed by
[Information Input Compatibility Governance](docs/spec/input-compatibility-governance.md).
The Phase 4 audience-to-contract coverage is recorded in
[Phase 4 Documentation Map](docs/spec/phase-4-documentation-map.md).

See [User Guide](docs/user-guide.md), [Reference Manual](src/main/car/manual/index.md),
[Cozy Catalog Fidelity](docs/spec/cozy-catalog-fidelity.md),
[ComponentReference Contract](docs/spec/component-reference-contract.md),
[SIE-mediated BoK Contract](docs/spec/sie-bok-information-source.md),
[Intent-Aware Usage Guidance](docs/spec/intent-aware-usage-guidance.md), and
[Evidence-Bounded Exact Retrieval](docs/spec/evidence-bounded-exact-retrieval.md).
The consolidated Phase 3 source roles and precedence are defined by
[Phase 3 Source and Precedence](docs/spec/phase-3-source-precedence.md).
