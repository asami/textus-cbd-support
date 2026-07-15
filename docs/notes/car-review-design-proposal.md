# CAR Review Design Proposal

Status: exploratory and non-normative

This note develops the proposal recorded in
`docs/journal/2026/07/car-review-spec-study-handoff.md`. It is intended for
promotion to `docs/design` and `docs/spec` only after the open contracts have
been resolved and executable examples have validated the model.

The responsibility boundary was revised on 2026-07-16: CBD Support leads the
CAR Review product, application workflow, provider orchestration, user
surfaces, and canonical report. Cozy and other components participate as
versioned Review Providers. `sbt-cozy` connects the local build and CI/CD
workflow to both the Cozy provider and the CBD Review Application.

## Purpose

CAR Review explains how a CAR realizes framework contracts and quality
capabilities. It must make three different questions answerable from one body
of evidence:

- which CNCF mechanisms the CAR declares and uses;
- where implementation findings, assurances, and assessment gaps occur; and
- how far relevant quality capabilities are established by CNCF, the CAR,
  configuration, deployment, or an external provider.

CAR Review is broader than CAR lint. Lint reports known violations. Review must
also retain positive assurance, explicit non-applicability, and unknown or
unassessed areas so that absence of a warning is never presented as proof of
quality.

## Current Baseline

The current Cozy `car lint` command already aggregates deterministic checks for
build declarations, CML, documentation, and ABI compatibility. Its result is a
flat sequence of `OK`, `WARN`, and `FAIL` findings rendered as text or JSON.
This is useful evidence, but it does not yet model:

- the evidence from which a conclusion was derived;
- positive assurances and unknown assessments;
- CNCF, implementation, and quality-capability mappings;
- applicability, coverage, maturity, or confidence;
- static, runtime, heuristic, and AI provenance; or
- multiple projections of one common result.

`textus-cbd-support` currently owns read-only, evidence-bearing component
catalog and usage retrieval and has a CNCF Web route declaration. Its intended
product boundary also includes CBD-oriented Web UI, report generation, and CLI
workflows. SIE owns BoK terminology and semantic retrieval. Cozy currently
owns local CAR/CML understanding and the deterministic CAR lint baseline.

## Responsibility Boundary

### Textus CBD Support

CBD Support should own CAR Review as a CBD-support product capability. MCP,
Web UI, CLI, and report generation must converge on the same Review
Application instead of implementing separate review policies. Its
responsibilities are:

- authorize and normalize the requested CAR or project target;
- create and manage Review Runs as CNCF Jobs;
- register, select, invoke, and observe compatible Review Providers;
- reconcile Cozy, CNCF runtime, catalog, BoK, test, documentation, and AI
  evidence without silently selecting a source winner;
- own cross-provider rules, capability assessment, profile policy,
  suppression, and report retention;
- build, validate, store, compare, and render the canonical Review Report;
- provide the CBD Review Web UI and user-facing CLI; and
- publish only explicitly safe, read-only report queries through MCP.

### Cozy

Cozy should own the CAR/CML-specific deterministic analyzer. It already owns
CML interpretation, generated model metadata, CAR packaging, ABI inspection,
and CAR lint. Its responsibilities are:

- inspect a local project or built CAR using the matching Cozy semantics;
- collect CML/model, generated contract, build, package, ABI, and
  documentation evidence;
- produce deterministic Cozy-owned observations and explicit analyzer
  limitations;
- identify the Cozy, CML, CAR schema, rule-set, and supported CNCF versions;
- emit a versioned `ReviewEvidenceBundle` with Cozy provider identity; and
- retain `cozy car lint` as a focused low-level validation command.

Cozy does not own the complete Review Report, Web UI, report history,
cross-provider quality assessment, AI policy, or CBD Support's CI/release exit
policy.

### sbt-cozy

`sbt-cozy` should connect an sbt build to CAR Review without owning the Review
policy. It has two roles:

- a local Review Provider that records generation, compilation, test,
  dependency-resolution, CAR-build, and related sbt task evidence; and
- a CI/CD client that invokes the Cozy analyzer, submits the resulting Cozy and
  sbt evidence bundles to CBD Support, receives the canonical report, and
  applies the returned gate result to the sbt task.

`sbt-cozy` must not implement independent quality assessment or reinterpret a
CBD Support report. CBD Support must not depend on `sbt-cozy` classes or task
implementation. Their boundary is the versioned Review protocol and report
artifacts.

### CNCF

CNCF should own the vocabulary and runtime meaning of CNCF mechanisms, such as
Action and UnitOfWork boundaries, authorization, jobs, CallTree,
observability, retry, and compensation. CNCF may provide versioned feature
descriptors and runtime-evidence exporters. CAR Review consumes those
contracts; it must not infer framework semantics from ad hoc source patterns.

### SIE

SIE may supply BoK terminology and semantic evidence for documentation,
domain-language, and AI-readiness rules. It must not synthesize CAR facts or
replace deterministic model and source analysis.

### AI provider

An AI provider evaluates only bounded, structured evidence supplied by the
review engine. It does not receive an unstructured repository dump and cannot
override deterministic findings. Provider, model, prompt contract, input
digest, response digest, and limitations must be recorded as evidence.

## Review Provider Integration

CBD Support and every analyzer or evidence source should exchange a versioned
provider bundle rather than share the full Review implementation. Cozy is the
first deterministic CAR/CML provider, not a special path embedded in the
canonical report model.

```scala
case class ReviewProviderDescriptor(
  providerId: ReviewProviderId,
  providerVersion: Version,
  schemaVersions: Set[ReviewSchemaVersion],
  capabilities: Set[ReviewProviderCapability]
)

case class ReviewProviderRequest(
  schemaVersion: ReviewSchemaVersion,
  reviewId: ReviewId,
  target: ReviewTarget,
  requestedEvidence: Set[EvidenceKind],
  baseline: Option[ReviewBaseline],
  rules: ProviderRuleSelection,
  limits: ReviewProviderLimits
)

case class ReviewEvidenceBundle(
  schemaVersion: ReviewSchemaVersion,
  provider: ReviewProviderDescriptor,
  target: ReviewedTarget,
  targetDigest: EvidenceDigest,
  evidence: Vector[ReviewEvidence],
  observations: Vector[ReviewObservation],
  limitations: Vector[ReviewLimitation]
)
```

Each descriptor records the provider and rule-set versions, supported schema
versions, and capabilities. A Cozy descriptor additionally records supported
CML, CAR, and CNCF contract ranges. CBD Support validates these fields before
admitting the bundle. An incompatible bundle becomes an attributable `Unknown`
or failed Review Run; CBD Support must not reinterpret it as current evidence.
CBD Support resolves its development, CI, or release profile into an explicit
provider-specific rule selection and limits; Cozy does not interpret the
product-level profile policy.

The protocol should be transport-neutral. The initial adapter may invoke a
Cozy analyzer command and exchange bounded JSON. A later implementation may
use an in-process provider or remote analyzer without changing the report
model. Process execution, filesystem access, and remote calls must remain
behind an authorized CNCF provider/driver boundary rather than appearing
directly in generated component logic.

The JSON schema is the integration contract. CBD Support initially owns the
Review Report schema; the provider-request and evidence-bundle schemas are
shared Textus contracts.
Neither side should require a binary dependency on the other's application
implementation. If additional consumers emerge, the protocol model can be
extracted to a neutral library without moving Review product ownership away
from CBD Support.

### Invocation Topologies

Local development and CI/CD use this route:

```text
sbt-cozy -> Cozy analyzer -> Cozy ReviewEvidenceBundle
sbt-cozy -> sbt tasks      -> sbt ReviewEvidenceBundle
sbt-cozy -> CBD Review Application -> canonical report and gate result
```

The developer workspace remains local. `sbt-cozy` sends bounded evidence
bundles rather than granting a CBD Support server arbitrary filesystem access.

Web review of an admitted uploaded CAR may use this route:

```text
CBD Review Application -> Review Provider protocol -> Cozy analyzer
```

The dependency and call directions are explicit:

- `sbt-cozy` may invoke both Cozy and CBD Support;
- CBD Support may invoke Cozy through the Review Provider protocol;
- Cozy never invokes or depends on CBD Support; and
- CBD Support never imports or depends on the `sbt-cozy` implementation.

Only one topology performs a given provider analysis for a Review Run. When
`sbt-cozy` supplies a valid Cozy bundle, CBD Support admits that bundle rather
than invoking the Cozy provider again.

## Processing Model

The proposed flow is:

```text
Web UI / CBD CLI / sbt-cozy / authorized MCP query
                   |
                   v
         CBD Review Application
                   |
              Review Job
                   |
       +-----------+-----------+
       |                       |
       v                       v
Cozy provider           other providers
CML/CAR/build/ABI       sbt/CNCF/runtime/catalog/SIE/AI
       |                       |
       +-----------+-----------+
                   v
             Evidence Bundles
                   |
                   v
 normalization / Finding / Assurance / Unknown
                   |
                   v
       cross-provider capability assessment
                   |
                   v
       canonical CBD Review Report
                   |
                   v
 Web / CLI / JSON / HTML / SARIF / authorized MCP
```

Analysis is performed before view projection. A renderer must not rerun a rule
or derive a conclusion that is absent from the canonical report.

## Canonical Review Report

CBD Support owns the canonical JSON report as the interchange and persistence
form. Text, HTML, and SARIF are projections and may be lossy. Analyzer bundles
are inputs to this report and are not themselves complete Review Reports.

```scala
case class CarReviewReport(
  schemaVersion: ReviewSchemaVersion,
  reviewId: ReviewId,
  target: CarReviewTarget,
  execution: ReviewExecution,
  evidence: Vector[ReviewEvidence],
  observations: Vector[ReviewObservation],
  assessments: Vector[CapabilityAssessment],
  limitations: Vector[ReviewLimitation],
  baseline: Option[ReviewBaseline]
)
```

`ReviewExecution` records tool, Cozy, CNCF, rule-set, and selected profile
versions plus start and completion times. `CarReviewTarget` records the CAR
identity, normalized project root or artifact identity, and a digest of the
reviewed inputs. A report must remain attributable even after the working tree
changes.

### Evidence

Evidence is an observed input, not a judgment.

```scala
case class ReviewEvidence(
  id: EvidenceId,
  kind: EvidenceKind,
  origin: EvidenceOrigin,
  subject: ReviewSubject,
  location: Option[ReviewLocation],
  digest: Option[EvidenceDigest],
  observedAt: Option[Instant],
  payload: EvidencePayload
)
```

Initial evidence kinds should include:

- CML/model metadata;
- CAR/project/package metadata;
- public operation and ABI contracts;
- Scala implementation and dependency structure;
- configuration;
- executable specification and test results;
- documentation;
- CNCF feature declarations;
- supplied runtime/CallTree/metric observations;
- catalog and BoK citations; and
- prior Review Report or CAR baseline.

Source content must not be copied into the report when a bounded fact, location,
and digest are sufficient. Credentials, tokens, request bodies, and other
sensitive values are excluded or redacted before evidence is persisted or sent
to AI.

### Observations

An observation is a rule's conclusion over one or more evidence items.

```scala
enum ObservationType:
  case Finding
  case Assurance
  case Unknown

case class ReviewObservation(
  id: ObservationId,
  observationType: ObservationType,
  rule: ReviewRuleRef,
  subject: ReviewSubject,
  severity: Option[ReviewSeverity],
  confidence: ReviewConfidence,
  message: String,
  rationale: String,
  evidence: Vector[EvidenceId],
  locations: Vector[ReviewLocation],
  mappings: ReviewMappings,
  disposition: ObservationDisposition
)
```

Severity applies to findings. Confidence applies to all observation types.
`Unknown` is required when evidence is missing, a rule does not support the
language or CNCF version, or analysis is intentionally disabled. A rule must
not emit an Assurance merely because it emitted no Finding.

`ObservationDisposition` allows an observation to remain visible while being
accepted, suppressed, or deferred. Suppression requires a stable rule ID,
bounded subject, justification, author, and optional expiry. Initial support
should use a project-level review configuration; source-code suppression
comments can be considered later.

### Capability Assessments

Capabilities are reusable definitions mapped into one or more quality views.
For example, idempotency can contribute to resilience, reliability,
testability, and domain realization without being duplicated.

```scala
case class CapabilityAssessment(
  capability: QualityCapabilityId,
  applicability: Applicability,
  maturity: QualityMaturity,
  coverage: Option[Coverage],
  confidence: ReviewConfidence,
  providers: Vector[AssuranceProvider],
  observations: Vector[ObservationId],
  evidence: Vector[EvidenceId]
)
```

The initial maturity vocabulary is `Unassessed`, `Missing`, `AdHoc`,
`Partial`, `Established`, `Verified`, and `Operational`.

- `Established` requires consistent design and implementation evidence.
- `Verified` additionally requires executable or deterministic verification.
- `Operational` requires supplied runtime evidence; static analysis alone
  cannot produce it.
- coverage is calculated only over applicable subjects with an explicit
  denominator; unknown subjects remain visible and are not silently removed.

The first implementation should avoid one aggregate quality score. Per-view and
per-capability maturity, coverage, confidence, unknown count, strengths, and
gaps are less misleading.

## Views

### Overview

Overview summarizes target identity, review profile, finding counts, assurance
counts, unknown counts, limitations, and capability maturity. It must not
convert these values into an unexplained score.

### CNCF View

The CNCF View groups observations by versioned CNCF feature identity. Initial
areas are component/service/operation, Command/Query/Event, entity runtime,
jobs, authorization, observability, configuration, timeout, retry, and
compensation.

### Implementation View

The Implementation View groups observations by module, package, file, class,
method, dependency, configuration, and executable specification. Locations
must be precise enough for IDE and SARIF navigation. Generated code and
hand-written code must remain distinguishable.

### Quality Attribute View

The initial quality set should be Security, Domain, Documentation, AI
Readiness, Resilience, Testability, and Observability. Performance, I18N,
Reliability, Compatibility, Maintainability, Availability, and Scalability can
follow after their capability and coverage contracts are specified.

Domain is a traceability-oriented view rather than a subjective score. It maps
CML concepts to implementation, tests, and documentation. Documentation and AI
Readiness likewise require structured coverage and consistency evidence, not a
document-volume metric.

## Rule Model

Every rule has a stable ID, version, kind, supported input/CNCF versions,
required evidence kinds, applicability predicate, and mappings.

- deterministic rules establish contracts that can be decided reliably;
- heuristic rules report uncertainty and never present estimates as facts;
- AI rules interpret semantic questions from structured evidence and record
  provider/model provenance.

Rules return observations, not renderer-specific messages. Capability
assessment is a separate aggregation step with a versioned policy. This keeps a
change in maturity calculation from rewriting the underlying evidence and
observations.

## CLI and Profiles

The user-facing entry point belongs to CBD Support:

```text
textus cbd review <project-root|car>
  [--view overview,cncf,implementation,quality]
  [--quality security,resilience,observability]
  [--profile development|ci|release]
  [--baseline <car|manifest|review-report>]
  [--runtime-evidence <file>]
  [--format text|json|html|sarif]
  [--fail-on warning|error|critical]
  [--ai-provider <configured-provider>]
  [--server <authorized-cbd-support-endpoint>]
```

Local and server-backed modes use the same CBD Review Application contract.
The local CLI may start an embedded application and Cozy analyzer; server mode
submits the run to an authorized CBD Support service. The command performs no
network access by default. Catalog, BoK, runtime, or AI evidence is used only
when explicitly supplied or enabled through an authorized configuration.

- `development` favors fast deterministic local feedback;
- `ci` uses the reproducible deterministic and selected heuristic rule set;
- `release` requires the declared baseline and mandatory evidence set and
  fails on unresolved release-gate unknowns;
- AI execution is opt-in and is never implied by a profile.

The JSON format contains the complete canonical report. SARIF contains
location-bearing findings and links back to report IDs; it cannot represent the
complete assurance and maturity model. HTML and text are view renderers.

## Web, Job, and MCP Surfaces

Review execution should be asynchronous because source analysis, test
evidence, runtime evidence, and AI providers may be long-running. CBD Support
creates a CNCF Job-backed `ReviewRun`; the Web UI displays progress and the CLI
may wait for completion or return the run identity.

The application surface should separate commands from queries:

- `startReview`, `cancelReview`, and report-retention administration are
  commands and are not MCP-ready by default;
- `getReviewRun` reports authorized progress and limitations;
- `getReviewReport`, `getReviewSummary`, and bounded finding/assurance queries
  may be MCP-ready only after authorization and redaction; and
- Web UI and CLI use the same application operations and canonical report
  projections rather than bypassing them.

Starting a review can read local files, execute analyzers, create stored
artifacts, cause external access, and incur AI cost. It must not become an MCP
query merely because the final report is read-only.

## sbt-cozy and CI/CD

The exact task names remain provisional. The intended task separation is:

```text
cozyReviewCar        produce or obtain the canonical Review Report
cozyReviewCarCheck   apply the CBD Support gate result to the sbt build
cozyReviewCarReport  materialize selected JSON, HTML, and SARIF projections
```

A representative CI sequence is:

```text
cozyGenerate
    -> compile / test
    -> cozyBuildCar
    -> cozyReviewCar
    -> cozyReviewCarCheck
    -> optional publish / distribute / deploy
```

The pipeline should preserve these artifacts:

```text
target/cbd-review/latest/report.json
target/cbd-review/latest/report.html
target/cbd-review/latest/report.sarif
target/cbd-review/latest/attestation.json
```

The attestation records the target digest, report digest, provider and rule-set
versions, profile, and gate result. An optional future publish gate may require
a successful attestation for the same target digest. Initial integration must
not silently redefine existing `publish`, `publishLocal`, distribution, or
deployment tasks.

Standard CI execution should be reproducible and offline: deterministic
providers are enabled, while external network access and AI providers are
disabled unless explicitly configured. Credentials are resolved only through
authorized references and are never written into the report, attestation,
SARIF, HTML, logs, or task output.

## Relationship to `car lint`

The first implementation should preserve `cozy car lint` as a focused,
deterministic compatibility command. Existing lint checks become one Cozy
`ReviewEvidenceBundle` provider through an adapter. A machine-facing command
such as `cozy car inspect --format evidence-json` may expose the shared bundle
without presenting itself as the complete Review product.

`cozy car lint` and the Cozy evidence provider must use the same underlying
rule results so Cozy and CBD Support cannot disagree about build, CML,
documentation, or ABI findings. CBD Support may add catalog, runtime, quality,
and AI observations, but must keep their provider identity distinct.

Whether `car lint` is eventually deprecated is a separate CLI lifecycle
decision. The Review design does not require that decision for its first
implementation.

## Security and Reproducibility

- Normalize and constrain local paths before traversal.
- Do not allow a server-side review request to name an arbitrary filesystem
  path; targets must resolve through configured development roots or admitted
  uploaded CAR artifacts.
- Distinguish working-tree, built CAR, published baseline, catalog, and runtime
  origins.
- Record digests and tool/rule versions for every reproducible evidence class.
- Exclude credential values and sensitive payloads from reports, logs, SARIF,
  HTML, CallTree, and AI inputs.
- Deny implicit network access and cross-origin evidence fetching.
- Treat incomplete analysis as `Unknown` or a report limitation, not success.
- Keep deterministic findings authoritative when AI output conflicts.
- Keep review-start commands private to MCP unless a separate authorization,
  filesystem, external-access, and cost policy explicitly admits them.

## Incremental Delivery

### Slice 1: Shared Review Provider protocol and Cozy adapter

- fix `ReviewProviderRequest`, `ReviewEvidenceBundle`, descriptor capability,
  version negotiation, and
  stable identities;
- adapt existing CAR lint results into Evidence and Observations;
- add analyzer identity, limitation, digest, and redaction contracts; and
- prove that existing lint findings are preserved exactly.

### Slice 2: CBD Review Application and canonical report

- add Review Run and CNCF Job lifecycle;
- establish Finding, Assurance, Unknown, capability, limitation, and location
  contracts;
- admit compatible Cozy bundles and reject incompatible ones explicitly;
- build and store canonical JSON; and
- provide the local user-facing CBD CLI.

### Slice 3: sbt-cozy CI/CD bridge

- emit sbt generation, build, test, dependency, and CAR-artifact evidence;
- invoke Cozy locally and submit both provider bundles to CBD Support;
- add report, check, and projection tasks without redefining publish tasks;
- write canonical report, HTML, SARIF, and attestation artifacts; and
- prove that local and server-backed execution use the same Review contract.

### Slice 4: Web UI, report queries, and projections

- add overview, run progress, CNCF, implementation, and quality pages;
- render text, JSON, and HTML from the same canonical report;
- add authorized report retrieval and bounded MCP-ready queries; and
- keep review execution and retention commands private to MCP.

### Slice 5: CNCF view

- introduce versioned CNCF feature definitions;
- map CML/model/runtime declarations to feature evidence;
- cover service/operation, Command/Query/Event, entity, job, authorization,
  observability, timeout, and retry; and
- add CNCF view projection without duplicating rule execution.

### Slice 6: Implementation view

- add Scala structure, call/effect, I/O, entity-access, dependency, and test
  evidence;
- add implementation locations and generated/hand-written provenance; and
- add SARIF projection.

### Slice 7: Quality capability assessment

- establish the first seven quality views and reusable capabilities;
- specify applicability, maturity, coverage, and provider aggregation; and
- add cross-view navigation by shared IDs.

### Slice 8: AI semantic review

- define bounded AI input schemas and provider policy;
- add CML/implementation, domain terminology, documentation, and test-adequacy
  rules; and
- record provider/model/prompt/input/output provenance and cost limitations.

### Slice 9: Runtime evidence

- define signed or attributable runtime evidence import;
- map CallTree, metrics, job, authorization, and failure records; and
- allow `Operational` maturity only from accepted runtime evidence.

## Open Questions

- Should the external CNCF projection remain named `CNCF View`, or should a
  product-facing alias such as `Framework View` be introduced?
- Which CNCF repository owns the versioned feature vocabulary and its release
  compatibility policy?
- What exact applicability and coverage formulas apply to each capability?
- Which unknowns block the `release` profile?
- What is the stable review-configuration and suppression-file format?
- Which runtime evidence is trustworthy enough for `Operational` maturity?
- Should quality-view definitions remain built-in and versioned initially, or
  be configurable through a later DSL?
- What retention, authorization, immutability, and comparison policy applies to
  Review Runs and stored Review Reports?
- What is the final command and package name for the CBD Support CLI?
- Which analyzer transport is used first, and how is the matching Cozy version
  discovered for a working tree or built CAR?
- What are the final sbt task and setting names?
- Which sbt task results are mandatory evidence for each Review profile?
- What attestation and opt-in policy connects a successful Review gate to
  publish, distribution, and deployment tasks?
- Which semantic rules justify AI cost in CI, and which remain interactive
  developer review only?

## Promotion Gates

Before this note is promoted to stable design and specification:

- validate the report model against representative existing CAR lint output;
- produce JSON examples for Finding, Assurance, Unknown, suppression, and
  limitation;
- define stable feature, capability, rule, evidence, and subject identities;
- decide the first profile and exit-code contracts;
- validate the generic Review Provider protocol, Cozy and sbt-cozy bundles, and
  the boundary among CNCF, SIE, catalog, runtime, and AI providers;
- prove that Web, CLI, and MCP report queries project the same stored report;
- prove that sbt local/CI and CBD server-backed execution produce equivalent
  canonical reports for identical evidence;
- define Review Run authorization, target admission, job, and retention
  contracts; and
- create executable specifications for report determinism, projection
  consistency, redaction, and unknown handling.
