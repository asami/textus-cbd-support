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

## Catalog and BoK Expiry

Catalog and BoK TTL values must be positive and no greater than 24 hours. The
production default is 15 minutes. A snapshot is fresh strictly before
`expiresAt` and stale at or after it. Readiness reuses a fresh snapshot and
attempts refresh for a missing or stale snapshot.

A failed refresh updates the sanitized diagnostic and
`lastRefreshAttemptAt`, but does not replace or delete the prior snapshot. The
source is `degraded`, its freshness is `stale`, and the prior observation time
and expiry remain visible. Initial failure without a snapshot exposes `empty`
freshness and no last-known-good evidence.

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

`InformationSourceRefreshSpec` verifies bounded catalog configuration and
discovery, BoK TTL and stale last-known-good retention, pre-transport SIE
request bounds, and independent timestamped local inspections.
