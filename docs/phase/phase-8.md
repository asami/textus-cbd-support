# Phase 8: Review Delivery, CI/CD, and Quality Rule Execution

Stage Status:
- Current status: IN_PROGRESS
- Current step: Stage 8.1 delivery and diagnosis contract is complete; next,
  implement the authorized Web dashboard and item-diagnosis surface.
- Owner: Textus CBD Support development
- Update rule: Update this block and `phase-8-checklist.md` only after each
  item has reproducible evidence. Stop at the human-confirmation stage when it
  is ready; do not close the phase without explicit human confirmation.

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

### Stage 8.3: Markdown and PDF report artifacts

Implement deterministic Markdown and accessible PDF output from the common
report-document model. Prove content, redaction, order, identity, and omission
policy agree with canonical JSON and Web views.

### Stage 8.4: CI/CD artifact and gate integration

Extend authorized `sbt-cozy`/CI integration to materialize report artifacts,
attestation, exit behavior, and explicit profile gate handling without
redefining publish/deploy tasks.

### Stage 8.5: Diagnosis persistence, reuse, and CAR evolution

Implement database persistence and the exact reuse key. Prove duplicate
request coalescing, immutable historical snapshots, safe expiry, and comparison
only across compatible CAR lineage/configuration. Add dashboard history and
baseline/trend projections from persisted canonical data.

### Stage 8.6: Quality-attribute rule matrix

For every supported quality attribute, define concrete check IDs, Evidence
requirements, applicability, deterministic/advisory/runtime authority,
Unknown/limitation behavior, and representative executable specifications.
Implement in prioritized slices while the matrix keeps unimplemented checks
visible rather than implicitly passing them.

### Stage 8.7: Cross-surface verification and human confirmation

Run focused/full tests, CAR build/lint, report artifact verification, Web/CLI/
CI equivalence, and quality-rule coverage checks. Present the resulting
dashboard, diagnosis, Markdown, PDF, CI artifact, and residual Unknowns for
human confirmation. Stop there until explicit confirmation is recorded.

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
- Phase closure awaits explicit human confirmation after Stage 8.6.
