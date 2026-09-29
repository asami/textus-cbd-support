# Phase 10.3 - Candidate Projection, Review, and Approval

Stage Status:
- Current status: IN_PROGRESS
- Predecessor: Phase 10.2
- Successor: Phase 10.4
- Development item: DEV-CBD-002
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-10.3-checklist.md` with reproducible evidence.

Split from Phase 10. It owns P10-30–P10-34: candidate projection, semantic diff, review, approval, supersession, and invalidation.

It inherits the sequence-wide authority and storage boundaries in `phase-10.md`.

## Current P10-30 handoff

P10-30A defines the accepted [candidate CML projection
contract](../spec/internal-model-candidate-cml-projection-contract.md) and its
[design](../design/internal-model-candidate-cml-projection.md) define candidate
content and additive continuity-family selection. P10-30B separately owns the
canonical codec, one-pass admission, and executable specification. Its authored
implementation is the [candidate projection value model](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateCmlProjection.scala),
[canonical codec](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateCmlProjectionCodec.scala),
[admission validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateCmlProjectionValidator.scala),
and [executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateCmlProjectionValidatorSpec.scala).
The representative specification passed 28/28; the complete eight-suite
affected-consumer accumulator passed 128/128. The independent protected-focused
complete-Step review `PHASE-10.3-P10-30-STEP-REVIEW-01` returned PASS with no
Current Boundary Blockers. The local commit carrying `Step-Acceptance: P10-30`
records the exact twelve-path acceptance boundary and this checklist projection.

P10-30 closes only after both Slices, focused representative plus accumulator
validation, independent protected-focused Step review, and a local Step
acceptance commit. The [P10-30 checklist](phase-10.3-checklist.md) records both
accepted Slices; its uncommitted projection alone does not prove Step closure.
P10-33/P10-34 and Phase closure remain OPEN. No candidate human approval or
canonical CML mutation is claimed.

## Current P10-31 handoff

P10-31A defines the accepted [durable semantic-diff
contract](../spec/internal-model-semantic-diff-contract.md) and its
[design](../design/internal-model-semantic-diff.md). The coherent slice records
the [typed values](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiff.scala),
[canonical codec](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiffCodec.scala),
[one-pass validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiffValidator.scala),
package selector, and [executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiffValidatorSpec.scala).
The representative specification passed 17/17; the complete ten-suite
affected-consumer accumulator passed 153/153. The independent protected-focused
complete-Step review `PHASE-10.3-P10-31-STEP-REVIEW-01` returned PASS with no
Current Boundary Blockers. The local commit carrying `Step-Acceptance: P10-31`
records the exact nine-path acceptance boundary and its checklist projection;
an uncommitted projection alone does not prove Step closure.
P10-30 remains accepted; P10-33/P10-34 and Phase closure remain OPEN. No
candidate human approval, canonical CML mutation, or Phase release is claimed.

## Current P10-32A handoff

P10-32A defines the accepted [durable candidate-review binding
contract](../spec/internal-model-candidate-review-binding-contract.md) and its
[design](../design/internal-model-candidate-review-binding.md), together with
the package-private review values, canonical codec, one-capture historical
package admission, and [executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateReviewBindingValidatorSpec.scala).
The binding preserves the entire reviewed V1 manifest separately from its newer
carrier and keeps attributed review evidence separate from P10-33 human
approval. The representative specification passed 15/15; the complete
eleven-suite affected-consumer accumulator passed 168/168. The independent
protected-focused complete-Step review `PHASE-10.3-P10-32-STEP-REVIEW-01`
returned PASS with no Current Boundary Blockers. The local commit carrying
`Step-Acceptance: P10-32` records the exact nine-path acceptance boundary and
this checklist projection; an uncommitted projection alone does not prove
Step closure. No provider execution or human approval is claimed.
P10-30/P10-31 remain accepted; P10-33/P10-34 and Phase closure remain OPEN.

## Current P10-33A acceptance handoff

P10-33A defines the accepted [human approval contract](../spec/internal-model-candidate-human-approval-contract.md)
and [design](../design/internal-model-candidate-human-approval.md), with a
package-private durable record, strict canonical codec, same-capture explicit
approval/review selection, and executable specification. The representative
specification passed 16/16; the complete twelve-suite affected-consumer
accumulator passed 184/184. Independent complete-Step review
`PHASE-10.3-P10-33-STEP-REVIEW-01` identified `CPB-P10-33-001` (Unicode-blank
approval text). The exact codec/spec repair passed independent focused closure
`PHASE-10.3-P10-33-FOCUSED-REREVIEW-01`, closing that sole blocker with no new
findings. The local commit carrying `Step-Acceptance: P10-33` records the exact
nine-path acceptance boundary and this checklist projection; an uncommitted
projection alone does not prove Step closure. No actual human approval,
lifecycle applicability, CML authority, or Phase release is claimed.
P10-34 and Phase closure remain OPEN.

## Closure boundary

Close only with reproducible evidence for its own checklist. Release the accepted Phase 10.3 contract as the frozen input to Phase 10.4.

## Non-goals

All later original Phase 10 groups belong exclusively to their named successor. No Phase creates a parallel semantic model or changes canonical CML outside its explicitly owned approval-gate boundary.

## Phase Plan Gate: PROCEED

- target: calibrated expected duration centered on 6h; allowed ceiling 8h
- estimate_calibration: no comparable planned/actual Phase 10 evidence; the original focused Stage is the bounded estimate basis.
- planning_demand: protected-decision
- recommended_parent_profile: gpt-5.6-terra / xhigh
- profile_cost_role: expensive reasoning kernel
- expensive_reasoning_kernel: exact-hash review, approval, supersession, and invalidation authority.
- frozen_profile_transition_handoff: accepted Phase 10.2 release contract.
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: 360 minutes expected; 300–420 minute uncertainty range; target fit yes; ceiling fit yes.
- incoming_semantic_handoffs: [{from_child: "10.2", to_child: "10.3", kind: release, input: "accepted Phase 10.2 contract", action: "consume without reinterpretation", output: "Candidate Projection, Review, and Approval contract", owner: "Phase 10.2", invalidation_reason: "predecessor contract revision"}]
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: every adjacent merge is 720 expected minutes and exceeds the 480-minute ceiling; profile cost was not used.
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: an accepted release makes this contract independently reusable and reviewable by the next child.
- agent_reasoning_mode_policy: default standard
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 10
