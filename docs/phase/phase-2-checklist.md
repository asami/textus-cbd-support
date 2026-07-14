# Phase 2 Checklist: Catalog Fidelity and Resolution

This checklist is the authoritative Phase 2 state ledger. Items are never
deleted; a checked item requires observable evidence.

## Catalog Fidelity

- [ ] `P2-01` Cozy repository parsing covers the Phase 2 version, dependency, runtime, artifact, and sidecar schema selected from real published catalogs.
- [ ] `P2-02` Version selection distinguishes stable, snapshot, explicit request, missing evidence, and incompatible evidence without synthesizing a version.
- [ ] `P2-03` The default public catalog exposes rich Cozy repository indexes and model metadata, or the remaining publisher-side gap is explicitly closed as a future development candidate.

## Dependency Resolution

- [x] `P2-11` Resolution retains direct dependency compatibility and traverses transitive dependencies only within the selected catalog and selected-version evidence boundary.
- [x] `P2-12` Resolution reports unresolved, ambiguous, cyclic, and conflicting explicit version evidence without selecting a winner or applying metadata from another version.
- [x] `P2-13` Dependency traversal has a documented and tested maximum depth.

## Source and Cache Policy

- [ ] `P2-20` Configured catalogs are accepted or rejected by an explicit source-authorization policy.
- [ ] `P2-21` Catalog snapshots have a bounded cache lifetime while preserving last-known-good failure behavior.
- [ ] `P2-22` Source state exposes enough refresh and cache observations to diagnose freshness and degraded service.

## Documentation and Verification

- [ ] `P2-30` README, user guide, reference manual, strategy, phase, and static specifications describe the completed Phase 2 behavior.
- [ ] `P2-31` Executable specifications cover real catalog fidelity, version selection, graph resolution, conflicts, authorization, caching, and refresh observations.
- [ ] `P2-32` Full tests, CAR build, CAR lint, and representative MCP projection pass with Phase 2 contracts.
