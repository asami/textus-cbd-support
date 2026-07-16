# CAR Review P5-11 Run Lifecycle Foundation — 2026-07-16

## Work

The P5-03 Review Run wire contract and P5-04 authorization boundary were
examined against the CNCF Job runtime before exposing Review operations.

## Decision

Review Run state is a CBD-owned projection of CNCF Job execution, not a second
scheduler. `CarReviewRun` retains the v1 Run document with distinct state,
failure, Review, target, profile, report, digest, provider, limitation, and
timestamp types. `CarReviewRunCodec` strictly rejects unknown fields, malformed
identities, invalid time order, duplicate providers, incomplete provider
completion, and inconsistent terminal/report/failure shapes.

`CarReviewRunLifecycle` admits one Review and projects CNCF status as follows:

- Submitted becomes `queued`;
- Running becomes `running` while preserving an existing `cancelling` intent;
- Suspended stays non-terminal and records `cncf-job-suspended`;
- Cancelled becomes `cancelled`;
- Succeeded becomes `completed` only with a report ID and digest, otherwise it
  becomes failed with `review-report-missing`; and
- Failed becomes `failed` with the supplied stable code or `cncf-job-failed`.

Terminal Run content is immutable. Repeating the same terminal CNCF readback is
idempotent, while later provider, limitation, report, or failure mutation is
rejected. Time-regressing updates and completion attached to a non-success Job
are rejected before projection.

## Evidence

- `CarReviewRunModel.scala`
- `CarReviewRunCodec.scala`
- `CarReviewRunLifecycle.scala`
- `CarReviewRunLifecycleSpec`: 5 executable scenarios

The scenarios cover strict canonical decode, typed lifecycle identity,
admitted/queued/running/completed progress, cancellation intent and terminal
cancellation, explicit and missing-report failure, attributable limitations,
terminal idempotency/immutability, stale update rejection, and incompatible
Job payload rejection.

## Boundary

This slice does not yet publish `startReview`, `getReviewRun`, or
`cancelReview`. The next P5-11 slice binds one Review ID to one actual CNCF Job
ID, applies the P5-04 roles at operation admission, keeps start/cancel private
to MCP, and exposes only the authorized bounded Run query through MCP.
