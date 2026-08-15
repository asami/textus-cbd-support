# Textus CBD Support Development Strategy

## Objective

Provide a dedicated CAR and development-support tooling whose MCP, Web, CLI,
and report surfaces help generative AI and developers perform Component-Based
Development without coupling component catalog semantics to SIE's BoK
knowledge model.

## Development Philosophy

- Keep SIE terminology/component-existence knowledge separate from CBD detail.
- Read authoritative Cozy-generated catalog and model-metadata documents.
- Return evidence and explicit absence; never synthesize catalog facts.
- Keep the default simplemodeling.org catalog usable without SIE.
- Allow configured catalogs without merging their identities silently.
- Preserve a last known good snapshot when refresh fails.
- Publish read-only CBD operations through MCP and keep administration private.
- Keep CBD Web, CLI, MCP, and report projections on one application contract.
- Lead CAR Review in CBD Support and admit Cozy, `sbt-cozy`, CNCF, SIE,
  catalog, runtime, and AI capabilities through attributable versioned
  providers.
- Use `sbt-cozy` as the local and CI/CD bridge without moving Review policy into
  the plugin or changing publication tasks implicitly.
- Keep project identity, Scala version, dependencies, and runtime compatibility
  authoritative in `project.yaml`; keep `build.sbt` declarative and small.

## Phase Overview

### Phase 1: CBD Support Extraction Baseline

Create the CAR, catalog provider, shared reference contract, MCP publication
policy, SIE reference-only boundary, documentation, and verification baseline.

### Phase 2: Catalog Fidelity and Resolution

Expand catalog schema coverage, version selection, dependency graph
resolution, conflict reporting, source authorization, caching, and refresh
observability using real published catalogs.

### Phase 3: Federated Development Context and AI Ergonomics

Add simplemodeling.org, configured BoK sites, SIE-mediated BoK knowledge,
configured development directories, and CAR versions in the local warehouse
and managed cache as distinct evidence-bearing input sources. Reconcile their
component and version observations without silently merging source identity,
then improve requirement matching, intent-aware usage guidance, evidence
citation, and Codex integration without turning inferred advice into catalog
fact.

### Phase 4: Runtime Hardening

Add authentication, bounded caching, production refresh policy, SAR composition
tests, compatibility governance, and release/publish evidence.

### Phase 5: CBD-Led CAR Review Platform

Develop CAR Review under CBD Support ownership. Add generic Review Providers,
canonical Review Reports and Review Runs, Cozy analysis, `sbt-cozy` CI/CD
integration, Web UI, user-facing CLI, authorized read-only MCP report queries,
quality-capability views, optional AI/runtime evidence, and reproducible
cross-repository verification without publishing automatically. AI Review
reuses Textus AI's provider-neutral `AiRunner`, structured `generateRecord`,
purpose profiles, runtime adapters, and its Phase 1 execution-fact,
confidentiality, deterministic-provider, and lifecycle contracts while CBD
Support retains Review policy, Evidence admission, canonical reporting, and
gate ownership.

### Phase 7: Action-Local Runtime Isolation

Ensure that a cached CBD runtime retains no ActionCall-local state and that
each action retains its own admitted local resource-tree inventory. Complete
the explicit configuration handoff in standalone and composed CBD/SIE SAR
harnesses, then prove isolation with deterministic interleaving and concurrent
ActionCall specifications.

### Phase 8: Review Delivery, CI/CD, and Quality Rule Execution

Operationalize the completed CAR Review foundation as a decision-support
product. Add a canonical-report-driven Web dashboard and item diagnosis,
Markdown and PDF report generation, CI/CD artifacts and gate integration, and
concrete attributable checks for each quality attribute. Persist diagnosis
results by CAR identity and content/configuration fingerprint so equivalent
work is reused rather than rerun, and visualize the CAR's evolution from its
retained diagnostic history. Keep all renderers, diagnoses, CI results, and
quality conclusions tied to the same canonical Evidence, Observation,
capability, and limitation identities; no surface may rerun a rule or invent a
conclusion.

## Current Priority

Phases 1 through 3 are complete. Phase 4 automated work is complete, while
Stage 4.6 Human Confirmation P4-45 is explicitly on hold and remains
unchecked. The human reviewer authorized Phase 5 to proceed independently
without treating that deferral as Phase 4 acceptance. Phase 5 and Phase 6 are
complete; the Phase 6 checkpoint removes the runtime-boundary debt from CBD
Support while the first released CAR ABI baseline remains separately deferred.
Phase 2 provides bounded,
same-catalog dependency graph resolution, version-specific profile projection,
and finite-lifetime catalog snapshots with observable refresh and stale-cache
state. Additional catalog sources now require explicit exact-origin
authorization and expose rejected configuration without network access. The
default public source's missing rich indexes are recorded as a publisher-owned
future candidate with deployment acceptance gates. Cozy schema fidelity is now
fixed to revision-pinned producer evidence, including runtime ranges, archive
checksums, nested ABI dependencies, sidecars, and diagnostics. Full tests, CAR
build, CML lint, CAR lint, and representative MCP projection close the phase
without changing the SIE/CBD ownership split. Phase 3 now has a unified
source/observation vocabulary plus bounded, read-only adapters for explicitly
configured development directories and CAR artifacts in the local warehouse
and managed cache. Authorized BoK sites now enter the live runtime only through
the Cozy `cncf.knowledge-source.v1` manifest and bounded machine-readable
glossary resources. SIE-mediated BoK input now enters through SIE's public
typed MCP component contract with exact-route authorization, bounded
responses, and mandatory evidence while keeping CBD component profiles
separate. Source-preserving reconciliation now reports duplicate, missing,
stale, incompatible, version, and checksum conflicts with purpose-specific
authority tiers and no automatic winner. Version-state reconciliation now
keeps working, locally published, cached, and remotely published availability
separate from release, snapshot, unknown, and conflicting maturity, including
all declared catalog identities and no implicit latest winner. Phase 3 has now
hardened remote-origin, derived-fetch, local-root, traversal,
symlink-escape, credential, and diagnostic-sanitization boundaries. Read-only
source-aware search now keeps catalog profiles separate from working and
local/cache observations, exposes bounded source, freshness, availability,
conflict, and purpose filters, reports participating evidence and precedence,
and does not select a hidden winner. The runtime also projects configured local
inputs through search, `listCatalogs`, and `status` without adding a publishing
administration mutation. Requirement matching now cites BoK semantic evidence
separately from CBD catalog and local evidence. BoK sites use deterministic
published-field matching, SIE retains its own match metadata for the current
query, and catalog components reference citations only through explicitly
equal published terms/tags without profile completion. Intent-aware
`getUsage` now reports the selected catalog source and version, separates
observed selection facts from deterministic operation inference, reserves a
distinct label for actual model inference, and emits no operation candidate
without explicit intent overlap. Exact component, usage, and dependency
retrieval now returns bounded catalog alternatives instead of a priority-based
hidden winner and uses attributable absence records for missing selection,
operation, intent, source, version, or dependency evidence. README, user guide,
CAR reference, strategy, phase ledger, and static contracts now share one
source-role and purpose-precedence model. Executable specifications now cover
all five source kinds, their authorization and freshness boundaries, local path
safety, version reconciliation, conflicts, citations, and inference labels.
Phase 3 closure passed full tests, CAR build and descriptor inspection, CML
lint, CAR lint, and representative source-aware MCP projection. Phase 4's
automated implementation and evidence are complete, but the phase remains open
and on hold solely for P4-45 human confirmation.
P4-01 now provides bounded, source-owned authentication schemes and
configuration-key references without projecting credential identity. P4-02
now carries source ownership through every remote provider boundary, resolves
the referenced value only inside the outbound CNCF ProviderCall, and scopes
bearer, Basic, or API-key headers to that source's authorized origin without
placing credential identity or value in CallTree metadata. P4-03 now
distinguishes missing, resolver-unavailable, explicitly expired, and rejected
credentials with stable sanitized source-failure codes and no alternate-source
fallback or authentication retry. P4-04 now executes catalog, BoK-site, and
SIE-mediated authentication through the production ProviderCall/UnitOfWork/
HTTP-driver path and proves exact per-source headers, cross-origin refusal,
redaction, and CallTree-safe metadata. P4-10 now gives catalog and BoK sources
bounded one-minute-through-24-hour normal refresh schedules and exposes each
next attempt without immediate repeated work after failure. P4-11 adds bounded
exponential retry, same-source single-flight, and a fair runtime-wide concurrency
limit against synchronized bursts. P4-12 caps configured source count and
retained Catalog, BoK, SIE, and local observations through stable per-source
quotas, preserves bounded attributable last-known-good evidence, and retains
the pre-policy runtime construction signatures. P4-13 proves that
authentication, transport, parse, and compatibility failures all preserve
stale source-owned evidence until a successful bounded retry establishes a new
current observation. P4-20 established the two snapshot CAR composition and
live CNCF `/mcp` endpoint. The current surface is twelve CBD retrieval tools and
seven SIE semantic-retrieval tools with no administration or mutation tools.
P4-21's policy matrix now verifies baseline `12/7`, global disable `0/0`, SIE
service disable `12/0`, and per-component status-operation disable `11/6`.
Exact tool-set assertions and
disabled `tools/call` rejection prove that runtime policy only narrows declared
readiness. P4-22 now runs a repository-owned
loopback fixture through that composed endpoint and proves that published
catalog, development-directory, and SIE semantic evidence remain separate;
conflicting versions retain both participants with no selected winner even
when result limits apply; and a missing catalog remains degraded under its
bounded retry schedule. P4-30 now keeps
`project.yaml` as the declaration authority and checks it against a
machine-readable assessed-candidate matrix before any representative runtime
work. The only current candidate, `0.5.1-SNAPSHOT`, passed the complete composed
CBD/SIE SAR from clean CNCF revision
`848ef5596af6927512af4e9c8c0d423d4add1253`; excluded is explicitly empty and
unlisted versions remain unassessed. P4-31 now packages a
source-managed seven-operation CAR ABI that is checked against generated CML
model metadata and the built archive. Cozy transition fixtures admit a minor
operation addition, reject a minor removal, and retain the same removal as a
visible permitted major transition. Because no CAR release exists yet, the
historical baseline remains explicitly pending rather than fabricated. The
P4-32 compatibility boundary now distinguishes unavailable Catalog endpoints
from incompatible returned documents, permits only the named deployed
publication contracts, fixes BoK and SIE to their public v1/typed contracts,
and keeps only the valid descriptor-without-version local CAR transition.
Malformed or contradictory input no longer enters an alternate parser or path
guess. P4-40 now projects the same authentication, bounded refresh/cache, SAR
composition, runtime/ABI/input compatibility, and operational failure
contracts through README, user guide, CAR reference manual, strategy, phase
ledger, and a static documentation map. P4-41 now maps all fourteen behavioral
Phase 4 checklist IDs to exact Scala scenarios or executable gate markers and
adds a meta-specification that rejects checklist, evidence-path, or anchor
drift. P4-42 now passes the full CBD and SIE test suites, CAR builds, CML and
CAR lint, packaged SIE component-local dependency resolution, and the complete
four-profile source-aware SAR projection. P4-43 records the assessment artifact,
runtime and dependency SNAPSHOTs, first-release ABI baseline state, residual
warnings, dependency-first manual publication procedure, and the explicit
`not publish-ready` result without publishing. Every Phase 4 checklist item is
complete through P4-44; P4-45 remains unchecked and on hold pending explicit
human confirmation. Phase 5 work proceeds under the recorded scheduling
exception, while actual publication remains a separately authorized workflow.

Phase 5 makes CBD Support the owner of the CAR Review product, provider
orchestration, canonical report, Review Run, Web UI, CLI, MCP report-query, and
gate policy. Cozy supplies CAR/CML deterministic evidence through the generic
provider contract. `sbt-cozy` supplies build/test/package evidence and connects
local or CI execution to Cozy and the CBD Review Application without moving
quality policy into the plugin. The phase proceeds through normative contract,
application core, provider/Cozy integration, sbt CI/CD, user surfaces,
quality/AI/runtime assessment, and final cross-repository verification. Textus
AI supplies provider-neutral structured AI execution and its Phase 1
normalized-provenance, deterministic-fixture, restrictive-observability, and
explicit failure/lifecycle contracts; CBD Support admits those contracts only
with executable compatibility evidence and treats unavailable or incompatible
capabilities as limitations or Unknown.
Publishing, deployment, automatic AI/network enablement, arbitrary server-side
filesystem inspection, and implicit changes to existing sbt publication tasks
remain outside Phase 5.

The P5-03 canonical report contract now fixes one immutable CBD-owned result
with attributable Evidence, Finding, Assurance, Unknown, capability
assessment, baseline, and gate records. A completed Run and CI attestation bind
the same target, profile, providers, rule sets, report digest, and gate result;
integer coverage and normalized SHA-256 digests keep projections and CI
evidence reproducible. P5-04 now fixes the deny-by-default security and
offline-execution envelope: client and server target authority remain
separate, resource work is finite, credential resolution is outbound-only,
redaction covers every projection and AI input, MCP publishes only bounded
authorized read queries, and standard CI is pinned and offline. The same audit
excludes volatile execution identity, times, and array arrival order from the
stable report digest while retaining them in the execution-specific
attestation. Stage 5.1 is complete. P5-10 now implements distinct domain value
types, strict wire and invariant admission, canonical JSON encoding, stable
content digests, and typed sanitized codec failures. P5-11 now has a typed
Review Run codec and CNCF Job status projection covering progress,
cancellation, completion, failure, limitations, stale update rejection, and
terminal immutability. The CBD Review Application adds authorized
start/read/cancel operations, stable Review-to-Job binding, actual persistent
CNCF Job submission/control, read-only `getReviewRun` MCP publication, and
private execution commands.

Phase 5 completed on 2026-07-16 with executable coverage for P5-01 through
P5-55, full CBD/Cozy/sbt-cozy/Textus AI verification, local/CI canonical
response equivalence, generated CAR ABI surface validation, and a representative
SAR runtime check. The remaining non-publication debt is explicitly relocated:
`FUTURE-CBD-RUNTIME-BOUNDARY-01` tracks the CAR lint ambient
clock/environment/filesystem/shell framework-boundary warnings, and
`FUTURE-CBD-ABI-RELEASE-01` tracks comparison against the first released CAR
ABI baseline. Neither item changes the completed Phase 5 Review contract or
authorizes publication.

Phase 6 completed on 2026-07-17 as the separately scoped resolution of
`FUTURE-CBD-RUNTIME-BOUNDARY-01`. It adopts the closed CNCF Phase 36 runtime
capabilities for execution time, declared configuration, opaque secret
references, admitted resource trees, and Process Execution. The work remains
CBD Support implementation: it preserves Review semantics and keeps CNCF free
of CBD-, Cozy-, and CAR-specific types. The checkpoint verified the exact local
CNCF development artifact, deterministic runtime-boundary specs, full CBD test
suite, CAR lint, ABI governance, and standalone SAR build. Publication and the
first released ABI baseline remain outside the completed phase.

Phase 7 is complete as the post-Phase-6 cache-lifetime follow-up. Its runtime
cache and invocation boundary, explicit-configuration harnesses, full test
suite, CAR ABI governance, and normal CAR lint are verified. It has no new
human-confirmation stage; P4-45 remains the independent ON_HOLD human gate.

Phase 8 is DONE. The developer-authorized P8-RQH scope
narrowing passed its focused rollback accumulator and normal Phase suite; exact
lint and the dependency-order locally published representative SAR matrix also
passed. The consumed Phase 8 full review was not rerun. The retained boundary
is existing direct provider submission and CI compatibility, Entity
persistence and exact Report reads, redaction hardening, and total quality
coverage. It does not close P4-45 or alter the independently deferred first
released CAR ABI baseline. Publication and a successor Phase remain outside
this phase; the Phase release commit closes the accepted boundary.

Production restart-safe Review Job integration is nonblocking Deferred Work
owned by cloud-native-component-framework Phase 69. CBD Support must not
locally substitute timeout adapters, Job scans, reservations/outboxes,
process-local results, synthesized leases/lifecycles, or private digests for
the missing durable Job result and exact lookup contracts.
