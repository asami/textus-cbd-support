# Textus CBD Support

`textus-cbd-support` is a CAR component for Component-Based Development (CBD).
It reads the component catalog published by simplemodeling.org plus explicitly
configured catalogs, then exposes evidence-bearing CAR/SAR discovery and usage
operations through CNCF MCP.

The component does not ingest its catalog into SIE. SIE remains the owner of
BoK terminology and may return a minimal `ComponentReference`; CBD Support is
the owner of versions, runtime compatibility, dependencies, operations,
artifacts, manuals, examples, and reuse guidance.

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

CBD Support auto-detects two catalog formats:

- Cozy repository indexes at `metadata/repository/{car|sar}/index.json`.
- The deployed simplemodeling.org publication catalog rooted at
  `en/catalog/index.html` with `cozy.publish-project.v1` JSON evidence.

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

The default public source currently serves the compatibility catalog but not
the rich Cozy CAR/SAR indexes. The publisher-side work and acceptance criteria
are recorded in [Default Catalog Rich Metadata Candidate](docs/future/default-catalog-rich-metadata.md);
CBD Support continues to report absent rich evidence instead of synthesizing it.

A `runtimeVersion` search constraint accepts only profiles that publish an
affirmative runtime minimum and, when present, a compatible maximum. Missing
runtime evidence is not treated as compatibility. Tested versions are evidence,
not an exclusive allowlist. Publication entries that cannot be loaded are retained as source
warnings, while `*-SNAPSHOT` versions remain separate from `latestStable`.
An explicit `version` projects only that version's artifact, runtime,
dependency, and model-metadata evidence; listed versions without detail do not
inherit fields from the catalog-selected version.

## MCP Operations

`CbdRetrieval` is MCP ready. `CbdCatalogAdmin` is intentionally private.

- `searchComponents`: find candidates from catalog evidence.
- `getComponent`: resolve one exact component.
- `getUsage`: obtain operations and documentation/artifact references; pass
  `kind` when CAR/SAR identities could overlap.
- `resolveDependencies`: retain direct published dependencies and return a
  bounded same-catalog dependency graph with unresolved, ambiguous, cyclic,
  and explicit version-conflict evidence. Selected component version and
  dependency-metadata version remain separate, so metadata is not reused for a
  different explicitly requested version.
- `listCatalogs`: inspect source readiness, cache freshness, expiry, and refresh
  attempt state.
- `status`: inspect aggregate readiness.

`refreshCatalog` is an administrative CNCF command and is not published as an
MCP tool. CAR/SAR runtime configuration may further disable a ready service or
operation through `cncf.mcp.enabled`, `cncf.mcp.disabled-services`, and
`cncf.mcp.disabled-operations`.

See [User Guide](docs/user-guide.md), [Reference Manual](src/main/car/manual/index.md),
[Cozy Catalog Fidelity](docs/spec/cozy-catalog-fidelity.md), and
[ComponentReference Contract](docs/spec/component-reference-contract.md).
