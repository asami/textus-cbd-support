# Phase 6 - CBD Runtime-Boundary Adoption

Stage Status:
- Current status: CLOSED
- Current step: Phase 6 complete
- Owner: Textus CBD Support development
- Update rule: Update this block and `phase-6-checklist.md` only after each
  item has reproducible evidence.

## Purpose

Phase 6 adopts the closed CNCF Phase 36 component runtime-boundary capabilities
in CBD Support. It removes CBD Support's ambient operational clock,
environment, host filesystem, and direct process access while preserving the
existing CBD-owned Review, MCP, HTTP, CLI, catalog, and provider contracts.

This is CBD Support implementation work. It does not add CAR Review rules or
move Cozy, sbt-cozy, or reviewed-CAR logic into CNCF.

## Upstream Contract

The source authority is CNCF commit `256eddf6` (Phase 36 closure) and its
normative contracts:

- `docs/design/component-runtime-boundary-capabilities.md`;
- `docs/spec/component-runtime-boundary-capabilities.md`;
- `docs/design/process-execution-runtime.md`; and
- `docs/spec/process-execution-runtime.md`.

CBD Support consumes the local `0.5.1-SNAPSHOT` development artifact rebuilt
from CNCF source revision `0f0f37e2`, which contains the selected Phase 36
surface. The rebuild was verified by its capability classes and by CBD Support
`Test/compile`; the pre-Phase-36 artifact is not an acceptable substitute. No
release publication is authorized by this phase.

## Migration Mapping

| CBD Support ambient dependency | CNCF capability | CBD-owned result |
|---|---|---|
| `Clock.systemUTC()` | bound `ExecutionContext` clock | deterministic source/review timestamps |
| `sys.env` configuration | declared `ComponentConfigurationKey` | typed public values and opaque secret references |
| registered development/CAR directories | `ResourceTreeAccess` snapshot | bounded logical source inventory |
| `ProcessBuilder` Cozy transport | admitted `ProcessExec` + WorkArea tree/artifact | CBD provider-result mapping |

## Scope

1. Freeze the CNCF capability version and add a source-to-contract mapping.
2. Replace configuration and clock construction with injected runtime values.
3. Replace local source traversal with admitted read-only tree snapshots.
4. Replace direct Cozy process launch with a registered Process Execution
   capability and a CBD-owned neutral-result adapter.
5. Add deterministic fake-runtime specifications and prove normal CAR lint,
   ABI, standalone SAR, and Review workflows.

## Declared Local-Source Contract

The component accepts logical resource-tree names only:

- `textus.cbd.development.trees`: comma-separated
  `<source-id>=<tree-name>` development-tree bindings;
- `textus.cbd.local-car.tree`: the named local CAR storage tree; and
- `textus.cbd.cache-car.tree`: the named cached CAR storage tree.

The host filesystem mapping belongs to the CNCF runtime's
`textus.resource.tree.file-roots` policy. CBD Support resolves each declared
name through the bound `ExecutionContext.resourceTrees`; unavailable or
invalid trees become source diagnostics before source/provider work begins.
The previous CBD-owned path and home-root configuration keys are not accepted.

> **Jul. 20, 2026 correction:** development-directory inspection no longer
> uses a complete `ResourceTreeAccess.snapshot`. It uses CNCF's bounded exact
> leaf-name `ResourceTreeQuery` for `project.yaml`, so a declared workspace can
> report every admitted project descriptor without materializing its complete
> tree. Local and cache CAR storage continue to use the strict snapshot
> capability described above.

## Cozy Review Process Contract

CBD Support identifies the provider invocation only as the logical
`cozy-car-review` capability. A trusted CNCF runtime must install its
program definition, finite limits, fixed arguments and environment policy,
optional admitted resource-tree grants, and Process Execution grant. The CBD
runner submits only the canonical provider-request bytes and maps terminal
states to the existing provider protocol: success stdout is the evidence
bundle; timeout, cancellation, launch, non-zero exit, and output/artifact
limit states map to the corresponding CBD failure codes. CBD does not own an
executable path, child environment, process handle, or output directory.

## Boundaries

- No direct `sys.env`, `System.getenv`, `Clock.system*`, host `Path` API,
  `Files` traversal, `ProcessBuilder`, process handle, executable location, or
  shell command text in CBD component/provider behavior.
- Existing canonical reports, gates, Review Run lifecycle, and provider wire
  contracts remain unchanged.
- Source authentication remains reference-only; credential values do not enter
  CBD Support runtime objects, logs, reports, MCP, or CallTree.
- CNCF remains provider-neutral. A newly discovered gap is recorded in CNCF
  separately rather than hidden behind a CBD-specific bypass.
- CAR publication, first released ABI baseline, deployment, remote/container
  execution, and production secret providers are out of scope.

## Completion Conditions

Phase 6 closes only when every P6 checklist item is complete, targeted ambient
CAR lint warnings are absent, and all remaining warnings have an explicitly
independent relocation target.
