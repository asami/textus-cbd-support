# Phase 12 - Interactive View and Model Editing

Stage Status:
- Current status: PLANNED
- Predecessor: Phase 11 Capability Catalog, Discovery, and Traceability
- Required inputs: accepted Phase 9 model/view contracts, the Phase 10 sequence,
  and the admitted Actor Goal / Event Storming projection follow-up contracts
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-12-checklist.md` with
  reproducible validation evidence.

## Planning relocation

This is the remote Interactive View and Model Editing proposal, originally
numbered Phase 11, relocated by the repository owner's 2026-10-03 sync decision.
Phase 11 remains Capability Catalog, Discovery, and Traceability. Historical
journal references retain their original numbering; this document and its
checklist define the current planned identity. No stage has started.

## Purpose

Phase 12 turns CBD Support views from read/review-only model projections into safe interaction surfaces for intentional model development, without making each view an independent editor or weakening canonical CML governance.

The phase introduces a shared semantic Model Edit Service and treats the CBD Support Update Palette, conversational interaction, ChatGPT Plugin/MCP, Codex MCP, and future clients as alternative Model Editing Clients.

Editing is explicitly provisional. Model Editing Clients develop a Candidate Object Model. The candidate is projected through normal Views, inspected by the user, optionally reviewed, and only an explicitly approved exact candidate revision may be promoted through the existing canonical change gate.

The first end-to-end validation scenario is **solo Event Storming**, with **Actor Goal List** providing an intent-oriented starting context: Actor -> Goal -> Use Case -> Event Storming. Phase 12 also introduces the development-realization confirmation chain **Actor Goal -> Use Case -> Use Case Slice -> Executable Specification -> Ticket**. This chain is explicit traceability, not an inferred one-to-one hierarchy.

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

## Use Case development-realization confirmation

Phase 12 owns review/edit behavior for the development-realization chain:

```text
Actor Goal
  -> Use Case
      -> Use Case Slice
          -> Executable Specification
              -> Ticket
```

A `UseCaseSlice` is a stable identified implementation slice of a Use Case,
sized to fit an iteration/work unit. It is not a UI grouping, generated hash,
or ticket alias. One Use Case may have multiple slices; a slice may have
multiple executable specifications and multiple implementation tickets when
explicitly modeled.

The internal candidate/read model should provide repository-equivalent typed
roles:

```text
UseCaseSlice
  - sliceRef
  - useCaseRef
  - title/intent
  - scopeRefs
  - acceptanceRefs
  - executableSpecificationRefs
  - ticketRefs
  - lifecycle/status

UseCaseDevelopmentTrace
  - actorGoalRefs
  - useCaseRef
  - sliceRefs
  - executableSpecificationRefs
  - ticketRefs
  - capabilityDemandRefs

UseCaseReviewFinding
  - kind
  - subjectRef
  - relatedRefs
  - severity/blocking policy
  - evidenceRefs
```

Initial deterministic Review finding kinds should include:

- `GoalWithoutUseCase`
- `UseCaseWithoutGoal` when project policy requires goal traceability
- `UseCaseWithoutSlice`
- `SliceWithoutExecutableSpecification`
- `SliceWithoutTicket`
- `DanglingExecutableSpecification`
- `DanglingTicket`
- `UncoveredUseCaseBehavior` when admitted Event Storming/behavioral projection
  proves an explicit coverage gap
- `MissingCapabilityDemandTrace` when an admitted Phase 11 demand projection
  is expected but absent

Review reports facts from explicit identities and admitted projections. It must
not manufacture a Goal, Slice, Specification, Ticket, capability demand or
coverage relation from textual similarity or AI judgment.

Ticket integration in Phase 12 is a semantic reference contract first. A ticket
reference contains provider/repository/project identity plus immutable external
ticket identity and optional observed status/version. Creating or updating an
external issue tracker ticket is outside the core model service unless a
separate authorized adapter is explicitly invoked.

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
12. Use Case Slice, Executable Specification, and Ticket links use stable explicit identities; AI/name similarity never creates traceability.
13. Phase 11 Capability demand traces are consumed as read-only evidence by Phase 12 Review and are not rewritten by the editing service.

## Scope

- Define Edit Session, Candidate Object Model, and common Model Edit Operation contracts.
- Implement Model Edit Service over candidate state.
- Support candidate projection, semantic diff, Review, exact-state approval, and canonical promotion.
- Add Update Palette and direct commands.
- Include Actor Goal operations and Goal-to-Use-Case relationships using shared Actor identity.
- Define Use Case Slice identity/lifecycle and explicit Use Case-to-Slice,
  Slice-to-Executable-Specification, and Slice-to-Ticket trace contracts.
- Provide deterministic Use Case development-realization Review findings and
  cross-navigation across Goal, Use Case, Slice, Specification, Ticket and
  admitted Phase 11 Capability demand traces.
- Support conversational edit intent.
- Expose candidate read/edit capabilities through MCP/Plugin boundaries for ChatGPT/Codex.
- Implement solo Event Storming starting optionally from Actor Goal List context.
- Reuse the same foundation for Actor Goal List, Mono-Koto Analysis, and at least one engineering-oriented view.
- Document authorization, confirmation, approval, audit, abandonment, and recovery expectations.

## Stages

Each stage should remain within the project's normal approximately six-hour work-unit target; split further before implementation when necessary.

### Stage 12.1 - Edit Session and operation contract

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-01 in
  [Phase 12 Checklist](phase-12-checklist.md).

Define Edit Session, Candidate Object Model identity/revision, semantic operation identity, parameters, target identity, preconditions, validation result, semantic diff, failure behavior, abandonment, and concurrency expectations. Include representative Actor Goal and Goal-to-Use-Case operations.

Also freeze `UseCaseSlice`, `UseCaseDevelopmentTrace`, typed external
`TicketRef`, and explicit Specification reference contracts. Define
deterministic operations equivalent to `createUseCaseSlice`,
`refineUseCaseSlice`, `linkSliceSpecification`,
`unlinkSliceSpecification`, `linkSliceTicket`, and `unlinkSliceTicket`.
All operations target candidate state and require typed subject references.

### Stage 12.2 - Candidate Model Edit Service

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-02 in
  [Phase 12 Checklist](phase-12-checklist.md).

Implement the service boundary over candidate object-model state. Prove that provisional mutations preserve stable identities, including shared Actor identity, without modifying canonical CML.

### Stage 12.3 - Candidate projection and confirmation

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-03 in
  [Phase 12 Checklist](phase-12-checklist.md).

Project candidate state through existing View machinery, including Actor Goal List and Event Storming. Clearly distinguish provisional content and support confirmation of an accumulated candidate model.

Add a Use Case development trace projection showing Goal -> Use Case -> Slice ->
Executable Specification -> Ticket plus read-only Phase 11 capability-demand
trace where available. The projection must distinguish absent upstream data
from an explicitly empty relationship.

### Stage 12.4 - Update Palette and direct commands

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-04 in
  [Phase 12 Checklist](phase-12-checklist.md).

Add the contextual Update Palette. Implement direct deterministic operations for Actor Goal List, Mono-Koto, and representative Event Storming elements/relationships. All commands update the current candidate Edit Session.

For Use Case detail, expose applicable deterministic commands for creating or
refining a Slice and linking/unlinking explicit Executable Specification and
Ticket references. External ticket creation is a separate adapter action and
must never occur as an implicit side effect of linking a model reference.

### Stage 12.5 - Conversational edit adapter

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-05 in
  [Phase 12 Checklist](phase-12-checklist.md).

Translate conversational edit intent into candidate Model Edit Operations. Support instructions such as refining an Actor's goals, connecting goals to Use Cases, developing related Event Storming behavior, and proposing a Use Case Slice or explicit Slice/Specification/Ticket link. AI interaction must not imply canonical promotion and must not fabricate external ticket or specification identities.

### Stage 12.6 - Plugin/MCP external editing boundary

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-06 in
  [Phase 12 Checklist](phase-12-checklist.md).

Expose semantic inspection, candidate-session operations, applicable-operation discovery, provisional edits, and candidate View/diff inspection through MCP/Plugin-facing capabilities. Validate ChatGPT and Codex as external editing palettes.

### Stage 12.7 - Actor Goal -> solo Event Storming end-to-end validation

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-07 in
  [Phase 12 Checklist](phase-12-checklist.md).

Support a session in which the user can:

- inspect Actor Goal List using shared Use Case Actor identity;
- establish/refine Actor Goals provisionally;
- link Goals to Use Cases;
- select Goal/Use Case context and open Event Storming View;
- develop Event Storming through direct commands or ChatGPT/Codex;
- inspect both Actor Goal List and Event Storming projections of the accumulated candidate;
- use Review to identify missing Goal realization or behavioral coverage;
- inspect the Use Case development trace and create/refine at least one stable
  Use Case Slice;
- link the Slice to an existing executable specification and external ticket
  reference through explicit identities;
- use Review to detect an intentionally missing Slice/Specification/Ticket
  relation and clear the finding by an explicit candidate edit;
- inspect the read-only Phase 11 Capability demand trace when available;
- refine the candidate;
- confirm the resulting Views;
- approve an exact candidate revision;
- promote it through the canonical change gate.

### Stage 12.8 - Cross-view reuse and closure

Stage Status:
- Current status: OPEN
- Owner: Textus CBD Support development
- Update rule: record completion only from EDIT-12-08 in
  [Phase 12 Checklist](phase-12-checklist.md).

Apply the same edit-session/candidate foundation to Mono-Koto Analysis and at least one engineering-oriented view such as Workflow or Structure. Validate governance, durable continuation, auditability, abandonment, failure recovery, and documentation.

Close with executable cross-view traceability tests proving that stable
Use Case/Slice/Specification/Ticket identities survive candidate editing,
Phase 10 continuation/resume and canonical promotion, and that Phase 11
Capability demand evidence remains read-only.

## Acceptance scenario

```text
Actor Goal List
  -> Actor: Customer
  -> provisional Goal: purchase a product
  -> link Goal to Place Order Use Case
  -> create Slice: Validate and submit order
  -> link Slice to executable specification ES-...
  -> link Slice to external ticket reference
  -> Review Use Case development-realization coverage
  -> Candidate Actor Goal List / Use Case Development Trace View
  -> open Event Storming for that Goal / Use Case
  -> provisional Event Storming edits through palette or ChatGPT/Codex
  -> Candidate Object Model
  -> Actor Goal List + Event Storming candidate Views
  -> Semantic Diff / Review
  -> user confirms cross-view result
  -> approval bound to exact candidate revision
  -> Phase 10 canonical change gate
  -> canonical object model/CML update
```

The phase is not complete if a normal Update Palette or AI edit can silently bypass the candidate/View/approval lifecycle, if Actor Goal List creates identities inconsistent with Use Case Actor semantics, if Use Case Slice is reduced to a ticket/UI grouping, or if Specification/Ticket/Capability traceability is inferred from names rather than explicit admitted identity.

## Continuity with Phase 9 and Phase 10

The accepted Phase 9 contracts supply canonical model identities, Use Case
Actor semantics, existing projections, Candidate Design Model, semantic diff,
and review boundaries. Actor Goal List, Goal-to-Use-Case traceability, and Event
Storming are still planned in
[Projection follow-up planning](phase-9-projection-followup.md); they must be
admitted through their own design/specification and executable evidence before
Phase 12 can consume them as implemented contracts. The Phase 10 sequence
supplies durability, exact typed identity/revision approval, drift handling,
and the CML change gate as its remaining ledgers close. Phase 12 plans Edit
Sessions, interactive candidate mutation, candidate View confirmation, and
external AI client access. Planning does not claim any of these new inputs
implemented or accepted.

The invariant remains:

**Edit develops candidate state; View confirms candidate state; Approval authorizes exact candidate state; the canonical gate alone promotes it.**

## Planning sources

- `docs/notes/actor-goal-list-view.md`
- `docs/notes/interactive-view-model-editing.md`
- `docs/journal/2026/09/2026-09-10-actor-goal-list-view.md`
- `docs/journal/2026/09/2026-09-10-interactive-view-model-editing.md`
- `docs/phase/phase-9.md`
- `docs/phase/phase-10.md`

- [Phase 12 Checklist](phase-12-checklist.md)
- [Projection follow-up planning](phase-9-projection-followup.md)
- [Planning relocation record](../journal/2026/10/2026-10-03-repository-sync-planning-relocation.md)

Approval binds explicit typed candidate identity, revision, dependencies, and
state. No management hash or content-derived approval token is introduced.
