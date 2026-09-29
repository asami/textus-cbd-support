---
status: draft
decision_scope: P10-30A
updated_at: 2026-09-29
---

# Internal-model Candidate CML Projection Contract

This normative P10-30A contract defines the closed candidate-content profile
`ccdm-candidate-cml-projection-v1` / `schemaVersion="1.0"`. Its companion is
the [Internal-model Candidate CML Projection
Design](../design/internal-model-candidate-cml-projection.md). The
[Internal-model Package Contract](internal-model-package-contract.md) remains
authoritative for manifest, inventory, digest, and dependency mechanics; the
[Internal-model Semantic Realization
Contract](internal-model-semantic-realization-contract.md) remains the CCDM
evidence ledger; and the [Internal-model Projection Continuity
Contract](internal-model-projection-continuity-contract.md) remains
authoritative for continuity content. The Phase 9 [Candidate Design and
Semantic-Diff Integration
Contract](candidate-design-semantic-diff-integration-contract.md) remains
authoritative for caller-supplied candidate, patch, and semantic-diff evidence.

P10-30B owns the canonical codec, one-pass admission, and executable proof.
This document defines neither a parser nor an executable success claim.

## 1. Authority and operation boundary

One candidate belongs to exactly one existing realization scope:
`componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`. It retains the accepted Phase 9 CCDM
meaning, the Phase 10.2 realization ledger, the selected V1/V2 continuity
binding, and their complete attribution and conditions. It creates neither a
second domain model nor an identity inferred from labels, paths, hashes, source
order, providers, diagram geometry, view copies, or lexical similarity.

Inputs are exact caller-supplied candidate/proposal/patch identities and bytes.
This profile records an explicit proposed CML transformation, but does not
parse, generate, normalize, validate, apply, read, fetch, or otherwise
interpret CML. It does not infer a semantic diff, approve a change, accept it
in Git, or mutate a package, filesystem, database, or canonical source.
Encoding returns canonical bytes to its caller and performs no write.

## 2. Closed canonical bytes and root

The content is canonical UTF-8 JSON: no BOM, duplicate member, unknown, extra,
or missing member at any defined object depth; UTF-8-byte-sorted object keys;
no insignificant whitespace; and exactly one final LF. A consumer parses
without repair and accepts only byte-for-byte canonical re-encoding.

The root has exactly these keys, in canonical order:

```text
candidateIdentity, candidateModelIdentity, candidateRevision,
continuityArtifactId, profile, realizationArtifactId, schemaVersion, scope,
targets
```

`profile` is exactly `"ccdm-candidate-cml-projection-v1"` and
`schemaVersion` is exactly `"1.0"`. `candidateIdentity` is a stable
caller-allocated artifact/candidate identity. `candidateModelIdentity` is the
supplied exact Phase 9 candidate-model identity, not a realization identity,
semantic subject, or hash. `candidateRevision` is an integer from 1 through
2147483647 and independently versions this candidate. This consumer generates
neither identity.

`scope` has exactly `componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`, each exactly equal to the selected
realization scope. `realizationArtifactId` and `continuityArtifactId` identify
the selected present manifest artifacts exactly.

The candidate serializes neither its own artifact hash nor a current package
digest. Package identity, revision, digest, inventory IDs/paths, and selected
artifact hashes remain verified external manifest context. Persisted candidate
bytes therefore do not prove source authenticity or freshness. A later history
comparison or invalidation is P10-34 work; the same candidate identity and
revision are never silently overwritten or recast here.

## 3. Targets and exact baseline/proposed bytes

`targets` is a nonempty array, unique and ascending UTF-8-byte sorted by
`targetId`. Each target has exactly these keys:

```text
baselineArtifactId, effects, mappings, patchIdentity, projectRelativePath,
proposedContent, source, targetId
```

`targetId` and `patchIdentity` are nonempty exact caller/owner-supplied CML
target and Phase 9 proposed-patch identities. Both are unique across the
candidate, as are `projectRelativePath` and `baselineArtifactId`. The target
set is explicitly supplied, never discovered from path, layout, or order.

`baselineArtifactId` selects one present manifest source snapshot whose
validated `snapshotKind` is exactly `cml-baseline`. `source` has exactly
`authority`, `identity`, `locator`, `revision`, and `sha256`, and equals that
snapshot's closed source envelope exactly. `projectRelativePath` equals its
`basis.projectRelativePath` and follows the existing safe project-relative path
grammar. `source.sha256` is the baseline raw-CML-byte digest, not the snapshot
artifact digest. A baseline path identifies a source file only; it proves no
semantic anchor or current freshness.

`proposedContent` has exactly `byteLength`, `rawBytesBase64`, and `sha256`.
Its bytes use padded RFC 4648 Base64 without whitespace or an alternate
alphabet; `byteLength` is the nonnegative decoded length; and `sha256` is
exactly `sha256:` followed by 64 lowercase hexadecimal characters matching the
decoded raw bytes. No newline conversion, Unicode normalization, CML parsing,
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

## 6. One verified inventory and additive profile selection

Candidate admission obtains package context and selected bytes through one
existing project-bound manifest/filesystem/inventory/digest pass. It selects
exactly one present realization, exactly one present continuity binding,
exactly one present candidate projection, and the exact present source snapshots
required by those artifacts. It does not reopen mutable artifact paths or
reparse the manifest. Realization and continuity retain their accepted
validators and V1/V2 content semantics.

Both continuity and candidate artifacts retain `role=projection`; this profile
adds no role and does not reinterpret a manifest V1 field. Content families
are selected only by recognizable exact `profile`/`schemaVersion` pairs, never
by path, artifact ID, encounter order, or first/last match. The continuity
family is `ccdm-projection-binding-v1`/`1.0` or
`ccdm-projection-binding-v2`/`2.0`; the candidate family is
`ccdm-candidate-cml-projection-v1`/`1.0`.

Every present projection considered by this selection has canonical JSON bytes
and a recognized exact pair. Unknown or malformed pairs fail closed and are
never fallbacks. Each requested family requires exactly one artifact; zero or
duplicate family matches, including a V1-plus-V2 continuity pair, reject. A
recognized nonselected family is skipped only for selection and is not
semantically accepted without its own validator. A legacy single continuity
artifact keeps its existing V1/V2 bytes, witnesses, role table, eight
constructors, and failure semantics unchanged.

The candidate `dependsOn` set equals exactly the selected realization artifact
ID, selected continuity artifact ID, and every selected target baseline artifact
ID, without extras or omissions. Continuity continues to depend on that
realization, and its version pair remains the accepted realization pair. Later
profiles require their own explicit later Step; this contract recognizes no
wildcard profile.

## 7. Admission matrix and non-claims

Admission rejects wrong or unknown schema or fields; malformed or noncanonical
bytes; absent or duplicate candidate/continuity selection; wrong dependency,
scope, realization, or binding identity; missing, non-CML, or wrong baseline;
source-envelope, path, hash, length, or Base64 mismatch; duplicate target,
patch, baseline, path, mapping, or effect identities; unknown, cross-scope, or
wrong-kind semantic references; incomplete, swapped, or promoted link arrays;
unresolved effect source/mapping IDs; unsupported kind/assessment; and a
missing required target, mapping, compatibility, or migration record. It makes
no repair, identity reconstruction, hidden winner, or partial successful
candidate.

P10-30B's executable matrix must prove exact encode/decode roundtrip, portable
one-pass package reopening, V1/V2 continuity coexistence, multi-target
baseline/proposed-byte traceability, label/order independence where labels are
not identity inputs, and the stated fail-closed cases. Its property-based tests
exercise supplied bytes, opaque identities, and canonical rejection, not this
document's prose.

Candidate admission is not CML syntax/semantic validation, safe application,
freshness, a computed semantic diff, actual compatibility/migration, human
approval, Review acceptance, Git acceptance, rehydration, provider invocation,
publication, or runtime/API/UI exposure. P10-31--P10-34 own their later exact
artifact contracts and proofs. Phase 10.4+ and all canonical CML mutation are
outside this work.
