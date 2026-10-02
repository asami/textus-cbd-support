---
status: working
decision_scope: P104-TYPED-APPROVAL-LIFECYCLE-001
updated_at: 2026-10-01
---

# Internal-model Candidate Approval Lifecycle Contract

This pure boundary derives point-in-time applicability for an already actually
admitted [human approval](internal-model-candidate-human-approval-contract.md).
The runtime-private values, [V2 link codec](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateApprovalSupersessionCodec.scala),
[evaluator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateApprovalLifecycleEvaluator.scala)
and [executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateApprovalLifecycleSpec.scala)
are authored. Validation, independent review, integrated consumer conversion,
Step acceptance and Phase closure remain pending.

## Retained evidence and structural admission

Inputs are original actual human admission, independently admitted current
review, independent expected current rule/provider basis, full original source
inventory, caller-owned live-source observations and optional explicit link.
Pure approval/review `validateAdmission` helpers check value grammar, supported
carrier syntax, exact selections/dependencies, complete subject and semantic
cross-binding consistency. Captured metadata must agree with admitted models.
The package-owned required lifecycle-state field keeps its existing ASCII
descriptive-token grammar, not a new enum. Null/malformed/contradictory graph
values fail structurally, not as ordinary drift. No helper manufactures Verified
captures, caches/compares canonical bytes or reauthenticates a person. Original
admission must already have checked actual independent human input; stored
records cannot replace it.

## Exact report, states and invalidation schema

The five state tokens remain `approved`, `rejected`, `changes-requested`,
`superseded`, `invalidated`. A report has exactly `state`, `approval`,
`invalidations`, `supersession` and retains original admission/decision/rationale
and ordered duplicate unresolved items. Reevaluation can derive different state
from new supplied evidence; retained history is never rewritten or rebased.

An invalidation has exactly `kind`, `artifactReference`, `changedDimensionNames`,
`missingDimensionNames`. Its optional affected artifact is a full typed
reference, not a bare ID or content token. Basis reasons use None and an empty
missing vector. The closed ordered kind vocabulary is:

1. `CandidateBasisChanged`
2. `ReviewBasisChanged`
3. `SemanticDiffBasisChanged`
4. `ReviewSubjectChanged`
5. `ScopeChanged`
6. `RulesChanged`
7. `ProvidersChanged`
8. `SourceChanged`
9. `SourceIncomplete`
10. `SourceUnavailable`
11. `SourceUnauthorized`
12. `SourceMalformed`
13. `SourceAmbiguousOrConflicting`
14. `MissingBaseline`

## Complete current-basis dimensions

Every representable dimension survives without short-circuiting:

- Candidate full artifact ID/revision/role, logical ID/revision, model identity.
- Review full artifact ID/revision/role and logical ID/revision.
- Diff full artifact ID/revision/role and logical ID/revision.
- Subject ID/revision, stable package namespace/project/package ID, three scope
  fields and complete ordered full input references.
- All three approval/current-review scope fields.
- Original review versus independent expected rules/providers ID membership and
  versions; current review versus the same independent expected basis.

Dimension names identify typed fields, for example
`candidateArtifactReference.artifactRevision`, `candidateReference.recordRevision`,
`subject.subjectRevision`, `subject.packageReference.packageId`,
`subject.artifacts`, `originalReview.rules.ids` and
`currentReview.providers[provider-id].version`. Malformed selected roles
contradict admission and reject before drift; full-reference comparison still
retains role as a dimension, never bare-ID matching.

Carrier revision, valid lifecycle token, unselected approval/review/resume controls
and transport-only additions do not change an unchanged historical subject.
The complete-subject rule still includes every semantic contribution; producers
cannot hide changed inputs behind a smaller subject.

## Source inventory and source-owner outcomes

Snapshots exactly cover every original carrier source-snapshot entry, including
unique IDs, full reference, path, required flag, ordered full dependencies and
presence. Present bytes require strict source-snapshot V2 grammar. Optional
absence retains metadata and derives `MissingBaseline`; required absence and
metadata contradictions fail structurally. This is metadata correspondence,
not whole-file equality, captured-byte caches or content mutation detection.

Live map keys are known inventory artifact-ID locators only; they are not
control identities. Null keys/values and unknown keys reject. Missing known
observation means `SourceUnavailable`. Every present baseline uses the existing
`InternalModelSourceSnapshotFreshness.compare` owner:

| Source-owner status | Lifecycle result |
| --- | --- |
| Unchanged | No source invalidation. |
| Changed | SourceChanged with changed dimensions. |
| Incomplete | SourceIncomplete with BOTH changed and missing dimensions. |
| Unavailable | SourceUnavailable. |
| Unauthorized | SourceUnauthorized. |
| Malformed | SourceMalformed. |
| AmbiguousOrConflicting | SourceAmbiguousOrConflicting. |

Declared source authority, identity, available revision and both CML paths
remain independently observable. Unknown baseline/observed versions retain
`baseline.source.revision` and/or `observed.source.revision` in missing dimensions,
even when identity/authority/path changed. No artifact/logical revision replaces
a missing source version. Ordinary raw source/CML payload is decoded evidence,
not identity/freshness/comparison permission. Undeclared same-reference content
change is not detected.

Basis reasons follow closed kind order; source reasons sort by kind then
unsigned UTF-8 artifact ID. Existing deterministic changed/missing dimension
order survives. Snapshot and map insertion order cannot affect a report.

## Exact portable supersession V2

Root fields are exactly `predecessorApproval`, `successorApproval`, `profile`,
`schemaVersion`; profile is `ccdm-candidate-approval-supersession-v2`, version
`2.0`. Both selections are complete `ArtifactReference(artifactId, artifactRevision,
role)` with Approval role and must differ. Package-owned ASCII ID syntax and
lexical positive Long revisions retain their domain. Strict ordinary JSON accepts
harmless key order/whitespace/LF variation, rejects BOM/duplicates/missing-extra
members/wrong-null types/invalid Unicode/revisions/unsupported formats/trailing
data. Deterministic formatting is not reader equality. There is no canonical-byte
field, hash, cache, V1 reader or inferred version. `validateValue` checks the same
structural grammar.

Caller supplies the link AND separately actually admitted successor. Predecessor
full reference equals original external approval; successor full reference equals
the separate successor external approval. Self-links and same-ID wrong
revision/role cannot satisfy this binding.

Successor must retain the same stable subject package reference, all three scope
fields, candidate logical ID and candidate-model identity. Its candidate logical
revision is at least the predecessor's; at the same logical revision the complete
candidate artifact reference must be equal. With the same approval logical ID,
successor logical revision is strictly higher. Distinct approval logical IDs have
no numeric ranking: only explicit exact link selects replacement. Artifact, subject
and carrier revisions are not logical-revision proxies. A higher logical candidate
revision may bind a changed artifact; a genuine successor may retain any decision.

## Precedence and boundary

A valid explicit link derives Superseded while preserving ALL basis/source
invalidations and both admissions. Without it, any reason derives Invalidated;
otherwise original human decision maps directly to its corresponding state.
Omitted link never triggers latest/sibling/rank/timestamp inference. Successor
applicability requires separate evaluation with its own evidence.

This boundary reads no filesystem/source/provider/Git/clock/network and writes
nothing. It authenticates nobody, allocates no versions, infers no history,
selects/stores no links, reconciles nothing and grants no CML application.
Continuation/action authority, actual fresh-process recovery and parent
assurance gates remain separately pending.
