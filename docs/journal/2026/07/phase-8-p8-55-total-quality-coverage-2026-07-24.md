# Phase 8 P8-55: Total quality coverage projection

status=completed
phase=Phase 8 P8-55
updated_at=2026-07-24

## Decision

The quality matrix is a total declaration, but an unrun provider must not be
silently absent from a Review view. CBD Support therefore adds a read-only
coverage projection over canonical Report data and the base matrix rules.

## Result

Every catalog capability appears exactly once. A capability with canonical
mapped Observation/Evidence is `observed` and keeps those exact identities. A
capability with no provider result is `unknown`, accompanied by the matrix's
missing-Evidence limitation and retryability. The projection neither calls a
provider nor writes a Report, so it cannot fabricate an Assurance, maturity,
or gate change.

## Evidence

`CarReviewQualityCoverageProjectionSpec` proves total sorted 155-capability
coverage, observed Domain identity retention, and explicit Security Unknown.
