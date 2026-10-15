# Capability Query and Demand Diagnosis Contract

Status: CAP-11-PKG01 accepted after focused validation and independent whole-Step conformance; Phase acceptance pending.
Scope: PHASE-11 / CAP-11-03 / S02-query-implementation

## Current Step acceptance — 2026-10-15

The current Component-owned Usage implementation and real Directory/normal CAR
consumer query path are accepted after focused validation and independent
whole-Step conformance; see
[CAP-11-PKG01 acceptance](../phase/phase-11.md#cap-11-pkg01-acceptance--2026-10-15).
This acceptance supersedes earlier pending/authored-only annotations only for
CAP-11-PKG01. The existing normative text and historical records below remain
preserved. Participant own PKG01/Phase acceptance, full validation, reviews and
releases, CBD full Phase review/release and authorized Cozy main integration
remain pending. CBD aggregate full-suite validation remains deferred-not-run to
Phase 11.1; no successor acceptance is established.

## CAP-11-PKG01 current authored contract — 2026-10-15

The six immutable-session operations retain their original finite-scope matching
and source-failure behavior. The current Usage operation is
`traceComponentUsageCapability(reference: ComponentUsageCapabilityReference):
Either[QueryFailure, QueryResult[Vector[ComponentUsageTrace]]]`.
`DemandSelection.ComponentUsage(reference)` replaces the former Application
selector; `ComponentUsageTrace(usage,paths)`, `ComponentUsageRequirementPath` and
`DemandPath(usage,mapping,required,provider)` retain complete owner records and
attributed nested mappings/Required/provider/own-realization branches. No old API,
selector alias or wire conversion is retained. Exact matching includes every
record with the versioned reference across full Component/package/resource/source
keys, never a label-derived winner or a Cartesian join.

With no available selected Usage package, trace returns typed
UnavailableProjection. Diagnosis retains the complete original
`Vector[ComponentUsagePackage]` in
`MissingComponentUsageCapabilityProjection(scope,reference,reason,families)`
with `MissingComponentUsageReason.ProjectionNotSupplied`. If any family is
available, an absent exact reference traces as scoped successful empty. Its
diagnosis reason is ProjectionNotSupplied if any selected package is unavailable,
otherwise AbsentSelectedReference. An acquired empty projection is Available,
therefore differs from optional absence. A matching available record remains
traceable alongside an unavailable package, preserving both owners' witnesses.
An original source Left prevents a session and cannot become a missing-Usage gap.

The remaining six gap variants preserve existing known-fact boundaries:
MissingRequiredComponentCapability, NoProvider, IncompatibleProviderVersion,
MissingRealization, MissingSpecification and MissingEvidence. Provider matching
remains equal opaque version or an explicit directed same-identity compatibility
edge; all provider witnesses and only their own realizations remain ordered.
No reverse/transitive/default/semver/name inference is introduced. Specification
and Evidence remain separate; all four runtime observations stay Unknown.
`traceUseCaseDemand` and UseCase diagnosis retain the exact not-yet-admitted
unavailable value. Reverse Usage demand/discovery links supply no forward facts.

[C11Q26](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala)
proves equal references across owners, mixed availability and acquired-empty
versus absent-reference diagnosis. Existing C11Q01–24 retain their exact original
matching/gap/attribution responsibilities, including active C11Q24 with at least
100 successful applicable cases and zero discards.
[C11Q25](../../src/test-capability-producer/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQueryProducerIntegrationSpec.scala)
uses real Store/Remote/UsageOnly Directory intake and normal Contracts Store/Archive
for direct multiple-provider and Archive-own-empty-realization diagnosis.
C11Q27 packages the same First Store/Remote images through normal package-car,
then public CNCF -> accepted CBD -> catalog/query. It compares Usage definitions,
keys, sources, versions, mappings, Required/provider witnesses and gaps while
retaining distinct Directory/CAR input locations. Reading/catalog/querying starts
no producer and invokes no native procedure. All clauses are authored pending
parent validation and fresh independent Step/Phase acceptance; old acceptance
counts below remain historical, with no new release or main-merge claim.

## Historical baseline and authoring record

The remaining sections preserve the original CAP-11-01/02/03 designs, dated
authoring decisions and acceptance history. Old Application descriptions, old
module signatures and earlier suite/case counts describe those prior boundaries;
they are superseded for current implementation by CAP-11-PKG01 above and do not
claim acceptance of changed current inputs or provide compatibility behavior.

## Current Component Usage adoption — 2026-10-15

The [Component Usage adoption contract](component-usage-capability-adoption.md)
supersedes Application-family names, ownership, wire and query APIs in the
baseline below. The Usage family is inductive and Component-owned, separate
from the deductive Component contract. Implement the naming map, explicit owner
context and source/mapping provenance through this document's original semantic
operations; do not flatten either family or derive provider contracts from usage.
Delete old symbols instead of retaining aliases or forwarding APIs. Stale typed
call sites must fail compilation; update fixtures/portable data with the producer.
CAP-11-PKG01 owns the pending migration and focused acceptance. Earlier accepted
case counts, module signatures and review history describe the old implementation
and do not claim new acceptance or authorize backward compatibility.

## Authority and accepted ingestion/catalog handoff

The [Phase 11 contract](../phase/phase-11.md) and
[original checklist](../phase/phase-11-checklist.md) own the nine original
CAP-11-03 criteria. This document promotes the sixteen settled query decisions
below. The [query design](../design/capability-queries.md) explains their
responsibilities and attribution. Apply the
[repository rules](../rules/repository-rules.md).

The accepted [ingestion contract](capability-ingestion-contract.md) and
[ingestion design](../design/capability-ingestion.md), particularly C11I-D11–13,
retain acquisition, independently supplied provenance and current/history
selection. The accepted [catalog contract](capability-catalog-contract.md) and
[catalog design](../design/capability-catalog.md), particularly C11C-D07–10,
supply original finite owner keys, ApplicationRequirementPath, directed
compatibility, traceability and four distinct Unknown observations. See
[CAP-11-01 acceptance](../phase/phase-11.md#cap-11-01-acceptance--2026-10-15) and
[CAP-11-02 acceptance](../phase/phase-11.md#cap-11-02-acceptance--2026-10-15).
Those accepted prerequisites do not establish executable query behavior.

These are internal deterministic read-only semantic operations. Capability is a
non-instantiated contract. S02's six package-private query modules and
all 25 executable cases below are accepted after focused validation, independent
whole-Step conformance and mechanical naming closure; see
[CAP-11-03 acceptance](../phase/phase-11.md#cap-11-03-acceptance--2026-10-15).
The unavailable forward UseCase branch remains explicit;
no positive upstream demand integration is claimed.

S01-query-contract authored these two normative documents on 2026-10-15 without
query code or executable cases and received no Step acceptance or commit. The
later S02 authoring handoff on the same date supersedes that implementation
status while retaining all sixteen settled decisions and nine original criteria.

Current acceptance — 2026-10-15: the S01/S02 authoring and subsequent-closure
requirements paragraphs retained below are historical authoring plans. Focused
validation, independent whole-Step conformance and mechanical local naming
closure now establish CAP-11-03 acceptance; the local Step commit binds it.
The original conditional forward UseCase demand criterion is accepted only
through the typed unavailable branch; positive projection and trace remain
unadmitted and unexercised. Whole-Phase review/local release, participant own
closures and authorized later Cozy main integration remain pending. CBD aggregate
full-suite validation remains deferred-not-run to Phase 11.1.

## Settled semantic decisions

### C11Q-D01 — Admitted finite query scope

Package-private deterministic read-only queries consume one CapabilityCatalogModel.Catalog established by the existing portable validator with independently supplied CatalogSelection(snapshotReference,provenance). open(input: Either[Diagnostic,PortableProjection],selection: CatalogSelection) returns either the original Diagnostic or a query session. It delegates existing version, finite-scope, reference and provenance validation. Original source Left is propagated unchanged before any query or gap; successful empty is possible only after admission. Current/historical selection and native acquisition remain in CapabilityIngestion; an existing readCurrent/readHistorical and CapabilityCatalog.fromSnapshot readout can supply the projection. No new source reader, cache, snapshot selector, fake admission or fallback from failed current to historical success.

### C11Q-D02 — Typed query inputs and exact failures

All six operations retain the selected scope and original snapshot/provenance in results. Capability identity is original CapabilityReference(ModelElementId,contractVersion); Component selection is exact original ComponentCoordinate including Component version; Application reference is original versioned ApplicationCapabilityReference; UseCase reference is a qualified ModelElementId with no invented producer version. Exact identity/version fields are explicit and nonempty; missing query qualifiers/version yields original DiagnosticKind.Malformed, never latest/default. Query failure algebra separates Source(Diagnostic) from UnavailableProjection(UnavailableUpstreamProjection). Preserve all seven original Diagnostic kinds and all fields, including Unauthorized, Unavailable, Stale and Ambiguous.

### C11Q-D03 — Six operations and responsibility

Freeze semantic signatures: getCapability(lookup: CapabilityLookup), findProviders(required: CapabilityReference), findRequirements(component: ComponentCoordinate), traceApplicationCapability(reference: ApplicationCapabilityReference), traceUseCaseDemand(reference: ModelElementId), diagnoseCapabilityDemand(demand: DemandSelection). CapabilityLookup is Exact(reference) or Search(text). DemandSelection is Application(reference), Required(DeclarationRecordKey), or UseCase(reference). Exact Scala carrier fields/helpers and code/spec ownership were frozen by the parent S02 PLAN before code edits and are recorded below; no implementation worker decides them. All six results use typed failure or success, retain selected snapshot/provenance, and are internal runtime values. No new JSON/wire/service CML/Web/MCP exposure or authorization policy.

### C11Q-D04 — Exact lookup and discovery assistance

Exact getCapability retains all Provided and Required declaration records with the exact qualified identity and exact contractVersion, including different Component/package/resource/source witnesses, in admitted catalog order. Search explicitly uses nonempty literal substring assistance over original definition name, Present summary and Present description, case-insensitive with Locale.ROOT; absent metadata supplies no string. It returns original records and a discovery-only designation, preserving original values and order. Text, labels, normalization, descriptions and AI never affect matching, provider selection or trace links; discovery matches with another identity remain another identity.

### C11Q-D05 — Provider matching and compatibility witnesses

A provider must be a Provided declaration with exactly equal qualified ModelElementId. Equal opaque contractVersion is an exact match; otherwise only original admitted directed Compatibility records with exactly this identity, requiredVersion and providedVersion permit a match. All such original compatibility context/Source/Origin witnesses are retained; no additional owner restriction is invented. Return each matching Provided record once in original catalog order with SameVersion or ExplicitCompatibility(all matching original records), its own original realization vector and four unknown observations. Preserve separate same-identity incompatible candidate records in the search result. No winner, priority, deduplication across distinct owning keys, reverse/transitive compatibility, semver/default/latest interpretation or name matching.

### C11Q-D06 — Requirement lookup

findRequirements selects only Required declarations whose owning original ComponentCoordinate equals the complete explicit input, retaining all package/resource/source context witnesses in catalog order. Provided on the same Component remains distinct. An absent exact Component in a successfully admitted finite scope returns successful empty; it asserts no global nonexistence and supplies no NoProvider gap.

### C11Q-D07 — Application trace ownership

traceApplicationCapability on an Available Application family returns all Application records having exactly the versioned reference, in original document/record order. Each result retains that complete Application record and its original ApplicationRequirementPath sequence. For each Local Resolved endpoint, emit a branch for each original Required record key in endpoint order, then its compatible ProviderMatch vector; every provider owns only its own realization vector. Preserve repeated genuine mappings, all contexts and original edge Source/Origin without assigning a content/order-derived semantic identity. This nested ownership prevents independent-list Cartesian joins and sibling-realization leakage. An Available family with no exact Application returns successful empty. An Unavailable family returns the exact typed UnavailableProjection, never an empty Application vector.

### C11Q-D08 — Unresolved local and external endpoints

Keep every original ApplicationRequirementPath and boundary. A Local Unresolved endpoint records the explicit selected mapping/reference and supports only MissingRequiredComponentCapability at that boundary; no Required record, provider or realization is manufactured. An External Unresolved endpoint remains explicit unresolved external evidence with original target/Source/Origin and establishes no local MissingRequired/NoProvider/version/realization/role gap. Resolved Required branches retain exact original declaration keys and full records.

### C11Q-D09 — Forward UseCase demand remains unavailable

traceUseCaseDemand validates the qualified query input and returns the original UseCase UnavailableUpstreamProjection with not-yet-admitted reason/detail. diagnoseCapabilityDemand(UseCase) returns the same unavailable outcome and no downstream gap. Admitted UseCase model facts and Application demand/discovery reverse links are retained read-only and cannot construct a positive forward demand. This is the explicitly allowed unavailable branch of the original conditional acceptance; no fabricated positive carrier/producer/admission, fake exercised positive integration or silent scope reduction.

### C11Q-D10 — Demand diagnosis selections

Application demand diagnosis covers every exact matching Application context and each explicit mapping branch. A Required demand selects an exact admitted Required DeclarationRecordKey and uses the same provider/role engine without an Application ancestor; a key of wrong direction or malformed identity is Malformed, and an absent selected Required key is Unavailable at that selected record boundary, never NoProvider. A versioned Application absent in an otherwise Available admitted family produces only MissingApplicationCapabilityProjection with reason absent-selected-reference and the admitted family/scope. An explicitly admitted ApplicationFamily.Unavailable produces only this same typed gap with reason projection-not-supplied and the exact original availability value. Both represent the known missing Application boundary; neither permits downstream gaps over absent facts. Source failure prevents opening a query session and cannot be converted into this gap.

### C11Q-D11 — No provider versus incompatible version

For a real resolved Required branch, candidates are all Provided records with its exact qualified identity in the admitted finite scope. No candidates yields NoProvider attached to that exact Required owner and optional original Application mapping. If candidates exist but no compatible match exists, emit IncompatibleProviderVersion retaining all same-identity candidates, both opaque versions, original context and compatibility evidence searched; do not also emit NoProvider. If compatible matches exist, retain incompatible candidate observations but emit neither NoProvider nor IncompatibleProviderVersion. Diagnostics are about this known finite scope, never a global absence or execution result.

### C11Q-D12 — Missing realization is declarative

Each compatible provider whose own realization vector is empty produces MissingRealization retaining the provider declaration key, exact optional Required/Application path, and original realization Presence including absence reason/detail. A sibling provider/Required declaration/other Capability cannot satisfy this branch. Static absence is not runtime unavailable/unauthorized/guard failure or failed execution. Nonempty vectors retain all Operation/Workflow/StateMachine edges in original order and no invocation occurs.

### C11Q-D13 — Specification and Evidence coverage

Role coverage is a vector of original AttributedTraceability facts with exact owner, role, target, Source, Origin and boundary. For a compatible provider branch, applicable owners are only its enclosing Application if present, its Required declaration plus exact context-local Required definition, and that provider declaration plus exact context-local provider definition. Owners are a membership set so a fact is not duplicated because one owner appears twice, while repeated original role facts remain repeated. Preserve facts in the original catalog traceability order and keep Specification/Evidence separate. Emit MissingSpecification or MissingEvidence only when that role has no fact on this applicable known branch; attach all assessed owner references, original Presence reasons and path, without inspecting an external target or claiming quality/runtime failure. When a real Required branch has no compatible provider, assess only known Application/Required owners and label that narrower known-facts boundary; no assertion about an unknown provider. Local unresolved/External/no-Application-definition branches yield no downstream role gap.

### C11Q-D14 — Seven typed gap variants and stable order

The closed gap family contains MissingApplicationCapabilityProjection, MissingRequiredComponentCapability, NoProvider, IncompatibleProviderVersion, MissingRealization, MissingSpecification and MissingEvidence. Every variant retains the selected query scope/snapshot, the applicable full Application/mapping/Required/provider owner records and original evidence/provenance; no content hash or manufactured semantic identity. Diagnosis order follows original Application contexts, mapping occurrences, Required context witnesses and Provided matches. Within a resolved branch, provider absence/version gap comes first if applicable; otherwise per provider MissingRealization, then MissingSpecification, then MissingEvidence. For no compatible provider, absence/version comes before the two known Required-path role gaps. Keep repeated genuine branches; do not collapse by labels or sort opaque identities into another order.

### C11Q-D15 — Runtime states and presentation handoff

All original four distinct Unknown observations remain unknown. Query matches, native metadata, explicit references and static gaps prove no availability, authorization, guard, execution, Evidence truth or permission. Future Phase11.1 presentation must map Source diagnostics, family unavailability, successful empty, unresolved External facts and each typed gap distinctly and retain identity/provenance and discovery-only labeling. No strings disguised as a new authorization policy; no public exposure now.

### C11Q-D16 — Declared failure model and implementation order

The failure model is supplied typed query inputs over a finite previously validated semantic projection, with original source outcomes and explicit selection. Reuse existing carrier validation rather than adding transport, null-accident defense, hashes, byte invariance, CAS, replay, cache invalidation, freshness, concurrency, acquisition, global coverage or activation guarantees. S01 promotes these parent decisions into exactly two spec/design documents first. S02 parent freezes exact Scala types/APIs/modules and meaningful executable cases, then authoring, representative actual producer query evidence and affected-consumer accumulator, one independent whole-Step review and exact Step commit. S01 alone receives no Step acceptance/commit; original all nine CAP03 criteria remain unchecked until actual complete code/evidence. All earlier counters/authority/bases/epoch/ledger and participant closures/main integration remain owed.

## Six operations, query scope and outcomes

The signatures below retain the S01 semantic responsibilities. The exact
parent-frozen S02 carriers, APIs and module ownership follow this table.
Each success retains the selected scope, snapshot
reference and complete original provenance.

| Operation | Selected input | Success meaning |
| --- | --- | --- |
| `getCapability(lookup: CapabilityLookup)` | `Exact(reference)` or `Search(text)` | Original Provided and Required records; Search explicitly carries discovery-only meaning. |
| `findProviders(required: CapabilityReference)` | Exact qualified identity and required opaque contractVersion | Ordered compatible Provided matches and separate incompatible candidates, with original compatibility witnesses and owned realizations. |
| `findRequirements(component: ComponentCoordinate)` | Complete original Component identity and explicit Component version | Only exact owning Component's Required records, across all original contexts. |
| `traceApplicationCapability(reference: ApplicationCapabilityReference)` | Exact versioned Application reference | All exact Application records and their nested original mapping/Required/provider/realization branches. |
| `traceUseCaseDemand(reference: ModelElementId)` | Qualified UseCase identity; no invented version | Original typed UseCase unavailability in this admitted model. |
| `diagnoseCapabilityDemand(demand: DemandSelection)` | `Application(reference)`, `Required(DeclarationRecordKey)` or `UseCase(reference)` | Scoped ordered typed gaps on known facts, or the original typed failure/unavailability. |

The semantic entry is
`open(input: Either[Diagnostic, PortableProjection], selection: CatalogSelection)`.
An original Left propagates unchanged before validation, a session or gaps. A
Right delegates to the existing selected portable validator to establish one
`CapabilityCatalogModel.Catalog`. Independent selection compares the exact
snapshot reference and provenance. The query layer cannot select history or
turn failed current acquisition into historical success.

### Exact S02 package-private carriers and APIs

All model types live inside `private[runtime] CapabilityQueryModel`; the original
ingestion/catalog/upstream types retain their owners. Scope is the original
`provenance.scope`, and successful empty results retain the original selection.

| Carrier | Exact fields or closed cases |
| --- | --- |
| `QueryScope` | `snapshotReference: SnapshotReference`, `provenance: Provenance` |
| `QueryResult[+A]` | `scope: QueryScope`, `value: A` |
| `QueryFailure` | `Source(diagnostic: Diagnostic)`; `UnavailableProjection(value: UnavailableUpstreamProjection)` |
| `CapabilityLookup` / `LookupDesignation` | `Exact(reference: CapabilityReference)` or `Search(text: String)`; `ExactIdentity` or `DiscoveryOnly` |
| `CapabilityLookupResult` | `lookup: CapabilityLookup`, `designation: LookupDesignation`, `records: Vector[CapabilityRecord]` |
| `MatchBasis` / `ProviderMatch` | `SameVersion` or `ExplicitCompatibility(records: Vector[CompatibilityRecord])`; `provider: CapabilityRecord`, `basis: MatchBasis` |
| `ProviderSearch` | `required: CapabilityReference`, `candidates: Vector[CapabilityRecord]`, `matches: Vector[ProviderMatch]`, `incompatible: Vector[CapabilityRecord]`, `consideredCompatibility: Vector[CompatibilityRecord]` |
| `RequirementSearch` | `component: ComponentCoordinate`, `records: Vector[CapabilityRecord]` |
| `RequiredProviderTrace` | `required: CapabilityRecord`, `providers: ProviderSearch` |
| `RequirementPathTrace` / `ApplicationTrace` | `path: ApplicationRequirementPath`, `required: Vector[RequiredProviderTrace]`; `application: ApplicationRecord`, `paths: Vector[RequirementPathTrace]` |
| `DemandSelection` | `Application(reference: ApplicationCapabilityReference)`; `Required(key: DeclarationRecordKey)`; `UseCase(reference: ModelElementId)` |
| `DemandPath` | `application: Option[ApplicationRecord]`, `mapping: Option[ApplicationRequirementPath]`, `required: CapabilityRecord`, `provider: Option[ProviderMatch]` |
| `OwnerTraceability` | `owner: TraceabilityOwner`, `traceability: Presence[Vector[Traceability]]` |
| `CoverageBoundary` / `RoleCoverage` | `RequiredPathOnly` or `ProviderPath`; `boundary: CoverageBoundary`, `owners: Vector[OwnerTraceability]`, `specification: Vector[AttributedTraceability]`, `evidence: Vector[AttributedTraceability]` |
| `MissingApplicationReason` | `AbsentSelectedReference` with tag `absent-selected-reference`; `ProjectionNotSupplied` with tag `projection-not-supplied` |
| `DemandDiagnosis` | `selection: DemandSelection`, `applications: Vector[ApplicationTrace]`, `required: Vector[RequiredProviderTrace]`, `gaps: Vector[CapabilityGap]` |

`CapabilityGap` is closed over these exact cases:

```scala
MissingApplicationCapabilityProjection(scope: QueryScope, reference: ApplicationCapabilityReference, reason: MissingApplicationReason, family: ApplicationFamily)
MissingRequiredComponentCapability(scope: QueryScope, application: ApplicationRecord, path: ApplicationRequirementPath)
NoProvider(scope: QueryScope, path: DemandPath, search: ProviderSearch)
IncompatibleProviderVersion(scope: QueryScope, path: DemandPath, search: ProviderSearch)
MissingRealization(scope: QueryScope, path: DemandPath, original: Presence[Vector[Realization]])
MissingSpecification(scope: QueryScope, path: DemandPath, coverage: RoleCoverage)
MissingEvidence(scope: QueryScope, path: DemandPath, coverage: RoleCoverage)
```

The entry and six session methods have exactly these types:

```scala
CapabilityQueries.open(input: Either[Diagnostic, PortableProjection], selection: CatalogSelection): Either[Diagnostic, CapabilityQueries.Session]
getCapability(lookup: CapabilityLookup): Either[QueryFailure, QueryResult[CapabilityLookupResult]]
findProviders(required: CapabilityReference): Either[QueryFailure, QueryResult[ProviderSearch]]
findRequirements(component: ComponentCoordinate): Either[QueryFailure, QueryResult[RequirementSearch]]
traceApplicationCapability(reference: ApplicationCapabilityReference): Either[QueryFailure, QueryResult[Vector[ApplicationTrace]]]
traceUseCaseDemand(reference: ModelElementId): Either[QueryFailure, QueryResult[Nothing]]
diagnoseCapabilityDemand(demand: DemandSelection): Either[QueryFailure, QueryResult[DemandDiagnosis]]
```

`Session` is final with a `private[CapabilityQueries]` constructor, `val catalog:
Catalog` and `val scope: QueryScope` derived from the catalog's original selected
snapshot/provenance. `Nothing` deliberately prevents a positive forward UseCase
carrier. An original open failure remains `Either[Diagnostic, Session].Left`;
query input failures use `QueryFailure.Source` with selected scope/source and
Component where present. No admission-bypassing session constructor is exposed.

| Exact runtime module | Responsibility |
| --- | --- |
| `CapabilityQueryModel.scala` | The carriers, closed failures, selectors, coverage and seven gap cases above. |
| `CapabilityQueries.scala` | Existing validator admission and thin six-method orchestration; exact lookup, literal Locale.ROOT discovery and exact Required Component lookup. |
| `CapabilityQueryInputPart.scala` | Original qualified identity/text/path checks for explicit versions and the complete Required key; selected input diagnostic provenance. |
| `CapabilityProviderMatchingPart.scala` | Ordered Provided exact-identity candidates, same-version or every directed witness, separate incompatible originals. |
| `CapabilityDemandQueryPart.scala` | Original nested Application paths, direct Required selection, original family unavailability and stable scoped gap order. |
| `CapabilityTraceabilityCoveragePart.scala` | Ordered exact known owners, first owner membership only, original repeated facts and separate Specification/Evidence coverage. |

These six files are package-private in
`org.simplemodeling.textus.cbdsupport.runtime`. They add no source acquisition,
mutable index/cache, public wire/service adapter or native activation.

| Boundary | Outcome and retained meaning |
| --- | --- |
| Original source/readout or existing validator failure | `Source(Diagnostic)`; original Malformed, Unsupported, Incompatible, Unauthorized, Unavailable, Stale or Ambiguous kind and every original field. |
| Missing qualified query identity or required explicit version | Original `DiagnosticKind.Malformed`; no default/latest selection. |
| Unavailable Application family during trace | `UnavailableProjection(UnavailableUpstreamProjection)` with the exact original value. |
| Forward UseCase trace or diagnosis | Same original UseCase unavailable value, including not-yet-admitted reason/detail; no downstream gap. |
| Available admitted scope with no exact lookup, provider, Component requirements or Application trace match | Successful empty with selected finite scope/provenance; no global absence claim. |
| Wrong-direction or malformed Required selection | Malformed at the selected record boundary. |
| Absent exact selected Required key | Unavailable at that boundary; no NoProvider inference. |
| Missing selected Application during diagnosis | Only the known MissingApplicationCapabilityProjection boundary, as distinguished below. |
| Unresolved External mapping | Original External evidence, never a local empty/NoProvider/role-gap substitute. |

## Exact provider compatibility and nested attribution

A provider must be Provided and have the required exact qualified ModelElementId.
Equal opaque contractVersion yields `SameVersion`. A different version matches
only through admitted directed same-identity requiredVersion/providedVersion
edges, yielding `ExplicitCompatibility(all matching original records)`. The
witnesses retain their context, Source and Origin without an added owner policy.
Each matching Provided record appears once in catalog order; distinct owning
keys stay distinct. Wrong-version same-identity candidates remain observable.

Each trace owns this nesting:

```text
original ApplicationRecord
  -> original ApplicationRequirementPath mapping occurrence
       -> original Required DeclarationRecordKey and full CapabilityRecord
            -> each compatible Provided CapabilityRecord and match evidence
                 -> only that provider's original CapabilityRealizationRef vector
```

Resolved endpoint keys retain endpoint order. Repeated genuine mappings stay
repeated; collection order preserves observations and never creates semantic
identity. There are no independent-list joins, sibling realization borrowing,
provider winner, priority or display-name deduplication. Local Unresolved and
External Unresolved preserve the original endpoint, boundary, target and edge
Source/Origin; neither manufactures a Required declaration.

## Seven typed gap variants, owners and evidence

Every gap retains selected query scope/snapshot/provenance and the applicable
full original owner records and evidence. Specification and Evidence remain
separate roles. Assessed owner references and original Presence reasons stay
attached to the particular path, rather than a flattened identity list.

| Variant | Applicable known boundary and evidence |
| --- | --- |
| `MissingApplicationCapabilityProjection` | Absent exact versioned reference in Available family: reason `absent-selected-reference`, admitted family and scope. Unavailable family: reason `projection-not-supplied`, exact original availability value. Only this boundary is emitted. |
| `MissingRequiredComponentCapability` | A Local Unresolved Application mapping: enclosing full Application, exact original mapping/reference, boundary and Source/Origin. No fabricated Required/provider branch. |
| `NoProvider` | Real resolved Required owner with zero Provided exact-identity candidates in the admitted finite scope; retain optional enclosing Application/mapping. |
| `IncompatibleProviderVersion` | Real Required owner with same-identity candidates but no compatible provider: all candidate records, both opaque versions, contexts and compatibility evidence searched; exclude NoProvider. |
| `MissingRealization` | Compatible Provided owner with an empty own realization vector: full provider key/record, optional Required/Application path and original realization Presence including absence reason/detail. |
| `MissingSpecification` | No Specification fact on the applicable known owner membership set: full assessed owners, original Presence reasons and path. |
| `MissingEvidence` | Independently no Evidence fact on that same applicable known owner membership set, with the same retained assessment evidence. |

For a compatible provider branch the owner membership set comprises only its
enclosing Application when present, its Required declaration and exact
context-local Required definition, and that provider declaration and exact
context-local provider definition. Membership prevents duplication from a
repeated owner; repeated original AttributedTraceability facts remain repeated
in catalog traceability order, with role, target, boundary, Source and Origin.
When no compatible provider exists, assess only known Application/Required
owners and explicitly label that narrower known-facts boundary. Local unresolved,
External and no-Application-definition branches have no downstream role gap.
External targets are not inspected and role presence claims no quality or truth.

Diagnosis follows Application context order, mapping occurrence order, Required
context witness order and Provided match order. For each resolved branch an
applicable provider absence/version gap comes first. With compatible providers,
each provider emits MissingRealization, then MissingSpecification, then
MissingEvidence when applicable. Without a compatible provider, the absence or
version gap precedes the two known Required-path role gaps. Keep repeated genuine
branches; do not sort opaque identities or collapse by labels. Compatible and
incompatible candidates together retain excluded observations without a blocking
NoProvider or IncompatibleProviderVersion gap.

## Original criteria and executable acceptance

All nine original criteria are accepted through focused validation, independent
whole-Step conformance and mechanical local naming closure; see
[CAP-11-03 acceptance](../phase/phase-11.md#cap-11-03-acceptance--2026-10-15).
The table reproduces their meaning and connects them to settled decisions and
authored cases. Decision abbreviations D01–16 mean C11Q-D01–16 above. Every case
C11Q01–25 below is accepted after focused validation and independent Step review.

| Criterion | Original requirement | Decisions | Authored cases |
| --- | --- | --- | --- |
| 1 | Provide deterministic query operations equivalent to `getCapability`, `findProviders`, `findRequirements`, `traceApplicationCapability`, `traceUseCaseDemand`, and `diagnoseCapabilityDemand`. | D01–03, D06–10 | C11Q01, C11Q03, C11Q05, C11Q07, C11Q11, C11Q25 |
| 2 | Search by qualified Capability identity and non-authoritative semantic metadata while keeping semantic matching identity/compatibility based. | D02, D04–05 | C11Q03, C11Q04, C11Q09, C11Q24 |
| 3 | Find Components providing or requiring a Capability. | D05–06, D11 | C11Q06, C11Q07, C11Q08, C11Q10, C11Q19, C11Q24, C11Q25 |
| 4 | Trace an admitted Use Case demand through Application Capability and required Component Capability to all explicit provider/realization refs. | D07–09, D12–15 | C11Q06, C11Q08, C11Q11, C11Q12, C11Q16, C11Q20, C11Q23, C11Q24, C11Q25 |
| 5 | Return a typed unavailable/unsupported result when Use Case demand input is not supplied by an admitted upstream projection. | D09 | C11Q16, C11Q25 |
| 6 | Preserve unauthorized, unavailable, stale and ambiguous source outcomes; establish an empty/no-provider result only within an admitted query scope. | D01–02, D08–11, D14–15 | C11Q01, C11Q02, C11Q05, C11Q10, C11Q12, C11Q14, C11Q15, C11Q17, C11Q23, C11Q25 |
| 7 | Distinguish at least: missing Application Capability projection, missing required Component Capability, no provider, incompatible provider version, missing realization, missing Specification, and missing Evidence. | D08, D10–14 | C11Q13, C11Q14, C11Q15, C11Q17, C11Q18, C11Q19, C11Q20, C11Q21, C11Q22, C11Q23, C11Q25 |
| 8 | Prohibit semantic matching, provider selection, compatibility or trace construction by display-name/description similarity alone. | D04–05, D13 | C11Q04, C11Q09, C11Q22, C11Q24 |
| 9 | Add executable specifications for multi-provider, multi-realization, incompatible-version, partial-evidence and zero-provider cases. | D05, D07, D11–16 | C11Q06, C11Q08, C11Q10, C11Q11, C11Q17, C11Q18, C11Q19, C11Q20, C11Q21, C11Q22, C11Q23, C11Q24, C11Q25 |

| Case | Authored observable behavior | Original criteria | Status and exact source |
| --- | --- | --- | --- |
| C11Q01 | Original source failures preserve all seven Diagnostic kinds, exact fields and no query session/no gaps. | 1, 6 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q02 | Independent snapshot/provenance selection rejects stale input through original validator; no fallback/history acquisition. | 6 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q03 | Exact getCapability preserves qualified identity, opaque version, both directions and every distinct source context. | 1, 2 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q04 | Discovery-only name/summary/description text assistance returns originals but cannot make identity/compatibility matches. | 2, 8 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q05 | Incomplete qualified inputs or missing explicit version are Malformed, never latest/default selection. | 1, 6 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q06 | Multiple Provided Components and all three realization kinds remain ordered original matches with declaration ownership. | 3, 4, 9 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q07 | Same Component can Provide and Require; requirement lookup is exact complete ComponentCoordinate across contexts. | 1, 3 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q08 | Explicit same-identity directed compatibility preserves all original context/source/origin witnesses. | 3, 4, 9 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q09 | Reverse, transitive, semver-like and same-label/different-qualified-identity candidates do not match. | 2, 8 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q10 | Available admitted finite scope with zero exact providers returns successful empty provider search. | 3, 6, 9 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q11 | Multiple Application contexts, repeated mappings and multiple Required/provider branches preserve original nested attribution/cardinality. | 1, 4, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q12 | External unresolved mappings retain evidence and never fabricate Local Required/provider/gap facts. | 4, 6 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q13 | Local unresolved Application mapping yields MissingRequiredComponentCapability with exact owner/edge/provenance only. | 7 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q14 | Unavailable Application family traces as original unavailable; diagnosis emits only its known missing-Application boundary. | 6, 7 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q15 | Absent exact Application in Available admitted family traces as successful empty and distinguishes selected-reference missing gap. | 6, 7 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q16 | Forward UseCase query and diagnosis stay original typed unavailable even when reverse links/model facts exist. | 4, 5 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q17 | Real admitted Required branch with no identity candidates yields scoped NoProvider and never global nonexistence. | 6, 7, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q18 | Only wrong-version same-identity candidates yield IncompatibleProviderVersion with all candidate versions, not NoProvider. | 7, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q19 | Mixed compatible and incompatible candidates retain excluded candidates but do not invent a blocking version gap. | 3, 7, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q20 | Compatible provider with empty own realization Presence yields MissingRealization without borrowing sibling edges/runtime inference. | 4, 7, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q21 | Specification present/Evidence absent and the opposite yield separate role gaps over the same exact applicable owners. | 7, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q22 | Role evidence on an unrelated Application/Capability/provider cannot satisfy the current path; no unknown-provider role inference. | 7, 8, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q23 | Stable gap order and repeated genuine branches preserve full original Source/Origin and four separate Unknown observations. | 4, 6, 7, 9 | Accepted after focused validation and independent Step review; [CapabilityDemandDiagnosisSpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala) |
| C11Q24 | Active generated finite scopes verify exact-versus-explicit-directed matching, independent expected cardinalities and isolation: minimum100successes/zero discards, no filtered invalid cases. | 2, 3, 4, 8, 9 | Accepted after focused validation and independent Step review; [CapabilityQuerySpec.scala](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQuerySpec.scala) |
| C11Q25 | Real normal Cozy producer/public CNCF Factory/current CBD Snapshot flows to query matching/Application trace/gaps without CML reparsing, activation or additional provider reads. | 1, 3, 4, 5, 6, 7, 9 | Accepted after focused validation and independent Step review; [CapabilityQueryProducerIntegrationSpec.scala](../../src/test-capability-producer/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQueryProducerIntegrationSpec.scala) |

## Declared failure model and implementation order

The failure model is typed query input over one finite previously validated
semantic projection with independently supplied selection and original source
outcomes. Existing carrier validation owns transport, reference and provenance
checks. No new transport, null-accident defense, hashes, byte invariance, CAS,
replay, cache invalidation, freshness, concurrency, acquisition, global coverage
or activation guarantee belongs to these queries.

S01 promoted this specification and its design first. S02 received parent-frozen
exact Scala types/APIs/modules and owned executable specifications before edits.
Behavioral scenarios use semantic AnyWordSpec `should`/`which` and adjacent
Given/When/Then. The active generated matching/attribution property requires at
least 100 successes and zero discards, valid finite scopes without filtered
invalid cases, independently expected cardinalities and isolation. Source-shape,
Markdown and closure tests cannot establish these behaviors.

S02 authored all ten programs and the two normative handoffs. Subsequent closure
requires one representative actual normal Cozy
producer/public CNCF Factory/current CBD Snapshot query readout and an affected
query/policy/demand-diagnosis plus original ingestion/catalog/control/output-
exclusion accumulator. Existing real producer proof is a prerequisite, not proof
of the newly authored query behavior. Parent-owned focused validation precedes
one independent whole-Step review and the exact Step commit. S01 alone receives
no Step acceptance or commit. The original authority, bases, epoch and closure
records remain in force; participant full validation/reviews/releases and the
already-authorized eventual Cozy main integration remain owed. CBD aggregate
full-suite validation stays deferred-not-run to Phase 11.1.

## Successor presentation handoff

[Phase 11.1](../phase/phase-11.1.md) later consumes this frozen internal contract.
It must present Source diagnostics, family unavailability, successful empty,
unresolved External evidence and each of the seven typed gaps distinctly, with
original qualified identity, opaque versions, scope and provenance. Text-assisted
results retain a discovery-only designation. Availability, Authorization, Guard
and execution result preserve four separate original Unknown observations.
Static matches, native metadata and Evidence references establish no runtime
availability, authorization, guard, execution success, Evidence truth or
permission. No public JSON/wire, service CML, Web/MCP surface or new authorization
policy is introduced here; successor execution is outside CAP-11-03.
