# Workflow Projection Contract

## Scope and status

status=stable
decision_scope=P9-51A
updated_at=2026-09-11

This specification normatively defines one read-only Workflow projection and
the boundary of its future read-only Web derivative for one exact established
Component identity and one bounded Canonical Component Design Model (CCDM)
context. The [Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for identity, source authority, attribution, bounded
locators, shared conditions, and proposal handling. Event, Use Case, and
Mono-Koto contracts retain authority for their own records and cross-view
relations.

This contract defines a documentation boundary only. It does not claim a Scala
model, runtime, executable specification, Web route or renderer, API, schema,
persistence, CML/source/Cozy retrieval, feedback editor, canonical mutation,
or technical-delivery behavior.

## Exact admission and retained records

A Workflow projection MUST be scoped to exactly one established Component
identity and one bounded CCDM context. It MUST retain only exact records and
relations directly admitted to that same scope. Every retained subject, flow
endpoint, relation, attribution, bounded locator, gap, and navigation target
MUST retain the exact Component/context, semantic identity and role, source
attribution, bounded source locator, and distinct applicable shared condition.

The projection MAY retain only these directly admitted record families:

| Record family | Normative meaning | Prohibited implication |
| --- | --- | --- |
| Workflow identity | Exact bounded Workflow identity. | Requirement, process execution, causal chain, lifecycle engine, or source authority. |
| Activity identity | Exact Workflow activity identity. | Event occurrence, StateMachine transition, inferred operation, or causal fact. |
| Participant identity | Exact participating identity. | Authorization, ownership, execution, or selected collaborator. |
| Domain-element identity | Exact participating domain identity. | Entity ownership, lifecycle, or structural fact. |
| Operation identity | Exact related operation identity. | Command, Event, implementation invocation, or delivery endpoint. |
| Event identity | Exact related Event identity. | Event occurrence, causality, flow, or lifecycle transition. |
| Published-state-effect identity | Exact admitted published effect identity. | StateMachine state or transition, Entity lifecycle, or generated effect. |
| Flow relation | Exact directed source-owned relation with explicit sequence state. | Causality, reverse flow, requirement priority, source-authority transfer, or canonical-design fact. |

A flow relation MUST preserve its own relation identity, exact Workflow
identity, endpoint identities and endpoint roles, direction, explicit
source-owned sequence key, attribution, bounded locator, condition, and an
admitted stable non-semantic tie key where needed. The projection MUST NOT
synthesize, reverse, merge, fill, complete, redirect, or reinterpret a subject,
endpoint, or flow relation.

## Source-owned sequence and deterministic presentation

An explicit source-owned sequence key is the only permissible presentation
order for a retained flow within its exact Workflow identity. That key MUST NOT
be inferred, repaired, or selected from collection iteration, display labels,
names, CML text, source encounter order, authority, freshness, layout,
provider preference, plausibility, another projection, or a Web view.

The projection MUST first present flow records by exact Workflow identity, then
by their explicit source-owned sequence key. Exact flow identity, exact endpoint
identities, and an admitted stable non-semantic tie key MAY make equal source
positions deterministic, but MUST NOT claim an additional source sequence,
causal relation, or selected winner. Every non-flow collection MUST use
identity-first order: exact admitted semantic identity, remaining stable
admitted identities, then an admitted stable non-semantic tie key only when
needed. Labels, names, CML, source order, layout, authority, freshness,
ranking, provider preference, and iteration order MUST NOT select an order,
interpretation, or winner.

## No inference and cross-view boundary

Workflow, Activity, Participant, Domain Element, Operation, Event, Published
State Effect, and flow records MUST be directly admitted. An Event, Command,
Koto, Use Case, Event Model assertion, source position, display label, CML
text, diagram arrangement, broad query, another view, or local copy MUST NOT
establish, recover, repair, redirect, complete, reverse, or reinterpret any of
them.

An Event MUST NOT prove an Activity occurred or a Workflow advanced. An
Operation MUST NOT prove a Command, Event, implementation call, or delivery
action. A Published State Effect MUST NOT create a StateMachine transition,
Entity lifecycle fact, or generated effect. A source-owned flow sequence MUST
NOT create causality, a requirement, a source-authority transfer, or a
canonical-design fact.

The Mono-Koto bridge MAY retain only existing exact-identity navigation in the
same Component/context. It MUST NOT admit a Workflow record, flow, endpoint,
sequence key, causality, or primary target. A cross-view link is navigation and
explanation only; it MUST NOT transfer authority or create semantic facts in
either view.

## Gaps, conditions, and navigation

When an admitted source or bounded Cozy material cannot supply requested
Workflow material, the projection MUST retain an attributable bounded gap. The
gap MUST identify the exact Component/context, affected exact retained identity
when known, requested Workflow role, bounded field or scope, source attribution,
bounded locator or unavailable scope, distinct condition, and limitation reason.
It MUST NOT contain an inferred value, endpoint, activity, flow, proxy,
substitute source, hidden winner, or broad absence assertion.

Every affected subject, endpoint, relation, attribution, bounded locator, gap,
and navigation affordance MUST distinguish and attribute unavailable,
unauthorized, redacted, explicitly absent, ambiguous, conflicting, stale,
malformed, and limited conditions. The projection MUST NOT convert these
conditions into one another, disclose withheld material, reauthorize a record,
broaden explicit absence, or resolve ambiguity or conflict by selecting a
source.

Forward or reverse navigation MAY be exposed only when the exact receiving
endpoint identity and Component/context are retained, the target preserves the
receiving endpoint's exact attribution and condition, every relevant condition
is usable, and an implemented receiving contract exists. When any gate fails,
the projection MUST retain the relation and suppress only that affordance. It
MUST NOT create a dead link, loose lookup, broad query, proxy target,
name-based target, or inferred reverse flow.

## Read-only Web derivative and prohibitions

A future Workflow Web view MUST be a read-only derivative of an already
admitted Workflow projection. It MAY render retained identities, source-owned
flow, attributable gaps, conditions, and only exact target-gated navigation.
It MUST NOT reread or reinterpret CCDM, CML, Cozy, canonical sources, or other
views; create a second Workflow model; infer a missing subject or relation;
turn rendering order into causality; or mutate feedback, CCDM, source material,
or canonical facts.

The Workflow projection and its Web derivative MUST remain read-only with
respect to CCDM, sources, CML, Mono-Koto, Use Case, Event, Entity,
StateMachine, and all other projections. Feedback MAY be represented only as
an attributable semantic proposal under the CCDM contract and MUST NOT mutate
the projection or any canonical source.

This contract does not authorize runtime or executable-specification work, a
Web route or renderer, API/schema/persistence/technical-delivery behavior,
source/CML/Cozy retrieval or mutation, feedback mutation, Phase/checklist
closure, validation, review, commit, push, publication, deployment, P9-52
StateMachine, P9-53 dynamic cross-view navigation, or later Phase work.
