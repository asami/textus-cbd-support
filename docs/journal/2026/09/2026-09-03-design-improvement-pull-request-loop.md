# CML Design Improvement Pull Request Loop

**Date:** 2026-09-03  
**Repository:** `textus-cbd-support`  
**Status:** Discussion / design direction

## Purpose

Extend the Design Support / Model Visualization direction with a Git-based improvement loop. In `textus-cbd-support`, the canonical design source is principally CML. Visualization and review should therefore not become an alternative editable source of truth. Instead, they should project the current design, identify findings, propose changes to CML, validate the candidate design, and deliver accepted proposals as Git pull requests.

The intended loop is:

```text
Canonical CML
    ↓ analyze / build
Component Design Model
    ├─ Visualization
    └─ Review
         ↓
       Finding
         ↓
   Design Guidance
         ↓
 Proposed CML Patch
         ↓
Candidate Design Model
         ↓
     Design Diff
         ↓
 Candidate Review
         ↓
 Git Branch / Pull Request
         ↓
    Human Review
         ↓
       Merge
         ↓
 Build / Analyze / Re-review
```

## Canonical-source principle

CML remains the canonical source for the component design where the design is expressible in CML. The Design Support Web UI should not silently persist an independent mutable model that can diverge from CML.

Other inputs such as runtime metadata, CAR metadata, generated definitions, implementation artifacts, tests, and review evidence can enrich the Component Design Model, but a proposed model change should be traced back to the canonical source that owns it.

For CML-owned design elements, the preferred correction target is a CML patch.

## Finding to proposed change

A Review Finding should be able to lead to a concrete design proposal. Representative examples include:

- change an Entity relationship from composition to aggregation;
- replace an inappropriate generalization with a Trait or Powertype classification;
- add a missing State or Transition required by a Workflow;
- split or reduce an oversized Aggregate boundary;
- add or correct a View projection;
- align a Workflow with existing Entity State Machines;
- add missing Use Case traceability.

The proposal should retain explicit traceability:

```text
Review Finding
  → affected model element
  → rationale / Design Guidance
  → canonical source location
  → proposed CML change
```

## Candidate Design Model

A proposed CML patch should be evaluated before a pull request is created whenever practical.

The system should apply the proposed change to a candidate source/model context, derive a **Candidate Design Model**, and compare it with the current model. This avoids treating textual patch generation as sufficient evidence that the design was improved.

```text
Current CML
   + Proposed Patch
          ↓
Candidate CML
          ↓
Candidate Design Model
          ↓
Visualization + Review
```

The candidate model should use the same model acquisition, visualization, analysis, and review rules as the current model.

## Design Diff

The proposal should be explained primarily as a semantic/model change, while retaining the ordinary textual Git diff underneath it.

Example:

```text
Proposed Design Change

Classification
  OrderParty
    Generalization → Trait

Aggregate
  OrderAggregate
    - Customer (composition)
    + Customer (aggregation)

State Machine
  Order
    + CancelRequested
    + Cancelled

Review
  Critical   1 → 0
  Warning    4 → 1
```

Useful Design Diff categories include:

- added / removed / changed Entities;
- Generalization / Trait / Powertype changes;
- composition / aggregation changes;
- Aggregate boundary changes;
- View participation/projection changes;
- State and Transition changes;
- Workflow participation and sequence changes;
- Use Case traceability changes;
- Operation/Event realization changes;
- Review Finding changes;
- coverage changes.

The UI should allow drill-down from a semantic change to the corresponding CML/text diff.

## Candidate Review

Before PR delivery, the candidate should be re-reviewed against relevant Design Support and Review & Assurance rules.

The candidate result should make clear whether the proposal:

- resolves the originating Finding;
- introduces new Findings;
- changes Aggregate or transaction boundaries;
- creates new coupling;
- changes Use Case / Workflow / State Machine coverage;
- changes compatibility or other non-design quality attributes.

A proposal that merely moves a problem elsewhere should not be presented as an improvement without qualification.

## Pull request delivery

After the candidate design is accepted for delivery, CBD Support may create a Git branch and pull request containing the canonical-source change.

The pull request should preserve normal Git reviewability and should include a generated summary of the model-level impact. A useful PR body can contain:

- originating Finding IDs;
- design rationale;
- semantic Design Diff;
- affected Use Cases / Workflows / Entities / Aggregates / Views;
- candidate review summary;
- remaining Findings;
- validation/build results;
- links or references to the affected CML source locations.

The pull request is a proposal, not an automatic acceptance mechanism. The canonical design changes only through the normal repository merge workflow.

## Human-in-the-loop rule

AI-assisted Design Guidance should not directly rewrite the canonical main branch as the normal path.

The preferred policy is:

```text
AI detects / explains / proposes
        ↓
Git diff + model diff
        ↓
Human reviews
        ↓
Merge establishes new canonical source
```

This preserves:

- version history;
- authorship and rationale;
- normal code/design review;
- rollback;
- branch comparison;
- reproducibility;
- auditability of AI-generated changes.

## Web Dashboard integration

The Design Support Web Dashboard can expose the improvement loop from any model-centric view.

For example, selecting a Review Finding on an Entity/Class Diagram could provide:

```text
Finding
  Cross-component composition

Affected model
  Order ◆ Customer

Guidance
  Customer has independent identity/lifecycle;
  consider aggregation/reference.

Actions
  View evidence
  Preview proposed design
  View Design Diff
  Prepare Pull Request
```

Likewise, a State Machine or Workflow Finding can be shown in its diagram context and lead to a candidate CML patch.

The Web UI remains a projection and review surface; the resulting source modification is represented as an explicit Git change.

## Branch and pull-request comparison

Because the source is Git-managed, Design Support can eventually compare two refs as models rather than only as text:

```text
main
  vs
feature/order-cancellation
```

and report:

```text
Model Changes
  + 2 States
  + 3 Transitions
  ~ 1 Aggregate relation
  + 1 Workflow step

Coverage
  Use Case coverage       82% → 91%
  Workflow consistency     2 findings → 0

Review
  Critical                 1 → 0
  Warning                  4 → 2
```

This **Design Diff Review** is a natural extension of ordinary Git review and is particularly valuable when a textual CML change has broad semantic consequences.

## Relationship to the Design Support hierarchy

The previously discussed hierarchy becomes:

```text
Design Support
├─ Model Acquisition
├─ Component Design Model
├─ Traceability
├─ Model Visualization
├─ Design Analysis
├─ Design Guidance
└─ Design Change Support
   ├─ Finding-to-source traceability
   ├─ Proposed CML Patch
   ├─ Candidate Design Model
   ├─ Design Diff
   ├─ Candidate Review
   └─ Pull Request Delivery
```

Review & Assurance remains a peer product capability, but participates in this loop by supplying Findings and by evaluating the candidate design before delivery.

## Design principle

The resulting principle is:

> **Model-driven improvement, Git-governed acceptance.**

`textus-cbd-support` should help understand, review, and improve a Component at the model level, while CML and Git remain the durable, versioned, reviewable source of design truth.