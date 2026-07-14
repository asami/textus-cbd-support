# Phase 1 Checklist: CBD Support Extraction Baseline

This checklist is the authoritative Phase 1 state ledger. Items are never
deleted; a checked item requires observable evidence.

## Project Baseline

- [x] `P1-01` Cozy scaffolds `textus-cbd-support` as a CAR with Scala 3.3.8.
- [x] `P1-02` `project.yaml` is authoritative for project identity, component version, dependencies, and CNCF runtime compatibility.
- [x] `P1-03` `build.sbt` remains a small project.yaml-to-sbt mapping.
- [x] `P1-04` ai/directive is installed with root `AGENT.md` and `RULE.md` links.

## Catalog Runtime

- [x] `P1-10` The default catalog source is simplemodeling.org and additional absolute HTTP(S) catalogs are configurable.
- [x] `P1-11` The provider parses Cozy CAR/SAR indexes, selected versions, runtime minimum, artifacts, sidecars, and dependencies.
- [x] `P1-12` The provider parses service/operation usage from Cozy model metadata.
- [x] `P1-13` Search supports CAR/SAR filters, runtime filters, exact/candidate classification, and Japanese terms.
- [x] `P1-14` Failed refresh preserves the last known good snapshot and reports degraded state.
- [x] `P1-15` Complete initial source failure returns unavailable instead of an empty success.
- [x] `P1-16` The provider auto-detects Cozy repository indexes and the deployed simplemodeling.org `cozy.publish-project.v1` publication catalog without treating a successful compatibility fallback as degraded.

## MCP and Ownership

- [x] `P1-20` CNCF filters `/mcp` tools by component-declared service/operation readiness.
- [x] `P1-21` CAR/SAR runtime configuration can disable MCP globally, by service, or by operation.
- [x] `P1-22` Cozy scaffold can generate a primary MCP-ready service declaration.
- [x] `P1-23` CBD publishes `CbdRetrieval` and keeps `CbdCatalogAdmin` private.
- [x] `P1-24` SIE publishes selected read-only knowledge operations and keeps mutations and the legacy MCP facade private.
- [x] `P1-25` SIE component output is limited to `ComponentReference`; CBD owns detailed profiles, usage, compatibility, dependencies, artifacts, and guidance.
- [x] `P1-26` The shared `ComponentReference` contract is documented and represented in both CML models.

## Documentation and Verification

- [x] `P1-30` README, user guide, reference manual, strategy, phase, and specification documents describe the implemented ownership split.
- [x] `P1-31` CBD unit/component tests cover catalog schema, model metadata, Unicode search, refresh failure, and MCP publication.
- [x] `P1-32` SIE full tests and `cozyBuildCAR` pass after reference-only migration.
- [x] `P1-33` CNCF focused MCP policy tests pass after disable-policy coverage is added.
- [x] `P1-34` Cozy scaffold focused tests pass after MCP-ready scaffold support is added.
- [x] `P1-35` CAR lint reports no blocking findings for CBD Support and SIE.
- [x] `P1-36` Representative CNCF MCP projection proves CBD detail tools and SIE knowledge/reference tools are separately visible.
- [x] `P1-37` Live CNCF `/mcp` calls search, resolve, and return usage evidence for the published SIE CAR from simplemodeling.org.
