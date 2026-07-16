# P5-24 Provider Behavior Matrix

status=complete
phase=5
checklist=P5-24
updated_at=2026-07-16

## Context

P5-12 and P5-20 establish detailed provider-admission and execution contracts,
but Phase 5 needs one direct executable statement that proves every required
provider outcome remains present as the Cozy transport is added.

## Decision

`CarReviewProviderBehaviorMatrixSpec` is the phase-level matrix. It uses the
canonical v1 Cozy descriptor, request, and bundle rather than a parallel test
format, and delegates detailed parser, registry, and transport behavior to
their existing executable specifications.

The matrix proves:

- compatible admission preserves the exact Cozy provider identity and explicit
  runtime-evidence limitation;
- a mismatched target digest is refused as incompatible without changing the
  provider identity;
- cancellation and timeout remain attributable terminal states; and
- an admitted request digest runs once and later calls return the cached bundle.

## Evidence

`sbt --batch 'testOnly
org.simplemodeling.textus.cbdsupport.CarReviewProviderBehaviorMatrixSpec'`
passed 3 specifications on 2026-07-16. The detailed supporting specifications
are `CarReviewProviderBundleAdmissionSpec`,
`CarReviewProviderExecutionCoordinatorSpec`, and
`CozyCarReviewProviderRunnerSpec`.

## Consequence

The provider framework and Cozy stage is complete. P5-30 starts the sbt-cozy
evidence bridge; it must submit source-attributable build evidence rather than
creating an independent quality or gate result.
