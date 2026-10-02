---
status: target
decision_scope: P10-31 / P104-TYPED-SEMANTIC-DIFF
updated_at: 2026-10-01
---

# Internal-model Semantic-Diff Contract

This normative current target contract persists and reconstructs supplied Phase 9
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

The [Typed Control Contract](internal-model-typed-control-contract.md) owns the
distinct public artifact/record reference domains used by the shared patch trace.
The basis is package/source V2, candidate V2 and realization/continuity V3 only.
Original P10-31A implementation and proof are historical. Current authoring
requires parent validation and independent review; it establishes neither Step
acceptance, all-consumer migration nor Phase 10.4 closure.

## Authority and admission

The only admitted content profile is `ccdm-semantic-diff-v2` with
`schemaVersion` `2.0`, carried as a `projection` artifact. It is selected from
one already captured, project-bound package inventory together with exactly one
candidate projection. Its dependency vector is exactly one selected candidate
`ArtifactReference`, including ID, positive Long revision and `projection` role.
The diff's own artifact reference and package-relative path remain external.
Admission has exactly `diff`, `diffArtifactReference`, `diffPackageRelativePath`
and `candidateAdmission`; there is no digest or cached content.

The root has exactly eight fields: `candidateArtifactReference`,
`candidateReference`, `candidateModelIdentity`, `profile`, `schemaVersion`,
`scope`, `semanticDiffReference` and `targets`. `scope` has exactly `componentIdentity`,
`projectionContextIdentity`, and `selectedUseCaseElementIdentity`. Candidate
artifact reference, logical candidate reference, model identity and complete
scope equal the selected candidate exactly. `candidateReference` and
`semanticDiffReference` are independent closed `RecordReference` values with
nonblank valid Unicode IDs and explicitly supplied positive lexical JSON Long
revisions. Neither revision is inferred from another record, artifact, carrier,
source version or payload. `candidateArtifactReference` has exactly
`artifactId`, `artifactRevision` and role `projection`; an artifact ID follows
the package's ASCII token grammar. No raw ID, scalar reference or old field is
admitted. All references are required and closed; signs, leading zeros,
fractions, exponents, strings, null and overflow reject for revision values.

## Closed content grammar

Bytes are strict UTF-8 JSON without BOM, duplicate members, malformed Unicode
or trailing non-JSON data. All object depths are closed. Readers admit harmless
member order, insignificant whitespace and equivalent escaping. The ordinary
deterministic writer may sort object keys and emit terminal LF; neither
re-encoding equality, a cache nor whole-byte comparison is an admission rule.
Writer output preserves supplied semantic arrays, including malformed ordering
or duplicates; it does not normalize a semantic defect into acceptance.

`targets` is nonempty, exactly the complete ordered candidate target IDs, and strictly
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

`patchTrace` has exactly eleven fields: `attribution`,
`baselineArtifactReference`, `cmlLocator`, `cmlOwner`, `component`, `condition`,
`context`, `id`, `limitations`, `proposedContentReference` and `stableTieKey`.
The baseline is a closed `ArtifactReference` with exact `source-snapshot` role;
proposed content is a closed `RecordReference`. Attribution has exactly `authorityScope`, `sourceId`, and
`sourceLocator`; condition has exactly `ambiguity`, `authorization`,
`availability`, `conflict`, `explicitAbsence`, `limitations`,
`malformedEvidence`, `redaction`, and `staleness`. Required strings are
nonblank valid Unicode without normalization. Optional condition facets are null or
nonblank. Limitation vectors preserve order and multiplicity.

## Binding and rejection

Null outer, candidate, selected metadata or bytes reject as structured
`operationInvalid` without a crash or partial admission. The current candidate
validator validates the complete captured package reference, schema/carrier
revision and deterministic inventory first. The selected diff must resolve to
exactly one entry in that same capture with equal entire artifact reference,
path, required flag, ordered dependencies and presence. There is no filesystem,
manifest, source or provider reread after capture. Duplicated dependencies,
changed roles/revisions and contradictory selected/context metadata reject.

Each target resolves once to the selected candidate. Its patch ID, scope,
baseline artifact reference, proposed content reference and source authority
bind exactly to that target's supplied values. A present
baseline locator must match; a null baseline locator retains the supplied
nonblank patch locator independently. Every entry binds to that patch, selected
candidate model, and scope. Its mapping exists in that exact target, retains its
explicit kind, and names that mapping's semantic identity. Element and
relationship identities are not inferred from opaque strings.

Unknown or wrong profiles, schema versions, fields, dependencies, target IDs,
array ordering, mappings, typed identities/revisions/roles, locators, ownership,
malformed UTF-8 and duplicate members reject. Old profiles, hash fields and
bare identities have no compatibility reader or inferred default. Recognized but
nonrequested families are skipped only for selection; they are not semantically
admitted by that skip.

Admission reconstructs the Phase 9 typed `CandidateDesignSemanticDiffEntry`
and `CandidateDesignProposedCmlPatchTrace` values. It does not parse, read,
write, generate, apply, or compare CML; reopen package paths; access a provider
or database; establish freshness, patch applicability, semantic correctness,
compatibility, approval, review acceptance, Git acceptance, canonical facts, or
Phase 10.4 rehydration. References are producer-owned provenance, not
authentication or proof of an undeclared payload change. Equal or different
ordinary CML payload bytes establish no binding, revision or semantic delta.
A source revision of `None` remains unknown beside known control versions.
