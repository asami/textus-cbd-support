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

Phase 9 joins two complementary CBD Support capabilities without conflating
their authority. Application-component composition makes application intent,
required capabilities, attributable component evidence, alternatives, gaps,
proposals, and human decisions reviewable. Component Dashboard makes the
content and condition of one exact Component understandable through the same
admitted evidence.

Composition answers what can cover an application's explicitly modeled needs.
Dashboard answers what one known Component is, models, does, how it is used,
and what condition its evidence supports. Neither surface creates catalog facts,
infers unsupported model semantics, selects a hidden winner, or replaces the
canonical Review conclusion.

Phase 9 also establishes the Dashboard's model-view architecture. Analysis and
engineering models are not maintained as independent artifacts connected by a
one-way `analysis -> design` transformation. CBD Support builds one attributable
Canonical Component Design Model from canonical source and admitted evidence,
then projects that model into views with different purposes and levels of
technical detail.

## Canonical source and projection principle

CML remains the canonical design source where the design is expressible in CML.
CAR/SAR metadata, runtime metadata, generated definitions, BoK knowledge, tests,
and Review evidence may enrich the Component Design Model, but they retain their
own attribution and authority.

The Dashboard must not silently create a second mutable source of design truth.
Instead:

```text
Canonical CML + admitted attributable evidence
                    |
                    v
        Canonical Component Design Model
                    |
        +-----------+------------+------------------+
        |                        |                  |
        v                        v                  v
Communication / Analysis   Static projections   Dynamic projections
        |                        |                  |
        +-- Mono-Koto            +-- Entity         +-- Event
        +-- Use Case             +-- Structure      +-- Workflow
                                 +-- Classification +-- StateMachine
```

A view is a projection of shared semantics, not an independently authoritative
model. The same model element may therefore appear differently in several views.
For example, an admitted domain event may appear as an Event in Event Model, as
part of a Koto in Mono-Koto Analysis, as an activity consequence in Workflow,
and as a transition trigger in StateMachine.

## Shared evidence boundary

Both composition and Dashboard capabilities consume only attributable catalog,
model, runtime, BoK, and Review evidence. They preserve source attribution,
authorization/redaction, explicit absence, conflicting evidence, and provider
limitations.

Composition may link an admitted component identity to its Dashboard entry, but
Dashboard does not turn a composition candidate, provider suggestion, or human
decision into a Component fact. A semantic or AI provider may make an
attributable suggestion; only an explicit human decision can select an existing
component or promote a proposed component.

The model-view architecture follows the same rule: projections may organize,
abstract, summarize, and cross-link admitted semantics, but may not invent a
model fact merely to complete a diagram or stakeholder-friendly presentation.

## Dashboard information architecture

```text
Component Dashboard
|
+-- Content
|    +-- Communication / Analysis
|    |    +-- Mono-Koto Analysis
|    |    +-- Use Case
|    |
|    +-- Domain Model
|         +-- Static Model
|         |    +-- Entity Model
|         |    +-- Structure
|         |    +-- Classification
|         |
|         +-- Dynamic Model
|              +-- Event Model
|              +-- Workflow
|              +-- StateMachine
|
+-- Usage
+-- Operation
+-- Quality / Review
```

Content is a first-class Dashboard concern, not a Review subsection. Discovery
finds Components and retrieves attributable evidence; Dashboard explains an
exact Component; Review remains the canonical evaluator of quality.

### Mono-Koto Analysis as communication projection

Mono-Koto Analysis is the stakeholder-facing conceptual overview of the same
Component Design Model used by engineering views. It is intentionally expressed
in domain vocabulary and should normally hide engineering-specific terms such
as `AggregateRoot`, `composition`, `Command`, `DomainEvent`, and
`StateTransition` unless a user drills down.

A Mono is not mechanically identical to one Entity. It is an overview projection
of structural/domain semantics and may summarize an Entity, Value, Aggregate,
or related structural elements.

A Koto is not mechanically identical to one Event. It is an overview projection
of behavioral/temporal semantics and may summarize Commands, Events, Workflow
activities, and state effects.

Representative relation:

```text
Mono: 注文
  <-> Order : AggregateRoot
      OrderLine : Entity
      OrderStatus : Value

Koto: 注文する
  <-> PlaceOrder : Command
      OrderPlaced : DomainEvent
```

This relation is intentionally bidirectional for navigation and review, while
semantic authority remains with canonical source and admitted evidence.

### Entity and Event model bridge

Entity Model and Event Model are explicit engineering projections because they
provide the semantic bridge between stakeholder analysis and detailed
Structure/Workflow/StateMachine views.

```text
Mono <---- semantic projection ----> Entity Model
  |                                  |
  |                                  +-- Entity / Value / Aggregate
  |                                  +-- ownership / lifecycle / identity
  |
Koto <---- semantic projection ----> Event Model
                                     |
                                     +-- Command / Event
                                     +-- cause / consequence / affected entity
```

Structure refines structural relationships such as composition, aggregation,
and association. Classification refines generalization, trait, and powertype.
Workflow organizes behavioral progression. StateMachine deep-dives the lifecycle
of an affected subject.

## Stakeholder feedback and design-change loop

Mono-Koto and Use Case views are not read-once upstream artifacts. They remain
available after detailed design begins so that stakeholders and engineers can
review the same semantics at different levels of detail.

An edit or correction expressed through an analysis view is treated as a
semantic change proposal, not as an independent analysis-only fact. Where CML
owns the affected design, CBD Support should trace the proposal back to CML and
use the existing candidate-design and Git-governed improvement loop:

```text
Canonical CML
    |
    v
Canonical Component Design Model
    |
    v
Mono-Koto / Use Case stakeholder review
    |
    v
Semantic change proposal
    |
    v
Proposed CML patch
    |
    v
Candidate Component Design Model
    |
    +-- Analysis-view diff
    +-- Engineering-view diff
    +-- Candidate Review
    |
    v
Git branch / Pull Request
    |
    v
Human review and merge
```

For example, a stakeholder-level correction to the relation between `注文` and
`支払う` may identify affected Order state-machine, Payment workflow, and event
relations. CBD Support may propose the corresponding canonical-source change,
but the analysis view does not directly rewrite the canonical main branch.

This preserves the existing principle: **model-driven improvement,
Git-governed acceptance**.

## Phase 10 handoff boundary

Phase 9 deliberately keeps composition plans and candidate modeling state
transient. Its responsibility is to establish the semantic identities,
projections, traceability, candidate design, semantic diff, review evidence, and
canonical-source change proposal that can be inspected in the current work
context.

Phase 10 takes over when that work must survive a process/session boundary or
become an explicit reviewed development state. The handoff is therefore not a
conversion to a different storage model:

```text
Phase 9
Canonical Component Design Model
  -> stakeholder / engineering projections
  -> semantic feedback
  -> Candidate Design Model
  -> Semantic Diff / Candidate Review
            |
            | durable handoff using the same semantic identities
            v
Phase 10
Project-local Internal Model Package
  -> source snapshots
  -> selected semantic state
  -> decisions / open issues
  -> candidate CML projection
  -> approval / validation
  -> continuation cursor
            |
            v
fresh-process rehydration
            |
            v
Phase 9 Canonical Component Design Model + projections
```

Phase 9 acceptance must therefore provide stable enough semantic identity,
projection, traceability, candidate-design, and semantic-diff contracts for
Phase 10 to persist and rehydrate them without reinterpretation. Phase 10 owns
durability, approval binding, continuation, freshness/invalidation, and the
exact CML mutation gate. Phase 9 does not need to implement those storage and
lifecycle concerns in advance.

## Scope

1. Promote the V1 composition responsibility, evidence, coverage, proposal,
   and human-decision boundary into design and specification contracts.
2. Define the shared source inventory and a deterministic Component Dashboard
   projection with explicit absence and attribution behavior.
3. Define the Canonical Component Design Model and projection rules shared by
   Mono-Koto, Use Case, Entity, Event, Structure, Classification, Workflow, and
   StateMachine views.
4. Implement a typed, transient composition plan with deterministic coverage,
   bounded alternatives and gaps, and no hidden winner.
5. Implement Dashboard Content and authorized read-only Web surfaces only from
   admitted normalized metadata and shared projection contracts.
6. Implement Mono-Koto Analysis as a stakeholder-facing overview linked to
   Entity and Event engineering projections rather than a separate analysis
   source of truth.
7. Implement Entity, Structure, Classification, Event, Workflow, StateMachine,
   and Use Case projections with stable semantic cross-navigation.
8. Preserve analysis-view feedback as explicit semantic proposals that can be
   traced to canonical-source changes and candidate design review.
9. Establish stable handoff identities and semantic-diff/candidate contracts
   sufficient for Phase 10 durable persistence and rehydration.
10. Integrate Usage, Operation, and canonical Review information into Dashboard
    without changing their source authority.
11. Verify the behavior with executable specifications and proportionate static,
    integration, and review evidence.

## Planning sources

- `docs/notes/usecase-driven-component-composition-v1.md` is the
  non-normative V1 composition direction.
- `docs/notes/component-dashboard-content-model.md` is the Dashboard content
  planning record introduced by the synchronized upstream history.
- `docs/journal/2026/09/2026-09-09-mono-koto-analysis-view.md` records the
  Mono-Koto analysis/projection and stakeholder-communication direction.
- `docs/journal/2026/09/2026-09-03-design-improvement-pull-request-loop.md`, where
  present in synchronized history, records the canonical-source candidate
  design and Git-governed improvement direction.
- `src/main/cml/usecase/application-component-composition.cml` remains working
  input until its syntax and contracts are promoted.
- `docs/phase/phase-10.md` defines the durable successor boundary for
  DEV-CBD-002.

## Non-goals

- Generating, assembling, building, publishing, deploying, or executing an
  application from a composition plan.
- Treating semantic evidence, provider inference, a proposal, or a human
  decision as a catalog-owned Component fact.
- Selecting a component or resolving conflicting evidence without an explicit
  human decision.
- Maintaining Mono-Koto, Entity, Event, or other Dashboard views as independent
  authoritative models that can silently diverge from canonical source.
- Treating `Mono = Entity` or `Koto = Event` as a mandatory one-to-one mapping.
- Parsing CML source, using names or diagram conventions, or implementing a
  local lifecycle engine to compensate for missing published model/runtime
  metadata.
- Allowing stakeholder-facing edits to bypass candidate-design validation and
  Git-governed acceptance where CML owns the affected design.
- Persisting composition plans, approval history, continuation packages,
  freshness/invalidation state, or durable candidate state; these are Phase 10
  responsibilities under DEV-CBD-002.
- Replacing Discovery search, Review conclusions, or existing retrieval
  selection rules.

## Planning rule

Each Stage is a bounded work slice. Split a checklist item before
implementation when its admitted boundary is materially larger than a focused
work session (target: approximately six hours or less). The checklist, not the
number of headings here, records completion.

## Stages

### Stage 9.1: Shared evidence, authority, and projection contract

Checklist basis: `P9-01` through `P9-06`.

Freeze the composition/provider/human split, Dashboard source inventory,
canonical-source authority, Canonical Component Design Model boundary,
projection semantics, shared attribution/absence rules, and required
design/specification promotion before implementation.

### Stage 9.2: Canonical composition plan

Checklist basis: `P9-10` through `P9-14`.

Implement the typed transient plan, deterministic coverage projection, explicit
human decision admission, advisory-provider limitations, and executable
specifications without storage or application-generation behavior.

### Stage 9.3: Dashboard foundation and Content

Checklist basis: `P9-20` through `P9-23`.

Implement one deterministic Component Dashboard projection, Content Overview,
and an authorized Web entry with no local source heuristics or dead links.

### Stage 9.4: Communication and analysis projections

Checklist basis: `P9-30` through `P9-33`.

Implement the stakeholder-facing Mono-Koto Analysis projection and Use Case
projection, including stable links from Mono to structural semantics and Koto
to behavioral semantics. Preserve source attribution and expose ambiguity or
missing semantics rather than synthesizing a complete analysis model.

### Stage 9.5: Static engineering projections

Checklist basis: `P9-40` through `P9-43`.

Implement Entity, Structure, and Classification projections only from published
normalized metadata. Preserve identity, ownership, lifecycle, cardinality,
navigability, generalization, trait, and powertype semantics and report missing
upstream metadata as explicit gaps.

### Stage 9.6: Dynamic engineering projections

Checklist basis: `P9-50` through `P9-53`.

Implement Event, Workflow, and StateMachine projections with explicit
cause/consequence and affected-entity links, plus stable forward and reverse
navigation. Do not reconstruct missing semantics from names or diagram layout.

### Stage 9.7: Cross-view feedback and evidence integration

Checklist basis: `P9-60` through `P9-63`.

Connect analysis and engineering projections, identify detailed-model impact of
stakeholder-facing changes, preserve lifecycle evidence, and integrate Usage,
Operation, and Quality/Review while retaining canonical ownership. Produce the
stable semantic identities and candidate/diff handoff needed by Phase 10, but
do not persist the continuation package here.

### Stage 9.8: Validation and closure

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-9-checklist.md` with
  reproducible evidence.

Checklist basis: `P9-70` through `P9-73`.

Run proportionate validation, synchronize the documentation record, account for
deferred work, and close only after final review and every ledger item is
complete or explicitly relocated. Closure records Phase 10 as the successor for
DEV-CBD-002 durability and continuation work.

## Cross-project dependencies

Cozy Phase 54 owns machine-readable semantic component metadata required by the
Canonical Component Design Model and its Entity, Structure, Classification,
Event, Workflow, StateMachine, and Use Case projections. Mono-Koto projection
may require additional semantic grouping/label metadata; if Cozy does not
publish sufficient information, Phase 9 must record the missing contract as an
upstream gap rather than infer it locally.

CNCF Phase 72 owns runtime lifecycle semantics and attributable runtime
evidence. Missing upstream contracts remain explicit gaps; they do not authorize
a CBD Support workaround.

Phase 10 consumes the accepted Phase 9 semantic/model contracts. A Phase 10
storage need does not by itself authorize Phase 9 to add a parallel semantic
model or infer missing upstream metadata.
