# Phase 8 P8-33: Ordinary publication boundary

date=2026-07-23
phase=Phase 8
status=completed-reviewed-and-validated

## Context

CAR Review provides explicit `cozyReviewPublish` and
`cozyReviewDistribute` aliases. Phase 8 must prove that adding Review artifacts
and a gate does not change ordinary sbt `publish`, `publishLocal`, or Cozy
distribution behavior, and that it does not create a Review deployment task.

## Decision

The existing CAR scripted fixture makes `cozyReviewGate` fail immediately. It
then executes ordinary release `publish`, ordinary `cozyDistribute`, and
SNAPSHOT `publishLocal` through marker implementations. Each must succeed
without reading CBD endpoint configuration or materializing Review artifacts; a
hidden gate dependency would fail the fixture.

The Review task-surface specification keeps the only allowed Review operations
as `publish` and `distribute`. A requested `deploy` operation is rejected; this
phase does not add an implicit or explicit Review deployment integration.

## Verification executed

- Focused `SbtReviewPublicationBoundarySpec` passed, checking the operation
  boundary and deployment refusal.
- `scripted cozy/project-yaml-car` passed, checking standard publish,
  distribution, and snapshot local publication against the failing Review-gate
  sentinel.
- Full `sbt-cozy` validation passed: 108 tests, 24 suites, no failures.
- Independent re-review found no actionable issue. The CBD executable-coverage
  manifest links this slice to the `sbt-cozy` boundary specification.
