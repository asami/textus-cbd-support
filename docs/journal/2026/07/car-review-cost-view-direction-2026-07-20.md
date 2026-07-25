# CAR Review Cost View Direction — 2026-07-20

status=consideration
updated_at=2026-07-20
tag=textus-cbd-support, car-review, cost, cncf, static-web, gemma, mcp

## Context

CNCF treats operational-cost optimization as one of its development themes.
Recent work already includes two concrete forms of architectural cost
reduction:

- using a Static Web App to reduce unnecessary access to a dynamic Web
  server; and
- using Gemma with MCP to handle suitable AI work at lower cost than routinely
  sending all work to a commercial or larger model.

The existing CAR Review proposal defined overview, CNCF, implementation, and
quality projections, but did not give these cost-oriented decisions a distinct
place. Folding them into a generic quality view would make expected savings,
actual measurements, and transferred operational cost difficult to compare.

## Decision

Add Cost View as a first-class CAR Review projection owned by CBD Support.
Cozy and other components may act as evidence providers; they do not own the
cross-provider cost conclusion. The Cost View is derived from the same
canonical Evidence and Observations as other views and must not rerun provider
analysis or create unattributable renderer-local recommendations.

The view covers more than direct infrastructure or API charges. Its scope
includes:

- infrastructure consumption;
- operational and support burden;
- development and maintenance effort;
- dependency change and migration cost;
- AI-model and token or equivalent usage; and
- risk-adjusted cost together with quality trade-offs.

An optimization records its current architecture, cost driver, proposed
change, expected reduction, measured reduction, quality constraints,
operational trade-offs, supporting Evidence, and confidence. Expected and
measured reductions remain separate. Missing price or runtime data produces an
explicit unknown or limitation rather than a fabricated monetary amount.

## Initial Scenarios

### Static Web App

Review whether static delivery removes avoidable dynamic-server work. Candidate
evidence includes origin requests, cache-hit rate, transfer volume, compute
utilization, update frequency, and changes in operational burden. Reduced
server access is the optimization mechanism; the report must still expose
cache invalidation, delivery, security, or freshness trade-offs.

### Gemma plus MCP

Review whether bounded work is routed to Gemma or another local/smaller model
before commercial-model fallback. Candidate evidence includes model routing,
request and token usage, latency, quality result, fallback rate, local compute,
model-management effort, and avoided external-provider usage. Local inference
is not automatically classified as cheaper: its compute and operational cost,
quality constraints, and fallback behavior remain part of the assessment.

## Consequences

- CBD Support needs a stable cost projection contract and read surfaces for
  CLI, Web, reports, and authorized MCP queries.
- Runtime, Textus AI, deployment, billing, Cozy, and sbt-cozy integrations may
  contribute attributable cost evidence through provider boundaries.
- The first implementation can report evidence-backed cost drivers and
  optimization opportunities before exact currency calculations are
  available.
- Cross-version or before/after comparisons require normalized units,
  comparison periods, freshness, and target identity before measured savings
  can be considered reliable.

## Documentation Result

`docs/notes/car-review-design-proposal.md` now defines Cost View, adds it to the
CLI view selection, records the two initial scenarios, and adds a dedicated
incremental-delivery slice and normalization question. This journal entry is
the chronological rationale; the note remains exploratory and non-normative.
