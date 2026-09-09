# Component Dashboard Content Model

Date: 2026-09-07
Updated: 2026-09-10

## Context

Textus CBD Support has three major product functions: Discovery finds components and attributable evidence; Review evaluates a CAR; Dashboard provides a human-oriented overview of a component. Dashboard projections must not invent new facts or conclusions; they project existing component model, catalog, runtime, BoK, and Review evidence.

## Dashboard Views

The Component Dashboard separates Content, Usage, Operation, and Quality. Content is a first-class semantic view rather than a Review subsection.

## Content View

Content View summarizes purpose/responsibility, major domain concepts and capabilities, representative Use Cases, important domain rules, external interfaces/events, and related knowledge. It is a compressed semantic projection, not a complete specification listing.

From Content View the user can navigate to DomainModel View and Use Case View.

## DomainModel View

DomainModel View distinguishes static and dynamic perspectives while preserving navigation among them.

### Static Model

#### Structure View

Structure View visualizes domain topology, ownership, and lifecycle semantics. Entity, Value, Aggregate, composition, aggregation, and association are first-class and semantically distinct. Composition expresses strong ownership/lifecycle dependence; aggregation expresses whole-part membership with independent part lifecycle; association carries no ownership semantics.

The view should expose roles, cardinality, navigability, ownership, independent creation/deletion, reassignment policy, lifecycle propagation, aggregate/persistence boundaries, reference integrity, and consistency with StateMachine lifecycle behavior.

#### Classification View

Classification View combines generalization, trait, and powertype so users can understand essential specialization, cross-cutting characteristics, and explicit classification dimensions together.

### Dynamic Model

Workflow is the primary overview of the dynamic model. StateMachine is the deep-dive view for exact lifecycle semantics of an individual modeled subject.

#### Workflow semantic basis

CBD Support does not define an independent activity-oriented Workflow language. Canonical Workflow semantics come from admitted CML/Cozy model metadata and CNCF runtime contracts. Workflow is StateMachine-based, reusing Composite/Sub StateMachine structure while adding Workflow-specific semantics such as dedicated WorkflowInstance identity, Component ownership/persistence, SubWorkflow composition, Operation/Job linkage, failure/recovery evidence, and related semantic metadata.

A Workflow may reference a local Workflow or a Workflow exported by a dependent Component as a SubWorkflow. Each SubWorkflow retains its own semantic identity and WorkflowInstance/runtime identity where runtime evidence is available.

Workflow semantic metadata should expose, where published by authoritative sources:

- purpose/goal;
- related Use Cases;
- related domain subjects;
- SubWorkflows;
- related Operations, Jobs, Events, StateMachines, and transitions; and
- source/provenance identity.

Actors are not duplicated as Workflow-owned facts. Human/system participants are derived through `Workflow -> related Use Case -> Actor`. The Dashboard should therefore be able to answer questions such as "who are the human participants related to this Workflow?" and show the Use Cases through which each participant is related.

#### Workflow View modes

Workflow View provides two selectable projections of the same canonical Workflow semantics.

**Flowchart View** is the default human-oriented overview, especially for non-engineering stakeholders. It presents major progression, decisions, events, operations, and SubWorkflows in a familiar flowchart-like form. It may omit, aggregate, or simplify StateMachine details. It is explicitly a non-authoritative explanatory projection and need not be reversible or semantically complete.

The rule is: a simplified view may be incomplete, but it must not invent unsupported semantics. When a detail cannot be represented safely, the view should omit it, mark the simplification/ambiguity, or direct the user to the engineering view.

**StateMachine View** is the semantically faithful engineering projection of the Workflow. It exposes admitted State, Transition, Trigger/Event, Guard/Predicate, Action/Effect, SubWorkflow/composite structure, Operation/Job relations, and related model/runtime evidence.

Both modes preserve the same canonical semantic identities. Selection should survive mode switching where a stable mapping exists. A SubWorkflow may appear as an expandable/collapsible process box in Flowchart View and as Composite/Sub StateMachine structure in StateMachine View.

Representative structure:

```text
Workflow
  +-- Flowchart View       human-oriented, simplified
  +-- StateMachine View    engineering, semantically faithful
```

#### Failure and recovery projection

When authoritative metadata/evidence exists, Workflow views may expose failure and recovery semantics without inventing runtime conclusions. `WorkflowFailed` represents execution failure. `CompensationRequired` represents the stronger condition that consistency cannot be guaranteed and recovery/compensation is an explicit obligation. Manual recovery is a first-class operational path when programmatic compensation cannot safely complete.

Flowchart View may summarize these as failure/recovery paths. StateMachine View should preserve the exact admitted transition/event semantics. Runtime obligations should link to Operation/Quality/Review evidence where appropriate.

#### Entity StateMachine Detail

From Workflow, SubWorkflow, or an affected domain element, the user can drill into its Entity StateMachine. StateMachine Detail exposes states, transitions, triggers, guards, actions, related Workflow semantics, operations, events, and business rules.

Representative navigation is therefore a graph rather than a mandatory linear chain:

```text
Use Case <-> Workflow <-> SubWorkflow
                    +-> Operation / Job
                    +-> affected StateMachine <-> Entity
```

Reverse navigation is required where stable metadata permits it.

## Use Case View

Use Case View explains what an actor is trying to accomplish with the component. Use Case describes an externally meaningful goal; Workflow describes the business-process progression realizing or supporting that goal; Entity StateMachine describes how an individual modeled subject changes during that work.

Use Case View should show actor, goal, preconditions, trigger, main and important alternative/exception flows, postconditions, participating domain elements, operations, events, and collaborating components/systems. A Use Case links to realizing/supporting Workflows and Domain Model elements, with reverse links where stable identities permit.

## Cross-View Navigation

Representative paths include:

```text
Content -> DomainModel -> Structure -> Entity
Content -> DomainModel -> Classification
Content -> DomainModel -> Workflow -> Flowchart / StateMachine View
Content -> Use Case -> Workflow -> affected StateMachine -> Entity
Entity -> StateMachine -> Workflow -> Use Case -> Actor
Workflow -> related Use Cases -> human Actors
```

This makes the Dashboard a component knowledge page rather than a management console or API reference.

## Project Boundary

Textus CBD Support owns Dashboard projection and navigation behavior. It consumes model metadata and runtime evidence from authoritative sources rather than redefining CML or CNCF semantics locally.

Cozy must preserve/publish sufficient semantic metadata for Structure, Classification, Workflow, StateMachine, Use Case, purpose, relationships, and stable identities. CNCF provides/enforces runtime semantics and attributable runtime evidence. CBD Support may simplify those semantics for presentation but must not create a second Workflow model or treat a flowchart projection as authoritative runtime state.