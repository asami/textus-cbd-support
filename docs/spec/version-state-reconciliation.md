# Version State Reconciliation Contract

## Two Independent Axes

Version state records availability and maturity separately. Availability has
four stable values:

- `working`: version evidence observed in an explicitly configured development
  directory;
- `local-published`: a CAR present in the canonical or explicitly configured
  local warehouse;
- `cached`: a CAR present in the canonical or explicitly configured managed
  cache;
- `remotely-published`: version evidence declared by one published catalog.

Availability does not imply maturity, recommendation, compatibility, or a
global ordering. In particular, a working version may be a release identity and
a cached version is not proof of remote publication.

Maturity has four stable values: `snapshot`, `release`, `unknown`, and
`conflicting`. Snapshot evidence comes from a `-SNAPSHOT` version identity,
snapshot/development channel, snapshot status, or a catalog's declared latest
snapshot. Release evidence comes from a release-form version identity,
stable/release channel, release status, or a catalog's declared latest stable
version. If snapshot and release signals describe the same source observation,
the maturity is `conflicting` and the contradiction remains diagnostic.
Unidentified or non-version values without a maturity signal are `unknown`.

## Source Preservation

Each version-state observation retains source ID and kind, component identity,
version, availability, maturity, catalog channel and status when present,
artifact checksum, evidence location, and diagnostics. Catalog normalization
emits every declared version identity, including distinct latest stable and
latest snapshot identities; it does not reduce the catalog to its preselected
profile version. Local observations retain the state assigned by their
authorized source adapter.

Reconciliation partitions observations by maturity for display and filtering
while preserving the original vector. Duplicate identities in different
availability states remain separate. `selectedObservation` is always absent:
this contract does not choose a latest version, turn precedence into a winner,
or rewrite conflicting evidence.

## Executable Evidence

`VersionStateReconciliationSpec` verifies all four availability states, release
and snapshot catalog identities, local maturity classification, conflicting and
unknown diagnostics, preservation of duplicate alternatives, and explicit
absence of an automatically selected observation.
