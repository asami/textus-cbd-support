# Phase 12 Checklist: Interactive View and Model Editing

Status: IN_PROGRESS — Stages 12.1–12.4 CLOSED; Stage 12.5 IN_PROGRESS; Stage 12.6+ OPEN
phase=[Phase 12](phase-12.md)
predecessor=[Phase 11.1](phase-11.1.md)

This is the authoritative Phase 12 completion ledger. The remote proposal was
formerly numbered Phase 11; no old numbering or repository synchronization
constitutes implementation or acceptance. Split items before execution when
their scope exceeds the normal focused work-unit boundary.

Preparation evidence and existing owners are in the
[local execution note](../notes/phase-11-12-local-execution-preparation.md).
Phase 10.7/11.1 handoffs, including the accepted Phase 11 query contract, and
the selected FU-01/FU-03/FU-07 projection inputs must
be admitted for the scenarios that consume them; no existing closed Phase 9
item proves those additions.

Current predecessor truth (2026-10-27): Phase 10.7/11.1/14/15/15.1 are CLOSED
in their own ledgers. EDIT-12-01 promotes the
[session contract](../spec/model-edit-session-contract.md) and
[design](../design/model-edit-session.md), with immutable typed foundation and
pure admission accepted. Native focused validation passed 41 tests/3 suites,
full regression passed 1,099 tests/137 suites, independent Step and bounded
whole-run reviews accepted, and the native run reached Terminal/completed.
That run closed EDIT-12-01. EDIT12-02 is now separately accepted at
`f6f2f354b6e2ef2767ee1737adfc8858bf5eead7`; parent Phase 12 remains
IN_PROGRESS. The compact
[handoff](../journal/2026/10/2026-10-27-phase-12-edit-session-foundation.md)
records the selected boundary without duplicating native execution evidence.

## EDIT-12-01: Edit Session and operation contract (Stage 12.1)

- [x] Promote design/specification for session, candidate identity/revision,
  operations/parameters/targets, preconditions, validation, diff, typed approval
  dependencies, failure, abandonment, and concurrency, including shared Actor
  Goal and Goal-to-Use-Case semantics.
- [x] Define stable `UseCaseSlice` identity/lifecycle as an implementation slice
  of a Use Case; explicitly prohibit treating a Slice as a UI group, generated
  hash or ticket alias.
- [x] Define `UseCaseDevelopmentTrace`, typed Executable Specification refs and
  external `TicketRef` (provider/repository/project + immutable ticket id).
- [x] Define deterministic candidate operations for create/refine Slice and
  link/unlink Slice-Specification and Slice-Ticket relationships.
- [x] Specify one-to-many cardinalities and exact-reference validation; no
  name/description/AI-based trace construction.
- [x] Freeze independently attributed Slice-Specification and Slice-Ticket
  edges, bounded reference resolution, complete-scope evidence for missing-link
  findings, and project-owned severity/blocking policy. Preserve unavailable,
  unauthorized and redacted evidence separately from proven dangling targets.
- [x] Identify the source/schema owner for new Slice/trace semantics and the
  explicit Phase 10 payload/subject/codec/continuation extensions they require;
  retain the canonical projection prerequisite before permitting promotion.
  Stage 12.1 identifies these unadmitted source mappings and integration gaps;
  implementation remains required in 12.2/12.3 and continuation proof in 12.8.
- [x] Validate the pure typed foundation and existing identity/approval
  specifications through native focused validation, independent Step and
  bounded whole-run review, and required native full regression.

## EDIT-12-02: Candidate Model Edit Service (Stage 12.2)

CLOSED by native Step acceptance and commit
`f6f2f354b6e2ef2767ee1737adfc8858bf5eead7` (parent `9739cb9`). The native
focused check passed 261 tests in 16 selected suites. Independent review
`cbd-phase12-edit12-02-independent-review-02` accepted the repaired candidate
and recorded original REVIEW `issue-1` RESOLVED. Immutable application, typed
candidate payload, captured rehydration and complete-subject integration are
accepted at this boundary. Canonical source mappings and consuming Views remain
Stage 12.3; this Step does not close FU-01/FU-03/FU-07 or prove fresh-process
continuation. The native accepted prefix in the current Stage 12.3 assignment
retains this commit and review.

- [x] Implement common candidate mutation and prove stable identities,
  authorization, and unchanged canonical CML during provisional editing.
- [x] Implement Use Case Slice candidate mutation through the same service; no
  view-local Slice store or ticket-owned model state.
- [x] Admit the source-owned Slice/trace payload, typed-subject and codec
  extensions required for durable candidate support; no opaque sidecar.
- [x] Prove failed/dangling Specification or Ticket refs are typed validation
  failures and do not partially mutate candidate state.
- [x] Prove unavailable reference resolution retains its distinct outcome and
  cannot silently create an admitted link or be reported as a nonexistent target.

## EDIT-12-03: Candidate projection and confirmation (Stage 12.3)

CLOSED by native Step acceptance and commit
`a08c7745748d32e7549ebd35da0d3cfa1e9b2d5b` (parent `f6f2f35`). Fresh native
validation passed 448 tests in 33 selected suites. Independent review
`cbd-phase12-edit12-03-independent-review-02` accepted the repaired candidate;
current TEST/REVIEW issues 2–6 are resolved. Accepted supplier Cozy commit
`da61cb223a1df16a9b6cbacd4498ba8134cf3a22` is consumed through its actual
production/read/render-candidate-v2/re-production JVM route, qualified against
existing Phase 10 validators. Plan revision 2 was genuinely adopted and retained
the accepted prefix. The earlier rejected internal dependency proposal, startup
recovery, check failures and four repair batches remain in native/journal history.

Source-authorized candidate projection, same-candidate Actor Goal/Event Storming
and independent development trace, complete-scope project-policy Review, and
explicit Phase 11 unavailable-versus-empty evidence are accepted here. The Use
Case demand owner mapping remains upstream unavailable; broader FU-07 lifecycle/
Observed/Phase 87 work is not closed by this consuming portion. Canonical
approval/application and fresh-process continuation retain their later Stages.
The temporary runtime assembly recovery is not a new Phase prerequisite.

- [x] Admit the required projection follow-up contracts and project candidate
  Actor Goal List/Event Storming through shared Views; clearly distinguish
  candidate/canonical state and confirm accumulated candidate edits.
- [x] Establish source-authorized Slice/trace projection and CML mappings before
  permitting those semantics through the existing Phase 10 promotion gate.
- [x] Add Use Case development trace projection:
  Actor Goal -> Use Case -> Use Case Slice, with independent Slice links to
  Executable Specifications and Tickets.
- [x] Join Phase 11 Use Case/Application Capability demand trace as read-only
  evidence when admitted; distinguish unavailable projection from empty demand.
- [x] Add deterministic Review findings at least for GoalWithoutUseCase,
  UseCaseWithoutSlice, SliceWithoutExecutableSpecification, SliceWithoutTicket,
  dangling Specification/Ticket and admitted behavioral/capability coverage gaps.
- [x] Cover policy-required UseCaseWithoutGoal and MissingCapabilityDemandTrace;
  distinguish unavailable Phase 11 evidence from a proven missing demand edge.

## EDIT-12-04: Update Palette and direct commands (Stage 12.4)

CLOSED by native Step acceptance and commit
`004e5eb43d1fdce5dec8c97e419b984cc00dde1a` (parent `a08c774`). Native ordinary-03
validation passed 73 tests in 16 selected suites and independent review
`cbd-phase12-edit12-04-independent-review-01` accepted the current candidate.
The [Palette contract](../spec/model-edit-palette-contract.md) and
[design](../design/model-edit-palette.md) define contextual inspection, direct
same-service commands and shared candidate confirmation. The accepted bounded
surface covers the existing ten commands across shared views; source-only
causal/event CRUD remains with its existing read-only source-owner boundary.
External ticket mutation is a separate adapter and is not introduced here.

- [x] Implement contextual deterministic Actor Goal List, Mono-Koto, and
  Event Storming operations through the same candidate Edit Session service.
- [x] Add Use Case detail commands for create/refine Slice and explicit
  Specification/Ticket link/unlink operations.
- [x] Keep external ticket creation/update behind a separate authorized adapter;
  linking a TicketRef has no implicit external side effect.

## EDIT-12-05: Conversational adapter (Stage 12.5)

IN_PROGRESS under the current native ImplementStep. The
[conversational contract](../spec/model-edit-conversational-contract.md) and
[design](../design/model-edit-conversational-adapter.md) define typed interpreted
intent, retained unresolved requests, independent bounded reference evidence and
same-service candidate application. The authored executable spec covers that
adapter; no live AI invocation, native qualification or independent acceptance is
claimed yet. Native ordinary-04 and independent review remain pending.

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
