# Canonical Component Design Model Projections

status=stable
decision_scope=P9-01C
updated_at=2026-09-10

## Purpose and authority

This design fixes the projection architecture for the [Canonical Component
Design Model Contract](../spec/canonical-component-design-model-contract.md).
It preserves the accepted P9-01A
[Evidence-Backed Component Composition and Dashboard
Design](evidence-backed-component-composition.md) and P9-01B
[Application Component Composition Model](application-component-composition-model.md)
contracts/designs. Those documents retain authority for shared evidence,
composition, source responsibility, human selection, and no-hidden-winner
behavior.

The design describes one semantic model and eight read-only projections. It is
not a runtime design, API design, schema, storage model, CML implementation, or
executable specification. CML remains planning input and a canonical design
source only where its own contract makes the design expressible; this design
does not parse, infer, or mutate CML.

## Model shape and authority lanes

For an exact Component and bounded projection context, the model is assembled
from the owning source facts and explicitly admitted evidence:

```text
exact Component identity + canonical design source facts
                             |
                             +---- source locators and authority
                             |
                 admitted, bounded enrichment evidence
                             |
                             v
             Canonical Component Design Model
                             |
       +---------+-----------+----------+---------+
       |         |           |          |         |
       v         v           v          v         v
  Mono-Koto  Use Case   Entity/Event  Structure  Classification
       |         |           |          |         |
       +---------+-----------+----------+---------+
                             |
                       Workflow / StateMachine
```

The arrows are attributable projection and navigation relationships. They do
not transfer authority. Exact Component identity and canonical source facts
remain authoritative. Enrichment can explain, qualify, or propose a relation,
but it cannot overwrite canonical source data or become a hidden selection
authority.

The model retains one semantic identity ledger rather than maintaining an
analysis model and an engineering model with a one-way conversion between
them. A view may choose a different level of detail, but its records point back
to the shared ledger.

## Identity ledger and source locators

The CCDM ledger has two identity families:

| Ledger record | Purpose | Required provenance and boundary |
| --- | --- | --- |
| Semantic element | Identifies a bounded Component design subject such as a domain subject, use-case concept, event, activity, state, or classification term | Element identity, exact Component scope, semantic kind, asserting source/authority, bounded source locator, evidence/limitation links, and explanatory label |
| Semantic relationship | Identifies an asserted relation, including endpoints, role, and direction where applicable | Relationship identity, exact Component scope, endpoint identities, relation role/direction, asserting source/authority, bounded source locator, evidence/limitation links, and explanatory label |

The ledger deliberately separates the identity of an assertion from its display
label. A source locator may be a document section, CML source span, record key,
or evidence reference; the locator is retained for traceability and does not
authorize a consumer to reinterpret the source.

Identity is stable semantic navigation, not a persistence promise. If the
owning source proposes a replacement assertion, the candidate may introduce a
new identity or an explicitly related successor. A view must never silently
reuse an old identity for a different subject merely because the display name
is unchanged.

All cross-view links use element and relationship identities directly. Names,
titles, namespaces, diagram coordinates, visual proximity, layout, ordering,
inferred role, and view-local copies are explanatory or presentation data only.
They are never reconstruction keys.

## Projection roles

The projections consume the same ledger but answer different questions:

| Projection | Role and navigation boundary |
| --- | --- |
| **Mono-Koto** | Stakeholder-facing aggregate orientation. A Mono can summarize an Aggregate, Entity, Value, or related structural elements; a Koto can summarize Commands, Events, Workflow activities, and state effects. It is not a mandatory Entity/Event one-to-one bridge. |
| **Use Case** | Communication view of actor, goal, trigger, flow, postcondition, collaborators, realizing workflow, and related Mono/Koto identities. It does not create requirements, design facts, or selections. |
| **Entity** | Engineering view of identity-bearing/value-bearing subjects, ownership, and lifecycle. It links to Mono and Structure identities without treating every Mono as an Entity. |
| **Event** | Engineering view of observable occurrences, causes, consequences, and affected subjects. It links to Koto, Workflow, and StateMachine identities without treating every Koto as one Event. |
| **Structure** | Static relations such as composition, aggregation, association, containment, and ownership. It presents asserted relationship identities; visual geometry has no semantic authority. |
| **Classification** | Generalization, specialization, trait, category, and powertype relations. It presents admitted taxonomy and keeps inferred or conflicting taxonomy explicit. |
| **Workflow** | Bounded activity flow with participants, operations, events, domain elements, and published state effects. It organizes admitted flow relations without turning sequence position into causality. |
| **StateMachine** | Lifecycle states, transitions, triggers, guards, and effects for an affected subject. It links transitions to shared event/workflow/entity identities and leaves missing state semantics explicit. |

Mono-Koto is therefore a communication and aggregate-orientation projection,
not an alternative model source. Entity and Event are explicit engineering
projections, while Structure, Classification, Workflow, and StateMachine refine
their respective semantics. One CCDM element or relation can appear in several
views with different labels and surrounding context, provided each appearance
retains the shared identity and source state.

## Shared visibility ledger

Every projection reads the same condition metadata for an element and relation:

```text
canonical/enrichment attribution
          |
          +--> available / unavailable / stale / malformed
          +--> authorized / redacted / denied
          +--> present / explicit absence
          +--> unambiguous / ambiguous / conflicting
          +--> stated limitations
          |
          v
all eight view projections
```

The view may present a concise audience-specific message, but the underlying
condition and attribution remain navigable. In particular:

- unavailable or redacted content is not silently treated as absent;
- absence is not converted into a negative design fact;
- ambiguity and conflict are not resolved by freshness, detail, display order,
  provider preference, or plausibility;
- an enrichment limitation remains visible beside the enrichment; and
- no projection invents a missing relation, state, role, or source winner to
  make a diagram or workflow appear complete.

This shared visibility ledger makes the views semantically consistent without
requiring them to have identical wording or visual layout.

## Navigation and feedback flow

Cross-view navigation and feedback follow one bounded flow:

```text
CCDM element/relationship identity
             |
             v
view-specific projection and human feedback
             |
             v
attributable semantic proposal
             |
             v
candidate change to owning canonical source
             |
             v
Git-governed repository acceptance
             |
             v
new canonical source facts -> rebuilt CCDM projections
```

Feedback identifies the affected semantic identity, source locator/evidence,
proposer, scope, and limitations. It is a proposal, not a view-local fact and
not an immediate model mutation. Only a candidate change to the owning source
followed by repository/Git acceptance can change the canonical fact. Until that
boundary closes, all views continue to show the existing canonical fact plus
the attributable proposal and any affected projection impact.

This design does not invent approval records, durable candidate storage,
continuation cursors, freshness/invalidation, or cross-session rehydration.
Those lifecycle concerns are deferred to Phase 10, which may persist the same
identities and source relationships without changing their meaning.

## Relationship to P9-01A and P9-01B

P9-01A supplies the shared inventory and source-authority boundary: catalog,
canonical design, model/runtime, BoK, semantic provider, Review, and human
decision evidence retain their own scope. Its no-hidden-winner rule applies to
every CCDM projection.

P9-01B supplies composition identities and dispositions. A composition
candidate, proposal, or exact Dashboard link may navigate to a Component and
its CCDM projections, but navigation does not make the candidate a Component
fact or a selected disposition. A human decision remains the only composition
selection authority.

Consequently, this P9-01C design adds the shared semantic-model and projection
boundary without changing either predecessor contract or creating a new source
of authority.

## Explicit implementation boundary

This design implements no runtime, API, schema, persistence, build, generation,
CML parsing/mutation, Dashboard/Web, provider, or executable-specification
behavior. P9-05 owns executable specifications; Phase 10 owns durable model and
continuation behavior. Any implementation must first preserve this design's
identity, source, visibility, and feedback boundaries.
