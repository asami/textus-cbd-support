# Phase 10.3 Checklist: Candidate Projection, Review, and Approval

Status: IN_PROGRESS
phase=[Phase 10.3](phase-10.3.md)
predecessor=[Phase 10.2](phase-10.2.md)
successor=[Phase 10.4](phase-10.4.md)
development-item=DEV-CBD-002

## P10-30: Candidate CML projection artifact

Stage Status:
- Current status: DONE
- Owner: Textus CBD Support development
- Update rule: this Step is accepted only with both Slice items below, the
  validation/review evidence, and the local commit carrying `Step-Acceptance: P10-30`.

- [x] P10-30A: Define the candidate content contract and additive continuity
  selection contract: [spec](../spec/internal-model-candidate-cml-projection-contract.md),
  [design](../design/internal-model-candidate-cml-projection.md).
- [x] P10-30B: Implement the canonical codec, one-pass admission, and
  executable specification. Accepted code/spec are
  `InternalModelCandidateCmlProjection.scala`,
  `InternalModelCandidateCmlProjectionCodec.scala`,
  `InternalModelCandidateCmlProjectionValidator.scala`, and
  `InternalModelCandidateCmlProjectionValidatorSpec.scala`.

P10-30 Step closure requires both Slices, focused representative plus
accumulator validation, independent protected-focused Step review, and a local
Step acceptance commit. This ledger is the acceptance projection included in
that exact commit; an uncommitted copy alone is not Step closure evidence.

Acceptance evidence:
- Representative: `Test/testOnly org.simplemodeling.textus.cbdsupport.runtime.InternalModelCandidateCmlProjectionValidatorSpec`, 28/28 passed.
- Accumulator: 128/128 passed across `InternalModelCandidateCmlProjectionValidatorSpec`,
  `InternalModelPackageValidatorSpec`, `InternalModelSemanticRealizationValidatorSpec`,
  `InternalModelProjectionContinuityValidatorSpec`, `InternalModelSourceSnapshotFreshnessSpec`,
  `InternalModelDecisionRecordValidatorSpec`, `InternalModelOpenIssueRecordValidatorSpec`,
  and `InternalModelPackageFreshnessSpec`, all in `org.simplemodeling.textus.cbdsupport.runtime`.
  Both serialized SBT invocations completed with the shared lock released.
- Independent complete-Step protected-focused review:
  `PHASE-10.3-P10-30-STEP-REVIEW-01`, PASS, no Current Boundary Blockers.
- Local acceptance commit: locate `Step-Acceptance: P10-30` in Git history;
  that commit must contain this ledger and all twelve P10-30A+B owned paths.

No candidate human approval, canonical CML mutation, or Phase release is claimed.

## P10-31: Durable semantic diff

- [ ] Persist/reconstruct the Phase 9 semantic diff independently of textual Git
  diff while retaining traceability to exact proposed CML changes.

## P10-32: Review binding

- [ ] Bind candidate review evidence to exact package, realization/design,
  projection, rules/provider versions, and content hashes.

## P10-33: Human approval artifact

- [ ] Define separate approval records bound to exact candidate identity,
  revision/hash, accountable human, decision, rationale, and unresolved items.

## P10-34: Supersession and approval invalidation

- [ ] Define deterministic rules for changes-requested, rejected, approved,
  superseded, and invalidated states without mutating the approved model hash.

## Closure

- [ ] Release the accepted Phase 10.3 contract to Phase 10.4.
