# Phase 8 P8-32: CI gate and offline determinism

date=2026-07-23
phase=Phase 8
status=completed-reviewed-and-validated

## Context

P8-31 made an attestation-digest CI artifact directory but the sbt gate still
read a canonical-response artifact directly. Its CI policy also described
allowed provider kinds without applying that choice at the submission boundary.
The CBD bundle kept PDF-renderer limitations but did not carry the safe,
attributable Report/provider limitations into the CI manifest input.

## Decision

CBD Support projects Report limitations through the existing delivery-safe
redaction boundary, renders each as `scope:code [subject] message`, and merges
them deterministically with renderer limitations in `artifactBundle`.
`sbt-cozy` materializes those exact strings in `review-artifacts.json`.

The sbt gate is a manifest consumer. It validates the manifest schema/document
type and the exact `gate.result` to `exitCode` mapping: pass=0, fail=2, and
unknown=3. It does not calculate a gate locally. The sbt task reports a failed
task for fail or unknown while retaining the CBD-owned value and code in both
the manifest and task message.

The current provider selection is explicitly the deterministic local pair
`cozy` and `sbt-cozy`; `SbtReviewCiPolicy` validates it before any provider
evidence or CBD HTTP work. External, AI, or remote-gateway execution remains
an explicit policy/profile path and is rejected in standard CI when not
approved.

## Verification executed

- CBD artifact-bundle specification proves attributable provider limitation
  retention and path redaction (4 tests).
- sbt-cozy gate specification proves all three gate/exit pairs and rejects a
  mismatched, incomplete, and path-substituted manifest; its materialization
  and CI-policy specifications prove the retained attempt behavior (16 focused
  tests).
- sbt-cozy policy specification proves the selected local provider set and
  rejects an external provider in standard CI.
- Full repository regression passed: 105 sbt-cozy tests and 247 CBD Support
  tests. The dedicated loopback probe passed on isolated server/fixture ports,
  retained the failing `gate=fail, exitCode=2` manifest pair, and observed the
  expected sbt `cozyReviewGate` failure message.
