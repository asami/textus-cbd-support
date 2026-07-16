# P5-34 Deterministic Offline CI

status=complete
phase=5
checklist=P5-34
updated_at=2026-07-16

## Decision

The `sbt-cozy` bridge owns the build-side admission boundary for a Review
execution. Standard CI is selected by `CI=true` or the explicit
`review.ci.profile: standard` setting. Its default providers are only the
local deterministic `cozy` and `sbt-cozy` providers; a CBD HTTP endpoint is
also local-only (loopback) unless `review.ci.network_gateway_enabled` is
explicitly true.

External and AI providers stay disabled by default and have separate named
opt-ins: `review.ci.external_providers_enabled` and
`review.ci.ai_providers_enabled`. This makes a remote CBD endpoint independent
of future cost-bearing or nondeterministic provider admission.

CBD Support remains the only authority that creates the canonical report,
gate, and attestation. Before sbt-cozy writes those response-derived artifacts,
it verifies the attestation SHA-256 digest and refuses secret-bearing JSON
field names and credential-shaped values. The task logs only the output path;
the source project and gateway credentials are not copied into Review output.

## Evidence

- `SbtReviewCiPolicySpec` proves default standard-CI local provider admission,
  loopback gateway enforcement, explicit named opt-ins, and invalid-profile
  refusal.
- `SbtReviewReportArtifactsSpec` proves canonical attestation-digest checking
  and refusal before artifact retention when a credential-shaped value occurs.
- `sbt --batch test` in `sbt-cozy` passed 92 tests on 2026-07-16.

## Consequence

P5-34 is complete. P5-31 through P5-33 still require their configured,
real-CBD-gateway materialization scenario; P5-34 does not convert that
separate transport/release mismatch into an external CI exception.
