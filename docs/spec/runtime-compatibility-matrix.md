# Runtime Compatibility Matrix

## Authority

`project.yaml` is the declaration authority for the component's CNCF minimum,
tested, and excluded versions. The adjacent
`runtime-compatibility-matrix.json` is the machine-readable assessment record:
it must repeat those declarations exactly and attach every compatible candidate
to representative execution evidence. Neither file may infer support for an
unlisted runtime.

`scripts/check-runtime-compatibility.py` compares both records before a live
candidate can run. `scripts/test/check-cbd-sie-sar.sh` invokes that check for its
selected `CNCF_VERSION` and emits `RUNTIME_COMPATIBILITY_EXECUTION_OK` only
after the complete composed CBD/SIE source-aware and disable-policy matrix
passes.

## Current Matrix

| CNCF version | minimum | tested | excluded | classification | evidence |
|---|---:|---:|---:|---|---|
| `0.5.3-SNAPSHOT` | yes | yes | no | tested-compatible current declaration; P9-70 representative execution passed and is accepted Phase evidence | representative CBD/SIE SAR (P9-70) |

The declared excluded set is empty. This is an explicit statement that no
version currently has sufficient project-owned evidence for an exclusion; it
is not a claim that every other version is compatible. Versions absent from the
table are unassessed and must not be reported as supported or incompatible.

`tested-compatible` is the current `project.yaml` declaration required by the
runtime compatibility checker: CNCF `0.5.3-SNAPSHOT`. P8-61 driver-CAR
acceptance for Cozy `0.3.4-SNAPSHOT` with CNCF `0.5.2-SNAPSHOT` is historical;
the JSON `p8_61` record points to its canonical handoff. The current
`representative-sar` execution for `0.5.3-SNAPSHOT` passed in P9-70 and is
recorded as `execution-passed-accepted`; it is accepted Phase evidence and must
not be inferred from P8-61 acceptance. This accepted evidence does not itself
close P9-73 or Phase 9. The declaration is not a promise that later artifacts
published under the mutable SNAPSHOT coordinate are identical. A release
decision must record immutable dependency and artifact evidence under P4-43.

## Representative Evidence

Representative Phase evidence is a two-part gate. A release-gate caller or
orchestrator normally runs `publishLocal` for Scraper, SIE, BoK, and CBD
Support in dependency order; automation serializes each top-level SBT
invocation.

Then `scripts/test/check-cbd-sie-sar.sh` resolves those CARs from the local CAR
repository, validates them, assembles descriptor-only SARs, and runs the live
matrix without invoking SBT.

Run from the repository root:

```bash
CNCF_VERSION=0.5.3-SNAPSHOT \
CNCF_RUNTIME_DEV_DIR=/path/to/cloud-native-component-framework \
  scripts/test/check-cbd-sie-sar.sh
```

`CNCF_RUNTIME_DEV_DIR` may be omitted when the runtime is available through the normal project environment.

The selected candidate must be `tested-compatible` before any server work begins. A successful run must emit all markers listed for `representative-sar` in the JSON matrix. The final marker includes the selected version and whether the runtime came from a coordinate or a local development directory; a development-directory run also records its Git revision and clean/dirty state.

The P9-70 execution instead used an isolated local CAR root containing a fresh
CBD Support CAR and exact retained SIE, BoK, and Scraper CAR inputs. It did not
mutate the shared local warehouse. The chronological result is recorded in
[`2026-09-11-phase-9-p9-70-validation-and-continuation-accounting.md`](../journal/2026/09/2026-09-11-phase-9-p9-70-validation-and-continuation-accounting.md).

The representative evidence covers:

- consuming dependency-ordered local publications of the CBD Support, Scraper,
  SIE, and BoK CARs with the selected candidate;
- live baseline retrieval with separate catalog, development, and SIE-owned
  evidence plus bounded failure behavior; and
- live global, service, and operation disable-policy profiles.

It does not establish ABI compatibility with another CAR version; that is the
separate P4-31 gate.

The broader CBD/SIE SAR command was freshly run for P9-70 against the current
candidate and passed as accepted Phase evidence. It does not itself close
P9-73 or Phase 9; P9-73 remains the final review and closure gate.

## P8-61 Accepted

P8-61 is accepted historically for Cozy `0.3.4-SNAPSHOT` and CNCF
`0.5.2-SNAPSHOT`. Forced generation invocation
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
