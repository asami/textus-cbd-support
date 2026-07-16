# P5-42 Canonical Cross-view Projection

status=complete
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

## Integration Evidence

`getReviewViews` is a Retrieval CML query and authenticated Web form as well
as an MCP-ready read-only operation. `CarReviewMcpReadApplication.views`
addresses one retained Report ID and reuses the same authorization boundary as
the other report projections. `ComponentFactorySpec` proves the generated MCP
tool, Web form, retained Report source, and all canonical implementation
locations remain visible rather than being reduced to a selected location.
