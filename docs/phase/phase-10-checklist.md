# Phase 10 Checklist: Internal-model Package and Integrity

Status: CLOSED
phase=[Phase 10](phase-10.md)
predecessor=[Phase 9](phase-9.md)
successor=[Phase 10.1](phase-10.1.md)
development-item=DEV-CBD-002

This is the authoritative ledger for the retained first Phase 10 child only.
It consumes accepted Phase 9 canonical-model, projection, candidate-design,
and semantic-diff contracts; persistence must not create parallel semantics.
The later P10 items have one owner each in the Phase 10.1–10.7 checklists.
P10-STEP-01 and P10-STEP-02 have acceptance commits. The Phase full review,
focused closure re-review, and release validation establish the Phase
acceptance boundary; Phase 10.1 and later remain not started.

## P10-01: Internal-model source-root contract

- [x] Promote the project-owned `src/main/internal-model/` role and its separation
  from canonical CML, CBD retained state, and disposable output into design/spec.
  Evidence: `docs/design/internal-model-package.md` and
  `docs/spec/internal-model-package-contract.md` section 1; Step commit `f9582ed`.

## P10-02: Package identity and schema

- [x] Define stable package identity, project identity, revision, lifecycle
  state, schema/version compatibility, and deterministic serialization rules.
  Evidence: `docs/spec/internal-model-package-contract.md` sections 2–3 and
  project-bound validator specifications; Step commits `f9582ed` and `09dee8f`.

## P10-03: Manifest and artifact inventory

- [x] Define `manifest.yaml` inventory roles, required/optional artifacts,
  package-relative references, dependency order, and exact content hashes.
  Evidence: `docs/spec/internal-model-package-contract.md` sections 4–6 and
  `InternalModelPackageValidatorSpec`; Step commits `f9582ed` and `09dee8f`.

## P10-04: Integrity executable contract

- [x] Prove fail-closed behavior for missing, mismatched, incompatible,
  duplicated, or unresolved required package artifacts.
  Evidence: `InternalModelPackageValidatorSpec` (10 executable scenarios) and
  `ReviewDiagnosisPersistenceSpec` (8 persistence scenarios), including the
  focused repair-tree run; Step commit `09dee8f`, Phase full review and focused
  closure re-review. The final full-suite receipt is bound to the Phase release.

## Closure

- [x] Release the accepted Phase 10 contract to Phase 10.1. Phase 10.1 remains
  planned/not started; this release transfers only the accepted Phase 10
  contract and does not implement successor work.
