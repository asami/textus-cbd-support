---
status: target
decision_scope: P9-61A / P104-TYPED-PATCH-TRACE
updated_at: 2026-10-01
---

# Candidate Design and Semantic-Diff Integration Design

This design fixes the P9-61A documentation boundary for a transient, read-only
candidate-design integration. Its normative companion is the
[Candidate Design and Semantic-Diff Integration Contract](../spec/candidate-design-semantic-diff-integration-contract.md).

Phase 10.4 changes the existing shared patch control fields to typed references.
Original P9-61 acceptance is historical. Current authoring, parent validation
and independent review remain separate; no Phase 9 checklist is reopened and no
current Step or Phase closure follows from this design.

## Purpose and authority

The integration makes one proposed design change inspectable without making
the proposal, its candidate, its review, or an external Git reference into a
canonical Component fact. It accepts one attributable P9-60
Analysis-to-Design Impact Projection proposal and its direct impact/link
evidence, then retains the explicitly supplied candidate-side evidence that is
needed to discuss that proposal in one exact Component and bounded Canonical
Component Design Model (CCDM) context.

The [Analysis-to-Design Impact Projection Design](analysis-design-impact-projection.md)
and its contract retain authority for proposal origin and direct impact/link
admission. The [Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and [Canonical Component Design Model Contract](../spec/canonical-component-design-model-contract.md)
retain authority for Component identity, CCDM semantics, canonical-source
ownership, source attribution, locators, and the rule that a canonical fact
changes only through source-owned candidate change plus repository/Git-governed
acceptance. The recorded CML design-improvement loop supplies the intended
direction, but this design does not implement that loop.

The integration is not a CML reader, parser, validator, generator, text-diff
engine, candidate applicator, model acquirer, Review invocation, Dashboard
navigation surface, Git client, approval mechanism, persistence model, or
canonical-source authority.

## One bounded record and its explicit associations

One candidate-design integration record is all-or-nothing and belongs to one
exact Component identity and one bounded CCDM context. It retains these
independently admitted values:

| Value | Required retained identity and boundary |
| --- | --- |
| Proposal evidence | One P9-60 proposal identity and its direct impact-record and proposal-to-impact-link identities. The proposal retains its own attributable origin and is not recreated here. |
| Proposed-CML patch trace | One patch identity, its exact baseline source-snapshot ArtifactReference, exact proposed-content RecordReference, exact CML owner, and bounded CML source locator. Supplied trace identities and versions are the admission boundary. |
| Candidate Component Design Model | One exact candidate-model identity scoped to the same Component/context and explicitly associated with the patch identity. |
| Semantic Design Diff | One caller-admitted diff identity and its explicitly supplied entries, each associated with the exact patch and candidate-model identities. |
| Candidate Review evidence | One externally supplied, read-only review-evidence snapshot identity, review-source identity/locator, and its association with the exact candidate-model identity. |
| Git-governance reference | One externally supplied governance-reference identity and bounded repository/ref or review locator that can identify governance evidence without operating it. |

Every association is explicit. The integration never reconstructs a missing
proposal, CML owner, patch, candidate model, semantic delta, review result, or
governance state from a name, label, source locator, reference equality alone, semantic
target equality, visual relationship, CML order, candidate order, provider
output, review score, Git reference, or another projection. A supplied value
that cannot establish the required exact Component/context or association is a
rejected admission, not material from which the integration may assemble a
partial record.

The retained value identities lead presentation. Within an integration record,
ordering is deterministic: Component/context, proposal identity, patch
identity, candidate-model identity, semantic-diff identity, review-evidence
identity, and governance-reference identity precede admitted non-semantic tie
keys. Source order, CML order, labels, display names, provider preference,
freshness, review conclusion, Git state, or caller iteration cannot select a
relation, interpretation, delta, review outcome, acceptance, or winner.

## Proposed-CML patch trace without source access

Where the stated impact is CML-owned, the integration retains an exact CML
owner and bounded CML source locator together with the explicitly supplied
proposed-CML patch identity, `baselineArtifactReference` and
`proposedContentReference`. This binds a
candidate to a stated source basis while preserving the owning source's
authority.

The record does not retain, retrieve, compare, interpret, normalize, or expose
the CML source text as its admission rule. It does not parse or validate CML,
generate a patch, calculate a textual diff, apply a patch, create candidate
CML, mutate a source, invoke source I/O, or claim that equal or unequal ordinary
payload establishes a semantic change. Exact owner, locator, patch identity and
producer-declared artifact/record versions remain bounded traceability evidence.

The shared patch already has a public construction boundary. It now consumes
the same distinct opaque artifact/record domains and reference classes as
durable internal-model consumers, with the existing role enum and factories
narrowly made public. Package identity/project token/package references and
all capture/codec APIs remain private. No parallel public reference model,
compatibility constructor, adapter or inferred version is introduced.

Integration admission validates nonnull reference metadata, domain-admitted ID,
positive Long revision, exact source-snapshot role for the baseline and nonblank
valid Unicode logical content identity. It collects those violations alongside
the existing scope, evidence and association violations without looking up a
package or reading payload. Artifact and logical revisions are independent;
declared provenance authenticates neither a source nor stored claim and cannot
detect an undeclared producer change. The supplied reference does not authorize
source access, patch application, review, approval or canonical acceptance.

## Candidate model and explicit semantic design diff

The Candidate Component Design Model is a named, caller-admitted candidate
identity. It is not a new canonical CCDM, a reconstructed model, or proof that
a proposed patch can be applied. The integration does not acquire, generate,
validate, compare, merge, or otherwise materialize candidate model contents.

Each semantic Design Diff entry is caller supplied and separately attributable.
It contains an exact entry identity, category, action, subject identity,
before relationship value, after relationship value, exact candidate-model
identity, exact proposed-patch identity, attribution, bounded locator,
applicable conditions, and stated limitations. Category, action, subject,
before, after, and relationship values are values explicitly supplied by the
admitting caller; they are not derived from prose, labels, target equality,
graph shape, CML ordering, patch text, a candidate-model shape, or another
projection.

The record can retain an explicitly stated absence, ambiguity, conflict,
redaction, availability state, or limitation at any affected association or
entry. These conditions remain distinct. In particular, unavailable evidence
is not absence; redaction is not unavailable; an ambiguous or conflicting
relationship has no local winner; and an explicit absence is limited to its
stated source scope. Such a condition neither synthesizes a semantic delta nor
converts a proposal or candidate into a canonical fact.

## Candidate Review and Git governance are evidence only

Candidate Review is an externally supplied, read-only evidence snapshot. It
links the exact candidate identity to an exact review-source identity and
bounded locator, preserves the supplied review state, attribution, conditions,
and limitations, and may remain unavailable, redacted, ambiguous, conflicting,
or limited. It neither invokes Review nor turns a candidate into a canonical
fact, a source mutation, an approval, or a repository acceptance.

Git-governed acceptance is likewise an externally supplied governance
reference. It identifies evidence from the owning repository process and may
state only the externally supplied bounded state such as unavailable,
unsubmitted, submitted, under-review, rejected, or accepted. A reference,
branch name, pull-request identity, commit identity, merge label, or review
result alone does not infer any omitted state. This documentation and its later
pure runtime do not create branches, pull requests, commits, human approvals,
merges, or canonical-source changes.

Canonical source ownership is unchanged until a source-owned candidate has
been accepted through that external Git-governance process. Before then, the
proposal, patch trace, candidate model, semantic diff, candidate Review, and
governance reference are transient, attributable, non-selecting evidence.

## Deferred work

P9-61B may define pure immutable values and executable specifications for only
this admitted record boundary. It cannot use this design to parse, retrieve,
generate, apply, or mutate CML or source, or to execute Review or Git
governance. P9-62 separately owns lifecycle and unified evidence. P9-63
separately owns candidate-to-Dashboard navigation. Phase 9 status and
documentation closure remain outside this slice. Phase 10 separately owns
durable persistence, approval binding/history, continuation, and rehydration.

This design defines no API, Web, schema, transport, persistence, source I/O,
validation, review execution, Git operation, commit, push, publication,
deployment, Phase/checklist, README, or journal behavior.
