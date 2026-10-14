# Capability Catalog and Attributed Trace Model Contract

Status: CAP-11-02 accepted after focused validation and independent whole-Step review.
Scope: PHASE-11 / CAP-11-02 / S01-catalog-contract and S02-catalog-implementation

The S01 authoring statements and future-case tables below retain their dated
scope. The S02 sections freeze and record the now-authored implementation and
executable scenarios; they claim no executed result or independent acceptance.

## Authority and accepted ingestion handoff

The [Phase 11 contract](../phase/phase-11.md) and
[original checklist](../phase/phase-11-checklist.md) own the completion boundary.
This specification settles the catalog semantics from the accepted
[ingestion consumer contract](capability-ingestion-contract.md) and
[ingestion design](../design/capability-ingestion.md), especially C11I-D10–13.
[CAP-11-01 acceptance](../phase/phase-11.md#cap-11-01-acceptance--2026-10-15)
supplies the ingestion prerequisite. The
[catalog design](../design/capability-catalog.md) explains these decisions.
The [repository rules](../rules/repository-rules.md) govern typed management
identity and bounded assurance.

### C11C-D01 — Selected accepted Snapshot

The catalog is a package-private read-only semantic view over one explicitly
selected accepted `CapabilityIngestionModel.Snapshot`. Construction consumes the
original Snapshot returned by `readHistorical`/`readCurrent`; acquisition,
snapshot selection, current/history state and their complete original Diagnostic
outcomes stay in CapabilityIngestion. No network, cache, storage, concurrency,
retry, replay or activation is added.

### C11C-D02 — Full context and provenance

Retain the snapshot reference and complete original Provenance, ordered
successful Discovered packages and Entry resources, Component coordinate/release,
package coordinate/version, acquisition location, resource path, producer/schema/ABI,
projection header Source, definition Source and independent declaration/edge
Source and Origin. Scope is the admitted finite scope, never a claim of global
completeness. CapabilityRecord is a semantic view, never a new competing Capability
identity or an encoded/content-derived management key.

## Typed records and owner keys

### C11C-D03 — Component and declaration ownership

Define an explicit ComponentContextKey from the admitted scopeId, Component
coordinate, package coordinate, resourcePath and original projection source
authority/path/revision. Retain optional source lines as provenance; do not use
line numbers, file order, display labels, mtime or bytes as semantic identity.
DeclarationRecordKey is that owning context plus the original
`DeclarationKey(component, direction, qualified CapabilityReference)`. Keep
Provided and Required distinct, including both directions on one Component.
Definition/reference contractVersion is separate from every other version domain.

## Exact definition, declaration and native semantic references

### C11C-D04 — Exact definition witnesses

Each CapabilityRecord retains its own full declaration, the exact resolved
definition and its context witness. Resolve definitions using the original
`Entry.admitted.capabilities`; do not globally collapse equal qualified identities
across distinct Component/package/source contexts. Retain definition
discovery/grounding/Presence and all ordered declaration/definition traceability
facts. Multiple genuine witnesses remain multiple records, while repeated
identical owning keys are Ambiguous; no winner or deduplication by name.

### C11C-D05 — Owned realization references

CapabilityRealizationRef retains the enclosing DeclarationRecordKey, complete
original Realization edge and exact TargetContract: explicit
Operation/Workflow/StateMachine kind, qualified model reference/boundary, target
Component, target contractVersion/generated ABI, edge Source/Origin and
native-contract source. A live view may retain the original ResolvedTarget metadata
observation. Portable semantic transport carries only those references, contracts
and provenance; it cannot serialize native procedures, construct a new native
object or establish native admission/execution.

## Application and path attribution and cardinality

### C11C-D06 — Complete Application ownership

Application records retain complete separately admitted Application documents,
versioned identity, every Presence field including Present(empty), demand/discovery
links, dependencies, componentMappings, grounding and traceability, and original
unresolved External evidence. ApplicationContextKey retains the independently
declared document authority/path/revision and versioned Application reference;
each mapping, dependency and link stays attached to that exact enclosing record
with its own boundary/Source/Origin. External unresolved is not a dangling Local
target.

### C11C-D07 — Owned ApplicationRequirementPath

Model ApplicationRequirementPath as an owning versioned Application record and its
particular original componentMapping followed only by exact matching Required
declaration records in the admitted scope. Preserve all matching requirements and
their separate context witnesses. CAP-11-03 later extends each such owned path to
compatible Provided declarations and only those declarations' own realization
edges. Never join independent endpoint vectors, manufacture cross-path Cartesian
products, infer Component owner from labels, or attach a sibling declaration's
realization. Mapping without a matching Required declaration remains an explicit
unresolved required endpoint for later typed diagnosis, not a fabricated record
or acquisition failure.

## Unavailable forward UseCase family

### C11C-D08 — Honest demand direction

CapabilityDemandTrace retains the original forward UseCase family availability.
This run has no admitted forward UseCase producer/schema and therefore returns
the exact typed
`UnavailableUpstreamProjection(family=UseCase,reason=not-yet-admitted,detail)`.
Authored Application-to-UseCase links and admitted model facts remain original
reverse-direction facts; neither creates a forward UseCase-to-Application demand.
Do not invent a positive demand carrier/admission path, mark positive integration
exercised or discard useful Application-to-Component paths.

## Separate Specification, Evidence and runtime observations

### C11C-D09 — Independent roles and observations

Specification and Evidence are independent role-bearing ModelReference edges
with their original boundary, Source and Origin, attached to their exact
definition/declaration/Application/path owner. Missing evidence never becomes
failed execution, and a Specification link never substitutes for Evidence.
Availability, Authorization, Guard and execution result remain distinct
observations; unknown without explicitly supplied observations. Static native
target resolution proves none of these runtime states.

### C11C-D10 — Directed compatibility evidence

Retain every original compatibility declaration with its Component/resource
context, exact qualified identity, requiredVersion, providedVersion, Source and
Origin. The later matching rule is equal version or one explicit directed
same-identity admitted edge. No reverse edge, transitive closure, semver
interpretation, inferred default/latest value or label similarity. Preserve the
evidence edge and all context witnesses rather than flattening independent version
domains.

## Portable semantic codec and validation responsibilities

### C11C-D11 — Portable semantics without admission

The internal portable catalog projection has an explicitly supplied schema
version, snapshot management reference, declared scope/provenance, original
ordered Component semantic projection and resolved-definition/target-contract
reference facts, complete Application family or explicit unavailable branch and
original unavailable forward UseCase family. Its codec roundtrips every semantic
reference, source/version, Presence, boundary, Origin, owner context and cardinality.
Delegate unchanged Component/Application transport shape and lexical/reference
domains to existing public CNCF and private CBD codecs; do not define a competing
upstream format. Decoding semantic transport does not create an accepted Snapshot,
public CNCF Discovered object, native runtime object, acquisition success,
freshness or permission.

### C11C-D12 — Carrier freeze and bounded validation

Before the S02 codec/code edit pass, parent PLAN must freeze the complete internal
carrier field/key shape and exact APIs from this settled semantic model, including
independent selected finite-scope context. No editing worker decides that transport
shape. The validator rejects malformed carriers, duplicate owning keys, dangling
Local/owner/declaration references, ambiguous exact candidates and realization
owner/target kind/version/ABI mismatch; preserve original Diagnostic kinds and
provenance. It validates only declared semantic links, with no new global cycle,
tamper, replay or concurrency guarantee.

### C11C-D13 — Responsibility boundaries

Implementation responsibilities are bounded CatalogModel (semantic
records/keys/availability), CatalogProjectionCodec with coherent
decoding/binding/encoding helpers, and CapabilityCatalog (accepted Snapshot to
semantic view). Existing ingestion/source selection, CatalogRuntime,
CarReviewCapabilityCatalog taxonomy, Job/approval programs and upstream generators
remain owners of their current behavior. Queries `getCapability`, `findProviders`,
`findRequirements`, `traceApplicationCapability`, `traceUseCaseDemand`,
`diagnoseCapabilityDemand` and seven typed gap variants are CAP-11-03; shared
Web/MCP views and authorization mapping are Phase 11.1. No API exposure/service
CML edits occur in this Slice.

## Future executable behavior obligations and original criterion map

The following eight criteria are the original CAP-11-02 requirements, numbered
here only for cross-reference. These are obligations, not completed criteria.

| Criterion | Original requirement | Decisions | Future cases |
| --- | --- | --- | --- |
| 1 | Typed qualified CapabilityRecord with exact provided/required source/version/ABI provenance. | D01–05, D10–12 | C11C01, C11C02, C11C09, C11C13, C11C14, C11C15 |
| 2 | Demand trace joins only explicit upstream UseCase -> Application -> Required Component facts; honest unavailable forward branch when absent. | D06–08 | C11C04, C11C05, C11C07 |
| 3 | Explicit typed Operation/Workflow/StateMachine realization refs without name inference. | D05, D07, D12 | C11C03, C11C06, C11C12, C11C15 |
| 4 | Separate Specification/Evidence references from Capability/demand/realization. | D04–06, D09 | C11C08, C11C09 |
| 5 | Distinct Availability/Authorization/Guard/execution result. | D05, D09, D11 | C11C08, C11C15 |
| 6 | Preserved one-to-many demand/provider/realization cardinalities. | D02–07, D11 | C11C01–06, C11C09, C11C15, C11C16 |
| 7 | Explicit edge attribution; independent endpoint lists cannot manufacture joins. | D03–07, D09–12 | C11C03–06, C11C08, C11C09, C11C11, C11C12, C11C14 |
| 8 | Semantic codecs/validators/executable specs for identity/cardinality roundtrip and dangling/ambiguous rejection. | D03–06, D10–12, D14 | C11C09–16 |

Decision abbreviations D01–14 mean C11C-D01–14 above. C11C01–16 below are future
observable behavior obligations. No executable scenarios are authored, run or
accepted by S01.

| Case | Future observable obligation | Criteria |
| --- | --- | --- |
| C11C01 | Exact Provided/Required identities, two direction records, original source/version/ABI and context witnesses. | 1, 6 |
| C11C02 | Distinct Components/resources providing one identity retain all records; equal display names do not merge different qualified identities. | 1, 6 |
| C11C03 | One declaration retains multiple Operation/Workflow/StateMachine realization refs with exact owner and target-contract provenance. | 3, 6, 7 |
| C11C04 | Complete Application documents/Presence/dependencies/mappings and unresolved External evidence remain ordered and lossless. | 2, 6, 7 |
| C11C05 | Multiple Application mappings and matching Required records retain every owned path, with no independent-list joins. | 2, 6, 7 |
| C11C06 | Two providers/realizations belonging to distinct declarations cannot be cross-attached by flattened endpoint lists. | 3, 6, 7 |
| C11C07 | Forward UseCase unavailable stays typed unavailable while authored reverse links and ApplicationRequirementPath remain available. | 2 |
| C11C08 | Separate Specification and Evidence roles/provenance never imply runtime Availability, Authorization, Guard or execution result. | 4, 5, 7 |
| C11C09 | Semantic portable roundtrip preserves Unicode, delimiter/case, full identity/version, original Source/Origin, Presence and cardinality, excluding native objects. | 1, 4, 6, 7, 8 |
| C11C10 | Repeated owning context/declaration/Application keys and ambiguous exact semantic candidates reject without a winner. | 8 |
| C11C11 | Dangling Local/declaration-owner refs reject; explicit unresolved External refs stay valid attributed evidence. | 7, 8 |
| C11C12 | Realization owner mismatch and target kind/identity/component/contract/ABI mismatch retain distinct malformed/incompatible diagnostics. | 3, 7, 8 |
| C11C13 | Opaque source and contract/producer/component/package/schema versions roundtrip independently; no normalization/latest inference. | 1, 8 |
| C11C14 | Original directed compatibility edge retains its owner/Source/Origin; no reversed/transitive/name-derived edge appears. | 1, 7, 8 |
| C11C15 | Actual accepted producer Snapshot yields all original public Factory semantic references and original native observations without rereading providers or activating procedures. | 1, 3, 5, 6, 8 |
| C11C16 | When codec is implemented, meaningful generated semantic roundtrips require at least 100 successful cases and zero discards; synthetic negative fixtures do not replace C11C15 actual producer proof. | 6, 8 |

## Remaining implementation, review and closure boundaries

### C11C-D14 — Authoring and acceptance boundary

This S01 authors only the settled semantic specification/design and exact accepted
journal projection; it claims no executable catalog, independent Step acceptance,
participant full review/release, positive forward UseCase integration or test
execution. CAP-11-02 stays open until all eight original criteria,
original-source/native integration behavior, semantic roundtrip/validator scenarios
and independent whole-Step acceptance pass. Preserve the exact original Phase 11
completion scope and deferred repository full suite to Phase 11.1; no successor
execution or Phase 14 acceptance.

S02 must first receive the parent-frozen carrier/API shape and exact source/spec
ownership, then author meaningful AnyWordSpec/GivenWhenThen/should scenarios,
including applicable active properties with at least 100 successes and zero
discards. Its parent selects one meaningful catalog representative and one
affected ingestion/control/output-exclusion accumulator. Existing actual producer
and ingestion evidence remains prerequisite evidence, not proof of this
unimplemented catalog. The eight-case actual producer proof may be reused where
causally relevant; synthetic negative fixtures cannot replace actual producer
consumption. No full suite occurs until its owning Phase release; CBD aggregate
full-suite validation remains deferred-not-run to Phase 11.1.

CAP-11-03 query implementation, independent Step acceptance and commits, participant
Phase full validation/reviews/releases, authorized Cozy main integration and the
original CBD Phase review/release remain separate obligations. No source, build,
test, fixture, public wire format, public service, upstream, maintenance cleanup,
management hash, encoded-content identity or runtime subsystem is added here.

## S02 complete internal carrier and API — 2026-10-15

All eight production objects are `private[runtime]` in
`org.simplemodeling.textus.cbdsupport.runtime`. Upstream identities, reference
types, Presence and Source/Origin remain the original CNCF types; Application,
Provenance and Snapshot references remain the existing ingestion types. These
records introduce no competing semantic identity or public transport.

### Exact semantic and live records

The following are the complete fields of `CapabilityCatalogModel` records,
in declaration order. Vectors preserve original order and repeated genuine
edges. Source lines remain in the original Source values and are excluded from
SourceKey identity.

| Record | Complete fields |
| --- | --- |
| SourceKey | modelAuthority:String, path:String, sourceRevision:String |
| ComponentContextKey | scopeId:String, component:ComponentCoordinate, packageCoordinate:PackageCoordinate, resourcePath:String, source:SourceKey |
| DeclarationRecordKey | context:ComponentContextKey, declaration:DeclarationKey |
| ApplicationContextKey | document:SourceKey, reference:ApplicationCapabilityReference |
| CatalogSelection | snapshotReference:SnapshotReference, provenance:Provenance |
| PortableProjection | schemaVersion:Int, snapshotReference:SnapshotReference, provenance:Provenance, packages:Vector[PackageProjection], application:ApplicationFamily, useCase:UnavailableUpstreamProjection |
| PackageProjection | input:PackageInput.Selected, targetContracts:Vector[TargetContract], resources:Vector[ResourceProjection] |
| ResourceProjection | resourcePath:String, projection:ComponentCapabilityProjection.Projection, resolvedDefinitions:Vector[CapabilityDefinition], realizations:Vector[ResolvedRealizationFact] |
| ResolvedRealizationFact | edge:Realization, target:TargetContract |
| ComponentResourceRecord | key:ComponentContextKey, input:PackageInput.Selected, resource:ResourceProjection |
| CapabilityRealizationRef | owner:DeclarationRecordKey, edge:Realization, target:TargetContract, observations:RuntimeObservations |
| CapabilityRecord | key:DeclarationRecordKey, context:ComponentResourceRecord, declaration:Declaration, definition:CapabilityDefinition, realizations:Vector[CapabilityRealizationRef] |
| CompatibilityRecord | context:ComponentContextKey, edge:Compatibility |
| ApplicationRecord | key:ApplicationContextKey, documentSource:Source, definition:ApplicationDefinition |
| ApplicationRequirementPath | owner:ApplicationContextKey, mapping:ComponentMapping, requiredEndpoint:RequiredEndpoint |
| CapabilityDemandTrace | useCase:UnavailableUpstreamProjection, application:ApplicationFamily, applicationPaths:Vector[ApplicationRequirementPath] |
| AttributedTraceability | owner:TraceabilityOwner, edge:Traceability |
| RuntimeObservations | availability:AvailabilityObservation, authorization:AuthorizationObservation, guard:GuardObservation, executionResult:ExecutionResultObservation |
| Catalog | projection:PortableProjection, resources:Vector[ComponentResourceRecord], capabilities:Vector[CapabilityRecord], applications:Vector[ApplicationRecord], compatibility:Vector[CompatibilityRecord], traceability:Vector[AttributedTraceability], demandTrace:CapabilityDemandTrace |
| NativeRealizationObservation | owner:DeclarationRecordKey, resolved:ComponentCapabilityDiscovery.ResolvedTarget |
| LiveCatalog | snapshot:Snapshot, catalog:Catalog, nativeObservations:Vector[NativeRealizationObservation] |

RequiredEndpoint has exactly `Resolved(records:Vector[DeclarationRecordKey])`
and `Unresolved(reference:CapabilityReference,boundary:ReferenceBoundary)`.
Derived Resolved vectors are nonempty. TraceabilityOwner has exactly
`CapabilityDefinition(context:ComponentContextKey,reference:CapabilityReference)`,
`Declaration(key:DeclarationRecordKey)` and
`Application(key:ApplicationContextKey)`.
AvailabilityObservation, AuthorizationObservation, GuardObservation and
ExecutionResultObservation are four distinct enums, each with
`Unknown(reason:String)`. Every statically derived realization observation uses
`Unknown("No runtime observation supplied.")` in each distinct domain.
Catalog contains semantic values only. Original Snapshot, Discovered entries,
ResolvedTarget, native contracts, snapshots, bindings and procedures remain
reachable only through LiveCatalog and its original Snapshot.

### Exact APIs and responsibility boundary

```scala
CapabilityCatalog.fromSnapshot(snapshot: Snapshot, schemaVersion: Int): Either[Diagnostic, LiveCatalog]
CapabilityCatalogProjectionCodec.decode(json: String, selection: CatalogSelection): Either[Diagnostic, Catalog]
CapabilityCatalogProjectionCodec.validate(projection: PortableProjection, selection: CatalogSelection): Either[Diagnostic, Catalog]
CapabilityCatalogProjectionCodec.encode(projection: PortableProjection, selection: CatalogSelection): Either[Diagnostic, String]
CapabilityCatalogBindingPart.bind(projection: PortableProjection, selection: CatalogSelection): Either[Diagnostic, Catalog]
CapabilityCatalogJsonPart.decode(json: io.circe.Json): Either[Diagnostic, PortableProjection]
CapabilityCatalogJsonEncodingPart.encode(projection: PortableProjection): Either[Diagnostic, io.circe.Json]
```

Schema and selected context are explicit, with no defaults or implicit-scope
overloads. Decode uses a duplicate-key-rejecting parser, the closed carrier
decoder and the same binder as validate/encode/fromSnapshot. Encode first
validates and then emits deterministic `noSpaces` JSON. ScopeJsonPart decodes
finite facts/definitions/contracts; ComponentJsonPart decodes original package
selection, resources and realization facts. Neither helper acquires a package.

CatalogSelection supplies the independently selected finite semantic context.
Comparison of snapshot/provenance records validates that declared context; it
does not prove acquisition, authenticity, permission, freshness or unchanged
transport. No native admission, provider reread, Factory call, file inspection,
native construction or activation occurs while building or decoding a catalog.

## S02 closed internal JSON contract

Internal `schemaVersion` is the explicit integral Int `1`. Valid unsupported
schema or closed tags produce Unsupported; malformed structure or lexical values
produce Malformed. Every object below has exactly its listed mandatory keys.
Mandatory values cannot be null except the explicitly nullable fields below.
Unknown/missing keys, duplicate JSON keys, implicit Presence, fractional revisions
and default versions are rejected. Record revisions are integral positive Longs
through the existing InternalModel factories and SnapshotReference wrapper.

| Object | Exact mandatory keys |
| --- | --- |
| Top projection | schemaVersion, snapshotReference, provenance, packages, application, useCase |
| Snapshot reference | recordId, recordRevision |
| Provenance | scope, packages, application, useCase |
| Scope | scopeId, source, modelFacts, termFacts, relationFacts, profileFacts, capabilities, targetContracts |
| Model fact | identity, kind, source; kind is the existing ModelKind.tag |
| Term/relation/profile fact | identity, source |
| TargetContract | targetKind, identity, component, contractVersion, generatedAbiVersion, source |
| PackageProjection | input, targetContracts, resources |
| PackageInput.Selected | scopeId, location, component, package |
| Location | kind, path; kind DIRECTORY or CAR |
| Component coordinate | namespace, id, version |
| Component reference | namespace, id |
| Package coordinate | coordinate, version |
| ResourceProjection | resourcePath, projection, resolvedDefinitions, realizations |
| ResolvedRealizationFact | edge, target |
| PackageExpectation | input, resources |
| ResourceExpectation | resourcePath, producer, schemaVersion, generatedAbiVersion, source |
| ApplicationExpectation | producer, schemaVersion, generatedAbiVersion, documents |
| UnavailableUpstreamProjection | family, reason, detail |
| Diagnostic | kind, detail, scopeId, component, source, location |
| ExternalReference | location, target, source, origin |

Scope.targetContracts is explicitly empty in original pre-native Provenance.
Package.targetContracts retains the original ordered native binding target
contracts as semantic facts. Location.path is the original `Path.toString`;
decode uses `Paths.get` without normalization, stat, existence checks, acquisition
or URI inference. ExternalReference.location retains its original JSON-pointer
text and receives no file-path normalization.

Application provenance uses exactly
`{status:"ACQUIRED",expected:<ApplicationExpectation>}`,
`{status:"UNAVAILABLE",value:<UnavailableUpstreamProjection>}` or
`{status:"REJECTED",diagnostic:<Diagnostic>}`. Rejected provenance propagates
its original Diagnostic and cannot validate as an accepted portable catalog.
ApplicationFamily uses exactly
`{status:"AVAILABLE",projection:<complete Application object>,unresolvedExternal:[...]}`
or `{status:"UNAVAILABLE",value:<UnavailableUpstreamProjection>}`.
Unavailable family tags are APPLICATION and USE_CASE. Forward USE_CASE retains
the original `not-yet-admitted` reason and unchanged detail, with no positive
UseCase carrier.

Diagnostic.kind uses the existing DiagnosticKind.toString. scopeId, component
and source are explicit JSON null for None or their original typed values for
Some; their keys are never omitted. ExternalTarget uses exactly
`{kind:"MODEL"|"TERM"|"RELATION"|"PROFILE",identity:<qualified value>}` or
`{kind:"COMPONENT_CAPABILITY"|"APPLICATION_CAPABILITY",reference:<versioned reference>}`.

### Inherited value shapes

Component and Application projection objects are embedded directly from their
unchanged public CNCF and private CBD encode/decode APIs. They are JSON objects,
never encoded strings or base64. The Component schema/ABI/producer/component/
package/declaration/compatibility format and complete Application document/
Presence format retain their existing owners.

| Value | Exact keys or branches |
| --- | --- |
| CapabilityDefinition | kind, identity, contractVersion, name, summary, description, source, discoveryReferences, grounding, traceability |
| Realization | declarationKey, targetKind, target, targetComponent, targetContractVersion, generatedAbiVersion, source, origin |
| DeclarationKey | component, direction, capability |
| CapabilityReference / ApplicationCapabilityReference | kind, identity, contractVersion; kind COMPONENT / APPLICATION respectively |
| Source | modelAuthority, path, sourceRevision, line; line null or positive Int |
| ModelElementId | modelId, elementId |
| TermId | vocabularyId, termId |
| RelationId | vocabularyId, relationId |
| ProfileId | vocabularyId, profileId |
| Producer | id, version |
| ModelReference | identity, boundary |
| ModelLink / Traceability | role, target, source, origin |
| Grounding | target, boundary, relation, profile, context, preferredLabel, source, origin |
| SemanticTarget | kind MODEL or TERM, identity |
| LocalizedLabel | text, language; language null or its original string |
| Presence | state PRESENT with value; state ABSENT with reason and detail |
| Origin | kind DECLARED only; kind DERIVED with ruleId and ordered nonempty inputs |

Directions, target kinds, roles and boundaries retain the upstream `.tag`.
AbsenceReason retains its existing `.toString`. Present(empty) is distinct from
Absent. Definition/header/edge source revisions, contract versions, producer
versions, component releases, package versions, generated ABI and local record
revision domains remain literal and independent. Native objects, runtime
observations, derived record/path vectors and execution/admission flags have
no portable encoder.

## S02 bounded capture, binding and attribution

Capture traverses original Snapshot.components and each original Entry in order.
Package input is discovered.acquired.input, contracts are
discovered.nativeInput.bindings.map(_.target), and each resource retains the
loaded path/projection, exact admitted.capabilities and admitted realizations'
edge/target facts. Snapshot Provenance, complete Application and unavailable
UseCase are unchanged. Live native observations retain each original
entry.realizations object under that resource context and its original
realization.edge.declarationKey. No native alias or selector is generated.

Binding validates SnapshotReference wrappers and original Provenance with the
existing IngestionSelectionPart._provenance/_compare, preserving Stale and other
original diagnostics. Selected package/resource coverage and order are exact.
Projection component/package must match input, and producer/schema/ABI/header
Source must match each original expectation. _source_compare preserves opaque
revision Stale. Repeated exact contexts, DeclarationRecordKeys and Application
owner keys are Ambiguous. Original whole-Snapshot component-plus-projection
SourceKey uniqueness and original package cross-resource uniqueness remain
in force.

For every resource, public ComponentCapabilityProjectionCodec and
ComponentCapabilityAdmission.admit bind against
`ScopeInput.Ready(provenance.scope.copy(targetContracts=package.targetContracts))`.
Duplicate exact component/identity/kind/contract/ABI target candidates are
Ambiguous. Resource.resolvedDefinitions must retain the entire ordered admitted
definition vector; missing facts are Malformed, competing exact candidates are
Ambiguous, and contradictory semantic witnesses are Incompatible. A declaration
selects only its own resource's exact definition candidate.

ResolvedRealizationFact vectors retain the exact ordered declared edges and
statically resolved contracts. Public validation/admission owns original
declaration/target diagnostics. A dangling attached fact edge/owner is Malformed;
fact target identity/Component mismatches are Malformed; kind/version/ABI
mismatches are Incompatible with their distinguishing paths; ambiguous targets
remain Ambiguous. Original scope/component/source are retained and upstream
locations receive only their actual carrier prefix. Conflicting attached facts
cannot exchange sibling edges. Absent/Present(empty) realization declarations
remain their original Presence values, with empty derived vectors.

Available Application requires original Acquired provenance and exact expected
headers through _application_observed. The unchanged Application codec/admission
validates complete documents against the finite Scope and all ordered resource
projection.capabilities; repeated Local definition candidates are never flattened
away. The complete admitted ExternalReference vector must agree exactly.
Unavailable branches must agree with provenance. ApplicationContextKey uses
document SourceKey and versioned Application reference independently of its
definition/edge sources.

Each original componentMapping becomes one ApplicationRequirementPath. Local
mappings retain all exact qualified identity/contractVersion Required keys.
A definition without a Required declaration yields explicit Unresolved. External
mappings remain Unresolved even if coincident Local facts exist. Repeated genuine
mappings remain repeated paths. Original compatibility edges retain direction,
context, Source, Origin and Presence. Full resolved-definition, declaration and
Application traceability retains its exact owner and per-owner order, with
distinct Specification/Evidence roles. No path extends to provider search or
query matching in CAP-11-02, and authored reverse links never create forward
UseCase demand.

## S02 authored executable scenario map

The three suites use AnyWordSpec semantic should/which grouping, should matchers
and adjacent Given/When/Then for each actual in-block and table/property body.
The following names map the authored scenarios to the original C11C01–16 and
the unchanged eight-criterion table above. These are authoring evidence only.

| Case | Suite | Authored scenario title |
| --- | --- | --- |
| C11C01 | CapabilityCatalogModelSpec | keep Provided and Required owner keys and all independent provenance |
| C11C02 | CapabilityCatalogModelSpec | retain every Component witness and equal-label distinct identities |
| C11C03 | CapabilityCatalogModelSpec | retain three explicit target kinds under their exact declaration owner |
| C11C04 | CapabilityCatalogModelSpec | retain complete documents, Presence, dependencies and External evidence |
| C11C05 | CapabilityCatalogModelSpec | retain repeated mappings, every matching Required context and explicit missing endpoints |
| C11C06 | CapabilityCatalogModelSpec | keep each provider realization attached to its own declaration |
| C11C07 | CapabilityCatalogModelSpec | preserve unavailable forward UseCase and useful authored reverse paths |
| C11C08 | CapabilityCatalogModelSpec | keep each role and owner while all four observations stay distinctly Unknown |
| C11C09 | CapabilityCatalogProjectionCodecSpec | preserve literal Unicode, full contexts, Presence and Source/Origin without native objects |
| C11C10 | CapabilityCatalogProjectionCodecSpec | reject repeated owner keys and ambiguous exact candidates without a winner |
| C11C11 | CapabilityCatalogProjectionCodecSpec | reject dangling Local and owner references while retaining explicit External facts |
| C11C12 | CapabilityCatalogProjectionCodecSpec | preserve owner/target mismatch kinds, field paths and independent Stale provenance |
| C11C13 | CapabilityCatalogProjectionCodecSpec | preserve every independent opaque version and reject malformed or unsupported wire fields |
| C11C14 | CapabilityCatalogProjectionCodecSpec | retain exactly the original directed compatibility fact and owner |
| C11C15 | CapabilityCatalogProducerIntegrationSpec | preserve accepted Store/Remote semantics, complete Application paths and exact native instances without rereading or execution |
| C11C16 | CapabilityCatalogProjectionCodecSpec | actively roundtrip generated semantic carriers with at least one hundred successes and zero discards |

C11C16 uses active ScalaCheck `minSuccessfulTests=100`
and explicit terminal assertions of at least 100 successes
and exactly zero discards. Valid-by-construction carriers vary qualified Unicode,
case/delimiters, independent version/source domains, Origin, Presence, one to
three Components, Applications, repeated mappings and explicit target kinds.
Per-sample assertions check complete typed equality and independently expected
owner keys, Required endpoints, path and realization cardinalities. No suchThat,
discard, ignored sample or retry-to-success substitutes for the property.

C11C15 uses existing actual withIntake, compiled First Store/Remote accessors,
original native inputs, complete primary Application, original ingestion/public
Factory and readCurrent. It captures the accepted Snapshot without reacquisition,
roundtrips full semantics, checks original native objects by reference and
requires each provider's read count to remain one. Poison implementation,
job-definition, guard and action bodies are excluded from JSON. Synthetic
fixtures construct only pure semantic carriers and never replace this proof.

The parent owns one actual producer representative followed by the exact
64-case seven-suite affected catalog/ingestion/Application/control/output-exclusion
accumulator, mechanical checks and fresh independent whole-Step review. The
planned 65 cases are not a passed result. CAP-11-02 remains OPEN pending those
gates. Existing S01 hygiene is retained without edit; prior six Hygiene and one
Development Candidate dispositions are unchanged. No existing Scala program is
edited, so prior same-file program debt is inapplicable. Queries, separate
participant closures/releases, eventual authorized Cozy main integration, CBD
Phase review/release and deferred Phase 11.1 aggregate validation retain their
original owners; successor and Phase 14 work remain unexecuted.
