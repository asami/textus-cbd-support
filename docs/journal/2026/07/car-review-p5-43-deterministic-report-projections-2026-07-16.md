# P5-43 Deterministic Review Report Projections

status=complete
phase=5
checklist=P5-43
updated_at=2026-07-16

## Decision

CBD Support owns projections of its canonical Review Report. Text, canonical
JSON, and HTML retain one Report's Review/gate identity. HTML escapes the text
projection and does not construct a new conclusion. SARIF is deliberately
lossy: it includes only location-bearing Findings, maps canonical severity to
SARIF level, retains the Observation ID, and records both the omitted Finding
count and `location-bearing-findings-only` policy.

## Evidence

`CarReviewReportProjectionSpec` renders the representative canonical Report
twice and proves equality of every projection, common Review/gate identity,
canonical report digest presence, HTML rendering, and the declared SARIF loss
boundary.
