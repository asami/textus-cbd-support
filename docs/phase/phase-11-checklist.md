# Phase 11 Checklist: Capability Catalog, Discovery, and Traceability

Status: OPEN
phase=[Phase 11](phase-11.md)
predecessor=[Phase 10.7](phase-10.7.md)
development-item=DEV-CBD-003

Execution preparation: [local evidence and integration points](../notes/phase-11-12-local-execution-preparation.md).
All items remain unstarted. Phase 10.7 is the execution predecessor.

## CAP-11-01: Projection ingestion and integrity

Stage Status:
- Current status: OPEN
- Owner: CBD Support ingestion/model owner
- Update rule: Close only after supported versions, integrity checks, and
  current/history snapshot semantics have executable specification coverage.

- [ ] Define supported CNCF Component Capability, Application Capability, and
      Use Case demand projection versions independently.
- [ ] Record each producer/schema/source revision and real fixture availability;
      promote the consumer design/specification before implementation, using
      explicit typed control under repository rules.
- [ ] Preserve exact Use Case, Application Capability, Component, package,
      source, Capability, realization, and ABI identity supplied upstream.
- [ ] Reject or quarantine ambiguous, duplicate, malformed, stale, and
      incompatible projections without repairing identity by name.
- [ ] Separate current selected state from historical ingestion attempts.
- [ ] Represent a missing/not-yet-supported upstream Use Case demand projection
      explicitly; do not reconstruct it from CML, names, descriptions or AI.
- [ ] Add executable fixtures for valid, stale, incompatible, duplicate,
      ambiguous and unavailable-upstream cases.

## CAP-11-02: Internal catalog and demand/realization traceability model

Stage Status:
- Current status: OPEN
- Owner: CBD Support catalog/model owner
- Update rule: Close only after internal projections preserve upstream meaning
  without introducing competing Use Case/Application/Component Capability identity.

- [ ] Model provided and required `CapabilityRecord` values with typed qualified
      references and exact source/version/ABI provenance.
- [ ] Model `CapabilityDemandTrace` (or repository-equivalent) joining only
      explicit upstream Use Case -> Application Capability -> required Component
      Capability references.
- [ ] Model explicit Operation, Workflow, and StateMachine realization links as
      typed `CapabilityRealizationRef` values; prohibit Operation-name inference.
- [ ] Model Specification and Evidence references separately from Capability,
      demand and realization state.
- [ ] Keep Availability, Authorization, Guard and execution result distinct.
- [ ] Preserve one-to-many demand/provider/realization cardinalities; do not
      assume one Use Case, one Capability, one provider or one realization.
- [ ] Preserve explicit edge attribution across multiple trace paths; prove
      independent endpoint lists cannot manufacture provider/realization joins.
- [ ] Add codec/validator/executable specifications proving round-trip identity,
      cardinality preservation and rejection of dangling/ambiguous explicit refs.

## CAP-11-03: Search, matching, demand trace, and gap analysis

Stage Status:
- Current status: OPEN
- Owner: CBD Support query owner
- Update rule: Close only after exact-identity search/matching, trace and typed
  gap behavior are specified and executable.

- [ ] Provide deterministic query operations equivalent to `getCapability`,
      `findProviders`, `findRequirements`, `traceApplicationCapability`,
      `traceUseCaseDemand`, and `diagnoseCapabilityDemand`.
- [ ] Search by qualified Capability identity and non-authoritative semantic
      metadata while keeping semantic matching identity/compatibility based.
- [ ] Find Components providing or requiring a Capability.
- [ ] Trace an admitted Use Case demand through Application Capability and
      required Component Capability to all explicit provider/realization refs.
- [ ] Return a typed unavailable/unsupported result when Use Case demand input
      is not supplied by an admitted upstream projection.
- [ ] Preserve unauthorized, unavailable, stale and ambiguous source outcomes;
      establish an empty/no-provider result only within an admitted query scope.
- [ ] Distinguish at least: missing Application Capability projection, missing
      required Component Capability, no provider, incompatible provider version,
      missing realization, missing Specification, and missing Evidence.
- [ ] Prohibit semantic matching, provider selection, compatibility or trace
      construction by display-name/description similarity alone.
- [ ] Add executable specifications for multi-provider, multi-realization,
      incompatible-version, partial-evidence and zero-provider cases.

## CAP-11-04: Capability and demand trace views

Stage Status:
- Current status: OPEN
- Owner: Component Dashboard owner
- Update rule: Close only after overview/detail views expose provenance and
  traceability without implying runtime execution or authorization.

- [ ] Add Component Capability overview and detail projections.
- [ ] Add a read-only demand trace projection:
      Use Case -> Application Capability -> Component Capability -> realization.
- [ ] Navigate between admitted Use Case/Application Capability, Capability,
      Component, and realization targets using exact references.
- [ ] Show source/version/provenance and Specification/Evidence links.
- [ ] Render typed gaps independently; do not collapse missing realization,
      unavailable deployment, unauthorized principal, unsatisfied Guard and
      missing Evidence into a generic unavailable state.
- [ ] Clearly label absent upstream Use Case demand projection as unavailable,
      not as an empty/no-demand Use Case.
- [ ] Add view-model tests proving Web rendering cannot manufacture trace links
      absent from the query result.

## CAP-11-05: Web/MCP consistency and end-to-end acceptance

Stage Status:
- Current status: OPEN
- Owner: CBD Support Web/MCP owners with CNCF Phase 78 and Cozy Phase 64
- Update rule: Close only after Web and MCP/API expose the same semantics for
  one real upstream fixture with reproducible evidence.

- [ ] Consume one Cozy-generated, CNCF-admitted Capability projection.
- [ ] When an admitted demand fixture is available, include at least one
      Use Case -> Application Capability -> Component Capability -> explicit
      realization path and one typed gap path.
- [ ] Record the supported or unavailable demand branch explicitly. If no real
      demand fixture is admitted, verify unavailable behavior and identify the
      unexercised positive integration; do not label synthetic proof as upstream
      integration. The real Component Capability fixture remains mandatory.
- [ ] Verify Web and MCP/API return the same qualified identities, provider sets,
      realization sets, gap kinds and provenance for identical queries.
- [ ] Prove no CML reparsing, AI reconstruction or local identity replacement occurs.
- [ ] Prove Phase 11 surfaces are read/query only and cannot create Use Case
      Slice, approve candidate state or mutate canonical CML.
- [ ] Record exact Cozy, CNCF ABI, demand-projection producer and cbd-support
      revisions plus focused validation and review evidence.

## Closure

- [ ] Close Phase 11 only after every item is checked or explicitly relocated
      under successor authority.
