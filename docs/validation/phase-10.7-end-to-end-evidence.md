---
status: focused-execution-validated
decision_scope: PHASE-10.7 / P107-STEP-02 / P107-S02-A / P107-S02-IMPLEMENT-001
updated_at: 2026-10-04
---

# Phase 10.7 End-to-end Evidence

## Stage Status

| Field | Value |
| --- | --- |
| Current status | focused-execution-validated |
| Owner | Parent Phase 10.7 workflow; registered runtime-session owner for execution and cleanup |
| Update rule | Fill only from parent-supplied admitted terminal results, complete raw outputs and semantic observations; retain failures and unavailable obligations explicitly. Authoring establishes no validation, Step acceptance or Phase closure. |

The [contract](../spec/internal-model-end-to-end-contract.md),
[design](../design/internal-model-end-to-end-validation.md) and
[executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEndToEndSpec.scala)
retain semantic authority. The new
[test session wrapper](../../scripts/test/run-internal-model-end-to-end-session.py)
coordinates the committed terminal producer and fresh consumer in external mode.
The parent-admitted execution below establishes focused external evidence.
Independent Step acceptance, the local Step commit, Step 03 and final Phase
closure remain outstanding. Positive continuity is pending independent acceptance.

## Reproducible Method

Use stable managed CPython >=3.12 through `uv run --no-cache --managed-python
--no-python-downloads --script`. The parent selects the registered runtime-session
runner and freezes absolute arguments before execution. Run roots are new direct
children of `target/internal-model-end-to-end/work/`; retained outputs are disposable
test evidence. `JAVA` below means the plan-selected existing Temurin 21 executable,
and `REPO` means the concrete normalized repository root. Neither is inferred from
the shell's default JVM. The classpath is the existing SBT-produced Test export;
the wrapper does not build or guess it.

```text
uv run --no-cache --managed-python --no-python-downloads --script \
  scripts/test/run-internal-model-end-to-end-session.py run \
  --run-root REPO/target/internal-model-end-to-end/work/p107-s02-positive-001 \
  --java JAVA \
  --classpath-file REPO/target/streams/test/fullClasspath/_global/streams/export \
  --label 'actual α' --reverse false --carrier 43 --cml-mode success
```

The selected three completed sessions ran sequentially with distinct roots:

| Run directory | Label | Reverse | Carrier | CML mode | Required observed outcome |
| --- | --- | --- | --- | --- | --- |
| p107-s02-positive-001 | actual α | false | 43 | success | ReprojectedPendingAcceptance |
| p107-s02-positive-002 | 再開の別表示 😀 | true | 79 | success | ReprojectedPendingAcceptance |
| p107-s02-negative-002 | actual failed validation | false | 43 | lint-failure | Failed, effects retained, no continuity |

The earlier `p107-s02-negative-001` reached readiness and was stopped when the
native Goal paused. It is retained separately below and supplies no semantic
result. Every reproduction must use a new direct-child run root; neither the
interrupted root nor any completed root may be reused.

The wrapper runs only two fixed Probe Java commands. The producer must exit
successfully within 60 seconds before the consumer starts. Each has separate
cwd/home/tmp and only explicit HOME/TMPDIR supplied. The observed child environment
allows the existing optional macOS CF marker. Readiness exposes actual distinct
PIDs, native wrapper ownership, applied targets and command bindings; no port,
provider, database or original session is used. The consumer retains its actual
ApplicationReport and its existing 1,800-second wait ceiling. The wrapper permits
at most 1,860 seconds for consumer terminal completion, then cleans only its own
Popen children, using a bounded graceful/forced wait if needed.

For each ready target, the parent separately freezes and executes the registered
Cozy command from the ready project cwd, with the existing launcher and runtime:

```text
COZY --runtime 0.3.3-SNAPSHOT lint cml READY_ABSOLUTE_TARGET --format json
uv run --no-cache --managed-python --no-python-downloads --script \
  scripts/test/run-internal-model-end-to-end-session.py observe \
  --run-root RUN_ABSOLUTE --target-id target-alpha --receipt ACTUAL_V6_RESULT_ABSOLUTE
```

Use exact `target-beta` for the second target; `cml-alpha`/`cml-beta` are source
identities, not target identifiers. Observe consumes only the parent-admitted
`cozy.exact-cli-command-result.v6` terminal Cozy result, checking timeout, native
integer exit, exact cwd/executable/argv and ready application binding. It reads
the complete declared UTF-8 stdout/stderr files and exclusively publishes one
complete existing command-observation transport. A real nonzero exit is retained.
Tails, synthetic outcomes, byte seals and hashes do not supply evidence or permission.

Both positive targets must yield successful actual lint. Negative alpha must yield
actual FAIL/nonzero while beta passes. For each positive run, execute and inspect
both separate generation results:

```text
COZY --runtime 0.3.3-SNAPSHOT modeler-scala-value READY_ABSOLUTE_TARGET \
  --save RUN_ABSOLUTE/generated/alpha
```

Use `generated/beta` for beta. Preserve the complete terminal results and raw
outputs, and record nonempty AlphaValue/BetaValue declarations from the actual
applied files. Generation does not establish that generated Scala compiles.
No negative generation is required. The semantic owner refresh is independently
declared fixture evidence using the existing constructors and validators; it is
not a model derived from CML or a provider response. Source versions remain
explicit owner declarations and human/owner inputs are freshly admitted.

On an interrupted session, the same registered owner uses the recorded wrapper PID:

```text
uv run --no-cache --managed-python --no-python-downloads --script \
  scripts/test/run-internal-model-end-to-end-session.py stop \
  --run-root RUN_ABSOLUTE --wrapper-pid OBSERVED_WRAPPER_PID
```

Stop exclusively writes an associated request. The named live wrapper alone
terminates its owned children; the client does not signal an arbitrary PID.
Require `terminal-summary.json` and actual terminal cleanup before acceptance.
Retain each run's `wrapper.json`, producer/consumer process records,
`producer-terminal.json`, `session-ready.json`, `producer.json`, `ready.json`,
`consumer-result.json`, both observation files and producer/consumer stdout/stderr.
The terminal summary records expectation conditions, native exits, cleanup and
relative evidence paths, including expected negative command exits.

## Requirement and Executable Scenario Mapping

| Scenario | Existing obligation and Step 02 method | Current evidence |
| --- | --- | --- |
| E1 | Terminal producer/fresh consumer; compare complete before, selection and Phase 9 reports; require eight positive before counts, sidecars, preapplication source retention, isolated environment and cursor retention. | Positive001 and positive002: complete before/selection/Phase 9 reports equal each run's own producer; all eight DTOs, sidecars, sources, isolation and cursor predicates true. |
| E2 | Fresh exact human/owner input; retain actual ApplicationReport equal to post-validation application; positive commands, eight positive post counts, sidecars, required sources, no problems and ReprojectedPendingAcceptance only. | Both positives: actual ApplicationReport retained/equal post report, fresh approval/application/effects predicates true, lint [0,0], all eight post counts nonempty, no problems; ReprojectedPendingAcceptance. |
| E3 | Existing grouped no-write refusal for missing/rejected human input, wrong candidate revision or missing ownership. | Retain parent-supplied Step 01 semantic evidence; no new refusal variant is claimed here. |
| E4 | Existing grouped owner-version drift/missing-version refusal. | Retain parent-supplied Step 01 semantic evidence; no source-version allocation or authentication is claimed. |
| E5 | Actual negative alpha lint composes Failed, empty counts/no continuity, nonempty problems and retained effects/cursor; compare native command outcomes with transported complete outputs. | Negative002: actual lint [1,0], Failed, post counts [], continuity None, two alpha problems, actual ApplicationReport and effects/cursor retained; full outputs transported uncoerced. Existing missing-observation case remains in the Scala specification. |
| E6 | Compare both positive semantic selections despite label/order/carrier variation; retain the existing four-variant ScalaCheck process-pair specification. | Whole semantic selections of positive001/positive002 equal although producer before reports differ; retained Step 01 four-variant property remains separate broader evidence. |

## Evidence and Remaining Obligations

All run directories below are relative to
`target/internal-model-end-to-end/work/`. Times are UTC on 2026-10-04; exits
are ordered wrapper/producer/consumer.

| Run directory | Actual PIDs: wrapper/producer/consumer | Wrapper start UTC | Consumer start UTC | Terminal UTC | Native exits | Terminal result |
| --- | --- | --- | --- | --- | --- | --- |
| p107-s02-positive-001 | 56587/56595/56605 | 01:31:35.482908Z | 01:31:37.236346Z | 01:39:49.499177Z | 0/0/0 | All 24 predicates true; ReprojectedPendingAcceptance |
| p107-s02-positive-002 | 60559/60560/60589 | 01:41:13.159738Z | 01:41:14.811784Z | 01:51:03.107475Z | 0/0/0 | All 24 predicates true; ReprojectedPendingAcceptance |
| p107-s02-negative-001 (interrupted) | 64724/64725/64751 | Not supplied | Not supplied | 02:08:50.192181Z | 130/0/143 | Associated owner stop after readiness; no semantic result or paired observations |
| p107-s02-negative-002 | 83077/83078/83103 | 02:45:47.720613Z | 02:45:49.250714Z | 02:53:47.572849Z | 0/0/0 | All 22 predicates true; expected semantic Failed |

All four terminal summaries recorded `cleanupComplete` and no cleanup errors.
The parent separately verified absence of all actual PIDs. Negative001's producer had already exited 0 when
the same registered owner stopped the wrapper; its consumer terminated 143.
One actual alpha lint was 1/FAIL, while beta was prepared but unexecuted. This
interruption is historical evidence, not a passed negative scenario or product
repair. Its artifacts remain retained alongside the fresh negative002 root.

The eight-count order is Mono-Koto, Use Case, Entity, Event, Structure,
Classification, Workflow and StateMachine. Both positives recorded before and
post vectors `[3,1,1,3,1,2,3,5]`. Each complete before, selection and Phase 9
report equalled its own producer; all eight DTOs, sidecars, required sources,
fresh approval/application/effects, isolated environment and cursor predicates
were true, with no problems. Whole semantic selections across the two positives
were equal despite different producer before reports. Neither result grants
acceptance or cursor advance.

Negative002 recorded before `[3,1,1,3,1,2,3,5]`, post `[]`, `Failed` and
continuity `None`. Its actual ApplicationReport was retained and equalled the
post report; physical effects and cursor evidence were retained. Alpha's actual
lint exit 1/FAIL reported `cml.domain.string-attribute` for `AlphaEntity.name`
using raw `string` at CML line 6. The two alpha problems were `command.exitcode`
and `command.finding`; beta exit 0 had findings `[]`. Complete actual outputs
were transported uncoerced. A failed command grants no continuity, rollback,
cursor advance or Git acceptance.

The selected three completed runs supply six actual lint results: four positive
exit-0/findings-`[]` results plus negative alpha exit 1 and beta exit 0. The
interrupted alpha result is additional historical evidence outside those six.
The parent inspected full stdout/stderr against actual native execution and v6
receipts. Run-owned wrapper/process/ready/producer/consumer/observation records,
`terminal-summary.json` and stdout/stderr files remain disposable ignored
evidence under the relative roots above. Original complete command receipts and
logs remain privately retained; no raw logs or private runner paths are committed.

Both positives independently generated alpha and beta from their actual applied
CML using `--runtime 0.3.3-SNAPSHOT modeler-scala-value` with their own
`--save RUN/generated/alpha` or `--save RUN/generated/beta`. All four terminal
receipts exited 0. Full stdout was `Tree` followed by a newline; full stderr
contained the SLF4J multiple-provider warning and actual provider selection,
with no error. Nonempty generated declarations were retained at:

| Run directory | Alpha output relative to run root | Beta output relative to run root |
| --- | --- | --- |
| p107-s02-positive-001 | generated/alpha/target/scala-3.3.8/src_managed/main/scala/domain/value/AlphaValue.scala | generated/beta/target/scala-3.3.8/src_managed/main/scala/domain/value/BetaValue.scala |
| p107-s02-positive-002 | generated/alpha/target/scala-3.3.8/src_managed/main/scala/domain/value/AlphaValue.scala | generated/beta/target/scala-3.3.8/src_managed/main/scala/domain/value/BetaValue.scala |

Each file has its `case class AlphaValue(value: String)` or
`case class BetaValue(value: String)` declaration at line 21, and generated
metadata records Cozy version `0.3.3-SNAPSHOT`. Generated Scala compilation was
not executed or proven. No negative generation was performed.

Observed local artifact/config selections and read metadata were Cozy launcher
`0.1.6`, explicit runtime `0.3.3-SNAPSHOT`, selected classpath Modeler
`1.1.26-SNAPSHOT`, SmartDox `2.4.19-SNAPSHOT` and collaborator API `0.2.0`.
The existing Test classpath used Scala `3.3.8` and CNCF `0.5.3-SNAPSHOT`;
project component was `0.1.0-SNAPSHOT`, user-selected sbt-cozy was
`0.1.18-SNAPSHOT`, with Temurin `21.0.2+13` and managed CPython `3.12`.
These observations establish local selections, not public release certification
or authentication. No install, publication or version change occurred.

The prior actual Step 01 representative six tests and ten-suite/185-test
accumulator all passed, with compilation/SBT/wrapper exit 0 and terminal serial
lock release. Scala owners are unchanged by Step 02; no redundant SBT rerun is
claimed. Step 01 command observations were synthetic owner-composition evidence;
Step 02 supplies actual external outcomes separately. Existing CAR lint exit 0
with four known warnings is retained, not rerun or claimed as final Phase lint.

The committed [compatibility checker](../../scripts/check-runtime-compatibility.py)
ran through the registered one-shot runner with
`--runtime 0.5.3-SNAPSHOT --evidence representative-sar`. Native exit was 0,
stderr was empty, and stdout reported `DECLARATION_OK` with
minimum/tested/compile all `0.5.3-SNAPSHOT`, excluded none, and `CANDIDATE_OK`
with runtime `0.5.3-SNAPSHOT`, classification `tested-compatible` and evidence
`representative-sar`. This establishes declaration consistency only, not new
live ABI/runtime validation.

Semantic owner refresh remains independently declared fixture evidence through
existing constructors/validators, not a CML-derived model or provider response.
Owner revisions and fresh exact human input are typed test inputs, not
authenticated public human interaction. No original producer/provider state
was retained into the fresh consumer. No configured public CBD endpoint,
installed service, MCP/HTTP, deployment, publication, push, rollback or Git
acceptance was exercised.

The parent's initial source/AST/whitespace/link preflight passed. Final report
truth/static preflight, diff verification, independent complete-Step review
and separate typed local Step commit remain required. Step 03 and final Phase
lint/full-suite/full review/checklist closure and a distinct release remain
outstanding. This report cannot close the Phase or substitute for those gates.

## Terminal validation and closure — 2026-10-04

This later acceptance record supersedes only pending execution status in the
historical Step02 report above. Accepted Steps are `272f59d`, `6f7cef8` and
`3f22573`; the sole independent `P107-PHASE-FULL-REVIEW-001/1` is PASS over
67 inputs / 20 Phase-owned paths and whole-file compliance for all six programs.
The original PLAN epoch/base and review/repair history are retained.

Final actual command `P107-PHASE-FULL-VALIDATION-002/1` used
`sbt --batch 'set Test / fork := true' test`: 1,048 succeeded, zero failed,
canceled, ignored or pending; 131 completed suites / zero aborted; SBT0,
wrapper0 and terminal lock released. No excluded test or program change was
needed. The earlier full001 ran without the required forked Test runtime and
failed 25 class-loading tests; it remains failed evidence, not coverage.
Full002's one-use same-child pre-submission retention recovery submitted the
retained request once; its initial procedural deviation remains recorded.

Actual successful result and serial summary are retained at:

- `/private/tmp/skill.cncf.d/a-ea5abc77-390d-47d7-9706-9b43643e12eb/command-result.json`
- `/private/tmp/skill.cncf.d/a-84b5f4f4-edb7-4b18-94fd-19de91e0c3bc/44856-20261004T071027Z.summary.json`

Current final CAR lint, native parent command
`exec-9bbb4d58-645f-447e-bbd2-4c3cfe275a6e` / session6358, exited0 with zero
FAIL/four known WARN. Retained owners are
`DEV-P10-04-FILESYSTEM-CAPABILITY-001`,
`DEV-P10.1-STEP-02-CAR-FILESYSTEM-001`, `HYG-P9-20A-CAR-ABI-BASELINE` and
`HYG-P9-20A-CAR-SBT-COZY-SNAPSHOT`; none is new Phase10.7 debt. The accepted
manual/CML/service/operation descriptions and user/developer/repository skill
instructions match the bounded test-only surface. Public CLI/Web help and
configured CBD review endpoints remain unverified.

[The canonical checklist](../phase/phase-10.7-checklist.md) closes P10-70–P10-73
and the Phase10 sequence in the distinct local release carrying
`Phase-Closure-Binding: P107-PHASE-CLOSURE-001@1`. Accepted Hygiene and
Development Candidate lists are empty; their two canonical follow-up journals
are absent-no-items. Exact shared index/strategy synchronization is deferred,
and all seven concurrent future planning paths are preserved outside staging.
This closure adds no proof of generated Scala compilation, new live ABI,
authenticated fixture inputs, deployed adapter, CML/Git/cursor acceptance,
rollback, transaction or crash/racing-writer safety. All original positive and
negative dispositions and independently declared semantic inputs remain intact.
