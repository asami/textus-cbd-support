# P5-13 Reconciliation Boundary — 2026-07-16

status=in-progress
checklist=P5-13

## Decision

CBD Support reconciles only an already-admitted bundle paired with its typed
admission. It never calls a provider from reconciliation and ignores a repeated
bundle digest in the same reconciliation input. Canonical report-local Evidence
and Observation IDs prefix provider-local IDs with the provider identity, while
Evidence retains the provider-local ID and admitted bundle digest.

## Non-Winner Semantics

Observations sharing a rule and subject remain separate canonical records. The
reconciler returns their conflict explicitly; it does not pick a preferred
provider or rewrite either observation. A provider Assurance with no admitted
Evidence is converted to canonical Unknown and receives an attributable
limitation. This prevents absence of a Finding or an unsupported positive claim
from becoming a CBD conclusion.

## Scope Boundary

This initial P5-13 slice creates canonical Evidence/Observation reconciliation
and duplicate-bundle protection. It does not yet construct capability
assessments, profile gate results, durable reconciliation state, or multi-run
baseline behavior; those remain P5-13/P5-14 work.
