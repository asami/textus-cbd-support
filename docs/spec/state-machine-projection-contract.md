# StateMachine Projection Contract

## Scope and status

status=stable
decision_scope=P9-52A
updated_at=2026-09-11

This specification normatively defines one read-only StateMachine projection
for one exact established Component identity and one bounded Canonical
Component Design Model (CCDM) context. The [Canonical Component Design Model
Contract](canonical-component-design-model-contract.md) retains authority for
CCDM identity, source authority, attribution, bounded locators, shared
conditions, and proposal handling. The [Event Model Projection
Contract](event-model-projection-contract.md) and [Workflow Projection
Contract](workflow-projection-contract.md) retain authority for their
respective record and relation boundaries.

This contract defines a documentation projection boundary only. It does not
claim a Scala model, runtime, executable specification, API, schema,
persistence, event store, lifecycle engine, Web route or renderer,
CML/source/Cozy retrieval, feedback editor, canonical mutation, or technical
delivery behavior.

## Exact admission and relation preservation

A StateMachine projection MUST be scoped to exactly one established Component
identity and one bounded CCDM context. It MUST retain only subjects, transition
endpoints, relations, attributions, locators, gaps, and local affordances
directly admitted to that same scope. Every retained item MUST preserve the
exact Component/context; exact semantic identity and role; source and authority
attribution; bounded source locator; and distinct applicable shared condition.

The projection MAY retain only these directly admitted record families:

| Record family | Normative meaning | Prohibited implication |
| --- | --- | --- |
| StateMachine identity | Exact admitted StateMachine semantic identity. | Lifecycle engine, canonical source, or lifecycle execution. |
| State identity | Exact admitted State identity in the bounded context. | State entry, state exit, current state, Entity lifecycle, or transition. |
| Trigger identity | Exact admitted Trigger identity. | Event occurrence, transition execution, causality, or authorization. |
| Guard identity | Exact admitted Guard identity. | Guard or rule evaluation, permission, selected path, or transition execution. |
| Action identity | Exact admitted Action identity. | Action execution, effect, causality, state entry, or state exit. |
| Activity identity | Exact admitted related Activity identity. | Workflow progress, Event occurrence, transition execution, or causality. |
| Operation identity | Exact admitted related Operation identity. | Command, invocation, delivery action, or transition execution. |
| Event identity | Exact admitted related Event identity. | Event occurrence, causal relation, Workflow flow, state change, or transition. |
| Rule identity | Exact admitted related Rule identity. | Rule evaluation, authorization, constraint satisfaction, or transition. |
| State-to-State transition relation | Exact independently admitted directed relation from one State endpoint to one State endpoint. | Reverse transition, chronology, causality, execution record, lifecycle fact, or source-authority transfer. |
| Transition-to-adjunct relationship | Exact independently admitted relationship between one transition and one Trigger, Guard, Action, Activity, Operation, Event, or Rule endpoint, with its admitted endpoint roles and direction. | State-to-State transition, completion of a transition, reverse relation, authorization, or lifecycle fact. |

StateMachine, State, Trigger, Guard, Action, Activity, Operation, Event, and
Rule identities MUST be directly admitted. A State-to-State transition MUST be
directly admitted as its own exact directed relation. A transition-to-adjunct
relationship MUST be directly admitted as its own exact relation. The two
relation families MUST remain separate. A transition-adjunct relationship MUST
NOT create, complete, interpret, reverse, collapse into, or become
authoritative for a State-to-State transition; a State-to-State transition MUST
NOT create, complete, interpret, reverse, collapse into, or become
authoritative for a transition-adjunct relationship.

Every transition relation MUST preserve its own relation identity,
StateMachine identity, exact source and target State endpoint identities and
roles, direction, attribution, bounded locator, condition, and an admitted
stable non-semantic tie key where required. Every transition-adjunct
relationship MUST preserve its own relation identity, exact transition and
adjunct endpoint identities and roles, admitted direction, attribution, bounded
locator, condition, and an admitted stable non-semantic tie key where required.
The projection MUST NOT synthesize, reverse, merge, fill, complete, redirect,
or reinterpret a subject, endpoint, transition, or transition-adjunct
relationship.

## No inference and descriptive lifecycle boundary

The StateMachine projection is descriptive evidence, not a lifecycle engine.
An Event, Workflow flow or Activity, Operation, Action, Guard, Rule, state
effect, source ordering, label, name, or StateMachine identity MUST NOT prove
transition execution, Event occurrence, causality, authorization, state entry,
state exit, current state, Entity lifecycle, rule evaluation, path selection,
or canonical fact. A transition relation itself MUST NOT prove any of those
matters beyond the exact relation admitted by its source.

Labels, names, namespaces, CML text, query results, source encounter order,
diagram layout, display proximity, Event Model, Workflow, Mono-Koto, Use Case,
another projection, a local copy, provider preference, freshness, or
plausibility MUST NOT establish, recover, repair, redirect, complete, reverse,
or reinterpret a retained subject, endpoint, relation, condition, or local
affordance. The projection MUST NOT use a name-based lookup, loose lookup,
broad query, or view-local copy to establish missing material or select a
source winner.

## Deterministic presentation

Every ordinary subject or adjunct collection MUST use identity-first order:
exact admitted subject identity, then remaining stable admitted identities, and
then an admitted stable non-semantic tie key only when needed to make the order
total. Labels, names, CML, queries, source order, layout, authority, freshness,
ranking, provider preference, and iteration order MUST NOT select an order,
interpretation, or winner.

Transition relations MUST first group by exact StateMachine identity, then
exact source State identity and exact target State identity. Exact transition
relation identity and an admitted stable non-semantic tie key MAY make equal
groups deterministic. Transition-adjunct relationships MUST be ordered by exact
transition identity, exact adjunct endpoint identity, their own relationship
identity, and then an admitted stable non-semantic tie key only when required.
This presentation order MUST NOT establish chronology, causality, priority,
authorization, authority, or a winner. If admitted keys cannot establish a
total order, the unresolved ordering condition MUST remain explicit rather than
allowing iteration order to decide.

## Gaps and distinct conditions

When an admitted source or bounded Cozy material cannot supply requested
StateMachine material, the projection MUST retain an attributable bounded gap.
The gap MUST identify:

1. the exact Component identity and bounded CCDM context;
2. the affected exact retained identity or relation when known;
3. the requested StateMachine role and unsupported source/Cozy field or bounded
   assertion;
4. source and authority attribution;
5. the bounded locator or bounded unavailable scope; and
6. the distinct condition and limitation reason.

A gap MUST NOT contain an inferred StateMachine, State, transition, adjunct
endpoint, relation, proxy, substitute source, hidden winner, default
transition, or broad absence claim. Explicit absence MUST apply only to the
source-declared bounded scope and MUST NOT become a claim that the Component
generally lacks StateMachine material.

Every affected subject, endpoint, transition, transition-adjunct relationship,
attribution, locator, gap, and local affordance MUST distinguish and attribute
the following conditions:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply the requested material; it is not explicit absence. |
| unauthorized | The necessary admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated bounded scope; it is not a broader negative fact. |
| ambiguous | More than one admissible identity, relation, endpoint, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; the affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, Cozy-field, or disclosure boundary prevents a stronger claim. |

The projection MUST NOT convert unavailable, unauthorized, redacted, stale,
malformed, ambiguous, conflicting, or limited material into explicit absence.
It MUST NOT broaden explicit absence, disclose withheld material, reauthorize a
record, resolve ambiguity or conflict, or select a source winner.

## Target-gated local affordances and reservation

Retention is distinct from a local affordance. A StateMachine-local transition
or adjunct-detail affordance MAY be exposed only when all of the following are
retained and usable:

1. the exact local target identity and exact Component/context scope;
2. source attribution and a bounded locator for the retained target relation;
3. the distinct condition state of the source, relation, target, attribution,
   and locator; and
4. an implemented receiving contract for that exact local target.

When any gate fails, the projection MUST retain the affected record or relation
and suppress only that affordance. It MUST NOT create a dead link, loose lookup,
broad query, proxy target, substitute target, name-based target, or inferred
reverse relation. A local forward or reverse index MUST be an index over the
same admitted transition or transition-adjunct relationship and MUST NOT create
an independently inferred reverse assertion.

This contract does not authorize dynamic cross-view navigation. A relationship
to Event, Workflow, Entity, Mono-Koto, Use Case, Structure, Classification, or
another StateMachine target is reserved for the separately admitted P9-53
boundary. A local StateMachine affordance MUST NOT transfer authority or create
a cross-view relation, causal fact, lifecycle fact, or canonical fact.

## Read-only authority and prohibitions

The StateMachine projection MUST remain read-only with respect to CCDM, source
and Cozy material, CML, canonical sources, Event Model, Workflow, Entity Model,
Mono-Koto, Use Case, and every other projection. It MUST NOT create or
normalize a StateMachine, State, Trigger, Guard, Action, Activity, Operation,
Event, Rule, transition, transition-adjunct relationship, lifecycle fact,
causal fact, authorization fact, source fact, or canonical fact.

Feedback MAY be represented only as an attributable semantic proposal under the
CCDM contract. It MUST identify an affected exact retained identity or relation,
bounded scope, source locator, proposer, and limitations. It MUST NOT mutate
the StateMachine projection, CCDM, Cozy material, CML, or a canonical source.
Only a candidate change in the owning source followed by Git-governed
acceptance MAY change canonical semantics.

This contract does not authorize a Scala runtime value or executable
specification (both are reserved for P9-52B); API/schema/persistence/technical-
delivery behavior; source/CML/Cozy retrieval or mutation; lifecycle execution,
event-store behavior, feedback mutation, or canonical mutation; Web delivery;
P9-53 dynamic cross-view navigation; Phase/checklist/journal closure;
validation, review, commit, push, publication, deployment; or later Phase work.
Those behaviors require separately admitted contracts.
