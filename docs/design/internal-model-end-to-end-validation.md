---
status: authored-unvalidated
decision_scope: PHASE-10.7 / P107-STEP-01 / P107-S01-A / P107-IMPLEMENT-001
updated_at: 2026-10-04
---

# Internal-model End-to-end Validation Design

The [paired contract](../spec/internal-model-end-to-end-contract.md) fixes this
test-only validation boundary. Four new test programs compose existing owners
without changing production declarations or accepted predecessor semantics.
The parent owns validation, independent review, Git acceptance and transitions.
No passing run or external proof has been claimed by authoring these files.

## Cohesive test responsibilities

| Program | Responsibility |
| --- | --- |
| [InternalModelEndToEndFixture](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndFixture.scala) | Declare rich/all-eight source-backed inputs; merge only explicit identity/scope/reference lanes; invoke actual Phase 9 impact/diff constructors; reload independent caller evidence; reuse the existing owner-refresh fixture. |
| [InternalModelEndToEndSupport](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndSupport.scala) | Keep caller transport outside the project; start sequential isolated actual JVMs; retain process/output evidence under one explicit private run. |
| [InternalModelEndToEndProbe](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndProbe.scala) | Produce to terminal exit or consume the existing project; retain actual application in the consumer; compose explicit synthetic or separate external observations with the unchanged C owner. |
| [InternalModelEndToEndSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndSpec.scala) | Describe E1–E6 using grouped Given/When/Then semantic results, actual source effects and bounded generated variants. |

Each lifecycle runs only beneath
`target/internal-model-end-to-end/work/<explicit-run>/`. The shared legacy
factories allocate relative `target/` trees. Their invocation occurs only inside
producer/consumer child cwd directories already below the admitted run; their
unchanged temporary layouts therefore remain contained without widening roots
or changing those fixture owners. Work is intentionally retained for inspection.
The synthetic lifecycle helper has a one-minute child ceiling and terminates
only its own child when needed. External mode is reserved for a parent-owned
runtime session and has the separately explicit thirty-minute observation ceiling.

## Composition without a parallel semantic model

Rich fixture controls retain the same actual package graph, decision/open-issue
records, candidate/diff/review/human admission and two CML baselines.
The positive original source inventory is complete; the optional-absent snapshot
is not added to it. Its unchanged owning `InternalModelCmlChangeGateSpec` requires
`Incomplete`, lifecycle invalidation and no application plan for `MissingBaseline`
even for an optional snapshot. The positive harness uses the existing default
that omits this entry without synthesizing snapshot bytes or owner evidence.
Core fixture facts supply substantive nested Entity, Event, Structure, Classification, Workflow
and StateMachine data, plus Mono-Koto and Use Case flow/step values. Only declared
core identities/anchors are prefixed; source identities and sequence-key values
retain their source-owner meaning. Shared `e-usecase` uses the rich canonical
witness. Scope and snapshot/source keys are explicitly rebound, then actual
unchanged codecs/validators reconstruct the model and all eight DTOs.

Four explicitly declared direct impact categories pass through the Phase 9
impact projection owner. Their declared semantic identities must actually exist
in the admitted realization. The selected admitted rich patch, candidate, diff
entries, review and pending governance reference pass through the actual
candidate/diff integration owner. These inputs express test-owner evidence;
no graph facts are inferred from CML text, lint output, names or provider state.
Full ordinary typed result transport retains constructor fields and sidecars
for direct specification expectations, with no digest or management byte seal.

The accepted `refreshOwner` fixture adds its own core declarations when refreshing
a rich graph. Since this fixture already contains those declarations before
application, fixture normalization retains one semantically equal declaration
per explicit key after refresh. Model facts use `(kind, identity, anchor)`;
view records use `(kind, identity, role)`; realization arrays use their declared
IDs. A contradiction remains a fixture defect, never a choice of first/last
authority. This is ordinary test data composition, not a new production owner.
The refreshed source revisions/references remain independent explicit owner
declarations and the unchanged C owner verifies actual selected package/source
freshness, mapping retention and all-eight projection continuity.

## Independent caller and fresh admission

The producer writes `project/`, a separate `caller-input.json`, and ordinary
`producer.json` semantic/process evidence, then exits. The transferred project
uses the existing explicit `success` or `lint-failure` candidate-payload fixture
mode. This supplies actual applied ordinary CML for subsequent positive or
expected-negative Cozy execution without assigning a terminal outcome.
The transferred project
contains only admitted present package files, `project.yaml`, candidate-declared
`cml/alpha.cml` and `cml/beta.cml`, and the original declared CML source. A new
JVM reads that existing project; it does not run a producer factory against it.
Its environment is cleared and only its own HOME/TMPDIR are supplied; cwd/home/tmp
and actual PID differ from the terminal producer. Observed child keys must include
HOME/TMPDIR and may additionally include only the exact `__CF_USER_TEXT_ENCODING`
marker when `os.name` is exactly `Mac OS X`; other platforms permit no extra key.
No provider configuration, optional DB or session state is inherited or transferred.

Original and current independent caller selections remain distinct. The original
human input is used to admit original approval through the existing human owner;
current input is freshly evaluated by the rooted application gate. Current review
uses the independently supplied rule/provider basis. Separate ownership input
names the exact root/package/scope, source provenance, complete target set and
approved next source versions. No gate or application plan is a transported
capability. Direct producer/consumer semantic comparisons are specification
expectations and are not consulted to authorize application.

The consumer captures complete pre-application semantics and source/cursor
observations, then calls `applyApproved`. Well-formed refusals preserve the actual
gate and no native report; structured owner rejection retains its diagnostic.
Complete native application stays in that JVM until validation returns.
Physical effects and unchanged cursor are ordinary outcome observations, with
no rollback or acceptance machinery.

## Observation protocol and truthful proof level

Synthetic modes declare their observation origin explicitly. Success proves the
composed gate/native-write/refresh/post-validation behavior only; failure/missing
modes prove retained effects and explicit non-success. They cannot replace actual
Cozy evidence. All positive results remain `ReprojectedPendingAcceptance`.

External mode writes `ready.json` after actual native application. It exposes the
two exact observation paths and command bindings for parent execution, retaining
the ApplicationReport in memory rather than serializing/reconstructing it as
permission. A scheduled existence check coordinates the thirty-minute bounded
wait. On expiry, absent observations remain Missing values. Present observations
are decoded through the existing acceptance support transport and admitted by
the unchanged C owner; raw outcome and bound target/reference/argv values survive.
The resulting complete ordinary report is retained at `consumer-result.json`.

Step 02 separately selects/runs the actual registered Cozy lint commands and
value-model generation against those applied files. The probe executes neither
command group nor Git acceptance. No source/person authentication, provider
session, optional DB, public API/CLI/MCP, installed skill, latest selection,
revision allocator, retry/lock/backup/checksum or deployment is added.

## Acceptance ownership

E6 draws the existing bounded generator of private named `GeneratedVariant`
inputs four times into a fixed four-position tuple, then evaluates all four
actual producer/consumer pairs. Each complete variant retains one of the four
declared labels, one Boolean encounter order and a `43L` or `79L` carrier;
ScalaCheck's generic non-container fallback leaves its fields intact instead
of shrinking outside that domain. Outer tuple shrinking retains every position,
including both enforced explicit carrier/control-addition cases, with the
existing `minSuccessful(1)` batch and all semantic comparisons.

E1–E6 are authored executable obligations. The frozen parent selects the
representative `InternalModelEndToEndSpec` and its ten-suite affected accumulator,
mechanical source/document checks and diff verification. Those executions and
independent Step review are still required. Step 02 actual external validation
and later repository/Phase acceptance remain distinct; this adapter is test-only,
uninstalled and undeployed, with no automatic checklist or cursor closure.
