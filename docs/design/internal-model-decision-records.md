---
status: target
decision_scope: P10-22A--P10-22B / P104-TYPED-DECISION-RECORD
updated_at: 2026-10-01
---

# Internal-model Decision Records Design

## Purpose and authority boundary

This design explains the immutable, portable decision-record artifact defined
by the [Internal-model Decision Record
Contract](../spec/internal-model-decision-record-contract.md). The package
manifest remains the authority for inventory, declared artifact versions and
exact typed dependencies; the accepted semantic realization remains the authority for current
scope, CCDM identities, source references, and conditions. This artifact
preserves a decision account about that realization. It is not a parallel CCDM,
canonical evidence lane, or an implementation of Phase 9 `HumanDecision`.

The existing package `decision` role is sufficient. A conventional portable
`decisions.yaml` artifact is separate from realization and projection
artifacts, and does not alter package role presence, multiplicity, or public package
validation. The filename is a convention, not semantic identity, and creates
no filename-based admission rule.
The current target consumes package V2, source V2 and realization V3 under the
[Typed Control Contract](../spec/internal-model-typed-control-contract.md),
[Package Contract](../spec/internal-model-package-contract.md),
[Source Snapshot Contract](../spec/internal-model-source-snapshot-contract.md)
and [Semantic Realization Contract](../spec/internal-model-semantic-realization-contract.md).
Original P10-22 acceptance remains historical. V2 authoring does not establish
compilation, validation, independent acceptance or Phase 10.4 closure.
Its explicit logical references identify decision-record navigation and attribution;
they never create semantic elements, relationships, or a new canonical lane.

## One authority pass and read-only admission

The package validator performs exactly one project-bound inventory/version authority
pass. `verifiedDecisionRecords` receives that captured result and returns an
immutable `InternalModelVerifiedDecisionPackage`: decision bytes, the one
selected decision artifact, the one selected current realization, and its
selected source snapshots. The decision artifact directly depends on that
realization. A missing decision artifact can leave the package structurally
valid, but cannot establish P10-22 readiness; multiple present decision or
realization selections fail readiness explicitly.

```text
project-bound inventory/version authority pass
             |
             v
captured decision bytes + selected current realization + selected snapshots
             |
             v
read-only decision admission ---> retained record account
```

This immutable handoff preserves one capture: the reader does
not reopen the manifest, rescan package files, fetch a source, inspect Git, or
query historical storage. Malformed, missing, ambiguous, duplicate,
or current-mismatched captured inputs fail closed rather than initiating a
repair path.

## Explicit control references and semantic identity

The ledger has exactly profile, schemaVersion, ledgerReference, scope and
records. Its producer allocates the ledger's recordId and positive
recordRevision independently of the carrier's revision and the decision
artifact's artifactRevision. Each decisionReference is another exact
RecordReference. These types retain explicit declared versions; a consumer
allocates nothing and never chooses a latest record or substitutes a carrier
version. The decision record's four-field basis binds the entire selected
realizationArtifactReference, admitted realizationReference, scope and status.
The artifact role must be realization. Bare identities, hashes, missing or
additional fields and legacy shapes reject without migration or compatibility.

| Concern | Authority and role |
| --- | --- |
| Selected realization artifact ID, revision and role | The project-bound inventory and exact captured ArtifactReference. |
| Logical realization and ledger/decision versions | Independently producer-allocated RecordReference values. |
| Current Component/context/Use Case scope and semantic targets | The selected realization and its Phase 9 CCDM identity ledger. |
| Decision account | Exact decision/topic/choice/alternative identities, actor/provenance claim, rationale, evidence, conditions, limitations and alternatives. |
| Decision currentness | Explicit accepted/superseded chains within one scope and topic, together with exact current-basis matching. |

Exact affected element/relationship identities retain the semantic targets;
labels, paths, array position, timestamps, ranks and provider scores cannot
replace them. Record/topic/choice/alternative local identities create no CCDM
fact or lane. No hash, whole-file equality, re-encoding protocol or cached
canonical bytes controls identity, admission or applicability.
Producer revisions declare provenance; they authenticate no actor and do not
detect undeclared payload mutation.

## Independent topics and explicit history

Record IDs are unique throughout one ledger, even when revisions differ.
The predecessor is a required nullable supersedes field containing null or an
exact recordId/recordRevision object. The entire reference resolves in the same
topic and scope, with no version fallback or two versions of one ID in a ledger.
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
present selected realization. Its old artifact/record references, targets, actor claim,
rationale, evidence, conditions, limitations, and rejected alternatives remain
inspectable. Because absent historical realization bytes and source facts are
not reread, that retained basis is marked historical-unverified. This is a
truthful historical attribution condition, not a second present realization,
an approval, a revalidated current decision, or permission to retarget history.

## Attributable evidence without authority promotion

The artifact records an accountable human identity/role and the provenance
claim that attributes a decision to that person. It can also preserve considered
human, provider, or other external evidence with each source's own identity,
authority, supplied locator/revision, conditions, and limitations. The common source shape has exactly authority, identity, locator and revision,
without hashes. Required nullable locator/revision remain explicit. Current
realization-source evidence equals the complete admitted source metadata and
complete sorted source-reference condition IDs. External evidence has null
sourceReferenceId and empty conditionIds, while retaining its own conditions
and limitations. Unknown source revision stays None even when artifact, ledger,
decision and realization versions are known; it is never fabricated. This
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

The current target freezes the closed `ccdm-decision-records-v2` / `2.0` grammar and represents it
with package-private immutable `InternalModelDecisionActor`, `Choice`,
`Evidence`, `Alternative`, `Basis`, `Record`, `Ledger`, and `Admission` case
classes. The ledger retains semantic values only; the admission retains the whole
selected `InternalModelSemanticRealization`, not a lossy copied summary. This
makes source conditions and limitations observable even when a decision does
not select them.

`InternalModelDecisionRecordCodec` is a pure closed decoder/encoder. It rejects
BOM, invalid UTF-8/JSON, unknown/missing fields, unsorted identity collections, duplicate keys,
invalid Unicode scalar strings, and unsupported vocabulary rather than
repairing them. Harmless whitespace, key order and equivalent escaping are
admitted; trailing non-JSON data rejects. Nested reference values use the existing
strict typed factories/codec, including lexical positive Long revisions. The
encoder sorts identity collections on demand as ordinary deterministic output;
author-supplied
assumptions, conditions, and limitations retain their order and duplicates.
It does not choose a current record, source winner, provider winner, approval,
or authority.

`InternalModelDecisionRecordValidator` consumes only the captured package
handoff. A null handoff, decision or realizationpackage returns structured
operationInvalid. Codec metadata checks precede byte content: no null
reference/path/bytes/dependencies, invalid IDs/revisions/roles, wrong decision
role, null dependency entries, duplicate or unordered dependency IDs or
self-dependency. It admits the captured realization, decodes the captured decision
bytes, requires the entire selected realization artifact reference in the
decision dependencies, checks exact current basis and source/condition visibility, leaves
historical-unverified predecessors as metadata-only retained history, and
checks explicit topic chains. Current status is equivalent to exact equality
of the selected captured artifact reference, admitted realization record reference
and scope. Accepted terminals are exact-current; an exact-current superseded
predecessor remains permitted. A differing historical basis requires superseded
state and historical-unverified status without consulting unavailable historical
artifacts, targets, sources or external history. No filesystem reread, source/Git/archive lookup, mutation,
projection rebuild, or approval is introduced.

Its executable specification owns portable package fixtures and proves complete
typed reload, legacy rejection, presentation-independent JSON, independent
logical revisions, exact predecessor revisions, malformed/null captures, unknown
source versions, empty optional arrays,
independent topics, same-basis and old-basis supersession, provider/actor
separation, source limitation visibility, malformed/current/chain rejection,
property-based semantic identity-order stability with independent positive revisions
and Unicode prose, and captured no-reread behavior.

Neither this design nor its reader writes CML, changes a source, performs
approval, fetches content, rehydrates application state, exposes an endpoint,
publishes an artifact, or deploys anything. Historical P10-22 acceptance is preserved; current V2 authoring remains pending
parent-owned validation, independent review and acceptance work.
