# Phase 3: Federated Development Context and AI Ergonomics

Stage Status:
- Current status: IN_PROGRESS
- Current step: P3-01 through P3-05 plus P3-11 through P3-14 are complete; the next slice hardens remote and local source authorization boundaries under P3-20.
- Owner: Textus CBD development
- Update rule: Update after each checklist item obtains reproducible evidence; closure is based only on `phase-3-checklist.md`.

## Purpose

Give generative AI an evidence-bearing view of published knowledge, semantic
BoK knowledge, the developer's current workspace, and locally available CAR
versions. Keep each observation tied to its source so that search, guidance,
and version selection can explain what is published, inferred, under
development, locally published, or merely cached.

## Planned Input Sources

1. **simplemodeling.org** remains the built-in public catalog and default
   published CBD source.
2. **Other BoK sites** are explicitly configured and authorized sources. Their
   machine-readable BoK manifests or catalogs are consumed without scraping
   rendered HTML or silently merging site identities.
3. **SIE-mediated BoK knowledge** is consumed through the evidence-bearing SIE
   reference contract. SIE supplies terminology, intent, and BoK relationships;
   CBD Support remains responsible for component detail and CAR usage evidence.
4. **Development directories** are explicitly configured, read-only workspace
   sources. Project metadata, CML, generated CAR metadata, and other selected
   development evidence are inspected without unrestricted filesystem scans or
   treating working state as published fact.
5. **CAR versions in local/cache storage** are read from the canonical local
   warehouse (`~/.cncf/local`) and managed cache (`~/.cncf/cache`), or explicitly
   configured equivalent roots. Local publication and cached availability are
   distinct states and do not imply remote publication or recommendation.

## Core Source Contract

Every observation retains a stable source identifier, source kind, evidence
location, observed component and version identity, freshness state, and any
diagnostic. The service does not create one synthetic record by silently
combining fields from different sources.

Precedence is purpose-specific rather than a single global winner:

- A configured development directory is authoritative only for that project's
  current working state.
- The local warehouse and managed cache are authoritative only for which CAR
  artifacts and versions are locally present.
- simplemodeling.org and configured BoK/catalog sites are authoritative for
  their own published observations.
- SIE is authoritative for the BoK terminology and semantic relationships it
  exposes, not for CBD artifact detail.

Search may aggregate observations, but exact version or usage guidance must
identify the selected source evidence. Conflicting version, checksum, source,
or freshness observations remain visible; Phase 3 does not silently select a
winner.

## Scope

- A unified descriptor and state model for all five input-source kinds.
- Explicit configuration and authorization for BoK sites and development
  directories.
- Read-only adapters for development project evidence and local/cache CAR
  inventories.
- SIE-mediated BoK retrieval that preserves the established SIE/CBD ownership
  boundary.
- Version reconciliation that distinguishes working, locally published,
  cached, and remotely published states, including snapshot and release
  identities.
- Source, version, freshness, and conflict filters in the read-only CBD/MCP
  surface.
- Requirement matching and intent-aware guidance whose catalog facts,
  semantic evidence, local observations, and model inference are separately
  attributable.
- Source-specific diagnostics, bounded refresh behavior, and executable
  verification.

## Security and Freshness Rules

- Remote sites use exact-origin authorization and sanitized diagnostics.
- Local roots use explicit allowlists, canonical paths, and protections against
  traversal and symlink escape; the service does not perform arbitrary home or
  repository scans.
- Local and cache adapters are read-only and have bounded inspection work.
- SIE is accessed through its public component/MCP/CNCF contract, not by
  reading SIE internal storage.
- Missing, stale, rejected, and incompatible sources remain observable. Any
  last-known-good behavior reports the age and failed refresh rather than
  presenting stale evidence as current.
- Two artifacts with the same component version but different checksums are a
  conflict, not interchangeable evidence.

## Non-Goals

- Installing, updating, deleting, or publishing CARs.
- Writing to configured development directories, local warehouses, or caches.
- Treating cache presence as proof of publication, compatibility, or quality.
- Scraping rendered BoK pages when no supported machine-readable contract is
  available.
- Moving CBD component details into SIE or duplicating SIE's semantic store.
- Automatically resolving source, version, or checksum conflicts.
- Phase 4 authentication, production credential lifecycle, and SAR composition
  governance.

## Planned Implementation Slices

1. Define the unified source, observation, evidence, freshness, and conflict
   model while preserving Phase 2 catalog compatibility.
2. Add bounded, read-only development-directory and local/cache CAR adapters,
   including version and checksum reconciliation.
3. Add configured BoK-site and SIE-mediated BoK adapters with authorization,
   ownership, and failure contracts.
4. Project source-aware search, filtering, version selection, diagnostics, and
   MCP results without exposing administration mutations.
5. Add requirement matching and intent-aware guidance with explicit separation
   of catalog fact, BoK evidence, local evidence, and inference.
6. Complete documentation, executable specifications, full tests, CAR build,
   CAR lint, and representative MCP projection.

## Verification Evidence

- The built-in simplemodeling.org source retains stable identity and records
  `published-catalog` kind with `built-in` authorization.
- The unified descriptor vocabulary fixes the five Phase 3 source kinds and
  projects identity, location, authorization, freshness, and diagnostics while
  preserving the Phase 2 catalog fields.
- Component observations retain their source snapshot time and expiry, selected
  version, evidence location, and artifact checksum. A retained profile is not
  relabeled with a later refresh, and a profile without source context exposes
  observation absence instead of an inferred source kind.
- Cozy `0.3.0-SNAPSHOT` generation and the focused `CatalogRuntimeSpec` plus
  `ComponentFactorySpec` validation passed 24 tests on 2026-07-14. The validation
  uses the current local CNCF `0.5.1-SNAPSHOT` MCP description fallback rather
  than a stale Maven-local artifact.
- Explicit canonical development roots expose bounded `project.yaml` identity
  and version observations as `working` evidence. Stable local/cache source IDs
  cannot be shadowed by configured development sources, and rejected paths
  remain diagnostic.
- Canonical or explicitly authorized local/cache roots inventory CAR artifacts
  separately as `local-published` and `cached` evidence. Descriptor and
  repository-path versions remain distinct, artifacts retain SHA-256 evidence,
  and artifact, directory, entry, depth, metadata, and checksum-read bounds
  produce observable warnings. `LocalSourceRuntimeSpec` passed 7 tests on
  2026-07-14.
- Explicitly configured BoK sites require exact-origin authorization and the
  Cozy `cncf.knowledge-source.v1` manifest at its canonical metadata path.
  Runtime initialization consumes only bounded, manifest-declared JSON glossary
  resources through the CNCF HTTP provider, preserves configured and publisher
  identities separately, rejects traversal/cross-origin/rendered-page inputs,
  and projects BoK readiness and diagnostics into unified source state.
  `BokSourceRuntimeSpec` and `ComponentFactorySpec` passed 15 focused tests on
  2026-07-14.
- Explicitly configured SIE BoK routes require exact-origin authorization and
  the fixed public `/mcp` component route. CBD calls only the typed public
  `SemanticIntegrationEngine.SemanticRetrieval.searchTerms` operation, bounds
  response bytes and result count, and rejects terms without the required
  evidence URI. SIE terminology remains a separate `sie-bok` observation and
  does not populate CBD-owned versions, dependencies, operations, artifacts,
  or usage guidance. `SieBokRuntimeSpec` passed 4 focused tests on 2026-07-14.
- Catalog and local observations now normalize into one source-preserving
  reconciliation shape. Reports enumerate duplicate, missing, stale,
  incompatible, version-conflicting, and checksum-conflicting evidence, cite
  all contributing sources and locations, and expose purpose-specific
  authority tiers while leaving `selectedObservation` absent. Organization,
  kind, and name form the comparison identity, so an unknown organization is
  not attached to a known publisher implicitly. `ObservationReconciliationSpec`
  passed 3 focused tests on 2026-07-14.
- Version availability now has four stable, independent states: `working`,
  `local-published`, `cached`, and `remotely-published`. Catalog normalization
  preserves every declared stable and snapshot identity, while version text,
  channel, status, and catalog declarations produce explicit `release`,
  `snapshot`, `unknown`, or `conflicting` maturity. Reconciliation retains all
  source alternatives and exposes no implicit latest winner.
  `VersionStateReconciliationSpec` passed 3 focused tests, and the combined
  version-state, local-source, and observation-reconciliation validation passed
  13 tests on 2026-07-14.

## Closure Basis

Phase 3 is DONE only when every item in `phase-3-checklist.md` is `[x]` and its
verification evidence is recorded here.
