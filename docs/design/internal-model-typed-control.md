---
status: target
decision_scope: P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-01
---

# Internal-model Typed Control Design

The [Typed Control Contract](../spec/internal-model-typed-control-contract.md)
owns normative reference types, version ownership, subject boundaries, and
failure outcomes. This design records the settled ownership rationale.
It describes the required target, with executable migration and verification
pending; current engineering status belongs in the Phase ledger.

## Producer ownership and explicit versions

Project-owned local single-writer records have one producer responsible for
allocating stable IDs and advancing revisions when control meaning changes.
Consumers resolve the exact declared versions. A stable package reference
identifies the project/package; the separate carrier revision identifies the
inventory state. Artifact references identify a version and role within that
package. Record references identify a logical version within its owner.

These distinct types keep package placement, artifact inventory, and semantic
record identity from being substituted for one another. Explicit revisions
record producer provenance. They do not authenticate a source or detect a
producer's undeclared content change. The execution model excludes speculative
locks, retries, backups, rollback, and content-derived control tokens.

## Shared public trace uses the existing reference domains

The existing public proposed-CML patch trace needs source-snapshot artifact and
proposed-content logical references. It uses the existing
`InternalModelArtifactReference` and `InternalModelRecordReference`, with only
their opaque ID/revision domains, companions and artifact-role enum/companion
narrowly made public. The domain factories, closed roles and positive Long
semantics stay the same; no parallel semantic type model or compatibility
constructor is added. Package ID/project token/package reference and capture/
codec APIs remain runtime-private.

This public surface lets an external caller supply its declared baseline and
content versions to the shared Phase 9 evidence boundary. Reference metadata
validation is pure and does not acquire a payload, source or package. Public
visibility neither authenticates a stored claim nor establishes all-consumer
migration, validation, current Step acceptance or Phase closure.

## Subject and carrier responsibilities

The carrier stores semantic inputs and later control records. Its closed
inventory and safe filesystem admission remain useful without checksum
integrity claims. A ReviewSubject names the exact semantic input versions and
scope actually reviewed. It is neither a copied historical manifest nor a
second semantic inventory.

```text
project/package + explicitly versioned semantic artifacts
  -> complete ReviewSubject
  -> attributable review + independently admitted rule/provider versions
  -> approval + independently admitted actual human input
  -> lifecycle applicability for that exact subject

current carrier + exact non-resume artifact references
  -> derived cursor + reconstructed CCDM state
  -> P10-43 independent check of the recorded action
```

The subject's inputs exclude the review, approval, and cursor controls. Review
and approval therefore do not recursively depend on the whole carrier.
Appending a cursor changes the carrier revision while leaving an unchanged
semantic subject available for exact review/approval binding. A changed
semantic input needs an explicit new subject revision and review; choosing a
subset cannot conceal it. Old approval remains immutable and cannot be rebased.

## Existing semantic and human authority

Phase 9's ledger owns CCDM elements, relationships, endpoints, roles,
directions, source witnesses, canonical/enrichment lanes, and conditions.
Realization and continuity organize that ledger and retain complete sidecars
beside all eight projections. Dashboard uses those admitted values.
The management types do not create Entity, Event, Workflow, Mono-Koto,
CML, or view-local meaning.

Review snapshots retain exact targets, attribution and condition facets,
including ordered/multiplicity-preserving limitations. Caller-admitted rule
and provider versions are independent of the stored record. They are evidence
basis, not provider authentication. Stored human approval still needs actual
independent human-input admission; provider review state cannot supply it.
Lifecycle retains explicit invalidation and supersession rather than selecting
a latest sibling record. None of these boundaries grants CML write permission.

## Payload and missing source versions

Ordinary payload retains original/proposed CML content needed by existing
projections. Decoding and semantic validation continue to matter; payload
encoding is not a control identity or permission protocol. Harmless JSON key
order/whitespace variation cannot change eligibility.

An artifact version describes the stored record, while a source revision is
owned by the source. If a source supplies no version, it remains unknown and
required evidence is incomplete. Rehydration preserves recorded source
attribution; it cannot invent live freshness from artifact versions.
Phase 10.5 continues to own live-source drift and CML change gating.

## Dependency and migration boundary

The package, review, and continuation designs apply this one management
contract. Dependencies resolve exact artifact ID/revision/role tuples and
remain acyclic; deterministic writer ordering is useful output discipline,
not parser-byte authority. Invalid structure, missing semantic evidence, and
contradictory declared facts remain distinguishable attributed outcomes.

No legacy decoder, adapter, default schema/version, or fallback accompanies
this redesign. Existing source, realization, decision/issue, candidate/diff,
approval/lifecycle, and continuation formats and specifications require
coordinated, separately owned migration. Their prior hash behavior is historical
evidence, not target compatibility. P10-40–P10-44 executable proofs and the
one complete Phase review/release remain pending; this design grants no acceptance.
