# Scoped ReviewRequest and External Execution Contract

Status: normative contract; external handoff accepted, standalone qualification in progress
Date: 2026-10-27
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
data, never tool or policy authority. The existing CML Operation/value declarations
implement the roles described here; E6 records their actual signatures. Their
presence does not establish connected external execution or owner acceptance.

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

## E7: Connected external verification

The test-host bridge `ScopedQualityReviewExternalExecutionMain` invokes the
generated ordinary Operation, factory ActionCall and provider/UoW route with an
installed trap AiRunner. Each invocation reports `ai_calls=0`; a call to that
runner makes the invocation fail. The test-only
`scripts/test/scoped-review-execution.py` helper launches this already compiled
bridge using the normal exported test classpath and a caller-selected Java
executable. In prepare/accept mode it starts no SBT, network client or external
reviewer and creates no review response. Native validation owns compilation of
changed source.

The caller retains the actual preparation input and returned definition, issues
that definition to its selected independent executor, and supplies the executor's
typed response with separately observed execution attribution to acceptance.
Preparation and acceptance output alone establish no worker independence or
consumer Admission. The bounded real-run references and actual coverage belong
in the existing Phase 15/15.1 handoff journal. Fixture specifications continue to
qualify result rules and do not replace that external response.

The [2026-10-27 connected example](../journal/2026/10/2026-10-18-phase-15-15.1-review-handoff.md#connected-external-execution--2026-10-27)
records actual preparation, independently produced semantic response and normal
Operation acceptance. Both Operation executions reported zero CBD AI calls.
The response covered four semantic checks with zero findings; CBD added the
selected deterministic CML result and returned `completed` with five covered
checks. Native focused validation then passed 18 examples across three suites,
and the independent Step review accepted the bounded external handoff for
sm-workflow Phase 10.

## E8: Explicit standalone qualification

`reviewScopedQuality` intentionally selects standalone execution. The provider
already uses the common prepared prompt/schema and common acceptance; it resolves
the installed optional AiRunner once. Operation specification O1 compares its
captured prompt/schema to an explicitly prepared standalone definition and
checks the returned standalone route and observed fixture metadata. That
RecordingAiRunner example is a connection specification, not real AI execution.

The test-only `ScopedQualityReviewStandaloneExecutionMain` explicitly loads the
existing optional Textus AI public Component factory from its runtime classpath,
configures the selected Gemma/Ollama profile, model and loopback endpoint, and
resolves ordinary SPI with CBD's application-purpose registration. It invokes
the generated `reviewScopedQuality` Operation with a real HTTP runtime driver.
No production dependency, provider implementation, fallback or new review policy
is introduced. The host retains the actual result and normal calltree; unavailable
or failed coverage remains a failed qualification run, never a synthesized PASS.
The test host initializes normal CNCF runtime admission from its Task-private
`.textus/config.json`. Its HTTP execution plan grants only the actual AI component
POST access to the explicitly selected loopback origin, with bounded connect/read
timeouts and response bytes. AI and CBD participants inherit that host's global
runtime scope; the HTTP runtime context belongs to the actual AI component.
The explicit Task-private controlled `test.yaml` descriptor is passed through
the normal `--textus.test.descriptor` bootstrap argument, as in the repository's
existing HTTP security harness. It supplies execution-profile/user activation
without substituting an AI response or changing production security policy.
The host handle and embedding are closed on every initialized execution path.
The explicit test-host output allowance is 8192 tokens; the existing 180-second
provider/read limit, zero record retries and 262144-byte response bound remain.
This allowance uses the existing execution-class configuration and changes no
CBD product policy or application-purpose registration.

The helper's explicit `reviewScopedQuality` mode owns one bounded loopback
Ollama process session: readiness, ordinary Java client, terminal result and
cleanup. Its receipt records the selected endpoint/model, actual server PID,
start/end times, readiness, client exit and cleanup. It reads installed models
without downloading models or changing global configuration. The actual
standalone result and observed provider/model remain pending until this new
test host is compiled by native validation and executed by the session runner.
The receipt's `runDirectory` selects the retained `server-process.json` record
(`baseUrl`, actual `pid`, `startedAt`). Successful execution requires
`ready=true`, `clientSucceeded=true`, `cleanup=true` and
`processCompletion=terminal`; these record runtime facts only.
