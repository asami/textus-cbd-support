# CAR Review Persistence Contract v1

status=specified
phase=Phase 8
checklist=P8-40
updated_at=2026-07-23

## Purpose

This contract defines the logical, database-mappable records that preserve CAR
Review attribution after a working tree, CAR artifact, provider, or policy has
changed. It is database-vendor-neutral: an implementation may use a relational
store or another transactional store, but it must preserve the entity keys,
relationships, immutability, and retention effects defined here.

`CarReviewRepository` is the earlier in-memory P5 retention boundary. It is
not a database implementation and does not by itself satisfy this contract.
An adapter may reuse its canonical report and terminal Run validation only when
the persisted records retain the stronger P8 identity and audit constraints.

## Logical Entities

The machine-readable entity model is
`docs/spec/examples/car-review-persistence-model-v1.json`.

| Entity | Primary key | Immutable retained content | Required relationships |
| --- | --- | --- | --- |
| `car-lineage` | `lineageId` | normalized CAR kind, organization, component name, and lineage digest | owns target snapshots |
| `review-target` | `targetId` | lineage, component version or artifact identity, target kind, and target digest | belongs to one lineage; owns Runs |
| `review-run` | `reviewId` | terminal state, profile, target, completion identity, and opaque reuse identity | belongs to one target; completed Run binds one Report and attestation |
| `review-report` | `reportId` | canonical JSON payload, report digest, Run, and target references | belongs to one terminal completed Run |
| `review-attestation` | `attestationId` | canonical attestation payload and digest, Run, Report, and target references | binds exactly the retained Report/Run/target tuple |
| `diagnosis-reuse-identity` | `reuseIdentityId` | opaque key-definition ID and key digest | is referenced by one or more Runs; never stores mutable provider work |
| `review-comparison` | `comparisonId` | baseline/current Report references, lineage, and configuration-compatibility identity | references two retained Report snapshots in one lineage |
| `review-retention-event` | `retentionEventId` | event action, entity identity, safe entity/report/target digests, and effective time | append-only audit for every retained, expired, or deleted payload |

The `lineageId` identifies the comparable CAR family; a component version and
target digest identify one immutable target snapshot inside that family. A
target digest is never overwritten to represent a later working tree or a
rebuilt artifact.

## Immutability and Atomic Retention

All stored Report and attestation payloads are already redacted canonical JSON.
Their IDs and digests are immutable. A completed Run, its Report, and its
attestation are retained in one transaction only when all Run, target, Report,
attestation, profile, and digest references agree. The transaction appends a
`retained` retention event; it does not update a previous immutable snapshot.

A Run may have transient execution state before completion, but its retained
terminal snapshot is immutable. Failed and cancelled snapshots are also
retained attributable outcomes; their reuse and expiry behavior is specified by
P8-43 rather than being silently treated as successful Reports.

Expiry or explicit deletion removes only the payload permitted by the retention
policy. It appends an `expired` or `deleted` retention event that preserves
record type, stable IDs, an entity digest, any applicable Report/target digest,
and effective time without copying canonical Report content, Evidence facts,
credentials, or provider payloads. The event relationship admits every retained
entity except itself, including reuse identities and comparison snapshots. The
lineage and tombstone remain attributable, so a vanished payload cannot be
mistaken for a successful reusable result.

## Reuse and Comparison Boundary

P8-40 reserves `diagnosis-reuse-identity` as a stored identity record only.
P8-41 owns the exact reuse-key input set and invalidation formula; P8-40 must
not infer or recompute that digest from a partial target, profile, or provider
field. P8-42 owns concurrent request coalescing and reuse of a completed
compatible Run.

Likewise, P8-40 stores immutable comparison references only. P8-44 owns the
comparison delta calculation and dashboard history projection. A comparison
cannot cross lineage or configuration compatibility identities, and a renderer
must never construct it from report text.

## Security and Access Boundary

This storage contract does not grant history access. Commands that write,
delete, or expire records remain private to MCP. Authorized bounded history and
MCP enumeration policy are P8-45 work. The database must not retain raw
credentials, filesystem roots, provider request bodies, or unredacted Evidence
facts merely to satisfy an entity relationship.

## Deferred Implementation

This definition intentionally does not choose a database product, migration
framework, table DDL syntax, reuse-key formula, concurrency algorithm,
comparison projection, or history API. Those are P8-41 through P8-45 work.
