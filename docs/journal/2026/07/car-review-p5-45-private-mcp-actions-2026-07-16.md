# P5-45 Private Review MCP Actions

status=complete
phase=5
checklist=P5-45
updated_at=2026-07-16

## Decision

MCP eligibility is an explicit allowlist of bounded read projections. All
other Review operations are private by default. The policy names Review start,
cancellation, retention deletion/purge, filesystem configuration, external
provider enablement, and AI Review enablement as private categories. A future
operation needs a separate policy decision before it can join the read-only
allowlist.

## Evidence

- `CarReviewMcpExposurePolicySpec` proves the complete read-only/private table
  and private treatment of unknown actions.
- `ComponentFactorySpec` proves generated CBD MCP publication includes
  Retrieval reads while Review administration/submission remains absent.
