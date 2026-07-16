# CAR Review P5-03 Report Contract — 2026-07-16

## Work

The CBD-owned result boundary was promoted from the architecture and provider
contract into a normative Review Run, canonical Review Report, and Review
attestation specification.

## Decision

`textus.cbd.review-report.v1` is the single v1 schema identity for three strict
document types:

- `review-run` is the lifecycle projection for one authorized execution;
- `review-report` is the immutable canonical result for one admitted evidence
  set; and
- `review-attestation` binds the target, report, profile, providers, rule
  sets, and gate result for CI or later release policy.

The canonical report keeps Evidence and provider identity intact. Finding,
Assurance, and Unknown are distinct Observation types: only a Finding has
severity, and absence of a Finding does not create an Assurance. A non-active
disposition retains its reason and author instead of erasing the Observation.

Capability assessment separates applicability, maturity, coverage,
confidence, strengths, and gaps. Coverage uses integer subject counts and
basis points, with Unknown included explicitly in the denominator. Static
Evidence alone cannot establish Operational maturity.

CBD Support owns the gate after evidence reconciliation. Provider output does
not become a competing report or gate. A completed Run and its attestation
must agree with the canonical report on target, profile, providers, report
digest, and gate result. Normalized report and attestation SHA-256 digests make
stale or mismatched CI evidence detectable.

## Evidence

- `docs/spec/car-review-report-contract.md`
- `docs/spec/schema/car-review-report-v1.schema.json`
- `docs/spec/examples/car-review-run-v1.json`
- `docs/spec/examples/car-review-report-v1.json`
- `docs/spec/examples/car-review-attestation-v1.json`
- `CarReviewReportContractSpec`: 4 scenarios passed

The executable specification verifies the controlled vocabulary, unique and
resolving local references, provider attribution, Finding-only severity,
disposition accountability, exact integer coverage, cross-document identity,
and recomputable report and attestation digests.

## Boundary

This slice does not decide target-path authorization, process or network
containment, credential resolution, redaction, AI input policy, MCP exposure,
retention, or deterministic/offline CI execution. Those remain P5-04 work.
Runtime model, lifecycle, persistence, and provider admission implementation
begin under P5-10 through P5-14 after the security contract is fixed.
