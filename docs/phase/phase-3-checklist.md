# Phase 3 Checklist: Federated Development Context and AI Ergonomics

This checklist is the authoritative Phase 3 state ledger. Items are never
deleted; a checked item requires observable evidence.

## Input Source Contracts

- [x] `P3-01` simplemodeling.org remains the built-in public source and its observations retain an explicit source identity.
- [x] `P3-02` Other BoK sites can be explicitly configured and authorized through supported machine-readable contracts without rendered-page scraping or silent identity merging.
- [x] `P3-03` SIE-mediated BoK knowledge is retrieved with evidence while preserving SIE ownership of terminology and semantic relationships and CBD Support ownership of component detail.
- [x] `P3-04` Explicitly configured development directories are inspected read-only with bounded scope and distinguish working evidence from published fact.
- [x] `P3-05` CAR versions in the local warehouse and managed cache are inventoried separately, including component, version, channel, artifact, and checksum evidence when available.

## Evidence and Reconciliation

- [x] `P3-11` A unified source descriptor exposes source kind, stable identity, location, authorization, freshness, and diagnostics for all five input-source kinds.
- [x] `P3-12` Component observations preserve their source, evidence location, version identity, freshness, and checksum instead of silently combining fields across sources.
- [x] `P3-13` Duplicate, missing, stale, incompatible, version-conflicting, and checksum-conflicting observations are reported with purpose-specific precedence and no automatic winner.
- [x] `P3-14` Version state distinguishes working, locally published, cached, and remotely published evidence, including snapshot and release identities.

## Security, Refresh, and Operations

- [x] `P3-20` Remote origins and local roots use explicit authorization; canonical path, traversal, symlink-escape, credential, and diagnostic-sanitization cases are covered.
- [x] `P3-21` Every input adapter has bounded discovery and refresh work, observable freshness, and an explicit last-known-good policy where caching applies.
- [x] `P3-22` Source-aware search, filters, version selection, and diagnostics are available through the read-only CBD/MCP surface without publishing administration mutations.

## AI Ergonomics

- [x] `P3-30` Requirement matching can cite BoK semantic evidence separately from CBD catalog, development-directory, and CAR artifact evidence.
- [x] `P3-31` Intent-aware usage guidance identifies selected sources and versions and marks model inference separately from observed facts.
- [x] `P3-32` Conflicting or insufficient evidence produces bounded alternatives or explicit absence rather than fabricated catalog facts or a hidden source winner.

## Documentation and Verification

- [x] `P3-40` README, user guide, reference manual, strategy, phase, and static specifications describe the completed Phase 3 behavior and source precedence.
- [x] `P3-41` Executable specifications cover all five input-source kinds, source authorization, local path safety, freshness, version-state reconciliation, conflicts, evidence citation, and inference boundaries.
- [ ] `P3-42` Full tests, CAR build, CML lint, CAR lint, and representative source-aware MCP projection pass with Phase 3 contracts.
