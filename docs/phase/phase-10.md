# Phase 10 - Internal-model Package and Integrity

Stage Status:
- Current status: CLOSED
- Predecessor: Phase 9
- Successor: Phase 10.1
- Development item: DEV-CBD-002
- Current step: P10-STEP-01 and P10-STEP-02 are accepted; the Phase 10 contract is released to Phase 10.1.
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-10-checklist.md` with reproducible evidence.

Phase 10 is the retained first child of the applied split. It owns P10-01–P10-04: source-root separation, package identity/schema, manifest inventory, and fail-closed integrity.

The accepted P10-01--P10-03 contract is recorded in
[`internal-model-package.md`](../design/internal-model-package.md) and
[`internal-model-package-contract.md`](../spec/internal-model-package-contract.md).
P10-04 provides its fail-closed executable proof. Checklist completion remains
governed solely by `phase-10-checklist.md`.

## Sequence-wide purpose and authority

The Phase 10–10.7 sequence makes Phase 9 model-development work durable,
reviewable, resumable, and safely applicable to canonical CML. Phase 9 owns the
Canonical Component Design Model, its stakeholder and engineering projections,
candidate design, semantic diff, review, and Git-governed acceptance boundaries.
The sequence persists those identities and semantics; it must not create a
second Entity/Event/Workflow or Mono-Koto model, nor a storage-only
interpretation. Rehydration must reconstruct the identities needed by Phase 9
projections or report an explicit incompatibility or gap.

The following layout is the original first-child release description; current
typed control supersession and sequence progress are recorded below.
The project-owned continuation root is provisionally
`src/main/internal-model/`. It must support stopping, committing, transferring,
and resuming the selected work in another process or developer's checkout
without relying on chat/provider history, `target/`, or an available CBD
Support database. The root `manifest.yaml` name and its V1 canonical
serialization are fixed by the Phase 10 design/spec contract. Artifact content
schemas and semantic realization of the following roles remain later-phase work:

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

## Historical sequence-wide storage and integrity boundaries

This original split/release wording is retained as history. Its hash-based
management/approval clauses are superseded by the dated current boundary below.

- Project-owned `src/main/internal-model/` is the portable, versioned minimum
  complete continuation package: semantic state, source basis, decisions,
  approval, validation, and cursor.
- CBD Support retained state may hold richer review runs, alternatives,
  proposals, semantic diffs, evidence, collaboration, and supersession history.
  It is an audit/collaboration store, not the sole resume authority.
- `target/cbd-support/` holds disposable caches, tentative extraction output,
  previews, and other non-authoritative working artifacts.
- CML remains canonical where the design is CML-owned. An internal model is not
  authoritative merely because it is committed.
- Stable identities and exact content hashes bind decisions and approvals.
  Required artifacts and hashes fail closed when missing or inconsistent.
- Source snapshots preserve a decision's semantic basis but do not replace live
  canonical sources. Drift is surfaced, never silently rebased.
- Human approval binds the exact model and projection revision/hash. Neither a
  skill nor CBD Support may mutate CML without applicable approval for that
  exact candidate state.
- Provider output is attributable evidence or proposal, never implicit human
  approval or a reconstructed decision.

## Sequence-wide non-goals and dependencies

The split does not replace Phase 9 projection/view contracts, make the internal
model a second canonical design source, reconstruct missing decisions from chat,
Git history, names, or provider output, automatically approve semantic changes
or CML mutations, publish raw prompts/responses or sensitive provider payloads
into project source by default, require retained state for resume, or accept
stale approval after semantic or baseline drift. The internal model is excluded
by default from runtime packaging, public APIs, ordinary CML generation,
documentation publication, and CAR/SAR artifacts; Phase 10.6 owns proof of
that exclusion.

Phase 9 supplies admitted semantic metadata and canonical-source behavior.
Missing Cozy or CNCF contracts remain explicit upstream gaps, not license for
local semantic reconstruction. Any dedicated CML-application skill must
preserve the exact approval/hash gate and Phase 9 Git-governed acceptance
policy. The terminal Phase 10.7 acceptance scenario validates this full loop.

The preceding approval/hash phrase is historical, not a current sequence
requirement; the current typed gate below governs application.

Planning sources: `docs/journal/2026/08/2026-08-17-project-internal-model-storage-direction.md`,
`docs/phase/phase-9.md`, `docs/phase/phase-9-checklist.md`,
`docs/journal/2026/09/2026-09-09-mono-koto-analysis-view.md`, and
`docs/journal/2026/09/2026-09-03-cml-design-improvement-pull-request-loop.md`
when present in synchronized journal history.

## Closure boundary

P10-STEP-01 committed the source-root, package-identity, manifest-inventory,
canonical-byte, and digest contract as `f9582ed`. P10-STEP-02 committed the
project-bound fail-closed validator and executable specifications as `09dee8f`.
The Phase full review and focused closure re-review accepted the final bounded
identity-helper extraction without changing Review persistence semantics.
The final repository-full test and CAR-lint receipts are bound to the distinct
Phase release commit; the release does not claim approval, rehydration, CML
mutation, or Phase 10.1 execution. The accepted contract is the frozen input
to Phase 10.1.

## Non-goals

All later original Phase 10 groups belong exclusively to their named successor. No Phase creates a parallel semantic model or changes canonical CML outside its explicitly owned approval-gate boundary.

## Phase Plan Gate: PROCEED

- target: calibrated expected duration centered on 6h; allowed ceiling 8h
- estimate_calibration: no comparable planned/actual Phase 10 evidence; the original focused Stage is the bounded estimate basis.
- planning_demand: protected-decision
- recommended_parent_profile: gpt-5.6-terra / xhigh
- profile_cost_role: expensive reasoning kernel
- expensive_reasoning_kernel: package identity, schema compatibility, and fail-closed integrity decisions.
- frozen_profile_transition_handoff: none
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: 360 minutes expected; 300–420 minute uncertainty range; target fit yes; ceiling fit yes.
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: every adjacent merge is 720 expected minutes and exceeds the 480-minute ceiling; profile cost was not used.
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: an accepted release makes this contract independently reusable and reviewable by the next child.
- agent_reasoning_mode_policy: default standard
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 10

## Applied split record — 2026-09-11

Apply-mode invocation: `$cncf-split-phase Phase 10`. The verified pre-goal entry result was `SPLIT_REQUIRED` with `time-bound`, 2,880 expected minutes, a 4,320-minute conservative upper bound, 360-minute target, and 480-minute ceiling. Source goal/status: `none / no-goal-pre-entry`.

The applied order is `10`, `10.1`, `10.2`, `10.3`, `10.4`, `10.5`, `10.6`, and `10.7`. Every original item was OPEN; no completed history, accepted evidence, or journal record moved. Each child owns one original Stage and hands off only its accepted release. Every adjacent merge is 720 expected minutes and exceeds the ceiling; no profile-cost-only rejection was used.

Terra/xhigh owns unresolved package, semantic-identity, approval/CML-gate, and API/security kernels (10, 10.2, 10.3, 10.5, 10.6). Terra/high owns bounded snapshot, rehydration, and closure work (10.1, 10.4, 10.7).

## Current sequence control and progress — 2026-10-04

The retained first child Phase10 stays CLOSED with its original accepted
commits/checklist. Historical canonical-byte/digest evidence above records that
release, not current management authority. The
[typed control contract](../spec/internal-model-typed-control-contract.md) and
[repository rules](../rules/repository-rules.md) supersede historical hash
requirements. Current package admission uses declared inventory/dependencies;
complete `ReviewSubject` package/scope/input selections, exact artifact
ID/revision/role and independent logical record ID/revision bind review and
approval. Carrier, subject, artifact, logical and source-owner revisions stay
distinct. Actual current independent human input and source-owner authority,
complete existing targets and explicit next source revisions are freshly
evaluated by the rooted gate. No hash, byte seal, inferred version or stored
approval claim establishes applicability or authentication.

[Phase10.6](phase-10.6-checklist.md) released retained-state/security and actual
output exclusion at `f0f91f7`. [Phase10.7's dated record](phase-10.7.md#accepted-step-evidence--2026-10-04)
records accepted Step01 `272f59d` (six representative/185 accumulator tests,
synthetic owner-composition observations) and Step02 `6f7cef8` (16 tests,
three completed external sessions, six actual lint/four generation results).
The same CCDM and all eight actual view owners preserve semantic selection;
positive re-projection is pending acceptance and negative Failed retains
actual effects/cursor. Step03 `3f22573` is independently accepted.
The terminal sequence is CLOSED by the
[Phase10.7 final ledger](phase-10.7-checklist.md): final full002 passed
1,048 tests / 131 suites, current CAR lint0/zero FAIL, sole full-review PASS
and distinct local release bound by `P107-PHASE-CLOSURE-001@1`.
No generated-code compilation,
new live ABI, human/source authentication or deployed-adapter proof is implied.

Shared Phase index/strategy synchronization is explicitly
[deferred in canonical Phase10.7](phase-10.7.md#shared-projection-synchronization-deferred--2026-10-04)
because of concurrent Phase11/12 planning. Those projections do not select
current sequence state; separate future synchronization retains responsibility.
