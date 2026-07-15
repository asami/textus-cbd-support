# CAR ABI Governance

## Authority and Scope

The CAR ABI is the component contract recorded by
`cozy.car.abi-manifest.v1`; JVM class-file ABI is not the primary compatibility
surface. The current source-managed manifest is
`src/main/car/abi-manifest.json`. Its operation names, kinds, inputs, and
outputs must match the generated `cozy.cml.model-metadata.v1` surface from
`src/main/cozy/textus-cbd-support.cml`, and the built CAR must embed that
manifest unchanged.

The current `0.1.0-SNAPSHOT` manifest exports the `textus-cbd-support`
component and seven operations: the six read-only `CbdRetrieval` operations and
the private-to-MCP `refreshCatalog` administration operation. MCP readiness is
a runtime publication policy; it does not remove an operation from the CAR ABI.
The component currently exports no CML entities and declares no component ABI
dependencies.

## Baseline Lifecycle

There is no previously released Textus CBD Support CAR. Cozy therefore reports
`abi.baseline.missing` for the current first-release line, including in strict
mode. This is an explicit pending state, not evidence that compatibility was
checked against a historical release, and the repository does not invent a
released baseline.

After CAR `x.y.z` is actually released, preserve its exact released manifest at:

```text
src/main/car/x.y.z/abi-manifest.json
```

The unversioned manifest remains the current development surface. Automatic
Cozy baseline selection uses the highest lower released SemVer directory and
ignores SNAPSHOT, current, higher, and non-SemVer directories. Creating a
versioned baseline is therefore a release-recording action and must not happen
before the corresponding release exists.

## Compatibility Decisions

`scripts/check-car-abi.sh` builds the CAR, checks the CML/current/package
three-way equality, and runs Cozy's committed `lint abi` SemVer policy against
transition fixtures under `src/test/resources/abi`:

| transition | fixture result | decision |
|---|---|---|
| `0.1.0` to `0.2.0`, operation added | `OK abi.operation.added` | compatible minor addition |
| `0.1.0` to `0.2.0`, operation removed | `FAIL abi.operation.removed` | breaking change rejected |
| `0.1.0` to `1.0.0`, operation removed | `OK abi.operation.removed` | intentional major transition allowed |

The fixtures specify policy behavior; they are not released Textus CBD Support
baselines and are never candidates for automatic project baseline selection.
Patch versions must retain an unchanged ABI. Minor versions may add components,
operations, entities, and optional fields but may not remove or change existing
contracts. Major versions may intentionally accept a reported breaking change;
the finding remains visible as an `OK` decision rather than disappearing.

## Required Check

Run from the repository root:

```bash
scripts/check-car-abi.sh
```

Success requires `CAR_ABI_SURFACE_OK`, `CAR_ABI_PACKAGE_MATCH_OK`, all three
transition decisions, and `CAR_ABI_GOVERNANCE_OK`. A missing historical
baseline remains `CAR_ABI_CURRENT_BASELINE_PENDING` until the first release is
actually published and recorded.
