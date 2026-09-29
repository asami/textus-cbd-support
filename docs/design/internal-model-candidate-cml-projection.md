---
status: draft
decision_scope: P10-30A
updated_at: 2026-09-29
---

# Internal-model Candidate CML Projection Design

This design records the fixed P10-30A boundary for a durable, explicit
candidate CML-projection artifact. Its normative companion is the
[Internal-model Candidate CML Projection
Contract](../spec/internal-model-candidate-cml-projection-contract.md). The
[Internal-model Semantic Realization
Design](internal-model-semantic-realization.md), [Internal-model Projection
Continuity Design](internal-model-projection-continuity.md), and [Candidate
Design and Semantic-Diff Integration
Design](candidate-design-semantic-diff-integration.md) retain their respective
semantic ledger, continuity, and Phase 9 candidate-evidence authority.

P10-30A decides the document contract only. P10-30B is still required to
implement its codec, one-pass package admission, and executable specification;
neither this design nor the companion contract claims that proof or Step
acceptance.

## One candidate beside one accepted ledger

The candidate is a proposal-side content profile beside, not inside, the
selected realization and continuity evidence. It carries one caller-allocated
candidate identity/revision, one Phase 9 candidate-model identity, exact
realization and continuity artifact IDs, and the same three-identity scope.
Those fields make the proposal transportable without converting it into a
second CCDM, a replacement realization, or a source of semantic authority.

```text
one verified manifest/inventory/digest pass
                  |
                  +--> selected realization and source evidence ledger
                  |
                  +--> selected V1/V2 continuity binding
                  |
                  +--> selected candidate CML projection
                           |
                           +--> explicit targets and baseline bytes
                           +--> explicit CCDM-to-proposed-CML mappings
                           +--> attributed expected effects
```

The selector is family-specific. Continuity is selected by its existing V1/V2
family and the candidate by its separate V1 candidate family, even though both
use `role=projection`. This admits coexistence without adding a package role or
using artifact order, path, or a first matching projection as a decision.
Canonical bytes and exact dependencies make the selected package input
portable, while package identity, inventory, and digest stay external verified
context rather than self-referential candidate content.

## CML bytes are traceability, not CML authority

Each target binds an exact existing CML-baseline snapshot to raw proposed bytes,
an exact target identity, a Phase 9 patch identity, and the snapshot's source
envelope/path. This offers a durable trace from an explicit proposed byte
sequence to a source-owner admitted baseline without treating a path, raw
digest, or byte equality as a semantic anchor, freshness proof, or authority to
read live CML.

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

One package pass selects the exact realization, continuity binding, candidate
profile, and required CML-baseline snapshots. Each content family is recognized
by an exact profile/version pair and fails closed for unknown or malformed
pairs, absent or duplicate matches, or dependency/scope disagreement. A
recognized nonselected family is only skipped for selection; it is not thereby
validated.

The later reader is a read-only admission boundary. It does not reopen mutable
paths, reconstruct identities, repair bytes, calculate a diff, validate or
apply CML, inspect freshness, invoke a provider, or write anything. Approval,
Review, Git acceptance, history/supersession, invalidation, rehydration, and
canonical CML ownership remain on their separately named Phase boundaries.

## P10-30B proof boundary

P10-30B must demonstrate canonical encode/decode roundtrip, reopening from
one verified package pass, V1/V2 continuity coexistence, multi-target
baseline/proposed-byte traceability, supplied opaque-identity and label/order
independence, and every closed rejection category in the contract. The proof
does not turn the candidate artifact into a CML parser, source refresher,
application engine, approval mechanism, or canonical mutation path.
