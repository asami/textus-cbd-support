# Application Component Composition Model Contract

## Scope and status

This specification defines the transient application-component composition
model for P9-04. It promotes the stable V1 identities and the promotion
boundary needed to account for application intent, required capabilities,
attributable Component evidence, coverage outcomes, proposals, and explicit
human decisions.

It is subordinate to the accepted [Evidence-Backed Component Composition
Contract](evidence-backed-component-composition-contract.md), which fixes the
shared evidence, authority, attribution, absence, and no-hidden-winner rules
for Phase 9. The accepted P9-01A design
([Evidence-Backed Component Composition and Dashboard Design](../design/evidence-backed-component-composition.md))
is the corresponding responsibility and authority decision. The exploratory
[V1 note](../notes/usecase-driven-component-composition-v1.md) is cited only
as the promoted planning source; it is not normative on its own.

This document specifies semantics and reviewable model records. It does not
claim that typed Scala models, a composition service, a Dashboard, or any
other runtime surface exists.

## Common identity and provenance contract

Every identity defined below MUST have:

- a stable identity within the composition projection and its review context;
- a bounded subject or scope, so that an identity cannot silently be reused
  for a different application, capability, evidence record, outcome, proposal,
  or decision;
- provenance identifying the asserting source or actor, the bounded source
  location or evidence reference, and the authority scope of that assertion;
- links to the evidence and limitations that justify the record, when such
  links are available; and
- a display label only as explanatory text, never as identity or a selection
  key.

Stable identity is an identity requirement, not a persistence requirement. A
future durable representation must preserve these identities and provenance,
but this contract does not choose a storage format or lifecycle.

Evidence from distinct authorities remains distinct. An association between
records is attributable navigation; it is not authority transfer, profile
merging, ranking, or selection.

## Composition identities

### ApplicationIntent

`ApplicationIntent` is the bounded statement of what an application or
application slice is intended to accomplish for the current composition
request. Its purpose is to establish the subject and scope from which
required capabilities may be reviewed.

Its stable identity identifies the intent and its composition scope. Its
provenance identifies the submitting actor or source, the request context,
and any bounded source location or evidence reference. A human or attributable
semantic provider may explain or decompose the intent, but such an explanation
remains separately attributed.

`ApplicationIntent` may relate to `RequiredCapability` records and their
evidence, but it does not establish catalog Component facts, interpret
unpromoted CML syntax, select a component, or authorize application
generation, build, publication, or execution.

### RequiredCapability

`RequiredCapability` is one bounded, reviewable responsibility that the
`ApplicationIntent` asks composition to account for. It is the unit to which a
coverage disposition is attached; it is not a claim that a component exists.

Its stable identity is scoped to its parent `ApplicationIntent` and identifies
the requirement independently of its display wording. Its provenance records
the source of the requirement, any source span or bounded locator, and whether
its decomposition is deterministic or an attributable provider
interpretation. A requirement may retain ambiguity or limitation rather than
being forced into a stronger capability claim.

`RequiredCapability` may be supported, disputed, absent, or unresolved by
`ComponentEvidence`. It does not choose a Component by name, CML order,
lexical similarity, provider order, or any other heuristic, and it does not
promote a provider interpretation to canonical CML semantics.

### ComponentEvidence

`ComponentEvidence` is an attributable record that bears on whether an exact
catalog Component, candidate, or declared responsibility can cover a
`RequiredCapability`. It preserves the source, authority, availability,
authorization, redaction, absence, limitation, subject identity, and bounded
locator required by the shared evidence contract.

Its stable identity identifies the evidence observation or assertion, not just
the Component named by it. Its provenance identifies the catalog, design,
model, runtime, BoK, Review, semantic provider, or other admitted source and
the observation/reference location. If the source establishes an exact
catalog Component identity, the evidence may link to that identity; otherwise
it remains evidence about a candidate, proposal, or absence.

`ComponentEvidence` supports a `CoverageDisposition` and can explain an
`Alternative`, `Gap`, `ComponentProposal`, or `unresolved` result. It never
becomes a selection authority, a catalog fact for another source, a canonical
ranking, or a human decision merely because it is available, detailed, fresh,
or displayed first.

### CoverageDisposition

`CoverageDisposition` is the reviewable, per-capability projection that
accounts for the current evidence and decision state for one
`RequiredCapability`. It retains the capability identity, the evidence and
limitations considered, one of the reviewable outcomes below, and any
applicable `HumanDecision` relationship.

Its stable identity identifies the disposition for the capability in the
current composition projection and review context. Its provenance identifies
the projection context and the contributing evidence/decision identities; it
does not pretend that a projection is a source observation.

The disposition is deterministic with respect to its admitted inputs, but
determinism does not imply selection. `selected` is valid only when an
explicit, in-scope `HumanDecision` identifies the selected existing Component
or the promoted proposal. Without that decision, the disposition MUST remain
`alternative`, `proposal`, `gap`, or `unresolved` as applicable. A disposition
does not mutate catalog truth, provider output, CML, or application state.

### Alternative

`Alternative` is a reviewable competing candidate or evidence path for a
`RequiredCapability` when more than one possible coverage path remains. Its
purpose is to keep competition, ambiguity, and supporting evidence visible
without manufacturing a winner.

Its stable identity identifies the alternative within the capability and
composition scope. Its provenance links the candidate/evidence source,
provider output when applicable, and limitations. Provider score, order,
freshness, naming similarity, and presentation order may be retained only as
attributed information.

An `Alternative` may become the target of a `HumanDecision`; until then it is
not selected, does not rank other alternatives, and does not become a catalog
Component fact or application dependency.

### Gap

`Gap` is a reviewable record that the current admitted evidence does not
establish coverage for a `RequiredCapability`, including no linked admitted
evidence or linked evidence whose absence is explicit. Its purpose is to make
unsupported responsibility visible.

Its stable identity identifies the uncovered capability and the bounded
projection context. Its provenance identifies the evidence searched or
missing, the absence/limitation scope, and any relevant source diagnostics.

A `Gap` does not assert that no Component could ever cover the capability, and
it does not authorize a guessed Component, a provider proposal, or an implicit
selection. A later `ComponentProposal` or new evidence remains separate until
reviewed.

### ComponentProposal

`ComponentProposal` is an attributable provider or other admitted proposal for
an existing Component whose exact target is independently established by linked
evidence. Its purpose is to preserve a reviewable suggestion and its rationale
without collapsing suggestion into truth. A suggestion for a not-yet-established
Component or responsibility is outside this transient Phase 9 input boundary:
it is not an admitted `ComponentProposal` and produces no proposal outcome,
evidence, selection, or catalog truth.

Its stable identity identifies the proposal and its proposal scope. Its
provenance identifies the proposing provider or actor, provider version or
request context when supplied, considered linked `ComponentEvidence`, and
limitations. Its exact existing Component target MUST be independently
established by that evidence; a proposal alone cannot create or mutate that
identity.

A `ComponentProposal` is never an existing catalog Component fact, selection,
canonical ranking, CML mutation, or application-generation instruction. An
explicit `HumanDecision` may promote it as a selected composition outcome;
that promotion remains a decision record and does not fabricate catalog facts
or bypass the owning catalog/design admission contract.

### HumanDecision

`HumanDecision` is the explicit, attributable decision that admits a selected
existing Component or promotes a `ComponentProposal` for a bounded
composition scope. It is the only selection authority in this model.

Its stable identity identifies the decision, its decision scope identifies the
`ApplicationIntent`, `RequiredCapability`, and disposition it addresses, and
its provenance records the human actor, rationale, decision time or revision
when supplied, source context, and evidence/proposal identities considered.
The decision MUST identify its target unambiguously and retain disagreement,
limitations, or conditions when they are part of the decision.

`HumanDecision` can authorize a `selected` reviewable disposition, but it does
not rewrite source authority, create a catalog Component, turn a proposal into
historical catalog truth, infer unsupported CML semantics, or authorize
generation, build, publication, execution, persistence, or continuation.

## Reviewable coverage outcomes

Each `CoverageDisposition` exposes exactly one primary reviewable outcome for
its `RequiredCapability` in the current projection. The outcome vocabulary is:

- **selected:** an explicit in-scope `HumanDecision` identifies an existing
  Component or promotes a proposal. The outcome is a projection of that
  decision, never an inference from evidence or ordering.
- **alternative:** one or more attributable candidate/evidence paths remain
  in competition and no applicable decision admits one as selected.
- **gap:** no linked evidence is admitted, or every linked evidence record
  declares an explicit absence.
- **proposal:** an attributable `ComponentProposal` is available for review,
  but no applicable decision has promoted it. A proposal may be shown beside a
  gap or unresolved limitation; it is not silently selected.
- **unresolved:** conflict, ambiguity, redaction, malformed or
  stale evidence, decomposition uncertainty, or another limitation prevents a
  truthful stronger outcome.

These outcomes are reviewable facts about the current projection. None selects
a component merely by existing in the projection. Only `selected` backed by
the required `HumanDecision` can expose a selected disposition, and that
decision must remain visible beside the outcome. A proposal, candidate,
alternative, Dashboard link, Discovery result, provider suggestion, or CML
record cannot substitute for it.

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

### P9-11B required executable matrix

P9-11B MUST cover the following classifier behavior without changing the
authority boundary above:

- no linked evidence produces `gap` with the explicit no-evidence reason;
- all explicit-absence evidence produces `gap` and retains ordered evidence
  IDs and absence reasons;
- ordered exact asserted candidates produce `alternative` and retain input
  evidence order without ranking or selection;
- redacted, limited, componentless, and mixed absence/non-absence evidence
  produces `unresolved` and retains every evidence identity and condition; and
- `selected` never emerges from evidence; it requires the in-scope
  `HumanDecision` owned by P9-12.

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

## Transient read-only composition projection

The composition projection is a deterministic, transient, read-only view over
an `ApplicationIntent`, its `RequiredCapability` records, admitted
`ComponentEvidence`, attributable proposals/alternatives, and admitted
`HumanDecision` records. It MUST:

1. retain stable identities and provenance for every projected record;
2. retain explicit absence, authorization, redaction, availability, conflict,
   and limitation information;
3. attach one reviewable `CoverageDisposition` to every admitted required
   capability; and
4. preserve exact links to an existing Component or Dashboard identity without
   treating navigation as selection.

CML may be supplied as planning input, including bounded source locations, but
this contract does not assign it unpromoted syntax semantics and does not
modify CML. The projection cannot infer a winner from CML source, source
order, names, diagrams, provider scores, or retrieval order.

Transient and read-only mean that the projection does not write canonical
source, catalog metadata, provider state, application state, or durable model
state. It is inspectable for the current review context and may be recreated
from its admitted inputs.

## Explicitly deferred boundaries

The following are outside this contract and require Phase 10 or separately
admitted work:

- persistence or durable internal-model storage;
- approval history, decision history, or continuation packages/cursors;
- freshness and invalidation lifecycle semantics;
- CML parsing, mutation, or projection storage;
- application generation, assembly, build, publication, or execution;
- runtime, public API, Dashboard, provider-administration, or UI behavior; and
- executable specifications for this model.

No implementation, API, lifecycle, or storage behavior may be inferred from
the identities or projection described here.

## Contract preservation

This model preserves the P9-01A attribution and no-hidden-winner contract. It
does not modify or supersede the accepted shared contract or design, and it
does not promote the V1 planning note beyond the stable boundaries stated
here. P9-05 executable specifications and P9-06 canonical Component Design
Model work remain separate Phase 9 slices.
