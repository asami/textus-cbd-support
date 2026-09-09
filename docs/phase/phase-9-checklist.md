# Phase 9 Checklist: Evidence-Backed Component Composition and Dashboard

Status: OPEN
phase=[Phase 9](phase-9.md)

This checklist is the authoritative Phase 9 state ledger. Each item remains unchecked until reproducible evidence is recorded. Split an item before implementation when its declared boundary exceeds a focused work slice; target approximately six hours or less per item.

## P9-01: Shared responsibility boundary
- [ ] Define CBD Support, semantic/AI-provider, human-decision, Dashboard, Discovery, and Review ownership without allowing any surface to create a catalog fact or select a component implicitly.

## P9-02: Shared evidence inventory
- [ ] Inventory catalog, canonical design source, model, runtime, BoK, Review, and existing Web evidence; define exact attribution, authorization, redaction, absence, and limitation behavior for composition and Dashboard.

## P9-03: No-hidden-winner and no-inference rules
- [ ] Specify how conflicting, insufficient, or unavailable evidence remains visible without selection, CML-source heuristics, naming heuristics, diagram-layout inference, or Dashboard-local conclusions.

## P9-04: Composition model and promotion boundary
- [ ] Promote stable V1 identities for application intent, required capability, component evidence, coverage disposition, alternative, gap, proposal, and human decision into design and specification contracts.

## P9-05: Shared executable contract skeleton
- [ ] Add executable specifications for attribution, absence, redaction, and the boundary between composition candidates, Dashboard projections, and canonical Component facts.

## P9-06: Canonical Component Design Model and projection contract
- [ ] Define canonical-source authority, admitted enrichment evidence, stable semantic identities, projection rules, cross-view identity/navigation, and explicit absence/ambiguity behavior shared by Mono-Koto, Use Case, Entity, Event, Structure, Classification, Workflow, and StateMachine views.

## P9-10: Canonical composition plan
- [ ] Implement one typed transient plan that retains exact evidence and a disposition for every admitted required capability.

## P9-11: Deterministic coverage projection
- [ ] Implement deterministic selected, alternative, gap, and unresolved coverage without changing catalog facts or retrieval selection behavior.

## P9-12: Human decision admission
- [ ] Implement explicit human-decision admission for selection and proposal promotion, retaining rationale and provenance.

## P9-13: Advisory-provider contract
- [ ] Define and implement a versioned attributable semantic/AI-provider contract whose suggestions cannot replace evidence or a human decision.

## P9-14: Composition executable specifications
- [ ] Add Given/When/Then specifications for complete coverage, alternatives, gaps, conflicts, proposals, unavailable providers, and unapproved decisions.

## P9-20: Component Dashboard projection contract
- [ ] Define one deterministic `ComponentDashboard` projection for identity, content, usage, operation, quality, knowledge attribution, explicit absences, and stable model-view navigation.

## P9-21: Content Overview
- [ ] Project purpose, responsibility, domain summary, capabilities, representative analysis/model entry points, rules, interfaces/events, and related knowledge where admitted evidence exists.

## P9-22: Dashboard Web entry
- [ ] Add an authorized exact-component Dashboard entry backed only by the common projection and navigation targets with implemented contracts.

## P9-23: Dashboard executable specifications
- [ ] Prove deterministic source attribution, absence-safe projection, authorization, redaction, and canonical-identity preservation for the Dashboard foundation.

## P9-30: Mono-Koto projection contract
- [ ] Define Mono and Koto as stakeholder-facing projections over shared semantic identities, explicitly rejecting mandatory `Mono = Entity` and `Koto = Event` one-to-one mappings.

## P9-31: Mono-Koto Web overview
- [ ] Implement a non-engineering-oriented Mono-Koto overview using domain vocabulary, simple relationships, source attribution, explicit ambiguity, and drill-down links without exposing engineering detail by default.

## P9-32: Mono-Koto semantic bridge
- [ ] Link each admitted Mono to relevant Entity/Value/Aggregate semantics and each admitted Koto to relevant Command/Event/Workflow/state-effect semantics with stable forward and reverse navigation.

## P9-33: Use Case communication projection
- [ ] Project actor, goal, trigger, flows, postconditions, domain elements, collaborators, realizing Workflow, and Mono-Koto relationships with stable semantic navigation.

## P9-40: Entity Model contract
- [ ] Normalize and project Entity, Value, Aggregate, identity, ownership, lifecycle, and aggregate-boundary metadata; record unsupported Cozy fields as explicit gaps.

## P9-41: Structure view
- [ ] Implement overview and exact composition/aggregation/association detail that preserves published ownership, independent-existence, reassignment, deletion/lifecycle, cardinality, and navigability semantics.

## P9-42: Classification view
- [ ] Project generalization, trait, and multiple powertype dimensions with exact detail and reverse navigation where stable metadata permits it.

## P9-43: Static cross-view navigation
- [ ] Preserve canonical identity across Mono-Koto, Entity, Structure, and Classification projections so a user can move between overview and detailed static semantics without name-based reconstruction.

## P9-50: Event Model contract
- [ ] Normalize and project Command/Event identities, cause, consequence, affected domain elements, generated state effects, and attribution without treating every Koto as a single Event.

## P9-51: Workflow dual-projection contract
- [ ] Normalize one canonical Workflow semantic projection from published Cozy/CNCF metadata, including identity, purpose, related Use Cases, related domain subjects, SubWorkflow identities, Operations/Jobs/Events/StateMachines, and source attribution. Actors/participants must be derived through related Use Cases rather than duplicated as Workflow-owned facts.

## P9-52: Workflow Flowchart and StateMachine views
- [ ] Implement selectable Flowchart View and StateMachine View over the same Workflow semantic identities. Flowchart View is a simplified, non-authoritative human-oriented projection that may omit/aggregate details but must not invent semantics. StateMachine View preserves admitted State/Transition/Trigger/Guard/Action/SubWorkflow semantics. Preserve selection across view switching where stable mapping exists.

## P9-53: Dynamic cross-view and recovery navigation
- [ ] Preserve canonical identity across Koto, Event, Use Case, Workflow, SubWorkflow, StateMachine, affected Entity, and derived Actor projections with stable forward/reverse navigation. Where authoritative evidence exists, expose Workflow failure, compensation-required, and manual-recovery links without reconstructing missing runtime semantics.

## P9-60: Analysis-to-design impact projection
- [ ] Given a stakeholder-facing Mono-Koto or Use Case correction/proposal, identify affected Entity, Event, Structure, Workflow, StateMachine, and canonical-source locations without directly mutating canonical main-branch source.

## P9-61: Candidate design and semantic diff integration
- [ ] Connect analysis-view proposals to the existing proposed-CML, Candidate Component Design Model, semantic Design Diff, candidate Review, and Git-governed acceptance loop where CML owns the affected design.

## P9-62: Lifecycle and unified evidence integration
- [ ] Present declared composition/aggregation/association semantics beside available runtime and Review evidence and integrate Usage/Discovery, Operation, and Quality/Review projections without changing their canonical owners or conclusions.

## P9-63: Composition-to-Dashboard cross-surface navigation
- [ ] Allow an admitted composition candidate to link to an exact Component Dashboard entry and its model projections without converting that candidate into selection or fact.

## P9-70: Validation and compatibility
- [ ] Run selected executable, static, ABI, integration, representative SAR, projection-consistency, and navigation validation appropriate to the admitted implementation boundary.

## P9-71: Documentation record
- [ ] Synchronize design, specification, CML, README/reference material, strategy, Phase ledger, model-view notes, and journals with accepted implementation evidence.

## P9-72: Deferred-work accounting
- [ ] Record persistence, approval-history, continuation, unavailable upstream semantic metadata, and successor work under DEV-CBD-002 or explicit successor authority.

## P9-73: Final review and closure
- [ ] Complete final review, commit validated work, and close Phase 9 only after every required item is checked or explicitly relocated.
