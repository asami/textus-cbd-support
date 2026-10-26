# Phase 15.1: ReviewRequest Provider and External AI Execution Boundary

Status: CLOSED
Planned: 2026-10-18; relocated from the original same-day Phase 15 plan
Depends on: accepted bounded Phase 15 scoped evaluation contract
Consumer: sm-workflow Phase 10 (CBD review integration)
Checklist: [Phase 15.1](phase-15.1-checklist.md)
Design: [Review execution ownership](../design/scoped-review-execution-boundary.md)
Contract: [ReviewRequest execution](../spec/scoped-review-execution-contract.md)

## Goal

Separate preparation and result acceptance from AI execution around the existing
Phase 15 evaluation semantics. Both orchestrated and standalone execution consume
one structured ReviewRequest and one result contract. Prompt text is a rendering,
not the API's only representation. No duplicated review policy is introduced.

## Work and main-route order

1. P15.1-RR01: extract the common ReviewRequest builder from existing scoped
   review semantics, preserving checks, deterministic findings and result rules.
2. P15.1-RR02: expose request preparation through normal CML/Operation wiring;
   orchestrated preparation invokes no AI and copies no worker conversation.
3. P15.1-RR04: accept an external typed result for that exact review/subject/scope
   and return the Phase 15 evaluation result with actual execution attribution.
4. P15.1-RR05 (external portion): validate the connected external route, zero CBD
   AI invocation and typed outcomes before its owner acceptance and handoff.
5. P15.1-RR06: deliver the bounded prepare -> external execution -> accept result
   handoff for sm-workflow Phase 10's existing Review/Submit/Advance route.
6. P15.1-RR03: adapt explicit standalone execution to use the same prepared
   request and result acceptance through the existing CBD runner.
7. P15.1-RR05 (remaining portion): verify standalone behavior and applicable
   regressions, then complete required independent Phase acceptance and closure.

Group this work into two implementation packages. The first is one vertical
external slice (RR01/02/04, RR05 external, RR06), from normal Operation preparation
to accepted ordinary result. The second adapts standalone to that proven slice
(RR03 and remaining RR05). Do not build all helpers before connecting the route,
or postpone its validation until the standalone package. RR05 remains one work
item and is complete only when both portions pass; RR06 can complete earlier.

Settle shared request/result types and the actual affected CML/caller signatures
against Phase 15 once, then update affected production callers directly in their
owning work packages. Avoid temporary duplicate APIs. Consumer connection/closure
remains sm-workflow-owned; declaring its target contract does not rewrite active
consumer work or accept its workflow.

IDs preserve the corresponding former P15-RR01..06 obligations; execution order
places RR04 before RR03 intentionally. RR06 now feeds sm-workflow Phase 10.
The earlier same-day 6.1-first allocation is superseded by the production-first
decision, without changing producer work IDs. The full mapping and retained
original scope are in the [handoff](../journal/2026/10/2026-10-18-phase-15-15.1-review-handoff.md).

## Acceptance and dependencies

Prepare one representative scoped CAR semantic review, execute it externally,
accept its typed response and expose the ordinary result for CAM consumption.
CBD makes zero AI-provider calls on this external route, including result
acceptance. Explicit standalone execution uses the same criteria/result contract
and the selected existing runner. Zero findings can be a valid covered result;
incomplete, failed or wrong-target output cannot become PASS.

Use a fresh external execution result for representative behavior; fixture output
alone does not prove connected execution. Record actual same-context versus
explicit independent selection without waiving required independent review.
CBD owns neither the caller's conversation nor lifecycle/commit decisions.

The main external route can be handed off after bounded owner acceptance before
the standalone work closes, but this Phase itself remains open until both routes
and required checks are accepted. sm-workflow Phase 10 awaits the necessary
accepted Phase 15 and 15.1 handoffs,
not unrelated provider roadmaps. Initial-use 6.1/6.2/6.3 no longer waits for CBD;
6.3 keeps its aggregate full-validation obligation. Phase 10 owns CBD-connected
repair/integration/Final validation. CBD closure does not depend on consumer closure.

## Validation and handoff allocation

| Boundary | Minimum evidence and reuse |
| --- | --- |
| External slice, before RR06 | One representative fresh external execution through prepare/accept Operations; zero CBD AI calls in both; actual executor attribution and ordinary typed result. Focused cases cover clean/blocking findings, wrong-target output, missing/failed coverage and preservation of deterministic findings. |
| Standalone package | Execute one representative case through the existing runner using the same builder/acceptance. Extend existing focused cases only for changed standalone wiring; reuse common result-rule evidence. |
| Phase closure | Reconcile the two packages, applicable regressions and existing required full-validation/independent-review obligations. Do not create a second acceptance cycle per helper, check or work ID. |
| sm-workflow consumer | Phase 10 uses accepted CBD15 and RR06 handoffs for actual Review/Submit/Advance/CAM connection. Consumer acceptance is recorded there; CBD does not wait for its full Phase closure. |

RR06 records actual Operation names/signatures, request/result examples, execution
and focused evidence references, supported checks/limitations and bounded owner
acceptance in the existing handoff journal. Required independent review remains
independent; use applicable accepted evidence at Phase closure instead of rerunning
an unchanged slice by default. An authored contract or fixture-only result cannot
substitute for this handoff. This plan does not add a receipt, ledger or integrity
control, and does not waive any existing required full-validation obligation.

The [2026-10-27 connected external example](../journal/2026/10/2026-10-18-phase-15-15.1-review-handoff.md#connected-external-execution--2026-10-27)
now demonstrates the existing RR01/02/04 Operations using actual supplied source,
an explicitly independent external executor and typed acceptance. Both Operation
runs reported zero CBD AI calls; all five selected checks were covered with zero
findings. Native focused validation and independent Step review then accepted
RR06 for sm-workflow Phase 10 at Step commit
`c4cc0e78162554b1ef489148f3e19bf813cdcac7`. The standalone package now retains
the existing product route and adds explicit connection qualification through
the actual optional Textus AI provider. Actual standalone attempt 04 completed
with observed `gemma` / `qwen3.5:4b` attribution, exact selected-path coverage and
no findings. Its explicit limitation concerns the absence of selected model
subjects and indirect behavioral validation; it does not claim whole-provider
or whole-repository review.

Native focused revision 165 passed 18 examples in three suites; whole-Phase
revision 176 passed all 1088 tests in 136 suites. Independent Phase review
`cbd151-phase-independent-review-20261027@1` was accepted without blockers.
The standalone Step is committed as
`ef9e1379fe50ff5fb35c749c27e1c9a986ceb9e7`; Session
`1e73a8e5a101225d3eefe2fdc8329cf2` returned `Terminal / completed`.
The [completion record](../journal/2026/10/2026-10-18-phase-15-15.1-review-handoff.md#standalone-qualification-and-phase-completion--2026-10-27)
retains actual evidence references, failures and execution limits. All checklist
obligations are complete. GitHub synchronization was separately deferred and
not attempted; consumer Phase 10 acceptance remains consumer-owned.

Preserve existing code and evidence as the starting point; change affected
production callers directly and use compiler errors to expose missed updates.
No compatibility aliases, legacy fallback or parallel policy implementation.
No conversation reconstruction, new persistence/Job framework, retries/replay,
duplicate-delivery machinery or management integrity controls are required.
Model escalation, A/B evaluation and consumer routing remain outside this Phase.
