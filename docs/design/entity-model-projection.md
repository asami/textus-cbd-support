# Entity Model Projection

status=stable
decision_scope=P9-40A
updated_at=2026-09-10

## Purpose and authority

This design fixes the read-only Entity Model projection boundary for one exact
Component identity and one bounded Canonical Component Design Model (CCDM)
context. It refines the Entity role in the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and preserves the communication boundaries in the
[Mono-Koto Projection Model](mono-koto-projection-model.md) and the
[Use Case Communication Projection Model](use-case-communication-projection-model.md).
Those predecessor designs and their contracts retain authority for CCDM
admission, semantic identities, attribution, bounded locators, condition
state, navigation, and feedback.

The Entity Model is an engineering projection of already admitted structural
subjects. It makes admitted Entity, Value, Aggregate, identity, ownership,
lifecycle, and aggregate-boundary material inspectable without making a new
source of domain truth. It is not a source-reader, CML interpreter, structure
model, classification model, event model, workflow model, state-machine
model, or lifecycle engine.

## Exact projection ledger

The projection begins with exactly one established Component identity and one
bounded CCDM context. It retains only identities and assertions already
admitted to that exact pair. Every retained record has the following direct
ledger links:

- the exact Component identity and bounded CCDM context;
- an exact admitted semantic element or relationship identity and its admitted
  Entity-Model role;
- the asserting source and authority attribution;
- a bounded source locator; and
- distinct shared condition state for the subject, relation, attribution,
  locator, and any target navigation.

The Entity-Model roles are deliberately narrow:

| Retained role | Meaning in this projection | It does not establish |
| --- | --- | --- |
| Entity | An already admitted identity-bearing domain subject. | A Mono-to-Entity equivalence, structural association, class, or lifecycle transition. |
| Value | An already admitted value-bearing domain subject. | An Entity, an owned member, or a classification. |
| Aggregate | An already admitted aggregate boundary subject. | Aggregate membership, root status, composition, or deletion semantics not separately admitted. |
| Identity metadata | An already admitted identity assertion about a retained Entity, Value, or Aggregate. | A recovered key, label-derived identity, or persistence identifier. |
| Ownership metadata or relationship | An already admitted bounded ownership assertion between exact retained identities. | Composition, aggregation, association, containment, independent existence, reassignment, or deletion semantics. |
| Lifecycle metadata or relationship | An already admitted lifecycle assertion for an exact retained subject. | A state machine, transition, event, workflow, or lifecycle engine. |
| Aggregate-boundary metadata or relationship | An already admitted assertion delimiting an Aggregate or relating an admitted subject to that boundary. | Aggregate membership, root selection, or a Structure-model relation not directly admitted. |

A relationship listed above is a retained source assertion with its own
relationship identity; it is not a view-local relationship created because two
displayed records appear related. A relation may remain retained while its
target navigation is suppressed. The Entity Model does not duplicate, repair,
or broaden an assertion into a general Structure relation.

Labels, CML text, source encounter order, diagram coordinates, layout,
namespaces, queries, local copies, Mono-Koto, Use Case, or another projection
can explain a retained record but cannot establish, recover, redirect, or
expand a record, metadata item, relationship, or endpoint identity.

## Metadata and explicit Cozy gaps

Identity, ownership, lifecycle, and aggregate-boundary material is shown only
when its own assertion has been admitted. Each displayed metadata item or
relationship keeps its own exact identity where one is admitted, source and
authority attribution, bounded locator, subject and endpoint identities where
applicable, and condition state. Presence of an Entity, Value, or Aggregate
does not imply any of its metadata.

When a requested Cozy field cannot be supplied by the admitted bounded Cozy
material, the projection retains an attributable gap rather than completing the
model locally. A gap identifies:

- the exact Component/context and, where known, affected admitted subject or
  relationship identity;
- the requested Entity-Model role and the unsupported field or bounded
  metadata assertion;
- the source/authority attribution and bounded locator or bounded unavailable
  scope that establish the limitation; and
- the applicable explicit condition and limitation reason.

A gap has no invented value, replacement source, proxy relationship, inferred
endpoint, or implied negative fact. In particular, unsupported Cozy fields are
not filled from CML text, labels, names, query results, Mono-Koto, Use Case,
diagram layout, source order, a local copy, or a plausibility judgment. An
explicit absence applies only to its source-declared bounded scope; it is not a
claim that the Component never has the omitted fact.

## Conditions, order, and navigation

The projection carries shared CCDM conditions distinctly for every displayed
subject, metadata item, relation, attribution, locator, gap, and navigation
target: unavailable, unauthorized, redacted, explicitly absent, ambiguous,
conflicting, stale, malformed, and limited. An unavailable, unauthorized,
redacted, stale, malformed, ambiguous, conflicting, or limited record is not
converted into explicit absence. A gap does not select a hidden winner or
disclose withheld material.

Collections use deterministic identity-first order. The exact admitted subject
identity leads, followed by an exact retained relationship or metadata identity
when relevant, then other stable admitted identities, and only then an admitted
stable non-semantic tie key needed to make the order total. Labels, names, CML,
source order, layout, authority, freshness, category preference, ranking,
provider preference, and iteration order do not determine an order,
interpretation, or winner. If the admitted keys cannot make an order total,
the unresolved ordering condition remains visible.

Navigation is distinct from retention. A navigation affordance exists only
when the exact target identity, exact Component/context, source attribution,
bounded locator, usable shared condition state, and an implemented receiving
contract are all retained. If any gate fails, the Entity Model suppresses the
affordance without deleting, rewriting, proxying, substituting, or loosely
looking up the retained record or relationship.

## Cross-view and feedback boundaries

An Entity-Model record may retain a direct existing identity link to Mono-Koto,
Use Case, or a later Structure, Classification, Event, Workflow, or
StateMachine projection only when the shared CCDM ledger has admitted that
exact identity relationship and the target passes the navigation gate. The
link is navigation and explanation, not a cross-view semantic reconstruction
or authority transfer.

The Entity Model never treats every Mono as an Entity. It never creates
structural association, composition, aggregation, containment, classification,
event, workflow, state-machine, causal, or lifecycle-transition semantics from
Entity/Value/Aggregate display. It does not infer identity, ownership,
lifecycle, aggregate membership, root status, or aggregate boundary from a
name, type, relationship shape, or view-local representation.

Feedback remains an attributable semantic proposal under the CCDM contract. It
identifies the affected retained identity, source locator, scope, proposer, and
limitations, but does not mutate the Entity Model, CCDM, Cozy material, CML, or
any canonical source. A source-owned candidate change and Git-governed
acceptance remain required before canonical semantics can change.

## Explicit non-implementation boundary

P9-40A introduces no runtime value, API, schema, persistence, source or Cozy
retrieval/mutation, CML parsing/mutation, Web route/renderer, technical
delivery, feedback editor, executable specification, validation, review,
commit, push, publication, deployment, Phase status, checklist, or journal
update. P9-41 and later Structure, Classification, cross-view, Event,
Workflow, and StateMachine work require separately admitted contracts.
