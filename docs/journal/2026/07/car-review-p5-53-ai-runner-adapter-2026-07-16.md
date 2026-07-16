# P5-53 AI Runner Adapter

status=in-progress
phase=5
checklist=P5-53
updated_at=2026-07-16

## Decision

CBD's AI boundary is `CarReviewAiRunnerAdapter`, parameterized only by the
CNCF `AiRunner` SPI. It calls `generateRecord` with an approved stable CAR
Review purpose and an empty tool set. CBD supplies a bounded, deterministic
sequence of already admitted Evidence IDs, subjects, and summaries; it never
passes workspace paths, provider credentials, provider endpoint details, or
raw Evidence facts.

Provider, mode, engine, and model resolution are deliberately delegated to a
Textus AI purpose profile. The adapter exposes only allowlisted normalized
execution, usage, and limitation metadata; provider-specific response content
is discarded.

## Evidence

`CarReviewAiRunnerAdapterSpec` uses a CNCF SPI test runner and proves the
approved purpose, empty tool set, deterministic bounded prompt, safe metadata
filtering, and pre-execution refusal for an unapproved purpose or too many
Evidence records. Current Textus AI Phase 1 `AiRunner` executable tests pass
independently; the runtime is not yet injected into a CBD provider bundle.
