# Capability Read Surface Contract

Date: 2026-10-16. Phase11.1 / CAP-11-05 / SURFACE01.

The seven generated `CbdRetrieval` QUERY operations expose the accepted immutable
[queries](capability-query-contract.md) and [views](capability-views-contract.md).
All take the common required payload `selector: String` through seven
operation-owned generated query action types and return the common
`CapabilityReadResponse(status: String, resultJson: String, html: String)`.
The selector is strict JSON: duplicate keys, unknown fields, omitted qualifiers
and caller-supplied roles, configuration or selection are rejected.

Normal CNCF record formatting emits the Scala/generated response field
`resultJson` as the outer wire field `result_json`; keys inside the compact JSON
retain their declared spelling.

| Operation | Input QUERY action type | Exact selector shape |
| --- | --- | --- |
| getCapabilityOverview | GetCapabilityOverviewRequest | `{}` |
| getCapability | GetCapabilityRequest | `{kind:"EXACT",reference:CapabilityReference}` or `{kind:"SEARCH",text:string}` |
| findProviders | FindProvidersRequest | `{reference:CapabilityReference}` |
| findRequirements | FindRequirementsRequest | `{component:{namespace,id,version}}` |
| traceComponentUsageCapability | TraceComponentUsageCapabilityRequest | `{reference:ComponentUsageCapabilityReference}` |
| traceUseCaseDemand | TraceUseCaseDemandRequest | `{reference:{modelId,elementId}}` |
| diagnoseCapabilityDemand | DiagnoseCapabilityDemandRequest | `{kind:"COMPONENT_USAGE",reference:ComponentUsageCapabilityReference}`, `{kind:"REQUIRED",key:DeclarationRecordKey}`, or `{kind:"USE_CASE",reference:{modelId,elementId}}` |

CapabilityReference is `{kind:"COMPONENT",identity:{modelId,elementId},contractVersion}`;
ComponentUsageCapabilityReference uses kind `COMPONENT_USAGE`. A full
DeclarationRecordKey is `{context,declaration}`. Context has
`scopeId,component:{namespace,id,version},packageCoordinate:{coordinate,version},resourcePath,source:{modelAuthority,path,sourceRevision}`.
Declaration has `component:{namespace,id},direction,capability:CapabilityReference`;
direction uses the native `PROVIDED`/`REQUIRED` tags. All versions are explicit,
opaque values; no latest, default, semver inference or winner is selected.

Authorization consumes actual ExecutionContext roles using CarReviewAuthorization.
Only viewer, reviewer, operator or admin may read. Denial precedes selector parsing
and configuration access and is structured `securityPermissionDenied` with
`capability.read requires a read role`. Invalid operation is a structured failure.
Malformed selectors uniformly return Source(Malformed), status SOURCE_FAILURE.

Only server ComponentConfigurationAccess supplies
`textus.cbd.capability.projection` (the original portable catalog JSON) and
`textus.cbd.capability.selection` (exactly `{snapshotReference,provenance}`).
Application, subsystem and runtime configuration follow existing precedence.
Selection is independently supplied, never inferred from projection. Original
codec and session admission preserve Malformed, Stale and Ambiguous diagnostics.
Missing input gives Source(Unavailable); scope may be null before session admission.

`resultJson` is compact JSON with exactly
`{schemaVersion:1,operation,status,scope,value,failure}`. Operation is the canonical
camelCase name. Status is `OK`, `SOURCE_FAILURE` or `UNAVAILABLE_PROJECTION` and
equals response status. Scope is the original `{snapshotReference,provenance}` or
null. Success has typed value and null failure; failure has null value. Source
failure is `{kind:"SOURCE",diagnostic:{kind,detail,location,scopeId,component,source}}`;
unavailability is `{kind:"UNAVAILABLE_PROJECTION",value:{family,reason,detail}}`.
Original diagnostic and native enum tags, null/None encoding, Presence and Origin
follow the original portable codec.

Typed values retain their original carrier fields: overview `contracts,usages,useCase`;
lookup `lookup,designation,records`; provider search
`required,candidates,matches,incompatible,consideredCompatibility`; requirements
`component,records`; usage `result,diagnosis` (both scoped); diagnosis
`selection,usages,required,gaps`. Lookup designation is `EXACT_IDENTITY` or
`DISCOVERY_ONLY`. Match basis is `SAME_VERSION` or `EXPLICIT_COMPATIBILITY` with
all original witness records. Every CapabilityRecord retains key, complete
context/resource/input, declaration, definition and its own realizations. Each
realization retains owner, edge, resolved target and four separate observations
`availability,authorization,guard,executionResult`, each `{kind:"UNKNOWN",reason}`.
Record ownership, original collection order and repeated occurrences survive.
Usage traces retain Usage -> mapping occurrence -> Required -> provider search ->
each provider's own realization nesting. Source, Origin, Presence, traceability
owner/role/target/boundary are encoded explicitly without product reflection.

Seven static gap tags are `MISSING_COMPONENT_USAGE_CAPABILITY_PROJECTION`,
`MISSING_REQUIRED_COMPONENT_CAPABILITY`, `NO_PROVIDER`,
`INCOMPATIBLE_PROVIDER_VERSION`, `MISSING_REALIZATION`, `MISSING_SPECIFICATION`,
`MISSING_EVIDENCE`. Each includes its original scope and variant fields unchanged:
respectively reference/reason/families; usage/path; path/search; path/search;
path/original; path/coverage; path/coverage. Missing Usage reason retains its native
tag. Coverage boundary is `REQUIRED_PATH_ONLY` or `PROVIDER_PATH`; owners and
attributed Specification/Evidence remain complete. Required endpoint tags are
`RESOLVED` (records) and `UNRESOLVED` (reference,boundary). Traceability owner tags
are `CAPABILITY_DEFINITION`, `DECLARATION`, `COMPONENT_USAGE`. Demand selection
uses the selector tags above. These static gaps never replace the four Unknown
runtime observations. An admitted empty remains success; failed source never
becomes empty.

HTML is rendered from the same queried typed value using CapabilityWebView, with
all dynamic text escaped. Native `/form/textus-cbd-support/cbd-retrieval/<kebab-operation>`
forms require authenticated access and render response html via
`textus-html-field source="result.body" field="html"`, retaining structured errors.
Normal generated API dispatch and CNCF MCP tools/call use the same actions.
The seven operations join the existing twelve retrieval tools; administration
remains unpublished. There is no new endpoint, script or transport implementation.

Directory and CAR inputs retain their actual original package location, qualified
Component/package/resource/Source identities and provenance; they do not become
new catalog identities. Reads never acquire providers, ingest, reparse CML,
refresh, mutate or activate Operations, Workflows or StateMachines. Permission to
read establishes no runtime authorization, availability, guard or execution fact.
Forward UseCase remains the original unavailable projection with actual session
scope; reverse Usage links do not manufacture forward success. The positive
forward UseCase branch remains unexercised.

Executable obligations: CapabilityReadApplicationSpec, ComponentFactorySpec and
CapabilityReadSurfaceProducerIntegrationSpec. Real producer acceptance uses normal
Cozy output and packaging, public CNCF acquisition, original ingestion/readCurrent
and production ComponentFactory through actual Subsystem, MCP and form rendering.
