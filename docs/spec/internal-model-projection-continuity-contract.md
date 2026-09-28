---
status: draft
decision_scope: P10-21A--P10-21B1
updated_at: 2026-09-28
---

# Internal-model Projection Continuity Contract

This normative contract retains P10-21A's logical continuity admission and
defines P10-21B1's closed, canonical `ccdm-projection-binding-v1` artifact and
P10-21B2A's V2 interpretation for V2 realizations.
Its companion is the [Internal-model Projection Continuity
Design](../design/internal-model-projection-continuity.md). The
[Internal-model Package Contract](internal-model-package-contract.md) remains
authoritative for the manifest, inventory, digest, and dependency relation.
The [Internal-model Semantic Realization
Contract](internal-model-semantic-realization-contract.md) remains authoritative
for the selected realization and its CCDM ledger. The [Canonical Component
Design Model Contract](canonical-component-design-model-contract.md) is the
sole semantic authority.

This binding is an organization of an accepted realization, not a second CCDM
or a new package role. P10-21B2 reconstructs all eight values only through the
existing Phase 9 `create` APIs; it does not extend their public DTOs or recast a
binding-local value as CCDM. B1 neither proves all eight nor closes P10-21 or
Phase 10.2.

## 1. Selected binding and authority

Continuity admission starts with the one verified package inventory/digest pass
required by the package contract. It selects exactly one present
`role=projection` artifact and exactly one accepted `role=realization` artifact.
The projection artifact's manifest `dependsOn` SHALL name that realization;
the realization continues to name its selected source snapshots. A package may
remain structurally valid without a projection artifact, but SHALL fail P10-21
continuity admission. Missing, duplicate, absent, cross-package, or
non-dependent selections SHALL fail without repair or fallback.

The binding's `realizationArtifactId` SHALL identify that selected realization
exactly. Its `scope` SHALL equal that realization's exact `componentIdentity`,
`projectionContextIdentity`, and `selectedUseCaseElementIdentity`; no other
scope fact may be repeated. The selected realization and its accepted source
snapshots are the sole CCDM semantic ledger. A projection-local record,
artifact ID, label, path, hash, layout, display order, tie key, view copy, or
inferred similarity SHALL NOT create, recover, or repair identity, role,
endpoint, direction, attribution, condition, source sequence, or any other
semantic fact.

## 2. Canonical `ccdm-projection-binding-v1` and V2 bytes

The artifact is canonical UTF-8 JSON: no BOM, duplicate member,
insignificant whitespace, or trailing byte after exactly one LF is admitted.
Every object has ascending UTF-8-byte key order. A consumer parses without
repair and accepts only when canonical re-encoding reproduces the supplied
bytes byte-for-byte. Unknown, missing, duplicate, or additional keys at any
defined object level reject.

The root has exactly these five keys; its canonical representation therefore
orders them as `profile`, `realizationArtifactId`, `schemaVersion`, `scope`,
and `views`:

```json
{"profile":"ccdm-projection-binding-v1","realizationArtifactId":"...","schemaVersion":"1.0","scope":{},"views":[]}
```

`schemaVersion` is exactly `"1.0"`; `profile` is exactly
`"ccdm-projection-binding-v1"`; and `realizationArtifactId` is the nonempty
exact selected manifest realization artifact ID. `scope` has exactly the
canonical keys `componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`, each equal to the corresponding realization
scope identity.

V1 binding bytes and their historical semantics remain unchanged. A binding
which consumes `ccdm-realization-v2` is instead explicitly
`ccdm-projection-binding-v2` with `schemaVersion="2.0"`. V2 retains the same
closed root keys, scope fields, eight family order, record fields, complete
assertion/condition ID arrays, gap form, sequence field, canonical encoding,
and fail-closed rules specified here. It does not reinterpret a V1 binding or
add a second semantic model. Its selected realization is the exact V2
realization with its association assertions and complete source-reference
sidecar.

`views` has exactly eight entries, once each and in this fixed family order:

1. `MonoKotoProjection`
2. `UseCaseCommunicationProjection`
3. `EntityModelProjection`
4. `EventModelProjection`
5. `StructureViewProjection`
6. `ClassificationViewProjection`
7. `WorkflowProjection`
8. `StateMachineProjection`

Each view has exactly `family` and `records` (canonical key order `family`,
`records`); `family` is the exact listed token and `records` is an array.
An empty `records` array is structurally permitted. P10-21B2 acceptance,
separately, requires substantive nonempty output for all eight families.

An `element` or `relationship` record has exactly these canonical keys:
`canonicalAssertionIds`, `conditionIds`, `enrichmentAssertionIds`,
`recordKind`, `semanticIdentity`, `sequenceAssertionId`, and `viewRole`.
`recordKind` is exactly `"element"`, `"relationship"`, or `"gap"`. A `gap`
record has those keys plus exactly `requestedFieldOrScope`, in canonical key
order between `recordKind` and `semanticIdentity`; no other gap-specific or
independently asserted fact is allowed. `requestedFieldOrScope` is a nonempty,
bounded request/reason scope, not a claim of absence.

Every `semanticIdentity` is a nonempty exact identity of an in-scope
realization element or relationship. For an `element` or `relationship`, each
of the three ID arrays SHALL equal that target's complete corresponding sorted
realization link array, including an empty array where the realization has one.
An omitted, extra, swapped, duplicate, or lane-promoted ID rejects. For a
`gap`, canonical and enrichment IDs are empty; `conditionIds` is the target's
complete sorted condition array and is admitted only when it contains exactly
one condition. That sole ID SHALL resolve to a realization condition whose
`affectedKind` and `affectedIdentity` respectively determine and exactly equal
the referenced target's kind and identity. An element and a relationship may
share an opaque identity string, so a consumer SHALL never infer target kind
from that string. `requestedFieldOrScope` SHALL equal the admitted condition's
exact `detail` as its bounded scope/reason; it SHALL NOT add, paraphrase, or
invent prose. Thus a gap names one exact affected target and one exact admitted
condition rather than selecting a condition winner from a larger set.

`sequenceAssertionId` is JSON `null` except for the two ordered relationship
roles in section 4. A non-null value is a canonical assertion ID on that same
relationship; its selected source-snapshot-backed assertion content is exactly
`sequence-key:<opaque-key>`, where `<opaque-key>` is the nonempty literal
suffix. An enrichment assertion, prose, label, array position, manifest order,
or stable tie key is never a sequence witness. A missing or malformed witness
rejects an ordered record; an attributable gap is possible only under the
single-condition rule above.

## 3. Direct source witness and all-eight role compatibility

`viewRole` is a case-sensitive, closed Phase 9 constructor token. A bound
element's realization `kind`, or a bound relationship's realization `role`,
must equal that token exactly and be directly witnessed by at least one of the
target's canonical assertions. The witness must target that same identity and
have source-snapshot-backed content exactly `kind:<token>` for an element or
`role:<token>` for a relationship. Other assertions are retained by ID but are
not interpreted as roles.

The selected Use Case subject is the sole narrow exception: its `viewRole` is
`use-case`, its realization kind is exactly `use-case`, and it has a canonical
same-identity `kind:use-case` witness. Use Case grouping and flow roles are
only the exact existing constructor tokens `UseCaseCommunicationFlow` and
`UseCaseCommunicationFlowStep`; both are relationships and require the same
identity-local `role:<token>` witness. `UseCaseCommunicationFlowStep` is the
Use Case ordered role in section 2. No name-similar token is accepted.

The complete compatibility table follows. A blank cell means no record of that
kind is admitted for the family. A `gap` may request only one role listed in its
family and must still name the exact affected realization target and admitted
condition; it does not make that requested role a fact.

| Family | Element `viewRole` tokens | Relationship `viewRole` tokens |
| --- | --- | --- |
| `MonoKotoProjection` | `Mono`, `Koto` | `StructuralDomain`, `BehavioralTemporal` |
| `UseCaseCommunicationProjection` | `use-case` | `Actor`, `Goal`, `Trigger`, `Postcondition`, `DomainElement`, `Collaborator`, `RealizingWorkflow`, `MonoKoto`, `UseCaseCommunicationFlow`, `UseCaseCommunicationFlowStep` |
| `EntityModelProjection` | `EntityModelEntity`, `EntityModelValue`, `EntityModelAggregate` | `IdentityMetadata`, `OwnershipMetadata`, `LifecycleMetadata`, `AggregateBoundaryMetadata` |
| `EventModelProjection` | `EventModelCommand`, `EventModelEvent` | `EventModelCausalAssertion`, `EventModelConsequenceAssertion`, `EventModelAffectedDomainElementAssertion`, `EventModelGeneratedStateEffectAssertion` |
| `StructureViewProjection` |  | `StructureComposition`, `StructureAggregation`, `StructureAssociation`, `StructureContainment`, `StructureOwnership`, `IndependentExistence`, `Reassignment`, `DeletionLifecycle`, `Cardinality`, `Navigability` |
| `ClassificationViewProjection` | `ClassificationPowertypeDimension` | `ClassificationGeneralization`, `ClassificationSpecialization`, `ClassificationTrait`, `ClassificationCategory`, `ClassificationDimensionValue`, `ClassificationDimensionQualifier`, `ClassificationDimensionExclusivity`, `ClassificationDimensionCoverage`, `ClassificationDimensionMembership` |
| `WorkflowProjection` | `WorkflowProjectionWorkflow`, `WorkflowProjectionActivity`, `WorkflowProjectionParticipant`, `WorkflowProjectionDomainElement`, `WorkflowProjectionOperation`, `WorkflowProjectionEvent`, `WorkflowProjectionPublishedStateEffect` | `WorkflowProjectionFlowRelation` |
| `StateMachineProjection` | `StateMachineProjectionStateMachine`, `StateMachineProjectionState`, `StateMachineProjectionTrigger`, `StateMachineProjectionGuard`, `StateMachineProjectionAction`, `StateMachineProjectionActivity`, `StateMachineProjectionOperation`, `StateMachineProjectionEvent`, `StateMachineProjectionRule` | `StateMachineProjectionTransitionRelation`, `StateMachineProjectionTransitionAdjunctRelation` |

This is direct admissibility, not a conversion table. In particular, Mono does
not become Entity, Koto does not become Event, Structure does not become
Classification, Workflow flow does not become a StateMachine transition or
adjunct, and the selected `use-case` subject does not acquire another view role
through name similarity. A role not in this table, a wrong record kind, an
unknown identity, or a cross-scope identity rejects.

## 4. Relationship, evidence, sequence, and ordering rules

A relationship record inherits its exact source and target element identities,
endpoint roles, relationship role, and direction only from the realization. The
binding contains none of those fields. An endpoint override is an unknown field
and rejects; an invalid realization endpoint remains a P10-20 validation
failure. The binding SHALL NOT reverse, normalize, merge, complete, or replace
a relationship.

### Use Case flow graph and source-owned step ordering

The nested Phase 9 `flows[].steps` DTO is admitted only from this exact graph
over accepted realization relationships. The selected Use Case element `U` has
`kind=use-case`. A bound flow relationship `F` has
`role=UseCaseCommunicationFlow`, `sourceElementIdentity=U.identity`,
`targetElementIdentity=V.identity`, and `direction=source-to-target`, where
`V` is a distinct target element with a directly canonical source-backed
`kind:use-case-flow` witness. A bound flow-step relationship `S` has
`role=UseCaseCommunicationFlowStep`, `sourceElementIdentity=V.identity`,
`targetElementIdentity=W.identity`, and `direction=source-to-target`, where
`W` is a distinct target element with a directly canonical source-backed
`kind:use-case-flow-step` witness. `F` and `S` retain their own same-identity
canonical `role:<exact token>` witnesses, and `S` retains its own
`sequence-key:<nonempty>` witness.

Each selected `V` SHALL belong to exactly one `F`, and each selected `W` SHALL
belong to exactly one `S`. A selected `S` matches exactly one selected `F`
solely when `F.targetElementIdentity == S.sourceElementIdentity`; missing,
duplicate, ambiguous, cross-scope, or wrongly directed chains reject.
`F.identity` and `V.identity` respectively map to the flow's
`semanticRelationshipId` and `semanticTargetId`; `S.identity` and `W.identity` map
likewise for the step. `flow.steps` is grouped by that exact matched `V`, then
ordered by the exact source-owned sequence key. Duplicate sequence keys within
one flow reject. Array order, labels, loose endpoints, and presentation tie
keys never establish membership or order. This rule adds neither binding fields
nor a realization schema or API.

Canonical and enrichment assertion lanes remain separate. The complete
realization condition ledger stays explicit and attributable; the binding may
not omit a condition, select a source or condition winner, concatenate
different condition details, or promote enrichment evidence to canonical.
The returned continuity value must retain the realization and this complete
binding ledger beside all eight Phase 9 values. The existing Phase 9 DTOs each
hold one `ComponentDashboardSourceAttribution` and compressed
`ComponentDashboardCondition`; B2 may create a DTO only when those fields
represent the bound source and condition evidence without choosing or
collapsing a record. Otherwise it rejects the bound record or returns the
attributable bounded gap supported by that DTO. It SHALL never select first or
last evidence as a hidden winner.

### V2 direct attribution, sidecar, and condition compression

For V1, every bound normal record collects source-reference IDs from all
canonical assertions, all enrichment assertions, and all linked conditions.
For a gap, it collects the ID from its one admitted condition. All collected
references SHALL resolve to exactly one distinct source-reference ID, whose
`target` is exactly `Some(kind, identity)` for that bound target. Zero,
missing, distinct, wrong-target, or ambiguous references reject. The
constructor SHALL not choose a first or last source, or borrow another
relation's source. That reference maps to
`ComponentDashboardSourceAttribution(sourceId = source.identity,
authorityScope = source.authority, sourceLocator = sourceAnchor)`; the full
reference and ledger remain in the sidecar.

For V2, the DTO attribution is instead the one unique direct canonical
same-identity `kind:<viewRole>` or `role:<viewRole>` witness's source
reference. It describes the projected role only. Complete other source
references—including association and sequence witnesses—remain in the retained
V2 realization sidecar, rather than being selected, discarded, or collapsed
into DTO attribution. A missing, multiple, distinct-source, wrong-target, or
conflicting direct role witness rejects; no first/last source winner exists.
The existing exact condition-compression and single-condition gap rules remain
unchanged for V2.

`ComponentDashboardCondition` uses fixed adapter sentinels
`availability="unverified"` and `authorization="unverified"`, which are not
CCDM facts and cannot authorize navigation (`navigationTarget=None`). Its
remaining optional fields start as `None` and its limitations start empty. In
ascending condition-ID order, each exact condition `kind` and `detail` maps
without coercion: `absence` maps to `explicitAbsence=Some(detail)`;
`ambiguity` to `ambiguity=Some(detail)`; `conflict` to
`conflict=Some(detail)`; `malformed` to `malformedEvidence=Some(detail)`;
and `limitation` appends one exact detail to `limitations`.
`authorization-redaction` and `availability-staleness` append the typed literal
`kind:detail` to `limitations`, leaving `redaction` and `staleness` as `None`
because the combined source condition cannot be split into a narrower claim.
More than one condition for a single-valued optional slot rejects rather than
choosing or concatenating. Empty conditions leave every optional field empty.
Every nested Phase 9 DTO endpoint and record uses its own exact target evidence
under this rule; it never copies attribution or condition from its parent. Full
condition IDs, kinds, and sources remain in the sidecar. Unknown or
nonrepresentable conditions or DTO shapes reject; a bounded gap remains
admitted only by the frozen one-condition gap rule. No condition defaults to
available or admitted.

`WorkflowProjectionFlowRelation` is the Workflow ordered relationship role;
together with `UseCaseCommunicationFlowStep`, it is the complete set of roles
for which `sequenceAssertionId` is required and non-null. All other roles have
`sequenceAssertionId: null`. Presentation uses the existing constructor's
identity-first order and, only for these roles, the exact source-owned sequence
key. Output order cannot establish chronology, causality, authority, priority,
or a winner, and an artifact label or order cannot affect semantic
reconstruction.

For the later B2B all-eight constructor proof, V2 consumes closed associations
only as follows: `owner` relates a Structure assertion relation to its exact
parent Structure relation, a Classification dimension assertion to its exact
dimension element, a Workflow flow to its exact Workflow element, a
StateMachine transition to its exact StateMachine element, and a StateMachine
adjunct to its exact transition relationship. `subject` relates a
Classification relationship to its exact subject element. The optional
`affected-*` claims name only the exact affected IDs needed by Structure or
Classification DTOs. Entity metadata subject/target, Use Case flow/step graph,
Mono-Koto relations, and Event relations retain their already admitted exact
endpoint rules. B2B rejects missing, duplicate, cross-scope, wrong-kind, or
contradicted required associations; it cannot silently leave nested values
empty or choose the only visible owner.

The adapter consumes one verified package handoff: one present projection, one
present realization, and the realization's verified source-snapshot
dependencies. The binding `realizationArtifactId` equals that realization and
the projection `dependsOn` contains it. The realization parser consumes the
same handoff, without reopening a manifest, inventory, digest, or artifact.

For a normal V2 record, DTO attribution is the source reference of its one
direct canonical same-target `kind:<viewRole>` or `role:<viewRole>` witness.
Association and sequence sources remain in the realization/binding sidecar.
Missing, multiple, cross-target, or conflicting witnesses reject rather than
choosing a source winner. An endpoint DTO uses that endpoint's own bound
evidence and never inherits a parent relationship's attribution or condition.

Within a view, records are sorted by the exact tuple `(recordKind,
semanticIdentity, viewRole)` using ascending UTF-8-byte string comparison;
identical tuples reject as duplicate semantic selections. The fixed `views`
order above is part of the profile. Re-encoding must preserve both orders
byte-for-byte.

## 5. Fail-closed gaps and navigation suppression

Affected absence, ambiguity, conflict, authorization/redaction,
availability/staleness, malformed, and limitation conditions remain explicit
and attributable under the realization contract. A gap cannot manufacture
absence, a negative fact, a substitute endpoint, a source, a role, an inferred
sequence, or a condition winner. It has no authority beyond its exact target,
one admitted condition, and requested bounded field or scope.

The profile has no navigation field. Navigation is suppressed in P10-21B2
unless a separately implemented exact receiving contract is proven. Retaining a
relation does not authorize a broad lookup, proxy, dead link, name-based
target, inferred reverse relation, reauthorization, disclosure, or source
fetch.

Continuity admission fails closed for malformed or noncanonical bytes; an
unknown, missing, extra, or duplicate field; wrong profile, scope, family, or
order; duplicate projection selection; realization dependency mismatch;
unknown/cross-scope/duplicate identity; unsupported role; missing direct role
witness; endpoint or direction mismatch; lane promotion; omitted condition;
unwitnessed sequence; or a hidden source/condition winner. It does not repair
rejection from order, label, path, hash, view copy, provider history,
chat/session state, or inferred meaning.

## 6. B2B executable proof and exclusions

P10-21B2B reopens a manifest-bound package through the one-pass
package-to-realization-to-binding path and constructs all eight projection
values. Its canonical successful fixture is substantively nonempty in every
family. Executable specifications check label/order invariance and reject
unknown/cross-scope/duplicate selections, role/endpoint/dependency mismatch,
missing relationship or sequence witness, lane promotion, omitted condition,
and multi-source or multi-condition hidden winners. The focused representative
and package/realization accumulator passed; P10-21 Step review and acceptance
commit remain pending.

B1 introduced no parser or constructor. B2B adds no package role, CCDM or Phase 9 public API,
mutable state, CML read/write, live source, approval, rehydration, Web/API
route, or Phase 10.3+ behavior. It does not authorize Phase 10.3 projection
workflow, review, or approval; Phase 10.4 rehydration; Phase 10.5 freshness,
reconciliation, or CML gate; publication; deployment; or Phase/checklist
closure.
