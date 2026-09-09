# Phase 9 - Evidence-Backed Component Composition and Dashboard

Stage Status:
- Current status: OPEN
- Current step: P9-01 through P9-06 establish the shared evidence,
  responsibility, canonical-model, and projection boundaries before
  implementation begins.
- Successor: Phase 10 - Durable Model Development and Continuation
- Owner: Textus CBD Support development
- Update rule: Update this block only after reproducible evidence is recorded
  in `phase-9-checklist.md`; phase closure is based solely on that ledger.

## Purpose

Phase 9 joins application-component composition and Component Dashboard without conflating authority. It also establishes a single attributable Canonical Component Design Model projected into stakeholder-facing and engineering views. Analysis is not a separate upstream model transformed one-way into design.

## Canonical source and projection principle

CML remains canonical where the design is expressible in CML. CAR/SAR metadata, runtime metadata, generated definitions, terminology/BoK knowledge, tests, and Review evidence may enrich the model while retaining attribution and authority.

```text
Canonical CML + admitted attributable evidence + terminology/BoK
                    |
                    v
        Canonical Component Design Model
                    |
        +-----------+----------------+------------------+
        |                            |                  |
        v                            v                  v
Communication / Analysis       Static projections   Dynamic projections
        |                            |                  |
        +-- Mono-Koto                +-- Entity         +-- Event
        +-- Use Case                 +-- Structure      +-- Workflow
        +-- Event Storming           +-- Classification +-- Flowchart
                                                        +-- StateMachine
```

A view is a projection of shared semantics, not an independently authoritative model. A model element may appear differently in several views.

## Communication / Analysis semantics

### Mono-Koto Analysis

Mono-Koto is the stakeholder-facing conceptual overview and terminology bridge. A Mono is not mechanically one Entity; it may summarize Entity, Value, Aggregate, and structural semantics. A Koto is not mechanically one Event; it may summarize Use Case intent, Command, Event, Workflow activity, and state effects.

Mono-Koto must cross-reference attributable terminology/BoK entries. Terms are semantic anchors, not merely labels. Preferred names, aliases, definitions, Mono/Koto relevance, and related model elements should be navigable where evidence exists. Similar terminology must not be silently merged; synonym candidates and terminology inconsistencies remain explicit until resolved by authoritative evidence or human decision.

### Use Case

Use Case remains the stakeholder intent view and the normal source of Actor semantics used by other communication projections. Actor identity must not be invented merely to complete a diagram.

### Event Storming

Event Storming is a stakeholder-facing behavioral overview across multiple engineering projections, not an alternate Event Model renderer.

```text
Actor -> Command -> Aggregate/Entity -> Domain Event
      -> Policy/Reaction -> Command -> Domain Event
```

Representative semantic sources are Use Case Actor, Command/Operation, Entity/Aggregate, Event, Workflow policy/reaction, external Component/dependency, and Query/View semantics. Hotspots are explicit analysis/review observations rather than canonical domain facts.

Mono-Koto and Event Storming should support bidirectional cross-navigation. A Koto may be expanded into its event/causal progression; an Event Storming event may navigate back to its Koto, Mono, terminology, Entity/Aggregate, Workflow, and other admitted semantics.

## Dynamic presentation roles

Event Storming, Flowchart, Workflow, and StateMachine have different roles:

```text
Event Storming
  domain/stakeholder view of what happens, why, and what follows
       |
       v
Flowchart
  approachable and potentially non-faithful Workflow presentation
       |
       v
Workflow
  faithful engineering projection of behavioral/control semantics
       |
       v
StateMachine
  detailed lifecycle projection of an affected subject
```

This is a conceptual refinement order, not a transformation pipeline. Flowchart may simplify or omit technical detail and must disclose that it is not necessarily faithful to every Workflow semantic detail. Workflow remains the precise engineering view.

## Stakeholder feedback and design-change loop

Mono-Koto, Use Case, Event Storming, and Flowchart remain available after detailed design begins. Edits/corrections are semantic change proposals, not independent analysis facts. Where CML owns the affected design, CBD Support traces proposals back to CML and uses candidate design, semantic diff, Review, and Git-governed acceptance.

```text
Canonical CML
    -> Canonical Component Design Model
    -> stakeholder / engineering projections
    -> Semantic change proposal
    -> Proposed CML patch
    -> Candidate Component Design Model
       + Analysis-view diff
       + Engineering-view diff
       + Candidate Review
    -> Git branch / Pull Request
    -> Human review and merge
```

## Scope additions for analysis views

Phase 9 must:

1. define the Canonical Component Design Model and projection rules shared by Mono-Koto, Use Case, Event Storming, Entity, Event, Structure, Classification, Workflow, Flowchart, and StateMachine;
2. implement Mono-Koto as stakeholder conceptual overview and terminology/BoK bridge;
3. preserve attributable terminology links and explicit synonym/inconsistency candidates without hidden semantic merging;
4. implement Event Storming as a cross-model behavioral projection using admitted Actor, Command, Entity/Aggregate, Event, Workflow, external-system, and Query/View semantics;
5. derive Actor semantics normally from Use Case;
6. implement stable forward/reverse navigation among terminology, Mono/Koto, Event Storming, Use Case, Entity/Event, Workflow, and StateMachine semantics;
7. keep Workflow faithful while permitting an explicitly non-faithful/simplified Flowchart projection for communication; and
8. route stakeholder-facing edits through semantic proposal and Git-governed design acceptance.

The existing composition, Dashboard, evidence, candidate-design, Phase 10 handoff, Usage, Operation, Quality/Review, and validation responsibilities remain unchanged.

## Stages

### Stage 9.1: Shared evidence, authority, terminology, and projection contract

Freeze composition/provider/human boundaries, Dashboard source inventory, canonical authority, Canonical Component Design Model, terminology/BoK linkage, projection semantics, attribution/absence rules, and required upstream contracts.

### Stage 9.2: Canonical composition plan

Implement the typed transient composition plan and deterministic evidence/decision behavior without durable storage or application generation.

### Stage 9.3: Dashboard foundation and Content

Implement deterministic Component Dashboard Content and authorized Web entry from admitted normalized metadata.

### Stage 9.4: Communication and analysis projections

Implement Mono-Koto, Use Case, and Event Storming. Mono-Koto acts as terminology bridge; Event Storming acts as cross-model behavioral overview. Provide stable cross-navigation and expose ambiguity/missing semantics rather than synthesizing completeness.

### Stage 9.5: Static engineering projections

Implement Entity, Structure, and Classification from published normalized metadata, preserving identity, ownership, lifecycle, cardinality, navigability, generalization, trait, and powertype semantics.

### Stage 9.6: Dynamic engineering projections

Implement Event, faithful Workflow, explicitly simplified/non-faithful Flowchart, and StateMachine projections with cause/consequence, affected-entity, actor, and navigation links. Do not reconstruct missing semantics from names or layout.

### Stage 9.7: Cross-view feedback and evidence integration

Connect terminology, analysis, and engineering projections; identify detailed-model impacts of stakeholder changes; preserve evidence; integrate Usage, Operation, and Quality/Review; and produce stable semantic identities/candidate contracts for Phase 10.

### Stage 9.8: Validation and closure

Run proportionate validation, synchronize documentation, account for deferred work, and close only when checklist evidence supports completion.

## Cross-project dependencies

Cozy Phase 54 owns machine-readable semantic component metadata required by Entity, Structure, Classification, Event, Workflow, StateMachine, Use Case, and related projections. Mono-Koto/Event Storming may require additional semantic grouping, terminology-link, Actor, Command/Event, policy/reaction, external-system, and Query/View metadata. Missing contracts must be recorded as upstream gaps rather than inferred locally.

CNCF Phase 72 owns runtime lifecycle semantics and attributable runtime evidence. CNCF runtime events/workflow evidence may enrich Event Storming and other projections only as attributable evidence; CNCF does not own stakeholder presentation or terminology projection.

Phase 10 consumes the accepted Phase 9 semantic/model contracts for durability and continuation.

## Planning sources

- `docs/journal/2026/09/2026-09-09-mono-koto-analysis-view.md` records the Mono-Koto, terminology, Event Storming, Flowchart, and stakeholder-communication direction.
- Existing composition, Dashboard, candidate-design, and Phase 10 planning records remain applicable.

## Non-goals

- independent authoritative Mono-Koto, Event Storming, Event, or other view models;
- treating `Mono = Entity` or `Koto = Event` as mandatory one-to-one mappings;
- treating terminology similarity as proof of semantic identity;
- inventing Actor, Event, policy, or causal semantics to make Event Storming visually complete;
- treating Flowchart as necessarily faithful to every Workflow semantic detail;
- bypassing candidate-design validation and Git-governed acceptance from stakeholder-facing edits;
- compensating locally for missing Cozy/CNCF published semantics.
