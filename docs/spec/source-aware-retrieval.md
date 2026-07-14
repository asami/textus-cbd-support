# Source-Aware Retrieval Contract

## Read-Only Projection

`CbdRetrieval.searchComponents` searches published catalog profiles together
with observations from explicitly configured development directories and the
local/cache CAR stores. The operation remains read-only. It does not install,
publish, update, delete, or refresh an administered source explicitly.

The response keeps two projections separate:

- `results` contains only catalog-backed `ComponentMatch` profiles;
- `observations` contains source-preserving catalog, development-directory,
  local-published, and cached evidence.

A local observation can therefore make the response `matched` while `results`
is empty. CBD Support never constructs a catalog profile from working or CAR
storage evidence, and it never fills an absent field from another source.
`issues` cites all contributing source IDs and evidence locations.
`precedence` explains purpose-specific authority but is not an automatic
selection rule. `selectedObservation` remains absent in this phase.

BoK-site and SIE observations remain available as terminology inputs and
source-state diagnostics. They are not converted into component observations
until requirement matching can cite their semantic evidence separately under
P3-30.

## Search Filters

The existing requirement, organization, component kind, version, runtime
version, and result limit filters remain available. Source-aware retrieval adds:

- `sourceId`: exact stable source identity;
- `sourceKind`: `published-catalog`, `development-directory`, or `car-storage`
  for component observations;
- `freshness`: `fresh`, `stale`, or `observed`;
- `versionState`: `working`, `local-published`, `cached`, or
  `remotely-published`;
- `conflictCode`: `duplicate`, `missing`, `stale`, `incompatible`,
  `version-conflict`, or `checksum-conflict`;
- `purpose`: `development-work`, `local-execution`, `published-reuse`, or
  `artifact-verification`.

Identity, source, freshness, and availability filters select observations
before reconciliation. A valid conflict filter retains only the requested
issue class and its participating alternatives. Unsupported finite-vocabulary
filters produce bounded warnings rather than broadening authority. An
unsupported conflict filter produces no observations. An unsupported purpose
uses `published-reuse` and reports that fallback explicitly.

The response limit is clamped to 1 through 100. Reconciliation still examines
all candidates already admitted by the bounded catalog and local adapters, so a
small response limit cannot erase a conflict diagnostic. An issue may therefore
cite a participating source and evidence location whose observation is outside
the requested response limit. Catalog matching retains its evidence-based
requirement behavior. Local matching uses only explicitly observed component
identity, kind, organization, and version tokens.

## Runtime Inputs and State

The default runtime reads local sources from:

- `TEXTUS_CBD_DEVELOPMENT_DIRECTORIES`: comma-separated `[id=]path` entries;
- `TEXTUS_CBD_LOCAL_CAR_ROOT`: optional local warehouse root;
- `TEXTUS_CBD_CACHE_CAR_ROOT`: optional managed-cache root.

Absent CAR-root settings use `~/.cncf/local` and `~/.cncf/cache`. Configuration
still passes the canonical-path, no-follow, ID-collision, and discovery-bound
rules in `local-information-sources.md`.

Retrieval readiness performs a new bounded local inspection and does not cache
an older inventory. `listCatalogs` and `status` include configured development,
local, and cache source state together with catalog, BoK, and SIE source state.
A completed local inspection has `observed` freshness; source-specific
inspection diagnostics make that source `degraded`. Before inspection it is
`not-started`.

## Executable Evidence

`SourceAwareRetrievalSpec` covers mixed catalog/working evidence, source,
freshness, version-state, exact-version, and conflict filters, local-only
matches without fabricated profiles, all configured local source states,
invalid-filter diagnostics, and the absence of an automatic winner.
`ComponentFactorySpec` verifies that the added filters are published in the
read-only MCP input schema.
