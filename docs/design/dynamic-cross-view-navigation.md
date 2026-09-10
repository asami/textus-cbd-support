---
status: stable
decision_scope: P9-53A
updated_at: 2026-09-11
---

# Dynamic Cross-View Navigation Design

This design defines the P9-53A documentation boundary for dynamic cross-view
navigation. Its normative companion is the [Dynamic Cross-View Navigation
Contract](../spec/dynamic-cross-view-navigation-contract.md).

## Authority and predecessors

This decision is constrained by, and preserves the responsibilities of:

- Phase 9 and the P9-53 checklist;
- the CCDM design and CCDM contract;
- the static cross-view navigation design and contract;
- the Event, Workflow, and StateMachine designs and contracts; and
- the Entity Model Projection Contract.

The P9-53 index is a read-only view over one exact, already-established
Component and its bounded CCDM context. It is not an identity resolver,
projection merger, source of authority, or feedback channel.

## Retained identity boundary

The index retains only explicitly admitted identity records and explicit
mappings between independently retained records. Those records may concern
Koto, Event, Workflow, StateMachine, and affected Entity projections. Every
retained record and mapping carries:

- its exact Component and bounded CCDM context;
- its exact shared semantic target;
- its category and independent role;
- attribution and a bounded locator; and
- its distinct condition.

Display equality of a shared identity is never an admission rule. The design
therefore deliberately keeps each projection record independent even when it
concerns the same exact semantic target. Membership in the index neither
copies nor transfers assertions between those records.

## Dynamic-role eligibility

Eligibility identifies the role held by a record without changing the
semantics owned by its projection:

| Projection | Eligible dynamic roles |
| --- | --- |
| Koto | subject, reference |
| Event | subject, assertion, endpoint |
| Workflow | subject, flow, endpoint |
| StateMachine | subject, transition, transition-adjunct, endpoint |
| Entity | subject, metadata |

Consequently, index membership does not establish Event causality, Workflow
execution, StateMachine lifecycle, Entity ownership or lifecycle, Koto
aggregation or requirement, canonical or source authority, or a winning
projection. It also does not turn an assertion, consequence, activity, state
effect, transition adjunct, or lifecycle metadata into an identity mapping.

## Admission and condition preservation

Admission is explicit and identity-first. No record or mapping may be
admitted, recovered, or reconstructed from a label, name, type, namespace,
CML, source order, layout, query, visual geometry, local copy, or another
projection. In particular, an Event causal, consequence, or affected-subject
assertion; a Workflow flow, activity, or state effect; a StateMachine
transition or adjunct; and Entity lifecycle metadata cannot infer a mapping.

Records, mappings, gaps, and affordances preserve each relevant condition as
distinct information: unavailable, unauthorized, redacted, explicitly absent,
ambiguous, conflicting, stale, malformed, and limited. Explicit absence is a
bounded condition; it is not another spelling of unavailable, unauthorized,
or any other condition.

## Bounded gaps

When an admitted source record lacks a receiving counterpart, or a receiver
is unimplemented, the index records a bounded, attributable gap. A gap names
the exact Component/context, affected retained source identity, requested
category/role, source attribution and bounded locator/scope, condition, and
limitation. It names a counterpart's exact semantic identity only when that
identity is itself admitted.

A gap is not permission to perform broad lookup, infer a missing identity,
create a proxy or replacement, merge records, select a hidden winner, or make
a negative assertion beyond its stated bounded scope.

## Affordance model

Forward and reverse navigation are distinct target-gated affordances backed by
one retained mapping. Neither direction adds a duplicate assertion, declares
a one-to-one or primary relationship, or transfers authority. Each direction
is available only when all of the following are present:

1. the exact receiving target and exact Component/context;
2. source and receiving attribution with bounded locators;
3. every relevant, distinct condition; and
4. an implemented receiving contract.

A failed gate suppresses that affordance only. It does not discard retained
records, mappings, or gaps, and it must not yield a dead link, loose lookup,
substitute target, inferred reverse mapping, or semantic reconstruction.

## Deterministic presentation

The read-only index orders retained material by exact shared semantic target,
then category/role, then remaining record or mapping identities. An admitted
non-semantic tie key may be used only when required to make an already-known
order deterministic. Labels, names, CML, source order, layout, authority,
freshness, ranking, provider preference, and caller iteration cannot order or
break ties. If no admitted total order exists, the unresolved order remains
explicit.

## Read-only and delivery boundary

P9-53A performs no source I/O or mutation against CCDM, any named projection,
CML, Cozy, canonical sources, or feedback. It neither synthesizes,
normalizes, merges, nor reinterprets source material. It has no feedback,
lifecycle, event-store, or execution effect, and defines no Web, API, schema,
persistence, or technical-delivery behavior.

P9-53A creates these design and contract documents only. P9-53B separately
owns any runtime and executable-specification work. Phase/checklist/journal
updates, validation, review, commit, push, publication, deployment, and later
phase work are explicitly outside this decision scope.
