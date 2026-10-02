---
status: target
decision_scope: P9-61A / P104-TYPED-PATCH-TRACE
updated_at: 2026-10-01
---

# Candidate Design and Semantic-Diff Integration Contract

This is the normative P9-61A contract for one transient, read-only
candidate-design integration record. Its rationale is in the
[Candidate Design and Semantic-Diff Integration Design](../design/candidate-design-semantic-diff-integration.md).

The Phase 10.4 target changes only the shared patch's control references.
Original P9-61 acceptance remains historical; current typed authoring requires
parent validation and independent review. It neither reopens a Phase 9
checklist nor establishes current Step or Phase acceptance.

## 1. Authority and exact scope

An integration record SHALL be scoped to one exact established Component
identity and one bounded Canonical Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for Component identity, semantic identity, canonical-source
ownership, attribution, locators, conditions, and the source-owned
candidate-plus-Git-governance condition for a canonical-source change. The
[Analysis-to-Design Impact Projection Contract](analysis-design-impact-projection-contract.md)
retains authority for P9-60 proposal origin and direct impact/link evidence.

This contract SHALL retain only caller-admitted, attributable evidence. It
SHALL NOT establish a canonical fact, canonical candidate model, source
authority, semantic identity, source patch, semantic delta, Review conclusion,
approval, acceptance, selection, or mutation authority.

## 2. Required all-or-nothing integration shape

Each integration record SHALL retain all of the following values in the same
exact Component/context, or SHALL fail admission without returning a partial
record:

1. exactly one P9-60 proposal identity and its directly admitted impact-record
   and proposal-to-impact-link identities;
2. exactly one proposed-CML patch identity, exact baseline source-snapshot
   artifact reference, exact proposed-content record reference, exact CML
   owner, and bounded CML source locator;
3. exactly one Candidate Component Design Model identity explicitly associated
   with that patch identity;
4. exactly one caller-admitted semantic Design Diff identity and its explicit
   semantic-diff entries, each associated with that exact patch and candidate
   model;
5. exactly one externally supplied candidate-Review evidence snapshot identity,
   review-source identity, and bounded review-source locator explicitly
   associated with that exact candidate model; and
6. exactly one externally supplied Git-governance reference identity and
   bounded repository/ref or review locator.

Every association SHALL be explicit. Name, label, source location, reference
equality alone, semantic target equality, visual relationship, CML order, candidate
order, review score, provider output, Git reference, or another projection
SHALL NOT admit, recover, replace, redirect, merge, infer, or select a missing
proposal, impact, link, CML owner, patch, candidate, diff entry, review
snapshot, governance state, acceptance, or canonical fact.

## 3. CML-owned patch-trace boundary

For a CML-owned impact, the integration record SHALL retain the exact CML
owner, exact proposed-CML patch identity, exact `baselineArtifactReference`
and `proposedContentReference`, and a bounded CML source locator. These supplied
trace identities and declared versions SHALL be the admission boundary.

`baselineArtifactReference` is the existing public
`InternalModelArtifactReference(artifactId, artifactRevision, role)`, with
nonnull reference/role, a factory-admitted ASCII token artifact ID, a positive
Long artifact revision and exactly `SourceSnapshot` (`source-snapshot`) role.
`proposedContentReference` is the existing public
`InternalModelRecordReference(recordId, recordRevision)`, with nonnull reference,
a nonblank valid Unicode logical ID and an independently supplied positive Long
logical revision. Invalid IDs, roles, revisions or missing reference metadata
join every other observed admission violation in the all-or-nothing result.
Validation uses supplied metadata only, without payload or package lookup.

Only the existing artifact/record opaque domains, companions, artifact role
enum/companion and reference values are public for this shared trace. Package
domains/references, captured package context and codecs remain private.
There is one typed reference model: no aliases, adapters, old digest constructor,
conversion reader or inferred default accompanies this boundary. The public
[external caller executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/api/CandidateDesignPatchReferenceSpec.scala)
documents construction and compiler-enforced distinct domains.

This contract SHALL NOT authorize CML or source retrieval, source I/O, parsing,
validation, generation, textual diffing, interpretation, normalization, patch
creation, candidate construction, patch application, source mutation, or any
claim that reference or ordinary payload equality establishes a semantic change.
Producer-owned references establish declared provenance, not authentication,
undeclared payload mutation detection, source freshness or patch applicability.
A locator is
traceability only and SHALL NOT grant source authority or source access.

## 4. Candidate model and semantic-diff entry admission

The Candidate Component Design Model SHALL be a caller-admitted exact identity
in the same Component/context. It SHALL be associated explicitly with the
proposed-CML patch identity. It SHALL NOT become a canonical CCDM, prove a
patch applicable, or authorize acquisition, generation, validation,
comparison, merging, materialization, or mutation of candidate content.

Each semantic Design Diff entry SHALL be explicitly supplied and attributable.
It SHALL independently retain:

| Required value | Contractual meaning |
| --- | --- |
| entry identity | An exact identity for this supplied semantic-diff entry. |
| category | An explicitly supplied category; no category is inferred. |
| action | An explicitly supplied action; it is not derived from source text or an ordering change. |
| subject identity | The exact stated subject; matching labels or targets cannot substitute. |
| before relationship value | The supplied before value, including an explicit bounded condition when no value is available. |
| after relationship value | The supplied after value, including an explicit bounded condition when no value is available. |
| relationship value | The supplied semantic relationship value; graph shape or endpoint equality cannot reconstruct it. |
| candidate and patch identities | The exact candidate-model and proposed-patch identities to which this entry traces. |
| attribution, locator, conditions, limitations | Source/authority attribution, bounded locator, each applicable condition, and stated limitation. |

This contract SHALL NOT synthesize a change from text, labels, target equality,
graph shape, CML ordering, candidate-model ordering or shape, patch content,
another projection, or an omitted before/after/relationship value. It SHALL
NOT treat a category, action, subject, or relationship as supplied merely
because an adjacent record appears compatible.

## 5. Conditions, explicit absence, and deterministic order

Every integration value and semantic-diff entry SHALL retain every applicable
condition distinctly, including availability, explicitly absent, ambiguous,
conflicting, redacted, unavailable, unauthorized, stale, malformed, and
limited. Availability does not suppress another applicable condition.

- Explicit absence SHALL remain bounded to its stated owner and source scope;
  it SHALL NOT become a broad negative semantic fact.
- Unavailable or unauthorized material SHALL NOT be represented as absence.
- Redacted material SHALL NOT be represented as unavailable material.
- Ambiguous or conflicting material SHALL preserve its attribution and SHALL
  NOT select a local interpretation or winner.
- A stated limitation SHALL NOT be repaired, widened, or removed by local
  inference.

An integration record and its retained values SHALL be presented
identity-first: exact Component/context, proposal, patch, candidate model,
semantic diff, review evidence, and governance reference identities precede
remaining retained identities and, only when required for a total order, an
admitted stable non-semantic tie key. Labels, names, CML/source/candidate
order, layout, provider preference, freshness, review result, Git state, or
caller iteration SHALL NOT determine order, relation, interpretation,
acceptance, or a winner. If admitted keys cannot make an order total, the
unresolved ordering condition SHALL remain explicit.

## 6. Candidate Review evidence

Candidate Review SHALL be an externally supplied, read-only evidence snapshot
that links the exact candidate-model identity to the exact review-source
identity and bounded review-source locator. It SHALL preserve supplied
attribution, state, conditions, and limitations, including unavailable,
redacted, ambiguous, conflicting, and limited states.

The integration SHALL NOT invoke Review, inspect a review provider, execute
validation, calculate a conclusion, update a snapshot, or convert Candidate
Review evidence into a canonical fact, source mutation, approval, repository
acceptance, or canonical-source change. Candidate validation or review SHALL
remain distinct from repository acceptance.

## 7. External Git-governance reference

Git-governed acceptance SHALL be represented only by an externally supplied
governance-reference identity and bounded repository/ref or review locator. An
externally supplied governance state MAY be retained only when it is explicitly
attributed and bounded, for example unavailable, unsubmitted, submitted,
under-review, rejected, or accepted.

This contract SHALL NOT infer an omitted state from a branch name, pull-request
identity, commit identity, merge label, review result, source locator, or any
other evidence. It SHALL NOT create or mutate a branch, pull request, commit,
human approval, merge, repository reference, or canonical source. A governance
reference is evidence only; it does not itself create acceptance.

Until the owning source's candidate has been accepted through its external
Git-governance process, the canonical fact SHALL remain unchanged. All proposal,
patch, candidate, semantic-diff, Candidate Review, and governance records
SHALL remain transient, attributable, and non-selecting. An external acceptance
does not transfer canonical-source ownership to this integration record.

## 8. Reserved work and prohibitions

P9-61B may separately implement pure immutable values and executable
specifications that preserve this exact contract. It SHALL NOT use this
contract to parse, retrieve, generate, validate, apply, or mutate CML or
source, or to execute Review or Git governance.

P9-62 lifecycle and unified evidence, P9-63 candidate-to-Dashboard navigation,
Phase 9 status/documentation closure, and Phase 10 durable persistence,
approval/history binding, continuation, and rehydration are reserved for their
separately accepted work. This documentation creates no API, Web, schema,
transport, persistence, source I/O, validation, Review execution, Git
operation, commit, push, publication, deployment, Phase/checklist, README, or
journal behavior.
