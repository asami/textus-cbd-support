# P5-50 Reusable Capability Catalog

status=complete
phase=5
checklist=P5-50
updated_at=2026-07-16

## Decision

CBD owns one reusable capability catalog spanning Security, Domain,
Documentation, AI Readiness, Resilience, Testability, and Observability. The
catalog projects the canonical Report's assessment and observation mappings;
it does not invoke, select, or rerun a provider. Each capability projection
retains original Evidence and Observation identities.

## Evidence

`CarReviewCapabilityCatalogSpec` proves representative Domain, Documentation,
and runtime Observability capability projections retain Report IDs and the
original Cozy Evidence/Observation relationships. `CarReviewViewProjection`
provides the corresponding CNCF and implementation views over the same IDs.
