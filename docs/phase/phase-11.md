# Phase 11 - Interactive View and Model Editing

Stage Status:
- Current status: PLANNED
- Predecessors: Phase 9 model/view contracts and Phase 10 durable continuation/change-gate contracts
- Owner: Textus CBD Support development
- Update rule: completion must be supported by reproducible validation evidence.

## Purpose

Phase 11 turns CBD Support views from read/review-only model projections into safe interaction surfaces for intentional model development, without making each view an independent editor or weakening canonical CML governance.

The phase introduces a shared semantic Model Edit Service and treats the CBD Support Update Palette, conversational interaction, ChatGPT Plugin/MCP, Codex MCP, and future clients as alternative Model Editing Clients.

Editing is explicitly **provisional**. Model Editing Clients develop a Candidate Object Model. The candidate is projected through normal Views, inspected by the user, optionally reviewed, and only an explicitly approved exact candidate revision may be promoted through the existing canonical change gate.

The first end-to-end validation scenario is **solo Event Storming**.

## Interaction model

```text
                 Reference View
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

Responsibilities remain explicit:

- View provides understandable canonical or clearly marked candidate projections.
- Review diagnoses quality, gaps, inconsistency, traceability, and model-strength issues.
- Edit changes candidate state, not canonical source directly.
- Review may hand a finding to Edit, but never mutates implicitly.
- Approval promotes an exact candidate state through the canonical change gate.

## Provisional editing lifecycle

The mandatory editing lifecycle is:

```text
provisional edit(s)
    -> Candidate Object Model
    -> View confirmation
    -> Semantic Diff / Review as needed
    -> approval bound to exact candidate revision/hash
    -> canonical promotion
```

Multiple provisional edits may be accumulated in an Edit Session. Approval is not required after every small operation; it is required before canonical promotion. Rejecting or abandoning the session leaves canonical CML unchanged.

Candidate state must be visibly distinguishable from canonical state in relevant views. A user must be able to inspect what the model will look like, not merely approve a textual operation list.

## Architectural principles

1. Reference views remain projections of canonical or explicitly identified candidate model state.
2. Editing is an interaction layer, not a second model authority.
3. A view-specific UI action and an AI instruction converge on the same semantic edit operation.
4. Edit operations target candidate state; canonical mutation is a separate approval-gated promotion.
5. Clients do not primarily rewrite CML text; CBD Support owns object-model mutation semantics.
6. Stable identity, validation, semantic diff, traceability, candidate revision identity, and canonical-source behavior remain centralized.
7. Phase 9 candidate/review contracts and Phase 10 approval/integrity/change gates are preserved.
8. ChatGPT and Codex are external Model Editing Clients, not privileged mutation paths.
9. Plugin/MCP capabilities should expose semantic model operations suitable for multiple authorized AI clients.
10. Event Storming is a validation use case; the editing architecture must be reusable across views.

## Scope

- Define common Model Edit Operation semantics and result contracts.
- Define Edit Session and Candidate Object Model identity/lifecycle.
- Implement Model Edit Service over candidate object-model state.
- Provide operation discovery/applicability so clients can determine legal edits in context.
- Support multiple provisional edits before confirmation.
- Project candidate state through existing View machinery with clear provisional marking.
- Return semantic diff and validation/review consequences against canonical baseline.
- Bind approval to an exact candidate revision/hash and reject stale approval.
- Promote only approved exact candidate state through the Phase 10 canonical change gate.
- Add an Update Palette to reference views without coupling model semantics to UI widgets.
- Support direct commands for common deterministic edits.
- Support conversational edit intent through an adapter to common edit operations.
- Expose appropriate candidate read/edit capabilities through MCP and the ChatGPT Plugin integration boundary.
- Validate Codex as an MCP-based external editing client.
- Implement solo Event Storming as the first end-to-end scenario.
- Reuse the same editing foundation for Mono-Koto Analysis and at least one engineering-oriented view.
- Document security, authorization, confirmation, approval, audit, abandonment, and recovery expectations.

## Non-goals

- Building an independent Event Storming storage model.
- Building a separate mutation engine for each view.
- Committing every edit operation immediately to canonical CML.
- Requiring approval after every small provisional edit.
- Allowing AI clients to bypass View confirmation, semantic validation, or applicable approval gates.
- Making conversational interaction mandatory for simple edits.
- Replacing direct CML editing for expert users where it remains appropriate.
- Requiring CBD Support to embed every AI capability that can instead be supplied by an authorized external client.

## Stages

Each stage should remain within the project's normal approximately six-hour work-unit target; split further before implementation when necessary.

### Stage 11.1 - Edit Session and operation contract

Define Edit Session, Candidate Object Model identity/revision, semantic operation identity, parameters, target identity, preconditions, validation result, semantic diff, failure behavior, abandonment, and idempotency/concurrency expectations.

### Stage 11.2 - Candidate Model Edit Service

Implement the service boundary over candidate object-model state. Prove that provisional mutations preserve stable identities without modifying canonical CML. Define exact candidate revision/hash used by later approval.

### Stage 11.3 - Candidate projection and confirmation

Project candidate state through existing View machinery. Clearly distinguish provisional content from canonical content and provide semantic diff/review context. Support confirmation of an accumulated candidate model rather than only individual operations.

### Stage 11.4 - Update Palette and direct commands

Add the contextual Update Palette to reference views. Implement direct deterministic operations such as adding a Mono/Koto and representative Event Storming elements/relationships. All commands update the current candidate Edit Session.

### Stage 11.5 - Conversational edit adapter

Translate conversational edit intent into candidate Model Edit Operations. Compound instructions may produce an ordered operation set. Ambiguous or unsafe edits remain proposals. AI interaction must not imply canonical promotion.

### Stage 11.6 - Plugin/MCP external editing boundary

Expose semantic model inspection, candidate-session creation/read, applicable-operation discovery, provisional edits, candidate View/diff inspection, and permitted approval/promotion boundaries through MCP/Plugin-facing capabilities.

Validate that ChatGPT and Codex can operate as external editing palettes while canonical promotion remains explicitly approval-gated.

### Stage 11.7 - Solo Event Storming end-to-end validation

Support a solo Event Storming session in which the user can:

- inspect an Event Storming View derived from CML/object-model semantics;
- start an Edit Session;
- add/refine Event Storming elements using multiple provisional direct commands;
- instruct an AI conversationally;
- use an external ChatGPT/Codex client through Plugin/MCP;
- inspect the accumulated candidate in Event Storming View;
- compare candidate and canonical state;
- invoke Review to identify missing or weak semantics;
- continue refining the same candidate;
- approve an exact candidate revision only after View confirmation;
- promote the approved candidate through the canonical change gate.

The implementation must reuse existing Actor/Use Case, Entity/Event, Workflow, glossary/Mono-Koto, and related semantic identities rather than introduce Event Storming-only duplicates.

### Stage 11.8 - Cross-view reuse and closure

Apply the same edit-session/candidate foundation to Mono-Koto Analysis View and at least one engineering-oriented view such as Workflow or Structure. Validate authorization, external-client boundaries, semantic diff, exact-state approval, durable continuation, auditability, abandonment, failure recovery, and documentation.

## Acceptance scenario

```text
User opens Event Storming View
  -> reference projection is generated from canonical object model
  -> user starts an Edit Session
  -> direct Update Palette commands create provisional changes
  -> user asks ChatGPT/Codex to refine the same candidate using additional authorized context
  -> external client invokes CBD Support semantic candidate-edit operations
  -> Candidate Object Model accumulates the edits
  -> candidate Event Storming View is rendered with provisional state clearly marked
  -> semantic diff and optional Review are inspected
  -> user makes further provisional edits if needed
  -> user confirms the resulting View
  -> approval is bound to the exact candidate revision/hash
  -> Phase 10 canonical change gate verifies applicable approval
  -> approved candidate is promoted to canonical object model/CML
  -> Event Storming View refreshes from canonical state
  -> session can be durably continued under Phase 10 rules
```

The phase is not complete if a normal Update Palette or AI edit can silently bypass the candidate/View/approval lifecycle.

## Continuity with Phase 9 and Phase 10

Phase 11 does not supersede the existing model-governance path. It operationalizes it for interactive model development.

Phase 9 supplies canonical model identities, projections, Candidate Design Model, semantic diff, and review boundaries. Phase 10 supplies durable state, exact-content approval, drift handling, and the CML change gate. Phase 11 supplies Edit Sessions, interactive candidate mutation, candidate View confirmation, and external AI client access to those governed capabilities.

The resulting invariant is:

**Edit develops candidate state; View confirms candidate state; Approval authorizes exact candidate state; the canonical gate alone promotes it.**

## Planning sources

- `docs/notes/interactive-view-model-editing.md`
- `docs/journal/2026/09/2026-09-10-interactive-view-model-editing.md`
- `docs/journal/2026/09/2026-09-09-mono-koto-analysis-view.md`
- `docs/journal/2026/09/2026-09-10-workflow-dual-projection-and-participant-derivation.md`
- `docs/phase/phase-9.md`
- `docs/phase/phase-10.md`
