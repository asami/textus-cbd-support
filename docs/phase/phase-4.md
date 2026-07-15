# Phase 4: Runtime Hardening

Stage Status:
- Current status: IN_PROGRESS
- Current step: P4-03 credential lifecycle failure classification is complete; the next slice proves the full authentication security matrix under P4-04.
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

## Closure Basis

Phase 4 is DONE only when every item in `phase-4-checklist.md` is `[x]` and its
verification evidence is recorded here.
