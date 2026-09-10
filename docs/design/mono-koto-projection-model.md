# Mono-Koto Projection Model

status=stable
decision_scope=P9-30A
updated_at=2026-09-10

## Purpose and authority

This design fixes the read-only Mono-Koto communication-projection boundary
for one exact Component and one bounded Canonical Component Design Model
(CCDM) context. It refines the shared projection roles in the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
without changing CCDM authority, identity, source attribution, visibility, or
feedback rules.

Mono-Koto is a stakeholder-facing projection of already admitted CCDM
semantics. It is not an independent analysis model, an upstream truth source,
or an analysis-to-design transformation. Canonical source facts and admitted
evidence remain authoritative; Mono-Koto only organizes and communicates them
at a bounded level of abstraction.

## Projection subject and aggregation roles

The projection begins with the exact Component identity and bounded CCDM
context that admit every displayed subject and relation. A Mono and a Koto are
attributed aggregations of zero or more already admitted CCDM identities:

| Projection role | Admitted aggregate membership | Required interpretation |
| --- | --- | --- |
| Mono | Structural or domain identities such as Aggregate, Entity, Value, and admitted structural relations | A stakeholder-domain subject that can summarize more than one structural identity, or none when the bounded source admits no membership. |
| Koto | Behavioral or temporal identities such as Command, Event, Workflow activity, and state effect | A stakeholder-domain occurrence, action, or progression that can summarize more than one behavioral identity, or none when the bounded source admits no membership. |

The aggregation does not create a new canonical semantic fact, transfer source
authority, or claim a complete analysis model. In particular, a conforming
projection rejects mandatory `Mono = Entity` and `Koto = Event` mappings. A
Mono is not required to be an Entity, and a Koto is not required to be an
Event, even where direct navigation connects the respective roles.

## Identity, scope, and navigation

Every Mono/Koto membership and presented relationship retains direct links to
the admitted semantic element or relationship identities. For each subject,
membership, and link, the projection retains:

- the exact Component identity and bounded CCDM context;
- the stable semantic identity and the admitted aggregation role;
- source attribution and a bounded source locator; and
- the shared condition state affecting the subject, relationship, attribution,
  locator, or target.

Navigation passes the exact retained identity to an already admitted target. A
label, name, CML text, source order, visual layout, diagram geometry, or
view-local copy is explanatory presentation only. None can recover a missing
semantic relationship, identity, attribution, locator, or navigation target.
The projection neither substitutes a loose label lookup nor creates a proxy
target when a target is unavailable or otherwise limited.

## Stakeholder presentation and deterministic order

Mono-Koto presents stakeholder domain vocabulary and simple relationships by
default. Engineering terms and detailed modeling notation are not the default
presentation; they remain available only through already admitted,
identity-based direct drill-down. Hiding that detail does not remove its
identity, attribution, condition, or navigability boundary.

Presentation is deterministic and identity-first. It uses the exact admitted
semantic identity of the displayed subject or relation, followed by remaining
stable admitted identities and an admitted stable non-semantic tie key when
needed to make the order total. It has no source-order fallback, ranking,
authority preference, provider preference, hidden winner, or layout-derived
meaning. If the admitted identities and tie key cannot establish a total order,
the projection preserves the unresolved ordering condition instead of choosing
an iteration order.

## Shared condition semantics

Mono-Koto propagates the CCDM condition state for every affected subject, link,
and navigation affordance. The following remain explicit and distinct:

- unavailable;
- unauthorized;
- redacted;
- explicitly absent;
- ambiguous;
- conflicting;
- stale;
- malformed; and
- limited.

The projection can state these conditions in stakeholder-appropriate language,
but it keeps the attributable detailed condition available. It does not turn
unavailable, unauthorized, redacted, stale, malformed, ambiguous, conflicting,
or limited material into explicit absence; turn an explicit absence into a
broad negative fact; or synthesize a relationship, authority, or target to
make the overview appear complete.

## Feedback boundary

Stakeholder feedback on a Mono, Koto, or simple relationship remains an
attributable semantic proposal. It identifies the affected retained identity,
source locator, bounded scope, proposer, and visible limitations. It does not
directly edit Mono-Koto, CCDM, CML, or any canonical source.

Where a canonical source owns the affected design, the proposal can lead to a
candidate canonical-source change. Only the owning source's candidate process
and Git-governed acceptance can change the canonical fact; after acceptance,
the CCDM and Mono-Koto projection may reflect the resulting canonical source
facts. Until then, the existing fact and the attributable proposal remain
distinct.

## Explicit non-implementation boundary

This design introduces no runtime, API, schema, persistence, Web, CML parsing
or mutation, feedback-editing, or executable-specification behavior. It does
not update Phase or checklist status, select an authority, or implement later
Mono-Koto, Use Case, Entity, Event, Workflow, or StateMachine work.
