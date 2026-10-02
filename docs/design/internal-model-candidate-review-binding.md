---
status: target
decision_scope: P10-32A / P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-01
---

# Internal-model Candidate Review Binding Design

The [Review Binding Contract](../spec/internal-model-candidate-review-binding-contract.md)
owns the `ccdm-candidate-review-binding-v2` / `2.0` target.
The [Typed Control Contract](../spec/internal-model-typed-control-contract.md)
owns reference/version semantics. This design explains the settled boundary,
with review implementation and executable migration authored and verification
pending.

## Three separate owners

The review keeps three distinct values: the complete explicit semantic
ReviewSubject, the current carrier holding review/control records, and the
independently caller-admitted rule/provider ID/version basis.

The runtime-private immutable subject has exactly `subjectId`,
`subjectRevision`, `packageReference`, `scope` and `artifacts`.
The subject names the exact semantic input versions and scope rather
than retaining a historical manifest. Source, realization, continuity,
candidate, semantic diff, and target review evidence contribute to it.
All present source/realization/projection/decision/open-issue entries are
contributing roots, along with explicitly selected evidence. Exact transitive
dependencies complete that basis. Missing, ambiguous or absent consumed
references, cycles, resume/approval dependencies and selected review
dependencies reject instead of being filtered out. Unconsumed optional
absent entries and inert unselected Validation remain in the carrier.
Decision/open-issue contribution establishes no separate semantic admission.
The review record, human approval, and cursor are controls over that basis and
are excluded from the subject. Added or changed semantic inputs require a new
explicit subject revision and review; selection cannot conceal changed evidence.

The carrier's revision can therefore advance for cursor/approval controls while
an unchanged subject remains unchanged. Reviews depend on exact subject inputs
rather than every present carrier file. Approval subsequently references the
exact review and subject identities/revisions, under independent actual
human-input admission. A changed semantic basis cannot be accepted by
rewriting the historical review or rebasing old approval.

## Exact evidence without self-authentication

The exact seventeen-field root stores `candidateArtifactReference`,
`candidateReference`, `candidateModelIdentity`,
`continuityArtifactReference`, `evidenceArtifacts`, `profile`,
`providers`, `realizationArtifactReference`, `realizationReference`,
`reviewReference`, `rules`, `schemaVersion`, `scope`,
`semanticDiffArtifactReference`, `semanticDiffReference`, `subject`
and `targets`. Logical references use the existing record domain with
independently supplied positive Long revisions.
Artifact references carry ID, positive producer-supplied revision, and closed
role. Rule/provider values carry their existing IDs and versions, independently
admitted by the caller. These explicit versions replace management digests;
they provide neither cryptographic authenticity nor evidence that a producer
never changed content without advancing its version.

Targets retain complete Phase 9 review snapshots, source attribution, every
condition facet, nullable facets/tie key, and ordered/multiplicity-preserving
limitations. Per-target evidence IDs resolve to the exact versioned evidence
selection. A recorded review state cannot authenticate the provider or become
a human decision, applicability conclusion, repository acceptance, or CML grant.

## Capture and semantic authority

`validate(projectRoot, reviewArtifact, expectedExecutionBasis)` accepts
an exact artifact reference. The package validator captures the carrier once.
The review handoff is exactly `carrierPackageContext`,
`semanticDiffPackage` and `reviewArtifact`. Review admission reuses that
capture for candidate, semantic diff, realization, continuity, source, and
target/evidence resolution through current `packagecontext`,
`candidatepackage`, `continuitypackage`, `realizationpackage` and
`semanticdiff` fields. Entire selected inventory metadata stays exact,
and review dependencies equal the ordered subject vector rather than a set
or whole carrier inventory. Its pure boundary never reopens source/package
paths or reads provider history. Non-subject control records stay raw and inert
until admitted by their own contracts with independent evidence.

CCDM meaning remains in the Phase 9 ledger and existing semantic contracts.
A typed subject is a management reference over that meaning, not a second
domain model or a format for regenerating semantic facts from names or bytes.
Missing source versions remain unknown; package-only evidence cannot establish
live freshness.

## Format and migration boundary

Closed strict JSON rejects ambiguous structure and unsupported schemas.
Harmless key order/whitespace variation is accepted; deterministic writer
output is convenience, not authority. No historical manifest bytes, hashes,
canonical-byte comparison, legacy readers/adapters, inferred versions, or
fallback accompany this redesign.

P10-33/P10-34 semantic human-input and lifecycle requirements remain, with
their management bindings pending separately owned migration. The executable
review/approval/lifecycle specifications must prove the new subject/carrier
distinction before acceptance. Admission retains exactly the binding, external
review artifact reference/path, carrier and diff admission; the immutable
subject is retained once in the binding. This design provides no implementation PASS,
action permission, Step acceptance, or Phase closure.
