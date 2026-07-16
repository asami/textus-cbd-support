# P5-35 Opt-in Release Gate

status=complete
phase=5
checklist=P5-35
updated_at=2026-07-16

## Decision

`sbt-cozy` does not alter ordinary publication or distribution behavior.
`publish`, `cozyPublishCar`, `cozyPublishSar`, `cozyDistributeCar`, and
`cozyDistributeSar` remain independent of CBD Review and therefore retain
their existing version and artifact behavior.

An operator who wants a release to depend on a Review must explicitly run
`cozyReviewPublish` or `cozyReviewDistribute`. Each task invokes
`cozyReviewGate`, which requires the CBD-owned canonical response and passing
gate/attestation path, before it delegates to the selected CAR or SAR release
task. No task deploys automatically; deployment remains outside this bridge.

## Evidence

- `CozyPublishVersionPolicySpec` proves Review-gated CAR/SAR publish and
  distribute mapping, invalid mapping refusal, and unchanged ordinary publish
  labels.
- `sbt --batch 'testOnly org.goldenport.cozy.CozyPublishVersionPolicySpec'`
  passed 8 tests on 2026-07-16.
