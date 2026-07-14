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

### Phase 3: Generative AI CBD Ergonomics

Improve requirement matching, intent-aware usage guidance, evidence citation,
and Codex integration without turning inferred advice into catalog fact.

### Phase 4: Runtime Hardening

Add authentication, bounded caching, production refresh policy, SAR composition
tests, compatibility governance, and release/publish evidence.

## Current Priority

Phase 1 is complete and Phase 2 is in progress. Current slices provide bounded,
same-catalog dependency graph resolution, version-specific profile projection,
and finite-lifetime catalog snapshots with observable refresh and stale-cache
state. The remaining Phase 2 boundary is richer default-catalog evidence and
source authorization, followed by full documentation and verification closure,
without changing the SIE/CBD ownership split.
