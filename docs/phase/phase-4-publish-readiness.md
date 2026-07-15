# Phase 4 Publish-Readiness Evidence

## Assessment

- Assessed: 2026-07-15
- Project: `/Users/asami/src/dev2026/textus-cbd-support`
- Current coordinate: `org.textus:textus-cbd-support:0.1.0-SNAPSHOT`
- Publication mode assessed: public release
- Result: **not publish-ready**
- Publication performed: **no**

P4-43 records the current state without treating a successful development build
as permission to publish. Phase 4 can close with an explicit not-ready result;
an actual release remains a separate developer-authorized workflow.

## Artifact Evidence

The P4-42 build produced
`target/textus-cbd-support-0.1.0-SNAPSHOT.car` with this assessment-time
metadata:

| field | value |
|---|---|
| size | `4,591,910` bytes |
| SHA-256 | `14609facf1d7ea8107f99353eede143d6bd12d9c9594578062107b610fb1c1c1` |
| descriptor | `textus-cbd-support:0.1.0-SNAPSHOT` |
| component | `textus-cbd-support` |
| ABI exports | 1 component, 7 operations, 0 entities |
| ABI dependencies | none |

The CAR contains `component-descriptor.json`, `abi-manifest.json`,
`component/main.jar`, the reference manual, and static Web descriptors. The
source-managed ABI manifest and packaged manifest were already checked for
structural JSON equality by `scripts/check-car-abi.sh`. The checksum identifies
this assessment artifact only; rebuilding a ZIP-based CAR may produce a new
checksum and requires refreshing this evidence before publication.

## Dependency and Runtime State

`project.yaml` is the authority for the component and runtime declarations:

| role | current value | release decision |
|---|---|---|
| component version | `0.1.0-SNAPSHOT` | change to a non-SNAPSHOT release version |
| Scala | `3.3.8` | recorded build input |
| compile dependency | `org.goldenport::goldenport-cncf:0.5.1-SNAPSHOT` | publish CNCF first, then use its release coordinate |
| CNCF minimum | `0.5.1-SNAPSHOT` | replace and reassess with the selected release runtime |
| CNCF tested | `0.5.1-SNAPSHOT` | replace only after representative SAR evidence passes |
| CNCF excluded | none | explicit empty assessment set |
| sbt-cozy plugin | `0.1.14` | latest published version reported by CAR lint |

The representative SAR passed against the resolved `0.5.1-SNAPSHOT`
coordinate, but that mutable coordinate is not immutable release evidence. The
local CNCF source is at revision
`848ef5596af6927512af4e9c8c0d423d4add1253` with unrelated uncommitted work, so
`cncf-lib-publish-readiness` cannot report that dependency as ready. The public
publish order is therefore:

1. Finish and validate the CNCF repository, publish a non-SNAPSHOT CNCF
   release, and record its immutable coordinate.
2. Update CBD Support's compile and runtime declarations and rerun the runtime
   compatibility matrix against that release.
3. Publish CBD Support only after a repeated readiness assessment passes.

## ABI and Residual Warnings

Strict CAR lint reported no failures and retained
`WARN abi.baseline.missing`. No Textus CBD Support CAR has been released, so a
historical baseline must not be fabricated. After `0.1.0` is actually
published, preserve the exact released manifest at
`src/main/car/0.1.0/abi-manifest.json`; only then may later development use it
as a released baseline.

The remaining publication blockers are:

- the target CAR and CNCF dependency/runtime declarations are SNAPSHOTs;
- the CNCF prerequisite worktree is dirty and has no completed release
  readiness result;
- `publishTo` currently resolves to `None`, so the public destination and
  credentials have not been selected;
- the CBD Support worktree must be clean and contain only the intended release
  state before publication.

The absent historical ABI baseline is not an independent blocker for the first
release. It remains an expected warning until that release succeeds, after
which the exact released manifest becomes the baseline for later versions.

The P4-42 quality gates remain green: CBD passed 116 tests in 16 suites, SIE
passed 81 tests in 11 suites, both projects passed CML and normal CAR lint, both
CARs built, and the four-profile source-aware SAR matrix passed.

## Manual Public Publication Procedure

Do not execute these steps without a separate explicit publication request.

1. Run `cncf-lib-publish-readiness` for CNCF from a clean intended worktree.
   Publish the CNCF release first with `cncf-lib-publish` only when that check
   reports ready.
2. Update `project.yaml` to the chosen CBD and CNCF release versions. Update
   `docs/spec/runtime-compatibility-matrix.json` and its companion document,
   `src/main/car/abi-manifest.json`, and version-specific script or document
   references that still name the SNAPSHOT artifact.
3. Configure and verify the public `publishTo` destination and credentials. Do
   not add ad hoc local resolver wiring and do not use `publishLocal` for a
   public release.
4. Rerun `cncf-car-publish-readiness`. It must include strict CAR lint, the full
   test suite, `git diff --check`, CAR/ABI checks, and the representative SAR
   against the selected released CNCF coordinate.
5. After dependencies are already published, invoke `cncf-car-publish` or run
   the following manual command under the explicit release request:

   ```sh
   sbt --batch publish
   ```

6. Inspect Maven and CAR publication output, repository metadata, and the
   absence of accidental local Ivy publication. If publication writes to the
   managed Maven repository workspace, run its `update.sh` and commit that
   repository separately.
7. Commit the target release version and publication records. Record the exact
   released ABI manifest under `src/main/car/0.1.0/` only after publication
   succeeds. CDN invalidation is a separate explicit follow-up.

Because the CNCF prerequisite is not published, neither `cncf-car-publish` nor
`cncf-car-publish-recursive` is ready to run from this assessment. Reassess the
dependency first; this document authorizes no publication.
