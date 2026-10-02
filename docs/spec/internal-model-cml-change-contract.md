---
status: validated-pending-Step-acceptance
decision_scope: P105-CML-CHANGE / P105-IMPLEMENT-001
updated_at: 2026-10-03
---

# Internal-model CML Change Contract

This contract owns P10-50–P10-54. The [typed control contract](internal-model-typed-control-contract.md),
[source snapshot contract](internal-model-source-snapshot-contract.md),
[continuation contract](internal-model-continuation-and-rehydration-contract.md),
[human approval contract](internal-model-candidate-human-approval-contract.md), and
[approval lifecycle contract](internal-model-candidate-approval-lifecycle-contract.md)
retain their existing reference domains and admission responsibilities.
[Repository rules](../rules/repository-rules.md) prohibit content-derived control.
The [Phase 9 CCDM contract](canonical-component-design-model-contract.md) remains
the sole semantic authority, including canonical/enrichment lanes, complete
conditions, attribution, identities and all eight projections.

## P10-50: Explicit invalidation

The runtime-private, immutable request supplies current continuation input,
original actually admitted human approval, the complete original source snapshot
inventory, separately actually admitted current review, exact typed live-source
inputs, explicit optional supersession and explicit optional mutation authority.
Inputs are attributable caller admissions; they do not authenticate people or
sources. Current review is mandatory and cannot be inferred from stored claims
or replaced by original review. Current execution basis comes only from the
independently supplied continuation request.

The rooted entry captures `verifiedContinuation` once. The captured entry
delegates current action admission to `InternalModelContinuationActionGate` and
applicability to `InternalModelCandidateApprovalLifecycleEvaluator`. Pure package
consumers never reopen package paths. Structural helpers admit original approval
and current review. Current review's carrier, exact selected review/candidate/
diff/realization references, full subject, scope and semantic metadata must agree
with the current capture and independent request. Every original source is
observed. Original approval remains unchanged when any basis changes.

Candidate, realization/design, full subject, Scenario, referenced model,
glossary/BoK, rules/providers, continuity and CML projection drift require new
explicit reconciliation and review. Rule/provider changes remain observable even
if current continuation admission rejects the changed independent basis.
Supersession retains the exact separately admitted successor and all drift; it
never makes the predecessor usable or selects a newest record.

## P10-51: Live CML evidence

Live-input keys select complete known original source `ArtifactReference` values,
including version and role. Unknown or substituted keys are inconsistent and
are not passed to the lifecycle owner as bare-ID matches. Missing known
observations remain unavailable. Non-CML observations retain their existing
source-owner outcome. CML baselines require owner-supplied `CmlObserved`
authority, identity, explicit optional revision and current relative path;
`NativeCmlFileReader.read(projectRoot, path)` performs the actual current
physical no-follow regular-file read. Caller-supplied CML `Observed` bytes
cannot substitute for it. Non-observed availability/authorization/malformed/
ambiguity outcomes remain attributed. Every native read failure survives.

The existing freshness owner compares declared authority, identity, revision
and CML path. It retains both changed and missing dimensions. Missing owner
versions remain unknown, even with artifact versions. A revision becoming
unknown alone is Incomplete, while independently changed authority/identity/path
still requires re-review and retains that same missing revision.
Payload bytes establish neither drift nor permission; undeclared changes under an unchanged source
version are not detected. No CBD-local CML parser is introduced.

## P10-52: Exact actual human approval

Only an exact Eligible `HandoffApprovedCandidate` continuation row, with current
actual independent human input, complete current accepted decisions/mappings,
no unresolved human items and no semantic-approval-blocking issue can proceed.
Provider reviewed/approved flags, stored approval, missing human input, rejected
or changes-requested decisions, wrong stage/action, missing decisions/mappings
and superseded approvals cannot produce an application plan.

The closed result is `EligibleForSkillApplication`, `ReReviewRequired`,
`Incomplete`, `Inconsistent`, or `Blocked`. Null, malformed serialized/typed or
internally contradictory admission graphs fail through structured
`operationInvalid`. Well-formed metadata/ownership contradictions are
Inconsistent; valid attributable drift is ReReviewRequired; explicit decision,
issue or supersession blocks are Blocked; missing semantic facts/versions are
Incomplete. Preserve every continuation problem, lifecycle invalidation,
source outcome and ownership problem. Precedence is Inconsistent >
ReReviewRequired > Blocked > Incomplete > EligibleForSkillApplication.

## P10-53: Skill-owned application

Mutation authority independently names an explicit request RecordReference,
normalized absolute consuming project root, exact package and scope, provenance
SemanticSource, and a nonempty unique exact target set. Each target supplies
target ID, existing project-relative path, source authority/identity and explicit
next source revision. Targets must exactly equal the candidate set: no subset,
extras, creation, rename or deletion. Supplied next revisions are nonblank,
source-owned, and distinct from every known current revision for that source;
the gate never allocates or advances them. Provenance is caller-admitted and
cannot be obtained from provider approval.

Only a clean report emits a typed plan retaining authority request, exact
package/scope/full subject, candidate/realization/continuity/diff/review/actual
approval artifact and logical references, complete proposed target payloads,
baseline attribution, mappings/effects and explicit next source versions.
The gate writes nothing and grants no repository acceptance. A stored plan or
Eligible report is not a permission token: the dedicated skill must reevaluate
against current CML immediately before application.

Slice B owns application within the exact admitted root/targets and producer
version contract. It preserves canonical-source ownership, retains an explicit
application record and failure outcomes, and presents the changed source through
the owning project's Git-governed acceptance process. It cannot broaden target
scope, rebase approval, infer source versions, or accept Git on behalf of a user.
No lock/retry/rollback framework is required by this local single-writer contract.

### Slice B application and physical outcomes

`InternalModelCmlChangeApplication.applyApproved(projectRoot, request,
applicationReference)` is a package-private engine. It requires an explicit
structurally valid typed application RecordReference and freshly evaluates the
rooted gate once immediately before mutation. A cached report or plan cannot
authorize a write. Structured admission failure has no effects; a well-formed
noneligible gate yields `Rejected` with the complete gate and no write report.

`NativeCmlFileWriter.replaceExisting` holds all existing target descriptors
through an all-target preflight before truncation. On Darwin LP64 it descends
physically from `/` with directory no-follow descriptors, permitting only the
reader's existing `/var` and `/tmp` system aliases. Targets open write-only,
no-follow, close-on-exec and nonblocking, without creation or truncation flags.
Opened targets must be regular, have unsigned link count one, and have distinct
transient device/inode pairs. Unsafe paths, missing/unwritable/nonregular files,
symlinks at any component, hardlinks and physical aliases reject the entire
preflight with every target `NotAttempted` and no changes. Unsupported platform
or ABI fails closed. Physical observations never become control identities.

After preflight, candidate order determines effects. Each target is conservatively
marked potentially changed before truncation, then receives its complete bytes
(including legitimate empty payload), flush and close. Positive short writes
continue; only an individual truncate/write/flush failure with explicit EINTR
resumes. Zero, negative or oversized counts fail. Close is attempted once and
never retried. Earlier `Applied` effects, the `Failed` target's precise written
count/potential-change flag, later `NotAttempted` targets, primary failure and
reverse cleanup failures all survive. An applied-target close failure is a
failed target. No rollback, backup, transaction, crash durability or racing-writer
guarantee is claimed. Mode and ownership of the existing files are retained.

The immutable application result retains the explicit application reference,
complete freshly evaluated gate and optional complete native report. Any native
failure, cleanup failure or incomplete target outcome yields `Failed`. Only
every target physically applied after write/flush/close yields
`AppliedPendingValidation`. Owner-supplied next versions remain evidence of
intent/application; no owner store is automatically advanced. No acceptance,
cursor change or Git action occurs. The runtime-private fault seam changes only
truncate/write/flush syscalls; real rooted admission, anchored preflight and
close still occur.

The [dedicated repository skill source](../skills/cbd-apply-cml-change/SKILL.md)
requires exact independently admitted human/owner inputs and a reviewed consuming
internal adapter invoking this engine through registered runtime execution.
This versioned instruction source is not installed and supplies no deployed
CLI/MCP/UI or adapter. A missing adapter or exact input is an explicit dependency
stop. Partial failures require disclosure and renewed owner direction, without
workflow retry or rollback. Pending validation hands off actual Cozy/CBD checks,
independently refreshed owner evidence and repository Git acceptance.

## P10-54: Validation and projection

Slice C owns actual post-change Cozy/CBD validation and evidence bound to the
applied exact source versions/request. Only successful required validation can
support subsequent acceptance. It regenerates the shared Phase 9 CCDM and all
eight stakeholder/engineering views through their existing owners: Mono-Koto,
Use Case, Entity, Event, Structure, Classification, Workflow and StateMachine.
Exact identities, canonical/enrichment lanes, source attribution, conditions,
nullable facets and complete sidecars survive. Failed/unavailable validation or
projection remains explicit and prevents acceptance; authored documentation or
a metadata receipt cannot stand in for execution.

`InternalModelCmlChangePostValidation.validate(projectRoot, request)` is the
package-private Slice C entry. Its immutable request retains the complete actual
`ApplicationReport`, an independent post-validation RecordReference, all raw
command observations and optional independently refreshed owner evidence.
Malformed typed graphs return structured `operationInvalid`; ordinary denials
retain all input evidence. Its closed dispositions are `Incomplete`, `Failed`
and `ReprojectedPendingAcceptance`, with Failed taking precedence over missing
facts. The successful result is still pending separate repository acceptance.

There must be exactly one uniquely referenced observation for every physically
Applied target. Each binds the exact application/root/target/path/source owner,
approved next revision and runtime `0.3.3-SNAPSHOT`. Its argv is exactly
`--runtime 0.3.3-SNAPSHOT lint cml <normalized absolute applied target> --format json`.
The caller independently admits actual execution; the entry executes no command
and authenticates no process. Missing or indeterminate outcomes stay Incomplete.
Nonzero exit, a FAIL finding or malformed stdout fails. Only the closed Cozy JSON
root `findings` array is parsed, with closed `level`, `code`, `message`, `path`,
positive Int `line` records and levels FAIL/WARN. Warnings and full stdout/stderr/
exit observations survive. Synthetic unit observations prove admission only.

Independent owner evidence selects the original package/scope, four explicit new
artifact/logical realization and binding references and one new SourceSnapshot
reference per target. It supplies exact new-reference-keyed live inputs; CML
requires `CmlObserved` source authority/identity/approved next revision/path.
Unknown, duplicate, stale or wrong-role references and CML byte substitutes fail.
Missing owner facts remain Incomplete; versions are never inferred or incremented.
The entry reloads `verifiedContinuation`, compares actual carrier selections,
uses `PackageFreshness.check` for actual no-follow CML reads and requires every
required snapshot and every selected changed-target snapshot to be Compared(Unchanged)
without missing dimensions, regardless of its optional inventory flag. Only unrelated
optional MissingBaseline survives. The actual `ProjectionContinuityValidator` reconstructs
the source-backed shared realization, binding and all eight DTOs. Existing owner
structured failures propagate unchanged.

Every candidate mapping remains attached to its exact `(kind, semanticIdentity)`
element or relationship, including the distinct `opaque-shared` lanes. Mapped
canonical/enrichment assertion content, association and source authority/identity/
locator/anchor, plus full condition kind/affected identity/detail, must survive.
Explicit source-owner revision/snapshot refresh is allowed. A semantically valid
graph that erases mapping links or changes lanes/conditions cannot progress.
The validator writes no CML/package/cursor/Git state and creates no local model,
CML parser, source-owner revision, acceptance token or production serializer.

## Executable obligations and boundaries

[InternalModelCmlChangeGateSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeGateSpec.scala)
specifies the Slice A success/failure matrix with a rich two-target actual-human
fixture, Given/When/Then, `should` matchers and active ScalaCheck. Source,
record/artifact versions and opaque identities remain independent; carrier
control additions and harmless ordering cannot invalidate unchanged semantics.

[NativeCmlFileWriterSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriterSpec.scala)
specifies anchored all-target preflight, actual byte effects and syscall failures.
[InternalModelCmlChangeApplicationSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplicationSpec.scala)
specifies fresh actual-human admission, exact retained evidence and pending
validation. The [developer guide](../developer-guide-internal-model-cml-change.md)
describes the private entry and consuming-adapter dependency.

[InternalModelCmlChangePostValidationSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidationSpec.scala)
uses real B writes/current carriers and owner constructors around explicitly
synthetic unit terminal observations, Given/When/Then, `should` and bounded
ScalaCheck. The test-only
[acceptance support](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceSupport.scala)
declares an independently refreshed rich/all-eight owner graph; it derives no
semantic facts from CML text or Cozy output. The
[same-JVM acceptance probe](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceProbe.scala)
retains the actual application while the parent runs both real registered Cozy
commands and supplies admitted full terminal observations. The positive plan
requires both exits 0, all required sources Unchanged, eight nonempty actual DTOs,
complete mapped/sequence/association/conflict/nullable sidecars and unchanged
resume bytes. The separate negative plan requires actual alpha exit 1 with
`cml.domain.string-attribute` FAIL, beta exit 0, Failed/no successful continuity
and retained actual file changes. Expected negative exit 0 from the probe proves
failed-Cozy handling; it is not successful validation.

Slice A authors read-only eligibility/ownership admission; Slice B authors the
existing-target application engine and instruction source. Their focused
dependency validation passed A 40 + 70 and B 27 + 118 specifications; these
results grant no Step acceptance. Slice C focused validation passed 17
specifications and the 215-specification accumulator; real positive and
expected-negative integration validation passed. Independent Step acceptance, mandatory full Phase
review, release validation/commit and checklist closure remain pending. No
public API/CLI/MCP/UI, publication, deployment, Phase 10.6/10.7, source/person
authentication, latest-selection, default/alias, hash, checksum, byte-equality
or encoded-content permission mechanism is introduced.
