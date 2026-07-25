# CAR Review Quality Provider Authority Contract v1

status=stable
phase=Phase 8 P8-54
updated_at=2026-07-24

## Policy

Every quality-provider execution supplies one authority and finite cost policy:

| Authority | Allowed result |
| --- | --- |
| `deterministic` | Non-advisory static Evidence and canonical Assurance/Finding/Unknown. Runtime Evidence is refused. |
| `runtime` | Assurance only when every referenced Evidence item is declared and has kind `runtime-observation`. |
| `advisory` | Only `ai.advisory.*` Finding or Unknown. Advisory Assurance is refused. |

`declaredCostUnits` and `maximumCostUnits` are nonnegative and declared cost may
not exceed the maximum. Coordinator policy execution performs this preflight
before runner invocation.

## Evidence boundary

The descriptor is authoritative for admissible Evidence kinds. A bundle cannot
introduce an undeclared kind. Evidence facts reject secret, credential,
password, API-key, authorization, raw request/response, endpoint, and URL
keys. The policy operates after strict schema/digest/target/provider admission;
it cannot repair a malformed bundle or select a fallback.

Every policy refusal is an attributed incompatible provider outcome with a
bounded limitation. Missing runtime Evidence is not converted to an Assurance;
an advisory candidate cannot become an Assurance merely because it has Evidence.
