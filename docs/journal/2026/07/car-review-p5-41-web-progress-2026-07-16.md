# P5-41 Review Web Progress

status=complete
phase=5
checklist=P5-41
updated_at=2026-07-16

## Decision

CBD Review Web forms are protected, static projections for the read-only
Retrieval operations. `getReviewRun` accepts one Review ID and renders only the
typed Review Run projection: Review/Job identity, target and profile, lifecycle
state/timestamps, limitations, and completed Report ID/digest. Exact Report-ID
forms then render the canonical summary, report/provider state, Findings,
Assurances, and cross views. They cannot start/cancel a Run, fetch arbitrary
filesystem evidence, or create a new quality conclusion.

The descriptor keeps this form under `form:` rather than under the unrelated
`admin:` entity configuration. A sbt-cozy form-generator fix now inserts
CML-generated form blocks before later top-level descriptor sections, so
future generated Review forms keep the same structure.

## Evidence

- `ComponentFactorySpec` proves all protected Review forms belong to CBD
  Retrieval, remain in the form section, expose report retention/readback, and
  do not replace unrelated operation ownership.
- `CarReviewRunApplicationSpec` proves the authorized CBD lifecycle transitions
  that the `getReviewRun` form projects, including queue/running/completed and
  canonical Report binding.
- `CozyWebDescriptorSyncSpec` proves sbt-cozy places a generated form block
  before a following top-level `admin:` section.
