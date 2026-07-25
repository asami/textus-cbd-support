# CAR Review Initial Static Quality Provider Contract v1

status=stable
phase=Phase 8 P8-51
updated_at=2026-07-24

## Purpose

The initial static-quality provider converts bounded results from Cozy or an
equivalent static analyzer into canonical CAR Review Evidence and Observations.
It is a fixed CBD contract, not a generic assertion endpoint.

## Fixed checks

| Area | Capability | Static check |
| --- | --- | --- |
| Security | `quality.security.authorization` | authorization policy |
| Security/Domain | `quality.security.domain.bounded-text-datatype` | bounded text datatypes |
| Domain | `quality.domain.identity-consistency` | model/artifact identity consistency |
| Documentation | `quality.documentation.rationale` | public rationale |
| Resilience | `quality.resilience` | failure/resilience contract |
| Testability | `quality.testability` | executable-test contract |
| Evaluability | `quality.evaluability.corpus-first-experiment` | versioned evaluation corpus |
| Observability | `quality.observability.structured-logging` | structured logging schema |
| UX | `quality.ux.web`, `quality.ux.cli`, `quality.ux.skill-assisted`, `quality.ux.cross-surface-consistency` | surface contracts |

Every input is `pass`, `fail`, or absent. Pass creates an Assurance; fail
creates a medium Finding; absent evidence creates a retryable Unknown and an
`evidence-unavailable` limitation. This does not claim operational maturity:
runtime Evidence remains separately required.

## Input and redaction

The provider accepts one SHA-256 source identity and booleans for the fixed
checks. It refuses an invalid source digest. Canonical Evidence retains only
the source digest, fixed check ID, capability ID, and outcome—never source
content, absolute paths, credentials, environment data, or analyzer payloads.
