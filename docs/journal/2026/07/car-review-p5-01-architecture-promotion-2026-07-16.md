# CAR Review P5-01 Architecture Promotion — 2026-07-16

## Event

Phase 5 began while Phase 4 P4-45 remained independently on hold. The first
bounded implementation slice promoted the settled CAR Review ownership and
provider decisions from notes and same-day journals into stable design.

## Evidence Reviewed

- `docs/notes/car-review-design-proposal.md`
- `docs/journal/2026/07/car-review-product-boundary-2026-07-16.md`
- `docs/journal/2026/07/car-review-provider-sbt-cozy-integration-2026-07-16.md`
- `docs/journal/2026/07/car-review-ai-integration-modes-2026-07-16.md`
- the active Textus AI Phase 1 strategy, dashboard, checklist, and CAR Review
  runtime design in the local `textus-ai` repository

## Promoted Decision

`docs/design/car-review-architecture.md` is now the normative P5-01 decision.
It fixes CBD Support as the sole CAR Review product, canonical report, Review
Run, reconciliation, user-surface, and gate-policy owner. Cozy, sbt-cozy,
Textus AI, CNCF, SIE, catalog, runtime, and other integrations contribute
versioned attributable evidence without producing competing reports.

The design also fixes positive call directions and prohibited implementation
dependencies. In particular, sbt-cozy may call Cozy and CBD Support; Cozy does
not call CBD Support; CBD Support does not import sbt-cozy implementation; and
Textus AI supplies provider-neutral AI execution without acquiring Review
policy or report authority.

## Deliberate Boundary

This slice does not define exact provider JSON, canonical report fields,
compatibility ranges, security limits, or runtime implementation. Those remain
P5-02 through P5-04 work. No publication or external provider invocation was
performed.
