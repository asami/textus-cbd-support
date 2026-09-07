# Phase 9 - Component Dashboard

Stage Status:
- Current status: PLANNED
- Current step: P9-01 projection contract and source-inventory analysis
- Owner: Textus CBD Support development
- Update rule: Update this block and `phase-9-checklist.md` only after each item has reproducible evidence.

## Purpose

Turn Dashboard into the general human entry point for understanding a Component.

Phase 8 completed a Review-centered dashboard and delivery surface. Phase 9 broadens that capability into a Component Dashboard that explains both component content and component condition without replacing Discovery or Review.

The product roles are:

- Discovery: find a Component and retrieve attributable evidence.
- Dashboard: understand what the Component is, what it models, what it does, how it is used, and what condition it is in.
- Review: evaluate the Component and produce canonical quality conclusions.

The Dashboard must not invent facts, infer missing model semantics, or create conclusions that do not exist in authoritative catalog, model, runtime, BoK, or Review evidence.

## Target Information Architecture

    Component Dashboard
    |
    +-- Content
    |    +-- DomainModel
    |    |    +-- Static Model
    |    |    |    +-- Structure View
    |    |    |    +-- Classification View
    |    |    +-- Dynamic Model
    |    |         +-- Workflow View
    |    |              +-- StateMachine Detail
    |    +-- Use Case View
    |
    +-- Usage
    +-- Operation
    +-- Quality / Review

Content is a first-class Dashboard concern, not a Review subsection.

## Planning Rule

Each subphase is sized as a bounded implementation slice intended to be completable within approximately six hours of focused development when its declared upstream inputs are available.

If implementation evidence shows a subphase is materially larger, split it before implementation rather than expanding the scope in place.

Large product capabilities are grouped below, but completion is tracked by the individual subphases in `phase-9-checklist.md`.

## Capability A: Dashboard Projection and Content Overview

### Phase 9.1: Source Inventory and Projection Contract

Define the source boundary and one deterministic `ComponentDashboard` projection contract.

The projection should distinguish at least:

- identity and summary;
- content;
- usage;
- operation;
- quality;
- knowledge/source attribution;
- explicit absences and unsupported model details.

Inventory the currently available component profile, CML/model metadata, Review projection, runtime/local evidence, BoK evidence, and existing Web surfaces. Record every metadata gap rather than compensating with source-text parsing or name-based inference.

Completion target: contract/spec plus executable projection skeleton and representative source-attribution tests.

### Phase 9.2: Content Overview Projection

Implement the semantic Content Overview from admitted model/component metadata.

It should project, where evidence exists:

- purpose and responsibility;
- domain summary;
- major capabilities;
- representative use cases;
- important rules;
- external interfaces and events;
- links to related knowledge.

The overview is intentionally compressed. It is not a complete model browser or API reference.

Completion target: deterministic content projection with explicit absence behavior and no model-source heuristics.

### Phase 9.3: Component Dashboard Web Entry

Add the general Component Dashboard Web entry and Content Overview presentation.

The first page should make the component identity, purpose, model-content counts/summary, usage condition, operational condition, and Review condition understandable at a glance while preserving authorization and redaction boundaries.

Provide navigation placeholders only for views whose contracts are complete; do not add dead or synthetic links.

Completion target: authorized Web entry backed only by the common Component Dashboard projection.

## Capability B: Static Domain Model

### Phase 9.4: Structure View Contract and Metadata Gap Gate

Define the Structure View contract before rendering it.

The contract must preserve Entity, Value, Aggregate, composition, aggregation, and association as distinct semantic elements. Relationship projection should be able to retain, when published:

- endpoint identities and roles;
- cardinality;
- navigability;
- ownership;
- independent-existence semantics;
- creation/deletion policy;
- reparenting/reassignment policy;
- lifecycle propagation;
- aggregate boundary.

Compare this contract with Cozy-published model metadata. Missing required metadata is an explicit upstream gap and blocks only the affected semantics; CBD Support must not reconstruct it heuristically.

Completion target: normalized Structure projection types, gap ledger, and representative specifications.

### Phase 9.5: Structure View Web Projection

Implement Structure View using only admitted normalized metadata.

The view must visually and semantically distinguish:

- composition;
- aggregation;
- association.

An element/relation detail should explain available lifecycle semantics rather than merely showing a UML-like edge.

Completion target: overview plus exact element/relation detail navigation and absence-safe rendering.

### Phase 9.6: Classification View

Implement a unified Classification View combining:

- generalization;
- trait;
- powertype.

Powertype is not a standalone top-level dashboard feature. The view should reveal the complete classification topology: essential specialization, cross-cutting traits, and one or more explicit powertype dimensions.

Completion target: classification overview, exact element detail, multiple-dimension preservation, and reverse navigation to affected domain elements.

## Capability C: Dynamic Domain Model

### Phase 9.7: Workflow View Contract and Projection

Make Workflow the primary dynamic-model overview.

The projection should preserve, where modeled:

- workflow identity and purpose;
- activities;
- control/flow relations;
- branch/merge information;
- participants;
- affected domain elements;
- operations/events;
- declared state effects.

Completion target: normalized Workflow projection plus deterministic overview/detail specifications.

### Phase 9.8: Workflow Web View

Implement the Workflow overview and activity detail navigation.

The view should make it possible to understand what domain work progresses, which domain elements participate, and which state effects are declared without requiring source inspection.

Completion target: Workflow Web projection backed only by the normalized contract.

### Phase 9.9: StateMachine Deep Dive

Implement StateMachine as the lifecycle deep dive from Workflow or a domain element.

Project, where available:

- states;
- transitions;
- triggers;
- guards;
- actions;
- related Workflow activities;
- operations;
- events;
- business rules.

Support forward and reverse semantic navigation between Workflow activity, affected domain element, StateMachine, and transition.

Completion target: exact StateMachine detail with stable cross-view identity and no isolated name-based lookup.

## Capability D: Use Case Semantics

### Phase 9.10: Use Case View

Implement the goal-oriented Use Case view.

Project, where modeled:

- actor;
- goal;
- trigger;
- preconditions;
- main flow;
- important alternative/exception flows;
- postconditions;
- participating domain elements;
- operations/events;
- collaborating components/systems;
- realizing Workflow.

Complete the principal semantic navigation chain:

    Use Case -> Workflow -> StateMachine -> Entity

and reverse navigation where stable metadata permits it.

Completion target: overview/detail Use Case view plus cross-view navigation specifications.

## Capability E: Lifecycle Semantics and Review Integration

### Phase 9.11: Lifecycle Semantics Evidence Projection

Connect static ownership semantics to runtime/Review evidence without moving runtime enforcement into CBD Support.

For composition, aggregation, and association, project the declared model semantics beside available runtime observations and Review evidence. Preserve disagreement or missing evidence explicitly.

Candidate future Review checks include:

- composition lifecycle consistency;
- aggregation lifecycle independence;
- Structure/StateMachine consistency.

This subphase does not implement missing CNCF runtime semantics and does not infer them from CML source text.

Completion target: evidence comparison projection and explicit boundary/gap behavior.

## Capability F: Unified Component Dashboard

### Phase 9.12: Usage, Operation, Quality Integration and Closure

Integrate existing Discovery/usage, runtime/operation, and Phase 8 Review information into the general Component Dashboard.

The top-level Dashboard should provide a compact overview such as:

- identity/version/kind;
- purpose;
- domain/use-case counts or summaries;
- operations/MCP/Skill usage status where available;
- runtime/publication/dependency/availability condition;
- Review gate and quality summary;
- links into Content, Usage, Operation, and Quality detail views.

Review remains canonical for quality conclusions. Dashboard remains a projection.

Completion target: one coherent authorized Component Dashboard, representative standalone/SAR verification, regression coverage for existing Review dashboard behavior, and documentation update.

## Cross-Project Dependencies

Phase 9 intentionally keeps project ownership explicit.

### Cozy Phase 54

Cozy Phase 54 `Semantic Component Model Metadata for Dashboard` owns the CML/model transformation and machine-readable publication work needed by Structure, Classification, Workflow, StateMachine, and Use Case views.

Phase 9.1 must inventory what is already available before assuming all of Phase 54 is required. Phase 9.4 through Phase 9.10 consume only the metadata that Cozy can publish authoritatively. Missing metadata remains an explicit upstream gap.

CBD Support must not bypass Phase 54 gaps by building a second CML parser.

### CNCF Phase 72

CNCF Phase 72 `Model-Driven Lifecycle Semantics` owns runtime realization/enforcement and attributable runtime evidence for strict composition, aggregation, association, and Structure/StateMachine lifecycle consistency.

Phase 9.11 consumes this evidence when available. Earlier Dashboard views remain useful without Phase 72 and must report unavailable runtime evidence explicitly.

CBD Support must not introduce a competing lifecycle engine or infer runtime semantics locally.

## Non-Goals

- Replacing Discovery with Dashboard search logic.
- Replacing canonical Review projections or conclusions.
- Parsing CML source locally to compensate for missing Cozy metadata.
- Inferring composition, aggregation, classification, Workflow, StateMachine, or Use Case semantics from names or diagram conventions.
- Implementing missing CNCF runtime semantics inside CBD Support.
- Building a generic UML modeling tool.
- Making every detail visible on the top-level Dashboard.

## Completion Conditions

Phase 9 closes when:

1. The general Component Dashboard is the human entry point for an exact Component.
2. Content Overview links to usable DomainModel and Use Case views.
3. Static Structure and Classification semantics preserve authoritative model distinctions.
4. Workflow is usable as the dynamic-model overview and StateMachine as its lifecycle deep dive.
5. Cross-view navigation uses stable model identity rather than name guessing.
6. Usage, Operation, and Quality/Review information are integrated without changing their source authority.
7. Missing Cozy/CNCF capabilities remain explicit upstream gaps rather than CBD-local workarounds.
8. Representative component, standalone SAR, and regression tests pass.
