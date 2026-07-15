# Catalog Cache and Refresh Observation Contract

## Purpose

CBD Support bounds the age of a catalog snapshot without discarding the last
known good evidence when its source becomes unavailable. Source state exposes
the observations needed to distinguish freshness, expiry, and refresh failure.

## Lifetime

- The production default snapshot TTL is 15 minutes.
- A cache policy must be positive and no greater than 24 hours.
- The production refresh interval is explicit, must be from one minute through
  24 hours, and must not exceed the snapshot TTL.
- `expiresAt` is `refreshedAt + TTL`.
- A snapshot is fresh strictly before `expiresAt` and stale at or after it.
- Retrieval readiness reuses retained snapshots without source traffic until
  `nextRefreshAttemptAt` is due.
- Retrieval readiness attempts to load every enabled source before its first
  attempt or when its normal schedule is due; an interval shorter than the TTL
  may refresh a still-fresh snapshot.
- The administrative refresh operation ignores freshness and explicitly
  attempts the selected enabled source or every enabled source.

## Normal Refresh Schedule

The default catalog refresh interval is 15 minutes. Runtime readiness performs
scheduled work on demand; P4-10 does not create a background thread. Before a
source has been attempted, `nextRefreshAttemptAt` is the runtime start time. A
successful refresh schedules the next normal attempt from `refreshedAt`; a
failed refresh schedules it from `lastRefreshAttemptAt` using an exponential
retry delay. A readiness call before that instant reuses retained state and
performs no source request.

Retry starts at the configured initial interval, one minute by production
default, and doubles after each consecutive failure up to the configured
maximum, which cannot exceed the normal refresh interval. A success resets the
failure sequence. One readiness call performs at most one attempt for each due
source; there is no retry loop inside an operation.

Concurrent work is source-kind and source-ID single-flight. Followers wait for
the leader's completed state instead of issuing another request. A fair
runtime-wide semaphore admits from one through eight distinct source refreshes,
with a production default of two; the stricter Catalog or BoK policy controls
the shared runtime boundary. This prevents synchronized due sources from
creating an unbounded request burst.

An explicit administrative refresh ignores `nextRefreshAttemptAt`, but still
uses the same single-flight and runtime-wide concurrency boundary.

Catalog configuration bounds configured sources and authorized origins.
Provider reads additionally bound index and metadata response bytes plus the
number of discovered component profiles. A reached profile bound remains a
source warning; the adapter does not silently claim a complete catalog.

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
- `nextRefreshAttemptAt`: earliest instant for the next normal readiness-driven
  attempt, or absent for a disabled source;
- `warning`: provider warning or latest refresh failure.

A stale snapshot is degraded even before a subsequent retrieval initiates its
automatic refresh. An empty source with a failed initial attempt is degraded;
an empty source with no attempt is not started.
