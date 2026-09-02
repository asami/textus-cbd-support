# Design Support and Model Visualization

**Date:** 2026-09-03  
**Repository:** `textus-cbd-support`  
**Status:** Discussion / design direction

## 1. Background

`textus-cbd-support` already has substantial capabilities around component discovery/reuse and CAR review/assurance. The discussion identified a broader product direction: make component design itself explicitly understandable and navigable, rather than treating design only as input to review.

The proposed top-level capability structure is:

```text
Textus CBD Support
├─ Discovery & Reuse Support
├─ Design Support
└─ Review & Assurance
```

Design Support and Review & Assurance should remain distinguishable product capabilities, but they should share a common **Component Design Model**. Review evaluates that model and its realization; Design Support visualizes, analyzes, and eventually guides design using the same model.

The key principle is:

> Navigate by model, not by files.

Developers should be able to enter a component through its use cases, entities, aggregates, views, state machines, and workflows, and descend to implementation artifacts only when necessary.

## 2. Object-model foundation

The basic object-model perspective is centered on:

1. classification of objects, especially Entities;
2. composition and aggregation between objects;
3. state machines describing object lifecycle;
4. workflows describing coordination across Entities and state machines.

Entity classification is especially important. Classification should not be reduced to a conventional inheritance tree. The model must distinguish and visualize:

- **Generalization** — `is-a` subtype/supertype classification;
- **Trait** — orthogonal characteristics or capabilities that should not be forced into a single inheritance hierarchy;
- **Powertype** — explicit modeling of the classification itself, allowing classification values/types to be represented at the model level.

This distinction should make it possible to detect designs where roles, states, categories, and structural subtypes have been incorrectly mixed into one generalization hierarchy.

## 3. Entity structural relations

Entity-to-Entity structure should explicitly visualize:

- **Composition**
- **Aggregation**

Composition should carry semantic meaning beyond a UML diamond. The design model should make visible, where available:

- ownership;
- lifecycle dependency;
- cardinality;
- persistence implications;
- transaction boundary;
- Aggregate boundary;
- realization in the CAR.

Aggregation should identify independently existing Entities and make visible:

- identity independence;
- lifecycle independence;
- reference mechanism, such as EntityId;
- cross-Aggregate relationships;
- cross-Component relationships.

This enables review rules such as composition boundary violations, incorrect ownership of shared Entities, cyclic composition, excessive Aggregate size, and inappropriate cross-Component coupling.

## 4. State machines and workflows

State Machine and Workflow are separate concepts.

### State Machine

A State Machine describes the lifecycle of one Entity:

```text
Entity
└─ State Machine
   ├─ State
   ├─ Transition
   ├─ Trigger
   └─ Operation / Event
```

### Workflow

A Workflow describes business progression across one or more Entities and State Machines:

```text
Workflow
├─ participating Entities
├─ participating State Machines
├─ transition sequence
├─ Operations / Commands
├─ Events
├─ conditions
└─ Aggregate / transaction boundaries
```

A Workflow should preferably be composed by referring to existing Entity and State Machine definitions rather than duplicating their state definitions.

This makes consistency analysis possible. For example, CBD Support should eventually be able to detect that a Workflow requires a transition that does not exist in the participating Entity's State Machine.

At Component level, the dashboard should answer questions such as:

- How many kinds of Workflow does this Component contain?
- Which Entities participate in each Workflow?
- Which State Machines participate?
- Which transitions are used?
- Which Operations and Events coordinate the Workflow?
- Which Aggregate and transaction boundaries does it cross?

## 5. Aggregate and View models

Aggregate and View must be first-class visualization targets alongside Entity and Workflow.

### Aggregate Model

Aggregate visualization should show:

- Aggregate Root;
- constituent Entities;
- embedded/composed Entities;
- joined/referenced Entities;
- transaction boundary;
- related Commands;
- related Workflows.

Conceptually, Aggregate is the **write/update perspective** over the Entity model.

Example:

```text
OrderAggregate
├─ root: Order
├─ composition: OrderLine
├─ composition: ShippingAddress
├─ reference: Customer
└─ reference: Product
```

### View Model

View visualization should show:

- source Entities;
- joins;
- projection structure;
- list/detail or other read-model roles;
- related Queries;
- related Use Cases.

Conceptually, View is the **read/projection perspective** over the Entity model.

Example:

```text
Order ─────┐
OrderLine ─┼──> OrderDetailView
Customer ──┤
Product ───┘
```

Aggregate and View can therefore be presented as two overlays on the same underlying Entity graph: write/update and read/projection.

## 6. Use Cases and traceability

Use Cases should be connected to the model rather than displayed as an isolated diagram.

A useful traceability chain is:

```text
Use Case
   ↓
Workflow
   ↓
Aggregate / View
   ↓
Entity
   ↓
State / Transition
   ↓
Operation / Event
   ↓
Implementation Artifact
```

The precise path can differ for Command-oriented and Query-oriented Use Cases. For example:

```text
Command Use Case
  → Workflow
  → Aggregate
  → Entity / State transition
  → Operation / Event

Query Use Case
  → View
  → source Entities
  → Query operation
```

Traceability should make both directions navigable. A developer should be able to start with a Use Case and descend toward implementation, or select an Entity and discover its Use Cases, Workflows, Aggregates, Views, and implementation artifacts.

This also enables coverage analysis such as:

```text
Use Case: Cancel Order

Workflow       ✓
Entity         ✓
State Machine  ✕
Operation      ✓
Test           ✕
```

## 7. Component Design Model

The shared model underlying visualization, analysis, review, and guidance is tentatively organized as follows:

```text
Component Design Model
│
├─ Use Case Model
│  ├─ Actor
│  ├─ Goal
│  └─ scenario / Workflow relation
│
├─ Entity Model
│  ├─ Classification
│  │  ├─ Generalization
│  │  ├─ Trait
│  │  └─ Powertype
│  └─ Structural Relations
│     ├─ Composition
│     └─ Aggregation
│
├─ Aggregate Model
│  ├─ Aggregate Root
│  ├─ constituent Entities
│  ├─ composition / embedding
│  ├─ reference / join
│  └─ transaction boundary
│
├─ View Model
│  ├─ source Entities
│  ├─ join
│  ├─ projection
│  └─ read-model role
│
├─ State Machine Model
│  ├─ State
│  ├─ Transition
│  ├─ Trigger
│  └─ Operation / Event
│
└─ Workflow Model
   ├─ workflow type
   ├─ participating Entities
   ├─ participating State Machines
   ├─ transition sequence
   ├─ Operations / Events
   └─ transaction / Aggregate boundaries
```

The model should also carry traceability to implementation artifacts.

## 8. Design Support

Design Support is proposed as a first-class capability, not merely a new review view.

```text
Design Support
├─ Model Acquisition
├─ Component Design Model
├─ Traceability
├─ Model Visualization
├─ Design Analysis
└─ Design Guidance
```

### Model Acquisition

Potential sources include:

- CML;
- CNCF runtime metadata;
- CAR metadata;
- implementation evidence;
- generated/runtime descriptions where appropriate.

The goal is to build a normalized Component Design Model rather than binding visualization directly to individual source files.

### Model Visualization

The initial visualization scope should include all model categories identified in this discussion from the beginning, rather than adding them incrementally as unrelated diagrams:

- Use Cases;
- Entity/class model;
- Entity classification;
- composition/aggregation;
- Aggregates;
- Views;
- State Machines;
- Workflows;
- Traceability.

### Design Analysis

Likely analysis areas include:

- classification consistency;
- misuse of generalization versus trait/powertype;
- composition and ownership consistency;
- Aggregate boundary quality;
- coupling and cohesion;
- Workflow complexity;
- State Machine / Workflow consistency;
- Use Case coverage;
- transaction-boundary consistency.

### Design Guidance

A later capability can use analysis and review findings to provide:

- redesign suggestions;
- pattern recommendations;
- boundary suggestions;
- reusable Component recommendations.

## 9. Relationship with Review & Assurance

Review should not be collapsed completely into Design Support. At the product-capability level the preferred structure is:

```text
Discovery & Reuse Support
Design Support
Review & Assurance
```

However, Design Support and Review & Assurance share the Component Design Model.

```text
                 Component Design Model
                          │
         ┌────────────────┼────────────────┐
         │                │                │
  Visualization       Analysis          Review
         │                │                │
  what is designed   structural       quality and
  and how             properties       assurance
         │                │                │
         └────────────────┼────────────────┘
                          │
                       Guidance
```

This allows the existing CAR Review capability to remain intact while gaining richer model context.

A major benefit is that Review Findings can be projected directly onto model diagrams. Examples include:

- a cross-Component aggregation finding shown on the relevant Entity relationship;
- a missing transition highlighted on a State Machine;
- an excessive Aggregate boundary highlighted on the Entity graph;
- a Workflow crossing too many synchronous boundaries;
- a transition with no supporting Use Case;
- a Use Case with no test or State Machine coverage.

Thus:

- Visualization without Review helps understand the design;
- Review without Visualization identifies issues;
- Visualization plus Review explains issues in their design context.

## 10. Web Dashboard

The CNCF Web Dashboard should become the primary model-navigation surface for this capability.

A tentative navigation structure is:

```text
Dashboard
├─ Overview
├─ Use Cases
├─ Entities
├─ Aggregates
├─ Views
├─ State Machines
├─ Workflows
├─ Traceability
└─ Review
```

### Component Overview

The overview should summarize model inventory, for example:

```text
Component: Order

Use Cases       12
Entities         8
Aggregates       3
Views            6
Workflows        4
State Machines   5
Operations      27
Review Issues    3
```

The exact metrics are not fixed by this journal; the important point is that the developer can understand the model shape before opening individual diagrams.

### Entity/Class Diagram

The Entity diagram should support layer selection rather than attempting to display all semantics simultaneously.

Example controls:

```text
Classification
  [x] Generalization
  [x] Trait
  [x] Powertype

Structure
  [x] Composition
  [x] Aggregation

Context
  [ ] Aggregate boundary
  [ ] View participation
  [ ] State
  [ ] Workflow participation
  [ ] Use Case participation
  [ ] Review findings
```

This permits the same underlying model to serve multiple design questions without producing an unreadable monolithic class diagram.

### State Machine View

The State Machine view should be Entity-centered. Selecting a transition should reveal information such as:

- trigger;
- Operation;
- Event;
- Workflows using the transition;
- Use Cases supported by the transition;
- review findings;
- implementation artifacts.

### Workflow View

The Workflow view should be scenario-centered. It should visualize the participating Entities and their State Machines, and show how transitions, Operations, Commands, and Events coordinate the business flow.

### Aggregate and View overlays

The Entity graph should be reusable with Aggregate and View overlays so that the developer can understand:

- write/update boundaries;
- read/projection participation;
- transaction boundaries;
- cross-boundary references.

### Traceability View

Traceability should be navigable rather than merely rendered as a static matrix. The user should be able to traverse:

```text
Use Case → Workflow → Aggregate/View → Entity → State/Transition
         → Operation/Event → Implementation Artifact
```

and traverse the same relationships in reverse.

## 11. Total functional hierarchy

The discussion currently suggests the following overall product hierarchy:

```text
Textus CBD Support
│
├─ 1. Discovery & Reuse Support
│  ├─ Component Discovery
│  │  ├─ searchComponents
│  │  ├─ semantic requirement matching
│  │  └─ candidate ranking / selection
│  ├─ Component Information
│  │  ├─ getComponent
│  │  ├─ version selection
│  │  ├─ runtime compatibility
│  │  └─ component reference
│  ├─ Usage Guidance
│  │  ├─ getUsage
│  │  ├─ intent-aware guidance
│  │  └─ examples / manuals / artifacts
│  ├─ Dependency Resolution
│  │  └─ resolveDependencies
│  └─ Information Sources
│     ├─ Catalog
│     ├─ Local CAR
│     ├─ BoK
│     ├─ SIE
│     ├─ cache / refresh
│     └─ authentication / authorization
│
├─ 2. Design Support
│  ├─ Model Acquisition
│  ├─ Component Design Model
│  │  ├─ Use Case Model
│  │  ├─ Entity Model
│  │  │  ├─ Classification
│  │  │  │  ├─ Generalization
│  │  │  │  ├─ Trait
│  │  │  │  └─ Powertype
│  │  │  └─ Composition / Aggregation
│  │  ├─ Aggregate Model
│  │  ├─ View Model
│  │  ├─ State Machine Model
│  │  └─ Workflow Model
│  ├─ Traceability
│  ├─ Model Visualization
│  │  ├─ Component Overview
│  │  ├─ Use Case View
│  │  ├─ Entity/Class Diagram
│  │  ├─ Aggregate View
│  │  ├─ View/Projection View
│  │  ├─ State Machine Diagram
│  │  ├─ Workflow Diagram
│  │  └─ Traceability View
│  ├─ Design Analysis
│  └─ Design Guidance
│
└─ 3. Review & Assurance
   ├─ Review Execution
   │  ├─ Review Run
   │  ├─ Provider Registry
   │  ├─ Provider Execution
   │  └─ Evidence Collection
   ├─ Review Views
   │  ├─ Domain / Design View
   │  ├─ CNCF View
   │  ├─ Implementation View
   │  ├─ Security View
   │  ├─ Performance View
   │  ├─ Resilience View
   │  ├─ Observability View
   │  ├─ Testability View
   │  ├─ Documentation View
   │  ├─ AI View
   │  ├─ I18N View
   │  ├─ Cost View
   │  └─ Evolution View
   ├─ Quality Assessment
   │  ├─ quality attribute catalog
   │  ├─ quality rule matrix
   │  ├─ capability assessment
   │  └─ coverage
   ├─ Findings & Assurance
   │  ├─ Finding
   │  ├─ Severity
   │  ├─ Evidence
   │  ├─ Gate
   │  └─ Attestation
   └─ Delivery
      ├─ Web
      ├─ CLI
      ├─ MCP
      ├─ CI artifacts
      └─ persistent review history
```

## 12. Current design direction

The main decisions/directions emerging from this discussion are:

1. Introduce **Design Support** as a first-class product capability.
2. Make **Entity classification** a central design perspective, explicitly distinguishing generalization, trait, and powertype.
3. Treat **composition and aggregation** as semantically rich Entity relationships, not merely diagram notation.
4. Distinguish **State Machine** (Entity lifecycle) from **Workflow** (cross-Entity business progression).
5. Treat **Aggregate** and **View** as first-class model visualization