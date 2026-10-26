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

Generation rules have an additional hazard: mentioning a defensive concern can
make that concern more salient to the implementation model. A prohibition such
as "do not use reservation files" may still teach the model that capacity
failure deserves special treatment, causing it to invent preflight checks,
re-checks, private ledgers, synchronization, or another substitute mechanism.
Thus an anti-overengineering rule can itself trigger overengineering.

For this class of recurring model tendency, the preferred control is
asymmetric:

1. keep generation guidance small and avoid incident-specific defensive topics;
2. let the implementation model produce the ordinary solution without an
   artificially expanded checklist;
3. detect suspicious structure after generation with deterministic external
   analysis where possible;
4. use CBD Review for the bounded semantic question of necessity,
   proportionality, guarantee, and residual responsibility.

This is not a claim that generation guidance is never useful. Requirements and
positive architecture contracts still belong in the implementation context.
The caution applies to accumulating negative incident-specific rules intended
to suppress recurring model instincts.

- Cozy CAR lint: cheap deterministic structural signals and source locations.
- CBD Support: semantic judgment, proportionality, and Human-in-the-loop
  disposition.
- AI review: bounded use only after deterministic signals or at an appropriate
  review boundary.

This extends the 2026-10-03 AI-generated business application lint candidate
work rather than creating a technique blacklist.

See [Ineffective Prevention and Pseudo Guarantee Review](../../notes/ineffective-prevention-and-pseudo-guarantee-review.md).
