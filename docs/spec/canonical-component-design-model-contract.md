# Canonical Component Design Model Contract

## Scope and status

status=stable
decision_scope=P9-01C
updated_at=2026-09-10

This specification defines the Canonical Component Design Model (CCDM) as the
one shared semantic model projected by the Mono-Koto, Use Case, Entity, Event,
Structure, Classification, Workflow, and StateMachine views. It is the
normative contract for the P9-01C canonical-model and projection boundary.

This contract preserves the [Evidence-Backed Component Composition
Contract](evidence-backed-component-composition-contract.md) and the
[Application Component Composition Model
Contract](application-component-composition-model-contract.md). Those contracts
remain authoritative for shared evidence, source responsibility, composition
identities, human selection, and no-hidden-winner behavior. This contract adds
the semantic identity and cross-view rules for a known exact Component; it does
not replace either contract.

The CCDM is a read-only semantic boundary. This document specifies behavior and
traceability; it does not claim that a typed Scala model, Dashboard route,
runtime, API, schema, persistence layer, CML parser, generator, or executable
specification has been implemented.

## Canonical model and source authority

For one exact Component identity and one bounded projection context, the CCDM is
the shared semantic model from which all eight views are derived. It consists
of:

1. the exact Component identity established by its owning catalog/source;
2. canonical Component design source facts, including CML facts where the
   design is expressible in CML; and
3. explicitly admitted enrichment evidence that is attributable, bounded, and
   still owned by its source.

The exact Component identity and canonical Component source facts are
authoritative for what the Component is allowed to assert. A catalog owns its
published Component identity and metadata. A canonical design source owns the
design facts within its source contract. Runtime metadata, generated
definitions, BoK knowledge, tests, Review evidence, and provider observations
may enrich a projection only when their source, authority, bounded subject or
scope, locator, availability, authorization, redaction, and limitations are
retained.

Enrichment is evidence about the canonical model, not an unowned merge into
canonical truth. An enrichment record MUST NOT overwrite a canonical source
fact, create a canonical fact absent from that source, become a selection or
ranking authority, or change the authority of the source that supplied it.
No view may elevate enrichment into a canonical fact merely because it is more
detailed, newer, available, plausible, or easier to display. A view may show a
source-attributed interpretation or proposal beside a canonical fact, but it
must label the distinction.

CML is planning input and canonical source only where the design is expressible
under the applicable CML contract. This specification does not infer meaning
from CML text, parse or mutate CML, assign semantics to unsupported syntax, or
use source order as authority. A bounded source locator may be retained so a
future implementation can navigate to an admitted source fact.

## Shared semantic identity

Every CCDM element has a stable semantic element identity scoped to the exact
Component and projection context. Every relationship has a stable semantic
relationship identity scoped to the same context and its endpoint identities.
The identities are semantic keys, not display labels and not a persistence
format.

An element identity MUST remain stable when a view changes its wording, layout,
abstraction level, ordering, or visual representation. A relationship identity
MUST identify the asserted relation and its direction or role, not merely the
two labels that happen to be displayed. When a source revision replaces an
assertion, the candidate source change may produce a new identity or an
explicitly related successor according to that source's contract; a view may
not silently reuse an old identity for a different semantic subject.

Each element and relationship retains, where available:

- its stable semantic identity;
- its exact Component and bounded projection scope;
- its semantic kind or relationship role;
- one or more source locators identifying the asserting source and bounded
  location (for example a document section, source span, record key, or
  evidence reference);
- source and authority attribution;
- links to contributing evidence and limitations; and
- a display label, which is explanatory text only.

Source locators provide traceability to the assertion; they do not grant
authority to a consumer and do not make a display label an identity. A locator
that is unavailable, unauthorized, redacted, or stale remains an explicit
condition on the record rather than a reason to invent a replacement locator.

Views MUST navigate directly through semantic element and relationship
identities. Reconstruction from display names, titles, namespaces, diagram
layout, visual proximity, ordering, inferred role, lexical similarity, or
view-local copies is forbidden. A view-local summary may link to the CCDM
identity it summarizes, but it is not an independent model and cannot be used
to reconstruct a missing canonical identity.

## One model, eight distinct projections

Each view consumes the same CCDM identities, relations, source attribution, and
visibility state. A view MAY select a bounded subset, summarize it, or render it
at a different abstraction level. It MUST NOT create an independent mutable
semantic model. The views have distinct roles:

| View | Distinct projection role | Required boundary |
| --- | --- | --- |
| Mono-Koto | Stakeholder-facing aggregate orientation of domain structure and behavior | A Mono and a Koto summarize shared semantics; neither is a mandatory one-to-one Entity or Event and neither creates canonical facts. |
| Use Case | Communication projection of actors, goals, triggers, flows, postconditions, and realizing semantics | A Use Case organizes interaction intent and links to CCDM identities; it does not become a source of design truth or a selection decision. |
| Entity | Engineering projection of identity-bearing or value-bearing domain subjects and lifecycle ownership | Entity records retain links to canonical elements and relationships; they do not assert that every Mono is an Entity. |
| Event | Engineering projection of observable domain occurrences, causes, consequences, and affected subjects | Event records retain links to canonical event semantics; they do not assert that every Koto is one Event. |
| Structure | Projection of structural relations such as composition, aggregation, association, ownership, and containment | Structure shows admitted relations and their identities; diagram geometry is never semantic evidence. |
| Classification | Projection of generalization, specialization, trait, category, and powertype semantics | Classification preserves asserted type relations and ambiguity; inferred taxonomy is not canonical fact. |
| Workflow | Projection of activities, participants, flow, operations, events, and published effects over a bounded behavior | Workflow sequences admitted relations for comprehension; sequence or position does not create a new causal fact. |
| StateMachine | Projection of lifecycle states, transitions, triggers, guards, and effects for an affected subject | StateMachine links transitions to CCDM identities; missing state semantics remain missing rather than being inferred from workflow layout. |

Mono-Koto is an aggregate orientation and communication projection. A Mono may
summarize an Aggregate, Entity, Value, or related structural elements. A Koto
may summarize Commands, Events, Workflow activities, and state effects. The
CCDM MUST reject any contract that requires `Mono = Entity` or `Koto = Event`
as a mandatory one-to-one mapping. Entity and Event remain distinct engineering
projections even when they are linked to a Mono or Koto.

Distinct roles do not create separate authority lanes. For example, a Koto can
link to an Event, a Workflow activity, and a StateMachine transition, while each
linked record keeps its own semantic identity and source attribution. The
relationship is navigation and explanation, not an authority transfer.

## Shared visibility and no-hidden-winner behavior

The following conditions are semantic state shared by the CCDM and every view:

- **absence:** the expected fact or source record is not present or cannot be
  established for the bounded scope;
- **ambiguity:** more than one interpretation or relation remains admissible
  without an accepted discriminator;
- **conflict:** attributable sources assert incompatible facts or relations;
- **authorization/redaction:** access is denied or content is withheld, with
  the reason and affected scope retained when it may be disclosed;
- **source availability:** the source is unavailable, inaccessible, malformed,
  stale, or otherwise unable to supply the requested record; and
- **limitation:** a source, evidence, compatibility, freshness, projection, or
  authority boundary prevents a stronger claim.

Each condition MUST remain explicit in the shared model and visible in every
view that presents the affected element or relationship. A projection may
summarize the condition for its audience, but it must preserve a direct link to
the detailed state and its attribution. An unavailable or redacted fact is not
silently converted into absence; absence is not silently converted into a
negative fact; ambiguity or conflict is not silently resolved by choosing the
first, newest, most detailed, or most plausible source.

No view may invent a placeholder relationship, inferred role, missing state,
winning source, or canonical fact merely to complete a diagram, workflow, or
stakeholder narrative. If a required semantic relation is unavailable, the
view shows the explicit absence, ambiguity, conflict, authorization/redaction,
source-availability, or limitation state and retains the affected identities
and locators when available.

## Feedback and canonical-source change

Feedback submitted from any projection is an attributable semantic proposal.
Feedback identifies the affected CCDM element or relationship, the proposing
actor or source, the proposal's bounded scope, supporting source locators or
evidence, and known limitations. Display edits, reordered diagrams, and
view-local annotations are not canonical-source changes.

A canonical fact changes only through both of the following boundaries:

1. a candidate change to the owning canonical source, with a traceable source
   locator and candidate model/diff; and
2. repository/Git-governed acceptance of that candidate source change under
   the owning project process.

Until both boundaries occur, the existing canonical fact remains unchanged and
the feedback remains a proposal. A proposal may identify affected projections
and expected semantic differences, but it cannot mutate the CCDM, canonical
source, CML, catalog metadata, or any view. Human review or repository
acceptance does not transfer source authority; it accepts the source change in
the source's own governance boundary.

This Phase 9 contract does not invent persistence, approval history,
continuation cursors, freshness/invalidation, or cross-session rehydration.
Durable candidate state, approval binding, and continuation belong to Phase 10.
The current CCDM and its feedback proposal are transient and inspectable in the
current work context only.

## Contract preservation and explicit non-implementation

The P9-01A contract remains authoritative for evidence admission, source
responsibility, attribution, availability, authorization, redaction, absence,
limitations, and no-hidden-winner behavior. The P9-01B contract remains
authoritative for composition identities, coverage dispositions, proposals,
human decisions, transient projections, and exact Component/Dashboard
navigation. A composition candidate or provider proposal may link to a CCDM
projection only as navigation; it does not become an exact Component fact or a
selected outcome.

This documentation slice does not implement or authorize:

- runtime behavior, public or private APIs, schemas, or typed Scala models;
- CML parsing, mutation, generation, build, publication, or execution;
- Dashboard/Web, provider, catalog, Review, or application behavior;
- persistence, approval/decision history, continuation, or Phase 10 durable
  lifecycle behavior; or
- executable specifications, tests, validation commands, or build changes.

Any future implementation MUST preserve this contract and add executable
specifications at the separately admitted P9-05 boundary before claiming the
semantic behavior is implemented.
