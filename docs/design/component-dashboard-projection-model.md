# Component Dashboard Projection Model

status=stable
decision_scope=P9-20A
updated_at=2026-09-10

## Purpose and decision

This design fixes the deterministic, read-only projection boundary for a
`ComponentDashboard`. A Dashboard begins with one exact, already admitted
Component identity and a bounded inventory of evidence admitted for that
Component. It organizes that inventory as Content, Usage, Operation, and
Quality/Review while retaining each statement's source authority, locator, and
condition. It does not become a catalog, model, Review, Discovery, provider,
or composition-decision authority.

`ComponentDashboard` names a projection concept in this design. It is not a
typed model, runtime service, Web entry, API, resolver, persistence format, or
executable behavior.

This design preserves the shared source and no-hidden-winner boundaries of the
[Evidence-Backed Component Composition Design](evidence-backed-component-composition.md)
and the semantic identity and view boundaries of the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md).
It adds no authority to either predecessor.

## Entry and admitted inventory

The Dashboard entry is an exact Component identity established by its owning
catalog or source contract. An input label, name, namespace, source path, CML
text, diagram geometry, provider result, composition candidate, or layout does
not establish, recover, or substitute for that identity.

The input inventory is bounded before projection. Each admitted record retains:

- its stable inventory and source-record identities;
- the exact Component subject identity or an already admitted, exact relation
  to that Component;
- source identity, source-owned authority scope, and bounded source locator;
- the source statement or the condition that prevents its disclosure;
- a declared, admitted Dashboard concern when one is available;
- availability, authorization, redaction, explicit-absence, ambiguity,
  conflict, staleness, malformed-evidence, and limitation metadata; and
- exact semantic element and relationship identities when an existing model
  contract admits them for navigation.

The inventory is a collection of attributed records, not a merged component
profile. Association permitted by an existing source contract preserves the
contributing records; it does not fill a source-owned field from another
authority or create a new canonical fact.

Content, Usage, Operation, and Quality/Review are Dashboard concerns, not
field sources. A record may appear under a concern only when its admitted
contract declares that concern or otherwise authorizes that bounded projection.
The Dashboard must not classify a record from its wording, name, source path,
CML, diagram, provider suggestion, or presentation context. A record without
an admitted concern remains an inventory condition rather than becoming a
locally inferred section item.

## Deterministic assembly

For the one exact Component, the Dashboard assembles the following fixed
presentation structure:

```text
ComponentDashboard(exact Component identity)
|
+-- Content
+-- Usage
+-- Operation
+-- Quality / Review
`-- shared attribution, conditions, and identity navigation
```

The fixed section sequence is a presentation convention only. It neither ranks
sources nor transfers authority, selects a Component, or implies that one
concern is more complete, current, or important than another.

For every admitted record that is permitted in a concern, the projection retains
a separate Dashboard item. The item carries its exact Component scope, stable
source/inventory identity, source authority, source locator, declared concern,
permitted statement representation, and all qualifying conditions. A Quality /
Review item links to the existing canonical Review conclusion, Finding,
Assurance, Unknown, limitation, gate, report, or attestation as admitted. The
Dashboard may summarize its disclosed representation but may not reinterpret it
as a new Review conclusion.

The stable order is identity-first:

1. use the fixed non-semantic concern sequence above;
2. order items by their exact admitted semantic target identity when present,
   otherwise by their stable admitted source-record identity;
3. refine that order by the remaining stable inventory and evidence identities;
   and
4. when those identity keys compare equal, use a required stable,
   non-semantic presentation tie key supplied at admission.

The tie key exists only to make an otherwise equal display order total. It must
be stable for the same admitted inventory and must not be derived from source
order, labels, names, paths, CML text, diagram layout, provider score or order,
freshness, source priority, selection, or authority. It confers no semantic
meaning, source preference, ranking, or disposition. Admission must reject an
ambiguous tie rather than silently falling back to input iteration order.

## Shared condition propagation

Condition state is cross-cutting rather than a fifth source concern. It is
propagated to every Dashboard item and navigation affordance affected by its
record or target. The following states remain distinct:

| Condition | Dashboard meaning |
| --- | --- |
| Availability | An available record, an unavailable source/record, and an inaccessible record are not interchangeable. |
| Authorization | The admission or access basis is sufficient, denied, expired, or otherwise insufficient; lack of authorization is not absence. |
| Redaction | A known record or field is withheld under a disclosure constraint; it is not an unavailable or absent fact. |
| Explicit absence | The admitted source states that bounded expected evidence is not present or cannot be established; it is not a negative fact outside that scope. |
| Ambiguity | More than one admissible identity, interpretation, or relation remains without a source-owned discriminator. |
| Conflict | Attributed records assert incompatible facts or relations; all affected attributions remain visible. |
| Staleness | A record's supplied freshness condition prevents it from being displayed as current. |
| Malformed evidence | A received record cannot support its intended statement under its admitted contract; it is not repaired from another source. |
| Limitation | A source, provider, compatibility, projection, or disclosure boundary prevents a stronger claim. |

The Dashboard may make an audience-appropriate concise representation, but it
must preserve a direct path to the detailed condition and permitted
attribution. If an attribution or locator is itself withheld, the item records
that bounded withholding explicitly rather than representing it as an unknown
or silently substituting a different locator.

## Identity-only model-view navigation

A Dashboard item can offer model-view navigation only when all of the following
are already admitted:

1. the exact target Component, semantic element, or semantic relationship
   identity is retained in the source record;
2. the target remains in the Dashboard's exact Component scope;
3. target attribution and locator are retained subject to their own disclosure
   conditions; and
4. the receiving view has an implemented target contract for that exact
   identity.

The navigation affordance passes the exact identity directly. Display labels,
names, paths, CML text, diagram geometry, layout, ordering, source locators,
provider output, and view-local copies are explanatory data only: none is a
target key and none can reconstruct a missing target. When the target contract
is not implemented, unavailable, unauthorized, redacted, ambiguous,
conflicting, stale, malformed, or otherwise limited, the Dashboard shows the
applicable state and does not create a dead link, proxy link, or inferred
destination.

## Composition, Discovery, and decision boundary

Discovery remains the authority for retrieval, ambiguity, and absence. A
Dashboard consumes already admitted results; it does not retrieve alternatives,
resolve an identity, or make a catalog fact. A semantic or AI provider remains
the authority only for its attributable suggestion, grouping, or proposal.

Composition may retain an exact Dashboard link for an admitted candidate. That
link is an identity/navigation relation and neither selects the candidate nor
promotes it to a Component fact. Only an explicit, attributed `HumanDecision`
can expose a selected composition disposition or promote a proposal. The
Dashboard never derives such a disposition from evidence availability,
ordering, source authority, labels, provider output, or its own layout.

## Explicit non-implementation boundary

This design does not parse or infer CML/model semantics; synthesize catalog
facts, Review conclusions, Component selections, unavailable or redacted
content; or create a Dashboard runtime, Web route, API, resolver, persistence
layer, source mutation, or executable specification. Those concerns require
separate admitted contracts and must preserve this projection boundary.
