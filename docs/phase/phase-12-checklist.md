# Phase 12 Checklist: Interactive View and Model Editing

Status: OPEN / NOT STARTED
phase=[Phase 12](phase-12.md)
predecessor=[Phase 11](phase-11.md)

This is the authoritative Phase 12 completion ledger. The remote proposal was
formerly numbered Phase 11; no old numbering or repository synchronization
constitutes implementation or acceptance. Split items before execution when
their scope exceeds the normal focused work-unit boundary.

Preparation evidence and existing owners are in the
[local execution note](../notes/phase-11-12-local-execution-preparation.md).
Phase 10.7/11 handoffs and the selected FU-01/FU-03/FU-07 projection inputs must
be admitted for the scenarios that consume them; no existing closed Phase 9
item proves those additions.

## EDIT-12-01: Edit Session and operation contract (Stage 12.1)

- [ ] Promote design/specification for session, candidate identity/revision,
  operations/parameters/targets, preconditions, validation, diff, typed approval
  dependencies, failure, abandonment, and concurrency, including shared Actor
  Goal and Goal-to-Use-Case semantics.
- [ ] Define stable `UseCaseSlice` identity/lifecycle as an implementation slice
  of a Use Case; explicitly prohibit treating a Slice as a UI group, generated
  hash or ticket alias.
- [ ] Define `UseCaseDevelopmentTrace`, typed Executable Specification refs and
  external `TicketRef` (provider/repository/project + immutable ticket id).
- [ ] Define deterministic candidate operations for create/refine Slice and
  link/unlink Slice-Specification and Slice-Ticket relationships.
- [ ] Specify one-to-many cardinalities and exact-reference validation; no
  name/description/AI-based trace construction.
- [ ] Freeze independently attributed Slice-Specification and Slice-Ticket
  edges, bounded reference resolution, complete-scope evidence for missing-link
  findings, and project-owned severity/blocking policy. Preserve unavailable,
  unauthorized and redacted evidence separately from proven dangling targets.
- [ ] Identify the source/schema owner for new Slice/trace semantics and the
  explicit Phase 10 payload/subject/codec/continuation extensions they require;
  establish canonical projection support before permitting promotion.

## EDIT-12-02: Candidate Model Edit Service (Stage 12.2)

- [ ] Implement common candidate mutation and prove stable identities,
  authorization, and unchanged canonical CML during provisional editing.
- [ ] Implement Use Case Slice candidate mutation through the same service; no
  view-local Slice store or ticket-owned model state.
- [ ] Prove failed/dangling Specification or Ticket refs are typed validation
  failures and do not partially mutate candidate state.
- [ ] Prove unavailable reference resolution retains its distinct outcome and
  cannot silently create an admitted link or be reported as a nonexistent target.

## EDIT-12-03: Candidate projection and confirmation (Stage 12.3)

- [ ] Admit the required projection follow-up contracts and project candidate
  Actor Goal List/Event Storming through shared Views; clearly distinguish
  candidate/canonical state and confirm accumulated candidate edits.
- [ ] Add Use Case development trace projection:
  Actor Goal -> Use Case -> Use Case Slice, with independent Slice links to
  Executable Specifications and Tickets.
- [ ] Join Phase 11 Use Case/Application Capability demand trace as read-only
  evidence when admitted; distinguish unavailable projection from empty demand.
- [ ] Add deterministic Review findings at least for GoalWithoutUseCase,
  UseCaseWithoutSlice, SliceWithoutExecutableSpecification, SliceWithoutTicket,
  dangling Specification/Ticket and admitted behavioral/capability coverage gaps.
- [ ] Cover policy-required UseCaseWithoutGoal and MissingCapabilityDemandTrace;
  distinguish unavailable Phase 11 evidence from a proven missing demand edge.

## EDIT-12-04: Update Palette and direct commands (Stage 12.4)

- [ ] Implement contextual deterministic Actor Goal List, Mono-Koto, and
  Event Storming operations through the same candidate Edit Session service.
- [ ] Add Use Case detail commands for create/refine Slice and explicit
  Specification/Ticket link/unlink operations.
- [ ] Keep external ticket creation/update behind a separate authorized adapter;
  linking a TicketRef has no implicit external side effect.

## EDIT-12-05: Conversational adapter (Stage 12.5)

- [ ] Convert conversational intent into authorized candidate operations and
  validation without implying canonical promotion or fabricating model facts.
- [ ] Allow AI to propose Slice structure/links only as candidate operations;
  require explicit existing Specification/Ticket identities for links and reject
  fabricated external identities.

## EDIT-12-06: External client boundary (Stage 12.6)

- [ ] Expose semantic inspection, applicable operations, candidate read/edit,
  and View/diff inspection through Plugin/MCP; validate ChatGPT and Codex as
  ordinary authorized clients with no privileged canonical mutation path.
- [ ] Expose Use Case development trace, Review findings and applicable Slice
  operations through the same typed API; do not expose arbitrary ticket-system
  mutation as part of the core Model Edit Service.

## EDIT-12-07: Actor Goal to solo Event Storming E2E (Stage 12.7)

- [ ] Prove shared Actor identity, provisional goals and Use Case links,
  cross-view direct/AI edits, Review/refinement/confirmation, explicit approval
  of the exact typed candidate revision, and promotion only through the
  canonical gate.
- [ ] Create/refine at least one stable Use Case Slice, link it to an executable
  specification and external ticket reference, intentionally exercise and clear
  a missing trace Review finding, and preserve exact identities through approval.
- [ ] When Phase 11 demand evidence is available, display it in the same Use Case
  context and prove Phase 12 cannot mutate that evidence.

## EDIT-12-08: Cross-view reuse and closure (Stage 12.8)

- [ ] Apply the same session/candidate foundation to Mono-Koto and at least
  one engineering view such as Workflow or Structure.
- [ ] Validate governance, durable continuation, auditability, abandonment,
  failure recovery, and consistent documentation with executable evidence.
- [ ] Prove Use Case/Slice/Specification/Ticket identities and Review state
  survive Phase 10 package continuation/resume and canonical promotion.
- [ ] Verify new relationships survive a fresh process with their exact endpoint
  attribution; changed candidate/trace revisions invalidate previous approval,
  and an unsupported source projection cannot pass canonical promotion.
- [ ] Prove cross-view Review uses explicit semantic identities and never closes
  a finding solely from display-name/text similarity.
- [ ] Complete independent review and close only when every required item is
  checked or explicitly relocated; no synchronization commit closes Phase 12.
