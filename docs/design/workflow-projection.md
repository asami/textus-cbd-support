# Workflow Projection

status=stable
decision_scope=P9-51A
updated_at=2026-09-11

## Purpose and authority

This design fixes the read-only Workflow projection and the boundary of its
future read-only Web view for one exact Component identity and one bounded
Canonical Component Design Model (CCDM) context. It refines the Workflow role
in [Canonical Component Design Model Projections](canonical-component-design-model-projections.md).
The Event, Use Case, and Mono-Koto designs retain authority for their own
admission and navigation boundaries. The CCDM contract retains authority for
identity, source attribution, bounded locators, shared conditions, and
proposal handling.

The projection presents already admitted behavioral records and relations. It
is not a source reader, CML interpreter, event store, causal model,
StateMachine, lifecycle engine, canonical design model, or source of domain
truth. A retained subject or flow remains attributable to the source that
admitted it.

## Exact projection ledger

The projection is scoped to exactly one Component identity and one bounded
CCDM context. Its ledger retains only records directly admitted to that pair.
Every retained subject, flow endpoint, relation, attribution, locator, gap, and
navigation target carries that exact Component/context, source and authority
attribution, bounded source locator, and its distinct shared condition state.

| Retained record | Projection meaning | Boundary |
| --- | --- | --- |
| Workflow identity | An admitted bounded behavior identity. | It is not a requirement, process execution, causal chain, or lifecycle engine. |
| Activity identity | An admitted activity within a Workflow relation. | It is not an Event occurrence, StateMachine transition, or inferred operation. |
| Participant identity | An admitted actor, role, or collaborating subject. | It does not authorize, own, or execute the Workflow. |
| Domain-element identity | An admitted participating domain identity. | It does not create Entity ownership, lifecycle, or structure facts. |
| Operation identity | An admitted related operation identity. | It is not a Command, Event, implementation call, or delivery endpoint unless separately admitted. |
| Event identity | An admitted related Event identity. | It does not establish occurrence, causality, transition, or flow by itself. |
| Published-state-effect identity | An admitted published effect identity. | It does not create a StateMachine state, transition, Entity lifecycle, or generated effect. |
| Flow relation | An admitted directed relation with source-owned sequence state. | Sequence presents flow only; it does not create causality or a reverse relation. |

The relation identity, endpoint identities and roles, direction, source-owned
sequence key, attribution, bounded locator, condition, and optional stable
non-semantic tie key remain one admitted relation. The projection never
synthesizes a reverse flow, fills an endpoint, collapses relations, or makes a
display label an identity.

## Admission and semantic non-implication

Workflow, Activity, Participant, Domain Element, Operation, Event, and
Published State Effect identities are retained only by direct CCDM admission.
A flow is retained only when its own exact directed relation is admitted. An
Event does not prove an Activity occurred or a Workflow advanced. An Operation
does not prove a Command was issued or an implementation was invoked. A
Published State Effect does not prove a StateMachine transition, Entity
lifecycle change, or generated Event. A flow position does not establish cause,
consequence, requirement priority, source authority, or canonical design fact.

Names, labels, namespaces, CML text, source encounter order, Event Model
assertions, Mono-Koto, Use Case, diagram layout, visual proximity, Web output,
another projection, a broad query, a local copy, freshness, provider
preference, or plausibility may explain a retained record but cannot admit,
repair, redirect, complete, reverse, or reinterpret it. The projection never
selects a hidden source winner.

The Mono-Koto bridge may provide only existing exact-identity navigation in the
same Component/context. A Koto can link to zero or more admitted Workflow
activities or published effects. The bridge neither admits a Workflow subject
nor establishes a flow, endpoint, sequence, causality, or primary target.

## Source-owned flow order

A retained flow belongs to one exact admitted Workflow identity and preserves
its explicit source-owned sequence key. That key communicates only the order
asserted by the admitting source. It is not reconstructed from collection
iteration, label, name, CML text, source encounter order, layout, authority,
freshness, provider preference, another view, or plausibility.

Workflow presentation first groups by exact Workflow identity. Within each
group it presents the explicit source sequence key; relation identity, exact
endpoint identities, and an admitted stable non-semantic tie key make equal
source positions deterministic without creating a new sequence or winner. All
non-flow collections are identity-first. If an ordering condition is unresolved,
the condition remains visible rather than allowing iteration order to choose.

## Unsupported material and attributable gaps

When an admitted source or bounded Cozy material cannot supply requested
Workflow material, the projection retains an attributable bounded gap. It
identifies the exact Component/context, affected retained identity when known,
requested role, bounded source/Cozy field or scope, source attribution,
bounded locator or unavailable scope, distinct condition, and limitation reason.

A gap contains no inferred identity, endpoint, activity, flow, proxy,
substitute source, hidden winner, or broad absence claim. Explicit absence is
only about the source-declared bounded scope; it never says that the Component
generally has no Workflow material.

## Conditions and target-gated navigation

The Workflow projection retains the shared CCDM conditions separately and
attributably for subjects, flow endpoints, relations, attribution, locator,
gaps, and navigation targets: unavailable, unauthorized, redacted, explicitly
absent, ambiguous, conflicting, stale, malformed, and limited. It does not
convert these states into one another, disclose withheld material, reauthorize
a record, or select a winner.

Retention is separate from navigation. A forward or reverse flow affordance is
available only when the exact receiving endpoint identity and Component/context
are retained; the target preserves that endpoint's exact attribution and
condition; all relevant conditions are usable; and an implemented receiving
contract exists. Gate failure suppresses only the affected affordance. The
relation remains retained, with no dead link, loose lookup, broad query, proxy
target, inferred reverse flow, or name-based target.

An exact link to Mono-Koto, Use Case, Entity, Event, Structure,
Classification, StateMachine, or another Workflow record is navigation and
explanation only. It does not transfer authority or establish a requirement,
causal relation, lifecycle fact, StateMachine transition, or implementation.

## Read-only Web derivative

A later Workflow Web view consumes only an admitted Workflow projection. It
may render retained identities, source-owned flow entries, attributable gaps,
conditions, and exact target-gated navigation affordances. It does not reread
CML, Cozy, or canonical sources; construct a second Workflow model; add a
presentation-only relation; turn rendering order into causality; offer a loose
lookup; or mutate feedback, CCDM, source material, or canonical facts.

## Read-only and non-implementation boundary

The Workflow projection is read-only with respect to CCDM, source and Cozy
material, CML, canonical sources, Mono-Koto, Use Case, Event Model, Entity
Model, StateMachine, and every other projection. Feedback can only be an
attributable semantic proposal under the CCDM contract; it cannot mutate this
projection or an owning source.

P9-51A creates no Scala runtime value, executable specification, Web route or
renderer, API, schema, persistence, CML/source/Cozy retrieval or mutation,
feedback mutation, validation, review, Phase/checklist/journal update, commit,
push, publication, deployment, P9-52 StateMachine, P9-53 dynamic navigation,
or later Phase behavior. Those behaviors require separately admitted work.
