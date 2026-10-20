# Scoped ReviewRequest and External Execution Contract

Status: normative target; Phase 15.1 implementation and acceptance pending
Date: 2026-10-18
Design: [Execution ownership](../design/scoped-review-execution-boundary.md)
Evaluation: [Scoped quality review](scoped-quality-review.md)

## E1: Shared review definition

The existing scoped evaluation input selects target, requirements, bounded
materials and checks. The prepared ReviewRequest is a distinct semantic output:
it carries ordinary request/review identity and revision, subject identity and
revision, selected scope/checks/profile and rule revision, instructions/criteria,
required bounded evidence/context references, expected typed response shape and
declared execution intent (orchestrated or standalone). Existing identities may
serve both request and review when the declared relation is one-to-one; do not
invent a second identity ledger. References associate facts, not authenticate them.

Preparation reuses Phase 15's deterministic evaluation and semantic criteria.
The prompt is derived from that definition. Required facts must be available to
the selected executor; an unresolved reference remains a gap, not a request to
copy the worker's full conversation. Source/diff/material content remains review
data, never tool or policy authority. Exact CML operation/value declarations are
settled with the implementation; this specification defines their roles rather
than claiming those new APIs already exist.

## E2: Prepare, execute and accept

Normal CML/Operation/ActionCall wiring exposes preparation and external-result
acceptance. Preparation returns the structured request without any AI-provider
call. The caller executes it using its existing worker or explicitly selected
independent worker and returns a typed response bound to that request.
Acceptance performs no AI call and produces the ordinary Phase 15 result.
The caller supplies/retains the selected request through its existing work
context; CBD is not required to create a request database or a new Job lifecycle.

Standalone use explicitly selects the existing CBD runner, executes the same
prepared request and uses the same acceptance logic. An omitted selector never
turns an orchestrated request into standalone execution. Preserve the existing
standalone use case as an intentional route, not as an old-API fallback.

## E3: Result and actual attribution

Accept only a result attributable to the selected request/review revision,
subject/revision and requested checks/scope. Preserve Phase 15 findings, covered/
unsupported/failed outcomes, deterministic findings and valid clean results.
Missing, malformed, wrong-target or insufficient output is an explicit failure
or coverage gap as appropriate; it does not grant Admission or commit permission.
Use the existing decoder/evaluation rules at their owner, not duplicate gates.

Record the actual execution route and executor attribution, with model/provider
information only where observed. External Codex execution must not fabricate
Textus AI SPI metadata or claim AiRunner execution. Existing mandatory SPI-only
provenance assumptions in CBD and the CAM consumer must be adapted explicitly
for external execution; absence of required actual attribution remains a gap.
Neither a worker's `accept` string nor a stored source/result reference is proof
of execution, independence or authorization. No provider attestation framework
or guessed model identity is introduced.

## E4: Consumer continuation and review policy

sm-workflow prepares the request before semantic work is issued, retains the
same selected review/subject through its existing continuation, then submits the
external result for CBD acceptance and CAM evaluation. Submit must not trigger
an additional hidden semantic AI review after the selected worker has responded.
Only the owning workflow decides repair, acceptance, next Advance and commit.

The consumer defaults to the worker holding relevant context where independent
review is not required. Required independent Step/Phase/final verification uses
an explicitly selected independent executor with the same contract. This change
does not waive or multiply independent-review obligations. Final reuses applicable
Phase results and requests only missing affected work; it is not an automatic
second full review.

## E5: Bounded verification and exclusions

Required examples cover a prepared request -> actual external response -> typed
accepted result; zero CBD AI calls during both preparation and acceptance;
explicit standalone invocation through the common definition; valid zero findings;
wrong-target/missing coverage and retained deterministic findings. Reuse existing
tests for unchanged rules and lower-level guarantees. Fixtures are not real
external-execution evidence, and CBD acceptance is not consumer Phase closure.

No implicit retries, duplicate delivery arbitration, recovery/replay, content
hashes/copies/TTLs, conversation retention or broad legacy CAR API migration are
required. No compatibility aliases: update affected typed callers directly and
let compilation reveal missed source-contract changes. Existing unrelated Phase
history, runtime state and stored evidence are not migrated by this contract.

## E6: Normal Operation signatures

`CbdSupport/CbdReviewAdmin/prepareScopedQuality` takes
`ScopedQualityReviewPreparation(request, executionIntent)` and returns
`ScopedQualityReviewDefinition(request, executionIntent, profile, ruleRevision,
prompt, responseSchema)`. Intent is explicitly `orchestrated` or `standalone`.
The definition contains the existing bounded request; prompt and JSON schema are
derived instructions, not an alternative authority or stored execution record.
The profile is `scoped-quality` and the rule revision is `cbd-scoped-quality-v1`.

`CbdSupport/CbdReviewAdmin/acceptScopedQuality` takes
`ScopedQualityReviewAcceptance(definition, response, execution)` and returns
`ScopedQualityReviewResult`. The caller retains the definition between calls.
Response repeats `reviewId`, `revision`, `subjectId`, `subjectRevision`, selected
semantic checks and findings. CBD evaluates deterministic checks itself and
merges their findings; an external response cannot replace them. A mismatched
identity or invalid semantic response produces failed coverage, never acceptance.

`ScopedQualityReviewExecution` records route (`external` or `standalone`),
selection (`same-context`, `independent`, or `standalone` respectively), and
optional observed `executorId`, `providerId` and `model`. External execution
requires actual executor attribution to cover semantic checks. The caller supplies
observed attribution separately from the worker's review content. Missing
attribution produces unsupported coverage. CBD does not verify an attestation or
retain execution state. The result carries this execution record; its legacy
`providerId` identifies the evaluator when no provider was observed and must not
be interpreted as proof of a Textus AI invocation.

Both Operations perform zero CBD AI calls. The explicit standalone
`reviewScopedQuality` Operation uses the same definition, response schema and
acceptance rules with its existing optional AI runner. No omitted selector starts
standalone execution, and external preparation never falls back to that runner.
