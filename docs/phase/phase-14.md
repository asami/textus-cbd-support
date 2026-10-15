# Phase 14 - CAR Review Native Job Integration and Acceptance

status=planned
development_item=DEV-CBD-004
depends_on=phase-13.md
validation_ownership=phase-local

Stage Status:

- Current status: PLANNED — transferred implementation exists but is not accepted.
- Roadmap predecessor: [Phase 13](phase-13.md).
- Owner: Textus CBD Support development.
- Update rule: completion is recorded only by the [Phase 14 checklist](phase-14-checklist.md) with reproducible validation and independent review evidence.
- Planning date: 2026-10-07 JST.

## Purpose and provenance

Complete CBD-owned CAR Review execution through the native CNCF Job runtime,
including production StartReview, registered Provider execution and cancellation,
Entity persistence, durable Review-to-Job binding and independently fresh reads.
Phase 14 owns the unfinished CBD work transferred from CNCF Phase 69.7 by the
[2026-10-06 handoff](../journal/2026/10/2026-10-06-cncf-job-cbd-scope-separation-handoff.md).

The transferred delta contains 21 tracked changed paths and 12 new files,
including one deleted persistence-contract spec. It is authored work, not a
validated release. Its original JM69-10CBD1/CBD2A/CBD2C manifests, journals,
focused results and failures retain their original provenance; they are not
Phase 14 acceptance records. CNCF Phase 69.7 retains its own closure owner.

This is a separate CBD Phase. It does not reopen completed Phases 5 or 8,
expand Capability Phases 11/11.1, or take ownership of CNCF's scheduler,
transaction engine, native cancellation or recovery implementation. Existing
Phase numbers and the order through Phase 13 remain unchanged; Phase 14 is
appended to the roadmap. No dependency on Phase 13 lint behavior is implied
by that scheduling position.

## Required inputs and execution entry

- The transferred CBD source, CML, specification, design, executable specs and
  recorded failed source-mode build attempt.
- Exact admitted CNCF Job staging, ActionEngine, ProviderCall, UOW and Entity
  contracts, source revision and available artifact/API versions. A planned
  or authored upstream contract does not establish acceptance or published
  compatibility. Upstream release/publication remains separately owned.
- Accepted CBD Review lifecycle, canonical Report, Entity persistence,
  authorization and internal-model output-exclusion contracts from the
  existing completed Phase ledgers.

Before execution, inspect the current source and prerequisite evidence, select
the exact admitted build inputs, and calibrate Step/Slice workload through the
ordinary Phase entry and splitting gates. This planning addition starts no
Goal, validation, implementation, acceptance review or release. It does not
resume or alter the blocked Phase 11 Goal.

## Work stack

| ID | Outcome | Status |
| --- | --- | --- |
| JOB-14-01 | Admit the transferred implementation and repair the current-source/generated-model build connection while preserving artifact mode. | planned |
| JOB-14-02 | Validate explicit Review identity/revision/selection, canonical Provider input and Entity persistence compatibility. | planned |
| JOB-14-03 | Complete and validate native staged Job execution, commit-gated completion and registered Provider cancellation. | planned |
| JOB-14-04 | Connect production StartReview to server-owned selection and one durable Review-to-Job binding. | planned |
| JOB-14-05 | Prove writer exit, independently fresh full-runtime reads, caller isolation and no redispatch. | planned |
| JOB-14-06 | Complete relevant regression/full validation, independent acceptance and local Phase release. | planned |

## JOB-14-01: Source admission and build connection

Resolve both recorded build prerequisites: `sbt-cozy` obtains
`META-INF/cncf/runtime.yaml` from dependency JARs in `Compile / update`, whereas
the current source `RootProject` dependency does not supply that descriptor;
`project/InternalModelExclusionBuild.scala` also requires a directly declared
CNCF artifact in root and fixture dependencies.

Select and document a concrete build solution before implementation. The
previously proposed source-JAR bridge remains unimplemented planning input.
Prove actual CML generation, generated-source compilation and required runtime
descriptors with the selected inputs. Preserve the declared `project.yaml`
artifact configuration when no source selector is supplied. Invalid explicit
source selection must fail without silently using another artifact.

The source-mode specs require the current staging API and CNCF Test fixtures,
including `NativeCbdReviewJobFixtures`. Admit that dependency explicitly and
keep test bridges outside production output. Source-mode success does not
establish compatibility with an older published artifact. Record the actual
supported production dependency before closure.

## JOB-14-02: Review identity, Provider input and persistence

Validate the explicit plan reference/revision and execution-selection contract,
Entity-issued ownership, claim/join/reuse/successor decisions and exact canonical
Provider input retained through registered execution and cache re-admission.
Preserve actual Report, bundle and target digests in their wire roles; they must
not substitute for missing management identity or revision.

Reconcile the deletion of `CarReviewPersistenceContractSpec.scala` with the
current persistence contract and executable-coverage ledger. Prove claim/join,
completion/reuse, exact authorized persisted Report reads, expiry/tombstones,
non-success history, concurrent successor/one start, issued-owner refusal and
current persisted terminal-state checks using observable production behavior.
Select explicit compatibility and migration treatment for older persisted roots;
missing mandatory fields must not trigger silent conversion or a new identity.

## JOB-14-03: Native execution and cancellation

Complete the [Job execution contract](../spec/car-review-job-execution-contract.md)
and [design](../design/car-review-job-execution.md). Validate work before identity
issuance, stage the exact native Job in the outer UOW, and activate only after
native commit. Abort/dispose must leave reservations unpublished.

Execute registered Providers in the real Job context and a distinct live UOW.
Complete the exact Entity-issued owner and persist the canonical Report and
attestation before native successful Job completion. Provider, owner, Entity,
prepare or commit failure must not become a successful Job. Validate actual
structured cancellation results, pre-dispatch cancellation, active callback
delivery and callback closure under the native best-effort policy.

Retain all CJE1-CJE10 and CANC1-CANC4 obligations, including the authored
shrinking property. The removed `provider-pipeline-pending` placeholder must
not supply success evidence. No CBD-local scheduler, replacement transaction
engine or memory-only success repository may replace the native path.

## JOB-14-04: Production StartReview and durable binding

Connect the actual generated StartReview operation to explicit server-owned
Provider selection and the admitted execution plan. Preserve authorization,
profile, target and selection checks. Persist one attributable Review-to-Job
binding in the same outer UOW as native staging; concurrent or repeated
requests must not launch duplicate Provider work.

Use that binding for authorized read/cancel/report operations. A transport-only
request without a plan retains its explicit refusal. Record the started-work
recovery/replay decision before claiming that path supported. Running or
Suspended work remains `RecoveryRequired` without an admitted policy;
restart must not silently replay it.

## JOB-14-05: Independently fresh runtime acceptance

Exercise production StartReview, Provider execution, Entity completion and
native Job outcome with persistent storage. Terminate the writer process and
read exact Review, Job, Report and attestation identities through a new full CBD
runtime. Prove caller isolation and authorization for read/cancel/report, stable
terminal outcomes, and no Provider redispatch or duplicate snapshots after
fresh reads. Same-process fixtures and copied-source build outputs alone do
not establish this boundary.

## JOB-14-06: Validation and closure

Each Step requires proportionate executable validation and independent
acceptance before its own local commit. Preserve the earlier results only for
their demonstrated prerequisite scope. Run the actual execution/cancellation,
identity/Provider-input and persistence specifications, generated StartReview
and lifecycle regressions, output-exclusion checks and representative fresh
runtime scenario against the selected dependencies.

Phase 14 owns its full SBT validation, applicable declared CAR lint, one
comprehensive Phase review and closure of admitted blockers before its local
release. Phase 11.1's deferred full-validation ownership covers only the
Capability sequence and cannot discharge Phase 14 validation. Synchronize the
checklist, handoff and current specification/design ownership with actual
results. Preserve separate Hygiene, including
`HYG-PH697-CBD-FACTORY-SIZE-01`, without treating maintenance as accepted work.
Push, publication and deployment are outside this Phase addition.
