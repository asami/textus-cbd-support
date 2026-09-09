# Component Dashboard Content Model

Date: 2026-09-07

## Context

Textus CBD Support should be understood as having three major product functions:

- Discovery finds components and retrieves attributable catalog, usage, dependency, and related evidence.
- Review evaluates a CAR and produces canonical Evidence, Findings, Assurances, Unknowns, gate results, reports, and attestations.
- Dashboard provides a human-oriented overview of a component.

The Dashboard must not be limited to operational or Review status. Opening a component should make both its content and its operational condition understandable at a glance. Dashboard projections must not invent new facts or conclusions; they project existing component model, catalog, runtime, BoK, and Review evidence.

## Dashboard Views

The Component Dashboard should separate at least these concerns:

- Content: what the component means and does.
- Usage: how the component is used.
- Operation: runtime, publication, dependency, availability, and operational state.
- Quality: Review results, quality attributes, limitations, and evolution.

Content is a first-class view rather than a subsection of Review.

## Content View

Content View is the semantic overview of the component. Its purpose is to let a developer understand the component before reading CML source, API signatures, manuals, or Review details.

The initial overview should summarize:

- purpose and responsibility;
- major domain concepts;
- major capabilities;
- representative use cases;
- important domain rules;
- external interfaces and events;
- related knowledge.

Content View is a compressed semantic projection, not a complete specification listing.

From Content View the user can navigate to two principal views:

- DomainModel View
- Use Case View

## DomainModel View

DomainModel View explains what the component models. It should distinguish static and dynamic perspectives while preserving navigation among them.

### Static Model

Static Model has two principal views.

#### Structure View

Structure View visualizes domain topology, ownership, and lifecycle semantics.

First-class structural elements include Entity, Value, Aggregate, and their relationships. Composition, aggregation, and association must be represented as semantically distinct relationships rather than cosmetic diagram line styles.

Composition means strong ownership and lifecycle dependence. A composed part normally has one owning whole, cannot exist independently of that owner, cannot be arbitrarily reparented, and terminates with the owner according to the declared lifecycle policy.

Aggregation represents a whole-part or membership relationship with an independently existing part. The part may continue after removal or termination of the aggregate and may be reassigned where the model permits it.

Association represents a relationship without ownership semantics.

Structure View should expose relationship details such as roles, cardinality, navigability, ownership, independent creation and deletion, reassignment policy, and lifecycle propagation.

The value of this distinction is executable modeling. Composition and aggregation semantics should be usable to derive or validate:

- creation and deletion rules;
- exclusive versus independent ownership;
- reassignment rules;
- cascade lifecycle behavior;
- aggregate and persistence boundaries;
- command/API access paths;
- reference integrity and cardinality validation;
- consistency with StateMachine lifecycle behavior.

The intended principle is:

    Structure View = topology + ownership + lifecycle semantics

#### Classification View

Classification View visualizes the classification system of the domain. Powertype is not treated as an isolated diagram type. The view combines:

- generalization;
- trait;
- powertype.

Generalization represents essential specialization and is-a structure. Trait represents cross-cutting roles or characteristics independent of a single inheritance hierarchy. Powertype represents explicit classification dimensions and their members.

The value of Classification View is seeing these mechanisms together: how a concept is specialized, which traits cross-cut the hierarchy, and along which powertype dimensions instances are classified. Multiple independent powertype dimensions should be visible when present.

### Dynamic Model

Workflow is the primary overview of the dynamic model. StateMachine is the deep-dive view for the lifecycle of an individual modeled subject.

#### Workflow View

Workflow View shows how domain work progresses across activities and participants. It should expose the participating entities/aggregates and, where useful, the state changes caused by each activity.

Workflow therefore acts as the backbone of the dynamic model rather than merely another peer diagram.

#### StateMachine Detail

From Workflow or an affected domain element, the user can drill into its StateMachine. StateMachine Detail should expose states, transitions, triggers, guards, actions, related Workflow activities, operations, events, and business rules.

The relationship among views is:

    Use Case -> Workflow -> StateMachine -> Entity

The reverse navigation should also be possible so that a developer can start from an Entity, inspect its lifecycle, find the Workflows causing transitions, and then find the Use Cases that require those Workflows.

## Use Case View

Use Case View explains what an actor is trying to accomplish with the component. It is distinct from Workflow:

- Use Case describes an externally meaningful goal.
- Workflow describes how domain work progresses to realize that goal.
- StateMachine describes how an individual modeled subject changes during that work.

Use Case View should show actor, goal, preconditions, trigger, main flow, important alternative/exception flows, postconditions, participating domain elements, operations, events, and collaborating components or systems.

A Use Case should link to the Workflow that realizes it and to the Domain Model elements it touches. Domain Model elements should provide reverse links to their relevant Use Cases.

## Cross-View Navigation

The Dashboard should support semantic navigation rather than isolated diagrams. Representative paths are:

    Content -> DomainModel -> Structure -> Entity
    Content -> DomainModel -> Classification
    Content -> DomainModel -> Workflow -> StateMachine
    Content -> Use Case -> Workflow -> StateMachine -> Entity
    Entity -> StateMachine -> Workflow -> Use Case

This makes the Dashboard a component knowledge page rather than a management console or API reference.

## Project Boundary

Textus CBD Support owns Dashboard projection and navigation behavior. It should consume model metadata and runtime evidence from their authoritative sources rather than redefining CML semantics or CNCF runtime semantics locally.

Cozy is expected to preserve and publish sufficient model metadata for Structure, Classification, Workflow, StateMachine, and Use Case projections. CNCF is expected to provide or enforce runtime semantics where model semantics have executable consequences. Those cross-project implications are recorded separately in the respective repositories.
