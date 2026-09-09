# Evidence-Backed Application Component Composition V1

**Date:** 2026-09-09
**Status:** Exploratory working direction; non-normative
**Related development item:** DEV-CBD-001

## Purpose

This note narrows the first implementation boundary for CBD Support's
application-component composition direction. It is a planning aid for Phase 9;
it does not alter the existing catalog, retrieval, CML, provider, MCP, or
application contracts.

## Confirmed constraints

- Existing CAR and SAR identity, version, operation, dependency, runtime, and
  source claims remain attributable CBD evidence.
- Semantic terminology evidence remains independent from a component profile.
- Deterministic inference, model inference, a proposal, and a human decision
  are distinct statement kinds.
- CBD Support must not select an implicit winner when evidence is conflicting
  or incomplete.
- A composition plan does not generate, assemble, build, publish, or execute
  an application.

## Proposed V1 responsibility boundary

| Concern | V1 responsibility |
| --- | --- |
| Evidence admission and requirement-coverage accounting | CBD Support |
| Intent interpretation, candidate grouping, and missing-component suggestions | Attributable semantic or AI provider |
| Selection of an existing component or promotion of a proposed component | Human decision |

CBD Support deterministically projects each admitted required capability as
`selected`, `alternative`, `gap`, or `unresolved` from explicit evidence and a
recorded decision. A provider may return an attributable candidate or proposal,
but cannot create a catalog fact, replace deterministic evidence, or select a
component. Until an explicit human decision is recorded, a competing candidate
remains an alternative and a new responsibility remains a proposal.

## V1 data and lifecycle boundary

The first contract should bound the following identities and their provenance:

- `ApplicationIntent` and `RequiredCapability`;
- `ComponentEvidence` and exact CAR/SAR or catalog identity;
- `CoverageDisposition`, alternatives, gaps, and unresolved evidence;
- `ComponentProposal` with provider identity and supporting evidence; and
- `HumanDecision` with the selected disposition and rationale.

The V1 plan is a transient, read-only projection. Persistence, approval
history, continuation packages, invalidation, and CML projection storage are
explicitly deferred to DEV-CBD-002. This keeps the first behavior contract
small enough to establish evidence and decision semantics before storage
semantics are chosen.

## Questions retained for Phase 9

1. Which Activity and Rule syntax is stable enough for the CML requirement
   model, and which syntax must remain provisional?
2. What publisher-declared capability vocabulary and evidence are required
   before deterministic coverage can claim a match?
3. Which input decomposition is deterministic, and which requires a versioned
   semantic or AI provider?
4. What bounded representation preserves source spans, confidence, alternatives,
   and human corrections without treating them as catalog facts?
5. Which read-only composition projection can be exposed without turning MCP
   into an execution, selection, or provider-administration surface?

## Promotion rule

No statement in this note is an implementation requirement. A stable V1
boundary must be promoted to `docs/design/` and `docs/spec/`, then receive
executable specifications before Phase 9 can claim implementation progress.
