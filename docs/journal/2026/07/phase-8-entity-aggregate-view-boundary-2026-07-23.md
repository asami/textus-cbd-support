# Phase 8: Entity Aggregate and View boundary

date=2026-07-23
phase=Phase 8 P8-42/P8-43
status=in-progress

CAR Review persistence uses the CNCF Entity layer only. `ReviewDiagnosis` is
the Aggregate root for one exact P8-41 reuse identity. Target, Run, Report,
and attestation snapshots are composition members, and the rebuildable
`ReviewDiagnosis` View is the exact-key admission/read projection.

The protected internal DSL performs the server-owned Entity operations with
`ServiceInternal` authorization. Review behavior must not add JDBC, SQL,
SQLite-driver, raw datastore, or separately persisted View access. This keeps
authorization, UnitOfWork lifecycle, CallTree/audit events, invalidation, and
future shared-datastore component scope intact.

The P8-43 terminal history implementation retains all four non-success states
as immutable Run snapshots and never returns them as a reusable successful
Report. Safe same-key successor ownership requires the conditional Entity
transition recorded in the CNCF handoff; it is intentionally not replaced by
process-local locking or a storage escape hatch.

## 2026-07-23 review-fix observation

The initial review-fix observed that the physical Entity record had changed
while a following read could expose `claimed`.  The response was deliberately
not to add a review-local cache, direct SQLite access, raw datastore access,
or a manually maintained View.  The read/codec path had to remain the audited
Entity source of truth.

## 2026-07-24 correction and closure evidence

The current generated scalar `ValueReader` accepts both its record form and
the scalar datastore form.  With that shared generated persistence contract,
the `ReviewDiagnosis` Aggregate reads its persisted terminal state correctly;
CBD Support has no `PersistedReviewDiagnosis` adapter or other review-local
persistence codec.  `ReviewDiagnosisPersistenceSpec` now passes all five
cases: owner/join admission, completed Report reuse across fresh UnitOfWork
reads, non-success terminal retention, lease enforcement, and terminal replay
rejection.

This closes P8-42.  P8-43 remains open solely for the distinct requirement:
a conditional Entity transition that can retain a terminal Run and admit a
safe same-key successor.  Any broader Cozy scalar-persistence regression work
is a proposed future Cozy phase, not a CBD Support Phase 16.
