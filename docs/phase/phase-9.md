# Phase 9 - Evidence-Backed Component Composition and Dashboard

Stage Status:
- Current status: OPEN
- Current step: P9-01 through P9-05 establish the shared evidence,
  responsibility, and projection boundaries before implementation begins.
- Owner: Textus CBD Support development
- Update rule: Update this block only after reproducible evidence is recorded
  in `phase-9-checklist.md`; phase closure is based solely on that ledger.

## Purpose

Phase 9 joins two complementary CBD Support capabilities without conflating
their authority. Application-component composition makes application intent,
required capabilities, attributable component evidence, alternatives, gaps,
proposals, and human decisions reviewable. Component Dashboard makes the
content and condition of one exact Component understandable through the same
admitted evidence.

Composition answers what can cover an application's explicitly modeled needs.
Dashboard answers what one known Component is, models, does, how it is used,
and what condition its evidence supports. Neither surface creates catalog facts,
infers unsupported model semantics, selects a hidden winner, or replaces the
canonical Review conclusion.

## Shared evidence boundary

Both capabilities consume only attributable catalog, model, runtime, BoK, and
Review evidence. They preserve source attribution, authorization/redaction,
explicit absence, conflicting evidence, and provider limitations.

Composition may link an admitted component identity to its Dashboard entry, but
Dashboard does not turn a composition candidate, provider suggestion, or human
decision into a Component fact. A semantic or AI provider may make an
attributable suggestion; only an explicit human decision can select an existing
component or promote a proposed component.

## Dashboard information architecture

    Component Dashboard
    |
    +-- Content
    |    +-- DomainModel
    |    |    +-- Static Model: Structure, Classification
    |    |    +-- Dynamic Model: Workflow, StateMachine detail
    |    +-- Use Case View
    |
    +-- Usage
    +-- Operation
    +-- Quality / Review

Content is a first-class Dashboard concern, not a Review subsection. Discovery
finds Components and retrieves attributable evidence; Dashboard explains an
exact Component; Review remains the canonical evaluator of quality.

## Scope

1. Promote the V1 composition responsibility, evidence, coverage, proposal,
   and human-decision boundary into design and specification contracts.
2. Define the shared source inventory and a deterministic Component Dashboard
   projection with explicit absence and attribution behavior.
3. Implement a typed, transient composition plan with deterministic coverage,
   bounded alternatives and gaps, and no hidden winner.
4. Implement Dashboard Content, Structure, Classification, Workflow,
   StateMachine, and Use Case projections only from admitted normalized
   metadata.
5. Add authorized read-only composition and Dashboard surfaces without adding
   application execution, provider administration, or synthetic navigation.
6. Integrate Usage, Operation, and canonical Review information into Dashboard
   without changing their source authority.
7. Verify the behavior with executable specifications and proportionate static,
   integration, and review evidence.

## Planning sources

- `docs/notes/usecase-driven-component-composition-v1.md` is the
  non-normative V1 composition direction.
- `docs/notes/component-dashboard-content-model.md` is the Dashboard content
  planning record introduced by the synchronized upstream history.
- `src/main/cml/usecase/application-component-composition.cml` remains working
  input until its syntax and contracts are promoted.

## Non-goals

- Generating, assembling, building, publishing, deploying, or executing an
  application from a composition plan.
- Treating semantic evidence, provider inference, a proposal, or a human
  decision as a catalog-owned Component fact.
- Selecting a component or resolving conflicting evidence without an explicit
  human decision.
- Parsing CML source, using names or diagram conventions, or implementing a
  local lifecycle engine to compensate for missing published model/runtime
  metadata.
- Persisting plans, approval history, or continuation packages; those remain
  DEV-CBD-002 work.
- Replacing Discovery search, Review conclusions, or existing retrieval
  selection rules.

## Planning rule

Each Stage is a bounded work slice. Split a checklist item before
implementation when its admitted boundary is materially larger than focused,
reproducible work. The checklist, not the number of headings here, records
completion.

## Stages

### Stage 9.1: Shared evidence and responsibility contract

Checklist basis: `P9-01` through `P9-05`.

Freeze the composition/provider/human split, the Dashboard source inventory,
shared attribution and absence rules, and the required design/specification
promotion before implementation.

### Stage 9.2: Canonical composition plan

Checklist basis: `P9-10` through `P9-14`.

Implement the typed transient plan, deterministic coverage projection, explicit
human decision admission, advisory-provider limitations, and executable
specifications without storage or application-generation behavior.

### Stage 9.3: Dashboard foundation and Content

Checklist basis: `P9-20` through `P9-23`.

Implement one deterministic Component Dashboard projection, Content Overview,
and an authorized Web entry with no local source heuristics or dead links.

### Stage 9.4: Static model views

Checklist basis: `P9-30` through `P9-32`.

Project Structure and Classification only from published normalized metadata.
Preserve relationship semantics and report unavailable Cozy metadata as an
upstream gap.

### Stage 9.5: Dynamic and Use Case views

Checklist basis: `P9-40` through `P9-42`.

Project Workflow, StateMachine, and Use Case evidence with stable forward and
reverse navigation, never isolated name-based lookup.

### Stage 9.6: Evidence integration

Checklist basis: `P9-50` through `P9-52`.

Connect declared lifecycle semantics to available runtime/Review evidence and
integrate Usage, Operation, and Quality views while retaining their canonical
owners.

### Stage 9.7: Validation and closure

Checklist basis: `P9-60` through `P9-63`.

Run proportionate validation, synchronize the documentation record, account for
deferred work, and close only after final review and every ledger item is
complete or explicitly relocated.

## Cross-project dependencies

Cozy Phase 54 owns machine-readable semantic component metadata for Structure,
Classification, Workflow, StateMachine, and Use Case views. CNCF Phase 72 owns
runtime lifecycle semantics and attributable runtime evidence. Missing upstream
contracts remain explicit gaps; they do not authorize a CBD Support workaround.
