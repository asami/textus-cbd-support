# Textus CBD Support Development Strategy

## Objective

Provide a dedicated CAR whose MCP surface helps generative AI perform
Component-Based Development without coupling component catalog semantics to
SIE's BoK knowledge model.

## Development Philosophy

- Keep SIE terminology/component-existence knowledge separate from CBD detail.
- Read authoritative Cozy-generated catalog and model-metadata documents.
- Return evidence and explicit absence; never synthesize catalog facts.
- Keep the default simplemodeling.org catalog usable without SIE.
- Allow configured catalogs without merging their identities silently.
- Preserve a last known good snapshot when refresh fails.
- Publish read-only CBD operations through MCP and keep administration private.
- Keep project identity, Scala version, dependencies, and runtime compatibility
  authoritative in `project.yaml`; keep `build.sbt` declarative and small.

## Phase Overview

### Phase 1: CBD Support Extraction Baseline

Create the CAR, catalog provider, shared reference contract, MCP publication
policy, SIE reference-only boundary, documentation, and verification baseline.

### Phase 2: Catalog Fidelity and Resolution

Expand catalog schema coverage, version selection, dependency graph
resolution, conflict reporting, source authorization, caching, and refresh
observability using real published catalogs.

### Phase 3: Federated Development Context and AI Ergonomics

Add simplemodeling.org, configured BoK sites, SIE-mediated BoK knowledge,
configured development directories, and CAR versions in the local warehouse
and managed cache as distinct evidence-bearing input sources. Reconcile their
component and version observations without silently merging source identity,
then improve requirement matching, intent-aware usage guidance, evidence
citation, and Codex integration without turning inferred advice into catalog
fact.

### Phase 4: Runtime Hardening

Add authentication, bounded caching, production refresh policy, SAR composition
tests, compatibility governance, and release/publish evidence.

## Current Priority

Phase 1, Phase 2, and Phase 3 are complete. Phase 2 provides bounded,
same-catalog dependency graph resolution, version-specific profile projection,
and finite-lifetime catalog snapshots with observable refresh and stale-cache
state. Additional catalog sources now require explicit exact-origin
authorization and expose rejected configuration without network access. The
default public source's missing rich indexes are recorded as a publisher-owned
future candidate with deployment acceptance gates. Cozy schema fidelity is now
fixed to revision-pinned producer evidence, including runtime ranges, archive
checksums, nested ABI dependencies, sidecars, and diagnostics. Full tests, CAR
build, CML lint, CAR lint, and representative MCP projection close the phase
without changing the SIE/CBD ownership split. Phase 3 now has a unified
source/observation vocabulary plus bounded, read-only adapters for explicitly
configured development directories and CAR artifacts in the local warehouse
and managed cache. Authorized BoK sites now enter the live runtime only through
the Cozy `cncf.knowledge-source.v1` manifest and bounded machine-readable
glossary resources. SIE-mediated BoK input now enters through SIE's public
typed MCP component contract with exact-route authorization, bounded
responses, and mandatory evidence while keeping CBD component profiles
separate. Source-preserving reconciliation now reports duplicate, missing,
stale, incompatible, version, and checksum conflicts with purpose-specific
authority tiers and no automatic winner. Version-state reconciliation now
keeps working, locally published, cached, and remotely published availability
separate from release, snapshot, unknown, and conflicting maturity, including
all declared catalog identities and no implicit latest winner. Phase 3 has now
hardened remote-origin, derived-fetch, local-root, traversal,
symlink-escape, credential, and diagnostic-sanitization boundaries. Read-only
source-aware search now keeps catalog profiles separate from working and
local/cache observations, exposes bounded source, freshness, availability,
conflict, and purpose filters, reports participating evidence and precedence,
and does not select a hidden winner. The runtime also projects configured local
inputs through search, `listCatalogs`, and `status` without adding a publishing
administration mutation. Requirement matching now cites BoK semantic evidence
separately from CBD catalog and local evidence. BoK sites use deterministic
published-field matching, SIE retains its own match metadata for the current
query, and catalog components reference citations only through explicitly
equal published terms/tags without profile completion. Intent-aware
`getUsage` now reports the selected catalog source and version, separates
observed selection facts from deterministic operation inference, reserves a
distinct label for actual model inference, and emits no operation candidate
without explicit intent overlap. Exact component, usage, and dependency
retrieval now returns bounded catalog alternatives instead of a priority-based
hidden winner and uses attributable absence records for missing selection,
operation, intent, source, version, or dependency evidence. README, user guide,
CAR reference, strategy, phase ledger, and static contracts now share one
source-role and purpose-precedence model. Executable specifications now cover
all five source kinds, their authorization and freshness boundaries, local path
safety, version reconciliation, conflicts, citations, and inference labels.
Phase 3 closure passed full tests, CAR build and descriptor inspection, CML
lint, CAR lint, and representative source-aware MCP projection. Phase 4 is now
defined by `docs/phase/phase-4.md` and `docs/phase/phase-4-checklist.md`.
P4-01 now provides bounded, source-owned authentication schemes and
configuration-key references without projecting credential identity. P4-02
now carries source ownership through every remote provider boundary, resolves
the referenced value only inside the outbound CNCF ProviderCall, and scopes
bearer, Basic, or API-key headers to that source's authorized origin without
placing credential identity or value in CallTree metadata. P4-03 now
distinguishes missing, resolver-unavailable, explicitly expired, and rejected
credentials with stable sanitized source-failure codes and no alternate-source
fallback or authentication retry. P4-04 now executes catalog, BoK-site, and
SIE-mediated authentication through the production ProviderCall/UnitOfWork/
HTTP-driver path and proves exact per-source headers, cross-origin refusal,
redaction, and CallTree-safe metadata. P4-10 now gives catalog and BoK sources
bounded one-minute-through-24-hour normal refresh schedules and exposes each
next attempt without immediate repeated work after failure. P4-11 adds bounded
exponential retry, same-source single-flight, and a fair runtime-wide concurrency
limit against synchronized bursts. P4-12 caps configured source count and
retained Catalog, BoK, SIE, and local observations through stable per-source
quotas, preserves bounded attributable last-known-good evidence, and retains
the pre-policy runtime construction signatures. P4-13 proves that
authentication, transport, parse, and compatibility failures all preserve
stale source-owned evidence until a successful bounded retry establishes a new
current observation. P4-20 builds the two snapshot CARs into a representative
`textus-cbd-sie` SAR and verifies one live CNCF `/mcp` endpoint whose exact
surface is six CBD retrieval tools and seven SIE semantic-retrieval tools with
no administration or mutation tools. P4-21 verifies four live composed
profiles: baseline `6/7`, global disable `0/0`, SIE service disable `6/0`, and
per-component status-operation disable `5/6`. Exact tool-set assertions and
disabled `tools/call` rejection prove that runtime policy only narrows declared
readiness. P4-22 now runs a repository-owned
loopback fixture through that composed endpoint and proves that published
catalog, development-directory, and SIE semantic evidence remain separate;
conflicting versions retain both participants with no selected winner even
when result limits apply; and a missing catalog remains degraded under its
bounded retry schedule. P4-30 now keeps
`project.yaml` as the declaration authority and checks it against a
machine-readable assessed-candidate matrix before any representative runtime
work. The only current candidate, `0.5.1-SNAPSHOT`, passed the complete composed
CBD/SIE SAR from clean CNCF revision
`848ef5596af6927512af4e9c8c0d423d4add1253`; excluded is explicitly empty and
unlisted versions remain unassessed. The next slice is P4-31 CAR ABI baseline
and compatible-addition/breaking-transition governance.
