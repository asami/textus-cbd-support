# Phase 11.1 Checklist: Capability Views, Web/MCP, and Acceptance

Status: CLOSED
phase=[Phase 11.1](phase-11.1.md)
predecessor=[Phase 11](phase-11.md)
successor=[Phase 12](phase-12.md)
development-item=DEV-CBD-003

Split from Phase 11 on 2026-10-04. This ledger owns CAP-11-04–05 exactly once,
with their original requirements and IDs. Consume the accepted Phase 11
ingestion/catalog/query handoff. The dated acceptance section below records
current progress; the split itself was not implementation evidence.

## CAP-11-04: Capability and demand trace views

Stage Status:
- Current status: CLOSED
- Owner: Component Dashboard owner
- Update rule: Close only after overview/detail views expose provenance and
  traceability without implying runtime execution or authorization, with every
  item in this Stage's checklist checked on its accepted Step evidence.

- [x] Expose separate Component Usage and Component contract views, preserving
      inductive evidence, deductive guarantees and explicit many-to-many links.
- [x] Remove old Application API/label/fixture names with no compatibility aliases;
      compile regenerated callers and use traceComponentUsageCapability.
- [x] Add Component Capability overview and detail projections.
- [x] Add a read-only demand trace projection:
      Use Case -> Component Usage Capability -> Component Capability -> realization.
- [x] Navigate between admitted Use Case/Component Usage Capability, Capability,
      Component, and realization targets using exact references.
- [x] Show source/version/provenance and Specification/Evidence links.
- [x] Render typed gaps independently; do not collapse missing realization,
      unavailable deployment, unauthorized principal, unsatisfied Guard and
      missing Evidence into a generic unavailable state.
- [x] Clearly label absent upstream Use Case demand projection as unavailable,
      not as an empty/no-demand Use Case.
- [x] Add view-model tests proving Web rendering cannot manufacture trace links
      absent from the query result.

## CAP-11-05: Web/MCP consistency and end-to-end acceptance

Stage Status:
- Current status: CLOSED
- Owner: CBD Support Web/MCP owners with CNCF Phase 78 and Cozy Phase 64
- Update rule: Close only after Web and MCP/API expose the same semantics for
  one real upstream fixture with reproducible evidence, with every item in
  this Stage's checklist checked on its accepted Step evidence.

- [x] Consume one Cozy-generated, CNCF-admitted Capability projection.
- [x] Consume CAP-11-PKG01's real owning Directory/CAR handoff, including
      packaged Component Usage metadata and semantic parity. Develop against the
      Directory directly; do not require CAR creation for routine reads or
      substitute a separately supplied legacy Application JSON file.
- [x] When an admitted demand fixture is available, include at least one
      Use Case -> Component Usage Capability -> Component Capability -> explicit
      realization path and one typed gap path.
- [x] Record the supported or unavailable demand branch explicitly. If no real
      demand fixture is admitted, verify unavailable behavior and identify the
      unexercised positive integration; do not label synthetic proof as upstream
      integration. The real Component Capability fixture remains mandatory.
- [x] Verify Web and MCP/API return the same qualified identities, provider sets,
      realization sets, gap kinds and provenance for identical queries.
- [x] Prove no CML reparsing, AI reconstruction or local identity replacement occurs.
- [x] Prove the split sequence's surfaces are read/query only and cannot create Use Case
      Slice, approve candidate state or mutate canonical CML.
- [x] Record exact Cozy, CNCF ABI, demand-projection producer and cbd-support
      revisions plus focused validation and review evidence.

## Current Step acceptance — 2026-10-16

CAP-11-04 commit `10ed60450a520da1e066cd3a9445be1eca45f606` passed its
35-test, three-suite view/query/diagnosis accumulator and independent Step
review. CAP-11-05 commit `e487f7d47f0fae490247a347050df861f6ada8cb` passed
62 tests in seven suites, including actual generated API/MCP dispatch and
native form rendering. Its independent Step review and subsequent focused
review closed all three bounded selector, ABI and consumer findings. The
subsequent normal CAR build passed mandatory ScalaDoc and packaging; the
existing ABI checker verified 26 operations, eight entities and package/source
equality. CAR lint has no FAIL and eight retained nonblocking warnings.

The real producer inputs are Cozy `0.3.3-SNAPSHOT` at
`834198b5c864c1599f29bc84effad2cc64146031` and CNCF `0.5.3-SNAPSHOT` at
`8c9e502a6c420eb7c4386f5198fd6b9b09995d76`, using their existing Phase 11
checkouts. Producer Directory/CAR acquisition and typed source admission run
through the existing producer integration executable specifications. No real
forward UseCase demand projection is admitted; that branch is explicitly
unavailable and positive integration is unexercised. The conditional demand
item above is satisfied by this selected unavailable branch under the Phase's
original acceptance rule, not by claiming a positive producer scenario.

Closure basis: the two Stage checklists, accepted Step commits, specifications
in `capability-views-contract.md` and `capability-read-surface-contract.md`, and
their executable specifications. Live composed SAR, CLI/HTTP help, manual and
OpenAPI routes have not been exercised. The final closure section below records the completed aggregate gate and
release boundary; these Step records retain their original assurance scope.

## Closure

- [x] Verify Phase 11's committed deferred-validation handoff and ancestry.
- [x] Close CAP-11-04–05 only after the mandatory real Component Capability
      fixture and the explicitly selected supported/unavailable demand branch.
- [x] Run the repository full SBT suite once on this final release tree for
      the complete PHASE-11 -> PHASE-11.1 sequence; record actual results.
- [x] Complete focused validation, independent Step and full Phase review,
      applicable CAR lint, closure journals and this Phase's release commit.
- [x] Synchronize the split sequence's completion projections and DEV-CBD-003
      status without claiming that Phase 12 or upstream Phases have closed.

## Phase closure — 2026-10-16

The committed Phase 11 deferred-validation handoff and native ancestry are
accepted. The complete repository SBT suite passed for the declared
`PHASE-11 -> PHASE-11.1` sequence after the test-harness repairs. Independent
Step reviews, the sole full Phase review and both focused closure repair
reviews are accepted with no current blocker. Applicable CAR lint and the
canonical Hygiene journal are reconciled; no Development Candidate journal
is required because no such findings were accepted.

The final local release is bound by
`Phase-Closure-Binding: cbd111-phase-closure-20261016-01@1`. The prepared closed
state takes effect only with that distinct commit and its verified closure
receipt. Detailed execution evidence remains local. The Phase index and
strategy project DEV-CBD-003 as closed; Phases 12, 13 and 14 remain planned.
The unavailable demand branch and unexercised live runtime routes stated
above remain explicit limitations.
