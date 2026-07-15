# MCP Default Component Version View

Status: exploratory requirement; not yet an authoritative behavior contract

## Context

CBD Support combines these component-information sources:

- the component collection managed by `https://www.simplemodeling.org/` as the
  built-in published source;
- explicitly registered development directories as working-state evidence;
- the local CAR warehouse and managed CAR cache as additional availability
  evidence; and
- explicitly configured additional catalogs, BoK sites, and SIE-mediated BoK
  routes when needed.

One component may have several published, working, locally published, and
cached versions. The MCP retrieval surface must retain those source and version
identities without merging their evidence.

## Requested Default View

When a component request does not specify a version, the default user-facing
view should provide at most these two primary observations when available:

1. the registered development-directory observation;
2. the latest released version from the built-in simplemodeling.org catalog.

The default selection order is development version first, then latest release
when no development observation exists. Both observations remain visible when
both exist so that working state can be compared with released state.

An explicit version request should retrieve that exact observed version rather
than applying the default order. Other local, cached, historical release, and
snapshot versions remain queryable through explicit version/source filters and
must not disappear from the underlying evidence inventory.

## Current Runtime Difference

The current `searchComponents` contract defaults to `published-reuse`, returns
all admitted published/development/local/cache observations, and leaves
`selectedObservation` empty. Supplying `purpose=development-work` changes the
reported authority tiers but still does not select or reorder an observation.

The requested default therefore requires a normative change to the existing
source-aware retrieval and observation-reconciliation contracts. It must not be
described as implemented until static specifications, executable
specifications, MCP projection, and live behavior agree.

## Decisions Still Required Before Promotion

- whether more than one registered development directory for the same component
  is an ambiguity or has an explicit deterministic selector;
- how duplicate published identities from additional catalogs interact with
  the built-in simplemodeling.org latest release;
- whether `getComponent`, `getUsage`, and `resolveDependencies` expose the same
  default two-version view or remain exact catalog operations; and
- how the default view reports local/cache versions without treating them as
  the selected development or release observation.
