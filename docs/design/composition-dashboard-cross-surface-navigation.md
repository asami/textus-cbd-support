---
status: stable
decision_scope: P9-63A
updated_at: 2026-09-11
---

# Composition-to-Dashboard Cross-Surface Navigation Design

This design fixes P9-63A's transient, read-only cross-surface association
between one caller-admitted composition candidate and one existing Component
Dashboard entry. Its normative companion is the [Composition-to-Dashboard
Cross-Surface Navigation Contract](../spec/composition-dashboard-cross-surface-navigation-contract.md).

## Purpose

Composition answers how admitted evidence accounts for an application's exact
intent and required capabilities. A Component Dashboard explains one exact,
already-admitted Component and its permitted model navigation. This design
allows a reader to retain an explicit association between those two surfaces
without making the association a selection, catalog fact, canonical state,
promotion, or external action.

The association is navigation-only evidence. It neither discovers nor chooses
a Component, and it cannot strengthen a candidate, proposal, alternative,
gap, or unresolved outcome. It does not make a Dashboard entry or a model
target available merely because a candidate names related text.

## Authority map

| Boundary | Retained authority | P9-63A consequence |
| --- | --- | --- |
| `ApplicationComponentCompositionPlan` and `HumanDecision` | Composition scope, coverage accounting, and the sole selection or proposal-promotion authority | A candidate link is not a `HumanDecision`, selected disposition, or promotion input. |
| `ComponentDashboard` | Exact Dashboard Component, concern projection, deterministic Dashboard ordering, and model-target gating | The association may retain an existing entry and targets only under this contract's existing admission and usability rules. |
| Canonical CCDM/source | Canonical Component and model semantic meaning | P9-63A does not interpret, create, or repair a model identity or meaning. |
| Discovery | Retrieval, ambiguity, and absence | P9-63A does not retrieve a Dashboard, resolve ambiguity, or infer a missing association. |
| Operation | Operation semantics | The association does not invoke, interpret, or create an operation. |
| Review | Canonical conclusions and authorized representations | The association neither creates nor changes a conclusion. |
| Runtime | Runtime observations | Lifecycle or runtime evidence cannot supply a navigation target or decision. |
| P9-62 unified evidence integration | Read-only declared relationship and evidence presentation | A lifecycle lane cannot be converted into a candidate-to-Dashboard association or navigation decision. |
| Phase 10 | Durable approval/history, continuation, and rehydration | P9-63A creates no persistence; a successor may retain or rehydrate an already-admitted link only under its own authority. |

## One bounded transient link record

The design admits one prospective `CompositionDashboardNavigationLink` record
only when its values are supplied directly by the caller. The name describes a
bounded record for P9-63B; it does not claim that a runtime type, route, API,
or resolver exists.

Its composition-candidate position retains all of the following:

- its own stable candidate identity;
- the exact `ApplicationIntent` and `RequiredCapability` scope;
- an exact `ComponentDashboardComponentIdentity` supplied directly as the
  candidate Component;
- provenance and attribution, every applicable condition, and limitations; and
- an optional stable non-semantic presentation tie key.

This candidate is explicitly not a `HumanDecision`, selected disposition,
catalog fact, canonical Component, or promoted proposal. A display label,
candidate ID text, source locator, concern, lifecycle evidence, or candidate
shape is explanatory context only and is never a Component or navigation key.

The association separately retains its own stable association identity, the
exact candidate identity, an exact
`ComponentDashboardComponentIdentity` supplied directly as the association
Component, caller-supplied association provenance, a bounded association
locator, distinct association conditions, and association limitations. Those
association values are distinct from candidate and Dashboard-record values;
P9-63A neither infers, reconstructs, repairs, copies, nor substitutes them.
The candidate Component, association Component, and existing
`ComponentDashboard.component` MUST be equal supplied values. Equal-looking
text, names, namespaces, locators, or inferred source relationships do not
establish that equality.

The Dashboard position is the existing exact `ComponentDashboard` Component
identity; it is not a new catalog identity. It may retain only the supplied
Dashboard entry and the entry's already-admitted
`ComponentDashboardProjectedRecord` model-navigation targets. Each retained
target remains in the same exact Component scope and retains its existing
source-record identity, target identity, attribution, condition, and
implemented-and-usable state under the Dashboard contract. When an individual
target is unavailable, redacted, ambiguous, conflicted, stale, malformed,
limited, or otherwise not implemented and usable, P9-63A suppresses that
target only and retains the direct association, its evidence, and the bounded
per-target condition or limitation. P9-63A never creates, guesses, makes
usable, relabels, semantically reorders, or repairs a model target.

## Exact admission and total result

P9-63B must reject the complete candidate-to-Dashboard link only when required
association input is malformed: a required association identity, scope,
attribution, provenance, bounded locator, condition, limitation, or explicit
association is missing, blank, mismatched, duplicate, foreign, unavailable,
or unauthorized. It must not recover a Dashboard Component or target from
candidate Component text, display label, name, namespace, source locator, CML
text, diagram or model layout, order, provider output, condition, lifecycle
evidence, concern, or candidate shape. A normal Dashboard target-gate
disposition is not malformed association input.

The pure association-admission result is all-or-nothing: either one admitted
navigation-only link retaining the exact candidate, association provenance,
bounded locator, distinct association conditions and limitations, and
Dashboard entry identity; or one typed rejection with no partial association.
Within an admitted association, each Dashboard target retains only its own
implemented-and-usable projection link. A gated target is suppressed with its
bounded condition or limitation retained, rather than rejecting the
association. A Dashboard with no usable admitted model target may still be
retained only with an explicit zero-target condition. It cannot produce a
proxy, dead link, or inferred model target.

## Deterministic presentation only

The link's presentation order is identity-first:

1. candidate identity;
2. exact Dashboard Component identity; then
3. for each retained model projection, the existing Dashboard semantic target
   identity and then source-record identity, followed by the caller-admitted
   stable non-semantic tie key only when required for a total order.

This order does not rank candidates, choose a Component, promote a proposal,
resolve ambiguity or conflict, establish freshness or currentness, or become a
`HumanDecision`. It preserves the existing Dashboard target order and MUST NOT
reorder navigation targets source-record-first.

## Exclusions and successor boundaries

P9-63A is documentation only. It creates no runtime, executable
specification, Dashboard route, API, resolver, selection, candidate/CCDM/Review
fact, CML or source access, Phase status, Git action, commit, push,
publication, or deployment claim.

P9-63B is reserved for pure immutable Scala values, a factory, and executable
specifications that preserve this design and its normative companion. It does
not authorize Web/API/route/resolver behavior, CML or source parsing or
mutation, Dashboard generation, candidate or Git governance, lifecycle
execution, external invocation, persistence, Phase 10 continuation, or
Phase/checklist/journal updates. Those boundaries require separately frozen
authority.
