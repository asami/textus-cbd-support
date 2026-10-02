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
Phase 10.7 owns the complete end-user workflow.

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
not a successful simulation. A/B focused dependency validation passed A 40 + 70
and B 27 + 118 specifications; complete Step acceptance and Phase review/release
remain pending. C focused validation passed 17 specifications and the
215-specification accumulator; real positive and expected-negative integration
validation passed.

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
