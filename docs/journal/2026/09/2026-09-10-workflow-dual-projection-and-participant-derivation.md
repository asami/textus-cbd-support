# Workflow Dual Projection and Participant Derivation

Date: 2026-09-10
Status: design direction / Phase 9 input

## Decision

CBD Support aligns its Workflow view with the current Cozy/CNCF model rather than maintaining an independent activity-based Workflow semantics.

Canonical Workflow semantics are StateMachine-based. Workflow reuses Composite/Sub StateMachine structure while adding Workflow-specific semantics such as WorkflowInstance identity, Component ownership/persistence, SubWorkflow composition, Operation/Job linkage, and failure/recovery contracts.

CBD Support presents the same canonical Workflow through two selectable views:

```text
Canonical Workflow semantics
  +-- Flowchart View
  +-- StateMachine View
```

## Flowchart View

Flowchart View is optimized for human comprehension and is expected to be especially useful for non-engineering stakeholders.

It may:

- collapse several detailed states/transitions into one understandable process node;
- show guards as simple decisions;
- show events/transitions as labeled flow edges;
- show Operations/Jobs as work nodes where useful;
- show a SubWorkflow as one expandable process box;
- omit internal transitions/history/guards that do not help the overview; and
- summarize failure/recovery paths.

It is deliberately not required to be a semantically complete or reversible representation of the Workflow StateMachine.

The safety rule is:

> Flowchart View may be incomplete or simplified, but it must not invent unsupported semantics.

The UI should make its overview/simplified nature clear and provide direct switching to StateMachine View for exact engineering semantics.

## StateMachine View

StateMachine View is the semantically faithful engineering projection. It exposes the admitted Workflow State, Transition, Trigger/Event, Guard/Predicate, Action/Effect, Composite/SubWorkflow structure, Operation/Job relations, and source/runtime evidence.

The two views are not separate models. They share canonical semantic identities. Where a stable mapping exists, selecting an element and switching views should retain the corresponding selection.

## SubWorkflow

SubWorkflow uses the existing Composite/Sub StateMachine structural mechanism but retains Workflow-specific identity and runtime semantics. A Workflow may use both local Workflows and Workflows exported by dependent Components as SubWorkflows within the current single-Subsystem target.

Flowchart View may render a SubWorkflow as a collapsed/expandable process box. StateMachine View renders the corresponding composite structure precisely.

## Workflow descriptive metadata

A Workflow needs enough semantic metadata to answer not only "how does it progress?" but also "what is this Workflow for?".

CBD Support should project, when authoritative metadata is available:

- purpose/goal;
- related Use Cases;
- related domain subjects;
- SubWorkflows;
- related Operations/Jobs/Events/StateMachines; and
- provenance/source identity.

Actor is intentionally not duplicated as an authoritative Workflow property.

The semantic path is:

```text
Actor
  <-> Use Case
        <-> Workflow
```

Therefore human participants for a Workflow are derived from the Actors of its related Use Cases. This allows CBD Support to answer:

- who are the human participants related to this Workflow?;
- through which Use Case is each person/role related?; and
- which Workflows involve a given Actor?

The derivation must retain provenance to the relevant Use Case rather than presenting the Actor as a Workflow-owned fact.

## Failure / compensation / human recovery

CNCF now records a design direction distinguishing:

```text
WorkflowFailed
  -> recovery analysis
  -> consistency cannot be guaranteed
  -> CompensationRequired
  -> programmatic compensation/recovery
  -> ManualRecoveryRequired when necessary
```

CBD Support may project this information when authoritative model/runtime evidence exists. Flowchart View may summarize the recovery path for human understanding; StateMachine View preserves the exact admitted semantics. Manual recovery is treated as a first-class operational concern, not an undocumented exception path.

## Phase 9 consequence

Stage 9.6 should implement Workflow as a dual projection rather than one activity diagram. Phase 9 must preserve Workflow purpose, Use Case relations, derived Actor provenance, SubWorkflow identities, and failure/recovery links where upstream metadata permits.

Missing Cozy/CNCF metadata remains an explicit upstream gap. CBD Support must not reconstruct canonical Workflow semantics from names or flowchart layout.