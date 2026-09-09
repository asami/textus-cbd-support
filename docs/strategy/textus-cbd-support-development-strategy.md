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

### Phase 10: Durable Model Development and Continuation

Make Phase 9 modeling work durable without changing its semantics. Introduce a
project-owned, hash-bound internal-model package that stores the source basis,
selected semantic state, decisions, open issues, candidate CML projection,
review/approval evidence, validation, and continuation cursor. A fresh CBD
Support process must be able to validate and rehydrate the package into the same
Phase 9 Canonical Component Design Model and projections, detect source drift,
and continue only the permitted next action. Exact human approval gates any CML
mutation; CML and Git remain the canonical design and acceptance authorities.

## Current Priority

Phases 1 through 8 are complete. Phase 9 is OPEN and establishes the semantic,
projection, candidate-design, and semantic-diff contracts consumed by planned
Phase 10. Phase 10 is planned under DEV-CBD-002 and must not pull durability or
approval-lifecycle concerns back into Phase 9 before the predecessor contracts
are accepted.

The detailed historical completion evidence for Phases 1 through 8 remains in
their phase ledgers and journals. Phase 9 and Phase 10 planning is authoritative
in `docs/phase/phase-9.md`, `docs/phase/phase-9-checklist.md`,
`docs/phase/phase-10.md`, and `docs/phase/phase-10-checklist.md`.

## 9. Development Item Status

| ID | Source | Development item | Disposition | Target | Status |
| --- | --- | --- | --- | --- | --- |
| DEV-CBD-001 | `docs/journal/2026/08/2026-08-15-usecase-driven-component-modeling-direction.md` | Use Case and Scenario intent is decomposed into attributable Activities, required capabilities, component coverage, gaps, and human-reviewed composition decisions; Phase 9 also establishes the Canonical Component Design Model and stakeholder/engineering projection architecture without turning inference into fact. | ACTIVE_PHASE_WORK | Phase 9 | OPEN |
| DEV-CBD-002 | `docs/journal/2026/08/2026-08-17-project-internal-model-storage-direction.md` | Project-local internal-model packages provide a portable, hash-bound continuation and approval boundary for reviewed Phase 9 semantic state, Use Case Realization, candidate design, semantic diff, and CML projection work. | ACTIVE_PHASE_WORK | Phase 10 | PLANNED |

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

DEV-CBD-002 is promoted to Phase 10. It owns the source layout, schema,
lifecycle, integrity, freshness, approval, build exclusion, and resume semantics
of a project-local internal-model package. Its semantic input is the accepted
Phase 9 model/projection/candidate contract; it must persist and rehydrate those
identities rather than define a parallel storage-only model.

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
contract work and are tracked by `docs/phase/phase-10-checklist.md`.
