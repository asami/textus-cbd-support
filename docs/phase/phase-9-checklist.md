# Phase 9 Checklist: Component Dashboard

Status: PLANNED
phase=[Phase 9](phase-9.md)

Planning rule: each subphase is intended to remain within approximately six hours of focused implementation once its declared upstream inputs are available. Split a subphase before implementation if evidence shows it is materially larger.

## Phase 9.1: Source inventory and projection contract

- [ ] Inventory authoritative sources for identity, content, usage, operation, quality, and knowledge.
- [ ] Record missing Cozy model metadata and missing CNCF runtime evidence explicitly.
- [ ] Define deterministic `ComponentDashboard` projection identity, attribution, absence, authorization, and redaction rules.
- [ ] Add a projection skeleton and focused specifications proving no Dashboard-local conclusion or hidden source winner.

## Phase 9.2: Content Overview

- [ ] Project purpose/responsibility, domain summary, capabilities, representative use cases, rules, interfaces/events, and related knowledge where evidence exists.
- [ ] Preserve source attribution and explicit partial/absent metadata.
- [ ] Add deterministic projection specifications.

## Phase 9.3: Dashboard Web Entry

- [ ] Add the exact-component Dashboard Web entry backed by the common projection.
- [ ] Present identity, purpose, compact content summary, usage condition, operation condition, and Review condition.
- [ ] Add only navigation whose target contract is implemented.
- [ ] Preserve existing authorization and redaction boundaries.

## Phase 9.4: Structure View Contract

- [ ] Define normalized Entity, Value, Aggregate, composition, aggregation, and association projection types.
- [ ] Preserve roles, cardinality, navigability, ownership, independent existence, creation/deletion, reassignment, lifecycle propagation, and aggregate boundary when published.
- [ ] Compare required fields with Cozy Phase 54 metadata and record unsupported fields as explicit gaps.
- [ ] Prove CBD Support does not parse CML source or infer lifecycle semantics by naming convention.

## Phase 9.5: Structure View Web

- [ ] Implement Structure overview and exact element/relation detail.
- [ ] Distinguish composition, aggregation, and association semantically and visually.
- [ ] Explain available ownership/lifecycle semantics in relation detail.
- [ ] Preserve absence behavior when upstream metadata is incomplete.

## Phase 9.6: Classification View

- [ ] Normalize generalization, trait, and powertype as distinct classification evidence.
- [ ] Render one integrated classification topology.
- [ ] Preserve multiple powertype dimensions for one concept.
- [ ] Add exact element detail and reverse navigation.

## Phase 9.7: Workflow Projection

- [ ] Normalize Workflow identity, purpose, activities, flow, branch/merge, participants, domain elements, operations/events, and state effects where published.
- [ ] Record Cozy Phase 54 gaps rather than reconstructing them.
- [ ] Add deterministic Workflow projection specifications.

## Phase 9.8: Workflow Web View

- [ ] Implement Workflow overview and activity detail.
- [ ] Show participating/affected domain elements and declared state effects.
- [ ] Preserve stable model identity through navigation.

## Phase 9.9: StateMachine Deep Dive

- [ ] Project states, transitions, triggers, guards, actions, related Workflow activities, operations, events, and rules where available.
- [ ] Support Workflow/activity -> domain element -> StateMachine/transition navigation.
- [ ] Support reverse navigation where stable metadata permits it.
- [ ] Avoid isolated name-based lookup.

## Phase 9.10: Use Case View

- [ ] Project actor, goal, trigger, preconditions, main/alternative/exception flows, postconditions, domain elements, operations/events, collaborators, and realizing Workflow where modeled.
- [ ] Implement Use Case overview and exact detail.
- [ ] Complete the semantic path `Use Case -> Workflow -> StateMachine -> Entity` and reverse links where possible.

## Phase 9.11: Lifecycle Semantics Evidence

- [ ] Project declared composition/aggregation/association semantics beside available runtime observations and Review evidence.
- [ ] Preserve disagreement and missing runtime evidence explicitly.
- [ ] Consume CNCF Phase 72 semantics/evidence only when that contract is available; do not implement a local lifecycle engine.
- [ ] Define future Review-check inputs for composition lifecycle consistency, aggregation lifecycle independence, and Structure/StateMachine consistency without claiming unsupported checks are implemented.

## Phase 9.12: Unified Dashboard Integration and Closure

- [ ] Integrate existing Usage/Discovery projections into the general Dashboard.
- [ ] Integrate runtime/publication/dependency/availability information into Operation.
- [ ] Integrate Phase 8 Review dashboard information into Quality without changing canonical Review conclusions.
- [ ] Verify Content, Usage, Operation, and Quality navigation on representative components.
- [ ] Run standalone SAR and relevant regression gates.
- [ ] Update user/manual documentation and close Phase 9 only with reproducible evidence.

## Cross-project dependencies

- Cozy Phase 54: semantic component model metadata for Structure, Classification, Workflow, StateMachine, and Use Case views.
- CNCF Phase 72: admitted runtime lifecycle semantics/evidence for composition, aggregation, and Structure/StateMachine consistency.

Neither dependency authorizes CBD Support to create a workaround when an upstream semantic contract is absent.
