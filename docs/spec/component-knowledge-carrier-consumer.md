# Component Knowledge Carrier Consumer Contract

status=partially-implemented
phase=cncf-59.6
updated_at=2026-08-26

## Admission

CBD Support MUST consume a Component knowledge carrier only when all of the
following are true:

- the generated `component-descriptor.json` declares the supported carrier
  schema, canonical logical path, and lowercase SHA-256 digest;
- the exact named carrier bytes match the declaration digest;
- the decoded consumer contract has the supported schema and the same
  Component identity and logical release as the descriptor; and
- for a configured development directory, exactly one generated
  `target/cncf.d/car-runtime-manifest.json` evidence entry names
  `target/cncf.d/<carrier logical path>` and has the digest of those exact
  carrier bytes.

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
carrier/digest/Component/release admission.  A profile with no declaration, an
off-route URI, an off-origin URI, or a failed fetch remains carrier-absent or
carrier-rejected as applicable.  CBD Support MUST NOT fetch, unpack, or infer
the contract from the published CAR artifact.

## Public Detail, Usage, and MCP

Public projections MAY contain only the declared Component/release, carrier
schema/logical path/digest, and resource logical identity, logical path,
kind/role, language/media type, size, digest, authority/stability/origin,
license/disclosure, availability, integrity, authorization, and logical
provenance metadata.  A projection MUST
NOT contain raw resource bytes, source/manual text, physical filesystem paths,
credentials, resolver results, operations, or execution authority.

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
