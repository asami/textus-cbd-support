# P5-63 Local/CI Review Workflow Matrix

status=complete
phase=5
checklist=P5-63
updated_at=2026-07-16

## Decision

Local stdin and CI HTTP submission are transport adapters only: both admit the
same path-free provider document through the same CBD Review Application. The
workflow matrix therefore compares the full canonical response, not merely a
rendered CLI summary or gate label.

## Evidence

- `CarReviewCliSpec` proves the exact canonical response from local stdin and
  authorized `application/json` HTTP admission is identical for the same
  provider evidence.
- `CarReviewAssessmentGateBuilderSpec` proves canonical pass, Finding failure,
  and provider Unknown gate behavior.
- `CarReviewProviderBehaviorMatrixSpec` proves incompatible evidence,
  cancellation, and timeout remain attributable without fallback.
- `CarReviewRepositorySpec` proves stale gate evidence is rejected after its
  retained Report/attestation binding is no longer valid.
- The focused matrix command passes 20 tests across those four suites.

## Consequence

P5-63 is complete. The representative SAR route remains P5-64 release
validation, rather than a substitute for this deterministic workflow matrix.
