# P5-64 Release-Boundary Validation

status=complete
phase=5
checklist=P5-64
updated_at=2026-07-16

## Evidence

- `scripts/check-car-abi.sh` builds the CAR, confirms the source ABI manifest
  matches generated CML metadata and the packaged CAR (17 operations), and
  passes compatible-addition, breaking-minor rejection, and intentional-major
  transition governance checks.
- Normal CAR lint reports no `FAIL`. Its ambient clock/environment/filesystem/
  shell and first-release ABI-baseline `WARN` entries are existing residual
  framework-boundary debt, recorded for P5-66 rather than publication-ready
  claims.
- `CNCF_RUNTIME_DEV_DIR=/Users/asami/src/dev2025/cloud-native-component-framework scripts/check-cbd-standalone.sh`
  builds the representative CAR/SAR, checks the retrieval MCP catalog, probes
  the private Review HTTP envelope, and reports
  `CBD_STANDALONE_INDEPENDENCE_OK`, `CBD_REVIEW_SUBMISSION_OK`, and
  `CBD_STANDALONE_SAR_OK`.

## Consequence

P5-64 is complete without publishing. The current target remains a SNAPSHOT;
the missing released ABI baseline is explicit residual readiness debt, not a
claim that a public compatibility comparison has been made.
