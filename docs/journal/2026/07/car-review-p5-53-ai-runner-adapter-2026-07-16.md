# P5-53 AI Runner Adapter

status=completed
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
Evidence records.

`TextusAiCarReviewProviderRunner` now supplies the provider-side bridge. Its
configured profile fixes provider/rule-set identity, one approved purpose, and
bounded admitted Evidence. It emits a strict v1 descriptor for
`ai.semantic-review`, calls the adapter, and converts only a schema-valid
structured candidate into an admitted provider bundle. The bundle exposes a
candidate digest and allowlisted execution facts, never raw model output,
credentials, paths, provider endpoints, or provider-specific response bodies.
Candidate Findings are low-confidence advisory observations with the
`ai.advisory.*` rule namespace and an `ai-advisory-only` limitation; this
runner can add no Assurance and does not alter deterministic findings.

`TextusAiCarReviewProviderRunnerSpec` proves descriptor registration, registry
and coordinator execution, v1 bundle admission, reconciliation, malformed
structured-output refusal, cancellation refusal, and no provider fallback.
The currently developing Textus AI fixture uses the unsupported `warning`
severity token; the bridge refuses it as `ai-structured-output-invalid` rather
than silently translating it. Aligning actual Textus AI output/provenance,
timeouts, retries, and cancellation evidence is intentionally retained as
P5-55.
