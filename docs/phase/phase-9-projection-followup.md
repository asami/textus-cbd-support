# Post-Phase 9 Projection Follow-up Planning

Status: PLANNED / NOT STARTED
Owner: Textus CBD Support development
Predecessor: accepted [Phase 9](phase-9.md)
Consumer: [Phase 12](phase-12.md), where its editing scenarios need these views
Closure basis: the open follow-up checklist below, not the Phase 9 ledger

## Relocation boundary

The repository owner's 2026-10-03 sync decision preserves the completed Phase 9
contract and its accepted evidence. Additions in the remote Phase 9 proposal
are retained here as separate, not-started work. No new numbered Phase is
invented and no historical Phase 9 checklist item is reset or newly accepted.
Split each follow-up item into focused work before implementation; planning
does not authorize a local workaround for missing upstream semantics.

The original remote proposal and checklist remain available in native Git
commit `06ef9367dc883db36497297ea3dbdc663bf89028` at
`docs/phase/phase-9.md` and `docs/phase/phase-9-checklist.md`. Existing composition,
Dashboard, evidence, Usage, Operation, Quality/Review, candidate-design, and
Phase 10 responsibilities retain their accepted owners and meaning.

## Open follow-up checklist

### FU-01: Actor Goal List and shared intent semantics

- [ ] Define and implement Actor Goal List as a stakeholder-facing intent
  overview using existing Use Case Actor identity; do not create a parallel
  Actor model or equate Goal and Use Case automatically.
- [ ] Retain attributable Actor -> Goal -> one-or-more Use Case relationships,
  explicit missing/ambiguous relationships, and weak/missing realization
  observations without inventing facts for diagram completeness.

### FU-02: Terminology and Mono-Koto linkage

- [ ] Add attributable terminology/BoK preferred names, aliases, definitions,
  Mono/Koto relevance, and related-model navigation. Keep synonym candidates
  and terminology inconsistencies explicit until evidence or human decision
  resolves them; similarity alone must not merge identities.
- [ ] Preserve Mono as a conceptual summary of Entity/Value/Aggregate and
  Koto as a summary of Use Case/Command/Event/Workflow/state effects without
  mandatory Mono = Entity or Koto = Event mappings.

### FU-03: Cross-model Event Storming

- [ ] Define and implement the stakeholder behavioral overview across Actor,
  Goal/Use Case, Command/Operation, Aggregate/Entity, Domain Event,
  Policy/Reaction, external Component/dependency, and Query/View evidence;
  do not reduce it to an alternate Event Model renderer.
- [ ] Preserve hotspots as analysis/review observations, not canonical facts,
  and support forward/reverse navigation among Actor Goal List, Use Case,
  terminology, Mono-Koto, Event Storming, Entity/Event, Workflow, and
  StateMachine where admitted stable mappings exist.

### FU-04: Aggregate and View (Read Model)

- [ ] Retain remote `P9-44` as new follow-up scope: Aggregate write/update and
  consistency-boundary navigation, Aggregate Root, constituent/composed/
  referenced Entities, Commands, Events, transaction boundary, related
  StateMachines, and related Workflows from attributable metadata.
- [ ] Retain remote `P9-45` as new follow-up scope: View read/projection
  navigation, source Entities/Aggregates, Queries, joins, projection structure,
  read-model role, and available freshness/version evidence; missing projection
  semantics must not be inferred.

### FU-05: Designed/Observed model navigation

- [ ] Retain remote `P9-46`: organize engineering navigation around Aggregate
  (write/consistency), View (read/projection), and Workflow (process/progression).
  Keep Entity, Structure, Classification, Event, StateMachine, Use Case, and
  Traceability as drill-down/cross-cutting projections, not subordinate facts.
- [ ] Preserve attributable Aggregate -> Event -> View,
  Workflow -> Operation/Command -> Aggregate, and Workflow -> Query -> View
  links wherever authoritative metadata exists.
- [ ] Retain remote `P9-47`: provide authorized bidirectional links to CNCF
  Dashboard Observed Model projections, explicitly exposing unavailable or
  ambiguous runtime mappings rather than reconstructing them.

### FU-06: Workflow dual projections and participants

- [ ] Retain the expanded remote `P9-51` through `P9-53` requirements as new
  follow-up work, not as a replacement for the accepted versions of those IDs.
  Project purpose, related Use Cases/domain subjects, local/dependent-Component
  SubWorkflows, Operations/Jobs/Events/StateMachines, and exact source evidence.
- [ ] Derive participants through Workflow -> related Use Case -> Actor;
  support forward/reverse queries that explain each relationship without
  storing duplicate Workflow-owned Actor facts.
- [ ] Keep one canonical Workflow: a simplified, potentially non-faithful,
  non-authoritative Flowchart may collapse/omit details but never invent them;
  the faithful Workflow/StateMachine projection preserves admitted States,
  Transitions, Triggers, Guards, Actions, and Composite/SubWorkflow structure.
  Preserve selection across modes only with stable mappings.
- [ ] Expose attributable Workflow failure, compensation-required,
  programmatic recovery, and manual-recovery navigation when available. Do not
  infer compensation obligations from generic failures. Apply the detailed
  [Workflow refinement proposal](phase-9-workflow-projection-refinement.md).

### FU-07: Feedback, upstream admission, and validation

- [ ] Extend stakeholder change-impact and candidate/diff/review navigation to
  the newly admitted views, preserving CML canonical-source ownership and the
  Phase 10 exact-state approval/change gate; no direct canonical mutation.
- [ ] Admit published Cozy semantic metadata (including any Actor/Goal,
  terminology, policy, external-system, Query/View, and grouping additions) and
  CNCF lifecycle/Observed Model mapping contracts, including the CNCF Phase 87
  direction where applicable. Record unavailable contracts as upstream gaps;
  no local CML/name/layout inference or duplicated authority is permitted.
- [ ] Promote each actual implementation boundary through design/specification
  and executable behavior evidence, proportionate integration/navigation
  validation, documentation, explicit deferred-work accounting, and independent
  review before marking it complete. Existing Phase 9 tests do not prove these
  added requirements.

## Planning sources

- [Actor Goal List proposal](../notes/actor-goal-list-view.md)
- [Mono-Koto direction](../journal/2026/09/2026-09-09-mono-koto-analysis-view.md)
- [Actor Goal direction](../journal/2026/09/2026-09-10-actor-goal-list-view.md)
- [Workflow direction](../journal/2026/09/2026-09-10-workflow-dual-projection-and-participant-derivation.md)
- [Aggregate/View alignment](../journal/2026/09/2026-09-20-aggregate-view-workflow-dashboard-alignment.md)
- [Planning relocation record](../journal/2026/10/2026-10-03-repository-sync-planning-relocation.md)
