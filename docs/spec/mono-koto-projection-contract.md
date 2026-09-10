# Mono-Koto Projection Contract

## Scope and status

status=stable
decision_scope=P9-30A
updated_at=2026-09-10

This specification normatively defines the read-only Mono-Koto stakeholder
projection for one exact Component and one bounded Canonical Component Design
Model (CCDM) context. It builds on the
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
for canonical authority, semantic identity, attribution, source locators,
shared conditions, navigation, and proposal handling. That contract retains
authority.

This contract specifies a projection boundary only. It does not claim a typed
model, runtime, API, schema, persistence layer, Web view, CML behavior, or
executable specification.

## Required admission and aggregation

A Mono-Koto projection MUST be scoped to exactly one already established
Component identity and one bounded CCDM context. Its subjects, memberships, and
relationships MUST be already admitted CCDM semantic identities or admitted
semantic relationship identities in that exact scope.

For every admitted Mono, Koto, membership, and presented relationship, the
projection MUST retain:

- the exact Component identity and bounded CCDM context;
- stable semantic subject, relationship, and membership identities as
  applicable;
- the admitted Mono or Koto aggregation role;
- source attribution and a bounded source locator; and
- availability, authorization, redaction, explicit-absence, ambiguity,
  conflict, staleness, malformed, and limitation state.

A Mono MUST be an attributed aggregation of zero or more already admitted
structural or domain identities, including Aggregate, Entity, Value, or
structural relation identities where admitted. A Koto MUST be an attributed
aggregation of zero or more already admitted behavioral or temporal identities,
including Command, Event, Workflow activity, or state-effect identities where
admitted. The projection MUST NOT require `Mono = Entity` or `Koto = Event` as
mandatory one-to-one mappings, and it MUST NOT turn either aggregation into an
independent source of semantic truth.

## Stakeholder abstraction and presentation

The default presentation MUST use stakeholder-domain vocabulary and simple
relationships. Engineering terminology and detail MUST NOT be exposed by
default, but an already admitted exact identity MAY offer direct drill-down to
an implemented receiving contract. That drill-down MUST pass the exact retained
identity and MUST NOT fall back to label, name, CML text, source order, visual
layout, diagram geometry, or a view-local copy.

The presentation MUST be deterministic and identity-first. It MUST order
subjects and relations by their exact admitted semantic identities, then by
remaining stable admitted identities, and only then by an admitted stable
non-semantic tie key needed to make the order total. Source order, labels,
names, CML, visual layout, authority, freshness, ranking, provider preference,
and iteration order MUST NOT choose an order, interpretation, authority, or
winner. If the admitted keys cannot make the order total, the projection MUST
retain the unresolved ordering condition rather than select a hidden winner.

## Conditions and stable navigation

Every affected Mono, Koto, membership, relationship, attribution, locator, and
navigation target MUST preserve its shared condition state. The following
conditions MUST remain distinct and visible with their permitted attribution:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The source or bounded record cannot supply the subject or target; it is not explicit absence. |
| unauthorized | The necessary admission or access basis is lacking; it is not absence or redaction. |
| redacted | A known subject, field, attribution, or locator is withheld; it is not unavailable content. |
| explicitly absent | The source cannot establish the expected fact for its stated bounded scope; it is not a broader negative fact. |
| ambiguous | More than one admissible identity, relation, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; the projection retains the affected attributions. |
| stale | The supplied freshness state prevents current representation. |
| malformed | The admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, or disclosure boundary prevents a stronger claim. |

Navigation MAY be exposed only when the exact target identity, exact Component
scope, source attribution, and bounded locator are retained subject to their
conditions and an implemented receiving target contract exists. A projection
MUST NOT reconstruct a missing target or relationship from labels, names, CML,
source order, visual layout, diagram geometry, or view-local copies. When the
target or its retained state is unavailable, unauthorized, redacted, explicitly
absent, ambiguous, conflicting, stale, malformed, or limited, the projection
MUST expose that state and MUST NOT create a dead link, proxy target, or loose
lookup.

## Source authority and feedback proposal

Mono-Koto is read-only with respect to CCDM and all canonical sources. It MUST
NOT transfer authority from a source, treat a source locator as authority, or
make a display label, aggregation, or presentation relation into a canonical
fact.

Stakeholder feedback MAY be represented only as an attributable semantic
proposal linked to the affected exact identity, bounded scope, source locator,
proposer, and limitations. It MUST NOT be an editable projection fact or a
direct mutation of CML, CCDM, or a canonical source. A proposal MAY lead to a
candidate change in the owning canonical source. Only that candidate process
and Git-governed acceptance MAY change canonical semantics; before acceptance,
the canonical fact and proposal MUST remain distinct.

## Prohibitions and non-implementation

This contract prohibits CML parsing, inference, or mutation; a separate model
authority; label, source-order, name, or layout reconstruction; source-authority
transfer; ranking or hidden-winner selection; runtime, API, schema, Web, or
persistence behavior; editable feedback; executable-specification claims; and
Phase or checklist status edits. It does not authorize later P9-30 runtime work
or P9-31 through P9-33 work.
