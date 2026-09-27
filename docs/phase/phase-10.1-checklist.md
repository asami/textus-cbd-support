# Phase 10.1 Checklist: Source Snapshots and Freshness

Status: OPEN
phase=[Phase 10.1](phase-10.1.md)
predecessor=[Phase 10](phase-10.md)
successor=[Phase 10.2](phase-10.2.md)
development-item=DEV-CBD-002

## P10-10: Scenario source snapshot

- [x] Define the self-contained selected Scenario snapshot with canonical source
  identity, revision/hash, normalized content, and Phase 9 trace identities.
  Evidence: `docs/spec/internal-model-source-snapshot-contract.md` sections
  2–4 and `docs/design/internal-model-source-snapshots.md` (selected Scenario);
  P10.1-STEP-01 complete-Step review `PHASE-10.1-STEP-01-COMPLETE-REVIEW-01`.

## P10-11: Model-context and glossary/BoK snapshots

- [x] Define bounded model-context and glossary/BoK snapshots containing the
  semantic basis actually used by the selected candidate and its provenance.
  Evidence: `docs/spec/internal-model-source-snapshot-contract.md` sections
  3, 5–6, and 8; `docs/design/internal-model-source-snapshots.md`
  (model-context and glossary/BoK); the same complete-Step review.

## P10-12: CML baseline snapshot

- [x] Define the exact CML baseline needed to interpret and safely apply a
  candidate projection without replacing live canonical CML authority.
  Evidence: `docs/spec/internal-model-source-snapshot-contract.md` sections
  3 and 7–8 and `docs/design/internal-model-source-snapshots.md` (exact CML
  baseline); the same complete-Step review. This accepts a read-only snapshot
  contract, not live CML mutation or the P10-13 freshness comparator.

## P10-13: Snapshot freshness checks

- [x] Implement deterministic comparison of source-owner-supplied Scenario,
  model-context, and glossary/BoK observations, plus the project-owned current
  CML file, with every selected recorded baseline; surface unchanged, changed,
  unavailable, unauthorized, malformed, and ambiguous/conflicting outcomes
  without silent rebase or CML mutation. Evidence: the package-wide checker and
  executable specifications in `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/`
  and `src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/`;
  `P10.1-STEP-02-SBT-010` (45/45 focused tests, lock released); complete-Step
  review `PHASE-10.1-P10.1-STEP-02-P10-13-step-protected-focused-01` with
  accepted disposition `27cbb3f6796be9c821b77feb6d0f82ef900cc9de71dc9997a6ef3f16286ca7c8`.

## Closure

- [ ] Release the accepted Phase 10.1 contract to Phase 10.2.
