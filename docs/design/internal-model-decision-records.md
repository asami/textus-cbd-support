---
status: accepted
decision_scope: P10-22A--P10-22B
updated_at: 2026-09-28
---

# Internal-model Decision Records Design

## Purpose and authority boundary

This design explains the immutable, portable decision-record artifact defined
by the [Internal-model Decision Record
Contract](../spec/internal-model-decision-record-contract.md). The package
manifest remains the authority for inventory, dependencies, and exact artifact
bytes; the accepted semantic realization remains the authority for current
scope, CCDM identities, source references, and conditions. This artifact
preserves a decision account about that realization. It is not a parallel CCDM,
canonical evidence lane, or an implementation of Phase 9 `HumanDecision`.

The existing package `decision` role is sufficient. A conventional portable
`decisions.yaml` artifact is separate from realization and projection
artifacts, and does not alter V1 role presence, multiplicity, or public package
validation. The filename is a convention, not semantic identity, and creates
no filename-based admission rule.
Its logical identity fields identify decision-record navigation and attribution;
they never create semantic elements, relationships, or a new canonical lane.

## One authority pass and read-only admission

The package validator performs exactly one manifest/digest/inventory authority
pass. `verifiedDecisionRecords` receives that captured result and returns an
immutable `InternalModelVerifiedDecisionPackage`: decision bytes, the one
selected decision artifact, the one selected current realization, and its
selected source snapshots. The decision artifact directly depends on that
realization. A missing decision artifact can leave the package structurally
valid, but cannot establish P10-22 readiness; multiple present decision or
realization selections fail readiness explicitly.

```text
manifest/digest/inventory authority pass
             |
             v
captured decision bytes + selected current realization + selected snapshots
             |
             v
read-only decision admission ---> retained record account
```

This handoff prevents time-of-check/time-of-use substitution: the reader does
not reopen the manifest, rescan package files, fetch a source, inspect Git, or
query historical storage. Malformed, missing, ambiguous, duplicate, tampered,
or current-mismatched captured inputs fail closed rather than initiating a
repair path.

## Exact hash and semantic identity have different jobs

The decision record binds the current realization artifact ID, its realization
identity, and the SHA-256 of its exact raw bytes. The hash proves the precise
captured realization revision; it does not name a semantic subject. Exact
affected element/relationship identities retain the semantic targets; labels,
paths, hashes, display order, timestamps, ranks, and provider scores cannot
replace them.

| Concern | Authority and role |
| --- | --- |
| Current realization bytes and digest | Manifest-verified inventory and the selected realization artifact. |
| Current Component/context/Use Case scope and semantic targets | The selected realization and its Phase 9 CCDM identity ledger. |
| Decision account | Stable decision/topic/choice/alternative identities, actor/provenance claim, rationale, evidence, conditions, limitations, and alternatives. |
| Decision currentness | Explicit accepted/superseded chain within one scope and topic, together with exact current-basis matching. |

This separation permits a decision to explain why a semantic target was chosen
without allowing a decision record to synthesize a missing target or silently
turn a raw digest into CCDM identity.

## Independent topics and explicit history

Each topic has its own explicit chain. A topic's current record is determined
by accepted/superseded relations, not list order or time. A superseded record
has exactly one successor; an accepted terminal has none. No self-link,
dangling link, cycle, fork, or duplicate accepted terminal is admitted. Topics
do not compete: different topics may each have a valid current terminal.

```text
topic A: decision A1 (superseded) ---> decision A2 (accepted terminal)
topic B: decision B1 (accepted terminal)
```

The predecessor is retained even when its realization basis differs from the
present selected realization. Its old identity/hash, targets, actor claim,
rationale, evidence, conditions, limitations, and rejected alternatives remain
inspectable. Because absent historical realization bytes and source facts are
not reread, that retained basis is marked historical-unverified. This is a
truthful historical attribution condition, not a second present realization,
an approval, a revalidated current decision, or permission to retarget history.

## Attributable evidence without authority promotion

The artifact records an accountable human identity/role and the provenance
claim that attributes a decision to that person. It can also preserve considered
human, provider, or other external evidence with each source's own identity,
exact digest, supplied locator/revision, conditions, and limitations. This
retention makes a rationale reviewable without changing the authority of its
inputs.

In particular, a provider output remains evidence. It cannot become the human
actor, choose the accepted choice, authenticate a person, prove approval, or
become canonical CML evidence. Likewise, declared human provenance does not
authenticate the declaration, establish semantic truth, promote a proposal, or
reconstruct the transient Phase 9 `HumanDecision`/coverage admission. A later
explicit human decision may choose a formerly rejected alternative, but no
automatic ranking or storage rule can do so.

## Concrete closed executable boundary

P10-22B freezes the closed `ccdm-decision-records-v1` grammar and represents it
with package-private immutable `InternalModelDecisionActor`, `Choice`,
`Evidence`, `Alternative`, `Basis`, `Record`, `Ledger`, and `Admission` case
classes. The ledger preserves canonical bytes; the admission retains the whole
selected `InternalModelSemanticRealization`, not a lossy copied summary. This
makes source conditions and limitations observable even when a decision does
not select them.

`InternalModelDecisionRecordCodec` is a pure closed decoder/encoder. It rejects
bad bytes, unknown/missing fields, noncanonical identity order, duplicate keys,
invalid Unicode scalar strings, and unsupported vocabulary rather than
repairing them. It canonicalizes identity collections only; author-supplied
assumptions, conditions, and limitations retain their order and duplicates.
It does not choose a current record, source winner, provider winner, approval,
or authority.

`InternalModelDecisionRecordValidator` consumes only the captured package
handoff. It admits the captured realization, decodes the captured decision
bytes, checks exact current basis and source/condition visibility, leaves
historical-unverified predecessors as metadata-only retained history, and
checks explicit topic chains. Exact raw-byte digest and semantic identity have
separate roles. No filesystem reread, source/Git/archive lookup, mutation,
projection rebuild, or approval is introduced.

Its executable specification owns portable package fixtures and proves complete
typed reload, V1/V2 compatibility, canonical encoding, empty optional arrays,
independent topics, same-basis and old-basis supersession, provider/actor
separation, source limitation visibility, malformed/current/chain rejection,
property-based identity-order stability, and captured no-reread behavior.

Neither this design nor its reader writes CML, changes a source, performs
approval, fetches content, rehydrates application state, exposes an endpoint,
publishes an artifact, or deploys anything. P10-22 remains draft and pending
parent-owned validation, review, and acceptance work.
