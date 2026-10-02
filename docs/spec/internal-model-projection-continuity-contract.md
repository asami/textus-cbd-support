---
status: target
decision_scope: P10-21 / P104-TYPED-PROJECTION-CONTINUITY
updated_at: 2026-10-01
---

# Internal-model Projection Continuity Contract

This normative contract preserves Phase 9 continuity and defines only
`ccdm-projection-binding-v3` / schemaVersion `3.0` paired with
`ccdm-realization-v3` / schemaVersion `3.0`, package V2 and source V2.
Its companion is the [Internal-model Projection Continuity
Design](../design/internal-model-projection-continuity.md). The
[Internal-model Package Contract](internal-model-package-contract.md) remains
authoritative for the manifest, inventory and exact versioned dependencies.
The [Typed Control Contract](internal-model-typed-control-contract.md) owns
explicit record/artifact references, and the [Source Snapshot
Contract](internal-model-source-snapshot-contract.md) owns source attribution.
The [Internal-model Semantic Realization
Contract](internal-model-semantic-realization-contract.md) remains authoritative
for the selected realization and its CCDM ledger. The [Canonical Component
Design Model Contract](canonical-component-design-model-contract.md) is the
sole semantic authority.

This binding is an organization of an accepted realization, not a second CCDM
or a new package role. P10-21B2 reconstructs all eight values only through the
existing Phase 9 `create` APIs; it does not extend their public DTOs or recast a
binding-local value as CCDM. Original P10-21 implementation and validation are
historical evidence. Current V3 authoring requires coordinated consumer
migration, parent validation and independent review; it establishes no Step
acceptance or Phase 10.4 closure and alters no earlier Phase history.

## 1. Selected binding and authority

Continuity admission starts with the one verified package inventory/version pass
required by the package contract. It selects exactly one present recognized
continuity-family `role=projection` artifact and exactly one accepted
`role=realization` artifact. The continuity family is exactly
`ccdm-projection-binding-v3` / `"3.0"`; duplicate continuity artifacts reject.
Recognized candidate-family artifacts may coexist and are skipped only for
continuity selection by the package contract's current classifier. Every
considered projection has strict JSON and a recognized exact profile/version
pair; unknown, legacy or malformed pairs fail
closed and are never a fallback. The selected continuity artifact's manifest
`dependsOn` SHALL contain that realization's exact ID/revision/role reference;
the realization continues to name its
selected source snapshots. A package may remain structurally valid without a
continuity artifact, but SHALL fail P10-21 continuity admission. Missing,
duplicate, absent, cross-package, or non-dependent selections SHALL fail
without repair or fallback.

The binding's `realizationArtifactReference` SHALL equal the selected captured
realization reference exactly, including its positive artifact revision and
`realization` role. Its `bindingReference` is a separately allocated logical
record identity/revision, distinct from the carrier artifact reference and the
selected realization artifact. Its version need not equal a carrier artifact,
realization record, package carrier or source revision. Its `scope` SHALL equal
that realization's exact `componentIdentity`,
`projectionContextIdentity`, and `selectedUseCaseElementIdentity`; no other
scope fact may be repeated. The selected realization and its accepted source
snapshots are the sole CCDM semantic ledger. A projection-local record,
artifact ID, label, path, hash, layout, display order, tie key, view copy, or
inferred similarity SHALL NOT create, recover, or repair identity, role,
endpoint, direction, attribution, condition, source sequence, or any other
semantic fact.

## 2. Strict presentation-independent V3 serialization

The artifact is strict UTF-8 JSON. BOM, invalid UTF-8, malformed JSON, duplicate
members at every depth and trailing non-JSON data reject. Harmless object-key
order, insignificant whitespace and equivalent JSON escaping are admitted.
Writers may emit deterministic keys and terminal LF on demand, with no cached
bytes or content identity. Re-encoding equality and whole-file comparisons are
not admission checks. Unknown, missing or additional keys at every defined
object level reject.

The root has exactly six fields: `bindingReference`, `profile`,
`realizationArtifactReference`, `schemaVersion`, `scope` and `views`:

```json
{"bindingReference":{"recordId":"binding-main","recordRevision":7},"profile":"ccdm-projection-binding-v3","realizationArtifactReference":{"artifactId":"realization-main","artifactRevision":23,"role":"realization"},"schemaVersion":"3.0","scope":{},"views":[]}
```

`schemaVersion` is exactly `"3.0"`; `profile` is exactly
`"ccdm-projection-binding-v3"`. `bindingReference` has exactly nonblank
`recordId` and positive `recordRevision`. `realizationArtifactReference` has
exactly token `artifactId`, positive `artifactRevision` and role `realization`.
Both revisions are explicit lexical JSON integers fitting `Long`: no sign,
fraction, exponent, leading zero, overflow, string, default or inferred version.
`scope` has exactly `componentIdentity`, `projectionContextIdentity`, and
`selectedUseCaseElementIdentity`, each equal to the corresponding realization
scope identity.

Only the V3 realization/binding pair is admitted. V1, V2 and mismatched pairs
reject; no retained decoder branch, compatibility alias, overload, migration,
fallback or inferred version exists. The closed scope, eight-family order,
record/link arrays, gap and sequence shapes preserve Phase 9 semantics. The
exact V3 realization retains associations and its complete source sidecar.

`views` has exactly eight entries, once each and in this fixed family order:

1. `MonoKotoProjection`
2. `UseCaseCommunicationProjection`
3. `EntityModelProjection`
4. `EventModelProjection`
5. `StructureViewProjection`
6. `ClassificationViewProjection`
7. `WorkflowProjection`
8. `StateMachineProjection`

Each view has exactly `family` and `records`; `family` is the exact listed
token and `records` is an array. Object-key order is presentation only.
An empty `records` array is structurally permitted. P10-21B2 acceptance,
separately, requires substantive nonempty output for all eight families.

An `element` or `relationship` record has exactly these fields:
`canonicalAssertionIds`, `conditionIds`, `enrichmentAssertionIds`,
`recordKind`, `semanticIdentity`, `sequenceAssertionId`, and `viewRole`.
`recordKind` is exactly `"element"`, `"relationship"`, or `"gap"`. A `gap`
record has those fields plus exactly `requestedFieldOrScope`; no other gap-specific or
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
must equal that token exactly and be directly witnessed by exactly one of the
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

### Direct attribution, sidecar, and condition compression

For a gap, attribution comes exactly from its sole admitted condition's source
reference, whose target is exactly `Some(kind, identity)` for that bound target.
The constructor SHALL not choose a first or last source or borrow another
relation's source. The admitted reference maps to
`ComponentDashboardSourceAttribution(sourceId = source.identity,
authorityScope = source.authority, sourceLocator = sourceAnchor)`; the full
reference and ledger remain in the sidecar.

For every normal record, DTO attribution is the one unique direct canonical
same-identity `kind:<viewRole>` or `role:<viewRole>` witness's source
reference. It describes the projected role only. Complete other source
references—including association and sequence witnesses—remain in the retained
V3 realization sidecar, rather than being selected, discarded, or collapsed
into DTO attribution. A missing, multiple, distinct-source, wrong-target, or
conflicting direct role witness rejects; no first/last source winner exists.
Two direct role assertions reject even when they share the same source
reference. Association, sequence, enrichment and condition sources remain
complete in the retained sidecar. The exact condition-compression and
single-condition gap rules remain unchanged. Explicit unknown source revisions
remain recorded unknowns; artifact/record versions never substitute for them or
establish readiness.

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

The all-eight reconstruction consumes closed associations
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
dependencies. The binding `realizationArtifactReference` equals that
realization's exact captured reference and the projection `dependsOn` contains
it. Supplied capture, projection, realization package, references, metadata,
bytes and dependencies must be non-null. IDs/revisions/roles must be valid,
dependency IDs sorted and unique, and self-dependency absent before content use.
The realization parser consumes `handoff.realizationpackage` and binding
admission selects `handoff.realizationpackage.realization.reference`, without
reopening a manifest, inventory or artifact. Null or malformed captured data
rejects through structured `operationInvalid` before a partial return.

For a normal V3 record, DTO attribution is the source reference of its one
direct canonical same-target `kind:<viewRole>` or `role:<viewRole>` witness.
Association and sequence sources remain in the realization/binding sidecar.
Missing, multiple, cross-target, or conflicting witnesses reject rather than
choosing a source winner. An endpoint DTO uses that endpoint's own bound
evidence and never inherits a parent relationship's attribution or condition.

Within a view, records are sorted by the exact tuple `(recordKind,
semanticIdentity, viewRole)` using ascending UTF-8-byte string comparison;
identical tuples reject as duplicate semantic selections. The fixed `views`
order above is part of the profile. Both semantic array orders are independent
of object-key order and JSON presentation.

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

Continuity admission fails closed for malformed UTF-8/JSON; an
unknown, missing, extra, or duplicate field; wrong profile, scope, family, or
order; duplicate projection selection; realization dependency mismatch;
unknown/cross-scope/duplicate identity; unsupported role; missing direct role
witness; endpoint or direction mismatch; lane promotion; omitted condition;
unwitnessed sequence; or a hidden source/condition winner. It does not repair
rejection from order, label, path, hash, view copy, provider history,
chat/session state, or inferred meaning.

## 6. Executable obligations and exclusions

The in-place V3 executable migration is
[InternalModelProjectionContinuityValidatorSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelProjectionContinuityValidatorSpec.scala).
It retains substantive all-eight normal and nested values, selected owners,
Value/Aggregate metadata, complete sidecars, association/sequence, condition and
gap behavior. It adds explicit independent reference revisions, lexical
rejections, exact realization revision/role/ID checks, malformed captured
metadata, legacy rejection, presentation variation and semantic writer
roundtrips. Given/setup establishes earlier realization admission where a later
semantic rejection is intended; active ScalaCheck varies identities, labels,
revisions and input encounter order. Fixture roots remain beneath
`target/internal-model-projection-continuity/work`; cleanup is confined to the
exact fixture subtree and closes its no-follow walk in `finally`.

Original P10-21 proofs are history, not validation of this V3 target.
Coordinated consumers, parent validation, independent review and Phase 10.4
acceptance/release remain pending. Producer-declared revisions provide provenance,
not hash authentication or undeclared-mutation detection.

Continuity reconstruction adds no package role, CCDM or Phase 9 public API,
mutable state, CML read/write, live source, approval, rehydration, Web/API
route, or Phase 10.3+ behavior. It does not authorize Phase 10.3 projection
workflow, review, or approval; Phase 10.4 rehydration; Phase 10.5 freshness,
reconciliation, or CML gate; publication; deployment; or Phase/checklist
closure.
