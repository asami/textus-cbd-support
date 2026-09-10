# Component Dashboard Web Entry Contract

status=stable
decision_scope=P9-22A
updated_at=2026-09-10

## Scope and status

This specification normatively defines the authorized, read-only Web delivery
boundary for one existing `ComponentDashboard`. It builds on the [Component
Dashboard Projection Contract](component-dashboard-projection-contract.md) for
Dashboard admission, attribution, conditions, ordering, and target-gated
navigation, and on the [Component Dashboard Content Overview
Contract](component-dashboard-content-overview-contract.md) for bounded Content
presentation. Those contracts retain their authority.

This contract defines no route, form, controller, role, API, runtime,
authorization-policy, identity-resolver, catalog/discovery, source-retrieval,
or persistence implementation. It does not claim that any such implementation
exists.

## Required authorization and projection identity

A Component Dashboard Web entry MUST receive all of the following as upstream
inputs:

1. an authorization decision that affirmatively admits delivery for one exact
   Component scope;
2. the exact Component identity carried directly by that decision; and
3. one already-created `ComponentDashboard` whose Component identity is exactly
   equal to the Component identity carried by the decision.

The authorization decision MUST be an unambiguous, currently sufficient
upstream decision for that exact Component. The Web entry MUST require exact
equality between the decision-carried identity and the Dashboard Component
identity. It MUST NOT broaden, narrow, normalize, map, resolve, or otherwise
interpret either identity to obtain equality.

When the decision is missing, denied, expired, ambiguous, or does not affirm
the carried Component scope, the entry MUST deny Dashboard delivery. When the
carried Component identity is missing or ambiguous, or differs from the
Dashboard Component identity, the entry MUST deny Dashboard delivery. Denial
or non-delivery MUST NOT represent the Component, Dashboard, source record, or
evidence as absent, complete, selected, or otherwise resolved.

The Web entry MUST NOT create a replacement Dashboard, choose a different
Component, retrieve additional input, or turn a mismatch into a filtered,
partial, or nearest Dashboard.

## No inferred authorization or identity

The Web entry MUST NOT infer an authorization decision, its exact Component
scope, or the Component identity from authentication, session state, route or
form visibility, a role string, Dashboard content, source/inventory fields,
discovery results, labels, family names, summaries, names, namespaces, source
locators or paths, CML text, provider output, diagram geometry, layout,
ordering, or a view-local copy.

Those values may be disclosed only as permitted representation data after the
required upstream tuple has been admitted. They MUST NOT become an entry key,
authorization substitute, identity resolver input, Component-selection basis,
or source of a new Dashboard projection.

## Read-only representation preservation

For an admitted entry, every displayed item MUST remain a representation of
the existing Dashboard record. It MUST retain, as applicable:

- the exact Component and bounded subject or semantic-relation scope;
- stable Dashboard inventory and source-record identities;
- declared Dashboard concern and, for Content, the admitted Content Overview
  family;
- source identity, source-owned authority scope, and permitted source locator;
- the existing permitted statement representation or the reason and scope that
  prevents disclosure or establishment; and
- every applicable availability, authorization, redaction, explicit-absence,
  ambiguity, conflict, staleness, malformed-evidence, and limitation condition.

The entry MAY make a concise delivery representation only when a direct path to
the item's permitted attribution, locator, and detailed conditions remains. If
an attribution or locator is withheld, the entry MUST retain and expose the
bounded withholding condition; it MUST NOT disclose, replace, or silently omit
it.

The entry MUST NOT turn denial, unavailable evidence, unauthorized evidence,
redaction, withheld provenance, ambiguity, conflict, staleness, malformed
evidence, or a limitation into explicit absence, completion, a Component
profile, catalog fact, capability, responsibility, rule, interface/event,
knowledge item, Review conclusion, composition disposition, Component
selection, or `HumanDecision`. It MUST NOT repair a record, fill an omitted
Content family, resolve a condition, choose between sources, or create a new
source authority.

## Exact pass-through navigation

The Web entry MAY expose a Dashboard navigation target only when the existing
target retains all of the following without change:

1. its exact semantic target identity;
2. its exact Component scope, equal to the delivered Dashboard Component;
3. target attribution and permitted locator subject to their conditions; and
4. the existing implemented and usable admission for that exact target.

The entry MUST pass the existing target identity through directly. It MUST NOT
construct, reconstruct, substitute, resolve, refresh, or proxy a target from a
route segment, label, family name, summary, source locator or path, CML text,
provider output, geometry, layout, ordering, or local copy.

If an existing target is not implemented or usable, or its identity, Component
scope, attribution, locator, or condition is unavailable, unauthorized,
redacted, ambiguous, conflicting, stale, malformed, limited, or otherwise not
admitted for the target, the entry MUST retain the applicable condition and
MUST NOT create a dead link, proxy link, inferred destination, or surrogate
navigation behavior.

## Prohibited implementation and authority claims

This contract authorizes no route, form, role, controller, API, runtime,
authorization evaluation, resolver, catalog lookup, discovery, source
retrieval, CML parsing or mutation, persistence, selection, `HumanDecision`,
executable specification, validation result, phase/checklist status, closure,
publication, or deployment claim. In particular, route visibility, form
visibility, authentication, or a role/API/controller name cannot be presented
as evidence that the required upstream authorization decision or a delivery
implementation exists.

A later implementation must be separately admitted. It must preserve exact
authorization/projection identity equality, no-inference admission, record
attribution and conditions, and exact target pass-through defined here.
