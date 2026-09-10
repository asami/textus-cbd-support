# Entity Model Projection Contract

## Scope and status

status=stable
decision_scope=P9-40A
updated_at=2026-09-10

This specification normatively defines one read-only Entity Model projection
for one exact already established Component identity and one bounded Canonical
Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for CCDM identity, source authority, provenance, bounded
locators, shared conditions, navigation, and proposal handling. The
[Mono-Koto Projection Contract](mono-koto-projection-contract.md) and
[Use Case Communication Projection Contract](use-case-communication-projection-contract.md)
retain authority for their respective communication projections.

This contract specifies a documentation projection boundary only. It does not
claim a Scala model, runtime, API, executable specification, schema,
persistence, Dashboard/Web view, Cozy/source or CML retrieval, feedback
editor, canonical mutation, or technical-delivery behavior.

## Exact admission and projection roles

An Entity Model projection MUST be scoped to exactly one already established
Component identity and one bounded CCDM context. It MUST contain only exact
CCDM semantic element and semantic relationship identities already admitted in
that same scope. A label, name, namespace, CML text, query, source order,
diagram layout, visual proximity, Mono-Koto, Use Case, another projection, or
view-local copy MUST NOT establish, recover, repair, redirect, replace, or
broaden a projected subject, metadata item, relationship, endpoint, or target.

The projection MAY retain only the following roles when each is already
admitted with its own supporting assertion:

| Role | Required interpretation | Forbidden conclusion |
| --- | --- | --- |
| Entity | An identity-bearing domain subject. | Every Mono is an Entity; an Entity has inferred ownership, lifecycle, membership, or structural semantics. |
| Value | A value-bearing domain subject. | The Value is an Entity, owned, classified, or contained without an admitted assertion. |
| Aggregate | An aggregate-boundary subject. | Membership, root status, composition, aggregation, or deletion semantics without an admitted assertion. |
| Identity metadata | A source-admitted identity assertion for an exact retained subject. | A reconstructed key, generated persistence identity, or identity inferred from text. |
| Ownership metadata or relationship | A source-admitted bounded ownership assertion between exact identities. | General association, composition, aggregation, containment, independent-existence, reassignment, or deletion semantics. |
| Lifecycle metadata or relationship | A source-admitted lifecycle assertion for an exact subject. | A state machine, transition, trigger, event, workflow, causal effect, or lifecycle engine. |
| Aggregate-boundary metadata or relationship | A source-admitted boundary assertion for exact identities. | Membership, root selection, or a Structure-model relationship not directly admitted. |

For every retained subject, metadata item, relationship, attribution, bounded
locator, gap, and navigation target, the projection MUST retain and keep
distinct:

- the exact Component identity and bounded CCDM context;
- its exact admitted element or relationship identity and Entity-Model role;
- source and authority attribution;
- a bounded source locator; and
- its applicable shared condition state.

A displayed Entity, Value, Aggregate, metadata item, or relationship MUST NOT
become evidence for a different missing role. A retained ownership, lifecycle,
or aggregate-boundary relationship MUST keep its own relationship identity and
MUST NOT be silently normalized into an association, composition, aggregation,
containment, class relation, classification, event relation, workflow flow, or
state-machine relation.

## Identity, metadata, and attributable unsupported-field gaps

Identity, ownership, lifecycle, and aggregate-boundary metadata MUST be shown
only when the exact metadata assertion or relationship is already admitted.
Its admitted source attribution, bounded locator, exact subject and endpoint
identities where applicable, and distinct condition state MUST remain directly
available. The existence of a retained Entity, Value, or Aggregate MUST NOT
imply any such metadata.

When admitted bounded Cozy material does not support a requested Entity-Model
field or metadata assertion, the projection MUST retain an explicit,
attributable gap. Each gap MUST retain:

1. the exact Component identity and bounded CCDM context;
2. the affected exact admitted subject or relationship identity when known;
3. the requested projection role and unsupported Cozy field or bounded
   metadata assertion;
4. source and authority attribution;
5. the bounded locator or bounded unavailable scope; and
6. the applicable distinct condition and limitation reason.

An unsupported-field gap MUST NOT contain an inferred field value, replacement
source, proxy relationship, inferred endpoint, hidden winner, or broader
negative assertion. The projection MUST NOT fill it from CML text, labels,
names, namespaces, queries, source order, diagram layout, Mono-Koto, Use Case,
another view, a local copy, provider preference, freshness, or plausibility.
Explicit absence applies only to the source-declared bounded scope and MUST NOT
be widened into a claim that the Component generally lacks the field.

## Conditions and deterministic presentation

The following shared conditions MUST remain distinct and attributable on every
affected subject, metadata item, relationship, attribution, locator, gap, and
navigation target:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply the requested material; it is not explicit absence. |
| unauthorized | The necessary admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated bounded scope; it is not a broader negative fact. |
| ambiguous | More than one admissible identity, relation, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; the affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, Cozy-field, or disclosure boundary prevents a stronger claim. |

The projection MUST NOT convert unavailable, unauthorized, redacted, stale,
malformed, ambiguous, conflicting, or limited material into explicit absence.
It MUST NOT turn explicit absence into a broad negative fact, disclose withheld
material, reauthorize a record, resolve an ambiguity or conflict, or choose a
source winner.

Every collection MUST use deterministic identity-first order: exact admitted
subject identity first; then exact retained metadata or relationship identity
where relevant; then other stable admitted identities; and only then an
admitted stable non-semantic tie key needed to make the order total. Labels,
names, CML, queries, source order, layout, category preference, authority,
freshness, ranking, provider preference, and iteration order MUST NOT choose
an order, interpretation, or winner. If admitted keys cannot establish a total
order, the unresolved ordering condition MUST remain explicit instead of
selecting an iteration order.

## Target-gated navigation and cross-view boundary

Retention of a relation is distinct from navigation. The projection MAY expose
a navigation affordance only when all of the following are retained and usable:

1. the exact target identity and exact Component/context scope;
2. source attribution and a bounded locator for the retained target relation;
3. the distinct shared condition state of affected source, relation, target,
   attribution, and locator; and
4. an implemented receiving contract for that exact target.

When any gate is unmet, the projection MUST suppress navigation without
deleting, rewriting, proxying, substituting, completing, or reconstructing the
retained subject, metadata item, or relationship. It MUST NOT create a dead
link, loose lookup, broad query, proxy target, or name-based target.

A direct exact-identity link to Mono-Koto, Use Case, or a later Structure,
Classification, Event, Workflow, or StateMachine projection MAY be retained
only when its relationship is already admitted in the same Component/context
and the target gate passes. Such a link is navigation and explanation only. It
MUST NOT transfer authority, create Entity/Event/Workflow/StateMachine
semantics, or authorize cross-view reconstruction.

## Read-only authority and prohibitions

The Entity Model MUST remain read-only with respect to CCDM, Cozy material,
CML, canonical sources, Mono-Koto, Use Case, and every other projection. It
MUST NOT create or normalize a structural association, composition,
aggregation, containment, classification, event, workflow, state machine,
lifecycle transition, causal relation, Entity, Value, Aggregate, identity,
ownership, lifecycle, aggregate membership, root status, aggregate boundary,
or source fact. It MUST NOT infer any such material from identity display,
type names, labels, relationship shape, metadata presence, query results,
source order, layout, another projection, or a local copy.

Feedback MAY be represented only as an attributable semantic proposal under
the CCDM contract. It MUST identify an affected exact retained identity,
bounded scope, source locator, proposer, and limitations. It MUST NOT mutate
the Entity Model, CCDM, Cozy material, CML, or a canonical source. Only a
candidate change in the owning source followed by Git-governed acceptance MAY
change canonical semantics.

This contract does not authorize runtime, executable-specification, API,
schema, persistence, CML/source/Cozy retrieval or mutation, Web or technical
delivery behavior, feedback mutation, Phase/checklist/journal update,
validation, review, commit, push, publication, deployment, P9-41 Structure,
or later Phase work. Those behaviors require separately admitted contracts.
