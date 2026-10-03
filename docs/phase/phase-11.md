# Phase 11 - Capability Catalog, Discovery, and Traceability

Stage Status:
- Current status: OPEN
- Predecessor: Phase 10.7
- Development item: DEV-CBD-003
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-11-checklist.md` with
  reproducible evidence.

## Purpose

Consume the CNCF Component Capability projection and make Capability
discoverable, comparable, and traceable through Textus CBD Support. Capability
remains a non-instantiated model IR; Phase 11 presents and queries its semantic
contract without executing it or reconstructing CML.

## Work stack

| ID | Outcome | Status |
| --- | --- | --- |
| CAP-11-01 | Freeze supported projection versions, ingestion, integrity, and historical/current snapshot semantics. | planned |
| CAP-11-02 | Extend internal catalog/index models for Capability and realization traceability. | planned |
| CAP-11-03 | Provide Capability search, provider matching, and gap/compatibility diagnostics. | planned |
| CAP-11-04 | Provide Capability overview/detail and realization/evidence views. | planned |
| CAP-11-05 | Expose consistent Web and MCP/API semantics and prove the end-to-end fixture. | planned |

## Ownership boundary

- Consume only CNCF-admitted Capability projections and versions.
- Preserve exact Component, package, source, Capability, realization, and ABI
  identity.
- Keep current selected snapshots distinct from historical attempts.
- Keep Capability, Availability, Authorization, Guard, realization, and
  execution Evidence as separate concepts.
- Do not parse CML, define Capability syntax, execute Capability, or replace
  upstream identity with cbd-support-local identity.

## Completion conditions

- Design/specification define supported versions, ingestion integrity,
  snapshot/history behavior, queries, views, and failure diagnostics.
- Executable specifications cover valid ingestion and rejection/quarantine of
  ambiguous, duplicate, malformed, stale, and incompatible projections.
- Search finds exact provided/required Capability identities, providers,
  requirements, and gaps without display-name inference.
- Views expose realization and Specification/Evidence traceability while
  avoiding false runtime availability or execution claims.
- Web and MCP/API surfaces return consistent semantics for a real Cozy -> CNCF
  fixture with exact provenance.

## Non-goals

- Capability source authoring or CML parsing.
- CNCF Capability contract or ABI ownership.
- Runtime Capability execution, Authorization decisions, or Guard evaluation.
- AI-based semantic merging of unrelated Capability identities.
- Closing CNCF Phase 78 or Cozy Phase 64 from this repository.

## References

- [Development note](../notes/capability-catalog-and-traceability-proposal.md)
- [Development item journal](../journal/2026/09/2026-09-16-capability-catalog-development-item.md)
- [Phase 11 Checklist](phase-11-checklist.md)
