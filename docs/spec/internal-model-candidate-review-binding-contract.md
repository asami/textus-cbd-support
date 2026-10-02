---
status: target
decision_scope: P10-32A / P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-01
---

# Internal-model Candidate Review Binding Contract

This target defines `ccdm-candidate-review-binding-v2` with
`schemaVersion="2.0"`. The [Typed Control Contract](internal-model-typed-control-contract.md)
owns its management references and semantic subject; the
[Review Binding Design](../design/internal-model-candidate-review-binding.md)
explains the boundary. Review implementation and executable migration are authored;
validation and independent acceptance remain pending. Prior V1 evidence does not prove this target; old readers,
historical-manifest adapters, migration, defaults, and fallback are unsupported.

## Scope and non-approval boundary

One exactly caller-selected, present `validation` artifact retains one
externally supplied review record for one complete candidate semantic subject.
The caller supplies the exact review artifact reference; labels, filename,
order, provider preference, state, or freshness never select it.

Recorded state such as `approved` is attributed evidence only. It creates no
actual human approval, repository acceptance, CML permission, provider
authentication, or live-currentness claim. The candidate projection,
semantic-diff, realization/continuity, and CCDM contracts retain their semantic
meaning. Their management bindings migrate under the typed contract.

## Closed record and typed bindings

The root has exactly seventeen closed fields:

```text
candidateArtifactReference, candidateReference, candidateModelIdentity,
continuityArtifactReference, evidenceArtifacts, profile, providers,
realizationArtifactReference, realizationReference, reviewReference, rules,
schemaVersion, scope, semanticDiffArtifactReference, semanticDiffReference,
subject, targets
```

`profile` is exactly `ccdm-candidate-review-binding-v2`;
`schemaVersion` is exactly `2.0`. Candidate, realization, review and
semantic-diff logical references use exactly `RecordReference(recordId,
recordRevision)` with explicitly supplied positive producer Long revisions. Required semantic strings remain nonblank, retained
without trimming or Unicode normalization. Artifact revisions are independent
positive producer-supplied `Long` values; a semantic candidate revision is
not an artifact revision.

The envelope is strict UTF-8 JSON. Invalid syntax/UTF-8, BOM, duplicate
members, missing/extra fields, wrong types, unsupported profile/schema, and
YAML-only syntax reject. Writers may use deterministic keys and LF; readers
accept harmless object-key order and insignificant JSON whitespace variation.
Canonical re-encoding/whole-file byte equality is not admission authority.

`candidateArtifactReference`, `continuityArtifactReference`,
`realizationArtifactReference`, `semanticDiffArtifactReference`, and each `evidenceArtifacts` entry are exact
`ArtifactReference(artifactId, artifactRevision, role)` values.
Candidate/continuity/diff roles are `projection`, realization is
`realization`, and selected evidence is `validation`.
There are no artifact hash fields. `rules` entries have exactly `ruleId`
and `ruleVersion`; `providers` entries have exactly `providerId`
and `providerVersion`. Both arrays remain nonempty, unique by ID, and
ordered by unsigned UTF-8 ID. The caller independently admits that exact
ID/version basis; retained values must equal it. The record cannot authenticate
its own provider or rule basis.

## Explicit complete ReviewSubject

`subject` has exactly `subjectId`, `subjectRevision`,
`packageReference`, `scope`, and `artifacts`, as defined by the
typed contract. Its artifact set is explicit, unique, and ordered by exact
artifact ID. Its stable package reference must match the admitted carrier's
project/package identity. Subject scope equals the record and selected
candidate/realization/continuity/diff scope.

The complete subject roots are EVERY present carrier entry with role
`source-snapshot`, `realization`, `projection`, `decision` or
`open-issue`, plus explicitly selected `evidenceArtifacts`.
Resolve each root and its complete transitive dependencies by exact reference
in the current capture. Missing, absent, revision/role-mismatched or ambiguous
consumed entries reject, as do cycles, selected-review dependencies and any
`resume` or `approval` dependency. Forbidden dependencies are failures,
never silently filtered out. Order the unique result by unsigned UTF-8
artifact ID and require exact equality with `subject.artifacts`,
including every revision and role. Every root binding and selected evidence
occurs there.

Unconsumed optional absent inventory remains only in the carrier. Other
unselected `validation` entries are inert unless explicitly selected
evidence or transitive dependencies. Decision/open-issue entries are raw
semantic inputs here; review admission does not separately admit their
semantic record contents.

Review, approval, and resume control records are excluded. Historical manifest
bytes, package digests, and the complete carrier inventory are not a reviewed
subject. Adding control records or advancing a cursor-only carrier revision
does not change an unchanged subject. Added/changed semantic input, input
revision, scope, or subject reference requires an explicit new subject revision
and review; old review/approval cannot be rebased.

## Lossless target evidence

`evidenceArtifacts` is nonempty, unique, ordered, and selects explicitly
inventoried, present `validation` evidence versions. It never selects the
review record itself. `targets` is the complete ordered candidate target set.
Each target retains exactly `evidenceArtifactIds`, `reviewSnapshot`,
and `targetId`; the evidence IDs resolve to the exact versioned bindings
above and the subject, rather than independently selecting bare-ID versions.

Snapshots retain the lossless Phase 9 `CandidateDesignReviewSnapshot`:
exact ID, state, Component, context, candidate model, patch, attribution,
all condition facets, nullable facets/tie key, and ordered/multiplicity-preserving
limitations. Snapshot IDs are globally unique. Per-target evidence IDs are
nonempty, unique, ordered; their union equals the complete selected evidence
ID set. Targets may share evidence. Every snapshot component/context/model/
patch equals its exact candidate target. No selected evidence or condition is
discarded, inferred, preferred, or promoted to human approval.

## One capture and pure admission

`validate(projectRoot, reviewArtifact: InternalModelArtifactReference,
expectedExecutionBasis)` uses
`verifiedCandidateReviewBinding(projectRoot, reviewArtifact)`.
The exact external reference selects one present Validation entry; the package
validator captures consuming-project identity, inventory and payload once for
candidate, diff, realization, continuity, source and review admission.

The handoff is exactly `carrierPackageContext`, `semanticDiffPackage`
and `reviewArtifact`. Current upstream captures use `candidatepackage`,
`packagecontext`, `continuitypackage`, `realizationpackage` and
`semanticdiff`. Selected reference/path/required/dependencies equal the
entire present inventory entry; carrier equals the admitted candidate context.
Null handoff/capture metadata and malformed independent basis reject with
structured operation-invalid failure before dereference.

`validateVerified` is pure and never reopens a project, package, CML, source,
provider, or path. It checks typed carrier/subject references, exact selected
artifact versions/roles/presence, candidate/diff/review identities and revisions,
scope, complete target/evidence bindings, and independent rule/provider basis.
Selected review dependencies equal `subject.artifacts` exactly in declared
order. The selected review depends on its exact semantic subject/evidence inputs,
not all present carrier files. Later approval/resume controls remain inert until
separately admitted and are not retroactively reviewed.

A successful admission has exactly `binding`, `reviewArtifactReference`,
`reviewArtifactPackageRelativePath`, `carrierPackageContext` and
`semanticDiffAdmission`. The supplied subject is retained once in the
binding with the external selection and admitted semantic values. Raw
historical manifest context or byte comparison supplies no authority.
Invalid structure rejects; missing independent evidence is attributed
incomplete, and contradictory identity/revision/role/scope/state is attributed
inconsistent. No repair, latest selection, inferred subject, or approval follows.

## Authored executable specification and pending assurance

[InternalModelCandidateReviewBindingValidatorSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateReviewBindingValidatorSpec.scala)
authors typed subject portability, lossless evidence, strict
grammar with harmless JSON presentation variation, same-capture admission,
exact versions/roles, complete dependency closure, independent rule/provider
basis and no inferred approval. Structurally admissible review-specific negatives
establish current upstream admission independently. Actual path deletion and
directory copy exercise portability. Binary proposed CML and all eight retained
projection/sidecar families remain covered. Validation and independent review
are not claimed.

`InternalModelCandidateHumanApprovalValidatorSpec` and
`InternalModelCandidateApprovalLifecycleSpec` must prove actual independent
human input and exact review/subject identities/revisions/scope. They must
distinguish cursor-only carrier changes from changed selected semantic inputs,
retain all invalidation dimensions and explicit supersession, and forbid
rewriting old approval. Prior tests are historical evidence only.
No code/test PASS, accepted Step, or Phase closure is claimed here.
