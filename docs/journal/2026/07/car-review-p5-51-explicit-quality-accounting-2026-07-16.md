# P5-51 Explicit Quality Accounting

status=complete
phase=5
checklist=P5-51
updated_at=2026-07-16

## Decision

CBD projects quality as deterministic, per-capability accounting. Each
capability keeps its applicability, maturity, optional coverage, confidence,
provider/evidence/observation attribution, strengths, gaps, and references to
Unknown observations from the canonical Report. The projection deliberately
does not calculate or expose a single quality score.

This keeps the meaning, source, and uncertainty of each assessment inspectable
and prevents an apparent aggregate conclusion from obscuring incompatible,
inapplicable, or Unknown evidence.

## Evidence

`CarReviewQualitySummarySpec` decodes the representative canonical Report and
proves the projection is capability-ID ordered, retains the Report's coverage
and provider attribution, carries the runtime Unknown reference, and has no
`score` product field.
