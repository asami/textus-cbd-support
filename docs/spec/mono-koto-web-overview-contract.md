# Mono-Koto Web Overview Contract

status=stable
decision_scope=P9-31A
updated_at=2026-09-10

## Scope and status

This specification normatively defines the read-only stakeholder Mono-Koto Web overview for one exact Component. It builds on the [Component Dashboard Web Entry Contract](component-dashboard-web-entry-contract.md) for affirmative exact-Component Web delivery and on the [Mono-Koto Projection Contract](mono-koto-projection-contract.md) for stakeholder subjects, source references, identity, attribution, conditions, ordering, and target-gated navigation. Those predecessor contracts retain authority.

This contract defines neither a Web route nor a renderer, transport, public API/schema, persistence representation, source-retrieval mechanism, CML behavior, feedback editor, Bridge, Use Case model, runtime value, or executable specification. It does not assert that any such implementation exists.

## Required exact input admission

An overview MUST receive exactly:

1. one already affirmative `ComponentDashboardWebEntry`, including its exact delivered Component identity; and
2. one already valid `MonoKotoProjection` whose Component identity is exactly equal to that delivered Component identity.

The overview MUST compare those Component identities directly. It MUST NOT broaden, narrow, normalize, map, resolve, retrieve, filter, rebuild, or substitute either input. A label, name, near Component, route fragment, session, authorization-like display value, source locator, CML text, source order, layout, provider output, engineering view, or local copy MUST NOT establish, recover, replace, or approximate an input identity or Web-entry admission.

An absent, non-affirmative, mismatched, or otherwise invalid input tuple MUST NOT produce a substitute overview. That state MUST NOT be represented as a Component, projection, source, or evidence absence.

## Default stakeholder presentation

For every admitted Mono/Koto subject, the default surface MUST use its existing attributed stakeholder subject label and source-admitted simple relationship and target labels. Aggregate, Entity, Value, Command, Event, Workflow, StateMachine, StructuralDomain, BehavioralTemporal, CML, source selection, and model notation MUST NOT be default presentation fields.

For every displayed subject and simple relationship, the overview MUST retain:

- its exact Mono/Koto source subject or reference identity;
- its exact semantic target identity and exact Component scope;
- source attribution and bounded source locator, subject to their disclosure conditions;
- the shared Mono/Koto condition state; and
- any separately admitted presentation attribution and presentation condition.

The overview MAY provide concise stakeholder vocabulary only when detailed conditions and permitted attribution remain directly available. A stakeholder label, display name, local copy, or presentation relation MUST NOT replace or recover a missing source identity, semantic target, Component scope, relation, provenance, locator, or condition.

## Relationship and target-label admission

Every displayed simple relationship MUST be directly linked to an admitted Mono/Koto source reference and its exact semantic target identity. Its relationship and target vocabulary MUST be explicit source-admitted presentation material linked to that exact retained reference.

The overview MUST NOT fabricate a human-readable relation or target label from a semantic identifier, technical type or classification, CML, a label from another subject, source authority, source order, layout, an engineering projection, or any view-local copy. None of those values may create a relation, recover an identity or provenance, repair a condition, or add authority to a presentation record.

## Conditions, attribution, and no-hidden-winner behavior

The overview MUST retain the following distinct, visible, attributable conditions for each affected source or presentation record, relationship, and navigation affordance:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The source or bounded record cannot supply the material; this is not explicit absence. |
| unauthorized | The required admission or access basis is lacking; this is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; this is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated scope; this is not a broad negative fact. |
| ambiguous | More than one admitted identity, relation, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; their affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, or disclosure boundary prevents a stronger claim. |

A concise audience phrase MAY coexist with a detailed condition and permitted attribution, but MUST NOT replace either. The overview MUST NOT remove, rewrite, repair, disclose, or complete an affected record in response to a condition. It MUST NOT select an authority, rank records, resolve ambiguity, choose a conflicting source, or create a hidden winner.

## Deterministic identity-first presentation

The overview MUST inherit the Mono-then-Koto, identity-first subject and reference ordering established by `MonoKotoProjection`. Separately admitted presentation labels, relationship labels, and target labels MUST NOT rank, reorder, select, resolve an ambiguity, transfer source authority, or introduce a source-order fallback. Source iteration order, labels, names, CML, technical classification, layout, authority, freshness, provider preference, or an engineering projection MUST NOT determine an order or interpretation.

Where inherited admitted ordering cannot become total, the overview MUST retain the unresolved ordering condition. It MUST NOT choose a local iteration order, semantic bridge, or hidden winner.

## Exact target-gated drill-down

The overview MAY expose drill-down only when all of the following hold:

1. the existing `MonoKotoProjection` retains the exact target-gated navigation target for the displayed source subject or reference;
2. the target remains in the exact Component scope delivered by the affirmative `ComponentDashboardWebEntry`; and
3. the target's attribution, locator, condition, and implemented-and-usable admission remain intact.

The overview MUST pass the exact retained target directly. It MUST NOT create a link, proxy, loose lookup, fallback, replacement, or destination from a label, name, route fragment, locator, CML, source order, layout, technical type, engineering view, or local copy.

When a target is unavailable, unauthorized, redacted, explicitly absent, ambiguous, conflicting, stale, malformed, limited, or unimplemented, the overview MUST keep its condition visible and MUST NOT create a dead link, proxy, loose lookup, fallback, or substitute target.

## Read-only prohibitions and later work

The overview MUST remain read-only. It MUST NOT edit feedback, CCDM, CML, or any owning source; retrieve a source; construct a renderer, HTTP route, transport, API/schema, persistence model, catalog/provider/Review behavior, Bridge, Use Case model, executable specification, or Phase/checklist status behavior. It MUST NOT make a source-authority, canonical-fact, completion, or implementation claim beyond the two admitted predecessor inputs.

P9-32 semantic-bridge behavior, P9-33 Use Case behavior, and all later Phase 9 behavior require separately admitted contracts.
