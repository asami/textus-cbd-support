# Actor Goal List View

## Purpose

Actor Goal List is a stakeholder-facing intent projection that answers a simple question before behavioral or structural detail: **what does each Actor want to accomplish?**

It complements rather than replaces Use Case, Mono-Koto Analysis, and Event Storming.

```text
Actor
  +-- Goal
  |     +-- Use Case(s)
  |            +-- Mono-Koto context
  |            +-- Event Storming behavior
  +-- Goal
```

## Semantic ownership

Actor Goal List is a View of shared canonical semantics, not an independent analysis model.

- Actor identity is the same Actor identity used by Use Case.
- Goal captures stakeholder intent and should be attributable to admitted model/evidence.
- A Goal may be realized by one or more Use Cases.
- A Use Case may contribute to more than one Goal where the model/evidence supports that relationship.
- Goal must not be automatically collapsed into Use Case; intent and realization are different roles.
- Missing Actor/Goal/Use-Case relationships are shown as gaps rather than guessed.

## Relationship to other views

Actor Goal List provides an intent-oriented entry point:

```text
Actor Goal List
      |
      +--> Use Case        realization of intent
      +--> Mono-Koto       conceptual and terminology context
      +--> Event Storming  behavioral/causal realization
```

This allows CBD Support and AI review to ask useful cross-view questions such as:

- Which goals have no realizing Use Case?
- Which Use Cases are not clearly motivated by an Actor Goal?
- Which Actor Goal has no corresponding behavioral path in Event Storming?
- Which Mono/Koto concepts are involved in realizing a Goal?
- Are the same Actor identities used consistently across views?

The view should remain understandable to non-engineering stakeholders.

## Phase 11 editing

Actor Goal List participates in the shared Interactive View / Model Editing architecture. It does not get a special editor.

Representative Update Palette operations include:

- add/refine a Goal for an existing Actor;
- associate/disassociate a Goal and Use Case;
- refine Goal wording/description;
- navigate from a Goal to related Use Case, Mono-Koto, and Event Storming projections.

Actor creation must respect Use Case Actor ownership/identity rules rather than create a view-local Actor.

All edits follow the common lifecycle:

```text
Actor Goal List
   -> Update Palette / ChatGPT / Codex
   -> provisional Goal/relationship edits
   -> Candidate Object Model
   -> Candidate Actor Goal List View
   -> Semantic Diff / Review
   -> user confirmation
   -> exact-state approval
   -> canonical promotion
```

This makes Actor Goal List a useful early editing surface for solo modeling: establish stakeholder goals, connect them to Use Cases, then explore conceptual detail through Mono-Koto and behavior through Event Storming.
