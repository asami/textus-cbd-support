# Phase 10.5 Checklist: Drift and CML Change Gate

Status: CLOSED
phase=[Phase 10.5](phase-10.5.md)
predecessor=[Phase 10.4](phase-10.4.md)
successor=[Phase 10.6](phase-10.6.md)
development-item=DEV-CBD-002

contract=[Internal-model CML Change Contract](../spec/internal-model-cml-change-contract.md)
design=[Internal-model CML Change Design](../design/internal-model-cml-change.md)
Step=P105-CML-CHANGE (accepted Step commit 7346a4cabbe09e806dc88724b55b3b34f164400b)
Slice A=P105-CML-CHANGE-A (accepted)
Slice B=P105-CML-CHANGE-B (accepted)
Slice C=P105-CML-CHANGE-C (accepted; actual positive and expected-refusal probes retained)

Slice B authoring evidence is the [application engine](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplication.scala),
[native writer](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriter.scala),
[native executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriterSpec.scala),
[application executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplicationSpec.scala),
[repository skill source](../skills/cbd-apply-cml-change/SKILL.md), and
[developer guide](../developer-guide-internal-model-cml-change.md). These links
record the historical authoring checkpoint. A focused validation passed 40 + 70
and B passed 27 + 118 specifications, admitting dependencies only at that time.
Subsequent Step and Phase acceptance is recorded below.

Slice C authored evidence is the [post-validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidation.scala),
[post-validation specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidationSpec.scala),
[same-JVM test-only probe](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceProbe.scala)
and [independent test-owner support](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceSupport.scala).
C parent focused validation passed 17 specifications and accumulator validation
passed 215 specifications (232 = 17 + 215). Actual positive Cozy commands on both
applied targets exited 0/0 and produced `ReprojectedPendingAcceptance` with all
eight nonempty families, retained conditions and an unchanged cursor. The separate
actual alpha string-attribute FAIL Cozy commands exited 1/0; the test probe
exited 0 after its expected-refusal checks passed and produced the expected
`Failed` result with no projections, an unchanged cursor and retained applied
changes; exit 1 is refusal proof, not CLI success. The
[developer guide](../developer-guide-internal-model-cml-change.md) records the
reproducible probe procedure. Synthetic unit observations are admission coverage
only. The final acceptance record below supersedes that historical pending
checkpoint and covers the six obligations.

## P10-50: Semantic drift invalidation

- [x] Invalidate or require re-review when Scenario, realization/design,
  referenced model, glossary/BoK basis, mapping rules, or CML projection changes.

## P10-51: Baseline drift handling

- [x] Detect live CML drift and require explicit reconciliation/review rather
  than silently applying a proposal against a different baseline.

## P10-52: Exact approval gate

- [x] Reject CML mutation unless an applicable human approval binds the exact
  selected realization/design, candidate projection and complete semantic subject
  through exact typed artifact and logical references with explicit revisions.

## P10-53: Skill-applied CML change boundary

- [x] Define the dedicated skill/repository mutation contract so approved CML
  changes preserve canonical-source ownership and Git-governed acceptance.

## P10-54: Post-change validation and re-projection

- [x] Run Cozy/CBD validation after the CML change and regenerate the Phase 9
  Canonical Component Design Model and stakeholder/engineering projections.

## Closure

- [x] Release the accepted Phase 10.5 contract to Phase 10.6.

## Final acceptance evidence — 2026-10-03

- P10-50 and P10-51: the gate executable specification independently changes
  semantic/source versions and physically reads live CML; drift, unknown
  versions and unavailable sources require explicit reconciliation/re-review.
- P10-52: the gate and application specifications reject missing, substituted,
  superseded or unresolved approval/subject/owner evidence. Only exact current
  independently admitted actual-human approval permits owned application.
- P10-53: the dedicated repository skill, native writer and application
  specifications preserve canonical-source ownership, anchored preflight,
  partial-effect evidence and the separate Git acceptance boundary.
- P10-54: actual positive Cozy alpha/beta commands exited 0/0 after real owned
  changes and the actual CBD owners regenerated all eight nonempty Phase 9
  families. Actual alpha/beta commands exited 1/0 in the separate negative
  probe; its successful assertions prove refusal, not positive CLI success.
  Both probes retain explicit sources, rich sidecars and unchanged cursors.
- Step acceptance: complete-Step review plus independent focused closure and
  native Step commit `7346a4cabbe09e806dc88724b55b3b34f164400b`.
- Phase acceptance: one full review P105-PHASE-FULL-REVIEW-001/1, one consumed
  repair cycle, and independent P105-PHASE-FOCUSED-REVIEW-001/1 PASS. Sole
  CPB-P105-FULL-001 resolved; 241/241 affected specifications pass. Optional
  consumed-model drift/missing/incompatible evidence cannot produce successful
  continuity; unchanged controls retain all eight projections.
- Release identity: `Phase-Closure-Binding: P105-PHASE-CLOSURE-001@1` in the
  distinct local release commit, with full forked repository `test`, applicable
  CAR lint and a verified committed closure receipt. The closed checklist is
  release-candidate content until that exact validated commit succeeds.
- Hygiene and Development Candidate: no accepted review items and zero
  unpersisted items; no empty journals. Existing nonblocking CAR warnings do
  not imply publication readiness or authorize dependency upgrades.
- Preserved work: unrelated journals/notes and the complete unstarted Phase
  10.6/10.7/11 planning set remain outside this commit. Shared Strategy
  synchronization is deferred. This invocation stops after Phase 10.5.
