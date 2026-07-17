# Phase 7 - Action-Local Runtime Isolation

Stage Status:
- Current status: DONE
- Current step: Phase 7 closed.
- Owner: Textus CBD Support development
- Update rule: Update this block only after reproducible evidence is recorded
  in `phase-7-checklist.md`; phase closure is based solely on that ledger.

## Purpose

Make the CBD Support component runtime safe to reuse across ActionCalls without
allowing one action's admitted local-source inventory to affect another action.
Phase 6 correctly moved host access behind CNCF runtime capabilities, but the
post-Phase-6 component cache must also preserve the lifetime and isolation of
each ActionCall's `ResourceTreeAccess` snapshot.

This is CBD Support implementation and integration-harness work. It is not a
CAR Review policy change, a new Review provider, or a CNCF runtime API change.

## Baseline and Problem Statement

`ComponentFactory` may reuse a `CbdRuntime` for equivalent declared
configuration and clock values. An ActionCall separately admits its development,
local-CAR, and cache-CAR snapshots. The runtime cache must not retain or mutate
that action-local inventory after it has served an action.

The current follow-up work introduces a cached runtime update path for admitted
local inventory. Before that work is accepted, Phase 7 requires an explicit
ownership model: a later ActionCall must not replace the inventory observed by
an earlier ActionCall, whether calls are sequentially interleaved or concurrent.

## Scope

1. Define an immutable runtime-cache key and ActionCall-local local-source
   inventory/view boundary.
2. Change runtime construction and retrieval paths so cached state contains no
   mutable ActionCall-local resource-tree inventory.
3. Prove two independently admitted resource-tree snapshots cannot contaminate
   each other's source state or source-aware search results.
4. Complete the standalone and CBD/SIE SAR harness migration from ambient
   configuration environment variables to explicit CNCF component configuration
   arguments, including declared resource-tree roots.
5. Verify the resulting boundary with deterministic specifications, the two
   integration scripts, full CBD tests, CAR lint, ABI governance, and a final
   review.

## Non-Goals

- Changing source precedence, version-selection, catalog, BoK, SIE, or Review
  semantics.
- Adding a shared mutable local-source cache, background local-tree refresh, or
  server-side arbitrary filesystem access.
- Moving CBD Support's invocation-local policy into CNCF or adding CBD- or
  Cozy-specific types to CNCF.
- Changing Cozy provider execution, `sbt-cozy` Review gates, publication, or
  the first released CAR ABI-baseline decision.

## Required Invariants

- A cache entry is reusable only for immutable, declared runtime configuration
  and the injected execution-time capability identity required by that state.
- Every admitted local inventory is owned by one ActionCall and is not stored in
  shared mutable runtime state.
- An ActionCall's local source evidence, diagnostics, and result projection use
  only that call's admitted snapshots.
- A missing or rejected resource tree produces attributable diagnostics for the
  affected ActionCall and cannot alter a previously admitted action.
- Component configuration for runtime integration is explicit command/config
  input; the shell environment is not a hidden configuration channel.

## Stages

### Stage 7.1: Contract and Cache Boundary

Stage Status:
- Current status: DONE
- Owner: Textus CBD Support development
- Checklist basis: `P7-01` through `P7-03`
- Update rule: Update after cache ownership, local-inventory lifetime, or their
  executable specifications change.

Establish the cache-key and ActionCall-local inventory contract, then remove
the mutable cross-invocation update path.

### Stage 7.2: Runtime and Integration-Harness Evidence

Stage Status:
- Current status: DONE
- Owner: Textus CBD Support development
- Checklist basis: `P7-10` through `P7-13`
- Update rule: Update after runtime behavior, configuration handoff, or
  integration evidence changes.

Implement the isolated invocation view and complete the explicit-configuration
harnesses for standalone and CBD/SIE SAR execution.

### Stage 7.3: Verification and Closure

Stage Status:
- Current status: DONE
- Owner: Textus CBD Support development
- Checklist basis: `P7-20` through `P7-23`
- Update rule: Update only after the recorded command or review evidence is
  reproducible.

Run focused and full verification, document the boundary, review the complete
slice, and commit only validated work.

## Human Confirmation

Phase 7 has no new human-confirmation stage: its closure is based on the
checklist's executable and integration evidence. Phase 4's P4-45 remains an
independent ON_HOLD human-confirmation item; Phase 7 neither satisfies nor
changes it.
