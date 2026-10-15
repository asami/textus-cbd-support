# Capability Read Surfaces Design

Date: 2026-10-16. Phase11.1 / CAP-11-05 / SURFACE01.

The [surface contract](../spec/capability-read-surface-contract.md) freezes one
public bridge, CapabilityReadApplication. Generated CbdRetrieval actions build
the existing ComponentConfigurationAccess from application/subsystem/runtime
sources and pass operation and selector. The bridge checks actual context roles
first, decodes an independently selected projection and opens an immutable query
session. No caller can choose source configuration or trusted roles.

CapabilityReadSelectorPart owns closed request decoding with the original strict
scalar/reference codecs. CapabilityCatalogJsonPart decodes independent selection
with original snapshot/provenance codecs. CapabilityReadJsonPart explicitly
encodes typed query/view values using original portable helper semantics. Helpers
remain within runtime-private objects; original query/catalog carriers do not
cross the bridge. The public response contains only status, compact JSON and HTML.

Each operation queries once and derives both JSON and HTML from that value.
CapabilityViews supplies overview, detail and combined Usage trace/diagnosis;
session queries supply providers, requirements and diagnosis. The original
forward UseCase failure retains the admitted session scope. Source errors before
admission use null scope. HTML renderer methods reuse original escaping and
ownership-aware rendering helpers. There is no reflection-based JSON encoding.

CML appends seven QUERY operations with seven operation-owned generated query
action types, each carrying the same required `selector: String` payload, and
one common response. Each generated action retains its owning operation's
createCall dispatch. Native generated API and MCP publication consume normal
generated metadata. Native form
configuration enables the seven authenticated operation forms and normal result
templates render already escaped HTML and structured errors. Navigation points
to those actual forms. No custom server or endpoint is introduced.

An explicit textus.cbd.cozyGeneratorRoot overrides common cncf.cozyGeneratorRoot.
In selected CNCF source mode that same selected Cozy root is the default real
producer input; an explicit capabilityProducerRoot overrides it. Invalid explicit
roots fail. With no source selectors artifact behavior remains unchanged. Existing
internal-model exclusion generation and producer packaging settings remain intact.

Specifications use finite original query fixtures for typed mapping and actual
producer products for native route parity. Test-only connection helpers configure
production server inputs and server authentication, and drive actual Subsystem,
McpJsonRpcAdapter and StaticFormAppRenderer APIs. Directory/CAR witnesses remain
different while semantic values agree. Forward UseCase remains unavailable and
its positive branch is explicitly unexercised. Runtime observations remain four
Unknown values regardless of the seven static gap types. There is no new store,
ingestion policy, identity synthesis, runtime activation or upstream change.
