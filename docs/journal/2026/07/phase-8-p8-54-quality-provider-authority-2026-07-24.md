# Phase 8 P8-54: Quality provider authority boundary

status=completed
phase=Phase 8 P8-54
updated_at=2026-07-24

## Decision

Quality-provider results require a second CBD admission layer after the common
provider-bundle contract. The layer is explicit about authority, finite cost,
runtime Evidence, advisory scope, and redaction, and is connected to the
coordinator overload used for policy-governed execution.

## Result

Deterministic providers cannot submit runtime or advisory Evidence. Runtime
Assurance must cite descriptor-declared `runtime-observation` Evidence. Advisory
providers can emit only `ai.advisory.*` Finding/Unknown observations, so AI
output cannot create an Assurance. Preflight blocks declared cost above the
finite limit before runner invocation.

The common bundle admission now also validates each actual Evidence kind against
the descriptor. Facts with secret, credential, password, API-key,
authorization, raw request/response, endpoint, or URL keys are rejected. All
violations remain attributable provider refusals rather than fallback behavior.

## Evidence

`CarReviewQualityProviderAdmissionSpec` proves finite cost, redaction,
deterministic/runtime/advisory negative cases, positive declared runtime
Evidence, and coordinator enforcement. `TextusAiCarReviewProviderRunnerSpec`
proves an advisory Textus AI bundle is admitted only under advisory authority.
