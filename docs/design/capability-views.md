# Capability Views Design

Date: 2026-10-16. Scope: Phase11.1 / CAP-11-04 / VIEW01.
Author: ASAMI, Tomoharu. Status: authored; parent validation and acceptance pending.

The [view contract](../spec/capability-views-contract.md) adds three
package-private runtime objects above the accepted immutable query session.
`CapabilityViewModel` defines immutable carriers, `CapabilityViews` connects
session operations to those carriers, and `CapabilityWebView` renders them.
The existing query, catalog and ingestion APIs remain the semantic authority.

## Mapping responsibilities

| View | Input mapping | Retained value |
| --- | --- | --- |
| Overview | Session scope and catalog | Original contracts, Usage records, unavailable UseCase |
| Detail | getCapability(lookup) | Complete scoped result including designation |
| UsageTrace | traceComponentUsageCapability(reference), diagnoseCapabilityDemand(ComponentUsage(reference)) | Both complete scoped results |
| UseCase trace | traceUseCaseDemand(reference) | Original Either including unavailability; no success carrier |

The model does not flatten query paths. Rendering walks nested records locally,
so a Provider's realization list cannot be borrowed from a sibling. Diagnosis
renders gap context and role coverage separately from the main trace, preserving
both original query results without manufacturing duplicate trace branches.
Source facts, Presence and Origin are displayed with their ownership rather
than reclassified as verified runtime facts. Opaque versions are displayed
verbatim. Original collection order, repeated paths and distinct contexts remain.

## Read-only Web presentation

Semantic sections, lists and details show the Component contract / Component
Usage observation distinction. A common text/attribute escape function handles
all dynamic values. Reference spans use full original `data-reference` and
`data-context` representations. These are reference navigation facts only;
there is no invented receiving endpoint, raw external href, script, selection
control or execution control. The representation is internal HTML presentation,
not a new public codec or identity schema.

Each main-trace Provider section contains its own complete record and realization
articles. A realization article carries original owner and target attributes
and labels kind, target Component/version/ABI, edge Source/Origin and resolved
contract Source. Four separate Unknown observations follow each realization.
Role links retain exact target and boundary along with Source, Origin and owner.

Gap case matching is exhaustive over the seven query variants and includes each
variant's original scope, owning Usage/mapping/Required/Provider context and
variant-specific search, Presence or coverage data. Source failures and explicit
unavailable projections use separate sections. Successful empty values have an
explicit empty message. UseCase remains unavailable even when admitted Component
or Usage collections are empty.

## Specification boundary

The executable specification uses existing synthetic CapabilityQueryFixtures.
It checks delegation, rendering, ownership, escaping and diagnostic presentation.
Its finite generated property derives expected Provider keys, target facts and
mapping multiplicities from input packages, with at least 100 successful samples
and zero discards. It cannot establish real producer interoperability or public
surface parity. Those acceptance obligations and wire/transport adapters remain
with CAP-11-05; this slice performs no acquisition, parsing, authorization
decision, mutation or runtime execution.
