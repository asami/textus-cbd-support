# Component Usage Capability Consumer Adoption

Status: CAP-11-PKG01 accepted after focused validation and independent whole-Step conformance; Phase acceptance pending.
Date: 2026-10-15

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

## Scope and model distinction

Replace the former ApplicationCapability family with `ComponentUsageCapability`.
It is the inductive model of what the reviewed Component can be used for, with
attributed Use Case/usage evidence. Keep `ComponentCapability` as a separate
model for deductive PROVIDED/REQUIRED contract matching and explicit realization.
Application remains reserved for the Application Model/Tier contrasted with Domain.
The reviewed Component owns its metadata in its CAR or generated Directory.
There is no Application-owned package and no separate whole-system owner here.

Cozy `docs/spec/component-usage-capability.md` owns Usage source/IR/payload meaning;
CNCF `docs/spec/component-usage-capability-package.md` owns acquisition/layout.
CBD keeps these families distinct in ingestion, records, query inputs/results,
gaps and presentation. Usage evidence alone cannot create a provided/required
contract, provider, native target, permission or successful execution result.

## Explicit ownership, cardinality and trace

The new payload `cozy.component-usage-capability.v1` describes the explicitly
selected Component and package. Every Usage record has a declared Component
owner and independent qualified model identity/version. Its original source,
discovery/demand links, derivation origin, grounding and traceability are retained.
Consume only the selected Component's records; multi-Component generation must
partition them upstream. Retain explicit external references across Components.

Use Case/usage evidence -> ComponentUsageCapability -> ComponentCapability is
many-to-many with attributed, directed edges. Never infer mapping from names,
collection position or separate endpoint lists. An inductive correspondence is
not automatically a REQUIRED declaration; retain the existing explicit declaration
joins and known-gap semantics. Missing evidence/mapping remains an explicit gap.
Unprovided forward UseCase demand remains unavailable: reverse discovery links
do not supply a positive forward-demand contract. Dependency edges express logical
usage requirements; they do not prescribe runtime execution order.

The catalog owner/context key retains selected Component coordinate/package,
canonical resource location, source header and Usage identity/version. Retain
these witnesses when different components refer to the same external concept.
Do not deduplicate them by text or flatten them into ComponentCapability records.

## Current naming map (breaking change)

| Historical implemented surface | New current surface |
| --- | --- |
| ApplicationCapability / ApplicationCapabilityReference | ComponentUsageCapability / ComponentUsageCapabilityReference |
| ApplicationRecord / ApplicationContextKey | ComponentUsageCapabilityRecord / ComponentUsageCapabilityContextKey |
| ApplicationRequirementPath / ApplicationTrace | ComponentUsageRequirementPath / ComponentUsageTrace |
| Application family/input / Application demand selector | per-package ComponentUsage family / ComponentUsage demand selector |
| traceApplicationCapability | traceComponentUsageCapability |
| MissingApplicationCapabilityProjection | MissingComponentUsageCapabilityProjection |
| Application projection codec and corresponding helper modules | Component Usage projection codec and correspondingly renamed helpers |
| cozy.application-capability.v1 / APPLICATION | cozy.component-usage-capability.v1 / COMPONENT_USAGE |

Apply this map to internal/public APIs, portable schemas, generated interfaces,
query discriminators, diagnostics and presentation labels together. Other query
operations keep their behavior while adopting typed Usage references where they
previously accepted Application references. The Component contract ABI remains
`cncf.component-capability.v1`; its existing semantic compatibility edges are
unrelated to legacy-name support and remain valid.

There is NO backward compatibility requirement before release. Delete old types,
constructors, methods, fields, codecs and names. Do not provide aliases, deprecated
forwarders, dual readers, old/new switches or automatic model conversion. Typed
old callers must fail compilation. Update CML, JSON, fixtures and generated data
in the same work; ordinary syntax/schema validation rejects stale serialized
input. Do not build a legacy-format migration/detection subsystem. Historical
acceptance records stay historical; they are not executable alternate contracts.

## Package route and assurance

Use the Component's `meta/component-usage-capability/index.json`, with exactly
`{"schemaVersion":1,"projections":["usage.json"]}`, and the sibling `usage.json`.
Obtain it through CNCF from either the generated Directory or CAR; never take a
loose Application JSON as the normal route. The Directory is usable before CAR
creation and needs no SBT/process execution merely to read metadata.
Absent optional family is unavailable; a present invalid/unreadable family retains
the upstream failure. Reuse CNCF's resource handling and CBD's semantic logic
with the revised typed model. Do not duplicate filesystem/archive checks.

The new focused acceptance must compile the affected producer/consumer graph with
old typed names removed; prove real Directory intake, owner partitioning, separate
inductive/deductive models with many-to-many provenance, and equivalent CAR
query results. Update existing relevant tests instead of retaining parallel
compatibility suites. Full validation remains finally owned by Phase 11.1.

## Authored package and consumer implementation — 2026-10-15

Each selected package has `PackageExpectation(input,resources,usage)` with explicit
optional producer/schema/ABI/ordered-source expectation. None selects absence;
present metadata is still acquired/decoded and cannot be ignored. Absent expected
metadata is Unavailable, present unselected metadata is Incompatible, and present
invalid/unreadable metadata retains its actual failure. CNCF public acquisition
owns transport and yields content/input/canonical resource witnesses; CBD retains
one ComponentUsagePackage per selected package. Empty Component resources use the
original metadata-only package acquisition and require empty index/projections,
without provider/native synthesis. Nonempty resources retain original public
native discovery and native object instances. Selection/Provenance have no loose
Usage input or top-level Usage provenance.

Portable schema 1 retains the ordered package and Usage vectors and nullable
per-package expectations. ComponentUsageCapabilityContextKey contains scopeId,
Component coordinate, package coordinate, resourcePath, document SourceKey and
versioned reference; each record also retains its original selected input.
Equal references across owners remain separate, with attributed many-to-many
Required/provider paths. Trace has typed unavailability only when no selected
Usage family is Available. Missing diagnosis retains all families and uses
ProjectionNotSupplied if any are unavailable, otherwise AbsentSelectedReference.
Acquired empty is Available; reverse links never supply forward UseCase demand.

Schema-2 real requests partition Store's two mapped records, UsageOnly's
independent record and Remote's record. UsageOnly is a genuine Component with
empty Component resources and no Scala output/provider. Archive selects no Usage.
The fifth primary Contracts production uses header:primary-r46 and explicit empty
Usage selections, retaining original Component contexts/contracts and producing
StoreContractsMetadata/ArchiveContractsMetadata alongside the six First/Second
accessors. Full canonical source-free Directory intake and normal CARs from the
same First Store/Remote images feed public CNCF -> CBD -> catalog -> query.
Semantic equivalence preserves different acquisition locations; image reading
runs no producer, CAR build or publication.

Current owner contracts and executable links are in
[ingestion](capability-ingestion-contract.md#cap-11-pkg01-current-authored-contract--2026-10-15),
[catalog](capability-catalog-contract.md#cap-11-pkg01-current-authored-contract--2026-10-15)
and [query](capability-query-contract.md#cap-11-pkg01-current-authored-contract--2026-10-15).
All existing active generated properties retain at least100 successful applicable
cases and zero discards. This is authored work awaiting parent validation, fresh
independent Step conformance and separate participant/Phase closure. Earlier
acceptance/suite counts apply only to historical inputs. No release, main merge,
positive forward UseCase proof or Phase11.1/14 execution is claimed.
