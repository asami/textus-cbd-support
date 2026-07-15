# Phase 4 Checklist: Runtime Hardening

This checklist is the authoritative Phase 4 state ledger. Items are never
deleted; a checked item requires observable evidence.

## Authentication and Credential Lifecycle

- [x] `P4-01` Every authenticated remote source uses an explicit authentication scheme and credential reference without storing a secret value in its URI, descriptor, state, diagnostic, MCP output, or CAR content.
- [x] `P4-02` Credential references are resolved through the CNCF provider/configuration boundary only when an outbound request is executed, and authentication headers remain scoped to the owning source and authorized origin.
- [x] `P4-03` Missing, unavailable, expired, and rejected credentials produce distinct sanitized source failures without fallback to another source credential or unbounded retry.
- [x] `P4-04` Executable security specifications prove credential isolation, header scoping, redaction, and CallTree-safe request metadata for catalog, BoK-site, and SIE-mediated inputs.

## Production Refresh and Cache Policy

- [x] `P4-10` Catalog and BoK sources have an explicit production refresh policy with bounded schedule intervals and observable next-attempt state.
- [x] `P4-11` Automatic refresh uses bounded retry/backoff and concurrency control, including single-flight behavior for the same source and protection against synchronized refresh bursts.
- [ ] `P4-12` Snapshot retention has explicit source-count, observation-count, and memory/work bounds while preserving attributable last-known-good evidence.
- [ ] `P4-13` Authentication, transport, parse, and compatibility failures have tested refresh-state transitions and never disguise stale evidence as current.

## SAR Composition and Runtime Projection

- [ ] `P4-20` A representative SAR composes CBD Support and SIE and exposes their selected read-only tools through one live CNCF `/mcp` endpoint without exposing either component's administration operations.
- [ ] `P4-21` Runtime disable policy can narrow the composed MCP tool set globally, by service, and by operation without expanding component-declared readiness.
- [ ] `P4-22` Live composed retrieval preserves CBD catalog/local evidence and SIE semantic evidence as separate source-owned records with bounded failures and no hidden winner.

## Compatibility Governance

- [ ] `P4-30` Declared CNCF minimum, tested, and excluded versions are checked against a documented runtime compatibility matrix and representative execution evidence.
- [ ] `P4-31` CAR ABI baselines and compatibility checks distinguish compatible additions, breaking changes, and intentional version transitions.
- [ ] `P4-32` Catalog, BoK manifest, SIE contract, and local CAR compatibility decisions preserve supported older inputs explicitly and reject incompatible inputs without speculative fallback.

## Documentation, Release, and Verification

- [ ] `P4-40` README, user guide, reference manual, strategy, phase ledger, and static specifications describe authentication, production refresh/cache, SAR composition, compatibility, and operational failure behavior.
- [ ] `P4-41` Executable specifications cover every Phase 4 authentication, refresh, cache, composition, and compatibility contract.
- [ ] `P4-42` Full tests, CAR build, CML lint, CAR lint, representative SAR build, and live source-aware MCP projection pass with Phase 4 contracts.
- [ ] `P4-43` Publish-readiness evidence records ABI baseline status, dependency/runtime versions, artifact metadata, residual warnings, and the explicit manual publication procedure without publishing automatically.
