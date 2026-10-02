# Phase 10.5 - Drift and CML Change Gate

Stage Status:
- Current status: OPEN
- Predecessor: Phase 10.4
- Successor: Phase 10.6
- Development item: DEV-CBD-002
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-10.5-checklist.md` with reproducible evidence.

Split from Phase 10. It owns P10-50–P10-54: semantic and baseline drift, exact approval gate, CML mutation boundary, validation, and re-projection.

It inherits the sequence-wide authority and storage boundaries in `phase-10.md`.

## Closure boundary

Close only with reproducible evidence for its own checklist. Release the accepted Phase 10.5 contract as the frozen input to Phase 10.6.

## Non-goals

All later original Phase 10 groups belong exclusively to their named successor. No Phase creates a parallel semantic model or changes canonical CML outside its explicitly owned approval-gate boundary.

## Phase Plan Gate: PROCEED

- target: calibrated expected duration centered on 6h; allowed ceiling 8h
- estimate_calibration: no comparable planned/actual Phase 10 evidence; the original focused Stage is the bounded estimate basis.
- planning_demand: protected-decision
- recommended_parent_profile: gpt-6.1-sol / high (P105-IMPLEMENT-001 Slice A bounded protected boundary)
- profile_cost_role: expensive reasoning kernel
- expensive_reasoning_kernel: drift invalidation and exact human approval for canonical CML mutation.
- frozen_profile_transition_handoff: accepted Phase 10.4 release contract.
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: 360 minutes expected; 300–420 minute uncertainty range; target fit yes; ceiling fit yes.
- incoming_semantic_handoffs: [{from_child: "10.4", to_child: "10.5", kind: release, input: "accepted Phase 10.4 contract", action: "consume without reinterpretation", output: "Drift and CML Change Gate contract", owner: "Phase 10.4", invalidation_reason: "predecessor contract revision"}]
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: every adjacent merge is 720 expected minutes and exceeds the 480-minute ceiling; profile cost was not used.
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: an accepted release makes this contract independently reusable and reviewable by the next child.
- agent_reasoning_mode_policy: default standard
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 10

## Current startup and frozen Step

Phase startup on 2026-10-02 admitted original base
`bbc0cf2cf3dd3420ebdb4094889e5ddfbf425ea2`, authority
P105-AUTHORITY-001 revision 1 and Goal binding P105-GOAL-BINDING-001 revision 1.
The parent owns that authority and all validation/review/transition decisions.
Current status remains OPEN; startup and authoring are not acceptance.

Step P105-CML-CHANGE owns the [CML change contract](../spec/internal-model-cml-change-contract.md)
and [three-Slice design](../design/internal-model-cml-change.md). Slice
P105-CML-CHANGE-A, frozen by P105-IMPLEMENT-001 revision 1, authors typed
invalidation/live-CML drift, exact actual-human approval and independent
existing-target ownership admission. Slice B owns dedicated skill application;
Slice C owns actual post-change validation and Phase 9 regeneration. The Slice A
gate emits no CML write, syntax validation, Git acceptance or closure.

Slice P105-CML-CHANGE-B, frozen by P105-IMPLEMENT-B-001 revision 1, authors
the private rooted [application engine](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplication.scala),
Darwin LP64 [native writer](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriter.scala),
[writer specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriterSpec.scala),
[application specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplicationSpec.scala),
[repository skill source](../skills/cbd-apply-cml-change/SKILL.md), and
[developer guide](../developer-guide-internal-model-cml-change.md).
Fresh exact actual-human/source-owner admission precedes all-target anchored
existing-file preflight and ordered actual effects. Complete failure outcomes
and `AppliedPendingValidation` do not grant validation, owner-store revision
allocation, cursor/Git acceptance or closure. The consuming adapter remains an
explicit dependency, and the repository instruction source is not installed.
Slice A focused validation passed 40 + 70 and Slice B passed 27 + 118
specifications. These are dependency admission only; complete Step acceptance
is pending. Status stays OPEN.

Slice P105-CML-CHANGE-C, frozen by P105-IMPLEMENT-C-001 revision 1, authors
the private [post-validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidation.scala),
[executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidationSpec.scala),
[same-JVM test-only probe](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceProbe.scala)
and [independent test owner support](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceSupport.scala).
It binds actual complete application, exact admitted command observations and
independent refreshed source/realization/binding selectors to actual rooted
freshness and all-eight Phase 9 owners. Incomplete/Failed remain explicit;
ReprojectedPendingAcceptance retains full shared evidence without acceptance.
The frozen parent test plan includes positive real Cozy commands on both targets
and a separate expected alpha lint failure with retained actual changes. Unit
synthetic observations provide no CLI integration proof. C parent focused
validation passed 17 specifications and accumulator validation passed 215
specifications (232 = 17 + 215). Actual positive Cozy commands on both targets
exited 0/0 and produced `ReprojectedPendingAcceptance` with all eight nonempty
families, retained conditions and an unchanged cursor. The actual negative Cozy
commands exited 1/0; the test probe exited 0 after its expected-refusal checks
passed and produced the expected `Failed` result with no projections, an
unchanged cursor and retained applied changes; exit 1 is refusal proof, not CLI
success. The [developer guide](../developer-guide-internal-model-cml-change.md)
records the reproducible probe procedure. Complete Step acceptance, full Phase
review/release and all six checklist obligations remain pending. No consuming adapter is deployed
and the repository skill source remains uninstalled.
