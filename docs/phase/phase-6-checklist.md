# Phase 6 - CBD Runtime-Boundary Adoption Checklist

This checklist is the authoritative Phase 6 state ledger. The summary is
`phase-6.md`.

## P6-01: Upstream Capability Freeze

- [x] Rebuild and verify the local CNCF development artifact containing Phase
  36 runtime-boundary capabilities. Evidence: CNCF source revision
  `0f0f37e2` was rebuilt with `sbt --batch publishLocal`; the selected local
  Ivy artifact contains `ComponentConfigurationAccess`, `ResourceTreeAccess`,
  and `ProcessExecutionResourceTreeInput` classes.
- [x] Record the selected CNCF source revision and CBD dependency mapping.
  Evidence: `phase-6.md` records Phase 36 source authority `256eddf6`, local
  rebuild revision `0f0f37e2`, and the four capability mappings.
- [x] Prove the CBD build resolves the selected capability classes. Evidence:
  CBD Support `sbt --batch Test/compile` passed against the rebuilt local
  `0.5.1-SNAPSHOT` artifact.

## P6-02: Clock and Declared Configuration

- [x] Replace ambient clock creation in CBD runtime paths with the bound
  execution-time capability or explicit injected clock. Evidence: the
  `ComponentFactory` passes `ActionCall.Core.executionContext.clock`; all CBD
  runtime/provider constructors now require a `Clock`; the ambient-clock
  static audit has no match.
- [x] Replace environment reads for catalog, BoK, SIE, local-source, source
  authentication, and CLI roles with declared configuration access. Evidence:
  `CbdRuntime.Configuration` and `ComponentConfigurationAccess` own input
  parsing, while local CLI roles are explicit `--roles` input.
- [x] Keep source credentials as opaque references and prove missing/malformed
  configuration produces safe structured outcomes. Evidence:
  `SourceAuthenticationSpec`, `InformationSourceSecuritySpec`, and
  `CbdRuntimeConfigurationSpec` pass with only opaque configuration-key
  references.

## P6-03: Admitted Local Source Trees

- [x] Represent registered development, local CAR, and cached CAR locations as
  named CNCF resource-tree references. Evidence: the declared
  `textus.cbd.*.tree` configuration contract is implemented in
  `ComponentFactory`.
- [x] Replace direct host root traversal with bounded snapshots. Evidence:
  `LocalInformationSourceInventory` receives only `ResourceTreeSnapshot`
  values, and `LocalSourceRuntimeSpec` plus `CbdRuntimeConfigurationSpec`
  prove development/CAR inspection from in-memory snapshots.
- [x] Prove unknown-tree, traversal, symbolic-link, and limit failures occur
  before CBD source/provider work. Evidence: `ComponentFactory` converts a
  failed tree snapshot into source diagnostics before inspection; traversal,
  symbolic-link, and snapshot-limit enforcement are the upstream admitted
  `ResourceTreeAccess` contract exercised before CBD receives a snapshot.

## P6-04: Cozy Provider Process Execution

- [x] Bind the logical Cozy evidence-provider process capability to the
  runtime-owned program definition, limits, environment policy, tree grant,
  and outputs. Evidence: `CozyCarReviewProviderRunner.fromScopeC` resolves
  only Process Execution admission/driver from the ActionCall scope; program
  registration remains a deployment/sbt-cozy responsibility.
- [x] Submit only logical request arguments, admitted WorkArea inputs, and
  declared artifacts from CBD Support. Evidence: the runner submits only
  canonical request bytes for `cozy-car-review`.
- [x] Map neutral Process Execution terminal results to existing CBD provider
  outcomes without changing the Review provider protocol. Evidence:
  `CozyCarReviewProviderRunnerSpec` covers success, identity/target refusal,
  output limit, and scope resolution with no live Cozy process.

## P6-05: Executable Evidence and Quality Gates

- [x] Add deterministic configuration, resource-tree, Process Execution, and
  terminal-result specifications without a live Cozy installation. Evidence:
  `CbdRuntimeConfigurationSpec`, `LocalSourceRuntimeSpec`, and
  `CozyCarReviewProviderRunnerSpec` use fixed clocks and in-memory runtime
  capabilities.
- [x] Run CBD focused and full tests, normal CAR lint, ABI validation, and
  standalone SAR validation. Evidence: `sbt --batch test` passed 221 tests;
  `cozy lint car .`, `scripts/check-car-abi.sh`, and
  `scripts/check-cbd-standalone.sh` passed. The only lint warning is the
  independent first-release ABI baseline (`abi.baseline.missing`).
- [x] Prove Review local/CI canonical-report behavior remains equivalent.
  Evidence: `CarReviewCliSpec` and `CarReviewSubmissionTransportAdaptersSpec`
  remain green in the Phase 6 full test run.

## P6-06: Documentation and Closure

- [x] Update strategy, developer/user reference material, and journal with
  adopted contracts and remaining independent residuals.
- [x] Run final review with no actionable Phase 6 findings. Evidence: the
  review found and removed the unbounded per-invocation runtime-cache key; the
  final naming scan, ambient-API scan, CAR lint, and `git diff --check` pass.
- [x] Commit validated work and close this phase. Evidence: this Phase 6
  checkpoint contains the validated implementation, phase ledger, and journal;
  the release commit records its final closure.
