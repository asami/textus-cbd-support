# Component Dashboard Content Overview Contract

status=stable
decision_scope=P9-21A
updated_at=2026-09-10

## Scope and status

This specification normatively defines Content Overview as the deterministic,
read-only Content subprojection of one exact admitted `ComponentDashboard`
Component. It builds on the [Component Dashboard Projection
Contract](component-dashboard-projection-contract.md) for input admission,
attribution, conditions, ordering, target-gated navigation, and human-decision
limits, and on the [Canonical Component Design Model
Contract](canonical-component-design-model-contract.md) for exact semantic
identity and cross-view navigation. Those contracts retain their authority.

Content Overview is not a catalog profile, canonical model, semantic
interpreter, source parser, Review conclusion, decision authority, runtime,
API, Web route, resolver, persistence format, or executable behavior. This
contract does not claim that any such implementation exists.

## Exact scope and admission

A Content Overview MUST have exactly the Component identity already admitted to
its owning ComponentDashboard. It MUST NOT recover, substitute, or create that
identity from labels, names, namespaces, source paths, CML text, source order,
diagram geometry, layout, provider output, composition candidates, or
presentation order.

Every displayed Content Overview statement and every displayed family condition
MUST retain:

- the exact Component identity and bounded subject or semantic-relation scope;
- stable admitted inventory and source-record identities;
- source identity, source-owned authority scope, and an admitted bounded source
  locator;
- the `Content` Dashboard concern, the admitted Content Overview family, and
  the permitted statement representation or non-disclosure/establishment
  reason; and
- all applicable availability, authorization, redaction, explicit-absence,
  ambiguity, conflict, staleness, malformed-evidence, and limitation
  conditions.

The Overview MUST preserve an item as an attributed statement. It MUST NOT merge
authorities into a second Component profile, use a source to complete a field
owned by another source, or treat a locator as an authority or target identity.
An overview statement is admitted only when an existing source/Dashboard
contract authorizes both its source record and its Content family. The Overview
MUST NOT infer admission or family membership from wording, labels, names, CML
text, source paths, source order, diagrams, layout, source kind, provider
suggestions, or presentation context.

## Distinct optional families

The Overview MUST preserve these eight distinct optional families:

1. purpose;
2. responsibility;
3. domain summary;
4. capabilities;
5. representative analysis/model entry points;
6. rules;
7. interfaces/events; and
8. related knowledge.

No family is a mandatory Component field and no family may be completed from
another family. In particular, a purpose does not establish a capability or
responsibility; a summary does not establish domain/model semantics or rules;
an interface/event does not establish a capability or protocol meaning; and
knowledge does not establish a catalog fact or Review conclusion.

A Content Overview MAY represent a family only from its admitted, attributable
statements and conditions. It MUST NOT supply an omitted family from naming,
source order, CML text, source paths, diagram content or geometry, layout,
provider suggestions, Review material, another family, or a local
interpretation.

## Omission and condition preservation

When a family has no admitted statement, the Overview MUST NOT create an empty
family assertion, placeholder content, inferred absence, or negative fact. It
MUST retain the applicable bounded source or inventory condition and, when that
family's omission is presented, expose the permitted condition and attribution.
Only an admitted explicit-absence statement may represent absence, and only
within the source's stated scope.

The Overview MUST preserve unavailable, unauthorized, redacted, ambiguous,
conflicting, stale, malformed, and limited conditions as distinct from explicit
absence. It MUST NOT:

- represent unavailable, unauthorized, redacted, stale, malformed, ambiguous,
  conflicting, or limited evidence as absent;
- turn an explicit absence into a broad negative conclusion;
- resolve ambiguity or conflict by selecting a first, newer, detailed,
  plausible, or preferred source; or
- repair a malformed, unavailable, or withheld statement from another source.

Where an attribution or locator is withheld, the Overview MUST expose the
bounded withholding condition rather than substitute an attribution or locator.

## Bounded concise summaries

An Overview MAY use concise, audience-oriented wording only as a bounded
representation of an admitted statement and its qualifying conditions. The
representation MUST retain a direct path to its permitted detailed attribution,
locator, and condition.

A summary MUST NOT add, strengthen, weaken, select, or imply a capability,
responsibility, rule, interface/event, knowledge item, domain/model semantic,
catalog fact, Review conclusion, Component selection, composition disposition,
or negative fact. It MUST NOT mask a condition to make a family or Overview
appear complete.

## Deterministic non-ranking order

The Overview MUST expose the eight families in this fixed presentation order:
purpose, responsibility, domain summary, capabilities, representative
analysis/model entry points, rules, interfaces/events, and related knowledge.
This order is presentation only. It MUST NOT rank a family, source, statement,
or Component; imply completeness or importance; transfer authority; choose a
conflicting record; or determine a composition disposition.

Within a family, the Overview MUST apply the ComponentDashboard's identity-first
ordering: exact admitted semantic target identity when present, otherwise
stable admitted source-record identity; then remaining stable inventory and
evidence identities; then its required stable non-semantic presentation tie key.
It MUST retain an unresolved ordering condition rather than fall back to source
iteration order, labels, names, namespaces, source paths, CML text, diagram
geometry, layout, provider score/order, freshness, source priority, authority,
ranking, or selection.

## Target-gated analysis/model entry points

An analysis/model entry point MAY be exposed only when an admitted record
retains the exact target Component, semantic element, or semantic relationship
identity; that target remains inside the exact Component scope; target
attribution and locator remain subject to their conditions; and an implemented
receiving target contract exists for that exact identity.

The navigation request MUST pass the exact retained identity directly. A family
label, display label, overview prose, name, namespace, locator, source path,
CML text, diagram geometry, layout, ordering, provider output, or view-local
copy MUST NOT reconstruct a target. If the target is not implemented,
unavailable, unauthorized, redacted, ambiguous, conflicting, stale, malformed,
or limited, the Overview MUST expose the applicable condition and MUST NOT
create a dead link, proxy target, or inferred destination.

## Authority and composition limits

Content Overview MUST NOT retrieve evidence; parse or infer CML/model semantics;
synthesize a catalog fact, source locator, capability, responsibility, rule,
interface/event, knowledge item, Review conclusion, Component identity, or
Component selection; or establish a new source authority.

An admitted composition candidate MAY retain an exact ComponentDashboard or
Content Overview navigation identity only. Neither an Overview family, item,
condition, summary, order, source authority, provider suggestion, nor entry
point may select the candidate, rank alternatives, promote a proposal, or
create a catalog fact. Only an explicit, attributed `HumanDecision` MAY expose
a selected composition disposition or promote a proposal.

This contract authorizes no runtime, Web entry, API, resolver, CML parser or
mutation, persistence, executable specification, validation result, Phase
status, checklist completion, publication, or deployment claim. A later
consumer must be separately admitted and preserve this contract.
