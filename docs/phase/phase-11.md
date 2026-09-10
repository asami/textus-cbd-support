# Phase 11 - Interactive View and Model Editing

Stage Status:
- Current status: PLANNED
- Predecessors: Phase 9 model/view contracts and Phase 10 durable continuation/change-gate contracts
- Owner: Textus CBD Support development
- Update rule: completion must be supported by reproducible validation evidence.

## Purpose

Phase 11 turns CBD Support views from read/review-only model projections into safe interaction surfaces for intentional model development, without making each view an independent editor or weakening canonical CML governance.

The phase introduces a shared semantic Model Edit Service and treats the CBD Support Update Palette, conversational interaction, ChatGPT Plugin/MCP, Codex MCP, and future clients as alternative Model Editing Clients.

Editing is explicitly provisional. Model Editing Clients develop a Candidate Object Model. The candidate is projected through normal Views, inspected by the user, optionally reviewed, and only an explicitly approved exact candidate revision may be promoted through the existing canonical change gate.

The first end-to-end validation scenario is **solo Event Storming**, with **Actor Goal List** providing an intent-oriented starting context: Actor -> Goal -> Use Case -> Event Storming.

## Interaction model

```text
Reference View (Actor Goal List / Mono-Koto / Event Storming / ...)
                      |
       +--------------+--------------+
       |              |              |
      View          Review          Edit
       |              |              |
   projection      findings     Update Palette
                                  /       \
                              Direct     Chat/AI
                                  \       /
                              Edit Operation
                                    |
                            Model Edit Service
                                    |
                           Candidate Object Model
                                    |
                       Candidate View + Semantic Diff
                                    |
                         User Confirmation / Review
                                    |
                         exact-state Approval
                                    |
                         Canonical Change Gate
                                    |
                           Canonical Model / CML
```

## Provisional editing lifecycle

The mandatory lifecycle is:

**provisional edit(s) -> Candidate Object Model -> View confirmation -> Semantic Diff/Review -> exact-state approval -> canonical promotion**.

Multiple provisional edits may be accumulated in an Edit Session. Candidate state must be visibly distinguishable from canonical state. Rejecting or abandoning the session leaves canonical CML unchanged.

## Actor Goal List interaction

Actor Goal List uses the same editing architecture as every other projection. It must reuse Phase 9 Use Case Actor identity and must not introduce a view-local Actor model.

Representative operations include:

- add/refine a Goal for an existing Actor;
- associate/disassociate Goal and Use Case where semantically permitted;
- inspect Goal realization through related Use Case, Mono-Koto, and Event Storming views;
- respond to Review findings such as a Goal without realizing Use Case or important Goal without behavioral realization.

Goal and Use Case remain distinct semantic roles: stakeholder intent versus realization. The service must not infer one-to-one equivalence merely for UI convenience.

## Architectural principles

1. Views remain projections of canonical or explicitly identified candidate model state.
2. Editing is an interaction layer, not a second model authority.
3. A view-specific UI action and an AI instruction converge on the same semantic edit operation.
4. Edit operations target candidate state; canonical mutation is a separate approval-gated promotion.
5. Actor Goal List reuses Use Case Actor identity and shared Goal/Use-Case trace semantics.
6. Clients do not primarily rewrite CML text; CBD Support owns object-model mutation semantics.
7. Stable identity, validation, semantic diff, traceability, candidate revision identity, and canonical-source behavior remain centralized.
8. Phase 9 candidate/review contracts and Phase 10 approval/integrity/change gates are preserved.
9. ChatGPT and Codex are external Model Editing Clients, not privileged mutation paths.
10. Plugin/MCP capabilities expose semantic model operations suitable for multiple authorized AI clients.
11. Event Storming is the primary E2E validation case; the architecture must be reusable across Actor Goal List, Mono-Koto, Workflow, Structure, and other views.

## Scope

- Define Edit Session, Candidate Object Model, and common Model Edit Operation contracts.
- Implement Model Edit Service over candidate state.
- Support candidate projection, semantic diff, Review, exact-state approval, and canonical promotion.
- Add Update Palette and direct commands.
- Include Actor Goal operations and Goal-to-Use-Case relationships using shared Actor identity.
- Support conversational edit intent.
- Expose candidate read/edit capabilities through MCP/Plugin boundaries for ChatGPT/Codex.
- Implement solo Event Storming starting optionally from Actor Goal List context.
- Reuse the same foundation for Actor Goal List, Mono-Koto Analysis, and at least one engineering-oriented view.
- Document authorization, confirmation, approval, audit, abandonment, and recovery expectations.

## Stages

Each stage should remain within the project's normal approximately six-hour work-unit target; split further before implementation when necessary.

### Stage 11.1 - Edit Session and operation contract

Define Edit Session, Candidate Object Model identity/revision, semantic operation identity, parameters, target identity, preconditions, validation result, semantic diff, failure behavior, abandonment, and concurrency expectations. Include representative Actor Goal and Goal-to-Use-Case operations.

### Stage 11.2 - Candidate Model Edit Service

Implement the service boundary over candidate object-model state. Prove that provisional mutations preserve stable identities, including shared Actor identity, without modifying canonical CML.

### Stage 11.3 - Candidate projection and confirmation

Project candidate state through existing View machinery, including Actor Goal List and Event Storming. Clearly distinguish provisional content and support confirmation of an accumulated candidate model.

### Stage 11.4 - Update Palette and direct commands

Add the contextual Update Palette. Implement direct deterministic operations for Actor Goal List, Mono-Koto, and representative Event Storming elements/relationships. All commands update the current candidate Edit Session.

### Stage 11.5 - Conversational edit adapter

Translate conversational edit intent into candidate Model Edit Operations. Support instructions such as refining an Actor's goals, connecting goals to Use Cases, and developing related Event Storming behavior. AI interaction must not imply canonical promotion.

### Stage 11.6 - Plugin/MCP external editing boundary

Expose semantic inspection, candidate-session operations, applicable-operation discovery, provisional edits, and candidate View/diff inspection through MCP/Plugin-facing capabilities. Validate ChatGPT and Codex as external editing palettes.

### Stage 11.7 - Actor Goal -> solo Event Storming end-to-end validation

Support a session in which the user can:

- inspect Actor Goal List using shared Use Case Actor identity;
- establish/refine Actor Goals provisionally;
- link Goals to Use Cases;
- select Goal/Use Case context and open Event Storming View;
- develop Event Storming through direct commands or ChatGPT/Codex;
- inspect both Actor Goal List and Event Storming projections of the accumulated candidate;
- use Review to identify missing Goal realization or behavioral coverage;
- refine the candidate;
- confirm the resulting Views;
- approve an exact candidate revision;
- promote it through the canonical change gate.

### Stage 11.8 - Cross-view reuse and closure

Apply the same edit-session/candidate foundation to Mono-Koto Analysis and at least one engineering-oriented view such as Workflow or Structure. Validate governance, durable continuation, auditability, abandonment, failure recovery, and documentation.

## Acceptance scenario

```text
Actor Goal List
  -> Actor: Customer
  -> provisional Goal: purchase a product
  -> link Goal to Place Order Use Case
  -> Candidate Actor Goal List View
  -> open Event Storming for that Goal / Use Case
  -> provisional Event Storming edits through palette or ChatGPT/Codex
  -> Candidate Object Model
  -> Actor Goal List + Event Storming candidate Views
  -> Semantic Diff / Review
  -> user confirms cross-view result
  -> approval bound to exact candidate revision/hash
  -> Phase 10 canonical change gate
  -> canonical object model/CML update
```

The phase is not complete if a normal Update Palette or AI edit can silently bypass the candidate/View/approval lifecycle, or if Actor Goal List creates identities inconsistent with Use Case Actor semantics.

## Continuity with Phase 9 and Phase 10

Phase 9 supplies Actor Goal List, shared Actor/Goal/Use Case semantics, canonical model identities, projections, Candidate Design Model, semantic diff, and review boundaries. Phase 10 supplies durable state, exact-content approval, drift handling, and the CML change gate. Phase 11 supplies Edit Sessions, interactive candidate mutation, candidate View confirmation, and external AI client access.

The invariant remains:

**Edit develops candidate state; View confirms candidate state; Approval authorizes exact candidate state; the canonical gate alone promotes it.**

## Planning sources

- `docs/notes/actor-goal-list-view.md`
- `docs/notes/interactive-view-model-editing.md`
- `docs/journal/2026/09/2026-09-10-actor-goal-list-view.md`
- `docs/journal/2026/09/2026-09-10-interactive-view-model-editing.md`
- `docs/phase/phase-9.md`
- `docs/phase/phase-10.md`
