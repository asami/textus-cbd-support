# Phase 11 - Interactive View and Model Editing

Stage Status:
- Current status: PLANNED
- Predecessors: Phase 9 model/view contracts and Phase 10 durable continuation/change-gate contracts
- Owner: Textus CBD Support development
- Update rule: completion must be supported by reproducible validation evidence.

## Purpose

Phase 11 turns CBD Support views from read/review-only model projections into safe interaction surfaces for intentional model development, without making each view an independent editor or weakening canonical CML governance.

The phase introduces a shared semantic Model Edit Service and treats the CBD Support Update Palette, conversational interaction, ChatGPT Plugin/MCP, Codex MCP, and future clients as alternative Model Editing Clients.

The first end-to-end validation scenario is **solo Event Storming**: a user develops Event Storming semantics through direct operations or AI conversation, CBD Support updates the canonical object model through semantic services, and Event Storming View continuously reflects the resulting CML-backed model.

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
                           Canonical Object Model
                                    |
                         CML / projection refresh
```

Responsibilities remain explicit:

- View provides understandable projections.
- Review diagnoses quality, gaps, inconsistency, traceability, and model-strength issues.
- Edit performs explicit semantic mutation.
- Review may hand a finding to Edit, but never mutates implicitly.

## Architectural principles

1. Reference views remain projections of the canonical model.
2. Editing is an interaction layer, not a second model authority.
3. A view-specific UI action and an AI instruction converge on the same semantic edit operation.
4. Clients do not primarily rewrite CML text; CBD Support owns object-model mutation semantics.
5. Stable identity, validation, semantic diff, traceability, and canonical-source behavior remain centralized.
6. Phase 9 candidate/review contracts and Phase 10 approval/integrity/change gates are preserved.
7. ChatGPT and Codex are external Model Editing Clients, not privileged mutation paths.
8. Plugin/MCP capabilities should expose semantic model operations suitable for multiple authorized AI clients.
9. Event Storming is a validation use case; the editing architecture must be reusable across views.

## Scope

- Define common Model Edit Operation semantics and result contracts.
- Implement Model Edit Service over the canonical object model.
- Provide operation discovery/applicability so clients can determine legal edits in context.
- Return validation consequences and semantic diff suitable for existing review/approval flows.
- Add an Update Palette to reference views without coupling model semantics to UI widgets.
- Support direct commands for common deterministic edits.
- Support conversational edit intent through an adapter to common edit operations.
- Expose appropriate read/edit capabilities through MCP and the ChatGPT Plugin integration boundary.
- Validate Codex as an MCP-based external editing client.
- Implement solo Event Storming as the first end-to-end scenario.
- Reuse the same editing foundation for Mono-Koto Analysis and at least one engineering-oriented view.
- Document security, authorization, confirmation, approval, and audit expectations for external editing clients.

## Non-goals

- Building an independent Event Storming storage model.
- Building a separate mutation engine for each view.
- Allowing AI clients to bypass semantic validation or approval gates.
- Making conversational interaction mandatory for simple edits.
- Replacing direct CML editing for expert users where it remains appropriate.
- Requiring CBD Support to embed every AI capability that can instead be supplied by an authorized external client.

## Stages

Each stage should remain within the project's normal approximately six-hour work-unit target; split further before implementation when necessary.

### Stage 11.1 - Edit operation contract

Define semantic operation identity, parameters, target identity, preconditions, validation result, semantic diff, failure behavior, and idempotency/concurrency expectations.

Representative operations should cover Mono-Koto and Event Storming needs without hard-coding those views into the generic service.

### Stage 11.2 - Model Edit Service

Implement the service boundary over the canonical object model. Prove that mutations preserve stable identities and flow through established CML persistence/projection rules.

### Stage 11.3 - Update Palette and direct commands

Add the contextual Update Palette to reference views. Implement direct deterministic operations such as adding a Mono/Koto and representative Event Storming elements/relationships.

The palette must remain a thin client of Model Edit Service.

### Stage 11.4 - Conversational edit adapter

Translate conversational edit intent into proposed/common Model Edit Operations. Compound instructions may produce an ordered operation set. Ambiguous or unsafe edits must remain proposals requiring the applicable confirmation/review/approval path.

### Stage 11.5 - Plugin/MCP external editing boundary

Expose semantic model inspection, applicable-operation discovery, proposed edits, and permitted edit execution through MCP/Plugin-facing capabilities.

Validate that ChatGPT and Codex can operate as external editing palettes without direct repository/CML mutation being required as the normal path.

### Stage 11.6 - Solo Event Storming end-to-end validation

Support a solo Event Storming session in which the user can:

- inspect an Event Storming View derived from CML/object-model semantics;
- add/refine Event Storming elements using direct commands;
- instruct an AI conversationally;
- use an external ChatGPT/Codex client through Plugin/MCP;
- observe model changes reflected back into Event Storming View;
- invoke Review to identify missing or weak semantics;
- transition a Review finding into an explicit Edit flow.

The implementation must reuse existing Actor/Use Case, Entity/Event, Workflow, glossary/Mono-Koto, and related semantic identities rather than introduce Event Storming-only duplicates.

### Stage 11.7 - Cross-view reuse

Apply the same edit foundation to Mono-Koto Analysis View and at least one engineering-oriented view such as Workflow or Structure. Confirm that no view-specific mutation architecture is required.

### Stage 11.8 - Governance and closure

Validate authorization, external-client boundaries, semantic diff, review/approval gates, durable continuation, auditability, failure recovery, and documentation. Close only when the end-to-end scenarios are reproducible.

## Acceptance scenario

```text
User opens Event Storming View
  -> reference projection is generated from canonical object model
  -> user adds/refines elements through direct Update Palette commands
  -> user asks ChatGPT/Codex to improve a scenario using additional authorized context
  -> external client discovers and invokes CBD Support semantic edit operations
  -> Model Edit Service produces candidate/model change and semantic diff
  -> applicable Phase 9/10 review/approval/change gate is honored
  -> canonical object model/CML is updated
  -> Event Storming View refreshes from the model
  -> Review identifies a missing relationship
  -> user explicitly chooses Fix/Edit
  -> same Model Edit Service resolves the finding
  -> session can be durably continued under Phase 10 rules
```

## Continuity with Phase 9 and Phase 10

Phase 11 does not supersede the existing model-governance path. It adds new clients and interaction affordances in front of it.

Phase 9 supplies canonical model identities, projections, candidate design, semantic diff, and review boundaries. Phase 10 supplies durable state, exact-content approval, drift handling, and the CML change gate. Phase 11 supplies intentional semantic editing interactions and external AI client access to those governed capabilities.

## Planning sources

- `docs/notes/interactive-view-model-editing.md`
- `docs/journal/2026/09/2026-09-10-interactive-view-model-editing.md`
- `docs/journal/2026/09/2026-09-09-mono-koto-analysis-view.md`
- `docs/journal/2026/09/2026-09-10-workflow-dual-projection-and-participant-derivation.md`
- `docs/phase/phase-9.md`
- `docs/phase/phase-10.md`
