# Phase 8 P8-61 Cozy Snapshot Runtime-Selection Acceptance Handoff

date=2026-08-03
phase=Phase 8
item=P8-61
status=accepted

## Context

Cozy Phase 25 aligned generated SimpleEntity CRUD source with CNCF's managed
Entity revision contract and recorded its source-level executable evidence.
The remaining driver-CAR acceptance was attempted in a clean CBD Support
worktree at commit `78eb062` using the then-declared `cozy --runtime
0.3.0-SNAPSHOT` command.

The Cozy launcher configuration had an enabled development runtime at
`/Users/asami/src/dev2025/cozy`. The launcher therefore executed Cozy
`0.3.1-SNAPSHOT`, despite the requested `0.3.0-SNAPSHOT`. Generation stopped
with `CNCF_DESCRIPTOR_TARGET_MISMATCH`: the selected runtime expected CNCF
`0.5.1`, while CBD Support's prepared descriptor correctly identified
`0.5.1-SNAPSHOT`.

## Decision

The remaining acceptance belongs to CBD Support because it owns the
driver-CAR's delegate command, launcher configuration, and P8-45 SQLite
persistence boundary. It is tracked as P8-61 rather than changing Cozy's
generated Entity contract or weakening the descriptor check.

## 2026-08-15 current resumption correction

The preceding failure is historical context. The current declaration authority
is `project.yaml`: Cozy `0.3.4-SNAPSHOT` and CNCF `0.5.2-SNAPSHOT`. These
versions supersede the historical P8-61 toolchain. Current execution evidence
is recorded below and has completed the focused re-review and Step acceptance.
This correction does not alter the historical failure's attribution and does
not complete or replace P8-60 human confirmation.

## 2026-08-15 current acceptance evidence

The shared project configuration `conf/cozy/launcher.yaml` sets only
`development.runtime.enabled: false`. `cozy runtime config show` exited 0 and
reported `runtime.devDir: (not configured)`, `development.enabled: true`,
`development.launcher.enabled: true`, and `development.runtime.enabled: false`.
`cozy --runtime 0.3.4-SNAPSHOT version` exited 0 with `cozy 0.3.4-SNAPSHOT`.

Forced-generation invocation `7556-20260814T213837Z` recorded
`sbt_exit=0`, `wrapper_exit=0`, and `lock=released`; it accepted
`goldenport-cncf_3:0.5.2-SNAPSHOT | cozy_2.12:0.3.4-SNAPSHOT` and generated
175 Scala sources rather than skipping generation. Its provenance was schema
`cozy.generation-provenance.v1`, `cozyVersion=0.3.4-SNAPSHOT`,
`cncfVersion=0.5.2-SNAPSHOT`, with runtime descriptor SHA-256
`9568025493a6273e99cba5d14df9d9ba9e0b26b33278f9dca4ef127d5b907dfd`.
The runtime descriptor identified CNCF `0.5.2-SNAPSHOT` and module
`org.goldenport:goldenport-cncf_3:0.5.2-SNAPSHOT`.

Initial exact-suite invocation `8248-20260814T214002Z` discovered 8 tests;
all failed at the common obsolete fixture with
`component ID must be namespace-qualified: CbdSupport`. This is failure
history, not acceptance evidence. Repair `P8-61-VF-001` made the fixture
derive its name, componentId, and instanceId from generated
`CbdSupportComponent` identity; independent full review closed that failure.

Final repair-validation invocation `9751-20260814T214332Z` recorded
`sbt_exit=0`, `wrapper_exit=0`, and `lock=released`; it compiled 1 test source
and `testOnly org.simplemodeling.textus.cbdsupport.impl.ReviewDiagnosisPersistenceSpec`
passed as 1 suite/8 tests. The successful fully-qualified `testOnly` runs its
`Test / compile` dependency, so no duplicate standalone compile was required.
Post-repair `git diff --check` passed.

## 2026-08-15 review acceptance

Full review accepted the runtime/config/generated identity behavior. Repair
`P8-61-VF-001` was accepted, and the focused re-review was CLEAN. Findings
`P8-61-VF-001` and `P8-61A-RV-001` are CLOSED. P8-61 is accepted; P8-60 human
confirmation remains independently pending and is not substituted by this
acceptance.

P8-60 human confirmation remains independent: neither P8-60 nor P8-61
substitutes for the other.
