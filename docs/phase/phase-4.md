# Phase 4: Runtime Hardening

Stage Status:
- Current status: IN_PROGRESS
- Current step: P4-12 bounded snapshot retention is complete; the next slice verifies refresh-state failure transitions under P4-13.
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

## Closure Basis

Phase 4 is DONE only when every item in `phase-4-checklist.md` is `[x]` and its
verification evidence is recorded here.
