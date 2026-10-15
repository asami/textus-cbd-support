# Phase 11.1 Checklist: Capability Views, Web/MCP, and Acceptance

Status: OPEN / NOT STARTED
phase=[Phase 11.1](phase-11.1.md)
predecessor=[Phase 11](phase-11.md)
successor=[Phase 12](phase-12.md)
development-item=DEV-CBD-003

Split from Phase 11 on 2026-10-04. This ledger owns CAP-11-04–05 exactly once,
with their original requirements and IDs. Consume the accepted Phase 11
ingestion/catalog/query handoff; all items remain unstarted.

## CAP-11-04: Capability and demand trace views

Stage Status:
- Current status: OPEN
- Owner: Component Dashboard owner
- Update rule: Close only after overview/detail views expose provenance and
  traceability without implying runtime execution or authorization.

- [ ] Expose separate Component Usage and Component contract views, preserving
      inductive evidence, deductive guarantees and explicit many-to-many links.
- [ ] Remove old Application API/label/fixture names with no compatibility aliases;
      compile regenerated callers and use traceComponentUsageCapability.
- [ ] Add Component Capability overview and detail projections.
- [ ] Add a read-only demand trace projection:
      Use Case -> Component Usage Capability -> Component Capability -> realization.
- [ ] Navigate between admitted Use Case/Component Usage Capability, Capability,
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
- [ ] Consume CAP-11-PKG01's real owning Directory/CAR handoff, including
      packaged Component Usage metadata and semantic parity. Develop against the
      Directory directly; do not require CAR creation for routine reads or
      substitute a separately supplied legacy Application JSON file.
- [ ] When an admitted demand fixture is available, include at least one
      Use Case -> Component Usage Capability -> Component Capability -> explicit
      realization path and one typed gap path.
- [ ] Record the supported or unavailable demand branch explicitly. If no real
      demand fixture is admitted, verify unavailable behavior and identify the
      unexercised positive integration; do not label synthetic proof as upstream
      integration. The real Component Capability fixture remains mandatory.
- [ ] Verify Web and MCP/API return the same qualified identities, provider sets,
      realization sets, gap kinds and provenance for identical queries.
- [ ] Prove no CML reparsing, AI reconstruction or local identity replacement occurs.
- [ ] Prove the split sequence's surfaces are read/query only and cannot create Use Case
      Slice, approve candidate state or mutate canonical CML.
- [ ] Record exact Cozy, CNCF ABI, demand-projection producer and cbd-support
      revisions plus focused validation and review evidence.

## Closure

- [ ] Verify Phase 11's committed deferred-validation handoff and ancestry.
- [ ] Close CAP-11-04–05 only after the mandatory real Component Capability
      fixture and the explicitly selected supported/unavailable demand branch.
- [ ] Run the repository full SBT suite once on this final release tree for
      the complete PHASE-11 -> PHASE-11.1 sequence; record actual results.
- [ ] Complete focused validation, independent Step and full Phase review,
      applicable CAR lint, closure journals and this Phase's release commit.
- [ ] Synchronize the split sequence's completion projections and DEV-CBD-003
      status without claiming that Phase 12 or upstream Phases have closed.
