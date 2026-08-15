# Phase 8 hygiene follow-up journal

**Date:** 2026-08-15
**Phase:** Phase 8 / P8-HYG hygiene persistence / P8-HYG-A canonical journal

## Scope

This non-normative journal records separately scoped maintenance only. It does not weaken Phase 8 validation and does not change product, design, or release requirements.

## HYG-P8-001 — workspace/generated-artifact hygiene

- **Status:** OPEN
- **Discovery date/state:** 2026-08-15; observed while freezing Phase 8 Step/release worktrees.
- **Repository:** `textus-cbd-support`
- **Locations:**
  - `tmp/pdfs/car-review-delivery-1.png`
  - `tmp/pdfs/car-review-delivery-2.png`
  - `tmp/pdfs/car-review-delivery.png`
  - `tmp/pdfs/car-review-tables.png`
- **Evidence/reproduction:** All four were present as untracked files in `git status --porcelain`; they were explicitly preserved and never staged in the P8-61 or P8-60 Step commits.
- **Category:** workspace/generated-artifact hygiene
- **Risk/priority:** Low; accidental staging, repository noise, and ambiguity over whether local rendered evidence is a deliverable.
- **Why outside Phase 8:** Files under `tmp/` are not source, committed review artifacts, runtime input, or required validation evidence; deleting or ignoring user-owned outputs is not part of the frozen Review delivery scope.
- **Proposed boundary:** A separate workspace-output hygiene task decides retention, archival, or ignore policy. Do not delete them in this journal or Phase.

## HYG-P8-002 — external validation-tool runtime-selection compatibility

- **Status:** OPEN
- **Discovery date/state:** 2026-08-15; observed during Phase 8 CAR review/lint evidence collection.
- **Affected repository:** `textus-cbd-support`
- **External tooling location:** `cncf-car-lint/scripts/cncf_car_lint.py` in the external Codex skill package.
- **Affected project evidence location:** `src/main/car/component-descriptor.json` with supported numeric `schemaVersion: 3`.
- **Evidence/reproduction boundary:**
  - The external Python wrapper invokes an unqualified PATH-selected `cozy lint car` and can select a runtime that misclassifies the numeric schemaVersion 3 descriptor as a blocking finding.
  - The project-declared Cozy `0.3.4-SNAPSHOT` path, invoked explicitly as `cozy --runtime 0.3.4-SNAPSHOT lint car . --format json`, completed successfully during Phase 8 evidence collection, with only documented development/first-release readiness warnings.
  - Therefore this item concerns external wrapper runtime selection/compatibility, not a request to weaken or suppress valid CAR lint findings.
- **Historical evidence for CPB-P8-REL-001:**
  - A locally published `org.simplemodeling:cozy_2.12:0.3.4-SNAPSHOT` artifact dated 2026-08-14 14:41 contained the pre-fix behavior treating `project.component.name` as `qualifiedId`.
  - Clean committed Cozy fix `9db5afb45489559936b7a94d70136db3590cf453` at 2026-08-14 21:16 established `component.name` as the local ID; current Cozy HEAD was clean at `69c322f5d1612682993135cd05ddf43b6decc83d`.
  - Serialized `publishLocal` invocation `6797-20260815T114236Z` succeeded with `sbt_exit=0`, `wrapper_exit=0`, and the lock released.
  - Exact `cozy --runtime 0.3.4-SNAPSHOT lint car . --format json` then exited 0 with `CAR_COMPONENT_IDENTITY_CANONICAL` and only the existing ABI-baseline/sbt-cozy warnings.
  - CPB-P8-REL-001 was a mutable local SNAPSHOT publication-skew signal and is cleared without changing cbd-support identity.
- **Category:** external validation-tool runtime-selection compatibility
- **Risk/priority:** Medium; a false blocking signal can prevent an otherwise valid Phase release, while blindly ignoring it could hide real findings.
- **Why outside Phase 8:** Correct repair belongs to the external `cncf-car-lint` skill/tooling package; the CBD project already declares the accepted Cozy runtime and descriptor contract.
- **Proposed boundary:** A separate tooling hygiene task makes the wrapper select the project-declared Cozy runtime (or pass it explicitly), adds a schemaVersion 3 fixture/regression, and preserves all genuine FAIL/WARN behavior.
- **Phase disposition:** Non-blocking only when exact project-declared Cozy lint and the other final Phase gates produce trustworthy evidence; final-gate validation must still run and classify its actual output.

## Ledger summary

- Two OPEN items; zero RESOLVED and zero SCHEDULED items.
- Neither item authorizes publication, deployment, or successor Phase work.
- Later maintenance must update these same IDs with task/commit references rather than deleting history.
