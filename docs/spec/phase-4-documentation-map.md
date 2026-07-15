# Phase 4 Documentation Map

## Purpose

This map is the P4-40 closure contract. It keeps the user-facing and maintainer-
facing documents aligned with the normative Phase 4 static specifications. A
summary document may omit implementation detail, but it must not change the
authority, fallback, failure, or verification semantics linked here.

## Normative Contract Map

| Phase 4 area | Normative static specifications | Required invariant |
|---|---|---|
| Source authentication | [source authentication](source-authentication.md), [information-source security](information-source-security.md) | authorization precedes source-owned late resolution; secrets and references are not projected; lifecycle failures are stable and sanitized |
| Production refresh and cache | [information-source refresh](information-source-refresh.md), [catalog cache policy](catalog-cache-policy.md) | bounded schedule/retry/concurrency/retention; stale last-known-good evidence stays attributable and never appears current |
| SAR composition | [MCP ownership](mcp-ownership.md), [runtime compatibility matrix](runtime-compatibility-matrix.md) | one composed endpoint exposes only declared read-only CBD/SIE tools; runtime policy only narrows; source ownership remains separate |
| Runtime, ABI, and input compatibility | [runtime compatibility matrix](runtime-compatibility-matrix.md), [CAR ABI governance](car-abi-governance.md), [input compatibility governance](input-compatibility-governance.md) | assessed runtime evidence and CAR ABI are explicit; only named older inputs are accepted; incompatible input has no speculative fallback |
| Operational failure behavior | [source authentication](source-authentication.md), [information-source refresh](information-source-refresh.md), [input compatibility governance](input-compatibility-governance.md), [local information sources](local-information-sources.md), [SIE-mediated BoK](sie-bok-information-source.md) | failures remain source-owned, bounded, sanitized, and observable; retained evidence, query scope, and non-cached local behavior are never conflated |

## Audience Coverage

| Document surface | Authentication | Refresh/cache | SAR composition | Compatibility | Operational failures |
|---|---|---|---|---|---|
| `README.md` | `Source Authentication` | `Catalogs` | `Representative CBD and SIE SAR` | build/runtime/ABI/input compatibility paragraphs | source failure and authentication code summaries |
| `docs/user-guide.md` | `Authenticated Remote Sources` | `Normal Workflow` | `Representative SAR Check` | `CAR ABI Check` and `Input Compatibility Decisions` | authentication table and `Troubleshooting` |
| `src/main/car/manual/index.md` | `Configuration / Source Authentication` | `CbdCatalogAdmin Operation` | configuration representative-SAR paragraphs | runtime/ABI, `Catalog Contract`, and `Input Compatibility` | `Failure and Limitation Semantics` |
| strategy | Phase 4 current-priority summary | P4-10 through P4-13 summary | P4-20 through P4-22 summary | P4-30 through P4-32 summary | cross-cutting failure/ownership statements |
| phase ledger | scope, constraints, and P4-01 through P4-04 evidence | P4-10 through P4-13 evidence | P4-20 through P4-22 evidence | P4-30 through P4-32 evidence | evidence bullets and closure checklist |
| static specifications | source-authentication/security | information-source-refresh/cache | mcp-ownership/runtime matrix | runtime matrix/CAR ABI/input governance | the source-specific failure contracts linked above |

## Update Rule

Behavior changes first update their owning static specification and executable
specification. The README, user guide, reference manual, strategy, and phase
ledger then receive audience-appropriate projections in the same development
slice. P4-40 is complete only when all six document surfaces cover all five
areas without a contradictory command, field name, fallback, source-of-truth,
or failure-state statement.

## Executable Coverage

[`phase-4-executable-coverage.json`](phase-4-executable-coverage.json) is the
machine-readable P4-41 map from every behavioral Phase 4 checklist ID to exact
Scala scenario names or executable script-gate markers.
`Phase4ExecutableCoverageSpec` compares that map with the authoritative
checklist and verifies every declared path, evidence kind, executable bit, and
anchor. Behavior changes must update the owning scenario or gate and this map
in the same slice; renaming or removing mapped evidence therefore fails the
coverage suite instead of silently reducing Phase 4 verification.
