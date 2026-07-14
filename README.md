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
stable source selector.

```sh
export TEXTUS_CBD_CATALOGS='team=https://catalog.example/team/,https://catalog.example/shared/'
```

CBD Support auto-detects two catalog formats:

- Cozy repository indexes at `metadata/repository/{car|sar}/index.json`.
- The deployed simplemodeling.org publication catalog rooted at
  `en/catalog/index.html` with `cozy.publish-project.v1` JSON evidence.

Cozy indexes provide the richer contract, including runtime, dependency, and
generated operation metadata. Publication compatibility mode provides
identity, version, artifact, and documentation evidence and reports unavailable
operation metadata from `getUsage`. Failed refreshes preserve the last known
good snapshot and report the source as `degraded`.

A `runtimeVersion` search constraint accepts only profiles that publish an
affirmative runtime minimum. Missing runtime evidence is not treated as
compatibility. Publication entries that cannot be loaded are retained as source
warnings, while `*-SNAPSHOT` versions remain separate from `latestStable`.

## MCP Operations

`CbdRetrieval` is MCP ready. `CbdCatalogAdmin` is intentionally private.

- `searchComponents`: find candidates from catalog evidence.
- `getComponent`: resolve one exact component.
- `getUsage`: obtain operations and documentation/artifact references; pass
  `kind` when CAR/SAR identities could overlap.
- `resolveDependencies`: return published dependencies for the selected CAR or
  SAR identity.
- `listCatalogs`: inspect source state.
- `status`: inspect aggregate readiness.

`refreshCatalog` is an administrative CNCF command and is not published as an
MCP tool. CAR/SAR runtime configuration may further disable a ready service or
operation through `cncf.mcp.enabled`, `cncf.mcp.disabled-services`, and
`cncf.mcp.disabled-operations`.

See [User Guide](docs/user-guide.md), [Reference Manual](src/main/car/manual/index.md),
and [ComponentReference Contract](docs/spec/component-reference-contract.md).
