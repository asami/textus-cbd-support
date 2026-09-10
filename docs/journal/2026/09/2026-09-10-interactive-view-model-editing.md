# 2026-09-10 Interactive View and Model Editing Direction

## Context

We considered how CBD Support can support a solo Event Storming workflow. The intended workflow is not a standalone sticky-note editor. Event Storming elements are expressed through the CML-backed object model, while Event Storming View is used to inspect and reason about the result.

This raised a broader question: current views are mainly reference/review surfaces, but model development also needs an intentional editing interaction.

## Decision: View / Review / Edit

Introduce editing as a distinct interaction responsibility alongside View and Review.

- **View** reads a projection.
- **Review** evaluates model quality and reports findings.
- **Edit** intentionally develops candidate model state through semantic operations.

The normal UI remains a reference view. When editing is requested, an **Update Palette** is exposed over/in the context of that view.

The palette supports two complementary usage styles:

1. direct semantic commands for known operations such as adding a Mono/Koto or Event;
2. conversational instructions for compound or exploratory changes.

These are not separate mutation implementations. Both are translated into common model-edit operations handled by CBD Support services.

## Decision: provisional update -> View confirmation -> approval

Editing must not directly commit each operation to canonical CML. The agreed lifecycle is:

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

A user should be able to accumulate multiple provisional edits during an exploratory modeling session and then confirm the resulting model through the relevant View. Approval is therefore normally associated with the exact accumulated candidate revision/hash rather than demanded after every small modeling operation.

The candidate/provisional state must be visibly distinguishable from canonical state. Rejection or abandonment of the candidate must leave canonical CML unchanged.

This applies equally to direct UI commands and AI-driven editing. ChatGPT or Codex may help construct and refine a candidate model, but this does not grant them implicit authority to promote it to canonical source.

## Service boundary

CBD Support will provide a Model Edit Service over candidate object-model state. UI and AI clients should invoke semantic operations rather than directly rewriting CML text.

This boundary allows validation, stable identity handling, semantic diff, traceability, review consequences, candidate revision identity, and approval/canonical-source rules to remain centralized.

Canonical promotion is distinct from candidate editing and must pass the Phase 9/10 governance boundary.

## External AI as an editing palette

The editing-palette concept is broader than CBD Support's own UI. ChatGPT and Codex can act as external Model Editing Clients through Plugin/MCP integration.

This is desirable because an external AI may have access, when authorized, to additional useful context such as BoK/glossary material, GitHub repository contents, design documents, implementation code, tests, and the active reasoning conversation.

CBD Support should therefore expose semantic read/edit capabilities rather than trying to own all AI reasoning itself. ChatGPT/Codex are clients of the same model-edit boundary as the direct Update Palette.

## Solo Event Storming workflow

The representative workflow becomes:

```text
Canonical CML / Object Model
          |
          v
Event Storming View
          |
    exploratory editing
          |
          +--> direct Update Palette
          +--> conversational instruction
          +--> ChatGPT/Codex via Plugin/MCP
                         |
                         v
                 Model Edit Service
                         |
                         v
                Candidate Object Model
                         |
                         v
             Candidate Event Storming View
                 + Semantic Diff / Review
                         |
                  user confirmation
                         |
                exact-state approval
                         |
                canonical change gate
                         |
                         v
                 Canonical CML update
```

Event Storming is the initial validation scenario, not a special editing architecture. The same mechanism is intended for Mono-Koto Analysis, Workflow, Entity/Event, Structure, StateMachine, and other views.

## Review relationship

Review findings may provide context to an edit action (for example, "this Event has no triggering Command" -> Fix), but Review never implicitly changes the model. A fix changes candidate state first, and the resulting projection is confirmed before approval and canonical promotion.

## Planning consequence

Phase 11 **Interactive View and Model Editing** must explicitly implement candidate editing as distinct from canonical mutation. Direct UI, built-in chat, ChatGPT Plugin/MCP, and Codex MCP are alternative Model Editing Clients over a shared Model Edit Service. The central interaction contract is now:

**provisional edit -> candidate View confirmation -> exact-state approval -> canonical promotion**.

Detailed design direction is recorded in `docs/notes/interactive-view-model-editing.md`.
