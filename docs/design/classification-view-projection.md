# Classification View Projection

status=stable
decision_scope=P9-42A
updated_at=2026-09-10

## Purpose and authority

This design fixes one read-only Classification projection for one exact already
established Component identity and one bounded Canonical Component Design Model
(CCDM) context. It refines the Classification role in the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and preserves the Entity and Structure boundaries in the
[Entity Model Projection](entity-model-projection.md) and
[Structure View Projection](structure-view-projection.md). The CCDM and its
[contract](../spec/canonical-component-design-model-contract.md) retain
authority for admission, semantic identity, source attribution, bounded
locators, shared condition state, target-gated navigation, and feedback.

The Classification projection makes already admitted taxonomy assertions
inspectable. It is not a source reader, CML interpreter, Entity or Structure
normalizer, hierarchy-layout model, canonical model, or independent source of
design truth. Its companion
[Classification View Projection Contract](../spec/classification-view-projection-contract.md)
defines the normative requirements.

## Exact taxonomy ledger

The projection begins with exactly one Component identity and one bounded CCDM
context. It retains only already admitted taxonomy relationship identities,
subject and endpoint identities, powertype dimension identities, and separately
admitted taxonomy-field assertions in that exact pair. A relationship or
subject is retained identity first; its label, narrative, diagram, nesting, or
rendering is explanatory only.

Every retained taxonomy relation, subject, endpoint, powertype dimension,
dimension value or qualifier, attribution, bounded locator, gap, and navigation
target retains the exact Component/context; its exact admitted semantic element
or relationship identity and Classification role; exact admitted subject and
endpoint identities with direction or endpoint role when applicable; asserting
source and authority attribution; a bounded source locator; and distinct shared
condition state.

No identity is reconstructed from a name, namespace, type, hierarchy shape,
diagram geometry, visual proximity, source encounter order, CML text,
Entity/Structure record, another projection, or a view-local copy. A locator
provides traceability only; it does not authorize re-interpretation or make a
display value an identity.

## Independent taxonomy roles

Generalization, specialization, trait, category, and every powertype dimension
are independent admitted roles. A shared subject or endpoint, a visible parent
or child position, a rendered hierarchy, or an Entity/Structure relationship
does not establish any of them.

| Retained role | Meaning | It does not establish |
| --- | --- | --- |
| Generalization relation | An exact admitted generalization assertion and endpoints. | Specialization, parent/child, trait, category, membership, coverage, exclusivity, or winner. |
| Specialization relation | An exact admitted specialization assertion and endpoints. | Generalization, parent/child, trait, category, membership, coverage, exclusivity, or winner. |
| Trait role or relation | An exact admitted trait assertion. | Category, type, superclass, specialization, membership, coverage, exclusivity, or winner. |
| Category role or relation | An exact admitted category assertion. | Trait, type, superclass, specialization, membership, coverage, exclusivity, or winner. |
| Powertype dimension | An exact admitted dimension in its bounded scope. | Another dimension, its values, qualifiers, membership, coverage, exclusivity, type, parent, child, or winner. |
| Dimension value, qualifier, exclusivity, coverage, or membership assertion | Its own exact admitted assertion for one dimension and affected identity or relation. | Another value, qualifier, dimension, relation, negative fact, or winner. |

The projection never treats generalization as a reversible shortcut for
specialization, a trait as a category, a category as a type, or a powertype
dimension as another dimension. Multiple powertype dimensions remain
independently retained even when their subjects, endpoints, values, or labels
coincide. A value, qualifier, exclusivity, coverage, or membership claim is
retained only with its separately admitted assertion, dimension identity,
attribution, locator, and condition. Ambiguous or conflicting claims remain
attributable; the projection neither merges them nor selects a hidden winner.

## Explicit Cozy taxonomy-field gaps

When admitted bounded Cozy material does not support a requested taxonomy field
or assertion, the projection records an attributable gap rather than completing
taxonomy locally. Each gap records the exact Component/context; affected exact
admitted relation, subject, endpoint, or dimension identity when known; the
requested role or unsupported Cozy field; source and authority attribution; a
bounded locator or bounded unavailable scope; and distinct condition and
limitation reason.

A gap holds no inferred type, parent, child, dimension, value, qualifier,
membership, coverage, exclusivity, endpoint, replacement source, proxy
relationship, negative fact, or hidden winner. Explicit absence remains limited
to its source-declared scope, not a claim that a Component, subject, relation,
endpoint, or dimension generally lacks the requested field.

## Conditions, presentation, and exact navigation

Every affected relation, subject, endpoint, dimension, field assertion,
attribution, locator, gap, and navigation target preserves distinct CCDM
conditions: unavailable, unauthorized, redacted, explicitly absent, ambiguous,
conflicting, stale, malformed, and limited. None is converted into another or
resolved from freshness, detail, authority, display order, hierarchy layout, or
plausibility.

All collections use deterministic identity-first order: exact admitted taxonomy
relationship, subject, and dimension identities first; then other stable
admitted identities; then only an admitted stable non-semantic tie key needed
to make the order total. Labels, names, types, CML, source order, diagram
geometry, layout, category preference, authority, freshness, ranking, provider
preference, and iteration order never choose presentation, interpretation, or a
winner. If the keys cannot make an order total, the unresolved condition remains
visible.

Retention does not expose forward or reverse navigation. An affordance is
available only when the exact target identity and Component/context, source
attribution, bounded locator, usable distinct shared conditions, and an
implemented receiving contract are retained. A failed gate suppresses the
affordance without deleting, rewriting, proxying, substituting, completing, or
reconstructing the relation, subject, endpoint, or dimension.

## Cross-view, feedback, and non-implementation boundary

An exact admitted cross-view identity relationship is target-gated navigation
and explanation only. It does not transfer authority or reconstruct
Classification semantics from Entity, Structure, Mono-Koto, Use Case, or
another view. Entity/Structure material is not taxonomy evidence.

Feedback is only an attributable CCDM semantic proposal naming its affected
identity, source locator, scope, proposer, and limitation. It does not mutate
CCDM, Cozy material, CML, or a canonical source; a source-owned candidate
change and Git-governed acceptance remain required.

P9-42A creates no runtime value, executable specification, API, schema,
persistence, CML/source/Cozy retrieval or mutation, Web route or renderer,
technical delivery, feedback editor, validation result, review, commit, push,
publication, deployment, Phase/checklist/journal update, or later Phase work.
P9-42B separately owns runtime implementation and executable specifications.
