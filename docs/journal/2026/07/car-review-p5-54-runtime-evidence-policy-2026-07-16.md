# P5-54 Runtime Evidence Policy

status=complete
phase=5
checklist=P5-54
updated_at=2026-07-16

## Decision

`Operational` is an admission rule, not a provider assertion. CBD accepts it
only when the assessment references `runtime-observation` Evidence whose
provider belongs to the assessment, and an assessment Observation both
references that Evidence and maps to the assessed capability. Normal Report
codec validation already requires Evidence provider/bundle attribution,
resolvable references, bounded facts, bounded messages, and safe locations.

Consequently static-analysis Evidence cannot be relabelled as operational, and
a runtime claim remains inspectable through its Evidence and Observation IDs.

## Evidence

`CarReviewRuntimeEvidencePolicySpec` proves that a static Report with its
maturity changed to `operational` fails with `runtime-evidence-required` before
digest calculation or encoding. It also proves a provider-bound,
capability-mapped runtime observation produces an admissible operational
Report. `CarReviewReportCodecSpec` passes unchanged.
