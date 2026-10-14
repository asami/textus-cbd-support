# Capability Catalog and Attributed Trace Model Design

Status: CAP-11-02 accepted after focused validation and independent whole-Step review.
Scope: PHASE-11 / CAP-11-02 / S01-catalog-contract and S02-catalog-implementation

The original S01 pending/authoring prose below describes that historical Slice.
The S02 section records the fully frozen carrier/API implementation and its
authored executable cases, without claiming validation or acceptance.

The [catalog contract](../spec/capability-catalog-contract.md) owns C11C-D01–14,
all eight original criteria and C11C01–16 future behavior obligations. This design
explains their responsibility flow without selecting carrier fields, wire keys or
APIs. The accepted [ingestion contract](../spec/capability-ingestion-contract.md),
[ingestion design](capability-ingestion.md) and
[CAP-11-01 acceptance](../phase/phase-11.md#cap-11-01-acceptance--2026-10-15)
provide the prerequisite. The [Phase checklist](../phase/phase-11-checklist.md)
retains acceptance ownership; the [repository rules](../rules/repository-rules.md)
apply to identity and assurance.

## Accepted Snapshot to original Entry view to typed records

C11C-D01/02 keep acquisition and current/history selection in CapabilityIngestion.
CapabilityCatalog receives the original explicitly selected accepted Snapshot
returned by readHistorical/readCurrent. It derives a package-private read-only
semantic view without rereading providers or activating native procedures.

```text
CapabilityIngestion readHistorical/readCurrent
  -> original accepted Snapshot + complete Provenance + finite scope
       -> ordered original Discovered packages / Entry resources
            -> full declarations + exact admitted definitions / context witnesses
                 -> CapabilityRecord + owned CapabilityRealizationRef
       -> complete separately admitted Application family or unavailable branch
            -> owned ApplicationRequirementPath
       -> original unavailable forward UseCase family
            -> typed CapabilityDemandTrace availability
```

Snapshot management reference, Component coordinate/release, package
coordinate/version, resource and acquisition location, producer/schema/ABI and
all original Source/Origin facts remain independent. A CapabilityRecord is a
view of admitted semantic identity within the declared finite scope. It supplies
no replacement Capability identity or global completeness claim.

## Context, declaration and Application owner keys

C11C-D03 defines ComponentContextKey using admitted scopeId, Component coordinate,
package coordinate, resourcePath and original projection authority/path/revision.
DeclarationRecordKey adds the original DeclarationKey with Component, direction
and qualified CapabilityReference. Source line is retained only as provenance.
Order, labels, mtime, bytes and encoded content cannot supply semantic identity.
Provided and Required records remain distinct even on the same Component.

C11C-D04 resolves each declaration against its original Entry.admitted.capabilities.
Its full declaration, exact definition, discovery/grounding/Presence and ordered
traceability remain attached to that context. Different Components/packages/sources
witnessing one qualified identity retain separate records. Repeated identical
owning keys are Ambiguous, with no selected winner or name-based deduplication.

C11C-D06 gives ApplicationContextKey the independently declared document
authority/path/revision and versioned Application reference. Complete ordered
documents preserve every Presence field, including Present(empty), demand/discovery
links, dependencies, mappings, grounding, traceability and unresolved External
evidence. Each edge retains its exact enclosing record and boundary/Source/Origin;
definition source revisions and edge source revisions are not replaced by the
document header. External unresolved evidence remains valid attributed evidence.

## One-to-many owned ApplicationRequirementPath

C11C-D07 starts each path at an owning versioned Application record and its
particular original componentMapping. It retains every exact matching Required
declaration with its separate context witness. A mapping with no Required match
retains an explicit unresolved required endpoint for later typed diagnosis;
catalog derivation does not fabricate a declaration or reclassify acquisition.

CAP-11-03 later extends that particular path through compatible Provided
declarations to each provider declaration's own realization edges. Independent
endpoint vectors cannot be joined into a Cartesian product, and sibling
declarations cannot exchange realization edges. Owner labels supply no matching
evidence. C11C-D10 preserves each compatibility declaration's context, exact
qualified identity, requiredVersion/providedVersion and Source/Origin. Matching
later permits equal version or one explicit directed same-identity admitted edge;
it permits no reverse or transitive edge, semver interpretation, default/latest
inference or label similarity.

C11C-D08 retains the exact original
`UnavailableUpstreamProjection(family=UseCase,reason=not-yet-admitted,detail)`.
There is no admitted forward UseCase producer/schema in this run. Original
Application-to-UseCase links and finite-scope model facts remain reverse-direction
facts; they do not create forward demand. Useful ApplicationRequirementPath values
remain available alongside the unavailable forward family. Positive forward
integration remains unexercised.

## Live native metadata and portable semantic transport

C11C-D05 attaches each CapabilityRealizationRef to its DeclarationRecordKey and
complete original Realization edge. The exact TargetContract retains explicit
Operation/Workflow/StateMachine kind, qualified reference/boundary, target
Component, target contractVersion/generated ABI, native-contract source and edge
Source/Origin. A live view may retain the original ResolvedTarget observation.
Portable transport retains semantic references, contracts and provenance only;
it serializes no native procedure and creates no native object or execution
admission.

C11C-D09 keeps Specification and Evidence as distinct role-bearing ModelReference
edges attached to their exact definition/declaration/Application/path owner, with
their own boundary/Source/Origin. Specification cannot substitute for Evidence,
and missing Evidence cannot imply failed execution. Availability, Authorization,
Guard and execution result stay separate and unknown without explicit
observations; static target resolution proves none of them.

C11C-D11 requires an explicitly supplied internal schema version, snapshot
management reference, declared scope/provenance, ordered original Component
semantic projection, exact definition/target-contract reference facts and complete
Application family or unavailable branch, together with original forward UseCase
unavailability. Every semantic reference, version, Source/Origin, Presence,
boundary, owner context and cardinality must roundtrip. Component/Application
transport shapes and lexical/reference domains stay with existing public CNCF and
private CBD codecs. Semantic decoding creates no accepted Snapshot, public
Discovered, native runtime object, acquisition success, freshness or permission.

## Model, codec, helpers and facade boundaries

The responsibilities are frozen by C11C-D12/13; exact transport fields/keys and
APIs, including independent selected finite-scope context, must be frozen by
parent PLAN before S02 editing.

| Owner | Bounded responsibility |
| --- | --- |
| CatalogModel | Semantic records, explicit owner keys and family availability. |
| CatalogProjectionCodec and coherent helpers | Decode, bind and encode portable semantics; preserve existing Component/Application codec domains. |
| CapabilityCatalog | Derive a read-only semantic view from the original accepted Snapshot. |
| CapabilityIngestion | Original source acquisition/selection, accepted Snapshot construction, immutable current/history and complete original Diagnostic outcomes. |
| CAP-11-03 | getCapability, findProviders, findRequirements, traceApplicationCapability, traceUseCaseDemand, diagnoseCapabilityDemand and seven typed gap variants. |
| Phase 11.1 | Shared Web/MCP presentation and authorization mapping after the accepted typed handoff. |

CatalogRuntime, CarReviewCapabilityCatalog taxonomy, Job/approval programs and
upstream generators keep their existing ownership. No public API/service CML
exposure or competing upstream format is introduced.

## Original diagnostic, scope and history ownership

C11C-D01/12 preserve complete original Diagnostic kinds and provenance. The
semantic validator rejects malformed carriers, duplicate owning keys, dangling
Local/owner/declaration references, ambiguous exact candidates and realization
owner/target kind/version/ABI mismatch. C11C12 also requires distinct diagnostics
for target identity and Component mismatches. Explicit unresolved External evidence
is retained. Validation covers declared semantic links only and adds no global
cycle, tamper, replay or concurrency guarantee.

Catalog construction does not select a fresher Snapshot, replace failed current
observations with historical success or reinterpret unavailable acquisition as an
empty catalog. Ingestion continues to own its original state and failures. Later
queries and gaps consume those typed outcomes and the declared finite scope.

## Implementation, executable specification and closure order

C11C-D14 limits S01 to this design, its normative contract and exact accepted
journal projection. It claims no executable catalog, tests, independent Step
acceptance, participant full review/release or positive forward UseCase integration.
The [contract criterion and case map](../spec/capability-catalog-contract.md#future-executable-behavior-obligations-and-original-criterion-map)
retains the exact original eight criteria and all sixteen future cases:

| Criteria | Design responsibility | Future obligations |
| --- | --- | --- |
| 1 | Qualified records and independent provenance/version domains. | C11C01, C11C02, C11C09, C11C13–15 |
| 2 | Explicit Application paths and unavailable forward demand. | C11C04, C11C05, C11C07 |
| 3 | Explicit owned native target references. | C11C03, C11C06, C11C12, C11C15 |
| 4 | Distinct Specification/Evidence roles. | C11C08, C11C09 |
| 5 | Separate unknown runtime observations. | C11C08, C11C15 |
| 6 | All genuine demand/provider/realization witnesses. | C11C01–06, C11C09, C11C15, C11C16 |
| 7 | Enclosing record and per-edge attribution. | C11C03–06, C11C08, C11C09, C11C11, C11C12, C11C14 |
| 8 | Semantic roundtrip and bounded rejection. | C11C09–16 |

Parent first freezes S02 carrier/API shape and exact code/spec ownership. S02 then
authors meaningful AnyWordSpec/GivenWhenThen/should behavior, applicable active
generated properties with at least 100 successful cases and zero discards, and
actual accepted producer Snapshot consumption. Synthetic negatives cannot replace
C11C15. Existing ingestion and actual producer evidence remains prerequisite
evidence, with causal reuse of the eight-case producer proof where appropriate.
Parent owns focused validation: one meaningful catalog representative and one
affected ingestion/control/output-exclusion accumulator, followed by independent
whole-Step acceptance against all eight original criteria.

CAP-11-02 remains open. CAP-11-03, participant Phase full validation/reviews/releases,
authorized Cozy main integration and the original CBD Phase review/release remain
separate work. No full suite occurs until its owning Phase release; CBD aggregate
full-suite validation stays deferred-not-run to Phase 11.1. The original Phase 11
completion scope is preserved. This Slice executes no successor or Phase 14 work,
changes no source/build/test/fixture/configuration, and performs no maintenance
cleanup, network/runtime/cache/persistence/concurrency/retry/replay or
content-derived management identity work.

## S02 exact implementation flow — 2026-10-15

The [complete carrier/API/wire specification](../spec/capability-catalog-contract.md#s02-complete-internal-carrier-and-api--2026-10-15)
now fixes all record fields, JSON keys, closed branches, attribution and binding.
All production modules remain private[runtime]. Their exact responsibility flow
is:

```text
original selected accepted Snapshot
  -> CapabilityCatalog.fromSnapshot(snapshot, explicit schemaVersion)
       -> PortableProjection(original Provenance, ordered packages/resources,
            full definition/edge/TargetContract facts, full Application,
            exact unavailable forward UseCase)
       -> CapabilityCatalogBindingPart.bind(projection, CatalogSelection)
            -> context-owned semantic Catalog
       -> LiveCatalog(original Snapshot, Catalog, original ResolvedTarget refs)

internal semantic JSON + independent CatalogSelection
  -> CapabilityCatalogProjectionCodec.decode
       -> strict duplicate-key rejecting parse
       -> JsonPart + ScopeJsonPart + ComponentJsonPart
       -> same bounded BindingPart -> semantic Catalog only

PortableProjection + independent CatalogSelection
  -> CapabilityCatalogProjectionCodec.validate -> same BindingPart
  -> CapabilityCatalogProjectionCodec.encode -> same BindingPart
       -> JsonEncodingPart -> deterministic noSpaces JSON
```

CatalogModel contains the complete immutable semantic/live types and four
distinct Unknown observation domains. Catalog captures original accepted values
and native references. ProjectionCodec owns only its three explicit facade
methods. JsonPart owns top-level/provenance/expectation/Application/unavailable/
Diagnostic/External branches; ScopeJsonPart owns finite Scope facts, original
definitions and TargetContracts; ComponentJsonPart owns original package inputs,
location/coordinates, resources and standalone realization facts. JsonEncodingPart
encodes full portable semantics using unchanged Component and Application
codecs and the existing reference/Presence helpers. BindingPart owns exact
semantic coverage, public Component admission, original Application admission,
record/path construction and attribution. No helper is an acquisition facade.

### Original live objects and semantic values

PackageProjection.input is the original discovered.acquired.input.
Its targetContracts preserve the ordered nativeInput.bindings.map(_.target).
ResourceProjection retains the original loaded path/projection and complete
Entry.admitted definition/realization reference facts. Original per-entry native
ResolvedTargets are retained by reference in NativeRealizationObservation and
remain owned by the resource context plus original declarationKey. The original
Snapshot/packages/entries/nativeInput/native snapshots/bindings/Operation/
Workflow/StateMachine graph remains reachable through LiveCatalog.snapshot.

PortableProjection and Catalog contain no native objects. The JSON encoder has
no native procedure, observation, derived record/path or execution/admission flag
branch. Component and Application transports are embedded as ordinary complete
JSON objects through their original codecs. Decoding never constructs Snapshot,
Discovered or native values. CatalogSelection compares independently declared
finite context; it supplies no acquisition, authenticity, currentness or
permission certificate.

### Exact finite owners and bounded diagnostics

SourceKey uses authority/path/opaque revision, excluding line. ComponentContextKey
uses scope, component coordinate, package coordinate, resource path and header
SourceKey; DeclarationRecordKey adds the full original Component/direction/
CapabilityReference. ApplicationContextKey uses the independent document
SourceKey and versioned Application reference. All original Source lines and
definition/edge/header revisions remain in the retained values.

The binder checks the existing SnapshotReference factories and
IngestionSelectionPart._provenance/_compare, ordered selected package/resource
coverage, input/header alignment and original _source_compare revision outcomes.
It preserves original whole-Snapshot and package uniqueness. Public Component
validation/admission binds every resource against original Scope with that
package's targetContracts. Full ordered definition witnesses and resolved edge/
TargetContract facts must agree; no global collapse or first-candidate winner
can replace an exact resource-local definition. Duplicate keys/candidates remain
Ambiguous, missing facts/Local owners remain Malformed, semantic contradictions
remain Incompatible and opaque source revision differences remain Stale. Upstream
Diagnostic source/component/scope remain intact with actual carrier path prefixes.

Available Application requires matching Acquired provenance, original observed
headers and unchanged full codec/admission against the finite Scope and ordered
resource projection.capabilities. Its complete original External vector agrees
exactly; no deduplication masks ambiguous Local facts. Unavailable provenance and
family agree; Rejected provenance propagates its original Diagnostic. Exact
Unavailable forward UseCase remains in Provenance, projection and demand trace.

Each original mapping derives one owner-attributed ApplicationRequirementPath.
Local endpoints retain every exact identity/contractVersion Required context,
or explicit Unresolved if a valid definition has no Required declaration.
External stays Unresolved even with coincident Local facts. Repeated genuine
mappings remain repeated paths. Definition/declaration/Application traceability
retains every role-bearing edge in original per-owner order. Compatibility retains
its original direction and context. All four runtime domains remain distinct
Unknown values despite static native resolution. No provider search or query
matching is implemented by these derivations.

### Executable authoring and unchanged acceptance owners

The [authored scenario map](../spec/capability-catalog-contract.md#s02-authored-executable-scenario-map)
maps all original eight criteria and sixteen cases to eight ModelSpec cases,
seven ProjectionCodecSpec cases and the single actual ProducerIntegrationSpec
case. Every in-block and table/property body uses adjacent Given/When/Then with
should matchers. C11C16 varies qualified semantic values and full owner/path
cardinalities through a valid-by-construction active generator configured for
100 successes and zero discards, including explicit count output.

C11C15 consumes the existing real source-free normal First Store/Remote harness,
compiled accessors, original native inputs and complete primary Application.
Original ingestion's public Factory path accepts the Snapshot; readCurrent
selects that actual value. Catalog capture/roundtrip retains exact original native
instances only in the live wrapper, original semantic directions and full
Application paths/external evidence, with one provider read each and excluded
poison implementation/guard/action/job bodies. Pure fixtures never create accepted
Snapshots or substitute for actual producer proof.

The parent owns actual selected representative/accumulator execution, mechanical
conformance and independent whole-Step acceptance. Authoring is complete but
CAP-11-02 remains OPEN. No validation or acceptance result is asserted here.
The retained S01 hygiene journal and prior debt classifications are unchanged;
new program historical same-file debt is inapplicable. CAP-11-03 six queries/
seven gaps, participant closures/releases, authorized future Cozy main
integration and original CBD Phase review/release retain their separate owners.
CBD aggregate full-suite validation remains deferred-not-run to Phase 11.1.
No successor, Phase 14, publication or runtime lifecycle work is executed.
