# Phase 11 - Capability Catalog, Discovery, and Traceability

Stage Status:
- Current status: OPEN
- Predecessor: Phase 10.7
- Development item: DEV-CBD-003
- Owner: Textus CBD Support development
- Update rule: completion is recorded only by `phase-11-checklist.md` with reproducible evidence.

## Purpose

Consume the CNCF Component Capability projection and make Capability
discoverable, comparable, and traceable through Textus CBD Support. Capability
remains a non-instantiated model IR; Phase 11 presents and queries its semantic
contract without executing it or reconstructing CML.

Phase 11 also owns the read-only demand-to-realization trace from an admitted
Use Case/Application Capability demand to Component Capability and its explicit
Operation/Workflow/StateMachine realization. It does not judge Use Case quality,
create Use Case Slice structure, or edit requirements; those responsibilities
belong to Phase 12.

## Work stack

| ID | Outcome | Status |
| --- | --- | --- |
| CAP-11-01 | Freeze supported projection versions, ingestion, integrity, and historical/current snapshot semantics. | planned |
| CAP-11-02 | Extend internal catalog/index models for Capability, demand, realization, Specification, and Evidence traceability. | planned |
| CAP-11-03 | Provide Capability search, provider matching, demand tracing, and typed gap/compatibility diagnostics. | planned |
| CAP-11-04 | Provide Capability overview/detail and demand-to-realization/evidence views. | planned |
| CAP-11-05 | Expose consistent Web and MCP/API semantics and prove the end-to-end fixture. | planned |

## Local execution preparation

The GitHub refinement through `a3c3de2` is incorporated. Implementation remains
not started. Consume the accepted Phase 10.7 handoff after Phase 10.6 and 10.7
close; their current ledgers remain open. Use the
[local preparation note](../notes/phase-11-12-local-execution-preparation.md)
for existing integration points and evidence still to be obtained.

Begin CAP-11-01 by admitting exact upstream contracts and fixtures, then promote
the planned ingestion/query behavior into design, specification, and executable
specifications. Treat CAP-11-01–05 as ordered work boundaries; size and split
each before implementation under the current workflow and model policy.
The existing CAR Review capability taxonomy is not the upstream Component
Capability model and cannot supply its identities or demand relationships.

## Required upstream projections

Phase 11 consumes upstream-owned identifiers and relationships; it does not
invent replacements. The consumer contract must admit, when available:

- CNCF Component Capability projection: provided/required Capability identity,
  Component/package/source identity, ABI/version, explicit compatibility and
  realization references.
- Application Capability projection: Application Capability identity and
  explicit relation to required Component Capability identities.
- Use Case demand projection: Use Case identity and explicit required
  Application Capability identities. If this projection is not yet admitted,
  the Use Case portion remains `UnavailableUpstreamProjection`; CBD Support
  must not infer it from names or descriptions.

Record producer, projection/schema version, source revision, scope, and actual
fixture availability separately for each projection. The real Cozy-generated,
CNCF-admitted Component Capability fixture is required for CAP-11-05 closure.
Application/Use Case demand availability is an explicit admission decision:
when supported, prove the positive trace and gap cases; when unavailable, prove
the typed unavailable branch and record the unexercised positive path. Neither
synthetic fixtures nor an unavailable result prove a real upstream demand trace.

## Internal read model

Implementation should introduce package-private/domain values equivalent to the
following roles; exact Scala names may follow repository conventions:

```text
CapabilityCatalogSnapshot
CapabilityRecord
  - capabilityRef
  - direction: PROVIDED | REQUIRED
  - componentRef
  - packageRef
  - sourceRef
  - abiRef/version
  - compatibilityRefs

CapabilityDemandTrace
  - useCaseRef?                 // only from admitted projection
  - applicationCapabilityRef
  - requiredComponentCapabilityRef
  - providerComponentRefs
  - realizationRefs
  - specificationRefs
  - evidenceRefs

CapabilityRealizationRef
  - kind: OPERATION | WORKFLOW | STATE_MACHINE
  - targetRef
  - source/version

CapabilityGap
  - MissingApplicationCapabilityProjection
  - MissingRequiredComponentCapability
  - NoProvider
  - IncompatibleProviderVersion
  - MissingRealization
  - MissingSpecification
  - MissingEvidence
```

A gap is a diagnostic over admitted facts, not a new model fact. Runtime
Availability, Authorization and Guard results are separate diagnostics and
must never be collapsed into `NoProvider` or `MissingRealization`.

The field sketch describes one trace path, not a flattened set of interchangeable
endpoints. Preserve each explicit demand/provider/realization edge, its scope,
source and revision across multiple paths; do not form a Cartesian product from
independent reference lists. Distinguish unsupported, unavailable, unauthorized,
stale and ambiguous input from an admitted empty relationship. `NoProvider`
means no provider in the declared successfully queried catalog scope, not
universal nonexistence. Failed source acquisition cannot establish that result.

## Query/service contract

The catalog/query boundary should support deterministic operations equivalent
to:

```text
getCapability(ref)
findProviders(requiredCapabilityRef)
findRequirements(componentRef)
traceApplicationCapability(applicationCapabilityRef)
traceUseCaseDemand(useCaseRef)
diagnoseCapabilityDemand(demandRef)
```

All identity-bearing inputs are qualified typed references. Display-name or
description similarity may be used only as UI search assistance and must not
establish semantic matching, compatibility, provider selection, or trace links.

`traceUseCaseDemand` returns an explicit unsupported/unavailable result when
the upstream Use Case demand projection is absent. It must not reconstruct the
relationship from CML, Operation names, Event Storming text, or AI output.

## Ownership boundary

- Consume only CNCF/admitted upstream Capability and demand projections and versions.
- Preserve exact Use Case, Application Capability, Component, package, source,
  Capability, realization, and ABI identity.
- Keep current selected snapshots distinct from historical attempts.
- Keep Capability, demand, Availability, Authorization, Guard, realization,
  Specification, and execution Evidence as separate concepts.
- Do not parse CML, define Capability syntax, execute Capability, evaluate Use
  Case quality, create Use Case Slice structure, or replace upstream identity
  with cbd-support-local identity.

## Completion conditions

- Design/specification define supported versions, ingestion integrity,
  snapshot/history behavior, demand trace semantics, queries, views, and typed
  failure/gap diagnostics.
- Executable specifications cover valid ingestion and rejection/quarantine of
  ambiguous, duplicate, malformed, stale, and incompatible projections.
- Search finds exact provided/required Capability identities, providers,
  requirements, and gaps without display-name inference.
- When upstream demand is admitted, an admitted Use Case/Application Capability
  demand can be traced through required Component Capability to explicit provider
  and realization links. Otherwise the recorded unavailable branch is verified
  without claiming the positive upstream integration passed.
- Missing upstream demand projection, provider, compatible version,
  realization, Specification, and Evidence produce distinct deterministic
  outcomes.
- Views expose demand, realization and Specification/Evidence traceability
  while avoiding false runtime availability, authorization or execution claims.
- Web and MCP/API surfaces return consistent semantics for a real Cozy -> CNCF
  fixture with exact provenance.

## Non-goals

- Capability source authoring or CML parsing.
- CNCF Capability contract or ABI ownership.
- Use Case quality/coverage review, Actor Goal review, Use Case Slice creation,
  Executable Specification ownership, or ticket management.
- Runtime Capability execution, Authorization decisions, or Guard evaluation.
- AI-based semantic merging of unrelated Capability identities.
- Management hashes or content-derived identity, revision, approval or dependency
  control; apply [repository rules](../rules/repository-rules.md).
- Closing CNCF Phase 78 or Cozy Phase 64 from this repository.

## References

- [Development note](../notes/capability-catalog-and-traceability-proposal.md)
- [Development item journal](../journal/2026/09/2026-09-16-capability-catalog-development-item.md)
- [Phase 11 Checklist](phase-11-checklist.md)
- [Phase 12](phase-12.md)
