# CBD Runtime-Boundary Phase 6 Start (2026-07-17)

Phase 6 adopts CNCF Phase 36 in CBD Support. The work is a component-runtime
migration, not a CAR Review rule change. Its upstream source authority is CNCF
commit `256eddf6`; its downstream coordination record is CNCF Phase 37.

The first audit found that CBD Support initially resolved
`goldenport-cncf:0.5.1-SNAPSHOT` from a local artifact dated 2026-07-15, which
did not contain the Phase 36 runtime-boundary classes. CNCF source revision
`0f0f37e2`, which contains Phase 36 closure revision `256eddf6`, was rebuilt
with `sbt --batch publishLocal`. The local Ivy artifact now exposes the
required configuration, resource-tree, and Process Execution classes, and CBD
Support `sbt --batch Test/compile` passes against it. This is local development
dependency preparation, not release publication.

The migration must preserve CBD Support's canonical Review result and provider
wire contracts while removing direct ambient clock, environment, filesystem,
and process access.

P6-02 now introduces `CbdRuntime.Configuration` and an explicit-clock factory.
The factory composes the existing catalog, BoK, SIE, local-source, and
source-authentication parsers from declared input values without reading the
host environment itself. `CbdRuntimeConfigurationSpec` proves deterministic
construction with a fixed clock. The legacy ambient entry point remains only
until ComponentFactory has been migrated to assemble this configuration through
the CNCF component runtime boundary; it is not Phase 6 completion evidence.

## Resource-tree action boundary

CBD Support now treats development, local-CAR, and cache-CAR inputs as named
resource trees. `ComponentFactory` reads only
`textus.cbd.development.trees`, `textus.cbd.local-car.tree`, and
`textus.cbd.cache-car.tree`, then obtains bounded snapshots from the
ActionCall's `ExecutionContext.resourceTrees`. Snapshot inspection produces
logical `resource-tree:<name>/...` evidence locations and can inspect CAR
metadata and checksums without a host path. The former CBD component
configuration keys for directory paths and `home-root` have been removed.

The legacy local-path implementation has now been removed. Local inspection
accepts only bounded snapshots and uses logical `resource-tree:` provenance;
it has no host root or traversal API. A failed tree admission becomes a source
diagnostic before local source/provider work begins. Upstream ResourceTree
admission remains responsible for traversal, symbolic-link, and tree-limit
rejection before CBD receives a snapshot.

> **Jul. 20, 2026 correction:** this historical Phase 6 description predates
> the separate bounded resource-tree query capability. Development sources now
> request exact `project.yaml` entries through `ResourceTreeQuery`; CAR storage
> retains the strict snapshot behavior described here. Both paths preserve only
> logical `resource-tree:` provenance in CBD.

## Cozy Process Execution boundary

The old CBD-owned `ProcessBuilder` transport was replaced by
`CozyCarReviewProviderRunner`, which takes a pre-admitted CNCF Process
Execution capability and driver. Its only submitted input is the bounded,
canonical provider-request byte stream. The runner owns the existing CBD
provider-result mapping but not the executable, child environment, WorkArea,
or output directory. Deterministic Process Execution results now prove
successful bundle handling, target/provider rejection, and output-limit
mapping without a live Cozy installation.

The local Review CLI no longer reads `TEXTUS_CBD_REVIEW_PROCESS_ROLES` from the
ambient process environment. Local roles are explicit `review submit --roles`
input; server authorization remains at the private server boundary.

The follow-up audit removed the remaining ambient clocks from CBD provider and
runtime helper constructors. All callers must now supply a bound clock; the
component path supplies `ActionCall.Core.executionContext.clock`. The report
codec's relative-location validation was also made string-based, leaving no
direct ambient environment, host filesystem, host-path, or host-process API
in `src/main`.

For Cozy integration, `CozyCarReviewProviderRunner.fromScopeC` resolves only
the runtime-owned Process Execution admission and driver from the invocation
scope. CBD Support identifies `cozy-car-review`; the runtime deployment (and
eventually sbt-cozy deployment wiring) owns the command template, limits,
environment, WorkArea grants, and artifact policy. This keeps Review protocol
mapping in CBD Support without granting it a process-launch escape hatch.

Validation after the final cache-boundary review passed `sbt --batch test`
(221 tests), `cozy lint car .`, `scripts/check-car-abi.sh`, and
`scripts/check-cbd-standalone.sh`. The lint's missing ABI baseline is the
independent first-release condition already covered by the ABI governance
script; it is not a Phase 6 runtime-boundary finding. Phase 6 is ready for a
release commit, after which its phase document can be closed.
