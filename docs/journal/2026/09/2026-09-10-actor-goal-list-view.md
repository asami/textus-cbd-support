# 2026-09-10 Actor Goal List View

## Decision

Add **Actor Goal List** to the Phase 9 communication/analysis projections and make it an editable projection under the Phase 11 Interactive View architecture.

The motivation is to provide a stakeholder-readable intent overview before or alongside Use Case, Mono-Koto Analysis, and Event Storming. For solo modeling it gives a natural starting question: what does each Actor want to accomplish?

## Semantic relationship

Actor Goal List does not own a separate Actor model. Actor identity is shared with Use Case Actor semantics.

```text
Actor
  -> Goal
      -> Use Case(s)
          -> Mono-Koto context
          -> Event Storming behavior
```

Goal represents stakeholder intent; Use Case represents realization of that intent. They are linked but are not automatically one-to-one or semantically identical.

The projection should expose missing or ambiguous Goal-to-Use-Case realization rather than inventing relationships. Cross-navigation should make it possible to move from Actor/Goal to realizing Use Cases and onward to Mono-Koto and Event Storming, and to navigate back from those projections to stakeholder intent.

## Review value

Actor Goal List adds useful review questions to the shared model-strength checks:

- Does every important Actor have attributable goals?
- Does a Goal have a realizing Use Case where one is expected?
- Is a Use Case motivated by a known Actor Goal?
- Does an important Goal have behavioral realization visible through Event Storming?
- Are Actor identities consistent across Use Case, Workflow participation, Event Storming, and Actor Goal List?

These are diagnostics; missing information must not be silently synthesized.

## Phase 11 editing

Actor Goal List uses the common Update Palette and Model Edit Service. Typical direct operations include adding/refining a Goal and linking a Goal to a Use Case. ChatGPT/Codex can perform the same candidate operations through Plugin/MCP while using additional authorized context.

The mandatory lifecycle remains:

```text
provisional Actor/Goal edit
  -> Candidate Object Model
  -> Candidate Actor Goal List View
  -> Semantic Diff / Review
  -> user confirmation
  -> exact-state approval
  -> canonical promotion
```

Actor creation or reassignment must preserve the shared Use Case Actor identity rules; the view must not create its own Actor namespace.

## Planning impact

- Phase 9 Stage 9.4 includes Actor Goal List alongside Mono-Koto, Use Case, and Event Storming.
- Phase 9 cross-view navigation includes Actor/Goal links.
- Phase 11 direct-edit and cross-view validation include Actor Goal List.
- Solo Event Storming can start from Actor -> Goal -> Use Case context before expanding into behavioral Event Storming semantics.
