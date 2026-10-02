---
status: target
decision_scope: P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-01
---

# Internal-model Typed Control Contract

This is the authoritative target contract for internal-model management and
control references. It realizes the owner-directed removal of management hashes
under [Repository Rules](../rules/repository-rules.md). Its rationale is in the
[Typed Control Design](../design/internal-model-typed-control.md).
Bounded typed foundations, package/source, semantic realization/continuity,
candidate/diff, review, and approval/lifecycle authoring are recorded by their
owned contracts. Their validation, independent review and integrated acceptance
remain pending. Remaining consumer migrations and assurance gates below are not
established by source or executable-specification authoring alone.

## Authority and execution model

The consuming project owns local single-writer records under
`src/main/internal-model/`. The producer explicitly allocates identities and
advances positive revisions when their control meaning changes. Allocated
identities and revisions are immutable for the version they name. Consumers
validate supplied versions and references; they do not allocate missing versions,
select a latest record, silently rebind a record, or infer history.

The [Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
remains the sole authority for CCDM meaning. Phase 9's semantic ledger,
source attribution, canonical/enrichment lanes, condition sidecars, Dashboard,
and all eight projections remain the semantic source of truth. Typed management
references organize those values; they create no parallel semantic model.

No hash, checksum, digest, content-addressed name, encoded-content token, or
whole-file byte comparison SHALL establish identity, revision, state, decision,
approval, dependency, applicability, or continuation permission. This execution
model requires no locks, retries, backups, rollback, or interference machinery.
Explicit revisions are control provenance under producer ownership, not
cryptographic authenticity or proof of undeclared content mutation.

## Distinct immutable references

These are distinct types, not interchangeable string tuples:

| Type | Exact closed fields | Meaning |
| --- | --- | --- |
| `PackageReference` | `packageId`, `projectNamespace`, `projectId` | Stable allocated package identity and consuming-project identity. |
| `ArtifactReference` | `artifactId`, `artifactRevision`, `role` | One exact inventoried artifact version and role within its package context. |
| `RecordReference` | `recordId`, `recordRevision` | One exact logical record version within its admitted owner/context. |

Package identity uses the package contract's lowercase UUID grammar. Project
and artifact IDs use its ASCII token grammar. Logical record identities retain
their existing source/record-owned semantics; an artifact ID is not a semantic
element or record identity. Artifact revisions are positive `Long` values explicitly supplied by the
producer, never a default, inferred counter, or content-derived value. Record
revisions are explicitly supplied positive values in their owning record's
revision domain, never defaults or content-derived values. A package carrier's separate `revision`
is also a positive producer-supplied `Long`; it is not part of
`PackageReference`.

The closed artifact roles are exactly `resume`, `source-snapshot`,
`decision`, `open-issue`, `realization`, `projection`, `approval`,
and `validation`. Role presence alone establishes no semantic readiness.
Every dependency is an exact `ArtifactReference`, never a bare ID. Unique
artifact IDs/paths, exact revision and role resolution, presence of consumed
dependencies, no self-dependency, and no cycles are required. Writers preserve
deterministic topological order, using exact artifact ID as the tie-break.
Neither file enumeration nor parser object-key order establishes authority.

## Narrow shared public patch reference boundary

The already public `CandidateDesignProposedCmlPatchTrace` consumes the same
`InternalModelArtifactReference` and `InternalModelRecordReference` domains as
persisted consumers. Only `InternalModelArtifactId`,
`InternalModelArtifactRevision`, `InternalModelRecordId`,
`InternalModelRecordRevision` and their companions, the
`InternalModelArtifactRole` enum/companion and those two reference classes are
public. Their opaque distinctions, factories, field names, roles and positive
Long semantics are unchanged. `InternalModelPackageId`,
`InternalModelProjectToken`, `InternalModelPackageReference`, captured package
context and all package/typed codecs remain private to runtime.

The patch supplies a `SourceSnapshot` artifact reference as
`baselineArtifactReference` and a logical reference as
`proposedContentReference`. There is no second public reference model, alias,
default, digest overload or compatibility reader. Public construction records
producer-declared evidence; it does not authenticate the claim, acquire source
authority or establish undeclared payload change detection. The
[external API executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/api/CandidateDesignPatchReferenceSpec.scala)
authors public construction and distinct-domain compilation obligations.
This visibility change establishes no validation, complete consumer conversion,
Step acceptance or Phase closure.

## Explicit semantic review subject

`ReviewSubject(subjectId, subjectRevision, packageReference, scope, artifacts)`
is a distinct immutable logical value. Its closed fields are exactly those
five names. `subjectId` is explicitly allocated; `subjectRevision` is a
positive producer-supplied revision. `packageReference` uses the type above.
`scope` has exactly `componentIdentity`, `projectionContextIdentity`,
and `selectedUseCaseElementIdentity`, preserving the existing semantic scope.
`artifacts` is the unique, explicitly selected set of exact
`ArtifactReference` values for the contributing source, realization,
continuity, candidate, semantic-diff, and review-evidence inputs.

This subject includes the complete semantic input set actually reviewed and its
required evidence/dependencies. A producer SHALL NOT hide added or changed
semantic inputs by choosing a smaller subset. Review, approval, and resume
control records are excluded from that subject. Neither historical manifest
bytes, a package digest, nor the complete carrier inventory is a subject.

### Bounded complete-subject admission

Under the local single-writer model, review admission derives its complete
required basis from one immutable current capture. Roots are EVERY present
carrier entry whose role is `SourceSnapshot`, `Realization`,
`Projection`, `Decision` or `OpenIssue`, plus explicitly selected
`evidenceArtifacts` with role `Validation`. Resolve complete transitive
dependencies using exact artifact ID, producer revision and role.
Missing, absent, ambiguous or mismatched consumed references reject.
Cycles and dependencies on `Resume`, `Approval` or the actual selected
review reject. Forbidden dependencies are never silently omitted.

Order the unique resulting references by unsigned UTF-8 artifact ID and
require exact vector equality with supplied `subject.artifacts`.
Selected review dependencies equal that same vector, including order,
revision and role. Subject artifacts are nonempty, unique by ID and ordered;
self-review is excluded against the actual external selection. Subject package
reference equals the stable carrier identity, and subject scope equals
review/candidate/diff/realization/continuity scope.

Unconsumed optional absent entries remain carrier inventory outside the
subject. Unselected Validation entries are inert unless selected evidence
or a transitive dependency. Resume, approval, unselected-review controls and
carrier revision changes do not change an unchanged subject. Raw
decision/open-issue contribution creates no separate semantic admission.

`subjectId` reuses the opaque logical record-ID domain, preserving nonblank
valid Unicode without normalization. `subjectRevision` reuses the positive
producer Long record-revision domain with lexical integer admission:
fractions, exponents, strings, zero, negatives, null and overflow reject.
No artifact or carrier revision substitutes for it. Admission checks declared
references and versions, not undeclared same-ID/revision payload mutation,
authenticity, live freshness or producer history.

The runtime-private five-field subject and current review V2 model,
codec/validator and executable migration are authored within this boundary.
Human-approval V2 and lifecycle V2 source/executable migration are also authored
within the separately frozen approval/lifecycle boundary. Continuation migration,
integrated validation and all P10-40 through P10-44 assurance gates remain pending.

A changed subject reference, scope, package reference, selected input set, or
selected artifact revision requires an explicit new subject revision and review.
Carrier-only control additions or a cursor revision do not change an otherwise
unchanged subject. Subject/version correspondence is the producer's explicit
obligation; a validator does not infer undeclared changes from payload bytes.

## Review, human input, and lifecycle

Review records bind the exact subject identity/revision and complete subject
value, candidate identities/revisions, review identity/revision, exact target
evidence, and independently caller-admitted rule/provider ID/version basis.
Artifact bindings are `ArtifactReference` values. Rule/provider bindings
contain only their existing ID and version fields; no content hash is retained.
All existing target bindings, snapshot attribution, nullable condition facets,
ordered and multiplicity-preserving limitations, and scope constraints remain.
A recorded review state authenticates neither a provider nor a human.

Approval uses ccdm-candidate-human-approval-v2/2.0 and has exactly
approval/profile/schemaVersion. Its seven-field input
uses actor, approvalReference, basis, decision, four-field provenance, rationale
and ordered unresolvedItems. Its nine-field basis uses candidateArtifactReference,
candidateReference, candidateModelIdentity, reviewArtifactReference,
reviewReference, subject, scope, semanticDiffArtifactReference and
semanticDiffReference. Full external artifact references remain distinct from
logical record versions and the complete review-owned subject. Approval depends
on exactly the selected full review reference. Existing independently supplied, actual attributable human input,
actor/provenance, decision, rationale, unresolved items, and explicit
supersession/applicability requirements remain mandatory. Stored approval SHALL
NOT authenticate itself or replace that independent input. A provider's
`approved` state is not a human decision.

The dependency direction is semantic subject -> review -> approval. A cursor
selects the required existing records, never itself. Review and approval do not
recursively review the carrier or cursor. A cursor-only carrier revision does
not invalidate an unchanged subject. Changing the subject reference or any
selected artifact revision invalidates applicability to that changed basis;
old approval is never rewritten or rebased. Explicit supersession does not
grant successor applicability, rank approvals, or choose the newest record.
All attributable invalidation dimensions remain observable. Invalidation values
retain kind, optional full artifactReference, changedDimensionNames and
missingDimensionNames. ReviewSubjectChanged replaces whole-carrier drift;
SourceIncomplete follows SourceChanged and preserves BOTH dimension vectors.
The supersession ccdm-candidate-approval-supersession-v2/2.0 root names exact
predecessorApproval and successorApproval ArtifactReferences with Approval role,
profile and schemaVersion. The
[human approval contract](internal-model-candidate-human-approval-contract.md)
and [lifecycle contract](internal-model-candidate-approval-lifecycle-contract.md)
fix their exact V2 grammar, actual independent-input admission, pure structural
helpers, explicit lineage and source-owner interpretation. This authored boundary
does not complete continuation, validation, review or acceptance.

## Cursor, missing versions, and outcomes

A derived cursor binds the exact current carrier revision and exact non-resume
`ArtifactReference` selections. Stage, action, blockers, preconditions,
invalidations, and acceptance criteria grant no action until P10-43 independently
checks the recorded action against reconstructed admitted evidence.

Source-owned authority, identity, revision when available, locator, anchor,
selected semantic content, and conditions remain source-owned facts. A missing
source version stays unknown/incomplete; an artifact revision is not a substitute
source revision or evidence of live freshness. No Phase 10.5 CML mutation or
live-source drift gate is implemented by this contract.

Consumers distinguish these outcomes:

| Outcome | Required interpretation |
| --- | --- |
| Invalid serialized structure | Malformed strict JSON, duplicate members, wrong types, missing/extra fields, unsupported schema/profile, or invalid declared syntax. Reject without normalization into acceptance. |
| Semantically incomplete | Required source versions, decisions, mappings, independent evidence, or prerequisites are unavailable/missing. Preserve attributed unknown/incomplete state. |
| Semantically inconsistent | Declared identity, revision, role, scope, dependency, selection, or state contradicts admitted evidence. Preserve all attributable contradictions. |

Missing required serialized fields are structural failures, distinct from an
explicitly represented unavailable semantic fact. No outcome permits defaults,
inferred decisions/mappings, latest-record selection, migration, compatibility
aliases, legacy decoders, or fallback.

## Serialization and ordinary payload

Control envelopes use strict UTF-8 JSON under existing file names. Duplicate
members, YAML-only syntax, unsupported schemas, wrong types, and missing/extra
fields reject. Writers may emit deterministic keys and one terminal LF.
Readers accept harmless object-key order and insignificant JSON whitespace
variation. Re-encoding equality is not an admission condition.

Original/proposed CML bytes may remain ordinary payload required by projections,
with strict decoding and stated length/content constraints. They are not
identity, equality, freshness, or permission tokens. Removing a hash check
does not remove payload decoding, semantic validation, source attribution,
or independently admitted evidence. Management references in source,
realization, decision, open-issue, projection, diff, review, approval, and
continuation records must migrate together without changing CCDM meaning.

This target has no legacy reader, adapter, compatibility alias, inferred schema,
or default version. The package, review, and cursor contracts fix their new
schema/profile pairs. Other affected serialized shapes and executable migrations
require separately owned parent manifests; prior hash/canonical-byte clauses
cannot authorize retention of those mechanisms.

## Pending executable obligations

The shared typed reference foundations and their
[InternalModelTypedControlCodecSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelTypedControlCodecSpec.scala)
are authored. Production consumer migration, validation, and integrated acceptance
remain pending; this foundation does not establish product hash removal.

The following executable obligations remain subject to parent-selected
validation and independent review. Authored migrations are not integrated
acceptance evidence:

- `InternalModelPackageValidatorSpec`, `InternalModelPackageFreshnessSpec`,
  and `InternalModelSourceSnapshotFreshnessSpec`: explicit versions, inventory,
  dependency resolution, strict structure, and unknown source versions.
- `InternalModelSemanticRealizationValidatorSpec` and
  `InternalModelProjectionContinuityValidatorSpec`: unchanged CCDM semantics,
  source witnesses, complete sidecars, and all eight projections with typed basis.
- `InternalModelDecisionRecordValidatorSpec` and
  `InternalModelOpenIssueRecordValidatorSpec`: explicit record versions and
  retained attribution, choices, alternatives, conditions, and unresolved items.
- `InternalModelCandidateCmlProjectionValidatorSpec` and
  `InternalModelSemanticDiffValidatorSpec`: typed dependencies, ordinary CML
  payload decoding, exact targets/mappings, and lossless semantic evidence.
- `InternalModelCandidateReviewBindingValidatorSpec` authors review subject
  binding, independent rule/provider basis and complete semantic evidence.
  `InternalModelCandidateHumanApprovalValidatorSpec` and
  `InternalModelCandidateApprovalLifecycleSpec` now author actual independent human
  input, separate typed/logical/subject/carrier revisions, complete drift and
  SourceIncomplete dimensions, explicit lineage, strict ordinary JSON and PBT.
  Rich two-target binary CML, all eight projections, nullable facets and ordered
  duplicate evidence remain. Their validation and independent review are pending.
- `InternalModelResumeCursorCodecSpec`,
  `InternalModelRehydrationValidatorSpec`, and
  `InternalModelFreshProcessRehydrationSpec`: strict typed cursor admission,
  same-capture pure reconstruction, package-only fresh JVM recovery, and
  explicit failures without control hashes or byte comparison.

Given/When/Then, `should` grouping, and active ScalaCheck remain required.
P10-40–P10-44 remain unfinished executable obligations, including P10-43's
action matrix and P10-44's actual stop/commit/checkout-transfer/resume proof.
Required validation, independent focused review, accepted Step commits, one
complete Phase review, and final Phase release remain with the parent.
This documentation contract does not complete any checklist item, Step, or Phase.
