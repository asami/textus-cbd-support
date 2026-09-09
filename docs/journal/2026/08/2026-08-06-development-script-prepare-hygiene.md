# Development Script Prepare Separation Hygiene

Status: Open hygiene

## Finding

`scripts/run-server.sh` consumes a cached classpath, but the maintained
`scripts/check-cbd-standalone.sh` and `scripts/check-car-abi.sh` checks perform
CAR builds through top-level SBT. `scripts/test/check-cbd-sie-sar.sh` is already
SBT-free but depends on locally published CARs. The workflow has neither one
explicit prepare owner nor digest-backed freshness evidence equivalent to
ArtScene.

## Required direction

- Separate CAR/dependency/runtime preparation into a bounded entry routed
  through the shared serialized SBT runner.
- Make runtime, HTTP, ABI, and SAR check entry points consume prepared evidence
  without invoking top-level SBT.
- Record artifact and relevant input digests; reject missing or stale state
  with a prepare-required diagnostic.
- Preserve a fast compile-and-restart repair loop when preparation is current.

## Completion evidence

Prepare succeeds under serialization; maintained runtime/check scripts are
statically SBT-free; current evidence passes; missing or stale evidence is
rejected without implicit build or publish work.
