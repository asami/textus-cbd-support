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
   `kind`, `version`, or `runtimeVersion` filters.
2. Inspect each result's match classification, rationale, source catalog, and
   evidence URI.
3. Resolve the exact identity with `getComponent`.
4. Read operation and documentation evidence with `getUsage`.
5. Read direct and transitive dependency evidence with `resolveDependencies`.
   Use `maxDepth` when a bound other than the default 8 is required, and inspect
   every resolution status and conflict rather than assuming a selected winner.
6. Select a component only when the returned catalog evidence supports the
   requested version and runtime. A runtime-constrained search excludes
   profiles that do not publish runtime compatibility evidence.

CBD Support never invents missing versions, dependencies, operations, or
documentation. Missing optional data is returned as warnings.

Dependency traversal stays inside the selected component's catalog. An
unresolved dependency is not silently taken from another configured source.
Conflicting explicit versions are returned with their evidence paths; CBD
Support does not choose which version should win.
`selectedVersion` identifies the component version chosen by the catalog, while
`dependencyMetadataVersion` identifies the version owning parsed dependency
data. If an explicit request differs, the affected dependency path stops and a
warning is returned instead of applying another version's metadata.

## SIE Handoff

SIE component discovery returns only `ComponentReference`. The shared fields
are `sourceId`, `catalogId`, `organization`, `name`, `title`, `kind`, `version`,
and `evidenceUri`. SIE normally supplies `sourceId`; CBD Support supplies
`catalogId`. The stable handoff fields are component identity, kind, version,
and evidence URI.

Do not expect SIE to explain component dependencies or usage. Do not load CBD
catalog data into SIE merely to make component development tools available.

## Troubleshooting

- `not-started`: no catalog has been loaded yet. Invoke a retrieval operation
  or run the administrative refresh command.
- `degraded` with retained components: the latest refresh failed and the last
  known good snapshot remains active.
- `degraded` with zero components: every initial catalog load failed. Check the
  configured base URI and the two metadata index paths.
- `no-match`: no catalog evidence satisfied the identity and filters. Remove
  optional filters only when broader discovery is acceptable.
- Missing operations: the catalog did not publish a model-metadata sidecar or
  it could not be read. Use the warning and evidence URI to diagnose the
  publisher.
- Partial publication failure: successfully loaded components remain available,
  but the source is degraded and lists each unreadable component entry.
- Publication compatibility mode: identity, version, artifact, and
  documentation are authoritative, but operation and dependency details wait
  for the publisher's Cozy repository index/model-metadata sidecars.
