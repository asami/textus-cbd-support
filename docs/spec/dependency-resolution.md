# Dependency Resolution Contract

## Purpose

`resolveDependencies` exposes the dependency evidence published by a selected
CAR or SAR and resolves that evidence into a bounded graph. Resolution must not
invent a version, merge catalog identities, or silently select evidence from a
different catalog.

## Resolution Boundary

- The selected component is the graph root.
- `dependencies` remains the root component's directly published dependency
  list for response compatibility.
- When an explicit root version differs from `dependencyMetadataVersion`,
  `dependencies` and `resolutions` are empty and the response explains that
  dependency metadata for the requested root version is unavailable.
- `resolutions` contains each traversed dependency edge, including transitive
  edges.
- A dependency is resolved only against profiles from the root component's
  `catalogId`.
- An explicit dependency `version` must occur in the candidate profile's
  published `versions`.
- An explicit dependency `kind` must match the candidate profile's kind.
- Zero candidates produce `unresolved`; multiple candidates produce
  `ambiguous`. Neither state is silently resolved from another catalog.
- `selectedVersion` records the catalog's selected component version.
  `dependencyMetadataVersion` separately records which version owns the parsed
  dependency metadata.
- A resolved edge is traversed only when an explicit requested version matches
  `dependencyMetadataVersion`. A mismatch keeps the resolved edge but stops
  the path and produces a metadata-version warning.

## Bounded Traversal

`maxDepth` is optional, defaults to 8, and is clamped to the inclusive range
1 through 32. Depth 1 is a direct dependency. A resolved component below the
limit is traversed recursively.

An ancestor cycle produces a `cycle` resolution entry and stops that path.
When the depth limit prevents traversal of a resolved component that publishes
dependencies, the response includes a truncation warning.

## Conflict Contract

A conflict exists when the graph contains two or more distinct explicit
versions for the same dependency `name` and effective `kind`. The response
contains the requested versions and every evidence path that contributed to
the conflict. An absent version is an unconstrained request and does not create
a version conflict by itself.

Conflicts are observations, not automatic version choices. CBD Support does
not select a winner or assert compatibility.

## Evidence and Failure Semantics

Each resolution entry contains the published dependency, status, depth, and a
root-to-dependency path. Resolved and cyclic entries also contain catalog,
organization, resolved version, dependency-metadata version, and evidence URI
when available.

Unresolved, ambiguous, cyclic, and truncated paths are returned as structured
resolution evidence plus warnings. They do not turn an otherwise successful
component lookup into `no-match` or an infrastructure failure.
