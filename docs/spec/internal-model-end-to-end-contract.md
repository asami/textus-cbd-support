---
status: authored-unvalidated
decision_scope: PHASE-10.7 / P107-STEP-01 / P107-S01-A / P107-IMPLEMENT-001
updated_at: 2026-10-04
---

# Internal-model End-to-end Validation Contract

This paired specification defines the test-only composed continuation,
application and re-analysis boundary. Its executable counterpart is
[InternalModelEndToEndSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndSpec.scala)
and its companion is the
[validation design](../design/internal-model-end-to-end-validation.md).
It changes no production contract, public API, schema, installed skill or CLI.
Authoring does not establish a passing validation, Step acceptance, P10-70
external proof, or Phase closure.

The existing [typed control contract](internal-model-typed-control-contract.md),
[continuation contract](internal-model-continuation-and-rehydration-contract.md),
[projection continuity contract](internal-model-projection-continuity-contract.md),
[candidate human approval contract](internal-model-candidate-human-approval-contract.md)
and [CML change contract](internal-model-cml-change-contract.md) retain their
authority. Canonical CML remains canonical; stored human claims, fixture
declarations, provider flags and projection DTOs supply no authentication or
mutation permission. [Repository rules](../rules/repository-rules.md) prohibit
content-derived management/control identity.

## Selected substantive semantic graph

The declared producer graph composes the existing rich two-target fixture with
the existing core all-eight fixture. The single selected Use Case remains
`e-usecase`. Core identities and their assertion, association, condition and
source-anchor links are explicitly prefixed with `post-`; the selected Use Case
is shared. Component/context scope and the exact model snapshot/source references
are explicitly rebound to the rich source-owner declaration. Display labels and
encounter order never supply identity or revision.

The separate `(element, opaque-shared)` and `(relationship, opaque-shared)`
candidate mappings retain their distinct canonical/enrichment lanes and bounded
conditions. The realization retains source attribution, association witnesses,
source-owned `step-1` and `flow-1` sequence keys, Workflow conflict detail, and
nullable DTO navigation. All eight actual existing constructor results must be
nonempty: Mono-Koto, Use Case, Entity, Event, Structure, Classification, Workflow
and StateMachine. Complete realization/binding sidecars accompany those DTOs.
Explicit Entity/Event/Workflow/StateMachine impact records and direct links pass
through `AnalysisDesignImpactProjection.create`; the admitted rich patch, semantic
diff, candidate and review pass through `CandidateDesignSemanticDiffIntegration.create`.
The Git governance reference is traceability only and remains pending acceptance.

## Terminal transfer and independent caller input

The producer exits before the consumer starts. Both are actual JVM processes
with distinct observed PIDs, separate cwd/home/tmp and a supplied environment
containing exactly their explicit `HOME` and `TMPDIR`. Observed child environments
must contain both supplied keys and no additional key, except the optional exact
`__CF_USER_TEXT_ENCODING` marker when `os.name` is exactly `Mac OS X`.
No provider configuration, optional DB, session state, producer object or AI
conversation is transferred. The retained work root is one
explicit run below `target/internal-model-end-to-end/work/`.
The producer takes an explicit ordinary candidate-payload mode, `success` or
`lint-failure`, through the existing acceptance fixture. The latter supplies
the already declared invalid alpha Entity attribute for the next Step's real
expected-negative lint run; it does not fabricate that command's outcome.

Only the admitted present package inventory, `project.yaml`, the two existing
candidate targets and the declared original CML source constitute the produced
project. Caller input stays at `caller-input.json` outside that project/package.
The positive original source inventory is complete and does not add the
optional-absent snapshot. The unchanged owning `InternalModelCmlChangeGateSpec`
correctly requires `Incomplete`, lifecycle invalidation and no application plan
for that snapshot's `MissingBaseline`, even when it is declared optional.
It independently declares original/current typed selections, rule/provider basis,
exact human input, accepted decision/mapping requirements, exact live-source
versions, mutation-owner provenance/targets/next versions, and application and
post-validation RecordReferences. No cached gate or plan is transported as a
permission token. Producer output records explicit project/input locators and PID.

The consumer does not recreate the consumed project or package. It reloads
package, snapshots, current review and original human evidence through existing
owners using separate caller selections/input. Pre-application semantic results
are directly compared in the executable specification with the producer's complete
results. Such result comparisons and ordinary payload Base64 serve evidence
transport, not a saved-body seal, applicability check or write capability.
Actual source/cursor effect observations are retained separately.

## Fresh native application and post-validation

Only fresh rooted `InternalModelCmlChangeApplication.applyApproved` admission can
authorize native replacement of the exact existing `cml/alpha.cml` and
`cml/beta.cml`. Missing or rejected current human input, wrong exact candidate
revision, or missing mutation authority cannot write, even when stored/provider
claims say approved. Explicit source-owner version drift yields re-review;
an explicitly missing version remains incomplete. Owner versions and human/source
authentication are never inferred from CML, lint output or stored approval.

The consumer retains the actual complete ApplicationReport in its JVM. It then
uses the existing independently declared `refreshOwner` fixture algorithm and
the unchanged post-validation/freshness/continuity owners. Repeated identical
core fixture declarations introduced by composing an already all-eight graph
are normalized to one declaration per exact semantic key; contradictory repeats
are a fixture defect. This introduces no semantic owner, parser or production
serializer. Refreshed realization/binding/source snapshot references and next
source versions remain explicit fixture-owner inputs.

Positive validation returns only `ReprojectedPendingAcceptance`, with all eight
nonempty constructor results, complete mapped lanes/conditions/sidecars, required
sources Unchanged and the original cursor unchanged. Failed or missing required
observations retain `Failed` or `Incomplete`, the actual application and physical
effects, and no successful continuity. There is no automatic Git acceptance,
cursor advance, revision allocation, rollback, retry, locking, backup or checksum.

## Synthetic and real observation boundaries

The grouped executable specification uses explicit `synthetic-success`,
`synthetic-failure` and `synthetic-missing` observations. These prove owner
composition and admission only. Synthetic stdout/stderr/exit values are labelled
as synthetic and cannot establish real Cozy execution or P10-70/P10-71 completion.

In `external` mode the same fresh consumer retains its actual ApplicationReport,
writes `ready.json` and waits for exact `alpha-cozy-observation.json` and
`beta-cozy-observation.json` locators below the run root. Readiness records its
actual PID, application reference, applied target paths, exact command references,
source authority/identity/approved next revision, runtime and lint argv.
Coordination waits at most 1,800 seconds; a missing observation at that ceiling
remains a Missing value. No external terminal result is fabricated or replaced
with a synthetic success.

Each external observation uses the existing closed test observation transport and
is admitted by the unchanged C owner. Required runtime is `0.3.3-SNAPSHOT` and
argv is exactly `--runtime 0.3.3-SNAPSHOT lint cml <absolute applied file> --format json`.
The parent owns the separate real command execution, its terminal/raw evidence,
and generation from the same applied ordinary CML through Cozy's
`modeler-scala-value <file> --save <dir>` surface. Generation evidence is separate
from the C owner's lint observation admission. The probe runs no Cozy, SBT, Git
acceptance, provider service, network operation, publication or deployment.

## Executable obligations

| Scenario | Semantic obligation |
| --- | --- |
| E1 | Terminal producer/fresh consumer preserve complete pre-application realization, all eight DTOs, actual Phase 9 impacts and rich candidate/diff/review/decision/mapping facts without source/cursor mutation. |
| E2 | Fresh independent human/owner input alone permits two-file replacement; synthetic success remains pending acceptance with complete sidecars and unchanged cursor. |
| E3 | Missing/rejected human input, wrong candidate revision and missing ownership retain no-write refusal despite provider/stored approval claims. |
| E4 | Explicit owner-version drift or missing revision retains re-review/incomplete and no application plan or write. |
| E5 | Failed/missing required command after actual application retains effects and failed/incomplete state without successful continuity. |
| E6 | A fixed four-position ScalaCheck tuple of bounded `GeneratedVariant` inputs across labels/order and explicit carrier/control-only additions preserves exact semantic selection and human approval meaning; shrinking retains each complete variant and all four actual process pairs. |

Each private named `GeneratedVariant` keeps one of the four declared labels,
one Boolean encounter order and a carrier from `43L` or `79L`. ScalaCheck's
generic non-container fallback does not shrink its fields beyond that domain.
Four explicit draws still form the outer fixed four-position tuple, and the
existing `minSuccessful(1)` batch executes four actual producer/consumer pairs
with all semantic comparisons retained.

Parent-selected compilation, focused representative/accumulator validation and
independent review remain outstanding. Real Cozy positive/negative/generation
execution belongs to P107-STEP-02. The adapter is repository-private test code,
uninstalled and undeployed; no public application surface or terminal Phase
acceptance is claimed.
