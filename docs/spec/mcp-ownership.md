# MCP Ownership

## SIE MCP

SIE publishes knowledge managed by SIE:

- semantic query and explanation;
- BoK term search and explanation;
- CAR/SAR existence discovery as `ComponentReference`.

SIE does not publish detailed CBD component usage guidance.

## CBD Support MCP

CBD Support publishes component-development knowledge obtained directly from
the default simplemodeling.org catalog and additional configured catalogs:

- component search and exact lookup;
- published versions and runtime requirements;
- dependencies;
- services and operations from model metadata;
- artifact, catalog, documentation, and model-metadata references.

## Publication Policy

CNCF projects declare MCP readiness at service or operation granularity. The
default is deny. CAR/SAR runtime configuration may only narrow the declaration;
it cannot publish an operation the component did not mark ready.

`CbdRetrieval` is ready as a read-only service. `CbdCatalogAdmin` is not ready.
SIE marks selected `SemanticRetrieval` query operations ready and keeps source
registration, indexing, rebuild, provider administration, and its legacy MCP
facade private.

The ownership split is between component tool sets, not a requirement for two
network endpoints. If a SAR contains both CARs as main components, CNCF
projects both selected tool sets through that SAR's single `/mcp` endpoint.
