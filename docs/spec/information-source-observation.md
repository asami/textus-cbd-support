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
- `authorization`: the policy that admitted the source.

The built-in simplemodeling.org source is a `published-catalog` authorized as
`built-in`. Additional Phase 2 catalogs remain `published-catalog` sources and
record `exact-origin-allowlist` authorization. Later Phase 3 adapters must use
the same descriptor rather than defining source-specific identity fields.

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
not used to complete an observation. Search may return multiple observations,
but later reconciliation must report conflicts rather than synthesize a
cross-source profile.

## Compatibility

The Phase 2 `catalogId`, `baseUri`, `cacheStatus`, `warning`, and component
profile fields remain present. The generic source and observation fields are
additive. `ComponentReference` remains the stable SIE/CBD handoff contract and
is not expanded by this slice.

## Executable Evidence

`CatalogRuntimeSpec` verifies the five source-kind values, built-in and
allowlisted authorization descriptors, source/version/freshness/checksum
preservation, snapshot isolation across refresh, and explicit absence for an
unbound profile.
