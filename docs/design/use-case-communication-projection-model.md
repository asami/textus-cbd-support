# Use Case Communication Projection Model

status=stable
decision_scope=P9-33A
updated_at=2026-09-10

## Purpose and authority

This design fixes one read-only Use Case communication projection for one exact
Component and one bounded Canonical Component Design Model (CCDM) context. It
refines the Use Case role in the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and preserves the exact Mono-Koto relationship boundary in the
[Mono-Koto Semantic Bridge Model](mono-koto-semantic-bridge-model.md). Those
predecessor designs and their contracts retain authority for CCDM admission,
semantic identity, provenance, bounded locators, condition state, and
cross-view navigation.

The projection communicates already admitted interaction semantics. It is not
a requirement source, a canonical design model, a workflow-causality model, a
component-selection surface, or a replacement Mono-Koto, Workflow, Entity,
Event, or StateMachine projection. It neither turns a Use Case into a
canonical fact nor transfers authority from an owning source.

## Exact input and read-only scope

The projection begins only with an exact already established Component identity
and one bounded CCDM context. It may contain only already admitted semantic
element and relationship identities in that same scope. The identity and
relationship ledger, rather than a label, name, CML text, incidental source
encounter order, layout, another view, broad query, or view-local copy,
establishes every record and link.

The exact Component/context pair remains attached to every retained record and
relationship. A display label may explain a retained identity for a
communication audience, but does not establish, recover, redirect, replace, or
broaden that identity. A local summary is never a second semantic model from
which a missing ledger record can be reconstructed.

## Retained communication ledger

Each admitted Use Case retains its exact semantic identity together with the
already admitted communication records and relationships below. A category not
admitted in the bounded source stays explicitly missing; the projection does
not complete it locally.

| Retained subject or relation | Communication role | Required retained state |
| --- | --- | --- |
| Use Case | Bounded communication subject | Exact Use Case identity, exact Component/context, provenance, bounded locator, and shared conditions. |
| Actor | Participant identity actually admitted for the Use Case | Exact actor identity and relationship identity where applicable; provenance, locator, and conditions. |
| Goal | Intent identity actually admitted for the Use Case | Exact goal identity and relationship identity where applicable; provenance, locator, and conditions. |
| Trigger | Admitted initiating identity or relation | Exact trigger identity and relationship identity where applicable; provenance, locator, and conditions. |
| Flow | Attributable source-owned ordered relation sequence | Exact flow and flow-relation identities, explicit admitted sequence key, provenance, locator, and conditions for every retained entry. |
| Postcondition | Admitted resulting condition or relation | Exact postcondition identity and relationship identity where applicable; provenance, locator, and conditions. |
| Domain element | Admitted shared domain identity participating in the communication | Exact identity and relationship identity where applicable; provenance, locator, and conditions. |
| Collaborator | Admitted collaborating identity or relation | Exact identity and relationship identity where applicable; provenance, locator, and conditions. |
| Realizing Workflow | Admitted Workflow identity related to the Use Case | Exact Workflow and relationship identities; provenance, locator, conditions, and target-gated navigation. |
| Mono-Koto relation | Admitted relation to shared Mono or Koto identities | Exact Mono/Koto and relationship identities; provenance, locator, conditions, and target-gated navigation. |

Every row retains source attribution and a bounded source locator for the
asserting source. The locator provides traceability only: it does not become
authority, a retrieval instruction, or permission to reinterpret its source.
The same applies independently to the subject, relation, attribution, locator,
and a navigation target. A missing actor, goal, trigger, flow, postcondition,
domain element, collaborator, Workflow, or Mono-Koto relation is explicit for
the bounded scope rather than a negative assertion about a Component generally.

## Source-owned flow sequence

A flow is an attributable ordered sequence of already admitted relationship
identities. Its order is owned by the source that admitted the flow and is
represented only by an explicit admitted sequence key retained with the flow
relation. That key may communicate sequence position; it does not make the
sequence a Workflow-causality, requirement, authority, or canonical-design
fact.

The projection preserves the exact admitted sequence, including the
provenance, bounded locator, and condition state of every flow relation. It
does not derive or repair sequence from collection iteration, display label,
name, CML text, incidental source order, authority, freshness, layout, provider
preference, plausibility, or another view. If a source has not admitted an
order, or the admitted order is unavailable, ambiguous, conflicting, stale,
malformed, or limited, that condition remains attached to the affected flow;
the projection must not choose a fallback order or imply causality.

All retained collections other than a source-owned flow sequence use
deterministic identity-first presentation: the exact admitted semantic identity
leads, followed by remaining stable admitted identities and only then an
admitted stable non-semantic tie key needed to make the order total. Labels,
names, CML, source order, layout, authority, freshness, ranking, provider
preference, and iteration order cannot select an ordering, interpretation, or
winner. An unresolved total order remains an explicit condition.

## Cross-view communication and navigation

Mono-Koto and realizing-Workflow links are retained only as exact shared
semantic identities and relationships already admitted to the same
Component/context. They communicate and navigate an existing relationship;
they do not make a Use Case into a requirement, create a canonical design fact,
assert Workflow causality, impose Entity/Event semantics, select a Component,
or transfer authority.

Navigation is a separate affordance from relation retention. It is exposed only
when all of the following are retained and usable:

1. the exact target identity and exact Component/context scope;
2. source attribution and a bounded locator for the target relationship;
3. the shared condition state for the affected source, relation, target,
   attribution, and locator; and
4. an implemented receiving contract for that exact target.

When any gate is not met, the projection suppresses the navigation affordance
without deleting, rewriting, proxying, substituting, or reconstructing the
retained relation. It creates no dead link or loose lookup, and it neither
reauthorizes material nor exposes a withheld source locator or attribution.

## Shared conditions and no hidden winner

The projection preserves and distinguishes the CCDM condition state for every
affected record, relationship, attribution, bounded locator, flow-sequence
entry, and navigation target:

- unavailable;
- unauthorized;
- redacted;
- explicitly absent;
- ambiguous;
- conflicting;
- stale;
- malformed; and
- limited.

These states remain attributable. Unavailable, unauthorized, redacted, stale,
malformed, ambiguous, conflicting, or limited material is not converted into
explicit absence. Explicit absence is not widened into a negative design fact.
The projection does not resolve, rank, select, reauthorize, or manufacture
completeness by choosing a more detailed, fresher, more plausible, or more
convenient source. It has no hidden winner.

## Explicit non-implementation boundary

P9-33A creates no Scala runtime value or executable specification; CML/source
processing or retrieval; editable feedback; canonical/source mutation;
Mono-Koto bridge or Workflow implementation; Entity, Event, or StateMachine
model; Web route or renderer; transport/API/schema; persistence; technical
delivery behavior; Phase/checklist status update; validation result; commit;
push; publication; or deployment claim. A later, separately admitted contract
may implement a projection while preserving this exact ledger and authority
boundary.
