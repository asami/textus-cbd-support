---
status: target
decision_scope: P10-30 / P104-TYPED-CANDIDATE-CML
updated_at: 2026-10-01
---

# Internal-model Candidate CML Projection Design

This design records the fixed current V2 boundary for a durable, explicit
candidate CML-projection artifact. Its normative companion is the
[Internal-model Candidate CML Projection
Contract](../spec/internal-model-candidate-cml-projection-contract.md). The
[Internal-model Semantic Realization
Design](internal-model-semantic-realization.md), [Internal-model Projection
Continuity Design](internal-model-projection-continuity.md), and [Candidate
Design and Semantic-Diff Integration
Design](candidate-design-semantic-diff-integration.md) retain their respective
semantic ledger, continuity, and Phase 9 candidate-evidence authority.

The [Typed Control Contract](../spec/internal-model-typed-control-contract.md)
owns producer-declared identities and versions; the [Package Contract](../spec/internal-model-package-contract.md)
and [Source Snapshot Contract](../spec/internal-model-source-snapshot-contract.md)
own the V2 capture and attribution basis. Only candidate V2/schema 2.0 with
realization/continuity V3 is current. Original P10-30A/B proof remains historical.
Current authoring, coordinated consumer migration, parent validation, independent
review and Phase 10.4 acceptance/release remain separate; no closure is claimed.

## One candidate beside one accepted ledger

The candidate is a proposal-side content profile beside, not inside, the
selected realization and continuity evidence. It carries one caller-allocated
candidate `RecordReference`, one Phase 9 candidate-model identity, exact
realization and continuity `ArtifactReference` values, and the same three-identity scope.
Those fields make the proposal transportable without converting it into a
second CCDM, a replacement realization, or a source of semantic authority.

```text
one verified manifest/inventory pass
                  |
                  +--> selected realization and source evidence ledger
                  |
                  +--> selected V3 continuity binding
                  |
                  +--> selected candidate CML projection
                           |
                           +--> explicit targets and baseline bytes
                           +--> explicit CCDM-to-proposed-CML mappings
                           +--> attributed expected effects
```

The selector is family-specific. Continuity is selected by its V3
family and the candidate by its separate V2 candidate family, even though both
use `role=projection`. This admits coexistence without adding a package role or
using artifact order, path, or a first matching projection as a decision.
Exact typed dependencies make the selected basis explicit, while package
reference, carrier revision and inventory remain external captured context.
The eight candidate root fields are candidateReference, candidateModelIdentity,
continuityArtifactReference, profile, realizationArtifactReference, schemaVersion,
scope and targets. Logical candidate and content records, carrier artifacts and
carrier revisions are distinct positive Long domains. They are explicitly
allocated, never derived from payload or substituted for source revisions.

## CML bytes are traceability, not CML authority

Each target binds an exact existing CML-baseline snapshot to raw proposed bytes,
an exact target identity, a Phase 9 patch identity, and the snapshot's source
envelope/path. This offers a durable trace from an explicit proposed byte
sequence to a source-owner admitted baseline without treating a path, raw
version or byte relationship as a semantic anchor, freshness proof, or authority to
read live CML. The baseline is an exact source-snapshot ArtifactReference.
Proposed content has exactly a separately allocated contentReference,
nonnegative byteLength and strict padded RFC4648 rawBytesBase64. Length, alphabet,
padding and unused pad-bit checks validate ordinary payload representation only.
The four-field authority/identity/locator/revision envelope equals the selected
V2 baseline exactly; unknown source locator/revision remain None.

The design deliberately allows proposed baseline and proposed bytes to be
equal, and proposed content to be empty. Both cases preserve what was supplied;
neither silently declares a semantic no-op, a valid empty CML file, a deletion,
or safe applicability. Creation, absence, moves, and deletions require their
own future contract because a synthetic baseline would falsely represent a
present CML source.

## Explicit mappings retain the realization sidecar

Mappings say which exact selected realization element or relationship a caller
proposes to associate with a supplied CML identity and bounded anchor. Complete
canonical/enrichment/condition links are repeated as equality-constrained IDs,
not copied evidence. The realization therefore remains the sidecar that owns
source references, attributions, conditions, endpoints, and Phase 9 meaning.

This separates an explicit proposed destination from parsing. A codec can
confirm a mapping's closed shape, selected kind, scope, and complete links, but
cannot search CML text to find a mapping or decide whether a CML anchor denotes
the supplied semantics. It also preserves many-to-one proposal mappings without
introducing a forced Mono/Entity or Koto/Event conversion.

## Effects are visible expectations, never a verdict

Compatibility and migration effects are separately attributed expectations
connected to local mapping IDs and retained realization source references.
They allow expected breaking, compatible, required, unknown, and disagreeing
outcomes to remain visible without creating an internal winner, source-owner
acceptance, calculated compatibility judgment, migration execution, or a
canonical assertion.

The requirement for both record kinds prevents a candidate from silently
presenting one concern as the other. Exact record shapes and local references
keep expectations reviewable while preserving their deliberately limited
authority.

## Deliberate admission and lifecycle boundary

One package pass captures the exact realization, continuity binding, candidate
profile, and required CML-baseline snapshots. Each content family is recognized
by an exact profile/version pair and fails closed for unknown or malformed
pairs, absent or duplicate matches, or dependency/scope disagreement. A
recognized nonselected family is only skipped for selection; it is not thereby
validated. Required nested references are closed RecordReference or
ArtifactReference values, with exact roles and positive lexical Long revisions.
Old profiles, bare IDs, hash-bearing shapes, missing versions, compatibility
aliases and fallback reject.

Before semantic iteration, the admission checks all captured context and selected
metadata, nulls, role/ID/revision syntax, safe paths, bytes, unique ordered
dependencies and inventory entries. Candidate and every selected baseline match
one exact context reference, path, required flag, dependencies and presence;
continuity and realization match that same context. Candidate basis references
and scope equal the admitted continuity realization, and dependencies equal the
full selected realization/continuity/baseline reference set. No metadata duplicate
is hidden by set conversion. Malformed handoffs return structured operationInvalid.

Strict UTF-8/JSON rejects BOM, duplicates, trailing data and missing/extra fields.
Harmless key/whitespace/escape presentation is admitted. The ordinary writer emits
deterministic JSON on demand while preserving supplied semantic array order;
it cannot sort or deduplicate invalid arrays into acceptance. No digest, cached
canonical bytes, whole-file comparison or re-encoding control protocol remains.
Explicit revisions provide producer provenance, not authentication or detection
of undeclared payload changes.

The later reader is a read-only admission boundary. It does not reopen mutable
paths, reconstruct identities, repair bytes, calculate a diff, validate or
apply CML, inspect freshness, invoke a provider, or write anything. Approval,
Review, Git acceptance, history/supersession, invalidation, rehydration, and
canonical CML ownership remain on their separately named Phase boundaries.

## Historical proof and current authoring boundary

P10-30B's original canonical/digest proof is historical. Current executable
authoring retains semantic roundtrip, reopening from one captured package pass,
V3 family independence, explicit legacy rejection, multi-target
baseline/proposed-byte traceability, supplied opaque-identity and label/order
independence, and every closed rejection category in the contract. The proof
adds independent record/artifact revisions, reference lexical/shape failures,
complete capture contradictions, harmless presentation and unknown source-version
evidence. It retains complete Phase 9 semantic lanes, condition sidecars and all
eight projections without reducing their evidence to empty fixtures. Parent
verification and review remain pending. None of this turns the artifact into a
CML parser, source refresher,
application engine, approval mechanism, or canonical mutation path.
