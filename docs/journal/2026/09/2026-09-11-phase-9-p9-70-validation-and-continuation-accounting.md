# Phase 9 P9-70 Validation and Continuation Accounting

**Date:** 2026-09-11
**Phase:** Phase 9
**Status:** Phase 9 closed by the accepted P9-73 release commit

## Purpose

This chronological, non-normative entry records the completed validation work
for P9-70, the documentation-record basis for P9-71, and the
successor/deferred-work boundary needed for P9-72. The authoritative completion
status remains
[`docs/phase/phase-9-checklist.md`](../../../phase/phase-9-checklist.md).
This entry supplies evidence but does not independently close a checklist item.

## Accepted implementation basis

The P9-60 through P9-63 implementation steps are committed on the current
Phase 9 lineage:

- `3d7e648` (P9-60A documentation) and `7dc0bba` (P9-60B feature);
- `1281a88` (P9-61A documentation) and `e0e8a09` (P9-61B feature);
- `0f9c1e1` (P9-62A documentation) and `e44e4ce` (P9-62B feature); and
- `9a0abe9` (P9-63A documentation) and `71f42f1` (P9-63B feature).

Each accepted step retained the Phase boundary: an admitted composition
candidate may navigate to an exact Dashboard projection, but neither surface
creates a catalog fact, selection, or canonical-source mutation.

## P9-70 execution evidence

- `P9-70-SBT-001`: the full `sbt --batch test` run passed with 457 succeeded,
  zero failed, 100 suites, and zero aborted.
- `P9-70-CAR-REBUILD-001`: the current `cozyBuildCAR` rebuild completed, and
  the three-way manifest/model-metadata/packaged-CAR ABI comparison reported
  `CAR_ABI_SURFACE_OK` for 19 operations and seven entities and
  `CAR_ABI_PACKAGE_MATCH_OK`.
- Strict CAR ABI policy checks accepted the current manifest and a compatible
  addition, rejected a breaking minor removal, and accepted the same removal
  with an intentional major-version change. CAR lint reported no failure; its
  two existing baseline/SNAPSHOT warnings remain deferred hygiene.
- The static runtime declaration check accepted `0.5.3-SNAPSHOT` with
  `representative-sar` evidence.
- The isolated representative CBD/SIE SAR session `53772` passed baseline,
  global-disable, SIE-disable, and operation-disable profiles and emitted
  `CBD_SIE_SAR_POLICY_MATRIX_OK` and
  `RUNTIME_COMPATIBILITY_EXECUTION_OK runtime=0.5.3-SNAPSHOT`. It used a fresh
  CBD CAR plus exact retained dependent CAR inputs in a task-private local CAR
  root; it did not mutate the shared warehouse, publish, deploy, or push.
- Final focused re-review found no Current Boundary Blocker. It identified one
  nonblocking, pre-existing documentation hygiene item:
  `HYG-P9-70-USER-GUIDE-COZY-COORDINATE-001` (`docs/user-guide.md` documents
  Cozy `0.3.4-SNAPSHOT` while `project.yaml` declares `0.3.3-SNAPSHOT`).

## Acceptance-record resolution

The original final review-disposition recording attempt could not store the
required Phase Goal-bound receipt. The operational details and its resolution
are recorded in
[`phase-9-closure-recording-infrastructure-incident.md`](../../../notes/phase-9-closure-recording-infrastructure-incident.md).

The shared recorder was corrected, the active Phase Goal binding was migrated
through the supported V1-to-V2 path, and both migration and current-binding
verification passed. The recorder then accepted the clean P9-70/P9-71 focused
re-review for the complete current tree as
`review-disposition-sha256-7e0d1abd0b747105fc0940332e2ce5e5116c9be093c0db8c076a6760250379a3`.
It contains no Current Boundary Blocker and retains
`HYG-P9-70-USER-GUIDE-COZY-COORDINATE-001` as nonblocking hygiene with a
`pending-journal-sync` disposition. Thus P9-70 execution, P9-71 documentation,
and P9-72 continuation accounting have accepted evidence; P9-73 remains the
separate final review, release, and closure boundary.

## P9-72 continuation accounting

The following items remain outside the P9-70 repair boundary and require
explicit successor or maintenance authority:

- `HYG-P9-20A-CAR-ABI-BASELINE`: establish a released CAR ABI baseline.
- `HYG-P9-20A-CAR-SBT-COZY-SNAPSHOT`: reconcile the snapshot build-plugin
  coordinate with release-readiness policy.
- `HYG-P9-53B-UNUSED-MAPPING-PARAM`: retain the pre-existing unused mapping
  parameter for a separately scoped cleanup decision.
- `HYG-P9-61B-SCALA-VERSION-HEADERS`: account for historical Scala-version
  header maintenance during Phase closure accounting.
- `HYG-P9-70-USER-GUIDE-COZY-COORDINATE-001`: reconcile the user-facing Cozy
  prerequisite with `project.yaml`, or state the supported external-toolchain
  policy.
- `DEV-CBD-002`: Phase 10 owns durable internal-model packages, approval
  history, continuation, rehydration, freshness/invalidation, and CML mutation
  gating. It is not started by this record.

P9-73 may start only after P9-70 acceptance is properly recorded and the
remaining Phase ledger items are either accepted or explicitly relocated.

## Materialized closure hygiene ledger

### HYG-P9-61B-SCALA-VERSION-HEADERS — Add the required Scala source headers

- Status: OPEN
- Source review identity: phase-9-p9-61b-focused-rereview
- Evidence: CandidateDesignSemanticDiffIntegration.scala and CandidateDesignSemanticDiffIntegrationSpec.scala lack the repository-required Scala version-history header.
- Current-Phase nonblocking reason: the frozen P9-61B behavioral repair can be validated independently; adding headers would require repository-format and attribution evidence not admitted to this slice.
- Owner repository: textus-cbd-support
- Target Phase: next explicitly admitted Scala-source maintenance slice.
- Dependency: accepted P9-61B runtime/spec boundary.
- Risk: low; source-history metadata is incomplete.
- Resume condition: a future admitted slice edits either P9-61B Scala file with repository header-format evidence.
- Prohibited local workaround: do not fabricate version, date, or author metadata.

### HYG-P9-70-USER-GUIDE-COZY-COORDINATE-001 — Reconcile the documented Cozy prerequisite

docs/user-guide.md:14 lists Cozy/sbt-cozy 0.3.4-SNAPSHOT as a prerequisite, while the authoritative project.yaml:20 declares cozyVersion 0.3.3-SNAPSHOT. Reconcile the user-facing prerequisite with project.yaml (or document the supported external toolchain policy) so setup guidance does not direct users to an unverified coordinate; this is pre-existing target-file documentation debt and is outside the P9-70 runtime/ABI fix boundary.

## P9-73 pre-release review resolution

The mandatory full Phase review recorded as
`review-disposition-sha256-c451e18db336d7e5ffd75afb33e561ddc1a0240e46c8ee70f0d3e3f6d5227a19`
found one Current Boundary Blocker:
`CB-P9-73-RUNTIME-ACCEPTANCE-STATUS-001`. The runtime compatibility matrix
still labelled the accepted P9-70 representative-SAR evidence as pending.

The bounded repair updated only the runtime compatibility JSON and Markdown
acceptance status. Its static runtime validation and CAR lint passed. The
required focused re-review recorded as
`review-disposition-sha256-f093e95ed73714ca9ac7f67673891ca42bf74f0266e219deb70d98d1e9059587`
accepted the repair without a Current Boundary Blocker. Final post-closure CAR
lint, the one final full SBT suite, and the Phase release commit remain pending;
this record does not claim Phase closure before those gates succeed.

## P9-73 final release validation and closure

The final post-closure CAR lint passed with only
`HYG-P9-20A-CAR-ABI-BASELINE` and `HYG-P9-20A-CAR-SBT-COZY-SNAPSHOT`, the
already materialized nonblocking warnings. No new CAR lint failure was found.

`P9-73-SBT-001` ran the one final `sbt --batch test` suite for this Phase. It
completed with 457 succeeded tests, zero failed tests, 100 completed suites,
zero aborted suites, and the terminal shared-lock `lock=released` marker. The
full-review blocker repair, focused re-review, CAR lint, and full-suite gate
are therefore accepted. This release commit closes Phase 9 without starting or
changing Phase 10.
