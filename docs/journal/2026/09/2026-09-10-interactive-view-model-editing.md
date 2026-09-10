# 2026-09-10 Interactive View and Model Editing Direction

## Context

We considered how CBD Support can support a solo Event Storming workflow. The intended workflow is not a standalone sticky-note editor. Event Storming elements are expressed through the CML-backed object model, while Event Storming View is used to inspect and reason about the result.

This raised a broader question: current views are mainly reference/review surfaces, but model development also needs an intentional editing interaction.

## Decision

Introduce editing as a distinct interaction responsibility alongside View and Review.

- **View** reads a projection.
- **Review** evaluates model quality and reports findings.
- **Edit** intentionally changes the canonical object model through semantic operations.

The normal UI remains a reference view. When editing is requested, an **Update Palette** is exposed over/in the context of that view.

The palette supports two complementary usage styles:

1. direct semantic commands for known operations such as adding a Mono/Koto or Event;
2. conversational instructions for compound or exploratory changes.

These are not separate mutation implementations. Both are translated into common model-edit operations handled by CBD Support services.

## Service boundary

CBD Support will provide a Model Edit Service over the canonical object model. UI and AI clients should invoke semantic operations rather than directly rewriting CML text.

This boundary allows validation, stable identity handling, semantic diff, traceability, review consequences, and approval/canonical-source rules to remain centralized.

## External AI as an editing palette

The editing-palette concept is broader than CBD Support's own UI. ChatGPT and Codex can act as external Model Editing Clients through Plugin/MCP integration.

This is desirable because an external AI may have access, when authorized, to additional useful context such as BoK/glossary material, GitHub repository contents, design documents, implementation code, tests, and the active reasoning conversation.

CBD Support should therefore expose semantic read/edit capabilities rather than trying to own all AI reasoning itself. ChatGPT/Codex are clients of the same model-edit boundary as the direct Update Palette.

## Solo Event Storming workflow

The representative workflow becomes:

```text
CML / Canonical Object Model
          |
          v
Event Storming View
          |
    user explores model
          |
          +--> direct Update Palette command
          |
          +--> conversational instruction
          |
          +--> ChatGPT/Codex via Plugin/MCP
                         |
                         v
                 Model Edit Service
                         |
                         v
                 Object Model update
                         |
                 CML / projection refresh
                         |
                         v
                 Event Storming View
```

Event Storming is the initial validation scenario, not a special editing architecture. The same mechanism is intended for Mono-Koto Analysis, Workflow, Entity/Event, Structure, StateMachine, and other views.

## Review relationship

Review findings may provide context to an edit action (for example, "this Event has no triggering Command" -> Fix), but Review never implicitly changes the model. Edit remains explicit and continues to respect Phase 9 candidate/semantic-diff rules and Phase 10 approval, integrity, continuation, and canonical-source gates.

## Planning consequence

Create Phase 11 for **Interactive View and Model Editing** after the Phase 9/10 foundations. Phase 11 should treat direct UI, built-in chat, ChatGPT Plugin/MCP, and Codex MCP as alternative Model Editing Clients over a shared Model Edit Service.

Detailed design direction is recorded in `docs/notes/interactive-view-model-editing.md`.
