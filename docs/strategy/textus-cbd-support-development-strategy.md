# Textus CBD Support Development Strategy

## Objective

Provide a dedicated CAR and development-support tooling whose MCP, Web, CLI,
and report surfaces help generative AI and developers perform Component-Based
Development without coupling component catalog semantics to SIE's BoK
knowledge model.

## Development Philosophy

- Keep SIE terminology/component-existence knowledge separate from CBD detail.
- Read authoritative Cozy-generated catalog and model-metadata documents.
- Return evidence and explicit absence; never synthesize catalog facts.
- Keep the default simplemodeling.org catalog usable without SIE.
- Allow configured catalogs without merging their identities silently.
- Preserve a last known good snapshot when refresh fails.
- Publish read-only CBD operations through MCP and keep administration private.
- Keep CBD Web, CLI, MCP, and report projections on one application contract.
- Lead CAR Review in CBD Support and admit Cozy, `sbt-cozy`, CNCF, SIE,
  catalog, runtime, and AI capabilities through attributable versioned
  providers.
- Use `sbt-cozy` as the local and CI/CD bridge without moving Review policy into
  the plugin or changing publication tasks implicitly.
- Keep project identity, Scala version, dependencies, and runtime compatibility
  authoritative in `project.yaml`; keep `build.sbt` declarative and small.

## Phase Overview

### Phase 1: CBD Support Extraction Baseline

Create the CAR, catalog provider, shared reference contract, MCP publication
policy, SIE reference-only boundary, documentation, and verification baseline.

### Phase 2: Catalog Fidelity and Resolution

Expand catalog schema coverage, version selection, dependency graph
resolution, conflict reporting, source authorization, caching, and refresh
observability using real published catalogs.

### Phase 3: Federated Development Context and AI Ergonomics

Add simplemodeling.org, configured BoK sites, SIE-mediated BoK knowledge,
configured development directories, and CAR versions in the local warehouse
and managed cache as distinct evidence-bearing input sources. Reconcile their
component and version observations without silently merging source identity,
then improve requirement matching, intent-aware usage guidance, evidence
citation, and Codex integration without turning inferred advice into catalog
fact.

### Phase 4: Runtime Hardening

Add authentication, bounded caching, production refresh policy, SAR composition
tests, compatibility governance, and release/publish evidence.

### Phase 5: CBD-Led CAR Review Platform

Develop CAR Review under CBD Support ownership. Add generic Review Providers,
canonical Review Reports and Review Runs, Cozy analysis, `sbt-cozy` CI/CD
integration, Web UI, user-facing CLI, authorized read-only MCP report queries,
quality-capability views, optional AI/runtime evidence, and reproducible
cross-repository verification without publishing automatically. AI Review
reuses Textus AI's provider-neutral `AiRunner`, structured `generateRecord`,
purpose profiles, runtime adapters, and its Phase 1 execution-fact,
confidentiality, deterministic-provider, and lifecycle contracts while CBD
Support retains Review policy, Evidence admission, canonical reporting, and
gate ownership.

### Phase 7: Action-Local Runtime Isolation

Ensure that a cached CBD runtime retains no ActionCall-local state and that
each action retains its own admitted local resource-tree inventory. Complete
the explicit configuration handoff in standalone and composed CBD/SIE SAR
harnesses, then prove isolation with deterministic interleaving and concurrent
ActionCall specifications.

### Phase 8: Review Delivery, CI/CD, and Quality Rule Execution

Operationalize the completed CAR Review foundation as a decision-support
product. Add a canonical-report-driven Web dashboard and item diagnosis,
Markdown and PDF report generation, CI/CD artifacts and gate integration, and
concrete attributable checks for each quality attribute. Persist diagnosis
results by CAR identity and content/configuration fingerprint so equivalent
work is reused rather than rerun, and visualize the CAR's evolution from its
retained diagnostic history. Keep all renderers, diagnoses, CI results, and
quality conclusions tied to the same canonical Evidence, Observation,
capability, and limitation identities; no surface may rerun a rule or invent a
conclusion.

### Phase 9: Evidence-Backed Component Composition and Dashboard

Turn application intent and attributable CAR/SAR component evidence into a
reviewable composition plan, while making each exact Component understandable
through an evidence-backed Dashboard. Establish one Canonical Component Design
Model projected into stakeholder-facing Mono-Koto/Use Case views and engineering
Entity/Event/Structure/Classification/Workflow/StateMachine views. Composition
retains alternatives, gaps, limitations, provider suggestions, and human
decisions without a hidden winner. Analysis-view feedback can become a traced
candidate design and semantic diff, but Phase 9 keeps the working state
transient and Git-governed rather than introducing a second durable design
source.

### Phase 10 sequence: Durable Model Development and Continuation

Phase 10 was applied as the ordered sequence `10`, `10.1`, `10.2`,
`10.3`, `10.4`, `10.5`, `10.6`, and `10.7` after its verified
pre-goal entry gate reported `SPLIT_REQUIRED` for the original 48-hour
scope. The children retain DEV-CBD-002 but independently close around the
six-hour target and eight-hour ceiling. Each successor consumes its
predecessor's accepted release contract; no ordinary local commit is a
semantic handoff.

| Phase | Focus | Recommended parent profile |
| --- | --- | --- |
| 10 | package identity, manifest, and integrity | Terra/xhigh |
| 10.1 | source snapshots and freshness | Terra/high |
| 10.2 | durable semantic state and traceability | Terra/xhigh |
| 10.3 | candidate projection, review, and approval | Terra/xhigh |
| 10.4 | continuation and rehydration | Terra/high |
| 10.5 | drift and exact CML change gate | Terra/xhigh |
| 10.6 | retained-state, API, and security integration | Terra/xhigh |
| 10.7 | end-to-end validation and closure | Terra/high |

## Current Priority

### Capability sequence position — 2026-10-16

[Phase 11](../phase/phase-11.md#current-phase-closure-and-successor-handoff--2026-10-16)
is closed after four independently accepted Step commits, the current epoch2
whole-Phase review and its local release. Normal producer/acquisition/consumer
focused validation passed 102 tests in 12 suites. Cozy Phase 64 is released and
integrated into its canonical main; CNCF Phase 78 is released on its dedicated
branch. Their separate full-validation results remain separate from CBD's
deferred full validation.

DEV-CBD-003 is closed by Phase 11.1's CAP-11-04–05 acceptance, shared read-only
Capability views, consistent native Web/MCP/API surfaces and aggregate full-SBT
validation for `PHASE-11 -> PHASE-11.1`. Its distinct local release retains
the sole full Phase review and accepted focused closure repairs. The accepted
ComponentUsage contract preserves explicit owners/evidence and distinct
Component contracts; forward UseCase positive demand remains unavailable.
See the [Phase 11.1 closure](../phase/phase-11.1.md#current-phase-closure-and-successor-handoff--2026-10-16).
The owner selected Phase 14 as the next execution boundary on 2026-10-16,
before Phases 12 and 13, to finish the existing transferred Job work in the
canonical CBD Support checkout while preserving its uncommitted work.
Phase 14 is closed by CBD-owned native Job integration, independent acceptance
and verified local release. Its source-mode development and unsupported
started-work recovery limits remain explicit in the Phase closure.
Phases 12 and 13 remain planned. The dated sections below retain their original
planning position; Phase numbers and completed histories are unchanged.

### CAR Review Job handoff ownership (2026-10-07)

[Phase 14](../phase/phase-14.md), CAR Review Native Job Integration and
Acceptance, owns DEV-CBD-004: the unvalidated CBD implementation transferred
from CNCF Phase 69.7. Its [checklist](../phase/phase-14-checklist.md) covers
source/generated-model build connection, explicit Review identity and canonical
Provider input, native execution/cancellation, production StartReview and durable
Review-to-Job binding, writer exit and independently fresh caller-isolated reads,
then its own validation, independent review and local release.

The roadmap is extended to `10.7 -> 11 -> 11.1 -> 12 -> 13 -> 14`; existing
Phase numbers and completion histories are preserved. Phase 14 is PLANNED with
transferred source present but no CBD-owned acceptance. This addition starts no
execution and does not resume or expand the Phase 11 Goal. The
[handoff](../journal/2026/10/2026-10-06-cncf-job-cbd-scope-separation-handoff.md)
retains original authoring/results/failures; CNCF closure remains separately
owned. Phase 14 owns its full validation rather than consuming the Phase 11.1
Capability-only aggregate deferral. Calibrate workload and apply ordinary entry
and splitting gates before executing this Phase.

### Applied Capability split and current position (2026-10-04)

The canonical Phase 10.6 and Phase 10.7 records are CLOSED. DEV-CBD-003 is
planned as the serial sequence below after `$cncf-split-phase 11`; no product
work or child execution is accepted by this planning operation.

| Phase | Scope | Expected duration / parent profile |
| --- | --- | --- |
| [11](../phase/phase-11.md) | CAP-11-01–03: upstream admission, ingestion, typed catalog and queries | 390 minutes (330–480); gpt-6.1-sol / high |
| [11.1](../phase/phase-11.1.md) | CAP-11-04–05: shared views, Web/MCP and real producer/consumer acceptance | 390 minutes (330–480); gpt-6.1-sol / medium if the query/presentation handoff is settled |

The 2026-10-04 sequence was `10.7 -> 11 -> 11.1 -> 12 -> 13`; the
2026-10-07 addition above appends Phase 14 without changing those identities.
Full validation is `final-only` / `sbt-full-suite`, owned by
Phase 11.1 for both Capability units. Each unit retains focused validation,
independent Step/Phase review, closure ledger and release commit. See the
[dated split evidence](../phase/phase-11.md#applied-split--2026-10-04) for the
pre-split 720-minute estimate, calibration limits, overhead, scope ownership
and ordinary accepted-contract/profile handoff. No genuine semantic split
gate or quantified cost saving is claimed. Future tasks reassess runtime.

Real upstream contracts and producer fixtures remain execution prerequisites;
the inspected CNCF Phase 78 and Cozy Phase 64 ledgers were planned/unchecked.
This planning operation does not admit source changes in those repositories.

### Historical repository-sync planning reconciliation (2026-10-03)

The following dated snapshot preserves its original planning position. Use
the 2026-10-04 section above and canonical checklists for current state.

The current Phase/checklist ledgers preserve the accepted Phase 9 and Phase 10
through 10.5 boundaries. Phase 10.6 and 10.7 remain not started. The historical
planning narrative below is retained, not a replacement for those ledgers.

After the Phase 10 sequence, [Phase 11](../phase/phase-11.md) remains Capability
Catalog, Discovery, and Traceability (DEV-CBD-003). The independent remote
Interactive View and Model Editing plan is retained as
[Phase 12](../phase/phase-12.md), also not started. New Actor Goal, terminology,
Event Storming, Aggregate/View, Designed/Observed navigation, and Workflow
requirements are retained in
[post-Phase 9 follow-up planning](../phase/phase-9-projection-followup.md), with
an unchecked ledger and explicit upstream dependencies. They do not reopen or
retroactively expand completed Phase 9. Phase 12 consumes these additional
contracts only after their own implementation and acceptance evidence exists.

The 2026-10-03 GitHub refinements add read-only Use Case/Application Capability
demand traces to Phase 11 and explicit Use Case Slice/Specification/Ticket
editing and Review to Phase 12. Their
[local preparation](../notes/phase-11-12-local-execution-preparation.md) retains
that order, upstream admission and the Phase 10 approval/continuation boundary;
neither refinement changes the current unstarted status or accepts new behavior.

### Historical Phase 9 / Phase 10 planning position

Phases 1 through 9 are complete. Phase 9 established the semantic,
projection, candidate-design, and semantic-diff contracts consumed by planned
Phase 10. Its Stage 9.7 cross-view feedback and evidence-integration work
(P9-60 through P9-63), plus the Stage 9.8 validation, documentation, and
deferred-work accounting work (P9-70 through P9-72), are accepted. P9-73's
full-review finding was repaired, focused re-review is clean, and the final CAR
lint and full SBT suite passed. The Phase 10 sequence is planned under
DEV-CBD-002 and must not pull durability or approval-lifecycle concerns back
into the closed Phase 9 boundary.

The detailed historical completion evidence for Phases 1 through 8 remains in
their phase ledgers and journals. Phase 9 and the Phase 10 sequence planning
are authoritative in `docs/phase/phase-9.md`,
`docs/phase/phase-9-checklist.md`, and the ordered
`docs/phase/phase-10*.md` plus `docs/phase/phase-10*-checklist.md` ledgers.

## 9. Development Item Status

| ID | Source | Development item | Disposition | Target | Status |
| --- | --- | --- | --- | --- | --- |
| DEV-CBD-001 | `docs/journal/2026/08/2026-08-15-usecase-driven-component-modeling-direction.md` | Use Case and Scenario intent is decomposed into attributable Activities, required capabilities, component coverage, gaps, and human-reviewed composition decisions; Phase 9 also establishes the Canonical Component Design Model and stakeholder/engineering projection architecture without turning inference into fact. | ACCEPTED_PHASE_WORK | Phase 9 | CLOSED |
| DEV-CBD-002 | `docs/journal/2026/08/2026-08-17-project-internal-model-storage-direction.md` | Project-local internal-model packages provide a portable, hash-bound continuation and approval boundary for reviewed Phase 9 semantic state, Use Case Realization, candidate design, semantic diff, and CML projection work. | ACCEPTED_PHASE_WORK | Phase 10 sequence (10–10.7), per canonical Phase 10.7 closure | CLOSED |
| DEV-CBD-003 | `docs/journal/2026/09/2026-09-16-capability-catalog-development-item.md` | Consume upstream Capability projections through attributable catalog/query contracts and shared read-only views, with real producer/consumer acceptance. | ACCEPTED_PHASE_WORK | Phase 11 -> Phase 11.1 | CLOSED |
| DEV-CBD-004 | `docs/journal/2026/10/2026-10-06-cncf-job-cbd-scope-separation-handoff.md` | Complete transferred CAR Review native Job integration: build connection, explicit Review identity, registered Provider execution/cancellation, Entity persistence, production StartReview, durable Review-to-Job binding and independently fresh runtime acceptance. | ACCEPTED_PHASE_WORK | Phase 14 | CLOSED |

### 9.1 Use Case-driven component composition and Dashboard

Phase 9 owns the requirement-side model and CBD Support workflow for turning
Story, Use Case, Scenario, and Step intent into explicit Activities, capability
requirements, component alternatives, gaps, proposals, and human decisions. It
also owns the Canonical Component Design Model and the Mono-Koto, Use Case,
Entity, Event, Structure, Classification, Workflow, and StateMachine projection
contracts. The draft `src/main/cml/usecase/application-component-composition.cml`
is working input, not an implemented product contract. Phase 9 must bound
provider ownership, stable CML syntax, decision authority, semantic identity,
candidate-design/semantic-diff handoff, and executable acceptance evidence
before it can claim implementation progress. Durable plan state, approval
history, continuation, freshness/invalidation, and exact mutation gating belong
to Phase 10.

### 9.2 Project-local internal-model continuation

DEV-CBD-002 is promoted to the Phase 10 sequence. It owns the source layout,
schema, lifecycle, integrity, freshness, approval, build exclusion, and resume
semantics of a project-local internal-model package. Its semantic input is the
accepted Phase 9 model/projection/candidate contract; it must persist and
rehydrate those identities rather than define a parallel storage-only model.

Phase 10's central continuity requirement is:

```text
Phase 9 Canonical Component Design Model
  -> Candidate Design / Semantic Diff
  -> Phase 10 durable internal-model package
  -> fresh-process rehydration
  -> same Phase 9 projections
  -> exact approval gate
  -> canonical CML change
  -> validation and Phase 9 re-projection
```

The portable minimum package, sensitive-data policy, CBD Support API, skill
authority, validation boundary, and build/publication exclusion are Phase 10
sequence contract work and are tracked by its ordered Phase checklists.
