# Phase 15: Scoped Quality Review Contract and Provider

Status: IN_PROGRESS; existing bounded implementation and focused evidence retained, Phase acceptance open
Reconciled: 2026-10-18
Consumer: sm-workflow Phase 10 (CBD review integration)
Checklist: [Phase 15](phase-15-checklist.md)
Contract: [Scoped quality review](../spec/scoped-quality-review.md)
Successor: [Phase 15.1](phase-15.1.md), ReviewRequest and external execution

## Goal and ownership

Establish the common scoped quality evaluation expected by sm-workflow's
SQR-01..04 references: selected checks, meaningful findings, coverage and explicit
gaps. CBD owns review semantics; sm-workflow owns lifecycle timing, CAM Admission,
repair decisions and commit authority. STEP/PHASE/FINAL are consumer concepts.

This restores the earlier consumer meaning of Phase 15. The 2026-10-18
ReviewRequest plan formerly at this path moves to Phase 15.1 with its work IDs
mapped in the [handoff](../journal/2026/10/2026-10-18-phase-15-15.1-review-handoff.md).
SQR-01..04 are defined below by this reconciliation; no recovered historical
item definitions or prior completion are claimed. Phases 16 and 17 are unchanged.

## Retained starting point

Reuse `ScopedQualityReviewProvider`, the ordinary CML/private QUERY/ActionCall
route, its provider/operation specifications and the current typed result.
Existing sm-workflow CAM projections and focused checks are consumer evidence,
not proof of completed real execution. The recorded 16-test CBD focused result,
107-test consumer result and subsequent bootstrap/schema work retain their
original scope. Do not restart or recreate that implementation or its evidence.
No new tests or acceptance are recorded by this planning edit.

## Work

- SQR-01: settle the selected evaluation input/result contract, ordinary review
  and subject references, profiles/checks and separation from Admission authority.
- SQR-02: reconcile the existing CML structural and selected semantic evaluation
  (`semantic-consistency`, `implementation-quality`, `excessive-checks`,
  `integration`) against the contract; preserve supplied-material limits.
- SQR-03: establish typed findings, coverage, unsupported/failed outcomes and
  affected-scope re-evaluation. Preserve attributable finding identity where
  applicable; no hash/TTL/state-copy or general reuse engine.
- SQR-04: deliver the ordinary Operation result contract and bounded provider
  evidence for CAM consumption. Record actual coverage and limitations without
  waiting for the whole sm-workflow workflow or claiming its acceptance.

## Delivery order and acceptance

Execute two coherent work packages; SQR IDs are obligations, not four separate
implementation/review cycles.

| Order | Work package | Exit evidence |
| --- | --- | --- |
| 1 | Reconcile SQR-01..03 with the existing provider, Operation specs and consumer decoder. Settle the shared input/result and execution-attribution boundary needed by 15.1; list only actual gaps. | Bounded agreed contract and gap disposition, with existing evidence references and limits. This permits 15.1 implementation preparation, not consumer acceptance. |
| 2 | Fill those gaps through the existing normal Operation route and complete SQR-04. Validate changed behavior in the same package. | Accepted selected evaluation/result, focused and applicable regression evidence, and bounded owner handoff. |

Use the existing `ScopedQualityReviewProviderSpec` and
`ScopedQualityReviewOperationSpec` as the first validation surface. Reuse unchanged
behavior/evidence; add or change cases only for identified contract gaps. Keep the
four selected checks in scope without creating a new general profile registry or
four independent provider implementations. Do not expand standalone AI execution
as an intermediate solution that Phase 15.1 would immediately replace.

First settle SQR-01 and the relevant existing SQR-02/03 behavior. Hand that bounded
contract to Phase 15.1 for request preparation and external-result acceptance.
Reuse the current standalone runner for Phase 15 validation; do not expand a
CBD-owned AI route merely to finish sm-workflow's orchestrated acceptance.

Phase 15 accepts representative selected evaluation through the normal Operation
boundary, typed clean/blocking findings, explicit coverage gaps and usable
consumer handoff. Deterministic findings cannot be overridden by AI. Supplied
fixtures establish only their tested behavior; report actual runner execution
separately. No full legacy CAR Review migration, new Job lifecycle or runtime
orchestration framework is required.

Own focused validation, applicable regressions and independent Phase acceptance.
Retain any existing required CBD full-validation obligation; sm-workflow's
Phase 6.3 aggregate policy does not silently waive CBD checks. Reuse relevant
prior results according to actual changed inputs, never mark all inherited work
complete merely because source exists.

sm-workflow Phase 10's CBD integration awaits accepted required deliveries
from both Phase 15 and Phase 15.1; initial production use in 6.1–6.3 does not. A bounded accepted owner handoff
may unblock that connection before unrelated owner work closes. CBD acceptance
does not require sm-workflow Phase 10 closure, avoiding circular waits.

The handoff identifies the actual Operation/input/result contract, selected
checks and supported coverage, evidence references, open limitations and owner
acceptance. Reuse the normal journal/review records; no additional receipt format
or state store is introduced. Distinguish contract readiness, accepted delivery
and full Phase closure in those records.
