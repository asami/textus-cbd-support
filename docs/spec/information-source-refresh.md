# Information Source Refresh Contract

## Common Rule

Every information-source adapter bounds work before external or local evidence
enters runtime state. A successful observation exposes when it was made. Any
truncation, failed refresh, or rejected request remains diagnostic instead of
being presented as complete evidence.

## Adapter Policies

| Source kind | Work bound | Freshness | Last-known-good policy |
|---|---|---|---|
| `published-catalog` | configured source/origin counts, response bytes, and discovered profiles | finite TTL with `fresh`, `stale`, `empty`, or `disabled` | retain the previous snapshot after refresh failure |
| `bok-site` | source/origin/resource/term counts and manifest/resource bytes | finite TTL with observation, expiry, and latest attempt times | retain the previous snapshot and terms after refresh failure |
| `sie-bok` | source/origin counts, query/category characters, response bytes, and accepted terms | each successful query records its observation time; no expiry is assigned | responses are query scoped and never reused for another query; the latest successful evidence remains only in degraded source state after failure |
| `development-directory` and `car-storage` | configured roots, artifacts, directories, entries, depth, metadata bytes, and artifact bytes | each completed inventory records `observedAt` | no retained cache; each call performs a new bounded, read-only inspection |

## Runtime Retention Boundary

The runtime admits no more than 64 configured sources in total and retains
only one latest successful snapshot per admitted source. Retained observation
totals are capped at 20,000 Catalog profiles, 20,000 BoK terms, 800 SIE BoK
terms, and 512 local component observations. These object-count bounds combine
with each adapter's response-byte, document-count, directory, artifact, depth,
and query limits to bound both retained memory and candidate-snapshot work.

Each observation total is divided into fixed per-source quotas by source
priority and source ID. Remainders go to earlier sources in that stable order.
A successful observation retains its source-owned prefix and reports
truncation; it never evicts another source's evidence. Catalog and BoK refresh
failure leaves the already-bounded last-known-good snapshot unchanged. SIE
returns the same bounded snapshot that it stores as latest diagnostic state,
while local inspection replaces the prior inventory with a newly bounded one.

## Catalog and BoK Expiry

Catalog and BoK TTL values must be positive and no greater than 24 hours. Their
normal refresh intervals are explicit, bounded from one minute through 24
hours, and no later than source expiry. The production default is 15 minutes
for both lifetime and schedule. A snapshot is fresh strictly before
`expiresAt` and stale at or after it. Readiness reuses a fresh snapshot and
attempts refresh only when the source's observable `nextRefreshAttemptAt` is
due. This is demand-triggered scheduled work, not a background thread.

A failed refresh updates the sanitized diagnostic and
`lastRefreshAttemptAt`, but does not replace or delete the prior snapshot. The
source is `degraded`, its freshness is `stale`, and the prior observation time
and expiry remain visible. Initial failure without a snapshot exposes `empty`
freshness and no last-known-good evidence.

Before the first attempt, the next normal attempt is the runtime start time.
Success schedules from the new observation time. Failure schedules from the
latest attempt time using the configured initial retry, one minute by
production default, which doubles after each consecutive failure up to the
policy maximum and never beyond the normal refresh interval. Success resets
that sequence. Disabled, query-scoped SIE, and uncached local inputs have no
scheduled next attempt.

Catalog and BoK refreshes use source-kind-qualified single-flight. Concurrent
followers wait for the leader and consume its resulting state without another
source request. A fair runtime-wide semaphore limits distinct in-flight source
work to the stricter configured Catalog/BoK limit, two by default. This bound
also protects administrative catalog refresh; administration bypasses only the
time schedule. Each readiness call makes at most one attempt per due source and
never runs an internal retry loop.

## Failure-State Transitions

Authentication failures, unavailable transport, invalid response syntax, and
incompatible source contracts enter the same bounded refresh-state transition.
The failed attempt updates `lastRefreshAttemptAt`, records a sanitized source
diagnostic, increments the bounded retry sequence, and schedules
`nextRefreshAttemptAt`. It does not change the retained observation,
`observedAt`, or `expiresAt`.

When prior evidence has expired, that evidence is explicitly `degraded` and
`stale`; its presence never makes the failed attempt appear successful or
current. A readiness call before the retry instant performs no additional
source work. At or after that instant, only a successful provider result
replaces the evidence and observation time, clears the failure, restores
`ready`/`fresh`, and returns scheduling to the normal refresh interval. Initial
failure without retained evidence remains degraded and empty.

## Non-Cached Inputs

SIE retrieval is a live, query-scoped operation. A result for one query is not
returned when a later query fails, even though source state retains the latest
successful observation for diagnosis. Query and category size limits are
checked before transport.

Local inventory has no runtime cache or fallback snapshot. Its `observedAt`
identifies the completed inspection that produced the returned observations.
Changing local evidence requires another bounded inspection and never causes
an older inventory to be labeled current.

## Executable Evidence

`InformationSourceRefreshSpec` and `CatalogRuntimeSpec` verify inclusive
schedule, retry, and concurrency bounds; schedule versus TTL constraints;
observable Catalog/BoK exponential next attempts; same-source single-flight;
distinct-source burst limits; bounded catalog configuration and discovery; BoK
stale last-known-good retention; pre-transport SIE request bounds; and
independent timestamped local inspections. Together with
`BokSourceRuntimeSpec` and `SieBokRuntimeSpec`, they also verify the combined
source-count boundary, fixed Catalog quota allocation, bounded last-known-good
retention, and Catalog, BoK, SIE, and local observation totals. The same refresh
spec also executes authentication, transport, parse, and compatibility
failures through the BoK provider boundary and verifies stale attribution,
bounded retry deferral, and successful recovery.
