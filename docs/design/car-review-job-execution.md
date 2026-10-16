# CAR Review Native Job Execution Design

status=accepted
acceptance_owner=PHASE-14
updated_at=2026-10-16

Current acceptance is the [Phase 14 closure](../phase/phase-14.md#current-phase-closure-and-successor-handoff--2026-10-16),
with its selected source-mode inputs and explicit recovery limits. This current
projection takes effect after the verified local Phase release.

## Historical authoring metadata — 2026-10-06

status=authored_review_pending
phase=PHASE-69.7
slice=JM69-10CBD2C
updated_at=2026-10-06
validation=pending
acceptance_owner=PHASE-14

Current CBD integration acceptance belongs to
[Phase 14](../phase/phase-14.md) and its
[checklist](../phase/phase-14-checklist.md), allocated on 2026-10-07. Phase 69.7
and Slice fields retain original authoring provenance; they do not represent
current acceptance. The
[scope-separation handoff](../journal/2026/10/2026-10-06-cncf-job-cbd-scope-separation-handoff.md)
defines the transferred CBD obligations independently of CNCF closure.

The [contract](../spec/car-review-job-execution-contract.md) fixes the server-owned
work, native staged submission, fresh Job UOW and canonical Entity completion.
Responsibility is split into `CarReviewJobExecutionWork` admission,
`CncfCarReviewJobGateway` staging and `CarReviewExecutionJobTask` execution rather
than growing ComponentFactory's unrelated runtime/history responsibilities.

Phase 14 connects generated StartReview through `CarReviewServerExecution`
(closed server configuration and registered Cozy capability) and
`ReviewJobProgram` (Entity binding, native Job query/control and completion
projection). The binding is created by the same outer UOW that stages the Job;
native Entity claim/successor ownership prevents duplicate submission. New
Report snapshots retain Job attribution so exact reads can require native
ownership and successful completion. Terminal restart reads reconstruct Run
completion from the bound Report snapshot; they never reconstruct live tasks.
Bounded Provider/owner failure codes are exposed by native Job result summaries.

The native chain is outer UOW stageSubmit -> native activation -> Job context ->
fresh UOW -> FunctionalActionCall/ActionEngine -> exact registered ProviderCall ->
retained admitted input -> canonical response -> existing generated Entity completion
-> actual UOW commit -> native TaskSucceeded. The outer caller still owns durable
Review-to-Job binding. No replacement owner, transaction engine, SQL pool, scheduler,
runtime scope or positive memory repository is introduced.

Cancellation has a separate Unit ProviderCall. `cancelC` preserves the actual result;
the original Unit SPI forwards once and discards it. `cancelRegistered` resolves the
exact immutable typed descriptor and registration. The task callback returns this
consequence to the original best-effort scope, which intentionally ignores it;
CallTree still records actual bounded failure. CBD observes cancellation around
dispatch/completion and closes callbacks in finally. Native race and exception
policies remain authoritative.

Explicit source mode and private copies connect the current CNCF main/Test source
with the CBD chapter without publication. The parent selects, prepares and validates
the private build. Semantic provenance records admitted roots/heads/copied paths;
it supplies no content certificate or management token. Ordinary builds retain
their metadata/artifact coordinates. Generated StartReview selection/binding,
fresh-process acceptance, compatibility/migration decisions and started-work replay
remain outside this slice and required by the original goal.
