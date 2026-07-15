# Future Candidate: Default Catalog Rich Metadata Publication

## Candidate

- ID: `FUTURE-CATALOG-PUBLISHER-01`
- Owner boundary: Cozy repository publisher and the simplemodeling.org site
  publication project
- Consumer: `textus-cbd-support`
- Status: deferred publisher work; the CBD runtime compatibility provider
  remains required until the acceptance gates below pass

## Evidence Audit

The default source was checked on 2026-07-14:

| Endpoint | Result | Meaning |
|---|---:|---|
| `https://www.simplemodeling.org/en/catalog/index.html` | HTTP 200 | Publication compatibility catalog is deployed. |
| `https://www.simplemodeling.org/metadata/artifacts/repository/textus-semantic-integration-engine.json` | HTTP 200 | Compatibility repository-artifact evidence is deployed. |
| `https://www.simplemodeling.org/metadata/repository/car/index.json` | HTTP 403 | Rich CAR index is not publicly accessible. |
| `https://www.simplemodeling.org/metadata/repository/sar/index.json` | HTTP 403 | Rich SAR index is not publicly accessible. |

The local simplemodeling.org publication workspace contains
`metadata/catalog/projects` and `metadata/artifacts/repository` JSON but no
`metadata/repository/car/index.json`, `metadata/repository/sar/index.json`, or
public repository catalog entries. The current Cozy producer implements rich
CAR index generation and publication copying; equivalent public SAR index
generation is not present in the inspected producer contract.

The local evidence is revision-pinned:

| Repository | Commit | Inspected evidence |
|---|---|---|
| `cozy` | `e6673648cb04b9172aeaf1019585845d83259692` | `src/main/scala/cozy/bok/CozyBok.scala`, `src/main/scala/cozy/archive/RepositoryArtifactCatalog.scala`, and `src/test/scala/cozy/CozyBokRepositoryCarSpec.scala` |
| `simplemodeling-org` | `5643afe687de469f278ce4749e1a0daff9b6cae0` | `website.d` and `target/war/standalone.d` publication metadata trees |

The inspected Cozy producer and specification files matched that commit. Its
unrelated dirty `docs/strategy/cozy-development-strategy.md` was excluded from
the evidence boundary. The inspected simplemodeling.org worktree was clean.

## Required Publisher Work

1. Register authoritative CAR and SAR entries under the publisher's
   `repository/catalog/{car|sar}` source of truth.
2. Generate `metadata/repository/car/index.json` and
   `metadata/repository/sar/index.json` from those entries during the normal
   site build.
3. Publish version artifact paths, runtime evidence, component descriptors,
   ABI manifests, and diagnostics without converting missing evidence into
   defaults.
4. Publish CML and model-metadata sidecars at same-origin public paths referenced
   by each index.
5. Deploy the generated metadata with the normal simplemodeling.org site
   release and retain the compatibility catalog until consumers migrate.

## Acceptance Gates

This candidate is complete only when:

- both rich index endpoints return HTTP 200 and valid JSON;
- at least one real CAR entry contains version, artifact, runtime, descriptor,
  ABI/dependency, and sidecar evidence;
- a real SAR entry is present when a SAR is published, without fabricating an
  empty SAR catalog contract;
- every referenced model-metadata JSON sidecar returns HTTP 200 from the same
  origin;
- `textus-cbd-support` provider specifications consume captured deployed
  evidence and the representative MCP search/lookup/usage flow passes; and
- publication compatibility fallback remains verified until an explicit
  migration decision removes it.

## Recheck Commands

```sh
curl -I https://www.simplemodeling.org/metadata/repository/car/index.json
curl -I https://www.simplemodeling.org/metadata/repository/sar/index.json
curl -I https://www.simplemodeling.org/en/catalog/index.html
```

This candidate does not authorize `textus-cbd-support` to publish or synthesize
catalog evidence. It records the producer/deployment gap so Phase 2 can close
the consumer boundary honestly.
