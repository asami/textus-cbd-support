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
| `ComponentProposal` | Preserve an attributable suggestion for existing or proposed coverage | Proposal identity/scope; provider/actor, request context, evidence, and limitations | May be admitted as `proposal` or selected only through a human decision | Is not an existing catalog Component fact, ranking, selection, or CML mutation |
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
- `gap`, when admitted evidence does not establish coverage and the absence or
  limitation is explicit;
- `proposal`, when an attributable proposal is available but unapproved; or
- `unresolved`, when conflict, ambiguity, authorization/redaction, malformed
  or stale evidence, decomposition uncertainty, or another limitation blocks a
  stronger statement.

The terms are reviewable outcome labels, not commands. A `selected` label
without its linked human decision is invalid. An `alternative`, `gap`,
`proposal`, or `unresolved` label never implies a fallback selection. An exact
Dashboard link is useful for inspection but has no promotion meaning.

This boundary preserves P9-01A: only explicit human choice selects an existing
Component or promotes a proposal, while all other actors retain their bounded
source authority.

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
