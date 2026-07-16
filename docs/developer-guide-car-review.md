# CAR Review Developer Guide

CBD Support is the sole CAR Review Application owner. It owns Review Run
lifecycle, canonical report/gate/attestation construction, retention, Web and
MCP projections, and policy. A provider supplies only a versioned descriptor,
request, and evidence bundle bound to one Review and target.

## Provider Boundary

Do not add a provider-specific CBD wire client, credential parser, report, or
gate implementation. Cozy analysis, sbt build evidence, and Textus AI
structured generation remain provider-owned. CBD admits their bounded evidence
through `textus.cbd.review-provider.v1` and produces the one
`textus.cbd.review-report.v1` result. The normative contracts are:

- [provider evidence](spec/car-review-provider-contract.md);
- [canonical report and attestation](spec/car-review-report-contract.md);
- [security and reproducible CI](spec/car-review-security-contract.md); and
- [local/HTTP submission](spec/car-review-submission-contract.md).

## Local and CI Integration

`sbt-cozy` runs Cozy locally, collects its own build evidence, and submits the
paired provider documents to CBD. CBD never receives a workspace path, command,
policy template, or caller-selected gate. The generated HTTP envelope is the
only server transport; client roles and credentials are not forwarded.

Run the representative CI-profile route without publishing:

```sh
CNCF_RUNTIME_DEV_DIR=/Users/asami/src/dev2025/cloud-native-component-framework \
  CBD_STANDALONE_SBT_COZY_REVIEW_PROBE=true \
  scripts/check-cbd-standalone.sh
```

The fixture verifies canonical JSON, HTML, SARIF, and attestation artifacts
under `target/cbd-review/sbt-cozy`, and proves a non-pass CBD gate stops the
explicit `cozyReviewGate` task. Ordinary publish/distribute tasks remain
ungated; only named Review-gated release tasks may require a passing gate.

## User Surfaces and Residual Boundaries

CLI local and server-backed modes use the same inner submission document. Web
and MCP read only authorized, redacted canonical report projections. Start,
cancel, retention administration, filesystem access, external providers, and
AI-cost-bearing actions remain private to MCP.

The current CAR is a SNAPSHOT and has no released ABI baseline. Use
`scripts/check-car-abi.sh` for current-surface/package consistency and
transition governance; do not represent that first-release condition as public
compatibility validation.
