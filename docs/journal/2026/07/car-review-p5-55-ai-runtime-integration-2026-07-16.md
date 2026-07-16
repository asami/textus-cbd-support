# P5-55 Textus AI Runtime Integration

status=completed
phase=5
checklist=P5-55
updated_at=2026-07-16

CBD consumes only the CNCF `AiRunner` protocol. The paired Textus AI
deterministic CAR Review fixture supplies a schema-valid `medium` severity and
safe purpose plus input/output-digest provenance; it remains a test fixture,
not a CBD binary dependency.

CBD's `CarReviewAiRunnerAdapter` admits only declared normalized execution,
usage, and limitation keys. Digest values, counters, identifiers, and purpose
must be bounded and valid; raw provider metadata and response content are not
retained. `TextusAiCarReviewProviderRunnerSpec` proves malformed output becomes
an attributable failed provider/Unknown through the coordinator and never
falls back to a different provider.

`CarReviewAssessmentGateBuilder` excludes `ai.advisory.*` Findings from the
deterministic assessment and gate while retaining a visible corroboration gap.
AI therefore cannot suppress, replace, downgrade, or independently block a
deterministic Review decision.
