# Phase 10 - Durable Model Development and Continuation

Stage Status:
- Current status: PLANNED
- Predecessor: Phase 9 - Evidence-Backed Component Composition and Dashboard
- Development item: DEV-CBD-002
- Current step: begin only after Phase 9 establishes accepted canonical-model,
  projection, candidate-design, and semantic-diff contracts required by this
  phase.
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-10-checklist.md` with
  reproducible evidence.

## Purpose

Phase 10 makes the model-development work established by Phase 9 durable,
reviewable, resumable, and safely applicable to canonical CML.

Phase 9 establishes how CBD Support acquires an attributable Canonical Component
Design Model and projects it into stakeholder and engineering views such as
Mono-Koto, Use Case, Entity, Structure, Classification, Event, Workflow, and
StateMachine. It also establishes candidate design, semantic diff, review, and
Git-governed acceptance boundaries.

Phase 10 does not replace those contracts. It adds a project-owned continuation
boundary so that a reviewed modeling workflow can be stopped, committed,
transferred to another process or developer, rehydrated, checked for drift, and
continued without relying on chat history, provider session state, disposable
`target/` output, or an available CBD Support database.

The provisional project source root is:

```text
src/main/internal-model/
```

## Phase 9 to Phase 10 continuity

The intended lifecycle is:

```text
Canonical CML + admitted evidence
              |
              v
Canonical Component Design Model             Phase 9
              |
     +--------+---------+
     |                  |
 stakeholder views   engineering views
 Mono-Koto / UseCase  Entity / Event / Structure /
                      Classification / Workflow /
                      StateMachine
     |                  |
     +--------+---------+
              v
 stakeholder/design feedback
              v
Candidate Design Model
              v
Semantic Diff + Candidate Review
              |
-------------- durable handoff ----------------------
              |
              v
Internal Model Package                        Phase 10
  - source snapshots
  - selected realization/model state
  - decisions and open issues
  - candidate CML projection
  - approval bound to exact content hash
  - validation evidence
  - continuation cursor
              |
              v
rehydrated Canonical Component Design Model
              |
              +--> same Phase 9 projections
              |
              v
approved CML change gate
              v
Git-governed canonical-source change
```

The continuity rule is that Phase 10 persists identities and semantics already
established by Phase 9. It must not invent a second Entity/Event/Workflow model,
a second Mono-Koto model, or a storage-only interpretation of the design.
Rehydration must reconstruct the same semantic identities needed by Phase 9
projections, or report an explicit incompatibility/gap.

## Storage layers

Phase 10 retains the three-layer direction recorded by DEV-CBD-002.

### Project-owned source

`src/main/internal-model/` is the portable continuation package intentionally
versioned with the project. It contains the minimum complete semantic state,
source basis, decisions, approval, validation, and continuation information
required to resume work.

### CBD Support retained state

CBD Support may retain richer review runs, alternatives, proposals, semantic
diffs, evidence references, collaboration history, and supersession history.
This is an audit/collaboration store, not the sole resume authority.

### Disposable working output

`target/cbd-support/` contains reproducible caches, tentative extraction output,
previews, and other non-authoritative working artifacts.

## Integrity and authority principles

- CML remains canonical where the design is CML-owned.
- An internal model is not authoritative merely because it is committed.
- Stable identities and exact content hashes bind decisions and approvals.
- The package fails closed when required artifacts or hashes are missing or
  inconsistent.
- Source snapshots preserve the semantic basis of a decision but do not replace
  live canonical sources.
- Drift is detected and surfaced; it is never silently rebased.
- Human approval is bound to an exact model/projection revision and hash.
- No skill or CBD Support operation may apply a CML mutation without applicable
  approval for the exact candidate state.
- Provider output remains attributable evidence/proposal and cannot become a
  human decision implicitly.

## Provisional package roles

```text
src/main/internal-model/
  manifest.yaml
  resume.yaml
  sources/
    scenario.yaml
    model-context.yaml
    glossary.yaml
    cml-baseline.yaml
  decisions.yaml
  open-issues.yaml
  usecase-realization/
    <usecase-id>/
      realization.yaml
      cml-projection.yaml
      approval.yaml
      validation.yaml
```

Exact names and serialization are finalized only through Phase 10 design/spec
work. The semantic roles are the starting contract.

## Scope

1. Promote the stable DEV-CBD-002 package, identity, integrity, lifecycle, and
   source-root direction into design and specification.
2. Define self-contained source snapshots with provenance and freshness checks.
3. Persist selected realization/design state using Phase 9 semantic identities
   and traceability rather than a parallel storage-only metamodel.
4. Persist human decisions, relevant rejected alternatives, assumptions, and
   blocking/non-blocking open issues.
5. Persist candidate CML projection and semantic-diff identity without treating
   the projection as permission to mutate canonical source.
6. Bind review and human approval to exact content hashes and define
   supersession/invalidation behavior.
7. Define `resume.yaml` as a derived continuation cursor with current stage,
   last completed action, next permitted action, blockers, preconditions, and
   acceptance criteria.
8. Rehydrate a fresh CBD Support process from the package and reconstruct the
   semantic state required by the Phase 9 projection architecture.
9. Detect live Scenario, model, glossary/BoK, CML baseline, mapping-rule, and
   projection drift and invalidate stale decisions/approvals explicitly.
10. Gate skill-applied CML changes on exact applicable approval, then validate
    the resulting design through the normal Cozy/CBD Support path.
11. Integrate project-owned continuation state with richer CBD Support retained
    history without making retained state mandatory for resume.
12. Guarantee build, runtime, CAR/SAR publication, ordinary CML transformation,
    and public documentation exclusion unless an explicit approved workflow
    selects the internal model.

## Non-goals

- Replacing Phase 9 projection/view contracts with stored copies.
- Making `src/main/internal-model/` a second canonical design source.
- Reconstructing missing decisions from chat, Git history, naming heuristics, or
  provider output.
- Automatically approving semantic changes or CML mutations.
- Publishing raw prompts/responses or sensitive provider payloads into project
  source by default.
- Requiring CBD Support retained state merely to resume current selected work.
- Treating a stale approval as applicable after semantic or baseline drift.

## Stages

Each Stage is intended to remain a focused work slice. Split a checklist item
before implementation when it cannot reasonably remain within the project's
normal approximately six-hour phase/subphase work unit.

### Stage 10.1: Package and integrity contract

Checklist basis: `P10-01` through `P10-04`.

Freeze source-root role, manifest, package identity/revision, schema version,
artifact inventory, hashes, and fail-closed integrity behavior.

### Stage 10.2: Source snapshots and freshness

Checklist basis: `P10-10` through `P10-13`.

Define Scenario, model-context, glossary/BoK, and CML-baseline snapshots with
canonical provenance and deterministic drift/freshness checks.

### Stage 10.3: Durable semantic state and traceability

Checklist basis: `P10-20` through `P10-23`.

Persist selected realization/design state, Phase 9 semantic identities, trace
links, decisions, alternatives, assumptions, and open issues without creating a
parallel semantic model.

### Stage 10.4: CML projection, review, and approval

Checklist basis: `P10-30` through `P10-34`.

Persist candidate CML projection and semantic diff, bind review/approval to exact
hashes, and define supersession and invalidation.

### Stage 10.5: Continuation and rehydration

Checklist basis: `P10-40` through `P10-44`.

Implement the continuation cursor and prove that a fresh CBD Support process can
reconstruct the current semantic state and Phase 9 views without previous
session state.

### Stage 10.6: Drift and CML change gate

Checklist basis: `P10-50` through `P10-54`.

Detect drift, calculate invalidation, enforce exact approval before mutation,
and validate approved CML changes through canonical tooling.

### Stage 10.7: Retained-state and security integration

Checklist basis: `P10-60` through `P10-63`.

Integrate richer CBD Support history, redaction/sensitive-data policy, MCP/API
boundaries, and build/publication exclusion without weakening project-local
resume independence.

### Stage 10.8: End-to-end validation and closure

Checklist basis: `P10-70` through `P10-73`.

Prove the complete Phase 9 -> durable handoff -> Phase 10 resume -> approved
CML change -> Phase 9 re-projection loop, synchronize documentation, and close
only after all ledger items are complete or explicitly relocated.

## Acceptance scenario

The representative closure scenario is:

```text
Use Case / Mono-Koto stakeholder review
  -> Phase 9 Candidate Design Model
  -> Entity/Event/Workflow/StateMachine impact
  -> Semantic Diff
  -> project-local internal-model package
  -> process/session termination
  -> fresh CBD Support process
  -> package integrity and freshness validation
  -> same selected semantic state and Phase 9 projections
  -> human approval for exact candidate hash
  -> approved CML change
  -> Cozy generation/validation
  -> CBD Support re-analysis and Review
  -> updated Phase 9 projections
```

The phase is not complete unless this loop is reproducible without relying on
the original AI/provider session.

## Planning sources

- `docs/journal/2026/08/2026-08-17-project-internal-model-storage-direction.md`
- `docs/phase/phase-9.md`
- `docs/phase/phase-9-checklist.md`
- `docs/journal/2026/09/2026-09-09-mono-koto-analysis-view.md`
- `docs/journal/2026/09/2026-09-03-cml-design-improvement-pull-request-loop.md`
  when present under the synchronized journal history.

## Cross-project dependencies

Phase 10 consumes the semantic metadata and canonical-source behavior admitted
by Phase 9. Missing Cozy or CNCF contracts remain explicit upstream gaps and do
not authorize local semantic reconstruction. Any dedicated skill that applies
approved CML changes must preserve the approval/hash gate defined here and the
Git-governed acceptance policy established by Phase 9.
