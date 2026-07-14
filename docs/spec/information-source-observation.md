# Information Source Observation Contract

## Stable Semantics

CBD Support represents every input through an information-source descriptor.
The descriptor fields are:

- `id`: stable identity within the configured runtime;
- `sourceKind`: one of `published-catalog`, `bok-site`, `sie-bok`,
  `development-directory`, or `car-storage`;
- `location`: sanitized URI, component route, or canonical filesystem root;
- `priority`: deterministic ordering input, not a declaration that one source
  may overwrite another;
- `enabled`: whether the source participates in retrieval;
- `authorization`: the policy that admitted the source;
- `authenticationScheme`: the configured remote authentication scheme, or
  `none`;
- `credentialConfigured`: whether the remote source has an admitted credential
  reference, without exposing that reference.

The built-in simplemodeling.org source is a `published-catalog` authorized as
`built-in`. Additional Phase 2 catalogs remain `published-catalog` sources and
record `exact-origin-allowlist` authorization. Phase 3 BoK, SIE, development,
and CAR-storage adapters use the same descriptor rather than defining
source-specific identity fields.

The descriptor never contains a credential reference or secret. The normative
Phase 4 binding and projection rules are defined in `source-authentication.md`.

Source state separates operational status from freshness. Freshness records
the cache state, observation time, expiry, and last refresh attempt. Source
diagnostics are a collection even while the Phase 2 compatibility field
`warning` remains available.

Every runtime-managed component profile includes one `ComponentObservation`
with:

- source identity and source kind;
- evidence location;
- selected or otherwise effective version identity;
- freshness and observation/expiry times;
- artifact SHA-256 when published by the source;
- source and profile diagnostics.

The observation retains immutable source identity, source kind, observation
time, and expiry from the snapshot that supplied the profile. A later refresh
must not attach its freshness or diagnostics to a retained older profile. If a
profile was not loaded through a source snapshot, observation is explicitly
absent rather than defaulting its source kind. Fields from another source are
not used to complete an observation. Search may return multiple observations.
The Phase 3 reconciliation contract reports duplicate, missing, stale,
incompatible, version-conflicting, and checksum-conflicting evidence without
synthesizing a cross-source profile or selecting a winner. Purpose-specific
precedence describes authority only.

## Compatibility

The Phase 2 `catalogId`, `baseUri`, `cacheStatus`, `warning`, and component
profile fields remain present. The generic source and observation fields are
additive. `ComponentReference` remains the stable SIE/CBD handoff contract.

The complete Phase 3 source-role and purpose-precedence matrix is normative in
`phase-3-source-precedence.md`.

## Executable Evidence

`CatalogRuntimeSpec` verifies the five source-kind values, built-in and
allowlisted authorization descriptors, source/version/freshness/checksum
preservation, snapshot isolation across refresh, and explicit absence for an
unbound profile. `ObservationReconciliationSpec` verifies conflict classes,
purpose-specific precedence, preservation of alternatives, and absence of an
automatic selection.
