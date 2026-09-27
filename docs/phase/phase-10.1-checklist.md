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

- [ ] Implement deterministic comparison of live Scenario, model, glossary/BoK,
  and CML sources with recorded baselines and surface drift without silent rebase.

## Closure

- [ ] Release the accepted Phase 10.1 contract to Phase 10.2.
