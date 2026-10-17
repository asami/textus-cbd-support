# Phase 15: ReviewRequest Provider and External AI Execution Boundary

Status: PLANNED
Planned: 2026-10-18
Related: CAR Review Architecture, Same-Context Review Principle
Consumer: sm-workflow Phase 6.2 / 6.3

## Goal

Separate CBD review knowledge from AI execution so that cbd-support can provide a
structured semantic ReviewRequest without starting a new AI session. Preserve
standalone cbd-support review by routing the same ReviewRequest through the existing
CBD-owned AI runner when standalone execution is explicitly selected.

The primary consumer is sm-workflow. In that route the active Codex worker already
holds implementation context; cbd-support must not silently start another AI and
force reconstruction of that context.

## Contract

Introduce one common ReviewRequest/ReviewSpec boundary containing at minimum:

- stable request/review identity and review kind;
- target/scope and applicable review profile;
- semantic instructions and criteria;
- required evidence/context references without copying the caller's conversation;
- expected typed Review result/finding shape;
- execution intent indicating external/orchestrated versus standalone execution;
- enough attribution to bind the returned Review result to the request.

Prompt text is a rendering of this contract, not the canonical API by itself.

Two routes consume the same review definition:

1. Standalone: ReviewRequest -> CBD AI runner -> Review result.
2. Orchestrated: ReviewRequest -> caller; no CBD AI invocation.

No review-policy duplication is allowed between these routes.

## Same-Context boundary

For orchestrated execution cbd-support owns review semantics, not the worker
conversation. It must not snapshot, reconstruct, persist or replay the Codex
conversation. It returns the bounded ReviewRequest and consumes the typed result.

A new AI session is permitted only when the caller explicitly selects an
independent execution route. Absence of an execution selector must not make the
orchestrated API silently fall back to CBD-owned AI.

## Work

- P15-RR01: extract current semantic review construction into a reusable
  ReviewRequest builder while preserving existing review profiles/findings.
- P15-RR02: expose an Operation/API usable by sm-workflow that returns the
  ReviewRequest without executing AI.
- P15-RR03: adapt standalone review to execute that same request through the
  existing CBD runner.
- P15-RR04: accept typed external Review results and preserve request/finding
  attribution needed by existing Review evidence.
- P15-RR05: executable specifications proving orchestrated mode performs zero
  AI-provider invocation and standalone mode still performs the selected runner.
- P15-RR06: publish the bounded consumer handoff for sm-workflow Phase 6.2.

## Acceptance

A representative CAR semantic review can be prepared once and used through both
routes with equivalent review criteria. The orchestrated route returns a structured
ReviewRequest and makes no AI-provider call. A caller can return a typed review
result bound to that request. Standalone use still completes a review through the
CBD runner. No conversation snapshot/context-reconstruction mechanism is added.

This Phase does not implement sm-workflow worker routing, model escalation, A/B
evaluation, Phase/Final review policy, or a new AI framework. Those remain consumer
or later orchestration responsibilities.
