# CAR Review Native Job Execution Contract

status=authored_review_pending
phase=PHASE-69.7
step=JM69-10
slice=JM69-10CBD2C
manifest=PHASE-69.7-JM69-10CBD2C-implementation-manifest02@1
updated_at=2026-10-06
validation=pending
acceptance=notaccepted
acceptance_owner=PHASE-14

Current CBD integration acceptance belongs to
[Phase 14](../phase/phase-14.md) and its
[checklist](../phase/phase-14-checklist.md), allocated on 2026-10-07. The Phase
69.7/Step/Slice/manifest fields above retain original authoring provenance.
The [scope-separation handoff](../journal/2026/10/2026-10-06-cncf-job-cbd-scope-separation-handoff.md)
supersedes the historical CNCF closure obligations below; authored behavior
remains unaccepted until CBD validation and independent review.

## Explicit server work and staged ownership

### Production selection and persisted binding (Phase 14)

The generated `StartReview` operation selects `textus.cbd.review.execution`
through the existing component/subsystem/runtime configuration precedence.
The configured JSON has exactly `schemaVersion`, `diagnosisId`, `revision`,
`selection`, `template` and `providers`. Its schema is
`textus.cbd.review-server.v1`; identity and positive revision are explicitly
declared. Selection and template use the existing codecs. Each of one to eight
providers contains an exact descriptor object and request object. The admitted
Cozy process capability supplies execution; configuration supplies no executable
path. Transport target/profile must match the declared selection. The runtime
issues Review/Report identities and binds the provider request to that Review.
Missing configuration retains `review-execution-plan-required`.

`ReviewJobBinding` retains Review ID, native Job ID, diagnosis ID, plan revision
and the canonical admitted/queued Run document in the same outer UOW as staging.
The native diagnosis claim or successor decision alone issues ownership. Joined
and reused requests resolve the existing binding. Read and cancel require the
existing Review role plus the native session-first Job ownership rule.

New Job-produced Report snapshots carry their native Job ID. Report reads require
the exact Review binding and an authorized succeeded Job. Older non-Job snapshots
without a Job ID retain their existing role-based read contract. Missing mandatory
diagnosis selection/revision is refused; this release does not migrate old roots.
After terminal native recovery, completion is obtained from the exact persisted
Report snapshot with matching diagnosis, Review and Job. Native recovery does not
need to reconstruct a live `OperationResponse`. Started work without an admitted
replay policy remains RecoveryRequired and must not redispatch on read.
The native runtime refuses startup when that started work cannot be rehydrated;
the application does not manufacture a live runtime or a successful Run after
that refusal. Acceptance observes the signed Running status before writer exit
and native startup refusal with unavailable execution in a newly started process.
The existing CNCF recovery specifications own the `RecoveryRequired`
classification; CBD does not add an inspection API to reopen a refused store.
A caller may first observe an already succeeded Job after the queued binding was
persisted. Run projection therefore permits queued-to-completed when the native
Job supplies an exact canonical completion; intermediate polling is not required.

`CarReviewJobExecutionWork` contains the declared execution plan, Entity-issued
diagnosis owner, canonical template and one to eight explicitly selected provider
calls. Validation precedes ActionId/JobId issuance. It checks the existing typed
selection contract, native root collection/reference/revision, exact owner/Review,
profile, semantic target kind/organization/name/version, unique provider selections,
member rules and descriptor provider/rule bindings. Provider selection order has
set semantics only after uniqueness checks. The template requires the current
canonical schema/document type, one configured assessment, no baseline, and the
declared gate policy ID/version. Neither content nor wire digest supplies a missing
management identity or revision. Existing v1 request/bundle admission remains in
the coordinator; output Report/attestation digests retain their wire roles.

`CncfCarReviewJobGateway.stageExecution` requires an actual Core component,
provider application, ComponentFactory and ActionEngine. It authorizes `review.start`
before issuing an action identity, creates one task and invokes the original native
`JobEngine.stageSubmit` with the caller's UOW, Persistent and Async options. The
returned `ReviewJobId` carries exactly the native Job ID. Only native staging owns
reservation, resource termination and activation. Request summary and bounded
metadata are retained, with explicit diagnosis ID and plan revision.

The compatible transport-only `submit` returns structured
`review-execution-plan-required`, with no submission or provider invocation. The
former `provider-pipeline-pending` successful task is removed. Read and cancel
continue their existing native policies; final caller isolation is a later obligation.

## Native execution, completion and failure

The task uses the actual Job-supplied context, Job/Task identities and cancellation
scope. It lazily rebinds `RuntimeContext.withUnitOfWorkContext` and refuses a context
whose UOW is unchanged or is not bound to the rebound ExecutionContext. Only action,
component and correlation are copied from the submitting Core. Native UOW rebinding
retains the configured event engine and recorder. The original submitting UOW is
never used for provider execution or completion.

An actual `FunctionalActionCall` executes through the supplied `ActionEngine`;
framework authorization, component managed datastore lease, interpretation and
commit/abort/dispose remain native-owned. Inside its program, CBD reauthorizes
`review.start` and `review.submit-bundle`, validates work, checks cancellation and
executes selected providers in stable provider ID/version order through
`executeForCanonicalReport`. Requests take their start time and cancellation state
from the real Job context. Missing registration, mismatched descriptor, admission
refusal and runner failure produce structured failure without another runner.

The canonical builder receives the exact retained `AdmittedProviderBundleInput`
values. Template start time comes from the plan; completion/creation time comes
from the Job clock. Generated Entity programs complete the exact issued owner.
CBD checks the returned `Reused` diagnosis/Review and canonical Report ID/output
digest, then returns a RecordResponse containing `reviewId`, `reportId` and
`reportDigest`. TaskSucceeded requires successful actual ActionEngine commit and
native committed termination evidence. Execution, owner, Entity, prepare or commit
failure produces TaskFailed. Acknowledged provider/Entity effects remain attributable;
this contract promises no refund, reconstruction, retry or universal row rollback.

## Structured cancellation and compatibility

`CncfCarReviewProviderRunner.cancelC` returns `Consequence[Unit]` from a real
`CancellationCall[Unit]` and `cancel-car-review-provider` step. One delegate call
returns Unit success. Delegate NonFatal failure is bounded inside the step as
`provider-cancellation-failed`; actual engine failure and outer NonFatal invocation
failure are bounded by the same code. Descriptor, request, bundle and raw exception
text are excluded. Success is never a successful `ProviderBundleRunnerResult.Failed`.
The original `CarReviewProviderRunner.cancel(request): Unit` calls `cancelC` once and
deliberately discards its consequence. Execute result/NonFatal mapping and labels
remain compatible. Native trace-enabled Throwable conversion and trace-disabled
propagation remain ProviderEngine-owned; CBD adds no Throwable/interrupt policy.

`cancelRegistered` rejects null Core/request/provider/descriptor with
`provider-cancellation-request-invalid`; exact registry selection and whole typed
descriptor comparison retain `provider-not-registered` and
`provider-registration-mismatch`. The admitted registration uses only the structured
wrapper. Unexpected NonFatal lookup/admission failure is bounded. There is no
cancellation-specific registry, digest identity, cache or native Job control policy.

Each active provider has one original JobCancellationScope callback, closed in
finally. Checks before dispatch, after outcome and before Entity completion refuse
observed cancellation as `review-job-cancelled`. Already-cancelled work runs no
provider. The native callback deliberately ignores the returned consequence under
best effort; a failed ProviderCall remains an observed bounded failure, not evidence
of successful delivery. Native final-outcome races remain native-owned. No new
linearization, replay, rollback, callback ledger or cancellation manager is introduced.

## Executable specification and source boundary

The source-mode [Job chapter](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewJobExecutionSpec.scala)
uses the [native-backed CBD fixture](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewJobExecutionFixtures.scala)
and the CNCF test-only `NativeCbdReviewJobFixtures` bridge over unchanged native
fixtures. The actual generated CBD component is bound to the real Subsystem and
all generated Review collections, including the durable Job binding. Managed native datastore/entity spaces,
real UOW finalizers and exact signed Job reads supply positive evidence; no
process-local Review repository supplies success.

| Case | Contract |
| --- | --- |
| CJE1 | Actual owner, unpublished staging, same-ID commit activation, ProviderCall, persisted Report/attestation and separate-context exact Report read |
| CJE2 | Outer abort/dispose, no resurrection, compatible missing-plan refusal |
| CJE3 | Null/incomplete/reference/revision/Review/profile/target/provider/descriptor/gate/template refusal before issuance |
| CJE4 | Ended outer UOW, distinct actual Job UOW/context and unchanged callback count |
| CJE5 | Missing registration, descriptor conflict, failed/thrown provider; bounded failed Job |
| CJE6 | Actual separately terminated Entity owner refuses stale completion |
| CJE7 | Controlled fresh-UOW prepare/commit faults prohibit native success; acknowledged effects are retained |
| CJE8 | Original native queued/active cancellation, one callback, best effort and finally closure |
| CJE9 | Repeated drain/signed reads retain exact result and one Report/attestation |
| CJE10 | Ordinary shrinking declared revision/policy-order/selection property, seed69710301, minimum24, fresh real fixture per case |
| CANC1 | Actual Unit cancellation success with one delegate and safe ProviderCall/step observations |
| CANC2 | Actual bounded delegate NonFatal failure and failure observations |
| CANC3 | Exact registry admission plus missing/mismatched/null refusals before invocation |
| CANC4 | Compatible Unit SPI forwards once and deliberately ignores actual success/failure |

The [cancellation chapter](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CncfCarReviewProviderCancellationSpec.scala)
keeps per-case GWT and inspects actual CallTree values without snapshot printing.
The existing parent-package `CncfCarReviewProviderRunnerSpec` remains unchanged.

Explicit development source mode selects system property
`textus.cbd.cncfSourceRoot`, then environment
`TEXTUS_CBD_SUPPORT_CNCF_SOURCE_ROOT`. A selected canonical directory must have
regular `build.sbt`; invalid selection fails without artifact fallback. Source
mode uses RootProject compile/test dependency and excludes exactly the root
`org.goldenport:goldenport-cncf` artifact, enabling `src/test-current-cncf/scala`.
Absent selector preserves declared project.yaml artifact dependencies, versions,
Cozy/JNA/test dependencies and the internal-model exclusion fixture. The declared
artifact may lack the newly authored stage API until separate publication; this
work makes no published-runtime compatibility claim.

## Historical handoff acceptance boundary (2026-10-06)

At the original handoff, generated StartReview lacked server-owned provider selection and durable
Review-to-Job Entity binding in its outer UOW and reached the missing-plan
precondition. Production StartReview, fresh full caller-isolated read/cancel/report
pipeline, actual writer exit and independently fresh runtime acceptance remain
required. These same-process fixtures and selected copied-source validation do
not close JM69-10. SC5/RC5 remain OPEN and started work retains RecoveryRequired.
Original CNCF protected M2 Step review/commit, predecessor full handoffs, aggregate full
validation, sole original-epoch Phase review and local release remain pending.

Current CBD production selection/binding is defined above and exercised by
`CarReviewProductionSpec` (CPR1/CPR2). `CarReviewFreshProcessSpec` (CFR1/CFR2)
launches independent writer/reader JVMs: terminal reads retain exact canonical
Report/attestation documents and owner checks without redispatch; an interrupted
Running writer without a replay policy results in native startup refusal and
zero executable tasks. Phase 14's checklist alone records current acceptance;
the original CNCF closure and replay-policy ownership remain separate.
