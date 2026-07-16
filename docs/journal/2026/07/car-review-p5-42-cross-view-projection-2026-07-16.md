# P5-42 Canonical Cross-view Projection

status=in-progress
phase=5
checklist=P5-42
updated_at=2026-07-16

## Decision

`CarReviewViewProjection` derives the CNCF, implementation, and quality view
collections exclusively from canonical Report mappings. It does not rerun a
provider, copy a provider conclusion, or invent a view-local identity. Every
view item retains the Report's Evidence IDs, Observation IDs, provider/rule/
bundle attribution, and normalized implementation locations.

## Evidence

`CarReviewViewProjectionSpec` projects the representative Report and proves
that its CNCF, implementation, and quality view keys retain the original
Observation/Evidence identity, Cozy provider link, and `project.yaml`
implementation location.

## Remaining Work

Publish completed Report view projections through the bounded authorized Web
and MCP read surfaces. That integration is necessary before P5-42 can close.
