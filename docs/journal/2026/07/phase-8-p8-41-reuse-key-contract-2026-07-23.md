# Phase 8 P8-41: Diagnosis reuse-key contract

date=2026-07-23
phase=Phase 8
status=completed

## Decision

`textus.cbd.review-reuse-key.v1` is a pure pre-execution identity. It binds
target identity/digest, profile, baseline digest, selected rule sets, provider
selection/version and availability policy, accepted Evidence snapshots including
runtime Evidence, and the profile/gate/reconciliation/suppression policy
bindings. Each array is canonically sorted before SHA-256 calculation.

Run IDs, times, renderer output, credential references, raw provider payloads,
and filesystem paths are excluded. They are volatile, sensitive, or provider
output rather than a reusable diagnosis input.

Independent review made two admission rules explicit: only the currently
supported canonical Review schema is accepted, and Evidence snapshot uniqueness
is a structured `(class, snapshot ID, provider ID, provider version)` identity,
not a delimiter-concatenated string. This keeps a colon-qualified identifier
from being mistaken for a duplicate snapshot.

## Boundary

P8-41 calculates and validates the identity only. P8-42 performs lookup,
database persistence, compatible completed-Run reuse, and concurrent request
coalescing. P8-43 owns non-success/expired outcomes and P8-45 owns access.

## Verification executed

- `CarReviewReuseKeySpec` verifies order independence, invalidation for every
  conclusion-affecting input class, and refusal of ambiguous/incomplete inputs.
- Focused `CarReviewReuseKeySpec` passed: 4 tests in 1 suite after the review
  fixes.
- `sbt --batch Test/compile` and `git diff --check` passed after the review
  fixes.
- The final independent re-review was clean. `Phase8ExecutableCoverageSpec`
  and the complete CBD Support suite passed: 253 tests in 62 suites.
