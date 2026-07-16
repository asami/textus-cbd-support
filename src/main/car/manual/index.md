# Textus CBD Support Reference Manual

## Component Contract

`CbdSupport` reads external Cozy component catalogs and serves read-only CBD
information. It owns no catalog publication and does not write into SIE.

The component contains three services:

- `CbdRetrieval`: read-only, MCP-ready discovery and inspection.
- `CbdCatalogAdmin`: explicit refresh administration, not MCP ready.
- `CbdReviewAdmin`: authorized Review start and cancellation, not MCP ready.

## Configuration

`TEXTUS_CBD_CATALOGS` adds catalog base URIs to the default
`https://www.simplemodeling.org/` source. The syntax is:

```text
[id=]https://host/base/,[id=]https://other/base/
```

Every additional source also requires an exact allowed origin:

```text
TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS=https://host,https://other:8443
```

Origin authorization compares normalized scheme, host, and effective port.
Allowlist entries must be origins without paths. Catalog base URIs may contain
paths but must be absolute HTTP(S), must not contain user information, query,
or fragment, and must use an alphanumeric/dot/underscore/hyphen source ID.
Invalid, non-allowlisted, and duplicate-ID entries are rejected before network
access. The first accepted ID is retained, and every rejection is returned as
a warning without echoing credential-bearing input. The default
simplemodeling.org source is built-in and does not require allowlisting.

Additional BoK sites use a separate exact-origin boundary:

```text
TEXTUS_CBD_BOK_SITES=[id=]https://host/bok/
TEXTUS_CBD_BOK_ALLOWED_ORIGINS=https://host
```

CBD Support reads only
`metadata/cncf/knowledge-source.json` with schema
`cncf.knowledge-source.v1`, followed by bounded manifest-declared
`glossary-terms` JSON resources. It does not scrape rendered BoK pages or guess
alternate metadata paths. Configured source identity and publisher manifest
identity remain separate evidence. Invalid, duplicate, traversal,
cross-origin, non-JSON, or oversized inputs remain diagnostic.

SIE-mediated BoK knowledge uses a separate public component-route boundary:

```text
TEXTUS_CBD_SIE_ALLOWED_ORIGINS=https://sie.example
TEXTUS_CBD_SIE_BOK_ROUTES=semantic=https://sie.example/mcp
```

Only exact-origin-authorized `/mcp` endpoints are accepted. CBD invokes
`SemanticIntegrationEngine.SemanticRetrieval.searchTerms`, bounds both result
count and response bytes, and requires the typed SIE term fields including an
absolute evidence URI. A transport, MCP, schema, or evidence failure degrades
that SIE source. CBD does not access SIE storage or administration routes, and
does not merge SIE-owned terminology into CBD-owned component details.
`searchComponents` invokes the SIE lookup with the same requirement before it
performs independent catalog matching.

### Source Authentication

Already authorized remote sources may bind credentials by configured source
ID:

```text
TEXTUS_CBD_SOURCE_AUTHENTICATION=source-id=scheme:config-key/runtime.configuration.key
```

Multiple bindings are comma-separated. The supported schemes are `bearer`,
`basic`, and `api-key`: bearer adds `Authorization: Bearer`, Basic expects the
resolved value to be the pre-encoded Basic payload, and API key adds
`X-Api-Key`. Raw values and
references not beginning with `config-key/` are rejected. Origin or component-
route authorization is checked before CNCF resolves the owning source's value
inside the outbound ProviderCall. No credential can expand authority or be
reused for another source.

Information-source descriptors expose only `authenticationScheme` and
`credentialConfigured`. Credential references, resolved values, request
headers, query data, and challenge bodies are excluded from MCP output,
diagnostics, CallTree metadata, and CAR content. Credential lifecycle failures
are `source-credential-missing`, `source-credential-unavailable`,
`source-credential-expired`, or `source-credential-rejected`. Authentication
does not retry or fall back to another key; cached-source recovery follows the
separate bounded refresh schedule.

Read-only development and CAR-storage inputs are configured separately:

```text
TEXTUS_CBD_DEVELOPMENT_DIRECTORIES=[id=]/absolute/project/path
TEXTUS_CBD_LOCAL_CAR_ROOT=/absolute/local/root
TEXTUS_CBD_CACHE_CAR_ROOT=/absolute/cache/root
```

The CAR roots default to `~/.cncf/local` and `~/.cncf/cache`. Those defaults
use `canonical-storage-root` authority; configured replacements and development
directories use `explicit-path-allowlist`. Every local root is canonical,
bounded, inspected without following escaping symbolic links, and never written
by CBD Support.

CNCF MCP publication can be narrowed with:

- `cncf.mcp.enabled=false`
- `cncf.mcp.disabled-services=CbdRetrieval`
- `cncf.mcp.disabled-operations=CbdRetrieval.getUsage`

The repository's representative `textus-cbd-sie` SAR composes this CAR and the
Textus Semantic Integration Engine CAR. Run `scripts/check-cbd-sie-sar.sh` from
the repository checkout to build both CARs and verify four temporary live
JSON-RPC `/mcp` profiles. Exact CBD/SIE counts must be `7/7` at baseline, `0/0`
under global disable, `7/0` under SIE service disable, and `6/6` when both
status operations are disabled. Disabled calls must return `-32602`. CBD
catalog administration, Review start/cancellation, SIE
mutation/administration, the legacy SIE facade, and any other tool are
rejected as unexpected publication.

The baseline profile also runs repository-owned catalog,
development-directory, and BoK fixtures through the live composed endpoint.
The check keeps the catalog and development versions as separate CBD
observations and the BoK term as separate SIE-owned semantic evidence. A
version conflict must name both CBD sources without selecting a winner, result
bounds must preserve those conflict participants, and a missing catalog must
remain degraded without an immediate retry loop.

The same script checks the selected CNCF version against the declared runtime
matrix before building either CAR. `project.yaml` owns the minimum, tested, and
excluded declarations; `docs/spec/runtime-compatibility-matrix.json` owns the
assessed candidate classifications and representative evidence IDs. The
current matrix admits `0.5.1-SNAPSHOT` as its only tested-compatible candidate,
has no excluded version, and treats every unlisted version as unassessed. The
final live marker records the runtime source, revision, and worktree state
rather than treating a mutable SNAPSHOT label as immutable evidence.

The source-managed CAR ABI is `src/main/car/abi-manifest.json`. Run
`scripts/check-car-abi.sh` to require its ten operation signatures to match
generated CML model metadata and the packaged CAR. The same check proves that a
minor operation addition is compatible, a minor removal is rejected, and an
intentional major transition retains the breaking finding as a permitted
decision. No historical Textus CBD Support release exists, so the current
first-release baseline remains explicitly pending instead of being fabricated.

## Source Precedence

The five source kinds preserve separate authority. Published catalogs own the
component facts they publish; BoK sites and SIE own their semantic evidence;
development directories own current working state; and CAR storage owns local
artifact availability. The `purpose` filter returns these authority tiers:

| purpose | authority order |
|---|---|
| `development-work` | working, local/cache artifact, published comparison |
| `local-execution` | local-published, cached, supporting working/published identity |
| `published-reuse` | published catalog, local comparison evidence |
| `artifact-verification` | peer checksum evidence with no winner |

Precedence never selects `selectedObservation`, merges fields, or resolves a
conflict. Catalog `priority` controls deterministic ordering only. Exact detail
operations require one catalog candidate or return bounded alternatives.

## CbdRetrieval Operations

### searchComponents

Requires `requirement`. Optional identity, version, source, freshness,
availability, conflict, purpose, and limit fields constrain results. `results`
contains only catalog-backed component profiles. `observations` also preserves
matching development-directory and local/cache CAR evidence without
fabricating a profile. `issues` cites conflicting sources, `precedence`
explains purpose-specific authority, and `selectedObservation` remains absent
because retrieval does not select a hidden winner. A runtime constraint still
requires affirmative catalog runtime evidence. `semanticEvidence` separately
returns matching BoK-site and current-query SIE terms with source, rationale,
freshness, and evidence URI. A catalog result uses `semanticEvidenceIds` only
for explicitly equal published terms/tags; semantic fields never complete the
component profile.

### getComponent

Requires exact `name`; optional organization, kind, version, and catalog ID
disambiguate. An explicit version returns only its version-specific detail; a
listed version without detail clears version-sensitive fields and adds a
warning. Zero candidates return status `no-match` with a
`component-not-found` absence. Multiple exact catalog candidates return status
`ambiguous` with an `ambiguous-selection` absence, the full `candidateCount`,
and at most 20 references in `alternatives`; no profile is selected until
`catalogId` or another identity constraint leaves one candidate.

### getUsage

Accepts the component identity fields, including optional CAR/SAR `kind`, and
an optional bounded `intent`.
Returns the selected profile, service/operation summaries from model metadata,
and authoritative catalog, artifact, model-metadata, and documentation links.
The response identifies `selectedSourceId`, `selectedSourceKind`, and
`selectedVersion`. Guidance records cite that source/version and their evidence
URIs. `observed-fact` records report selection evidence;
`deterministic-inference` records recommend only observed operations with an
explicit intent-token overlap. `model-inference` is reserved for generative
advice and is not emitted by the current runtime. No token overlap produces no
operation recommendation. Missing source attribution withholds guidance, and
an unavailable sidecar yields a warning and the remaining references.
Selection ambiguity uses the same bounded alternatives contract as
`getComponent`. Missing operation evidence, rejected or unmatched intent, and
missing source/version attribution are returned as stable `absences` rather
than synthetic guidance.

### resolveDependencies

Accepts the component identity fields, including optional CAR/SAR `kind`, and
an optional `maxDepth` from 1 through 32 (default 8). The compatible
`dependencies` field remains the selected root's direct published evidence.
`resolutions` adds bounded transitive paths resolved only inside the selected
catalog. Each path is `resolved`, `unresolved`, `ambiguous`, or `cycle`.
`conflicts` reports distinct explicit versions requested for the same
dependency together with their evidence paths; CBD Support never chooses a
winner. `selectedVersion` and `dependencyMetadataVersion` are independent.
When an explicit requested version differs from the dependency metadata
version, the resolved edge remains visible but traversal stops; a root mismatch
also withholds the compatible direct `dependencies` field. Missing dependency
data is an empty evidence set, not an assertion of no dependencies.
An exact-selection conflict returns bounded catalog alternatives without
starting traversal. When the selected version has no published dependency
metadata, the empty dependency fields include `dependency-metadata-absent`;
an authoritative empty array requires matching `dependencyMetadataVersion`.

### listCatalogs

Returns authorized information-source identity, base URI or path, kind, priority,
readiness, observation count, freshness, latest and next refresh-attempt times,
and diagnostics for catalog, BoK, SIE, development-directory, local warehouse,
and cache inputs. A successfully inspected local source uses `observed` freshness;
its diagnostics make that source degraded. The response-level `warnings`
field also reports rejected remote and local configuration. The operation name
remains `listCatalogs` for Phase 2 compatibility.

### status

Returns aggregate state and counts across catalog, BoK-site, SIE, development,
local warehouse, and cache inputs. Local inspection is bounded, read-only, and
non-cached.

### getReviewRun

Requires one CBD-owned `reviewId`. The `viewer`, `reviewer`, `operator`, and
`admin` roles may read the bounded Run projection. It includes the exact CNCF
Job ID binding, digest-bound target, profile, state, safe limitations,
timestamps, and a report identity only after canonical completion. The query
refreshes state from the bound CNCF Job and repeats authorization at that Job
boundary. It does not expose arbitrary Job history, provider payloads, source
content, credentials, or host paths.

## CbdCatalogAdmin Operation

### refreshCatalog

Refreshes one source ID or every enabled source. A failed refresh records the
failure and preserves any previous snapshot. The operation is excluded from
MCP because it changes runtime state and may cause external traffic.

Snapshots have a default finite TTL of 15 minutes and the runtime cache policy
rejects non-positive lifetimes or lifetimes over 24 hours. Retrieval readiness
reuses retained state before the next scheduled attempt and refreshes an
unattempted source or a source whose normal schedule is due. If automatic
refresh fails, the stale last-known-good snapshot stays available and the
source becomes `degraded`. The same lifetime, schedule, and stale
last-known-good rule applies to BoK snapshots; `refreshCatalog` itself remains
catalog-only administration.

Catalog and BoK normal refresh intervals are explicit, default to 15 minutes,
must be from one minute through 24 hours, and cannot be later than source
expiry. `nextRefreshAttemptAt` is the earliest readiness-driven attempt;
readiness before it performs no source request. Explicit catalog administration
bypasses that schedule. A failed attempt retries after the configured initial
interval, one minute by default, and doubles the delay after each consecutive
failure up to the configured maximum; success resets the sequence. Same-source
callers join one flight, and a fair runtime-wide limit admits two distinct
refreshes by default. Administration uses these same concurrency bounds.

The runtime admits at most 64 configured information sources and retains only
the latest snapshot for each source. Total retained observations are bounded
to 20,000 Catalog profiles, 20,000 BoK terms, 800 SIE terms, and 512 local
observations. Each total is divided into stable source-priority/source-ID
quotas. Refresh replaces only the owning source's bounded quota, reports
truncation, and never evicts another source's evidence. Failed Catalog and BoK
refreshes therefore preserve an already-bounded attributable last-known-good
snapshot. Adapter response-byte, resource, query, directory, depth, and
artifact limits separately bound the work that creates a candidate snapshot.

Authentication, transport, JSON parse, and source-contract compatibility
failures share one observable transition: the attempt and bounded retry time
advance, while the retained observation and expiry do not. Expired evidence is
reported as `degraded`/`stale`, never current. Calls before the retry boundary
perform no source request. Only a successful retry replaces evidence and
observation time, clears the failure diagnostic, and returns the source to
`ready`/`fresh` on its normal schedule.

## CbdReviewAdmin Operations

### startReview

Requires `targetKind`, `name`, a `sha256:` target digest, and Review `profile`;
organization and version are optional identity evidence. The `reviewer`,
`operator`, and `admin` roles may start a Review. CBD Support creates the
Review ID, admits the typed Run, submits one persistent asynchronous CNCF Job,
and returns the stable Review-to-Job binding. This command is private to MCP;
Web, CLI, `sbt-cozy`, and internal callers use the component command surface.
A Job that settles without a canonical report is represented as failed with
`review-report-missing`, never as an empty successful Review.

### cancelReview

Requires one Review ID. Only `operator` and `admin` may cancel. The application
records `cancelling` only after the CNCF Job control boundary accepts the
request; a later `getReviewRun` projects terminal `cancelled`. Terminal Runs
remain immutable and a failed or cancelled Run never fabricates a report.
This command is private to MCP.

### post (private Review submission HTTP gateway)

The generated HTTP gateway accepts a bounded Review provider-document
submission at:

```text
POST /rest/v1/cbd-support/cbd-review-admin/post
Content-Type: application/json
```

The generated outer request is
`{"submissionDocument":"<provider-document-submission JSON>"}`. The inner
string uses `textus.cbd.review-submission.v1` and must contain only the Review
and Target-bound provider descriptors, provider requests, and evidence bundles.
CBD returns `{"canonicalResponse":"<canonical-review-response JSON>"}`;
the inner response is CBD-owned and is the only report/gate result the caller
may use. The role must be `reviewer`, `operator`, or `admin`. This operation is
private to MCP. A local CLI adapter uses the identical inner contract through
stdin; its standalone command is documented when the CBD Support CLI is added.

## Catalog Contract

CBD Support first attempts the Cozy repository contract for each kind:

```text
metadata/repository/car/index.json
metadata/repository/sar/index.json
```

It follows `sidecars.model_metadata_json` and preserves the selected version's
channel, status, component, publication time, artifact path and SHA-256, plus
`runtime.cncf.minimum`, `maximum`, and `tested`. Runtime filtering treats the
minimum/maximum range as inclusive; `tested` remains supporting evidence rather
than an exclusive allowlist. ABI dependencies under
`abi_manifest.abi.dependencies` are retained. Missing or JSON `null` archive
metadata is distinguished from an authoritative empty dependency array.
Repository diagnostics are returned as source warnings. A catalog remains useful
when one kind index is absent and the other succeeds; the absent side becomes
a source warning. Model metadata is fetched only from the same origin as the
catalog evidence. A cross-origin sidecar URI remains visible as an unfetched
reference and produces a warning.

When those indexes are unavailable, the provider can consume the deployed
simplemodeling.org publication catalog at `en/catalog/index.html` and follow
its `cozy.publish-project.v1` project and repository-artifact JSON. This
compatibility contract supplies component identity, release version, CAR/SAR
artifact, and documentation evidence. It does not claim operation,
dependency, or runtime compatibility facts that the publication catalog does
not expose. Unreadable component entries degrade the source without hiding
successfully loaded entries. Snapshot project versions are never labeled as
`latestStable`.

Availability fallback is not parser fallback. If a rich index endpoint returns
invalid JSON, an invalid envelope/entry, or an unknown declared schema, the
source is incompatible and the publication adapter is not attempted. A valid
index for one kind may coexist with an unavailable other-kind index, which is
reported as a warning.

As verified on 2026-07-14, the default public compatibility catalog and its
repository-artifact metadata are available, while the rich CAR/SAR index
endpoints are not publicly accessible. Generating and deploying those indexes
and same-origin model-metadata sidecars is publisher-owned future work, not a
fallback responsibility of this CAR. The tracked candidate is
`FUTURE-CATALOG-PUBLISHER-01`; it requires both rich indexes to return valid
JSON, real CAR/SAR evidence and same-origin model metadata to be published, and
the compatibility fallback to remain verified until an explicit migration.

## Input Compatibility

| Boundary | Accepted older input | Incompatible input behavior |
|---|---|---|
| Catalog | deployed `cozy.publish-project.v1` and observed unversioned publication JSON, only after rich endpoint unavailability | reject malformed/unknown-schema rich input without publication fallback |
| BoK | none; only `cncf.knowledge-source.v1` is supported | reject schema, identity, kind, or resource-contract mismatch without page/path guessing |
| SIE | none; only the public typed `searchTerms` result is supported | reject legacy facade/camelCase, partial, malformed, or evidence-free results |
| Local CAR | valid component descriptor without `version`; retain the path version as `repository-path` evidence | reject missing/malformed descriptor, missing identity, or descriptor/path conflict without choosing a side |

Supported older evidence keeps its original label and authority. It is not
rewritten into the current shape. The normative decision table is
`docs/spec/input-compatibility-governance.md`.

## Failure and Limitation Semantics

- All enabled sources failing initial load causes retrieval operations to fail.
- Refresh failure with an existing snapshot returns degraded state and keeps
  serving the old snapshot.
- Cache expiry is inclusive: a snapshot is stale at `expiresAt`. Normal
  readiness-driven attempts follow `nextRefreshAttemptAt`, which cannot be
  later than that expiry.
- Optional missing fields produce warnings where the contract can continue.
- Credential failures retain only a stable `source-credential-*` code and safe
  source posture; no configuration key, credential, header, or challenge body
  is exposed.
- Incompatible Catalog, BoK, SIE, or local CAR input is rejected at its owning
  boundary and never repaired through field translation, path guessing, or an
  unrelated fallback parser.
- Catalog profile matching remains deterministic metadata matching and is not
  a claim of semantic compatibility. BoK/SIE citations remain separate
  `semanticEvidence`.
- CBD Support does not install components, choose a winner for a transitive
  version conflict, or validate a target SAR composition.

## Example Requests

Search input:

```json
{"requirement":"account authentication","kind":"car","runtimeVersion":"0.5.1","limit":5}
```

Exact usage input:

```json
{"name":"textus-user-account","kind":"car","catalogId":"simplemodeling"}
```

The returned `ComponentReference` can be retained as evidence while the
detailed `ComponentProfile`, operations, dependencies, and references are used
for implementation planning.
