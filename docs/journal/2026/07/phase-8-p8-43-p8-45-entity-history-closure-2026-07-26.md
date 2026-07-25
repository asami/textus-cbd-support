# Phase 8 P8-43 / P8-45 Entity History Closure

date=2026-07-26
phase=Phase 8
items=P8-43,P8-45
status=implemented

## Decision

CAR Review terminal history uses the CBD-owned `SimpleEntity` collections and
the protected Entity/UnitOfWork DSL. Revision initialization and advancement
remain framework-owned; Review requests, Create models, and public operations
do not carry a caller-provided revision.

`ReviewSubmissionDocument` uses the CML `ContentBody` value type, rather than a raw string, so
the generated persistence shape retains an exact JSON document as a typed
content value and survives a SQLite-backed Entity round trip without an
application persistence codec.

## P8-43

The persisted conditional transition compares the terminal diagnosis state and
revision, retains the immutable terminal Run, and installs one successor. A
concurrent request either becomes that Owner or joins it. No process-local
lock, direct SQL, or datastore call is used.

## P8-45

Persisted Report reads require authorization and one exact Report ID. Expiry
requires the retention role and policy age, writes an attributable tombstone,
removes the payload, and does not permit the expired result to be reused.
MCP exposes exact-report projections only; it has no history enumeration
selector.

## Evidence

- `ReviewDiagnosisPersistenceSpec`: SQLite-backed admission, terminal
  successor, exact read/denial, expiry/tombstone, and fresh successor.
- `CarReviewMcpReadProjectionSpec`: exact authorized bounded reads and denial
  of unbounded/absent reads.
- `ComponentFactorySpec`: generated MCP tool surface omits history
  enumeration.
