# Phase 9 Workflow Projection Refinement

Status: OPEN / Phase 9 Stage 9.6 refinement
Parent: [Phase 9](phase-9.md)
Checklist: [Phase 9 Checklist](phase-9-checklist.md)
Date: 2026-09-10

## Purpose

Refine Phase 9 Stage 9.6 so CBD Support Workflow projections align with the current Cozy/CNCF StateMachine-based Workflow model while retaining a human-friendly flowchart presentation.

This refinement does not create a second Workflow model. It specializes the existing Phase 9 canonical-model/projection architecture.

## Selected direction

```text
Canonical Workflow semantics
        |
        +-- Flowchart View
        |     simplified / human-oriented / non-authoritative
        |
        +-- StateMachine View
              semantically faithful engineering projection
```

Both views use the same canonical semantic identities and should preserve selection across mode switching where stable mappings exist.

## Workflow information projected by CBD Support

Where authoritative Cozy/CNCF metadata exists, the Workflow projection includes:

- Workflow identity and owning Component;
- purpose/goal;
- related Use Cases;
- related domain subjects;
- local and dependent-Component SubWorkflow identities;
- related Operations, Jobs, Events, StateMachines, and transitions;
- source/provenance identity; and
- runtime failure/recovery evidence when available.

Actors are derived through related Use Cases rather than stored as Workflow-owned facts:

```text
Workflow -> related Use Case -> Actor
```

This must support both forward and reverse queries, including identifying the human participants related to a Workflow and explaining the Use Case through which each participant is related.

## Flowchart View

Flowchart View is intentionally approximate. It may collapse states/transitions, simplify decisions, summarize events/actions, and render SubWorkflow as an expandable process box.

It may omit information that does not fit the overview. It must not invent unsupported semantics. Ambiguous or unavailable mappings remain explicit or direct the user to StateMachine View.

## StateMachine View

StateMachine View preserves admitted Workflow State, Transition, Trigger/Event, Guard/Predicate, Action/Effect, Composite/SubWorkflow structure, Operation/Job links, and exact source/runtime evidence.

## Failure and recovery

Where authoritative evidence exists, dynamic projections may expose:

```text
WorkflowFailed
  -> CompensationRequired when consistency cannot be guaranteed
  -> programmatic recovery/compensation
  -> ManualRecoveryRequired when necessary
```

Flowchart View may summarize these paths. StateMachine View preserves exact semantics. CBD Support does not infer a compensation obligation from a generic failure when CNCF has not published that evidence.

## Checklist mapping

This refinement is implemented through:

- `P9-51` Workflow dual-projection semantic contract;
- `P9-52` selectable Flowchart and StateMachine views; and
- `P9-53` dynamic cross-view, participant, SubWorkflow, and recovery navigation.

If any upstream semantic metadata required by these items is unavailable, it is recorded as an explicit Cozy/CNCF gap rather than reconstructed locally.