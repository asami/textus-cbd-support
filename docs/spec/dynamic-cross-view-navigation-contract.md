---
status: stable
decision_scope: P9-53A
updated_at: 2026-09-11
---

# Dynamic Cross-View Navigation Contract

This is the normative P9-53A contract for the dynamic cross-view navigation
documentation boundary. Its design rationale is in the [Dynamic Cross-View
Navigation Design](../design/dynamic-cross-view-navigation.md).

## 1. Authority and bounded index

This contract preserves the authority of Phase 9 / P9-53 checklist, CCDM
design and contract, static cross-view navigation design and contract, Event,
Workflow, and StateMachine designs and contracts, and the Entity Model
Projection Contract.

The P9-53 index SHALL be read-only and limited to one exact established
Component and bounded CCDM context. It SHALL NOT establish a new source of
truth, identity authority, projection authority, or feedback path.

## 2. Explicit retention and mapping

The index SHALL retain only explicitly admitted identity records and explicit
mappings among independently retained Koto, Event, Workflow, StateMachine,
and affected Entity projection records. Every retained record and mapping
SHALL retain all of the following independently:

1. exact Component and bounded context;
2. exact shared semantic target;
3. category and independent role;
4. attribution and bounded locator; and
5. distinct condition.

Equality of a displayed shared identity SHALL NOT admit a record or mapping.
Membership SHALL NOT transfer or copy assertions between projection records.

## 3. Eligibility and semantic separation

Eligibility SHALL use only the following dynamic roles:

| Projection | Roles |
| --- | --- |
| Koto | subject, reference |
| Event | subject, assertion, endpoint |
| Workflow | subject, flow, endpoint |
| StateMachine | subject, transition, transition-adjunct, endpoint |
| Entity | subject, metadata |

Eligibility or membership SHALL NOT make or imply Event causality, Workflow
execution, StateMachine lifecycle, Entity ownership or lifecycle, Koto
aggregation, Koto requirement, canonical or source authority, or a winner.
It SHALL NOT use an Event causal, consequence, or affected-subject assertion;
Workflow flow, activity, or state effect; StateMachine transition or adjunct;
or Entity lifecycle metadata as an identity-mapping inference.

## 4. Prohibited admission and reconstruction

The index SHALL NOT admit, recover, or reconstruct a record or mapping from a
label, name, type, namespace, CML, source order, layout, query, visual
geometry, local copy, or another projection. It SHALL NOT derive mappings
from Event causal/consequence/affected-subject assertions, Workflow
flow/activity/state effects, StateMachine transitions/adjuncts, or Entity
lifecycle metadata.

## 5. Conditions

Each record, mapping, gap, and affordance SHALL preserve, without conflation,
every applicable condition among unavailable, unauthorized, redacted,
explicitly absent, ambiguous, conflicting, stale, malformed, and limited.
Explicit absence SHALL remain bounded and distinct from every other condition.

## 6. Receiving gaps

When a retained source identity has no receiving counterpart or its receiver
is unimplemented, the index SHALL retain an attributable bounded gap. The gap
SHALL contain:

1. exact Component/context;
2. affected retained source identity;
3. requested category/role;
4. known counterpart exact semantic identity only if already admitted;
5. attribution and bounded locator/scope; and
6. condition and limitation.

Such a gap SHALL NOT infer an identity, perform broad lookup, create a proxy
or replacement, merge identities, choose a hidden winner, or make a negative
assertion outside the stated bounded scope.

## 7. Target-gated affordances

Forward and reverse navigation SHALL be distinct affordances over one retained
mapping. They SHALL NOT duplicate an assertion, establish one-to-one or
primary-target semantics, or transfer authority.

An affordance SHALL be emitted only when its gate establishes the exact
receiving target and Component/context, source and receiving attribution and
bounded locator, all relevant distinct conditions, and an implemented
receiving contract. When a gate fails, only that affordance SHALL be
suppressed. Retained records, mappings, and gaps SHALL remain available as
retained evidence; the index SHALL NOT emit a dead link, loose lookup,
substitute, inferred reverse mapping, or semantic reconstruction.

## 8. Deterministic identity-first order

The index SHALL order by exact shared semantic target, category/role, and then
remaining record/mapping identities. It MAY use an admitted non-semantic tie
key only when necessary. It SHALL NOT use labels, names, CML, source order,
layout, authority, freshness, ranking, provider preference, or caller
iteration. When no admitted total ordering is available, it SHALL leave that
ordering explicitly unresolved.

## 9. Read-only and non-implementation boundary

The index SHALL NOT perform source I/O or mutation against CCDM, named
projections, CML, Cozy, canonical sources, or feedback. It SHALL NOT
synthesize, normalize, merge, reinterpret, feed back, alter lifecycle,
operate an event store, execute a workflow, or define Web, API, schema,
persistence, or technical-delivery behavior.

P9-53A creates documentation only. P9-53B separately owns runtime and
executable-specification work. This contract excludes Phase/checklist/journal
updates, validation, review, commit, push, publication, deployment, and all
later phase work.
