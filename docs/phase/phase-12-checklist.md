# Phase 12 Checklist: Interactive View and Model Editing

Status: OPEN / NOT STARTED
phase=[Phase 12](phase-12.md)

This is the authoritative Phase 12 completion ledger. The remote proposal was
formerly numbered Phase 11; no old numbering or repository synchronization
constitutes implementation or acceptance. Split items before execution when
their scope exceeds the normal focused work-unit boundary.

## EDIT-12-01: Edit Session and operation contract (Stage 12.1)

- [ ] Promote design/specification for session, candidate identity/revision,
  operations/parameters/targets, preconditions, validation, diff, typed approval
  dependencies, failure, abandonment, and concurrency, including shared Actor
  Goal and Goal-to-Use-Case semantics.

## EDIT-12-02: Candidate Model Edit Service (Stage 12.2)

- [ ] Implement common candidate mutation and prove stable identities,
  authorization, and unchanged canonical CML during provisional editing.

## EDIT-12-03: Candidate projection and confirmation (Stage 12.3)

- [ ] Admit the required projection follow-up contracts and project candidate
  Actor Goal List/Event Storming through shared Views; clearly distinguish
  candidate/canonical state and confirm accumulated candidate edits.

## EDIT-12-04: Update Palette and direct commands (Stage 12.4)

- [ ] Implement contextual deterministic Actor Goal List, Mono-Koto, and
  Event Storming operations through the same candidate Edit Session service.

## EDIT-12-05: Conversational adapter (Stage 12.5)

- [ ] Convert conversational intent into authorized candidate operations and
  validation without implying canonical promotion or fabricating model facts.

## EDIT-12-06: External client boundary (Stage 12.6)

- [ ] Expose semantic inspection, applicable operations, candidate read/edit,
  and View/diff inspection through Plugin/MCP; validate ChatGPT and Codex as
  ordinary authorized clients with no privileged canonical mutation path.

## EDIT-12-07: Actor Goal to solo Event Storming E2E (Stage 12.7)

- [ ] Prove shared Actor identity, provisional goals and Use Case links,
  cross-view direct/AI edits, Review/refinement/confirmation, explicit approval
  of the exact typed candidate revision, and promotion only through the
  canonical gate.

## EDIT-12-08: Cross-view reuse and closure (Stage 12.8)

- [ ] Apply the same session/candidate foundation to Mono-Koto and at least
  one engineering view such as Workflow or Structure.
- [ ] Validate governance, durable continuation, auditability, abandonment,
  failure recovery, and consistent documentation with executable evidence.
- [ ] Complete independent review and close only when every required item is
  checked or explicitly relocated; no synchronization commit closes Phase 12.
