# Phase 5: CBD-Led CAR Review Platform

Stage Status:
- Current status: IN_PROGRESS
- Current step: P5-31 — submit local Cozy and sbt-cozy evidence through the CBD Review Application.
- Owner: Textus CBD development
- Update rule: Update after a Phase 5 checklist item obtains reproducible evidence; closure is based only on `phase-5-checklist.md`.

## Purpose

Develop CAR Review as a capability led and owned by `textus-cbd-support`.
CBD Support owns provider orchestration, Review Runs, canonical reports,
quality assessment, Web UI, user-facing CLI, CI gate policy, and authorized
read-only MCP report queries. Cozy, `sbt-cozy`, CNCF, SIE, catalog, runtime,
and AI integrations participate through versioned Review Provider contracts.

## Source of Truth

This phase operationalizes the exploratory proposal and chronological decisions
recorded in:

- `docs/notes/car-review-design-proposal.md`
- `docs/journal/2026/07/car-review-spec-study-handoff.md`
- `docs/journal/2026/07/car-review-design-consideration-2026-07-15.md`
- `docs/journal/2026/07/car-review-product-boundary-2026-07-16.md`
- `docs/journal/2026/07/car-review-provider-sbt-cozy-integration-2026-07-16.md`
- `docs/journal/2026/07/car-review-ai-integration-modes-2026-07-16.md`

The note remains non-normative. Stable decisions must be promoted to
`docs/design` and behavior must be promoted to `docs/spec` with executable
specifications before implementation is declared complete.

The first promoted stable decision is
`docs/design/car-review-architecture.md`, which completes P5-01 ownership,
authority, dependency-direction, and execution-topology scope.

## Repository Boundary

Primary repository:

- `textus-cbd-support`: Review Application, provider orchestration, canonical
  report, Review Run lifecycle, quality assessment, Web UI, CLI, report
  projections, MCP report queries, and documentation.

Supporting repositories:

- `cozy`: CAR/CML/build/package/ABI/documentation analyzer and preservation of
  existing CAR lint behavior through the generic provider contract;
- `sbt-cozy`: sbt build/test/package evidence, local and CI Review client,
  report/check/projection tasks, and Review attestation artifacts;
- `textus-ai`: provider-neutral `AiRunner` execution, structured
  `generateRecord`, purpose-profile selection, Gemma/Ollama, OpenAI, and
  Google Gemini adapters, and the Phase 1 execution-fact, confidentiality,
  deterministic-provider, and lifecycle contracts needed by bounded AI
  Review; and
- CNCF, SIE, or other provider repositories only when an owned provider or
  runtime-evidence contract cannot be implemented safely in CBD Support.

CBD Support leads cross-repository design and acceptance. Supporting
repositories do not own competing Review Reports or gate policies.

## In Scope

- Generic, versioned Review Provider request, descriptor, evidence-bundle,
  capability, limitation, and compatibility contracts.
- Canonical Review Report with attributable Evidence, Finding, Assurance,
  Unknown, location, confidence, severity, disposition, capability assessment,
  provider identity, limitation, and baseline records.
- Review Run command/query model, CNCF Job execution, target admission,
  authorization, progress, cancellation, persistence, retention, comparison,
  and deterministic report identity.
- Cozy provider integration without a Cozy-to-CBD dependency or duplicate
  provider execution.
- `sbt-cozy` generation, compile, test, dependency, CAR-build, and task-result
  evidence plus local/CI report, gate, projection, and attestation integration.
- CBD Support CLI for local and authorized server-backed review.
- CBD Support Web UI for Review Runs, overview, CNCF, implementation, and
  quality views with cross-view evidence navigation.
- Canonical JSON plus consistent text, HTML, and findings-oriented SARIF
  projections.
- Explicitly authorized and redacted MCP queries for completed Review Runs and
  reports; execution and retention commands remain private by default.
- Initial quality-capability assessment for Security, Domain, Documentation,
  AI Readiness, Resilience, Testability, and Observability.
- Bounded optional AI semantic review that reuses Textus AI's provider-neutral
  `AiRunner`, `generateRecord`, purpose profiles, and local/commercial runtime
  adapters rather than adding provider wire APIs to CBD Support.
- Admission of the Textus AI Phase 1 contracts under development for
  normalized execution facts, deterministic CAR Review fixtures, restrictive
  CallTree/metadata publication, and explicit provider/lifecycle outcomes.
- Attributable runtime-evidence import.
- Development, CI, and release profiles with deterministic exit/gate policy.
- Executable specifications, cross-repository validation, user/developer
  documentation, and phase closure evidence.

## Out of Scope

- Publishing, distributing, deploying, or releasing CBD Support, Cozy,
  `sbt-cozy`, or any CAR artifact.
- Making Cozy or `sbt-cozy` the owner of the canonical Review Report, quality
  assessment, Web UI, or gate policy.
- Replacing or deprecating `cozy car lint` in Phase 5.
- Silently redefining existing sbt `publish`, `publishLocal`, distribution, or
  deployment tasks to run or require Review.
- Enabling network, catalog, BoK, runtime, or AI providers in standard CI
  without explicit configuration and authorization.
- Allowing a CBD Support server to inspect an arbitrary client filesystem path.
- Publishing `startReview`, `cancelReview`, or retention administration through
  MCP by default.
- Treating absence of a Finding as an Assurance or hiding an incompatible,
  unavailable, or disabled provider as success.
- Claiming `Operational` maturity from static analysis alone.

## Security and Reproducibility Constraints

- A local project target is analyzed locally by an admitted client/provider;
  a server target must be an authorized configured development root or admitted
  CAR artifact.
- Provider input, output, time, count, size, traversal, process, and network
  behavior is bounded and attributable.
- Credentials and sensitive source content do not enter reports,
  attestations, SARIF, HTML, logs, CallTree properties, MCP output, or AI
  inputs.
- Standard CI is offline and deterministic; external and AI providers are
  opt-in.
- CAR Review purpose profiles keep Textus AI `web_search` and `url_context`
  tools disabled unless a separate source, authorization, citation,
  confidentiality, and cost contract explicitly admits them.
- CBD Support owns bounded/redacted Evidence, prompt and output contracts,
  result admission, cache/cost policy, and Review conclusions. Textus AI owns
  provider resolution, execution, response normalization, and safe execution
  facts; it does not own the canonical Review Report or gate policy.
- Target, evidence, provider, rule-set, report, and attestation digests are
  sufficient to detect stale or mismatched gate evidence.
- Incompatible provider/schema versions produce explicit Unknown or failed-run
  evidence without fallback field translation.
- When `sbt-cozy` supplies an admitted Cozy bundle, CBD Support does not invoke
  Cozy again for the same Review Run.

## Stage 5.1: Normative Contract

Stage Status:
- Current status: DONE
- Owner: Textus CBD development
- Checklist basis: `P5-01` through `P5-04`
- Update rule: Update when ownership, provider, report, run, or security contracts are promoted or their executable examples change.

Promote the accepted product boundary and generic provider model from notes to
stable design and specification. Fix schema identities, compatibility,
security, redaction, profile, and lifecycle behavior before broad code changes.

Phase 5 started on 2026-07-16 by explicit human direction. Phase 4 P4-45 remains
an independent, incomplete human-confirmation gate; Phase 5 progress neither
satisfies that gate nor authorizes Phase 4 publication.

P5-01 completed on 2026-07-16 by promoting the CBD-owned product boundary,
provider authority matrix, canonical processing topology, call/dependency
directions, Textus AI execution boundary, and user/automation surface ownership
to `docs/design/car-review-architecture.md`. Exact schemas remain P5-02 work.

P5-02 completed on 2026-07-16 with
`docs/spec/car-review-provider-contract.md`, the strict
`textus.cbd.review-provider.v1` JSON Schema, descriptor/request/evidence-bundle
examples, and `CarReviewProviderContractSpec`. The executable specification
proves schema and document identities, capability admission, provider/rule-set,
Review and target agreement, unique local references, bounded request values,
and recomputable request and bundle digests. Canonical report and Review Run
semantics remain P5-03 work.

P5-03 completed on 2026-07-16 with
`docs/spec/car-review-report-contract.md`, the strict
`textus.cbd.review-report.v1` JSON Schema, coherent Review Run, canonical
Report, and attestation examples, and `CarReviewReportContractSpec`. The
executable specification proves controlled Observation and assessment terms,
local Evidence and Observation reference integrity, provider attribution,
Finding-only severity, disposition accountability, integer coverage,
cross-document identity, and recomputable report and attestation digests.
Target admission, authorization, containment, redaction, retention, MCP
exposure, and deterministic/offline execution remain P5-04 work.

P5-04 completed on 2026-07-16 with
`docs/spec/car-review-security-contract.md`, the strict
`textus.cbd.review-security.v1` policy Schema, development and standard CI
policy examples, and `CarReviewSecurityContractSpec`. The contract fixes
deny-by-default action and MCP authority, client/server target admission,
bounded filesystem/process/network work, outbound-only credential resolution,
shared projection redaction, structured AI input, finite immutable retention,
and pinned offline CI behavior. The same determinism audit refined P5-03 so
volatile Run/report IDs, execution timestamps, baseline report ID, and array
arrival order do not affect `reportDigest`; the attestation still binds one
concrete execution. Stage 5.1 is complete and Review Application model work
begins at P5-10.

## Stage 5.2: Review Application Core

Stage Status:
- Current status: COMPLETE
- Owner: Textus CBD development
- Checklist basis: `P5-10` through `P5-14`
- Update rule: Update when canonical report, Review Run, provider orchestration, persistence, or gate behavior gains executable evidence.

Implement the CBD-owned canonical model and Review Application. Review Runs
execute as CNCF Jobs, admit provider bundles through explicit compatibility,
and retain deterministic reports without merging provider identity.

P5-10 completed on 2026-07-16 with `CarReviewModel`,
`CarReviewReportCodec`, and `CarReviewReportCodecSpec`. The runtime model uses
distinct Scala value types for Review/report/Evidence/Observation/capability,
provider/rule, digest/version/profile/instant, and all controlled report
vocabularies rather than collapsing them into undifferentiated strings. The
codec rejects unknown wire fields and incompatible identities, checks local
references, provider attribution, Finding/Assurance/Unknown semantics,
disposition, coverage, baseline, gate, bounds, duplicates, and timestamp
ordering, and emits canonical JSON. Executable scenarios prove typed decode,
stable encode/decode, equivalent digest across volatile Run metadata and array
arrival order, six distinct rejection classes, and validated digest
recalculation. Review Run Job lifecycle begins at P5-11.

P5-11 completed on 2026-07-16 with `CarReviewRunModel`,
`CarReviewRunCodec`, `CarReviewRunLifecycle`, and
`CarReviewRunLifecycleSpec`. Review Run v1 retains distinct state, failure,
Review, report, digest, provider, target, profile, limitation, and timestamp
types; rejects unknown fields and invalid terminal shapes; and projects CNCF
`Submitted`, `Running`, `Suspended`, `Cancelled`, `Succeeded`, and `Failed`
states without losing cancellation intent or limitations. Terminal projection
is immutable and repeated identical CNCF readback is idempotent.
`CarReviewRunApplication` and `CncfCarReviewJobGateway` add role-specific
admission, one stable Review-to-Job binding, persistent asynchronous CNCF Job
submission, authorized Job read/control policies, and safe completion/failure
projection. CML publishes only `getReviewRun` through MCP-ready
`CbdRetrieval`; private `CbdReviewAdmin` owns `startReview` and `cancelReview`.
`CarReviewRunApplicationSpec` proves the application contract with a controlled
gateway and an actual held `InMemoryJobEngine`; `ComponentFactorySpec` proves
the generated service/MCP boundary. Provider execution and evidence-bundle
admission now begin at P5-12.

P5-12 admission begins with `CarReviewProviderBundleAdmission` and
`CarReviewProviderBundleAdmissionSpec`. This CBD-owned boundary admits only
the exact v1 descriptor/request/bundle exchange: document identity, strict root
shape, advertised capability and evidence-kind coverage, Review/target/digest
agreement, local IDs/references, normalized request/bundle digests, and finite
bundle item/byte limits are checked before any provider Observation can enter
reconciliation. `incompatible`, `unavailable`, and `disabled` outcomes are
retained as provider-attributed Unknown-shaped refusals; `failed` retains the
same attribution as a run failure. It has no implicit provider rerun or
fallback. `CarReviewProviderExecutionCoordinator` follows this boundary with
one injected provider runner: it applies the request timeout, propagates
cancellation, retains provider failures, and caches each admitted
provider/request digest so it never re-runs an already admitted bundle.
`CarReviewProviderExecutionCoordinatorSpec` proves the normal, cached,
timeout, cancellation, and provider-failure paths. P5-12 is complete.
Provider discovery and the production Cozy adapter remain P5-21 work, and
report reconciliation begins at P5-13.

P5-13 begins with `CarReviewBundleReconciler` and
`CarReviewBundleReconcilerSpec`. The reconciliation boundary accepts only a
bundle paired with its exact typed admission, creates canonical report-local
Evidence and Observation IDs without losing provider-local IDs or bundle
digest, and ignores a repeated admitted bundle rather than reconciling it
twice. It preserves each provider Observation and returns explicit conflicts;
it neither selects a winner nor creates an Assurance without admitted Evidence.
Multi-provider assessment, gate construction, and persisted reconciliation are
still P5-13/P5-14 work.

`CarReviewAssessmentGateBuilder` derives a deterministic capability assessment
and profile gate from reconciled records. It retains provider and
Evidence/Observation identities, calculates integer coverage, keeps Unknown
accounting explicit, and fails only from canonical Findings. Multi-capability
policy configuration and report assembly remain P5-13 work.

`CarReviewReportAssembler` joins reconciled records and the CBD-owned
assessment/gate result into one immutable canonical report before invoking the
existing deterministic report codec. Broader multi-capability policy remains
open in P5-13.

P5-13 is complete. P5-14 completes the Review Application Core with
`CarReviewRepository`. It atomically retains a completed Run and Report only
when their review, target, report ID, and digest attribution agree; it also
enforces finite per-target Run/Report limits, records content-free
expiry/deletion audit entries, and rejects stale or target-mismatched
gate/baseline evidence. The detailed decision and executable evidence are in
`docs/journal/2026/07/car-review-p5-14-retention-2026-07-16.md` and
`CarReviewRepositorySpec`.

## Stage 5.3: Provider Framework and Cozy

Stage Status:
- Current status: OPEN
- Owner: Textus CBD development with Cozy provider support
- Checklist basis: `P5-20` through `P5-24`
- Update rule: Update when generic provider behavior or Cozy analyzer evidence changes.

Establish provider registration, capability negotiation, bounded invocation,
and limitation reporting. Adapt Cozy's CAR/CML/build/package/ABI/documentation
analysis and existing lint results into the common evidence contract.

P5-20 completed on 2026-07-16 with `CarReviewProviderRegistry`: strict descriptor admission,
immutable provider registration, bounded deterministic capability discovery,
local runner lookup, CNCF `ProviderCall` execution/cancellation, and
CallTree-safe observability are now executable. Registry-to-coordinator
selection now rejects unregistered or descriptor-mismatched runners before
execution. `CarReviewProviderExecutionApplication` constructs the selected
runner through the CNCF adapter at the CBD-owned application entry point; its
specification proves the full exchange does not enter CallTree. The decision
record is
`docs/journal/2026/07/car-review-p5-20-provider-registry-2026-07-16.md`.

P5-21 completed on 2026-07-16 in Cozy with the CBD-neutral
`CozyCarReviewProvider`. It emits the v1 descriptor and an attributable bundle
from CAR project metadata, resolved CML/model source, build metadata, generated
CAR archives, and the integrated lint's ABI and documentation results. The
bundle binds CBD's Review/target/request identity without importing CBD Support,
keeps the Cozy provider/rule-set identity and declared CNCF versions explicit,
and reports the absence of runtime evidence as a limitation. Provider-request
limits validate positive evidence/observation/input-byte/time values, bound the
CAR/CML/documentation analysis inputs, and report invalid, input-byte,
evidence-count, or observation-count limits without permissive fallback. Its
capability, Evidence-kind, and include/exclude rule selection are also explicit:
an unrequested Cozy capability produces no static analysis and an attributable
limitation rather than another provider behavior. Its executable specification
is `cozy.review.CozyCarReviewProviderSpec`. The
decision record is
`docs/journal/2026/07/car-review-p5-21-cozy-evidence-provider-2026-07-16.md`.

P5-22 completed on 2026-07-16 with an executable exact-preservation check in
`cozy.review.CozyCarReviewProviderSpec`. It compares every provider-adapted
lint Evidence fact (category, code, level, project-relative path, line, and
message) against `CozyCarLint.lint`, and compares the independent JSON command
output and normal exit policy against the same result. P5-23 must now connect
the CBD-owned provider protocol to this Cozy boundary without adding a Cozy
dependency on CBD Support. The decision record is
`docs/journal/2026/07/car-review-p5-22-cozy-lint-preservation-2026-07-16.md`.

P5-23 completed on 2026-07-16 with a one-way local provider boundary. CBD
Support's `CozyCarReviewProviderRunner` first binds the configured local root
to the admitted `ReviewTarget`, then invokes only the fixed
`cozy review car-evidence` command template with the provider request on
stdin. Its process transport clears the child environment, uses the request
timeout, keeps the response in a bounded CBD-owned output root, and never
exposes the exchange document to logs or CallTree. Cozy independently parses
the neutral v1 request and emits the provider evidence bundle; it imports or
calls no CBD Support code. The executable specifications cover registry
selection, target refusal before execution, bounded command exchange, and the
Cozy command's request-digest binding. The decision record is
`docs/journal/2026/07/car-review-p5-23-cozy-provider-transport-2026-07-16.md`.

P5-24 completed on 2026-07-16 with
`CarReviewProviderBehaviorMatrixSpec`. The phase-level matrix exercises the
same canonical v1 Cozy descriptor, request, and bundle used by the detailed
contract specifications: compatible admission preserves the exact provider
identity and provider-owned limitation; a target digest mismatch is refused
without reattribution; and cancellation, timeout, and duplicate request-digest
execution remain attributable and bounded. The decision record is
`docs/journal/2026/07/car-review-p5-24-provider-behavior-matrix-2026-07-16.md`.

## Stage 5.4: sbt-cozy CI/CD Bridge

Stage Status:
- Current status: OPEN
- Owner: Textus CBD development with sbt-cozy support
- Checklist basis: `P5-30` through `P5-35`
- Update rule: Update when sbt evidence, Review tasks, CI artifacts, attestation, or optional release-gate behavior changes.

Connect sbt builds to Cozy and CBD Support without moving Review policy into
the plugin. Produce canonical report, HTML, SARIF, and attestation artifacts;
make gate failure reproducible and keep existing publication tasks unchanged by
default.

P5-30 completed on 2026-07-16 in `sbt-cozy`. `cozyReviewSbtEvidence` now
records generation, compilation, test, dependency-resolution, CAR-build, and
aggregate task-result outcomes in deterministic v1 provider descriptor,
request, and evidence-bundle documents under
`target/cbd-review/sbt-cozy`. Its source digest excludes generated output, and
the task uses a dynamic CAR branch so non-CAR builds record `not-applicable`
without evaluating CAR packaging. The descriptor advertises only attributable
build evidence and a possible `unknown` Observation vocabulary; each bundle
contains no Observations, no assessment, and the explicit
`sbt-evidence-no-quality-assessment` limitation. `SbtReviewEvidenceSpec` and
the `cozy/review-evidence` scripted fixture prove the document and task
contracts. Submission, canonical report admission, and gate application remain
P5-31/P5-32 work.

P5-31 CBD-side admission is now available through
`CarReviewPairedBundleReviewApplication`. It accepts a bounded vector of
path-free provider descriptor/request/bundle documents, checks each document's
Review and Target binding against the canonical report template, admits each
bundle through the generic v1 boundary, and returns the one CBD-owned Report
and Gate. It reconstructs completed provider executions from admitted bundle
identity and never silently drops a stale baseline or chooses between two
different bundles from the same provider identity. The local `sbt-cozy` client
and its Cozy command transport already produce the paired document submission;
the remaining P5-31 work is to bind that client transport to this public CBD
submission contract and prove the actual two-provider exchange.

`CarReviewProviderDocumentSubmissionApplication` now makes the public CBD
boundary explicit: the client supplies only one Review/Target-bound provider
document set, while CBD resolves the canonical report template through its own
policy provider before it admits evidence. An unauthorized caller is rejected
before that resolver is called. The JSON wire contract and concrete
`sbt-cozy` transport remain the next P5-31 slice.

P5-31 supports both private HTTP and local CLI submission adapters over the
same wire/application boundary. HTTP accepts only `application/json` and the
bounded request body after gateway role resolution; CLI accepts the same
bounded JSON through stdin with process-resolved roles. The generated private
`CbdReviewAdmin.post` operation exposes the HTTP form at
`POST /rest/v1/cbd-support/cbd-review-admin/post`: its outer generated request
is `{ "submissionDocument": "<provider-document-submission JSON>" }` and its
outer response is `{ "canonicalResponse": "<canonical-review-response JSON>" }`.
The component operation and the `CarReviewSubmissionCliAdapter` invoke the same
bounded admission and CBD-owned development template provider, returning only
the canonical response. Neither transport or operation gains workspace,
process, credential, Report-template, or Gate authority.

`sbt-cozy` now configures this endpoint with `review.cbd.endpoint` and
`cozyReviewSubmit`; it invokes Cozy locally, submits the paired Cozy and sbt
provider documents in the generated HTTP envelope, then unwraps only CBD's
canonical response. The endpoint client rejects credential-bearing, non-HTTP,
redirect, malformed, non-JSON, and oversized exchanges. Its executable HTTP
fixture proves the envelope round trip. For a loopback development server only,
`review.cbd.role` may carry one of `reviewer`, `operator`, or `admin` as the
fixed `role` header; arbitrary headers and URL credentials remain unsupported.
Production uses the configured CBD authentication boundary rather than this
development fallback. A standalone CBD CLI executable and a role-authenticated
live server exchange remain P5-40/P5-63 work; the shared CLI adapter is
intentionally not represented as a separate command yet.

P5-32 implementation has begun in `sbt-cozy`. `cozyReviewSubmit` now writes a
deterministic canonical JSON artifact plus `cozyReviewCanonicalJson`,
`cozyReviewReportHtml`, `cozyReviewReportSarif`, and `cozyReviewGate` task
surfaces. The HTML view safely projects the CBD-owned report; SARIF projects
only location-bearing Findings and records its lossy projection policy; the
gate task rejects every non-`pass` CBD result. The artifact renderer refuses an
outer response gate that disagrees with the report gate. Unit specifications
prove these projection and refusal contracts. P5-32 remains open until the
configured task route produces its selected artifacts against a real canonical
CBD response.

P5-33 now has a CBD-owned canonical attestation path. CBD attaches the
attestation to the submission response and binds its digest to the canonical
Report, target, profile, provider/rule-set/bundle identities, and Gate.
`sbt-cozy` validates those bindings before writing
`target/cbd-review/sbt-cozy/canonical-attestation.json`; it exposes the file as
`cozyReviewAttestation` and never manufactures a local substitute. Live CI
materialization remains required before checking P5-33.

P5-34 completed on 2026-07-16 in `sbt-cozy`. `SbtReviewCiPolicy` selects
standard CI from `CI=true` or an explicit `review.ci.profile: standard` and
admits only the deterministic local Cozy and sbt-cozy providers by default.
A configured CBD HTTP gateway must be loopback in that profile. External,
AI, and network gateway use are each separate, named boolean opt-ins, so
enabling a server route cannot silently enable a cost-bearing provider. Before
writing canonical JSON, HTML, SARIF, or attestation output,
`SbtReviewReportArtifacts` verifies the CBD attestation digest and refuses
sensitive JSON fields or credential-shaped values. `SbtReviewCiPolicySpec` and
`SbtReviewReportArtifactsSpec` make those refusals executable. The decision
record is `docs/journal/2026/07/car-review-p5-34-deterministic-ci-2026-07-16.md`.

P5-35 completed on 2026-07-16 in `sbt-cozy`. `cozyReviewPublish` and
`cozyReviewDistribute` are opt-in tasks: each first requires
`cozyReviewGate`, then delegates to the selected CAR/SAR release publication
or distribution task. Existing `publish`, `cozyPublishCar`, `cozyPublishSar`,
`cozyDistributeCar`, and `cozyDistributeSar` have no Review dependency and
retain their default behavior. `CozyPublishVersionPolicySpec` proves both
unchanged ordinary task selection and the complete explicit Review-gated
mapping. The decision record is
`docs/journal/2026/07/car-review-p5-35-opt-in-release-gate-2026-07-16.md`.

## Stage 5.5: Web, CLI, Report, and MCP Surfaces

Stage Status:
- Current status: OPEN
- Owner: Textus CBD development
- Checklist basis: `P5-40` through `P5-45`
- Update rule: Update when a user surface, report projection, authorization rule, or cross-view navigation contract changes.

Provide one Review Application through Web UI and CBD CLI. Render all views
from one canonical report and expose only bounded, authorized, redacted report
queries through MCP.

P5-40 implementation has begun with `CarReviewCliMain review submit`. It
accepts the exact `textus.cbd.review-submission.v1` inner document from stdin.
The local route calls `CarReviewSubmissionCliAdapter` and requires roles from
the process boundary; the server route sends the generated private HTTP
envelope with no role or credential forwarding and trusts server-side
authentication. Both routes parse the CBD-owned canonical response into a
stable `review-cli-result` containing Review ID, canonical response, gate, and
documented exit behavior. `CarReviewCliSpec` proves the local and server
adapter paths, canonical identity/gate retention, and malformed response
refusal. A real authorized HTTP command scenario remains required before
checking P5-40.

P5-41 implementation has begun with a protected static Web form for
`CbdRetrieval.getReviewRun`. It projects the typed Review Run record directly:
Review/Job identity, digest-bound target, profile, lifecycle state, timestamps,
provider-attributed limitations, and completed report ID/digest remain source
fields rather than Web-derived conclusions. `ComponentFactorySpec` verifies
that the form belongs to the retrieval service and remains inside the `form:`
descriptor section. A completed-report overview and live progress scenario are
still required before checking P5-41.

P5-42 implementation has begun with `CarReviewViewProjection`. The CNCF,
implementation, and quality view collections group only canonical mappings;
each item preserves the original Evidence/Observation IDs, provider/rule-set/
bundle attribution, and implementation locations. `CarReviewViewProjectionSpec`
proves a representative Report can navigate from every view back to the same
canonical identifiers. Exposing completed Report views through the authorized
Web/MCP read surface remains required before checking P5-42.

P5-43 completed on 2026-07-16 with `CarReviewReportProjection`. CBD renders
the same canonical Report into stable text, canonical JSON, safe HTML, and
SARIF. SARIF includes only Findings with a usable location and carries both
the omitted Finding count and `location-bearing-findings-only` projection
policy. `CarReviewReportProjectionSpec` proves repeat rendering remains
identical and does not project a location-free Unknown as SARIF.

P5-44 implementation has begun with `CarReviewMcpReadApplication`. Authorized
callers can request one retained Report ID's summary, redacted report, bounded
Findings, or bounded Assurances. The model omits Evidence facts/rationale,
sanitizes credential-shaped text, reduces paths to a basename, rejects an
unknown Report ID, and rejects limits outside 1–100. Its MCP CML service and
runtime storage integration remain required before checking P5-44.

P5-45 completed on 2026-07-16 with `CarReviewMcpExposurePolicy`. Its fixed
MCP allowlist is read-only Review projection names; start/cancel, retention,
filesystem, external-provider, and AI enablement actions are explicitly
private, and an unknown Review action is private by default. The generated CBD
component continues to expose only Retrieval operations through MCP.

## Stage 5.6: Quality, AI, and Runtime Assessment

Stage Status:
- Current status: OPEN
- Owner: Textus CBD development with optional providers
- Checklist basis: `P5-50` through `P5-55`
- Update rule: Update when capability assessment, AI provenance, or runtime-evidence maturity behavior changes.

Implement reusable quality capabilities, applicability, maturity, coverage,
confidence, provider attribution, and first quality views. AI consumes bounded
structured evidence through Textus AI's provider-neutral structured-generation
surface. Phase 5 admits the Textus AI Phase 1 execution-fact,
confidentiality, deterministic-provider, and failure/lifecycle contracts only
after their executable evidence is available; unavailable or incompatible
contracts remain explicit limitations or Unknown. Runtime evidence is required
for `Operational`.

P5-50 completed on 2026-07-16 with `CarReviewCapabilityCatalog`. Security,
Domain, Documentation, AI Readiness, Resilience, Testability, and Observability
definitions are CBD-owned reusable capabilities. Their projection consumes only
canonical assessment/observation/evidence mappings and preserves the original
IDs; `CarReviewCapabilityCatalogSpec` proves representative Domain,
Documentation, and runtime Observability mappings do not cause provider work.

## Stage 5.7: Verification and Closure

Stage Status:
- Current status: OPEN
- Owner: Textus CBD development
- Checklist basis: `P5-60` through `P5-66`
- Update rule: Update only from reproducible executable, build, lint, CI, UI, MCP, and review evidence.

Verify the complete CBD-led workflow across the primary and supporting
repositories. Promote stable behavior, document residual limitations, and
close Phase 5 without publishing or overstating optional provider coverage.

## Closure Basis

Phase 5 is DONE only when every item in `phase-5-checklist.md` is `[x]`, all
required focused and full validation passes, actionable review findings are
resolved, the primary and supporting repositories have validated commits, and
the verification evidence is recorded here. Any deferred item must have an
explicit relocation target and must not remain unchecked at closure.
