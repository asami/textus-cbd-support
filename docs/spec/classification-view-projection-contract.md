# Classification View Projection Contract

## Scope and status

status=stable
decision_scope=P9-42A
updated_at=2026-09-10

This specification normatively defines one read-only Classification projection
for one exact already established Component identity and one bounded Canonical
Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for CCDM semantic identity, source authority, provenance,
bounded locators, shared conditions, target-gated navigation, and proposal
handling. The [Entity Model Projection Contract](entity-model-projection-contract.md)
and [Structure View Projection Contract](structure-view-projection-contract.md)
retain authority for their respective engineering projections; neither supplies
taxonomy semantics to this view.

This contract specifies a documentation projection boundary only. It does not
claim a Scala model, runtime, executable specification, API, schema,
persistence, Dashboard/Web view, CML/source/Cozy retrieval or mutation,
feedback editor, canonical mutation, or technical-delivery behavior.

## Exact admission and identity-first projection

A Classification projection MUST be scoped to exactly one already established
Component identity and one bounded CCDM context. It MUST contain only exact
CCDM taxonomy relationship identities, subject and endpoint identities,
powertype dimension identities, and taxonomy-field assertions already admitted
in that same scope. A label, name, namespace, type, CML text, hierarchy or
diagram layout, visual proximity, query result, source encounter order, local
copy, Entity/Structure view, another projection, or presentation record MUST
NOT establish, recover, repair, redirect, replace, or broaden a taxonomy
relation, subject, endpoint, dimension, field, or target.

Every retained taxonomy relation, subject, endpoint, dimension, dimension
value or qualifier, field assertion, attribution, bounded locator, gap, and
navigation target MUST retain and keep distinct:

- the exact Component identity and bounded CCDM context;
- its exact admitted semantic element or relationship identity and
  Classification role;
- exact admitted subject and endpoint identities, including admitted direction
  or endpoint role when applicable;
- source and authority attribution;
- a bounded source locator; and
- the applicable distinct shared condition state.

The projection MUST present exact relationship, subject, and dimension identity
before explanatory label or visual rendering. An identity MUST NOT be recovered
from the other identities, a rendered hierarchy, type label, visual nesting,
proximity, or a view-local copy. A locator provides traceability only; it does
not transfer source authority, authorize retrieval, or make a display value an
identity.

## Separate taxonomy roles and dimensions

The projection MAY present a generalization, specialization, trait, category,
or powertype dimension only if the exact admitted identity carries that exact
admitted Classification role. These roles remain distinct. The presence of one
role, a shared endpoint, a visible hierarchy, a type, or an Entity/Structure
relationship MUST NOT normalize it into another role or imply an unadmitted
taxonomy fact.

| Retained assertion | Required interpretation | Forbidden conclusion from its presence |
| --- | --- | --- |
| Generalization relation | Exact admitted generalization identity with exact admitted subjects/endpoints. | Specialization, trait, category, type, parent, child, membership, coverage, exclusivity, or a winner. |
| Specialization relation | Exact admitted specialization identity with exact admitted subjects/endpoints. | Generalization, trait, category, type, parent, child, membership, coverage, exclusivity, or a winner. |
| Trait role or relation | Exact admitted trait assertion for exact retained identities. | Category, type, superclass, specialization, membership, coverage, exclusivity, or a winner. |
| Category role or relation | Exact admitted category assertion for exact retained identities. | Trait, type, superclass, specialization, membership, coverage, exclusivity, or a winner. |
| Powertype dimension | Exact admitted classification dimension identity in its bounded scope. | Another dimension, any dimension value or qualifier, membership, coverage, exclusivity, type, parent, child, or a winner. |
| Dimension value, qualifier, exclusivity, coverage, or membership assertion | Its own exact admitted attributable bounded assertion about an exact dimension and affected identity or relation. | Another value, qualifier, dimension, role, endpoint, negative fact, proxy, or winner. |

Every powertype dimension MUST remain independent even when its subject,
endpoint, label, or value coincides with another dimension. A dimension value,
qualifier, exclusivity, coverage, or membership claim MUST be retained only
when its own assertion, exact dimension identity, attribution, bounded locator,
and condition state are admitted. The projection MUST NOT merge dimensions,
infer dimension membership or coverage, impose exclusivity, treat a dimension
as a type or hierarchy, or select a classification winner. Ambiguous and
conflicting taxonomy material MUST remain attributable and distinct.

## Attributable unsupported-field gaps

When admitted bounded Cozy material does not support a requested taxonomy field
or assertion, the projection MUST retain an explicit attributable gap. Each gap
MUST retain:

1. the exact Component identity and bounded CCDM context;
2. the affected exact admitted taxonomy relation, subject, endpoint, or
   dimension identity when known;
3. the requested Classification role or unsupported Cozy field;
4. source and authority attribution;
5. the bounded locator or bounded unavailable scope; and
6. the applicable distinct condition and limitation reason.

An unsupported-field gap MUST NOT contain an inferred type, parent, child,
dimension, value, qualifier, membership, coverage, exclusivity, endpoint,
replacement source, proxy relationship, negative fact, hidden winner, or
broader assertion. The projection MUST NOT fill a gap from CML text, labels,
names, namespaces, types, query results, source order, hierarchy or diagram
layout, Entity/Structure/other-view material, a local copy, provider preference,
freshness, or plausibility. Explicit absence applies only to the source-declared
bounded scope and MUST NOT become a claim that a Component, subject, endpoint,
relation, or dimension generally lacks the field.

## Conditions and deterministic presentation

The following conditions MUST remain distinct and attributable on every
affected taxonomy relation, subject, endpoint, dimension, field assertion,
attribution, locator, gap, and navigation target:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply requested material; it is not explicit absence. |
| unauthorized | The required admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated scope; it is not a broader negative fact. |
| ambiguous | More than one admissible taxonomy identity, relation, role, dimension, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible taxonomy material; all affected attributions remain visible. |
| stale | Supplied freshness prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A source, authority, projection, compatibility, Cozy-field, or disclosure boundary prevents a stronger claim. |

The projection MUST NOT convert unavailable, unauthorized, redacted, stale,
malformed, ambiguous, conflicting, or limited material into explicit absence.
It MUST NOT convert explicit absence into a broad negative fact, disclose
withheld material, reauthorize a record, resolve ambiguity or conflict, or
select a source winner.

Every collection MUST use deterministic identity-first order: exact admitted
taxonomy relationship, subject, and dimension identities first; then other
stable admitted identities; and only then an admitted stable non-semantic tie
key needed to make the order total. Labels, names, types, CML, query results,
source order, hierarchy or diagram layout, visual relation shape, authority,
freshness, provider preference, ranking, and iteration order MUST NOT choose an
order, interpretation, or winner. If admitted keys cannot establish a total
order, the unresolved ordering condition MUST remain explicit instead of
selecting an iteration order.

## Exact target-gated navigation and read-only boundary

Retention of a taxonomy relation is distinct from navigation. The projection
MAY expose a forward or reverse navigation affordance only when all of the
following are retained and usable:

1. the exact target identity and exact Component/context scope;
2. source attribution and a bounded locator for the retained target relation;
3. the distinct shared condition state of affected source, relation, subject,
   endpoint, dimension, target, attribution, and locator; and
4. an implemented receiving contract for that exact target.

When any gate is unmet, the projection MUST suppress navigation without
deleting, rewriting, proxying, substituting, completing, or reconstructing the
retained relation, subject, endpoint, or dimension. It MUST NOT create a dead
link, loose lookup, broad query, proxy target, reconstructed target, source
transfer, or cross-view semantic reconstruction.

The Classification projection MUST remain read-only with respect to CCDM, Cozy
material, CML, canonical sources, Entity/Structure/Mono-Koto inputs, and every
other projection. It MUST NOT create or mutate a canonical/source record;
infer a taxonomy relation, subject, endpoint, type, parent, child, trait,
category, powertype dimension, value, qualifier, membership, coverage,
exclusivity, source fact, proxy, or winner; parse, retrieve, or mutate
CML/source/Cozy material; or provide editable feedback.

Feedback MAY be represented only as an attributable CCDM semantic proposal. It
MUST identify an affected exact retained identity, bounded scope, source
locator, proposer, and limitations. It MUST NOT mutate the Classification
projection, CCDM, Cozy material, CML, or canonical source. Only a candidate
change in the owning source followed by Git-governed acceptance MAY change
canonical semantics.

This contract does not authorize runtime, executable-specification, API,
schema, persistence, CML/source/Cozy retrieval or mutation, Web or technical
delivery behavior, feedback mutation, Phase/checklist/journal update,
validation, review, commit, push, publication, deployment, or later Phase
work. P9-42B separately owns runtime implementation and executable
specifications for this projection.
