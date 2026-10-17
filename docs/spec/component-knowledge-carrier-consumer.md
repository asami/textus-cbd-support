# Component Knowledge Carrier Consumer Contract

status=implemented
phase=cncf-59.6
updated_at=2026-10-17

## Admission

CBD Support MUST consume a Component knowledge carrier only when all of the
following are true:

- the generated `component-descriptor.json` declares the supported carrier
  schema, consumer-contract schema, and canonical logical path, admitted by
  the current CNCF carrier validator;
- the named consumer-contract bytes decode as strict UTF-8 and the supported
  consumer-contract schema, with the same
  Component identity and logical release as the descriptor; and
- for a configured development directory, exactly one generated
  `target/cncf.d/car-runtime-manifest.json` evidence entry names
  `target/cncf.d/<carrier logical path>` in a supported
  `cncf.car-development-runtime-manifest.v2` manifest shape. The evidence
  array contains objects with declared string paths; no carrier checksum is
  required or compared.

The carrier has only `carrierSchema`, `consumerContractSchema`, and
`logicalPath`. It has no digest. Admission MUST NOT derive an identity or
control token from consumer bytes, compare snapshots, or replace the removed
digest check with byte equality. Development observations report
`artifactChecksumSha256=None`; a carrier is not an artifact checksum.

CBD Support MUST query only the fixed descriptor, carrier, runtime-manifest,
and `project.yaml` leaves under each configured development root.  It MUST NOT
discover carrier filenames, recursively scan a project, read an undeclared CAR
entry, fall back to a packaged CAR, or retain raw carrier bytes after
admission.

Missing or invalid declared evidence MUST produce a bounded rejected-carrier
diagnostic while preserving the generic local observation.  A descriptor that
does not declare a carrier MUST produce the explicit
`component-knowledge-absent` absence.  A rejected carrier MUST produce
`component-knowledge-rejected` and MUST NOT create a detail profile.

For a selected published catalog profile, CBD Support MAY consume Component
knowledge only when the selected version declares a strict carrier and the
exact same-origin, version-scoped `consumer_contract` URI
`repository/car/<artifact>/<version>/component-knowledge.json`.  CBD MUST
fetch only that URI, impose the metadata byte bound, and apply the same
carrier/schema/Component/release admission.  A profile with no declaration, an
off-route URI, an off-origin URI, or a failed fetch remains carrier-absent or
carrier-rejected as applicable.  CBD Support MUST NOT fetch, unpack, or infer
the contract from the published CAR artifact.

## Public Detail, Usage, and MCP

Public projections MAY contain only the declared Component/release, carrier
schema/logical path, and resource logical identity, logical path,
kind/role, language/media type, size, digest, authority/stability/origin,
license/disclosure, availability, integrity, authorization, and logical
provenance metadata.  A projection MUST
NOT contain raw resource bytes, source/manual text, physical filesystem paths,
credentials, resolver results, operations, or execution authority.

`ComponentKnowledgeDetail` has no `carrierSha256` field. Its local value-only
evidence URI uses explicit Component identity and logical release as separately
escaped path components. Resource digests and provenance fields still belong
to the current CNCF consumer contract and remain descriptive metadata.

An exact selected profile MAY expose carrier-backed `componentKnowledge` in
the existing read-only component and usage MCP responses.  The resource array
is limited to 100 entries; `truncatedResourceCount` makes every omitted entry
observable.  `getUsage` cites only a syntactically valid logical resource URI
whose availability is `available` and authorization is `granted`.  Withheld
references are reported as `component-knowledge-reference-withheld`.

## CAR Review

The metadata provider may evaluate declared resource-integrity and source
disclosure-policy facts.  Manual completeness, Scaladoc, Help discovery, and
BoK publication readiness are reported as Finding or Unknown when their
required content or independent publication evidence is not admitted.  The
provider evidence is marked `metadataOnly=true`; no provider conclusion may
claim to have read a resource it did not receive.
Provider profiles and metadata facts have no carrier-derived `sourceDigest`.
They retain explicit `componentId`, `logicalRelease`, `sourceId`, and
`evidenceLocation`, and all six existing rule diagnostics. The independent
legacy provider-bundle digest protocol is outside this carrier migration.

## Historical correction

The 2026-08-26 DOC-06 acceptance established value-only admission, bounded
projection, exact Component/release selection, and explicit absence/rejection.
Those historical facts remain. Its digest-bound carrier and runtime-evidence
descriptions are superseded by the current CNCF declaration and semantic
admission contract above; they do not authorize restoring carrier hashes.

## Executable Evidence

`ComponentKnowledgeIntegrationSpec`, `ComponentKnowledgeLocalSourceSpec`, and
`ComponentKnowledgeProjectionSpec` prove carrier admission, development
runtime-evidence enforcement, explicit absence/rejection, exact selection,
MCP-safe bounded projection, and usage-reference withholding.
`CatalogRuntimeSpec` proves that a published catalog supplies a carrier only
for its exact selected version-scoped route; a generic or otherwise
noncanonical route is not admitted.
`ComponentKnowledgeCarReviewProviderRunnerSpec` proves deterministic provider
bundle admission and the explicit Unknown boundaries for content and BoK
checks.
