# Ineffective Prevention / Pseudo Guarantee review

**Date:** 2026-10-22
**Status:** review-design decision

## Context

A CNCF cache-capacity implementation introduced reservation files, repeated
filesystem scans, and capacity bookkeeping intended to reduce disk-full risk.
The mechanism did not reserve physical filesystem capacity. Disk-full remained
possible, IOException handling remained required, and incomplete-output cleanup
remained required.

An initial reaction of replacing the implementation with a better reservation
abstraction was also judged excessive: the requirement did not justify a
reservation mechanism in the first place.

## Decision

Extend CBD Support's existing speculative-defensive-machinery review with two
explicit concepts:

- **Ineffective Prevention**: prevention machinery adds non-trivial cost while
  leaving essentially the same failure responsibilities in place.
- **Pseudo Guarantee**: a mechanism implies a guarantee that its authority
  boundary cannot actually establish.

The primary review question is:

> What failure handling or responsibility becomes unnecessary because this
> mechanism exists?

Review should compare Cost, Benefit, Guarantee, Residual Responsibility, and a
simpler baseline.

## Enforcement boundary

Do not solve this by growing Skills or AI directives. The observed behavior is
treated as a recurring generation tendency that literal rules may merely
redirect into another mechanism.

- Cozy CAR lint: cheap deterministic structural signals and source locations.
- CBD Support: semantic judgment, proportionality, and Human-in-the-loop
  disposition.
- AI review: bounded use only after deterministic signals or at an appropriate
  review boundary.

This extends the 2026-10-03 AI-generated business application lint candidate
work rather than creating a technique blacklist.

See [Ineffective Prevention and Pseudo Guarantee Review](../../notes/ineffective-prevention-and-pseudo-guarantee-review.md).
