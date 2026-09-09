# Phase 10 Planning: Durable Model Development and Continuation

**Date:** 2026-09-09
**Development item:** DEV-CBD-002
**Target:** Phase 10
**Status:** PLANNED

## Decision

DEV-CBD-002 is assigned to Phase 10 as the durable successor to Phase 9.

Phase 9 establishes the Canonical Component Design Model, stakeholder-facing
Mono-Koto and Use Case projections, engineering Entity/Event/Structure/
Classification/Workflow/StateMachine projections, semantic feedback, Candidate
Design Model, Semantic Diff, and candidate review/change proposal boundaries.

Phase 10 persists and resumes that work. It does not introduce a second semantic
model. The project-local internal-model package must preserve Phase 9 semantic
identities and be sufficient for a fresh CBD Support process to reconstruct the
selected semantic state and the same projection architecture.

## Continuity

```text
Phase 9
  Canonical Component Design Model
    -> stakeholder / engineering review
    -> semantic feedback
    -> Candidate Design Model
    -> Semantic Diff / Candidate Review
             |
             v
Phase 10
  src/main/internal-model/
    -> source snapshots
    -> selected semantic state
    -> decisions / open issues
    -> candidate CML projection
    -> review / exact-hash approval
    -> validation / resume cursor
             |
             v
  fresh-process rehydration
             |
             v
Phase 9 projections
             |
             v
  approved CML mutation gate
             |
             v
  Cozy/CBD validation and re-projection
```

Phase 9 remains responsible for model meaning and projection behavior. Phase 10
is responsible for durability, integrity, lifecycle, continuation, freshness,
approval binding, and the exact canonical-source mutation gate.

## Planning records

- `docs/phase/phase-10.md`
- `docs/phase/phase-10-checklist.md`
- `docs/phase/phase-9.md` now records the explicit successor/handoff boundary.
- `docs/journal/2026/08/2026-08-17-project-internal-model-storage-direction.md`
  is updated to assign DEV-CBD-002 to Phase 10.
- `docs/strategy/textus-cbd-support-development-strategy.md` records
  DEV-CBD-002 as Phase 10 planned work.

## Work-size rule

Phase 10 stages and checklist items are intended to remain focused work slices.
Any item that cannot reasonably remain within the project's normal roughly
six-hour phase/subphase unit should be split before implementation rather than
allowed to become an oversized stage.

This journal records planning authority only. Implementation acceptance remains
with the Phase 10 checklist and reproducible evidence.
