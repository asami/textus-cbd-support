# Catalog Cache and Refresh Observation Contract

## Purpose

CBD Support bounds the age of a catalog snapshot without discarding the last
known good evidence when its source becomes unavailable. Source state exposes
the observations needed to distinguish freshness, expiry, and refresh failure.

## Lifetime

- The production default snapshot TTL is 15 minutes.
- A cache policy must be positive and no greater than 24 hours.
- `expiresAt` is `refreshedAt + TTL`.
- A snapshot is fresh strictly before `expiresAt` and stale at or after it.
- Retrieval readiness reuses fresh snapshots without source traffic.
- Retrieval readiness attempts to load every enabled source whose snapshot is
  missing or stale.
- The administrative refresh operation ignores freshness and explicitly
  attempts the selected enabled source or every enabled source.

## Last-Known-Good Behavior

A successful refresh replaces the source snapshot, updates `refreshedAt`, and
clears its recorded failure. A failed refresh records the failure but does not
delete an existing snapshot. Retrieval can therefore continue with stale
evidence and reports the source as degraded. If all initial loads fail and no
snapshot exists, readiness fails.

## Source Observations

Each source state returns:

- `status`: `ready`, `degraded`, `not-started`, or `disabled`;
- `cacheStatus`: `fresh`, `stale`, `empty`, or `disabled`;
- `refreshedAt`: time of the last successful snapshot replacement;
- `expiresAt`: freshness boundary for that snapshot;
- `lastRefreshAttemptAt`: time of the latest successful or failed attempt;
- `warning`: provider warning or latest refresh failure.

A stale snapshot is degraded even before a subsequent retrieval initiates its
automatic refresh. An empty source with a failed initial attempt is degraded;
an empty source with no attempt is not started.
