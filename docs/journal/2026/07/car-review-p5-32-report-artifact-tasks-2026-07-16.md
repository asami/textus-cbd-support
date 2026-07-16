# P5-32 sbt-cozy Report Artifact Tasks

status=complete
phase=5
checklist=P5-32
updated_at=2026-07-16

## Context

`sbt-cozy` must consume, not recreate, the CBD-owned canonical Review Report
and Gate. Local CI artifacts need stable names so that later attestation and
publication policy can bind them without changing standard publication tasks.

## Decision

`cozyReviewSubmit` is the single submission task. After validating that the
outer response gate exactly equals the Report's `gate.result`, it writes these
files under `cozyReviewEvidenceDir`:

- `canonical-response.json`;
- `canonical-report.html`; and
- `canonical-report.sarif`.

`cozyReviewCanonicalJson`, `cozyReviewReportHtml`, and
`cozyReviewReportSarif` expose those stable outputs. `cozyReviewGate` is the
only gate-applying task and fails unless CBD returned `pass`; it does not infer
or override a result from build-task evidence.

HTML is a safe projection of the canonical report. SARIF is deliberately
lossy: it contains only Findings with a declared location, maps severity to a
SARIF level, and records the omitted-Finding count and
`location-bearing-findings-only` policy. It does not project Assurances,
Unknowns, Assessments, or any new quality result.

## Evidence

- `SbtReviewReportArtifactsSpec` proves deterministic artifact generation,
  HTML escaping, location-bearing Finding projection, explicit omission, gate
  preservation, and response/report gate mismatch refusal.
- `CNCF_RUNTIME_DEV_DIR=/Users/asami/src/dev2025/cloud-native-component-framework CBD_STANDALONE_SBT_COZY_REVIEW_PROBE=true scripts/check-cbd-standalone.sh`
  runs `cozy/review-submit` with `CI=true` against the representative CBD HTTP
  endpoint. Its verification task obtains `cozyReviewCanonicalJson`,
  `cozyReviewAttestation`, `cozyReviewReportHtml`, and
  `cozyReviewReportSarif`; the same probe then confirms that `cozyReviewGate`
  rejects CBD's actual `fail` result.

## Remaining Work

P5-32 is complete. P5-33 validates the corresponding canonical attestation;
P5-43 remains the broader cross-surface renderer and SARIF contract.
