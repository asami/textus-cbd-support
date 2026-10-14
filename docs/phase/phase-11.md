# Phase 11 - Capability Ingestion, Catalog, and Queries

status=in-progress
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=none
validation_ownership=aggregate-deferred
aggregate_validation_owner=PHASE-11.1
aggregate_validation_sequence=["PHASE-11","PHASE-11.1"]
depends_on=phase-10.7.md
successor=phase-11.1.md

Stage Status:
- Current status: OPEN
- Predecessor: Phase 10.7
- Successor: Phase 11.1
- Development item: DEV-CBD-003
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-11-checklist.md` with reproducible evidence.

## Purpose

Consume the CNCF Component Capability projection and make Capability
discoverable, comparable, and traceable through Textus CBD Support. Capability
remains a non-instantiated model IR; Phase 11 ingests and queries its semantic
contract without executing it or reconstructing CML. It retains the original
Phase identity as the first unit of the applied `11 -> 11.1` split.

Phase 11 also owns the read-only demand-to-realization trace from an admitted
Use Case/Application Capability demand to Component Capability and its explicit
Operation/Workflow/StateMachine realization. It does not judge Use Case quality,
create Use Case Slice structure, or edit requirements; those responsibilities
belong to Phase 12. Capability views, public Web/MCP delivery and the original
real producer/consumer end-to-end acceptance belong to
[Phase 11.1](phase-11.1.md).

## Work stack

| ID | Outcome | Status |
| --- | --- | --- |
| CAP-11-01 | Freeze supported projection versions, ingestion, integrity, and historical/current snapshot semantics. | closed |
| CAP-11-02 | Extend internal catalog/index models for Capability, demand, realization, Specification, and Evidence traceability. | closed |
| CAP-11-03 | Provide Capability search, provider matching, demand tracing, and typed gap/compatibility diagnostics. | planned |

CAP-11-04 and CAP-11-05 are owned exactly once by the
[Phase 11.1 checklist](phase-11.1-checklist.md); their original IDs are retained.

## Local execution preparation

The GitHub refinement through `a3c3de2` is incorporated. CAP-11-01 private
ingestion and actual normal producer/public Factory interoperability are accepted;
see [CAP-11-01 acceptance](#cap-11-01-acceptance--2026-10-15).
CAP-11-02 catalog is independently accepted; see
[CAP-11-02 acceptance](#cap-11-02-acceptance--2026-10-15).
CAP-11-03 remains planned and not started.
The canonical Phase 10.6 and Phase 10.7 ledgers are CLOSED;
consume the accepted [Phase 10.7 handoff](phase-10.7.md). Use the
[local preparation note](../notes/phase-11-12-local-execution-preparation.md)
for existing integration points and evidence still to be obtained.

CAP-11-01's frozen [consumer contract](../spec/capability-ingestion-contract.md)
and [ingestion design](../design/capability-ingestion.md) record supported versions,
strict separate Application input, public CNCF discovery, complete selection,
current/history transitions and the later catalog/query handoff. The private
kernel and real normal producer interoperability have completed focused validation
and independent whole-Step acceptance with mechanical naming closure.
CAP-11-01–03 are this Phase's ordered Step boundaries;
CAP-11-04–05 follow only in Phase 11.1 after this Phase closes. Size Slices
before implementation under the current workflow and model policy.
The existing CAR Review capability taxonomy is not the upstream Component
Capability model and cannot supply its identities or demand relationships.

### CAP-11-01 acceptance — 2026-10-15

CAP-11-01 is accepted after focused validation, independent whole-Step review
and mechanical naming closure. Supported independent versions and exact provenance,
complete separate Application input and current/history semantics, and actual normal
Cozy -> generated Scala 3 native -> public Directory/CAR Factory -> source-free CBD
intake are accepted. Intake retains the original native objects and rejects failed
input without fallback. Forward UseCase demand remains explicitly not-yet-admitted;
its positive path is unexercised. All seven CAP-11-01 checklist criteria are DONE.

Phase 11 remains in-progress / OPEN; CAP-11-02/03 remain unstarted. Participant
CAP-78-05/CAP-64-05 acceptance, participant Phase closures/full validation/reviews/
releases, eventual authorized Cozy main integration and CBD Phase review/release
remain separate. CBD full-suite validation is deferred-not-run to Phase 11.1.
Phase 14 failed Job behavior remains unaccepted. No commit is claimed here.

The dated 2026-10-14 authoring and validation-ownership sections below preserve
historical pending/unchecked states. This acceptance supersedes those states only
for current CAP-11-01 status; their contracts and other owners remain unchanged.

### CAP-11-02 acceptance — 2026-10-15

Stage Status:
- Current status: DONE
- Owner: CBD Support catalog/model owner
- Update rule: acceptance is recorded against the
  [original eight criteria](phase-11-checklist.md#cap-11-02-internal-catalog-and-demandrealization-traceability-model).

CAP-11-02 is accepted after focused validation and independent whole-Step review.
The local Step commit binds this acceptance. The
[catalog contract](../spec/capability-catalog-contract.md) and
[catalog design](../design/capability-catalog.md) record exact typed qualified
Provided/Required records and their independent provenance, complete separate
Application projections, and attributed Required paths with preserved demand/
provider/realization cardinalities. Operation, Workflow and StateMachine remain
three explicit declaration-owned realization kinds. Specification and Evidence
remain separate, while Availability, Authorization, Guard and execution result
retain four distinct Unknown observation domains. The strict portable codec and
validator preserve semantic identity and attribution; the live catalog retains
the original native objects from the selected accepted Snapshot.

Phase 11 remains in-progress / OPEN; CAP-11-03 has not started. The unavailable
forward UseCase positive branch remains unadmitted. Aggregate full-suite
validation remains deferred-not-run to Phase 11.1. Participant own-Phase full
tests, full reviews and releases, authorized eventual Cozy main integration,
and CBD Phase review/release remain pending. Phase 14 transferred Job failed
behavior remains unaccepted.

The preceding dated CAP-11-01 acceptance's pending participant and unstarted
catalog statements are historical before this CAP-11-02 acceptance. They are
superseded only by later independently accepted Steps within their own ownership;
this acceptance establishes no retrospective acceptance of other owners. The
original dated authoring and validation-ownership bodies below remain historical
and their contracts remain unchanged.

### CAP-11-01 consumer contract authoring — 2026-10-14

S01-consumer-contract records the sixteen settled consumer decisions against
accepted CNCF CAP-78-01–04 and Cozy CAP-64-01–04 contracts, plus authored
Cozy CAP-64-05 normal producer/publication and accepted bounded repair conformance.
The [contract inventory](../spec/capability-ingestion-contract.md#actual-upstream-sources-and-fixture-availability--c11i-d14)
records exact selected checkout revisions, independent producer/build/schema/ABI
domains and real CML fixture availability. Forward Use Case demand remains
UnavailableUpstreamProjection, not-yet-admitted; authored Application demand
links do not supply its inverse.

Actual Scala 3/native accessor compilation, real canonical package Factory
discovery and CBD ingestion without CML reparsing remain pending. Contract
authoring establishes no executable coverage or Step acceptance: all seven
CAP-11-01 and four Cozy CAP-64-05 criteria remain unchecked. Separate participant
Step/Phase closure, full validation/review/local release, authorized Cozy main
integration and this original Phase's own review/release remain owed. CAP-11-02/03
remain OPEN and unstarted; CBD full-suite validation remains deferred-not-run
to Phase 11.1. The applied split and dated planning history below are retained.

### CAP-11-01 ingestion kernel authoring — 2026-10-14

S02-ingestion-kernel authors nine private runtime sources, two synthetic fixture
sources and three executable suites against the frozen S01 contract. The
[kernel API/case map](../spec/capability-ingestion-contract.md#s02-ingestion-kernel-authoring--2026-10-14)
records complete strict Application transport/finite Local binding, public
Factory intake, exact expected/observed provenance and immutable current/history.
C11A01–11, C11K01–10 and C11S01–07 use meaningful Given/When/Then and should
clauses, including both active properties configured for at least 100 successes
and zero discards. Parent focused validation and independent acceptance remain
pending; authoring supplies no executed-result claim.

The handcrafted canonical package/native Operation and full Application inputs
are synthetic kernel conformance. Actual normal Cozy modeler production,
generated Scala 3 accessor/native observations, canonical Directory/CAR public
Factory discovery and CBD producer consumption without CML reparsing remain
mandatory later same-Step work. All seven CAP-11-01 and four Cozy CAP-64-05
criteria remain unchecked. CAP-11-02/03 and all successors remain unstarted;
CBD full-suite validation stays deferred-not-run to Phase 11.1. Participant
closures/main integration and original CBD review/release remain owed.

### CAP-11-01 validation ownership clarification — 2026-10-14

Phase 11 retains complete configured Compile/Test source compilation, including
preserved transferred Job sources/specifications, and the original Capability,
typed-control and output-exclusion focused behavior. Actual normal Cozy production,
generated Scala 3/native observations, Directory/CAR public CNCF Factory discovery
and CBD ingestion without CML reparsing, with full Application roundtrip, remain
required. All CAP-11-01–03 criteria, separate CNCF Phase 78/Cozy Phase 64 closures
and authorized Cozy main integration remain owed; CBD full-suite validation stays
deferred-not-run to Phase 11.1.

Transferred CAR Review Job/Provider/Entity execution and correction remain owned
by [Phase 14](phase-14.md) / DEV-CBD-004 and unaccepted. Their native execution
success is not an additional CAP-11-01 criterion. Preserve transferred sources,
specifications and failed evidence; this ownership clarification waives no failure
and supplies no validation or acceptance result.

### CAP-11-01 real producer harness authoring — 2026-10-14

S03-real-producer-interoperability authors the
[Test harness and C11R01–08 case map](../spec/capability-ingestion-contract.md#s03-real-producer-interoperability-authoring--2026-10-14).
The explicit Test-only `textus.cbd.capabilityProducerRoot` selects the dedicated
Cozy producer independently of ordinary service generation. Four bounded normal
CLI invocations each select exactly one original CML; six real generated
Store/Archive/Remote First/Second accessors enter configured Scala 3 Test compile.
Independent public native definitions and full separate Application products flow
through actual canonical Directory/normal CAR public Factory intake to CBD with
source-free consumer fixtures. Original procedures stay metadata; Component
activation and CML reparsing are absent from intake.

The positive primary selection is Store+Remote. Archive+Remote retains true
Provided Present(empty); Store+Archive+Remote preserves both definition witnesses
and rejects the complete primary Local mapping as Ambiguous. First/Second current/
history and stale/original native failure scenarios retain explicit independent
header, definition/edge, native and local record domains. No combined Application
or unavailable forward UseCase success is inferred.

These are authored fixture commands/specifications, not executed results. Parent
focused validation and independent acceptance remain pending. All seven CAP-11-01
criteria remain unchecked; CAP-11-02/03 remain OPEN/unstarted. Original CNCF Phase
78/Cozy Phase 64 own Step/Phase validation/reviews/local releases and authorized
eventual Cozy main integration remain separate. This original Phase retains its
own review/release and aggregate full-suite deferral to Phase 11.1. The applied
split, authority, dated history and Phase 14 failed-work ownership are unchanged.

## Required upstream projections

Phase 11 consumes upstream-owned identifiers and relationships; it does not
invent replacements. The consumer contract must admit, when available:

- CNCF Component Capability projection: provided/required Capability identity,
  Component/package/source identity, ABI/version, explicit compatibility and
  realization references.
- Application Capability projection: Application Capability identity and
  explicit relation to required Component Capability identities.
- Use Case demand projection: Use Case identity and explicit required
  Application Capability identities. If this projection is not yet admitted,
  the Use Case portion remains `UnavailableUpstreamProjection`; CBD Support
  must not infer it from names or descriptions.

Record producer, projection/schema version, source revision, scope, and actual
fixture availability separately for each projection. The real Cozy-generated,
CNCF-admitted Component Capability fixture is required for CAP-11-05 closure
in Phase 11.1. Phase 11's implementation acceptance also needs the admitted
upstream contract and producer input for its selected ingestion/query boundary;
planning alone or a consumer-local substitute cannot supply that evidence.
Application/Use Case demand availability is an explicit admission decision:
when supported, prove the positive trace and gap cases; when unavailable, prove
the typed unavailable branch and record the unexercised positive path. Neither
synthetic fixtures nor an unavailable result prove a real upstream demand trace.

## Internal read model

Implementation should introduce package-private/domain values equivalent to the
following roles; exact Scala names may follow repository conventions:

```text
CapabilityCatalogSnapshot
CapabilityRecord
  - capabilityRef
  - direction: PROVIDED | REQUIRED
  - componentRef
  - packageRef
  - sourceRef
  - abiRef/version
  - compatibilityRefs

CapabilityDemandTrace
  - useCaseRef?                 // only from admitted projection
  - applicationCapabilityRef
  - requiredComponentCapabilityRef
  - providerComponentRefs
  - realizationRefs
  - specificationRefs
  - evidenceRefs

CapabilityRealizationRef
  - kind: OPERATION | WORKFLOW | STATE_MACHINE
  - targetRef
  - source/version

CapabilityGap
  - MissingApplicationCapabilityProjection
  - MissingRequiredComponentCapability
  - NoProvider
  - IncompatibleProviderVersion
  - MissingRealization
  - MissingSpecification
  - MissingEvidence
```

A gap is a diagnostic over admitted facts, not a new model fact. Runtime
Availability, Authorization and Guard results are separate diagnostics and
must never be collapsed into `NoProvider` or `MissingRealization`.

The field sketch describes one trace path, not a flattened set of interchangeable
endpoints. Preserve each explicit demand/provider/realization edge, its scope,
source and revision across multiple paths; do not form a Cartesian product from
independent reference lists. Distinguish unsupported, unavailable, unauthorized,
stale and ambiguous input from an admitted empty relationship. `NoProvider`
means no provider in the declared successfully queried catalog scope, not
universal nonexistence. Failed source acquisition cannot establish that result.

## Query/service contract

The catalog/query boundary should support deterministic operations equivalent
to:

```text
getCapability(ref)
findProviders(requiredCapabilityRef)
findRequirements(componentRef)
traceApplicationCapability(applicationCapabilityRef)
traceUseCaseDemand(useCaseRef)
diagnoseCapabilityDemand(demandRef)
```

All identity-bearing inputs are qualified typed references. Display-name or
description similarity may be used only as UI search assistance and must not
establish semantic matching, compatibility, provider selection, or trace links.

`traceUseCaseDemand` returns an explicit unsupported/unavailable result when
the upstream Use Case demand projection is absent. It must not reconstruct the
relationship from CML, Operation names, Event Storming text, or AI output.

## Ownership boundary

- Consume only CNCF/admitted upstream Capability and demand projections and versions.
- Preserve exact Use Case, Application Capability, Component, package, source,
  Capability, realization, and ABI identity.
- Keep current selected snapshots distinct from historical attempts.
- Keep Capability, demand, Availability, Authorization, Guard, realization,
  Specification, and execution Evidence as separate concepts.
- Do not parse CML, define Capability syntax, execute Capability, evaluate Use
  Case quality, create Use Case Slice structure, or replace upstream identity
  with cbd-support-local identity.

## Completion conditions

- Design/specification define supported versions, ingestion integrity,
  snapshot/history behavior, demand trace semantics, queries, and typed
  failure/gap diagnostics.
- Executable specifications cover valid ingestion and rejection/quarantine of
  ambiguous, duplicate, malformed, stale, and incompatible projections.
- Search finds exact provided/required Capability identities, providers,
  requirements, and gaps without display-name inference.
- When upstream demand is admitted, an admitted Use Case/Application Capability
  demand can be traced through required Component Capability to explicit provider
  and realization links. Otherwise the recorded unavailable branch is verified
  without claiming the positive upstream integration passed.
- Missing upstream demand projection, provider, compatible version,
  realization, Specification, and Evidence produce distinct deterministic
  outcomes.
- The accepted handoff records exact producer/schema/source revisions, fixture
  availability, typed reference/result/gap variants, query scope, public
  presentation/authorization mapping requirements and executable evidence.
- All Steps have focused validation and independent acceptance review; the
  Phase has its own comprehensive review, closure ledger and release commit.
  The repository full suite is explicitly deferred to Phase 11.1.

## Validation and successor handoff

This serial split uses `final-only` / `sbt-full-suite` in this repository.
Phase 11 retains focused compile/behavior and affected-consumer accumulator
validation, independent Step and Phase reviews, Step commits and its own
release commit. Its closure records `repository_full_suite=deferred-not-run`
with the named final owner Phase 11.1; it does not claim full-suite assurance.

Phase 11.1 verifies this committed deferred-validation handoff and accepted
ancestry, then runs the repository full suite once on its final release tree
for both members. CAR lint and all other applicable closure obligations remain.
The split itself runs only static documentation checks and starts no Phase.

Freeze the complete ingestion/schema/query/result contract as an internal
Step handoff before presentation implementation. It must settle exact identity,
source/version and compatibility, edge provenance/cardinality, supported versus
unavailable demand, typed query failures/gaps, and Web/MCP presentation and
authorization mapping expectations. This lets Phase 11.1 implement shared
read-only surfaces without rediscovering consumer semantics. An actual change
to that contract requires reassessment, not a silent reinterpretation.

## Applied split — 2026-10-04

Apply-mode invocation: `$cncf-split-phase 11` in the same task as the original
`$cncf-goal-phase 11` entry. Source goal/status:
`none / no-goal-pre-entry`. All five original items were unstarted; there are
no accepted Steps, Slices or product commits to move. Existing dated planning
and independent Phase history are preserved.

### Dated pre-split gate evidence

The typed entry `phase11-entry-20261004-01@1` returned `SPLIT_REQUIRED` with
`split_reasons: [time-bound]`: 720 expected minutes, a 600–960 minute uncertainty
range and 960-minute conservative upper bound, against a 360-minute target and
480-minute expected ceiling. This is a workload estimate, not measured duration;
no comparable planned/actual calibration was available. It excludes upstream
implementation and wait time. The critical path is upstream admission ->
ingestion -> reference/catalog model -> queries/gaps -> views -> Web/MCP ->
real-fixture acceptance -> review, validation and release.

### Partition and profile allocation

| Phase | Owned items | Expected / range | Parent profile | Cost role |
| --- | --- | --- | --- | --- |
| 11 | CAP-11-01–03 | 390 / 330–480 minutes | gpt-6.1-sol / high | Interacting identity/schema/query decisions and their coherent implementation |
| 11.1 | CAP-11-04–05 | 390 / 330–480 minutes | gpt-6.1-sol / medium, if the handoff is settled | Shared views, delivery and acceptance over the frozen query contract |

The first Phase owns the expensive reasoning kernel as an internal contract
Step: supported upstream identity/version/schema and query/availability
semantics. Its ingestion/model/query implementation stays with that decision
because those executable specifications establish the handoff. The successor
consumes that exact handoff; runtime suitability is assessed in its execution
task. No child profile is inferred from this planning task's runtime.

Each unit is 30 minutes above the six-hour target and 90 minutes below the
eight-hour expected ceiling. Neither is below five or four hours, so short-child
rebalance/merge exceptions do not apply. Recombining them gives the original
720-minute estimate, above 480; profile cost is not the rejection reason.
Extra handoff/review/closure work is estimated at about 60 minutes in total,
with no extra full-suite run under `final-only`. No numeric price or savings
claim is made; lower-cost settled work can use medium after the high-profile
contract handoff. Confidence remains low.

The order is `10.7 -> 11 -> 11.1 -> 12 -> 13`; Phase 12 and Phase 13 identities
are unchanged. This is a time-bound packing split, not a new human, external,
migration, release-authority or approval boundary.
`incoming_semantic_handoffs_per_child: {"PHASE-11": [], "PHASE-11.1": []}`.
The ordinary accepted-contract/profile handoff is defined above and is not
classified as a genuine semantic split gate.

Split-planning capability: `PROCEED_UNVERIFIED`, `bounded-settled`, standard
parent mode. The mandatory task-profile resolver was attempted with task ID
`/root`; the installed helper rejected `resolve-entry-task-profile` as an
unsupported subcommand. No current model/effort evidence was inferred. Applying
this settled documentation partition does not verify a future execution task.
No child numbering collision was found; the prior proposal's `11.1/11.2`
labels are corrected to the required retained-source sequence `11/11.1`.

## Phase Plan Gate: PROCEED

- target: 360-minute expected target; 480-minute expected ceiling
- estimate_calibration: no comparable planned/actual evidence; workload estimate
- planning_demand: protected-decision
- recommended_parent_profile: gpt-6.1-sol / high
- profile_cost_role: expensive reasoning kernel
- expensive_reasoning_kernel: upstream identity/schema/version admission and typed query/availability contract, as one internal Step boundary
- frozen_profile_transition_handoff: accepted Phase 10.7 release; upstream contracts and producer evidence admitted by CAP-11-01
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: 390 minutes expected; 330–480 uncertainty range; 30 above target, 90 below ceiling
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: combined source estimate 720 minutes exceeds 480; no profile-cost rejection
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: about 60 minutes additional sequence handoff/review/closure; one final full suite; no quantified cost saving
- agent_reasoning_mode_policy: default standard; consider pro only at an eligible agent launch with supported, quality-first evidence
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 11

## Non-goals

- Capability source authoring or CML parsing.
- CNCF Capability contract or ABI ownership.
- Use Case quality/coverage review, Actor Goal review, Use Case Slice creation,
  Executable Specification ownership, or ticket management.
- Runtime Capability execution, Authorization decisions, or Guard evaluation.
- AI-based semantic merging of unrelated Capability identities.
- Management hashes or content-derived identity, revision, approval or dependency
  control; apply [repository rules](../rules/repository-rules.md).
- Closing CNCF Phase 78 or Cozy Phase 64 from this repository.
- Capability views/public delivery or CAP-11-05 closure, owned by Phase 11.1.

## References

- [Development note](../notes/capability-catalog-and-traceability-proposal.md)
- [Development item journal](../journal/2026/09/2026-09-16-capability-catalog-development-item.md)
- [Phase 11 Checklist](phase-11-checklist.md)
- [Phase 11.1](phase-11.1.md)
- [Phase 12](phase-12.md)
