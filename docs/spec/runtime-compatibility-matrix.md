# Runtime Compatibility Matrix

## Authority

`project.yaml` is the declaration authority for the component's CNCF minimum,
tested, and excluded versions. The adjacent
`runtime-compatibility-matrix.json` is the machine-readable assessment record:
it must repeat those declarations exactly and attach every compatible candidate
to representative execution evidence. Neither file may infer support for an
unlisted runtime.

`scripts/check-runtime-compatibility.py` compares both records before a live
candidate can run. `scripts/check-cbd-sie-sar.sh` invokes that check for its
selected `CNCF_VERSION` and emits `RUNTIME_COMPATIBILITY_EXECUTION_OK` only
after the complete composed CBD/SIE source-aware and disable-policy matrix
passes.

## Current Matrix

| CNCF version | minimum | tested | excluded | classification | evidence |
|---|---:|---:|---:|---|---|
| `0.5.2-SNAPSHOT` | yes | yes | no | tested-compatible declaration; P8-61 accepted; representative Phase release validation pending | representative CBD/SIE SAR (Phase release gate) and P8-61 driver-CAR acceptance |

The declared excluded set is empty. This is an explicit statement that no
version currently has sufficient project-owned evidence for an exclusion; it
is not a claim that every other version is compatible. Versions absent from the
table are unassessed and must not be reported as supported or incompatible.

`tested-compatible` is the current `project.yaml` declaration required by the
runtime compatibility checker. P8-61 driver-CAR acceptance for Cozy
`0.3.4-SNAPSHOT` with CNCF `0.5.2-SNAPSHOT` is accepted; the JSON `p8_61`
record points to its canonical handoff. The separate `representative-sar`
evidence remains `pending-phase-release-validation` and must not be inferred
from P8-61 acceptance. The declaration is not a promise that later artifacts
published under the mutable SNAPSHOT coordinate are identical. A release
decision must record immutable dependency and artifact evidence under P4-43.

## Representative Evidence

Run from the repository root:

```bash
CNCF_RUNTIME_DEV_DIR=/path/to/cloud-native-component-framework \
  scripts/check-cbd-sie-sar.sh
```

Omit `CNCF_RUNTIME_DEV_DIR` to exercise the resolved runtime artifact instead.
The selected version must occur as a non-excluded `tested-compatible` candidate
before any build or server work begins. A successful run must emit all markers
listed for `representative-sar` in the JSON matrix. The final marker includes
the selected version and whether the runtime came from a coordinate or a local
development directory; a development-directory run also records its Git
revision and clean/dirty state.

The representative evidence covers:

- building the CBD Support and SIE CARs with the selected CNCF candidate;
- live baseline retrieval with separate catalog, development, and SIE-owned
  evidence plus bounded failure behavior; and
- live global, service, and operation disable-policy profiles.

It does not establish ABI compatibility with another CAR version; that is the
separate P4-31 gate.

The broader CBD/SIE SAR command has not been freshly run for the final Phase
tree. It remains required evidence at the Phase release gate and is therefore
not claimed as passed by this matrix.

## P8-61 Accepted

P8-61 is accepted for Cozy `0.3.4-SNAPSHOT` and CNCF `0.5.2-SNAPSHOT`,
matching `project.yaml`. Forced generation invocation
`7556-20260814T213837Z` accepted the declared toolchain. Final persistence
invocation `9751-20260814T214332Z` supplied `Test/compile` and passed
`ReviewDiagnosisPersistenceSpec` as 1 suite/8 tests. Findings
`P8-61-VF-001` and `P8-61A-RV-001` are closed, and the focused re-review was
clean. The historical Cozy `0.3.0-SNAPSHOT` / CNCF `0.5.1-SNAPSHOT`
launcher-selection failure remains recorded in the P8-61 handoff as historical
context, not as current compatibility evidence.

P8-60 human confirmation completed on 2026-08-15. P8-61 runtime acceptance
does not prove or replace final-tree representative SAR or full Phase release
validation, which remain pending for the release gate.

## Update Rule

Any change to `packaging.car.runtime.cncf` or the compile-time
`goldenport-cncf` coordinate must update the JSON matrix in the same change.
The minimum must also occur in `tested` and be classified
`tested-compatible`; every tested candidate needs named representative
evidence. Every excluded candidate needs a non-empty reason and must not be
executed by the representative script. An unlisted upgrade candidate remains
unassessed until an explicit matrix change and successful representative run
provide evidence.
