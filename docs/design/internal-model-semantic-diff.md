---
status: target
decision_scope: P10-31 / P104-TYPED-SEMANTIC-DIFF
updated_at: 2026-10-01
---

# Internal-model Semantic-Diff Design

The current V2 boundary uses an ordinary deterministic JSON projection to serialize
the exact Phase 9 diff and patch-trace values already admitted with a selected
candidate CML projection. The [semantic-diff contract](../spec/internal-model-semantic-diff-contract.md)
defines grammar; this document records the stable responsibility split.

`InternalModelSemanticDiff` is content, `InternalModelSemanticDiffAdmission` is
the content plus captured external artifact and candidate-package context, and
`InternalModelSemanticDiffValidator` is the one-pass boundary. It consumes the
same V2 package/source, V3 realization/continuity and V2 candidate capture.
The package validator selects candidate and semantic-diff families by exact
headers without changing role vocabulary. The diff uses only
`ccdm-semantic-diff-v2` / schema `2.0`; original P10-31 proof is historical.

The eight root fields are `candidateArtifactReference`, `candidateReference`,
`candidateModelIdentity`, `profile`, `schemaVersion`, `scope`,
`semanticDiffReference` and `targets`. The shared patch's eleven fields are
`attribution`, `baselineArtifactReference`, `cmlLocator`, `cmlOwner`,
`component`, `condition`, `context`, `id`, `limitations`,
`proposedContentReference` and `stableTieKey`. Artifact references carry exact
ID/positive Long revision/role, and logical references carry independently
allocated ID/positive Long revision. These are the existing distinct reference
domains, narrowly made public to support the already public shared patch.

The selected candidate remains the source of complete target membership,
mappings, baseline snapshots, proposed bytes, realization binding, and source
conditions. Diff entries and patch traces are reconstructed as direct Phase 9
typed values rather than converted to summaries or a parallel semantic model.
This preserves opaque identities, multiple conditions, limitations, and
attribution without choosing a winner.

The full candidate capture validates deterministic inventory and exact declared
dependencies. The selected diff then matches one entire inventory entry:
reference, path, required flag, dependencies and presence. Its dependency
vector is exactly the selected candidate reference. Ordered target IDs, patch
identity/scope/baseline/content references, owner/known locator and every
mapping-local entry binding remain equality constrained. An unknown source
locator retains independent supplied patch traceability; an unknown source
revision stays unknown. Capture permits later reconstruction without reopening
paths; it does not assert live-source freshness.

JSON presentation is ordinary transport. Member order, whitespace and
equivalent escaping cannot change meaning or eligibility. Writers preserve
semantic arrays, while readers reject malformed ordering/duplicates and closed
shape violations. There is no hash, content cache, byte-equality protocol,
legacy reader, compatibility adapter or inferred default. Equal or unequal CML
payload is ordinary evidence, not identity, version or permission.

The boundary deliberately admits traceability, not an approved or applicable
change. It neither interprets raw CML/Git bytes nor claims freshness, source
authority, review, approval, compatibility, source mutation, or rehydration.
Later review/approval/invalidation work owns those transitions. Declared control
versions record producer provenance without authenticating a stored claim or
detecting undeclared content changes. Current authoring requires parent
validation, independent review and coordinated remaining consumer migration;
this design grants no Step acceptance or Phase closure.
