# Phase 14 Checklist - CAR Review Native Job Integration and Acceptance

status=in_progress
phase=[Phase 14](phase-14.md)
development_item=DEV-CBD-004
validation_ownership=phase-local

Transferred implementation is present but unaccepted. Every item below remains
unchecked until actual CBD-owned validation and independent acceptance are
recorded. Original CNCF Phase 69.7 authoring and validation records retain their
original scope and do not close this Phase.

## JOB-14-01: Source admission and build connection

- [ ] Record the exact transferred source, original manifests/results/failures
  and admitted CNCF runtime/API, source and artifact dependencies.
- [ ] Reconcile the existing source-JAR/runtime-descriptor connection and the
  root/output-exclusion source/artifact modes; repair only demonstrated gaps.
- [ ] Prove CML generation, generated-source compilation and runtime descriptor
  availability; retain the unpublished Test-bridge dependency explicitly.
- [ ] Prove unchanged normal artifact selection, structured invalid-source
  refusal and exclusion of Test bridges from production output; record actual
  supported production dependency compatibility.

## JOB-14-02: Review identity, Provider input and persistence

- [ ] Prove plan reference/revision/selection validation and actual Entity-issued
  ownership without substituting management digests or derived identities.
- [ ] Prove registered canonical Provider input, cache re-admission, mismatch
  refusal and attributable failure without duplicate Provider execution.
- [ ] Reconcile the deleted persistence spec and coverage ledger; prove all
  retained persistence, authorization, expiry, successor and terminal behaviors.
- [ ] Record and validate explicit older-root compatibility/migration treatment;
  refuse unsupported roots without silent conversion or replacement identity.

## JOB-14-03: Native execution and cancellation

- [ ] Validate all CJE1-CJE10 cases, including actual staged publication,
  abort/dispose, a distinct Job UOW and the shrinking property.
- [ ] Prove canonical Entity Report/attestation completion and actual commit
  before native success; Provider/owner/Entity/prepare/commit failures stay failed.
- [ ] Validate CANC1-CANC4 and native queued/active cancellation, bounded actual
  ProviderCall outcomes, best-effort disposition and callback closure.

## JOB-14-04: Production StartReview and durable binding

- [ ] Connect generated StartReview to admitted server-owned selection/plan
  with authorization and target/profile/descriptor checks.
- [ ] Persist one exact Review-to-Job binding in the outer staging UOW; prove
  concurrent/repeated starts do not duplicate Provider work.
- [ ] Connect authorized reads/cancellation/report queries to that binding and
  preserve explicit missing-plan refusal on the transport-only API.
- [ ] Record an admitted started-work recovery/replay policy and prove it;
  retain `RecoveryRequired` for unsupported Running/Suspended work without replay.

## JOB-14-05: Independently fresh runtime acceptance

- [ ] Execute the production StartReview-to-Provider-to-Entity-to-Job path using
  persistent storage and terminate the writer process.
- [ ] Read exact Review/Job/Report/attestation through an independently new full
  runtime with no writer memory, redispatch or duplicate snapshots.
- [ ] Prove caller-isolated read/cancel/report authorization, stable terminal
  results and attributable unsupported recovery behavior.

## JOB-14-06: Validation and closure

- [ ] Record relevant focused, generated-model, lifecycle, persistence and
  output-exclusion regressions plus representative fresh-runtime results.
- [ ] Independently accept and locally commit each coherent Step with actual
  evidence; retain historical authoring/prerequisite provenance separately.
- [ ] Run Phase 14's own full SBT validation and applicable declared CAR lint.
- [ ] Complete the comprehensive Phase review and close admitted blockers.
- [ ] Synchronize handoff/specification/design/strategy and remaining Hygiene
  with actual accepted results; keep CNCF and Capability Phase closure separate.
- [ ] Execute and verify the validated local Phase release; only then mark
  Phase 14 CLOSED. No push/publication/deployment is implied.
