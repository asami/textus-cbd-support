---
status: target
decision_scope: P10-23 / P104-TYPED-OPEN-ISSUE-001
updated_at: 2026-10-01
---

# Internal-model Open-issue Record Contract

This V2 target is the normative logical contract for a portable ledger of unresolved
questions. Its companion is the [Internal-model Open-issue Records
Design](../design/internal-model-open-issue-records.md). The
[Internal-model Package Contract](internal-model-package-contract.md) remains
authoritative for the V2 manifest, inventory, explicit artifact revisions, dependency graph,
and existing `open-issue` role. The [Internal-model Semantic Realization
Contract](internal-model-semantic-realization-contract.md) remains authoritative
for the selected realization, its exact scope, CCDM identities, source
references, and conditions. The [Canonical Component Design Model
Contract](canonical-component-design-model-contract.md) remains the sole
authority for CCDM meaning.

The [Typed Control Contract](internal-model-typed-control-contract.md) and
[Source Snapshot Contract](internal-model-source-snapshot-contract.md) own
reference identity/revision and source attribution. Original P10-23 acceptance
remains historical evidence, separate from this current V2 target. Authoring
here establishes no validation, independent review, acceptance or Phase closure.
Only `ccdm-open-issue-records-v2` / schemaVersion `2.0`, realization V3 / `3.0`,
package V2 and source V2 are admitted; there is no legacy reader, adapter,
overload, inferred/default version or fallback.

This target defines the closed `ccdm-open-issue-records-v2` JSON grammar, immutable
retained record model, one captured package handoff, and read-only admission
described below. This remains a record of unresolved questions, not a decision,
workflow gate, source refresh, or a second CCDM.

## 1. Scope, role, and selected admission

One open-issue ledger is represented by one selected present artifact using the
existing `open-issue` role. A conventional `open-issues.yaml` filename is
compatible, but a filename is neither an identity nor an admission rule. This
contract uses the current V2 package role vocabulary, optional role presence,
role multiplicity, declared-version and inventory behavior, and public
`validateStructure` boundary.

Structural package validity does not establish issue admission. Issue admission
requires exactly one selected present open-issue artifact and exactly one
selected present realization artifact. The selected issue artifact SHALL
directly depend on that selected current realization in the manifest. The
realization's selected source snapshots remain the transitive basis. Missing or
multiple selections, or a missing direct dependency, fail issue admission; they
do not create a new structural package rule. A decision artifact and absent
history are not required, and an issue ledger SHALL not create a dependency cycle or infer a package,
artifact or logical version from content.

## 2. Ledger basis and identity boundary

The ledger has one explicit `ledgerReference: RecordReference(recordId,
recordRevision)`, one required scope retaining the selected realization's
Component, projection-context and selected-Use-Case identities, and one exact
basis. Basis has exactly `realizationArtifactReference: ArtifactReference
(artifactId, artifactRevision, role)` with role `realization`, and
`realizationReference: RecordReference(recordId, recordRevision)`. The entire
artifact reference, entire admitted realization logical reference and scope
must equal the selected current capture before issue iteration, including an
empty ledger. The producer allocates ledger and issue logical revisions
independently of package, artifact and realization revisions.

The stable ledger and issue-local identities are attribution and navigation
identities. They are not CCDM element or relationship identities, and SHALL
not create, replace, or promote canonical CCDM facts. An issue identity is not
derived from a label, question text, path, filename, digest, collection order,
time, score, or another inferred identity.

An old or mismatched scope, artifact ID/revision/role or realization record ID/revision
fails current issue admission. A reader SHALL neither silently rebind, retarget,
drop a record, nor claim that a historical basis was verified. Persisting the
current unresolved-question ledger is the boundary here; it is not an
issue-event history, supersession history, or issue-resolution workflow.

## 3. Required open-issue account

Every issue SHALL have an explicit `issueReference: RecordReference(recordId,
recordRevision)` and explicit open state. Issue record IDs are unique within
the ledger even when revisions differ; two versions cannot choose a latest
issue. Error attribution uses the declared recordId.
It SHALL retain all of the following logical content:

| Required content | Required meaning |
| --- | --- |
| Unresolved question | A nonblank question whose unresolved status is not inferred from placement or prose. |
| Decision responsibility | A nonblank required decision role. It keeps responsibility inspectable without inventing an accountable human. |
| Human owner claim | An optional supplied accountable human-owner identity. Its absence records explicit unknown ownership; it does not infer an owner. |
| Impact | A nonblank impact account. |
| Affected targets | Exact existing CCDM element and/or relationship identities in the admitted scope, or an explicit empty target collection with the scope-wide meaning in section 4. |
| Evidence and options | The attributable evidence and considered options described in sections 5 and 6; either collection may be explicitly empty when none was recorded. |
| Context | Explicit assumptions, conditions, and limitations, including the realization-condition references required by section 8. |
| Blocking declarations | The four separately supplied mandatory Boolean declarations in section 7. |
| Basis | The exact current basis from section 2. |

All supplied actor, owner, authority, and provenance information is an
attributable claim only. It does not authenticate a person, authorize a role,
prove an approval, or establish semantic truth.

## 4. Exact target and scope-wide meaning

Every nonempty affected-target collection SHALL resolve each target exactly as
an existing CCDM element or relationship identity in the admitted realization
scope. Unknown, unsupported, or cross-scope references fail issue admission;
they do not establish another semantic lane.

An explicitly empty affected-target collection means that the question is
scope-wide and has no more specific recorded target. It neither fabricates a
missing element or relationship, asserts no impact, nor erases the exact ledger
scope. It is not interchangeable with a missing target collection.

## 5. Attributable evidence

Every evidence item has a stable identity within its owning issue and retains
its own source kind, authority claim, source identity,
supplied locator and source-owned revision, conditions, and limitations. The logical source
kinds distinguish selected-realization source evidence, external human evidence,
provider proposal, and other external evidence.

A selected-realization evidence reference SHALL resolve exactly against the
selected realization and retain the complete matching source metadata and the
complete source-reference condition IDs. External evidence remains external:
its supplied authority text cannot upgrade it to canonical source evidence,
human decision, or CCDM fact. Raw sensitive provider payload is not imported
automatically.

## 6. Considered options and no hidden choice

Every option has a stable identity within its owning issue, a nonblank
description, referenced considered-evidence identities, and explicit
assumptions, conditions, and limitations. Every option-evidence link SHALL
resolve within that issue. Evidence and options may be explicitly empty only to
record that none was supplied; emptiness does not infer a candidate or a result.

Identity uniqueness is within each owning collection, not across unrelated
issues or record types. Conditions and prose preserve supplied author order and
duplicates; neither is an identity, ranking, or selection criterion. No
identity, order, score, label, provider suggestion, or source claim may infer,
rank, select, reject, promote, or resolve an option. A later human decision is
separate P10-22 accounting, not a state generated by issue admission.

## 7. Independent blocking declarations

Each issue SHALL explicitly declare whether it blocks each of these four
operations under its exact retained basis:

1. semantic approval;
2. CML projection;
3. application; and
4. validation.

All four declarations are mandatory, independently supplied Booleans.
Omission, an unknown value, a string, or a null-like value cannot default to
`false`. `true` records the stated block for that named operation. `false`
records only that the issue is declared nonblocking for that named operation.
Neither `false` nor an empty issue ledger proves readiness, semantic
completeness, approval, safe application, freshness, successful validation, or
permission. Existing realization and source conditions remain independently
visible. The ledger preserves these declarations; it neither enforces nor
grants a later workflow gate and does not change projection constructors.

## 8. Conditions and complete selected realization retention

An issue with exact targets SHALL retain every selected-realization condition
that affects those targets. A scope-wide issue SHALL retain every selected
realization condition ID. It SHALL also retain every condition ID referenced by
its evidence. Each declared realization-condition ID SHALL resolve exactly.

Admission retains the complete selected realization and source-condition
content, including limitations not selected by an issue, so that no unselected
condition becomes invisible. Externally supplied conditions and limitations are
retained as supplied prose; they are not parsed into a canonical condition or
inferred blocking policy.

## 9. Captured authority and read-only failure boundary

One project-bound manifest/inventory/declared-version authority pass captures immutable
open-issue bytes, the selected realization, and the selected source-snapshot
bytes. Issue admission consumes only that captured handoff. It SHALL not
independently reread a file or manifest, query a live source, provider, chat,
Git history, or archive, repair data, write CML, execute a workflow, or
rehydrate a model.

Malformed, ambiguous, duplicate, malformed, missing, or current-mismatched
captured inputs fail closed. Rejection does not repair a record, promote an
authority, select an option, or alter existing package/realization behavior. An
empty admitted issue collection means only that no questions were recorded in
that exact selected artifact; a missing artifact is not an empty admitted
collection.

## 10. Logical admission and rejection matrix

| Situation | Required result |
| --- | --- |
| One selected present open-issue artifact directly depends on one selected current realization; its ledger has an exact current basis and complete, resolvable open accounts | Admit the retained current issue ledger. |
| The selected artifact has an explicitly empty issue collection and an exact current basis | Admit an empty ledger meaning only no questions were recorded in that artifact. |
| The package has no open-issue artifact | V2 structural validity may remain valid; this is not an empty admitted ledger and issue admission is not established. |
| Open-issue or realization selection is absent or multiple, or the issue artifact lacks a direct dependency on the selected realization | Fail issue admission without changing structural package rules. |
| Ledger or issue identity is missing; an identity is duplicated in its owning collection; open state, question, decision role, impact, basis, required context, or any blocking declaration is absent or invalid | Fail issue admission without repair or inferred values. |
| A target, selected-realization evidence reference, option-evidence link, or declared realization-condition reference is unknown, unsupported, cross-scope, dangling, or incomplete | Fail issue admission without creating a new semantic lane or winner. |
| An identity, ordering, score, provider proposal, authority claim, or empty collection is presented as a chosen option, human decision, approval, canonical source, readiness proof, or issue resolution | Fail issue admission. |
| The ledger's scope or complete artifact/logical realization reference differs from the selected captured realization | Fail current issue admission without rebinding, retargeting, dropping records, or claiming historical verification. |
| A malformed, ambiguous, duplicate, malformed, absent, or mismatched input reaches the captured authority handoff | Fail closed; do not reread, fetch, repair, promote, or execute. |
| The selected current realization is admitted in V3 form and all issue invariants hold | Preserve that complete realization and its source-condition ledger. |

## 11. Closed V2 JSON grammar and immutable record model

The selected `open-issue` artifact is strict UTF-8 JSON even when its
conventional filename is `open-issues.yaml`. BOM, invalid UTF-8, malformed
JSON, trailing non-JSON data and duplicate object members at every depth reject.
Harmless object-key order, insignificant whitespace and equivalent escaping
are admitted. Ordinary deterministic encoding may sort keys and append LF on
demand; whole-file byte equality, re-encoding admission and cached canonical
bytes have no control role.

The root has exactly `profile`, `schemaVersion`, `ledgerReference`, `scope`,
`basis`, and `issues`. Profile is exactly `ccdm-open-issue-records-v2` and
schemaVersion exactly `2.0`. Every RecordReference has exactly `recordId`
and `recordRevision`; every ArtifactReference has exactly `artifactId`,
`artifactRevision` and `role`. Both revisions are required positive lexical
JSON integers fitting Long, including 1 and Long.MaxValue. Strings, signs,
fractional/exponent forms, leading zeros, null, overflow, absence and default
reject. Record IDs and required identity/prose strings contain valid Unicode
scalars and at least one non-whitespace code point, without trimming or
normalization. Artifact IDs and roles obey the typed/package factories.
Bare IDs, additional members and hash-bearing reference shapes reject.
Root `scope` has exactly `componentIdentity`,
`projectionContextIdentity`, and `selectedUseCaseElementIdentity`; root
`basis` has exactly `realizationArtifactReference` and
`realizationReference`. Scope and basis bind every issue and an empty array.

An issue has exactly `issueReference`, `state`, `question`, `decisionRole`,
`ownerIdentity`, `impact`, `affectedTargets`, `consideredEvidence`, `options`,
`assumptions`, `conditions`, `limitations`, `realizationConditionIds`, and
`blocking`. Its state is exactly `open`. `ownerIdentity` is a required nullable
key: `null` retains supplied unknown ownership and any non-null value is a
nonblank owner claim. All arrays are required. An explicit empty target array
means scope-wide; explicit empty evidence, option, or prose arrays mean only
that none was recorded.

An affected target has exactly `semanticIdentityKind` and `semanticIdentity`;
the kind is exactly `element` or `relationship`. Evidence has exactly
`evidenceIdentity`, `kind`, `source`, `sourceReferenceId`, `conditionIds`,
`conditions`, and `limitations`. Its closed kind tokens are
`realization-source`, `external-human`, `provider-proposal`, and
`external-other`. Source has exactly `authority`, `identity`, `locator`,
and `revision`; `locator` and `revision` are required nullable nonblank Unicode
navigation/version strings. Source metadata has no hash. An unknown source
revision remains unknown independently of known producer-owned record,
artifact and carrier revisions.
Only realization-source evidence has a non-null source-reference ID and its
full source object and condition IDs shall match the selected realization.
Every external kind has null `sourceReferenceId` and an empty `conditionIds`
array. An option has exactly `optionIdentity`, `description`, `evidenceIds`,
`assumptions`, `conditions`, and `limitations`; each evidence ID resolves only
within that owning issue.

`blocking` has exactly `semanticApproval`, `cmlProjection`, `application`, and
`validation`. Each is independently a required JSON Boolean. Omission, null,
string, number, object, unknown member, or any default is invalid. All sixteen
Boolean tuples are retained verbatim; no tuple, false value, or empty ledger
is a readiness, approval, permission, completeness, freshness, application,
or validation outcome.

Canonical identity collections are unique and strictly sorted by unsigned
UTF-8 bytes: issues by `issueReference.recordId`, targets by
`(semanticIdentityKind, semanticIdentity)`, evidence by `evidenceIdentity`,
options by `optionIdentity`, and option-evidence, issue-condition, and
evidence-condition ID arrays by their exact IDs. Raw unsorted or duplicate
arrays fail decoding. Pure encoding sorts those identity collections but never
deduplicates them. Prose arrays retain their supplied order and duplicates.

The immutable package-private model consists of
`InternalModelOpenIssueState.Open`,
`InternalModelOpenIssueEvidenceKind.RealizationSource`, `ExternalHuman`,
`ProviderProposal`, and `ExternalOther`; their wire values are the closed
tokens above. `InternalModelOpenIssueBasis`, `InternalModelOpenIssueBlocking`,
`InternalModelOpenIssueEvidence`, `InternalModelOpenIssueOption`,
`InternalModelOpenIssueRecord`, `InternalModelOpenIssueLedger`, and
`InternalModelOpenIssueAdmission` retain the fields defined by this section.
They reuse the admitted semantic scope, target, source, and realization types.
They expose no winner, readiness, application, approval, resolution, or
completeness property.

The executable representation is owned by the
[immutable record types](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecords.scala),
[closed codec](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecordCodec.scala),
and [read-only validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecordValidator.scala).

## 12. Captured current admission

`InternalModelPackageValidator.verifiedOpenIssueRecords` performs one existing
package authority pass, selects exactly one present realization and one present
open-issue artifact, and requires the issue artifact to directly depend on that
realization. It preserves all public structural role optionality and
multiplicity behavior. Its captured handoff contains only the verified issue,
realization, and selected source-snapshot bytes. The codec and semantic issue
validator consume that handoff only; they do not reread the filesystem,
manifest, source, provider, chat, Git history, or archive, write CML, refresh
data, reconstruct a model, or make a second CCDM.

Admission first checks capture metadata and validates the complete captured
V3 realization with the same realization/source package, then requires ledger
scope and the entire artifact and logical realization references to equal the
selected capture. The selected issue dependencies must include the entire exact
selected realization artifact reference. This applies before issue iteration
and therefore also to empty ledgers. Every nonempty target resolves in that
realization. Every declared realization condition resolves; targeted issues
retain all conditions for their targets, scope-wide issues retain all selected
realization conditions, and evidence conditions are included. Additional known
condition IDs are allowed. The complete admitted realization, including
unselected source references, conditions, details, and limitations, remains in
the result.

Selected open-issue, reference, path, bytes and dependencies must be non-null;
IDs/revisions/roles are validated, the role is exactly open-issue, dependencies
contain valid exact references with unique IDs in ascending unsigned UTF-8
order and no self-dependency. These checks precede issue content. Null handoff,
open-issue, realization package, selected realization, source collection, required
paths or snapshot entries produce structured operationInvalid without a partial
account. Realization admission also rejects malformed source metadata, absent
exact dependencies, wrong roles and revision contradictions. There is no reread.

No historical basis/status, resolution, supersession, selected option, score,
approval, authentication, role authorization, provider promotion or decision
record is accepted or inferred. Conventional and safe nonconventional manifest
paths remain permitted. Missing/multiple selections, missing exact dependency,
malformed bytes, unknown fields/tokens, duplicate/unresolved IDs, current-basis
mismatch and hidden required conditions fail closed. Declared versions provide
producer-owned provenance; admission does not detect undeclared valid payload
mutation or prove source authenticity/live freshness.

## 13. Exclusions

Unresolved claims are not canonical CCDM facts, human decisions, approvals,
permissions, or an issue-resolution workflow. This contract does not decide
truth, authenticate owners, authorize CML, apply CML, select a provider,
construct a public API, reconstruct a source/model, add a second CCDM, or claim
complete discovery of issues.

The [executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOpenIssueRecordValidatorSpec.scala)
is the paired executable authoring surface for
the closed grammar, retained records, canonical identity ordering, current
basis, source/condition authority, package capture, and Unicode/prose
properties. Parent-owned validation, review, acceptance, and commit remain
separate from this contract.
