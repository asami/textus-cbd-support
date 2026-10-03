# Phase 10.6 Checklist: Retained-state and Security Integration

Status: CLOSED
phase=[Phase 10.6](phase-10.6.md)
predecessor=[Phase 10.5](phase-10.5.md)
successor=[Phase 10.7](phase-10.7.md)
development-item=DEV-CBD-002

## P10-60: CBD retained-state integration

- [x] Retain richer review/proposal/alternative/supersession/evidence history
  while proving it is not required to resume the current selected state.

## P10-61: MCP/API continuation surface

- [x] Define bounded authorized operations for reading, proposing, reviewing,
  resuming, and recording continuation state without exposing an implicit
  approval or arbitrary repository mutation surface.

## P10-62: Sensitive-data and provider evidence policy

- [x] Define retention/redaction for prompts, responses, provider/model/tool
  identity, hashes, CallTree/evidence references, and non-reconstructable
  sensitive evidence.

## P10-63: Build and publication exclusion

- [x] Prove internal-model source is excluded by default from runtime packaging,
  public APIs, ordinary CML generation, documentation publication, and CAR/SAR
  artifacts.

## Closure

- [x] Release the accepted Phase 10.6 contract to Phase 10.7.

## Final acceptance evidence — 2026-10-04

The canonical contract is
[Internal-model Retained-state Security Contract](../spec/internal-model-retained-state-security-contract.md),
with the [responsibility design](../design/internal-model-retained-state-security.md).

- P10-60: [durable history specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryPersistenceSpec.scala)
  proves generated Entity/UnitOfWork persistence across independent SQLite
  reopen, exact typed selections/revisions, immutable claims, 30-day expiry,
  deletion tombstones and explicit supersession endpoints. History remains
  optional audit data, never a second canonical semantic model. The API and
  existing action-gate specifications prove package-only resume with absent,
  unavailable, unrelated or stale history, without substituting review history
  for independent requirements or actual human approval.
- P10-61: [continuation API specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelContinuationApiSpec.scala)
  exercises all five bounded operations and maintenance, per-call authenticated
  principal/session/role admission, exact package/scope/subject/artifact
  selection, malformed/stale/foreign inputs and preserved CML/cursor state.
  The selected surface is a runtime-internal API, not a new public MCP endpoint.
  All 28 generated history Entity/Aggregate/View operations, including default
  Summary/Detail routes, are absent from actual Factory protocol listing and
  resolution; unrelated operations and service metadata are positive controls.
- P10-62: [evidence policy specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEvidencePolicySpec.scala)
  and [history validator specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryValidatorSpec.scala)
  cover all payload/evidence kinds, finite limits, exact identities, invalid
  graphs and ScalaCheck properties. Raw prompts/responses cannot be retained;
  provider/model/tool tokens are metadata only, narratives require an explicit
  admitted redaction reference, and CallTree/external evidence bodies remain
  omitted with non-reconstruction explicit. No content hash controls history
  identity, revision, authority or continuation; inherited hash wording grants
  no exception to repository rules.
- P10-63: [output exclusion specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOutputExclusionSpec.scala)
  consumes actual default CML generation, runtime JAR, public API JAR/descriptor,
  Scaladoc/local publication and CAR/SAR outputs. Present private inputs and
  nonempty public controls prevent vacuous success. Complete names/content and
  nested archives are scanned with fail-closed resource bounds. The repository
  and fixture operational Cozy configuration disable source-manifest publication;
  public project/page metadata survive in the actual final publication bundle.
  This proves this consuming project's selected default, not Cozy's different
  generic enabled profile or an external deployment/publication.

### Accepted Steps and review closure

- P106-STEP-01: `3b24ec7bb3fc914a6bc782d9fc684e8682e61aaa`, 3 suites / 36 tests,
  independent protected-focused acceptance `P106-S01-REVIEW-001/1`.
- P106-STEP-02: `f82b5e4dfe5638b5334877d9ac99cef844ebf745`, 6 suites / 99 tests,
  focused acceptance `P106-S02-FOCUSED-REVIEW-002/1`. The generated-view bypass
  was corrected within the separately user-approved one-attempt repair.
- P106-STEP-03: `5f7e7f80760a77291f752faa6d549b1d76456c5a`, 7 suites / 106 tests,
  independent protected-focused acceptance `P106-S03-REVIEW-001/1`.
- Original Phase base: `a3c3de2e98d5b6e265f364dc40bc6c6276ef9763`.
  The sole epoch-1 full Phase review `P106-FULL-REVIEW-001/1` covered the
  complete Phase accumulator and consumed contracts. Its only blocker,
  `CPB-P106-CAR-ABI-ENTITY-SURFACE-001`, was closed by independent focused
  PASS `P106-PHASE-FOCUSED-REVIEW-001/1`, saved/read in review ledger revision7.
  Canonical/generated/packaged ABI agrees on all 19 operations and 8 ordered
  entities; the unchanged strict checker confirms packaged manifest equality.
  No second full review, new PLAN epoch or counter reset was used.
- Phase repair cycle1 is independently accepted; consecutive nonconverging0.
  Historical Step01 2/3, Step02 3/3 plus separate1/1, and Step03 3/3 remain
  consumed history rather than renewed allowances.

### Final validation and local release binding

- Exact logical full-suite command: `sbt --batch 'set Test / fork := true' test`,
  executed by registered `cncf_command_runner` through the shared serial wrapper.
  `P106-PHASE-FULL-RESULT-001/1`: 129 suites / 1032 tests succeeded, 0 failed,
  aborted, canceled, ignored or pending; SBT/wrapper exit0, `lock=released`.
  Main/test and actual output fixture task dependencies passed.
- Final parent CAR lint `P106-PHASE-FINAL-CAR-LINT-001/1`: exit0, no FAIL,
  four pre-existing warnings. The two filesystem warnings retain
  `DEV-P10-04-FILESYSTEM-CAPABILITY-001` and
  `DEV-P10.1-STEP-02-CAR-FILESYSTEM-001`; the absent previous-release ABI
  baseline and selected SNAPSHOT plugin retain `HYG-P9-20A-CAR-ABI-BASELINE`
  and `HYG-P9-20A-CAR-SBT-COZY-SNAPSHOT`. No baseline or release history
  was fabricated and no dependency was upgraded.
- Component `0.1.0-SNAPSHOT`, sbt-cozy `0.1.18-SNAPSHOT`, Cozy `0.3.3-SNAPSHOT`
  and CNCF `0.5.3-SNAPSHOT` are preserved. Scala version/history headers were
  maintained in accepted Steps with their actual source-update dates; the
  remaining release changes add no Scala declarations or executable semantics.
- Distinct local release binding: `P106-PHASE-CLOSURE-001@1`; selected request
  `P106-PHASE-RELEASE-COMMIT-001/1`. This closure is authoritative only after
  the exact local release commit carrying
  `Phase-Closure-Binding: P106-PHASE-CLOSURE-001@1` and its verified committed
  closure receipt succeed. Step commits alone do not establish Phase closure.
- No push, public publication, deployment or Phase 10.7/11/12 execution is
  included. CLI/Web runtime help remains unverified and CBD review integration
  is not configured; neither is represented as a passing runtime probe.

### Accepted findings and preserved planning

The [canonical Hygiene journal](../journal/2026/10/2026-10-03-phase-10.6-hygiene-follow-up.md)
contains exact accepted `HYG-P106-S02-FACTORY-SIZE-001` and
`HYG-P106-S02-FACTORY-PRIVATE-FIELDS-001` records. Both remain OPEN nonblocking
follow-ups, not current Phase blockers. No new Development Candidate was
accepted, so no empty Phase Development Candidate journal is created.
Both unpersisted accepted-ID lists are empty.

Shared projection synchronization is explicitly deferred, not Phase work deferred.
The complete coexisting planning set remains unstaged and unmodified by this
release: `docs/phase/README.md`, `docs/phase/phase-11-checklist.md`,
`docs/phase/phase-11.md`, `docs/phase/phase-12-checklist.md`,
`docs/phase/phase-12.md`,
`docs/strategy/textus-cbd-support-development-strategy.md`, and
`docs/notes/phase-11-12-local-execution-preparation.md`.
The canonical committed Phase 10.6/checklist controls completion, not stale
shared index/strategy projections. Phase 10.7 is a separate not-started goal.
