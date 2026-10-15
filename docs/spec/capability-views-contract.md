# Capability Views Contract

Date: 2026-10-16. Scope: Phase11.1 / CAP-11-04 / VIEW01.
Author: ASAMI, Tomoharu. Status: authored; parent validation and acceptance pending.

The [current Component Usage amendment](component-usage-capability-adoption.md)
and [query contract](capability-query-contract.md) govern these read-only views.
Component contracts are deductive declarations; Component Usage observations
are inductive evidence. Presentation keeps the two families visibly separate.

## Immutable shared values

`CapabilityViewModel.Overview(scope, contracts, usages, useCase)` retains the
session's complete scope and original catalog vectors in their original order.
`Detail(result)` retains the complete `QueryResult[CapabilityLookupResult]`.
`UsageTrace(result, diagnosis)` retains the complete scoped nested trace and
demand diagnosis. Source, Origin, Presence, full owner keys and runtime
observations remain typed original values. Labels never replace identity.

`CapabilityViews` consumes an already-open `CapabilityQueries.Session` only.
`overview` reads its catalog. `detail` delegates `getCapability` unchanged.
`usageTrace` delegates `traceComponentUsageCapability` and
`diagnoseCapabilityDemand(ComponentUsage(reference))`. Either failure propagates
unchanged. `useCaseTrace` delegates `traceUseCaseDemand` with its original
`Either[QueryFailure, QueryResult[Nothing]]` signature. There is no positive
UseCase carrier. These functions perform no admission, acquisition, snapshot
selection, inference, mutation or identity creation.

## HTML mapping and reference navigation

`CapabilityWebView.overview`, `detail`, `usageTrace` and `failure` produce
escaped semantic HTML. Overview visibly includes UseCase unavailability.
Detail shows ExactIdentity or DiscoveryOnly and the original lookup. Successful
empty collections have an explicit empty message distinct from failure.

Every record retains its qualified identity, opaque contract version,
Component/package/resource/scope/source context and metadata. Reference spans
carry escaped `data-reference` and `data-context` values containing the complete
original reference/context representation. They declare exact navigation facts;
they introduce no endpoint or external URL execution. Specification and Evidence
links show original target, boundary, owner, Source and Origin as attributed facts.
No link or source authorizes execution or proves runtime behavior.

Usage rendering follows each original Usage -> mapping -> Required -> Provider
-> that Provider's realizations. Repeated paths and many-to-many branches stay
repeated. Provider match basis and original candidate/incompatible/compatibility
witnesses remain visible; there is no winner or join of independent endpoints.
Each realization shows kind, target, Component, contract version, ABI, Source,
Origin, resolved target contract and owning declaration. Availability,
Authorization, Guard and Execution result each show Unknown and their own reason.

## Failure and gap presentation

All seven gap case names are visible with original scope and owner context:
MissingComponentUsageCapabilityProjection, MissingRequiredComponentCapability,
NoProvider, IncompatibleProviderVersion, MissingRealization,
MissingSpecification and MissingEvidence. Coverage retains attributed role
owners and their original Presence values. Missing Usage retains the original
package families and reason. Gaps do not become runtime observations.

Source failure displays its original diagnostic kind, detail, location, scope,
Component and Source. UnavailableProjection displays original family, reason and
detail. Unauthorized, Unavailable, Stale, Ambiguous and the other diagnostic
kinds never become successful empty collections. Dynamic text and attributes
escape ampersands, angle brackets, double quotes and single quotes. External
values never enter raw hrefs, scripts or execution controls.

## Executable specification and boundary

[CapabilityViewsSpec](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityViewsSpec.scala)
authors these contracts with Given/When/Then and should matchers. Its finite
ScalaCheck property requires at least 100 successes and zero discards and uses
source package facts and mappings as its expected ownership/cardinality oracle.
CapabilityQueryFixtures are synthetic semantic inputs, not real producer
acceptance. CAP-11-05 owns real producer/CAR/Web/MCP acceptance and public
wire/transport/service/authorization mapping. This slice adds none of those APIs.
