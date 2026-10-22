# Ineffective Prevention and Pseudo Guarantee Review

Status: exploratory review rule design
Date: 2026-10-22
Provider input: Cozy CAR lint and other attributable review evidence

## Purpose

Extend the existing speculative-defensive-machinery and AI-generated business
application lint work with a general semantic test for prevention mechanisms
that add complexity without establishing a useful guarantee or reducing the
failure responsibilities that remain in the application.

This is intentionally not a Codex-specific prohibition and not a Skill rule.

## Review concepts

### Ineffective Prevention

A mechanism is suspicious when it is introduced to prevent or reduce a failure,
but after adding it:

- the same underlying failure can still occur;
- the same failure-handling path remains mandatory; and
- no concrete requirement or measured benefit justifies the additional
  implementation, runtime, and maintenance cost.

The key review question is:

> What failure handling or responsibility becomes unnecessary because this
> mechanism exists?

"No responsibility is removed" is not automatically an error, but it is a
strong bad-smell signal when the mechanism is non-trivial and its benefit is
only a small probability reduction.

### Pseudo Guarantee

A stronger finding applies when code presents or behaves like a guarantee while
the layer does not control the authoritative resource or concurrency boundary
needed to establish that guarantee.

Examples include local reservation ledgers that do not reserve the real
resource, check-then-act logic over concurrently mutable external state, or
private consistency protocols that cannot exclude changes by the actual
authority.

## Review frame

Evaluate suspicious prevention machinery with:

- **Cost**: code, persistent state, scans, synchronization, runtime I/O,
  recovery, maintenance, and AI/review amplification.
- **Benefit**: concrete failure avoided, work avoided, latency saved, or other
  measurable effect.
- **Guarantee**: property actually established, including the authority that
  makes it true.
- **Residual Responsibility**: failure handling, cleanup, retry, reconciliation,
  or recovery that remains mandatory after the mechanism is added.
- **Simpler Baseline**: behavior if the mechanism is removed and the underlying
  failure is handled normally.

A useful review comparison is:

```text
with mechanism    = prevention machinery + residual failure handling
without mechanism = normal operation + residual failure handling
```

When the residual responsibilities are effectively identical, the mechanism
needs a concrete requirement and proportional benefit to survive review.

## Motivating case: filesystem capacity reservation

Observed implementation:

- persistent reservation files;
- repeated scanning/accounting in ordinary cache I/O;
- capacity reservation bookkeeping for expansion/staging;
- no physical filesystem capacity reservation.

Result:

- disk-full remained possible because other processes and actual expansion
  could consume capacity;
- IOException handling remained mandatory;
- partial generated files still required cleanup;
- management cost was added without removing the original failure path.

Preferred baseline for the observed requirement is ordinary I/O, normal
disk-full failure propagation, and cleanup of incomplete output. A preflight
capacity check is justified only by an explicit requirement such as avoiding a
known expensive operation when insufficiency can be usefully detected early;
it must not be represented as a guarantee.

## Relationship to existing CBD Review work

This rule family specializes the existing
`speculative defensive machinery` and `model-unjustified state machinery`
candidates recorded in
`docs/journal/2026/10/2026-10-03-ai-generated-business-application-lint-candidates.md`.

It also complements the CAR Review principle that significant mechanisms should
be traceable to a model, use case, failure model, non-functional requirement,
or recognized infrastructure boundary.

Cozy may emit deterministic structural signals such as CHECK_THEN_IO,
CUSTOM_RESOURCE_RESERVATION, EXTERNAL_STATE_RESCAN, or
MANAGEMENT_STATE_IN_HOT_PATH. CBD Support owns the semantic conclusion and must
retain provider identity/evidence.

## Review outcome

Suggested finding categories:

- **IneffectivePrevention**: non-trivial prevention cost with little/no reduction
  in residual failure responsibility.
- **PseudoGuarantee**: claimed/implied guarantee cannot be established by the
  mechanism's authority boundary.
- **UnjustifiedPreventionCost**: some benefit exists, but no requirement or
  evidence supports paying the complexity/runtime cost.

These should initially be review findings, not universal blocking errors.
Human-in-the-loop disposition remains available for explicit infrastructure or
operational requirements.

## AI usage

AI semantic review may be used after deterministic signals narrow the scope.
Do not add incident-specific rules to Skills and do not ask the implementation
model to execute a growing checklist of anti-pattern prohibitions.

A bounded review prompt should ask for:

1. the failure the mechanism attempts to prevent;
2. the property it actually guarantees and the authority establishing it;
3. failure handling that becomes unnecessary;
4. residual failure handling that remains;
5. concrete requirement/evidence for the remaining benefit.

The review should recommend simplification when no useful guarantee,
responsibility reduction, or proportional requirement is found.
