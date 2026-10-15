# Phase 11.1 - Capability Views, Web/MCP, and Acceptance

status=planned
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=none
validation_ownership=aggregate-final-owner
aggregate_validation_owner=PHASE-11.1
aggregate_validation_sequence=["PHASE-11","PHASE-11.1"]
depends_on=phase-11.md
successor=phase-12.md

Stage Status:
- Current status: PLANNED / NOT STARTED
- Predecessor: Phase 11
- Successor: Phase 12
- Development item: DEV-CBD-003
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-11.1-checklist.md` with reproducible evidence.

Split from Phase 11 by the apply-mode `$cncf-split-phase 11` invocation on
2026-10-04. The original Phase retains ingestion/catalog/query work and all
prior history. This new unit owns only the unfinished CAP-11-04 and CAP-11-05
requirements; it is not implementation or acceptance evidence.

## Purpose and scope

Make the accepted Capability query model understandable through shared read-only
views and consistent Web/MCP/API responses. Consume the
[Phase 11](phase-11.md) schema, source/version, reference, query, gap and
availability contract without redefining upstream semantics.

| ID | Outcome | Status |
| --- | --- | --- |
| CAP-11-04 | Capability overview/detail and demand-to-realization/evidence views | planned |
| CAP-11-05 | Consistent Web/MCP/API semantics and real producer/consumer acceptance | planned |

The [checklist](phase-11.1-checklist.md) retains the original unchecked items.
CAP-11-01–03 belong only to the predecessor. Phase 12 consumes this final
Capability presentation/acceptance handoff before interactive editing; its
identity and independent closure criteria are unchanged.

## Package route dependency — 2026-10-15

Consume corrected CAP-11-PKG01 with CNCF CAP-78-PKG01 / Cozy CAP-64-PKG01:
ComponentUsageCapability is an inductive Component-owned family, distinct from
ComponentCapability. Application-owned packages are withdrawn. Require accepted
Directory intake, owner partitioning, revised typed query/gap results and CAR
parity before CAP-11-05 final acceptance. Use Directory for routine view work.

Rename public/generated operations such as `traceApplicationCapability` to
`traceComponentUsageCapability` and their inputs/results/labels together. Remove
old typed APIs without aliases so stale callers fail compilation. No old JSON
reader or compatibility surface is added. Presentation must distinguish usage
observations and their evidence from provided/required contract guarantees.
Preserve the forward UseCase unavailable branch. Do not repeat transport checks;
aggregate full-suite ownership remains here. Historical acceptance is baseline,
not proof of this new semantic/packaging dependency.

## Accepted input and profile transition

Require Phase 11's accepted release and committed deferred-validation handoff.
Its durable contract must identify:

- supported producer/schema/source revisions and real fixture availability;
- exact qualified Component, package, source, ABI, Capability and demand refs;
- explicit edge provenance and many-to-many cardinalities;
- typed results for the six Capability queries, including failure/gap variants;
- authorized query scopes and supported/unavailable demand branches;
- presentation and public Web/MCP authorization/result-mapping expectations;
- executable success/rejection and affected-consumer evidence.

Phase 11 owns the interacting identity/schema/query decisions as an internal
Step. This unit is eligible for gpt-6.1-sol / medium only when that complete
handoff settles its semantics. Reassess planning demand and current runtime at
execution entry if public mapping, authorization or producer compatibility is
still undecided. Do not use a cheaper profile to invent missing input.

This is an ordinary accepted-contract/profile handoff, not a new genuine
semantic split gate. Incoming semantic handoffs are therefore `[]`; the split
reason is time-bound packing only.

## View and public-surface invariants

Use one typed query result for Component Capability overview/detail and the
read-only Use Case -> Component Usage Capability -> Component Capability ->
realization trace. Preserve exact refs and Specification/Evidence links.
Do not manufacture joins from independent endpoint lists.

Keep unavailable, unauthorized, stale and ambiguous source outcomes distinct
from an admitted empty/no-provider result. Show gaps independently from runtime
Availability, Authorization, Guard satisfaction and execution evidence.
A catalog or realization record does not prove that runtime behavior ran.

Freeze and generate public operations through the existing project tooling.
Web and MCP/API must expose the same qualified identities, provider and
realization sets, gap kinds and provenance for the same authorized query.
No surface may reparse CML, reconstruct relationships with AI, infer identity
from names, select a hidden provider, approve a candidate or mutate canonical CML.

## Real producer/consumer acceptance

Consume one actual Cozy-generated, CNCF-admitted Component Capability fixture
and record the exact source, Cozy, CNCF ABI and CBD Support revisions.
Synthetic fixtures do not satisfy this original mandatory acceptance condition.

When an admitted Application/Use Case demand fixture exists, prove a positive
demand/provider/realization path and a typed gap path. If that input is absent,
prove the explicit unavailable branch and record the unexercised positive
integration. This optional branch does not waive the real Component Capability
fixture. The inspected CNCF Phase 78 and Cozy Phase 64 plans/checklists were
planned/unchecked at split time; obtain actual inputs at execution entry.
This split grants no source-mutation authority in either upstream repository.

## Validation and independent closure

Own the aggregate `sbt-full-suite` validation for the serial
`PHASE-11 -> PHASE-11.1` sequence under `final-only`. Before the final suite,
verify the predecessor's committed deferred-validation release, accepted
ancestry and reciprocal declarations. Run the repository full suite once on
this Phase's settled release tree; do not reinterpret the predecessor's
focused assurance as an earlier full-suite pass.

Every Step still needs its focused behavior/compile and affected-consumer
accumulator checks plus independent acceptance review. This Phase also needs
its own comprehensive review, applicable CAR lint, accepted journal/closure
ledger and final release commit. Documentation-only Slices use static checks;
the split operation itself runs no SBT or runtime command.

Close only when CAP-11-04–05, the real producer/consumer scenario, cross-surface
consistency and the aggregate full-validation gate pass or are disposed under
explicit successor/acceptance authority. Preserve the original DEV-CBD-003
scope and record complete sequence progress before handing off to Phase 12.
Missing required upstream inputs are dependencies, not permission for a local
replacement or an invented success.

## Phase Plan Gate: PROCEED

- target: 360-minute expected target; 480-minute expected ceiling
- estimate_calibration: no comparable planned/actual evidence; workload estimate
- planning_demand: bounded-settled, conditional on the complete Phase 11 handoff
- recommended_parent_profile: gpt-6.1-sol / medium; reassess if the handoff leaves material decisions open
- profile_cost_role: lower-cost execution
- expensive_reasoning_kernel: none after the predecessor settles identity/schema/query/public mapping
- frozen_profile_transition_handoff: Phase 11 accepted typed ingestion/catalog/query/presentation contract and actual source/fixture inventory
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: 390 minutes expected; 330–480 uncertainty range; 30 above target, 90 below ceiling; upstream implementation/wait excluded
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: original combined estimate 720 minutes exceeds 480; no profile-cost rejection
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: about 60 minutes additional sequence handoff/review/closure; one aggregate full suite; no quantified cost saving
- agent_reasoning_mode_policy: default standard; consider pro only at an eligible agent launch with supported, quality-first evidence
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 11

Confidence is low; no measured planned/actual multiplier was available.
The expensive kernel and coherent producer/query evidence stay in Phase 11;
this unit consumes the exact accepted handoff. Standard parent/agent mode
applies. The dated source split note owns full estimate and tradeoff provenance.

## Non-goals and references

Ingestion/query redesign, upstream Capability ABI or CML syntax ownership,
Capability execution, implicit candidate approval, interactive model editing,
ticket mutation, management hashes, publication and deployment are outside
this unit. Preserve the [repository rules](../rules/repository-rules.md).

- [Phase 11 and its applied split note](phase-11.md#applied-split--2026-10-04)
- [Phase 11.1 checklist](phase-11.1-checklist.md)
- [Local execution preparation](../notes/phase-11-12-local-execution-preparation.md)
- [Development proposal](../notes/capability-catalog-and-traceability-proposal.md)
- [Phase 12](phase-12.md)
