# Information Input Compatibility Governance

## Decision Rule

CBD Support recognizes an input only when it matches a named contract or an
explicitly documented legacy shape. Availability fallback and compatibility
fallback are separate decisions: an unavailable newer catalog endpoint may use
the deployed publication contract, while an endpoint that returns a malformed
or unsupported document fails as incompatible. No adapter repairs field names,
scrapes a presentation page, guesses an alternate resource, or selects one side
of contradictory identity/version evidence.

## Accepted and Rejected Inputs

| Boundary | Current accepted input | Supported older input | Rejected without fallback |
|---|---|---|---|
| Cozy catalog | Revision-pinned unversioned repository index with an `entries` array and optional `diagnostics` array | Deployed publication metadata using `schema=cozy.publish-project.v1`, plus its already-deployed unversioned JSON shape | Declared unknown schema, invalid JSON/envelope/entry, or an invalid declared publication document type |
| BoK site | `schemaVersion=cncf.knowledge-source.v1`, `kind=bok-site`, matching manifest/source reference, and declared safe JSON resources | None | Missing/unknown schema, contradictory identity/kind, unsafe resource reference, or guessed HTML/glossary path |
| SIE-mediated BoK | Public `SemanticIntegrationEngine.SemanticRetrieval.searchTerms` MCP result with the documented snake_case evidence fields | None; the internal `sie.searchTerms` facade is not a CBD contract | Legacy/camelCase response fields, malformed MCP payload, partial result, or missing evidence URI |
| Local CAR | Readable CAR with a valid `component-descriptor.json` whose component and version agree with repository coordinates | A valid older descriptor that identifies the component but omits `version`; the repository path remains labeled `repository-path` evidence | Missing/malformed descriptor, missing component identity, or descriptor/path component or version conflict |

An accepted legacy input remains labeled by its actual evidence source. It is
not rewritten to look like the current contract. An incompatible input produces
a bounded source diagnostic and no observation synthesized from nearby paths or
alternate formats.

## Catalog Fallback Boundary

The compatibility provider probes the Cozy CAR and SAR repository-index
locations first. Fetch-level unavailability of both locations permits the
explicit deployed-publication adapter. If either location returns a document
that cannot satisfy the Cozy repository-index contract, the complete source is
incompatible and publication fallback is not attempted. A valid index for one
component kind may coexist with an unavailable index for the other kind; the
missing kind remains a warning.

Publication JSON with no `schema` remains supported because that is an observed
deployed predecessor. Once a `schema` is declared it must be
`cozy.publish-project.v1`, and its `type` must match `catalog-project` or
`repository-artifact` at the corresponding endpoint.

## Local CAR Boundary

Repository coordinates are corroborating evidence, not a replacement for a
CAR descriptor. Only the known legacy transition—valid component identity with
an omitted descriptor version—uses the path version. A malformed or missing
descriptor and any name/version contradiction reject the artifact. The local
inventory reports the rejection without choosing descriptor or path as a
winner.

## Executable Evidence

- `CatalogRuntimeSpec` covers current rich input, schema-v1 and unversioned
  publication input, unavailable-endpoint fallback, and incompatible-input
  refusal before publication probing.
- `BokSourceRuntimeSpec` covers the fixed v1 manifest and rejects unsupported
  manifests without HTML or alternate-path discovery.
- `SieBokRuntimeSpec` covers the public typed tool/result and rejects legacy
  response field names without translation.
- `LocalSourceRuntimeSpec` covers the descriptor-version omission transition
  and rejects malformed or coordinate-conflicting CARs without path guessing.
