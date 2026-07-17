# Action-Local Runtime Isolation — Phase 7 Start (2026-07-18)

## Context

Phase 6 moved CBD Support's clock, configuration, local resource access, and
Cozy process execution behind CNCF runtime capabilities. Its component factory
can reuse a runtime for equivalent declared configuration. The post-phase
follow-up work also needs to supply each ActionCall's admitted local-source
inventory to the retrieval runtime.

## Decision

Define Phase 7 as **Action-Local Runtime Isolation**. A reusable runtime may
retain configuration-scoped remote cache state, but no ActionCall-local state.
Development, local-CAR, and cache-CAR snapshots admitted through an ActionCall
are invocation-local and must not be inserted into mutable shared runtime
state.

The acceptance evidence must include two distinct snapshot inventories in both
interleaved and concurrent calls, so a cache reuse implementation cannot pass
merely because the test executes one action at a time.

## Integration-Harness Consequence

The standalone and CBD/SIE SAR checks become part of this phase because they
are migrating the same configuration boundary: fixture resource roots and CBD
bindings are explicit component/runtime arguments rather than environment
variables inherited by the launched process. This is harness configuration
ownership, not a new production configuration surface.

## Boundaries Retained

- CBD Support owns the cache and ActionCall-local retrieval boundary.
- CNCF continues to own resource-tree admission and runtime capability APIs.
- Cozy and `sbt-cozy` are unaffected except for validation of the existing
  composed harnesses.
- CAR Review policy, provider contracts, publication, and the first released
  ABI baseline are out of scope.
- Phase 4 P4-45 remains ON_HOLD and is not a Phase 7 approval gate.

## Checkpoint Evidence

The reusable `CbdRuntime` now contains only shared remote catalog and BoK
state. `CbdRuntimeInvocation` owns the admitted local inventory and its bounded
observed projection. `ComponentFactory` reuses the shared runtime only for
equivalent declared configuration and clock identity, then creates one
invocation view per ActionCall.

On 2026-07-18, the following evidence passed:

- deterministic sequential and concurrent invocation isolation specifications;
- the full CBD Support suite: 227 succeeded, 0 failed;
- standalone SAR and the four-profile CBD/SIE SAR policy matrix;
- CAR ABI governance and normal CAR lint.

The normal lint retains only the known first-release ABI baseline warning
(`FUTURE-CBD-ABI-RELEASE-01`). The final review found no actionable Phase 7
findings, so `P7-23` is closed with the validated Phase 7 commit.
