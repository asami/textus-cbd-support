# Phase 7 - Action-Local Runtime Isolation Checklist

This checklist is the authoritative Phase 7 state ledger. The scope and stage
summaries are in `phase-7.md`.

## P7-01: Cache and Invocation Contract

- [x] Define the configuration-scoped shared runtime state, its immutable
  cache key, and the ActionCall-local inventory/view boundary without adding
  a CBD-specific CNCF contract.

## P7-02: ActionCall Isolation Invariant

- [x] Specify that one ActionCall cannot observe a later ActionCall's admitted
  development, local-CAR, or cache-CAR snapshot.

## P7-03: Cache Reuse Rules

- [x] Record the cache invalidation/reuse rules for declared configuration,
  execution-time capability, and invocation-local inventory.

## P7-10: Runtime Isolation

- [x] Remove shared mutable admitted-local-inventory state from the reusable
  `CbdRuntime` path, or replace it with an equivalently immutable
  ActionCall-local projection.

## P7-11: Retrieval Isolation

- [x] Ensure local source state and source-aware search paths consume only the
  current ActionCall's admitted inventory and retain existing attributable
  diagnostics.

## P7-12: Isolation Specifications

- [x] Add deterministic sequential-interleaving and concurrent ActionCall
  executable specifications using distinct in-memory resource-tree snapshots;
  each result must retain only its own source evidence and contents.

## P7-13: Explicit Integration Configuration

- [x] Migrate `scripts/check-cbd-standalone.sh` to explicit component/runtime
  configuration arguments, including declared resource-tree roots and CBD
  source bindings, with no hidden CBD configuration environment variables.
- [x] Migrate `scripts/check-cbd-sie-sar.sh` equivalently while preserving the
  composed CBD/SIE ownership and its bounded fixture behavior.
- [x] Prove the two scripts reject or diagnose missing declared trees/configured
  sources through normal component configuration handling rather than ambient
  process state.

## P7-20: Verification and Closure

- [x] Focused runtime-isolation and configuration specifications pass, followed
  by the full CBD Support test suite.

## P7-21: Integration and Compatibility Gates

- [x] `scripts/check-cbd-standalone.sh`, `scripts/check-cbd-sie-sar.sh`,
  `scripts/check-car-abi.sh`, and normal CAR lint pass; any first-release ABI
  baseline warning remains explicitly attributed to
  `FUTURE-CBD-ABI-RELEASE-01`.

## P7-22: Documentation Record

- [x] Strategy, phase ledger, and journal record the final boundary and any
  residuals without changing P4-45's ON_HOLD state.

## P7-23: Final Review and Closure

- [x] Final review has no actionable Phase 7 findings, validated changes are
  committed, and the phase is closed only after every required item above is
  checked.
