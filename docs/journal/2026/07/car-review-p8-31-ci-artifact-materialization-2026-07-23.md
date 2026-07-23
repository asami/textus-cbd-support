# CAR Review P8-31 CI Artifact Materialization

date=2026-07-23
phase=Phase 8
checklist=P8-31
status=completed

## Decision

CBD Support remains the Review and renderer authority. Its private provider
submission response carries the exact canonical response and one
`review-artifact-bundle`; the bundle contains CBD-rendered Markdown and PDF
bound to the canonical Report digest. `sbt-cozy` is a bounded CI client: it
does not rerun providers, regenerate a Report, substitute a gate, or invent a
renderer result.

## Implementation

`CarReviewArtifactBundle` validates the canonical Report and outer gate, then
renders the existing delivery projection to Markdown/PDF in memory. The
`sbt-cozy` materializer validates the bundle schema, Report digest, Report and
attestation binding, and gate policy before atomically moving a complete
attestation-digest attempt directory into `target/cbd-review/`. That directory
contains canonical response, Report, attestation, Markdown, PDF, HTML, SARIF,
and `review-artifacts.json` with byte digests.

The pre-existing HTML and SARIF projections remain separate authorized local
projections of the same admitted Report. The standard `cozyReviewGate` task
continues to consume the CBD-owned result and fails a `fail` gate; normal
publish/distribute tasks are unchanged.

## Validation

- `sbt --batch test` in `sbt-cozy`: 99 tests passed.
- `sbt --batch test` in `textus-cbd-support`: 245 tests passed.
- `CNCF_SERVER_PORT=19548 CBD_STANDALONE_FIXTURE_PORT=19549
  CNCF_RUNTIME_DEV_DIR=/Users/asami/src/dev2025/cloud-native-component-framework
  CBD_STANDALONE_SBT_COZY_REVIEW_PROBE=true scripts/check-cbd-standalone.sh`:
  passed. The loopback SAR probe observed one real CBD response, a complete
  digest-keyed `sbt-cozy` artifact directory, and the expected `fail` gate
  task failure.

## Post-implementation review correction

The first review found that the bounded-output contract was not enforced and
that the manifest discarded CBD-owned limitations. The review fix now makes the
per-artifact bound 16 MiB and the complete-attempt bound 64 MiB, rejects
credential-shaped Markdown/PDF text, deletes a failed temporary directory, and
records up to 64 bounded limitations in the manifest. A fresh re-review also
identified a multiline credential redaction bypass in the pre-existing
Report/attestation projection guard. The guard now uses whole-text matching and
the focused specification proves that neither canonical document can carry a
multiline credential into retained artifacts. The corresponding schema, example,
contract, and focused specifications were updated. The private CBD action also
converts unexpected bundle-rendering failures into a structured operation
failure rather than throwing. P8-31 remained open until its final fresh
re-review and release commit.

The subsequent fresh review found that canonical Report, attestation, HTML, and
SARIF payloads were bounded only after temporary files were opened. The
materializer now validates their actual retained byte representation, including
the newline added to JSON/SARIF files, before creating the temporary attempt
directory. A regression specification proves that an oversized canonical
response leaves no attempt directory.

The same review identified that PDF Base64 was decoded before the decoded-byte
bound could be checked. sbt-cozy now rejects an encoded PDF whose length
cannot fit in the 16 MiB artifact limit before invoking the decoder; its
regression specification proves no decoded oversized payload or CI attempt is
created.

## Completion

P8-31 passed its final clean re-review on 2026-07-23. Full validation passed:
103 tests in sbt-cozy and 246 tests in CBD Support. The strict CAR lint review
reported no failure; its pre-existing ABI-baseline and development SNAPSHOT
warnings remain release-readiness debt outside this slice.

## Runtime probe follow-up

The current CNCF runtime requires `MCP-Protocol-Version: 2025-11-25` and
exposes six fixed runtime tools in addition to the component MCP surface.
The standalone probe now sends the required header and asserts that exact
CBD-plus-runtime surface, while continuing to reject SIE exposure.
