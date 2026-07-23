# Phase 8 P8-40: Persistence entity and retention contract

date=2026-07-23
phase=Phase 8
status=completed-reviewed-and-validated

## Decision

P8-40 defines a database-vendor-neutral logical persistence model before any
database adapter, reuse algorithm, or history surface is implemented. The
model separates stable CAR lineage from version/digest-bound target snapshots,
terminal Review Run snapshots, canonical Report and attestation payloads,
opaque reuse identities, comparison references, and append-only retention
events.

The existing P5 in-memory `CarReviewRepository` stays intact. It is an earlier
terminal retention boundary, not the database required by Phase 8. A future
adapter may reuse its validation only when it preserves P8's stronger lineage,
attestation, reuse-identity, comparison, and tombstone constraints.

## Boundary

- P8-41 owns the exact reuse-key input set and invalidation formula.
- P8-42 owns physical storage, compatible completed-Run reuse, and concurrent
  request coalescing.
- P8-43 owns failed/cancelled/expired/incompatible runtime outcomes.
- P8-44 owns comparison deltas and history presentation.
- P8-45 owns database authorization, retention administration, and bounded MCP
  history access.

## Review correction

The retention event now relates to every retained entity except itself and
carries a safe `recordDigest` in addition to applicable Report/target digests.
The executable specification also rejects duplicate database tables, missing
primary-key fields, and relationships to undeclared entity names.

## Verification executed

- `CarReviewPersistenceContractSpec` checks all required entity identities,
  immutable/terminal retained records, opaque reuse-key reservation, and
  append-only content-safe retention audit.
- Focused `CarReviewPersistenceContractSpec` passed: 2 tests in 1 suite.
- After independent-review correction, the same focused specification and
  `sbt --batch Test/compile` passed; `git diff --check` passed.
- Independent re-review found no actionable issue. Full `sbt --batch test`
  passed: 249 tests in 61 suites.
