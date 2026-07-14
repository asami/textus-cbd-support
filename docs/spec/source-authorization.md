# Catalog Source Authorization Contract

## Purpose

Candidate catalog configuration must not become outbound network authority by
itself. CBD Support accepts an additional source only when its network origin
is explicitly allowlisted, and it makes every rejection observable without
exposing credential-bearing input.

## Built-In Source

`https://www.simplemodeling.org/` with source ID `simplemodeling` is the
built-in default source. It is trusted by the component contract and does not
require an environment allowlist entry.

## Additional Sources

`TEXTUS_CBD_CATALOGS` contains candidate base URIs with optional source IDs.
`TEXTUS_CBD_CATALOG_ALLOWED_ORIGINS` contains permitted origins. An additional
source is accepted only when:

- its source ID contains only ASCII letters, digits, dot, underscore, or
  hyphen;
- its base URI is absolute HTTP(S) with a host;
- its base URI contains no user information, query, or fragment;
- its normalized origin exactly matches an allowlist origin; and
- its source ID has not already been accepted, including the built-in ID.

Origin equality covers lowercase scheme and host plus effective port. Explicit
ports 80 for HTTP and 443 for HTTPS normalize to their default origins. Scheme
or non-default-port differences do not match. An allowlist entry is an origin
and therefore cannot contain a path.

## Rejection and Observation

Rejected candidates are never added to runtime sources and therefore are not
fetched or refreshed. `listCatalogs.warnings` and retrieval-operation warnings
contain deterministic reasons for invalid allowlist entries, invalid candidate
IDs or URIs, missing origin permission, and duplicate IDs. Invalid URI input is
not echoed, so embedded credentials cannot leak through warning text.

Catalog metadata may retain artifact, documentation, or sidecar URIs as
evidence, but `getUsage` fetches model metadata only when its origin matches the
catalog evidence origin. A cross-origin sidecar remains visible as an
unfetched reference and produces a warning; catalog content cannot expand
outbound network authority.
