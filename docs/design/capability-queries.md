# Capability Queries and Demand Diagnosis Design

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

## CAP-11-PKG01 current authored query flow — 2026-10-15

The [current query contract](../spec/capability-query-contract.md#cap-11-pkg01-current-authored-contract--2026-10-15)
uses traceComponentUsageCapability, ComponentUsageCapabilityReference,
DemandSelection.ComponentUsage, ComponentUsageTrace and ComponentUsageRequirementPath.
Session still opens only through the selected catalog validator; original source
Left remains a source failure. Catalog retains separate deductive Component
contracts and inductive owned Usage records with full Component/package/resource/
source/version keys. The exact Usage trace selects every matching owner context
and retains original mapping -> Required -> compatible Provided -> own realization
nesting. No old API forwarding or serialized conversion remains.

Availability is per selected package. No Available Usage yields typed unavailable
trace and a MissingComponentUsageCapabilityProjection diagnosis retaining all
original package families with ProjectionNotSupplied. With any Available family,
no exact reference gives finite empty trace; diagnosis uses ProjectionNotSupplied
when another family is unavailable, otherwise AbsentSelectedReference. Available
empty content differs from absence. Matching owners stay traceable despite an
unavailable sibling package. Missing Usage creates no downstream facts or gaps.
The other six gaps, exact opaque/directed compatibility and distinct Specification/
Evidence coverage keep their existing semantics. All four runtime dimensions stay
Unknown. Unavailable forward UseCase remains not-yet-admitted despite reverse links.

[C11Q26](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityDemandDiagnosisSpec.scala)
authors mixed availability and equal-reference owner isolation; C11Q24 remains an
active generated minimum100-success/zero-discard property.
[C11Q25/C11Q27](../../src/test-capability-producer/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQueryProducerIntegrationSpec.scala)
consume real Store/Remote/UsageOnly Directory products, normal primary Contracts
Store/Archive multiple-provider products and CARs normally packaged from the same
First Store/Remote images. Comparison covers meaning-bearing Usage definitions,
source/version, mappings, Required/provider witnesses and gaps while preserving
Directory versus CAR locations and original native instances without activation.
These authored clauses await parent validation, independent Step conformance and
separate Phase acceptance; earlier counts/signatures/acceptance below are historical.

## Historical baseline and authoring record

The remaining sections preserve the original CAP-11-01/02/03 designs, dated
authoring decisions and acceptance history. Old Application descriptions, old
module signatures and earlier suite/case counts describe those prior boundaries;
they are superseded for current implementation by CAP-11-PKG01 above and do not
claim acceptance of changed current inputs or provide compatibility behavior.

The [query contract](../spec/capability-query-contract.md) freezes C11Q-D01–16,
all nine original criteria and all 25 authored behavioral obligations. This design
records the exact parent-frozen S02 Scala APIs, carriers and modules. The
[Phase 11 checklist](../phase/phase-11-checklist.md) records executable acceptance;
the executable Step is accepted after focused validation, independent whole-Step
conformance and mechanical naming closure; see
[CAP-11-03 acceptance](../phase/phase-11.md#cap-11-03-acceptance--2026-10-15).
Apply the [repository rules](../rules/repository-rules.md).

On 2026-10-15 S01-query-contract authored only the normative spec/design, with no
query implementation or executable cases and no Step acceptance/commit. The later
S02 authoring on the same date supersedes that current implementation status and
preserves the original semantic decisions and acceptance distinctions.

Current acceptance — 2026-10-15: the dated S01/S02 authoring plan and its
pending/unchecked closure statements below are retained as historical records.
Current CAP-11-03 acceptance supersedes those statuses only; the local Step
commit binds acceptance. The original conditional forward UseCase criterion is
accepted only through its typed unavailable branch, with positive projection
and trace unadmitted and unexercised. Whole-Phase review/local release,
participant own closures, authorized later Cozy main integration and successor
execution retain their separate obligations. CBD aggregate full-suite validation
remains deferred-not-run to Phase 11.1.

## Current Usage model correction — 2026-10-15

Apply the [Component Usage adoption](../spec/component-usage-capability-adoption.md)
to this baseline architecture. Replace the old Application family with the
Component-owned inductive Usage model, retaining attributed many-to-many paths
and separate deductive contracts. Include Component/package acquisition ownership
in Usage contexts. Remove old typed API names rather than forwarding them; compile
errors expose missed callers. The implementation/signature sections below retain
historical evidence until CAP-11-PKG01 supplies revised implementation/acceptance.

## Selected readout and immutable query session — C11Q-D01–03

Accepted [ingestion](capability-ingestion.md) owns native acquisition and explicit
current/historical selection. Accepted [catalog construction](capability-catalog.md)
owns semantic validation and context-attributed records. The authored data flow is:

```text
original readCurrent/readHistorical outcome
  -> CapabilityCatalog.fromSnapshot readout
       -> original PortableProjection + independent CatalogSelection
            -> unchanged CapabilityCatalogProjectionCodec.validate
                 -> one immutable CapabilityCatalogModel.Catalog
                      -> CapabilityQueries.Session -> query/diagnosis
original Left Diagnostic -> original Source failure; no session and no gaps
```

The semantic entry is `open(input: Either[Diagnostic, PortableProjection],
selection: CatalogSelection)`. It propagates an original input Left before any
query and delegates all existing version, finite-scope, reference and provenance
validation on a Right. It introduces no admission authority, reader, cache,
snapshot selector or failed-current-to-history fallback. A portable semantic
projection establishes no acquisition or native execution success.

All six operations retain selected scope, snapshot and provenance. Exact inputs
use original CapabilityReference(ModelElementId, contractVersion), complete
ComponentCoordinate with Component version, versioned ApplicationCapabilityReference,
and qualified UseCase ModelElementId without an invented version. Missing
qualifiers or required explicit versions yield Malformed, never latest/default.
The internal failure distinction is Source(Diagnostic) versus
UnavailableProjection(UnavailableUpstreamProjection); all seven original
Diagnostic kinds and every original field survive.

The semantic operations are getCapability(Exact(reference) or Search(text)),
findProviders(required), findRequirements(component),
traceApplicationCapability(reference), traceUseCaseDemand(reference), and
diagnoseCapabilityDemand(Application(reference), Required(DeclarationRecordKey)
or UseCase(reference)). Their exact Scala carrier fields/helpers and module
ownership were frozen by the parent before S02 authoring and are recorded below.
None is a new public wire,
service CML, Web/MCP API or authorization policy.

## Exact six-module S02 implementation

All six modules are `private[runtime]` in
`org.simplemodeling.textus.cbdsupport.runtime`. Exact fields and all seven gap
signatures are in the [carrier contract](../spec/capability-query-contract.md#exact-s02-package-private-carriers-and-apis).

| Module | Frozen API and responsibility |
| --- | --- |
| `CapabilityQueryModel` | Original selected `QueryScope`, covariant `QueryResult`, closed lookup/failure/match/demand/coverage carriers and seven typed gaps. `DemandPath` retains full optional enclosing Application/mapping, full Required and exact optional compatible provider. |
| `CapabilityQueries` | `open(Either[Diagnostic, PortableProjection], CatalogSelection): Either[Diagnostic, Session]`; original Left first, original validator on Right. Final Session has private constructor, original `catalog` and derived `scope`, and exactly the six methods below. |
| `CapabilityQueryInputPart` | `capability`, `component`, `application`, `useCase`, `required`, `search`, each `(declared input, QueryScope): Either[Diagnostic, Unit]`. Reuses original model/text/path validation. Required validates direction, full identity/version/context/package/resource/SourceKey and Component equality. Search requires only nonempty literal text. |
| `CapabilityProviderMatchingPart` | `find(Catalog, CapabilityReference): ProviderSearch`. Filters original Provided identity candidates and same-identity compatibility in catalog order; exact opaque equality or all exact directed pair witnesses; no owner restriction, sorting or winner. |
| `CapabilityDemandQueryPart` | `trace(Catalog, ApplicationCapabilityReference): Either[QueryFailure, Vector[ApplicationTrace]]`; `diagnose(Catalog, QueryScope, DemandSelection): Either[QueryFailure, DemandDiagnosis]`. Preserves original endpoint/key nesting and emits only gaps supported at each known boundary. |
| `CapabilityTraceabilityCoveragePart` | `assess(Catalog, DemandPath): RoleCoverage`. Application, Required declaration/definition, compatible Provided declaration/definition in order; first owner membership only; original repeated attributed facts and Presence retained. |

The six Session return types are:

```scala
getCapability(lookup: CapabilityLookup): Either[QueryFailure, QueryResult[CapabilityLookupResult]]
findProviders(required: CapabilityReference): Either[QueryFailure, QueryResult[ProviderSearch]]
findRequirements(component: ComponentCoordinate): Either[QueryFailure, QueryResult[RequirementSearch]]
traceApplicationCapability(reference: ApplicationCapabilityReference): Either[QueryFailure, QueryResult[Vector[ApplicationTrace]]]
traceUseCaseDemand(reference: ModelElementId): Either[QueryFailure, QueryResult[Nothing]]
diagnoseCapabilityDemand(demand: DemandSelection): Either[QueryFailure, QueryResult[DemandDiagnosis]]
```

`Session` has a `private[CapabilityQueries]` constructor and `val catalog: Catalog`;
`val scope: QueryScope` retains the projection's exact snapshot reference and
provenance, whose `scope` remains original. `QueryResult` wraps every success,
including empty. `Nothing` prevents a positive forward UseCase result shape.
`RequiredProviderTrace` owns its `ProviderSearch`; `RequirementPathTrace` owns
the original mapping and Required vector; `ApplicationTrace` owns those path
occurrences. Direct Required diagnosis uses no Application ancestor. Missing
Application diagnosis retains its full original family and has no downstream
branch. No constructor bypass, source read or mutable index is introduced.

## Lookup and explicit compatibility — C11Q-D04–06

Exact lookup filters original catalog CapabilityRecord values by complete
qualified identity and opaque contractVersion, retaining Provided and Required
and every Component/package/resource/source witness in catalog order. Text
assistance is separately designated discovery-only: a nonempty literal substring
search of original definition name, Present summary and Present description,
case-insensitive using Locale.ROOT. Absent metadata supplies no string. Results
retain original values and order; metadata similarity never changes identity,
provider selection, compatibility or trace links.

Provider matching considers only Provided declarations with the exact required
ModelElementId. Equal contractVersion gives SameVersion. Otherwise every admitted
directed compatibility record with that identity and exactly the requiredVersion
and providedVersion contributes an original context/Source/Origin witness to
ExplicitCompatibility. There is no extra compatibility-owner restriction. Return
each matching Provided record once in catalog order with only its own realization
vector and four original Unknown observations. Keep same-identity incompatible
candidate records separately. No priority/winner, reverse or transitive edge,
semver/default/latest interpretation, name match or merging across owning keys is
permitted.

findRequirements selects only Required declarations whose original complete
ComponentCoordinate equals the explicit input; Provided on the same Component
stays separate. All context witnesses remain ordered. An absent exact Component
in a successfully admitted finite scope gives successful empty without NoProvider
or a global absence assertion.

## Owned Application paths — C11Q-D07–09

An Available Application family supplies every exact versioned ApplicationRecord
in document/record order. Each trace retains that complete record and its original
ApplicationRequirementPath sequence. The nesting is:

```text
ApplicationRecord + ApplicationContextKey
  -> original mapping occurrence + boundary + Source/Origin
       -> each Resolved original Required DeclarationRecordKey in endpoint order
            -> full Required CapabilityRecord
                 -> each compatible Provided record + compatibility witnesses
                      -> that Provided owner's realization refs only
```

This structure retains repeated genuine mappings and all context witnesses
without a content/order-derived semantic identity. It prevents independent-list
Cartesian joins and sibling realization leakage. Available with no exact
Application returns successful empty. Unavailable Application returns its exact
UnavailableProjection during trace, never an empty Application vector.

Every original mapping and boundary remains. A Local Unresolved endpoint retains
the selected mapping/reference and supports only
MissingRequiredComponentCapability there; it creates no Required/provider/
realization. An External Unresolved endpoint retains original target/Source/
Origin as unresolved external evidence and creates no local MissingRequired,
NoProvider, version, realization or role gap. Resolved endpoints preserve their
exact declaration keys and full records.

Forward traceUseCaseDemand validates its qualified input and returns the original
UseCase UnavailableUpstreamProjection with not-yet-admitted reason/detail.
UseCase diagnosis returns the same unavailable outcome without downstream gaps.
Read-only admitted UseCase model facts and Application demand/discovery reverse
links cannot create positive forward demand. This is the original conditional
acceptance's allowed unavailable branch, not a fabricated producer, positive
carrier or exercised positive integration.

## Demand selectors and known boundaries — C11Q-D10–12

Application diagnosis covers all exact matching Application contexts and mapping
branches. Required diagnosis selects an exact admitted Required
DeclarationRecordKey and uses the same provider/role semantics without an
Application ancestor. Wrong direction or malformed identity is Malformed;
absence of the exact selected Required key is Unavailable at that record boundary,
never NoProvider.

An absent versioned Application in an Available admitted family yields only
MissingApplicationCapabilityProjection with reason absent-selected-reference and
the admitted family/scope. An explicitly admitted ApplicationFamily.Unavailable
yields only that gap with reason projection-not-supplied and the exact original
availability value. Neither yields downstream gaps over absent facts. Source
failure prevents a session and cannot become a missing-Application gap.

For each real resolved Required branch, candidates are all Provided records of
its exact qualified identity in the finite scope. Zero candidates yields scoped
NoProvider on that Required owner and optional Application/mapping. Candidates
with no compatible match yield IncompatibleProviderVersion retaining all candidate
records, both opaque versions, original contexts and searched compatibility
evidence; NoProvider is excluded. A mix including compatible matches retains
incompatible observations but yields neither absence nor blocking version gap.

Each compatible provider with an empty own realization vector yields
MissingRealization, retaining provider key/full record, optional Required/
Application path and original realization Presence including absence reason/detail.
A sibling provider, Required declaration or another Capability cannot satisfy it.
Nonempty vectors retain Operation/Workflow/StateMachine edges in original order.
No edge is invoked; declarative absence implies no runtime failure.

## Role owners and stable gap order — C11Q-D13–14

Role assessment uses original AttributedTraceability facts. For each compatible
provider branch its applicable owner membership set contains only:

- The enclosing Application when present.
- The Required declaration and its exact context-local Required definition.
- That provider declaration and its exact context-local provider definition.

Repeated membership does not duplicate a fact; repeated original facts remain
repeated. Filter in original catalog traceability order and preserve owner, role,
target, boundary, Source and Origin. Specification and Evidence are independently
assessed. MissingSpecification or MissingEvidence occurs only when its role has
no fact on the applicable known branch, retaining all assessed owner references,
original Presence reasons and path. Targets outside the scope are not inspected;
role presence establishes no quality or runtime result. With no compatible
provider, assess only known Application/Required owners and label that narrower
known-facts boundary. Local unresolved, External and no-Application-definition
branches create no downstream role gap.

The closed family is MissingApplicationCapabilityProjection,
MissingRequiredComponentCapability, NoProvider, IncompatibleProviderVersion,
MissingRealization, MissingSpecification and MissingEvidence. Each retains
selected scope/snapshot/provenance, applicable full original Application/mapping/
Required/provider records and evidence. No content hash or manufactured identity
is added.

Traverse original Application contexts, mapping occurrences, Required witnesses
and Provided matches in their existing order. In a resolved branch, applicable
absence/version gap precedes the two known Required-path role gaps when no
compatible provider exists. Otherwise each compatible provider's order is
MissingRealization, MissingSpecification, MissingEvidence. Repeated genuine
branches remain repeated; opaque identities are not sorted and labels are not
collapsed. The [contract's gap table](../spec/capability-query-contract.md#seven-typed-gap-variants-owners-and-evidence)
records each precise boundary and retained evidence.

## Distinct outcomes and later presentation — C11Q-D15

Original source diagnostics, family unavailability, successful finite empty,
unresolved External facts and each typed gap remain separate internal values.
Missing Application diagnosis describes a known missing boundary; unavailable
Application trace still returns original typed unavailability. UseCase
unavailability yields no downstream diagnosis. A provider search's successful
empty is not itself a NoProvider gap; diagnosis requires a real Required owner.

AvailabilityObservation, AuthorizationObservation, GuardObservation and
ExecutionResultObservation retain four distinct original Unknown values.
Static matches, native metadata, explicit references and gaps establish no
availability, authorization, guard, execution success, Evidence truth or permission.
[Phase 11.1](../phase/phase-11.1.md) later maps every distinct outcome with original
identity/provenance and discovery-only labeling. No new authorization strings,
public exposure or successor implementation belongs to this Slice.

## Responsibility freeze and executable handoff — C11Q-D16

The failure model is typed input over a finite previously validated semantic
projection, with original source outcomes and explicit independent selection.
Existing validation retains its own responsibilities. Do not add transport,
null-accident defense, hashes, byte invariance, CAS, replay, cache invalidation,
freshness, concurrency, acquisition, global coverage or activation guarantees.
Accepted ingestion/catalog code and original runtime/upstream owners retain their
behavior. The six modules and exact carrier shapes above implement only the
parent-frozen S02 responsibilities.

S01 authored only the two normative documents. The S02 parent froze exact Scala
types/APIs/modules and meaningful executable cases before code authoring and
owns focused validation, representative actual producer query evidence and the
affected-consumer accumulator. One independent whole-Step review and exact Step
commit follow complete executable evidence. All nine original CAP-11-03 criteria
remain unchecked through this S02 authoring handoff; existing producer evidence
proves a prerequisite rather than acceptance of the newly authored queries.

The [criterion and case map](../spec/capability-query-contract.md#original-criteria-and-executable-acceptance)
records C11Q01–25 as accepted executable obligations after focused validation,
independent whole-Step conformance and mechanical local naming closure.
The contract's adjacent case-to-source table records every actual authored path.
S02 scenarios
use semantic AnyWordSpec should/which and adjacent Given/When/Then. C11Q24 must
actively generate finite valid scopes for exact-versus-directed matching,
independently expected cardinality and isolation with at least 100 successes and
zero discards, without filtering invalid cases. Markdown/source-shape/closure
tests are not behavioral evidence. C11Q25 must connect the real normal Cozy
producer/public CNCF Factory/current CBD Snapshot to matching, Application trace
and gaps without CML reparsing, activation or additional provider reads.

Parent-selected focused evidence comprises one real representative and a
query/policy/demand-diagnosis plus original ingestion/catalog/control/output-
exclusion accumulator. CBD aggregate full-suite validation remains deferred-not-run
to Phase 11.1. Original authority, bases, epoch and ledger remain in force;
participant full validation/reviews/releases and already-authorized eventual Cozy
main integration retain their owners. Phase review/release and successor execution
remain separate obligations. No checklist, phase status, journal, workflow control,
participant source or accepted ingestion/catalog executable is changed by this
authoring Slice.
