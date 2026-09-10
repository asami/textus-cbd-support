---
status: stable
decision_scope: P9-63A
updated_at: 2026-09-11
---

# Composition-to-Dashboard Cross-Surface Navigation Contract

This normative P9-63A contract specifies one transient, read-only,
navigation-only association between a caller-admitted composition candidate and
an existing exact `ComponentDashboard` entry. Its rationale is in the
[Composition-to-Dashboard Cross-Surface Navigation Design](../design/composition-dashboard-cross-surface-navigation.md).

This is documentation only. It creates no runtime, executable specification,
Dashboard route, API, persistence, selection, candidate/CCDM/Review fact, CML
or source access, Phase status, commit, push, publication, or deployment claim.

## 1. Scope and authority

`ApplicationComponentCompositionPlan` and `HumanDecision` retain composition
scope and the sole authority to select an existing Component or promote a
proposal. `ComponentDashboard` retains the exact Dashboard Component,
Dashboard concern/order, and existing model-target gating. Canonical
CCDM/source retains semantic meaning. Discovery retains retrieval, ambiguity,
and absence; Operation retains operation semantics; Review retains conclusions;
and runtime retains runtime observations. P9-62 lifecycle-unified evidence
remains a read-only presentation and SHALL NOT become a navigation decision.

The association SHALL NOT transfer, merge, replace, or manufacture any of
those authorities. In particular, it SHALL NOT make a candidate a selection,
catalog fact, canonical Component, promoted proposal, or `HumanDecision`.

## 2. Caller-admitted candidate and association inputs

The P9-63B input boundary SHALL accept a composition candidate only when the
caller directly supplies all of the following:

- the candidate's own stable identity;
- the exact `ApplicationIntent` and `RequiredCapability` scope;
- an exact `ComponentDashboardComponentIdentity` as the candidate Component;
- provenance and attribution;
- every applicable availability, authorization, redaction, explicit-absence,
  ambiguity, conflict, staleness, malformed-evidence, and limitation condition;
  and
- an optional stable non-semantic presentation tie key.

The candidate SHALL NOT be a `HumanDecision`, selected disposition, catalog
fact, canonical Component, or promoted proposal. Its candidate ID, Component
text, display label, name, namespace, source locator, condition, concern,
lifecycle evidence, and candidate shape SHALL NOT act as an identity-recovery
or target-resolution mechanism.

The separately supplied candidate-to-Dashboard association SHALL retain its
own stable association identity, the exact candidate identity, an exact
`ComponentDashboardComponentIdentity` as its association Component,
caller-supplied association provenance, a bounded association locator,
distinct association conditions, and association limitations. Association
values SHALL remain distinct from candidate and Dashboard-record values and
SHALL NOT be inferred, reconstructed, repaired, copied, or substituted. The
candidate Component, association Component, and the supplied existing
`ComponentDashboard.component` SHALL be equal values supplied directly by the
caller. This equality SHALL NOT be inferred from text, labels, names,
namespaces, locators, CML/source text, diagram/model layout, ordering,
provider output, lifecycle evidence, concerns, or candidate shape.

## 3. Existing Dashboard entry and retained model navigation

The Dashboard entry SHALL be the existing exact `ComponentDashboard` Component
identity, not a new catalog identity. The association MAY retain the supplied
entry and its already-admitted `ComponentDashboardProjectedRecord`
model-navigation targets only when every retained target:

1. remains within the same exact Component scope;
2. retains its existing source-record identity and target identity;
3. retains its existing attribution and all qualifying conditions; and
4. is implemented and usable under the Component Dashboard projection
   contract.

P9-63B SHALL NOT create, guess, make usable, relabel, semantically reorder, or
repair a model target. When an individual target is unavailable, redacted,
ambiguous, conflicted, stale, malformed, limited, or otherwise not implemented
and usable, P9-63B SHALL suppress that target only and retain the direct
association, its evidence, and the bounded per-target condition or limitation.
A supplied Dashboard with zero usable admitted model targets MAY be retained
only with an explicit zero-target condition. It SHALL NOT create a proxy, dead
link, or inferred target.

## 4. Exact admission, rejection, and totality

The factory SHALL reject the complete association only when required
association input is malformed: a required association identity, scope,
attribution, provenance, bounded locator, condition, limitation, or explicit
association is missing, blank, mismatched, duplicate, foreign, unavailable,
or unauthorized. It SHALL NOT recover a target from candidate Component text,
display label, name, namespace, source locator, CML text, diagram/model
layout, order, provider output, condition, lifecycle evidence, concern, or
candidate shape. A normal Dashboard target-gate disposition SHALL NOT be
treated as malformed association input.

The association-admission result SHALL be all-or-nothing: either one admitted
navigation-only link with the exact candidate, association provenance, bounded
locator, distinct association conditions and limitations, and Dashboard entry
identity; or one typed rejection with no partial association. Within an
admitted association, each Dashboard target SHALL retain only its own
implemented-and-usable projection link. A gated target SHALL be suppressed
with its bounded condition or limitation retained, rather than rejecting the
association. A rejection SHALL NOT discover another Component, resolve
ambiguity or conflict, make an unavailable target usable, or construct a
partial association.

## 5. Deterministic presentation

Presentation SHALL be identity-first in this order:

1. candidate identity;
2. exact Dashboard Component identity; then
3. each retained model-projection's existing Dashboard semantic target identity
   and then source-record identity, with the caller-admitted stable
   non-semantic tie key only where available and required for a total order.

This order SHALL NOT rank candidates, choose a Component, promote a proposal,
resolve ambiguity or conflict, establish freshness or currentness, or become a
`HumanDecision`. It SHALL preserve existing Dashboard target order and SHALL
NOT reorder navigation targets source-record-first.

## 6. Prohibitions and successor work

This contract SHALL NOT authorize Web/API/route/resolver behavior, CML/source
parsing or mutation, Dashboard generation, candidate or Git governance,
lifecycle execution, external invocation, persistence, selection, canonical
mutation, Phase 10 continuation, Phase/checklist/journal updates, validation,
review, commit, push, publication, or deployment.

P9-63B may separately implement only pure immutable Scala values, a factory,
and executable specifications that preserve this contract. Phase 10 may retain
or rehydrate an already-admitted association only under its separately admitted
durability, approval/history, continuation, and rehydration authority. No such
successor behavior is implied here.
