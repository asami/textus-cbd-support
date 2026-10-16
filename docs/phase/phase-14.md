# Phase 14 - CAR Review Native Job Integration and Acceptance

status=closed
development_item=DEV-CBD-004
depends_on=phase-11.1.md

Stage Status:

- Current status: CLOSED; CBD-owned native Job integration is accepted; this closure projection takes effect only after the verified local Phase release.
- Execution predecessor: [Phase 11.1](phase-11.1.md). The owner selected Phase 14 before Phases 12 and 13 on 2026-10-16; Phase numbers and completed histories are preserved.
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
Phase numbers remain unchanged. The original roadmap appended Phase 14 after
Phase 13 without a dependency on its lint behavior. The 2026-10-16 owner
direction changes execution priority to finish the existing Job work first.

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
ordinary Phase entry and splitting gates. The owner authorized implementation,
validation and review on 2026-10-16 in the canonical CBD checkout. Existing
uncommitted work is retained. This execution does not create a Goal or alter
the completed Phase 11 and 11.1 acceptance histories.

One coherent implementation Step, `JOB-14-INTEGRATION`, covers the transferred
native execution boundary, production selection/binding and fresh runtime reads
(JOB-14-01 through JOB-14-05). The same generated model and binding connect
these behaviors; acceptance covers the complete accumulator before its local
Step commit. JOB-14-06 is the separate Phase closure. Reuse native
Entity ownership, staging, transaction, cancellation and recovery mechanisms;
validate CBD wiring without adding a replacement concurrency or replay protocol.

## Work stack

| ID | Outcome | Status |
| --- | --- | --- |
| JOB-14-01 | Admit the transferred implementation and repair the current-source/generated-model build connection while preserving artifact mode. | accepted |
| JOB-14-02 | Validate explicit Review identity/revision/selection, canonical Provider input and Entity persistence compatibility. | accepted |
| JOB-14-03 | Complete and validate native staged Job execution, commit-gated completion and registered Provider cancellation. | accepted |
| JOB-14-04 | Connect production StartReview to server-owned selection and one durable Review-to-Job binding. | accepted |
| JOB-14-05 | Prove writer exit, independently fresh full-runtime reads, caller isolation and no redispatch. | accepted |
| JOB-14-06 | Complete relevant regression/full validation, independent acceptance and local Phase release. | closed on verified local Phase release |

## Selected development inputs and current status — 2026-10-16

The primary checkout is `/Users/asami/src/dev2026/textus-cbd-support` at initial
base `588395deb64d59f074e6d3a6597170f25b1bc476`. Its pre-existing dirty work
was retained before execution; unrelated Capability planning stays outside the
Phase commit boundary. The selected CNCF checkout is
`.codex-workflow/checkouts/goldenport-cncf-phase78`, initial base
`8c9e502a6c420eb7c4386f5198fd6b9b09995d76`. Its three changed Test fixtures
provide native observations and full-bootstrap test access; CNCF production
code is unchanged. Cozy checkout `.codex-workflow/checkouts/cozy-phase64` at
`834198b5c864c1599f29bc84effad2cc64146031` supplies generation without changes.

Actual generation and compilation succeeded using Scala 3.3.8, CNCF
0.5.3-SNAPSHOT source and Cozy 0.3.3-SNAPSHOT. The existing source-JAR bridge
required no replacement. The declared artifact coordinates remain unchanged;
source-mode acceptance is not a publication or a claim about older installed
artifacts. The CNCF Test bridge is not a production dependency or CAR output.

The execution, identity, Provider, persistence, production and separate-JVM
restart checks, CBD and CNCF full validation and CAR generation have passed.
The coherent implementation Step is independently accepted and locally committed.
The final independent Phase review and verified release govern closure. Detailed
execution results remain in private local storage. Native
unsupported started-work recovery refuses bootstrap; the fresh reader proves
that refusal and zero executable tasks rather than reopening a released store.

## JOB-14-01: Source admission and build connection

Reconcile both historically recorded build prerequisites: `sbt-cozy` obtains
`META-INF/cncf/runtime.yaml` from dependency JARs in `Compile / update`, whereas
the current source `RootProject` dependency does not supply that descriptor;
`project/InternalModelExclusionBuild.scala` also requires a directly declared
CNCF artifact in root and fixture dependencies.

The current `InternalModelExclusionBuild.sourceDependencySettings` already
exports and checks the selected CNCF source package JAR, and the exclusion
fixture distinguishes source and artifact dependency modes. The Capability
producer also reads the actual JAR runtime descriptor. Reuse these accepted
build connections and identify any remaining Job-specific gap through the
actual current build; the 2026-10-06 failure is historical evidence, not a
current failure or a request to implement the same bridge again.
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

## Current Phase closure and successor handoff — 2026-10-16

`JOB-14-INTEGRATION` is committed in CBD main at
`455ece0e15154246f0460eab671d04e61dfef917` and the selected CNCF worktree at
`8af769bcfd18e94e12ba9d8cc2d939292b7ed096`. The final release binds this
Phase and its checklist to `cbd-phase14-20261016-closure@1`; closure requires
the sole independent Phase review and verified native release receipt. The
closed statuses in this release are the resulting acceptance projection.

Production StartReview uses the declared server plan, native staged Job and
durable Entity binding. Completed work remains readable through a fresh runtime
with native authorization and no Provider redispatch. Unsupported started-work
recovery explicitly refuses bootstrap; this Phase adds no replay policy. Older
roots missing mandatory selection/reference fields are refused without migration.
The actual Provider process driver in production acceptance is controlled test
input; this does not certify an external installed Cozy process or publication.

Acceptance uses the selected source-mode development pair. Normal artifact-mode
selection is preserved, but compatibility with older installed artifacts is not
claimed. The generated CAR excludes CNCF Test fixtures. Strict CAR lint has no
FAIL; remaining legacy filesystem/value-type warnings and development publication
checks are retained as residual limitations, not a clean publication-readiness
claim. Phases 12 and 13 remain planned; Phase 13 retains CAR lint resolution.
The two nonblocking Factory maintenance items remain in the
[canonical Hygiene journal](../journal/2026/10/2026-10-16-phase-14-hygiene.md).
There are no newly adopted Development Candidates. CNCF Phase 69.7 closure and
Capability acceptance histories remain separately owned.

Detailed execution and review evidence is private. After release, retain it at
`macmini:~/backup/journal/2026/10/textus-cbd-support/phase-14/cbd-phase14-20261016/`
through 2026-11-15. The location is local retention metadata, not a publication.
The seven unrelated or historical user-work files remain uncommitted. No push,
publication or deployment is part of this Phase release.
