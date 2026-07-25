# CAR Review Evolution View Contract v1

status=stable
phase=Phase 8
checklist=P8-44
updated_at=2026-07-24

## Purpose

The CAR evolution View compares two immutable retained canonical Report
snapshots. It makes changes visible without rerunning a provider, deriving a
conclusion from rendered text, or permitting a caller to enumerate storage.

## Admitted input

The Entity Aggregate adapter supplies exactly two `CarReviewHistoryEntry`
values. Each binds a canonical Report to a `lineageId` and a
`configurationCompatibilityId`. The projection must reject an entry pair when
either ID differs, or when target kind, organization, or component name differs.
Different component versions and target digests within the same CAR lineage are
the expected evolution case.

## Output

`CarReviewEvolutionDelta` preserves both Report IDs/digests, both target
identities, both gate results, and sorted added/removed/unchanged Observation
IDs. It also reports added/removed/changed capability assessment IDs. It never
relabels a Finding, Assurance, Unknown, maturity, or gate.

## Boundary

`CarReviewEvolutionProjection` is a pure View projection. It has no Entity,
DataStore, SQL, SQLite, filesystem, network, provider, repository, or clock
dependency. The future Entity-backed View supplies bounded authorized snapshot
inputs; P8-45 owns its history authorization and retention bounds.
