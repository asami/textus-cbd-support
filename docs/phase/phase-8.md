# Phase 8: Review Delivery, CI/CD, and Quality Rule Execution

Stage Status:
- Current status: DONE
- Current step: P8-RQH-A developer-authorized review-boundary scope
  reconstruction and P8-REL release validation are complete. The Phase full
  review was consumed exactly once; no replacement review ran. The accepted
  tree retains direct submission and exact Entity-backed reads without the
  rejected local production Job workaround. The Phase release commit closes
  this accepted boundary.
- Owner: Textus CBD Support development
- Update rule: Update this block and `phase-8-checklist.md` only after each
  item has reproducible evidence. Human confirmation and release validation
  are satisfied, and this release commit closes the Phase.

## Purpose

Turn the completed CAR Review foundation into a usable decision-support
product. A developer, reviewer, and CI/CD system must be able to understand a
Review Report through the same canonical Evidence, Observations, assessments,
limitations, and gate result—not through view-local rules or untraceable
summaries.

## Scope

### Web delivery

- A Review dashboard showing run health, gate, trend/baseline delta, quality
  attribute maturity/coverage/Unknowns, provider limitations, and the most
  important unresolved items.
- CAR evolution views derived from retained diagnostic history, showing
  comparable target versions/digests, report/gate changes, added/resolved/
  unchanged findings, capability changes, and explicit gaps where a run cannot
  be compared. History is never reconstructed from rendered report text.
- An item-diagnosis view for one Finding, Assurance, Unknown, or capability.
  It explains the applicable rule, Evidence, provider, affected locations,
  disposition, quality attributes, and bounded next actions.
- The Web surface projects canonical records only. It must not rerun an
  analyzer, hide an Unknown, manufacture remediation, or weaken authorization
  and redaction.

### Report delivery

- Deterministic Markdown and PDF report artifacts from one report-document
  projection model, including report/run/target identity, gate, dashboard
  summary, attribute views, item diagnoses, evidence links, limitations, and
  redaction/omission notes.
- Markdown remains a reviewable source artifact. PDF is a reproducible,
  accessible human-reading projection, not the report's interchange form.
- JSON remains canonical; HTML and SARIF remain existing specialized
  projections. Artifact generation must not change a Report digest or
  re-evaluate a rule.

### CI/CD

- `sbt-cozy` and other authorized CI clients materialize canonical JSON,
  Markdown, PDF, HTML, SARIF, and attestation artifacts for one Review Run.
- CI consumes an explicit profile and gate policy, reports `pass`/`fail`/
  `unknown` with attributable causes, and preserves artifacts for the target
  digest. Publish, distribution, and deployment remain opt-in integrations;
  this phase does not silently redefine existing tasks.
- Deterministic/offline CI remains the baseline. AI, external network, and
  cost-bearing providers require explicit profile policy and yield limitations
  or Unknown when unavailable.

### Diagnosis persistence and reuse

- Persist admitted Review Runs, canonical Reports, attestations, reusable
  diagnosis identity, and CAR-lineage history in the CBD Support database.
- Define a diagnosis reuse key from reviewed CAR identity and digest, selected
  profile, rule-set and provider selection/version, accepted runtime evidence
  identity, and other policy inputs that can change a conclusion. Reuse only a
  completed compatible Report for an exact key; otherwise create a new Run.
- Concurrent requests for the same key join one bounded in-progress Run rather
  than launching duplicate provider work. Failed, cancelled, expired,
  incompatible, or policy-different Runs are never presented as reusable
  success.
- Retain immutable report snapshots and compare them by CAR lineage, version,
  target digest, and compatible Review configuration. Database retention and
  authorization must preserve historical attribution while preventing arbitrary
  report-history enumeration through MCP.

### Quality rule execution

- Turn each supported quality attribute into concrete deterministic, runtime,
  provider, or advisory rule definitions with applicability, required Evidence,
  outcome, limitation, and maturity semantics.
- Cover the current catalog attributes, including Security, Domain,
  Documentation, AI Readiness, Resilience, Testability, Evaluability,
  Observability, UX, Cost, and later catalog-defined attributes. A catalog
  definition alone is not a check and cannot establish an Assurance.
- Add explicit AI View checks for Textus MCP and standard Skill support as well
  as the appropriateness of component-provided MCP operations and Skills.
- Make every check traceable from dashboard, diagnosis, Markdown, PDF, CI, and
  authorized read projections to the same canonical IDs.

## Non-goals

- Replacing CBD Support as the canonical Review owner or moving policy into
  Cozy, `sbt-cozy`, a renderer, or a Codex Skill.
- Treating absence of a quality Finding as an Assurance.
- Requiring every provider, AI mode, runtime metric, or quality attribute to
  be enabled in every development or CI profile.
- Allowing Web, MCP, a report renderer, or a Skill to start privileged Review,
  filesystem, external-provider, or AI-cost-bearing work without existing
  explicit authorization.
- Closing the separate Phase 4 P4-45 human confirmation or the first-release
  ABI-baseline debt.

## P8-RQH scope decision

The developer rejected the CBD-local Review Job workaround layer. Phase 8
retains established direct provider submission and CI compatibility, Entity
persistence and exact Report-bound reads, accepted redaction hardening, and
total quality coverage projections. Restart-safe production Review Job
integration is nonblocking Deferred Work for
cloud-native-component-framework Phase 69; it is not implemented, entered,
or validated by this phase. The local boundary must not replace that work with
bounded Job scans, synchronous timeout adapters, RunSnapshot reservations or
outboxes, current-process result dependence, synthesized terminal
leases/lifecycle, or private digest protocols.

## Stages

### Stage 8.1: Delivery and diagnosis contract

Define dashboard cards, item-diagnosis identities, baseline/trend semantics,
redaction, pagination, authorization, and the common report-document model.

Completed on 2026-07-23. `CarReviewDeliveryProjection` now projects the
immutable canonical Report into one deterministic dashboard/document/diagnosis
model without provider invocation, repository history access, or conclusion
derivation. `docs/spec/car-review-delivery-contract.md` defines exact-report
authorization, redaction, pagination, Markdown/PDF structure, and accessible
PDF requirements; `CarReviewDeliveryProjectionSpec` proves identity retention,
baseline handling, redaction, and exact missing-item behavior.

### Stage 8.2: Web dashboard and item diagnosis

Implement authorized dashboard and drill-down views with executable Web and
projection specifications. Prove all displayed conclusions retain canonical
Evidence/Observation/capability/provider IDs.

Completed on 2026-07-23. `CarReviewWebDeliveryApplication` serves one exact,
authorized retained Report through private `CbdReviewAdmin` dashboard and
diagnosis queries. Its static forms remain authenticated and non-MCP-ready;
the diagnosis form admits only `observation` or `capability`. The Web boundary
uses the common delivery document only: it does not invoke providers, enumerate
history, or alter conclusions. `CarReviewWebDeliveryApplicationSpec` proves
Finding, Assurance, Unknown, and capability diagnosis; canonical gate,
baseline, limitations, and disposition retention; authorization/missing-item
failure; and descriptor-supported bounded choices. `scripts/check-car-abi.sh`
proves the generated CML, source ABI manifest, and packaged CAR expose the
same 19-operation surface.

### Stage 8.3: Markdown and PDF report artifacts

Implement deterministic Markdown and accessible PDF output from the common
report-document model. Prove content, redaction, order, identity, and omission
policy agree with canonical JSON and Web views.

Completed on 2026-07-23. `CarReviewDeliveryArtifactRenderer` renders only the
already-projected `CarReviewDeliveryDocument`; it has no repository, provider,
clock, filesystem, browser, host-font, or network dependency. It produces
fixed-order Markdown and deterministic self-contained tagged PDF bytes.
`CarReviewDeliveryArtifactRendererSpec` proves stable bytes, common identities
and order, Markdown table output, PDF title/language/searchable text/heading
and table structure, explicit unsupported-character omission, and no conclusion
change. The PDF was inspected with `pdfinfo`, pypdf text extraction, and
Poppler rasterization. The implementation follows this completed plan:

- add one in-memory `CarReviewDeliveryArtifactRenderer` over
  `CarReviewDeliveryDocument`; it accepts no filesystem path, clock, provider,
  or report repository and returns Markdown, PDF bytes, and explicit renderer
  limitations only;
- render Markdown in the common contract order: Report identity, gate, counts,
  baseline, capabilities, Observations, limitations, and redaction/omission.
  Use fixed headings and tables/lists, canonical IDs, and no generated advice
  or timestamp;
- render a self-contained, deterministic tagged PDF from the same intermediate
  sections. It has fixed object ordering, no creation-time metadata, document
  title/language, searchable text, heading/table structure, text labels, and
  a non-colour-only gate/severity presentation. It must not rely on an ambient
  browser, host font, external tool, or network service;
- represent an unsupported printable character, page/layout limit, or tagging
  limitation as an explicit PDF omission marker and renderer limitation. It is
  never silently removed or changed into a conclusion;
- add `CarReviewDeliveryArtifactRendererSpec` for byte-identical repeated
  Markdown/PDF output, exact Web/JSON identity/order/redaction equivalence,
  readable PDF text/structure metadata, and explicit renderer limitations.
  The PDF skill's Poppler render and text/metadata inspection will be used for
  final visual verification.

### Stage 8.4: CI/CD artifact and gate integration

Extend authorized `sbt-cozy`/CI integration to materialize report artifacts,
attestation, exit behavior, and explicit profile gate handling without
redefining publish/deploy tasks.

P8-30 completed on 2026-07-23. The CBD-owned CI artifact contract fixes one
attestation-digest attempt directory, the canonical JSON/Report/attestation/
Markdown/PDF/HTML/SARIF file names, per-artifact byte digests, exact report and
profile binding, offline CI posture, pass/fail/unknown exit semantics, and
CI-workspace retention without implicit publication, distribution, or
deployment. Its JSON Schema constrains every artifact member to its fixed name,
and `CarReviewCiArtifactContractSpec` proves the representative unknown-gate
attempt, retention, and schema constraints. P8-31 is the separate
`sbt-cozy` materialization work; P8-32 and P8-33 cover gate behavior and
non-interference.

P8-31 completed on 2026-07-23. The private CBD submission boundary returns
one exact canonical response plus a CBD-rendered Markdown/PDF artifact bundle;
it validates the Report and outer-gate binding before rendering and does no
provider work. `sbt-cozy` admits that bundle with the canonical Report and
attestation, then atomically writes the seven contract artifacts and
`review-artifacts.json` into the attestation-digest attempt directory. Its
existing HTML/SARIF projections remain authorized local projections of the
same admitted response. `CarReviewArtifactBundleSpec`,
`SbtReviewReportArtifactsSpec`, and the loopback standalone SAR probe prove
the bound response, artifact directory, and expected failing-gate behavior.
Post-implementation review corrections enforce per-artifact bounds before
temporary materialization, reject oversized PDF Base64 before decoding, retain
bounded renderer/provider limitations in the manifest, and reject multiline
credential-shaped output across every retained artifact. The final clean
re-review, 103 sbt-cozy tests, and 246 CBD Support tests passed.

P8-33 completed on 2026-07-23. `SbtReviewPublicationBoundarySpec` keeps the
Review task surface explicitly limited to publish and distribution, and rejects
deployment. The `project-yaml-car` scripted fixture sets `cozyReviewGate` to
fail, then proves that ordinary release `publish`, ordinary `cozyDistribute`,
and SNAPSHOT `publishLocal` still execute their normal CAR task paths. The
independent re-review found no new issues; the focused specification, scripted
fixture, and 108 sbt-cozy tests passed.

### Stage 8.5: Diagnosis persistence, reuse, and CAR evolution

Implement database persistence and the exact reuse key. Prove duplicate
request coalescing, immutable historical snapshots, safe expiry, and comparison
only across compatible CAR lineage/configuration. Add dashboard history and
baseline/trend projections from persisted canonical data.

P8-40 completed on 2026-07-23. The CBD-owned persistence contract and its
machine-readable model define database-mappable CAR lineage, target snapshots,
terminal Run, canonical Report, attestation, opaque reuse identity, comparison,
and append-only retention-event records. They preserve immutable payloads and
safe tombstones without choosing a database product or reuse algorithm.
`CarReviewPersistenceContractSpec` verifies unique entity/table/key mappings,
valid relations, complete retention-event attribution, and the P8-41 boundary.
The independent re-review was clean; 249 CBD Support tests passed.

P8-41 completed on 2026-07-23. `CarReviewReuseKey` calculates the fixed v1
SHA-256 identity from a sorted canonical input document before provider work.
The identity binds target/digest, profile, baseline, rule sets, provider and
availability policy selections, accepted Evidence snapshots including runtime,
and required profile/gate/reconciliation/suppression policy bindings. It
rejects unsupported Review schemas and incomplete/ambiguous identity rather
than inventing a reusable result. `CarReviewReuseKeySpec` proves canonical
order independence, every conclusion-affecting invalidation class, and safe
schema/Evidence admission. Both independent reviews were clean; the focused
and executable-coverage specs and 253 CBD Support tests passed.

P8-42 completed on 2026-07-24. `ReviewDiagnosis` uses the protected CNCF
Entity boundary to load or atomically claim one deterministic exact-key root.
The owner persists
immutable Target, completed Run, canonical Report, and attestation composition
snapshots, then marks the root completed; a later exact-key request receives
the retained Report identity as `Reused`, while an in-progress request is
`Joined`. `ReviewDiagnosisPersistenceSpec` proves Owner/Joined coalescing and
completion/reuse through fresh UnitOfWork reads. Completion and terminal
transitions use the generated `ReviewDiagnosis` update model with the
server-derived stable root ID and `ServiceInternal` UnitOfWork authorization.
The generated scalar Review datatype decoder is shared infrastructure; CBD
Support no longer owns a private persistence codec or raw datastore path.

P8-43 completed on 2026-07-26. `ReviewDiagnosis` retains failed, cancelled,
expired, and incompatible outcomes as immutable Run composition snapshots.
One CNCF Entity conditional transition compares the persisted terminal root,
installs exactly one successor, and preserves the predecessor snapshot. It
uses no process-local lock, SQL, or raw datastore operation.
`ReviewDiagnosisPersistenceSpec` proves every terminal state rejects
successful reuse, admits a successor, and resolves simultaneous successor
requests to one Owner plus Joined callers.

P8-44 is complete. `CarReviewEvolutionProjection` is a pure View that
compares two Entity-adapter-supplied immutable canonical Reports only when
lineage, configuration compatibility, and CAR identity agree. It preserves
both version/digest/gate identities and computes Observation/capability deltas
without a repository lookup or provider run.

P8-45 completed on 2026-07-26. Persisted report reads require an authorized
exact Report ID; missing and unauthorized requests fail. Retention expiry
requires the operator role and policy age, appends an attributable tombstone,
removes the payload, and admits a fresh successor rather than successful
reuse. The MCP surface has only exact-report and bounded exact-report
projection operations—no lineage, target, or history enumeration selector.
`ReviewDiagnosisPersistenceSpec`, `CarReviewMcpReadProjectionSpec`, and
`ComponentFactorySpec` prove those persistence, authorization, and exposure
boundaries.

### Stage 8.6: Quality-attribute rule matrix

For every supported quality attribute, define concrete check IDs, Evidence
requirements, applicability, deterministic/advisory/runtime authority,
Unknown/limitation behavior, and representative executable specifications.
Implement in prioritized slices while the matrix keeps unimplemented checks
visible rather than implicitly passing them.

P8-50 completed on 2026-07-24. `CarReviewQualityRuleMatrix` derives one
stable check row for every supported capability: a check ID, applicability,
required Evidence kinds, deterministic/runtime authority, mandatory
Unknown/limitation result when Evidence is absent, and evidence-backed
maturity ceiling. MCP and Skill have independent AI-operability rows. This
does not claim a provider has run: P8-51 through P8-54 must implement and
admit the concrete checks through the provider bundle boundary.

P8-52 completed on 2026-07-24. `TextusAiSurfaceCarReviewProviderRunner`
turns bounded Textus-compatibility, MCP-projection policy, standard Skill-set,
and optional component-published surface metadata into ordinary admitted
provider Evidence and canonical AI View mappings. Compatible Textus MCP and
standard Skill support produce independent attributable Assurances; that
support does not imply useful MCP/Skill content. Structurally complete content
remains an explicit `unknown` pending advisory or human semantic review, while
an actually supplied but incomplete publication produces a deterministic
Finding. The provider retains no endpoint, credential, raw content, or
invocation payload. Provider-bundle admission now validates that every named
quality mapping exists in the catalog and was declared by the emitting
provider; reconciliation preserves it for dashboard and item diagnosis.

P8-53 completed on 2026-07-24. `CarReviewCostScenarioProviderRunner` covers
the first two CNCF cost scenarios: Static Web App delivery and Gemma + MCP
routing. Each optimization retains current architecture, cost driver, change,
expected and measured reductions as distinct values, comparison conditions,
quality constraints, operational trade-offs, and confidence in attributed
Evidence. Expected-only optimization remains an explicit Unknown; a measured
result becomes an Assurance only with both normalized unit and comparison
period. An unqualified measured-saving claim is a deterministic Finding.
`CarReviewCostViewProjection` derives a read-only Cost View from those canonical
records and does not fetch billing data, invoke AI, rerun providers, calculate
currency, or convert an estimate into a measurement.

P8-51 completed on 2026-07-24. `CarReviewInitialStaticQualityProviderRunner`
implements the initial fixed deterministic checks for Security, Domain,
Documentation, Resilience, Testability, Evaluability, Observability, and UX.
It accepts only a bounded static-analyzer result with one source digest and
maps a predefined rule set to catalog capabilities; callers cannot name an
arbitrary capability or invent a rule. A supplied pass becomes an attributable
Assurance, a supplied failure a deterministic medium Finding, and a missing
fact an explicit retryable Unknown/limitation. This establishes static
structure and contract evidence only: P8-54 and the runtime-evidence policy
continue to control operational claims.

P8-54 completed on 2026-07-24. `CarReviewQualityProviderAdmission` adds the
quality-provider authority boundary to strict v1 bundle admission and the
provider execution coordinator. Every policy supplies a finite declared and
maximum cost. Deterministic authority rejects advisory and runtime Evidence;
Runtime authority permits Assurance only when every referenced Evidence item is
descriptor-declared `runtime-observation`; Advisory authority permits only
`ai.advisory.*` Finding or Unknown results. The boundary rejects forbidden raw
or secret-bearing fact keys and an Evidence kind omitted from the descriptor.
Invalid authority, budget, redaction, or missing runtime evidence becomes an
attributable incompatible/Unknown-shaped provider refusal, never a fallback or
silent pass.

P8-55 completed on 2026-07-24. `CarReviewQualityCoverageProjection` is a
total, read-only projection of the catalog's base quality rules. It preserves
canonical Evidence/Observation identity where an admitted provider has checked
a capability. For every other capability it shows a retryable Unknown with the
matrix-defined missing-Evidence limitation. Thus an unimplemented, disabled, or
absent provider is visible in Web/CLI/report consumers through one projection
rather than disappearing from the quality view or being inferred as a pass.
The completion checkpoint observed 155 supported capabilities. After the
admitted catalog expansion, the current observed total is 161;
`CarReviewQualityCoverageProjectionSpec` remains dynamically total over
`CarReviewCapabilityCatalog.definitions.size`, so the observed count is not a
second fixed coverage contract. This records current catalog truth without
claiming new test execution.

### Stage 8.7: Cross-surface verification and human confirmation

Stage Status:
- Current status: DONE
- Current step: P8-61 completed and accepted on 2026-08-15 after clean focused
  re-review. P8-60 human confirmation completed on 2026-08-15 with the exact
  response `Phase 8 human confirmation complete.` for approved packet SHA-256
  `9ddfcb0c9ce5305d83a898a7bee95849a52957c0a01fe8bf600ef8455fa8e717`.
  Final Phase release validation passed on 2026-08-16.
- Owner: Textus CBD Support development and human reviewer
- Checklist basis: `P8-60`
- Update rule: Only attributable human confirmation can complete P8-60. The
  recorded confirmation did not replace final Phase release validation, the
  consumed one-time review, or the release commit.

Run focused/full tests, CAR build/lint, report artifact verification, Web/CLI/
CI equivalence, and quality-rule coverage checks. Present the resulting
dashboard, diagnosis, Markdown, PDF, CI artifact, and residual Unknowns for
human confirmation. The project owner deferred that confirmation on
2026-07-26 and authorized subsequent work to proceed independently. That
deferral remains historical scheduling and non-acceptance evidence for its
original date; it did not accept the Phase and was superseded by the
attributable P8-60 response recorded on 2026-08-15. The review packet,
acceptance criteria, and confirmation record are in
`phase-8-human-confirmation.md`.

P8-61 was completed and accepted on 2026-08-15 after clean focused re-review,
using the current declared Cozy `0.3.4-SNAPSHOT` and CNCF `0.5.2-SNAPSHOT`
toolchain. The shared
`conf/cozy/launcher.yaml` sets only `development.runtime.enabled: false`;
`cozy runtime config show` exited 0 with `runtime.devDir: (not configured)`,
`development.enabled: true`, `development.launcher.enabled: true`, and
`development.runtime.enabled: false`; `cozy --runtime 0.3.4-SNAPSHOT version`
exited 0 with `cozy 0.3.4-SNAPSHOT`. Forced-generation invocation
`7556-20260814T213837Z` recorded `sbt_exit=0`, `wrapper_exit=0`,
`lock=released`, accepted `goldenport-cncf_3:0.5.2-SNAPSHOT |
cozy_2.12:0.3.4-SNAPSHOT`, and generated 175 Scala sources. The provenance
was `cozy.generation-provenance.v1`, `cozyVersion=0.3.4-SNAPSHOT`,
`cncfVersion=0.5.2-SNAPSHOT`, with runtime descriptor SHA-256
`9568025493a6273e99cba5d14df9d9ba9e0b26b33278f9dca4ef127d5b907dfd`; the
descriptor identified CNCF `0.5.2-SNAPSHOT`, module
`org.goldenport:goldenport-cncf_3:0.5.2-SNAPSHOT`. Initial exact-suite
invocation `8248-20260814T214002Z` discovered 8 tests; all 8 failed at the
common obsolete `CbdSupport` fixture. Repair `P8-61-VF-001` now derives the
fixture name, componentId, and instanceId from generated
`CbdSupportComponent` identity. Final repair-validation
invocation `9751-20260814T214332Z` recorded `sbt_exit=0`, `wrapper_exit=0`,
`lock=released`, compiled 1 test source, and passed
`testOnly org.simplemodeling.textus.cbdsupport.impl.ReviewDiagnosisPersistenceSpec`
as 1 suite/8 tests; its fully-qualified `testOnly` supplies `Test / compile`,
with no duplicate standalone compile required. Post-repair `git diff --check`
passed. Full review accepted the runtime/config/generated identity behavior;
`P8-61-VF-001` was repaired, and focused re-review was CLEAN. Findings
`P8-61-VF-001` and `P8-61A-RV-001` are CLOSED. The historical Cozy
`0.3.0-SNAPSHOT` / CNCF `0.5.1-SNAPSHOT` launcher-selection failure remains
context only. P8-60 is complete and no checklist item remains unresolved.
The rollback accumulator passed 17 suites/80 tests, followed by the normal
Phase suite at serialized invocation `10514-20260815T115341Z` with 71
suites/289 tests. Exact project-declared Cozy CAR lint passed; the external
Python wrapper mismatch remains nonblocking `HYG-P8-002`. For final
representative evidence, dependency-order local publication passed for
Scraper (`48714-20260815T194744Z`), SIE (`49308-20260815T194858Z`), BoK
(`49807-20260815T195010Z`), and CBD Support (`50265-20260815T195113Z`), with
every shared SBT lock released. `scripts/test/check-cbd-sie-sar.sh` then passed
the complete canonical component catalog, source-ownership, BoK/CBD design
flow, four-profile policy matrix, and
`RUNTIME_COMPATIBILITY_EXECUTION_OK`. The one-time Phase full review remains
consumed and was not rerun. The Phase release commit closes this accepted tree.

## Acceptance

- One retained canonical Report renders consistent dashboard, diagnosis,
  Markdown, PDF, JSON, HTML, SARIF, and CI artifacts without additional rule
  execution.
- A reviewer can navigate every displayed quality conclusion and diagnosed item
  to attributed Evidence, provider, rule, location, limitation, and gate
  impact.
- CI has deterministic artifact naming, target/report/attestation digest
  binding, explicit exit behavior, and no implicit publication/deployment.
- Equivalent diagnosis requests reuse one compatible persisted Report; changed
  CAR or Review-policy evidence creates an attributable new Run. A reviewer can
  visualize comparable CAR evolution from retained immutable history.
- Every supported quality attribute has an explicit rule matrix entry and at
  least one executable evidence path, or is visibly `Unknown` with its missing
  provider/check identified.
- No checklist item remains unresolved: P8-60 human confirmation, P8-61
  acceptance, P8-RQH-A, and P8-REL validation are complete. The Phase release
  commit closes this accepted boundary; the consumed full review must not be
  repeated.
