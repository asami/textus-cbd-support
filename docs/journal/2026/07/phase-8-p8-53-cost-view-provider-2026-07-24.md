# Phase 8 P8-53: Cost View provider and projection

status=completed
phase=Phase 8 P8-53
updated_at=2026-07-24

## Decision

CBD Support owns Cost View as a projection of canonical Review data. The first
provider admits bounded scenarios for Static Web App and Gemma + MCP rather
than calculating a billing total or making a local-model cost assumption.

## Result

`CarReviewCostScenarioProviderRunner` retains current architecture, cost driver,
optimization, expected reduction, measured reduction, comparison period,
normalized unit, quality constraints, trade-offs, and confidence in provider
Evidence. Static Web maps to reduced origin/server work; Gemma + MCP maps to
reduced commercial-model work while retaining local-compute, fallback, and
quality costs.

Expected reduction and measured reduction are distinct by type and projection.
Missing measurement produces Unknown; a claimed measurement without comparison
period or normalized unit is a deterministic Finding. This prevents a planning
estimate or per-call API absence from becoming a cost-saving assertion.

`CarReviewCostViewProjection` is pure and read-only. It exposes only canonical
Evidence/Observation identities and the admitted fields; it does not invoke
providers or generate a new conclusion.

## Evidence

`CarReviewCostScenarioProviderRunnerSpec` proves an expected-only Static Web
scenario remains Unknown, a normalized Gemma + MCP measurement becomes
Assurance, the projection keeps both distinct, and an unqualified measured
claim is a Finding.
