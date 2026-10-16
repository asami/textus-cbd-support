# CNCF Job closure and CBD implementation handoff

Stage Status:

- Current status: ACCEPTED_BY_CBD_PHASE_14 — effective after its verified local release.
- Owner: textus-cbd-support development; CNCF Phase 69.7 closure remains with the original CNCF task.
- Acceptance owner: [CBD Phase 14](../../../phase/phase-14.md), DEV-CBD-004, allocated on 2026-10-07 JST.
- Update rule: Record actual CBD build/test/review results here and in the [Phase 14 checklist](../../../phase/phase-14-checklist.md) before promoting this handoff to accepted implementation. This document does not close a CBD Phase or CNCF Phase.
- Closure basis: CBD's own implementation and acceptance work; the transferred changes do not supply CNCF Phase 69.7 acceptance.

## Current CBD acceptance — 2026-10-16 JST

The [Phase 14 closure](../../../phase/phase-14.md#current-phase-closure-and-successor-handoff--2026-10-16)
and [checklist](../../../phase/phase-14-checklist.md) own current acceptance.
The existing source bridge, explicit plan/selection, registered execution and
cancellation, production StartReview, durable binding, canonical completion
and independent fresh-runtime reads are accepted within that declared boundary.
Unsupported started work refuses bootstrap without replay; missing mandatory
older-root data refuses without migration. The selected source-mode development
pair is not older published-artifact compatibility. CNCF production is unchanged;
its selected worktree supplies only Test observations, and Cozy requires no edit.

Earlier sections below describe the transfer-time state and remain historical.
Their pending build proposal and unfinished CBD statements are superseded by
Phase 14's current closure. CNCF Phase 69.7 closure remains separate. Detailed
execution evidence remains private under the Phase's local retention metadata.

## User-directed scope change — 2026-10-06 JST

The user explicitly directed:

> CBDを外して、CNCFのJobはクローズして。
> 現在CBDを作り込んだ部分については、該当する正規ソースに引き継いで。

CBD is removed from CNCF Phase 69.7's completion requirements. Complete the CNCF Job work using its native executable specifications, cross-process evidence, required validation and independent review. CBD production Provider/Entity/StartReview/fresh-runtime integration is separate CBD work. Do not mark the unfinished CBD integration as successful or retain it as a prerequisite for CNCF closure.

The user separately authorized sending this instruction to the original `SimpleModeling開発` task. That task owns the revised CNCF Goal, Phase/checklist projection, validation, review, local commits and actual closure audit. No push, publication or deployment is requested by this handoff.

## Canonical source transfer

The working delta from the dedicated `textus-cbd-support` checkout under the CNCF workflow was applied to the regular `textus-cbd-support` checkout at `~/src/dev2026/textus-cbd-support`. Both checkouts had the same native Git base, `44c99a09864696db0416324923b7587ee2cfccce`, and origin `asami/textus-cbd-support`.

The transfer included 21 tracked changed paths and 12 new files. One tracked change deletes `CarReviewPersistenceContractSpec.scala`; this is carried as the actual current source delta and is not a claim that its coverage is unnecessary. CBD's own review must reconcile that deletion with the persistence specifications and executable coverage.

The regular checkout's 11 existing capability-catalog and Phase 11/11.1/12 planning changes were disjoint and preserved. The original dedicated checkout, workflow history, failed validation evidence and copied build outputs were retained. Nothing was staged, committed, pushed or published by the transfer. `git diff --check` passed in the canonical checkout. No SBT invocation or new runtime acceptance was performed during this handoff.

Transferred responsibilities include:

- Provider canonical input and registered Provider execution/cancellation.
- Explicit Review identity, reference/revision, execution selection and Entity-issued completion ownership.
- CML, generated-model contracts and ComponentFactory persistence/completion integration.
- Native staged Job submission and fresh Job UOW execution, with Entity completion before successful Job completion.
- Source-mode Job/cancellation executable specifications and their fixtures.
- The related specification, design and implementation journals.

See the retained [Job execution contract](../../../spec/car-review-job-execution-contract.md)
and [design](../../../design/car-review-job-execution.md). The October 6
`phase-69.7-explicit-review-identity`, `phase-69.7-provider-canonical-input` and
`phase-69.7-provider-job-execution` journals remain preserved local authoring
material, unchanged and outside Phase 14's commit boundary because they contain
historical execution evidence. Their Phase 69.7 references describe provenance;
this handoff supersedes their statements that unfinished CBD integration is
required for CNCF closure.

## Actual validation boundary and remaining CBD work

Earlier identity/canonical-input/staging results prove only their recorded prerequisite scope. The new actual Provider/Job execution chapter remains unvalidated. The latest focused source-mode attempt compiled current CNCF main/test sources, then stopped before CBD compilation or tests on two build assumptions:

1. sbt-cozy generation extracts `META-INF/cncf/runtime.yaml` from dependency JARs in `Compile / update`; a source `RootProject` dependency alone does not provide that descriptor to the generator.
2. `project/InternalModelExclusionBuild.scala` requires a direct declared CNCF artifact in both root and fixture dependencies; the new source mode removes that root artifact.

The proposed source-JAR build bridge was planning only at transfer time. Do not describe it as implemented or validated. Preserve normal artifact mode and actual generated-output verification when CBD resolves these prerequisites. The test source mode also requires the unpublished current CNCF staging API and CNCF Test fixtures, including `org.goldenport.cncf.job.NativeCbdReviewJobFixtures`; it does not prove compatibility with an older published artifact.

CBD continuation must assess the transferred implementation, fix its source-build connection, run the actual execution/cancellation and relevant persistence/output-exclusion specifications, and independently review it. Production StartReview server selection, durable Review-to-Job binding, writer exit, fresh full-runtime reads, caller isolation and no redispatch remain unfinished CBD integration work. Started-work replay policy was not resolved by this transfer; do not infer permission to replay Running or Suspended work.

The transfer is preservation of work in the owning repository, not acceptance, a release, or completion of CBD development.

## CBD Phase allocation — 2026-10-07 JST

The user requested a dedicated owning Phase for these transferred corrections.
Phase 14, CAR Review Native Job Integration and Acceptance, now owns the build
prerequisites, identity/Provider-input/persistence checks, native execution and
cancellation, production StartReview and durable binding, independently fresh
full-runtime proof and separate CBD validation/review/release. All checklist
items remain unchecked. The current handoff status stays
`HANDED_OFF_UNVALIDATED`; existing source and prior prerequisite results do not
establish accepted integration.

Original Phase 69.7 manifest and journal identities remain authoring provenance.
This allocation changes neither CNCF closure nor completed CBD Phase histories,
and it does not resume or expand Capability Phase 11. Phase 14 is appended after
Phase 13 in the roadmap; no implementation or release is performed by allocation.
