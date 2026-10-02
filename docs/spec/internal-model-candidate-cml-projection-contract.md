---
status: target
decision_scope: P10-30 / P104-TYPED-CANDIDATE-CML
updated_at: 2026-10-01
---

# Internal-model Candidate CML Projection Contract

This normative target contract defines the closed candidate-content profile
`ccdm-candidate-cml-projection-v2` / `schemaVersion="2.0"`. Its companion is
the [Internal-model Candidate CML Projection
Design](../design/internal-model-candidate-cml-projection.md). The
[Internal-model Package Contract](internal-model-package-contract.md) remains
authoritative for manifest, inventory, typed reference, and dependency mechanics; the
[Internal-model Semantic Realization
Contract](internal-model-semantic-realization-contract.md) remains the CCDM
evidence ledger; and the [Internal-model Projection Continuity
Contract](internal-model-projection-continuity-contract.md) remains
authoritative for continuity content. The Phase 9 [Candidate Design and
Semantic-Diff Integration
Contract](candidate-design-semantic-diff-integration-contract.md) remains
authoritative for caller-supplied candidate, patch, and semantic-diff evidence.

The [Typed Control Contract](internal-model-typed-control-contract.md) owns
explicit version/reference semantics. The [Package Contract](internal-model-package-contract.md)
and [Source Snapshot Contract](internal-model-source-snapshot-contract.md) own
package/source V2. [Realization](internal-model-semantic-realization-contract.md)
and [Continuity](internal-model-projection-continuity-contract.md) own the V3
evidence basis. Original P10-30A/B implementation and proof are historical;
current authoring does not establish validation, acceptance or Phase closure.

## 1. Authority and operation boundary

One candidate belongs to exactly one existing realization scope:
`componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`. It retains the accepted Phase 9 CCDM
meaning, the Phase 10.2 realization ledger, the selected V3 continuity
binding, and their complete attribution and conditions. It creates neither a
second domain model nor an identity inferred from labels, paths, hashes, source
order, providers, diagram geometry, view copies, or lexical similarity.

Inputs are exact caller-supplied candidate/proposal/patch identities and bytes.
This profile records an explicit proposed CML transformation, but does not
parse, generate, normalize, validate, apply, read, fetch, or otherwise
interpret CML. It does not infer a semantic diff, approve a change, accept it
in Git, or mutate a package, filesystem, database, or canonical source.
Encoding returns ordinary deterministic JSON bytes to its caller and performs no write.

## 2. Closed JSON and explicit references

The content is strict UTF-8 JSON: no BOM, malformed Unicode, duplicate member,
trailing data, unknown, extra or missing member at any defined object depth.
Readers admit harmless object-key order, insignificant whitespace and equivalent
escaping. Writers may emit deterministic keys and a terminal LF on demand.
There is no cached canonical content, whole-file comparison, re-encoding
acceptance condition, hash or payload-derived control token.

The root has exactly these eight keys, displayed in writer order:

```text
candidateModelIdentity, candidateReference, continuityArtifactReference,
profile, realizationArtifactReference, schemaVersion, scope,
targets
```

`profile` is exactly `"ccdm-candidate-cml-projection-v2"` and
`schemaVersion` is exactly `"2.0"`. `candidateReference` is a required
producer-allocated `RecordReference(recordId, recordRevision)`.
`candidateModelIdentity` is the
supplied exact Phase 9 candidate-model identity, not a realization identity,
semantic subject, or control reference. Its meaning is not inferred from
`candidateReference`. A logical revision is an explicit positive lexical JSON
`Long`, independent of every artifact and carrier revision. No identity or
revision is generated or defaulted by this consumer.

`scope` has exactly `componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`, each exactly equal to the selected
realization scope. `realizationArtifactReference` and
`continuityArtifactReference` are exact selected present
`ArtifactReference(artifactId, artifactRevision, role)` values, with roles
`realization` and `projection` respectively. All reference objects are required
and closed. Record IDs are nonblank valid Unicode; artifact IDs use the package's
ASCII grammar. Revisions fit positive `Long` without signs, leading zeros,
fractions, exponents, strings, null, overflow, inference or defaults.

The separately captured package reference, carrier revision and inventory provide
declared control provenance. They prove neither authenticity, live freshness nor
undeclared payload mutation. Later history/invalidation remains separately owned.

## 3. Targets and exact baseline/proposed bytes

`targets` is a nonempty array, unique and ascending UTF-8-byte sorted by
`targetId`. Each target has exactly these keys:

```text
baselineArtifactReference, effects, mappings, patchIdentity, projectRelativePath,
proposedContent, source, targetId
```

`targetId` and `patchIdentity` are nonempty exact caller/owner-supplied CML
target and Phase 9 proposed-patch identities. Both are unique across the
candidate, as are `projectRelativePath` and baseline artifact IDs, even when
different baseline revisions are supplied. The target
set is explicitly supplied, never discovered from path, layout, or order.

`baselineArtifactReference` is an exact `ArtifactReference` with role
`source-snapshot`. It selects one present captured source snapshot whose
validated `snapshotKind` is exactly `cml-baseline`. `source` has exactly
`authority`, `identity`, `locator`, and `revision`, and equals that
snapshot's closed source envelope exactly. `projectRelativePath` equals its
`basis.projectRelativePath` and follows the existing safe project-relative path
grammar. Authority and identity are nonblank valid Unicode; locator and revision
are required nullable nonblank valid Unicode strings. A missing source version
remains `None` even with known positive artifact/logical versions. A baseline
path identifies a source file only; it proves no
semantic anchor or current freshness.

`proposedContent` has exactly `contentReference`, `byteLength`, and
`rawBytesBase64`. `contentReference` is a required closed `RecordReference`
with an independently producer-allocated positive `Long` revision.
Its bytes use padded RFC 4648 Base64 without whitespace or an alternate
alphabet or nonzero unused pad bits; `byteLength` is the nonnegative lexical
JSON `Long` matching decoded length. These are ordinary payload-representation
checks, never identity, revision, semantic change, currentness or permission
controls. No newline conversion, Unicode normalization, CML parsing,
or rewriting occurs. Equal or unequal baseline/proposed bytes do not establish
or remove a semantic change.

Only existing present-file CML targets with exact admitted baselines are in
scope. New or missing targets, synthetic zero-byte absence baselines, moves,
and deletions are not admitted. Empty proposed raw content, when supplied, is
only bytes; it does not assert syntactic validity, deletion, or safe
application.

## 4. Explicit semantic mappings

Each target's nonempty `mappings` array is uniquely sorted by `mappingId`.
Each mapping has exactly these keys:

```text
canonicalAssertionIds, cmlAnchor, cmlSemanticIdentity, conditionIds,
enrichmentAssertionIds, mappingId, semanticIdentity, semanticIdentityKind
```

`mappingId` is nonempty and unique across the candidate.
`semanticIdentityKind` is exactly `element` or `relationship`, and
`semanticIdentity` resolves to that exact kind in the selected realization,
even when opaque strings are shared by both kinds. The sorted, unique
`canonicalAssertionIds`, `enrichmentAssertionIds`, and `conditionIds` arrays
each equal the target's complete corresponding realization arrays. They cannot
omit conditions, promote enrichment, choose a witness winner, or replace source
links. The realization remains the evidence sidecar and is not copied or
rewritten.

`cmlSemanticIdentity` and `cmlAnchor` are nonempty exact caller-supplied
proposed-CML destination identity and bounded anchor. They are neither labels
nor evidence that baseline or proposed text parses into the supplied semantics.
No CML search establishes either value. Multiple CCDM subjects may map to one
CML destination through distinct mapping IDs; no Mono-to-Entity or
Koto-to-Event one-to-one conversion is imposed.

## 5. Expected compatibility and migration effects

Each target's nonempty `effects` array contains at least one compatibility and
one migration record and is uniquely sorted by `effectId`. Each record has
exactly these keys:

```text
assessment, detail, effectId, kind, mappingIds, sourceReferenceId
```

`effectId` is nonempty and unique across the candidate. `kind` is exactly
`compatibility` or `migration`. A compatibility `assessment` is exactly
`unchanged`, `compatible`, `breaking`, or `unknown`; a migration `assessment`
is exactly `not-required`, `required`, or `unknown`. `detail` is nonempty
exact supplied expected-effect text. `mappingIds` is nonempty, sorted, unique,
and names mappings in the same target. `sourceReferenceId` resolves exactly to
one retained realization source reference, whose complete attribution and
conditions remain in the sidecar.

Effects are caller-supplied, attributed expectations, not calculated
compatibility, source-owner acceptance, actual migration proof, or a new
canonical assertion. Unknown and disagreeing expectations remain visible;
multiple distinct attributed expectations can coexist without a
first/last/newest-source winner. Admission checks only shape and references,
not whether source prose or CML bytes prove an assessment.

## 6. One captured inventory and exact profile selection

Candidate admission obtains package context and selected bytes through one
existing project-bound manifest/filesystem/inventory pass. It selects
exactly one present realization, exactly one present continuity binding,
exactly one present candidate projection, and the exact present source snapshots
required by those artifacts. It does not reopen mutable artifact paths or
reparse the manifest. Realization and continuity retain their accepted
validators and current V3 content semantics.

Before decoding or iterating semantic values, handoff/context/continuity/
realization/source/candidate captures and inventory entries must be present.
References, roles, IDs, positive revisions, safe paths, bytes and dependencies
must be valid. Dependency IDs are unique, ascending and non-self-referential;
every inventory dependency resolves its exact version/role and declared presence.
Every selected candidate, continuity, realization and source capture matches
exactly one context entry's full reference, path, required flag, dependencies and
presence. Contradictory ID/revision/role metadata rejects with structured
`operationInvalid`, rather than a crash or partial model. No duplicate metadata
is erased by set conversion.

Both continuity and candidate artifacts retain `role=projection`; this profile
adds no role. Content families
are selected only by recognizable exact `profile`/`schemaVersion` pairs, never
by path, artifact ID, encounter order, or first/last match. The continuity
family is `ccdm-projection-binding-v3`/`3.0`; the candidate family is
`ccdm-candidate-cml-projection-v2`/`2.0`, with realization V3 and package/source V2.
Old and mismatched pairs reject without a reader, adapter, alias, inferred version
or fallback.

Every present projection considered by this selection has strict JSON
and a recognized exact pair. Unknown or malformed pairs fail closed and are
never fallbacks. Each requested family requires exactly one artifact; zero or
duplicate family matches reject. A
recognized nonselected family is skipped only for selection and is not
semantically accepted without its own validator. A current single V3 continuity
artifact retains its current V3 semantic values, witnesses, role table, eight
constructors, and failure semantics unchanged.

The candidate `dependsOn` array equals exactly the ascending unique selected
realization, continuity and every target baseline `ArtifactReference`, without
extras, omissions or revision/role substitutions. Continuity depends on that
realization, and its version pair remains the accepted realization pair. Later
profiles require their own explicit later Step; this contract recognizes no
wildcard profile.

## 7. Admission matrix and non-claims

Admission rejects wrong or unknown schema or fields; malformed UTF-8/JSON or
invalid payload representation; absent or duplicate candidate/continuity selection; wrong dependency,
scope, realization, or binding identity; missing, non-CML, or wrong baseline;
source-envelope, path, length, or Base64 mismatch; duplicate target,
patch, baseline, path, mapping, or effect identities; unknown, cross-scope, or
wrong-kind semantic references; incomplete, swapped, or promoted link arrays;
unresolved effect source/mapping IDs; unsupported kind/assessment; and a
missing required target, mapping, compatibility, or migration record. It makes
no repair, identity reconstruction, hidden winner, or partial successful
candidate.

Harmless JSON object-key order, insignificant whitespace, and equivalent escaping
remain admissible; strict Base64 spelling and decoded payload length remain required.

The current executable authoring matrix must retain semantic writer roundtrips,
portable one-pass package reopening, current-family-independent V3 continuity,
strict old-family rejection, multi-target
baseline/proposed-byte traceability, label/order independence where labels are
not identity inputs, and the stated fail-closed cases. Its property-based tests
exercise supplied bytes, opaque identities, independent Long revisions,
required reference shapes, exact captured bindings, strict rejection and harmless
JSON presentation, not this
document's prose.

Candidate admission is not CML syntax/semantic validation, safe application,
freshness, a computed semantic diff, actual compatibility/migration, human
approval, Review acceptance, Git acceptance, rehydration, provider invocation,
publication, or runtime/API/UI exposure. P10-31--P10-34 own their later exact
artifact contracts and proofs. The authored
[Executable Specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateCmlProjectionValidatorSpec.scala)
must retain full canonical/enrichment/condition evidence, kind-aware shared IDs,
all eight projections, arbitrary/empty/equal payload, source and effect attribution,
ordering, selection, missing/optional baseline and exact dependency failures.
Captured deletion/substitution versus fresh admission remains explicit. Fixture
cleanup is confined to the exact newly created subtree beneath
`target/internal-model-candidate-cml/work`, with no-follow walk and `finally` close.
Parent validation, independent review, coordinated downstream migration and Phase
10.4 acceptance/release remain pending. All canonical CML mutation is outside this work.
