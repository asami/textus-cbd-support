# Scoped Review Semantics and Execution Ownership

Status: accepted design; external handoff accepted, standalone qualification in progress
Decision date: 2026-10-18

CBD owns scoped review knowledge, selected criteria, deterministic evaluation,
semantic request construction and interpretation of typed findings/coverage.
The execution owner is separate. Orchestrated callers obtain the ReviewRequest,
select an existing worker/engine and return its result. Explicit standalone use
executes the same request through CBD's existing AiRunner. Both share policy and
result acceptance; neither creates a competing quality report.

Phase 15 owns the [evaluation contract](../spec/scoped-quality-review.md).
Phase 15.1 owns the [execution separation](../spec/scoped-review-execution-contract.md).
These extend the scoped route without reopening the accepted historical
[CAR Review architecture](car-review-architecture.md) or requiring its wholesale
Job/report migration. Existing scoped implementation is the extraction source.

sm-workflow owns selected scope, lifecycle requirements, CAM, continuation and
commit authority. Same-context review is the ordinary choice when independence
is not required. A separate worker/model is an explicit consumer decision.
Existing independent review obligations remain independent; same-context work
cannot be relabeled to satisfy them. CBD neither starts a hidden AI session nor
snapshots, persists or reconstructs a Codex conversation.

Follow the external main route first: prepare -> existing worker -> accept result
-> consumer CAM/continuation. Then connect standalone execution to the same
contract. Reuse the existing Continuation and effect owners; execution separation
alone does not justify new durable request storage, Job state, replay, locking,
deduplication or integrity machinery. Missing execution/result is an explicit
gap. No hash/content comparison is request identity or execution proof.

This is pre-release contract development. Update actual typed callers together;
do not add compatibility aliases or fallback paths to hide incomplete migration.
Recorded histories and applicable earlier validation remain retained evidence.

The existing external test-host bridge connects generated preparation and acceptance
Operations to the real provider with a trap AiRunner. The test-only Python
helper runs that compiled bridge from the ordinary test classpath. The owning
caller performs the external execution between those calls and supplies actual
executor attribution. The helper does not select a worker, synthesize findings,
start SBT or create a second request lifecycle.

Standalone verification explicitly loads the existing Textus AI public Component
factory in its test host, configures the selected local provider and resolves
ordinary SPI with CBD. This optional runtime classpath connection adds no CBD
product dependency or fallback. The same test helper owns the selected loopback
Ollama session only for explicit standalone verification, including readiness,
client termination and owned-process cleanup. The native validation/runtime
owners supply actual qualification evidence after compilation.
