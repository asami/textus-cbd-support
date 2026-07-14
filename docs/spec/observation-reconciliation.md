# Observation Reconciliation Contract

## Preserved Evidence

Reconciliation accepts normalized catalog and local observations. Each input
retains its source ID and kind, organization, component kind and name, version,
version state, freshness, runtime range, artifact checksum, evidence location,
and diagnostics. Missing fields remain absent. Reconciliation never fills an
observation from another source and returns every original observation.

Component comparison uses organization, component kind, and component name.
An absent organization remains a distinct unknown identity; it is not silently
matched to one of several known organizations. Version and checksum comparisons
occur only inside that identity.

## Issue Classes

The report has six stable issue codes:

- `duplicate`: more than one observation describes the same identity and
  version;
- `missing`: evidence required for the selected purpose is absent;
- `stale`: an observation is retained past its freshness boundary;
- `incompatible`: an observed version or runtime range excludes the request;
- `version-conflict`: one identity has multiple observed versions;
- `checksum-conflict`: one identity and version has multiple observed SHA-256
  values.

Issues cite all contributing source IDs and evidence locations. Duplicate and
conflicting observations remain in the report; no record is discarded as a
loser.

## Purpose-specific Precedence

Precedence is authority guidance rather than a selection algorithm:

- `development-work` puts working-directory state before local artifacts and
  uses published catalogs for comparison;
- `local-execution` distinguishes locally published artifacts from cached
  availability;
- `published-reuse` treats published catalogs as reuse and compatibility
  authority while local state remains comparison evidence;
- `artifact-verification` treats checksums as peer evidence, so disagreement
  has no winner.

Every report sets `selectedObservation` to absent. Later read-only projection
may display precedence and alternatives, but must not convert a tier into an
automatic source or version choice.

## Executable Evidence

`ObservationReconciliationSpec` verifies source-shape normalization, all six
issue classes, organization-aware identity, purpose-specific precedence,
preservation of every alternative, and explicit absence of a selected winner.
