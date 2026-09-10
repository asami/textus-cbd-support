# Event Model Projection Contract

## Scope and status

status=stable
decision_scope=P9-50A
updated_at=2026-09-10

This specification normatively defines one read-only Event Model projection for
one exact already established Component identity and one bounded Canonical
Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for CCDM identity, source authority, attribution, bounded
locators, shared conditions, and proposal handling. The
[Mono-Koto Semantic Bridge Contract](mono-koto-semantic-bridge-contract.md) and
[Entity Model Projection Contract](entity-model-projection-contract.md) retain
authority for their respective identity and cross-view boundaries.

This contract defines a documentation projection boundary only. It does not
claim a Scala model, runtime, event store, API, executable specification,
schema, persistence, Dashboard/Web view, Cozy/source or CML retrieval,
feedback editor, canonical mutation, or technical-delivery behavior.

## Exact scope and record admission

An Event Model projection MUST be scoped to exactly one established Component
identity and one bounded CCDM context. It MUST retain only exact records
directly admitted to that same scope. Every retained subject, assertion,
endpoint, attribution, locator, gap, and navigation target MUST preserve:

- the exact Component identity and bounded CCDM context;
- its exact semantic identity and role, and exact admitted endpoints for an
  assertion;
- source and authority attribution;
- a bounded source locator; and
- its distinct applicable shared condition state.

The projection MAY retain only these directly admitted record families:

| Record family | Normative meaning | Prohibited implication |
| --- | --- | --- |
| Command identity | An exact CCDM-admitted Command identity. | It is not an Event and does not prove an Event occurrence. |
| Event identity | An exact CCDM-admitted Event identity. | It does not prove occurrence, causality, Workflow flow, StateMachine transition, Entity lifecycle, or a lifecycle-engine fact. |
| Causal assertion | An independently admitted assertion with its own exact identity, direction, and endpoints. | Names, labels, proximity, order, a Command, or an Event cannot create, complete, reverse, or reinterpret it. |
| Consequence assertion | An independently admitted assertion with its own exact identity, direction, and endpoints. | It is not a state transition, Workflow activity, or lifecycle fact without a separate admitted assertion. |
| Affected-domain-element assertion | An independently admitted assertion with its own exact identity and exact affected-domain-element endpoint or endpoints. | It does not create an Entity Model, ownership, membership, lifecycle, or StateMachine fact. |
| Generated-state-effect assertion | An independently admitted assertion with its own exact identity and exact generated-state-effect endpoint or endpoints. | It does not create a StateMachine transition, Workflow flow, Entity lifecycle, or lifecycle-engine fact. |

Command and Event identities MUST be admitted directly by CCDM. Causal,
consequence, affected-domain-element, and generated-state-effect records MUST
be admitted as their own exact assertions; they MUST NOT be derived from
displayed identities or another assertion. The projection MUST preserve each
assertion's own identity, kind, endpoint identities and roles, direction,
attribution, bounded locator, and condition. It MUST NOT synthesize an
assertion, reverse an assertion, infer a missing endpoint, collapse two
assertions, or make a label or view-local copy authoritative.

## Mono-Koto boundary and semantic non-implication

A Command MUST NOT be treated as proof that an Event occurred. An Event MUST
NOT be treated as proof of a StateMachine transition, Workflow flow, Entity
lifecycle, or causal relation. An affected-domain-element or generated-state-
effect assertion MUST NOT create an Entity Model, StateMachine, or
lifecycle-engine fact.

The Mono-Koto bridge MAY provide only an existing exact identity navigation
relation in the same Component/context. A Koto MAY have zero or more admitted
Command, Event, Workflow, or state-effect links. The projection MUST NOT
normalize a Koto to one Event, select a primary Event, or use the bridge to
establish Event Model input, causal meaning, endpoints, reverse links, or
state effects. A bridge link MUST NOT transfer authority or create an Event
Model assertion.

Labels, names, namespaces, CML text, queries, source order, diagram layout,
display proximity, Mono-Koto, Use Case, another projection, a local copy, or
plausibility MUST NOT establish, recover, repair, redirect, complete, or
reinterpret a retained identity, assertion, endpoint, or relation.

## Unsupported source or Cozy material

When an admitted source or bounded Cozy material cannot supply requested Event
Model material, the projection MUST retain an attributable bounded gap. Each
gap MUST retain:

1. the exact Component identity and bounded CCDM context;
2. the affected exact admitted Command, Event, or assertion identity when
   known;
3. the requested projection role and unsupported source/Cozy field or bounded
   assertion;
4. source and authority attribution;
5. the bounded locator or bounded unavailable scope; and
6. the applicable distinct condition and limitation reason.

An unsupported-field gap MUST NOT contain an inferred value, inferred
endpoint, proxy relation, substitute source, hidden winner, or broader
negative assertion. The projection MUST NOT fill a gap from CML text, labels,
names, namespaces, queries, source order, diagram layout, Mono-Koto, Use Case,
another view, a local copy, provider preference, freshness, or plausibility.
Explicit absence MUST apply only to the source-declared bounded scope and MUST
NOT be widened into a claim that the Component generally lacks the material.

## Conditions and deterministic collections

The following conditions MUST remain distinct and attributable on every
affected subject, assertion, endpoint, attribution, locator, gap, and
navigation target:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply the requested material; it is not explicit absence. |
| unauthorized | The necessary admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated bounded scope; it is not a broader negative fact. |
| ambiguous | More than one admissible identity, assertion, endpoint, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; the affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, Cozy-field, or disclosure boundary prevents a stronger claim. |

The projection MUST NOT convert unavailable, unauthorized, redacted, stale,
malformed, ambiguous, conflicting, or limited material into explicit absence.
It MUST NOT broaden explicit absence, disclose withheld material, reauthorize a
record, resolve ambiguity or conflict, or choose a source winner.

Every collection MUST use deterministic identity-first order: exact admitted
subject identity first; then exact retained assertion or metadata identity
where relevant; then other stable admitted identities; and only then an
admitted stable non-semantic tie key needed to make the order total. Labels,
names, CML, queries, source order, layout, category preference, authority,
freshness, ranking, provider preference, and iteration order MUST NOT choose
an order, interpretation, or winner. If admitted keys cannot establish a
total order, the unresolved ordering condition MUST remain explicit rather than
selecting an iteration order.

## Target-gated navigation

Retention is distinct from navigation. The projection MAY expose a navigation
affordance only when all of the following are retained and usable:

1. the exact target identity and exact Component/context scope;
2. source attribution and a bounded locator for the retained target relation;
3. the distinct shared condition state of source, relation, target,
   attribution, and locator; and
4. an implemented receiving contract for that exact target.

When any gate is unmet, the projection MUST suppress navigation without
deleting, rewriting, proxying, substituting, completing, or reconstructing the
retained record or assertion. It MUST NOT create a dead link, loose lookup,
broad query, proxy target, or name-based target. Forward and reverse indexes
MUST be indexes over the same retained identity or assertion relation and MUST
NOT create an independently inferred reverse assertion.

A direct exact-identity link to Mono-Koto, Use Case, Entity, Structure,
Classification, Workflow, or StateMachine MAY be retained only when its
relationship is already admitted in the same Component/context and the target
gate passes. Such a link is navigation and explanation only. It MUST NOT
transfer authority or create Event, Entity, Workflow, StateMachine, lifecycle,
causal, or state-transition semantics.

## Read-only authority and prohibitions

The Event Model MUST remain read-only with respect to CCDM, source and Cozy
material, CML, canonical sources, Mono-Koto, Use Case, Entity Model, and every
other projection. It MUST NOT create or normalize Command, Event, causal,
consequence, affected-domain-element, generated-state-effect, Entity,
Workflow, StateMachine, lifecycle, or source facts. It MUST NOT infer such
material from identity display, type names, labels, relationship shape,
metadata presence, query results, source order, layout, another projection, or
a local copy.

Feedback MAY be represented only as an attributable semantic proposal under
the CCDM contract. It MUST identify an affected exact retained identity,
bounded scope, source locator, proposer, and limitations. It MUST NOT mutate the
Event Model, CCDM, Cozy material, CML, or a canonical source. Only a candidate
change in the owning source followed by Git-governed acceptance MAY change
canonical semantics.

This contract does not authorize a runtime, executable specification, API,
schema, persistence, CML/source/Cozy retrieval or mutation, Web or technical
delivery behavior, feedback mutation, Phase/checklist/journal update,
validation, review, commit, push, publication, deployment, P9-51 Workflow,
P9-52 StateMachine, P9-53 dynamic cross-view navigation, or later Phase work.
Those behaviors require separately admitted contracts.
