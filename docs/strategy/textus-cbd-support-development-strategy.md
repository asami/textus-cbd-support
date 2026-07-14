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

Phase 1 and Phase 2 are complete. Phase 2 provides bounded,
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
glossary resources. The next slice adds SIE-mediated BoK input through SIE's
public component contract before projecting all sources through search and MCP.
