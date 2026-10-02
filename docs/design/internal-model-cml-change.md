---
status: validated-pending-Step-acceptance
decision_scope: P105-CML-CHANGE / P105-IMPLEMENT-001
updated_at: 2026-10-03
---

# Internal-model CML Change Design

The [CML change contract](../spec/internal-model-cml-change-contract.md) fixes
the behavior. One bounded eligibility/owned-mutation admission boundary composes
existing typed owners; their API, codecs and Phase 9 semantics are unchanged.

## Three slices and responsibilities

| Slice | Responsibility | Output |
| --- | --- | --- |
| P105-CML-CHANGE-A | Current action admission, explicit applicability/live drift and independently admitted exact ownership | Complete typed report and optional plan; no writes |
| P105-CML-CHANGE-B | Dedicated skill reevaluation and exact existing-target application under source ownership | Explicit applied-source/request evidence and visible failure |
| P105-CML-CHANGE-C | Actual post-change Cozy/CBD validation and existing Phase 9 regeneration | Attributable validation and complete shared CCDM/eight-view evidence |

The parent owns validation selection/execution, independent Step acceptance,
one mandatory complete Phase review, Git acceptance/release and closure.
Runtime results neither advance a cursor nor complete a Phase.

## Slice A composition

The rooted gate captures the continuation once and delegates to the captured
gate. It checks original human/current review structural admissions, original
source inventory and exact typed live keys. Current review metadata agrees with
that current capture and independent selection, while a separately supplied
current rule/provider basis is evaluated independently of stored review fields.
The continuation gate readmits current human input, decisions, mappings and
issues from actual captured payload. The lifecycle evaluator separately retains
all original-to-current subject/execution/source invalidations even when the
continuation report is not Eligible.

For each original source snapshot, an immutable live entry preserves the existing
freshness-owner result. CML requests invoke the existing native no-follow reader;
they deliberately obtain current source evidence, not another persisted package
capture. Caller payload never substitutes for a current CML read. Exact-key
validation precedes conversion to the lifecycle owner's existing ID-locator map;
invalid full references are retained as contradictions and never injected there.

The report retains continuation, optional lifecycle, every source result, every
ownership/selection problem, and optional plan. Explicit problem categories
select Inconsistent, ReReviewRequired, Blocked, Incomplete or Eligible by the
contract's fixed precedence. A changed-and-missing source retains both causes
and therefore requires re-review as well as missing-version completion.
An unavailable lifecycle prerequisite suppresses only that impossible owner
call; its absence and all other observations remain visible.

Authority checks operate on a complete existing candidate target set. Root,
package, scope, exact target identity/path, source authority/identity and next
revision binding are independent caller evidence. Unknown current revisions
remain incomplete. A clean plan retains the original actual approval and all
exact current reference domains plus complete candidate targets/proposed bytes.
It is an inspectable application input requiring a fresh Slice B reevaluation,
not a durable authorization capability.

## Canonical application and validation

Slice B applies only admitted existing targets, preserves source-owned versions
and canonical authority, and reports the concrete source change for repository
acceptance. Slice C binds actual validation to that applied request/source basis
and reprojects through existing Phase 9 constructors. It retains all eight
views, full evidence/condition sidecars and canonical/enrichment separation.
No local CML semantic model/parser, inferred source revision, hidden winner,
automatic Git acceptance or speculative concurrency mechanism is introduced.

All five Phase requirements are covered by these responsibilities; Slice A
authoring alone does not establish P10-53 application or P10-54 execution.

## Slice B descriptor ownership

The package-private application entry validates the caller's explicit application
reference, invokes the rooted gate once, and forwards the eligible plan's target
bytes in candidate order to the native writer. Its complete gate remains in every
ordinary result. Failure is physical evidence, while `AppliedPendingValidation`
only hands that evidence to Slice C.

The writer owns one bounded Darwin LP64 descriptor lifetime. It records each
successful open, finishes root/parent/regular/link-count/physical-alias preflight
for every target, then truncates, writes, flushes and closes each target. Private
device/inode observations serve only preparation-resource ownership. Cleanup
walks still-owned descriptors in reverse and attempts each close once; cleanup
failures are separate from the primary cause. Earlier effects survive later
failure. The narrow runtime-private `WriteOperations` decorator supplies only
truncate/write/flush fault results, preserving real admission/open/stat/close.
The fixed ABI uses a 144-byte stat, mode at 4, unsigned link count at 6, signed
device at 0 and inode at 8, guarded by macOS and LP64 checks before native calls.

This mechanism addresses the frozen existing-target and partial-I/O failure
model. The local single writer supplies the concurrency assumption; descriptor
anchoring grants no transaction, rollback, crash durability or racing-writer
guarantee. There is no CML parser or source revision store.

The [developer guide](../developer-guide-internal-model-cml-change.md) and
[repository skill source](../skills/cbd-apply-cml-change/SKILL.md) explain the
actual internal engine and missing consuming-adapter stop. Phase 10.7 owns the
complete end-user workflow. New native/application executable specifications
are linked from the contract; focused dependency validation passed, while complete
Step/Phase acceptance remains pending.

## Slice C observation and owner composition

The package-private
[`InternalModelCmlChangePostValidation.validate`](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidation.scala)
keeps the actual complete B report beside independent raw command and refreshed
owner inputs. Closed results preserve missing and incompatible causes together;
Failed dominates Incomplete, and the successful full result is
ReprojectedPendingAcceptance. No command execution, source allocation, package
mutation, cursor advance or acceptance happens inside this boundary.

Exact one-per-target command observations bind the actual application and next
source revisions to runtime 0.3.3-SNAPSHOT and the frozen lint argv. Closed Cozy
finding JSON is the only parsed stdout; CML itself is not parsed. Full exit/
stdout/stderr and warning/FAIL evidence remain attributable caller admissions.
Independent owner selectors must name new realization/binding and target
snapshot references rather than infer them from versions or output. Rooted reload,
actual PackageFreshness CML reads and the existing ProjectionContinuityValidator
compose the source-backed shared CCDM and all eight owners. Every required snapshot
and every selected changed-target snapshot must be Compared(Unchanged) with no missing
dimensions, regardless of its optional inventory flag; only unrelated optional
MissingBaseline survives. Existing structured owner failures
propagate. Mapped exact identities, canonical/enrichment links, assertions,
association, attribution and complete conditions are checked against the actual
application's original shared realization.

The [post-validation specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidationSpec.scala)
uses synthetic command observations only to exercise this admission contract.
The [test support](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceSupport.scala)
merges the rich graph with independently declared all-eight core facts under
prefixed identities, preserving the single original selected Use Case and all
rich opaque lanes/conditions. Explicit owner refresh references and model source
revision are test declarations, unrelated to CML parsing or lint findings.
The [test-only probe](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceProbe.scala)
retains the actual application in one JVM while the parent executes both Cozy
commands. Positive and expected-negative runs have separate fresh retained roots;
coordination JSON supplies neither native ownership nor validation authority.
No probe is a deployed consuming adapter.

A (40 + 70) and B (27 + 118) focused passing specifications admit dependencies
only. C focused validation passed 17 specifications and the 215-specification
accumulator; real positive and expected-negative integration validation passed.
Complete-Step acceptance, full Phase review,
release and closure remain pending; Phase 10.5 remains OPEN.
