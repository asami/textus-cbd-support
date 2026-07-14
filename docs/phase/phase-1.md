# Phase 1: CBD Support Extraction Baseline

Stage Status:
- Current status: DONE
- Current step: All Phase 1 checklist items have verification evidence.
- Owner: Textus CBD development
- Update rule: Update after each checklist item obtains reproducible evidence; closure is based only on `phase-1-checklist.md`.

## Purpose

Establish `textus-cbd-support` as an independent CAR and move detailed
component-development discovery out of SIE while preserving SIE's BoK-managed
component existence references.

## Scope

- Cozy scaffold using Scala 3.3.8 and project.yaml-driven build metadata.
- Default simplemodeling.org and configured catalog sources, with Cozy
  repository-index and deployed publication-catalog compatibility.
- Cozy CAR/SAR index and model-metadata parsing.
- Search, exact lookup, usage, dependency, source-state, and status operations.
- Service/operation-level MCP readiness with runtime disable policy.
- Shared `ComponentReference` handoff contract.
- SIE reference-only component MCP responses.
- Unit, component, CAR packaging, lint, and representative MCP projection tests.

## Non-Goals

- Copying catalog detail into SIE knowledge storage.
- Inventing data absent from catalog evidence.
- Provider-side catalog mutation through MCP.
- Advanced recommendation ranking or automatic dependency installation.
- Production authentication and refresh scheduling.

## Verification Evidence

- CNCF full test: 1668 tests passed on 2026-07-14 after the qualified
  service/operation readiness contract was added.
- CBD focused test: 6 tests passed on 2026-07-14.
- CBD CAR build: `target/textus-cbd-support-0.1.0-SNAPSHOT.car` generated on
  2026-07-14.
- SIE focused `ComponentFactorySpec`: 22 tests passed on 2026-07-14.
- Final SIE verification: 74 tests passed and
  `target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car` was generated
  on 2026-07-14.
- Final CBD verification: 13 tests passed and
  `target/textus-cbd-support-0.1.0-SNAPSHOT.car` was generated on 2026-07-14.
- CNCF MCP policy verification: 11 focused tests passed on 2026-07-14; the
  final full CNCF run passed 1668 tests.
- Cozy MCP-ready scaffold and operation-description verification: 41 focused
  tests and the full 495-test suite passed on 2026-07-14.
- simple-modeler Scala 3 generation verification: the full 26-test suite
  passed, and clean CBD/SIE generation compiled under Scala 3.3.8 on
  2026-07-14.
- CAR lint: no FAIL findings for CBD Support or SIE. Both generated CAR files
  contain `abi-manifest.json`; lint retains a non-blocking warning because no
  project-local release ABI baseline has been committed.
- MCP projection: CBD exposes exactly 6 `CbdRetrieval` tools; SIE exposes
  exactly 7 selected `SemanticRetrieval` knowledge/reference tools.
- Live CNCF `/mcp` verification on 2026-07-14 loaded four published CAR
  profiles from simplemodeling.org, matched `semantic integration`, resolved
  `org.textus:textus-semantic-integration-engine`, and returned authoritative
  catalog, CAR artifact, and documentation references from `getUsage`. The six
  published CBD tools exposed their CML descriptions and typed input schemas.
- CNCF MCP argument bridge verification: 9 focused tests passed after named
  JSON arguments were normalized once as CML operation properties.

## Closure Basis

Phase 1 is DONE only when every item in `phase-1-checklist.md` is `[x]` and the
final full-test, CAR-build, lint, and MCP projection evidence is recorded here.
