# Component Dashboard Projection Contract

status=stable
decision_scope=P9-20A
updated_at=2026-09-10

## Scope and status

This specification normatively defines the deterministic, read-only
`ComponentDashboard` projection for one exact admitted Component. It builds on
the [Evidence-Backed Component Composition Contract](evidence-backed-component-composition-contract.md)
for evidence admission, source responsibility, no-hidden-winner behavior, and
human selection, and on the
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
for semantic identity, source locators, visibility state, and model-view
navigation. Those contracts retain their authority.

This contract does not claim an implemented runtime, Dashboard Web entry,
public API, source parser, resolver, persistence layer, or executable
specification. `ComponentDashboard` is a specified projection boundary, not a
new Component fact or a new source of model truth.

## Required input admission

A ComponentDashboard MUST have exactly one Component identity that has already
been established by its owning catalog or source contract. The Dashboard MUST
NOT recover, substitute, or create that identity from a label, name, namespace,
path, CML text, source order, diagram geometry, layout, provider output,
composition candidate, or presentation order.

Its input MUST be a bounded inventory of records admitted for that exact
Component identity or for an existing, exact admitted relation to it. For every
admitted record, the inventory MUST retain:

- stable inventory-record and source-record identities;
- the exact Component subject scope and any exact semantic element or
  relationship identity;
- source identity, source-owned authority scope, and bounded source locator;
- the statement, or the reason and scope that prevents the statement from being
  disclosed or established;
- an admitted Dashboard concern when the record is permitted in a section;
- availability, authorization, redaction, explicit-absence, ambiguity,
  conflict, staleness, malformed-evidence, and limitation state; and
- a stable non-semantic presentation tie key whenever the identity order would
  otherwise not be total.

Admission MUST preserve each source record as an attributed record. It MUST NOT
merge distinct authorities into a synthesized Component profile, use one source
to complete a field owned by another source, or treat a locator as an authority
or navigation identity.

## Section projection

The Dashboard MUST expose four distinct concerns in this fixed presentation
sequence: Content, Usage, Operation, and Quality/Review. The sequence is
non-semantic presentation only. It MUST NOT rank a source, imply completeness,
transfer source authority, select a Component, or determine a composition
disposition.

Content, Usage, Operation, and Quality/Review are concerns, not field sources.
An item MAY be projected into a concern only when an existing admitted contract
declares or authorizes that exact concern for the record. A Dashboard MUST NOT
infer section membership from prose, labels, names, source paths, CML text,
diagrams, source kind, provider output, or layout. A record with no admitted
concern MUST remain visible only through its permitted inventory condition; it
MUST NOT become a locally classified section item.

Every projected item MUST retain its exact Component scope, stable item/source
identity, source authority, bounded locator, declared concern, permitted
statement representation, and all qualifying conditions. A Dashboard MUST
retain source attribution when it summarizes an admitted statement. Where an
attribution or locator is redacted, it MUST expose the bounded
attribution-withheld condition instead of substituting another source or
presenting an unattributed assertion.

The Quality/Review concern MAY link to or present an authorized representation
of an existing canonical Review conclusion, Finding, Assurance, Unknown,
limitation, gate, report, or attestation. It MUST retain the Review attribution
and MUST NOT reinterpret, aggregate, strengthen, weaken, or synthesize a
canonical Review conclusion.

## Condition semantics

For every affected item or target, the Dashboard MUST preserve the following
states as distinct, visible conditions with their permitted source attribution:

| State | Required interpretation |
| --- | --- |
| unavailable | The source or record is not available for the bounded projection; it is not explicit absence. |
| unauthorized | The consumer lacks a sufficient admission or access basis; it is not absence or redaction. |
| redacted | A known record, field, attribution, or locator is withheld under a disclosure constraint; it is not unavailable content. |
| explicitly absent | The source asserts that expected bounded evidence is not present or cannot be established; this is not a negative fact outside the asserted scope. |
| ambiguous | More than one admissible identity, relation, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible facts or relations; the Dashboard retains the affected attributions rather than choosing one. |
| stale | Supplied freshness information prevents a record from being represented as current. |
| malformed | The admitted evidence cannot support its intended statement under its contract; the Dashboard does not repair it from another source. |
| limited | A stated provider, source, compatibility, authority, projection, or disclosure limit prevents a stronger claim. |

A concise audience representation MAY be used only if a direct path to the
detailed state and permitted attribution remains. The Dashboard MUST NOT turn
unavailable, unauthorized, redacted, stale, malformed, ambiguous, conflicting,
or limited evidence into explicit absence; turn explicit absence into a broad
negative conclusion; or hide a condition to make a section appear complete.

## Deterministic ordering

Within each fixed concern, a Dashboard MUST order items identity-first:

1. by exact admitted semantic target identity when present, otherwise by stable
   admitted source-record identity;
2. by remaining stable inventory-record and evidence identities; and
3. only if those identity keys compare equal, by the required stable,
   non-semantic presentation tie key.

The tie key MUST make the order total for the same admitted inventory. It MUST
NOT be derived from source iteration order, labels, names, namespaces, paths,
CML text, diagram geometry, layout, provider score/order, freshness, source
priority, authority, ranking, or selection. If no valid tie key is admitted,
the Dashboard MUST retain the unresolved ordering condition instead of using an
implicit source or provider order.

Ordering MUST NOT select a Component, choose between conflicting records,
elevate a source, or turn a provider suggestion into a canonical ranking.

## Target-gated identity navigation

A Dashboard MAY expose navigation only when the source record retains the exact
target Component, semantic element, or semantic relationship identity; the
target is within the exact Component scope; target attribution and locator are
retained subject to their conditions; and an implemented receiving target
contract exists for that exact identity.

The navigation request MUST pass the exact retained identity directly. A label,
name, namespace, source path, locator, CML text, diagram geometry, layout,
ordering, provider output, or view-local copy MUST NOT act as a target key or
reconstruct a target identity. If the target is not implemented, unavailable,
unauthorized, redacted, ambiguous, conflicting, stale, malformed, or limited,
the Dashboard MUST expose the applicable condition and MUST NOT create a dead
link, proxy target, or inferred destination.

## Authority, composition, and prohibited inferences

Discovery remains responsible for retrieval, ambiguity, and absence. Review
remains responsible for canonical evaluation and conclusions. Catalog,
canonical design, model, runtime, BoK, and provider evidence retain their
respective source authorities. A Dashboard MUST NOT parse or infer CML/model
semantics; synthesize a catalog fact, Review conclusion, Component identity,
Component selection, unavailable content, redacted content, or missing source
locator; or establish a new source authority.

An admitted composition candidate MAY retain an exact Dashboard identity link.
That link MUST remain navigation only: it MUST NOT create a catalog fact, select
the candidate, rank alternatives, or promote a proposal. Only an explicit,
attributed `HumanDecision` MAY expose a selected composition disposition or
promote a proposal. No Dashboard section, item, condition, order, layout,
source preference, freshness, provider suggestion, or navigation affordance can
stand in for that decision.

This contract does not authorize a runtime, Web route, API, resolver,
persistence, source parsing or mutation, executable test, validation result, or
Phase/checklist completion claim. Any later consumer must be separately
admitted and preserve this contract's input, identity, attribution, condition,
ordering, navigation, and human-decision boundaries.
