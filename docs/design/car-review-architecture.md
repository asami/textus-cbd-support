# CAR Review Architecture and Ownership

status=stable
decision_scope=P5-01
updated_at=2026-07-16

## Purpose

This document fixes the ownership, authority, dependency, and execution
boundaries for CAR Review. It is normative for Phase 5 implementation.
Provider JSON fields, canonical report fields, compatibility ranges, and
security limits are defined by the P5-02 through P5-04 specifications rather
than by this architecture document.

## Product Ownership

Textus CBD Support owns CAR Review as one application exposed through Web,
CLI, report, CI, and authorized MCP projections. It is the only owner of:

- Review Run admission, lifecycle, persistence, comparison, and retention;
- provider registration, selection, orchestration, compatibility admission,
  and limitation handling;
- cross-provider reconciliation and quality-capability assessment;
- the canonical Review Report and its report, gate, and exit semantics;
- development, CI, and release Review profiles;
- Evidence admission, redaction, suppression, cache, and cost policy; and
- user-facing Review behavior and authorization.

No provider produces a competing canonical Review Report or independently
defines the CBD gate result.

## Component Responsibilities

| Component | Owned responsibility | Explicitly not owned |
| --- | --- | --- |
| CBD Support | Review Application, Runs, canonical report, reconciliation, quality assessment, user surfaces, gate policy | Cozy analysis internals, sbt task implementation, AI adapter wire protocols |
| Cozy | Deterministic CAR/CML/model/build/package/ABI/documentation analysis and preservation of focused `cozy car lint` behavior | Canonical Review Report, report history, quality policy, Web UI, CBD gate |
| sbt-cozy | Sbt build/test/package evidence and the local/CI bridge to Cozy and CBD Support | Cross-provider assessment, canonical report construction, independent gate interpretation |
| Textus AI | Provider-neutral `AiRunner` execution, structured generation, provider selection, adapter normalization, and safe execution facts | CAR Review policy, admitted Evidence, canonical observations, Assurance, report, or gate decisions |
| CNCF | Provider/driver and Job lifecycle boundaries, ExecutionContext, CallTree, authorization, cancellation, and structured failure mechanisms | CAR Review product and quality policy |
| SIE / BoK providers | Attributable terminology and semantic evidence | CAR implementation facts or canonical Review conclusions |
| Runtime evidence providers | Attributable bounded runtime observations | Static-to-operational maturity promotion without admitted runtime evidence |

Catalog, runtime, SIE, BoK, and AI integrations are providers of attributable
Evidence or Observations. Their absence, disablement, incompatibility, or
failure remains visible as a limitation or Unknown and is never converted into
Assurance.

## Canonical Processing Boundary

All entry points use the same CBD Review Application:

```text
Web / CBD CLI / sbt-cozy / private command surface
                       |
                       v
              CBD Review Application
                       |
                  Review Run (Job)
                       |
          +------------+-------------+
          |            |             |
          v            v             v
        Cozy       sbt/CNCF      SIE/runtime/AI
      provider      providers       providers
          |            |             |
          +------------+-------------+
                       v
              attributable bundles
                       |
                       v
        admission -> reconciliation -> assessment
                       |
                       v
             canonical Review Report
                       |
                       v
       Web / CLI / JSON / HTML / SARIF / MCP
```

Renderers and projections do not rerun analysis or derive conclusions that are
absent from the canonical report. SARIF is an explicitly lossy Finding
projection; it is not a second report authority.

## Provider Boundary

CBD Support and providers exchange a versioned, transport-neutral Review
Provider contract. A provider descriptor identifies the provider, version,
rule set, supported schemas, capabilities, and limitations. A provider result
is an attributable evidence bundle, not a complete Review Report.

CBD Support admits a bundle only after validating its schema, provider and rule
identity, capability, target identity and digest, bounds, and compatibility.
It preserves provider identity through reconciliation. It does not silently
translate incompatible fields, select an implicit source winner, fabricate
missing evidence, or rerun a bundle already admitted for the same Review Run.

The initial transport may be a bounded local command, an in-process adapter,
or a CNCF provider call. Transport choice does not change ownership or report
semantics. Filesystem, process, and network access remain behind authorized
CNCF provider/driver boundaries.

## Submission Transport Adapters

The initial P5-31 provider-document submission has two supported adapters over
the exact same `textus.cbd.review-submission.v1` body:

- the private HTTP `POST` adapter for an authorized CBD server; and
- the local CBD CLI adapter using one JSON document on stdin and one JSON
  response on stdout.

Both adapters enforce their own content-type/input-byte bound and resolve
caller roles before invoking the same CBD submission application. Neither
accepts a workspace path, command, environment, credential, report template,
or gate from the caller. The HTTP adapter is disabled for the standard offline
CI policy; the CLI adapter remains local and receives no source path.

## Call and Dependency Directions

The supported local and CI route is:

```text
sbt-cozy -> Cozy analyzer -> Cozy evidence bundle
sbt-cozy -> sbt tasks      -> sbt evidence bundle
sbt-cozy -> CBD Review Application -> canonical report and gate result
```

For an admitted CAR artifact or configured development target, CBD Support may
invoke Cozy or another provider through the Review Provider protocol.

The following dependency rules are normative:

- Cozy does not call or depend on CBD Support.
- CBD Support does not import or depend on sbt-cozy implementation classes.
- sbt-cozy may call both Cozy and the CBD Review Application through their
  public command/protocol surfaces.
- CBD Support may call Cozy only through the provider protocol or an adapter
  implementing that protocol.
- When sbt-cozy supplies an admitted Cozy bundle, CBD Support does not invoke
  Cozy again for that Review Run.
- Shared schemas may later move to a neutral artifact without transferring CAR
  Review product ownership away from CBD Support.

## AI Execution Boundary

AI Review is optional and advisory. CBD Support constructs and redacts bounded
Evidence, selects the prompt and output contracts, admits results, reconciles
them with deterministic Evidence, and owns cache and cost policy.

Textus AI is the provider-neutral execution dependency for admitted local
Gemma/Ollama and explicitly enabled OpenAI or Google Gemini execution. CBD
Support uses `AiRunner`, structured `generateRecord`, and purpose profiles; it
does not embed provider credentials, wire APIs, model aliases, or
provider-specific response parsing.

AI output may create a candidate Finding, explanation, grouping, question,
Unknown, or limitation. It cannot suppress, lower, replace, or override a
deterministic Finding and cannot establish final Assurance by itself.

## User and Automation Surfaces

- Web UI and CBD CLI may start and observe authorized Review Runs through the
  same application contract.
- sbt-cozy may submit bounded local evidence and consume the returned report
  and gate result in development or CI.
- MCP exposes only explicitly authorized, bounded, redacted, read-only queries
  over admitted Runs and reports by default.
- Review start, cancellation, retention administration, filesystem access,
  external-provider enablement, and cost-bearing AI execution remain private
  unless a later policy explicitly admits them.
- Existing publish, publishLocal, distribution, and deployment tasks are not
  implicitly redefined by CAR Review.

## Decision Consequences

1. CBD Support is developed first as the Review product and contract owner.
2. Cozy, sbt-cozy, Textus AI, CNCF, SIE, catalog, and runtime changes are made
   only to satisfy their attributable provider responsibilities.
3. A provider can evolve or be substituted without changing canonical Review
   ownership.
4. An unavailable provider reduces assessment coverage and is reported; it
   does not make the remaining assessment appear complete.
5. Exact schema and compatibility work proceeds next under P5-02, followed by
   canonical report/run semantics under P5-03 and security policy under P5-04.

## Superseded Exploratory Decisions

The 2026-07-15 proposal that placed the complete Review engine and user-facing
command in Cozy is superseded for ownership purposes. Historical journals
remain unchanged as chronological evidence. Unresolved schema and policy ideas
in `docs/notes/car-review-design-proposal.md` remain exploratory until promoted
by later Phase 5 checklist items.
