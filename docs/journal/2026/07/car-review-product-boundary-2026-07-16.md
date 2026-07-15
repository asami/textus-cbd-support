# CAR Review Product Boundary Revision — 2026-07-16

## Context

The 2026-07-15 consideration placed the local CAR Review engine and user-facing
command in Cozy. A subsequent product-direction clarification established that
`textus-cbd-support` is intended to provide not only MCP operations, but also a
CBD-support Web UI, report generation, and CLI workflows.

That direction makes Review part of the CBD Support product boundary. The
previous conclusion that Cozy owns the complete Review engine and CLI was too
narrow and is superseded by this entry. The historical entry remains unchanged
as the record of the earlier reasoning.

## Revised Decision

CBD Support owns:

- the Review Application and Review Run lifecycle;
- CNCF Job-backed asynchronous execution;
- target authorization and provider selection;
- the canonical Review Report and its persistence/comparison policy;
- cross-provider observation reconciliation and quality-capability assessment;
- development, CI, and release profiles;
- suppression and report-retention policy;
- Web UI, user-facing CLI, and report rendering; and
- explicitly authorized read-only MCP report queries.

Cozy owns:

- CAR/CML/build/package/ABI/documentation inspection;
- Cozy-version-specific deterministic rules and limitations;
- the existing focused `cozy car lint` behavior; and
- emission of a versioned, attributable `CozyEvidenceBundle` for CBD Support.

Cozy does not own the complete report, report history, Web UI, cross-provider
quality assessment, AI policy, or CBD Support's profile exit policy.

## Integration Decision

CBD Support sends a versioned `CarAnalysisRequest`; Cozy returns a bounded
`CozyEvidenceBundle` containing analyzer identity, target digest, evidence,
deterministic observations, and limitations. The bundle schema, not Cozy's
internal Scala API, is the integration contract.

The initial transport may be a Cozy analyzer command exchanging JSON. The
protocol remains transport-neutral so an in-process or remote provider can be
introduced later. Execution must stay behind an authorized CNCF provider or
driver boundary. CBD component logic must not call an unmanaged process,
filesystem, or remote service directly.

An analyzer bundle is not a complete Review Report. CBD Support combines it
with CNCF runtime, catalog, SIE/BoK, test, documentation, and optional AI
evidence while preserving provider identity.

## Surface Decision

The primary user-facing CLI belongs to CBD Support, provisionally:

```text
textus cbd review <project-root|car>
```

Cozy retains `cozy car lint` and may expose a machine-facing inspection command
such as `cozy car inspect --format evidence-json`. Web UI and CLI must call the
same CBD Review Application and render the same canonical report.

Review execution and report access are separate surfaces:

- starting, cancelling, or retaining a review is a command and is not
  MCP-ready by default;
- run status and stored reports are queries;
- bounded, authorized, redacted report queries may be MCP-ready; and
- starting a review through MCP requires a later explicit policy for target
  access, external calls, generated artifacts, and AI cost.

## Reasons

1. Web UI, CLI, MCP report access, and stored reports need one application
   policy and one canonical report.
2. Cozy has the strongest CAR/CML knowledge, but does not need to own catalog,
   BoK, runtime, AI, quality-view, or report-history responsibilities.
3. A versioned evidence boundary avoids a binary dependency between the CBD
   product and Cozy internals.
4. Provider identity prevents Cozy findings, catalog facts, runtime evidence,
   and AI interpretations from being merged into unattributable conclusions.
5. CNCF Job execution allows long-running analysis to serve Web and CLI without
   creating a separate synchronous implementation.

## Remaining Decisions

- final CBD Support CLI command and packaging;
- ownership and publication location of the analyzer-bundle schema;
- first analyzer transport and Cozy-version discovery;
- Review Run storage, immutability, retention, and authorization;
- exact MCP-ready report query set;
- target-root and uploaded-CAR admission policy;
- server-side test execution policy; and
- AI provider enablement and cost policy.

## Documentation Result

`docs/notes/car-review-design-proposal.md` was revised to reflect this product
boundary, integration protocol, Review Job model, Web/CLI/MCP separation, and
incremental delivery order. The proposal remains non-normative until promoted
to design and specification with executable contracts.

## Same-Day Follow-up

`car-review-provider-sbt-cozy-integration-2026-07-16.md` generalizes the
Cozy-specific request/bundle names in this entry to the common Review Provider
contract and records `sbt-cozy`'s CI/CD bridge role. The CBD Support product
ownership decided here remains unchanged; only the provider and integration
model is superseded.
