# CAR Review Terminal History Contract v1

status=implemented
phase=Phase 8
checklist=P8-43
updated_at=2026-07-26

## Purpose

P8-43 preserves every non-success terminal Review outcome as attributable
history without allowing it to become a reusable completed Report.

## Terminal outcomes

`failed`, `cancelled`, `expired`, and `incompatible` are terminal outcomes.
Each must retain one immutable `ReviewRunSnapshot` with the exact Review ID,
target, profile, provider state, bounded limitation/failure information, and
terminal instant. Only `completed` may add a Report and attestation snapshot.

## Reuse rule

An exact reuse-key root in a non-success terminal state MUST NOT produce
`CarReviewDiagnosisAdmission.Reused`. A request with a different policy or
evidence already has a different P8-41 digest and claims a separate root. A
request with the same key may create a successor Run only after the terminal
snapshot is retained; it must never overwrite the former Run snapshot or its
failure attribution.

## Expiry

Expiry removes only payload authorized by retention policy and appends an
attributable tombstone event. An expired identity is never a successful reuse
candidate. P8-45 owns retention authorization and history enumeration.

## Boundary

This behavior uses CBD-owned Entity records through protected internal DSL
operations. It has no direct SQL, SQLite, JDBC, or raw datastore path. One
conditional Entity transition atomically compares the persisted terminal root,
installs the successor Run, and advances only the root's active-run pointer.
The predecessor Run stays immutable and attributable. `P8-45` owns the
separate authorization, expiry, and bounded read policy for retained payloads.
