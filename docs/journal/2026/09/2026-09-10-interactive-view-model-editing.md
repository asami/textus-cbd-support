# 2026-09-10 Interactive View and Model Editing Direction

## Context

CBD Support should support solo model development through reference views without turning each view into an independent editor. Event Storming is the initial end-to-end case, and Actor Goal List is added as an intent-oriented stakeholder projection that can precede it.

## Decision: View / Review / Edit

- **View** reads a projection.
- **Review** evaluates model quality and reports findings.
- **Edit** intentionally develops candidate model state through semantic operations.

The normal UI remains a reference view. Editing exposes an Update Palette with direct semantic commands and conversational instructions. Both use common Model Edit Service operations.

## Decision: provisional update -> View confirmation -> approval

Editing must not directly commit each operation to canonical CML.

```text
Edit Request
  -> Provisional Update
  -> Candidate Object Model
  -> View / Semantic Diff / Review
  -> User Confirmation
  -> Approval for exact candidate state
  -> canonical change gate
  -> Canonical Object Model / CML
```

Multiple provisional edits can be accumulated during a modeling session. Candidate state must be visibly distinguishable from canonical state. ChatGPT or Codex may construct/refine the candidate but do not gain implicit canonical commit authority.

## Actor Goal List addition

Actor Goal List is now part of the Phase 9 communication/analysis view family and participates in Phase 11 editing.

```text
Actor
  -> Goal
      -> Use Case
          -> Mono-Koto context
          -> Event Storming behavior
```

Actor identity remains owned/shared by Use Case semantics. Goal captures stakeholder intent and is linked to realizing Use Cases where supported by model/evidence; Goal and Use Case are not automatically identical.

The interactive architecture should allow direct or AI-assisted provisional operations such as adding/refining a Goal and linking it to a Use Case. The result is inspected through Candidate Actor Goal List View before approval.

This creates a useful solo-modeling progression:

```text
Actor Goal List
  -> clarify Actor goals
  -> connect goals to Use Cases
  -> inspect Mono-Koto conceptual context
  -> expand selected intent into Event Storming behavior
```

Because all are projections of the same candidate object model, changes can be checked across views rather than transformed through a one-way analysis pipeline.

Review can detect missing Goal realization, Use Cases without clear Goal motivation, or important goals without Event Storming behavioral coverage. Such findings remain diagnostics until explicitly handed to Edit.

## External AI as an editing palette

ChatGPT and Codex can act as external Model Editing Clients through Plugin/MCP. They can use additional authorized context while CBD Support owns semantic candidate mutation, validation, candidate identity, and the approval/canonical-source boundary.

## Planning consequence

Phase 11 must include Actor Goal List in direct-edit operations and cross-view validation. Solo Event Storming should be able to start from Actor -> Goal -> Use Case context. The common interaction contract remains:

**provisional edit -> candidate View confirmation -> exact-state approval -> canonical promotion**.

Projection-specific details are recorded in `docs/notes/actor-goal-list-view.md`; general editing details are in `docs/notes/interactive-view-model-editing.md`.
