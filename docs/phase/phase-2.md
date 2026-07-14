# Phase 2: Catalog Fidelity and Resolution

Stage Status:
- Current status: IN_PROGRESS
- Current step: The default-catalog rich-metadata gap (`P2-03`) is recorded as a publisher-owned future candidate and is under post-implementation review.
- Owner: Textus CBD development
- Update rule: Update after each checklist item obtains reproducible evidence; closure is based only on `phase-2-checklist.md`.

## Purpose

Deepen catalog fidelity and resolution while preserving evidence, catalog
identity, and the established SIE/CBD ownership split.

## Scope

- Richer Cozy catalog schema and explicit version-selection behavior.
- Bounded dependency graph traversal with unresolved, ambiguous, cyclic, and
  version-conflict evidence.
- Configured-source authorization.
- Bounded catalog caching and refresh observability.
- Verification against representative and deployed catalogs.

## Non-Goals

- Automatic dependency installation or conflict winner selection.
- Moving component detail into SIE.
- Generative recommendation ranking, which belongs to Phase 3.
- Production credential lifecycle and SAR composition governance, which belong
  to Phase 4.

## Current Implementation Slice

The first slice keeps the existing direct `dependencies` response and adds a
bounded graph projection. Same-catalog resolution, incomplete-edge reporting,
cycle detection, conflict reporting, and depth bounds are implemented.
`selectedVersion` and `dependencyMetadataVersion` are separate evidence: an
explicitly requested version is traversed only when its dependency metadata
version matches, including at the graph root.

The second slice retains version-specific artifact, runtime, dependency, and
model-metadata evidence. Explicit-version search and lookup project only that
evidence. A listed version without detail remains selectable by identity but
clears version-sensitive fields and reports the missing evidence.

The third slice gives every catalog snapshot a finite 15-minute default
lifetime. Readiness reuses fresh snapshots, refreshes missing or expired
sources, and preserves stale last-known-good evidence on failure. Source state
exposes cache freshness, expiry, last successful refresh, last refresh attempt,
and the failure warning.

The fourth slice keeps the built-in simplemodeling.org source and requires
every additional catalog candidate to match an explicitly configured origin.
Scheme, host, and effective port are authoritative. Invalid, non-allowlisted,
credential-bearing, and duplicate-ID entries are rejected before network
access, with sanitized reasons exposed as warnings.

The fifth slice verifies that the default public source still serves the
publication compatibility catalog while rich CAR/SAR repository indexes are
not publicly accessible. The remaining generation/deployment work is outside
this CAR and is closed as the publisher-owned candidate documented in
`docs/future/default-catalog-rich-metadata.md`, with concrete acceptance gates.

## Verification Evidence

- Focused dependency/runtime and MCP projection specifications: 16 tests passed
  on 2026-07-14.
- Full repository test: 16 tests passed on 2026-07-14 using the required local
  Cozy `0.3.0-SNAPSHOT` generation runtime.
- CAR build: `target/textus-cbd-support-0.1.0-SNAPSHOT.car` generated on
  2026-07-14.
- CAR lint: no FAIL findings; the project-local ABI baseline warning remains
  non-blocking for this development checkpoint.
- CML lint: no findings on 2026-07-14.
- Focused version-selection, dependency/runtime, and MCP projection
  specifications: 18 tests passed on 2026-07-14.
- Focused cache-lifetime, refresh-observation, version-selection,
  dependency/runtime, and MCP projection specifications: 19 tests passed on
  2026-07-14.
- Focused source-authorization, cache, catalog, dependency/runtime, and MCP
  projection specifications: 21 tests passed on 2026-07-14.
- Default public catalog audit on 2026-07-14: compatibility catalog and
  repository-artifact metadata returned HTTP 200; rich CAR and SAR index
  endpoints returned HTTP 403. The local simplemodeling.org publication
  workspace likewise contains compatibility metadata but no rich index output.

## Closure Basis

Phase 2 is DONE only when every item in `phase-2-checklist.md` is `[x]` and its
verification evidence is recorded here.
