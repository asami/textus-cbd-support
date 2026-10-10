# CAR lint and AI overchecking review

Date: 2026-10-10
Status: design decision

## Problem

AI-generated implementation repeatedly introduces excessive defensive checks and supporting machinery. Removing these mechanisms from sm-workflow and Cozy has become a substantial engineering cost. Literal prohibition of individual techniques is insufficient because the same instinct reappears through different mechanisms.

## Decision

CBD Support separates two complementary mechanisms.

### CAR lint: cheap deterministic detection

CAR lint remains deterministic, reproducible, and cheap enough for routine Candidate/commit-side diagnostics. It detects only patterns that can be recognized with useful precision without semantic AI review, including known families such as hash/integrity substitutes, obvious read-back verification, repeated observation, duplicate/shadow state, and other mechanically recognizable defensive sequences.

CAR lint MUST NOT become a general AI semantic review and MUST NOT accumulate expensive analysis merely to catch every possible form of overchecking.

### Review: semantic AI-assisted detection

CAR Review is the primary home for semantic detection of excessive defensive machinery. AI providers inspect bounded change/component context and ask whether validation, verification, recovery, synchronization, duplicate state, integrity machinery, or other defensive complexity is actually justified by the domain model, Execution Model, Failure Model, architecture, or explicit requirement.

The key question is not whether a suspicious API or syntax occurs, but whether the mechanism has a legitimate semantic reason to exist.

AI findings remain attributable advisory Review evidence. Human judgment remains the design authority.

## Review modes

Two useful scopes are expected:

1. change/diff-focused review for newly introduced defensive complexity;
2. lower-frequency Component hygiene review capable of finding pre-existing architectural overchecking that a diff-only review would miss.

Review should expose defensive-complexity shape and delta as component-level indicators where practical, rather than collapse them into an opaque quality score.

## Learning loop

The two mechanisms form a feedback loop:

    deterministic CAR lint -> known cheap patterns
    AI Review -> unknown/semantic patterns
    Human judgment -> accepted finding classification
    recurring mechanically detectable pattern -> candidate CAR lint rule
    otherwise -> retained Review knowledge/corpus

Observed removals from sm-workflow, Cozy, and other projects are valuable positive corpus examples. Preserve before/after context and the reason the mechanism was unnecessary where practical.

The objective is for AI Review to discover new classes of overengineering while CAR lint gradually absorbs stable, cheap, high-confidence detections.

## Workflow implication

Routine commit/admission latency must not depend on a heavy AI overchecking scan. sm-workflow may use CAR lint at its normal deterministic gate, while semantic overchecking review runs through the ordinary Review path, explicit hygiene work, or lower-frequency/nightly quality activity.

This avoids the paradox of adding excessive checking to detect excessive checking.
