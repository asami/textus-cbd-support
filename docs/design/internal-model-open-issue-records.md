---
status: accepted
decision_scope: P10-23
updated_at: 2026-09-28
---

# Internal-model Open-issue Records Design

This design explains the unresolved-question recording boundary fixed by the
[Internal-model Open-issue Record Contract](../spec/internal-model-open-issue-record-contract.md).
That contract is normative. The [Internal-model Package
Contract](../spec/internal-model-package-contract.md) remains the authority for
the existing `open-issue` artifact role, inventory, dependency graph, and exact
artifact bytes. The [Internal-model Semantic Realization
Contract](../spec/internal-model-semantic-realization-contract.md) remains the
authority for current scope, CCDM identities, source references, and conditions.
The [Canonical Component Design Model
Contract](../spec/canonical-component-design-model-contract.md) remains the
sole authority for CCDM meaning.

P10-23B now realizes this boundary through the closed
`ccdm-open-issue-records-v1` canonical JSON form, package-private immutable
records, the [closed codec](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecordCodec.scala),
[read-only validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecordValidator.scala),
and its [paired executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecordValidatorSpec.scala).
The [immutable record types](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecords.scala)
remain package-private. The contract remains normative; this document explains
why the implementation deliberately keeps the record boundary narrow.

## Purpose: retain an unresolved account without promoting it

The existing package role is sufficient because an open issue is a portable
account of an unresolved question, not another kind of semantic source. A
conventional `open-issues.yaml` name helps people locate the artifact, but it
cannot identify a ledger, admit it, or choose a result.

The account retains a required decision role, an optional supplied owner claim,
impact, exact targets when known, evidence, options, limitations, and four
independent operation-blocking declarations. This makes responsibility and
incompleteness reviewable while leaving unknown ownership explicitly unknown.
It deliberately does not turn a claim into authenticated identity,
authorization, approval, or a completed human decision.

An unresolved account also is not a canonical CCDM fact. It may point to exact
existing CCDM identities, but it cannot create an element, relationship,
canonical assertion, source authority lane, or inferred semantic fact. Its
options retain alternatives without ranking or selecting one. The P10-22 human
decision account remains separate: no reader promotes an option by order,
score, provider provenance, or storage position.

## Ownership and one captured handoff

The division of responsibilities is intentionally narrow:

```text
project-bound package authority pass
  -> captured selected open-issue bytes
  -> captured selected realization and source snapshots
  -> read-only issue admission
  -> immutable unresolved-question account plus complete realization ledger
```

The package pass alone establishes inventory, exact bytes, selection, and the
direct open-issue-to-realization dependency. The realization alone supplies the
exact Component/projection-context/selected-Use-Case scope, semantic targets,
source references, and conditions. The issue account supplies only its own
attributable unresolved material. Keeping these roles separate prevents a
second manifest pass, mutable filesystem substitution, live-source lookup,
chat/Git/archive reconstruction, or a storage record becoming a new CCDM.

The ledger's basis intentionally uses both the current realization's semantic
identity and its exact raw-byte hash. The raw-byte hash identifies the precise
captured realization revision; it cannot identify a semantic target. Exact
CCDM identities identify targets; they cannot substitute for the captured
bytes. Retaining both prevents a changed realization with familiar labels or
similar identities from being treated as the same basis.

## Scope-wide questions and condition visibility

An issue can name exact current semantic targets. When it cannot, the explicit
empty target collection means a question about the entire admitted scope, not
that no target or impact exists. This preserves a meaningful scope-wide
question without inventing an absent entity or relation.

Conditions remain a separate shared visibility ledger. A targeted issue keeps
the conditions that affect its targets; a scope-wide issue keeps all selected
realization condition IDs; evidence retains its referenced condition IDs. The
admitted result also retains the full selected realization/source-condition
ledger, rather than a summary that hides limitations unrelated to the issue's
immediate wording. Externally supplied conditions remain attributable prose and
do not become parsed CCDM conditions or implied blocking policy.

## Blocking declarations are records, not gates

The four required Boolean declarations record whether an issue blocks semantic
approval, CML projection, application, or validation for its exact retained
basis. Their independence matters: a declaration about one operation says
nothing about another. A false declaration is only a stated nonblock for that
operation; it cannot establish readiness, completeness, freshness, approval,
permission, safe application, or successful validation.

The reader retains all four values but never executes, enforces, grants, or
waives a later workflow gate. Existing projection constructors and their
contracts therefore remain unchanged. Approval, application, and validation
continue to require their separately owned future boundaries.

## Evidence and alternatives remain attributable

Each evidence item carries its own source kind, authority claim, identity,
digest, locator/revision, conditions, and limitations. Realization-source
evidence must match the retained realization source reference; external human,
provider, and other evidence remain external. A provider payload is not pulled
into the ledger automatically, and authority wording cannot promote any
external material to canonical source evidence or human decision.

Options are linked to the evidence considered for that particular issue and
retain their own assumptions, conditions, and limitations. Empty evidence or
option collections truthfully mean only that none was recorded. They are not
an inference engine for a candidate, a winner, a rejection, a resolution, or
an exhaustive search.

## Closed representation and current-basis boundary

The ledger has one root scope and one root current basis, including when it
contains no issues. This avoids an empty artifact becoming an unbound claim and
prevents per-record old bases from turning question preservation into a history
or resolution system. The semantic scope identifies the retained CCDM context;
the raw realization hash identifies the exact captured revision. Neither can
substitute for the other.

Scala 3 enums model only the closed `open` state and the four evidence kinds.
Immutable case classes retain the supplied scalar, option, source, target,
condition, Boolean, and prose values. The codec recognizes explicit wire
tokens, rejects unknown values, and canonicalizes identity collections only
while encoding. It does not use string substitutes for closed alternatives,
deduplicate author prose, or manufacture a semantic identity from a label,
path, provider claim, or collection order.

The four Boolean declarations remain four values rather than an aggregate
status. They record an issue's supplied declarations but cannot cause a gate,
grant an approval, authorize CML application, or turn an empty ledger into a
readiness signal. This preserves the separate authorities of realization,
projection, decision, and future approval/application consumers.

## Captured one-pass admission

`InternalModelPackageValidator` captures the selected open-issue artifact in
the same `_validate` pass that captures the selected realization and source
snapshots. Its new package handoff is intentionally analogous to the existing
realization, projection, and decision handoffs; it does not add a public
structural role rule or another inventory read. The codec receives exactly the
captured issue bytes. The semantic validator receives the same handoff and
first delegates realization admission to the existing realization validator.

This division proves a meaningful distinction: byte identity comes from the
single package pass, while semantic currentness comes from the already admitted
realization in that pass. It prevents an on-disk substitution after capture
from changing the result and makes a fresh reader failure distinct from a
captured-handoff admission. The read-only path never fetches a source, repairs
data, writes CML, resolves history, rehydrates a model, or invokes a second
CCDM.

## Condition and evidence visibility

Issue-local links are checked only against their owning issue. A repeated ID in
another issue or another record kind therefore has no authority to satisfy a
link. Exact targets select their realization conditions; empty targets expose
the complete selected condition ledger. Realization-source evidence must carry
the exact reference source and all reference-owned conditions. External
evidence remains attributable external material even when its prose mentions a
source-like authority. The admission retains the complete realization instead
of creating a filtered realization for the issue.

## Deliberate incompleteness

Neither slice becomes an issue-resolution or supersession-history system,
approval/CML-application enforcement, source/model rehydration path, public
API, or second CCDM. It also does not claim that issue discovery is complete,
that P10-23 is accepted, or that Phase 10.2 is complete. The normative
[Open-issue Record Contract](../spec/internal-model-open-issue-record-contract.md)
defines the exact grammar and failure matrix; the linked implementation and
executable specification provide it without making an acceptance claim.
