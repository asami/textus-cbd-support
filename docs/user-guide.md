# Textus CBD Support User Guide

## Purpose

Use CBD Support when a generative AI developer needs to find another CAR/SAR,
understand its contract, and determine how to reuse it. Use SIE separately for
BoK terminology grounding and for discovering that a BoK-managed component
exists.

## Prerequisites

- Java 21
- sbt 1.9.7 or later
- Cozy/sbt-cozy `0.3.0-SNAPSHOT` development environment
- CNCF `0.5.1-SNAPSHOT`
- Network access to simplemodeling.org or one configured component catalog

## First Success

1. Run `sbt --batch test cozyBuildCAR`.
2. Start the CAR through the CNCF server runtime used by the target SAR.
3. Inspect generated component Help for `CbdSupport`.
4. Call `CbdRetrieval.status` and confirm at least one source is `ready`.
5. Call `CbdRetrieval.searchComponents` with a concrete requirement.
6. Pass the returned `ComponentReference` to `getComponent`, `getUsage`, or
   `resolveDependencies` by using its identity fields, including `kind`.

## Review Run Operations

Use the private `CbdReviewAdmin.startReview` command with `targetKind`, `name`,
`targetDigest`, and `profile`; organization and version are optional. The
caller needs the `reviewer`, `operator`, or `admin` role. The response contains
both the CBD-owned Review ID and its bound CNCF Job ID.

Use the MCP-ready `CbdRetrieval.getReviewRun` query with that Review ID to read
bounded progress. The caller needs `viewer`, `reviewer`, `operator`, or
`admin`. Only `operator` or `admin` may invoke the private
`CbdReviewAdmin.cancelReview` command. Start and cancellation are deliberately
absent from MCP; the Web, CLI, `sbt-cozy`, or internal component command path
must supply the authorized call.

A completed Run always has a canonical report ID and digest. A Job that fails,
is cancelled, or succeeds without a canonical report is projected as an
explicit failed/cancelled Run with limitations; it is never presented as an
empty successful Review. Provider bundle execution begins in P5-12, so P5-11
establishes the operation and Job lifecycle rather than claiming provider
coverage.

For an authenticated completed-report view, enter that exact Report ID in the
Web forms or use the MCP-ready `CbdRetrieval.getReviewSummary`,
`CbdRetrieval.getReviewReport`, `CbdRetrieval.listReviewFindings`, or
`CbdRetrieval.listReviewAssurances` queries. These queries require the same
read role and never list Review history. Finding and Assurance results require
an explicit limit from 1 to 100. The report projection excludes Evidence facts
and rationale, redacts credential-shaped messages, and reduces local paths to
a basename.

Use `CbdRetrieval.getReviewViews` for the corresponding CNCF, implementation,
and quality navigation views. Every view item retains the Report's canonical
Evidence and Observation IDs plus provider/rule/bundle attribution and safely
rendered implementation locations; the view does not rerun analysis or choose
one provider conclusion.

## Review Submission Gateway

For an authorized local or CI integration, submit provider evidence to the
private HTTP gateway with `POST /rest/v1/cbd-support/cbd-review-admin/post`.
Set `Content-Type: application/json` and send a generated outer object whose
only field is `submissionDocument`; its value is the complete
`textus.cbd.review-submission.v1` JSON document encoded as a JSON string. The
response's `canonicalResponse` field is likewise a JSON string containing the
CBD-owned canonical response. The gateway accepts only `reviewer`, `operator`,
or `admin`; it never accepts a workspace path, process command, policy
template, or caller-selected gate.

`sbt-cozy` uses `review.cbd.endpoint` and the `cozyReviewSubmit` task to invoke
Cozy locally, combine Cozy and sbt evidence, and exchange this envelope. CBD
Support also provides the same submission contract as a CLI main class:

```bash
TEXTUS_CBD_REVIEW_PROCESS_ROLES=reviewer \
  sbt --batch 'runMain org.simplemodeling.textus.cbdsupport.runtime.CarReviewCliMain review submit' \
  < provider-document-submission.json

sbt --batch 'runMain org.simplemodeling.textus.cbdsupport.runtime.CarReviewCliMain review submit --endpoint http://127.0.0.1:19538/rest/v1/cbd-support/cbd-review-admin/post' \
  < provider-document-submission.json
```

The local command resolves roles only from its process boundary; it has no
`--role` option. The server-backed command sends no role header, credential, or
workspace path, and relies on the configured server authentication boundary.
Both variants print one stable `review-cli-result` JSON document containing the
CBD Review ID (`runId`), canonical response, and gate. Exit code `0` is `pass`,
`2` is `fail`, `3` is `unknown`, and `1` denotes usage, transport, admission,
or response errors.

## Review Web Progress

The CBD Support static Web app exposes authenticated
`CbdRetrieval.getReviewRun`, `getReviewSummary`, `getReviewReport`,
`listReviewFindings`, and `listReviewAssurances` forms. Enter a Review ID to view the CBD-owned
Review/Job binding, digest-bound target, profile, lifecycle timestamps, current
state, provider-attributed limitations, and—once completed—the Report ID and
digest. The Web form renders these response fields as supplied; it does not
derive a quality score, a provider conclusion, or a replacement gate.

## Representative SAR Check

From the CBD Support project root, run:

```bash
scripts/check-cbd-sie-sar.sh
```

The command builds the CBD Support and sibling SIE snapshot CARs, creates a
temporary `textus-cbd-sie` SAR for each policy profile, and checks each through
a separately owned loopback CNCF server. Success requires exact CBD/SIE counts
of baseline `7/7`, global disable `0/0`, SIE service disable `7/0`, and status-
operation disable `6/6`. Representative disabled calls must return JSON-RPC
`-32602`. CBD catalog administration, SIE mutation/administration, the legacy
SIE MCP facade, and all other unexpected tools must remain absent. Each server
and temporary SAR is removed on exit. Set `TEXTUS_SIE_ROOT` only when the
sibling SIE checkout is in a non-default location; set `CNCF_RUNTIME_DEV_DIR`
when validating a local CNCF runtime checkout.

Before building, the command checks the selected `CNCF_VERSION` against
`project.yaml` and `docs/spec/runtime-compatibility-matrix.json`. The current
matrix admits only the declared tested candidate `0.5.1-SNAPSHOT`; the excluded
set is empty and every unlisted version is unassessed. A successful final marker
records whether the runtime came from a resolved coordinate or development
directory, plus its revision and worktree state when Git evidence is available.

The baseline profile additionally serves repository-owned fixtures and ingests
their BoK glossary into the temporary in-memory SIE. Its live source-aware
probe requires a published-catalog `textus-runtime` at `1.0.0`, a separate
development-directory observation at `1.1.0-SNAPSHOT`, and SIE-owned
`architecture:runtime` evidence. The version conflict must retain both CBD
source IDs and expose no selected observation, including when the visible
result limit is one. A deliberately missing catalog must remain degraded with
bounded diagnostics and unchanged immediate-retry timestamps.

## CAR ABI Check

Run the project-owned ABI gate before release review:

```bash
scripts/check-car-abi.sh
```

The check builds the CAR, compares `src/main/car/abi-manifest.json` with the
generated CML operation surface and the packaged top-level manifest, then runs
Cozy strict ABI lint over compatible-addition, breaking-minor, and
intentional-major transition fixtures. The current `0.1.0-SNAPSHOT` line has no
previously released baseline, so `CAR_ABI_CURRENT_BASELINE_PENDING` is expected
until an actual release manifest is preserved under
`src/main/car/<released-version>/abi-manifest.json`.

## Authenticated Remote Sources

Authorize the catalog, BoK, or SIE origin/route first, then bind its configured
source ID to a credential reference:

```bash
export TEXTUS_CBD_SOURCE_AUTHENTICATION='team=bearer:config-key/catalog.team.token,knowledgehub=basic:config-key/bok.basic,semantic=api-key:config-key/sie.api.key'
```

The supported schemes are `bearer`, `basic`, and `api-key`. The reference must
begin with `config-key/` and names a CNCF resolved runtime parameter; do not put
a token, password, header value, or secret-bearing URI in this setting. The
runtime resolves the owning source's value once at the outbound ProviderCall
boundary, after exact-origin or `/mcp` route authorization. It never tries a
different source's reference.

Bearer adds `Authorization: Bearer`; Basic expects the CNCF value to be the
pre-encoded Basic payload; API key adds `X-Api-Key`. These values are resolved
at request time and must not be copied into ordinary source configuration.

Use `listCatalogs` to inspect only the safe posture fields
`authenticationScheme` and `credentialConfigured`. The credential reference,
resolved value, and authentication header are absent from MCP records,
diagnostics, CallTree output, and CAR metadata. Operational failures use these
stable sanitized codes:

| Code | Operator interpretation |
|---|---|
| `source-credential-missing` | the source-owned CNCF configuration key has no value |
| `source-credential-unavailable` | the CNCF configuration resolver could not complete lookup |
| `source-credential-expired` | an authenticated 401 explicitly reported expiry |
| `source-credential-rejected` | the value was unsafe, or the remote source rejected authentication |

Authentication itself has no retry or alternate-key fallback. For a cached
Catalog or BoK source, the failure enters the normal degraded/stale transition
and a later attempt occurs only at the bounded `nextRefreshAttemptAt`.

## Input Compatibility Decisions

Treat input availability separately from compatibility:

- Catalog accepts the revision-pinned Cozy repository-index envelope. Only
  fetch-level unavailability of both CAR and SAR indexes permits the deployed
  `cozy.publish-project.v1` or observed unversioned publication contract. A
  returned malformed or unknown-schema rich document fails without that
  fallback.
- BoK accepts only `cncf.knowledge-source.v1`; it does not scrape HTML or guess
  another manifest/glossary path.
- SIE accepts only the public typed
  `SemanticIntegrationEngine.SemanticRetrieval.searchTerms` result with its
  snake_case evidence fields; legacy facade/field shapes are rejected.
- A local CAR must have a valid descriptor agreeing with its repository
  coordinates. The one supported older input is a valid descriptor that
  identifies the component but omits `version`; the path version remains
  explicitly labeled `repository-path` evidence.

An incompatible input contributes a bounded diagnostic, not a synthesized
observation. See `docs/spec/input-compatibility-governance.md` for the
normative matrix.

## Normal Workflow

1. Search with a requirement such as `account authentication` and optional
   identity, version, source, freshness, availability, conflict, or purpose
   filters.
2. Inspect catalog-backed `results` separately from source-preserving
   `observations`, `issues`, and `precedence`.
3. Resolve the exact identity with `getComponent`.
4. Read operation and documentation evidence with `getUsage`. Supply an
   optional concrete `intent`, such as `retrieve an order`, when operation
   guidance is useful.
5. Read direct and transitive dependency evidence with `resolveDependencies`.
   Use `maxDepth` when a bound other than the default 8 is required, and inspect
   every resolution status and conflict rather than assuming a selected winner.
6. Select a component only when the returned catalog evidence supports the
   requested version and runtime. A runtime-constrained search excludes
   profiles that do not publish runtime compatibility evidence or whose
   inclusive minimum/maximum range excludes the requested runtime.

CBD Support never invents missing versions, dependencies, operations, or
documentation. Missing evidence is returned as a warning or a stable
`absences` record, according to the operation contract.

`getUsage` reports `selectedSourceId`, `selectedSourceKind`, and
`selectedVersion` for the catalog profile whose usage was actually read. Its
`guidance` keeps the selected-source statement as `observed-fact`. An operation
is recommended only when its observed service, operation, kind, or description
shares an explicit token with the bounded intent; that recommendation is
`deterministic-inference` with a score, rationale, and catalog/model-metadata
evidence URIs. An unrelated intent produces no operation recommendation. The
`model-inference` kind is reserved for generative advice; this runtime does not
invoke a generative model and never relabels deterministic matching as model
inference. Missing source context withholds guidance rather than inventing a
published source.

Exact lookup does not use source priority to hide ambiguity. If the same exact
identity and version occur in several catalogs, `getComponent`, `getUsage`, and
`resolveDependencies` return status `ambiguous`, no selected component, the
full `candidateCount`, and at most 20 `alternatives`. Choose one returned
`catalogId` explicitly before requesting details. If no candidate exists, the
response contains `component-not-found`. Other insufficient evidence appears
in `absences` with source/version/evidence citations. In particular,
`dependency-metadata-absent` distinguishes unpublished dependency metadata
from an authoritative empty dependency array.

Dependency traversal stays inside the selected component's catalog. An
unresolved dependency is not silently taken from another configured source.
Conflicting explicit versions are returned with their evidence paths; CBD
Support does not choose which version should win.
`selectedVersion` identifies the component version chosen by the catalog, while
`dependencyMetadataVersion` identifies the version owning parsed dependency
data. If an explicit request differs, the affected dependency path stops and a
warning is returned instead of applying another version's metadata.
When `version` is explicit, search and exact lookup project only evidence for
that version. If the catalog lists the version without detail, identity remains
available but artifact, runtime, dependency, and model-metadata fields are
absent with a warning.

Rich Cozy profiles retain the selected version's channel, status, component,
publication time, runtime minimum/maximum/tested versions, artifact SHA-256,
and model-metadata sidecar. `runtimeTested` is supporting evidence, not an
exclusive allowlist. Cozy repository diagnostics remain source warnings even
when the affected catalog entry can still be used.

Catalog snapshots have a 15-minute lifetime. Retrieval operations reuse a
retained snapshot until `nextRefreshAttemptAt` and attempt refresh when that
normal schedule is due. A failed attempt retains the stale last-known-good
snapshot. Use `listCatalogs`
to inspect `cacheStatus`, `refreshedAt`, `expiresAt`,
`lastRefreshAttemptAt`, `nextRefreshAttemptAt`, and any warning. The normal
catalog and BoK refresh interval defaults to 15 minutes, is bounded from one
minute through 24 hours, and cannot be later than source expiry. Readiness
before `nextRefreshAttemptAt` reuses retained state without another source
request; explicit catalog administration bypasses the normal schedule.
After a source failure, retries start after the configured initial interval,
one minute by default, and double up to the configured maximum; success resets
the sequence. Concurrent work for the same source is coalesced into one
request, and the runtime admits at most two distinct source refreshes by
default. Administration shares these concurrency bounds even though it
bypasses the time schedule.
Catalog source/origin configuration, index and metadata bytes, and discovered
profile count are bounded; truncation remains visible as a warning.
The runtime also admits at most 64 sources and stores latest snapshots only.
Across those snapshots it retains at most 20,000 Catalog profiles, 20,000 BoK
terms, 800 SIE terms, and 512 local observations. Stable per-source quotas are
allocated by priority and source ID, so one refresh cannot evict another
source's evidence. Quota truncation is diagnostic, and a failed Catalog or BoK
refresh preserves the already-bounded last-known-good snapshot.
Authentication, transport, JSON parsing, and source-contract compatibility
failures all leave that snapshot visibly `degraded` and `stale`; they do not
advance its observation time. Check the diagnostic and
`nextRefreshAttemptAt`. Readiness performs no additional request before that
instant, and only a successful retry restores `ready`/`fresh` with a new
observation time.

Additional catalog configuration is authorized in two steps. Put candidate
base URIs in `TEXTUS_CBD_CATALOGS` and their permitted network origins in
`TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS`. Origin matching uses scheme, host, and
effective port; it does not authorize another scheme or port. The built-in
simplemodeling.org source does not require an allowlist entry. Rejected entries
are excluded from network access and returned as warnings.

Additional BoK sites use `TEXTUS_CBD_BOK_SITES` and
`TEXTUS_CBD_BOK_ALLOWED_ORIGINS` with the same `[id=]base-uri` and exact-origin
authorization model. A supported site must publish
`metadata/cncf/knowledge-source.json` using
`schemaVersion=cncf.knowledge-source.v1`; CBD Support follows only bounded
manifest-declared `glossary-terms` JSON and never scrapes rendered pages.
`listCatalogs` retains its compatibility name but reports both catalog and BoK
source identity, kind, readiness, freshness, and diagnostics. BoK terms remain
semantic evidence and are not converted into component catalog facts. BoK
snapshots also have a 15-minute default lifetime; failed expiry refresh retains
stale terms with the original observation/expiry and the latest attempt time.

To retrieve BoK terminology through SIE, authorize the SIE origin with
`TEXTUS_CBD_SIE_ALLOWED_ORIGINS` and configure one or more `[id=].../mcp`
routes in `TEXTUS_CBD_SIE_BOK_ROUTES`. CBD accepts only the public `/mcp` route
and invokes the typed
`SemanticIntegrationEngine.SemanticRetrieval.searchTerms` operation. Each term
must include the SIE contract's identity, definition, dataset, match, rationale,
score, and absolute `evidence_uri`; an incomplete term response is rejected as
a source failure. Query/category characters, response body, and result count
are bounded. Results are query scoped and are never reused for another query;
the last successful observation remains only as degraded source-state evidence
after a later failure.
`searchComponents` triggers this SIE lookup with the same requirement, while
its component matches continue to come only from CBD-owned catalog evidence.

Configure working directories with comma-separated `[id=]path` entries in
`TEXTUS_CBD_DEVELOPMENT_DIRECTORIES`. Optional
`TEXTUS_CBD_LOCAL_CAR_ROOT` and `TEXTUS_CBD_CACHE_CAR_ROOT` settings override
the canonical `~/.cncf/local` and `~/.cncf/cache` roots. These inputs are
inspected read-only and within explicit bounds whenever retrieval inputs are
initialized. They appear in `listCatalogs` and `status` with `observed`
freshness and their own diagnostics.

Source-aware `searchComponents` adds `sourceId`, `sourceKind`, `freshness`,
`versionState`, `conflictCode`, and `purpose` filters. Its `results` contain
only catalog-backed profiles. Working, local-published, and cached evidence is
returned separately in `observations`; `issues` cites conflicts and
`precedence` explains purpose-specific authority. `selectedObservation`
remains absent because CBD Support does not choose a hidden winner.

Matching BoK-site and current-query SIE terms appear separately in
`semanticEvidence`, including source, term, match rationale, freshness, and
evidence URI. A component result lists the applicable citation IDs in
`semanticEvidenceIds` only when its catalog explicitly publishes the same term
or tag. Semantic evidence can help discover that catalog profile, but it never
adds component versions, runtime constraints, dependencies, operations, or
artifact facts. An older retained SIE response is never reused for a different
query.

An authorized SIE source appears as `sie-bok` with
`component-route-allowlist` authorization. Before retrieval it is
`not-started`; a valid response makes it `ready`, while transport, MCP, schema,
or evidence failures make it `degraded`. Retrieved terms remain SIE-owned
observations. They do not add versions, dependencies, operations, artifacts,
or usage statements to CBD component profiles.

### Choosing Purpose Precedence

Use `purpose` to ask which evidence is authoritative for the current decision:

| Purpose | Authority order |
|---|---|
| `development-work` | working directory, then local/cache artifacts, then published comparison |
| `local-execution` | local-published artifact, then cached artifact; working/published identity is supporting evidence |
| `published-reuse` | published catalog, with working/local/cache evidence retained for comparison |
| `artifact-verification` | all available checksums are peers and a disagreement has no winner |

The returned `precedence` is guidance, not selection. Always inspect
`observations` and `issues`; `selectedObservation` remains absent. Exact detail
operations independently require one catalog candidate and return bounded
alternatives when catalog identity is ambiguous.

## SIE Handoff

SIE component discovery returns only `ComponentReference`. The shared fields
are `sourceId`, `catalogId`, `organization`, `name`, `title`, `kind`, `version`,
and `evidenceUri`. SIE normally supplies `sourceId`; CBD Support supplies
`catalogId`. The stable handoff fields are component identity, kind, version,
and evidence URI.

Do not expect SIE to explain component dependencies or usage. CBD accesses
only SIE's public read-only component/MCP contract, never its RDF/vector stores
or private ingestion operations. Do not load CBD catalog data into SIE merely
to make component development tools available.

## Troubleshooting

- `not-started`: no catalog has been loaded yet. Invoke a retrieval operation
  or run the administrative refresh command.
- `degraded` with retained components: the latest refresh failed and the last
  known good snapshot remains active, or a snapshot is stale. Inspect
  `cacheStatus`, expiry, last attempt, and warning together.
- `degraded` with zero components: every initial catalog load failed. Check the
  configured base URI and the two metadata index paths.
- `no-match`: no catalog evidence satisfied the identity and filters. Remove
  optional filters only when broader discovery is acceptable.
- Missing operations: the catalog did not publish a model-metadata sidecar or
  it could not be read. Use the warning and evidence URI to diagnose the
  publisher.
- Cozy archive diagnostic: inspect the warning code, artifact, and version.
  Missing or JSON `null` descriptor/ABI metadata means dependency evidence is
  unavailable; an explicit empty dependency array means authoritative no
  dependencies.
- Partial publication failure: successfully loaded components remain available,
  but the source is degraded and lists each unreadable component entry.
- Rejected configured source: inspect catalog warnings for an invalid source
  ID/base URI, missing exact origin permission, or duplicate source ID. Keep
  credentials, query strings, and fragments out of catalog base URIs.
- Credential failure: use the stable `source-credential-*` code to distinguish
  missing, resolver-unavailable, expired, and rejected states. Do not copy the
  configuration key or credential into diagnostics while investigating it.
- Incompatible input: do not rename fields, guess paths, or force the catalog
  publication adapter. Correct the producer contract identified by the source
  diagnostic.
- Publication compatibility mode: identity, version, artifact, and
  documentation are authoritative, but operation and dependency details wait
  for the publisher's Cozy repository index/model-metadata sidecars. The
  publisher gap remains identified as `FUTURE-CATALOG-PUBLISHER-01`.
