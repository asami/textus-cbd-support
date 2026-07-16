# P5-44 Bounded Redacted MCP Read Model

status=complete
phase=5
checklist=P5-44
updated_at=2026-07-16

## Decision

`CarReviewMcpReadApplication` is the report-read boundary for the MCP
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
`ComponentFactorySpec` proves the generated Retrieval CML operations publish
as MCP tools, the component submission retention hook stores canonical reports
for the same bounded reader, exact Report-ID reads work for a `viewer`, and an
unauthorized or missing read fails.
