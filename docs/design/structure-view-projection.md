# Structure View Projection

status=stable
decision_scope=P9-41A
updated_at=2026-09-10

## Purpose and authority

This design fixes one read-only Structure projection for one exact already
established Component identity and one bounded Canonical Component Design Model
(CCDM) context. It refines the Structure role in the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and preserves the Entity boundary in the
[Entity Model Projection](entity-model-projection.md). The CCDM and its
[contract](../spec/canonical-component-design-model-contract.md) retain
authority for admission, semantic identity, source attribution, bounded
locators, shared condition state, target-gated navigation, and feedback.

The Structure projection makes already admitted static relationship assertions
inspectable. It is not a source reader, CML interpreter, Entity semantic
normalizer, diagram-semantic model, lifecycle engine, canonical model, or
independent source of design truth. Its companion
[Structure View Projection Contract](../spec/structure-view-projection-contract.md)
defines the normative requirements.

## Exact relation ledger

The projection begins with exactly one Component identity and one bounded CCDM
context. It retains only already admitted semantic relationship identities and
their exact endpoint identities in that pair. A relation is retained identity
first; its label, narrative, or rendering is explanatory only.

Every retained relation, endpoint, structural-field assertion, attribution,
bounded locator, gap, and navigation target carries direct links to:

- the exact Component identity and bounded CCDM context;
- its exact admitted relationship identity and admitted structural role;
- its exact admitted endpoint identity or identities, including admitted
  direction or endpoint role where applicable;
- asserting source and authority attribution;
- a bounded source locator; and
- distinct shared condition state.

The retained relation identity is not reconstructed from a pair of endpoint
labels. Neither endpoint identity is reconstructed from relation shape,
diagram geometry, a local copy, a name, namespace, query result, CML text,
source encounter order, an Entity or Mono-Koto record, or another view.

## Narrow static relation roles

The view may present composition, aggregation, association, containment, and
ownership only when an exact admitted relationship identity has that exact
structural role. The role is a source-attributed assertion about that relation;
it is not a consequence of the endpoint kinds, an Entity ownership record, a
visual line, or a diagram's nesting.

| Retained structural role | Meaning in this projection | It does not establish |
| --- | --- | --- |
| Composition | An exact admitted composition relationship. | Aggregate membership, root selection, independent existence, reassignment, deletion, cardinality, or navigability. |
| Aggregation | An exact admitted aggregation relationship. | Composition, containment, ownership, lifecycle, or endpoint replacement. |
| Association | An exact admitted association relationship. | Composition, aggregation, containment, ownership, lifecycle, or navigability. |
| Containment | An exact admitted containment relationship. | Composition, aggregation, ownership, independent existence, deletion, or replacement semantics. |
| Ownership | An exact admitted structural ownership relationship. | Composition, aggregation, containment, independent existence, reassignment, deletion, cardinality, or navigability. |

An admitted Entity-Model ownership, lifecycle, or aggregate-boundary assertion
remains an Entity assertion unless that same scoped ledger admits an exact
structural relationship identity and role. The Structure projection never
normalizes a displayed Entity relationship into a structural role.

## Independently admitted structural fields

Independent existence, reassignment, deletion or lifecycle effect, cardinality,
and navigability are not relation labels or defaults. Each may be retained only
as its own exact admitted, attributable bounded assertion about the retained
relationship or endpoint. Its provenance, bounded locator, affected relation
and endpoint identities, and condition state remain visible beside the
assertion.

The presence of a relationship or endpoint implies none of those fields. In
particular, the projection cannot derive a part's independent existence, a
replacement source, deletion propagation, multiplicity, direction, or target
navigation from composition, aggregation, association, containment, ownership,
Entity/Value/Aggregate presentation, a diagram, or a source label.

## Explicit Cozy structural-field gaps

When admitted bounded Cozy material does not support a requested structural
field, the projection keeps an attributable gap rather than completing the
relation. A gap records the exact Component/context; affected exact relation or
endpoint identity when known; requested role or unsupported field; source and
authority attribution; bounded locator or unavailable scope; and distinct
condition and limitation reason.

A gap holds no inferred endpoint, replacement source, negative fact, inferred
field value, proxy relation, or hidden winner. Explicit absence remains limited
to its source-declared scope. It does not mean that the Component or relation
generally lacks the requested field.

## Conditions, presentation, and navigation

Every affected relation, endpoint, structural-field assertion, attribution,
locator, gap, and navigation target preserves the distinct CCDM conditions:
unavailable, unauthorized, redacted, explicitly absent, ambiguous, conflicting,
stale, malformed, and limited. These conditions remain attributable; none is
silently converted into another or resolved through source freshness, detail,
authority, display order, or plausibility.

All collections use deterministic identity-first order: exact relationship
identity first, then exact endpoint identities and other stable admitted
identities, and only then an admitted stable non-semantic tie key needed to
make the order total. Labels, names, CML, query results, layout, visual
relation shape, source order, provider preference, and iteration order never
choose presentation, interpretation, or a winner. If the admitted keys cannot
make an order total, the unresolved ordering condition remains visible.

Retaining a relation does not expose its endpoints as navigable targets.
Navigation is available only when the exact target identity and exact
Component/context, source attribution, bounded locator, usable distinct shared
conditions, and an implemented receiving contract are retained. A failed gate
suppresses navigation without deleting, rewriting, proxying, substituting, or
reconstructing the relation or endpoint.

## Cross-view, feedback, and non-implementation boundary

An exact admitted cross-view identity relationship may be shown only as
target-gated navigation and explanation. It does not transfer authority or
reconstruct Structure semantics from Entity, Mono-Koto, Use Case, or a later
Classification, Event, Workflow, or StateMachine view.

Feedback is only an attributable CCDM semantic proposal naming its affected
retained identity, source locator, scope, proposer, and limitation. It does not
mutate CCDM, Cozy material, CML, or any canonical source; a source-owned
candidate change and Git-governed acceptance remain required.

P9-41A creates no runtime value, executable specification, API, schema,
persistence, CML/source/Cozy retrieval or mutation, Web route or renderer,
technical delivery, feedback editor, validation result, review, commit, push,
publication, deployment, Phase/checklist/journal update, or later Phase work.
