---
status: stable
decision_scope: P9-60A
updated_at: 2026-09-11
---

# Analysis-to-Design Impact Projection Contract

This is the normative P9-60A contract for one read-only Analysis-to-Design
Impact Projection. Its rationale is in the
[Analysis-to-Design Impact Projection Design](../design/analysis-design-impact-projection.md).

## 1. Authority and bounded scope

The projection SHALL be limited to one exact already established Component
identity and one bounded Canonical Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for Component identity, source authority, semantic identity,
attribution, bounded locators, conditions, navigation, and feedback. The
[Mono-Koto Projection Contract](mono-koto-projection-contract.md) and
[Use Case Communication Projection Contract](use-case-communication-projection-contract.md)
retain authority for communication-origin admission. The static and dynamic
cross-view navigation contracts retain authority for their own mappings and
affordances.

This projection SHALL be read-only. It SHALL NOT establish a new canonical
fact, source authority, feedback authority, identity authority, requirement,
selection, candidate design, semantic diff, or Review conclusion.

## 2. Proposal and origin admission

Each retained correction or proposal SHALL have exactly one already admitted
Mono-Koto or Use Case origin in the same exact Component/context. The retained
proposal SHALL preserve, independently:

1. the exact Component identity and bounded CCDM context;
2. the exact admitted origin semantic identity and distinct origin-record
   identity;
3. the origin projection category and independently admitted role;
4. source and authority attribution and a bounded source locator;
5. every applicable distinct condition;
6. the proposer;
7. a bounded correction or proposal statement;
8. stated limitations; and
9. an admitted identity-first tie key only where needed to make presentation
   order total.

A label, name, shared display wording, source order, shared display identity,
another projection, CML, layout, broad query, or view-local copy SHALL NOT
admit, recover, replace, repair, redirect, or broaden an origin or proposal.
The projection SHALL NOT turn a retained proposal into a canonical fact,
requirement, change instruction, selection decision, or direct source edit.

## 3. Direct impact-record admission

An impact record SHALL be explicitly admitted in the same exact
Component/context as its retained proposal. It SHALL be exactly one of the
following categories:

| Category | Required retained subject |
| --- | --- |
| Entity | An already admitted Entity projection record and its independent role. |
| Event | An already admitted Event projection record and its independent role. |
| Structure | An already admitted Structure projection record and its independent role. |
| Workflow | An already admitted Workflow projection record and its independent role. |
| StateMachine | An already admitted StateMachine projection record and its independent role. |
| Canonical Source Location | A bounded canonical-source locator retained for traceability only. |

Every impact record SHALL retain its exact semantic target identity where
applicable, category and independently admitted role, source and authority
attribution, bounded locator, every applicable distinct condition, and an
admitted stable tie key when necessary. Impact membership SHALL NOT transfer
authority or turn Entity ownership, Event causality, Structure relations,
Workflow flow, StateMachine lifecycle, or a canonical source location into a
different semantic assertion.

A Canonical Source Location SHALL provide traceability only. It SHALL NOT
authorize retrieval, parsing, interpretation, mutation, candidate construction,
or main-branch change of CML or any canonical source.

## 4. Explicit proposal-to-impact links

Each proposal-to-impact association SHALL be an explicit attributable semantic
relationship between one retained proposal and one retained impact record in
the same exact Component/context. It SHALL retain the two record identities,
their categories and roles, attribution, bounded locators, every applicable
distinct condition, and limitations.

The projection SHALL NOT infer an impact record or association from a label,
name, source location, category, nearest relation, display order, shared target
identity, Mono/Koto aggregation, Use Case relation, Event cause or consequence,
Workflow flow, StateMachine transition, Entity lifecycle relation, CML, source
order, layout, another projection, broad query, or caller iteration. It SHALL
NOT construct, select, apply, approve, accept, or mutate a design change from
an association.

## 5. Conditions and bounded gaps

Each proposal, impact record, link, gap, attribution, and locator SHALL retain,
distinguish, and attribute every applicable condition below:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source or impact record cannot supply material; it is not explicit absence. |
| unauthorized | The required admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish an expected fact in its stated bounded scope; it is not a broad negative fact. |
| ambiguous | More than one admissible impact, identity, relation, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed material asserts incompatible impacts or relations; affected attribution remains visible. |
| stale | Supplied freshness state prevents current representation. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A source, authority, projection, implementation, compatibility, or disclosure boundary prevents a stronger claim. |

When an origin has no admitted record for a requested impact category, the
projection SHALL retain an attributable bounded gap. The gap SHALL include the
retained proposal identity, requested impact category, attribution, bounded
locator or bounded unavailable scope, condition, and limitation. It SHALL NOT
convert unknown impact into no impact or an unbounded negative semantic fact,
or authorize lookup, proxying, identity recovery, replacement, merging,
reconstruction, or a hidden winner.

## 6. Deterministic identity-first order

The projection SHALL order proposals, impact records, links, and gaps by exact
retained identities first; then category and role; then remaining retained
identities; and only then an admitted stable non-semantic tie key when needed
to make the order total. Labels, names, CML, source order, layout, freshness,
ranking, provider preference, authority, caller iteration, or another
projection SHALL NOT select an impact, relation, interpretation, order, or
winner. When admitted keys cannot make the order total, the unresolved ordering
condition SHALL remain explicit.

## 7. Read-only prohibitions and later work

The projection SHALL NOT perform source I/O or mutation against CCDM,
Mono-Koto, Use Case, Entity, Event, Structure, Workflow, StateMachine, CML,
canonical sources, feedback, or main-branch source. It SHALL NOT synthesize,
normalize, merge, infer, select, edit, parse, retrieve, reauthorize, or mutate
an analysis record, impact, canonical source, or source fact.

This P9-60A contract creates documentation only. P9-60B separately owns runtime
values and executable specifications. P9-61 separately owns candidate CML,
Candidate Component Design Model, semantic diff, candidate Review, and
Git-governed acceptance. This contract does not authorize API, Web, schema,
persistence, lifecycle, technical delivery, Phase/checklist/journal closure,
validation, review, commit, push, publication, deployment, or later Phase
work.
