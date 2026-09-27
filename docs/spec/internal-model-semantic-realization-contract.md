---
status: draft
decision_scope: P10-20A--P10-20B
updated_at: 2026-09-28
---

# Internal-model Semantic Realization Contract

This normative contract defines the logical content and admission boundary of
one portable internal-model `realization` artifact for P10-20. Its companion
design is [Internal-model Semantic Realization
Design](../design/internal-model-semantic-realization.md). The V1 package
envelope, inventory, dependencies, and exact-byte digest remain governed by the
[Internal-model Package Contract](internal-model-package-contract.md). The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
remains authoritative for the meaning of every semantic identity, source fact,
and projection.

P10-20B fixes the V1 artifact-byte grammar and read-only executable admission
proof below. P10-21 owns the later projection proof. This draft does not claim
Step or Phase acceptance.

## 1. Scope and authority

One realization records selected candidate state for exactly all of the
following, without widening any of them:

1. one admitted Component identity;
2. one bounded CCDM projection-context identity; and
3. one selected Use Case semantic element identity within that Component and
   context.

The realization records Phase 9 CCDM assertions. It SHALL NOT create a second
Entity, Event, Workflow, Mono-Koto, CML, or view-local semantic model. Every
element and relationship identity is the exact Phase 9 identity in the selected
scope. An identity is opaque to this artifact: it SHALL NOT be generated,
matched, recovered, or replaced from a label, path, order, diagram geometry,
content hash, provider output, or lexical similarity.

The record is not human approval, CML mutation permission, a statement of
live-source freshness, Git acceptance, or proof of complete rehydration. A
manifest role, artifact digest, or structural admission SHALL NOT be treated as
any of those claims.

## 2. P10-20B canonical JSON V1 and read-only admission

The realization is UTF-8 canonical JSON: no BOM, duplicate member,
insignificant whitespace, or trailing byte after exactly one LF is admitted.
Every object has ascending UTF-8-byte key order; parsing never repairs input,
and re-encoding the accepted value must reproduce the supplied bytes exactly.
The root has exactly these fields:

```json
{"canonicalAssertions":[],"conditions":[],"elements":[],"enrichmentAssertions":[],"profile":"ccdm-realization-v1","realizationIdentity":"...","relationships":[],"schemaVersion":"1.0","scope":{},"sourceReferences":[],"successorLinks":[],"traceability":{}}
```

`scope` has exactly `componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`. `traceability` has exactly the sorted,
unique `consumedSnapshotArtifactIds` array. Its set must equal both the
selected manifest realization entry's `dependsOn` set and the set of
`sourceReferences.snapshotArtifactId`; the realization serializes neither its
raw-byte digest nor package revision.

An element has exactly `canonicalAssertionIds`, `conditionIds`,
`enrichmentAssertionIds`, `identity`, `kind`, and explanatory `label`.
A relationship has exactly the same three link arrays plus `direction`,
`identity`, `label`, `role`, `sourceElementIdentity`, and
`targetElementIdentity`. Element and relationship arrays are sorted uniquely
by exact identity. `direction` is exactly `source-to-target`,
`target-to-source`, or `undirected`. The selected Use Case is an element whose
exact `kind` is `use-case`.

Each assertion lane record has exactly `assertionId`, `conditionIds`,
`content`, `semanticIdentity`, `semanticIdentityKind`, and
`sourceReferenceId`. `semanticIdentityKind` is exactly `element` or
`relationship`; both lanes are separately sorted by `assertionId`, and an ID
cannot occur in both lanes. Each structural record lists exactly the assertion
and condition IDs which target that exact kind/identity. Every element and
relationship has at least one canonical assertion witness.

Each condition has exactly `affectedIdentity`, `affectedKind`, `conditionId`,
`detail`, `kind`, and `sourceReferenceId`. Its closed V1 `kind` is one of
`absence`, `ambiguity`, `conflict`, `authorization-redaction`,
`availability-staleness`, `malformed`, or `limitation`. Conditions are sorted
uniquely by `conditionId`; no winner, replacement, or implicit condition field
exists. Every limitation carried by a referenced model-context fact must occur
as an attributed `limitation` condition with the same detail.

Each `sourceReferences` record has exactly `referenceId`, `snapshotArtifactId`,
`source`, `sourceAnchor`, and `target`. `source` has exactly `authority`,
`identity`, `locator`, `revision`, and `sha256`; it must equal the accepted
source-snapshot envelope byte-for-byte in value. `target` is either `null` or
an object with exactly `semanticIdentity` and `semanticIdentityKind`. A target
must be witnessed at the exact Component/context/kind/identity/anchor tuple by
a Scenario `traceLinks` entry or model-context `facts` entry. Assertions must
use a target matching their exact semantic ID and content matching that exact
source basis. A targetless reference still requires a source-owned anchor;
glossary/BoK evidence may support attributable non-identity material but never
a CCDM identity assertion. A CML `projectRelativePath` is never such an anchor.

Each successor link has exactly `priorIdentity`, `priorKind`,
`sourceReferenceId`, `successorIdentity`, and `successorKind`, sorted by that
tuple. Both identities resolve in the selected scope; its source reference must
carry an explicit target matching the prior or successor identity. The link
retains, rather than overwrites, the predecessor.

Admission starts with exactly one project-bound package inventory/digest pass.
It requires one present `role=realization` entry, validates every selected
snapshot with the accepted Phase 10.1 validator before inspecting it, and uses
only the exact verified bytes returned by that pass. It rejects a missing or
duplicate realization, noncanonical/unknown profile or field, wrong source
metadata/hash/anchor, dependency mismatch, missing witness, cross-scope ID,
dangling endpoint, lane collision, omitted reported limitation, unsupported
direction, or malformed source basis. It performs no file write, approval,
CML application, projection, rehydration, source refresh, or semantic repair.

## 3. Logical record shape

The following is the logical shape mapped by the canonical JSON V1 grammar in
section 2; it does not create a second JSON, YAML, Scala, or storage model.

| Logical field | Required meaning |
| --- | --- |
| `realizationIdentity` | Stable artifact identity allocated for this candidate; never derived from content, labels, paths, hashes, providers, ordering, or geometry. |
| `scope` | Exact `componentIdentity`, `projectionContextIdentity`, and `selectedUseCaseElementIdentity`; all are Phase 9 identities and the Use Case is an element in that scope. |
| `canonicalAssertions` | Selected canonical source assertions for used elements/relationships, with affected ID, kind or relation role, source/authority, snapshot reference, source-owned anchor, and condition/limitation links. |
| `enrichmentAssertions` | Bounded source-attributed evidence with the same trace form; it is a separate lane and SHALL NOT add, replace, or promote a canonical fact. |
| `elements` | Exact element IDs with semantic kind, explanatory non-key label, assertion, source, and condition/limitation references. |
| `relationships` | Exact relationship IDs with exact source/target element IDs, asserted role and direction, explanatory non-key label, assertion, source, and condition/limitation references. |
| `sourceReferences` | Each selected basis reference names an exact consumed manifest `source-snapshot` artifact ID, source authority/identity, exact snapshot-supplied `source.sha256` raw-source hash, and source-owned anchor. Only source revision and locator have admitted unavailable (`null`) forms. |
| `conditions` | Explicit absence, ambiguity, conflict, authorization/redaction, availability/staleness, malformed-source, and limitation records linked to affected IDs and source references. |
| `successorLinks` | Explicit source-attributed replacement links between prior and successor identities. |
| `traceability` | In-band realization identity/profile and the complete consumed source-snapshot dependency set. Package revision and the manifest inventory entry's artifact ID/path and realization raw-byte digest are external manifest/package context, not fields in the realization's logical record. |

An explanatory label is presentation text only. It SHALL NOT be a key,
uniqueness criterion, endpoint substitute, source locator, or evidence for
identity equality.

`canonicalAssertions` and `enrichmentAssertions` are separate collections even
when they concern the same semantic ID. A canonical assertion identifies what
the canonical source asserts. Enrichment may describe, qualify, or expose a
limitation of that assertion only with its own source attribution; it SHALL NOT
silently overwrite a canonical assertion or become canonical because it is
newer, more detailed, available, plausible, or convenient.

## 4. Identity, relationship, and replacement invariants

Every referenced element and relationship SHALL belong to the exact Component
and projection context in `scope`. A relationship SHALL retain its own Phase 9
relationship identity, both exact endpoint identities, semantic relationship
role, and direction whenever Phase 9 asserts direction or role. It is invalid
to substitute an unordered endpoint pair, labels, view layout, or inferred role
for this record.

A candidate source revision that changes the semantic subject or assertion
SHALL use a new semantic identity or an explicit `successorLink` according to
the authoritative source contract. It SHALL NOT reuse the old identity merely
because an explanatory label, source path, or displayed relation appears
similar. A successor link identifies both identities, its authoritative basis,
and the source reference that establishes the replacement; it does not erase
the predecessor.

The realization may reference only identities supplied by the selected Phase 9
CCDM basis. It SHALL reject unknown, dangling, cross-Component,
cross-projection-context, or otherwise cross-scope identity references. It
SHALL reject a relationship with a missing endpoint, missing relationship
identity, absent kind/role/direction assertion, or missing source basis.

## 5. Source, snapshot, and digest traceability

Every canonical assertion, enrichment assertion, condition, limitation, and
successor link SHALL identify the asserting source/authority and bounded source
locator. In a realization, that locator is represented by a `sourceReference`
to both:

1. the exact Phase 10.1 `source-snapshot` artifact ID; and
2. an exact source-owned anchor carried by or linked from that snapshot.

A locator supports navigation and accountability only. It neither transfers
source authority nor grants authorization to retrieve the source. A source
snapshot preserves selected evidence; it does not make its consumer the source
owner or permit invention of a missing canonical assertion.

For every snapshot kind other than `cml-baseline`, the source reference SHALL
retain the exact source-owned assertion anchor required by the selected basis.
For a `cml-baseline`, the source-owner-supplied
`basis.projectRelativePath` identifies only the bounded exact-byte baseline
file. It is not evidence of a particular CCDM assertion and SHALL NOT be
treated as an assertion-level anchor. If a realization needs a finer
source-owned anchor for an assertion and that anchor is unavailable, admission
SHALL fail rather than infer one from CML text or its path.

The realization's manifest inventory entry SHALL have `role = realization` and
shall bind the exact SHA-256 of the realization's raw bytes, as required by the
Internal-model Package Contract. The entry's `dependsOn` SHALL contain every
selected Phase 10.1 source-snapshot artifact actually consumed by this
realization. Every `sourceReference` snapshot ID SHALL name one of those
dependencies, and every required dependency SHALL resolve to a present manifest
artifact with `role = source-snapshot`. A source snapshot that is not consumed
by a realization need not be named by it.

Traceability retains source authority, identity, revision when available, exact
snapshot-supplied raw-source hash, selected source-snapshot artifact ID, and
the applicable source-owned anchor. The realization's in-band logical record
does not contain package revision, manifest inventory artifact ID/path, or its
own raw-byte digest; the external manifest/package context binds those values.
A change to recorded source basis, realization bytes, or declared dependency
creates a different candidate state; this contract does not select a replacement
or decide its approval or freshness disposition.

## 6. Explicit conditions and no-hidden-winner rule

The artifact SHALL retain each applicable condition as an explicit,
source-attributed record linked to the affected element, relationship,
assertion, or source reference. The admitted condition classes are:

| Condition | Required meaning |
| --- | --- |
| explicit absence | The attributed source expressly records no asserted fact at the bounded subject; it is not a synthetic negative fact. |
| ambiguity | The source or admitted evidence does not establish one interpretation. |
| conflict | Distinct attributed assertions disagree; all affected assertions remain visible. |
| unauthorized or redacted | Evidence is inaccessible or intentionally withheld; no replacement content or locator is inferred. |
| unavailable or stale | The source cannot presently be reached, or retained revision/hash is not current evidence of live freshness. |
| malformed | Supplied source evidence or bounded assertion cannot be admitted as the asserted basis. |
| limitation | A source-attributed qualification, coverage boundary, or known insufficiency. |

Absence, ambiguity, conflict, authorization/redaction, availability/staleness,
malformation, and limitations SHALL NOT produce a hidden preferred assertion,
synthetic fact, inferred endpoint, inferred relationship role, replacement
source, or unrecorded winner. A closed inventory, matching digest, or
structurally valid record does not establish semantic completeness.

## 7. Admission, rejection, and compatibility

Logical admission requires all of the following:

- exactly one complete scope tuple under section 1;
- exact Phase 9 identity and relationship references within that scope;
- canonical and enrichment assertions kept in their distinct authority lanes;
- exact endpoints, role/direction, source/authority, bounded source anchor, and
  condition/limitation links for each retained relationship or assertion;
- each source reference's exact snapshot-supplied `source.sha256` raw-source
  hash, with only revision and locator permitted to be unavailable; and
- an exact manifest `realization` entry whose raw-byte digest matches the
  artifact bytes; and
- a complete, resolvable mapping from every consumed source reference to a
  declared Phase 10.1 source-snapshot dependency.

The realization SHALL fail closed and be rejected, without repair or inference,
when its scope or semantic identity is unknown, dangling, or cross-scope; an
endpoint, semantic kind, role/direction, source authority, source-owned anchor,
required exact raw-source hash, or required basis is missing; canonical and
enrichment lanes are conflated; a condition is omitted or assigned a hidden
winner; a snapshot reference is unknown, non-snapshot, absent, or not in
`dependsOn`; the manifest role, dependency, or raw-byte digest is inconsistent;
provenance cannot be resolved; or a replacement is silently represented as the
prior identity. For a `cml-baseline`, a bounded file path is not a substitute
for an unavailable assertion-level source-owned anchor.

P10-20B defines the recognizable `ccdm-realization-v1` content profile/version
and canonical byte encoding. A consumer that does not recognize that
profile/version, cannot apply
these logical invariants, or cannot resolve its manifest/snapshot basis SHALL
reject the realization rather than interpreting it through a fallback,
label-based migration, or storage-specific default. Compatible evolution may
add only explicitly versioned semantics while preserving Phase 9 identity,
authority, no-hidden-winner, and raw-byte-binding invariants. It SHALL NOT
reinterpret a prior realization from provider/chat history or an inferred view
copy.

## 8. Continuity and phase boundaries

The durable realization records the shared Phase 9 CCDM ledger. It therefore
carries the P10-21 continuity invariant: Mono-Koto, Use Case, Entity, Event,
Structure, Classification, Workflow, and StateMachine projections SHALL consume
one shared semantic ledger. A later presentation-only binding may refer to an
existing element or relationship ID; it SHALL NOT create a canonical fact,
endpoint, relationship role, or missing semantic identity.

This slice does not prove that all eight projections can be rebuilt, select an
exact view-binding serialization, or authorize any presentation binding.
P10-21 owns that proof. Phase 10.3 owns projection, review, and approval; Phase
10.4 owns rehydration; and Phase 10.5 owns drift,
invalidation/reconciliation, human approval, and every CML application gate.
P10-20B alone owns the content-byte grammar and executable proof for this
logical contract.
