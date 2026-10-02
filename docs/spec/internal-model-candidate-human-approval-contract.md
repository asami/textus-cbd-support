---
status: working
decision_scope: P104-TYPED-APPROVAL-LIFECYCLE-001
updated_at: 2026-10-01
---

# Internal-model Candidate Human Approval Contract

This contract defines the runtime-private typed V2 human-approval boundary.
The [typed-control contract](internal-model-typed-control-contract.md) governs
shared reference domains, producer revisions and removal of control hashes.
The [review-binding contract](internal-model-candidate-review-binding-contract.md)
owns the complete subject and independent rule/provider evidence. The model,
strict codec, validator and [executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateHumanApprovalValidatorSpec.scala)
are authored. Validation, independent review, integrated consumer conversion,
Step acceptance and Phase closure remain pending.

## Exact V2 schemas

The root has exactly `approval`, `profile`, `schemaVersion`; profile is
`ccdm-candidate-human-approval-v2`, version `2.0`. There is no V1 reader,
compatibility adapter, inferred schema or default revision.

`approval` has exactly seven fields: `actor`, `approvalReference`, `basis`,
`decision`, `provenance`, `rationale`, `unresolvedItems`.

- `actor` has exactly `kind`, `identity`, `role`; kind is exactly `human`.
- `approvalReference` is `RecordReference(recordId, recordRevision)`, separate
  from the external approval artifact version.
- `decision` is exactly `approved`, `rejected`, `changes-requested`.
- `provenance` is the four-field `SemanticSource(authority, identity, locator,
  revision)`; authority is exactly `human-decision`. Locator/revision fields
  are required and their values are null or nonblank strings.
- `rationale` is nonblank text.
- `unresolvedItems` is a required ordered nonblank-string array retaining
  duplicates. Approved may retain unresolved items; an empty array proves no absence.

`basis` has exactly nine fields:

| Field | Type / meaning |
| --- | --- |
| `candidateArtifactReference` | Exact Projection ArtifactReference. |
| `candidateReference` | Exact candidate RecordReference. |
| `candidateModelIdentity` | Existing candidate-model identity. |
| `reviewArtifactReference` | Exact selected Validation ArtifactReference. |
| `reviewReference` | Exact selected review RecordReference. |
| `subject` | Complete review-owned ReviewSubject. |
| `scope` | Existing three-part semantic scope. |
| `semanticDiffArtifactReference` | Exact Projection ArtifactReference. |
| `semanticDiffReference` | Exact semantic-diff RecordReference. |

`ArtifactReference` has exactly `artifactId`, `artifactRevision`, `role`;
`RecordReference` exactly `recordId`, `recordRevision`. The subject reuses the
review-owned five-field grammar: `subjectId`, `subjectRevision`,
`packageReference`, `scope`, `artifacts`. The stable package reference has
`packageId`, `projectNamespace`, `projectId`. Scope has exactly
`componentIdentity`, `projectionContextIdentity`, `selectedUseCaseElementIdentity`.
Subject input references retain their complete ordered ID/revision/role values;
approval introduces no competing subject parser or carrier-inventory selection.

Artifact IDs retain package-owned ASCII token syntax and closed roles. Logical
record IDs, subject IDs and human identity/prose retain nonblank valid Unicode
scalars verbatim, without trimming or normalization. Unicode-whitespace-only
strings reject, including non-null nullable provenance values. Artifact, logical
record and subject revisions are explicit lexical positive JSON Long integers:
strings, zero, negatives, fractions, exponents, null and overflow reject;
`Long.MaxValue` is valid. Carrier, subject, artifact and logical revisions
are distinct domains. Missing source revision stays unknown; no control revision
substitutes for it.

## Strict ordinary JSON and pure structural values

Readers require exact closed objects and strict UTF-8 JSON. BOM, duplicate
members, missing/extra fields, wrong/null nonnullable types, unsupported formats,
invalid Unicode scalar syntax, invalid declared values and trailing data reject.
Harmless key order, insignificant whitespace, equivalent valid escapes and an
absent terminal LF are accepted. Writers emit deterministic keys and one LF as
formatting only; byte-identical re-encoding is not admission.

The in-memory record has exactly `approval`, `profile`, `schemaVersion`, without
canonical bytes, hash, digest, checksum or content token. `validateValue` checks
the same grammar and null/malformed object graphs without filesystem capture.

## Actual admission and independent authority

The caller explicitly supplies a full Approval selection, a full Validation
review selection, independently admitted rules/providers and separately
attributable actual human input. One actual package capture resolves present
selections. Approval dependencies equal exactly `Vector(reviewArtifactReference)`,
including ID, revision, role and order. Missing, optional absent, wrong role/
revision/dependency, ambiguous, malformed and contradictory values reject.
Unselected approval siblings remain inert even with unknown profiles; no latest,
ordinal, timestamp, content or provider-state heuristic selects them.

Review admission checks its actual semantic predecessors, source witnesses,
complete subject, typed dependencies, scopes and targets against independently
supplied rule/provider ID/version evidence. Original CCDM conditions, nullable
facets, binary CML, all eight projections, evidence selection and ordered
duplicate limitations remain authoritative.

Independent human input must be valid and exactly equal to every decoded
stored semantic field. Then all nine basis fields equal the actually admitted
review/candidate/diff/subject basis. Matching stored input cannot authenticate
itself; missing independent input rejects. Provider `approved` evidence is not
a human decision. Actor/provenance are attributable claims, not signatures or
person authentication.

The admission has exactly `record`, `approvalArtifactReference`,
`approvalArtifactPackageRelativePath`, `reviewAdmission`. Pure
`validateAdmission` checks retained grammar, full external selection metadata,
carrier syntax and semantic cross-binding consistency. It creates no simulated
Verified capture, reopens no path, retains no byte cache, compares no canonical
bytes and does not repeat human authentication. Only actual admission with
independent human input establishes the retained handoff consumed by lifecycle.

## Boundary

Grammar, human input, exact reference/version, subject/scope, independent
execution evidence and captured-metadata contradictions fail with structured
operation results. This boundary fetches no provider/provenance, consults no
network/Git/clock, performs no post-capture reread, mutates no artifact and
grants no live currentness, supersession or CML permission. Captured evidence
is point-in-time; a copied carrier needs fresh admission with the same explicit
references and independent human input, not path/process-local identity.

Producer references/revisions neither authenticate claims nor detect undeclared
same-ID/revision payload changes. The [lifecycle contract](internal-model-candidate-approval-lifecycle-contract.md)
separately classifies supplied currentness without rewriting the original decision.
