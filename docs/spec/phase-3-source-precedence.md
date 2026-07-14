# Phase 3 Source and Precedence Contract

## Source Roles

Phase 3 exposes five stable `sourceKind` values without merging their evidence:

| Input | `sourceKind` | Evidence role |
|---|---|---|
| built-in simplemodeling.org and configured component catalogs | `published-catalog` | remotely published component identity, version, compatibility, artifact, dependency, operation, and documentation facts actually published by that catalog |
| configured machine-readable BoK sites | `bok-site` | site-owned terminology and definitions |
| current-query SIE public MCP results | `sie-bok` | SIE-owned terminology, semantic match, dataset, score, rationale, and evidence URI |
| explicitly configured development directories | `development-directory` | current read-only `working` project evidence |
| local warehouse and managed cache CAR roots | `car-storage` | read-only `local-published` and `cached` artifact availability and checksum evidence |

Local or semantic evidence never completes a catalog profile. Catalog evidence
never becomes SIE-owned terminology. Every observation retains its source ID,
location, version state, freshness, diagnostics, and available checksum.

## Purpose-Specific Precedence

Precedence describes which evidence is authoritative for one purpose. It is not
a selection algorithm and never populates `selectedObservation`:

| Purpose | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|---|
| `development-work` | working development directory | local-published and cached artifacts | remotely published catalog comparison |
| `local-execution` | local-published artifact | cached artifact | working and published identity evidence |
| `published-reuse` | remotely published catalog | working, local-published, and cached comparison evidence | — |
| `artifact-verification` | all catalog, development, and CAR-storage checksums are peers; disagreement has no winner | — | — |

Source configuration priority provides deterministic presentation order only.
Exact component retrieval also refuses a priority winner: ambiguous catalog
candidates remain bounded alternatives until the caller supplies a catalog ID
or stricter identity.

## Read-Only Projection

`searchComponents` keeps catalog-backed `results`, source-preserving
`observations`, conflict `issues`, purpose `precedence`, and BoK/SIE
`semanticEvidence` separate. `getComponent`, `getUsage`, and
`resolveDependencies` read one explicitly selected catalog profile or return
bounded alternatives and explicit absence. `listCatalogs` and `status` project
operational state for all source kinds. No read-only operation installs,
publishes, updates, or deletes a source or CAR.

## Detailed Contracts

Authorization and path safety are defined by
`information-source-security.md`; freshness by
`information-source-refresh.md`; reconciliation by
`observation-reconciliation.md`; version availability by
`version-state-reconciliation.md`; semantic ownership by
`semantic-requirement-matching.md`; and exact retrieval by
`evidence-bounded-exact-retrieval.md`.
