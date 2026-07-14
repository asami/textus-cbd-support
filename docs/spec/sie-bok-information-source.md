# SIE-mediated BoK Information Source

## Public Boundary

CBD Support consumes SIE BoK terminology only through SIE's public CNCF MCP
contract. A source is configured as `[id=]https://host/mcp` in
`TEXTUS_CBD_SIE_BOK_ROUTES`; its exact scheme, host, and effective port must be
present in `TEXTUS_CBD_SIE_ALLOWED_ORIGINS`. Credentials, query strings,
fragments, non-HTTP(S) schemes, and paths other than `/mcp` are rejected before
network access.

The only operation used by this adapter is:

```text
SemanticIntegrationEngine.SemanticRetrieval.searchTerms
```

The JSON-RPC method is `tools/call`. CBD supplies `query`, an optional
`category`, and a bounded `limit`. Response bytes and accepted result count are
bounded, and query/category character limits are checked before transport. CBD
does not use the legacy internal `sie.searchTerms` facade, SIE
administration or ingestion routes, or SIE's RDF/vector storage.
`CbdRetrieval.searchComponents` triggers the lookup with its requirement. The
current response becomes independent `semanticEvidence`; any returned
component profile remains exclusively a CBD catalog observation.

## Evidence Contract

A response must have `status`, `query`, and a `results` array. Every accepted
result retains these SIE fields without synthesizing absent values:

- `id`, `title`, and `definition`;
- optional `category` plus required `term_type` and `dataset_id`;
- `match_kind`, numeric `score`, and `rationale`;
- an absolute `evidence_uri`.

An MCP error, malformed envelope, non-JSON text payload, missing required
field, invalid evidence URI, or oversized response is a source failure. The
source becomes `degraded`, its sanitized diagnostic remains observable, and no
partial term result is accepted. Transport failures and publisher warnings pass
through the common bounded diagnostic sanitizer before entering unified source
state.

## Ownership

SIE owns terminology, definitions, datasets, semantic match classification,
rationale, and evidence links. CBD Support owns component versions, runtime
compatibility, dependencies, operations, artifacts, manuals, examples, and
reuse guidance. SIE terms are stored as independent `sie-bok` observations;
they are never copied into or used to complete a component profile. A catalog
match may cite an SIE evidence ID only when its published `terms` or `tags`
explicitly names that SIE term ID or title.

SIE responses are query scoped and are not cached for reuse. Failure for a new
query returns no result from an earlier query. Unified source state may retain
the latest successful observation and its age as degraded diagnostic evidence,
but that retained evidence never substitutes for the failed query response.

The SIE `ComponentReference` handoff remains separate. It proves that a CAR or
SAR exists, but detailed component development evidence must be resolved by
CBD Support from its catalog and local information sources.

## Executable Evidence

`SieBokRuntimeSpec` verifies fixed-route authorization, exact-origin rejection,
reserved identities, typed public tool invocation, response bounds, mandatory
evidence, unified source readiness, and the absence of synthetic component
profiles.
`SemanticRequirementMatchingSpec` verifies that only the current query's SIE
response is cited and that SIE match metadata remains SIE-owned.
