# P5-14 Review Run/Report retention and stale-evidence boundary

date=2026-07-16
status=implemented
checklist=P5-14

## Decision

`CarReviewRepository` is the CBD-owned persistence boundary for terminal Review
Runs and canonical Review Reports.  It retains a completed Run and its Report
atomically only when the `reviewId`, complete target attribution, `reportId`,
and `reportDigest` agree.  A retained Run, Report, or Run-to-Report binding
cannot be replaced by a different value under the same identity.

The repository records a finite, explicit retention policy: a positive maximum
age plus positive maximum counts of retained Reports and terminal Runs per exact
target. It does not silently evict a record when either target limit is full.
Expiry and explicit deletion require a caller-supplied execution time. Expiry
removes old Runs, their bindings, and old Reports; deletion removes a Report.
Both append a content-free audit record containing only record type, IDs,
digests, action, and effective time. Authorization for the deletion command
remains at the CBD application boundary; this storage boundary never supplies
authority.

## Gate and baseline consequences

Gate evidence contains the Review ID, full target, Report ID, Report digest,
and gate result.  It is valid only when every field matches a retained immutable
Run/Report binding.  Deleted reports therefore invalidate an old gate evidence
record without mutating the immutable Run.  Baseline comparison requires a
retained baseline and a report with exactly the same target attribution; target
mismatch and an unretained current report are rejected rather than yielding a
comparison that could be mistaken for a gate decision.

## Evidence

`CarReviewRepositorySpec` covers conflicting Report IDs, atomic completed
Run/Report retention, stale evidence after deletion, finite per-target capacity,
expiry, and target-mismatched baseline rejection. The repository does not
consult a JVM clock: expiry receives its effective instant explicitly so CI and
replay remain deterministic.

## Follow-up boundary

Deployment-specific backing-store wiring, authorized command wiring,
Web/CLI/read-model projection, and MCP exposure are later Phase 5 work. In
particular, deletion is private to MCP under the v1 security contract and must
be authorized before it reaches this repository.
