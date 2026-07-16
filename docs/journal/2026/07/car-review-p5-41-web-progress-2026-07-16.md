# P5-41 Review Web Progress

status=in-progress
phase=5
checklist=P5-41
updated_at=2026-07-16

## Decision

The first CBD Review Web surface is a protected static form for the existing
read-only `CbdRetrieval.getReviewRun` operation. It accepts one Review ID and
renders only the typed Review Run projection: Review/Job identity, target and
profile, lifecycle state/timestamps, provider-attributed limitations, and
completed Report ID/digest. It cannot start/cancel a Run, fetch arbitrary
filesystem evidence, or create a new quality conclusion.

The descriptor keeps this form under `form:` rather than under the unrelated
`admin:` entity configuration. A sbt-cozy form-generator fix now inserts
CML-generated form blocks before later top-level descriptor sections, so
future generated Review forms keep the same structure.

## Evidence

- `ComponentFactorySpec` proves the protected `get-review-run` form belongs to
  CBD Retrieval, remains in the form section, and does not replace unrelated
  operation ownership.
- `CozyWebDescriptorSyncSpec` proves sbt-cozy places a generated form block
  before a following top-level `admin:` section.

## Remaining Work

Add the completed canonical report overview and execute a live authenticated
progress scenario before checking P5-41.
