# Mono-Koto Analysis View and Model Projection

Date: 2026-09-09

## Context

CBD Support should provide a モノ・コト分析 (Mono-Koto Analysis) view that can be used both for domain analysis and for communication with stakeholders who do not need to understand engineering-level modeling notation.

The important design decision is that analysis and design are **not separate models connected by a one-way analysis-to-design transformation**. Instead, the analysis model is an overview/projection of the same semantic model represented in more detailed engineering views.

## Core Principle

Treat CBD Support as a set of views over a canonical semantic model.

```text
Canonical Semantic Model
        |
        +-- projection --> Mono-Koto Analysis View
        +-- projection --> Entity Model View
        +-- projection --> Event Model View
        +-- projection --> Structure View
        +-- projection --> Classification View
        +-- projection --> Workflow View
        +-- projection --> StateMachine View
```

The Mono-Koto Analysis View is therefore not merely an upstream artifact that is discarded or transformed after design begins. It remains useful throughout modeling as an overview of the detailed design.

## Mono and Koto

At the analysis level, use domain vocabulary that is understandable without engineering terminology.

```text
Mono
  顧客
  注文
  商品
  支払い

Koto
  注文する
  支払う
  出荷する
  キャンセルする
```

Mono and Koto should not be mapped mechanically one-to-one to engineering concepts.

A Mono is an overview projection of structural/domain concepts. For example, the stakeholder concept `注文` may project detailed elements such as:

```text
Order       : AggregateRoot
OrderLine   : Entity
OrderStatus : Value
```

A Koto is an overview projection of behavioral/temporal concepts. For example, `注文する` may project:

```text
PlaceOrder  : Command
OrderPlaced : DomainEvent
```

A Koto can therefore cover intent/action, occurrence/event, and process aspects rather than being equivalent to a Domain Event.

## Entity Model and Event Model Integration

The Mono-Koto view should be directly connected to the Entity Model and Event Model through the shared semantic model.

Conceptually:

```text
Mono -------------------- Entity / Value / Aggregate
  \                       Structure / lifecycle
   \
    \ shared semantics
     \
Koto -------------------- Command / Event
                          Workflow / StateMachine
```

Examples of useful detailed relationships include:

```text
OrderPlaced
  creates  -> Order
  affects  -> Inventory
  refers   -> Customer

OrderCancelled
  changes  -> Order.status
  releases -> InventoryReservation
```

The same event can appear differently in different views: as an Event in the Event Model, as part of a Koto in the Mono-Koto Analysis View, and as a transition trigger in a StateMachine View.

## Projection Rather Than One-Way Transformation

Do not define the lifecycle as:

```text
Analysis -> Design
```

Instead use:

```text
Detailed / Canonical Model
          |
          | projection / abstraction
          v
   Analysis Overview
          |
          | stakeholder/design feedback
          v
Detailed / Canonical Model
```

The semantic authority remains the shared canonical model. Analysis views and engineering views expose different abstractions of it.

This avoids maintaining two independent models and eliminates the usual analysis/design synchronization problem.

Edits made through an analysis view should be interpreted as proposed semantic changes. CBD Support can resolve or propose how those changes should be represented in detailed engineering concepts.

For example, if a user adds the stakeholder-level relation:

```text
注文 --含む--> 注文明細
```

CBD Support may propose:

```text
Order composition OrderLine
```

If the detailed relationship later changes, the Mono-Koto projection should reflect the change automatically.

## Stakeholder Communication

A primary purpose of the Mono-Koto Analysis View is communication with non-engineering stakeholders.

Engineering concepts such as `AggregateRoot`, `composition`, `Command`, `DomainEvent`, and `StateTransition` should normally be hidden in this view. The stakeholder-facing representation should use domain vocabulary and simple relationships.

For example:

```text
顧客
  |
  +-- 注文する --> 注文
                    |
                    +-- 商品を含む
                    +-- 支払われる
                    +-- 出荷される
```

Internally the same representation may correspond to Aggregate, Entity, Command, Event, Workflow, and StateMachine elements.

This makes the analysis view a **communication projection** as well as an analysis projection.

## Stakeholder Feedback Interface

The view should eventually support editing during stakeholder discussions.

For example, a discussion may reveal that the relationship between `注文` and `支払う` is incorrect. A stakeholder-level edit should not create a disconnected analysis-only model. Instead CBD Support should identify affected detailed model elements and present them to the designer, for example:

```text
Analysis-view change:
  relationship between 注文 and 支払う changed

Potentially affected design elements:
  Order state machine
  Payment workflow
  OrderPlaced / PaymentCompleted event relationship
```

This makes the Mono-Koto view a stakeholder feedback interface into the canonical model.

## View Roles

A useful conceptual grouping is:

```text
Communication / Analysis Views
  - Mono-Koto Analysis
  - Use Case
  - Conceptual Overview

Engineering Views
  - Entity Model
  - Event Model
  - Structure
  - Classification
  - Workflow
  - StateMachine
```

These groups are presentation roles, not separate underlying models.

## Design Implication for CBD Support

CBD Support should evolve toward a model in which:

1. a canonical semantic model is the shared source of semantics;
2. each modeling notation is a projection/view over that model;
3. Mono-Koto Analysis provides a deliberately simplified domain overview;
4. Entity and Event views expose more precise engineering semantics;
5. Workflow and StateMachine views expose behavioral detail;
6. changes in any editable view can be reconciled with the canonical model and reflected in the other views; and
7. the Mono-Koto view remains useful after detailed design begins, especially for stakeholder communication and whole-model validation.

This positions CBD Support not only as a tool for creating engineering models, but also as a tool for translating the same domain semantics between different levels of expertise.