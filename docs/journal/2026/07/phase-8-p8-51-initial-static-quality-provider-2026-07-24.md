# Phase 8 P8-51: Initial static quality provider

status=completed
phase=Phase 8 P8-51
updated_at=2026-07-24

## Decision

CBD Support receives static analysis as a bounded, digest-bound provider input,
then applies a fixed quality-rule set. It does not accept arbitrary capability
or rule identifiers from an analyzer result; that would turn a provider payload
into unreviewed policy.

## Result

The first rules cover Security, Domain, Documentation, Resilience, Testability,
Evaluability, Observability, and UX. Each field is independently pass, fail, or
absent. Pass and fail become canonical attributed Assurance/Finding. Absent
evidence becomes an explicit retryable Unknown, not a pass. The input source
digest is mandatory and invalid digest input fails the provider before any
Evidence is created.

Static checks establish only static structure and contract presence. They do
not establish runtime operation or operational maturity; runtime evidence keeps
its separate authority boundary.

## Evidence

`CarReviewInitialStaticQualityProviderRunnerSpec` proves fixed mappings across
all initial areas, pass/fail/Unknown behavior, retained source digest, and
invalid-source refusal.
