# Use Case Communication Projection Contract

status=stable
decision_scope=P9-33A
updated_at=2026-09-10

## Scope and status

This specification normatively defines one read-only Use Case communication
projection for one exact already established Component identity and one bounded
Canonical Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for shared semantic identity, source authority, attribution,
bounded locators, conditions, and cross-view navigation. The
[Mono-Koto Projection Contract](mono-koto-projection-contract.md) and
[Mono-Koto Semantic Bridge Contract](mono-koto-semantic-bridge-contract.md)
retain authority for Mono/Koto admission, aggregation, and exact bridge
relations.

This contract specifies a documentation projection boundary only. It does not
claim a typed model, runtime, executable specification, CML/source processing,
feedback editor, canonical mutation, Mono-Koto bridge, Workflow
implementation, Entity/Event/StateMachine model, Web route or renderer,
transport/API/schema, persistence, or technical delivery behavior.

## Exact admission and retained records

A Use Case communication projection MUST be scoped to one exact already
established Component identity and one bounded CCDM context. It MUST contain
only already admitted exact CCDM semantic identities and already admitted exact
semantic relationship identities in that same scope. A display label, name,
CML text, incidental source order, layout, another view, broad query, or
view-local copy MUST NOT establish, recover, replace, repair, redirect, or
broaden a record or link.

For every admitted Use Case, the projection MUST retain the exact Use Case
identity and, where actually admitted, exact identities and relationships for:

- actor;
- goal;
- trigger;
- zero or more flow records and flow relations;
- zero or more postcondition records and relations;
- zero or more domain-element records and relations;
- zero or more collaborator records and relations;
- realizing Workflow; and
- related Mono and Koto identities and relations.

For every retained record or relationship, including an affected attribution,
bounded locator, flow entry, and navigation target, the projection MUST retain:

- the exact Component identity and bounded CCDM context;
- its exact semantic element or relationship identity and admitted role;
- source attribution;
- a bounded source locator; and
- distinct shared conditions for availability, authorization, redaction,
  explicit absence, ambiguity, conflict, staleness, malformed material, and
  limitation.

A source locator is traceability to the asserting source. It MUST NOT transfer
source authority, authorize source retrieval, or turn a label or view-local
record into an identity. A category not actually admitted MUST remain explicit
for the bounded scope; the projection MUST NOT locally complete it or assert a
broader negative design fact.

## Source-owned flow order

Each retained flow MUST be an attributable source-owned ordered sequence of
already admitted exact relationship identities. Every presented flow relation
MUST retain its explicit admitted sequence key, source attribution, bounded
locator, and shared conditions. The explicit admitted sequence key alone MAY
present the source-owned flow order.

Flow order MUST NOT be established, recovered, repaired, or selected from
iteration order, display label, name, CML text, incidental source encounter
order, authority, freshness, layout, provider preference, plausibility, or
another view. A source-owned sequence key MUST NOT be interpreted as a
requirement, canonical-design fact, workflow-causality fact, source-authority
transfer, or Component-selection decision.

When a flow order is missing, unavailable, unauthorized, redacted, explicitly
absent, ambiguous, conflicting, stale, malformed, or limited, the projection
MUST retain and disclose that attributable condition. It MUST NOT use a fallback
iteration order, manufacture a sequence, select a hidden winner, or imply
causality.

Every collection other than a source-owned flow sequence MUST be deterministic
and identity-first. It MUST order by exact admitted semantic identity, then by
remaining stable admitted identities, and only then by an admitted stable
non-semantic tie key needed to make the order total. Labels, names, CML, source
order, layout, authority, freshness, ranking, provider preference, and
iteration order MUST NOT determine an order, interpretation, or winner. If the
admitted keys cannot make the order total, the projection MUST preserve the
unresolved ordering condition instead of selecting an order.

## Mono-Koto and realizing-Workflow relations

A Mono-Koto or realizing-Workflow relation MUST retain an already admitted
exact shared semantic identity and relationship identity in the same exact
Component/context. Such a relation is communication and navigation only. It
MUST NOT create a requirement, canonical design fact, Workflow causality,
Entity/Event semantics, Component selection, a source-authority transfer, or a
replacement relation in another view.

The projection MUST NOT infer an actor, goal, trigger, flow, postcondition,
domain element, collaborator, Workflow, Mono, Koto, relation role, endpoint,
or causality from names, labels, CML text, source order, visual arrangement,
lexical similarity, plausibility, a broad query, or a view-local copy.

## Conditions and target-gated navigation

Every affected Use Case record, relation, source attribution, bounded locator,
flow entry, and navigation affordance MUST retain, distinguish, and attribute
the following conditions:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply material; this is not explicit absence. |
| unauthorized | The necessary admission or access basis is lacking; this is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; this is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated scope; this is not a broad negative fact. |
| ambiguous | More than one admitted identity, relation, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; the affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, or disclosure boundary prevents a stronger claim. |

Navigation MAY be exposed only when the exact retained target identity, exact
Component/context scope, source attribution, bounded locator, usable shared
conditions, and an implemented receiving contract are all present. The target
MUST be the exact retained target; loose lookup, a broad query, a proxy target,
or a reconstructed target is forbidden.

When any target gate is unmet, the projection MUST suppress navigation without
deleting, rewriting, completing, proxying, substituting, or reconstructing the
retained relation. It MUST NOT create a dead link, reauthorize material,
disclose withheld data, convert unavailable material into explicit absence, or
resolve ambiguity, conflict, or incomplete semantics.

## Read-only authority and prohibitions

The projection MUST remain read-only with respect to CCDM, CML, all canonical
sources, Mono-Koto inputs, Workflow identities, and every other projection. It
MUST NOT create or mutate a canonical/source record; infer a requirement,
interaction, workflow causality, selected Component, missing semantics, or
winner; rank or select sources; make a source locator an authority; parse,
retrieve, or mutate CML/source material; or provide editable feedback.

This contract does not authorize runtime or executable-specification work,
Mono-Koto bridge or Workflow implementation, Entity/Event/StateMachine work,
Web/API/schema/persistence/technical-delivery behavior, Phase/checklist
closure, validation, review, commit, push, publication, or deployment. Those
behaviors require separately admitted later contracts.
