# Structure View Projection Contract

## Scope and status

status=stable
decision_scope=P9-41A
updated_at=2026-09-10

This specification normatively defines one read-only Structure projection for
one exact already established Component identity and one bounded Canonical
Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for CCDM semantic identity, source authority, provenance,
bounded locators, shared conditions, target-gated navigation, and proposal
handling. The [Entity Model Projection Contract](entity-model-projection-contract.md)
retains authority for Entity, Value, Aggregate, ownership, lifecycle, and
aggregate-boundary presentation.

This contract specifies a documentation projection boundary only. It does not
claim a Scala model, runtime, executable specification, API, schema,
persistence, Dashboard/Web view, CML/source/Cozy retrieval or mutation,
feedback editor, canonical mutation, or technical-delivery behavior.

## Exact admission and relation-identity-first projection

A Structure projection MUST be scoped to exactly one already established
Component identity and one bounded CCDM context. It MUST contain only exact
CCDM semantic relationship identities already admitted in that same scope and
their exact already admitted endpoint identities. A label, CML text, layout,
visual relation shape, name, namespace, query result, source encounter order,
local copy, Entity/Mono-Koto view, another projection, or presentation record
MUST NOT establish, recover, repair, redirect, replace, or broaden a
relationship, endpoint, structural role, metadata item, or target.

Every retained relationship, endpoint, structural-field assertion, attribution,
bounded locator, gap, and navigation target MUST retain and keep distinct:

- the exact Component identity and bounded CCDM context;
- its exact admitted semantic relationship identity and structural role;
- the exact admitted endpoint identity or identities, and an admitted endpoint
  role or direction when applicable;
- source and authority attribution;
- a bounded source locator; and
- the applicable distinct shared condition state.

The projection MUST present relation identity before explanatory label or
visual rendering. Relationship and endpoint identities MUST NOT be recovered
from each other, a rendered line, nesting, proximity, labels, or a view-local
copy. A source locator provides traceability only; it does not transfer source
authority, authorize retrieval, or turn a display value into an identity.

## Structural roles and independently admitted assertions

The projection MAY present a relationship as composition, aggregation,
association, containment, or ownership only if that exact admitted relationship
identity carries that exact admitted structural role. The roles remain distinct.
The presence of any role MUST NOT normalize it into another role or imply an
unadmitted structural or Entity-Model fact.

| Retained assertion | Required interpretation | Forbidden conclusion from its presence |
| --- | --- | --- |
| Composition relation | Exact admitted composition identity and endpoints. | Aggregation, containment, ownership, independent existence, reassignment, deletion/lifecycle, cardinality, or navigability. |
| Aggregation relation | Exact admitted aggregation identity and endpoints. | Composition, containment, ownership, independent existence, reassignment, deletion/lifecycle, cardinality, or navigability. |
| Association relation | Exact admitted association identity and endpoints. | Composition, aggregation, containment, ownership, independent existence, reassignment, deletion/lifecycle, cardinality, or navigability. |
| Containment relation | Exact admitted containment identity and endpoints. | Composition, aggregation, ownership, independent existence, reassignment, deletion/lifecycle, cardinality, or navigability. |
| Ownership relation | Exact admitted structural ownership identity and endpoints. | Composition, aggregation, association, containment, independent existence, reassignment, deletion/lifecycle, cardinality, or navigability. |
| Independent-existence, reassignment, deletion/lifecycle, cardinality, or navigability assertion | Its own exact admitted, attributable bounded assertion about a retained relation or endpoint. | Any other field, a replacement source, an endpoint, a role, or a navigation affordance. |

Independent existence, reassignment, deletion or lifecycle effect, cardinality,
and navigability MUST be retained only when their own assertions have been
admitted. Each such assertion MUST keep its exact affected relation or endpoint
identity, source and authority attribution, bounded locator, and condition
state. A relation, an endpoint, an Entity ownership/lifecycle assertion, or a
visual shape implies none of these fields.

An Entity-Model ownership, lifecycle, or aggregate-boundary relationship MUST
NOT be displayed as a Structure relation unless the CCDM admits an exact
structural relationship identity and structural role in the same
Component/context. The Structure projection MUST NOT create or normalize a
structural role from Entity/Value/Aggregate display, endpoint type, relation
shape, or diagram geometry.

## Attributable unsupported-field gaps

When admitted bounded Cozy material does not support a requested structural
field or assertion, the projection MUST retain an explicit attributable gap.
Each gap MUST retain:

1. the exact Component identity and bounded CCDM context;
2. the affected exact admitted relationship or endpoint identity when known;
3. the requested structural role or unsupported Cozy field;
4. source and authority attribution;
5. the bounded locator or bounded unavailable scope; and
6. the applicable distinct condition and limitation reason.

An unsupported-field gap MUST NOT contain an inferred field value, endpoint,
replacement source, proxy relationship, negative fact, hidden winner, or
broader assertion. The projection MUST NOT fill a gap from CML text, labels,
names, namespaces, query results, source order, diagram layout, visual shape,
Entity/Mono-Koto/other-view material, a local copy, provider preference,
freshness, or plausibility. Explicit absence applies only to the
source-declared bounded scope and MUST NOT become a claim that a Component,
relationship, or endpoint generally lacks the field.

## Conditions and deterministic presentation

The following conditions MUST remain distinct and attributable on every
affected relation, endpoint, structural-field assertion, attribution, locator,
gap, and navigation target:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply requested material; it is not explicit absence. |
| unauthorized | The required admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated scope; it is not a broader negative fact. |
| ambiguous | More than one admissible identity, relation, role, endpoint, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; all affected attributions remain visible. |
| stale | Supplied freshness prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A source, authority, projection, compatibility, Cozy-field, or disclosure boundary prevents a stronger claim. |

The projection MUST NOT convert unavailable, unauthorized, redacted, stale,
malformed, ambiguous, conflicting, or limited material into explicit absence.
It MUST NOT convert explicit absence into a broad negative fact, disclose
withheld material, reauthorize a relation, resolve ambiguity or conflict, or
select a source winner.

Every collection MUST use deterministic identity-first order: exact admitted
relationship identity first; then exact endpoint identities and other stable
admitted identities; and only then an admitted stable non-semantic tie key
needed to make the order total. Labels, names, CML, query results, source
order, layout, visual relation shape, authority, freshness, provider
preference, ranking, and iteration order MUST NOT choose an order,
interpretation, or winner. If admitted keys cannot establish a total order, the
unresolved ordering condition MUST remain explicit instead of selecting an
iteration order.

## Target-gated navigation and read-only boundary

Retention of a relation is distinct from navigation. The projection MAY expose
a navigation affordance only when all of the following are retained and usable:

1. the exact target identity and exact Component/context scope;
2. source attribution and a bounded locator for the retained target relation;
3. the distinct shared condition state of affected source, relation, endpoint,
   target, attribution, and locator; and
4. an implemented receiving contract for that exact target.

When any gate is unmet, the projection MUST suppress navigation without
deleting, rewriting, proxying, substituting, completing, or reconstructing the
retained relation or endpoint. It MUST NOT create a dead link, loose lookup,
broad query, proxy target, reconstructed target, source transfer, or cross-view
semantic reconstruction.

The Structure projection MUST remain read-only with respect to CCDM, Cozy
material, CML, canonical sources, Entity/Mono-Koto inputs, and every other
projection. It MUST NOT create or mutate a canonical/source record; infer a
structural relation, endpoint, role, independent-existence, reassignment,
deletion/lifecycle, cardinality, navigability, source fact, or winner; parse,
retrieve, or mutate CML/source/Cozy material; or provide editable feedback.

Feedback MAY be represented only as an attributable CCDM semantic proposal. It
MUST identify an affected exact retained identity, bounded scope, source
locator, proposer, and limitations. It MUST NOT mutate the Structure
projection, CCDM, Cozy material, CML, or canonical source. Only a candidate
change in the owning source followed by Git-governed acceptance MAY change
canonical semantics.

This contract does not authorize runtime, executable-specification, API,
schema, persistence, CML/source/Cozy retrieval or mutation, Web or technical
delivery behavior, feedback mutation, Phase/checklist/journal update,
validation, review, commit, push, publication, deployment, or later Phase
work. Those behaviors require separately admitted contracts.
