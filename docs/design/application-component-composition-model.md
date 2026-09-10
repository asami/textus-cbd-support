# Application Component Composition Model Design

status=stable
decision_scope=P9-01B
updated_at=2026-09-10

## Purpose and authority

This design fixes the application-component composition identities and the
promotion boundary for Phase 9. It is the design companion to the
[Application Component Composition Model Contract](../spec/application-component-composition-model-contract.md)
and preserves the accepted P9-01A
[Evidence-Backed Component Composition and Dashboard Design](evidence-backed-component-composition.md).

The V1 note
([Evidence-Backed Application Component Composition V1](../notes/usecase-driven-component-composition-v1.md))
is used only as the promoted planning source. Stable authority remains with
the P9-01A shared contract/design and this specification/design pair.

This is a semantic design decision, not an implementation of runtime models,
APIs, CML handling, Dashboard behavior, or persistence.

## Composition shape

The model has one bounded flow with separate authority lanes:

```text
ApplicationIntent
       |
       v
RequiredCapability <---- ComponentEvidence <---- admitted sources
       |
       v
CoverageDisposition <---- HumanDecision (only selection authority)
       |
       +--> selected | alternative | gap | proposal | unresolved
       |
       +--> exact Component/Dashboard identity link (navigation only)

semantic provider / other admitted actor ---> ComponentProposal
```

The arrows show attributable relationships and projection inputs. They do not
transfer authority. In particular, evidence, provider output, a proposal,
Discovery, Dashboard, CML, and presentation order cannot stand in for a human
decision.

## Identity ledger

| Identity | Bounded purpose | Stable identity and provenance | Evidence/decision relationship | Non-authority limit |
| --- | --- | --- | --- | --- |
| `ApplicationIntent` | Scope what the application or application slice intends to accomplish | Intent-scoped identity; submitting actor/source, request context, and bounded locator | Parent of required capabilities; provider interpretations remain separate evidence | Cannot create Component facts, interpret unpromoted CML, select, or authorize execution |
| `RequiredCapability` | Name one reviewable responsibility to account for | Parent-intent-scoped identity; source, source span/locator, and decomposition provenance | Unit receiving one coverage disposition and its evidence | Cannot select by name/order or turn provider interpretation into canonical semantics |
| `ComponentEvidence` | Preserve an attributable observation relevant to coverage | Observation/assertion identity; source, authority, subject, locator, availability, authorization, redaction, absence, limitations | Supports dispositions, alternatives, gaps, proposals, and unresolved states | Cannot rank, select, create another source's fact, or become a decision |
| `CoverageDisposition` | Project the current evidence/decision state for one capability | Capability- and projection-scoped identity; contributing evidence/decision IDs and projection context | Carries exactly one reviewable outcome and retains its causes | Deterministic projection is not selection and cannot mutate source or application state |
| `Alternative` | Keep competing coverage paths reviewable | Capability- and composition-scoped identity; candidate/provider evidence and limitations | May be target of a later human decision | Is not a winner, rank, catalog fact, or dependency |
| `Gap` | Make unsupported responsibility and explicit absence visible | Capability/projection-scoped identity; searched evidence, absence, and limitation provenance | Explains why coverage is not established | Does not prove impossibility or authorize a guess |
| `ComponentProposal` | Preserve an attributable provider suggestion for an existing, independently evidenced Component target | Proposal identity/scope; provider/actor, request context, exact target, considered linked evidence, and limitations | May be admitted as `proposal` or selected only through a human decision | Is not an existing catalog Component fact, ranking, selection, or CML mutation; a not-yet-established Component or responsibility is outside this transient Phase 9 input boundary |
| `HumanDecision` | Admit an explicit selected Component or proposal for bounded scope | Decision identity; actor, scope, rationale, target, considered evidence, and provenance | Only input that can make a disposition `selected` | Does not rewrite source authority, fabricate catalog truth, or authorize generation/build/publication/execution |

The ledger deliberately separates the identity of an evidence observation from
the identity of the Component named by that observation. It also separates a
proposal from an existing catalog Component fact. A future durable model must
retain these distinctions without being implied by this Phase 9 design.

## Reviewable outcomes and promotion boundary

Every admitted `RequiredCapability` receives one `CoverageDisposition` in the
current read-only projection. The disposition may be:

- `selected`, only after a `HumanDecision` in the exact decision scope names
  an existing Component or promotes a proposal;
- `alternative`, while competing evidence or candidates remain visible;
- `gap`, when no linked evidence is admitted or every linked evidence record
  declares an explicit absence;
- `proposal`, when an attributable proposal is available but unapproved; or
- `unresolved`, when conflict, ambiguity, redaction, malformed
  or stale evidence, decomposition uncertainty, or another limitation blocks a
  stronger statement.

The terms are reviewable outcome labels, not commands. A `selected` label
without its linked human decision is invalid. An `alternative`, `gap`,
`proposal`, or `unresolved` label never implies a fallback selection. An exact
Dashboard link is useful for inspection but has no promotion meaning.

This boundary preserves P9-01A: only explicit human choice selects an existing
Component or promotes a proposal, while all other actors retain their bounded
source authority.

## P9-11 deterministic coverage-classification contract

P9-11 classifies each `RequiredCapability` only from the typed structural
fields of its linked `ComponentEvidence`: evidence presence, `absenceReason`,
`redactionReason`, `limitations`, and asserted `componentId`. The rules are
evaluated in the listed precedence; no later rule overrides an earlier one.
`RequiredCapability` input order and each capability's linked evidence input
order are retained for accounting and navigation only. They are not a
classification or selection key.

| Precedence | Deterministic structural rule |
| --- | --- |
| 1 | No linked admitted `ComponentEvidence` for a `RequiredCapability` produces `gap` with an explicit no-evidence reason. |
| 2 | When every linked `ComponentEvidence` has an explicit `absenceReason`, the capability produces `gap` while retaining the ordered evidence IDs and explicit absence reasons. |
| 3 | When any linked evidence has `redactionReason`, nonempty `limitations`, no asserted `componentId`, or explicit-absence evidence mixed with non-absence evidence, the capability produces `unresolved` while retaining every evidence identity and condition. No stronger outcome may hide that limitation or ambiguity. |
| 4 | Otherwise, when all linked evidence has an asserted exact `componentId` and no preceding gap/unresolved predicate applies, the capability produces `alternative`. The candidate/evidence identities retain input evidence order and are not ranked or selected. |
| 5 | `selected` has no P9-11 producer and remains invalid without P9-12's explicit, in-scope `HumanDecision`. `proposal` has no P9-11 producer and remains P9-13 provider work. |
| 6 | availability and authorization value strings remain preserved, attributable data. P9-11 does not parse, normalize, or infer semantic authority from their contents. |

## P9-12 human-decision admission contract

P9-12 admits `selected` only as a transient projection of a valid
`HumanDecision` for an existing Component. This Slice does not construct a
`ComponentProposal`, select a provider, or execute proposal promotion; P9-13
owns provider/proposal construction and promotion execution.

The decision-admission record retains the `HumanDecision` stable identity,
human actor and provenance, exact `ApplicationIntent`, `RequiredCapability`,
and `CoverageDisposition` scope, target existing `componentId`, considered
evidence identities, rationale, and every explicit condition, disagreement,
or limitation. Decision time, revision, and source context remain attributable
when supplied. A valid decision MUST have this exact admitted scope and an
unambiguous target `componentId` asserted by at least one linked, admitted
`ComponentEvidence` for that `RequiredCapability`; the matching evidence
identity MUST be among the decision's considered evidence. A missing or
mismatched scope, target, linked evidence, or considered-evidence relationship
is invalid and MUST NOT emit `selected`.

When the decision is valid, the projection may expose `selected` for its
addressed disposition and retains the decision identity, human provenance,
target, matching evidence identity, all linked evidence identities and
conditions, and the decision's rationale and conditions. `selected` changes
neither catalog truth nor source authority, and it does not erase P9-11
evidence, absence, redaction, limitation, availability, or authorization
attribution. When there is no valid decision, including when the decision is
invalid, the projection MUST preserve the P9-11 deterministic disposition and
MUST NOT infer a fallback selection.

No source value, input or presentation order, name, provider output or score,
or Component, Dashboard, Discovery, or CML navigation link substitutes for a
valid `HumanDecision`. P9-12 does not mutate evidence, catalog metadata,
provider state, CML, application state, or a source authority, and it does not
create persistence, approval history, API, transport, Dashboard, or execution
behavior.

### P9-12B required executable matrix

P9-12B MUST cover the following decision-admission behavior without changing
P9-11 structural classification or the P9-13 proposal/provider boundary:

- a valid exact-scope decision whose existing `componentId` matches linked,
  admitted considered evidence produces `selected` and retains the decision,
  evidence, and condition attribution;
- no decision preserves the P9-11 disposition without a selected result;
- a decision with a missing or mismatched scope, target, linked evidence, or
  considered-evidence relationship is invalid, produces no `selected`, and
  preserves the P9-11 disposition;
- source values, order, names, provider output or score, and navigation links
  cannot produce `selected` without a valid decision; and
- a proposal or provider result cannot produce `selected` in P9-12; its
  construction and promotion execution remain P9-13 work.

## P9-13 advisory-provider and proposal contract

`AdvisoryProviderContract` is a versioned, attributable declaration for an
advisory provider. It retains a stable provider identity, contract version,
provider provenance, typed provider availability, and every supplied
limitation. Availability is explicit typed input; no availability value string
is parsed, normalized, or inferred into authority.

`ComponentProposal` is a provider-attributed, version-matching suggestion for
one exact `ApplicationIntent` and `RequiredCapability` scope. It retains a
stable proposal identity, the target existing `Component` identity, provider
rationale and provenance, considered evidence identities, conditions, and
limitations. A proposal cannot create a catalog fact or evidence.

A proposal is admitted only when its provider is known and typed available,
its version matches that provider contract, its intent and capability scope
are exact, all considered evidence is known and linked to that capability,
and at least one considered linked evidence record asserts its exact existing
target `Component`. Invalid, duplicate, unknown, unavailable, or
provider/proposal-version-mismatched input is a typed admission failure and
MUST preserve the P9-11/P9-12 projection.

For an unselected capability with one or more admitted proposals, the primary
outcome may be `proposal`. That outcome retains the P9-11 structural baseline
and every proposal, provider, evidence, condition, and limitation identity in
input order. It neither ranks nor selects competing proposals, and it does not
hide the retained P9-11 gap, alternative, or unresolved accounting.

A valid P9-12 `HumanDecision` remains the sole selected authority. It may
explicitly name an admitted exact-scope proposal so that promotion attribution
is visible, but the target Component MUST still meet P9-12's evidence-backed
existing-Component admission. An absent or invalid decision never promotes a
proposal.

No provider suggestion, availability, provider type or name, score,
input/presentation order, source, navigation, CML, or Dashboard link
substitutes for evidence or a `HumanDecision`. The P9-13 model is transient
and read-only: it defines no provider call, persistence, provider-state change,
catalog/source/CML mutation, API, Dashboard, build, generation, publication,
or execution.

### P9-13B required focused executable coverage

P9-13B MUST cover valid attributable proposal preservation,
version/provider/scope/evidence rejection, typed unavailable-provider
preservation, non-ranking multiple proposals, and explicit
`HumanDecision`-only promotion. P9-14 owns the complete cross-outcome
specification matrix and structural grouping remediation.

## Transient projection and source boundary

CBD Support may construct a deterministic projection from the admitted intent,
requirements, evidence, proposals, alternatives, and decisions. The
projection is read-only and transient:

- it retains attribution, stable identities, explicit absence, conflicts,
  authorization/redaction, availability, freshness information when supplied,
  and limitations;
- it can link to an exact Component or Dashboard identity without changing
  candidate status or catalog truth;
- it treats CML as planning input only, without assigning unpromoted syntax
  semantics, choosing by source order, or modifying the CML file; and
- it has no write path to canonical source, catalog metadata, provider state,
  application state, or durable model state.

Projection determinism means that the same admitted inputs yield the same
reviewable accounting; it does not introduce a canonical ranking or hidden
winner. A provider's score or order remains provider-attributed data only.

## Deferred lifecycle and implementation boundaries

This design intentionally defers persistence, approval/decision history,
continuation, freshness/invalidation, durable CML projection, application
generation, build, publication, execution, runtime/API/UI behavior, provider
administration, and executable specifications. Phase 10 or another explicitly
admitted slice owns those decisions.

P9-05 owns executable specifications, and P9-06 owns the canonical Component
Design Model and its projections. Neither boundary is silently pulled into
this model design.
