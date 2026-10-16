# Capability Catalog and Traceability Development Item

Date: 2026-09-16
Status: admitted for planning
Development item: DEV-CBD-003
Target phase: Phase 11

## Record

SimpleModeling adopted Capability Model as a non-instantiated model IR, and
CNCF requires a public Component Capability specification surface. Textus CBD
Support therefore needs a dedicated consumer capability for catalog ingestion,
search, comparison, visualization, and traceability.

This record admits DEV-CBD-003 and creates Phase 11 as its execution ledger. It
is chronological and non-normative. Behavior becomes authoritative only after
promotion into design/specification and executable specifications.

## Cross-repository boundary

- CNCF Phase 78 owns the public Capability projection and compatibility rules.
- Cozy Phase 64 owns Capability Model source support and projection generation.
- Textus CBD Support Phase 11 owns supported-version ingestion, internal
  snapshot/index projection, query behavior, Web/MCP presentation, and
  provenance diagnostics.

Phase 11 must not create a local Capability identity, reparse CML, or infer
semantics from Operation names. It consumes admitted CNCF metadata and keeps
Availability, Authorization, Guard, realization, and execution evidence
separate.

## Dependency order

Phase 11 may freeze its consumer design while upstream contracts are planned,
but implementation acceptance depends on CNCF Phase 78 and a Cozy Phase 64
fixture. The end-to-end acceptance order is CNCF contract, Cozy producer, then
cbd-support consumer.

## References

- [Proposal](../../../notes/capability-catalog-and-traceability-proposal.md)
- [Phase 11](../../../phase/phase-11.md)
- `asami/goldenport-cncf/docs/phase/phase-78.md`
- `asami/cozy/docs/phase/phase-64.md`

## Applied planning split — 2026-10-04

The owner invoked `$cncf-split-phase 11` after the same-task full-Phase entry
reported SPLIT_REQUIRED / time-bound. The original chronological record above
is preserved; it describes the pre-split scope and does not select current
item ownership.

- [Phase 11](../../../phase/phase-11.md) retains the original identity and owns
  CAP-11-01–03: upstream admission, ingestion, catalog and queries.
- [Phase 11.1](../../../phase/phase-11.1.md) owns CAP-11-04–05: views, Web/MCP
  and the original real Cozy -> CNCF -> CBD Support acceptance.
- Phase 11.1 is the final full-SBT validation owner for both units; independent
  focused validation, reviews, Step acceptance and release remain per unit.
- Both units remain unstarted. Phase 12 and later identities are unchanged.
  This split grants no upstream repository mutation authority or acceptance.

The [source dated split note](../../../phase/phase-11.md#applied-split--2026-10-04)
records estimates, calibration limits and the exact accepted-contract handoff.
