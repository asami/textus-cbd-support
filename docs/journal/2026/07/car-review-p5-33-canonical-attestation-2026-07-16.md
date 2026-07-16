# P5-33 Canonical CI Attestation

status=in-progress
phase=5
checklist=P5-33
updated_at=2026-07-16

## Decision

CBD Support, not `sbt-cozy`, creates the Review attestation. The canonical
submission response now carries a `review-attestation` document in addition to
the Report and Gate. Its identity and SHA-256 digest bind the exact Review,
Report, target digest, profile, provider/rule-set/bundle identities, and gate.

`sbt-cozy` writes this unchanged document as
`target/cbd-review/sbt-cozy/canonical-attestation.json` and exposes it through
`cozyReviewAttestation`. Before it writes any artifact, it checks the
attestation against the canonical Report's Review ID, Report ID/digest, target
digest, profile, complete provider/rule-set/bundle collection, and Gate. It
does not manufacture an attestation when CBD omitted one or the binding drifts.

## Evidence

- `CarReviewCanonicalResponseApplicationSpec` proves CBD creates an
  attestation bound to its Report, target, profile, provider, and gate.
- `SbtReviewReportArtifactsSpec` proves artifact retention of a bound
  attestation and refusal of a mismatched attestation.

## Remaining Work

P5-33 remains open until a real CBD gateway response produces all canonical
CI artifacts on a configured sbt task path. Attestation release/publish use is
separately opt-in P5-35 work.
