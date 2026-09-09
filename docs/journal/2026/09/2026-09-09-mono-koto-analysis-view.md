# Mono-Koto Analysis, Terminology, and Event Storming Views

Date: 2026-09-09
Updated: 2026-09-10

## Context

CBD Support should provide stakeholder-facing analysis views over the same canonical semantic/design model used by engineering views. Analysis and design are not separate models connected by a one-way transformation.

The analysis family now consists primarily of:

- Mono-Koto Analysis: conceptual/domain overview and terminology bridge.
- Use Case: actor, goal, and stakeholder intent view.
- Event Storming: behavioral overview organized around events and causal progression.

## Projection Principle

```text
                    Terminology / BoK
                           |
                    domain vocabulary
                           |
                           v
             Communication / Analysis Views
              /             |              \
      Mono-Koto          Use Case       Event Storming
              \             |              /
               \            |             /
                 Canonical Design Model
                  /                  \
          Static Views            Dynamic Views
        Entity / Structure       Event / Workflow
        / Classification         / StateMachine
```

All views are projections of shared semantics. None is an independently authoritative model. Changes made through a stakeholder-facing view are semantic change proposals that must be reconciled with canonical source and the existing candidate-design/Git-governed acceptance loop.

## Mono-Koto Analysis

Mono-Koto is the stakeholder-facing conceptual overview and terminology bridge.

A Mono is not mechanically one Entity. It may summarize an Entity, Value, Aggregate, structural relations, or other related structural semantics.

A Koto is not mechanically one Event. It may summarize Use Case intent, Command, Domain Event, Workflow activity, state effect, or related behavioral semantics.

Example:

```text
Mono: 注文
  <-> Term: 注文 / Order
  <-> Order : AggregateRoot
      OrderLine : Entity
      OrderStatus : Value

Koto: 注文する
  <-> Term: 注文する / Place Order
  <-> Use Case: Place Order
      PlaceOrder : Command
      OrderPlaced : DomainEvent
      Order Placement : Workflow
```

Engineering vocabulary should normally be hidden until drill-down.

## Terminology / BoK Integration

Terminology is not merely a display-label dictionary. A domain term is a semantic anchor that may be cross-referenced with multiple model elements.

Representative information includes:

```text
Term
  - preferred name
  - aliases / alternative expressions
  - definition
  - language / localized labels where available
  - Mono / Koto relevance
  - related Use Cases
  - related Entities / Values / Aggregates
  - related Commands / Events
  - related Workflows
```

CBD Support should support navigation questions such as:

- Where is this domain term used in the design?
- What stakeholder/domain term describes this Entity?
- Which events and workflows realize this Koto?
- Which terminology is relevant to this Workflow?

Terminology evidence must remain attributable. Similar names or AI suggestions must not silently merge concepts. Possible synonyms such as `注文`, `受注`, and `オーダー` should be represented as synonym candidates or terminology inconsistencies until authoritative evidence or an explicit human decision resolves them.

Mono-Koto therefore serves as the primary bridge between stakeholder vocabulary / BoK and engineering semantics.

## Event Storming View

Event Storming is a stakeholder-facing behavioral overview. It is not merely another rendering of Event Model because it crosses several engineering projections.

Conceptually:

```text
Actor
  -> Command
  -> Aggregate / Entity
  -> Domain Event
  -> Policy / Reaction
  -> Command
  -> Domain Event
```

Representative mappings are:

```text
Event Storming concept   Semantic source
Actor                    Use Case Actor
Command                  Command / Operation semantics
Aggregate                Entity Model
Domain Event             Event Model
Policy / Reaction        Workflow semantics
External System          Component / external dependency
Read Model               Query / View semantics
Hotspot                  Issue / unresolved semantic question
```

Actor identity should normally derive from admitted Use Case semantics rather than being independently invented by the Event Storming projection.

Hotspots are analysis/review observations, not canonical domain facts.

## Relationship to Workflow and Flowchart

Event Storming, Flowchart, Workflow, and StateMachine have distinct purposes:

```text
Event Storming
  stakeholder/domain view of what happens, why, and what follows
       |
       v
Flowchart
  intentionally approachable, potentially non-faithful Workflow presentation
       |
       v
Workflow
  faithful engineering view of behavioral progression and control semantics
       |
       v
StateMachine
  detailed lifecycle view of an affected subject
```

This is a conceptual refinement order, not a transformation pipeline. All views project from shared admitted semantics.

A Flowchart view may simplify or omit technical detail for communication. It must therefore disclose that it is not necessarily a faithful representation of every Workflow semantic detail. Workflow remains the precise engineering projection.

## Mono-Koto and Event Storming Navigation

Mono-Koto provides the conceptual map; Event Storming expands behavioral concepts into causal/event progression.

Example:

```text
Mono-Koto
  Mono: 注文
  Koto: 注文する
  Koto: 支払う
  Koto: 発送する

       drill down: 注文する

Event Storming
  Customer
    -> PlaceOrder
    -> Order
    -> OrderPlaced
    -> ReserveStock
    -> StockReserved
```

Reverse navigation is equally important:

```text
OrderPlaced
  -> Koto: 注文する
  -> Mono: 注文
  -> Term: 注文 / Order
  -> Entity/Aggregate: Order
```

This supports stakeholder review without losing traceability to engineering semantics.

## View Roles

```text
Communication / Analysis
  - Mono-Koto Analysis      domain world / terminology overview
  - Use Case                actor, goal, stakeholder intent
  - Event Storming          event and causal behavioral overview

Engineering Bridge
  - Entity Model            precise structural/domain semantics
  - Event Model             precise command/event semantics

Detailed Engineering
  - Structure               composition / aggregation / association
  - Classification          generalization / trait / powertype
  - Workflow                precise behavioral progression
  - Flowchart               approachable projection of Workflow
  - StateMachine            entity/subject lifecycle detail
```

These are presentation and interaction roles, not separate underlying models.

## Stakeholder Feedback

Mono-Koto, Use Case, Event Storming, and Flowchart remain useful after detailed design begins. Corrections made through them should identify affected canonical semantics and become explicit semantic change proposals.

For example, changing the perceived relation between `注文` and `支払う`, or inserting a missing event in Event Storming, may identify impacts on:

- terminology links,
- Order state machine,
- Payment workflow,
- commands and domain events,
- Use Case actor/goal relations.

CBD Support may propose the corresponding canonical-source change and show analysis/engineering diffs, but stakeholder-facing views do not directly rewrite authoritative source.

## Design Direction

CBD Support should evolve so that:

1. one canonical semantic/design model supplies shared semantic identity;
2. terminology/BoK provides attributable domain vocabulary linked to those identities;
3. Mono-Koto is the conceptual overview and terminology bridge;
4. Use Case supplies actor and goal semantics;
5. Event Storming is a cross-model behavioral overview built from Use Case, Entity, Event, Workflow, and related semantics;
6. Entity and Event views bridge stakeholder analysis to engineering precision;
7. Workflow is faithful while Flowchart may intentionally simplify for human communication;
8. StateMachine deepens lifecycle semantics;
9. all views support stable cross-navigation; and
10. edits in stakeholder-facing views enter the semantic proposal and Git-governed design-change loop rather than creating parallel sources of truth.
