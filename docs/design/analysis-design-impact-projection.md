---
status: stable
decision_scope: P9-60A
updated_at: 2026-09-11
---

# Analysis-to-Design Impact Projection Design

This design fixes the P9-60A documentation boundary for one read-only
Analysis-to-Design Impact Projection. Its normative companion is the
[Analysis-to-Design Impact Projection Contract](../spec/analysis-design-impact-projection-contract.md).

## Purpose and authority

The projection presents already admitted, attributable feedback from the
Mono-Koto or Use Case communication projections together with already admitted
engineering and traceability impacts. It is scoped to one exact established
Component and one bounded Canonical Component Design Model (CCDM) context.
Its purpose is to make the bounded impact of a stakeholder correction or
proposal inspectable without turning that feedback into a design change.

The [Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and [Canonical Component Design Model Contract](../spec/canonical-component-design-model-contract.md)
retain authority for Component scope, semantic identity, source authority,
attribution, locators, conditions, navigation, and feedback. The
[Mono-Koto Projection Model](mono-koto-projection-model.md) and
[Use Case Communication Projection Model](use-case-communication-projection-model.md),
with their companion contracts, retain authority for the communication records
from which a correction or proposal may originate. The static and dynamic
cross-view navigation designs and contracts retain authority for their own
retained mappings and target-gated navigation.

This projection is not an analysis-to-design transformation, an identity
resolver, a candidate design, a semantic diff, a Review conclusion, a source
reader, or a source of canonical authority.

## Retained proposal origin

Each retained correction or proposal begins with one already admitted
Mono-Koto or Use Case origin. It preserves, without reconstructing any part of
the origin:

- the exact Component identity and bounded CCDM context;
- the admitted origin identity and its distinct origin-record identity;
- the origin projection category and independently admitted role;
- source and authority attribution with a bounded source locator;
- every applicable distinct condition;
- the proposer;
- a bounded correction or proposal statement;
- stated limitations; and
- an admitted identity-first tie key when one is needed for a total order.

Shared wording, labels, names, a source encounter order, a common display
identity, another projection, or a broad lookup do not establish an origin.
The retained statement is attributable feedback, not a requirement, a
canonical fact, a change instruction, or a selection decision.

## Direct impact records and traceability

For each retained proposal, the projection may show only explicitly admitted
impact records in the same exact Component/context. An impact record is one of
the following categories:

| Impact category | Retained purpose and boundary |
| --- | --- |
| Entity | An already admitted affected Entity projection record and its independent role. |
| Event | An already admitted affected Event projection record and its independent role. |
| Structure | An already admitted affected Structure projection record and its independent role. |
| Workflow | An already admitted affected Workflow projection record and its independent role. |
| StateMachine | An already admitted affected StateMachine projection record and its independent role. |
| Canonical Source Location | A bounded locator for traceability to a canonical-source assertion or scope. |

Every retained impact record carries its exact semantic target identity where
applicable, independent role, source and authority attribution, bounded
locator, distinct conditions, and an admitted stable tie key when necessary.
The records retain their independent meanings; impact membership does not
change Entity ownership, Event cause or consequence, Structure semantics,
Workflow execution, StateMachine lifecycle, or canonical-source authority.

A Canonical Source Location is traceability only. It may identify where an
already admitted assertion or bounded source scope is recorded, but it neither
becomes authority nor grants permission to retrieve, parse, interpret, edit,
or mutate CML or any canonical source.

## Explicit proposal-to-impact links and bounded gaps

A proposal-to-impact association is an explicit attributable semantic
relationship between one retained proposal and one retained impact record. The
link preserves the exact Component/context, both retained identities, their
categories and roles, attribution, bounded locators, distinct conditions, and
limitations. It explains a directly admitted possible impact; it does not
construct, apply, approve, or accept a design change.

The projection does not infer an impact or association from a shared label,
source location, category, nearest relation, display order, common target
identity, Mono/Koto aggregation, Use Case communication relation, Event cause
or consequence, Workflow flow, StateMachine transition, Entity lifecycle
relation, CML, source order, layout, another projection, or a broad query.

If an origin has no admitted record for a requested impact category, the
projection retains an attributable bounded gap. A gap names the retained
proposal, requested impact category, attribution, bounded locator or bounded
unavailable scope, applicable condition, and limitation. It does not claim
that there is no impact, create a negative semantic fact outside that scope,
or authorize lookup, proxying, identity recovery, replacement, merging, or a
hidden winner.

## Conditions and identity-first presentation

Proposals, impact records, links, gaps, attribution, and locators retain each
applicable condition as distinct state: unavailable, unauthorized, redacted,
explicitly absent, ambiguous, conflicting, stale, malformed, and limited.
Explicit absence remains bounded. No other condition is rewritten as absence,
and absence is never broadened into a negative design conclusion.

Presentation is deterministic and identity-first: exact retained proposal or
impact identity leads, then category and role, then remaining retained
identities, and finally an admitted stable non-semantic tie key only when
needed. Caller iteration, labels, names, CML, source order, layout, freshness,
ranking, provider preference, or authority cannot select an impact,
interpretation, ordering, relation, or winner. Where admitted keys cannot make
the order total, the unresolved ordering condition remains explicit.

## Read-only and later-work boundary

P9-60A creates these documentation contracts only. It performs no source I/O
or mutation against CCDM, Mono-Koto, Use Case, Entity, Event, Structure,
Workflow, StateMachine, CML, canonical sources, or feedback. It does not
synthesize, normalize, merge, infer, select, edit, or mutate an analysis
record, impact, canonical source, or main-branch source.

P9-60B separately owns runtime values and executable specifications while
preserving this boundary. P9-61 separately owns any candidate CML, Candidate
Component Design Model, semantic diff, candidate Review, and Git-governed
acceptance. This design defines no API, Web, schema, persistence, lifecycle,
technical-delivery, validation, review, commit, push, publication, deployment,
or Phase/checklist/journal behavior.
