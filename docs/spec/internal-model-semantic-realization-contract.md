---
status: target
decision_scope: P10-20A--P10-20B / P104-TYPED-SEMANTIC-REALIZATION
updated_at: 2026-10-01
---

# Internal-model Semantic Realization Contract

This normative contract defines the logical content and admission boundary of
one portable internal-model `realization` artifact for P10-20. Its companion
design is [Internal-model Semantic Realization
Design](../design/internal-model-semantic-realization.md). The V2 package
envelope, inventory, and exact versioned dependencies remain governed by the
[Internal-model Package Contract](internal-model-package-contract.md). The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
remains authoritative for the meaning of every semantic identity, source fact,
and projection.

The [Typed Control Contract](internal-model-typed-control-contract.md) and
[Source Snapshot Contract](internal-model-source-snapshot-contract.md) govern
distinct logical record and artifact revisions, and source-owned attribution.
This target admits only `ccdm-realization-v3` / schemaVersion `3.0`, with package
V2 and source V2. Original P10-20 acceptance at commit
`fb3716d071692fb9e2540b7459ce0a5d31113c97` and the former association work are
historical evidence. V3 implementation authoring does not establish validation,
independent review, acceptance, or Phase 10.4 closure. No legacy reader, alias,
migration, inferred version, or fallback is admitted.

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
manifest role, artifact revision, or structural admission SHALL NOT be treated as
any of those claims.

## 2. Strict JSON V3 and read-only admission

The realization is strict UTF-8 JSON. BOM, malformed UTF-8/JSON, duplicate
members at any depth, trailing non-JSON data and missing/extra fields reject.
Harmless whitespace, object-key order and equivalent JSON escaping are admitted.
An encoder may sort keys and append LF as ordinary output. Re-encoding equality,
whole-file comparison, cached canonical bytes and content-derived identity have
no admission or permission role.
The root has exactly these fields:

```json
{"canonicalAssertions":[],"conditions":[],"elements":[],"enrichmentAssertions":[],"profile":"ccdm-realization-v3","realizationReference":{"recordId":"...","recordRevision":1},"relationships":[],"schemaVersion":"3.0","scope":{},"sourceReferences":[],"successorLinks":[],"traceability":{}}
```

`scope` has exactly `componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`. `traceability` has exactly the sorted,
unique `consumedSnapshotReferences` array. Each reference has exactly
`artifactId`, `artifactRevision`, and `role`, with role `source-snapshot`.
Artifact IDs are unique and ascending by UTF-8 order. Its exact set and size must equal the
selected manifest realization entry's `dependsOn` set and the set of
`sourceReferences.snapshotReference` selection. A reference resolves only its
exact captured ID/revision/role, never an existing ID or newest version.
`realizationReference` has exactly `recordId` and `recordRevision`. The producer
explicitly supplies both; recordRevision is a positive lexical JSON integer
fitting Long, with no string, sign, decimal, exponent, leading zero or overflow.
The existing opaque realization identity is its logical recordId, distinct from
semantic identities and manifest artifact identity/revision. No version is
inferred from another record, artifact, package or source.

An element has exactly `canonicalAssertionIds`, `conditionIds`,
`enrichmentAssertionIds`, `identity`, `kind`, and explanatory `label`.
A relationship has exactly the same three link arrays plus `direction`,
`identity`, `label`, `role`, `sourceElementIdentity`, and
`targetElementIdentity`. Element and relationship arrays are sorted uniquely
by exact identity. `direction` is exactly `source-to-target`,
`target-to-source`, or `undirected`. The selected Use Case is an element whose
exact `kind` is `use-case`.

Each assertion lane record has exactly `association`, `assertionId`, `conditionIds`,
`content`, `semanticIdentity`, `semanticIdentityKind`, and
`sourceReferenceId`. `semanticIdentityKind` is exactly `element` or
`relationship`; both lanes are separately sorted by `assertionId`, and an ID
cannot occur in both lanes. Each structural record lists exactly the assertion
and condition IDs which target that exact kind/identity. Every element and
relationship has at least one canonical assertion witness.

Each condition has exactly `affectedIdentity`, `affectedKind`, `conditionId`,
`detail`, `kind`, and `sourceReferenceId`. Its closed `kind` is one of
`absence`, `ambiguity`, `conflict`, `authorization-redaction`,
`availability-staleness`, `malformed`, or `limitation`. Conditions are sorted
uniquely by `conditionId`; no winner, replacement, or implicit condition field
exists. Every limitation carried by a referenced model-context fact must occur
as an attributed `limitation` condition with the same detail.

Each `sourceReferences` record has exactly `referenceId`, `snapshotReference`,
`source`, `sourceAnchor`, and `target`. `source` has exactly `authority`,
`identity`, `locator`, and `revision`; it must equal the admitted V2
source-snapshot metadata by semantic attribution equality. Authority and identity
are nonempty; locator and revision are explicit null or nonempty strings. Unknown
source revisions remain unknown even when all artifact and record revisions are
known. Matching unknown attribution may structurally admit recorded state but
does not establish semantic completeness or permit an action requiring known
source versions. `target` is either `null` or
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

Admission starts with exactly one project-bound package inventory/version pass.
It requires one present `role=realization` entry, validates every selected
snapshot with the current V2 validator before inspecting it, and uses
only captured ordinary bytes returned by that pass. Unconsumed snapshots remain
structurally captured but are not semantically parsed. A null handoff, reference,
collection or bytes Option, duplicate supplied source ID/reference, wrong role,
nonpositive or inconsistent reference, or absent exact dependency rejects before
a partial candidate. No filesystem reread occurs inside captured admission.
It rejects a missing or duplicate realization, unknown profile or field, wrong source
metadata/anchor, dependency mismatch, missing witness, cross-scope ID,
dangling endpoint, lane collision, omitted reported limitation, unsupported
direction, or malformed source basis. It performs no file write, approval,
CML application, projection, rehydration, source refresh, or semantic repair.

## 3. Logical record shape

The following is the logical shape mapped by the strict JSON V3 grammar in
section 2; it does not create a second JSON, YAML, Scala, or storage model.

| Logical field | Required meaning |
| --- | --- |
| `realizationReference` | Explicit logical recordId and positive recordRevision allocated by the producer; never derived from bytes, labels, paths, providers, ordering, artifact versions, or source versions. |
| `scope` | Exact `componentIdentity`, `projectionContextIdentity`, and `selectedUseCaseElementIdentity`; all are Phase 9 identities and the Use Case is an element in that scope. |
| `canonicalAssertions` | Selected canonical source assertions for used elements/relationships, with affected ID, kind or relation role, source/authority, snapshot reference, source-owned anchor, and condition/limitation links. |
| `enrichmentAssertions` | Bounded source-attributed evidence with the same trace form; it is a separate lane and SHALL NOT add, replace, or promote a canonical fact. |
| `elements` | Exact element IDs with semantic kind, explanatory non-key label, assertion, source, and condition/limitation references. |
| `relationships` | Exact relationship IDs with exact source/target element IDs, asserted role and direction, explanatory non-key label, assertion, source, and condition/limitation references. |
| `sourceReferences` | Each selected basis reference names an exact consumed source-snapshot ArtifactReference, source authority/identity and source-owned anchor. Source revision and locator retain explicit unavailable (`null`) forms. |
| `conditions` | Explicit absence, ambiguity, conflict, authorization/redaction, availability/staleness, malformed-source, and limitation records linked to affected IDs and source references. |
| `successorLinks` | Explicit source-attributed replacement links between prior and successor identities. |
| `traceability` | Complete consumedSnapshotReferences dependency set. Package revision and the manifest inventory entry's artifact reference/path are external package context, distinct from the logical record. |

An explanatory label is presentation text only. It SHALL NOT be a key,
uniqueness criterion, endpoint substitute, source locator, or evidence for
identity equality.

`canonicalAssertions` and `enrichmentAssertions` are separate collections even
when they concern the same semantic ID. A canonical assertion identifies what
the canonical source asserts. Enrichment may describe, qualify, or expose a
limitation of that assertion only with its own source attribution; it SHALL NOT
silently overwrite a canonical assertion or become canonical because it is
newer, more detailed, available, plausible, or convenient.

### Explicit V3 source-backed associations

Every V3 assertion requires `association`. It is JSON
`null` for an ordinary assertion. A non-null value is a closed object with
exactly `associationRole`, `relatedSemanticIdentity`, and
`relatedSemanticIdentityKind`. The related kind is exactly `element` or
`relationship`; the related identity is an exact retained identity in the same
Component and projection context. It is never a label, endpoint inference,
path, view copy, source hash, or newly allocated identity.

Only a canonical assertion whose own exact target is an existing relationship
may carry a non-null association. An enrichment assertion has
`association=null` and cannot promote an association claim. The closed role
vocabulary and related-target compatibility are:

| `associationRole` | Admitted related kind |
| --- | --- |
| `owner` | `element` or `relationship` |
| `subject` | `element` |
| `affected-relationship` | `relationship` |
| `affected-subject` | `element` |
| `affected-endpoint` | `element` |

For one asserting relationship and one role there is at most one admitted
association claim. A duplicate or a claim to a different related identity
rejects; the realization never selects one. The assertion's source reference
must itself target that asserting relationship, and its exact selected
source-snapshot witness content must be the exact semantic string
`association:<associationRole>:<relatedSemanticIdentityKind>:<relatedSemanticIdentity>`.
The existing source-reference target/anchor and witness checks apply without
relaxation. CML paths, labels, endpoint pairs, enrichment, view content, or
content hashes are not association witnesses. A separate supplied
`role:<token>` or `sequence-key:<key>` canonical witness retains its own exact
source reference and source-owned anchor; it need not share an association
anchor or reference.

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

## 5. Source and exact artifact traceability

Every canonical assertion, enrichment assertion, condition, limitation, and
successor link SHALL identify the asserting source/authority and bounded source
locator. In a realization, that locator is represented by a `sourceReference`
to both:

1. the exact V2 `source-snapshot` ArtifactReference (ID, revision, role); and
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

The realization's manifest inventory entry SHALL have `role = realization`
and an explicit positive artifact revision under the package contract. Its
`dependsOn` SHALL contain every exact source ArtifactReference consumed by this
realization. Every snapshotReference SHALL equal one of those dependencies,
which must resolve to a present captured artifact with role source-snapshot.
An unconsumed snapshot need not be named or semantically parsed.

Traceability retains source authority, identity, explicit available or unknown
source revision, locator, exact selected artifact reference and source-owned
anchor. Positive artifact/record revisions never substitute for source versions.
Producer-owned versions provide declared provenance, with no hash authenticity,
live freshness or undeclared-content-mutation guarantee. A producer must advance
the appropriate explicit version when its control meaning changes; admission
does not infer that history or choose a replacement or approval disposition.

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
| unavailable or stale | The source cannot presently be reached, or retained revision is not current evidence of live freshness. |
| malformed | Supplied source evidence or bounded assertion cannot be admitted as the asserted basis. |
| limitation | A source-attributed qualification, coverage boundary, or known insufficiency. |

Absence, ambiguity, conflict, authorization/redaction, availability/staleness,
malformation, and limitations SHALL NOT produce a hidden preferred assertion,
synthetic fact, inferred endpoint, inferred relationship role, replacement
source, or unrecorded winner. A closed inventory, matching declared references, or
structurally valid record does not establish semantic completeness.

## 7. Admission, rejection, and unsupported profiles

Logical admission requires all of the following:

- exactly one complete scope tuple under section 1;
- exact Phase 9 identity and relationship references within that scope;
- canonical and enrichment assertions kept in their distinct authority lanes;
- exact endpoints, role/direction, source/authority, bounded source anchor, and
  condition/limitation links for each retained relationship or assertion;
- each source reference's exact admitted V2 source metadata, with only source
  revision and locator permitted to be unavailable; and
- an exact manifest realization artifact reference with its declared revision; and
- a complete, resolvable mapping from every consumed source reference to a
  declared Phase 10.1 source-snapshot dependency.

The realization SHALL fail closed and be rejected, without repair or inference,
when its scope or semantic identity is unknown, dangling, or cross-scope; an
endpoint, semantic kind, role/direction, source authority, source-owned anchor,
or required basis is missing; canonical and
enrichment lanes are conflated; a condition is omitted or assigned a hidden
winner; a snapshot reference is unknown, non-snapshot, absent, or not in
`dependsOn`; the manifest role, exact revision or dependency is inconsistent;
provenance cannot be resolved; or a replacement is silently represented as the
prior identity. For a `cml-baseline`, a bounded file path is not a substitute
for an unavailable assertion-level source-owned anchor.

Only `ccdm-realization-v3` / schemaVersion `3.0` is recognized. V1/1.0,
V2/2.0 and all mismatched pairs reject outright. No decoder, branch, migration,
overload, accessor alias, inferred version or fallback retains old shapes.
Consumers SHALL preserve Phase 9 identity, source authority and no-hidden-winner
invariants and reject unresolved basis. Neither provider/chat history nor an
inferred view copy may reinterpret a prior realization.

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
The V3 executable migration is represented by
`InternalModelSemanticRealizationValidatorSpec`; coordinated downstream migration,
parent validation and independent review remain pending. This contract changes
no earlier Phase checklist/history and establishes no current Step acceptance.
