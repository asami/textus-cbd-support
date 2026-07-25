# CAR Review Cost View Contract v1

status=stable
phase=Phase 8 P8-53
updated_at=2026-07-24

## Boundary

Cost View is a CBD-owned, read-only projection over canonical CAR Review
Evidence and Observations. It never calls a provider, reads a price sheet,
executes an AI model, estimates currency, or modifies a Report. Runtime,
deployment, billing, Textus AI, Cozy, and sbt-cozy integrations contribute only
through the ordinary admitted provider-bundle boundary.

## Cost optimization evidence

Each item carries these bounded values:

| Value | Rule |
| --- | --- |
| Current architecture, cost driver, optimization | Required bounded text; identifies the optimization without implying a saving. |
| Expected reduction | Optional design-time value and unit. It is never presented as measured. |
| Measured reduction | Optional observed value and unit. It requires both comparison period and normalized unit. |
| Quality constraints and operational trade-offs | Required nonempty bounded lists; cost reduction must not hide freshness, availability, security, quality, fallback, or operational burden. |
| Confidence | `low`, `medium`, or `high`, from provider evidence. |

`Static Web App` maps to infrastructure/operations/resource-efficiency/work
avoidance. `Gemma + MCP` maps to operations/development/resource-efficiency/work
avoidance. Both mappings remain attributable to the emitting provider and
Evidence.

## Deterministic outcome

- An expected-only opportunity is `unknown` with
  `cost-measurement-unavailable`; it is not an Assurance.
- A measured value with both comparison period and normalized unit is an
  Assurance, subject to the provider's attributable Evidence.
- A measured value missing either condition is a medium Finding.
- A malformed optimization declaration, including missing quality constraints
  or operational trade-offs, is a medium Finding.

The initial provider is deliberately currency-neutral. It can report units such
as requests/day, percent origin requests, tokens, or normalized compute work;
it must not invent monetary savings.
