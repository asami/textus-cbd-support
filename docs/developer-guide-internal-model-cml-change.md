# Internal CML change application

The package-private engine
[`InternalModelCmlChangeApplication.applyApproved`](../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplication.scala)
accepts the exact consuming root, an independently admitted
`InternalModelCmlChangeRequest` and an explicit application RecordReference.
It freshly evaluates the rooted gate immediately before native mutation. Cached
eligibility and plans do not authorize application.

The [contract](spec/internal-model-cml-change-contract.md) defines the source,
human and owner evidence; the [design](design/internal-model-cml-change.md)
defines the existing-owner composition and descriptor lifetime.
[`NativeCmlFileWriter`](../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriter.scala)
uses Darwin LP64 anchored no-follow all-target preflight and existing-only writes.
Its private preparation device/inode observations never become control identity.
It retains actual ordered file effects and primary/cleanup failures.

## Consuming adapter dependency

The [repository skill source](skills/cbd-apply-cml-change/SKILL.md) is versioned
instructions, not an installed command. A reviewed consuming-project internal
adapter must call the actual rooted engine through registered runtime execution.
There is no deployed consuming adapter or public CLI/MCP/UI endpoint in this
Slice. Missing exact input, source-owner authority or adapter is a dependency
stop; neither fabricated command lines nor generated test sources supply it.
Phase 10.7 composes the accepted owners in a test-only end-to-end proof; it
does not provide a deployed end-user adapter.

## Ownership and results

Source authority, source identity and both current/next revisions are explicit
owner inputs. Versions can be opaque and independently ordered; artifact,
logical record and semantic subject revisions do not allocate source versions.
Applied next revisions are intent/application evidence, not a persisted owner
store update. The engine does not parse CML or infer refreshed semantic facts.

`Rejected` carries the complete fresh gate and no write report. A structured
gate failure propagates without effects. `Failed` carries concrete native
outcomes, including an Applied prefix and a possibly truncated/partially written
Failed target; later targets remain NotAttempted. Cleanup causes do not erase
the primary cause. Close occurs once and never retries. A local single writer
supplies the concurrency assumption: there is no rollback, transaction, crash
durability, locking or racing-writer guarantee.

Only all targets physically applied after write/flush/close yield
`AppliedPendingValidation`. That result grants no acceptance, cursor mutation
or Git action. Slice C must run actual required Cozy/CBD validation and obtain
independently refreshed owner source/realization/continuity evidence matching
applied targets and next revisions, then regenerate the shared Phase 9 CCDM
and all eight stakeholder/engineering views. Repository acceptance remains
source-owner Git-governed work after the required evidence succeeds.

## Executable specification

- [Native writer specification](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriterSpec.scala):
  actual byte effects, preserved file mode/owner, symlink/hardlink/physical-alias
  refusal, all-target preflight, bounded generated payloads and precise syscall
  failure/partial-progress outcomes.
- [Rooted application specification](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplicationSpec.scala):
  exact retained actual-human/owner/reference evidence, independent explicit
  version properties, fresh denial after earlier eligibility, actual preflight
  and partial-I/O failure, and truthful pending validation.
- [Read-only gate specification](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeGateSpec.scala):
  complete typed admission and drift causes, independently owned by Slice A.

The runtime-private test decorator substitutes only truncate/write/flush
syscalls while preserving real gate, opening/stat and close. Production always
uses actual native operations. Unsupported platform/ABI is an explicit failure,
not a successful simulation. Historical Slice evidence passed A 40 + 70,
B 27 + 118, and C 17 plus the 215-specification accumulator, with real positive
and expected-negative integration. Those earlier counts are not current Phase
10.7/full-suite evidence. As of 2026-10-04, Phase 10.6 is released at `f0f91f7`
under its [canonical checklist](phase/phase-10.6-checklist.md), including
retained-state/security and actual output exclusion. Phase 10.7 Steps 01/02
are separately accepted as recorded in the
[current ledger](phase/phase-10.7.md#accepted-step-evidence--2026-10-04);
final Phase validation, review and release remain pending.

## Slice C validation and test observation transport

[`InternalModelCmlChangePostValidation.validate`](../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidation.scala)
accepts a normalized absolute root and immutable Request: complete actual
ApplicationReport, explicit post-validation RecordReference, raw command
observations and optional independently refreshed OwnerEvidence. It executes no
command or mutation. Dispositions are Incomplete, Failed and
ReprojectedPendingAcceptance. Failed dominates missing facts; actual raw inputs,
findings and owner/freshness evidence survive ordinary denials. Existing owner
Consequence failures propagate. Only a clean result contains the complete actual
source-backed shared realization, binding and all eight DTOs; it grants no Git,
cursor or repository acceptance.

Commands bind one unique reference per applied target to the application/root/
source/path/approved next revision and exact runtime 0.3.3-SNAPSHOT argv. Only
closed Cozy finding stdout is parsed. Missing/indeterminate commands stay
Incomplete; nonzero exit, FAIL or malformed JSON fails. Independently refreshed
owner selectors name new realization/binding artifact and logical references
and new target snapshot references, with exact new-keyed live source inputs.
The existing rooted package, freshness/native-read and projection owners are
used directly. Every required snapshot and every selected changed-target snapshot
must be Compared(Unchanged) without missing dimensions, regardless of its optional
inventory flag. Mapped exact identities/lanes/attribution/full conditions survive;
only unrelated optional MissingBaseline survives and remains visible.

The [specification](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidationSpec.scala)
uses explicit synthetic terminal unit observations around real B writes and
owner construction. It does not prove real Cozy integration. The
[acceptance support](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceSupport.scala)
supplies independently declared rich/all-eight owner data. The
[acceptance probe](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceProbe.scala)
is test-only: its main receives one exact absolute fresh workdir under
`target/internal-model-cml-change-acceptance/<run>` and mode `success` or
`lint-failure`. It copies only project.yaml, admitted present package artifacts
and two declared CML baselines, independently readmits them, performs actual B
application and retains that exact report in the same JVM.

The probe writes ready.json once with schema
`textus.cml-change-probe-ready.v1`, actual positive processId, workdir,
projectRoot, applicationReference, mode and exact target paths/source revisions/
command references (cozy-alpha/613 and cozy-beta/617). The parent first admits
the actual native JVM and independently observed PID/command. It then uses the
registered Cozy runner for both exact live commands and, only after native
terminal execution is admitted, supplies alpha-cozy-observation.json and
beta-cozy-observation.json. The probe starts no process or CLI. Coordination
checks only existence periodically and waits at most 1800 seconds; timeout fails.

Each observation is a closed JSON object with fields `schema` (exactly
`textus.cml-change-command-observation.v1`), `commandReference`,
`applicationReference`, `projectRoot`, `targetId`, `projectRelativePath`,
`sourceAuthority`, `sourceIdentity`, `sourceRevision`, `runtimeVersion`, `argv`
and `outcome`. References use the existing typed codec recordId/recordRevision
objects. Terminal outcome is exactly `kind: "terminal"`, integer `exitCode`,
full string `stdout` and `stderr`; missing/indeterminate outcome contains only
`kind` and `reason`. The required argv is
`["--runtime","0.3.3-SNAPSHOT","lint","cml",<normalized absolute actual target>,"--format","json"]`.
No metadata receipt is a substitute for actual runner evidence.

After observations arrive, explicit owner refresh selects realization-main/503,
realization-order/509, projection-main/521, binding-order/523, snapshot-model/557,
snapshot-cml-alpha/541 and snapshot-cml-beta/547 with declared model-owner-C1 and
the exact applied next CML revisions. Original scenario/glossary evidence and
raw resume bytes remain. The parent-selected positive plan requires both real
exits 0, Unchanged required sources, eight nonempty DTOs and complete rich/core
sidecars. The separate negative alpha ENTITY/string-attribute payload must
produce actual exit 1 and cml.domain.string-attribute FAIL; beta stays valid.
Failed/no successful continuity and retained actual changes are required.

One closed stdout result uses schema `textus.cml-change-probe-result.v1` and
fields mode, processId, applicationReference, postValidationReference,
disposition, commandExitCodes, requiredSourcesUnchanged, projectionFamilies,
nonemptyFamilies, opaqueLaneAndConditionsRetained and cursorUnchanged. Exit 0
means all expected mode predicates were observed; negative exit 0 proves refusal,
not positive validation. Exceptions or false predicates fail. Evidence/root
is retained for the parent. Neither test transport nor probe is a deployed
adapter; missing consuming adapter remains the application dependency stop.

## End-to-end reproduction

The accepted [end-to-end specification](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndSpec.scala)
and [Probe](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndProbe.scala)
compose actual package/continuation, human gate, native application,
post-validation and Phase 9 owners. The
[session wrapper](../scripts/test/run-internal-model-end-to-end-session.py)
owns external orchestration, associated observations and child cleanup; the
[readiness specification](../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndReadinessSpec.scala)
covers ten scenarios, partial-publication properties and actual wrapper-bytecode
absence. The [evidence report](validation/phase-10.7-end-to-end-evidence.md)
is the detailed method/result reference; its pending-Step paragraphs are the
historical pre-commit snapshot, superseded for acceptance status by the dated
[canonical Step record](phase/phase-10.7.md#accepted-step-evidence--2026-10-04).

Reproduction requires existing managed CPython >=3.12, the explicitly selected
Temurin/JDK 21 executable, and the existing SBT-produced Test classpath export.
`REPO`, `JAVA` and `COZY` below are concrete admitted root/executable locators;
do not infer them from shell defaults. Prepare any required SBT through the
registered serial command owner. The wrapper does not build or guess its
classpath. The parent separately selects the registered runtime-session owner
and registered Cozy command owner, freezing absolute arguments and readiness
bindings. These are test instructions, not a public application command.

Each run root must be a new direct child of
`target/internal-model-end-to-end/work/`. The following exact evidence-report
template describes the original positive001 arguments; replace the root with a
fresh admitted direct-child name when reproducing it, never reuse retained runs.

```text
uv run --no-cache --managed-python --no-python-downloads --script \
  scripts/test/run-internal-model-end-to-end-session.py run \
  --run-root REPO/target/internal-model-end-to-end/work/p107-s02-positive-001 \
  --java JAVA \
  --classpath-file REPO/target/streams/test/fullClasspath/_global/streams/export \
  --label 'actual α' --reverse false --carrier 43 --cml-mode success
```

The second positive selects label `再開の別表示 😀`, reverse `true`, carrier `79`
and `success`. The negative selects label `actual failed validation`, reverse
`false`, carrier `43` and `lint-failure`. Original completed roots were
positive001, positive002 and negative002; negative001 was interrupted and
supplies no fourth successful scenario. The producer must exit 0 within
60 seconds before the isolated fresh consumer starts. The consumer retains
its actual ApplicationReport in memory with a 1,800-second observation ceiling;
the wrapper permits at most 1,860 seconds for terminal consumer completion.

For each ready target, the separate Cozy owner executes lint from the ready
project cwd. After admitting the actual complete native terminal v6 result,
the associated observation client uses:

```text
COZY --runtime 0.3.3-SNAPSHOT lint cml READY_ABSOLUTE_TARGET --format json
uv run --no-cache --managed-python --no-python-downloads --script \
  scripts/test/run-internal-model-end-to-end-session.py observe \
  --run-root RUN_ABSOLUTE --target-id target-alpha --receipt ACTUAL_V6_RESULT_ABSOLUTE
```

Use `target-beta` for beta. `cml-alpha`/`cml-beta` identify sources and are not
target IDs. `observe` admits `cozy.exact-cli-command-result.v6`, including
timeout state, native integer exit, exact cwd/executable/argv and ready binding,
and reads the complete UTF-8 stdout/stderr files. A tail, synthesized outcome,
hash or byte seal supplies neither terminal evidence nor permission.

Both positive targets require actual lint success. Negative alpha requires
actual exit 1/FAIL while beta passes; expected-negative wrapper exit 0 proves
the refusal predicates, not successful lint. For each positive run, generate
both alpha and beta from their actual applied CML:

```text
COZY --runtime 0.3.3-SNAPSHOT modeler-scala-value READY_ABSOLUTE_TARGET \
  --save RUN_ABSOLUTE/generated/alpha
```

Use `generated/beta` for beta. Retain complete terminal results/raw outputs
and inspect nonempty `AlphaValue`/`BetaValue` declarations. Generation proves
output existence, not generated Scala compilation. The existing compatibility
checker result proves CNCF declaration consistency only, not new live ABI
certification. Actual selections were Cozy `0.3.3-SNAPSHOT`, sbt-cozy
`0.1.18-SNAPSHOT`, CNCF `0.5.3-SNAPSHOT` and Scala `3.3.8`/JDK 21.

An interrupted run uses its observed wrapper PID through the same session owner:

```text
uv run --no-cache --managed-python --no-python-downloads --script \
  scripts/test/run-internal-model-end-to-end-session.py stop \
  --run-root RUN_ABSOLUTE --wrapper-pid OBSERVED_WRAPPER_PID
```

The client writes only an associated stop request; the wrapper terminates only
its own children. Require `terminal-summary.json`, actual terminal exits and
owned cleanup. Retain wrapper/process/producer/ready/consumer/observation
records and complete stdout/stderr under the disposable run root.

Accepted Step01 `272f59d` passed six representative and 185 accumulator tests;
its command observations are synthetic composition proof. Accepted Step02
`6f7cef8` passed two suites / 16 tests and the three external sessions, six
actual lint and four generation results. Both positives retained before/post
counts `[3,1,1,3,1,2,3,5]` in Mono-Koto, Use Case, Entity, Event, Structure,
Classification, Workflow, StateMachine order, all 24 predicates and complete
own-producer semantics, plus cross-positive selection equality. They remain
`ReprojectedPendingAcceptance`. Negative002 retained all 22 predicates, lint
`[1,0]`, `Failed`, post `[]`, no continuity and actual file/cursor evidence.
All three wrapper/producer/consumer exits were 0 with owned cleanup.

Graph, refresh, source revisions and original/current human/owner decisions
are independently supplied typed test inputs through existing owners. They
are not inferred from CML/output/provider state and do not authenticate a
human or source. No installed adapter, public endpoint, retained producer
session, automatic Git/cursor acceptance or deployment was exercised.
[Phase 10.7](phase/phase-10.7-checklist.md) remains OPEN; documentation is
prepared/review-pending, and full-suite/current lint/full review/release remain owed.
