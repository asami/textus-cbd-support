# Component Version Selection Contract

## Purpose

CBD Support must project catalog evidence for the version a caller requests.
It must not return the selected/latest version's artifact, runtime,
dependencies, or model metadata as though they belonged to another version.

## Catalog Selection

Without an explicit version, the profile uses catalog evidence in this order:

1. `recommended`;
2. latest stable;
3. latest snapshot;
4. first published version.

`latestStable` and `latestSnapshot` remain catalog facts even when another
version is explicitly selected. `selectedVersion` records the version used for
the returned profile.

## Explicit Selection

When `searchComponents` or `getComponent` receives `version`:

- the version must occur in the profile's published `versions`;
- version-specific channel, status, component, publication time, runtime
  minimum/maximum/tested versions, dependencies, artifact URI and SHA-256, and
  model-metadata evidence replace the selected/default fields;
- `ComponentReference.version` uses the explicitly selected version;
- runtime filtering evaluates the selected version's runtime evidence.

If the version is listed but has no version-specific metadata, the profile
keeps its identity and `selectedVersion`, clears version-sensitive detail, and
returns a warning. It never falls back to another version's detail.

## Compatibility and Absence

- A runtime-constrained search excludes a selected version whose runtime
  evidence is absent, whose minimum exceeds the requested runtime, or whose
  published maximum is below it. Minimum and maximum bounds are inclusive.
- `runtimeTested` remains supporting evidence and is not an exclusive
  compatibility allowlist.
- Missing version detail is not evidence that dependencies are empty or that
  an artifact/runtime is compatible.
- Snapshot versions are never promoted to `latestStable`.
