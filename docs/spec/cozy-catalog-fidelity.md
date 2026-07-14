# Cozy Catalog Fidelity

## Evidence Baseline

The Phase 2 parser contract is fixed to Cozy revision
`e6673648cb04b9172aeaf1019585845d83259692`. The selected producer evidence is
`src/main/scala/cozy/bok/CozyBok.scala`, especially `RepositoryCarIndex`,
`RepositoryCarEntry`, `RepositoryCarVersion`, and `RepositoryCarDiagnostic`.

The executable fixture
`src/test/resources/catalog/cozy-repository-car-index.json` is a byte-for-byte
capture of that Cozy revision's repository-index output from the locally
published `textus-georesolver` warehouse. It is not a consumer-authored schema
example. The capture provenance on 2026-07-14 is:

| Evidence | Revision or SHA-256 |
|---|---|
| Cozy source revision | `e6673648cb04b9172aeaf1019585845d83259692` |
| clean `textus-georesolver` source revision | `35af42bb3f908f5b77c504a377e59a16c29af66c` |
| input `textus-georesolver.yaml` | `56bb87bf81f5ad663565acc24a6bda4f6ccd4f6e8c4eb1fde86823e9e58f4fa5` |
| input `textus-georesolver-0.2.0.car` | `5561f32790e4edb211f5dce85050b2407439bf06ebe9052acfed7a979c6d975e` |
| generated index and committed fixture | `f11915b29a72caf28f3db28b7ef78418bf290a3a73810053dab4722aa43a293f` |

The capture ran `cozy bok build` with the fixed local Cozy runtime and the
warehouse passed explicitly. Dox/Antora presentation commands were no-op test
adapters so the run remained local; Cozy's repository catalog loading, CAR
inspection, diagnostic production, and index serialization ran unmodified.
The generated file and committed fixture were compared by SHA-256.

The captured output keeps null archive metadata, nested ABI dependencies,
runtime ranges and tested versions, artifact checksums, sidecars, and
repository diagnostics so that loss of these distinctions fails a spec.

## Selected Phase 2 Schema

For each CAR entry CBD Support retains:

- artifact identity, aliases, tags, terms, status, and version selectors;
- version, channel, status, component, publication time, and artifact path;
- CNCF runtime minimum, maximum, and tested versions;
- artifact SHA-256 checksum;
- model-metadata sidecar URI;
- dependency evidence from direct, component-descriptor, or ABI-manifest
  dependency arrays; and
- producer diagnostics as source warnings.

The Cozy revision does not generate a rich SAR index. SAR uses the same
consumer envelope when a publisher provides one, but the Phase 2 fixture and
field claims are CAR-specific.

## Evidence Semantics

An empty dependency array is affirmative evidence that the selected metadata
contains no dependencies. A missing or JSON `null` descriptor/manifest is
absence of dependency evidence and must not be treated as an empty dependency
set. ABI dependencies generated under `abi_manifest.abi.dependencies` are
authoritative for that version.

Explicit version projection replaces every version-sensitive field: runtime
range and tested versions, dependency metadata, artifact URI and checksum,
model-metadata URI, channel, status, component, and publication time. A listed
version without detail clears those fields rather than inheriting them.

A runtime-constrained search requires a published minimum. When a maximum is
also present, the requested runtime must be within the inclusive minimum and
maximum range. Tested versions are retained as evidence but are not an
exclusive compatibility allowlist.

Repository diagnostics do not invalidate otherwise usable entries. They are
included in the snapshot warning with their code, artifact, version, and
available mismatch coordinates so the source becomes observably degraded.
