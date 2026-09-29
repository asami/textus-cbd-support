---
status: draft
decision_scope: P10-31A
updated_at: 2026-09-29
---

# Internal-model Semantic-Diff Contract

This normative P10-31A contract persists and reconstructs an admitted Phase 9
semantic diff without deriving any semantic delta from CML or Git text. It is
implemented by the [semantic-diff value model](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiff.scala),
[codec](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiffCodec.scala),
[validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiffValidator.scala), and
[executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelSemanticDiffValidatorSpec.scala).
The [Internal-model Package Contract](internal-model-package-contract.md),
[Candidate CML Projection Contract](internal-model-candidate-cml-projection-contract.md),
[Source Snapshot Contract](internal-model-source-snapshot-contract.md), and Phase 9
[Candidate Design and Semantic-Diff Integration Contract](candidate-design-semantic-diff-integration-contract.md)
retain their respective authority.

## Authority and admission

The only admitted content profile is `ccdm-semantic-diff-v1` with
`schemaVersion` `1.0`, carried as a `projection` artifact. It is selected from
one already captured, project-bound package inventory together with exactly one
candidate projection. Its manifest dependency set is exactly the selected
candidate artifact ID. The diff retains its own artifact ID, raw-byte digest,
and package-relative path only in the external admission context; it serializes
neither its own hash nor package digest.

The root has exactly `candidateArtifactId`, `candidateArtifactSha256`,
`candidateIdentity`, `candidateModelIdentity`, `candidateRevision`, `profile`,
`schemaVersion`, `scope`, `semanticDiffIdentity`, `semanticDiffRevision`, and
`targets`. `scope` has exactly `componentIdentity`,
`projectionContextIdentity`, and `selectedUseCaseElementIdentity`. Candidate
identity/model/revision and scope equal the decoded selected candidate exactly;
the diff identity is a separately supplied nonblank value and revision is a
canonical positive `Int`.

## Closed content grammar

Bytes are UTF-8 JSON without BOM, duplicate members, insignificant whitespace,
or excess final bytes. Every object key is ascending unsigned UTF-8 byte order
and bytes end in exactly one LF. Decoder re-encoding must equal supplied bytes.
All object depths are closed. Digests are lowercase `sha256:` plus 64 hex
characters and positive revisions use canonical integer syntax.

`targets` is nonempty, exactly the complete candidate target set, and strictly
ascending by unsigned UTF-8 `targetId`. A target has exactly `entries`,
`patchTrace`, and `targetId`; entries are nonempty, strictly sorted by entry
ID, and globally unique. A mapped entry has exactly `entry`, `mappingId`, and
`semanticIdentityKind`. Its entry has exactly `action`, `after`, `attribution`,
`before`, `candidateModelId`, `category`, `component`, `condition`, `context`,
`id`, `limitations`, `patchId`, `relationship`, `stableTieKey`, and `subject`.
`component` and `context` are exact identity values, and nullable
`before`/`after`/`stableTieKey` preserve `None` as JSON null. An absent
before/after requires explicit absence, never mere unavailability,
unauthorization, or redaction.

`patchTrace` has exactly `attribution`, `baseDigest`, `cmlLocator`, `cmlOwner`,
`component`, `condition`, `context`, `id`, `limitations`, `proposedDigest`, and
`stableTieKey`. Attribution has exactly `authorityScope`, `sourceId`, and
`sourceLocator`; condition has exactly `ambiguity`, `authorization`,
`availability`, `conflict`, `explicitAbsence`, `limitations`,
`malformedEvidence`, `redaction`, and `staleness`. Required strings are
nonblank without byte normalization. Optional condition facets are null or
nonblank. Limitation vectors preserve order and multiplicity.

## Binding and rejection

Each target resolves once to the selected candidate. Its patch ID, scope,
baseline/proposed raw-byte digests, and source authority bind exactly. A present
baseline locator must match; a null baseline locator retains the supplied
nonblank patch locator independently. Every entry binds to that patch, selected
candidate model, and scope. Its mapping exists in that exact target, retains its
explicit kind, and names that mapping's semantic identity. Element and
relationship identities are not inferred from opaque strings.

Unknown or wrong profiles, schema versions, fields, dependencies, target sets,
array ordering, mappings, identities, digests, locators, ownership, malformed
UTF-8, duplicate members, and noncanonical bytes reject. Recognized but
nonrequested families are skipped only for selection; they are not semantically
admitted by that skip.

Admission reconstructs the Phase 9 typed `CandidateDesignSemanticDiffEntry`
and `CandidateDesignProposedCmlPatchTrace` values. It does not parse, read,
write, generate, apply, or compare CML; reopen package paths; access a provider
or database; establish freshness, patch applicability, semantic correctness,
compatibility, approval, review acceptance, Git acceptance, canonical facts, or
Phase 10.4 rehydration.
