# StateMachine Projection

status=stable
decision_scope=P9-52A
updated_at=2026-09-11

## Purpose and authority

This design fixes the read-only StateMachine projection boundary for one exact
Component identity and one bounded Canonical Component Design Model (CCDM)
context. It refines the StateMachine role in the [Canonical Component Design
Model Projections](canonical-component-design-model-projections.md). The
[Event Model Projection](event-model-projection.md) and
[Workflow Projection](workflow-projection.md) retain authority for their
respective records and relation boundaries. The [Canonical Component Design
Model Contract](../spec/canonical-component-design-model-contract.md) retains
authority for identity, source authority, attribution, bounded locators, shared
conditions, and proposal handling.

The projection describes already admitted StateMachine evidence. It is not a
source reader, CML interpreter, event store, workflow model, lifecycle engine,
authorization model, causal model, Entity lifecycle model, canonical source,
or source of domain truth. A retained subject or relation remains attributable
to the source that admitted it.

## Exact projection ledger

The projection is scoped to exactly one Component identity and one bounded CCDM
context. Its ledger retains only records directly admitted to that pair. Every
retained subject, transition endpoint, relation, attribution, locator, gap, and
local affordance carries the exact Component/context, exact semantic identity
and role, source and authority attribution, bounded source locator, and its
distinct shared condition state.

The ledger has two separate relation families. A directed State-to-State
transition is one independently admitted assertion family. A relationship from
that transition to a Trigger, Guard, Action, Activity, Operation, Event, or
Rule is another independently admitted transition-adjunct family. Neither
family is a representation, completion, reverse form, or authority source for
the other.

| Retained record | Projection meaning | Boundary |
| --- | --- | --- |
| StateMachine identity | An admitted StateMachine semantic identity. | It is not a lifecycle engine, canonical source, or proof that a lifecycle is executed. |
| State identity | An admitted State identity in the bounded StateMachine context. | It does not prove state entry, state exit, current state, Entity lifecycle, or a transition. |
| Trigger identity | An admitted Trigger identity. | It does not establish Event occurrence, transition execution, causality, or authorization. |
| Guard identity | An admitted Guard identity. | It does not establish rule evaluation, permission, transition execution, or a selected path. |
| Action identity | An admitted Action identity. | It does not establish Action execution, effect, causality, or state entry/exit. |
| Activity identity | An admitted related Activity identity. | It does not establish Workflow progress, Event occurrence, transition execution, or causality. |
| Operation identity | An admitted related Operation identity. | It does not establish a Command, invocation, delivery action, or transition execution. |
| Event identity | An admitted related Event identity. | It does not establish Event occurrence, causality, Workflow flow, state change, or a transition. |
| Rule identity | An admitted related Rule identity. | It does not establish rule evaluation, authorization, constraint satisfaction, or a transition. |
| State-to-State transition relation | An independently admitted directed relation with one exact source State endpoint and one exact target State endpoint. | It is descriptive evidence only; it is not a reverse transition, chronology, causal relation, execution record, or lifecycle fact. |
| Transition-to-adjunct relationship | An independently admitted relation between one exact transition and one exact Trigger, Guard, Action, Activity, Operation, Event, or Rule endpoint, with its admitted role and direction. | It is not a State-to-State transition and cannot create, complete, interpret, reverse, or authorize one. |

Every transition preserves its own relation identity, StateMachine identity,
source and target State endpoint identities and roles, direction, attribution,
bounded locator, condition, and admitted stable non-semantic tie key where
needed. Every transition-adjunct relationship preserves its own relation
identity, transition and adjunct endpoint identities and roles, admitted
direction, attribution, bounded locator, condition, and admitted stable
non-semantic tie key where needed. The projection never synthesizes a reverse
relation, fills an endpoint, collapses relation identities, or turns a display
label into an identity.

## Admission and lifecycle non-implication

StateMachine, State, Trigger, Guard, Action, Activity, Operation, Event, and
Rule identities are retained only through direct CCDM admission. A transition
is retained only when its own exact directed State-to-State relation is
admitted. A transition-adjunct relationship is retained only when its own exact
relation is admitted. A StateMachine, State, Event, Workflow flow or Activity,
Operation, Action, Guard, Rule, state effect, source sequence, or name cannot
create, repair, complete, reverse, redirect, or reinterpret either relation
family.

The projection is descriptive, not operational. An Event does not prove an
Event occurrence, a transition execution, causality, or a state change. A
Workflow flow or Activity does not prove workflow progress or a transition. An
Operation does not prove a Command, invocation, delivery action, authorization,
or state change. A Trigger, Guard, Action, or Rule does not prove a trigger was
received, a guard or rule was evaluated, an Action executed, a path chosen, or
a state entered or exited. A transition relation does not prove chronology,
causality, authorization, current state, Entity lifecycle, lifecycle-engine
state, canonical fact, or any material absent from its own admitted assertion.

Labels, names, namespaces, CML text, query results, source encounter order,
diagram layout, display proximity, Event Model material, Workflow material,
Mono-Koto, Use Case, another projection, a local copy, provider preference,
freshness, or plausibility may explain an admitted record but cannot establish,
recover, repair, redirect, complete, reverse, or reinterpret a subject,
endpoint, transition, transition-adjunct relationship, or condition. The
projection does not choose a hidden source winner.

## Presentation order and attributable gaps

Every collection uses identity-first deterministic presentation. Ordinary
subject and adjunct collections present exact admitted subject identity first,
then other stable admitted identities, and only then an admitted stable
non-semantic tie key needed to make the order total. Labels, names, CML,
queries, source order, layout, authority, freshness, ranking, provider
preference, and iteration order do not choose an order, interpretation, or
winner.

Transition presentation first groups by exact StateMachine identity, then exact
source State identity and exact target State identity. The exact transition
relation identity and an admitted stable non-semantic tie key make equal groups
deterministic. Transition-adjunct relationships remain ordered by their exact
transition identity, exact adjunct endpoint identity, their own relation
identity, and then an admitted stable non-semantic tie key where needed. These
orders do not establish chronology, causality, priority, authorization,
authority, or a winner. If admitted keys cannot establish a total order, the
unresolved ordering condition remains visible rather than allowing iteration
order to decide.

When an admitted source or bounded Cozy material cannot supply requested
StateMachine material, the projection retains an attributable bounded gap. A
gap identifies the exact Component/context; affected retained identity or
relation when known; requested StateMachine role; unsupported source/Cozy field
or bounded assertion; source and authority attribution; bounded locator or
bounded unavailable scope; distinct condition; and limitation reason.

A gap contains no inferred StateMachine, State, transition, adjunct endpoint,
relation, proxy, substitute source, hidden winner, default transition, or broad
absence claim. Explicit absence applies only to the source-declared bounded
scope; it never says that the Component generally has no StateMachine material.

## Conditions and target-gated local affordances

The projection retains the shared CCDM conditions separately and attributably
for every subject, endpoint, transition, transition-adjunct relationship,
attribution, locator, gap, and local affordance: unavailable, unauthorized,
redacted, explicitly absent, ambiguous, conflicting, stale, malformed, and
limited. It does not convert those conditions into one another, disclose
withheld material, reauthorize a record, broaden explicit absence, or resolve
ambiguity or conflict by selecting a source.

Retention is distinct from a local affordance. A StateMachine-local transition
or adjunct-detail affordance is eligible only when all of the following are
retained and usable:

1. the exact local target identity and exact Component/context scope;
2. source attribution and a bounded locator for the retained target relation;
3. the distinct condition state of the source, relation, target, attribution,
   and locator; and
4. an implemented receiving contract for that exact local target.

When any gate fails, the projection suppresses only that affordance. It retains
the affected record or relation and does not create a dead link, loose lookup,
broad query, proxy target, substitute target, inferred reverse relation, or
name-based target. A local forward or reverse presentation index is an index
over the same admitted transition or transition-adjunct relationship; it does
not create an independently inferred reverse assertion.

This slice neither exposes nor authorizes cross-view navigation. In particular,
an Event, Workflow, Entity, Mono-Koto, Use Case, Structure, Classification, or
other StateMachine projection target is reserved for the separately admitted
P9-53 dynamic cross-view navigation boundary. Local StateMachine affordance
eligibility does not transfer authority or establish any cross-view relation.

## Read-only and non-implementation boundary

The StateMachine projection is read-only with respect to CCDM, source and Cozy
material, CML, canonical sources, Event Model, Workflow, Entity Model,
Mono-Koto, Use Case, and every other projection. Feedback can only be an
attributable semantic proposal under the CCDM contract; it cannot mutate this
projection, an owning source, or a retained condition.

P9-52A creates documentation only. It creates no Scala runtime value,
executable specification, API, schema, persistence, CML/source/Cozy retrieval
or mutation, Web route or renderer, lifecycle execution, event store, feedback
mutation, canonical mutation, validation, review, Phase/checklist/journal
update, commit, push, publication, deployment, P9-52B runtime/specification,
P9-53 dynamic cross-view navigation, or later Phase behavior. Those behaviors
require separately admitted work.
