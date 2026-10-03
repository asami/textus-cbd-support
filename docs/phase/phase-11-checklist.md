# Phase 11 Checklist: Capability Catalog, Discovery, and Traceability

Status: OPEN
phase=[Phase 11](phase-11.md)
predecessor=[Phase 10.7](phase-10.7.md)
development-item=DEV-CBD-003

## CAP-11-01: Projection ingestion and integrity

Stage Status:
- Current status: OPEN
- Owner: CBD Support ingestion/model owner
- Update rule: Close only after supported versions, integrity checks, and
  current/history snapshot semantics have executable specification coverage.

- [ ] Define supported CNCF Capability projection versions.
- [ ] Preserve exact Component, package, source, Capability, and ABI identity.
- [ ] Reject or quarantine ambiguous, duplicate, malformed, stale, and
      incompatible projections.
- [ ] Separate current selected state from historical ingestion attempts.

## CAP-11-02: Internal catalog and traceability model

Stage Status:
- Current status: OPEN
- Owner: CBD Support catalog/model owner
- Update rule: Close only after internal projections preserve upstream meaning
  without introducing a competing Capability identity.

- [ ] Model provided and required Capability records.
- [ ] Model explicit Operation, Workflow, and StateMachine realization links.
- [ ] Model Specification and Evidence references separately from Capability
      state.
- [ ] Keep Availability, Authorization, and Guard distinct.

## CAP-11-03: Search, matching, and gap analysis

Stage Status:
- Current status: OPEN
- Owner: CBD Support query owner
- Update rule: Close only after exact-identity search/matching and gap behavior
  are specified and executable.

- [ ] Search by qualified Capability identity and semantic metadata.
- [ ] Find Components providing or requiring a Capability.
- [ ] Report unmet requirements and incompatible versions explicitly.
- [ ] Prohibit matching by display name or description similarity alone.

## CAP-11-04: Capability views

Stage Status:
- Current status: OPEN
- Owner: Component Dashboard owner
- Update rule: Close only after overview/detail views expose provenance and
  traceability without implying runtime execution or authorization.

- [ ] Add Component Capability overview and detail projections.
- [ ] Navigate between Capability, Component, and realization targets.
- [ ] Show source/version/provenance and Specification/Evidence links.
- [ ] Distinguish missing realization, unavailable deployment, unauthorized
      principal, unsatisfied Guard, and missing Evidence.

## CAP-11-05: Web/MCP consistency and end-to-end acceptance

Stage Status:
- Current status: OPEN
- Owner: CBD Support Web/MCP owners with CNCF Phase 78 and Cozy Phase 64
- Update rule: Close only after Web and MCP/API expose the same semantics for
  one real upstream fixture with reproducible evidence.

- [ ] Consume one Cozy-generated, CNCF-admitted Capability projection.
- [ ] Verify consistent Web and MCP/API search/detail results.
- [ ] Prove no CML reparsing or local identity replacement occurs.
- [ ] Record exact Cozy, CNCF ABI, and cbd-support revisions plus focused
      validation and review evidence.

## Closure

- [ ] Close Phase 11 only after every item is checked or explicitly relocated
      under successor authority.
