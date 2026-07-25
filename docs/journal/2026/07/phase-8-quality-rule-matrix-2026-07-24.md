# Phase 8 quality-rule matrix

date=2026-07-24
phase=Phase 8 P8-50
status=completed

## Decision

CAR Review exposes a total quality-rule matrix derived from the CBD capability
catalog. Each base capability has a stable check ID, applicability, sorted
required Evidence kinds, authority, explicit Unknown result and limitation
when Evidence is absent, and an evidence-backed maturity ceiling.

The matrix is a policy declaration, not a claim that a provider has executed a
check. Provider results must still pass through the admitted provider-bundle
and canonical Report boundaries.

## MCP and Skill

MCP and Skill support use distinct deterministic base checks. Textus-managed
MCP and standard Skill installation can provide support Evidence automatically.
Their content quality is deliberately separate: MCP descriptions and permitted
operations, and Skill task guidance, authority, prerequisites, limitations,
and recovery information are covered by advisory `.content` checks. Advisory
results remain `unassessed` until deterministic or human corroboration; they
cannot manufacture an Assurance or change the Review gate.

## Evidence

`CarReviewQualityRuleMatrixSpec` proves total catalog coverage, stable unique
check IDs, Evidence/authority/Unknown/limitation/maturity semantics, and the
separate MCP/Skill support and content rows. `Phase8ExecutableCoverageSpec`
keeps every completed Phase 8 checklist item bound to an existing executable
anchor.
