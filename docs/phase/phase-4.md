# Phase 4: Runtime Hardening

Stage Status:
- Current status: DONE
- Current step: P4-01 through P4-43 are complete; Phase 4 is closed with publication left as a separate explicitly authorized workflow.
- Owner: Textus CBD development
- Update rule: Update after each checklist item obtains reproducible evidence; closure is based only on `phase-4-checklist.md`.

## Purpose

Harden CBD Support for production runtime use without weakening the Phase 3
source-ownership, evidence-attribution, read-only MCP, and explicit-absence
contracts. Add authenticated source access, bounded production refresh and
cache behavior, composed-SAR verification, compatibility governance, and
release evidence.

## Baseline

- Remote sources already require exact-origin or fixed-component-route
  authorization, but outbound requests do not yet have a credential-reference
  or authentication policy.
- Catalog and BoK snapshots have finite TTLs and last-known-good behavior, but
  refresh is demand-driven rather than governed by a production scheduler,
  bounded retry, or single-flight policy.
- CBD publishes six read-only retrieval tools and keeps catalog refresh out of
  MCP, but the Phase 3 closure used component-level projection rather than a
  composed-SAR runtime matrix.
- CAR metadata records CNCF minimum and tested versions, while release ABI
  baselines and an explicit compatibility decision process remain incomplete.

## Scope

- Per-source authentication metadata that references credentials without
  storing secret values in URIs, source state, diagnostics, or CAR content.
- Credential resolution at the CNCF provider/configuration boundary and
  authenticated HTTP requests that remain visible through normal execution
  context and CallTree behavior.
- Production refresh scheduling, retry/backoff, concurrency, and cache bounds
  with observable source state and preserved last-known-good evidence.
- Representative SAR composition and live `/mcp` verification for CBD and SIE
  ownership, runtime disable policy, source-aware retrieval, and private
  administration.
- CNCF/CAR ABI and runtime compatibility governance across declared minimum,
  tested, excluded, and upgrade candidates.
- Documentation, executable specifications, CAR/SAR build evidence, lint, and
  publish-readiness evidence.

## Security and Runtime Constraints

- Source authorization and source authentication are independent: a valid
  credential never authorizes an origin or route that policy rejected.
- Configuration stores only a credential reference and authentication scheme;
  secret material is resolved as late as possible at the outbound provider
  boundary.
- Secret values must not enter source descriptors, warnings, exceptions,
  metrics, CallTree properties, MCP records, generated documentation, or
  persisted CAR/SAR metadata.
- Missing, expired, rejected, or unavailable credentials fail explicitly and
  never fall back to an unrelated source credential.
- Authenticated transport continues to use CNCF execution-context-aware HTTP
  helpers rather than a direct HTTP client.
- Automatic refresh has finite work, bounded retry, and concurrency control;
  an authentication failure does not create an unbounded retry loop.
- Scheduled or administrative refresh does not make a mutation operation MCP
  ready.

## Non-Goals

- Implementing an identity provider, token issuer, or general secret store
  inside CBD Support.
- Accepting credentials embedded in source URIs, query strings, diagnostics,
  or ordinary component configuration values.
- Bypassing CNCF provider/configuration and CallTree boundaries for transport.
- Installing, updating, deleting, or automatically publishing discovered CARs.
- Selecting a winner for source, version, checksum, or dependency conflicts.
- Moving CBD component detail into SIE or moving SIE semantic storage into CBD.
- Publishing a public release without an explicit publish request after
  readiness evidence passes.

## Planned Implementation Slices

1. Define source authentication schemes, credential references, late
   credential resolution, authenticated transport, redaction, and explicit
   authentication failures.
2. Add production refresh scheduling, bounded retry/backoff, single-flight
   refresh, concurrency limits, and cache capacity observations.
3. Verify CBD and SIE together in a representative SAR through the live CNCF
   `/mcp` boundary while preserving tool ownership and private administration.
4. Establish runtime/ABI compatibility gates, upgrade evidence, and documented
   decisions for supported, excluded, and incompatible combinations.
5. Complete documentation, executable specifications, full tests, CAR/SAR
   builds, lint, representative runtime projection, and publish readiness.

## Verification Evidence

- Remote catalog, BoK-site, and SIE sources now accept bounded
  `source-id=scheme:config-key/...` bindings from
  `TEXTUS_CBD_SOURCE_AUTHENTICATION`. Only configured source IDs and the
  explicit `bearer`, `basic`, and `api-key` schemes are admitted; invalid,
  duplicate, unknown, unsupported, raw-value, and overflow entries remain
  sanitized diagnostics.
- Source objects retain the credential reference internally while shared
  descriptors and MCP source records expose only `authenticationScheme` and
  `credentialConfigured`. Generated CML and runtime projections contain no
  `credentialRef`, and P4-01 performs no secret lookup or header construction.
- `SourceAuthenticationSpec`, `CatalogRuntimeSpec`, `BokSourceRuntimeSpec`,
  `SieBokRuntimeSpec`, and `ComponentFactorySpec` passed 47 focused tests on
  2026-07-15, including explicit binding-bound truncation.
- Catalog, BoK-site, and SIE providers now pass their owning source into
  source-aware fetcher/transport overloads. `CbdHttp` resolves the internal
  configuration key only inside `ProviderCall.build_Program` through
  `provider_config_string`, then uses CNCF `http_get`/`http_post`; no direct
  HTTP or configuration client was added.
- Header construction checks the owning source's exact normalized origin before
  resolution, performs no lookup for unauthenticated or cross-origin requests,
  and maps bearer, pre-encoded Basic, and API-key values to their defined
  headers. CallTree request attributes retain only a sanitized URI, source ID,
  scheme, and configured state.
- `SourceAuthenticationSpec`, `CatalogRuntimeSpec`, `BokSourceRuntimeSpec`, and
  `SieBokRuntimeSpec` passed 39 focused tests on 2026-07-15. The executable
  assertions cover all three header schemes, pre-resolution cross-origin
  refusal, no-auth lookup avoidance, and source propagation through catalog,
  BoK, and SIE provider boundaries.
- Credential resolution and authenticated HTTP responses now produce the
  stable sanitized codes `source-credential-missing`,
  `source-credential-unavailable`, `source-credential-expired`, and
  `source-credential-rejected`. Expiry requires an explicit bounded
  `WWW-Authenticate` expiry signal; other authenticated 401/403 responses are
  rejected, while unauthenticated-source responses retain transport ownership.
- The authentication boundary performs one lookup of the owning source's key
  per outbound attempt and has no alternate-key fallback or authentication
  retry. `SourceAuthenticationSpec` passed 9 focused tests on 2026-07-15,
  including distinct failure codes, resolver-exception redaction, unsafe-value
  rejection, explicit remote expiry, and public-source non-classification.
- `CbdHttpSecuritySpec` executes catalog, BoK-site, and SIE-mediated requests
  through the production `CbdHttp` ProviderCall, UnitOfWork interpreter, and a
  recording CNCF HTTP driver. Its 2 executable specifications passed on
  2026-07-15 and prove exact per-source bearer, Basic, and API-key headers,
  three-way credential isolation, and pre-driver cross-origin refusal.
- The same execution captures the CNCF CallTree and proves that it retains the
  sanitized URI, source ID, and authentication scheme while query data,
  request bodies, configuration keys, resolved credentials, and authentication
  header names remain absent. CNCF additionally masks the configured-state
  value in rendered CallTree output.
- Catalog and BoK policies now carry explicit normal refresh intervals bounded
  from one minute through 24 hours and no later than source expiry. Production
  defaults both lifetime and schedule to 15 minutes.
- `nextRefreshAttemptAt` is projected through unified source state and MCP
  output: runtime start before an initial attempt, observation plus interval
  after success, attempt plus the active retry delay after failure, and absent
  for disabled, query-scoped SIE, or uncached local inputs. Readiness calls before the due
  instant perform no remote work; administrative catalog refresh bypasses the
  normal schedule.
- `InformationSourceRefreshSpec`, `CatalogRuntimeSpec`, and
  `ComponentFactorySpec` provide executable evidence for interval bounds,
  catalog and BoK scheduling, administrative schedule bypass, failed-attempt
  deferral, and public next-attempt projection.
- Production-default Catalog and BoK failures schedule demand-triggered retries
  at one, two, four, and successively doubled minute intervals capped by the
  policy maximum and normal refresh interval. Success clears the consecutive-
  failure count; each readiness call performs at most one attempt for a due
  source.
- Concurrent callers for the same source join one source-kind-qualified flight.
  A fair runtime-wide semaphore admits at most the strictest configured Catalog/
  BoK concurrency bound (two by default), so different sources cannot create an
  unbounded synchronized burst. Administrative refresh bypasses time scheduling
  but still uses the same single-flight and concurrency boundary.
- `InformationSourceRefreshSpec` and `CatalogRuntimeSpec` provide executable
  evidence for retry/concurrency policy bounds, Catalog 1/2/4-minute backoff,
  BoK 1/2-minute backoff, four-caller single-flight, and a three-source burst
  limited to two active reads.
- `InformationSourceRetentionPolicy` now rejects more than 64 configured input
  sources and caps retained latest-snapshot observations at 20,000 Catalog
  profiles, 20,000 BoK terms, 800 SIE terms, and 512 local component
  observations. Candidate snapshots remain independently bounded by the
  adapters' byte, resource, query, directory, depth, and artifact policies.
- Each observation total is assigned as a fixed per-source quota in priority
  and source-ID order. Refresh can replace only its owning quota, never evicts
  another source, and records truncation in that source's diagnostics. No
  history is retained; Catalog and BoK failures leave the already-bounded
  attributable last-known-good snapshot unchanged.
- `InformationSourceRefreshSpec`, `CatalogRuntimeSpec`,
  `BokSourceRuntimeSpec`, and `SieBokRuntimeSpec` passed 47 focused tests on
  2026-07-15. They prove invalid-bound rejection, combined source-count
  rejection, fixed multi-Catalog allocation with failed-refresh retention, and
  Catalog, BoK, SIE, and local observation truncation.
- The retention-aware constructor and factory preserve their previous JVM
  signatures through forwarding overloads, so introducing the new policy does
  not invalidate already-compiled callers.
- The complete 15-suite test run passed 108 tests on 2026-07-15 after the
  retention boundary and compatibility-preservation review fix.
- `InformationSourceRefreshSpec` now drives an initially valid BoK snapshot
  through credential-expired authentication, unavailable transport, invalid
  JSON, and unsupported manifest-schema failures at expiry. Every outcome
  preserves the original term, observation time, and expiry as degraded stale
  evidence, records the failure and bounded retry time, and performs no work
  before that retry becomes due.
- The same four executable transitions recover at the retry boundary. Only the
  successful provider result replaces the term and observation time, restores
  fresh/ready state, clears the failure diagnostic, and returns to the normal
  refresh schedule. The 10 focused `InformationSourceRefreshSpec` examples
  passed on 2026-07-15. The complete 15-suite test run then passed 109 tests on
  2026-07-15.
- `examples/cbd-sie-sar/subsystem-descriptor.yaml` now defines the
  `textus-cbd-sie` subsystem with explicit CBD Support and SIE snapshot CAR
  coordinates. `scripts/check-cbd-sie-sar.sh` builds both CARs, assembles a
  temporary descriptor-only SAR beside them in `component.d`, starts one owned
  loopback CNCF server, probes its JSON-RPC `/mcp`, and removes all temporary
  runtime state.
- The live P4-20 check passed on 2026-07-15 with CNCF `0.5.1-SNAPSHOT` at one
  endpoint. Its exact tool set contained six `CbdSupport.CbdRetrieval` tools and
  seven `SemanticIntegrationEngine.SemanticRetrieval` tools. No CBD catalog
  administration, SIE mutation/administration, legacy facade, or unexpected
  tool was exposed.
- The P4-21 live matrix passed on 2026-07-15 against the local CNCF
  `0.5.1-SNAPSHOT` checkout containing runtime-config preservation commit
  `848ef559`. Four separately owned server runs exposed exact CBD/SIE tool
  counts of `6/7` for baseline, `0/0` when MCP was globally disabled, `6/0`
  when the SIE `SemanticRetrieval` service was disabled, and `5/6` when each
  component's `status` operation was disabled.
- Every matrix profile remained a subset of the thirteen component-declared
  baseline tools. Calls to representative globally, service, and operation-
  disabled tools returned JSON-RPC invalid-params code `-32602`; no disabled
  tool remained invocable through a hidden route.
- The P4-22 baseline probe passed on 2026-07-15 with a repository-owned
  loopback fixture and the composed CBD/SIE endpoint. One search returned the
  `fixture-catalog` published-catalog observation at `1.0.0`, the `working`
  development-directory observation at `1.1.0-SNAPSHOT`, and SIE-owned
  `semantic` evidence for `architecture:runtime` as three separate records.
- The conflicting CBD versions produced a `version-conflict` issue containing
  both source IDs and no `selectedObservation`. Repeating the search with
  `limit=1` bounded the returned observations and semantic evidence without
  dropping either conflict participant or choosing a hidden winner.
- The same run configured a missing catalog beside the valid fixture. It
  remained degraded with bounded diagnostics, and an immediate second search
  preserved its retry timestamps instead of performing unbounded repeated
  work. The SIE term was ingested only into the temporary in-memory SIE fixture;
  external catalog availability was not required for these assertions.
- `project.yaml` remains authoritative for the CNCF minimum, tested, and
  excluded declarations. `docs/spec/runtime-compatibility-matrix.json` records
  the assessed candidates and evidence IDs, while
  `scripts/check-runtime-compatibility.py` rejects declaration drift,
  unassessed candidates, excluded candidates, and compile dependencies that
  are not declared tested.
- The P4-30 declaration check passed on 2026-07-15 with minimum, tested, and
  compile dependency all equal to `0.5.1-SNAPSHOT`; the excluded set is
  explicitly empty. The one candidate is classified `tested-compatible` and
  requires the `representative-sar` evidence. Unlisted versions remain
  unassessed rather than implicitly compatible or incompatible.
- The representative evidence then passed from a clean local clone at CNCF
  revision `848ef5596af6927512af4e9c8c0d423d4add1253`. It rebuilt both CARs,
  passed source-aware baseline retrieval and all four policy profiles, and
  emitted `RUNTIME_COMPATIBILITY_EXECUTION_OK` with runtime
  `0.5.1-SNAPSHOT`, evidence `representative-sar`, and `worktree=clean`.
- `src/main/car/abi-manifest.json` now owns the current CAR ABI surface. It
  exports the component and all seven CML operations, including the
  administration operation that runtime MCP policy keeps private, with no
  exported entities or component ABI dependencies.
- `scripts/check-car-abi.sh` passed on 2026-07-15. It rebuilt the CAR, matched
  the current ABI against generated CML model metadata, and proved the CAR
  embeds the source-managed manifest unchanged. The resulting markers reported
  seven operations, zero entities, and package equality.
- Cozy strict ABI lint reports the current `0.1.0-SNAPSHOT` manifest as
  readable and `abi.baseline.missing` because no Textus CBD Support CAR has
  been released. The repository records that honest first-release pending state
  and does not create a fictitious versioned release baseline.
- Transition fixtures passed the committed Cozy SemVer policy: an operation
  addition from `0.1.0` to `0.2.0` produced `OK abi.operation.added`, an
  operation removal in the same minor transition produced
  `FAIL abi.operation.removed`, and the same removal in a `1.0.0` transition
  remained visible as an intentionally permitted major-version finding.
- `docs/spec/input-compatibility-governance.md` now records the accepted current
  and older input shapes for Catalog, BoK, SIE, and local CAR boundaries. A
  fallback is permitted only for a named older contract, never as a parser or
  identity guess after incompatible evidence is observed.
- Catalog ingestion accepts the revision-pinned unversioned Cozy index and the
  deployed `cozy.publish-project.v1` or unversioned publication contract. The
  publication adapter runs only when both rich-index kinds are unavailable; a
  returned malformed, structurally invalid, or unknown-schema rich index fails
  the source without probing publication metadata. Declared publication
  schemas and document types are validated before component metadata is used.
- BoK input remains fixed to `cncf.knowledge-source.v1` with no older accepted
  schema or presentation-page fallback. SIE remains fixed to the public typed
  `SemanticIntegrationEngine.SemanticRetrieval.searchTerms` result and rejects
  legacy camelCase/internal-facade shapes without field translation.
- Local CAR inspection preserves one supported legacy transition: a valid
  descriptor with component identity but no version may retain the repository
  path version as explicitly labeled path evidence. Missing/malformed
  descriptors, missing component identity, and descriptor/path name or version
  conflicts now reject the artifact instead of manufacturing an observation.
- The four focused compatibility suites passed 51 tests on 2026-07-15,
  including unavailable-versus-incompatible Catalog decisions, named and
  unversioned publication input, BoK v1 refusal, typed SIE refusal, the legacy
  local path-version transition, malformed descriptor rejection, and
  coordinate-conflict rejection.
- The complete 15-suite test run passed 114 tests on 2026-07-15 after the
  P4-32 compatibility changes. Normal CAR lint had no failures; its only
  residual warning remains the documented first-release ABI baseline pending
  state.
- `docs/spec/phase-4-documentation-map.md` now maps the normative Phase 4
  authentication, refresh/cache, SAR composition, compatibility, and
  operational-failure contracts to all six required document surfaces.
- README, user guide, and CAR reference manual now publish the same remote-
  source authentication syntax, supported schemes, safe posture fields,
  credential lifecycle codes, late CNCF resolution boundary, and no alternate-
  key/internal-retry rule. Their operator guidance also connects credential
  failures to the bounded degraded/stale refresh transition.
- The three audience documents now distinguish unavailable Catalog endpoints
  from incompatible returned documents and summarize the supported older
  Catalog/local-CAR inputs plus fail-closed BoK and SIE contracts. None directs
  an operator to translate fields, guess paths, or force an unrelated parser.
- Existing representative-SAR, runtime matrix, ABI, refresh/cache, retention,
  source-ownership, and failure-state guidance was checked against the static
  contracts. The stale link to a concurrently moved publisher candidate was
  removed while its stable `FUTURE-CATALOG-PUBLISHER-01` identity remains.
- `docs/spec/phase-4-executable-coverage.json` now maps all fourteen behavioral
  checklist IDs, P4-01 through P4-32, to exact Scala scenario names or
  executable script-gate markers. Its four areas cover authentication,
  refresh/cache, SAR composition, and compatibility without treating the
  documentation and release gates as behavioral specifications.
- `Phase4ExecutableCoverageSpec` reads the authoritative checklist and the
  machine-readable map together. It rejects a missing, duplicate, or extra
  behavioral ID; an unknown area or evidence kind; a non-relative or missing
  evidence path; a non-executable script gate; and an absent exact scenario or
  marker anchor.
- The focused P4-41 coverage suite passed two tests on 2026-07-15. Existing
  behavior specifications and live gates remain the contract evidence; the new
  meta-specification makes future checklist or scenario drift fail visibly.
- The complete 16-suite test run then passed 116 tests. Normal CAR lint again
  reported no failures and only the documented first-release ABI baseline
  pending warning.
- P4-42 passed the complete verification boundary on 2026-07-15. CBD Support
  passed 116 tests across 16 suites, while SIE passed 81 tests across 11
  suites. Both projects passed CML lint and normal CAR lint; CBD retained only
  the documented first-release `abi.baseline.missing` warning, and SIE retained
  its existing missing ABI manifest warning.
- The same gate rebuilt both snapshot CARs and verified that the SIE CAR
  declares `org.jsoup:jsoup:1.18.1` as a component-local dependency without
  bundling it. The resolved CNCF `0.5.1-SNAPSHOT` runtime loaded that dependency
  through the packaged-CAR classloader and completed the source-aware baseline
  retrieval.
- The representative SAR then completed all four live policy profiles with
  exact CBD/SIE read-tool counts of `6/7`, `0/0`, `6/0`, and `5/6`. It emitted
  `CBD_SIE_SOURCE_AWARE_OK`, `CBD_SIE_SAR_POLICY_MATRIX_OK`, and
  `RUNTIME_COMPATIBILITY_EXECUTION_OK` for the resolved runtime coordinate.
- `docs/phase/phase-4-publish-readiness.md` records the P4-43 assessment without
  publishing. The assessment artifact is the `0.1.0-SNAPSHOT` CAR built by
  P4-42, including its descriptor, ABI surface, size, and SHA-256.
- The public-release result is explicitly `not publish-ready`: the target CAR
  and its required CNCF `0.5.1-SNAPSHOT` compile/runtime coordinate are mutable
  snapshots, the CNCF prerequisite worktree is not a clean release candidate,
  and `publishTo` is unset. Separately, the first released ABI baseline
  correctly remains pending and is not an independent first-release blocker.
  Strict CAR lint reported no failures and retained only the expected
  `abi.baseline.missing` warning.
- The readiness record defines the dependency-first publish order, every
  version and compatibility record that must change, the validation rerun, the
  manual `sbt --batch publish` command, post-publish repository checks, and the
  rule that the `0.1.0` ABI baseline is recorded only after a real release.
  Neither a publish command nor a local publication was executed.

## Closure Basis

Phase 4 is DONE only when every item in `phase-4-checklist.md` is `[x]` and its
verification evidence is recorded here.
