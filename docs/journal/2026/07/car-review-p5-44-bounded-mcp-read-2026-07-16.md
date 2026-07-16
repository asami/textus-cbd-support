# P5-44 Bounded Redacted MCP Read Model

status=in-progress
phase=5
checklist=P5-44
updated_at=2026-07-16

## Decision

`CarReviewMcpReadApplication` is the report-read boundary for future MCP
operations. It admits only `review.read-run` roles, addresses a single retained
Report ID, and returns summary/report/Finding/Assurance projections. Findings
and Assurances are capped at 100 records; there is no arbitrary report-history
enumeration operation.

The projection deliberately excludes Evidence facts and observation rationale,
sanitizes credential-shaped messages, and exposes only a basename for local
paths. Provider identity, canonical IDs, rule IDs, severity, and bounded
locations remain available for attributable diagnosis.

## Evidence

`CarReviewMcpReadProjectionSpec` proves authorized summary/report/Finding/
Assurance reads retain canonical identity while withholding a test credential,
full local path, facts, and unbounded/history-like access.

## Remaining Work

Add corresponding CML/MCP operations and connect their retained Report source
to completed Review execution before checking P5-44.
