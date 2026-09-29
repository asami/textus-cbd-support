---
status: working
decision_scope: P10-32A
updated_at: 2026-09-29
---

# Internal-model Candidate Review Binding Contract

This contract defines a portable `ccdm-candidate-review-binding-v1` review
artifact. Its rationale is in the [Candidate Review Binding Design](../design/internal-model-candidate-review-binding.md). It extends neither the [V1 package schema](internal-model-package-contract.md), the [P10-30 candidate projection contract](internal-model-candidate-cml-projection-contract.md), nor the [P10-31 semantic-diff contract](internal-model-semantic-diff-contract.md); those remain authoritative for their own values.

## Scope and non-approval boundary

One selected validation-role artifact retains exactly one externally supplied
review record for one complete candidate package. The caller supplies its
`reviewArtifactId` exactly. Filename, labels, artifact order, provider
preference, review state, or freshness never select it. A retained state such
as `approved` is attributed evidence only: it does not create a human approval,
repository acceptance, CML mutation authority, provider authentication, or
currentness claim.

The root is closed and has exactly `candidateArtifact`, `candidateIdentity`,
`candidateModelIdentity`, `candidateRevision`, `continuityArtifact`,
`evidenceArtifacts`, `profile`, `providers`, `realizationArtifact`,
`realizationIdentity`, `reviewIdentity`, `reviewRevision`,
`reviewedPackageManifest`, `rules`, `schemaVersion`, `scope`,
`semanticDiffArtifact`, `semanticDiffIdentity`, `semanticDiffRevision`, and
`targets`. `profile` is `ccdm-candidate-review-binding-v1`; `schemaVersion` is
`1.0`. Required strings are nonblank without trimming or Unicode
normalization. Candidate, review, and semantic-diff revisions are positive
canonical JSON integers.

## Canonical bytes and complete evidence

The artifact is canonical UTF-8 JSON: no BOM, duplicate member, unknown or
missing field, insignificant whitespace, noncanonical order, or bytes after the
one terminal LF are accepted. Object keys use ascending unsigned UTF-8-byte
order. Every artifact binding has exactly `artifactId` and a lowercase raw-byte
`sha256:` digest. `rules` and `providers` have exact ID/version/digest triples;
they are nonempty, unique by ID, and ordered by unsigned UTF-8 IDs. The caller
separately admits this expected basis and every retained triple must equal it.
The record therefore does not authenticate its own provider or rule basis.

`reviewedPackageManifest` reuses `InternalModelCandidateCmlContent` with exact
`byteLength`, strict standard padded `rawBytesBase64`, and raw-byte digest. It
contains the complete canonical historical V1 `manifest.yaml` bytes, including
the V1 `packageDigest`; the raw-byte digest is distinct from that package
digest. A filtered inventory, rehashed subset, or current carrier manifest is
not a reviewed basis.

`evidenceArtifacts` is nonempty, unique, ordered, and binds only explicitly
inventoried validation-role evidence. `targets` is the complete ordered
candidate target set. Each target has exact `evidenceArtifactIds`, a lossless
Phase 9 `CandidateDesignReviewSnapshot`, and `targetId`. Snapshot attribution,
all condition facets, ordered/multiplicity-preserving limitations, nullable
facets/tie key, component, context, candidate model, patch, ID, and state are
preserved. Snapshot IDs are globally unique. Evidence IDs within each target are
nonempty, unique, and ordered. Their union equals the complete evidence artifact
ID set; multiple targets may share evidence.

## Historical package and capture admission

`verifiedCandidateReviewBinding(projectRoot, reviewArtifactId)` validates the
carrier V1 package and captures its project identity, manifest, inventory, and
artifact bytes once. It uses the same capture for existing continuity/candidate/
semantic-diff selection and selects the stated present validation artifact.
`validateVerified` is pure: it consumes that handoff and never reopens a
package, project, CML, source, provider, or path.

The decoded historical manifest is validated under the existing V1 grammar,
project binding, package digest, topological inventory, and present-dependency
rules using the captured carrier only. Its package/project/schema match the
carrier and its revision is lower. Every historical inventory entry exactly
matches the captured carrier entry in ID, role, path, required flag, raw hash,
and dependencies; presence is derived from that capture. Required entries are
present and optional absence remains explicit. The historical inventory cannot
contain the selected review artifact or its path. Carrier additions can contain
only that selected review artifact and structurally valid `approval`-role
artifacts; they are not selected, decoded, or admitted as approval.

The selected review artifact depends exactly on all present historical artifact
IDs in canonical order. The candidate, continuity, realization, and semantic
diff artifacts, their hashes, identities, revisions, and scope must equal both
the selected same-capture values and the historical inventory. Evidence is a
present validation-role historical artifact with its exact raw hash. Every
snapshot component/context/model/patch binds to its exact candidate target.

The successful admission retains the original record and external review
artifact identity/hash/path, the reviewed package context, the distinct current
carrier package context, and accepted semantic-diff/candidate/realization/
continuity values. It serializes neither its own artifact hash nor carrier
package digest.

## Executable specification

[InternalModelCandidateReviewBindingValidatorSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateReviewBindingValidatorSpec.scala)
is the executable specification for portability, lossless evidence, strict
grammar, one-capture validation, full historical identity, independent
rule/provider basis, and the prohibition on inferred approval.
