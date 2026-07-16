# P5-52 Capability Assessment Matrix

status=complete
phase=5
checklist=P5-52
updated_at=2026-07-16

## Decision

CBD defines seven quality capabilities: Security, Domain, Documentation, AI
Readiness, Resilience, Testability, and Observability. Each definition now
records a focused question and the representative evidence kinds that can
support it. `CarReviewCapabilityAssessment` accepts only observations that map
explicitly to a defined capability and delegates the resulting coverage,
maturity, confidence, attribution, and gate calculation to the canonical
assessment builder.

This is a catalogue and assessment contract, not a provider claim: providers
remain responsible for producing admitted evidence and observations.

## Evidence

`CarReviewCapabilityAssessmentSpec` constructs one attributable, mapped
Assurance for each definition and proves that all seven generate established
assessments with full one-subject coverage and provider identity. It also
proves that an unspecified capability ID is rejected. The existing
`CarReviewCapabilityCatalogSpec` continues to prove projection uses canonical
identities without rerunning a provider.
